package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.Text;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.layout.LayoutResult;

/**
 * What a selection means on the screen of one rendered page (plan item P11-08): where the caret goes for a point of the page, which part of
 * the page a {@link Range} covers (the {@link SelectionHighlight}), and which text the user copies. It joins the DOM {@link Range} with the
 * layout of the page, using the same mapping between the characters of a text node and the layout fragments that
 * {@link HitTester#hit(double, double)} uses (a fragment keeps the range of the source data that it painted). An instance belongs to one
 * {@link RenderedPage}; it reads only immutable layout data, but it must be used on the document thread because it reads the DOM.
 * <p>
 * Copied text rules (<code>innerText</code>-like): the painted text of the selected fragments (white space already collapsed); nothing between
 * fragments of one line; a space between lines of one block that were broken by wrapping, a line feed when the source has a line feed and
 * <code>white-space</code> keeps it; a line feed for <code>br</code>; a line feed between different blocks, two for a <code>p</code>; a tab
 * between table cells of one row and a line feed between rows; nothing for text that is <code>user-select: none</code> or hidden.
 * <p>
 * 한 렌더링된 페이지의 화면에서 선택이 뜻하는 것(계획 P11-08): 페이지의 한 점에 캐럿이 놓일 곳, {@link Range} 가 덮는 페이지의 부분({@link SelectionHighlight}),
 * 사용자가 복사하는 텍스트. DOM {@link Range} 를 페이지의 레이아웃과 잇는다. 텍스트 노드의 문자와 레이아웃 조각 사이의 대응은 {@link HitTester#hit(double, double)} 가
 * 쓰는 것과 같다(조각은 자기가 그린 원본 데이터의 범위를 가진다). 인스턴스는 한 {@link RenderedPage} 에 속하며 불변 레이아웃 데이터만 읽지만 DOM 을 읽으므로
 * 문서 스레드에서 써야 한다.
 * <p>
 * 복사 문자열 규칙(<code>innerText</code> 와 비슷): 선택된 조각의 그려진 텍스트(공백은 이미 접혀 있다). 한 줄의 조각 사이에는 아무것도 없고, 한 블록에서 자동으로
 * 줄이 바뀐 곳에는 공백 한 칸, 원본에 개행이 있고 <code>white-space</code> 가 보존하면 줄바꿈 한 개, <code>br</code> 은 줄바꿈 한 개, 다른 블록 사이는 줄바꿈 한 개
 * (<code>p</code> 이면 두 개), 한 행의 표 셀 사이는 탭, 행 사이는 줄바꿈이다. <code>user-select: none</code> 이거나 숨겨진 텍스트는 넣지 않는다.
 */
public final class TextSelections {

    /**
     * The position of the caret for a point: a text node and an offset in its data, with the layout fragment that the point belongs to.
     * <p>
     * 한 점에 대한 캐럿 위치: 텍스트 노드와 그 데이터 안의 오프셋, 그리고 점이 속한 레이아웃 조각.
     */
    public static final class Caret {
        /** The text node. 텍스트 노드. */
        private final Node node;
        /** The offset in the data of the node. 노드 데이터 안의 오프셋. */
        private final int offset;
        /** The fragment. 조각. */
        private final LayoutBox box;

        /**
         * Creates a caret position.
         * <p>
         * 캐럿 위치를 만든다.
         *
         * @param node   the text node / 텍스트 노드
         * @param offset the offset / 오프셋
         * @param box    the fragment / 조각
         */
        Caret(Node node, int offset, LayoutBox box) {
            this.node = node;
            this.offset = offset;
            this.box = box;
        }

        /**
         * Returns the text node.
         * <p>
         * 텍스트 노드를 반환한다.
         *
         * @return the node / 노드
         */
        public Node getNode() {
            return node;
        }

        /**
         * Returns the offset in the data of the text node.
         * <p>
         * 텍스트 노드 데이터 안의 오프셋을 반환한다.
         *
         * @return the offset / 오프셋
         */
        public int getOffset() {
            return offset;
        }

        /**
         * Returns the layout fragment that the point belongs to.
         * <p>
         * 점이 속한 레이아웃 조각을 반환한다.
         *
         * @return the fragment / 조각
         */
        public LayoutBox getBox() {
            return box;
        }
    }

    /**
     * Receives the parts of a range in tree order.
     * <p>
     * 범위의 부분을 트리 순서로 받는다.
     */
    private interface Visitor {
        /**
         * A text node that the range covers, from <code>from</code> to <code>to</code> (offsets in its data, from &lt; to).
         * <p>
         * 범위가 덮는 텍스트 노드. 데이터 오프셋 <code>from</code> 부터 <code>to</code> 까지(from &lt; to).
         *
         * @param node the node / 노드
         * @param from the start / 시작
         * @param to   the end / 끝
         */
        void text(Text node, int from, int to);

        /**
         * An element inside the range's common ancestor.
         * <p>
         * 범위의 공통 조상 안의 요소.
         *
         * @param element the element / 요소
         * @param range   the range / 범위
         */
        void element(Element element, Range range);
    }

    /**
     * The rendered page.
     * <p>
     * 렌더링된 페이지.
     */
    private final RenderedPage page;

    /**
     * The layout of the page.
     * <p>
     * 페이지의 레이아웃.
     */
    private final LayoutResult layout;

    /**
     * The text fragments of the page in painting order, built when first needed.
     * <p>
     * 페이지의 텍스트 조각(그리기 순서). 처음 필요할 때 만든다.
     */
    private LayoutBox[] texts;

    /**
     * Creates the selection geometry of a rendered page.
     * <p>
     * 렌더링된 페이지의 선택 기하를 만든다.
     *
     * @param page the page, not null / 페이지(null 불가)
     */
    public TextSelections(RenderedPage page) {
        if (page == null) {
            throw new IllegalArgumentException("page must not be null");
        }
        this.page = page;
        this.layout = page.getLayout();
    }

