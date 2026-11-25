package org.duckdns.hjow.swingwebpanel.js;

import java.util.WeakHashMap;

/**
 * Base class for JavaScript values that may carry WeakMap associations.
 * Associations live with the key, so a value referring back to its key does not keep that key alive through the map.
 * <p>
 * WeakMap 항목을 가질 수 있는 JavaScript 값의 기반 클래스.
 * 항목은 키와 함께 있으므로 값에서 키를 참조해도 맵을 통해 키를 계속 살려 두지 않는다.
 */
public abstract class JsWeakKey extends JsValue {

    /** WeakMap identity tokens and associated values, allocated on first insertion. / 처음 삽입할 때 할당하는 WeakMap 식별 토큰과 값. */
    private WeakHashMap<Object, JsValue> weakAssociations;

    /**
     * Looks up the value for the given WeakMap's private identity token.
     * <p>
     * 주어진 WeakMap의 비공개 식별 토큰에 대응하는 값을 찾는다.
     *
     * @param token the WeakMap's token / WeakMap의 토큰
     * @return the associated value or Java null when absent / 연결된 값. 없으면 Java null
     */
    public final synchronized JsValue getWeakAssociation(Object token) {
        return weakAssociations == null ? null : weakAssociations.get(token);
    }

    /**
     * Associates a value with this key for the given WeakMap.
     * <p>
     * 주어진 WeakMap에서 이 키에 값을 연결한다.
     *
     * @param token the WeakMap's token / WeakMap의 토큰
     * @param value the value to associate / 연결할 값
     */
    public final synchronized void putWeakAssociation(Object token, JsValue value) {
        if (weakAssociations == null) {
            weakAssociations = new WeakHashMap<Object, JsValue>();
        }
        weakAssociations.put(token, value);
    }

    /**
     * Removes the value associated with the given WeakMap.
     * <p>
     * 주어진 WeakMap에 연결한 값을 제거한다.
     *
     * @param token the WeakMap's token / WeakMap의 토큰
     * @return the former value or Java null / 이전 값 또는 Java null
     */
    public final synchronized JsValue removeWeakAssociation(Object token) {
        if (weakAssociations == null) {
            return null;
        }
        JsValue old = weakAssociations.remove(token);
        if (weakAssociations.isEmpty()) {
            weakAssociations = null;
        }
        return old;
    }
}
