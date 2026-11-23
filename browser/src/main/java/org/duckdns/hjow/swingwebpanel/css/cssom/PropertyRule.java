package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.values.ValueGrammar;

/**
 * An <code>@property</code> rule (CSS Properties and Values API Level 1, <a href="https://www.w3.org/TR/css-properties-values-api-1/">
 * https://www.w3.org/TR/css-properties-values-api-1/</a>, section 3): the registration of a custom property with a <code>syntax</code> (a string in the
 * value definition syntax of the data types <code>&lt;length&gt;</code>, <code>&lt;number&gt;</code>, <code>&lt;percentage&gt;</code>,
 * <code>&lt;length-percentage&gt;</code>, <code>&lt;color&gt;</code>, <code>&lt;image&gt;</code>, <code>&lt;url&gt;</code>, <code>&lt;integer&gt;</code>,
 * <code>&lt;angle&gt;</code>, <code>&lt;time&gt;</code>, <code>&lt;resolution&gt;</code>, <code>&lt;transform-function&gt;</code>,
 * <code>&lt;transform-list&gt;</code>, <code>&lt;custom-ident&gt;</code>, <code>&lt;string&gt;</code>, keywords, with the combinators <code>|</code>,
 * <code>+</code> and <code>#</code>, or <code>*</code> for anything), whether it <code>inherits</code> and its <code>initial-value</code> (required unless
 * the syntax is <code>*</code>). The rule only holds what was written; the cascade computes the values ({@code css.cascade.RegisteredProperty}), and
 * checks that the initial value does not depend on its context.
 * <p>
 * <code>@property</code> 규칙(CSS Properties and Values API Level 1, <a href="https://www.w3.org/TR/css-properties-values-api-1/">
 * https://www.w3.org/TR/css-properties-values-api-1/</a>, 3 절): 사용자 정의 속성을 <code>syntax</code>(자료형 <code>&lt;length&gt;</code>,
 * <code>&lt;number&gt;</code>, <code>&lt;percentage&gt;</code>, <code>&lt;length-percentage&gt;</code>, <code>&lt;color&gt;</code>, <code>&lt;image&gt;</code>,
 * <code>&lt;url&gt;</code>, <code>&lt;integer&gt;</code>, <code>&lt;angle&gt;</code>, <code>&lt;time&gt;</code>, <code>&lt;resolution&gt;</code>,
 * <code>&lt;transform-function&gt;</code>, <code>&lt;transform-list&gt;</code>, <code>&lt;custom-ident&gt;</code>, <code>&lt;string&gt;</code>, 키워드를
 * <code>|</code>, <code>+</code>, <code>#</code> 로 조합한 값 정의 문법의 문자열, 또는 무엇이든 되는 <code>*</code>), 상속 여부(<code>inherits</code>), 초기값
 * (<code>initial-value</code>, 문법이 <code>*</code> 가 아니면 필수)과 함께 등록한다. 규칙은 쓰인 것만 보관하고, 값의 계산은 캐스케이드가
 * ({@code css.cascade.RegisteredProperty}) 하며 초기값이 문맥에 의존하지 않는지도 거기서 검사한다.
 */
public final class PropertyRule extends CssRule {

    /** The names of the data types the syntax may use. / 문법이 쓸 수 있는 자료형 이름. */
    private static final Pattern TYPE = Pattern.compile("<([a-zA-Z-]+)>");

    /** The allowed type names. / 허용하는 자료형 이름. */
    private static final String[] TYPES = {"length", "number", "percentage", "length-percentage", "color", "image", "url", "integer", "angle", "time",
        "resolution", "transform-function", "transform-list", "custom-ident", "string"};

    /** The property name including the two hyphens. / 하이픈 두 개를 포함한 속성 이름. */
    private final String name;

    /** The syntax string as written, trimmed. / 쓰인 그대로의(앞뒤를 다듬은) 문법 문자열. */
    private final String syntax;

    /** The compiled syntax, or null for <code>*</code>. / 컴파일한 문법. <code>*</code> 이면 null. */
    private final ValueGrammar grammar;

    /** True if the property inherits. / 속성이 상속되면 true. */
    private final boolean inherits;

    /** The tokens of the initial value, or null if there is none. / 초기값의 토큰. 없으면 null. */
    private final List<ComponentValue> initial;

    /**
     * Creates a rule.
     * <p>
     * 규칙을 만든다.
     *
     * @param name    the property name / 속성 이름
     * @param syntax  the syntax string / 문법 문자열
     * @param grammar the compiled syntax or null / 컴파일한 문법 또는 null
     * @param inherits true if the property inherits / 상속되면 true
     * @param initial the initial value tokens or null / 초기값 토큰 또는 null
     */
    private PropertyRule(String name, String syntax, ValueGrammar grammar, boolean inherits, List<ComponentValue> initial) {
        this.name = name;
        this.syntax = syntax;
        this.grammar = grammar;
        this.inherits = inherits;
        this.initial = initial;
    }

