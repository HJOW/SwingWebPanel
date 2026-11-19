package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * The <code>An+B</code> micro-syntax of <code>:nth-child()</code> and its relatives: the keywords <code>odd</code> and <code>even</code>, a plain
 * integer, or <code>An+B</code> with all the sign and whitespace variants of the specification (<code>2n+1</code>, <code>-n + 3</code>,
 * <code>+n</code>, <code>n- 2</code>, ...). Because the tokenizer glues signs and digits into dimensions and identifiers, parsing is done on the
 * token pattern, not on the text.
 * <p>
 * <code>:nth-child()</code> 계열의 <code>An+B</code> 미세 문법: 키워드 <code>odd</code>, <code>even</code>, 정수 하나, 또는 규격의 모든 부호와 공백 변형을 가진
 * <code>An+B</code>(<code>2n+1</code>, <code>-n + 3</code>, <code>+n</code>, <code>n- 2</code> 등). 토큰화기가 부호와 숫자를 차원과 식별자로 붙이므로 텍스트가 아니라
 * 토큰 패턴으로 해석한다.
 */
public final class AnPlusB {

    /** The step A. / 증분 A. */
    private final int a;

    /** The offset B. / 오프셋 B. */
    private final int b;

    /**
     * Creates the pair.
     * <p>
     * 쌍을 만든다.
     *
     * @param a step / 증분
     * @param b offset / 오프셋
     */
    public AnPlusB(int a, int b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Returns A.
     * <p>
     * A 를 반환한다.
     *
     * @return step / 증분
     */
    public int getA() {
        return a;
    }

    /**
     * Returns B.
     * <p>
     * B 를 반환한다.
     *
     * @return offset / 오프셋
     */
    public int getB() {
        return b;
    }

    /**
     * Tells whether a 1-based position is selected: there must be an integer n &gt;= 0 with <code>a*n + b == position</code>.
     * <p>
     * 1부터 시작하는 위치가 선택되는지 알려 준다: <code>a*n + b == position</code> 인 정수 n &gt;= 0 이 있어야 한다.
     *
     * @param position 1-based position / 1부터 시작하는 위치
     * @return true if selected / 선택되면 true
     */
    public boolean matches(int position) {
        long diff = (long) position - b;
        if (a == 0) {
            return diff == 0;
        }
        // 차이가 증분의 음이 아닌 정수배인지 확인한다.
        return diff % a == 0 && diff / a >= 0;
    }

    /**
     * Serializes in the canonical form of CSSOM.
     * <p>
     * CSSOM 의 정규 형태로 직렬화한다.
     *
     * @return text such as <code>2n+1</code>, <code>odd</code> / <code>2n+1</code>, <code>odd</code> 같은 문자열
     */
    public String toCssText() {
        if (a == 2 && b == 1) {
            return "odd";
        }
        if (a == 2 && b == 0) {
            return "even";
        }
        if (a == 0) {
            return Integer.toString(b);
        }
        String as = a == 1 ? "n" : a == -1 ? "-n" : a + "n";
        if (b == 0) {
            return as;
        }
        return as + (b > 0 ? "+" + b : Integer.toString(b));
    }

    /**
     * Parses the component values of an An+B expression.
     * <p>
     * An+B 식의 컴포넌트 값을 파싱한다.
     *
     * @param values component values (surrounding whitespace is ignored) / 컴포넌트 값(앞뒤 공백은 무시)
     * @return the parsed pair, or null if the syntax is invalid / 파싱한 쌍. 문법이 틀리면 null
     */
    public static AnPlusB parse(List<ComponentValue> values) {
        List<ComponentValue> v = CssParser.trim(values);
        if (v.isEmpty() || !(v.get(0) instanceof CssToken)) {
            return null;
        }
        CssToken first = (CssToken) v.get(0);
        if (first.is(CssToken.IDENT)) {
            String s = first.getValue().toLowerCase();
            if (v.size() == 1 && s.equals("odd")) {
                return new AnPlusB(2, 1);
            }
            if (v.size() == 1 && s.equals("even")) {
                return new AnPlusB(2, 0);
            }
            if (s.equals("n") || s.equals("-n")) {
                return finishAfterN(v, 1, s.equals("n") ? 1 : -1);
            }
            if (s.startsWith("n-") || s.startsWith("-n-")) {
                int pre = s.startsWith("n-") ? 2 : 3;
                return finishDashed(v, 1, s.equals("-n-") || s.startsWith("-n-") ? -1 : 1, s.substring(pre));
            }
            return null;
        }
        if (first.isDelim('+') && v.size() >= 2 && v.get(1) instanceof CssToken && ((CssToken) v.get(1)).is(CssToken.IDENT)) {
            String s = ((CssToken) v.get(1)).getValue().toLowerCase();
            if (s.equals("n")) {
                return finishAfterN(v, 2, 1);
            }
            if (s.startsWith("n-")) {
                return finishDashed(v, 2, 1, s.substring(2));
            }
            return null;
        }
        if (first.is(CssToken.NUMBER) && first.isInteger()) {
            return v.size() == 1 ? new AnPlusB(0, (int) first.getNumber()) : null;
        }
        if (first.is(CssToken.DIMENSION) && first.isInteger()) {
            String u = first.getUnit().toLowerCase();
            int a = (int) first.getNumber();
            if (u.equals("n")) {
                return finishAfterN(v, 1, a);
            }
            if (u.startsWith("n-")) {
                return finishDashed(v, 1, a, u.substring(2));
            }
            return null;
        }
        return null;
    }

    /**
     * Finishes after a bare <code>n</code>: an optional signed B follows.
     * <p>
     * 단독 <code>n</code> 뒤를 마무리한다: 부호 있는 B 가 선택적으로 온다.
     *
     * @param v    trimmed values / 다듬은 값
     * @param i    index after the n / n 다음 위치
     * @param a    step / 증분
     * @return the pair or null / 쌍 또는 null
     */
    private static AnPlusB finishAfterN(List<ComponentValue> v, int i, int a) {
        int n = v.size();
        // n 과 B 사이의 공백을 건너뛴다.
        while (i < n && CssParser.isWhitespace(v.get(i))) {
            i++;
        }
        if (i == n) {
            return new AnPlusB(a, 0);
        }
        ComponentValue c = v.get(i);
        if (!(c instanceof CssToken)) {
            return null;
        }
        CssToken t = (CssToken) c;
        if (t.is(CssToken.NUMBER) && t.isInteger() && i == n - 1 && hasSign(t)) {
            return new AnPlusB(a, (int) t.getNumber());
        }
        if (t.isDelim('+') || t.isDelim('-')) {
            int j = i + 1;
            // 부호와 숫자 사이의 공백을 건너뛴다.
            while (j < n && CssParser.isWhitespace(v.get(j))) {
                j++;
            }
            if (j == n - 1 && v.get(j) instanceof CssToken) {
                CssToken num = (CssToken) v.get(j);
                if (num.is(CssToken.NUMBER) && num.isInteger() && !hasSign(num)) {
                    int val = (int) num.getNumber();
                    return new AnPlusB(a, t.isDelim('-') ? -val : val);
                }
            }
        }
        return null;
    }

    /**
     * Finishes after an <code>n-</code> prefixed unit: either digits follow in the same token, or a separate unsigned integer follows.
     * <p>
     * <code>n-</code> 로 시작하는 단위 뒤를 마무리한다: 같은 토큰에 숫자가 이어지거나 부호 없는 정수가 따로 온다.
     *
     * @param v    trimmed values / 다듬은 값
     * @param i    index after the token / 토큰 다음 위치
     * @param a    step / 증분
     * @param rest the text after "n-" / "n-" 뒤의 문자열
     * @return the pair or null / 쌍 또는 null
     */
    private static AnPlusB finishDashed(List<ComponentValue> v, int i, int a, String rest) {
        int n = v.size();
        if (rest.length() > 0) {
            // 같은 토큰 안의 나머지가 숫자로만 되어 있어야 한다.
            for (int k = 0; k < rest.length(); k++) {
                if (rest.charAt(k) < '0' || rest.charAt(k) > '9') {
                    return null;
                }
            }
            if (i != n) {
                return null;
            }
            return new AnPlusB(a, -parseInt(rest));
        }
        // n- 뒤의 공백을 건너뛰고 부호 없는 정수를 읽는다.
        while (i < n && CssParser.isWhitespace(v.get(i))) {
            i++;
        }
        if (i == n - 1 && v.get(i) instanceof CssToken) {
            CssToken t = (CssToken) v.get(i);
            if (t.is(CssToken.NUMBER) && t.isInteger() && !hasSign(t)) {
                return new AnPlusB(a, -(int) t.getNumber());
            }
        }
        return null;
    }

    /**
     * Tells whether a number token was written with an explicit sign.
     * <p>
     * 숫자 토큰에 부호가 명시되어 있는지 알려 준다.
     *
     * @param t number token / 숫자 토큰
     * @return true if the source had a sign / 소스에 부호가 있으면 true
     */
    private static boolean hasSign(CssToken t) {
        String s = t.toCssText();
        return s.length() > 0 && (s.charAt(0) == '+' || s.charAt(0) == '-');
    }

    /**
     * Parses digits, saturating on overflow.
     * <p>
     * 숫자를 읽으며 넘치면 최대값으로 고정한다.
     *
     * @param s digits / 숫자 문자열
     * @return value / 값
     */
    private static int parseInt(String s) {
        long v = 0;
        // 자리마다 값을 모으되 int 범위를 넘으면 멈춘다.
        for (int i = 0; i < s.length(); i++) {
            v = v * 10 + (s.charAt(i) - '0');
            if (v > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) v;
    }
}
