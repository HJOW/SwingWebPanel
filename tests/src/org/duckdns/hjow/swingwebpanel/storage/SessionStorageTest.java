package org.duckdns.hjow.swingwebpanel.storage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link SessionStorage} (plan items P16-02, P16-08, P16-10): isolation between components and origins, notifications that skip
 * the source document, opaque origins and collectability.
 * <p>
 * {@link SessionStorage} 시험(계획 P16-02, P16-08, P16-10): 컴포넌트와 오리진 사이의 분리, 출처 문서를 제외한 통지, 불투명 오리진, 수거 가능성.
 */
public class SessionStorageTest {

    /**
     * Records notifications for tests.
     * <p>
     * 시험용으로 통지를 기록한다.
     */
    private static final class Recorder implements StorageListener {
        /**
         * Origin the recorder listens for.
         * <p>
         * 기록기가 듣는 오리진.
         */
        private final String origin;

        /**
         * Received notifications as text.
         * <p>
         * 받은 통지(문자열).
         */
        final List<String> events = new ArrayList<String>();

        /**
         * Creates a recorder.
         * <p>
         * 기록기를 만든다.
         *
         * @param origin origin to listen for / 들을 오리진
         */
        Recorder(String origin) {
            this.origin = origin;
        }

        /**
         * Returns the origin.
         * <p>
         * 오리진을 반환한다.
         *
         * @return origin / 오리진
         */
        public String getOrigin() {
            return origin;
        }

        /**
         * Records a change.
         * <p>
         * 변경을 기록한다.
         *
         * @param key      key / 키
         * @param oldValue old value / 이전 값
         * @param newValue new value / 새 값
         * @param source   source document / 출처 문서
         */
        public void storageChanged(String key, String oldValue, String newValue, Object source) {
            events.add(key + ":" + oldValue + "->" + newValue);
        }
    }

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SessionStorageTest() {
    }

    /**
     * P16-02/P16-10: two components never see each other's sessionStorage; two origins of one component are separate.
     * <p>
     * P16-02/P16-10: 두 컴포넌트는 서로의 sessionStorage 를 보지 못하고 한 컴포넌트의 두 오리진은 분리된다.
     */
    public void testIsolation() {
        SessionStorage one = new SessionStorage();
        SessionStorage two = new SessionStorage();
        Origin a = Origin.of("http://a.test/x");
        Origin b = Origin.of("http://b.test/");
        one.forOrigin(a, null).setItem("k", "1");
        Check.isNull(two.forOrigin(a, null).getItem("k"), "another component");
        Check.isNull(one.forOrigin(b, null).getItem("k"), "another origin of the same component");
        Check.equal("1", one.forOrigin(Origin.of("http://a.test/other/page"), null).getItem("k"), "same origin, other path");
        Check.isNull(one.forOrigin(Origin.of("https://a.test/"), null).getItem("k"), "other scheme is another origin");
        Check.equal("1", one.forOrigin(Origin.of("http://a.test:80/"), null).getItem("k"), "the default port is normalized");
        Check.isNull(one.forOrigin(Origin.of("http://a.test:8080/"), null).getItem("k"), "another port is another origin");
    }

    /**
     * P16-01: the Java view returns copies of all items of all origins in a stable order.
     * <p>
     * P16-01: Java 조회는 모든 오리진의 모든 항목을 안정된 순서로 복사해 반환한다.
     */
    public void testEntriesAcrossOrigins() {
        SessionStorage s = new SessionStorage();
        s.forOrigin(Origin.of("http://b.test/"), null).setItem("z", "1");
        s.forOrigin(Origin.of("http://a.test/"), null).setItem("y", "2");
        s.forOrigin(Origin.of("http://a.test/"), null).setItem("x", "3");
        List<StorageEntry> list = s.entries();
        Check.equal(3, list.size(), "all items");
        Check.equal("http://a.test", list.get(0).getOrigin(), "sorted by origin");
        Check.equal("x", list.get(0).getKey(), "then by key");
        Check.equal("y", list.get(1).getKey(), "second");
        Check.equal("http://b.test", list.get(2).getOrigin(), "third origin");
        s.forOrigin(Origin.of("http://a.test/"), null).setItem("x", "changed");
        Check.equal("3", list.get(0).getValue(), "the copy does not change");
        s.clear();
        Check.equal(0, s.entries().size(), "clear removes everything");
        Check.equal(3, list.size(), "and the copy still has three");
    }

