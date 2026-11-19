package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>HTMLPropertiesCollection</code> of the Note "HTML Microdata" (section 6.1): the collection of the property elements of one item
 * (<code>element.properties</code>). It is live in the sense that every access reads the tree again ({@link Microdata#properties(Element)}), so
 * it follows changes of the document; it is empty while the element is not an item. <code>names</code> lists the property names in tree
 * order without duplicates, and {@link #getNamed(String)} returns the {@link PropertyNodeList} of one name (the same object every time).
 * <p>
 * 노트 "HTML Microdata"(6.1절)의 <code>HTMLPropertiesCollection</code>: 아이템 하나의 프로퍼티 요소 컬렉션(<code>element.properties</code>). 접근할 때마다 트리를 다시
 * 읽으므로({@link Microdata#properties(Element)}) 문서의 변경을 따르며, 요소가 아이템이 아니면 비어 있다. <code>names</code> 는 프로퍼티 이름을 중복 없이 트리
 * 순서로 나열하고, {@link #getNamed(String)} 는 이름 하나의 {@link PropertyNodeList} 를 반환한다(매번 같은 객체).
 */
public final class PropertiesCollection extends ElementCollection {

    /**
     * The element whose properties the collection lists.
     * <p>
     * 컬렉션이 프로퍼티를 나열하는 요소.
     */
    private final Element item;

    /**
     * The lists of the names that were asked for.
     * <p>
     * 요청된 이름의 목록.
     */
    private final Map<String, PropertyNodeList> named = new HashMap<String, PropertyNodeList>();

    /**
     * Creates the collection of an element.
     * <p>
     * 요소의 컬렉션을 만든다.
     *
     * @param item the element (an item or not) / 요소(아이템이거나 아님)
     */
    public PropertiesCollection(Element item) {
        super(item);
        this.item = item;
    }

    /**
     * Not used: the elements are found by {@link Microdata#properties(Element)}.
     * <p>
     * 쓰이지 않는다: 요소는 {@link Microdata#properties(Element)} 로 찾는다.
     *
     * @param element a candidate / 후보
     * @return false / false
     */
    boolean matches(Element element) {
        return false;
    }

    /**
     * Returns the number of properties.
     * <p>
     * 프로퍼티 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return Microdata.properties(item).size();
    }

    /**
     * Returns the property element at an index in tree order.
     * <p>
     * 위치의 프로퍼티 요소를 트리 순서로 반환한다.
     *
     * @param index the index / 위치
     * @return the element or null / 요소 또는 null
     */
    public Element item(int index) {
        List<Element> list = Microdata.properties(item);
        return index >= 0 && index < list.size() ? list.get(index) : null;
    }

    /**
     * Returns the first property element whose name is the given name (the supported name of the HTMLCollection is overridden by the names
     * of the properties).
     * <p>
     * 이름이 같은 첫 프로퍼티 요소를 반환한다(HTMLCollection 의 지원 이름은 프로퍼티 이름으로 대체된다).
     *
     * @param name the name / 이름
     * @return the element or null / 요소 또는 null
     */
    public Element namedItem(String name) {
        List<Element> list = Microdata.properties(item);
        // 프로퍼티 이름에 그 이름이 있는 첫 요소를 찾는다.
        for (int i = 0; i < list.size(); i++) {
            if (Microdata.propertyNames(list.get(i)).contains(name)) {
                return list.get(i);
            }
        }
        return null;
    }

    /**
     * Returns the property names in tree order without duplicates.
     * <p>
     * 프로퍼티 이름을 중복 없이 트리 순서로 반환한다.
     *
     * @return the names / 이름
     */
    public List<String> getNames() {
        List<String> names = new ArrayList<String>();
        List<Element> list = Microdata.properties(item);
        // 요소마다 이름을 모으되 이미 나온 이름은 건너뛴다.
        for (int i = 0; i < list.size(); i++) {
            List<String> own = Microdata.propertyNames(list.get(i));
            // 요소의 이름 중 아직 없는 것만 더한다.
            for (int j = 0; j < own.size(); j++) {
                if (!names.contains(own.get(j))) {
                    names.add(own.get(j));
                }
            }
        }
        return names;
    }

    /**
     * Returns the list of the elements that add a property of a name, or null when no property has that name (<code>collection[name]</code>
     * and <code>namedItem(name)</code>).
     * <p>
     * 이름이 같은 프로퍼티를 더하는 요소의 목록을 반환한다. 그 이름의 프로퍼티가 없으면 null(<code>collection[name]</code>, <code>namedItem(name)</code>).
     *
     * @param name the property name / 프로퍼티 이름
     * @return the list or null / 목록 또는 null
     */
    public PropertyNodeList getNamed(String name) {
        if (!getNames().contains(name)) {
            return null;
        }
        PropertyNodeList list = named.get(name);
        if (list == null) {
            list = new PropertyNodeList(item, name);
            named.put(name, list);
        }
        return list;
    }
}
