package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the sample rate converter (plan item P25-05). Every expectation is computed by the test from a closed form: sine waves of known
 * frequency are converted and compared with the same sine evaluated at the output times, gains are measured by a least squares fit of a
 * sinusoid of the known frequency (which has no spectral leakage, unlike a single bin of a Fourier transform), the pass band, the
 * transition band and the stop band are measured with tones at chosen fractions of the lower Nyquist frequency, and the length is checked
 * against the exact ceiling of the ratio computed here in integers. The tolerances come from the design of the kernel (100 dB, about
 * 10<sup>-5</sup> of the amplitude; see {@link Resampler}) and from what the implementation measured with a margin of about two.
 * <p>
 * 표본율 변환기의 시험(계획 P25-05). 모든 기대값은 시험이 닫힌 식으로 계산한다. 주파수를 아는 사인파를 변환해 출력 시각에서 계산한 같은 사인파와 비교하고,
 * 이득은 주파수를 아는 정현파의 최소 제곱 적합으로 잰다(푸리에 변환의 한 구간과 달리 스펙트럼 누설이 없다). 통과 대역, 전이 대역, 정지 대역은 낮은 쪽
 * 나이퀴스트 주파수의 정해진 비율에 있는 톤으로 재고, 길이는 여기서 정수로 계산한 비율의 정확한 올림과 비교한다. 허용 오차는 커널의 설계(100 dB, 진폭의
 * 약 10<sup>-5</sup>. {@link Resampler} 참조)와 구현이 잰 값에 약 2배의 여유를 둔 것이다.
 */
public class ResamplerTest {

    /**
     * The pairs of sample rates that most tests use (they include exact phases, an interpolated table and the extreme ratios).
     * <p>
     * 대부분의 시험이 쓰는 표본율 쌍(정확한 위상, 보간하는 표, 극단적인 비율을 포함한다).
     */
    private static final double[][] PAIRS = {{44100, 48000}, {48000, 44100}, {22050, 48000}, {8000, 48000}, {96000, 44100}, {44100, 22050}, {44100, 96000},
            {11025, 48000}, {16000, 44100}, {32000, 48000}, {3000, 768000}, {768000, 3000}, {44101, 48000}, {44100.5, 48000}, {48000, 44100.25}};

    /**
     * Creates the test class.
     * <p>
     * 시험 클래스를 만든다.
     */
    public ResamplerTest() {
    }

    /**
     * Creates a sine wave.
     * <p>
     * 사인파를 만든다.
     *
     * @param hz  the frequency / 주파수
     * @param sr  the sample rate / 표본율
     * @param n   the length in samples / 길이(표본)
     * @param amp the amplitude / 진폭
     * @return the samples / 표본
     */
    static float[] sine(double hz, double sr, int n, double amp) {
        float[] f = new float[n];
        // 표본마다 해당 시각의 사인 값을 계산한다.
        for (int i = 0; i < n; i++) {
            f[i] = (float) (amp * Math.sin(2 * Math.PI * hz * i / sr));
        }
        return f;
    }

    /**
     * The least squares amplitude of a sinusoid of a known frequency: fits <code>a sin + b cos</code> to the samples and returns
     * <code>sqrt(a^2 + b^2)</code>. For a pure tone the result is exact whatever the number of cycles in the range.
     * <p>
     * 주파수를 아는 정현파의 최소 제곱 진폭: 표본에 <code>a sin + b cos</code> 를 적합하고 <code>sqrt(a^2 + b^2)</code> 를 반환한다. 순수한 톤이면 구간에 든
     * 주기의 수와 무관하게 결과가 정확하다.
     *
     * @param a    the samples / 표본
     * @param from the first index / 첫 인덱스
     * @param to   the end index (exclusive) / 끝 인덱스(미포함)
     * @param hz   the frequency / 주파수
     * @param sr   the sample rate / 표본율
     * @return the amplitude / 진폭
     */
    static double fitAmplitude(float[] a, int from, int to, double hz, double sr) {
        double[] xy = fit(a, from, to, hz, sr);
        return Math.sqrt(xy[0] * xy[0] + xy[1] * xy[1]);
    }

