package org.duckdns.hjow.swingwebpanel.net;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enforcement policies carried by the final HTTP response of a document or worker script.
 * <p>
 * 문서나 워커 스크립트의 최종 HTTP 응답에 담긴 적용 정책.
 */
public final class CrossOriginPolicy {
    /**
     * Cross-origin embedder policy values supported by HTML.
     * <p>
     * HTML이 지원하는 교차 오리진 삽입 정책 값.
     */
    public enum Embedder {
        /** The default permits the usual cross-origin loads. / 기본 상태로 일반적인 교차 오리진 로드를 허용한다. */
        UNSAFE_NONE,
        /** Cross-origin no-CORS loads require resource permission. / 교차 오리진 no-cors 로드에는 리소스 허용이 필요하다. */
        REQUIRE_CORP,
        /** Cross-origin no-CORS loads omit credentials. / 교차 오리진 no-cors 로드는 자격 증명을 생략한다. */
        CREDENTIALLESS
    }

    /** The RFC 8941 token grammar. / RFC 8941 토큰 문법. */
    private static final String TOKEN = "[A-Za-z*][!#$%&'*+.^_`|~0-9A-Za-z:/-]*";

    /** The RFC 8941 bare item types allowed as parameter values. / 매개변수 값에 쓸 수 있는 RFC 8941 기본 항목 형식. */
    private static final String PARAM_VALUE = "(?:"
            + TOKEN + "|-?[0-9]{1,12}\\.[0-9]{1,3}|-?[0-9]{1,15}|\\?[01]|:[A-Za-z0-9+/]*={0,2}:"
            + "|\"(?:[\\x20-\\x21\\x23-\\x5B\\x5D-\\x7E]|\\\\[\"\\\\])*+\")";

    /** A structured item with a token value, including optional parameters. / 선택적 매개변수를 포함한 토큰형 구조화 항목. */
    private static final Pattern TOKEN_ITEM = Pattern.compile("(" + TOKEN + ")"
            + "(?:; *[a-z*][a-z0-9_.*-]*(?:=" + PARAM_VALUE + ")?)*+");

    /** Policy used when a response cannot opt in. / 응답이 정책을 적용할 수 없을 때 사용하는 값. */
    public static final CrossOriginPolicy NONE = new CrossOriginPolicy(null, Embedder.UNSAFE_NONE);

    /** The effective opener token, or null for the default. / 적용되는 opener 토큰. 기본값이면 null. */
    private final String opener;

    /** The effective embedder policy. / 적용되는 삽입 정책. */
    private final Embedder embedder;

    /**
     * Records the parsed enforcement policies.
     * <p>
     * 파싱한 적용 정책을 저장한다.
     *
     * @param opener the valid opener token or null / 올바른 opener 토큰 또는 null
     * @param embedder the effective embedder policy / 적용되는 삽입 정책
     */
    private CrossOriginPolicy(String opener, Embedder embedder) {
        this.opener = opener;
        this.embedder = embedder;
    }

    /**
     * Obtains the policies from a response in a potentially trustworthy origin.
     * <p>
     * 잠재적으로 신뢰 가능한 오리진의 응답에서 정책을 읽는다.
     *
     * @param response the final HTTP response / 최종 HTTP 응답
     * @return the policy or default settings / 정책 또는 기본 설정
     */
    public static CrossOriginPolicy from(ResourceResponse response) {
        if (response.getFailureKind() != FailureKind.NONE || !Origin.of(response.getFinalUrl()).isPotentiallyTrustworthy()) {
            return NONE;
        }
        String opener = token(response.getHeaders("Cross-Origin-Opener-Policy"));
        String embedder = token(response.getHeaders("Cross-Origin-Embedder-Policy"));
        Embedder value = "require-corp".equals(embedder) ? Embedder.REQUIRE_CORP
                : "credentialless".equals(embedder) ? Embedder.CREDENTIALLESS : Embedder.UNSAFE_NONE;
        // 기본값과 모르는 토큰은 효력이 없는 값으로 통일한다.
        if (!"same-origin".equals(opener) && !"same-origin-allow-popups".equals(opener) && !"noopener-allow-popups".equals(opener)) {
            opener = null;
        }
        return opener == null && value == Embedder.UNSAFE_NONE ? NONE : new CrossOriginPolicy(opener, value);
    }

    /**
     * Reads one token item. Multiple field lines would form a list, which is invalid for an item.
     * <p>
     * 토큰 항목 하나를 읽는다. 여러 헤더 줄을 합치면 목록이 되므로 단일 항목으로는 올바르지 않다.
     *
     * @param values the field line values / 헤더 줄 값
     * @return the token or null if absent or invalid / 토큰. 없거나 잘못되었으면 null
     */
    private static String token(List<String> values) {
        if (values.size() != 1) {
            return null;
        }
        Matcher matcher = TOKEN_ITEM.matcher(values.get(0).trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Returns the opener policy before combination with the embedder policy.
     * <p>
     * 삽입 정책과 결합하기 전의 opener 정책을 반환한다.
     *
     * @return the effective header token, or "unsafe-none" for the default / 적용되는 헤더 토큰. 기본값은 "unsafe-none"
     */
    public String getOpener() {
        return opener == null ? "unsafe-none" : opener;
    }

    /**
     * Returns the embedder policy for subsequent requests.
     * <p>
     * 후속 요청에 적용할 삽입 정책을 반환한다.
     *
     * @return the policy / 정책
     */
    public Embedder getEmbedder() {
        return embedder;
    }
}
