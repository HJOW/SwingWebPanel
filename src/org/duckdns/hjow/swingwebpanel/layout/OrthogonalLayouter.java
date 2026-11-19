package org.duckdns.hjow.swingwebpanel.layout;

import java.util.List;

/**
 * Layout of orthogonal flow roots and the conversion between writing mode frames (plan item P10-03, CSS Writing Modes Level 3, W3C
 * REC 2019-12-10, section 7.3). All layout code works in one set of logical dimensions (the dimensions of horizontal text: a box has
 * an inline size along the lines and a block size across them). A box whose writing mode is perpendicular to that of its containing
 * block (vertical text inside horizontal text, a horizontal box inside vertical text, a vertical root element) is an <i>orthogonal flow
 * root</i>. It is laid out in two phases, like the specification says: first its own size and its content are computed in the logical
 * dimensions of <i>its own</i> writing mode (the inline size, which is the physical height of a vertical box, is the fit-content size
 * <code>min(max-content, max(min-content, available))</code> where the available space is the height of the containing block or, if
 * that is not definite, the height of the viewport; the block size, the physical width, follows from the content), then the result is
 * converted into the logical dimensions of the containing block, which swaps width and height of the box, and the box takes part in
 * the layout of the containing block with its outward facing margins (resolved in the frame of the containing block, as the
 * specification says). The content of a vertical root is converted into physical orientation for good
 * ({@link #convertChildren}): the first line of <code>vertical-rl</code> is at the right, that of <code>vertical-lr</code> at the left,
 * the inline direction runs from top to bottom (bottom to top for <code>sideways-lr</code>); the glyphs of text boxes are rotated by a
 * quarter turn ({@link LayoutBox#getTextRotation()}). The conversion makes horizontal boxes inside vertical text stay horizontal
 * without further work: such a box (an <i>island</i>, also a table, which is always horizontal) is laid out in physical dimensions and
 * only its own box is swapped, its content is not converted again.
 * <p>
 * Supported: <code>writing-mode</code> <code>horizontal-tb</code>, <code>vertical-rl</code>, <code>vertical-lr</code>,
 * <code>sideways-rl</code> (as <code>vertical-rl</code>), <code>sideways-lr</code>; the physical <code>width</code>/<code>height</code>,
 * margins, paddings, borders and offsets of boxes in vertical text; blocks, inline content with line wrapping, lists, replaced
 * elements, inline blocks, flex containers and multi-column containers (columns lie along the inline axis) in vertical text;
 * <code>direction</code> (inline-start is the top, or the bottom with <code>direction:rtl</code>). The orientation of the characters
 * (P10-03-e, {@link InlineLayouter}, {@link VerticalText}): <code>text-orientation</code> <code>mixed</code> (Hangul, Han, kana and
 * the other upright scripts of UAX #50 stand upright in a cell of one em, Latin letters and digits lie sideways), <code>upright</code>,
 * <code>sideways</code> and <code>text-combine-upright</code> (<code>all</code>, <code>digits n</code>). Not supported (documented limits):
 * vertical glyph forms (<code>vert</code>) of upright punctuation and brackets (they are set with their horizontal glyphs, see
 * {@link VerticalText}), <code>float:left</code>/<code>right</code> float to the line-left/line-right (top/bottom) side,
 * tables are always horizontal (their cells inherit the vertical mode), <code>vertical-lr</code> inside <code>vertical-rl</code> uses
 * the block direction of its parent, the baseline of an orthogonal inline block is its bottom margin edge, a vertical root element
 * is anchored at the right edge of the viewport only if it is narrower than the viewport, text decoration lines of rotated text are on
 * the descender side.
 * <p>
 * 직교 흐름 루트의 레이아웃과 쓰기 모드 프레임 사이의 변환(계획 P10-03, CSS Writing Modes Level 3, W3C REC 2019-12-10, 7.3 절). 모든 레이아웃 코드는 하나의 논리적 크기
 * 체계(가로쓰기의 크기: 상자는 줄을 따라가는 인라인 크기와 줄을 가로지르는 블록 크기를 가진다)로 일한다. 쓰기 모드가 포함 블록과 수직인 상자(가로쓰기 안의 세로쓰기, 세로쓰기
 * 안의 가로 상자, 세로 루트 요소)는 <i>직교 흐름 루트</i>이다. 규격대로 두 단계로 배치한다: 먼저 상자의 크기와 내용을 <i>자신의</i> 쓰기 모드의 논리적 크기로 계산하고(세로
 * 상자에서는 물리적 높이인 인라인 크기는 fit-content 크기 <code>min(max-content, max(min-content, available))</code> 이며 쓸 수 있는 공간은 포함 블록의 높이, 정해지지 않았으면
 * 뷰포트의 높이이고, 물리적 너비인 블록 크기는 내용에서 나온다), 그 결과를 포함 블록의 논리적 크기로 바꾼다(상자의 너비와 높이가 바뀐다). 상자는 바깥을 향하는 margin(규격대로
 * 포함 블록의 프레임으로 푼다)으로 포함 블록의 레이아웃에 참여한다. 세로 루트의 내용은 물리적 방향으로 완전히 바뀐다({@link #convertChildren}): <code>vertical-rl</code> 의 첫 줄은
 * 오른쪽, <code>vertical-lr</code> 의 첫 줄은 왼쪽, 인라인 방향은 위에서 아래(<code>sideways-lr</code> 은 아래에서 위)이며 텍스트 상자의 글리프는 사분원만큼 회전한다
 * ({@link LayoutBox#getTextRotation()}). 이 변환 덕분에 세로쓰기 안의 가로 상자는 더 손대지 않아도 가로로 남는다: 그런 상자(<i>섬</i>이며 언제나 가로인 표도 같다)는 물리적
 * 크기로 배치하고 자신의 상자만 바꾸며 내용은 다시 변환하지 않는다.
 * <p>
 * 지원: <code>writing-mode</code> 의 <code>horizontal-tb</code>, <code>vertical-rl</code>, <code>vertical-lr</code>, <code>sideways-rl</code>(<code>vertical-rl</code> 처럼),
 * <code>sideways-lr</code>. 세로쓰기 안 상자의 물리적 <code>width</code>/<code>height</code>, margin, padding, border, 오프셋. 세로쓰기 안의 블록, 줄바꿈하는 인라인 콘텐츠, 목록,
 * 치환 요소, inline-block, 플렉스 컨테이너, 다단 컨테이너(열이 인라인 축을 따라 놓임). <code>direction</code>(인라인 시작은 위쪽이며 <code>direction:rtl</code> 이면 아래쪽).
 * 글자의 방향(P10-03-e, {@link InlineLayouter}, {@link VerticalText}): <code>text-orientation</code> 의 <code>mixed</code>(한글, 한자, 가나와 UAX #50 의 그 밖의 똑바로 서는
 * 문자 체계는 1em 한 칸에 똑바로 서고 라틴 글자와 숫자는 눕는다), <code>upright</code>, <code>sideways</code>, <code>text-combine-upright</code>(<code>all</code>,
 * <code>digits n</code>). 지원하지 않음(문서화된 한계): 똑바로 선 문장 부호와 괄호의 세로 자형(<code>vert</code>, 가로 자형 그대로 놓는다. {@link VerticalText} 참조),
 * <code>float:left</code>/<code>right</code> 는 줄의 왼쪽/오른쪽(위/아래)으로 뜬다, 표는 언제나 가로(셀은 세로 모드를 물려받음), <code>vertical-rl</code> 안의
 * <code>vertical-lr</code> 은 부모의 블록 방향을 쓴다, 직교 inline-block 의 기준선은 아래쪽 margin 가장자리이다, 세로 루트 요소는 뷰포트보다 좁을 때만 뷰포트 오른쪽에
 * 붙인다, 회전한 텍스트의 장식선은 descender 쪽에 있다.
 */
