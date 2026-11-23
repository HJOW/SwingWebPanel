package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * One item of the inline content of a block container, in document order: a piece of text, the start or end of an inline element, an
 * atomic inline-level box (inline-block or inline replaced element), an optional wrap opportunity, or a forced line break. The box builder produces the items and the
 * inline layouter turns them into lines.
 * <p>
 * 블록 컨테이너의 인라인 콘텐츠를 이루는 항목 하나(문서 순서): 텍스트 조각, 인라인 요소의 시작이나 끝, 원자적 인라인 수준 상자(inline-block 또는
 * 인라인 치환 요소), 선택적 줄바꿈 기회, 강제 줄바꿈. 상자 빌더가 항목을 만들고 인라인 레이아웃이 줄로 바꾼다.
 */
final class InlineItem {

    /** Kinds of items. 항목 종류. */
    enum Kind {
        /** Text content. 텍스트 내용. */
        TEXT,
        /** Start of an inline element. 인라인 요소의 시작. */
        OPEN,
        /** End of an inline element. 인라인 요소의 끝. */
        CLOSE,
        /** Atomic inline-level box. 원자적 인라인 수준 상자. */
        ATOMIC,
        /** Forced line break. 강제 줄바꿈. */
        BREAK,
        /** Optional zero-width wrap opportunity, honoring its own white-space. 자신의 white-space를 따르는 폭 없는 선택적 줄바꿈 기회. */
        OPPORTUNITY,
        /** A float: the box is placed beside the lines (P10-01); it takes no space in the line itself. float: 상자는 줄 옆에 놓이며 줄 자체의 공간은 차지하지 않는다. */
        FLOAT,
        /** An absolutely positioned box: it only records its static position here (P10-01). 절대 배치 상자: 여기서는 정적 위치만 기록한다. */
        ABS
    }

    /** Kind of the item. 항목 종류. */
    final Kind kind;
    /** Raw text of a TEXT item. TEXT 항목의 원문. */
    String text;
    /** DOM node the text came from (a Text node, or the element for generated content), or null. 텍스트의 원본 DOM 노드. */
    Node node;
    /** Style of the text (the style of the innermost element). 텍스트의 스타일. */
    ComputedStyle style;
    /** For OPEN/CLOSE the inline element box, for ATOMIC the atomic box. OPEN/CLOSE 는 인라인 요소 상자, ATOMIC 은 원자적 상자. */
    LayoutBox box;
    /** The innermost open inline element around a TEXT or ATOMIC item, or null. TEXT/ATOMIC 을 감싼 가장 안쪽 인라인 요소. */
    LayoutBox owner;
    /** For OPEN true if the start edge (margin, border, padding) belongs here; for CLOSE the end edge. OPEN 이면 시작 가장자리, CLOSE 이면 끝 가장자리. */
    boolean edge;
    /** Text decoration bits that apply to the text. 텍스트에 적용되는 장식 비트. */
    int decoration;
    /** True if the text is the inside list marker. 안쪽 목록 표식 텍스트이면 true. */
    boolean marker;
    /** Offset of the text in the data of the source text node (the text after a split-off first letter starts there). 원본 텍스트 노드 데이터 안에서 텍스트의 시작 위치(떼어 낸 첫 글자 뒤의 텍스트가 거기서 시작한다). */
    int srcOffset;
    /** Position of the item in the list that the inline layouter is working on (set while the segments are made, P10-04-f). 인라인 레이아웃이 처리하는 목록 안의 항목 위치(세그먼트를 만들 때 정해진다, P10-04-f). */
    int index;
    /** For BREAK: true if the break is a soft wrap point (the line ends like a wrapped line and may be justified), false for a forced break. BREAK 이면: 부드러운 줄바꿈 지점(줄이 줄바꿈된 것처럼 끝나고 양쪽 정렬될 수 있음)이면 true, 강제 줄바꿈이면 false. */
    boolean soft;

    /**
     * Creates an item.
     * <p>
     * 항목을 만든다.
     *
     * @param kind kind / 종류
     */
    InlineItem(Kind kind) {
        this.kind = kind;
    }

    /**
     * Creates a text item.
     * <p>
     * 텍스트 항목을 만든다.
     *
     * @param text  the raw text / 원문
     * @param node  source node / 원본 노드
     * @param style style / 스타일
     * @param owner innermost open inline element / 가장 안쪽 인라인 요소
     * @param deco  decoration bits / 장식 비트
     * @return the item / 항목
     */
    static InlineItem text(String text, Node node, ComputedStyle style, LayoutBox owner, int deco) {
        InlineItem i = new InlineItem(Kind.TEXT);
        i.text = text;
        i.node = node;
        i.style = style;
        i.owner = owner;
        i.decoration = deco;
        return i;
    }
}
