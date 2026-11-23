package org.duckdns.hjow.swingwebpanel.js;

/**
 * A wrapper object for a primitive Boolean, Number or Symbol (<code>new Boolean(true)</code>, <code>Object(1)</code>).
 * It stores the primitive in the [[BooleanData]]/[[NumberData]]/[[SymbolData]] slot. String wrappers use {@link JsStringObject}.
 * <p>
 * 원시 Boolean, Number, Symbol 의 래퍼 객체(<code>new Boolean(true)</code>, <code>Object(1)</code>).
 * 원시 값을 [[BooleanData]]/[[NumberData]]/[[SymbolData]] 슬롯에 보관한다. 문자열 래퍼는 {@link JsStringObject} 를 쓴다.
 */
public class JsPrimitiveObject extends JsObject {

    /**
     * The wrapped primitive value.
     * <p>
     * 감싼 원시 값.
     */
    private final JsValue primitive;

    /**
     * The builtinTag ("Boolean", "Number" or "Symbol").
     * <p>
     * 내장 클래스 태그(Boolean, Number, Symbol 중 하나).
     */
    private final String tag;

    /**
     * Creates a wrapper object.
     * <p>
     * 래퍼 객체를 만든다.
     *
     * @param proto     the prototype / 프로토타입
     * @param primitive the primitive value / 원시 값
     * @param tag       the builtinTag / builtinTag
     */
    public JsPrimitiveObject(JsObject proto, JsValue primitive, String tag) {
        super(proto);
        this.primitive = primitive;
        this.tag = tag;
    }

    /**
     * Returns the wrapped primitive.
     * <p>
     * 감싼 원시 값을 반환한다.
     *
     * @return the primitive / 원시 값
     */
    public JsValue getPrimitive() {
        return primitive;
    }

    /**
     * Returns the builtinTag.
     * <p>
     * builtinTag 를 반환한다.
     *
     * @return the tag / 태그
     */
    public String className() {
        return tag;
    }
}
