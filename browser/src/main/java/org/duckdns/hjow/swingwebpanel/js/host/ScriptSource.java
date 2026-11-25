package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;

/**
 * Supplies the text of external scripts and modules to the page (the component fetches them through its request layer, so User-Agent, extra
 * headers, cookies, SSL hook, scheme limits and cancellation apply). The call may block the calling thread.
 * <p>
 * 페이지에 외부 스크립트와 모듈의 텍스트를 공급한다(컴포넌트가 요청 계층으로 가져오므로 User-Agent, 추가 헤더, 쿠키, SSL 훅, 스킴 제한, 취소가
 * 적용된다). 호출한 스레드를 막을 수 있다.
 */
public interface ScriptSource {

    /**
     * A host source that supplies response metadata for classic scripts.
     * <p>
     * 일반 스크립트의 응답 정보도 제공하는 호스트 공급원.
     */
    interface WithResponse extends ScriptSource {
        /**
         * Fetches a classic script with its final URL and response policy.
         * <p>
         * 최종 URL과 응답 정책을 담아 일반 스크립트를 가져온다.
         *
         * @param url the requested URL / 요청한 URL
         * @return the fetched script / 읽은 스크립트
         * @throws IOException if the request fails / 요청 실패 시
         */
        ScriptText fetchScript(String url) throws IOException;

        /**
         * Fetches a worker's classic script using the worker's own embedder policy.
         * <p>
         * 워커의 삽입 정책을 사용해 일반 스크립트를 가져온다.
         *
         * @param url the requested URL / 요청한 URL
         * @param policy the requester's policy / 요청자의 삽입 정책
         * @return the fetched script / 읽은 스크립트
         * @throws IOException if the request fails / 요청 실패 시
         */
        ScriptText fetchScript(String url, CrossOriginPolicy.Embedder policy) throws IOException;
    }

    /**
     * Fetches the text of a script.
     * <p>
     * 스크립트의 텍스트를 가져온다.
     *
     * @param url the absolute URL / 절대 URL
     * @return the source text / 원문
     * @throws IOException if the script cannot be fetched (the page then gets an <code>error</code> event on the script element) / 가져올 수 없으면(페이지는 script 요소에 <code>error</code> 이벤트를 받는다)
     */
    String fetch(String url) throws IOException;
}
