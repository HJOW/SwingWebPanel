package org.duckdns.hjow.swingwebpanel.js.dom;

import org.duckdns.hjow.swingwebpanel.js.ErrorKind;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsErrorObject;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;

/**
 * Creates the JavaScript <code>DOMException</code> objects (name, message and the legacy numeric code). They inherit from the
 * <code>DOMException</code> prototype, which inherits from <code>Error.prototype</code>, so <code>e instanceof Error</code> and
 * <code>e instanceof DOMException</code> are both true.
 * <p>
 * JavaScript <code>DOMException</code> 객체(name, message, 예전 숫자 code)를 만든다. <code>DOMException</code> 프로토타입을 상속하고 그것이
 * <code>Error.prototype</code> 을 상속하므로 <code>e instanceof Error</code> 와 <code>e instanceof DOMException</code> 이 모두 참이다.
 */
public final class DomExceptionObjects {

    /** Intrinsic prototype used when deserializing into a different realm. / 다른 realm에서 역직렬화할 때 쓰는 내장 프로토타입. */
    static final String PROTOTYPE_INTRINSIC = "%DOMException.prototype%";

    /**
     * Names of the legacy error codes, indexed by code (index 0 unused).
     * <p>
     * 예전 오류 코드의 이름. 코드가 위치이다(0 번은 쓰지 않음).
     */
    private static final String[] NAMES = { "", "IndexSizeError", "DOMStringSizeError", "HierarchyRequestError", "WrongDocumentError",
        "InvalidCharacterError", "NoDataAllowedError", "NoModificationAllowedError", "NotFoundError", "NotSupportedError",
        "InUseAttributeError", "InvalidStateError", "SyntaxError", "InvalidModificationError", "NamespaceError", "InvalidAccessError",
        "ValidationError", "TypeMismatchError", "SecurityError", "NetworkError", "AbortError", "URLMismatchError", "QuotaExceededError",
        "TimeoutError", "InvalidNodeTypeError", "DataCloneError" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DomExceptionObjects() {
    }

    /**
     * Returns the legacy numeric code of an error name, or 0.
     * <p>
     * 오류 이름의 예전 숫자 코드를 반환한다. 없으면 0.
     *
     * @param name the error name / 오류 이름
     * @return the code / 코드
     */
    public static int codeOf(String name) {
        // 이름 표를 훑어 위치(=코드)를 찾는다.
        for (int i = 1; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Returns the list of legacy error names (for the constants of the interface object).
     * <p>
     * 예전 오류 이름 목록을 반환한다(인터페이스 객체의 상수용).
     *
     * @return names indexed by code / 코드로 색인한 이름
     */
    public static String[] legacyNames() {
        return NAMES.clone();
    }

    /**
     * Creates a <code>DOMException</code> object.
     * <p>
     * <code>DOMException</code> 객체를 만든다.
     *
     * @param bridge  the bridge of the page / 페이지의 브리지
     * @param name    the error name such as <code>NotFoundError</code> / <code>NotFoundError</code> 같은 오류 이름
     * @param message the message / 메시지
     * @return the object / 객체
     */
    public static JsObject create(DomBridge bridge, String name, String message) {
        return create(bridge.getRealm(), name, message);
    }

    /**
     * Creates a branded DOMException with the target realm's intrinsic prototype.
     * <p>
     * 대상 realm의 내장 프로토타입을 사용하는 내부 브랜드가 있는 DOMException을 만든다.
     *
     * @param realm owning realm / 객체의 realm
     * @param name exception name / 예외 이름
     * @param message exception message / 예외 메시지
     * @return DOMException / DOMException 객체
     */
    public static Value create(JsRealm realm, String name, String message) {
        JsObject proto = realm.getIntrinsic(PROTOTYPE_INTRINSIC);
        return new Value(proto == null ? realm.getErrorPrototype(ErrorKind.ERROR) : proto, name, message == null ? "" : message);
    }

    /**
     * DOMException with the ErrorData brand and its serializable Web IDL values.
     * <p>
     * ErrorData 브랜드와 직렬화할 Web IDL 값을 가진 DOMException.
     */
    public static final class Value extends JsErrorObject {
        /** Stored exception name. / 저장한 예외 이름. */
        private final String domName;

        /** Stored exception message. / 저장한 예외 메시지. */
        private final String domMessage;

        /**
         * Creates the exception and its script-facing fields.
         * <p>
         * 예외와 스크립트에서 사용할 프로퍼티를 만든다.
         *
         * @param proto realm prototype / realm 프로토타입
         * @param name exception name / 예외 이름
         * @param message normalized message / 정규화한 메시지
         */
        private Value(JsObject proto, String name, String message) {
            super(proto);
            domName = name;
            domMessage = message;
            defineData("message", Js.str(message), true, false, true);
            defineData("name", Js.str(name), true, false, true);
            defineData("code", Js.num(codeOf(name)), true, false, true);
        }

        /**
         * Returns the stored exception name for serialization.
         * <p>
         * 직렬화에 쓸 저장된 예외 이름을 반환한다.
         *
         * @return name / 이름
         */
        public String getDomName() {
            return domName;
        }

        /**
         * Returns the stored exception message for serialization.
         * <p>
         * 직렬화에 쓸 저장된 예외 메시지를 반환한다.
         *
         * @return message / 메시지
         */
        public String getDomMessage() {
            return domMessage;
        }
    }
}
