package org.duckdns.hjow.swingwebpanel.layout;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * Block formatting (plan item P09-02): CSS 2.1 normal flow of block-level boxes: used widths and horizontal margins (auto, percentage,
 * <code>min-width</code>/<code>max-width</code>, <code>box-sizing</code>, auto margins), heights (auto, specified, minimum, maximum),
 * margin collapsing between siblings, between parent and first/last child and through empty boxes, shrink-to-fit widths for atomic inline
 * boxes, sizes of replaced elements and the outside list marker. Inline content is handed to {@link InlineLayouter}. All coordinates set
 * here are relative to the parent border box ({@link LayoutBox#lx}/{@link LayoutBox#ly}); {@link LayoutEngine} converts them to document
 * coordinates afterwards. Floats and absolutely positioned boxes (P10-01) are not children of the normal flow: a float is placed beside the
 * lines by the inline layout (or by this class for its content), and an absolutely positioned box is laid out by {@link AbsoluteLayouter}
 * when its containing block has been laid out. Every block formatting context root owns a {@link FloatContext}; the position of a box in
 * that context is not known before its margins have collapsed, so it is estimated first, and a subtree whose estimate turned out wrong
 * is laid out a second time when floats could be affected (see {@link #layoutChildren}).
 * <p>
 * 블록 서식(계획 P09-02): 블록 수준 상자의 CSS 2.1 정상 흐름: 사용 너비와 가로 margin(auto, 백분율, <code>min-width</code>/<code>max-width</code>,
 * <code>box-sizing</code>, auto margin), 높이(auto, 지정, 최소, 최대), 형제 사이, 부모와 첫/마지막 자식 사이, 빈 상자를 관통하는 margin 합치기, 원자적 인라인
 * 상자의 shrink-to-fit 너비, 치환 요소의 크기, 바깥쪽 목록 표식. 인라인 콘텐츠는 {@link InlineLayouter} 에 넘긴다. 여기서 정하는 좌표는 모두 부모 border
 * box 기준({@link LayoutBox#lx}/{@link LayoutBox#ly})이며 {@link LayoutEngine} 이 나중에 문서 좌표로 바꾼다. float 와 절대 배치 상자(P10-01)는 정상 흐름의
 * 자식이 아니다: float 는 인라인 레이아웃이(또는 이 클래스가 그 내용을 위해) 줄 옆에 놓고, 절대 배치 상자는 포함 블록의 배치가 끝난 뒤
 * {@link AbsoluteLayouter} 가 배치한다. 블록 서식 문맥의 루트는 각각 {@link FloatContext} 를 가진다. 상자의 그 문맥 안 위치는 margin 이 합쳐지기 전에는 알 수
 * 없으므로 먼저 추정하고, 추정이 틀렸고 float 가 영향을 받을 수 있으면 그 하위 트리를 한 번 더 배치한다({@link #layoutChildren} 참조).
 */
final class BlockLayouter {

    /** Measures text. 텍스트 측정기. */
    private final TextMeasure tm;
    /** The inline layout engine. 인라인 레이아웃. */
    final InlineLayouter inline;
    /** Tolerance when comparing positions. 위치 비교 허용 오차. */
    private static final double EPS = 0.01;
    /** Maximum number of subtrees that are laid out a second time in one layout run. 한 번의 배치에서 두 번째로 배치하는 하위 트리의 최대 수. */
    private static final int RELAYOUT_BUDGET = 4000;
    /** Float context of the block formatting context that is being laid out. 배치 중인 블록 서식 문맥의 float 문맥. */
    FloatContext fc;
    /** x of the content box of the box that is being laid out, in the coordinates of {@link #fc}. {@link #fc} 좌표로 배치 중인 상자의 content box x. */
    double fcx;
    /** y of the content box of the box that is being laid out, in the coordinates of {@link #fc}. {@link #fc} 좌표로 배치 중인 상자의 content box y. */
    double fcy;
    /** Estimated x of the border box of the box that is about to be laid out, in the coordinates of {@link #fc}. 곧 배치할 상자의 border box 추정 x. */
    double nextX;
    /** Estimated y of the border box of the box that is about to be laid out, in the coordinates of {@link #fc}. 곧 배치할 상자의 border box 추정 y. */
    double nextY;
    /** Number of second layouts that may still be done. 아직 할 수 있는 두 번째 배치의 수. */
    private int relayouts = RELAYOUT_BUDGET;
    /** Lays out absolutely positioned boxes. 절대 배치 상자를 배치한다. */
    final AbsoluteLayouter absolute;
    /** Lays out tables (P10-02). 표를 배치한다(P10-02). */
    final TableLayouter tables;
    /** Lays out flex containers (P10-03). 플렉스 컨테이너를 배치한다(P10-03). */
    final FlexLayouter flex;
    /** Lays out multi-column containers (P10-03). 다단 컨테이너를 배치한다(P10-03). */
    final MulticolLayouter multicol;
    /** Lays out grid containers (P10-03). 그리드 컨테이너를 배치한다(P10-03). */
    final GridLayouter grid;
    /** Lays out orthogonal flow roots (vertical text inside horizontal text and vice versa, P10-03). 직교 흐름 루트(가로 안의 세로쓰기와 그 반대, P10-03)를 배치한다. */
    final OrthogonalLayouter orthogonal;
    /** Width of the viewport of this layout run (the fallback size of orthogonal flows). 이 레이아웃 실행의 뷰포트 너비(직교 흐름의 대체 크기). */
    double viewportW;
    /** Height of the viewport of this layout run (the fallback size of orthogonal flows). 이 레이아웃 실행의 뷰포트 높이(직교 흐름의 대체 크기). */
    double viewportH;
    /** The position options (<code>@position-try</code>) of the style sheets, or null if the style source does not know them. 스타일 시트의 위치 대안(<code>@position-try</code>). 스타일 공급자가 모르면 null. */
    PositionTrySource positionTry;
    /**
     * True if the document is in quirks mode or limited-quirks mode: the line height quirks of the Quirks Mode Standard (3.3 and 3.4,
     * no strut for lines without text of their own and inline boxes without text act as zero line height) apply.
     * 문서가 호환 모드나 제한적 호환 모드이면 true: 호환 모드 표준의 줄 높이 quirk(3.3, 3.4: 자기 텍스트가 없는 줄에는 strut 가 없고 텍스트 없는 인라인 상자는 줄 높이 0 으로 본다)가 적용된다.
     */
    boolean lineQuirks;
    /**
     * True if the document is in quirks mode (not limited-quirks): the table cell height quirk (Quirks Mode Standard 3.13), the percentage
     * height quirk (3.5) and the nowrap cell quirk (3.9) apply.
     * 문서가 호환 모드(제한적 호환 모드 제외)이면 true: 표 셀 높이 quirk(호환 모드 표준 3.13), 백분율 높이 quirk(3.5), nowrap 셀 quirk(3.9)가 적용된다.
     */
    boolean quirksMode;

    /**
     * Creates the block layouter.
     * <p>
     * 블록 레이아웃을 만든다.
     *
     * @param tm text measurement of this layout run / 이 레이아웃 실행의 텍스트 측정기
     */
    BlockLayouter(TextMeasure tm) {
        this.tm = tm;
        this.inline = new InlineLayouter(tm, this);
        this.absolute = new AbsoluteLayouter(this);
        this.tables = new TableLayouter(this);
        this.flex = new FlexLayouter(this);
        this.multicol = new MulticolLayouter(this);
        this.grid = new GridLayouter(this);
        this.orthogonal = new OrthogonalLayouter(this);
    }

    /**
     * Replaces NaN by zero.
     * <p>
     * NaN 을 0 으로 바꾼다.
     *
     * @param v value / 값
     * @return value or 0 / 값 또는 0
     */
    static double nz(double v) {
        return Double.isNaN(v) ? 0 : v;
    }

    /**
     * Tells whether the box ignores its own style for box properties (anonymous boxes).
     * <p>
     * 상자가 상자 속성에 자신의 스타일을 무시하는지(익명 상자) 알려 준다.
     *
     * @param b the box / 상자
     * @return true for anonymous boxes / 익명 상자이면 true
     */
    private static boolean anon(LayoutBox b) {
        return b.type == LayoutBox.Type.ANONYMOUS_BLOCK || (b.node == null && b.type == LayoutBox.Type.TABLE);
    }

    /**
     * Resolves padding, border and margin of a box against the width of its containing block.
     * <p>
     * 상자의 padding, border, margin 을 포함 블록의 너비에 맞춰 푼다.
     *
     * @param b   the box / 상자
     * @param cbW width of the containing block (NaN if unknown) / 포함 블록의 너비(알 수 없으면 NaN)
     * @return bit 1 if the left margin is auto, bit 2 if the right margin is auto / 왼쪽 margin 이 auto 이면 비트 1, 오른쪽이 auto 이면 비트 2
     */
    int edges(LayoutBox b, double cbW) {
        int auto = styleEdges(b, cbW);
        if (b.type == LayoutBox.Type.TABLE) {
            // 표의 border 와 padding 은 border 모델(합치기)에 따라 달라진다.
            tables.adjustEdges(b);
        }
        return auto;
    }

    /**
     * Resolves padding, border and margin of a box from its style (see {@link #edges}).
     * <p>
     * 상자의 padding, border, margin 을 스타일에서 푼다({@link #edges} 참조).
     *
     * @param b   the box / 상자
     * @param cbW width of the containing block (NaN if unknown) / 포함 블록의 너비(알 수 없으면 NaN)
     * @return bit 1 if the left margin is auto, bit 2 if the right margin is auto / 왼쪽 margin 이 auto 이면 비트 1, 오른쪽이 auto 이면 비트 2
     */
    private int styleEdges(LayoutBox b, double cbW) {
        int fr = frameOf(b);
        if (anon(b)) {
            b.paddingTop = b.paddingRight = b.paddingBottom = b.paddingLeft = 0;
            b.borderTop = b.borderRight = b.borderBottom = b.borderLeft = 0;
            b.marginTop = b.marginRight = b.marginBottom = b.marginLeft = 0;
            return 0;
        }
        ComputedStyle s = b.style;
        // 논리적 변(0 블록 시작, 1 인라인 끝, 2 블록 끝, 3 인라인 시작)에 해당하는 물리적 속성을 프레임에 따라 고른다.
        b.paddingTop = Math.max(0, nz(s.resolve(Css.side(Css.PADDING, fr, 0), cbW)));
        b.paddingRight = Math.max(0, nz(s.resolve(Css.side(Css.PADDING, fr, 1), cbW)));
        b.paddingBottom = Math.max(0, nz(s.resolve(Css.side(Css.PADDING, fr, 2), cbW)));
        b.paddingLeft = Math.max(0, nz(s.resolve(Css.side(Css.PADDING, fr, 3), cbW)));
        b.borderTop = Css.border(s, Css.side(Css.BORDER_WIDTH, fr, 0), Css.side(Css.BORDER_STYLE, fr, 0));
        b.borderRight = Css.border(s, Css.side(Css.BORDER_WIDTH, fr, 1), Css.side(Css.BORDER_STYLE, fr, 1));
        b.borderBottom = Css.border(s, Css.side(Css.BORDER_WIDTH, fr, 2), Css.side(Css.BORDER_STYLE, fr, 2));
        b.borderLeft = Css.border(s, Css.side(Css.BORDER_WIDTH, fr, 3), Css.side(Css.BORDER_STYLE, fr, 3));
        b.marginTop = nz(s.resolve(Css.side(Css.MARGIN, fr, 0), cbW));
        b.marginBottom = nz(s.resolve(Css.side(Css.MARGIN, fr, 2), cbW));
        int auto = 0;
        double ml = s.resolve(Css.side(Css.MARGIN, fr, 3), cbW);
        double mr = s.resolve(Css.side(Css.MARGIN, fr, 1), cbW);
        if (Double.isNaN(ml)) {
            auto |= 1;
            ml = 0;
        }
        if (Double.isNaN(mr)) {
            auto |= 2;
            mr = 0;
        }
        b.marginLeft = ml;
        b.marginRight = mr;
        return auto;
    }

    /**
     * Returns the frame in which the size and the edges of a box are resolved: its own frame, but for an orthogonal flow root that is
     * not laid out in its own frame at the moment the frame of its containing block (the outward facing size and margins).
     * <p>
     * 상자의 크기와 가장자리를 푸는 프레임을 반환한다: 자신의 프레임이다. 다만 직교 흐름 루트를 지금 자신의 프레임으로 배치하는 중이 아니면 포함 블록의 프레임이다(바깥을 향하는
     * 크기와 margin).
     *
     * @param b the box / 상자
     * @return the frame / 프레임
     */
    static int frameOf(LayoutBox b) {
        return b.orthRoot && !b.orthActive ? b.parentFrame : b.frame;
    }

    /**
     * Returns the horizontal padding plus border of a box.
     * <p>
     * 상자의 가로 padding 과 border 의 합을 반환한다.
     *
     * @param b the box / 상자
     * @return sum / 합
     */
    static double hExtras(LayoutBox b) {
        return b.paddingLeft + b.paddingRight + b.borderLeft + b.borderRight;
    }

    /**
     * Returns the vertical padding plus border of a box.
     * <p>
     * 상자의 세로 padding 과 border 의 합을 반환한다.
     *
     * @param b the box / 상자
     * @return sum / 합
     */
    static double vExtras(LayoutBox b) {
        return b.paddingTop + b.paddingBottom + b.borderTop + b.borderBottom;
    }

    /**
     * Converts a length of the <code>width</code>/<code>height</code> family to a content-box length according to <code>box-sizing</code>.
     * <p>
     * <code>width</code>/<code>height</code> 계열 길이를 <code>box-sizing</code> 에 따라 content box 길이로 바꾼다.
     *
     * @param b      the box / 상자
     * @param v      the resolved length (NaN stays NaN) / 푼 길이(NaN 은 그대로)
     * @param extras padding plus border in that direction / 그 방향의 padding 과 border 합
     * @return content-box length / content box 길이
     */
    static double toContent(LayoutBox b, double v, double extras) {
        if (Double.isNaN(v)) {
            return v;
        }
        return Math.max(0, "border-box".equals(Css.kw(b.style, Prop.BOX_SIZING)) ? v - extras : v);
    }

    /**
     * Converts a length of the <code>height</code> family to a content-box length: like {@link #toContent} but in quirks mode a table
     * cell always uses its border box for <code>height</code>, <code>min-height</code> and <code>max-height</code> (the table cell height
     * box sizing quirk, Quirks Mode Standard 3.13, https://quirks.spec.whatwg.org/, retrieved 2026-11-17).
     * <p>
     * <code>height</code> 계열 길이를 content box 길이로 바꾼다: {@link #toContent} 와 같으나 호환 모드의 표 셀은 <code>height</code>, <code>min-height</code>,
     * <code>max-height</code> 에서 항상 border box 를 쓴다(표 셀 높이 box-sizing quirk, 호환 모드 표준 3.13).
     *
     * @param b the box / 상자
     * @param v the resolved length (NaN stays NaN) / 푼 길이(NaN 은 그대로)
     * @return content-box length / content box 길이
     */
    double toContentHeight(LayoutBox b, double v) {
        // 호환 모드의 셀은 box-sizing 과 무관하게 높이를 border box 로 해석한다(가로 프레임의 물리적 높이만).
        if (quirksMode && b.type == LayoutBox.Type.TABLE_CELL && frameOf(b) == Css.FRAME_H && !Double.isNaN(v)) {
            return Math.max(0, v - vExtras(b));
        }
        return toContent(b, v, vExtras(b));
    }

    /**
     * Returns the specified content width or NaN for <code>auto</code>.
     * <p>
     * 지정된 content 너비를 반환한다. <code>auto</code> 이면 NaN.
     *
     * @param b   the box / 상자
     * @param cbW containing block width / 포함 블록 너비
     * @return width or NaN / 너비 또는 NaN
     */
    double specWidth(LayoutBox b, double cbW) {
        return anon(b) ? Double.NaN : toContent(b, b.style.resolve(Css.inlineSizeProp(frameOf(b), 0), cbW), hExtras(b));
    }

    /**
     * Returns the minimum content width (0 if none).
     * <p>
     * 최소 content 너비를 반환한다(없으면 0).
     *
     * @param b   the box / 상자
     * @param cbW containing block width / 포함 블록 너비
     * @return minimum width / 최소 너비
     */
    double minWidth(LayoutBox b, double cbW) {
        return anon(b) ? 0 : nz(toContent(b, b.style.resolve(Css.inlineSizeProp(frameOf(b), 1), cbW), hExtras(b)));
    }

    /**
     * Returns the maximum content width (infinity if none).
     * <p>
     * 최대 content 너비를 반환한다(없으면 무한대).
     *
     * @param b   the box / 상자
     * @param cbW containing block width / 포함 블록 너비
     * @return maximum width / 최대 너비
     */
    double maxWidth(LayoutBox b, double cbW) {
        if (anon(b)) {
            return Double.POSITIVE_INFINITY;
        }
        double v = toContent(b, b.style.resolve(Css.inlineSizeProp(frameOf(b), 2), cbW), hExtras(b));
        return Double.isNaN(v) ? Double.POSITIVE_INFINITY : v;
    }

    /**
     * Returns the specified content height or NaN for <code>auto</code> (or a percentage of an indefinite height).
     * <p>
     * 지정된 content 높이를 반환한다. <code>auto</code>(또는 정해지지 않은 높이의 백분율)이면 NaN.
     *
     * @param b   the box / 상자
     * @param cbH containing block height or NaN / 포함 블록 높이 또는 NaN
     * @return height or NaN / 높이 또는 NaN
     */
    double specHeight(LayoutBox b, double cbH) {
        return anon(b) ? Double.NaN : toContentHeight(b, b.style.resolve(Css.blockSizeProp(frameOf(b), 0), cbH));
    }

    /**
     * Returns the minimum content height (0 if none).
     * <p>
     * 최소 content 높이를 반환한다(없으면 0).
     *
     * @param b   the box / 상자
     * @param cbH containing block height or NaN / 포함 블록 높이 또는 NaN
     * @return minimum height / 최소 높이
     */
    double minHeight(LayoutBox b, double cbH) {
        if (anon(b)) {
            return 0;
        }
        double m = nz(toContentHeight(b, b.style.resolve(Css.blockSizeProp(frameOf(b), 1), cbH)));
        // 호환 모드에서 뷰포트를 채우는 html 과 html 을 채우는 body 는 최소 블록 크기가 채울 크기만큼 올라간다.
        return b.quirkFill == 0 ? m : Math.max(m, quirkFillMin(b));
    }

    /**
     * Returns the minimum content block size that the "html element fills the viewport" and "body element fills the html element" quirks
     * of quirks mode (Quirks Mode Standard 3.6 and 3.7, https://quirks.spec.whatwg.org/, retrieved 2026-11-17) give a box. The border box
     * size in the block flow direction becomes the bigger value of the size and the normal size, where the size is that of the initial
     * containing block in the block flow direction minus the margins of the <code>html</code> element, and, for the <code>body</code>
     * element, the size of the content box of the parent in the block flow direction minus the margins of the body. The quirk applies
     * only if the block size of the box is <code>auto</code>, so the box is laid out with the filled size and the boxes that depend on
     * it (positioned descendants, flex items) see it. The frame of the body has to be that of the html element (the standard leaves the
     * mixed case open).
     * <p>
     * 호환 모드의 "html 요소가 뷰포트를 채운다"와 "body 요소가 html 요소를 채운다" quirk(호환 모드 표준 3.6, 3.7)가 상자에 주는 최소 content 블록 크기를 반환한다.
     * 블록 흐름 방향의 border box 크기는 (크기, 보통 크기) 중 큰 값이 되며 크기는 블록 흐름 방향의 초기 포함 블록 크기에서 <code>html</code> 의 margin 을 뺀 값, 그리고
     * <code>body</code> 는 블록 흐름 방향의 부모 content box 크기에서 body 의 margin 을 뺀 값이다. 상자의 블록 크기가 <code>auto</code> 일 때만 적용하며, 상자를 채운 크기로
     * 배치하므로 그 크기에 의존하는 상자(위치 지정된 자손, 플렉스 항목)도 그 크기를 본다. body 의 프레임은 html 요소의 프레임과 같아야 한다(표준은 섞인 경우를 열어 둔다).
     *
     * @param b the html or body box (see {@link LayoutBox#quirkFill}) / html 또는 body 상자({@link LayoutBox#quirkFill} 참조)
     * @return the minimum content block size, 0 if the quirk does not apply / 최소 content 블록 크기. quirk 가 적용되지 않으면 0
     */
    private double quirkFillMin(LayoutBox b) {
        int frame = frameOf(b);
        if (!b.style.isAuto(Css.blockSizeProp(frame, 0))) {
            return 0;
        }
        double size;
        if (b.quirkFill == 1) {
            size = (frame == Css.FRAME_H ? viewportH : viewportW) - b.marginTop - b.marginBottom;
        } else {
            LayoutBox p = b.parent;
            if (p == null || p.style == null || p.frame != b.frame) {
                return 0;
            }
            double pc;
            if (!p.style.isAuto(Css.blockSizeProp(frameOf(p), 0))) {
                // html 의 블록 크기가 정해져 있으면 그 content 크기를 채운다.
                pc = specHeight(p, frame == Css.FRAME_H ? viewportH : viewportW);
            } else if (p.quirkFill == 1) {
                pc = (frame == Css.FRAME_H ? viewportH : viewportW) - p.marginTop - p.marginBottom - vExtras(p);
            } else {
                return 0;
            }
            if (Double.isNaN(pc)) {
                return 0;
            }
            size = pc - b.marginTop - b.marginBottom;
        }
        return Math.max(0, size - vExtras(b));
    }

    /**
     * Returns the maximum content height (infinity if none).
     * <p>
     * 최대 content 높이를 반환한다(없으면 무한대).
     *
     * @param b   the box / 상자
     * @param cbH containing block height or NaN / 포함 블록 높이 또는 NaN
     * @return maximum height / 최대 높이
     */
    double maxHeight(LayoutBox b, double cbH) {
        if (anon(b)) {
            return Double.POSITIVE_INFINITY;
        }
        double v = toContentHeight(b, b.style.resolve(Css.blockSizeProp(frameOf(b), 2), cbH));
        return Double.isNaN(v) ? Double.POSITIVE_INFINITY : v;
    }

    /**
     * Computes the content size of a replaced element from its natural size, its specified size and its limits (CSS 2.1 10.3.2, 10.6.2).
     * An <code>auto</code> side follows the aspect ratio of the natural size; a width limit shrinks the height proportionally.
     * <p>
     * 치환 요소의 content 크기를 자연 크기, 지정 크기, 제한으로 계산한다(CSS 2.1 10.3.2, 10.6.2). <code>auto</code> 인 쪽은 자연 크기의 가로세로 비율을
     * 따르고 너비 제한은 높이를 비례해서 줄인다.
     *
     * @param b   the replaced box / 치환 상자
     * @param cbW containing block width / 포함 블록 너비
     * @param cbH containing block height or NaN / 포함 블록 높이 또는 NaN
     * @return array of content width and content height / content 너비와 높이 배열
     */
    double[] replacedSize(LayoutBox b, double cbW, double cbH) {
        double[] nat = Replaced.natural(b, frameOf(b));
        double nw = nat[0];
        double nh = nat[1];
        double ratio = nw > 0 && nh > 0 ? nw / nh : Double.NaN;
        double sw = specWidth(b, cbW);
        double sh = specHeight(b, cbH);
        double w;
        double h;
        if (!Double.isNaN(sw) && !Double.isNaN(sh)) {
            w = sw;
            h = sh;
        } else if (!Double.isNaN(sw)) {
            w = sw;
            h = !Double.isNaN(ratio) ? w / ratio : nz(nh);
        } else if (!Double.isNaN(sh)) {
            h = sh;
            w = !Double.isNaN(ratio) ? h * ratio : nz(nw);
        } else {
            w = nz(nw);
            h = nz(nh);
        }
        double maxW = maxWidth(b, cbW);
        double minW = minWidth(b, cbW);
        if (w > maxW) {
            if (Double.isNaN(sh) && !Double.isNaN(ratio)) {
                h = maxW / ratio;
            }
            w = maxW;
        }
        if (w < minW) {
            if (Double.isNaN(sh) && !Double.isNaN(ratio)) {
                h = minW / ratio;
            }
            w = minW;
        }
        h = Math.max(minHeight(b, cbH), Math.min(h, maxHeight(b, cbH)));
        return new double[] { w, h };
    }

    /**
     * Lays out a block-level box in normal flow: used width and margins, content, height and margin collapsing information
     * ({@link LayoutBox#outTop}, {@link LayoutBox#outBottom}, {@link LayoutBox#through}). The caller sets the position of the box.
     * <p>
     * 블록 수준 상자를 정상 흐름으로 배치한다: 사용 너비와 margin, 내용, 높이, margin 합치기 정보({@link LayoutBox#outTop}, {@link LayoutBox#outBottom},
     * {@link LayoutBox#through}). 상자의 위치는 호출자가 정한다.
     *
     * @param b   the box / 상자
     * @param cbW width of the containing block's content box / 포함 블록 content box 의 너비
     * @param cbH height of the containing block's content box or NaN / 포함 블록 content box 의 높이 또는 NaN
     */
    void layoutBlockLevel(LayoutBox b, double cbW, double cbH) {
        if (b.orthRoot && !b.orthActive) {
            // 직교 흐름 루트는 자신의 프레임으로 배치한 뒤 포함 블록의 프레임으로 옮긴다(P10-03).
            orthogonal.layoutBlockLevel(b, cbW, cbH);
            return;
        }
        if (b.type == LayoutBox.Type.TABLE) {
            tables.layout(b, cbW, false);
            return;
        }
        // 호출자는 margin 가장자리에 이전 margin 을 더해 nextX 를 정했다: 사용 값이 달라지면(auto 나 자손 정렬) 안쪽 float 문맥의 x 도 같이 옮겨야 한다.
        double edgeX = nextX - b.marginLeft;
        int auto = edges(b, cbW);
        double extras = hExtras(b);
        double cw;
        double[] rsz = null;
        if (b.type == LayoutBox.Type.REPLACED) {
            rsz = replacedSize(b, cbW, cbH);
            cw = rsz[0];
            resolveMargins(b, cbW, auto, cw, extras);
        } else {
            double spec = specWidth(b, cbW);
            boolean autoW = Double.isNaN(spec);
            double w = autoW ? Math.max(0, cbW - b.marginLeft - b.marginRight - extras) : spec;
            cw = Math.max(minWidth(b, cbW), Math.min(w, maxWidth(b, cbW)));
            if (!autoW || cw != w) {
                resolveMargins(b, cbW, auto, cw, extras);
            }
        }
        nextX = edgeX + b.marginLeft;
        finish(b, cw, cbH, rsz);
    }

    /**
     * Resolves auto horizontal margins and the over-constrained case (CSS 2.1 10.3.3, left-to-right).
     * <p>
     * auto 가로 margin 과 제약이 넘치는 경우를 푼다(CSS 2.1 10.3.3, 왼쪽에서 오른쪽).
     *
     * @param b      the box / 상자
     * @param cbW    containing block width / 포함 블록 너비
     * @param auto   bits of {@link #edges} / {@link #edges} 의 비트
     * @param cw     used content width / 사용 content 너비
     * @param extras horizontal padding plus border / 가로 padding 과 border 합
     */
    void resolveMargins(LayoutBox b, double cbW, int auto, double cw, double extras) {
        double ml = (auto & 1) != 0 ? 0 : b.marginLeft;
        double mr = (auto & 2) != 0 ? 0 : b.marginRight;
        double free = cbW - extras - cw;
        if (auto == 3) {
            double rest = free;
            if (rest > 0) {
                b.marginLeft = rest / 2;
                b.marginRight = rest / 2;
            } else {
                b.marginLeft = 0;
                b.marginRight = rest;
            }
        } else if (auto == 1) {
            b.marginLeft = free - mr;
        } else {
            // 오른쪽 margin 이 auto 이거나 제약이 넘치면(오른쪽 margin 무시) 남는 너비를 오른쪽 margin 으로 한다.
            double forced = free - ml;
            b.marginRight = forced;
            // 두 margin 이 모두 auto 가 아니고 오른쪽 margin 이 더 크게 강제되면 center/div align 조상의 자손 정렬을 적용한다(P07-07-b-5).
            double slack = forced - mr;
            if (auto == 0 && slack > EPS) {
                int align = AlignDescendants.forBox(b);
                if (align == AlignDescendants.CENTER) {
                    // 다른 브라우저처럼 margin 상자를 가운데에 둔다: 남는 공간을 양쪽에 반씩 더한다.
                    b.marginLeft = ml + slack / 2;
                    b.marginRight = forced - slack / 2;
                } else if (align == AlignDescendants.RIGHT) {
                    // 남는 공간을 모두 왼쪽 margin 으로 옮긴다.
                    b.marginLeft = ml + slack;
                    b.marginRight = mr;
                }
            }
        }
    }

    /**
     * Lays out an atomic inline-level box (inline-block or inline replaced element) at its shrink-to-fit or specified width. Auto margins
     * count as zero. The caller sets the position.
     * <p>
     * 원자적 인라인 수준 상자(inline-block 또는 인라인 치환 요소)를 shrink-to-fit 너비나 지정 너비로 배치한다. auto margin 은 0 으로 본다. 위치는
     * 호출자가 정한다.
     *
     * @param b     the box / 상자
     * @param availW content width of the containing block / 포함 블록의 content 너비
     */
    void layoutAtomic(LayoutBox b, double availW) {
        if (b.orthRoot && !b.orthActive) {
            orthogonal.layoutAtomic(b, availW);
            return;
        }
        if (b.type == LayoutBox.Type.TABLE) {
            tables.layout(b, availW, true);
            return;
        }
        edges(b, availW);
        double cw;
        double[] rsz = null;
        if (b.type == LayoutBox.Type.REPLACED) {
            rsz = replacedSize(b, availW, Double.NaN);
            cw = rsz[0];
        } else {
            cw = atomicContentWidth(b, availW);
        }
        finish(b, cw, Double.NaN, rsz);
    }

    /**
     * Returns the content width of an atomic inline box: the specified width, else shrink-to-fit (CSS 2.1 10.3.9). The edges of the box
     * must already be resolved.
     * <p>
     * 원자적 인라인 상자의 content 너비를 반환한다: 지정 너비, 없으면 shrink-to-fit(CSS 2.1 10.3.9). 상자의 가장자리는 이미 풀려 있어야 한다.
     *
     * @param b      the box / 상자
     * @param availW content width of the containing block / 포함 블록의 content 너비
     * @return content width / content 너비
     */
    double atomicContentWidth(LayoutBox b, double availW) {
        double spec = specWidth(b, availW);
        double w;
        if (!Double.isNaN(spec)) {
            w = spec;
        } else {
            double avail = Math.max(0, availW - b.marginLeft - b.marginRight - hExtras(b));
            w = Math.min(Math.max(contentMin(b), avail), contentMax(b));
        }
        return Math.max(minWidth(b, availW), Math.min(w, maxWidth(b, availW)));
    }

    /**
     * Returns the width of the margin box of an atomic inline box without laying out its content (used by the dry run that measures
     * intrinsic widths).
     * <p>
     * 내용을 배치하지 않고 원자적 인라인 상자의 margin box 너비를 반환한다(고유 너비를 재는 시험 실행이 쓴다).
     *
     * @param b      the box / 상자
     * @param availW content width of the containing block / 포함 블록의 content 너비
     * @return margin box width / margin box 너비
     */
    double atomicOuterWidth(LayoutBox b, double availW) {
        if (b.orthRoot && !b.orthActive) {
            return orthogonal.outerWidth(b, availW);
        }
        if (b.type == LayoutBox.Type.TABLE) {
            return tables.outerWidth(b, availW);
        }
        edges(b, availW);
        double cw = b.type == LayoutBox.Type.REPLACED ? replacedSize(b, availW, Double.NaN)[0] : atomicContentWidth(b, availW);
        return cw + hExtras(b) + b.marginLeft + b.marginRight;
    }

    /**
     * Completes the layout of a box whose content width is known: lays out the content, resolves the height, records the collapsed
     * margins, creates the outside list marker and lays out the absolutely positioned boxes that this box is the containing block of. A
     * block formatting context root starts a new float context and, if its height is automatic, grows to the lowest float inside. The
     * method may run twice for one box (see {@link #layoutChildren}); the content that an earlier run generated is removed first.
     * <p>
     * content 너비가 정해진 상자의 배치를 마친다: 내용을 배치하고 높이를 정하며 합쳐진 margin 을 기록하고 바깥쪽 목록 표식을 만들며 이 상자가 포함 블록인 절대 배치
     * 상자를 배치한다. 블록 서식 문맥 루트는 새 float 문맥을 시작하고 높이가 auto 이면 안쪽의 가장 낮은 float 까지 늘어난다. 한 상자에 두 번 실행될 수 있으며
     * ({@link #layoutChildren} 참조) 이전 실행이 만든 내용은 먼저 지운다.
     *
     * @param b    the box / 상자
     * @param cw   used content width / 사용 content 너비
     * @param cbH  containing block height or NaN / 포함 블록 높이 또는 NaN
     * @param rsz  size of a replaced box or null / 치환 상자의 크기 또는 null
     */
    void finish(LayoutBox b, double cw, double cbH, double[] rsz) {
        if (b.orthRoot && !b.orthActive) {
            // 호출자가 정한 content 너비는 포함 블록 프레임의 값이다: 직교 흐름 루트의 블록 크기를 그 값으로 강제한다.
            orthogonal.finishRoot(b, cw, cbH);
            return;
        }
        if (b.columnSource != null) {
            // 이전 배치가 자식을 열 상자로 바꿨으면 원래 자식에서 다시 시작한다.
            b.children.clear();
            for (int i = 0; i < b.columnSource.size(); i++) {
                b.columnSource.get(i).parent = b;
                b.children.add(b.columnSource.get(i));
            }
            b.columnSource = null;
            b.columnRules = null;
            b.columnCount = 0;
        }
        stripGenerated(b);
        double contentLeft = b.borderLeft + b.paddingLeft;
        double contentTop = b.borderTop + b.paddingTop;
        double specH = rsz != null ? rsz[1] : (!Double.isNaN(b.forcedH) ? b.forcedH : (b.measureContent ? Double.NaN : specHeight(b, cbH)));
        double minH = rsz != null || b.measureContent ? 0 : minHeight(b, cbH);
        double maxH = rsz != null || b.measureContent ? Double.POSITIVE_INFINITY : maxHeight(b, cbH);
        if (b.type == LayoutBox.Type.TABLE_CELL && !Double.isNaN(specH)) {
            // 셀의 height 는 최솟값이다: 내용이 더 크면 셀이 커진다(CSS 2.1 17.5.3).
            minH = Math.max(minH, specH);
            specH = Double.NaN;
        }
        b.outTop = Margin.of(b.marginTop);
        b.outBottom = Margin.of(b.marginBottom);
        b.baseline = Double.NaN;
        boolean replaced = b.type == LayoutBox.Type.REPLACED;
        boolean flowBox = !replaced && b.type != LayoutBox.Type.INLINE_BLOCK;
        boolean topCan = flowBox && !b.bfcRoot && b.paddingTop == 0 && b.borderTop == 0;
        boolean bottomCan = flowBox && !b.bfcRoot && b.paddingBottom == 0 && b.borderBottom == 0 && Double.isNaN(specH) && minH <= 0;
        FloatContext savedFc = fc;
        double savedX = fcx;
        double savedY = fcy;
        boolean ownBfc = !replaced && (b.bfcRoot || fc == null);
        if (ownBfc) {
            // 블록 서식 문맥의 루트는 새 float 문맥을 시작하고 content box 의 왼쪽 위가 원점이다.
            fc = new FloatContext();
            fcx = 0;
            fcy = 0;
        } else if (!replaced) {
            fcx = nextX + contentLeft;
            fcy = nextY + contentTop;
        }
        double ch;
        if (replaced) {
            ch = rsz[1];
        } else if (b.flexContainer) {
            // 플렉스 컨테이너는 플렉스 레이아웃이 자식(플렉스 항목)을 배치한다(P10-03).
            ch = flex.layout(b, cw, specH, minH, maxH, contentLeft, contentTop);
        } else if (b.gridContainer) {
            // 그리드 컨테이너는 그리드 레이아웃이 자식(그리드 항목)을 배치한다(P10-03).
            ch = grid.layout(b, cw, specH, minH, maxH, contentLeft, contentTop);
        } else if (b.multicol) {
            // 다단 컨테이너는 내용을 열 너비로 흐름 배치한 뒤 열로 나눈다(P10-03).
            ch = multicol.layout(b, cw, specH, minH, maxH, contentLeft, contentTop);
        } else if (b.items != null) {
            ch = inline.layout(b, cw, contentLeft, contentTop);
            // text-overflow 의 줄임표와 line-clamp 의 줄 수 제한으로 줄을 자른다.
            ch = TextOverflow.apply(b, cw, ch, contentLeft, contentTop, tm);
        } else {
            ch = layoutChildren(b, cw, Double.isNaN(specH) ? quirkPercentBase(b, cbH) : specH, contentLeft, contentTop, topCan, bottomCan);
        }
        double flowH = ch;
        if (ownBfc && Double.isNaN(specH)) {
            // 높이가 auto 인 블록 서식 문맥 루트는 안쪽 float 의 아래쪽 margin 가장자리까지 늘어난다.
            double fb = fc.maxBottom();
            if (fb > ch) {
                ch = fb;
            }
        }
        fc = savedFc;
        fcx = savedX;
        fcy = savedY;
        if (!Double.isNaN(specH)) {
            ch = specH;
        } else if (!replaced && b.node != null && Containment.size(b.style)) {
            // 크기 격리가 있는 상자의 자동 높이는 내용이 없는 것처럼 contain-intrinsic-height(없으면 0)이다(CSS Containment 2).
            ch = Containment.intrinsicHeight(b);
        }
        ch = Math.max(minH, Math.min(ch, maxH));
        b.width = cw + hExtras(b);
        b.height = ch + vExtras(b);
        b.through = flowBox && !b.bfcRoot && topCan && (Double.isNaN(specH) || specH == 0) && minH <= 0 && b.paddingBottom == 0
                && b.borderBottom == 0 && flowH == 0 && ch == 0 && !hasLines(b);
        if (b.flexContainer || b.gridContainer) {
            // 플렉스/그리드 컨테이너의 기준선은 그 레이아웃이 정했다. 내용을 자르면 기준선이 없다.
            if (b.clipsContent) {
                b.baseline = Double.NaN;
            }
        } else if (b.type == LayoutBox.Type.INLINE_BLOCK && !b.clipsContent) {
            b.baseline = lastBaseline(b);
        } else if (replaced) {
            // 폼 컨트롤은 글의 기준선을 가진다(그 밖의 치환 요소는 NaN 이라 아래쪽 margin 가장자리가 기준선이다).
            b.baseline = ControlSizes.baseline(b);
        }
        if (b.markerText != null) {
            addMarker(b);
        }
        if (b.absList != null) {
            absolute.layoutContaining(b);
        }
    }

    /**
     * Returns the height that the percentage heights of the children of a box with an automatic height refer to. Outside quirks mode that
     * height is indefinite (NaN). In quirks mode the percentage height calculation quirk (Quirks Mode Standard 3.5,
     * https://quirks.spec.whatwg.org/, retrieved 2026-11-17) skips the containing blocks that are block containers with an automatic height
     * that are not positioned absolutely or fixed, so the children refer to the height that the box itself refers to (the nearest
     * ancestor with a definite height, in the end the initial containing block). A box that is not such a block container (a table cell,
     * an absolutely positioned box, a box with a percentage height that is indefinite) ends the search with an indefinite height.
     * <p>
     * 높이가 auto 인 상자의 자식이 백분율 높이로 참조하는 높이를 반환한다. 호환 모드가 아니면 그 높이는 정해지지 않는다(NaN). 호환 모드에서는 백분율 높이 계산 quirk
     * (호환 모드 표준 3.5)가 높이가 auto 이고 절대/고정 배치가 아닌 블록 컨테이너인 포함 블록을 건너뛰므로 자식은 상자 자신이 참조하는 높이(높이가 정해진 가장 가까운
     * 조상, 끝에서는 초기 포함 블록)를 참조한다. 그런 블록 컨테이너가 아닌 상자(표 셀, 절대 배치 상자, 정해지지 않은 백분율 높이를 가진 상자)는 정해지지 않은 높이로 탐색을
     * 끝낸다.
     *
     * @param b   the box with an automatic (or indefinite) height / 높이가 auto(또는 정해지지 않음)인 상자
     * @param cbH the height of the containing block of the box or NaN / 상자의 포함 블록 높이 또는 NaN
     * @return the height for the children or NaN / 자식이 참조할 높이 또는 NaN
     */
    private double quirkPercentBase(LayoutBox b, double cbH) {
        if (!quirksMode || Double.isNaN(cbH) || b.type != LayoutBox.Type.BLOCK || b.isAbsolutelyPositioned() || b.isFixedPosition()
                || b.flexContainer || b.gridContainer || b.multicol) {
            return Double.NaN;
        }
        // 익명 상자는 항상 높이가 auto 이다. 요소의 상자는 계산된 높이가 auto 일 때만 건너뛴다(정해지지 않은 백분율 높이는 거기서 끝난다).
        if (!anon(b) && !b.style.isAuto(Css.blockSizeProp(frameOf(b), 0))) {
            return Double.NaN;
        }
        return cbH;
    }

    /**
     * Removes what an earlier layout run of the box generated (line boxes, floats and absolutely positioned boxes that were attached to a
     * container of inline content, and the list marker), so that the box can be laid out again.
     * <p>
     * 상자의 이전 배치가 만든 것(인라인 콘텐츠 컨테이너에 붙은 줄 상자, float, 절대 배치 상자와 목록 표식)을 지워 상자를 다시 배치할 수 있게 한다.
     *
     * @param b the box / 상자
     */
    private static void stripGenerated(LayoutBox b) {
        if (b.children.isEmpty()) {
            return;
        }
        if (b.items != null) {
            // 인라인 콘텐츠 컨테이너의 자식은 모두 배치가 만든 것이다.
            b.children.clear();
            return;
        }
        // 블록 자식은 빌더가 만든 것이므로 두고 표식만 지운다.
        for (int i = b.children.size() - 1; i >= 0; i--) {
            if (b.children.get(i).type == LayoutBox.Type.MARKER) {
                b.children.remove(i);
            }
        }
    }

    /**
     * Tells whether a block contains line boxes.
     * <p>
     * 블록이 줄 상자를 가지고 있는지 알려 준다.
     *
     * @param b the block / 블록
     * @return true if it has lines / 줄이 있으면 true
     */
    private static boolean hasLines(LayoutBox b) {
        // 자식 중 줄 상자가 있는지 확인한다.
        for (int i = 0; i < b.children.size(); i++) {
            if (b.children.get(i).type == LayoutBox.Type.LINE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Lays out the block-level children of a box with margin collapsing and returns the height of the content.
     * <p>
     * A float that is placed inside a child depends on where the child is in the block formatting context, and that is known only after
     * the margins of the child have collapsed (with the margins of its own first children, which are known only after it has been laid
     * out). So the position is estimated before a child is laid out (the margin that the first children will contribute is looked up
     * in advance). If the estimate was wrong by more than a hundredth of a pixel and floats exist, the child is laid out a second time
     * at the position that the first run found; the floats that the first run added are removed first. This is done at most once per
     * child and at most {@link #RELAYOUT_BUDGET} times in one layout run (after that the estimates are accepted).
     * <p>
     * A child that clears floats is moved below them, and a child that starts a new block formatting context (a table, a box with
     * <code>overflow</code> other than visible, a block-level replaced element) must not overlap the floats beside it: it is narrowed to
     * the space between them if it fits there, else moved below them (CSS 2.1 9.5 and 9.5.2).
     * <p>
     * 상자의 블록 수준 자식을 margin 합치기와 함께 배치하고 내용의 높이를 반환한다.
     * <p>
     * 자식 안에 놓이는 float 는 자식이 블록 서식 문맥 안 어디에 있는지에 달려 있고, 그것은 자식의 margin 이 합쳐진 뒤(자식 자신의 첫 자식의 margin 과도
     * 합쳐지며 그것은 자식을 배치한 뒤에야 알 수 있음)에야 알 수 있다. 그래서 자식을 배치하기 전에 위치를 추정한다(첫 자식들이 더할 margin 을 미리 찾아 본다).
     * 추정이 0.01 픽셀보다 크게 틀렸고 float 가 있으면 첫 실행이 찾은 위치에서 자식을 한 번 더 배치하며 첫 실행이 더한 float 는 먼저 지운다. 자식마다 최대 한 번,
     * 한 번의 배치에서 최대 {@link #RELAYOUT_BUDGET} 번만 하며 그 뒤에는 추정을 받아들인다.
     * <p>
     * float 를 clear 하는 자식은 그 아래로 내려가고, 새 블록 서식 문맥을 시작하는 자식(표, <code>overflow</code> 가 visible 이 아닌 상자, 블록 수준 치환 요소)은
     * 옆의 float 와 겹치면 안 되므로 float 사이 공간에 들어가면 그만큼 좁아지고 아니면 float 아래로 내려간다(CSS 2.1 9.5, 9.5.2).
     *
     * @param b          the box / 상자
     * @param cw         content width / content 너비
     * @param childCbH   height that percentage heights of the children refer to, or NaN / 자식의 백분율 높이가 참조하는 높이. 없으면 NaN
     * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
     * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
     * @param topCan     true if the top margin may collapse with the first child / 위 margin 이 첫 자식과 합쳐질 수 있으면 true
     * @param bottomCan  true if the bottom margin may collapse with the last child / 아래 margin 이 마지막 자식과 합쳐질 수 있으면 true
     * @return height of the content / 내용의 높이
     */
    private double layoutChildren(LayoutBox b, double cw, double childCbH, double contentLeft, double contentTop, boolean topCan,
            boolean bottomCan) {
        Margin pend = new Margin();
        boolean topOpen = topCan;
        double y = 0;
        final FloatContext ffc = fc;
        final double bx = fcx;
        final double by = fcy;
        final LayoutBox legend = b.legendChild;
        if (legend != null) {
            // fieldset 의 legend 는 위쪽 경계선 위에 세로 가운데로 놓고, 나머지 내용은 legend 아래에서 시작한다.
            nextX = bx + legend.marginLeft;
            nextY = by + legend.marginTop;
            layoutAtomic(legend, cw);
            double full = legend.marginTop + legend.height + legend.marginBottom;
            double slack = Math.max(0, cw - legend.marginLeft - legend.width - legend.marginRight);
            String align = legend.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element && ((org.duckdns.hjow.swingwebpanel.dom.Element) legend.node).getAttribute("align") != null
                    ? org.duckdns.hjow.swingwebpanel.dom.Namespaces.asciiLower(((org.duckdns.hjow.swingwebpanel.dom.Element) legend.node).getAttribute("align")) : "";
            boolean rtl = "rtl".equals(Css.kw(b.style, Prop.DIRECTION));
            // legend 의 가로 위치: align 속성(center, right, left)이 먼저이고, 없으면 direction 이 rtl 일 때 오른쪽, 아니면 왼쪽이다.
            double shift = "center".equals(align) ? slack / 2 : ("right".equals(align) || (rtl && !"left".equals(align)) ? slack : 0);
            legend.lx = contentLeft + legend.marginLeft + shift;
            legend.ly = (b.borderTop - full) / 2 + legend.marginTop;
            y = Math.max(0, full - b.borderTop);
            double overhang = (full - b.borderTop) / 2;
            // 위로 튀어나온 부분은 fieldset 의 위쪽 margin 으로 비워 둔다.
            if (overhang > b.marginTop) {
                b.outTop = Margin.of(overhang);
            }
        }
        // 자식을 차례로 배치하며 인접한 위/아래 margin 을 합친다.
        for (int i = 0; i < b.children.size(); i++) {
            LayoutBox c = b.children.get(i);
            if (c.type == LayoutBox.Type.MARKER || c == legend) {
                continue;
            }
            edges(c, cw);
            Margin chain = chainMargin(c, cw);
            int sizeBefore = ffc.size();
            double hypo;
            if (topOpen) {
                hypo = by;
            } else {
                Margin m = new Margin();
                m.addAll(pend);
                m.addAll(chain);
                hypo = by + y + m.value();
            }
            double placed;
            double leftInset;
            // 추정한 위치에서 배치하고, 위치가 틀렸으면 실제 위치로 한 번 더 배치한다.
            while (true) {
                placed = hypo;
                int sides = clearSides(c);
                if (sides != 0) {
                    double clearBottom = ffc.clearY(sides);
                    if (clearBottom > placed + EPS) {
                        placed = clearBottom;
                    }
                }
                double cwChild = cw;
                leftInset = 0;
                if (c.bfcRoot && ffc.size() > 0) {
                    double[] n = besideFloats(c, ffc, placed, bx, cw);
                    leftInset = n[0] - bx;
                    cwChild = n[1];
                    placed = n[2];
                }
                nextX = bx + leftInset + c.marginLeft;
                nextY = placed;
                layoutBlockLevel(c, cwChild, childCbH);
                double actual = hypo;
                if (!topOpen) {
                    Margin m = new Margin();
                    m.addAll(pend);
                    m.addAll(c.outTop);
                    if (c.through) {
                        m.addAll(c.outBottom);
                    }
                    actual = by + y + m.value();
                }
                if (Math.abs(actual - hypo) > EPS && ffc.size() > 0 && relayouts > 0) {
                    relayouts--;
                    ffc.truncate(sizeBefore);
                    hypo = actual;
                    continue;
                }
                break;
            }
            double cleared = placed - hypo;
            c.lx = contentLeft + leftInset + c.marginLeft;
            if (topOpen && cleared <= EPS) {
                // 부모의 위 margin 과 합쳐지는 동안에는 자식 위치를 밀지 않는다.
                b.outTop.addAll(c.outTop);
                if (c.through) {
                    b.outTop.addAll(c.outBottom);
                    c.ly = contentTop;
                    continue;
                }
                topOpen = false;
                c.ly = contentTop + y;
                y += c.height;
                pend = new Margin();
                pend.addAll(c.outBottom);
                continue;
            }
            if (topOpen) {
                // 첫 자식이 clear 로 내려가면 부모의 위 margin 과 합쳐지지 않는다.
                topOpen = false;
                y = placed - by;
                c.ly = contentTop + y;
                y += c.height;
                pend = new Margin();
                pend.addAll(c.outBottom);
                continue;
            }
            pend.addAll(c.outTop);
            if (c.through) {
                pend.addAll(c.outBottom);
                c.ly = contentTop + y + pend.value();
                continue;
            }
            if (cleared > EPS) {
                // clear 나 float 옆 자리 부족으로 내려간 상자는 앞선 margin 과 합쳐지지 않고 정해진 위치에 놓인다.
                y = placed - by;
            } else {
                y += pend.value();
            }
            c.ly = contentTop + y;
            y += c.height;
            pend = new Margin();
            pend.addAll(c.outBottom);
        }
        if (bottomCan) {
            b.outBottom.addAll(pend);
        } else {
            y += pend.value();
        }
        return y;
    }

    /**
     * Returns the sides that a box clears: 1 for left, 2 for right, 3 for both, 0 for none.
     * <p>
     * 상자가 clear 하는 방향을 반환한다: 왼쪽 1, 오른쪽 2, 양쪽 3, 없으면 0.
     *
     * @param c the box / 상자
     * @return the bit set / 비트 집합
     */
    static int clearSides(LayoutBox c) {
        if (c.style == null || c.type == LayoutBox.Type.ANONYMOUS_BLOCK) {
            return 0;
        }
        return clearSidesOf(c.style);
    }

    /**
     * Returns the sides that a style clears (also for a <code>br</code> element, whose forced line break clears the floats): 1 for
     * left, 2 for right, 3 for both, 0 for none.
     * <p>
     * 스타일이 clear 하는 방향을 반환한다(강제 줄바꿈이 float 를 비우는 <code>br</code> 요소도 포함): 왼쪽 1, 오른쪽 2, 양쪽 3, 없으면 0.
     *
     * @param style the computed style / 계산 스타일
     * @return the bit set / 비트 집합
     */
    static int clearSidesOf(ComputedStyle style) {
        String v = Css.kw(style, Prop.CLEAR);
        if ("both".equals(v)) {
            return 3;
        }
        if ("left".equals(v) || "inline-start".equals(v)) {
            return 1;
        }
        if ("right".equals(v) || "inline-end".equals(v)) {
            return 2;
        }
        return 0;
    }

    /**
     * Collects the top margin of a box together with the top margins that collapse with it through its first children (a first child
     * without padding or border above it, down to the first box that starts a block formatting context or has a border or padding).
     * It is the estimate of the margin that the box will contribute once it is laid out (see {@link #layoutChildren}).
     * <p>
     * 상자의 위 margin 을 그것과 첫 자식들을 통해 합쳐지는 위 margin 과 함께 모은다(위에 padding 이나 border 가 없는 첫 자식을, 블록 서식 문맥을 시작하거나 border 나
     * padding 이 있는 첫 상자까지). 상자를 배치했을 때 더할 margin 의 추정값이다({@link #layoutChildren} 참조).
     *
     * @param c  the box whose margins were resolved / margin 이 풀린 상자
     * @param cw width of the containing block / 포함 블록 너비
     * @return the margins that are expected to collapse / 합쳐질 것으로 예상하는 margin
     */
    private Margin chainMargin(LayoutBox c, double cw) {
        Margin m = Margin.of(c.marginTop);
        LayoutBox x = c;
        // 첫 자식 사슬을 따라 내려가며 위 margin 을 더한다(깊이를 제한한다).
        for (int depth = 0; depth < 64 && collapsesWithFirstChild(x); depth++) {
            LayoutBox g = firstFlowChild(x);
            if (g == null) {
                break;
            }
            edges(g, cw);
            m.add(g.marginTop);
            x = g;
        }
        return m;
    }

    /**
     * Tells whether the top margin of a box collapses with the margin of its first child.
     * <p>
     * 상자의 위 margin 이 첫 자식의 margin 과 합쳐지는지 알려 준다.
     *
     * @param x the box / 상자
     * @return true if it can / 합쳐질 수 있으면 true
     */
    private static boolean collapsesWithFirstChild(LayoutBox x) {
        return (x.type == LayoutBox.Type.BLOCK || x.type == LayoutBox.Type.ANONYMOUS_BLOCK) && x.items == null && !x.bfcRoot
                && x.paddingTop == 0 && x.borderTop == 0;
    }

    /**
     * Returns the first child of a box that takes part in the normal flow (a block, an anonymous block or a block-level replaced box).
     * <p>
     * 상자의 자식 중 정상 흐름에 참여하는 첫 자식(블록, 익명 블록, 블록 수준 치환 상자)을 반환한다.
     *
     * @param x the box / 상자
     * @return the child or null / 자식 또는 null
     */
    private static LayoutBox firstFlowChild(LayoutBox x) {
        // 표식과 줄 상자를 건너뛰고 첫 블록 수준 자식을 찾는다.
        for (int i = 0; i < x.children.size(); i++) {
            LayoutBox k = x.children.get(i);
            if (k.type == LayoutBox.Type.BLOCK || k.type == LayoutBox.Type.ANONYMOUS_BLOCK || k.type == LayoutBox.Type.REPLACED) {
                return k;
            }
        }
        return null;
    }

    /**
     * Finds the place of a box that starts a block formatting context beside the floats of the enclosing context (CSS 2.1 9.5): the
     * space between the floats at the top of the box, or, if the box (with its margins) does not fit there, the first height below
     * floats at which it fits or at which no float is beside it.
     * <p>
     * 둘러싼 문맥의 float 옆에서 블록 서식 문맥을 시작하는 상자의 자리를 찾는다(CSS 2.1 9.5): 상자 위쪽에서 float 사이의 공간, 상자(margin 포함)가 거기 들어가지
     * 않으면 들어가거나 옆에 float 가 없는 첫 높이.
     *
     * @param c   the box whose margins were resolved / margin 이 풀린 상자
     * @param ffc the floats / float
     * @param y   the height of the top border edge / 위쪽 border 가장자리의 높이
     * @param bx  left edge of the containing block / 포함 블록의 왼쪽
     * @param cw  width of the containing block / 포함 블록 너비
     * @return {left edge of the margin box, available width, height of the top border edge} / {margin box 왼쪽, 쓸 수 있는 너비, 위쪽 border 가장자리의 높이}
     */
    private double[] besideFloats(LayoutBox c, FloatContext ffc, double y, double bx, double cw) {
        double spec = specWidth(c, cw);
        double need = (Double.isNaN(spec) ? contentMin(c) : spec) + hExtras(c) + c.marginLeft + c.marginRight;
        double yy = y;
        // 들어갈 자리가 나올 때까지 float 아래쪽으로 내려간다(float 수만큼만 반복해 끝을 보장한다).
        for (int guard = 0; guard <= 1000; guard++) {
            double l = ffc.leftEdge(yy, 1, bx);
            double r = ffc.rightEdge(yy, 1, bx + cw);
            if (l <= bx + EPS && r >= bx + cw - EPS) {
                return new double[] { bx, cw, yy };
            }
            if (need <= r - l + EPS) {
                return new double[] { l, r - l, yy };
            }
            double nb = ffc.nextBottom(yy, 1);
            if (Double.isInfinite(nb)) {
                return new double[] { l, Math.max(0, r - l), yy };
            }
            yy = nb;
        }
        return new double[] { bx, cw, yy };
    }

    /**
     * Returns the local y of the baseline of the last line box inside a box (relative to its border box), or NaN if it has none.
     * <p>
     * 상자 안 마지막 줄 상자의 기준선의 지역 y(상자 border box 기준)를 반환한다. 없으면 NaN.
     *
     * @param b the box / 상자
     * @return y or NaN / y 또는 NaN
     */
    static double lastBaseline(LayoutBox b) {
        // 마지막 자식부터 거슬러 올라가며 기준선이 있는 줄을 찾는다.
        for (int i = b.children.size() - 1; i >= 0; i--) {
            LayoutBox c = b.children.get(i);
            if (c.type == LayoutBox.Type.LINE) {
                return c.ly + c.baseline;
            }
            if (c.type == LayoutBox.Type.BLOCK || c.type == LayoutBox.Type.ANONYMOUS_BLOCK) {
                double v = lastBaseline(c);
                if (!Double.isNaN(v)) {
                    return c.ly + v;
                }
            }
        }
        return Double.NaN;
    }

    /**
     * Returns the local y of the baseline of the first line box inside a box, or NaN if it has none.
     * <p>
     * 상자 안 첫 줄 상자의 기준선의 지역 y 를 반환한다. 없으면 NaN.
     *
     * @param b the box / 상자
     * @return y or NaN / y 또는 NaN
     */
    static double firstBaseline(LayoutBox b) {
        // 첫 자식부터 기준선이 있는 줄을 찾는다.
        for (int i = 0; i < b.children.size(); i++) {
            LayoutBox c = b.children.get(i);
            if (c.type == LayoutBox.Type.LINE) {
                return c.ly + c.baseline;
            }
            if (c.type == LayoutBox.Type.BLOCK || c.type == LayoutBox.Type.ANONYMOUS_BLOCK) {
                double v = firstBaseline(c);
                if (!Double.isNaN(v)) {
                    return c.ly + v;
                }
            }
        }
        return Double.NaN;
    }

    /**
     * Creates the outside marker box of a list item: its right edge touches the left border edge of the item and its baseline is the
     * baseline of the first line.
     * <p>
     * 목록 항목의 바깥쪽 표식 상자를 만든다: 오른쪽 가장자리가 항목의 왼쪽 border 가장자리에 닿고 기준선은 첫 줄의 기준선이다.
     *
     * @param b the list item box / 목록 항목 상자
     */
    private void addMarker(LayoutBox b) {
        // ::marker 가 있으면 표식은 그 스타일(색, 글꼴)을 쓴다(P10-04).
        ComputedStyle markerStyle = b.markerStyle != null ? b.markerStyle : b.style;
        LayoutBox m = new LayoutBox(LayoutBox.Type.MARKER, b.node, markerStyle);
        m.text = b.markerText;
        m.font = FontResolver.resolve(markerStyle, m.text);
        double[] fm = FontResolver.metrics(m.font);
        m.ascent = fm[0];
        m.descent = fm[1];
        m.width = tm.advance(m.font, m.text);
        m.height = m.ascent + m.descent;
        if (b.markerImage != null) {
            // 이미지 표식은 고유 크기(없으면 1em 정사각형)이며 아래쪽 가장자리가 기준선에 놓이고 오른쪽에 공백 하나만큼 떨어진다(CSS Lists 3 3.3 절).
            double em = markerStyle.fontSizePx();
            double iw = Double.isNaN(b.markerImageSize[0]) ? em : b.markerImageSize[0];
            double ih = Double.isNaN(b.markerImageSize[1]) ? em : b.markerImageSize[1];
            double gap = tm.advance(m.font, " ");
            m.text = "";
            m.markerImage = b.markerImage;
            m.ascent = ih;
            m.descent = 0;
            m.width = iw + gap;
            m.height = ih;
            m.markerImageSize = new double[] {iw, ih};
        }
        m.inlineLevel = true;
        m.decoration = 0;
        double base = firstBaseline(b);
        double top = Double.isNaN(base) ? b.borderTop + b.paddingTop : base - m.ascent;
        m.lx = -m.width;
        m.ly = top;
        m.baseline = m.ascent;
        m.parent = b;
        b.children.add(0, m);
    }

    /**
     * Returns the min-content width of the content box: the width of the narrowest layout that does not overflow (the longest unbreakable
     * unit, the widest atomic box, the widest block child).
     * <p>
     * content box 의 min-content 너비를 반환한다: 넘치지 않는 가장 좁은 배치의 너비(가장 긴 쪼갤 수 없는 단위, 가장 넓은 원자적 상자, 가장 넓은 블록 자식).
     *
     * @param b the box / 상자
     * @return width / 너비
     */
    double contentMin(LayoutBox b) {
        if (b.orthRoot && !b.orthActive) {
            return orthogonal.contentWidth(b);
        }
        if (!Double.isNaN(b.minContent)) {
            return b.minContent;
        }
        double v = intrinsic(b, false);
        b.minContent = v;
        return v;
    }

    /**
     * Returns the max-content width of the content box: the width when no line wraps.
     * <p>
     * content box 의 max-content 너비를 반환한다: 줄바꿈이 전혀 없을 때의 너비.
     *
     * @param b the box / 상자
     * @return width / 너비
     */
    double contentMax(LayoutBox b) {
        if (b.orthRoot && !b.orthActive) {
            return orthogonal.contentWidth(b);
        }
        if (!Double.isNaN(b.maxContent)) {
            return b.maxContent;
        }
        double v = intrinsic(b, true);
        b.maxContent = v;
        return v;
    }

    /**
     * Computes an intrinsic width of the content box.
     * <p>
     * content box 의 고유 너비를 계산한다.
     *
     * @param b   the box / 상자
     * @param max true for max-content, false for min-content / max-content 이면 true, min-content 이면 false
     * @return width / 너비
     */
    private double intrinsic(LayoutBox b, boolean max) {
        // 인라인 크기 격리가 있는 상자는 내용이 없는 것처럼 contain-intrinsic-width(없으면 0)의 고유 너비를 가진다(CSS Containment 2).
        if (b.node != null && b.type != LayoutBox.Type.REPLACED && Containment.inlineSize(b.style)) {
            return Containment.intrinsicWidth(b);
        }
        if (b.type == LayoutBox.Type.TABLE) {
            return tables.intrinsic(b, max);
        }
        if (b.type == LayoutBox.Type.REPLACED) {
            edges(b, Double.NaN);
            return replacedSize(b, Double.NaN, Double.NaN)[0];
        }
        if (b.flexContainer) {
            return flex.intrinsic(b, max);
        }
        if (b.gridContainer) {
            return grid.intrinsic(b, max);
        }
        if (b.multicol) {
            return multicol.intrinsic(b, max);
        }
        return flowIntrinsic(b, max);
    }

    /**
     * Lays out the content of a box as one continuous flow at a content width, with its origin at the top-left corner of the content box
     * (used by the multi-column layout, which cuts the flow into columns afterwards). The float context of the box must be set up.
     * <p>
     * 상자의 내용을 content 너비로 하나의 연속된 흐름으로 배치한다. 원점은 content box 의 왼쪽 위이다(다단 레이아웃이 나중에 흐름을 열로 자른다). 상자의 float 문맥이
     * 준비되어 있어야 한다.
     *
     * @param b the box / 상자
     * @param w the content width of the flow / 흐름의 content 너비
     * @return height of the flow / 흐름의 높이
     */
    double layoutFlowContent(LayoutBox b, double w) {
        if (b.items != null) {
            return inline.layout(b, w, 0, 0);
        }
        return layoutChildren(b, w, Double.NaN, 0, 0, false, false);
    }

    /**
     * Computes an intrinsic width of the content box of an ordinary block container (inline content or block children).
     * <p>
     * 보통 블록 컨테이너(인라인 콘텐츠 또는 블록 자식)의 content box 고유 너비를 계산한다.
     *
     * @param b   the box / 상자
     * @param max true for max-content / max-content 이면 true
     * @return width / 너비
     */
    double flowIntrinsic(LayoutBox b, boolean max) {
        if (b.items != null) {
            return inline.widest(b, max ? Double.POSITIVE_INFINITY : 0);
        }
        double v = 0;
        // 블록 자식의 바깥 너비 중 가장 큰 값을 고른다.
        for (int i = 0; i < b.children.size(); i++) {
            LayoutBox c = b.children.get(i);
            if (c.type == LayoutBox.Type.MARKER) {
                continue;
            }
            edges(c, Double.NaN);
            double spec = specWidth(c, Double.NaN);
            double content = !Double.isNaN(spec) ? spec : (max ? contentMax(c) : contentMin(c));
            v = Math.max(v, content + hExtras(c) + c.marginLeft + c.marginRight);
        }
        return v;
    }
}
