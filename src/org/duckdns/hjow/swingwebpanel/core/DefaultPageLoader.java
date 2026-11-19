package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.content.error.ErrorKind;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPage;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPageBuilder;
import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;

/**
 * The loader used until the real network/content/rendering pipeline is connected (plan stages P04 to P09). It honestly
 * handles what it can: <code>about:blank</code> becomes the empty page and <code>about:text</code> keeps the given source
 * unchanged. Addresses that need the network get a clearly marked error page that says loading is not implemented yet
 * (never a fake success), and addresses with a forbidden scheme get the "unsupported address" error page without any
 * connection being made (API contract 5.2).
 * <p>
 * 실제 네트워크/콘텐츠/렌더링 파이프라인이 연결되기 전(계획 P04~P09 단계)까지 쓰는 로더. 할 수 있는 것은 정직하게 처리한다:
 * <code>about:blank</code> 는 빈 페이지가 되고 <code>about:text</code> 는 받은 원문을 그대로 보관한다. 네트워크가 필요한 주소에는
 * 아직 구현되지 않았다고 분명히 표시한 오류 페이지를 주며(거짓 성공 금지), 금지된 스킴의 주소에는 연결 없이 "지원하지 않는 주소"
 * 오류 페이지를 준다(API 계약 5.2).
 */
public final class DefaultPageLoader implements PageLoader {

    /**
     * HTTP status shown for addresses whose scheme is not allowed.
     * <p>
     * 허용되지 않은 스킴의 주소에 표시하는 HTTP 상태.
     */
    public static final int STATUS_UNSUPPORTED_ADDRESS = 400;

    /**
     * HTTP status shown for pages whose loading is not implemented yet.
     * <p>
     * 아직 구현되지 않아 읽을 수 없는 페이지에 표시하는 HTTP 상태.
     */
    public static final int STATUS_NOT_IMPLEMENTED = 501;

    /**
     * Creates the default loader.
     * <p>
     * 기본 로더를 만든다.
     */
    public DefaultPageLoader() {
    }

    /**
     * Loads a page as described in the class description.
     * <p>
     * 클래스 설명대로 페이지를 읽는다.
     *
     * @param request what to load / 읽을 대상
     * @return the confirmed page / 확정된 페이지
     */
    public PageSnapshot load(NavigationRequest request) {
        String url = request.getUrl();
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        switch (kind) {
        case ABOUT_BLANK:
            return PageSnapshot.blank();
        case ABOUT_TEXT:
            return PageSnapshot.text(request.getContent() == null ? "" : request.getContent(),
                    request.getMimeType() == null ? "text/plain" : request.getMimeType());
        case HTTP:
        case HTTPS:
        case FTP:
        case CLASSPATH:
            return errorPage(url, STATUS_NOT_IMPLEMENTED, "Not implemented",
                    "Loading this kind of address is not implemented yet in this build.");
        default:
            return errorPage(url, STATUS_UNSUPPORTED_ADDRESS, "Unsupported address",
                    "This address is not supported. Only http, https, ftp and classpath addresses can be loaded.");
        }
    }

    /**
     * Builds a small error page. Every value that comes from outside is escaped.
     * <p>
     * 작은 오류 페이지를 만든다. 바깥에서 온 값은 모두 이스케이프한다.
     *
     * @param url     requested address / 요청한 주소
     * @param status  status code to show / 표시할 상태 코드
     * @param title   short title / 짧은 제목
     * @param message explanation / 설명
     * @return the error snapshot / 오류 스냅샷
     */
    private static PageSnapshot errorPage(String url, int status, String title, String message) {
        ErrorPage page = ErrorPageBuilder.build(status == STATUS_UNSUPPORTED_ADDRESS ? ErrorKind.BLOCKED_SCHEME : ErrorKind.LOAD_FAILED, url, status, title, message, "");
        return PageSnapshot.error(url, page.getHtml(), page.getMimeType(), page.getStatus()).withTitle(page.getHeading());
    }
}
