package org.duckdns.hjow.swingwebpanel.core;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.storage.LocalStorageManager;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.storage.StorageListener;
import org.duckdns.hjow.swingwebpanel.storage.WebStorage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the connection between {@link BrowserSession} and the Web Storage classes (plan items P16-02, P16-09, P16-10, P21-06):
 * copies returned by the Java getters, the effect of <code>clean()</code> and <code>dispose()</code> on the shared localStorage, notifications
 * to another component and collectability of a component whose sessionStorage holds data.
 * <p>
 * {@link BrowserSession} 과 Web Storage 클래스의 연결 시험(계획 P16-02, P16-09, P16-10, P21-06): Java 조회 메서드가 반환하는 복사본,
 * <code>clean()</code> 과 <code>dispose()</code> 가 공용 localStorage 에 미치는 영향, 다른 컴포넌트로의 통지, sessionStorage 에 데이터가 있는 컴포넌트의 수거 가능성.
 */
public class StorageIntegrationTest {

    /**
     * Shared store file of the case.
     * <p>
     * 사례의 공용 저장소 파일.
     */
    private File storeFile;

    /**
     * Manager shared by the sessions of the case (never the process-wide one).
     * <p>
     * 사례의 세션들이 공유하는 관리자(프로세스 공용 관리자가 아님).
     */
    private LocalStorageManager shared;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StorageIntegrationTest() {
    }

    /**
     * Creates a private store file and manager for each case.
     * <p>
     * 사례마다 전용 저장소 파일과 관리자를 만든다.
     *
     * @throws Exception if the file cannot be created / 파일을 만들 수 없으면
     */
    public void setUp() throws Exception {
        File dir = File.createTempFile("swp-integ-", ".d");
        dir.delete();
        dir.mkdirs();
        storeFile = new File(dir, "store.dat");
        shared = new LocalStorageManager(storeFile);
    }

    /**
     * Removes the files of the case.
     * <p>
     * 사례의 파일을 지운다.
     */
    public void tearDown() {
        File dir = storeFile.getParentFile();
        File[] children = dir.listFiles();
        if (children != null) {
            // 디렉터리 안의 파일을 하나씩 지운다.
            for (File c : children) {
                c.delete();
            }
        }
        dir.delete();
    }

    /**
     * Creates a session that uses the shared manager.
     * <p>
     * 공용 관리자를 쓰는 세션을 만든다.
     *
     * @return session / 세션
     */
    private BrowserSession newSession() {
        return new BrowserSession(new FakeLoader(), new TestSupport.Recorder(), new org.duckdns.hjow.swingwebpanel.net.CookieStore(), shared);
    }

    /**
     * P16-09/P16-10: sessionStorage is separate per component, localStorage is shared, and the Java getters return independent copies.
     * <p>
     * P16-09/P16-10: sessionStorage 는 컴포넌트마다 분리되고 localStorage 는 공유되며 Java 조회 메서드는 독립된 복사본을 반환한다.
     */
    public void testSessionSeparatedLocalShared() {
        BrowserSession one = newSession();
        BrowserSession two = newSession();
        try {
            Origin o = Origin.of("http://a.test/");
            one.getSessionStorageStore().forOrigin(o, null).setItem("s", "1");
            one.getLocalStorageManager().forOrigin(o, null).setItem("l", "2");
            Check.equal(1, one.getSessionStorage().size(), "session item of the first component");
            Check.equal(0, two.getSessionStorage().size(), "the second component has no session item");
            Check.equal(1, two.getLocalStorage().size(), "the second component sees the local item");
            List<StorageEntry> copy = one.getLocalStorage();
            one.getLocalStorageManager().forOrigin(o, null).setItem("l", "changed");
            Check.equal("2", copy.get(0).getValue(), "the copy of the Java getter does not follow later changes");
            Check.equal("changed", two.getLocalStorage().get(0).getValue(), "a new call shows the new value");
            Check.equal("http://a.test", copy.get(0).getOrigin(), "origin in the entry");
        } finally {
            one.dispose();
            two.dispose();
        }
    }

