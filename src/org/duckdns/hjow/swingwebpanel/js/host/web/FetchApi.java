package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Installs the Fetch API (Fetch Standard, section 5): the global <code>fetch()</code> function and the <code>Request</code> and
 * <code>Response</code> interfaces with their Body mixin. <code>fetch()</code> builds a <code>Request</code>, hands a {@link WebRequest} to the
 * component's request layer (which applies User-Agent, extra headers, cookies by the credentials mode, the SSL hook, the scheme
 * policy, the HTTP cache and the same-origin policy with CORS) and resolves its promise with a <code>Response</code> when the
 * headers and the body have arrived, or rejects it with a <code>TypeError</code> for a network error and with the signal's reason when an
 * <code>AbortSignal</code> aborts. A non-simple cross-origin request is announced with a CORS preflight by the request layer. Not supported,
 * and reported honestly instead of ignored: <code>redirect: "manual"</code>, <code>cache: "only-if-cached"</code>, streaming request bodies and the
 * <code>ReadableStream</code> <code>body</code> of a response (it is <code>null</code>), <code>keepalive</code> and <code>priority</code> are accepted
 * and have no effect.
 * <p>
 * Fetch API 를 설치한다(Fetch 표준 5절): 전역 <code>fetch()</code> 함수와 Body mixin 을 가진 <code>Request</code>, <code>Response</code> 인터페이스.
 * <code>fetch()</code> 는 <code>Request</code> 를 만들고 컴포넌트의 요청 계층에 {@link WebRequest} 를 넘기며(요청 계층이 User-Agent, 추가 헤더, 자격 증명
 * 모드에 따른 쿠키, SSL 훅, 스킴 정책, HTTP 캐시, CORS 를 포함한 동일 출처 정책을 적용한다) 헤더와 본문이 도착하면 <code>Response</code> 로 프라미스를
 * 이행하고 네트워크 오류는 <code>TypeError</code> 로, <code>AbortSignal</code> 이 중단되면 신호의 이유로 거부한다. 단순하지 않은 교차 오리진 요청은 요청 계층이 CORS
 * 사전 요청으로 미리 알린다. 지원하지 않는 것은 무시하지 않고 솔직하게 알린다: <code>redirect: "manual"</code>, <code>cache: "only-if-cached"</code>,
 * 스트리밍 요청 본문, 응답의 <code>ReadableStream</code> <code>body</code>(<code>null</code>). <code>keepalive</code> 와 <code>priority</code> 는 받아들이되 효과가 없다.
 */
final class FetchApi {

    /**
     * The script object of a <code>Request</code>.
     * <p>
     * <code>Request</code> 의 스크립트 객체.
     */
    static final class RequestObject extends BodyApi.BodyObject {
        /** The absolute URL. / 절대 URL. */
        String url = "";

        /** The method. / 메서드. */
        String method = "GET";

        /** The mode: cors, same-origin or no-cors. / 모드: cors, same-origin, no-cors. */
        String mode = "cors";

        /** The credentials mode. / 자격 증명 모드. */
        String credentials = "same-origin";

        /** The cache mode. / 캐시 모드. */
        String cache = "default";

        /** The redirect mode. / 리디렉션 모드. */
        String redirect = "follow";

        /** The referrer: "client", "no-referrer" or a URL. / 참조: "client", "no-referrer" 또는 URL. */
        String referrer = "client";

        /** The referrer policy or "". / 참조 정책 또는 "". */
        String referrerPolicy = "";

        /** The integrity metadata. / 무결성 메타데이터. */
        String integrity = "";

        /** The keepalive flag. / keepalive 표시. */
        boolean keepalive;

        /** The signal. / 신호. */
        AbortSignal signal;

        /**
         * Creates an object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        RequestObject(JsObject proto) {
            super(proto);
        }
    }

    /**
     * The script object of a <code>Response</code>.
     * <p>
     * <code>Response</code> 의 스크립트 객체.
     */
    static final class ResponseObject extends BodyApi.BodyObject {
        /** The response type. / 응답 종류. */
        String type = "default";

        /** The URL or "". / URL 또는 "". */
        String url = "";

        /** True if redirected. / 리디렉션되었으면 true. */
        boolean redirected;

        /** The status. / 상태. */
        int status = 200;

        /** The reason phrase. / 이유 문구. */
        String statusText = "";

        /**
         * Creates an object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        ResponseObject(JsObject proto) {
            super(proto);
        }
    }

    /**
     * One running <code>fetch()</code> call: the promise, the request and the way to cancel it.
     * <p>
     * 실행 중인 <code>fetch()</code> 호출 하나: 프라미스, 요청, 취소 방법.
     */
    private final class Pending implements WebRequestSource.Callback {
        /** The promise. / 프라미스. */
        final Promises.Deferred deferred;

        /** The request object. / 요청 객체. */
        final RequestObject request;

        /** The network call. / 네트워크 호출. */
        WebRequestSource.Call call;

        /** The abort algorithm registered on the signal. / 신호에 등록한 중단 알고리즘. */
        Runnable abortStep;

        /** True once the promise was settled. / 프라미스가 확정된 뒤에는 true. */
        boolean done;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param deferred the promise / 프라미스
         * @param request  the request / 요청
         */
        Pending(Promises.Deferred deferred, RequestObject request) {
            this.deferred = deferred;
            this.request = request;
        }

