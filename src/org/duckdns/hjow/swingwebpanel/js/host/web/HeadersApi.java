package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * Installs <code>Headers</code> (Fetch Standard, section 5.1): an ordered list of (name, value) pairs with case-insensitive names. The
 * header list has a <em>guard</em> that decides what a script may change: <code>none</code> (a free list), <code>request</code> (forbidden
 * request headers are silently ignored), <code>request-no-cors</code> (only the no-CORS safelist is accepted), <code>response</code> (forbidden
 * response-header names are ignored) and <code>immutable</code> (every change throws a <code>TypeError</code>; the headers of a response that
 * <code>fetch()</code> returns). Names and values are validated as <code>ByteString</code>s. Reading combines the values of one name with
 * <code>", "</code>, except <code>Set-Cookie</code>, which <code>getSetCookie()</code> lists. Iteration is sorted by name.
 * <p>
 * <code>Headers</code> 를 설치한다(Fetch 표준 5.1절): 이름이 대소문자를 구분하지 않는 (이름, 값) 쌍의 순서 있는 목록이다. 헤더 목록에는 스크립트가 바꿀 수 있는
 * 것을 정하는 <em>guard</em> 가 있다: <code>none</code>(자유로운 목록), <code>request</code>(금지된 요청 헤더는 조용히 무시), <code>request-no-cors</code>(no-CORS
 * 안전 목록만 허용), <code>response</code>(금지된 응답 헤더 이름은 무시), <code>immutable</code>(모든 변경이 <code>TypeError</code> 를 던진다. <code>fetch()</code> 가
 * 반환하는 응답의 헤더). 이름과 값은 <code>ByteString</code> 으로 검증한다. 읽을 때 같은 이름의 값은 <code>", "</code> 로 합치며 <code>Set-Cookie</code> 만
 * <code>getSetCookie()</code> 가 따로 나열한다. 반복은 이름순이다.
 */
final class HeadersApi {

    /**
     * The script object of a <code>Headers</code>.
     * <p>
     * <code>Headers</code> 의 스크립트 객체.
     */
    static final class HeadersObject extends JsObject {
        /** The list of {lower-case name, value}. / {소문자 이름, 값} 의 목록. */
        final List<String[]> list = new ArrayList<String[]>();

        /** The guard: none, request, request-no-cors, response or immutable. / 보호 모드: none, request, request-no-cors, response, immutable. */
        String guard = "none";

        /**
         * Creates an empty header list.
         * <p>
         * 빈 헤더 목록을 만든다.
         *
         * @param proto the prototype / 프로토타입
         * @param guard the guard / guard
         */
        HeadersObject(JsObject proto, String guard) {
            super(proto);
            this.guard = guard;
        }

        /**
         * Validates a header for this list's guard (Fetch Standard "validate").
         * <p>
         * 이 목록의 guard 에 대해 헤더를 검증한다(Fetch 표준 "validate").
         *
         * @param name  the name / 이름
         * @param value the normalized value / 정규화한 값
         * @param what  the method name for messages / 메시지용 메서드 이름
         * @return false if the header is silently ignored / 헤더를 조용히 무시하면 false
         */
        boolean validate(String name, String value, String what) {
            if (!HeaderRules.isToken(name)) {
                throw JsException.typeError("Failed to execute '" + what + "' on 'Headers': Invalid name");
            }
            if (!HeaderRules.isValidValue(value)) {
                throw JsException.typeError("Failed to execute '" + what + "' on 'Headers': Invalid value");
            }
            if ("immutable".equals(guard)) {
                throw JsException.typeError("Failed to execute '" + what + "' on 'Headers': Headers are immutable");
            }
            if ("request".equals(guard) && HeaderRules.isForbiddenRequestHeader(name, value)) {
                return false;
            }
            if ("response".equals(guard) && HeaderRules.isForbiddenResponseHeaderName(name)) {
                return false;
            }
            return true;
        }

        /**
         * Appends a header (with the guard rules).
         * <p>
         * 헤더를 덧붙인다(guard 규칙 적용).
         *
         * @param name  the name / 이름
         * @param value the raw value / 원본 값
         */
        void append(String name, String value) {
            String v = HeaderRules.normalizeValue(value);
            if (!validate(name, v, "append")) {
                return;
            }
            if ("request-no-cors".equals(guard)) {
                String old = get(name);
                String combined = old == null ? v : old + ", " + v;
                if (!HeaderRules.isSafelistedRequestHeader(name, combined)) {
                    return;
                }
            }
            list.add(new String[] { name.toLowerCase(), v });
        }

