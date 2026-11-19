package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * A box of the layout tree (plan item P09-01). The layout tree is separate from the DOM: <code>display:none</code> elements have no box,
 * anonymous boxes exist where CSS needs them, text is split into line fragments and every box keeps the DOM node it came from for hit
 * testing. After {@link LayoutEngine#layout} returns, a box never changes again, so a renderer on another thread can read the tree while the
 * document thread builds the next one.
 * <p>
 * Geometry: all values are CSS pixels in <b>document coordinates</b> (the origin is the top-left corner of the page, scrolling is the
 * renderer's job). {@link #getX()}/{@link #getY()}/{@link #getWidth()}/{@link #getHeight()} describe the <b>border box</b>; the margin,
 * border and padding widths are the used values, so the padding box, content box and margin box follow by addition. Line boxes
 * ({@link Type#LINE}) span the content width of their block; fragments ({@link Type#TEXT}, {@link Type#INLINE}, atomic inline boxes) are
 * children of the line. The children of a box are listed in paint order for in-flow content (a renderer paints a box, then its children).
 * <p>
 * 레이아웃 트리의 상자(계획 P09-01). 레이아웃 트리는 DOM 과 분리되어 있다: <code>display:none</code> 요소는 상자가 없고, CSS 가 필요로 하는 곳에는
 * 익명 상자가 있으며, 텍스트는 줄 조각으로 나뉘고, 모든 상자는 hit testing 을 위해 자신이 나온 DOM 노드를 가진다. {@link LayoutEngine#layout} 이
 * 돌아온 뒤 상자는 다시 바뀌지 않으므로 다른 스레드의 렌더러가 읽는 동안 문서 스레드가 다음 트리를 만들 수 있다.
 * <p>
 * 기하: 모든 값은 <b>문서 좌표</b>(원점은 페이지의 왼쪽 위, 스크롤은 렌더러의 몫)의 CSS 픽셀이다. {@link #getX()}/{@link #getY()}/{@link #getWidth()}/
 * {@link #getHeight()} 는 <b>border box</b> 를 나타내며 margin, border, padding 너비는 사용 값이므로 padding box, content box, margin box 는 더해서
 * 얻는다. 줄 상자({@link Type#LINE})는 블록의 콘텐츠 너비에 걸치고 조각({@link Type#TEXT}, {@link Type#INLINE}, 원자적 인라인 상자)은 줄의 자식이다.
 * 정상 흐름 콘텐츠의 자식은 그리기 순서로 나열되어 있다(렌더러는 상자를 그린 뒤 자식을 그린다).
 */
public final class LayoutBox {

    /**
     * The kind of a box.
     * <p>
     * 상자의 종류.
     */
    public enum Type {
        /** A block-level box of an element (also list items, the root, and block-level replaced elements are {@link #REPLACED}). 요소의 블록 수준 상자. */
        BLOCK,
        /** An anonymous block box that wraps inline content between block boxes. 블록 상자 사이의 인라인 콘텐츠를 감싸는 익명 블록 상자. */
        ANONYMOUS_BLOCK,
        /** An atomic inline-level box (<code>display:inline-block</code>); its content is laid out like a block. 원자적 인라인 수준 상자(inline-block). */
        INLINE_BLOCK,
        /** A replaced element (image, canvas, form control ...), block-level or inline-level, see {@link #isInlineLevel()}. 치환 요소. */
        REPLACED,
        /** A line box; its children are the fragments on that line. 줄 상자. 자식은 그 줄의 조각. */
        LINE,
        /** The fragment of an inline element on one line (its background, border and padding area). 한 줄 위의 인라인 요소 조각. */
        INLINE,
        /** A run of text on one line. 한 줄 위의 텍스트 조각. */
        TEXT,
        /** A list marker (outside of the list item, or inside as text). 목록 표식. */
        MARKER,
        /**
         * A table (P10-02): the border box includes the captions above and below the grid; the borders, the padding and the background
         * of the table belong to the grid, see {@link LayoutBox#getGridRectangle()}. It is block-level, or inline-level for
         * <code>display:inline-table</code>. 표(P10-02): border box 는 격자 위아래의 caption 을 포함한다. 표의 border, padding, 배경은
         * 격자의 것이다({@link LayoutBox#getGridRectangle()} 참조). 블록 수준이고 <code>display:inline-table</code> 이면 인라인 수준이다.
         */
        TABLE,
        /** A caption of a table (a block container). 표의 caption(블록 컨테이너). */
        TABLE_CAPTION,
        /** A row group (<code>thead</code>, <code>tbody</code>, <code>tfoot</code>), also an anonymous one. 행 그룹(<code>thead</code>, <code>tbody</code>, <code>tfoot</code>). 익명일 수도 있다. */
        TABLE_ROW_GROUP,
        /** A table row, also an anonymous one. 표의 행. 익명일 수도 있다. */
        TABLE_ROW,
        /** A table cell, a block container that starts a block formatting context, also an anonymous one. 표의 셀. 블록 서식 문맥을 시작하는 블록 컨테이너이며 익명일 수도 있다. */
        TABLE_CELL,
        /** A column group (<code>colgroup</code>); it has no content, only a background and borders. 열 그룹(<code>colgroup</code>). 내용이 없고 배경과 border 만 있다. */
        TABLE_COLUMN_GROUP,
        /** A column (<code>col</code>); it has no content, only a background and borders. 열(<code>col</code>). 내용이 없고 배경과 border 만 있다. */
        TABLE_COLUMN
    }

    /** Bit of {@link #getTextDecoration()}: underline. {@link #getTextDecoration()} 의 비트: 밑줄. */
    public static final int DECORATION_UNDERLINE = 1;

    /** Bit of {@link #getTextDecoration()}: overline. {@link #getTextDecoration()} 의 비트: 윗줄. */
    public static final int DECORATION_OVERLINE = 2;

    /** Bit of {@link #getTextDecoration()}: line-through. {@link #getTextDecoration()} 의 비트: 취소선. */
    public static final int DECORATION_LINE_THROUGH = 4;

    /** Value of {@link #getGlyphMode()}: one sideways (rotated) run, also all text of horizontal lines. {@link #getGlyphMode()} 의 값: 눕힌(회전한) 하나의 구간. 가로 줄의 모든 텍스트도 같다. */
    public static final int GLYPHS_SIDEWAYS = 0;

    /** Value of {@link #getGlyphMode()}: upright characters, one cell of one em per character cluster. {@link #getGlyphMode()} 의 값: 똑바로 선 글자. 글자 클러스터마다 1em 한 칸. */
    public static final int GLYPHS_UPRIGHT = 1;

    /** Value of {@link #getGlyphMode()}: text composed horizontally into one upright cell (<code>text-combine-upright</code>). {@link #getGlyphMode()} 의 값: 가로로 짜서 똑바로 선 한 칸에 넣은 텍스트(<code>text-combine-upright</code>). */
    public static final int GLYPHS_COMBINED = 2;

    /** Kind of the box. 상자 종류. */
    final Type type;
    /** DOM node the box came from (the originating element for pseudo-elements, the Text node for text), or null. 상자의 원본 DOM 노드. */
    Node node;
    /** <code>before</code> or <code>after</code> for generated content, else null. 생성된 콘텐츠이면 before/after. */
    String pseudo;
    /** Computed style (for anonymous boxes the style of the parent block). 계산 스타일. */
    ComputedStyle style;
    /** Parent box or null for the root. 부모 상자. */
    LayoutBox parent;
    /** Children in paint order. 그리기 순서의 자식. */
    final List<LayoutBox> children = new ArrayList<LayoutBox>();
    /** True for inline-level boxes (inline fragments, atomic inlines, inline replaced). 인라인 수준이면 true. */
    boolean inlineLevel;

    /** X relative to the parent border box while laying out. 배치 중 부모 border box 기준 x. */
    double lx;
    /** Y relative to the parent border box while laying out. 배치 중 부모 border box 기준 y. */
    double ly;
    /** Absolute x of the border box. border box 의 절대 x. */
    double x;
    /** Absolute y of the border box. border box 의 절대 y. */
    double y;
    /** Border box width. border box 너비. */
    double width;
    /** Border box height. border box 높이. */
    double height;
    /** Used top margin. 사용된 위 margin. */
    double marginTop;
    /** Used right margin. 사용된 오른쪽 margin. */
    double marginRight;
    /** Used bottom margin. 사용된 아래 margin. */
    double marginBottom;
    /** Used left margin. 사용된 왼쪽 margin. */
    double marginLeft;
    /** Used top border width. 위 border 너비. */
    double borderTop;
    /** Used right border width. 오른쪽 border 너비. */
    double borderRight;
    /** Used bottom border width. 아래 border 너비. */
    double borderBottom;
    /** Used left border width. 왼쪽 border 너비. */
    double borderLeft;
    /** Used top padding. 위쪽 안쪽 여백(padding). */
    double paddingTop;
    /** Used right padding. 오른쪽 padding. */
    double paddingRight;
    /** Used bottom padding. 아래 padding. */
    double paddingBottom;
    /** Used left padding. 왼쪽 padding. */
    double paddingLeft;
    /** Local baseline offset from the top of the border box (NaN if none); absolute after finishing. 상자 위쪽 기준 기준선(없으면 NaN), 완료 뒤 절대. */
    double baseline = Double.NaN;

    /** Painted text of a TEXT or MARKER fragment. TEXT/MARKER 조각의 그려지는 텍스트. */
    String text;
    /** First source offset in the Text node data. Text 노드 데이터의 첫 원본 위치. */
    int srcStart;
    /** End source offset (exclusive). 원본 끝 위치(제외). */
    int srcEnd;
    /** The rendered legend of a fieldset (its first legend child that is in the flow), or null. fieldset 의 렌더링되는 legend(흐름 안의 첫 legend 자식). 없으면 null. */
    LayoutBox legendChild;
    /** Font of a text fragment. 텍스트 조각의 글꼴. */
    Font font;
    /** True if a text fragment waits for a web font in the block period of its <code>font-display</code>: it is laid out with {@link #font} but not painted. 텍스트 조각이 <code>font-display</code> 의 block 기간에 웹 글꼴을 기다리면 true: {@link #font} 로 배치하되 그리지 않는다. */
    boolean fontBlocked;
    /** Font ascent in pixels. 글꼴 ascent. */
    double ascent;
    /** Font descent in pixels. 글꼴 descent. */
    double descent;
    /** Unicode bidirectional embedding level. 양방향 임베딩 수준. */
    int bidiLevel;
    /** Decoration bits. 장식 비트. */
    int decoration;
    /** Letter spacing applied to the fragment. 조각에 적용된 글자 간격. */
    double letterSpacing;

    /** Content of a replaced element. 치환 요소의 콘텐츠. */
    ReplacedContent replaced;
    /** True if the box clips its content (<code>overflow</code> other than visible). 콘텐츠를 자르는 상자이면 true. */
    boolean clipsContent;
    /** The source of the image that shapes a float whose <code>shape-outside</code> is an image, or null. <code>shape-outside</code> 가 이미지인 float 의 모양을 주는 이미지 공급자. 없으면 null. */
    ShapeImageSource shapeSource;
    /** True if <code>position-visibility</code> hid this anchor-positioned box (the computed <code>visibility</code> is forced to hidden for it and its descendants; CSS Anchor Positioning 1 section 6.6). <code>position-visibility</code> 가 앵커 위치 상자를 숨겼으면 true(상자와 자손의 <code>visibility</code> 계산 값이 hidden 으로 강제된다. CSS Anchor Positioning 1 6.6 절). */
    boolean positionHidden;
    /** Relative positioning offset in x already applied to the geometry. 이미 기하에 반영된 상대 배치 x 오프셋. */
    double relativeX;
    /** Relative positioning offset in y already applied to the geometry. 이미 기하에 반영된 상대 배치 y 오프셋. */
    double relativeY;

    /** Inline content of a block container that establishes an inline formatting context. 인라인 서식 문맥을 만드는 블록의 인라인 콘텐츠. */
    List<InlineItem> items;
    /** Text of the outside list marker or null. 바깥쪽 목록 표식의 텍스트 또는 null. */
    String markerText;
    /** The image of the outside list marker (<code>list-style-image</code>), or null. 바깥쪽 목록 표식의 이미지(<code>list-style-image</code>). 없으면 null. */
    org.duckdns.hjow.swingwebpanel.css.values.CssValue markerImage;
    /** The size {width, height} of the marker image, or null. 표식 이미지의 크기 {너비, 높이}. 없으면 null. */
    double[] markerImageSize;
    /** The style of <code>::marker</code> of a list item, or null (P10-04). 목록 항목의 <code>::marker</code> 스타일. 없으면 null(P10-04). */
    ComputedStyle markerStyle;
    /** The style of <code>::placeholder</code> of a text field or a text area, or null (P10-08). 텍스트 필드나 텍스트 영역의 <code>::placeholder</code> 스타일. 없으면 null(P10-08). */
    ComputedStyle placeholderStyle;
    /** True for a box of the top layer (a modal dialog or its <code>::backdrop</code>): painted above everything else. 최상위 계층의 상자(모달 대화상자나 그 <code>::backdrop</code>)이면 true. 다른 모든 것 위에 그린다. */
    boolean topLayer;
    /** The style of <code>::file-selector-button</code> of a file control, or null (P10-08-h). 파일 컨트롤의 <code>::file-selector-button</code> 스타일. 없으면 null(P10-08-h). */
    ComputedStyle fileButtonStyle;
    /** The properties that the rules of <code>::file-selector-button</code> declare, or null. <code>::file-selector-button</code> 규칙이 선언하는 속성. 없으면 null. */
    java.util.BitSet fileButtonDeclared;
    /** Ordinal of a list item (for the marker). 목록 항목의 번호. */
    int listOrdinal;
    /** True if this box's first-child and last-child margins may not collapse with its own (BFC root). BFC 루트이면 true. */
    boolean bfcRoot;
    /** Collapsed top margin that the parent uses to place this box. 부모가 이 상자를 놓을 때 쓰는 합쳐진 위 margin. */
    Margin outTop;
    /** Collapsed bottom margin that the parent uses. 부모가 쓰는 합쳐진 아래 margin. */
    Margin outBottom;
    /** True if the top and bottom margins collapse through the box. 위/아래 margin 이 상자를 관통해 합쳐지면 true. */
    boolean through;
    /** Cached min-content width (NaN: not computed). 캐시된 min-content 너비. */
    double minContent = Double.NaN;
    /** Cached max-content width (NaN: not computed). 캐시된 max-content 너비. */
    double maxContent = Double.NaN;
    /** Nearest enclosing inline element box for inline items. 인라인 항목을 둘러싼 가장 가까운 인라인 요소 상자. */
    LayoutBox inlineParent;
    /** True if the first edge of this inline element belongs to a line (start edge). 인라인 요소의 시작 가장자리를 가졌으면 true. */
    boolean hasStartEdge = true;

    /** Value of {@link #getFloatSide()} for boxes that do not float. {@link #getFloatSide()} 값: 떠 있지 않은 상자. */
    public static final int FLOAT_NONE = 0;
    /** Value of {@link #getFloatSide()} for left floats. {@link #getFloatSide()} 값: 왼쪽 float. */
    public static final int FLOAT_LEFT = 1;
    /** Value of {@link #getFloatSide()} for right floats. {@link #getFloatSide()} 값: 오른쪽 float. */
    public static final int FLOAT_RIGHT = 2;

    /** Value of {@link #getPositionKind()}: <code>position:static</code>. {@link #getPositionKind()} 의 값으로 static 을 뜻한다. */
    public static final int POS_STATIC = 0;
    /** Value of {@link #getPositionKind()}: <code>position:relative</code>. {@link #getPositionKind()} 의 값으로 relative 를 뜻한다. */
    public static final int POS_RELATIVE = 1;
    /** Value of {@link #getPositionKind()}: <code>position:absolute</code>. {@link #getPositionKind()} 의 값으로 absolute 를 뜻한다. */
    public static final int POS_ABSOLUTE = 2;
    /** Value of {@link #getPositionKind()}: <code>position:fixed</code>. {@link #getPositionKind()} 의 값으로 fixed 를 뜻한다. */
    public static final int POS_FIXED = 3;
    /** Value of {@link #getPositionKind()}: <code>position:sticky</code>. {@link #getPositionKind()} 의 값으로 sticky 를 뜻한다. */
    public static final int POS_STICKY = 4;

    /** Float side (<code>FLOAT_*</code>); only boxes that really float have a value other than {@link #FLOAT_NONE}. float 방향. 실제로 떠 있는 상자만 값이 있다. */
    int floatSide;
    /** Position kind (<code>POS_*</code>) of block-level, atomic and replaced boxes. 블록 수준, 원자적, 치환 상자의 위치 종류. */
    int posKind;
    /** For absolutely positioned boxes: the positioned box that is the containing block, null for the initial containing block or the viewport. 절대 배치 상자의 포함 블록(위치 지정 상자). 초기 포함 블록이나 뷰포트이면 null. */
    LayoutBox absCb;
    /** Absolutely positioned boxes whose containing block is this box, in source order (null if none). 이 상자가 포함 블록인 절대 배치 상자(소스 순서). 없으면 null. */
    List<LayoutBox> absList;
    /** x of the static position relative to the parent border box, set while the parent is laid out. 부모 border box 기준 정적 위치 x. 부모를 배치하는 동안 정해진다. */
    double staticX;
    /** y of the static position relative to the parent border box. 부모 border box 기준 정적 위치 y. */
    double staticY;
    /** For an absolutely positioned child of a positioned grid container: the grid area that is its containing block as {left, right, top, bottom} in the border box of the container, NaN for an edge that is the padding edge (null: the padding box). 위치 지정된 그리드 컨테이너의 절대 배치 자식의 포함 블록인 그리드 영역. 컨테이너 border box 안 {왼쪽, 오른쪽, 위, 아래}이며 padding 가장자리인 쪽은 NaN. null 이면 padding box. */
    double[] gridCb;
    /** Content height forced by the layout of an absolutely positioned box (NaN: none). 절대 배치 상자의 배치가 강제하는 content 높이(NaN 이면 없음). */
    double forcedH = Double.NaN;
    /** Quirks mode fill of the block size: 0 none, 1 for the <code>html</code> element that fills the viewport, 2 for the <code>body</code> element that fills the html element (Quirks Mode Standard 3.6, 3.7); the layout raises the minimum block size of the box (see {@link BlockLayouter#minHeight}). 호환 모드의 블록 크기 채우기: 0 없음, 1 은 뷰포트를 채우는 <code>html</code> 요소, 2 는 html 요소를 채우는 <code>body</code> 요소(호환 모드 표준 3.6, 3.7). 레이아웃이 상자의 최소 블록 크기를 올린다({@link BlockLayouter#minHeight} 참조). */
    int quirkFill;
    /** True if <code>z-index</code> is not <code>auto</code> for a positioned box. 위치 지정 상자의 <code>z-index</code> 가 auto 가 아니면 true. */
    boolean hasZ;
    /** The integer <code>z-index</code> (only meaningful if {@link #hasZ}). 정수 <code>z-index</code>(hasZ 일 때만 의미가 있음). */
    int zIndex;
    /** True if the box forms a stacking context. 쌓임 맥락을 만드는 상자이면 true. */
    boolean stackingContext;
    /** Creation sequence number in source (tree) order, used to order boxes that are painted in tree order. 소스(트리) 순서의 생성 번호. 트리 순서로 그리는 상자의 순서를 정한다. */
    int order;
    /** The table model (a {@link TableModel}) of a {@link Type#TABLE} box, built when it is first needed. {@link Type#TABLE} 상자의 표 모델({@link TableModel}). 처음 필요할 때 만든다. */
    TableModel tableModel;
    /** Grid rectangle of a table relative to its border box: {x, y, width, height} (NaN entries: not laid out). 표 격자의 border box 기준 사각형 {x, y, 너비, 높이}(NaN: 배치 전). */
    double[] grid;
    /** True for a table that uses collapsed borders, and for its rows and cells: their borders are painted by the table. 테두리 합치기를 쓰는 표와 그 행/셀이면 true. 그 border 는 표가 그린다. */
    boolean collapsedBorders;
    /** The collapsed border segments of a table in the coordinates of its border box, null for the separated model. 합쳐진 border 의 표 border box 기준 선분. 분리 모델이면 null. */
    List<TableBorder> collapsedEdges;
    /** True for a table cell that has no content and is hidden by <code>empty-cells:hide</code>. 내용이 없어 <code>empty-cells:hide</code> 로 숨겨지는 셀이면 true. */
    boolean hiddenEmptyCell;
    /** x of a table part in the coordinates of its table while the table is laid out. 표를 배치하는 동안 표 부품의 표 좌표 x. */
    double tx;
    /** y of a table part in the coordinates of its table while the table is laid out. 표를 배치하는 동안 표 부품의 표 좌표 y. */
    double ty;
    /** First grid row of a table cell or row (0-based). 표 셀이나 행의 첫 격자 행(0부터). */
    int tableRow;
    /** First grid column of a table cell or column (0-based). 표 셀이나 열의 첫 격자 열(0부터). */
    int tableCol;
    /** Number of grid rows of a cell. 셀이 차지하는 격자 행 수. */
    int rowSpan = 1;
    /** Number of grid columns of a cell or column. 셀이나 열이 차지하는 격자 열 수. */
    int colSpan = 1;
    /** For text fragments, inline fragments and atomic inline boxes: the innermost inline element they belong to (its <code>position:relative</code> offset moves them). 텍스트 조각, 인라인 조각, 원자적 인라인 상자가 속한 가장 안쪽 인라인 요소(그 <code>position:relative</code> 오프셋이 이들을 옮긴다). */
    LayoutBox inlineOwner;
    /** Rectangle of <code>clip: rect()</code> in the coordinates of the border box as {top, right, bottom, left} (NaN entries: auto), or null. <code>clip: rect()</code> 의 border box 기준 {위, 오른쪽, 아래, 왼쪽}(NaN 은 auto). 없으면 null. */
    double[] clipRect;
    /** True for a flex container (<code>display:flex</code> or <code>inline-flex</code>): its children are flex items and absolutely positioned boxes (P10-03). 플렉스 컨테이너(<code>display:flex</code>, <code>inline-flex</code>)이면 true. 자식은 플렉스 항목과 절대 배치 상자(P10-03). */
    boolean flexContainer;
    /** True for a flex item (a child of a flex container that takes part in the flex layout). 플렉스 항목(플렉스 레이아웃에 참여하는 플렉스 컨테이너의 자식)이면 true. */
    boolean flexItem;
    /** True for a grid container (<code>display:grid</code> or <code>inline-grid</code>): its children are grid items and absolutely positioned boxes (P10-03). 그리드 컨테이너(<code>display:grid</code>, <code>inline-grid</code>)이면 true. 자식은 그리드 항목과 절대 배치 상자(P10-03). */
    boolean gridContainer;
    /** True for a grid item (a child of a grid container that is placed in the grid). 그리드 항목(그리드에 놓이는 그리드 컨테이너의 자식)이면 true. */
    boolean gridItem;
    /** Writing-mode frame of the box (<code>Css.FRAME_*</code>): the layout works in the logical dimensions of this frame (P10-03). 상자의 쓰기 모드 프레임(<code>Css.FRAME_*</code>). 레이아웃은 이 프레임의 논리적 크기로 일한다(P10-03). */
    int frame;
    /** Frame of the containing block of the box, used for everything that faces outwards (margins, outer size). 상자의 포함 블록의 프레임. 바깥을 향하는 것(margin, 바깥 크기)에 쓴다. */
    int parentFrame;
    /** True if the box establishes an orthogonal flow: its frame is perpendicular to that of its containing block. 상자가 직교 흐름을 만들면(프레임이 포함 블록의 프레임과 수직) true. */
    boolean orthRoot;
    /** True while an orthogonal flow root is laid out in its own frame. 직교 흐름 루트를 자신의 프레임으로 배치하는 동안 true. */
    boolean orthActive;
    /** Cache of the content width that an orthogonal flow root contributes in the frame of its containing block (NaN: not computed). 직교 흐름 루트가 포함 블록 프레임에서 기여하는 content 너비의 캐시(NaN: 계산 전). */
    double orthWidth = Double.NaN;
    /** Quarter turns clockwise that the glyphs of a text box are rotated by (0 upright, 1 sideways in vertical text). 텍스트 상자의 글리프가 시계 방향으로 돈 사분원 수(0 똑바로, 1 세로쓰기의 눕힌 글자). */
    int textRot;
    /** How the glyphs of a text box in vertical text are set: one of the <code>GLYPHS_*</code> constants (P10-03-e). 세로쓰기 텍스트 상자의 글리프를 놓는 방식: <code>GLYPHS_*</code> 상수 중 하나(P10-03-e). */
    int glyphMode;
    /** True for a multi-column container (<code>column-count</code> or <code>column-width</code> is not auto): its children are the column boxes (P10-03). 다단 컨테이너(<code>column-count</code> 나 <code>column-width</code> 가 auto 가 아님)이면 true. 자식은 열 상자(P10-03). */
    boolean multicol;
    /** The children that the box builder and the flow layout made, kept while the children are replaced by column boxes so that a second layout of the container starts from them. 열 상자로 바뀌기 전의 원래 자식. 컨테이너를 다시 배치할 때 여기서 시작한다. */
    List<LayoutBox> columnSource;
    /** Rectangles {x, y, width, height} of the column rules relative to the border box (null: none). 열 구분선의 border box 기준 사각형 {x, y, 너비, 높이}(null: 없음). */
    double[][] columnRules;
    /** Number of column boxes of a multi-column container. 다단 컨테이너의 열 상자 수. */
    int columnCount;
    /** True while the layout of a flex item measures its content size: the specified height and the minimum and maximum heights are ignored. 플렉스 항목의 내용 크기를 잴 때 true: 지정 높이와 최소/최대 높이를 무시한다. */
    boolean measureContent;
    /** Width of the static-position rectangle of an absolutely positioned child of a flex container (NaN: the rectangle is a point). 플렉스 컨테이너의 절대 배치 자식의 정적 위치 사각형 너비(NaN: 점). */
    double staticW = Double.NaN;
    /** Height of the static-position rectangle (NaN: a point). 정적 위치 사각형의 높이(NaN: 점). */
    double staticH = Double.NaN;
    /** Alignment of the box in the static-position rectangle horizontally: 0 start, 1 center, 2 end. 정적 위치 사각형 안 가로 정렬: 0 시작, 1 가운데, 2 끝. */
    int staticAlignX;
    /** Alignment of the box in the static-position rectangle vertically: 0 start, 1 center, 2 end. 정적 위치 사각형 안 세로 정렬: 0 시작, 1 가운데, 2 끝. */
    int staticAlignY;
    /** The style of <code>::first-line</code> that applies to the first line of this block container, or null (P10-04-f). 이 블록 컨테이너의 첫 줄에 적용되는 <code>::first-line</code> 스타일. 없으면 null(P10-04-f). */
    ComputedStyle firstLineStyle;
    /** The style of the element that declares <code>::first-line</code> (what the first-line style is compared with), P10-04-f. <code>::first-line</code> 을 선언한 요소의 스타일(첫 줄 스타일과 비교하는 기준), P10-04-f. */
    ComputedStyle firstLineBase;
    /** The transform of this box in document coordinates (origin applied), null if none (P10-06). 이 상자의 문서 좌표 변환(원점 적용). 없으면 null(P10-06). */
    java.awt.geom.AffineTransform transform;
    /** The sticky constraint of a <code>position:sticky</code> box that is shifted while the page scrolls, else null. 페이지가 스크롤되는 동안 옮겨지는 <code>position:sticky</code> 상자의 제약. 아니면 null. */
    StickyConstraint sticky;

    /**
     * Creates a box.
     * <p>
     * 상자를 만든다.
     *
     * @param type  kind / 종류
     * @param node  DOM node or null / DOM 노드 또는 null
     * @param style computed style / 계산 스타일
     */
    LayoutBox(Type type, Node node, ComputedStyle style) {
        this.type = type;
        this.node = node;
        this.style = style;
    }

    /**
     * Appends a child box.
     * <p>
     * 자식 상자를 덧붙인다.
     *
     * @param c the child / 자식
     */
    void add(LayoutBox c) {
        c.parent = this;
        children.add(c);
    }

    /**
     * Returns the kind of the box.
     * <p>
     * 상자의 종류를 반환한다.
     *
     * @return kind / 종류
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the DOM node this box came from: the element for element boxes and pseudo-elements, the Text node for text fragments, null
     * for anonymous boxes and line boxes.
     * <p>
     * 상자의 원본 DOM 노드를 반환한다: 요소 상자와 의사 요소는 요소, 텍스트 조각은 Text 노드, 익명 상자와 줄 상자는 null.
     *
     * @return the node or null / 노드 또는 null
     */
    public Node getNode() {
        return node;
    }

    /**
     * Returns <code>before</code> or <code>after</code> for boxes generated by <code>::before</code>/<code>::after</code>, else null.
     * <p>
     * <code>::before</code>/<code>::after</code> 가 만든 상자이면 <code>before</code> 또는 <code>after</code>, 아니면 null 을 반환한다.
     *
     * @return pseudo-element name or null / 의사 요소 이름 또는 null
     */
    public String getPseudo() {
        return pseudo;
    }

    /**
     * Returns the computed style of the box.
     * <p>
     * 상자의 계산 스타일을 반환한다.
     *
     * @return style / 스타일
     */
    public ComputedStyle getStyle() {
        return style;
    }

    /**
     * Returns the parent box (null for the root).
     * <p>
     * 부모 상자를 반환한다(루트는 null).
     *
     * @return parent / 부모
     */
    public LayoutBox getParent() {
        return parent;
    }

    /**
     * Returns the children in paint order (unmodifiable).
     * <p>
     * 자식을 그리기 순서로 반환한다(수정 불가).
     *
     * @return children / 자식
     */
    public List<LayoutBox> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Tells whether the box is inline-level (an inline fragment, an atomic inline or an inline replaced element).
     * <p>
     * 상자가 인라인 수준(인라인 조각, 원자적 인라인, 인라인 치환 요소)인지 알려 준다.
     *
     * @return true for inline-level / 인라인 수준이면 true
     */
    public boolean isInlineLevel() {
        return inlineLevel;
    }

    /**
     * Returns the absolute x of the border box.
     * <p>
     * border box 의 절대 x 를 반환한다.
     *
     * @return x in CSS pixels / CSS 픽셀 x
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the absolute y of the border box.
     * <p>
     * border box 의 절대 y 를 반환한다.
     *
     * @return y in CSS pixels / CSS 픽셀 y
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the width of the border box.
     * <p>
     * border box 의 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getWidth() {
        return width;
    }

    /**
     * Returns the height of the border box.
     * <p>
     * border box 의 높이를 반환한다.
     *
     * @return height / 높이
     */
    public double getHeight() {
        return height;
    }

    /**
     * Returns the used top margin.
     * <p>
     * 사용된 위 margin 을 반환한다.
     *
     * @return margin / margin
     */
    public double getMarginTop() {
        return marginTop;
    }

    /**
     * Returns the used right margin.
     * <p>
     * 사용된 오른쪽 margin 을 반환한다.
     *
     * @return margin / margin
     */
    public double getMarginRight() {
        return marginRight;
    }

    /**
     * Returns the used bottom margin.
     * <p>
     * 사용된 아래 margin 을 반환한다.
     *
     * @return margin / margin
     */
    public double getMarginBottom() {
        return marginBottom;
    }

    /**
     * Returns the used left margin.
     * <p>
     * 사용된 왼쪽 margin 을 반환한다.
     *
     * @return margin / margin
     */
    public double getMarginLeft() {
        return marginLeft;
    }

    /**
     * Returns the used top border width.
     * <p>
     * 사용된 위 border 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getBorderTop() {
        return borderTop;
    }

    /**
     * Returns the used right border width.
     * <p>
     * 사용된 오른쪽 border 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getBorderRight() {
        return borderRight;
    }

    /**
     * Returns the used bottom border width.
     * <p>
     * 사용된 아래 border 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getBorderBottom() {
        return borderBottom;
    }

    /**
     * Returns the used left border width.
     * <p>
     * 사용된 왼쪽 border 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getBorderLeft() {
        return borderLeft;
    }

    /**
     * Returns the used top padding.
     * <p>
     * 사용된 위 padding 을 반환한다.
     *
     * @return padding / padding
     */
    public double getPaddingTop() {
        return paddingTop;
    }

    /**
     * Returns the used right padding.
     * <p>
     * 사용된 오른쪽 padding 을 반환한다.
     *
     * @return padding / padding
     */
    public double getPaddingRight() {
        return paddingRight;
    }

    /**
     * Returns the used bottom padding.
     * <p>
     * 사용된 아래 padding 을 반환한다.
     *
     * @return padding / padding
     */
    public double getPaddingBottom() {
        return paddingBottom;
    }

    /**
     * Returns the used left padding.
     * <p>
     * 사용된 왼쪽 padding 을 반환한다.
     *
     * @return padding / padding
     */
    public double getPaddingLeft() {
        return paddingLeft;
    }

    /**
     * Returns the absolute x of the content box.
     * <p>
     * content box 의 절대 x 를 반환한다.
     *
     * @return x / x
     */
    public double getContentX() {
        return x + borderLeft + paddingLeft;
    }

    /**
     * Returns the absolute y of the content box.
     * <p>
     * content box 의 절대 y 를 반환한다.
     *
     * @return y / y
     */
    public double getContentY() {
        return y + borderTop + paddingTop;
    }

    /**
     * Returns the width of the content box.
     * <p>
     * content box 의 너비를 반환한다.
     *
     * @return width / 너비
     */
    public double getContentWidth() {
        return Math.max(0, width - borderLeft - borderRight - paddingLeft - paddingRight);
    }

    /**
     * Returns the height of the content box.
     * <p>
     * content box 의 높이를 반환한다.
     *
     * @return height / 높이
     */
    public double getContentHeight() {
        return Math.max(0, height - borderTop - borderBottom - paddingTop - paddingBottom);
    }

    /**
     * Returns the absolute y of the baseline of a line, text fragment, inline fragment, marker, atomic inline or replaced box (NaN where a
     * box has no baseline).
     * <p>
     * 줄, 텍스트 조각, 인라인 조각, 표식, 원자적 인라인, 치환 상자의 기준선 절대 y 를 반환한다(기준선이 없으면 NaN).
     *
     * @return baseline y or NaN / 기준선 y 또는 NaN
     */
    public double getBaseline() {
        return baseline;
    }

    /**
     * Returns the painted text of a {@link Type#TEXT} or {@link Type#MARKER} box: white space is collapsed and <code>text-transform</code>
     * applied; null for other boxes.
     * <p>
     * {@link Type#TEXT} 또는 {@link Type#MARKER} 상자의 그려지는 텍스트를 반환한다. 공백은 접혀 있고 <code>text-transform</code> 이 적용되어 있다. 다른 상자는 null.
     *
     * @return text or null / 텍스트 또는 null
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the start of the source range in the data of the Text node (the first character that the fragment consumed).
     * <p>
     * 조각이 소비한 Text 노드 데이터의 원본 범위 시작을 반환한다.
     *
     * @return offset / 위치
     */
    public int getSourceStart() {
        return srcStart;
    }

    /**
     * Returns the end (exclusive) of the source range in the data of the Text node.
     * <p>
     * Text 노드 데이터의 원본 범위 끝(제외)을 반환한다.
     *
     * @return offset / 위치
     */
    public int getSourceEnd() {
        return srcEnd;
    }

    /**
     * Returns the rendered legend of a fieldset box: the first legend child that is in the normal flow. It sits over the top border of the
     * fieldset, which the painter breaks at the legend.
     * <p>
     * fieldset 상자의 렌더링되는 legend 를 반환한다: 정상 흐름에 있는 첫 legend 자식. fieldset 의 위쪽 경계선 위에 놓이며 그리는 쪽이 legend 에서 경계선을 끊는다.
     *
     * @return the legend box or null / legend 상자 또는 null
     */
    public LayoutBox getRenderedLegend() {
        return legendChild;
    }

    /**
     * Returns the font used to measure and paint a text fragment.
     * <p>
     * 텍스트 조각을 측정하고 그릴 때 쓰는 글꼴을 반환한다.
     *
     * @return font or null / 글꼴 또는 null
     */
    public Font getFont() {
        return font;
    }

    /**
     * Tells whether a text fragment is invisible because it waits for a web font in the block period of its <code>font-display</code> (CSS Fonts 4 section 4.8):
     * the fragment takes the space of the fallback font ({@link #getFont()}) and the renderer does not paint it.
     * <p>
     * 텍스트 조각이 <code>font-display</code> 의 block 기간에 웹 글꼴을 기다려 보이지 않는지 알려 준다(CSS Fonts 4 4.8 절): 조각은 대체 글꼴({@link #getFont()})의 자리를 차지하고
     * 렌더러는 그리지 않는다.
     *
     * @return true if the text is not painted / 텍스트를 그리지 않으면 true
     */
    public boolean isFontBlocked() {
        return fontBlocked;
    }

    /**
     * Returns the ascent of the font of a text fragment.
     * <p>
     * 텍스트 조각 글꼴의 ascent 를 반환한다.
     *
     * @return ascent / ascent
     */
    public double getAscent() {
        return ascent;
    }

    /**
     * Returns the descent of the font of a text fragment.
     * <p>
     * 텍스트 조각 글꼴의 descent 를 반환한다.
     *
     * @return descent / descent
     */
    public double getDescent() {
        return descent;
    }

    /**
     * Returns the Unicode bidirectional embedding level of a text fragment (odd levels run right to left).
     * <p>
     * 텍스트 조각의 유니코드 양방향 임베딩 수준을 반환한다(홀수는 오른쪽에서 왼쪽).
     *
     * @return level / 수준
     */
    public int getBidiLevel() {
        return bidiLevel;
    }

    /**
     * Returns the text decorations that apply to a text fragment as a bit set of the <code>DECORATION_*</code> constants.
     * <p>
     * 텍스트 조각에 적용되는 텍스트 장식을 <code>DECORATION_*</code> 상수의 비트 집합으로 반환한다.
     *
     * @return decoration bits / 장식 비트
     */
    public int getTextDecoration() {
        return decoration;
    }

    /**
     * Returns the letter spacing that was added after every character of a text fragment.
     * <p>
     * 텍스트 조각의 모든 글자 뒤에 더해진 글자 간격을 반환한다.
     *
     * @return spacing in pixels / 픽셀 단위 간격
     */
    public double getLetterSpacing() {
        return letterSpacing;
    }

    /**
     * Returns the content of a replaced element.
     * <p>
     * 치환 요소의 콘텐츠를 반환한다.
     *
     * @return content or null / 콘텐츠 또는 null
     */
    public ReplacedContent getReplacedContent() {
        return replaced;
    }

    /**
     * Tells whether the box clips its content (<code>overflow</code> other than <code>visible</code>); the clip is the padding box.
     * <p>
     * 상자가 콘텐츠를 자르는지(<code>overflow</code> 가 <code>visible</code> 이 아님) 알려 준다. 자르는 영역은 padding box 이다.
     *
     * @return true if content is clipped / 콘텐츠를 자르면 true
     */
    public boolean isClipsContent() {
        return clipsContent;
    }

    /**
     * Returns the horizontal offset of <code>position:relative</code> that is already part of the geometry.
     * <p>
     * 이미 기하에 포함된 <code>position:relative</code> 의 가로 오프셋을 반환한다.
     *
     * @return offset / 오프셋
     */
    public double getRelativeOffsetX() {
        return relativeX;
    }

    /**
     * Returns the vertical offset of <code>position:relative</code> that is already part of the geometry.
     * <p>
     * 이미 기하에 포함된 <code>position:relative</code> 의 세로 오프셋을 반환한다.
     *
     * @return offset / 오프셋
     */
    public double getRelativeOffsetY() {
        return relativeY;
    }

    /**
     * Returns the side a box floats to: {@link #FLOAT_LEFT}, {@link #FLOAT_RIGHT} or {@link #FLOAT_NONE}. An absolutely positioned
     * element never floats.
     * <p>
     * 상자가 떠 있는 방향을 반환한다: {@link #FLOAT_LEFT}, {@link #FLOAT_RIGHT}, {@link #FLOAT_NONE}. 절대 배치 요소는 뜨지 않는다.
     *
     * @return the float side / float 방향
     */
    public int getFloatSide() {
        return floatSide;
    }

    /**
     * Tells whether the box is a float.
     * <p>
     * 상자가 float 인지 알려 준다.
     *
     * @return true for floats / float 이면 true
     */
    public boolean isFloat() {
        return floatSide != FLOAT_NONE;
    }

    /**
     * Returns the position kind of the box: one of the <code>POS_*</code> constants.
     * <p>
     * 상자의 위치 종류를 반환한다: <code>POS_*</code> 상수 중 하나.
     *
     * @return the position kind / 위치 종류
     */
    public int getPositionKind() {
        return posKind;
    }

    /**
     * Tells whether the box is positioned (<code>position</code> other than <code>static</code>).
     * <p>
     * 상자가 위치 지정 상자(<code>position</code> 이 <code>static</code> 이 아님)인지 알려 준다.
     *
     * @return true if positioned / 위치 지정이면 true
     */
    public boolean isPositioned() {
        return posKind != POS_STATIC;
    }

    /**
     * Tells whether the box is absolutely positioned (<code>absolute</code> or <code>fixed</code>), that is taken out of the flow.
     * <p>
     * 상자가 절대 배치(<code>absolute</code> 또는 <code>fixed</code>)되어 흐름에서 빠졌는지 알려 준다.
     *
     * @return true if out of the flow by position / 위치로 흐름에서 빠졌으면 true
     */
    public boolean isAbsolutelyPositioned() {
        return posKind == POS_ABSOLUTE || posKind == POS_FIXED;
    }

    /**
     * Tells whether the box is fixed to the viewport (<code>position:fixed</code>): the renderer keeps it at the same place of the viewport
     * when the page scrolls. The geometry of such a box is the one at scroll offset 0.
     * <p>
     * 상자가 뷰포트에 고정(<code>position:fixed</code>)되었는지 알려 준다: 렌더러는 페이지가 스크롤되어도 뷰포트의 같은 자리에 둔다. 이런 상자의 기하는
     * 스크롤 오프셋 0 일 때의 값이다.
     *
     * @return true for fixed boxes / fixed 이면 true
     */
    public boolean isFixedPosition() {
        // 변환된 조상이 있으면 그 조상이 포함 블록이므로(CSS Transforms 1 2 절) 뷰포트에 고정되지 않고 페이지와 함께 스크롤된다.
        return posKind == POS_FIXED && absCb == null;
    }

    /**
     * Returns the 2D transform of the box in document coordinates (<code>transform</code> applied around <code>transform-origin</code>,
     * plan item P10-06): the matrix that maps the laid-out coordinates of the box and its content to where they are painted, in the
     * coordinates of the parent (the transforms of the ancestors are not included). The layout is not changed by a transform.
     * <p>
     * 상자의 2D 변환을 문서 좌표로 반환한다(<code>transform-origin</code> 둘레에 적용한 <code>transform</code>, 계획 P10-06): 상자와 그 내용의 배치된 좌표를 그려지는 곳으로
     * 옮기는 행렬이며 부모의 좌표로 나타낸다(조상의 변환은 포함하지 않는다). 변환은 배치를 바꾸지 않는다.
     *
     * @return a copy of the transform, or null if the box has none / 변환의 사본. 없으면 null
     */
    public java.awt.geom.AffineTransform getTransform() {
        return transform == null ? null : new java.awt.geom.AffineTransform(transform);
    }

    /**
     * Tells whether a text box belongs to the inline element that this inline fragment is a part of (the text is in the element or in an inline element
     * inside it). The text boxes of an inline element are siblings of its fragment in the line box, not its children, so the renderer asks this
     * to find the text of an inline element (<code>background-clip: text</code>).
     * <p>
     * 텍스트 상자가 이 인라인 조각이 속한 인라인 요소에 속하는지(요소 안이나 그 안의 인라인 요소 안에 있는 텍스트) 알려 준다. 인라인 요소의 텍스트 상자는 줄 상자 안에서
     * 조각의 자식이 아니라 형제이므로, 렌더러는 인라인 요소의 텍스트를 찾을 때 이것을 묻는다(<code>background-clip: text</code>).
     *
     * @param text the TEXT box / TEXT 상자
     * @return true if the text is inside the inline element of this fragment / 텍스트가 이 조각의 인라인 요소 안에 있으면 true
     */
    public boolean isTextOfInline(LayoutBox text) {
        if (type != Type.INLINE || inlineOwner == null || text == null) {
            return false;
        }
        // 텍스트가 속한 인라인 요소의 사슬(안쪽부터)에 이 조각의 요소가 있는지 찾는다.
        for (LayoutBox o = text.inlineOwner; o != null; o = o.inlineParent) {
            if (o == inlineOwner) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the sticky constraint of the box: the box is <code>position:sticky</code> with at least one inset other than <code>auto</code>
     * and can be shifted. The renderer shifts it by {@link StickyConstraint#shift} of the viewport while the page scrolls; the geometry of the
     * box is the one without the shift.
     * <p>
     * 상자의 sticky 제약을 반환한다: 상자가 <code>position:sticky</code> 이고 <code>auto</code> 가 아닌 오프셋이 하나 이상 있어 옮길 수 있는 경우이다. 렌더러는 페이지가
     * 스크롤되는 동안 뷰포트에 대한 {@link StickyConstraint#shift} 만큼 옮긴다. 상자의 기하는 옮기기 전의 것이다.
     *
     * @return the constraint or null / 제약 또는 null
     */
    public StickyConstraint getSticky() {
        return sticky;
    }

    /**
     * Tells whether a box or an ancestor of it has a transform, and returns the transform of the box in the coordinates of the document:
     * the transforms of the ancestors applied after the transform of the box itself.
     * <p>
     * 상자나 그 조상에 변환이 있는지 알려 주며 상자의 변환을 문서 좌표로 반환한다: 상자 자신의 변환 뒤에 조상의 변환을 적용한 것.
     *
     * @return the full transform, or null if neither the box nor an ancestor has one / 전체 변환. 상자도 조상도 변환이 없으면 null
     */
    public java.awt.geom.AffineTransform getFullTransform() {
        java.awt.geom.AffineTransform full = null;
        // 상자에서 루트까지 올라가며 변환을 안쪽 것부터 바깥쪽 순서로 이어 붙인다.
        for (LayoutBox b = this; b != null; b = b.parent) {
            if (b.transform != null) {
                if (full == null) {
                    full = new java.awt.geom.AffineTransform(b.transform);
                } else {
                    java.awt.geom.AffineTransform outer = new java.awt.geom.AffineTransform(b.transform);
                    outer.concatenate(full);
                    full = outer;
                }
            }
        }
        return full;
    }

    /**
     * Returns the box that is the containing block of an absolutely positioned box (the nearest positioned ancestor box), or null for the
     * initial containing block (or the viewport for fixed boxes). For all other boxes the result is the parent block box.
     * <p>
     * 절대 배치 상자의 포함 블록 상자(가장 가까운 위치 지정 조상 상자)를 반환한다. 초기 포함 블록(fixed 이면 뷰포트)이면 null. 다른 상자는 부모 블록 상자이다.
     *
     * @return the containing block box or null / 포함 블록 상자 또는 null
     */
    public LayoutBox getContainingBlock() {
        if (isAbsolutelyPositioned()) {
            return absCb;
        }
        LayoutBox p = parent;
        // 줄 상자와 인라인 조각을 건너뛰어 가장 가까운 블록 컨테이너를 찾는다.
        while (p != null && (p.type == Type.LINE || p.type == Type.INLINE)) {
            p = p.parent;
        }
        return p;
    }

    /**
     * Tells whether <code>z-index</code> is an integer for this positioned box (not <code>auto</code>).
     * <p>
     * 위치 지정 상자의 <code>z-index</code> 가 정수(<code>auto</code> 가 아님)인지 알려 준다.
     *
     * @return true if an integer z-index applies / 정수 z-index 가 적용되면 true
     */
    public boolean hasZIndex() {
        return hasZ;
    }

    /**
     * Returns the integer <code>z-index</code> (0 if {@link #hasZIndex()} is false).
     * <p>
     * 정수 <code>z-index</code> 를 반환한다({@link #hasZIndex()} 가 false 이면 0).
     *
     * @return the z-index / z-index
     */
    public int getZIndex() {
        return hasZ ? zIndex : 0;
    }

    /**
     * Tells whether the box forms a stacking context: the root, positioned boxes with an integer <code>z-index</code>, fixed boxes and
     * boxes with <code>opacity</code> below 1.
     * <p>
     * 상자가 쌓임 맥락을 만드는지 알려 준다: 루트, 정수 <code>z-index</code> 를 가진 위치 지정 상자, fixed 상자, <code>opacity</code> 가 1 보다 작은 상자.
     *
     * @return true if a stacking context / 쌓임 맥락이면 true
     */
    public boolean isStackingContext() {
        return stackingContext;
    }

    /**
     * Returns the sequence number of the box in source (tree) order; boxes that are painted in tree order are ordered by it.
     * <p>
     * 소스(트리) 순서의 상자 번호를 반환한다. 트리 순서로 그리는 상자는 이 번호로 순서가 정해진다.
     *
     * @return the sequence number / 번호
     */
    public int getOrder() {
        return order;
    }

    /**
     * Tells whether the box is a flex container (<code>display:flex</code> or <code>inline-flex</code>).
     * <p>
     * 상자가 플렉스 컨테이너(<code>display:flex</code> 또는 <code>inline-flex</code>)인지 알려 준다.
     *
     * @return true for flex containers / 플렉스 컨테이너이면 true
     */
    public boolean isFlexContainer() {
        return flexContainer;
    }

    /**
     * Tells whether the box is a flex item. Flex items are painted like inline blocks, in the order-modified document order, which is the
     * order of the children of their flex container.
     * <p>
     * 상자가 플렉스 항목인지 알려 준다. 플렉스 항목은 inline-block 처럼, order 를 반영한 문서 순서(플렉스 컨테이너 자식의 순서)로 그린다.
     *
     * @return true for flex items / 플렉스 항목이면 true
     */
    public boolean isFlexItem() {
        return flexItem;
    }

    /**
     * Tells whether the box is a grid container (<code>display:grid</code> or <code>inline-grid</code>).
     * <p>
     * 상자가 그리드 컨테이너(<code>display:grid</code> 또는 <code>inline-grid</code>)인지 알려 준다.
     *
     * @return true for grid containers / 그리드 컨테이너이면 true
     */
    public boolean isGridContainer() {
        return gridContainer;
    }

    /**
     * Tells whether the box is a grid item. Grid items are painted like inline blocks, in the order-modified document order, which is the
     * order of the children of their grid container.
     * <p>
     * 상자가 그리드 항목인지 알려 준다. 그리드 항목은 inline-block 처럼, order 를 반영한 문서 순서(그리드 컨테이너 자식의 순서)로 그린다.
     *
     * @return true for grid items / 그리드 항목이면 true
     */
    public boolean isGridItem() {
        return gridItem;
    }

    /**
     * Tells whether the box is a multi-column container; its children are the column boxes (anonymous blocks, one per column).
     * <p>
     * 상자가 다단 컨테이너인지 알려 준다. 자식은 열 상자(열마다 익명 블록 하나)이다.
     *
     * @return true for multi-column containers / 다단 컨테이너이면 true
     */
    public boolean isMulticolContainer() {
        return multicol;
    }

    /**
     * Tells whether the box is laid out in a vertical writing mode (<code>vertical-rl</code>, <code>vertical-lr</code>,
     * <code>sideways-rl</code>, <code>sideways-lr</code>).
     * <p>
     * 상자가 세로 쓰기 모드(<code>vertical-rl</code>, <code>vertical-lr</code>, <code>sideways-rl</code>, <code>sideways-lr</code>)로 배치되는지 알려 준다.
     *
     * @return true for vertical boxes / 세로 상자이면 true
     */
    public boolean isVertical() {
        return frame != Css.FRAME_H;
    }

    /**
     * Tells whether the box establishes an orthogonal flow (its writing mode is perpendicular to that of its containing block).
     * <p>
     * 상자가 직교 흐름을 만드는지(쓰기 모드가 포함 블록의 쓰기 모드와 수직인지) 알려 준다.
     *
     * @return true for orthogonal flow roots / 직교 흐름 루트이면 true
     */
    public boolean isOrthogonalFlowRoot() {
        return orthRoot;
    }

    /**
     * Returns the rotation of the glyphs of a text fragment or a list marker in quarter turns clockwise: 0 for text on a horizontal
     * line, 1 for sideways text in a vertical line of <code>vertical-rl</code> or <code>vertical-lr</code> (the tops of the glyphs
     * point to the right) and 3 for <code>sideways-lr</code> (the tops point to the left).
     * <p>
     * 텍스트 조각이나 목록 표식의 글리프 회전을 시계 방향 사분원 수로 반환한다: 가로 줄의 텍스트는 0, <code>vertical-rl</code> 이나 <code>vertical-lr</code> 의 세로 줄에서
     * 눕힌 텍스트는 1(글자의 위쪽이 오른쪽을 향함), <code>sideways-lr</code> 은 3(위쪽이 왼쪽을 향함).
     *
     * @return the number of quarter turns / 사분원 수
     */
    public int getTextRotation() {
        return textRot;
    }

    /**
     * Returns the advance of a text fragment along its line: the width of the fragment for text on a horizontal line and its height
     * for rotated text.
     * <p>
     * 텍스트 조각의 줄 방향 advance 를 반환한다: 가로 줄의 텍스트는 조각의 너비, 회전한 텍스트는 높이이다.
     *
     * @return the advance / advance
     */
    public double getTextAdvance() {
        return (textRot & 1) == 0 ? width : height;
    }

    /**
     * Returns how the glyphs of a text fragment of vertical text are set (CSS Writing Modes Level 3): {@link #GLYPHS_SIDEWAYS} (the
     * fragment is one run that is rotated, see {@link #getTextRotation()}; also every fragment of horizontal text), {@link #GLYPHS_UPRIGHT}
     * (the characters stand upright, one cell of one em along the line for each character cluster, see {@link #getClusterOffsets()}) or
     * {@link #GLYPHS_COMBINED} (<code>text-combine-upright</code>: the text is composed horizontally into the space of one upright
     * character, one em along the line).
     * <p>
     * 세로쓰기 텍스트 조각의 글리프를 놓는 방식을 반환한다(CSS Writing Modes Level 3): {@link #GLYPHS_SIDEWAYS}(조각이 회전한 하나의 구간이다.
     * {@link #getTextRotation()} 참조. 가로쓰기 텍스트의 모든 조각도 같다), {@link #GLYPHS_UPRIGHT}(글자가 똑바로 서며 글자 클러스터마다 줄을 따라 1em 한 칸이다.
     * {@link #getClusterOffsets()} 참조), {@link #GLYPHS_COMBINED}(<code>text-combine-upright</code>: 텍스트를 가로로 짜서 똑바로 선 글자 하나의 공간, 줄을 따라
     * 1em 에 넣는다).
     *
     * @return one of the <code>GLYPHS_*</code> constants / <code>GLYPHS_*</code> 상수 중 하나
     */
    public int getGlyphMode() {
        return glyphMode;
    }

    /**
     * Returns the start offsets of the character clusters of the text of an upright fragment followed by the length of the text: cluster
     * <i>k</i> is the range from element <i>k</i> to element <i>k+1</i> and takes the <i>k</i>-th cell of the fragment along the line.
     * <p>
     * 똑바로 선 조각의 텍스트의 글자 클러스터 시작 위치와 마지막에 텍스트 길이를 반환한다: 클러스터 <i>k</i> 는 원소 <i>k</i> 부터 <i>k+1</i> 까지의 범위이며 조각의
     * 줄을 따라 <i>k</i> 번째 칸을 차지한다.
     *
     * @return the offsets (a new array) / 위치(새 배열)
     */
    public int[] getClusterOffsets() {
        return VerticalText.clusterOffsets(text == null ? "" : text);
    }

    /**
     * Returns the number of columns that a multi-column container was laid out in (the used columns, including the overflow columns that
     * appear when the content does not fit the height).
     * <p>
     * 다단 컨테이너가 배치된 열의 수를 반환한다(내용이 높이에 들어가지 않을 때 생기는 넘침 열 포함).
     *
     * @return the number of column boxes / 열 상자 수
     */
    public int getColumnCount() {
        return columnCount;
    }

    /**
     * Returns the column rules of a multi-column container in document coordinates as {x, y, width, height} rectangles (empty if the
     * rule style is none or the container has fewer than two columns). The renderer paints each rectangle as a left border of the width
     * and style of <code>column-rule</code>.
     * <p>
     * 다단 컨테이너의 열 구분선을 문서 좌표의 {x, y, 너비, 높이} 사각형으로 반환한다(구분선 스타일이 none 이거나 열이 둘 미만이면 비어 있음). 렌더러는 각 사각형을
     * <code>column-rule</code> 의 너비와 스타일을 가진 왼쪽 border 로 그린다.
     *
     * @return the rectangles (a copy) / 사각형(복사본)
     */
    public double[][] getColumnRules() {
        if (columnRules == null) {
            return new double[0][];
        }
        double[][] r = new double[columnRules.length][];
        // 상자 기준 사각형을 문서 좌표로 옮긴다.
        for (int i = 0; i < r.length; i++) {
            r[i] = new double[] { x + columnRules[i][0], y + columnRules[i][1], columnRules[i][2], columnRules[i][3] };
        }
        return r;
    }

    /**
     * Creates the fragment of a box that is split between columns: a new box of the same kind, node and style with the same edges,
     * flags and paint-order information but no children, no text and no position. The caller sets the geometry and the children.
     * <p>
     * 열에 걸쳐 나뉘는 상자의 조각을 만든다: 같은 종류, 노드, 스타일, 가장자리, 표시, 그리기 순서 정보를 가지고 자식, 텍스트, 위치가 없는 새 상자. 호출자가 기하와 자식을
     * 정한다.
     *
     * @return the new box / 새 상자
     */
    LayoutBox fragmentCopy() {
        LayoutBox f = new LayoutBox(type, node, style);
        f.pseudo = pseudo;
        f.inlineLevel = inlineLevel;
        f.marginTop = marginTop;
        f.marginRight = marginRight;
        f.marginBottom = marginBottom;
        f.marginLeft = marginLeft;
        f.borderTop = borderTop;
        f.borderRight = borderRight;
        f.borderBottom = borderBottom;
        f.borderLeft = borderLeft;
        f.paddingTop = paddingTop;
        f.paddingRight = paddingRight;
        f.paddingBottom = paddingBottom;
        f.paddingLeft = paddingLeft;
        f.decoration = decoration;
        f.clipsContent = clipsContent;
        f.bfcRoot = bfcRoot;
        f.floatSide = floatSide;
        f.shapeSource = shapeSource;
        f.posKind = posKind;
        f.hasZ = hasZ;
        f.zIndex = zIndex;
        f.stackingContext = stackingContext;
        f.order = order;
        f.clipRect = clipRect;
        f.absCb = absCb;
        f.flexItem = flexItem;
        f.gridItem = gridItem;
        f.frame = frame;
        f.parentFrame = parentFrame;
        f.orthRoot = orthRoot;
        f.listOrdinal = listOrdinal;
        f.width = width;
        return f;
    }

    /**
     * Returns the grid rectangle of a table box in document coordinates as {x, y, width, height}: the area that the table background, the
     * table border and the cells occupy (the captions are outside of it). For other boxes it is the border box.
     * <p>
     * 표 상자의 격자 사각형을 문서 좌표의 {x, y, 너비, 높이}로 반환한다: 표 배경, 표 border, 셀이 차지하는 영역(caption 은 그 밖). 다른 상자는 border box 이다.
     *
     * @return the rectangle / 사각형
     */
    public double[] getGridRectangle() {
        if (grid == null) {
            return new double[] { x, y, width, height };
        }
        return new double[] { x + grid[0], y + grid[1], grid[2], grid[3] };
    }

    /**
     * Tells whether the borders of a table, its rows and its cells are collapsed (<code>border-collapse:collapse</code>): they are then
     * painted by the table as {@link #getCollapsedBorders() segments}, not by the boxes.
     * <p>
     * 표, 그 행, 그 셀의 border 가 합쳐지는지(<code>border-collapse:collapse</code>) 알려 준다. 그러면 상자가 아니라 표가 {@link #getCollapsedBorders() 선분}으로 그린다.
     *
     * @return true if the borders are collapsed / 합쳐지면 true
     */
    public boolean isCollapsedBorderModel() {
        return collapsedBorders;
    }

    /**
     * Returns the collapsed border segments of a table in document coordinates (empty for other boxes and for the separated model).
     * <p>
     * 표의 합쳐진 border 선분을 문서 좌표로 반환한다(다른 상자와 분리 모델은 빈 목록).
     *
     * @return the segments in painting order / 그리기 순서의 선분
     */
    public List<TableBorder> getCollapsedBorders() {
        if (collapsedEdges == null) {
            return Collections.<TableBorder>emptyList();
        }
        List<TableBorder> out = new ArrayList<TableBorder>(collapsedEdges.size());
        // 표 border box 기준 선분을 문서 좌표로 옮긴다.
        for (int i = 0; i < collapsedEdges.size(); i++) {
            out.add(collapsedEdges.get(i).moved(x, y));
        }
        return out;
    }

    /**
     * Tells whether a table cell is hidden because it is empty and <code>empty-cells:hide</code> applies: neither its background nor
     * its border is painted.
     * <p>
     * 셀이 비어 있고 <code>empty-cells:hide</code> 가 적용되어 숨겨지는지 알려 준다: 배경도 border 도 그리지 않는다.
     *
     * @return true if hidden / 숨겨지면 true
     */
    public boolean isHiddenEmptyCell() {
        return hiddenEmptyCell;
    }

    /**
     * Returns the first grid row of a table cell or row (0-based).
     * <p>
     * 표 셀이나 행의 첫 격자 행(0부터)을 반환한다.
     *
     * @return the row index / 행 번호
     */
    public int getTableRow() {
        return tableRow;
    }

    /**
     * Returns the first grid column of a table cell or column (0-based).
     * <p>
     * 표 셀이나 열의 첫 격자 열(0부터)을 반환한다.
     *
     * @return the column index / 열 번호
     */
    public int getTableColumn() {
        return tableCol;
    }

    /**
     * Returns the number of grid rows that a table cell spans (after the limit to its row group).
     * <p>
     * 표 셀이 차지하는 격자 행 수를 반환한다(행 그룹 한도 적용 후).
     *
     * @return the row span / 행 수
     */
    public int getRowSpan() {
        return rowSpan;
    }

    /**
     * Returns the number of grid columns that a table cell or column spans.
     * <p>
     * 표 셀이나 열이 차지하는 격자 열 수를 반환한다.
     *
     * @return the column span / 열 수
     */
    public int getColumnSpan() {
        return colSpan;
    }

    /**
     * Returns the rectangle of <code>clip: rect()</code> of an absolutely positioned box in document coordinates as {x, y, width, height},
     * or null if the box has no such clip.
     * <p>
     * 절대 배치 상자의 <code>clip: rect()</code> 영역을 문서 좌표의 {x, y, 너비, 높이}로 반환한다. 그런 자르기가 없으면 null.
     *
     * @return the rectangle or null / 사각형 또는 null
     */
    public double[] getClipRectangle() {
        if (clipRect == null) {
            return null;
        }
        double l = Double.isNaN(clipRect[3]) ? 0 : clipRect[3];
        double t = Double.isNaN(clipRect[0]) ? 0 : clipRect[0];
        double r = Double.isNaN(clipRect[1]) ? width : clipRect[1];
        double b = Double.isNaN(clipRect[2]) ? height : clipRect[2];
        return new double[] { x + l, y + t, Math.max(0, r - l), Math.max(0, b - t) };
    }

    /**
     * Tells whether a point (document coordinates) lies inside the border box.
     * <p>
     * 점(문서 좌표)이 border box 안에 있는지 알려 준다.
     *
     * @param px x / x
     * @param py y / y
     * @return true if inside / 안에 있으면 true
     */
    public boolean contains(double px, double py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    /**
     * Returns a short description for diagnostics.
     * <p>
     * 진단용 짧은 설명을 반환한다.
     *
     * @return description / 설명
     */
    public String toString() {
        return type + "[" + x + "," + y + " " + width + "x" + height + (text != null ? " '" + text + "'" : "") + "]";
    }
    /**
     * Returns the computed style of the <code>::placeholder</code> pseudo-element of a text field or a text area (the color of the
     * placeholder text), or null for the other boxes.
     * <p>
     * 텍스트 필드나 텍스트 영역의 <code>::placeholder</code> 의사 요소의 계산 스타일(placeholder 글의 색)을 반환한다. 다른 상자는 null.
     *
     * @return the style or null / 스타일. 없으면 null
     */
    public ComputedStyle getPlaceholderStyle() {
        return placeholderStyle;
    }

    /**
     * Returns the image that a MARKER box shows (<code>list-style-image</code>), or null for a text marker.
     * <p>
     * MARKER 상자가 보여 주는 이미지(<code>list-style-image</code>)를 반환한다. 텍스트 표식이면 null.
     *
     * @return the image value or null / 이미지 값 또는 null
     */
    public org.duckdns.hjow.swingwebpanel.css.values.CssValue getMarkerImage() {
        return markerImage;
    }

    /**
     * Returns the computed style of the <code>::file-selector-button</code> pseudo-element of a file control (the button that the painter draws), or null if no
     * rule targets it or the box is not a file control.
     * <p>
     * 파일 컨트롤의 <code>::file-selector-button</code> 의사 요소(그리는 쪽이 그리는 단추)의 계산 스타일을 반환한다. 그것을 가리키는 규칙이 없거나 파일 컨트롤이 아니면 null.
     *
     * @return the style or null / 스타일. 없으면 null
     */
    public ComputedStyle getFileButtonStyle() {
        return fileButtonStyle;
    }

    /**
     * Returns the properties that the rules of <code>::file-selector-button</code> declare (the painter keeps its own look for the others), or null.
     * <p>
     * <code>::file-selector-button</code> 규칙이 선언하는 속성을 반환한다(그리는 쪽은 나머지 속성에 대해 자신의 모양을 지킨다). 없으면 null.
     *
     * @return a copy of the property ids, or null / 속성 id 의 복사본. 없으면 null
     */
    public java.util.BitSet getFileButtonDeclared() {
        return fileButtonDeclared == null ? null : (java.util.BitSet) fileButtonDeclared.clone();
    }
}
