package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;

/**
 * Text of list markers for the predefined counter styles by name (plan item P09-03): a thin layer over {@link CounterStyles}, which holds the counter
 * styles themselves (bullets, decimal, alphabetic, roman, Greek and the others, and the <code>@counter-style</code> definitions of a document). A
 * name that is not a predefined style is written in decimal.
 * <p>
 * 미리 정의된 카운터 스타일의 이름에 대한 목록 표식의 텍스트(계획 P09-03): 카운터 스타일 자체(글머리 기호, 십진수, 알파벳, 로마 숫자, 그리스 문자 등과 문서의
 * <code>@counter-style</code> 정의)를 가진 {@link CounterStyles} 위의 얇은 계층. 미리 정의되지 않은 이름은 십진수로 쓴다.
 */
final class ListMarkers {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ListMarkers() {
    }

    /**
     * Returns the marker text including the prefix and the suffix (the trailing space or the full stop), or null for <code>none</code>.
     * <p>
     * 접두사와 접미사(뒤의 공백이나 마침표)를 포함한 표식 텍스트를 반환한다. <code>none</code> 이면 null.
     *
     * @param type    the value of <code>list-style-type</code> / <code>list-style-type</code> 값
     * @param ordinal the item number / 항목 번호
     * @return marker text or null / 표식 텍스트 또는 null
     */
    static String text(String type, int ordinal) {
        if (type == null) {
            return null;
        }
        return CounterStyles.DEFAULT.marker(new CssKeyword(type), ordinal);
    }

    /**
     * Returns the number of a counter in a counter style without the punctuation of a list marker (<code>counter()</code> of
     * <code>content</code>, plan item P10-04): <code>decimal</code> gives <code>3</code>, <code>lower-roman</code> gives
     * <code>iii</code>, a bullet style gives the bullet, <code>none</code> gives the empty string.
     * <p>
     * 카운터의 번호를 목록 표식의 구두점 없이 카운터 스타일로 반환한다(<code>content</code> 의 <code>counter()</code>, 계획 P10-04): <code>decimal</code> 은
     * <code>3</code>, <code>lower-roman</code> 은 <code>iii</code>, 글머리 기호 스타일은 그 기호, <code>none</code> 은 빈 문자열.
     *
     * @param type the counter style / 카운터 스타일
     * @param n    the value / 값
     * @return the text / 텍스트
     */
    static String numeral(String type, int n) {
        return CounterStyles.DEFAULT.number(type == null ? null : new CssKeyword(type), n);
    }
}
