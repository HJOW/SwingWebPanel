package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutput;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.ManualOutputFactory;

/**
 * A simulated sound device for the tests of real-time contexts (not a suite): it plays the outputs that a {@link ManualOutputFactory} opened at the pace of the wall clock, as a device does (a started
 * output consumes as many frames per second as its sample rate says), so that a context renders at real speed and does not race ahead of the script of the page. The audio that an output received stays
 * readable ({@link ManualOutput#copyChannel(int)}). Pacing can be switched off to look at a buffer that nobody consumes.
 * <p>
 * 실시간 컨텍스트 시험을 위한 모의 소리 장치(스위트가 아님): {@link ManualOutputFactory} 가 연 출력을 벽시계의 속도로 재생한다. 장치처럼 시작된 출력은 초당 표본율만큼의 프레임을 소비하므로
 * 컨텍스트는 실제 속도로 렌더링하며 페이지의 스크립트보다 앞서 달려가지 않는다. 출력이 받은 오디오는 계속 읽을 수 있다({@link ManualOutput#copyChannel(int)}). 아무도 소비하지 않는 버퍼를
 * 보려면 속도 맞춤을 끌 수 있다.
 */
final class PacedDevice implements Runnable {

    /**
     * The factory whose outputs are played.
     * <p>
     * 출력을 재생하는 팩터리.
     */
    private final ManualOutputFactory factory;

    /**
     * The fraction of a frame that was not consumed yet, per output (the thread of the device only).
     * <p>
     * 출력마다 아직 소비하지 않은 프레임의 소수 부분(장치 스레드만).
     */
    private final List<Double> carry = new ArrayList<Double>();

    /**
     * True to stop the thread.
     * <p>
     * 스레드를 멈추려면 true.
     */
    private volatile boolean stopped;

    /**
     * True while the device consumes.
     * <p>
     * 장치가 소비하는 동안 true.
     */
    private volatile boolean playing = true;

    /**
     * The thread of the device.
     * <p>
     * 장치의 스레드.
     */
    private final Thread thread;

    /**
     * Starts the device for the outputs of a factory.
     * <p>
     * 팩터리의 출력을 위한 장치를 시작한다.
     *
     * @param factory the factory, whose outputs must not consume by themselves / 팩터리. 출력이 스스로 소비하면 안 된다
     */
    PacedDevice(ManualOutputFactory factory) {
        this.factory = factory;
        this.thread = new Thread(this, "swingwebpanel-test-device");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Switches the pacing on or off.
     * <p>
     * 속도 맞춤을 켜거나 끈다.
     *
     * @param value true to consume, false to leave the buffers alone / 소비하려면 true, 버퍼를 그대로 두려면 false
     */
    void setPlaying(boolean value) {
        this.playing = value;
    }

    /**
     * Stops the thread of the device.
     * <p>
     * 장치의 스레드를 멈춘다.
     */
    void close() {
        stopped = true;
        thread.interrupt();
    }

    /**
     * The loop of the device: every few milliseconds every started output consumes the frames that the wall clock allows.
     * <p>
     * 장치의 루프: 몇 밀리초마다 시작된 모든 출력이 벽시계가 허용하는 만큼의 프레임을 소비한다.
     */
    public void run() {
        long last = System.nanoTime();
        // 멈출 때까지 짧게 쉬며 출력마다 지난 시간만큼의 프레임을 소비한다.
        while (!stopped) {
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                return;
            }
            long now = System.nanoTime();
            double seconds = (now - last) / 1e9;
            last = now;
            int count = factory.count();
            // 새 출력의 소수 부분 자리를 마련한다.
            while (carry.size() < count) {
                carry.add(Double.valueOf(0));
            }
            // 열린 출력마다 소비한다.
            for (int i = 0; i < count; i++) {
                ManualOutput o = factory.get(i);
                if (o == null || o.isClosed() || !o.isStarted() || !playing) {
                    continue;
                }
                double due = o.getSampleRate() * seconds + carry.get(i).doubleValue();
                long whole = (long) Math.floor(due);
                carry.set(i, Double.valueOf(due - whole));
                if (whole > 0) {
                    o.consume(whole);
                }
            }
        }
    }
}
