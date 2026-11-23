package org.duckdns.hjow.swingwebpanel.css.props;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssPending;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.GrammarRegistry;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;
import org.duckdns.hjow.swingwebpanel.css.values.ValueGrammar;

/**
 * The table of CSS properties known to the engine: the longhands ({@link Property}, ids in {@link Prop}), the shorthands ({@link Shorthand}), the
 * vendor-prefixed aliases, the named value definitions, and the names that are recognised but deliberately unsupported (3D transforms, logical
 * properties, ...). It parses a declaration into longhand values with the error handling of a browser: an invalid value drops the whole declaration.
 * <p>
 * 엔진이 아는 CSS 속성의 표: 개별 속성({@link Property}, id 는 {@link Prop}), 단축 속성({@link Shorthand}), 접두사 붙은 별칭, 이름 있는 값 정의, 알아보지만 의도적으로
 * 지원하지 않는 이름(3D 변형, 논리 속성 등). 브라우저처럼 오류를 처리하여 선언을 개별 속성 값으로 파싱한다: 올바르지 않은 값은 선언 전체를 버린다.
 */
public final class PropertyRegistry implements GrammarRegistry {

    /** Result: the declaration was parsed. / 결과: 선언을 파싱했다. */
    public static final int OK = 0;
    /** Result: the property name is unknown. / 결과: 속성 이름을 모른다. */
    public static final int UNKNOWN = 1;
    /** Result: the property is known but unsupported by this engine. / 결과: 속성을 알지만 이 엔진이 지원하지 않는다. */
    public static final int UNSUPPORTED = 2;
    /** Result: the value is invalid for the property. / 결과: 값이 속성에 올바르지 않다. */
    public static final int INVALID = 3;

    /** The shared instance. / 공유 인스턴스. */
    private static PropertyRegistry instance;

    /** Longhands by id (index 0 unused). / id 별 개별 속성(0번은 쓰지 않음). */
    private final Property[] byId = new Property[Prop.COUNT + 1];

    /** Longhands by name. / 이름별 개별 속성. */
    private final Map<String, Property> byName = new HashMap<String, Property>();

    /** Shorthands by name. / 이름별 단축 속성. */
    private final Map<String, Shorthand> shorthands = new HashMap<String, Shorthand>();

    /** Alias to canonical name. / 별칭에서 정식 이름으로. */
    private final Map<String, String> aliases = new HashMap<String, String>();

    /** Named value definitions. / 이름 있는 값 정의. */
    private final Map<String, ValueGrammar> productions = new HashMap<String, ValueGrammar>();

    /** Recognised but unsupported property names. / 알아보지만 지원하지 않는 속성 이름. */
    private final Set<String> unsupported = new HashSet<String>();

    /**
     * Returns the shared registry.
     * <p>
     * 공유 등록부를 반환한다.
     *
     * @return the registry / 등록부
     */
    public static synchronized PropertyRegistry getInstance() {
        if (instance == null) {
            instance = new PropertyRegistry();
        }
        return instance;
    }

    /**
     * Builds the registry from the property table.
     * <p>
     * 속성 표에서 등록부를 만든다.
     */
    private PropertyRegistry() {
        // 이름 있는 값 정의를 먼저 컴파일한다(속성 문법이 참조한다).
        for (int i = 0; i < PropertyTable.PRODUCTIONS.length; i++) {
            productions.put(PropertyTable.PRODUCTIONS[i][0], ValueGrammar.compile(PropertyTable.PRODUCTIONS[i][1]));
        }
        ParseEnv env = new ParseEnv(this, null);
        // 속성 표의 행마다 개별 속성을 만들고 초기값을 문법으로 파싱한다.
        for (int i = 0; i < PropertyTable.DEFS.length; i++) {
            String[] row = PropertyTable.DEFS[i];
            ValueGrammar g = ValueGrammar.compile(row[3]);
            CssValue initial = g.match(CssParser.parseComponentValues(row[2]), env);
            if (initial == null) {
                throw new IllegalStateException("the initial value of " + row[0] + " does not match its grammar: " + row[2]);
            }
            Property p = new Property(i + 1, row[0], row[1].equals("i"), initial, g, row[4].indexOf('P') >= 0);
            byId[i + 1] = p;
            byName.put(row[0], p);
        }
        List<Shorthand> sh = Shorthands.create();
        // 단축 속성을 이름으로 등록한다.
        for (int i = 0; i < sh.size(); i++) {
            shorthands.put(sh.get(i).getName(), sh.get(i));
        }
        registerAliases();
        registerUnsupported();
    }

