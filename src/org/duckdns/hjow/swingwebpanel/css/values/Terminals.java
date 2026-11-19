package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * The built-in value types of the grammar (<code>&lt;length&gt;</code>, <code>&lt;percentage&gt;</code>, <code>&lt;color&gt;</code>,
 * <code>&lt;url&gt;</code>, ...). Each type matches exactly one component value and returns the typed {@link CssValue}, or null. Math functions
 * (<code>calc()</code> and friends) are accepted wherever the type of their result fits.
 * <p>
 * 문법의 기본 값 형식(<code>&lt;length&gt;</code>, <code>&lt;percentage&gt;</code>, <code>&lt;color&gt;</code>, <code>&lt;url&gt;</code> 등). 각 형식은 컴포넌트 값
 * 하나와 일치하며 형식이 있는 {@link CssValue} 나 null 을 반환한다. 수학 함수(<code>calc()</code> 등)는 결과 형식이 맞는 곳에서 받아들인다.
 */
final class Terminals {

    /** Names of the built-in types. / 기본 형식의 이름. */
    private static final Set<String> TYPES = new HashSet<String>();

    static {
        String[] names = {"length", "percentage", "length-percentage", "number", "integer", "angle", "time", "frequency", "resolution", "flex",
            "color", "url", "string", "ident", "custom-ident", "dashed-ident", "image", "any", "zero", "unicode-range", "generic-function", "anchor-function"};
        // 기본 형식 이름을 집합에 넣는다.
        for (int i = 0; i < names.length; i++) {
            TYPES.add(names[i]);
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Terminals() {
    }

    /**
     * Tells whether a name is a built-in type.
     * <p>
     * 이름이 기본 형식인지 알려 준다.
     *
     * @param name type name / 형식 이름
     * @return true for built-in types / 기본 형식이면 true
     */
    static boolean isType(String name) {
        return TYPES.contains(name);
    }

    /**
     * Matches one component value against a built-in type.
     * <p>
     * 컴포넌트 값 하나를 기본 형식에 맞춘다.
     *
     * @param type type name / 형식 이름
     * @param min  minimum accepted number / 허용하는 최소 숫자
     * @param max  maximum accepted number / 허용하는 최대 숫자
     * @param v    the component value / 컴포넌트 값
     * @param env  parse environment / 파싱 환경
     * @return the typed value, or null if it does not match / 형식이 있는 값. 맞지 않으면 null
     */
    static CssValue match(String type, double min, double max, ComponentValue v, ParseEnv env) {
        CssToken t = v instanceof CssToken ? (CssToken) v : null;
        CssCalc calc = null;
        if (v instanceof CssFunction && CssCalc.isMathFunction(((CssFunction) v).getName())) {
            calc = CssCalc.parse((CssFunction) v);
            if (calc == null) {
                return null;
            }
        }
        if (type.equals("length")) {
            return length(t, calc, min, max, false);
        }
        if (type.equals("length-percentage")) {
            CssValue l = length(t, calc, min, max, true);
            if (l != null) {
                return l;
            }
            return percentage(t, calc, min, max, true);
        }
        if (type.equals("percentage")) {
            return percentage(t, calc, min, max, false);
        }
        if (type.equals("number") || type.equals("integer")) {
            return number(t, calc, min, max, type.equals("integer"));
        }
        if (type.equals("zero")) {
            return t != null && t.is(CssToken.NUMBER) && t.getNumber() == 0 ? new CssNumber(0, true) : null;
        }
        if (type.equals("angle") || type.equals("time") || type.equals("frequency") || type.equals("resolution") || type.equals("flex")) {
            return dimension(type, t, calc, min, max);
        }
        if (type.equals("color")) {
            return CssColor.parseValue(v);
        }
        if (type.equals("url")) {
            return url(v, env);
        }
        if (type.equals("string")) {
            return t != null && t.is(CssToken.STRING) ? new CssString(t.getValue()) : null;
        }
        if (type.equals("ident")) {
            return t != null && t.is(CssToken.IDENT) ? new CssKeyword(t.getValue()) : null;
        }
        if (type.equals("custom-ident")) {
            return customIdent(t);
        }
        if (type.equals("dashed-ident")) {
            return t != null && t.is(CssToken.IDENT) && t.getValue().startsWith("--") ? new CssKeyword(t.getValue()) : null;
        }
        if (type.equals("image")) {
            return image(v, env);
        }
        if (type.equals("unicode-range")) {
            return t != null && t.is(CssToken.UNICODE_RANGE) ? new CssKeyword(t.getValue()) : null;
        }
        if (type.equals("generic-function")) {
            return v instanceof CssFunction ? generic(v, env) : null;
        }
        if (type.equals("anchor-function")) {
            // anchor() 와 anchor-size() 만 받는다(인수는 배치할 때 앵커를 찾아 푼다. CSS Anchor Positioning 1).
            String fn = v instanceof CssFunction ? CssKeyword.lower(((CssFunction) v).getName()) : "";
            return fn.equals("anchor") || fn.equals("anchor-size") ? generic(v, env) : null;
        }
        return generic(v, env);
    }

    /**
     * Matches a length.
     * <p>
     * 길이를 맞춘다.
     *
     * @param t      the token or null / 토큰 또는 null
     * @param calc   the math function value or null / 수학 함수 값 또는 null
     * @param min    minimum / 최소
     * @param max    maximum / 최대
     * @param withPc true if a calc with a percentage is acceptable / 백분율이 든 calc 도 허용하면 true
     * @return the length or null / 길이 또는 null
     */
    private static CssValue length(CssToken t, CssCalc calc, double min, double max, boolean withPc) {
        if (calc != null) {
            return calc.isLength() || (withPc && calc.isLengthPercentage()) ? calc : null;
        }
        if (t == null) {
            return null;
        }
        if (t.is(CssToken.DIMENSION) && CssUnits.isLength(CssKeyword.lower(t.getUnit()))) {
            return t.getNumber() >= min && t.getNumber() <= max ? new CssLength(t.getNumber(), t.getUnit()) : null;
        }
        if (t.is(CssToken.NUMBER) && t.getNumber() == 0) {
            return min <= 0 ? CssLength.ZERO : null;
        }
        return null;
    }

    /**
     * Matches a percentage.
     * <p>
     * 백분율을 맞춘다.
     *
     * @param t      the token or null / 토큰 또는 null
     * @param calc   the math function value or null / 수학 함수 값 또는 null
     * @param min    minimum / 최소
     * @param max    maximum / 최대
     * @param withPc true if a calc mixing lengths is acceptable / 길이가 섞인 calc 도 허용하면 true
     * @return the percentage or null / 백분율 또는 null
     */
    private static CssValue percentage(CssToken t, CssCalc calc, double min, double max, boolean withPc) {
        if (calc != null) {
            return calc.isPercentage() || (withPc && calc.isLengthPercentage()) ? calc : null;
        }
        if (t != null && t.is(CssToken.PERCENTAGE) && t.getNumber() >= min && t.getNumber() <= max) {
            return new CssPercentage(t.getNumber());
        }
        return null;
    }

    /**
     * Matches a number or an integer.
     * <p>
     * 숫자나 정수를 맞춘다.
     *
     * @param t       the token or null / 토큰 또는 null
     * @param calc    the math function value or null / 수학 함수 값 또는 null
     * @param min     minimum / 최소
     * @param max     maximum / 최대
     * @param integer true to require an integer / 정수를 요구하면 true
     * @return the number or null / 숫자 또는 null
     */
    private static CssValue number(CssToken t, CssCalc calc, double min, double max, boolean integer) {
        if (calc != null) {
            return calc.isNumber() ? calc : null;
        }
        if (t == null || !t.is(CssToken.NUMBER) || (integer && !t.isInteger())) {
            return null;
        }
        return t.getNumber() >= min && t.getNumber() <= max ? new CssNumber(t.getNumber(), t.isInteger()) : null;
    }

    /**
     * Matches an angle, time, frequency, resolution or flex value.
     * <p>
     * 각도, 시간, 주파수, 해상도, flex 값을 맞춘다.
     *
     * @param type the type name / 형식 이름
     * @param t    the token or null / 토큰 또는 null
     * @param calc the math function value or null / 수학 함수 값 또는 null
     * @param min  minimum / 최소
     * @param max  maximum / 최대
     * @return the value or null / 값 또는 null
     */
    private static CssValue dimension(String type, CssToken t, CssCalc calc, double min, double max) {
        if (calc != null) {
            return (type.equals("angle") && calc.isAngle()) || (type.equals("time") && calc.isTime()) ? calc : null;
        }
        if (t == null || !t.is(CssToken.DIMENSION)) {
            return null;
        }
        int want = type.equals("angle") ? CssUnits.ANGLE : type.equals("time") ? CssUnits.TIME : type.equals("frequency") ? CssUnits.FREQUENCY
                : type.equals("resolution") ? CssUnits.RESOLUTION : CssUnits.FLEX;
        if (CssUnits.kindOf(t.getUnit()) != want || t.getNumber() < min || t.getNumber() > max) {
            return null;
        }
        return new CssDimension(t.getNumber(), t.getUnit());
    }

    /**
     * Matches a custom identifier (not a CSS-wide keyword and not <code>default</code>).
     * <p>
     * 사용자 정의 식별자(CSS 전역 키워드와 <code>default</code> 제외)를 맞춘다.
     *
     * @param t the token or null / 토큰 또는 null
     * @return the keyword or null / 키워드 또는 null
     */
    private static CssValue customIdent(CssToken t) {
        if (t == null || !t.is(CssToken.IDENT)) {
            return null;
        }
        String l = CssKeyword.lower(t.getValue());
        if (l.equals("inherit") || l.equals("initial") || l.equals("unset") || l.equals("revert") || l.equals("revert-layer")
                || l.equals("default")) {
            return null;
        }
        return new CssKeyword(t.getValue());
    }

    /**
     * Matches a URL: the <code>url()</code> token or <code>url("...")</code> with a string.
     * <p>
     * URL 을 맞춘다: <code>url()</code> 토큰 또는 문자열을 쓴 <code>url("...")</code>.
     *
     * @param v   the component value / 컴포넌트 값
     * @param env parse environment / 파싱 환경
     * @return the url value or null / url 값 또는 null
     */
    private static CssValue url(ComponentValue v, ParseEnv env) {
        if (v instanceof CssToken && ((CssToken) v).is(CssToken.URL)) {
            String u = ((CssToken) v).getValue();
            return new CssUrl(u, env.resolve(u));
        }
        if (v instanceof CssFunction && ((CssFunction) v).isNamed("url")) {
            List<ComponentValue> a = CssParser.trim(((CssFunction) v).getArguments());
            if (a.size() >= 1 && a.get(0) instanceof CssToken && ((CssToken) a.get(0)).is(CssToken.STRING)) {
                String u = ((CssToken) a.get(0)).getValue();
                return new CssUrl(u, env.resolve(u));
            }
        }
        return null;
    }

    /**
     * Matches an image: a url or one of the image functions (gradients, image-set, cross-fade, element, image).
     * <p>
     * 이미지를 맞춘다: url 이나 이미지 함수(그라디언트, image-set, cross-fade, element, image).
     *
     * @param v   the component value / 컴포넌트 값
     * @param env parse environment / 파싱 환경
     * @return the image value or null / 이미지 값 또는 null
     */
    private static CssValue image(ComponentValue v, ParseEnv env) {
        CssValue u = url(v, env);
        if (u != null) {
            return u;
        }
        if (v instanceof CssFunction) {
            String n = CssKeyword.lower(((CssFunction) v).getName());
            if (n.endsWith("-gradient") || n.equals("image-set") || n.equals("-webkit-image-set")) {
                return generic(v, env);
            }
            // CSS Images 4 의 함수는 문법을 확인해 틀리면 선언 전체를 무효로 한다(2.5~2.7 절).
            if (n.equals("cross-fade")) {
                return crossFade((CssFunction) v, env);
            }
            if (n.equals("-webkit-cross-fade")) {
                return legacyCrossFade((CssFunction) v, env);
            }
            if (n.equals("image")) {
                return imageFunction((CssFunction) v, env);
            }
            if (n.equals("element")) {
                return elementFunction((CssFunction) v);
            }
        }
        return null;
    }

    /**
     * Reads <code>cross-fade( &lt;cf-image&gt;# )</code> with <code>&lt;cf-image&gt; = [ &lt;image&gt; | &lt;color&gt; ] &amp;&amp; &lt;percentage [0,100]&gt;?</code>
     * (CSS Images Level 4 section 2.6): every argument is one image or one color and at most one percentage between 0% and 100% in either order. The arguments
     * keep the form in which they were written; the omitted percentages are filled in when the image is painted.
     * <p>
     * <code>cross-fade( &lt;cf-image&gt;# )</code> 를 읽는다(CSS Images Level 4 2.6 절, <code>&lt;cf-image&gt; = [ &lt;image&gt; | &lt;color&gt; ] &amp;&amp;
     * &lt;percentage [0,100]&gt;?</code>): 인수마다 이미지나 색 하나와 0%~100% 의 백분율 많아야 하나가 어느 순서로든 온다. 인수는 쓴 모양 그대로 두고, 빠진 백분율은
     * 이미지를 그릴 때 채운다.
     *
     * @param f   the function / 함수
     * @param env parse environment / 파싱 환경
     * @return the value, or null if the function is not valid / 값. 함수가 올바르지 않으면 null
     */
    private static CssValue crossFade(CssFunction f, ParseEnv env) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(f.getArguments());
        List<CssValue> out = new ArrayList<CssValue>();
        // 쉼표로 나눈 인수마다 이미지(또는 색) 하나와 선택적인 백분율을 읽는다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> items = CssParser.trim(parts.get(i));
            List<ComponentValue> part = new ArrayList<ComponentValue>();
            // 공백을 뺀 성분만 모은다.
            for (int k = 0; k < items.size(); k++) {
                if (!CssParser.isWhitespace(items.get(k))) {
                    part.add(items.get(k));
                }
            }
            if (part.isEmpty() || part.size() > 2) {
                return null;
            }
            CssValue picture = null;
            CssValue amount = null;
            List<CssValue> written = new ArrayList<CssValue>();
            // 성분마다 백분율이면 양으로, 아니면 이미지나 색으로 읽는다(한 종류씩만).
            for (int k = 0; k < part.size(); k++) {
                ComponentValue c = part.get(k);
                if (c instanceof CssToken && ((CssToken) c).is(CssToken.PERCENTAGE)) {
                    double p = ((CssToken) c).getNumber();
                    if (amount != null || p < 0 || p > 100) {
                        return null;
                    }
                    amount = new CssPercentage(p);
                    written.add(amount);
                } else {
                    CssValue img = image(c, env);
                    if (img == null) {
                        img = CssColor.parseValue(c);
                    }
                    if (img == null || picture != null) {
                        return null;
                    }
                    picture = img;
                    written.add(img);
                }
            }
            if (picture == null) {
                return null;
            }
            out.add(written.size() == 1 ? written.get(0) : new CssList(written, false));
        }
        return out.isEmpty() ? null : new CssFunctionValue("cross-fade", out);
    }

