package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the layout tests: parse HTML, run the real CSS engine (user-agent sheet plus the page's own style elements), lay out and
 * find boxes. Expected text widths are computed from the font that the fragments report, so the tests do not depend on a particular
 * installed font.
 * <p>
 * 레이아웃 시험의 도우미: HTML 을 해석하고 실제 CSS 엔진(사용자 에이전트 시트와 페이지의 style 요소)을 돌려 배치하고 상자를 찾는다. 기대하는 텍스트 너비는
 * 조각이 알려 주는 글꼴로 계산하므로 시험이 특정 설치 글꼴에 의존하지 않는다.
 */
public final class LayoutTestSupport {

    /** Reset sheet that removes the default margins so that coordinates are easy to predict. 기본 margin 을 없애 좌표를 예측하기 쉽게 하는 초기화 시트. */
    public static final String RESET = "<style>html,body{margin:0;padding:0} body{font:10px monospace; line-height:normal}</style>";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private LayoutTestSupport() {
    }

    /**
     * Parses and lays out a document with the reset sheet in front.
     * <p>
     * 초기화 시트를 앞에 붙여 문서를 해석하고 배치한다.
     *
     * @param body  markup that follows the reset sheet / 초기화 시트 뒤에 오는 마크업
     * @param width viewport width / 뷰포트 너비
     * @return the result / 결과
     */
    public static LayoutResult layout(String body, double width) {
        return layout(null, body, width);
    }

    /**
     * Parses and lays out a document with the reset sheet in front and an optional resolver of replaced content.
     * <p>
     * 초기화 시트를 앞에 붙이고 치환 콘텐츠 리졸버를 둘 수 있게 문서를 해석하고 배치한다.
     *
     * @param resolver replaced content source or null / 치환 콘텐츠 공급자 또는 null
     * @param body     markup that follows the reset sheet / 초기화 시트 뒤에 오는 마크업
     * @param width    viewport width / 뷰포트 너비
     * @return the result / 결과
     */
    public static LayoutResult layout(ReplacedResolver resolver, String body, double width) {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head>" + RESET + "</head><body>" + body + "</body></html>");
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(width, 600));
        css.loadStyleSheets();
        return LayoutEngine.layout(doc, new CssStyleSource(css), resolver, width, 600);
    }

    /**
     * Finds the first box of the element with an id.
     * <p>
     * id 를 가진 요소의 첫 상자를 찾는다.
     *
     * @param r  the result / 결과
     * @param id the id / id
     * @return the box / 상자
     */
    public static LayoutBox box(LayoutResult r, String id) {
        Element e = ((Element) r.getRoot().getNode()).getOwnerDocument().getElementById(id);
        Check.notNull(e, "element #" + id);
        List<LayoutBox> l = r.getBoxes(e);
        Check.isFalse(l.isEmpty(), "element #" + id + " has a box");
        return l.get(0);
    }

    /**
     * Returns all line boxes below a box in document order.
     * <p>
     * 상자 아래의 모든 줄 상자를 문서 순서로 반환한다.
     *
     * @param b the box / 상자
     * @return the lines / 줄
     */
    public static List<LayoutBox> lines(LayoutBox b) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        collect(b, LayoutBox.Type.LINE, out);
        return out;
    }

    /**
     * Returns all boxes of a type below a box in document order.
     * <p>
     * 상자 아래의 한 종류의 모든 상자를 문서 순서로 반환한다.
     *
     * @param b    the box / 상자
     * @param type the type / 종류
     * @param out  receives the boxes / 상자를 받는다
     */
    public static void collect(LayoutBox b, LayoutBox.Type type, List<LayoutBox> out) {
        if (b.getType() == type) {
            out.add(b);
        }
        // 자식을 차례로 훑는다.
        for (LayoutBox c : b.getChildren()) {
            collect(c, type, out);
        }
    }

    /**
     * Returns the text of a line: the text of its text fragments joined in x order.
     * <p>
     * 줄의 텍스트를 반환한다: 텍스트 조각의 텍스트를 x 순서로 이은 것.
     *
     * @param line the line box / 줄 상자
     * @return the text / 텍스트
     */
    public static String lineText(LayoutBox line) {
        List<LayoutBox> t = new ArrayList<LayoutBox>();
        collect(line, LayoutBox.Type.TEXT, t);
        StringBuilder sb = new StringBuilder();
        // 조각을 x 가 작은 순서로 이어 붙인다.
        while (!t.isEmpty()) {
            int best = 0;
            // 삽입 정렬로 x 가 작은 조각을 앞에 둔다.
            for (int i = 1; i < t.size(); i++) {
                if (t.get(i).getX() < t.get(best).getX()) {
                    best = i;
                }
            }
            sb.append(t.remove(best).getText());
        }
        return sb.toString();
    }

    /**
     * Returns the advance of a string in the font that a box uses.
     * <p>
     * 상자가 쓰는 글꼴에서 문자열의 advance 를 반환한다.
     *
     * @param f the font / 글꼴
     * @param s the string / 문자열
     * @return width / 너비
     */
    public static double advance(Font f, String s) {
        return f.getStringBounds(s, FontResolver.FRC).getWidth();
    }

    /**
     * Returns the width of one character of the 10 pixel monospace font of the reset sheet.
     * <p>
     * 초기화 시트의 10 픽셀 monospace 글꼴에서 한 글자의 너비를 반환한다.
     *
     * @return width / 너비
     */
    public static double charWidth() {
        return advance(new Font(Font.MONOSPACED, Font.PLAIN, 1).deriveFont(10f), "0");
    }
}
