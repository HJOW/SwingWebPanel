package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.media.MediaQueryList;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * The prelude of an <code>@container</code> rule (CSS Conditional Level 5, section 4, "Container Queries"): an optional container name and a condition
 * built from size features in parentheses (<code>(min-width: 400px)</code>, <code>(width &gt; 400px)</code>, <code>(inline-size &lt; 30em)</code>,
 * <code>(aspect-ratio &gt; 1/1)</code>, <code>(orientation: landscape)</code>), custom property queries (<code>style(--theme: dark)</code>), nested
 * conditions, and the operators <code>not</code>, <code>and</code>, <code>or</code>. A size feature is evaluated by the media query evaluator against an
 * environment whose size is the content box of the query container (the logical features <code>inline-size</code> and <code>block-size</code> are
 * rewritten to the physical ones by the writing mode of the container). Anything the grammar does not recognise is "unknown", which counts as false;
 * an invalid prelude makes the rule never match.
 * <p>
 * <code>@container</code> 규칙의 전문(CSS Conditional Level 5, 4 절 "Container Queries"): 선택적인 컨테이너 이름, 그리고 괄호 안의 크기 특성
 * (<code>(min-width: 400px)</code>, <code>(width &gt; 400px)</code>, <code>(inline-size &lt; 30em)</code>, <code>(aspect-ratio &gt; 1/1)</code>,
 * <code>(orientation: landscape)</code>), 사용자 정의 속성 질의(<code>style(--theme: dark)</code>), 중첩된 조건, 연산자 <code>not</code>, <code>and</code>,
 * <code>or</code> 로 만든 조건. 크기 특성은 질의 컨테이너의 content box 크기를 가진 환경에서 미디어 질의 평가기로 평가한다(논리 특성 <code>inline-size</code>,
 * <code>block-size</code> 는 컨테이너의 쓰기 모드에 따라 물리 특성으로 바꾼다). 문법이 알아보지 못하는 것은 "unknown" 이며 거짓으로 센다. 올바르지 않은 전문은
 * 규칙이 절대 일치하지 않게 한다.
 */
public final class ContainerCondition {

    /** Bit of the horizontal axis. / 가로축 비트. */
    public static final int AXIS_WIDTH = 1;
    /** Bit of the vertical axis. / 세로축 비트. */
    public static final int AXIS_HEIGHT = 2;

    /**
     * Looks up the computed custom properties of the container for <code>style()</code> queries.
     * <p>
     * <code>style()</code> 질의를 위해 컨테이너의 계산된 사용자 정의 속성을 조회한다.
     */
    public interface StyleLookup {
        /**
         * Returns the computed value of a custom property.
         * <p>
         * 사용자 정의 속성의 계산 값을 반환한다.
         *
         * @param name the property name including <code>--</code> / <code>--</code> 를 포함한 속성 이름
         * @return the value text, or null if the property is not defined / 값 문자열. 정의되지 않았으면 null
         */
        String customProperty(String name);
    }

    /** The evaluation inputs. / 평가 입력. */
    private static final class Ctx {
        /** Width of the container content box. / 컨테이너 content box 의 너비. */
        final double width;
        /** Height of the container content box. / 컨테이너 content box 의 높이. */
        final double height;
        /** True if the container has a vertical writing mode. / 컨테이너가 세로쓰기이면 true. */
        final boolean vertical;
        /** The style lookup, may be null. / 스타일 조회. null 일 수 있다. */
        final StyleLookup style;

        /**
         * Creates the inputs.
         * <p>
         * 입력을 만든다.
         *
         * @param width    width / 너비
         * @param height   height / 높이
         * @param vertical vertical writing mode / 세로쓰기 여부
         * @param style    style lookup or null / 스타일 조회 또는 null
         */
        Ctx(double width, double height, boolean vertical, StyleLookup style) {
            this.width = width;
            this.height = height;
            this.vertical = vertical;
            this.style = style;
        }
    }

