package org.duckdns.hjow.swingwebpanel.js.host.canvas;

/**
 * The Gaussian blur of canvas shadows: <code>shadowBlur</code> is twice the standard deviation of the Gaussian that is applied to the alpha
 * of the shadow. The blur is separable (one horizontal and one vertical pass over a float array) so its cost grows with the radius and not
 * with its square.
 * <p>
 * 캔버스 그림자의 가우시안 번짐: <code>shadowBlur</code> 는 그림자의 알파에 적용하는 가우시안 표준편차의 두 배이다. 번짐은 분리 가능하여(float 배열에
 * 가로와 세로 한 번씩) 비용이 반지름의 제곱이 아니라 반지름에 비례한다.
 */
final class ShadowBlur {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ShadowBlur() {
    }

    /**
     * Returns the number of pixels around a shape that a blur of a given <code>shadowBlur</code> can reach (three standard deviations).
     * <p>
     * 주어진 <code>shadowBlur</code> 의 번짐이 도형 둘레로 닿는 화소 수(표준편차의 세 배)를 반환한다.
     *
     * @param blur the value of <code>shadowBlur</code> / <code>shadowBlur</code> 값
     * @return the extent in pixels / 번짐 범위(화소)
     */
    static int extent(double blur) {
        return blur <= 0 ? 0 : (int) Math.ceil(blur / 2 * 3);
    }

    /**
     * Blurs the values of an array that holds a rectangle of <code>w * h</code> alpha values in place; values outside the rectangle count as 0.
     * <p>
     * <code>w * h</code> 개의 알파 값이 든 사각형 배열을 제자리에서 번지게 한다. 사각형 밖의 값은 0 으로 본다.
     *
     * @param alpha the alpha values 0..255 / 알파 값 0..255
     * @param w     the width / 너비
     * @param h     the height / 높이
     * @param blur  the value of <code>shadowBlur</code> / <code>shadowBlur</code> 값
     */
    static void blur(float[] alpha, int w, int h, double blur) {
        double sigma = blur / 2;
        if (sigma <= 0 || w <= 0 || h <= 0) {
            return;
        }
        int radius = (int) Math.ceil(sigma * 3);
        float[] kernel = new float[radius * 2 + 1];
        double sum = 0;
        // 가우시안 가중치를 계산한다.
        for (int i = -radius; i <= radius; i++) {
            double v = Math.exp(-(i * i) / (2 * sigma * sigma));
            kernel[i + radius] = (float) v;
            sum += v;
        }
        // 가중치의 합이 1 이 되도록 정규화한다.
        for (int i = 0; i < kernel.length; i++) {
            kernel[i] = (float) (kernel[i] / sum);
        }
        float[] temp = new float[alpha.length];
        // 가로 방향으로 한 번 번지게 한다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float acc = 0;
                int from = Math.max(0, x - radius);
                int to = Math.min(w - 1, x + radius);
                // 커널이 닿는 이웃 화소의 가중합을 구한다.
                for (int k = from; k <= to; k++) {
                    acc += alpha[y * w + k] * kernel[k - x + radius];
                }
                temp[y * w + x] = acc;
            }
        }
        // 세로 방향으로 한 번 번지게 한다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float acc = 0;
                int from = Math.max(0, y - radius);
                int to = Math.min(h - 1, y + radius);
                // 세로로 이웃한 화소의 가중합을 구한다.
                for (int k = from; k <= to; k++) {
                    acc += temp[k * w + x] * kernel[k - y + radius];
                }
                alpha[y * w + x] = acc;
            }
        }
    }
}
