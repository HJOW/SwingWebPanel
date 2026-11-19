package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * The <code>::first-line</code> pseudo-element (plan item P10-04-f, CSS Pseudo-Elements Level 4, CSS 2.1 5.12.1). The style of the
 * pseudo-element applies to the first formatted line of a block container: the properties that apply to it (font, color, text
 * decoration, text transform, letter and word spacing, line height, text shadow; the background paints behind the text of the line as an
 * inline fragment) are laid over the styles of the text of that line. Which text is on the first line depends on the styles: a larger
 * font breaks the line earlier. So the inline layouter first measures the whole content with the first-line styles (a probe that
 * creates no boxes), takes the end of the first line from it, and lays out for real with the text before that point restyled and a soft
 * line break at it, so the rest of the text keeps the normal style and cannot slip onto the first line. A restyled value is taken only
 * for a text whose own style still has the value of the element (an inline element inside the first line that sets a property itself keeps
 * it). This class makes the item lists; the probe and the real layout are done by {@link InlineLayouter}.
 * <p>
 * Documented limits: only horizontal block containers (a vertical writing mode ignores <code>::first-line</code>); the probe does not
 * lay out floats that start inside the container (floats before the container narrow its first line as usual); intrinsic size
 * measurement (min-content and max-content) ignores the pseudo-element; a first-line style on the inline elements inside the line comes
 * only through the inherited properties of their text; <code>text-transform</code> that changes the length of the text (for example
 * German sharp s) shifts the break position.
 * <p>
 * <code>::first-line</code> 의사 요소(계획 P10-04-f, CSS Pseudo-Elements Level 4, CSS 2.1 5.12.1). 의사 요소의 스타일은 블록 컨테이너의 첫 서식 줄에 적용된다:
 * 적용되는 속성(글꼴, 색, 텍스트 장식, text-transform, 글자/낱말 간격, 줄 높이, 텍스트 그림자. 배경은 그 줄의 텍스트 뒤에 인라인 조각으로 그려진다)을 그 줄 텍스트의
 * 스타일 위에 덮는다. 어느 텍스트가 첫 줄에 놓이는지는 스타일에 달려 있다(큰 글꼴은 줄을 더 일찍 나눈다). 그래서 인라인 레이아웃은 먼저 전체 내용을 첫 줄 스타일로
 * 잰 뒤(상자를 만들지 않는 탐색) 거기서 첫 줄의 끝을 얻고, 그 지점 앞의 텍스트를 다시 스타일링하고 그 지점에 부드러운 줄바꿈을 두어 실제로 배치한다. 그러면 나머지 텍스트는
 * 보통 스타일을 유지하고 첫 줄로 끼어들 수 없다. 다시 스타일링한 값은 텍스트 자신의 스타일이 아직 요소의 값을 가질 때만 가져온다(첫 줄 안에서 스스로 속성을 정한 인라인
 * 요소는 그 값을 유지한다). 이 클래스는 항목 목록을 만들고 탐색과 실제 배치는 {@link InlineLayouter} 가 한다.
 * <p>
 * 문서화된 한계: 가로쓰기 블록 컨테이너만(세로 쓰기 모드는 <code>::first-line</code> 을 무시한다), 탐색은 컨테이너 안에서 시작하는 float 를 배치하지 않는다(컨테이너 앞의
 * float 는 평소처럼 첫 줄을 좁힌다), 고유 크기 측정(min-content, max-content)은 의사 요소를 무시한다, 줄 안의 인라인 요소는 자기 텍스트의 상속 속성으로만 첫 줄 스타일을
 * 받는다, 텍스트 길이를 바꾸는 <code>text-transform</code>(예: 독일어 ß)은 줄바꿈 위치를 어긋나게 한다.
 */
final class FirstLine {

    /**
     * The properties that <code>::first-line</code> may set: color, the font properties, line height, text transform, text decoration,
     * text shadow, letter spacing and word spacing.
     * <p>
     * <code>::first-line</code> 이 정할 수 있는 속성: 색, 글꼴 속성, 줄 높이, text-transform, 텍스트 장식, 텍스트 그림자, 글자 간격, 낱말 간격.
     */
    static final int[] IDS = { Prop.COLOR, Prop.FONT_FAMILY, Prop.FONT_SIZE, Prop.FONT_STYLE, Prop.FONT_VARIANT_CAPS, Prop.FONT_WEIGHT, Prop.FONT_STRETCH,
            Prop.LINE_HEIGHT, Prop.FONT_SIZE_ADJUST, Prop.FONT_KERNING, Prop.FONT_FEATURE_SETTINGS, Prop.TEXT_TRANSFORM, Prop.TEXT_DECORATION_LINE,
            Prop.TEXT_DECORATION_STYLE, Prop.TEXT_DECORATION_COLOR, Prop.TEXT_DECORATION_THICKNESS, Prop.TEXT_SHADOW, Prop.LETTER_SPACING,
            Prop.WORD_SPACING };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FirstLine() {
    }