    /**
     * Fits <code>x sin + y cos</code> of a known frequency to the samples by least squares (the 2 by 2 normal equations).
     * <p>
     * 주파수를 아는 <code>x sin + y cos</code> 를 표본에 최소 제곱으로 적합한다(2 x 2 정규 방정식).
     *
     * @param a    the samples / 표본
     * @param from the first index / 첫 인덱스
     * @param to   the end index (exclusive) / 끝 인덱스(미포함)
     * @param hz   the frequency / 주파수
     * @param sr   the sample rate / 표본율
     * @return the two coefficients <code>{x, y}</code> / 두 계수 <code>{x, y}</code>
     */
    static double[] fit(float[] a, int from, int to, double hz, double sr) {
        double ss = 0;
        double cc = 0;
        double sc = 0;
        double sy = 0;
        double cy = 0;
        double w = 2 * Math.PI * hz / sr;
        // 구간의 표본마다 정규 방정식의 합을 누적한다.
        for (int i = from; i < to; i++) {
            double s = Math.sin(w * i);
            double c = Math.cos(w * i);
            ss += s * s;
            cc += c * c;
            sc += s * c;
            sy += s * a[i];
            cy += c * a[i];
        }
        double det = ss * cc - sc * sc;
        return new double[] {(sy * cc - cy * sc) / det, (cy * ss - sy * sc) / det};
    }

    /**
     * The largest sample of what remains after the best fitting sinusoid of a known frequency is subtracted: a measure of everything in the
     * signal that is not that tone (images, aliasing, the rounding of float samples).
     * <p>
     * 주파수를 아는 가장 잘 맞는 정현파를 뺀 나머지의 가장 큰 표본: 신호에서 그 톤이 아닌 모든 것(이미지, 앨리어싱, float 표본의 반올림)의 척도.
     *
     * @param a    the samples / 표본
     * @param from the first index / 첫 인덱스
     * @param to   the end index (exclusive) / 끝 인덱스(미포함)
     * @param hz   the frequency / 주파수
     * @param sr   the sample rate / 표본율
     * @return the largest absolute value of the remainder / 나머지의 절댓값 최댓값
     */
    static double remainderPeak(float[] a, int from, int to, double hz, double sr) {
        double[] xy = fit(a, from, to, hz, sr);
        double w = 2 * Math.PI * hz / sr;
        double max = 0;
        // 구간의 모든 표본에서 적합한 정현파를 뺀 값의 절댓값 최댓값을 찾는다.
        for (int i = from; i < to; i++) {
            max = Math.max(max, Math.abs(a[i] - xy[0] * Math.sin(w * i) - xy[1] * Math.cos(w * i)));
        }
        return max;
    }

    /**
     * The number of output samples at each end that the zero extension of the input disturbs (the kernel length measured in output samples, twice its half width, plus a margin).
     * <p>
     * 입력의 0 연장이 방해하는 양 끝의 출력 표본 수(출력 표본으로 잰 커널 길이, 곧 반폭의 두 배에 여유를 더한 값).
     *
     * @param r the converter / 변환기
     * @return the margin in output samples / 출력 표본 단위의 여유
     */
    private static int margin(Resampler r) {
        return (int) Math.ceil(r.getTaps() * r.getOutputRate() / r.getInputRate()) + 20;
    }

    /**
     * The largest difference between the converted sine and the ideal sine at the output times, in the part of the signal that the ends do
     * not disturb.
     * <p>
     * 변환한 사인파와 출력 시각의 이상적인 사인파 사이의 최대 차이. 양 끝이 방해하지 않는 구간에서 잰다.
     *
     * @param inRate  the input rate / 입력 표본율
     * @param outRate the output rate / 출력 표본율
     * @param hz      the frequency of the tone / 톤의 주파수
     * @param seconds the duration of the input / 입력의 길이(초)
     * @return the largest difference / 최대 차이
     */
    private static double sineError(double inRate, double outRate, double hz, double seconds) {
        int n = (int) (inRate * seconds);
        float[] out = Resampler.resample(sine(hz, inRate, n, 0.5), inRate, outRate);
        int m = margin(new Resampler(inRate, outRate));
        double max = 0;
        // 가장자리를 뺀 모든 출력 표본을 이상적인 사인 값과 비교한다.
        for (int i = m; i < out.length - m; i++) {
            max = Math.max(max, Math.abs(out[i] - 0.5 * Math.sin(2 * Math.PI * hz * i / outRate)));
        }
        return max;
    }

