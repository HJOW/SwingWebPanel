package org.duckdns.hjow.swingwebpanel.media.playback;

import javax.sound.sampled.AudioSystem;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The real sound output through Java Sound (plan item P18-10; FEATURE_MATRIX AUD-10), kept apart from the fake-output checks of
 * {@link MediaPlayerTest}: a format that no device can play fails with ACCESS_FAILED (with or without a device), and, when the machine
 * has a mixer, a player opens the real line, writes <em>silence</em> to it and reaches the end. These checks show that the real line can
 * be opened, written and closed and that the position clock follows the device; they do <strong>not</strong> show that anything can be
 * heard, because the sound that they play is silence and nobody listens (P22-10 stays open for a person with a sound device). When the
 * machine has no mixer or the device is busy, the device checks are skipped with that reason.
 * <p>
 * Java Sound 를 통한 실제 소리 출력(계획 P18-10; FEATURE_MATRIX AUD-10)을 {@link MediaPlayerTest} 의 가짜 출력 확인과 따로 둔다: 어떤 장치도 재생할 수 없는
 * 형식은 (장치가 있든 없든) ACCESS_FAILED 로 실패하고, 컴퓨터에 믹서가 있으면 재생기가 실제 라인을 열어 <em>무음</em>을 쓰고 끝에 닿는다. 이 확인은 실제
 * 라인을 열고 쓰고 닫을 수 있다는 것과 위치 시계가 장치를 따라간다는 것을 보여 줄 뿐, 무언가 들린다는 것을 보여 주지 <strong>않는다</strong>. 재생하는
 * 소리가 무음이고 아무도 듣지 않기 때문이다(P22-10 은 소리 장치를 가진 사람을 위해 열려 있다). 믹서가 없거나 장치를 쓰고 있으면 장치 확인은 그 이유를 대고
 * 건너뛴다.
 */
public class JavaSoundDeviceTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public JavaSoundDeviceTest() {
    }

    /**
     * A format that no device supports (an absurd rate, too many channels) fails with ACCESS_FAILED and a message, and never with another
     * exception; this holds on a machine without a mixer too.
     * <p>
     * 어떤 장치도 지원하지 않는 형식(터무니없는 주파수, 너무 많은 채널)은 ACCESS_FAILED 와 메시지로 실패하며 다른 예외는 던지지 않는다. 믹서가 없는
     * 컴퓨터에서도 마찬가지이다.
     */
    public void testUnsupportedFormatFailsWithAccessFailed() {
        JavaSoundOutputFactory factory = new JavaSoundOutputFactory();
        int[][] formats = { { 3, 1 }, { 1000000000, 2 }, { 44100, 9 }, { 8000, 0 }, { -5, 1 } };
        // 지원하지 않는 형식을 하나씩 열어 본다.
        for (int[] f : formats) {
            try {
                factory.open(f[0], f[1]);
                Check.fail("an output was opened for " + f[0] + " Hz, " + f[1] + " channels");
            } catch (MediaException e) {
                Check.equal(MediaException.Kind.ACCESS_FAILED, e.getKind(), "the kind of the failure for " + f[0] + " Hz, " + f[1] + " channels");
                Check.isTrue(e.getMessage() != null && e.getMessage().length() > 0, "the failure has a message");
            }
        }
    }

    /**
     * Opens the real line for a moment: the output accepts silence, reports its queue, stops, starts, flushes and closes without an error,
     * and a closed output can be replaced by a new one. Skipped when the machine has no mixer or the device cannot be opened.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRealLineAcceptsSilence() throws Exception {
        if (AudioSystem.getMixerInfo().length == 0) {
            Check.skip("this machine has no Java Sound mixer");
        }
        JavaSoundOutputFactory factory = new JavaSoundOutputFactory();
        AudioOutput out = null;
        try {
            out = factory.open(44100, 1);
        } catch (MediaException e) {
            Check.skip("the sound device cannot be opened here: " + e.getMessage());
        }
        try {
            byte[] silence = new byte[44100 / 10 * 2];
            out.start();
            int written = 0;
            long end = System.currentTimeMillis() + 5000;
            // 한 번에 다 들어가지 않을 수 있으므로 장치에 빈 곳이 생길 때마다 이어서 쓴다.
            while (written < silence.length && System.currentTimeMillis() < end) {
                int n = out.write(silence, written, silence.length - written);
                written += n;
                if (n == 0) {
                    Thread.sleep(5);
                }
            }
            Check.equal(silence.length, written, "the whole silence was accepted");
            Check.isTrue(out.getQueuedFrames() >= 0, "the queue is reported");
            out.stop();
            out.start();
            out.flush();
            Check.equal(0, out.getQueuedFrames(), "a flushed line holds nothing");
        } finally {
            out.close();
        }
        AudioOutput second = factory.open(44100, 2);
        second.close();
    }

    /**
     * A player with the real output plays half a second of silence to the end: the output was opened, the clock reached the duration, the end
     * was reported once and the line is closed. The reports are the ones that the media element uses, so this is the path of a real page with
     * the real device, but with silence.
     * Skipped when the machine has no mixer or the device cannot be opened.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlayerPlaysSilenceThroughTheRealLine() throws Exception {
        if (AudioSystem.getMixerInfo().length == 0) {
            Check.skip("this machine has no Java Sound mixer");
        }
        RecordingListener l = new RecordingListener();
        MediaPlayer p = new MediaPlayer(new JavaSoundOutputFactory(), l);
        try {
            p.load(AudioKit.wav(1, 1, 44100, 16, new byte[44100 / 2 * 2], -1), "audio/wav");
            l.await("loaded", 1);
            p.play();
            long end = System.currentTimeMillis() + 20000;
            // 끝이나 실패가 보고될 때까지 기다린다.
            while (l.count("ended") == 0 && l.count("failed") == 0 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            if (l.count("failed") > 0) {
                Check.skip("the sound device cannot be played here: " + l.error().getMessage());
            }
            Check.equal(1, l.count("ended"), "the end was reported once");
            Check.near(0.5, p.getPosition(), 1e-6, "the clock reached the duration");
            Check.isTrue(p.isEnded(), "ended");
        } finally {
            p.close();
        }
    }
}
