package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The whole way of a form submission through the real component, without a test host: the controls of the page, the form host of the
 * component, the form submit sink and the browser session (plan items P11-06, P15-10, P19-07). A script that submits a form starts the
 * navigation, and with JavaScript switched off a click on a submit button still sends the form, because forms are a basic function of the
 * page and not of the scripts.
 * <p>
 * 폼 제출이 시험용 호스트 없이 실제 컴포넌트를 지나는 전체 경로: 페이지의 컨트롤, 컴포넌트의 폼 호스트, 폼 제출 싱크, 브라우저 세션(계획 P11-06,
 * P15-10, P19-07). 폼을 제출하는 스크립트가 이동을 시작하고, JavaScript 를 끈 채로도 제출 단추의 클릭이 폼을 보낸다. 폼은 스크립트가 아니라 페이지의
 * 기본 기능이기 때문이다.
 */
public class FormSubmitWiringTest {

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Server of the case.
     * <p>
     * 사례의 서버.
     */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormSubmitWiringTest() {
    }

    /**
     * Starts the server with the pages of the cases.
     * <p>
     * 사례의 페이지가 있는 서버를 시작한다.
     *
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        String button = "<button style=\"position:absolute;left:0;top:0;width:120px;height:40px\">go</button>";
        server.routeFixed("/scripted", TestHttpServer.Response.html("<!doctype html><html><body>"
                + "<form id=f action=\"/order\" method=post><input name=item value=pen><input name=note value=\"한글\"></form>"
                + "<script>setTimeout(function () { document.getElementById('f').requestSubmit(); }, 30);</script></body></html>"));
        server.routeFixed("/plain", TestHttpServer.Response.html("<!doctype html><html><body style=\"margin:0\">"
                + "<form action=\"/search\" method=get>" + button + "<input type=hidden name=q value=\"a b\"></form></body></html>"));
        server.route("/order", new TestHttpServer.Handler() {
            /**
             * Echoes the body.
             * <p>
             * 본문을 돌려준다.
             *
             * @param request the request / 요청
             * @return the page / 페이지
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.html("<p>got " + request.method + " " + request.bodyText() + "</p>");
            }
        });
        server.route("/search", new TestHttpServer.Handler() {
            /**
             * Echoes the query.
             * <p>
             * 쿼리를 돌려준다.
             *
             * @param request the request / 요청
             * @return the page / 페이지
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.html("<p>found " + request.query + "</p>");
            }
        });
        server.start();
    }

    /**
     * Releases the components and the server.
     * <p>
     * 컴포넌트와 서버를 해제한다.
     */
    public void tearDown() {
        // 만든 패널을 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Waits until the component shows an address with a text in its source and the load ended.
     * <p>
     * 컴포넌트가 주소를 보여 주고 원문에 어떤 텍스트가 있으며 로딩이 끝날 때까지 기다린다.
     *
     * @param p      the component / 컴포넌트
     * @param url    the address / 주소
     * @param source a text of the source / 원문의 텍스트
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel p, String url, String source) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || p.getStatusText().startsWith("Loading") || p.getContent().indexOf(source) < 0)
                && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "page");
        Check.contains(p.getContent(), source, "source");
    }

    /**
     * A script that calls <code>requestSubmit()</code> sends the form with <code>POST</code> through the form host and the session, and
     * the answer is a page of the history.
     * <p>
     * <code>requestSubmit()</code> 을 부르는 스크립트는 폼 호스트와 세션을 거쳐 폼을 <code>POST</code> 로 보내고, 응답은 이력의 페이지가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptedRequestSubmitPostsTheForm() throws Exception {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(server.url("/scripted"));
        await(p, server.url("/order"), "got POST item=pen&note=%ED%95%9C%EA%B8%80");
        Check.equal(3, p.getHistories().size(), "the form page and the answer: " + p.getHistories());
        Check.equal("POST", server.last("/order").method, "sent with POST");
    }

    /**
     * Clicks the point (20, 20) of a component through its event dispatch.
     * <p>
     * 컴포넌트의 이벤트 전달로 (20, 20) 지점을 클릭한다.
     *
     * @param p the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static void click(final SwingWebPanel p) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the click.
             * <p>
             * 클릭을 전달한다.
             */
            public void run() {
                p.setSize(300, 200);
                p.dispatchEvent(new MouseEvent(p, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 20, 20, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Reads one item of the sessionStorage of a component through the Java getter.
     * <p>
     * 컴포넌트의 sessionStorage 항목 하나를 Java 조회 메서드로 읽는다.
     *
     * @param p   the component / 컴포넌트
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String session(SwingWebPanel p, String key) {
        // 복사한 항목에서 키를 찾는다.
        for (org.duckdns.hjow.swingwebpanel.storage.StorageEntry e : p.getSessionStorage()) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * An inline handler does not run while JavaScript is switched off, although the page is shown; switching it on again does not wake the
     * stopped page, and the next load of the page (a refresh) runs the handler.
     * <p>
     * JavaScript 를 끄면 페이지는 보이지만 인라인 핸들러는 실행되지 않는다. 다시 켜도 멈춘 페이지는 되살아나지 않으며, 페이지를 다음에 읽을 때(새로고침)
     * 핸들러가 실행된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInlineHandlerOffThenOnAfterRefresh() throws Exception {
        server.routeFixed("/handler", TestHttpServer.Response.html("<!doctype html><html><body style=\"margin:0\">"
                + "<div style=\"position:absolute;left:0;top:0;width:100px;height:40px\" onclick=\"sessionStorage.setItem('h','ran')\">x</div>"
                + "</body></html>"));
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setJavascriptEnabled(false);
        p.setPage(server.url("/handler"));
        await(p, server.url("/handler"), "onclick");
        click(p);
        Thread.sleep(300);
        Check.isTrue(session(p, "h") == null, "the inline handler did not run with JavaScript off");
        p.setJavascriptEnabled(true);
        click(p);
        Thread.sleep(300);
        Check.isTrue(session(p, "h") == null, "switching it on does not wake the stopped page");
        p.refresh(true);
        await(p, server.url("/handler"), "onclick");
        click(p);
        long end = System.currentTimeMillis() + 10000;
        // 새로 읽은 페이지의 핸들러가 값을 저장할 때까지 짧게 쉬며 기다린다.
        while (session(p, "h") == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("ran", session(p, "h"), "the next load runs the handler");
    }

    /**
     * With JavaScript switched off, a click on the submit button of a form sends the form: the basic function of the page does not depend on
     * the scripts. The click goes through the event dispatch of the component.
     * <p>
     * JavaScript 를 끄면 폼의 제출 단추 클릭이 폼을 보낸다: 페이지의 기본 기능은 스크립트에 의존하지 않는다. 클릭은 컴포넌트의 이벤트 전달을 지난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClickOnSubmitButtonWithJavascriptOff() throws Exception {
        final SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setJavascriptEnabled(false);
        p.setPage(server.url("/plain"));
        await(p, server.url("/plain"), "<button");
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Clicks the submit button.
             * <p>
             * 제출 단추를 클릭한다.
             */
            public void run() {
                p.setSize(300, 200);
                p.dispatchEvent(new MouseEvent(p, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 20, 20, 1, false, MouseEvent.BUTTON1));
            }
        });
        await(p, server.url("/search?q=a+b"), "found q=a+b");
        Check.equal(3, p.getHistories().size(), "an entry was added: " + p.getHistories());
        p.back();
        await(p, server.url("/plain"), "<button");
    }
}