        /**
         * Sets a header: the first header of that name gets the value, the others are removed.
         * <p>
         * 헤더를 지정한다: 같은 이름의 첫 헤더가 값을 받고 나머지는 제거한다.
         *
         * @param name  the name / 이름
         * @param value the raw value / 원본 값
         */
        void set(String name, String value) {
            String v = HeaderRules.normalizeValue(value);
            if (!validate(name, v, "set")) {
                return;
            }
            if ("request-no-cors".equals(guard) && !HeaderRules.isSafelistedRequestHeader(name, v)) {
                return;
            }
            String n = name.toLowerCase();
            boolean found = false;
            // 이름이 같은 첫 항목은 값을 바꾸고 나머지는 지운다.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i)[0].equals(n)) {
                    if (!found) {
                        list.get(i)[1] = v;
                        found = true;
                    } else {
                        list.remove(i);
                        i--;
                    }
                }
            }
            if (!found) {
                list.add(new String[] { n, v });
            }
        }

        /**
         * Removes the headers of a name.
         * <p>
         * 이름이 같은 헤더를 제거한다.
         *
         * @param name the name / 이름
         */
        void remove(String name) {
            if (!validate(name, "", "delete")) {
                return;
            }
            String n = name.toLowerCase();
            // 이름이 같은 항목을 모두 지운다.
            for (int i = list.size() - 1; i >= 0; i--) {
                if (list.get(i)[0].equals(n)) {
                    list.remove(i);
                }
            }
        }

        /**
         * Returns the combined value of a name, or null.
         * <p>
         * 이름의 합친 값을 반환한다. 없으면 null.
         *
         * @param name the name / 이름
         * @return the values joined with ", ", or null / ", " 로 이은 값. 없으면 null
         */
        String get(String name) {
            String n = name.toLowerCase();
            StringBuilder out = null;
            // 이름이 같은 값을 순서대로 ", " 로 잇는다.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i)[0].equals(n)) {
                    if (out == null) {
                        out = new StringBuilder(list.get(i)[1]);
                    } else {
                        out.append(", ").append(list.get(i)[1]);
                    }
                }
            }
            return out == null ? null : out.toString();
        }

        /**
         * Returns the list sorted by name with equal names combined (<code>Set-Cookie</code> stays separate).
         * <p>
         * 이름순으로 정렬하고 같은 이름을 합친 목록을 반환한다(<code>Set-Cookie</code> 는 따로 둔다).
         *
         * @return the (name, value) pairs / (이름, 값) 쌍
         */
        List<String[]> sorted() {
            List<String> names = new ArrayList<String>();
            // 이름의 중복을 없앤 목록을 만든다.
            for (int i = 0; i < list.size(); i++) {
                if (!names.contains(list.get(i)[0])) {
                    names.add(list.get(i)[0]);
                }
            }
            Collections.sort(names, new Comparator<String>() {
                public int compare(String a, String b) {
                    return a.compareTo(b);
                }
            });
            List<String[]> out = new ArrayList<String[]>();
            // 이름마다 값을 합치되 set-cookie 는 값마다 항목을 따로 낸다.
            for (int i = 0; i < names.size(); i++) {
                String n = names.get(i);
                if ("set-cookie".equals(n)) {
                    // set-cookie 값은 하나씩 따로 낸다.
                    for (int j = 0; j < list.size(); j++) {
                        if (list.get(j)[0].equals(n)) {
                            out.add(new String[] { n, list.get(j)[1] });
                        }
                    }
                } else {
                    out.add(new String[] { n, get(n) });
                }
            }
            return out;
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
    HeadersApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs <code>Headers</code>.
     * <p>
     * <code>Headers</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        JsRealm realm = api.realm();
        Natives.ClassDef c = Natives.defineClass(realm, g, "Headers", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HeadersObject h = new HeadersObject(Ops.getPrototypeFromConstructor(r, newTarget, proto), "none");
                fill(h, Js.arg(args, 0));
                return h;
            }
        });
        proto = c.prototype;
        Natives.method(realm, proto, "append", 2, this, "append");
        Natives.method(realm, proto, "delete", 1, this, "delete");
        Natives.method(realm, proto, "get", 1, this, "get");
        Natives.method(realm, proto, "getSetCookie", 0, this, "getSetCookie");
        Natives.method(realm, proto, "has", 1, this, "has");
        Natives.method(realm, proto, "set", 2, this, "set");
        Natives.method(realm, proto, "forEach", 1, this, "forEach");
        Natives.method(realm, proto, "keys", 0, this, "keys");
        Natives.method(realm, proto, "values", 0, this, "values");
        NativeFunction entries = Natives.function(realm, "entries", 0, this, "entries");
        proto.defineData("entries", entries, true, true, true);
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, entries);
    }

    /**
     * Creates an empty headers object with a guard.
     * <p>
     * guard 를 가진 빈 headers 객체를 만든다.
     *
     * @param guard the guard / guard
     * @return the object / 객체
     */
    HeadersObject create(String guard) {
        return new HeadersObject(proto, guard);
    }

    /**
     * Creates a headers object from the visible headers of a web response.
     * <p>
     * 웹 응답의 보이는 헤더로 headers 객체를 만든다.
     *
     * @param pairs the (name, value) pairs / (이름, 값) 쌍
     * @param guard the guard / guard
     * @return the object / 객체
     */
    HeadersObject create(List<String[]> pairs, String guard) {
        HeadersObject h = new HeadersObject(proto, "none");
        // 쌍마다 검증 없이 목록에 넣는다(응답 헤더는 이미 걸러졌다).
        for (int i = 0; i < pairs.size(); i++) {
            h.list.add(new String[] { pairs.get(i)[0].toLowerCase(), pairs.get(i)[1] });
        }
        h.guard = guard;
        return h;
    }

    /**
     * Fills a headers object from a <code>HeadersInit</code>: a sequence of pairs (a <code>Headers</code> object is one) or a record.
     * <p>
     * <code>HeadersInit</code> 으로 headers 객체를 채운다: 쌍의 시퀀스(<code>Headers</code> 객체도 해당) 또는 레코드.
     *
     * @param h    the object / 객체
     * @param init the init value / 초기 값
     */
    void fill(HeadersObject h, JsValue init) {
        if (init.isUndefined()) {
            return;
        }
        if (!init.isObject()) {
            throw JsException.typeError("Failed to construct 'Headers': The provided value is not of type '(record<ByteString, ByteString> or "
                    + "sequence<sequence<ByteString>>)'.");
        }
        JsObject o = (JsObject) init;
        if (o.get(WellKnownSymbols.ITERATOR).isCallable()) {
            List<JsValue> items = IteratorHelper.toList(api.realm(), o);
            // 각 항목은 이름과 값 정확히 둘로 된 시퀀스여야 한다.
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isObject()) {
                    throw JsException.typeError("Failed to construct 'Headers': The provided value cannot be converted to a sequence.");
                }
                List<JsValue> pair = IteratorHelper.toList(api.realm(), items.get(i));
                if (pair.size() != 2) {
                    throw JsException.typeError("Failed to construct 'Headers': Invalid value");
                }
                h.append(Natives.byteString(pair.get(0), "append", "Headers"), Natives.byteString(pair.get(1), "append", "Headers"));
            }
            return;
        }
        List<String> keys = Ops.enumerableOwnKeys(o);
        // 레코드의 열거 가능한 문자열 키마다 헤더 하나를 덧붙인다.
        for (int i = 0; i < keys.size(); i++) {
            h.append(Natives.byteString(Js.str(keys.get(i)), "append", "Headers"), Natives.byteString(o.get(keys.get(i)), "append", "Headers"));
        }
    }

    /**
     * Implements <code>headers.append(name, value)</code>.
     * <p>
     * <code>headers.append(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue append(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        Natives.requireArgs(args, 2, "append", "Headers");
        h.append(Natives.byteString(args[0], "append", "Headers"), Natives.byteString(args[1], "append", "Headers"));
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>headers.delete(name)</code>.
     * <p>
     * <code>headers.delete(name)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue delete(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        Natives.requireArgs(args, 1, "delete", "Headers");
        h.remove(Natives.byteString(args[0], "delete", "Headers"));
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>headers.get(name)</code>.
     * <p>
     * <code>headers.get(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the combined value or null / 합친 값 또는 null
     */
    JsValue get(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        Natives.requireArgs(args, 1, "get", "Headers");
        String name = Natives.byteString(args[0], "get", "Headers");
        if (!HeaderRules.isToken(name)) {
            throw JsException.typeError("Failed to execute 'get' on 'Headers': Invalid name");
        }
        String v = h.get(name);
        return v == null ? Js.NULL : Js.str(v);
    }

    /**
     * Implements <code>headers.getSetCookie()</code>.
     * <p>
     * <code>headers.getSetCookie()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an array of values / 값의 배열
     */
    JsValue getSetCookie(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        List<JsValue> out = new ArrayList<JsValue>();
        // set-cookie 항목의 값을 순서대로 모은다.
        for (int i = 0; i < h.list.size(); i++) {
            if ("set-cookie".equals(h.list.get(i)[0])) {
                out.add(Js.str(h.list.get(i)[1]));
            }
        }
        return api.realm().newArray(out);
    }

    /**
     * Implements <code>headers.has(name)</code>.
     * <p>
     * <code>headers.has(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return whether the name exists / 이름이 있는지
     */
    JsValue has(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        Natives.requireArgs(args, 1, "has", "Headers");
        String name = Natives.byteString(args[0], "has", "Headers");
        if (!HeaderRules.isToken(name)) {
            throw JsException.typeError("Failed to execute 'has' on 'Headers': Invalid name");
        }
        return Js.bool(h.get(name) != null);
    }

    /**
     * Implements <code>headers.set(name, value)</code>.
     * <p>
     * <code>headers.set(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue set(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        Natives.requireArgs(args, 2, "set", "Headers");
        h.set(Natives.byteString(args[0], "set", "Headers"), Natives.byteString(args[1], "set", "Headers"));
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>headers.forEach(callback, thisArg)</code>.
     * <p>
     * <code>headers.forEach(callback, thisArg)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue forEach(JsValue self, JsValue[] args) {
        HeadersObject h = Natives.self(self, HeadersObject.class);
        Natives.requireArgs(args, 1, "forEach", "Headers");
        if (!args[0].isCallable()) {
            throw JsException.typeError("Failed to execute 'forEach' on 'Headers': The callback provided as parameter 1 is not a function.");
        }
        JsValue thisArg = Js.arg(args, 1);
        List<String[]> pairs = h.sorted();
        // 정렬된 쌍마다 콜백(값, 이름, headers)을 부른다.
        for (int i = 0; i < pairs.size(); i++) {
            Ops.call(args[0], thisArg, new JsValue[] { Js.str(pairs.get(i)[1]), Js.str(pairs.get(i)[0]), h });
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>headers.keys()</code>.
     * <p>
     * <code>headers.keys()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue keys(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, HeadersObject.class), 0);
    }

    /**
     * Implements <code>headers.values()</code>.
     * <p>
     * <code>headers.values()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue values(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, HeadersObject.class), 1);
    }

    /**
     * Implements <code>headers.entries()</code> and <code>headers[Symbol.iterator]()</code>.
     * <p>
     * <code>headers.entries()</code> 와 <code>headers[Symbol.iterator]()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue entries(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, HeadersObject.class), 2);
    }

    /**
     * Creates an iterator over the sorted names, values or pairs.
     * <p>
     * 정렬된 이름, 값, 쌍에 대한 반복자를 만든다.
     *
     * @param h    the object / 객체
     * @param kind 0 names, 1 values, 2 pairs / 0 이름, 1 값, 2 쌍
     * @return the iterator / 반복자
     */
    private JsValue iterate(HeadersObject h, int kind) {
        List<String[]> pairs = h.sorted();
        List<JsValue> out = new ArrayList<JsValue>();
        // 정렬된 쌍으로 이름, 값, 또는 [이름, 값] 배열의 목록을 만든다.
        for (int i = 0; i < pairs.size(); i++) {
            if (kind == 0) {
                out.add(Js.str(pairs.get(i)[0]));
            } else if (kind == 1) {
                out.add(Js.str(pairs.get(i)[1]));
            } else {
                out.add(Natives.pair(api.realm(), Js.str(pairs.get(i)[0]), Js.str(pairs.get(i)[1])));
            }
        }
        return Natives.iterator(api.realm(), out);
    }
}
