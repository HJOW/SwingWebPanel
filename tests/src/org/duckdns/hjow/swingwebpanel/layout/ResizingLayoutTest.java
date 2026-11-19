package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the geometry of <code>resize</code> (CSS Basic User Interface Module Level 4 section 6.1, https://www.w3.org/TR/css-ui-4/#resize): which boxes can be
 * resized, in which directions, where the handle is, how <code>block</code> and <code>inline</code> follow the writing mode, and how the value of
 * <code>width</code> follows from the wanted border box size.
 * <p>
 * <code>resize</code> 의 기하 시험(CSS Basic User Interface Module Level 4 6.1 절): 크기를 바꿀 수 있는 상자, 방향, 핸들의 위치, <code>block</code> 과
 * <code>inline</code> 이 쓰기 모드를 따르는 방법, 원하는 border box 크기로부터 <code>width</code> 값을 얻는 방법.
 */
public class ResizingLayoutTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ResizingLayoutTest() {
    }

    /**
     * Lays out one box with a style.
     * <p>
     * 스타일을 가진 상자 하나를 배치한다.
     *
     * @param style the style of the box with id <code>b</code> / id 가 <code>b</code> 인 상자의 스타일
     * @return the box / 상자
     */
    private static LayoutBox box(String style) {
        LayoutResult r = LayoutTestSupport.layout("<div id=b style='" + style + "'>x</div>", 400);
        return LayoutTestSupport.box(r, "b");
    }

    /**
     * A box with <code>resize</code> and a scrolling overflow is resizable in the given directions; without <code>resize</code>, with
     * <code>overflow: visible</code> or <code>clip</code> it is not.
     * <p>
     * <code>resize</code> 와 스크롤하는 overflow 가 있는 상자는 주어진 방향으로 크기를 바꿀 수 있다. <code>resize</code> 가 없거나 <code>overflow: visible</code>,
     * <code>clip</code> 이면 바꿀 수 없다.
     */
    public void testDirections() {
        Check.equal(Resizing.BOTH, Resizing.directions(box("width:100px;height:60px;overflow:auto;resize:both")), "both");
        Check.equal(Resizing.HORIZONTAL, Resizing.directions(box("width:100px;height:60px;overflow:hidden;resize:horizontal")), "horizontal");
        Check.equal(Resizing.VERTICAL, Resizing.directions(box("width:100px;height:60px;overflow:scroll;resize:vertical")), "vertical");
        Check.equal(Resizing.NONE, Resizing.directions(box("width:100px;height:60px;overflow:auto")), "resize is none by default");
        Check.equal(Resizing.NONE, Resizing.directions(box("width:100px;height:60px;resize:both")), "overflow visible: the property does not apply");
        Check.equal(Resizing.NONE, Resizing.directions(box("width:100px;height:60px;overflow:clip;resize:both")), "overflow clip is not a scroll container");
        Check.equal(Resizing.BOTH, Resizing.directions(box("width:100px;height:60px;overflow-y:auto;resize:both")), "one scrolling axis is enough");
        Check.equal(Resizing.NONE, Resizing.directions(box("display:inline;overflow:auto;resize:both")), "an inline box is not a block container");
    }

    /**
     * <code>block</code> and <code>inline</code> follow the writing mode.
     * <p>
     * <code>block</code> 과 <code>inline</code> 은 쓰기 모드를 따른다.
     */
    public void testLogicalValuesFollowTheWritingMode() {
        Check.equal(Resizing.VERTICAL, Resizing.directions(box("width:100px;height:60px;overflow:auto;resize:block")), "horizontal-tb: block is vertical");
        Check.equal(Resizing.HORIZONTAL, Resizing.directions(box("width:100px;height:60px;overflow:auto;resize:inline")), "horizontal-tb: inline is horizontal");
        Check.equal(Resizing.HORIZONTAL, Resizing.directions(box("width:100px;height:60px;overflow:auto;resize:block;writing-mode:vertical-rl")),
                "vertical-rl: block is horizontal");
        Check.equal(Resizing.VERTICAL, Resizing.directions(box("width:100px;height:60px;overflow:auto;resize:inline;writing-mode:vertical-rl")),
                "vertical-rl: inline is vertical");
    }

    /**
     * The handle is a 15 pixel square in the bottom right corner of the padding box; a padding box that is smaller has none.
     * <p>
     * 핸들은 padding box 오른쪽 아래의 15 픽셀 정사각형이다. 더 작은 padding box 에는 없다.
     */
    public void testHandleGeometry() {
        LayoutBox b = box("width:100px;height:60px;overflow:auto;resize:both;border:3px solid red");
        double[] h = Resizing.handle(b);
        Check.notNull(h, "the box has a handle");
        // border box 는 106x66 이고 padding box 오른쪽 아래는 (103, 63) 이다.
        Check.near(b.getX() + 106 - 3 - 15, h[0], 0.01, "x of the handle");
        Check.near(b.getY() + 66 - 3 - 15, h[1], 0.01, "y of the handle");
        Check.near(15, h[2], 0.01, "width of the handle");
        Check.near(15, h[3], 0.01, "height of the handle");
        Check.isTrue(Resizing.handle(box("width:10px;height:60px;overflow:auto;resize:both")) == null, "too narrow: no handle");
        Check.isTrue(Resizing.handle(box("width:100px;height:10px;overflow:auto;resize:both")) == null, "too short: no handle");
        Check.isTrue(Resizing.handle(box("width:100px;height:60px;overflow:auto")) == null, "no resize: no handle");
    }

    /**
     * A point is on the handle of the box that was hit or of an ancestor; a point on the box outside the handle is not.
     * <p>
     * 점이 맞은 상자나 조상의 핸들 위에 있는지 찾는다. 핸들 밖의 점은 아니다.
     */
    public void testFind() {
        LayoutResult r = LayoutTestSupport.layout("<div id=b style='width:100px;height:60px;overflow:auto;resize:both'>"
                + "<p id=p style='margin:0;height:200px'>x</p></div>", 400);
        LayoutBox b = LayoutTestSupport.box(r, "b");
        LayoutBox p = LayoutTestSupport.box(r, "p");
        Check.isTrue(Resizing.find(p, 92, 52) == b, "the content that is hit at the corner leads to the box with the handle");
        Check.isTrue(Resizing.find(b, 92, 52) == b, "the box itself");
        Check.isTrue(Resizing.find(b, 50, 30) == null, "the middle of the box is not the handle");
        Check.isTrue(Resizing.find(b, 84, 52) == null, "just left of the handle");
        Check.isTrue(Resizing.find(b, 92, 44) == null, "just above the handle");
        Check.isTrue(Resizing.find(null, 92, 52) == null, "nothing was hit");
    }

    /**
     * A transformed box has its handle where it is seen: the point is mapped back through the transform.
     * <p>
     * 변환된 상자는 핸들이 보이는 자리에 있다: 점은 변환을 거꾸로 적용해 옮긴다.
     */
    public void testFindThroughATransform() {
        LayoutResult r = LayoutTestSupport.layout("<div id=b style='width:100px;height:60px;overflow:auto;resize:both;"
                + "transform:translate(200px,50px)'>x</div>", 600);
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.isTrue(Resizing.find(b, 292, 102) == b, "the handle was moved by the transform");
        Check.isTrue(Resizing.find(b, 92, 52) == null, "its old place is no handle any more");
    }

    /**
     * The insets: padding and border for <code>content-box</code>, none for <code>border-box</code>.
     * <p>
     * 안쪽 여백: <code>content-box</code> 는 padding 과 border, <code>border-box</code> 는 없음.
     */
    public void testInsets() {
        LayoutBox content = box("width:100px;height:60px;overflow:auto;resize:both;padding:5px 7px;border:2px solid red");
        Check.near(18, Resizing.insets(content, true), 0.01, "7 + 7 + 2 + 2");
        Check.near(14, Resizing.insets(content, false), 0.01, "5 + 5 + 2 + 2");
        LayoutBox border = box("width:100px;height:60px;overflow:auto;resize:both;padding:5px 7px;border:2px solid red;box-sizing:border-box");
        Check.near(0, Resizing.insets(border, true), 0.01, "border-box: none");
    }
}
