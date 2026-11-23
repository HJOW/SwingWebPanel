package org.duckdns.hjow.swingwebpanel.css.media;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssUnits;

/**
 * A parsed media query list (Media Queries Level 4): media types with <code>not</code> and <code>only</code>, <code>and</code> / <code>or</code> /
 * <code>not</code> conditions with parentheses, features in boolean (<code>(color)</code>), plain (<code>(min-width: 600px)</code>) and range
 * (<code>(400px &lt;= width &lt; 700px)</code>) form, and unknown features or functions (which are false and cannot be negated into true). A query that
 * cannot be parsed is replaced by <code>not all</code>, as the specification says, without affecting the other queries of the list. The list matches an
 * environment when any of its queries does.
 * <p>
 * 파싱한 미디어 질의 목록(Media Queries Level 4): <code>not</code>, <code>only</code> 가 붙은 미디어 종류, 괄호가 있는 <code>and</code> / <code>or</code> /
 * <code>not</code> 조건, 불리언(<code>(color)</code>), 일반(<code>(min-width: 600px)</code>), 범위(<code>(400px &lt;= width &lt; 700px)</code>) 형태의 특성,
 * 알 수 없는 특성이나 함수(거짓이며 부정해도 참이 되지 않음). 파싱할 수 없는 질의는 규격대로 <code>not all</code> 로 바뀌며 목록의 다른 질의에 영향을 주지 않는다.
 * 목록은 질의 중 하나라도 일치하면 환경과 일치한다.
 */
public final class MediaQueryList {

    /** Result: true. / 결과: 참. */
    private static final int T = 1;
    /** Result: false. / 결과: 거짓. */
    private static final int F = 0;
    /** Result: unknown (treated as false, but negation keeps it unknown). / 결과: 알 수 없음(거짓으로 다루지만 부정해도 알 수 없음으로 남는다). */
    private static final int U = -1;

    /** The queries. / 질의들. */
    private final List<Query> queries;

    /** True if the list was parsed from no text at all (CSSOM: a media list with no medium; it still matches like <code>all</code>). / 텍스트 없이 파싱한 목록이면 true(CSSOM: 미디어가 없는 미디어 목록. 그래도 <code>all</code> 처럼 일치한다). */
    private final boolean empty;

    /**
     * Creates a list.
     * <p>
     * 목록을 만든다.
     *
     * @param queries the queries / 질의들
     * @param empty   true if no text was given / 텍스트가 없었으면 true
     */
    private MediaQueryList(List<Query> queries, boolean empty) {
        this.queries = queries;
        this.empty = empty;
    }

    /**
     * Parses media query text (the value of a <code>media</code> attribute or the prelude of <code>@media</code>). Empty text is the list
     * <code>all</code>.
     * <p>
     * 미디어 질의 문자열(<code>media</code> 속성의 값이나 <code>@media</code> 의 전문)을 파싱한다. 빈 문자열은 <code>all</code> 목록이다.
     *
     * @param text the text / 문자열
     * @return the list, never null / 목록. null 이 아니다
     */
    public static MediaQueryList parse(String text) {
        return parse(CssParser.parseComponentValues(text == null ? "" : text));
    }

