package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.PropertyRule;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.values.CssRaw;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;
import org.duckdns.hjow.swingwebpanel.css.values.ValueGrammar;

/**
 * A registered custom property (CSS Properties and Values API Level 1, <a href="https://www.w3.org/TR/css-properties-values-api-1/">
 * https://www.w3.org/TR/css-properties-values-api-1/</a>): the registration of an <code>@property</code> rule with its initial value already computed.
 * A registered property differs from an ordinary custom property (a stream of tokens that inherits) in three ways: its value must match the
 * <code>syntax</code> (a value that does not match is invalid at computed-value time and becomes <code>unset</code>), it has a computed value in the
 * canonical form of its type (<code>2em</code> of <code>&lt;length&gt;</code> becomes <code>32px</code> at the element, and that is what the
 * descendants inherit), and it has an initial value and an inheritance flag of its own.
 * <p>
 * 등록된 사용자 정의 속성(CSS Properties and Values API Level 1, <a href="https://www.w3.org/TR/css-properties-values-api-1/">
 * https://www.w3.org/TR/css-properties-values-api-1/</a>): 초기값을 미리 계산해 둔 <code>@property</code> 규칙의 등록. 등록된 속성은 보통의 사용자 정의 속성(상속되는
 * 토큰 열)과 세 가지가 다르다: 값이 <code>syntax</code> 와 맞아야 하고(맞지 않는 값은 계산 시점에 무효가 되어 <code>unset</code> 이 된다), 자료형의 정규 형태의 계산 값을
 * 가지며(<code>&lt;length&gt;</code> 의 <code>2em</code> 은 요소에서 <code>32px</code> 이 되고 자손은 그것을 상속한다), 자신의 초기값과 상속 여부를 가진다.
 */
final class RegisteredProperty {

    /** The name with the two hyphens. / 하이픈 두 개를 포함한 이름. */
    final String name;

    /** True if the property inherits. / 상속되면 true. */
    final boolean inherits;

    /** The compiled syntax, or null for <code>*</code>. / 컴파일한 문법. <code>*</code> 이면 null. */
    private final ValueGrammar grammar;

    /** The computed initial value, or null for the guaranteed-invalid value. / 계산한 초기값. 보증된 무효 값이면 null. */
    final CssRaw initial;

    /**
     * Creates a registration.
     * <p>
     * 등록을 만든다.
     *
     * @param name     the name / 이름
     * @param inherits true if inherited / 상속되면 true
     * @param grammar  the syntax or null / 문법 또는 null
     * @param initial  the computed initial value or null / 계산한 초기값 또는 null
     */
    private RegisteredProperty(String name, boolean inherits, ValueGrammar grammar, CssRaw initial, boolean integers) {
        this.name = name;
        this.inherits = inherits;
        this.grammar = grammar;
        this.initial = initial;
        this.integers = integers;
    }

    /** True if the syntax has <code>&lt;integer&gt;</code>: only then are whole numbers kept as integers when typed values are mixed (a <code>&lt;number&gt;</code> is real). 문법에 <code>&lt;integer&gt;</code> 가 있으면 true. 그때만 형 있는 값을 섞을 때 정수가 정수로 남는다(<code>&lt;number&gt;</code> 는 실수). */
    private final boolean integers;

    /**
     * Makes the registration of a rule, or null if its initial value is not valid for the syntax or depends on its context (it uses a relative unit
     * such as <code>em</code> or <code>vw</code>), which the specification calls "not computationally independent".
     * <p>
     * 규칙의 등록을 만든다. 초기값이 문법에 맞지 않거나 문맥에 의존하면(<code>em</code>, <code>vw</code> 같은 상대 단위를 쓰면) null 이다. 규격은 이것을
     * "computationally independent 가 아니다" 라고 부른다.
     *
     * @param r the rule / 규칙
     * @return the registration or null / 등록. 무효이면 null
     */
    static RegisteredProperty of(PropertyRule r) {
        if (r.getGrammar() == null) {
            // 무엇이든 되는 문법은 초기값이 없어도 되고, 있으면 토큰 그대로이다.
            return new RegisteredProperty(r.getName(), r.isInherits(), null, r.getInitial() == null ? null : new CssRaw(r.getInitial()), false);
        }
        boolean integers = r.getSyntax() != null && r.getSyntax().indexOf("<integer>") >= 0;
        RegisteredProperty probe = new RegisteredProperty(r.getName(), r.isInherits(), r.getGrammar(), null, integers);
        CssRaw a = probe.compute(r.getInitial(), new LengthContext(16, 16, 19.2, 800, 600));
        CssRaw b = probe.compute(r.getInitial(), new LengthContext(20, 24, 30, 1000, 700));
        // 맥락이 다른 두 환경에서 같은 값이 나와야 맥락에 의존하지 않는 초기값이다.
        if (a == null || b == null || !a.toCssText().equals(b.toCssText())) {
            return null;
        }
        return new RegisteredProperty(r.getName(), r.isInherits(), r.getGrammar(), a, integers);
    }