    /**
     * Equal rates pass every sample through unchanged and need no tail.
     * <p>
     * 표본율이 같으면 모든 표본이 그대로 통과하며 꼬리가 필요 없다.
     */
    public void testEqualRatesPassSamplesThrough() {
        Resampler r = new Resampler(44100, 44100);
        Check.isTrue(r.isIdentity(), "identity");
        float[] in = sine(440, 44100, 1000, 0.7);
        float[] out = new float[r.maxOutputFrames(in.length)];
        int n = r.process(in, 0, in.length, out, 0);
        Check.equal(in.length, n, "all samples are produced at once");
        Check.isTrue(Arrays.equals(in, Arrays.copyOf(out, n)), "the samples are identical");
        Check.equal(0, r.finish(out, 0), "no tail");
        Check.equal(1234, r.outputLength(1234), "length");
        Check.isTrue(Arrays.equals(in, Resampler.resample(in, 44100, 44100)), "the one-call interface too");
    }

    /**
     * The output length is exactly <code>ceil(N * out / in)</code> computed in integers, and the converter produces exactly that many
     * samples, for many input lengths (including 0, 1 and lengths whose ratio is just below or at a whole number).
     * <p>
     * 출력 길이는 정수로 계산한 <code>ceil(N * out / in)</code> 와 정확히 같고, 변환기는 정확히 그만큼의 표본을 만든다. 입력 길이는 0, 1, 비율이 정수 바로
     * 아래이거나 정수인 길이를 포함해 여러 가지를 쓴다.
     */
    public void testOutputLengthIsTheCeilingOfTheRatio() {
        long[][] rates = {{44100, 48000}, {48000, 44100}, {8000, 48000}, {22050, 48000}, {96000, 44100}, {44100, 22050}, {44101, 48000}, {3000, 768000}, {768000, 3000}};
        int[] lengths = {0, 1, 2, 3, 5, 6, 7, 100, 147, 148, 160, 161, 1000, 4410, 44099, 44100, 44101};
        // 표본율 쌍과 입력 길이의 모든 조합을 확인한다.
        for (int p = 0; p < rates.length; p++) {
            long in = rates[p][0];
            long out = rates[p][1];
            Resampler r = new Resampler(in, out);
            // 입력 길이마다 길이 식과 실제 출력을 확인한다.
            for (int k = 0; k < lengths.length; k++) {
                long n = lengths[k];
                long want = (n * out + in - 1) / in;
                Check.equal(want, r.outputLength(n), "outputLength(" + n + ") for " + in + " -> " + out);
                float[] produced = Resampler.resample(new float[(int) n], in, out);
                Check.equal(want, produced.length, "the produced length for " + n + " frames, " + in + " -> " + out);
            }
        }
    }

    /**
     * A sine of a frequency in the pass band is reproduced to within the design accuracy of the kernel (absolute error below 10<sup>-5</sup>
     * for an amplitude of 0.5, which is -94 dB) for every pair of rates, at three tones: 1 kHz (or the lowest of the pair), half the lower
     * Nyquist frequency and 89 % of it.
     * <p>
     * 통과 대역의 주파수를 가진 사인파는 모든 표본율 쌍에서 커널의 설계 정확도 안으로 재현된다(진폭 0.5 에서 절대 오차 10<sup>-5</sup> 미만, 곧 -94 dB).
     * 톤은 1 kHz(또는 쌍 중 낮은 쪽), 낮은 쪽 나이퀴스트 주파수의 절반, 그 89 % 의 셋이다.
     */
    public void testSineIsReproducedForEveryPairOfRates() {
        // 표본율 쌍마다 세 톤을 변환해 이상적인 사인과 비교한다.
        for (int p = 0; p < PAIRS.length; p++) {
            double in = PAIRS[p][0];
            double out = PAIRS[p][1];
            double low = Math.min(in, out) / 2;
            double[] tones = {Math.min(1000.0, low * 0.5), low * 0.5, low * 0.89};
            // 톤마다 변환 오차를 확인한다.
            for (int t = 0; t < tones.length; t++) {
                double err = sineError(in, out, tones[t], 0.4);
                Check.isTrue(err < 1e-5, in + " -> " + out + " tone " + tones[t] + " Hz: error " + err + " is not below 1e-5");
            }
        }
    }

