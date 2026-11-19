package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;

/**
 * A <code>@page</code> rule (CSS Paged Media Level 3, <a href="https://www.w3.org/TR/css-page-3/">https://www.w3.org/TR/css-page-3/</a>, section 4,
 * W3C Working Draft 14 September 2023; CSSOM <code>CSSPageRule</code>, legacy type 6): a list of page selectors (an optional page type name followed by
 * the page pseudo-classes <code>:left</code>, <code>:right</code>, <code>:first</code>, <code>:blank</code>; an empty selector list matches every page),
 * a block of page descriptors (<code>size</code>, <code>margin</code>, <code>marks</code>, <code>bleed</code>, <code>page-orientation</code> ...) and the
 * page-margin rules inside it ({@link MarginRule}). The engine does not print, so the rule is parsed, kept and exposed through the CSSOM, and its
 * selectors can be tested against a page ({@link #matches}); the page boxes are never laid out.
 * <p>
 * <code>@page</code> 규칙(CSS Paged Media Level 3, <a href="https://www.w3.org/TR/css-page-3/">https://www.w3.org/TR/css-page-3/</a>, 4 절,
 * W3C 작업 초안 2023-09-14, CSSOM <code>CSSPageRule</code>, 옛 종류 6): 쪽 선택자 목록(선택적인 쪽 이름 뒤에 쪽 의사 클래스 <code>:left</code>, <code>:right</code>,
 * <code>:first</code>, <code>:blank</code>. 선택자 목록이 비면 모든 쪽에 일치), 쪽 서술자 블록(<code>size</code>, <code>margin</code>, <code>marks</code>,
 * <code>bleed</code>, <code>page-orientation</code> 등), 그 안의 쪽 여백 규칙({@link MarginRule}). 엔진은 인쇄하지 않으므로 규칙은 파싱해 보관하고 CSSOM 으로
 * 보여 주며 선택자를 쪽에 대해 시험할 수 있다({@link #matches}). 쪽 상자는 배치하지 않는다.
 */
public final class PageRule extends CssRule {

    /** The names of the sixteen page-margin at-rules. / 열여섯 쪽 여백 at-규칙의 이름. */
    private static final String[] MARGIN_NAMES = {"top-left-corner", "top-left", "top-center", "top-right", "top-right-corner", "bottom-left-corner",
        "bottom-left", "bottom-center", "bottom-right", "bottom-right-corner", "left-top", "left-middle", "left-bottom", "right-top", "right-middle",
        "right-bottom"};

    /**
     * One page selector: an optional page type name and any of the page pseudo-classes.
     * <p>
     * 쪽 선택자 하나: 선택적인 쪽 이름과 쪽 의사 클래스.
     */
    public static final class Selector {

        /** The page type name (case-sensitive), or null. / 쪽 이름(대소문자 구분). 없으면 null. */
        private final String name;

        /** True for <code>:left</code>. / <code>:left</code> 이면 true. */
        private final boolean left;

        /** True for <code>:right</code>. / <code>:right</code> 이면 true. */
        private final boolean right;

        /** True for <code>:first</code>. / <code>:first</code> 이면 true. */
        private final boolean first;

        /** True for <code>:blank</code>. / <code>:blank</code> 이면 true. */
        private final boolean blank;

        /** The number of <code>:first</code> and <code>:blank</code> pseudo-classes (repeats count). / <code>:first</code> 와 <code>:blank</code> 의 개수(반복도 센다). */
        private final int firstBlank;

        /** The number of <code>:left</code> and <code>:right</code> pseudo-classes (repeats count). / <code>:left</code> 와 <code>:right</code> 의 개수(반복도 센다). */
        private final int leftRight;

        /** The text in the order that it was written, with the pseudo-classes in lower case. / 쓰인 순서의 문자열(의사 클래스는 소문자). */
        private final String text;

