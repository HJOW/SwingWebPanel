package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The URL utilities of the hyperlink elements (HTML5 2014 REC section 4.6.3, the interface <code>HTMLHyperlinkElementUtils</code>) for
 * <code>a</code> and <code>area</code>: <code>origin</code>, <code>protocol</code>, <code>username</code>, <code>password</code>,
 * <code>host</code>, <code>hostname</code>, <code>port</code>, <code>pathname</code>, <code>search</code>, <code>hash</code> and
 * <code>toString()</code>. The properties read the URL that the <code>href</code> attribute makes against the base URL of the document; when
 * there is no <code>href</code> or it cannot be parsed they are empty strings (and <code>origin</code> is empty), and a write is ignored. A write
 * changes the <code>href</code> attribute to the URL with the new part. Only http-like URLs have the default ports left out
 * (<code>80</code>, <code>443</code>, <code>21</code>).
 * <p>
 * 하이퍼링크 요소의 URL 유틸리티(HTML5 2014 REC 4.6.3절, 인터페이스 <code>HTMLHyperlinkElementUtils</code>)를 <code>a</code> 와 <code>area</code> 에 제공한다:
 * <code>origin</code>, <code>protocol</code>, <code>username</code>, <code>password</code>, <code>host</code>, <code>hostname</code>, <code>port</code>,
 * <code>pathname</code>, <code>search</code>, <code>hash</code>, <code>toString()</code>. 프로퍼티는 <code>href</code> 속성을 문서의 기준 URL 에 대해 만든 URL 을
 * 읽으며, <code>href</code> 가 없거나 해석할 수 없으면 빈 문자열이고(<code>origin</code> 도 빈 문자열) 쓰기는 무시한다. 쓰기는 <code>href</code> 속성을 새 부분이
 * 들어간 URL 로 바꾼다. 기본 포트(<code>80</code>, <code>443</code>, <code>21</code>)만 생략한다.
 */
final class HyperlinkBindings {

    /**
     * The parts that have a property.
     * <p>
     * 프로퍼티가 있는 부분.
     */
    private static final String[] PARTS = { "origin", "protocol", "username", "password", "host", "hostname", "port", "pathname", "search", "hash" };

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private HyperlinkBindings() {
    }