    /**
     * Hands the <code>::first-line</code> style of a block that has no inline content of its own (its content starts with block-level
     * children) down to its first in-flow block-level child, which is where the first line of the block is (CSS 2.1 5.12.1). A child
     * that has a first-line style of its own keeps it.
     * <p>
     * 자신의 인라인 콘텐츠가 없는(내용이 블록 수준 자식으로 시작하는) 블록의 <code>::first-line</code> 스타일을 첫 정상 흐름 블록 수준 자식에게 넘긴다. 블록의 첫 줄은
     * 거기에 있다(CSS 2.1 5.12.1). 자신의 첫 줄 스타일을 가진 자식은 그것을 유지한다.
     *
     * @param b the block box whose children are complete / 자식이 완성된 블록 상자
     */
    static void propagate(LayoutBox b) {
        if (b.firstLineStyle == null || (b.items != null && !b.items.isEmpty())) {
            return;
        }
        // 첫 정상 흐름 블록 자식을 찾아 스타일을 넘기고 그 아래로도 이어 간다.
        for (int i = 0; i < b.children.size(); i++) {
            LayoutBox c = b.children.get(i);
            if (c.floatSide != LayoutBox.FLOAT_NONE || c.isAbsolutelyPositioned()) {
                continue;
            }
            if (c.type == LayoutBox.Type.BLOCK || c.type == LayoutBox.Type.ANONYMOUS_BLOCK) {
                if (c.firstLineStyle == null) {
                    c.firstLineStyle = b.firstLineStyle;
                    c.firstLineBase = b.firstLineBase;
                }
                propagate(c);
            }
            return;
        }
    }

    /**
     * Tells whether a block container takes part in <code>::first-line</code> layout: it has a first-line style, inline content and a
     * horizontal writing mode.
     * <p>
     * 블록 컨테이너가 <code>::first-line</code> 배치에 해당하는지 알려 준다: 첫 줄 스타일과 인라인 콘텐츠가 있고 가로쓰기이다.
     *
     * @param b the block container / 블록 컨테이너
     * @return true if the first line is styled / 첫 줄에 스타일이 적용되면 true
     */
    static boolean applies(LayoutBox b) {
        return b.firstLineStyle != null && b.items != null && !b.items.isEmpty() && b.frame == Css.FRAME_H;
    }

    /**
     * Creates the box of the first-line fragment: an inline element box with the style of the pseudo-element that wraps all the items;
     * only the first line of it is painted (its start edge is empty, the later lines never mark it as used).
     * <p>
     * 첫 줄 조각의 상자를 만든다: 의사 요소의 스타일을 가지고 모든 항목을 감싸는 인라인 요소 상자. 첫 줄만 그려진다(시작 가장자리는 비어 있고 뒤의 줄은 사용됨으로 표시하지
     * 않는다).
     *
     * @param b the block container / 블록 컨테이너
     * @return the proto box / 원형 상자
     */
    static LayoutBox proto(LayoutBox b) {
        LayoutBox p = new LayoutBox(LayoutBox.Type.INLINE, b.node, b.firstLineStyle);
        p.pseudo = "first-line";
        p.inlineLevel = true;
        return p;
    }

    /**
     * Copies an item.
     * <p>
     * 항목을 복사한다.
     *
     * @param it the item / 항목
     * @return the copy / 사본
     */
    private static InlineItem copy(InlineItem it) {
        InlineItem c = new InlineItem(it.kind);
        c.text = it.text;
        c.node = it.node;
        c.style = it.style;
        c.box = it.box;
        c.owner = it.owner;
        c.edge = it.edge;
        c.decoration = it.decoration;
        c.marker = it.marker;
        c.srcOffset = it.srcOffset;
        c.soft = it.soft;
        return c;
    }

