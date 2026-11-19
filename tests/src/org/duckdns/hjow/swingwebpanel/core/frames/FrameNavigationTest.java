package org.duckdns.hjow.swingwebpanel.core.frames;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Navigation of nested browsing contexts through the real component (plan item P20-02, stage d): scripts navigate their own frame through
 * <code>location</code>, the frame keeps a local history that <code>history.back()</code> follows while the history of the component is
 * not changed, forms go to the browsing context that their <code>target</code> selects, a fragment link inside a frame scrolls the frame,
 * and <code>_blank</code> and unknown names reuse the current context. All navigations pass through the same navigation policy as the
 * navigation of the top-level document.
 * <p>
 * 실제 컴포넌트로 확인하는 중첩 브라우징 문맥의 이동(계획 P20-02, 단계 d): 스크립트는 <code>location</code> 으로 자기 프레임을 이동시키고, 프레임은 자기
 * 지역 이력을 가지며 <code>history.back()</code> 이 이를 따르되 컴포넌트의 이력은 바뀌지 않고, 폼은 <code>target</code> 이 고르는 브라우징 문맥으로 가며,
 * 프레임 안의 프래그먼트 링크는 프레임을 스크롤하고, <code>_blank</code> 와 모르는 이름은 현재 문맥을 재사용한다. 모든 이동은 최상위 문서의 이동과 같은
 * 이동 정책을 거친다.
 */
