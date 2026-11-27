package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WindowRequest;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13Config;
import org.duckdns.hjow.swingwebpanel.tls13.Tls13SocketFactory;
import org.duckdns.hjow.swingwebpanel.tls13.proto.JsseFixture;
import org.duckdns.hjow.swingwebpanel.tls13.proto.JsseServer;

/**
 * Browser integration regression using the optional module and independent loopback JSSE.
 * / 선택 모듈과 독립 루프백 JSSE로 브라우저 연동을 확인하는 회귀 시험.
 */
public final class OptionalTlsBrowserTest {

    /** A modern server suite unavailable in the JRE 6 JSSE. / JRE 6 JSSE에 없는 현대 서버 스위트. */
    private static final String AES_13 = "TLS_AES_128_GCM_SHA256";

    /** A classic suite absent from the optional module. / 선택 모듈이 지원하지 않는 옛 스위트. */
    private static final String RSA_CBC = "TLS_RSA_WITH_AES_128_CBC_SHA";

    /** Observes the SSL extension point and keeps any script popup for inspection. / SSL 확장 지점을 관찰하고 스크립트 팝업을 보관한다. */
    private static final class Panel extends SwingWebPanel {

        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /** Whether the override delegates to the component. / 재정의가 컴포넌트에 위임하는지 여부. */
        final boolean callSuper;

        /** Factory that trusts only the generated loopback root. / 생성한 루프백 루트만 신뢰하는 팩터리. */
        final SSLSocketFactory testFactory;

        /** Most recent factory selected by the base hook, before test trust is installed. / 시험 신뢰 설치 전에 기본 훅이 고른 최신 팩터리. */
        volatile SSLSocketFactory observed;

        /** A window opened by the script, or null until then. / 스크립트가 연 창. 열기 전에는 null. */
        volatile SwingWebPanel popup;

        /**
         * Creates the component. / 컴포넌트를 만든다.
         *
         * @param callSuper whether to exercise the base hook / 기본 훅을 쓸지 여부
         * @param testFactory optional private trust / 선택 시험 신뢰
         */
        Panel(boolean callSuper, SSLSocketFactory testFactory) {
            this.callSuper = callSuper;
            this.testFactory = testFactory;
        }

        /**
         * Observes the chosen path and replaces only module trust when needed.
         * / 선택된 경로를 관찰하고 필요할 때 모듈의 시험 신뢰만 바꾼다.
         *
         * @param connection HTTPS connection / HTTPS 연결
         * @throws IOException if the hook fails / 훅이 실패했을 때
         */
        protected void configureSsl(HttpsURLConnection connection) throws IOException {
            if (callSuper) {
                super.configureSsl(connection);
            }
            observed = connection.getSSLSocketFactory();
            if (observed instanceof Tls13SocketFactory && testFactory != null) {
                connection.setSSLSocketFactory(testFactory);
            }
        }

        /** Allows the test to keep a popup without a visible frame. / 보이는 창 없이 팝업을 보관하게 한다. */
        protected boolean canOpenPopupWindows() {
            return true;
        }

        /**
         * Keeps the popup panel available for assertions. / 팝업 패널을 확인할 수 있게 보관한다.
         *
         * @param request script navigation request / 스크립트 이동 요청
         */
        protected void openPopupWindow(WindowRequest request) {
            popup = request.getPanel();
        }
    }

    /** Creates a regression test instance. / 회귀 시험 인스턴스를 만든다. */
    public OptionalTlsBrowserTest() {
    }

    /** Tests eligibility for old, modern and missing version values. / 이전·최신·누락 버전 값의 대상 여부를 확인한다. */
    public void testRuntimeSelection() {
        Check.isTrue(DesktopTls.isEligible("1.6"), "Java 6");
        Check.isTrue(DesktopTls.isEligible("1.7"), "Java 7");
        String[] others = { null, "", "1.5", "6", "7", "1.8", "1.8.0_202", "11", "25" };
        // 정확히 1.6과 1.7만 고르는지 확인한다.
        for (String version : others) {
            Check.isFalse(DesktopTls.isEligible(version), "excluded Java specification version " + version);
        }
    }

