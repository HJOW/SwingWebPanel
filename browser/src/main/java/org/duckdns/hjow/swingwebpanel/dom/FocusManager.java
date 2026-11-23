package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The focus of a document (HTML Standard, "focus"): which elements can have the focus, the order of the Tab key, moving the focus
 * with the <code>blur</code>, <code>focusout</code>, <code>focus</code> and <code>focusin</code> events in the order of the UI Events
 * specification, and the effect that the focus of the window (the component) has on the event of the focused element. The judgment is made
 * from the DOM only (attributes, tree); elements that are not displayed are excluded by the caller that knows the layout.
 * <p>
 * 문서의 포커스(HTML 표준 "focus"): 포커스를 가질 수 있는 요소, Tab 키의 순서, UI Events 명세의 순서로 <code>blur</code>, <code>focusout</code>,
 * <code>focus</code>, <code>focusin</code> 이벤트를 보내며 포커스를 옮기는 일, 창(컴포넌트)의 포커스가 포커스를 가진 요소의 이벤트에 주는 영향.
 * 판정은 DOM(속성, 트리)만으로 하며, 표시되지 않는 요소는 레이아웃을 아는 호출자가 제외한다.
 */
public final class FocusManager {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FocusManager() {
    }

    /**
     * Returns the integer value of the <code>tabindex</code> attribute, or null if the attribute is missing or not an integer.
     * <p>
     * <code>tabindex</code> 속성의 정수 값을 반환한다. 속성이 없거나 정수가 아니면 null.
     *
     * @param e the element / 요소
     * @return the value or null / 값. 없으면 null
     */
    private static Integer tabIndexAttribute(Element e) {
        String v = e.getAttribute("tabindex");
        if (v == null) {
            return null;
        }
        String t = v.trim();
        try {
            return Integer.valueOf(t.startsWith("+") ? t.substring(1) : t);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Tells whether an element is focusable by itself: an element with a valid <code>tabindex</code>, a link with <code>href</code>, an
     * enabled form control (not <code>hidden</code> inputs), an <code>iframe</code>, an editable element or a <code>summary</code>; an
     * element that is disabled or has a <code>hidden</code> ancestor is not.
     * <p>
     * 요소가 스스로 포커스를 가질 수 있는지 알려 준다: 유효한 <code>tabindex</code> 가 있는 요소, <code>href</code> 가 있는 링크, 사용할 수 있는 폼
     * 컨트롤(<code>hidden</code> 입력은 제외), <code>iframe</code>, 편집 가능한 요소, <code>summary</code>. 비활성이거나 <code>hidden</code> 조상이 있는
     * 요소는 아니다.
     *
     * @param e the element / 요소
     * @return true if it can have the focus / 포커스를 가질 수 있으면 true
     */
    public static boolean isFocusable(Element e) {
        if (e == null || !e.isConnected() || !Namespaces.HTML.equals(e.getNamespaceURI())) {
            return false;
        }
        // hidden 속성을 가진 조상이 있거나 비활성 컨트롤이면 포커스를 가질 수 없다.
        for (Node n = e; n instanceof Element; n = n.getParentNode()) {
            if (((Element) n).hasAttribute("hidden")) {
                return false;
            }
        }
        String name = e.getLocalName();
        boolean control = "button".equals(name) || "input".equals(name) || "select".equals(name) || "textarea".equals(name);
        if (control && e.hasAttribute("disabled")) {
            return false;
        }
        if (isInDisabledFieldset(e)) {
            return false;
        }
        if ("input".equals(name) && "hidden".equalsIgnoreCase(e.getAttribute("type"))) {
            return false;
        }
        if (tabIndexAttribute(e) != null) {
            return true;
        }
        if (("a".equals(name) || "area".equals(name)) && e.hasAttribute("href")) {
            return true;
        }
        if (control || "iframe".equals(name) || "summary".equals(name)) {
            return true;
        }
        // 편집 호스트(바깥쪽 것)는 포커스를 가진다. 편집 호스트 안의 contenteditable 요소는 따로 포커스를 가지지 않는다.
        return org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts.isEditingHost(e) && org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts.hostOf(e) == e;
    }

    /**
     * Tells whether a form control sits inside a disabled <code>fieldset</code> (outside of its first <code>legend</code>).
     * <p>
     * 폼 컨트롤이 비활성 <code>fieldset</code> 안(첫 <code>legend</code> 밖)에 있는지 알려 준다.
     *
     * @param e the element / 요소
     * @return true if disabled by a fieldset / fieldset 때문에 비활성이면 true
     */
    private static boolean isInDisabledFieldset(Element e) {
        String name = e.getLocalName();
        if (!("button".equals(name) || "input".equals(name) || "select".equals(name) || "textarea".equals(name))) {
            return false;
        }
        // 조상 fieldset 중 disabled 인 것이 있으면, 컨트롤이 그 fieldset 의 첫 legend 안에 있는 경우만 예외이다.
        for (Node n = e.getParentNode(); n instanceof Element; n = n.getParentNode()) {
            Element p = (Element) n;
            if ("fieldset".equals(p.getLocalName()) && p.hasAttribute("disabled")) {
                Element legend = null;
                // fieldset 의 첫 legend 자식을 찾는다.
                for (Node c = p.getFirstChild(); c != null; c = c.getNextSibling()) {
                    if (c instanceof Element && "legend".equals(((Element) c).getLocalName())) {
                        legend = (Element) c;
                        break;
                    }
                }
                if (legend == null || !legend.contains(e)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the position of the element in the Tab order: the <code>tabindex</code> value, or 0 for an element that is focusable by
     * default, or -1 if it cannot be reached with the Tab key.
     * <p>
     * 요소의 Tab 순서 위치를 반환한다: <code>tabindex</code> 값, 기본으로 포커스를 가지는 요소는 0, Tab 키로 닿을 수 없으면 -1.
     *
     * @param e the element / 요소
     * @return the value / 값
     */
    public static int tabIndex(Element e) {
        if (!isFocusable(e)) {
            return -1;
        }
        Integer v = tabIndexAttribute(e);
        return v == null ? 0 : v.intValue();
    }

    /**
     * Returns the element that has the focus, or null if none has it.
     * <p>
     * 포커스를 가진 요소를 반환한다. 없으면 null.
     *
     * @param doc the document / 문서
     * @return the element or null / 요소. 없으면 null
     */
    public static Element getFocused(Document doc) {
        Element f = doc.focusedElement;
        if (f != null && (!f.isConnected() || f.getOwnerDocument() != doc)) {
            doc.focusedElement = null;
            return null;
        }
        return f;
    }

    /**
     * Gives the focus to an element if it can have it (the <code>focus()</code> method and the focus of a click). Events are sent in the
     * order <code>blur</code>, <code>focusout</code> to the old element and <code>focus</code>, <code>focusin</code> to the new one. The
     * events are trusted: the user agent sends them.
     * <p>
     * 요소가 포커스를 가질 수 있으면 포커스를 준다(<code>focus()</code> 메서드와 클릭의 포커스). 이벤트는 이전 요소에 <code>blur</code>,
     * <code>focusout</code>, 새 요소에 <code>focus</code>, <code>focusin</code> 순서로 보낸다. 사용자 에이전트가 보내므로 신뢰 이벤트이다.
     *
     * @param target the element / 요소
     * @param windowHasFocus whether the window (the component) has the focus, so that the events are sent / 창(컴포넌트)에 포커스가 있어 이벤트를 보낼지
     * @return true if the focus moved / 포커스가 옮겨졌으면 true
     */
    public static boolean focus(Element target, boolean windowHasFocus) {
        if (target == null || !isFocusable(target)) {
            return false;
        }
        Document doc = target.getOwnerDocument();
        Element old = getFocused(doc);
        if (old == target) {
            return false;
        }
        doc.focusedElement = target;
        if (windowHasFocus) {
            if (old != null) {
                send(old, "blur", false, target);
                send(old, "focusout", true, target);
            }
            send(target, "focus", false, old);
            send(target, "focusin", true, old);
        }
        return true;
    }

    /**
     * Removes the focus from an element if it has it (the <code>blur()</code> method).
     * <p>
     * 요소가 포커스를 가졌으면 포커스를 거둔다(<code>blur()</code> 메서드).
     *
     * @param e              the element / 요소
     * @param windowHasFocus whether the events are sent / 이벤트를 보낼지
     * @return true if the element had the focus / 요소가 포커스를 가지고 있었으면 true
     */
    public static boolean blur(Element e, boolean windowHasFocus) {
        Document doc = e.getOwnerDocument();
        if (getFocused(doc) != e) {
            return false;
        }
        doc.focusedElement = null;
        if (windowHasFocus) {
            send(e, "blur", false, null);
            send(e, "focusout", true, null);
        }
        return true;
    }

    /**
     * Tells the document that the window (the component) gained or lost the focus: the focused element gets <code>focus</code> and
     * <code>focusin</code> or <code>blur</code> and <code>focusout</code> but stays the focused element.
     * <p>
     * 창(컴포넌트)이 포커스를 얻거나 잃었음을 문서에 알린다. 포커스를 가진 요소는 <code>focus</code>, <code>focusin</code> 또는 <code>blur</code>,
     * <code>focusout</code> 을 받으며 포커스를 가진 요소로 남는다.
     *
     * @param doc    the document / 문서
     * @param gained true if the window gained the focus / 창이 포커스를 얻었으면 true
     */
    public static void windowFocusChanged(Document doc, boolean gained) {
        Element f = getFocused(doc);
        if (f == null) {
            return;
        }
        if (gained) {
            send(f, "focus", false, null);
            send(f, "focusin", true, null);
        } else {
            send(f, "blur", false, null);
            send(f, "focusout", true, null);
        }
    }

    /**
     * Sends one trusted focus event.
     * <p>
     * 신뢰 포커스 이벤트 하나를 보낸다.
     *
     * @param target  the target / 대상
     * @param type    the event type / 이벤트 종류
     * @param bubbles whether it bubbles / 버블링 여부
     * @param related the other element or null / 상대 요소. 없으면 null
     */
    private static void send(Element target, String type, boolean bubbles, Element related) {
        FocusEvent event = new FocusEvent(type, bubbles, false);
        event.setRelatedTarget(related);
        target.dispatchTrustedEvent(event);
    }

    /**
     * Finds the element that the Tab key (or Shift+Tab) moves the focus to: elements with a positive <code>tabindex</code> come first, in
     * the order of their values and then the document order, then the others in document order; elements with a negative
     * <code>tabindex</code> are skipped. Returns null after the last (or before the first) element, which is the moment to leave the
     * document.
     * <p>
     * Tab 키(또는 Shift+Tab)가 포커스를 옮길 요소를 찾는다: <code>tabindex</code> 가 양수인 요소가 값 순서와 문서 순서대로 먼저 오고, 그다음 나머지가
     * 문서 순서로 온다. <code>tabindex</code> 가 음수인 요소는 건너뛴다. 마지막 요소 다음(또는 첫 요소 이전)이면 null 이며, 문서를 떠날 때이다.
     *
     * @param doc     the document / 문서
     * @param from    the element that has the focus now, or null / 지금 포커스를 가진 요소. 없으면 null
     * @param forward true for Tab, false for Shift+Tab / Tab 이면 true, Shift+Tab 이면 false
     * @param visible tells whether an element is displayed, or null to accept all / 요소가 표시되는지 알려 준다. 모두 받으려면 null
     * @return the next element or null / 다음 요소. 없으면 null
     */
    public static Element next(Document doc, Element from, boolean forward, Visibility visible) {
        final List<Element> order = tabOrder(doc, visible);
        if (order.isEmpty()) {
            return null;
        }
        int index = from == null ? -1 : order.indexOf(from);
        if (index < 0 && from != null) {
            // 포커스를 가진 요소가 순서에 없으면(예: tabindex=-1) 문서 순서로 가장 가까운 앞뒤 요소를 찾는다.
            return neighbor(doc, from, forward, order);
        }
        int target = forward ? index + 1 : (index < 0 ? order.size() - 1 : index - 1);
        return target < 0 || target >= order.size() ? null : order.get(target);
    }

    /**
     * Tells whether an element is displayed (the layout side knows; the DOM does not).
     * <p>
     * 요소가 표시되는지 알려 준다(레이아웃 쪽은 알고 DOM 은 모른다).
     */
    public interface Visibility {
        /**
         * Returns whether the element is displayed.
         * <p>
         * 요소가 표시되는지 반환한다.
         *
         * @param e the element / 요소
         * @return true if displayed / 표시되면 true
         */
        boolean isDisplayed(Element e);
    }

    /**
     * Returns the elements that the Tab key visits, in order.
     * <p>
     * Tab 키가 방문하는 요소를 순서대로 반환한다.
     *
     * @param doc     the document / 문서
     * @param visible the visibility test or null / 표시 판정. 없으면 null
     * @return the list / 목록
     */
    private static List<Element> tabOrder(Document doc, Visibility visible) {
        final List<Element> all = new ArrayList<Element>();
        Element root = doc.getDocumentElement();
        // 문서 순서로 모든 요소를 돌며 Tab 으로 닿는 요소를 모은다.
        for (Node n = root; n != null; n = n.nextInTree(n, root)) {
            if (n instanceof Element && tabIndex((Element) n) >= 0 && (visible == null || visible.isDisplayed((Element) n))) {
                all.add((Element) n);
            }
        }
        final List<Element> positive = new ArrayList<Element>();
        final List<Element> natural = new ArrayList<Element>();
        // tabindex 가 양수인 요소와 그렇지 않은 요소를 나눈다.
        for (Element e : all) {
            (tabIndex(e) > 0 ? positive : natural).add(e);
        }
        // 양수끼리는 값 순서로 정렬한다(안정 정렬이므로 같은 값은 문서 순서가 유지된다).
        Collections.sort(positive, new Comparator<Element>() {
            /**
             * Compares the <code>tabindex</code> values.
             * <p>
             * <code>tabindex</code> 값을 비교한다.
             *
             * @param a first element / 첫 요소
             * @param b second element / 둘째 요소
             * @return the order / 순서
             */
            public int compare(Element a, Element b) {
                return tabIndex(a) - tabIndex(b);
            }
        });
        positive.addAll(natural);
        return positive;
    }

    /**
     * Finds the nearest element in the Tab order before or after an element that is not in the order itself.
     * <p>
     * 순서에 없는 요소의 앞이나 뒤에서 Tab 순서의 가장 가까운 요소를 찾는다.
     *
     * @param doc     the document / 문서
     * @param from    the element that is not in the order / 순서에 없는 요소
     * @param forward the direction / 방향
     * @param order   the Tab order / Tab 순서
     * @return the element or null / 요소. 없으면 null
     */
    private static Element neighbor(Document doc, Element from, boolean forward, List<Element> order) {
        Element best = null;
        // 문서 순서상 from 뒤(또는 앞)에 오는 첫(또는 마지막) 순서 요소를 찾는다.
        for (Element e : order) {
            boolean after = (from.compareDocumentPosition(e) & Node.DOCUMENT_POSITION_FOLLOWING) != 0;
            if (forward && after) {
                return e;
            }
            if (!forward && !after) {
                best = e;
            }
        }
        return forward ? null : best;
    }
}
