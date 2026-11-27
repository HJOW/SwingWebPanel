package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;

/**
 * Cuts a finished layout into pages for printing (plan item P31-10; CSS Fragmentation Module Level 3,
 * <a href="https://www.w3.org/TR/css-break-3/">https://www.w3.org/TR/css-break-3/</a>, sections 3 and 4, as far as a layout that was made
 * without pages can follow them). The document is laid out once as one tall column of the width of the page area; this class then chooses
 * the vertical positions at which one page ends and the next begins. The content is not moved: a page shows the band of the layout
 * between two positions, and a page that ends early (before a forced break, or above something that must not be split) leaves the rest
 * of its area empty.
 * <p>
 * Rules: a forced break (<code>break-before</code>/<code>break-after</code> with <code>page</code>, <code>always</code>,
 * <code>left</code>, <code>right</code>, <code>recto</code>, <code>verso</code>, and the aliases <code>page-break-*: always</code>) ends
 * the page at the edge of the box. Otherwise a page ends at its full height, moved up so that it does not cut through a line box, a
 * replaced element, a table row, a box with a transform, or a box with <code>break-inside: avoid</code> or <code>avoid-page</code>;
 * through the first <code>orphans</code> or the last <code>widows</code> lines of a block; or between a box with
 * <code>break-after: avoid</code> (or <code>avoid-page</code>) and the box that follows it (and the same for <code>break-before</code>).
 * Something that is taller than a page cannot be kept whole: requests to avoid a break are dropped first, and a line or image that is
 * still too tall is cut at the page height.
 * <p>
 * 끝난 레이아웃을 인쇄를 위해 쪽으로 자른다(계획 P31-10. CSS Fragmentation Module Level 3 의 3절과 4절을, 쪽 없이 만든 레이아웃이 따를 수 있는
 * 만큼). 문서는 쪽 영역의 폭을 가진 긴 단 하나로 한 번 배치하고, 이 클래스는 한 쪽이 끝나고 다음 쪽이 시작하는 세로 위치를 고른다. 내용은 옮기지
 * 않는다: 쪽은 두 위치 사이의 레이아웃 띠를 보이고, 일찍 끝난 쪽(강제 나눔 앞, 또는 갈라서는 안 되는 것의 위)은 영역의 나머지를 비워 둔다.
 * <p>
 * 규칙: 강제 나눔(<code>break-before</code>/<code>break-after</code> 의 <code>page</code>, <code>always</code>, <code>left</code>,
 * <code>right</code>, <code>recto</code>, <code>verso</code>, 그리고 별칭 <code>page-break-*: always</code>)은 상자의 가장자리에서 쪽을 끝낸다. 그
 * 밖에는 쪽이 온전한 높이에서 끝나되, 줄 상자, 치환 요소, 표의 행, 변환이 있는 상자, <code>break-inside: avoid</code> 나
 * <code>avoid-page</code> 인 상자를 가르지 않도록 위로 옮긴다. 블록의 처음 <code>orphans</code> 줄과 마지막 <code>widows</code> 줄 사이, 그리고
 * <code>break-after: avoid</code>(또는 <code>avoid-page</code>)인 상자와 그 뒤 상자 사이(<code>break-before</code> 도 같다)도 가르지 않는다. 쪽보다
 * 큰 것은 온전히 둘 수 없다: 나눔을 피하라는 요청을 먼저 버리고, 그래도 너무 큰 줄이나 이미지는 쪽 높이에서 자른다.
 */
public final class PageBreaks {

    /** Positions closer than this count as the same position (CSS pixels). / 이보다 가까운 위치는 같은 위치로 친다(CSS 픽셀). */
    private static final double EPSILON = 0.01;

    /** The largest number of pages; a layout that needs more is cut off. / 최대 쪽 수. 더 필요한 레이아웃은 잘린다. */
    public static final int MAX_PAGES = 10000;

    /**
     * Gives the height of the page area of a page.
     * <p>
     * 쪽의 쪽 영역 높이를 준다.
     */
    public interface Heights {

        /**
         * Returns the height that the content of a page may take.
         * <p>
         * 한 쪽의 내용이 차지할 수 있는 높이를 반환한다.
         *
         * @param pageNumber the number of the page, starting at 1 / 쪽 번호(1 부터)
         * @return the height in CSS pixels, greater than 0 / 높이(CSS 픽셀). 0 보다 크다
         */
        double pageHeight(int pageNumber);
    }