    /**
     * Registers the vendor-prefixed and legacy aliases.
     * <p>
     * 접두사가 붙은 이름과 이전 이름의 별칭을 등록한다.
     */
    private void registerAliases() {
        aliases.put("word-wrap", "overflow-wrap");
        aliases.put("grid-gap", "gap");
        aliases.put("grid-row-gap", "row-gap");
        aliases.put("grid-column-gap", "column-gap");
        // 예전 쪽 나눔 속성과 접두사 붙은 다단 나눔 속성은 break-* 의 별칭이다(CSS Fragmentation 3 3.4 절).
        aliases.put("page-break-before", "break-before");
        aliases.put("page-break-after", "break-after");
        aliases.put("page-break-inside", "break-inside");
        aliases.put("-webkit-column-break-before", "break-before");
        aliases.put("-webkit-column-break-after", "break-after");
        aliases.put("-webkit-column-break-inside", "break-inside");
        aliases.put("-webkit-box-decoration-break", "box-decoration-break");
        aliases.put("-webkit-line-clamp", "line-clamp");
        aliases.put("-webkit-backdrop-filter", "backdrop-filter");
        aliases.put("-webkit-mask-border-source", "mask-border-source");
        String[] prefixes = {"-webkit-", "-moz-", "-ms-", "-o-"};
        String[] same = {"box-shadow", "box-sizing", "transform", "transform-origin", "transition", "transition-property", "transition-duration",
            "transition-timing-function", "transition-delay", "animation", "animation-name", "animation-duration", "animation-timing-function",
            "animation-delay", "animation-iteration-count", "animation-direction", "animation-fill-mode", "animation-play-state", "border-radius",
            "border-top-left-radius", "border-top-right-radius", "border-bottom-left-radius", "border-bottom-right-radius", "user-select",
            "appearance", "background-size", "background-origin", "background-clip", "opacity", "column-count", "column-gap", "column-width",
            "column-rule", "column-rule-width", "column-rule-style", "column-rule-color", "columns", "flex", "flex-grow", "flex-shrink",
            "flex-basis", "flex-direction", "flex-wrap", "flex-flow", "order", "justify-content", "align-items", "align-self", "align-content",
            "hyphens", "text-overflow", "tab-size", "object-fit", "object-position", "mask", "mask-image", "mask-size", "mask-position", "mask-repeat",
            "mask-origin", "mask-clip"};
        // 접두사가 붙은 이름은 접두사 없는 이름의 별칭이다.
        for (int i = 0; i < prefixes.length; i++) {
            for (int j = 0; j < same.length; j++) {
                aliases.put(prefixes[i] + same[j], same[j]);
            }
        }
    }

    /**
     * Registers the names that are recognised but unsupported.
     * <p>
     * 알아보지만 지원하지 않는 이름을 등록한다.
     */
    private void registerUnsupported() {
        String[] names = {"perspective", "perspective-origin", "transform-style", "backface-visibility",
            "-webkit-box-orient", "-webkit-box-flex", "-webkit-font-smoothing",
            "-moz-osx-font-smoothing", "-webkit-text-size-adjust", "-moz-text-size-adjust", "-ms-text-size-adjust", "text-size-adjust",
            "-webkit-tap-highlight-color", "-webkit-overflow-scrolling", "-ms-overflow-style", "-webkit-print-color-adjust", "grid-area", "grid-row", "grid-column",
            "grid-template", "grid", "grid-auto-position", "zoom", "ruby-position", "ruby-align", "dominant-baseline",
            "alignment-baseline", "baseline-shift", "vector-effect", "fill", "stroke", "stroke-width", "stroke-linecap", "stroke-linejoin",
            "stroke-dasharray", "stroke-dashoffset", "stroke-opacity", "fill-opacity", "fill-rule", "stop-color", "stop-opacity",
            "marker", "marker-start", "marker-mid", "marker-end", "paint-order", "shape-rendering", "text-anchor", "color-interpolation",
            "color-interpolation-filters", "flood-color", "flood-opacity", "lighting-color", "mask-type", "d", "cx", "cy", "r", "rx", "ry", "x", "y"};
        // 이름을 집합에 넣는다.
        for (int i = 0; i < names.length; i++) {
            unsupported.add(names[i]);
        }
    }

    /**
     * Returns the grammar of a named value definition (the {@link GrammarRegistry} contract).
     * <p>
     * 이름 있는 값 정의의 문법을 반환한다({@link GrammarRegistry} 계약).
     *
     * @param name definition name / 정의 이름
     * @return the grammar or null / 문법 또는 null
     */
    public ValueGrammar get(String name) {
        return productions.get(name);
    }

