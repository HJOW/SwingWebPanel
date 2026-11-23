package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.selector.MatchContext;
import org.duckdns.hjow.swingwebpanel.css.selector.NamespaceMap;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorList;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorMatcher;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorParser;
import org.duckdns.hjow.swingwebpanel.css.selector.SelectorSyntaxException;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The prelude of an <code>@scope</code> rule (CSS Cascading and Inheritance Level 6, section 2, "Scoping Styles"): <code>@scope (&lt;scope-start&gt;)
 * to (&lt;scope-end&gt;)</code>. The scoping roots are the elements that match the start selector (the root element if there is none); the scope of a
 * root is its descendants except the elements that match the end selector (<code>:scope</code> in the end selector is the root) and their
 * descendants. The rules inside the block only apply to the elements in a scope, and among rules of equal specificity the one whose scoping root is
 * nearer to the element wins (scoping proximity).
 * <p>
 * <code>@scope</code> 규칙의 전문(CSS Cascading and Inheritance Level 6, 2 절 "Scoping Styles"): <code>@scope (&lt;scope-start&gt;) to (&lt;scope-end&gt;)</code>.
 * 범위 루트는 시작 선택자와 일치하는 요소이고(없으면 루트 요소), 루트의 범위는 그 자손에서 끝 선택자와 일치하는 요소(끝 선택자의 <code>:scope</code> 는 루트이다)와 그 자손을
 * 뺀 것이다. 블록 안의 규칙은 범위 안의 요소에만 적용되며, 구체성이 같으면 범위 루트가 요소에 더 가까운 규칙이 이긴다(범위 근접도).
 */
public final class ScopeSpec implements MatchContext.ScopeRoot {

    /** The start selector, or null for the root element. / 시작 선택자. 루트 요소이면 null. */
    private final SelectorList start;

    /** The end selector, or null. / 끝 선택자. 없으면 null. */
    private final SelectorList end;

    /** The prelude text. / 전문 문자열. */
    private final String text;

    /** The text of the start selector list, or null. / 시작 선택자 목록의 문자열. 없으면 null. */
    private final String startText;

    /** The text of the end selector list, or null. / 끝 선택자 목록의 문자열. 없으면 null. */
    private final String endText;

    /**
     * Creates a scope.
     * <p>
     * 범위를 만든다.
     *
     * @param start the start selector or null / 시작 선택자 또는 null
     * @param end   the end selector or null / 끝 선택자 또는 null
     * @param text  the prelude text / 전문 문자열
     * @param startText the start selector text or null / 시작 선택자 문자열 또는 null
     * @param endText   the end selector text or null / 끝 선택자 문자열 또는 null
     */
    private ScopeSpec(SelectorList start, SelectorList end, String text, String startText, String endText) {
        this.start = start;
        this.end = end;
        this.text = text;
        this.startText = startText;
        this.endText = endText;
    }

    /**
     * Parses the prelude of an <code>@scope</code> rule: an optional parenthesised start selector list and an optional <code>to</code> with a
     * parenthesised end selector list.
     * <p>
     * <code>@scope</code> 규칙의 전문을 파싱한다: 괄호 안의 선택적 시작 선택자 목록과, <code>to</code> 와 괄호 안의 선택적 끝 선택자 목록.
     *
     * @param prelude the prelude values / 전문 값
     * @param ns      the namespace declarations or null / 네임스페이스 선언 또는 null
     * @return the scope / 범위
     * @throws SelectorSyntaxException if the prelude is invalid / 전문이 올바르지 않으면
     */
    public static ScopeSpec parse(List<ComponentValue> prelude, NamespaceMap ns) throws SelectorSyntaxException {
        List<ComponentValue> it = new ArrayList<ComponentValue>();
        // 공백이 아닌 항목만 모은다.
        for (int i = 0; i < prelude.size(); i++) {
            if (!CssParser.isWhitespace(prelude.get(i))) {
                it.add(prelude.get(i));
            }
        }
        SelectorList start = null;
        SelectorList end = null;
        String startText = null;
        String endText = null;
        int pos = 0;
        if (pos < it.size() && isParen(it.get(pos))) {
            start = SelectorParser.parse(((CssBlock) it.get(pos)).getContents(), ns);
            startText = CssBlock.join(((CssBlock) it.get(pos)).getContents()).trim();
            pos++;
        }
        if (pos < it.size() && it.get(pos) instanceof CssToken && ((CssToken) it.get(pos)).isIdent("to")) {
            pos++;
            if (pos >= it.size() || !isParen(it.get(pos))) {
                throw new SelectorSyntaxException("@scope to needs a parenthesised selector");
            }
            end = SelectorParser.parse(((CssBlock) it.get(pos)).getContents(), ns);
            endText = CssBlock.join(((CssBlock) it.get(pos)).getContents()).trim();
            pos++;
        }
        if (pos != it.size()) {
            throw new SelectorSyntaxException("unexpected tokens in the @scope prelude");
        }
        return new ScopeSpec(start, end, CssBlock.join(prelude).trim(), startText, endText);
    }

    /**
     * Tells whether a value is a parenthesised block.
     * <p>
     * 값이 괄호 블록인지 알려 준다.
     *
     * @param v the value / 값
     * @return true for a <code>( )</code> block / <code>( )</code> 블록이면 true
     */
    private static boolean isParen(ComponentValue v) {
        return v instanceof CssBlock && ((CssBlock) v).getOpenType() == CssToken.LEFT_PAREN;
    }

