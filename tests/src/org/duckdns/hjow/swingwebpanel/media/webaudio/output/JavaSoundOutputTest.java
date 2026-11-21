package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.io.IOException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the Java Sound output (plan item P25-07) against a fake sound system, because a sound device is not available on every machine
 * (the one that runs the tests has none): the conversion of floats to 16-bit little-endian PCM (rounding, clipping, NaN, the order of the
 * bytes), the format and the buffer size that are opened, the choice of another rate or channel count when the device does not play the
 * requested one and the conversion of the rate (a sine that is written at 44100 Hz arrives at 48000 Hz as the same sine), the write that
 * waits for the device when the line buffer is full and is woken by stop and close, and the reports of the queue in frames of the rate of the
 * data. The real sound system is tried only to show that a machine without a device fails with an <code>IOException</code> and that a machine
 * with one opens and closes a line without an error. That real sound comes out of a speaker is not checked by any test (plan item P22-10).
 * <p>
 * Java Sound 출력 시험(계획 P25-07)을 가짜 소리 시스템으로 한다. 소리 장치가 모든 컴퓨터에 있는 것은 아니기 때문이다(시험을 돌리는 컴퓨터에는 없다): float 을
 * 16비트 리틀 엔디언 PCM 으로 바꾸기(반올림, 자르기, NaN, 바이트 순서), 여는 형식과 버퍼 크기, 장치가 요청한 것을 재생하지 못할 때 다른 주파수나 채널 수를 고르기와
 * 주파수 변환(44100 Hz 로 쓴 사인이 48000 Hz 에서 같은 사인으로 도착한다), 라인 버퍼가 가득 차면 장치를 기다리고 정지와 닫기로 깨어나는 쓰기, 데이터 주파수의 프레임으로
 * 보고하는 큐. 실제 소리 시스템은 장치가 없는 컴퓨터가 <code>IOException</code> 으로 실패하고 장치가 있는 컴퓨터가 라인을 오류 없이 열고 닫는다는 것만 확인한다.
 * 실제 소리가 스피커로 나오는지는 어떤 시험도 확인하지 않는다(계획 P22-10).
 */
