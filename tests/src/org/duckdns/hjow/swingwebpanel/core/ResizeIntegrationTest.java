package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Panel;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the <code>resize</code> property through the real component (plan item P10-08-d, CSS Basic User Interface Level 4 section 6.1): the handle is
 * painted in the corner of the padding box, a press on it starts a resize without a <code>mousedown</code> for the page, the drag writes the inline
 * <code>width</code> and <code>height</code> of the element (as far as the direction of the value allows, and reduced by padding and border for
 * <code>content-box</code>), the page is laid out again and painted, and the pointer shows a resize cursor over the handle. The events are real AWT
 * events that the component dispatches; the pixels are read from its own paint path.
 * <p>
 * 실제 컴포넌트로 <code>resize</code> 속성을 확인하는 시험(계획 P10-08-d, CSS Basic User Interface Level 4 6.1 절): 핸들은 padding box 모서리에 그려지고, 핸들을 누르면
 * 페이지에 <code>mousedown</code> 을 보내지 않고 크기 조절이 시작되며, 끌면 요소의 인라인 <code>width</code>, <code>height</code> 를 쓰고(값의 방향이 허용하는 만큼,
 * <code>content-box</code> 는 padding 과 border 를 뺌), 페이지를 다시 배치하고 그리며, 핸들 위에서 포인터가 크기 조절 커서를 보인다. 이벤트는 컴포넌트가 전달하는 실제 AWT
 * 이벤트이고 픽셀은 컴포넌트 자신의 그리기 경로에서 읽는다.
 */
public class ResizeIntegrationTest {

