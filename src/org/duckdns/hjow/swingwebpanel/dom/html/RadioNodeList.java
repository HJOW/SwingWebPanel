package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The <code>RadioNodeList</code> (HTML Standard 2.7.2.? / 4.10.3): the live list of the controls of a form that share an id or name. Its
 * <code>value</code> is the value of the checked radio button of the list (and setting it checks the radio button with that value).
 * <p>
 * <code>RadioNodeList</code>: 폼에서 id 나 name 을 공유하는 컨트롤의 라이브 목록. <code>value</code> 는 목록에서 체크된 라디오 버튼의 값이며(값을 지정하면 그 값을 가진
 * 라디오 버튼을 체크한다).
 */
public final class RadioNodeList extends FormElementList {

    /** The form whose controls this collection lists. / 이 컬렉션이 컨트롤을 나열하는 폼. */
    private final HtmlFormElement form;

    /** The shared name or id. / 공유하는 name 또는 id. */
    private final String name;

    /**
     * Creates the list.
     * <p>
     * 목록을 만든다.
     *
     * @param form the form / 폼
     * @param name the shared name or id / 공유하는 name 또는 id
     */
    RadioNodeList(HtmlFormElement form, String name) {
        super(form.document());
        this.form = form;
        this.name = name;
    }

    /**
     * Collects the controls with the name.
     * <p>
     * 이름이 같은 컨트롤을 모은다.
     *
     * @return the controls / 컨트롤
     */
    protected List<Element> compute() {
        return form.getElements().matches(name);
    }

    /**
     * Returns the <code>value</code> IDL attribute: the value of the first checked radio button of the list, or the empty string.
     * <p>
     * <code>value</code> IDL 속성을 반환한다: 목록에서 처음 체크된 라디오 버튼의 값. 없으면 빈 문자열.
     *
     * @return the value / 값
     */
    public String getValue() {
        List<Element> l = current();
        // 라디오 버튼 중 체크된 첫 번째의 값을 찾는다.
        for (int i = 0; i < l.size(); i++) {
            Element e = l.get(i);
            if (e instanceof HtmlInputElement && ((HtmlInputElement) e).kind() == InputKinds.RADIO && ((HtmlInputElement) e).isChecked()) {
                return ((HtmlInputElement) e).getValue();
            }
        }
        return "";
    }

    /**
     * Sets <code>value</code>: checks the first radio button whose value is the given value.
     * <p>
     * <code>value</code> 를 지정한다: 값이 같은 첫 라디오 버튼을 체크한다.
     *
     * @param value the value / 값
     */
    public void setValue(String value) {
        List<Element> l = current();
        // 값이 같은 첫 라디오 버튼을 체크한다.
        for (int i = 0; i < l.size(); i++) {
            Element e = l.get(i);
            if (e instanceof HtmlInputElement && ((HtmlInputElement) e).kind() == InputKinds.RADIO && ((HtmlInputElement) e).getValue().equals(value)) {
                ((HtmlInputElement) e).setChecked(true);
                return;
            }
        }
    }
}
