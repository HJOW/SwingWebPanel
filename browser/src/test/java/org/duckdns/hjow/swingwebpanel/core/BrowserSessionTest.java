package org.duckdns.hjow.swingwebpanel.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the settings, input validation, asynchronous loading, stale response handling and status/progress of
 * {@link BrowserSession} (plan items P03-02, P03-03, P03-06; contract cases C-INIT, C-UA, C-PAGE, C-SETCONTENT, C-STATUS, C-HEADERS).
 * <p>
 * {@link BrowserSession} 의 설정, 입력 검증, 비동기 로딩, 오래된 응답 처리, 상태/진행률 시험(계획 P03-02, P03-03, P03-06,
 * 계약 사례 C-INIT, C-UA, C-PAGE, C-SETCONTENT, C-STATUS, C-HEADERS).
 */
public class BrowserSessionTest {

    /**
     * Loader used by the tests of this suite.
     * <p>
     * 이 시험들이 쓰는 로더.
     */
    private FakeLoader loader;

    /**
     * Session under test.
     * <p>
     * 시험 대상 세션.
     */
    private BrowserSession session;

    /**
     * Recorder of notifications.
     * <p>
     * 통지 기록기.
     */
    private TestSupport.Recorder recorder;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrowserSessionTest() {
    }

    /**
     * Creates a fresh session before each case.
     * <p>
     * 사례마다 새 세션을 만든다.
     */
    public void setUp() {
        loader = new FakeLoader();
        recorder = new TestSupport.Recorder();
        session = new BrowserSession(loader, recorder);
    }

    /**
     * Disposes the session after each case.
     * <p>
     * 사례가 끝나면 세션을 폐기한다.
     */
    public void tearDown() {
        session.dispose();
    }

    /**
     * C-INIT-1: the initial state of a new session.
     * <p>
     * C-INIT-1: 새 세션의 초기 상태.
     */
    public void testInitialState() {
        Check.equal("about:blank", session.getPage(), "initial URL");
        Check.equal("", session.getContent(), "initial content");
        Check.equal(1, session.getHistories().size(), "history size");
        Check.equal("about:blank", session.getHistories().get(0), "history entry");
        Check.equal(0, session.getHistoryIndex(), "history index");
        Check.equal("swingwebpanel/1.0", session.getUserAgent(), "default user agent");
        Check.isTrue(session.getHeaders().isEmpty(), "no extra headers");
        Check.isTrue(session.isJavascriptEnabled(), "JavaScript is enabled by default");
        Check.equal(100, session.getProgress(), "initial progress");
        Check.equal("", session.getStatusText(), "initial status text");
        Check.equal("", session.getTitle(), "initial title");
    }

