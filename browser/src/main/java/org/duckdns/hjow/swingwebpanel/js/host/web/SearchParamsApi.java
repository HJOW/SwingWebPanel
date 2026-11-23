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
 * Installs <code>URLSearchParams</code> (URL Standard, section 6): a list of name/value pairs that is parsed from and serialized to
 * <code>application/x-www-form-urlencoded</code> text. The object created by <code>new URL(...).searchParams</code> is linked to its URL:
 * changing the list rewrites the query of the URL, and changing the query of the URL rebuilds the list. Iteration returns a snapshot
 * iterator; the pairs themselves are always Unicode scalar value strings.
 * <p>
 * <code>URLSearchParams</code> 를 설치한다(URL 표준 6절): <code>application/x-www-form-urlencoded</code> 텍스트에서 해석하고 그것으로 직렬화하는 이름/값 쌍의
 * 목록이다. <code>new URL(...).searchParams</code> 가 만드는 객체는 URL과 연결된다: 목록을 바꾸면 URL의 쿼리가 다시 쓰이고 URL의 쿼리를 바꾸면 목록이 다시
 * 만들어진다. 반복은 스냅샷 반복자를 반환하며 쌍은 항상 유니코드 스칼라 값 문자열이다.
 */
final class SearchParamsApi {

    /**
     * The script object of a <code>URLSearchParams</code>.
     * <p>
     * <code>URLSearchParams</code> 의 스크립트 객체.
     */
    static final class ParamsObject extends JsObject {
        /** The (name, value) pairs. / (이름, 값) 쌍. */
        final List<String[]> list = new ArrayList<String[]>();

        /** The URL whose query this list mirrors, or null. / 이 목록이 쿼리를 비추는 URL. 없으면 null. */
        UrlApi.UrlObject url;

        /**
         * Creates an empty object.
         * <p>
         * 빈 객체를 만든다.
         *
         * @param proto the prototype / 프로토타입
         */
        ParamsObject(JsObject proto) {
            super(proto);
        }

        /**
         * Rewrites the query of the linked URL after the list changed.
         * <p>
         * 목록이 바뀐 뒤 연결된 URL의 쿼리를 다시 쓴다.
         */
        void update() {
            if (url != null) {
                String s = Encodings.formSerialize(list);
                url.setQueryFromParams(s.length() == 0 ? null : s);
            }
        }

        /**
         * Replaces the list by the parsed text.
         * <p>
         * 목록을 해석한 텍스트로 바꾼다.
         *
         * @param query the query text without '?', or null for none / '?' 없는 쿼리 텍스트. 없으면 null
         */
        void reset(String query) {
            list.clear();
            if (query != null) {
                list.addAll(Encodings.formParse(query));
            }
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
    SearchParamsApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs <code>URLSearchParams</code>.
     * <p>
     * <code>URLSearchParams</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        JsRealm realm = api.realm();
        Natives.ClassDef c = Natives.defineClass(realm, g, "URLSearchParams", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                ParamsObject p = new ParamsObject(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                fill(p, Js.arg(args, 0));
                return p;
            }
        });
        proto = c.prototype;
        Natives.method(realm, proto, "append", 2, this, "append");
        Natives.method(realm, proto, "delete", 1, this, "delete");
        Natives.method(realm, proto, "get", 1, this, "get");
        Natives.method(realm, proto, "getAll", 1, this, "getAll");
        Natives.method(realm, proto, "has", 1, this, "has");
        Natives.method(realm, proto, "set", 2, this, "set");
        Natives.method(realm, proto, "sort", 0, this, "sort");
        Natives.method(realm, proto, "toString", 0, this, "string");
        Natives.method(realm, proto, "forEach", 1, this, "forEach");
        Natives.method(realm, proto, "keys", 0, this, "keys");
        Natives.method(realm, proto, "values", 0, this, "values");
        NativeFunction entries = Natives.function(realm, "entries", 0, this, "entries");
        proto.defineData("entries", entries, true, true, true);
        proto.defineBuiltin(WellKnownSymbols.ITERATOR, entries);
        Natives.getter(realm, proto, "size", this, "size");
    }

    /**
     * Creates an empty list object linked to a URL.
     * <p>
     * URL에 연결된 빈 목록 객체를 만든다.
     *
     * @param url the URL or null / URL 또는 null
     * @return the object / 객체
     */
    ParamsObject create(UrlApi.UrlObject url) {
        ParamsObject p = new ParamsObject(proto);
        p.url = url;
        return p;
    }

