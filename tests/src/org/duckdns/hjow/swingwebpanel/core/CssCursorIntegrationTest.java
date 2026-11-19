package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests of the CSS <code>cursor</code> property (plan item P10-08, CSS UI Level 3, https://www.w3.org/TR/css-ui-3/#cursor) through the component: the mouse cursor of the
 * component follows the keyword of the box under the pointer (the hand for <code>pointer</code>, the wait cursor for <code>wait</code>, a resize cursor for
 * <code>e-resize</code>, the default arrow for a keyword that Java 6 has no cursor for), the property is inherited, <code>auto</code> keeps the link hand and the
 * text cursor, an author's <code>cursor: default</code> overrides the hand of a link, and the mapping of the keywords is checked without a display.
 * <p>
 * CSS <code>cursor</code> 속성을 컴포넌트를 통해 확인하는 시험(계획 P10-08, CSS UI Level 3): 컴포넌트의 마우스 커서는 포인터 아래 상자의 키워드를 따른다(<code>pointer</code> 는 손,
 * <code>wait</code> 는 기다림 커서, <code>e-resize</code> 는 크기 조정 커서, Java 6 에 커서가 없는 키워드는 기본 화살표). 속성은 상속되고, <code>auto</code> 는 링크의 손과
 * 텍스트 커서를 유지하며, 작성자의 <code>cursor: default</code> 는 링크의 손을 덮고, 키워드 대응은 디스플레이 없이도 확인한다.
 */
public class CssCursorIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public CssCursorIntegrationTest() {
    }

    /**
     * The mapping of the keywords to the cursors that Java has, needing no display.
     * <p>
     * 디스플레이가 필요 없는, 키워드를 Java 의 커서로 대응시키는 시험.
     */
    public void testKeywordMapping() {
        Check.equal(Cursor.DEFAULT_CURSOR, CssCursors.cursorFor("auto", false, false).getType(), "auto over nothing: the arrow");
        Check.equal(Cursor.HAND_CURSOR, CssCursors.cursorFor("auto", true, false).getType(), "auto over a link: the hand");
        Check.equal(Cursor.TEXT_CURSOR, CssCursors.cursorFor("auto", false, true).getType(), "auto over text: the text cursor");
        Check.equal(Cursor.HAND_CURSOR, CssCursors.cursorFor("pointer", false, false).getType(), "pointer");
        Check.equal(Cursor.DEFAULT_CURSOR, CssCursors.cursorFor("default", true, true).getType(), "default overrides the hand and the text cursor");
        Check.equal(Cursor.TEXT_CURSOR, CssCursors.cursorFor("text", true, false).getType(), "text");
        Check.equal(Cursor.WAIT_CURSOR, CssCursors.cursorFor("wait", false, false).getType(), "wait");
        Check.equal(Cursor.WAIT_CURSOR, CssCursors.cursorFor("progress", false, false).getType(), "progress");
        Check.equal(Cursor.CROSSHAIR_CURSOR, CssCursors.cursorFor("crosshair", false, false).getType(), "crosshair");
        Check.equal(Cursor.MOVE_CURSOR, CssCursors.cursorFor("move", false, false).getType(), "move");
        Check.equal(Cursor.E_RESIZE_CURSOR, CssCursors.cursorFor("ew-resize", false, false).getType(), "ew-resize");
        Check.equal(Cursor.N_RESIZE_CURSOR, CssCursors.cursorFor("row-resize", false, false).getType(), "row-resize");
        Check.equal(Cursor.NE_RESIZE_CURSOR, CssCursors.cursorFor("nesw-resize", false, false).getType(), "nesw-resize");
        Check.equal(Cursor.SE_RESIZE_CURSOR, CssCursors.cursorFor("se-resize", false, false).getType(), "se-resize");
        Check.equal(Cursor.DEFAULT_CURSOR, CssCursors.cursorFor("not-allowed", false, false).getType(), "a keyword without a Java cursor: the arrow");
        Check.equal(Cursor.DEFAULT_CURSOR, CssCursors.cursorFor("help", true, false).getType(), "help: the arrow, not the hand of the link");
        Check.notNull(CssCursors.cursorFor("none", false, false), "none gives a cursor (transparent, or the arrow without a display)");
    }

    /**
     * The keyword of a style: the last item of a list (the images are not used), inherited.
     * <p>
     * 스타일의 키워드: 목록이면 마지막 항목(이미지는 쓰지 않는다), 상속된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testKeywordOfTheStyle() throws Exception {
        org.duckdns.hjow.swingwebpanel.layout.LayoutResult r = org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport.layout("<div id=a style='cursor:url(x.png) 2 3, wait'><p id=b>x</p></div><div id=c></div>", 300);
        ComputedStyle a = org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport.box(r, "a").getStyle();
        Check.equal("wait", CssCursors.nameOf(a), "the keyword after the image");
        Check.equal("wait", CssCursors.nameOf(org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport.box(r, "b").getStyle()), "inherited by the child");
        Check.equal("auto", CssCursors.nameOf(org.duckdns.hjow.swingwebpanel.layout.LayoutTestSupport.box(r, "c").getStyle()), "the initial value");
        Check.equal("auto", CssCursors.nameOf(null), "no style");
    }

    /**
     * Creates the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static SwingWebPanel newPanel() throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(400, 400);
            }
        });
        return result[0];
    }

    /**
     * Moves the pointer to a point and waits until the cursor of the component has the expected type (the hover lookup runs on the document queue), then returns the type.
     * <p>
     * 포인터를 한 점으로 옮기고 컴포넌트의 커서가 기대한 종류가 될 때까지 기다린 뒤(호버 조회는 문서 큐에서 돈다) 종류를 반환한다.
     *
     * @param p        the component / 컴포넌트
     * @param y        the y coordinate of the pointer / 포인터의 y 좌표
     * @param expected the expected type / 기대한 종류
     * @return the type at the end of the wait / 기다린 뒤의 종류
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static int cursorAt(final SwingWebPanel p, final int y, int expected) throws Exception {
        long end = System.currentTimeMillis() + 10000;
        int type = -1;
        // 기대한 종류가 될 때까지 포인터를 움직여 보며 기다린다(처음 조회는 페이지가 그려지기 전일 수 있다).
        while (System.currentTimeMillis() < end) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Delivers a pointer move through the component.
                 * <p>
                 * 컴포넌트를 통해 포인터 이동을 전달한다.
                 */
                public void run() {
                    p.dispatchEvent(new MouseEvent(p, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 5, y, 0, false));
                }
            });
            Thread.sleep(60);
            final int[] t = new int[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Reads the cursor on the EDT.
                 * <p>
                 * EDT 에서 커서를 읽는다.
                 */
                public void run() {
                    t[0] = p.getCursor().getType();
                }
            });
            type = t[0];
            if (type == expected) {
                return type;
            }
        }
        return type;
    }

    /**
     * The cursor of the component over boxes with different <code>cursor</code> values: the keyword of the box wins over the link hand, an inheriting child keeps it, and
     * the default is restored when the pointer leaves.
     * <p>
     * 서로 다른 <code>cursor</code> 값을 가진 상자 위에서 컴포넌트의 커서: 상자의 키워드가 링크의 손을 이기고, 상속하는 자식은 그것을 유지하며, 포인터가 떠나면 기본으로 돌아간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testComponentCursor() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel p = newPanel();
        try {
            String html = "<!doctype html><html><body style='margin:0;font:10px monospace'>"
                    + "<div style='height:30px;cursor:pointer'>pointer</div>"
                    + "<div style='height:30px;cursor:wait'><span>wait child</span></div>"
                    + "<div style='height:30px;cursor:e-resize'>resize</div>"
                    + "<a href='/x' style='display:block;height:30px;cursor:default'>link with default</a>"
                    + "<a href='/y' style='display:block;height:30px'>link</a>"
                    + "<div style='height:30px;cursor:not-allowed'>no</div>"
                    + "<div style='height:30px'>plain text</div>"
                    + "<div style='height:30px;cursor:auto'>auto</div>"
                    + "</body></html>";
            server.routeFixed("/page", TestHttpServer.Response.html(html));
            server.start();
            p.setPage(server.url("/page"));
            long end = System.currentTimeMillis() + 60000;
            // 페이지가 확정될 때까지 기다린다.
            while (!server.url("/page").equals(p.getPage()) && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            Thread.sleep(300);
            Check.equal(Cursor.HAND_CURSOR, cursorAt(p, 15, Cursor.HAND_CURSOR), "cursor: pointer on a box that is no link");
            Check.equal(Cursor.WAIT_CURSOR, cursorAt(p, 45, Cursor.WAIT_CURSOR), "cursor: wait on the box and its inline child");
            Check.equal(Cursor.E_RESIZE_CURSOR, cursorAt(p, 75, Cursor.E_RESIZE_CURSOR), "cursor: e-resize");
            Check.equal(Cursor.DEFAULT_CURSOR, cursorAt(p, 105, Cursor.DEFAULT_CURSOR), "cursor: default beats the hand of the link");
            Check.equal(Cursor.HAND_CURSOR, cursorAt(p, 135, Cursor.HAND_CURSOR), "a link without a cursor value: the hand (auto)");
            Check.equal(Cursor.DEFAULT_CURSOR, cursorAt(p, 165, Cursor.DEFAULT_CURSOR), "not-allowed: the arrow (Java 6 has no such cursor)");
            Check.equal(Cursor.TEXT_CURSOR, cursorAt(p, 185, Cursor.TEXT_CURSOR), "no cursor value over text: the text cursor");
            Check.equal(Cursor.TEXT_CURSOR, cursorAt(p, 215, Cursor.TEXT_CURSOR), "cursor: auto over text: the text cursor");
            Check.equal(Cursor.DEFAULT_CURSOR, cursorAt(p, 205, Cursor.DEFAULT_CURSOR), "below the text line (inside the box) there is no text: the arrow");
        } finally {
            p.dispose();
            server.stop();
        }
    }
}
