package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.io.IOException;
import java.util.ArrayList;

/**
 * An {@link OutputFactory} for tests (plan item P25-07 tests): it opens {@link ManualOutput}s, remembers all of them and can be told to fail
 * (a machine without a device), to open a different number of channels than the context asks for (a mono device), or to make every output
 * consume at once. The class is not a suite.
 * <p>
 * 시험용 {@link OutputFactory}(계획 P25-07 시험): {@link ManualOutput} 을 열고 모두 기억하며, 실패하도록(장치가 없는 컴퓨터), 컨텍스트가 요청한 것과 다른 채널
 * 수를 열도록(모노 장치), 모든 출력이 곧바로 소비하도록 지시할 수 있다. 이 클래스는 스위트가 아니다.
 */
public final class ManualOutputFactory implements OutputFactory {

    /**
     * The outputs that were opened.
     * <p>
     * 열린 출력.
     */
    private final ArrayList<ManualOutput> opened = new ArrayList<ManualOutput>();

    /**
     * The message of the failure, or null if the factory opens outputs.
     * <p>
     * 실패 메시지. 팩터리가 출력을 열면 null.
     */
    private volatile String failure;

    /**
     * The number of channels to open instead of the requested number, or 0.
     * <p>
     * 요청한 수 대신 열 채널 수. 없으면 0.
     */
    private volatile int channelsOverride;

    /**
     * True if the outputs consume at once.
     * <p>
     * 출력이 곧바로 소비하면 true.
     */
    private volatile boolean autoConsume;

    /**
     * Creates a factory that opens outputs.
     * <p>
     * 출력을 여는 팩터리를 만든다.
     */
    public ManualOutputFactory() {
    }

    /**
     * Makes the factory fail with a message (null makes it open outputs again).
     * <p>
     * 팩터리가 메시지와 함께 실패하게 한다(null 이면 다시 출력을 연다).
     *
     * @param message the message or null / 메시지 또는 null
     */
    public void setFailure(String message) {
        this.failure = message;
    }

    /**
     * Makes the factory open a number of channels instead of the requested number (0 for the requested number).
     * <p>
     * 요청한 수 대신 채널 수를 열게 한다(0 이면 요청한 수).
     *
     * @param channels the number of channels or 0 / 채널 수 또는 0
     */
    public void setChannelsOverride(int channels) {
        this.channelsOverride = channels;
    }

    /**
     * Makes the outputs that are opened later consume at once.
     * <p>
     * 이후 열리는 출력이 곧바로 소비하게 한다.
     *
     * @param value true to consume at once / 곧바로 소비하려면 true
     */
    public void setAutoConsume(boolean value) {
        this.autoConsume = value;
    }

    /**
     * Opens a manual output, or fails if asked to.
     * <p>
     * 수동 출력을 연다. 그러라고 했으면 실패한다.
     *
     * @param sampleRate   the sample rate / 샘플링 주파수
     * @param channels     the requested channels / 요청한 채널 수
     * @param bufferFrames the buffer in frames / 버퍼(프레임)
     * @return the output / 출력
     * @throws IOException if the factory was told to fail / 실패하라고 했으면
     */
    public AudioOutput open(int sampleRate, int channels, int bufferFrames) throws IOException {
        String f = failure;
        if (f != null) {
            throw new IOException(f);
        }
        int ch = channelsOverride > 0 ? channelsOverride : channels;
        ManualOutput out = new ManualOutput(sampleRate, ch, bufferFrames);
        out.setAutoConsume(autoConsume);
        synchronized (opened) {
            opened.add(out);
        }
        return out;
    }

    /**
     * Returns the number of outputs that were opened.
     * <p>
     * 열린 출력의 수를 반환한다.
     *
     * @return the count / 수
     */
    public int count() {
        synchronized (opened) {
            return opened.size();
        }
    }

    /**
     * Returns an output that was opened (0 is the first), or null if there is none.
     * <p>
     * 열린 출력을 반환한다(0 이 첫 번째). 없으면 null.
     *
     * @param index the index / 인덱스
     * @return the output or null / 출력 또는 null
     */
    public ManualOutput get(int index) {
        synchronized (opened) {
            return index < opened.size() ? opened.get(index) : null;
        }
    }

    /**
     * Returns the output that was opened last, or null.
     * <p>
     * 마지막으로 열린 출력을 반환한다. 없으면 null.
     *
     * @return the output or null / 출력 또는 null
     */
    public ManualOutput last() {
        synchronized (opened) {
            return opened.isEmpty() ? null : opened.get(opened.size() - 1);
        }
    }

    /**
     * Waits until at least a number of outputs were opened.
     * <p>
     * 적어도 일정 수의 출력이 열릴 때까지 기다린다.
     *
     * @param n      the number of outputs / 출력 수
     * @param millis the longest wait in milliseconds / 가장 오래 기다릴 시간(밀리초)
     * @return the output with the index n - 1, or null on a timeout / 인덱스 n - 1 의 출력. 시간이 지나면 null
     * @throws InterruptedException if interrupted / 중단되면
     */
    public ManualOutput waitFor(int n, long millis) throws InterruptedException {
        long end = System.currentTimeMillis() + millis;
        // 시간이 지날 때까지 출력이 열리기를 짧게 쉬며 기다린다.
        while (System.currentTimeMillis() < end) {
            ManualOutput o = get(n - 1);
            if (o != null) {
                return o;
            }
            Thread.sleep(2);
        }
        return get(n - 1);
    }
}
