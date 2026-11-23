package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>legend</code> element (HTML Standard 4.10.16). Its <code>form</code> is the form owner of the <code>fieldset</code> that is its parent.
 * <p>
 * <code>legend</code> 요소(HTML 표준 4.10.16). <code>form</code> 은 부모인 <code>fieldset</code> 의 폼 소유자이다.
 */
public final class HtmlLegendElement extends HtmlControl {

    /**
     * Creates a legend.
     * <p>
     * legend 를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlLegendElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Returns the form owner of the parent fieldset.
     * <p>
     * 부모 fieldset 의 폼 소유자를 반환한다.
     *
     * @return the form or null / 폼 또는 null
     */
    public HtmlFormElement getForm() {
        Node p = getParentNode();
        return p instanceof HtmlFieldSetElement ? ((HtmlFieldSetElement) p).getForm() : null;
    }
}
