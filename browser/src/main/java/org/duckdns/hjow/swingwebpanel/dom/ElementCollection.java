package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * A live collection of elements (<code>HTMLCollection</code>): the result of <code>getElementsByTagName</code>,
 * <code>getElementsByClassName</code> and <code>children</code>. It re-reads the tree when the document changed since the last access and
 * otherwise answers from a cache, so repeated access in a loop stays cheap while the result always reflects the current tree.
 * <p>
 * 요소의 라이브 컬렉션(<code>HTMLCollection</code>): <code>getElementsByTagName</code>, <code>getElementsByClassName</code>,
 * <code>children</code> 의 결과. 마지막 접근 뒤에 문서가 바뀌었으면 트리를 다시 읽고, 아니면 캐시로 답하므로 반복문에서 되풀이해 접근해도
 * 저렴하면서 결과는 항상 현재 트리를 반영한다.
 */
public abstract class ElementCollection {

    /**
     * Root of the search.
     * <p>
     * 검색의 루트.
     */
    private final Node root;

    /**
     * Cached result.
     * <p>
     * 캐시된 결과.
     */
    private List<Element> cache;

    /**
     * Document version of the cache.
     * <p>
     * 캐시의 문서 버전.
     */
    private long cacheVersion = -1;

    /**
     * Creates a collection.
     * <p>
     * 컬렉션을 만든다.
     *
     * @param root root of the search / 검색의 루트
     */
    ElementCollection(Node root) {
        this.root = root;
    }

    /**
     * Tells whether an element belongs to the collection.
     * <p>
     * 요소가 컬렉션에 속하는지 알려 준다.
     *
     * @param element candidate descendant of the root / 루트의 후손 후보
     * @return true if it matches / 일치하면 true
     */
    abstract boolean matches(Element element);

    /**
     * Tells whether only the children of the root are candidates (otherwise all descendants).
     * <p>
     * 루트의 자식만 후보인지 알려 준다(아니면 모든 후손).
     *
     * @return true for children only / 자식만이면 true
     */
    boolean childrenOnly() {
        return false;
    }

    /**
     * Returns the current elements.
     * <p>
     * 현재 요소 목록을 반환한다.
     *
     * @return elements in tree order (do not modify) / 트리 순서의 요소(수정 금지)
     */
    private List<Element> current() {
        long version = root.doc.getMutationVersion();
        if (cache == null || cacheVersion != version) {
            List<Element> list = new ArrayList<Element>();
            if (childrenOnly()) {
                // 자식만 후보이면 자식 중 조건에 맞는 요소를 모은다.
                for (Node c = root.firstChild; c != null; c = c.nextSibling) {
                    if (c instanceof Element && matches((Element) c)) {
                        list.add((Element) c);
                    }
                }
            } else {
                Node n = Node.nextInTree(root, root);
                // 모든 후손을 트리 순서로 훑으며 조건에 맞는 요소를 모은다.
                while (n != null) {
                    if (n instanceof Element && matches((Element) n)) {
                        list.add((Element) n);
                    }
                    n = Node.nextInTree(n, root);
                }
            }
            cache = list;
            cacheVersion = version;
        }
        return cache;
    }

    /**
     * Returns the number of elements.
     * <p>
     * 요소의 수를 반환한다.
     *
     * @return length / 길이
     */
    public int getLength() {
        return current().size();
    }

    /**
     * Returns the element at an index.
     * <p>
     * 위치의 요소를 반환한다.
     *
     * @param index zero based index / 0 기반 위치
     * @return element or null if out of range / 요소. 범위 밖이면 null
     */
    public Element item(int index) {
        List<Element> list = current();
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    /**
     * Returns the first element whose id equals the name, or, for HTML elements, whose name attribute equals it
     * (<code>namedItem</code>).
     * <p>
     * id 가 이름과 같은 첫 요소, 또는 HTML 요소이면 name 속성이 같은 첫 요소를 반환한다(<code>namedItem</code>).
     *
     * @param name id or name / id 또는 name
     * @return element or null / 요소 또는 null
     */
    public Element namedItem(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        List<Element> list = current();
        // 먼저 id 가 일치하는 요소를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            if (name.equals(list.get(i).getId())) {
                return list.get(i);
            }
        }
        // 없으면 HTML 요소의 name 속성이 일치하는 요소를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            Element e = list.get(i);
            if (Namespaces.HTML.equals(e.getNamespaceURI()) && name.equals(e.getAttribute("name"))) {
                return e;
            }
        }
        return null;
    }

    /**
     * Returns a snapshot copy of the elements.
     * <p>
     * 요소의 스냅샷 복사본을 반환한다.
     *
     * @return elements / 요소 목록
     */
    public List<Element> toList() {
        return new ArrayList<Element>(current());
    }
}