    /**
     * Returns the rendered page that this geometry belongs to.
     * <p>
     * 이 기하가 속한 렌더링된 페이지를 반환한다.
     *
     * @return the page / 페이지
     */
    public RenderedPage getPage() {
        return page;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // user-select
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether the text of a layout fragment can be selected: its <code>user-select</code> is not <code>none</code> (the used value is
     * the first value that is not <code>auto</code> on the way up to the root), and it is not hidden.
     * <p>
     * 레이아웃 조각의 텍스트를 선택할 수 있는지 알려 준다: <code>user-select</code> 가 <code>none</code> 이 아니고(사용 값은 루트로 올라가며 처음으로
     * <code>auto</code> 가 아닌 값) 숨겨져 있지 않다.
     *
     * @param box the fragment / 조각
     * @return true if selectable / 선택할 수 있으면 true
     */
    public static boolean isSelectable(LayoutBox box) {
        ComputedStyle own = box.getStyle();
        // 숨겨진 텍스트는 선택하지도 복사하지도 않는다.
        if (own != null && ("hidden".equals(own.keyword(Prop.VISIBILITY)) || "collapse".equals(own.keyword(Prop.VISIBILITY)))) {
            return false;
        }
        // 조상으로 올라가며 auto 가 아닌 첫 값이 사용 값이다.
        for (LayoutBox b = box; b != null; b = b.getParent()) {
            ComputedStyle s = b.getStyle();
            String v = s == null ? null : s.keyword(Prop.USER_SELECT);
            if (v != null && !"auto".equals(v)) {
                return !"none".equals(v);
            }
        }
        return true;
    }

    /**
     * Returns the outermost element whose used <code>user-select</code> is <code>all</code> for a fragment: a click inside it selects all its
     * content as one unit. The search goes up until a value other than <code>auto</code> and <code>all</code> ends it.
     * <p>
     * 조각에 대해 사용 <code>user-select</code> 가 <code>all</code> 인 가장 바깥 요소를 반환한다: 그 안을 클릭하면 내용 전체가 한 단위로 선택된다. 위로 올라가다
     * <code>auto</code> 와 <code>all</code> 이 아닌 값을 만나면 멈춘다.
     *
     * @param box the fragment / 조각
     * @return the element or null / 요소. 없으면 null
     */
    public static Element allElement(LayoutBox box) {
        Element found = null;
        // 조상으로 올라가며 all 이 이어지는 동안 가장 바깥 요소를 기억한다.
        for (LayoutBox b = box; b != null; b = b.getParent()) {
            ComputedStyle s = b.getStyle();
            String v = s == null ? null : s.keyword(Prop.USER_SELECT);
            if (v == null || "auto".equals(v)) {
                continue;
            }
            if (!"all".equals(v)) {
                break;
            }
            if (b.getNode() instanceof Element) {
                found = (Element) b.getNode();
            }
        }
        return found;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 캐럿 위치
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the text fragments of the page in painting order.
     * <p>
     * 페이지의 텍스트 조각을 그리기 순서로 반환한다.
     *
     * @return the fragments / 조각
     */
    synchronized LayoutBox[] textBoxes() {
        if (texts == null) {
            List<LayoutBox> list = new ArrayList<LayoutBox>();
            List<LayoutBox> stack = new ArrayList<LayoutBox>();
            stack.add(layout.getRoot());
            // 명시적 스택으로 상자를 모두 훑으며 텍스트 노드의 조각을 모은다(자식을 거꾸로 넣어 문서 순서로 꺼낸다).
            while (!stack.isEmpty()) {
                LayoutBox b = stack.remove(stack.size() - 1);
                if (b.getType() == LayoutBox.Type.TEXT && b.getPseudo() == null && b.getNode() instanceof Text) {
                    list.add(b);
                }
                List<LayoutBox> children = b.getChildren();
                // 자식을 거꾸로 스택에 넣어 문서 순서로 꺼내게 한다.
                for (int i = children.size() - 1; i >= 0; i--) {
                    stack.add(children.get(i));
                }
            }
            texts = list.toArray(new LayoutBox[list.size()]);
        }
        return texts;
    }

    /**
     * Finds the caret position for a point: the character boundary nearest to the point in the text fragment under it; if the point is not
     * over selectable text, the nearest selectable fragment (the line whose band contains the point, the nearest one horizontally; else the
     * nearest line), at its start if the point is before it and at its end if after.
     * <p>
     * 한 점의 캐럿 위치를 찾는다: 점 아래의 텍스트 조각 안에서 점에 가장 가까운 문자 경계. 점이 선택할 수 있는 텍스트 위가 아니면 가장 가까운
     * 선택 가능한 조각(점이 대역에 드는 줄 중 가로로 가장 가까운 것, 없으면 가장 가까운 줄)의 점이 앞이면 시작, 뒤이면 끝.
     *
     * @param docX x in document coordinates / 문서 좌표 x
     * @param docY y in document coordinates / 문서 좌표 y
     * @param hit  the hit test result of the point, or null / 점의 히트 테스트 결과. 없으면 null
     * @return the caret position, or null if the page has no selectable text / 캐럿 위치. 선택할 수 있는 텍스트가 없으면 null
     */
    public Caret caretAt(double docX, double docY, HitResult hit) {
        // 점이 선택할 수 있는 텍스트 조각 위이면 히트 테스트가 계산한 문자 오프셋을 그대로 쓴다.
        if (hit != null && hit.getTextNode() instanceof Text && hit.getBox() != null && hit.getBox().getType() == LayoutBox.Type.TEXT
                && hit.getTextOffset() >= 0 && isSelectable(hit.getBox())) {
            return new Caret(hit.getTextNode(), clamp(hit.getTextNode(), hit.getTextOffset()), hit.getBox());
        }
        return nearest(docX, docY);
    }

    /**
     * Finds the nearest selectable text fragment of a point and the caret position in it.
     * <p>
     * 점에서 가장 가까운 선택 가능한 텍스트 조각과 그 안의 캐럿 위치를 찾는다.
     *
     * @param docX x in document coordinates / 문서 좌표 x
     * @param docY y in document coordinates / 문서 좌표 y
     * @return the caret position or null / 캐럿 위치. 없으면 null
     */
    private Caret nearest(double docX, double docY) {
        LayoutBox best = null;
        double bestAcross = Double.POSITIVE_INFINITY;
        double bestAlong = Double.POSITIVE_INFINITY;
        LayoutBox[] all = textBoxes();
        // 모든 텍스트 조각을 훑으며 줄과 나란한 방향의 거리가 먼저, 같으면 줄 방향 거리가 가장 짧은 조각을 고른다.
        for (int i = 0; i < all.length; i++) {
            LayoutBox b = all[i];
            if (b.getText() == null || b.getText().length() == 0 || b.getSourceEnd() <= b.getSourceStart() || !isSelectable(b)) {
                continue;
            }
            double dx = docX < b.getX() ? b.getX() - docX : (docX > b.getX() + b.getWidth() ? docX - (b.getX() + b.getWidth()) : 0);
            double dy = docY < b.getY() ? b.getY() - docY : (docY > b.getY() + b.getHeight() ? docY - (b.getY() + b.getHeight()) : 0);
            boolean horizontalLine = b.getTextRotation() == 0;
            double across = horizontalLine ? dy : dx;
            double along = horizontalLine ? dx : dy;
            // 줄 사이 거리가 (반 픽셀 넘게) 더 가깝거나, 같은 줄이면 줄 방향 거리가 더 가까운 조각이 이긴다.
            if (best == null || across < bestAcross - 0.5 || (Math.abs(across - bestAcross) <= 0.5 && along < bestAlong)) {
                best = b;
                bestAcross = across;
                bestAlong = along;
            }
        }
        if (best == null) {
            return null;
        }
        int offset = offsetAt(best, docX, docY);
        return new Caret(best.getNode(), clamp(best.getNode(), offset), best);
    }

    /**
     * Returns the offset in the data of the text node for a point, as the hit test does, but for a point that may be outside the fragment
     * (it is moved onto the line of the fragment first).
     * <p>
     * 점에 대한 텍스트 노드 데이터의 오프셋을 히트 테스트와 같은 방식으로 반환한다. 점이 조각 밖일 수 있으므로 먼저 조각의 줄 위로 옮긴다.
     *
     * @param b    the fragment / 조각
     * @param docX x in document coordinates / 문서 좌표 x
     * @param docY y in document coordinates / 문서 좌표 y
     * @return the offset / 오프셋
     */
    private static int offsetAt(LayoutBox b, double docX, double docY) {
        int rot = b.getTextRotation();
        double along = rot == 0 ? docX - b.getX() : (rot == 1 ? docY - b.getY() : b.getY() + b.getHeight() - docY);
        along = Math.max(0, Math.min(b.getTextAdvance(), along));
        return HitTester.textOffset(b, b.getX() + along);
    }

    /**
     * Keeps an offset inside the data of a node.
     * <p>
     * 오프셋을 노드 데이터 안에 둔다.
     *
     * @param node   the node / 노드
     * @param offset the offset / 오프셋
     * @return the offset in 0 to the length / 0 부터 길이까지의 오프셋
     */
    private static int clamp(Node node, int offset) {
        int length = node instanceof CharacterData ? ((CharacterData) node).getLength() : 0;
        return Math.max(0, Math.min(length, offset));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 키보드로 캐럿(focus) 옮기기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the fragment of a text node that a caret position belongs to: the one whose source range holds the offset (a position at the
     * end of one fragment and at the start of the next belongs to the next one), or null if no selectable fragment shows it.
     * <p>
     * 캐럿 위치가 속한 텍스트 노드의 조각을 반환한다: 원본 범위가 오프셋을 가진 조각(한 조각의 끝이자 다음 조각의 시작인 위치는 다음 조각에 속한다). 그 위치를
     * 보여 주는 선택 가능한 조각이 없으면 null.
     *
     * @param node   the text node / 텍스트 노드
     * @param offset the offset in its data / 데이터 안의 오프셋
     * @return the fragment or null / 조각. 없으면 null
     */
    private LayoutBox fragmentAt(Node node, int offset) {
        LayoutBox found = null;
        List<LayoutBox> boxes = layout.getBoxes(node);
        // 노드의 조각 중 오프셋을 품은 것을 찾되, 조각의 끝에서 그친 위치는 다음 조각이 있으면 그쪽을 쓴다.
        for (int i = 0; i < boxes.size(); i++) {
            LayoutBox b = boxes.get(i);
            if (b.getType() != LayoutBox.Type.TEXT || b.getText() == null || b.getPseudo() != null || !isSelectable(b)) {
                continue;
            }
            if (offset >= b.getSourceStart() && offset <= b.getSourceEnd()) {
                found = b;
                if (offset < b.getSourceEnd()) {
                    break;
                }
            }
        }
        return found;
    }

    /**
     * Returns the rectangle of the caret at a position of a text node (zero width, as high as the line fragment), in document coordinates,
     * or null if the position is not shown or the text is vertical.
     * <p>
     * 텍스트 노드의 한 위치의 캐럿 사각형(너비 0, 줄 조각의 높이)을 문서 좌표로 반환한다. 그 위치가 보이지 않거나 세로쓰기 텍스트이면 null.
     *
     * @param node   the text node / 텍스트 노드
     * @param offset the offset in its data / 데이터 안의 오프셋
     * @return the rectangle or null / 사각형. 없으면 null
     */
    public Rectangle2D caretRect(Node node, int offset) {
        LayoutBox b = fragmentAt(node, offset);
        if (b == null || b.getTextRotation() != 0) {
            return null;
        }
        return fragmentRect(b, offset, offset);
    }

    /**
     * Returns the rectangle of the caret at any DOM position (zero width, as high as the line), in document coordinates, or null if the
     * position has no place on the page. A position in a text node is the position of that character boundary; a position in an element
     * takes the end of the text before it, else the start of the text or the box after it, else the content box of the element itself
     * (an empty block); a position right after a <code>br</code> is the start of the next line. This is where the editing caret is drawn
     * and what a collapsed range reports as its rectangle.
     * <p>
     * 아무 DOM 위치의 캐럿 사각형(너비 0, 줄 높이)을 문서 좌표로 반환한다. 위치가 페이지에 자리가 없으면 null. 텍스트 노드 안의 위치는 그 문자 경계이고,
     * 요소 안의 위치는 앞의 텍스트의 끝, 없으면 뒤의 텍스트나 상자의 시작, 그것도 없으면 요소 자신의 내용 상자(빈 블록)이다. <code>br</code> 바로 뒤의 위치는
     * 다음 줄의 시작이다. 편집 캐럿을 그리는 곳이며 접힌 범위가 자신의 사각형으로 알리는 값이다.
     *
     * @param container the node of the position / 위치의 노드
     * @param offset    the offset of the position / 위치의 오프셋
     * @return the rectangle or null / 사각형. 없으면 null
     */
    public Rectangle2D caretRectAt(Node container, int offset) {
        if (container == null) {
            return null;
        }
        if (container instanceof Text) {
            Rectangle2D r = caretRect(container, offset);
            if (r != null || container.getParentNode() == null) {
                return r;
            }
            // 이 텍스트가 보이지 않으면(공백만 접힘 등) 부모의 위치로 대신한다.
            int index = 0;
            // 부모 안에서 텍스트 노드의 위치를 센다.
            for (Node s = container.getPreviousSibling(); s != null; s = s.getPreviousSibling()) {
                index++;
            }
            return caretRectAt(container.getParentNode(), index);
        }
        Node before = null;
        Node after = null;
        int index = 0;
        // 오프셋 앞뒤의 자식을 찾는다.
        for (Node c = container.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (index == offset - 1) {
                before = c;
            } else if (index == offset) {
                after = c;
            }
            index++;
        }
        // 앞이 br 이면 다음 줄의 시작이다.
        if (before instanceof Element && "br".equals(((Element) before).getLocalName())) {
            Rectangle2D next = firstPlace(after);
            if (next != null) {
                return next;
            }
            Rectangle2D prev = lastPlace(before.getPreviousSibling());
            LayoutBox block = container instanceof Element ? firstBox((Element) container) : null;
            if (prev != null && block != null) {
                return new Rectangle2D.Double(block.getContentX(), prev.getMaxY(), 0, prev.getHeight());
            }
        }
        Rectangle2D r = lastPlace(before);
        if (r != null) {
            return r;
        }
        r = firstPlace(after);
        if (r != null) {
            return r;
        }
        // 앞뒤에 보이는 것이 없으면 컨테이너 자신(빈 블록)의 내용 상자 왼쪽 위에 줄 높이로 둔다.
        LayoutBox box = container instanceof Element ? firstBox((Element) container) : null;
        if (box == null) {
            return null;
        }
        ComputedStyle s = box.getStyle();
        double lh = s == null ? Double.NaN : s.lineHeightPx();
        // normal 이면 NaN 이므로 글꼴 크기의 1.2 배를 쓴다.
        double h = s == null ? 16 : Math.max(1, Double.isNaN(lh) ? s.fontSizePx() * 1.2 : lh);
        return new Rectangle2D.Double(box.getContentX(), box.getContentY(), 0, h);
    }

    /**
     * Returns the mark of the editing caret at a DOM position: a one pixel wide bar as high as the line, in the color of the
     * <code>caret-color</code> property (<code>auto</code> is the text color of the element that holds the position), or null if the
     * position has no place on the page.
     * <p>
     * DOM 위치의 편집 캐럿 표시를 반환한다: 줄 높이의 1 픽셀 너비 막대이며 색은 <code>caret-color</code> 속성의 색이다(<code>auto</code> 는 그 위치를 가진 요소의
     * 글자색). 위치가 페이지에 자리가 없으면 null.
     *
     * @param container the node of the position / 위치의 노드
     * @param offset    the offset of the position / 위치의 오프셋
     * @return the mark or null / 표시. 없으면 null
     */
    public SelectionHighlight caretMark(Node container, int offset) {
        Rectangle2D r = caretRectAt(container, offset);
        if (r == null) {
            return null;
        }
        int color = 0xFF000000;
        Node n = container;
        // 위치를 가진 노드에서 위로 올라가며 상자가 있는 첫 요소의 스타일에서 캐럿 색을 정한다.
        while (n != null) {
            LayoutBox box = n instanceof Element ? firstBox((Element) n) : null;
            ComputedStyle s = box == null ? null : box.getStyle();
            if (s != null) {
                org.duckdns.hjow.swingwebpanel.css.values.CssColor c = s.colorValue(Prop.CARET_COLOR);
                if (c == null) {
                    c = s.colorValue(Prop.COLOR);
                }
                if (c != null) {
                    color = c.getArgb();
                }
                break;
            }
            n = n.getParentNode();
        }
        return SelectionHighlight.caret(page, new Rectangle2D.Double(Math.rint(r.getX()), r.getY(), 1, Math.max(1, r.getHeight())), color);
    }

    /**
     * Returns the first layout box of an element that has a box.
     * <p>
     * 요소의 첫 레이아웃 상자를 반환한다. 상자가 없으면 null.
     *
     * @param e the element / 요소
     * @return the box or null / 상자. 없으면 null
     */
    private LayoutBox firstBox(Element e) {
        List<LayoutBox> boxes = layout.getBoxes(e);
        return boxes.isEmpty() ? null : boxes.get(0);
    }

    /**
     * Returns the caret place at the very end of the visible content of a node (the end of its last text fragment, or the right edge of
     * a replaced element), or null.
     * <p>
     * 노드의 보이는 내용의 맨 끝의 캐럿 자리(마지막 텍스트 조각의 끝이나 치환 요소의 오른쪽 가장자리)를 반환한다. 없으면 null.
     *
     * @param node the node, may be null / 노드. null 가능
     * @return the rectangle or null / 사각형. 없으면 null
     */
    private Rectangle2D lastPlace(Node node) {
        if (node == null) {
            return null;
        }
        List<Node> all = new ArrayList<Node>();
        // 노드와 그 후손을 트리 순서로 모은다.
        for (Node c = node; c != null; c = Node.nextInTree(c, node)) {
            all.add(c);
        }
        // 마지막 후손부터 거꾸로 훑어 보이는 위치가 있는 첫 것을 찾는다.
        for (int i = all.size() - 1; i >= 0; i--) {
            Rectangle2D r = placeOf(all.get(i), false);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns the caret place at the start of the visible content of a node (the start of its first text fragment, or the left edge of a
     * replaced element), or null.
     * <p>
     * 노드의 보이는 내용의 시작의 캐럿 자리(첫 텍스트 조각의 시작이나 치환 요소의 왼쪽 가장자리)를 반환한다. 없으면 null.
     *
     * @param node the node, may be null / 노드. null 가능
     * @return the rectangle or null / 사각형. 없으면 null
     */
    private Rectangle2D firstPlace(Node node) {
        if (node == null) {
            return null;
        }
        // 노드와 그 후손을 트리 순서로 훑어 보이는 위치가 있는 첫 것을 찾는다.
        for (Node c = node; c != null; c = Node.nextInTree(c, node)) {
            Rectangle2D r = placeOf(c, true);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Returns the caret place of one node, at its start or its end: a text node with a visible fragment, or a replaced element.
     * <p>
     * 노드 하나의 시작이나 끝의 캐럿 자리를 반환한다: 보이는 조각이 있는 텍스트 노드나 치환 요소.
     *
     * @param n     the node / 노드
     * @param start true for the start, false for the end / 시작이면 true, 끝이면 false
     * @return the rectangle or null / 사각형. 없으면 null
     */
    private Rectangle2D placeOf(Node n, boolean start) {
        if (n instanceof Text) {
            List<LayoutBox> boxes = layout.getBoxes(n);
            // 텍스트 노드의 보이는 조각 중 첫(시작) 또는 마지막(끝) 조각의 가장자리를 쓴다.
            for (int i = 0; i < boxes.size(); i++) {
                LayoutBox b = boxes.get(start ? i : boxes.size() - 1 - i);
                if (b.getType() == LayoutBox.Type.TEXT && b.getText() != null && b.getText().length() > 0 && b.getPseudo() == null
                        && b.getTextRotation() == 0) {
                    int at = start ? b.getSourceStart() : b.getSourceEnd();
                    return fragmentRect(b, at, at);
                }
            }
            return null;
        }
        if (n instanceof Element) {
            List<LayoutBox> boxes = layout.getBoxes(n);
            // 치환 요소(이미지 등)는 왼쪽(시작) 또는 오른쪽(끝) 가장자리에 놓는다.
            for (int i = 0; i < boxes.size(); i++) {
                LayoutBox b = boxes.get(i);
                if (b.getType() == LayoutBox.Type.REPLACED || b.getType() == LayoutBox.Type.INLINE_BLOCK) {
                    return new Rectangle2D.Double(start ? b.getX() : b.getX() + b.getWidth(), b.getY(), 0, b.getHeight());
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 범위의 사각형 (CSSOM View)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the client rectangles of a range in document coordinates (CSSOM View, <code>Range.getClientRects()</code>): the border boxes of
     * every element that the range selects completely and whose parent it does not select, and for every text node that the range selects
     * or partly selects the rectangles of the selected characters, one per line (the fragments of one line are joined). The result is in
     * tree order. A collapsed range in a text node gives one rectangle of zero width at the caret; a collapsed range elsewhere gives the
     * place of the caret if there is one. The rectangles include the transform of the boxes; clipping by <code>overflow</code> does not
     * apply.
     * <p>
     * 범위의 클라이언트 사각형을 문서 좌표로 반환한다(CSSOM View, <code>Range.getClientRects()</code>): 범위가 완전히 선택하고 부모는 선택하지 않는 모든 요소의
     * border box, 그리고 범위가 선택하거나 일부 선택하는 모든 텍스트 노드의 선택된 글자 사각형(줄마다 하나이며 한 줄의 조각은 합친다). 결과는 트리 순서이다. 텍스트
     * 노드 안의 접힌 범위는 캐럿 위치의 너비 0 사각형 하나이고, 그 밖의 접힌 범위는 캐럿 자리가 있으면 그것이다. 사각형에는 상자의 변환이 들어가며
     * <code>overflow</code> 로 자르는 것은 적용하지 않는다.
     *
     * @param range the range, may be null / 범위. null 가능
     * @return the rectangles / 사각형 목록
     */
    public List<Rectangle2D> rects(Range range) {
        List<Rectangle2D> out = new ArrayList<Rectangle2D>();
        if (range == null) {
            return out;
        }
        Node start = range.getStartContainer();
        Node end = range.getEndContainer();
        int startOffset = range.getStartOffset();
        int endOffset = range.getEndOffset();
        Node root = range.getCommonAncestorContainer();
        Node n = root;
        // 공통 조상 아래를 트리 순서로 훑는다. 완전히 덮인 요소는 자기 상자를 주고 그 안쪽은 건너뛴다.
        while (n != null) {
            boolean covered = false;
            if (n instanceof Text) {
                if (range.intersectsNode(n)) {
                    int length = ((CharacterData) n).getLength();
                    int from = n == start ? startOffset : 0;
                    int to = n == end ? endOffset : length;
                    addTextRects(out, (Text) n, from, to);
                }
            } else if (n instanceof Element && n != root && covers(range, n)) {
                addElementRects(out, (Element) n);
                covered = true;
            }
            n = covered ? skipChildren(n, root) : Node.nextInTree(n, root);
        }
        // 접힌 범위가 텍스트 밖에 있으면 캐럿 자리를 알려 준다.
        if (out.isEmpty() && range.isCollapsed()) {
            Rectangle2D caret = caretRectAt(start, startOffset);
            if (caret != null) {
                out.add(caret);
            }
        }
        return out;
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
     * Adds the rectangles of the selected characters of a text node: one per line, the fragments of a line joined into one.
     * <p>
     * 텍스트 노드의 선택된 글자의 사각형을 더한다: 줄마다 하나이며 한 줄의 조각은 하나로 합친다.
     *
     * @param out  the list / 목록
     * @param node the text node / 텍스트 노드
     * @param from the start in its data / 데이터 안의 시작
     * @param to   the end in its data / 데이터 안의 끝
     */
    private void addTextRects(List<Rectangle2D> out, Text node, int from, int to) {
        if (from >= to) {
            // 접힌 범위는 그 위치의 너비 0 사각형 하나를 준다.
            Rectangle2D caret = caretRect(node, from);
            if (caret != null) {
                out.add(transformed(fragmentAt(node, from), caret));
            }
            return;
        }
        // 서로게이트 쌍이나 결합 문자 묶음의 일부만 덮는 범위는 그 묶음 전체의 사각형을 낸다(CSSOM View).
        int[] whole = wholeClusters(node.getData(), from, to);
        from = whole[0];
        to = whole[1];
        List<LayoutBox> boxes = layout.getBoxes(node);
        Rectangle2D joined = null;
        LayoutBox joinedLine = null;
        // 노드의 조각을 줄 순서로 훑으며 같은 줄의 조각은 합치고 줄이 바뀌면 지금까지의 것을 내보낸다.
        for (int i = 0; i < boxes.size(); i++) {
            LayoutBox b = boxes.get(i);
            if (b.getType() != LayoutBox.Type.TEXT || b.getText() == null || b.getPseudo() != null) {
                continue;
            }
            int a = Math.max(from, b.getSourceStart());
            int z = Math.min(to, b.getSourceEnd());
            if (a >= z) {
                continue;
            }
            Rectangle2D r = transformed(b, fragmentRect(b, a, z));
            LayoutBox line = Extractor.lineOf(b);
            if (joined != null && line != null && line == joinedLine) {
                joined = joined.createUnion(r);
            } else {
                if (joined != null) {
                    out.add(joined);
                }
                joined = r;
                joinedLine = line;
            }
        }
        if (joined != null) {
            out.add(joined);
        }
    }

    /**
     * Widens a part of a text to whole typographic character units (a surrogate pair or a base character with its combining marks): the
     * start moves back to the start of the unit it is inside, the end forward to the end of the unit it is inside.
     * <p>
     * 텍스트의 한 부분을 글자 단위(서로게이트 쌍, 기본 문자와 결합 문자)의 전체로 넓힌다: 시작은 그것이 속한 단위의 시작으로, 끝은 그것이 속한 단위의 끝으로 간다.
     *
     * @param data the text / 텍스트
     * @param from the start offset / 시작 오프셋
     * @param to   the end offset / 끝 오프셋
     * @return {start, end} after widening / 넓힌 {시작, 끝}
     */
    static int[] wholeClusters(String data, int from, int to) {
        int start = Math.min(from, data.length());
        int end = Math.min(to, data.length());
        int i = 0;
        boolean startSet = false;
        // 앞에서부터 글자 단위를 하나씩 보며 시작과 끝이 단위 안이면 그 경계로 옮긴다.
        while (i < data.length()) {
            int e = TextRenderer.clusterEnd(data, i);
            if (!startSet && from < e) {
                start = i;
                startSet = true;
            }
            if (to <= e) {
                end = e;
                break;
            }
            i = e;
        }
        return new int[] {start, Math.max(start, end)};
    }

    /**
     * Adds the border boxes of an element.
     * <p>
     * 요소의 border box 들을 더한다.
     *
     * @param out the list / 목록
     * @param e   the element / 요소
     */
    private void addElementRects(List<Rectangle2D> out, Element e) {
        List<LayoutBox> boxes = layout.getBoxes(e);
        // 요소의 상자를 모두 훑으며 비어 있지 않은 border box 를 더한다.
        for (int i = 0; i < boxes.size(); i++) {
            LayoutBox b = boxes.get(i);
            if (b.getWidth() > 0 || b.getHeight() > 0) {
                out.add(transformed(b, new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight())));
            }
        }
    }

    /**
     * Returns the bounds of a rectangle after the transform of its box.
     * <p>
     * 사각형을 상자의 변환으로 옮긴 경계를 반환한다.
     *
     * @param box the box the rectangle belongs to, may be null / 사각형이 속한 상자. null 가능
     * @param r   the rectangle / 사각형
     * @return the transformed rectangle / 변환된 사각형
     */
    private static Rectangle2D transformed(LayoutBox box, Rectangle2D r) {
        AffineTransform full = box == null ? null : box.getFullTransform();
        return full == null ? r : full.createTransformedShape(r).getBounds2D();
    }

    /**
     * Moves a caret position by one character (or one word) to the visible position that follows or precedes it, crossing into the next or
     * the previous text node of the page. A position in an element (after Select All) is first moved to the text position nearest to it.
     * White space that the layout collapsed is skipped, so every step moves to a position that the user can see.
     * <p>
     * 캐럿 위치를 글자 하나(또는 낱말 하나)만큼 뒤나 앞의 보이는 위치로 옮기며, 페이지의 다음이나 이전 텍스트 노드로 넘어간다. 요소 안의 위치(전체 선택 뒤)는 먼저
     * 가장 가까운 텍스트 위치로 옮긴다. 레이아웃이 접은 공백은 건너뛰므로 모든 걸음은 사용자가 볼 수 있는 위치로 간다.
     *
     * @param node    the node of the position / 위치의 노드
     * @param offset  the offset of the position / 위치의 오프셋
     * @param forward true to move forward (to the right), false to move backward / 앞(오른쪽)으로 가면 true, 뒤로 가면 false
     * @param word    true to move by a word / 낱말 단위이면 true
     * @return the new position, or null if there is no further position / 새 위치. 더 갈 곳이 없으면 null
     */
    public Caret step(Node node, int offset, boolean forward, boolean word) {
        Node current = node;
        int position = offset;
        LayoutBox[] all = textBoxes();
        // 요소 안의 위치이면 방향에 맞는 가장 가까운 텍스트 위치로 먼저 옮긴다.
        if (!(current instanceof Text)) {
            LayoutBox pick = null;
            // 텍스트 조각을 문서 순서로 훑으며 위치 앞(뒤로 갈 때)이나 뒤(앞으로 갈 때)의 첫/마지막 조각을 찾는다.
            for (int i = 0; i < all.length; i++) {
                LayoutBox b = all[i];
                if (b.getText().length() == 0 || !isSelectable(b)) {
                    continue;
                }
                boolean before = Range.comparePointOrder(b.getNode(), b.getSourceEnd(), current, position) <= 0;
                if (forward && !before && pick == null) {
                    pick = b;
                } else if (!forward && before) {
                    pick = b;
                }
            }
            if (pick == null) {
                return null;
            }
            Node n = pick.getNode();
            // 앞으로 갈 때는 그 조각의 시작이 곧 다음 위치이고, 뒤로 갈 때는 그 조각의 끝에서 한 걸음 물러난다.
            if (forward) {
                return new Caret(n, pick.getSourceStart(), pick);
            }
            current = n;
            position = pick.getSourceEnd();
        }
        String data = ((CharacterData) current).getData();
        int o = position;
        // 같은 노드 안에서 다음(이전) 보이는 위치를 찾는다. 낱말 단위이면 공백만 있는 낱말은 건너뛴다.
        while (true) {
            if (word) {
                BreakIterator words = BreakIterator.getWordInstance(Locale.ENGLISH);
                words.setText(data);
                int next = forward ? words.following(o) : words.preceding(o);
                if (next == BreakIterator.DONE) {
                    break;
                }
                int from = Math.min(o, next);
                int to = Math.max(o, next);
                boolean blank = data.substring(from, to).trim().length() == 0;
                o = next;
                if (blank && (forward ? o < data.length() : o > 0)) {
                    continue;
                }
            } else if (forward) {
                if (o >= data.length()) {
                    break;
                }
                o = TextRenderer.clusterEnd(data, o);
            } else {
                if (o <= 0) {
                    break;
                }
                o = clusterStart(data, o);
            }
            LayoutBox b = fragmentAt(current, o);
            if (b != null && (o != position)) {
                return new Caret(current, o, b);
            }
        }
        // 노드의 끝을 넘으면 다음(이전) 텍스트 노드의 처음(끝)으로 간다.
        int index = -1;
        for (int i = 0; i < all.length; i++) {
            if (all[i].getNode() == current) {
                index = i;
                if (forward) {
                    continue;
                }
                break;
            }
        }
        if (index < 0) {
            return null;
        }
        // 방향대로 이웃 조각을 훑으며 글자가 있고 선택할 수 있는 첫 조각을 찾는다.
        for (int j = forward ? index + 1 : index - 1; j >= 0 && j < all.length; j += forward ? 1 : -1) {
            LayoutBox b = all[j];
            if (b.getNode() != current && b.getText().length() > 0 && isSelectable(b)) {
                return new Caret(b.getNode(), forward ? b.getSourceStart() : b.getSourceEnd(), b);
            }
        }
        return null;
    }

    /**
     * Returns the start of the character cluster that ends at an index (the inverse of the cluster end that the painter uses).
     * <p>
     * 인덱스에서 끝나는 글자 클러스터의 시작을 반환한다(그리기가 쓰는 클러스터 끝의 역).
     *
     * @param s     the string / 문자열
     * @param index an index greater than 0 / 0 보다 큰 인덱스
     * @return the start of the cluster / 클러스터의 시작
     */
    private static int clusterStart(String s, int index) {
        int i = index - 1;
        // 결합 문자를 거슬러 올라가 기본 문자를 찾고, 서로게이트 쌍의 뒤 절반이면 앞 절반까지 간다.
        while (i > 0) {
            int type = Character.getType(s.charAt(i));
            if (type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK) {
                i--;
            } else {
                break;
            }
        }
        if (i > 0 && Character.isLowSurrogate(s.charAt(i)) && Character.isHighSurrogate(s.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    /**
     * Moves a caret position to the line above or below, keeping its horizontal position; on the first (last) line it goes to the start
     * (end) of the line, like the arrow keys of a browser.
     * <p>
     * 캐럿 위치를 가로 위치를 유지한 채 위나 아래 줄로 옮긴다. 첫(마지막) 줄에서는 줄의 처음(끝)으로 간다(브라우저의 화살표 키처럼).
     *
     * @param node   the node of the position / 위치의 노드
     * @param offset the offset of the position / 위치의 오프셋
     * @param down   true for the line below, false for the line above / 아래 줄이면 true, 위 줄이면 false
     * @return the new position, or null if the position cannot be located / 새 위치. 위치를 찾을 수 없으면 null
     */
    public Caret line(Node node, int offset, boolean down) {
        Rectangle2D r = caretRect(node, offset);
        if (r == null) {
            return null;
        }
        double middle = r.getY() + r.getHeight() / 2;
        double y = down ? r.getMaxY() + r.getHeight() / 2 : r.getY() - r.getHeight() / 2;
        Caret c = caretAt(r.getX(), y, null);
        // 더 갈 줄이 없어 같은 자리에 머물면 줄의 끝(위로 가면 처음)으로 보낸다.
        if (c == null || (c.getNode() == node && c.getOffset() == offset)) {
            return caretAt(down ? 1e9 : -1e9, middle, null);
        }
        return c;
    }

    /**
     * Moves a caret position to the start or the end of its line.
     * <p>
     * 캐럿 위치를 줄의 처음이나 끝으로 옮긴다.
     *
     * @param node   the node of the position / 위치의 노드
     * @param offset the offset of the position / 위치의 오프셋
     * @param end    true for the end of the line, false for the start / 줄의 끝이면 true, 처음이면 false
     * @return the new position, or null if the position cannot be located / 새 위치. 위치를 찾을 수 없으면 null
     */
    public Caret lineEdge(Node node, int offset, boolean end) {
        Rectangle2D r = caretRect(node, offset);
        return r == null ? null : caretAt(end ? 1e9 : -1e9, r.getY() + r.getHeight() / 2, null);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 낱말과 문단
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the word around a caret position in the data of a text node: the word that contains the character after the caret (the
     * character before it at the end of the data), found with {@link BreakIterator#getWordInstance(Locale)}. A run of white space is a
     * unit as well. A word never leaves the text node.
     * <p>
     * 텍스트 노드 데이터에서 캐럿 위치 둘레의 낱말을 반환한다: 캐럿 뒤 문자(데이터 끝이면 앞 문자)가 속한 낱말이며
     * {@link BreakIterator#getWordInstance(Locale)} 로 찾는다. 공백 연속도 한 단위이다. 낱말은 텍스트 노드를 벗어나지 않는다.
     *
     * @param data   the data of the text node / 텍스트 노드의 데이터
     * @param offset the caret offset / 캐럿 오프셋
     * @return {start, end} (offsets in the data; both 0 for empty data) / {시작, 끝}(데이터 오프셋. 빈 데이터이면 둘 다 0)
     */
    public static int[] wordRange(String data, int offset) {
        int length = data.length();
        if (length == 0) {
            return new int[] { 0, 0 };
        }
        int index = Math.max(0, Math.min(length - 1, offset));
        BreakIterator words = BreakIterator.getWordInstance(Locale.ENGLISH);
        words.setText(data);
        int end = words.following(index);
        if (end == BreakIterator.DONE) {
            end = length;
        }
        int start = words.preceding(index + 1);
        if (start == BreakIterator.DONE) {
            start = 0;
        }
        return new int[] { start, end };
    }

    /**
     * Returns the nearest block-level box above a fragment, outside of the inline-blocks that contain it: the block that its text belongs
     * to for the purposes of paragraphs, line feeds and table cells. Text inside an <code>inline-block</code> belongs to the block that
     * contains the inline-block.
     * <p>
     * 조각 위의, 그것을 담은 inline-block 바깥의 가장 가까운 블록 수준 상자를 반환한다: 문단, 줄바꿈, 표 셀에서 그 텍스트가 속한 블록.
     * <code>inline-block</code> 안의 텍스트는 그 inline-block 을 담은 블록에 속한다.
     *
     * @param box the fragment / 조각
     * @return the block box or null / 블록 상자. 없으면 null
     */
    static LayoutBox blockOf(LayoutBox box) {
        LayoutBox candidate = null;
        // 위로 올라가며 가장 가까운 블록을 기억하되, inline-block 을 지나면 그 안에서 찾은 것은 버린다.
        for (LayoutBox b = box.getParent(); b != null; b = b.getParent()) {
            LayoutBox.Type t = b.getType();
            if (t == LayoutBox.Type.INLINE_BLOCK) {
                candidate = null;
            } else if (candidate == null && (t == LayoutBox.Type.BLOCK || t == LayoutBox.Type.ANONYMOUS_BLOCK || t == LayoutBox.Type.TABLE_CELL
                    || t == LayoutBox.Type.TABLE_CAPTION)) {
                candidate = b;
            }
        }
        return candidate;
    }

    /**
     * Returns the element whose content a triple click selects for a caret position: the element of the block that the text belongs to.
     * <p>
     * 캐럿 위치에 대해 트리플 클릭이 내용을 선택하는 요소를 반환한다: 텍스트가 속한 블록의 요소.
     *
     * @param caret the caret position / 캐럿 위치
     * @return the element, or null if there is none / 요소. 없으면 null
     */
    public static Element paragraphElement(Caret caret) {
        LayoutBox block = caret.getBox() == null ? null : blockOf(caret.getBox());
        // 익명 블록은 요소가 없으므로 위로 올라가 요소가 있는 상자를 찾는다.
        for (LayoutBox b = block; b != null; b = b.getParent()) {
            if (b.getNode() instanceof Element) {
                return (Element) b.getNode();
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 범위 훑기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Visits the text nodes and elements of a range in tree order.
     * <p>
     * 범위의 텍스트 노드와 요소를 트리 순서로 방문한다.
     *
     * @param range   the range / 범위
     * @param visitor the visitor / 방문자
     */
    private static void walk(Range range, Visitor visitor) {
        Node start = range.getStartContainer();
        Node end = range.getEndContainer();
        int startOffset = range.getStartOffset();
        int endOffset = range.getEndOffset();
        Node root = range.getCommonAncestorContainer();
        // 공통 조상 아래를 트리 순서로 훑으며 범위가 닿는 텍스트 노드와 모든 요소를 방문한다.
        for (Node n = root; n != null; n = Node.nextInTree(n, root)) {
            if (n instanceof Text) {
                if (!range.intersectsNode(n)) {
                    continue;
                }
                int length = ((CharacterData) n).getLength();
                int from = n == start ? startOffset : 0;
                int to = n == end ? endOffset : length;
                if (from < to) {
                    visitor.text((Text) n, from, to);
                }
            } else if (n instanceof Element) {
                visitor.element((Element) n, range);
            }
        }
    }

    /**
     * Tells whether a range covers a node completely (the node lies between the boundary points; touching them is allowed).
     * <p>
     * 범위가 노드를 완전히 덮는지 알려 준다(노드가 경계점 사이에 있으며 경계점에 닿아도 된다).
     *
     * @param range the range / 범위
     * @param node  the node / 노드
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

    // ---------------------------------------------------------------------------------------------------------------------------
    // 조각 안의 위치
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Converts an offset in the data of the text node to an index in the painted text of a fragment (the inverse of what the hit test does).
     * <p>
     * 텍스트 노드 데이터의 오프셋을 조각의 그려진 텍스트의 인덱스로 바꾼다(히트 테스트가 하는 것의 역).
     *
     * @param b      the fragment / 조각
     * @param source the offset in the data of the node (between the start and the end of the fragment) / 노드 데이터의 오프셋(조각의 시작과 끝 사이)
     * @return the index in 0 to the length of the painted text / 0 부터 그려진 텍스트 길이까지의 인덱스
     */
    private static int textIndex(LayoutBox b, int source) {
        int n = b.getText().length();
        int start = b.getSourceStart();
        int end = b.getSourceEnd();
        int index = end - start == n ? source - start : (int) Math.round((double) (source - start) * n / (end - start));
        return Math.max(0, Math.min(n, index));
    }

    /**
     * Returns the distance along the line from the start of a fragment to the boundary before the character with an index of its painted
     * text (the inverse of {@link HitTester#textOffset}).
     * <p>
     * 조각의 시작에서 그려진 텍스트의 인덱스 문자 앞 경계까지 줄을 따라간 거리를 반환한다({@link HitTester#textOffset} 의 역).
     *
     * @param b     the fragment / 조각
     * @param index the index in the painted text / 그려진 텍스트의 인덱스
     * @return the distance in pixels, measured from the physical start of the fragment along the line / 조각의 물리적 시작에서 줄을 따라 잰 거리(px)
     */
    private static double along(LayoutBox b, int index) {
        String s = b.getText();
        int n = s.length();
        double advance = b.getTextAdvance();
        if (n == 0 || advance <= 0) {
            return 0;
        }
        double logical;
        if (index <= 0) {
            logical = 0;
        } else if (index >= n) {
            logical = advance;
        } else if (b.getGlyphMode() == LayoutBox.GLYPHS_COMBINED) {
            // 조합한 묶음은 한 글자처럼 앞 절반이면 처음, 뒤 절반이면 끝이다.
            logical = index * 2 < n ? 0 : advance;
        } else if (b.getGlyphMode() == LayoutBox.GLYPHS_UPRIGHT) {
            // 똑바로 선 글자는 클러스터마다 같은 크기의 칸이므로 인덱스를 넘지 않는 가장 큰 클러스터 경계의 칸 번호를 쓴다.
            int[] clusters = b.getClusterOffsets();
            int cells = clusters.length - 1;
            int k = 0;
            // 인덱스를 넘지 않는 클러스터 경계를 하나씩 세어 칸 번호를 구한다.
            while (k < cells && clusters[k + 1] <= index) {
                k++;
            }
            logical = k * (advance / Math.max(1, cells));
        } else if (TextRenderer.isComplex(s) || b.getFont() == null) {
            logical = advance * index / n;
        } else {
            logical = TextRenderer.advance(b.getFont(), s.substring(0, index)) + b.getLetterSpacing() * index;
        }
        // 오른쪽에서 왼쪽 조각은 논리 순서의 앞이 물리적으로 줄의 끝쪽이다.
        return (b.getBidiLevel() & 1) == 1 ? advance - logical : logical;
    }

    /**
     * Returns the rectangle of a part of a text fragment in document coordinates (before the transform of the box).
     * <p>
     * 텍스트 조각의 한 부분의 사각형을 문서 좌표로 반환한다(상자의 변환 전).
     *
     * @param b    the fragment / 조각
     * @param from the start in the data of the node (inside the fragment) / 노드 데이터의 시작(조각 안)
     * @param to   the end in the data of the node (inside the fragment) / 노드 데이터의 끝(조각 안)
     * @return the rectangle / 사각형
     */
    static Rectangle2D fragmentRect(LayoutBox b, int from, int to) {
        double a0 = along(b, textIndex(b, from));
        double a1 = along(b, textIndex(b, to));
        double lo = Math.min(a0, a1);
        double hi = Math.max(a0, a1);
        int rot = b.getTextRotation();
        if (rot == 0) {
            return new Rectangle2D.Double(b.getX() + lo, b.getY(), hi - lo, b.getHeight());
        }
        if (rot == 1) {
            return new Rectangle2D.Double(b.getX(), b.getY() + lo, b.getWidth(), hi - lo);
        }
        return new Rectangle2D.Double(b.getX(), b.getY() + b.getHeight() - hi, b.getWidth(), hi - lo);
    }

    /**
     * Returns the rectangle of a part of a text fragment, given by indexes of its painted text, in document coordinates (before the
     * transform of the box); the page search uses it, which works on the painted text.
     * <p>
     * 그려진 텍스트의 인덱스로 준 텍스트 조각의 한 부분의 사각형을 문서 좌표로 반환한다(상자의 변환 전). 그려진 텍스트로 일하는 페이지 찾기가 쓴다.
     *
     * @param b    the fragment / 조각
     * @param from the start index in the painted text / 그려진 텍스트의 시작 인덱스
     * @param to   the end index in the painted text / 그려진 텍스트의 끝 인덱스
     * @return the rectangle / 사각형
     */
    static Rectangle2D paintedRect(LayoutBox b, int from, int to) {
        double a0 = along(b, from);
        double a1 = along(b, to);
        double lo = Math.min(a0, a1);
        double hi = Math.max(a0, a1);
        int rot = b.getTextRotation();
        if (rot == 0) {
            return new Rectangle2D.Double(b.getX() + lo, b.getY(), hi - lo, b.getHeight());
        }
        if (rot == 1) {
            return new Rectangle2D.Double(b.getX(), b.getY() + lo, b.getWidth(), hi - lo);
        }
        return new Rectangle2D.Double(b.getX(), b.getY() + b.getHeight() - hi, b.getWidth(), hi - lo);
    }

    /**
     * Tells whether two text fragments are on one line (see the rules of the copied text); for the page search.
     * <p>
     * 두 텍스트 조각이 한 줄에 있는지 알려 준다(복사 문자열 규칙 참고). 페이지 찾기가 쓴다.
     *
     * @param a the first fragment / 첫째 조각
     * @param b the second fragment / 둘째 조각
     * @return true if on one line / 한 줄이면 true
     */
    static boolean onOneLine(LayoutBox a, LayoutBox b) {
        return Extractor.sameLine(a, b);
    }

    /**
     * Tells whether the source between two text fragments holds white space that the layout collapsed away; for the page search.
     * <p>
     * 두 텍스트 조각 사이의 원본에 레이아웃이 접어 없앤 공백이 있는지 알려 준다. 페이지 찾기가 쓴다.
     *
     * @param prev the first fragment / 첫째 조각
     * @param cur  the second fragment / 둘째 조각
     * @return true if white space was collapsed between them / 둘 사이에서 공백이 접혔으면 true
     */
    static boolean collapsedGap(LayoutBox prev, LayoutBox cur) {
        return Extractor.gapHasWhitespace(prev, cur);
    }

    /**
     * Returns the area that the padding boxes of the clipping ancestors of a box leave visible, or null if nothing clips it.
     * <p>
     * 상자의 자르는 조상들의 padding box 가 남기는 보이는 영역을 반환한다. 자르는 것이 없으면 null.
     *
     * @param b the box / 상자
     * @return the visible area or null / 보이는 영역. 없으면 null
     */
    private static Rectangle2D clipOf(LayoutBox b) {
        Rectangle2D clip = null;
        // 위로 올라가며 내용을 자르는 조상의 padding box 를 교차시킨다(변환된 조상은 좌표계가 달라 건너뛴다).
        for (LayoutBox p = b.getParent(); p != null; p = p.getParent()) {
            if (p.isClipsContent() && p.getType() != LayoutBox.Type.REPLACED && p.getTransform() == null) {
                Rectangle2D padding = new Rectangle2D.Double(p.getX() + p.getBorderLeft(), p.getY() + p.getBorderTop(),
                        Math.max(0, p.getWidth() - p.getBorderLeft() - p.getBorderRight()),
                        Math.max(0, p.getHeight() - p.getBorderTop() - p.getBorderBottom()));
                clip = clip == null ? padding : clip.createIntersection(padding);
            }
        }
        return clip;
    }

    /**
     * Adds a rectangle of a box to a path, cut by the clipping ancestors and moved by the transform of the box.
     * <p>
     * 상자의 사각형을 자르는 조상으로 자르고 상자의 변환으로 옮겨 경로에 더한다.
     *
     * @param path the path / 경로
     * @param box  the box that the rectangle belongs to / 사각형이 속한 상자
     * @param r    the rectangle / 사각형
     * @return true if something was added / 무언가 더했으면 true
     */
    static boolean add(Path2D.Double path, LayoutBox box, Rectangle2D r) {
        Rectangle2D shown = r;
        Rectangle2D clip = clipOf(box);
        if (clip != null) {
            shown = r.createIntersection(clip);
        }
        if (shown.getWidth() <= 0 || shown.getHeight() <= 0) {
            return false;
        }
        Shape s = shown;
        AffineTransform full = box.getFullTransform();
        if (full != null) {
            s = full.createTransformedShape(shown);
        }
        path.append(s, false);
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 강조
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Computes the highlight of a range: the selected part of every selectable text fragment, and the border box of every replaced element
     * (images) that the range covers completely.
     * <p>
     * 범위의 강조를 계산한다: 선택할 수 있는 모든 텍스트 조각의 선택된 부분과, 범위가 완전히 덮는 모든 치환 요소(이미지)의 border box.
     *
     * @param range the range, may be null / 범위. null 가능
     * @return the highlight, or null if the range covers nothing that is drawn / 강조. 범위가 그려지는 것을 덮지 않으면 null
     */
    public SelectionHighlight highlight(Range range) {
        if (range == null || range.isCollapsed()) {
            return null;
        }
        final Path2D.Double path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        final boolean[] any = new boolean[1];
        walk(range, new Visitor() {
            /**
             * Adds the selected part of the fragments of a text node.
             * <p>
             * 텍스트 노드의 조각의 선택된 부분을 더한다.
             *
             * @param node the node / 노드
             * @param from the start / 시작
             * @param to   the end / 끝
             */
            public void text(Text node, int from, int to) {
                List<LayoutBox> boxes = layout.getBoxes(node);
                // 노드의 조각(줄마다 하나)을 훑으며 선택과 겹치는 부분의 사각형을 더한다.
                for (int i = 0; i < boxes.size(); i++) {
                    LayoutBox b = boxes.get(i);
                    if (b.getType() != LayoutBox.Type.TEXT || b.getText() == null || b.getPseudo() != null || !isSelectable(b)) {
                        continue;
                    }
                    int a = Math.max(from, b.getSourceStart());
                    int z = Math.min(to, b.getSourceEnd());
                    if (a < z && add(path, b, fragmentRect(b, a, z))) {
                        any[0] = true;
                    }
                }
            }

            /**
             * Adds the border box of a replaced element that the range covers completely.
             * <p>
             * 범위가 완전히 덮는 치환 요소의 border box 를 더한다.
             *
             * @param element the element / 요소
             * @param r       the range / 범위
             */
            public void element(Element element, Range r) {
                List<LayoutBox> boxes = layout.getBoxes(element);
                // 치환 상자가 있는 요소만 확인한다(대부분의 요소는 건너뛴다).
                for (int i = 0; i < boxes.size(); i++) {
                    LayoutBox b = boxes.get(i);
                    if (b.getType() == LayoutBox.Type.REPLACED && b.getReplacedContent() != null && covers(r, element) && isSelectable(b)
                            && add(path, b, new Rectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight()))) {
                        any[0] = true;
                    }
                }
            }
        });
        return any[0] ? new SelectionHighlight(page, path, SelectionHighlight.DEFAULT_COLOR) : null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 복사 문자열
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the text that copying the range puts on the clipboard (see the class description for the rules).
     * <p>
     * 범위를 복사하면 클립보드에 담기는 텍스트를 반환한다(규칙은 클래스 설명 참조).
     *
     * @param range the range, may be null / 범위. null 가능
     * @return the text, empty for an empty range / 텍스트. 빈 범위이면 빈 문자열
     */
    public String text(Range range) {
        if (range == null || range.isCollapsed()) {
            return "";
        }
        final Extractor out = new Extractor();
        walk(range, new Visitor() {
            /**
             * Adds the selected part of the fragments of a text node.
             * <p>
             * 텍스트 노드의 조각의 선택된 부분을 더한다.
             *
             * @param node the node / 노드
             * @param from the start / 시작
             * @param to   the end / 끝
             */
            public void text(Text node, int from, int to) {
                List<LayoutBox> boxes = layout.getBoxes(node);
                // 노드의 조각을 줄 순서로 훑으며 선택된 부분을 모은다.
                for (int i = 0; i < boxes.size(); i++) {
                    LayoutBox b = boxes.get(i);
                    if (b.getType() != LayoutBox.Type.TEXT || b.getText() == null || b.getPseudo() != null || !isSelectable(b)) {
                        continue;
                    }
                    int a = Math.max(from, b.getSourceStart());
                    int z = Math.min(to, b.getSourceEnd());
                    if (a < z) {
                        int ia = textIndex(b, a);
                        int iz = textIndex(b, z);
                        if (ia < iz) {
                            out.piece(b, b.getText().substring(ia, iz));
                        }
                    }
                }
            }

            /**
             * Adds a line feed for a <code>br</code> element that the range covers.
             * <p>
             * 범위가 덮는 <code>br</code> 요소에 줄바꿈을 더한다.
             *
             * @param element the element / 요소
             * @param r       the range / 범위
             */
            public void element(Element element, Range r) {
                if ("br".equals(element.getLocalName()) && covers(r, element)) {
                    out.lineBreak();
                }
            }
        });
        return out.toString();
    }

    /**
     * Collects the pieces of text of a selection and decides what goes between them.
     * <p>
     * 선택의 텍스트 조각을 모으고 그 사이에 들어갈 것을 정한다.
     */
    private static final class Extractor {
        /** The text so far. 지금까지의 텍스트. */
        private final StringBuilder out = new StringBuilder();
        /** The fragment of the last piece, or null at the start and after a line break. 마지막 조각의 레이아웃 조각. 처음이나 줄바꿈 뒤이면 null. */
        private LayoutBox previous;

        /**
         * Creates the collector.
         * <p>
         * 수집기를 만든다.
         */
        Extractor() {
        }

        /**
         * Adds a line feed (a <code>br</code>).
         * <p>
         * 줄바꿈(<code>br</code>)을 더한다.
         */
        void lineBreak() {
            out.append('\n');
            previous = null;
        }

        /**
         * Adds a piece of text that belongs to a fragment, after the separator that its place requires.
         * <p>
         * 조각에 속한 텍스트를 그 자리가 요구하는 구분자 뒤에 더한다.
         *
         * @param box  the fragment / 조각
         * @param text the selected text of the fragment / 조각의 선택된 텍스트
         */
        void piece(LayoutBox box, String text) {
            if (previous != null) {
                out.append(separator(previous, box, text));
            }
            out.append(text);
            previous = box;
        }

        /**
         * Returns the text of the selection.
         * <p>
         * 선택의 텍스트를 반환한다.
         *
         * @return the text / 텍스트
         */
        public String toString() {
            return out.toString();
        }

        /**
         * Returns what goes between the last piece and the next one.
         * <p>
         * 마지막 조각과 다음 조각 사이에 들어갈 것을 반환한다.
         *
         * @param prev the fragment of the last piece / 마지막 조각의 레이아웃 조각
         * @param cur  the fragment of the next piece / 다음 조각의 레이아웃 조각
         * @param text the text of the next piece / 다음 조각의 텍스트
         * @return the separator / 구분자
         */
        private String separator(LayoutBox prev, LayoutBox cur, String text) {
            LayoutBox pb = blockOf(prev);
            LayoutBox cb = blockOf(cur);
            // 다른 블록이면 표 셀, 문단, 일반 블록 순으로 구분자를 정한다.
            if (pb != cb) {
                if (pb != null && cb != null && pb.getType() == LayoutBox.Type.TABLE_CELL && cb.getType() == LayoutBox.Type.TABLE_CELL) {
                    return pb.getParent() == cb.getParent() ? "\t" : "\n";
                }
                int wanted = paragraph(pb) || paragraph(cb) ? 2 : 1;
                int have = 0;
                // 이미 끝에 줄바꿈이 있으면 필요한 만큼만 더 넣는다.
                while (have < out.length() && have < 2 && out.charAt(out.length() - 1 - have) == '\n') {
                    have++;
                }
                StringBuilder sb = new StringBuilder();
                // 필요한 줄바꿈 수에서 이미 있는 만큼을 뺀 개수를 더한다.
                for (int i = have; i < wanted; i++) {
                    sb.append('\n');
                }
                return sb.toString();
            }
            // 한 줄에서는 조각 사이에 접힌 공백이 있었으면(조각의 텍스트에 이미 공백이 없을 때) 공백 한 칸을 넣고, 아니면 아무것도 넣지 않는다.
            if (sameLine(prev, cur)) {
                boolean spaced = out.length() > 0 && Character.isWhitespace(out.charAt(out.length() - 1))
                        || text.length() > 0 && Character.isWhitespace(text.charAt(0));
                return !spaced && gapHasWhitespace(prev, cur) ? " " : "";
            }
            // 같은 블록의 다른 줄: 개행을 보존하는 원본의 개행이면 줄바꿈, 아니면 자동 줄바꿈이므로 공백 한 칸(이미 공백이 있으면 없음).
            if (preservesNewlines(cur) && gapHasNewline(prev, cur)) {
                return "\n";
            }
            boolean spaceBefore = out.length() > 0 && Character.isWhitespace(out.charAt(out.length() - 1));
            boolean spaceAfter = text.length() > 0 && Character.isWhitespace(text.charAt(0));
            return spaceBefore || spaceAfter ? "" : " ";
        }

        /**
         * Tells whether a block is (or inside) a paragraph element.
         * <p>
         * 블록이 문단 요소(<code>p</code>)인지 알려 준다.
         *
         * @param block the block box or null / 블록 상자. 없으면 null
         * @return true for <code>p</code> / <code>p</code> 이면 true
         */
        private static boolean paragraph(LayoutBox block) {
            // 익명 블록은 요소가 없으므로 위로 올라가 요소가 있는 첫 상자를 본다.
            for (LayoutBox b = block; b != null; b = b.getParent()) {
                if (b.getNode() instanceof Element) {
                    return "p".equals(((Element) b.getNode()).getLocalName());
                }
            }
            return false;
        }

        /**
         * Tells whether two fragments are on one line: the same line box, or a (nearly) equal band across the line when an inline-block made
         * the line boxes differ.
         * <p>
         * 두 조각이 한 줄에 있는지 알려 준다: 같은 줄 상자이거나, inline-block 때문에 줄 상자가 달라도 줄과 수직인 방향의 대역이 (거의) 같다.
         *
         * @param a the first fragment / 첫째 조각
         * @param b the second fragment / 둘째 조각
         * @return true if on one line / 한 줄이면 true
         */
        private static boolean sameLine(LayoutBox a, LayoutBox b) {
            if (lineOf(a) == lineOf(b)) {
                return true;
            }
            boolean horizontal = a.getTextRotation() == 0 && b.getTextRotation() == 0;
            double lo = horizontal ? Math.max(a.getY(), b.getY()) : Math.max(a.getX(), b.getX());
            double hi = horizontal ? Math.min(a.getY() + a.getHeight(), b.getY() + b.getHeight())
                    : Math.min(a.getX() + a.getWidth(), b.getX() + b.getWidth());
            double smaller = horizontal ? Math.min(a.getHeight(), b.getHeight()) : Math.min(a.getWidth(), b.getWidth());
            return smaller > 0 && hi - lo > smaller / 2;
        }

        /**
         * Returns the line box that a fragment is on.
         * <p>
         * 조각이 놓인 줄 상자를 반환한다.
         *
         * @param box the fragment / 조각
         * @return the line box or null / 줄 상자. 없으면 null
         */
        private static LayoutBox lineOf(LayoutBox box) {
            LayoutBox b = box.getParent();
            // 줄 상자를 만날 때까지 위로 올라간다.
            while (b != null && b.getType() != LayoutBox.Type.LINE) {
                b = b.getParent();
            }
            return b;
        }

        /**
         * Tells whether the <code>white-space</code> of a fragment keeps the line feeds of the source.
         * <p>
         * 조각의 <code>white-space</code> 가 원본의 개행을 보존하는지 알려 준다.
         *
         * @param box the fragment / 조각
         * @return true if line feeds are kept / 개행을 보존하면 true
         */
        private static boolean preservesNewlines(LayoutBox box) {
            ComputedStyle s = box.getStyle();
            String v = s == null ? null : s.keyword(Prop.WHITE_SPACE);
            return "pre".equals(v) || "pre-wrap".equals(v) || "pre-line".equals(v) || "break-spaces".equals(v);
        }

        /**
         * Tells whether the source between two fragments of one line holds white space that the layout collapsed away: in the data between
         * them if they belong to one text node, else white space right after the first fragment or right before the second.
         * <p>
         * 한 줄의 두 조각 사이의 원본에 레이아웃이 접어 없앤 공백이 있는지 알려 준다: 같은 텍스트 노드이면 그 사이의 데이터, 아니면 첫 조각 바로 뒤나 둘째 조각
         * 바로 앞의 공백.
         *
         * @param prev the first fragment / 첫째 조각
         * @param cur  the second fragment / 둘째 조각
         * @return true if white space was collapsed between them / 둘 사이에서 공백이 접혔으면 true
         */
        private static boolean gapHasWhitespace(LayoutBox prev, LayoutBox cur) {
            if (!(prev.getNode() instanceof CharacterData) || !(cur.getNode() instanceof CharacterData)) {
                return false;
            }
            String a = ((CharacterData) prev.getNode()).getData();
            String b = ((CharacterData) cur.getNode()).getData();
            if (prev.getNode() == cur.getNode()) {
                int from = Math.min(a.length(), prev.getSourceEnd());
                int to = Math.max(from, Math.min(a.length(), cur.getSourceStart()));
                // 두 조각 사이에 있는 원본 문자 중 공백이 있는지 하나씩 본다.
                for (int i = from; i < to; i++) {
                    if (Character.isWhitespace(a.charAt(i))) {
                        return true;
                    }
                }
                return false;
            }
            int after = prev.getSourceEnd();
            int before = cur.getSourceStart();
            return after < a.length() && Character.isWhitespace(a.charAt(after)) || before > 0 && before <= b.length() && Character.isWhitespace(b.charAt(before - 1));
        }

        /**
         * Tells whether the source between two fragments has a line feed: in the data between them if they belong to one text node, else in the
         * rest of the first node and the start of the second.
         * <p>
         * 두 조각 사이의 원본에 개행이 있는지 알려 준다: 같은 텍스트 노드이면 그 사이의 데이터, 아니면 첫 노드의 나머지와 둘째 노드의 처음.
         *
         * @param prev the first fragment / 첫째 조각
         * @param cur  the second fragment / 둘째 조각
         * @return true if there is a line feed / 개행이 있으면 true
         */
        private static boolean gapHasNewline(LayoutBox prev, LayoutBox cur) {
            if (!(prev.getNode() instanceof CharacterData) || !(cur.getNode() instanceof CharacterData)) {
                return false;
            }
            String a = ((CharacterData) prev.getNode()).getData();
            String b = ((CharacterData) cur.getNode()).getData();
            if (prev.getNode() == cur.getNode()) {
                int from = Math.min(a.length(), prev.getSourceEnd());
                int to = Math.max(from, Math.min(a.length(), cur.getSourceStart()));
                return a.substring(from, to).indexOf('\n') >= 0;
            }
            return a.substring(Math.min(a.length(), prev.getSourceEnd())).indexOf('\n') >= 0
                    || b.substring(0, Math.min(b.length(), cur.getSourceStart())).indexOf('\n') >= 0;
        }
    }
}