        /**
         * Ignores the early headers (the promise resolves when the whole response is there).
         * <p>
         * 이른 헤더 알림은 무시한다(전체 응답이 도착하면 프라미스를 이행한다).
         *
         * @param head the head / 머리
         */
        public void onHeaders(WebResponse head) {
        }

        /**
         * Ignores progress.
         * <p>
         * 진행 알림은 무시한다.
         *
         * @param loaded bytes / 바이트
         * @param total  length / 길이
         */
        public void onProgress(long loaded, long total) {
        }

        /**
         * Hands the result to the page thread.
         * <p>
         * 결과를 페이지 스레드에 넘긴다.
         *
         * @param response the response / 응답
         */
        public void onComplete(final WebResponse response) {
            api.post(new Runnable() {
                public void run() {
                    finish(Pending.this, response);
                }
            });
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The body helper. / 본문 도우미. */
    private final BodyApi bodies;

    /** The prototype of requests. / 요청의 프로토타입. */
    private JsObject requestProto;

    /** The prototype of responses. / 응답의 프로토타입. */
    private JsObject responseProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    FetchApi(WebApis api) {
        this.api = api;
        this.bodies = new BodyApi(api);
    }

    /**
     * Installs <code>Request</code>, <code>Response</code> and <code>fetch</code>.
     * <p>
     * <code>Request</code>, <code>Response</code>, <code>fetch</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        JsRealm realm = api.realm();
        Natives.ClassDef rq = Natives.defineClass(realm, g, "Request", 1, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Natives.requireArgs(args, 1, "constructor", "Request");
                return newRequest(args[0], Js.arg(args, 1), Ops.getPrototypeFromConstructor(r, newTarget, requestProto));
            }
        });
        requestProto = rq.prototype;
        Natives.getter(realm, requestProto, "method", this, "requestMethod");
        Natives.getter(realm, requestProto, "url", this, "requestUrl");
        Natives.getter(realm, requestProto, "headers", this, "requestHeaders");
        Natives.getter(realm, requestProto, "destination", this, "requestDestination");
        Natives.getter(realm, requestProto, "referrer", this, "requestReferrer");
        Natives.getter(realm, requestProto, "referrerPolicy", this, "requestReferrerPolicy");
        Natives.getter(realm, requestProto, "mode", this, "requestMode");
        Natives.getter(realm, requestProto, "credentials", this, "requestCredentials");
        Natives.getter(realm, requestProto, "cache", this, "requestCache");
        Natives.getter(realm, requestProto, "redirect", this, "requestRedirect");
        Natives.getter(realm, requestProto, "integrity", this, "requestIntegrity");
        Natives.getter(realm, requestProto, "keepalive", this, "requestKeepalive");
        Natives.getter(realm, requestProto, "isReloadNavigation", this, "requestFalse");
        Natives.getter(realm, requestProto, "isHistoryNavigation", this, "requestFalse");
        Natives.getter(realm, requestProto, "signal", this, "requestSignal");
        Natives.getter(realm, requestProto, "duplex", this, "requestDuplex");
        Natives.method(realm, requestProto, "clone", 0, this, "requestClone");
        bodies.installMixin(requestProto);
        Natives.ClassDef rs = Natives.defineClass(realm, g, "Response", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return newResponse(Js.arg(args, 0), Js.arg(args, 1), Ops.getPrototypeFromConstructor(r, newTarget, responseProto));
            }
        });
        responseProto = rs.prototype;
        Natives.getter(realm, responseProto, "type", this, "responseType");
        Natives.getter(realm, responseProto, "url", this, "responseUrl");
        Natives.getter(realm, responseProto, "redirected", this, "responseRedirected");
        Natives.getter(realm, responseProto, "status", this, "responseStatus");
        Natives.getter(realm, responseProto, "ok", this, "responseOk");
        Natives.getter(realm, responseProto, "statusText", this, "responseStatusText");
        Natives.getter(realm, responseProto, "headers", this, "responseHeaders");
        Natives.method(realm, responseProto, "clone", 0, this, "responseClone");
        bodies.installMixin(responseProto);
        Natives.method(realm, rs.constructor, "error", 0, this, "staticError");
        Natives.method(realm, rs.constructor, "redirect", 1, this, "staticRedirect");
        Natives.method(realm, rs.constructor, "json", 1, this, "staticJson");
        g.defineBuiltin("fetch", Natives.function(realm, "fetch", 1, this, "fetch"));
    }

    // ------------------------------------------------------------------ Request

    /**
     * Reads a member of an init dictionary: undefined means "not present".
     * <p>
     * init 딕셔너리의 멤버를 읽는다. undefined 는 "없음"이다.
     *
     * @param init the dictionary object or null / 딕셔너리 객체 또는 null
     * @param key  the member name / 멤버 이름
     * @return the value, undefined if absent / 값. 없으면 undefined
     */
    private static JsValue member(JsObject init, String key) {
        return init == null ? Js.UNDEFINED : init.get(key);
    }

    /**
     * Checks an enumeration value.
     * <p>
     * 열거 값을 검사한다.
     *
     * @param v      the value / 값
     * @param name   the enumeration name for messages / 메시지용 열거 이름
     * @param allowed the allowed values / 허용 값
     * @return the string / 문자열
     */
    private static String enumValue(JsValue v, String name, String[] allowed) {
        String s = Ops.toString(v);
        // 허용 목록에 있는 값인지 확인한다.
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i].equals(s)) {
                return s;
            }
        }
        throw JsException.typeError("Failed to construct 'Request': The provided value '" + s + "' is not a valid enum value of type " + name + ".");
    }

