package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * A compiled value definition in the notation of CSS Values and Units: keywords, <code>&lt;type&gt;</code> references (with an optional
 * <code>[min,max]</code> range), juxtaposition, <code>&amp;&amp;</code>, <code>||</code>, <code>|</code>, brackets, the multipliers
 * <code>? * + {A,B} # !</code>, literal commas and slashes, and function notations such as <code>translate(&lt;length&gt;, &lt;length&gt;?)</code>.
 * Matching is a backtracking search over the component values of a declaration; it returns the typed value mirroring the written structure
 * (a {@link CssList} for sequences, the single value itself for one component, a comma list for <code>#</code>), or null if the value does not
 * satisfy the definition. A prefix match supports the shorthands whose parts may come in any order.
 * <p>
 * CSS Values and Units 표기로 컴파일한 값 정의: 키워드, <code>&lt;type&gt;</code> 참조(선택적인 <code>[min,max]</code> 범위), 나열, <code>&amp;&amp;</code>,
 * <code>||</code>, <code>|</code>, 대괄호, 반복 표기 <code>? * + {A,B} # !</code>, 쉼표와 슬래시 리터럴, <code>translate(&lt;length&gt;, &lt;length&gt;?)</code> 같은
 * 함수 표기. 일치 검사는 선언의 컴포넌트 값에 대한 역추적 탐색이며, 쓰인 구조를 반영한 값(나열은 {@link CssList}, 컴포넌트 하나는 그 값 자체,
 * <code>#</code> 는 쉼표 목록)을 반환하고 정의를 만족하지 않으면 null 을 반환한다. 접두사 일치는 부분의 순서가 자유로운 단축 속성을 지원한다.
 */
public final class ValueGrammar {

    /** Infinity bound for repetition counts and ranges. / 반복 횟수와 범위의 무한대 경계. */
    private static final int MANY = Integer.MAX_VALUE;

    /** The root node. / 루트 노드. */
    private final Node root;

    /** The definition text. / 정의 문자열. */
    private final String source;

    /**
     * Creates a grammar.
     * <p>
     * 문법을 만든다.
     *
     * @param source the definition text / 정의 문자열
     * @param root   the root node / 루트 노드
     */
    private ValueGrammar(String source, Node root) {
        this.source = source;
        this.root = root;
    }

    /**
     * Compiles a definition text.
     * <p>
     * 정의 문자열을 컴파일한다.
     *
     * @param text definition in value definition syntax / 값 정의 문법의 정의
     * @return the grammar / 문법
     * @throws IllegalArgumentException if the definition itself is malformed (a programming error) / 정의 자체가 잘못되었으면(프로그래밍 오류)
     */
    public static ValueGrammar compile(String text) {
        Node n = new GrammarParser(CssParser.parseComponentValues(text), text).parseAlt();
        return new ValueGrammar(text, n);
    }

    /**
     * Returns the definition text.
     * <p>
     * 정의 문자열을 반환한다.
     *
     * @return the definition / 정의
     */
    public String getSource() {
        return source;
    }

    /**
     * Drops the whitespace tokens of the top level so that a list can be matched.
     * <p>
     * 목록을 맞출 수 있도록 최상위의 공백 토큰을 뺀다.
     *
     * @param values component values / 컴포넌트 값
     * @return the values without whitespace / 공백이 없는 값
     */
    public static List<ComponentValue> prepare(List<ComponentValue> values) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 공백이 아닌 값만 모은다.
        for (int i = 0; i < values.size(); i++) {
            if (!CssParser.isWhitespace(values.get(i))) {
                out.add(values.get(i));
            }
        }
        return out;
    }

    /**
     * Matches the whole component value list against this definition.
     * <p>
     * 컴포넌트 값 목록 전체를 이 정의에 맞춘다.
     *
     * @param values the declaration value (whitespace is ignored) / 선언 값(공백은 무시)
     * @param env    parse environment / 파싱 환경
     * @return the typed value, or null if it does not match / 형식이 있는 값. 맞지 않으면 null
     */
    public CssValue match(List<ComponentValue> values, ParseEnv env) {
        final List<ComponentValue> items = prepare(values);
        final CssValue[] result = new CssValue[1];
        boolean ok = root.match(new Input(items, env), 0, new Cont() {
            public boolean next(int pos, CssValue v) {
                if (pos != items.size()) {
                    return false;
                }
                result[0] = v;
                return true;
            }
        });
        return ok ? (result[0] == null ? EMPTY : result[0]) : null;
    }

    /** The value of a match that captured nothing. / 아무것도 붙잡지 않은 일치의 값. */
    private static final CssValue EMPTY = new CssList(new ArrayList<CssValue>(), false);

    /**
     * The result of a prefix match.
     * <p>
     * 접두사 일치의 결과.
     */
    public static final class Prefix {
        /** Index after the last consumed component value. / 마지막으로 소비한 컴포넌트 값 다음 위치. */
        public final int end;
        /** The captured value. / 붙잡은 값. */
        public final CssValue value;

        /**
         * Creates a result.
         * <p>
         * 결과를 만든다.
         *
         * @param end   end index / 끝 위치
         * @param value captured value / 붙잡은 값
         */
        Prefix(int end, CssValue value) {
            this.end = end;
            this.value = value;
        }
    }

    /**
     * Finds the longest prefix of a prepared item list, starting at an index, that satisfies the definition.
     * <p>
     * 준비한 항목 목록에서 시작 위치부터 정의를 만족하는 가장 긴 접두사를 찾는다.
     *
     * @param items prepared component values / 준비한 컴포넌트 값
     * @param start first index to match / 맞출 첫 위치
     * @param env   parse environment / 파싱 환경
     * @return the longest match, or null if nothing (not even the empty prefix) matches / 가장 긴 일치. 아무것도 맞지 않으면 null
     */
    public Prefix matchPrefix(List<ComponentValue> items, int start, ParseEnv env) {
        final int[] bestEnd = {-1};
        final CssValue[] bestValue = new CssValue[1];
        root.match(new Input(items, env), start, new Cont() {
            public boolean next(int pos, CssValue v) {
                if (pos > bestEnd[0]) {
                    bestEnd[0] = pos;
                    bestValue[0] = v;
                }
                return false;
            }
        });
        return bestEnd[0] < 0 ? null : new Prefix(bestEnd[0], bestValue[0]);
    }

    /**
     * The input of a match: the items and the environment.
     * <p>
     * 일치 검사의 입력: 항목과 환경.
     */
    private static final class Input {
        /** The prepared component values. / 준비한 컴포넌트 값. */
        final List<ComponentValue> items;
        /** Parse environment. / 파싱 환경. */
        final ParseEnv env;

        /**
         * Creates an input.
         * <p>
         * 입력을 만든다.
         *
         * @param items component values / 컴포넌트 값
         * @param env   environment / 환경
         */
        Input(List<ComponentValue> items, ParseEnv env) {
            this.items = items;
            this.env = env;
        }
    }

    /**
     * The continuation called after a node matched: it receives the position after the match and the captured value (null if nothing was
     * captured) and returns true to accept (stop searching) or false to make the search backtrack.
     * <p>
     * 노드가 일치한 뒤 호출되는 연속: 일치 뒤의 위치와 붙잡은 값(없으면 null)을 받아 받아들이면(탐색 중단) true, 역추적하게 하려면 false 를 반환한다.
     */
    private interface Cont {
        /**
         * Continues the search after a match.
         * <p>
         * 일치 뒤에 탐색을 이어 간다.
         *
         * @param pos position after the match / 일치 뒤의 위치
         * @param v   captured value or null / 붙잡은 값 또는 null
         * @return true to accept / 받아들이면 true
         */
        boolean next(int pos, CssValue v);
    }

    /**
     * A grammar node.
     * <p>
     * 문법 노드.
     */
    private abstract static class Node {
        /**
         * Tries to match at a position and calls the continuation for every way the node can match.
         * <p>
         * 위치에서 일치를 시도하고 노드가 일치할 수 있는 모든 방법마다 연속을 호출한다.
         *
         * @param in  the input / 입력
         * @param pos start position / 시작 위치
         * @param k   the continuation / 연속
         * @return true if the continuation accepted / 연속이 받아들였으면 true
         */
        abstract boolean match(Input in, int pos, Cont k);
    }

    /**
     * Builds the value of a sequence from the captured items.
     * <p>
     * 붙잡은 항목들로 나열의 값을 만든다.
     *
     * @param acc captured items / 붙잡은 항목
     * @return null if empty, the item if single, else a space list / 비면 null, 하나면 그 항목, 아니면 공백 목록
     */
    private static CssValue build(List<CssValue> acc) {
        if (acc.isEmpty()) {
            return null;
        }
        if (acc.size() == 1) {
            return acc.get(0);
        }
        return new CssList(acc, false);
    }

    /**
     * Matches a keyword.
     * <p>
     * 키워드를 맞춘다.
     */
    private static final class Keyword extends Node {
        /** Lower-case keyword. / 소문자 키워드. */
        final String word;

        /**
         * Creates a keyword node.
         * <p>
         * 키워드 노드를 만든다.
         *
         * @param w the keyword / 키워드
         */
        Keyword(String w) {
            this.word = CssKeyword.lower(w);
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            if (pos >= in.items.size()) {
                return false;
            }
            ComponentValue v = in.items.get(pos);
            return v instanceof CssToken && ((CssToken) v).is(CssToken.IDENT) && ((CssToken) v).getValue().equalsIgnoreCase(word)
                    && k.next(pos + 1, CssKeyword.of(word));
        }
    }

    /**
     * Matches a built-in type.
     * <p>
     * 기본 형식을 맞춘다.
     */
    private static final class Terminal extends Node {
        /** Type name. / 형식 이름. */
        final String type;
        /** Minimum accepted number. / 허용하는 최소 숫자. */
        final double min;
        /** Maximum accepted number. / 허용하는 최대 숫자. */
        final double max;

        /**
         * Creates a type node.
         * <p>
         * 형식 노드를 만든다.
         *
         * @param type type name / 형식 이름
         * @param min  minimum / 최소
         * @param max  maximum / 최대
         */
        Terminal(String type, double min, double max) {
            this.type = type;
            this.min = min;
            this.max = max;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            if (pos >= in.items.size()) {
                return false;
            }
            CssValue v = Terminals.match(type, min, max, in.items.get(pos), in.env);
            return v != null && k.next(pos + 1, v);
        }
    }

    /**
     * Matches a reference to a named definition.
     * <p>
     * 이름 있는 정의에 대한 참조를 맞춘다.
     */
    private static final class Ref extends Node {
        /** Definition name. / 정의 이름. */
        final String name;

        /**
         * Creates a reference node.
         * <p>
         * 참조 노드를 만든다.
         *
         * @param name definition name / 정의 이름
         */
        Ref(String name) {
            this.name = name;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            ValueGrammar g = in.env.getRegistry() == null ? null : in.env.getRegistry().get(name);
            if (g == null) {
                throw new IllegalStateException("undefined value definition <" + name + ">");
            }
            return g.root.match(in, pos, k);
        }
    }

    /**
     * Matches a literal comma or slash.
     * <p>
     * 리터럴 쉼표나 슬래시를 맞춘다.
     */
    private static final class Literal extends Node {
        /** True for the comma, false for the slash. / 쉼표이면 true, 슬래시이면 false. */
        final boolean comma;

        /**
         * Creates a literal node.
         * <p>
         * 리터럴 노드를 만든다.
         *
         * @param comma true for a comma / 쉼표이면 true
         */
        Literal(boolean comma) {
            this.comma = comma;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            if (pos >= in.items.size() || !(in.items.get(pos) instanceof CssToken)) {
                return false;
            }
            CssToken t = (CssToken) in.items.get(pos);
            boolean ok = comma ? t.is(CssToken.COMMA) : t.isDelim('/');
            return ok && k.next(pos + 1, comma ? CssOperator.COMMA : CssOperator.SLASH);
        }
    }

    /**
     * Matches a juxtaposition of nodes.
     * <p>
     * 노드의 나열을 맞춘다.
     */
    private static final class Seq extends Node {
        /** The parts in order. / 순서대로 놓인 부분. */
        final Node[] parts;

        /**
         * Creates a sequence node.
         * <p>
         * 나열 노드를 만든다.
         *
         * @param parts the parts / 부분
         */
        Seq(List<Node> parts) {
            this.parts = parts.toArray(new Node[parts.size()]);
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            return from(in, 0, pos, new ArrayList<CssValue>(), k);
        }

        /**
         * Matches the parts from an index on.
         * <p>
         * 위치부터의 부분들을 맞춘다.
         *
         * @param in  input / 입력
         * @param idx index of the next part / 다음 부분의 위치
         * @param pos position / 위치
         * @param acc captured values so far / 지금까지 붙잡은 값
         * @param k   continuation / 연속
         * @return true if accepted / 받아들여졌으면 true
         */
        private boolean from(final Input in, final int idx, int pos, final List<CssValue> acc, final Cont k) {
            if (idx == parts.length) {
                return k.next(pos, build(acc));
            }
            return parts[idx].match(in, pos, new Cont() {
                public boolean next(int p, CssValue v) {
                    int mark = acc.size();
                    if (v != null) {
                        acc.add(v);
                    }
                    boolean r = from(in, idx + 1, p, acc, k);
                    // 실패하면 이 부분이 붙잡은 값을 되돌린다.
                    while (!r && acc.size() > mark) {
                        acc.remove(acc.size() - 1);
                    }
                    return r;
                }
            });
        }
    }

    /**
     * Matches one of several alternatives.
     * <p>
     * 여러 대안 중 하나를 맞춘다.
     */
    private static final class Alt extends Node {
        /** The alternatives. / 대안들. */
        final Node[] options;

        /**
         * Creates an alternative node.
         * <p>
         * 대안 노드를 만든다.
         *
         * @param options the alternatives / 대안
         */
        Alt(List<Node> options) {
            this.options = options.toArray(new Node[options.size()]);
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            // 대안을 차례로 시도해 하나라도 받아들여지면 끝낸다.
            for (int i = 0; i < options.length; i++) {
                if (options[i].match(in, pos, k)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Matches nodes in any order: all of them (<code>&amp;&amp;</code>) or at least one (<code>||</code>).
     * <p>
     * 순서에 상관없이 노드를 맞춘다: 모두(<code>&amp;&amp;</code>) 또는 하나 이상(<code>||</code>).
     */
    private static final class Any extends Node {
        /** The parts. / 부분들. */
        final Node[] parts;
        /** True if every part is required. / 모든 부분이 필요하면 true. */
        final boolean all;

        /**
         * Creates an any-order node.
         * <p>
         * 순서 무관 노드를 만든다.
         *
         * @param parts the parts / 부분
         * @param all   true for <code>&amp;&amp;</code> / <code>&amp;&amp;</code> 이면 true
         */
        Any(List<Node> parts, boolean all) {
            this.parts = parts.toArray(new Node[parts.size()]);
            this.all = all;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            return go(in, new boolean[parts.length], 0, pos, new ArrayList<CssValue>(), k);
        }

        /**
         * Tries every unused part at the position, then finishes.
         * <p>
         * 위치에서 쓰지 않은 부분을 모두 시도한 뒤 마무리한다.
         *
         * @param in    input / 입력
         * @param used  which parts were used / 쓴 부분
         * @param count number of used parts / 쓴 부분의 수
         * @param pos   position / 위치
         * @param acc   captured values / 붙잡은 값
         * @param k     continuation / 연속
         * @return true if accepted / 받아들여졌으면 true
         */
        private boolean go(final Input in, final boolean[] used, final int count, int pos, final List<CssValue> acc, final Cont k) {
            // 쓰지 않은 부분을 하나씩 이 위치에 맞춰 본다.
            for (int i = 0; i < parts.length; i++) {
                if (used[i]) {
                    continue;
                }
                final int idx = i;
                used[idx] = true;
                boolean r = parts[idx].match(in, pos, new Cont() {
                    public boolean next(int p, CssValue v) {
                        int mark = acc.size();
                        if (v != null) {
                            acc.add(v);
                        }
                        boolean r2 = go(in, used, count + 1, p, acc, k);
                        // 실패하면 붙잡은 값을 되돌린다.
                        while (!r2 && acc.size() > mark) {
                            acc.remove(acc.size() - 1);
                        }
                        return r2;
                    }
                });
                used[idx] = false;
                if (r) {
                    return true;
                }
            }
            boolean done = all ? count == parts.length : count >= 1;
            return done && k.next(pos, build(acc));
        }
    }

    /**
     * Matches a repeated node: <code>?</code>, <code>*</code>, <code>+</code>, <code>{A,B}</code> and the comma separated <code>#</code>.
     * <p>
     * 반복되는 노드를 맞춘다: <code>?</code>, <code>*</code>, <code>+</code>, <code>{A,B}</code>, 쉼표로 구분한 <code>#</code>.
     */
    private static final class Rep extends Node {
        /** The repeated node. / 반복되는 노드. */
        final Node node;
        /** Minimum count. / 최소 횟수. */
        final int min;
        /** Maximum count. / 최대 횟수. */
        final int max;
        /** True for comma separated repetition. / 쉼표로 구분한 반복이면 true. */
        final boolean comma;

        /**
         * Creates a repetition node.
         * <p>
         * 반복 노드를 만든다.
         *
         * @param node  repeated node / 반복되는 노드
         * @param min   minimum / 최소
         * @param max   maximum / 최대
         * @param comma comma separated / 쉼표 구분
         */
        Rep(Node node, int min, int max, boolean comma) {
            this.node = node;
            this.min = min;
            this.max = max;
            this.comma = comma;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            return rep(in, 0, pos, new ArrayList<CssValue>(), k);
        }

        /**
         * Matches as many repetitions as possible first, then fewer.
         * <p>
         * 가능한 많은 반복을 먼저 맞추고 그다음 더 적게 맞춘다.
         *
         * @param in    input / 입력
         * @param count repetitions so far / 지금까지의 반복 수
         * @param pos   position / 위치
         * @param acc   captured values / 붙잡은 값
         * @param k     continuation / 연속
         * @return true if accepted / 받아들여졌으면 true
         */
        private boolean rep(final Input in, final int count, final int pos, final List<CssValue> acc, final Cont k) {
            if (count < max) {
                int p = pos;
                boolean canTry = true;
                if (comma && count > 0) {
                    if (p < in.items.size() && in.items.get(p) instanceof CssToken && ((CssToken) in.items.get(p)).is(CssToken.COMMA)) {
                        p++;
                    } else {
                        canTry = false;
                    }
                }
                if (canTry) {
                    final int start = p;
                    boolean r = node.match(in, start, new Cont() {
                        public boolean next(int p2, CssValue v) {
                            if (p2 == start && v == null) {
                                return false;
                            }
                            int mark = acc.size();
                            if (v != null) {
                                acc.add(v);
                            }
                            boolean r2 = rep(in, count + 1, p2, acc, k);
                            // 실패하면 붙잡은 값을 되돌린다.
                            while (!r2 && acc.size() > mark) {
                                acc.remove(acc.size() - 1);
                            }
                            return r2;
                        }
                    });
                    if (r) {
                        return true;
                    }
                }
            }
            if (count < min) {
                return false;
            }
            if (comma) {
                return k.next(pos, new CssList(acc, true));
            }
            return k.next(pos, build(acc));
        }
    }

    /**
     * Requires the wrapped node to consume at least one component (the <code>!</code> multiplier of groups).
     * <p>
     * 감싼 노드가 구성 요소를 하나 이상 소비하도록 요구한다(그룹의 <code>!</code> 표기).
     */
    private static final class NonEmpty extends Node {
        /** The wrapped node. / 감싼 노드. */
        final Node node;

        /**
         * Creates the wrapper.
         * <p>
         * 감싸는 노드를 만든다.
         *
         * @param node the node / 노드
         */
        NonEmpty(Node node) {
            this.node = node;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, final int pos, final Cont k) {
            return node.match(in, pos, new Cont() {
                public boolean next(int p, CssValue v) {
                    return p > pos && k.next(p, v);
                }
            });
        }
    }

    /**
     * Matches a function notation such as <code>translate(&lt;length&gt;, &lt;length&gt;?)</code>.
     * <p>
     * <code>translate(&lt;length&gt;, &lt;length&gt;?)</code> 같은 함수 표기를 맞춘다.
     */
    private static final class Func extends Node {
        /** Lower-case function name. / 소문자 함수 이름. */
        final String name;
        /** Grammar of the arguments. / 인수의 문법. */
        final Node args;

        /**
         * Creates a function node.
         * <p>
         * 함수 노드를 만든다.
         *
         * @param name function name / 함수 이름
         * @param args argument grammar / 인수 문법
         */
        Func(String name, Node args) {
            this.name = CssKeyword.lower(name);
            this.args = args;
        }

        /** Matches this node at a position. 이 노드를 위치에서 맞춘다. */
        boolean match(Input in, int pos, Cont k) {
            if (pos >= in.items.size() || !(in.items.get(pos) instanceof CssFunction) || !((CssFunction) in.items.get(pos)).isNamed(name)) {
                return false;
            }
            final List<ComponentValue> inner = prepare(((CssFunction) in.items.get(pos)).getArguments());
            final CssValue[] res = new CssValue[1];
            boolean ok = args.match(new Input(inner, in.env), 0, new Cont() {
                public boolean next(int p, CssValue v) {
                    if (p != inner.size()) {
                        return false;
                    }
                    res[0] = v;
                    return true;
                }
            });
            return ok && k.next(pos + 1, new CssFunctionValue(name, split(res[0])));
        }

        /**
         * Splits a captured argument value at the literal commas.
         * <p>
         * 붙잡은 인수 값을 리터럴 쉼표에서 나눈다.
         *
         * @param v the captured value or null / 붙잡은 값 또는 null
         * @return the arguments / 인수들
         */
        private static List<CssValue> split(CssValue v) {
            List<CssValue> out = new ArrayList<CssValue>();
            if (v == null) {
                return out;
            }
            if (v instanceof CssList && ((CssList) v).isComma()) {
                return new ArrayList<CssValue>(((CssList) v).getItems());
            }
            List<CssValue> flat = new ArrayList<CssValue>();
            flatten(v, flat);
            List<CssValue> cur = new ArrayList<CssValue>();
            // 리터럴 쉼표마다 지금까지 모은 값을 인수 하나로 확정한다.
            for (int i = 0; i < flat.size(); i++) {
                if (flat.get(i) == CssOperator.COMMA) {
                    out.add(cur.size() == 1 ? cur.get(0) : new CssList(cur, false));
                    cur = new ArrayList<CssValue>();
                } else {
                    cur.add(flat.get(i));
                }
            }
            out.add(cur.size() == 1 ? cur.get(0) : new CssList(cur, false));
            return out;
        }

        /**
         * Flattens nested space separated lists so that literal commas inside optional groups become visible.
         * <p>
         * 선택 그룹 안의 리터럴 쉼표가 보이도록 중첩된 공백 구분 목록을 편다.
         *
         * @param v   the value / 값
         * @param out receives the flat items / 편 항목을 받는다
         */
        private static void flatten(CssValue v, List<CssValue> out) {
            if (v instanceof CssList && !((CssList) v).isComma()) {
                List<CssValue> items = ((CssList) v).getItems();
                // 중첩 목록의 항목을 차례로 편다.
                for (int i = 0; i < items.size(); i++) {
                    flatten(items.get(i), out);
                }
            } else {
                out.add(v);
            }
        }

        /**
         * Wraps a value in a list.
         * <p>
         * 값을 목록으로 감싼다.
         *
         * @param v value / 값
         * @return one-item list / 한 항목 목록
         */
        private static List<CssValue> singleton(CssValue v) {
            List<CssValue> l = new ArrayList<CssValue>();
            l.add(v);
            return l;
        }
    }

    /**
     * The parser of definition text.
     * <p>
     * 정의 문자열의 파서.
     */
    private static final class GrammarParser {
        /** The component values of the definition. / 정의의 컴포넌트 값. */
        private final List<ComponentValue> in;
        /** Current index. / 현재 위치. */
        private int pos;
        /** The whole definition text for error messages. / 오류 메시지용 정의 전체 문자열. */
        private final String text;

        /**
         * Creates a parser.
         * <p>
         * 파서를 만든다.
         *
         * @param in   component values / 컴포넌트 값
         * @param text definition text / 정의 문자열
         */
        GrammarParser(List<ComponentValue> in, String text) {
            this.in = in;
            this.text = text;
        }

        /**
         * Skips whitespace.
         * <p>
         * 공백을 건너뛴다.
         */
        private void ws() {
            // 연속된 공백 토큰을 건너뛴다.
            while (pos < in.size() && CssParser.isWhitespace(in.get(pos))) {
                pos++;
            }
        }

        /**
         * Reports a malformed definition.
         * <p>
         * 잘못된 정의를 보고한다.
         *
         * @param msg the problem / 문제
         * @return never returns / 반환하지 않음
         */
        private IllegalArgumentException bad(String msg) {
            return new IllegalArgumentException("bad value definition '" + text + "': " + msg);
        }

        /**
         * Tells whether a double bar or ampersand pair starts at the current index.
         * <p>
         * 현재 위치에서 겹친 막대나 앰퍼샌드 쌍이 시작하는지 알려 준다.
         *
         * @param c the character / 문자
         * @return true if two of them are adjacent / 둘이 붙어 있으면 true
         */
        private boolean pair(char c) {
            return pos + 1 < in.size() && isDelim(in.get(pos), c) && isDelim(in.get(pos + 1), c);
        }

        /**
         * Tells whether a component value is a delimiter.
         * <p>
         * 컴포넌트 값이 구분 문자인지 알려 준다.
         *
         * @param v the value / 값
         * @param c the character / 문자
         * @return true if it is that delimiter / 그 구분 문자이면 true
         */
        private static boolean isDelim(ComponentValue v, char c) {
            return v instanceof CssToken && ((CssToken) v).isDelim(c);
        }

        /**
         * Parses alternatives separated by a single bar.
         * <p>
         * 막대 하나로 구분한 대안을 파싱한다.
         *
         * @return the node / 노드
         */
        Node parseAlt() {
            List<Node> list = new ArrayList<Node>();
            list.add(parseOr());
            ws();
            // 단일 막대가 이어지는 동안 대안을 더한다.
            while (pos < in.size() && isDelim(in.get(pos), '|') && !pair('|')) {
                pos++;
                list.add(parseOr());
                ws();
            }
            if (pos < in.size()) {
                throw bad("unexpected " + in.get(pos).toCssText());
            }
            return list.size() == 1 ? list.get(0) : new Alt(list);
        }

        /**
         * Parses nodes joined by a double bar.
         * <p>
         * 겹친 막대로 이은 노드를 파싱한다.
         *
         * @return the node / 노드
         */
        private Node parseOr() {
            List<Node> list = new ArrayList<Node>();
            list.add(parseAnd());
            ws();
            // 겹친 막대가 이어지는 동안 부분을 더한다.
            while (pair('|')) {
                pos += 2;
                list.add(parseAnd());
                ws();
            }
            return list.size() == 1 ? list.get(0) : new Any(list, false);
        }

        /**
         * Parses nodes joined by a double ampersand.
         * <p>
         * 겹친 앰퍼샌드로 이은 노드를 파싱한다.
         *
         * @return the node / 노드
         */
        private Node parseAnd() {
            List<Node> list = new ArrayList<Node>();
            list.add(parseSeq());
            ws();
            // 겹친 앰퍼샌드가 이어지는 동안 부분을 더한다.
            while (pair('&')) {
                pos += 2;
                list.add(parseSeq());
                ws();
            }
            return list.size() == 1 ? list.get(0) : new Any(list, true);
        }

        /**
         * Parses a juxtaposition.
         * <p>
         * 나열을 파싱한다.
         *
         * @return the node / 노드
         */
        private Node parseSeq() {
            List<Node> list = new ArrayList<Node>();
            ws();
            // 막대나 앰퍼샌드나 끝을 만날 때까지 항을 읽는다.
            while (pos < in.size() && !isDelim(in.get(pos), '|') && !isDelim(in.get(pos), '&')) {
                list.add(parseTerm());
                ws();
            }
            if (list.isEmpty()) {
                throw bad("empty alternative");
            }
            return list.size() == 1 ? list.get(0) : new Seq(list);
        }

        /**
         * Parses an atom followed by multipliers.
         * <p>
         * 원자와 뒤따르는 반복 표기를 파싱한다.
         *
         * @return the node / 노드
         */
        private Node parseTerm() {
            Node n = parseAtom();
            // 반복 표기가 이어지는 동안 노드를 감싼다.
            while (pos < in.size()) {
                ComponentValue v = in.get(pos);
                if (isDelim(v, '?')) {
                    n = new Rep(n, 0, 1, false);
                } else if (isDelim(v, '*')) {
                    n = new Rep(n, 0, MANY, false);
                } else if (isDelim(v, '+')) {
                    n = new Rep(n, 1, MANY, false);
                } else if (isDelim(v, '#')) {
                    n = new Rep(n, 1, MANY, true);
                    if (pos + 1 < in.size() && in.get(pos + 1) instanceof CssBlock && ((CssBlock) in.get(pos + 1)).getOpenType() == CssToken.LEFT_BRACE) {
                        int[] r = counts((CssBlock) in.get(pos + 1));
                        n = new Rep(((Rep) n).node, r[0], r[1], true);
                        pos++;
                    }
                } else if (isDelim(v, '!')) {
                    n = new NonEmpty(n);
                } else if (v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_BRACE) {
                    int[] r = counts((CssBlock) v);
                    n = new Rep(n, r[0], r[1], false);
                } else {
                    break;
                }
                pos++;
            }
            return n;
        }

        /**
         * Reads the counts of a <code>{A,B}</code> multiplier.
         * <p>
         * <code>{A,B}</code> 반복 표기의 횟수를 읽는다.
         *
         * @param b the brace block / 중괄호 블록
         * @return minimum and maximum / 최소와 최대
         */
        private int[] counts(CssBlock b) {
            List<ComponentValue> c = prepare(b.getContents());
            if (c.size() == 1 && c.get(0) instanceof CssToken) {
                int a = (int) ((CssToken) c.get(0)).getNumber();
                return new int[] {a, a};
            }
            if (c.size() == 2 && c.get(1) instanceof CssToken && ((CssToken) c.get(1)).is(CssToken.COMMA)) {
                return new int[] {(int) ((CssToken) c.get(0)).getNumber(), MANY};
            }
            if (c.size() == 3) {
                return new int[] {(int) ((CssToken) c.get(0)).getNumber(), (int) ((CssToken) c.get(2)).getNumber()};
            }
            throw bad("bad repetition count");
        }

        /**
         * Parses one atom: keyword, type reference, literal, bracketed group or function.
         * <p>
         * 원자 하나를 파싱한다: 키워드, 형식 참조, 리터럴, 대괄호 그룹, 함수.
         *
         * @return the node / 노드
         */
        private Node parseAtom() {
            ws();
            if (pos >= in.size()) {
                throw bad("unexpected end");
            }
            ComponentValue v = in.get(pos);
            if (v instanceof CssToken) {
                CssToken t = (CssToken) v;
                if (t.is(CssToken.IDENT)) {
                    pos++;
                    return new Keyword(t.getValue());
                }
                if (t.is(CssToken.COMMA)) {
                    pos++;
                    return new Literal(true);
                }
                if (t.isDelim('/')) {
                    pos++;
                    return new Literal(false);
                }
                if (t.isDelim('<')) {
                    return reference();
                }
                throw bad("unexpected " + t.toCssText());
            }
            if (v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_BRACKET) {
                pos++;
                return new GrammarParser(((CssBlock) v).getContents(), text).parseAlt();
            }
            if (v instanceof CssFunction) {
                pos++;
                CssFunction f = (CssFunction) v;
                return new Func(f.getName(), new GrammarParser(f.getArguments(), text).parseAlt());
            }
            throw bad("unexpected " + v.toCssText());
        }

        /**
         * Parses <code>&lt;name [min,max]&gt;</code>.
         * <p>
         * <code>&lt;name [min,max]&gt;</code> 를 파싱한다.
         *
         * @return a type or reference node / 형식 또는 참조 노드
         */
        private Node reference() {
            pos++;
            ws();
            if (pos >= in.size() || !(in.get(pos) instanceof CssToken) || !((CssToken) in.get(pos)).is(CssToken.IDENT)) {
                throw bad("a type name is missing");
            }
            String name = CssKeyword.lower(((CssToken) in.get(pos)).getValue());
            pos++;
            ws();
            double min = Double.NEGATIVE_INFINITY;
            double max = Double.POSITIVE_INFINITY;
            if (pos < in.size() && in.get(pos) instanceof CssBlock && ((CssBlock) in.get(pos)).getOpenType() == CssToken.LEFT_BRACKET) {
                List<ComponentValue> r = prepare(((CssBlock) in.get(pos)).getContents());
                if (r.size() != 3) {
                    throw bad("bad range");
                }
                min = bound(r.get(0));
                max = bound(r.get(2));
                pos++;
                ws();
            }
            if (pos >= in.size() || !isDelim(in.get(pos), '>')) {
                throw bad("missing > after <" + name);
            }
            pos++;
            return Terminals.isType(name) ? (Node) new Terminal(name, min, max) : new Ref(name);
        }

        /**
         * Reads a range bound (a number, <code>inf</code> or <code>-inf</code>).
         * <p>
         * 범위 경계(숫자, <code>inf</code>, <code>-inf</code>)를 읽는다.
         *
         * @param v the component value / 컴포넌트 값
         * @return the bound / 경계
         */
        private double bound(ComponentValue v) {
            if (v instanceof CssToken) {
                CssToken t = (CssToken) v;
                if (t.is(CssToken.NUMBER)) {
                    return t.getNumber();
                }
                if (t.isIdent("inf")) {
                    return Double.POSITIVE_INFINITY;
                }
                if (t.isIdent("-inf")) {
                    return Double.NEGATIVE_INFINITY;
                }
            }
            throw bad("bad range bound");
        }
    }
}