    /**
     * Reads the legacy <code>-webkit-cross-fade(&lt;image&gt;, &lt;image&gt;, &lt;percentage&gt; | &lt;number&gt;)</code>: the third argument is how much of the second
     * image is kept (a number from 0 to 1 or a percentage from 0% to 100%).
     * <p>
     * 옛 형식 <code>-webkit-cross-fade(&lt;image&gt;, &lt;image&gt;, &lt;percentage&gt; | &lt;number&gt;)</code> 을 읽는다: 셋째 인수는 둘째 이미지를 남기는 양이다(0~1 의 수나
     * 0%~100% 의 백분율).
     *
     * @param f   the function / 함수
     * @param env parse environment / 파싱 환경
     * @return the value or null / 값 또는 null
     */
    private static CssValue legacyCrossFade(CssFunction f, ParseEnv env) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(f.getArguments());
        if (parts.size() != 3) {
            return null;
        }
        List<CssValue> out = new ArrayList<CssValue>();
        // 앞의 두 인수는 이미지 하나씩이다.
        for (int i = 0; i < 2; i++) {
            List<ComponentValue> p = CssParser.trim(parts.get(i));
            CssValue img = p.size() == 1 ? image(p.get(0), env) : null;
            if (img == null) {
                return null;
            }
            out.add(img);
        }
        List<ComponentValue> last = CssParser.trim(parts.get(2));
        if (last.size() != 1 || !(last.get(0) instanceof CssToken)) {
            return null;
        }
        CssToken t = (CssToken) last.get(0);
        double amount;
        if (t.is(CssToken.PERCENTAGE)) {
            amount = t.getNumber() / 100.0;
        } else if (t.is(CssToken.NUMBER)) {
            amount = t.getNumber();
        } else {
            return null;
        }
        if (amount < 0 || amount > 1) {
            return null;
        }
        out.add(t.is(CssToken.PERCENTAGE) ? (CssValue) new CssPercentage(t.getNumber()) : new CssNumber(t.getNumber(), t.isInteger()));
        return new CssFunctionValue("-webkit-cross-fade", out);
    }

    /**
     * Reads <code>image( &lt;image-tags&gt;? [ &lt;image-src&gt;? , &lt;color&gt;? ]! )</code> (CSS Images Level 4 section 2.5): an optional direction
     * <code>ltr</code> or <code>rtl</code>, then a URL (a <code>url()</code> or a string, resolved like any URL), a color, or both separated by a comma.
     * <p>
     * <code>image( &lt;image-tags&gt;? [ &lt;image-src&gt;? , &lt;color&gt;? ]! )</code> 를 읽는다(CSS Images Level 4 2.5 절): 선택적인 방향 <code>ltr</code> 이나
     * <code>rtl</code>, 그다음 URL(<code>url()</code> 이나 문자열. 여느 URL 처럼 해석), 색, 또는 쉼표로 나눈 둘 다.
     *
     * @param f   the function / 함수
     * @param env parse environment / 파싱 환경
     * @return the value or null / 값 또는 null
     */
    private static CssValue imageFunction(CssFunction f, ParseEnv env) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(f.getArguments());
        if (parts.isEmpty() || parts.size() > 2) {
            return null;
        }
        List<ComponentValue> first = new ArrayList<ComponentValue>();
        List<ComponentValue> raw = parts.get(0);
        // 첫 묶음에서 공백이 아닌 성분만 모은다.
        for (int k = 0; k < raw.size(); k++) {
            if (!CssParser.isWhitespace(raw.get(k))) {
                first.add(raw.get(k));
            }
        }
        List<CssValue> head = new ArrayList<CssValue>();
        int at = 0;
        // 방향 꼬리표(ltr, rtl)가 맨 앞에 올 수 있다.
        if (!first.isEmpty() && first.get(0) instanceof CssToken && (((CssToken) first.get(0)).isIdent("ltr") || ((CssToken) first.get(0)).isIdent("rtl"))) {
            head.add(new CssKeyword(CssKeyword.lower(((CssToken) first.get(0)).getValue())));
            at = 1;
        }
        CssValue src = null;
        CssValue color = null;
        if (at < first.size()) {
            if (first.size() - at != 1) {
                return null;
            }
            ComponentValue c = first.get(at);
            src = url(c, env);
            if (src == null && c instanceof CssToken && ((CssToken) c).is(CssToken.STRING)) {
                String u = ((CssToken) c).getValue();
                src = new CssUrl(u, env.resolve(u));
            }
            if (src == null && parts.size() == 1) {
                // 쉼표가 없으면 하나뿐인 성분은 URL 이 아니면 색이다.
                color = CssColor.parseValue(c);
                if (color == null) {
                    return null;
                }
            } else if (src == null) {
                return null;
            }
        }
        if (parts.size() == 2) {
            if (src == null) {
                // 쉼표 앞에는 URL 이 있어야 한다(쉼표는 빠진 URL 옆에서 생략된다).
                return null;
            }
            List<ComponentValue> second = CssParser.trim(parts.get(1));
            if (second.size() != 1) {
                return null;
            }
            color = CssColor.parseValue(second.get(0));
            if (color == null) {
                return null;
            }
        }
        if (src == null && color == null) {
            return null;
        }
        List<CssValue> out = new ArrayList<CssValue>();
        if (src != null) {
            head.add(src);
        } else {
            head.add(color);
            color = null;
        }
        out.add(head.size() == 1 ? head.get(0) : new CssList(head, false));
        if (color != null) {
            out.add(color);
        }
        return new CssFunctionValue("image", out);
    }

    /**
     * Reads <code>element( &lt;id-selector&gt; )</code> (CSS Images Level 4 section 2.7): exactly one ID selector, kept as the text <code>#id</code>.
     * <p>
     * <code>element( &lt;id-selector&gt; )</code> 를 읽는다(CSS Images Level 4 2.7 절): ID 선택자 정확히 하나이며 <code>#id</code> 텍스트로 둔다.
     *
     * @param f the function / 함수
     * @return the value or null / 값 또는 null
     */
    private static CssValue elementFunction(CssFunction f) {
        List<ComponentValue> a = CssParser.trim(f.getArguments());
        if (a.size() != 1 || !(a.get(0) instanceof CssToken)) {
            return null;
        }
        CssToken t = (CssToken) a.get(0);
        if (!t.is(CssToken.HASH) || !t.isHashId()) {
            return null;
        }
        List<CssValue> out = new ArrayList<CssValue>();
        out.add(new CssRaw(single(t)));
        return new CssFunctionValue("element", out);
    }

    /**
     * Converts any component value to a loosely typed value (used for function arguments that are not validated further).
     * <p>
     * 임의의 컴포넌트 값을 느슨한 형식의 값으로 바꾼다(더 검증하지 않는 함수 인수에 쓴다).
     *
     * @param v   the component value / 컴포넌트 값
     * @param env parse environment / 파싱 환경
     * @return the value (never null) / 값(null 아님)
     */
    static CssValue generic(ComponentValue v, ParseEnv env) {
        if (v instanceof CssToken) {
            CssToken t = (CssToken) v;
            switch (t.getType()) {
            case CssToken.NUMBER:
                return new CssNumber(t.getNumber(), t.isInteger());
            case CssToken.PERCENTAGE:
                return new CssPercentage(t.getNumber());
            case CssToken.DIMENSION:
                return CssUnits.isLength(CssKeyword.lower(t.getUnit())) ? (CssValue) new CssLength(t.getNumber(), t.getUnit())
                        : new CssDimension(t.getNumber(), t.getUnit());
            case CssToken.STRING:
                return new CssString(t.getValue());
            case CssToken.URL:
                return new CssUrl(t.getValue(), env.resolve(t.getValue()));
            case CssToken.IDENT: {
                CssValue c = CssColor.parseValue(v);
                return c != null ? c : new CssKeyword(t.getValue());
            }
            case CssToken.HASH: {
                CssValue c = CssColor.parseValue(v);
                return c != null ? c : new CssRaw(single(v));
            }
            case CssToken.COMMA:
                return CssOperator.COMMA;
            default:
                if (t.isDelim('/')) {
                    return CssOperator.SLASH;
                }
                return new CssRaw(single(v));
            }
        }
        if (v instanceof CssFunction) {
            CssFunction f = (CssFunction) v;
            if (CssCalc.isMathFunction(f.getName())) {
                CssCalc c = CssCalc.parse(f);
                if (c != null) {
                    return c;
                }
            }
            CssValue color = CssColor.parseValue(v);
            if (color != null) {
                return color;
            }
            CssValue u = url(v, env);
            if (u != null) {
                return u;
            }
            return new CssFunctionValue(f.getName(), genericArguments(f.getArguments(), env));
        }
        return new CssRaw(single(v));
    }

    /**
     * Wraps one component value in a list.
     * <p>
     * 컴포넌트 값 하나를 목록으로 감싼다.
     *
     * @param v the component value / 컴포넌트 값
     * @return a one-item list / 한 항목짜리 목록
     */
    private static List<ComponentValue> single(ComponentValue v) {
        List<ComponentValue> l = new ArrayList<ComponentValue>();
        l.add(v);
        return l;
    }

    /**
     * Converts function arguments: comma separated groups, each a value or a space separated list.
     * <p>
     * 함수 인수를 바꾼다: 쉼표로 나눈 묶음마다 값 하나 또는 공백으로 구분한 목록.
     *
     * @param args the arguments / 인수
     * @param env  parse environment / 파싱 환경
     * @return the argument values / 인수 값
     */
    static List<CssValue> genericArguments(List<ComponentValue> args, ParseEnv env) {
        List<CssValue> out = new ArrayList<CssValue>();
        List<List<ComponentValue>> parts = CssParser.splitCommas(args);
        // 쉼표로 나눈 묶음마다 공백이 아닌 값들을 모아 값 하나나 목록으로 만든다.
        for (int i = 0; i < parts.size(); i++) {
            List<CssValue> items = new ArrayList<CssValue>();
            List<ComponentValue> part = parts.get(i);
            // 묶음 안의 공백이 아닌 값을 느슨한 값으로 바꾼다.
            for (int j = 0; j < part.size(); j++) {
                if (!CssParser.isWhitespace(part.get(j))) {
                    items.add(generic(part.get(j), env));
                }
            }
            if (items.size() == 1) {
                out.add(items.get(0));
            } else if (items.size() > 1 || parts.size() > 1) {
                out.add(new CssList(items, false));
            }
        }
        return out;
    }
}