    /**
     * The pass band is flat: tones from 5 % to 90 % of the lower Nyquist frequency come out with a gain within 0.001 dB of 1 (the design
     * has a ripple of about 0.0001 dB), for every pair of rates, measured by a least squares fit.
     * <p>
     * 통과 대역은 평탄하다: 낮은 쪽 나이퀴스트 주파수의 5 % ~ 90 % 톤이 모든 표본율 쌍에서 이득이 1 에서 0.001 dB 이내로 나온다(설계의 리플은 약
     * 0.0001 dB). 최소 제곱 적합으로 잰다.
     */
    public void testPassBandIsFlat() {
        // 표본율 쌍마다 여러 주파수의 이득을 잰다.
        for (int p = 0; p < PAIRS.length; p++) {
            double in = PAIRS[p][0];
            double out = PAIRS[p][1];
            double low = Math.min(in, out) / 2;
            int n = (int) (in * 0.4);
            Resampler r = new Resampler(in, out);
            int m = margin(r);
            // 통과 대역 안의 여러 비율에서 이득을 잰다.
            for (double frac = 0.05; frac < 0.9001; frac += 0.1625) {
                double hz = low * frac;
                float[] o = Resampler.resample(sine(hz, in, n, 0.5), in, out);
                double db = 20 * Math.log10(fitAmplitude(o, m, o.length - m, hz, out) / 0.5);
                Check.isTrue(Math.abs(db) < 0.001, in + " -> " + out + " at " + frac + " of the Nyquist frequency: gain " + db + " dB");
            }
        }
    }

    /**
     * The transition band has the shape of the design: the gain is -6.02 dB (half the amplitude) at 95 % of the lower Nyquist frequency,
     * the cut-off of a symmetric transition band, and falls below -20 dB at 97.5 %.
     * <p>
     * 전이 대역은 설계한 모양이다: 낮은 쪽 나이퀴스트 주파수의 95 %(대칭 전이 대역의 차단 주파수)에서 이득이 -6.02 dB(진폭의 절반)이고 97.5 % 에서는
     * -20 dB 아래로 내려간다.
     */
    public void testTransitionBandShape() {
        double[][] pairs = {{44100, 48000}, {48000, 44100}, {96000, 44100}, {44100, 22050}};
        // 표본율 쌍마다 차단 주파수와 그 근처의 이득을 잰다.
        for (int p = 0; p < pairs.length; p++) {
            double in = pairs[p][0];
            double out = pairs[p][1];
            double low = Math.min(in, out) / 2;
            int n = (int) (in * 0.4);
            Resampler r = new Resampler(in, out);
            int m = margin(r);
            float[] at95 = Resampler.resample(sine(low * 0.95, in, n, 0.5), in, out);
            double db95 = 20 * Math.log10(fitAmplitude(at95, m, at95.length - m, low * 0.95, out) / 0.5);
            Check.near(-6.0206, db95, 0.01, in + " -> " + out + ": the gain at the cut-off");
            float[] at975 = Resampler.resample(sine(low * 0.975, in, n, 0.5), in, out);
            double db975 = 20 * Math.log10(fitAmplitude(at975, m, at975.length - m, low * 0.975, out) / 0.5);
            Check.isTrue(db975 < -20, in + " -> " + out + ": the gain at 97.5 % is " + db975 + " dB");
        }
    }

    /**
     * The stop band attenuates by at least 96 dB (the resolution of the 16 bit samples that the decoders deliver; the design is 100 dB):
     * a tone above the Nyquist frequency of the lower rate that a down-conversion would alias to a lower frequency leaves at most -96 dB
     * (the largest sample of the output relative to the input amplitude), at several frequencies.
     * <p>
     * 정지 대역은 96 dB 이상 감쇠시킨다(디코더가 주는 16비트 표본의 해상도. 설계는 100 dB). 낮은 쪽 표본율의 나이퀴스트 주파수보다 높아서 내릴 때 낮은
     * 주파수로 앨리어싱될 톤이 여러 주파수에서 최대 -96 dB(출력의 가장 큰 표본을 입력 진폭에 대한 비로) 이하로 남는다.
     */
    public void testStopBandRejectsWhatWouldAlias() {
        double[][] cases = {{48000, 44100, 22491}, {48000, 44100, 23500}, {48000, 44100, 24000 - 1}, {96000, 44100, 30000}, {96000, 44100, 47999}, {44100, 22050, 11135},
                {44100, 22050, 15000}, {44100, 22050, 20000}, {44101, 22050, 12000}, {768000, 3000, 1530}, {768000, 3000, 100000}};
        // 경우마다 톤을 변환하고 가장자리를 뺀 출력의 최댓값을 잰다.
        for (int c = 0; c < cases.length; c++) {
            double in = cases[c][0];
            double out = cases[c][1];
            double hz = cases[c][2];
            int n = (int) (in * 0.3);
            Resampler r = new Resampler(in, out);
            int m = margin(r);
            float[] o = Resampler.resample(sine(hz, in, n, 0.5), in, out);
            double peak = 0;
            // 가장자리를 뺀 구간에서 가장 큰 표본을 찾는다.
            for (int i = m; i < o.length - m; i++) {
                peak = Math.max(peak, Math.abs(o[i]));
            }
            double db = 20 * Math.log10(peak / 0.5 + 1e-30);
            Check.isTrue(db < -96, in + " -> " + out + " tone " + hz + " Hz leaves " + db + " dB");
        }
    }

