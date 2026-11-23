package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.cssom.SupportsCondition;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;

/**
 * The <code>CSS</code> namespace object of the CSSOM (CSSOM section 6.6, <code>CSS.escape</code>, and CSS Conditional Rules Module Level 3 section 6,
 * <code>CSS.supports</code>, https://www.w3.org/TR/css-conditional-3/#the-css-namespace): <code>CSS.supports(property, value)</code> tells whether the
 * engine accepts a declaration, <code>CSS.supports(condition)</code> evaluates a condition as <code>@supports</code> does (the condition may be
 * written without the outer parentheses, <code>CSS.supports("display: grid")</code>), and <code>CSS.escape(text)</code> serializes text as an
 * identifier. Both share the engine's own grammar tables, so they agree with what a style sheet does with the same text.
 * <p>
 * CSSOM 의 <code>CSS</code> 네임스페이스 객체(CSSOM 6.6 절의 <code>CSS.escape</code> 와 CSS Conditional Rules Module Level 3 6 절의 <code>CSS.supports</code>,
 * https://www.w3.org/TR/css-conditional-3/#the-css-namespace): <code>CSS.supports(property, value)</code> 는 엔진이 선언을 받아들이는지 알려 주고,
 * <code>CSS.supports(condition)</code> 은 <code>@supports</code> 처럼 조건을 평가하며(조건은 바깥 괄호 없이 쓸 수 있다, <code>CSS.supports("display: grid")</code>),
 * <code>CSS.escape(text)</code> 는 텍스트를 식별자로 직렬화한다. 둘 다 엔진 자신의 문법 표를 쓰므로 스타일 시트가 같은 텍스트로 하는 일과 일치한다.
 */
final class CssNamespaceApi {

    /** The page. / 페이지. */
    private final PageScripts page;

    /**
     * Creates the installer of a page.
     * <p>
     * 페이지의 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    CssNamespaceApi(PageScripts page) {
        this.page = page;
    }

    /**
     * Evaluates <code>CSS.supports(condition)</code>: the text as a supports condition, and if that does not hold, as a condition in parentheses
     * (the form without parentheses is a single declaration condition).
     * <p>
     * <code>CSS.supports(condition)</code> 을 평가한다: 텍스트를 supports 조건으로, 그것이 성립하지 않으면 괄호로 감싼 조건으로 평가한다(괄호 없는 형태는 선언 조건
     * 하나이다).
     *
     * @param text the condition text / 조건 문자열
     * @return true if the condition holds / 조건이 성립하면 true
     */
    static boolean supportsCondition(String text) {
        return SupportsCondition.evaluate(text) || SupportsCondition.evaluate("(" + text + ")");
    }

    /**
     * Evaluates <code>CSS.supports(property, value)</code>: the property name is case insensitive unless it is a custom property, and an empty
     * value is never supported.
     * <p>
     * <code>CSS.supports(property, value)</code> 를 평가한다: 속성 이름은 사용자 정의 속성이 아니면 대소문자를 구분하지 않고, 빈 값은 지원하지 않는다.
     *
     * @param property the property name / 속성 이름
     * @param value    the value text / 값 문자열
     * @return true if the declaration is supported / 선언을 지원하면 true
     */
    static boolean supportsDeclaration(String property, String value) {
        if (value.trim().length() == 0) {
            return false;
        }
        return SupportsCondition.supports(property.startsWith("--") ? property : property.toLowerCase(java.util.Locale.ENGLISH), value);
    }

    /**
     * Installs the <code>CSS</code> object on the global object.
     * <p>
     * <code>CSS</code> 객체를 전역 객체에 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        JsObject css = realm.newObject();
        css.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("CSS"), false, false, true);
        css.defineData("supports", realm.newFunction("supports", 1, new NativeFunction.Call() {
            /** Evaluates the one-argument condition form or the two-argument declaration form. / 인수 하나의 조건 형태나 인수 둘의 선언 형태를 평가한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'supports' on 'CSS': 1 argument required, but only 0 present.");
                }
                if (args.length == 1) {
                    return Js.bool(supportsCondition(Ops.toString(args[0])));
                }
                return Js.bool(supportsDeclaration(Ops.toString(args[0]), Ops.toString(args[1])));
            }
        }), true, true, true);
        css.defineData("escape", realm.newFunction("escape", 1, new NativeFunction.Call() {
            /** Serializes the text as an identifier. / 텍스트를 식별자로 직렬화한다. */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (args.length == 0) {
                    throw JsException.typeError("Failed to execute 'escape' on 'CSS': 1 argument required, but only 0 present.");
                }
                return Js.str(CssEscape.ident(Ops.toString(args[0])));
            }
        }), true, true, true);
        // CSS Custom Highlight API 의 Highlight, HighlightRegistry 와 CSS.highlights 를 설치한다.
        new HighlightApi(page).install(css);
        g.defineData("CSS", css, true, false, true);
    }
}
