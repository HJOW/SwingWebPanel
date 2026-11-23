package org.duckdns.hjow.swingwebpanel.core;

/**
 * Build-time constants that identify the SwingWebPanel library.
 * <p>
 * Use {@link #VERSION} to show the library version in an about box or log.
 * This class cannot be instantiated.
 * <p>
 * SwingWebPanel 라이브러리를 식별하는 상수 모음.
 * 정보 창이나 로그에 라이브러리 버전을 표시하려면 {@link #VERSION} 을 사용한다.
 * 이 클래스는 인스턴스를 만들 수 없다.
 */
public final class Version {

    /**
     * Product name.
     * <p>
     * 제품 이름.
     */
    public static final String PRODUCT_NAME = "SwingWebPanel";

    /**
     * Library version string (major.minor.patch).
     * <p>
     * 라이브러리 버전 문자열(주.부.수).
     */
    public static final String VERSION = "0.1.0";

    /**
     * Private constructor: this is a constants holder.
     * <p>
     * 비공개 생성자: 상수 보관용 클래스이다.
     */
    private Version() {
    }
}
