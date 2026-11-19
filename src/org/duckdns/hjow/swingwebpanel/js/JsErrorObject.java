package org.duckdns.hjow.swingwebpanel.js;

/**
 * An Error instance (an object with the [[ErrorData]] internal slot). Its builtinTag is "Error" so that
 * Object.prototype.toString reports <code>[object Error]</code>.
 * <p>
 * Error 인스턴스([[ErrorData]] 내부 슬롯을 가진 객체). builtinTag 가 "Error" 이므로 Object.prototype.toString 이
 * <code>[object Error]</code> 를 돌려준다.
 */
public class JsErrorObject extends JsObject {

    /**
     * Creates an error object.
     * <p>
     * 오류 객체를 만든다.
     *
     * @param proto the prototype / 프로토타입
     */
    public JsErrorObject(JsObject proto) {
        super(proto);
    }

    /**
     * Returns "Error".
     * <p>
     * "Error" 를 반환한다.
     *
     * @return the class name / 클래스 이름
     */
    public String className() {
        return "Error";
    }
}
