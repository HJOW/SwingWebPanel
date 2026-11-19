package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The generated content of <code>::before</code> and <code>::after</code> (plan item P10-04, CSS Generated Content, CSS Lists Level 3
 * section 4 for the counters): the <code>content</code> value (strings, <code>attr()</code>, quote keywords, <code>counter()</code>,
 * <code>counters()</code>) is turned into text, and the CSS counters are kept while the box tree is built in document order.
 * <code>counter-reset</code> creates a counter that is in scope for the following siblings of the element and their descendants and for
 * the element itself, <code>counter-increment</code> increments the innermost counter of that name (and creates one if there is none),
 * <code>counter-set</code> sets it, in this order (CSS Lists 3, 4.4). A counter that is created by a child of an element goes out of
 * scope when that element ends. <code>counters(name, sep)</code> joins the values of the nested counters of that name from the outermost
 * to the innermost. The styles are <code>list-style-type</code> values (decimal, alphabetic, roman, Greek, bullets).
 * <p>
 * Documented limits: the counters of table rows, row groups and columns are ignored (they do not pass through the box builder as
 * elements with generated content); the implicit <code>list-item</code> counter is the separate numbering of list items; the
 * quotes are the fixed pair of double quotes, without nesting; <code>url()</code> in <code>content</code> is not supported.
 * <p>
 * <code>::before</code> 와 <code>::after</code> 의 생성 콘텐츠(계획 P10-04, CSS Generated Content, 카운터는 CSS Lists Level 3 4 절): <code>content</code> 값(문자열,
 * <code>attr()</code>, 따옴표 키워드, <code>counter()</code>, <code>counters()</code>)을 텍스트로 만들고, 상자 트리를 문서 순서로 만드는 동안 CSS 카운터를 관리한다.
 * <code>counter-reset</code> 은 요소 자신과 뒤따르는 형제와 그 자손에서 쓸 수 있는 카운터를 만들고, <code>counter-increment</code> 는 그 이름의 가장 안쪽 카운터를
 * 증가시키며(없으면 만든다), <code>counter-set</code> 은 값을 정한다. 이 순서로 처리한다(CSS Lists 3, 4.4). 요소의 자식이 만든 카운터는 그 요소가 끝나면 범위를
 * 벗어난다. <code>counters(name, sep)</code> 는 그 이름의 중첩된 카운터 값을 바깥에서 안쪽으로 이어 붙인다. 스타일은 <code>list-style-type</code> 값이다(십진수, 알파벳,
 * 로마 숫자, 그리스 문자, 글머리 기호).
 * <p>
 * 문서화된 한계: 표의 행, 행 그룹, 열의 카운터는 무시한다(생성 콘텐츠를 가진 요소로 상자 빌더를 지나지 않는다). 암시적 <code>list-item</code> 카운터는 목록 항목의 별도
 * 번호 매기기이다. 따옴표는 고정된 큰따옴표 한 쌍이며 중첩하지 않는다. <code>content</code> 의 <code>url()</code> 은 지원하지 않는다.
 */
final class GeneratedContent {

    /** One counter. 카운터 하나. */
    private static final class Counter {
        /** The name. 이름. */
        final String name;
        /** The current value of the counter. 카운터의 현재 값. */
        int value;
        /** The node whose end closes the scope of the counter. 끝나면 카운터의 범위가 닫히는 노드. */
        final Node scopeEnd;

        /**
         * Creates a counter.
         * <p>
         * 카운터를 만든다.
         *
         * @param name     the name / 이름
         * @param value    the initial value / 처음 값
         * @param scopeEnd the node whose end closes the scope / 끝나면 범위가 닫히는 노드
         */
        Counter(String name, int value, Node scopeEnd) {
            this.name = name;
            this.value = value;
            this.scopeEnd = scopeEnd;
        }
    }

    /** The counters in scope, outermost first. 범위 안의 카운터(바깥쪽이 먼저). */
    private final List<Counter> stack = new ArrayList<Counter>();

