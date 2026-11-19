package org.duckdns.hjow.swingwebpanel.core.frames;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The <code>sandbox</code> attribute of <code>iframe</code> through the real component (plan item P20-02, stage e; HTML5 REC 2014-10-28
 * 4.7.2): without <code>allow-scripts</code> no script and no event handler attribute runs, without <code>allow-same-origin</code> the
 * document has an opaque origin (so it cannot reach its parent, its storage or its cookies, and the parent cannot reach it),
 * <code>allow-forms</code>, <code>allow-popups</code> and <code>allow-top-navigation</code> gate forms, new contexts and the navigation of
 * the top-level page, the flags accumulate through nested frames, a <code>srcdoc</code> document inherits the origin of its creator unless
 * the sandbox makes it opaque, and the attribute is read when the frame navigates.
 * <p>
 * 실제 컴포넌트로 확인하는 <code>iframe</code> 의 <code>sandbox</code> 속성(계획 P20-02, 단계 e. HTML5 REC 2014-10-28 4.7.2): <code>allow-scripts</code> 가
 * 없으면 스크립트도 이벤트 핸들러 속성도 실행되지 않고, <code>allow-same-origin</code> 이 없으면 문서의 오리진이 불투명하여(부모, 저장소, 쿠키에 닿지 못하고
 * 부모도 그 문서에 닿지 못한다), <code>allow-forms</code>, <code>allow-popups</code>, <code>allow-top-navigation</code> 이 폼, 새 문맥, 최상위 페이지
 * 이동을 가로막고, 플래그는 중첩된 프레임을 거치며 누적되고, <code>srcdoc</code> 문서는 샌드박스가 불투명하게 하지 않는 한 생성자의 오리진을 물려받고,
 * 속성은 프레임이 이동할 때 읽힌다.
 */
public class FrameSandboxTest {

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
    public FrameSandboxTest() {
    }