    /**
     * P16-08: other documents of the component are told about a change; the source document is not; unchanged writes are silent.
     * <p>
     * P16-08: 컴포넌트의 다른 문서는 변경을 통지받고 출처 문서는 통지받지 않으며 값이 같은 쓰기는 조용하다.
     */
    public void testNotifications() {
        SessionStorage s = new SessionStorage();
        Origin o = Origin.of("http://a.test/");
        Recorder r1 = new Recorder("http://a.test");
        Recorder r2 = new Recorder("http://a.test");
        Recorder other = new Recorder("http://b.test");
        s.addListener(r1);
        s.addListener(other);
        WebStorage w = s.forOrigin(o, r2);
        s.addListener(r2);
        w.setItem("k", "v");
        Check.equal(1, r1.events.size(), "another document of the origin is told");
        Check.equal("k:null->v", r1.events.get(0), "event content");
        Check.equal(0, r2.events.size(), "the document that changed is not told");
        Check.equal(0, other.events.size(), "another origin is not told");
        w.setItem("k", "v");
        Check.equal(1, r1.events.size(), "an unchanged value is not reported");
        w.setItem("k", "w");
        Check.equal("k:v->w", r1.events.get(1), "old and new value");
        w.removeItem("k");
        Check.equal("k:w->null", r1.events.get(2), "removal");
        w.removeItem("k");
        Check.equal(3, r1.events.size(), "removing a missing key is silent");
        w.setItem("a", "1");
        w.clear();
        Check.equal("null:null->null", r1.events.get(r1.events.size() - 1), "clear is reported with a null key");
        int count = r1.events.size();
        w.clear();
        Check.equal(count, r1.events.size(), "clearing an empty storage is silent");
        s.removeListener(r1);
        w.setItem("q", "1");
        Check.equal(count, r1.events.size(), "a removed listener hears nothing");
    }

    /**
     * P16-07: an opaque origin (about:blank, about:text) has no storage and fails with SecurityError.
     * <p>
     * P16-07: 불투명 오리진(about:blank, about:text)에는 저장소가 없으며 SecurityError 로 실패한다.
     */
    public void testOpaqueOrigin() {
        final SessionStorage s = new SessionStorage();
        Throwable t = Check.throwsException(DomException.class, new Block() {
            /**
             * Asks for the storage of an opaque origin.
             * <p>
             * 불투명 오리진의 저장소를 요청한다.
             */
            public void run() {
                s.forOrigin(Origin.opaque(), null);
            }
        }, "opaque origin");
        Check.equal("SecurityError", ((DomException) t).getName(), "error name");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Asks for the storage of a null origin.
             * <p>
             * null 오리진의 저장소를 요청한다.
             */
            public void run() {
                s.forOrigin(null, null);
            }
        }, "null origin");
    }

    /**
     * P16-07: the quota of one origin applies to the session storage, and a failed write keeps the old data.
     * <p>
     * P16-07: 한 오리진의 쿼터는 세션 저장소에도 적용되며 실패한 쓰기는 옛 데이터를 유지한다.
     */
    public void testQuota() {
        SessionStorage s = new SessionStorage();
        final WebStorage w = s.forOrigin(Origin.of("http://a.test/"), null);
        char[] big = new char[StorageLimits.MAX_ORIGIN_CHARS - 2];
        java.util.Arrays.fill(big, 'x');
        w.setItem("k", new String(big));
        Check.throwsException(StorageQuotaException.class, new Block() {
            /**
             * Adds another key beyond the quota.
             * <p>
             * 쿼터를 넘는 키를 더 추가한다.
             */
            public void run() {
                w.setItem("more", "data");
            }
        }, "over the quota");
        Check.equal(1, w.getLength(), "nothing was added");
        s.forOrigin(Origin.of("http://b.test/"), null).setItem("k", "small");
        Check.equal("small", s.forOrigin(Origin.of("http://b.test/"), null).getItem("k"), "the quota is per origin");
    }

    /**
     * P16-02/P21-06: a session storage with listeners and views can be collected once nothing else refers to it.
     * <p>
     * P16-02/P21-06: 리스너와 뷰가 있는 세션 저장소도 다른 곳에서 참조하지 않으면 수거될 수 있다.
     *
     * @throws Exception if the wait is interrupted / 대기가 중단되면
     */
    public void testCollectable() throws Exception {
        SessionStorage s = new SessionStorage();
        WebStorage w = s.forOrigin(Origin.of("http://a.test/"), null);
        w.setItem("k", "v");
        s.addListener(new Recorder("http://a.test"));
        WeakReference<SessionStorage> ref = new WeakReference<SessionStorage>(s);
        s = null;
        w = null;
        long end = System.currentTimeMillis() + 30000;
        // 수거될 때까지 GC 를 반복 요청하며 기다린다.
        while (ref.get() != null && System.currentTimeMillis() < end) {
            System.gc();
            Thread.sleep(50);
        }
        Check.isNull(ref.get(), "the session storage was collected");
    }
}
