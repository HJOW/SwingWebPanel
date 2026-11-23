package org.duckdns.hjow.swingwebpanel.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FormHost;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The navigation that a form submission asks for, through the real component and real HTTP (plan items P11-06, P19-07, P19-01, P19-02, the
 * form part of P15-08-c-2): a <code>GET</code> form goes to the address with the encoded query, a <code>POST</code> form sends its body,
 * the page that answers a <code>POST</code> is shown again from its stored source by the history (the form is never sent twice by a
 * history move), a refresh sends the form again only after the user agreed, a <code>POST</code> that ends in a redirect leaves an ordinary
 * page, and the response of a form that must be saved as a file also asks first. The form host of the page is a small test host that
 * hands the submission to the session, as the component's own form host does.
 * <p>
 * 폼 제출이 요구하는 이동을 실제 컴포넌트와 실제 HTTP 로 확인하는 시험(계획 P11-06, P19-07, P19-01, P19-02, P15-08-c-2 의 폼 부분):
 * <code>GET</code> 폼은 인코딩한 쿼리가 붙은 주소로 가고, <code>POST</code> 폼은 본문을 보내며, <code>POST</code> 에 응답한 페이지는 이력이 저장한
 * 원문으로 다시 보여 주고(이력 이동은 폼을 두 번 보내지 않는다), 새로고침은 사용자가 동의한 뒤에만 폼을 다시 보내고, 리디렉션으로 끝나는
 * <code>POST</code> 는 일반 페이지를 남기며, 파일로 저장해야 하는 폼의 응답도 먼저 묻는다. 페이지의 폼 호스트는 컴포넌트 자신의 폼 호스트처럼 제출을
 * 세션에 넘기는 작은 시험용 호스트이다.
 */
public class FormNavigationIntegrationTest {

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
     * What the confirmer answers.
     * <p>
     * 확인기가 대답할 값.
     */
    private volatile boolean answer;

    /**
     * The addresses that the confirmer was asked about.
     * <p>
     * 확인기가 질문받은 주소.
     */
    private final List<String> asked = new ArrayList<String>();

    /**
     * Counts the answers of <code>/order</code>.
     * <p>
     * <code>/order</code> 의 응답 수를 센다.
     */
    private final AtomicInteger orders = new AtomicInteger();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public FormNavigationIntegrationTest() {
    }

    /**
     * Starts the server of a case.
     * <p>
     * 사례의 서버를 시작한다.
     *
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/form", TestHttpServer.Response.html("<!doctype html><html><body>"
                + "<form id=g action=\"/search\" method=get><input name=q value=\"한글 a&amp;b\"><input name=n value=\"1+1\"></form>"
                + "<form id=p action=\"/order\" method=post><input name=item value=pen><input name=note value=\"한글\"></form>"
                + "<form id=m action=\"/order\" method=post enctype=\"multipart/form-data\"><input name=a value=1></form>"
                + "<form id=r action=\"/redirect\" method=post><input name=x value=1></form>"
                + "<form id=d action=\"/export\" method=post><input name=k value=v></form>"
                + "<form id=f action=\"classpath://org/duckdns/hjow/swingwebpanel/testdata/hello.html\" method=post><input name=k value=v></form>"
                + "</body></html>"));
        server.route("/search", new TestHttpServer.Handler() {
            /**
             * Shows the query.
             * <p>
             * 쿼리를 보여 준다.
             *
             * @param request the request / 요청
             * @return the page / 페이지
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.html("<p id=r>" + request.method + " " + request.query + "</p>");
            }
        });
        server.route("/order", new TestHttpServer.Handler() {
            /**
             * Answers with a counter, so that a second send is visible in the page.
             * <p>
             * 두 번째 전송이 페이지에 보이도록 카운터와 함께 응답한다.
             *
             * @param request the request / 요청
             * @return the page / 페이지
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                int n = orders.incrementAndGet();
                return TestHttpServer.Response.html("<p>order " + n + " " + request.method + "</p>");
            }
        });
        server.routeFixed("/redirect", new TestHttpServer.Response(303).header("Location", "/done"));
        server.routeFixed("/done", TestHttpServer.Response.html("<p>done</p>"));
        server.routeFixed("/export", new TestHttpServer.Response(200).bytes("application/zip", new byte[] {1, 2, 3, 4})
                .header("Content-Disposition", "attachment; filename=\"out.zip\""));
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
     * Reads the session of a component (the component does not expose it).
     * <p>
     * 컴포넌트의 세션을 읽는다(컴포넌트가 노출하지 않는다).
     *
     * @param panel the component / 컴포넌트
     * @return the session / 세션
     * @throws Exception if the field cannot be read / 필드를 읽을 수 없으면
     */
    private static BrowserSession sessionOf(SwingWebPanel panel) throws Exception {
        Field f = SwingWebPanel.class.getDeclaredField("session");
        f.setAccessible(true);
        return (BrowserSession) f.get(panel);
    }

