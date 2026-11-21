package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.KemarReference;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the interpolation and the rendering kernels of the HRTF set (plan item P25-04): at a measured direction the kernels are
 * the measured minimum phase kernels delayed by their delays with an eight point Lagrange interpolation written out in the test; a
 * delay of a fraction of a sample is flat up to 0.22 of the sample rate (measured on a synthetic data set that holds an impulse); the
 * left side is the right side with the ears swapped; elevations and azimuths beyond the data are clamped; between measured
 * directions the kernels and the delays are the weighted sums; the data at other sample rates has the same magnitude response at the
 * same frequencies; and against the original KEMAR responses of the test resources the rendering kernels have the same magnitude
 * response (within 0.5 dB up to 10 kHz) and the same interaural time difference of the low frequency part (within 1 sample).
 * <p>
 * HRTF 세트의 보간과 렌더링 커널 시험(계획 P25-04): 측정한 방향에서 커널은 측정한 최소 위상 커널을 지연만큼, 시험에 적은 8점 라그랑주 보간으로 지연한 것이다. 샘플의
 * 소수 지연은 샘플링 주파수의 0.22 배까지 평탄하다(임펄스를 담은 합성 자료로 측정). 왼쪽은 귀를 바꾼 오른쪽이다. 자료 밖의 고도와 방위각은 제한된다. 측정한 방향
 * 사이에서 커널과 지연은 가중 합이다. 다른 샘플링 주파수의 자료는 같은 주파수에서 크기 응답이 같다. 시험 자원의 원본 KEMAR 응답과 비교해 렌더링 커널은 크기 응답이
 * 같고(10 kHz 까지 0.5 dB 이내) 저주파 부분의 두 귀 시간차도 같다(1 샘플 이내).
 */
public class HrtfSetTest {

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public HrtfSetTest() {
    }

    /**
     * Returns the value at a tap of an array, or 0 outside.
     * <p>
     * 배열의 탭 값을 반환한다. 범위 밖이면 0.
     *
     * @param h the array / 배열
     * @param i the index / 인덱스
     * @return the value / 값
     */
    private static double at(float[] h, int i) {
        return i < 0 || i >= h.length ? 0.0 : h[i];
    }

    /**
     * The eight point Lagrange delay written out for the test: the value of the sequence at the position <code>k - delay</code> from the
     * nodes -3 to 4 around the integer part (the formula of the interpolation polynomial, evaluated by its definition).
     * <p>
     * 시험에 직접 적은 8점 라그랑주 지연: 정수 부분 둘레의 노드 -3 ~ 4 에서 구한 위치 <code>k - delay</code> 의 수열 값(보간 다항식의 정의대로 계산).
     *
     * @param h      the sequence / 수열
     * @param delay  the delay in samples / 지연(샘플)
     * @param length the number of output taps / 출력 탭 수
     * @return the delayed sequence / 지연한 수열
     */
    private static double[] lagrangeDelay(float[] h, double delay, int length) {
        double[] out = new double[length];
        // 출력 탭마다 위치 k - delay 의 값을 8개 노드의 라그랑주 다항식으로 구한다.
        for (int k = 0; k < length; k++) {
            double pos = k - delay;
            int i0 = (int) Math.floor(pos);
            double sum = 0;
            // 반복해서 처리한다.
            for (int j = i0 - 3; j <= i0 + 4; j++) {
                double basis = 1;
                // 반복해서 처리한다.
                for (int m = i0 - 3; m <= i0 + 4; m++) {
                    if (m != j) {
                        basis *= (pos - m) / (double) (j - m);
                    }
                }
                sum += basis * at(h, j);
            }
            out[k] = sum;
        }
        return out;
    }