public class FrameNavigationTest {

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
    public FrameNavigationTest() {
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
     * A script in a frame that assigns <code>location.href</code> navigates the frame; the component, its source and its history stay.
     * <p>
     * 프레임 안의 스크립트가 <code>location.href</code> 에 대입하면 프레임이 이동한다. 컴포넌트와 그 원문, 이력은 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLocationInAFrameNavigatesTheFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"><script>"
                    + "setTimeout(function(){location.href='/f2.html'},300)</script></body>"));
            server.routeFixed("/f2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f1.html\" width=\"60\" height=\"30\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the frame navigated itself");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(2, panel.getHistories().size(), "the history of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The local history of a frame: <code>history.length</code> counts its navigations, <code>history.back()</code> loads the previous
     * document again and <code>forward()</code> comes back; the history of the component is not involved.
     * <p>
     * 프레임의 지역 이력: <code>history.length</code> 는 프레임의 이동을 세고, <code>history.back()</code> 은 이전 문서를 다시 로드하고,
     * <code>forward()</code> 는 되돌아온다. 컴포넌트의 이력은 관여하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFrameHasItsOwnHistory() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\">"
                    + "<script>sessionStorage.setItem('len1',String(history.length))</script></body>"));
            server.routeFixed("/f2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\">"
                    + "<script>sessionStorage.setItem('len2',String(history.length));setTimeout(function(){history.back()},300)</script></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe id=\"f\" src=\"/f1.html\" width=\"60\" height=\"30\"></iframe>"
                    + "<script>window.onload=function(){setTimeout(function(){document.getElementById('f').contentWindow.location.href='/f2.html'},300)}"
                    + "</script></body></html>");
            // f1 -> f2 (이력 2 칸) -> back() 으로 f1 을 다시 로드한다.
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the frame navigated to the second document");
            FrameTestSupport.awaitStored(panel, "len2", "2");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "history.back() of the frame loaded the first document again");
            Check.equal(2, server.count("/f1.html"), "the first document was requested again");
            Check.equal(2, panel.getHistories().size(), "the history of the component stays");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>history.pushState</code> in a frame changes the address of the frame document and its local history only, and
     * <code>history.back()</code> fires <code>popstate</code> with the state.
     * <p>
     * 프레임 안의 <code>history.pushState</code> 는 프레임 문서의 주소와 지역 이력만 바꾸고, <code>history.back()</code> 은 상태와 함께
     * <code>popstate</code> 를 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPushStateInAFrameStaysInTheFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f.html", TestHttpServer.Response.html("<body><script>"
                    + "window.onpopstate=function(e){sessionStorage.setItem('pop',JSON.stringify(e.state)+'|'+location.hash+'|'+history.length)};"
                    + "history.pushState({n:1},'','#one');sessionStorage.setItem('after',location.hash+'|'+history.length+'|'+JSON.stringify(history.state));"
                    + "setTimeout(function(){history.back()},200)</script></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f.html\" width=\"60\" height=\"30\"></iframe></body></html>");
            FrameTestSupport.awaitStored(panel, "after", "#one|2|{\"n\":1}");
            FrameTestSupport.awaitStored(panel, "pop", "null||2");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(2, panel.getHistories().size(), "the history of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A form in a frame goes to its own frame by default, to the top-level document with <code>target="_top"</code>; a POST sends its body.
     * <p>
     * 프레임 안의 폼은 기본으로 자기 프레임으로, <code>target="_top"</code> 이면 최상위 문서로 간다. POST 는 본문을 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFormTargetsInAFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/form.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\">"
                    + "<form id=\"a\" action=\"/own\" method=\"post\"><input type=\"hidden\" name=\"k\" value=\"v\"></form>"
                    + "<form id=\"b\" action=\"/outer\" method=\"get\" target=\"_top\"><input type=\"hidden\" name=\"q\" value=\"1\"></form>"
                    + "<script>window.onload=function(){setTimeout(function(){document.getElementById(sessionStorage.getItem('which')||'a').submit()},100)}</script></body>"));
            server.routeFixed("/own", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            server.routeFixed("/outer", TestHttpServer.Response.html("<body style=\"margin:0;background:#ff0\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/form.html\" width=\"60\" height=\"30\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the POST form answered inside the frame");
            List<TestHttpServer.Request> all = server.requests();
            String body = null;
            // 프레임이 보낸 POST 요청의 본문을 찾는다.
            for (int i = 0; i < all.size(); i++) {
                if ("/own".equals(all.get(i).path)) {
                    body = all.get(i).method + ":" + all.get(i).bodyText();
                }
            }
            Check.equal("POST:k=v", body, "the form body of the POST");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(2, panel.getHistories().size(), "the history of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A form with <code>target="_top"</code> in a frame navigates the top-level document (a GET with the query).
     * <p>
     * 프레임 안의 <code>target="_top"</code> 폼은 최상위 문서를 이동시킨다(쿼리가 붙은 GET).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFormTargetTopNavigatesTheComponent() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/form.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\">"
                    + "<form id=\"b\" action=\"/outer\" method=\"get\" target=\"_top\"><input type=\"hidden\" name=\"q\" value=\"1\"></form>"
                    + "<script>window.onload=function(){setTimeout(function(){document.getElementById('b').submit()},100)}</script></body>"));
            server.routeFixed("/outer", TestHttpServer.Response.html("<body style=\"margin:0;background:#ff0\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/form.html\" width=\"60\" height=\"30\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 300, 250, 0xFFFF00, "the top-level document answered");
            Check.equal(server.url("/outer?q=1"), panel.getPage(), "the top-level document was navigated with the query");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A fragment link inside a frame scrolls the frame to the target and does not request the document again; the frame keeps its entries
     * of the local history.
     * <p>
     * 프레임 안의 프래그먼트 링크는 프레임을 대상으로 스크롤하며 문서를 다시 요청하지 않는다. 프레임은 지역 이력의 항목을 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testFragmentLinkScrollsTheFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/tall.html", TestHttpServer.Response.html("<body style=\"margin:0\"><a href=\"#low\" style=\"display:block;height:30px;background:#f00\">go</a>"
                    + "<div style=\"height:400px\"></div><div id=\"low\" style=\"height:200px;background:#00f\"></div></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/tall.html\" width=\"60\" height=\"50\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 40, 22, 0xFF0000, "the link at the top of the frame");
            FrameTestSupport.click(panel, 40, 22);
            // #low 는 문서의 y=430 에 있다. 프레임이 그 위치로 스크롤하므로 프레임의 맨 위 화소가 파란색이 된다.
            FrameTestSupport.awaitPixel(panel, 40, 22, 0x0000FF, "the frame scrolled to the target of the fragment");
            Check.equal(1, server.count("/tall.html"), "the document was not requested again");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>_blank</code> and the name of a frame that does not exist cannot open a window: the current context is reused, whether the
     * link is in the top-level document or in a frame.
     * <p>
     * <code>_blank</code> 와 없는 프레임 이름은 창을 열 수 없다: 링크가 최상위 문서에 있든 프레임에 있든 현재 문맥을 재사용한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlankAndUnknownNamesReuseTheCurrentContext() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0\"><a href=\"/f2.html\" target=\"_blank\" "
                    + "style=\"display:block;height:30px;background:#f00\">go</a></body>"));
            server.routeFixed("/f2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"><a href=\"/f3.html\" target=\"nowhere\" "
                    + "style=\"display:block;height:30px;background:#ff0\">go</a></body>"));
            server.routeFixed("/f3.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#00f\"></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f1.html\" width=\"100\" height=\"50\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 80, 10, 0xFF0000, "the first document");
            FrameTestSupport.click(panel, 80, 10);
            FrameTestSupport.awaitPixel(panel, 80, 10, 0xFFFF00, "_blank reused the frame");
            FrameTestSupport.click(panel, 80, 10);
            FrameTestSupport.awaitPixel(panel, 80, 10, 0x0000FF, "an unknown name reused the frame");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Navigation to a scheme that is not allowed is refused for frames too: the frame shows its error document and nothing is requested.
     * <p>
     * 허용되지 않은 스킴으로의 이동은 프레임에서도 거부된다: 프레임은 오류 문서를 보이고 아무것도 요청하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedSchemesStayBlockedInFrames() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0\"><a href=\"file:///etc/hosts\" "
                    + "style=\"display:block;height:30px;background:#f00\">go</a><script>"
                    + "setTimeout(function(){location.href='javascript:alert(1)';sessionStorage.setItem('after','done')},300)</script></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f1.html\" width=\"100\" height=\"50\"></iframe>"
                    + "<iframe src=\"file:///etc/hosts\" width=\"100\" height=\"50\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 80, 10, 0xFF0000, "the first frame");
            FrameTestSupport.click(panel, 80, 10);
            FrameTestSupport.awaitStored(panel, "after", "done");
            Thread.sleep(300);
            // 링크도 location 도 frame 을 이동시키지 못해 첫 프레임은 그대로이다.
            FrameTestSupport.awaitPixel(panel, 80, 10, 0xFF0000, "the frame stays at its document");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
            Check.equal(1, server.count("/f1.html"), "no further request");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>window.open</code> selects an existing browsing context like a link target does: the name of a frame navigates that frame and
     * returns its window, an empty address returns the window without navigating, <code>_self</code>, <code>_parent</code> and
     * <code>_top</code> select the context, and a name that would need a new window (<code>_blank</code>, an unknown name) gives
     * <code>null</code> and requests nothing, because a component has no windows.
     * <p>
     * <code>window.open</code> 은 링크의 대상처럼 기존 브라우징 문맥을 고른다: 프레임의 이름은 그 프레임을 이동시키고 그 window 를 반환하고, 빈 주소는
     * 이동 없이 window 를 반환하고, <code>_self</code>, <code>_parent</code>, <code>_top</code> 은 그 문맥을 고르며, 새 창이 필요한 이름
     * (<code>_blank</code>, 모르는 이름)은 컴포넌트에 창이 없으므로 <code>null</code> 을 주고 아무것도 요청하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWindowOpenSelectsExistingContextsOnly() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"></body>"));
            server.routeFixed("/f2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"><script>"
                    + "sessionStorage.setItem('inner',[parent.open('','_self')===parent,window.open('','_parent')===parent,window.open('','_top')===top,"
                    + "window.open('','_self')===window].join('|'))</script></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe name=\"main\" src=\"/f1.html\" width=\"60\" height=\"30\"></iframe>"
                    + "<script>window.onload=function(){setTimeout(function(){"
                    + "var w=window.open('/f2.html','main');var same=window.open('','main');"
                    + "sessionStorage.setItem('open',[w===frames[0],same===frames[0],window.open('/f3.html','_blank')===null,"
                    + "window.open('/f4.html','nowhere')===null,window.open('','_self')===window].join('|'))},0)}</script></body></html>");
            FrameTestSupport.awaitStored(panel, "open", "true|true|true|true|true");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the frame navigated by name");
            FrameTestSupport.awaitStored(panel, "inner", "true|true|true|true");
            Check.equal(0, server.count("/f3.html"), "no request for a new window");
            Check.equal(0, server.count("/f4.html"), "no request for an unknown name");
            Check.equal(server.url("/page"), panel.getPage(), "the page of the component stays");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The navigations of a frame leave the history of the component alone, and when the component moves on and comes back, the frame is built
     * again from the markup of the page (the state that the frame navigated to is not restored).
     * <p>
     * 프레임의 이동은 컴포넌트의 이력을 건드리지 않고, 컴포넌트가 다른 곳으로 갔다가 돌아오면 프레임은 페이지의 마크업에서 다시 만들어진다(프레임이
     * 이동해 간 상태는 복원되지 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testComponentHistoryIgnoresFrameNavigationAndBackRebuildsTheFrame() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f1.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"></body>"));
            server.routeFixed("/f2.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            server.routeFixed("/other", TestHttpServer.Response.html("<body>other</body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f1.html\" width=\"60\" height=\"30\"></iframe>"
                    + "<script>window.onload=function(){setTimeout(function(){frames[0].location.href='/f2.html'},100)}</script></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the frame navigated itself");
            Check.equal(2, panel.getHistories().size(), "the frame navigation added no entry to the history of the component");
            panel.setPage(server.url("/other"));
            long end = System.currentTimeMillis() + 30000;
            // 컴포넌트가 다른 페이지로 옮겨 갈 때까지 기다린다.
            while (!server.url("/other").equals(panel.getPage()) && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.equal(server.url("/other"), panel.getPage(), "the component moved on");
            panel.back();
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the frame navigates itself again after back()");
            Check.equal(2, server.count("/f1.html"), "the frame was built again from the markup (its first document was requested again)");
            Check.equal(server.url("/page"), panel.getPage(), "back() returned to the page");
            Check.equal(3, panel.getHistories().size(), "the history of the component has the three pages only");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * <code>refresh(true)</code> bypasses the caches for the documents of frames too, while <code>refresh(false)</code> lets a frame
     * document that the cache holds (<code>max-age</code>) stay in the cache.
     * <p>
     * <code>refresh(true)</code> 는 프레임의 문서에도 캐시를 우회하지만, <code>refresh(false)</code> 는 캐시가 가진(<code>max-age</code>) 프레임 문서를
     * 캐시에 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testForcedRefreshReloadsFramesFromTheNetwork() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"></body>").header("Cache-Control", "max-age=3600"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body><iframe src=\"/f.html\" width=\"60\" height=\"30\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the frame document");
            Check.equal(1, server.count("/f.html"), "the frame document was requested once");
            panel.refresh(false);
            awaitCount(server, "/page", 2);
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the frame document after the plain refresh");
            Thread.sleep(500);
            Check.equal(1, server.count("/f.html"), "the plain refresh took the frame document from the cache");
            panel.refresh(true);
            awaitCount(server, "/f.html", 2);
            Check.equal(2, server.count("/f.html"), "the forced refresh asked the server for the frame document again");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Waits until the server has seen a number of requests for a path.
     * <p>
     * 서버가 한 경로의 요청을 일정 수 받을 때까지 기다린다.
     *
     * @param server the server / 서버
     * @param path   the path / 경로
     * @param count  the number / 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitCount(TestHttpServer server, String path, int count) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 요청이 그만큼 도착할 때까지 짧게 쉬며 확인한다.
        while (server.count(path) < count && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.isTrue(server.count(path) >= count, "the server received " + count + " requests for " + path + " (got " + server.count(path) + ")");
    }
}