    /** The elements with style containment that are open, innermost last (CSS Contain 2, 3.3). 열려 있는 스타일 격리 요소. 가장 안쪽이 마지막(CSS Contain 2 3.3). */
    private final List<Element> containers = new ArrayList<Element>();

    /** For every open element with style containment the size of the counter stack when it started: the counters below belong to the outside. 열려 있는 스타일 격리 요소마다 시작할 때의 카운터 스택 크기. 그 아래의 카운터는 바깥에 속한다. */
    private final List<Integer> containBase = new ArrayList<Integer>();

    /** The counter styles that write the counters (the predefined ones and the <code>@counter-style</code> definitions). 카운터를 쓰는 카운터 스타일(미리 정의된 것과 <code>@counter-style</code> 정의). */
    private final CounterStyles styles;

    /**
     * Creates the generated content of a document with the predefined counter styles.
     * <p>
     * 미리 정의된 카운터 스타일로 문서의 생성 콘텐츠를 만든다.
     */
    GeneratedContent() {
        this(CounterStyles.DEFAULT);
    }

    /**
     * Creates the generated content of a document.
     * <p>
     * 문서의 생성 콘텐츠를 만든다.
     *
     * @param styles the counter styles / 카운터 스타일
     */
    GeneratedContent(CounterStyles styles) {
        this.styles = styles;
    }

    /**
     * Applies the counter properties of the style of an element when its box is started (its <code>::before</code> comes next).
     * <p>
     * 요소의 상자를 시작할 때 요소 스타일의 카운터 속성을 적용한다(다음이 <code>::before</code>).
     *
     * @param e the element / 요소
     * @param s the style of the element / 요소의 스타일
     */
    void enter(Element e, ComputedStyle s) {
        if (s != null) {
            apply(s, e.getParentNode(), e);
            // 스타일 격리(contain: style)가 있으면 요소 자신은 바깥에 속하고 그 뒤(::before, 자손, ::after)의 증가와 설정은 요소의 하위 트리로 한정된다(CSS Contain 2 3.3).
            if ((Containment.flags(s) & Containment.STYLE) != 0) {
                containers.add(e);
                containBase.add(Integer.valueOf(stack.size()));
            }
        }
    }

    /**
     * Applies the counter properties of the style of a pseudo-element of an element (the scope of what it creates ends with the element).
     * <p>
     * 요소의 의사 요소 스타일의 카운터 속성을 적용한다(그것이 만든 카운터의 범위는 요소가 끝날 때 닫힌다).
     *
     * @param ps the style of the pseudo-element / 의사 요소의 스타일
     * @param e  the element it belongs to / 속한 요소
     */
    void applyPseudo(ComputedStyle ps, Element e) {
        apply(ps, e, e);
    }

