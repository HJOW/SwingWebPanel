package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of two parts of CSS Anchor Positioning Level 1 (plan item P10-08-d, https://www.w3.org/TR/css-anchor-position-1/): <code>anchor()</code> and
 * <code>anchor-size()</code> inside <code>calc()</code>, <code>min()</code>, <code>max()</code> and <code>clamp()</code> (sections 3.2 and 5.1), and the implicit anchor
 * element (2.4 and 2.4.1: <code>position-anchor: auto</code>, <code>normal</code> together with a <code>position-area</code>, <code>match-parent</code>; the implicit anchor
 * of a pseudo-element is its originating element). The container is 300 by 200 and the anchor is 100 by 30 at (50, 40) in it.
 * <p>
 * CSS Anchor Positioning Level 1 의 두 부분 시험(계획 P10-08-d): <code>calc()</code>, <code>min()</code>, <code>max()</code>, <code>clamp()</code> 안의 <code>anchor()</code> 와
 * <code>anchor-size()</code>(3.2, 5.1 절), 암묵 앵커 요소(2.4, 2.4.1: <code>position-anchor: auto</code>, <code>position-area</code> 와 함께 쓴 <code>normal</code>,
 * <code>match-parent</code>. 의사 요소의 암묵 앵커는 그 원래 요소). 컨테이너는 300x200 이고 앵커는 그 안 (50, 40) 의 100x30 이다.
 */
public class AnchorCalcImplicitLayoutTest {

    /** The container of 300 by 200 with a 40 px spacer. 40px 간격 상자가 있는 300x200 컨테이너. */
    private static final String WRAP = "<div id=wrap style='position:relative;width:300px;height:200px;margin:0'><div style='height:40px'></div>";

    /** The anchor: 100 by 30 at (50, 40). 앵커: (50, 40) 의 100x30. */
    private static final String ANCHOR = "<div id=anc style='anchor-name:--a;margin-left:50px;width:100px;height:30px'></div>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AnchorCalcImplicitLayoutTest() {
    }

    /**
     * Lays out the container with a positioned box that has a style.
     * <p>
     * 스타일이 있는 배치 상자가 든 컨테이너를 배치한다.
     *
     * @param style the style of the box / 상자의 스타일
     * @return the layout / 레이아웃
     */
    private static LayoutResult layout(String style) {
        return LayoutTestSupport.layout(WRAP + "<div id=pop style='position:absolute;" + style + "'></div>" + ANCHOR + "</div>", 600);
    }

    /**
     * The x of a box relative to the container.
     * <p>
     * 컨테이너에 대한 상자의 x.
     *
     * @param r  the layout / 레이아웃
     * @param id the id / id
     * @return x / x
     */
    private static double x(LayoutResult r, String id) {
        return LayoutTestSupport.box(r, id).getX() - LayoutTestSupport.box(r, "wrap").getX();
    }

    /**
     * The y of a box relative to the container.
     * <p>
     * 컨테이너에 대한 상자의 y.
     *
     * @param r  the layout / 레이아웃
     * @param id the id / id
     * @return y / y
     */
    private static double y(LayoutResult r, String id) {
        return LayoutTestSupport.box(r, id).getY() - LayoutTestSupport.box(r, "wrap").getY();
    }

    /**
     * A function in <code>calc()</code> gives its length to the expression.
     * <p>
     * <code>calc()</code> 안의 함수는 그 길이를 식에 준다.
     */
    public void testAnchorInCalc() {
        LayoutResult r = layout("top:calc(anchor(--a bottom) + 10px);left:calc(anchor(--a left) - 5px);width:calc(anchor-size(--a width) / 2);height:calc(anchor-size(--a height) * 2)");
        Check.near(80, y(r, "pop"), 0.01, "top = the bottom edge of the anchor (70) + 10");
        Check.near(45, x(r, "pop"), 0.01, "left = the left edge of the anchor (50) - 5");
        Check.near(50, LayoutTestSupport.box(r, "pop").getWidth(), 0.01, "width = half the width of the anchor");
        Check.near(60, LayoutTestSupport.box(r, "pop").getHeight(), 0.01, "height = twice the height of the anchor");
    }

    /**
     * <code>min()</code>, <code>max()</code> and <code>clamp()</code> take the functions as arguments, and the default anchor is used when the name is left out.
     * <p>
     * <code>min()</code>, <code>max()</code>, <code>clamp()</code> 는 함수를 인수로 받으며 이름을 생략하면 기본 앵커를 쓴다.
     */
    public void testAnchorInMathFunctions() {
        LayoutResult r = layout("position-anchor:--a;left:max(anchor(left), 70px);top:clamp(0px, anchor(top), 30px);width:min(anchor-size(width), 80px);height:10px");
        Check.near(70, x(r, "pop"), 0.01, "max(50, 70)");
        Check.near(30, y(r, "pop"), 0.01, "clamp(0, 40, 30)");
        Check.near(80, LayoutTestSupport.box(r, "pop").getWidth(), 0.01, "min(100, 80)");
    }

    /**
     * A function that cannot be resolved uses its fallback inside the expression; without a fallback the property is <code>auto</code>.
     * <p>
     * 풀 수 없는 함수는 식 안에서 대체값을 쓴다. 대체값이 없으면 속성은 <code>auto</code> 이다.
     */
    public void testFallbackInCalc() {
        LayoutResult r = layout("top:calc(anchor(--missing top, 5px) + 1px);left:0;width:10px;height:10px");
        Check.near(6, y(r, "pop"), 0.01, "the fallback 5px plus 1px");
        LayoutResult none = layout("top:calc(anchor(--missing top) + 1px);left:0;width:10px;height:10px");
        Check.near(40, y(none, "pop"), 0.01, "no fallback: top is auto, the static position (below the spacer)");
    }

    /**
     * The implicit anchor of a pseudo-element is its originating element (<code>position-anchor: auto</code>).
     * <p>
     * 의사 요소의 암묵 앵커는 그 원래 요소이다(<code>position-anchor: auto</code>).
     */
    public void testImplicitAnchorOfAPseudoElement() {
        LayoutResult r = LayoutTestSupport.layout("<style>#host::after{content:'';position:absolute;position-anchor:auto;top:anchor(bottom);left:anchor(left);width:10px;height:10px}</style>"
                + "<div id=host style='position:relative;margin:20px;width:200px;height:100px'></div>", 600);
        LayoutBox host = LayoutTestSupport.box(r, "host");
        LayoutBox after = null;
        // 호스트의 자식 중 ::after 의사 요소 상자를 찾는다.
        for (LayoutBox c : host.getChildren()) {
            if ("after".equals(c.getPseudo())) {
                after = c;
            }
        }
        Check.notNull(after, "the ::after box");
        Check.near(host.getY() + 100, after.getY(), 0.01, "top: anchor(bottom) is the bottom edge of the host");
        Check.near(host.getX(), after.getX(), 0.01, "left: anchor(left) is the left edge of the host");
    }

    /**
     * <code>normal</code> is <code>none</code> without a <code>position-area</code> and <code>auto</code> with one.
     * <p>
     * <code>normal</code> 은 <code>position-area</code> 가 없으면 <code>none</code> 이고 있으면 <code>auto</code> 이다.
     */
    public void testNormalDependsOnThePositionArea() {
        // 컨테이너(300x200, 포함 블록)의 (50, 40) 에 있는 100x30 호스트의 ::after 가 배치 상자이다.
        String base = "<style>#host::after{content:'';position:absolute;width:10px;height:10px;%s}</style>"
                + "<div id=wrap style='position:relative;width:300px;height:200px;margin:0'><div style='height:40px'></div><div id=host style='margin-left:50px;width:100px;height:30px'></div></div>";
        LayoutResult none = LayoutTestSupport.layout(String.format(base, "top:anchor(bottom, 7px);left:0"), 600);
        LayoutBox after = pseudoAfter(none, "wrap");
        Check.near(LayoutTestSupport.box(none, "wrap").getY() + 7, after.getY(), 0.01, "normal without a position-area: no default anchor, the fallback is used");
        LayoutResult withArea = LayoutTestSupport.layout(String.format(base, "position-area:bottom"), 600);
        LayoutBox after2 = pseudoAfter(withArea, "wrap");
        LayoutBox wrap = LayoutTestSupport.box(withArea, "wrap");
        Check.near(wrap.getY() + 70, after2.getY(), 0.01, "normal with a position-area is auto: the host is the anchor, the area is below it (40 + 30)");
        Check.near(wrap.getX() + 95, after2.getX(), 0.01, "and the box is centered on the host (100 - 5)");
    }

    /**
     * Finds the <code>::after</code> box below a box (it can be anywhere below, a positioned box lives in its containing block).
     * <p>
     * 상자 아래의 <code>::after</code> 상자를 찾는다(어디에든 있을 수 있다. 배치 상자는 포함 블록 안에 있다).
     *
     * @param r  the layout / 레이아웃
     * @param id the id of the box to search below / 아래를 찾을 상자의 id
     * @return the box / 상자
     */
    private static LayoutBox pseudoAfter(LayoutResult r, String id) {
        LayoutBox found = findAfter(LayoutTestSupport.box(r, id));
        Check.notNull(found, "the ::after box");
        return found;
    }

    /**
     * Searches the <code>::after</code> box in tree order.
     * <p>
     * <code>::after</code> 상자를 트리 순서로 찾는다.
     *
     * @param b the root of the search / 찾기의 루트
     * @return the box or null / 상자. 없으면 null
     */
    private static LayoutBox findAfter(LayoutBox b) {
        if ("after".equals(b.getPseudo())) {
            return b;
        }
        // 자식을 차례로 훑으며 처음 찾은 것을 반환한다.
        for (LayoutBox c : b.getChildren()) {
            LayoutBox f = findAfter(c);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    /**
     * <code>match-parent</code> uses the default anchor of the parent element, and for a pseudo-element its originating element.
     * <p>
     * <code>match-parent</code> 는 부모 요소의 기본 앵커를 쓰고, 의사 요소이면 그 원래 요소를 쓴다.
     */
    public void testMatchParent() {
        LayoutResult r = LayoutTestSupport.layout(WRAP + "<div id=par style='position-anchor:--a'><div id=pop style='position:absolute;position-anchor:match-parent;top:anchor(bottom);left:0;width:10px;height:10px'></div></div>"
                + ANCHOR + "</div>", 600);
        Check.near(70, y(r, "pop"), 0.01, "the anchor of the parent is --a: top = the bottom edge of the anchor");
        LayoutResult plain = LayoutTestSupport.layout(WRAP + "<div id=par><div id=pop style='position:absolute;position-anchor:match-parent;top:anchor(bottom, 3px);left:0;width:10px;height:10px'></div></div>"
                + ANCHOR + "</div>", 600);
        Check.near(3, y(plain, "pop"), 0.01, "the parent has no default anchor: the fallback is used");
    }
}
