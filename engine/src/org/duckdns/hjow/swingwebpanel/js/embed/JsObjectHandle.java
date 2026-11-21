package org.duckdns.hjow.swingwebpanel.js.embed;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * A live view of a script object as a <code>Map&lt;String, Object&gt;</code>. The entries are the enumerable own properties with string keys
 * (what <code>Object.keys</code> returns), in the order of the script. <code>get</code> reads any property including inherited and
 * non-enumerable ones (a missing property gives <code>null</code>, a property that holds <code>undefined</code> gives
 * <code>JsEngine.UNDEFINED</code>), <code>put</code> assigns a property (strictly: assigning to a read-only property throws), and
 * <code>remove</code> deletes it (a property that cannot be deleted, such as a global <code>var</code>, is set to undefined instead).
 * Values follow the conversion rules of {@link JsEngine}. Two handles are equal when they refer to the same script object; the
 * <code>equals</code>, <code>hashCode</code> and <code>toString</code> of the <code>Map</code> interface are not used because they would not
 * terminate for an object that contains itself.
 * <p>
 * 스크립트 객체를 <code>Map&lt;String, Object&gt;</code> 로 보여 주는 살아 있는 보기. 항목은 열거 가능한 고유 문자열 키 프로퍼티(<code>Object.keys</code> 가
 * 돌려주는 것)이며 스크립트의 순서를 따른다. <code>get</code> 은 상속된 것과 열거 불가인 것을 포함해 모든 프로퍼티를 읽고(없는 프로퍼티는
 * <code>null</code>, <code>undefined</code> 를 담은 프로퍼티는 <code>JsEngine.UNDEFINED</code>), <code>put</code> 은 프로퍼티를 대입하며(엄격하게:
 * 읽기 전용 프로퍼티에 대입하면 예외), <code>remove</code> 는 삭제한다(전역 <code>var</code> 처럼 삭제할 수 없는 프로퍼티는 대신 undefined 로 만든다).
 * 값은 {@link JsEngine} 의 변환 규칙을 따른다. 핸들 두 개는 같은 스크립트 객체를 가리킬 때 같다. 자기 자신을 포함한 객체에서는 끝나지 않으므로
 * <code>Map</code> 인터페이스의 <code>equals</code>, <code>hashCode</code>, <code>toString</code> 은 쓰지 않는다.
 */
public class JsObjectHandle extends AbstractMap<String, Object> implements JsHandle {

    /**
     * The engine that owns the object.
     * <p>
     * 객체를 가진 엔진.
     */
    private final JsEngine engine;

    /**
     * The script object.
     * <p>
     * 스크립트 객체.
     */
    private final JsObject object;

    /**
     * Creates a handle (use {@link JsEngine#newObject()} or convert a script value to get one).
     * <p>
     * 핸들을 만든다({@link JsEngine#newObject()} 를 쓰거나 스크립트 값을 바꿔 얻는다).
     *
     * @param engine the engine / 엔진
     * @param object the script object / 스크립트 객체
     */
    JsObjectHandle(JsEngine engine, JsObject object) {
        this.engine = engine;
        this.object = object;
    }

    /**
     * Returns the engine that owns the object.
     * <p>
     * 객체를 가진 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    public JsEngine getEngine() {
        return engine;
    }

    /**
     * Returns the script object itself.
     * <p>
     * 스크립트 객체 자체를 반환한다.
     *
     * @return the object / 객체
     */
    public JsObject getJsObject() {
        return object;
    }

    /**
     * Tells whether the object is a function that can be called (true for a {@link JsFunctionHandle}, false for other objects).
     * <p>
     * 객체가 호출할 수 있는 함수인지 알려 준다({@link JsFunctionHandle} 이면 true, 다른 객체이면 false).
     *
     * @return true for a function / 함수이면 true
     */
    public boolean isCallable() {
        return object.isCallable();
    }

