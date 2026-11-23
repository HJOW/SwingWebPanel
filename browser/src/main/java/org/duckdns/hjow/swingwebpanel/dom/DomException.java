package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The DOM exception (DOMException of the DOM Standard). Every DOM operation that fails by specification throws it, with the standard
 * error name (for example <code>HierarchyRequestError</code>) and the legacy numeric code. The JavaScript layer converts it into a
 * <code>DOMException</code> object with the same name, code and message.
 * <p>
 * DOM 예외(DOM 표준의 DOMException). 규격에 따라 실패하는 모든 DOM 연산이 표준 오류 이름(예: <code>HierarchyRequestError</code>)과
 * 옛 숫자 코드와 함께 이 예외를 던진다. JavaScript 계층은 같은 이름, 코드, 메시지의 <code>DOMException</code> 객체로 바꾼다.
 */
public class DomException extends RuntimeException {

    /**
     * Serialization version id.
     * <p>
     * 직렬화 버전 식별자.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Legacy code of IndexSizeError.
     * <p>
     * IndexSizeError 의 옛 코드.
     */
    public static final short INDEX_SIZE_ERR = 1;

    /**
     * Legacy code of HierarchyRequestError.
     * <p>
     * HierarchyRequestError 의 옛 코드.
     */
    public static final short HIERARCHY_REQUEST_ERR = 3;

    /**
     * Legacy code of WrongDocumentError.
     * <p>
     * WrongDocumentError 의 옛 코드.
     */
    public static final short WRONG_DOCUMENT_ERR = 4;

    /**
     * Legacy code of InvalidCharacterError.
     * <p>
     * InvalidCharacterError 의 옛 코드.
     */
    public static final short INVALID_CHARACTER_ERR = 5;

    /**
     * Legacy code of NoModificationAllowedError.
     * <p>
     * NoModificationAllowedError 의 옛 코드.
     */
    public static final short NO_MODIFICATION_ALLOWED_ERR = 7;

    /**
     * Legacy code of NotFoundError.
     * <p>
     * NotFoundError 의 옛 코드.
     */
    public static final short NOT_FOUND_ERR = 8;

    /**
     * Legacy code of NotSupportedError.
     * <p>
     * NotSupportedError 의 옛 코드.
     */
    public static final short NOT_SUPPORTED_ERR = 9;

    /**
     * Legacy code of InUseAttributeError.
     * <p>
     * InUseAttributeError 의 옛 코드.
     */
    public static final short INUSE_ATTRIBUTE_ERR = 10;

    /**
     * Legacy code of InvalidStateError.
     * <p>
     * InvalidStateError 의 옛 코드.
     */
    public static final short INVALID_STATE_ERR = 11;

    /**
     * Legacy code of SyntaxError.
     * <p>
     * SyntaxError 의 옛 코드.
     */
    public static final short SYNTAX_ERR = 12;

    /**
     * Legacy code of InvalidModificationError.
     * <p>
     * InvalidModificationError 의 옛 코드.
     */
    public static final short INVALID_MODIFICATION_ERR = 13;

    /**
     * Legacy code of NamespaceError.
     * <p>
     * NamespaceError 의 옛 코드.
     */
    public static final short NAMESPACE_ERR = 14;

    /**
     * Legacy code of InvalidAccessError.
     * <p>
     * InvalidAccessError 의 옛 코드.
     */
    public static final short INVALID_ACCESS_ERR = 15;

    /**
     * Legacy code of SecurityError.
     * <p>
     * SecurityError 의 옛 코드.
     */
    public static final short SECURITY_ERR = 18;

    /**
     * Legacy code of NetworkError.
     * <p>
     * NetworkError 의 옛 코드.
     */
    public static final short NETWORK_ERR = 19;

    /**
     * Legacy code of AbortError.
     * <p>
     * AbortError 의 옛 코드.
     */
    public static final short ABORT_ERR = 20;

    /**
     * Legacy code of QuotaExceededError.
     * <p>
     * QuotaExceededError 의 옛 코드.
     */
    public static final short QUOTA_EXCEEDED_ERR = 22;

