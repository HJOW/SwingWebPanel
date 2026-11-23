package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.dom.CloseEvent;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * The <code>WebSocket</code> interface (HTML Standard "Network" chapter) and <code>CloseEvent</code> under the scheme policy of this
 * component (feature matrix EXC-06): the component opens only <code>http:</code>, <code>https:</code> and <code>ftp:</code> addresses, so no
 * WebSocket connection (<code>ws:</code>, <code>wss:</code>) can ever be made. The constructor still follows the specification for what it
 * can check without a network: a URL that cannot be parsed, has a fragment, or has a scheme other than <code>ws</code>, <code>wss</code>,
 * <code>http</code> or <code>https</code> throws a <code>SyntaxError</code>; sub-protocols that are not tokens or are repeated throw a
 * <code>SyntaxError</code>. A valid request returns a socket in the <code>CONNECTING</code> state whose connection then fails like a refused
 * connection: as a page task the socket fires <code>error</code>, its <code>readyState</code> becomes <code>CLOSED</code> and it fires
 * <code>close</code> with <code>wasClean</code> false and the code 1006. Pages that fall back to polling when the socket fails therefore
 * keep working. <code>send</code> in the <code>CONNECTING</code> state throws <code>InvalidStateError</code>, in the other states it does
 * nothing; <code>close</code> checks its arguments like the specification (1000 or 3000-4999, a reason of at most 123 bytes in UTF-8).
 * <p>
 * <code>WebSocket</code> 인터페이스(HTML Standard "Network" 장)와 <code>CloseEvent</code> 를 이 컴포넌트의 스킴 정책(기능 표 EXC-06) 아래에서
 * 제공한다: 컴포넌트는 <code>http:</code>, <code>https:</code>, <code>ftp:</code> 주소만 열므로 WebSocket 연결(<code>ws:</code>,
 * <code>wss:</code>)은 만들 수 없다. 생성자는 네트워크 없이 확인할 수 있는 것은 규격대로 처리한다: 해석할 수 없거나 프래그먼트가 있거나 스킴이
 * <code>ws</code>, <code>wss</code>, <code>http</code>, <code>https</code> 가 아닌 URL, 토큰이 아니거나 중복된 하위 프로토콜은
 * <code>SyntaxError</code> 이다. 올바른 요청은 <code>CONNECTING</code> 상태의 소켓을 반환하고, 연결은 거부된 연결처럼 실패한다: 페이지 작업으로
 * 소켓이 <code>error</code> 를 발생시키고 <code>readyState</code> 가 <code>CLOSED</code> 가 되며 <code>wasClean</code> 이 false, 코드가 1006 인
 * <code>close</code> 를 발생시킨다. 소켓이 실패하면 폴링으로 되돌아가는 페이지는 그대로 동작한다. <code>CONNECTING</code> 상태의
 * <code>send</code> 는 <code>InvalidStateError</code> 를 던지고 다른 상태에서는 아무것도 하지 않는다. <code>close</code> 는 인수를 규격대로
 * 검사한다(1000 또는 3000-4999, UTF-8 로 123바이트 이하의 이유).
 */
final class WebSocketApi {

    /**
     * A socket: the state that the script sees.
     * <p>
     * 소켓: 스크립트가 보는 상태.
     */
    static final class Socket extends EventTarget {
        /**
         * The URL that the constructor resolved.
         * <p>
         * 생성자가 해석한 URL.
         */
        private final String url;

        /**
         * The ready state: 0 connecting, 1 open, 2 closing, 3 closed.
         * <p>
         * 준비 상태: 0 연결 중, 1 열림, 2 닫는 중, 3 닫힘.
         */
        private int readyState;

        /**
         * The binary type: <code>blob</code> or <code>arraybuffer</code>.
         * <p>
         * 이진 형식: <code>blob</code> 또는 <code>arraybuffer</code>.
         */
        private String binaryType = "blob";

