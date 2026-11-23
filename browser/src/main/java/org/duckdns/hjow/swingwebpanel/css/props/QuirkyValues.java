package org.duckdns.hjow.swingwebpanel.css.props;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * The value quirks of CSS that is parsed in quirks mode: the <code>&lt;quirky-length&gt;</code> (a unitless number is a <code>px</code> length in
 * a fixed list of properties, CSS Values 4 appendix C) and the <code>&lt;quirky-color&gt;</code> (a hex color without the leading hash in
 * <code>color</code>, <code>background-color</code> and the border colors, CSS Color 4 appendix B), as the Quirks Mode Standard sections 3.2 and
 * 3.1 refer to them. The quirks are applied to the top level components of the value only, never inside functions such as
 * <code>calc()</code> or <code>rgb()</code>, and not to shorthands that merely contain the listed properties (<code>background</code>,
 * <code>border</code>, <code>font</code>). The component values are rewritten into the ordinary dimension and hash tokens before the regular
 * grammar runs, so everything after that point is unchanged. Sources: https://drafts.csswg.org/css-values-4/#quirky-length and
 * https://drafts.csswg.org/css-color-4/#quirky-color (living drafts, retrieved 2026-11-17, last modified 2026-11-16) and
 * https://quirks.spec.whatwg.org/ (Living Standard of 2026-10-19). The one exception to the rule about functions is the <code>rect()</code> of <code>clip</code>, whose arguments may be quirky lengths.
 * <p>
 * 호환 모드로 파싱하는 CSS 의 값 quirk: <code>&lt;quirky-length&gt;</code>(정해진 속성 목록에서 단위 없는 숫자가 <code>px</code> 길이, CSS Values 4 부록 C)와
 * <code>&lt;quirky-color&gt;</code>(<code>color</code>, <code>background-color</code>, border 색에서 # 없는 16진수 색, CSS Color 4 부록 B)이며 호환 모드 표준 3.2, 3.1절이
 * 가리킨다. quirk 는 값의 최상위 구성 요소에만 적용하고 <code>calc()</code>, <code>rgb()</code> 같은 함수 안에는 적용하지 않으며 나열된 속성을 담기만 하는
 * 단축 속성(<code>background</code>, <code>border</code>, <code>font</code>)에도 적용하지 않는다. 구성 요소 값을 일반 차원/해시 토큰으로 바꾼 뒤 평소의 문법을 돌리므로
 * 그 뒤는 달라지지 않는다. 함수에 적용하지 않는다는 규칙의 유일한 예외는 <code>clip</code> 의 <code>rect()</code> 이며 그 인수는 quirky length 일 수 있다.
 */
public final class QuirkyValues {

    /** The properties that accept a unitless length in quirks mode (without <code>clip</code>). / 호환 모드에서 단위 없는 길이를 받는 속성(<code>clip</code> 제외). */
    private static final Set<String> LENGTH_PROPERTIES = new HashSet<String>(Arrays.asList(new String[] {
        "background-position", "border-spacing", "border-top-width", "border-right-width", "border-bottom-width", "border-left-width",
        "border-width", "bottom", "font-size", "height", "left", "letter-spacing", "margin-right", "margin-left", "margin-top", "margin-bottom",
        "margin", "max-height", "max-width", "min-height", "min-width", "padding-top", "padding-right", "padding-bottom", "padding-left",
        "padding", "right", "text-indent", "top", "vertical-align", "width", "word-spacing"
    }));

    /** The properties that accept a hex color without a hash in quirks mode. / 호환 모드에서 # 없는 16진수 색을 받는 속성. */
    private static final Set<String> COLOR_PROPERTIES = new HashSet<String>(Arrays.asList(new String[] {
        "background-color", "border-color", "border-top-color", "border-right-color", "border-bottom-color", "border-left-color", "color"
    }));

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private QuirkyValues() {
    }

    /**
     * Rewrites the quirky components of a declaration value into ordinary tokens. The list is returned unchanged if the property takes
     * no quirks or no component needs rewriting.
     * <p>
     * 선언 값의 quirk 구성 요소를 일반 토큰으로 바꾼다. 속성이 quirk 를 받지 않거나 바꿀 구성 요소가 없으면 목록을 그대로 반환한다.
     *
     * @param property the canonical (lower-case) property name / 정규(소문자) 속성 이름
     * @param value    the declaration value / 선언 값
     * @return the value to parse / 파싱할 값
     */
    public static List<ComponentValue> rewrite(String property, List<ComponentValue> value) {
        boolean length = LENGTH_PROPERTIES.contains(property);
        boolean color = COLOR_PROPERTIES.contains(property);
        boolean clip = "clip".equals(property);
        if (!length && !color && !clip) {
            return value;
        }
        List<ComponentValue> out = null;
        // 최상위 구성 요소를 하나씩 보며 quirk 값이면 일반 토큰으로 바꾼다.
        for (int i = 0; i < value.size(); i++) {
            ComponentValue v = value.get(i);
            ComponentValue replaced = null;
            if (clip) {
                // clip 은 rect() 함수의 인수에만 quirky length 가 허용된다(CSS Values 4 부록 C 의 예외).
                replaced = v instanceof CssFunction && ((CssFunction) v).isNamed("rect") ? rectOf((CssFunction) v) : null;
            } else if (v instanceof CssToken) {
                replaced = length ? lengthOf((CssToken) v) : colorOf((CssToken) v);
            }
            if (replaced != null) {
                if (out == null) {
                    out = new ArrayList<ComponentValue>(value);
                }
                out.set(i, replaced);
            }
        }
        return out == null ? value : out;
    }

    /**
     * Rewrites the plain numbers among the arguments of the <code>rect()</code> of <code>clip</code> into <code>px</code> dimensions.
     * <p>
     * <code>clip</code> 의 <code>rect()</code> 인수 중 일반 숫자를 <code>px</code> 차원으로 바꾼다.
     *
     * @param f the <code>rect()</code> function / <code>rect()</code> 함수
     * @return the rewritten function, or null if no argument is a plain number / 바꾼 함수. 일반 숫자 인수가 없으면 null
     */
    private static ComponentValue rectOf(CssFunction f) {
        List<ComponentValue> args = null;
        // 인수를 하나씩 보며 숫자이면 px 길이로 바꾼다.
        for (int i = 0; i < f.getArguments().size(); i++) {
            ComponentValue a = f.getArguments().get(i);
            ComponentValue r = a instanceof CssToken ? lengthOf((CssToken) a) : null;
            if (r != null) {
                if (args == null) {
                    args = new ArrayList<ComponentValue>(f.getArguments());
                }
                args.set(i, r);
            }
        }
        return args == null ? null : new CssFunction(f.getName(), args);
    }

    /**
     * Turns a plain number token into a <code>px</code> dimension (the quirky length).
     * <p>
     * 일반 숫자 토큰을 <code>px</code> 차원 토큰(quirky length)으로 바꾼다.
     *
     * @param t the token / 토큰
     * @return the dimension token, or null if the token is not a number / 차원 토큰. 숫자가 아니면 null
     */
    private static ComponentValue lengthOf(CssToken t) {
        if (!t.is(CssToken.NUMBER)) {
            return null;
        }
        return new CssToken(CssToken.DIMENSION, "", t.getNumber(), t.isInteger(), "px", false, t.toCssText() + "px", t.getStart());
    }

    /**
     * Turns an integer number, an integer dimension or an identifier of three or six hexadecimal digits into a hash token (the quirky
     * color): the digits of a number are written out and padded on the left with zeros up to six characters.
     * <p>
     * 정수 숫자, 정수 차원, 16진수 3자리 또는 6자리 식별자를 해시 토큰(quirky color)으로 바꾼다: 숫자는 자릿수를 풀어 쓰고 왼쪽을 0 으로 6자까지 채운다.
     *
     * @param t the token / 토큰
     * @return the hash token, or null if the token is not a quirky color / 해시 토큰. quirky color 가 아니면 null
     */
    private static ComponentValue colorOf(CssToken t) {
        String text;
        if (t.is(CssToken.IDENT)) {
            text = t.getValue();
            // 식별자는 16진수 숫자 3개 또는 6개일 때만 색이다.
            if (text.length() != 3 && text.length() != 6) {
                return null;
            }
        } else if (t.is(CssToken.NUMBER) && t.isInteger()) {
            text = pad(Long.toString((long) t.getNumber()));
        } else if (t.is(CssToken.DIMENSION) && t.isInteger()) {
            text = pad(Long.toString((long) t.getNumber()) + t.getUnit());
        } else {
            return null;
        }
        // 모든 글자가 16진수 숫자이고 길이가 3 또는 6 이어야 한다(음수는 '-' 때문에 걸러진다).
        if (!(text.length() == 3 || text.length() == 6) || !isHex(text)) {
            return null;
        }
        return new CssToken(CssToken.HASH, text, 0, false, null, false, null, t.getStart());
    }

    /**
     * Pads a text on the left with zeros up to six characters.
     * <p>
     * 문자열의 왼쪽을 0 으로 6자까지 채운다.
     *
     * @param s the text / 문자열
     * @return the padded text, or the text if it is already six or more characters long / 채운 문자열. 이미 6자 이상이면 그대로
     */
    private static String pad(String s) {
        StringBuilder sb = new StringBuilder();
        // 6자가 될 때까지 앞에 0 을 붙인다.
        for (int i = s.length(); i < 6; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }

    /**
     * Tells whether all characters of a text are hexadecimal digits.
     * <p>
     * 문자열의 모든 글자가 16진수 숫자인지 알려 준다.
     *
     * @param s the text / 문자열
     * @return true if it is not empty and all characters are hexadecimal digits / 비어 있지 않고 모두 16진수 숫자이면 true
     */
    private static boolean isHex(String s) {
        // 글자를 하나씩 16진수 숫자인지 확인한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return s.length() > 0;
    }
}
