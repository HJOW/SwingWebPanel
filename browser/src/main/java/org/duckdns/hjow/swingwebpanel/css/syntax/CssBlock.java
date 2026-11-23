package org.duckdns.hjow.swingwebpanel.css.syntax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A simple block: component values between a pair of brackets, parentheses or curly braces.
 * <p>
 * 단순 블록: 대괄호, 소괄호, 중괄호 한 쌍 사이의 컴포넌트 값들.
 */
public final class CssBlock extends ComponentValue {

    /** Opening token type (LEFT_BRACKET, LEFT_PAREN or LEFT_BRACE). / 여는 토큰의 종류. */
    private final int openType;

    /** Contents of the block. / 블록의 내용. */
    private final List<ComponentValue> contents;

    /**
     * Creates a block.
     * <p>
     * 블록을 만든다.
     *
     * @param openType opening token type / 여는 토큰 종류
     * @param contents contained component values / 포함된 컴포넌트 값
     */
    public CssBlock(int openType, List<ComponentValue> contents) {
        this.openType = openType;
        this.contents = Collections.unmodifiableList(new ArrayList<ComponentValue>(contents));
    }

    /**
     * Returns the opening token type.
     * <p>
     * 여는 토큰 종류를 반환한다.
     *
     * @return token type / 토큰 종류
     */
    public int getOpenType() {
        return openType;
    }

    /**
     * Returns the contents.
     * <p>
     * 내용을 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ComponentValue> getContents() {
        return contents;
    }

    /**
     * Serializes the block with its brackets.
     * <p>
     * 괄호와 함께 블록을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String toCssText() {
        String open = openType == CssToken.LEFT_BRACKET ? "[" : openType == CssToken.LEFT_PAREN ? "(" : "{";
        String close = openType == CssToken.LEFT_BRACKET ? "]" : openType == CssToken.LEFT_PAREN ? ")" : "}";
        return open + join(contents) + close;
    }

    /**
     * Serializes a list of component values.
     * <p>
     * 컴포넌트 값 목록을 직렬화한다.
     *
     * @param list component values / 컴포넌트 값
     * @return CSS text / CSS 문자열
     */
    public static String join(List<ComponentValue> list) {
        StringBuilder sb = new StringBuilder();
        // 각 컴포넌트 값을 순서대로 이어 붙인다.
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toCssText());
        }
        return sb.toString();
    }
}
