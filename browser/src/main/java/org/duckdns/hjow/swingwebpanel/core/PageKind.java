package org.duckdns.hjow.swingwebpanel.core;

/**
 * The kind of a confirmed page: how its content came to be.
 * <p>
 * 확정된 페이지의 종류: 콘텐츠가 어떻게 만들어졌는지를 나타낸다.
 */
public enum PageKind {

    /**
     * The empty page (<code>about:blank</code>).
     * <p>
     * 빈 페이지(<code>about:blank</code>).
     */
    BLANK,

    /**
     * Content given by the host with <code>setContent</code> (<code>about:text</code>).
     * <p>
     * 호스트가 <code>setContent</code> 로 넣은 콘텐츠(<code>about:text</code>).
     */
    TEXT,

    /**
     * A page loaded from a URL (http, https, ftp or classpath).
     * <p>
     * URL(http, https, ftp, classpath)에서 읽은 페이지.
     */
    URL,

    /**
     * A generated error page (network failure, HTTP error, blocked address).
     * <p>
     * 생성한 오류 페이지(네트워크 실패, HTTP 오류, 차단된 주소).
     */
    ERROR
}