        /**
         * Creates a socket in the connecting state.
         * <p>
         * 연결 중 상태의 소켓을 만든다.
         *
         * @param url the resolved URL / 해석한 URL
         */
        Socket(String url) {
            this.url = url;
        }
    }

    /**
     * The state names and values.
     * <p>
     * 상태 이름과 값.
     */
    private static final String[] STATES = { "CONNECTING", "OPEN", "CLOSING", "CLOSED" };

    /**
     * The installer of the web APIs of the page.
     * <p>
     * 페이지의 웹 API 설치기.
     */
    private final WebApis api;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the web APIs of the page / 페이지의 웹 API
     */
    WebSocketApi(WebApis api) {
        this.api = api;
    }

    /**
     * Returns the socket of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 소켓을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the socket / 소켓
     */
    private static Socket socket(JsValue self) {
        Object t = DomBridge.targetOf(self, Socket.class);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return (Socket) t;
    }

    /**
     * Resolves the URL of the constructor and checks it (the "parse a WebSocket URL" steps without the connection).
     * <p>
     * 생성자의 URL 을 해석하고 검사한다(연결을 제외한 "WebSocket URL 해석" 단계).
     *
     * @param text the script value / 스크립트 값
     * @return the resolved URL / 해석한 URL
     */
    private String resolveUrl(String text) {
        String abs;
        try {
            abs = api.resolve(text);
            URI uri = new URI(abs);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            // ws, wss 와 http, https 만 WebSocket 주소가 될 수 있다.
            if (!("ws".equals(scheme) || "wss".equals(scheme) || "http".equals(scheme) || "https".equals(scheme))) {
                throw api.domError("SyntaxError", "The URL's scheme must be either 'ws', 'wss', 'http', or 'https'. '" + scheme + "' is not allowed.");
            }
            if (uri.getRawFragment() != null) {
                throw api.domError("SyntaxError", "The URL contains a fragment identifier ('#" + uri.getRawFragment() + "'). Fragment identifiers are "
                        + "not allowed in WebSocket URLs.");
            }
        } catch (URISyntaxException e) {
            throw api.domError("SyntaxError", "The URL '" + text + "' is invalid.");
        }
        return abs;
    }

    /**
     * Checks the sub-protocol names: each must be a token (RFC 2616) and none may repeat.
     * <p>
     * 하위 프로토콜 이름을 검사한다: 각각 토큰(RFC 2616)이어야 하고 중복이 없어야 한다.
     *
     * @param value the script value: a string or a sequence of strings / 스크립트 값: 문자열 또는 문자열 시퀀스
     */
    private void checkProtocols(JsValue value) {
        if (value.isUndefined()) {
            return;
        }
        JsValue[] items = value.isObject() ? Ops.createListFromArrayLike(value) : new JsValue[] { value };
        Set<String> seen = new HashSet<String>();
        // 이름마다 토큰 문자만 있는지, 앞에 나온 이름과 겹치지 않는지 확인한다.
        for (int i = 0; i < items.length; i++) {
            String p = Ops.toString(items[i]);
            boolean token = p.length() > 0;
            // 문자가 모두 토큰 문자인지 하나씩 확인한다.
            for (int c = 0; token && c < p.length(); c++) {
                char ch = p.charAt(c);
                token = ch > 0x20 && ch < 0x7f && "()<>@,;:\\\"/[]?={}".indexOf(ch) < 0;
            }
            if (!token) {
                throw api.domError("SyntaxError", "The subprotocol '" + p + "' is invalid.");
            }
            if (!seen.add(p)) {
                throw api.domError("SyntaxError", "The subprotocol '" + p + "' is duplicated.");
            }
        }
    }