public class JavaSoundOutputTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public JavaSoundOutputTest() {
    }

    /**
     * Decodes 16-bit little-endian bytes of one channel layout into floats (value / 32768), channel by channel.
     * <p>
     * 16비트 리틀 엔디언 바이트를 float(값 / 32768)로 채널별로 풀어낸다.
     *
     * @param bytes    the bytes / 바이트
     * @param channels the number of channels / 채널 수
     * @return one array per channel / 채널마다 하나의 배열
     */
    static float[][] decode(byte[] bytes, int channels) {
        return FakeLine.decode16(bytes, channels);
    }

    /**
     * A float becomes the nearest 16-bit value: the common values, the limits (full scale clips to 32767), values beyond the range (clipped),
     * a NaN (silence), and half a step (a tie rounds up), and for every value from a seeded random sequence the error is at most half a step
     * of 1/32768 (the larger step of 1/32768 for the values that clip at the top).
     * <p>
     * float 는 가장 가까운 16비트 값이 된다: 흔한 값, 한계(풀 스케일은 32767 로 잘린다), 범위를 넘는 값(잘린다), NaN(무음), 반 단계의 값. 고정한 씨앗의 무작위 값마다
     * 오차는 최대 반 단계(1/32768 의 절반)이다(위에서 잘리는 값은 한 단계).
     */
    public void testToPcm16() {
        Check.equal(0L, JavaSoundOutput.toPcm16(0f), "zero");
        Check.equal(16384L, JavaSoundOutput.toPcm16(0.5f), "half");
        Check.equal(-16384L, JavaSoundOutput.toPcm16(-0.5f), "minus half");
        Check.equal(32767L, JavaSoundOutput.toPcm16(1f), "full scale clips to 32767");
        Check.equal(-32768L, JavaSoundOutput.toPcm16(-1f), "minus full scale");
        Check.equal(32767L, JavaSoundOutput.toPcm16(7f), "far above the range");
        Check.equal(-32768L, JavaSoundOutput.toPcm16(-7f), "far below the range");
        Check.equal(32767L, JavaSoundOutput.toPcm16(Float.POSITIVE_INFINITY), "infinity");
        Check.equal(-32768L, JavaSoundOutput.toPcm16(Float.NEGATIVE_INFINITY), "minus infinity");
        Check.equal(0L, JavaSoundOutput.toPcm16(Float.NaN), "NaN is silence");
        Check.equal(1L, JavaSoundOutput.toPcm16(1f / 32768f), "one step");
        Check.equal(0L, JavaSoundOutput.toPcm16(0.4f / 32768f), "less than half a step rounds down");
        Check.equal(1L, JavaSoundOutput.toPcm16(0.6f / 32768f), "more than half a step rounds up");
        Check.equal(-1L, JavaSoundOutput.toPcm16(-0.6f / 32768f), "more than half a step rounds down for negative values");
        java.util.Random random = new java.util.Random(20261122L);
        // 무작위 값마다 오차가 반 단계 이하인지(위로 잘리는 값은 한 단계 이하인지) 확인한다.
        for (int i = 0; i < 20000; i++) {
            float v = random.nextFloat() * 2f - 1f;
            double error = Math.abs(JavaSoundOutput.toPcm16(v) / 32768.0 - v);
            double bound = v > 32767.0 / 32768.0 ? 1.0 / 32768.0 : 0.5 / 32768.0 + 1e-9;
            Check.isTrue(error <= bound, "the error " + error + " of " + v + " is at most " + bound);
        }
    }

    /**
     * The bytes are interleaved frame by frame, channel by channel, low byte first.
     * <p>
     * 바이트는 프레임별로, 채널별로 낮은 바이트부터 인터리브된다.
     */
    public void testEncodeIsInterleavedLittleEndian() {
        float[][] src = {{0.5f, -1f}, {-0.5f, 1f}};
        byte[] out = new byte[8];
        JavaSoundOutput.encode16(src, 2, 2, out);
        byte[] expected = {0, 0x40, 0, (byte) 0xC0, 0, (byte) 0x80, (byte) 0xFF, 0x7F};
        // 바이트를 하나씩 기대값과 비교한다.
        for (int i = 0; i < 8; i++) {
            Check.equal((long) expected[i], (long) out[i], "byte " + i);
        }
    }

    /**
     * A device that plays the requested format is opened as asked: PCM_SIGNED, 16 bits, little endian, the rate and the channels, a buffer of
     * the requested number of frames, and the output reports what it is.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpensTheRequestedFormat() throws Exception {
        FakeLines lines = new FakeLines();
        JavaSoundOutput out = JavaSoundOutput.open(lines, 44100, 2, 2048);
        FakeLine line = lines.last();
        Check.notNull(line, "a line was opened");
        AudioFormat f = line.getFormat();
        Check.isTrue(AudioFormat.Encoding.PCM_SIGNED.equals(f.getEncoding()), "signed PCM");
        Check.equal(16L, f.getSampleSizeInBits(), "16 bits");
        Check.equal(2L, f.getChannels(), "channels");
        Check.isFalse(f.isBigEndian(), "little endian");
        Check.equal(44100L, (long) f.getSampleRate(), "rate");
        Check.equal(2048L * 4, line.getBufferSize(), "the buffer of 2048 frames of 4 bytes");
        Check.equal(2048L, out.getBufferFrames(), "capacity in frames");
        Check.equal(44100L, out.getSampleRate(), "sample rate of the data");
        Check.equal(44100L, out.getDeviceSampleRate(), "sample rate of the device");
        Check.equal(2L, out.getChannels(), "channels of the output");
        Check.isTrue(out.isAudible(), "a line is audible");
        Check.isFalse(line.isRunning(), "the output is created stopped");
        out.start();
        Check.isTrue(line.isRunning(), "started");
        out.stop();
        Check.isFalse(line.isRunning(), "stopped");
        out.close();
        Check.isFalse(line.isOpen(), "closing the output closes the line");
        out.close();
    }

    /**
     * A device that plays only 48000 Hz and mono is opened with that format, the output keeps the rate of the data (44100 Hz) and reports the
     * channels that were opened, and the buffer in frames of the data rate is still the requested size.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChoosesAnotherRateAndChannelCount() throws Exception {
        FakeLines lines = new FakeLines().onlyRates(48000).onlyChannels(1);
        JavaSoundOutput out = JavaSoundOutput.open(lines, 44100, 2, 4410);
        AudioFormat f = lines.last().getFormat();
        Check.equal(48000L, (long) f.getSampleRate(), "the device rate");
        Check.equal(1L, f.getChannels(), "the device channels");
        Check.equal(44100L, out.getSampleRate(), "the data rate");
        Check.equal(48000L, out.getDeviceSampleRate(), "the device rate");
        Check.equal(1L, out.getChannels(), "the channels that were opened");
        Check.equal(4800L * 2, lines.last().getBufferSize(), "the line buffer is 4800 device frames");
        Check.equal(4410L, out.getBufferFrames(), "the capacity in frames of the data rate");
        Check.contains(out.getDescription(), "48000", "the description names the device rate");
        Check.contains(out.getDescription(), "44100", "the description names the converted rate");
        out.close();
        // 같은 주파수에서 채널만 모자라면 주파수는 바꾸지 않는다.
        FakeLines stereoOnly = new FakeLines().onlyChannels(2);
        JavaSoundOutput six = JavaSoundOutput.open(stereoOnly, 44100, 6, 1000);
        Check.equal(2L, six.getChannels(), "6 channels were asked for, 2 were opened");
        Check.equal(44100L, six.getDeviceSampleRate(), "the rate was kept");
        six.close();
    }

    /**
     * Without a format that the system supports, and with a device that cannot be opened, opening fails with an <code>IOException</code> that
     * says why. The real sound system does the same on a machine without a mixer, and opens and closes a line on one with a mixer.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoDeviceFailsWithAnIOException() throws Exception {
        FakeLines nothing = new FakeLines().onlyRates(12345);
        try {
            JavaSoundOutput.open(nothing, 44100, 2, 1000);
            Check.fail("an output was opened without a supported format");
        } catch (IOException e) {
            Check.contains(e.getMessage(), "no audio output device", "the message");
        }
        FakeLines busy = new FakeLines();
        busy.setBusy(true);
        try {
            JavaSoundOutput.open(busy, 44100, 2, 1000);
            Check.fail("an output was opened on a busy device");
        } catch (IOException e) {
            Check.contains(e.getMessage(), "not available", "the message of a busy device");
        }
        // 실제 소리 시스템: 믹서가 없으면 IOException, 있으면 열고 닫을 수 있어야 한다.
        if (AudioSystem.getMixerInfo().length == 0) {
            try {
                new SystemOutputFactory().open(44100, 2, 2205);
                Check.fail("a real line was opened on a machine without a mixer");
            } catch (IOException e) {
                Check.contains(e.getMessage(), "no audio output device", "the message of a machine without a mixer");
            }
        } else {
            try {
                AudioOutput real = new SystemOutputFactory().open(44100, 2, 2205);
                real.close();
            } catch (IOException e) {
                Check.skip("the sound device cannot be opened here: " + e.getMessage());
            }
        }
    }

    /**
     * A sine of 1000 Hz and the amplitude 0.5 that is written at 44100 Hz to a device that plays only 48000 Hz arrives as the same sine:
     * a least squares fit of a sine of 1000 Hz to the frames in the middle leaves a residual of less than 0.002 (the 16-bit step is 3e-5 and
     * the stop band of the converter is at -100 dB), and the amplitude of the fit is 0.5 within 0.002. The number of frames is that of the
     * 48000 Hz rate for the length of the audio, and the writes of the pieces of 441 frames are joined without a seam (the memory of the
     * converter continues from piece to piece).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testConvertedSineArrivesAtTheDeviceRate() throws Exception {
        FakeLines lines = new FakeLines().onlyRates(48000).onlyChannels(1);
        JavaSoundOutput out = JavaSoundOutput.open(lines, 44100, 1, 44100);
        int pieces = 50;
        int piece = 441;
        float[][] data = new float[1][piece];
        // 441 프레임(10 ms)씩 사인을 이어서 쓴다.
        for (int p = 0; p < pieces; p++) {
            for (int i = 0; i < piece; i++) {
                int n = p * piece + i;
                data[0][i] = (float) (0.5 * Math.sin(2 * Math.PI * 1000.0 * n / 44100.0));
            }
            out.write(data, piece);
        }
        float[][] heard = decode(lines.last().getRecorded(), 1);
        int frames = heard[0].length;
        // 변환기는 선형 위상 필터라 처음 프레임들이 입력을 더 기다린다. 그 지연(약 64 프레임)만큼 모자라게 나온다.
        long lag = Math.round(out.getConversionDelayFrames() * 48000.0 / 44100.0);
        Check.isTrue(out.getConversionDelayFrames() > 0 && out.getConversionDelayFrames() < 200, "the delay of the converter: " + out.getConversionDelayFrames());
        Check.isTrue(frames <= 24000 + 8 && frames >= 24000 - lag - 8, "0.5 s at 48000 Hz is 24000 frames minus the delay of " + lag + ": " + frames);
        // 가운데 구간(앞뒤 경계의 필터 효과를 피한다)에서 1000 Hz 사인의 최소 제곱 맞춤을 구한다.
        int from = 2000;
        int to = frames - 2000;
        double cc = 0;
        double ss = 0;
        double cs = 0;
        double yc = 0;
        double ys = 0;
        // 구간의 모든 프레임에서 정규 방정식의 합을 구한다.
        for (int n = from; n < to; n++) {
            double w = 2 * Math.PI * 1000.0 * n / 48000.0;
            double c = Math.cos(w);
            double s = Math.sin(w);
            cc += c * c;
            ss += s * s;
            cs += c * s;
            yc += heard[0][n] * c;
            ys += heard[0][n] * s;
        }
        double det = cc * ss - cs * cs;
        double a = (yc * ss - ys * cs) / det;
        double b = (ys * cc - yc * cs) / det;
        double residual = 0;
        // 맞춤을 뺀 나머지의 제곱 평균 제곱근을 구한다.
        for (int n = from; n < to; n++) {
            double w = 2 * Math.PI * 1000.0 * n / 48000.0;
            double e = heard[0][n] - (a * Math.cos(w) + b * Math.sin(w));
            residual += e * e;
        }
        residual = Math.sqrt(residual / (to - from));
        Check.near(0.5, Math.sqrt(a * a + b * b), 0.002, "the amplitude of the converted sine");
        Check.isTrue(residual < 0.002, "the residual of the fit is " + residual);
        out.close();
    }

    /**
     * The queue is reported in frames of the data rate: 441 frames at 44100 Hz that are written to a 48000 Hz device wait there as 480
     * frames and are reported as 441 (within one frame), a flush empties the queue, and a closed output reports 0.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQueuedFramesAreInTheDataRate() throws Exception {
        FakeLines lines = new FakeLines().onlyRates(48000).onlyChannels(1);
        JavaSoundOutput out = JavaSoundOutput.open(lines, 44100, 1, 4410);
        out.write(new float[1][441], 441);
        long queued = out.getQueuedFrames();
        // 변환기의 지연만큼 덜 나오므로 441 프레임을 쓰면 그 지연을 뺀 만큼이 큐에 보인다.
        long expected = 441 - out.getConversionDelayFrames();
        Check.isTrue(Math.abs(queued - expected) <= 2, "441 data frames minus the delay are reported as " + queued + ", expected about " + expected);
        out.flush();
        Check.equal(0L, out.getQueuedFrames(), "a flushed output holds nothing");
        Check.equal(1L, lines.last().getFlushCount(), "the line was flushed once by flush");
        out.close();
        Check.equal(0L, out.getQueuedFrames(), "a closed output holds nothing");
        Check.equal(2L, lines.last().getFlushCount(), "closing flushes the line once more (the sound stops at once)");
    }

    /**
     * A write waits for the device: a line buffer of 1000 frames and a write of 3000 frames that is started leave the writer blocked with
     * exactly 1000 frames in the line, and as the test plays frames (a few at a time) the writer goes on until all 3000 frames arrived
     * in order, without a gap or a repeat and without writing more than the free room (the fake line fails such a write).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWriteWaitsForTheDeviceAndKeepsTheOrder() throws Exception {
        final FakeLines lines = new FakeLines();
        final JavaSoundOutput out = JavaSoundOutput.open(lines, 8000, 1, 1000);
        out.start();
        final float[][] data = new float[1][3000];
        // 1/4000 씩 늘어나는 램프라서 순서가 바뀌면 값이 어긋난다.
        for (int i = 0; i < 3000; i++) {
            data[0][i] = i / 4000f;
        }
        final Throwable[] failure = new Throwable[1];
        Thread writer = new Thread(new Runnable() {
            public void run() {
                try {
                    out.write(data, 3000);
                } catch (Throwable t) {
                    failure[0] = t;
                }
            }
        });
        writer.setDaemon(true);
        writer.start();
        Thread.sleep(200);
        Check.isTrue(writer.isAlive(), "the writer waits for the device");
        Check.equal(2000L, (long) lines.last().getRecorded().length, "exactly the line buffer (1000 frames) was written");
        long end = System.currentTimeMillis() + 20000;
        // 장치가 몇 프레임씩 재생하는 것을 흉내 내며 쓰는 쪽이 끝날 때까지 기다린다.
        while (writer.isAlive() && System.currentTimeMillis() < end) {
            lines.last().consume(300);
            Thread.sleep(3);
        }
        Check.isFalse(writer.isAlive(), "the writer finished");
        Check.isNull(failure[0], "the writer failed: " + failure[0]);
        float[][] heard = decode(lines.last().getRecorded(), 1);
        Check.equal(3000L, (long) heard[0].length, "all frames arrived");
        // 모든 프레임이 순서대로, 16비트 반올림 오차 안에서 도착했는지 확인한다.
        for (int i = 0; i < 3000; i++) {
            if (Math.abs(heard[0][i] - data[0][i]) > 0.5 / 32768 + 1e-9) {
                Check.fail("frame " + i + " is " + heard[0][i] + " instead of " + data[0][i]);
            }
        }
        out.close();
    }

    /**
     * A stopped output that is full takes what fits and drops the rest at once; a writer that waits is woken by stop and by close (the
     * context stops an output to end its rendering thread).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStoppedOutputDropsAndStopOrCloseWakeAWriter() throws Exception {
        FakeLines lines = new FakeLines();
        JavaSoundOutput stopped = JavaSoundOutput.open(lines, 8000, 1, 100);
        stopped.write(new float[1][300], 300);
        Check.equal(200L, (long) lines.last().getRecorded().length, "a stopped output took the 100 frames that fit");
        stopped.close();
        // 정지와 닫기 두 방법으로 기다리는 쓰는 쪽이 깨어나는지 확인한다.
        for (int round = 0; round < 2; round++) {
            FakeLines fl = new FakeLines();
            final JavaSoundOutput out = JavaSoundOutput.open(fl, 8000, 1, 100);
            out.start();
            Thread writer = new Thread(new Runnable() {
                public void run() {
                    out.write(new float[1][10000], 10000);
                }
            });
            writer.setDaemon(true);
            writer.start();
            Thread.sleep(150);
            Check.isTrue(writer.isAlive(), "the writer waits (round " + round + ")");
            if (round == 0) {
                out.stop();
            } else {
                out.close();
            }
            writer.join(3000);
            Check.isFalse(writer.isAlive(), "the writer was woken (round " + round + ")");
            out.close();
        }
    }

    /**
     * Writing to a closed output does nothing, and closing twice is harmless.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClosedOutputIgnoresWrites() throws Exception {
        FakeLines lines = new FakeLines();
        JavaSoundOutput out = JavaSoundOutput.open(lines, 8000, 1, 100);
        out.close();
        out.write(new float[1][50], 50);
        Check.equal(0L, (long) lines.last().getRecorded().length, "nothing was written after the close");
        out.start();
        out.stop();
        out.flush();
        out.close();
        Check.isFalse(lines.last().isOpen(), "closed");
    }
}
