package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The result of a layout run (plan items P09-01, P09-02, P09-03): the root of the box tree, the size of the whole document for
 * <code>preferredSize</code>, the map from DOM nodes to their boxes and hit testing. Nothing in a result changes after
 * {@link LayoutEngine#layout} has returned, so it can be handed to the event dispatch thread and painted there while the document thread
 * lays out the next version.
 * <p>
 * 레이아웃 실행의 결과(계획 P09-01, P09-02, P09-03): 상자 트리의 루트, <code>preferredSize</code> 를 위한 문서 전체 크기, DOM 노드에서 상자로의 대응,
 * hit testing. {@link LayoutEngine#layout} 이 돌아온 뒤 결과의 어떤 것도 바뀌지 않으므로 이벤트 디스패치 스레드에 넘겨 그리고 그동안 문서 스레드가 다음
 * 판을 배치할 수 있다.
 */
public final class LayoutResult {

    /** Root of the box tree. 상자 트리의 루트. */
    private final LayoutBox root;
    /** Width of the viewport used for the layout. 배치에 쓴 뷰포트 너비. */
    private final double viewportWidth;
    /** Height of the viewport used for the layout. 배치에 쓴 뷰포트 높이. */
    private final double viewportHeight;
    /** Width of the scrollable area. 스크롤 영역 너비. */
    private final double contentWidth;
    /** Height of the scrollable area. 스크롤 영역 높이. */
    private final double contentHeight;
    /** Boxes by DOM node. DOM 노드별 상자. */
    private final Map<Node, List<LayoutBox>> byNode;
    /** Number of boxes. 상자 수. */
    private final int count;
    /** The boxes in painting order, built when the first point is looked up. 그리기 순서의 상자. 처음 점을 찾을 때 만든다. */
    private volatile PaintSequence sequence;

    /**
     * Creates the result and indexes the box tree.
     * <p>
     * 결과를 만들고 상자 트리의 색인을 만든다.
     *
     * @param root     root box / 루트 상자
     * @param vw       viewport width / 뷰포트 너비
     * @param vh       viewport height / 뷰포트 높이
     */
    LayoutResult(LayoutBox root, double vw, double vh) {
        this.root = root;
        this.viewportWidth = vw;
        this.viewportHeight = vh;
        Map<Node, List<LayoutBox>> map = new IdentityHashMap<Node, List<LayoutBox>>();
        List<LayoutBox> stack = new ArrayList<LayoutBox>();
        stack.add(root);
        // 스크롤 영역에 속하지 않는 부분(fixed 상자, 자르는 상자 밖)을 빼기 위해 상자마다 보이는 범위의 오른쪽/아래쪽 한계를 기억한다.
        Map<LayoutBox, double[]> limits = new IdentityHashMap<LayoutBox, double[]>();
        Map<LayoutBox, double[]> inside = new IdentityHashMap<LayoutBox, double[]>();
        // 변환된 상자와 그 자손의 문서 좌표 변환 사슬(변환이 없으면 항목이 없다).
        Map<LayoutBox, java.awt.geom.AffineTransform> chains = new IdentityHashMap<LayoutBox, java.awt.geom.AffineTransform>();
        if (root.transform != null) {
            chains.put(root, new java.awt.geom.AffineTransform(root.transform));
        }
        double right = root.x + root.width + root.marginRight;
        double bottom = root.y + root.height + root.marginBottom;
        limits.put(root, new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY });
        int n = 0;
        // 모든 상자를 훑어 노드 색인을 만들고 스크롤 영역의 크기를 구한다(명시적 스택으로 깊이 제한이 없다).
        while (!stack.isEmpty()) {
            LayoutBox b = stack.remove(stack.size() - 1);
            n++;
            double[] lim = limits.get(b);
            java.awt.geom.AffineTransform full = chains.get(b);
            if (full == null) {
                right = Math.max(right, Math.min(b.x + b.width, lim[0]));
                bottom = Math.max(bottom, Math.min(b.y + b.height, lim[1]));
            } else {
                // 변환된 상자는 변환한 뒤의 영역이 스크롤 영역에 기여한다(변환은 배치를 바꾸지 않고 스크롤 가능한 넘침에는 들어간다. CSS Transforms 1 2 절).
                java.awt.geom.Rectangle2D tr = full.createTransformedShape(new java.awt.geom.Rectangle2D.Double(b.x, b.y, b.width, b.height)).getBounds2D();
                right = Math.max(right, Math.min(tr.getMaxX(), lim[0]));
                bottom = Math.max(bottom, Math.min(tr.getMaxY(), lim[1]));
            }
            // ::backdrop 상자는 요소의 상자가 아니므로 노드 색인에 넣지 않는다(요소의 크기와 그림에 섞이지 않게).
            if (b.node != null && !"backdrop".equals(b.pseudo)) {
                List<LayoutBox> l = map.get(b.node);
                if (l == null) {
                    l = new ArrayList<LayoutBox>(1);
                    map.put(b.node, l);
                }
                l.add(b);
            }
            // 자르는 상자의 자손은 padding box 의 오른쪽/아래쪽을 넘는 부분이 스크롤 영역에 들지 않는다.
            double[] in = lim;
            if (b.clipsContent && b.type != LayoutBox.Type.REPLACED) {
                in = new double[] { Math.min(lim[0], b.x + b.width - b.borderRight), Math.min(lim[1], b.y + b.height - b.borderBottom) };
            }
            inside.put(b, in);
            // 자식을 거꾸로 넣어 문서 순서로 꺼낸다. 절대 배치 상자는 포함 블록의 범위를 따르고 fixed 상자와 그 자손은 스크롤 영역을 넓히지 않는다.
            for (int i = b.children.size() - 1; i >= 0; i--) {
                LayoutBox c = b.children.get(i);
                double[] cl = in;
                if (c.isFixedPosition()) {
                    cl = new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY };
                } else if (c.isAbsolutelyPositioned()) {
                    cl = c.absCb == null ? new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY } : inside.get(c.absCb);
                    if (cl == null) {
                        cl = in;
                    }
                }
                limits.put(c, cl);
                // 자식의 변환 사슬: 부모의 사슬에 자식 자신의 변환을 먼저 적용한 것(없으면 부모의 것을 그대로 쓴다).
                java.awt.geom.AffineTransform pc = chains.get(b);
                if (c.transform != null) {
                    java.awt.geom.AffineTransform cc = pc == null ? new java.awt.geom.AffineTransform(c.transform) : new java.awt.geom.AffineTransform(pc);
                    if (pc != null) {
                        cc.concatenate(c.transform);
                    }
                    chains.put(c, cc);
                } else if (pc != null) {
                    chains.put(c, pc);
                }
                stack.add(c);
            }
        }
        this.contentWidth = right;
        this.contentHeight = bottom;
        this.byNode = map;
        this.count = n;
    }

    /**
     * Returns the root box (the box of the root element).
     * <p>
     * 루트 상자(루트 요소의 상자)를 반환한다.
     *
     * @return the root / 루트
     */
    public LayoutBox getRoot() {
        return root;
    }

    /**
     * The style source that the layout was made with (the renderer asks it for the styles of the highlight pseudo-elements), or null.
     * <p>
     * 레이아웃을 만든 스타일 공급자(렌더러가 하이라이트 의사 요소의 스타일을 묻는다). 없으면 null.
     */
    private volatile StyleSource styles;

    /**
     * Records the style source that the layout was made with.
     * <p>
     * 레이아웃을 만든 스타일 공급자를 기록한다.
     *
     * @param styles the source / 공급자
     */
    void setStyles(StyleSource styles) {
        this.styles = styles;
    }

    /**
     * Returns the style source that the layout was made with: the renderer asks it for the styles of pseudo-elements that it paints without a box of their own
     * (the custom highlights). Call it on the thread that owns the document.
     * <p>
     * 레이아웃을 만든 스타일 공급자를 반환한다: 렌더러가 자신의 상자 없이 그리는 의사 요소(사용자 정의 하이라이트)의 스타일을 묻는다. 문서를 소유한 스레드에서 부른다.
     *
     * @return the source or null / 공급자. 없으면 null
     */
    public StyleSource getStyles() {
        return styles;
    }

    /**
     * Returns the viewport width that the layout used.
     * <p>
     * 배치에 쓴 뷰포트 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getViewportWidth() {
        return viewportWidth;
    }

    /**
     * Returns the viewport height that the layout used.
     * <p>
     * 배치에 쓴 뷰포트 높이를 반환한다.
     *
     * @return height / 높이
     */
    public double getViewportHeight() {
        return viewportHeight;
    }

    /**
     * Returns the width of the scrollable area: the right edge of the farthest box (a wide unbreakable line or a wide image makes it larger
     * than the viewport), at least the root box including its margin.
     * <p>
     * 스크롤 영역의 너비를 반환한다: 가장 먼 상자의 오른쪽 가장자리(넓은 쪼갤 수 없는 줄이나 넓은 이미지는 뷰포트보다 커진다). 최소한 margin 을 포함한 루트
     * 상자의 너비이다.
     *
     * @return width / 너비
     */
    public double getContentWidth() {
        return contentWidth;
    }

    /**
     * Returns the height of the scrollable area (the document height).
     * <p>
     * 스크롤 영역의 높이(문서 높이)를 반환한다.
     *
     * @return height / 높이
     */
    public double getContentHeight() {
        return contentHeight;
    }

    /**
     * Returns the number of boxes in the tree.
     * <p>
     * 트리의 상자 수를 반환한다.
     *
     * @return count / 개수
     */
    public int getBoxCount() {
        return count;
    }

    /**
     * Returns the boxes that were made for a DOM node in document order: one box for an element, several fragments for text or for an inline
     * element that spans lines, none for <code>display:none</code>.
     * <p>
     * DOM 노드에 만들어진 상자를 문서 순서로 반환한다: 요소는 상자 하나, 줄에 걸친 텍스트나 인라인 요소는 여러 조각, <code>display:none</code> 은 없음.
     *
     * @param node the node / 노드
     * @return the boxes (unmodifiable, possibly empty) / 상자(수정 불가, 비어 있을 수 있음)
     */
    public List<LayoutBox> getBoxes(Node node) {
        List<LayoutBox> l = byNode.get(node);
        return l == null ? Collections.<LayoutBox>emptyList() : Collections.unmodifiableList(l);
    }

    /**
     * Finds the topmost box at a point (document coordinates): the last box in painting order (CSS 2.1 Appendix E: positive
     * <code>z-index</code> contexts above the positioned boxes, above the inline content, above the floats, above the block backgrounds,
     * above the negative contexts) that contains the point. A box that is clipped by an ancestor (<code>overflow</code> other than
     * visible, <code>clip: rect()</code>) is found only inside the clip, and boxes with <code>visibility:hidden</code> or
     * <code>pointer-events:none</code> are never found. Fixed boxes are looked up as if the page were not scrolled; see
     * {@link #boxAt(double, double, double, double)}.
     * <p>
     * 점(문서 좌표)의 가장 위 상자를 찾는다: 점을 포함하는 그리기 순서(CSS 2.1 부록 E: 양수 <code>z-index</code> 맥락이 위치 지정 상자 위, 그것이 인라인 콘텐츠 위,
     * 그것이 float 위, 그것이 블록 배경 위, 그것이 음수 맥락 위)의 마지막 상자. 조상에게 잘린 상자(<code>overflow</code> 가 visible 이 아님,
     * <code>clip: rect()</code>)는 자르기 안에서만 찾고 <code>visibility:hidden</code> 이나 <code>pointer-events:none</code> 인 상자는 찾지 않는다. fixed 상자는 페이지가
     * 스크롤되지 않은 것으로 찾는다. {@link #boxAt(double, double, double, double)} 참조.
     *
     * @param x x in document coordinates / 문서 좌표 x
     * @param y y in document coordinates / 문서 좌표 y
     * @return the box or null / 상자 또는 null
     */
    public LayoutBox boxAt(double x, double y) {
        return boxAt(x, y, 0, 0);
    }

    /**
     * Finds the topmost box at a point of the document while the page is scrolled: boxes of <code>position:fixed</code> stay at the same
     * place of the viewport, so the point is compared with them after the scroll offset is subtracted.
     * <p>
     * 페이지가 스크롤된 상태에서 문서의 점의 가장 위 상자를 찾는다: <code>position:fixed</code> 상자는 뷰포트의 같은 자리에 머무르므로 스크롤 오프셋을 뺀 점을
     * 그 상자와 비교한다.
     *
     * @param x       x in document coordinates / 문서 좌표 x
     * @param y       y in document coordinates / 문서 좌표 y
     * @param scrollX horizontal scroll offset / 가로 스크롤 오프셋
     * @param scrollY vertical scroll offset / 세로 스크롤 오프셋
     * @return the box or null / 상자 또는 null
     */
    public LayoutBox boxAt(double x, double y, double scrollX, double scrollY) {
        return sequence().hit(x, y, scrollX, scrollY, viewportWidth, viewportHeight);
    }

    /**
     * Returns the painting-order sequence, recording it on first use. The record is a pure function of the immutable box tree, so two
     * threads that race to build it build the same thing.
     * <p>
     * 그리기 순서의 기록을 반환하며 처음 쓸 때 만든다. 기록은 불변 상자 트리의 순수 함수이므로 두 스레드가 경쟁해 만들어도 같은 것이 만들어진다.
     *
     * @return the sequence / 기록
     */
    private PaintSequence sequence() {
        PaintSequence s = sequence;
        if (s == null) {
            s = PaintSequence.of(root);
            sequence = s;
        }
        return s;
    }
}
