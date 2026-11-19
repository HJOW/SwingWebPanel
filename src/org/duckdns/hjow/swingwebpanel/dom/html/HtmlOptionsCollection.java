package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>HTMLOptionsCollection</code> of a <code>select</code> (<code>select.options</code>): the live list of its options, which, unlike a
 * plain collection, can be changed: <code>length</code>, <code>add</code>, <code>remove</code>, <code>selectedIndex</code> and assignment to an
 * index.
 * <p>
 * <code>select</code> 의 <code>HTMLOptionsCollection</code>(<code>select.options</code>): option 의 라이브 목록이며 일반 컬렉션과 달리 바꿀 수 있다:
 * <code>length</code>, <code>add</code>, <code>remove</code>, <code>selectedIndex</code>, 위치에 대한 대입.
 */
public final class HtmlOptionsCollection extends FormElementList {

    /** The select element whose options this collection lists. / 이 컬렉션이 option 을 나열하는 select 요소. */
    private final HtmlSelectElement select;

    /**
     * Creates the collection of a select.
     * <p>
     * select 의 컬렉션을 만든다.
     *
     * @param select the select / select
     */
    HtmlOptionsCollection(HtmlSelectElement select) {
        super(select.document());
        this.select = select;
    }

    /**
     * Collects the options of the select.
     * <p>
     * select 의 option 을 모은다.
     *
     * @return the options / option 들
     */
    protected List<Element> compute() {
        return new ArrayList<Element>(select.optionList());
    }

    /**
     * Returns the select.
     * <p>
     * select 를 반환한다.
     *
     * @return the select / select
     */
    public HtmlSelectElement getSelect() {
        return select;
    }

    /**
     * Sets <code>length</code>, see {@link HtmlSelectElement#setLength(int)}.
     * <p>
     * <code>length</code> 를 지정한다. {@link HtmlSelectElement#setLength(int)} 를 참조한다.
     *
     * @param value the new length / 새 길이
     */
    public void setLength(int value) {
        select.setLength(value);
    }

    /**
     * Implements the indexed setter: <code>options[index] = option</code>. A null value removes the option at the index; a larger index first
     * appends empty options.
     * <p>
     * 위치 setter 를 구현한다: <code>options[index] = option</code>. null 이면 위치의 option 을 제거하고, 위치가 길이보다 크면 먼저 빈 option 을 덧붙인다.
     *
     * @param index  the index / 위치
     * @param option an <code>option</code> element or null / <code>option</code> 요소 또는 null
     */
    public void setItem(int index, Element option) {
        if (index < 0) {
            return;
        }
        if (option == null) {
            select.removeOption(index);
            return;
        }
        int n = select.getLength();
        if (index >= n) {
            // 모자란 위치는 빈 option 으로 채우고 새 option 을 끝에 붙인다.
            for (int i = n; i < index && i < n + 100000; i++) {
                select.appendChild(select.document().createElement("option"));
            }
            select.appendChild(option);
            return;
        }
        Element current = select.item(index);
        if (current == option) {
            return;
        }
        Node parent = current.getParentNode();
        if (option.contains(parent)) {
            throw DomException.hierarchyRequest("the option cannot contain the select");
        }
        parent.replaceChild(option, current);
    }
    /**
     * Implements <code>add(element, before)</code> of the options collection: see {@link HtmlSelectElement#add(Element, Element)}.
     * <p>
     * options 컬렉션의 <code>add(element, before)</code> 를 구현한다. {@link HtmlSelectElement#add(Element, Element)} 를 참조한다.
     *
     * @param element an <code>option</code> or <code>optgroup</code> / <code>option</code> 또는 <code>optgroup</code>
     * @param before  the element to go before, or null / 앞에 놓일 요소. 없으면 null
     */
    public void add(Element element, Element before) {
        select.add(element, before);
    }

    /**
     * Implements <code>remove(index)</code> of the options collection: removes the option at the index (nothing happens for an invalid index).
     * <p>
     * options 컬렉션의 <code>remove(index)</code> 를 구현한다: 위치의 option 을 제거한다(올바르지 않은 위치이면 아무 일도 없다).
     *
     * @param index the index / 위치
     */
    public void remove(int index) {
        select.removeOption(index);
    }

    /**
     * Returns the <code>selectedIndex</code> of the select.
     * <p>
     * select 의 <code>selectedIndex</code> 를 반환한다.
     *
     * @return the index or -1 / 위치. 없으면 -1
     */
    public int getSelectedIndex() {
        return select.getSelectedIndex();
    }

    /**
     * Sets the <code>selectedIndex</code> of the select.
     * <p>
     * select 의 <code>selectedIndex</code> 를 지정한다.
     *
     * @param index the new index / 새 위치
     */
    public void setSelectedIndex(int index) {
        select.setSelectedIndex(index);
    }
}