    /** A node of the condition tree; the result is TRUE, FALSE or null for unknown. / 조건 트리의 노드. 결과는 TRUE, FALSE, unknown 이면 null. */
    private abstract static class Node {
        /**
         * Evaluates the node.
         * <p>
         * 노드를 평가한다.
         *
         * @param c the inputs / 입력
         * @return true, false or null for unknown / true, false, unknown 이면 null
         */
        abstract Boolean eval(Ctx c);

        /**
         * Collects the physical axes the size features of the node need.
         * <p>
         * 노드의 크기 특성이 필요로 하는 물리 축을 모은다.
         *
         * @param vertical true for a vertical container / 세로 컨테이너이면 true
         * @return bits of {@link ContainerCondition#AXIS_WIDTH} and {@link ContainerCondition#AXIS_HEIGHT} / 축 비트
         */
        abstract int axes(boolean vertical);

        /**
         * Tells whether the node contains a size feature.
         * <p>
         * 노드에 크기 특성이 있는지 알려 준다.
         *
         * @return true if one occurs / 있으면 true
         */
        abstract boolean hasSize();

        /**
         * Tells whether the node contains a style query.
         * <p>
         * 노드에 스타일 질의가 있는지 알려 준다.
         *
         * @return true if one occurs / 있으면 true
         */
        abstract boolean hasStyle();
    }

    /** A negated condition, <code>not (...)</code>. / 부정한 조건 <code>not (...)</code>. */
    private static final class Not extends Node {
        /** The operand. / 피연산자. */
        final Node n;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param n the operand / 피연산자
         */
        Not(Node n) {
            this.n = n;
        }

        /**
         * Negates; unknown stays unknown.
         * <p>
         * 부정한다. unknown 은 그대로이다.
         *
         * @param c the inputs / 입력
         * @return the result / 결과
         */
        Boolean eval(Ctx c) {
            Boolean r = n.eval(c);
            return r == null ? null : Boolean.valueOf(!r.booleanValue());
        }

        /**
         * Returns the axes of the operand.
         * <p>
         * 피연산자의 축을 반환한다.
         *
         * @param v vertical container / 세로 컨테이너
         * @return axis bits / 축 비트
         */
        int axes(boolean v) {
            return n.axes(v);
        }

        /**
         * Tells whether the operand has a size feature.
         * <p>
         * 피연산자에 크기 특성이 있는지 알려 준다.
         *
         * @return true if so / 있으면 true
         */
        boolean hasSize() {
            return n.hasSize();
        }

        /**
         * Tells whether the operand has a style query.
         * <p>
         * 피연산자에 스타일 질의가 있는지 알려 준다.
         *
         * @return true if so / 있으면 true
         */
        boolean hasStyle() {
            return n.hasStyle();
        }
    }

    /** <code>and</code> / <code>or</code> of operands. / 피연산자의 <code>and</code> / <code>or</code>. */
    private static final class Logic extends Node {
        /** True for and, false for or. / and 이면 true, or 이면 false. */
        final boolean and;
        /** The operands. / 피연산자. */
        final List<Node> list;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param and  true for and / and 이면 true
         * @param list the operands / 피연산자
         */
        Logic(boolean and, List<Node> list) {
            this.and = and;
            this.list = list;
        }

        /**
         * Evaluates with the three-valued logic of CSS (unknown is neither true nor false).
         * <p>
         * CSS 의 3값 논리로 평가한다(unknown 은 참도 거짓도 아니다).
         *
         * @param c the inputs / 입력
         * @return the result / 결과
         */
        Boolean eval(Ctx c) {
            boolean unknown = false;
            // 피연산자를 차례로 평가한다: and 는 거짓이 하나라도 있으면 거짓, or 는 참이 하나라도 있으면 참이다.
            for (int i = 0; i < list.size(); i++) {
                Boolean r = list.get(i).eval(c);
                if (r == null) {
                    unknown = true;
                } else if (r.booleanValue() != and) {
                    return Boolean.valueOf(!and);
                }
            }
            return unknown ? null : Boolean.valueOf(and);
        }

