package org.duckdns.hjow.swingwebpanel.css.selector;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.SelectorEngine;

/**
 * The selector engine behind <code>querySelector</code>, <code>querySelectorAll</code>, <code>matches</code> and <code>closest</code>
 * (plan item P08-09): it plugs into the DOM through {@link SelectorEngine}. Selector text is parsed with {@link SelectorParser} (an invalid
 * selector raises a DOM <code>SyntaxError</code>), parsed lists are cached, results come in tree order, and the node list returned by
 * <code>querySelectorAll</code> is a static snapshot. Selectors with namespace prefixes are not valid here because no namespace map exists.
 * Pseudo-elements never match an element. Install it with {@link #install(Document)}.
 * <p>
 * <code>querySelector</code>, <code>querySelectorAll</code>, <code>matches</code>, <code>closest</code> 뒤에 있는 선택자 엔진(계획 P08-09). {@link SelectorEngine} 로 DOM 에
 * 연결된다. 선택자 텍스트는 {@link SelectorParser} 로 파싱하며(올바르지 않으면 DOM <code>SyntaxError</code>), 파싱한 목록은 캐시하고, 결과는 트리 순서이며,
 * <code>querySelectorAll</code> 이 반환하는 목록은 고정된 스냅샷이다. 네임스페이스 맵이 없으므로 네임스페이스 접두사가 있는 선택자는 여기서 올바르지 않다.
 * 의사 요소는 요소와 일치하지 않는다. {@link #install(Document)} 로 설치한다.
 */
public final class CssSelectorEngine implements SelectorEngine {

    /** Number of parsed selector lists kept in the cache. / 캐시에 보관하는 파싱된 선택자 목록의 수. */
    public static final int CACHE_SIZE = 256;

    /** Parse cache (access ordered, bounded). / 파싱 캐시(접근 순서, 크기 제한). */
    private final Map<String, SelectorList> cache = new LinkedHashMap<String, SelectorList>(64, 0.75f, true) {
        private static final long serialVersionUID = 1L;

        protected boolean removeEldestEntry(Map.Entry<String, SelectorList> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /** Dynamic state provider for pseudo-classes or null. / 의사 클래스용 동적 상태 제공자 또는 null. */
    private volatile ElementState state;

    /**
     * Creates an engine without a dynamic state provider.
     * <p>
     * 동적 상태 제공자가 없는 엔진을 만든다.
     */
    public CssSelectorEngine() {
    }

    /**
     * Creates an engine and installs it on a document.
     * <p>
     * 엔진을 만들어 문서에 설치한다.
     *
     * @param document the document / 문서
     * @return the installed engine / 설치한 엔진
     */
    public static CssSelectorEngine install(Document document) {
        CssSelectorEngine e = new CssSelectorEngine();
        document.setSelectorEngine(e);
        return e;
    }

    /**
     * Sets the provider of dynamic pseudo-class state (hover, focus, form state).
     * <p>
     * 동적 의사 클래스 상태(hover, focus, 폼 상태)의 제공자를 설정한다.
     *
     * @param state provider or null / 제공자 또는 null
     */
    public void setElementState(ElementState state) {
        this.state = state;
    }

    /**
     * Parses selector text with the cache; an invalid selector raises <code>SyntaxError</code>.
     * <p>
     * 캐시를 이용해 선택자 텍스트를 파싱한다. 올바르지 않으면 <code>SyntaxError</code> 를 낸다.
     *
     * @param selectors selector text / 선택자 문자열
     * @return the parsed list / 파싱한 목록
     */
    private SelectorList parse(String selectors) {
        synchronized (cache) {
            SelectorList l = cache.get(selectors);
            if (l != null) {
                return l;
            }
        }
        SelectorList parsed;
        try {
            parsed = SelectorParser.parse(selectors, null);
        } catch (SelectorSyntaxException e) {
            throw DomException.syntax("'" + selectors + "' is not a valid selector: " + e.getMessage());
        }
        synchronized (cache) {
            cache.put(selectors, parsed);
        }
        return parsed;
    }

    /**
     * Creates the matching context of an operation.
     * <p>
     * 작업의 매칭 컨텍스트를 만든다.
     *
     * @param scope the scoping element or null / 범위 요소 또는 null
     * @return a new context / 새 컨텍스트
     */
    private MatchContext context(Element scope) {
        return new MatchContext(scope, state);
    }

    /**
     * Returns the first descendant element of a root that matches (tree order).
     * <p>
     * 루트의 자손 요소 중 처음 일치하는 요소를 반환한다(트리 순서).
     *
     * @param root      document, fragment or element / 문서, 조각 또는 요소
     * @param selectors selector text / 선택자 문자열
     * @return the element or null / 요소 또는 null
     */
    public Element querySelector(Node root, String selectors) {
        SelectorList list = parse(selectors);
        MatchContext ctx = context(root instanceof Element ? (Element) root : null);
        // 루트의 자손을 트리 순서로 훑으며 처음 일치하는 요소를 찾는다.
        for (Node n = Node.nextInTree(root, root); n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof Element) {
                ctx.resetBudget();
                if (SelectorMatcher.matches(list, (Element) n, ctx)) {
                    return (Element) n;
                }
            }
        }
        return null;
    }

    /**
     * Returns all descendant elements of a root that match, as a static list in tree order.
     * <p>
     * 루트의 자손 요소 중 일치하는 모든 요소를 트리 순서의 고정된 목록으로 반환한다.
     *
     * @param root      document, fragment or element / 문서, 조각 또는 요소
     * @param selectors selector text / 선택자 문자열
     * @return matching elements / 일치하는 요소
     */
    public List<Element> querySelectorAll(Node root, String selectors) {
        SelectorList list = parse(selectors);
        MatchContext ctx = context(root instanceof Element ? (Element) root : null);
        List<Element> out = new ArrayList<Element>();
        // 루트의 자손을 트리 순서로 훑으며 일치하는 요소를 모두 모은다.
        for (Node n = Node.nextInTree(root, root); n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof Element) {
                ctx.resetBudget();
                if (SelectorMatcher.matches(list, (Element) n, ctx)) {
                    out.add((Element) n);
                }
            }
        }
        return out;
    }

    /**
     * Tells whether an element matches a selector list; <code>:scope</code> is the element itself.
     * <p>
     * 요소가 선택자 목록과 일치하는지 알려 준다. <code>:scope</code> 는 요소 자신이다.
     *
     * @param element   the element / 요소
     * @param selectors selector text / 선택자 문자열
     * @return true if it matches / 일치하면 true
     */
    public boolean matches(Element element, String selectors) {
        SelectorList list = parse(selectors);
        return SelectorMatcher.matches(list, element, context(element));
    }

    /**
     * Returns the closest inclusive ancestor of an element that matches a selector list.
     * <p>
     * 요소와 그 조상 중 선택자 목록과 일치하는 가장 가까운 요소를 반환한다.
     *
     * @param element   the element / 요소
     * @param selectors selector text / 선택자 문자열
     * @return the element or null / 요소 또는 null
     */
    public Element closest(Element element, String selectors) {
        SelectorList list = parse(selectors);
        MatchContext ctx = context(element);
        // 요소에서 위로 올라가며 처음 일치하는 요소를 찾는다.
        for (Element e = element; e != null; e = e.getParentElement()) {
            ctx.resetBudget();
            if (SelectorMatcher.matches(list, e, ctx)) {
                return e;
            }
        }
        return null;
    }
}
