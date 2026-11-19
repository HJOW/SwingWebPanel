package org.duckdns.hjow.swingwebpanel.js.interp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * A module namespace exotic object (ECMA-262 6th edition 9.4.6): the object of <code>import * as ns</code>. It has no
 * prototype, is not extensible, lists the exported names in alphabetical order and exposes every export as a writable-looking
 * but read-only live property that always reads the current value of the exporting module's binding.
 * <p>
 * 모듈 네임스페이스 이국적 객체(ECMA-262 6판 9.4.6): <code>import * as ns</code> 의 객체. 프로토타입이 없고 확장할 수 없으며,
 * export 이름을 알파벳 순으로 나열하고, 모든 export 를 export 하는 모듈 바인딩의 현재 값을 항상 읽는 읽기 전용 실시간 프로퍼티로 보여 준다.
 */
final class ModuleNamespace extends JsObject {

    /**
     * Exported name to the binding that holds the value.
     * <p>
     * export 이름과 값을 가진 바인딩.
     */
    private final Map<String, Binding> exports = new TreeMap<String, Binding>();

    /**
     * Creates the namespace object.
     * <p>
     * 네임스페이스 객체를 만든다.
     */
    ModuleNamespace() {
        super(null);
        extensible = false;
    }

    /**
     * Adds an export (during linking).
     * <p>
     * export 를 추가한다(링크 중).
     *
     * @param name    the export name / export 이름
     * @param binding the binding / 바인딩
     */
    void addExport(String name, Binding binding) {
        exports.put(name, binding);
    }

    /**
     * Returns "Module".
     * <p>
     * "Module" 을 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Module";
    }

    /**
     * Exposes the exports and the toStringTag.
     * <p>
     * export 와 toStringTag 를 보여 준다.
     *
     * @param key the key / 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        if (key instanceof String) {
            Binding b = exports.get(key);
            if (b == null) {
                return null;
            }
            return Property.data(b.read((String) key), true, true, false);
        }
        if (key == WellKnownSymbols.TO_STRING_TAG) {
            return Property.data(JsString.of("Module"), false, false, false);
        }
        return null;
    }

    /**
     * Rejects all definitions that would change the namespace.
     * <p>
     * 네임스페이스를 바꾸는 모든 정의를 거부한다.
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     * @return true only for compatible redefinitions / 호환되는 재정의일 때만 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        Property cur = getOwnPropertySlot(key);
        if (cur == null) {
            return false;
        }
        return validateAndApply(key, desc, cur);
    }

    /**
     * Assignments always fail.
     * <p>
     * 대입은 항상 실패한다.
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / receiver
     * @return false / false
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        return false;
    }

    /**
     * Exports cannot be deleted.
     * <p>
     * export 는 지울 수 없다.
     *
     * @param key the key / 키
     * @return true only if the property does not exist / 프로퍼티가 없을 때만 true
     */
    public boolean delete(Object key) {
        return getOwnPropertySlot(key) == null;
    }

    /**
     * The prototype of a namespace object is always null.
     * <p>
     * 네임스페이스 객체의 프로토타입은 항상 null 이다.
     *
     * @param proto the new prototype / 새 프로토타입
     * @return true only for null / null 일 때만 true
     */
    public boolean setPrototypeOf(JsObject proto) {
        return proto == null;
    }

    /**
     * Lists the export names alphabetically followed by the toStringTag symbol.
     * <p>
     * export 이름을 알파벳 순으로, 이어서 toStringTag 심볼을 나열한다.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        List<Object> keys = new ArrayList<Object>(exports.keySet());
        Collections.sort(keys, new java.util.Comparator<Object>() {
            public int compare(Object a, Object b) {
                return ((String) a).compareTo((String) b);
            }
        });
        keys.add(WellKnownSymbols.TO_STRING_TAG);
        return keys;
    }

    /**
     * Returns the value of an export or undefined.
     * <p>
     * export 의 값을 반환한다. 없으면 undefined.
     *
     * @param name the export name / export 이름
     * @return the value / 값
     */
    JsValue valueOf(String name) {
        Binding b = exports.get(name);
        return b == null ? Js.UNDEFINED : b.read(name);
    }
}
