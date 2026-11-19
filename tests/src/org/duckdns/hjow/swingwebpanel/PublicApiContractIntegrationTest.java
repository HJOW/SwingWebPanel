package org.duckdns.hjow.swingwebpanel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The public methods of {@link SwingWebPanel} that the requirements list, used the way a host application uses them: only the public
 * methods, a real component, a real HTTP server whose request log shows what really went over the wire (plan items P22-01 to P22-04). Each
 * case names the contract it checks (<code>docs/API_CONTRACT.md</code>): initial values, normal use, invalid arguments, the moment the
 * effect starts, copies that do not write through, and what a script can and cannot change.
 * <p>
 * {@link SwingWebPanel} 의 요구 목록 공개 메서드를 호스트 응용이 쓰는 방식 그대로 검증한다: 공개 메서드만, 실제 컴포넌트, 실제로 선로를 오간 것을 보여 주는
 * 요청 기록이 있는 실제 HTTP 서버(계획 P22-01~P22-04). 각 사례는 점검하는 계약(<code>docs/API_CONTRACT.md</code>)을 밝힌다: 초기값, 정상 사용, 잘못된
 * 인수, 효과가 시작되는 시점, 쓰기가 전파되지 않는 복사본, 스크립트가 바꿀 수 있는 것과 없는 것.
 */
public class PublicApiContractIntegrationTest {

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The components that the case created.
     * <p>
     * 사례가 만든 컴포넌트.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PublicApiContractIntegrationTest() {
    }