final class OrthogonalLayouter {

    /** The block layouter that lays out the content. 내용을 배치하는 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the orthogonal layouter.
     * <p>
     * 직교 레이아웃을 만든다.
     *
     * @param bl the block layouter of this layout run / 이 레이아웃 실행의 블록 레이아웃
     */
    OrthogonalLayouter(BlockLayouter bl) {
        this.bl = bl;
    }

    /**
     * Returns the size along the inline axis of a root that is available when nothing else is known: the size of the viewport in that
     * direction (the fallback of CSS Writing Modes 7.3.1).
     * <p>
     * 아무것도 모를 때 루트의 인라인 축 방향으로 쓸 수 있는 크기를 반환한다: 그 방향의 뷰포트 크기(CSS Writing Modes 7.3.1 의 대체 크기).
     *
     * @param r the orthogonal flow root / 직교 흐름 루트
     * @return the fallback size / 대체 크기
     */
    private double fallback(LayoutBox r) {
        return r.frame == Css.FRAME_H ? bl.viewportW : bl.viewportH;
    }

    /**
     * Lays out an orthogonal flow root in the normal flow of its containing block (the caller sets the position): the box gets its size,
     * its outward facing margins (the auto margins are resolved in the frame of the containing block) and the information for margin
     * collapsing.
     * <p>
     * 직교 흐름 루트를 포함 블록의 정상 흐름에서 배치한다(위치는 호출자가 정한다): 상자의 크기, 바깥을 향하는 margin(auto margin 은 포함 블록의 프레임으로 푼다), margin 합치기
     * 정보를 정한다.
     *
     * @param r   the root / 루트
     * @param cbW the content width of the containing block (in its frame) / 포함 블록의 content 너비(그 프레임 기준)
     * @param cbH the content height of the containing block or NaN / 포함 블록의 content 높이 또는 NaN
     */
    void layoutBlockLevel(LayoutBox r, double cbW, double cbH) {
        // 포함 블록의 블록 크기가 루트의 인라인 축 방향 크기이다. 정해지지 않았으면 뷰포트 크기를 쓴다.
        double avail = Double.isNaN(cbH) ? fallback(r) : cbH;
        layoutRoot(r, avail, Double.NaN, cbW);
        int auto = bl.edges(r, cbW);
        double extras = BlockLayouter.hExtras(r);
        bl.resolveMargins(r, cbW, auto, Math.max(0, r.width - extras), extras);
        r.outTop = Margin.of(r.marginTop);
        r.outBottom = Margin.of(r.marginBottom);
        r.through = false;
    }

