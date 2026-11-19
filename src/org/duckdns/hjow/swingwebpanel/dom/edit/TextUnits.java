package org.duckdns.hjow.swingwebpanel.dom.edit;

import java.text.BreakIterator;
import java.util.Locale;

/**
 * The units of text that editing moves and deletes by: a character (a surrogate pair, or a base character with its combining marks, is one
 * unit), a word, and the white space that HTML collapses. All methods work on the UTF-16 data of a text node and return offsets in it.
 * <p>
 * 편집이 이동하거나 지우는 텍스트 단위: 글자(서로게이트 쌍이나 기본 문자와 결합 문자는 한 단위), 낱말, HTML 이 접는 공백. 모든 메서드는 텍스트 노드의 UTF-16 데이터에
 * 대해 일하며 그 안의 오프셋을 반환한다.
 */
final class TextUnits {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TextUnits() {
    }

    /**
     * Tells whether a character is white space that HTML collapses (space, tab, line feed, carriage return, form feed); the no-break space
     * is not collapsed.
     * <p>
     * 문자가 HTML 이 접는 공백(공백, 탭, 줄바꿈, 캐리지 리턴, 폼 피드)인지 알려 준다. 줄바꿈 없는 공백(NBSP)은 접히지 않는다.
     *
     * @param c the character / 문자
     * @return true for collapsible white space / 접히는 공백이면 true
     */
    static boolean isCollapsible(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f';
    }

    /**
     * Tells whether a character is a combining mark that belongs to the character before it.
     * <p>
     * 문자가 앞 문자에 속하는 결합 표시인지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for a combining mark / 결합 표시이면 true
     */
    private static boolean isMark(char c) {
        int t = Character.getType(c);
        return t == Character.NON_SPACING_MARK || t == Character.ENCLOSING_MARK || t == Character.COMBINING_SPACING_MARK || c == '\u200D';
    }

    /**
     * Returns the end of the character unit that starts at an offset.
     * <p>
     * 오프셋에서 시작하는 글자 단위의 끝을 반환한다.
     *
     * @param s the text / 텍스트
     * @param i the offset, less than the length / 오프셋(길이 미만)
     * @return the offset after the unit / 단위 뒤의 오프셋
     */
    static int next(String s, int i) {
        int e = i + 1;
        if (e < s.length() && Character.isHighSurrogate(s.charAt(i)) && Character.isLowSurrogate(s.charAt(e))) {
            e++;
        }
        // 뒤따르는 결합 표시를 모두 같은 단위에 넣는다.
        while (e < s.length() && isMark(s.charAt(e))) {
            e++;
        }
        return e;
    }

    /**
     * Returns the start of the character unit that ends at an offset.
     * <p>
     * 오프셋에서 끝나는 글자 단위의 시작을 반환한다.
     *
     * @param s the text / 텍스트
     * @param i the offset, more than 0 / 오프셋(0 초과)
     * @return the offset before the unit / 단위 앞의 오프셋
     */
    static int previous(String s, int i) {
        int b = i - 1;
        // 결합 표시는 앞의 기본 문자까지 거슬러 올라간다.
        while (b > 0 && isMark(s.charAt(b))) {
            b--;
        }
        if (b > 0 && Character.isLowSurrogate(s.charAt(b)) && Character.isHighSurrogate(s.charAt(b - 1))) {
            b--;
        }
        return b;
    }

    /**
     * Returns where deleting a word backward from an offset ends: the white space before the offset is skipped, then the word.
     * <p>
     * 오프셋에서 뒤로 낱말을 지울 때 끝나는 곳을 반환한다: 오프셋 앞의 공백을 건너뛰고 그 앞의 낱말까지.
     *
     * @param s the text / 텍스트
     * @param i the offset / 오프셋
     * @return the offset that the deletion reaches / 지우기가 닿는 오프셋
     */
    static int wordBefore(String s, int i) {
        int p = i;
        // 캐럿 앞의 공백을 먼저 건너뛴다.
        while (p > 0 && (isCollapsible(s.charAt(p - 1)) || s.charAt(p - 1) == '\u00A0')) {
            p--;
        }
        if (p == 0) {
            return 0;
        }
        BreakIterator words = BreakIterator.getWordInstance(Locale.ENGLISH);
        words.setText(s);
        int b = words.preceding(p);
        return b == BreakIterator.DONE ? 0 : b;
    }

    /**
     * Returns where deleting a word forward from an offset ends: the rest of the word, then the white space after it.
     * <p>
     * 오프셋에서 앞으로 낱말을 지울 때 끝나는 곳을 반환한다: 낱말의 나머지와 그 뒤의 공백까지.
     *
     * @param s the text / 텍스트
     * @param i the offset / 오프셋
     * @return the offset that the deletion reaches / 지우기가 닿는 오프셋
     */
    static int wordAfter(String s, int i) {
        int p = i;
        if (p < s.length() && !(isCollapsible(s.charAt(p)) || s.charAt(p) == '\u00A0')) {
            BreakIterator words = BreakIterator.getWordInstance(Locale.ENGLISH);
            words.setText(s);
            int b = words.following(p);
            p = b == BreakIterator.DONE ? s.length() : b;
        }
        // 낱말 뒤의 공백도 함께 지운다.
        while (p < s.length() && (isCollapsible(s.charAt(p)) || s.charAt(p) == '\u00A0')) {
            p++;
        }
        return p;
    }
}