    /**
     * Implements the <code>Request</code> constructor (Fetch Standard 5.4).
     * <p>
     * <code>Request</code> 생성자를 구현한다(Fetch 표준 5.4).
     *
     * @param input the URL or request / URL 또는 요청
     * @param init  the init dictionary / init 딕셔너리
     * @param proto the prototype / 프로토타입
     * @return the request / 요청
     */
    RequestObject newRequest(JsValue input, JsValue init, JsObject proto) {
        RequestObject r = new RequestObject(proto);
        RequestObject src = null;
        AbortSignal signal = null;
        String fallbackMode = null;
        if (input instanceof RequestObject) {
            src = (RequestObject) input;
            r.url = src.url;
            r.method = src.method;
            r.mode = src.mode;
            r.credentials = src.credentials;
            r.cache = src.cache;
            r.redirect = src.redirect;
            r.referrer = src.referrer;
            r.referrerPolicy = src.referrerPolicy;
            r.integrity = src.integrity;
            r.keepalive = src.keepalive;
            signal = src.signal;
        } else {
            String s = Natives.usv(input);
            String abs;
            UrlUtil.Parts parts;
            try {
                abs = api.resolve(s);
                parts = UrlUtil.parse(abs);
            } catch (IllegalArgumentException e) {
                throw JsException.typeError("Failed to construct 'Request': Failed to parse URL from " + s);
            }
            if (parts.userInfo != null && parts.userInfo.length() > 0) {
                throw JsException.typeError("Failed to construct 'Request': Request cannot be constructed from a URL that includes credentials: "
                        + s);
            }
            r.url = abs;
            fallbackMode = "cors";
        }
        JsObject dict = null;
        if (!init.isNullOrUndefined()) {
            if (!init.isObject()) {
                throw JsException.typeError("Failed to construct 'Request': The provided value is not of type 'RequestInit'.");
            }
            dict = (JsObject) init;
        }
        boolean initNonEmpty = false;
        String[] keys = { "method", "headers", "body", "referrer", "referrerPolicy", "mode", "credentials", "cache", "redirect", "integrity",
            "keepalive", "signal", "duplex", "priority", "window" };
        // 인식하는 멤버 중 undefined 가 아닌 것이 하나라도 있으면 init 은 비어 있지 않다.
        for (int i = 0; i < keys.length && dict != null; i++) {
            if (!dict.get(keys[i]).isUndefined()) {
                initNonEmpty = true;
            }
        }
        JsValue window = member(dict, "window");
        if (!window.isUndefined() && !window.isNull()) {
            throw JsException.typeError("Failed to construct 'Request': Window can only be null.");
        }
        if (initNonEmpty) {
            applyInit(r, dict, fallbackMode);
            JsValue sv = member(dict, "signal");
            if (!sv.isUndefined()) {
                if (sv.isNull()) {
                    signal = null;
                } else {
                    signal = AbortApi.signalOf(sv);
                    if (signal == null) {
                        throw JsException.typeError("Failed to construct 'Request': Failed to read the 'signal' property from 'RequestInit': "
                                + "Failed to convert value to 'AbortSignal'.");
                    }
                }
            }
        } else if (fallbackMode != null) {
            r.mode = fallbackMode;
        }
        List<AbortSignal> sources = new ArrayList<AbortSignal>();
        if (signal != null) {
            sources.add(signal);
        }
        r.signal = api.abort.dependent(sources);
        r.headers = api.headers.create(r.mode.equals("no-cors") ? "request-no-cors" : "request");
        if (r.mode.equals("no-cors") && !("GET".equals(r.method) || "HEAD".equals(r.method) || "POST".equals(r.method))) {
            throw JsException.typeError("Failed to construct 'Request': '" + r.method + "' is unsupported in no-cors mode.");
        }
        JsValue headersInit = member(dict, "headers");
        if (initNonEmpty) {
            if (!headersInit.isUndefined()) {
                if (headersInit instanceof HeadersApi.HeadersObject) {
                    List<String[]> l = ((HeadersApi.HeadersObject) headersInit).list;
                    // Headers 객체이면 그 목록의 헤더를 하나씩 덧붙인다.
                    for (int i = 0; i < l.size(); i++) {
                        r.headers.append(l.get(i)[0], l.get(i)[1]);
                    }
                } else {
                    api.headers.fill(r.headers, headersInit);
                }
            } else if (src != null) {
                // 새 모드가 요청 헤더에 다른 제한을 줄 수 있으므로 복사한 헤더도 새 guard 로 다시 덧붙인다.
                for (int i = 0; i < src.headers.list.size(); i++) {
                    r.headers.append(src.headers.list.get(i)[0], src.headers.list.get(i)[1]);
                }
            }
        } else if (src != null) {
            copyHeaders(src, r);
        }
        JsValue bodyInit = member(dict, "body");
        byte[] inputBody = src == null ? null : src.body;
        boolean hasInitBody = initNonEmpty && !bodyInit.isNullOrUndefined();
        if ((hasInitBody || inputBody != null) && ("GET".equals(r.method) || "HEAD".equals(r.method))) {
            throw JsException.typeError("Failed to construct 'Request': Request with GET/HEAD method cannot have body.");
        }
        if (hasInitBody) {
            BodyApi.BodyData data = bodies.extract(bodyInit);
            r.body = data.bytes;
            if (data.type != null && r.headers.get("content-type") == null) {
                r.headers.append("content-type", data.type);
            }
        } else if (inputBody != null) {
            if (src.unusable()) {
                throw JsException.typeError("Failed to construct 'Request': Cannot construct a Request with a Request object that has already "
                        + "been used.");
            }
            r.body = inputBody;
            src.used = true;
        }
        return r;
    }

