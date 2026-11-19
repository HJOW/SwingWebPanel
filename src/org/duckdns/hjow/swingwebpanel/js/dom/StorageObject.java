package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;
import org.duckdns.hjow.swingwebpanel.storage.WebStorage;

/**
 * The wrapper of a <code>Storage</code> object (<code>localStorage</code>, <code>sessionStorage</code>) with its named properties: reading
 * <code>storage.name</code> gives the item (unless the name is a member of the interface such as <code>length</code> or
 * <code>getItem</code>), assigning stores the string value (a quota error becomes <code>QuotaExceededError</code>), <code>delete</code>
 * removes the item and the property keys are the item keys (plan item P16-01).
 * <p>
 * <code>Storage</code> 객체(<code>localStorage</code>, <code>sessionStorage</code>)의 래퍼. 이름 있는 프로퍼티를 가진다:
 * <code>storage.name</code> 을 읽으면 항목이 나오고(이름이 <code>length</code>, <code>getItem</code> 같은 인터페이스 멤버가 아니면), 대입하면 문자열 값을
 * 저장하며(쿼터 오류는 <code>QuotaExceededError</code> 가 된다), <code>delete</code> 는 항목을 지우고 프로퍼티 키는 항목 키이다(계획 P16-01).
 */
public final class StorageObject extends HostObject {

    /**
     * The storage behind the wrapper.
     * <p>
     * 래퍼 뒤의 저장소.
     */
    private final WebStorage storage;

    /**
     * Creates the wrapper.
     * <p>
     * 래퍼를 만든다.
     *
     * @param bridge  the bridge / 브리지
     * @param proto   the prototype / 프로토타입
     * @param storage the storage / 저장소
     */
    public StorageObject(DomBridge bridge, JsObject proto, WebStorage storage) {
        super(bridge, proto, storage);
        this.storage = storage;
    }

    /**
     * Tells whether a name is a member of the interface (a property of the prototype chain).
     * <p>
     * 이름이 인터페이스 멤버(프로토타입 사슬의 프로퍼티)인지 알려 준다.
     *
     * @param key the key / 키
     * @return true for members / 멤버이면 true
     */
    private boolean isMember(Object key) {
        JsObject p = getPrototypeOf();
        return p != null && p.hasProperty(key);
    }

    /**
     * Returns an item as a data property unless the key names an own property or an interface member.
     * <p>
     * 키가 자체 프로퍼티나 인터페이스 멤버를 가리키지 않으면 항목을 데이터 프로퍼티로 반환한다.
     *
     * @param key the key / 키
     * @return the property slot or null / 프로퍼티 칸 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        Property own = super.getOwnPropertySlot(key);
        if (own != null || !(key instanceof String) || isMember(key)) {
            return own;
        }
        String v = storage.getItem((String) key);
        return v == null ? null : Property.data(Js.str(v), true, true, true);
    }

    /**
     * Stores an assignment to a non-member name as an item.
     * <p>
     * 멤버가 아닌 이름에 대한 대입을 항목으로 저장한다.
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / 수신자
     * @return true if stored / 저장했으면 true
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        if (key instanceof String && super.getOwnPropertySlot(key) == null && !isMember(key)) {
            JsValue setItem = getPrototypeOf().get("setItem");
            Ops.call(setItem, this, new JsValue[] { Js.str((String) key), Js.str(Ops.toString(value)) });
            return true;
        }
        return super.set(key, value, receiver);
    }

    /**
     * Removes the item of a non-member name.
     * <p>
     * 멤버가 아닌 이름의 항목을 지운다.
     *
     * @param key the key / 키
     * @return true / true
     */
    public boolean delete(Object key) {
        if (key instanceof String && super.getOwnPropertySlot(key) == null && !isMember(key)) {
            storage.removeItem((String) key);
            return true;
        }
        return super.delete(key);
    }

    /**
     * Returns the item keys followed by the ordinary own keys.
     * <p>
     * 항목 키에 이어 일반 자체 키를 반환한다.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        List<Object> keys = new ArrayList<Object>();
        int n = storage.getLength();
        // 저장소의 키를 위치 순서대로 나열한다.
        for (int i = 0; i < n; i++) {
            String k = storage.key(i);
            if (k != null) {
                keys.add(k);
            }
        }
        keys.addAll(super.ownKeys());
        return keys;
    }
}
