package org.duckdns.hjow.swingwebpanel.css.props;

/**
 * The mapping of the logical properties to the physical ones (CSS Logical Properties and Values Level 1, https://www.w3.org/TR/css-logical-1/,
 * retrieved 2026-11-17): <code>margin-block-start</code>, <code>padding-inline-end</code>, <code>inset-block-start</code>,
 * <code>border-inline-start-width</code> (and the style and the color), <code>border-start-end-radius</code>, <code>inline-size</code>,
 * <code>min-block-size</code> and the rest of the logical longhands (the ids {@link Prop#MARGIN_BLOCK_START} to {@link Prop#MAX_BLOCK_SIZE}).
 * A logical property is a different name for a physical property that depends on the <code>writing-mode</code> and the
 * <code>direction</code> of the element: the block axis is vertical in <code>horizontal-tb</code> and horizontal in the vertical modes (block-start is the
 * top, or the right in <code>vertical-rl</code> and <code>sideways-rl</code>, the left in <code>vertical-lr</code> and <code>sideways-lr</code>), the
 * inline axis is the other one and its start is the left (right in <code>rtl</code>) in <code>horizontal-tb</code>, the top (bottom in
 * <code>rtl</code>) in <code>vertical-rl</code>, <code>vertical-lr</code> and <code>sideways-rl</code> and the bottom (top in <code>rtl</code>) in
 * <code>sideways-lr</code>. The cascade writes the value of a logical declaration to the physical longhand that this class names, so a logical and a physical declaration
 * of the same side compete in the order of the cascade (the later one wins) and the layout code only knows the physical properties.
 * <p>
 * 논리 속성을 물리 속성으로 옮기는 규칙(CSS Logical Properties and Values Level 1, https://www.w3.org/TR/css-logical-1/, 조회 2026-11-17): <code>margin-block-start</code>,
 * <code>padding-inline-end</code>, <code>inset-block-start</code>, <code>border-inline-start-width</code>(와 스타일, 색), <code>border-start-end-radius</code>, <code>inline-size</code>,
 * <code>min-block-size</code> 등 논리 개별 속성({@link Prop#MARGIN_BLOCK_START} 부터 {@link Prop#MAX_BLOCK_SIZE} 까지의 id). 논리 속성은 요소의 <code>writing-mode</code> 와
 * <code>direction</code> 에 따라 정해지는 물리 속성의 다른 이름이다: 블록 축은 <code>horizontal-tb</code> 에서 세로이고 세로쓰기에서 가로이다(block-start 는 위쪽이며
 * <code>vertical-rl</code>, <code>sideways-rl</code> 에서는 오른쪽, <code>vertical-lr</code>, <code>sideways-lr</code> 에서는 왼쪽). 인라인 축은 다른 축이며 그 시작은
 * <code>horizontal-tb</code> 에서 왼쪽(<code>rtl</code> 이면 오른쪽), <code>vertical-rl</code>, <code>vertical-lr</code>, <code>sideways-rl</code> 에서 위쪽(<code>rtl</code> 이면 아래쪽),
 * <code>sideways-lr</code> 에서 아래쪽(<code>rtl</code> 이면 위쪽)이다. 계단식은 논리 선언의 값을 이 클래스가 알려 주는 물리 개별 속성에 쓰므로 같은 변의 논리 선언과 물리 선언은 계단식
 * 순서(나중 것이 이김)로 겨루고, 레이아웃 코드는 물리 속성만 안다.
 */
public final class LogicalProps {

    /** The first id of the logical longhands. 논리 개별 속성의 첫 id. */
    public static final int FIRST = Prop.MARGIN_BLOCK_START;

    /** The last id of the logical longhands. 논리 개별 속성의 마지막 id. */
    public static final int LAST = Prop.MAX_BLOCK_SIZE;

    /** The first id of the logical scroll margins and scroll paddings (CSS Scroll Snap Level 1): a second range, ids 296 to 303. 논리 scroll-margin/scroll-padding 의 첫 id(CSS Scroll Snap Level 1): 둘째 범위, id 296~303. */
    public static final int SCROLL_FIRST = Prop.SCROLL_MARGIN_BLOCK_START;

