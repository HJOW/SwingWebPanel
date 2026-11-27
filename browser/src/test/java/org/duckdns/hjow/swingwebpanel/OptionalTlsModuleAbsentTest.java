package org.duckdns.hjow.swingwebpanel;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Exercises the standalone browser when the optional TLS JAR is absent.
 * / 선택 TLS JAR이 없는 브라우저 단독 실행을 검증한다.
 */
public final class OptionalTlsModuleAbsentTest {

    /** Creates a regression test instance. / 회귀 시험 인스턴스를 만든다. */
    public OptionalTlsModuleAbsentTest() {
    }

    /**
     * An unavailable module cannot be enabled; the component can still navigate.
     * / 없는 모듈은 켤 수 없으며 컴포넌트는 계속 탐색할 수 있다.
     *
     * @throws Exception if the document wait is interrupted / 문서 대기가 중단됐을 때
     */
    public void testStandaloneBrowser() throws Exception {
        Check.isFalse(SwingWebPanel.isTls13ModuleAvailable(), "the browser test class path contains no optional TLS JAR");
        final SwingWebPanel panel = new SwingWebPanel();
        try {
            Check.isFalse(panel.isTls13ModuleEnabled(), "the initial setting is off");
            try {
                panel.setTls13ModuleEnabled(true);
                throw new AssertionError("opening a missing module succeeded");
            } catch (IllegalStateException expected) {
                Check.isFalse(panel.isTls13ModuleEnabled(), "the failed call kept the original setting");
            }
            panel.setContent("<title>standalone</title>", "text/html");
            long end = System.currentTimeMillis() + 5000;
            // 독립 브라우저의 문서 작업을 기다린다.
            while (!"standalone".equals(panel.getTitle()) && System.currentTimeMillis() < end) {
                Thread.sleep(10);
            }
            Check.equal("standalone", panel.getTitle(), "the standalone browser still navigates");
        } finally {
            panel.dispose();
        }
    }
}
