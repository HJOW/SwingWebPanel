package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the render tests: build a page with the real HTML parser, CSS engine, layout engine and renderer, paint it into a headless
 * image and read pixels.
 * <p>
 * 렌더 시험의 도우미: 실제 HTML 파서, CSS 엔진, 레이아웃 엔진, 렌더러로 페이지를 만들고 헤드리스 이미지에 그려 픽셀을 읽는다.
 */
public final class RenderTestSupport {

    /**
     * Resets the margins and the font so that coordinates are easy to predict.
     * <p>
     * 좌표를 예측하기 쉽도록 여백과 글꼴을 초기화한다.
     */
    public static final String RESET = "<style>html,body{margin:0;padding:0} body{font:10px monospace; line-height:normal}</style>";

    /**
     * The document of the last {@link #page} call.
     * <p>
     * 마지막 {@link #page} 호출의 문서.
     */
    public static Document lastDocument;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RenderTestSupport() {
    }

    /**
     * An image source with fixed answers for the tests.
     * <p>
     * 시험용으로 답이 정해진 이미지 소스.
     */
    public static final class FixedImages implements ImageSource {
        /**
         * The answers by URL.
         * <p>
         * URL 별 답.
         */
        private final Map<String, ImageResource> map = new HashMap<String, ImageResource>();

        /**
         * Registers the state of a URL.
         * <p>
         * URL 의 상태를 등록한다.
         *
         * @param url the URL / URL
         * @param r   the state / 상태
         * @return this / 자기 자신
         */
        public FixedImages put(String url, ImageResource r) {
            map.put(url, r);
            return this;
        }

        /**
         * Returns the state of an image.
         * <p>
         * 이미지의 상태를 반환한다.
         *
         * @param absoluteUrl the URL / URL
         * @return the state or null / 상태 또는 null
         */
        public ImageResource getImage(String absoluteUrl) {
            return map.get(absoluteUrl);
        }

        /**
         * Returns the state of a media resource.
         * <p>
         * 미디어 리소스의 상태를 반환한다.
         *
         * @param absoluteUrl the URL / URL
         * @return the state or null / 상태 또는 null
         */
        public ImageResource getMedia(String absoluteUrl) {
            return map.get(absoluteUrl);
        }
    }