    /**
     * At every measured direction of some rings the kernels are the minimum phase kernels delayed by their delays; the left ear for the
     * mirrored (negative) azimuth is the right ear of the positive one with the delays swapped too.
     * <p>
     * 몇 고리의 모든 측정 방향에서 커널은 최소 위상 커널을 지연만큼 지연한 것이다. 거울(음의) 방위각의 왼쪽 귀는 양의 방위각의 오른쪽 귀이며 지연도 바뀐다.
     */
    public void testMeasuredDirectionIsTheKernelWithItsDelay() {
        HrtfDatabase db = HrtfDatabaseTest.db();
        HrtfSet set = new HrtfSet(db, 44100f);
        int n = set.getKernelLength();
        float[] l = new float[n];
        float[] r = new float[n];
        float[] wl = new float[n];
        float[] wr = new float[n];
        float[] ml = new float[n];
        float[] mr = new float[n];
        int[] rings = {0, 4, 7, 9, 12};
        // 고리마다 측정한 방위각 일부에서 닫힌 식과 비교한다.
        for (int i = 0; i < rings.length; i++) {
            int e = rings[i];
            // 방위각마다 처리한다.
            for (int a = 0; a < db.getAzimuthCount(e); a += 3) {
                int az = db.getAzimuth(e, a);
                set.direction(az, db.getElevation(e), l, r, wl, wr);
                double[] el = lagrangeDelay(db.getKernel(e, a, 0), db.getDelay(e, a, 0) + HrtfSet.LEAD, n);
                double[] er = lagrangeDelay(db.getKernel(e, a, 1), db.getDelay(e, a, 1) + HrtfSet.LEAD, n);
                // 커널의 탭마다 처리한다.
                for (int k = 0; k < n; k++) {
                    Check.near(el[k], l[k], 2e-6, "left kernel " + db.getElevation(e) + "/" + az + " tap " + k);
                    Check.near(er[k], r[k], 2e-6, "right kernel " + db.getElevation(e) + "/" + az + " tap " + k);
                }
                // 음의 방위각은 귀를 바꾼 같은 방향이다.
                set.direction(-az, db.getElevation(e), ml, mr, wl, wr);
                for (int k = 0; k < n; k++) {
                    Check.near(r[k], ml[k], 1e-7, "mirrored left tap " + k);
                    Check.near(l[k], mr[k], 1e-7, "mirrored right tap " + k);
                }
            }
        }
    }

