package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * GetSubstitution (ECMA-262 6th edition 21.1.3.14.1): expands the replacement patterns <code>$$</code>, <code>$&amp;</code>,
 * <code>$`</code>, <code>$'</code>, <code>$n</code> and <code>$nn</code> of a replacement string. It is public so that the RegExp built-in
 * can use the same implementation. Web compatible behavior is used where the specification text is ambiguous: for <code>$nn</code> with
 * a two digit number larger than the number of captures the single digit form <code>$n</code> followed by a literal digit is used when
 * <code>n</code> is a valid capture; references to captures that do not exist are left as written.
 * <p>
 * GetSubstitution(ECMA-262 6판 21.1.3.14.1): 치환 문자열의 <code>$$</code>, <code>$&amp;</code>, <code>$`</code>, <code>$'</code>,
 * <code>$n</code>, <code>$nn</code> 패턴을 전개한다. RegExp 내장 객체가 같은 구현을 쓸 수 있도록 public 이다. 규격 문장이 모호한 곳은 웹
 * 호환 동작을 따른다: 두 자리 <code>$nn</code> 이 캡처 수보다 크면 <code>n</code> 이 유효한 캡처일 때 한 자리 <code>$n</code> 뒤에 숫자
 * 문자가 이어진 것으로 보며, 없는 캡처에 대한 참조는 쓴 그대로 둔다.
 */
public final class Substitution {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Substitution() {
    }

    /**
     * Expands a replacement pattern.
     * <p>
     * 치환 패턴을 전개한다.
     *
     * @param matched     the matched substring / 일치한 부분 문자열
     * @param str         the whole string being searched / 검색 대상 전체 문자열
     * @param position    index of the match in <code>str</code> / <code>str</code> 안에서 일치 위치
     * @param captures    captured groups: strings, or undefined for groups that did not participate (may be empty) / 캡처 그룹: 문자열이거나
     *                    참여하지 않은 그룹은 undefined (비어 있을 수 있음)
     * @param replacement the replacement string that contains patterns / 패턴이 들어 있는 치환 문자열
     * @return the expanded text / 전개한 문자열
     */
    public static String expand(String matched, String str, int position, JsValue[] captures, String replacement) {
        int m = captures.length;
        int tailPos = Math.min(str.length(), position + matched.length());
        StringBuilder sb = new StringBuilder(replacement.length() + 16);
        int n = replacement.length();
        int i = 0;
        // 치환 문자열을 앞에서부터 읽으며 $ 패턴을 만나면 전개한다.
        while (i < n) {
            char c = replacement.charAt(i);
            if (c != '$' || i + 1 >= n) {
                sb.append(c);
                i++;
                continue;
            }
            char next = replacement.charAt(i + 1);
            if (next == '$') {
                sb.append('$');
                i += 2;
            } else if (next == '&') {
                sb.append(matched);
                i += 2;
            } else if (next == '`') {
                sb.append(str, 0, position);
                i += 2;
            } else if (next == '\'') {
                sb.append(str, tailPos, str.length());
                i += 2;
            } else if (next >= '0' && next <= '9') {
                int d1 = next - '0';
                int d2 = i + 2 < n && replacement.charAt(i + 2) >= '0' && replacement.charAt(i + 2) <= '9' ? replacement.charAt(i + 2) - '0' : -1;
                int two = d2 >= 0 ? d1 * 10 + d2 : -1;
                if (two >= 1 && two <= m) {
                    appendCapture(sb, captures[two - 1]);
                    i += 3;
                } else if (d1 >= 1 && d1 <= m) {
                    appendCapture(sb, captures[d1 - 1]);
                    i += 2;
                } else {
                    sb.append('$');
                    i++;
                }
            } else {
                sb.append('$');
                i++;
            }
        }
        return sb.toString();
    }

    /**
     * Appends a capture: undefined captures become the empty string.
     * <p>
     * 캡처를 덧붙인다. undefined 캡처는 빈 문자열이 된다.
     *
     * @param sb      target / 대상
     * @param capture capture value / 캡처 값
     */
    private static void appendCapture(StringBuilder sb, JsValue capture) {
        if (!capture.isUndefined()) {
            sb.append(Ops.toString(capture));
        }
    }
}
