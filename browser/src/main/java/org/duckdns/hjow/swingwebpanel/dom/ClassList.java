package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * The token list view of the <code>class</code> attribute of an element (<code>classList</code>, a <code>DOMTokenList</code>). It reads
 * and writes the attribute directly, so it is always in sync; every change re-serializes the tokens separated by one space (the "update
 * steps" of the DOM Standard) and is reported like a <code>setAttribute</code> call.
 * <p>
 * 요소의 <code>class</code> 속성에 대한 토큰 목록 뷰(<code>classList</code>, <code>DOMTokenList</code>). 속성을 직접 읽고 쓰므로 항상 동기화되어 있다.
 * 모든 변경은 토큰을 공백 하나로 구분해 다시 직렬화하며(DOM 표준의 "update steps") <code>setAttribute</code> 호출처럼 보고된다.
 */
public final class ClassList {

    /**
     * The element whose attribute is viewed.
     * <p>
     * 속성을 보여 주는 요소.
     */
    private final Element element;

    /**
     * The name of the viewed attribute (<code>class</code> for <code>classList</code>, <code>rel</code> for <code>relList</code> and so on).
     * <p>
     * 보여 주는 속성의 이름(<code>classList</code> 는 <code>class</code>, <code>relList</code> 는 <code>rel</code> 등).
     */
    private final String attribute;

    /**
     * Creates the view.
     * <p>
     * 뷰를 만든다.
     *
     * @param element owner element / 소유 요소
     */
    ClassList(Element element) {
        this(element, "class");
    }

    /**
     * Creates the token list view of another attribute (<code>relList</code>, <code>sandbox</code> ...).
     * <p>
     * 다른 속성의 토큰 목록 뷰를 만든다(<code>relList</code>, <code>sandbox</code> 등).
     *
     * @param element   owner element / 소유 요소
     * @param attribute the attribute name / 속성 이름
     */
    ClassList(Element element, String attribute) {
        this.element = element;
        this.attribute = attribute;
    }

    /**
     * Splits a string into the ordered set of tokens (ASCII whitespace separated, duplicates removed, first occurrence kept).
     * <p>
     * 문자열을 순서 있는 토큰 집합으로 나눈다(ASCII 공백으로 구분, 중복 제거, 처음 나온 것을 유지).
     *
     * @param value attribute value / 속성 값
     * @return the tokens / 토큰
     */
    static List<String> parseTokens(String value) {
        List<String> tokens = new ArrayList<String>();
        int i = 0;
        int n = value.length();
        // 공백이 아닌 구간을 하나씩 잘라 중복 없이 모은다.
        while (i < n) {
            while (i < n && Namespaces.isAsciiWhitespace(value.charAt(i))) {
                i++;
            }
            int start = i;
            // 공백을 만날 때까지 토큰의 끝을 찾는다.
            while (i < n && !Namespaces.isAsciiWhitespace(value.charAt(i))) {
                i++;
            }
            if (i > start) {
                String t = value.substring(start, i);
                if (!tokens.contains(t)) {
                    tokens.add(t);
                }
            }
        }
        return tokens;
    }

    /**
     * Returns the current tokens.
     * <p>
     * 현재 토큰을 반환한다.
     *
     * @return tokens / 토큰
     */
    private List<String> tokens() {
        String v = element.getAttribute(attribute);
        return parseTokens(v == null ? "" : v);
    }

    /**
     * Validates a token: not empty (SyntaxError) and without ASCII whitespace (InvalidCharacterError).
     * <p>
     * 토큰을 검증한다: 비어 있으면 안 되고(SyntaxError) ASCII 공백이 없어야 한다(InvalidCharacterError).
     *
     * @param token token to validate / 검증할 토큰
     */
    private static void validate(String token) {
        if (token == null || token.length() == 0) {
            throw DomException.syntax("the token must not be empty");
        }
        // 토큰 안에 공백이 있는지 확인한다.
        for (int i = 0; i < token.length(); i++) {
            if (Namespaces.isAsciiWhitespace(token.charAt(i))) {
                throw DomException.invalidCharacter("the token must not contain whitespace");
            }
        }
    }

    /**
     * Writes the tokens back to the class attribute, unless nothing changed and the attribute is absent.
     * <p>
     * 토큰을 class 속성에 다시 쓴다. 바뀐 것이 없고 속성이 없으면 쓰지 않는다.
     *
     * @param list new tokens / 새 토큰
     */
    private void update(List<String> list) {
        if (list.isEmpty() && !element.hasAttribute(attribute)) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        // 토큰을 공백 하나로 이어 붙인다.
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(list.get(i));
        }
        element.setAttribute(attribute, sb.toString());
    }

    /**
     * Returns the number of tokens.
     * <p>
     * 토큰의 수를 반환한다.
     *
     * @return length / 길이
     */
    public int getLength() {
        return tokens().size();
    }

    /**
     * Returns the token at an index.
     * <p>
     * 위치의 토큰을 반환한다.
     *
     * @param index zero based index / 0 기반 위치
     * @return token or null / 토큰 또는 null
     */
    public String item(int index) {
        List<String> t = tokens();
        return index >= 0 && index < t.size() ? t.get(index) : null;
    }

    /**
     * Tells whether the list contains a token (case-sensitive).
     * <p>
     * 목록에 토큰이 있는지 알려 준다(대소문자 구분).
     *
     * @param token token / 토큰
     * @return true if present / 있으면 true
     */
    public boolean contains(String token) {
        return tokens().contains(token);
    }

    /**
     * Adds tokens that are not yet present.
     * <p>
     * 아직 없는 토큰을 추가한다.
     *
     * @param newTokens tokens to add / 추가할 토큰
     * @throws DomException SyntaxError for an empty token, InvalidCharacterError for whitespace / 빈 토큰은 SyntaxError, 공백은 InvalidCharacterError
     */
    public void add(String... newTokens) {
        // 모든 토큰을 먼저 검증한 뒤 하나도 적용되지 않은 상태에서 실패하게 한다.
        for (int i = 0; i < newTokens.length; i++) {
            validate(newTokens[i]);
        }
        List<String> list = tokens();
        // 아직 없는 토큰만 목록 끝에 추가한다.
        for (int i = 0; i < newTokens.length; i++) {
            if (!list.contains(newTokens[i])) {
                list.add(newTokens[i]);
            }
        }
        update(list);
    }

    /**
     * Removes tokens.
     * <p>
     * 토큰을 제거한다.
     *
     * @param oldTokens tokens to remove / 제거할 토큰
     * @throws DomException SyntaxError for an empty token, InvalidCharacterError for whitespace / 빈 토큰은 SyntaxError, 공백은 InvalidCharacterError
     */
    public void remove(String... oldTokens) {
        // 모든 토큰을 먼저 검증한다.
        for (int i = 0; i < oldTokens.length; i++) {
            validate(oldTokens[i]);
        }
        List<String> list = tokens();
        // 제거할 토큰을 하나씩 목록에서 뺀다.
        for (int i = 0; i < oldTokens.length; i++) {
            list.remove(oldTokens[i]);
        }
        update(list);
    }

    /**
     * Toggles a token and returns whether it is present afterwards.
     * <p>
     * 토큰을 토글하고 그 뒤에 있는지 반환한다.
     *
     * @param token token / 토큰
     * @return true if the token is present after the call / 호출 뒤에 토큰이 있으면 true
     */
    public boolean toggle(String token) {
        return toggle(token, null);
    }

    /**
     * Toggles a token or forces its presence or absence.
     * <p>
     * 토큰을 토글하거나 있음/없음을 강제한다.
     *
     * @param token token / 토큰
     * @param force TRUE to force presence, FALSE to force absence, null to toggle / TRUE 이면 있게, FALSE 이면 없게, null 이면 토글
     * @return true if the token is present after the call / 호출 뒤에 토큰이 있으면 true
     */
    public boolean toggle(String token, Boolean force) {
        validate(token);
        List<String> list = tokens();
        if (list.contains(token)) {
            if (force == null || !force.booleanValue()) {
                list.remove(token);
                update(list);
                return false;
            }
            return true;
        }
        if (force == null || force.booleanValue()) {
            list.add(token);
            update(list);
            return true;
        }
        return false;
    }

    /**
     * Replaces a token with another one.
     * <p>
     * 토큰을 다른 토큰으로 바꾼다.
     *
     * @param oldToken token to replace / 바꿀 토큰
     * @param newToken new token / 새 토큰
     * @return true if the old token was present and replaced / 이전 토큰이 있어 바꾸었으면 true
     */
    public boolean replace(String oldToken, String newToken) {
        validate(oldToken);
        validate(newToken);
        List<String> list = tokens();
        int index = list.indexOf(oldToken);
        if (index < 0) {
            return false;
        }
        int existing = list.indexOf(newToken);
        if (existing >= 0 && existing != index) {
            // 새 토큰이 이미 있으면 이전 토큰만 지워 중복을 막는다.
            list.remove(index);
        } else {
            list.set(index, newToken);
        }
        update(list);
        return true;
    }

    /**
     * Returns the attribute value (the string that the tokens came from).
     * <p>
     * 속성 값(토큰의 원본 문자열)을 반환한다.
     *
     * @return value, empty if the attribute is absent / 값. 속성이 없으면 빈 문자열
     */
    public String getValue() {
        String v = element.getAttribute(attribute);
        return v == null ? "" : v;
    }

    /**
     * Sets the attribute value.
     * <p>
     * 속성 값을 지정한다.
     *
     * @param value new value / 새 값
     */
    public void setValue(String value) {
        element.setAttribute(attribute, value);
    }

    /**
     * Returns the tokens as a Java list copy.
     * <p>
     * 토큰을 Java 목록 복사본으로 반환한다.
     *
     * @return tokens / 토큰
     */
    public List<String> toList() {
        return tokens();
    }

    /**
     * Returns the attribute value.
     * <p>
     * 속성 값을 반환한다.
     *
     * @return value / 값
     */
    public String toString() {
        return getValue();
    }
}
