package org.duckdns.hjow.swingwebpanel.core.frames;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Nested browsing contexts through the real component and real HTTP (plan item P20-02, stage b): an <code>iframe</code> with
 * <code>srcdoc</code> or <code>src</code> shows its own document inside its content box at the right place, size and clip; the
 * <code>load</code> event of the element and the <code>load</code> event and the progress of the parent wait for the frame; a frame that
 * fails shows an error document inside the frame and leaves the component state alone; a page that embeds itself, a page that nests frames
 * too deeply and a page with too many frames stop at the documented limits; frames follow changes of the DOM.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 중첩 브라우징 문맥(계획 P20-02, 단계 b): <code>srcdoc</code> 또는 <code>src</code> 가 있는 <code>iframe</code>
 * 이 자기 문서를 콘텐츠 상자 안의 올바른 위치, 크기, 클립으로 보여 준다. 요소의 <code>load</code> 이벤트와 부모의 <code>load</code> 이벤트 및 진행률이
 * 프레임을 기다린다. 실패한 프레임은 프레임 안에 오류 문서를 보이고 컴포넌트 상태는 그대로 둔다. 자기 자신을 임베드하는 페이지, 너무 깊이 중첩하는 페이지,
 * 프레임이 너무 많은 페이지는 문서화한 한도에서 멈춘다. 프레임은 DOM 변경을 따른다.
 */
public class FrameRenderingTest {

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
    public FrameRenderingTest() {
    }

