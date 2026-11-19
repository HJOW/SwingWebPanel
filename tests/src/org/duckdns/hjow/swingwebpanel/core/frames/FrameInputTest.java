package org.duckdns.hjow.swingwebpanel.core.frames;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Input inside nested browsing contexts through the real component (plan item P20-02, stage b/d): mouse events and clicks reach the document
 * of the frame under the pointer with the coordinates of that document, links and <code>target</code> attributes navigate the frame that they
 * select (and only that frame), the wheel and the overlay scrollbars scroll the frame, hover styles work inside frames and the keyboard goes
 * to the frame that was clicked last.
 * <p>
 * 실제 컴포넌트로 확인하는 중첩 브라우징 문맥 안의 입력(계획 P20-02, 단계 b/d): 마우스 이벤트와 클릭은 포인터 아래 프레임의 문서에 그 문서의 좌표로
 * 전달되고, 링크와 <code>target</code> 속성은 그것이 고르는 프레임(그 프레임만)을 이동시키며, 휠과 오버레이 스크롤바는 프레임을 스크롤하고, 호버 스타일은 프레임
 * 안에서 동작하며, 키보드는 마지막으로 클릭한 프레임으로 간다.
 */
public class FrameInputTest {

    /**
     * The style that removes margins and borders so that the pixels of the tests have known positions.
     * <p>
     * 시험 화소의 위치를 알 수 있도록 여백과 경계선을 없애는 스타일.
     */
    private static final String RESET = "<style>html,body{margin:0;padding:0;background:#fff}iframe{border:0;display:block}</style>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FrameInputTest() {
    }

    /**
     * Escapes a document for the value of a <code>srcdoc</code> attribute.
     * <p>
     * 문서를 <code>srcdoc</code> 속성 값으로 쓰도록 이스케이프한다.
     *
     * @param html the document / 문서
     * @return the attribute text / 속성 텍스트
     */
    private static String attr(String html) {
        return html.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Serves a page and loads it.
     * <p>
     * 페이지를 제공하고 로드한다.
     *
     * @param panel  the component / 컴포넌트
     * @param server the server / 서버
     * @param html   the page / 페이지
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private static void serve(SwingWebPanel panel, TestHttpServer server, String html) throws Exception {
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.start();
        panel.setPage(server.url("/page"));
    }

    /**
     * A click inside a frame is an event of the document of the frame, with coordinates relative to the viewport of the frame.
     * <p>
     * 프레임 안의 클릭은 프레임의 문서의 이벤트이며 좌표는 프레임 뷰포트 기준이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClickInsideAFrameIsAnEventOfThatDocument() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<body style='margin:0;background:#eee'><div style='height:500px'></div><script>"
                    + "document.addEventListener('click',function(e){sessionStorage.setItem('clicked',e.clientX+','+e.clientY+','+document.URL)})"
                    + "</script></body>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><div style=\"height:20px\"></div>"
                    + "<iframe width=\"100\" height=\"60\" srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<script>document.addEventListener('click',function(){sessionStorage.setItem('parentClick','1')})</script></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 30, 0xEEEEEE, "the frame is shown");
            FrameTestSupport.click(panel, 30, 50);
            FrameTestSupport.awaitStored(panel, "clicked", "30,30,about:srcdoc");
            Thread.sleep(200);
            Check.isTrue(FrameTestSupport.stored(panel, "parentClick") == null, "a click inside the frame is not an event of the parent document");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A link in a frame navigates the frame only: the page, the source and the history of the component stay.
     * <p>
     * 프레임 안의 링크는 프레임만 이동시킨다: 컴포넌트의 페이지, 원문, 이력은 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLinkInAFrameNavigatesTheFrameOnly() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\">"
                    + "<a href=\"/f2.html\" style=\"display:block;height:30px;background:#f00\">go</a></body>"));
            server.routeFixed("/f2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f1.html\" width=\"120\" height=\"60\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 100, 50, 0x0000FF, "the first document");
            FrameTestSupport.click(panel, 10, 10);
            FrameTestSupport.awaitPixel(panel, 100, 50, 0x00FF00, "the frame navigated");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(2, panel.getHistories().size(), "the history of the component does not change");
            Check.equal(1, server.count("/f2.html"), "the new document was requested once");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A link with <code>target="_top"</code> in a frame navigates the top-level document.
     * <p>
     * 프레임 안의 <code>target="_top"</code> 링크는 최상위 문서를 이동시킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTargetTopNavigatesTheComponent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0\"><a href=\"/top2.html\" target=\"_top\" "
                    + "style=\"display:block;height:30px;background:#f00\">out</a></body>"));
            server.routeFixed("/top2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f1.html\" width=\"120\" height=\"60\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 100, 20, 0xFF0000, "the link of the frame");
            FrameTestSupport.click(panel, 100, 20);
            FrameTestSupport.awaitPixel(panel, 300, 250, 0x00FF00, "the top-level document navigated");
            Check.equal(server.url("/top2.html"), panel.getPage(), "the page of the component is the new page");
            Check.equal(3, panel.getHistories().size(), "the top-level navigation is a history entry");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A link of the parent with the name of a frame as its target navigates that frame.
     * <p>
     * 부모의 링크가 프레임의 이름을 대상으로 가지면 그 프레임을 이동시킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTargetNamesAFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/x.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<a href=\"/x.html\" target=\"out\" style=\"display:block;height:20px;background:#f00\">open</a>"
                    + "<iframe name=\"out\" width=\"100\" height=\"40\" srcdoc=\"" + attr("<body style='margin:0;background:#ccc'>") + "\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 30, 0xCCCCCC, "the frame");
            FrameTestSupport.click(panel, 10, 10);
            FrameTestSupport.awaitPixel(panel, 10, 30, 0x0000FF, "the named frame navigated");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The mouse wheel scrolls the frame under the pointer when it can scroll in that direction; the position is kept inside the document of the
     * frame.
     * <p>
     * 마우스 휠은 그 방향으로 스크롤할 수 있으면 포인터 아래 프레임을 스크롤한다. 위치는 프레임 문서 안으로 제한된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWheelScrollsTheFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<body style='margin:0'><div style='height:60px;background:#f00'></div><div style='height:2000px;background:#00f'></div></body>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe width=\"100\" height=\"50\" srcdoc=\"" + attr(inner)
                    + "\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the top of the frame document");
            FrameTestSupport.wheel(panel, 10, 10, 5);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x0000FF, "the frame scrolled by five lines");
            FrameTestSupport.wheel(panel, 10, 10, -20);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the frame scrolled back to the top and stops there");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The thumb of the overlay scrollbar of a frame can be dragged; the track scrolls by a page.
     * <p>
     * 프레임의 오버레이 스크롤바의 썸은 끌 수 있고, 트랙은 한 쪽씩 스크롤한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScrollbarThumbCanBeDragged() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<body style='margin:0'><div style='height:440px;background:#f00'></div><div style='height:60px;background:#00f'></div></body>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe width=\"100\" height=\"50\" srcdoc=\"" + attr(inner)
                    + "\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the top of the frame document");
            // 썸은 트랙의 위쪽 16px 이고 트랙 끝까지 끌면 문서의 끝까지 스크롤된다.
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_MOVED, 95, 5, 0);
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, 95, 5, 1);
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_DRAGGED, 95, 40, 0);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x0000FF, "the frame scrolled to the end with the thumb");
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, 95, 40, 1);
            // 트랙 위쪽을 누르면 한 쪽(45px) 위로 스크롤하므로 450 에서 405 가 되어 위쪽 화소가 빨간 영역(440 미만)으로 돌아간다.
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, 95, 2, 1);
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, 95, 2, 1);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the track scrolls a page towards the pointer");
            FrameTestSupport.awaitPixel(panel, 10, 48, 0x0000FF, "the end of the document is still at the bottom");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A hover style inside a frame applies while the pointer is over the element and ends when it leaves.
     * <p>
     * 프레임 안의 호버 스타일은 포인터가 요소 위에 있는 동안 적용되고 벗어나면 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHoverStyleWorksInsideAFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<style>body{margin:0}#h{width:50px;height:30px;background:#f00}#h:hover{background:#0f0}</style><div id='h'></div>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe width=\"100\" height=\"60\" srcdoc=\"" + attr(inner)
                    + "\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "not hovered");
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_MOVED, 10, 10, 0);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "hovered");
            FrameTestSupport.mouse(panel, java.awt.event.MouseEvent.MOUSE_MOVED, 90, 50, 0);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "not hovered again");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Key events go to the document of the frame that was clicked last, and back to the top-level document after a click outside.
     * <p>
     * 키 이벤트는 마지막으로 클릭한 프레임의 문서로 가고, 바깥을 클릭한 뒤에는 다시 최상위 문서로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testKeyboardGoesToTheFrameThatWasClicked() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<body style='margin:0;background:#ddd'><script>document.addEventListener('keydown',function(e){"
                    + "sessionStorage.setItem('frameKey',e.key)})</script></body>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe width=\"100\" height=\"60\" srcdoc=\"" + attr(inner)
                    + "\"></iframe><script>document.addEventListener('keydown',function(e){sessionStorage.setItem('topKey',e.key)})</script>"
                    + "</body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xDDDDDD, "the frame");
            FrameTestSupport.click(panel, 10, 10);
            FrameTestSupport.key(panel, java.awt.event.KeyEvent.VK_A, 'a');
            FrameTestSupport.awaitStored(panel, "frameKey", "a");
            Check.isTrue(FrameTestSupport.stored(panel, "topKey") == null, "the top-level document does not get the key");
            // 프레임 밖(최상위 문서의 빈 영역)을 클릭하면 키보드는 최상위 문서로 돌아간다.
            FrameTestSupport.click(panel, 300, 200);
            FrameTestSupport.key(panel, java.awt.event.KeyEvent.VK_B, 'b');
            FrameTestSupport.awaitStored(panel, "topKey", "b");
            Check.equal("a", FrameTestSupport.stored(panel, "frameKey"), "the frame does not get the second key");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