    /**
     * An up-conversion removes the images: a tone near the top of the input band converted to a much higher rate leaves no component at the
     * image frequency <code>inputRate - f</code> larger than -96 dB. The tone is removed from the output by a least squares fit and the
     * remainder (the images, the aliasing and the rounding of float samples) is measured as its largest sample.
     * <p>
     * 올리는 변환은 이미지를 없앤다: 입력 대역의 윗부분에 있는 톤을 훨씬 높은 표본율로 바꾸어도 이미지 주파수 <code>inputRate - f</code> 의 성분이 -96 dB 보다
     * 크지 않다. 출력에서 톤을 최소 제곱 적합으로 빼고 나머지(이미지, 앨리어싱, float 표본의 반올림)를 가장 큰 표본으로 잰다.
     */
    public void testUpConversionRemovesImages() {
        double[][] cases = {{22050, 48000, 9000}, {22050, 48000, 9800}, {8000, 48000, 3500}, {44100, 96000, 19000}, {44100, 96000, 19800}, {11025, 48000, 4800}};
        // 경우마다 톤을 변환하고 톤을 뺀 나머지의 최댓값을 잰다.
        for (int c = 0; c < cases.length; c++) {
            double in = cases[c][0];
            double out = cases[c][1];
            double hz = cases[c][2];
            int n = (int) (in * 0.4);
            Resampler r = new Resampler(in, out);
            int m = margin(r);
            float[] o = Resampler.resample(sine(hz, in, n, 0.5), in, out);
            double max = remainderPeak(o, m, o.length - m, hz, out);
            double db = 20 * Math.log10(max / 0.5 + 1e-30);
            Check.isTrue(db < -96, in + " -> " + out + " tone " + hz + " Hz: the remainder is " + db + " dB");
        }
    }

    /**
     * A constant input stays constant: every phase of the kernel is normalised to the gain 1 at DC (exact and interpolated tables).
     * <p>
     * 일정한 입력은 일정하게 남는다: 커널의 모든 위상은 DC 이득이 1 이 되도록 정규화되어 있다(정확한 표와 보간하는 표 모두).
     */
    public void testDcGainIsExactlyOne() {
        // 표본율 쌍마다 일정한 입력을 변환해 가장자리를 뺀 모든 출력이 같은 값인지 확인한다.
        for (int p = 0; p < PAIRS.length; p++) {
            double in = PAIRS[p][0];
            double out = PAIRS[p][1];
            float[] constant = new float[(int) (in * 0.3)];
            Arrays.fill(constant, 0.75f);
            float[] o = Resampler.resample(constant, in, out);
            int m = margin(new Resampler(in, out));
            // 가장자리를 뺀 모든 출력 표본이 0.75 인지 확인한다.
            for (int i = m; i < o.length - m; i++) {
                if (Math.abs(o[i] - 0.75f) > 2e-7) {
                    Check.fail(in + " -> " + out + ": sample " + i + " is " + o[i] + " instead of 0.75");
                }
            }
        }
    }

