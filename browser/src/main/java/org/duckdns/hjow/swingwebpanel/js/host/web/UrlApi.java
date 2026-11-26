package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.net.IDN;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * Installs <code>URL</code> (URL Standard, section 6): the parsed form of an absolute URL with the usual component properties and setters
 * (<code>href</code>, <code>origin</code>, <code>protocol</code>, <code>username</code>, <code>password</code>, <code>host</code>, <code>hostname</code>,
 * <code>port</code>, <code>pathname</code>, <code>search</code>, <code>searchParams</code>, <code>hash</code>), <code>toString()</code>, <code>toJSON()</code>,
 * <code>URL.canParse()</code> and <code>URL.parse()</code>. The parser is built on the component's own {@link UrlUtil} (RFC 3986 with the
 * normalizations of that class), plus the URL Standard's handling of the special schemes: backslashes count as slashes, a host is
 * required, an IDN host becomes ASCII (Punycode), and the default port disappears. The remaining differences to the full URL Standard
 * algorithm (for example exotic IPv4 notations such as <code>0x7f.1</code>, and the full percent-encode sets of each component) are not
 * implemented. <code>URL.createObjectURL</code> and <code>URL.revokeObjectURL</code> use the File API store of the owning component.
 * <p>
 * <code>URL</code> 을 설치한다(URL 표준 6절): 절대 URL의 해석된 형태이며 일반적인 구성요소 프로퍼티와 setter(<code>href</code>, <code>origin</code>,
 * <code>protocol</code>, <code>username</code>, <code>password</code>, <code>host</code>, <code>hostname</code>, <code>port</code>, <code>pathname</code>,
 * <code>search</code>, <code>searchParams</code>, <code>hash</code>), <code>toString()</code>, <code>toJSON()</code>, <code>URL.canParse()</code>,
 * <code>URL.parse()</code> 를 가진다. 해석기는 컴포넌트 자신의 {@link UrlUtil}(RFC 3986과 그 클래스의 정규화) 위에 세우고 URL 표준의 특수 스킴 처리를 더했다:
 * 역슬래시는 슬래시로 보고, 호스트가 필요하며, IDN 호스트는 ASCII(Punycode)가 되고, 기본 포트는 사라진다. 전체 URL 표준 알고리즘과의 나머지 차이(예:
 * <code>0x7f.1</code> 같은 특이한 IPv4 표기, 구성요소별 전체 퍼센트 인코딩 집합)는 구현하지 않았다. <code>URL.createObjectURL</code>과
 * <code>URL.revokeObjectURL</code>은 소유 컴포넌트의 File API 저장소를 사용한다.
 */
final class UrlApi {

    /**
     * The script object of a <code>URL</code>: it keeps the normalized parts of the address.
     * <p>
     * <code>URL</code> 의 스크립트 객체. 주소의 정규화된 구성요소를 가진다.
     */
    static final class UrlObject extends JsObject {
        /** The normalized parts. / 정규화된 구성요소. */
        UrlUtil.Parts parts;

        /** The linked <code>searchParams</code> object. / 연결된 <code>searchParams</code> 객체. */
        SearchParamsApi.ParamsObject params;