    /**
     * Fills a list from the constructor argument (a sequence of pairs, a record or a query string).
     * <p>
     * 생성자 인수(쌍의 시퀀스, 레코드, 쿼리 문자열)로 목록을 채운다.
     *
     * @param p    the object / 객체
     * @param init the init value / 초기 값
     */
    private void fill(ParamsObject p, JsValue init) {
        if (init.isUndefined()) {
            return;
        }
        if (!init.isObject()) {
            String s = Natives.usv(init);
            p.reset(s.startsWith("?") ? s.substring(1) : s);
            return;
        }
        JsObject o = (JsObject) init;
        if (o.get(WellKnownSymbols.ITERATOR).isCallable()) {
            List<JsValue> items = IteratorHelper.toList(api.realm(), o);
            // 각 항목은 이름과 값 정확히 둘로 된 시퀀스여야 한다.
            for (int i = 0; i < items.size(); i++) {
                if (!items.get(i).isObject()) {
                    throw JsException.typeError("Failed to construct 'URLSearchParams': The provided value cannot be converted to a sequence.");
                }
                List<JsValue> pair = IteratorHelper.toList(api.realm(), items.get(i));
                if (pair.size() != 2) {
                    throw JsException.typeError("Failed to construct 'URLSearchParams': Each query pair must be an iterable [name, value] "
                            + "tuple");
                }
                p.list.add(new String[] { Natives.usv(pair.get(0)), Natives.usv(pair.get(1)) });
            }
            return;
        }
        List<String> keys = Ops.enumerableOwnKeys(o);
        // 레코드의 열거 가능한 문자열 키마다 한 쌍을 만든다.
        for (int i = 0; i < keys.size(); i++) {
            p.list.add(new String[] { Natives.usv(keys.get(i)), Natives.usv(o.get(keys.get(i))) });
        }
    }