    /**
     * Legacy code of TimeoutError.
     * <p>
     * TimeoutError 의 옛 코드.
     */
    public static final short TIMEOUT_ERR = 23;

    /**
     * Legacy code of InvalidNodeTypeError.
     * <p>
     * InvalidNodeTypeError 의 옛 코드.
     */
    public static final short INVALID_NODE_TYPE_ERR = 24;

    /**
     * Legacy code of DataCloneError.
     * <p>
     * DataCloneError 의 옛 코드.
     */
    public static final short DATA_CLONE_ERR = 25;

    /**
     * Standard error name such as <code>NotFoundError</code>.
     * <p>
     * <code>NotFoundError</code> 같은 표준 오류 이름.
     */
    private final String name;

    /**
     * Legacy numeric code, or 0 if the error has none.
     * <p>
     * 옛 숫자 코드. 없으면 0.
     */
    private final short code;

    /**
     * Creates a DOM exception.
     * <p>
     * DOM 예외를 만든다.
     *
     * @param name    standard error name / 표준 오류 이름
     * @param code    legacy code or 0 / 옛 코드 또는 0
     * @param message human readable message / 사람이 읽는 메시지
     */
    public DomException(String name, short code, String message) {
        super(name + ": " + message);
        this.name = name;
        this.code = code;
    }

    /**
     * Returns the standard error name.
     * <p>
     * 표준 오류 이름을 반환한다.
     *
     * @return name / 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the legacy numeric code.
     * <p>
     * 옛 숫자 코드를 반환한다.
     *
     * @return code or 0 / 코드 또는 0
     */
    public short getCode() {
        return code;
    }

    /**
     * Creates a HierarchyRequestError.
     * <p>
     * HierarchyRequestError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException hierarchyRequest(String message) {
        return new DomException("HierarchyRequestError", HIERARCHY_REQUEST_ERR, message);
    }

    /**
     * Creates a NotFoundError.
     * <p>
     * NotFoundError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException notFound(String message) {
        return new DomException("NotFoundError", NOT_FOUND_ERR, message);
    }

    /**
     * Creates an InvalidCharacterError.
     * <p>
     * InvalidCharacterError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException invalidCharacter(String message) {
        return new DomException("InvalidCharacterError", INVALID_CHARACTER_ERR, message);
    }

    /**
     * Creates a NotSupportedError.
     * <p>
     * NotSupportedError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException notSupported(String message) {
        return new DomException("NotSupportedError", NOT_SUPPORTED_ERR, message);
    }

    /**
     * Creates an IndexSizeError.
     * <p>
     * IndexSizeError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException indexSize(String message) {
        return new DomException("IndexSizeError", INDEX_SIZE_ERR, message);
    }

    /**
     * Creates a NamespaceError.
     * <p>
     * NamespaceError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException namespace(String message) {
        return new DomException("NamespaceError", NAMESPACE_ERR, message);
    }

    /**
     * Creates a SyntaxError (for example for an invalid selector or token).
     * <p>
     * SyntaxError 를 만든다(예: 잘못된 선택자나 토큰).
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException syntax(String message) {
        return new DomException("SyntaxError", SYNTAX_ERR, message);
    }

    /**
     * Creates an InvalidStateError.
     * <p>
     * InvalidStateError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException invalidState(String message) {
        return new DomException("InvalidStateError", INVALID_STATE_ERR, message);
    }

    /**
     * Creates a SecurityError.
     * <p>
     * SecurityError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException security(String message) {
        return new DomException("SecurityError", SECURITY_ERR, message);
    }

    /**
     * Creates an InUseAttributeError.
     * <p>
     * InUseAttributeError 를 만든다.
     *
     * @param message message / 메시지
     * @return the exception / 예외
     */
    public static DomException inUseAttribute(String message) {
        return new DomException("InUseAttributeError", INUSE_ATTRIBUTE_ERR, message);
    }
}