    /**
     * Returns the parsed URL of the <code>href</code> of an element, or null.
     * <p>
     * 요소의 <code>href</code> 가 만드는 해석한 URL 을 반환한다. 없으면 null.
     *
     * @param e the element / 요소
     * @return the parts or null / 부분 또는 null
     */
    private static UrlUtil.Parts parts(Element e) {
        String href = e.getAttribute("href");
        if (href == null) {
            return null;
        }
        try {
            String base = e.document().getBaseUri();
            String abs = base == null || base.length() == 0 || "about:blank".equals(base) || "about:text".equals(base) ? href : UrlUtil.resolve(base, href);
            UrlUtil.Parts p = UrlUtil.parse(abs);
            return p.scheme == null || p.scheme.length() == 0 ? null : p;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /**
     * Returns the port text, empty for the default port.
     * <p>
     * 포트 문자열을 반환한다. 기본 포트이면 빈 문자열.
     *
     * @param p the URL / URL
     * @return the text / 문자열
     */
    private static String portText(UrlUtil.Parts p) {
        return p.port < 0 || p.port == UrlUtil.defaultPort(p.scheme) ? "" : Integer.toString(p.port);
    }

    /**
     * Returns the value of a part.
     * <p>
     * 부분의 값을 반환한다.
     *
     * @param p    the URL / URL
     * @param part the name of the part / 부분의 이름
     * @return the value / 값
     */
    private static String read(UrlUtil.Parts p, String part) {
        String user = p.userInfo == null ? "" : p.userInfo;
        int colon = user.indexOf(':');
        if ("protocol".equals(part)) {
            return p.scheme + ":";
        } else if ("username".equals(part)) {
            return colon < 0 ? user : user.substring(0, colon);
        } else if ("password".equals(part)) {
            return colon < 0 ? "" : user.substring(colon + 1);
        } else if ("hostname".equals(part)) {
            return p.host == null ? "" : p.host;
        } else if ("port".equals(part)) {
            return portText(p);
        } else if ("host".equals(part)) {
            String port = portText(p);
            return (p.host == null ? "" : p.host) + (port.length() == 0 ? "" : ":" + port);
        } else if ("pathname".equals(part)) {
            return p.path == null || p.path.length() == 0 ? (p.hasAuthority ? "/" : "") : p.path;
        } else if ("search".equals(part)) {
            return p.query == null || p.query.length() == 0 ? "" : "?" + p.query;
        } else if ("hash".equals(part)) {
            return p.fragment == null || p.fragment.length() == 0 ? "" : "#" + p.fragment;
        }
        String s = p.scheme.toLowerCase();
        boolean web = "http".equals(s) || "https".equals(s) || "ftp".equals(s);
        String port = portText(p);
        return web ? s + "://" + p.host + (port.length() == 0 ? "" : ":" + port) : "null";
    }

    /**
     * Returns the URL with a part replaced, or the same parts if the value cannot be used.
     * <p>
     * 부분을 바꾼 URL 을 반환한다. 값을 쓸 수 없으면 같은 부분을 그대로 반환한다.
     *
     * @param p     the URL / URL
     * @param part  the name of the part / 부분의 이름
     * @param value the new value / 새 값
     * @return the new URL / 새 URL
     */
    private static UrlUtil.Parts write(UrlUtil.Parts p, String part, String value) {
        String scheme = p.scheme;
        String userInfo = p.userInfo;
        String host = p.host;
        int port = p.port;
        String path = p.path;
        String query = p.query;
        String fragment = p.fragment;
        String user = userInfo == null ? "" : userInfo;
        int colon = user.indexOf(':');
        String name = colon < 0 ? user : user.substring(0, colon);
        String pass = colon < 0 ? "" : user.substring(colon + 1);
        if ("hash".equals(part)) {
            fragment = value.startsWith("#") ? value.substring(1) : value;
            if (fragment.length() == 0) {
                fragment = null;
            }
        } else if ("search".equals(part)) {
            query = value.startsWith("?") ? value.substring(1) : value;
            if (query.length() == 0) {
                query = null;
            }
        } else if ("pathname".equals(part)) {
            path = value.length() == 0 || value.charAt(0) == '/' ? value : "/" + value;
        } else if ("hostname".equals(part)) {
            host = value;
        } else if ("host".equals(part)) {
            int c = value.lastIndexOf(':');
            if (c >= 0 && value.indexOf(']') < c) {
                host = value.substring(0, c);
                port = parsePort(value.substring(c + 1), port);
            } else {
                host = value;
            }
        } else if ("port".equals(part)) {
            port = value.length() == 0 ? -1 : parsePort(value, port);
        } else if ("protocol".equals(part)) {
            String s = value.endsWith(":") ? value.substring(0, value.length() - 1) : value;
            if (s.length() > 0) {
                scheme = s.toLowerCase();
            }
        } else if ("username".equals(part)) {
            name = value;
            userInfo = name.length() == 0 && pass.length() == 0 ? null : (pass.length() == 0 ? name : name + ":" + pass);
        } else if ("password".equals(part)) {
            pass = value;
            userInfo = name.length() == 0 && pass.length() == 0 ? null : (pass.length() == 0 ? name : name + ":" + pass);
        }
        return new UrlUtil.Parts(scheme, p.hasAuthority, userInfo, host, port, path, query, fragment);
    }

    /**
     * Reads the leading digits of a port text (the old port if there are none).
     * <p>
     * 포트 문자열의 앞쪽 숫자를 읽는다(없으면 이전 포트).
     *
     * @param text the text / 문자열
     * @param old  the old port / 이전 포트
     * @return the port / 포트
     */
    private static int parsePort(String text, int old) {
        int end = 0;
        // 앞쪽의 숫자 문자만 센다.
        while (end < text.length() && text.charAt(end) >= '0' && text.charAt(end) <= '9') {
            end++;
        }
        if (end == 0 || end > 5) {
            return old;
        }
        int n = Integer.parseInt(text.substring(0, end));
        return n > 65535 ? old : n;
    }

    /**
     * Installs the properties and <code>toString()</code> on <code>HTMLAnchorElement</code> and <code>HTMLAreaElement</code> (call after
     * both interfaces exist).
     * <p>
     * <code>HTMLAnchorElement</code> 와 <code>HTMLAreaElement</code> 에 프로퍼티와 <code>toString()</code> 을 설치한다(두 인터페이스가 생긴 뒤에 호출한다).
     *
     * @param bridge the bridge / 브리지
     */
    static void install(DomBridge bridge) {
        String[] interfaces = { "HTMLAnchorElement", "HTMLAreaElement" };
        // 두 인터페이스의 프로토타입에 같은 멤버를 설치한다.
        for (int i = 0; i < interfaces.length; i++) {
            JsObject proto = bridge.getInterface(interfaces[i]).getPrototype();
            // 부분마다 읽기와 쓰기 접근자를 정의한다.
            for (int k = 0; k < PARTS.length; k++) {
                defineAccessor(bridge, proto, PARTS[k]);
            }
            proto.defineBuiltin("toString", bridge.getRealm().newFunction("toString", 0, new NativeFunction.Call() {
                /**
                 * Returns the <code>href</code> property.
                 * <p>
                 * <code>href</code> 프로퍼티를 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the URL text / URL 문자열
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    return ((JsObject) self).get("href");
                }
            }));
        }
    }

    /**
     * Defines the accessor of one part.
     * <p>
     * 부분 하나의 접근자를 정의한다.
     *
     * @param bridge the bridge / 브리지
     * @param proto  the prototype / 프로토타입
     * @param part   the name of the part / 부분의 이름
     */
    private static void defineAccessor(final DomBridge bridge, JsObject proto, final String part) {
        JsRealm realm = bridge.getRealm();
        JsObject get = realm.newFunction("get " + part, 0, new NativeFunction.Call() {
            /**
             * Reads the part of the URL.
             * <p>
             * URL 의 부분을 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the value / 값
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                UrlUtil.Parts p = parts(receiver(self));
                return Js.str(p == null ? "" : read(p, part));
            }
        });
        JsObject set = "origin".equals(part) ? null : realm.newFunction("set " + part, 1, new NativeFunction.Call() {
            /**
             * Writes the part of the URL into the <code>href</code> attribute.
             * <p>
             * URL 의 부분을 <code>href</code> 속성에 쓴다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Element e = receiver(self);
                UrlUtil.Parts p = parts(e);
                if (p != null) {
                    e.setAttribute("href", UrlUtil.toString(write(p, part, Ops.toString(Js.arg(args, 0)))));
                }
                return Js.UNDEFINED;
            }
        });
        proto.defineAccessor(part, get, set, true, true);
    }

    /**
     * Returns the receiver as an element or throws the standard TypeError.
     * <p>
     * 수신자를 요소로 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element receiver(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }
}