    /**
     * Lays out an orthogonal flow root as an atomic inline-level box (an inline block of vertical text in horizontal text); the auto
     * margins count as zero and the box has no baseline.
     * <p>
     * 직교 흐름 루트를 원자적 인라인 수준 상자(가로쓰기 안의 세로쓰기 inline-block)로 배치한다. auto margin 은 0 이고 상자는 기준선이 없다.
     *
     * @param r      the root / 루트
     * @param availW the content width of the containing block / 포함 블록의 content 너비
     */
    void layoutAtomic(LayoutBox r, double availW) {
        layoutRoot(r, fallback(r), Double.NaN, availW);
        bl.edges(r, availW);
    }

    /**
     * Returns the width of the margin box of an orthogonal flow root as an atomic inline box (used by the dry run that measures
     * intrinsic widths).
     * <p>
     * 직교 흐름 루트를 원자적 인라인 상자로 볼 때의 margin box 너비를 반환한다(고유 너비를 재는 시험 실행이 쓴다).
     *
     * @param r      the root / 루트
     * @param availW the content width of the containing block / 포함 블록의 content 너비
     * @return the width / 너비
     */
    double outerWidth(LayoutBox r, double availW) {
        layoutAtomic(r, availW);
        return r.width + r.marginLeft + r.marginRight;
    }