        /**
         * Creates a selector.
         * <p>
         * 선택자를 만든다.
         *
         * @param name  the page type name or null / 쪽 이름 또는 null
         * @param left  <code>:left</code> / <code>:left</code>
         * @param right <code>:right</code> / <code>:right</code>
         * @param first <code>:first</code> / <code>:first</code>
         * @param blank <code>:blank</code> / <code>:blank</code>
         * @param firstBlank the number of :first and :blank (repeats count) / :first 와 :blank 의 개수(반복도 센다)
         * @param leftRight  the number of :left and :right (repeats count) / :left 와 :right 의 개수(반복도 센다)
         * @param text  the serialization / 직렬화 문자열
         */
        Selector(String name, boolean left, boolean right, boolean first, boolean blank, int firstBlank, int leftRight, String text) {
            this.firstBlank = firstBlank;
            this.leftRight = leftRight;
            this.name = name;
            this.left = left;
            this.right = right;
            this.first = first;
            this.blank = blank;
            this.text = text;
        }

        /**
         * Tells whether the selector matches a page (all of its components must match). A page type name <code>auto</code> never matches.
         * <p>
         * 선택자가 쪽에 일치하는지 알려 준다(모든 구성 요소가 일치해야 한다). 쪽 이름 <code>auto</code> 는 절대 일치하지 않는다.
         *
         * @param pageName the named page of the page, or null/empty for the unnamed page / 쪽의 이름. 이름 없는 쪽이면 null 이나 빈 문자열
         * @param number   the page number counted from 1 / 1 부터 센 쪽 번호
         * @param isBlank  true if the page is a blank page that a forced break made / 강제 쪽 나눔이 만든 빈 쪽이면 true
         * @param isRight  true for a right page, false for a left page / 오른쪽 쪽이면 true, 왼쪽 쪽이면 false
         * @return true if it matches / 일치하면 true
         */
        public boolean matches(String pageName, int number, boolean isBlank, boolean isRight) {
            // 쪽 이름은 대소문자를 구분하며 auto 는 절대 일치하지 않는다.
            if (name != null && (name.equalsIgnoreCase("auto") || !name.equals(pageName))) {
                return false;
            }
            return !(left && isRight) && !(right && !isRight) && !(first && number != 1) && !(blank && !isBlank);
        }

        /**
         * Returns the specificity packed in an integer: the page type name counts as the highest (a), <code>:first</code> and <code>:blank</code> as
         * the middle (b) and <code>:left</code> and <code>:right</code> as the lowest (c) component (section 4.2 of the specification).
         * <p>
         * 정수에 묶은 구체성을 반환한다: 쪽 이름이 가장 높은(a), <code>:first</code> 와 <code>:blank</code> 가 중간(b), <code>:left</code> 와 <code>:right</code> 가 가장
         * 낮은(c) 요소이다(규격 4.2 절).
         *
         * @return the specificity / 구체성
         */
        public int getSpecificity() {
            return ((name != null ? 1 : 0) << 16) | (Math.min(firstBlank, 255) << 8) | Math.min(leftRight, 255);
        }

        /**
         * Serializes the selector: the name followed by the pseudo-classes in the order in which they were written.
         * <p>
         * 선택자를 직렬화한다: 이름 뒤에 의사 클래스를 쓰인 순서대로 쓴다.
         *
         * @return the text / 문자열
         */
        public String toCssText() {
            return text;
        }
    }

    /** The parsed selectors; empty means every page. / 파싱한 선택자. 비어 있으면 모든 쪽. */
    private List<Selector> selectors;

    /** The page descriptors. / 쪽 서술자. */
    private final CssStyleDeclaration style;

    /** The page-margin rules inside the rule. / 규칙 안의 쪽 여백 규칙. */
    private final List<CssRule> margins;

    /** The read-only view that {@link #getRules()} hands out. / {@link #getRules()} 가 내주는 읽기 전용 뷰. */
    private final List<CssRule> marginsView;

    /** The rule list object of the CSSOM, created when it is first asked for. / CSSOM 의 규칙 목록 객체. 처음 요청할 때 만든다. */
    private CssRuleList ruleList;

