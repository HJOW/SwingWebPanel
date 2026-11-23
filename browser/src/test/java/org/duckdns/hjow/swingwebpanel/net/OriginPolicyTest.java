package org.duckdns.hjow.swingwebpanel.net;

import java.util.LinkedHashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link Origin} and {@link OriginPolicy} (plan item P04-11): origin computation, same-origin checks and the
 * different treatment of display purposes and CORS purposes for cross-origin responses.
 * <p>
 * {@link Origin} 과 {@link OriginPolicy} 시험(계획 P04-11): 오리진 계산, 동일 오리진 검사, 교차 오리진 응답에 대한
 * 표시 목적과 CORS 목적의 서로 다른 처리.
 */
public class OriginPolicyTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public OriginPolicyTest() {
    }

    /**
     * Scheme, host and effective port decide the origin; default ports and letter case are normalized.
     * <p>
     * 스킴, 호스트, 실효 포트가 오리진을 정한다. 기본 포트와 대소문자는 정규화한다.
     */
    public void testTupleOrigins() {
        Origin a = Origin.of("http://Example.com/x");
        Check.isTrue(a.isSameOrigin(Origin.of("http://example.com:80/y?z#f")), "default port and case");
        Check.isFalse(a.isSameOrigin(Origin.of("http://example.com:81/")), "other port");
        Check.isFalse(a.isSameOrigin(Origin.of("https://example.com/")), "other scheme");
        Check.isFalse(a.isSameOrigin(Origin.of("http://www.example.com/")), "other host");
        Check.isTrue(Origin.of("https://h/").isSameOrigin(Origin.of("https://h:443/")), "https default port");
        Check.equal("http://example.com", a.serialize(), "serialization omits the default port");
        Check.equal("http://h:8080", Origin.of("http://h:8080/").serialize(), "serialization keeps other ports");
        Check.equal("ftp://h", Origin.of("ftp://h/").serialize(), "ftp");
    }

    /**
     * Class path documents share one origin; special and blocked addresses have opaque origins that equal nothing.
     * <p>
     * 클래스패스 문서는 오리진 하나를 공유하고, 특수/차단 주소는 어떤 것과도 같지 않은 불투명 오리진을 가진다.
     */
    public void testClasspathAndOpaqueOrigins() {
        Check.isTrue(Origin.of("classpath://a/x.html").isSameOrigin(Origin.of("classpath://b/y.html")), "all classpath documents share an origin");
        Check.equal("classpath://", Origin.of("classpath://a/x.html").serialize(), "classpath serialization");
        Origin blank = Origin.of("about:blank");
        Check.isTrue(blank.isOpaque(), "about:blank is opaque");
        Check.equal("null", blank.serialize(), "opaque serializes as null");
        Check.isFalse(blank.isSameOrigin(blank), "an opaque origin is not same-origin even with itself");
        Check.isFalse(blank.isSameOrigin(Origin.of("about:blank")), "two opaque origins differ");
        Check.isTrue(Origin.of("file:///x").isOpaque(), "blocked scheme is opaque");
        Check.isFalse(Origin.of("classpath://a").isSameOrigin(Origin.of("http://h/")), "classpath vs http");
    }

    /**
     * Builds a response carrying CORS headers.
     * <p>
     * CORS 헤더를 가진 응답을 만든다.
     *
     * @param allowOrigin      Access-Control-Allow-Origin or null / Access-Control-Allow-Origin 또는 null
     * @param allowCredentials Access-Control-Allow-Credentials or null / Access-Control-Allow-Credentials 또는 null
     * @return the response / 응답
     */
    private static ResourceResponse corsResponse(String allowOrigin, String allowCredentials) {
        ResourceResponse r = new ResourceResponse("http://api.example/data");
        r.setStatus(200, "OK");
        if (allowOrigin != null) {
            r.addHeader("Access-Control-Allow-Origin", allowOrigin);
        }
        if (allowCredentials != null) {
            r.addHeader("Access-Control-Allow-Credentials", allowCredentials);
        }
        return r;
    }

    /**
     * Same origin may always read; cross-origin images and audio can be shown but never read by scripts.
     * <p>
     * 같은 오리진은 항상 읽을 수 있다. 교차 오리진 이미지/오디오는 표시할 수 있어도 스크립트가 읽을 수는 없다.
     */
    public void testDisplayVersusScriptAccess() {
        Origin page = Origin.of("http://site.example/");
        Origin other = Origin.of("http://img.example/");
        ResourceResponse plain = corsResponse(null, null);
        Check.isTrue(OriginPolicy.mayScriptReadBody(page, Origin.of("http://site.example/data"), RequestPurpose.XHR, plain, false), "same origin xhr");
        Check.isTrue(OriginPolicy.mayUse(page, other, RequestPurpose.IMAGE, plain, false), "cross-origin image may be displayed");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, other, RequestPurpose.IMAGE, plain, false), "but scripts must not read its pixels or bytes");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, other, RequestPurpose.AUDIO, plain, false), "audio too");
        Check.isTrue(OriginPolicy.mayUse(page, other, RequestPurpose.SCRIPT, plain, false), "cross-origin script may execute");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, other, RequestPurpose.SCRIPT, plain, false), "but its source is hidden");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, other, RequestPurpose.DOCUMENT, plain, false), "documents of other origins are hidden");
    }

    /**
     * CORS purposes need Access-Control-Allow-Origin; credentials need an exact origin and the credentials header.
     * <p>
     * CORS 목적은 Access-Control-Allow-Origin 이 필요하다. 자격 증명을 쓰면 정확한 오리진과 자격 증명 헤더가 필요하다.
     */
    public void testCorsRules() {
        Origin page = Origin.of("http://site.example:8080/");
        Origin api = Origin.of("http://api.example/");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse(null, null), false), "no header");
        Check.isTrue(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse("*", null), false), "wildcard without credentials");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse("*", "true"), true), "wildcard is not allowed with credentials");
        Check.isTrue(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse("http://site.example:8080", null), false), "exact origin");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse("http://site.example", null), false), "wrong port is another origin");
        Check.isFalse(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse("http://site.example:8080", null), true), "credentials need the header");
        Check.isTrue(OriginPolicy.mayScriptReadBody(page, api, RequestPurpose.XHR, corsResponse("http://site.example:8080", "true"), true), "credentials allowed");
        Check.isFalse(OriginPolicy.mayUse(page, api, RequestPurpose.FONT, corsResponse(null, null), false), "cross-origin fonts need CORS");
        Check.isTrue(OriginPolicy.mayUse(page, api, RequestPurpose.FONT, corsResponse("*", null), false), "font with wildcard");
        Check.isFalse(OriginPolicy.corsAllows(Origin.opaque(), "*", null, false), "opaque initiators never pass");
        Check.isTrue(OriginPolicy.requiresCors(RequestPurpose.XHR) && OriginPolicy.requiresCors(RequestPurpose.FONT), "purposes that need CORS");
        Check.isFalse(OriginPolicy.requiresCors(RequestPurpose.IMAGE), "images do not need CORS to be displayed");
    }

    /**
     * Only simple cross-origin requests are supported until preflight exists.
     * <p>
     * 사전 요청이 구현되기 전에는 단순 교차 오리진 요청만 지원한다.
     */
    public void testSimpleRequests() {
        Map<String, String> h = new LinkedHashMap<String, String>();
        Check.isTrue(OriginPolicy.isSimpleRequest("GET", h), "GET");
        Check.isTrue(OriginPolicy.isSimpleRequest("post", null), "POST without headers");
        Check.isFalse(OriginPolicy.isSimpleRequest("PUT", h), "PUT needs preflight");
        Check.isFalse(OriginPolicy.isSimpleRequest("DELETE", h), "DELETE needs preflight");
        h.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Check.isTrue(OriginPolicy.isSimpleRequest("POST", h), "form content type");
        h.put("Content-Type", "application/json");
        Check.isFalse(OriginPolicy.isSimpleRequest("POST", h), "json content type needs preflight");
        h.clear();
        h.put("X-Custom", "1");
        Check.isFalse(OriginPolicy.isSimpleRequest("GET", h), "custom header needs preflight");
        h.clear();
        h.put("Accept-Language", "ko");
        Check.isTrue(OriginPolicy.isSimpleRequest("GET", h), "safelisted header");
    }
}
