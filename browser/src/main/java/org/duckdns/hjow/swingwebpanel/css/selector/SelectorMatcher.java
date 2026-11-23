package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The selector matcher: decides whether an element matches a selector. Complex selectors are matched right to left (the subject first, then
 * the combinators walk towards the ancestors and previous siblings) with backtracking for the descendant and subsequent-sibling combinators.
 * The HTML case rules are applied: type and attribute names are case-insensitive for HTML elements of HTML documents, a fixed list of attributes
 * has case-insensitive values, and ids and classes are case-insensitive in quirks mode. The matcher only reads the DOM and keeps no state, so it
 * can be called from any thread that owns the document.
 * <p>
 * 선택자 매처: 요소가 선택자와 일치하는지 판정한다. 복합 선택자는 오른쪽에서 왼쪽으로(대상이 먼저, 이어서 결합자가 조상과 이전 형제 쪽으로) 맞추며
 * 자손 결합자와 이후 형제 결합자에는 역추적이 있다. HTML 대소문자 규칙을 적용한다: HTML 문서의 HTML 요소에서는 타입과 속성 이름이 대소문자를 구분하지
 * 않고, 정해진 속성 목록은 값도 구분하지 않으며, 호환 모드에서는 id 와 클래스도 구분하지 않는다. 매처는 DOM 을 읽기만 하고 상태가 없으므로 문서를
 * 소유한 어느 스레드에서든 호출할 수 있다.
 */
public final class SelectorMatcher {

    /** Attributes whose values are ASCII case-insensitive on HTML elements of HTML documents. / HTML 문서의 HTML 요소에서 값이 ASCII 대소문자를 구분하지 않는 속성. */
    private static final Set<String> CASE_INSENSITIVE_VALUES = new HashSet<String>();

