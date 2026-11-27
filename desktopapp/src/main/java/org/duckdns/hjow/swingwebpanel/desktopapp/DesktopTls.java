package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.util.concurrent.atomic.AtomicBoolean;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

/**
 * Selects the optional TLS module for this application's Java 6 and 7 panels.
 * / 이 응용 프로그램의 Java 6·7 패널에 선택 TLS 모듈을 지정한다.
 */
public final class DesktopTls {

    /** Whether the application has already reported a missing module. / 모듈을 쓸 수 없다고 이미 알렸는지 여부. */
    private static final AtomicBoolean reported = new AtomicBoolean();

    /** Utility class. / 인스턴스를 만들지 않는다. */
    private DesktopTls() {
    }

    /**
     * Checks the Java specification version. / Java 규격 버전을 확인한다.
     *
     * @param version specification version or null / 규격 버전 또는 null
     * @return true for Java 6 or 7 / Java 6 또는 7이면 true
     */
    static boolean isEligible(String version) {
        return "1.6".equals(version) || "1.7".equals(version);
    }

    /**
     * Reads the runtime version using the same rule as the app startup. / 앱 시작과 같은 규칙으로 실행 환경의 버전을 읽는다.
     *
     * @return whether the current JRE is eligible / 현재 JRE가 대상인지 여부
     */
    static boolean isEligibleRuntime() {
        try {
            return isEligible(System.getProperty("java.specification.version"));
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Configures a newly created panel and reports an initialization failure at most once.
     * / 새 패널을 설정하고 초기화 실패를 한 번만 알린다.
     *
     * @param panel the new panel / 새 패널
     */
    public static void configure(SwingWebPanel panel) {
        if (!isEligibleRuntime()) {
            return;
        }
        try {
            // 이 호출이 모듈 가용 여부와 초기화 성공을 함께 확인한다.
            panel.setTls13ModuleEnabled(true);
        } catch (IllegalStateException e) {
            if (reported.compareAndSet(false, true)) {
                System.err.println(Texts.of("TLS module unavailable; using the Java runtime TLS implementation.",
                        "TLS 모듈을 불러올 수 없어 Java 실행 환경의 TLS를 사용합니다."));
            }
        }
    }
}