    /**
     * Builds a rendered page for a body.
     * <p>
     * body 내용으로 렌더링된 페이지를 만든다.
     *
     * @param body   the body markup / body 마크업
     * @param width  viewport width / 뷰포트 너비
     * @param height viewport height / 뷰포트 높이
     * @param images image states or null / 이미지 상태 또는 null
     * @return the page / 페이지
     */
    public static RenderedPage page(String body, int width, int height, ImageSource images) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head>" + RESET + "</head><body>" + body + "</body></html>",
                "http://t.test/dir/page.html", null, true);
        lastDocument = doc;
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(width, height));
        css.loadStyleSheets();
        return PageRenderer.render(doc, css, images, width, height);
    }

    /**
     * Paints a page at 100 percent into a new white image.
     * <p>
     * 페이지를 100 % 로 새 흰색 이미지에 그린다.
     *
     * @param p      the page / 페이지
     * @param width  image width / 이미지 너비
     * @param height image height / 이미지 높이
     * @return the image / 이미지
     */
    public static BufferedImage paint(RenderedPage p, int width, int height) {
        BufferedImage img = blank(width, height);
        Graphics2D g = img.createGraphics();
        try {
            p.paint(g, new Rectangle(0, 0, width, height), 0, 0);
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Creates a white image.
     * <p>
     * 흰색 이미지를 만든다.
     *
     * @param width  width / 너비
     * @param height height / 높이
     * @return the image / 이미지
     */
    public static BufferedImage blank(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }

    /**
     * Returns the RGB value of a pixel (alpha dropped).
     * <p>
     * 픽셀의 RGB 값을 반환한다(알파 제외).
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @return 0xRRGGBB / 0xRRGGBB
     */
    public static int rgb(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) & 0xFFFFFF;
    }

    /**
     * Counts the dark pixels (ink) in a rectangle.
     * <p>
     * 사각형 안의 어두운 픽셀(잉크) 수를 센다.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @return number of pixels darker than the middle gray / 중간 회색보다 어두운 픽셀 수
     */
    public static int ink(BufferedImage img, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 어두운 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                int c = img.getRGB(x, y);
                int sum = ((c >> 16) & 0xFF) + ((c >> 8) & 0xFF) + (c & 0xFF);
                if (sum < 384) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Tells whether two RGB values are within a tolerance per channel.
     * <p>
     * 두 RGB 값이 채널마다 허용 오차 안에 있는지 알려 준다.
     *
     * @param a   first / 첫째
     * @param b   second / 둘째
     * @param tol tolerance per channel / 채널별 허용 오차
     * @return true if close / 가까우면 true
     */
    public static boolean near(int a, int b, int tol) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) <= tol && Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) <= tol
                && Math.abs((a & 0xFF) - (b & 0xFF)) <= tol;
    }

    /**
     * Returns the first layout box of an element by id.
     * <p>
     * id 로 요소의 첫 레이아웃 상자를 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id / id
     * @return the box / 상자
     */
    public static LayoutBox box(RenderedPage p, String id) {
        Element e = lastDocument.getElementById(id);
        Check.notNull(e, "element #" + id);
        List<LayoutBox> l = p.getLayout().getBoxes(e);
        Check.isFalse(l.isEmpty(), "element #" + id + " has a box");
        return l.get(0);
    }

    /**
     * Creates a string from code points (avoids escape sequences in the source).
     * <p>
     * 코드 포인트로 문자열을 만든다(소스에 이스케이프 시퀀스를 쓰지 않기 위함).
     *
     * @param cps the UTF-16 code units / UTF-16 코드 단위
     * @return the string / 문자열
     */
    public static String str(int... cps) {
        char[] c = new char[cps.length];
        // 코드 단위를 하나씩 문자로 옮긴다.
        for (int i = 0; i < cps.length; i++) {
            c[i] = (char) cps[i];
        }
        return new String(c);
    }

    /**
     * Creates a solid-color image.
     * <p>
     * 단색 이미지를 만든다.
     *
     * @param w    width / 너비
     * @param h    height / 높이
     * @param argb color / 색
     * @return the image / 이미지
     */
    public static DecodedImage solid(int w, int h, int argb) {
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, argb);
        return new DecodedImage(w, h, px);
    }

    /**
     * Counts the pixels that are not (nearly) white: anything that was painted, including light anti-aliased text.
     * <p>
     * 흰색(에 가까운 것)이 아닌 픽셀의 수를 센다: 옅은 안티앨리어싱 텍스트를 포함해 그려진 모든 것.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @return the number of painted pixels / 그려진 픽셀 수
     */
    public static int touched(BufferedImage img, int x0, int y0, int x1, int y1) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 흰색이 아닌 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                int c = img.getRGB(x, y);
                int sum = ((c >> 16) & 0xFF) + ((c >> 8) & 0xFF) + (c & 0xFF);
                if (sum < 735) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Returns the layout box of the first element with a tag name.
     * <p>
     * 태그 이름이 같은 첫 요소의 레이아웃 상자를 반환한다.
     *
     * @param p   the page / 페이지
     * @param tag the tag name / 태그 이름
     * @return the box / 상자
     */
    public static LayoutBox firstBox(RenderedPage p, String tag) {
        Element e = lastDocument.getElementsByTagName(tag).item(0);
        Check.notNull(e, "a <" + tag + "> element");
        List<LayoutBox> l = p.getLayout().getBoxes(e);
        Check.isFalse(l.isEmpty(), "<" + tag + "> has a box");
        return l.get(0);
    }
}