    /** The positions of forced breaks. / 강제 나눔의 위치. */
    private final List<Double> forced = new ArrayList<Double>();

    /** The bands {top, bottom} that must never be split. / 절대 가르면 안 되는 띠 {위, 아래}. */
    private final List<double[]> hard = new ArrayList<double[]>();

    /** The bands {top, bottom} that should not be split if it can be avoided. / 피할 수 있으면 가르지 않을 띠 {위, 아래}. */
    private final List<double[]> soft = new ArrayList<double[]>();

    /** The bottom edge of the lowest thing that is painted. / 그려지는 것 가운데 가장 아래쪽 가장자리. */
    private double painted;

    /**
     * Creates the collector.
     * <p>
     * 수집기를 만든다.
     */
    private PageBreaks() {
    }

    /**
     * Cuts a layout into pages.
     * <p>
     * 레이아웃을 쪽으로 자른다.
     *
     * @param layout  the layout of the whole document at the width of the page area / 쪽 영역의 폭으로 배치한 문서 전체의 레이아웃
     * @param heights the height of the page area of every page / 쪽마다의 쪽 영역 높이
     * @return the positions of the page edges: the first is 0 and page <code>i</code> (from 0) shows the band from position <code>i</code> to
     *         position <code>i + 1</code>; there is always at least one page / 쪽 가장자리의 위치: 처음은 0 이고 쪽 <code>i</code>(0 부터)는
     *         위치 <code>i</code> 부터 위치 <code>i + 1</code> 까지의 띠를 보인다. 쪽은 항상 하나 이상이다
     */
    public static double[] cut(LayoutResult layout, Heights heights) {
        PageBreaks p = new PageBreaks();
        LayoutBox root = layout.getRoot();
        if (root != null) {
            p.collect(root);
        }
        double total = Math.min(Math.max(0, layout.getContentHeight()), p.painted);
        List<Double> edges = new ArrayList<Double>();
        edges.add(Double.valueOf(0));
        double y = 0;
        Collections.sort(p.forced);
        // 내용이 남아 있는 동안 쪽을 하나씩 만든다.
        while (y < total - EPSILON && edges.size() <= MAX_PAGES) {
            double height = Math.max(1, heights.pageHeight(edges.size()));
            double limit = y + height;
            double end = p.forcedIn(y, Math.min(limit, total));
            if (Double.isNaN(end)) {
                end = limit >= total - EPSILON ? total : p.fit(y, limit);
            }
            edges.add(Double.valueOf(end));
            y = end;
        }
        if (edges.size() == 1) {
            // 그릴 것이 없는 문서도 빈 쪽 하나는 가진다.
            edges.add(Double.valueOf(Math.max(1, heights.pageHeight(1))));
        }
        double[] out = new double[edges.size()];
        // 목록을 배열로 옮긴다.
        for (int i = 0; i < out.length; i++) {
            out[i] = edges.get(i).doubleValue();
        }
        return out;
    }

    /**
     * Returns the first forced break behind a position and not behind a limit.
     * <p>
     * 한 위치 뒤이고 한도를 넘지 않는 첫 강제 나눔을 반환한다.
     *
     * @param from  the start of the page / 쪽의 시작
     * @param limit the largest position / 가장 큰 위치
     * @return the position, or NaN if there is none / 위치. 없으면 NaN
     */
    private double forcedIn(double from, double limit) {
        // 정렬된 위치를 앞에서부터 본다.
        for (int i = 0; i < forced.size(); i++) {
            double f = forced.get(i).doubleValue();
            if (f > from + EPSILON && f <= limit + EPSILON) {
                return f;
            }
        }
        return Double.NaN;
    }

