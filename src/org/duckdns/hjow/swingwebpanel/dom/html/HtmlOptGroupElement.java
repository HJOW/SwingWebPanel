package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * The <code>optgroup</code> element (HTML Standard 4.10.9): a labelled group of the options of a <code>select</code>. Its
 * <code>disabled</code> attribute disables the options inside it.
 * <p>
 * <code>optgroup</code> 요소(HTML 표준 4.10.9): <code>select</code> 의 option 을 묶는 이름 있는 그룹. <code>disabled</code> 속성은 안의 option 을 비활성으로 만든다.
 */
public final class HtmlOptGroupElement extends HtmlControl {

    /**
     * Creates an optgroup.
     * <p>
     * optgroup 을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlOptGroupElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Tells whether the group is disabled (the <code>disabled</code> attribute).
     * <p>
     * 그룹이 비활성인지(<code>disabled</code> 속성) 알려 준다.
     *
     * @return true if disabled / 비활성이면 true
     */
    public boolean isDisabled() {
        return hasAttribute("disabled");
    }
}
