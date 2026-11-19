package org.duckdns.hjow.swingwebpanel.media.playback;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.audio.testkit.AudioKit;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the audio player (plan items P18-08, P18-10; FEATURE_MATRIX AUD-10): the PCM that reaches the output, the position clock,
 * pause and resume, seeking, looping, the software volume, a machine without a sound device, unsupported and damaged data, the ranges, a
 * source that does not know its length or cannot seek, and the release of the thread and the output. The sound goes to a fake output
 * ({@link FakeOutputs}), so everything is exact; no real sound device is used and nothing is heard (the check with a real device is a
 * separate manual step).
 * <p>
 * 오디오 재생기 시험(계획 P18-08, P18-10; FEATURE_MATRIX AUD-10): 출력에 도달하는 PCM, 위치 시계, 일시정지와 재개, 탐색, 반복, 소프트웨어 음량,
 * 소리 장치가 없는 컴퓨터, 지원하지 않거나 손상된 데이터, 범위, 길이를 모르거나 탐색할 수 없는 소스, 스레드와 출력의 해제. 소리는 가짜 출력
 * ({@link FakeOutputs})으로 가므로 모든 것이 정확하다. 실제 소리 장치는 쓰지 않으며 아무 소리도 들리지 않는다(실제 장치 확인은 별도의 수동 단계이다).
 */
public class MediaPlayerTest {

    /**
     * The sample rate of the test media.
     * <p>
     * 시험 미디어의 샘플링 주파수.
     */
    private static final int RATE = 8000;

    /**
     * Creates the suite (required by the runner) and registers the scripted decoder.
     * <p>
     * 스위트를 만들고(실행기가 필요로 함) 스크립트 디코더를 등록한다.
     */
    public MediaPlayerTest() {
        ScriptedDecoder.install();
    }

    /**
     * Makes deterministic mono 16-bit PCM.
     * <p>
     * 결정적인 모노 16비트 PCM 을 만든다.
     *
     * @param frames the number of frames / 프레임 수
     * @return the bytes / 바이트
     */
    private static byte[] pcm(int frames) {
        return ScriptedDecoder.pcm(0, frames);
    }

    /**
     * Makes a mono WAV file of the ramp.
     * <p>
     * 램프의 모노 WAV 파일을 만든다.
     *
     * @param frames the number of frames / 프레임 수
     * @return the file / 파일
     */
    private static byte[] wav(int frames) {
        return AudioKit.wav(1, 1, RATE, 16, pcm(frames), -1);
    }

    /**
     * Creates a loaded player and waits for the report.
     * <p>
     * 로드된 재생기를 만들고 보고를 기다린다.
     *
     * @param outs the outputs / 출력
     * @param l    the listener / 리스너
     * @param file the data / 데이터
     * @return the player / 재생기
     * @throws Exception on failure / 실패 시
     */
    private static MediaPlayer loaded(FakeOutputs outs, RecordingListener l, byte[] file) throws Exception {
        MediaPlayer p = new MediaPlayer(outs, l);
        p.load(file, "audio/wav");
        l.await("loaded", 1);
        return p;
    }

