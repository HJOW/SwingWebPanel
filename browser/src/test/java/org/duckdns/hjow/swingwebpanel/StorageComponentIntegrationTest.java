package org.duckdns.hjow.swingwebpanel;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The Storage API through real components, scripts and real HTTP (plan item P16-10): <code>sessionStorage</code> belongs to one component,
 * <code>localStorage</code> is shared by the components of the process but separated by origin, a change of a shared item reaches the other
 * documents of the origin as a <code>storage</code> event (not the document that made it), a script that overflows the quota gets a
 * <code>QuotaExceededError</code> and leaves the storage unchanged, and <code>clean()</code> empties the shared <code>localStorage</code>
 * without touching the <code>sessionStorage</code> of other components. File damage, concurrent writes, quotas and the separation of
 * processes are checked at the level of the storage classes (<code>LocalStorageManagerTest</code>).
 * <p>
 * 실제 컴포넌트, 스크립트, 실제 HTTP 로 Storage API 를 확인하는 시험(계획 P16-10): <code>sessionStorage</code> 는 컴포넌트 하나의 것이고,
 * <code>localStorage</code> 는 프로세스의 컴포넌트가 공유하되 오리진별로 분리되며, 공유 항목의 변경은 같은 오리진의 다른 문서에 <code>storage</code>
 * 이벤트로 전달되고(변경한 문서에는 전달되지 않음), 쿼터를 넘기는 스크립트는 <code>QuotaExceededError</code> 를 받고 저장소는 바뀌지 않으며,
 * <code>clean()</code> 은 공유 <code>localStorage</code> 를 비우되 다른 컴포넌트의 <code>sessionStorage</code> 는 건드리지 않는다. 파일 손상, 동시 쓰기, 용량,
 * 프로세스 분리는 저장소 클래스 수준에서 확인한다(<code>LocalStorageManagerTest</code>).
 */
public class StorageComponentIntegrationTest {

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Servers created by the case.
     * <p>
     * 사례가 만든 서버.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StorageComponentIntegrationTest() {
    }

    /**
     * Empties the shared localStorage that the case filled and releases everything it created.
     * <p>
     * 사례가 채운 공유 localStorage 를 비우고 만든 것을 모두 해제한다.
     */
    public void tearDown() {
        // 첫 패널의 clean() 으로 프로세스 공용 localStorage 를 비운다. 다른 시험이 남은 항목을 보지 않게 한다.
        if (!panels.isEmpty()) {
            panels.get(0).clean();
        }
        // 만든 패널을 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        // 시작한 서버를 모두 중지한다.
        for (TestHttpServer s : servers) {
            s.stop();
        }
    }

    /**
     * Starts a server whose pages are the given scripts.
     * <p>
     * 주어진 스크립트가 페이지인 서버를 시작한다.
     *
     * @param pages path and script pairs / 경로와 스크립트의 쌍
     * @return the server / 서버
     * @throws Exception if it cannot start / 시작할 수 없으면
     */
    private TestHttpServer serve(String[]... pages) throws Exception {
        TestHttpServer server = new TestHttpServer();
        // 경로마다 스크립트 하나를 담은 페이지를 등록한다.
        for (String[] page : pages) {
            server.routeFixed(page[0], TestHttpServer.Response.html("<!doctype html><html><body><script>" + page[1] + "</script></body></html>"));
        }
        server.start();
        servers.add(server);
        return server;
    }

    /**
     * Creates a panel that is disposed after the case.
     * <p>
     * 사례가 끝나면 폐기되는 패널을 만든다.
     *
     * @return the panel / 패널
     */
    private SwingWebPanel panel() {
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        return p;
    }

