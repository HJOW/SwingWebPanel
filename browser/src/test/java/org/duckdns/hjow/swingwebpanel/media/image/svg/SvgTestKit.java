package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers shared by the SVG tests: building small SVG documents, decoding them, reading pixels and loading the test resources.
 * <p>
 * SVG 시험들이 함께 쓰는 도우미: 작은 SVG 문서 만들기, 디코딩, 픽셀 읽기, 시험 자료 읽기.
 */
final class SvgTestKit {

    /**
     * The SVG namespace declaration used by the small documents.
     * <p>
     * 작은 문서가 쓰는 SVG 네임스페이스 선언.
     */
    static final String NS = "xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\"";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgTestKit() {
    }

    /**
     * Encodes a text as UTF-8.
     * <p>
     * 텍스트를 UTF-8 로 인코딩한다.
     *
     * @param s the text / 텍스트
     * @return the bytes / 바이트
     */
    static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e.toString());
        }
    }

    /**
     * Builds an SVG document of the given size (and view box equal to the size) around a body.
     * <p>
     * 본문을 지정한 크기(viewBox 도 같은 크기)의 SVG 문서로 감싼다.
     *
     * @param w    width / 너비
     * @param h    height / 높이
     * @param body the content / 내용
     * @return the document text / 문서 텍스트
     */
    static String doc(int w, int h, String body) {
        return "<svg " + NS + " width=\"" + w + "\" height=\"" + h + "\" viewBox=\"0 0 " + w + " " + h + "\">" + body + "</svg>";
    }

    /**
     * Decodes SVG text at its natural size with the default limits.
     * <p>
     * SVG 텍스트를 기본 제한으로 자연 크기에서 디코딩한다.
     *
     * @param svg the document / 문서
     * @return the image / 이미지
     * @throws MediaException on a decoding failure / 디코딩 실패 시
     */
    static DecodedImage decode(String svg) throws MediaException {
        return new SvgDecoder().decode(utf8(svg), DecodeLimits.DEFAULT);
    }

    /**
     * Returns a pixel of the first frame.
     * <p>
     * 첫 프레임의 픽셀을 반환한다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return the non-premultiplied ARGB pixel / 알파를 곱하지 않은 ARGB 픽셀
     */
    static int px(DecodedImage img, int x, int y) {
        return img.getFrames().get(0).getArgb()[y * img.getWidth() + x];
    }

    /**
     * Checks that a pixel equals an expected colour within a tolerance per channel.
     * <p>
     * 픽셀이 기대한 색과 채널별 허용 오차 안에서 같은지 검사한다.
     *
     * @param expected expected ARGB / 기대 ARGB
     * @param actual   actual ARGB / 실제 ARGB
     * @param tol      tolerance per channel / 채널별 허용 오차
     * @param what     description / 설명
     */
    static void near(int expected, int actual, int tol, String what) {
        // 알파, 빨강, 초록, 파랑 순서로 비교한다.
        for (int shift = 24; shift >= 0; shift -= 8) {
            int e = (expected >>> shift) & 0xFF;
            int a = (actual >>> shift) & 0xFF;
            if (Math.abs(e - a) > tol) {
                Check.fail(what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(actual) + " (tolerance " + tol + ")");
            }
        }
    }

    /**
     * Checks that decoding a text fails with a kind of {@link MediaException}.
     * <p>
     * 텍스트를 디코딩하면 지정한 종류의 {@link MediaException} 으로 실패하는지 검사한다.
     *
     * @param kind expected kind / 기대하는 종류
     * @param svg  the document text / 문서 텍스트
     * @param what description / 설명
     */
    static void fails(final MediaException.Kind kind, final String svg, String what) {
        Throwable t = Check.throwsException(MediaException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            public void run() throws Exception {
                decode(svg);
            }
        }, what);
        Check.equal(kind, ((MediaException) t).getKind(), what + ": kind (message: " + t.getMessage() + ")");
    }

    /**
     * Reads a test resource from the class path.
     * <p>
     * 클래스패스에서 시험 자료를 읽는다.
     *
     * @param name the absolute resource name / 절대 자료 이름
     * @return the bytes or null if missing / 바이트. 없으면 null
     * @throws IOException on a read failure / 읽기 실패 시
     */
    static byte[] resource(String name) throws IOException {
        InputStream in = SvgTestKit.class.getResourceAsStream(name);
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 끝까지 읽어 모은다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
