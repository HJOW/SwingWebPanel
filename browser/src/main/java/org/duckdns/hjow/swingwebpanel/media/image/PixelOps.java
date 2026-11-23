package org.duckdns.hjow.swingwebpanel.media.image;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Small pixel and safety helpers shared by the image decoders: EXIF orientation, alpha compositing, byte reading with bounds checks and the
 * cancellation check. All pixels are non-premultiplied <code>0xAARRGGBB</code>.
 * <p>
 * 이미지 디코더들이 함께 쓰는 작은 픽셀/안전 도우미: EXIF 방향, 알파 합성, 범위를 확인하는 바이트 읽기, 취소 확인. 모든 픽셀은 알파를 미리 곱하지
 * 않은 <code>0xAARRGGBB</code> 이다.
 */
final class PixelOps {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PixelOps() {
    }

    /**
     * Throws if the current thread was interrupted (a navigation was cancelled); the interrupt flag stays set.
     * <p>
     * 현재 스레드가 인터럽트되었으면(이동이 취소됨) 예외를 던진다. 인터럽트 플래그는 켜진 채로 둔다.
     *
     * @throws MediaException CANCELLED if interrupted / 인터럽트되었으면 CANCELLED
     */
    static void checkCancelled() throws MediaException {
        if (Thread.currentThread().isInterrupted()) {
            throw new MediaException(MediaException.Kind.CANCELLED, "decoding was cancelled");
        }
    }

    /**
     * Reads an unsigned 16-bit big-endian value, failing with CORRUPT if the data is too short.
     * <p>
     * 부호 없는 16비트 빅 엔디언 값을 읽으며 데이터가 짧으면 CORRUPT 로 실패한다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @return value / 값
     * @throws MediaException CORRUPT on short data / 데이터가 짧으면 CORRUPT
     */
    static int u16be(byte[] d, int pos) throws MediaException {
        need(d, pos, 2);
        return ((d[pos] & 0xFF) << 8) | (d[pos + 1] & 0xFF);
    }

    /**
     * Reads an unsigned 16-bit little-endian value, failing with CORRUPT if the data is too short.
     * <p>
     * 부호 없는 16비트 리틀 엔디언 값을 읽으며 데이터가 짧으면 CORRUPT 로 실패한다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @return value / 값
     * @throws MediaException CORRUPT on short data / 데이터가 짧으면 CORRUPT
     */
    static int u16le(byte[] d, int pos) throws MediaException {
        need(d, pos, 2);
        return (d[pos] & 0xFF) | ((d[pos + 1] & 0xFF) << 8);
    }

