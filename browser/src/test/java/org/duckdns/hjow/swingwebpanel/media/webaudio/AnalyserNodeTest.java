package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the analyser node (Web Audio API 1.8, plan item P25-04): the defaults and the errors of the attributes; the time-domain
 * data against a ramp and against the stereo down-mix; the byte conversion of the time domain (<code>floor(128 (1 + x))</code> with the
 * clipping); the frequency data of a sine that lies exactly on a bin against the closed form of the Blackman window (the bins
 * <code>k0</code>, <code>k0 +- 1</code>, <code>k0 +- 2</code> have the magnitudes <code>a0 A / 2</code>, <code>a1 A / 4</code>,
 * <code>a2 A / 4</code> with <code>1 / N</code> scaling, all other bins are zero); the smoothing over time (the magnitude after
 * <code>m</code> analyses of a stationary signal is <code>(1 - tau^m)</code> of the unsmoothed one), the single analysis within one
 * render quantum, the reset by <code>fftSize</code>; the byte frequency data against the formula of the specification; the pass-through
 * of the audio; the processing of an analyser whose output is not connected; the flush of the history after the input ended; and reads
 * from another thread while the context renders.
 * <p>
 * 분석기 노드 시험(Web Audio API 1.8, 계획 P25-04): 속성의 기본값과 오류, 시간 영역 데이터를 램프와 스테레오 다운믹스에 대해, 시간 영역의 바이트 변환
 * (<code>floor(128 (1 + x))</code> 과 자르기), 정확히 빈 위에 있는 사인의 주파수 데이터를 Blackman 창의 닫힌 식과 비교(빈 <code>k0</code>,
 * <code>k0 +- 1</code>, <code>k0 +- 2</code> 의 크기가 <code>1 / N</code> 비례에서 <code>a0 A / 2</code>, <code>a1 A / 4</code>, <code>a2 A / 4</code> 이고 나머지
 * 빈은 0), 시간에 따른 평활(정상 신호를 <code>m</code> 번 분석한 크기는 평활 없는 값의 <code>(1 - tau^m)</code> 배), 한 render quantum 안에서 분석을 한
 * 번만 하는 것, <code>fftSize</code> 로 인한 초기화, 바이트 주파수 데이터를 규격의 식과 비교, 오디오 통과, 출력이 연결되지 않은 분석기의 처리, 입력이 끝난 뒤
 * 이력이 비워지는 것, 컨텍스트가 렌더링하는 동안 다른 스레드에서 읽는 것을 확인한다.
 */
public class AnalyserNodeTest {

    /**
     * The sample rate of the contexts of the tests.
     * <p>
     * 시험 컨텍스트의 샘플링 주파수.
     */
    private static final float RATE = 8000f;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public AnalyserNodeTest() {
    }

    /**
     * Creates a context with a buffer source of a mono signal that plays at once and an analyser that is connected to it (and not
     * connected to the destination).
     * <p>
     * 모노 신호를 바로 재생하는 버퍼 원천과 거기에 연결된 분석기(목적지에는 연결하지 않음)가 있는 컨텍스트를 만든다.
     *
     * @param signal the samples / 샘플
     * @param frames the length of the context in frames / 컨텍스트의 길이(프레임)
     * @return the analyser (its context is reachable through <code>getContext()</code>, which is an offline context) / 분석기
     */
    private static AnalyserNode analyserOf(float[] signal, int frames) {
        OfflineAudioContext ctx = new OfflineAudioContext(1, frames, RATE);
        AudioBuffer b = new AudioBuffer(1, signal.length, RATE);
        System.arraycopy(signal, 0, b.getChannelData(0), 0, signal.length);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(b);
        AnalyserNode an = ctx.createAnalyser();
        src.connect(an);
        src.start(0);
        return an;
    }

    /**
     * Renders an offline context to its end.
     * <p>
     * 오프라인 컨텍스트를 끝까지 렌더링한다.
     *
     * @param an the analyser of the context / 컨텍스트의 분석기
     */
    private static void finish(AnalyserNode an) {
        ((OfflineAudioContext) an.getContext()).startRendering();
    }