    /**
     * Ends the scope of the counters that the children of an element created.
     * <p>
     * 요소의 자식이 만든 카운터의 범위를 끝낸다.
     *
     * @param e the element that ends / 끝나는 요소
     */
    void exit(Element e) {
        // 스타일 격리 요소가 끝나면 격리 상태를 닫는다(그 안에서 만든 카운터는 아래에서 함께 지워진다).
        if (!containers.isEmpty() && containers.get(containers.size() - 1) == e) {
            containers.remove(containers.size() - 1);
            containBase.remove(containBase.size() - 1);
        }
        // 스택 뒤에서부터 이 요소의 끝으로 범위가 닫히는 카운터를 지운다.
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (stack.get(i).scopeEnd == e) {
                stack.remove(i);
            }
        }
    }

    /**
     * Applies <code>counter-reset</code>, <code>counter-increment</code> and <code>counter-set</code> in this order.
     * <p>
     * <code>counter-reset</code>, <code>counter-increment</code>, <code>counter-set</code> 을 이 순서로 적용한다.
     *
     * @param s        the style / 스타일
     * @param scopeEnd the node whose end closes the scope of new counters / 끝나면 새 카운터의 범위가 닫히는 노드
     * @param owner    the element (a counter that is reset replaces one of the same scope) / 요소(리셋되는 카운터는 같은 범위의 것을 대체한다)
     */
    private void apply(ComputedStyle s, Node scopeEnd, Element owner) {
        List<Object[]> reset = entries(s.get(Prop.COUNTER_RESET), 0);
        // 리셋: 같은 범위에 같은 이름이 있으면 대체하고 없으면 새로 만든다.
        for (int i = 0; i < reset.size(); i++) {
            String name = (String) reset.get(i)[0];
            int v = ((Integer) reset.get(i)[1]).intValue();
            // 같은 요소가 닫는 같은 이름의 앞선 카운터(앞선 형제의 것)는 새 카운터가 대체한다.
            for (int k = stack.size() - 1; k >= 0; k--) {
                if (stack.get(k).scopeEnd == scopeEnd && name.equals(stack.get(k).name)) {
                    stack.remove(k);
                }
            }
            stack.add(new Counter(name, v, scopeEnd));
        }
        List<Object[]> inc = entries(s.get(Prop.COUNTER_INCREMENT), 1);
        // 증가: 가장 안쪽 같은 이름의 카운터를 올린다. 없으면 0 에서 시작해 만든다.
        for (int i = 0; i < inc.size(); i++) {
            Counter c = find((String) inc.get(i)[0], scopeEnd);
            c.value += ((Integer) inc.get(i)[1]).intValue();
        }
        List<Object[]> set = entries(s.get(Prop.COUNTER_SET), 0);
        // 설정: 가장 안쪽 같은 이름의 카운터 값을 정한다. 없으면 만든다.
        for (int i = 0; i < set.size(); i++) {
            Counter c = find((String) set.get(i)[0], scopeEnd);
            c.value = ((Integer) set.get(i)[1]).intValue();
        }
    }

    /**
     * Finds the innermost counter of a name, or creates it (value 0, the scope of an element that resets it).
     * <p>
     * 이름의 가장 안쪽 카운터를 찾고 없으면 만든다(값 0, 리셋하는 요소의 범위).
     *
     * @param name     the name / 이름
     * @param scopeEnd the node whose end closes the scope of a new counter / 끝나면 새 카운터의 범위가 닫히는 노드
     * @return the counter / 카운터
     */
    private Counter find(String name, Node scopeEnd) {
        // 스타일 격리 안이면 격리 요소의 하위 트리에서 만든 카운터만 찾는다. 없으면 격리 요소가 0 으로 만든 것처럼 새 카운터를 만들고 그 요소가 끝날 때 닫는다(CSS Contain 2 3.3).
        int from = containBase.isEmpty() ? 0 : containBase.get(containBase.size() - 1).intValue();
        // 안쪽(스택 뒤)부터 같은 이름을 찾는다.
        for (int i = stack.size() - 1; i >= from; i--) {
            if (name.equals(stack.get(i).name)) {
                return stack.get(i);
            }
        }
        Counter c = new Counter(name, 0, containers.isEmpty() ? scopeEnd : containers.get(containers.size() - 1));
        stack.add(c);
        return c;
    }

    /**
     * Reads the entries of a counter property: pairs of a name and an integer (the default if the integer is omitted).
     * <p>
     * 카운터 속성의 항목을 읽는다: 이름과 정수의 쌍(정수를 생략하면 기본값).
     *
     * @param v   the value (<code>none</code>, or a list of names with optional integers, flat or nested) / 값(<code>none</code> 이나 정수가 붙을 수 있는 이름의 목록. 평평하거나 중첩)
     * @param def the integer of an entry without one / 정수가 없는 항목의 정수
     * @return the entries {name, Integer} / 항목 {이름, Integer}
     */
    private static List<Object[]> entries(CssValue v, int def) {
        List<Object[]> out = new ArrayList<Object[]>();
        if (v == null || v.isKeyword("none")) {
            return out;
        }
        List<CssValue> flat = new ArrayList<CssValue>();
        flatten(v, flat);
        // 이름이 항목을 시작하고 뒤따르는 숫자가 그 값이다.
        for (int i = 0; i < flat.size(); i++) {
            CssValue t = flat.get(i);
            if (t instanceof CssKeyword) {
                int val = def;
                if (i + 1 < flat.size() && flat.get(i + 1) instanceof CssNumber) {
                    val = (int) ((CssNumber) flat.get(i + 1)).getValue();
                    i++;
                }
                out.add(new Object[] { ((CssKeyword) t).getName(), Integer.valueOf(val) });
            }
        }
        return out;
    }

    /**
     * Flattens nested lists into a list of their leaves.
     * <p>
     * 중첩 목록을 잎의 목록으로 편다.
     *
     * @param v   the value / 값
     * @param out receives the leaves / 잎을 받는다
     */
    private static void flatten(CssValue v, List<CssValue> out) {
        if (v instanceof CssList) {
            // 목록의 항목을 차례로 편다.
            for (int i = 0; i < ((CssList) v).size(); i++) {
                flatten(((CssList) v).get(i), out);
            }
        } else {
            out.add(v);
        }
    }

    // ---------------------------------------------------------------------------------------------- content text

    /**
     * Returns the text of the <code>content</code> of a pseudo-element style.
     * <p>
     * 의사 요소 스타일의 <code>content</code> 텍스트를 반환한다.
     *
     * @param ps the style of the pseudo-element / 의사 요소의 스타일
     * @param e  the originating element (for <code>attr()</code>) / 기원 요소(<code>attr()</code> 용)
     * @return text, the empty string for <code>""</code>, or null if there is no content / 텍스트, <code>""</code> 이면 빈 문자열, 콘텐츠가 없으면 null
     */
    String text(ComputedStyle ps, Element e) {
        CssValue v = ps.get(Prop.CONTENT);
        if (v instanceof CssKeyword) {
            String k = ((CssKeyword) v).getName();
            return "none".equals(k) || "normal".equals(k) ? null : keywordText(k);
        }
        StringBuilder b = new StringBuilder();
        if (v instanceof CssList) {
            // 목록의 각 항목(문자열, attr(), 카운터, 따옴표 키워드)을 이어 붙인다.
            List<CssValue> items = ((CssList) v).getItems();
            for (int i = 0; i < items.size(); i++) {
                append(b, items.get(i), e);
            }
            return b.toString();
        }
        append(b, v, e);
        return b.toString();
    }

    /**
     * Appends the text of one item of a <code>content</code> value.
     * <p>
     * <code>content</code> 값의 항목 하나의 텍스트를 덧붙인다.
     *
     * @param b the output / 출력
     * @param v the item / 항목
     * @param e the originating element / 기원 요소
     */
    private void append(StringBuilder b, CssValue v, Element e) {
        if (v instanceof CssString) {
            b.append(((CssString) v).getValue());
        } else if (v instanceof CssKeyword) {
            String t = keywordText(((CssKeyword) v).getName());
            if (t != null) {
                b.append(t);
            }
        } else if (v instanceof CssFunctionValue) {
            CssFunctionValue f = (CssFunctionValue) v;
            List<CssValue> args = f.getArguments();
            if (f.isNamed("attr")) {
                if (!args.isEmpty() && args.get(0) instanceof CssKeyword) {
                    String a = e.getAttribute(((CssKeyword) args.get(0)).getName());
                    if (a != null) {
                        b.append(a);
                    }
                }
            } else if (f.isNamed("counter") && !args.isEmpty() && args.get(0) instanceof CssKeyword) {
                CssValue style = args.size() > 1 ? args.get(1) : null;
                Counter c = innermost(((CssKeyword) args.get(0)).getName());
                b.append(styles.number(style, c == null ? 0 : c.value));
            } else if (f.isNamed("counters") && args.size() >= 2 && args.get(0) instanceof CssKeyword && args.get(1) instanceof CssString) {
                String name = ((CssKeyword) args.get(0)).getName();
                String sep = ((CssString) args.get(1)).getValue();
                CssValue style = args.size() > 2 ? args.get(2) : null;
                boolean any = false;
                // 바깥에서 안쪽으로 같은 이름의 카운터 값을 구분자로 이어 붙인다.
                for (int i = 0; i < stack.size(); i++) {
                    if (name.equals(stack.get(i).name)) {
                        if (any) {
                            b.append(sep);
                        }
                        b.append(styles.number(style, stack.get(i).value));
                        any = true;
                    }
                }
                if (!any) {
                    b.append(styles.number(style, 0));
                }
            }
        }
    }

    /**
     * Returns the innermost counter of a name, or null.
     * <p>
     * 이름의 가장 안쪽 카운터를 반환한다. 없으면 null.
     *
     * @param name the name / 이름
     * @return the counter or null / 카운터 또는 null
     */
    private Counter innermost(String name) {
        // 안쪽(스택 뒤)부터 같은 이름을 찾는다.
        for (int i = stack.size() - 1; i >= 0; i--) {
            if (name.equals(stack.get(i).name)) {
                return stack.get(i);
            }
        }
        return null;
    }

    /**
     * Returns the text of a <code>content</code> keyword (quotes), or null.
     * <p>
     * <code>content</code> 키워드(따옴표)의 텍스트를 반환한다. 없으면 null.
     *
     * @param k the keyword / 키워드
     * @return text or null / 텍스트 또는 null
     */
    private static String keywordText(String k) {
        if ("open-quote".equals(k)) {
            return "“";
        }
        if ("close-quote".equals(k)) {
            return "”";
        }
        return null;
    }

    // ----------------------------------------------------------------------------------------------- first letter

    /**
     * Finds the part of a text that <code>::first-letter</code> styles (CSS Pseudo-Elements 4, 4.2): the punctuation before the first
     * letter, the first letter (a letter, a digit or a symbol, with the combining marks after it) and the punctuation after it, after the
     * leading white space. A text that has only white space or only punctuation has no first letter.
     * <p>
     * <code>::first-letter</code> 가 꾸미는 텍스트 부분을 찾는다(CSS Pseudo-Elements 4, 4.2): 앞선 공백을 지나 첫 글자 앞의 문장 부호, 첫 글자(글자, 숫자, 기호와 뒤따르는
     * 결합 부호), 뒤따르는 문장 부호. 공백이나 문장 부호만 있는 텍스트에는 첫 글자가 없다.
     *
     * @param t the text / 텍스트
     * @return {start, end, letterStart}: the range of the unit and the offset of the letter itself, or null / {시작, 끝, 글자 위치}: 단위의 범위와 글자 자신의 위치. 없으면 null
     */
    static int[] firstLetter(String t) {
        int n = t.length();
        int i = 0;
        // 앞선 공백을 지난다.
        while (i < n && (t.charAt(i) == ' ' || t.charAt(i) == '\t' || t.charAt(i) == '\n' || t.charAt(i) == '\r' || t.charAt(i) == '\f')) {
            i++;
        }
        int start = i;
        // 첫 글자 앞의 문장 부호를 지난다.
        while (i < n && isPunctuation(t.codePointAt(i))) {
            i += Character.charCount(t.codePointAt(i));
        }
        if (i >= n || Character.isWhitespace(t.codePointAt(i))) {
            return null;
        }
        int letter = i;
        i += Character.charCount(t.codePointAt(i));
        // 첫 글자에 붙은 결합 부호를 지난다.
        while (i < n && VerticalText.isMark(t.codePointAt(i))) {
            i += Character.charCount(t.codePointAt(i));
        }
        // 뒤따르는 문장 부호를 지난다.
        while (i < n && isPunctuation(t.codePointAt(i))) {
            i += Character.charCount(t.codePointAt(i));
        }
        return new int[] { start, i, letter };
    }

    /**
     * Tells whether a code point is punctuation in the sense of <code>::first-letter</code> (the Unicode classes Ps, Pe, Pi, Pf and Po).
     * <p>
     * 코드 포인트가 <code>::first-letter</code> 의 뜻의 문장 부호(유니코드 분류 Ps, Pe, Pi, Pf, Po)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for punctuation / 문장 부호이면 true
     */
    private static boolean isPunctuation(int cp) {
        int t = Character.getType(cp);
        return t == Character.START_PUNCTUATION || t == Character.END_PUNCTUATION || t == Character.INITIAL_QUOTE_PUNCTUATION
                || t == Character.FINAL_QUOTE_PUNCTUATION || t == Character.OTHER_PUNCTUATION;
    }
}
