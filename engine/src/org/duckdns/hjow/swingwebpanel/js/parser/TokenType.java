package org.duckdns.hjow.swingwebpanel.js.parser;

/**
 * The kinds of tokens produced by the {@link Lexer}.
 * <p>
 * {@link Lexer} 가 만드는 토큰의 종류.
 */
public enum TokenType {
    /**
     * End of the source text.
     * <p>
     * 소스 텍스트의 끝.
     */
    EOF,
    /**
     * An IdentifierName: identifiers and keywords (the parser decides which).
     * <p>
     * IdentifierName: 식별자와 키워드(구분은 파서가 한다).
     */
    NAME,
    /**
     * A punctuator such as <code>{</code>, <code>===</code> or <code>=&gt;</code>.
     * <p>
     * <code>{</code>, <code>===</code>, <code>=&gt;</code> 같은 구두점.
     */
    PUNCT,
    /**
     * A numeric literal.
     * <p>
     * 숫자 리터럴.
     */
    NUMBER,
    /**
     * A string literal.
     * <p>
     * 문자열 리터럴.
     */
    STRING,
    /**
     * A template literal part: the head, a middle part or the tail.
     * <p>
     * 템플릿 리터럴의 한 부분: 머리, 중간 또는 꼬리.
     */
    TEMPLATE,
    /**
     * A regular expression literal.
     * <p>
     * 정규식 리터럴.
     */
    REGEXP
}