    /**
     * Returns a longhand by id.
     * <p>
     * id 로 개별 속성을 반환한다.
     *
     * @param id id from {@link Prop} / {@link Prop} 의 id
     * @return the property / 속성
     */
    public Property byId(int id) {
        return byId[id];
    }

    /**
     * Returns a longhand by name (aliases are resolved).
     * <p>
     * 이름으로 개별 속성을 반환한다(별칭은 해석한다).
     *
     * @param name property name / 속성 이름
     * @return the property or null / 속성 또는 null
     */
    public Property byName(String name) {
        return byName.get(canonicalName(name));
    }

    /**
     * Returns a shorthand by name (aliases are resolved).
     * <p>
     * 이름으로 단축 속성을 반환한다(별칭은 해석한다).
     *
     * @param name property name / 속성 이름
     * @return the shorthand or null / 단축 속성 또는 null
     */
    public Shorthand getShorthand(String name) {
        return shorthands.get(canonicalName(name));
    }

    /**
     * Returns the canonical lower-case name: aliases are resolved and ASCII letters are lower-cased.
     * <p>
     * 정식 소문자 이름을 반환한다: 별칭을 해석하고 ASCII 글자를 소문자로 바꾼다.
     *
     * @param name property name / 속성 이름
     * @return canonical name / 정식 이름
     */
    public String canonicalName(String name) {
        String l = CssKeyword.lower(name);
        String a = aliases.get(l);
        return a != null ? a : l;
    }

    /**
     * Returns the ids of the longhands a property name stands for: the property itself, or all the longhands of a shorthand.
     * <p>
     * 속성 이름이 뜻하는 개별 속성의 id 를 반환한다: 속성 자신이거나 단축 속성의 모든 개별 속성.
     *
     * @param name property name / 속성 이름
     * @return the ids, or null if the name is not a supported property / id. 지원하는 속성이 아니면 null
     */
    public int[] longhandsOf(String name) {
        String c = canonicalName(name);
        Property p = byName.get(c);
        if (p != null) {
            return new int[] {p.getId()};
        }
        Shorthand s = shorthands.get(c);
        if (s != null) {
            return s.getLonghands();
        }
        if (c.equals("all")) {
            List<Integer> l = new ArrayList<Integer>();
            // direction 과 unicode-bidi 를 뺀 모든 속성이 all 에 포함된다.
            for (int i = 1; i <= Prop.COUNT; i++) {
                if (i != Prop.DIRECTION && i != Prop.UNICODE_BIDI) {
                    l.add(Integer.valueOf(i));
                }
            }
            int[] a = new int[l.size()];
            // a 의 항목을 하나씩 처리한다.
            for (int i = 0; i < a.length; i++) {
                a[i] = l.get(i).intValue();
            }
            return a;
        }
        return null;
    }

    /**
     * Returns the names of all supported properties (longhands and shorthands).
     * <p>
     * 지원하는 모든 속성(개별과 단축)의 이름을 반환한다.
     *
     * @return sorted names / 정렬된 이름
     */
    public List<String> names() {
        List<String> l = new ArrayList<String>(byName.keySet());
        l.addAll(shorthands.keySet());
        l.add("all");
        Collections.sort(l);
        return l;
    }

    /**
     * Returns the names that are recognised but deliberately unsupported (declarations of them are dropped with an "unsupported" diagnostic).
     * <p>
     * 알아보지만 의도적으로 지원하지 않는 이름을 반환한다(그 선언은 "unsupported" 진단과 함께 버려진다).
     *
     * @return sorted names / 정렬된 이름
     */
    public List<String> unsupportedNames() {
        List<String> l = new ArrayList<String>(unsupported);
        Collections.sort(l);
        return l;
    }

    /**
     * Returns the alias names (vendor-prefixed and legacy names) with the standard name each stands for.
     * <p>
     * 별칭 이름(접두사 붙은 이름과 이전 이름)과 각각이 뜻하는 표준 이름을 반환한다.
     *
     * @return a map from alias to standard name, sorted by alias / 별칭에서 표준 이름으로 가는 맵(별칭 순서로 정렬)
     */
    public Map<String, String> aliasNames() {
        return new java.util.TreeMap<String, String>(aliases);
    }

    /**
     * Tells whether the name is a custom property name (two hyphens).
     * <p>
     * 이름이 사용자 정의 속성 이름(하이픈 두 개)인지 알려 준다.
     *
     * @param name property name / 속성 이름
     * @return true for custom properties / 사용자 정의 속성이면 true
     */
    public static boolean isCustomProperty(String name) {
        return name.startsWith("--");
    }