    /**
     * P16-09: clean() of one component clears its sessionStorage and the shared localStorage; the other component notices and keeps its own
     * sessionStorage.
     * <p>
     * P16-09: 한 컴포넌트의 clean() 은 자신의 sessionStorage 와 공용 localStorage 를 비운다. 다른 컴포넌트는 이를 알아차리며 자신의 sessionStorage 는 유지한다.
     */
    public void testCleanClearsSharedLocalStorage() {
        BrowserSession one = newSession();
        BrowserSession two = newSession();
        try {
            Origin o = Origin.of("http://a.test/");
            one.getSessionStorageStore().forOrigin(o, null).setItem("s", "1");
            two.getSessionStorageStore().forOrigin(o, null).setItem("s", "2");
            one.getLocalStorageManager().forOrigin(o, null).setItem("l", "3");
            final List<String> heard = new java.util.ArrayList<String>();
            StorageListener listener = new StorageListener() {
                /**
                 * Origin of the listener.
                 * <p>
                 * 리스너의 오리진.
                 *
                 * @return origin / 오리진
                 */
                public String getOrigin() {
                    return "http://a.test";
                }

                /**
                 * Records the event.
                 * <p>
                 * 이벤트를 기록한다.
                 *
                 * @param key      key / 키
                 * @param oldValue old value / 이전 값
                 * @param newValue new value / 새 값
                 * @param source   source / 출처
                 */
                public void storageChanged(String key, String oldValue, String newValue, Object source) {
                    heard.add(key + ":" + oldValue + "->" + newValue);
                }
            };
            two.getLocalStorageManager().addListener(listener);
            one.clean();
            Check.equal(0, one.getSessionStorage().size(), "sessionStorage of the cleaned component");
            Check.equal(0, one.getLocalStorage().size(), "the shared localStorage is empty");
            Check.equal(0, two.getLocalStorage().size(), "the other component sees the empty localStorage");
            Check.equal(1, two.getSessionStorage().size(), "the other component keeps its sessionStorage");
            Check.equal(1, heard.size(), "the other component was notified");
            Check.equal("null:null->null", heard.get(0), "as a clear event");
            Check.equal("about:blank", one.getPage(), "the cleaned component shows the blank page");
            Check.isFalse(one.isDisposed(), "clean() does not dispose the component");
        } finally {
            one.dispose();
            two.dispose();
        }
    }

    /**
     * P16-09/P01-16: after dispose() the Java getters return empty collections and a disposed component cannot write.
     * <p>
     * P16-09/P01-16: dispose() 뒤에 Java 조회 메서드는 빈 컬렉션을 반환하며 폐기된 컴포넌트는 쓰지 못한다.
     */
    public void testDisposedGettersAreEmpty() {
        BrowserSession one = newSession();
        Origin o = Origin.of("http://a.test/");
        one.getSessionStorageStore().forOrigin(o, null).setItem("s", "1");
        one.getLocalStorageManager().forOrigin(o, null).setItem("l", "2");
        one.dispose();
        Check.equal(0, one.getLocalStorage().size(), "localStorage view after dispose");
        Check.equal(0, one.getSessionStorage().size(), "sessionStorage view after dispose");
        Check.equal(0, shared.entries().size(), "dispose clears the shared localStorage like clean()");
        one.dispose();
    }

    /**
     * P16-02/P21-06: a component that holds sessionStorage data and shares a manager can be collected after dispose; the manager does not
     * keep it alive.
     * <p>
     * P16-02/P21-06: sessionStorage 데이터를 가지고 관리자를 공유하는 컴포넌트는 dispose 뒤에 수거될 수 있으며 관리자가 그것을 붙잡지 않는다.
     *
     * @throws Exception if the wait is interrupted / 대기가 중단되면
     */
    public void testComponentIsCollectable() throws Exception {
        BrowserSession s = newSession();
        WebStorage w = s.getSessionStorageStore().forOrigin(Origin.of("http://a.test/"), null);
        w.setItem("k", "v");
        s.getLocalStorageManager().forOrigin(Origin.of("http://a.test/"), s).setItem("k", "v");
        WeakReference<BrowserSession> ref = new WeakReference<BrowserSession>(s);
        s.dispose();
        s = null;
        w = null;
        long end = System.currentTimeMillis() + 30000;
        // 수거될 때까지 GC 를 반복 요청하며 기다린다.
        while (ref.get() != null && System.currentTimeMillis() < end) {
            System.gc();
            Thread.sleep(50);
        }
        Check.isNull(ref.get(), "the session was collected although the shared manager still exists");
    }
}