    /**
     * Finishes an orthogonal flow root whose width in the frame of its containing block is given by the caller (a flex item, a table cell,
     * an absolutely positioned box): the content width is the block size of the root, its inline size follows from the fit-content rule.
     * The edges that the caller resolved are kept.
     * <p>
     * 포함 블록 프레임에서의 너비를 호출자가 정한 직교 흐름 루트(플렉스 항목, 표 셀, 절대 배치 상자)를 마친다: content 너비가 루트의 블록 크기이고 인라인 크기는 fit-content
     * 규칙에서 나온다. 호출자가 푼 가장자리는 유지한다.
     *
     * @param r  the root / 루트
     * @param cw the content width in the frame of the containing block / 포함 블록 프레임의 content 너비
     * @param cbH the content height of the containing block or NaN / 포함 블록의 content 높이 또는 NaN
     */
    void finishRoot(LayoutBox r, double cw, double cbH) {
        double[] saved = { r.marginTop, r.marginRight, r.marginBottom, r.marginLeft, r.borderTop, r.borderRight, r.borderBottom, r.borderLeft,
            r.paddingTop, r.paddingRight, r.paddingBottom, r.paddingLeft };
        layoutRoot(r, fallback(r), cw, Double.isNaN(cbH) ? cw : cbH);
        r.marginTop = saved[0];
        r.marginRight = saved[1];
        r.marginBottom = saved[2];
        r.marginLeft = saved[3];
        r.borderTop = saved[4];
        r.borderRight = saved[5];
        r.borderBottom = saved[6];
        r.borderLeft = saved[7];
        r.paddingTop = saved[8];
        r.paddingRight = saved[9];
        r.paddingBottom = saved[10];
        r.paddingLeft = saved[11];
        r.outTop = Margin.of(r.marginTop);
        r.outBottom = Margin.of(r.marginBottom);
        r.through = false;
    }

    /**
     * Returns the content width that an orthogonal flow root contributes in the frame of its containing block (both its min-content and
     * its max-content width): the block size of the root after it was laid out with the fallback size.
     * <p>
     * 직교 흐름 루트가 포함 블록 프레임에서 기여하는 content 너비(min-content 와 max-content 모두)를 반환한다: 대체 크기로 배치한 뒤 루트의 블록 크기.
     *
     * @param r the root / 루트
     * @return the content width / content 너비
     */
    double contentWidth(LayoutBox r) {
        if (!Double.isNaN(r.orthWidth)) {
            return r.orthWidth;
        }
        layoutRoot(r, fallback(r), Double.NaN, Double.NaN);
        bl.edges(r, Double.NaN);
        r.orthWidth = Math.max(0, r.width - BlockLayouter.hExtras(r));
        return r.orthWidth;
    }

    /**
     * Lays out an orthogonal flow root in its own frame and converts the result into the frame of its containing block. Afterwards the
     * width and the height of the box are those of the containing block's frame; the edges are those of the root's own frame and must be
     * resolved again by the caller (<code>edges</code>).
     * <p>
     * 직교 흐름 루트를 자신의 프레임으로 배치하고 그 결과를 포함 블록의 프레임으로 바꾼다. 이후 상자의 너비와 높이는 포함 블록 프레임의 값이다. 가장자리는 루트 자신의
     * 프레임의 값이므로 호출자가 다시 풀어야 한다(<code>edges</code>).
     *
     * @param r            the root / 루트
     * @param availInline  the available size along the inline axis of the root / 루트의 인라인 축 방향 사용 가능 크기
     * @param forcedBlock  the content size along the block axis that the caller forces, or NaN / 호출자가 강제하는 블록 축 방향 content 크기 또는 NaN
     * @param percentBase  the base of percentage block sizes (the content width of the containing block) / 백분율 블록 크기의 기준(포함 블록의 content 너비)
     */
    private void layoutRoot(LayoutBox r, double availInline, double forcedBlock, double percentBase) {
        boolean wasActive = r.orthActive;
        r.orthActive = true;
        try {
            bl.edges(r, availInline);
            double extrasI = BlockLayouter.hExtras(r);
            double mi = r.marginLeft + r.marginRight;
            double spec = bl.specWidth(r, availInline);
            double ci;
            if (!Double.isNaN(spec)) {
                ci = spec;
            } else {
                // fit-content: min(max-content, max(min-content, 쓸 수 있는 공간))
                double avail = Math.max(0, availInline - mi - extrasI);
                ci = Math.min(Math.max(bl.contentMin(r), avail), bl.contentMax(r));
            }
            ci = Math.max(bl.minWidth(r, availInline), Math.min(ci, bl.maxWidth(r, availInline)));
            r.forcedH = forcedBlock;
            if (r.type == LayoutBox.Type.TABLE) {
                bl.tables.layout(r, ci + extrasI + mi, true);
            } else {
                bl.finish(r, ci, percentBase, null);
            }
            r.forcedH = Double.NaN;
            double w = r.width;
            double h = r.height;
            // 세로 루트의 내용은 물리적 방향으로 바꾼다. 가로 상자(섬)는 내용을 그대로 두고 자신의 상자만 바꾼다.
            if (r.frame != Css.FRAME_H && r.parentFrame == Css.FRAME_H) {
                convertChildren(r, r.frame, w, h);
            }
            r.width = h;
            r.height = w;
            r.baseline = Double.NaN;
        } finally {
            r.orthActive = wasActive;
        }
        r.orthWidth = Double.NaN;
    }

