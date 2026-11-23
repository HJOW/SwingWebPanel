package org.duckdns.hjow.swingwebpanel.dom.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The live <code>labels</code> list of a labelable element: the <code>label</code> elements, in tree order, whose labeled control is the element.
 * <p>
 * 라벨을 붙일 수 있는 요소의 라이브 <code>labels</code> 목록: labeled control 이 그 요소인 <code>label</code> 요소(트리 순서).
 */
final class LabelList extends FormElementList {

    /** The labeled element. / 라벨이 붙은 요소. */
    private final HtmlControl control;

    /**
     * Creates the list of an element.
     * <p>
     * 요소의 목록을 만든다.
     *
     * @param control the labelable element / 라벨을 붙일 수 있는 요소
     */
    LabelList(HtmlControl control) {
        super(control.document());
        this.control = control;
    }

    /**
     * Collects the labels in the tree that contains the element.
     * <p>
     * 요소가 속한 트리에서 label 을 모은다.
     *
     * @return the labels / label 들
     */
    protected List<Element> compute() {
        List<Element> out = new ArrayList<Element>();
        Node root = control.getRootNode();
        // 트리의 모든 노드를 훑으며 labeled control 이 이 요소인 label 을 모은다.
        for (Node n = Node.nextInTree(root, root); n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof HtmlLabelElement && ((HtmlLabelElement) n).getControl() == control) {
                out.add((Element) n);
            }
        }
        return out;
    }
}
