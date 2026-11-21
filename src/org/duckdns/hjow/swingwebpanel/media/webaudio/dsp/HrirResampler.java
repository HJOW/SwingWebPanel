package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * Converts head-related impulse responses from one sample rate to another with a windowed band-limited (sinc) interpolation. It exists
 * only for the HRTF data of {@link HrtfSet}, which is measured at one rate and is needed at the rate of the audio context; it is not a
 * general audio resampler.
 * <p>
 * The frequency response is kept: with the positions <code>p_k = k fsOut / fsIn</code> of the input taps in the output sample grid and
 * the cutoff factor <code>c = min(1, fsIn / fsOut)</code>, an impulse response <code>h[k]</code> at the rate <code>fsIn</code> becomes
 * <code>h'[m] = sum_k h[k] c sinc(c (m - p_k)) w((m - p_k) / W)</code> at the rate <code>fsOut</code>, with a Hann window <code>w</code>
 * of the half width <code>W = 10 / c</code> output samples (ten zero crossings of the sinc on each side). The response above half of the
 * lower rate is dropped. The window is wide enough that the cut-off of the sinc precursor and tail, which every band-limiting
 * conversion has, is kept: the result has <code>lead = ceil(W)</code> extra taps in front of the converted response and as many behind
 * it (the output starts <code>lead</code> samples later than the input: a constant latency of every converted response, which does not
 * change the difference between the two ears). The conversion for equal rates is the identity without extra taps. Because all the
 * responses of a data set are converted with the same factors, the conversion is prepared once as a matrix.
 * <p>
 * 머리전달 임펄스 응답을 창을 씌운 대역 제한(sinc) 보간으로 한 샘플링 주파수에서 다른 샘플링 주파수로 바꾼다. 한 속도로 측정되어 오디오 컨텍스트의 속도로 필요한
 * {@link HrtfSet} 의 HRTF 자료만을 위한 것이며 범용 오디오 리샘플러가 아니다.
 * <p>
 * 주파수 응답은 유지된다. 입력 탭의 출력 샘플 격자 위치를 <code>p_k = k fsOut / fsIn</code>, 차단 계수를 <code>c = min(1, fsIn / fsOut)</code> 라 하면 속도
 * <code>fsIn</code> 의 임펄스 응답 <code>h[k]</code> 는 속도 <code>fsOut</code> 에서 <code>h'[m] = sum_k h[k] c sinc(c (m - p_k)) w((m - p_k) / W)</code> 가
 * 된다. <code>w</code> 는 반폭 <code>W = 10 / c</code> 출력 샘플의 Hann 창이다(양쪽에 sinc 의 영점 교차 열 번). 낮은 속도의 절반 위의 응답은 버린다. 창이
 * 충분히 넓어서 모든 대역 제한 변환이 가진 sinc 의 선행과 후행 성분이 잘려 나가지 않는다. 결과는 변환한 응답 앞뒤에 <code>lead = ceil(W)</code> 탭씩 더 가진다(출력이
 * 입력보다 <code>lead</code> 샘플 늦게 시작한다. 변환한 모든 응답에 같은 고정 지연이므로 두 귀 사이의 차이는 바뀌지 않는다). 같은 속도의 변환은 추가 탭이 없는
 * 항등이다. 자료의 모든 응답을 같은 계수로 바꾸므로 변환을 행렬로 한 번 준비한다.
 */
final class HrirResampler {

    /**
     * The number of zero crossings of the sinc on each side that the window keeps.
     * <p>
     * 창이 양쪽에서 남기는 sinc 의 영점 교차 수.
     */
    private static final double ZERO_CROSSINGS = 10.0;

    /**
     * The conversion matrix, [output tap][input tap].
     * <p>
     * 변환 행렬 [출력 탭][입력 탭].
     */
    private final float[][] matrix;

    /**
     * The number of taps of the converted responses.
     * <p>
     * 변환한 응답의 탭 수.
     */
    private final int outLength;

    /**
     * The number of extra taps in front of the converted response (and behind it).
     * <p>
     * 변환한 응답 앞(과 뒤)의 추가 탭 수.
     */
    private final int lead;

    /**
     * Prepares the conversion.
     * <p>
     * 변환을 준비한다.
     *
     * @param inLength the number of taps of the input responses / 입력 응답의 탭 수
     * @param fsIn     the sample rate of the input / 입력의 샘플링 주파수
     * @param fsOut    the sample rate of the output / 출력의 샘플링 주파수
     */
    HrirResampler(int inLength, double fsIn, double fsOut) {
        boolean same = fsIn == fsOut;
        double c = Math.min(1.0, fsIn / fsOut);
        double half = ZERO_CROSSINGS / c;
        this.lead = same ? 0 : (int) Math.ceil(half);
        this.outLength = (int) Math.ceil(inLength * fsOut / fsIn - 1e-9) + 2 * lead;
        this.matrix = new float[outLength][inLength];
        // 출력 탭마다(앞쪽 lead 탭은 음수 시간) 입력 탭 위치와의 거리로 창을 씌운 sinc 가중치를 채운다.
        for (int j = 0; j < outLength; j++) {
            int m = j - lead;
            // 입력 탭마다 가중치를 계산한다.
            for (int k = 0; k < inLength; k++) {
                double u = m - k * fsOut / fsIn;
                double window = same ? 1.0 : (Math.abs(u) < half ? 0.5 * (1.0 + Math.cos(Math.PI * u / half)) : 0.0);
                matrix[j][k] = (float) (c * sinc(c * u) * window);
            }
        }
    }

    /**
     * The normalized sinc function, <code>sin(pi x) / (pi x)</code>, exactly 1 at 0 and 0 at the other integers.
     * <p>
     * 정규화한 sinc 함수 <code>sin(pi x) / (pi x)</code>. 0 에서 정확히 1 이고 다른 정수에서 0 이다.
     *
     * @param x the argument / 인수
     * @return the value / 값
     */
    private static double sinc(double x) {
        if (Math.abs(x) < 1e-12) {
            return 1.0;
        }
        double r = Math.rint(x);
        if (Math.abs(x - r) < 1e-12) {
            return 0.0;
        }
        return Math.sin(Math.PI * x) / (Math.PI * x);
    }

    /**
     * Returns the number of taps of the converted responses.
     * <p>
     * 변환한 응답의 탭 수를 반환한다.
     *
     * @return the number of taps / 탭 수
     */
    int getOutLength() {
        return outLength;
    }

    /**
     * Returns the number of extra taps in front of the converted response.
     * <p>
     * 변환한 응답 앞의 추가 탭 수를 반환한다.
     *
     * @return the number of taps / 탭 수
     */
    int getLead() {
        return lead;
    }

    /**
     * Converts one response.
     * <p>
     * 응답 하나를 변환한다.
     *
     * @param in the input response / 입력 응답
     * @return the converted response / 변환한 응답
     */
    float[] apply(float[] in) {
        float[] out = new float[outLength];
        // 출력 탭마다 행과 입력의 곱을 더한다.
        for (int m = 0; m < outLength; m++) {
            double s = 0.0;
            float[] row = matrix[m];
            // 입력 탭마다 가중치와 값을 곱해 더한다.
            for (int k = 0; k < in.length; k++) {
                s += row[k] * in[k];
            }
            out[m] = (float) s;
        }
        return out;
    }
}
