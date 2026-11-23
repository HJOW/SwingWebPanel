package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the writing modes (plan item P10-03, CSS Writing Modes Level 3): vertical text in <code>vertical-rl</code>,
 * <code>vertical-lr</code> and <code>sideways-lr</code> as an orthogonal flow root of horizontal text (the fit-content inline size
 * which is the height, the block size which is the width), the stacking of the lines from the right or from the left, the rotation of
 * the glyphs, physical <code>width</code>, <code>height</code>, margins, paddings and borders of vertical boxes, line wrapping at the
 * height, blocks and lists in vertical text, horizontal islands in vertical text, atomic inline vertical boxes, replaced elements,
 * flex and multi-column containers and absolutely positioned boxes in vertical text, <code>direction:rtl</code> and a vertical root
 * element. The expected coordinates are computed by hand from the specification and checked against real runs of the HTML parser, the
 * CSS engine and the layout engine.
 * <p>
 * 쓰기 모드 시험(계획 P10-03, CSS Writing Modes Level 3): 가로쓰기의 직교 흐름 루트로서 <code>vertical-rl</code>, <code>vertical-lr</code>, <code>sideways-lr</code> 세로쓰기(높이인
 * fit-content 인라인 크기, 너비인 블록 크기), 오른쪽이나 왼쪽에서 쌓이는 줄, 글리프 회전, 세로 상자의 물리적 <code>width</code>, <code>height</code>, margin, padding, border,
 * 높이에서의 줄바꿈, 세로쓰기 안의 블록과 목록, 세로쓰기 안의 가로 섬, 원자적 인라인 세로 상자, 치환 요소, 플렉스와 다단 컨테이너, 절대 배치 상자,
 * <code>direction:rtl</code>, 세로 루트 요소. 기대 좌표는 규격으로 손계산했으며 HTML 파서, CSS 엔진, 레이아웃 엔진을 실제로 돌려 확인한다.
 */