    /** The last id of the logical scroll margins and scroll paddings. 논리 scroll-margin/scroll-padding 의 마지막 id. */
    public static final int SCROLL_LAST = Prop.SCROLL_PADDING_INLINE_END;

    /** The physical longhands of the scroll margin and the scroll padding in the order top, right, bottom, left. scroll-margin 과 scroll-padding 의 물리 개별 속성(위, 오른쪽, 아래, 왼쪽 순서). */
    private static final int[][] SCROLL_FAMILY = {
        {Prop.SCROLL_MARGIN_TOP, Prop.SCROLL_MARGIN_RIGHT, Prop.SCROLL_MARGIN_BOTTOM, Prop.SCROLL_MARGIN_LEFT},
        {Prop.SCROLL_PADDING_TOP, Prop.SCROLL_PADDING_RIGHT, Prop.SCROLL_PADDING_BOTTOM, Prop.SCROLL_PADDING_LEFT}};

    /** The physical longhands of the four-sided families in the order top, right, bottom, left: margin, padding, inset, border width, style, color. 변이 넷인 계열의 물리 개별 속성(위, 오른쪽, 아래, 왼쪽 순서): margin, padding, 오프셋, border 너비, 스타일, 색. */
    private static final int[][] FAMILY = {
        {Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT},
        {Prop.PADDING_TOP, Prop.PADDING_RIGHT, Prop.PADDING_BOTTOM, Prop.PADDING_LEFT},
        {Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT},
        {Prop.BORDER_TOP_WIDTH, Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_LEFT_WIDTH},
        {Prop.BORDER_TOP_STYLE, Prop.BORDER_RIGHT_STYLE, Prop.BORDER_BOTTOM_STYLE, Prop.BORDER_LEFT_STYLE},
        {Prop.BORDER_TOP_COLOR, Prop.BORDER_RIGHT_COLOR, Prop.BORDER_BOTTOM_COLOR, Prop.BORDER_LEFT_COLOR}};

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private LogicalProps() {
    }

    /**
     * Tells whether a property id is one of the logical longhands.
     * <p>
     * 속성 id 가 논리 개별 속성인지 알려 준다.
     *
     * @param id the property id / 속성 id
     * @return true for a logical longhand / 논리 개별 속성이면 true
     */
    public static boolean isLogical(int id) {
        return id >= FIRST && id <= LAST || id >= SCROLL_FIRST && id <= SCROLL_LAST || id == Prop.CONTAIN_INTRINSIC_INLINE_SIZE || id == Prop.CONTAIN_INTRINSIC_BLOCK_SIZE;
    }

    /**
     * Returns the physical side (0 top, 1 right, 2 bottom, 3 left) that a logical side stands for.
     * <p>
     * 논리적 변이 가리키는 물리적 변(0 위, 1 오른쪽, 2 아래, 3 왼쪽)을 반환한다.
     *
     * @param k    the logical side: 0 block-start, 1 block-end, 2 inline-start, 3 inline-end / 논리적 변: 0 block-start, 1 block-end, 2 inline-start, 3 inline-end
     * @param mode the <code>writing-mode</code> keyword / <code>writing-mode</code> 키워드
     * @param rtl  true for <code>direction: rtl</code> / <code>direction: rtl</code> 이면 true
     * @return the physical side / 물리적 변
     */
    public static int side(int k, String mode, boolean rtl) {
        boolean vertical = mode != null && mode.startsWith("vertical") || mode != null && mode.startsWith("sideways");
        if (!vertical) {
            // 가로쓰기: 블록 축은 세로(위가 시작), 인라인 축은 가로(ltr 이면 왼쪽이 시작).
            return k == 0 ? 0 : (k == 1 ? 2 : (k == 2 ? (rtl ? 1 : 3) : (rtl ? 3 : 1)));
        }
        boolean blockStartRight = !"vertical-lr".equals(mode) && !"sideways-lr".equals(mode);
        boolean inlineStartBottom = "sideways-lr".equals(mode) ? !rtl : rtl;
        // 세로쓰기: 블록 축은 가로(vertical-rl 과 sideways-rl 은 오른쪽이 시작), 인라인 축은 세로.
        if (k == 0) {
            return blockStartRight ? 1 : 3;
        }
        if (k == 1) {
            return blockStartRight ? 3 : 1;
        }
        if (k == 2) {
            return inlineStartBottom ? 2 : 0;
        }
        return inlineStartBottom ? 0 : 2;
    }