    /**
     * The response to a unit impulse is the kernel centred on the output position: for a down-conversion by 2 an impulse at the input
     * sample 400 gives the peak at the output sample 200 (no delay), the peak is the kernel centre <code>2 * cutoff = 0.475</code> and the
     * response is symmetric around it; for an up-conversion by 2 the same holds around the output sample 800.
     * <p>
     * 단위 임펄스에 대한 응답은 출력 위치에 중심을 둔 커널이다. 2 로 내리는 변환에서 입력 표본 400 의 임펄스는 출력 표본 200 에서 정점을 만들고(지연 없음),
     * 정점은 커널의 중심 <code>2 * 차단 주파수 = 0.475</code> 이며 응답은 그 둘레에 대칭이다. 2 로 올리는 변환에서는 출력 표본 800 둘레에서 같다.
     */
    public void testImpulseResponseIsSymmetricWithoutDelay() {
        float[] impulse = new float[800];
        impulse[400] = 1f;
        float[] down = Resampler.resample(impulse, 44100, 22050);
        Check.near(0.475, down[200], 1e-3, "the kernel centre after the down-conversion");
        // 정점 둘레의 응답이 좌우 대칭인지 확인한다.
        for (int k = 1; k < 150; k++) {
            Check.near(down[200 - k], down[200 + k], 1e-7, "symmetry at " + k + " (down)");
        }
        float[] up = Resampler.resample(impulse, 22050, 44100);
        Check.near(0.95, up[800], 1e-3, "the kernel centre after the up-conversion");
        // 정점 둘레의 응답이 좌우 대칭인지 확인한다.
        for (int k = 1; k < 300; k++) {
            Check.near(up[800 - k], up[800 + k], 1e-7, "symmetry at " + k + " (up)");
        }
    }

    /**
     * The result does not depend on how the input is split into chunks: any sequence of chunk sizes (1 sample, odd sizes, whole blocks,
     * a single call) gives exactly the same samples as the one-call conversion, also with a table that is interpolated.
     * <p>
     * 결과는 입력을 덩어리로 나누는 방식과 무관하다. 덩어리 크기의 어떤 순서(표본 1 개, 홀수 크기, 큰 블록, 한 번의 호출)도 한 번에 변환한 결과와 정확히 같은
     * 표본을 만든다. 보간하는 표에서도 같다.
     */
    public void testChunkingDoesNotChangeTheResult() {
        double[][] pairs = {{44100, 48000}, {96000, 44100}, {44101, 48000}, {8000, 48000}};
        int[][] chunkings = {{1}, {7}, {255, 1, 4096}, {4096}, {10000}, {3, 5, 8, 13, 21, 34}};
        // 표본율 쌍마다 여러 나눔 방식으로 변환해 한 번에 변환한 결과와 비교한다.
        for (int p = 0; p < pairs.length; p++) {
            double in = pairs[p][0];
            double out = pairs[p][1];
            float[] signal = new float[(int) (in * 0.12)];
            java.util.Random rnd = new java.util.Random(1234 + p);
            // 잡음 신호를 쓰면 한 표본이라도 어긋나면 드러난다.
            for (int i = 0; i < signal.length; i++) {
                signal[i] = (float) (rnd.nextDouble() * 2 - 1);
            }
            float[] whole = Resampler.resample(signal, in, out);
            // 나눔 방식마다 변환해 한 번에 변환한 결과와 비교한다.
            for (int c = 0; c < chunkings.length; c++) {
                Resampler r = new Resampler(in, out);
                float[] got = new float[(int) r.outputLength(signal.length)];
                int written = 0;
                int pos = 0;
                int k = 0;
                // 정해진 덩어리 크기를 되풀이하며 입력을 끝까지 준다.
                while (pos < signal.length) {
                    int n = Math.min(chunkings[c][k % chunkings[c].length], signal.length - pos);
                    k++;
                    float[] tmp = new float[r.maxOutputFrames(n)];
                    int w = r.process(signal, pos, n, tmp, 0);
                    System.arraycopy(tmp, 0, got, written, w);
                    written += w;
                    pos += n;
                }
                float[] tail = new float[r.maxFinishFrames()];
                int w = r.finish(tail, 0);
                System.arraycopy(tail, 0, got, written, w);
                written += w;
                Check.equal(got.length, written, "the number of samples, " + in + " -> " + out + " chunking " + c);
                Check.isTrue(Arrays.equals(whole, got), in + " -> " + out + ": chunking " + c + " differs from the one-call conversion");
            }
        }
    }

