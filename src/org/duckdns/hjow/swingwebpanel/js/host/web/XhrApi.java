package org.duckdns.hjow.swingwebpanel.js.host.web;

import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlResult;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * Installs <code>XMLHttpRequest</code> (XMLHttpRequest Standard) with <code>XMLHttpRequestEventTarget</code>, <code>XMLHttpRequestUpload</code> and
 * <code>ProgressEvent</code>. The state machine is in {@link Xhr}; this class connects it to the scripts: the interface objects with their
 * constants, the attributes and operations with the WebIDL argument conversions, the <code>on...</code> event handler properties, and the
 * construction of response documents (<code>responseType = "document"</code> and <code>responseXML</code>) with the component's own HTML and XML
 * parsers. A response document has no scripts, loads nothing and has no link to the page; <code>responseXML</code> is only built for XML
 * MIME types (and for HTML when the type is <code>"document"</code>).
 * <p>
 * <code>XMLHttpRequest</code>(XMLHttpRequest 표준)를 <code>XMLHttpRequestEventTarget</code>, <code>XMLHttpRequestUpload</code>, <code>ProgressEvent</code> 와
 * 함께 설치한다. 상태 기계는 {@link Xhr} 에 있고 이 클래스는 그것을 스크립트에 연결한다: 상수를 가진 인터페이스 객체, WebIDL 인수 변환을 거치는 속성과
 * 연산, <code>on...</code> 이벤트 핸들러 프로퍼티, 컴포넌트 자신의 HTML/XML 파서로 응답 문서(<code>responseType = "document"</code> 와
 * <code>responseXML</code>)를 만드는 일. 응답 문서에는 스크립트가 없고 아무것도 읽어 오지 않으며 페이지와 연결되지 않는다. <code>responseXML</code> 은
 * XML MIME 타입에서만(그리고 타입이 <code>"document"</code> 일 때 HTML 에서) 만든다.
 */
final class XhrApi {

    /** The valid response types. / 올바른 응답 종류. */
    private static final String[] RESPONSE_TYPES = { "", "arraybuffer", "blob", "document", "json", "text" };

    /** The events of an XMLHttpRequestEventTarget. / XMLHttpRequestEventTarget 의 이벤트. */
    private static final String[] TARGET_EVENTS = { "loadstart", "progress", "abort", "error", "load", "timeout", "loadend" };

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The body helper. / 본문 도우미. */
    private final BodyApi bodies;

    /** The prototype of <code>XMLHttpRequest</code>. / <code>XMLHttpRequest</code> 의 프로토타입. */
    private JsObject xhrProto;

    /** The prototype of <code>ProgressEvent</code>. / <code>ProgressEvent</code> 의 프로토타입. */
    private JsObject progressProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    XhrApi(WebApis api) {
        this.api = api;
        this.bodies = new BodyApi(api);
    }

