package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the basic form of CSS Anchor Positioning Level 1 (plan item P10-08, https://www.w3.org/TR/css-anchor-position-1/, W3C Working Draft, Last-Modified 2026-03-27):
 * <code>anchor-name</code> makes an anchor, an absolutely positioned box takes the edges of the anchor box as its insets with <code>anchor()</code> (sides, percentages,
 * <code>center</code>, <code>inside</code>, <code>outside</code>, the fallback value) and its size with <code>anchor-size()</code>, the default anchor of
 * <code>position-anchor</code> is used when the function has no name, the target anchor element is the nearest ancestor or else the last one in tree order, an anchor that is
 * not laid out before the box is not acceptable, and a function that cannot be resolved computes to its fallback value or to <code>auto</code>.
 * <p>
 * CSS Anchor Positioning Level 1 의 기본 형태 시험(계획 P10-08): <code>anchor-name</code> 이 앵커를 만들고, 절대 배치 상자는 <code>anchor()</code> 로 앵커 상자의 가장자리(변, 백분율,
 * <code>center</code>, <code>inside</code>, <code>outside</code>, 대체값)를 오프셋으로, <code>anchor-size()</code> 로 앵커 상자의 크기를 가지며, 함수에 이름이 없으면
 * <code>position-anchor</code> 의 기본 앵커를 쓰고, 대상 앵커 요소는 가장 가까운 조상이거나 트리 순서의 마지막 것이며, 상자보다 먼저 배치되지 않은 앵커는 쓸 수 없고, 풀 수 없는
 * 함수는 대체값이나 <code>auto</code> 로 계산된다.
 */
public class AnchorPositioningLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AnchorPositioningLayoutTest() {
    }

    /** A container of 300 by 200 at the origin of the page that is the containing block of the positioned box, with a 40 px spacer. 페이지 원점에 있는 300x200 컨테이너(배치 상자의 포함 블록)와 40px 간격 상자. */
    private static final String WRAP = "<div id=wrap style='position:relative;width:300px;height:200px;margin:0'><div style='height:40px'></div>";

    /** The anchor: 100 by 30 at (50, 40) of the container. 앵커: 컨테이너의 (50, 40) 에 있는 100x30. */
    private static final String ANCHOR = "<div id=anc style='anchor-name:--a;margin-left:50px;width:100px;height:30px'></div>";

    /**
     * Lays out the container with the anchor and a positioned box that has the style.
     * <p>
     * 앵커와 주어진 스타일의 배치 상자가 든 컨테이너를 배치한다. 배치 상자는 앵커 앞에 있어서 정적 위치는 (0, 40) 이다.
     *
     * @param style the style of the positioned box / 배치 상자의 스타일
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
     * <code>top</code> and <code>left</code> take the bottom and the left edge of the anchor (the default anchor of <code>position-anchor</code> or the name in the
     * function).
     * <p>
     * <code>top</code> 과 <code>left</code> 가 앵커의 아래쪽과 왼쪽 가장자리를 가진다(<code>position-anchor</code> 의 기본 앵커나 함수 안의 이름).
     */
    public void testTopAndLeft() {
        LayoutResult r = layout("position-anchor:--a;top:anchor(bottom);left:anchor(left);width:60px;height:20px");
        Check.near(70, y(r, "pop"), 0.01, "top = the bottom edge of the anchor (40 + 30)");
        Check.near(50, x(r, "pop"), 0.01, "left = the left edge of the anchor");
        LayoutResult named = layout("top:anchor(--a bottom);left:anchor(--a right);width:60px;height:20px");
        Check.near(70, y(named, "pop"), 0.01, "a name in the function, without position-anchor");
        Check.near(150, x(named, "pop"), 0.01, "left = the right edge of the anchor (50 + 100)");
    }

    /**
     * <code>bottom</code> and <code>right</code> align the opposite edge of the containing block with the edge of the anchor: the bottom edge of the positioned box lies on
     * the top edge of the anchor.
     * <p>
     * <code>bottom</code> 과 <code>right</code> 는 포함 블록의 반대쪽 가장자리를 앵커의 가장자리에 맞춘다: 배치 상자의 아래쪽 가장자리가 앵커의 위쪽 가장자리에 놓인다.
     */
    public void testBottomAndRight() {
        LayoutResult r = layout("position-anchor:--a;bottom:anchor(top);right:anchor(right);width:60px;height:20px");
        Check.near(40, y(r, "pop") + 20, 0.01, "the bottom edge of the box is the top edge of the anchor (y 40)");
        Check.near(150, x(r, "pop") + 60, 0.01, "the right edge of the box is the right edge of the anchor (x 150)");
    }

    /**
     * Percentages, <code>center</code>, <code>inside</code> and <code>outside</code>: <code>outside</code> is the opposite side of the property, <code>inside</code> the same.
     * <p>
     * 백분율, <code>center</code>, <code>inside</code>, <code>outside</code>: <code>outside</code> 는 속성의 반대쪽 변이고 <code>inside</code> 는 같은 쪽 변이다.
     */
    public void testPercentagesAndKeywords() {
        Check.near(100, x(layout("position-anchor:--a;left:anchor(center);top:0;width:10px;height:10px"), "pop"), 0.01, "center = 50 % of the anchor");
        Check.near(75, x(layout("left:anchor(--a 25%);top:0;width:10px;height:10px"), "pop"), 0.01, "25 % of the width of the anchor (50 + 25)");
        Check.near(70, y(layout("position-anchor:--a;top:anchor(outside);left:0;width:10px;height:10px"), "pop"), 0.01, "top: outside is the bottom edge of the anchor");
        Check.near(40, y(layout("position-anchor:--a;top:anchor(inside);left:0;width:10px;height:10px"), "pop"), 0.01, "top: inside is the top edge");
        Check.near(50, x(layout("position-anchor:--a;left:anchor(inside);top:0;width:10px;height:10px"), "pop"), 0.01, "left: inside is the left edge");
        Check.near(150, x(layout("position-anchor:--a;left:anchor(outside);top:0;width:10px;height:10px"), "pop"), 0.01, "left: outside is the right edge");
        LayoutResult startEnd = layout("position-anchor:--a;top:anchor(end);left:anchor(start);width:10px;height:10px");
        Check.near(70, y(startEnd, "pop"), 0.01, "end of the block axis is the bottom");
        Check.near(50, x(startEnd, "pop"), 0.01, "start of the inline axis is the left");
    }

    /**
     * <code>anchor-size()</code> gives the width or the height of the anchor box as a length (also <code>inline</code> and <code>block</code>), in the size properties and
     * their minimum and maximum.
     * <p>
     * <code>anchor-size()</code> 는 앵커 상자의 너비나 높이를 길이로 준다(<code>inline</code> 과 <code>block</code> 도). 크기 속성과 그 최소/최대 크기에서 쓴다.
     */
    public void testAnchorSize() {
        LayoutResult r = layout("position-anchor:--a;top:0;left:0;width:anchor-size(width);height:anchor-size(--a height)");
        Check.near(100, LayoutTestSupport.box(r, "pop").getWidth(), 0.01, "width = the width of the anchor");
        Check.near(30, LayoutTestSupport.box(r, "pop").getHeight(), 0.01, "height = the height of the anchor");
        LayoutResult inline = layout("position-anchor:--a;top:0;left:0;width:anchor-size(inline);height:anchor-size(block)");
        Check.near(100, LayoutTestSupport.box(inline, "pop").getWidth(), 0.01, "inline size in a horizontal writing mode");
        Check.near(30, LayoutTestSupport.box(inline, "pop").getHeight(), 0.01, "block size");
        LayoutResult max = layout("position-anchor:--a;top:0;left:0;width:300px;height:10px;max-width:anchor-size(width)");
        Check.near(100, LayoutTestSupport.box(max, "pop").getWidth(), 0.01, "max-width is limited to the width of the anchor");
    }

    /**
     * An unresolvable function: a name that no element has, a side of the other axis and a missing default anchor compute to the fallback value, or to <code>auto</code>
     * (the static position) without one.
     * <p>
     * 풀 수 없는 함수: 어떤 요소에도 없는 이름, 다른 축의 변, 기본 앵커가 없는 경우는 대체값으로, 대체값이 없으면 <code>auto</code>(정적 위치)로 계산된다.
     */
    public void testUnresolvableFunctions() {
        Check.near(5, y(layout("top:anchor(--missing bottom, 5px);left:0;width:10px;height:10px"), "pop"), 0.01, "no such anchor: the fallback value");
        Check.near(3, y(layout("top:anchor(bottom, 3px);left:0;width:10px;height:10px"), "pop"), 0.01, "without a name and a default anchor: the fallback value");
        Check.near(8, x(layout("left:anchor(--a top, 8px);top:0;width:10px;height:10px"), "pop"), 0.01, "a vertical side in a horizontal property: the fallback value");
        // 대체값이 없으면 auto 이므로 정적 위치(앵커 앞 흐름의 y 40)에 놓인다.
        LayoutResult auto = layout("top:anchor(--missing bottom);left:0;width:10px;height:10px");
        Check.near(40, y(auto, "pop"), 0.01, "no fallback: auto, the static position (y 40)");
        Check.near(40, y(layout("left:0;width:10px;height:10px"), "pop"), 0.01, "the static position without any inset is y 40, not the bottom of the anchor");
    }

    /**
     * The target anchor element: the nearest ancestor of the positioned box, else the last element with the name in tree order; an anchor after the box in the tree is fine
     * when it is in the flow (it is laid out before the positioned box), an anchor that is positioned itself and comes later is not acceptable.
     * <p>
     * 대상 앵커 요소: 배치 상자의 가장 가까운 조상이고, 없으면 트리 순서에서 그 이름을 가진 마지막 요소이다. 흐름 안의 앵커는 트리에서 상자 뒤에 있어도 된다(배치 상자보다 먼저
     * 배치된다). 자신이 배치되었고 뒤에 오는 앵커는 쓸 수 없다.
     */
    public void testTargetAnchorElement() {
        // 같은 이름의 앵커가 둘이면 트리 순서에서 뒤의 것(y 100~130)을 쓴다.
        LayoutResult two = LayoutTestSupport.layout(WRAP + ANCHOR + "<div id=anc2 style='anchor-name:--a;margin-left:20px;width:50px;height:30px'></div>"
                + "<div id=pop style='position:absolute;position-anchor:--a;top:anchor(bottom);left:anchor(left);width:10px;height:10px'></div></div>", 600);
        Check.near(100, y(two, "pop"), 0.01, "the last anchor in tree order (the second one ends at y 100)");
        Check.near(20, x(two, "pop"), 0.01, "its left edge");
        // 앵커가 상자 뒤에 있어도 흐름 안이면 쓸 수 있다.
        LayoutResult later = LayoutTestSupport.layout(WRAP + "<div id=pop style='position:absolute;position-anchor:--a;top:anchor(bottom);left:anchor(left);width:10px;height:10px'></div>"
                + "<div id=anc style='anchor-name:--a;margin-left:50px;width:100px;height:30px'></div></div>", 600);
        Check.near(70, y(later, "pop"), 0.01, "an anchor in the flow after the box in tree order is acceptable");
        // 자신이 배치된 앵커가 뒤에 오면 쓸 수 없어 정적 위치로 돌아간다.
        LayoutResult notYet = LayoutTestSupport.layout(WRAP + "<div id=pop style='position:absolute;top:anchor(--a bottom);left:0;width:10px;height:10px'></div>"
                + "<div id=anc style='position:absolute;top:60px;left:50px;anchor-name:--a;width:100px;height:30px'></div></div>", 600);
        Check.near(40, y(notYet, "pop"), 0.01, "a positioned anchor that comes later is not acceptable: auto, the static position (y 40)");
        // 앞에 오는 배치된 앵커는 쓸 수 있다.
        LayoutResult before = LayoutTestSupport.layout(WRAP + "<div id=anc style='position:absolute;top:60px;left:50px;anchor-name:--a;width:100px;height:30px'></div>"
                + "<div id=pop style='position:absolute;top:anchor(--a bottom);left:anchor(--a left);width:10px;height:10px'></div></div>", 600);
        Check.near(90, y(before, "pop"), 0.01, "a positioned anchor before the box is acceptable (60 + 30)");
        Check.near(50, x(before, "pop"), 0.01, "its left edge");
        // 가장 가까운 조상이 앵커이면 그것을 쓴다(앞에 같은 이름의 다른 앵커가 있어도).
        LayoutResult ancestor = LayoutTestSupport.layout("<div id=wrap style='position:relative;width:300px;height:200px;margin:0'>"
                + "<div style='anchor-name:--a;height:20px'></div>"
                + "<div id=anc style='anchor-name:--a;margin-top:30px;height:50px'><div id=pop style='position:absolute;top:anchor(--a bottom);left:0;width:10px;height:10px'></div></div></div>", 600);
        Check.near(100, y(ancestor, "pop"), 0.01, "the ancestor with the name is the anchor: its bottom edge (20 + 30 + 50)");
    }

    /**
     * Without a positioned ancestor the containing block is the initial one, and every box of the page can be the anchor.
     * <p>
     * 배치된 조상이 없으면 포함 블록은 초기 블록이고 페이지의 모든 상자가 앵커가 될 수 있다.
     */
    public void testInitialContainingBlock() {
        LayoutResult r = LayoutTestSupport.layout("<div style='height:25px'></div><div style='margin-left:30px'><div id=anc style='anchor-name:--b;width:80px;height:15px'></div></div>"
                + "<div id=pop style='position:absolute;top:anchor(--b bottom);left:anchor(--b left);width:10px;height:10px'></div>", 600);
        Check.near(40, LayoutTestSupport.box(r, "pop").getY(), 0.01, "the bottom edge of the anchor in page coordinates (25 + 15)");
        Check.near(30, LayoutTestSupport.box(r, "pop").getX(), 0.01, "the left edge of the anchor");
    }

    /**
     * The properties: <code>anchor-name</code> takes dashed names (a list) or <code>none</code>, <code>position-anchor</code> a dashed name or a keyword, the inset and size
     * properties take <code>anchor()</code> and <code>anchor-size()</code> but no other function.
     * <p>
     * 속성: <code>anchor-name</code> 은 대시 이름(목록)이나 <code>none</code> 을, <code>position-anchor</code> 는 대시 이름이나 키워드를 받고, 오프셋과 크기 속성은
     * <code>anchor()</code> 와 <code>anchor-size()</code> 만 받고 다른 함수는 받지 않는다.
     */
    public void testProperties() {
        org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry reg = org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.getInstance();
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "anchor-name", "--a, --b"), "a list of dashed names");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "anchor-name", "none"), "none");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.INVALID, parse(reg, "anchor-name", "a"), "a name without the dashes");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "position-anchor", "--a"), "position-anchor with a name");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "position-anchor", "auto"), "position-anchor: auto");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "top", "anchor(--a top, 10px)"), "anchor() in top");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "inset-block-start", "anchor(bottom)"), "anchor() in a logical inset");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "width", "anchor-size(width)"), "anchor-size() in width");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.OK, parse(reg, "max-height", "anchor-size(--a height, 20px)"), "anchor-size() in max-height");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.INVALID, parse(reg, "top", "foo(1)"), "another function is invalid");
        Check.equal(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry.INVALID, parse(reg, "color", "anchor(top)"), "anchor() in another property is invalid");
    }

    /**
     * Parses a declaration and returns the result code of the registry.
     * <p>
     * 선언을 파싱해 등록부의 결과 코드를 반환한다.
     *
     * @param reg   the registry / 등록부
     * @param name  the property / 속성
     * @param value the value / 값
     * @return the result code / 결과 코드
     */
    private static int parse(org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry reg, String name, String value) {
        return reg.parse(name, value, new java.util.LinkedHashMap<Integer, org.duckdns.hjow.swingwebpanel.css.values.CssValue>());
    }

    /**
     * A box without anchor functions is not touched, and the anchor properties are known to the style: the computed value of <code>top</code> keeps the function.
     * <p>
     * 앵커 함수가 없는 상자는 건드리지 않으며, 앵커 속성은 스타일이 알고 있다: <code>top</code> 의 계산 값은 함수를 유지한다.
     */
    public void testNothingChangesWithoutFunctions() {
        LayoutResult r = layout("top:12px;left:7px;width:10px;height:10px");
        Check.near(12, y(r, "pop"), 0.01, "a normal inset");
        Check.near(7, x(r, "pop"), 0.01, "a normal inset");
        Check.isFalse(AnchorPositioning.uses(LayoutTestSupport.box(r, "pop").getStyle()), "no function");
    }
}
