package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of container queries and containment (plan item P10-08, CSS Conditional Level 5 "Container Queries" at https://www.w3.org/TR/css-conditional-5/,
 * CSS Containment Level 3 and Level 2): <code>@container</code> evaluated against the content box of the nearest container, the container name, the
 * <code>container</code> shorthand, size features in the plain, range and logical forms, the operators, the container types and the axes they contain,
 * <code>style()</code> queries, the layout that settles in a second pass, and the effects of size containment, layout containment, paint containment and
 * <code>content-visibility</code> on the boxes.
 * <p>
 * 컨테이너 질의와 격리 시험(계획 P10-08, CSS Conditional Level 5 "Container Queries" https://www.w3.org/TR/css-conditional-5/, CSS Containment Level 3, Level 2):
 * 가장 가까운 컨테이너의 content box 로 평가하는 <code>@container</code>, 컨테이너 이름, <code>container</code> 단축 속성, 일반/범위/논리 형태의 크기 특성, 연산자,
 * 컨테이너 종류와 그것이 격리하는 축, <code>style()</code> 질의, 두 번째 배치에서 안정되는 레이아웃, 크기 격리, 레이아웃 격리, 그리기 격리,
 * <code>content-visibility</code> 가 상자에 주는 효과.
 */
public class ContainerQueryLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ContainerQueryLayoutTest() {
    }

    /**
     * Returns the width of the element <code>i</code> of a page.
     * <p>
     * 페이지의 요소 <code>i</code> 의 너비를 반환한다.
     *
     * @param css   the style rules / 스타일 규칙
     * @param body  the markup / 마크업
     * @param width the viewport width / 뷰포트 너비
     * @return the border box width / border box 너비
     */
    private static double widthOfI(String css, String body, double width) {
        LayoutResult r = LayoutTestSupport.layout("<style>" + css + "</style>" + body, width);
        return LayoutTestSupport.box(r, "i").getWidth();
    }

    /**
     * A rule inside <code>@container</code> applies when the content box of the container satisfies the condition; the container is measured in
     * the layout and the styles are computed again.
     * <p>
     * <code>@container</code> 안의 규칙은 컨테이너의 content box 가 조건을 만족할 때 적용된다. 컨테이너를 레이아웃에서 재고 스타일을 다시 계산한다.
     */
    public void testMinWidthOfTheContainer() {
        String css = ".wrap { container-type: inline-size; width: 300px } .item { width: 50px; height: 10px }"
                + " @container (min-width: 200px) { .item { width: 100px } } @container (min-width: 400px) { .item { width: 150px } }";
        String body = "<div class=wrap><div class=item id=i></div></div>";
        Check.near(100.0, widthOfI(css, body, 600), 0.01, "300px is at least 200px but less than 400px");
        Check.near(50.0, widthOfI(css.replace("width: 300px", "width: 150px"), body, 600), 0.01, "a container of 150px matches neither query");
        Check.near(150.0, widthOfI(css.replace("width: 300px", "width: 500px"), body, 600), 0.01, "a container of 500px matches both, the later rule wins");
    }

    /**
     * The size of the container is its content box: padding and border are not part of it; a block container fills the viewport, so the layout
     * follows the width of the viewport.
     * <p>
     * 컨테이너의 크기는 content box 이다: padding 과 border 는 포함하지 않는다. 블록 컨테이너는 뷰포트를 채우므로 레이아웃은 뷰포트 너비를 따른다.
     */
    public void testContentBoxAndViewport() {
        String css = ".wrap { container-type: inline-size; padding: 0 30px } .item { width: 10px; height: 10px } @container (min-width: 250px) { .item { width: 99px } }";
        String body = "<div class=wrap><div class=item id=i></div></div>";
        Check.near(99.0, widthOfI(css, body, 320), 0.01, "320 - 60 = 260 is at least 250");
        Check.near(10.0, widthOfI(css, body, 300), 0.01, "300 - 60 = 240 is less than 250");
    }

    /**
     * Range syntax, <code>max-</code>, the logical <code>inline-size</code> and the operators <code>not</code>, <code>and</code> and <code>or</code>.
     * <p>
     * 범위 문법, <code>max-</code>, 논리 <code>inline-size</code>, 연산자 <code>not</code>, <code>and</code>, <code>or</code>.
     */
    public void testConditionSyntax() {
        String base = ".wrap { container-type: inline-size; width: 300px } .item { width: 10px; height: 10px } ";
        String body = "<div class=wrap><div class=item id=i></div></div>";
        Check.near(70.0, widthOfI(base + "@container (width > 250px) { .item { width: 70px } }", body, 600), 0.01, "width > 250px");
        Check.near(10.0, widthOfI(base + "@container (width > 350px) { .item { width: 70px } }", body, 600), 0.01, "width > 350px fails");
        Check.near(70.0, widthOfI(base + "@container (200px <= width <= 400px) { .item { width: 70px } }", body, 600), 0.01, "a range with two bounds");
        Check.near(70.0, widthOfI(base + "@container (max-width: 300px) { .item { width: 70px } }", body, 600), 0.01, "max-width is inclusive");
        Check.near(70.0, widthOfI(base + "@container (inline-size >= 300px) { .item { width: 70px } }", body, 600), 0.01, "the logical inline-size");
        Check.near(70.0, widthOfI(base + "@container not (min-width: 400px) { .item { width: 70px } }", body, 600), 0.01, "not");
        Check.near(70.0, widthOfI(base + "@container (min-width: 100px) and (max-width: 400px) { .item { width: 70px } }", body, 600), 0.01, "and");
        Check.near(10.0, widthOfI(base + "@container (min-width: 100px) and (max-width: 200px) { .item { width: 70px } }", body, 600), 0.01, "and fails");
        Check.near(70.0, widthOfI(base + "@container (min-width: 900px) or (max-width: 400px) { .item { width: 70px } }", body, 600), 0.01, "or");
        Check.near(70.0, widthOfI(base + "@container ((min-width: 900px) or (max-width: 400px)) and (min-width: 1px) { .item { width: 70px } }", body, 600), 0.01,
                "a nested condition");
        Check.near(10.0, widthOfI(base + "@container (min-width: 100px) and (max-width: 400px) or (min-width: 1px) { .item { width: 70px } }", body, 600), 0.01,
                "and and or cannot be mixed without parentheses: the rule is invalid");
        Check.near(10.0, widthOfI(base + "@container (frobnicate: 1) { .item { width: 70px } }", body, 600), 0.01, "an unknown feature is unknown and counts as false");
        Check.near(70.0, widthOfI(base + "@container (min-width: 1px) or (frobnicate: 1) { .item { width: 70px } }", body, 600), 0.01, "true or unknown is true");
        Check.near(10.0, widthOfI(base + "@container not (frobnicate: 1) { .item { width: 70px } }", body, 600), 0.01, "not unknown is still unknown");
    }

    /**
     * A container name restricts the query to the containers with that name, and the nearest named container wins over a nearer unnamed one; the
     * <code>container</code> shorthand sets the name and the type.
     * <p>
     * 컨테이너 이름은 질의를 그 이름을 가진 컨테이너로 제한하며, 더 가까운 이름 없는 컨테이너보다 가장 가까운 이름 있는 컨테이너가 우선한다. <code>container</code> 단축 속성은 이름과
     * 종류를 정한다.
     */
    public void testNamedContainers() {
        String css = ".outer { container: side / inline-size; width: 400px } .inner { container-type: inline-size; width: 100px }"
                + " .item { width: 10px; height: 10px }";
        String body = "<div class=outer><div class=inner><div class=item id=i></div></div></div>";
        Check.near(60.0, widthOfI(css + " @container side (min-width: 300px) { .item { width: 60px } }", body, 600), 0.01, "the named outer container is 400px");
        Check.near(10.0, widthOfI(css + " @container (min-width: 300px) { .item { width: 60px } }", body, 600), 0.01, "the unnamed query uses the nearest container, 100px");
        Check.near(10.0, widthOfI(css + " @container other (min-width: 1px) { .item { width: 60px } }", body, 600), 0.01, "no container has that name");
        Check.near(60.0, widthOfI(".outer { container-name: a b; container-type: inline-size; width: 400px } .item { width: 10px; height: 10px }"
                + " @container b (min-width: 300px) { .item { width: 60px } }", body.replace("class=inner", ""), 600), 0.01, "a container can have several names");
    }

    /**
     * The container type decides the axes that are contained: an <code>inline-size</code> container cannot be queried for its height (the query goes
     * on to the next container or fails); a <code>size</code> container can.
     * <p>
     * 컨테이너 종류가 격리되는 축을 정한다: <code>inline-size</code> 컨테이너는 높이를 질의할 수 없고(질의는 다음 컨테이너로 가거나 실패한다), <code>size</code> 컨테이너는 가능하다.
     */
    public void testAxes() {
        String item = ".item { width: 10px; height: 10px } ";
        String body = "<div class=wrap><div class=item id=i></div></div>";
        Check.near(70.0, widthOfI(item + ".wrap { container-type: size; width: 200px; height: 120px }"
                + " @container (min-height: 100px) { .item { width: 70px } }", body, 600), 0.01, "a size container has a height");
        Check.near(10.0, widthOfI(item + ".wrap { container-type: inline-size; width: 200px; height: 120px }"
                + " @container (min-height: 100px) { .item { width: 70px } }", body, 600), 0.01, "an inline-size container does not contain its height");
        Check.near(70.0, widthOfI(item + ".wrap { container-type: size; width: 200px; height: 120px }"
                + " @container (aspect-ratio > 1) { .item { width: 70px } }", body, 600), 0.01, "aspect-ratio needs both axes");
        Check.near(70.0, widthOfI(item + ".wrap { container-type: size; width: 200px; height: 120px }"
                + " @container (orientation: landscape) { .item { width: 70px } }", body, 600), 0.01, "orientation");
        Check.near(10.0, widthOfI(item + ".wrap { width: 200px; height: 120px }"
                + " @container (min-width: 1px) { .item { width: 70px } }", body, 600), 0.01, "an element without container-type is not a container");
        Check.near(70.0, widthOfI(item + ".outer { container-type: size; width: 300px; height: 150px } .wrap { container-type: inline-size; width: 200px }"
                + " @container (min-height: 100px) { .item { width: 70px } }", "<div class=outer>" + body + "</div>", 600), 0.01,
                "the height query skips the inline-size container and finds the size container outside");
    }

    /**
     * <code>style()</code> queries compare a custom property of the parent (or of the named container); they need no container type.
     * <p>
     * <code>style()</code> 질의는 부모(또는 이름 있는 컨테이너)의 사용자 정의 속성을 비교한다. 컨테이너 종류는 필요 없다.
     */
    public void testStyleQueries() {
        String css = ".item { width: 10px; height: 10px } @container style(--theme: dark) { .item { width: 70px } } @container style(--flag) { .item { height: 33px } }";
        Check.near(70.0, widthOfI(css, "<div style='--theme: dark'><div class=item id=i></div></div>", 600), 0.01, "the parent has --theme: dark");
        Check.near(10.0, widthOfI(css, "<div style='--theme: light'><div class=item id=i></div></div>", 600), 0.01, "a different value");
        Check.near(10.0, widthOfI(css, "<div><div class=item id=i></div></div>", 600), 0.01, "no custom property");
        LayoutResult r = LayoutTestSupport.layout("<style>" + css + "</style><div style='--flag: 1'><div class=item id=i></div></div>", 600);
        Check.near(33.0, LayoutTestSupport.box(r, "i").getHeight(), 0.01, "style(--flag) is true if the property is defined");
    }

    /**
     * A container query applies to the descendants only, never to the container itself; a container nested in another one that is not yet
     * settled gets its size in a later pass.
     * <p>
     * 컨테이너 질의는 자손에만 적용되고 컨테이너 자신에는 적용되지 않는다. 아직 안정되지 않은 다른 컨테이너 안의 컨테이너는 이후 배치에서 크기를 얻는다.
     */
    public void testNestedContainersSettle() {
        String css = ".a { container-type: inline-size } .b { container-type: inline-size; width: 50px } .item { width: 10px; height: 10px }"
                + " @container (min-width: 500px) { .b { width: 250px } } @container (min-width: 200px) { .item { width: 33px } }";
        String body = "<div class=a><div class=b><div class=item id=i></div></div></div>";
        Check.near(33.0, widthOfI(css, body, 600), 0.01, "a is 600px, so b becomes 250px, so the item sees a container of 250px");
        Check.near(10.0, widthOfI(css, body, 400), 0.01, "a is 400px: b stays 50px and the item sees 50px");
    }

    /**
     * The container relative lengths <code>cqw</code>, <code>cqh</code>, <code>cqi</code>, <code>cqb</code>, <code>cqmin</code> and <code>cqmax</code> are
     * percents of the size of the nearest container (also without any <code>@container</code> rule); without a container they are percents of the viewport.
     * <p>
     * 컨테이너 상대 길이 <code>cqw</code>, <code>cqh</code>, <code>cqi</code>, <code>cqb</code>, <code>cqmin</code>, <code>cqmax</code> 는 가장 가까운 컨테이너 크기의 백분율이다
     * (<code>@container</code> 규칙이 없어도). 컨테이너가 없으면 뷰포트의 백분율이다.
     */
    public void testContainerLengths() {
        String wrap = ".wrap { container-type: size; width: 300px; height: 200px } .item { height: 10px }";
        String body = "<div class=wrap><div class=item id=i></div></div>";
        Check.near(150.0, widthOfI(wrap + " .item { width: 50cqw }", body, 600), 0.01, "50cqw is half of the container width");
        Check.near(150.0, widthOfI(wrap + " .item { width: 50cqi }", body, 600), 0.01, "cqi is the inline size");
        Check.near(20.0, widthOfI(wrap + " .item { width: 10cqh }", body, 600), 0.01, "10cqh is a tenth of the height of a size container");
        Check.near(20.0, widthOfI(wrap + " .item { width: 10cqb }", body, 600), 0.01, "cqb is the block size");
        Check.near(30.0, widthOfI(wrap + " .item { width: 10cqmax }", body, 600), 0.01, "cqmax is the larger of cqw and cqh");
        Check.near(20.0, widthOfI(wrap + " .item { width: 10cqmin }", body, 600), 0.01, "cqmin is the smaller");
        Check.near(130.0, widthOfI(wrap + " .item { width: calc(50cqw - 20px) }", body, 600), 0.01, "inside calc()");
        Check.near(60.0, widthOfI(".item { width: 10cqw; height: 10px }", "<div class=item id=i></div>", 600), 0.01, "without a container: a percent of the viewport width");
        Check.near(300.0, widthOfI(".wrap { container-type: inline-size; width: 300px } .item { width: 100cqw; height: 10px }", body, 600), 0.01,
                "an inline-size container gives cqw");
        Check.near(60.0, widthOfI(".wrap { container-type: inline-size; width: 300px } .item { width: 10cqh; height: 10px }", body, 600), 0.01,
                "but not cqh: the viewport height (600) is used, a tenth of it");
        Check.near(100.0, widthOfI(".outer { container-type: size; width: 400px; height: 100px } .wrap { container-type: inline-size; width: 200px }"
                + " .item { width: 100cqh; height: 10px }", "<div class=outer>" + body + "</div>", 600), 0.01, "cqh skips the inline-size container and finds the size container");
        Check.near(100.0, widthOfI(".outer { container-type: inline-size } .item { width: 25cqw; height: 10px }", "<div class=outer>" + body + "</div>", 400), 0.01,
                "a block container fills the viewport: a quarter of 400");
    }

    /**
     * Inline-size containment makes the intrinsic width of the box zero: a shrink-to-fit container does not grow with its content (it still has
     * <code>contain-intrinsic-width</code>).
     * <p>
     * 인라인 크기 격리는 상자의 고유 너비를 0 으로 만든다: shrink-to-fit 컨테이너는 내용에 따라 커지지 않는다(<code>contain-intrinsic-width</code> 는 가진다).
     */
    public void testInlineSizeContainmentOfShrinkToFit() {
        String inner = "<div style='width:100px;height:10px'></div>";
        LayoutResult plain = LayoutTestSupport.layout("<div id=b style='display:inline-block'>" + inner + "</div>", 600);
        Check.near(100.0, LayoutTestSupport.box(plain, "b").getWidth(), 0.01, "an ordinary inline-block fits its content");
        LayoutResult c = LayoutTestSupport.layout("<div id=b style='display:inline-block;container-type:inline-size'>" + inner + "</div>", 600);
        Check.near(0.0, LayoutTestSupport.box(c, "b").getWidth(), 0.01, "an inline-size container ignores its content");
        LayoutResult k = LayoutTestSupport.layout("<div id=b style='display:inline-block;contain:inline-size;contain-intrinsic-width:40px'>" + inner + "</div>", 600);
        Check.near(40.0, LayoutTestSupport.box(k, "b").getWidth(), 0.01, "contain-intrinsic-width");
        LayoutResult f = LayoutTestSupport.layout("<div id=b style='float:left;container-type:inline-size'>" + inner + "</div>", 600);
        Check.near(0.0, LayoutTestSupport.box(f, "b").getWidth(), 0.01, "a float");
    }

    /**
     * Size containment makes an automatic height zero (or <code>contain-intrinsic-height</code>), although the contents are laid out and overflow;
     * an explicit height is kept.
     * <p>
     * 크기 격리는 자동 높이를 0(또는 <code>contain-intrinsic-height</code>)으로 만든다. 내용은 배치되어 넘친다. 명시한 높이는 유지된다.
     */
    public void testSizeContainmentHeight() {
        String inner = "<div id=c style='width:100px;height:60px'></div>";
        LayoutResult plain = LayoutTestSupport.layout("<div id=b>" + inner + "</div>", 600);
        Check.near(60.0, LayoutTestSupport.box(plain, "b").getHeight(), 0.01, "an ordinary block grows with its content");
        LayoutResult s = LayoutTestSupport.layout("<div id=b style='contain:size'>" + inner + "</div>", 600);
        Check.near(0.0, LayoutTestSupport.box(s, "b").getHeight(), 0.01, "contain: size");
        Check.near(60.0, LayoutTestSupport.box(s, "c").getHeight(), 0.01, "the child is still laid out");
        LayoutResult k = LayoutTestSupport.layout("<div id=b style='container-type:size;contain-intrinsic-height:25px'>" + inner + "</div>", 600);
        Check.near(25.0, LayoutTestSupport.box(k, "b").getHeight(), 0.01, "contain-intrinsic-height");
        LayoutResult e = LayoutTestSupport.layout("<div id=b style='contain:strict;height:15px'>" + inner + "</div>", 600);
        Check.near(15.0, LayoutTestSupport.box(e, "b").getHeight(), 0.01, "an explicit height wins");
        LayoutResult i = LayoutTestSupport.layout("<div id=b style='contain:inline-size'>" + inner + "</div>", 600);
        Check.near(60.0, LayoutTestSupport.box(i, "b").getHeight(), 0.01, "inline-size containment does not touch the height");
    }

    /**
     * Layout containment makes the box a block formatting context (it contains its floats), and the containing block of the absolutely positioned
     * and fixed positioned descendants.
     * <p>
     * 레이아웃 격리는 상자를 블록 서식 문맥(안의 float 를 포함)과 절대 배치와 fixed 자손의 포함 블록으로 만든다.
     */
    public void testLayoutContainment() {
        LayoutResult plain = LayoutTestSupport.layout("<div id=b><div style='float:left;width:10px;height:40px'></div></div>", 600);
        Check.near(0.0, LayoutTestSupport.box(plain, "b").getHeight(), 0.01, "a float does not stretch an ordinary block");
        LayoutResult l = LayoutTestSupport.layout("<div id=b style='contain:layout'><div style='float:left;width:10px;height:40px'></div></div>", 600);
        Check.near(40.0, LayoutTestSupport.box(l, "b").getHeight(), 0.01, "a layout contained block contains its float");
        LayoutResult a = LayoutTestSupport.layout("<div style='height:200px'></div><div id=b style='contain:layout;height:100px;width:100px'>"
                + "<div id=c style='position:absolute;top:5px;left:7px;width:10px;height:10px'></div></div>", 600);
        LayoutBox b = LayoutTestSupport.box(a, "b");
        LayoutBox c = LayoutTestSupport.box(a, "c");
        Check.near(b.getX() + 7, c.getX(), 0.01, "the absolute box is placed against the contained box (x)");
        Check.near(b.getY() + 5, c.getY(), 0.01, "and (y)");
        LayoutResult fx = LayoutTestSupport.layout("<div style='height:200px'></div><div id=b style='contain:paint;height:100px;width:100px'>"
                + "<div id=c style='position:fixed;top:5px;left:7px;width:10px;height:10px'></div></div>", 600);
        Check.near(LayoutTestSupport.box(fx, "b").getX() + 7, LayoutTestSupport.box(fx, "c").getX(), 0.01, "paint containment also contains fixed boxes");
        Check.near(LayoutTestSupport.box(fx, "b").getY() + 5, LayoutTestSupport.box(fx, "c").getY(), 0.01, "fixed y");
    }

    /**
     * <code>content-visibility: hidden</code> builds no boxes for the contents; <code>auto</code> shows them.
     * <p>
     * <code>content-visibility: hidden</code> 은 내용의 상자를 만들지 않고, <code>auto</code> 는 내용을 보인다.
     */
    public void testContentVisibility() {
        LayoutResult h = LayoutTestSupport.layout("<div id=b style='content-visibility:hidden;width:100px'><p id=p>hello world</p></div>", 600);
        Check.near(0.0, LayoutTestSupport.box(h, "b").getHeight(), 0.01, "hidden contents have no height");
        Check.isTrue(h.getBoxes(((org.duckdns.hjow.swingwebpanel.dom.Element) h.getRoot().getNode()).getOwnerDocument().getElementById("p")).isEmpty(), "the child has no box");
        LayoutResult s = LayoutTestSupport.layout("<div id=b style='content-visibility:hidden;width:100px;height:30px'>text</div>", 600);
        Check.near(30.0, LayoutTestSupport.box(s, "b").getHeight(), 0.01, "an explicit height is kept");
        LayoutResult a = LayoutTestSupport.layout("<div id=b style='content-visibility:auto;width:100px'><div id=c style='height:20px'></div></div>", 600);
        Check.near(20.0, LayoutTestSupport.box(a, "b").getHeight(), 0.01, "auto shows the contents");
    }
}
