package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.ModuleRequest;
import org.duckdns.hjow.swingwebpanel.js.host.ModuleScriptSource.Credentials;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Module source lookup shared by browser scripting hosts.
 * <p>
 * 브라우저 스크립트 호스트가 공유하는 모듈 소스 조회.
 */
public final class ModuleSources {

    /** Uses static operations only. / 정적 메서드만 사용한다. */
    private ModuleSources() {
    }

    /**
     * Converts a Web IDL RequestCredentials value to a fetch mode.
     * <p>
     * Web IDL RequestCredentials 값을 가져오기 모드로 변환한다.
     *
     * @param value the option string / 옵션 문자열
     * @return credentials mode / 자격 증명 모드
     */
    public static Credentials credentials(String value) {
        if ("same-origin".equals(value)) {
            return Credentials.SAME_ORIGIN;
        }
        if ("include".equals(value)) {
            return Credentials.INCLUDE;
        }
        if ("omit".equals(value)) {
            return Credentials.OMIT;
        }
        throw JsException.typeError("The provided value '" + value + "' is not a valid enum value of type RequestCredentials.");
    }

    /**
     * Resolves a module specifier against the importing script's base URL.
     * <p>
     * 가져오는 스크립트의 기준 URL로 모듈 지정자를 해석한다.
     *
     * @param base the base URL / 기준 URL
     * @param specifier the module specifier / 모듈 지정자
     * @return the resolved URL without a fragment / 조각 식별자를 제외한 해석된 URL
     */
    public static String resolve(String base, String specifier) {
        try {
            return UrlUtil.stripFragment(UrlUtil.resolve(base, specifier));
        } catch (RuntimeException e) {
            throw JsException.typeError("Failed to resolve module specifier '" + specifier + "'");
        }
    }

    /**
     * Fetches a module through the source using the requested module type.
     * <p>
     * 요청된 모듈 타입으로 공급원을 통해 모듈을 가져온다.
     *
     * @param source the script source / 스크립트 공급원
     * @param request the resolved module request / 해석된 모듈 요청
     * @param credentials true if cross-origin credentials are requested / 다른 오리진에도 자격 증명을 보내려면 true
     * @return source text and final URL / 소스 텍스트와 최종 URL
     * @throws IOException if the source cannot supply the requested module / 공급원이 요청한 모듈을 제공할 수 없으면
     */
    public static ScriptText fetch(ScriptSource source, ModuleRequest request, boolean credentials) throws IOException {
        return fetch(source, request, credentials ? Credentials.INCLUDE : Credentials.SAME_ORIGIN, false);
    }

    /**
     * Fetches a module with explicit credentials and worker entry rules.
     * <p>
     * 지정한 자격 증명과 worker 진입 모듈 규칙을 적용해 가져온다.
     *
     * @param source the script source / 스크립트 공급원
     * @param request the resolved module request / 해석된 모듈 요청
     * @param credentials credentials mode / 자격 증명 모드
     * @param workerEntry true for a worker entry module / worker 진입 모듈이면 true
     * @return source text and final URL / 소스 텍스트와 최종 URL
     * @throws IOException if the source cannot supply the requested module / 공급원이 요청한 모듈을 제공할 수 없으면
     */
    public static ScriptText fetch(ScriptSource source, ModuleRequest request, Credentials credentials, boolean workerEntry) throws IOException {
        String key = request.getSpecifier();
        if (source == null) {
            throw new IOException("scripts cannot be fetched");
        }
        if (source instanceof ModuleScriptSource.WithAttributes) {
            return ((ModuleScriptSource.WithAttributes) source).fetchModule(request, credentials, workerEntry);
        }
        // 타입을 검사할 수 없는 공급원은 기존 JavaScript 경로에만 사용한다.
        if (request.isJson()) {
            throw new IOException("the script source cannot fetch JSON modules");
        }
        if (source instanceof ModuleScriptSource) {
            return ((ModuleScriptSource) source).fetchModule(key, credentials == Credentials.INCLUDE);
        }
        return new ScriptText(source.fetch(key), key);
    }
}