public class WritingModeLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WritingModeLayoutTest() {
    }

    /**
     * Gets the box of an id.
     * <p>
     * id 의 상자를 얻는다.
     *
     * @param r  the result / 결과
     * @param id the id / id
     * @return the box / 상자
     */
    private static LayoutBox b(LayoutResult r, String id) {
        return LayoutTestSupport.box(r, id);
    }

    /**
     * Returns the text fragments below a box in document order.
     * <p>
     * 상자 아래의 텍스트 조각을 문서 순서로 반환한다.
     *
     * @param box the box / 상자
     * @return the fragments / 조각
     */
    private static List<LayoutBox> texts(LayoutBox box) {
        List<LayoutBox> out = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(box, LayoutBox.Type.TEXT, out);
        return out;
    }

    /**
     * In <code>vertical-rl</code> the lines run from top to bottom and stack from right to left; the physical height is the inline size
     * (here specified), the width is the block size (the lines); the glyphs are rotated clockwise.
     * <p>
     * <code>vertical-rl</code> 에서 줄은 위에서 아래로 가고 오른쪽에서 왼쪽으로 쌓인다. 물리적 높이는 인라인 크기(여기서는 지정)이고 너비는 블록 크기(줄)이며 글리프는 시계 방향으로 회전한다.
     */
    public void testVerticalRlLines() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:100px;line-height:10px'>aaa<br>bb<br>c</div><div id=after style='height:5px'></div>", 400);
        LayoutBox v = b(r, "v");
        Check.isTrue(v.isVertical() && v.isOrthogonalFlowRoot(), "vertical orthogonal flow root");
        Check.near(100, v.getHeight(), T, "physical height = specified inline size");
        Check.near(30, v.getWidth(), T, "physical width = three lines of 10");
        Check.near(0, v.getX(), T, "block-level box at the left of the containing block");
        Check.near(100, b(r, "after").getY(), T, "the next block follows the box");
        List<LayoutBox> lines = LayoutTestSupport.lines(v);
        Check.equal(3, lines.size(), "three lines");
        Check.near(20, lines.get(0).getX(), T, "the first line is at the right");
        Check.near(10, lines.get(1).getX(), T, "the second line is left of it");
        Check.near(0, lines.get(2).getX(), T, "the third line is at the left");
        Check.near(10, lines.get(0).getWidth(), T, "a line is as wide as the line height");
        Check.near(100, lines.get(0).getHeight(), T, "and as high as the inline size");
        List<LayoutBox> t = texts(v);
        Check.near(3 * cw, t.get(0).getHeight(), T, "text height = advance of aaa");
        Check.near(0, t.get(0).getY(), T, "text starts at the top");
        Check.equal(1, t.get(0).getTextRotation(), "glyphs rotated by a quarter turn clockwise");
        Check.near(3 * cw, t.get(0).getTextAdvance(), T, "advance along the line");
    }

    /**
     * In <code>vertical-lr</code> the first line is at the left.
     * <p>
     * <code>vertical-lr</code> 에서 첫 줄은 왼쪽에 있다.
     */
    public void testVerticalLrLines() {
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-lr;height:100px;line-height:10px'>aaa<br>bb<br>c</div>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(b(r, "v"));
        Check.near(0, lines.get(0).getX(), T, "first line at the left");
        Check.near(10, lines.get(1).getX(), T, "second line right of it");
        Check.near(20, lines.get(2).getX(), T, "third line");
        Check.near(30, b(r, "v").getWidth(), T, "width");
        Check.equal(1, texts(b(r, "v")).get(0).getTextRotation(), "clockwise rotation also in vertical-lr");
    }

    /**
     * <code>sideways-lr</code>: the lines run from bottom to top and stack from left to right; the glyphs are rotated counter-clockwise.
     * <p>
     * <code>sideways-lr</code>: 줄은 아래에서 위로 가고 왼쪽에서 오른쪽으로 쌓이며 글리프는 시계 반대 방향으로 회전한다.
     */
    public void testSidewaysLr() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:sideways-lr;height:100px;line-height:10px'>aaa<br>bb</div>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(b(r, "v"));
        Check.near(0, lines.get(0).getX(), T, "first line at the left");
        Check.near(10, lines.get(1).getX(), T, "second line right of it");
        List<LayoutBox> t = texts(b(r, "v"));
        Check.equal(3, t.get(0).getTextRotation(), "counter-clockwise");
        Check.near(100 - 3 * cw, t.get(0).getY(), T, "the text starts at the bottom and runs upwards");
    }

    /**
     * Lines wrap at the inline size (the height); the block size grows with the number of lines.
     * <p>
     * 줄은 인라인 크기(높이)에서 줄바꿈하고 블록 크기는 줄 수에 따라 커진다.
     */
    public void testWrappingAtTheHeight() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:" + (cw * 10) + "px;line-height:10px'>aaaaaaaaa bbbbbbbbb ccc</div>", 400);
        List<LayoutBox> lines = LayoutTestSupport.lines(b(r, "v"));
        Check.equal(3, lines.size(), "one line per word (a line takes ten characters)");
        Check.near(30, b(r, "v").getWidth(), T, "three lines");
        Check.near(cw * 10, b(r, "v").getHeight(), T, "height");
    }

    /**
     * Without a height the inline size is the fit-content size: the max-content size if it fits the viewport (600 here), else the
     * viewport height.
     * <p>
     * 높이가 없으면 인라인 크기는 fit-content 크기이다: 뷰포트(여기서는 600)에 들어가면 max-content 크기, 아니면 뷰포트 높이.
     */
    public void testFitContentInlineSize() {
        double cw = LayoutTestSupport.charWidth();
        StringBuilder longText = new StringBuilder();
        // 600 보다 훨씬 긴 텍스트를 만든다.
        for (int i = 0; i < 60; i++) {
            longText.append("word").append(i).append(' ');
        }
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='writing-mode:vertical-rl;line-height:10px'>aaaaaaaaaa</div>"
                + "<div id=c style='writing-mode:vertical-rl;line-height:10px'>" + longText + "</div>", 400);
        Check.near(10 * cw, b(r, "a").getHeight(), T, "max-content of ten characters");
        Check.near(10, b(r, "a").getWidth(), T, "one line");
        Check.near(600, b(r, "c").getHeight(), T, "limited by the height of the viewport");
        Check.isTrue(b(r, "c").getWidth() >= 20, "several lines: " + b(r, "c").getWidth());
    }

    /**
     * Margins, paddings and borders keep their physical sides in vertical text: the top and bottom ones lie along the inline axis.
     * <p>
     * margin, padding, border 는 세로쓰기에서도 물리적 변을 유지한다: 위쪽과 아래쪽은 인라인 축을 따라 놓인다.
     */
    public void testPhysicalEdges() {
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:100px;line-height:10px;padding:5px 10px 15px 20px;border:2px solid #000;margin:3px 4px 6px 8px'>aaa<br>bb<br>c</div>"
                + "<div id=after></div>", 400);
        LayoutBox v = b(r, "v");
        Check.near(5, v.getPaddingTop(), T, "padding-top");
        Check.near(10, v.getPaddingRight(), T, "padding-right");
        Check.near(15, v.getPaddingBottom(), T, "padding-bottom");
        Check.near(20, v.getPaddingLeft(), T, "padding-left");
        Check.near(2, v.getBorderTop(), T, "border");
        Check.near(100 + 5 + 15 + 4, v.getHeight(), T, "height: content (inline size) + padding + borders");
        Check.near(30 + 10 + 20 + 4, v.getWidth(), T, "width: three lines + padding + borders");
        Check.near(8, v.getX(), T, "margin-left");
        Check.near(3, v.getY(), T, "margin-top");
        Check.near(3 + 100 + 5 + 15 + 4 + 6, b(r, "after").getY(), T, "the next block follows the margin box");
        List<LayoutBox> lines = LayoutTestSupport.lines(v);
        // 내용 상자는 x 가 8+2+20=30 부터 30+30=60 까지이고 첫 줄은 오른쪽 끝에 있다.
        Check.near(50, lines.get(0).getX(), T, "first line at the right of the content box");
        Check.near(30, lines.get(2).getX(), T, "third line at the left of the content box");
        Check.near(3 + 2 + 5, lines.get(0).getY(), T, "the lines start below the top padding");
    }

    /**
     * A specified <code>width</code> is the physical width (the block size); <code>min-height</code> and <code>max-height</code>
     * limit the inline size.
     * <p>
     * 지정한 <code>width</code> 는 물리적 너비(블록 크기)이고 <code>min-height</code>, <code>max-height</code> 는 인라인 크기를 제한한다.
     */
    public void testPhysicalSizeProperties() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='writing-mode:vertical-rl;width:80px;height:50px;line-height:10px'>x</div>"
                + "<div id=b style='writing-mode:vertical-rl;max-height:30px;line-height:10px'>aaaa aaaa aaaa aaaa</div>"
                + "<div id=c style='writing-mode:vertical-rl;min-height:70px;line-height:10px'>a</div>", 400);
        Check.near(80, b(r, "a").getWidth(), T, "specified width");
        Check.near(50, b(r, "a").getHeight(), T, "specified height");
        Check.near(30, b(r, "b").getHeight(), T, "max-height limits the inline size");
        Check.near(40, b(r, "b").getWidth(), T, "so the text wraps into four lines (a word is 24 pixels, two words do not fit)");
        Check.near(70, b(r, "c").getHeight(), T, "min-height");
    }

    /**
     * Auto margins in the containing block's frame center the box horizontally; blocks stack vertically in the horizontal parent.
     * <p>
     * 포함 블록 프레임의 auto margin 은 상자를 가로로 가운데에 놓는다. 가로 부모에서 블록은 세로로 쌓인다.
     */
    public void testPositioningInHorizontalParent() {
        LayoutResult r = LayoutTestSupport.layout("<div id=a style='height:10px'></div><div id=v style='writing-mode:vertical-rl;height:40px;width:30px;margin:0 auto'>x</div>"
                + "<div id=z style='height:5px'></div>", 400);
        Check.near(185, b(r, "v").getX(), T, "centered by the auto margins");
        Check.near(10, b(r, "v").getY(), T, "below the previous block");
        Check.near(50, b(r, "z").getY(), T, "the next block follows");
    }

    /**
     * Blocks inside vertical text stack from right to left; a list item is a block too.
     * <p>
     * 세로쓰기 안의 블록은 오른쪽에서 왼쪽으로 쌓이고 목록 항목도 블록이다.
     */
    public void testBlocksInVerticalText() {
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:100px;line-height:10px'><p id=p1 style='margin:0'>aa<br>bb</p>"
                + "<p id=p2 style='margin:0 5px 0 0'>cc</p><div id=p3 style='margin:0'>dd</div></div>", 400);
        LayoutBox p1 = b(r, "p1");
        LayoutBox p2 = b(r, "p2");
        LayoutBox p3 = b(r, "p3");
        // 전체 너비: p1 2 줄(20) + p2 1 줄(10) + margin 5 + p3 1 줄(10) = 45
        Check.near(45, b(r, "v").getWidth(), T, "total block size");
        Check.near(25, p1.getX(), T, "p1 is at the right (45 - 20)");
        Check.near(20, p1.getWidth(), T, "p1 is two lines wide");
        Check.near(100, p1.getHeight(), T, "and as high as the inline size");
        Check.near(10, p2.getX(), T, "p2 left of p1 after its margin (the margin is on the block-end side: the left)");
        Check.near(0, p3.getX(), T, "p3 at the left");
    }

    /**
     * A horizontal box inside vertical text is a horizontal island: it keeps its physical size and its text is not rotated.
     * <p>
     * 세로쓰기 안의 가로 상자는 가로 섬이다: 물리적 크기를 유지하고 텍스트는 회전하지 않는다.
     */
    public void testHorizontalIslandInVerticalText() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:100px;line-height:10px'>"
                + "<div id=h style='writing-mode:horizontal-tb;width:50px;line-height:10px'>abc</div></div>", 400);
        LayoutBox h = b(r, "h");
        Check.near(50, h.getWidth(), T, "physical width");
        Check.near(10, h.getHeight(), T, "one line of 10");
        Check.near(0, h.getX(), T, "the island is the only block: at the left edge of the vertical box (width 50)");
        Check.near(50, b(r, "v").getWidth(), T, "the vertical box is as wide as the island");
        LayoutBox text = texts(h).get(0);
        Check.equal(0, text.getTextRotation(), "the text of the island is not rotated");
        Check.near(3 * cw, text.getWidth(), T, "its width is its advance");
        Check.isTrue(Math.abs(text.getY() - h.getY()) < 2, "the text is on the line of the island: " + (text.getY() - h.getY()));
    }

    /**
     * A vertical inline block in horizontal text is an atomic box: its width is the lines, its height the inline size.
     * <p>
     * 가로쓰기 안의 세로 inline-block 은 원자적 상자이다: 너비는 줄이고 높이는 인라인 크기이다.
     */
    public void testVerticalInlineBlockInHorizontalText() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=p style='line-height:10px'>ab<span id=s style='display:inline-block;writing-mode:vertical-rl;height:50px;line-height:10px'>xyz</span>cd</div>", 400);
        LayoutBox s = b(r, "s");
        Check.near(10, s.getWidth(), T, "one vertical line");
        Check.near(50, s.getHeight(), T, "height");
        Check.near(2 * cw, s.getX(), T, "after the text ab");
        Check.isTrue(b(r, "p").getHeight() >= 50 && b(r, "p").getHeight() < 55, "the line is as high as the inline block (plus the descent): " + b(r, "p").getHeight());
        List<LayoutBox> t = texts(b(r, "p"));
        Check.near(2 * cw + 10, t.get(t.size() - 1).getX(), T, "the text cd follows the inline block");
    }

    /**
     * Images keep their orientation in vertical text.
     * <p>
     * 이미지는 세로쓰기에서도 방향을 유지한다.
     */
    public void testReplacedElementsKeepOrientation() {
        ReplacedResolver res = new ReplacedResolver() {
            /**
             * Gives every image the natural size 40 by 20.
             * <p>
             * 모든 이미지에 자연 크기 40×20 을 준다.
             *
             * @param element the element / 요소
             * @return the content / 콘텐츠
             */
            public ReplacedContent getReplacedContent(Element element) {
                return new ReplacedContent() {
                    /**
                     * Returns 40.
                     * <p>
                     * 40 을 반환한다.
                     *
                     * @return width / 너비
                     */
                    public double getNaturalWidth() {
                        return 40;
                    }

                    /**
                     * Returns 20.
                     * <p>
                     * 20 을 반환한다.
                     *
                     * @return height / 높이
                     */
                    public double getNaturalHeight() {
                        return 20;
                    }
                };
            }
        };
        LayoutResult r = LayoutTestSupport.layout(res, "<div id=v style='writing-mode:vertical-rl;height:100px;line-height:10px'>a<img id=i>b</div>", 400);
        LayoutBox i = b(r, "i");
        Check.near(40, i.getWidth(), T, "natural width stays the width");
        Check.near(20, i.getHeight(), T, "natural height stays the height");
        Check.isTrue(b(r, "v").getWidth() >= 40 && b(r, "v").getWidth() < 45, "the line is as wide as the image (plus the descent of the strut): " + b(r, "v").getWidth());
    }

    /**
     * Inline direction <code>rtl</code> starts the lines at the bottom.
     * <p>
     * 인라인 방향 <code>rtl</code> 이면 줄이 아래에서 시작한다.
     */
    public void testDirectionRtl() {
        double cw = LayoutTestSupport.charWidth();
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;direction:rtl;height:100px;line-height:10px'>aaa</div>", 400);
        LayoutBox t = texts(b(r, "v")).get(0);
        Check.near(100 - 3 * cw, t.getY(), T, "the text is at the bottom (inline-start)");
    }

    /**
     * A flex container in vertical text: the row direction is the inline axis, that is vertical.
     * <p>
     * 세로쓰기 안의 플렉스 컨테이너: 행 방향은 인라인 축이며 세로이다.
     */
    public void testFlexInVerticalText() {
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:100px'><div id=f style='display:flex;line-height:10px'>"
                + "<div id=a style='height:30px;width:20px'></div><div id=b style='height:40px;width:20px'></div></div></div>", 400);
        LayoutBox a = b(r, "a");
        LayoutBox bb = b(r, "b");
        Check.near(a.getY() + 30, bb.getY(), T, "the second item follows the first one downwards");
        Check.near(a.getX(), bb.getX(), T, "both items are in the same column");
        Check.near(30, a.getHeight(), T, "item height");
        Check.near(20, a.getWidth(), T, "item width");
        Check.near(100, b(r, "f").getHeight(), T, "the container fills the inline size (100) of the vertical box");
        Check.near(20, b(r, "f").getWidth(), T, "and is as wide as its items (the block size)");
    }

    /**
     * A multi-column container in vertical text lays its columns along the inline axis (vertically).
     * <p>
     * 세로쓰기 안의 다단 컨테이너는 열을 인라인 축(세로)을 따라 놓는다.
     */
    public void testMulticolInVerticalText() {
        LayoutResult r = LayoutTestSupport.layout("<div id=c style='writing-mode:vertical-rl;height:100px;column-count:2;column-gap:0;line-height:10px'>"
                + "<div id=a style='width:20px'></div><div id=b style='width:20px'></div></div>", 400);
        Check.equal(2, b(r, "c").getColumnCount(), "two columns");
        Check.near(0, b(r, "a").getY() - b(r, "c").getY(), T, "the first column is at the top");
        Check.near(50, b(r, "b").getY() - b(r, "c").getY(), T, "the second column starts after the first one (inline size 100 / 2)");
    }

    /**
     * An absolutely positioned box in vertical text uses the physical offsets: <code>top</code> and <code>right</code> are measured
     * from the physical top and right edges.
     * <p>
     * 세로쓰기 안의 절대 배치 상자는 물리적 오프셋을 쓴다: <code>top</code> 과 <code>right</code> 는 물리적인 위쪽과 오른쪽 가장자리에서 잰다.
     */
    public void testAbsoluteBoxInVerticalText() {
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;position:relative;height:100px;width:60px;line-height:10px'>"
                + "<div id=a style='position:absolute;top:10px;right:5px;width:20px;height:15px'></div>x</div>", 400);
        LayoutBox a = b(r, "a");
        LayoutBox v = b(r, "v");
        Check.near(v.getY() + 10, a.getY(), T, "10 pixels below the top edge");
        Check.near(v.getX() + 60 - 5 - 20, a.getX(), T, "5 pixels from the right edge");
        Check.near(20, a.getWidth(), T, "physical width");
        Check.near(15, a.getHeight(), T, "physical height");
    }

    /**
     * A vertical root element: the page flows from right to left and the document is anchored at the right edge of the viewport when
     * it is narrower.
     * <p>
     * 세로 루트 요소: 페이지가 오른쪽에서 왼쪽으로 흐르고 문서가 뷰포트보다 좁으면 뷰포트의 오른쪽 가장자리에 붙는다.
     */
    public void testVerticalRootElement() {
        LayoutResult r = LayoutTestSupport.layout("<style>html{writing-mode:vertical-rl}body{line-height:10px;height:100px}</style>"
                + "<p id=p1 style='margin:0'>aa</p><p id=p2 style='margin:0'>bb</p>", 400);
        LayoutBox p1 = b(r, "p1");
        LayoutBox p2 = b(r, "p2");
        Check.isTrue(r.getRoot().isVertical(), "the root is vertical");
        Check.near(400 - 10, p1.getX(), T, "the first paragraph is at the right edge of the viewport");
        Check.near(400 - 20, p2.getX(), T, "the second one is left of it");
        Check.near(0, p1.getY(), T, "at the top");
    }

    /**
     * Horizontal documents are not affected by the frames: the boxes are not vertical and the text is not rotated.
     * <p>
     * 가로 문서는 프레임의 영향을 받지 않는다: 상자는 세로가 아니고 텍스트는 회전하지 않는다.
     */
    public void testHorizontalTextIsUnchanged() {
        LayoutResult r = LayoutTestSupport.layout("<div id=h>text</div>", 400);
        Check.isFalse(b(r, "h").isVertical(), "not vertical");
        Check.isFalse(b(r, "h").isOrthogonalFlowRoot(), "not an orthogonal flow root");
        Check.equal(0, texts(b(r, "h")).get(0).getTextRotation(), "no rotation");
    }

    /**
     * A table inside vertical text is horizontal: it keeps its grid and its size.
     * <p>
     * 세로쓰기 안의 표는 가로이다: 격자와 크기를 유지한다.
     */
    public void testTableInVerticalText() {
        LayoutResult r = LayoutTestSupport.layout("<div id=v style='writing-mode:vertical-rl;height:200px;line-height:10px'><table id=t style='border-collapse:separate;border-spacing:0'>"
                + "<tr><td id=c1 style='width:30px;height:20px;padding:0'></td><td id=c2 style='width:40px;padding:0'></td></tr></table></div>", 400);
        LayoutBox t = b(r, "t");
        Check.near(70, t.getWidth(), T, "table width");
        Check.near(20, t.getHeight(), T, "table height");
        Check.near(30, b(r, "c1").getWidth(), T, "first cell");
        Check.near(b(r, "c1").getX() + 30, b(r, "c2").getX(), T, "second cell follows the first one horizontally");
    }
}