    /**
     * Reads a 32-bit big-endian value as an unsigned number, failing with CORRUPT if the data is too short.
     * <p>
     * 32비트 빅 엔디언 값을 부호 없는 수로 읽으며 데이터가 짧으면 CORRUPT 로 실패한다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @return value (0 to 4294967295) / 값(0~4294967295)
     * @throws MediaException CORRUPT on short data / 데이터가 짧으면 CORRUPT
     */
    static long u32be(byte[] d, int pos) throws MediaException {
        need(d, pos, 4);
        return ((long) (d[pos] & 0xFF) << 24) | ((d[pos + 1] & 0xFF) << 16) | ((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF);
    }

    /**
     * Reads a 32-bit little-endian value as a signed number, failing with CORRUPT if the data is too short.
     * <p>
     * 32비트 리틀 엔디언 값을 부호 있는 수로 읽으며 데이터가 짧으면 CORRUPT 로 실패한다.
     *
     * @param d   data / 데이터
     * @param pos position / 위치
     * @return value / 값
     * @throws MediaException CORRUPT on short data / 데이터가 짧으면 CORRUPT
     */
    static int s32le(byte[] d, int pos) throws MediaException {
        need(d, pos, 4);
        return (d[pos] & 0xFF) | ((d[pos + 1] & 0xFF) << 8) | ((d[pos + 2] & 0xFF) << 16) | ((d[pos + 3] & 0xFF) << 24);
    }

    /**
     * Checks that a range lies inside the data.
     * <p>
     * 범위가 데이터 안에 있는지 확인한다.
     *
     * @param d   data / 데이터
     * @param pos start / 시작
     * @param n   length / 길이
     * @throws MediaException CORRUPT if the range is outside / 범위 밖이면 CORRUPT
     */
    static void need(byte[] d, long pos, long n) throws MediaException {
        if (pos < 0 || n < 0 || pos + n > d.length) {
            throw new MediaException(MediaException.Kind.CORRUPT, "unexpected end of data");
        }
    }

    /**
     * Composites a non-premultiplied source pixel over a destination pixel (the PNG/APNG "over" operator).
     * <p>
     * 알파를 미리 곱하지 않은 원본 픽셀을 대상 픽셀 위에 합성한다(PNG/APNG 의 "over" 연산).
     *
     * @param src source pixel / 원본 픽셀
     * @param dst destination pixel / 대상 픽셀
     * @return composed pixel / 합성된 픽셀
     */
    static int over(int src, int dst) {
        int sa = src >>> 24;
        if (sa == 255) {
            return src;
        }
        if (sa == 0) {
            return dst;
        }
        int da = dst >>> 24;
        if (da == 0) {
            return src;
        }
        // 출력 알파(255 배 단위)와 각 색 성분의 가중 평균을 정수로 계산한다.
        int oa = sa * 255 + da * (255 - sa);
        int r = (((src >> 16) & 0xFF) * sa * 255 + ((dst >> 16) & 0xFF) * da * (255 - sa) + oa / 2) / oa;
        int g = (((src >> 8) & 0xFF) * sa * 255 + ((dst >> 8) & 0xFF) * da * (255 - sa) + oa / 2) / oa;
        int b = ((src & 0xFF) * sa * 255 + (dst & 0xFF) * da * (255 - sa) + oa / 2) / oa;
        int a = (oa + 127) / 255;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Applies an EXIF orientation (1 to 8) to a pixel array. For orientations 5 to 8 width and height are swapped. Orientation 1 (and
     * any value outside 1 to 8) returns the same array.
     * <p>
     * 픽셀 배열에 EXIF 방향(1~8)을 적용한다. 방향 5~8 은 가로와 세로가 바뀐다. 방향 1(과 1~8 밖의 값)은 같은 배열을 반환한다.
     *
     * @param src         source pixels / 원본 픽셀
     * @param w           width / 가로
     * @param h           height / 세로
     * @param orientation EXIF orientation value / EXIF 방향 값
     * @param outSize     receives the resulting width and height in elements 0 and 1 / 결과 가로와 세로를 0, 1 번 요소로 받는다
     * @return the pixels in display orientation / 표시 방향의 픽셀
     * @throws MediaException CANCELLED if interrupted / 인터럽트되면 CANCELLED
     */
    static int[] orient(int[] src, int w, int h, int orientation, int[] outSize) throws MediaException {
        return orient(src, w, h, orientation, outSize, true);
    }

    /**
     * Applies an EXIF orientation like {@link #orient(int[], int, int, int, int[])}, optionally without looking at the interrupt flag (for
     * code on the painting side that has to finish).
     * <p>
     * {@link #orient(int[], int, int, int, int[])} 처럼 EXIF 방향을 적용하되, 원하면 인터럽트 표시를 보지 않는다(그리는 쪽에서 돌며 반드시 끝나야 하는 코드용).
     *
     * @param src         source pixels / 원본 픽셀
     * @param w           width / 가로
     * @param h           height / 세로
     * @param orientation EXIF orientation value / EXIF 방향 값
     * @param outSize     receives the resulting width and height in elements 0 and 1 / 결과 가로와 세로를 0, 1 번 요소로 받는다
     * @param cancellable true to throw CANCELLED when the thread is interrupted / 스레드가 인터럽트되면 CANCELLED 를 던지려면 true
     * @return the pixels in display orientation / 표시 방향의 픽셀
     * @throws MediaException CANCELLED if interrupted and cancellable / 인터럽트되고 cancellable 이면 CANCELLED
     */
    static int[] orient(int[] src, int w, int h, int orientation, int[] outSize, boolean cancellable) throws MediaException {
        outSize[0] = w;
        outSize[1] = h;
        if (orientation < 2 || orientation > 8) {
            return src;
        }
        boolean swap = orientation >= 5;
        int ow = swap ? h : w;
        int oh = swap ? w : h;
        int[] dst = new int[w * h];
        // 출력 좌표마다 원본 좌표를 계산해 복사한다.
        for (int dy = 0; dy < oh; dy++) {
            if (cancellable && (dy & 63) == 0) {
                checkCancelled();
            }
            // 각 항목을 차례로 처리한다.
            for (int dx = 0; dx < ow; dx++) {
                int sx;
                int sy;
                switch (orientation) {
                case 2:
                    sx = w - 1 - dx;
                    sy = dy;
                    break;
                case 3:
                    sx = w - 1 - dx;
                    sy = h - 1 - dy;
                    break;
                case 4:
                    sx = dx;
                    sy = h - 1 - dy;
                    break;
                case 5:
                    sx = dy;
                    sy = dx;
                    break;
                case 6:
                    sx = dy;
                    sy = h - 1 - dx;
                    break;
                case 7:
                    sx = w - 1 - dy;
                    sy = h - 1 - dx;
                    break;
                default:
                    sx = w - 1 - dy;
                    sy = dx;
                    break;
                }
                dst[dy * ow + dx] = src[sy * w + sx];
            }
        }
        outSize[0] = ow;
        outSize[1] = oh;
        return dst;
    }
}
