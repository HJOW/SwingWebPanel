package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of history, back/forward and refresh behavior (contract cases C-HIST and C-REFRESH, plan items P01-07 to P01-09).
 * <p>
 * 이력, 뒤로/앞으로, 새로고침 동작 시험(계약 사례 C-HIST, C-REFRESH, 계획 P01-07~P01-09).
 */
public class HistoryNavigationTest {

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
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HistoryNavigationTest() {
    }

    /**
     * Creates a fresh session before each case.
     * <p>
     * 사례마다 새 세션을 만든다.
     */
    public void setUp() {
        loader = new FakeLoader();
        session = new BrowserSession(loader, new TestSupport.Recorder());
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
     * Navigates and waits until the page is confirmed.
     * <p>
     * 이동하고 페이지가 확정될 때까지 기다린다.
     *
     * @param url address / 주소
     */
    private void go(String url) {
        session.setPage(url);
        TestSupport.settle(session);
    }

    /**
     * Returns the history as one comma separated string for compact assertions.
     * <p>
     * 간단히 비교하도록 이력을 쉼표로 이은 문자열로 돌려준다.
     *
     * @return history URLs / 이력 URL
     */
    private String urls() {
        StringBuilder sb = new StringBuilder();
        // 이력 URL 을 순서대로 이어 붙인다.
        for (String u : session.getHistories()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(u);
        }
        return sb.toString();
    }

    /**
     * C-HIST-1..4: boundaries of back and forward; n must be reachable exactly, otherwise nothing happens.
     * <p>
     * C-HIST-1..4: 뒤로/앞으로의 경계. n 칸을 정확히 갈 수 없으면 아무 일도 일어나지 않는다.
     */
    public void testBackForwardBoundaries() {
        session.back(1);
        session.forward(1);
        TestSupport.settle(session);
        Check.equal(0, loader.requestCount(), "back/forward on a single entry does nothing");
        go("http://a.test/");
        go("http://b.test/");
        go("http://c.test/");
        Check.equal(3, session.getHistoryIndex(), "index after three navigations");
        session.back(0);
        session.back(-3);
        session.forward(0);
        session.forward(-1);
        session.forward(1);
        session.back(4);
        session.back(100);
        TestSupport.settle(session);
        Check.equal(3, session.getHistoryIndex(), "invalid or unreachable moves do nothing");
        Check.equal("http://c.test/", session.getPage(), "page unchanged");
        session.back(1);
        TestSupport.settle(session);
        Check.equal("http://b.test/", session.getPage(), "back(1)");
        Check.equal(2, session.getHistoryIndex(), "index after back(1)");
        session.back(2);
        TestSupport.settle(session);
        Check.equal("about:blank", session.getPage(), "back(2) reaches the first entry");
        Check.equal(0, session.getHistoryIndex(), "index at the start");
        session.back(1);
        TestSupport.settle(session);
        Check.equal(0, session.getHistoryIndex(), "back at the first entry does nothing");
        session.forward(4);
        TestSupport.settle(session);
        Check.equal(0, session.getHistoryIndex(), "forward(4) would pass the end: nothing happens (no clamping)");
        session.forward(3);
        TestSupport.settle(session);
        Check.equal("http://c.test/", session.getPage(), "forward(3) reaches the last entry");
        Check.equal("about:blank,http://a.test/,http://b.test/,http://c.test/", urls(), "moving never changes the entries");
    }

    /**
     * C-HIST-3: a new navigation after going back deletes the forward entries.
     * <p>
     * C-HIST-3: 뒤로 간 뒤 새로 이동하면 앞쪽 항목이 삭제된다.
     */
    public void testNewNavigationAfterBackDeletesForwardEntries() {
        go("http://a.test/");
        go("http://b.test/");
        go("http://c.test/");
        session.back(2);
        TestSupport.settle(session);
        go("http://d.test/");
        Check.equal("about:blank,http://a.test/,http://d.test/", urls(), "forward entries are gone");
        Check.equal(2, session.getHistoryIndex(), "index points to the new entry");
        session.forward(1);
        TestSupport.settle(session);
        Check.equal("http://d.test/", session.getPage(), "nothing to go forward to");
    }

    /**
     * C-HIST-4: the same URL twice in a row creates two entries.
     * <p>
     * C-HIST-4: 같은 URL 이 연속되면 항목이 둘 생긴다.
     */
    public void testConsecutiveSameUrlAreSeparateEntries() {
        go("http://a.test/");
        go("http://a.test/");
        Check.equal("about:blank,http://a.test/,http://a.test/", urls(), "two entries");
    }

    /**
     * C-HIST-5: a redirect is recorded as one entry with the final URL.
     * <p>
     * C-HIST-5: 리디렉션은 최종 URL 한 건으로 기록된다.
     */
    public void testRedirectIsRecordedAsFinalUrlOnly() {
        loader.redirect("http://r.test/start", "http://r.test/final");
        go("http://r.test/start");
        Check.equal("about:blank,http://r.test/final", urls(), "only the final URL");
        Check.equal("http://r.test/final", session.getPage(), "page URL is the final URL");
    }

    /**
     * C-HIST-6: set content is restored from the history with its source and MIME type.
     * <p>
     * C-HIST-6: setContent 는 이력에서 원문과 MIME 타입과 함께 복원된다.
     */
    public void testSetContentIsRestoredFromHistory() {
        session.setContent("hello <b>world</b>", "text/html; charset=UTF-8");
        TestSupport.settle(session);
        go("http://a.test/");
        session.back(1);
        TestSupport.settle(session);
        Check.equal("about:text", session.getPage(), "URL of the restored entry");
        Check.equal("hello <b>world</b>", session.getContent(), "source is restored exactly");
        Check.equal("text/html", session.getMimeType(), "MIME type is restored");
        Check.equal("about:blank,about:text,http://a.test/", urls(), "entries are unchanged");
    }

    /**
     * C-REFRESH-1..2: refresh does not add entries; force is handed to the loader.
     * <p>
     * C-REFRESH-1..2: 새로고침은 항목을 추가하지 않으며 force 가 로더에 전달된다.
     */
    public void testRefreshDoesNotAddEntriesAndPassesForce() {
        go("http://a.test/");
        session.refresh(false);
        TestSupport.settle(session);
        session.refresh(true);
        TestSupport.settle(session);
        Check.equal(3, loader.requestCount(), "one initial load and two refreshes");
        Check.isFalse(loader.requests.get(1).isForce(), "refresh(false) is not forced");
        Check.isTrue(loader.requests.get(2).isForce(), "refresh(true) is forced");
        Check.equal(NavigationType.REFRESH, loader.requests.get(2).getType(), "request type");
        Check.equal("about:blank,http://a.test/", urls(), "no entries added");
        Check.equal(1, session.getHistoryIndex(), "position unchanged");
    }

    /**
     * C-REFRESH-3..4: about:text is rebuilt from the stored source; about:blank stays blank.
     * <p>
     * C-REFRESH-3..4: about:text 는 보관한 원문으로 다시 만들어지고 about:blank 는 빈 채로 유지된다.
     */
    public void testRefreshOfTextAndBlank() {
        session.refresh(false);
        TestSupport.settle(session);
        Check.equal("about:blank", session.getPage(), "blank stays blank");
        Check.equal("", session.getContent(), "blank content");
        session.setContent("keep me", "text/plain");
        TestSupport.settle(session);
        session.refresh(true);
        TestSupport.settle(session);
        Check.equal("about:text", session.getPage(), "URL");
        Check.equal("keep me", session.getContent(), "content rebuilt from the stored source");
        Check.equal("about:blank,about:text", urls(), "no entries added");
    }

    /**
     * C-REFRESH-5: refreshing an error page requests the same URL again.
     * <p>
     * C-REFRESH-5: 오류 페이지를 새로고침하면 같은 URL 을 다시 요청한다.
     */
    public void testRefreshOfErrorPageRequestsTheSameUrl() {
        loader.fail("http://boom.test/", new IllegalStateException("x"));
        go("http://boom.test/");
        Check.equal(PageKind.ERROR, session.getSnapshot().getKind(), "error page");
        loader.fail("http://boom.test/", null);
        session.refresh(false);
        TestSupport.settle(session);
        Check.equal("http://boom.test/", loader.requests.get(1).getUrl(), "the failed URL is requested again");
    }

    /**
     * Moving in the history while a load is running replaces that load; the position changes only when the move is confirmed.
     * <p>
     * 로딩 중에 이력 이동을 하면 그 로딩을 대체하고, 위치는 이동이 확정될 때에만 바뀐다.
     */
    public void testHistoryMoveSupersedesRunningLoad() {
        go("http://a.test/");
        go("http://b.test/");
        loader.gate("http://slow.test/");
        session.setPage("http://slow.test/");
        session.back(1);
        TestSupport.settle(session);
        Check.equal("http://a.test/", session.getPage(), "the history move won");
        Check.equal("about:blank,http://a.test/,http://b.test/", urls(), "the superseded navigation left no trace");
        Check.equal(1, session.getHistoryIndex(), "position of the confirmed move");
    }

    /**
     * Documents of replaced pages are disposed exactly once, including pages that fall out of the history.
     * <p>
     * 교체된 페이지의 문서는 이력에서 빠지는 페이지를 포함해 정확히 한 번 해제된다.
     */
    public void testDocumentsOfReplacedPagesAreDisposed() {
        int before = loader.disposed.get();
        go("http://a.test/");
        go("http://b.test/");
        Check.equal(before + 1, loader.disposed.get(), "a.test left the screen and was disposed");
        session.back(1);
        TestSupport.settle(session);
        Check.equal(before + 2, loader.disposed.get(), "b.test was disposed when going back");
        session.dispose();
        Check.equal(before + 3, loader.disposed.get(), "the visible page is disposed with the session");
    }
}