    /**
     * Makes the list of the items with the first-line style applied to all text: the opening and closing of the first-line fragment
     * around copies of the items whose text styles are laid over by the style of the pseudo-element.
     * <p>
     * 모든 텍스트에 첫 줄 스타일을 적용한 항목 목록을 만든다: 첫 줄 조각의 열기와 닫기가 항목 사본을 감싸고, 사본의 텍스트 스타일은 의사 요소의 스타일을 덮어쓴다.
     *
     * @param b     the block container / 블록 컨테이너
     * @param proto the first-line fragment box / 첫 줄 조각 상자
     * @return the items (the original index plus 1 is the index in the list) / 항목(원래 위치 + 1 이 목록 안의 위치)
     */
    static List<InlineItem> restyled(LayoutBox b, LayoutBox proto) {
        List<InlineItem> out = new ArrayList<InlineItem>(b.items.size() + 2);
        InlineItem open = new InlineItem(InlineItem.Kind.OPEN);
        open.box = proto;
        open.edge = true;
        out.add(open);
        // 항목마다 사본을 만들고 텍스트 항목의 스타일과 장식을 첫 줄 스타일로 덮는다.
        for (int i = 0; i < b.items.size(); i++) {
            InlineItem c = copy(b.items.get(i));
            if (c.kind == InlineItem.Kind.TEXT && c.style != null) {
                c.style = c.style.overlay(b.firstLineBase, b.firstLineStyle, IDS);
                c.decoration |= Css.decoration(c.style);
            }
            out.add(c);
        }
        InlineItem close = new InlineItem(InlineItem.Kind.CLOSE);
        close.box = proto;
        // 닫기의 가장자리 표시는 끈다: 켜면 뒤의 줄에서 조각이 사용됨으로 표시되어 첫 줄 조각이 그 줄에도 그려진다.
        close.edge = false;
        out.add(close);
        return out;
    }

    /**
     * Makes the list of the items for the real layout: the text before the end of the first line keeps the first-line style, a soft line
     * break follows it, and the text after it has its normal style.
     * <p>
     * 실제 배치를 위한 항목 목록을 만든다: 첫 줄 끝 앞의 텍스트는 첫 줄 스타일을 유지하고, 그 뒤에 부드러운 줄바꿈이 오며, 그 뒤의 텍스트는 보통 스타일이다.
     *
     * @param b   the block container / 블록 컨테이너
     * @param all the restyled items made by {@link #restyled} / {@link #restyled} 가 만든 다시 스타일링한 항목
     * @param end the end of the first line: {index in all, offset in the raw text (-1 for the whole item)} / 첫 줄의 끝: {all 안의 위치, 원문 안의 위치(-1 이면 항목 전체)}
     * @return the items for the layout / 배치용 항목
     */
    static List<InlineItem> split(LayoutBox b, List<InlineItem> all, int[] end) {
        int idx = end[0];
        int off = end[1];
        List<InlineItem> out = new ArrayList<InlineItem>(all.size() + 2);
        // 첫 줄의 끝 항목 앞까지는 첫 줄 스타일 사본을 그대로 쓴다.
        for (int i = 0; i < idx; i++) {
            out.add(all.get(i));
        }
        InlineItem at = all.get(idx);
        InlineItem soft = new InlineItem(InlineItem.Kind.BREAK);
        soft.soft = true;
        // 첫 줄이 강제 줄바꿈으로 끝났으면 그 줄바꿈이 줄을 끝내므로 부드러운 줄바꿈을 더하지 않는다.
        boolean needSoft = end.length < 3 || end[2] == 0;
        if (at.kind == InlineItem.Kind.TEXT && off > 0 && off < at.text.length()) {
            // 끝 항목은 둘로 나눈다: 앞은 첫 줄 스타일, 뒤는 원래 스타일.
            InlineItem head = copy(at);
            head.text = at.text.substring(0, off);
            out.add(head);
            if (needSoft) {
                out.add(soft);
            }
            InlineItem tail = copy(b.items.get(idx - 1));
            tail.text = at.text.substring(off);
            tail.srcOffset = at.srcOffset + off;
            out.add(tail);
        } else {
            out.add(at);
            if (needSoft) {
                out.add(soft);
            }
        }
        // 끝 항목 뒤는 원래 항목이다(마지막의 닫기 항목 하나만 첫 줄 조각의 것이다).
        for (int i = idx + 1; i < all.size() - 1; i++) {
            out.add(b.items.get(i - 1));
        }
        out.add(all.get(all.size() - 1));
        return out;
    }
}
