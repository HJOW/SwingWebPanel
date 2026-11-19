package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the pseudo-elements and the user interface properties of form controls (plan item P10-08, CSS Pseudo-Elements Level 4 and CSS
 * Basic User Interface Level 4): the color of <code>::placeholder</code> in a text field and in a text area, the default placeholder
 * color, <code>accent-color</code> of a check box, a radio button, a progress bar and a range slider, the computed values of
 * <code>caret-color</code> and of the properties of <code>::selection</code>.
 * <p>
 * 폼 컨트롤의 의사 요소와 사용자 인터페이스 속성 시험(계획 P10-08, CSS Pseudo-Elements Level 4, CSS Basic User Interface Level 4): 텍스트 필드와 텍스트 영역의
 * <code>::placeholder</code> 색, 기본 placeholder 색, 체크박스, 라디오, 진행 막대, range 슬라이더의 <code>accent-color</code>, <code>caret-color</code> 와
 * <code>::selection</code> 속성의 계산 값.
 */
public class PseudoUiPaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PseudoUiPaintTest() {
    }

    /**
     * Returns the snapshot of the control with an id.
     * <p>
     * id 를 가진 컨트롤의 스냅샷을 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id / id
     * @return the snapshot / 스냅샷
     */
    private static ControlSnapshot snap(RenderedPage p, String id) {
        Element e = RenderTestSupport.lastDocument.getElementById(id);
        ControlSnapshot s = p.getControl(e);
        Check.notNull(s, "control #" + id + " has a snapshot");
        return s;
    }

    /**
     * Counts the pixels in an area that are close to a color.
     * <p>
     * 영역에서 어떤 색에 가까운 픽셀을 센다.
     *
     * @param img the image / 이미지
     * @param x0  left / 왼쪽
     * @param y0  top / 위쪽
     * @param x1  right (exclusive) / 오른쪽(미포함)
     * @param y1  bottom (exclusive) / 아래쪽(미포함)
     * @param rgb the color / 색
     * @param tol the tolerance per channel / 채널별 허용 오차
     * @return the number of pixels / 픽셀 수
     */
    private static int near(BufferedImage img, int x0, int y0, int x1, int y1, int rgb, int tol) {
        int n = 0;
        // 영역의 모든 픽셀을 훑으며 색이 가까운 것을 센다.
        for (int y = Math.max(0, y0); y < Math.min(img.getHeight(), y1); y++) {
            for (int x = Math.max(0, x0); x < Math.min(img.getWidth(), x1); x++) {
                if (RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), rgb, tol)) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * <code>::placeholder</code> gives the color of the placeholder of a text field and of a text area; the default is the gray of the
     * user agent style sheet.
     * <p>
     * <code>::placeholder</code> 는 텍스트 필드와 텍스트 영역의 placeholder 색을 정하며 기본값은 사용자 에이전트 스타일시트의 회색이다.
     */
    public void testPlaceholderColor() {
        RenderedPage p = RenderTestSupport.page("<input id=a placeholder='abcdef' style='font:14px monospace'>"
                + "<textarea id=b placeholder='ghijkl' style='font:14px monospace'></textarea>"
                + "<style>#c::placeholder{color:#f00} #d::placeholder{color:rgba(0,0,255,.5)}</style>"
                + "<input id=c placeholder='mnopqr' style='font:14px monospace'><textarea id=d placeholder='stuvwx' style='font:14px monospace'></textarea>", 500, 300, null);
        Check.equal(0xFF757575, snap(p, "a").placeholderColor, "the default color of a placeholder");
        Check.equal(0xFF757575, snap(p, "b").placeholderColor, "the default color in a text area");
        Check.equal(0xFFFF0000, snap(p, "c").placeholderColor, "::placeholder color in a field");
        Check.equal(0x800000FF, snap(p, "d").placeholderColor, "::placeholder color with alpha in a text area");
        BufferedImage img = RenderTestSupport.paint(p, 500, 300);
        LayoutBox c = RenderTestSupport.box(p, "c");
        int red = near(img, (int) c.getContentX(), (int) c.getContentY(), (int) (c.getContentX() + c.getContentWidth()), (int) (c.getContentY() + c.getContentHeight()), 0xFF0000, 40);
        Check.isTrue(red > 10, "the placeholder is painted red: " + red);
        LayoutBox a = RenderTestSupport.box(p, "a");
        int gray = near(img, (int) a.getContentX(), (int) a.getContentY(), (int) (a.getContentX() + a.getContentWidth()), (int) (a.getContentY() + a.getContentHeight()), 0x757575, 40);
        Check.isTrue(gray > 10, "the default placeholder is gray: " + gray);
        // 값이 있으면 placeholder 가 아니라 값을 그린다.
        RenderedPage v = RenderTestSupport.page("<style>input::placeholder{color:#f00}</style><input id=a value='xyz' placeholder='abcdef' style='font:14px monospace'>", 300, 100, null);
        BufferedImage iv = RenderTestSupport.paint(v, 300, 100);
        LayoutBox va = RenderTestSupport.box(v, "a");
        Check.equal(0, near(iv, (int) va.getContentX(), (int) va.getContentY(), (int) (va.getContentX() + va.getContentWidth()), (int) (va.getContentY() + va.getContentHeight()), 0xFF0000, 40),
                "the value is painted instead of the placeholder");
    }

    /**
     * <code>accent-color</code> colors the controls that have an accent: a checked check box, a progress bar and a range slider; the
     * default accent is used for <code>auto</code>.
     * <p>
     * <code>accent-color</code> 는 강조색이 있는 컨트롤(체크된 체크박스, 진행 막대, range 슬라이더)의 색을 정한다. <code>auto</code> 이면 기본 강조색이다.
     */
    public void testAccentColor() {
        RenderedPage p = RenderTestSupport.page("<input id=a type=checkbox checked style='accent-color:#f00'><input id=b type=checkbox checked>"
                + "<progress id=c value=1 max=1 style='accent-color:#0a0;width:100px'></progress><progress id=d value=1 max=1 style='width:100px'></progress>"
                + "<input id=e type=range value=100 min=0 max=100 style='accent-color:#f0f;width:100px'>", 500, 300, null);
        Check.equal(0xFFFF0000, snap(p, "a").accent, "the accent of the check box");
        Check.equal(0, snap(p, "b").accent, "auto is no accent");
        BufferedImage img = RenderTestSupport.paint(p, 500, 300);
        LayoutBox a = RenderTestSupport.box(p, "a");
        Check.isTrue(near(img, (int) a.getContentX(), (int) a.getContentY(), (int) (a.getContentX() + a.getContentWidth()), (int) (a.getContentY() + a.getContentHeight()), 0xFF0000, 12) > 20,
                "the checked box is filled with the accent");
        LayoutBox b = RenderTestSupport.box(p, "b");
        Check.isTrue(near(img, (int) b.getContentX(), (int) b.getContentY(), (int) (b.getContentX() + b.getContentWidth()), (int) (b.getContentY() + b.getContentHeight()), 0x0075FF, 12) > 20,
                "the default accent when the accent is auto");
        LayoutBox c = RenderTestSupport.box(p, "c");
        Check.isTrue(near(img, (int) c.getContentX(), (int) c.getContentY(), (int) (c.getContentX() + c.getContentWidth()), (int) (c.getContentY() + c.getContentHeight()), 0x00AA00, 12) > 100,
                "a full progress bar is filled with the accent");
        LayoutBox d = RenderTestSupport.box(p, "d");
        Check.isTrue(near(img, (int) d.getContentX(), (int) d.getContentY(), (int) (d.getContentX() + d.getContentWidth()), (int) (d.getContentY() + d.getContentHeight()), 0x0075FF, 12) > 100,
                "the default progress bar accent");
        LayoutBox e = RenderTestSupport.box(p, "e");
        Check.isTrue(near(img, (int) e.getContentX(), (int) e.getContentY(), (int) (e.getContentX() + e.getContentWidth()), (int) (e.getContentY() + e.getContentHeight()), 0xFF00FF, 12) > 20,
                "the range slider uses the accent");
        // accent-color 는 상속한다.
        RenderedPage inh = RenderTestSupport.page("<div style='accent-color:#f00'><input id=a type=checkbox checked></div>", 300, 100, null);
        Check.equal(0xFFFF0000, snap(inh, "a").accent, "accent-color is inherited");
    }

    /**
     * The computed values of <code>caret-color</code>, <code>accent-color</code> and the properties of <code>::selection</code>.
     * <p>
     * <code>caret-color</code>, <code>accent-color</code> 와 <code>::selection</code> 속성의 계산 값.
     */
    public void testComputedValues() {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head><style>"
                + "#a{caret-color:#0f0;accent-color:#123456;color:#800000} #b{caret-color:currentcolor} #c::selection{color:#fff;background-color:#00f} p::selection{background:#ff0}"
                + "</style></head><body><input id=a><input id=b><p id=c>text</p><p id=d>other</p></body></html>", "http://t.test/dir/page.html", null, true);
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(300, 200));
        css.loadStyleSheets();
        ComputedStyle a = css.getComputedStyle(doc.getElementById("a"));
        Check.equal(0xFF00FF00, a.color(Prop.CARET_COLOR), "caret-color");
        Check.equal(0xFF123456, a.color(Prop.ACCENT_COLOR), "accent-color");
        Check.isTrue(css.getComputedStyle(doc.getElementById("b")).color(Prop.CARET_COLOR) == 0xFF000000, "currentcolor of the caret: black");
        Check.isTrue(css.getComputedStyle(doc.getElementById("c")).isAuto(Prop.CARET_COLOR), "the initial caret-color is auto");
        ComputedStyle sel = css.getResolver().getPseudoStyle(doc.getElementById("c"), "selection");
        Check.notNull(sel, "::selection of #c");
        Check.equal(0xFFFFFFFF, sel.color(Prop.COLOR), "::selection color");
        Check.equal(0xFF0000FF, sel.color(Prop.BACKGROUND_COLOR), "::selection background color");
        ComputedStyle other = css.getResolver().getPseudoStyle(doc.getElementById("d"), "selection");
        Check.notNull(other, "p::selection reaches #d");
        Check.equal(0xFFFFFF00, other.color(Prop.BACKGROUND_COLOR), "the background of p::selection");
        Check.isNull(css.getResolver().getPseudoStyle(doc.getElementById("a"), "selection"), "no ::selection rule targets #a");
    }
}
