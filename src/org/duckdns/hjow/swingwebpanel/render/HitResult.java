package org.duckdns.hjow.swingwebpanel.render;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;

/**
 * The result of a hit test (plan item P09-06): the deepest layout box at a point, the DOM element it belongs to, the nearest enclosing link
 * with its absolute URL and, over text, the text node with the character offset. Instances are immutable.
 * <p>
 * 히트 테스트 결과(계획 P09-06): 점 위의 가장 깊은 레이아웃 상자, 그것이 속한 DOM 요소, 가장 가까운 링크와 그 절대 URL, 텍스트 위라면 텍스트 노드와
 * 문자 오프셋. 인스턴스는 불변이다.
 */
public final class HitResult {

    /**
     * The deepest box.
     * <p>
     * 가장 깊은 상자.
     */
    private final LayoutBox box;

    /**
     * The element of the hit (the parent element for text).
     * <p>
     * 히트한 요소(텍스트이면 부모 요소).
     */
    private final Element element;

    /**
     * The nearest enclosing <code>a</code>/<code>area</code> element with an <code>href</code>, or null.
     * <p>
     * <code>href</code> 가 있는 가장 가까운 <code>a</code>/<code>area</code> 요소. 없으면 null.
     */
    private final Element link;

    /**
     * The absolute URL of the link or null.
     * <p>
     * 링크의 절대 URL 또는 null.
     */
    private final String href;

    /**
     * The text node under the point or null.
     * <p>
     * 점 아래의 텍스트 노드 또는 null.
     */
    private final Node textNode;

    /**
     * The character offset in the data of the text node, or -1.
     * <p>
     * 텍스트 노드 데이터 안의 문자 오프셋. 없으면 -1.
     */
    private final int textOffset;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param box        the deepest box or null / 가장 깊은 상자 또는 null
     * @param element    the element or null / 요소 또는 null
     * @param link       the link element or null / 링크 요소 또는 null
     * @param href       the absolute URL or null / 절대 URL 또는 null
     * @param textNode   the text node or null / 텍스트 노드 또는 null
     * @param textOffset the offset or -1 / 오프셋 또는 -1
     */
    HitResult(LayoutBox box, Element element, Element link, String href, Node textNode, int textOffset) {
        this.box = box;
        this.element = element;
        this.link = link;
        this.href = href;
        this.textNode = textNode;
        this.textOffset = textOffset;
    }

    /**
     * Returns the deepest layout box at the point.
     * <p>
     * 점 위의 가장 깊은 레이아웃 상자를 반환한다.
     *
     * @return the box, or null if the point is outside the document / 상자. 점이 문서 밖이면 null
     */
    public LayoutBox getBox() {
        return box;
    }

    /**
     * Returns the element at the point (the parent element when the point is over text).
     * <p>
     * 점 위의 요소를 반환한다(텍스트 위이면 부모 요소).
     *
     * @return the element or null / 요소 또는 null
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the nearest enclosing link element.
     * <p>
     * 가장 가까운 링크 요소를 반환한다.
     *
     * @return the <code>a</code> or <code>area</code> element, or null / <code>a</code> 또는 <code>area</code> 요소. 없으면 null
     */
    public Element getLink() {
        return link;
    }

    /**
     * Returns the absolute URL of the link (the <code>href</code> resolved against the base URL of the document).
     * <p>
     * 링크의 절대 URL 을 반환한다(<code>href</code> 를 문서의 기준 URL 로 해석한 값).
     *
     * @return the URL or null / URL 또는 null
     */
    public String getHref() {
        return href;
    }

    /**
     * Returns the text node under the point.
     * <p>
     * 점 아래의 텍스트 노드를 반환한다.
     *
     * @return the text node or null / 텍스트 노드 또는 null
     */
    public Node getTextNode() {
        return textNode;
    }

    /**
     * Returns the character offset of the point in the data of the text node (the caret position nearest to the point).
     * <p>
     * 점의 문자 오프셋을 텍스트 노드 데이터 안에서 반환한다(점에 가장 가까운 캐럿 위치).
     *
     * @return the offset, -1 if the point is not over text / 오프셋. 텍스트 위가 아니면 -1
     */
    public int getTextOffset() {
        return textOffset;
    }
}
