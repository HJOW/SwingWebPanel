package org.duckdns.hjow.swingwebpanel.css.selector;

/**
 * Thrown by the selector parser when a selector text is not valid. The engine converts it to a DOM <code>SyntaxError</code>; the style sheet
 * builder uses it to drop the rule, as a browser does.
 * <p>
 * 선택자 텍스트가 올바르지 않을 때 선택자 파서가 던진다. 엔진은 이것을 DOM <code>SyntaxError</code> 로 바꾸고, 스타일 시트 구성기는 브라우저처럼
 * 규칙을 버리는 데 쓴다.
 */
public class SelectorSyntaxException extends Exception {

    /** Serial version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     * <p>
     * 예외를 만든다.
     *
     * @param message what is wrong / 무엇이 잘못되었는지
     */
    public SelectorSyntaxException(String message) {
        super(message);
    }
}