    /** Verifies the toggle, EDT events, an overriding host and disposal. / 토글, EDT 알림, 호스트 재정의와 폐기를 검증한다. */
    public void testToggleAndLifecycle() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader old = thread.getContextClassLoader();
        try {
            thread.setContextClassLoader(new ClassLoader(null) { });
            Check.isTrue(SwingWebPanel.isTls13ModuleAvailable(), "the library loader is the fallback");
        } finally {
            thread.setContextClassLoader(old);
        }
        Panel panel = new Panel(true, null);
        final List<String> events = new ArrayList<String>();
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_TLS13_MODULE_ENABLED, new PropertyChangeListener() {
            /**
             * Records ordering and dispatch thread. / 순서와 디스패치 스레드를 기록한다.
             *
             * @param change change event / 변경 이벤트
             */
            public void propertyChange(PropertyChangeEvent change) {
                events.add(change.getOldValue() + ">" + change.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "" : "@other"));
            }
        });
        try {
            Check.isFalse(panel.isTls13ModuleEnabled(), "default off even when packaged");
            panel.setTls13ModuleEnabled(true);
            panel.setTls13ModuleEnabled(true);
            panel.clean();
            Check.isTrue(panel.isTls13ModuleEnabled(), "clean preserves the setting");
            panel.setTls13ModuleEnabled(false);
            settle();
            Check.equal("[false>true, true>false]", events.toString(), "changes queued once and on the EDT");
            panel.setTls13ModuleEnabled(true);
        } finally {
            panel.disposeKeepingStorage();
        }
        Check.isFalse(panel.isTls13ModuleEnabled(), "a disposed component reports the default setting");
        try {
            panel.setTls13ModuleEnabled(false);
            throw new AssertionError("disposed panel accepted a configuration change");
        } catch (IllegalStateException expected) {
            // 폐기 뒤에는 값이 같아도 변경 메서드가 거부한다.
        }
    }

    /** Verifies TLS navigation, the CORS sockets and the setting inherited through the popup hook. / TLS 이동, CORS 소켓과 팝업 훅 상속을 검증한다. */
    public void testRequestsAndPopup() throws Exception {
        JsseFixture server = new JsseFixture("TLSv1.3", AES_13, "x25519", "normal", false);
        JsseFixture cors = null;
        Panel panel = null;
        Panel override = null;
        try {
            SSLSocketFactory trusted = new Tls13SocketFactory(Tls13Config.defaults().withKeyStore(JsseFixture.trustKeys()));
            panel = new Panel(true, trusted);
            panel.setTls13ModuleEnabled(true);
            String first = server.base() + "/main";
            panel.setPage(first);
            awaitTitle(panel, JsseServer.TITLE);
            Check.equal(first, panel.getPage(), "top-level page is served over HTTPS");
            Check.isTrue(panel.observed instanceof Tls13SocketFactory, "the default hook installed the module");
            cors = new JsseFixture("TLSv1.3", AES_13, "x25519", "normal", false);
            panel.setPage(server.base() + "/cors-page?" + cors.port());
            awaitTitle(panel, "cors-ok");
            awaitConnections(cors, 2);
            Check.equal("[https, https]", cors.connectionOrder(), "both CORS sockets used HTTPS");
            panel.setPopupBlockingEnabled(false);
            panel.setContent("<title>opener</title><script>window.open('" + server.base() + "/popup');</script>", "text/html");
            long deadline = System.currentTimeMillis() + 10000;
            // 팝업을 호스트에 전달하는 EDT 작업을 기다린다.
            while (panel.popup == null && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.isTrue(panel.popup != null, "the popup was delivered");
            awaitTitle(panel.popup, JsseServer.TITLE);
            Check.equal(server.base() + "/popup", panel.popup.getPage(), "popup used the opener's TLS factory");

            override = new Panel(false, trusted);
            override.setTls13ModuleEnabled(true);
            openAndWait(override, server.base() + "/override");
            Check.isFalse(override.observed instanceof Tls13SocketFactory, "override without super selected JSSE");
            if (DesktopTls.isEligibleRuntime()) {
                Check.isFalse(JsseServer.TITLE.equals(override.getTitle()), "legacy JSSE cannot connect to the TLS 1.3 server");
            }
            panel.setTls13ModuleEnabled(false);
            openAndWait(panel, server.base() + "/disabled");
            Check.isFalse(panel.observed instanceof Tls13SocketFactory, "the next connection uses JSSE again");
            panel.setTls13ModuleEnabled(true);
            panel.disposeKeepingStorage();
            openAndWait(panel.popup, server.base() + "/surviving-popup");
            Check.equal(server.base() + "/surviving-popup", panel.popup.getPage(), "the still-open popup keeps the disposed opener's SSL policy");
            awaitTitle(panel.popup, JsseServer.TITLE);
        } finally {
            if (panel != null) {
                if (panel.popup != null) {
                    panel.popup.disposeKeepingStorage();
                }
                panel.disposeKeepingStorage();
            }
            if (override != null) {
                override.disposeKeepingStorage();
            }
            if (cors != null) {
                cors.close();
            }
            server.close();
        }
    }

    /** Distinguishes module negotiation retry from certificate handling. / 모듈 협상 재시도와 인증서 실패 처리를 구분한다. */
    public void testRetryOrder() throws Exception {
        retry("TLSv1.2", RSA_CBC, 3, "[https, https, http]");
        retry("TLSv1.3", AES_13, 2, "[https, http]");
    }

    /** Checks a module-enabled panel is collectible after disposal. / 모듈을 켠 패널도 폐기 뒤 수거되는지 확인한다. */
    public void testDisposedPanelCanBeCollected() throws Exception {
        WeakReference<SwingWebPanel> disposed = disposedPanel();
        settle();
        long deadline = System.currentTimeMillis() + 5000;
        // EDT와 백그라운드 작업의 참조가 사라지고 GC가 실행되기를 기다린다.
        while (disposed.get() != null && System.currentTimeMillis() < deadline) {
            System.gc();
            Thread.sleep(30);
        }
        Check.isTrue(disposed.get() == null, "the optional module does not retain the disposed panel");
    }

    /**
     * Disposes a module-enabled panel before returning its weak reference. / 모듈을 켠 패널을 폐기하고 약한 참조를 반환한다.
     *
     * @return weak reference to the disposed panel / 폐기한 패널의 약한 참조
     */
    private static WeakReference<SwingWebPanel> disposedPanel() {
        SwingWebPanel panel = new SwingWebPanel();
        panel.setTls13ModuleEnabled(true);
        panel.disposeKeepingStorage();
        return new WeakReference<SwingWebPanel>(panel);
    }

    /**
     * Counts real connections to the same address after a TLS failure.
     * / TLS 실패 이후 같은 주소에 이루어진 실제 연결 수를 센다.
     *
     * @param protocol server protocol / 서버 프로토콜
     * @param suite single server suite / 서버의 유일한 스위트
     * @param count module, optional JSSE, and HTTP attempts / 모듈·선택 JSSE·HTTP 연결 시도 수
     * @param order expected connection schemes / 기대 연결 스킴 순서
     * @throws Exception if checks fail / 확인 실패 시
     */
    private static void retry(String protocol, String suite, int count, String order) throws Exception {
        JsseFixture server = new JsseFixture(protocol, suite, "x25519", "normal", false);
        Panel panel = new Panel(true, null);
        try {
            panel.setTls13ModuleEnabled(true);
            openAndWait(panel, server.base() + "/retry");
            awaitConnections(server, count);
            Check.equal(order, server.connectionOrder(), "HTTPS and HTTP attempt order");
            Check.isTrue(panel.getPage().startsWith("http:"), "reported URL is the final plain HTTP attempt");
            Check.isTrue(panel.observed instanceof Tls13SocketFactory, "the first attempt used the optional path");
        } finally {
            panel.disposeKeepingStorage();
            server.close();
        }
    }

    /**
     * Waits for the server to report an exact number of accepts. / 서버가 지정한 연결 수를 보고하기까지 기다린다.
     *
     * @param server loopback server / 루프백 서버
     * @param expected expected count / 기대 수
     * @throws Exception if interrupted / 대기가 중단됐을 때
     */
    private static void awaitConnections(JsseFixture server, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        // 서버가 받은 연결을 표준 출력으로 전달할 시간을 준다.
        while (server.connections() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(expected, server.connections(), "number of server connection attempts");
    }

    /**
     * Navigates and waits for a committed page or error. / 이동한 뒤 확정된 페이지 또는 오류를 기다린다.
     *
     * @param panel component / 컴포넌트
     * @param url navigation URL / 이동 주소
     * @throws Exception if interrupted / 대기가 중단됐을 때
     */
    private static void openAndWait(SwingWebPanel panel, String url) throws Exception {
        String former = panel.getPage();
        panel.setPage(url);
        long deadline = System.currentTimeMillis() + 15000;
        // 직전 확정 페이지가 교체되고 진행률이 끝날 때까지 기다린다.
        while ((former.equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.isFalse(former.equals(panel.getPage()), "navigation was committed: " + panel.getStatusText());
    }

    /**
     * Waits for the expected title. / 기대한 제목이 올 때까지 기다린다.
     *
     * @param panel component / 컴포넌트
     * @param title expected title / 기대 제목
     * @throws Exception if interrupted / 대기가 중단됐을 때
     */
    private static void awaitTitle(SwingWebPanel panel, String title) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        // 문서 스레드의 파싱과 스크립트 실행이 끝날 때까지 기다린다.
        while (!title.equals(panel.getTitle()) && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(title, panel.getTitle(), "title at " + panel.getPage() + " (" + panel.getStatusText() + ")");
    }

    /** Drains queued EDT work. / 대기 중인 EDT 작업을 처리한다. */
    private static void settle() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Wait barrier. / 앞선 알림을 기다린다. */
            public void run() {
            }
        });
    }
}
