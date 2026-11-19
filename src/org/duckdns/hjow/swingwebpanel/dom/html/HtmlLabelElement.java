package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The <code>label</code> element (HTML Standard 4.10.4). Its labeled control is the labelable element that the <code>for</code> attribute
 * names (an element of the same tree that is labelable), or, without that attribute, its first labelable descendant. Activating a label
 * (a click that is not on interactive content inside it) focuses and clicks the labeled control.
 * <p>
 * <code>label</code> 요소(HTML 표준 4.10.4). labeled control 은 <code>for</code> 속성이 가리키는, 같은 트리에서 라벨을 붙일 수 있는 요소이며, 속성이 없으면 첫 번째
 * 라벨을 붙일 수 있는 후손이다. label 을 활성화하면(안의 상호작용 콘텐츠가 아닌 곳의 클릭) labeled control 에 포커스를 주고 클릭한다.
 */
public final class HtmlLabelElement extends HtmlControl {

    /**
     * Creates a label.
     * <p>
     * label 을 만든다.
     *
     * @param owner     owner document / 소유 문서
     * @param prefix    prefix or null / 접두사 또는 null
     * @param localName local name / 로컬 이름
     */
    HtmlLabelElement(Document owner, String prefix, String localName) {
        super(owner, Namespaces.HTML, prefix, localName);
    }

    /**
     * Returns the labeled control.
     * <p>
     * labeled control 을 반환한다.
     *
     * @return the control or null / 컨트롤 또는 null
     */
    public Element getControl() {
        String f = getAttribute("for");
        if (f != null) {
            Element e = HtmlControl.findById(this, f);
            return e instanceof HtmlControl && ((HtmlControl) e).isLabelable() ? e : null;
        }
        // for 속성이 없으면 첫 번째 라벨 가능한 후손을 찾는다.
        for (Node n = Node.nextInTree(this, this); n != null; n = Node.nextInTree(n, this)) {
            if (n instanceof HtmlControl && ((HtmlControl) n).isLabelable()) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Returns the form owner of the labeled control.
     * <p>
     * labeled control 의 폼 소유자를 반환한다.
     *
     * @return the form or null / 폼 또는 null
     */
    public HtmlFormElement getForm() {
        Element c = getControl();
        return c instanceof HtmlListedElement ? ((HtmlListedElement) c).getForm() : null;
    }

    /**
     * A label has an activation behavior.
     * <p>
     * label 에는 활성화 동작이 있다.
     *
     * @return true / true
     */
    boolean hasActivationBehavior() {
        return true;
    }

    /**
     * Focuses the labeled control (if it can have the focus) and sends it a click.
     * <p>
     * labeled control 에 (포커스를 가질 수 있으면) 포커스를 주고 click 을 보낸다.
     *
     * @param click the click that activated the label / label 을 활성화한 click
     */
    void activate(Event click) {
        Element c = getControl();
        if (c == null) {
            return;
        }
        FocusManager.focus(c, document().isWindowFocused());
        FormActivation.click(c, click.isTrusted());
    }
}
