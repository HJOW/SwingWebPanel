package org.duckdns.hjow.swingwebpanel.js.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.Property;

/**
 * The <code>dataset</code> object of an element (<code>DOMStringMap</code>): a property <code>fooBar</code> stands for the content attribute
 * <code>data-foo-bar</code>. Reading gives the attribute value, assigning stores a string, <code>delete</code> removes the attribute and the
 * property keys are the converted attribute names.
 * <p>
 * 요소의 <code>dataset</code> 객체(<code>DOMStringMap</code>): 프로퍼티 <code>fooBar</code> 는 콘텐츠 속성 <code>data-foo-bar</code> 를 나타낸다.
 * 읽으면 속성 값이 나오고, 대입하면 문자열을 저장하며, <code>delete</code> 는 속성을 지우고, 프로퍼티 키는 변환한 속성 이름이다.
 */
public final class DatasetObject extends JsObject {

    /**
     * The element.
     * <p>
     * 요소.
     */
    private final Element element;

    /**
     * Creates the object.
     * <p>
     * 객체를 만든다.
     *
     * @param proto   the prototype / 프로토타입
     * @param element the element / 요소
     */
    public DatasetObject(JsObject proto, Element element) {
        super(proto);
        this.element = element;
    }

    /**
     * Converts a property name to the attribute name (<code>fooBar</code> to <code>data-foo-bar</code>), or null if the name has a dash
     * followed by a lower-case letter (such names cannot be written).
     * <p>
     * 프로퍼티 이름을 속성 이름으로 바꾼다(<code>fooBar</code> 를 <code>data-foo-bar</code> 로). 대시 뒤에 소문자가 오는 이름은 쓸 수 없으므로 null.
     *
     * @param prop the property name / 프로퍼티 이름
     * @return the attribute name or null / 속성 이름 또는 null
     */
    static String attributeName(String prop) {
        StringBuilder sb = new StringBuilder("data-");
        // 대문자는 대시와 소문자로 바꾸고, 대시 뒤 소문자는 거부한다.
        for (int i = 0; i < prop.length(); i++) {
            char c = prop.charAt(i);
            if (c == '-' && i + 1 < prop.length() && Character.isLowerCase(prop.charAt(i + 1))) {
                return null;
            }
            if (Character.isUpperCase(c)) {
                sb.append('-').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Converts an attribute name to the property name, or null if it is not a <code>data-</code> attribute.
     * <p>
     * 속성 이름을 프로퍼티 이름으로 바꾼다. <code>data-</code> 속성이 아니면 null.
     *
     * @param attr the attribute name / 속성 이름
     * @return the property name or null / 프로퍼티 이름 또는 null
     */
    static String propertyName(String attr) {
        if (!attr.startsWith("data-") || attr.length() == 5) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 대시 뒤 소문자를 대문자로 바꾼다.
        for (int i = 5; i < attr.length(); i++) {
            char c = attr.charAt(i);
            if (c == '-' && i + 1 < attr.length() && Character.isLowerCase(attr.charAt(i + 1))) {
                sb.append(Character.toUpperCase(attr.charAt(++i)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the attribute as a data property.
     * <p>
     * 속성을 데이터 프로퍼티로 반환한다.
     *
     * @param key the key / 키
     * @return the property slot or null / 프로퍼티 칸 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        Property own = super.getOwnPropertySlot(key);
        if (own != null || !(key instanceof String)) {
            return own;
        }
        String attr = attributeName((String) key);
        String v = attr == null ? null : element.getAttribute(attr);
        return v == null ? null : Property.data(Js.str(v), true, true, true);
    }

    /**
     * Stores an assignment as an attribute; names with a dash and a lower-case letter throw a SyntaxError.
     * <p>
     * 대입을 속성으로 저장한다. 대시와 소문자가 이어진 이름은 SyntaxError 를 던진다.
     *
     * @param key      the key / 키
     * @param value    the value / 값
     * @param receiver the receiver / 수신자
     * @return true / true
     */
    public boolean set(Object key, JsValue value, JsValue receiver) {
        if (!(key instanceof String)) {
            return super.set(key, value, receiver);
        }
        String attr = attributeName((String) key);
        if (attr == null) {
            throw JsException.syntaxError("'" + key + "' is not a valid property name for dataset");
        }
        element.setAttribute(attr, Ops.toString(value));
        return true;
    }

    /**
     * Removes the attribute.
     * <p>
     * 속성을 제거한다.
     *
     * @param key the key / 키
     * @return true / true
     */
    public boolean delete(Object key) {
        if (key instanceof String) {
            String attr = attributeName((String) key);
            if (attr != null) {
                element.removeAttribute(attr);
                return true;
            }
        }
        return super.delete(key);
    }

    /**
     * Returns the converted names of the <code>data-</code> attributes.
     * <p>
     * <code>data-</code> 속성의 변환한 이름을 반환한다.
     *
     * @return the keys / 키 목록
     */
    public List<Object> ownKeys() {
        List<Object> keys = new ArrayList<Object>();
        List<String> names = element.getAttributeNames();
        // 속성 이름 중 data- 로 시작하는 것을 프로퍼티 이름으로 바꿔 모은다.
        for (int i = 0; i < names.size(); i++) {
            String p = propertyName(names.get(i));
            if (p != null) {
                keys.add(p);
            }
        }
        keys.addAll(super.ownKeys());
        return keys;
    }
}