    /**
     * Creates the rule from its descriptors, or null if the registration is invalid: the name is not a custom property name, the syntax is missing
     * or malformed, <code>inherits</code> is missing or not <code>true</code>/<code>false</code>, or the initial value is missing for a syntax that
     * is not <code>*</code> or does not match the syntax.
     * <p>
     * 서술자로 규칙을 만든다. 등록이 올바르지 않으면 null: 이름이 사용자 정의 속성 이름이 아니거나, 문법이 없거나 틀렸거나, <code>inherits</code> 가 없거나
     * <code>true</code>/<code>false</code> 가 아니거나, <code>*</code> 가 아닌 문법에 초기값이 없거나 문법과 맞지 않는 경우.
     *
     * @param name     the property name / 속성 이름
     * @param syntax   the text of the <code>syntax</code> descriptor (without the quotes), or null / <code>syntax</code> 서술자의 문자열(따옴표 없이). 없으면 null
     * @param inherits the text of the <code>inherits</code> descriptor, or null / <code>inherits</code> 서술자의 문자열. 없으면 null
     * @param initial  the tokens of <code>initial-value</code>, or null / <code>initial-value</code> 의 토큰. 없으면 null
     * @return the rule or null / 규칙. 올바르지 않으면 null
     */
    public static PropertyRule create(String name, String syntax, String inherits, List<ComponentValue> initial) {
        if (name == null || !name.startsWith("--") || name.length() < 3 || syntax == null || inherits == null) {
            return null;
        }
        boolean inh;
        if (inherits.equalsIgnoreCase("true")) {
            inh = true;
        } else if (inherits.equalsIgnoreCase("false")) {
            inh = false;
        } else {
            return null;
        }
        String s = syntax.trim();
        ValueGrammar g = null;
        if (s.equals("*")) {
            return new PropertyRule(name, s, null, inh, initial);
        }
        if (s.length() == 0) {
            return null;
        }
        Matcher m = TYPE.matcher(s);
        // 문법에 쓰인 자료형이 모두 허용 목록에 있어야 한다.
        while (m.find()) {
            boolean ok = false;
            // 허용 목록과 하나씩 비교한다.
            for (int i = 0; i < TYPES.length; i++) {
                ok |= TYPES[i].equals(m.group(1));
            }
            if (!ok) {
                return null;
            }
        }
        try {
            g = ValueGrammar.compile(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (initial == null) {
            return null;
        }
        return new PropertyRule(name, s, g, inh, initial);
    }

    /**
     * Returns the property name.
     * <p>
     * 속성 이름을 반환한다.
     *
     * @return the name with the hyphens / 하이픈을 포함한 이름
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the syntax string.
     * <p>
     * 문법 문자열을 반환한다.
     *
     * @return the syntax, <code>*</code> for anything / 문법. 무엇이든 되면 <code>*</code>
     */
    public String getSyntax() {
        return syntax;
    }

    /**
     * Returns the compiled syntax.
     * <p>
     * 컴파일한 문법을 반환한다.
     *
     * @return the grammar, or null for <code>*</code> / 문법. <code>*</code> 이면 null
     */
    public ValueGrammar getGrammar() {
        return grammar;
    }

    /**
     * Tells whether the property inherits.
     * <p>
     * 속성이 상속되는지 알려 준다.
     *
     * @return true if it inherits / 상속되면 true
     */
    public boolean isInherits() {
        return inherits;
    }

    /**
     * Returns the tokens of the initial value.
     * <p>
     * 초기값의 토큰을 반환한다.
     *
     * @return the tokens, or null if none was written / 토큰. 쓰이지 않았으면 null
     */
    public List<ComponentValue> getInitial() {
        return initial;
    }

    /**
     * Returns the CSSOM <code>initialValue</code>: the text of the initial value.
     * <p>
     * CSSOM <code>initialValue</code> 를 반환한다: 초기값의 문자열.
     *
     * @return the text, or null if no initial value was written / 문자열. 초기값이 쓰이지 않았으면 null
     */
    public String getInitialValue() {
        return initial == null ? null : CssBlock.join(initial).trim();
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #PROPERTY_RULE} / PROPERTY_RULE
     */
    public int getType() {
        return PROPERTY_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        return "@property " + name + " { syntax: \"" + syntax + "\"; inherits: " + inherits
                + (initial == null ? "" : "; initial-value: " + CssBlock.join(initial).trim()) + "; }";
    }
}