    /**
     * Creates a component whose confirmer answers with {@link #answer} and records the questions.
     * <p>
     * 확인기가 {@link #answer} 로 대답하고 질문을 기록하는 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private SwingWebPanel panel() throws Exception {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        sessionOf(p).setResubmitConfirmer(new ResubmitConfirmer() {
            /**
             * Records the question and answers.
             * <p>
             * 질문을 기록하고 대답한다.
             *
             * @param url the address / 주소
             * @return the prepared answer / 준비한 대답
             */
            public boolean confirmResubmit(String url) {
                synchronized (asked) {
                    asked.add(url);
                }
                return answer;
            }
        });
        return p;
    }

    /**
     * Waits until the component shows an address and the load ended.
     * <p>
     * 컴포넌트가 주소를 보여 주고 로딩이 끝날 때까지 기다린다.
     *
     * @param p      the component / 컴포넌트
     * @param url    the address / 주소
     * @param source a text that the source must contain / 원문에 들어 있어야 하는 텍스트
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel p, String url, String source) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 기대한 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || !"Done".equals(p.getStatusText()) || p.getContent().indexOf(source) < 0)
                && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "page");
        Check.contains(p.getContent(), source, "source of " + url);
        Check.equal("Done", p.getStatusText(), "status");
    }

    /**
     * Loads the page with the forms and installs a form host that hands the submissions to the session.
     * <p>
     * 폼이 있는 페이지를 읽고 제출을 세션에 넘기는 폼 호스트를 설치한다.
     *
     * @param p the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private void openForms(SwingWebPanel p) throws Exception {
        p.setPage(server.url("/form"));
        await(p, server.url("/form"), "id=g");
        final BrowserSession session = sessionOf(p);
        HtmlPageDocument holder = (HtmlPageDocument) session.getSnapshot().getDocument();
        holder.getDocument().setFormHost(new FormHost() {
            /**
             * Hands the submission to the session.
             * <p>
             * 제출을 세션에 넘긴다.
             *
             * @param submission the submission / 제출 정보
             * @param form       the form / 폼
             */
            public void submit(FormSubmission submission, Element form) {
                session.submitForm(submission);
            }

            /**
             * Ignored.
             * <p>
             * 무시한다.
             *
             * @param control the control / 컨트롤
             * @param message the message / 메시지
             */
            public void reportInvalid(Element control, String message) {
            }

            /**
             * Offers no file chooser.
             * <p>
             * 파일 선택기를 제공하지 않는다.
             *
             * @param input    the input / 입력
             * @param multiple several files / 여러 파일
             * @param accept   the accept attribute / accept 속성
             * @return null / null
             */
            public FileEntry[] chooseFiles(Element input, boolean multiple, String accept) {
                return null;
            }

            /**
             * Ignored.
             * <p>
             * 무시한다.
             *
             * @param element the element / 요소
             */
            public void stateChanged(Element element) {
            }
        });
    }

    /**
     * Submits a form of the page on the document queue, as the activation of a submit button does.
     * <p>
     * 제출 단추의 활성화처럼 페이지의 폼을 문서 큐에서 제출한다.
     *
     * @param p  the component / 컴포넌트
     * @param id the id of the form / 폼의 id
     * @throws Exception on failure / 실패 시
     */
    private static void submit(SwingWebPanel p, final String id) throws Exception {
        final BrowserSession session = sessionOf(p);
        final HtmlPageDocument holder = (HtmlPageDocument) session.getSnapshot().getDocument();
        Check.isTrue(session.submitDocumentTask(new Runnable() {
            /**
             * Submits the form.
             * <p>
             * 폼을 제출한다.
             */
            public void run() {
                ((HtmlFormElement) holder.getDocument().getElementById(id)).submit();
            }
        }), "the document queue accepted the task");
    }

    /**
     * A <code>GET</code> form is an ordinary navigation to the address with the encoded query (also non-ASCII text, <code>&amp;</code> and
     * a plus sign), and an entry of the history.
     * <p>
     * <code>GET</code> 폼은 인코딩한 쿼리가 붙은 주소로의 일반 이동이며(비ASCII 문자, <code>&amp;</code>, 더하기 기호 포함) 이력의 항목이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGetFormNavigatesWithTheQuery() throws Exception {
        SwingWebPanel p = panel();
        openForms(p);
        submit(p, "g");
        String url = server.url("/search?q=%ED%95%9C%EA%B8%80+a%26b&n=1%2B1");
        await(p, url, "GET q=%ED%95%9C%EA%B8%80+a%26b&n=1%2B1");
        Check.equal(3, p.getHistories().size(), "the form page and the answer are entries: " + p.getHistories());
        p.back();
        await(p, server.url("/form"), "id=g");
    }

    /**
     * A <code>POST</code> form sends its encoded body (the content type, the Referer and the text are checked at the server); the answer
     * is shown, and history moves show it again from the stored source without sending the form a second time.
     * <p>
     * <code>POST</code> 폼은 인코딩한 본문을 보낸다(콘텐츠 타입, Referer, 텍스트는 서버에서 확인). 응답이 보이고, 이력 이동은 폼을 두 번째로 보내지
     * 않고 저장한 원문으로 다시 보여 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPostFormSendsTheBodyAndHistoryDoesNotSendItAgain() throws Exception {
        SwingWebPanel p = panel();
        openForms(p);
        submit(p, "p");
        await(p, server.url("/order"), "order 1 POST");
        TestHttpServer.Request sent = server.last("/order");
        Check.equal("POST", sent.method, "method");
        Check.equal("item=pen&note=%ED%95%9C%EA%B8%80", sent.bodyText(), "the urlencoded body in UTF-8");
        Check.contains(sent.header("Content-Type"), "application/x-www-form-urlencoded", "content type");
        Check.equal(server.url("/form"), sent.header("Referer"), "the page with the form is the referrer");
        p.back();
        await(p, server.url("/form"), "id=g");
        p.forward();
        await(p, server.url("/order"), "order 1 POST");
        Check.equal(1, orders.get(), "going forward showed the stored answer: the form was not sent again");
        Check.equal(0, asked.size(), "and nobody was asked");
        Check.equal(3, p.getHistories().size(), "the history has three entries: " + p.getHistories());
    }

    /**
     * A refresh of the answer to a <code>POST</code> sends the form again only if the user agrees: with a no (or without a confirmer) the
     * page stays and the server sees no second request, with a yes the form goes out again and the new answer replaces the page without a
     * new history entry.
     * <p>
     * <code>POST</code> 에 대한 응답의 새로고침은 사용자가 동의할 때만 폼을 다시 보낸다. 아니오(또는 확인기가 없음)이면 페이지가 그대로이고 서버는 두
     * 번째 요청을 보지 못하며, 예이면 폼이 다시 나가고 새 응답이 새 이력 항목 없이 페이지를 대신한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRefreshOfAPostAnswerAsksTheUser() throws Exception {
        SwingWebPanel p = panel();
        openForms(p);
        submit(p, "p");
        await(p, server.url("/order"), "order 1 POST");
        answer = false;
        p.refresh();
        long end = System.currentTimeMillis() + 5000;
        // 질문이 기록될 때까지 짧게 쉬며 기다린다.
        while (asked.isEmpty() && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(1, asked.size(), "the user was asked once");
        Check.equal(server.url("/order"), asked.get(0), "about the address of the form");
        Thread.sleep(300);
        Check.equal(1, orders.get(), "a no sends nothing");
        Check.contains(p.getContent(), "order 1", "the page stays");
        answer = true;
        p.refresh(true);
        await(p, server.url("/order"), "order 2 POST");
        Check.equal(2, orders.get(), "a yes sends the form again");
        Check.equal(3, p.getHistories().size(), "and adds no history entry");
        Check.equal("item=pen&note=%ED%95%9C%EA%B8%80", server.last("/order").bodyText(), "with the same body");
    }

    /**
     * A <code>multipart/form-data</code> form sends a multipart body with a boundary, and a <code>POST</code> that ends in a redirect
     * leaves an ordinary page: nothing is asked when it is refreshed.
     * <p>
     * <code>multipart/form-data</code> 폼은 경계가 있는 multipart 본문을 보내고, 리디렉션으로 끝나는 <code>POST</code> 는 일반 페이지를 남긴다.
     * 그 페이지를 새로고침해도 묻지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMultipartBodyAndPostRedirectGetPage() throws Exception {
        SwingWebPanel p = panel();
        openForms(p);
        submit(p, "m");
        await(p, server.url("/order"), "order 1 POST");
        Check.contains(server.last("/order").header("Content-Type"), "multipart/form-data; boundary=", "multipart with a boundary");
        Check.contains(server.last("/order").bodyText(), "Content-Disposition: form-data; name=\"a\"", "the part");
        p.back();
        await(p, server.url("/form"), "id=g");
        openForms(p);
        submit(p, "r");
        await(p, server.url("/done"), "done");
        Check.equal("GET", server.last("/done").method, "the redirect target is requested with GET");
        answer = false;
        p.refresh();
        await(p, server.url("/done"), "done");
        Check.equal(0, asked.size(), "a page that a redirect led to is refreshed without a question");
    }

    /**
     * A form that would be sent to the class path from a network page is blocked and nothing happens; the response of a form that must be
     * saved as a file asks before the form is sent a second time for the download.
     * <p>
     * 네트워크 페이지에서 클래스패스로 보내려는 폼은 차단되어 아무 일도 일어나지 않는다. 파일로 저장해야 하는 폼의 응답은 다운로드를 위해 폼을 두 번째로
     * 보내기 전에 묻는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedTargetAndDownloadAsksBeforeSendingAgain() throws Exception {
        SwingWebPanel p = panel();
        openForms(p);
        submit(p, "f");
        Thread.sleep(400);
        Check.equal(server.url("/form"), p.getPage(), "the blocked form changed nothing");
        // 응답이 첨부 파일인 폼: 처음 응답은 버리고 사용자가 동의하면 다운로드가 폼을 다시 보낸다.
        final List<String> saved = new ArrayList<String>();
        sessionOf(p).setDownloadHandler(new DownloadHandler() {
            /**
             * Records the download.
             * <p>
             * 다운로드를 기록한다.
             *
             * @param request  the navigation / 이동
             * @param download the response / 응답
             */
            public void downloadRequired(NavigationRequest request, DownloadRequiredException download) {
                synchronized (saved) {
                    saved.add(download.getFinalUrl() + " post=" + (request.getPost() != null));
                }
            }
        });
        answer = false;
        submit(p, "d");
        long end = System.currentTimeMillis() + 5000;
        // 질문이 기록될 때까지 짧게 쉬며 기다린다.
        while (asked.isEmpty() && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(200);
        Check.equal(1, asked.size(), "the user was asked");
        Check.equal(0, saved.size(), "a no saves nothing");
        Check.equal(server.url("/form"), p.getPage(), "the page stays");
        answer = true;
        submit(p, "d");
        end = System.currentTimeMillis() + 5000;
        // 다운로드가 시작될 때까지 짧게 쉬며 기다린다.
        while (saved.isEmpty() && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(1, saved.size(), "a yes starts the download");
        Check.contains(saved.get(0), "post=true", "of the response to the form");
        Check.equal(server.url("/form"), p.getPage(), "the page and the history stay");
    }
}
