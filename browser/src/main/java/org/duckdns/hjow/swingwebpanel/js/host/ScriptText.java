package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;

/**
 * The text of a fetched script together with the URL that finally served it. The final URL differs from the requested one after a
 * redirect, and relative specifiers inside a module resolve against it (the module's base URL), while the module keeps the requested URL
 * as its identity in the module map.
 * <p>
 * 가져온 스크립트의 텍스트와 그것을 최종적으로 제공한 URL. 최종 URL은 리디렉션 뒤에는 요청한 URL과 다르며, 모듈 안의 상대 지정자는 이 URL
 * (모듈의 기준 URL)을 기준으로 해석한다. 모듈은 모듈 맵에서 요청한 URL을 자기 식별자로 유지한다.
 */
public final class ScriptText {

    /**
     * The script source text.
     * <p>
     * 스크립트 원문.
     */
    private final String text;

    /**
     * The URL that served the text (after redirects).
     * <p>
     * 텍스트를 제공한 URL(리디렉션 뒤).
     */
    private final String url;

    /** The final response policy used for worker entry scripts. / 워커 진입 스크립트에 쓰는 최종 응답 정책. */
    private final CrossOriginPolicy crossOriginPolicy;

    /**
     * Creates the value.
     * <p>
     * 값을 만든다.
     *
     * @param text the source text, not null / 원문(null 불가)
     * @param url  the final URL, not null / 최종 URL(null 불가)
     */
    public ScriptText(String text, String url) {
        this(text, url, CrossOriginPolicy.NONE);
    }

    /**
     * Creates a script response with the final origin policies.
     * <p>
     * 최종 오리진 정책을 담은 스크립트 응답을 만든다.
     *
     * @param text the source text / 원문
     * @param url the final URL / 최종 URL
     * @param policy the response policy / 응답 정책
     */
    public ScriptText(String text, String url, CrossOriginPolicy policy) {
        if (text == null || url == null) {
            throw new IllegalArgumentException("text and url must not be null");
        }
        this.text = text;
        this.url = url;
        this.crossOriginPolicy = policy;
    }

    /**
     * Returns the source text.
     * <p>
     * 원문을 반환한다.
     *
     * @return the text / 원문
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the final URL.
     * <p>
     * 최종 URL을 반환한다.
     *
     * @return the URL / URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the parsed policy from the final response headers.
     * <p>
     * 최종 응답 헤더에서 파싱한 정책을 반환한다.
     *
     * @return the policy / 정책
     */
    public CrossOriginPolicy getCrossOriginPolicy() {
        return crossOriginPolicy;
    }
}
