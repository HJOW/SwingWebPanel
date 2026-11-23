package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The HRTF data of {@link HrtfDatabase} prepared for one sample rate, and the interpolation between the measured directions: for any
 * azimuth and elevation it produces the pair of impulse responses (the kernels of the left and the right ear) that a source in that
 * direction needs. The object is immutable after construction and may be shared by any number of renderers and threads; the caller
 * passes the work arrays.
 * <p>
 * Method (the specification of the <code>PannerNode</code> leaves the HRTF processing to the implementation):
 * <ol>
 * <li>The measured responses are stored as minimum phase kernels with a separate delay per ear (see <code>tools/hrtfgen</code>), so
 * that they can be averaged without the comb filtering that unaligned responses would cause: the weighted sum of the kernels and the
 * weighted sum of the delays are taken separately.</li>
 * <li>The data is converted to the sample rate of the context (kernels with the band-limited interpolation of {@link HrirResampler},
 * delays scaled by the ratio of the rates).</li>
 * <li>A direction is interpolated bilinearly: the azimuth between the two nearest measured azimuths of each of the two nearest
 * elevations, then between the two elevations. Negative azimuths (the left side) use the right side measurements with the ears
 * swapped. Elevations below the lowest measured one (-40 degrees) use the lowest elevation.</li>
 * <li>The delay of every ear is put into its kernel with an eight point Lagrange fractional delay, so that the result is one
 * ready-to-use FIR kernel per ear of the fixed length {@link #getKernelLength()}.</li>
 * </ol>
 * <p>
 * {@link HrtfDatabase} 의 HRTF 자료를 한 샘플링 주파수에 맞게 준비한 것과 측정한 방향 사이의 보간: 임의의 방위각과 고도에 대해 그 방향의 음원이 필요로 하는 임펄스 응답
 * 쌍(왼쪽과 오른쪽 귀의 커널)을 만든다. 객체는 만든 뒤에는 불변이라 여러 렌더러와 스레드가 공유할 수 있고 작업 배열은 호출자가 넘긴다.
 * <p>
 * 방법(<code>PannerNode</code> 규격은 HRTF 처리를 구현에 맡긴다):
 * <ol>
 * <li>측정한 응답은 귀마다 지연을 따로 둔 최소 위상 커널로 저장된다(<code>tools/hrtfgen</code> 참고). 그래서 정렬되지 않은 응답을 평균할 때 생기는 빗살 필터링 없이
 * 평균할 수 있다. 커널의 가중 합과 지연의 가중 합을 따로 구한다.</li>
 * <li>자료는 컨텍스트의 샘플링 주파수로 바꾼다(커널은 {@link HrirResampler} 의 대역 제한 보간, 지연은 속도 비로 환산).</li>
 * <li>방향은 쌍선형으로 보간한다. 가장 가까운 두 고도 각각에서 가장 가까운 두 측정 방위각 사이를, 그다음 두 고도 사이를 보간한다. 음의 방위각(왼쪽)은 귀를 바꾼
 * 오른쪽 측정을 쓴다. 가장 낮은 측정 고도(-40도) 아래의 고도는 가장 낮은 고도를 쓴다.</li>
 * <li>귀마다의 지연은 8점 라그랑주 소수 지연으로 커널에 넣는다. 그래서 결과는 귀마다 고정 길이 {@link #getKernelLength()} 의 바로 쓸 수 있는 FIR 커널 하나이다.</li>
 * </ol>
 */
public final class HrtfSet {

    /**
     * The number of taps by which both kernels start later than the measured responses (a constant latency of both ears, which does not
     * change the interaural time difference): it keeps the precursor of the fractional delay interpolation, which would be cut off if the
     * kernel had to start at the tap 0.
     * <p>
     * 두 커널이 측정한 응답보다 늦게 시작하는 탭 수(두 귀에 같은 고정 지연이므로 두 귀 사이의 시간차는 바뀌지 않는다). 소수 지연 보간의 선행 성분을 지킨다. 커널이
     * 탭 0 에서 시작해야 한다면 그 성분이 잘려 나간다.
     */
    public static final int LEAD = 4;

    /**
     * The sample rate of the set in Hz.
     * <p>
     * 세트의 샘플링 주파수(Hz).
     */
    private final float sampleRate;

    /**
     * The number of taps of the converted minimum phase kernels.
     * <p>
     * 변환한 최소 위상 커널의 탭 수.
     */
    private final int baseLength;

    /**
     * The constant latency of both ears in samples: the lead of the rate conversion and {@link #LEAD}.
     * <p>
     * 두 귀의 고정 지연(샘플): 속도 변환의 선행 탭과 {@link #LEAD}.
     */
    private final int latency;

    /**
     * The length of the final kernels: the converted kernel plus room for the largest delay.
     * <p>
     * 최종 커널의 길이: 변환한 커널에 가장 큰 지연의 공간을 더한 값.
     */
    private final int kernelLength;

    /**
     * The elevations in degrees, ascending.
     * <p>
     * 고도(도), 오름차순.
     */
    private final int[] elevations;

    /**
     * The azimuths in degrees of every elevation.
     * <p>
     * 고도마다의 방위각(도).
     */
    private final int[][] azimuths;

    /**
     * The converted kernels, [elevation][azimuth][ear][tap].
     * <p>
     * 변환한 커널 [고도][방위각][귀][탭].
     */
    private final float[][][][] kernels;

    /**
     * The delays in samples at the rate of the set, [elevation][azimuth][ear].
     * <p>
     * 세트 속도에서의 지연(샘플) [고도][방위각][귀].
     */
    private final double[][][] delays;

    /**
     * The largest delay of the set in samples.
     * <p>
     * 세트에서 가장 큰 지연(샘플).
     */
    private final double maxDelay;

    /**
     * Prepares a database for a sample rate.
     * <p>
     * 데이터베이스를 샘플링 주파수에 맞게 준비한다.
     *
     * @param db         the database / 데이터베이스
     * @param sampleRate the sample rate in Hz / 샘플링 주파수(Hz)
     */
    public HrtfSet(HrtfDatabase db, float sampleRate) {
        this.sampleRate = sampleRate;
        double ratio = sampleRate / (double) db.getSampleRate();
        HrirResampler resampler = new HrirResampler(db.getTaps(), db.getSampleRate(), sampleRate);
        this.baseLength = resampler.getOutLength();
        this.latency = resampler.getLead() + LEAD;
        int count = db.getElevationCount();
        this.elevations = new int[count];
        this.azimuths = new int[count][];
        this.kernels = new float[count][][][];
        this.delays = new double[count][][];
        double largest = 0.0;
        // 고도와 방위각마다 커널을 변환하고 지연의 속도를 환산한다.
        for (int e = 0; e < count; e++) {
            elevations[e] = db.getElevation(e);
            int n = db.getAzimuthCount(e);
            azimuths[e] = new int[n];
            kernels[e] = new float[n][2][];
            delays[e] = new double[n][2];
            // 이 고도의 방위각마다 처리한다.
            for (int a = 0; a < n; a++) {
                azimuths[e][a] = db.getAzimuth(e, a);
                // 두 귀를 처리한다.
                for (int ear = 0; ear < 2; ear++) {
                    kernels[e][a][ear] = resampler.apply(db.getKernel(e, a, ear));
                    delays[e][a][ear] = db.getDelay(e, a, ear) * ratio;
                    largest = Math.max(largest, delays[e][a][ear]);
                }
            }
        }
        this.maxDelay = largest;
        this.kernelLength = baseLength + LEAD + (int) Math.ceil(largest) + 4;
    }

    /**
     * Returns the sample rate of the set.
     * <p>
     * 세트의 샘플링 주파수를 반환한다.
     *
     * @return the sample rate / 샘플링 주파수
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Returns the length of the final kernels that {@link #direction} produces, which is also the number of frames that the output
     * lasts after the input ended.
     * <p>
     * {@link #direction} 이 만드는 최종 커널의 길이를 반환한다. 입력이 끝난 뒤 출력이 이어지는 프레임 수이기도 하다.
     *
     * @return the length in taps / 탭 수
     */
    public int getKernelLength() {
        return kernelLength;
    }

    /**
     * Returns the constant latency that both ears have in samples (the interaural time difference is not changed by it): the lead of
     * the conversion to the sample rate of the set plus {@link #LEAD}.
     * <p>
     * 두 귀가 가진 고정 지연(샘플)을 반환한다(두 귀 사이의 시간차는 이것으로 바뀌지 않는다). 세트의 샘플링 주파수로의 변환에서 생긴 선행 탭과 {@link #LEAD} 이다.
     *
     * @return the latency in samples / 지연(샘플)
     */
    public int getLatency() {
        return latency;
    }

    /**
     * Returns the largest delay of any measured direction and ear in samples.
     * <p>
     * 측정한 모든 방향과 귀 중 가장 큰 지연(샘플)을 반환한다.
     *
     * @return the delay / 지연
     */
    public double getMaxDelay() {
        return maxDelay;
    }

    /**
     * Finds the lower one of the two measured azimuths of a ring that enclose an azimuth; the last index if the azimuth is not below
     * the last measured azimuth (then there is no upper one and the weight of the upper one is 0).
     * <p>
     * 고리에서 방위각을 둘러싸는 두 측정 방위각 중 낮은 것의 인덱스를 찾는다. 방위각이 마지막 측정 방위각보다 낮지 않으면 마지막 인덱스이다(그러면 높은 쪽이 없고
     * 높은 쪽의 가중치는 0).
     *
     * @param ring the azimuths of the elevation / 고도의 방위각
     * @param az   the azimuth in degrees, 0 or more / 방위각(도). 0 이상
     * @return the index of the lower azimuth / 낮은 방위각의 인덱스
     */
    private static int bracketLow(int[] ring, double az) {
        int last = ring.length - 1;
        if (az >= ring[last] || last == 0) {
            return last;
        }
        int i = 0;
        // az 이하인 가장 큰 방위각 인덱스를 찾는다.
        while (i + 1 < last && ring[i + 1] <= az) {
            i++;
        }
        return i;
    }

    /**
     * Produces the kernels of a direction: the left and the right ear, each of {@link #getKernelLength()} taps, with the interaural
     * delays included.
     * <p>
     * 방향의 커널을 만든다. 왼쪽과 오른쪽 귀 각각 {@link #getKernelLength()} 탭이며 귀 사이의 지연을 포함한다.
     *
     * @param azimuth   the azimuth in degrees, from -180 to 180; 0 is the front, positive is to the right / 방위각(도, -180 ~ 180). 0 은 정면, 양수는 오른쪽
     * @param elevation the elevation in degrees, from -90 to 90 / 고도(도, -90 ~ 90)
     * @param left      the array for the left ear kernel, at least {@link #getKernelLength()} long / 왼쪽 귀 커널 배열(길이 {@link #getKernelLength()} 이상)
     * @param right     the array for the right ear kernel / 오른쪽 귀 커널 배열
     * @param workL     a work array of at least {@link #getKernelLength()} elements / 작업 배열(요소 {@link #getKernelLength()} 개 이상)
     * @param workR     a second work array / 두 번째 작업 배열
     */
    public void direction(double azimuth, double elevation, float[] left, float[] right, float[] workL, float[] workR) {
        double az = Math.max(-180.0, Math.min(180.0, azimuth));
        boolean mirror = az < 0.0;
        az = Math.abs(az);
        double el = Math.max(elevations[0], Math.min(elevations[elevations.length - 1], elevation));
        int e0 = 0;
        // el 이하인 가장 큰 고도 인덱스를 찾는다.
        while (e0 + 1 < elevations.length && elevations[e0 + 1] <= el) {
            e0++;
        }
        int e1 = Math.min(e0 + 1, elevations.length - 1);
        double we = e1 == e0 ? 0.0 : (el - elevations[e0]) / (double) (elevations[e1] - elevations[e0]);
        java.util.Arrays.fill(workL, 0, baseLength, 0f);
        java.util.Arrays.fill(workR, 0, baseLength, 0f);
        double delayL = 0.0;
        double delayR = 0.0;
        // 두 고도 고리 각각에서 방위각을 보간해 가중 합에 더한다.
        for (int r = 0; r < 2; r++) {
            int e = r == 0 ? e0 : e1;
            double ringWeight = r == 0 ? 1.0 - we : we;
            if (ringWeight <= 0.0) {
                continue;
            }
            int low = bracketLow(azimuths[e], az);
            int high = Math.min(low + 1, azimuths[e].length - 1);
            double wHigh = high == low ? 0.0 : (az - azimuths[e][low]) / (double) (azimuths[e][high] - azimuths[e][low]);
            // 고리의 두 방위각을 가중치대로 더한다.
            for (int s = 0; s < 2; s++) {
                int a = s == 0 ? low : high;
                double w = ringWeight * (s == 0 ? 1.0 - wHigh : wHigh);
                if (w <= 0.0) {
                    continue;
                }
                float[] kl = kernels[e][a][0];
                float[] kr = kernels[e][a][1];
                // 커널의 탭마다 가중치를 곱해 더한다.
                for (int t = 0; t < baseLength; t++) {
                    workL[t] += (float) (w * kl[t]);
                    workR[t] += (float) (w * kr[t]);
                }
                delayL += w * delays[e][a][0];
                delayR += w * delays[e][a][1];
            }
        }
        // 왼쪽 방위각은 오른쪽 측정에서 귀를 바꿔 얻는다.
        if (mirror) {
            float[] tmp = workL;
            double d = delayL;
            delayL = delayR;
            delayR = d;
            delayed(workR, delayL, left);
            delayed(tmp, delayR, right);
        } else {
            delayed(workL, delayL, left);
            delayed(workR, delayR, right);
        }
    }

    /**
     * Delays a kernel by a fractional number of samples with an eight point (seventh order) Lagrange interpolation: the output at the
     * tap <code>k</code> is the interpolated value of the input at the position <code>k - delay - LEAD</code>, and zero outside the input.
     * The interpolation is flat (within 0.15 dB) up to 0.22 of the sample rate for every fraction (measured in HrtfSetTest), where a four
     * point one would lose more than 1 dB at 12 kHz for a delay of half a sample.
     * <p>
     * 커널을 소수 샘플만큼 8점(7차) 라그랑주 보간으로 지연한다. 탭 <code>k</code> 의 출력은 입력의 <code>k - delay - LEAD</code> 위치의 보간 값이고 입력 밖은 0 이다.
     * 보간은 모든 소수 값에서 샘플링 주파수의 0.22 배까지 평탄하다(0.15 dB 이내, HrtfSetTest 에서 측정). 4점 보간은 반 샘플 지연에서 12 kHz 에 1 dB 넘는 손실이 있다.
     *
     * @param h     the kernel with {@link #baseLength} taps / {@link #baseLength} 탭의 커널
     * @param delay the delay in samples, 0 or more / 지연(샘플). 0 이상
     * @param out   the output with {@link #getKernelLength()} taps / {@link #getKernelLength()} 탭의 출력
     */
    private void delayed(float[] h, double delay, float[] out) {
        double shift = -(delay + LEAD);
        int base = (int) Math.floor(shift);
        double t = shift - base;
        // 소수 부분이 모든 탭에 같으므로 가중치 8개를 한 번만 계산한다(노드 -3 ~ 4).
        double w0 = lagrange(t, -3);
        double w1 = lagrange(t, -2);
        double w2 = lagrange(t, -1);
        double w3 = lagrange(t, 0);
        double w4 = lagrange(t, 1);
        double w5 = lagrange(t, 2);
        double w6 = lagrange(t, 3);
        double w7 = lagrange(t, 4);
        // 출력 탭마다 입력의 k - delay 위치 둘레 8개 탭을 가중해 더한다.
        for (int k = 0; k < kernelLength; k++) {
            int i = k + base;
            out[k] = (float) (w0 * at(h, i - 3) + w1 * at(h, i - 2) + w2 * at(h, i - 1) + w3 * at(h, i) + w4 * at(h, i + 1) + w5 * at(h, i + 2)
                    + w6 * at(h, i + 3) + w7 * at(h, i + 4));
        }
    }

    /**
     * The Lagrange weight of the node <code>j</code> at the position <code>t</code> for the nodes -3 to 4: the product of
     * <code>(t - m) / (j - m)</code> over the other nodes <code>m</code>.
     * <p>
     * 노드 -3 ~ 4 에 대한 위치 <code>t</code> 에서의 노드 <code>j</code> 의 라그랑주 가중치: 다른 노드 <code>m</code> 에 대한 <code>(t - m) / (j - m)</code> 의 곱.
     *
     * @param t the position / 위치
     * @param j the node / 노드
     * @return the weight / 가중치
     */
    private static double lagrange(double t, int j) {
        double w = 1.0;
        // 다른 노드마다 인수를 곱한다.
        for (int m = -3; m <= 4; m++) {
            if (m != j) {
                w *= (t - m) / (j - m);
            }
        }
        return w;
    }

    /**
     * Returns a tap of a kernel, or 0 outside of it.
     * <p>
     * 커널의 탭을 반환한다. 범위 밖이면 0.
     *
     * @param h the kernel / 커널
     * @param i the index / 인덱스
     * @return the value / 값
     */
    private float at(float[] h, int i) {
        return i < 0 || i >= baseLength ? 0f : h[i];
    }
}
