package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor;
import org.duckdns.hjow.swingwebpanel.js.PropertyKeys;

/**
 * A wrapper for list-like host objects (<code>NodeList</code>, <code>HTMLCollection</code>, <code>NamedNodeMap</code>,
 * <code>DOMTokenList</code>): integer keys read the live list, an optional named lookup serves keys such as element ids, and everything else
 * is an ordinary property (including the prototype's <code>length</code> and <code>item</code>). Indexed properties are read-only, enumerable
 * and configurable as WebIDL requires; writes and deletes of them are ignored.
 * <p>
 * 목록 형태의 호스트 객체(<code>NodeList</code>, <code>HTMLCollection</code>, <code>NamedNodeMap</code>, <code>DOMTokenList</code>)의 래퍼:
 * 정수 키는 살아 있는 목록을 읽고, 선택적인 이름 조회가 요소 id 같은 키를 처리하며, 나머지는 일반 프로퍼티(프로토타입의 <code>length</code> 와
 * <code>item</code> 포함)이다. 인덱스 프로퍼티는 WebIDL 요구대로 읽기 전용, 열거 가능, 설정 가능이며 쓰기와 삭제는 무시한다.
 */
public final class IndexedObject extends HostObject {

    /**
     * How to read the items of one kind of list.
     * <p>
     * 한 종류의 목록에서 항목을 읽는 방법.
     */
    public interface Source {
        /**
         * Returns the number of items.
         * <p>
         * 항목 수를 반환한다.
         *
         * @param target the Java list / Java 목록
         * @return length / 길이
         */
        int length(Object target);

        /**
         * Returns an item (a Java object that the bridge wraps, or a String).
         * <p>
         * 항목을 반환한다(브리지가 래핑하는 Java 객체 또는 String).
         *
         * @param target the Java list / Java 목록
         * @param index  the index, 0 or more and below the length / 위치(0 이상, 길이 미만)
         * @return the item / 항목
         */
        Object item(Object target, int index);

        /**
         * Returns the item with a name, or null if the list has no named access or no such item.
         * <p>
         * 이름으로 항목을 찾는다. 이름 접근이 없거나 항목이 없으면 null.
         *
         * @param target the Java list / Java 목록
         * @param name   the name / 이름
         * @return the item or null / 항목 또는 null
         */
        Object named(Object target, String name);
    }

    /**
     * The reader of this list.
     * <p>
     * 이 목록의 읽기 방법.
     */
    private final Source source;

    /**
     * Creates the wrapper.
     * <p>
     * 래퍼를 만든다.
     *
     * @param bridge the owning bridge / 소유한 브리지
     * @param proto  prototype / 프로토타입
     * @param target the Java list / Java 목록
     * @param source how to read it / 읽는 방법
     */
    public IndexedObject(DomBridge bridge, JsObject proto, Object target, Source source) {
        super(bridge, proto, target);
        this.source = source;
    }

    /**
     * Returns the index that a key stands for, or -1.
     * <p>
     * 키가 가리키는 위치를 반환한다. 아니면 -1.
     *
     * @param key the property key / 프로퍼티 키
     * @return the index below the current length, or -1 / 현재 길이 미만의 위치. 아니면 -1
     */
    private int indexOf(Object key) {
        if (!(key instanceof String)) {
            return -1;
        }
        long idx = PropertyKeys.arrayIndex((String) key);
        return idx >= 0 && idx < source.length(getTarget()) ? (int) idx : -1;
    }

    /**
     * Returns the own property of an index or a name, otherwise the ordinary own property.
     * <p>
     * 위치나 이름의 자체 프로퍼티를 반환하고 그 밖에는 일반 자체 프로퍼티를 반환한다.
     *
     * @param key the key / 키
     * @return the property slot or null / 프로퍼티 칸 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        int i = indexOf(key);
        if (i >= 0) {
            return Property.data(getBridge().toJs(source.item(getTarget(), i)), false, true, true);
        }
        Property own = super.getOwnPropertySlot(key);
        if (own == null && key instanceof String && PropertyKeys.arrayIndex((String) key) < 0) {
            Object named = source.named(getTarget(), (String) key);
            if (named != null) {
                return Property.data(getBridge().toJs(named), false, false, true);
            }
        }
        return own;
    }

    /**
     * Returns the own property descriptor (see {@link #getOwnPropertySlot(Object)}).
     * <p>
     * 자체 프로퍼티 기술자를 반환한다({@link #getOwnPropertySlot(Object)} 참고).
     *
     * @param key the key / 키
     * @return the descriptor or null / 기술자 또는 null
     */
    public PropertyDescriptor getOwnProperty(Object key) {
        Property p = getOwnPropertySlot(key);
        return p == null ? null : p.toDescriptor();
    }

    /**
     * Ignores writes to index keys (read-only), otherwise sets normally.
     * <p>
     * 위치 키에 대한 쓰기는 무시하고(읽기 전용) 그 밖에는 일반적으로 설정한다.
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / 수신자
     * @return false if refused / 거부되면 false
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        if (indexOf(key) >= 0) {
            return false;
        }
        return super.set(key, value, receiver);
    }

    /**
     * Refuses to delete an index key.
     * <p>
     * 위치 키는 삭제하지 않는다.
     *
     * @param key the key / 키
     * @return false for index keys / 위치 키이면 false
     */
    public boolean delete(Object key) {
        if (indexOf(key) >= 0) {
            return false;
        }
        return super.delete(key);
    }

    /**
     * Returns the index keys first, then the ordinary own keys.
     * <p>
     * 위치 키를 먼저, 그 다음 일반 자체 키를 반환한다.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        int n = source.length(getTarget());
        List<Object> keys = new ArrayList<Object>(n);
        // 목록의 위치를 문자열 키로 나열한다.
        for (int i = 0; i < n; i++) {
            keys.add(PropertyKeys.fromIndex(i));
        }
        keys.addAll(super.ownKeys());
        return keys;
    }
}