        /**
         * Returns the union of the axes of the operands.
         * <p>
         * 피연산자의 축의 합집합을 반환한다.
         *
         * @param v vertical container / 세로 컨테이너
         * @return axis bits / 축 비트
         */
        int axes(boolean v) {
            int a = 0;
            // 피연산자의 축을 합친다.
            for (int i = 0; i < list.size(); i++) {
                a |= list.get(i).axes(v);
            }
            return a;
        }

        /**
         * Tells whether an operand has a size feature.
         * <p>
         * 피연산자에 크기 특성이 있는지 알려 준다.
         *
         * @return true if so / 있으면 true
         */
        boolean hasSize() {
            // 피연산자 중 하나라도 크기 특성이 있으면 true.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).hasSize()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Tells whether an operand has a style query.
         * <p>
         * 피연산자에 스타일 질의가 있는지 알려 준다.
         *
         * @return true if so / 있으면 true
         */
        boolean hasStyle() {
            // 피연산자 중 하나라도 스타일 질의가 있으면 true.
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).hasStyle()) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Anything the grammar does not recognise (general-enclosed). / 문법이 알아보지 못하는 것(general-enclosed). */
    private static final class Unknown extends Node {
        /**
         * Always unknown.
         * <p>
         * 항상 unknown.
         *
         * @param c the inputs / 입력
         * @return null / null
         */
        Boolean eval(Ctx c) {
            return null;
        }

        /**
         * Needs no axis.
         * <p>
         * 축이 필요 없다.
         *
         * @param v vertical container / 세로 컨테이너
         * @return 0 / 0
         */
        int axes(boolean v) {
            return 0;
        }

        /**
         * Has no size feature.
         * <p>
         * 크기 특성이 없다.
         *
         * @return false / false
         */
        boolean hasSize() {
            return false;
        }

        /**
         * Has no style query.
         * <p>
         * 스타일 질의가 없다.
         *
         * @return false / false
         */
        boolean hasStyle() {
            return false;
        }
    }

    /** A size feature such as <code>(min-width: 400px)</code>. / <code>(min-width: 400px)</code> 같은 크기 특성. */
    private static final class Size extends Node {
        /** The tokens inside the parentheses. / 괄호 안의 토큰. */
        final List<ComponentValue> inner;
        /** The media query for a horizontal container, parsed lazily. / 가로쓰기 컨테이너용 미디어 질의. 처음 쓸 때 파싱한다. */
        private MediaQueryList horizontal;
        /** The media query for a vertical container, parsed lazily. / 세로쓰기 컨테이너용 미디어 질의. 처음 쓸 때 파싱한다. */
        private MediaQueryList verticalList;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param inner the tokens inside the parentheses / 괄호 안의 토큰
         */
        Size(List<ComponentValue> inner) {
            this.inner = inner;
        }

        /**
         * Evaluates the feature against the size of the container.
         * <p>
         * 컨테이너 크기에 대해 특성을 평가한다.
         *
         * @param c the inputs / 입력
         * @return the result / 결과
         */
        Boolean eval(Ctx c) {
            MediaQueryList q;
            if (c.vertical) {
                if (verticalList == null) {
                    verticalList = MediaQueryList.parse("(" + rewrite(inner, true) + ")");
                }
                q = verticalList;
            } else {
                if (horizontal == null) {
                    horizontal = MediaQueryList.parse("(" + rewrite(inner, false) + ")");
                }
                q = horizontal;
            }
            int need = axes(c.vertical);
            if (need == 0) {
                return null;
            }
            MediaEnvironment env = new MediaEnvironment(c.width, c.height);
            return Boolean.valueOf(q.matches(env));
        }