    /**
     * Copies the headers of one request to another without validation.
     * <p>
     * 요청 하나의 헤더를 검증 없이 다른 요청에 복사한다.
     *
     * @param from the source / 원본
     * @param to   the target / 대상
     */
    private static void copyHeaders(RequestObject from, RequestObject to) {
        // 헤더 목록의 항목을 복사해 서로 영향을 주지 않게 한다.
        for (int i = 0; i < from.headers.list.size(); i++) {
            to.headers.list.add(new String[] { from.headers.list.get(i)[0], from.headers.list.get(i)[1] });
        }
    }

    /**
     * Applies the members of a <code>RequestInit</code> that change the request's own fields.
     * <p>
     * 요청 자신의 필드를 바꾸는 <code>RequestInit</code> 멤버를 적용한다.
     *
     * @param r            the request / 요청
     * @param dict         the dictionary / 딕셔너리
     * @param fallbackMode the mode when init has none, or null / init 에 없을 때의 모드. 없으면 null
     */
    private void applyInit(RequestObject r, JsObject dict, String fallbackMode) {
        JsValue v = dict.get("referrer");
        if (!v.isUndefined()) {
            String ref = Natives.usv(v);
            if (ref.length() == 0) {
                r.referrer = "no-referrer";
            } else {
                String abs;
                try {
                    abs = api.resolve(ref);
                } catch (IllegalArgumentException e) {
                    throw JsException.typeError("Failed to construct 'Request': Invalid referrer URL: " + ref);
                }
                boolean sameOrigin = Origin.of(api.documentUrl()).isSameOrigin(Origin.of(abs));
                r.referrer = "about:client".equals(abs) || !sameOrigin ? "client" : abs;
            }
        }
        v = dict.get("referrerPolicy");
        if (!v.isUndefined()) {
            r.referrerPolicy = enumValue(v, "ReferrerPolicy", new String[] { "", "no-referrer", "no-referrer-when-downgrade", "same-origin",
                "origin", "strict-origin", "origin-when-cross-origin", "strict-origin-when-cross-origin", "unsafe-url" });
        }
        v = dict.get("mode");
        if (!v.isUndefined()) {
            String m = enumValue(v, "RequestMode", new String[] { "navigate", "same-origin", "no-cors", "cors" });
            if ("navigate".equals(m)) {
                throw JsException.typeError("Failed to construct 'Request': Cannot construct a Request with a RequestInit whose mode member is "
                        + "set as 'navigate'.");
            }
            r.mode = m;
        } else if (fallbackMode != null) {
            r.mode = fallbackMode;
        }
        v = dict.get("credentials");
        if (!v.isUndefined()) {
            r.credentials = enumValue(v, "RequestCredentials", new String[] { "omit", "same-origin", "include" });
        }
        v = dict.get("cache");
        if (!v.isUndefined()) {
            r.cache = enumValue(v, "RequestCache", new String[] { "default", "no-store", "reload", "no-cache", "force-cache", "only-if-cached" });
        }
        if ("only-if-cached".equals(r.cache) && !"same-origin".equals(r.mode)) {
            throw JsException.typeError("Failed to construct 'Request': 'only-if-cached' can be set only with 'same-origin' mode");
        }
        v = dict.get("redirect");
        if (!v.isUndefined()) {
            r.redirect = enumValue(v, "RequestRedirect", new String[] { "follow", "error", "manual" });
        }
        v = dict.get("integrity");
        if (!v.isUndefined()) {
            r.integrity = Ops.toString(v);
        }
        v = dict.get("keepalive");
        if (!v.isUndefined()) {
            r.keepalive = Ops.toBoolean(v);
        }
        v = dict.get("method");
        if (!v.isUndefined()) {
            String m = Natives.byteString(v, "constructor", "Request");
            if (!HeaderRules.isToken(m)) {
                throw JsException.typeError("Failed to construct 'Request': '" + m + "' is not a valid HTTP method.");
            }
            if (HeaderRules.isForbiddenMethod(m)) {
                throw JsException.typeError("Failed to construct 'Request': '" + m + "' HTTP method is unsupported.");
            }
            r.method = HeaderRules.normalizeMethod(m);
        }
        v = dict.get("duplex");
        if (!v.isUndefined()) {
            enumValue(v, "RequestDuplex", new String[] { "half" });
        }
        v = dict.get("priority");
        if (!v.isUndefined()) {
            enumValue(v, "RequestPriority", new String[] { "high", "low", "auto" });
        }
    }