    /**
     * Chooses the end of a page that has no forced break: the page height, moved up out of the bands that must not be split.
     * <p>
     * 강제 나눔이 없는 쪽의 끝을 고른다: 쪽 높이에서, 가르면 안 되는 띠 밖으로 위로 옮긴 곳.
     *
     * @param from  the start of the page / 쪽의 시작
     * @param limit the start plus the page height / 시작에 쪽 높이를 더한 곳
     * @return the end of the page, behind the start / 쪽의 끝. 시작보다 뒤이다
     */
    private double fit(double from, double limit) {
        double end = settle(from, limit, true);
        if (end <= from + EPSILON) {
            // 피하라는 요청까지 지키면 쪽에 아무것도 들어가지 않으므로 요청은 버린다.
            end = settle(from, limit, false);
        }
        // 그래도 들어가지 않는 것(쪽보다 큰 줄이나 이미지)은 쪽 높이에서 자른다.
        return end <= from + EPSILON ? limit : end;
    }

    /**
     * Moves a position up until no band contains it.
     * <p>
     * 어떤 띠도 그 위치를 품지 않을 때까지 위치를 위로 옮긴다.
     *
     * @param from     the start of the page / 쪽의 시작
     * @param limit    the position to start from / 시작할 위치
     * @param withSoft true to respect the bands that should not be split too / 가르지 않을 띠도 지키려면 true
     * @return the position; not behind the start if nothing fits / 위치. 들어가는 것이 없으면 시작보다 뒤가 아니다
     */
    private double settle(double from, double limit, boolean withSoft) {
        double end = limit;
        boolean moved = true;
        // 옮긴 자리가 다른 띠 안일 수 있으므로 더 옮길 것이 없을 때까지 되풀이한다.
        while (moved && end > from + EPSILON) {
            moved = false;
            double up = raise(hard, end);
            if (withSoft) {
                up = Math.min(up, raise(soft, end));
            }
            if (up < end - EPSILON) {
                end = up;
                moved = true;
            }
        }
        return end;
    }

    /**
     * Returns the top of the highest band that contains a position, or the position itself if none does.
     * <p>
     * 위치를 품은 띠 가운데 가장 위에 있는 것의 위쪽을 반환한다. 품은 띠가 없으면 그 위치 그대로이다.
     *
     * @param bands the bands / 띠
     * @param at    the position / 위치
     * @return the position to cut at / 자를 위치
     */
    private static double raise(List<double[]> bands, double at) {
        double out = at;
        // 띠마다 위치가 그 안쪽이면 띠의 위로 올린다.
        for (int i = 0; i < bands.size(); i++) {
            double[] b = bands.get(i);
            if (b[0] < at - EPSILON && b[1] > at + EPSILON && b[0] < out) {
                out = b[0];
            }
        }
        return out;
    }

    /**
     * Walks the boxes of the layout and collects the forced breaks, the bands and the lowest painted edge.
     * <p>
     * 레이아웃의 상자를 훑어 강제 나눔, 띠, 그려지는 가장 아래 가장자리를 모은다.
     *
     * @param root the root box / 루트 상자
     */
    private void collect(LayoutBox root) {
        List<LayoutBox> stack = new ArrayList<LayoutBox>();
        stack.add(root);
        // 명시적 스택으로 모든 상자를 훑는다(깊이 제한이 없다).
        while (!stack.isEmpty()) {
            LayoutBox b = stack.remove(stack.size() - 1);
            // 뷰포트에 고정된 상자는 쪽마다 되풀이되므로 쪽을 나누는 데 관여하지 않는다.
            if (b.isFixedPosition()) {
                continue;
            }
            double top = b.y;
            double bottom = b.y + b.height;
            boolean atomic = b.type == LayoutBox.Type.LINE || b.type == LayoutBox.Type.REPLACED || b.type == LayoutBox.Type.TABLE_ROW
                    || b.transform != null;
            if (atomic && b.height > 0) {
                hard.add(new double[] { top, bottom });
            }
            if (paints(b)) {
                painted = Math.max(painted, bottom);
            }
            if (b.style != null && b.node != null && b.type != LayoutBox.Type.LINE && b.type != LayoutBox.Type.TEXT && !b.inlineLevel) {
                breaksOf(b, top, bottom);
            }
            if (b.transform == null) {
                lines(b);
                stack.addAll(b.children);
            }
        }
    }

