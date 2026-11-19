package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * The live view of the attributes of an element (<code>NamedNodeMap</code> of the <code>attributes</code> property).
 * <p>
 * 요소의 속성에 대한 라이브 뷰(<code>attributes</code> 속성의 <code>NamedNodeMap</code>).
 */
public final class NamedAttributeMap {

    /**
     * The element.
     * <p>
     * 요소.
     */
    private final Element element;

    /**
     * Creates the view.
     * <p>
     * 뷰를 만든다.
     *
     * @param element owner element / 소유 요소
     */
    NamedAttributeMap(Element element) {
        this.element = element;
    }

    /**
     * Returns the number of attributes.
     * <p>
     * 속성의 수를 반환한다.
     *
     * @return length / 길이
     */
    public int getLength() {
        return element.attributeCount();
    }

    /**
     * Returns the attribute at an index (in the order they were added).
     * <p>
     * 위치의 속성을 반환한다(추가된 순서).
     *
     * @param index zero based index / 0 기반 위치
     * @return attribute or null / 속성 또는 null
     */
    public Attr item(int index) {
        return element.attributeAt(index);
    }

    /**
     * Returns the attribute with a qualified name.
     * <p>
     * 정규화된 이름의 속성을 반환한다.
     *
     * @param qualifiedName name / 이름
     * @return attribute or null / 속성 또는 null
     */
    public Attr getNamedItem(String qualifiedName) {
        return element.getAttributeNode(qualifiedName);
    }

    /**
     * Returns the attribute with a namespace and local name.
     * <p>
     * 네임스페이스와 로컬 이름의 속성을 반환한다.
     *
     * @param namespace namespace or null / 네임스페이스 또는 null
     * @param localName local name / 로컬 이름
     * @return attribute or null / 속성 또는 null
     */
    public Attr getNamedItemNS(String namespace, String localName) {
        return element.getAttributeNodeNS(namespace, localName);
    }

    /**
     * Sets an attribute object, replacing one with the same name.
     * <p>
     * 속성 객체를 지정하며 같은 이름의 것을 교체한다.
     *
     * @param attr attribute / 속성
     * @return replaced attribute or null / 교체된 속성 또는 null
     * @throws DomException InUseAttributeError if the attribute belongs to another element / 다른 요소의 속성이면 InUseAttributeError
     */
    public Attr setNamedItem(Attr attr) {
        return element.setAttributeNode(attr);
    }

    /**
     * Removes the attribute with a qualified name.
     * <p>
     * 정규화된 이름의 속성을 제거한다.
     *
     * @param qualifiedName name / 이름
     * @return removed attribute / 제거한 속성
     * @throws DomException NotFoundError if there is no such attribute / 속성이 없으면 NotFoundError
     */
    public Attr removeNamedItem(String qualifiedName) {
        Attr a = element.getAttributeNode(qualifiedName);
        if (a == null) {
            throw DomException.notFound("no attribute named " + qualifiedName);
        }
        return element.removeAttributeNode(a);
    }

    /**
     * Returns a snapshot copy of the attributes.
     * <p>
     * 속성의 스냅샷 복사본을 반환한다.
     *
     * @return attributes / 속성
     */
    public List<Attr> toList() {
        List<Attr> list = new ArrayList<Attr>();
        // 속성을 순서대로 담는다.
        for (int i = 0; i < getLength(); i++) {
            list.add(item(i));
        }
        return list;
    }
}