    /**
     * Returns <code>request.method</code>.
     * <p>
     * <code>request.method</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the method / 메서드
     */
    JsValue requestMethod(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).method);
    }

    /**
     * Returns <code>request.url</code>.
     * <p>
     * <code>request.url</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the URL / URL
     */
    JsValue requestUrl(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).url);
    }

    /**
     * Returns <code>request.headers</code>.
     * <p>
     * <code>request.headers</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the headers object / headers 객체
     */
    JsValue requestHeaders(JsValue self, JsValue[] args) {
        return Natives.self(self, RequestObject.class).headers;
    }

    /**
     * Returns <code>request.destination</code>: always "" for requests that a page makes with <code>fetch()</code>.
     * <p>
     * <code>request.destination</code> 을 반환한다: 페이지가 <code>fetch()</code> 로 만든 요청은 항상 "".
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return "" / ""
     */
    JsValue requestDestination(JsValue self, JsValue[] args) {
        Natives.self(self, RequestObject.class);
        return Js.str("");
    }

    /**
     * Returns <code>request.referrer</code>.
     * <p>
     * <code>request.referrer</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the referrer text / 참조 문자열
     */
    JsValue requestReferrer(JsValue self, JsValue[] args) {
        String r = Natives.self(self, RequestObject.class).referrer;
        return Js.str("no-referrer".equals(r) ? "" : "client".equals(r) ? "about:client" : r);
    }

    /**
     * Returns <code>request.referrerPolicy</code>.
     * <p>
     * <code>request.referrerPolicy</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the policy / 정책
     */
    JsValue requestReferrerPolicy(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).referrerPolicy);
    }

    /**
     * Returns <code>request.mode</code>.
     * <p>
     * <code>request.mode</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the mode / 모드
     */
    JsValue requestMode(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).mode);
    }

    /**
     * Returns <code>request.credentials</code>.
     * <p>
     * <code>request.credentials</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the credentials mode / 자격 증명 모드
     */
    JsValue requestCredentials(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).credentials);
    }

    /**
     * Returns <code>request.cache</code>.
     * <p>
     * <code>request.cache</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the cache mode / 캐시 모드
     */
    JsValue requestCache(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).cache);
    }

    /**
     * Returns <code>request.redirect</code>.
     * <p>
     * <code>request.redirect</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the redirect mode / 리디렉션 모드
     */
    JsValue requestRedirect(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).redirect);
    }

    /**
     * Returns <code>request.integrity</code>.
     * <p>
     * <code>request.integrity</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the metadata / 메타데이터
     */
    JsValue requestIntegrity(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, RequestObject.class).integrity);
    }

    /**
     * Returns <code>request.keepalive</code>.
     * <p>
     * <code>request.keepalive</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the flag / 표시
     */
    JsValue requestKeepalive(JsValue self, JsValue[] args) {
        return Js.bool(Natives.self(self, RequestObject.class).keepalive);
    }

    /**
     * Returns false: a request created by a page is never a navigation request.
     * <p>
     * false 를 반환한다: 페이지가 만든 요청은 이동 요청이 아니다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return false / false
     */
    JsValue requestFalse(JsValue self, JsValue[] args) {
        Natives.self(self, RequestObject.class);
        return Js.FALSE;
    }

    /**
     * Returns <code>request.signal</code>.
     * <p>
     * <code>request.signal</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the signal / 신호
     */
    JsValue requestSignal(JsValue self, JsValue[] args) {
        return api.bridge().toJs(Natives.self(self, RequestObject.class).signal);
    }

    /**
     * Returns <code>request.duplex</code>.
     * <p>
     * <code>request.duplex</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return "half" / "half"
     */
    JsValue requestDuplex(JsValue self, JsValue[] args) {
        Natives.self(self, RequestObject.class);
        return Js.str("half");
    }

    /**
     * Implements <code>request.clone()</code>.
     * <p>
     * <code>request.clone()</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the copy / 복사본
     */
    JsValue requestClone(JsValue self, JsValue[] args) {
        RequestObject s = Natives.self(self, RequestObject.class);
        if (s.unusable()) {
            throw JsException.typeError("Failed to execute 'clone' on 'Request': Request body is already used");
        }
        RequestObject c = new RequestObject(requestProto);
        c.url = s.url;
        c.method = s.method;
        c.mode = s.mode;
        c.credentials = s.credentials;
        c.cache = s.cache;
        c.redirect = s.redirect;
        c.referrer = s.referrer;
        c.referrerPolicy = s.referrerPolicy;
        c.integrity = s.integrity;
        c.keepalive = s.keepalive;
        c.body = s.body;
        List<AbortSignal> sources = new ArrayList<AbortSignal>();
        sources.add(s.signal);
        c.signal = api.abort.dependent(sources);
        c.headers = api.headers.create(s.headers.guard);
        // 헤더 목록을 복사한다.
        for (int i = 0; i < s.headers.list.size(); i++) {
            c.headers.list.add(new String[] { s.headers.list.get(i)[0], s.headers.list.get(i)[1] });
        }
        return c;
    }

    // ------------------------------------------------------------------ Response

    /**
     * Tells whether a status is a null body status.
     * <p>
     * 상태가 본문 없는 상태인지 알려 준다.
     *
     * @param status the status / 상태
     * @return true for 101, 103, 204, 205 and 304 / 101, 103, 204, 205, 304 이면 true
     */
    private static boolean nullBodyStatus(int status) {
        return status == 101 || status == 103 || status == 204 || status == 205 || status == 304;
    }

    /**
     * Applies a <code>ResponseInit</code> (Fetch Standard "initialize a response").
     * <p>
     * <code>ResponseInit</code> 을 적용한다(Fetch 표준 "initialize a response").
     *
     * @param r    the response / 응답
     * @param init the init value / init 값
     * @param body the extracted body or null / 뽑아낸 본문 또는 null
     */
    private void initResponse(ResponseObject r, JsValue init, BodyApi.BodyData body) {
        JsObject dict = null;
        if (!init.isNullOrUndefined()) {
            if (!init.isObject()) {
                throw JsException.typeError("Failed to construct 'Response': The provided value is not of type 'ResponseInit'.");
            }
            dict = (JsObject) init;
        }
        int status = 200;
        JsValue sv = member(dict, "status");
        if (!sv.isUndefined()) {
            status = Ops.toUint16(sv);
        }
        if (status < 200 || status > 599) {
            throw JsException.rangeError("Failed to construct 'Response': The status provided (" + status + ") is outside the range [200, 599].");
        }
        String text = "";
        JsValue tv = member(dict, "statusText");
        if (!tv.isUndefined()) {
            text = Natives.byteString(tv, "constructor", "Response");
            // 이유 문구는 탭, 가시 ASCII, 확장 문자만 가질 수 있다.
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (!(c == '\t' || (c >= 0x20 && c <= 0x7E) || c >= 0x80)) {
                    throw JsException.typeError("Failed to construct 'Response': Invalid statusText");
                }
            }
        }
        r.status = status;
        r.statusText = text;
        JsValue hv = member(dict, "headers");
        if (!hv.isUndefined()) {
            if (hv instanceof HeadersApi.HeadersObject) {
                List<String[]> l = ((HeadersApi.HeadersObject) hv).list;
                // Headers 객체이면 그 목록의 헤더를 하나씩 덧붙인다.
                for (int i = 0; i < l.size(); i++) {
                    r.headers.append(l.get(i)[0], l.get(i)[1]);
                }
            } else {
                api.headers.fill(r.headers, hv);
            }
        }
        if (body != null) {
            if (nullBodyStatus(status)) {
                throw JsException.typeError("Failed to construct 'Response': Response with null body status cannot have body");
            }
            r.body = body.bytes;
            if (body.type != null && r.headers.get("content-type") == null) {
                r.headers.append("content-type", body.type);
            }
        }
    }

    /**
     * Implements the <code>Response</code> constructor.
     * <p>
     * <code>Response</code> 생성자를 구현한다.
     *
     * @param body  the body value / 본문 값
     * @param init  the init value / init 값
     * @param proto the prototype / 프로토타입
     * @return the response / 응답
     */
    private ResponseObject newResponse(JsValue body, JsValue init, JsObject proto) {
        ResponseObject r = new ResponseObject(proto);
        r.headers = api.headers.create("response");
        initResponse(r, init, body.isNullOrUndefined() ? null : bodies.extract(body));
        return r;
    }

    /**
     * Creates the <code>Response</code> that <code>fetch()</code> resolves with.
     * <p>
     * <code>fetch()</code> 가 이행 값으로 주는 <code>Response</code> 를 만든다.
     *
     * @param w          the filtered web response / 걸러진 웹 응답
     * @param noBodyData true if the request was HEAD, so that no body exists / 요청이 HEAD 여서 본문이 없으면 true
     * @return the response object / 응답 객체
     */
    private ResponseObject fromWeb(WebResponse w, boolean noBodyData) {
        ResponseObject r = new ResponseObject(responseProto);
        r.type = w.getType();
        r.url = w.getUrl();
        r.redirected = w.isRedirected();
        r.status = w.getStatus();
        r.statusText = w.getStatusText();
        r.headers = api.headers.create(w.getHeaders(), "immutable");
        boolean none = noBodyData || nullBodyStatus(r.status) || WebResponse.TYPE_OPAQUE.equals(w.getType());
        r.body = none ? null : w.getBody();
        return r;
    }

    /**
     * Returns <code>response.type</code>.
     * <p>
     * <code>response.type</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the type / 종류
     */
    JsValue responseType(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, ResponseObject.class).type);
    }

    /**
     * Returns <code>response.url</code>.
     * <p>
     * <code>response.url</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the URL without the fragment / 프래그먼트 없는 URL
     */
    JsValue responseUrl(JsValue self, JsValue[] args) {
        return Js.str(UrlUtil.stripFragment(Natives.self(self, ResponseObject.class).url));
    }

    /**
     * Returns <code>response.redirected</code>.
     * <p>
     * <code>response.redirected</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the flag / 표시
     */
    JsValue responseRedirected(JsValue self, JsValue[] args) {
        return Js.bool(Natives.self(self, ResponseObject.class).redirected);
    }

    /**
     * Returns <code>response.status</code>.
     * <p>
     * <code>response.status</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the status / 상태
     */
    JsValue responseStatus(JsValue self, JsValue[] args) {
        return Js.num(Natives.self(self, ResponseObject.class).status);
    }

    /**
     * Returns <code>response.ok</code>.
     * <p>
     * <code>response.ok</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return true for 200..299 / 200..299 이면 true
     */
    JsValue responseOk(JsValue self, JsValue[] args) {
        int s = Natives.self(self, ResponseObject.class).status;
        return Js.bool(s >= 200 && s <= 299);
    }

    /**
     * Returns <code>response.statusText</code>.
     * <p>
     * <code>response.statusText</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the reason phrase / 이유 문구
     */
    JsValue responseStatusText(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, ResponseObject.class).statusText);
    }

    /**
     * Returns <code>response.headers</code>.
     * <p>
     * <code>response.headers</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the headers object / headers 객체
     */
    JsValue responseHeaders(JsValue self, JsValue[] args) {
        return Natives.self(self, ResponseObject.class).headers;
    }

    /**
     * Implements <code>response.clone()</code>.
     * <p>
     * <code>response.clone()</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the copy / 복사본
     */
    JsValue responseClone(JsValue self, JsValue[] args) {
        ResponseObject s = Natives.self(self, ResponseObject.class);
        if (s.unusable()) {
            throw JsException.typeError("Failed to execute 'clone' on 'Response': Response body is already used");
        }
        ResponseObject c = new ResponseObject(responseProto);
        c.type = s.type;
        c.url = s.url;
        c.redirected = s.redirected;
        c.status = s.status;
        c.statusText = s.statusText;
        c.body = s.body;
        c.headers = api.headers.create(s.headers.guard);
        // 헤더 목록을 복사한다.
        for (int i = 0; i < s.headers.list.size(); i++) {
            c.headers.list.add(new String[] { s.headers.list.get(i)[0], s.headers.list.get(i)[1] });
        }
        return c;
    }

    /**
     * Implements <code>Response.error()</code>.
     * <p>
     * <code>Response.error()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the network error response / 네트워크 오류 응답
     */
    JsValue staticError(JsValue self, JsValue[] args) {
        ResponseObject r = new ResponseObject(responseProto);
        r.type = "error";
        r.status = 0;
        r.headers = api.headers.create("immutable");
        return r;
    }

    /**
     * Implements <code>Response.redirect(url, status)</code>.
     * <p>
     * <code>Response.redirect(url, status)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the redirect response / 리디렉션 응답
     */
    JsValue staticRedirect(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "redirect", "Response");
        String abs;
        try {
            abs = api.resolve(Natives.usv(args[0]));
        } catch (IllegalArgumentException e) {
            throw JsException.typeError("Failed to execute 'redirect' on 'Response': Invalid URL");
        }
        int status = args.length > 1 && !args[1].isUndefined() ? Ops.toUint16(args[1]) : 302;
        if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308) {
            throw JsException.rangeError("Failed to execute 'redirect' on 'Response': Invalid status code");
        }
        ResponseObject r = new ResponseObject(responseProto);
        r.status = status;
        r.headers = api.headers.create("immutable");
        r.headers.list.add(new String[] { "location", abs });
        return r;
    }

    /**
     * Implements <code>Response.json(data, init)</code>.
     * <p>
     * <code>Response.json(data, init)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the response / 응답
     */
    JsValue staticJson(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "json", "Response");
        JsValue text = Ops.invoke(api.realm(), api.realm().getIntrinsic("%JSON%"), "stringify", new JsValue[] { args[0] });
        if (text.isUndefined()) {
            throw JsException.typeError("Failed to execute 'json' on 'Response': The data is not JSON serializable");
        }
        ResponseObject r = new ResponseObject(responseProto);
        r.headers = api.headers.create("response");
        initResponse(r, Js.arg(args, 1), new BodyApi.BodyData(Encodings.utf8(Ops.toString(text)), "application/json"));
        return r;
    }

    // ------------------------------------------------------------------ fetch()

    /**
     * Implements <code>fetch(input, init)</code>.
     * <p>
     * <code>fetch(input, init)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the promise of a response / 응답의 프라미스
     */
    JsValue fetch(JsValue self, JsValue[] args) {
        JsRealm realm = api.realm();
        Promises.Deferred d = Promises.create(realm);
        RequestObject req;
        try {
            Natives.requireArgs(args, 1, "fetch", "Window");
            req = newRequest(args[0], Js.arg(args, 1), requestProto);
        } catch (JsException e) {
            d.reject(e.getValue(realm));
            return d.promise;
        }
        if (req.signal.isAborted()) {
            d.reject(req.signal.getReason());
            return d.promise;
        }
        if ("manual".equals(req.redirect)) {
            d.reject(JsException.typeError("Failed to fetch: redirect mode 'manual' is not supported").getValue(realm));
            return d.promise;
        }
        if ("only-if-cached".equals(req.cache)) {
            d.reject(JsException.typeError("Failed to fetch: cache mode 'only-if-cached' is not supported").getValue(realm));
            return d.promise;
        }
        WebRequestSource source = api.source();
        if (source == null || api.scheduler() == null) {
            realm.getConsole().message("error", "Failed to fetch " + req.url + ": network access is not available");
            d.reject(JsException.typeError("Failed to fetch").getValue(realm));
            return d.promise;
        }
        final Pending p = new Pending(d, req);
        if (req.body != null) {
            req.used = true;
        }
        p.abortStep = new Runnable() {
            public void run() {
                abortFetch(p);
            }
        };
        req.signal.addAlgorithm(p.abortStep);
        try {
            p.call = source.start(toWebRequest(req), p);
        } catch (RuntimeException e) {
            req.signal.removeAlgorithm(p.abortStep);
            realm.getConsole().message("error", "Failed to fetch " + req.url + ": " + e.getMessage());
            d.reject(JsException.typeError("Failed to fetch").getValue(realm));
            return d.promise;
        }
        api.track(p.call);
        return d.promise;
    }

    /**
     * Builds the request for the component's request layer.
     * <p>
     * 컴포넌트의 요청 계층에 줄 요청을 만든다.
     *
     * @param r the request object / 요청 객체
     * @return the web request / 웹 요청
     */
    private WebRequest toWebRequest(RequestObject r) {
        WebRequest w = new WebRequest(r.url).setMethod(r.method).setCredentials(r.credentials).setMode(r.mode).setCache(r.cache)
                .setRedirect(r.redirect).setReferrerPolicy(r.referrerPolicy);
        w.setReferrer("no-referrer".equals(r.referrer) ? WebRequest.REFERRER_NONE : "client".equals(r.referrer) ? WebRequest.REFERRER_CLIENT : r.referrer);
        Map<String, String[]> combined = new LinkedHashMap<String, String[]>();
        String contentType = null;
        // 같은 이름의 헤더 값은 ", " 로 합쳐 하나로 보낸다. 본문의 Content-Type 은 본문과 함께 넘긴다.
        for (int i = 0; i < r.headers.list.size(); i++) {
            String n = r.headers.list.get(i)[0];
            String v = r.headers.list.get(i)[1];
            if ("content-type".equals(n) && r.body != null) {
                contentType = contentType == null ? v : contentType + ", " + v;
                continue;
            }
            String[] old = combined.get(n);
            if (old == null) {
                combined.put(n, new String[] { n, v });
            } else {
                old[1] = old[1] + ", " + v;
            }
        }
        // 합친 헤더를 요청에 옮긴다.
        for (String[] h : combined.values()) {
            w.addHeader(h[0], h[1]);
        }
        w.setBody(r.body, contentType);
        return w;
    }

    /**
     * The abort step of a <code>fetch()</code>: reject the promise and cancel the network call.
     * <p>
     * <code>fetch()</code> 의 중단 단계: 프라미스를 reject 하고 네트워크 호출을 취소한다.
     *
     * @param p the call / 호출
     */
    private void abortFetch(Pending p) {
        if (p.done) {
            return;
        }
        p.done = true;
        if (p.call != null) {
            p.call.cancel();
            api.untrack(p.call);
        }
        p.deferred.reject(p.request.signal.getReason());
    }

    /**
     * Settles the promise of a <code>fetch()</code> when its response arrived (runs as a page task).
     * <p>
     * 응답이 도착하면 <code>fetch()</code> 의 프라미스를 확정한다(페이지 작업으로 실행).
     *
     * @param p        the call / 호출
     * @param response the filtered response / 걸러진 응답
     */
    private void finish(Pending p, WebResponse response) {
        if (p.done) {
            return;
        }
        p.done = true;
        p.request.signal.removeAlgorithm(p.abortStep);
        if (p.call != null) {
            api.untrack(p.call);
        }
        if (response.isError()) {
            api.realm().getConsole().message("error", "Failed to fetch " + p.request.url + ": " + response.getErrorMessage());
            p.deferred.reject(JsException.typeError("Failed to fetch").getValue(api.realm()));
            return;
        }
        if (p.request.integrity.length() > 0 && (WebResponse.TYPE_OPAQUE.equals(response.getType()) || !integrityOk(p.request.integrity, response.getBody()))) {
            api.realm().getConsole().message("error", "Failed to find a valid digest in the 'integrity' attribute for resource '" + p.request.url
                    + "' with computed digest");
            p.deferred.reject(JsException.typeError("Failed to fetch").getValue(api.realm()));
            return;
        }
        p.deferred.resolve(fromWeb(response, "HEAD".equals(p.request.method)));
    }

    /**
     * Checks subresource integrity metadata (Subresource Integrity, "does response match metadataList"): the strongest of the listed
     * algorithms (sha512, sha384, sha256) is computed over the body and must equal one of the digests that name it. Metadata without a
     * recognized token always matches, as the standard says.
     * <p>
     * 하위 리소스 무결성 메타데이터를 검사한다(Subresource Integrity, "does response match metadataList"): 나열된 알고리즘 중 가장 강한 것(sha512, sha384,
     * sha256)을 본문에 대해 계산하여 그 알고리즘을 쓴 다이제스트 중 하나와 같아야 한다. 인식하는 토큰이 없는 메타데이터는 표준대로 항상 일치한다.
     *
     * @param metadata the integrity attribute value / integrity 속성 값
     * @param body     the body bytes / 본문 바이트
     * @return true if the body matches / 본문이 일치하면 true
     */
    static boolean integrityOk(String metadata, byte[] body) {
        String[] algs = { "sha512", "sha384", "sha256" };
        String[] names = { "SHA-512", "SHA-384", "SHA-256" };
        String[] tokens = metadata.trim().split("\\s+");
        // 가장 강한 알고리즘부터 그 알고리즘을 쓴 토큰이 있는지 찾는다.
        for (int a = 0; a < algs.length; a++) {
            List<byte[]> digests = new ArrayList<byte[]>();
            // 토큰마다 현재 알고리즘의 다이제스트를 모은다.
            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i];
                int q = t.indexOf('?');
                if (q >= 0) {
                    t = t.substring(0, q);
                }
                if (t.startsWith(algs[a] + "-")) {
                    byte[] d = Encodings.base64Decode(t.substring(algs[a].length() + 1));
                    if (d != null) {
                        digests.add(d);
                    }
                }
            }
            if (digests.isEmpty()) {
                continue;
            }
            byte[] actual;
            try {
                actual = java.security.MessageDigest.getInstance(names[a]).digest(body);
            } catch (java.security.NoSuchAlgorithmException e) {
                return false;
            }
            // 같은 알고리즘의 다이제스트 중 하나라도 본문의 것과 같으면 일치한다.
            for (int i = 0; i < digests.size(); i++) {
                if (java.util.Arrays.equals(digests.get(i), actual)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}