    /**
     * Loads a page and waits until the load ended ("Done" and progress 100).
     * <p>
     * 페이지를 읽고 로딩이 끝날 때(상태 "Done", 진행률 100)까지 기다린다.
     *
     * @param p   the panel / 패널
     * @param url the address / 주소
     */
    private static void load(SwingWebPanel p, String url) {
        p.setPage(url);
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || !"Done".equals(p.getStatusText())) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
        Check.equal(url, p.getPage(), "the page was confirmed");
    }

    /**
     * Reads one item of the sessionStorage of a panel through the Java getter.
     * <p>
     * 패널의 sessionStorage 항목 하나를 Java 조회 메서드로 읽는다.
     *
     * @param p   the panel / 패널
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String session(SwingWebPanel p, String key) {
        // 복사한 항목에서 키를 찾는다.
        for (StorageEntry e : p.getSessionStorage()) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until a sessionStorage item has the expected value.
     * <p>
     * sessionStorage 항목이 기대한 값이 될 때까지 기다린다.
     *
     * @param p        the panel / 패널
     * @param key      the key / 키
     * @param expected the expected value / 기대한 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitSession(SwingWebPanel p, String key, String expected) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 기다린다.
        while (!expected.equals(session(p, key)) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(expected, session(p, key), "sessionStorage item " + key);
    }

    /**
     * Reads one item of the localStorage of an origin through the Java getter.
     * <p>
     * 오리진의 localStorage 항목 하나를 Java 조회 메서드로 읽는다.
     *
     * @param p      the panel / 패널
     * @param origin the origin like <code>http://localhost:1234</code> / 오리진(<code>http://localhost:1234</code> 형태)
     * @param key    the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String local(SwingWebPanel p, String origin, String key) {
        // 복사한 항목에서 오리진과 키가 같은 것을 찾는다.
        for (StorageEntry e : p.getLocalStorage()) {
            if (origin.equals(e.getOrigin()) && key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Two components and two origins: the session items stay with their component (also over a second navigation), the local items are
     * shared inside an origin and separate between origins, the writer of a shared item gets no storage event and the other document of the
     * origin gets exactly one, a document of another origin hears nothing, and <code>clean()</code> empties the shared localStorage only.
     * <p>
     * 컴포넌트 두 개와 오리진 두 개: session 항목은 컴포넌트에 남고(두 번째 이동에도), local 항목은 오리진 안에서 공유되고 오리진 사이에서는 분리되며,
     * 공유 항목을 바꾼 쪽은 storage 이벤트를 받지 않고 같은 오리진의 다른 문서가 정확히 한 번 받으며, 다른 오리진의 문서는 아무것도 듣지 못하고,
     * <code>clean()</code> 은 공유 localStorage 만 비운다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSessionSeparatedLocalSharedAndSeparatedByOrigin() throws Exception {
        TestHttpServer a = serve(
                new String[] { "/p1", "localStorage.setItem('shared','one'); sessionStorage.setItem('mine','p1');"
                        + "window.addEventListener('storage', function (e) { sessionStorage.setItem('ev', (sessionStorage.getItem('ev') || '')"
                        + " + e.key + ':' + e.oldValue + '->' + e.newValue + ';'); });" },
                new String[] { "/p2", "sessionStorage.setItem('mine','p2'); localStorage.setItem('shared','two');"
                        + "window.addEventListener('storage', function (e) { sessionStorage.setItem('ev', 'writer-heard-itself'); });" },
                new String[] { "/other", "sessionStorage.setItem('other','yes');" });
        TestHttpServer b = serve(new String[] { "/p3", "sessionStorage.setItem('seen', String(localStorage.getItem('shared')));"
                + "localStorage.setItem('b','bee');" });
        String originA = "http://localhost:" + a.getPort();
        String originB = "http://localhost:" + b.getPort();
        SwingWebPanel one = panel();
        SwingWebPanel two = panel();
        SwingWebPanel three = panel();
        one.clean();
        load(one, a.url("/p1"));
        load(two, a.url("/p2"));
        // 같은 오리진의 다른 문서(첫 패널)는 변경 이벤트를 정확히 한 번 받고, 변경한 문서(둘째 패널)는 받지 않는다.
        awaitSession(one, "ev", "shared:one->two;");
        Thread.sleep(200);
        Check.equal("shared:one->two;", session(one, "ev"), "the other document heard the change exactly once");
        Check.isTrue(session(two, "ev") == null, "the writer did not hear its own change");
        Check.equal("p1", session(one, "mine"), "sessionStorage of the first component");
        Check.equal("p2", session(two, "mine"), "sessionStorage of the second component");
        Check.equal("two", local(one, originA, "shared"), "the shared item as the first component sees it");
        Check.equal("two", local(two, originA, "shared"), "the shared item as the second component sees it");
        // 다른 오리진은 같은 키를 보지 못하고, 그 오리진의 변경은 이쪽 문서에 이벤트로 오지 않는다.
        load(three, b.url("/p3"));
        Check.equal("null", session(three, "seen"), "another origin does not see the item");
        Check.equal("bee", local(one, originB, "b"), "the Java getter lists the items of every origin");
        Thread.sleep(200);
        Check.equal("shared:one->two;", session(one, "ev"), "a change in another origin sends no event");
        // 같은 컴포넌트의 두 번째 이동에도 sessionStorage 는 남는다.
        load(one, a.url("/other"));
        Check.equal("p1", session(one, "mine"), "the session survives a navigation of the component");
        Check.equal("yes", session(one, "other"), "and the new page adds to it");
        // clean() 은 공유 localStorage 를 모든 컴포넌트에서 비우고 다른 컴포넌트의 sessionStorage 는 그대로 둔다.
        two.clean();
        Check.equal(0, one.getLocalStorage().size(), "the first component sees the empty localStorage");
        Check.equal(0, three.getLocalStorage().size(), "the third component sees the empty localStorage");
        Check.equal(0, two.getSessionStorage().size(), "the cleaned component lost its sessionStorage");
        Check.equal("p1", session(one, "mine"), "other components keep their sessionStorage");
        Check.equal("null", session(three, "seen"), "also the third one");
    }

    /**
     * A script that overflows the quota gets a <code>QuotaExceededError</code> at the item that does not fit, and the storage keeps the items
     * stored before.
     * <p>
     * 쿼터를 넘기는 스크립트는 들어가지 않는 항목에서 <code>QuotaExceededError</code> 를 받고, 저장소는 그 전에 저장한 항목을 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQuotaExceededErrorInAScript() throws Exception {
        TestHttpServer a = serve(new String[] { "/big", "var big = new Array(1000001).join('x'); var i = 0;"
                + "try { for (; i < 10; i++) { localStorage.setItem('k' + i, big); } sessionStorage.setItem('quota', 'none'); }"
                + "catch (e) { sessionStorage.setItem('quota', e.name + ':' + i); }" });
        String origin = "http://localhost:" + a.getPort();
        SwingWebPanel p = panel();
        p.clean();
        load(p, a.url("/big"));
        Check.equal("QuotaExceededError:2", session(p, "quota"), "the third item of one million characters does not fit");
        Check.equal(2, p.getLocalStorage().size(), "the two items that fit stayed");
        Check.equal(1000000, local(p, origin, "k1").length(), "and are complete");
        Check.isTrue(local(p, origin, "k2") == null, "the item that did not fit left no trace");
    }
}
