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
     * A loader that can fetch JavaScript and JSON with separate host rules for each type.
     * <p>
     * JavaScript와 JSON에 서로 다른 호스트 규칙을 적용해 가져올 수 있는 로더.
     */
    interface WithAttributes extends ModuleLoader {

        /**
         * Fetches a resolved module with its validated import attributes.
         * <p>
         * 해석된 모듈을 검사된 가져오기 속성에 맞게 가져온다.
         *
         * @param request resolved module key and attributes / 해석된 모듈 키와 속성
         * @return module source / 모듈 소스
         * @throws JsException if the host cannot load the requested module / 요청 모듈을 불러올 수 없는 경우
         */
        String fetch(ModuleRequest request) throws JsException;
    }

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