    /**
     * Converts the children (and their descendants) of a box from the logical dimensions of a vertical frame into physical ones: the
     * geometry relative to the parent border box, the edges and the text rotation. Orthogonal roots that are horizontal (islands) are
     * converted as boxes but their content is physical already.
     * <p>
     * 상자의 자식(과 그 자손)을 세로 프레임의 논리적 크기에서 물리적 크기로 바꾼다: 부모 border box 기준 기하, 가장자리, 텍스트 회전. 가로인 직교 루트(섬)는 상자만 바꾸고
     * 내용은 이미 물리적이다.
     *
     * @param p    the parent (its geometry is still logical) / 부모(기하는 아직 논리적)
     * @param mode the frame of the vertical root / 세로 루트의 프레임
     * @param plw  the logical width (inline size) of the parent border box / 부모 border box 의 논리적 너비(인라인 크기)
     * @param plh  the logical height (block size) of the parent border box / 부모 border box 의 논리적 높이(블록 크기)
     */
    private void convertChildren(LayoutBox p, int mode, double plw, double plh) {
        boolean line = p.type == LayoutBox.Type.LINE;
        List<LayoutBox> kids = p.children;
        // 자식마다 위치와 크기를 바꾸고 가장자리를 돌리고 자신의 자식도 바꾼다.
        for (int i = 0; i < kids.size(); i++) {
            LayoutBox c = kids.get(i);
            double lx = c.lx;
            double ly = c.ly;
            double w = c.width;
            double h = c.height;
            double nx;
            double ny;
            if (mode == Css.FRAME_VRL) {
                nx = plh - (ly + h);
                ny = lx;
            } else if (mode == Css.FRAME_VLR) {
                nx = line ? plh - (ly + h) : ly;
                ny = lx;
            } else {
                nx = ly;
                ny = plw - (lx + w);
            }
            if (c.isAbsolutelyPositioned()) {
                convertStatic(c, mode, line, plw, plh);
            }
            boolean island = c.orthRoot && c.frame == Css.FRAME_H;
            if (!island) {
                convertChildren(c, mode, w, h);
            }
            c.lx = nx;
            c.ly = ny;
            c.width = h;
            c.height = w;
            c.baseline = Double.NaN;
            rotateEdges(c, mode);
            if (c.type == LayoutBox.Type.TEXT || c.type == LayoutBox.Type.MARKER) {
                c.textRot = (c.textRot + (mode == Css.FRAME_SLR ? 3 : 1)) & 3;
            }
            if (c.columnRules != null) {
                c.columnRules = rotateRules(c.columnRules, mode, w, h);
            }
        }
    }

