package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the overflow management of CSS Anchor Positioning Level 1 section 6 (plan item P10-08, https://www.w3.org/TR/css-anchor-position-1/, Last-Modified 2026-03-27):
 * <code>position-try-fallbacks</code> with the tactics <code>flip-block</code>, <code>flip-inline</code>, <code>flip-start</code>, <code>flip-x</code>, <code>flip-y</code>,
 * with <code>@position-try</code> rules and with position areas; <code>position-try-order</code> and the <code>position-try</code> shorthand; <code>position-visibility</code>
 * (6.6); the scope of anchor names of <code>anchor-scope</code> (2.2) and <code>anchor-center</code> (4.2). The container is 300 by 200; the box is 40 by 20 and the anchor
 * is at a place that the test names (the anchor is 100 by 30 at (50, 40) unless said otherwise).
 * <p>
 * CSS Anchor Positioning Level 1 6 절 넘침 관리의 시험(계획 P10-08): 전술 <code>flip-block</code>, <code>flip-inline</code>, <code>flip-start</code>, <code>flip-x</code>,
 * <code>flip-y</code>, <code>@position-try</code> 규칙, 위치 영역을 쓴 <code>position-try-fallbacks</code>. <code>position-try-order</code> 와 <code>position-try</code> 단축 속성,
 * <code>position-visibility</code>(6.6), <code>anchor-scope</code> 의 앵커 이름 범위(2.2), <code>anchor-center</code>(4.2). 컨테이너는 300x200 이고 상자는 40x20 이며 앵커의 자리는
 * 시험이 정한다(달리 말하지 않으면 (50, 40) 의 100x30).
 */
public class PositionTryLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PositionTryLayoutTest() {
    }

    /**
     * Lays out the container with a positioned box and the anchor.
     * <p>
     * 배치 상자와 앵커가 든 컨테이너를 배치한다.
     *
     * @param head    markup before the container (style sheets) / 컨테이너 앞의 마크업(스타일 시트)
     * @param spacer  the height of the spacer above the anchor: the y of the anchor / 앵커 위 간격 상자의 높이: 앵커의 y
     * @param left    the margin to the left of the anchor: the x of the anchor / 앵커 왼쪽 마진: 앵커의 x
     * @param width   the width of the anchor / 앵커의 너비
     * @param height  the height of the anchor / 앵커의 높이
     * @param popStyle the style that is added to the box / 상자에 더하는 스타일
     * @return the layout / 레이아웃
     */
    private static LayoutResult page(String head, int spacer, int left, int width, int height, String popStyle) {
        return LayoutTestSupport.layout(head + "<div id=wrap style='position:relative;width:300px;height:200px;margin:0'><div style='height:" + spacer + "px'></div>"
                + "<div id=pop style='position:absolute;position-anchor:--a;width:40px;height:20px;" + popStyle + "'></div>"
                + "<div id=anc style='anchor-name:--a;margin-left:" + left + "px;width:" + width + "px;height:" + height + "px'></div></div>", 600);
    }

    /**
     * The x of the box relative to the container.
     * <p>
     * 컨테이너에 대한 상자의 x.
     *
     * @param r the layout / 레이아웃
     * @return x / x
     */
    private static double x(LayoutResult r) {
        return LayoutTestSupport.box(r, "pop").getX() - LayoutTestSupport.box(r, "wrap").getX();
    }

    /**
     * The y of the box relative to the container.
     * <p>
     * 컨테이너에 대한 상자의 y.
     *
     * @param r the layout / 레이아웃
     * @return y / y
     */
    private static double y(LayoutResult r) {
        return LayoutTestSupport.box(r, "pop").getY() - LayoutTestSupport.box(r, "wrap").getY();
    }

    /**
     * <code>flip-block</code>: the anchor is near the bottom, the area <code>bottom</code> is 20 px tall and the box (30 px tall) overflows it, so it flips to
     * <code>top</code> (150 px tall) and ends at the top edge of the anchor. Without the fallback the box is only shifted into the container.
     * <p>
     * <code>flip-block</code>: 앵커가 아래쪽에 있어 <code>bottom</code> 영역은 20px 이고 상자(30px)가 넘치므로 <code>top</code>(150px)으로 뒤집혀 앵커의 위쪽 가장자리에서 끝난다.
     * 대안이 없으면 상자는 컨테이너 안으로 밀릴 뿐이다.
     */
    public void testFlipBlock() {
        LayoutResult plain = page("", 150, 50, 100, 30, "height:30px;position-area:bottom");
        Check.near(170, y(plain), 0.01, "without a fallback the box overflows its area and is shifted back into the container (200 - 30)");
        LayoutResult flipped = page("", 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:flip-block");
        Check.near(120, y(flipped), 0.01, "flip-block: the area top, the bottom edge of the box on the top edge of the anchor (150 - 30)");
        Check.near(80, x(flipped), 0.01, "centered on the anchor horizontally");
        LayoutResult fits = page("", 40, 50, 100, 30, "position-area:bottom;position-try-fallbacks:flip-block");
        Check.near(70, y(fits), 0.01, "a box that fits keeps its first position");
    }

    /**
     * <code>flip-inline</code>, <code>flip-x</code> and the pair of tactics <code>flip-x flip-y</code> in the horizontal writing mode.
     * <p>
     * 가로쓰기에서 <code>flip-inline</code>, <code>flip-x</code>, 전술 한 쌍 <code>flip-x flip-y</code>.
     */
    public void testFlipInlineAndXY() {
        LayoutResult plain = page("", 40, 250, 40, 30, "position-area:right");
        Check.near(260, x(plain), 0.01, "the column right of the anchor is 10 px wide; the box is shifted back (300 - 40)");
        LayoutResult inline = page("", 40, 250, 40, 30, "position-area:right;position-try-fallbacks:flip-inline");
        Check.near(210, x(inline), 0.01, "flip-inline: the area left, the right edge of the box on the left edge of the anchor (250 - 40)");
        Check.near(45, y(inline), 0.01, "centered on the anchor vertically");
        LayoutResult flipX = page("", 40, 250, 40, 30, "position-area:right;position-try-fallbacks:flip-x");
        Check.near(210, x(flipX), 0.01, "flip-x does the same in the horizontal writing mode");
        LayoutResult both = page("", 10, 20, 100, 30, "position-area:top left;position-try-fallbacks:flip-x flip-y");
        Check.near(120, x(both), 0.01, "flip-x flip-y: the area bottom right, the box starts at the right edge of the anchor");
        Check.near(40, y(both), 0.01, "and at its bottom edge");
        LayoutResult order = page("", 10, 20, 100, 30, "position-area:top left;position-try-fallbacks:flip-y flip-x");
        Check.near(120, x(order), 0.01, "the order of two flips of different axes does not matter: x");
        Check.near(40, y(order), 0.01, "y");
    }

    /**
     * <code>flip-start</code> exchanges the block-start and the inline-start sides: the area <code>top</code> becomes <code>left</code>.
     * <p>
     * <code>flip-start</code> 는 블록 시작 변과 인라인 시작 변을 맞바꾼다: 영역 <code>top</code> 은 <code>left</code> 가 된다.
     */
    public void testFlipStart() {
        LayoutResult plain = page("", 10, 50, 100, 30, "position-area:top");
        Check.near(0, y(plain), 0.01, "the row above the anchor is 10 px tall, the box is shifted down into the container");
        LayoutResult r = page("", 10, 50, 100, 30, "position-area:top;position-try-fallbacks:flip-start");
        Check.near(30, x(r), 0.01, "flip-start: the area left (50 px wide); the width and the height are exchanged (20 by 40), the box ends at the left edge of the anchor (50 - 20)");
        Check.near(5, y(r), 0.01, "centered on the anchor vertically (center 25 - 20)");
        Check.near(20, LayoutTestSupport.box(r, "pop").getWidth(), 0.01, "the width is the old height");
        Check.near(40, LayoutTestSupport.box(r, "pop").getHeight(), 0.01, "the height is the old width");
        LayoutResult flipped = page("", 10, 50, 100, 30, "position-area:top;position-try-fallbacks:flip-start flip-inline");
        Check.near(150, x(flipped), 0.01, "flip-start flip-inline: the area right, the box starts at the right edge of the anchor");
        LayoutResult sized = page("", 10, 50, 100, 30, "position-area:top;width:anchor-size(width);height:20px;position-try-fallbacks:flip-start");
        Check.near(20, LayoutTestSupport.box(sized, "pop").getWidth(), 0.01, "the height 20px moved to the width");
        Check.near(30, LayoutTestSupport.box(sized, "pop").getHeight(), 0.01, "width: anchor-size(width) moved to the height and became anchor-size(height), the height of the anchor");
    }

    /**
     * Tactics on boxes that are positioned with insets and <code>anchor()</code> (no position area): <code>top: anchor(bottom)</code> becomes
     * <code>bottom: anchor(top)</code> for <code>flip-block</code>, the margins are exchanged, a <code>calc()</code> with an anchor function is rewritten too.
     * <p>
     * 오프셋과 <code>anchor()</code> 로 배치하는 상자(위치 영역 없음)에 대한 전술: <code>flip-block</code> 으로 <code>top: anchor(bottom)</code> 은 <code>bottom: anchor(top)</code> 이 되고,
     * 마진은 맞바뀌며, anchor 함수가 든 <code>calc()</code> 도 고쳐 쓴다.
     */
    public void testFlipInsetsWithAnchorFunctions() {
        LayoutResult plain = page("", 150, 50, 100, 30, "height:30px;top:anchor(bottom);left:anchor(left)");
        Check.near(180, y(plain), 0.01, "without a fallback the box hangs below the anchor and leaves the container");
        LayoutResult block = page("", 150, 50, 100, 30, "height:30px;top:anchor(bottom);left:anchor(left);position-try-fallbacks:flip-block");
        Check.near(120, y(block), 0.01, "flip-block: bottom: anchor(top), the bottom edge of the box on the top edge of the anchor");
        Check.near(50, x(block), 0.01, "left stays");
        LayoutResult margin = page("", 150, 50, 100, 30, "height:30px;top:anchor(bottom);left:anchor(left);margin-top:7px;position-try-fallbacks:flip-block");
        Check.near(113, y(margin), 0.01, "margin-top: 7px became margin-bottom: 7px (200 - 50 - 7 - 30)");
        LayoutResult calc = page("", 150, 50, 100, 30, "height:30px;top:calc(anchor(bottom) + 5px);left:anchor(left);position-try-fallbacks:flip-block");
        Check.near(115, y(calc), 0.01, "the anchor function inside calc() is rewritten: bottom: calc(anchor(top) + 5px)");
        LayoutResult inline = page("", 40, 250, 40, 30, "top:anchor(top);left:anchor(right);position-try-fallbacks:flip-inline");
        Check.near(210, x(inline), 0.01, "flip-inline: right: anchor(left), the right edge of the box on the left edge of the anchor");
        LayoutResult low = page("", 170, 50, 100, 30, "height:30px;top:anchor(20%);left:anchor(left)");
        Check.near(176, y(low), 0.01, "top: anchor(20%) of the anchor 170 to 200 is 176, the box leaves the container");
        LayoutResult percent = page("", 170, 50, 100, 30, "height:30px;top:anchor(20%);left:anchor(left);position-try-fallbacks:flip-block");
        Check.near(164, y(percent), 0.01, "a percentage becomes 100% minus itself: bottom: anchor(80%) = 194, the box ends there (194 - 30)");
    }

    /**
     * The permutations of the sides that the tactics make for the writing modes and the directions, and the mirroring of the tracks of a position area.
     * <p>
     * 쓰기 모드와 방향에 따라 전술이 만드는 변의 순열, 그리고 위치 영역의 트랙 뒤집기.
     */
    public void testTacticPermutations() {
        Check.equal("flip-y", PositionTry.physical("flip-block", "horizontal-tb"), "flip-block in a horizontal mode");
        Check.equal("flip-x", PositionTry.physical("flip-block", "vertical-rl"), "flip-block in a vertical mode");
        Check.equal("flip-x", PositionTry.physical("flip-inline", "horizontal-tb"), "flip-inline in a horizontal mode");
        Check.equal("flip-y", PositionTry.physical("flip-inline", "vertical-lr"), "flip-inline in a vertical mode");
        Check.equal("flip-start", PositionTry.physical("flip-start", "vertical-lr"), "flip-start stays");
        Check.equal(new int[] {2, 1, 0, 3}, PositionTry.permutation("flip-y", "horizontal-tb", "ltr"), "flip-y exchanges top and bottom");
        Check.equal(new int[] {0, 3, 2, 1}, PositionTry.permutation("flip-x", "horizontal-tb", "ltr"), "flip-x exchanges left and right");
        Check.equal(new int[] {3, 2, 1, 0}, PositionTry.permutation("flip-start", "horizontal-tb", "ltr"), "flip-start: top with left, bottom with right");
        Check.equal(new int[] {1, 0, 3, 2}, PositionTry.permutation("flip-start", "horizontal-tb", "rtl"), "flip-start in rtl: top with right, bottom with left");
        Check.equal(new int[] {1, 0, 3, 2}, PositionTry.permutation("flip-start", "vertical-rl", "ltr"), "flip-start in vertical-rl: right (block-start) with top (inline-start)");
        Check.equal(new int[] {3, 2, 1, 0}, PositionTry.permutation("flip-start", "vertical-lr", "ltr"), "flip-start in vertical-lr: left with top");
        Check.equal(new int[] {1, 0, 3, 2}, PositionTry.permutation("flip-start", "sideways-lr", "ltr"), "flip-start in sideways-lr: left with bottom");
        Check.isNull(PositionTry.permutation("flip-diagonal", "horizontal-tb", "ltr"), "an unknown tactic");
        int[] flipY = {2, 1, 0, 3};
        int[] flipX = {0, 3, 2, 1};
        int[] start = {3, 2, 1, 0};
        Check.equal("bottom span-all", mirrored("top", flipY), "flip-y: top becomes bottom");
        Check.equal("span-all right", mirrored("left", flipX), "flip-x: left becomes right");
        Check.equal("top span-right", mirrored("top span-left", flipX), "flip-x: span-left becomes span-right");
        Check.equal("center span-left", mirrored("center span-right", flipX), "flip-x: the middle track stays, span-right becomes span-left");
        Check.equal("top right", mirrored("bottom left", start), "flip-start: the transposed corner");
        Check.equal("top left", mirrored("top left", start), "flip-start: the corner on the diagonal stays");
        Check.equal("span-all right", mirrored("top", new int[] {1, 0, 3, 2}), "flip-start in rtl: top becomes the right column");
        Check.equal("none", mirrored("none", flipY), "none stays");
    }

    /**
     * Mirrors the <code>position-area</code> value of a text and returns the text of the result.
     * <p>
     * 텍스트의 <code>position-area</code> 값을 뒤집어 결과의 텍스트를 반환한다.
     *
     * @param text the value / 값
     * @param perm the permutation of the sides / 변의 순열
     * @return the text / 텍스트
     */
    private static String mirrored(String text, int[] perm) {
        org.duckdns.hjow.swingwebpanel.css.values.CssValue v = org.duckdns.hjow.swingwebpanel.css.cssom.CssStyleDeclaration.parse("position-area:" + text, null, new org.duckdns.hjow.swingwebpanel.css.CssDiagnostics())
                .normal().get(Integer.valueOf(org.duckdns.hjow.swingwebpanel.css.props.Prop.POSITION_AREA));
        return PositionTry.flipArea(v, perm).toCssText();
    }

    /**
     * A named <code>@position-try</code> rule: its <code>position-area</code> replaces the one of the box; a rule with insets; a later rule of the same name wins;
     * a missing name has no effect.
     * <p>
     * 이름 있는 <code>@position-try</code> 규칙: 규칙의 <code>position-area</code> 가 상자의 것을 대체한다. 오프셋이 있는 규칙. 같은 이름의 나중 규칙이 이긴다. 없는 이름은 효과가 없다.
     */
    public void testNamedOptions() {
        String head = "<style>@position-try --right { position-area: right }</style>";
        LayoutResult r = page(head, 40, 20, 100, 30, "position-area:left;position-try-fallbacks:--right");
        Check.near(120, x(r), 0.01, "the rule: the area right of the anchor");
        Check.near(45, y(r), 0.01, "centered on the anchor");
        LayoutResult plain = page(head, 40, 20, 100, 30, "position-area:left");
        Check.near(0, x(plain), 0.01, "without the fallback: the box is shifted into the container");
        String manual = "<style>@position-try --manual { position-area: none; top: anchor(bottom); left: anchor(left) }</style>";
        LayoutResult m = page(manual, 10, 50, 100, 30, "position-area:top;position-try-fallbacks:--manual");
        Check.near(50, x(m), 0.01, "a rule with insets: left on the left edge of the anchor");
        Check.near(40, y(m), 0.01, "top on the bottom edge of the anchor");
        String later = "<style>@position-try --o { position-area: left } @position-try --o { position-area: right }</style>";
        LayoutResult l = page(later, 40, 20, 100, 30, "position-area:left;position-try-fallbacks:--o");
        Check.near(120, x(l), 0.01, "the later rule of the name wins (the rules cascade like keyframes)");
        LayoutResult missing = page("", 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:--missing, flip-block");
        Check.near(120, y(missing), 0.01, "a name without a rule has no effect, the next entry is used");
        String withTactic = "<style>@position-try --bt { position-area: bottom span-right }</style>";
        LayoutResult t = page(withTactic, 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:--bt flip-block");
        Check.near(120, y(t), 0.01, "a name together with a tactic: the rule is applied, then mirrored (bottom becomes top)");
        Check.near(50, x(t), 0.01, "span-right stays: the box starts at the left edge of the anchor");
        String margin = "<style>@position-try --mg { margin-left: 7px }</style>";
        LayoutResult g = page(margin, 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:--mg flip-block");
        Check.near(120, y(g), 0.01, "the area of the base style is kept and mirrored");
        Check.near(83.5, x(g), 0.01, "the margin of the rule is kept (flip-block exchanges only the top and the bottom margins): centered on the anchor with the margin (100 - 47 / 2 + 7)");
    }

    /**
     * An entry that is a position area sets only that property; the first option that fits wins and none that fits keeps the base style.
     * <p>
     * 위치 영역인 항목은 그 속성만 정한다. 맞는 첫 대안이 이기고 맞는 것이 없으면 기본 스타일을 유지한다.
     */
    public void testAreaOptionsAndNoFit() {
        LayoutResult r = page("", 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:bottom left, top");
        Check.near(120, y(r), 0.01, "bottom left overflows (20 px), top fits");
        LayoutResult wide = page("", 40, 50, 100, 30, "width:400px;position-area:left;position-try-fallbacks:flip-inline");
        LayoutResult wideBase = page("", 40, 50, 100, 30, "width:400px;position-area:left");
        Check.near(x(wideBase), x(wide), 0.01, "no option fits (the box is wider than the container): the base style stays");
        Check.near(y(wideBase), y(wide), 0.01, "also vertically");
        LayoutResult first = page("", 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:top, flip-block");
        Check.near(120, y(first), 0.01, "the first option that fits wins");
    }

    /**
     * <code>position-try-order</code>: the options (the base style first) are sorted by the size of their inset-modified containing block, the largest first, with a
     * stable sort; <code>most-width</code> prefers the options that span the container, <code>most-height</code> the tall ones; the <code>position-try</code> shorthand
     * sets the order and the fallbacks together.
     * <p>
     * <code>position-try-order</code>: 대안(기본 스타일이 처음)을 inset-modified containing block 의 크기로 큰 것부터 안정 정렬한다. <code>most-width</code> 는 컨테이너 전체에 걸친 대안을,
     * <code>most-height</code> 는 키가 큰 대안을 선호한다. <code>position-try</code> 단축 속성은 순서와 대안을 함께 정한다.
     */
    public void testPositionTryOrder() {
        String pop = "height:70px;position-area:top;";
        LayoutResult normal = page("", 60, 50, 100, 30, pop + "position-try-fallbacks:left, flip-block");
        Check.near(10, x(normal), 0.01, "normal order: left fits first (the box ends at the left edge of the anchor)");
        Check.near(40, y(normal), 0.01, "centered on the anchor vertically (center 75 - 35)");
        LayoutResult width = page("", 60, 50, 100, 30, pop + "position-try-fallbacks:left, flip-block;position-try-order:most-width");
        Check.near(80, x(width), 0.01, "most-width: the bottom row (300 wide) comes before left (50 wide)");
        Check.near(90, y(width), 0.01, "the row below the anchor");
        LayoutResult height = page("", 60, 50, 100, 30, pop + "position-try-fallbacks:left, flip-block;position-try-order:most-height");
        Check.near(10, x(height), 0.01, "most-height: left (200 tall) comes before the bottom row (110 tall)");
        LayoutResult block = page("", 60, 50, 100, 30, pop + "position-try-fallbacks:left, flip-block;position-try-order:most-inline-size");
        Check.near(80, x(block), 0.01, "most-inline-size is the width in the horizontal writing mode");
        LayoutResult sh = page("", 60, 50, 100, 30, pop + "position-try:most-width left, flip-block");
        Check.near(80, x(sh), 0.01, "the shorthand sets the order");
        Check.near(90, y(sh), 0.01, "and the fallbacks");
        LayoutResult bsz = page("", 60, 50, 100, 30, pop + "position-try:most-block-size left, flip-block");
        Check.near(10, x(bsz), 0.01, "most-block-size is the height in the horizontal writing mode");
    }

    /**
     * <code>position-visibility</code>: <code>no-overflow</code> hides a box that still overflows after the fallbacks; <code>always</code> shows it; a box that
     * fits is shown; the box and its descendants are hidden (the computed visibility is forced).
     * <p>
     * <code>position-visibility</code>: <code>no-overflow</code> 는 대안을 써도 넘치는 상자를 숨기고 <code>always</code> 는 보인다. 맞는 상자는 보인다. 상자와 자손은 모두 숨겨진다
     * (계산된 visibility 가 강제된다).
     */
    public void testPositionVisibilityNoOverflow() {
        String wide = "width:400px;position-area:left;";
        LayoutResult hidden = page("", 40, 50, 100, 30, wide + "position-visibility:no-overflow");
        Check.isTrue(LayoutTestSupport.box(hidden, "pop").positionHidden, "no-overflow: hidden");
        Check.isTrue(LayoutTestSupport.box(hidden, "pop").style.isKeyword(org.duckdns.hjow.swingwebpanel.css.props.Prop.VISIBILITY, "hidden"), "its visibility is hidden");
        LayoutResult shown = page("", 40, 50, 100, 30, wide + "position-visibility:always");
        Check.isFalse(LayoutTestSupport.box(shown, "pop").positionHidden, "always: shown");
        LayoutResult fits = page("", 40, 50, 100, 30, "position-area:bottom;position-visibility:no-overflow");
        Check.isFalse(LayoutTestSupport.box(fits, "pop").positionHidden, "a box that fits is shown");
        LayoutResult rescued = page("", 150, 50, 100, 30, "height:30px;position-area:bottom;position-try-fallbacks:flip-block;position-visibility:no-overflow");
        Check.isFalse(LayoutTestSupport.box(rescued, "pop").positionHidden, "a fallback that fits keeps the box shown");
        LayoutResult child = LayoutTestSupport.layout("<div id=wrap style='position:relative;width:300px;height:200px'>"
                + "<div id=pop style='position:absolute;width:400px;height:20px;left:0;position-visibility:no-overflow;position-area:left;position-anchor:--a'>"
                + "<span id=kid style='visibility:visible'>x</span></div><div style='anchor-name:--a;margin:40px 0 0 50px;width:100px;height:30px'></div></div>", 600);
        Check.isTrue(LayoutTestSupport.box(child, "kid").style.isKeyword(org.duckdns.hjow.swingwebpanel.css.props.Prop.VISIBILITY, "hidden"), "a descendant cannot make itself visible");
    }

    /**
     * <code>position-visibility: anchors-visible</code> (the initial value) hides a box whose default anchor is completely clipped by an intervening box that clips its
     * overflow; <code>always</code> and an anchor that is inside the clip do not; <code>anchors-valid</code> hides a box with an anchor function that has no anchor
     * and no fallback.
     * <p>
     * <code>position-visibility: anchors-visible</code>(초기값)은 넘침을 자르는 중간 상자가 기본 앵커를 완전히 잘라 내면 상자를 숨긴다. <code>always</code> 와 자르기 안에 있는 앵커는
     * 그렇지 않다. <code>anchors-valid</code> 는 앵커도 대체값도 없는 anchor 함수가 있는 상자를 숨긴다.
     */
    public void testPositionVisibilityAnchors() {
        String clipped = "<div id=wrap style='position:relative;width:300px;height:200px'><div style='overflow:hidden;width:100px;height:50px'>"
                + "<div id=anc style='anchor-name:--a;margin-left:150px;width:40px;height:20px'></div></div>"
                + "<div id=pop style='position:absolute;width:40px;height:20px;position-anchor:--a;top:anchor(bottom);left:0;";
        LayoutResult hidden = LayoutTestSupport.layout(clipped + "'></div></div>", 600);
        Check.isTrue(LayoutTestSupport.box(hidden, "pop").positionHidden, "anchors-visible: the anchor is outside of the clip rectangle, the box is hidden");
        LayoutResult always = LayoutTestSupport.layout(clipped + "position-visibility:always'></div></div>", 600);
        Check.isFalse(LayoutTestSupport.box(always, "pop").positionHidden, "always: shown");
        LayoutResult inside = LayoutTestSupport.layout("<div id=wrap style='position:relative;width:300px;height:200px'><div style='overflow:hidden;width:100px;height:50px'>"
                + "<div id=anc style='anchor-name:--a;margin-left:50px;width:40px;height:20px'></div></div>"
                + "<div id=pop style='position:absolute;width:40px;height:20px;position-anchor:--a;top:anchor(bottom);left:0'></div></div>", 600);
        Check.isFalse(LayoutTestSupport.box(inside, "pop").positionHidden, "an anchor inside of the clip: shown");
        LayoutResult same = LayoutTestSupport.layout("<div id=wrap style='position:relative;overflow:hidden;width:100px;height:50px'>"
                + "<div id=anc style='anchor-name:--a;margin-left:150px;width:40px;height:20px'></div>"
                + "<div id=pop style='position:absolute;width:40px;height:20px;position-anchor:--a;top:anchor(bottom);left:0'></div></div>", 600);
        Check.isFalse(LayoutTestSupport.box(same, "pop").positionHidden, "the anchor is clipped by the containing block itself, which clips the box too: shown");
        LayoutResult none = LayoutTestSupport.layout("<div id=wrap style='position:relative;width:300px;height:200px'>"
                + "<div id=pop style='position:absolute;width:40px;height:20px;top:0;left:0'></div></div>", 600);
        Check.isFalse(LayoutTestSupport.box(none, "pop").positionHidden, "a box without anchors is shown");
        String valid = "<div id=wrap style='position:relative;width:300px;height:200px'><div id=pop style='position:absolute;width:40px;height:20px;top:0;position-visibility:anchors-valid;";
        LayoutResult missing = LayoutTestSupport.layout(valid + "left:anchor(--nope right)'></div></div>", 600);
        Check.isTrue(LayoutTestSupport.box(missing, "pop").positionHidden, "anchors-valid: an anchor() without an anchor and without a fallback hides the box");
        LayoutResult fallback = LayoutTestSupport.layout(valid + "left:anchor(--nope right, 5px)'></div></div>", 600);
        Check.isFalse(LayoutTestSupport.box(fallback, "pop").positionHidden, "with a fallback value the reference is not required");
        LayoutResult present = LayoutTestSupport.layout(valid + "left:anchor(--a right)'></div><div style='anchor-name:--a;width:50px;height:10px'></div></div>", 600);
        Check.isFalse(LayoutTestSupport.box(present, "pop").positionHidden, "an anchor that exists: shown");
        Check.near(50, LayoutTestSupport.box(present, "pop").getX() - LayoutTestSupport.box(present, "wrap").getX(), 0.01, "and used");
    }

    /**
     * <code>anchor-scope</code>: a name that an element limits is visible only inside the subtree of that element, in both directions (a positioned box inside the
     * subtree sees only the anchors of the subtree, a positioned box outside of it does not see the anchor in it); <code>all</code> limits every name.
     * <p>
     * <code>anchor-scope</code>: 요소가 제한한 이름은 그 요소의 서브트리 안에서만 보인다. 양방향이다(서브트리 안의 배치 상자는 서브트리의 앵커만 보고, 밖의 배치 상자는 안의 앵커를 보지 못한다).
     * <code>all</code> 은 모든 이름을 제한한다.
     */
    public void testAnchorScope() {
        // 두 컨테이너가 같은 이름의 앵커를 가진다. 범위가 없으면 첫 컨테이너의 배치 상자도 마지막 앵커(둘째 컨테이너)를 쓴다.
        String unscoped = scopeHtml("", "");
        Check.near(70, y(LayoutTestSupport.layout(unscoped, 600), "p1"), 0.01, "without anchor-scope the last anchor of the tree order is used (70)");
        String scoped = scopeHtml("anchor-scope:--a", "anchor-scope:--a");
        LayoutResult r = LayoutTestSupport.layout(scoped, 600);
        Check.near(10, y(r, "p1"), 0.01, "with anchor-scope the box sees the anchor of its own container (10)");
        Check.near(99, y(r, "p2"), 0.01, "a box outside of the scopes does not see the scoped anchors: the fallback value");
        LayoutResult all = LayoutTestSupport.layout(scopeHtml("anchor-scope:all", "anchor-scope:all"), 600);
        Check.near(10, y(all, "p1"), 0.01, "all: every name is limited");
        Check.near(99, y(all, "p2"), 0.01, "all: the box outside does not see the anchors");
        LayoutResult other = LayoutTestSupport.layout(scopeHtml("anchor-scope:--b", "anchor-scope:--b"), 600);
        Check.near(70, y(other, "p1"), 0.01, "a scope for another name changes nothing");
    }

    /**
     * Builds two containers with an anchor of the same name each and a positioned box in the first one and one outside of both.
     * <p>
     * 같은 이름의 앵커를 가진 컨테이너 둘과 첫 컨테이너 안의 배치 상자, 둘 밖의 배치 상자를 만든다.
     *
     * @param s1 the style of the first container / 첫 컨테이너의 스타일
     * @param s2 the style of the second container / 둘째 컨테이너의 스타일
     * @return the markup / 마크업
     */
    private static String scopeHtml(String s1, String s2) {
        return "<div id=wrap style='position:relative;width:300px;height:300px'>"
                + "<div style='height:50px;padding-top:10px;" + s1 + "'><div style='anchor-name:--a;height:20px'></div>"
                + "<div id=p1 style='position:absolute;position-anchor:--a;top:anchor(top);left:0;width:10px;height:10px'></div></div>"
                + "<div style='height:50px;padding-top:10px;" + s2 + "'><div style='anchor-name:--a;height:20px'></div></div>"
                + "<div id=p2 style='position:absolute;position-anchor:--a;top:anchor(top, 99px);left:0;width:10px;height:10px'></div></div>";
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
     * <code>anchor-center</code>: the box is centered on the default anchor in the axis of the property; the other axis keeps its normal layout; together with a position
     * area it replaces the alignment of the area in that axis; a flex item takes it as <code>center</code>.
     * <p>
     * <code>anchor-center</code>: 상자는 속성의 축에서 기본 앵커의 가운데에 놓이고 다른 축은 평소대로 배치한다. 위치 영역과 함께 쓰면 그 축의 영역 정렬을 바꾼다. 플렉스 항목은 <code>center</code> 로 본다.
     */
    public void testAnchorCenter() {
        LayoutResult both = page("", 40, 50, 100, 30, "justify-self:anchor-center;align-self:anchor-center");
        Check.near(80, x(both), 0.01, "centered on the anchor horizontally (center 100 - 20)");
        Check.near(45, y(both), 0.01, "and vertically (center 55 - 10)");
        LayoutResult inline = page("", 40, 50, 100, 30, "justify-self:anchor-center;top:5px");
        Check.near(80, x(inline), 0.01, "only the inline axis: centered");
        Check.near(5, y(inline), 0.01, "the other axis keeps the offset");
        LayoutResult place = page("", 40, 50, 100, 30, "place-self:anchor-center");
        Check.near(80, x(place), 0.01, "place-self sets both axes: x");
        Check.near(45, y(place), 0.01, "y");
        LayoutResult area = page("", 40, 50, 100, 30, "position-area:top left;justify-self:anchor-center");
        Check.near(80, x(area), 0.01, "with a position area the alignment of the axis is replaced: the area is the left column (50 wide) but the box is centered on the anchor");
        Check.near(20, y(area), 0.01, "the other axis keeps the alignment of the area (end of the row above the anchor)");
        LayoutResult noAnchor = LayoutTestSupport.layout("<div id=wrap style='position:relative;width:300px;height:200px'><div id=pop style='position:absolute;width:40px;height:20px;"
                + "left:7px;top:9px;justify-self:anchor-center;align-self:anchor-center'></div></div>", 600);
        Check.near(7, LayoutTestSupport.box(noAnchor, "pop").getX() - LayoutTestSupport.box(noAnchor, "wrap").getX(), 0.01, "without an anchor the insets decide: x");
        Check.near(9, LayoutTestSupport.box(noAnchor, "pop").getY() - LayoutTestSupport.box(noAnchor, "wrap").getY(), 0.01, "y");
        LayoutResult flex = LayoutTestSupport.layout("<div id=wrap style='display:flex;height:100px;width:300px;align-items:flex-start'>"
                + "<div id=item style='height:20px;width:30px;align-self:anchor-center'></div></div>", 600);
        Check.near(40, LayoutTestSupport.box(flex, "item").getY() - LayoutTestSupport.box(flex, "wrap").getY(), 0.01, "a flex item with anchor-center is centered");
    }
}
