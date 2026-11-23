package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * A live list of elements that the form layer exposes (<code>form.elements</code>, <code>select.options</code>,
 * <code>select.selectedOptions</code>, <code>control.labels</code>, <code>datalist.options</code>, radio node lists). Like the DOM's live collections it
 * is computed again when the document changed since the last access, and also when the state version of the form support changed (the
 * selectedness of an option is state, not an attribute), and otherwise answers from a cache. Subclasses only say how to compute the elements.
 * <p>
 * 폼 계층이 노출하는 요소의 라이브 목록(<code>form.elements</code>, <code>select.options</code>, <code>select.selectedOptions</code>, <code>control.labels</code>,
 * <code>datalist.options</code>, 라디오 노드 목록). DOM 의 라이브 컬렉션처럼 마지막 접근 뒤 문서가 바뀌었거나 폼 지원의 상태 버전이 바뀌었으면(option 의
 * selectedness 는 속성이 아니라 상태이다) 다시 계산하고 아니면 캐시로 답한다. 하위 클래스는 요소를 계산하는 방법만 말한다.
 */
public abstract class FormElementList {

    /** The document whose versions invalidate the cache. / 버전이 캐시를 무효화하는 문서. */
    private final Document document;

    /** The cached elements. / 캐시한 요소. */
    private List<Element> cache;

    /** The mutation version of the cache. / 캐시의 변경 버전. */
    private long cachedMutation = -1;

    /** The state version of the cache. / 캐시의 상태 버전. */
    private long cachedState = -1;

    /**
     * Creates a list of a document.
     * <p>
     * 문서의 목록을 만든다.
     *
     * @param document the document / 문서
     */
    protected FormElementList(Document document) {
        this.document = document;
    }

    /**
     * Computes the elements in tree order.
     * <p>
     * 요소를 트리 순서로 계산한다.
     *
     * @return the elements / 요소
     */
    protected abstract List<Element> compute();

    /**
     * Returns the cached elements, recomputing them if the document or a control state changed.
     * <p>
     * 캐시한 요소를 반환한다. 문서나 컨트롤 상태가 바뀌었으면 다시 계산한다.
     *
     * @return the elements (do not modify) / 요소(수정 금지)
     */
    final List<Element> current() {
        long m = document.getMutationVersion();
        long s = document.getFormSupport().getStateVersion();
        if (cache == null || m != cachedMutation || s != cachedState) {
            cache = compute();
            cachedMutation = m;
            cachedState = s;
        }
        return cache;
    }

    /**
     * Returns the number of elements.
     * <p>
     * 요소의 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return current().size();
    }

    /**
     * Returns the element at an index.
     * <p>
     * 위치의 요소를 반환한다.
     *
     * @param index the index / 위치
     * @return the element or null if out of range / 요소. 범위 밖이면 null
     */
    public Element item(int index) {
        List<Element> l = current();
        return index >= 0 && index < l.size() ? l.get(index) : null;
    }

    /**
     * Returns the first element whose id equals the name or, otherwise, whose <code>name</code> attribute equals it.
     * <p>
     * id 가 이름과 같은 첫 요소, 없으면 <code>name</code> 속성이 같은 첫 요소를 반환한다.
     *
     * @param name the id or name / id 또는 name
     * @return the element, a list of elements for lists that group same-named controls, or null / 요소(같은 이름의 컨트롤을 묶는 목록이면 요소 목록). 없으면 null
     */
    public Object namedItem(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        List<Element> l = current();
        // 먼저 id 가 일치하는 요소를 찾는다.
        for (int i = 0; i < l.size(); i++) {
            if (name.equals(l.get(i).getId())) {
                return l.get(i);
            }
        }
        // 없으면 HTML 요소의 name 속성이 일치하는 요소를 찾는다.
        for (int i = 0; i < l.size(); i++) {
            Element e = l.get(i);
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
     * @return the elements / 요소
     */
    public List<Element> toList() {
        return new ArrayList<Element>(current());
    }
}
