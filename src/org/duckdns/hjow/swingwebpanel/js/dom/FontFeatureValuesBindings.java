package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsArray;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The CSSOM interfaces of <code>@font-feature-values</code> (CSS Fonts Level 4 section 9.1, https://www.w3.org/TR/css-fonts-4/ WD 12 November 2026):
 * <code>CSSFontFeatureValuesRule</code> (<code>fontFamily</code> and the seven maps <code>annotation</code>, <code>ornaments</code>, <code>stylistic</code>,
 * <code>swash</code>, <code>characterVariant</code>, <code>styleset</code>, <code>historicalForms</code>) and <code>CSSFontFeatureValuesMap</code> (a maplike
 * from names to arrays of feature indexes with <code>set(name, index or indexes)</code>, which checks the indexes like a declaration of its block and
 * throws an <code>InvalidAccessError</code> otherwise).
 * <p>
 * <code>@font-feature-values</code> 의 CSSOM 인터페이스(CSS Fonts Level 4 9.1 절): <code>CSSFontFeatureValuesRule</code>(<code>fontFamily</code> 와 일곱 맵 <code>annotation</code>,
 * <code>ornaments</code>, <code>stylistic</code>, <code>swash</code>, <code>characterVariant</code>, <code>styleset</code>, <code>historicalForms</code>)과
 * <code>CSSFontFeatureValuesMap</code>(이름에서 기능 번호 배열로 가는 maplike. <code>set(이름, 번호나 번호들)</code> 은 블록의 선언처럼 번호를 확인하고 아니면
 * <code>InvalidAccessError</code> 를 던진다).
 */
final class FontFeatureValuesBindings {

    /** The attribute names of the maps, in the order of {@link FontFeatureValuesRule#TYPES}. {@link FontFeatureValuesRule#TYPES} 순서의 맵 속성 이름. */
    private static final String[] ATTRIBUTES = { "swash", "stylistic", "ornaments", "annotation", "characterVariant", "styleset", "historicalForms" };

    /**
     * Not instantiable; the class only installs interfaces.
     * <p>
     * 인스턴스를 만들지 않는다. 인터페이스만 설치한다.
     */
    private FontFeatureValuesBindings() {
    }

    /**
     * Tells the interface name of an object of this binding.
     * <p>
     * 이 바인딩의 객체의 인터페이스 이름을 알려 준다.
     *
     * @param o the object / 객체
     * @return the name or null / 이름 또는 null
     */
    static String classify(Object o) {
        if (o instanceof FontFeatureValuesRule) {
            return "CSSFontFeatureValuesRule";
        }
        return o instanceof FontFeatureValuesRule.ValueMap ? "CSSFontFeatureValuesMap" : null;
    }

    /**
     * Installs <code>CSSFontFeatureValuesRule</code> and <code>CSSFontFeatureValuesMap</code> (after <code>CSSRule</code>).
     * <p>
     * <code>CSSFontFeatureValuesRule</code> 과 <code>CSSFontFeatureValuesMap</code> 을 설치한다(<code>CSSRule</code> 다음에).
     *
     * @param bridge the bridge / 브리지
     */
    static void install(DomBridge bridge) {
        final JsRealm realm = bridge.getRealm();
        DomBridge.InterfaceInfo rule = bridge.defineInterface("CSSFontFeatureValuesRule", "CSSRule", FontFeatureValuesRule.class, new String[] { "a:fontFamily=" });
        // 일곱 맵 속성을 설치한다.
        for (int i = 0; i < ATTRIBUTES.length; i++) {
            final String type = FontFeatureValuesRule.TYPES[i];
            CssomMembers.accessor(bridge, rule, ATTRIBUTES[i], new CssomMembers.Op() {
                /** Returns the map of the type. / 종류의 맵을 반환한다. */
                public JsValue run(DomBridge b, Object target, JsValue[] args) {
                    return b.toJs(((FontFeatureValuesRule) target).getMap(type));
                }
            }, null);
        }
        DomBridge.InterfaceInfo map = bridge.defineInterface("CSSFontFeatureValuesMap", null, FontFeatureValuesRule.ValueMap.class, new String[0]);
        CssomMembers.method(bridge, map, "get", 1, new CssomMembers.Op() {
            /** Returns the indexes of a name as an array, or undefined. / 이름의 번호를 배열로 반환한다. 없으면 undefined. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                int[] v = ((FontFeatureValuesRule.ValueMap) target).get(Ops.toString(Js.arg(args, 0)));
                return v == null ? Js.UNDEFINED : array(realm, v);
            }
        });
        CssomMembers.method(bridge, map, "has", 1, new CssomMembers.Op() {
            /** Tells whether a name is defined. / 이름이 정의되어 있는지 알려 준다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.bool(((FontFeatureValuesRule.ValueMap) target).has(Ops.toString(Js.arg(args, 0))));
            }
        });
        CssomMembers.method(bridge, map, "set", 2, new CssomMembers.Op() {
            /** Defines a name with an index or a sequence of indexes and returns the map. / 이름을 번호나 번호 목록으로 정의하고 맵을 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                JsValue v = Js.arg(args, 1);
                int[] indexes;
                if (v instanceof JsObject) {
                    // 배열 같은 객체이면 원소를 unsigned long 으로 바꾼다.
                    JsObject o = (JsObject) v;
                    int n = (int) Math.min(1 << 16, Math.max(0, Ops.toLength(o.get("length"))));
                    indexes = new int[n];
                    // 원소를 차례로 바꾼다.
                    for (int i = 0; i < n; i++) {
                        indexes[i] = (int) Ops.toUint32(o.get(String.valueOf(i)));
                    }
                } else {
                    indexes = new int[] { (int) Ops.toUint32(v) };
                }
                try {
                    ((FontFeatureValuesRule.ValueMap) target).set(Ops.toString(Js.arg(args, 0)), indexes);
                } catch (IllegalArgumentException e) {
                    throw b.domError(new org.duckdns.hjow.swingwebpanel.dom.DomException("InvalidAccessError", org.duckdns.hjow.swingwebpanel.dom.DomException.INVALID_ACCESS_ERR, e.getMessage()));
                }
                return b.toJs(target);
            }
        });
        CssomMembers.method(bridge, map, "delete", 1, new CssomMembers.Op() {
            /** Removes a name. / 이름을 지운다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.bool(((FontFeatureValuesRule.ValueMap) target).delete(Ops.toString(Js.arg(args, 0))));
            }
        });
        CssomMembers.method(bridge, map, "clear", 0, new CssomMembers.Op() {
            /** Removes every name. / 모든 이름을 지운다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                ((FontFeatureValuesRule.ValueMap) target).clear();
                return Js.UNDEFINED;
            }
        });
        CssomMembers.accessor(bridge, map, "size", new CssomMembers.Op() {
            /** Returns the number of names. / 이름의 수를 반환한다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                return Js.num(((FontFeatureValuesRule.ValueMap) target).getSize());
            }
        }, null);
        CssomMembers.method(bridge, map, "forEach", 1, new CssomMembers.Op() {
            /** Calls a function with the indexes, the name and the map of every entry. / 항목마다 번호, 이름, 맵으로 함수를 부른다. */
            public JsValue run(DomBridge b, Object target, JsValue[] args) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to execute 'forEach' on 'CSSFontFeatureValuesMap': The callback provided as parameter 1 is not a function.");
                }
                FontFeatureValuesRule.ValueMap m = (FontFeatureValuesRule.ValueMap) target;
                List<String> names = m.names();
                // 이름의 스냅샷을 순서대로 넘긴다.
                for (int i = 0; i < names.size(); i++) {
                    int[] v = m.get(names.get(i));
                    if (v != null) {
                        Ops.call(cb, Js.arg(args, 1), new JsValue[] { array(realm, v), Js.str(names.get(i)), b.toJs(target) });
                    }
                }
                return Js.UNDEFINED;
            }
        });
        final String[] kinds = { "keys", "values", "entries" };
        // 반복자 메서드 셋을 설치한다.
        for (int k = 0; k < kinds.length; k++) {
            final int kind = k;
            CssomMembers.method(bridge, map, kinds[k], 0, new CssomMembers.Op() {
                /** Returns an iterator over a snapshot of the names, the indexes or the pairs. / 이름, 번호, 쌍의 스냅샷에 대한 반복자를 반환한다. */
                public JsValue run(DomBridge b, Object target, JsValue[] args) {
                    FontFeatureValuesRule.ValueMap m = (FontFeatureValuesRule.ValueMap) target;
                    List<String> names = m.names();
                    JsValue[] out = new JsValue[names.size()];
                    // 이름마다 원하는 모양의 값을 만든다.
                    for (int i = 0; i < out.length; i++) {
                        JsValue name = Js.str(names.get(i));
                        JsValue value = array(realm, m.get(names.get(i)));
                        out[i] = kind == 0 ? name : kind == 1 ? value : realm.newArray(new JsValue[] { name, value });
                    }
                    JsArray a = realm.newArray(out);
                    return Ops.call(a.get("values"), a, new JsValue[0]);
                }
            });
        }
        map.getPrototype().defineData(WellKnownSymbols.ITERATOR, map.getPrototype().get("entries"), true, false, true);
    }

    /**
     * Makes an array of numbers.
     * <p>
     * 수의 배열을 만든다.
     *
     * @param realm the realm / realm
     * @param v     the numbers / 수
     * @return the array / 배열
     */
    private static JsArray array(JsRealm realm, int[] v) {
        JsValue[] out = new JsValue[v.length];
        // 수를 스크립트 값으로 바꾼다.
        for (int i = 0; i < v.length; i++) {
            out[i] = Js.num(v[i]);
        }
        return realm.newArray(out);
    }
}
