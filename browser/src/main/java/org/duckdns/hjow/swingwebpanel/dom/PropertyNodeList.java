package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>PropertyNodeList</code> of the Note "HTML Microdata" (section 6.1): the elements of an item that add a property of one name, in
 * tree order, read from the tree at every access. {@link #getValues()} gives the values of the elements (<code>itemValue</code>: a string, or
 * the element itself for a nested item).
 * <p>
 * 노트 "HTML Microdata"(6.1절)의 <code>PropertyNodeList</code>: 아이템에서 이름 하나의 프로퍼티를 더하는 요소를 트리 순서로 나열하며 접근할 때마다 트리에서 읽는다.
 * {@link #getValues()} 는 요소의 값(<code>itemValue</code>: 문자열, 중첩된 아이템이면 요소 자신)을 준다.
 */
public final class PropertyNodeList implements NodeList {

    /**
     * The item whose properties are listed.
     * <p>
     * 프로퍼티를 나열하는 아이템.
     */
    private final Element item;

    /**
     * The property name.
     * <p>
     * 프로퍼티 이름.
     */
    private final String name;

    /**
     * Creates the list.
     * <p>
     * 목록을 만든다.
     *
     * @param item the item / 아이템
     * @param name the property name / 프로퍼티 이름
     */
    PropertyNodeList(Element item, String name) {
        this.item = item;
        this.name = name;
    }

    /**
     * Reads the elements with the name from the tree.
     * <p>
     * 이름이 같은 요소를 트리에서 읽는다.
     *
     * @return the elements / 요소
     */
    private List<Element> elements() {
        List<Element> out = new ArrayList<Element>();
        List<Element> all = Microdata.properties(item);
        // 프로퍼티 요소 중 이름이 있는 것만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (Microdata.propertyNames(all.get(i)).contains(name)) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /**
     * Returns the number of elements.
     * <p>
     * 요소 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return elements().size();
    }

    /**
     * Returns the element at an index.
     * <p>
     * 위치의 요소를 반환한다.
     *
     * @param index the index / 위치
     * @return the element or null / 요소 또는 null
     */
    public Node item(int index) {
        List<Element> list = elements();
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    /**
     * Returns a snapshot copy of the elements as a Java list.
     * <p>
     * 요소를 Java 목록의 스냅샷 복사본으로 반환한다.
     *
     * @return the elements / 요소
     */
    public List<Node> toList() {
        return new ArrayList<Node>(elements());
    }

    /**
     * Returns the values of the elements in tree order: a String, or the Element itself for a nested item.
     * <p>
     * 요소의 값을 트리 순서로 반환한다: 문자열, 중첩된 아이템이면 Element 자신.
     *
     * @return the values / 값
     */
    public List<Object> getValues() {
        List<Object> out = new ArrayList<Object>();
        List<Element> list = elements();
        // 요소마다 itemValue 를 읽는다.
        for (int i = 0; i < list.size(); i++) {
            out.add(Microdata.itemValue(list.get(i)));
        }
        return out;
    }
}
