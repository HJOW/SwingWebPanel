package org.duckdns.hjow.swingwebpanel.media.audio.reference;

/**
 * Comparison of the PCM that the product decoded with the reference PCM of ffmpeg: error statistics, the downmix of more than two channels by speaker
 * role (written independently of the product's mixer from its documented policy) and the search for a time shift that explains a large error.
 * <p>
 * 제품이 디코딩한 PCM 과 ffmpeg 의 참조 PCM 의 비교: 오차 통계, 두 개를 넘는 채널의 스피커 역할별 다운믹스(제품의 믹서와 별개로 문서화된 정책에서 직접
 * 작성), 큰 오차를 설명하는 시간 이동의 탐색.
 */
public final class ReferenceCompare {

    /**
     * Weight of a centre or rear centre channel on each side (-3 dB).
     * <p>
     * 중앙/뒤 중앙 채널이 양쪽에 주는 가중치(-3 dB).
     */
    private static final double HALF_POWER = 0.7071067811865476;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ReferenceCompare() {
    }

    /**
     * Error statistics of one comparison.
     * <p>
     * 비교 한 번의 오차 통계.
     */
    public static final class Diff {

        /**
         * Number of sample frames compared (the shorter of the two).
         * <p>
         * 비교한 샘플 프레임 수(둘 중 짧은 쪽).
         */
        public int frames;

        /**
         * Largest absolute difference of a sample, in units of the 16-bit least significant bit.
         * <p>
         * 샘플 하나의 최대 절대 차이(16비트 최하위 비트 단위).
         */
        public int maxAbs;

        /**
         * Index of the sample with the largest difference.
         * <p>
         * 차이가 가장 큰 샘플의 위치.
         */
        public int maxAt;

        /**
         * Root mean square of the difference, in least significant bits.
         * <p>
         * 차이의 제곱 평균 제곱근(최하위 비트 단위).
         */
        public double rms;

        /**
         * Root mean square of the reference signal, in least significant bits.
         * <p>
         * 참조 신호의 제곱 평균 제곱근(최하위 비트 단위).
         */
        public double refRms;

        /**
         * Number of samples that differ at all.
         * <p>
         * 조금이라도 다른 샘플 수.
         */
        public long differing;

        /**
         * Signal to error ratio in decibels (999 if the signals are identical).
         * <p>
         * 신호 대 오차비(dB). 같으면 999.
         */
        public double snr() {
            return rms == 0 ? 999.0 : 20.0 * Math.log10(refRms / rms);
        }

        /**
         * Describes the statistics in one line.
         * <p>
         * 통계를 한 줄로 설명한다.
         *
         * @return the description / 설명
         */
        @Override
        public String toString() {
            return String.format("max=%d rms=%.4f snr=%.1fdB differing=%d/%d", maxAbs, rms, snr(), differing, (long) frames);
        }
    }

    /**
     * Compares two interleaved sample arrays over their common length.
     * <p>
     * 인터리브된 두 샘플 배열을 공통 길이에서 비교한다.
     *
     * @param ours our decoder's samples / 우리 디코더의 샘플
     * @param ref  the reference samples / 참조 샘플
     * @return the statistics (the sample count is in {@link Diff#frames}, counted in samples) / 통계(샘플 수는 {@link Diff#frames}, 샘플 단위)
     */
    public static Diff compare(short[] ours, short[] ref) {
        Diff d = new Diff();
        int n = Math.min(ours.length, ref.length);
        double sumErr = 0;
        double sumRef = 0;
        // 샘플마다 차이를 누적한다.
        for (int i = 0; i < n; i++) {
            int e = ours[i] - ref[i];
            if (e != 0) {
                d.differing++;
                int a = Math.abs(e);
                if (a > d.maxAbs) {
                    d.maxAbs = a;
                    d.maxAt = i;
                }
            }
            sumErr += (double) e * e;
            sumRef += (double) ref[i] * ref[i];
        }
        d.frames = n;
        d.rms = n == 0 ? 0 : Math.sqrt(sumErr / n);
        d.refRms = n == 0 ? 0 : Math.sqrt(sumRef / n);
        return d;
    }

