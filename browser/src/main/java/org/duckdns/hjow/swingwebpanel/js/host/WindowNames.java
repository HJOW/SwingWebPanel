package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollections;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;

/**
 * The named properties of the <code>Window</code> object of a page (HTML5 REC 2014-10-28 section 5.1.4, named access on the Window
 * object): the global name <code>logo</code> is the element with <code>id="logo"</code> or the <code>img</code>, <code>form</code>,
 * <code>a</code>, <code>embed</code> and so on that has <code>name="logo"</code> (see {@link ElementCollections#windowNames}), as old
 * pages use them (<code>menu.style.display = ...</code> with no <code>getElementById</code>). One element is that element, several are a
 * live collection of them. A name that the global object or its prototype chain has is never hidden (see {@link JsRealm.NamedGlobals}),
 * the names are indexed once for each state of the document, and the names of frames are not a part of this class.
 * <p>
 * 페이지의 <code>Window</code> 객체의 이름 있는 프로퍼티(HTML5 REC 2014-10-28 §5.1.4, Window 객체의 이름 접근): 전역 이름 <code>logo</code> 는 <code>id="logo"</code> 인 요소,
 * 또는 <code>name="logo"</code> 인 <code>img</code>, <code>form</code>, <code>a</code>, <code>embed</code> 등이다({@link ElementCollections#windowNames} 참조). 옛
 * 페이지가 <code>getElementById</code> 없이 쓰던 방식이다(<code>menu.style.display = ...</code>). 요소가 하나이면 그 요소, 여럿이면 그 요소들의 라이브 컬렉션이다. 전역 객체나
 * 프로토타입 체인에 있는 이름은 가려지지 않으며({@link JsRealm.NamedGlobals} 참조), 이름은 문서의 상태마다 한 번 색인하고, 프레임의 이름은 이 클래스의 몫이 아니다.
 */
final class WindowNames implements JsRealm.NamedGlobals {

    /**
     * The page whose document and bridge are used.
     * <p>
     * 문서와 브리지를 쓰는 페이지.
     */
    private final PageScripts page;

    /**
     * The elements by name for the state of the document that the version names (in tree order).
     * <p>
     * 버전이 가리키는 문서 상태의 이름별 요소(트리 순서).
     */
    private Map<String, List<Element>> index;

    /**
     * The document that the index was built for.
     * <p>
     * 색인을 만든 문서.
     */
    private Document indexed;

    /**
     * The mutation version of the document that the index is valid for.
     * <p>
     * 색인이 유효한 문서의 변경 버전.
     */
    private long indexVersion = -1;

    /**
     * Creates the lookup of a page.
     * <p>
     * 페이지의 조회를 만든다.
     *
     * @param page the page / 페이지
     */
    WindowNames(PageScripts page) {
        this.page = page;
    }

    /**
     * Looks a name up: the element, a live collection of the elements, or null.
     * <p>
     * 이름을 찾는다: 요소, 요소들의 라이브 컬렉션, 또는 null.
     *
     * @param name the name / 이름
     * @return the value or null / 값 또는 null
     */
    public synchronized JsValue lookup(String name) {
        Document doc = page.getDocument();
        if (doc == null || name.length() == 0) {
            return null;
        }
        long version = doc.getMutationVersion();
        if (index == null || indexed != doc || indexVersion != version) {
            Map<String, List<Element>> built = new LinkedHashMap<String, List<Element>>();
            // 문서의 모든 요소를 트리 순서로 훑으며 이름 있는 프로퍼티가 되는 이름마다 요소를 모은다.
            for (Node n = Node.nextInTree(doc, doc); n != null; n = Node.nextInTree(n, doc)) {
                if (n instanceof Element) {
                    List<String> names = ElementCollections.windowNames((Element) n);
                    // 한 요소의 이름들을 색인에 더한다(같은 이름이 둘이면 한 번만).
                    for (int i = 0; i < names.size(); i++) {
                        List<Element> list = built.get(names.get(i));
                        if (list == null) {
                            list = new ArrayList<Element>(1);
                            built.put(names.get(i), list);
                        }
                        if (list.isEmpty() || list.get(list.size() - 1) != n) {
                            list.add((Element) n);
                        }
                    }
                }
            }
            index = built;
            indexed = doc;
            indexVersion = version;
        }
        List<Element> hits = index.get(name);
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        return hits.size() == 1 ? page.getBridge().toJs(hits.get(0)) : page.getBridge().toJs(ElementCollections.windowNamed(doc, name));
    }
}
