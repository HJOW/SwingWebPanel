package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.props.LogicalProps;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the logical properties (plan item P10-08, CSS Logical Properties and Values Level 1): the mapping of the logical sides to the
 * physical ones for <code>horizontal-tb</code>, <code>rtl</code> and the vertical writing modes, the margins, paddings, borders,
 * offsets, radii and sizes that follow from it in the layout, the shorthands, the order of the cascade between a logical and a physical
 * declaration (the later one wins, also across <code>!important</code> and the <code>style</code> attribute), the dependency on the
 * <code>direction</code> that is declared after, inherited or taken from a <code>var()</code>, and the computed values that are asked
 * for by their logical names.
 * <p>
 * 논리 속성 시험(계획 P10-08, CSS Logical Properties and Values Level 1): <code>horizontal-tb</code>, <code>rtl</code>, 세로쓰기에서 논리적 변이 물리적 변으로 옮겨지는 규칙, 그에 따른
 * 레이아웃의 margin, padding, border, 오프셋, 반지름, 크기, 단축 속성, 논리 선언과 물리 선언 사이의 계단식 순서(나중 것이 이기며 <code>!important</code> 와
 * <code>style</code> 속성에도 마찬가지), 나중에 선언하거나 상속하거나 <code>var()</code> 로 가져온 <code>direction</code> 에 대한 의존, 논리 이름으로 묻는 계산 값.
 */