    /**
     * Calls a method of the object.
     * <p>
     * 객체의 메서드를 호출한다.
     *
     * @param name the method name / 메서드 이름
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    public Object callMember(String name, Object... args) {
        return engine.invokeMethod(this, name, args);
    }

    /**
     * Returns the value of a property, or null if the object has no such property.
     * <p>
     * 프로퍼티의 값을 반환한다. 그런 프로퍼티가 없으면 null.
     *
     * @param key the property name (other types give null) / 프로퍼티 이름(다른 타입이면 null)
     * @return the value / 값
     */
    public Object get(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        synchronized (engine) {
            try {
                if (!object.hasProperty(key)) {
                    return null;
                }
                return JavaValues.toJava(engine, object.get(key));
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Tells whether the object has a property (own or inherited, enumerable or not).
     * <p>
     * 객체에 프로퍼티(고유 또는 상속, 열거 가능 여부 무관)가 있는지 알려 준다.
     *
     * @param key the property name / 프로퍼티 이름
     * @return true if the property exists / 프로퍼티가 있으면 true
     */
    public boolean containsKey(Object key) {
        if (!(key instanceof String)) {
            return false;
        }
        synchronized (engine) {
            try {
                return object.hasProperty(key);
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Assigns a property.
     * <p>
     * 프로퍼티를 대입한다.
     *
     * @param key   the property name / 프로퍼티 이름
     * @param value the value (converted by the rules of {@link JsEngine}) / 값({@link JsEngine} 의 규칙대로 변환)
     * @return the previous value, or null if there was none / 이전 값. 없었으면 null
     * @throws IllegalArgumentException if the value cannot be converted / 값을 바꿀 수 없으면
     * @throws JsScriptError            if the assignment fails (for example for a read-only property) / 대입이 실패하면(예: 읽기 전용 프로퍼티)
     */
    public Object put(String key, Object value) {
        synchronized (engine) {
            try {
                Object previous = get(key);
                object.setStrict(key, JavaValues.toJs(engine, value, key));
                return previous;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Deletes a property; a property that cannot be deleted is set to undefined instead.
     * <p>
     * 프로퍼티를 삭제한다. 삭제할 수 없는 프로퍼티는 대신 undefined 로 만든다.
     *
     * @param key the property name / 프로퍼티 이름
     * @return the previous value, or null if there was none / 이전 값. 없었으면 null
     */
    public Object remove(Object key) {
        if (!(key instanceof String)) {
            return null;
        }
        synchronized (engine) {
            try {
                Object previous = get(key);
                if (!object.delete(key)) {
                    object.set(key, Js.UNDEFINED);
                }
                return previous;
            } catch (JsException e) {
                throw engine.wrap(e);
            }
        }
    }

    /**
     * Returns the entries: the enumerable own properties with string keys. The set is a live view; its iterator works on the keys that
     * existed when it was created.
     * <p>
     * 항목을 반환한다: 열거 가능한 고유 문자열 키 프로퍼티. 이 집합은 살아 있는 보기이며 반복자는 만들어질 때 있던 키로 동작한다.
     *
     * @return the entries / 항목
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return new EntrySet();
    }

    /**
     * Tells whether another object is a handle of the same script object.
     * <p>
     * 다른 객체가 같은 스크립트 객체의 핸들인지 알려 준다.
     *
     * @param o the other object / 다른 객체
     * @return true if both refer to the same script object / 같은 스크립트 객체를 가리키면 true
     */
    public boolean equals(Object o) {
        return o instanceof JsHandle && ((JsHandle) o).getJsObject() == object;
    }

    /**
     * Returns a hash code of the script object's identity.
     * <p>
     * 스크립트 객체의 동일성에 대한 해시 코드를 반환한다.
     *
     * @return the hash code / 해시 코드
     */
    public int hashCode() {
        return System.identityHashCode(object);
    }

    /**
     * Describes the object (without running script code).
     * <p>
     * 객체를 설명한다(스크립트 코드를 실행하지 않는다).
     *
     * @return the description / 설명
     */
    public String toString() {
        return "[script " + object.className() + "]";
    }

    /**
     * The entries of the object.
     * <p>
     * 객체의 항목.
     */
    private final class EntrySet extends AbstractSet<Map.Entry<String, Object>> {

        /**
         * Creates the set.
         * <p>
         * 집합을 만든다.
         */
        EntrySet() {
        }

        /**
         * Returns an iterator over the keys that exist now.
         * <p>
         * 지금 있는 키에 대한 반복자를 반환한다.
         *
         * @return the iterator / 반복자
         */
        public Iterator<Map.Entry<String, Object>> iterator() {
            final List<String> keys;
            synchronized (engine) {
                try {
                    keys = Ops.enumerableOwnKeys(object);
                } catch (JsException e) {
                    throw engine.wrap(e);
                }
            }
            return new Iterator<Map.Entry<String, Object>>() {
                /**
                 * The index of the next key.
                 * <p>
                 * 다음 키의 인덱스.
                 */
                private int next;

                /**
                 * The key that was returned last.
                 * <p>
                 * 마지막으로 돌려준 키.
                 */
                private String last;

                public boolean hasNext() {
                    return next < keys.size();
                }

                public Map.Entry<String, Object> next() {
                    if (next >= keys.size()) {
                        throw new NoSuchElementException();
                    }
                    last = keys.get(next++);
                    return new PropertyEntry(last);
                }

                public void remove() {
                    if (last == null) {
                        throw new IllegalStateException();
                    }
                    JsObjectHandle.this.remove(last);
                    last = null;
                }
            };
        }

        /**
         * Returns the number of entries.
         * <p>
         * 항목 수를 반환한다.
         *
         * @return the number / 개수
         */
        public int size() {
            synchronized (engine) {
                try {
                    return Ops.enumerableOwnKeys(object).size();
                } catch (JsException e) {
                    throw engine.wrap(e);
                }
            }
        }
    }

    /**
     * One entry: a property read and written through the handle.
     * <p>
     * 항목 하나: 핸들을 통해 읽고 쓰는 프로퍼티.
     */
    private final class PropertyEntry implements Map.Entry<String, Object> {

        /**
         * The property name.
         * <p>
         * 프로퍼티 이름.
         */
        private final String key;

        /**
         * Creates the entry.
         * <p>
         * 항목을 만든다.
         *
         * @param key the property name / 프로퍼티 이름
         */
        PropertyEntry(String key) {
            this.key = key;
        }

        /**
         * Returns the property name.
         * <p>
         * 프로퍼티 이름을 반환한다.
         *
         * @return the name / 이름
         */
        public String getKey() {
            return key;
        }

        /**
         * Reads the property.
         * <p>
         * 프로퍼티를 읽는다.
         *
         * @return the value / 값
         */
        public Object getValue() {
            return JsObjectHandle.this.get(key);
        }

        /**
         * Assigns the property.
         * <p>
         * 프로퍼티를 대입한다.
         *
         * @param value the new value / 새 값
         * @return the previous value / 이전 값
         */
        public Object setValue(Object value) {
            return JsObjectHandle.this.put(key, value);
        }

        /**
         * Compares with another entry by key and value.
         * <p>
         * 다른 항목과 키와 값으로 비교한다.
         *
         * @param o the other object / 다른 객체
         * @return true if equal / 같으면 true
         */
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return key.equals(e.getKey()) && (getValue() == null ? e.getValue() == null : getValue().equals(e.getValue()));
        }

        /**
         * Returns a hash code from the key and the value.
         * <p>
         * 키와 값으로 해시 코드를 만든다.
         *
         * @return the hash code / 해시 코드
         */
        public int hashCode() {
            Object v = getValue();
            return key.hashCode() ^ (v == null ? 0 : v.hashCode());
        }

        /**
         * Describes the entry as <code>key=value</code>.
         * <p>
         * 항목을 <code>key=value</code> 로 설명한다.
         *
         * @return the description / 설명
         */
        public String toString() {
            return key + "=" + getValue();
        }
    }
}