        /**
         * Returns the axes the feature needs: width and inline-size/block-size by the writing mode, aspect-ratio and orientation both.
         * <p>
         * 특성이 필요로 하는 축을 반환한다: width 등과 쓰기 모드에 따른 inline-size/block-size, aspect-ratio 와 orientation 은 둘 다.
         *
         * @param vertical true for a vertical container / 세로 컨테이너이면 true
         * @return axis bits / 축 비트
         */
        int axes(boolean vertical) {
            int a = 0;
            // 괄호 안의 식별자에서 특성 이름을 찾는다(min-, max- 접두사 제외).
            for (int i = 0; i < inner.size(); i++) {
                if (!(inner.get(i) instanceof CssToken) || !((CssToken) inner.get(i)).is(CssToken.IDENT)) {
                    continue;
                }
                String n = ((CssToken) inner.get(i)).getValue().toLowerCase();
                if (n.startsWith("min-") || n.startsWith("max-")) {
                    n = n.substring(4);
                }
                if (n.equals("width")) {
                    a |= AXIS_WIDTH;
                } else if (n.equals("height")) {
                    a |= AXIS_HEIGHT;
                } else if (n.equals("inline-size")) {
                    a |= vertical ? AXIS_HEIGHT : AXIS_WIDTH;
                } else if (n.equals("block-size")) {
                    a |= vertical ? AXIS_WIDTH : AXIS_HEIGHT;
                } else if (n.equals("aspect-ratio") || n.equals("orientation")) {
                    a |= AXIS_WIDTH | AXIS_HEIGHT;
                }
            }
            return a;
        }

        /**
         * A size feature is a size feature.
         * <p>
         * 크기 특성은 크기 특성이다.
         *
         * @return true / true
         */
        boolean hasSize() {
            return true;
        }

        /**
         * No style query.
         * <p>
         * 스타일 질의가 아니다.
         *
         * @return false / false
         */
        boolean hasStyle() {
            return false;
        }
    }

    /** A custom property query <code>style(--x: value)</code>. / 사용자 정의 속성 질의 <code>style(--x: value)</code>. */
    private static final class Style extends Node {
        /** The custom property name. / 사용자 정의 속성 이름. */
        final String name;
        /** The normalised value to compare with, or null to test whether the property is defined. / 비교할 정규화한 값. 정의 여부만 보려면 null. */
        final String value;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param name  the name / 이름
         * @param value the value or null / 값 또는 null
         */
        Style(String name, String value) {
            this.name = name;
            this.value = value;
        }

        /**
         * Compares the computed custom property of the container.
         * <p>
         * 컨테이너의 계산된 사용자 정의 속성을 비교한다.
         *
         * @param c the inputs / 입력
         * @return the result / 결과
         */
        Boolean eval(Ctx c) {
            if (c.style == null) {
                return null;
            }
            String actual = c.style.customProperty(name);
            if (value == null) {
                return Boolean.valueOf(actual != null && actual.trim().length() > 0);
            }
            return Boolean.valueOf(actual != null && normalise(actual).equals(value));
        }

        /**
         * Needs no axis.
         * <p>
         * 축이 필요 없다.
         *
         * @param v vertical container / 세로 컨테이너
         * @return 0 / 0
         */
        int axes(boolean v) {
            return 0;
        }

        /**
         * No size feature.
         * <p>
         * 크기 특성이 아니다.
         *
         * @return false / false
         */
        boolean hasSize() {
            return false;
        }

        /**
         * A style query.
         * <p>
         * 스타일 질의이다.
         *
         * @return true / true
         */
        boolean hasStyle() {
            return true;
        }
    }

    /** The container name, or null. / 컨테이너 이름. 없으면 null. */
    private final String name;

    /** The condition, or null if the prelude is invalid. / 조건. 전문이 올바르지 않으면 null. */
    private final Node root;

    /** The prelude text for serialization. / 직렬화용 전문 문자열. */
    private final String text;

    /**
     * Creates a condition.
     * <p>
     * 조건을 만든다.
     *
     * @param name the name or null / 이름 또는 null
     * @param root the condition tree or null / 조건 트리 또는 null
     * @param text the prelude text / 전문 문자열
     */
    private ContainerCondition(String name, Node root, String text) {
        this.name = name;
        this.root = root;
        this.text = text;
    }