    /**
     * Escapes a document for the value of an attribute.
     * <p>
     * 문서를 속성 값으로 쓰도록 이스케이프한다.
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
     * Without <code>allow-scripts</code> neither a <code>script</code> element nor an event handler attribute runs; with it they do. The
     * frames show it by turning their own background green when their scripts run.
     * <p>
     * <code>allow-scripts</code> 가 없으면 <code>script</code> 요소도 이벤트 핸들러 속성도 실행되지 않고, 있으면 실행된다. 프레임은 자기 스크립트가
     * 실행되면 배경을 초록으로 바꾸어 이를 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptsRunOnlyWithAllowScripts() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String paint = "<body style=\"margin:0;background:#f00\"><script>document.body.style.background='#0f0'</script>";
            String handler = "<body style=\"margin:0;background:#f00\" onload=\"document.body.style.background='#0f0'\">";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe sandbox=\"\" width=\"20\" height=\"20\" srcdoc=\"" + attr(paint) + "\"></iframe>"
                    + "<iframe sandbox=\"\" width=\"20\" height=\"20\" srcdoc=\"" + attr(handler) + "\"></iframe>"
                    + "<iframe sandbox=\"allow-scripts\" width=\"20\" height=\"20\" srcdoc=\"" + attr(paint) + "\"></iframe>"
                    + "<iframe sandbox=\"allow-scripts\" width=\"20\" height=\"20\" srcdoc=\"" + attr(handler) + "\"></iframe>"
                    + "<iframe width=\"20\" height=\"20\" srcdoc=\"" + attr(paint) + "\"></iframe></body></html>");
            // 아래 둘(allow-scripts 가 있는 프레임)과 마지막(샌드박스 없음)이 초록이 될 때까지 기다린 뒤 위 둘이 빨강인지 본다.
            FrameTestSupport.awaitPixel(panel, 10, 50, 0x00FF00, "a script runs with allow-scripts");
            FrameTestSupport.awaitPixel(panel, 10, 70, 0x00FF00, "an event handler attribute runs with allow-scripts");
            FrameTestSupport.awaitPixel(panel, 10, 90, 0x00FF00, "a script runs without a sandbox");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "no script runs in a sandbox without allow-scripts");
            FrameTestSupport.awaitPixel(panel, 10, 30, 0xFF0000, "no event handler runs in a sandbox without allow-scripts");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A sandboxed document without <code>allow-same-origin</code> has an opaque origin: its script cannot read the parent, its cookies or its
     * storage (each throws <code>SecurityError</code>), but it can post a message, which arrives with the origin <code>null</code>; the parent
     * cannot reach into it either. The same document with <code>allow-same-origin</code> keeps the origin of its creator and may use the parent
     * and the storage.
     * <p>
     * <code>allow-same-origin</code> 이 없는 샌드박스 문서는 불투명 오리진을 가진다: 그 스크립트는 부모, 쿠키, 저장소를 읽지 못하지만(각각
     * <code>SecurityError</code>) 메시지는 보낼 수 있고 그 메시지는 오리진 <code>null</code> 로 도착한다. 부모도 그 안에 닿지 못한다. 같은 문서에
     * <code>allow-same-origin</code> 이 있으면 생성자의 오리진을 유지하고 부모와 저장소를 쓸 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSandboxWithoutSameOriginIsOpaque() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>function t(f){try{f();return 'ok'}catch(e){return e.name}}"
                    + "parent.postMessage([t(function(){return parent.document}),t(function(){return localStorage.length}),"
                    + "t(function(){return document.cookie})].join('|'),'*')</script>";
            String page = "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe id=\"a\" sandbox=\"allow-scripts\" width=\"20\" height=\"20\" srcdoc=\"" + attr(inner) + "\"></iframe>"
                    + "<script>window.addEventListener('message',function(e){sessionStorage.setItem('msg',e.data+'@'+e.origin)});"
                    + "window.onload=function(){var a=document.getElementById('a');"
                    + "function t(f){try{f();return 'ok'}catch(e){return e.name}}"
                    + "sessionStorage.setItem('outer',[a.contentDocument===null,t(function(){return a.contentWindow.document}),"
                    + "t(function(){return a.contentWindow.localStorage}),typeof a.contentWindow.postMessage].join('|'))}</script></body></html>";
            serve(panel, server, page);
            FrameTestSupport.awaitStored(panel, "msg", "SecurityError|SecurityError|SecurityError@null");
            FrameTestSupport.awaitStored(panel, "outer", "true|SecurityError|SecurityError|function");
        } finally {
            panel.dispose();
            server.stop();
        }
        server = new TestHttpServer();
        panel = FrameTestSupport.newPanel(400, 300);
        try {
            String inner = "<script>parent.sessionStorage.setItem('same',[parent.document.body.id,typeof localStorage.length].join('|'))</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body id=\"pb\">"
                    + "<iframe sandbox=\"allow-scripts allow-same-origin\" width=\"20\" height=\"20\" srcdoc=\"" + attr(inner) + "\"></iframe></body></html>");
            FrameTestSupport.awaitStored(panel, "same", "pb|number");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The flags accumulate: a frame inside a sandboxed frame cannot get back what the outer sandbox took away, whatever its own
     * <code>sandbox</code> attribute lists.
     * <p>
     * 플래그는 누적된다: 샌드박스된 프레임 안의 프레임은 자기 <code>sandbox</code> 속성에 무엇을 적든 바깥 샌드박스가 빼앗은 것을 되찾을 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSandboxFlagsAccumulateThroughNestedFrames() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            // 안쪽 프레임은 allow-scripts allow-same-origin 을 적지만 바깥이 allow-same-origin 을 주지 않았으므로 불투명 오리진이다.
            String innermost = "<script>function t(f){try{f();return 'ok'}catch(e){return e.name}}"
                    + "parent.parent.postMessage('inner:'+t(function(){return localStorage.length}),'*')</script>";
            String middle = "<body><iframe sandbox=\"allow-scripts allow-same-origin\" srcdoc=\"" + attr(innermost) + "\"></iframe></body>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe sandbox=\"allow-scripts\" width=\"20\" height=\"20\" srcdoc=\"" + attr(middle) + "\"></iframe>"
                    + "<script>window.addEventListener('message',function(e){sessionStorage.setItem('nested',e.data)})</script></body></html>");
            FrameTestSupport.awaitStored(panel, "nested", "inner:SecurityError");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A sandbox without <code>allow-forms</code> does not submit forms; with it the form is submitted into the frame.
     * <p>
     * <code>allow-forms</code> 가 없는 샌드박스는 폼을 제출하지 않고, 있으면 폼이 프레임으로 제출된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSandboxForbidsFormsUnlessAllowed() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/submitted", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            String form = "<body style=\"margin:0;background:#f00\"><form action=\"/submitted\" method=\"get\"><input type=\"submit\" id=\"s\" "
                    + "style=\"display:block;width:20px;height:20px\"></form>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe sandbox=\"allow-scripts\" width=\"40\" height=\"40\" srcdoc=\"" + attr(form
                            + "<script>setTimeout(function(){document.getElementById('s').click()},100)</script>") + "\"></iframe>"
                    + "<iframe sandbox=\"allow-scripts allow-forms\" width=\"40\" height=\"40\" srcdoc=\"" + attr(form
                            + "<script>setTimeout(function(){document.getElementById('s').click()},100)</script>") + "\"></iframe></body></html>");
            // allow-forms 가 있는 프레임(아래)만 제출 결과(초록)로 바뀐다. 위의 프레임은 제출이 막혀 빨강 그대로이다.
            FrameTestSupport.awaitPixel(panel, 30, 70, 0x00FF00, "the form was submitted with allow-forms");
            Thread.sleep(300);
            FrameTestSupport.awaitPixel(panel, 30, 30, 0xFF0000, "the form was not submitted without allow-forms");
            Check.equal(1, server.count("/submitted"), "only one submission reached the server");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Navigating the top-level page needs <code>allow-top-navigation</code>, for links with <code>target="_top"</code> and for scripts that
     * assign <code>top.location</code> alike; a sandboxed frame may still navigate itself.
     * <p>
     * 최상위 페이지의 이동에는 <code>allow-top-navigation</code> 이 필요하다. <code>target="_top"</code> 링크도 <code>top.location</code> 에 대입하는
     * 스크립트도 마찬가지이다. 샌드박스된 프레임은 자기 자신은 이동시킬 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSandboxTopNavigationNeedsPermission() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/top2", TestHttpServer.Response.html("<body>second</body>"));
            server.routeFixed("/self", TestHttpServer.Response.html("<body style=\"margin:0;background:#0f0\"></body>"));
            // 첫 프레임: 막힌 두 가지 시도 뒤에 자기 자신을 이동시킨다. 그 일이 끝난 것(초록)을 보면 앞의 두 시도도 처리된 것이다.
            String blocked = "<body style=\"margin:0;background:#f00\"><a id=\"l\" target=\"_top\" href=\"/top2\">x</a><script>"
                    + "document.getElementById('l').click();top.location.href='/top2';parent.location='/top2';"
                    + "setTimeout(function(){location.href='/self'},300)</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe sandbox=\"allow-scripts allow-same-origin\" width=\"40\" height=\"40\" srcdoc=\"" + attr(blocked) + "\"></iframe></body></html>");
            FrameTestSupport.awaitPixel(panel, 20, 20, 0x00FF00, "the sandboxed frame navigated itself");
            Check.equal(server.url("/page"), panel.getPage(), "the top-level page was not navigated");
            Check.equal(0, server.count("/top2"), "nothing was requested for the top-level navigation");
        } finally {
            panel.dispose();
            server.stop();
        }
        server = new TestHttpServer();
        panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/top2", TestHttpServer.Response.html("<body>second</body>"));
            String allowed = "<body><script>top.location.href='/top2'</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe sandbox=\"allow-scripts allow-top-navigation\" width=\"40\" height=\"40\" srcdoc=\"" + attr(allowed) + "\"></iframe></body></html>");
            long end = System.currentTimeMillis() + 20000;
            // 컴포넌트가 최상위 이동을 마칠 때까지 기다린다.
            while (!server.url("/top2").equals(panel.getPage()) && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.equal(server.url("/top2"), panel.getPage(), "the top-level page was navigated with allow-top-navigation");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A sandbox without <code>allow-popups</code> refuses <code>window.open</code> and a link to a new context; the unsandboxed frame gets
     * <code>null</code> for a window that would have to be new (a component has no windows) but can open a window by the name of a frame.
     * <p>
     * <code>allow-popups</code> 가 없는 샌드박스는 <code>window.open</code> 과 새 문맥으로의 링크를 거부한다. 샌드박스 없는 프레임은 새 창이 필요하면
     * <code>null</code> 을 받지만(컴포넌트에는 창이 없다) 프레임의 이름으로는 window 를 열 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSandboxForbidsPopupsUnlessAllowed() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            String opener = "<body><script>parent.postMessage('open:'+(window.open('/target.html','_blank')===null),'*')</script>";
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe sandbox=\"allow-scripts allow-same-origin\" width=\"20\" height=\"20\" srcdoc=\"" + attr(opener) + "\"></iframe>"
                    + "<script>window.addEventListener('message',function(e){sessionStorage.setItem('popup',e.data)})</script></body></html>");
            FrameTestSupport.awaitStored(panel, "popup", "open:true");
            Check.equal(0, server.count("/target.html"), "nothing was requested for the refused window");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * The <code>sandbox</code> attribute is read when the frame navigates: removing it from a loaded frame does not give the document its
     * scripts, but the next navigation of the frame does.
     * <p>
     * <code>sandbox</code> 속성은 프레임이 이동할 때 읽힌다: 로드된 프레임에서 속성을 지워도 그 문서가 스크립트를 얻지는 않고, 프레임의 다음 이동에서 얻는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSandboxAttributeIsReadAtNavigation() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(400, 300);
        try {
            server.routeFixed("/f.html", TestHttpServer.Response.html("<body style=\"margin:0;background:#f00\"><script>"
                    + "document.body.style.background='#0f0'</script></body>"));
            serve(panel, server, "<!DOCTYPE html><html><head>" + RESET + "</head><body>"
                    + "<iframe id=\"f\" sandbox=\"\" src=\"/f.html\" width=\"20\" height=\"20\"></iframe>"
                    + "<script>window.onload=function(){setTimeout(function(){var f=document.getElementById('f');f.removeAttribute('sandbox');"
                    + "sessionStorage.setItem('removed','yes');setTimeout(function(){f.src='/f.html?again'},400)},400)}</script></body></html>");
            FrameTestSupport.awaitStored(panel, "removed", "yes");
            FrameTestSupport.awaitPixel(panel, 10, 10, 0xFF0000, "the loaded document stays sandboxed after the attribute was removed");
            // 다음 이동은 속성이 없는 상태로 읽으므로 스크립트가 실행되어 초록이 된다.
            FrameTestSupport.awaitPixel(panel, 10, 10, 0x00FF00, "the next navigation runs scripts");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