    /**
     * Creates a rule.
     * <p>
     * 규칙을 만든다.
     *
     * @param selectors the parsed selectors / 파싱한 선택자
     * @param style     the page descriptors / 쪽 서술자
     * @param margins   the margin rules / 여백 규칙
     */
    private PageRule(List<Selector> selectors, CssStyleDeclaration style, List<MarginRule> margins) {
        this.selectors = selectors;
        this.style = style;
        this.margins = new ArrayList<CssRule>(margins);
        this.marginsView = Collections.unmodifiableList(this.margins);
        style.setParentRule(this);
    }

    /**
     * Creates a rule from its prelude, or returns null if the page selector list is invalid (the whole rule is then dropped).
     * <p>
     * 전문으로 규칙을 만든다. 쪽 선택자 목록이 올바르지 않으면 null(그 규칙 전체를 버린다).
     *
     * @param prelude the prelude component values / 전문 컴포넌트 값
     * @param style   the page descriptors / 쪽 서술자
     * @param margins the margin rules / 여백 규칙
     * @return the rule or null / 규칙. 올바르지 않으면 null
     */
    public static PageRule create(List<ComponentValue> prelude, CssStyleDeclaration style, List<MarginRule> margins) {
        List<Selector> sel = parseSelectors(prelude);
        return sel == null ? null : new PageRule(sel, style, margins);
    }