    /**
     * Parses the prelude of an <code>@container</code> rule.
     * <p>
     * <code>@container</code> 규칙의 전문을 파싱한다.
     *
     * @param prelude the prelude component values / 전문 컴포넌트 값
     * @return the condition; never null, invalid preludes never match / 조건. null 이 아니며 올바르지 않은 전문은 절대 일치하지 않는다
     */
    public static ContainerCondition parse(List<ComponentValue> prelude) {
        List<ComponentValue> it = items(prelude);
        String text = CssBlock.join(prelude).trim();
        int pos = 0;
        String name = null;
        // 첫 식별자가 조건 연산자가 아니면 컨테이너 이름이다.
        if (!it.isEmpty() && it.get(0) instanceof CssToken && ((CssToken) it.get(0)).is(CssToken.IDENT)) {
            String w = ((CssToken) it.get(0)).getValue();
            String lw = w.toLowerCase();
            if (!lw.equals("not") && !lw.equals("and") && !lw.equals("or") && !lw.equals("none")) {
                name = w;
                pos = 1;
            }
        }
        int[] p = {pos};
        Node n = parseCondition(it, p, 0);
        if (n == null || p[0] != it.size()) {
            return new ContainerCondition(name, null, text);
        }
        return new ContainerCondition(name, n, text);
    }

    /**
     * Returns the container name the query asks for.
     * <p>
     * 질의가 요구하는 컨테이너 이름을 반환한다.
     *
     * @return the name, or null for any container / 이름. 아무 컨테이너나 되면 null
     */
    public String getName() {
        return name;
    }

    /**
     * Tells whether the prelude is valid.
     * <p>
     * 전문이 올바른지 알려 준다.
     *
     * @return true if the rule can match / 규칙이 일치할 수 있으면 true
     */
    public boolean isValid() {
        return root != null;
    }

    /**
     * Tells whether the condition has a size feature, which needs a container with size containment.
     * <p>
     * 조건에 크기 특성이 있는지 알려 준다. 크기 특성은 크기 격리가 있는 컨테이너가 필요하다.
     *
     * @return true if a size feature occurs / 크기 특성이 있으면 true
     */
    public boolean usesSize() {
        return root != null && root.hasSize();
    }

    /**
     * Tells whether the condition has a <code>style()</code> query.
     * <p>
     * 조건에 <code>style()</code> 질의가 있는지 알려 준다.
     *
     * @return true if one occurs / 있으면 true
     */
    public boolean usesStyle() {
        return root != null && root.hasStyle();
    }

    /**
     * Returns the physical axes the size features need of a container.
     * <p>
     * 크기 특성이 컨테이너에 요구하는 물리 축을 반환한다.
     *
     * @param vertical true if the container has a vertical writing mode / 컨테이너가 세로쓰기이면 true
     * @return bits of {@link #AXIS_WIDTH} and {@link #AXIS_HEIGHT} / 축 비트
     */
    public int axes(boolean vertical) {
        return root == null ? 0 : root.axes(vertical);
    }

    /**
     * Evaluates the condition for a container.
     * <p>
     * 컨테이너에 대해 조건을 평가한다.
     *
     * @param width    the width of the content box of the container / 컨테이너 content box 의 너비
     * @param height   the height of the content box / content box 의 높이
     * @param vertical true for a vertical writing mode / 세로쓰기이면 true
     * @param style    the lookup of the custom properties of the container or null / 컨테이너의 사용자 정의 속성 조회 또는 null
     * @return true if the condition holds; unknown counts as false / 조건이 성립하면 true. unknown 은 false 로 센다
     */
    public boolean matches(double width, double height, boolean vertical, StyleLookup style) {
        if (root == null) {
            return false;
        }
        return Boolean.TRUE.equals(root.eval(new Ctx(width, height, vertical, style)));
    }

    /**
     * Returns the prelude text.
     * <p>
     * 전문 문자열을 반환한다.
     *
     * @return the text as written / 쓰인 그대로의 문자열
     */
    public String toCssText() {
        return text;
    }

