package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * "Align descendants" of the legacy <code>center</code> element and <code>align</code> attributes (plan item P07-07-b-5).
 * HTML5 2014 section 10.2 (definition of aligning descendants), 10.3.3 (flow content) and 10.3.9 (tables) say that <code>center</code>, <code>div[align]</code>,
 * <code>caption</code> and the table parts <code>thead</code>, <code>tbody</code>, <code>tfoot</code>, <code>tr</code>, <code>td</code> and
 * <code>th</code> with an <code>align</code> attribute not only align their text but also align their descendants: only those descendants
 * whose <code>margin-left</code> and <code>margin-right</code> do not compute to <code>auto</code>, that are over-constrained (CSS 2.1
 * 10.3.3) with one margin forced to a greater used value, and that have no applicable <code>align</code> attribute themselves. The most
 * deeply nested aligning element wins. The aligned box gets the used values of its margins set accordingly: left keeps the box at the
 * start (the forced margin stays on the right), center splits the free space around the margin box (as other browsers do) and right
 * moves all of it to the left margin. This class only decides the alignment of an ancestor chain; {@link BlockLayouter#resolveMargins}
 * applies it. It is independent of <code>text-align</code>, so author CSS that changes the text alignment does not cancel it. Direction
 * is not considered: the engine forces the right margin (left-to-right). Source:
 * https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html (2014-10-28, retrieved 2026-11-17).
 * <p>
 * 이전 방식의 <code>center</code> 요소와 <code>align</code> 속성이 가지는 "자손 정렬"(계획 P07-07-b-5). HTML5 2014 10.2(자손 정렬의 정의), 10.3.3(플로 콘텐츠), 10.3.9(표)에 따르면
 * <code>center</code>, <code>div[align]</code>, <code>caption</code>과 <code>align</code> 속성이 있는 표 부품(<code>thead</code>, <code>tbody</code>,
 * <code>tfoot</code>, <code>tr</code>, <code>td</code>, <code>th</code>)은 글뿐 아니라 자손도 정렬한다: <code>margin-left</code>와
 * <code>margin-right</code>가 <code>auto</code>가 아니고, 제약이 넘쳐(CSS 2.1 10.3.3) 한쪽 margin 의 사용 값이 더 크게 강제되며, 자신은 적용 가능한
 * <code>align</code> 속성이 없는 자손만 정렬한다. 가장 깊게 중첩된 정렬 요소가 이긴다. 정렬된 상자는 margin 의 사용 값을 그에 맞게 정한다: left 는
 * 상자를 시작 쪽에 두고(강제된 margin 은 오른쪽에 남는다), center 는 다른 브라우저처럼 margin 상자 주위로 남는 공간을 나누며, right 는 모두 왼쪽
 * margin 으로 옮긴다. 이 클래스는 조상 사슬의 정렬만 판정하고 적용은 {@link BlockLayouter#resolveMargins} 가 한다. <code>text-align</code>과는 독립이므로
 * 글 정렬을 바꾸는 작성자 CSS가 이 정렬을 취소하지 않는다. 글쓰기 방향은 고려하지 않는다: 엔진이 오른쪽 margin 을 강제한다(왼쪽에서 오른쪽).
 */
final class AlignDescendants {

    /** No alignment of descendants. 자손 정렬 없음. */
    static final int NONE = 0;
    /** Align descendants to the left (the start side). 자손을 왼쪽(시작 쪽)으로 정렬. */
    static final int LEFT = 1;
    /** Align descendants to the center. 자손을 가운데로 정렬. */
    static final int CENTER = 2;
    /** Align descendants to the right (the end side). 자손을 오른쪽(끝 쪽)으로 정렬. */
    static final int RIGHT = 3;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private AlignDescendants() {
    }

    /**
     * Returns the alignment that applies to the margins of a block-level box, or {@link #NONE}: the alignment of its nearest aligning
     * ancestor element, unless the element of the box has an applicable <code>align</code> attribute itself. For a box of generated
     * content the originating element is the first candidate because the box is its child.
     * <p>
     * 블록 수준 상자의 margin 에 적용되는 정렬 또는 {@link #NONE} 을 반환한다: 가장 가까운 정렬 조상 요소의 정렬이며, 상자의 요소 자신에게 적용 가능한
     * <code>align</code> 속성이 있으면 없다. 생성 콘텐츠의 상자는 원래 요소의 자식이므로 그 요소가 첫 후보이다.
     *
     * @param b the box / 상자
     * @return {@link #NONE}, {@link #LEFT}, {@link #CENTER} or {@link #RIGHT} / 정렬 값
     */
    static int forBox(LayoutBox b) {
        Node n = b.node;
        if (n == null) {
            return NONE;
        }
        Node start;
        if (b.pseudo != null) {
            // 생성 콘텐츠 상자는 원래 요소의 자식이므로 그 요소부터 찾는다.
            start = n;
        } else {
            if (n instanceof Element && hasApplicableAlign((Element) n)) {
                // 자신에게 적용 가능한 align 속성이 있으면 조상의 정렬 대상이 아니다.
                return NONE;
            }
            start = n.getParentNode();
        }
        // 가장 가까운 정렬 조상이 이긴다: 안쪽에서 바깥쪽으로 올라가며 처음 만난 정렬 요소를 쓴다.
        for (Node p = start; p != null; p = p.getParentNode()) {
            if (p instanceof Element) {
                int a = sourceOf((Element) p);
                if (a != NONE) {
                    return a;
                }
            }
        }
        return NONE;
    }

    /**
     * Returns the alignment of descendants that an element requests (HTML5 2014 10.3.3 and the table rules of 10.3.9): the
     * <code>center</code> element centers; <code>div</code>, <code>thead</code>, <code>tbody</code>, <code>tfoot</code>, <code>tr</code>,
     * <code>td</code> and <code>th</code> follow their <code>align</code> attribute (<code>center</code>/<code>middle</code> center,
     * <code>left</code> and <code>justify</code> left, <code>right</code> right); <code>caption</code> centers unless its attribute says
     * <code>left</code>, <code>right</code> or <code>justify</code>.
     * <p>
     * 요소가 요구하는 자손 정렬을 반환한다(HTML5 2014 10.3.3과 10.3.9의 표 규칙): <code>center</code> 요소는 가운데이다. <code>div</code>,
     * <code>thead</code>, <code>tbody</code>, <code>tfoot</code>, <code>tr</code>, <code>td</code>, <code>th</code>는 <code>align</code> 속성을 따른다
     * (<code>center</code>/<code>middle</code> 가운데, <code>left</code>와 <code>justify</code> 왼쪽, <code>right</code> 오른쪽).
     * <code>caption</code>은 속성이 <code>left</code>, <code>right</code>, <code>justify</code>가 아니면 가운데이다.
     *
     * @param e the element / 요소
     * @return the alignment or {@link #NONE} / 정렬 또는 {@link #NONE}
     */
    static int sourceOf(Element e) {
        if (!Namespaces.HTML.equals(e.getNamespaceURI())) {
            return NONE;
        }
        String n = Namespaces.asciiLower(e.getLocalName());
        if (n.equals("center")) {
            return CENTER;
        }
        boolean caption = n.equals("caption");
        if (!caption && !n.equals("div") && !isTablePart(n)) {
            return NONE;
        }
        String a = alignValue(e);
        if (a == null) {
            // caption 은 속성이 없어도 가운데이고 다른 요소는 속성이 있어야 정렬한다.
            return caption ? CENTER : NONE;
        }
        if (a.equals("center") || a.equals("middle")) {
            return CENTER;
        }
        if (a.equals("left") || a.equals("justify")) {
            return LEFT;
        }
        if (a.equals("right")) {
            return RIGHT;
        }
        return caption ? CENTER : NONE;
    }

    /**
     * Tells whether an element has an <code>align</code> attribute that one of the rendering rules applies to it: the aligning elements
     * (see {@link #sourceOf}), paragraphs and headings (<code>left</code>, <code>right</code>, <code>center</code>, <code>justify</code>),
     * tables and horizontal rules (<code>left</code>, <code>right</code>, <code>center</code>; a table also takes <code>middle</code>).
     * Such an element is not aligned by an ancestor.
     * <p>
     * 요소에 렌더링 규칙 하나가 적용하는 <code>align</code> 속성이 있는지 알려 준다: 정렬 요소({@link #sourceOf} 참조), 문단과 제목
     * (<code>left</code>, <code>right</code>, <code>center</code>, <code>justify</code>), 표와 가로줄(<code>left</code>, <code>right</code>,
     * <code>center</code>, 표는 <code>middle</code>도). 이런 요소는 조상이 정렬하지 않는다.
     *
     * @param e the element / 요소
     * @return true if the attribute is applicable / 속성이 적용 가능하면 true
     */
    static boolean hasApplicableAlign(Element e) {
        if (!Namespaces.HTML.equals(e.getNamespaceURI())) {
            return false;
        }
        String a = alignValue(e);
        if (a == null) {
            return false;
        }
        String n = Namespaces.asciiLower(e.getLocalName());
        boolean lrc = a.equals("left") || a.equals("right") || a.equals("center");
        if (n.equals("div") || isTablePart(n) || n.equals("caption")) {
            return lrc || a.equals("middle") || a.equals("justify");
        }
        if (n.equals("p") || n.equals("h1") || n.equals("h2") || n.equals("h3") || n.equals("h4") || n.equals("h5") || n.equals("h6")) {
            return lrc || a.equals("justify");
        }
        if (n.equals("table")) {
            return lrc || a.equals("middle");
        }
        return n.equals("hr") && lrc;
    }

    /**
     * Returns the trimmed lower-case <code>align</code> attribute or null.
     * <p>
     * 공백을 없애고 소문자로 만든 <code>align</code> 속성을 반환한다. 없으면 null.
     *
     * @param e the element / 요소
     * @return the value or null / 값 또는 null
     */
    private static String alignValue(Element e) {
        String v = e.getAttribute("align");
        return v == null ? null : Namespaces.asciiLower(v.trim());
    }

    /**
     * Tells whether a lower-case local name is a table section, row or cell.
     * <p>
     * 소문자 지역 이름이 표의 구역, 행, 셀인지 알려 준다.
     *
     * @param n the name / 이름
     * @return true for thead, tbody, tfoot, tr, td and th / thead, tbody, tfoot, tr, td, th 이면 true
     */
    private static boolean isTablePart(String n) {
        return n.equals("thead") || n.equals("tbody") || n.equals("tfoot") || n.equals("tr") || n.equals("td") || n.equals("th");
    }
}
