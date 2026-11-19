package org.duckdns.hjow.swingwebpanel.css.syntax;

/**
 * A component value of the CSS Syntax Module: a preserved token, a simple block or a function. Component values are what the parser
 * hands to the selector, value and media query code, which interpret them later.
 * <p>
 * CSS Syntax 모듈의 컴포넌트 값: 보존된 토큰, 단순 블록, 함수. 파서가 선택자, 값, 미디어 질의 코드에 넘기는 단위이며 그쪽에서 나중에 해석한다.
 */
public abstract class ComponentValue {

    /**
     * Serializes the component value back to CSS text following the CSSOM serialization rules.
     * <p>
     * 컴포넌트 값을 CSSOM 직렬화 규칙에 따라 CSS 문자열로 되돌린다.
     *
     * @return CSS text / CSS 문자열
     */
    public abstract String toCssText();

    /**
     * Returns the CSS text.
     * <p>
     * CSS 문자열을 반환한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toString() {
        return toCssText();
    }
}