    /**
     * Very short inputs: no sample gives no output; one sample gives <code>ceil(out / in)</code> samples that are the response of the
     * kernel (the first output sample is the kernel centre); the converter does not need more input than it has.
     * <p>
     * 아주 짧은 입력: 표본이 없으면 출력도 없다. 표본 하나는 <code>ceil(out / in)</code> 개의 표본을 만들며 그것은 커널의 응답이다(첫 출력 표본이 커널의
     * 중심). 변환기는 가진 것보다 많은 입력을 요구하지 않는다.
     */
    public void testVeryShortInputs() {
        Check.equal(0, Resampler.resample(new float[0], 44100, 48000).length, "no input");
        float[] one = Resampler.resample(new float[] {1f}, 8000, 48000);
        Check.equal(6, one.length, "one sample at 8000 -> 48000");
        Check.near(0.95, one[0], 1e-3, "the first sample is the kernel centre");
        // 중심에서 멀어질수록 커널은 작아지고 부호가 번갈아야 하므로 모든 값은 중심보다 작다.
        for (int i = 1; i < one.length; i++) {
            Check.isTrue(Math.abs(one[i]) < one[0], "sample " + i + " is smaller than the centre");
        }
        float[] one2 = Resampler.resample(new float[] {1f}, 44100, 22050);
        Check.equal(1, one2.length, "one sample at 44100 -> 22050");
        Check.near(0.475, one2[0], 1e-3, "the kernel centre of the down-conversion");
        float[] two = Resampler.resample(new float[] {1f, -1f}, 48000, 3000);
        Check.equal(1, two.length, "two samples at 48000 -> 3000");
    }

    /**
     * Large ratios and rates that are not whole numbers work: 3000 to 768000 (256 times) and back, a table that is interpolated for a
     * pair of coprime rates, and rates given as floats with a fraction. Their properties describe the table that was chosen.
     * <p>
     * 큰 비율과 정수가 아닌 표본율도 동작한다: 3000 에서 768000(256배)과 그 반대, 서로소인 표본율 쌍의 보간하는 표, 소수부가 있는 float 표본율. 성질로
     * 선택된 표를 확인한다.
     */
    public void testLargeRatiosAndFractionalRates() {
        Resampler exact = new Resampler(44100, 48000);
        Check.isTrue(exact.isExact(), "44100 -> 48000 has exact phases");
        Check.equal(160, exact.getPhases(), "160 phases (48000 / 300)");
        Check.equal(130, exact.getTaps(), "130 taps at equal rates");
        Resampler coprime = new Resampler(44101, 48000);
        Check.isFalse(coprime.isExact(), "a coprime pair is interpolated");
        Check.equal(Resampler.INTERPOLATED_PHASES, coprime.getPhases(), "rows of the interpolated table");
        Resampler wide = new Resampler(768000, 3000);
        Check.isTrue(wide.getTaps() > 30000, "the kernel is stretched by the ratio: " + wide.getTaps());
        Check.isTrue(wide.isExact(), "768000 -> 3000 is a single exact phase");
        Check.equal(1, wide.getPhases(), "one phase");
        Resampler fractional = new Resampler(44100.5, 48000);
        Check.isFalse(fractional.isExact(), "a fractional rate is interpolated");
        // 보간하는 표는 행 수와 탭 수의 곱이 표 한도를 넘지 않아야 한다.
        double[][] cases = {{768000, 3003}, {768000, 3001}, {3001, 768000}, {99999, 3001}};
        for (int i = 0; i < cases.length; i++) {
            Resampler r = new Resampler(cases[i][0], cases[i][1]);
            Check.isTrue((long) (r.getPhases() + 1) * r.getTaps() <= 7 * 1024 * 1024, cases[i][0] + " -> " + cases[i][1] + ": the table is too large: " + r.getPhases() + " x " + r.getTaps());
        }
    }

    /**
     * Invalid rates are refused by the constructor: not a number, zero, negative, infinite and a ratio above 1024 in either direction.
     * <p>
     * 올바르지 않은 표본율은 생성자가 거부한다: 숫자가 아님, 0, 음수, 무한대, 어느 방향이든 1024 를 넘는 비율.
     */
    public void testInvalidRatesAreRefused() {
        final double[][] bad = {{Double.NaN, 44100}, {44100, Double.NaN}, {0, 44100}, {44100, 0}, {-1, 44100}, {44100, -48000}, {Double.POSITIVE_INFINITY, 44100},
                {44100, Double.POSITIVE_INFINITY}, {100, 768000}, {768000, 100}};
        // 잘못된 표본율 쌍마다 IllegalArgumentException 이 나야 한다.
        for (int i = 0; i < bad.length; i++) {
            final double a = bad[i][0];
            final double b = bad[i][1];
            Check.throwsException(IllegalArgumentException.class, new Block() {
                public void run() throws Exception {
                    new Resampler(a, b);
                }
            }, a + " -> " + b);
        }
    }