    /**
     * Converts the static position of an absolutely positioned box (a point at the top-left of the box that would be) to the physical
     * orientation; the static position rectangle of a flex container is dropped.
     * <p>
     * 절대 배치 상자의 정적 위치(있었을 상자의 왼쪽 위 점)를 물리적 방향으로 바꾼다. 플렉스 컨테이너의 정적 위치 사각형은 버린다.
     *
     * @param c    the box / 상자
     * @param mode the frame of the vertical root / 세로 루트의 프레임
     * @param line true if the parent is a line box / 부모가 줄 상자이면 true
     * @param plw  logical width of the parent / 부모의 논리적 너비
     * @param plh  logical height of the parent / 부모의 논리적 높이
     */
    private static void convertStatic(LayoutBox c, int mode, boolean line, double plw, double plh) {
        double sx = c.staticX;
        double sy = c.staticY;
        if (mode == Css.FRAME_VRL) {
            c.staticX = plh - sy;
            c.staticY = sx;
        } else if (mode == Css.FRAME_VLR) {
            c.staticX = line ? plh - sy : sy;
            c.staticY = sx;
        } else {
            c.staticX = sy;
            c.staticY = plw - sx;
        }
        c.staticW = Double.NaN;
        c.staticH = Double.NaN;
        c.staticAlignX = 0;
        c.staticAlignY = 0;
    }

    /**
     * Rotates the margins, borders and paddings of a box from the logical sides of a frame to the physical sides.
     * <p>
     * 상자의 margin, border, padding 을 프레임의 논리적 변에서 물리적 변으로 돌린다.
     *
     * @param c    the box / 상자
     * @param mode the frame / 프레임
     */
    private static void rotateEdges(LayoutBox c, int mode) {
        double[] m = rotate(new double[] { c.marginTop, c.marginRight, c.marginBottom, c.marginLeft }, mode);
        c.marginTop = m[0];
        c.marginRight = m[1];
        c.marginBottom = m[2];
        c.marginLeft = m[3];
        double[] b = rotate(new double[] { c.borderTop, c.borderRight, c.borderBottom, c.borderLeft }, mode);
        c.borderTop = b[0];
        c.borderRight = b[1];
        c.borderBottom = b[2];
        c.borderLeft = b[3];
        double[] d = rotate(new double[] { c.paddingTop, c.paddingRight, c.paddingBottom, c.paddingLeft }, mode);
        c.paddingTop = d[0];
        c.paddingRight = d[1];
        c.paddingBottom = d[2];
        c.paddingLeft = d[3];
    }

    /**
     * Maps four values given by logical side (block-start, inline-end, block-end, inline-start) to the physical sides (top, right,
     * bottom, left).
     * <p>
     * 논리적 변(블록 시작, 인라인 끝, 블록 끝, 인라인 시작)별로 주어진 네 값을 물리적 변(위, 오른쪽, 아래, 왼쪽)으로 옮긴다.
     *
     * @param v    the values / 값
     * @param mode the frame / 프레임
     * @return the values by physical side / 물리적 변별 값
     */
    private static double[] rotate(double[] v, int mode) {
        double[] r = new double[4];
        // 논리적 변마다 대응하는 물리적 변에 값을 옮긴다.
        for (int l = 0; l < 4; l++) {
            r[Css.physSide(mode, l)] = v[l];
        }
        return r;
    }

    /**
     * Converts the rectangles of column rules (relative to the border box) from the logical to the physical dimensions of a frame.
     * <p>
     * 열 구분선 사각형(border box 기준)을 프레임의 논리적 크기에서 물리적 크기로 바꾼다.
     *
     * @param rules the rectangles {x, y, width, height} / 사각형 {x, y, 너비, 높이}
     * @param mode  the frame / 프레임
     * @param lw    logical width of the box / 상자의 논리적 너비
     * @param lh    logical height of the box / 상자의 논리적 높이
     * @return the converted rectangles / 바뀐 사각형
     */
    private static double[][] rotateRules(double[][] rules, int mode, double lw, double lh) {
        double[][] out = new double[rules.length][];
        // 사각형마다 위치와 크기를 바꾼다.
        for (int i = 0; i < rules.length; i++) {
            double x = rules[i][0];
            double y = rules[i][1];
            double w = rules[i][2];
            double h = rules[i][3];
            double nx;
            double ny;
            if (mode == Css.FRAME_SLR) {
                nx = y;
                ny = lw - (x + w);
            } else if (mode == Css.FRAME_VLR) {
                nx = y;
                ny = x;
            } else {
                nx = lh - (y + h);
                ny = x;
            }
            out[i] = new double[] { nx, ny, h, w };
        }
        return out;
    }
}
