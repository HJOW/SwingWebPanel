package org.duckdns.hjow.swingwebpanel.js.parser;

/**
 * Hook that lets the parser validate the pattern of a regular expression literal as an early error (ECMA-262 6th edition
 * 12.2.8.1). The regular expression engine is a separate component (<code>org.duckdns.hjow.swingwebpanel.js.regexp</code>); until it is
 * connected through {@link Parser#setDefaultRegExpValidator(RegExpValidator)} only the flags are validated by the lexer.
 * <p>
 * 파서가 정규식 리터럴의 패턴을 조기 오류로 검증하게 하는 연결 지점(ECMA-262 6판 12.2.8.1). 정규식 엔진은 별도 구성요소
 * (<code>org.duckdns.hjow.swingwebpanel.js.regexp</code>)이며, {@link Parser#setDefaultRegExpValidator(RegExpValidator)} 로 연결하기 전에는
 * 렉서가 플래그만 검증한다.
 */
public interface RegExpValidator {

    /**
     * Validates a pattern.
     * <p>
     * 패턴을 검증한다.
     *
     * @param pattern the pattern source / 패턴 소스
     * @param flags   the flags / 플래그
     * @return null if valid, otherwise the SyntaxError message / 올바르면 null, 아니면 SyntaxError 메시지
     */
    String validate(String pattern, String flags);
}