    /** The green of the boxes. 상자의 초록색. */
    private static final int GREEN = 0x00CC00;
    /** The common head of the pages. 페이지의 공통 머리. */
    private static final String HEAD = "<!doctype html><style>html,body{margin:0;padding:0} #b{width:100px;height:60px;overflow:auto;resize:both;background:#00cc00}";
    /** The script that reports the style attribute and the number of mouse presses on the page. 스타일 속성과 페이지가 받은 마우스 누름 수를 알리는 스크립트. */
    private static final String SCRIPT = "<script>var md=0;document.addEventListener('mousedown',function(){md++;sessionStorage.setItem('md',String(md))});"
            + "setInterval(function(){sessionStorage.setItem('style',document.getElementById('b').getAttribute('style')||'')},20)</script>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ResizeIntegrationTest() {
    }

    /**
     * Shows a page with one box <code>b</code> and waits until it is painted green.
     * <p>
     * 상자 <code>b</code> 하나가 있는 페이지를 보여 주고 초록색으로 그려질 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param css   the extra style of the box / 상자의 추가 스타일
     * @throws Exception on failure / 실패 시
     */
    private static void open(Panel panel, String css) throws Exception {
        panel.show(HEAD + css + "</style><div id=b>text</div>" + SCRIPT);
        awaitPixel(panel, 5, 5, GREEN, "the box is painted");
    }

    /**
     * Waits until a pixel of the component has a color.
     * <p>
     * 컴포넌트의 픽셀이 어떤 색이 될 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param x     x / x
     * @param y     y / y
     * @param rgb   the expected RGB / 기대 RGB
     * @param what  the description / 설명
     * @throws Exception on failure / 실패 시
     */
    private static void awaitPixel(Panel panel, int x, int y, int rgb, String what) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        int actual;
        // 문서 작업과 그림이 끝날 때까지 실제 그림을 반복해서 확인한다.
        do {
            TestSupport.flushEdt();
            actual = TextSelectionFixture.paint(panel).getRGB(x, y) & 0xFFFFFF;
            if (actual == rgb) {
                return;
            }
            Thread.sleep(20);
        } while (System.currentTimeMillis() < deadline);
        Check.equal(rgb, actual, what);
    }

    /**
     * Waits until the style attribute that the page reports contains a text.
     * <p>
     * 페이지가 알리는 style 속성이 어떤 텍스트를 포함할 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param fragment the text / 텍스트
     * @throws Exception on failure / 실패 시
     */
    private static void awaitStyle(Panel panel, String fragment) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        // 스크립트가 스타일을 알릴 때까지 반복해서 확인한다.
        while (System.currentTimeMillis() < deadline) {
            if (TextSelectionFixture.stored(panel, "style").indexOf(fragment) >= 0) {
                return;
            }
            Thread.sleep(20);
        }
        Check.contains(TextSelectionFixture.stored(panel, "style"), fragment, "the style attribute of the box");
    }

    /**
     * Presses the main button, moves in two steps and releases (component coordinates).
     * <p>
     * 주 버튼을 누르고 두 단계로 옮겨 뗀다(컴포넌트 좌표).
     *
     * @param panel the component / 컴포넌트
     * @param x1    the x of the press / 누른 곳의 x
     * @param y1    the y of the press / 누른 곳의 y
     * @param x2    the x of the release / 뗀 곳의 x
     * @param y2    the y of the release / 뗀 곳의 y
     * @throws Exception on failure / 실패 시
     */
    private static void drag(Panel panel, int x1, int y1, int x2, int y2) throws Exception {
        TextSelectionFixture.drag(panel, x1, y1, x2, y2);
    }

    /**
     * The handle is painted in the corner of the padding box.
     * <p>
     * 핸들이 padding box 모서리에 그려진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHandleIsPainted() throws Exception {
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, "");
            BufferedImage image = TextSelectionFixture.paint(panel);
            int marks = 0;
            // 핸들 영역(85..99, 45..59)에서 초록이 아닌 픽셀(줄)을 센다.
            for (int y = 45; y < 60; y++) {
                for (int x = 85; x < 100; x++) {
                    if ((image.getRGB(x, y) & 0xFFFFFF) != GREEN) {
                        marks++;
                    }
                }
            }
            Check.isTrue(marks >= 8, "the grip lines are painted in the handle area, marks=" + marks);
            // 핸들 밖(왼쪽 위)은 그대로 초록이다.
            Check.equal(GREEN, image.getRGB(60, 20) & 0xFFFFFF, "the rest of the box is green");
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * Without <code>resize</code>, or with <code>overflow: visible</code>, there is no handle and a drag at the corner does not change the size.
     * <p>
     * <code>resize</code> 가 없거나 <code>overflow: visible</code> 이면 핸들이 없고 모서리를 끌어도 크기가 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNoHandleWithoutTheConditions() throws Exception {
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, " #b{overflow:visible}");
            BufferedImage image = TextSelectionFixture.paint(panel);
            int marks = 0;
            // 핸들 영역에는 초록이 아닌 픽셀이 없어야 한다.
            for (int y = 45; y < 60; y++) {
                for (int x = 85; x < 100; x++) {
                    if ((image.getRGB(x, y) & 0xFFFFFF) != GREEN) {
                        marks++;
                    }
                }
            }
            Check.equal(0, marks, "no handle with overflow visible");
            drag(panel, 92, 52, 142, 92);
            Thread.sleep(300);
            TestSupport.flushEdt();
            Check.equal(0, TextSelectionFixture.stored(panel, "style").length(), "the style of the box was not changed");
            Check.equal(0xFFFFFF, TextSelectionFixture.paint(panel).getRGB(130, 80) & 0xFFFFFF, "the box did not grow");
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * Dragging the handle changes the width and the height; the box is laid out again and painted at the new size.
     * <p>
     * 핸들을 끌면 너비와 높이가 바뀌고, 상자는 새 크기로 다시 배치되어 그려진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDragResizesTheBox() throws Exception {
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, "");
            Check.equal(0xFFFFFF, TextSelectionFixture.paint(panel).getRGB(130, 80) & 0xFFFFFF, "before: outside of the box");
            drag(panel, 92, 52, 142, 92);
            awaitStyle(panel, "width: 150px");
            awaitStyle(panel, "height: 100px");
            awaitPixel(panel, 140, 90, GREEN, "the box grew to 150 by 100");
            Check.equal(0xFFFFFF, TextSelectionFixture.paint(panel).getRGB(160, 90) & 0xFFFFFF, "right of the new box");
            Check.equal(0xFFFFFF, TextSelectionFixture.paint(panel).getRGB(140, 110) & 0xFFFFFF, "below the new box");
            // 다시 줄이면 핸들이 새 모서리에 있다.
            drag(panel, 142, 92, 72, 52);
            awaitStyle(panel, "width: 80px");
            awaitStyle(panel, "height: 60px");
            awaitPixel(panel, 90, 70, 0xFFFFFF, "the box shrank again to 80 by 60");
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * <code>resize: horizontal</code> changes the width only and <code>resize: vertical</code> the height only.
     * <p>
     * <code>resize: horizontal</code> 은 너비만, <code>resize: vertical</code> 은 높이만 바꾼다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDirectionsLimitTheResize() throws Exception {
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, " #b{resize:horizontal}");
            drag(panel, 92, 52, 122, 82);
            awaitStyle(panel, "width: 130px");
            Check.isFalse(TextSelectionFixture.stored(panel, "style").indexOf("height") >= 0, "the height is not written: " + TextSelectionFixture.stored(panel, "style"));
            awaitPixel(panel, 120, 30, GREEN, "wider");
            Check.equal(0xFFFFFF, TextSelectionFixture.paint(panel).getRGB(120, 70) & 0xFFFFFF, "but not taller");
        } finally {
            TextSelectionFixture.close(panel);
        }
        panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, " #b{resize:vertical}");
            drag(panel, 92, 52, 122, 82);
            awaitStyle(panel, "height: 90px");
            Check.isFalse(TextSelectionFixture.stored(panel, "style").indexOf("width") >= 0, "the width is not written: " + TextSelectionFixture.stored(panel, "style"));
            awaitPixel(panel, 30, 80, GREEN, "taller");
            Check.equal(0xFFFFFF, TextSelectionFixture.paint(panel).getRGB(120, 30) & 0xFFFFFF, "but not wider");
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * A <code>content-box</code> box gets the dragged border box size minus its padding and border.
     * <p>
     * <code>content-box</code> 상자는 끈 border box 크기에서 padding 과 border 를 뺀 값을 얻는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testContentBoxSizing() throws Exception {
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, " #b{padding:5px;border:2px solid #000}");
            // border box 는 114x74 이고 핸들은 (97..112, 57..72) 이다.
            drag(panel, 105, 65, 141, 91);
            awaitStyle(panel, "width: 136px");
            awaitStyle(panel, "height: 86px");
            awaitPixel(panel, 148, 98, 0x000000, "the right border at the new border box 150 by 100");
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * A press on the handle is a user interface action: the page gets no <code>mousedown</code>; a press elsewhere on the box does.
     * <p>
     * 핸들을 누르는 것은 사용자 인터페이스 동작이다: 페이지는 <code>mousedown</code> 을 받지 않는다. 상자의 다른 곳을 누르면 받는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPressOnTheHandleSendsNoMouseDown() throws Exception {
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, "");
            drag(panel, 92, 52, 100, 60);
            Thread.sleep(300);
            TestSupport.flushEdt();
            Check.equal(0, TextSelectionFixture.stored(panel, "md").length(), "no mousedown for the handle");
            TextSelectionFixture.click(panel, 20, 20, 1, false);
            long deadline = System.currentTimeMillis() + 15000;
            // 상자의 다른 곳을 누르면 mousedown 이 한 번 온다.
            while (!"1".equals(TextSelectionFixture.stored(panel, "md")) && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            Check.equal("1", TextSelectionFixture.stored(panel, "md"), "a press elsewhere on the box is a mousedown");
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * The pointer over the handle shows a resize cursor, and the normal cursor elsewhere.
     * <p>
     * 핸들 위의 포인터는 크기 조절 커서를 보이고 다른 곳에서는 보통 커서를 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCursor() throws Exception {
        final Panel panel = TextSelectionFixture.create(400, 300);
        try {
            open(panel, "");
            TextSelectionFixture.mouse(panel, MouseEvent.MOUSE_MOVED, 92, 52, 0, 0);
            awaitCursor(panel, Cursor.SE_RESIZE_CURSOR);
            TextSelectionFixture.mouse(panel, MouseEvent.MOUSE_MOVED, 300, 200, 0, 0);
            awaitCursor(panel, Cursor.DEFAULT_CURSOR);
        } finally {
            TextSelectionFixture.close(panel);
        }
    }

    /**
     * Waits until the component shows a cursor type.
     * <p>
     * 컴포넌트가 어떤 종류의 커서를 보일 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param type  the cursor type / 커서 종류
     * @throws Exception on failure / 실패 시
     */
    private static void awaitCursor(Panel panel, int type) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        // 호버 조회가 문서 큐에서 끝나 EDT 가 커서를 바꿀 때까지 기다린다.
        while (panel.getCursor().getType() != type && System.currentTimeMillis() < deadline) {
            TestSupport.flushEdt();
            Thread.sleep(20);
        }
        Check.equal(type, panel.getCursor().getType(), "the cursor type");
    }
}
