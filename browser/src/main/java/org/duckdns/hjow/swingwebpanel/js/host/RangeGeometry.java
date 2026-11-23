package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * The geometry methods of <code>Range</code> (CSSOM View: <code>getBoundingClientRect()</code> and <code>getClientRects()</code>), which need
 * the layout of the page and are therefore added by the host. The layout of this component is known per element (the border box of an
 * element in viewport pixels, see {@link HostEnvironment#getElementBounds}). When the host knows the real geometry of the text (see {@link HostEnvironment#getRangeRects}) the rectangles are the exact glyph rectangles, one per line. Without it (a host that has no layout) the rectangles are an approximation: an
 * element that the range covers completely gives its own box, and text that the range covers (completely or partly) gives the box of the
 * element that holds the text. A range that is collapsed has no rectangle. The result is a list of <code>DOMRect</code>-like objects; the
 * bounding rectangle is their union (all zero for an empty list).
 * <p>
 * <code>Range</code> 의 기하 메서드(CSSOM View: <code>getBoundingClientRect()</code>, <code>getClientRects()</code>). 페이지의 레이아웃이 필요하므로 호스트가
 * 더한다. 이 컴포넌트의 레이아웃은 텍스트 줄 단위가 아니라 요소 단위로 알려져 있으므로({@link HostEnvironment#getElementBounds} 의 뷰포트 픽셀
 * 테두리 상자) 사각형은 근사값이다: 범위가 완전히 덮는 요소는 자기 상자를, 범위가 덮는(전부 또는 일부) 텍스트는 그 텍스트를 가진 요소의 상자를
 * 준다. 접힌 범위는 사각형이 없다. 결과는 <code>DOMRect</code> 와 같은 객체의 목록이며 경계 사각형은 그 합집합이다(목록이 비면 모두 0).
 */
final class RangeGeometry {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    RangeGeometry(PageScripts page) {
        this.page = page;
    }

    /**
     * Builds a <code>DOMRect</code>.
     * <p>
     * <code>DOMRect</code> 객체를 만든다.
     *
     * @param b the bounds x, y, width, height / 경계 x, y, 폭, 높이
     * @return the object / 객체
     */
    private JsValue rect(double[] b) {
        return page.getBridge().wrap(new org.duckdns.hjow.swingwebpanel.dom.DomRect(b[0], b[1], b[2], b[3]));
    }

    /**
     * Installs the two methods on <code>Range.prototype</code>.
     * <p>
     * <code>Range.prototype</code> 에 두 메서드를 설치한다.
     */
    void install() {
        JsObject proto = page.getBridge().getInterface("Range").getPrototype();
        proto.defineData("getBoundingClientRect", page.getRealm().newFunction("getBoundingClientRect", 0, new NativeFunction.Call() {
            /**
             * Returns the union of the rectangles of the range.
             * <p>
             * 범위의 사각형의 합집합을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the rectangle / 사각형
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                List<double[]> rects = rectangles(rangeOf(self));
                if (rects.isEmpty()) {
                    return rect(new double[4]);
                }
                double[] u = null;
                // 면적이 있는(너비와 높이가 모두 0 이 아닌) 사각형만 합집합에 넣고, 모두 너비나 높이가 0 이면 첫 사각형을 그대로 쓴다(CSSOM View).
                for (int i = 0; i < rects.size(); i++) {
                    double[] b = rects.get(i);
                    if (b[2] == 0 || b[3] == 0) {
                        continue;
                    }
                    if (u == null) {
                        u = b.clone();
                        continue;
                    }
                    double right = Math.max(u[0] + u[2], b[0] + b[2]);
                    double bottom = Math.max(u[1] + u[3], b[1] + b[3]);
                    u[0] = Math.min(u[0], b[0]);
                    u[1] = Math.min(u[1], b[1]);
                    u[2] = right - u[0];
                    u[3] = bottom - u[1];
                }
                if (u == null) {
                    return rect(rects.get(0));
                }
                return rect(u);
            }
        }), true, true, true);
        JsObject selectionProto = page.getBridge().getInterface("Selection").getPrototype();
        selectionProto.defineData("toString", page.getRealm().newFunction("toString", 0, new NativeFunction.Call() {
            /**
             * Returns the selected text the way copying it gives it (rendered text, Selection API: "the string that the user agent would
             * produce when copying"); the raw character data of the range if the host has no layout.
             * <p>
             * 선택된 텍스트를 복사했을 때 나오는 모습으로 반환한다(렌더링된 텍스트. Selection API: "사용자 에이전트가 복사할 때 만드는 문자열"). 호스트에 레이아웃이
             * 없으면 범위의 원본 문자 데이터.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 텍스트
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                org.duckdns.hjow.swingwebpanel.dom.Selection selection = (org.duckdns.hjow.swingwebpanel.dom.Selection) DomBridge.targetOf(self, org.duckdns.hjow.swingwebpanel.dom.Selection.class);
                if (selection == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                if (selection.getRangeCount() == 0 || selection.isCollapsed()) {
                    return Js.str("");
                }
                String text = page.getEnvironment().getRangeText(selection.getRangeAt(0));
                return Js.str(text != null ? text : selection.toString());
            }
        }), true, true, true);
        proto.defineData("getClientRects", page.getRealm().newFunction("getClientRects", 0, new NativeFunction.Call() {
            /**
             * Returns the rectangles of the range as an array.
             * <p>
             * 범위의 사각형을 배열로 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the rectangles / 사각형 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                List<double[]> rects = rectangles(rangeOf(self));
                java.util.List<org.duckdns.hjow.swingwebpanel.dom.DomRect> list = new java.util.ArrayList<org.duckdns.hjow.swingwebpanel.dom.DomRect>();
                // 사각형마다 DOMRect 를 만들어 목록에 담는다.
                for (int i = 0; i < rects.size(); i++) {
                    double[] b = rects.get(i);
                    list.add(new org.duckdns.hjow.swingwebpanel.dom.DomRect(b[0], b[1], b[2], b[3]));
                }
                return page.getBridge().wrap(new org.duckdns.hjow.swingwebpanel.dom.DomRectList(list));
            }
        }), true, true, true);
    }

    /**
     * Returns the range of a <code>this</code> value.
     * <p>
     * <code>this</code> 값의 범위를 반환한다.
     *
     * @param self this value / this 값
     * @return the range / 범위
     */
    private static Range rangeOf(JsValue self) {
        Range range = (Range) DomBridge.targetOf(self, Range.class);
        if (range == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return range;
    }

    /**
     * Collects the rectangles of a range (see the class description for what they stand for).
     * <p>
     * 범위의 사각형을 모은다(무엇을 뜻하는지는 클래스 설명 참조).
     *
     * @param range the range / 범위
     * @return the rectangles as x, y, width, height / x, y, 폭, 높이 사각형
     */
    private List<double[]> rectangles(Range range) {
        List<double[]> out = new ArrayList<double[]>();
        double[][] real = page.getEnvironment().getRangeRects(range);
        // 호스트가 실제 글자 기하를 알려 주면 그것을 쓴다.
        if (real != null) {
            for (int i = 0; i < real.length; i++) {
                out.add(real[i]);
            }
            return out;
        }
        if (range.isCollapsed()) {
            return out;
        }
        IdentityHashMap<Element, Boolean> seen = new IdentityHashMap<Element, Boolean>();
        Node root = range.getCommonAncestorContainer();
        Node n = root;
        // 공통 조상 아래를 트리 순서로 훑는다. 완전히 덮인 요소는 자기 상자를 주고 그 안쪽은 건너뛴다.
        while (n != null) {
            boolean covered = false;
            if (n instanceof Element && n != root && covers(range, n)) {
                add(out, seen, (Element) n);
                covered = true;
            } else if (n instanceof org.duckdns.hjow.swingwebpanel.dom.Text && range.intersectsNode(n) && n.getParentNode() instanceof Element) {
                // 범위가 닿는 텍스트는 그 텍스트를 가진 요소의 상자를 준다.
                add(out, seen, (Element) n.getParentNode());
            }
            n = covered ? skipChildren(n, root) : Node.nextInTree(n, root);
        }
        return out;
    }

    /**
     * Tells whether the range covers a node completely (the node lies between the boundary points, touching them is allowed).
     * <p>
     * 범위가 노드를 완전히 덮는지 알려 준다(노드가 경계점 사이에 있으며 경계점에 닿아도 된다).
     *
     * @param range the range / 범위
     * @param node  the node, with a parent / 노드(부모가 있다)
     * @return true if covered / 덮이면 true
     */
    private static boolean covers(Range range, Node node) {
        Node parent = node.getParentNode();
        if (parent == null) {
            return false;
        }
        int index = 0;
        // 부모 안에서 노드의 위치를 센다.
        for (Node s = node.getPreviousSibling(); s != null; s = s.getPreviousSibling()) {
            index++;
        }
        return range.comparePoint(parent, index) >= 0 && range.comparePoint(parent, index + 1) <= 0;
    }

    /**
     * Returns the node that follows a node and its descendants inside a root.
     * <p>
     * 루트 안에서 노드와 그 후손 다음에 오는 노드를 반환한다.
     *
     * @param node the node / 노드
     * @param root the root / 루트
     * @return the next node or null / 다음 노드 또는 null
     */
    private static Node skipChildren(Node node, Node root) {
        Node c = node;
        // 다음 형제가 있는 노드를 찾을 때까지 위로 올라간다.
        while (c != null && c != root) {
            if (c.getNextSibling() != null) {
                return c.getNextSibling();
            }
            c = c.getParentNode();
        }
        return null;
    }

    /**
     * Adds the box of an element once, unless it is empty.
     * <p>
     * 요소의 상자를 한 번만 더한다(비어 있으면 더하지 않는다).
     *
     * @param out     the list / 목록
     * @param seen    the elements added already / 이미 더한 요소
     * @param element the element / 요소
     */
    private void add(List<double[]> out, IdentityHashMap<Element, Boolean> seen, Element element) {
        if (seen.containsKey(element)) {
            return;
        }
        seen.put(element, Boolean.TRUE);
        double[] b = page.getEnvironment().getElementBounds(element);
        if (b != null && (b[2] > 0 || b[3] > 0)) {
            out.add(b);
        }
    }
}