    /**
     * C-UA-1..3: setting and validating the user agent.
     * <p>
     * C-UA-1..3: User-Agent 설정과 검증.
     */
    public void testUserAgent() {
        session.setUserAgent("MyAgent/2.0");
        Check.equal("MyAgent/2.0", session.getUserAgent(), "set value");
        String[] bad = { null, "", "   ", "a\r\nb", "x\ny" };
        // 잘못된 값들은 모두 IllegalArgumentException 이어야 하고 기존 값은 유지되어야 한다.
        for (int i = 0; i < bad.length; i++) {
            final String value = bad[i];
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /**
                 * Sets an invalid user agent.
                 * <p>
                 * 잘못된 User-Agent 를 지정한다.
                 */
                public void run() {
                    session.setUserAgent(value);
                }
            }, "invalid user agent " + value);
        }
        Check.equal("MyAgent/2.0", session.getUserAgent(), "value is kept after rejected input");
    }

    /**
     * C-UA-4: the user agent in effect is handed to the loader.
     * <p>
     * C-UA-4: 적용 중인 User-Agent 가 로더에 전달된다.
     */
    public void testUserAgentAndHeadersReachTheLoader() {
        session.setUserAgent("AgentX");
        Map<String, String> h = new HashMap<String, String>();
        h.put("Accept-Language", "ko");
        session.addHeaders(h);
        session.setPage("http://t.test/a");
        TestSupport.settle(session);
        NavigationRequest r = loader.requests.get(0);
        Check.equal("AgentX", r.getUserAgent(), "user agent in request");
        Check.equal("ko", r.getHeaders().get("accept-language"), "extra header in request");
        Check.isTrue(r.isJavascriptEnabled(), "JavaScript flag in request");
    }

    /**
     * C-PAGE-4: invalid addresses are rejected with IllegalArgumentException and never reach the loader.
     * <p>
     * C-PAGE-4: 잘못된 주소는 IllegalArgumentException 으로 거부되며 로더에 도달하지 않는다.
     */
    public void testSetPageRejectsInvalidAddresses() {
        String[] bad = { null, "", "   ", "foo.html", "/abs/path", "about:text", "http://", "ht tp://x", "bad" + (char) 7 + "url" };
        // 모든 잘못된 주소는 예외여야 한다.
        for (int i = 0; i < bad.length; i++) {
            final String value = bad[i];
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /**
                 * Navigates to an invalid address.
                 * <p>
                 * 잘못된 주소로 이동한다.
                 */
                public void run() {
                    session.setPage(value);
                }
            }, "invalid address " + value);
        }
        TestSupport.settle(session);
        Check.equal(0, loader.requestCount(), "the loader must not be called for invalid input");
        Check.equal("about:blank", session.getPage(), "page is unchanged");
    }

    /**
     * C-PAGE-5: a well-formed address with a forbidden scheme becomes an error page, not an exception.
     * <p>
     * C-PAGE-5: 형식은 올바르지만 금지된 스킴의 주소는 예외가 아니라 오류 페이지가 된다.
     */
    public void testBlockedSchemeBecomesErrorPage() {
        BrowserSession s = new BrowserSession(new DefaultPageLoader(), null);
        try {
            s.setPage("mailto:<b>x@y.z</b>");
            TestSupport.settle(s);
            PageSnapshot p = s.getSnapshot();
            Check.equal(PageKind.ERROR, p.getKind(), "kind");
            Check.equal(DefaultPageLoader.STATUS_UNSUPPORTED_ADDRESS, p.getStatusCode(), "status code");
            Check.contains(p.getContent(), "&lt;b&gt;x@y.z&lt;/b&gt;", "the address is escaped in the error page");
            Check.isFalse(p.getContent().indexOf("<b>") >= 0, "no raw markup from the address");
            Check.equal(2, s.getHistories().size(), "the error page is recorded in the history");
        } finally {
            s.dispose();
        }
    }

    /**
     * Network addresses must not report a fake success while loading is not implemented.
     * <p>
     * 로딩이 구현되지 않은 동안 네트워크 주소가 거짓 성공을 보고하면 안 된다.
     */
    public void testNetworkAddressIsHonestlyNotImplementedByDefaultLoader() {
        BrowserSession s = new BrowserSession(new DefaultPageLoader(), null);
        try {
            s.setPage("http://example.com/");
            TestSupport.settle(s);
            Check.equal(PageKind.ERROR, s.getSnapshot().getKind(), "kind");
            Check.equal(DefaultPageLoader.STATUS_NOT_IMPLEMENTED, s.getSnapshot().getStatusCode(), "status code");
            Check.equal("http://example.com/", s.getPage(), "URL");
        } finally {
            s.dispose();
        }
    }

    /**
     * C-SETCONTENT-1..4: content validation and MIME normalization.
     * <p>
     * C-SETCONTENT-1..4: 콘텐츠 검증과 MIME 정규화.
     */
    public void testSetContentValidationAndNormalization() {
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Passes null content.
             * <p>
             * null 콘텐츠를 넘긴다.
             */
            public void run() {
                session.setContent(null, "text/html");
            }
        }, "null content");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Passes a null MIME type.
             * <p>
             * null MIME 타입을 넘긴다.
             */
            public void run() {
                session.setContent("x", null);
            }
        }, "null MIME type");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Passes an unsupported MIME type.
             * <p>
             * 지원하지 않는 MIME 타입을 넘긴다.
             */
            public void run() {
                session.setContent("x", "image/png");
            }
        }, "unsupported MIME type");
        session.setContent("", " TEXT/HTML ; charset=UTF-8 ");
        TestSupport.settle(session);
        Check.equal("about:text", session.getPage(), "URL of set content");
        Check.equal("", session.getContent(), "empty content is allowed and kept");
        Check.equal("text/html", session.getMimeType(), "MIME type is normalized");
        session.setContent("# Title\r\n", "text/markdown");
        TestSupport.settle(session);
        Check.equal("# Title\r\n", session.getContent(), "content is kept exactly as given");
        Check.equal("text/markdown", session.getMimeType(), "markdown type");
        session.setContent("<x/>", "application/xhtml+xml");
        session.setContent("plain", "text/plain");
        TestSupport.settle(session);
        Check.equal("plain", session.getContent(), "the last content wins");
    }

    /**
     * C-PAGE-2/3: while a page loads the previous page is reported; at confirmation URL and source switch together.
     * <p>
     * C-PAGE-2/3: 페이지를 읽는 동안에는 이전 페이지가 보고되고, 확정 시 URL 과 원문이 함께 바뀐다.
     */
    public void testPreviousPageIsReportedWhileLoading() {
        session.setContent("A", "text/plain");
        TestSupport.settle(session);
        loader.gate("http://slow.test/");
        session.setPage("http://slow.test/");
        Check.equal("about:text", session.getPage(), "URL while loading");
        Check.equal("A", session.getContent(), "content while loading");
        Check.equal(2, session.getHistories().size(), "history while loading");
        Check.equal(0, session.getProgress(), "progress at the start");
        Check.equal("Loading http://slow.test/", session.getStatusText(), "status while loading");
        loader.release("http://slow.test/");
        TestSupport.settle(session);
        Check.equal("http://slow.test/", session.getPage(), "URL after confirmation");
        Check.equal("content of http://slow.test/", session.getContent(), "content after confirmation");
        Check.equal(100, session.getProgress(), "progress after confirmation");
        Check.equal("Done", session.getStatusText(), "status after confirmation");
    }

    /**
     * C-PAGE-8: a response that arrives late for an outdated request is discarded, and its resources are released.
     * <p>
     * C-PAGE-8: 오래된 요청의 늦은 응답은 폐기되고 그 자원은 해제된다.
     */
    public void testStaleResponseIsDiscarded() {
        loader.ignoreCancel = true;
        loader.gate("http://old.test/");
        int disposedBefore = loader.disposed.get();
        session.setPage("http://old.test/");
        // 이전 요청이 로더에 실제로 도착한 뒤에 새 요청을 시작해야 늦은 완료 경로를 확실히 시험할 수 있다.
        new BrowserSessionProbe(session).waitUntilRequested(loader, 1);
        session.setPage("http://new.test/");
        TestSupport.waitForPage(session, "http://new.test/");
        loader.release("http://old.test/");
        TestSupport.settle(session);
        Check.equal("http://new.test/", session.getPage(), "the newest request wins");
        Check.equal(2, session.getHistories().size(), "the stale page is not in the history");
        Check.equal("http://new.test/", session.getHistories().get(1), "history entry");
        Check.equal(disposedBefore + 1, loader.disposed.get(), "the discarded page must be disposed exactly once");
    }

    /**
     * A cooperative loader is cancelled through its token as soon as a newer navigation starts.
     * <p>
     * 협조적인 로더는 더 새로운 이동이 시작되면 취소 토큰으로 곧바로 중단된다.
     */
    public void testCancelTokenIsCancelledByNewNavigation() {
        loader.gate("http://slow.test/");
        session.setPage("http://slow.test/");
        new BrowserSessionProbe(session).waitUntilRequested(loader, 1);
        NavigationRequest first = loader.requests.get(0);
        Check.isFalse(first.getCancelToken().isCancelled(), "not cancelled yet");
        session.setPage("http://next.test/");
        Check.isTrue(first.getCancelToken().isCancelled(), "cancelled by the newer navigation");
        TestSupport.settle(session);
        Check.equal("http://next.test/", session.getPage(), "newer page is shown");
    }

    /**
     * An exception from the loader becomes an error page, not a crash or a hang.
     * <p>
     * 로더의 예외는 충돌이나 멈춤이 아니라 오류 페이지가 된다.
     */
    public void testLoaderFailureBecomesErrorPage() {
        loader.fail("http://boom.test/", new IllegalStateException("<script>bad</script>"));
        session.setPage("http://boom.test/");
        TestSupport.settle(session);
        PageSnapshot p = session.getSnapshot();
        Check.equal(PageKind.ERROR, p.getKind(), "kind");
        Check.equal(500, p.getStatusCode(), "status code");
        Check.isFalse(p.getContent().indexOf("<script>") >= 0, "message text is escaped");
        Check.equal(100, session.getProgress(), "progress after failure");
        Check.contains(session.getStatusText(), "Error", "status text after failure");
    }

    /**
     * stop() cancels a running navigation and resets the status.
     * <p>
     * stop() 은 진행 중인 이동을 취소하고 상태를 되돌린다.
     */
    public void testStopCancelsNavigation() {
        loader.ignoreCancel = true;
        loader.gate("http://slow.test/");
        session.setPage("http://slow.test/");
        Check.equal(0, session.getProgress(), "progress while loading");
        session.stop();
        Check.equal(100, session.getProgress(), "progress after stop");
        loader.release("http://slow.test/");
        TestSupport.settle(session);
        Check.equal("about:blank", session.getPage(), "the stopped navigation must not be confirmed");
    }

    /**
     * C-STATUS-1..2: notifications arrive on the EDT, in order, and only for real changes.
     * <p>
     * C-STATUS-1..2: 통지는 EDT 에서 순서대로 도착하고 실제 변경에만 발생한다.
     */
    public void testNotificationsAreOrderedAndOnTheEdt() {
        session.setPage("http://a.test/");
        TestSupport.settle(session);
        Check.isTrue(recorder.allOnEdt, "all notifications run on the EDT");
        Object[] expected = { "status Loading http://a.test/", "progress 0", "confirmed http://a.test/",
            "page about:blank -> http://a.test/", "status Done", "progress 100" };
        Check.equal(java.util.Arrays.asList(expected), new java.util.ArrayList<String>(recorder.events), "event sequence");
        recorder.events.clear();
        session.setPage("http://a.test/");
        TestSupport.settle(session);
        Check.isFalse(recorder.events.contains("page http://a.test/ -> http://a.test/"), "no page change for the same URL");
        Check.contains(recorder.events.toString(), "confirmed http://a.test/", "but the page is confirmed again");
    }

    /**
     * C-HEADERS-1..6: overwrite semantics, case-insensitivity, user agent exclusion, validation and copies.
     * <p>
     * C-HEADERS-1..6: 덮어쓰기, 대소문자 무시, User-Agent 제외, 검증, 복사본.
     */
    public void testHeaders() {
        Map<String, String> h = new HashMap<String, String>();
        h.put("X-One", "1");
        h.put("x-two", "2");
        session.addHeaders(h);
        Map<String, String> h2 = new HashMap<String, String>();
        h2.put("X-ONE", "uno");
        h2.put("User-Agent", "evil");
        h2.put("USER-AGENT", "evil2");
        session.addHeaders(h2);
        Map<String, String> got = session.getHeaders();
        Check.equal(2, got.size(), "two headers, user agent excluded");
        Check.equal("uno", lookup(got, "x-one"), "overwritten value");
        Check.equal("2", lookup(got, "x-two"), "untouched header stays");
        Check.equal("swingwebpanel/1.0", session.getUserAgent(), "user agent cannot be changed through headers");
        got.clear();
        Check.equal(2, session.getHeaders().size(), "getHeaders returns a copy");

        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Adds headers where one value contains CR/LF.
             * <p>
             * 값 하나에 CR/LF 가 든 헤더를 추가한다.
             */
            public void run() {
                Map<String, String> m = new HashMap<String, String>();
                m.put("X-Three", "3");
                m.put("X-Bad", "a\r\nInjected: 1");
                session.addHeaders(m);
            }
        }, "CR/LF in value");
        Check.equal(2, session.getHeaders().size(), "nothing is applied when an entry is invalid");
        Check.throwsException(IllegalArgumentException.class, new Block() {
            /**
             * Passes a null map.
             * <p>
             * null 맵을 넘긴다.
             */
            public void run() {
                session.addHeaders(null);
            }
        }, "null map");
    }

    /**
     * Looks a header up ignoring case.
     * <p>
     * 대소문자를 무시하고 헤더를 찾는다.
     *
     * @param m    headers / 헤더
     * @param name name / 이름
     * @return value or null / 값 또는 null
     */
    private static String lookup(Map<String, String> m, String name) {
        // 이름을 대소문자 구분 없이 비교하며 찾는다.
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Small helper that waits until the loader has seen a number of requests.
     * <p>
     * 로더가 요청을 일정 수 받을 때까지 기다리는 작은 도우미.
     */
    private static final class BrowserSessionProbe {
        /**
         * Session being probed.
         * <p>
         * 대상 세션.
         */
        private final BrowserSession target;

        /**
         * Creates a probe.
         * <p>
         * 도우미를 만든다.
         *
         * @param target session / 세션
         */
        BrowserSessionProbe(BrowserSession target) {
            this.target = target;
        }

        /**
         * Waits for the loader to receive the given number of requests.
         * <p>
         * 로더가 주어진 수의 요청을 받을 때까지 기다린다.
         *
         * @param l     the loader / 로더
         * @param count expected request count / 기대하는 요청 수
         * @return the probed session / 대상 세션
         */
        BrowserSession waitUntilRequested(FakeLoader l, int count) {
            long end = System.currentTimeMillis() + 5000;
            // 로더가 요청을 받을 때까지 짧게 쉬며 기다린다.
            while (l.requestCount() < count && System.currentTimeMillis() < end) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            Check.isTrue(l.requestCount() >= count, "loader must receive the request");
            return target;
        }
    }

    /**
     * The history vector returned to callers is a copy and must not write through.
     * <p>
     * 호출자에게 돌려준 이력 벡터는 복사본이며 원본에 영향을 주면 안 된다.
     */
    public void testHistoryCopyDoesNotWriteThrough() {
        session.setPage("http://a.test/");
        TestSupport.settle(session);
        Vector<String> copy = session.getHistories();
        copy.clear();
        copy.add("junk");
        Check.equal(2, session.getHistories().size(), "the history is unaffected by changes to the copy");
        Check.equal("http://a.test/", session.getHistories().get(1), "entries unchanged");
    }
}