public class LogicalPropertiesTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LogicalPropertiesTest() {
    }

    /**
     * Lays out a body and returns the box of an id.
     * <p>
     * 본문을 배치하고 id 의 상자를 반환한다.
     *
     * @param body the markup / 마크업
     * @param id   the id / id
     * @return the box / 상자
     */
    private static LayoutBox box(String body, String id) {
        return LayoutTestSupport.box(LayoutTestSupport.layout(body, 400), id);
    }

    /**
     * Checks the four margins of a box.
     * <p>
     * 상자의 네 margin 을 확인한다.
     *
     * @param b    the box / 상자
     * @param top  top / 위
     * @param right right / 오른쪽
     * @param bottom bottom / 아래
     * @param left left / 왼쪽
     * @param what the message / 메시지
     */
    private static void margins(LayoutBox b, double top, double right, double bottom, double left, String what) {
        // 사용된 margin 은 over-constrained 일 때 조정되므로 계산된 margin 으로 비교한다.
        Check.near(top, b.getStyle().px(Prop.MARGIN_TOP), T, what + ": margin-top");
        Check.near(right, b.getStyle().px(Prop.MARGIN_RIGHT), T, what + ": margin-right");
        Check.near(bottom, b.getStyle().px(Prop.MARGIN_BOTTOM), T, what + ": margin-bottom");
        Check.near(left, b.getStyle().px(Prop.MARGIN_LEFT), T, what + ": margin-left");
    }

    /**
     * The mapping table itself: every logical side of every writing mode and direction.
     * <p>
     * 매핑 표 자체: 모든 쓰기 모드와 방향의 모든 논리적 변.
     */
    public void testMappingTable() {
        // 인수: 쓰기 모드, 방향 -> block-start, block-end, inline-start, inline-end 의 물리적 변(0 위, 1 오른쪽, 2 아래, 3 왼쪽)
        Object[][] rows = {
            {"horizontal-tb", "ltr", 0, 2, 3, 1}, {"horizontal-tb", "rtl", 0, 2, 1, 3},
            {"vertical-rl", "ltr", 1, 3, 0, 2}, {"vertical-rl", "rtl", 1, 3, 2, 0},
            {"vertical-lr", "ltr", 3, 1, 0, 2}, {"vertical-lr", "rtl", 3, 1, 2, 0},
            {"sideways-rl", "ltr", 1, 3, 0, 2}, {"sideways-rl", "rtl", 1, 3, 2, 0},
            {"sideways-lr", "ltr", 3, 1, 2, 0}, {"sideways-lr", "rtl", 3, 1, 0, 2}};
        // 쓰기 모드와 방향마다 네 논리적 변의 물리적 변을 확인한다.
        for (int i = 0; i < rows.length; i++) {
            String mode = (String) rows[i][0];
            boolean rtl = "rtl".equals(rows[i][1]);
            // 네 논리적 변을 하나씩 확인한다.
            for (int k = 0; k < 4; k++) {
                Check.equal(((Integer) rows[i][2 + k]).intValue(), LogicalProps.side(k, mode, rtl), mode + " " + rows[i][1] + " side " + k);
            }
        }
        // 물리 속성 id: horizontal-tb ltr 에서 margin-inline-start 는 margin-left, block-end 는 bottom 이다.
        Check.equal(Prop.MARGIN_LEFT, LogicalProps.physical(Prop.MARGIN_INLINE_START, "horizontal-tb", "ltr"), "margin-inline-start");
        Check.equal(Prop.MARGIN_BOTTOM, LogicalProps.physical(Prop.MARGIN_BLOCK_END, "horizontal-tb", "ltr"), "margin-block-end");
        Check.equal(Prop.PADDING_RIGHT, LogicalProps.physical(Prop.PADDING_INLINE_START, "horizontal-tb", "rtl"), "padding-inline-start in rtl");
        Check.equal(Prop.TOP, LogicalProps.physical(Prop.INSET_INLINE_START, "vertical-rl", "ltr"), "inset-inline-start in vertical-rl");
        Check.equal(Prop.BORDER_LEFT_STYLE, LogicalProps.physical(Prop.BORDER_BLOCK_END_STYLE, "vertical-rl", "ltr"), "border-block-end-style in vertical-rl");
        Check.equal(Prop.BORDER_TOP_COLOR, LogicalProps.physical(Prop.BORDER_BLOCK_START_COLOR, "horizontal-tb", "rtl"), "border-block-start-color");
        Check.equal(Prop.BORDER_TOP_LEFT_RADIUS, LogicalProps.physical(Prop.BORDER_START_START_RADIUS, "horizontal-tb", "ltr"), "start-start radius");
        Check.equal(Prop.BORDER_TOP_RIGHT_RADIUS, LogicalProps.physical(Prop.BORDER_START_END_RADIUS, "horizontal-tb", "ltr"), "start-end radius");
        Check.equal(Prop.BORDER_BOTTOM_LEFT_RADIUS, LogicalProps.physical(Prop.BORDER_END_START_RADIUS, "horizontal-tb", "ltr"), "end-start radius");
        Check.equal(Prop.BORDER_BOTTOM_RIGHT_RADIUS, LogicalProps.physical(Prop.BORDER_END_END_RADIUS, "horizontal-tb", "ltr"), "end-end radius");
        Check.equal(Prop.BORDER_BOTTOM_RIGHT_RADIUS, LogicalProps.physical(Prop.BORDER_END_START_RADIUS, "horizontal-tb", "rtl"), "end-start radius in rtl");
        Check.equal(Prop.BORDER_TOP_RIGHT_RADIUS, LogicalProps.physical(Prop.BORDER_START_START_RADIUS, "vertical-rl", "ltr"), "start-start radius in vertical-rl");
        Check.equal(Prop.WIDTH, LogicalProps.physical(Prop.INLINE_SIZE, "horizontal-tb", "ltr"), "inline-size");
        Check.equal(Prop.HEIGHT, LogicalProps.physical(Prop.INLINE_SIZE, "vertical-lr", "ltr"), "inline-size in a vertical mode");
        Check.equal(Prop.MIN_HEIGHT, LogicalProps.physical(Prop.MIN_BLOCK_SIZE, "horizontal-tb", "ltr"), "min-block-size");
        Check.equal(Prop.MAX_WIDTH, LogicalProps.physical(Prop.MAX_BLOCK_SIZE, "vertical-rl", "ltr"), "max-block-size in a vertical mode");
        Check.equal(Prop.COLOR, LogicalProps.physical(Prop.COLOR, "vertical-rl", "rtl"), "a physical id is unchanged");
        Check.isTrue(LogicalProps.isLogical(Prop.BORDER_INLINE_END_COLOR) && !LogicalProps.isLogical(Prop.BORDER_LEFT_COLOR), "isLogical");
    }

    /**
     * Margins, paddings and borders of horizontal text, left to right and right to left.
     * <p>
     * 가로쓰기의 margin, padding, border(왼쪽에서 오른쪽, 오른쪽에서 왼쪽).
     */
    public void testHorizontalBoxes() {
        LayoutBox a = box("<div id=a style='margin-block-start:10px;margin-block-end:11px;margin-inline-start:20px;margin-inline-end:21px;width:100px;height:50px'></div>", "a");
        margins(a, 10, 21, 11, 20, "ltr");
        Check.near(20, a.getX(), T, "the box is placed by its logical margin");
        Check.near(10, a.getY(), T, "(y)");
        LayoutBox r = box("<div style='direction:rtl'><div id=a style='margin-inline-start:20px;margin-inline-end:30px;width:100px;height:50px'></div></div>", "a");
        margins(r, 0, 20, 0, 30, "rtl");
        LayoutBox p = box("<div id=a style='padding-block:3px 4px;padding-inline:5px 6px;border:0 solid red;width:100px;height:50px'></div>", "a");
        Check.near(3, p.getPaddingTop(), T, "padding-block-start");
        Check.near(4, p.getPaddingBottom(), T, "padding-block-end");
        Check.near(5, p.getPaddingLeft(), T, "padding-inline-start");
        Check.near(6, p.getPaddingRight(), T, "padding-inline-end");
        LayoutBox bb = box("<div id=a style='border-inline-start:4px solid red;border-block-end:6px solid blue;width:100px;height:50px'></div>", "a");
        Check.near(4, bb.getBorderLeft(), T, "border-inline-start");
        Check.near(0, bb.getBorderRight(), T, "no border at the inline end");
        Check.near(6, bb.getBorderBottom(), T, "border-block-end");
        Check.near(0, bb.getBorderTop(), T, "no border at the block start");
        LayoutBox w = box("<div id=a style='border-inline-width:1px 2px;border-block-width:3px 4px;border-style:solid;width:100px;height:50px'></div>", "a");
        Check.near(1, w.getBorderLeft(), T, "border-inline-width start");
        Check.near(2, w.getBorderRight(), T, "border-inline-width end");
        Check.near(3, w.getBorderTop(), T, "border-block-width start");
        Check.near(4, w.getBorderBottom(), T, "border-block-width end");
        LayoutBox st = box("<div id=a style='border-inline-style:solid dashed;border-inline-width:5px;border-block:0 none'></div>", "a");
        Check.equal("solid", st.getStyle().get(Prop.BORDER_LEFT_STYLE).toCssText(), "border-inline-style start");
        Check.equal("dashed", st.getStyle().get(Prop.BORDER_RIGHT_STYLE).toCssText(), "border-inline-style end");
        Check.near(5, st.getBorderLeft(), T, "the width of a solid inline border");
        LayoutBox rb = box("<div id=a style='direction:rtl;border-inline-start:7px solid red;width:100px;height:20px'></div>", "a");
        Check.near(7, rb.getBorderRight(), T, "in rtl the inline start border is on the right");
        Check.near(0, rb.getBorderLeft(), T, "(left)");
    }

    /**
     * The vertical writing modes: the block axis is horizontal.
     * <p>
     * 세로쓰기: 블록 축이 가로이다.
     */
    public void testVerticalModes() {
        String m = "margin-block-start:5px;margin-block-end:6px;margin-inline-start:7px;margin-inline-end:8px;width:60px;height:80px";
        margins(box("<div id=a style='writing-mode:vertical-rl;" + m + "'></div>", "a"), 7, 5, 8, 6, "vertical-rl");
        margins(box("<div id=a style='writing-mode:vertical-lr;" + m + "'></div>", "a"), 7, 6, 8, 5, "vertical-lr");
        margins(box("<div id=a style='writing-mode:sideways-lr;" + m + "'></div>", "a"), 8, 6, 7, 5, "sideways-lr");
        margins(box("<div id=a style='writing-mode:vertical-rl;direction:rtl;" + m + "'></div>", "a"), 8, 5, 7, 6, "vertical-rl rtl");
        LayoutBox s = box("<div id=a style='writing-mode:vertical-rl;inline-size:80px;block-size:30px'></div>", "a");
        Check.near(30, s.getWidth(), T, "block-size is the width in a vertical mode");
        Check.near(80, s.getHeight(), T, "inline-size is the height in a vertical mode");
        LayoutBox h = box("<div id=a style='inline-size:80px;block-size:30px'></div>", "a");
        Check.near(80, h.getWidth(), T, "inline-size is the width");
        Check.near(30, h.getHeight(), T, "block-size is the height");
        LayoutBox mx = box("<div id=a style='width:100px;max-inline-size:50px;min-block-size:40px;height:10px'></div>", "a");
        Check.near(50, mx.getWidth(), T, "max-inline-size caps the width");
        Check.near(40, mx.getHeight(), T, "min-block-size raises the height");
    }

    /**
     * The offsets of a positioned box follow the writing mode and the direction.
     * <p>
     * 위치 지정 상자의 오프셋은 쓰기 모드와 방향을 따른다.
     */
    public void testInset() {
        String box = "<div style='position:relative;width:300px;height:100px'><div id=a style='position:absolute;%s;width:50px;height:20px'></div></div>";
        LayoutBox l = box(String.format(box, "inset-inline-start:11px;inset-block-start:7px"), "a");
        Check.near(11, l.getX(), T, "inset-inline-start is left");
        Check.near(7, l.getY(), T, "inset-block-start is top");
        LayoutBox r = box("<div style='position:relative;width:300px;height:100px;direction:rtl'><div id=a style='position:absolute;inset-inline-start:11px;width:50px;height:20px'></div></div>", "a");
        Check.near(300 - 11 - 50, r.getX(), T, "in rtl inset-inline-start is right");
        LayoutBox pair = box("<div style='position:relative;width:300px;height:100px'><div id=a style='position:absolute;inset-inline:10px 20px;inset-block:5px 7px'></div></div>", "a");
        Check.near(10, pair.getX(), T, "inset-inline start");
        Check.near(5, pair.getY(), T, "inset-block start");
        Check.near(300 - 10 - 20, pair.getWidth(), T, "the width is what is left between the two inline insets");
        Check.near(100 - 5 - 7, pair.getHeight(), T, "the height is what is left between the two block insets");
    }

    /**
     * The order of the cascade between logical and physical declarations.
     * <p>
     * 논리 선언과 물리 선언 사이의 계단식 순서.
     */
    public void testCascadeOrder() {
        margins(box("<div id=a style='margin-left:10px;margin-inline-start:20px'></div>", "a"), 0, 0, 0, 20, "the later logical declaration wins");
        margins(box("<div id=a style='margin-inline-start:20px;margin-left:10px'></div>", "a"), 0, 0, 0, 10, "the later physical declaration wins");
        margins(box("<div id=a style='margin-inline-start:20px !important;margin-left:10px'></div>", "a"), 0, 0, 0, 20, "!important wins");
        margins(box("<style>#a{margin-inline-start:20px}</style><div id=a style='margin-left:7px'></div>", "a"), 0, 0, 0, 7, "the style attribute beats the rule");
        margins(box("<style>#a{margin-left:20px}</style><div id=a style='margin-inline-start:7px'></div>", "a"), 0, 0, 0, 7, "a logical declaration in the attribute beats a physical rule");
        margins(box("<style>div{margin-inline-start:20px} #a{margin-left:5px}</style><div id=a></div>", "a"), 0, 0, 0, 5, "specificity decides between a logical and a physical rule");
        margins(box("<style>#a{margin-left:5px} #a{margin-inline-start:9px}</style><div id=a></div>", "a"), 0, 0, 0, 9, "source order between rules");
        margins(box("<div id=a style='margin:3px;margin-inline:10px'></div>", "a"), 3, 10, 3, 10, "a logical shorthand after the margin shorthand");
        margins(box("<div id=a style='margin-inline:10px;margin:3px'></div>", "a"), 3, 3, 3, 3, "the margin shorthand after a logical one");
    }

    /**
     * The direction and the writing mode that are declared later, inherited or taken from a variable decide the mapping.
     * <p>
     * 나중에 선언하거나 상속하거나 변수에서 가져온 방향과 쓰기 모드가 매핑을 정한다.
     */
    public void testDirectionSources() {
        margins(box("<div id=a style='margin-inline-start:15px;direction:rtl'></div>", "a"), 0, 15, 0, 0, "direction declared after");
        margins(box("<div style='direction:rtl'><div id=a style='margin-inline-start:15px'></div></div>", "a"), 0, 15, 0, 0, "direction inherited");
        margins(box("<div style='direction:rtl'><div id=a style='direction:ltr;margin-inline-start:15px'></div></div>", "a"), 0, 0, 0, 15, "direction overridden");
        margins(box("<style>#a{margin-inline-start:15px} .r{direction:rtl}</style><div id=a class=r></div>", "a"), 0, 15, 0, 0, "direction from another rule");
        margins(box("<div id=a style='--m:15px;margin-inline-start:var(--m);direction:rtl'></div>", "a"), 0, 15, 0, 0, "var() in a logical property");
        margins(box("<div id=a style='--m:15px;margin-inline:var(--m) 7px;direction:rtl'></div>", "a"), 0, 15, 0, 7, "var() in a logical shorthand");
        margins(box("<div style='writing-mode:vertical-rl'><div id=a style='margin-block-start:15px'></div></div>", "a"), 0, 15, 0, 0, "writing-mode is inherited");
    }

    /**
     * The radii and the computed values that are asked for by their logical names.
     * <p>
     * 반지름과 논리 이름으로 묻는 계산 값.
     */
    public void testRadiiAndComputedValues() {
        LayoutBox a = box("<div id=a style='border-start-start-radius:10px;border-end-end-radius:20px 5px'></div>", "a");
        Check.equal("10px", a.getStyle().get(Prop.BORDER_TOP_LEFT_RADIUS).toCssText(), "start-start is the top left in ltr");
        Check.equal("20px 5px", a.getStyle().get(Prop.BORDER_BOTTOM_RIGHT_RADIUS).toCssText(), "end-end is the bottom right in ltr");
        LayoutBox r = box("<div id=a style='direction:rtl;border-start-start-radius:10px;border-end-start-radius:6px'></div>", "a");
        Check.equal("10px", r.getStyle().get(Prop.BORDER_TOP_RIGHT_RADIUS).toCssText(), "start-start is the top right in rtl");
        Check.equal("6px", r.getStyle().get(Prop.BORDER_BOTTOM_RIGHT_RADIUS).toCssText(), "end-start is the bottom right in rtl");
        LayoutBox c = box("<div id=a style='direction:rtl;margin-inline-start:15px;margin-block-end:4px;padding-inline-end:9px'></div>", "a");
        Check.equal("15px", c.getStyle().getPropertyValue("margin-inline-start"), "getPropertyValue of a logical name gives the physical value");
        Check.equal("15px", c.getStyle().getPropertyValue("margin-right"), "and the physical name gives the same");
        Check.equal("4px", c.getStyle().getPropertyValue("margin-block-end"), "block end");
        Check.equal("9px", c.getStyle().getPropertyValue("padding-inline-end"), "padding inline end in rtl is the left padding");
        Check.equal("9px", c.getStyle().getPropertyValue("padding-left"), "(left)");
        Check.equal("0px", c.getStyle().getPropertyValue("margin-inline-end"), "the other side stays 0");
    }
}