    /**
     * Tells whether the property has a syntax that gives typed values (anything but the universal syntax <code>*</code>): only those can be interpolated.
     * <p>
     * 속성의 문법이 형 있는 값을 주는지(보편 문법 <code>*</code> 이외) 알려 준다. 그런 것만 보간할 수 있다.
     *
     * @return true for a typed syntax / 형 있는 문법이면 true
     */
    boolean isTyped() {
        return grammar != null;
    }

    /**
     * Parses tokens with the syntax of the property into a typed value.
     * <p>
     * 속성의 문법으로 토큰을 형 있는 값으로 파싱한다.
     *
     * @param tokens the tokens / 토큰
     * @return the typed value, or null if the property has the universal syntax or the tokens do not match / 형 있는 값. 보편 문법이거나 맞지 않으면 null
     */
    CssValue typed(List<ComponentValue> tokens) {
        if (grammar == null) {
            return null;
        }
        CssValue v = grammar.match(tokens, new ParseEnv(PropertyRegistry.getInstance(), null));
        return integers ? v : reals(v);
    }

    /**
     * Turns the whole numbers of a value into real numbers (the number <code>1</code> of a <code>&lt;number&gt;</code> property is a real number that happens to
     * be whole, and an interpolation of it must not be rounded).
     * <p>
     * 값의 정수를 실수로 바꾼다(<code>&lt;number&gt;</code> 속성의 숫자 <code>1</code> 은 우연히 정수인 실수이며 그 보간을 반올림하면 안 된다).
     *
     * @param v the value or null / 값. 없으면 null
     * @return the value with real numbers / 실수로 바꾼 값
     */
    private static CssValue reals(CssValue v) {
        if (v instanceof org.duckdns.hjow.swingwebpanel.css.values.CssNumber && ((org.duckdns.hjow.swingwebpanel.css.values.CssNumber) v).isInteger()) {
            return new org.duckdns.hjow.swingwebpanel.css.values.CssNumber(((org.duckdns.hjow.swingwebpanel.css.values.CssNumber) v).getValue(), false);
        }
        if (v instanceof org.duckdns.hjow.swingwebpanel.css.values.CssList) {
            List<CssValue> items = new java.util.ArrayList<CssValue>();
            // 목록의 항목을 하나씩 바꾼다.
            for (CssValue it : ((org.duckdns.hjow.swingwebpanel.css.values.CssList) v).getItems()) {
                items.add(reals(it));
            }
            return new org.duckdns.hjow.swingwebpanel.css.values.CssList(items, ((org.duckdns.hjow.swingwebpanel.css.values.CssList) v).isComma());
        }
        return v;
    }

    /**
     * Computes a value against the syntax: it is matched, relative lengths become pixels and the value is written in its canonical form.
     * <p>
     * 값을 문법에 맞춰 계산한다: 맞추고, 상대 길이는 픽셀이 되고, 값은 정규 형태로 쓴다.
     *
     * @param tokens the value tokens / 값 토큰
     * @param lc     the length context of the element / 요소의 길이 컨텍스트
     * @return the computed value, or null if the value does not match the syntax / 계산 값. 문법과 맞지 않으면 null
     */
    CssRaw compute(List<ComponentValue> tokens, LengthContext lc) {
        if (grammar == null) {
            return new CssRaw(tokens);
        }
        CssValue v = grammar.match(tokens, new ParseEnv(PropertyRegistry.getInstance(), null));
        if (v == null) {
            return null;
        }
        return new CssRaw(CssParser.parseComponentValues(StyleComputer.map(v, lc).toCssText()));
    }