    /**
     * Renders frames of a gated output until the player wrote a number of bytes.
     * <p>
     * 일정 바이트가 쓰일 때까지 재생기가 쓰는 출력의 프레임을 재생한다.
     *
     * @param o     the output / 출력
     * @param bytes the bytes to wait for / 기다릴 바이트 수
     * @throws Exception on failure / 실패 시
     */
    private static void renderUntilWritten(FakeOutputs.Out o, int bytes) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        // 충분히 쓰일 때까지 조금씩 재생하며 쉰다.
        while (o.written().length < bytes) {
            o.consume(300);
            if (System.currentTimeMillis() > end) {
                throw new AssertionError("only " + o.written().length + " of " + bytes + " bytes were written");
            }
            Thread.sleep(1);
        }
    }

    /**
     * Renders a gated output until the player reports the end.
     * <p>
     * 재생기가 끝을 알릴 때까지 출력을 재생한다.
     *
     * @param o the output / 출력
     * @param l the listener / 리스너
     * @throws Exception on failure / 실패 시
     */
    private static void renderUntilEnded(FakeOutputs.Out o, RecordingListener l) throws Exception {
        long end = System.currentTimeMillis() + 20000;
        // 끝이 보고될 때까지 조금씩 재생하며 쉰다.
        while (l.count("ended") == 0) {
            o.consume(500);
            if (System.currentTimeMillis() > end) {
                throw new AssertionError("the end was not reported");
            }
            Thread.sleep(1);
        }
    }

    /**
     * Counts the live playback threads.
     * <p>
     * 살아 있는 재생 스레드 수를 센다.
     *
     * @return the number / 수
     */
    private static int liveThreads() {
        int n = 0;
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        // 이름이 재생 스레드인 살아 있는 스레드를 센다.
        for (Thread t : all.keySet()) {
            if ("swingwebpanel-audio".equals(t.getName()) && t.isAlive()) {
                n++;
            }
        }
        return n;
    }

    /**
     * Waits until the playback thread stops asking for the output (paused or ended): the written bytes stay the same.
     * <p>
     * 재생 스레드가 출력에 쓰기를 멈출 때까지(일시정지나 끝) 쓰인 바이트가 그대로일 때까지 기다린다.
     *
     * @param o the output / 출력
     * @throws Exception on failure / 실패 시
     */
    private static void awaitQuiet(FakeOutputs.Out o) throws Exception {
        int before = -1;
        // 두 번 연속 같은 값이 나올 때까지 짧게 쉬며 확인한다.
        for (int i = 0; i < 100; i++) {
            int now = o.written().length;
            if (now == before) {
                return;
            }
            before = now;
            Thread.sleep(15);
        }
        throw new AssertionError("the player keeps writing");
    }

    /**
     * Everything that was decoded reaches the output unchanged and in order, the output is opened once with the format of the media, it is closed
     * at the end, and the report order is loaded, then ended.
     * <p>
     * 디코딩한 모든 것이 변경 없이 순서대로 출력에 도달하고, 출력은 미디어의 형식으로 한 번 열리며 끝에서 닫히고, 보고 순서는 loaded 다음 ended 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlaysEverythingInOrder() throws Exception {
        FakeOutputs outs = new FakeOutputs();
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(4000));
        try {
            Check.equal(RATE, l.info().getSampleRate(), "sample rate");
            Check.equal(1, l.info().getChannels(), "channels");
            Check.equal(4000, l.info().getFrameCount(), "frames");
            Check.isTrue(l.info().isSeekable(), "WAV is seekable");
            Check.near(0.5, p.getDuration(), 1e-9, "duration");
            Check.near(0.0, p.getPosition(), 1e-9, "the position before playing");
            Check.isFalse(p.isEnded(), "not ended before playing");
            Check.equal(0, outs.openCalls, "no output before playing");
            p.play();
            l.await("ended", 1);
            Check.equal(1, outs.openCalls, "one output");
            FakeOutputs.Out o = outs.await(0);
            Check.equal(RATE, o.sampleRate, "output rate");
            Check.equal(1, o.channels, "output channels");
            Check.isTrue(Arrays.equals(pcm(4000), o.written()), "the PCM reached the output unchanged");
            Check.equal(1, o.closeCalls(), "the output is closed at the end");
            Check.isTrue(p.isEnded(), "ended");
            Check.near(0.5, p.getPosition(), 1e-9, "the position at the end is the duration");
            Check.isFalse(p.isPlaying(), "not playing after the end");
            Check.equal("loaded,started,ended", l.summary(), "reports");
        } finally {
            p.close();
        }
    }

    /**
     * Stereo media is delivered with two channels.
     * <p>
     * 스테레오 미디어는 채널 2개로 전달된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStereoFormat() throws Exception {
        byte[] stereo = new byte[800 * 4];
        // 채널마다 다른 값을 채운다.
        for (int i = 0; i < 800; i++) {
            stereo[i * 4] = (byte) i;
            stereo[i * 4 + 1] = 0x10;
            stereo[i * 4 + 2] = (byte) (255 - i);
            stereo[i * 4 + 3] = 0x20;
        }
        FakeOutputs outs = new FakeOutputs();
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, AudioKit.wav(1, 2, 22050, 16, stereo, -1));
        try {
            p.play();
            l.await("ended", 1);
            FakeOutputs.Out o = outs.await(0);
            Check.equal(22050, o.sampleRate, "rate");
            Check.equal(2, o.channels, "two channels");
            Check.isTrue(Arrays.equals(stereo, o.written()), "interleaved stereo unchanged");
        } finally {
            p.close();
        }
    }

    /**
     * The position is the sound that was rendered: it stays at the start while the output holds the data, and follows the frames that the output
     * renders, also while the player refills the buffer.
     * <p>
     * 위치는 재생된 소리이다: 출력이 데이터를 쥐고 있는 동안은 처음에 머물고, 출력이 재생하는 프레임을 따라가며, 재생기가 버퍼를 다시 채우는 동안에도
     * 그렇다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPositionFollowsRenderedFrames() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(16000));
        try {
            p.play();
            FakeOutputs.Out o = outs.await(0);
            o.awaitQueued(1000);
            Check.near(0.0, p.getPosition(), 1e-9, "nothing rendered yet");
            long total = 0;
            // 여러 번 조금씩 재생하며 위치가 재생된 프레임 수를 따르는지 확인한다.
            for (int i = 0; i < 6; i++) {
                total += o.consume(250);
                Check.near((double) total / RATE, p.getPosition(), 1e-9, "position after rendering " + total + " frames");
                o.awaitQueued(1000);
            }
            Check.isTrue(p.isPlaying(), "playing");
            Check.isFalse(p.isEnded(), "not ended");
        } finally {
            p.close();
        }
    }

    /**
     * Pausing stops the output before the call returns, keeps the position, stops the writing, and the played ranges end there; resuming
     * continues without a lost or repeated sample.
     * <p>
     * 일시정지는 호출이 돌아오기 전에 출력을 멈추고, 위치를 유지하며, 쓰기를 멈추고, 재생한 범위가 거기서 끝난다. 재개하면 빠지거나 반복되는 샘플
     * 없이 이어진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPauseAndResume() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(6000));
        try {
            p.play();
            FakeOutputs.Out o = outs.await(0);
            o.awaitQueued(1000);
            long rendered = o.consume(300);
            p.pause();
            Check.isFalse(o.isStarted(), "the output is stopped when pause returns");
            Check.equal(1, o.stopCalls(), "one stop");
            Check.isFalse(p.isPlaying(), "paused");
            double pausedAt = p.getPosition();
            Check.near((double) rendered / RATE, pausedAt, 1e-9, "the position at the pause");
            Check.equal(0, o.consume(500), "a stopped output renders nothing");
            awaitQuiet(o);
            int written = o.written().length;
            Thread.sleep(40);
            Check.equal(written, o.written().length, "nothing is written while paused");
            Check.near(pausedAt, p.getPosition(), 1e-9, "the position stands still");
            Check.equal("[0.0," + ((double) rendered / RATE) + "]", p.getPlayed().toString(), "the played range ends at the pause");
            p.play();
            renderUntilEnded(o, l);
            Check.equal(2, o.startCalls(), "started again by the resume");
            Check.isTrue(Arrays.equals(pcm(6000), o.written()), "no sample was lost or repeated over the pause");
            Check.near(0.75, p.getPosition(), 1e-9, "position at the end");
        } finally {
            p.close();
        }
    }

    /**
     * Seeking is visible in the position at once, flushes the output, and the sound continues from the new frame; a position past the end moves
     * to the end and a negative one to the start.
     * <p>
     * 탐색은 위치에 곧바로 보이고, 출력을 비우며, 소리는 새 프레임에서 이어진다. 끝을 넘는 위치는 끝으로, 음수는 처음으로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeekFlushesAndContinues() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(16000));
        try {
            p.play();
            FakeOutputs.Out o = outs.await(0);
            o.awaitQueued(1000);
            o.consume(200);
            p.seek(1.0);
            Check.near(1.0, p.getPosition(), 1e-9, "the new position is visible at once");
            l.await("seeked", 1);
            Check.isTrue(o.flushCalls() >= 1, "the output was flushed");
            o.awaitQueued(1000);
            Check.near(1.0, p.getPosition(), 1e-9, "the position after the seek");
            byte[] after = o.writtenSinceFlush();
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(8000, 9000), Arrays.copyOf(after, 2000)), "the sound continues at frame 8000");
            p.seek(-3);
            Check.near(0.0, p.getPosition(), 1e-9, "a negative position moves to the start");
            l.await("seeked", 2);
            o.awaitQueued(1000);
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(0, 1000), Arrays.copyOf(o.writtenSinceFlush(), 2000)), "the sound restarted at frame 0");
            p.seek(100);
            Check.near(2.0, p.getPosition(), 1e-9, "a position past the end moves to the end");
            l.await("ended", 1);
            Check.isTrue(p.isEnded(), "the end is reported after seeking to the end");
        } finally {
            p.close();
        }
    }

    /**
     * Seeking while paused moves the position without any sound, and the ranges of played time are merged: two separate runs give two ranges.
     * <p>
     * 일시정지 중의 탐색은 소리 없이 위치만 옮기고, 재생한 시간 범위는 합쳐진다: 따로 재생한 두 구간은 범위 두 개가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeekWhilePausedAndRanges() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(16000));
        try {
            Check.equal("[0.0,2.0]", p.getSeekable().toString(), "everything is seekable");
            Check.equal(0, p.getPlayed().size(), "nothing played yet");
            p.play();
            FakeOutputs.Out o = outs.await(0);
            o.awaitQueued(1000);
            o.consume(400);
            p.pause();
            p.seek(1.0);
            Check.near(1.0, p.getPosition(), 1e-9, "position after a seek while paused");
            l.await("seeked", 1);
            Check.near(1.0, p.getPosition(), 1e-9, "still there");
            Check.equal(1, p.getPlayed().size(), "one played range");
            Check.near(0.05, p.getPlayed().end(0), 1e-9, "played until 400 frames");
            p.play();
            o.awaitQueued(1000);
            o.consume(400);
            p.pause();
            awaitQuiet(o);
            Check.equal(2, p.getPlayed().size(), "two separate played ranges");
            Check.near(1.0, p.getPlayed().start(1), 1e-9, "the second range starts at the seek target");
            Check.near(1.05, p.getPlayed().end(1), 1e-9, "the second range");
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(8000, 9000), Arrays.copyOf(o.writtenSinceFlush(), 2000)), "the sound after the seek");
        } finally {
            p.close();
        }
    }

    /**
     * The loop flag wraps around to the start at the end without a gap and without an end report; the position stays inside the media.
     * <p>
     * 반복 플래그는 끝에서 틈 없이, 끝 보고 없이 처음으로 되돌아가며 위치는 미디어 안에 머문다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoop() throws Exception {
        FakeOutputs outs = new FakeOutputs(1500);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(3000));
        try {
            p.setLoop(true);
            p.play();
            FakeOutputs.Out o = outs.await(0);
            renderUntilWritten(o, 3000 * 2 * 3);
            p.pause();
            awaitQuiet(o);
            Check.isTrue(l.count("looped") >= 2, "wrapped around at least twice: " + l.count("looped"));
            Check.equal(0, l.count("ended"), "a loop never ends");
            byte[] written = o.written();
            byte[] one = pcm(3000);
            // 세 바퀴 모두 같은 소리인지 확인한다.
            for (int pass = 0; pass < 3; pass++) {
                Check.isTrue(Arrays.equals(one, Arrays.copyOfRange(written, pass * one.length, (pass + 1) * one.length)), "pass " + pass);
            }
            Check.isTrue(p.getPosition() >= 0 && p.getPosition() <= p.getDuration(), "the position stays inside the media: " + p.getPosition());
            p.setLoop(false);
            p.play();
            renderUntilEnded(o, l);
            Check.equal(1, l.count("ended"), "ends once the loop flag is off");
        } finally {
            p.close();
        }
    }

    /**
     * The volume scales the samples in software: half volume halves every sample, mute gives silence without losing the volume, values are
     * limited to 0..1, and a NaN means full volume.
     * <p>
     * 음량은 샘플을 소프트웨어로 곱한다: 절반 음량은 모든 샘플을 절반으로 하고, 음소거는 음량을 잃지 않고 무음을 내며, 값은 0..1 로 제한되고, NaN 은
     * 최대 음량이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVolumeAndMute() throws Exception {
        byte[] source = pcm(2500);
        double[] volumes = { 0.5, 0.25, 1.0, 5.0, -1.0, Double.NaN, 0.0 };
        int[] gains = { 16384, 8192, 32768, 32768, 0, 32768, 0 };
        // 음량마다 새 재생기로 재생하여 출력 샘플이 정수 이득의 곱과 정확히 같은지 확인한다.
        for (int v = 0; v < volumes.length; v++) {
            FakeOutputs outs = new FakeOutputs();
            RecordingListener l = new RecordingListener();
            MediaPlayer p = loaded(outs, l, wav(2500));
            try {
                p.setVolume(volumes[v]);
                p.play();
                l.await("ended", 1);
                short[] expected = AudioKit.toShorts(source);
                short[] actual = AudioKit.toShorts(outs.await(0).written());
                Check.equal(expected.length, actual.length, "length at volume " + volumes[v]);
                // 샘플마다 기대값과 비교한다.
                for (int i = 0; i < expected.length; i++) {
                    if (actual[i] != (short) ((expected[i] * gains[v]) >> 15)) {
                        Check.fail("sample " + i + " at volume " + volumes[v] + ": " + actual[i] + " instead of " + ((expected[i] * gains[v]) >> 15));
                    }
                }
            } finally {
                p.close();
            }
        }
        FakeOutputs outs = new FakeOutputs();
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(2500));
        try {
            p.setVolume(0.5);
            p.setMuted(true);
            p.play();
            l.await("ended", 1);
            Check.isTrue(Arrays.equals(new byte[source.length], outs.await(0).written()), "muted: silence");
            p.setMuted(false);
            p.seek(0);
            l.await("seeked", 1);
            p.play();
            l.await("ended", 2);
            byte[] all = outs.await(1).written();
            short[] expected = AudioKit.toShorts(source);
            short[] actual = AudioKit.toShorts(all);
            Check.equal((expected[7] * 16384) >> 15, actual[7], "unmuted again with the volume that was set before");
        } finally {
            p.close();
        }
    }

    /**
     * The playback rate may be a quarter to four times; other values are refused and the old rate stays.
     * <p>
     * 재생 속도는 4분의 1배에서 4배까지 가능하며, 그 밖의 값은 거절되고 이전 속도가 유지된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRateRange() throws Exception {
        MediaPlayer p = new MediaPlayer(new FakeOutputs(), new RecordingListener());
        try {
            Check.near(1.0, p.getRate(), 0, "the default rate");
            Check.isTrue(p.setRate(2.0), "2.0 is supported");
            Check.isTrue(p.setRate(0.5), "0.5 is supported");
            Check.near(0.5, p.getRate(), 0, "the rate that was set");
            Check.isFalse(p.setRate(8.0), "8.0 is not supported");
            Check.isFalse(p.setRate(0), "0 is not supported");
            Check.isFalse(p.setRate(-1), "backwards is not supported");
            Check.isFalse(p.setRate(Double.NaN), "NaN is not supported");
            Check.near(0.5, p.getRate(), 0, "a refused rate keeps the old one");
            Check.isTrue(MediaPlayer.isRateSupported(4.0) && !MediaPlayer.isRateSupported(4.01), "the upper limit");
        } finally {
            p.close();
        }
    }

    /**
     * At twice the speed the output gets about half the frames, the position clock counts two source frames per rendered frame and reaches
     * the duration at the end, and the end is reported; at half the speed twice the frames arrive.
     * <p>
     * 두 배 속도에서는 출력이 대략 절반의 프레임을 받고, 위치 시계는 재생한 프레임마다 소스 프레임 둘을 세어 끝에서 길이에 닿으며, 끝이 보고된다.
     * 절반 속도에서는 두 배의 프레임이 도착한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRateChangesLengthAndPosition() throws Exception {
        // 두 배 속도와 절반 속도를 차례로 확인한다.
        for (int k = 0; k < 2; k++) {
            double rate = k == 0 ? 2.0 : 0.5;
            FakeOutputs outs = new FakeOutputs(1000);
            RecordingListener l = new RecordingListener();
            MediaPlayer p = loaded(outs, l, wav(16000));
            try {
                Check.isTrue(p.setRate(rate), "the rate is supported");
                p.play();
                FakeOutputs.Out o = outs.await(0);
                o.awaitQueued(1000);
                long total = o.consume(400);
                // 첫 구간에서는 재생된 프레임마다 속도만큼의 소스 프레임을 센다.
                Check.near(total * rate / RATE, p.getPosition(), 0.02, "position after rendering " + total + " frames at " + rate);
                renderUntilEnded(o, l);
                int frames = o.written().length / 2;
                Check.isTrue(Math.abs(frames - 16000 / rate) <= 16000 / rate * 0.02 + 8, "output length at " + rate + ": " + frames);
                Check.isTrue(p.isEnded(), "ended");
                Check.near(2.0, p.getPosition(), 1e-9, "the position at the end is the duration");
            } finally {
                p.close();
            }
        }
    }

    /**
     * Changing the rate while playing keeps playing: the sound before the change has the old speed, the sound after it the new one, and the
     * position never runs backwards.
     * <p>
     * 재생 중에 속도를 바꿔도 계속 재생된다: 바꾸기 전의 소리는 이전 속도이고 이후는 새 속도이며, 위치는 뒤로 가지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRateChangeWhilePlaying() throws Exception {
        FakeOutputs outs = new FakeOutputs(1000);
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(32000));
        try {
            p.play();
            FakeOutputs.Out o = outs.await(0);
            o.awaitQueued(1000);
            o.consume(800);
            double before = p.getPosition();
            p.setRate(2.0);
            double last = before;
            // 속도를 바꾼 뒤 조금씩 재생하며 위치가 뒤로 가지 않는지 확인한다.
            for (int i = 0; i < 10; i++) {
                o.consume(300);
                Thread.sleep(2);
                double now = p.getPosition();
                Check.isTrue(now >= last - 1e-9, "the position runs forward: " + last + " -> " + now);
                last = now;
            }
            renderUntilEnded(o, l);
            Check.near(4.0, p.getPosition(), 1e-9, "the end");
            int frames = o.written().length / 2;
            Check.isTrue(frames < 32000 * 0.75 && frames > 32000 * 0.4, "a part of the sound was played twice as fast: " + frames);
        } finally {
            p.close();
        }
    }

    /**
     * A seek and a loop restart keep the rate: the sound after a seek is converted too, and the converter does not hold sound of the old
     * position.
     * <p>
     * 탐색과 반복 재시작은 속도를 유지한다: 탐색 뒤의 소리도 변환되고, 변환기는 이전 위치의 소리를 갖고 있지 않다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRateSurvivesSeekAndLoop() throws Exception {
        FakeOutputs outs = new FakeOutputs();
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(8000));
        try {
            p.setRate(2.0);
            p.seek(0.5);
            l.await("seeked", 1);
            p.play();
            l.await("ended", 1);
            int frames = outs.await(0).written().length / 2;
            Check.isTrue(Math.abs(frames - 2000) <= 80, "half of the remaining 4000 frames: " + frames);
            Check.near(1.0, p.getPosition(), 1e-9, "the end");
            p.setLoop(true);
            p.seek(0);
            l.await("seeked", 2);
            p.play();
            l.await("looped", 2);
            Check.isTrue(p.getRate() == 2.0, "the rate is still two");
            p.pause();
        } finally {
            p.close();
        }
    }

    /**
     * A machine without a sound device: playing fails with ACCESS_FAILED, the position does not move, nothing keeps running, and a later play
     * works as soon as a device is available.
     * <p>
     * 소리 장치가 없는 컴퓨터: 재생은 ACCESS_FAILED 로 실패하고, 위치는 움직이지 않으며, 계속 실행되는 것이 없고, 장치가 생기면 이후의 재생은 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoOutputDevice() throws Exception {
        FakeOutputs outs = new FakeOutputs();
        outs.failure = new MediaException(MediaException.Kind.ACCESS_FAILED, "no sound device");
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(2000));
        try {
            p.play();
            l.await("failed", 1);
            Check.equal(MediaException.Kind.ACCESS_FAILED, l.error().getKind(), "the device failure");
            Check.isFalse(p.isPlaying(), "not playing after the failure");
            Check.near(0.0, p.getPosition(), 1e-9, "the position did not move");
            Check.equal(1, outs.openCalls, "tried once");
            outs.failure = null;
            p.play();
            l.await("ended", 1);
            Check.isTrue(Arrays.equals(pcm(2000), outs.await(0).written()), "the retry played everything");
        } finally {
            p.close();
        }
        RecordingListener l2 = new RecordingListener();
        MediaPlayer none = new MediaPlayer(null, l2);
        try {
            none.load(wav(100), "audio/wav");
            l2.await("loaded", 1);
            none.play();
            l2.await("failed", 1);
            Check.equal(MediaException.Kind.ACCESS_FAILED, l2.error().getKind(), "no factory at all");
        } finally {
            none.close();
        }
    }

    /**
     * Data that cannot be played are reported as a load failure of the right kind and the player stays harmless.
     * <p>
     * 재생할 수 없는 데이터는 알맞은 종류의 로드 실패로 알리고 재생기는 해가 없는 채로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnsupportedAndDamagedData() throws Exception {
        byte[][] files = { new byte[0], "this is certainly not audio data at all".getBytes("US-ASCII"), "RIFF\u0004\u0000\u0000\u0000WAVE".getBytes("ISO-8859-1") };
        MediaException.Kind[] kinds = { MediaException.Kind.CORRUPT, MediaException.Kind.UNSUPPORTED_FORMAT, MediaException.Kind.CORRUPT };
        // 파일마다 알맞은 종류의 실패가 보고되는지 확인한다.
        for (int i = 0; i < files.length; i++) {
            RecordingListener l = new RecordingListener();
            MediaPlayer p = new MediaPlayer(new FakeOutputs(), l);
            try {
                p.load(files[i], "audio/wav");
                l.await("loadFailed", 1);
                Check.equal(kinds[i], l.error().getKind(), "kind of failure " + i);
                Check.equal("loadFailed", l.summary(), "only the failure is reported");
                p.play();
                p.seek(1);
                Check.near(0.0, p.getPosition(), 1e-9, "no position without media");
                Check.isTrue(Double.isNaN(p.getDuration()), "no duration without media");
                Check.isNull(p.getInfo(), "no info");
            } finally {
                p.close();
            }
        }
    }

    /**
     * A decoder that fails in the middle: the sound before the damage is played, the error is reported once as CORRUPT, the output is released,
     * and playing again fails the same way.
     * <p>
     * 도중에 실패하는 디코더: 손상 전의 소리는 재생되고, 오류는 CORRUPT 로 한 번 보고되며, 출력은 해제되고, 다시 재생해도 같은 식으로 실패한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamageInTheMiddle() throws Exception {
        FakeOutputs outs = new FakeOutputs();
        RecordingListener l = new RecordingListener();
        MediaPlayer p = new MediaPlayer(outs, l);
        try {
            p.load(ScriptedDecoder.data('C', 6000), "audio/x-script");
            l.await("loaded", 1);
            p.play();
            l.await("failed", 1);
            Check.equal(MediaException.Kind.CORRUPT, l.error().getKind(), "damaged data");
            FakeOutputs.Out o = outs.await(0);
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(0, 3000), o.written()), "the sound before the damage was written");
            Check.equal(1, o.closeCalls(), "the output was released");
            Check.isFalse(p.isPlaying(), "stopped");
            p.play();
            l.await("failed", 2);
            Check.equal(2, outs.openCalls, "the second try opened a second output");
        } finally {
            p.close();
        }
    }

    /**
     * Closing releases the thread and the output before it returns, reports nothing afterwards, is repeatable, and a closed or loaded player
     * cannot be loaded again.
     * <p>
     * 닫으면 돌아오기 전에 스레드와 출력을 해제하고, 그 뒤에는 아무것도 보고하지 않으며, 반복할 수 있고, 닫았거나 로드한 재생기는 다시 로드할 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCloseReleasesEverything() throws Exception {
        int before = liveThreads();
        FakeOutputs outs = new FakeOutputs(100);
        RecordingListener l = new RecordingListener();
        final MediaPlayer p = loaded(outs, l, wav(8000));
        p.play();
        FakeOutputs.Out o = outs.await(0);
        o.awaitQueued(100);
        p.close();
        Check.equal(1, o.closeCalls(), "the output is closed when close returns");
        Check.equal(before, liveThreads(), "the playback thread ended");
        String reports = l.summary();
        Thread.sleep(40);
        Check.equal(reports, l.summary(), "nothing is reported after close");
        p.close();
        p.play();
        p.seek(1);
        p.pause();
        Check.equal(1, o.closeCalls(), "closing twice does not close the output twice");
        Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Loads a player that was closed.
             * <p>
             * 닫은 재생기를 로드한다.
             */
            public void run() {
                p.load(new byte[1], null);
            }
        }, "a closed player cannot be loaded");
        MediaPlayer never = new MediaPlayer(outs, new RecordingListener());
        never.close();
        never.close();
    }

    /**
     * A player is loaded once.
     * <p>
     * 재생기는 한 번만 로드한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoadOnlyOnce() throws Exception {
        final MediaPlayer p = new MediaPlayer(new FakeOutputs(), new RecordingListener());
        try {
            p.load(wav(10), "audio/wav");
            Check.throwsException(IllegalStateException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
                /**
                 * Loads a second time.
                 * <p>
                 * 두 번째로 로드한다.
                 */
                public void run() {
                    p.load(wav(10), "audio/wav");
                }
            }, "second load");
        } finally {
            p.close();
        }
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Creates a player without a listener.
             * <p>
             * 리스너 없이 재생기를 만든다.
             */
            public void run() {
                new MediaPlayer(new FakeOutputs(), null);
            }
        }, "a listener is required");
    }

    /**
     * A source that does not know its length: the duration is infinite until the end, then it becomes known and is reported; the source
     * cannot seek, so seeking and looping read it again from the start.
     * <p>
     * 길이를 모르는 소스: 끝까지는 길이가 무한대이다가 끝에서 알게 되어 보고된다. 소스는 탐색할 수 없으므로 탐색과 반복은 처음부터 다시 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnknownLengthAndNoSeek() throws Exception {
        FakeOutputs outs = new FakeOutputs();
        RecordingListener l = new RecordingListener();
        MediaPlayer p = new MediaPlayer(outs, l);
        try {
            p.load(ScriptedDecoder.data('U', 4000), "audio/x-script");
            l.await("loaded", 1);
            Check.isTrue(Double.isInfinite(p.getDuration()), "the length is not known");
            Check.equal(0, p.getSeekable().size(), "nothing to seek in an unseekable source");
            p.play();
            l.await("ended", 1);
            Check.equal(1, l.count("duration"), "the length became known at the end");
            Check.near(0.5, l.knownDuration(), 1e-9, "the known length");
            Check.near(0.5, p.getDuration(), 1e-9, "the duration now");
            FakeOutputs.Out o = outs.await(0);
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(0, 4000), o.written()), "all frames");
            p.seek(0.25);
            l.await("seeked", 1);
            Check.near(0.25, p.getPosition(), 1e-9, "the seek target");
            p.play();
            l.await("ended", 2);
            FakeOutputs.Out o2 = outs.await(1);
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(2000, 4000), o2.written()), "the unseekable source was read again and skipped to frame 2000");
            p.setLoop(true);
            p.seek(0);
            l.await("seeked", 2);
            p.play();
            FakeOutputs.Out o3 = outs.await(2);
            o3.awaitWritten(4000 * 2 * 2 + 20);
            Check.isTrue(l.count("looped") >= 1, "looped by opening the data again");
            Check.isTrue(Arrays.equals(ScriptedDecoder.pcm(0, 4000), Arrays.copyOfRange(o3.written(), 8000, 16000)), "the second pass");
        } finally {
            p.close();
        }
    }

    /**
     * An owner that disappears: the player holds its listener weakly, so after the listener was collected the playback thread ends itself and
     * closes the output. The collection of garbage is not guaranteed, so a test that sees no collection is skipped with a reason.
     * <p>
     * 사라진 소유자: 재생기는 리스너를 약하게 잡으므로 리스너가 수거되면 재생 스레드가 스스로 끝나고 출력을 닫는다. 가비지 컬렉션은 보장되지 않으므로
     * 수거되지 않으면 이유를 적고 건너뛴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAbandonedPlayerEndsItself() throws Exception {
        FakeOutputs outs = new FakeOutputs(100);
        WeakReference<RecordingListener> ref = startAbandoned(outs);
        FakeOutputs.Out o = outs.await(0);
        long end = System.currentTimeMillis() + 15000;
        // 리스너가 수거되고 재생기가 스스로 출력을 닫을 때까지 가비지 컬렉션을 요청하며 기다린다.
        while (o.closeCalls() == 0 && System.currentTimeMillis() < end) {
            System.gc();
            Thread.sleep(50);
        }
        if (o.closeCalls() == 0) {
            Check.skip("the garbage collector did not collect the listener within 15 s (not guaranteed); listener collected: " + (ref.get() == null));
        }
        Check.equal(1, o.closeCalls(), "the abandoned player closed its output");
    }

    /**
     * Starts a player whose listener is not referenced by anybody else when the method returns.
     * <p>
     * 메서드가 돌아올 때 리스너를 참조하는 곳이 더 없는 재생기를 시작한다.
     *
     * @param outs the outputs / 출력
     * @return a weak reference to the listener / 리스너에 대한 약한 참조
     * @throws Exception on failure / 실패 시
     */
    private static WeakReference<RecordingListener> startAbandoned(FakeOutputs outs) throws Exception {
        RecordingListener l = new RecordingListener();
        MediaPlayer p = loaded(outs, l, wav(8000));
        p.play();
        outs.await(0).awaitQueued(100);
        return new WeakReference<RecordingListener>(l);
    }

    /**
     * The normalized time ranges merge overlapping and touching ranges, keep order, ignore empty ranges and test inclusively.
     * <p>
     * 정규화된 시간 범위는 겹치거나 맞닿는 범위를 합치고, 순서를 유지하고, 빈 범위를 무시하며, 끝점을 포함하여 판정한다.
     */
    public void testMediaRanges() {
        MediaRanges r = MediaRanges.EMPTY;
        Check.equal(0, r.size(), "empty");
        r = r.add(3, 4).add(1, 2);
        Check.equal("[1.0,2.0][3.0,4.0]", r.toString(), "ordered");
        Check.equal("[1.0,4.0]", r.add(2, 3).toString(), "touching ranges merge");
        Check.equal("[0.5,2.0][3.0,4.0]", r.add(0.5, 1.5).toString(), "overlap at the start");
        Check.equal("[0.0,10.0]", r.add(0, 10).toString(), "swallows everything");
        Check.equal("[1.0,2.0][3.0,4.0]", r.add(5, 5).toString(), "an empty range changes nothing");
        Check.equal("[1.0,2.0][3.0,4.0]", r.add(2.5, 2.4).toString(), "a reversed range changes nothing");
        Check.equal("[1.0,2.0][3.0,4.0]", r.add(1, Double.NaN).toString(), "a NaN range changes nothing");
        Check.isTrue(r.contains(2.0), "the end is included");
        Check.isTrue(r.contains(1.0), "the start is included");
        Check.isFalse(r.contains(2.5), "a gap");
        Check.near(3.0, r.start(1), 0, "start");
        Check.near(4.0, r.end(1), 0, "end");
        Check.equal("[0.0,1.0]", MediaRanges.of(0, 1).toString(), "of");
        Check.equal(0, MediaRanges.of(1, 1).size(), "an empty of");
    }
}