    /**
     * Returns the physical longhand that a logical longhand stands for under a writing mode and a direction.
     * <p>
     * 쓰기 모드와 방향에서 논리 개별 속성이 가리키는 물리 개별 속성을 반환한다.
     *
     * @param id        the id of a logical longhand / 논리 개별 속성의 id
     * @param mode      the <code>writing-mode</code> keyword / <code>writing-mode</code> 키워드
     * @param direction the <code>direction</code> keyword / <code>direction</code> 키워드
     * @return the id of the physical longhand, or the id itself if it is not logical / 물리 개별 속성의 id. 논리 속성이 아니면 id 자신
     */
    public static int physical(int id, String mode, String direction) {
        if (!isLogical(id)) {
            return id;
        }
        boolean rtl = "rtl".equals(direction);
        // 논리 contain-intrinsic 크기: 인라인 크기는 가로쓰기에서 너비, 세로쓰기에서 높이이다(CSS Box Sizing 4 5.2 절).
        if (id == Prop.CONTAIN_INTRINSIC_INLINE_SIZE || id == Prop.CONTAIN_INTRINSIC_BLOCK_SIZE) {
            boolean vert = mode != null && (mode.startsWith("vertical") || mode.startsWith("sideways"));
            return (id == Prop.CONTAIN_INTRINSIC_INLINE_SIZE) != vert ? Prop.CONTAIN_INTRINSIC_WIDTH : Prop.CONTAIN_INTRINSIC_HEIGHT;
        }
        // 논리 scroll-margin/scroll-padding 은 둘째 범위이다: 계열(0 margin, 1 padding)과 변 번호(block-start, block-end, inline-start, inline-end)로 찾는다.
        if (id >= SCROLL_FIRST) {
            int r = id - SCROLL_FIRST;
            return SCROLL_FAMILY[r / 4][side(r % 4, mode, rtl)];
        }
        int rel = id - FIRST;
        if (rel < 24) {
            return FAMILY[rel / 4][side(rel % 4, mode, rtl)];
        }
        if (rel < 28) {
            // 모서리는 블록 변과 인라인 변의 교차점이다(24 start-start, 25 start-end, 26 end-start, 27 end-end).
            int block = side(rel == 24 || rel == 25 ? 0 : 1, mode, rtl);
            int inline = side(rel == 24 || rel == 26 ? 2 : 3, mode, rtl);
            boolean blockIsHorizontalEdge = block == 0 || block == 2;
            int verticalSide = blockIsHorizontalEdge ? block : inline;
            int horizontalSide = blockIsHorizontalEdge ? inline : block;
            boolean isTop = verticalSide == 0;
            boolean isRight = horizontalSide == 1;
            return isTop ? (isRight ? Prop.BORDER_TOP_RIGHT_RADIUS : Prop.BORDER_TOP_LEFT_RADIUS) : (isRight ? Prop.BORDER_BOTTOM_RIGHT_RADIUS : Prop.BORDER_BOTTOM_LEFT_RADIUS);
        }
        // 크기: 인라인 크기는 가로쓰기에서 너비, 세로쓰기에서 높이이다.
        boolean vertical = mode != null && (mode.startsWith("vertical") || mode.startsWith("sideways"));
        int kind = (rel - 28) / 2;
        boolean inline = (rel - 28) % 2 == 0;
        boolean width = inline != vertical;
        int[] w = {Prop.WIDTH, Prop.MIN_WIDTH, Prop.MAX_WIDTH};
        int[] h = {Prop.HEIGHT, Prop.MIN_HEIGHT, Prop.MAX_HEIGHT};
        return width ? w[kind] : h[kind];
    }
}
