package org.duckdns.hjow.swingwebpanel.js.script;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import org.duckdns.hjow.swingwebpanel.js.embed.JsEngine;
import org.duckdns.hjow.swingwebpanel.js.embed.JsObjectHandle;

/**
 * The <code>ENGINE_SCOPE</code> bindings of the default context of a {@link JsScriptEngine}: a live view of the global object of the engine.
 * <code>engine.put("x", 1)</code> is <code>globalThis.x = 1</code>, and a <code>var</code>, function declaration or assignment of a script is visible through
 * <code>engine.get("x")</code>. Values follow the conversion rules of {@link JsEngine} with one difference that fits the Java Scripting API:
 * a script value <code>undefined</code> reads as <code>null</code>. The keys follow the contract of {@link Bindings}: a null key is a
 * <code>NullPointerException</code>, a key that is not a string is a <code>ClassCastException</code>, an empty key is an
 * <code>IllegalArgumentException</code>. The enumerable global variables are the entries; the built-in objects are readable by name but are not
 * entries. A global declared with <code>var</code> cannot be deleted from a JavaScript global object, so <code>remove</code> sets it to undefined.
 * <p>
 * {@link JsScriptEngine} 의 기본 컨텍스트의 <code>ENGINE_SCOPE</code> 바인딩: 엔진의 전역 객체에 대한 살아 있는 보기. <code>engine.put("x", 1)</code> 은
 * <code>globalThis.x = 1</code> 이며, 스크립트의 <code>var</code>, 함수 선언, 대입은 <code>engine.get("x")</code> 로 보인다. 값은 {@link JsEngine} 의 변환 규칙을
 * 따르되 Java Scripting API 에 맞춘 차이 하나가 있다: 스크립트 값 <code>undefined</code> 는 <code>null</code> 로 읽힌다. 키는 {@link Bindings} 의 규약을
 * 따른다: null 키는 <code>NullPointerException</code>, 문자열이 아닌 키는 <code>ClassCastException</code>, 빈 키는 <code>IllegalArgumentException</code>. 항목은 열거 가능한
 * 전역 변수이며 내장 객체는 이름으로 읽을 수 있지만 항목은 아니다. <code>var</code> 로 선언한 전역은 JavaScript 전역 객체에서 삭제할 수 없으므로
 * <code>remove</code> 는 그것을 undefined 로 만든다.
 */
public final class JsGlobalBindings extends AbstractMap<String, Object> implements Bindings {

    /**
     * The engine whose global object this is.
     * <p>
     * 전역 객체를 가진 엔진.
     */
    private final JsEngine engine;

    /**
     * The global object as a map.
     * <p>
     * 맵으로 본 전역 객체.
     */
    private final JsObjectHandle global;

    /**
     * Creates the bindings of an engine.
     * <p>
     * 엔진의 바인딩을 만든다.
     *
     * @param engine the engine / 엔진
     */
    JsGlobalBindings(JsEngine engine) {
        this.engine = engine;
        this.global = engine.getGlobalObject();
    }

    /**
     * Returns the engine whose global object these bindings show.
     * <p>
     * 이 바인딩이 보여 주는 전역 객체를 가진 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    public JsEngine getEngine() {
        return engine;
    }

    /**
     * Checks a key by the contract of {@link Bindings}.
     * <p>
     * {@link Bindings} 의 규약대로 키를 검사한다.
     *
     * @param key the key / 키
     * @return the key as a string / 문자열인 키
     */
    static String checkKey(Object key) {
        if (key == null) {
            throw new NullPointerException("key can not be null");
        }
        if (!(key instanceof String)) {
            throw new ClassCastException("key should be a String");
        }
        if (((String) key).length() == 0) {
            throw new IllegalArgumentException("key can not be empty");
        }
        return (String) key;
    }

    /**
     * Turns the Java value of an undefined script value into null.
     * <p>
     * undefined 스크립트 값의 Java 값을 null 로 바꾼다.
     *
     * @param value the value / 값
     * @return the value, null for undefined / 값. undefined 이면 null
     */
    static Object visible(Object value) {
        return value == JsEngine.UNDEFINED ? null : value;
    }

    /**
     * Reads a global variable.
     * <p>
     * 전역 변수를 읽는다.
     *
     * @param key the name / 이름
     * @return the value, null if there is none or it is undefined / 값. 없거나 undefined 이면 null
     */
    public Object get(Object key) {
        return visible(global.get(checkKey(key)));
    }

    /**
     * Tells whether a global variable exists.
     * <p>
     * 전역 변수가 있는지 알려 준다.
     *
     * @param key the name / 이름
     * @return true if it exists / 있으면 true
     */
    public boolean containsKey(Object key) {
        return global.containsKey(checkKey(key));
    }

    /**
     * Assigns a global variable.
     * <p>
     * 전역 변수에 대입한다.
     *
     * @param name  the name / 이름
     * @param value the value (converted by the rules of {@link JsEngine}) / 값({@link JsEngine} 의 규칙대로 변환)
     * @return the previous value or null / 이전 값. 없으면 null
     */
    public Object put(String name, Object value) {
        return visible(global.put(checkKey(name), value));
    }

    /**
     * Deletes a global variable (or sets it to undefined when it cannot be deleted).
     * <p>
     * 전역 변수를 삭제한다(삭제할 수 없으면 undefined 로 만든다).
     *
     * @param key the name / 이름
     * @return the previous value or null / 이전 값. 없으면 null
     */
    public Object remove(Object key) {
        return visible(global.remove(checkKey(key)));
    }

    /**
     * Copies all entries of a map into the global object.
     * <p>
     * 맵의 모든 항목을 전역 객체로 복사한다.
     *
     * @param toMerge the entries / 항목
     */
    public void putAll(Map<? extends String, ? extends Object> toMerge) {
        if (toMerge == null) {
            throw new NullPointerException("toMerge map is null");
        }
        // 항목마다 키를 검사하고 대입한다.
        for (Map.Entry<? extends String, ? extends Object> e : toMerge.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * Returns the entries (the enumerable global variables) as a live view.
     * <p>
     * 항목(열거 가능한 전역 변수)을 살아 있는 보기로 반환한다.
     *
     * @return the entries / 항목
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        final Set<Map.Entry<String, Object>> inner = global.entrySet();
        return new AbstractSet<Map.Entry<String, Object>>() {
            public Iterator<Map.Entry<String, Object>> iterator() {
                final Iterator<Map.Entry<String, Object>> it = inner.iterator();
                return new Iterator<Map.Entry<String, Object>>() {
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    public Map.Entry<String, Object> next() {
                        final Map.Entry<String, Object> e = it.next();
                        return new Map.Entry<String, Object>() {
                            public String getKey() {
                                return e.getKey();
                            }

                            public Object getValue() {
                                return visible(e.getValue());
                            }

                            public Object setValue(Object value) {
                                return visible(e.setValue(value));
                            }

                            public boolean equals(Object o) {
                                if (!(o instanceof Map.Entry)) {
                                    return false;
                                }
                                Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
                                Object mine = getValue();
                                return getKey().equals(other.getKey()) && (mine == null ? other.getValue() == null : mine.equals(other.getValue()));
                            }

                            public int hashCode() {
                                Object mine = getValue();
                                return getKey().hashCode() ^ (mine == null ? 0 : mine.hashCode());
                            }
                        };
                    }

                    public void remove() {
                        it.remove();
                    }
                };
            }

            public int size() {
                return inner.size();
            }
        };
    }
}