    /**
     * Tells whether a name is one of the sixteen page-margin at-rules.
     * <p>
     * 이름이 열여섯 쪽 여백 at-규칙 중 하나인지 알려 준다.
     *
     * @param name the at-rule name in lower case / 소문자 at-규칙 이름
     * @return true for a margin rule name / 여백 규칙 이름이면 true
     */
    public static boolean isMarginName(String name) {
        // 이름 목록과 하나씩 비교한다.
        for (int i = 0; i < MARGIN_NAMES.length; i++) {
            if (MARGIN_NAMES[i].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses a page selector list: <code>&lt;page-selector&gt;#</code> where a page selector is an optional identifier followed by pseudo-pages
     * with no whitespace between the components; an empty prelude is the empty list.
     * <p>
     * 쪽 선택자 목록을 파싱한다: <code>&lt;page-selector&gt;#</code>. 쪽 선택자는 선택적 식별자와 뒤따르는 쪽 의사 클래스이며 구성 요소 사이에 공백이 없다. 전문이 비면 빈 목록.
     *
     * @param prelude the component values / 컴포넌트 값
     * @return the selectors, or null if the list is invalid / 선택자. 올바르지 않으면 null
     */
    static List<Selector> parseSelectors(List<ComponentValue> prelude) {
        List<Selector> out = new ArrayList<Selector>();
        List<ComponentValue> all = CssParser.trim(prelude);
        if (all.isEmpty()) {
            return out;
        }
        List<List<ComponentValue>> parts = CssParser.splitCommas(all);
        // 쉼표로 나눈 조각이 각각 올바른 쪽 선택자여야 한다.
        for (int i = 0; i < parts.size(); i++) {
            Selector s = parseOne(parts.get(i));
            if (s == null) {
                return null;
            }
            out.add(s);
        }
        return out;
    }

    /**
     * Parses one page selector.
     * <p>
     * 쪽 선택자 하나를 파싱한다.
     *
     * @param part the component values without the surrounding whitespace / 앞뒤 공백이 없는 컴포넌트 값
     * @return the selector or null if invalid / 선택자. 올바르지 않으면 null
     */
    private static Selector parseOne(List<ComponentValue> part) {
        int p = 0;
        String name = null;
        boolean left = false;
        boolean right = false;
        boolean first = false;
        boolean blank = false;
        int firstBlank = 0;
        int leftRight = 0;
        StringBuilder text = new StringBuilder();
        if (part.isEmpty()) {
            return null;
        }
        if (part.get(0) instanceof CssToken && ((CssToken) part.get(0)).is(CssToken.IDENT)) {
            name = ((CssToken) part.get(0)).getValue();
            text.append(name);
            p = 1;
        }
        // 이름 뒤에는 공백 없이 ':' 와 의사 클래스 이름이 쌍으로 이어져야 한다.
        while (p < part.size()) {
            if (p + 1 >= part.size() || !(part.get(p) instanceof CssToken) || !((CssToken) part.get(p)).is(CssToken.COLON)
                    || !(part.get(p + 1) instanceof CssToken) || !((CssToken) part.get(p + 1)).is(CssToken.IDENT)) {
                return null;
            }
            String pseudo = ((CssToken) part.get(p + 1)).getValue().toLowerCase();
            if (pseudo.equals("left")) {
                left = true;
                leftRight++;
            } else if (pseudo.equals("right")) {
                right = true;
                leftRight++;
            } else if (pseudo.equals("first")) {
                first = true;
                firstBlank++;
            } else if (pseudo.equals("blank")) {
                blank = true;
                firstBlank++;
            } else {
                return null;
            }
            text.append(':').append(pseudo);
            p += 2;
        }
        return new Selector(name, left, right, first, blank, firstBlank, leftRight, text.toString());
    }

    /**
     * Returns the selectors.
     * <p>
     * 선택자를 반환한다.
     *
     * @return the selectors; an empty list matches every page / 선택자. 비어 있으면 모든 쪽에 일치
     */
    public List<Selector> getSelectors() {
        return Collections.unmodifiableList(selectors);
    }

    /**
     * Returns the CSSOM <code>selectorText</code>: the selectors separated by a comma and a space.
     * <p>
     * CSSOM <code>selectorText</code> 를 반환한다: 쉼표와 공백으로 구분한 선택자.
     *
     * @return the text; empty if the rule matches every page / 문자열. 모든 쪽에 일치하면 빈 문자열
     */
    public String getSelectorText() {
        StringBuilder sb = new StringBuilder();
        // 선택자를 쉼표로 이어 쓴다.
        for (int i = 0; i < selectors.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(selectors.get(i).toCssText());
        }
        return sb.toString();
    }

    /**
     * Replaces the selectors by the parsed text (the CSSOM <code>selectorText</code> setter); text that is not a valid page selector list is ignored.
     * <p>
     * 선택자를 파싱한 텍스트로 바꾼다(CSSOM <code>selectorText</code> 설정). 올바른 쪽 선택자 목록이 아닌 텍스트는 무시한다.
     *
     * @param text the new selector list text / 새 선택자 목록 문자열
     */
    public void setSelectorText(String text) {
        List<Selector> parsed = parseSelectors(CssParser.parseComponentValues(text == null ? "" : text));
        // 올바르지 않으면 규칙을 그대로 둔다.
        if (parsed == null) {
            return;
        }
        this.selectors = parsed;
        StyleSheet sheet = getParentStyleSheet();
        if (sheet != null) {
            sheet.markModified();
        }
        changed();
    }

    /**
     * Tells whether the rule applies to a page: its selector list is empty or one of the selectors matches.
     * <p>
     * 규칙이 쪽에 적용되는지 알려 준다: 선택자 목록이 비었거나 선택자 하나가 일치한다.
     *
     * @param pageName the named page, or null/empty for the unnamed page / 쪽의 이름. 이름 없는 쪽이면 null 이나 빈 문자열
     * @param number   the page number counted from 1 / 1 부터 센 쪽 번호
     * @param blank    true for a blank page / 빈 쪽이면 true
     * @param right    true for a right page / 오른쪽 쪽이면 true
     * @return true if the rule applies / 적용되면 true
     */
    public boolean matches(String pageName, int number, boolean blank, boolean right) {
        return specificity(pageName, number, blank, right) >= 0;
    }

    /**
     * Returns the specificity of the best matching selector for a page, or -1 if the rule does not apply (an empty selector list has the
     * specificity 0).
     * <p>
     * 쪽에 대해 일치하는 선택자 중 가장 높은 구체성을 반환한다. 규칙이 적용되지 않으면 -1(선택자 목록이 비면 구체성 0).
     *
     * @param pageName the named page, or null/empty for the unnamed page / 쪽의 이름. 이름 없는 쪽이면 null 이나 빈 문자열
     * @param number   the page number counted from 1 / 1 부터 센 쪽 번호
     * @param blank    true for a blank page / 빈 쪽이면 true
     * @param right    true for a right page / 오른쪽 쪽이면 true
     * @return the specificity or -1 / 구체성 또는 -1
     */
    public int specificity(String pageName, int number, boolean blank, boolean right) {
        if (selectors.isEmpty()) {
            return 0;
        }
        int best = -1;
        // 일치하는 선택자 중 가장 구체적인 것을 고른다.
        for (int i = 0; i < selectors.size(); i++) {
            Selector s = selectors.get(i);
            if (s.matches(pageName, number, blank, right)) {
                best = Math.max(best, s.getSpecificity());
            }
        }
        return best;
    }

    /**
     * Returns the page descriptors (CSSOM <code>style</code>).
     * <p>
     * 쪽 서술자를 반환한다(CSSOM <code>style</code>).
     *
     * @return the declarations / 선언
     */
    public CssStyleDeclaration getStyle() {
        return style;
    }

    /**
     * Returns the page-margin rules of the rule. The list is a read-only view.
     * <p>
     * 규칙의 쪽 여백 규칙을 반환한다. 목록은 읽기 전용 뷰이다.
     *
     * @return the margin rules / 여백 규칙
     */
    public List<CssRule> getRules() {
        return marginsView;
    }

    /**
     * Returns the rule list object of the CSSOM, <code>cssRules</code> (the same object every time).
     * <p>
     * CSSOM 의 규칙 목록 객체 <code>cssRules</code> 를 반환한다(매번 같은 객체).
     *
     * @return the rule list / 규칙 목록
     */
    public CssRuleList getCssRules() {
        if (ruleList == null) {
            ruleList = new CssRuleList(marginsView);
        }
        return ruleList;
    }

    /**
     * Returns the rule type.
     * <p>
     * 규칙 종류를 반환한다.
     *
     * @return {@link #PAGE_RULE} / PAGE_RULE
     */
    public int getType() {
        return PAGE_RULE;
    }

    /**
     * Serializes the rule.
     * <p>
     * 규칙을 직렬화한다.
     *
     * @return CSS text / CSS 문자열
     */
    public String getCssText() {
        StringBuilder sb = new StringBuilder("@page");
        String sel = getSelectorText();
        if (sel.length() > 0) {
            sb.append(' ').append(sel);
        }
        sb.append(" {");
        String decls = style.getCssText();
        if (decls.length() > 0) {
            sb.append(' ').append(decls);
        }
        // 여백 규칙을 차례로 이어 쓴다.
        for (int i = 0; i < margins.size(); i++) {
            sb.append(' ').append(margins.get(i).getCssText());
        }
        return sb.append(" }").toString();
    }

    /**
     * Puts the rule and its margin rules into a sheet: a change of a declaration block reaches the sheet.
     * <p>
     * 규칙과 그 여백 규칙을 시트에 넣는다: 선언 블록의 변경이 시트에 닿는다.
     *
     * @param sheet  the sheet / 시트
     * @param parent the containing rule or null / 담은 규칙 또는 null
     */
    void attach(StyleSheet sheet, CssRule parent) {
        super.attach(sheet, parent);
        style.setBaseUrl(sheet == null ? null : sheet.getUrl());
        style.setChangeListener(new Runnable() {
            /**
             * Tells the sheet that the declarations changed.
             * <p>
             * 선언이 바뀌었다고 시트에 알린다.
             */
            public void run() {
                StyleSheet s = getParentStyleSheet();
                if (s != null) {
                    s.markModified();
                }
                changed();
            }
        });
        // 여백 규칙마다 이 규칙을 부모로 하여 같은 시트에 붙인다.
        for (int i = 0; i < margins.size(); i++) {
            margins.get(i).attach(sheet, this);
        }
    }

    /**
     * Takes the rule and its margin rules out of their sheet.
     * <p>
     * 규칙과 그 여백 규칙을 시트에서 뺀다.
     */
    void detach() {
        super.detach();
        style.setChangeListener(null);
        // 여백 규칙도 시트에서 뗀다.
        for (int i = 0; i < margins.size(); i++) {
            margins.get(i).detach();
        }
    }
}
