package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.syntax.DeclarationBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.SyntaxDeclaration;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;

/**
 * The condition of <code>@supports</code> and of <code>CSS.supports()</code>: <code>(property: value)</code> is true when this engine parses the
 * declaration (a property that is merely parsed-only counts as supported, an unsupported one does not), <code>selector(...)</code> when the selector
 * parses, <code>not</code>, <code>and</code> and <code>or</code> combine conditions, and anything else (<code>font-tech()</code>, unknown functions) is
 * false.
 * <p>
 * <code>@supports</code> 와 <code>CSS.supports()</code> 의 조건: <code>(property: value)</code> 는 이 엔진이 선언을 파싱하면 참(파싱만 하는 속성은 지원으로 치고, 지원하지 않는 속성은
 * 거짓), <code>selector(...)</code> 는 선택자가 파싱되면 참, <code>not</code>, <code>and</code>, <code>or</code> 로 조건을 합치며, 그 밖의 것(<code>font-tech()</code>, 알 수 없는 함수)은
 * 거짓이다.
 */
public final class SupportsCondition {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SupportsCondition() {
    }

    /**
     * Evaluates a condition given as text.
     * <p>
     * 문자열로 주어진 조건을 평가한다.
     *
     * @param text the condition, for example <code>(display: grid) and (not (foo: bar))</code> / 조건. 예: <code>(display: grid) and (not (foo: bar))</code>
     * @return true if it holds / 성립하면 true
     */
    public static boolean evaluate(String text) {
        return evaluate(CssParser.parseComponentValues(text == null ? "" : text));
    }

    /**
     * Evaluates the declaration form <code>CSS.supports(property, value)</code>.
     * <p>
     * <code>CSS.supports(property, value)</code> 의 선언 형태를 평가한다.
     *
     * @param property the property name / 속성 이름
     * @param value    the value text / 값 문자열
     * @return true if the declaration is valid / 선언이 올바르면 true
     */
    public static boolean supports(String property, String value) {
        return supports(property, value, false);
    }

    /**
     * Evaluates a declaration of an <code>@supports</code> condition, optionally in the quirks mode of its style sheet: there the properties
     * of the Quirks Mode Standard accept unitless lengths (CSS Values 4 appendix C: <code>&lt;quirky-length&gt;</code> must be valid when
     * parsing the affected properties in the <code>@supports</code> rule, but not in the method <code>CSS.supports()</code>, which calls
     * {@link #supports(String, String)}).
     * <p>
     * <code>@supports</code> 조건의 선언을 평가한다. 스타일 시트가 호환 모드이면 그 모드로 평가하며 호환 모드 표준의 속성은 단위 없는 길이를 받는다(CSS Values 4 부록 C:
     * <code>&lt;quirky-length&gt;</code> 는 <code>@supports</code> 규칙에서는 올바르고 <code>CSS.supports()</code> 메서드에서는 올바르지 않다. 메서드는
     * {@link #supports(String, String)} 을 부른다).
     *
     * @param property the property name / 속성 이름
     * @param value    the value text / 값 문자열
     * @param quirks   true if the style sheet is parsed in quirks mode / 스타일 시트를 호환 모드로 파싱하면 true
     * @return true if the declaration is valid / 선언이 올바르면 true
     */
    public static boolean supports(String property, String value, boolean quirks) {
        if (PropertyRegistry.isCustomProperty(property)) {
            return true;
        }
        Map<Integer, CssValue> out = new LinkedHashMap<Integer, CssValue>();
        return PropertyRegistry.getInstance().parseDeclaration(property, CssParser.parseComponentValues(value),
                new ParseEnv(PropertyRegistry.getInstance(), null, quirks), out) == PropertyRegistry.OK;
    }

    /**
     * Evaluates a condition given as component values.
     * <p>
     * 컴포넌트 값으로 주어진 조건을 평가한다.
     *
     * @param values the condition / 조건
     * @return true if it holds / 성립하면 true
     */
    public static boolean evaluate(List<ComponentValue> values) {
        return evaluate(values, false);
    }

    /**
     * Evaluates a condition given as component values, optionally in the quirks mode of the style sheet that contains it (see
     * {@link #supports(String, String, boolean)}).
     * <p>
     * 컴포넌트 값으로 주어진 조건을 평가한다. 조건을 담은 스타일 시트의 호환 모드로 평가할 수 있다({@link #supports(String, String, boolean)} 참조).
     *
     * @param values the condition / 조건
     * @param quirks true if the style sheet is parsed in quirks mode / 스타일 시트를 호환 모드로 파싱하면 true
     * @return true if it holds / 성립하면 true
     */
    public static boolean evaluate(List<ComponentValue> values, boolean quirks) {
        List<ComponentValue> it = new ArrayList<ComponentValue>();
        // 공백이 아닌 항목만 모은다.
        for (int i = 0; i < values.size(); i++) {
            if (!CssParser.isWhitespace(values.get(i))) {
                it.add(values.get(i));
            }
        }
        if (it.isEmpty()) {
            return false;
        }
        if (isIdent(it.get(0), "not")) {
            return it.size() == 2 && !inParens(it.get(1), quirks);
        }
        boolean result = inParens(it.get(0), quirks);
        String op = null;
        // 연산자와 피연산자가 번갈아 나오며 연산자는 하나로 통일되어야 한다.
        for (int i = 1; i < it.size(); i += 2) {
            if (i + 1 >= it.size() || !(isIdent(it.get(i), "and") || isIdent(it.get(i), "or"))) {
                return false;
            }
            String w = ((CssToken) it.get(i)).getValue().toLowerCase();
            if (op != null && !op.equals(w)) {
                return false;
            }
            op = w;
            boolean r = inParens(it.get(i + 1), quirks);
            result = w.equals("and") ? result && r : result || r;
        }
        return result;
    }

