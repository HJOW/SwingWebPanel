package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.io.IOException;

/**
 * The plug-in through which style sheet sources are loaded: <code>@import</code> rules and <code>&lt;link rel="stylesheet"&gt;</code> elements ask it for the
 * text of a style sheet. The host implements it on top of the common request layer (User-Agent, extra headers, cookies, the allowed URL schemes, the
 * cache), so that style sheets use the same policies as every other resource. The implementation decodes the bytes with {@link StyleSheetDecoder}.
 * <p>
 * 스타일 시트 출처를 읽는 플러그인: <code>@import</code> 규칙과 <code>&lt;link rel="stylesheet"&gt;</code> 요소가 스타일 시트의 텍스트를 요청한다. 호스트가 공통 요청 계층(User-Agent,
 * 추가 헤더, 쿠키, 허용 URL 스키마, 캐시) 위에 구현하여 스타일 시트도 다른 모든 리소스와 같은 정책을 쓰게 한다. 구현은 {@link StyleSheetDecoder} 로 바이트를 디코딩한다.
 */
public interface StyleSheetLoader {

    /**
     * Loads a style sheet.
     * <p>
     * 스타일 시트를 읽는다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @return the loaded text and the final URL (after redirects) / 읽은 텍스트와 (리디렉션 뒤의) 최종 URL
     * @throws IOException if the sheet cannot be loaded (network error, error status, blocked scheme) / 읽을 수 없으면(네트워크 오류, 오류 상태, 차단된 스키마)
     */
    LoadedStyleSheet load(String absoluteUrl) throws IOException;
}