    /**
     * Creates the server.
     * <p>
     * 서버를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
    }

    /**
     * Disposes the components and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        // 사례가 만든 컴포넌트를 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Creates a component for the case.
     * <p>
     * 사례의 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Waits until the component confirmed an address, finished loading and has a text in its source.
     * <p>
     * 컴포넌트가 주소를 확정하고 로딩을 끝내며 원문에 텍스트가 있을 때까지 기다린다.
     *
     * @param p      the component / 컴포넌트
     * @param url    the address / 주소
     * @param source a text of the source (may be empty) / 원문의 텍스트(비어 있을 수 있음)
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel p, String url, String source) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정되고 로딩이 끝나며 원문이 도착할 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || p.getContent().indexOf(source) < 0) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "confirmed page");
        Check.contains(p.getContent(), source, "source of " + url);
    }

    /**
     * Returns a value of a storage collection by key, or null.
     * <p>
     * 저장소 컬렉션에서 키로 값을 찾는다. 없으면 null.
     *
     * @param entries the entries / 항목
     * @param key     the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String valueOf(Collection<StorageEntry> entries, String key) {
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : entries) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * P22-01 initial values and invalid arguments: user agent, page, JavaScript flag, content.
     * <p>
     * P22-01 초기값과 잘못된 인수: User-Agent, 페이지, JavaScript 설정, 콘텐츠.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInitialValuesAndInvalidArguments() throws Exception {
        SwingWebPanel p = panel();
        Check.equal("swingwebpanel/1.0", p.getUserAgent(), "default user agent");
        Check.equal("about:blank", p.getPage(), "initial page");
        Check.equal("", p.getContent(), "initial content");
        Check.isTrue(p.isJavascriptEnabled(), "JavaScript is on at the start");
        Check.equal(100, p.getProgress(), "initial progress");
        Check.equal("", p.getStatusText(), "initial status");
        String[] badAgents = { null, "", "a\r\nb" };
        // 잘못된 User-Agent 를 하나씩 넣어 모두 IllegalArgumentException 인지 확인한다.
        for (int i = 0; i < badAgents.length; i++) {
            try {
                p.setUserAgent(badAgents[i]);
                Check.fail("user agent " + i + " must be refused");
            } catch (IllegalArgumentException expected) {
                Check.isTrue(true, "refused " + i);
            }
        }
        Check.equal("swingwebpanel/1.0", p.getUserAgent(), "a refused value changes nothing");
        String[] badPages = { null, "", "   ", "about:text", "not a url" };
        // 잘못된 주소를 하나씩 넣어 모두 IllegalArgumentException 인지 확인한다.
        for (int i = 0; i < badPages.length; i++) {
            try {
                p.setPage(badPages[i]);
                Check.fail("page " + badPages[i] + " must be refused");
            } catch (IllegalArgumentException expected) {
                Check.isTrue(true, "refused page " + i);
            }
        }
        Check.equal("about:blank", p.getPage(), "refused pages change nothing");
        try {
            p.setContent(null, "text/html");
            Check.fail("null content must be refused");
        } catch (IllegalArgumentException expected) {
            Check.isTrue(true, "null content refused");
        }
        try {
            p.setContent("x", "image/png");
            Check.fail("an unsupported MIME type must be refused");
        } catch (IllegalArgumentException expected) {
            Check.isTrue(true, "unsupported MIME refused");
        }
        p.setContent("", "text/plain");
        await(p, "about:text", "");
        Check.equal("about:text", p.getPage(), "empty content is allowed and is about:text");
        Check.equal("", p.getContent(), "empty content is kept");
    }

    /**
     * P22-01 the user agent reaches the server from the next request on, setPage loads asynchronously, <code>getContent()</code> keeps the
     * received source although the script changes the document, and JavaScript can be switched off and on.
     * <p>
     * P22-01 User-Agent 는 다음 요청부터 서버에 닿고, setPage 는 비동기로 읽으며, 스크립트가 문서를 바꿔도 <code>getContent()</code> 는 수신 원문을 유지하고,
     * JavaScript 를 껐다 켤 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUserAgentPageContentAndJavascript() throws Exception {
        String page = "<!doctype html><meta charset=utf-8><body><p id=t>original</p><script>document.getElementById('t').textContent = 'changed';"
                + "sessionStorage.setItem('ran', '1');</" + "script>";
        server.routeFixed("/p", TestHttpServer.Response.html(page).delay(150));
        server.start();
        SwingWebPanel p = panel();
        p.setUserAgent("Contract/2");
        long before = System.currentTimeMillis();
        p.setPage(server.url("/p"));
        Check.isTrue(System.currentTimeMillis() - before < 100, "setPage returns at once");
        Check.equal("about:blank", p.getPage(), "the previous page is reported while loading");
        await(p, server.url("/p"), "original");
        Check.equal("Contract/2", server.last("/p").header("User-Agent"), "the user agent reached the server");
        Check.equal(page, p.getContent(), "the received source is returned, not the changed document");
        Check.equal("1", valueOf(p.getSessionStorage(), "ran"), "the script ran");
        p.setJavascriptEnabled(false);
        Check.isFalse(p.isJavascriptEnabled(), "the switch is visible");
        Check.equal("1", valueOf(p.getSessionStorage(), "ran"), "switching changes nothing that exists");
        p.clean();
        p.setPage(server.url("/p"));
        await(p, server.url("/p"), "original");
        Check.isNull(valueOf(p.getSessionStorage(), "ran"), "no script ran while JavaScript is off");
        Check.equal(page, p.getContent(), "the source is the same");
        p.setJavascriptEnabled(true);
        Check.isNull(valueOf(p.getSessionStorage(), "ran"), "switching on does not run the stopped page");
        p.refresh(true);
        await(p, server.url("/p"), "original");
        long end = System.currentTimeMillis() + 10000;
        // 다시 읽은 페이지의 스크립트가 값을 저장할 때까지 기다린다.
        while (valueOf(p.getSessionStorage(), "ran") == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("1", valueOf(p.getSessionStorage(), "ran"), "the next load runs the script");
    }

    /**
     * P22-01 <code>setContent</code> with the four MIME types: the input is kept verbatim, the address is <code>about:text</code>, MIME
     * parameters and case are accepted, and the content is shown (the title proves that the markup was parsed).
     * <p>
     * P22-01 네 MIME 타입의 <code>setContent</code>: 입력은 그대로 보관되고, 주소는 <code>about:text</code> 이며, MIME 매개변수와 대소문자를 받아들이고, 내용을
     * 보여 준다(제목이 마크업을 해석했음을 보여 준다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSetContentTypes() throws Exception {
        SwingWebPanel p = panel();
        String html = "<title>T-html</title><p>x\r\n  y</p>";
        p.setContent(html, "TEXT/HTML; charset=UTF-8");
        await(p, "about:text", "x\r\n  y");
        Check.equal(html, p.getContent(), "html is kept verbatim (also the line breaks)");
        Check.equal("T-html", p.getTitle(), "the html was parsed");
        String md = "# Heading\n\n* item\n";
        p.setContent(md, "text/markdown");
        await(p, "about:text", "Heading");
        Check.equal(md, p.getContent(), "markdown is kept as written, not as the generated html");
        String text = "<b>not markup</b>\n";
        p.setContent(text, "text/plain");
        await(p, "about:text", "not markup");
        Check.equal(text, p.getContent(), "plain text is kept verbatim");
        String xhtml = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>T-xhtml</title></head><body><p>xp</p></body></html>";
        p.setContent(xhtml, "application/xhtml+xml");
        await(p, "about:text", "xp");
        Check.equal(xhtml, p.getContent(), "xhtml is kept verbatim");
        Check.equal("T-xhtml", p.getTitle(), "the xhtml was parsed");
        Check.equal(5, p.getHistories().size(), "each setContent is a history entry: " + p.getHistories());
        p.back();
        await(p, "about:text", "not markup");
        Check.equal(text, p.getContent(), "back restores the text content, not only the address");
    }

    /**
     * P22-02 history: <code>back(n)</code> and <code>forward(n)</code> move exactly n entries or do nothing, zero and negative numbers do
     * nothing, a new navigation after going back drops the forward entries, and the list that <code>getHistories()</code> returns is a copy.
     * <p>
     * P22-02 이력: <code>back(n)</code> 과 <code>forward(n)</code> 은 정확히 n 칸 이동하거나 아무것도 하지 않고, 0 과 음수는 아무것도 하지 않으며, 뒤로 간 뒤의
     * 새 이동은 앞쪽 항목을 지우고, <code>getHistories()</code> 의 목록은 복사본이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHistoryMovesAndBranches() throws Exception {
        server.routeFixed("/a", TestHttpServer.Response.html("page A"));
        server.routeFixed("/b", TestHttpServer.Response.html("page B"));
        server.routeFixed("/c", TestHttpServer.Response.html("page C"));
        server.routeFixed("/d", TestHttpServer.Response.html("page D"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/a"));
        await(p, server.url("/a"), "page A");
        p.setPage(server.url("/b"));
        await(p, server.url("/b"), "page B");
        p.setPage(server.url("/c"));
        await(p, server.url("/c"), "page C");
        Check.equal(4, p.getHistories().size(), "about:blank and three pages: " + p.getHistories());
        Check.equal(3, p.getHistoryIndex(), "at the end");
        p.back(0);
        p.back(-1);
        p.forward(1);
        p.forward(0);
        Check.equal(server.url("/c"), p.getPage(), "zero, negative and impossible moves do nothing");
        p.back(5);
        Check.equal(server.url("/c"), p.getPage(), "back(n) with too few entries does nothing, it is not clamped");
        p.back(2);
        await(p, server.url("/a"), "page A");
        Check.equal(1, p.getHistoryIndex(), "two entries back");
        p.forward(3);
        Check.equal(server.url("/a"), p.getPage(), "forward(n) with too few entries does nothing");
        p.forward();
        await(p, server.url("/b"), "page B");
        Vector<String> copy = p.getHistories();
        copy.clear();
        Check.equal(4, p.getHistories().size(), "changing the returned list does not change the history");
        p.setPage(server.url("/d"));
        await(p, server.url("/d"), "page D");
        Check.equal(4, p.getHistories().size(), "a new navigation after going back drops the forward entries: " + p.getHistories());
        Check.equal(server.url("/b"), p.getHistories().get(2), "the entry before is kept");
        p.forward();
        Check.equal(server.url("/d"), p.getPage(), "there is no forward entry any more");
        p.back(3);
        await(p, "about:blank", "");
        Check.equal("about:blank", p.getPage(), "back(3) reaches the first entry");
        Check.equal(0, p.getHistoryIndex(), "first entry");
    }

    /**
     * P22-02 redirects and refresh: a redirect is one history entry with the final address, <code>refresh()</code> follows the cache policy,
     * <code>refresh(true)</code> asks the server again, and neither adds a history entry.
     * <p>
     * P22-02 리디렉션과 새로고침: 리디렉션은 최종 주소의 이력 항목 하나이고, <code>refresh()</code> 는 캐시 정책을 따르며, <code>refresh(true)</code> 는 서버에 다시
     * 묻고, 둘 다 이력 항목을 더하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectAndRefresh() throws Exception {
        server.routeFixed("/old", TestHttpServer.Response.redirect(302, "/new"));
        server.routeFixed("/new", TestHttpServer.Response.html("cacheable"));
        server.routeFixed("/fresh", TestHttpServer.Response.html("fresh page").header("Cache-Control", "max-age=3600"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/old"));
        await(p, server.url("/new"), "cacheable");
        Check.equal(2, p.getHistories().size(), "one entry for the redirect: " + p.getHistories());
        Check.equal(server.url("/new"), p.getHistories().get(1), "the final address is recorded");
        p.setPage(server.url("/fresh"));
        await(p, server.url("/fresh"), "fresh page");
        Check.equal(1, server.count("/fresh"), "one request so far");
        p.refresh();
        await(p, server.url("/fresh"), "fresh page");
        Check.equal(1, server.count("/fresh"), "refresh() follows the cache policy: the fresh copy is used");
        p.refresh(true);
        await(p, server.url("/fresh"), "fresh page");
        long end = System.currentTimeMillis() + 10000;
        // 강제 새로고침의 요청이 서버에 닿을 때까지 기다린다.
        while (server.count("/fresh") < 2 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(server.count("/fresh") >= 2, "refresh(true) asked the server again");
        Check.equal(3, p.getHistories().size(), "refreshing adds no entry: " + p.getHistories());
    }

    /**
     * P22-03 status and progress, hyperlink listeners and the effects of <code>clean()</code> and <code>dispose()</code>.
     * <p>
     * P22-03 상태와 진행률, 하이퍼링크 리스너, <code>clean()</code> 과 <code>dispose()</code> 의 효과.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStatusListenersCleanAndDispose() throws Exception {
        server.routeFixed("/slow", TestHttpServer.Response.html("<a id=l href=\"/next\" style=\"position:absolute;left:0;top:0\">go</a>").delay(400));
        server.routeFixed("/next", TestHttpServer.Response.html("next"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/slow"));
        Thread.sleep(150);
        Check.isTrue(p.getProgress() < 100, "progress is below 100 while the document is received: " + p.getProgress());
        Check.isTrue(p.getStatusText().length() > 0, "a status text while loading");
        await(p, server.url("/slow"), "go");
        Check.equal(100, p.getProgress(), "100 when done");
        final List<HyperlinkEvent> events = new ArrayList<HyperlinkEvent>();
        HyperlinkListener listener = new HyperlinkListener() {
            /**
             * Records the event.
             * <p>
             * 이벤트를 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void hyperlinkUpdate(HyperlinkEvent e) {
                events.add(e);
            }
        };
        try {
            p.addHyperlinkListener(null);
            Check.fail("a null listener must be refused");
        } catch (IllegalArgumentException expected) {
            Check.isTrue(true, "null listener refused");
        }
        p.addHyperlinkListener(listener);
        p.removeHyperlinkListener(listener);
        p.removeHyperlinkListener(listener);
        p.addHyperlinkListener(listener);
        p.removeHyperlinkEvents();
        p.addHyperlinkListener(listener);
        p.removeHyperlinkEvents();
        p.clean();
        Check.equal("about:blank", p.getPage(), "clean shows the blank page");
        Check.equal(1, p.getHistories().size(), "clean resets the history");
        Check.equal("", p.getContent(), "clean empties the content");
        p.setUserAgent("Kept/1");
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Kept", "yes");
        p.addHeaders(headers);
        p.clean();
        Check.equal("Kept/1", p.getUserAgent(), "clean keeps the user agent");
        Check.equal("yes", p.getHeaders().get("X-Kept"), "clean keeps the additional headers");
        p.dispose();
        p.dispose();
        Check.equal("about:blank", p.getPage(), "disposed: blank");
        Check.equal("", p.getContent(), "disposed: no content");
        Check.equal(0, p.getLocalStorage().size() + p.getSessionStorage().size(), "disposed: no storage");
        try {
            p.setPage(server.url("/next"));
            Check.fail("a disposed component refuses setPage");
        } catch (IllegalStateException expected) {
            Check.isTrue(true, "setPage refused");
        }
        try {
            p.addHyperlinkListener(listener);
            Check.fail("a disposed component refuses listeners");
        } catch (IllegalStateException expected) {
            Check.isTrue(true, "listener refused");
        }
    }

    /**
     * P22-04 headers: names are case-insensitive and a later value replaces an earlier one, other headers stay, <code>User-Agent</code> is
     * neither accepted nor reported, invalid entries change nothing, and <code>getHeaders()</code> is a copy; the headers go to the server.
     * <p>
     * P22-04 헤더: 이름은 대소문자를 구분하지 않고 나중 값이 이전 값을 대신하며, 다른 헤더는 유지되고, <code>User-Agent</code> 는 받지도 보고하지도 않으며, 잘못된
     * 항목은 아무것도 바꾸지 않고, <code>getHeaders()</code> 는 복사본이며, 헤더는 서버에 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHeaders() throws Exception {
        server.routeFixed("/h", TestHttpServer.Response.html("headers"));
        server.start();
        SwingWebPanel p = panel();
        Map<String, String> first = new LinkedHashMap<String, String>();
        first.put("X-One", "1");
        first.put("Accept-Language", "ko");
        first.put("user-agent", "Evil/1");
        p.addHeaders(first);
        Map<String, String> second = new LinkedHashMap<String, String>();
        second.put("x-one", "uno");
        p.addHeaders(second);
        Map<String, String> got = p.getHeaders();
        Check.equal(2, got.size(), "two headers, no User-Agent: " + got);
        Check.equal("uno", valueOf(got, "x-one"), "the later value replaced the earlier one");
        Check.equal("ko", valueOf(got, "Accept-Language"), "the other header stayed");
        got.clear();
        Check.equal(2, p.getHeaders().size(), "getHeaders() is a copy");
        Map<String, String> bad = new LinkedHashMap<String, String>();
        bad.put("X-Bad", "a\r\nInjected: 1");
        try {
            p.addHeaders(bad);
            Check.fail("a header with CR/LF must be refused");
        } catch (IllegalArgumentException expected) {
            Check.isTrue(true, "refused");
        }
        Check.equal(2, p.getHeaders().size(), "a refused map applies nothing");
        p.setPage(server.url("/h"));
        await(p, server.url("/h"), "headers");
        Check.equal("uno", server.last("/h").header("X-One"), "the header reached the server");
        Check.equal("ko", server.last("/h").header("Accept-Language"), "the second header reached the server");
        Check.equal("swingwebpanel/1.0", server.last("/h").header("User-Agent"), "the user agent is the component's");
    }

    /**
     * Returns the value of a map entry by case-insensitive key, or null.
     * <p>
     * 맵 항목의 값을 대소문자 구분 없는 키로 찾는다. 없으면 null.
     *
     * @param map the map / 맵
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String valueOf(Map<String, String> map, String key) {
        // 맵의 키를 하나씩 대소문자 없이 비교한다.
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * P22-04 cookies and storages: the cookies that the server set come back in <code>name=value; name=value</code> form, the storage
     * collections are copies with origin, key and value, sessionStorage belongs to one component while localStorage is shared by the
     * components of the process, and <code>clean()</code> empties them (also the shared localStorage).
     * <p>
     * P22-04 쿠키와 저장소: 서버가 정한 쿠키는 <code>name=value; name=value</code> 형식으로 돌아오고, 저장소 컬렉션은 오리진, 키, 값을 가진 복사본이며,
     * sessionStorage 는 컴포넌트 하나의 것이고 localStorage 는 프로세스의 컴포넌트가 공유하며, <code>clean()</code> 은 그것들을(공용 localStorage 도) 비운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCookiesAndStorages() throws Exception {
        String store = "<!doctype html><meta charset=utf-8><script>sessionStorage.setItem('s', 'session-value');"
                + "localStorage.setItem('l', 'local-value');</" + "script>store";
        server.routeFixed("/store", TestHttpServer.Response.html(store).header("Set-Cookie", "b=2; Path=/").header("Set-Cookie", "a=1; Path=/"));
        server.routeFixed("/plain", TestHttpServer.Response.html("plain"));
        server.start();
        SwingWebPanel p = panel();
        SwingWebPanel q = panel();
        p.setPage(server.url("/store"));
        await(p, server.url("/store"), "store");
        long end = System.currentTimeMillis() + 10000;
        // 스크립트가 두 저장소에 값을 쓸 때까지 기다린다.
        while ((valueOf(p.getSessionStorage(), "s") == null || valueOf(p.getLocalStorage(), "l") == null) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal("a=1; b=2", p.getCookies(), "cookies in name order as one string");
        Check.equal("session-value", valueOf(p.getSessionStorage(), "s"), "sessionStorage entry");
        Check.equal("local-value", valueOf(p.getLocalStorage(), "l"), "localStorage entry");
        StorageEntry entry = p.getSessionStorage().iterator().next();
        Check.equal(originOf(server.url("/")), entry.getOrigin(), "the entry carries its origin");
        Check.equal("", q.getCookies(), "another component has its own cookies");
        Check.equal(0, q.getSessionStorage().size(), "sessionStorage is not shared between components");
        Check.equal("local-value", valueOf(q.getLocalStorage(), "l"), "localStorage is shared by the components of the process");
        Collection<StorageEntry> copy = p.getSessionStorage();
        try {
            copy.clear();
        } catch (UnsupportedOperationException readOnly) {
            Check.isTrue(true, "the copy is read-only");
        }
        Check.equal("session-value", valueOf(p.getSessionStorage(), "s"), "changing the copy does not change the storage");
        p.clean();
        Check.equal("a=1; b=2", p.getCookies(), "clean keeps the cookies");
        Check.equal(0, p.getSessionStorage().size(), "clean empties sessionStorage");
        Check.equal(0, q.getLocalStorage().size(), "clean empties the shared localStorage, also as the other component sees it");
    }

    /**
     * Returns the origin text of a URL (<code>scheme://host:port</code>).
     * <p>
     * URL 의 오리진 문자열(<code>scheme://host:port</code>)을 반환한다.
     *
     * @param url the URL / URL
     * @return the origin / 오리진
     */
    private static String originOf(String url) {
        int slash = url.indexOf('/', url.indexOf("//") + 2);
        return url.substring(0, slash);
    }
}
