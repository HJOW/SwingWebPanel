package org.duckdns.hjow.swingwebpanel.js.interp;

import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * A completion record of statement execution (ECMA-262 6th edition 6.2.2): normal, break, continue or return, with an
 * optional value and a label target. A <em>throw</em> completion is represented by the Java exception
 * {@link org.duckdns.hjow.swingwebpanel.js.JsException}; the interpreter keeps every abrupt completion explicit, so <code>finally</code>
 * and nested loops preserve and override completions exactly as specified.
 * <p>
 * 문장 실행의 완료 레코드(ECMA-262 6판 6.2.2): normal, break, continue, return 이며 선택적인 값과 레이블 대상을 가진다.
 * <em>throw</em> 완료는 Java 예외 {@link org.duckdns.hjow.swingwebpanel.js.JsException} 으로 나타낸다. 인터프리터는 모든 비정상 완료를 명시적으로
 * 유지하므로 <code>finally</code> 와 중첩 반복문이 규격대로 완료를 보존하거나 덮어쓴다.
 */
final class Completion {

    /**
     * Type: normal completion.
     * <p>
     * 종류: 정상 완료.
     */
    static final int NORMAL = 0;

    /**
     * Type: break.
     * <p>
     * 종류: break.
     */
    static final int BREAK = 1;

    /**
     * Type: continue.
     * <p>
     * 종류: continue.
     */
    static final int CONTINUE = 2;

    /**
     * Type: return.
     * <p>
     * 종류: return.
     */
    static final int RETURN = 3;

    /**
     * The shared normal completion without a value (empty).
     * <p>
     * 값이 없는(empty) 공유 정상 완료.
     */
    static final Completion EMPTY = new Completion(NORMAL, null, null);

    /**
     * The completion type.
     * <p>
     * 완료 종류.
     */
    final int type;

    /**
     * The completion value, or null for empty.
     * <p>
     * 완료 값. 비어 있으면 null.
     */
    final JsValue value;

    /**
     * The label of a break or continue, or null.
     * <p>
     * break 나 continue 의 레이블. 없으면 null.
     */
    final String target;

    /**
     * Creates a completion.
     * <p>
     * 완료를 만든다.
     *
     * @param type   the type / 종류
     * @param value  the value or null / 값 또는 null
     * @param target the label or null / 레이블 또는 null
     */
    Completion(int type, JsValue value, String target) {
        this.type = type;
        this.value = value;
        this.target = target;
    }

    /**
     * Returns a normal completion with a value.
     * <p>
     * 값이 있는 정상 완료를 반환한다.
     *
     * @param v the value or null for empty / 값. empty 이면 null
     * @return the completion / 완료
     */
    static Completion normal(JsValue v) {
        return v == null ? EMPTY : new Completion(NORMAL, v, null);
    }

    /**
     * Returns the same completion with the value replaced when it is empty (UpdateEmpty).
     * <p>
     * 값이 비어 있으면 그 값을 채운 같은 완료를 반환한다(UpdateEmpty).
     *
     * @param c the completion / 완료
     * @param v the value to use if the completion is empty / 완료가 비어 있을 때 쓸 값
     * @return the updated completion / 갱신된 완료
     */
    static Completion updateEmpty(Completion c, JsValue v) {
        if (c.value != null || v == null) {
            return c;
        }
        return new Completion(c.type, v, c.target);
    }
}
