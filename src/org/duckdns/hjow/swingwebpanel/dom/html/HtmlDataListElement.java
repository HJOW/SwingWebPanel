package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>datalist</code> element (HTML Standard 4.10.10): a set of suggested values for an <code>input</code>. The controls inside it
 * are barred from constraint validation and not submitted. Its <code>options</code> collection holds the <code>option</code> descendants.
 * <p>
 * <code>datalist</code> 요소(HTML 표준 4.10.10): <code>input</code> 의 제안 값 집합. 안의 컨트롤은 제약 검증에서 제외되며 제출되지 않는다.
 * <code>options</code> 컬렉션은 <code>option</code> 후손을 가진다.
 */
public final class HtmlDataListElement extends HtmlControl {

    /** The cached options collection. / 캐시한 options 컬렉션. */
    private FormElementList options;

    /**
     * Creates a datalist.
     * <p>
     * datalist 를 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlDataListElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Returns the live <code>options</code> collection: the <code>option</code> descendants in tree order.
     * <p>
     * 라이브 <code>options</code> 컬렉션을 반환한다: 트리 순서의 <code>option</code> 후손.
     *
     * @return the same collection every time / 매번 같은 컬렉션
     */
    public FormElementList getOptions() {
        if (options == null) {
            options = new FormElementList(document()) {
                /**
                 * Collects the option descendants.
                 * <p>
                 * option 후손을 모은다.
                 *
                 * @return the options / option 들
                 */
                protected List<Element> compute() {
                    List<Element> out = new ArrayList<Element>();
                    // 후손을 트리 순서로 훑으며 option 을 모은다.
                    for (Node n = Node.nextInTree(HtmlDataListElement.this, HtmlDataListElement.this); n != null;
                            n = Node.nextInTree(n, HtmlDataListElement.this)) {
                        if (n instanceof HtmlOptionElement) {
                            out.add((Element) n);
                        }
                    }
                    return out;
                }
            };
        }
        return options;
    }
}
