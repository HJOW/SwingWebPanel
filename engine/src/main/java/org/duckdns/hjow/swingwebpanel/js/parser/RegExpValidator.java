package org.duckdns.hjow.swingwebpanel.js.parser;

/**
 * Validates regular expression literal patterns during parsing (ECMA-262 6th edition 12.2.8.1).
 * The parser uses the regular expression compiler by default and allows a replacement via
 * {@link Parser#setDefaultRegExpValidator(RegExpValidator)}.
 * <p>
 * 파싱할 때 정규식 리터럴 패턴을 검증한다(ECMA-262 6판 12.2.8.1). 기본은 정규식 컴파일러이며,
 * {@link Parser#setDefaultRegExpValidator(RegExpValidator)} 로 교체할 수 있다.
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
