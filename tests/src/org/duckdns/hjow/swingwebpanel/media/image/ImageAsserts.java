package org.duckdns.hjow.swingwebpanel.media.image;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Assertion helpers shared by the image decoder tests.
 * <p>
 * 이미지 디코더 시험들이 함께 쓰는 검사 도우미.
 */
final class ImageAsserts {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ImageAsserts() {
    }

    /**
     * Checks the size and every pixel of the first frame exactly.
     * <p>
     * 크기와 첫 프레임의 모든 픽셀이 정확히 같은지 검사한다.
     *
     * @param img      decoded image / 디코딩된 이미지
     * @param w        expected width / 기대 가로
     * @param h        expected height / 기대 세로
     * @param expected expected pixels / 기대 픽셀
     * @param what     description / 설명
     */
    static void pixels(DecodedImage img, int w, int h, int[] expected, String what) {
        Check.equal(w, img.getWidth(), what + ": width");
        Check.equal(h, img.getHeight(), what + ": height");
        framePixels(img.getFrames().get(0), expected, what);
    }

    /**
     * Checks every pixel of a frame exactly.
     * <p>
     * 프레임의 모든 픽셀이 정확히 같은지 검사한다.
     *
     * @param f        frame / 프레임
     * @param expected expected pixels / 기대 픽셀
     * @param what     description / 설명
     */
    static void framePixels(DecodedFrame f, int[] expected, String what) {
        int[] got = f.getArgb();
        Check.equal(expected.length, got.length, what + ": pixel count");
        // 처음 다른 픽셀을 찾아 보고한다.
        for (int i = 0; i < got.length; i++) {
            if (got[i] != expected[i]) {
                Check.fail(what + ": pixel " + i + " expected " + Integer.toHexString(expected[i]) + " but was " + Integer.toHexString(got[i]));
            }
        }
    }

    /**
     * Checks that two pixels are equal within a tolerance per channel (alpha included).
     * <p>
     * 두 픽셀이 채널마다(알파 포함) 허용 오차 안에서 같은지 검사한다.
     *
     * @param expected expected pixel / 기대 픽셀
     * @param actual   actual pixel / 실제 픽셀
     * @param tol      tolerance per channel / 채널별 허용 오차
     * @param what     description / 설명
     */
    static void near(int expected, int actual, int tol, String what) {
        // 알파, 빨강, 초록, 파랑 순서로 비교한다.
        for (int shift = 24; shift >= 0; shift -= 8) {
            int e = (expected >>> shift) & 0xFF;
            int a = (actual >>> shift) & 0xFF;
            if (Math.abs(e - a) > tol) {
                Check.fail(what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual) + " (tolerance " + tol
                        + ")");
            }
        }
    }

    /**
     * Checks that a block throws a {@link MediaException} of a given kind.
     * <p>
     * 블록이 주어진 종류의 {@link MediaException} 을 던지는지 검사한다.
     *
     * @param kind  expected kind / 기대하는 종류
     * @param block code to run / 실행할 코드
     * @param what  description / 설명
     */
    static void fails(MediaException.Kind kind, Block block, String what) {
        Throwable t = Check.throwsException(MediaException.class, block, what);
        Check.equal(kind, ((MediaException) t).getKind(), what + ": kind (message: " + t.getMessage() + ")");
    }

    /**
     * Returns a copy of a byte array with a range replaced.
     * <p>
     * 일부 구간을 바꾼 바이트 배열의 복사본을 반환한다.
     *
     * @param d      data / 데이터
     * @param pos    start of the range / 구간의 시작
     * @param values replacement bytes / 바꿀 바이트
     * @return the changed copy / 바뀐 복사본
     */
    static byte[] patched(byte[] d, int pos, int... values) {
        byte[] r = d.clone();
        // 지정한 위치부터 값을 덮어쓴다.
        for (int i = 0; i < values.length; i++) {
            r[pos + i] = (byte) values[i];
        }
        return r;
    }

    /**
     * Returns the first n bytes of an array.
     * <p>
     * 배열의 앞 n 바이트를 반환한다.
     *
     * @param d data / 데이터
     * @param n number of bytes / 바이트 수
     * @return the prefix / 앞부분
     */
    static byte[] prefix(byte[] d, int n) {
        return java.util.Arrays.copyOf(d, n);
    }
}
