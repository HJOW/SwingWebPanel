package org.duckdns.hjow.swingwebpanel.media.image.webp;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Decoder of the <code>ALPH</code> chunk, the alpha plane that accompanies a lossy WebP image (plan item P18-03). The first byte describes
 * the compression (none, or a headerless lossless stream whose green channel is the alpha value), the prediction filter (none, horizontal,
 * vertical, gradient) that the encoder applied to the plane and a pre-processing hint that does not matter for decoding. The decoder
 * reverses the filter so that the result is the plain alpha value of each pixel.
 * <p>
 * <code>ALPH</code> 청크, 곧 손실 WebP 이미지에 딸린 알파 평면의 디코더(계획 P18-03). 첫 바이트는 압축 방식(없음, 또는 녹색 채널이 알파 값인 헤더 없는
 * 무손실 스트림), 인코더가 평면에 적용한 예측 필터(없음, 가로, 세로, 기울기), 디코딩에는 상관없는 전처리 힌트를 나타낸다. 디코더는 필터를 되돌려
 * 결과가 픽셀별 알파 값 그대로가 되게 한다.
 */
final class WebpAlpha {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private WebpAlpha() {
    }

    /**
     * Decodes an alpha chunk.
     * <p>
     * 알파 청크를 디코딩한다.
     *
     * @param data   data / 데이터
     * @param off    start of the chunk data / 청크 데이터 시작
     * @param len    length of the chunk data / 청크 데이터 길이
     * @param width  width of the image / 이미지 가로
     * @param height height of the image / 이미지 세로
     * @return one alpha byte per pixel, row by row / 픽셀마다 알파 바이트 하나, 행 순서
     * @throws MediaException CORRUPT if the chunk is damaged, CANCELLED if interrupted / 청크가 손상되면 CORRUPT, 인터럽트되면 CANCELLED
     */
    static byte[] decode(byte[] data, int off, int len, int width, int height) throws MediaException {
        if (len < 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the alpha chunk is empty");
        }
        int header = data[off] & 0xff;
        int method = header & 3;
        int filter = (header >> 2) & 3;
        int n = width * height;
        byte[] alpha = new byte[n];
        if (method == 0) {
            // 압축 없음: 헤더 뒤에 픽셀 수만큼의 바이트가 온다.
            if (len - 1 < n) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the alpha plane is truncated");
            }
            System.arraycopy(data, off + 1, alpha, 0, n);
        } else if (method == 1) {
            // 무손실 압축: 헤더 없는 VP8L 스트림이며 알파 값은 녹색 채널에 있다.
            int[] argb = Vp8lDecoder.decodeHeaderless(data, off + 1, len - 1, width, height);
            for (int i = 0; i < n; i++) {
                alpha[i] = (byte) (argb[i] >> 8);
            }
        } else {
            throw new MediaException(MediaException.Kind.CORRUPT, "unknown alpha compression " + method);
        }
        unfilter(alpha, width, height, filter);
        return alpha;
    }

    /**
     * Reverses the prediction filter in place.
     * <p>
     * 예측 필터를 제자리에서 되돌린다.
     *
     * @param a      alpha plane / 알파 평면
     * @param w      width / 가로
     * @param h      height / 세로
     * @param filter 0 none, 1 horizontal, 2 vertical, 3 gradient / 0 없음, 1 가로, 2 세로, 3 기울기
     */
    private static void unfilter(byte[] a, int w, int h, int filter) {
        if (filter == 0) {
            return;
        }
        // 첫 행은 어느 필터든 왼쪽 값으로 예측한다(첫 픽셀은 0).
        for (int x = 1; x < w; x++) {
            a[x] = (byte) (a[x] + a[x - 1]);
        }
        // 둘째 행부터는 필터에 따라 예측한다.
        for (int y = 1; y < h; y++) {
            int row = y * w;
            a[row] = (byte) (a[row] + a[row - w]);
            // 한 행의 나머지 픽셀을 처리한다.
            for (int x = 1; x < w; x++) {
                int pred;
                if (filter == 1) {
                    pred = a[row + x - 1] & 0xff;
                } else if (filter == 2) {
                    pred = a[row + x - w] & 0xff;
                } else {
                    int g = (a[row + x - 1] & 0xff) + (a[row + x - w] & 0xff) - (a[row + x - w - 1] & 0xff);
                    pred = g < 0 ? 0 : (g > 255 ? 255 : g);
                }
                a[row + x] = (byte) (a[row + x] + pred);
            }
        }
    }
}
