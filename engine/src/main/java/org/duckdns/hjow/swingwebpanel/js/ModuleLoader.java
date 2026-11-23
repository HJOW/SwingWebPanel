package org.duckdns.hjow.swingwebpanel.js;

/**
 * The host hook that resolves and loads modules (HostResolveImportedModule, ECMA-262 6th edition 15.2.1.17). The engine never
 * touches the network or the file system itself: for <code>import x from 'spec'</code> it asks the host to turn the specifier
 * into a unique key and to supply the source text. The same key must always denote the same module.
 * <p>
 * 모듈을 해석하고 불러오는 호스트 연결 지점(HostResolveImportedModule, ECMA-262 6판 15.2.1.17). 엔진은 네트워크나 파일 시스템에
 * 직접 접근하지 않는다. <code>import x from 'spec'</code> 에 대해 호스트에게 지정자를 고유한 키로 바꾸고 소스 텍스트를 달라고 요청한다.
 * 같은 키는 항상 같은 모듈을 가리켜야 한다.
 */
public interface ModuleLoader {

    /**
     * Resolves a module specifier relative to the importing module.
     * <p>
     * 가져오는 모듈을 기준으로 모듈 지정자를 해석한다.
     *
     * @param specifier the string after <code>from</code> / <code>from</code> 뒤의 문자열
     * @param referrer  the key of the importing module, or null for the entry module / 가져오는 모듈의 키. 진입 모듈이면 null
     * @return the unique module key (for example an absolute URL) / 고유한 모듈 키(예: 절대 URL)
     * @throws JsException to report a failure to the script / 스크립트에 실패를 알리려면 던진다
     */
    String resolve(String specifier, String referrer) throws JsException;

    /**
     * Fetches the source text of a module.
     * <p>
     * 모듈의 소스 텍스트를 가져온다.
     *
     * @param key the key returned by {@link #resolve(String, String)} / {@link #resolve(String, String)} 가 반환한 키
     * @return the source text / 소스 텍스트
     * @throws JsException to report a failure to the script / 스크립트에 실패를 알리려면 던진다
     */
    String fetch(String key) throws JsException;
}