    /**
     * The defaults, the derived <code>frequencyBinCount</code> and the errors of the attributes: <code>fftSize</code> must be a power of
     * two from 32 to 32768; <code>minDecibels</code> must stay below <code>maxDecibels</code>; the smoothing constant lies in 0..1.
     * <p>
     * 기본값, 거기서 나오는 <code>frequencyBinCount</code>, 속성의 오류: <code>fftSize</code> 는 32 ~ 32768 의 2 의 거듭제곱이어야 하고,
     * <code>minDecibels</code> 는 <code>maxDecibels</code> 보다 작아야 하며, 평활 상수는 0~1 이다.
     */
    public void testDefaultsAndErrors() {
        OfflineAudioContext ctx = WebAudioTestKit.mono(256);
        final AnalyserNode an = ctx.createAnalyser();
        Check.equal(2048L, (long) an.getFftSize(), "fftSize");
        Check.equal(1024L, (long) an.getFrequencyBinCount(), "frequencyBinCount");
        Check.near(-100.0, an.getMinDecibels(), 0.0, "minDecibels");
        Check.near(-30.0, an.getMaxDecibels(), 0.0, "maxDecibels");
        Check.near(0.8, an.getSmoothingTimeConstant(), 0.0, "smoothingTimeConstant");
        Check.equal(2L, (long) an.getChannelCount(), "channelCount");
        Check.same(ChannelCountMode.MAX, an.getChannelCountMode(), "channelCountMode");
        Check.same(ChannelInterpretation.SPEAKERS, an.getChannelInterpretation(), "channelInterpretation");
        int[] bad = {0, 1, 16, 31, 33, 100, 1000, 65536, -32};
        // 잘못된 fftSize 는 IndexSizeError 이다.
        for (int i = 0; i < bad.length; i++) {
            final int size = bad[i];
            WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
                public void run() {
                    an.setFftSize(size);
                }
            }, "fftSize " + size);
        }
        // 32 ~ 32768 의 모든 2 의 거듭제곱은 허용된다.
        for (int size = 32; size <= 32768; size *= 2) {
            an.setFftSize(size);
            Check.equal((long) size / 2, (long) an.getFrequencyBinCount(), "frequencyBinCount of " + size);
        }
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                an.setMaxDecibels(-100.0);
            }
        }, "maxDecibels equal to minDecibels");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                an.setMinDecibels(-30.0);
            }
        }, "minDecibels equal to maxDecibels");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                an.setSmoothingTimeConstant(1.01);
            }
        }, "smoothing above 1");
        WebAudioTestKit.assertThrows(AudioException.Kind.INDEX_SIZE, new Runnable() {
            public void run() {
                an.setSmoothingTimeConstant(-0.01);
            }
        }, "smoothing below 0");
        an.setSmoothingTimeConstant(0.0);
        an.setSmoothingTimeConstant(1.0);
        an.setMaxDecibels(0.0);
        an.setMinDecibels(-10.0);
        Check.near(-10.0, an.getMinDecibels(), 0.0, "minDecibels is set");
    }

    /**
     * The time-domain data are the most recent <code>fftSize</code> frames, oldest first: a ramp <code>i / 1000</code> that has played
     * to the end of the rendering shows the last 64 values; the arrays that are shorter or longer than <code>fftSize</code> get as many
     * values as they hold or leave the rest alone.
     * <p>
     * 시간 영역 데이터는 가장 최근 <code>fftSize</code> 프레임이며 오래된 것부터이다. 렌더링 끝까지 재생된 램프 <code>i / 1000</code> 은 마지막 64 개 값을 보여
     * 준다. <code>fftSize</code> 보다 짧거나 긴 배열은 담을 수 있는 만큼만 받고 나머지는 건드리지 않는다.
     */
    public void testTimeDomainDataIsTheRecentRamp() {
        float[] ramp = new float[1024];
        // 값이 i / 1000 인 램프를 만든다.
        for (int i = 0; i < 1024; i++) {
            ramp[i] = i / 1000f;
        }
        AnalyserNode an = analyserOf(ramp, 1024);
        an.setFftSize(64);
        float[] before = new float[64];
        an.getFloatTimeDomainData(before);
        // 렌더링 전에는 이력이 무음이다.
        for (int i = 0; i < 64; i++) {
            Check.near(0.0, before[i], 0.0, "silent history " + i);
        }
        finish(an);
        float[] data = new float[64];
        an.getFloatTimeDomainData(data);
        // 마지막 64 개 값(960 ~ 1023)을 오래된 것부터 본다.
        for (int i = 0; i < 64; i++) {
            Check.near((960 + i) / 1000.0, data[i], 1e-7, "value " + i);
        }
        float[] longer = new float[80];
        java.util.Arrays.fill(longer, 7f);
        an.getFloatTimeDomainData(longer);
        Check.near(7.0, longer[64], 0.0, "the elements beyond fftSize are left alone");
        Check.near(1.023 - 0.001 * 63, longer[0], 1e-7, "the first element of a longer array");
        float[] shorter = new float[10];
        an.getFloatTimeDomainData(shorter);
        Check.near(0.960, shorter[0], 1e-7, "a shorter array gets the oldest values");
        Check.near(0.969, shorter[9], 1e-7, "a shorter array holds 10 values");
    }

    /**
     * With the default settings a stereo input of 0.5 and -0.25 is down-mixed to mono with the speakers rule for the analysis
     * (<code>(0.5 - 0.25) / 2 = 0.125</code>) while the audio passes to the output unchanged, with both channels (checked at the
     * destination). The channel settings of the node apply before the analysis (as in Chromium): with the mode <code>explicit</code>,
     * one channel and the interpretation <code>discrete</code> the input is first reduced to its first channel, so the analysis sees 0.5.
     * <p>
     * 기본 설정에서 0.5 와 -0.25 의 스테레오 입력은 분석을 위해 speakers 규칙으로 모노로 다운믹스되고(<code>(0.5 - 0.25) / 2 = 0.125</code>) 오디오는 두 채널
     * 그대로 출력으로 지나간다(목적지에서 확인). 노드의 채널 설정은 분석에 앞서 적용된다(Chromium 과 같다). 모드 <code>explicit</code>, 채널 1개, 해석
     * <code>discrete</code> 이면 입력이 먼저 첫 채널로 줄어들므로 분석은 0.5 를 본다.
     */
    public void testStereoDownMixAndPassThrough() {
        int[] modes = {0, 1};
        // 기본 설정과 explicit/discrete 설정에서 분석 결과와 출력을 확인한다.
        for (int m = 0; m < modes.length; m++) {
            OfflineAudioContext ctx = new OfflineAudioContext(2, 512, RATE);
            AudioBuffer in = new AudioBuffer(2, 512, RATE);
            java.util.Arrays.fill(in.getChannelData(0), 0.5f);
            java.util.Arrays.fill(in.getChannelData(1), -0.25f);
            AudioBufferSourceNode src = ctx.createBufferSource();
            src.setBuffer(in);
            AnalyserNode an = ctx.createAnalyser();
            if (m == 1) {
                an.setChannelCountMode(ChannelCountMode.EXPLICIT);
                an.setChannelCount(1);
                an.setChannelInterpretation(ChannelInterpretation.DISCRETE);
            }
            src.connect(an);
            an.connect(ctx.getDestination());
            src.start(0);
            AudioBuffer out = ctx.startRendering();
            float[] data = new float[2048];
            an.setFftSize(32);
            an.getFloatTimeDomainData(data);
            double expected = m == 0 ? 0.125 : 0.5;
            // 이력의 값이 기대값인지 본다.
            for (int i = 0; i < 32; i++) {
                Check.near(expected, data[i], 1e-7, "mode " + m + " down-mixed value " + i);
            }
            Check.near(0.5, out.getChannelData(0)[100], 1e-6, "mode " + m + ": the first channel passes to the output");
            Check.near(m == 0 ? -0.25 : 0.5, out.getChannelData(1)[100], 1e-6, "mode " + m + ": the second channel at the output");
        }
    }

    /**
     * The byte time-domain data: <code>floor(128 (1 + x))</code> clipped to 0..255, for constant inputs -2, -1, -0.5, 0, 0.5, 0.999,
     * 1 and 1.5 (a byte is unsigned).
     * <p>
     * 바이트 시간 영역 데이터: 일정한 입력 -2, -1, -0.5, 0, 0.5, 0.999, 1, 1.5 에서 <code>floor(128 (1 + x))</code> 을 0~255 로 자른 값(바이트는 부호 없음).
     */
    public void testByteTimeDomainData() {
        double[] xs = {-2, -1, -0.5, 0, 0.5, 0.999, 1, 1.5};
        // 입력 값마다 규격의 식과 비교한다.
        for (int i = 0; i < xs.length; i++) {
            float[] signal = new float[256];
            java.util.Arrays.fill(signal, (float) xs[i]);
            AnalyserNode an = analyserOf(signal, 256);
            an.setFftSize(32);
            finish(an);
            byte[] bytes = new byte[40];
            an.getByteTimeDomainData(bytes);
            double expected = Math.floor(128 * (1 + (float) xs[i]));
            expected = Math.max(0, Math.min(255, expected));
            Check.equal((long) expected, (long) (bytes[0] & 0xFF), "byte for " + xs[i]);
            Check.equal((long) expected, (long) (bytes[31] & 0xFF), "last byte for " + xs[i]);
            Check.equal(0L, (long) bytes[32], "the elements beyond fftSize are left alone");
        }
    }

    /**
     * Creates a sine of an amplitude that lies exactly on a bin of an FFT size, <code>k0 / N</code> cycles per frame.
     * <p>
     * 어떤 FFT 크기의 빈 위에 정확히 놓이는 진폭의 사인을 만든다. 프레임당 <code>k0 / N</code> 주기이다.
     *
     * @param frames    the length / 길이
     * @param k0        the bin / 빈
     * @param fftSize   the FFT size / FFT 크기
     * @param amplitude the amplitude / 진폭
     * @return the samples / 샘플
     */
    private static float[] sine(int frames, int k0, int fftSize, double amplitude) {
        float[] s = new float[frames];
        // 샘플마다 사인 값을 계산한다.
        for (int i = 0; i < frames; i++) {
            s[i] = (float) (amplitude * Math.sin(2 * Math.PI * k0 * (double) (i % fftSize) / fftSize));
        }
        return s;
    }

    /**
     * The frequency data of a sine on the bin <code>k0 = 100</code> of an FFT of 2048 points with the amplitude 0.5 and no smoothing
     * follow the closed form of the Blackman window: <code>a0 A / 2 = 0.105</code> at <code>k0</code>, <code>a1 A / 4 = 0.0625</code> at
     * <code>k0 +- 1</code>, <code>a2 A / 4 = 0.01</code> at <code>k0 +- 2</code>, and nothing elsewhere.
     * <p>
     * 2048 점 FFT 의 빈 <code>k0 = 100</code> 위에 있는 진폭 0.5 의 사인의 주파수 데이터는 평활이 없을 때 Blackman 창의 닫힌 식을 따른다.
     * <code>k0</code> 에서 <code>a0 A / 2 = 0.105</code>, <code>k0 +- 1</code> 에서 <code>a1 A / 4 = 0.0625</code>, <code>k0 +- 2</code> 에서
     * <code>a2 A / 4 = 0.01</code> 이고 그 밖에는 없다.
     */
    public void testFrequencyDataOfASine() {
        int n = 2048;
        AnalyserNode an = analyserOf(sine(8192, 100, n, 0.5), 8192);
        an.setSmoothingTimeConstant(0.0);
        finish(an);
        float[] y = new float[1024];
        an.getFloatFrequencyData(y);
        double a0 = 0.42;
        double a1 = 0.5;
        double a2 = 0.08;
        Check.near(db(a0 * 0.5 / 2), y[100], 0.01, "the bin of the sine");
        Check.near(db(a1 * 0.5 / 4), y[99], 0.01, "the bin below");
        Check.near(db(a1 * 0.5 / 4), y[101], 0.01, "the bin above");
        Check.near(db(a2 * 0.5 / 4), y[98], 0.02, "two bins below");
        Check.near(db(a2 * 0.5 / 4), y[102], 0.02, "two bins above");
        // 그 밖의 빈은 부동소수점 잡음 수준(-110 dB 미만)이다.
        for (int k = 0; k < 1024; k++) {
            if (k < 98 || k > 102) {
                Check.isTrue(y[k] < -110.0, "bin " + k + " is " + y[k] + " dB");
            }
        }
        float[] longer = new float[1100];
        java.util.Arrays.fill(longer, 5f);
        an.getFloatFrequencyData(longer);
        Check.near(5.0, longer[1024], 0.0, "the elements beyond frequencyBinCount are left alone");
    }

    /**
     * Converts a linear magnitude to decibels.
     * <p>
     * 선형 크기를 데시벨로 바꾼다.
     *
     * @param x the magnitude / 크기
     * @return the decibels / 데시벨
     */
    private static double db(double x) {
        return 20 * Math.log10(x);
    }

    /**
     * The smoothing over time with the default constant 0.8: while the context renders quantum by quantum and the analyser is read
     * after each quantum, the magnitude of the stationary sine after <code>m</code> readings is <code>(1 - 0.8^m)</code> times the
     * unsmoothed magnitude; several reads within one quantum return the same data and smooth once; changing <code>fftSize</code>
     * starts the smoothing again.
     * <p>
     * 기본 상수 0.8 의 시간 평활: 컨텍스트가 quantum 단위로 렌더링하고 quantum 마다 분석기를 읽으면 정상 사인의 크기는 <code>m</code> 번째 읽기에서 평활 없는
     * 크기의 <code>(1 - 0.8^m)</code> 배이다. 한 quantum 안의 여러 번 읽기는 같은 데이터를 돌려주고 평활도 한 번만 하며, <code>fftSize</code> 를 바꾸면
     * 평활이 다시 시작된다.
     */
    public void testSmoothingOverTime() {
        int n = 2048;
        AnalyserNode an = analyserOf(sine(8192, 100, n, 0.5), 8192);
        OfflineAudioContext ctx = (OfflineAudioContext) an.getContext();
        // 창이 사인으로 가득 찰 때까지(2048 프레임 = 16 quantum 보다 더) 렌더링한다.
        for (int q = 0; q < 20; q++) {
            ctx.renderNextQuantum();
        }
        double full = 0.42 * 0.5 / 2;
        float[] y = new float[1024];
        float[] again = new float[1024];
        // quantum 마다 한 번씩 읽으면서 평활된 크기를 닫힌 식과 비교한다.
        for (int m = 1; m <= 6; m++) {
            an.getFloatFrequencyData(y);
            an.getFloatFrequencyData(again);
            Check.near(db((1 - Math.pow(0.8, m)) * full), y[100], 0.01, "after " + m + " readings");
            Check.near(y[100], again[100], 0.0, "a second read in the same quantum returns the same data");
            ctx.renderNextQuantum();
        }
        an.setFftSize(2048);
        an.getFloatFrequencyData(y);
        Check.near(db((1 - Math.pow(0.8, 7)) * full), y[100], 0.01, "setting the same size keeps the smoothing state");
        an.setFftSize(1024);
        an.setFftSize(2048);
        ctx.renderNextQuantum();
        an.getFloatFrequencyData(y);
        Check.near(db(0.2 * full), y[100], 0.01, "changing fftSize resets the smoothing: one reading gives 1 - 0.8");
    }

    /**
     * The byte frequency data follow the formula of the specification: <code>floor(255 (Y - min) / (max - min))</code> clipped to
     * 0..255, for a range of -60 to 0 dB: the bin of the sine (-19.58 dB) gives 171, the bin above it (-24.08 dB) gives 150, the
     * silent bins give 0.
     * <p>
     * 바이트 주파수 데이터는 규격의 식 <code>floor(255 (Y - min) / (max - min))</code> 을 0~255 로 자른 값을 따른다. 범위 -60 ~ 0 dB 에서 사인의 빈(-19.58 dB)은 171,
     * 그 위의 빈(-24.08 dB)은 150, 무음 빈은 0 이다.
     */
    public void testByteFrequencyData() {
        int n = 2048;
        AnalyserNode an = analyserOf(sine(8192, 100, n, 0.5), 8192);
        an.setSmoothingTimeConstant(0.0);
        an.setMinDecibels(-60.0);
        an.setMaxDecibels(0.0);
        finish(an);
        byte[] b = new byte[1024];
        an.getByteFrequencyData(b);
        double[] mags = {0.42 * 0.5 / 2, 0.5 * 0.5 / 4};
        int[] bins = {100, 101};
        // 사인의 빈과 이웃 빈을 규격의 식과 비교한다.
        for (int i = 0; i < bins.length; i++) {
            double y = db(mags[i]);
            long expected = (long) Math.floor(255.0 / 60.0 * (y + 60.0));
            Check.equal(expected, (long) (b[bins[i]] & 0xFF), "byte of bin " + bins[i]);
        }
        Check.equal(0L, (long) (b[500] & 0xFF), "a silent bin clips to 0");
        an.setMinDecibels(-200.0);
        an.setMaxDecibels(-30.0);
        an.setMaxDecibels(-20.0);
        an.getByteFrequencyData(b);
        Check.isTrue((b[100] & 0xFF) > 150, "a wider range keeps the loud bin high");
        an.setMinDecibels(-40.0);
        an.setMaxDecibels(-30.0);
        an.getByteFrequencyData(b);
        Check.equal(255L, (long) (b[100] & 0xFF), "a bin above the maximum clips to 255");
    }

    /**
     * An analyser whose output is not connected is processed while a started source feeds it, and after the source ended the history is
     * flushed with silence within the tail of the window, after which the node is released by the context.
     * <p>
     * 출력이 연결되지 않은 분석기도 시작한 원천이 먹이는 동안 처리되고, 원천이 끝난 뒤에는 창의 꼬리 안에 이력이 무음으로 채워지며 그 뒤에 컨텍스트가 노드를
     * 놓아 준다.
     */
    public void testFlushAfterTheInputEnded() {
        float[] signal = new float[1024];
        java.util.Arrays.fill(signal, 0.5f);
        AnalyserNode an = analyserOf(signal, 4096);
        an.setFftSize(256);
        OfflineAudioContext ctx = (OfflineAudioContext) an.getContext();
        // 원천이 재생되는 1024 프레임까지 렌더링한다.
        while (ctx.getRenderedFrames() < 1024) {
            ctx.renderNextQuantum();
        }
        float[] data = new float[256];
        an.getFloatTimeDomainData(data);
        Check.near(0.5, data[255], 1e-7, "the unconnected analyser sees the source");
        // 컨텍스트 끝까지 렌더링한다.
        while (!ctx.isComplete()) {
            ctx.renderNextQuantum();
        }
        an.getFloatTimeDomainData(data);
        // 입력이 끝나고 창만큼 지나면 이력은 모두 무음이다.
        for (int i = 0; i < 256; i++) {
            Check.near(0.0, data[i], 0.0, "flushed " + i);
        }
        ctx.finishRendering();
        Check.equal(0L, (long) ctx.getPinnedNodeCount(), "the analyser is released after the tail");
    }

    /**
     * A thread that reads the time-domain data while another thread renders always sees a consistent history: a ramp that rises by
     * 0.001 per frame shows steps of 0 (before the first frame arrived) or 0.001 and never a torn mixture of two quanta.
     * <p>
     * 다른 스레드가 렌더링하는 동안 시간 영역 데이터를 읽는 스레드는 항상 일관된 이력을 본다. 프레임당 0.001 씩 올라가는 램프는 0(첫 프레임이 도착하기 전)이나
     * 0.001 의 간격만 보이고 두 quantum 이 찢어져 섞인 모습은 보이지 않는다.
     */
    public void testReadingWhileRenderingInAnotherThread() throws Exception {
        float[] ramp = new float[40960];
        // 값이 i / 1000 인 긴 램프를 만든다.
        for (int i = 0; i < ramp.length; i++) {
            ramp[i] = i / 1000f;
        }
        final AnalyserNode an = analyserOf(ramp, 40960);
        an.setFftSize(512);
        final OfflineAudioContext ctx = (OfflineAudioContext) an.getContext();
        final boolean[] done = new boolean[1];
        Thread renderer = new Thread(new Runnable() {
            public void run() {
                ctx.startRendering();
                done[0] = true;
            }
        });
        renderer.start();
        float[] data = new float[512];
        int reads = 0;
        // 렌더링이 끝날 때까지(또는 충분히 읽을 때까지) 계속 읽으며 일관성을 확인한다.
        while (!done[0] || reads < 50) {
            an.getFloatTimeDomainData(data);
            // 연속한 두 값의 차이는 0 또는 0.001 이어야 한다.
            for (int i = 1; i < data.length; i++) {
                double d = data[i] - data[i - 1];
                Check.isTrue(Math.abs(d) < 1e-6 || Math.abs(d - 0.001) < 1e-5, "step " + d + " at " + i + " in read " + reads);
            }
            reads++;
            if (reads > 200000) {
                break;
            }
        }
        renderer.join();
        Check.isTrue(reads >= 50, "the history was read many times: " + reads);
    }
}