    /**
     * Implements <code>params.append(name, value)</code>.
     * <p>
     * <code>params.append(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue append(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 2, "append", "URLSearchParams");
        p.list.add(new String[] { Natives.usv(args[0]), Natives.usv(args[1]) });
        p.update();
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>params.delete(name, value)</code>.
     * <p>
     * <code>params.delete(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue delete(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 1, "delete", "URLSearchParams");
        String name = Natives.usv(args[0]);
        String value = args.length > 1 && !args[1].isUndefined() ? Natives.usv(args[1]) : null;
        // 이름(과 값)이 같은 쌍을 모두 지운다.
        for (int i = p.list.size() - 1; i >= 0; i--) {
            String[] e = p.list.get(i);
            if (e[0].equals(name) && (value == null || e[1].equals(value))) {
                p.list.remove(i);
            }
        }
        p.update();
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>params.get(name)</code>.
     * <p>
     * <code>params.get(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the first value or null / 첫 값 또는 null
     */
    JsValue get(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 1, "get", "URLSearchParams");
        String name = Natives.usv(args[0]);
        // 이름이 같은 첫 쌍의 값을 찾는다.
        for (int i = 0; i < p.list.size(); i++) {
            if (p.list.get(i)[0].equals(name)) {
                return Js.str(p.list.get(i)[1]);
            }
        }
        return Js.NULL;
    }

    /**
     * Implements <code>params.getAll(name)</code>.
     * <p>
     * <code>params.getAll(name)</code> 을 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an array of values / 값의 배열
     */
    JsValue getAll(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 1, "getAll", "URLSearchParams");
        String name = Natives.usv(args[0]);
        List<JsValue> out = new ArrayList<JsValue>();
        // 이름이 같은 모든 쌍의 값을 순서대로 모은다.
        for (int i = 0; i < p.list.size(); i++) {
            if (p.list.get(i)[0].equals(name)) {
                out.add(Js.str(p.list.get(i)[1]));
            }
        }
        return api.realm().newArray(out);
    }

    /**
     * Implements <code>params.has(name, value)</code>.
     * <p>
     * <code>params.has(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return whether a pair matches / 일치하는 쌍이 있는지
     */
    JsValue has(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 1, "has", "URLSearchParams");
        String name = Natives.usv(args[0]);
        String value = args.length > 1 && !args[1].isUndefined() ? Natives.usv(args[1]) : null;
        // 이름(과 값)이 같은 쌍이 하나라도 있는지 찾는다.
        for (int i = 0; i < p.list.size(); i++) {
            String[] e = p.list.get(i);
            if (e[0].equals(name) && (value == null || e[1].equals(value))) {
                return Js.TRUE;
            }
        }
        return Js.FALSE;
    }

    /**
     * Implements <code>params.set(name, value)</code>.
     * <p>
     * <code>params.set(name, value)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue set(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 2, "set", "URLSearchParams");
        String name = Natives.usv(args[0]);
        String value = Natives.usv(args[1]);
        boolean found = false;
        // 이름이 같은 첫 쌍은 값을 바꾸고 나머지는 지운다.
        for (int i = 0; i < p.list.size(); i++) {
            String[] e = p.list.get(i);
            if (e[0].equals(name)) {
                if (!found) {
                    e[1] = value;
                    found = true;
                } else {
                    p.list.remove(i);
                    i--;
                }
            }
        }
        if (!found) {
            p.list.add(new String[] { name, value });
        }
        p.update();
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>params.sort()</code>: a stable sort by the UTF-16 code units of the names.
     * <p>
     * <code>params.sort()</code> 를 구현한다: 이름의 UTF-16 코드 단위로 안정 정렬한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue sort(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Collections.sort(p.list, new Comparator<String[]>() {
            public int compare(String[] a, String[] b) {
                return a[0].compareTo(b[0]);
            }
        });
        p.update();
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>params.toString()</code>.
     * <p>
     * <code>params.toString()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the serialization / 직렬화 문자열
     */
    JsValue string(JsValue self, JsValue[] args) {
        return Js.str(Encodings.formSerialize(Natives.self(self, ParamsObject.class).list));
    }

    /**
     * Implements <code>params.forEach(callback, thisArg)</code>.
     * <p>
     * <code>params.forEach(callback, thisArg)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue forEach(JsValue self, JsValue[] args) {
        ParamsObject p = Natives.self(self, ParamsObject.class);
        Natives.requireArgs(args, 1, "forEach", "URLSearchParams");
        if (!args[0].isCallable()) {
            throw JsException.typeError("Failed to execute 'forEach' on 'URLSearchParams': The callback provided as parameter 1 is not a "
                    + "function.");
        }
        JsValue thisArg = Js.arg(args, 1);
        // 호출 중 목록이 바뀔 수 있으므로 매번 현재 크기를 다시 확인하며 순회한다.
        for (int i = 0; i < p.list.size(); i++) {
            String[] e = p.list.get(i);
            Ops.call(args[0], thisArg, new JsValue[] { Js.str(e[1]), Js.str(e[0]), p });
        }
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>params.keys()</code>.
     * <p>
     * <code>params.keys()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue keys(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, ParamsObject.class), 0);
    }

    /**
     * Implements <code>params.values()</code>.
     * <p>
     * <code>params.values()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue values(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, ParamsObject.class), 1);
    }

    /**
     * Implements <code>params.entries()</code> and <code>params[Symbol.iterator]()</code>.
     * <p>
     * <code>params.entries()</code> 와 <code>params[Symbol.iterator]()</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return an iterator / 반복자
     */
    JsValue entries(JsValue self, JsValue[] args) {
        return iterate(Natives.self(self, ParamsObject.class), 2);
    }

    /**
     * Returns <code>params.size</code>.
     * <p>
     * <code>params.size</code> 를 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the number of pairs / 쌍의 수
     */
    JsValue size(JsValue self, JsValue[] args) {
        return Js.num(Natives.self(self, ParamsObject.class).list.size());
    }

    /**
     * Creates an iterator over the keys, the values or the pairs.
     * <p>
     * 키, 값, 쌍에 대한 반복자를 만든다.
     *
     * @param p    the object / 객체
     * @param kind 0 for keys, 1 for values, 2 for pairs / 0 은 키, 1 은 값, 2 는 쌍
     * @return the iterator / 반복자
     */
    private JsValue iterate(ParamsObject p, int kind) {
        List<JsValue> out = new ArrayList<JsValue>();
        // 현재 목록의 스냅샷을 만들어 그 배열 반복자를 돌려준다.
        for (int i = 0; i < p.list.size(); i++) {
            String[] e = p.list.get(i);
            if (kind == 0) {
                out.add(Js.str(e[0]));
            } else if (kind == 1) {
                out.add(Js.str(e[1]));
            } else {
                out.add(Natives.pair(api.realm(), Js.str(e[0]), Js.str(e[1])));
            }
        }
        return Natives.iterator(api.realm(), out);
    }
}