    /**
     * Tells whether component values contain a <code>var()</code> function at any depth.
     * <p>
     * 컴포넌트 값이 어느 깊이에든 <code>var()</code> 함수를 담고 있는지 알려 준다.
     *
     * @param values component values / 컴포넌트 값
     * @return true if var() occurs / var() 가 있으면 true
     */
    public static boolean containsVar(List<ComponentValue> values) {
        // 값을 훑으며 함수와 블록 안까지 재귀로 var() 를 찾는다.
        for (int i = 0; i < values.size(); i++) {
            ComponentValue v = values.get(i);
            if (v instanceof CssFunction) {
                CssFunction f = (CssFunction) v;
                if (f.isNamed("var") || containsVar(f.getArguments())) {
                    return true;
                }
            } else if (v instanceof CssBlock && containsVar(((CssBlock) v).getContents())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a declaration into longhand values.
     * <p>
     * 선언을 개별 속성 값으로 파싱한다.
     * <p>
     * A CSS-wide keyword is written to every longhand; a value that contains <code>var()</code> becomes a {@link CssPending} for every longhand;
     * otherwise the shorthand parser or the longhand grammar decides. Custom properties (<code>--x</code>) are not handled here.
     * <p>
     * CSS 전역 키워드는 모든 개별 속성에 쓰고, <code>var()</code> 가 든 값은 모든 개별 속성에 대해 {@link CssPending} 이 되며, 그 밖에는 단축 속성 파서나 개별 속성 문법이
     * 판정한다. 사용자 정의 속성(<code>--x</code>)은 여기서 다루지 않는다.
     *
     * @param name  the property name as written / 쓰인 속성 이름
     * @param value the declaration value / 선언 값
     * @param env   the parse environment / 파싱 환경
     * @param out   receives longhand id and specified value in order / 개별 속성 id 와 지정 값을 순서대로 받는다
     * @return {@link #OK}, {@link #UNKNOWN}, {@link #UNSUPPORTED} or {@link #INVALID} / OK, UNKNOWN, UNSUPPORTED, INVALID 중 하나
     */
    public int parseDeclaration(String name, List<ComponentValue> value, ParseEnv env, Map<Integer, CssValue> out) {
        String c = canonicalName(name);
        int[] ids = longhandsOf(c);
        if (ids == null) {
            return unsupported.contains(c) || (c.startsWith("-") && !c.startsWith("--")) ? UNSUPPORTED : UNKNOWN;
        }
        // 호환 모드의 CSS 는 단위 없는 길이와 # 없는 16진수 색을 일반 토큰으로 바꾼 뒤 평소의 문법으로 파싱한다.
        List<ComponentValue> items = ValueGrammar.prepare(env != null && env.isQuirks() ? QuirkyValues.rewrite(c, value) : value);
        if (items.isEmpty()) {
            return INVALID;
        }
        if (items.size() == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).is(CssToken.IDENT)) {
            CssKeyword k = CssKeyword.of(((CssToken) items.get(0)).getValue());
            if (k.isCssWide()) {
                // CSS 전역 키워드는 모든 개별 속성에 그대로 쓴다.
                for (int i = 0; i < ids.length; i++) {
                    out.put(Integer.valueOf(ids[i]), k);
                }
                return OK;
            }
        }
        if (c.equals("all")) {
            return INVALID;
        }
        if (containsVar(items)) {
            CssPending pending = new CssPending(c, CssParser.trim(value), env.getBaseUrl());
            // var() 가 든 값은 모든 개별 속성에 대해 치환 대기 값이 된다.
            for (int i = 0; i < ids.length; i++) {
                out.put(Integer.valueOf(ids[i]), pending);
            }
            return OK;
        }
        Property p = byName.get(c);
        if (p != null) {
            CssValue v = p.getGrammar().match(items, env);
            if (v == null) {
                return INVALID;
            }
            out.put(Integer.valueOf(p.getId()), v);
            return OK;
        }
        Map<Integer, CssValue> tmp = new LinkedHashMap<Integer, CssValue>();
        if (!shorthands.get(c).expand(items, this, env, tmp)) {
            return INVALID;
        }
        out.putAll(tmp);
        return OK;
    }

    /**
     * Parses a value text for a property into its longhand values (a convenience for tests and <code>CSS.supports</code>).
     * <p>
     * 속성의 값 문자열을 개별 속성 값으로 파싱한다(시험과 <code>CSS.supports</code> 를 위한 편의 메서드).
     *
     * @param name the property name / 속성 이름
     * @param text the value text / 값 문자열
     * @param out  receives the longhand values / 개별 속성 값을 받는다
     * @return the result code / 결과 코드
     */
    public int parse(String name, String text, Map<Integer, CssValue> out) {
        return parseDeclaration(name, CssParser.parseComponentValues(text), new ParseEnv(this, null), out);
    }
}
