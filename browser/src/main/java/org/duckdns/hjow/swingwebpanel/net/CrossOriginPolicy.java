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
    public static final CrossOriginPolicy NONE = new CrossOriginPolicy(null, Embedder.UNSAFE_NONE, null);

    /** The effective opener token, or null for the default. / 적용되는 opener 토큰. 기본값이면 null. */
    private final String opener;

    /** The effective embedder policy. / 적용되는 삽입 정책. */
    private final Embedder embedder;

    /** The declared isolation allowlist, or null for the default. / 선언된 격리 허용 목록. 기본값이면 null. */
    private final String isolationAllowlist;

    /**
     * Records the parsed enforcement policies.
     * <p>
     * 파싱한 적용 정책을 저장한다.
     *
     * @param opener the valid opener token or null / 올바른 opener 토큰 또는 null
     * @param embedder the effective embedder policy / 적용되는 삽입 정책
     * @param isolationAllowlist the Permissions-Policy declaration or null / Permissions-Policy 선언 또는 null
     */
    private CrossOriginPolicy(String opener, Embedder embedder, String isolationAllowlist) {
        this.opener = opener;
        this.embedder = embedder;
        this.isolationAllowlist = isolationAllowlist;
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
        String allowlist = IsolationPermission.read(response.getHeaders("Permissions-Policy"));
        return opener == null && value == Embedder.UNSAFE_NONE && allowlist == null ? NONE : new CrossOriginPolicy(opener, value, allowlist);
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
     * Tells whether a navigation of a top-level window must move the window into a browsing context group of its own, which cuts its
     * connection to its opener and to the windows that it opened (WHATWG HTML 7.1.3.1, "check if COOP values require a browsing
     * context group switch").
     * <p>
     * 최상위 창의 이동이 그 창을 자기만의 브라우징 문맥 그룹으로 옮겨야 하는지 알려 준다. 옮기면 여는 창, 그리고 자기가 연 창들과의 관계가
     * 끊긴다(WHATWG HTML 7.1.3.1, "check if COOP values require a browsing context group switch").
     *
     * @param initial     true if the window still shows its first <code>about:blank</code> document / 창이 아직 처음
     *                    <code>about:blank</code> 문서를 보이면 true
     * @param active      the opener policy value of the document that the window shows / 창이 보이는 문서의 opener 정책 값
     * @param activeOrigin the origin of that document / 그 문서의 오리진
     * @param response    the opener policy value of the response / 응답의 opener 정책 값
     * @param responseOrigin the origin of the response / 응답의 오리진
     * @return true if the window must leave its group / 창이 그룹을 떠나야 하면 true
     */
    public static boolean requiresGroupSwitch(boolean initial, String active, Origin activeOrigin, String response, Origin responseOrigin) {
        if ("noopener-allow-popups".equals(response)) {
            return true;
        }
        // 처음 빈 문서를 보이는 팝업은, 여는 쪽이 팝업을 허용했고 응답이 정책을 내지 않았으면 그룹에 남는다.
        if (initial && ("same-origin-allow-popups".equals(active) || "noopener-allow-popups".equals(active)) && "unsafe-none".equals(response)) {
            return false;
        }
        boolean activeNone = "unsafe-none".equals(active);
        boolean responseNone = "unsafe-none".equals(response);
        if (activeNone && responseNone) {
            return false;
        }
        if (activeNone || responseNone) {
            return true;
        }
        return !(active.equals(response) && activeOrigin != null && responseOrigin != null && activeOrigin.isSameOrigin(responseOrigin));
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

    /**
     * Tests whether this document's own header permits its isolation capability.
     * <p>
     * 이 문서의 헤더가 문서 자체의 격리 권한을 허용하는지 검사한다.
     *
     * @param origin the document's origin / 문서 오리진
     * @return true if permitted / 허용되면 true
     */
    public boolean allowsIsolation(Origin origin) {
        return IsolationPermission.declaredAllows(isolationAllowlist, origin, origin);
    }

    /**
     * Tests whether this document's allowlist and the iframe's attribute delegate isolation.
     * <p>
     * 이 문서의 허용 목록과 iframe 속성이 격리 권한을 이어주는지 검사한다.
     *
     * @param parent the container document's origin / 컨테이너 문서 오리진
     * @param target the child document's origin / 자식 문서 오리진
     * @param source the declared iframe source origin / iframe의 선언 src 오리진
     * @param allow the iframe allow attribute / iframe의 allow 속성
     * @return true if both policies permit delegation / 두 정책이 권한 위임을 허용하면 true
     */
    public boolean allowsChildIsolation(Origin parent, Origin target, Origin source, String allow) {
        return IsolationPermission.declaredAllows(isolationAllowlist, parent, target)
                && IsolationPermission.containerAllows(allow, parent, source, target);
    }

    /**
     * Creates the inherited embedder state of a locally created child document.
     * <p>
     * 로컬에서 만든 하위 문서가 물려받을 삽입 정책을 만든다.
     *
     * @param policy the parent document's embedder policy / 부모 문서의 삽입 정책
     * @return the inherited response settings / 물려받은 응답 설정
     */
    public static CrossOriginPolicy inheritedEmbedder(Embedder policy) {
        return policy == Embedder.UNSAFE_NONE ? NONE : new CrossOriginPolicy(null, policy, null);
    }

    /**
     * Tells whether the fetch is a no-CORS subresource load.
     * <p>
     * CORS를 쓰지 않는 하위 리소스 요청인지 확인한다.
     *
     * @param purpose the request's destination / 요청 대상 종류
     * @return true for no-CORS subresources / CORS 없는 하위 리소스이면 true
     */
    private static boolean isNoCorsSubresource(RequestPurpose purpose) {
        return !OriginPolicy.requiresCors(purpose) && purpose != RequestPurpose.WORKER_MODULE
                && purpose != RequestPurpose.DOCUMENT && purpose != RequestPurpose.FRAME && purpose != RequestPurpose.DOWNLOAD;
    }

    /**
     * Returns the cache partition for a potentially credentialless no-CORS request.
     * <p>
     * 자격 증명을 생략할 수 있는 no-cors 요청의 캐시 분기를 반환한다.
     *
     * @param request the resource request / 리소스 요청
     * @return the credentialless initiator, or the empty default partition / credentialless 시작 오리진 또는 빈 기본 분기
     */
    static String cachePartition(ResourceRequest request) {
        Origin origin = request.getInitiator();
        if (origin == null || request.getEmbedderPolicy() != Embedder.CREDENTIALLESS || !isNoCorsSubresource(request.getPurpose())) {
            return "";
        }
        return origin.serialize();
    }

    /**
     * Determines whether a credentialless request strips credentials on this particular HTTP hop.
     * <p>
     * 현재 HTTP 구간에서 credentialless 요청의 자격 증명을 생략해야 하는지 확인한다.
     *
     * @param request the resource request / 리소스 요청
     * @param url the current hop URL after redirects / 리디렉션을 적용한 현재 구간 주소
     * @return true if credentials must be omitted / 자격 증명을 생략해야 하면 true
     */
    static boolean omitsCredentials(ResourceRequest request, String url) {
        return cachePartition(request).length() != 0 && !request.getInitiator().isSameOrigin(Origin.of(url));
    }

    /**
     * Applies the Fetch CORP decision and HTML child navigation embedder inheritance check.
     * <p>
     * Fetch의 CORP 판정과 HTML 하위 이동의 삽입 정책 상속 검사를 적용한다.
     *
     * @param request the initiating request / 시작 요청
     * @param response the received response / 받은 응답
     * @return true when the response may be delivered / 응답을 전달할 수 있으면 true
     */
    static boolean permits(ResourceRequest request, ResourceResponse response) {
        Origin initiator = request.embedderOrigin();
        Embedder policy = request.getEmbedderPolicy();
        boolean frame = request.getPurpose() == RequestPurpose.FRAME;
        if (initiator == null || (frame ? policy == Embedder.UNSAFE_NONE : !isNoCorsSubresource(request.getPurpose()))) {
            return true;
        }
        // 격리를 요구하는 부모 아래에서는 자식 응답도 격리 가능한 삽입 정책을 명시해야 한다.
        if (frame && from(response).getEmbedder() == Embedder.UNSAFE_NONE) {
            return false;
        }
        String url = response.getFinalUrl();
        Origin target = Origin.of(url);
        if (initiator.isSameOrigin(target)) {
            return true;
        }
        List<String> headers = response.getHeaders("Cross-Origin-Resource-Policy");
        String corp = headers.size() == 1 ? headers.get(0).trim() : "";
        if ("cross-origin".equals(corp)) {
            return true;
        }
        if ("same-origin".equals(corp)) {
            return false;
        }
        if ("same-site".equals(corp)) {
            return ("https".equals(initiator.getScheme()) || !"https".equals(target.getScheme()))
                    && PublicSuffixList.sameSite(initiator, target);
        }
        return policy == Embedder.UNSAFE_NONE || (policy == Embedder.CREDENTIALLESS && !frame && omitsCredentials(request, url));
    }

    /**
     * Builds a policy failure without exposing the blocked response body.
     * <p>
     * 차단한 응답의 본문을 노출하지 않는 정책 실패를 만든다.
     *
     * @param source the blocked response / 차단한 응답
     * @return a failure containing only the request and final addresses / 요청과 최종 주소만 담은 실패
     */
    static ResourceResponse blocked(ResourceResponse source) {
        ResourceResponse result = ResourceResponse.failure(source.getRequestUrl(), FailureKind.BLOCKED_EMBEDDER_POLICY,
                "Blocked by cross-origin embedder or resource policy");
        result.setFinalUrl(source.getFinalUrl());
        return result;
    }
}