    /**
     * Installs the interfaces.
     * <p>
     * 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = api.bridge();
        final JsRealm realm = api.realm();
        bridge.addClassifier(new DomBridge.Classifier() {
            public String classify(Object target) {
                if (target instanceof Xhr) {
                    return "XMLHttpRequest";
                }
                return target instanceof XhrUpload ? "XMLHttpRequestUpload" : null;
            }
        });
        DomBridge.InterfaceInfo et = bridge.defineInterface("XMLHttpRequestEventTarget", "EventTarget", XhrEventTarget.class, new String[0]);
        bridge.getEventHandlers().define(et.getPrototype(), XhrEventTarget.class, TARGET_EVENTS);
        bridge.defineInterface("XMLHttpRequestUpload", "XMLHttpRequestEventTarget", XhrUpload.class, new String[0]);
        DomBridge.InterfaceInfo xi = bridge.defineInterface("XMLHttpRequest", "XMLHttpRequestEventTarget", Xhr.class, new String[0]);
        xhrProto = xi.getPrototype();
        bridge.getEventHandlers().define(xhrProto, Xhr.class, new String[] { "readystatechange" });
        NativeFunction xhrCtor = new NativeFunction(realm, realm.getFunctionPrototype(), "XMLHttpRequest", 0,
                new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                        throw JsException.typeError("Failed to construct 'XMLHttpRequest': Please use the 'new' operator, this DOM object "
                                + "constructor cannot be called as a function.");
                    }
                }, new NativeFunction.Construct() {
                    public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                        HostObject w = bridge.wrap(new Xhr(api, XhrApi.this));
                        w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, xhrProto));
                        return w;
                    }
                });
        bridge.setConstructor("XMLHttpRequest", xhrCtor);
        String[] states = { "UNSENT", "OPENED", "HEADERS_RECEIVED", "LOADING", "DONE" };
        // 상태 상수를 새 인터페이스 객체와 프로토타입에 정의한다.
        for (int i = 0; i < states.length; i++) {
            xhrCtor.defineData(states[i], Js.num(i), false, true, false);
            xhrProto.defineData(states[i], Js.num(i), false, true, false);
        }
        Natives.getter(realm, xhrProto, "readyState", this, "readyState");
        Natives.method(realm, xhrProto, "open", 2, this, "open");
        Natives.method(realm, xhrProto, "setRequestHeader", 2, this, "setRequestHeader");
        Natives.accessor(realm, xhrProto, "timeout", this, "getTimeout", "setTimeout");
        Natives.accessor(realm, xhrProto, "withCredentials", this, "getWithCredentials", "setWithCredentials");
        Natives.getter(realm, xhrProto, "upload", this, "upload");
        Natives.method(realm, xhrProto, "send", 0, this, "send");
        Natives.method(realm, xhrProto, "abort", 0, this, "abort");
        Natives.getter(realm, xhrProto, "responseURL", this, "responseUrl");
        Natives.getter(realm, xhrProto, "status", this, "status");
        Natives.getter(realm, xhrProto, "statusText", this, "statusText");
        Natives.method(realm, xhrProto, "getResponseHeader", 1, this, "getResponseHeader");
        Natives.method(realm, xhrProto, "getAllResponseHeaders", 0, this, "getAllResponseHeaders");
        Natives.method(realm, xhrProto, "overrideMimeType", 1, this, "overrideMimeType");
        Natives.accessor(realm, xhrProto, "responseType", this, "getResponseType", "setResponseType");
        Natives.getter(realm, xhrProto, "response", this, "response");
        Natives.getter(realm, xhrProto, "responseText", this, "responseText");
        Natives.getter(realm, xhrProto, "responseXML", this, "responseXml");
        installProgressEvent(bridge);
    }

    /**
     * Installs <code>ProgressEvent</code>, including its constructor.
     * <p>
     * <code>ProgressEvent</code> 를 생성자와 함께 설치한다.
     *
     * @param bridge the DOM bridge / DOM 연결
     */
    private void installProgressEvent(final DomBridge bridge) {
        final JsRealm realm = api.realm();
        DomBridge.InterfaceInfo pi = bridge.defineInterface("ProgressEvent", "Event", ProgressEvent.class,
                new String[] { "a:lengthComputable", "a:loaded", "a:total" });
        progressProto = pi.getPrototype();
        bridge.setConstructor("ProgressEvent", new NativeFunction(realm, realm.getFunctionPrototype(), "ProgressEvent", 1,
                new NativeFunction.Call() {
                    public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                        throw JsException.typeError("Failed to construct 'ProgressEvent': Please use the 'new' operator, this DOM object "
                                + "constructor cannot be called as a function.");
                    }
                }, new NativeFunction.Construct() {
                    public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                        Natives.requireArgs(args, 1, "constructor", "ProgressEvent");
                        JsValue opts = Js.arg(args, 1);
                        boolean computable = false;
                        double loaded = 0;
                        double total = 0;
                        boolean bubbles = false;
                        boolean cancelable = false;
                        if (opts.isObject()) {
                            JsObject o = (JsObject) opts;
                            computable = Ops.toBoolean(o.get("lengthComputable"));
                            loaded = o.get("loaded").isUndefined() ? 0 : Ops.toNumber(o.get("loaded"));
                            total = o.get("total").isUndefined() ? 0 : Ops.toNumber(o.get("total"));
                            bubbles = Ops.toBoolean(o.get("bubbles"));
                            cancelable = Ops.toBoolean(o.get("cancelable"));
                        }
                        HostObject w = bridge.wrap(new ProgressEvent(Ops.toString(args[0]), bubbles, cancelable, computable, loaded, total));
                        w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, progressProto));
                        return w;
                    }
                }));
        // 이벤트 래퍼 팩터리: ProgressEvent 의 래퍼는 ProgressEvent 프로토타입을 쓴다. 다른 이벤트는 일반 래퍼이다.
        bridge.setFactory("Event", new DomBridge.Factory() {
            public HostObject create(DomBridge b, JsObject proto, Object target) {
                return new HostObject(b, target instanceof ProgressEvent ? progressProto : proto, target);
            }
        });
    }

    /**
     * Returns the request object that this value stands for or throws the standard TypeError.
     * <p>
     * 이 값이 나타내는 요청 객체를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the object / 객체
     */
    private static Xhr xhr(JsValue self) {
        Object t = DomBridge.targetOf(self, Xhr.class);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return (Xhr) t;
    }

    /**
     * Returns <code>readyState</code>.
     * <p>
     * <code>readyState</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the state / 상태
     */
    JsValue readyState(JsValue self, JsValue[] args) {
        return Js.num(xhr(self).getReadyState());
    }

    /**
     * Implements <code>open(method, url, async, user, password)</code>.
     * <p>
     * <code>open(method, url, async, user, password)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue open(JsValue self, JsValue[] args) {
        Xhr x = xhr(self);
        Natives.requireArgs(args, 2, "open", "XMLHttpRequest");
        String method = Natives.byteString(args[0], "open", "XMLHttpRequest");
        String url = Natives.usv(args[1]);
        boolean async = args.length > 2 ? Ops.toBoolean(args[2]) : true;
        x.open(method, url, async);
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>setRequestHeader(name, value)</code>.
     * <p>
     * <code>setRequestHeader(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setRequestHeader(JsValue self, JsValue[] args) {
        Xhr x = xhr(self);
        Natives.requireArgs(args, 2, "setRequestHeader", "XMLHttpRequest");
        x.setRequestHeader(Natives.byteString(args[0], "setRequestHeader", "XMLHttpRequest"),
                Natives.byteString(args[1], "setRequestHeader", "XMLHttpRequest"));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>timeout</code>.
     * <p>
     * <code>timeout</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the timeout / 제한 시간
     */
    JsValue getTimeout(JsValue self, JsValue[] args) {
        return Js.num(xhr(self).getTimeout());
    }

    /**
     * Sets <code>timeout</code>.
     * <p>
     * <code>timeout</code> 을 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setTimeout(JsValue self, JsValue[] args) {
        xhr(self).setTimeout(Ops.toUint32(Js.arg(args, 0)));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>withCredentials</code>.
     * <p>
     * <code>withCredentials</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the flag / 표시
     */
    JsValue getWithCredentials(JsValue self, JsValue[] args) {
        return Js.bool(xhr(self).getWithCredentials());
    }

    /**
     * Sets <code>withCredentials</code>.
     * <p>
     * <code>withCredentials</code> 를 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setWithCredentials(JsValue self, JsValue[] args) {
        xhr(self).setWithCredentials(Ops.toBoolean(Js.arg(args, 0)));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>upload</code>.
     * <p>
     * <code>upload</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the upload object / 업로드 객체
     */
    JsValue upload(JsValue self, JsValue[] args) {
        return api.bridge().toJs(xhr(self).upload);
    }

    /**
     * Implements <code>send(body)</code>.
     * <p>
     * <code>send(body)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue send(JsValue self, JsValue[] args) {
        Xhr x = xhr(self);
        JsValue body = Js.arg(args, 0);
        BodyApi.BodyData data = null;
        boolean text = false;
        if (!body.isNullOrUndefined()) {
            Object doc = DomBridge.targetOf(body, Document.class);
            if (doc instanceof Document) {
                Document d = (Document) doc;
                Element root = d.getDocumentElement();
                String markup = root == null ? "" : root.getOuterHtml();
                data = new BodyApi.BodyData(Encodings.utf8(markup), d.isHtmlDocument() ? "text/html;charset=UTF-8" : "application/xml;charset=UTF-8");
                text = true;
            } else {
                data = bodies.extract(body);
                text = !(body instanceof BlobApi.BlobObject) && !(body instanceof FormDataApi.FormDataObject)
                        && TypedArrays.bytesOf(body) == null;
            }
        }
        x.send(data, text);
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>abort()</code>.
     * <p>
     * <code>abort()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue abort(JsValue self, JsValue[] args) {
        xhr(self).abort();
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>responseURL</code>.
     * <p>
     * <code>responseURL</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the URL / URL
     */
    JsValue responseUrl(JsValue self, JsValue[] args) {
        return Js.str(xhr(self).getResponseUrl());
    }

    /**
     * Returns <code>status</code>.
     * <p>
     * <code>status</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the status / 상태
     */
    JsValue status(JsValue self, JsValue[] args) {
        return Js.num(xhr(self).getStatus());
    }

    /**
     * Returns <code>statusText</code>.
     * <p>
     * <code>statusText</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the reason phrase / 이유 문구
     */
    JsValue statusText(JsValue self, JsValue[] args) {
        return Js.str(xhr(self).getStatusText());
    }

    /**
     * Implements <code>getResponseHeader(name)</code>.
     * <p>
     * <code>getResponseHeader(name)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the value or null / 값 또는 null
     */
    JsValue getResponseHeader(JsValue self, JsValue[] args) {
        Xhr x = xhr(self);
        Natives.requireArgs(args, 1, "getResponseHeader", "XMLHttpRequest");
        String v = x.getResponseHeader(Natives.byteString(args[0], "getResponseHeader", "XMLHttpRequest"));
        return v == null ? Js.NULL : Js.str(v);
    }

    /**
     * Implements <code>getAllResponseHeaders()</code>.
     * <p>
     * <code>getAllResponseHeaders()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the headers text / 헤더 텍스트
     */
    JsValue getAllResponseHeaders(JsValue self, JsValue[] args) {
        return Js.str(xhr(self).getAllResponseHeaders());
    }

    /**
     * Implements <code>overrideMimeType(mime)</code>.
     * <p>
     * <code>overrideMimeType(mime)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue overrideMimeType(JsValue self, JsValue[] args) {
        Xhr x = xhr(self);
        Natives.requireArgs(args, 1, "overrideMimeType", "XMLHttpRequest");
        x.overrideMimeType(Ops.toString(args[0]));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>responseType</code>.
     * <p>
     * <code>responseType</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the type / 종류
     */
    JsValue getResponseType(JsValue self, JsValue[] args) {
        return Js.str(xhr(self).getResponseType());
    }

    /**
     * Sets <code>responseType</code>; a value outside the enumeration is ignored as WebIDL says.
     * <p>
     * <code>responseType</code> 을 지정한다. 열거형 밖의 값은 WebIDL 대로 무시한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setResponseType(JsValue self, JsValue[] args) {
        Xhr x = xhr(self);
        String v = Ops.toString(Js.arg(args, 0));
        // worker 에는 문서가 없으므로 XHR 표준대로 "document" 는 조용히 무시한다.
        if ("document".equals(v) && !api.hasDocuments()) {
            return Js.UNDEFINED;
        }
        // 열거형에 없는 값은 설정하지 않고 무시한다.
        for (int i = 0; i < RESPONSE_TYPES.length; i++) {
            if (RESPONSE_TYPES[i].equals(v)) {
                x.setResponseType(v);
                return Js.UNDEFINED;
            }
        }
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>response</code>.
     * <p>
     * <code>response</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the response / 응답
     */
    JsValue response(JsValue self, JsValue[] args) {
        return xhr(self).getResponse();
    }

    /**
     * Returns <code>responseText</code>.
     * <p>
     * <code>responseText</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the text / 텍스트
     */
    JsValue responseText(JsValue self, JsValue[] args) {
        return Js.str(xhr(self).getResponseText());
    }

    /**
     * Returns <code>responseXML</code>.
     * <p>
     * <code>responseXML</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the document or null / 문서 또는 null
     */
    JsValue responseXml(JsValue self, JsValue[] args) {
        return xhr(self).getResponseXml();
    }

    /**
     * Builds the response document of an HTML or XML response; null if the response cannot be parsed as such a document.
     * <p>
     * HTML 또는 XML 응답의 응답 문서를 만든다. 그런 문서로 해석할 수 없으면 null.
     *
     * @param xhr     the request / 요청
     * @param bytes   the body / 본문
     * @param mime    the final MIME type / 최종 MIME 타입
     * @param charset the charset label or null / 문자셋 레이블 또는 null
     * @param url     the URL of the response / 응답의 URL
     * @return the document or null / 문서 또는 null
     */
    JsValue documentOf(Xhr xhr, byte[] bytes, String mime, String charset, String url) {
        // 문서가 없는 문맥(worker)에서는 응답 문서를 만들지 않는다.
        if (!api.hasDocuments()) {
            return Js.NULL;
        }
        String base = mime.indexOf(';') < 0 ? mime : mime.substring(0, mime.indexOf(';'));
        if ("text/html".equals(base)) {
            Document d = HtmlParser.parseDocument(bytes, charset, url);
            CssSelectorEngine.install(d);
            return api.bridge().toJs(d);
        }
        if (base.endsWith("xml")) {
            XhtmlResult r = XhtmlParser.parseDocument(bytes, charset, url);
            if (!r.isWellFormed() || r.getDocument() == null) {
                return Js.NULL;
            }
            CssSelectorEngine.install(r.getDocument());
            return api.bridge().toJs(r.getDocument());
        }
        return Js.NULL;
    }
}