    /**
     * Tells whether a box paints something of its own that has to appear on a page.
     * <p>
     * 상자가 쪽에 나타나야 하는 자기 것을 그리는지 알려 준다.
     *
     * @param b the box / 상자
     * @return true for lines, replaced elements and boxes with a border or a background / 줄, 치환 요소, border 나 배경이 있는 상자이면 true
     */
    private static boolean paints(LayoutBox b) {
        if (b.height <= 0) {
            return false;
        }
        if (b.type == LayoutBox.Type.LINE || b.type == LayoutBox.Type.REPLACED) {
            return true;
        }
        if (b.borderTop + b.borderBottom + b.borderLeft + b.borderRight > 0) {
            return true;
        }
        ComputedStyle s = b.style;
        // 루트와 body 의 배경은 캔버스로 퍼지므로 내용의 끝을 정하지 않는다.
        if (s == null || b.parent == null || b.parent.parent == null) {
            return false;
        }
        return (s.color(Prop.BACKGROUND_COLOR) >>> 24) != 0;
    }

    /**
     * Adds the forced breaks and the bands that the break properties of a block-level box ask for.
     * <p>
     * 블록 수준 상자의 나눔 속성이 요청하는 강제 나눔과 띠를 더한다.
     *
     * @param b      the box / 상자
     * @param top    its top edge / 위쪽 가장자리
     * @param bottom its bottom edge / 아래쪽 가장자리
     */
    private void breaksOf(LayoutBox b, double top, double bottom) {
        String before = keyword(b, Prop.BREAK_BEFORE);
        String after = keyword(b, Prop.BREAK_AFTER);
        String inside = keyword(b, Prop.BREAK_INSIDE);
        if (isForced(before)) {
            forced.add(Double.valueOf(top));
        }
        if (isForced(after)) {
            forced.add(Double.valueOf(bottom));
        }
        if (isAvoid(inside) && b.height > 0) {
            soft.add(new double[] { top, bottom });
        }
        LayoutBox parent = b.parent;
        if (parent == null || !(isAvoid(before) || isAvoid(after))) {
            return;
        }
        int at = parent.children.indexOf(b);
        // 앞 형제와 떨어지지 않으려면 앞 형제의 마지막 덩어리부터 이 상자의 첫 덩어리까지를 한 띠로 묶는다.
        if (isAvoid(before)) {
            LayoutBox prev = sibling(parent, at, -1);
            if (prev != null) {
                soft.add(new double[] { edge(prev, false)[0], edge(b, true)[1] });
            }
        }
        if (isAvoid(after)) {
            LayoutBox next = sibling(parent, at, 1);
            if (next != null) {
                soft.add(new double[] { edge(b, false)[0], edge(next, true)[1] });
            }
        }
    }

    /**
     * Returns the nearest sibling of a child in a direction that is part of the normal flow.
     * <p>
     * 한 방향으로 가장 가까운, 정상 흐름에 속한 형제를 반환한다.
     *
     * @param parent the parent box / 부모 상자
     * @param at     the position of the child / 자식의 위치
     * @param step   1 for the next sibling, -1 for the previous one / 다음 형제이면 1, 이전 형제이면 -1
     * @return the sibling or null / 형제. 없으면 null
     */
    private static LayoutBox sibling(LayoutBox parent, int at, int step) {
        // 정상 흐름에 속한 형제가 나올 때까지 그 방향으로 간다.
        for (int i = at + step; i >= 0 && i < parent.children.size(); i += step) {
            LayoutBox k = parent.children.get(i);
            if (k.floatSide == LayoutBox.FLOAT_NONE && !k.isAbsolutelyPositioned() && k.type != LayoutBox.Type.MARKER && k.height > 0) {
                return k;
            }
        }
        return null;
    }

