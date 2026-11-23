package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;

/**
 * A {@link ScriptSource} that also knows the browser rules of module scripts: the request uses CORS mode (a cross-origin module must be
 * allowed by the server), cookies are sent to the page's own origin only unless the page asked for credentials
 * (<code>crossorigin="use-credentials"</code>), the response must have a JavaScript MIME type, and the final URL after redirects is
 * reported so that the module's relative imports resolve against it. A source that does not implement it is used for modules through
 * {@link ScriptSource#fetch(String)} without these checks (tests and simple hosts).
 * <p>
 * 모듈 스크립트의 브라우저 규칙까지 아는 {@link ScriptSource}: 요청은 CORS 모드를 쓰고(교차 오리진 모듈은 서버가 허용해야 한다), 페이지가 자격
 * 증명을 요청하지(<code>crossorigin="use-credentials"</code>) 않으면 쿠키는 페이지 자신의 오리진에만 보내며, 응답은 JavaScript MIME 타입이어야
 * 하고, 리디렉션 뒤의 최종 URL을 알려 모듈의 상대 import 가 그 URL을 기준으로 해석되게 한다. 이를 구현하지 않은 공급원은
 * {@link ScriptSource#fetch(String)} 로 이런 검사 없이 모듈에 쓰인다(시험과 단순한 호스트).
 */
public interface ModuleScriptSource extends ScriptSource {

    /**
     * Fetches a module script or a module that a module imports.
     * <p>
     * 모듈 스크립트나 모듈이 import 하는 모듈을 가져온다.
     *
     * @param url            the absolute URL / 절대 URL
     * @param useCredentials true to send cookies to every origin (<code>crossorigin="use-credentials"</code>), false to send them to the
     *                       page's own origin only / 모든 오리진에 쿠키를 보내려면 true(<code>crossorigin="use-credentials"</code>), 페이지 자신의
     *                       오리진에만 보내려면 false
     * @return the text and the final URL / 텍스트와 최종 URL
     * @throws IOException if the request fails, the server refuses the origin or the MIME type is not JavaScript / 요청이 실패하거나 서버가 오리진을
     *                     거부하거나 MIME 타입이 JavaScript 가 아니면
     */
    ScriptText fetchModule(String url, boolean useCredentials) throws IOException;
}
