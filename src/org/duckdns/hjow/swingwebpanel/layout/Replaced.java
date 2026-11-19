package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Knowledge about replaced elements: which tags are replaced and what their natural size is when the {@link ReplacedResolver} gives no
 * size (the <code>width</code>/<code>height</code> attributes, then built-in defaults similar to desktop browsers).
 * <p>
 * 치환 요소에 대한 지식: 어떤 태그가 치환 요소인지와, {@link ReplacedResolver} 가 크기를 주지 않을 때의 자연 크기(<code>width</code>/<code>height</code>
 * 속성, 그다음 데스크톱 브라우저와 비슷한 내장 기본값).
 */
final class Replaced {

    /** The tags of replaced elements. 치환 요소의 태그. */
    private static final String[] TAGS = { "img", "video", "audio", "canvas", "iframe", "frame", "embed", "object", "input", "select", "textarea", "progress", "meter", "svg" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Replaced() {
    }

    /**
     * Tells whether an element is laid out as a replaced element. Like {@link #isReplacedTag(String)}, except for the <code>object</code>
     * element: the component runs no plug-in and loads no nested document into an <code>object</code>, so an <code>object</code> that has
     * fallback content (an element child other than <code>param</code>, or text that is not only white space) is laid out as an ordinary
     * element that shows the fallback content (HTML5 2014 REC section 4.8.7, the content of <code>object</code> is the fallback).
     * <p>
     * 요소를 치환 요소로 배치하는지 알려 준다. <code>object</code> 요소를 제외하면 {@link #isReplacedTag(String)} 과 같다: 컴포넌트는 <code>object</code> 에
     * 플러그인을 실행하거나 중첩 문서를 읽지 않으므로, 대체 내용(<code>param</code> 이외의 요소 자식이나 공백뿐이 아닌 텍스트)이 있는 <code>object</code> 는 대체 내용을
     * 보이는 일반 요소로 배치한다(HTML5 2014 REC 4.8.7절, <code>object</code> 의 내용이 대체 내용이다).
     *
     * @param e   the element / 요소
     * @param tag the lower-case tag name / 소문자 태그 이름
     * @return true if laid out as a replaced element / 치환 요소로 배치하면 true
     */
    static boolean isReplaced(Element e, String tag) {
        if (!isReplacedTag(tag)) {
            return false;
        }
        if (!"object".equals(tag)) {
            return true;
        }
        // 대체 내용이 있는 object 는 치환 요소가 아니다.
        for (org.duckdns.hjow.swingwebpanel.dom.Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && !"param".equals(BoxBuilder.lower(((Element) n).getLocalName()))) {
                return false;
            }
            if (n instanceof org.duckdns.hjow.swingwebpanel.dom.Text && ((org.duckdns.hjow.swingwebpanel.dom.Text) n).getData().trim().length() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a lower-case tag name is a replaced element.
     * <p>
     * 소문자 태그 이름이 치환 요소인지 알려 준다.
     *
     * @param tag the tag name / 태그 이름
     * @return true if replaced / 치환 요소이면 true
     */
    static boolean isReplacedTag(String tag) {
        // 치환 요소 목록에서 태그를 찾는다.
        for (int i = 0; i < TAGS.length; i++) {
            if (TAGS[i].equals(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the natural size of a replaced box: the size from the resolver, else the HTML attributes, else a default. A component may
     * be NaN when unknown (an image without any size has no natural size).
     * <p>
     * 치환 상자의 자연 크기를 반환한다: 리졸버가 준 크기, 없으면 HTML 속성, 없으면 기본값. 알 수 없는 성분은 NaN 일 수 있다(크기가 전혀 없는 이미지).
     *
     * @param b the replaced box / 치환 상자
     * @return array of width and height / 너비와 높이 배열
     */
    static double[] natural(LayoutBox b) {
        if (b.node instanceof Element) {
            // 폼 컨트롤은 글꼴과 속성에서 고유 크기를 정한다(작성자 CSS 크기가 우선하는 것은 호출자가 처리한다).
            double[] control = ControlSizes.natural(b, (Element) b.node);
            if (control != null) {
                return control;
            }
        }
        double w = Double.NaN;
        double h = Double.NaN;
        if (b.replaced != null) {
            w = b.replaced.getNaturalWidth();
            h = b.replaced.getNaturalHeight();
        }
        Element e = b.node instanceof Element ? (Element) b.node : null;
        String tag = e == null ? "" : BoxBuilder.lower(e.getLocalName());
        if (e != null && Double.isNaN(w) && Double.isNaN(h)) {
            w = number(e.getAttribute("width"));
            h = number(e.getAttribute("height"));
        }
        if (Double.isNaN(w) && Double.isNaN(h)) {
            double[] d = defaults(e, tag);
            w = d[0];
            h = d[1];
        }
        return new double[] { w, h };
    }

    /**
     * Returns the natural size of a replaced box in the logical dimensions of a frame: the inline size and the block size, that is
     * width and height in horizontal frames and height and width in vertical ones (an image keeps its orientation on the page).
     * <p>
     * 치환 상자의 자연 크기를 프레임의 논리적 크기로 반환한다: 인라인 크기와 블록 크기이며 가로 프레임에서는 너비와 높이, 세로 프레임에서는 높이와 너비이다(이미지는 페이지에서
     * 방향을 유지한다).
     *
     * @param b     the replaced box / 치환 상자
     * @param frame the frame of the box / 상자의 프레임
     * @return array of inline size and block size / 인라인 크기와 블록 크기 배열
     */
    static double[] natural(LayoutBox b, int frame) {
        double[] n = natural(b);
        return frame == Css.FRAME_H ? n : new double[] { n[1], n[0] };
    }

    /**
     * Returns the default size of a replaced element without any size information.
     * <p>
     * 크기 정보가 전혀 없는 치환 요소의 기본 크기를 반환한다.
     *
     * @param e   the element or null / 요소 또는 null
     * @param tag its lower-case tag / 소문자 태그
     * @return array of width and height (NaN: none) / 너비와 높이 배열(NaN 은 없음)
     */
    private static double[] defaults(Element e, String tag) {
        if ("audio".equals(tag)) {
            return new double[] { 300, 54 };
        }
        if ("img".equals(tag)) {
            return new double[] { Double.NaN, Double.NaN };
        }
        return new double[] { 300, 150 };
    }

    /**
     * Parses an HTML dimension attribute (a number, optionally followed by <code>px</code>); percentages are not supported here.
     * <p>
     * HTML 크기 속성(숫자, 뒤에 <code>px</code> 가 올 수 있음)을 해석한다. 백분율은 여기서 지원하지 않는다.
     *
     * @param s the attribute value or null / 속성 값 또는 null
     * @return the number or NaN / 숫자 또는 NaN
     */
    private static double number(String s) {
        if (s == null) {
            return Double.NaN;
        }
        String t = s.trim().toLowerCase();
        if (t.endsWith("px")) {
            t = t.substring(0, t.length() - 2).trim();
        }
        try {
            double v = Double.parseDouble(t);
            return v < 0 || Double.isInfinite(v) ? Double.NaN : v;
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }
}
