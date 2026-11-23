package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.css.props.LogicalProps;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.values.CssPending;
import org.duckdns.hjow.swingwebpanel.css.values.CssRaw;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;

/**
 * The substitution of <code>var()</code> references (CSS Custom Properties for Cascading Variables). Custom properties are resolved first, per
 * element, with cycle detection (every property in a cycle becomes invalid); then every declaration that contained <code>var()</code> is
 * substituted and parsed. A reference to an undefined property uses its fallback, or makes the declaration invalid at computed-value time. The
 * substituted token count is limited so that exponential expansion cannot exhaust memory.
 * <p>
 * <code>var()</code> 참조의 치환(CSS Custom Properties for Cascading Variables). 사용자 정의 속성을 요소마다 먼저 순환 검출과 함께 해석하고(순환에 든 모든 속성은 무효),
 * <code>var()</code> 가 든 선언은 치환한 뒤 파싱한다. 정의되지 않은 속성의 참조는 대체값을 쓰거나 계산 값 시점에 선언을 무효로 만든다. 지수적 확장이 메모리를 소진하지 못하도록
 * 치환한 토큰 수를 제한한다.
 */
final class VarResolver {

    /** Maximum number of tokens produced by substitutions of one value. / 값 하나의 치환이 만들 수 있는 최대 토큰 수. */
    static final int MAX_TOKENS = 100000;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private VarResolver() {
    }

    /**
     * Looks up the value of a custom property while a value is being substituted.
     * <p>
     * 값을 치환하는 동안 사용자 정의 속성의 값을 찾는다.
     */
    private interface Lookup {
        /**
         * Returns the tokens of a custom property.
         * <p>
         * 사용자 정의 속성의 토큰을 반환한다.
         *
         * @param name the name including the two hyphens / 하이픈 두 개를 포함한 이름
         * @return the tokens, or null if the property is undefined or invalid / 토큰. 정의되지 않았거나 무효이면 null
         */
        List<ComponentValue> value(String name);
    }

    /**
     * Resolves the custom properties of an element: inherited values overlaid with the declared ones, with <code>var()</code> substituted.
     * <p>
     * 요소의 사용자 정의 속성을 해석한다: 상속받은 값에 선언된 값을 덮고 <code>var()</code> 를 치환한다.
     *
     * @param inherited the parent's custom properties / 부모의 사용자 정의 속성
     * @param declared  the declared values (cascade winners) / 선언된 값(캐스케이드 승자)
     * @return the resolved custom properties / 해석한 사용자 정의 속성
     */
    static Map<String, CssRaw> resolveCustom(Map<String, CssRaw> inherited, Map<String, List<ComponentValue>> declared) {
        Map<String, CssRaw> result = new LinkedHashMap<String, CssRaw>(inherited);
        if (declared.isEmpty()) {
            return result;
        }
        Map<String, List<ComponentValue>> done = new HashMap<String, List<ComponentValue>>();
        Set<String> invalid = new HashSet<String>();
        Set<String> visiting = new HashSet<String>();
        // 선언된 사용자 정의 속성을 하나씩 해석한다(서로 참조하면 재귀로 먼저 해석).
        for (String name : declared.keySet()) {
            resolveOne(name, declared, inherited, done, invalid, visiting);
        }
        // 해석 결과를 반영하고 무효인 속성은 상속값까지 지운다.
        for (String name : declared.keySet()) {
            if (invalid.contains(name)) {
                result.remove(name);
            } else {
                result.put(name, new CssRaw(done.get(name)));
            }
        }
        return result;
    }

    /**
     * Resolves one declared custom property.
     * <p>
     * 선언된 사용자 정의 속성 하나를 해석한다.
     *
     * @param name      the property / 속성
     * @param declared  declared values / 선언된 값
     * @param inherited inherited values / 상속받은 값
     * @param done      resolved tokens / 해석한 토큰
     * @param invalid   names found invalid / 무효로 판정된 이름
     * @param visiting  names being resolved (cycle detection) / 해석 중인 이름(순환 검출)
     */
    private static void resolveOne(final String name, final Map<String, List<ComponentValue>> declared, final Map<String, CssRaw> inherited,
            final Map<String, List<ComponentValue>> done, final Set<String> invalid, final Set<String> visiting) {
        if (done.containsKey(name) || invalid.contains(name)) {
            return;
        }
        if (visiting.contains(name)) {
            invalid.add(name);
            return;
        }
        visiting.add(name);
        List<ComponentValue> tokens = substitute(declared.get(name), new Lookup() {
            public List<ComponentValue> value(String ref) {
                if (declared.containsKey(ref)) {
                    resolveOne(ref, declared, inherited, done, invalid, visiting);
                    return invalid.contains(ref) ? null : done.get(ref);
                }
                CssRaw r = inherited.get(ref);
                return r == null ? null : r.getTokens();
            }
        }, new int[] {0});
        visiting.remove(name);
        if (tokens == null || invalid.contains(name)) {
            invalid.add(name);
        } else {
            done.put(name, tokens);
        }
    }

