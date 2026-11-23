package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The <code>HTMLFormControlsCollection</code> of a form (<code>form.elements</code>): the live list of its listed elements, in tree order
 * (image buttons excluded for historical reasons). <code>namedItem</code> returns the element itself if only one control has the name or id,
 * and a {@link RadioNodeList} of all of them otherwise.
 * <p>
 * 폼의 <code>HTMLFormControlsCollection</code>(<code>form.elements</code>): 목록 요소의 라이브 목록(트리 순서. 이미지 버튼은 역사적인 이유로 제외).
 * <code>namedItem</code> 은 이름이나 id 가 같은 컨트롤이 하나이면 그 요소를, 여럿이면 모두를 담은 {@link RadioNodeList} 를 반환한다.
 */
public final class FormControlsCollection extends FormElementList {

    /** The form whose controls this collection lists. / 이 컬렉션이 컨트롤을 나열하는 폼. */
    private final HtmlFormElement form;

    /**
     * Creates the collection of a form.
     * <p>
     * 폼의 컬렉션을 만든다.
     *
     * @param form the form / 폼
     */
    FormControlsCollection(HtmlFormElement form) {
        super(form.document());
        this.form = form;
    }

    /**
     * Collects the listed elements of the form.
     * <p>
     * 폼의 목록 요소를 모은다.
     *
     * @return the elements / 요소
     */
    protected List<Element> compute() {
        List<HtmlListedElement> all = form.listedElements();
        List<Element> out = new ArrayList<Element>(all.size());
        // 목록 요소 중 elements 에 오르는 것만 모은다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isInFormElements()) {
                out.add(all.get(i));
            }
        }
        return out;
    }

    /**
     * Returns the control with an id or name, or a radio node list if several have it.
     * <p>
     * id 나 name 이 같은 컨트롤을 반환한다. 여럿이면 라디오 노드 목록.
     *
     * @param name the id or name / id 또는 name
     * @return an element, a {@link RadioNodeList} or null / 요소, {@link RadioNodeList} 또는 null
     */
    public Object namedItem(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        List<Element> match = matches(name);
        if (match.isEmpty()) {
            return null;
        }
        return match.size() == 1 ? (Object) match.get(0) : new RadioNodeList(form, name);
    }

    /**
     * Returns the controls whose id or name is the given name.
     * <p>
     * id 나 name 이 주어진 이름인 컨트롤을 반환한다.
     *
     * @param name the name / 이름
     * @return the controls / 컨트롤
     */
    List<Element> matches(String name) {
        List<Element> l = current();
        List<Element> out = new ArrayList<Element>();
        // 컨트롤을 순서대로 보며 id 나 name 이 일치하는 것을 모은다.
        for (int i = 0; i < l.size(); i++) {
            Element e = l.get(i);
            if (name.equals(e.getId()) || name.equals(e.getAttribute("name"))) {
                out.add(e);
            }
        }
        return out;
    }
}