    /**
     * Tells whether a component value is an identifier with a lower-case name.
     * <p>
     * 컴포넌트 값이 주어진 소문자 이름의 식별자인지 알려 준다.
     *
     * @param v    the value / 값
     * @param name the name / 이름
     * @return true if it is / 맞으면 true
     */
    private static boolean isIdent(ComponentValue v, String name) {
        return v instanceof CssToken && ((CssToken) v).is(CssToken.IDENT) && ((CssToken) v).getValue().equalsIgnoreCase(name);
    }

    /**
     * Evaluates <code>font-format(&lt;format&gt;)</code> (CSS Conditional Level 4, 2.3): true for the font formats that <code>@font-face</code> can load
     * here, which are <code>truetype</code>, <code>opentype</code> (a font with CFF outlines is tried and may be refused by the Java runtime) and
     * <code>woff</code> and <code>woff2</code> (the sfnt is rebuilt by the own Brotli and WOFF2 decoders); <code>embedded-opentype</code>, <code>svg</code> and <code>collection</code> are not supported. (There is no
     * <code>font-tech()</code> support: the engine has no OpenType layout features, variations or color fonts, so it is always false.)
     * <p>
     * <code>font-format(&lt;format&gt;)</code> 를 평가한다(CSS Conditional Level 4, 2.3): <code>@font-face</code> 가 여기서 읽을 수 있는 글꼴 형식(<code>truetype</code>,
     * <code>opentype</code>(CFF 윤곽선 글꼴은 시도하며 Java 런타임이 거부할 수 있다), <code>woff</code>, <code>woff2</code>(자체 Brotli 와 WOFF2 디코더가 sfnt 를 다시 만든다))이면 참이고 <code>embedded-opentype</code>,
     * <code>svg</code>, <code>collection</code> 은 지원하지 않는다. (<code>font-tech()</code> 는 지원하지 않는다: 엔진에 OpenType 레이아웃 기능, 가변 글꼴, 색 글꼴이
     * 없으므로 항상 거짓이다.)
     *
     * @param f the function / 함수
     * @return true if the format is supported / 지원하는 형식이면 true
     */
    private static boolean fontFormat(CssFunction f) {
        List<ComponentValue> a = CssParser.trim(f.getArguments());
        if (a.size() != 1 || !(a.get(0) instanceof CssToken) || !((CssToken) a.get(0)).is(CssToken.IDENT)) {
            return false;
        }
        String n = ((CssToken) a.get(0)).getValue().toLowerCase();
        return n.equals("truetype") || n.equals("opentype") || n.equals("woff") || n.equals("woff2");
    }

    /**
     * Evaluates <code>at-rule(@name)</code> (CSS Conditional Level 5): true for the at-rules this engine applies.
     * <p>
     * <code>at-rule(@name)</code> 을 평가한다(CSS Conditional Level 5): 이 엔진이 적용하는 at-규칙이면 참.
     *
     * @param f the function / 함수
     * @return true if the at-rule is supported / 지원하는 at-규칙이면 true
     */
    private static boolean atRule(CssFunction f) {
        List<ComponentValue> a = CssParser.trim(f.getArguments());
        if (a.size() != 1 || !(a.get(0) instanceof CssToken) || !((CssToken) a.get(0)).is(CssToken.AT_KEYWORD)) {
            return false;
        }
        String n = ((CssToken) a.get(0)).getValue().toLowerCase();
        return n.equals("media") || n.equals("supports") || n.equals("import") || n.equals("font-face") || n.equals("keyframes") || n.equals("layer")
                || n.equals("namespace") || n.equals("container") || n.equals("property") || n.equals("counter-style") || n.equals("scope")
                || n.equals("charset");
    }

    /**
     * Evaluates one operand: a parenthesized condition or declaration, or a function.
     * <p>
     * 피연산자 하나를 평가한다: 괄호 안의 조건이나 선언, 또는 함수.
     *
     * @param v      the component value / 컴포넌트 값
     * @param quirks true if the style sheet is parsed in quirks mode / 스타일 시트를 호환 모드로 파싱하면 true
     * @return true if it holds / 성립하면 true
     */
    private static boolean inParens(ComponentValue v, boolean quirks) {
        if (v instanceof CssFunction) {
            CssFunction f = (CssFunction) v;
            if (f.isNamed("selector")) {
                try {
                    SelectorParser.parse(f.getArguments(), null);
                    return true;
                } catch (SelectorSyntaxException e) {
                    return false;
                }
            }
            if (f.isNamed("font-format")) {
                return fontFormat(f);
            }
            if (f.isNamed("at-rule")) {
                return atRule(f);
            }
            return false;
        }
        if (!(v instanceof CssBlock) || ((CssBlock) v).getOpenType() != CssToken.LEFT_PAREN) {
            return false;
        }
        List<ComponentValue> inner = CssParser.trim(((CssBlock) v).getContents());
        if (inner.isEmpty()) {
            return false;
        }
        if (inner.get(0) instanceof CssBlock || isIdent(inner.get(0), "not")) {
            return evaluate(inner, quirks);
        }
        DeclarationBlock b = CssParser.parseDeclarationBlock(inner, null);
        if (b.getDeclarations().size() != 1) {
            return false;
        }
        SyntaxDeclaration d = b.getDeclarations().get(0);
        return supports(d.getName(), CssBlock.join(d.getValue()), quirks);
    }
}
