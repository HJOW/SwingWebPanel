package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementFactory;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;

/**
 * The default element factory of documents: it creates the specialized classes of the HTML form elements (<code>form</code>, <code>input</code>,
 * <code>button</code>, <code>select</code>, <code>option</code>, <code>optgroup</code>, <code>textarea</code>, <code>label</code>, <code>fieldset</code>,
 * <code>legend</code>, <code>datalist</code>, <code>output</code>, <code>progress</code>, <code>meter</code>) for the HTML namespace and
 * {@link HtmlGenericElement} for everything else. Names are matched in lower case, as the HTML parser and <code>createElement</code> of an HTML
 * document produce them; an XML document keeps the case, so <code>&lt;INPUT&gt;</code> of an XHTML document is a plain element.
 * <p>
 * 문서의 기본 요소 팩터리: HTML 네임스페이스의 HTML 폼 요소(<code>form</code>, <code>input</code>, <code>button</code>, <code>select</code>,
 * <code>option</code>, <code>optgroup</code>, <code>textarea</code>, <code>label</code>, <code>fieldset</code>, <code>legend</code>, <code>datalist</code>,
 * <code>output</code>, <code>progress</code>, <code>meter</code>)에는 특화된 클래스를, 그 밖의 모든 것에는 {@link HtmlGenericElement} 를 만든다. 이름은
 * HTML 파서와 HTML 문서의 <code>createElement</code> 가 만드는 대로 소문자로 비교한다. XML 문서는 대소문자를 유지하므로 XHTML 문서의
 * <code>&lt;INPUT&gt;</code> 은 일반 요소이다.
 */
public final class HtmlElementFactory implements ElementFactory {

    /** The shared instance (the factory has no state). / 공유 인스턴스(팩터리는 상태가 없다). */
    public static final HtmlElementFactory INSTANCE = new HtmlElementFactory();

    /** Use {@link #INSTANCE}; the factory has no state, so nobody needs another one. / {@link #INSTANCE} 를 쓴다. 팩터리는 상태가 없어 다른 인스턴스가 필요 없다. */
    private HtmlElementFactory() {
    }

    /**
     * Creates an element.
     * <p>
     * 요소를 만든다.
     *
     * @param document     owner document / 소유 문서
     * @param namespaceURI namespace or null / 네임스페이스 또는 null
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name / 로컬 이름
     * @return the new element / 새 요소
     */
    public Element createElement(Document document, String namespaceURI, String prefix, String localName) {
        if (Namespaces.HTML.equals(namespaceURI)) {
            Element e = createHtml(document, prefix, localName);
            if (e != null) {
                return e;
            }
        }
        return new HtmlGenericElement(document, namespaceURI, prefix, localName);
    }

    /**
     * Creates the specialized element of an HTML element name.
     * <p>
     * HTML 요소 이름의 특화된 요소를 만든다.
     *
     * @param d      document / 문서
     * @param prefix prefix or null / 접두사 또는 null
     * @param name   local name / 로컬 이름
     * @return the element, or null if the name has no specialized class / 요소. 특화된 클래스가 없는 이름이면 null
     */
    private static Element createHtml(Document d, String prefix, String name) {
        if ("input".equals(name)) {
            return new HtmlInputElement(d, prefix, name);
        } else if ("form".equals(name)) {
            return new HtmlFormElement(d, prefix, name);
        } else if ("button".equals(name)) {
            return new HtmlButtonElement(d, prefix, name);
        } else if ("select".equals(name)) {
            return new HtmlSelectElement(d, prefix, name);
        } else if ("option".equals(name)) {
            return new HtmlOptionElement(d, prefix, name);
        } else if ("optgroup".equals(name)) {
            return new HtmlOptGroupElement(d, prefix, name);
        } else if ("textarea".equals(name)) {
            return new HtmlTextAreaElement(d, prefix, name);
        } else if ("label".equals(name)) {
            return new HtmlLabelElement(d, prefix, name);
        } else if ("fieldset".equals(name)) {
            return new HtmlFieldSetElement(d, prefix, name);
        } else if ("legend".equals(name)) {
            return new HtmlLegendElement(d, prefix, name);
        } else if ("datalist".equals(name)) {
            return new HtmlDataListElement(d, prefix, name);
        } else if ("output".equals(name)) {
            return new HtmlOutputElement(d, prefix, name);
        } else if ("progress".equals(name)) {
            return new HtmlProgressElement(d, prefix, name);
        } else if ("meter".equals(name)) {
            return new HtmlMeterElement(d, prefix, name);
        }
        return null;
    }
}