    /**
     * Applies the registrations to the custom properties of an element: a declared value is computed against the syntax (an invalid one becomes
     * <code>unset</code>), an undeclared or unset value is the inherited value (if the property inherits and the parent has one) or the initial value,
     * and the CSS-wide keywords have their usual meaning.
     * <p>
     * 요소의 사용자 정의 속성에 등록을 적용한다: 선언된 값은 문법에 맞춰 계산하고(맞지 않으면 <code>unset</code>), 선언되지 않았거나 unset 인 값은 (속성이 상속되고
     * 부모에 값이 있으면) 상속값이고 아니면 초기값이며, CSS 전역 키워드는 보통의 의미를 가진다.
     *
     * @param raw      the custom properties with <code>var()</code> substituted, by name / <code>var()</code> 를 치환한 이름별 사용자 정의 속성
     * @param parent   the custom properties of the parent (already computed) / 부모의 사용자 정의 속성(이미 계산됨)
     * @param declared the declared custom properties of the element / 요소에 선언된 사용자 정의 속성
     * @param reg      the registrations by name / 이름별 등록
     * @param lc       the length context of the element / 요소의 길이 컨텍스트
     * @return the computed custom properties / 계산한 사용자 정의 속성
     */
    static Map<String, CssRaw> apply(Map<String, CssRaw> raw, Map<String, CssRaw> parent, Map<String, List<ComponentValue>> declared,
            Map<String, RegisteredProperty> reg, LengthContext lc) {
        Map<String, CssRaw> out = new LinkedHashMap<String, CssRaw>(raw);
        // 등록된 속성마다 값을 정한다(선언이 없는 속성도 초기값을 가진다).
        for (RegisteredProperty p : reg.values()) {
            CssRaw v;
            CssRaw r = declared.containsKey(p.name) ? raw.get(p.name) : null;
            String kw = r == null ? null : keyword(r);
            if (r == null || kw != null && (kw.equals("unset") || kw.equals("revert") || kw.equals("revert-layer"))) {
                v = p.unsetValue(parent);
            } else if (kw != null && kw.equals("initial")) {
                v = p.initial;
            } else if (kw != null && kw.equals("inherit")) {
                v = parent.get(p.name);
            } else {
                v = p.compute(r.getTokens(), lc);
                if (v == null) {
                    v = p.unsetValue(parent);
                }
            }
            if (v == null) {
                out.remove(p.name);
            } else {
                out.put(p.name, v);
            }
        }
        return out;
    }

    /**
     * Returns the value of the property when it is <code>unset</code>: the inherited value for an inherited property that the parent has, the initial
     * value otherwise.
     * <p>
     * 속성이 <code>unset</code> 일 때의 값을 반환한다: 상속되는 속성이고 부모에 값이 있으면 상속값, 아니면 초기값.
     *
     * @param parent the custom properties of the parent / 부모의 사용자 정의 속성
     * @return the value or null / 값 또는 null
     */
    private CssRaw unsetValue(Map<String, CssRaw> parent) {
        if (inherits && parent.containsKey(name)) {
            return parent.get(name);
        }
        return initial;
    }

    /**
     * Returns the keyword of a value that is only a CSS-wide keyword.
     * <p>
     * 값이 CSS 전역 키워드 하나뿐이면 그 키워드를 반환한다.
     *
     * @param r the value / 값
     * @return the lower-case keyword or null / 소문자 키워드. 아니면 null
     */
    private static String keyword(CssRaw r) {
        List<ComponentValue> t = CssParser.trim(r.getTokens());
        if (t.size() != 1 || !(t.get(0) instanceof CssToken) || !((CssToken) t.get(0)).is(CssToken.IDENT)) {
            return null;
        }
        String k = ((CssToken) t.get(0)).getValue().toLowerCase();
        return k.equals("initial") || k.equals("inherit") || k.equals("unset") || k.equals("revert") || k.equals("revert-layer") ? k : null;
    }
}