    /**
     * Substitutes every <code>var()</code> in component values.
     * <p>
     * 컴포넌트 값의 모든 <code>var()</code> 를 치환한다.
     *
     * @param in     the values / 값
     * @param lookup resolves custom properties / 사용자 정의 속성을 해석한다
     * @param budget one-element counter of tokens produced / 만든 토큰 수를 세는 한 칸짜리 카운터
     * @return the substituted values, or null if a reference is invalid / 치환한 값. 참조가 무효이면 null
     */
    private static List<ComponentValue> substitute(List<ComponentValue> in, Lookup lookup, int[] budget) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 값을 훑으며 var() 는 치환하고 함수와 블록은 안쪽을 재귀로 치환한다.
        for (int i = 0; i < in.size(); i++) {
            ComponentValue v = in.get(i);
            if (v instanceof CssFunction && ((CssFunction) v).isNamed("var")) {
                List<ComponentValue> rep = substituteVar((CssFunction) v, lookup, budget);
                if (rep == null) {
                    return null;
                }
                out.addAll(rep);
            } else if (v instanceof CssFunction) {
                List<ComponentValue> args = substitute(((CssFunction) v).getArguments(), lookup, budget);
                if (args == null) {
                    return null;
                }
                out.add(new CssFunction(((CssFunction) v).getName(), args));
            } else if (v instanceof CssBlock) {
                List<ComponentValue> c = substitute(((CssBlock) v).getContents(), lookup, budget);
                if (c == null) {
                    return null;
                }
                out.add(new CssBlock(((CssBlock) v).getOpenType(), c));
            } else {
                out.add(v);
            }
            budget[0] += 1;
            if (budget[0] > MAX_TOKENS) {
                return null;
            }
        }
        return out;
    }

    /**
     * Substitutes one <code>var()</code>: the value of the property, or the fallback.
     * <p>
     * <code>var()</code> 하나를 치환한다: 속성의 값 또는 대체값.
     *
     * @param f      the var() function / var() 함수
     * @param lookup resolves custom properties / 사용자 정의 속성을 해석한다
     * @param budget token counter / 토큰 카운터
     * @return the replacement tokens or null if invalid / 대체 토큰. 무효이면 null
     */
    private static List<ComponentValue> substituteVar(CssFunction f, Lookup lookup, int[] budget) {
        List<ComponentValue> args = f.getArguments();
        int i = 0;
        // 앞쪽 공백을 건너뛴다.
        while (i < args.size() && CssParser.isWhitespace(args.get(i))) {
            i++;
        }
        if (i >= args.size() || !(args.get(i) instanceof CssToken) || !((CssToken) args.get(i)).is(CssToken.IDENT)
                || !((CssToken) args.get(i)).getValue().startsWith("--")) {
            return null;
        }
        String name = ((CssToken) args.get(i)).getValue();
        i++;
        List<ComponentValue> value = lookup.value(name);
        if (value != null) {
            budget[0] += value.size();
            return budget[0] > MAX_TOKENS ? null : value;
        }
        // 값이 없으면 쉼표 뒤의 대체값을 쓴다.
        while (i < args.size() && CssParser.isWhitespace(args.get(i))) {
            i++;
        }
        if (i < args.size() && args.get(i) instanceof CssToken && ((CssToken) args.get(i)).is(CssToken.COMMA)) {
            return substitute(CssParser.trim(args.subList(i + 1, args.size())), lookup, budget);
        }
        return null;
    }

    /**
     * Substitutes a pending declaration for one longhand and parses it.
     * <p>
     * 치환 대기 선언을 개별 속성 하나에 대해 치환하고 파싱한다.
     *
     * @param p       the pending value / 치환 대기 값
     * @param id      the longhand being computed / 계산 중인 개별 속성
     * @param custom  the element's custom properties / 요소의 사용자 정의 속성
     * @param reg     the property registry / 속성 등록부
     * @param cache   parsed declarations by pending value (identity) / 치환 대기 값별로 파싱한 선언(동일성 기준)
     * @param modes   the <code>writing-mode</code> and <code>direction</code> keywords of the element, to find the value of a logical longhand for its physical longhand, or null / 요소의 <code>writing-mode</code> 와 <code>direction</code> 키워드. 논리 개별 속성의 값을 그 물리 개별 속성에서 찾기 위한 것. 없으면 null
     * @return the specified value for the longhand, or null if the declaration is invalid at computed-value time / 개별 속성의 지정 값. 계산 시점에 무효이면 null
     */
    static CssValue resolvePending(CssPending p, int id, final Map<String, CssRaw> custom, PropertyRegistry reg,
            IdentityHashMap<CssPending, Map<Integer, CssValue>> cache, String[] modes) {
        Map<Integer, CssValue> parsed = cache.get(p);
        if (parsed == null) {
            parsed = new LinkedHashMap<Integer, CssValue>();
            List<ComponentValue> tokens = substitute(p.getTokens(), new Lookup() {
                public List<ComponentValue> value(String name) {
                    CssRaw r = custom.get(name);
                    return r == null ? null : r.getTokens();
                }
            }, new int[] {0});
            if (tokens == null || reg.parseDeclaration(p.getProperty(), tokens, new ParseEnv(reg, p.getBaseUrl()), parsed) != PropertyRegistry.OK) {
                parsed = new LinkedHashMap<Integer, CssValue>();
            }
            cache.put(p, parsed);
        }
        CssValue direct = parsed.get(Integer.valueOf(id));
        if (direct == null && modes != null) {
            // 논리 속성으로 선언한 값은 쓰기 모드와 방향이 가리키는 물리 개별 속성의 값이다.
            for (Map.Entry<Integer, CssValue> e : parsed.entrySet()) {
                int k = e.getKey().intValue();
                if (LogicalProps.isLogical(k) && LogicalProps.physical(k, modes[0], modes[1]) == id) {
                    return e.getValue();
                }
            }
        }
        return direct;
    }
}