    static {
        String[] names = {"accept", "accept-charset", "align", "alink", "axis", "bgcolor", "charset", "checked", "clear", "codetype", "color",
            "compact", "declare", "defer", "dir", "direction", "disabled", "enctype", "face", "frame", "hreflang", "http-equiv", "lang",
            "language", "link", "media", "method", "multiple", "nohref", "noresize", "noshade", "nowrap", "readonly", "rel", "rev", "rules",
            "scope", "scrolling", "selected", "shape", "target", "text", "type", "valign", "valuetype", "vlink"};
        // 값의 대소문자를 구분하지 않는 속성 이름을 집합에 넣는다.
        for (int i = 0; i < names.length; i++) {
            CASE_INSENSITIVE_VALUES.add(names[i]);
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SelectorMatcher() {
    }

    /**
     * Tells whether an element matches any selector of a list.
     * <p>
     * 요소가 목록의 선택자 중 하나와 일치하는지 알려 준다.
     *
     * @param list the selector list / 선택자 목록
     * @param e    the element / 요소
     * @param ctx  matching context / 매칭 컨텍스트
     * @return true if any selector matches / 하나라도 일치하면 true
     */
    public static boolean matches(SelectorList list, Element e, MatchContext ctx) {
        List<ComplexSelector> sels = list.getSelectors();
        // 목록의 선택자를 차례로 시도해 하나라도 일치하면 끝낸다.
        for (int i = 0; i < sels.size(); i++) {
            if (matches(sels.get(i), e, ctx)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches the argument list of a logical pseudo-class: the pseudo-element of the outer match does not apply to nested selectors.
     * <p>
     * 논리 의사 클래스의 인수 목록을 맞춘다: 바깥 매칭의 의사 요소는 중첩된 선택자에 적용되지 않는다.
     *
     * @param list the nested selector list / 중첩된 선택자 목록
     * @param e    the element / 요소
     * @param ctx  matching context / 매칭 컨텍스트
     * @return true if any selector matches / 하나라도 일치하면 true
     */
    private static boolean matchesNested(SelectorList list, Element e, MatchContext ctx) {
        List<ComplexSelector> sels = list.getSelectors();
        ctx.enterNested();
        try {
            // 중첩 목록의 선택자를 차례로 시도한다.
            for (int i = 0; i < sels.size(); i++) {
                ComplexSelector s = sels.get(i);
                if (matchChain(s, s.getCompounds().size() - 1, e, null, ctx)) {
                    return true;
                }
            }
            return false;
        } finally {
            ctx.leaveNested();
        }
    }

    /**
     * Returns the most specific selector of a list that matches an element.
     * <p>
     * 목록에서 요소와 일치하는 가장 구체적인 선택자를 반환한다.
     *
     * @param list the selector list / 선택자 목록
     * @param e    the element / 요소
     * @param ctx  matching context / 매칭 컨텍스트
     * @return the specificity of the best match, or -1 if nothing matches / 가장 잘 맞는 선택자의 구체성. 일치하는 것이 없으면 -1
     */
    public static long bestSpecificity(SelectorList list, Element e, MatchContext ctx) {
        long best = -1;
        List<ComplexSelector> sels = list.getSelectors();
        // 일치하는 선택자 중 구체성이 가장 큰 것을 고른다.
        for (int i = 0; i < sels.size(); i++) {
            ComplexSelector s = sels.get(i);
            if (s.getSpecificity() > best && matches(s, e, ctx)) {
                best = s.getSpecificity();
            }
        }
        return best;
    }

    /**
     * Tells whether an element matches a complex selector (relative selectors are not matched this way).
     * <p>
     * 요소가 복합 선택자와 일치하는지 알려 준다(상대 선택자는 이 방식으로 일치시키지 않는다).
     *
     * @param s   the selector / 선택자
     * @param e   the element / 요소
     * @param ctx matching context / 매칭 컨텍스트
     * @return true if it matches / 일치하면 true
     */
    public static boolean matches(ComplexSelector s, Element e, MatchContext ctx) {
        String pe = s.getPseudoElement();
        String want = ctx.getPseudoElement();
        // 의사 요소가 같아야 한다(둘 다 없거나 같은 이름).
        if (pe == null ? want != null : !pe.equals(want)) {
            return false;
        }
        return matchChain(s, s.getCompounds().size() - 1, e, null, ctx);
    }

    /**
     * Matches the compound at an index and, recursively, the ones to its left.
     * <p>
     * 위치의 복합 선택자와 그 왼쪽을 재귀적으로 맞춘다.
     *
     * @param s      the complex selector / 복합 선택자
     * @param idx    index of the compound to match against <code>e</code> / <code>e</code> 와 맞출 복합 선택자의 위치
     * @param e      the element / 요소
     * @param anchor the anchor element of a relative selector, or null / 상대 선택자의 기준 요소. 아니면 null
     * @param ctx    matching context / 매칭 컨텍스트
     * @return true if the chain matches / 연쇄가 일치하면 true
     */
    private static boolean matchChain(ComplexSelector s, int idx, Element e, Element anchor, MatchContext ctx) {
        if (!ctx.tick()) {
            return false;
        }
        if (!matchCompound(s.getCompounds().get(idx), e, ctx)) {
            return false;
        }
        if (idx == 0) {
            return anchor == null || relatedToAnchor(s.getLeadingCombinator(), e, anchor);
        }
        char comb = s.getCombinator(idx - 1);
        switch (comb) {
        case ComplexSelector.CHILD: {
            Element p = e.getParentElement();
            return p != null && matchChain(s, idx - 1, p, anchor, ctx);
        }
        case ComplexSelector.DESCENDANT: {
            // 조상을 위로 올라가며 왼쪽 연쇄가 일치하는 조상을 찾는다.
            for (Element p = e.getParentElement(); p != null; p = p.getParentElement()) {
                if (matchChain(s, idx - 1, p, anchor, ctx)) {
                    return true;
                }
                if (ctx.isExhausted()) {
                    return false;
                }
            }
            return false;
        }
        case ComplexSelector.NEXT_SIBLING: {
            Element p = e.getPreviousElementSibling();
            return p != null && matchChain(s, idx - 1, p, anchor, ctx);
        }
        default: {
            // 이전 형제를 거슬러 올라가며 왼쪽 연쇄가 일치하는 형제를 찾는다.
            for (Element p = e.getPreviousElementSibling(); p != null; p = p.getPreviousElementSibling()) {
                if (matchChain(s, idx - 1, p, anchor, ctx)) {
                    return true;
                }
                if (ctx.isExhausted()) {
                    return false;
                }
            }
            return false;
        }
        }
    }

    /**
     * Checks the relation between the leftmost matched element of a relative selector and its anchor.
     * <p>
     * 상대 선택자에서 가장 왼쪽에 일치한 요소와 기준 요소의 관계를 확인한다.
     *
     * @param leading leading combinator / 맨 앞 결합자
     * @param x       the leftmost matched element / 가장 왼쪽에 일치한 요소
     * @param anchor  the anchor element / 기준 요소
     * @return true if the relation holds / 관계가 성립하면 true
     */
    private static boolean relatedToAnchor(char leading, Element x, Element anchor) {
        switch (leading) {
        case ComplexSelector.CHILD:
            return x.getParentNode() == anchor;
        case ComplexSelector.NEXT_SIBLING:
            return x.getPreviousElementSibling() == anchor;
        case ComplexSelector.LATER_SIBLING: {
            // x 의 앞쪽 형제 중에 기준 요소가 있는지 본다.
            for (Element p = x.getPreviousElementSibling(); p != null; p = p.getPreviousElementSibling()) {
                if (p == anchor) {
                    return true;
                }
            }
            return false;
        }
        default:
            return x != anchor && anchor.contains(x);
        }
    }

    /**
     * Matches a relative selector list against an anchor element (the semantics of <code>:has()</code>).
     * <p>
     * 상대 선택자 목록을 기준 요소에 대해 맞춘다(<code>:has()</code> 의 의미).
     *
     * @param list   relative selector list / 상대 선택자 목록
     * @param anchor the element <code>:has()</code> is applied to / <code>:has()</code> 가 적용되는 요소
     * @param ctx    matching context / 매칭 컨텍스트
     * @return true if some selector matches an element related to the anchor / 기준 요소와 관계 있는 요소에 일치하는 선택자가 있으면 true
     */
    private static boolean matchesRelative(SelectorList list, Element anchor, MatchContext ctx) {
        ctx.enterNested();
        try {
            return matchesRelativeSelectors(list, anchor, ctx);
        } finally {
            ctx.leaveNested();
        }
    }

    /**
     * Tries the relative selectors of the argument of <code>:has()</code> (the body of {@link #matchesRelative}, which marks the match
     * as nested).
     * <p>
     * <code>:has()</code> 인수의 상대 선택자를 시도한다({@link #matchesRelative} 의 본문이며 그쪽이 매칭을 중첩으로 표시한다).
     *
     * @param list   the relative selectors / 상대 선택자
     * @param anchor the anchor element / 기준 요소
     * @param ctx    matching context / 매칭 컨텍스트
     * @return true if some selector matches an element related to the anchor / 기준 요소와 관계 있는 요소에 일치하는 선택자가 있으면 true
     */
    private static boolean matchesRelativeSelectors(SelectorList list, Element anchor, MatchContext ctx) {
        List<ComplexSelector> sels = list.getSelectors();
        // 상대 선택자마다 후보 요소를 훑어 일치하는 것이 있는지 찾는다.
        for (int i = 0; i < sels.size(); i++) {
            ComplexSelector s = sels.get(i);
            char lead = s.getLeadingCombinator();
            boolean siblings = lead == ComplexSelector.NEXT_SIBLING || lead == ComplexSelector.LATER_SIBLING;
            int last = s.getCompounds().size() - 1;
            Node start = siblings ? anchor.getNextSibling() : anchor.getFirstChild();
            Node limit = siblings ? anchor.getParentNode() : anchor;
            // 후보 요소를 트리 순서로 훑는다(형제 관계이면 뒤따르는 형제와 그 자손, 아니면 자손).
            for (Node n = start; n != null; n = Node.nextInTree(n, limit)) {
                if (n instanceof Element && matchChain(s, last, (Element) n, anchor, ctx)) {
                    return true;
                }
                if (ctx.isExhausted()) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Matches one compound selector against an element.
     * <p>
     * 복합 선택자 하나를 요소에 맞춘다.
     *
     * @param c   the compound selector / 복합 선택자
     * @param e   the element / 요소
     * @param ctx matching context / 매칭 컨텍스트
     * @return true if every simple selector matches / 모든 단순 선택자가 일치하면 true
     */
    private static boolean matchCompound(CompoundSelector c, Element e, MatchContext ctx) {
        List<SimpleSelector> simples = c.getSimples();
        // 호환 모드의 :active/:hover quirk: 이 둘뿐인 복합 선택자는 링크가 아닌 요소와 일치하지 않는다(호환 모드 표준 4.1).
        if (!ctx.isNested() && ctx.getPseudoElement() == null && inQuirksMode(e) && onlyHoverOrActive(simples) && !HtmlStates.isLink(e)) {
            return false;
        }
        // 단순 선택자가 모두 일치해야 한다.
        for (int i = 0; i < simples.size(); i++) {
            if (!matchSimple(simples.get(i), e, ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether a compound selector uses <code>:hover</code> or <code>:active</code> and nothing that narrows the match otherwise:
     * no type, id, class, attribute or other pseudo-class selector (the universal selector does not count). The Quirks Mode Standard
     * section 4.1 makes such selectors match links only in quirks mode.
     * <p>
     * 복합 선택자가 <code>:hover</code> 나 <code>:active</code> 를 쓰고 그 밖에 일치를 좁히는 것(타입, id, 클래스, 속성, 다른 의사 클래스 선택자)이 없는지
     * 알려 준다(전체 선택자는 세지 않는다). 호환 모드 표준 4.1절은 이런 선택자가 호환 모드에서 링크에만 일치하게 한다.
     *
     * @param simples the simple selectors of the compound / 복합 선택자의 단순 선택자
     * @return true if the quirk applies to the compound / quirk 가 적용되는 복합 선택자이면 true
     */
    private static boolean onlyHoverOrActive(List<SimpleSelector> simples) {
        boolean found = false;
        // 단순 선택자를 하나씩 보며 hover/active 이외의 좁히는 선택자가 있는지 확인한다.
        for (int i = 0; i < simples.size(); i++) {
            SimpleSelector s = simples.get(i);
            if (s.getKind() == SimpleSelector.UNIVERSAL) {
                continue;
            }
            if (s.getKind() == SimpleSelector.PSEUDO_CLASS && (s.getPseudoId() == PseudoClasses.HOVER || s.getPseudoId() == PseudoClasses.ACTIVE)) {
                found = true;
                continue;
            }
            return false;
        }
        return found;
    }

    /**
     * Tells whether the document of an element is an HTML document.
     * <p>
     * 요소가 속한 문서가 HTML 문서인지 알려 준다.
     *
     * @param e element / 요소
     * @return true for HTML documents / HTML 문서이면 true
     */
    private static boolean inHtmlDocument(Element e) {
        Document d = e.getOwnerDocument();
        return d != null && d.isHtmlDocument();
    }

    /**
     * Tells whether the document of an element is in quirks mode.
     * <p>
     * 요소가 속한 문서가 호환 모드인지 알려 준다.
     *
     * @param e element / 요소
     * @return true for quirks mode / 호환 모드이면 true
     */
    private static boolean inQuirksMode(Element e) {
        Document d = e.getOwnerDocument();
        return d != null && d.getCompatibilityMode() == Document.QUIRKS_MODE;
    }

    /**
     * Compares two namespace URIs where null and the empty string both mean "no namespace".
     * <p>
     * 두 네임스페이스 URI 를 비교한다. null 과 빈 문자열은 모두 "네임스페이스 없음"이다.
     *
     * @param a first URI / 첫 URI
     * @param b second URI / 둘째 URI
     * @return true if equal / 같으면 true
     */
    private static boolean sameNamespace(String a, String b) {
        String x = a == null ? "" : a;
        String y = b == null ? "" : b;
        return x.equals(y);
    }

    /**
     * Matches one simple selector.
     * <p>
     * 단순 선택자 하나를 맞춘다.
     *
     * @param s   the simple selector / 단순 선택자
     * @param e   the element / 요소
     * @param ctx matching context / 매칭 컨텍스트
     * @return true if it matches / 일치하면 true
     */
    private static boolean matchSimple(SimpleSelector s, Element e, MatchContext ctx) {
        switch (s.getKind()) {
        case SimpleSelector.TYPE: {
            if (!s.isNamespaceAny() && !sameNamespace(s.getNamespace(), e.getNamespaceURI())) {
                return false;
            }
            String local = e.getLocalName();
            boolean ci = inHtmlDocument(e) && HtmlStates.isHtml(e);
            return ci ? Namespaces.asciiLower(s.getName()).equals(local) : s.getName().equals(local);
        }
        case SimpleSelector.UNIVERSAL:
            return s.isNamespaceAny() || sameNamespace(s.getNamespace(), e.getNamespaceURI());
        case SimpleSelector.ID: {
            String id = e.getId();
            return inQuirksMode(e) ? Namespaces.asciiLower(id).equals(Namespaces.asciiLower(s.getName())) : id.equals(s.getName());
        }
        case SimpleSelector.CLASS:
            return hasClass(e.getAttributeNS(null, "class"), s.getName(), inQuirksMode(e));
        case SimpleSelector.ATTRIBUTE:
            return matchAttribute(s, e);
        default:
            return matchPseudo(s, e, ctx);
        }
    }

    /**
     * Tells whether a class attribute value contains a class name.
     * <p>
     * class 속성 값이 클래스 이름을 포함하는지 알려 준다.
     *
     * @param value attribute value or null / 속성 값 또는 null
     * @param name  class name / 클래스 이름
     * @param ci    true for ASCII case-insensitive comparison / ASCII 대소문자를 구분하지 않으려면 true
     * @return true if a token equals the name / 토큰 중 이름과 같은 것이 있으면 true
     */
    private static boolean hasClass(String value, String name, boolean ci) {
        if (value == null || value.length() == 0) {
            return false;
        }
        int n = value.length();
        int i = 0;
        // 공백으로 구분된 토큰을 하나씩 잘라 이름과 비교한다.
        while (i < n) {
            // 토큰 앞의 공백을 건너뛴다.
            while (i < n && Namespaces.isAsciiWhitespace(value.charAt(i))) {
                i++;
            }
            int j = i;
            // 토큰의 끝(다음 공백)까지 위치를 옮긴다.
            while (j < n && !Namespaces.isAsciiWhitespace(value.charAt(j))) {
                j++;
            }
            if (j > i && j - i == name.length() && (ci ? asciiEquals(value, i, name) : value.startsWith(name, i))) {
                return true;
            }
            i = j;
        }
        return false;
    }

    /**
     * Compares a region of a string with another string ignoring only ASCII case.
     * <p>
     * 문자열의 일부를 다른 문자열과 ASCII 대소문자만 무시하고 비교한다.
     *
     * @param value  the string / 문자열
     * @param offset start of the region / 구간 시작
     * @param name   the other string / 다른 문자열
     * @return true if equal ignoring ASCII case / ASCII 대소문자를 무시하고 같으면 true
     */
    private static boolean asciiEquals(String value, int offset, String name) {
        // 글자마다 ASCII 소문자로 바꿔 같은지 본다.
        for (int k = 0; k < name.length(); k++) {
            if (lowerAscii(value.charAt(offset + k)) != lowerAscii(name.charAt(k))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lower-cases an ASCII letter.
     * <p>
     * ASCII 글자를 소문자로 바꾼다.
     *
     * @param c character / 문자
     * @return lower-cased character / 소문자로 바꾼 문자
     */
    private static char lowerAscii(char c) {
        return c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
    }

    /**
     * Matches an attribute selector.
     * <p>
     * 속성 선택자를 맞춘다.
     *
     * @param s the selector / 선택자
     * @param e the element / 요소
     * @return true if it matches / 일치하면 true
     */
    private static boolean matchAttribute(SimpleSelector s, Element e) {
        boolean htmlElement = inHtmlDocument(e) && HtmlStates.isHtml(e);
        String local = htmlElement ? Namespaces.asciiLower(s.getName()) : s.getName();
        if (s.isNamespaceAny()) {
            // 모든 네임스페이스의 속성을 훑어 지역 이름이 같은 것 중 값이 일치하는 것을 찾는다.
            List<Attr> attrs = e.getAttributes().toList();
            for (int i = 0; i < attrs.size(); i++) {
                Attr a = attrs.get(i);
                if (local.equals(a.getLocalName()) && matchValue(s, a, htmlElement)) {
                    return true;
                }
            }
            return false;
        }
        Attr a = e.getAttributeNodeNS(s.getNamespace(), local);
        return a != null && matchValue(s, a, htmlElement);
    }

    /**
     * Applies the operator of an attribute selector to an attribute.
     * <p>
     * 속성 선택자의 연산자를 속성에 적용한다.
     *
     * @param s           the selector / 선택자
     * @param a           the attribute / 속성
     * @param htmlElement true for an HTML element of an HTML document / HTML 문서의 HTML 요소이면 true
     * @return true if the value matches / 값이 일치하면 true
     */
    private static boolean matchValue(SimpleSelector s, Attr a, boolean htmlElement) {
        int op = s.getOperator();
        if (op == SimpleSelector.OP_EXISTS) {
            return true;
        }
        boolean ci;
        if (s.getCaseMode() == SimpleSelector.CASE_INSENSITIVE) {
            ci = true;
        } else if (s.getCaseMode() == SimpleSelector.CASE_SENSITIVE) {
            ci = false;
        } else {
            ci = htmlElement && a.getNamespaceURI() == null && CASE_INSENSITIVE_VALUES.contains(a.getLocalName());
        }
        String actual = a.getValue();
        String wanted = s.getValue();
        if (ci) {
            actual = Namespaces.asciiLower(actual);
            wanted = Namespaces.asciiLower(wanted);
        }
        switch (op) {
        case SimpleSelector.OP_EQUALS:
            return actual.equals(wanted);
        case SimpleSelector.OP_INCLUDES:
            return wanted.length() > 0 && !containsWhitespace(wanted) && hasClass(actual, wanted, false);
        case SimpleSelector.OP_DASH:
            return actual.equals(wanted) || actual.startsWith(wanted + "-");
        case SimpleSelector.OP_PREFIX:
            return wanted.length() > 0 && actual.startsWith(wanted);
        case SimpleSelector.OP_SUFFIX:
            return wanted.length() > 0 && actual.endsWith(wanted);
        default:
            return wanted.length() > 0 && actual.indexOf(wanted) >= 0;
        }
    }

    /**
     * Tells whether a string contains ASCII whitespace.
     * <p>
     * 문자열에 ASCII 공백이 있는지 알려 준다.
     *
     * @param s string / 문자열
     * @return true if it contains whitespace / 공백이 있으면 true
     */
    private static boolean containsWhitespace(String s) {
        // 문자마다 공백인지 검사한다.
        for (int i = 0; i < s.length(); i++) {
            if (Namespaces.isAsciiWhitespace(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Asks the state provider about a pseudo-class and, when it does not know, computes the attribute-derived default.
     * <p>
     * 상태 제공자에게 의사 클래스를 묻고, 모르면 속성에서 얻은 기본값을 계산한다.
     *
     * @param ctx      matching context / 매칭 컨텍스트
     * @param e        element / 요소
     * @param id       pseudo-class id / 의사 클래스 id
     * @param fallback attribute-derived default / 속성에서 얻은 기본값
     * @return the answer / 답
     */
    private static boolean state(MatchContext ctx, Element e, int id, boolean fallback) {
        ctx.markDynamic();
        ElementState st = ctx.getState();
        if (st != null) {
            int r = st.get(e, id);
            if (r == ElementState.TRUE) {
                return true;
            }
            if (r == ElementState.FALSE) {
                return false;
            }
        }
        return fallback;
    }

    /**
     * Matches a pseudo-class.
     * <p>
     * 의사 클래스를 맞춘다.
     *
     * @param s   the selector / 선택자
     * @param e   the element / 요소
     * @param ctx matching context / 매칭 컨텍스트
     * @return true if it matches / 일치하면 true
     */
    private static boolean matchPseudo(SimpleSelector s, Element e, MatchContext ctx) {
        int id = s.getPseudoId();
        switch (id) {
        case PseudoClasses.ROOT:
            return isRoot(e);
        case PseudoClasses.EMPTY:
            return isEmpty(e);
        case PseudoClasses.FIRST_CHILD:
            return e.getPreviousElementSibling() == null;
        case PseudoClasses.LAST_CHILD:
            return e.getNextElementSibling() == null;
        case PseudoClasses.ONLY_CHILD:
            return e.getPreviousElementSibling() == null && e.getNextElementSibling() == null;
        case PseudoClasses.FIRST_OF_TYPE:
            return position(e, false, true, null, ctx) == 1;
        case PseudoClasses.LAST_OF_TYPE:
            return position(e, true, true, null, ctx) == 1;
        case PseudoClasses.ONLY_OF_TYPE:
            return position(e, false, true, null, ctx) == 1 && position(e, true, true, null, ctx) == 1;
        case PseudoClasses.NTH_CHILD:
            return matchNth(s, e, false, false, ctx);
        case PseudoClasses.NTH_LAST_CHILD:
            return matchNth(s, e, true, false, ctx);
        case PseudoClasses.NTH_OF_TYPE:
            return matchNth(s, e, false, true, ctx);
        case PseudoClasses.NTH_LAST_OF_TYPE:
            return matchNth(s, e, true, true, ctx);
        case PseudoClasses.NOT:
            return !matchesNested(s.getList(), e, ctx);
        case PseudoClasses.IS:
        case PseudoClasses.WHERE:
            return matchesNested(s.getList(), e, ctx);
        case PseudoClasses.HAS:
            return matchesRelative(s.getList(), e, ctx);
        case PseudoClasses.HOVER:
        case PseudoClasses.ACTIVE:
        case PseudoClasses.FOCUS:
        case PseudoClasses.FOCUS_VISIBLE:
        case PseudoClasses.FOCUS_WITHIN:
            return state(ctx, e, id, false);
        case PseudoClasses.LINK:
            return HtmlStates.isLink(e) && !state(ctx, e, PseudoClasses.VISITED, false);
        case PseudoClasses.ANY_LINK:
            return HtmlStates.isLink(e);
        case PseudoClasses.VISITED:
            return HtmlStates.isLink(e) && state(ctx, e, id, false);
        case PseudoClasses.TARGET:
            return state(ctx, e, id, HtmlStates.isTarget(e));
        case PseudoClasses.SCOPE:
            if (ctx.getScopeTest() != null) {
                return ctx.getScopeTest().isScope(e, ctx);
            }
            return ctx.getScope() != null ? ctx.getScope() == e : isRoot(e);
        case PseudoClasses.ENABLED:
            return HtmlStates.isEnabled(e);
        case PseudoClasses.DISABLED:
            return HtmlStates.isDisabled(e);
        case PseudoClasses.CHECKED:
            return state(ctx, e, id, HtmlStates.isChecked(e));
        case PseudoClasses.INDETERMINATE:
            return state(ctx, e, id, HtmlStates.isIndeterminate(e));
        case PseudoClasses.DEFAULT:
            return HtmlStates.isDefault(e);
        case PseudoClasses.REQUIRED:
            return HtmlStates.isRequired(e);
        case PseudoClasses.OPTIONAL:
            return HtmlStates.isOptional(e);
        case PseudoClasses.READ_WRITE:
            return HtmlStates.isReadWrite(e);
        case PseudoClasses.READ_ONLY:
            return !HtmlStates.isReadWrite(e);
        case PseudoClasses.PLACEHOLDER_SHOWN:
            return state(ctx, e, id, HtmlStates.isPlaceholderShown(e));
        case PseudoClasses.VALID:
            return state(ctx, e, id, HtmlStates.isValid(e));
        case PseudoClasses.INVALID:
            return state(ctx, e, id, HtmlStates.isInvalid(e));
        case PseudoClasses.IN_RANGE:
            return state(ctx, e, id, HtmlStates.isInRange(e));
        case PseudoClasses.OUT_OF_RANGE:
            return state(ctx, e, id, HtmlStates.isOutOfRange(e));
        case PseudoClasses.LANG:
            return HtmlStates.matchesLang(e, s.getWords());
        case PseudoClasses.DIR:
            return HtmlStates.direction(e).equals(s.getWords().get(0));
        case PseudoClasses.DEFINED:
            return HtmlStates.isDefined(e);
        case PseudoClasses.OPEN:
            return HtmlStates.isOpen(e);
        case PseudoClasses.MODAL:
            return HtmlStates.isModal(e);
        default:
            return false;
        }
    }

    /**
     * Tells whether an element is the root element of its document.
     * <p>
     * 요소가 문서의 루트 요소인지 알려 준다.
     *
     * @param e element / 요소
     * @return true for the document element / 문서 요소이면 true
     */
    private static boolean isRoot(Element e) {
        Node p = e.getParentNode();
        return p instanceof Document && ((Document) p).getDocumentElement() == e;
    }

    /**
     * Implements <code>:empty</code>: no element children and no text.
     * <p>
     * <code>:empty</code> 를 구현한다: 요소 자식과 글이 없다.
     *
     * @param e element / 요소
     * @return true if empty / 비어 있으면 true
     */
    private static boolean isEmpty(Element e) {
        // 자식을 훑어 요소나 글이 있으면 비어 있지 않다.
        for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
            short t = c.getNodeType();
            if (t == Node.ELEMENT_NODE) {
                return false;
            }
            if (t == Node.TEXT_NODE || t == Node.CDATA_SECTION_NODE) {
                String v = c.getNodeValue();
                if (v != null && v.length() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Computes the 1-based position of an element among its siblings.
     * <p>
     * 요소의 형제 중 1부터 시작하는 위치를 계산한다.
     *
     * @param e      the element / 요소
     * @param last   true to count from the end / 끝에서부터 세려면 true
     * @param ofType true to count only siblings of the same type / 같은 종류의 형제만 세려면 true
     * @param filter selector list that siblings must match (the <code>of S</code> clause), or null / 형제가 일치해야 하는 선택자 목록(<code>of S</code> 절). 없으면 null
     * @param ctx    matching context / 매칭 컨텍스트
     * @return the position, or 0 if the element does not match the filter / 위치. 요소가 필터와 일치하지 않으면 0
     */
    private static int position(Element e, boolean last, boolean ofType, SelectorList filter, MatchContext ctx) {
        if (filter != null && !matchesNested(filter, e, ctx)) {
            return 0;
        }
        int pos = 1;
        // 한 방향의 형제 요소를 훑으며 같은 종류이고 필터와 일치하는 것을 센다.
        for (Element s = last ? e.getNextElementSibling() : e.getPreviousElementSibling(); s != null;
                s = last ? s.getNextElementSibling() : s.getPreviousElementSibling()) {
            if (ofType && !sameType(s, e)) {
                continue;
            }
            if (filter != null && !matchesNested(filter, s, ctx)) {
                continue;
            }
            pos++;
        }
        return pos;
    }

    /**
     * Tells whether two elements have the same type (local name and namespace).
     * <p>
     * 두 요소가 같은 종류(지역 이름과 네임스페이스)인지 알려 준다.
     *
     * @param a first element / 첫 요소
     * @param b second element / 둘째 요소
     * @return true if same type / 같은 종류이면 true
     */
    private static boolean sameType(Element a, Element b) {
        return a.getLocalName().equals(b.getLocalName()) && sameNamespace(a.getNamespaceURI(), b.getNamespaceURI());
    }

    /**
     * Matches the nth pseudo-classes.
     * <p>
     * nth 계열 의사 클래스를 맞춘다.
     *
     * @param s      the selector with the An+B argument / An+B 인수를 가진 선택자
     * @param e      the element / 요소
     * @param last   true for the <code>-last-</code> variants / <code>-last-</code> 변형이면 true
     * @param ofType true for the <code>-of-type</code> variants / <code>-of-type</code> 변형이면 true
     * @param ctx    matching context / 매칭 컨텍스트
     * @return true if the position is selected / 위치가 선택되면 true
     */
    private static boolean matchNth(SimpleSelector s, Element e, boolean last, boolean ofType, MatchContext ctx) {
        int p = position(e, last, ofType, s.getList(), ctx);
        return p > 0 && s.getAnPlusB().matches(p);
    }
}