    /**
     * After <code>finish</code> the converter refuses more input until it is reset, and after a <code>reset</code> it produces exactly the same
     * output again; the tail can be written into small arrays by calling <code>finish</code> repeatedly.
     * <p>
     * <code>finish</code> 뒤에는 초기화하기 전까지 더 이상 입력을 받지 않으며 <code>reset</code> 뒤에는 정확히 같은 출력을 다시 만든다. 꼬리는 작은 배열에
     * <code>finish</code> 를 되풀이해 부르며 쓸 수 있다.
     */
    public void testFinishResetAndSmallOutputArrays() {
        final Resampler r = new Resampler(44100, 48000);
        float[] signal = sine(1000, 44100, 5000, 0.5);
        float[] first = Resampler.resample(signal, 44100, 48000);
        float[] got = new float[first.length];
        int written = r.process(signal, 0, signal.length, got, 0);
        // 꼬리를 3 표본짜리 배열로 나누어 받는다.
        for (;;) {
            float[] small = new float[3];
            int w = r.finish(small, 0);
            if (w == 0) {
                break;
            }
            System.arraycopy(small, 0, got, written, w);
            written += w;
        }
        Check.equal(first.length, written, "all samples were produced");
        Check.isTrue(Arrays.equals(first, got), "the tail written in pieces equals the one-call result");
        Check.throwsException(IllegalStateException.class, new Block() {
            public void run() throws Exception {
                r.process(new float[1], 0, 1, new float[10], 0);
            }
        }, "process after finish");
        r.reset();
        float[] again = new float[first.length + 8];
        int w = r.process(signal, 0, signal.length, again, 0);
        w += r.finish(again, w);
        Check.equal(first.length, w, "the length after a reset");
        Check.isTrue(Arrays.equals(first, Arrays.copyOf(again, w)), "the output after a reset");
    }

    /**
     * Converters share nothing: two threads that convert different signals at once get the same results as when they run one after the other.
     * <p>
     * 변환기끼리는 아무것도 공유하지 않는다: 서로 다른 신호를 동시에 변환하는 두 스레드는 차례로 돌릴 때와 같은 결과를 얻는다.
     *
     * @throws Exception if a thread fails / 스레드가 실패하면
     */
    public void testInstancesAreIndependentAcrossThreads() throws Exception {
        final float[] a = sine(1000, 44100, 20000, 0.5);
        final float[] b = sine(3000, 44100, 20000, 0.3);
        final float[] wantA = Resampler.resample(a, 44100, 48000);
        final float[] wantB = Resampler.resample(b, 44100, 48000);
        final boolean[] ok = {true, true};
        Thread[] threads = new Thread[2];
        // 두 스레드가 각자의 신호를 여러 번 변환한다.
        for (int t = 0; t < 2; t++) {
            final int id = t;
            threads[t] = new Thread(new Runnable() {
                public void run() {
                    // 같은 변환을 되풀이하며 결과가 항상 같은지 확인한다.
                    for (int i = 0; i < 30; i++) {
                        float[] got = Resampler.resample(id == 0 ? a : b, 44100, 48000);
                        if (!Arrays.equals(got, id == 0 ? wantA : wantB)) {
                            ok[id] = false;
                        }
                    }
                }
            });
            threads[t].start();
        }
        threads[0].join();
        threads[1].join();
        Check.isTrue(ok[0] && ok[1], "the threads got the same results as the single thread run");
    }

    /**
     * A minute of audio converts in a time that excludes accidentally quadratic work (the window is compacted as it advances): measured at
     * about half a second on the development machine, the limit here is 20 seconds so that a slow or loaded machine does not fail it.
     * <p>
     * 1분 분량의 오디오가 우연히 제곱에 비례하는 작업을 배제하는 시간에 변환된다(창은 앞으로 가면서 압축한다). 개발 PC 에서 약 0.5 초로 쟀고, 느리거나 부하가
     * 큰 PC 에서 실패하지 않도록 여기의 한도는 20 초이다.
     */
    public void testOneMinuteConvertsQuickly() {
        float[] minute = sine(440, 44100, 44100 * 60, 0.5);
        long t0 = System.currentTimeMillis();
        float[] out = Resampler.resample(minute, 44100, 48000);
        long ms = System.currentTimeMillis() - t0;
        Check.equal(48000L * 60, out.length, "length");
        Check.isTrue(ms < 20000, "a minute took " + ms + " ms");
    }
}