    /**
     * Builds the bytes of a synthetic HRTF resource with one direction: both ears have a unit impulse at the tap 10 and a delay.
     * <p>
     * 방향이 하나인 합성 HRTF 자원의 바이트를 만든다: 두 귀 모두 탭 10 에 단위 임펄스가 있고 지연이 있다.
     *
     * @param delay the delay of both ears in samples / 두 귀의 지연(샘플)
     * @return the bytes / 바이트
     * @throws IOException never / 발생하지 않는다
     */
    private static byte[] synthetic(float delay) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(bos);
        os.write("SWPHRTF1".getBytes("US-ASCII"));
        os.writeInt(1);
        os.writeInt(44100);
        os.writeInt(128);
        os.writeInt(1);
        os.writeShort(0);
        os.writeShort(1);
        os.writeShort(0);
        os.writeFloat(delay);
        os.writeFloat(delay);
        // 두 귀의 커널(탭 10 의 단위 임펄스)을 쓴다.
        for (int ear = 0; ear < 2; ear++) {
            for (int t = 0; t < 128; t++) {
                os.writeFloat(t == 10 ? 1f : 0f);
            }
        }
        os.writeInt(0);
        os.close();
        return bos.toByteArray();
    }

    /**
     * The fractional delay is flat: for a unit impulse delayed by 0, 0.25, 0.5 and 0.75 sample the magnitude response of the kernel is
     * 0 dB within 0.15 dB up to 0.22 of the sample rate (10 kHz at 44100 Hz), and the group delay (the phase slope at low frequency) is
     * the delay.
     * <p>
     * 소수 지연은 평탄하다. 0, 0.25, 0.5, 0.75 샘플 지연한 단위 임펄스에서 커널의 크기 응답은 샘플링 주파수의 0.22 배(44100 Hz 에서 10 kHz)까지 0.15 dB 이내로 0 dB 이고,
     * 군지연(저주파의 위상 기울기)은 지연이다.
     */
    public void testFractionalDelayIsFlat() throws Exception {
        float[] fractions = {0f, 0.25f, 0.5f, 0.75f};
        // 소수 지연마다 합성 자료로 세트를 만들어 크기와 군지연을 잰다.
        for (int i = 0; i < fractions.length; i++) {
            HrtfDatabase db = HrtfDatabase.read(new ByteArrayInputStream(synthetic(3f + fractions[i])));
            HrtfSet set = new HrtfSet(db, 44100f);
            int n = set.getKernelLength();
            float[] l = new float[n];
            float[] r = new float[n];
            set.direction(0, 0, l, r, new float[n], new float[n]);
            double[] kd = new double[n];
            // 커널의 탭마다 처리한다.
            for (int k = 0; k < n; k++) {
                kd[k] = l[k];
            }
            // 10 kHz(샘플링 주파수의 0.227 배)까지 500 Hz 간격으로 크기를 본다.
            for (double f = 500; f <= 10000; f += 500) {
                Check.near(0.0, KemarReference.magnitudeDb(kd, f, 44100.0), 0.15, "magnitude of the fractional delay " + fractions[i] + " at " + f + " Hz");
            }
            // 저주파 위상에서 군지연을 구한다: 두 주파수의 위상 차이를 각주파수 차이로 나눈다.
            double w1 = 2 * Math.PI * 200.0 / 44100.0;
            double w2 = 2 * Math.PI * 400.0 / 44100.0;
            double p1 = phase(kd, w1);
            double p2 = phase(kd, w2);
            Check.near(10.0 + 3.0 + HrtfSet.LEAD + fractions[i], -(p2 - p1) / (w2 - w1), 1e-3, "group delay of the fractional delay " + fractions[i]);
        }
    }

    /**
     * Returns the phase of a response at an angular frequency by a direct sum.
     * <p>
     * 응답의 각주파수에서의 위상을 직접 합으로 반환한다.
     *
     * @param h the response / 응답
     * @param w the angular frequency in radians per sample / 각주파수(라디안/샘플)
     * @return the phase / 위상
     */
    private static double phase(double[] h, double w) {
        double re = 0;
        double im = 0;
        // 탭마다 복소 지수와 곱해 더한다.
        for (int k = 0; k < h.length; k++) {
            re += h[k] * Math.cos(w * k);
            im -= h[k] * Math.sin(w * k);
        }
        return Math.atan2(im, re);
    }

    /**
     * Elevations below the lowest measured one use the lowest ring, azimuths beyond 180 degrees are clamped to 180, and the top (90
     * degrees) has the same kernels for every azimuth that is measured on its ring.
     * <p>
     * 가장 낮은 측정 고도 아래의 고도는 가장 낮은 고리를 쓰고, 180도를 넘는 방위각은 180도로 제한되며, 꼭대기(90도)는 고리에 측정된 모든 방위각에서 같은 커널을 가진다.
     */
    public void testClamping() {
        HrtfDatabase db = HrtfDatabaseTest.db();
        HrtfSet set = new HrtfSet(db, 44100f);
        int n = set.getKernelLength();
        float[] l1 = new float[n];
        float[] r1 = new float[n];
        float[] l2 = new float[n];
        float[] r2 = new float[n];
        float[] w1 = new float[n];
        float[] w2 = new float[n];
        set.direction(37, -80, l1, r1, w1, w2);
        set.direction(37, -40, l2, r2, w1, w2);
        // 커널의 탭마다 처리한다.
        for (int k = 0; k < n; k++) {
            Check.near(l2[k], l1[k], 0.0, "below the lowest elevation: left " + k);
            Check.near(r2[k], r1[k], 0.0, "below the lowest elevation: right " + k);
        }
        set.direction(200, 10, l1, r1, w1, w2);
        set.direction(180, 10, l2, r2, w1, w2);
        // 커널의 탭마다 처리한다.
        for (int k = 0; k < n; k++) {
            Check.near(l2[k], l1[k], 0.0, "beyond 180 degrees: left " + k);
        }
        set.direction(10, 90, l1, r1, w1, w2);
        set.direction(150, 90, l2, r2, w1, w2);
        // 커널의 탭마다 처리한다.
        for (int k = 0; k < n; k++) {
            Check.near(l2[k], l1[k], 1e-6, "the top does not depend on the azimuth: left " + k);
            Check.near(r2[k], r1[k], 1e-6, "the top does not depend on the azimuth: right " + k);
        }
        set.direction(0, 90, l1, r1, w1, w2);
        // 커널의 탭마다 처리한다.
        for (int k = 0; k < n; k++) {
            Check.near(l1[k], r1[k], 1e-6, "the top is the same for both ears " + k);
        }
    }

    /**
     * Between two measured directions the kernels and the delays are the weighted sums: the azimuth 2.5 on the horizontal ring is the
     * mean of the measurements at 0 and 5 degrees, the elevation 5 at the azimuth 30 is the mean of the rings of 0 and 10 degrees, and a
     * quarter way gives the weights 3/4 and 1/4 (the sum of the kernels is delayed by the sum of the delays).
     * <p>
     * 측정한 두 방향 사이에서 커널과 지연은 가중 합이다. 수평 고리의 방위각 2.5 는 0도와 5도 측정의 평균이고, 방위각 30의 고도 5는 0도와 10도 고리의 평균이며, 4분의 1
     * 지점은 가중치 3/4 와 1/4 이다(커널의 합을 지연의 합만큼 지연한다).
     */
    public void testInterpolationIsTheWeightedSum() {
        HrtfDatabase db = HrtfDatabaseTest.db();
        HrtfSet set = new HrtfSet(db, 44100f);
        int n = set.getKernelLength();
        float[] l = new float[n];
        float[] r = new float[n];
        float[] w1 = new float[n];
        float[] w2 = new float[n];
        // 고리 4(고도 0)의 방위각 0 과 5: 인덱스 0 과 1.
        double[] weights = {0.5, 0.75};
        double[] azimuths = {2.5, 1.25};
        // 항목을 하나씩 처리한다.
        for (int c = 0; c < weights.length; c++) {
            double wa = weights[c];
            set.direction(azimuths[c], 0, l, r, w1, w2);
            // 두 귀를 차례로 처리한다.
            for (int ear = 0; ear < 2; ear++) {
                float[] mix = new float[128];
                double d = 0;
                // 두 측정의 커널과 지연을 가중 합한다.
                for (int k = 0; k < 128; k++) {
                    mix[k] = (float) (wa * db.getKernel(4, 0, ear)[k] + (1 - wa) * db.getKernel(4, 1, ear)[k]);
                }
                d = wa * db.getDelay(4, 0, ear) + (1 - wa) * db.getDelay(4, 1, ear);
                double[] expected = lagrangeDelay(mix, d + HrtfSet.LEAD, n);
                float[] actual = ear == 0 ? l : r;
                // 커널의 탭마다 처리한다.
                for (int k = 0; k < n; k++) {
                    Check.near(expected[k], actual[k], 3e-6, "azimuth " + azimuths[c] + " ear " + ear + " tap " + k);
                }
            }
        }
        // 고도 5, 방위각 30: 고리 4(고도 0)와 5(고도 10)의 방위각 30(인덱스 6)의 평균.
        set.direction(30, 5, l, r, w1, w2);
        float[] mixL = new float[128];
        // 커널의 탭마다 처리한다.
        for (int k = 0; k < 128; k++) {
            mixL[k] = 0.5f * db.getKernel(4, 6, 0)[k] + 0.5f * db.getKernel(5, 6, 0)[k];
        }
        double dl = 0.5 * db.getDelay(4, 6, 0) + 0.5 * db.getDelay(5, 6, 0);
        double[] expectedL = lagrangeDelay(mixL, dl + HrtfSet.LEAD, n);
        // 커널의 탭마다 처리한다.
        for (int k = 0; k < n; k++) {
            Check.near(expectedL[k], l[k], 3e-6, "elevation 5 tap " + k);
        }
    }

    /**
     * The data at other sample rates has the same magnitude response at the same frequencies: the kernels at 48000 Hz and at 8000 Hz
     * against the kernels at 44100 Hz for several directions (within 0.5 dB, up to 10 kHz for 48000 Hz, 5.5 kHz for 22050 Hz and 2 kHz for 8000 Hz), and the
     * kernel lengths follow the rate.
     * <p>
     * 다른 샘플링 주파수의 자료는 같은 주파수에서 크기 응답이 같다. 여러 방향에서 48000 Hz 와 8000 Hz 의 커널을 44100 Hz 의 커널과 비교한다(48000 Hz 는 10 kHz, 22050 Hz 는 5.5 kHz, 8000 Hz 는 2 kHz
     * 낮은 주파수까지 0.5 dB 이내). 커널 길이는 속도를 따른다.
     */
    public void testOtherSampleRates() {
        HrtfDatabase db = HrtfDatabaseTest.db();
        HrtfSet base = new HrtfSet(db, 44100f);
        float[] rates = {48000f, 8000f, 22050f};
        double[] limits = {10000, 2000, 5500};
        // 샘플링 주파수마다 여러 방향의 크기 응답을 비교한다.
        for (int i = 0; i < rates.length; i++) {
            HrtfSet other = new HrtfSet(db, rates[i]);
            // 변환의 선행 탭은 sinc 영점 교차 10번의 폭(출력 샘플)이고, 커널 길이는 변환한 응답 + 양쪽 선행 탭 + 소수 지연의 선행 + 가장 큰 지연 + 4 이다.
            int lead = other.getLatency() - HrtfSet.LEAD;
            Check.equal((long) Math.ceil(10.0 * Math.max(1.0, rates[i] / 44100.0)), (long) lead, "the lead of the conversion at " + rates[i]);
            long expectedLength = (long) Math.ceil(128 * rates[i] / 44100.0 - 1e-9) + 2 * lead + HrtfSet.LEAD + (long) Math.ceil(other.getMaxDelay()) + 4;
            Check.equal(expectedLength, (long) other.getKernelLength(), "the kernel length follows the rate " + rates[i]);
            Check.equal(0L, (long) base.getLatency() - HrtfSet.LEAD, "no conversion lead at the rate of the data");
            double[][] dirs = {{90, 0}, {-45, 20}, {135, -20}, {20, 50}};
            // 항목을 하나씩 처리한다.
            for (int d = 0; d < dirs.length; d++) {
                float[] l1 = new float[base.getKernelLength()];
                float[] r1 = new float[l1.length];
                float[] l2 = new float[other.getKernelLength()];
                float[] r2 = new float[l2.length];
                base.direction(dirs[d][0], dirs[d][1], l1, r1, new float[l1.length], new float[l1.length]);
                other.direction(dirs[d][0], dirs[d][1], l2, r2, new float[l2.length], new float[l2.length]);
                double[] a = toDouble(l1);
                double[] b = toDouble(l2);
                // 낮은 쪽 속도의 한계 주파수까지 500 Hz 간격으로 크기를 비교한다. 매우 작은 크기(피크보다 30 dB 아래)는 건너뛴다.
                double peak = -1000;
                for (double f = 500; f <= limits[i]; f += 500) {
                    peak = Math.max(peak, KemarReference.magnitudeDb(a, f, 44100.0));
                }
                // 주파수를 올려 가며 처리한다.
                for (double f = 500; f <= limits[i]; f += 500) {
                    double ma = KemarReference.magnitudeDb(a, f, 44100.0);
                    if (ma < peak - 30) {
                        continue;
                    }
                    Check.near(ma, KemarReference.magnitudeDb(b, f, rates[i]), 0.5, "rate " + rates[i] + " direction " + dirs[d][0] + "/" + dirs[d][1] + " at " + f + " Hz");
                }
            }
        }
    }

    /**
     * Converts a float array to doubles.
     * <p>
     * float 배열을 double 로 바꾼다.
     *
     * @param f the array / 배열
     * @return the doubles / double 배열
     */
    private static double[] toDouble(float[] f) {
        double[] d = new double[f.length];
        // 요소마다 변환한다.
        for (int i = 0; i < f.length; i++) {
            d[i] = f[i];
        }
        return d;
    }

    /**
     * The rendering kernels against the original KEMAR responses at the measured directions of the horizontal and two raised rings:
     * the magnitude response from 200 Hz to 10 kHz is within 0.5 dB of the original wherever the original is within 30 dB of its
     * peak (the kernel is the minimum phase version, and the delay is a flat fractional delay), and the interaural time difference
     * of the low frequency part (the lag of the cross-correlation of the responses low-passed at 1200 Hz) agrees within 1 sample.
     * <p>
     * 렌더링 커널을 수평 고리와 위로 올라간 두 고리의 측정 방향에서 원본 KEMAR 응답과 비교한다. 200 Hz ~ 10 kHz 의 크기 응답은 원본이 피크보다 30 dB 이내인 곳에서 원본과
     * 0.5 dB 이내로 같고(커널은 최소 위상 버전이며 지연은 평탄한 소수 지연이다), 저주파 부분의 두 귀 시간차(1200 Hz 로 저역 통과한 응답의 상호 상관 시차)는 1 샘플 이내로
     * 일치한다.
     */
    public void testRenderingKernelsAgainstTheOriginal() {
        HrtfDatabase db = HrtfDatabaseTest.db();
        HrtfSet set = new HrtfSet(db, 44100f);
        int n = set.getKernelLength();
        float[] l = new float[n];
        float[] r = new float[n];
        float[] w1 = new float[n];
        float[] w2 = new float[n];
        int[] rings = {4, 6, 9};
        double worstMagnitude = 0;
        double worstLag = 0;
        // 고리마다 측정한 방위각에서 크기와 시차를 원본과 비교한다.
        for (int i = 0; i < rings.length; i++) {
            int e = rings[i];
            // 방위각마다 처리한다.
            for (int a = 0; a < db.getAzimuthCount(e); a += 2) {
                int az = db.getAzimuth(e, a);
                double[][] original = KemarReference.get(db.getElevation(e), az);
                set.direction(az, db.getElevation(e), l, r, w1, w2);
                double[][] rendered = {toDouble(l), toDouble(r)};
                // 두 귀를 차례로 처리한다.
                for (int ear = 0; ear < 2; ear++) {
                    double peak = -1000;
                    // 주파수를 올려 가며 처리한다.
                    for (double f = 200; f <= 10000; f += 200) {
                        peak = Math.max(peak, KemarReference.magnitudeDb(original[ear], f, 44100.0));
                    }
                    // 200 Hz 간격으로 크기 응답을 원본과 비교한다.
                    for (double f = 200; f <= 10000; f += 200) {
                        double o = KemarReference.magnitudeDb(original[ear], f, 44100.0);
                        if (o < peak - 30) {
                            continue;
                        }
                        double d = Math.abs(KemarReference.magnitudeDb(rendered[ear], f, 44100.0) - o);
                        worstMagnitude = Math.max(worstMagnitude, d);
                        Check.isTrue(d < 0.5, "direction " + db.getElevation(e) + "/" + az + " ear " + ear + " at " + f + " Hz differs by " + d + " dB");
                    }
                }
                double lagOriginal = KemarReference.lagLeftLater(KemarReference.lowpass(original[0], 1200), KemarReference.lowpass(original[1], 1200));
                double lagRendered = KemarReference.lagLeftLater(KemarReference.lowpass(rendered[0], 1200), KemarReference.lowpass(rendered[1], 1200));
                worstLag = Math.max(worstLag, Math.abs(lagRendered - lagOriginal));
                Check.near(lagOriginal, lagRendered, 1.0, "interaural time difference at " + db.getElevation(e) + "/" + az);
            }
        }
        Check.isTrue(worstMagnitude < 0.5 && worstLag < 1.0, "worst magnitude " + worstMagnitude + " dB, worst lag " + worstLag + " samples");
    }
}