    /**
     * Parses media queries given as component values.
     * <p>
     * 컴포넌트 값으로 주어진 미디어 질의를 파싱한다.
     *
     * @param values the component values / 컴포넌트 값
     * @return the list, never null / 목록. null 이 아니다
     */
    public static MediaQueryList parse(List<ComponentValue> values) {
        List<Query> out = new ArrayList<Query>();
        List<List<ComponentValue>> parts = CssParser.splitCommas(values);
        // 쉼표로 나눈 조각마다 질의 하나를 파싱하고 실패하면 not all 로 바꾼다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> part = parts.get(i);
            if (part.isEmpty() && parts.size() == 1) {
                out.add(new Query(false, false, "all", null, true));
                continue;
            }
            Query q = part.isEmpty() ? null : parseQuery(CssParser.trim(part));
            out.add(q != null ? q : new Query(true, false, "all", null, true));
        }
        return new MediaQueryList(out, CssParser.trim(values).isEmpty());
    }

    /**
     * Tells whether the list was parsed from empty text (the CSSOM <code>MediaList</code> of such a list has no medium and the text
     * <code>""</code>, although the list matches every environment as <code>all</code> does).
     * <p>
     * 목록이 빈 텍스트에서 파싱되었는지 알려 준다(그런 목록의 CSSOM <code>MediaList</code> 는 미디어가 없고 텍스트가 <code>""</code> 이지만, 목록은
     * <code>all</code> 처럼 모든 환경에 일치한다).
     *
     * @return true for an empty list / 빈 목록이면 true
     */
    public boolean isEmptyList() {
        return empty;
    }

    /**
     * Returns the serialized form of every query of the list (none for an empty list), as the items of a CSSOM <code>MediaList</code>.
     * <p>
     * 목록의 모든 질의의 직렬화한 형태를 반환한다(빈 목록이면 없음). CSSOM <code>MediaList</code> 의 항목이다.
     *
     * @return the query texts in order / 순서대로의 질의 문자열
     */
    public List<String> toQueryTexts() {
        List<String> out = new ArrayList<String>();
        // 빈 목록이 아니면 질의를 하나씩 직렬화해 모은다.
        for (int i = 0; !empty && i < queries.size(); i++) {
            out.add(queries.get(i).text());
        }
        return out;
    }

    /**
     * Tells whether the list matches an environment.
     * <p>
     * 목록이 환경과 일치하는지 알려 준다.
     *
     * @param env the environment / 환경
     * @return true if any query is true / 질의 중 하나라도 참이면 true
     */
    public boolean matches(MediaEnvironment env) {
        // 질의를 차례로 평가해 하나라도 참이면 일치이다.
        for (int i = 0; i < queries.size(); i++) {
            if (queries.get(i).eval(env) == T) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of queries.
     * <p>
     * 질의 수를 반환한다.
     *
     * @return count / 개수
     */
    public int size() {
        return queries.size();
    }

    /**
     * Serializes the list: queries separated by a comma and a space, features written as <code>(name: value)</code>.
     * <p>
     * 목록을 직렬화한다: 질의는 쉼표와 공백으로 구분하고 특성은 <code>(name: value)</code> 로 쓴다.
     *
     * @return text / 문자열
     */
    public String toCssText() {
        StringBuilder sb = new StringBuilder();
        // 질의를 쉼표와 공백으로 이어 쓴다.
        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(queries.get(i).text());
        }
        return sb.toString();
    }

    /**
     * Returns the serialized form.
     * <p>
     * 직렬화한 문자열을 반환한다.
     *
     * @return text / 문자열
     */
    public String toString() {
        return toCssText();
    }

    /**
     * One media query: an optional media type with modifiers and an optional condition.
     * <p>
     * 미디어 질의 하나: 한정자가 붙을 수 있는 미디어 종류와 선택적인 조건.
     */
    private static final class Query {
        /** True for <code>not</code>. / <code>not</code> 이면 true. */
        final boolean not;
        /** True for <code>only</code>. / <code>only</code> 이면 true. */
        final boolean only;
        /** The media type (lower case), or null if only a condition was written. / 미디어 종류(소문자). 조건만 쓰였으면 null. */
        final String type;
        /** The condition or null. / 조건 또는 null. */
        final Cond cond;
        /** True for the replacement of an invalid query or the empty query list. / 올바르지 않은 질의의 대체물이거나 빈 질의 목록이면 true. */
        final boolean synthetic;

        /**
         * Creates a query.
         * <p>
         * 질의를 만든다.
         *
         * @param not       not flag / not 플래그
         * @param only      only flag / only 플래그
         * @param type      media type or null / 미디어 종류 또는 null
         * @param cond      condition or null / 조건 또는 null
         * @param synthetic true for generated queries / 만들어 낸 질의이면 true
         */
        Query(boolean not, boolean only, String type, Cond cond, boolean synthetic) {
            this.not = not;
            this.only = only;
            this.type = type;
            this.cond = cond;
            this.synthetic = synthetic;
        }

        /**
         * Evaluates the query.
         * <p>
         * 질의를 평가한다.
         *
         * @param env the environment / 환경
         * @return T, F or U / T, F, U 중 하나
         */
        int eval(MediaEnvironment env) {
            int r = T;
            if (type != null && !type.equals("all")) {
                r = type.equals(env.getMediaType()) ? T : F;
            }
            if (cond != null) {
                int c = cond.eval(env);
                r = r == F || c == F ? F : (r == U || c == U ? U : T);
            }
            if (not) {
                return r == U ? U : (r == T ? F : T);
            }
            return r;
        }

        /**
         * Serializes the query.
         * <p>
         * 질의를 직렬화한다.
         *
         * @return text / 문자열
         */
        String text() {
            StringBuilder sb = new StringBuilder();
            if (not) {
                sb.append("not ");
            }
            if (only) {
                sb.append("only ");
            }
            if (type != null) {
                sb.append(type);
            }
            if (cond != null) {
                sb.append(type != null ? " and " : "").append(cond.text());
            }
            return sb.toString();
        }
    }

    /**
     * A node of a media condition.
     * <p>
     * 미디어 조건의 노드.
     */
    private abstract static class Cond {
        /**
         * Evaluates the node.
         * <p>
         * 노드를 평가한다.
         *
         * @param env the environment / 환경
         * @return T, F or U / T, F, U 중 하나
         */
        abstract int eval(MediaEnvironment env);

        /**
         * Serializes the node.
         * <p>
         * 노드를 직렬화한다.
         *
         * @return text / 문자열
         */
        abstract String text();
    }

    /**
     * <code>not</code> followed by a condition in parentheses.
     * <p>
     * 괄호 안의 조건 앞에 붙은 <code>not</code>.
     */
    private static final class NotCond extends Cond {
        /** The negated node. / 부정되는 노드. */
        final Cond inner;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param inner the negated node / 부정되는 노드
         */
        NotCond(Cond inner) {
            this.inner = inner;
        }

        /** Evaluates the node. 노드를 평가한다. */
        int eval(MediaEnvironment env) {
            int r = inner.eval(env);
            return r == U ? U : (r == T ? F : T);
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text() {
            return "not " + inner.text();
        }
    }

    /**
     * A list of conditions joined by <code>and</code> or by <code>or</code>.
     * <p>
     * <code>and</code> 나 <code>or</code> 로 이은 조건 목록.
     */
    private static final class ListCond extends Cond {
        /** The operands. / 피연산자. */
        final List<Cond> items;
        /** True for <code>and</code>, false for <code>or</code>. / <code>and</code> 이면 true, <code>or</code> 이면 false. */
        final boolean and;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param items operands / 피연산자
         * @param and   true for and / and 이면 true
         */
        ListCond(List<Cond> items, boolean and) {
            this.items = items;
            this.and = and;
        }

        /** Evaluates the node. 노드를 평가한다. */
        int eval(MediaEnvironment env) {
            int r = and ? T : F;
            // 피연산자를 차례로 평가해 and 는 거짓이, or 는 참이 하나라도 있으면 결정한다.
            for (int i = 0; i < items.size(); i++) {
                int c = items.get(i).eval(env);
                if (and) {
                    if (c == F) {
                        return F;
                    }
                    if (c == U) {
                        r = U;
                    }
                } else {
                    if (c == T) {
                        return T;
                    }
                    if (c == U) {
                        r = U;
                    }
                }
            }
            return r;
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text() {
            StringBuilder sb = new StringBuilder();
            // 피연산자를 and 나 or 로 이어 쓴다.
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) {
                    sb.append(and ? " and " : " or ");
                }
                sb.append(items.get(i).text());
            }
            return sb.toString();
        }
    }

    /**
     * Something in parentheses that is not understood: it evaluates to unknown.
     * <p>
     * 괄호 안의 이해하지 못한 것: 알 수 없음으로 평가된다.
     */
    private static final class Enclosed extends Cond {
        /** The written text. / 쓰인 문자열. */
        final String text;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param text the text / 문자열
         */
        Enclosed(String text) {
            this.text = text;
        }

        /** Evaluates the node. 노드를 평가한다. */
        int eval(MediaEnvironment env) {
            return U;
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text() {
            return text;
        }
    }

    /**
     * A value written in a media feature.
     * <p>
     * 미디어 특성에 쓰인 값.
     */
    private static final class Mv {
        /** Kind: number. / 종류: 숫자. */
        static final int NUMBER = 0;
        /** Kind: length in pixels. / 종류: 픽셀 길이. */
        static final int LENGTH = 1;
        /** Kind: resolution in dppx. / 종류: dppx 해상도. */
        static final int RESOLUTION = 2;
        /** Kind: identifier. / 종류: 식별자. */
        static final int IDENT = 3;
        /** Kind: ratio. / 종류: 비율. */
        static final int RATIO = 4;
        /** The kind. / 종류. */
        final int kind;
        /** The numeric value (ratio as a quotient). / 숫자 값(비율은 몫). */
        final double v;
        /** The identifier or the written text. / 식별자 또는 쓰인 문자열. */
        final String text;

        /**
         * Creates a value.
         * <p>
         * 값을 만든다.
         *
         * @param kind kind / 종류
         * @param v    number / 숫자
         * @param text text / 문자열
         */
        Mv(int kind, double v, String text) {
            this.kind = kind;
            this.v = v;
            this.text = text;
        }
    }

    /**
     * A media feature test.
     * <p>
     * 미디어 특성 검사.
     */
    private static final class Feature extends Cond {
        /** The feature name without min- or max-. / min- 이나 max- 가 없는 특성 이름. */
        final String name;
        /** The comparison operators (0 for a boolean test), see {@link #compare}. / 비교 연산자(불리언 검사는 0). {@link #compare} 참조. */
        final char[] ops;
        /** The values compared with the feature. / 특성과 비교하는 값. */
        final Mv[] vals;
        /** The text to serialize. / 직렬화할 문자열. */
        final String text;

        /**
         * Creates a feature test.
         * <p>
         * 특성 검사를 만든다.
         *
         * @param name feature name / 특성 이름
         * @param ops  operators: one of the characters '=', '<', '>', 'l' (&lt;=) and 'g' (&gt;=) / 연산자: '=', '<', '>', 'l'(&lt;=), 'g'(&gt;=)
         * @param vals values / 값
         * @param text text / 문자열
         */
        Feature(String name, char[] ops, Mv[] vals, String text) {
            this.name = name;
            this.ops = ops;
            this.vals = vals;
            this.text = text;
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text() {
            return text;
        }

        /** Evaluates the node. 노드를 평가한다. */
        int eval(MediaEnvironment env) {
            Mv actual = actual(name, env);
            if (actual == null) {
                return U;
            }
            if (ops.length == 0) {
                return truthy(actual) ? T : F;
            }
            // 모든 비교가 성립해야 참이다.
            for (int i = 0; i < ops.length; i++) {
                int r = compare(actual, ops[i], vals[i]);
                if (r != T) {
                    return r;
                }
            }
            return T;
        }

        /**
         * Evaluates the feature in a boolean context.
         * <p>
         * 불리언 문맥에서 특성을 평가한다.
         *
         * @param a the actual value / 실제 값
         * @return true unless the value is zero, none or no-preference / 값이 0, none, no-preference 가 아니면 true
         */
        private static boolean truthy(Mv a) {
            if (a.kind == Mv.IDENT) {
                return !a.text.equals("none") && !a.text.equals("no-preference");
            }
            return a.v != 0;
        }

        /**
         * Compares the actual value with a written value.
         * <p>
         * 실제 값을 쓰인 값과 비교한다.
         *
         * @param a  the actual value / 실제 값
         * @param op the operator / 연산자
         * @param w  the written value / 쓰인 값
         * @return T, F or U (a value of the wrong type is unknown) / T, F, U(형식이 다른 값은 알 수 없음)
         */
        private static int compare(Mv a, char op, Mv w) {
            if (a.kind == Mv.IDENT) {
                return w.kind == Mv.IDENT && op == '=' ? (a.text.equals(w.text) ? T : F) : U;
            }
            if (w.kind == Mv.IDENT) {
                return U;
            }
            boolean ok = a.kind == w.kind || (a.kind == Mv.NUMBER && w.kind == Mv.RATIO) || (a.kind == Mv.RATIO && w.kind == Mv.NUMBER)
                    || (a.kind == Mv.LENGTH && w.kind == Mv.NUMBER && w.v == 0);
            if (!ok) {
                return U;
            }
            switch (op) {
            case '=':
                return Math.abs(a.v - w.v) < 1e-9 ? T : F;
            case '<':
                return a.v < w.v ? T : F;
            case '>':
                return a.v > w.v ? T : F;
            case 'l':
                return a.v <= w.v + 1e-9 ? T : F;
            default:
                return a.v >= w.v - 1e-9 ? T : F;
            }
        }
    }

    /**
     * Returns the actual value of a feature in an environment.
     * <p>
     * 환경에서 특성의 실제 값을 반환한다.
     *
     * @param name the feature name / 특성 이름
     * @param e    the environment / 환경
     * @return the value, or null for an unknown feature / 값. 알 수 없는 특성이면 null
     */
    private static Mv actual(String name, MediaEnvironment e) {
        if (name.equals("width")) {
            return new Mv(Mv.LENGTH, e.getWidth(), null);
        }
        if (name.equals("height")) {
            return new Mv(Mv.LENGTH, e.getHeight(), null);
        }
        if (name.equals("device-width")) {
            return new Mv(Mv.LENGTH, e.getDeviceWidth(), null);
        }
        if (name.equals("device-height")) {
            return new Mv(Mv.LENGTH, e.getDeviceHeight(), null);
        }
        if (name.equals("aspect-ratio")) {
            return new Mv(Mv.RATIO, e.getWidth() / e.getHeight(), null);
        }
        if (name.equals("device-aspect-ratio")) {
            return new Mv(Mv.RATIO, e.getDeviceWidth() / e.getDeviceHeight(), null);
        }
        if (name.equals("resolution")) {
            return new Mv(Mv.RESOLUTION, e.getResolution(), null);
        }
        if (name.equals("-webkit-device-pixel-ratio")) {
            return new Mv(Mv.NUMBER, e.getResolution(), null);
        }
        if (name.equals("color")) {
            return new Mv(Mv.NUMBER, e.getMonochromeBits() > 0 ? 0 : e.getColorBits(), null);
        }
        if (name.equals("monochrome")) {
            return new Mv(Mv.NUMBER, e.getMonochromeBits(), null);
        }
        if (name.equals("color-index") || name.equals("grid")) {
            return new Mv(Mv.NUMBER, 0, null);
        }
        if (name.equals("orientation")) {
            return ident(e.getHeight() >= e.getWidth() ? "portrait" : "landscape");
        }
        if (name.equals("hover") || name.equals("any-hover")) {
            return ident(e.getHover());
        }
        if (name.equals("pointer") || name.equals("any-pointer")) {
            return ident(e.getPointer());
        }
        if (name.equals("prefers-color-scheme")) {
            return ident(e.getColorScheme());
        }
        if (name.equals("prefers-reduced-motion")) {
            return ident(e.getReducedMotion());
        }
        if (name.equals("prefers-contrast")) {
            return ident(e.getContrast());
        }
        if (name.equals("display-mode")) {
            return ident(e.getDisplayMode());
        }
        if (name.equals("scripting")) {
            return ident(e.isScripting() ? "enabled" : "none");
        }
        if (name.equals("scan") || name.equals("update")) {
            return ident(name.equals("scan") ? "progressive" : "fast");
        }
        if (name.equals("overflow-block") || name.equals("overflow-inline")) {
            return ident("scroll");
        }
        if (name.equals("prefers-reduced-data") || name.equals("prefers-reduced-transparency")) {
            return ident("no-preference");
        }
        if (name.equals("forced-colors") || name.equals("inverted-colors")) {
            return ident("none");
        }
        if (name.equals("dynamic-range") || name.equals("video-dynamic-range")) {
            return ident("standard");
        }
        return null;
    }

    /**
     * Creates an identifier value.
     * <p>
     * 식별자 값을 만든다.
     *
     * @param s the identifier / 식별자
     * @return the value / 값
     */
    private static Mv ident(String s) {
        return new Mv(Mv.IDENT, 0, s);
    }

    /**
     * Returns the non-whitespace items.
     * <p>
     * 공백이 아닌 항목을 반환한다.
     *
     * @param list the component values / 컴포넌트 값
     * @return the items / 항목
     */
    private static List<ComponentValue> items(List<ComponentValue> list) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 공백이 아닌 항목만 모은다.
        for (int i = 0; i < list.size(); i++) {
            if (!CssParser.isWhitespace(list.get(i))) {
                out.add(list.get(i));
            }
        }
        return out;
    }

    /**
     * Tells whether a component value is an identifier with the given lower-case name.
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
     * Parses one media query.
     * <p>
     * 미디어 질의 하나를 파싱한다.
     *
     * @param part the component values of the query / 질의의 컴포넌트 값
     * @return the query, or null if it is invalid / 질의. 올바르지 않으면 null
     */
    private static Query parseQuery(List<ComponentValue> part) {
        List<ComponentValue> it = items(part);
        if (it.isEmpty()) {
            return null;
        }
        boolean not = false;
        boolean only = false;
        int i = 0;
        ComponentValue first = it.get(0);
        if (first instanceof CssToken && ((CssToken) first).is(CssToken.IDENT)) {
            String w = CssKeyword.lower(((CssToken) first).getValue());
            if ((w.equals("not") || w.equals("only")) && it.size() >= 2 && it.get(1) instanceof CssToken && ((CssToken) it.get(1)).is(CssToken.IDENT)
                    && !isIdent(it.get(1), "and") && !isIdent(it.get(1), "or") && !isIdent(it.get(1), "not")) {
                not = w.equals("not");
                only = w.equals("only");
                i = 1;
            } else if (w.equals("not")) {
                Cond c = parseCondition(it, true);
                return c == null ? null : new Query(false, false, null, c, false);
            }
            if (!(it.get(i) instanceof CssToken) || !((CssToken) it.get(i)).is(CssToken.IDENT)) {
                return null;
            }
            String type = CssKeyword.lower(((CssToken) it.get(i)).getValue());
            if (type.equals("and") || type.equals("or") || type.equals("layer")) {
                return null;
            }
            i++;
            Cond c = null;
            if (i < it.size()) {
                if (!isIdent(it.get(i), "and") || i + 1 >= it.size()) {
                    return null;
                }
                c = parseCondition(it.subList(i + 1, it.size()), false);
                if (c == null) {
                    return null;
                }
            }
            return new Query(not, only, type, c, false);
        }
        Cond c = parseCondition(it, true);
        return c == null ? null : new Query(false, false, null, c, false);
    }

    /**
     * Parses a media condition: <code>not</code> and one operand, or operands joined by <code>and</code> or <code>or</code> (not mixed).
     * <p>
     * 미디어 조건을 파싱한다: <code>not</code> 과 피연산자 하나, 또는 <code>and</code> 나 <code>or</code> 로 이은 피연산자(섞을 수 없음).
     *
     * @param it      the items / 항목
     * @param allowOr true if <code>or</code> is allowed (not after a media type) / <code>or</code> 를 허용하면 true(미디어 종류 뒤에서는 안 됨)
     * @return the condition or null / 조건 또는 null
     */
    private static Cond parseCondition(List<ComponentValue> it, boolean allowOr) {
        if (it.isEmpty()) {
            return null;
        }
        if (isIdent(it.get(0), "not")) {
            if (it.size() != 2) {
                return null;
            }
            Cond inner = inParens(it.get(1));
            return inner == null ? null : new NotCond(inner);
        }
        List<Cond> ops = new ArrayList<Cond>();
        Cond c0 = inParens(it.get(0));
        if (c0 == null) {
            return null;
        }
        ops.add(c0);
        if (it.size() == 1) {
            return c0;
        }
        String op = null;
        // 피연산자와 연산자가 번갈아 나와야 하며 연산자는 하나로 통일되어야 한다.
        for (int i = 1; i < it.size(); i += 2) {
            ComponentValue o = it.get(i);
            String w = o instanceof CssToken && ((CssToken) o).is(CssToken.IDENT) ? CssKeyword.lower(((CssToken) o).getValue()) : null;
            if (w == null || !(w.equals("and") || (allowOr && w.equals("or"))) || (op != null && !op.equals(w)) || i + 1 >= it.size()) {
                return null;
            }
            op = w;
            Cond c = inParens(it.get(i + 1));
            if (c == null) {
                return null;
            }
            ops.add(c);
        }
        return new ListCond(ops, "and".equals(op));
    }

    /**
     * Parses an operand: a condition or feature in parentheses, or an unknown enclosed text.
     * <p>
     * 피연산자를 파싱한다: 괄호 안의 조건이나 특성, 또는 알 수 없는 묶인 텍스트.
     *
     * @param v the component value / 컴포넌트 값
     * @return the node or null if it is not parenthesized / 노드. 괄호로 묶이지 않았으면 null
     */
    private static Cond inParens(ComponentValue v) {
        if (v instanceof CssFunction) {
            return new Enclosed(v.toCssText());
        }
        if (!(v instanceof CssBlock) || ((CssBlock) v).getOpenType() != CssToken.LEFT_PAREN) {
            return null;
        }
        List<ComponentValue> inner = items(((CssBlock) v).getContents());
        if (inner.isEmpty()) {
            return new Enclosed("()");
        }
        if (inner.get(0) instanceof CssBlock || isIdent(inner.get(0), "not")) {
            Cond c = parseCondition(inner, true);
            if (c != null) {
                return new Paren(c);
            }
            return new Enclosed(v.toCssText());
        }
        Feature f = parseFeature(inner);
        return f != null ? f : new Enclosed(v.toCssText());
    }

    /**
     * A condition wrapped in parentheses.
     * <p>
     * 괄호로 감싼 조건.
     */
    private static final class Paren extends Cond {
        /** The inner condition. / 안쪽 조건. */
        final Cond inner;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param inner the condition / 조건
         */
        Paren(Cond inner) {
            this.inner = inner;
        }

        /** Evaluates the node. 노드를 평가한다. */
        int eval(MediaEnvironment env) {
            return inner.eval(env);
        }

        /** Serializes the node. 노드를 직렬화한다. */
        String text() {
            return "(" + inner.text() + ")";
        }
    }

    /**
     * Parses the inside of a feature parenthesis: boolean, plain or range form.
     * <p>
     * 특성 괄호의 안쪽을 파싱한다: 불리언, 일반, 범위 형태.
     *
     * @param it the items / 항목
     * @return the feature, or null if the syntax is not a feature / 특성. 특성 문법이 아니면 null
     */
    private static Feature parseFeature(List<ComponentValue> it) {
        ComponentValue a = it.get(0);
        if (it.size() == 1) {
            if (a instanceof CssToken && ((CssToken) a).is(CssToken.IDENT)) {
                String n = CssKeyword.lower(((CssToken) a).getValue());
                return new Feature(n, new char[0], new Mv[0], "(" + n + ")");
            }
            return null;
        }
        if (it.size() >= 3 && a instanceof CssToken && ((CssToken) a).is(CssToken.IDENT) && it.get(1) instanceof CssToken
                && ((CssToken) it.get(1)).is(CssToken.COLON)) {
            return plain(CssKeyword.lower(((CssToken) a).getValue()), it.subList(2, it.size()));
        }
        return range(it);
    }

    /**
     * Parses <code>name: value</code>, including the <code>min-</code> and <code>max-</code> forms.
     * <p>
     * <code>name: value</code> 를 파싱한다. <code>min-</code> 과 <code>max-</code> 형태를 포함한다.
     *
     * @param name  the written name / 쓰인 이름
     * @param value the value items / 값 항목
     * @return the feature or null / 특성 또는 null
     */
    private static Feature plain(String name, List<ComponentValue> value) {
        char op = '=';
        String base = name;
        if (name.startsWith("min-")) {
            op = 'g';
            base = name.substring(4);
        } else if (name.startsWith("max-")) {
            op = 'l';
            base = name.substring(4);
        } else if (name.startsWith("-webkit-min-")) {
            op = 'g';
            base = "-webkit-" + name.substring(12);
        } else if (name.startsWith("-webkit-max-")) {
            op = 'l';
            base = "-webkit-" + name.substring(12);
        }
        Mv v = value(value);
        if (v == null) {
            return null;
        }
        if (op != '=' && (v.kind == Mv.IDENT || !isRangeType(base))) {
            return null;
        }
        return new Feature(base, new char[] {op}, new Mv[] {v}, "(" + name + ": " + CssBlock.join(value).trim() + ")");
    }

    /**
     * Tells whether a feature accepts ranges (and the min- and max- prefixes).
     * <p>
     * 특성이 범위(와 min-, max- 접두사)를 받는지 알려 준다.
     *
     * @param n the feature name / 특성 이름
     * @return true for range features / 범위 특성이면 true
     */
    private static boolean isRangeType(String n) {
        return n.equals("width") || n.equals("height") || n.equals("device-width") || n.equals("device-height") || n.equals("aspect-ratio")
                || n.equals("device-aspect-ratio") || n.equals("resolution") || n.equals("color") || n.equals("color-index") || n.equals("monochrome")
                || n.equals("-webkit-device-pixel-ratio");
    }

    /**
     * Parses the range forms <code>feature op value</code>, <code>value op feature</code> and <code>value op feature op value</code>.
     * <p>
     * 범위 형태 <code>feature op value</code>, <code>value op feature</code>, <code>value op feature op value</code> 를 파싱한다.
     *
     * @param it the items / 항목
     * @return the feature or null / 특성 또는 null
     */
    private static Feature range(List<ComponentValue> it) {
        // 비교 연산자 위치와 종류를 모은다: < > = 다음에 = 가 붙으면 <= 와 >= 이다.
        List<Integer> opStart = new ArrayList<Integer>();
        List<Character> opKind = new ArrayList<Character>();
        List<Integer> opEnd = new ArrayList<Integer>();
        int i = 0;
        // 항목을 훑으며 비교 연산자를 찾는다.
        while (i < it.size()) {
            ComponentValue v = it.get(i);
            if (v instanceof CssToken && (((CssToken) v).isDelim('<') || ((CssToken) v).isDelim('>') || ((CssToken) v).isDelim('='))) {
                char c = ((CssToken) v).getValue().charAt(0);
                int end = i + 1;
                char kind = c;
                if (c != '=' && end < it.size() && it.get(end) instanceof CssToken && ((CssToken) it.get(end)).isDelim('=')) {
                    kind = c == '<' ? 'l' : 'g';
                    end++;
                }
                opStart.add(Integer.valueOf(i));
                opKind.add(Character.valueOf(kind));
                opEnd.add(Integer.valueOf(end));
                i = end;
            } else {
                i++;
            }
        }
        if (opStart.size() < 1 || opStart.size() > 2) {
            return null;
        }
        List<List<ComponentValue>> segs = new ArrayList<List<ComponentValue>>();
        int from = 0;
        // 연산자 사이의 항목을 조각으로 나눈다.
        for (int k = 0; k < opStart.size(); k++) {
            segs.add(it.subList(from, opStart.get(k).intValue()));
            from = opEnd.get(k).intValue();
        }
        segs.add(it.subList(from, it.size()));
        // segs 의 항목을 하나씩 처리한다.
        for (int k = 0; k < segs.size(); k++) {
            if (segs.get(k).isEmpty()) {
                return null;
            }
        }
        // 특성 이름 조각을 찾는다: 식별자 하나이며 값이 될 수 없는 조각.
        int nameSeg = -1;
        for (int k = 0; k < segs.size(); k++) {
            List<ComponentValue> s = segs.get(k);
            if (s.size() == 1 && s.get(0) instanceof CssToken && ((CssToken) s.get(0)).is(CssToken.IDENT) && isRangeType(CssKeyword.lower(((CssToken) s.get(0)).getValue()))) {
                nameSeg = k;
                break;
            }
        }
        if (nameSeg < 0 || (opStart.size() == 2 && nameSeg != 1)) {
            return null;
        }
        String name = CssKeyword.lower(((CssToken) segs.get(nameSeg).get(0)).getValue());
        List<Character> ops = new ArrayList<Character>();
        List<Mv> vals = new ArrayList<Mv>();
        // 이름 조각 양쪽의 값마다 "특성 연산자 값" 꼴의 비교를 만든다.
        for (int k = 0; k < segs.size(); k++) {
            if (k == nameSeg) {
                continue;
            }
            Mv v = value(segs.get(k));
            if (v == null || v.kind == Mv.IDENT) {
                return null;
            }
            // 값이 이름 앞에 있으면 연산자를 뒤집고 뒤에 있으면 그대로 쓴다.
            char kind = opKind.get(k < nameSeg ? k : k - 1).charValue();
            if (k < nameSeg) {
                kind = flip(kind);
            }
            if (opStart.size() == 2 && (kind == '=')) {
                return null;
            }
            ops.add(Character.valueOf(kind));
            vals.add(v);
        }
        char[] oa = new char[ops.size()];
        // 연산자 목록을 배열로 옮긴다.
        for (int k = 0; k < oa.length; k++) {
            oa[k] = ops.get(k).charValue();
        }
        StringBuilder text = new StringBuilder("(");
        // 조각과 연산자를 공백으로 이어 정규화한 문자열을 만든다.
        for (int k = 0; k < segs.size(); k++) {
            if (k > 0) {
                char kind = opKind.get(k - 1).charValue();
                text.append(' ').append(kind == 'l' ? "<=" : kind == 'g' ? ">=" : String.valueOf(kind)).append(' ');
            }
            text.append(CssBlock.join(segs.get(k)).trim());
        }
        return new Feature(name, oa, vals.toArray(new Mv[vals.size()]), text.append(')').toString());
    }

    /**
     * Flips a comparison operator (for a value written before the feature name).
     * <p>
     * 비교 연산자를 뒤집는다(특성 이름 앞에 쓴 값을 위해).
     *
     * @param k the operator / 연산자
     * @return the flipped operator / 뒤집은 연산자
     */
    private static char flip(char k) {
        switch (k) {
        case '<':
            return '>';
        case '>':
            return '<';
        case 'l':
            return 'g';
        case 'g':
            return 'l';
        default:
            return k;
        }
    }

    /**
     * Parses the value of a feature: a number, a length, a resolution, a ratio or an identifier.
     * <p>
     * 특성의 값을 파싱한다: 숫자, 길이, 해상도, 비율, 식별자.
     *
     * @param list the value items / 값 항목
     * @return the value or null / 값 또는 null
     */
    private static Mv value(List<ComponentValue> list) {
        List<ComponentValue> v = items(list);
        if (v.size() == 1 && v.get(0) instanceof CssToken) {
            CssToken t = (CssToken) v.get(0);
            if (t.is(CssToken.NUMBER)) {
                return new Mv(Mv.NUMBER, t.getNumber(), null);
            }
            if (t.is(CssToken.IDENT)) {
                return ident(CssKeyword.lower(t.getValue()));
            }
            if (t.is(CssToken.DIMENSION)) {
                String u = CssKeyword.lower(t.getUnit());
                int k = CssUnits.kindOf(u);
                if (k == CssUnits.ABSOLUTE_LENGTH) {
                    return new Mv(Mv.LENGTH, CssUnits.absoluteToPx(t.getNumber(), u), null);
                }
                if (k == CssUnits.FONT_RELATIVE_LENGTH) {
                    return new Mv(Mv.LENGTH, t.getNumber() * 16.0, null);
                }
                if (k == CssUnits.RESOLUTION) {
                    return new Mv(Mv.RESOLUTION, CssUnits.resolutionToDppx(t.getNumber(), u), null);
                }
            }
            return null;
        }
        if (v.size() == 3 && v.get(0) instanceof CssToken && ((CssToken) v.get(0)).is(CssToken.NUMBER) && v.get(1) instanceof CssToken
                && ((CssToken) v.get(1)).isDelim('/') && v.get(2) instanceof CssToken && ((CssToken) v.get(2)).is(CssToken.NUMBER)) {
            double d = ((CssToken) v.get(2)).getNumber();
            return d == 0 ? null : new Mv(Mv.RATIO, ((CssToken) v.get(0)).getNumber() / d, null);
        }
        return null;
    }
}