    /**
     * Tells whether an element is a scoping root: it matches the start selector, or it is the root element if the scope has none.
     * <p>
     * 요소가 범위 루트인지 알려 준다: 시작 선택자와 일치하거나, 시작 선택자가 없으면 루트 요소이다.
     *
     * @param e   the element / 요소
     * @param ctx the matching context (its element state is used) / 매칭 컨텍스트(요소 상태를 쓴다)
     * @return true for a root / 루트이면 true
     */
    public boolean isScope(Element e, MatchContext ctx) {
        if (start == null) {
            return e.getParentElement() == null;
        }
        MatchContext c = new MatchContext(null, ctx == null ? null : ctx.getState());
        return SelectorMatcher.matches(start, e, c);
    }

    /**
     * Tells whether an element ends the scope of a root: it matches the end selector (with <code>:scope</code> the root).
     * <p>
     * 요소가 루트의 범위를 끝내는지 알려 준다: 끝 선택자(<code>:scope</code> 는 루트)와 일치한다.
     *
     * @param x    the element / 요소
     * @param root the scoping root / 범위 루트
     * @param ctx  the matching context / 매칭 컨텍스트
     * @return true if the scope ends at the element / 범위가 그 요소에서 끝나면 true
     */
    private boolean isLimit(Element x, Element root, MatchContext ctx) {
        MatchContext c = new MatchContext(root, ctx == null ? null : ctx.getState());
        return SelectorMatcher.matches(end, x, c);
    }

    /**
     * Returns the number of generations between an element and the nearest scoping root whose scope contains the element (the scoping proximity
     * of CSS Cascade 6, section 2.4), or -1 if the element is in no scope.
     * <p>
     * 요소와, 그 요소를 범위에 포함하는 가장 가까운 범위 루트 사이의 세대 수(CSS Cascade 6, 2.4 절의 범위 근접도)를 반환한다. 요소가 어떤 범위에도 없으면 -1.
     *
     * @param e         the element / 요소
     * @param allowSelf true if the root itself counts as inside the scope (a selector that names <code>:scope</code>) / 루트 자신도 범위 안으로 보면 true(<code>:scope</code> 를 쓴 선택자)
     * @param ctx       the matching context / 매칭 컨텍스트
     * @return the distance, or -1 / 거리. 범위 밖이면 -1
     */
    public int distance(Element e, boolean allowSelf, MatchContext ctx) {
        Element root = findRoot(e, allowSelf, ctx);
        if (root == null) {
            return -1;
        }
        int hops = 0;
        // 요소에서 루트까지 올라간 세대 수를 센다.
        for (Element a = e; a != root; a = a.getParentElement()) {
            hops++;
        }
        return hops;
    }

    /**
     * Returns the nearest scoping root whose scope contains an element: an ancestor (or the element itself if <code>allowSelf</code>) that matches
     * the start selector, with no element that matches the end selector between it and the element.
     * <p>
     * 요소를 범위에 포함하는 가장 가까운 범위 루트를 반환한다: 시작 선택자와 일치하는 조상(<code>allowSelf</code> 이면 요소 자신도)이며, 그것과 요소 사이에 끝 선택자와
     * 일치하는 요소가 없다.
     *
     * @param e         the element / 요소
     * @param allowSelf true if the element itself can be the root / 요소 자신이 루트일 수 있으면 true
     * @param ctx       the matching context / 매칭 컨텍스트
     * @return the root or null if the element is in no scope / 루트. 범위 밖이면 null
     */
    public Element findRoot(Element e, boolean allowSelf, MatchContext ctx) {
        // 요소에서 위로 올라가며 범위 루트를 찾는다(가까운 루트가 먼저이다).
        for (Element a = allowSelf ? e : e.getParentElement(); a != null; a = a.getParentElement()) {
            if (!isScope(a, ctx)) {
                continue;
            }
            boolean blocked = false;
            // 루트와 요소 사이(요소 포함, 루트 제외)에 끝 선택자와 일치하는 요소가 있으면 이 루트의 범위가 아니다.
            for (Element x = e; end != null && x != a && !blocked; x = x.getParentElement()) {
                blocked = isLimit(x, a, ctx);
            }
            if (!blocked) {
                return a;
            }
        }
        return null;
    }

    /**
     * Returns the prelude text.
     * <p>
     * 전문 문자열을 반환한다.
     *
     * @return the text / 문자열
     */
    public String toCssText() {
        return text;
    }

    /**
     * Returns the text of the start selector list (the CSSOM <code>start</code> of <code>CSSScopeRule</code>).
     * <p>
     * 시작 선택자 목록의 문자열을 반환한다(<code>CSSScopeRule</code> 의 CSSOM <code>start</code>).
     *
     * @return the selector text, or null if the scope has no start selector / 선택자 문자열. 시작 선택자가 없으면 null
     */
    public String getStartText() {
        return startText;
    }

    /**
     * Returns the text of the end selector list (the CSSOM <code>end</code> of <code>CSSScopeRule</code>).
     * <p>
     * 끝 선택자 목록의 문자열을 반환한다(<code>CSSScopeRule</code> 의 CSSOM <code>end</code>).
     *
     * @return the selector text, or null if the scope has no end selector / 선택자 문자열. 끝 선택자가 없으면 null
     */
    public String getEndText() {
        return endText;
    }
}
