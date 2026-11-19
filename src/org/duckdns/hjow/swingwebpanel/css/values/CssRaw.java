package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;

/**
 * A value kept as component values without further interpretation: the value of a custom property, or the value of a property whose syntax
 * is only checked loosely (<code>grid-template-areas</code>, <code>src</code> of <code>@font-face</code> ...). It serializes back to the
 * written text.
 * <p>
 * 더 해석하지 않고 컴포넌트 값 그대로 보관하는 값: 사용자 정의 속성의 값, 또는 문법을 느슨하게만 검사하는 속성의 값(<code>grid-template-areas</code>,
 * <code>@font-face</code> 의 <code>src</code> 등). 쓰인 문자열로 다시 직렬화된다.
 */
public final class CssRaw extends CssValue {

    /** The component values. / 컴포넌트 값. */
    private final List<ComponentValue> tokens;

    /**
     * Creates a raw value.
     * <p>
     * 원본 값을 만든다.
     *
     * @param tokens component values (copied) / 컴포넌트 값(복사됨)
     */
    public CssRaw(List<ComponentValue> tokens) {
        this.tokens = Collections.unmodifiableList(new ArrayList<ComponentValue>(tokens));
    }

    /**
     * Returns the component values.
     * <p>
     * 컴포넌트 값을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ComponentValue> getTokens() {
        return tokens;
    }

    /**
     * Serializes the component values and trims the result.
     * <p>
     * 컴포넌트 값을 직렬화하고 앞뒤 공백을 다듬는다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        return CssBlock.join(tokens).trim();
    }

    /**
     * Compares the serialized text.
     * <p>
     * 직렬화한 문자열을 비교한다.
     *
     * @param o other object / 다른 객체
     * @return true if equal / 같으면 true
     */
    public boolean equals(Object o) {
        return o instanceof CssRaw && ((CssRaw) o).toCssText().equals(toCssText());
    }

    /**
     * Hash of the serialized text.
     * <p>
     * 직렬화한 문자열의 해시.
     *
     * @return hash code / 해시 코드
     */
    public int hashCode() {
        return toCssText().hashCode();
    }
}
