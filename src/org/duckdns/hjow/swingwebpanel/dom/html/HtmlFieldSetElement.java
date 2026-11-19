package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>fieldset</code> element (HTML Standard 4.10.15). A fieldset is a listed element that is barred from constraint validation. Its
 * <code>disabled</code> attribute disables the form controls inside it, except those inside its first <code>legend</code> child (see
 * {@link HtmlListedElement#isDisabled()}); this class provides that first legend and the <code>elements</code> collection.
 * <p>
 * <code>fieldset</code> 요소(HTML 표준 4.10.15). fieldset 은 목록에 오르는 요소이며 제약 검증에서 제외된다. <code>disabled</code> 속성은 안의 폼 컨트롤을
 * 비활성으로 만들고, 첫 <code>legend</code> 자식 안의 컨트롤만 예외이다({@link HtmlListedElement#isDisabled()} 참조). 이 클래스는 그 첫 legend 와
 * <code>elements</code> 컬렉션을 제공한다.
 */
public final class HtmlFieldSetElement extends HtmlListedElement {

    /** The cached elements collection. / 캐시한 elements 컬렉션. */
    private FormElementList elements;

    /**
     * Creates a fieldset.
     * <p>
     * fieldset 을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlFieldSetElement(Document owner, String prefix, String localName) {
        super(owner, org.duckdns.hjow.swingwebpanel.dom.Namespaces.HTML, prefix, localName);
    }

    /**
     * A fieldset is never validated.
     * <p>
     * fieldset 은 검증 대상이 아니다.
     *
     * @return true / true
     */
    boolean isBarredBySelf() {
        return true;
    }

    /**
     * Returns the first <code>legend</code> child, or null.
     * <p>
     * 첫 <code>legend</code> 자식을 반환한다. 없으면 null.
     *
     * @return the legend or null / legend 또는 null
     */
    public Element getFirstLegend() {
        // 자식 중 처음 만나는 legend 를 찾는다.
        for (Node c = getFirstChild(); c != null; c = c.getNextSibling()) {
            if (HtmlControl.isHtml(c, "legend")) {
                return (Element) c;
            }
        }
        return null;
    }

    /**
     * Returns the <code>type</code> IDL attribute: <code>fieldset</code>.
     * <p>
     * <code>type</code> IDL 속성을 반환한다: <code>fieldset</code>.
     *
     * @return "fieldset" / "fieldset"
     */
    public String getType() {
        return "fieldset";
    }

    /**
     * Returns the live <code>elements</code> collection: the listed elements that are descendants of the fieldset.
     * <p>
     * 라이브 <code>elements</code> 컬렉션을 반환한다: fieldset 의 후손인 목록 요소.
     *
     * @return the same collection every time / 매번 같은 컬렉션
     */
    public FormElementList getElements() {
        if (elements == null) {
            elements = new FormElementList(document()) {
                /**
                 * Collects the listed descendants.
                 * <p>
                 * 목록에 오르는 후손을 모은다.
                 *
                 * @return the elements / 요소
                 */
                protected List<Element> compute() {
                    List<Element> out = new ArrayList<Element>();
                    // 후손을 트리 순서로 훑으며 목록 요소를 모은다.
                    for (Node n = Node.nextInTree(HtmlFieldSetElement.this, HtmlFieldSetElement.this); n != null;
                            n = Node.nextInTree(n, HtmlFieldSetElement.this)) {
                        if (n instanceof HtmlListedElement) {
                            out.add((Element) n);
                        }
                    }
                    return out;
                }
            };
        }
        return elements;
    }
}
