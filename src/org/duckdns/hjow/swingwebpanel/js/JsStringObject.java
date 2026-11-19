package org.duckdns.hjow.swingwebpanel.js;

import java.util.ArrayList;
import java.util.List;

/**
 * A String exotic object (ECMA-262 6th edition 9.4.3): a wrapper around a primitive string that exposes the code units as
 * read-only enumerable index properties and a read-only "length" property.
 * <p>
 * String 이국적 객체(ECMA-262 6판 9.4.3): 원시 문자열을 감싸며 코드 단위를 읽기 전용 열거 가능 인덱스 프로퍼티로,
 * 길이를 읽기 전용 "length" 프로퍼티로 보여 준다.
 */
public class JsStringObject extends JsObject {

    /**
     * The wrapped string.
     * <p>
     * 감싼 문자열.
     */
    private final String value;

    /**
     * Creates a String wrapper.
     * <p>
     * 문자열 래퍼를 만든다.
     *
     * @param proto the prototype / 프로토타입
     * @param value the wrapped string / 감싼 문자열
     */
    public JsStringObject(JsObject proto, String value) {
        super(proto);
        this.value = value;
    }

    /**
     * Returns the wrapped Java string.
     * <p>
     * 감싼 Java 문자열을 반환한다.
     *
     * @return the string / 문자열
     */
    public String getStringValue() {
        return value;
    }

    /**
     * Returns "String".
     * <p>
     * "String" 을 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "String";
    }

    /**
     * Exposes the index and length properties.
     * <p>
     * 인덱스와 length 프로퍼티를 보여 준다.
     *
     * @param key the key / 키
     * @return the property or null / 프로퍼티 또는 null
     */
    public Property getOwnPropertySlot(Object key) {
        Property own = super.getOwnPropertySlot(key);
        if (own != null) {
            return own;
        }
        if (key instanceof String) {
            String s = (String) key;
            if ("length".equals(s)) {
                return Property.data(JsNumber.of(value.length()), false, false, false);
            }
            long idx = PropertyKeys.arrayIndex(s);
            if (idx >= 0 && idx < value.length()) {
                return Property.data(JsString.of(String.valueOf(value.charAt((int) idx))), false, true, false);
            }
        }
        return null;
    }

    /**
     * Defines a property; the virtual index and length properties can only be redefined compatibly.
     * <p>
     * 프로퍼티를 정의한다. 가상 인덱스와 length 프로퍼티는 호환되는 재정의만 가능하다.
     *
     * @param key  the key / 키
     * @param desc the descriptor / 기술자
     * @return true on success / 성공하면 true
     */
    public boolean defineOwnProperty(Object key, PropertyDescriptor desc) {
        if (super.getOwnPropertySlot(key) == null) {
            Property virtual = getOwnPropertySlot(key);
            if (virtual != null) {
                // 가상 프로퍼티는 복사본에 적용해 호환 여부만 검사한다.
                return validateAndApply(key, desc, virtual);
            }
        }
        return super.defineOwnProperty(key, desc);
    }

    /**
     * Lists the own keys: string indices, other integer indices, "length", other strings, symbols.
     * <p>
     * 자체 키를 나열한다: 문자열 인덱스, 그 밖의 정수 인덱스, "length", 나머지 문자열, 심볼.
     *
     * @return list of keys / 키 목록
     */
    public List<Object> ownKeys() {
        List<Object> base = super.ownKeys();
        List<Object> result = new ArrayList<Object>();
        for (int i = 0; i < value.length(); i++) { // 문자열의 각 코드 단위를 인덱스 키로 만든다.
            result.add(PropertyKeys.fromIndex(i));
        }
        int pos = 0;
        // 정수 인덱스 키가 끝나는 위치를 찾아 그 뒤에 length 를 넣는다.
        while (pos < base.size() && base.get(pos) instanceof String && PropertyKeys.arrayIndex((String) base.get(pos)) >= 0) {
            result.add(base.get(pos));
            pos++;
        }
        result.add("length");
        while (pos < base.size()) { // 나머지 키를 순서대로 옮긴다.
            result.add(base.get(pos));
            pos++;
        }
        return result;
    }
}