    /**
     * Returns the query without the container name (the CSSOM <code>containerQuery</code> of <code>CSSContainerRule</code>).
     * <p>
     * 컨테이너 이름을 뺀 질의를 반환한다(<code>CSSContainerRule</code> 의 CSSOM <code>containerQuery</code>).
     *
     * @return the text after the name, or the whole prelude if there is no name / 이름 뒤의 문자열. 이름이 없으면 전문 전체
     */
    public String getQueryText() {
        // 이름은 전문의 맨 앞에 쓰인 식별자이므로 그 길이만큼 잘라 낸다.
        if (name != null && text.startsWith(name)) {
            return text.substring(name.length()).trim();
        }
        return text;
    }

    /**
     * Parses a condition: <code>not X</code>, or <code>X (and X)*</code>, or <code>X (or X)*</code>.
     * <p>
     * 조건을 파싱한다: <code>not X</code>, <code>X (and X)*</code>, <code>X (or X)*</code>.
     *
     * @param it    the non-whitespace values / 공백이 아닌 값
     * @param p     the position (one element, advanced) / 위치(한 원소, 전진한다)
     * @param depth nesting depth / 중첩 깊이
     * @return the node or null if invalid / 노드. 올바르지 않으면 null
     */
    private static Node parseCondition(List<ComponentValue> it, int[] p, int depth) {
        if (depth > 16 || p[0] >= it.size()) {
            return null;
        }
        if (isIdent(it.get(p[0]), "not")) {
            p[0]++;
            Node x = parseInParens(it, p, depth);
            return x == null ? null : new Not(x);
        }
        Node first = parseInParens(it, p, depth);
        if (first == null) {
            return null;
        }
        List<Node> list = new ArrayList<Node>();
        list.add(first);
        String op = null;
        // and 나 or 로 이어진 피연산자를 모은다(두 연산자를 섞을 수 없다).
        while (p[0] < it.size()) {
            ComponentValue v = it.get(p[0]);
            String w = v instanceof CssToken && ((CssToken) v).is(CssToken.IDENT) ? ((CssToken) v).getValue().toLowerCase() : "";
            if (!w.equals("and") && !w.equals("or")) {
                return null;
            }
            if (op == null) {
                op = w;
            } else if (!op.equals(w)) {
                return null;
            }
            p[0]++;
            Node x = parseInParens(it, p, depth);
            if (x == null) {
                return null;
            }
            list.add(x);
        }
        return list.size() == 1 ? first : new Logic("and".equals(op), list);
    }

    /**
     * Parses a parenthesised condition or feature, or a <code>style()</code> query.
     * <p>
     * 괄호 안의 조건이나 특성, 또는 <code>style()</code> 질의를 파싱한다.
     *
     * @param it    the non-whitespace values / 공백이 아닌 값
     * @param p     the position (one element, advanced) / 위치(한 원소, 전진한다)
     * @param depth nesting depth / 중첩 깊이
     * @return the node, or null if invalid / 노드. 올바르지 않으면 null
     */
    private static Node parseInParens(List<ComponentValue> it, int[] p, int depth) {
        if (p[0] >= it.size()) {
            return null;
        }
        ComponentValue v = it.get(p[0]);
        if (v instanceof CssFunction) {
            p[0]++;
            CssFunction f = (CssFunction) v;
            return f.isNamed("style") ? styleQuery(f) : new Unknown();
        }
        if (!(v instanceof CssBlock) || ((CssBlock) v).getOpenType() != CssToken.LEFT_PAREN) {
            return null;
        }
        p[0]++;
        List<ComponentValue> inner = items(((CssBlock) v).getContents());
        if (inner.isEmpty()) {
            return new Unknown();
        }
        ComponentValue f = inner.get(0);
        boolean nested = isIdent(f, "not") || (f instanceof CssBlock && ((CssBlock) f).getOpenType() == CssToken.LEFT_PAREN)
                || (f instanceof CssFunction && ((CssFunction) f).isNamed("style"));
        if (nested) {
            int[] q = {0};
            Node n = parseCondition(inner, q, depth + 1);
            return n == null || q[0] != inner.size() ? new Unknown() : n;
        }
        // 크기 특성이다. 이름이 알려진 특성이 아니면 unknown 으로 둔다.
        Size s = new Size(inner);
        return s.axes(false) == 0 ? new Unknown() : s;
    }