        /**
         * Creates an object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        UrlObject(JsObject proto) {
            super(proto);
        }

        /**
         * Sets the query from the <code>searchParams</code> list without rebuilding that list.
         * <p>
         * <code>searchParams</code> 목록을 다시 만들지 않고 쿼리를 지정한다.
         *
         * @param query the serialized query or null / 직렬화한 쿼리 또는 null
         */
        void setQueryFromParams(String query) {
            UrlUtil.Parts p = parts;
            parts = renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, p.port, p.path, query, p.fragment));
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The prototype. / 프로토타입. */
    private JsObject proto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    UrlApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs <code>URL</code>.
     * <p>
     * <code>URL</code> 을 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        JsRealm realm = api.realm();
        Natives.ClassDef c = Natives.defineClass(realm, g, "URL", 1, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Natives.requireArgs(args, 1, "constructor", "URL");
                UrlObject u = parseObject(Natives.usv(args[0]), args.length > 1 && !args[1].isUndefined() ? Natives.usv(args[1]) : null,
                        Ops.getPrototypeFromConstructor(r, newTarget, proto), true);
                return u;
            }
        });
        proto = c.prototype;
        Natives.accessor(realm, proto, "href", this, "getHref", "setHref");
        Natives.getter(realm, proto, "origin", this, "getOrigin");
        Natives.accessor(realm, proto, "protocol", this, "getProtocol", "setProtocol");
        Natives.accessor(realm, proto, "username", this, "getUsername", "setUsername");
        Natives.accessor(realm, proto, "password", this, "getPassword", "setPassword");
        Natives.accessor(realm, proto, "host", this, "getHost", "setHost");
        Natives.accessor(realm, proto, "hostname", this, "getHostname", "setHostname");
        Natives.accessor(realm, proto, "port", this, "getPort", "setPort");
        Natives.accessor(realm, proto, "pathname", this, "getPathname", "setPathname");
        Natives.accessor(realm, proto, "search", this, "getSearch", "setSearch");
        Natives.getter(realm, proto, "searchParams", this, "getSearchParams");
        Natives.accessor(realm, proto, "hash", this, "getHash", "setHash");
        Natives.method(realm, proto, "toString", 0, this, "toHref");
        Natives.method(realm, proto, "toJSON", 0, this, "toHref");
        Natives.method(realm, c.constructor, "canParse", 1, this, "canParse");
        Natives.method(realm, c.constructor, "parse", 1, this, "parseStatic");
        Natives.method(realm, c.constructor, "createObjectURL", 1, this, "createObjectUrl");
        Natives.method(realm, c.constructor, "revokeObjectURL", 1, this, "revokeObjectUrl");
    }

    /**
     * Registers a Blob or File in the component's object URL store.
     * <p>
     * Blob이나 File을 컴포넌트의 객체 URL 저장소에 등록한다.
     *
     * @param self the constructor receiver / 생성자 수신 값
     * @param args the Blob to register / 등록할 Blob
     * @return the generated URL / 생성한 URL
     */
    JsValue createObjectUrl(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "createObjectURL", "URL");
        BlobApi.BlobObject blob = Natives.self(args[0], BlobApi.BlobObject.class);
        return Js.str(api.blobUrls.create(blob.data, blob.type));
    }

    /**
     * Revokes an object URL in the requesting origin's partition.
     * <p>
     * 요청 오리진의 파티션에서 객체 URL을 해제한다.
     *
     * @param self the constructor receiver / 생성자 수신 값
     * @param args the object URL / 객체 URL
     * @return undefined / undefined
     */
    JsValue revokeObjectUrl(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "revokeObjectURL", "URL");
        api.blobUrls.revoke(Natives.usv(args[0]));
        return Js.UNDEFINED;
    }

    /**
     * Tells whether a scheme is one of the special schemes of the URL Standard.
     * <p>
     * 스킴이 URL 표준의 특수 스킴인지 알려 준다.
     *
     * @param scheme the lower-case scheme / 소문자 스킴
     * @return true for http, https, ftp, ws, wss and file / http, https, ftp, ws, wss, file 이면 true
     */
    static boolean special(String scheme) {
        return "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme) || "ws".equals(scheme) || "wss".equals(scheme)
                || "file".equals(scheme);
    }

    /**
     * Parses a URL text, optionally against a base; returns null if the text is not a valid URL.
     * <p>
     * URL 문자열을 (선택적으로 기준 URL에 대해) 해석한다. 올바른 URL이 아니면 null 을 반환한다.
     *
     * @param input the text / 문자열
     * @param base  the base URL text or null / 기준 URL 문자열 또는 null
     * @return the normalized parts, or null / 정규화된 구성요소. 실패하면 null
     */
    static UrlUtil.Parts parse(String input, String base) {
        String text = strip(input);
        String baseText = base == null ? null : strip(base);
        try {
            String abs;
            String scheme = UrlUtil.getScheme(text);
            if (scheme == null) {
                if (baseText == null) {
                    return null;
                }
                UrlUtil.Parts b = parse(baseText, null);
                if (b == null) {
                    return null;
                }
                // 특수 스킴의 기준 URL 에서는 역슬래시를 슬래시로 본다.
                abs = UrlUtil.resolve(UrlUtil.toString(b), special(b.scheme) ? text.replace('\\', '/') : text);
            } else {
                abs = UrlUtil.normalize(special(scheme.toLowerCase()) ? backslashes(text) : text);
            }
            return check(UrlUtil.parse(abs));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Removes the leading and trailing C0 control or space characters and every tab and newline, as the URL parser does first.
     * <p>
     * URL 해석기가 먼저 하듯이 앞뒤의 C0 제어 문자와 공백, 그리고 모든 탭과 줄바꿈을 제거한다.
     *
     * @param s the text / 문자열
     * @return the cleaned text / 정리한 문자열
     */
    private static String strip(String s) {
        int a = 0;
        int b = s.length();
        // 앞뒤에서 U+0020 이하의 문자를 걷어 낸다.
        while (a < b && s.charAt(a) <= ' ') {
            a++;
        }
        // 뒤쪽도 같은 방식으로 걷어 낸다.
        while (b > a && s.charAt(b - 1) <= ' ') {
            b--;
        }
        StringBuilder out = new StringBuilder(b - a);
        // 가운데의 탭, CR, LF 는 모두 버린다.
        for (int i = a; i < b; i++) {
            char c = s.charAt(i);
            if (c != '\t' && c != '\r' && c != '\n') {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Turns the backslashes before the query or fragment into slashes (special schemes).
     * <p>
     * 쿼리나 프래그먼트 앞의 역슬래시를 슬래시로 바꾼다(특수 스킴).
     *
     * @param s the text / 문자열
     * @return the converted text / 변환한 문자열
     */
    private static String backslashes(String s) {
        int end = s.length();
        int q = s.indexOf('?');
        int h = s.indexOf('#');
        if (q >= 0) {
            end = Math.min(end, q);
        }
        if (h >= 0) {
            end = Math.min(end, h);
        }
        return s.substring(0, end).replace('\\', '/') + s.substring(end);
    }

    /**
     * Applies the host rules of the URL Standard to parsed parts: special schemes need a host, an IDN host is converted to ASCII.
     * <p>
     * 해석된 구성요소에 URL 표준의 호스트 규칙을 적용한다: 특수 스킴은 호스트가 필요하고 IDN 호스트는 ASCII 로 바꾼다.
     *
     * @param p the parts / 구성요소
     * @return the checked parts, or null if invalid / 검사한 구성요소. 올바르지 않으면 null
     */
    private static UrlUtil.Parts check(UrlUtil.Parts p) {
        if (p.scheme == null) {
            return null;
        }
        if (special(p.scheme) && !"file".equals(p.scheme) && (p.host == null || p.host.length() == 0)) {
            return null;
        }
        // UrlUtil 은 ASCII 가 아닌 호스트 문자를 이미 %XX 로 바꿔 두었으므로 먼저 풀어서 IDN 변환 대상인지 본다.
        String decoded = p.host == null ? null : UrlUtil.percentDecode(p.host);
        if (decoded != null && !isAscii(decoded)) {
            try {
                String ascii = IDN.toASCII(decoded, IDN.ALLOW_UNASSIGNED).toLowerCase();
                return renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, ascii, p.port, p.path, p.query, p.fragment));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return p;
    }

    /**
     * Tells whether a string has ASCII characters only.
     * <p>
     * 문자열이 ASCII 문자로만 되어 있는지 알려 준다.
     *
     * @param s the string / 문자열
     * @return true if ASCII only / ASCII 뿐이면 true
     */
    private static boolean isAscii(String s) {
        // 128 이상의 문자가 있으면 ASCII 가 아니다.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    /**
     * Normalizes modified parts again (default port, dot segments, case).
     * <p>
     * 바뀐 구성요소를 다시 정규화한다(기본 포트, 점 세그먼트, 대소문자).
     *
     * @param p the parts / 구성요소
     * @return the normalized parts / 정규화한 구성요소
     */
    static UrlUtil.Parts renormalize(UrlUtil.Parts p) {
        return UrlUtil.parse(UrlUtil.normalize(UrlUtil.toString(p)));
    }

    /**
     * Parses into a script object.
     * <p>
     * 해석하여 스크립트 객체를 만든다.
     *
     * @param input the text / 문자열
     * @param base  the base or null / 기준 또는 null
     * @param proto the prototype / 프로토타입
     * @param throwOnFailure true to throw a TypeError on failure / 실패하면 TypeError 를 던지려면 true
     * @return the object, or null on failure when not throwing / 객체. 던지지 않는 경우 실패하면 null
     */
    private UrlObject parseObject(String input, String base, JsObject proto, boolean throwOnFailure) {
        if (base != null && parse(base, null) == null) {
            if (throwOnFailure) {
                throw JsException.typeError("Failed to construct 'URL': Invalid base URL");
            }
            return null;
        }
        UrlUtil.Parts p = parse(input, base);
        if (p == null) {
            if (throwOnFailure) {
                throw JsException.typeError("Failed to construct 'URL': Invalid URL");
            }
            return null;
        }
        UrlObject u = new UrlObject(proto);
        u.parts = p;
        u.params = api.params.create(u);
        u.params.reset(p.query);
        return u;
    }

    /**
     * Creates a <code>URL</code> object for a Java caller (<code>null</code> if the text is not a valid URL).
     * <p>
     * Java 호출자를 위해 <code>URL</code> 객체를 만든다(올바른 URL이 아니면 <code>null</code>).
     *
     * @param input the text / 문자열
     * @param base  the base URL or null / 기준 URL 또는 null
     * @return the object or null / 객체 또는 null
     */
    UrlObject create(String input, String base) {
        return parseObject(input, base, proto, false);
    }

    /**
     * Returns the <code>href</code> of a URL.
     * <p>
     * URL의 <code>href</code> 를 반환한다.
     *
     * @param u the URL / URL
     * @return the serialization / 직렬화 문자열
     */
    private static String href(UrlObject u) {
        return UrlUtil.toString(u.parts);
    }

    /**
     * Returns <code>url.href</code>.
     * <p>
     * <code>url.href</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the href / href
     */
    JsValue getHref(JsValue self, JsValue[] args) {
        return Js.str(href(Natives.self(self, UrlObject.class)));
    }

    /**
     * Implements <code>url.toString()</code> and <code>url.toJSON()</code>.
     * <p>
     * <code>url.toString()</code> 과 <code>url.toJSON()</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the href / href
     */
    JsValue toHref(JsValue self, JsValue[] args) {
        return getHref(self, args);
    }

    /**
     * Sets <code>url.href</code>.
     * <p>
     * <code>url.href</code> 를 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setHref(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        UrlUtil.Parts p = parse(Natives.usv(Js.arg(args, 0)), null);
        if (p == null) {
            throw JsException.typeError("Failed to set the 'href' property on 'URL': Invalid URL");
        }
        u.parts = p;
        u.params.reset(p.query);
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>url.origin</code>.
     * <p>
     * <code>url.origin</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the origin text / 오리진 문자열
     */
    JsValue getOrigin(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        String s = u.parts.scheme;
        if ("blob".equals(s) || "http".equals(s) || "https".equals(s) || "ftp".equals(s) || "ws".equals(s) || "wss".equals(s)) {
            return Js.str(Origin.of(href(u)).serialize());
        }
        return Js.str("null");
    }

    /**
     * Returns <code>url.protocol</code>.
     * <p>
     * <code>url.protocol</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the scheme with a colon / 콜론이 붙은 스킴
     */
    JsValue getProtocol(JsValue self, JsValue[] args) {
        return Js.str(Natives.self(self, UrlObject.class).parts.scheme + ":");
    }

    /**
     * Sets <code>url.protocol</code>: a new scheme must keep the kind (special or not) of the old one.
     * <p>
     * <code>url.protocol</code> 을 지정한다. 새 스킴은 옛 스킴의 종류(특수 여부)를 유지해야 한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setProtocol(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        String v = Natives.usv(Js.arg(args, 0));
        int colon = v.indexOf(':');
        String scheme = (colon < 0 ? v : v.substring(0, colon)).toLowerCase();
        if (scheme.length() == 0 || !Character.isLetter(scheme.charAt(0)) || special(scheme) != special(u.parts.scheme)) {
            return Js.UNDEFINED;
        }
        // 스킴은 영문자로 시작하고 영문자, 숫자, + - . 만 쓸 수 있다.
        for (int i = 0; i < scheme.length(); i++) {
            char c = scheme.charAt(i);
            if (!(c < 128 && (Character.isLetterOrDigit(c) || c == '+' || c == '-' || c == '.'))) {
                return Js.UNDEFINED;
            }
        }
        UrlUtil.Parts p = u.parts;
        u.parts = renormalize(new UrlUtil.Parts(scheme, p.hasAuthority, p.userInfo, p.host, p.port, p.path, p.query, p.fragment));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>url.username</code>.
     * <p>
     * <code>url.username</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the user name / 사용자 이름
     */
    JsValue getUsername(JsValue self, JsValue[] args) {
        String ui = Natives.self(self, UrlObject.class).parts.userInfo;
        if (ui == null) {
            return Js.str("");
        }
        int c = ui.indexOf(':');
        return Js.str(c < 0 ? ui : ui.substring(0, c));
    }

    /**
     * Returns <code>url.password</code>.
     * <p>
     * <code>url.password</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the password / 비밀번호
     */
    JsValue getPassword(JsValue self, JsValue[] args) {
        String ui = Natives.self(self, UrlObject.class).parts.userInfo;
        int c = ui == null ? -1 : ui.indexOf(':');
        return Js.str(c < 0 ? "" : ui.substring(c + 1));
    }

    /**
     * Sets the user info of a URL.
     * <p>
     * URL의 사용자 정보를 지정한다.
     *
     * @param u        the URL / URL
     * @param username the new user name or null to keep / 새 사용자 이름. 유지하려면 null
     * @param password the new password or null to keep / 새 비밀번호. 유지하려면 null
     */
    private static void setUserInfo(UrlObject u, String username, String password) {
        UrlUtil.Parts p = u.parts;
        if (p.host == null || p.host.length() == 0 || "file".equals(p.scheme)) {
            return;
        }
        String ui = p.userInfo == null ? "" : p.userInfo;
        int c = ui.indexOf(':');
        String user = c < 0 ? ui : ui.substring(0, c);
        String pass = c < 0 ? "" : ui.substring(c + 1);
        if (username != null) {
            user = encodeUserInfo(username);
        }
        if (password != null) {
            pass = encodeUserInfo(password);
        }
        String merged = user.length() == 0 && pass.length() == 0 ? null : (pass.length() == 0 ? user : user + ":" + pass);
        u.parts = renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, merged, p.host, p.port, p.path, p.query, p.fragment));
    }

    /**
     * Percent-encodes the characters that cannot appear in user information.
     * <p>
     * 사용자 정보에 올 수 없는 문자를 퍼센트 인코딩한다.
     *
     * @param s the text / 문자열
     * @return the encoded text / 인코딩한 문자열
     */
    private static String encodeUserInfo(String s) {
        StringBuilder out = new StringBuilder();
        // 구분자 문자는 직접 인코딩하고 나머지는 UrlUtil 이 인코딩하게 한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (":@/?#[]\\ ".indexOf(c) >= 0) {
                out.append('%').append(Character.toUpperCase(Character.forDigit(c >> 4, 16))).append(Character.toUpperCase(Character.forDigit(c & 15, 16)));
            } else {
                out.append(c);
            }
        }
        return UrlUtil.encodeIllegal(out.toString());
    }

    /**
     * Sets <code>url.username</code>.
     * <p>
     * <code>url.username</code> 을 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setUsername(JsValue self, JsValue[] args) {
        setUserInfo(Natives.self(self, UrlObject.class), Natives.usv(Js.arg(args, 0)), null);
        return Js.UNDEFINED;
    }

    /**
     * Sets <code>url.password</code>.
     * <p>
     * <code>url.password</code> 를 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setPassword(JsValue self, JsValue[] args) {
        setUserInfo(Natives.self(self, UrlObject.class), null, Natives.usv(Js.arg(args, 0)));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>url.host</code>.
     * <p>
     * <code>url.host</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the host and the explicit port / 호스트와 명시한 포트
     */
    JsValue getHost(JsValue self, JsValue[] args) {
        UrlUtil.Parts p = Natives.self(self, UrlObject.class).parts;
        if (p.host == null) {
            return Js.str("");
        }
        return Js.str(p.port >= 0 ? p.host + ":" + p.port : p.host);
    }

    /**
     * Returns <code>url.hostname</code>.
     * <p>
     * <code>url.hostname</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the host / 호스트
     */
    JsValue getHostname(JsValue self, JsValue[] args) {
        String h = Natives.self(self, UrlObject.class).parts.host;
        return Js.str(h == null ? "" : h);
    }

    /**
     * Returns <code>url.port</code>.
     * <p>
     * <code>url.port</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the explicit port or "" / 명시한 포트 또는 ""
     */
    JsValue getPort(JsValue self, JsValue[] args) {
        int port = Natives.self(self, UrlObject.class).parts.port;
        return Js.str(port < 0 ? "" : String.valueOf(port));
    }

    /**
     * Sets host and port from text: <code>host</code> or <code>host:port</code> (the port part only when allowed).
     * <p>
     * 문자열로 호스트와 포트를 지정한다: <code>host</code> 또는 <code>host:port</code>(포트 부분은 허용될 때만).
     *
     * @param u        the URL / URL
     * @param text     the new value / 새 값
     * @param withPort true if a port part is accepted / 포트 부분을 받으면 true
     */
    private static void setHostText(UrlObject u, String text, boolean withPort) {
        UrlUtil.Parts p = u.parts;
        if (!p.hasAuthority || "file".equals(p.scheme)) {
            return;
        }
        String s = text;
        // 경로, 쿼리, 프래그먼트가 시작되는 곳에서 자른다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || c == '?' || c == '#' || c == '\\') {
                s = s.substring(0, i);
                break;
            }
        }
        String host = s;
        int port = p.port;
        int colon = s.startsWith("[") ? s.indexOf(':', Math.max(s.indexOf(']'), 0)) : s.lastIndexOf(':');
        if (colon >= 0) {
            host = s.substring(0, colon);
            String ps = s.substring(colon + 1);
            if (withPort) {
                int digits = 0;
                // 앞쪽의 숫자만 포트로 읽는다.
                while (digits < ps.length() && Character.isDigit(ps.charAt(digits)) && ps.charAt(digits) < 128) {
                    digits++;
                }
                if (digits > 0) {
                    try {
                        int v = Integer.parseInt(ps.substring(0, Math.min(digits, 6)));
                        if (v <= 65535) {
                            port = v;
                        }
                    } catch (NumberFormatException e) {
                        port = p.port;
                    }
                }
            } else {
                return;
            }
        }
        if (host.length() == 0 && special(p.scheme)) {
            return;
        }
        try {
            UrlUtil.Parts q = check(renormalize(new UrlUtil.Parts(p.scheme, true, p.userInfo, host.toLowerCase(), port, p.path, p.query, p.fragment)));
            if (q != null) {
                u.parts = q;
            }
        } catch (IllegalArgumentException e) {
            // 호스트가 올바르지 않으면 값을 바꾸지 않는다.
        }
    }

    /**
     * Sets <code>url.host</code>.
     * <p>
     * <code>url.host</code> 를 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setHost(JsValue self, JsValue[] args) {
        setHostText(Natives.self(self, UrlObject.class), Natives.usv(Js.arg(args, 0)), true);
        return Js.UNDEFINED;
    }

    /**
     * Sets <code>url.hostname</code>.
     * <p>
     * <code>url.hostname</code> 을 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setHostname(JsValue self, JsValue[] args) {
        setHostText(Natives.self(self, UrlObject.class), Natives.usv(Js.arg(args, 0)), false);
        return Js.UNDEFINED;
    }

    /**
     * Sets <code>url.port</code>: digits only (up to 65535); the empty string removes the port.
     * <p>
     * <code>url.port</code> 를 지정한다. 숫자만(65535 까지) 가능하며 빈 문자열은 포트를 지운다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setPort(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        UrlUtil.Parts p = u.parts;
        if (p.host == null || p.host.length() == 0 || "file".equals(p.scheme)) {
            return Js.UNDEFINED;
        }
        String v = Natives.usv(Js.arg(args, 0));
        int port = -1;
        if (v.length() > 0) {
            int digits = 0;
            // 앞쪽의 숫자만 읽는다. 숫자로 시작하지 않으면 값을 바꾸지 않는다.
            while (digits < v.length() && v.charAt(digits) >= '0' && v.charAt(digits) <= '9') {
                digits++;
            }
            if (digits == 0) {
                return Js.UNDEFINED;
            }
            try {
                port = Integer.parseInt(v.substring(0, Math.min(digits, 6)));
            } catch (NumberFormatException e) {
                return Js.UNDEFINED;
            }
            if (port > 65535) {
                return Js.UNDEFINED;
            }
        }
        u.parts = renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, port, p.path, p.query, p.fragment));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>url.pathname</code>.
     * <p>
     * <code>url.pathname</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the path / 경로
     */
    JsValue getPathname(JsValue self, JsValue[] args) {
        UrlUtil.Parts p = Natives.self(self, UrlObject.class).parts;
        return Js.str(p.path);
    }

    /**
     * Sets <code>url.pathname</code>.
     * <p>
     * <code>url.pathname</code> 을 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setPathname(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        UrlUtil.Parts p = u.parts;
        if (!p.hasAuthority && !UrlUtil.SCHEME_CLASSPATH.equals(p.scheme)) {
            return Js.UNDEFINED;
        }
        String v = Natives.usv(Js.arg(args, 0)).replace("?", "%3F").replace("#", "%23");
        if (special(p.scheme)) {
            v = v.replace('\\', '/');
        }
        if (!v.startsWith("/")) {
            v = "/" + v;
        }
        u.parts = renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, p.port, v, p.query, p.fragment));
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>url.search</code>.
     * <p>
     * <code>url.search</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the query with '?' or "" / '?' 가 붙은 쿼리 또는 ""
     */
    JsValue getSearch(JsValue self, JsValue[] args) {
        String q = Natives.self(self, UrlObject.class).parts.query;
        return Js.str(q == null || q.length() == 0 ? "" : "?" + q);
    }

    /**
     * Sets <code>url.search</code> and rebuilds the <code>searchParams</code> list.
     * <p>
     * <code>url.search</code> 를 지정하고 <code>searchParams</code> 목록을 다시 만든다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setSearch(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        String v = Natives.usv(Js.arg(args, 0));
        UrlUtil.Parts p = u.parts;
        String q = null;
        if (v.length() > 0) {
            q = v.startsWith("?") ? v.substring(1) : v;
            q = q.replace("#", "%23");
        }
        u.parts = renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, p.port, p.path, q, p.fragment));
        u.params.reset(u.parts.query);
        return Js.UNDEFINED;
    }

    /**
     * Returns <code>url.searchParams</code>.
     * <p>
     * <code>url.searchParams</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the linked list object / 연결된 목록 객체
     */
    JsValue getSearchParams(JsValue self, JsValue[] args) {
        return Natives.self(self, UrlObject.class).params;
    }

    /**
     * Returns <code>url.hash</code>.
     * <p>
     * <code>url.hash</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the fragment with '#' or "" / '#' 가 붙은 프래그먼트 또는 ""
     */
    JsValue getHash(JsValue self, JsValue[] args) {
        String f = Natives.self(self, UrlObject.class).parts.fragment;
        return Js.str(f == null || f.length() == 0 ? "" : "#" + f);
    }

    /**
     * Sets <code>url.hash</code>.
     * <p>
     * <code>url.hash</code> 를 지정한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue setHash(JsValue self, JsValue[] args) {
        UrlObject u = Natives.self(self, UrlObject.class);
        String v = Natives.usv(Js.arg(args, 0));
        UrlUtil.Parts p = u.parts;
        String f = null;
        if (v.length() > 0) {
            f = v.startsWith("#") ? v.substring(1) : v;
        }
        u.parts = renormalize(new UrlUtil.Parts(p.scheme, p.hasAuthority, p.userInfo, p.host, p.port, p.path, p.query, f));
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>URL.canParse(url, base)</code>.
     * <p>
     * <code>URL.canParse(url, base)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return whether the text is a valid URL / 올바른 URL인지
     */
    JsValue canParse(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "canParse", "URL");
        String base = args.length > 1 && !args[1].isUndefined() ? Natives.usv(args[1]) : null;
        return Js.bool(parse(Natives.usv(args[0]), base) != null && (base == null || parse(base, null) != null));
    }

    /**
     * Implements <code>URL.parse(url, base)</code>: a URL object or null.
     * <p>
     * <code>URL.parse(url, base)</code> 를 구현한다: URL 객체 또는 null.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the object or null / 객체 또는 null
     */
    JsValue parseStatic(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "parse", "URL");
        String base = args.length > 1 && !args[1].isUndefined() ? Natives.usv(args[1]) : null;
        UrlObject u = parseObject(Natives.usv(args[0]), base, proto, false);
        return u == null ? Js.NULL : u;
    }
}