    /**
     * Mixes the channels of a reference down to stereo from the speaker roles, with the documented policy of the product: front left/right keep their
     * side, centre and rear centre go to both sides at -3 dB, rear and side channels go to their side at -3 dB, LFE is dropped, and each side is divided
     * by the larger of the two sums of weights so that full scale is never exceeded.
     * <p>
     * 참조의 채널을 스피커 역할에서 스테레오로 섞는다. 제품의 문서화된 정책 그대로: 앞 왼쪽/오른쪽은 자기 쪽, 중앙과 뒤 중앙은 -3 dB 로 양쪽,
     * 뒤/옆 채널은 -3 dB 로 자기 쪽, LFE 는 버리고, 각 쪽은 두 가중치 합 중 큰 값으로 나누어 풀 스케일을 넘지 않게 한다.
     *
     * @param pcm   interleaved samples of all channels / 모든 채널의 인터리브된 샘플
     * @param roles the roles of the channels in order / 채널의 역할(순서대로)
     * @return interleaved stereo samples / 인터리브된 스테레오 샘플
     */
    public static short[] downmix(short[] pcm, String[] roles) {
        int ch = roles.length;
        double[] wl = new double[ch];
        double[] wr = new double[ch];
        double sl = 0;
        double sr = 0;
        // 역할마다 왼쪽/오른쪽 가중치를 정한다.
        for (int c = 0; c < ch; c++) {
            String r = roles[c];
            if (r.equals("FL")) {
                wl[c] = 1;
            } else if (r.equals("FR")) {
                wr[c] = 1;
            } else if (r.equals("FC") || r.equals("BC")) {
                wl[c] = HALF_POWER;
                wr[c] = HALF_POWER;
            } else if (r.equals("BL") || r.equals("SL")) {
                wl[c] = HALF_POWER;
            } else if (r.equals("BR") || r.equals("SR")) {
                wr[c] = HALF_POWER;
            }
            sl += wl[c];
            sr += wr[c];
        }
        double scale = 1.0 / Math.max(sl, sr);
        int frames = pcm.length / ch;
        short[] out = new short[frames * 2];
        // 프레임마다 두 쪽의 합을 구한다.
        for (int i = 0; i < frames; i++) {
            double l = 0;
            double r = 0;
            // 채널마다 가중치를 곱해 두 쪽에 더한다.
            for (int c = 0; c < ch; c++) {
                double v = pcm[i * ch + c];
                l += wl[c] * v;
                r += wr[c] * v;
            }
            out[2 * i] = clip(Math.floor(l * scale + 0.5));
            out[2 * i + 1] = clip(Math.floor(r * scale + 0.5));
        }
        return out;
    }

    /**
     * Clips a value to the 16-bit range.
     * <p>
     * 값을 16비트 범위로 자른다.
     *
     * @param v the value / 값
     * @return the clipped sample / 자른 샘플
     */
    private static short clip(double v) {
        if (v > 32767) {
            return 32767;
        }
        if (v < -32768) {
            return -32768;
        }
        return (short) v;
    }

    /**
     * Finds the time shift (in frames) of our samples against the reference that gives the smallest error, to explain a large difference as a delay.
     * A positive value means that our output starts later (it has to be moved earlier to match).
     * <p>
     * 참조에 대한 우리 샘플의 시간 이동(프레임 단위) 중 오차가 가장 작은 값을 찾아 큰 차이를 지연으로 설명한다. 양수는 우리 출력이 더 늦게
     * 시작한다는 뜻이다(맞추려면 앞으로 당겨야 한다).
     *
     * @param ours     our samples / 우리 샘플
     * @param ref      the reference samples / 참조 샘플
     * @param channels channels per frame / 프레임당 채널 수
     * @param maxLag   the largest shift to try, in frames / 시도할 최대 이동(프레임)
     * @return the best shift in frames / 가장 좋은 이동(프레임)
     */
    public static int bestLag(short[] ours, short[] ref, int channels, int maxLag) {
        int best = 0;
        double bestErr = Double.MAX_VALUE;
        int frames = Math.min(ours.length, ref.length) / channels;
        // 이동 후보마다 앞쪽 일부 구간의 평균 제곱 오차를 구한다.
        for (int lag = -maxLag; lag <= maxLag; lag++) {
            double err = 0;
            int count = 0;
            int from = Math.max(0, -lag);
            int to = Math.min(frames, frames - lag);
            // 앞쪽 일부 프레임만 세 프레임 간격으로 본다.
            for (int i = from; i < to && count < 6000; i += 3) {
                // 채널마다 제곱 오차를 더한다.
                for (int c = 0; c < channels; c++) {
                    double e = ours[(i + lag) * channels + c] - ref[i * channels + c];
                    err += e * e;
                }
                count++;
            }
            if (count > 0 && err / count < bestErr) {
                bestErr = err / count;
                best = lag;
            }
        }
        return best;
    }
}