    /**
     * Parses <code>style(--name: value)</code> and <code>style(--name)</code>.
     * <p>
     * <code>style(--name: value)</code> 와 <code>style(--name)</code> 를 파싱한다.
     *
     * @param f the function / 함수
     * @return the node / 노드
     */
    private static Node styleQuery(CssFunction f) {
        List<ComponentValue> a = items(f.getArguments());
        if (a.isEmpty() || !(a.get(0) instanceof CssToken) || !((CssToken) a.get(0)).is(CssToken.IDENT)
                || !((CssToken) a.get(0)).getValue().startsWith("--")) {
            return new Unknown();
        }
        String n = ((CssToken) a.get(0)).getValue();
        if (a.size() == 1) {
            return new Style(n, null);
        }
        if (a.size() < 3 || !(a.get(1) instanceof CssToken) || !((CssToken) a.get(1)).is(CssToken.COLON)) {
            return new Unknown();
        }
        return new Style(n, normalise(CssBlock.join(new ArrayList<ComponentValue>(a.subList(2, a.size())))));
    }

    /**
     * Rewrites the logical features <code>inline-size</code> and <code>block-size</code> into <code>width</code> and <code>height</code> by the
     * writing mode of the container.
     * <p>
     * 논리 특성 <code>inline-size</code> 와 <code>block-size</code> 를 컨테이너의 쓰기 모드에 따라 <code>width</code> 와 <code>height</code> 로 바꿔 쓴다.
     *
     * @param inner    the tokens inside the parentheses / 괄호 안의 토큰
     * @param vertical true for a vertical container / 세로 컨테이너이면 true
     * @return the text / 문자열
     */
    private static String rewrite(List<ComponentValue> inner, boolean vertical) {
        StringBuilder sb = new StringBuilder();
        // 토큰을 차례로 이으며 논리 특성 이름만 물리 이름으로 바꾼다.
        for (int i = 0; i < inner.size(); i++) {
            ComponentValue c = inner.get(i);
            if (c instanceof CssToken && ((CssToken) c).is(CssToken.IDENT)) {
                String n = ((CssToken) c).getValue().toLowerCase();
                String prefix = n.startsWith("min-") ? "min-" : n.startsWith("max-") ? "max-" : "";
                String base = n.substring(prefix.length());
                if (base.equals("inline-size")) {
                    sb.append(prefix).append(vertical ? "height" : "width").append(' ');
                    continue;
                }
                if (base.equals("block-size")) {
                    sb.append(prefix).append(vertical ? "width" : "height").append(' ');
                    continue;
                }
            }
            sb.append(c.toCssText()).append(' ');
        }
        return sb.toString();
    }

    /**
     * Collapses the white space of a value text for comparison.
     * <p>
     * 비교를 위해 값 문자열의 공백을 줄인다.
     *
     * @param s the text / 문자열
     * @return the normalised text / 정규화한 문자열
     */
    static String normalise(String s) {
        return s.trim().replaceAll("\\s+", " ");
    }

    /**
     * Returns the non-whitespace values.
     * <p>
     * 공백이 아닌 값을 반환한다.
     *
     * @param list the values / 값
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
     * Tells whether a value is the identifier with the given lower-case name.
     * <p>
     * 값이 주어진 소문자 이름의 식별자인지 알려 준다.
     *
     * @param v    the value / 값
     * @param name the lower-case name / 소문자 이름
     * @return true if it is / 맞으면 true
     */
    private static boolean isIdent(ComponentValue v, String name) {
        return v instanceof CssToken && ((CssToken) v).is(CssToken.IDENT) && ((CssToken) v).getValue().equalsIgnoreCase(name);
    }
}
