package org.duckdns.hjow.swingwebpanel.dom.xpath;

/**
 * The error of compiling or evaluating an XPath expression: a syntax error (the text is not an XPath 1.0 expression, an unknown function, a wrong number of arguments),
 * a namespace error (a prefix that the resolver cannot resolve) or a type error (a variable, a value that is not a node set where one is needed).
 * <p>
 * XPath 식을 컴파일하거나 평가할 때의 오류: 구문 오류(XPath 1.0 식이 아님, 알 수 없는 함수, 잘못된 인수 개수), 네임스페이스 오류(리졸버가 해석하지 못하는 접두사),
 * 형식 오류(변수, 노드 집합이 필요한 곳의 다른 값).
 */
public final class XPathException extends RuntimeException {

    /**
     * Serialization version.
     * <p>
     * 직렬화 버전.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The kind: the text is not an expression.
     * <p>
     * 종류: 식이 아니다.
     */
    public static final int SYNTAX = 0;

    /**
     * The kind: a prefix cannot be resolved.
     * <p>
     * 종류: 접두사를 해석할 수 없다.
     */
    public static final int NAMESPACE = 1;

    /**
     * The kind: a value has the wrong type.
     * <p>
     * 종류: 값의 형식이 잘못되었다.
     */
    public static final int TYPE = 2;

    /**
     * The kind of the error.
     * <p>
     * 오류의 종류.
     */
    private final int kind;

    /**
     * Creates an error.
     * <p>
     * 오류를 만든다.
     *
     * @param kind    {@link #SYNTAX}, {@link #NAMESPACE} or {@link #TYPE} / {@link #SYNTAX}, {@link #NAMESPACE}, {@link #TYPE}
     * @param message the message / 메시지
     */
    public XPathException(int kind, String message) {
        super(message);
        this.kind = kind;
    }

    /**
     * Returns the kind of the error.
     * <p>
     * 오류의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public int getKind() {
        return kind;
    }
}