    /**
     * Returns the band of the first or the last piece of a box that cannot be split: its first or last line, or the box itself if it has
     * no lines.
     * <p>
     * 상자에서 가를 수 없는 첫 덩어리 또는 마지막 덩어리의 띠를 반환한다: 첫 줄이나 마지막 줄, 줄이 없으면 상자 자체.
     *
     * @param box   the box / 상자
     * @param first true for the first piece / 첫 덩어리이면 true
     * @return {top, bottom} / {위, 아래}
     */
    private static double[] edge(LayoutBox box, boolean first) {
        LayoutBox b = box;
        // 줄 상자를 만날 때까지 첫 자식 또는 마지막 자식으로 내려간다.
        for (int depth = 0; depth < 64; depth++) {
            if (b.type == LayoutBox.Type.LINE || b.type == LayoutBox.Type.REPLACED || b.children.isEmpty()) {
                break;
            }
            LayoutBox next = null;
            int i = first ? 0 : b.children.size() - 1;
            // 높이가 있는 정상 흐름 자식을 가장자리에서부터 찾는다.
            for (; i >= 0 && i < b.children.size(); i += first ? 1 : -1) {
                LayoutBox k = b.children.get(i);
                if (k.floatSide == LayoutBox.FLOAT_NONE && !k.isAbsolutelyPositioned() && k.height > 0 && k.type != LayoutBox.Type.TEXT) {
                    next = k;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            b = next;
        }
        return new double[] { b.y, b.y + b.height };
    }

    /**
     * Adds the bands that keep the first <code>orphans</code> and the last <code>widows</code> lines of a block together.
     * <p>
     * 블록의 처음 <code>orphans</code> 줄과 마지막 <code>widows</code> 줄을 함께 두는 띠를 더한다.
     *
     * @param box a box / 상자
     */
    private void lines(LayoutBox box) {
        List<LayoutBox> all = new ArrayList<LayoutBox>();
        // 상자의 줄 상자를 모은다.
        for (int i = 0; i < box.children.size(); i++) {
            if (box.children.get(i).type == LayoutBox.Type.LINE && box.children.get(i).height > 0) {
                all.add(box.children.get(i));
            }
        }
        int n = all.size();
        if (n < 2) {
            return;
        }
        int orphans = count(box, Prop.ORPHANS);
        int widows = count(box, Prop.WIDOWS);
        if (n < orphans + widows) {
            // 줄이 모자라면 어디서도 나눌 수 없으므로 문단 전체를 한 띠로 묶는다.
            soft.add(new double[] { all.get(0).y, all.get(n - 1).y + all.get(n - 1).height });
            return;
        }
        if (orphans > 1) {
            soft.add(new double[] { all.get(0).y, all.get(orphans - 1).y + all.get(orphans - 1).height });
        }
        if (widows > 1) {
            soft.add(new double[] { all.get(n - widows).y, all.get(n - 1).y + all.get(n - 1).height });
        }
    }

    /**
     * Reads <code>orphans</code> or <code>widows</code> of a box.
     * <p>
     * 상자의 <code>orphans</code> 나 <code>widows</code> 를 읽는다.
     *
     * @param box the box / 상자
     * @param id  the property / 속성
     * @return the number of lines, at least 1; 2 if the box has no style / 줄 수. 1 이상이며 상자에 스타일이 없으면 2
     */
    private static int count(LayoutBox box, int id) {
        ComputedStyle s = box.style;
        // 익명 블록은 가장 가까운, 스타일이 있는 조상의 값을 쓴다(두 속성은 상속된다).
        for (LayoutBox b = box; s == null && b != null; b = b.parent) {
            s = b.style;
        }
        double v = s == null ? 2 : s.number(id);
        return Double.isNaN(v) ? 2 : Math.max(1, (int) Math.round(v));
    }

    /**
     * Returns the keyword of a break property of a box.
     * <p>
     * 상자의 나눔 속성 키워드를 반환한다.
     *
     * @param b  the box / 상자
     * @param id the property / 속성
     * @return the keyword, <code>auto</code> if there is none / 키워드. 없으면 <code>auto</code>
     */
    private static String keyword(LayoutBox b, int id) {
        String v = b.style.keyword(id);
        return v == null ? "auto" : v;
    }

    /**
     * Tells whether a break value forces a page break.
     * <p>
     * 나눔 값이 쪽을 강제로 나누는지 알려 준다.
     *
     * @param v the keyword / 키워드
     * @return true if the break is forced / 강제이면 true
     */
    private static boolean isForced(String v) {
        return "page".equals(v) || "always".equals(v) || "all".equals(v) || "left".equals(v) || "right".equals(v) || "recto".equals(v)
                || "verso".equals(v);
    }

    /**
     * Tells whether a break value asks to avoid a page break.
     * <p>
     * 나눔 값이 쪽을 나누지 말라고 요청하는지 알려 준다.
     *
     * @param v the keyword / 키워드
     * @return true to avoid a break / 나누지 말라면 true
     */
    private static boolean isAvoid(String v) {
        return "avoid".equals(v) || "avoid-page".equals(v);
    }
}