    /**
     * Serves a page and loads it.
     * <p>
     * 페이지를 제공하고 로드한다.
     *
     * @param panel  the component / 컴포넌트
     * @param server the server, not started yet / 서버(아직 시작 전)
     * @param html   the page / 페이지
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private static void serve(SwingWebPanel panel, TestHttpServer server, String html) throws Exception {
        server.routeFixed("/page", TestHttpServer.Response.html(html));
        server.start();
        panel.setPage(server.url("/page"));
    }

    /**
     * An iframe with srcdoc shows its own document in its content box: size, position and the clip at the edges.
     * <p>
     * srcdoc 가 있는 iframe 은 자기 문서를 콘텐츠 상자 안에 보여 준다: 크기, 위치, 가장자리의 클립.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSrcdocFramePaintsItsDocumentInsideItsBox() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><div style=\"height:20px\"></div>"
                    + "<iframe width=\"100\" height=\"50\" srcdoc=\"&lt;body style='margin:0;background:#f00'&gt;&lt;div style='width:500px;height:500px'&gt;&lt;/div&gt;\"></iframe></body></html>");
            // 프레임의 상자 안(위쪽 20px 아래, 100 x 50)은 프레임 문서의 배경색으로 칠해진다.
            FrameTestSupport.awaitPixel(panel, 10, 30, 0xFF0000, "inside the frame");
            FrameTestSupport.awaitPixel(panel, 99, 69, 0xFF0000, "last pixel of the frame");
            // 상자 밖은 부모의 배경이다(프레임 문서의 큰 내용이 상자 밖으로 새지 않는다).
            FrameTestSupport.awaitPixel(panel, 100, 30, 0xFFFFFF, "right of the frame");
            FrameTestSupport.awaitPixel(panel, 10, 70, 0xFFFFFF, "below the frame");
            FrameTestSupport.awaitPixel(panel, 10, 19, 0xFFFFFF, "above the frame");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An iframe without a size is 300 by 150 pixels.
     * <p>
     * 크기가 없는 iframe 은 300 x 150 픽셀이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultSizeOfAFrameIs300By150() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe srcdoc=\"&lt;body style='margin:0;background:#0a0'&gt;\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 299, 149, 0x00AA00, "last pixel of the default box");
            FrameTestSupport.awaitPixel(panel, 300, 10, 0xFFFFFF, "right of the default box");
            FrameTestSupport.awaitPixel(panel, 10, 150, 0xFFFFFF, "below the default box");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * An iframe with src loads its document from the network with its own request, shows it and leaves the page, the source and the
     * history of the component alone.
     * <p>
     * src 가 있는 iframe 은 자기 요청으로 문서를 네트워크에서 로드해 보여 주며 컴포넌트의 페이지, 원문, 이력은 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSrcFrameLoadsItsOwnDocumentAndLeavesTheComponentAlone() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String html = "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe src=\"/inner.html\" width=\"80\" height=\"40\"></iframe></body></html>";
            server.routeFixed("/inner.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\">inner</body>"));
            serve(panel, server, html);
            FrameTestSupport.awaitPixel(panel, 79, 39, 0x0000FF, "the frame document");
            Check.equal(1, server.count("/inner.html"), "the frame document was requested once");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component is the top-level page");
            Check.equal(html, panel.getContent(), "getContent() returns the source of the top-level page only");
            Check.equal(2, panel.getHistories().size(), "the history holds the blank page and the page, and the frame adds no entry");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The scripts of a frame run in the frame: they see their own document and location, and a frame without a src starts empty.
     * <p>
     * 프레임의 스크립트는 프레임 안에서 실행된다: 자기 문서와 location 을 본다. src 가 없는 프레임은 빈 문서로 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptsOfAFrameSeeTheirOwnDocument() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/inner.html", TestHttpServer.Response.html("<body><script>sessionStorage.setItem('inner', document.URL + '|' "
                    + "+ document.title + '|' + (document.body.getAttribute('id')));</script></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "<title>top</title></head><body id=\"t\">"
                    + "<iframe src=\"/inner.html\" width=\"10\" height=\"10\"></iframe></body></html>");
            FrameTestSupport.awaitStored(panel, "inner", server.url("/inner.html") + "||null");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The load event of the element and of the parent wait for the frame, and the progress of the component stays below 100 while a frame
     * loads.
     * <p>
     * 요소의 load 이벤트와 부모의 load 이벤트는 프레임을 기다리고, 프레임이 로딩하는 동안 컴포넌트의 진행률은 100 미만이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testParentLoadAndProgressWaitForASlowFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.route("/slow.html", new TestHttpServer.Handler() {
                /**
                 * Answers late.
                 * <p>
                 * 늦게 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the response / 응답
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    return TestHttpServer.Response.html("<body style=\"margin:0;background:#f0f\">slow</body>").delay(900);
                }
            });
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe id=\"f\" src=\"/slow.html\" width=\"50\" height=\"30\" onload=\"sessionStorage.setItem('frameLoad','1');"
                    + "sessionStorage.setItem('order', (sessionStorage.getItem('order')||'')+'F')\"></iframe>"
                    + "<script>window.onload=function(){sessionStorage.setItem('order', (sessionStorage.getItem('order')||'')+'P');"
                    + "sessionStorage.setItem('parentLoad','1')};</script></body></html>");
            // 프레임이 로딩하는 동안에는 부모의 load 도 진행률 100 도 아직이다.
            Thread.sleep(400);
            Check.isTrue(FrameTestSupport.stored(panel, "parentLoad") == null, "the parent load event waits for the frame");
            Check.isTrue(panel.getProgress() < 100, "the progress is below 100 while a frame loads");
            FrameTestSupport.awaitStored(panel, "parentLoad", "1");
            // 프레임의 load 가 부모의 load 보다 먼저이다.
            Check.equal("FP", FrameTestSupport.stored(panel, "order"), "the frame load event fires before the load event of the parent");
            FrameTestSupport.awaitProgress(panel, 100);
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A frame that fails shows an error document inside its box; the page, the source and the status of the component do not change.
     * <p>
     * 실패한 프레임은 상자 안에 오류 문서를 보여 준다. 컴포넌트의 페이지, 원문, 상태는 바뀌지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFailedFrameShowsErrorInsideTheFrameOnly() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String html = "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe src=\"/missing.html\" width=\"200\" height=\"100\"></iframe><p style=\"margin:0\">after</p></body></html>";
            serve(panel, server, html);
            FrameTestSupport.awaitProgress(panel, 100);
            // 오류 문서는 흰 바탕에 글자를 그리므로 프레임 상자 안에 흰색이 아닌 화소가 있다.
            long end = System.currentTimeMillis() + 30000;
            int dark = 0;
            // 오류 문서가 그려질 때까지 상자 안의 흰색이 아닌 화소를 센다.
            while (dark == 0 && System.currentTimeMillis() < end) {
                BufferedImage image = FrameTestSupport.snapshot(panel);
                dark = 0;
                // 프레임 상자 안의 모든 화소를 훑는다.
                for (int y = 0; y < 100; y++) {
                    // 한 줄의 화소를 훑는다.
                    for (int x = 0; x < 200; x++) {
                        if ((image.getRGB(x, y) & 0xFFFFFF) != 0xFFFFFF) {
                            dark++;
                        }
                    }
                }
                if (dark == 0) {
                    Thread.sleep(20);
                }
            }
            Check.isTrue(dark > 20, "the error document of the frame is painted inside the frame (" + dark + " pixels)");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(html, panel.getContent(), "the source of the component stays");
            Check.isTrue(!panel.getStatusText().startsWith("Error"), "the status does not report the error of a frame: " + panel.getStatusText());
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page that embeds itself is not loaded again inside itself: the frame shows an error document and no further request is made.
     * <p>
     * 자기 자신을 임베드하는 페이지는 자기 안에 다시 로드되지 않는다: 프레임은 오류 문서를 보이고 더 요청하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageThatEmbedsItselfStopsAtTheFirstFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/self", TestHttpServer.Response.html("<!DOCTYPE html><html><head>" + RESET
                    + "</head><body><iframe src=\"/self\" width=\"200\" height=\"100\"></iframe></body></html>"));
            server.start();
            panel.setPage(server.url("/self"));
            FrameTestSupport.awaitProgress(panel, 100);
            Thread.sleep(300);
            Check.equal(1, server.count("/self"), "the page was requested once: the frame that would load it again is refused");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A chain of pages that nest frames with different addresses stops at the documented depth.
     * <p>
     * 서로 다른 주소로 프레임을 중첩하는 페이지의 사슬은 문서화한 깊이에서 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNestingStopsAtTheDepthLimit() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.route("/loop*", new TestHttpServer.Handler() {
                /**
                 * Answers with a page that embeds the next one.
                 * <p>
                 * 다음 페이지를 임베드하는 페이지로 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the response / 응답
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) {
                    int n = Integer.parseInt(request.query == null ? "0" : request.query.substring(2));
                    return TestHttpServer.Response.html("<!DOCTYPE html><html><body style=\"margin:0\"><iframe src=\"/loop?n=" + (n + 1)
                            + "\" width=\"100\" height=\"60\" style=\"border:0\"></iframe></body></html>");
                }
            });
            server.start();
            panel.setPage(server.url("/loop?n=0"));
            FrameTestSupport.awaitProgress(panel, 100);
            // 최상위(0단계)와 프레임 8단계까지 모두 9번 요청되고 9단계의 프레임은 요청하지 않는다.
            long end = System.currentTimeMillis() + 30000;
            while (server.requests().size() < FrameLimits.MAX_DEPTH + 1 && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Thread.sleep(300);
            Check.equal(FrameLimits.MAX_DEPTH + 1, server.requests().size(), "the top-level page and " + FrameLimits.MAX_DEPTH + " levels of frames");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A page gets at most {@link FrameLimits#MAX_FRAMES_PER_PAGE} frames; further frame elements stay empty.
     * <p>
     * 페이지는 최대 {@link FrameLimits#MAX_FRAMES_PER_PAGE} 개의 프레임을 가진다. 그 이상의 프레임 요소는 비어 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageHasAtMostSixtyFourFrames() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            StringBuilder sb = new StringBuilder("<!DOCTYPE html><html><head>" + RESET + "<style>iframe{float:left}</style></head><body>");
            // 4 x 4 픽셀의 빨간 프레임 70 개를 한 줄로 늘어놓는다.
            for (int i = 0; i < 70; i++) {
                sb.append("<iframe width=\"4\" height=\"4\" srcdoc=\"&lt;body style='margin:0;background:#f00'&gt;\"></iframe>");
            }
            sb.append("</body></html>");
            serve(panel, server, sb.toString());
            // 64 번째 프레임(위치 252..255)은 보이고 65 번째(256..259)는 비어 있다.
            FrameTestSupport.awaitPixel(panel, 253, 1, 0xFF0000, "the 64th frame");
            Thread.sleep(300);
            Check.equal(0xFFFFFF, FrameTestSupport.snapshot(panel).getRGB(257, 1) & 0xFFFFFF, "the 65th frame element has no browsing context");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Frames follow changes of the DOM: a changed <code>src</code> loads the new address, an inserted element gets a frame and a removed
     * element loses it.
     * <p>
     * 프레임은 DOM 변경을 따른다: <code>src</code> 가 바뀌면 새 주소를 로드하고, 삽입된 요소는 프레임을 얻고, 제거된 요소는 프레임을 잃는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFramesFollowTheDom() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/a.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"></body>"));
            server.routeFixed("/b.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe id=\"f\" src=\"/a.html\" width=\"60\" height=\"20\"></iframe>"
                    // load 이벤트는 프레임을 기다리므로 첫 문서가 화면에 나온 뒤에 변경을 시작한다.
                    + "<script>window.onload=function(){setTimeout(function(){"
                    + "var f=document.getElementById('f');"
                    // 먼저 src 를 바꾸고, 새 프레임을 하나 더하고, 그 뒤에 처음 프레임을 제거한다.
                    + "f.src='/b.html';"
                    + "setTimeout(function(){var g=document.createElement('iframe');g.width=60;g.height=20;"
                    + "g.setAttribute('srcdoc',\"<body style='margin:0;background:#0f0'>\");document.body.appendChild(g);"
                    + "setTimeout(function(){document.body.removeChild(f);},1200);},1200);},1200);};</script></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the first document");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x0000FF, "the new src");
            FrameTestSupport.awaitPixel(panel, 10, 30, 0x00FF00, "the inserted frame");
            // 처음 프레임을 제거하면 삽입된 프레임이 위로 올라온다.
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the removed frame is gone");
            Check.equal(1, server.count("/b.html"), "the new address was requested once");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
