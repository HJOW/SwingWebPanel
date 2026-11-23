package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>contain-intrinsic-*</code> properties (plan item P10-08-h, CSS Box Sizing Level 4 section 5.2, https://www.w3.org/TR/css-sizing-4/ WD 4
 * September 2026): the shorthand <code>contain-intrinsic-size</code> with <code>auto</code> values, the logical longhands
 * <code>contain-intrinsic-inline-size</code> and <code>contain-intrinsic-block-size</code> in horizontal and vertical writing modes, and the last remembered
 * size of <code>auto</code> (section 5.2.1) that a box which skips its contents uses.
 * <p>
 * <code>contain-intrinsic-*</code> 속성의 시험(계획 P10-08-h, CSS Box Sizing Level 4 5.2 절): <code>auto</code> 값이 있는 단축 속성 <code>contain-intrinsic-size</code>, 가로와
 * 세로쓰기의 논리 개별 속성 <code>contain-intrinsic-inline-size</code>, <code>contain-intrinsic-block-size</code>, 그리고 내용을 건너뛰는 상자가 쓰는 <code>auto</code> 의 마지막으로
 * 기억한 크기(5.2.1 절).
 */
public class ContainIntrinsicSizeTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContainIntrinsicSizeTest() {
    }

    /**
     * The shorthand takes one or two values, each an optional <code>auto</code> and <code>none</code> or a length.
     * <p>
     * 단축 속성은 값 하나나 둘을 받으며 각각은 선택적인 <code>auto</code> 와 <code>none</code> 이나 길이이다.
     */
    public void testShorthand() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='contain-intrinsic-size:auto 100px'></div><div id=b style='contain-intrinsic-size:auto 10px auto none'></div>"
                + "<div id=c style='contain-intrinsic-size:20px auto 30px'></div><div id=d style='contain-intrinsic-size:30px'></div>"
                + "<div id=e style='contain-intrinsic-size:auto'></div><div id=f style='contain-intrinsic-size:auto auto 10px'></div>", 400);
        Check.equal("auto 100px|auto 100px", values(r, "a"), "one value with auto applies to both");
        Check.equal("auto 10px|auto none", values(r, "b"), "two values with auto");
        Check.equal("20px|auto 30px", values(r, "c"), "a length and auto with a length");
        Check.equal("30px|30px", values(r, "d"), "one length");
        Check.equal("none|none", values(r, "e"), "auto alone is invalid");
        Check.equal("none|none", values(r, "f"), "auto twice is invalid");
    }

    /**
     * Returns the computed width and height values of an element.
     * <p>
     * 요소의 계산된 너비 값과 높이 값을 반환한다.
     *
     * @param r  the layout / 레이아웃
     * @param id the id / id
     * @return "width|height" / "너비|높이"
     */
    private static String values(LayoutResult r, String id) {
        LayoutBox b = LayoutTestSupport.box(r, id);
        return b.getStyle().get(Prop.CONTAIN_INTRINSIC_WIDTH).toCssText() + "|" + b.getStyle().get(Prop.CONTAIN_INTRINSIC_HEIGHT).toCssText();
    }

    /**
     * The logical longhands map to the width or the height by the writing mode, and compete with the physical ones in the order of the cascade.
     * <p>
     * 논리 개별 속성은 쓰기 모드에 따라 너비나 높이에 대응하며 계단식 순서로 물리 속성과 겨룬다.
     */
    public void testLogicalLonghands() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='contain:size;contain-intrinsic-block-size:40px;contain-intrinsic-inline-size:70px;width:max-content'></div>"
                + "<div id=b style='contain:size;writing-mode:vertical-rl;contain-intrinsic-block-size:40px;contain-intrinsic-inline-size:70px'></div>"
                + "<div id=c style='contain:size;contain-intrinsic-block-size:40px;contain-intrinsic-height:25px'></div>", 400);
        Check.equal(316, Prop.CONTAIN_INTRINSIC_INLINE_SIZE, "the id of contain-intrinsic-inline-size");
        Check.equal(317, Prop.CONTAIN_INTRINSIC_BLOCK_SIZE, "the id of contain-intrinsic-block-size");
        Check.equal("70px|40px", values(r, "a"), "horizontal: inline is the width");
        Check.near(40, LayoutTestSupport.box(r, "a").getHeight(), 0.5, "the block size is the height of the size-contained box");
        Check.equal("40px|70px", values(r, "b"), "vertical: block is the width");
        Check.equal("none|25px", values(r, "c"), "the later physical declaration wins");
        Check.near(25, LayoutTestSupport.box(r, "c").getHeight(), 0.5, "and gives the height");
    }

    /**
     * <code>auto</code>: an element that is laid out without size containment remembers its inner size; when it skips its contents
     * (<code>content-visibility: hidden</code>) it has that size instead of the length; it forgets it when the value has no <code>auto</code> any more.
     * <p>
     * <code>auto</code>: 크기 격리 없이 배치된 요소는 안쪽 크기를 기억하고, 내용을 건너뛸 때(<code>content-visibility: hidden</code>) 길이 대신 그 크기를 가지며, 값에 더는
     * <code>auto</code> 가 없으면 잊는다.
     */
    public void testLastRememberedSize() {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head>" + LayoutTestSupport.RESET + "</head><body><div id=d style='contain-intrinsic-size:auto 50px'>"
                + "<div style='height:200px'></div></div><div id=n style='content-visibility:hidden;contain-intrinsic-size:auto 50px'><div style='height:200px'></div></div>"
                + "</body></html>");
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(400, 600));
        css.loadStyleSheets();
        LayoutResult first = LayoutEngine.layout(doc, new CssStyleSource(css), null, 400, 600);
        Check.near(200, LayoutTestSupport.box(first, "d").getHeight(), 0.5, "without containment the contents give the height");
        Check.near(50, LayoutTestSupport.box(first, "n").getHeight(), 0.5, "skipping from the start: no remembered size, the length");
        double[] remembered = doc.getLastRememberedSize(doc.getElementById("d"));
        Check.notNull(remembered, "the size was remembered");
        Check.near(200, remembered[1], 0.5, "the remembered inner height");
        Element d = doc.getElementById("d");
        d.setAttribute("style", "content-visibility:hidden;contain-intrinsic-size:auto 50px");
        LayoutResult hidden = LayoutEngine.layout(doc, new CssStyleSource(css), null, 400, 600);
        Check.near(200, LayoutTestSupport.box(hidden, "d").getHeight(), 0.5, "skipping its contents it keeps the remembered height");
        d.setAttribute("style", "content-visibility:hidden;contain-intrinsic-size:50px");
        LayoutResult plain = LayoutEngine.layout(doc, new CssStyleSource(css), null, 400, 600);
        Check.near(50, LayoutTestSupport.box(plain, "d").getHeight(), 0.5, "without auto the length is used");
        Check.isTrue(doc.getLastRememberedSize(d) == null, "and the remembered size is forgotten");
    }
}