    /**
     * Fires the failure of the connection: <code>error</code>, then the state <code>CLOSED</code> and <code>close</code> (code 1006).
     * <p>
     * 연결 실패를 알린다: <code>error</code>, 그 뒤 <code>CLOSED</code> 상태와 <code>close</code>(코드 1006).
     *
     * @param s the socket / 소켓
     */
    private void fail(Socket s) {
        // 이미 닫힌 소켓은 아무것도 하지 않는다. 연결 중이거나 연결이 끝나기 전에 닫는 중인 소켓은 실패로 끝난다.
        if (s.readyState == 3) {
            return;
        }
        s.dispatchEvent(new Event("error", false, false));
        s.readyState = 3;
        s.dispatchEvent(new CloseEvent("close", false, false, 1006, ""));
    }

    /**
     * Installs the interfaces and the constructor.
     * <p>
     * 인터페이스와 생성자를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = api.bridge();
        final JsRealm realm = api.realm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a socket.
             * <p>
             * 소켓의 인터페이스를 고른다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object target) {
                return target instanceof Socket ? "WebSocket" : null;
            }
        });
        bridge.defineInterface("CloseEvent", "Event", CloseEvent.class, new String[] { "a:wasClean", "a:code", "a:reason" });
        DomBridge.InterfaceInfo info = bridge.defineInterface("WebSocket", "EventTarget", Socket.class, new String[0]);
        final JsObject proto = info.getPrototype();
        bridge.getEventHandlers().define(proto, Socket.class, new String[] { "open", "message", "error", "close" });
        NativeFunction ctor = new NativeFunction(realm, realm.getFunctionPrototype(), "WebSocket", 1, new NativeFunction.Call() {
            /**
             * Refuses a call without <code>new</code>.
             * <p>
             * <code>new</code> 없는 호출을 거부한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never returns / 반환하지 않는다
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'WebSocket': Please use the 'new' operator, this DOM object constructor cannot "
                        + "be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a socket whose connection fails (see the class description).
             * <p>
             * 연결이 실패하는 소켓을 만든다(클래스 설명 참조).
             *
             * @param r         the realm / realm
             * @param args      arguments: URL and sub-protocols / 인수: URL 과 하위 프로토콜
             * @param newTarget the new target / new 대상
             * @return the socket / 소켓
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Natives.requireArgs(args, 1, "constructor", "WebSocket");
                final Socket s = new Socket(resolveUrl(Natives.usv(args[0])));
                checkProtocols(Js.arg(args, 1));
                HostObject w = bridge.wrap(s);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                // 연결은 다음 작업에서 실패한다(스킴 정책 때문에 연결을 만들 수 없다).
                api.postLater(new Runnable() {
                    /**
                     * Fires the failure; the scheduler runs this inside a host (page or worker) task.
                     * <p>
                     * 실패를 알린다. 스케줄러가 이 작업을 호스트(페이지 또는 worker) 작업 안에서 실행한다.
                     */
                    public void run() {
                        fail(s);
                    }
                }, 0);
                return w;
            }
        });
        bridge.setConstructor("WebSocket", ctor);
        // 상태 상수를 인터페이스 객체와 프로토타입에 정의한다.
        for (int i = 0; i < STATES.length; i++) {
            ctor.defineData(STATES[i], Js.num(i), false, true, false);
            proto.defineData(STATES[i], Js.num(i), false, true, false);
        }
        Natives.getter(realm, proto, "url", this, "url");
        Natives.getter(realm, proto, "readyState", this, "readyState");
        Natives.getter(realm, proto, "bufferedAmount", this, "bufferedAmount");
        Natives.getter(realm, proto, "extensions", this, "extensions");
        Natives.getter(realm, proto, "protocol", this, "protocol");
        Natives.accessor(realm, proto, "binaryType", this, "getBinaryType", "setBinaryType");
        Natives.method(realm, proto, "send", 1, this, "send");
        Natives.method(realm, proto, "close", 0, this, "close");
    }

    /**
     * Returns <code>url</code>.
     * <p>
     * <code>url</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the URL / URL
     */
    JsValue url(JsValue self, JsValue[] args) {
        return Js.str(socket(self).url);
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
        return Js.num(socket(self).readyState);
    }

    /**
     * Returns <code>bufferedAmount</code> (nothing is ever buffered).
     * <p>
     * <code>bufferedAmount</code> 를 반환한다(버퍼에 쌓이는 것이 없다).
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return 0 / 0
     */
    JsValue bufferedAmount(JsValue self, JsValue[] args) {
        socket(self);
        return Js.num(0);
    }

    /**
     * Returns <code>extensions</code> (none are negotiated).
     * <p>
     * <code>extensions</code> 를 반환한다(협상한 확장이 없다).
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an empty string / 빈 문자열
     */
    JsValue extensions(JsValue self, JsValue[] args) {
        socket(self);
        return Js.str("");
    }

    /**
     * Returns <code>protocol</code> (none is negotiated).
     * <p>
     * <code>protocol</code> 을 반환한다(협상한 프로토콜이 없다).
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an empty string / 빈 문자열
     */
    JsValue protocol(JsValue self, JsValue[] args) {
        socket(self);
        return Js.str("");
    }

    /**
     * Returns <code>binaryType</code>.
     * <p>
     * <code>binaryType</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the type / 형식
     */
    JsValue getBinaryType(JsValue self, JsValue[] args) {
        return Js.str(socket(self).binaryType);
    }

    /**
     * Sets <code>binaryType</code>: only <code>blob</code> and <code>arraybuffer</code> are accepted, other values are ignored.
     * <p>
     * <code>binaryType</code> 을 지정한다: <code>blob</code> 과 <code>arraybuffer</code> 만 받고 다른 값은 무시한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setBinaryType(JsValue self, JsValue[] args) {
        Socket s = socket(self);
        String v = Ops.toString(Js.arg(args, 0));
        if ("blob".equals(v) || "arraybuffer".equals(v)) {
            s.binaryType = v;
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>send(data)</code>: <code>InvalidStateError</code> while connecting, nothing in the other states.
     * <p>
     * <code>send(data)</code> 를 구현한다: 연결 중이면 <code>InvalidStateError</code>, 다른 상태에서는 아무것도 하지 않는다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue send(JsValue self, JsValue[] args) {
        Socket s = socket(self);
        Natives.requireArgs(args, 1, "send", "WebSocket");
        if (s.readyState == 0) {
            throw api.domError("InvalidStateError", "Still in CONNECTING state.");
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>close(code, reason)</code>: it checks the arguments and, if the socket is still connecting, moves it to the
     * <code>CLOSING</code> state (the failure that is already queued then ends it with <code>error</code> and <code>close</code>).
     * <p>
     * <code>close(code, reason)</code> 를 구현한다: 인수를 검사하고, 소켓이 아직 연결 중이면 <code>CLOSING</code> 상태로 옮긴다(이미 줄 선 실패가
     * <code>error</code> 와 <code>close</code> 로 끝낸다).
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue close(JsValue self, JsValue[] args) {
        Socket s = socket(self);
        JsValue codeValue = Js.arg(args, 0);
        if (!codeValue.isUndefined()) {
            double code = Ops.toNumber(codeValue);
            // 닫기 코드는 1000 이거나 3000 이상 4999 이하여야 한다.
            if (!(code == 1000 || (code >= 3000 && code <= 4999))) {
                throw api.domError("InvalidAccessError", "The code must be either 1000, or between 3000 and 4999. " + Ops.toString(codeValue)
                        + " is neither.");
            }
        }
        JsValue reasonValue = Js.arg(args, 1);
        if (!reasonValue.isUndefined()) {
            String reason = Ops.toString(reasonValue);
            byte[] bytes;
            try {
                bytes = reason.getBytes("UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                bytes = new byte[0];
            }
            if (bytes.length > 123) {
                throw api.domError("SyntaxError", "The message must not be greater than 123 bytes.");
            }
        }
        // 연결 중에 닫으면 닫는 중 상태가 되고 줄 선 실패 작업이 error 와 close 로 마무리한다.
        if (s.readyState == 0) {
            s.readyState = 2;
        }
        return Js.UNDEFINED;
    }
}
