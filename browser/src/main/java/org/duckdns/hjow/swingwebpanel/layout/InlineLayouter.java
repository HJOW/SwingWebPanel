package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.text.BreakIterator;
import java.text.Bidi;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * Inline formatting (plan item P09-03): turns the inline items of a block container into line boxes. White space is collapsed or kept
 * according to <code>white-space</code>; <code>text-transform</code> is applied; text is broken into lines at the break opportunities of
 * {@link BreakIterator} (<code>overflow-wrap</code>, <code>word-break</code> and <code>white-space</code> adjust them); atomic inline boxes
 * are laid out and placed; <code>text-indent</code>, <code>text-align</code> (including <code>justify</code>), <code>line-height</code> and
 * <code>vertical-align</code> are applied; lines with right-to-left text are reordered visually with {@link Bidi}. Every line becomes a
 * {@link LayoutBox.Type#LINE} box whose children are the {@link LayoutBox.Type#INLINE} fragments of the inline elements, the
 * {@link LayoutBox.Type#TEXT} fragments and the atomic boxes, all positioned relative to the line.
 * <p>
 * Simplifications that are documented behaviour: inline elements use the metrics of their own primary font (a fallback font only changes
 * the fragment box); a box aligned with <code>top</code>/<code>bottom</code> uses its own extent; in lines that need bidi reordering the
 * horizontal margin, border and padding of inline elements are not applied. Floats (P10-01): a line is as wide as the space between
 * the floats at the top of the line (a float that starts lower, inside the height of the line, does not shorten it); a line that cannot
 * hold its first unbreakable unit beside floats moves down; a float found in the middle of a line goes to the top of that line if it fits
 * beside the content that is already there, otherwise to the top of the next line.
 * <p>
 * Vertical text (P10-03-e, CSS Writing Modes Level 3): in a container with a vertical writing mode the characters of every text run
 * are classified by {@link VerticalText} as sideways, upright or combined (<code>text-orientation</code>,
 * <code>text-combine-upright</code>). The line is laid out like a horizontal line in the logical dimensions; a sideways run is measured
 * as a string, an upright character takes a cell of one em (the font size plus the letter spacing) and a combined group one em, and
 * a text piece is split into one fragment ({@link LayoutBox#getGlyphMode()}) for every run of the same kind. A line is not broken
 * inside a combined group, between Hangul syllables (without <code>word-break: keep-all</code>) and next to a combined group
 * a break is allowed. <code>text-orientation: upright</code> forces the direction to ltr. The orthogonal layouter later rotates the
 * fragments into the physical orientation.
 * <p>
 * 인라인 서식(계획 P09-03): 블록 컨테이너의 인라인 항목을 줄 상자로 만든다. 공백은 <code>white-space</code> 에 따라 접거나 유지하고,
 * <code>text-transform</code> 을 적용하며, 텍스트는 {@link BreakIterator} 의 줄바꿈 기회에서 줄로 나누고(<code>overflow-wrap</code>,
 * <code>word-break</code>, <code>white-space</code> 가 조정), 원자적 인라인 상자는 배치해 놓고, <code>text-indent</code>, <code>text-align</code>(
 * <code>justify</code> 포함), <code>line-height</code>, <code>vertical-align</code> 을 적용하며, 오른쪽에서 왼쪽으로 쓰는 텍스트가 있는 줄은 {@link Bidi} 로
 * 시각 순서로 재배열한다. 각 줄은 {@link LayoutBox.Type#LINE} 상자가 되고 자식은 인라인 요소의 {@link LayoutBox.Type#INLINE} 조각,
 * {@link LayoutBox.Type#TEXT} 조각, 원자적 상자이며 모두 줄 기준으로 위치한다.
 * <p>
 * 문서화된 단순화: 인라인 요소는 자신의 주 글꼴 메트릭을 쓴다(대체 글꼴은 조각 상자만 바꾼다). <code>top</code>/<code>bottom</code> 정렬 상자는 자기 범위를
 * 쓴다. 양방향 재배열이 필요한 줄에서는 인라인 요소의 가로 margin, border, padding 을 적용하지 않는다. float(P10-01): 줄의 너비는 줄 위쪽에서 float 사이의
 * 공간이다(줄 높이 안에서 더 낮게 시작하는 float 는 줄을 줄이지 않는다). float 옆에 첫 쪼갤 수 없는 단위가 들어가지 않는 줄은 아래로 내려간다. 줄 중간에서
 * 만난 float 는 이미 있는 내용 옆에 들어가면 그 줄 위쪽에, 아니면 다음 줄 위쪽에 놓인다.
 * <p>
 * 세로쓰기(P10-03-e, CSS Writing Modes Level 3): 세로 쓰기 모드의 컨테이너에서는 텍스트 구간의 글자를 {@link VerticalText} 가 눕힘, 똑바로, 조합으로 분류한다
 * (<code>text-orientation</code>, <code>text-combine-upright</code>). 줄은 논리적 크기에서 가로 줄처럼 배치한다. 눕힌 구간은 문자열로 재고, 똑바로 선 글자는 1em
 * (글꼴 크기와 글자 간격) 한 칸, 조합 묶음은 1em 을 차지하며, 텍스트 조각은 종류가 같은 구간마다 조각({@link LayoutBox#getGlyphMode()}) 하나로 나뉜다. 조합 묶음 안에서는
 * 줄을 나누지 않고, 한글 음절 사이(<code>word-break: keep-all</code> 이 아니면)와 조합 묶음 곁에서는 줄바꿈이 가능하다. <code>text-orientation: upright</code> 는 방향을
 * ltr 로 강제한다. 직교 레이아웃이 나중에 조각을 물리적 방향으로 돌린다.
 */
final class InlineLayouter {

    /** Tolerance when comparing widths. 너비 비교 허용 오차. */
    private static final double EPS = 0.001;

    /** The size of a small capital relative to the font size (what browsers synthesize when a font has no small capitals). 작은 대문자의 크기(글꼴 크기에 대한 비율. 글꼴에 작은 대문자가 없을 때 브라우저가 합성하는 값). */
    private static final double SMALL_CAPS_SCALE = 0.8;
    /** The size of superscript and subscript text (font-variant-position) relative to the font size. 위첨자와 아래첨자(font-variant-position) 글자의 글꼴 크기 대비 크기. */
    private static final double POSITION_SCALE = 0.7;

    /** The soft hyphen character U+00AD, a place where a word may be broken with a hyphen. 소프트 하이픈 U+00AD. 단어를 하이픈과 함께 나눌 수 있는 자리. */
    private static final char SOFT_HYPHEN = (char) 0x00AD;

    /** The zero width space U+200B, which stands for a soft hyphen in the laid out text: a break opportunity that has no width and draws nothing. 폭 없는 공백 U+200B. 배치하는 텍스트에서 소프트 하이픈을 대신하며 폭이 없고 아무것도 그리지 않는 줄바꿈 기회이다. */
    private static final char ZERO_WIDTH_SPACE = (char) 0x200B;

    /** Measures text. 텍스트 측정기. */
    private final TextMeasure tm;
    /** The block layouter, for atomic boxes. 원자적 상자를 위한 블록 레이아웃. */
    private final BlockLayouter bl;

    /**
     * Creates the inline layouter.
     * <p>
     * 인라인 레이아웃을 만든다.
     *
     * @param tm text measurement / 텍스트 측정기
     * @param bl block layouter / 블록 레이아웃
     */
    InlineLayouter(TextMeasure tm, BlockLayouter bl) {
        this.tm = tm;
        this.bl = bl;
    }

    /**
     * Lays out the lines of a block container and creates its line boxes.
     * <p>
     * 블록 컨테이너의 줄을 배치하고 줄 상자를 만든다.
     *
     * @param b           the block container with inline items / 인라인 항목을 가진 블록 컨테이너
     * @param cw          content width / content 너비
     * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
     * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
     * @return height of all lines / 모든 줄의 높이
     */
    double layout(LayoutBox b, double cw, double contentLeft, double contentTop) {
        List<InlineItem> items = b.items;
        LayoutBox flProto = null;
        // ::first-line 이 있으면 먼저 첫 줄 스타일로 탐색해 첫 줄의 끝을 얻고 그 지점까지만 첫 줄 스타일을 적용한 항목으로 배치한다(P10-04-f).
        if (FirstLine.applies(b)) {
            flProto = FirstLine.proto(b);
            List<InlineItem> all = FirstLine.restyled(b, flProto);
            Flow probe = new Flow(b, all, flProto, cw, true, true, 0, 0);
            probe.run();
            items = probe.probeEnd == null ? all : FirstLine.split(b, all, probe.probeEnd);
        }
        double width = cw;
        double shift = 0;
        // text-wrap: balance 이면 줄 수를 유지하는 가장 좁은 너비로 배치하고 줄을 정렬에 맞게 옮긴다(CSS Text 4 text-wrap-style).
        if (flProto == null && balanced(b) && !Double.isInfinite(cw) && !Double.isNaN(cw) && (bl.fc == null || bl.fc.size() == 0)) {
            double bw = balancedWidth(b, items, cw);
            if (bw < cw - 0.5) {
                width = bw;
                String align = Css.kw(b.style, Prop.TEXT_ALIGN);
                shift = "center".equals(align) ? (cw - bw) / 2 : "right".equals(align) || "end".equals(align) ? cw - bw : 0;
            }
        }
        int prettyLine = -1;
        double prettyTo = 0;
        // text-wrap: pretty 이면 마지막 줄이 한두 단어만 남는 경우를 막도록 끝에서 둘째 줄을 줄인다(CSS Text 4 text-wrap-style).
        if (flProto == null && pretty(b) && !Double.isInfinite(cw) && !Double.isNaN(cw) && (bl.fc == null || bl.fc.size() == 0)) {
            double[] pr = prettyShrink(b, items, cw);
            if (pr != null) {
                prettyLine = (int) pr[0];
                prettyTo = pr[1];
            }
        }
        int before = b.children.size();
        Flow f = new Flow(b, items, flProto, width, false, false, contentLeft, contentTop);
        f.shrinkLine = prettyLine;
        f.shrinkTo = prettyTo;
        f.run();
        // 줄인 너비로 배치한 줄은 컨테이너 안에서 정렬되도록 옮긴다.
        for (int i = before; shift != 0 && i < b.children.size(); i++) {
            if (b.children.get(i).type == LayoutBox.Type.LINE) {
                b.children.get(i).lx += shift;
            }
        }
        // 줄인 줄은 줄인 너비 안에서 정렬되었으므로 가운데나 오른쪽 정렬이면 컨테이너 안의 자리로 옮긴다.
        if (prettyLine >= 0) {
            String align = Css.kw(b.style, Prop.TEXT_ALIGN);
            double move = "center".equals(align) ? (cw - prettyTo) / 2 : ("right".equals(align) || "end".equals(align) ? cw - prettyTo : 0);
            int seen = 0;
            // 줄 상자를 세어 줄인 줄을 찾는다.
            for (int i = before; move != 0 && i < b.children.size(); i++) {
                if (b.children.get(i).type == LayoutBox.Type.LINE) {
                    if (seen == prettyLine) {
                        b.children.get(i).lx += move;
                        break;
                    }
                    seen++;
                }
            }
        }
        return f.y;
    }

    /**
     * Tells whether a container avoids short last lines: <code>text-wrap-style: pretty</code> with wrapping text in a horizontal left-to-right
     * container (CSS Text Level 4, https://www.w3.org/TR/css-text-4/#text-wrap-style).
     * <p>
     * 컨테이너가 짧은 마지막 줄을 피하는지 알려 준다: 가로쓰기, 왼쪽에서 오른쪽 컨테이너에서 줄바꿈하는 텍스트의 <code>text-wrap-style: pretty</code>(CSS Text Level 4,
     * https://www.w3.org/TR/css-text-4/#text-wrap-style).
     *
     * @param b the container / 컨테이너
     * @return true for pretty wrapping / pretty 이면 true
     */
    private static boolean pretty(LayoutBox b) {
        return "pretty".equals(Css.kw(b.style, Prop.TEXT_WRAP_STYLE)) && "wrap".equals(Css.kw(b.style, Prop.TEXT_WRAP_MODE)) && b.frame == Css.FRAME_H
                && !"rtl".equals(Css.kw(b.style, Prop.DIRECTION)) && !Css.whiteSpace(b.style).startsWith("pre");
    }

    /**
     * The fraction of the content width below which the last line of a paragraph counts as an orphan for <code>text-wrap: pretty</code>.
     * <p>
     * <code>text-wrap: pretty</code> 에서 문단의 마지막 줄이 이보다 짧으면 고아로 보는 content 너비의 비율.
     */
    private static final double PRETTY_MIN_LAST = 0.25;

    /**
     * The widest part of the content width that the second last line may give up so that a word goes down to a short last line (a paragraph
     * is never made more than this much ragged).
     * <p>
     * 짧은 마지막 줄로 단어가 내려가도록 끝에서 둘째 줄이 내줄 수 있는 content 너비의 최대 비율(문단이 이보다 더 들쭉날쭉해지지는 않는다).
     */
    private static final double PRETTY_MAX_SHRINK = 0.4;

    /**
     * Looks for a narrower width of the second last line with which a short last line (an orphan) becomes longer without adding a line: the
     * paragraph is laid out at full width; if its last line is shorter than {@link #PRETTY_MIN_LAST} of the width, the second last line is
     * narrowed step by step up to {@link #PRETTY_MAX_SHRINK} until the last line is long enough and the line count is the same. The other
     * lines keep the full width.
     * <p>
     * 짧은 마지막 줄(고아)이 줄을 늘리지 않고 길어지게 하는 끝에서 둘째 줄의 더 좁은 너비를 찾는다: 문단을 전체 너비로 배치해 마지막 줄이 너비의
     * {@link #PRETTY_MIN_LAST} 보다 짧으면, 마지막 줄이 충분히 길고 줄 수가 같아질 때까지 끝에서 둘째 줄을 {@link #PRETTY_MAX_SHRINK} 까지 조금씩 줄인다.
     * 다른 줄은 전체 너비를 유지한다.
     *
     * @param b     the container / 컨테이너
     * @param items the items / 항목
     * @param cw    the full content width / 전체 content 너비
     * @return {index of the line to narrow, its width}, or null if the paragraph is left alone / {줄일 줄의 번호, 그 너비}. 그대로 두면 null
     */
    private double[] prettyShrink(LayoutBox b, List<InlineItem> items, double cw) {
        Flow full = new Flow(b, items, null, cw, true, false, 0, 0);
        full.run();
        int n = full.lineCount;
        if (n < 2 || full.lastLineWidth >= PRETTY_MIN_LAST * cw) {
            return null;
        }
        double step = cw * PRETTY_MAX_SHRINK / 16;
        // 끝에서 둘째 줄을 조금씩 좁혀 가며 줄 수가 같고 마지막 줄이 충분히 길어지는 첫 너비를 찾는다.
        for (int k = 1; k <= 16; k++) {
            double w = cw - k * step;
            Flow t = new Flow(b, items, null, cw, true, false, 0, 0);
            t.shrinkLine = n - 2;
            t.shrinkTo = w;
            t.run();
            if (t.lineCount > n) {
                return null;
            }
            if (t.lineCount == n && t.lastLineWidth >= PRETTY_MIN_LAST * cw) {
                return new double[] { n - 2, w };
            }
        }
        return null;
    }

    /**
     * Tells whether a code point is a CJK letter unit that justification may spread (Han ideographs, Hiragana, Katakana, Hangul syllables and the compatibility forms).
     * <p>
     * 코드 포인트가 양쪽 정렬이 늘일 수 있는 한중일 글자 단위(한자, 히라가나, 가타카나, 한글 음절, 호환 형태)인지 알려 준다.
     *
     * @param cp the code point / 코드 포인트
     * @return true for a CJK letter / 한중일 글자이면 true
     */
    static boolean isCjk(int cp) {
        Character.UnicodeBlock b = Character.UnicodeBlock.of(cp);
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS || b == Character.UnicodeBlock.HIRAGANA || b == Character.UnicodeBlock.KATAKANA
                || b == Character.UnicodeBlock.HANGUL_SYLLABLES || b == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS && cp >= 0xFF66 && cp <= 0xFF9F
                || (cp >= 0x20000 && cp <= 0x2FA1F);
    }

    /**
     * Returns the end of the character (grapheme cluster) that starts at an index: the code point, the combining marks, variation selectors and
     * zero width joiners that follow it, and the character that a joiner joins; conjoining Hangul jamo vowels and final consonants stay with their syllable.
     * <p>
     * 한 인덱스에서 시작하는 글자(자소 클러스터)의 끝을 반환한다: 코드 포인트, 뒤따르는 결합 부호, 이체자 선택자, 폭 없는 접합자, 접합자가 잇는 글자이며
     * 이어지는 한글 자모의 모음과 받침은 음절과 함께 둔다.
     *
     * @param t the text / 텍스트
     * @param i the index of the first char of the character / 글자의 첫 char 인덱스
     * @return the index after the character / 글자 뒤의 인덱스
     */
    static int nextCluster(String t, int i) {
        int j = i + Character.charCount(t.codePointAt(i));
        // 결합 부호나 접합자가 이어지는 동안 같은 글자로 본다.
        while (j < t.length()) {
            int cp = t.codePointAt(j);
            int type = Character.getType(cp);
            int prev = t.codePointBefore(j);
            boolean joins = type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK || cp == 0x200D
                    || (cp >= 0xFE00 && cp <= 0xFE0F) || prev == 0x200D || (cp >= 0x1160 && cp <= 0x11FF) || (cp >= 0xD7B0 && cp <= 0xD7FF);
            if (!joins) {
                break;
            }
            j += Character.charCount(cp);
        }
        return j;
    }

    /**
     * Tells whether the lines of a container are balanced: <code>text-wrap-style: balance</code> with wrapping text in a horizontal left-to-right
     * container (CSS Text Level 4, <a href="https://www.w3.org/TR/css-text-4/#text-wrap-style">https://www.w3.org/TR/css-text-4/#text-wrap-style</a>).
     * <p>
     * 컨테이너의 줄을 균형 맞추는지 알려 준다: 가로쓰기, 왼쪽에서 오른쪽 컨테이너에서 줄바꿈하는 텍스트의 <code>text-wrap-style: balance</code>(CSS Text Level 4,
     * <a href="https://www.w3.org/TR/css-text-4/#text-wrap-style">https://www.w3.org/TR/css-text-4/#text-wrap-style</a>).
     *
     * @param b the container / 컨테이너
     * @return true if the lines are balanced / 줄을 균형 맞추면 true
     */
    private static boolean balanced(LayoutBox b) {
        return "balance".equals(Css.kw(b.style, Prop.TEXT_WRAP_STYLE)) && "wrap".equals(Css.kw(b.style, Prop.TEXT_WRAP_MODE))
                && b.frame == Css.FRAME_H && !"rtl".equals(Css.kw(b.style, Prop.DIRECTION)) && !Css.whiteSpace(b.style).startsWith("pre");
    }

    /**
     * The most lines a balanced paragraph may have (Chromium balances at most six lines, which keeps the search cheap).
     * <p>
     * 균형을 맞추는 문단의 최대 줄 수(Chromium 은 여섯 줄까지만 맞추며 이는 탐색 비용을 낮춘다).
     */
    private static final int MAX_BALANCED_LINES = 6;

    /**
     * Finds the narrowest width at which the content still fills the same number of lines as at the full width: the lines are then as even as
     * the breaking allows. The search halves the interval between the width of the longest unbreakable unit and the full width, laying the text out
     * without creating boxes. A paragraph of one line or more than {@link #MAX_BALANCED_LINES} lines keeps the full width.
     * <p>
     * 전체 너비에서와 같은 줄 수를 유지하는 가장 좁은 너비를 찾는다. 그러면 줄이 나눔이 허락하는 만큼 고르게 된다. 가장 긴 쪼갤 수 없는 단위의 너비와 전체 너비 사이를 반씩
     * 줄여 가며 상자를 만들지 않고 배치한다. 한 줄이거나 {@link #MAX_BALANCED_LINES} 줄을 넘는 문단은 전체 너비를 유지한다.
     *
     * @param b     the container / 컨테이너
     * @param items the items / 항목
     * @param cw    the full content width / 전체 content 너비
     * @return the width to lay out with / 배치에 쓸 너비
     */
    private double balancedWidth(LayoutBox b, List<InlineItem> items, double cw) {
        Flow full = new Flow(b, items, null, cw, true, false, 0, 0);
        full.run();
        int n = full.lineCount;
        if (n < 2 || n > MAX_BALANCED_LINES) {
            return cw;
        }
        double lo = widest(b, 0);
        double hi = cw;
        // 줄 수가 같은 가장 좁은 너비를 이분 탐색으로 찾는다(너비가 좁을수록 줄이 늘어나므로 단조롭다).
        for (int i = 0; i < 12 && hi - lo > 0.5; i++) {
            double mid = (lo + hi) / 2;
            Flow t = new Flow(b, items, null, mid, true, false, 0, 0);
            t.run();
            if (t.lineCount <= n) {
                hi = mid;
            } else {
                lo = mid;
            }
        }
        return hi;
    }

    /**
     * Returns the width of the widest line when the container is laid out at a given width, without creating boxes (used for min-content
     * and max-content widths).
     * <p>
     * 컨테이너를 주어진 너비로 배치했을 때 가장 넓은 줄의 너비를 상자를 만들지 않고 반환한다(min-content, max-content 너비에 쓴다).
     *
     * @param b     the block container / 블록 컨테이너
     * @param avail available width (0 for min-content, infinity for max-content) / 사용 가능한 너비(min-content 는 0, max-content 는 무한대)
     * @return width of the widest line / 가장 넓은 줄의 너비
     */
    double widest(LayoutBox b, double avail) {
        Flow f = new Flow(b, b.items, null, avail, true, false, 0, 0);
        // 호환 모드의 너비가 auto 인 표 셀의 min-content 에서는 이미지 앞뒤에 줄바꿈 기회가 없다(호환 모드 표준 3.8).
        f.glueImages = avail == 0 && bl.quirksMode && b.type == LayoutBox.Type.TABLE_CELL && b.node != null && b.style.isAuto(Prop.WIDTH);
        f.run();
        return f.widest;
    }

    /**
     * Returns the style of the strut of the first line of a container whose <code>::first-line</code> applies: the style of the
     * container with the first-line style laid over it (the line height and the font of the first line).
     * <p>
     * <code>::first-line</code> 이 적용되는 컨테이너의 첫 줄 strut 스타일을 반환한다: 컨테이너 스타일 위에 첫 줄 스타일을 덮은 것(첫 줄의 줄 높이와 글꼴).
     *
     * @param b the container / 컨테이너
     * @return the style / 스타일
     */
    private static ComputedStyle bsOverlay(LayoutBox b) {
        return b.style.overlay(b.firstLineBase, b.firstLineStyle, FirstLine.IDS);
    }

    /** A processed piece of the inline content. 처리된 인라인 콘텐츠 조각. */
    private static final class Seg {
        /** Kind of the underlying item. 원래 항목의 종류. */
        InlineItem.Kind kind;
        /** Processed text. 처리된 텍스트. */
        String text;
        /** Source offsets of the characters of the text, or null. 텍스트 문자의 원본 위치. 없으면 null. */
        int[] src;
        /** Font of the text. 텍스트 글꼴. */
        Font font;
        /** True if the text waits for a web font in the block period of its <code>font-display</code> and is laid out but not painted. 텍스트가 <code>font-display</code> 의 block 기간에 웹 글꼴을 기다려 배치하되 그리지 않으면 true. */
        boolean invisible;
        /** Font ascent. 글꼴 ascent. */
        double asc;
        /** Font descent. 글꼴 descent. */
        double desc;
        /** Distance that the text is raised above the baseline of its inline box (negative: lowered), for superscript and subscript. 텍스트를 인라인 상자의 기준선보다 올리는 거리(음수는 내림). 위첨자와 아래첨자용. */
        double baseShift;
        /** Room that the emphasis marks need above the box of the text (text-emphasis over). 강조점이 텍스트 상자 위에 필요로 하는 공간(text-emphasis over). */
        double emUp;
        /** Room that the emphasis marks need below the box of the text (text-emphasis under). 강조점이 텍스트 상자 아래에 필요로 하는 공간(text-emphasis under). */
        double emDown;
        /** Letter spacing. 글자 간격. */
        double ls;
        /** Word spacing. 단어 간격. */
        double ws;
        /** Style of the text. 텍스트 스타일. */
        ComputedStyle style;
        /** Source node. 원본 노드. */
        Node node;
        /** Innermost inline element. 가장 안쪽 인라인 요소. */
        LayoutBox owner;
        /** Inline element or atomic box. 인라인 요소 또는 원자적 상자. */
        LayoutBox box;
        /** Edge flag of OPEN and CLOSE. OPEN, CLOSE 의 가장자리 표시. */
        boolean edge;
        /** Decoration bits. 장식 비트. */
        int deco;
        /** True if no soft wrap is allowed. 부드러운 줄바꿈이 금지되면 true. */
        boolean nowrap;
        /** True if white space collapses. 공백이 접히면 true. */
        boolean collapse;
        /** True if an overlong word may be broken anywhere. 긴 단어를 아무 데서나 끊을 수 있으면 true. */
        boolean breakWord;
        /** True for <code>word-break: break-all</code>. word-break: break-all 이면 true. */
        boolean breakAll;
        /** True for <code>word-break: keep-all</code>. word-break: keep-all 이면 true. */
        boolean keepAll;
        /** The keyword of <code>line-break</code> (auto, loose, normal or strict) that sets the strictness of the rules for CJK text. CJK 텍스트의 규칙 엄격도를 정하는 <code>line-break</code> 키워드(auto, loose, normal, strict). */
        String lineBreak;
        /**
         * The characters of the text that are soft hyphens (U+00AD, CSS Text 3 section 5.1): the text holds a zero-width space at those places, which
         * is a break opportunity that draws nothing, and a hyphen is drawn there when the line breaks. Null if there is none.
         * 텍스트에서 소프트 하이픈(U+00AD, CSS Text 3 5.1 절)이었던 글자: 텍스트에는 그 자리에 폭 없는 공백이 있어 아무것도 그리지 않는 줄바꿈 기회가 되며, 줄이 거기서
         * 나뉘면 하이픈을 그린다. 없으면 null.
         */
        boolean[] shy;
        /**
         * In vertical text, the kind of every UTF-16 unit of the text ({@link VerticalText#SIDE}, {@link VerticalText#UP},
         * {@link VerticalText#COMBO}), or null if the whole text is sideways (always null in horizontal text). 세로쓰기에서 텍스트의
         * UTF-16 단위마다의 종류. 텍스트 전체가 눕혀지면 null(가로쓰기에서는 언제나 null).
         */
        byte[] kinds;
        /** Position of the item this segment comes from in the item list of the layout (P10-04-f). 이 세그먼트의 출처 항목이 배치의 항목 목록에서 갖는 위치(P10-04-f). */
        int itemIndex;
        /** For BREAK segments: true for a soft wrap point. BREAK 세그먼트에서: 부드러운 줄바꿈 지점이면 true. */
        boolean soft;
    }

    /** A measured piece that sits on a line. 줄 위에 놓이는 측정된 조각. */
    private static final class Piece {
        /** Segment it comes from. 출처 세그먼트. */
        Seg seg;
        /** Kind (TEXT, OPEN, CLOSE, ATOMIC). 종류. */
        InlineItem.Kind kind;
        /** Text range start in the segment. 세그먼트 안 텍스트 범위 시작. */
        int from;
        /** Text range end (exclusive) including hanging spaces. 매달린 공백을 포함한 텍스트 범위 끝. */
        int to;
        /** Width of the text without trailing spaces. 뒤 공백을 뺀 텍스트 너비. */
        double coreW;
        /** Width of the trailing spaces. 뒤 공백의 너비. */
        double spaceW;
        /** Total width on the line. 줄에서의 전체 너비. */
        double w;
        /** Atomic box or inline element. 원자적 상자 또는 인라인 요소. */
        LayoutBox box;
        /** Bidi level. 양방향 수준. */
        int level;
        /** Text of the piece as shown. 보이는 조각 텍스트. */
        String text;
        /** Source start offset in the Text node. Text 노드 안 원본 시작 위치. */
        int srcFrom;
        /** Source end offset in the Text node. Text 노드 안 원본 끝 위치. */
        int srcTo;
    }

    /** The horizontal extent of an inline element on a line. 줄 위 인라인 요소의 가로 범위. */
    private static final class Span {
        /** Left edge of the fragment. 조각의 왼쪽. */
        double start;
        /** Right edge of the fragment. 조각의 오른쪽. */
        double end;
        /** True if the start edge (margin, border, padding) is on this line. 시작 가장자리가 이 줄에 있으면 true. */
        boolean startEdge;
        /** True if the end edge is on this line. 끝 가장자리가 이 줄에 있으면 true. */
        boolean endEdge;
        /** True if the element has content or an edge on this line. 이 줄에 내용이나 가장자리가 있으면 true. */
        boolean used;
        /** True if the end is known. 끝이 정해졌으면 true. */
        boolean closed;
    }

    /** The state of one inline formatting run. 인라인 서식 한 번의 상태. */
    private final class Flow {
        /** The block container. 블록 컨테이너. */
        final LayoutBox b;
        /** Its style. 스타일. */
        final ComputedStyle bs;
        /** Content width. content 너비. */
        final double cw;
        /** Width used for percentages (NaN if infinite). 백분율 기준 너비(무한이면 NaN). */
        final double pctBase;
        /** True if no boxes are created. 상자를 만들지 않으면 true. */
        final boolean dry;
        /** Left position of the content box. content box 의 왼쪽 위치(x). */
        final double contentLeft;
        /** Top position of the content box. content box 의 위쪽 위치(y). */
        final double contentTop;
        /** Processed segments. 처리된 세그먼트. */
        final List<Seg> segs = new ArrayList<Seg>();
        /** Pieces of the current line. 현재 줄의 조각. */
        final List<Piece> line = new ArrayList<Piece>();
        /** The offsets in the text being collected that are soft hyphens. 모으는 텍스트에서 소프트 하이픈인 위치. */
        final List<Integer> shyAt = new ArrayList<Integer>();
        /** True while the text being collected is drawn in small capitals. 모으는 텍스트를 작은 대문자로 그리는 동안 true. */
        boolean smallNow;
        /** Pieces of the pending unbreakable unit. 대기 중인 쪼갤 수 없는 단위의 조각. */
        final List<Piece> unit = new ArrayList<Piece>();
        /** Width used on the current line. 현재 줄에서 쓴 너비. */
        double x;
        /** Width of the pending unit. 대기 중인 단위의 너비. */
        double unitW;
        /** Trailing space width of the pending unit. 대기 중인 단위의 뒤 공백 너비. */
        double unitTrail;
        /** True if the current line has visible content. 현재 줄에 보이는 내용이 있으면 true. */
        boolean lineContent;
        /** True when an explicit wbr permits wrapping before the pending unit, even if that unit is nowrap. 명시적 wbr가 다음 대기 단위 앞의 줄바꿈을 허용하면 true이며 단위 자체가 nowrap이어도 적용한다. */
        boolean explicitWrap;
        /** True if the container is laid out in a vertical writing mode (its text has orientations, P10-03-e). 컨테이너가 세로 쓰기 모드로 배치되면(텍스트에 방향이 있다) true(P10-03-e). */
        final boolean vert;
        /** True for <code>text-orientation: upright</code> in vertical text: the direction is ltr and all characters are strong ltr. 세로쓰기의 <code>text-orientation: upright</code> 이면 true: 방향은 ltr 이고 모든 글자는 강한 ltr 이다. */
        final boolean uprightBlock;
        /** True before the first line was finished. 첫 줄을 마치기 전이면 true. */
        boolean first = true;

        /** True while the line that is built is justified between characters (<code>text-justify: inter-character</code> or <code>distribute</code>). 만드는 줄이 글자 사이로 양쪽 정렬되는 중(<code>text-justify: inter-character</code> 나 <code>distribute</code>)이면 true. */
        boolean justifyByChar;

        /** The number of characters of the line that are still to be placed while it is justified between characters (the last one gets no space after it). 글자 사이 양쪽 정렬에서 아직 놓지 않은 글자 수(마지막 글자 뒤에는 간격을 두지 않는다). */
        int charsLeft;
        /** Height of the lines so far. 지금까지 줄의 높이. */
        double y;
        /** Width of the widest line. 가장 넓은 줄의 너비. */
        double widest;
        /** The number of lines so far (a line that has no visible content and is not forced is not counted). 지금까지의 줄 수(보이는 내용이 없고 강제도 아닌 줄은 세지 않는다). */
        int lineCount;
        /** Width of the last line that was finished (without trailing white space). 마지막으로 끝낸 줄의 너비(뒤쪽 공백 제외). */
        double lastLineWidth;
        /** The index of the line whose width is narrowed to {@link #shrinkTo} (<code>text-wrap: pretty</code>), -1 for none. 너비를 {@link #shrinkTo} 로 줄이는 줄의 번호(<code>text-wrap: pretty</code>). 없으면 -1. */
        int shrinkLine = -1;
        /** The width of the line {@link #shrinkLine}. {@link #shrinkLine} 줄의 너비. */
        double shrinkTo;
        /** Inline elements that are open at the start of the current line. 현재 줄 시작에 열려 있는 인라인 요소. */
        List<LayoutBox> startStack = new ArrayList<LayoutBox>();
        /** Text indent of the first line. 첫 줄의 들여쓰기. */
        final double indent;
        /** Cache of inline element metrics. 인라인 요소 메트릭 캐시. */
        final Map<LayoutBox, double[]> pm = new IdentityHashMap<LayoutBox, double[]>();
        /** Floats of the block formatting context (null in a measuring run). 블록 서식 문맥의 float(측정 실행에서는 null). */
        final FloatContext fc;
        /** x of the content box in the coordinates of the float context. float 문맥 좌표로 content box 의 x. */
        final double bfcX;
        /** y of the content box in the coordinates of the float context. float 문맥 좌표로 content box 의 y. */
        final double bfcY;
        /** Left edge of the current line relative to the content box (floats on the left push it right). 현재 줄의 content box 기준 왼쪽(왼쪽 float 가 오른쪽으로 민다). */
        double availL;
        /** Width of the current line (the content width minus the floats beside the line). 현재 줄의 너비(content 너비에서 줄 옆 float 를 뺀 값). */
        double availW;
        /** Height of the band at the top of a line that is checked against floats (the strut line height), NaN until needed. 줄 위쪽에서 float 와 비교하는 띠의 높이(strut 줄 높이). 필요할 때까지 NaN. */
        double strutH = Double.NaN;
        /** Floats that did not fit beside the current line and wait for the next line. 현재 줄 옆에 들어가지 않아 다음 줄을 기다리는 float. */
        final List<LayoutBox> pendingFloats = new ArrayList<LayoutBox>();
        /** The items that are laid out (the items of the container, or the items that <code>::first-line</code> made of them). 배치하는 항목(컨테이너의 항목이나 <code>::first-line</code> 이 그것으로 만든 항목). */
        final List<InlineItem> items;
        /** True for the probe of <code>::first-line</code>: a measuring run that reads the floats and records where the first line ends. <code>::first-line</code> 의 탐색이면 true: 상자를 만들지 않고 float 를 읽으며 첫 줄이 끝나는 곳을 기록하는 측정 실행. */
        final boolean probe;
        /** {index of the item, offset in its raw text (-1: the whole item)} where the first line ended, set by the probe, else null. 첫 줄이 끝난 {항목 위치, 원문 안의 위치(-1: 항목 전체)}. 탐색이 정하며 아니면 null. */
        int[] probeEnd;
        /** The style of the strut of the first line when <code>::first-line</code> applies, else null. <code>::first-line</code> 이 적용될 때 첫 줄의 strut 스타일. 아니면 null. */
        final ComputedStyle firstStrut;
        /** True if no soft wrap opportunity exists before and after an <code>img</code> (the table cell width quirk of quirks mode, Quirks Mode Standard 3.8). 이미지 앞뒤에 줄바꿈 기회가 없으면 true(호환 모드의 표 셀 너비 quirk, 호환 모드 표준 3.8). */
        boolean glueImages;
        /** Index of the segment that is being handled (read by the image gluing). 처리 중인 세그먼트의 위치(이미지 붙이기가 읽는다). */
        int segIndex;
        /** Width of the opening punctuation that hangs outside the start of the first line (<code>hanging-punctuation: first</code>), 0 for none. 첫 줄의 시작 밖에 매달리는 여는 문장 부호의 너비(<code>hanging-punctuation: first</code>). 없으면 0. */
        double hangFirst;
        /** True once the start of the first line has been examined for hanging punctuation. 첫 줄의 시작에서 매달리는 문장 부호를 이미 살펴보았으면 true. */
        boolean hangFirstDone;

        /**
         * Creates the state.
         * <p>
         * 상태를 만든다.
         *
         * @param b       the container / 컨테이너
         * @param items   the items to lay out / 배치할 항목
         * @param flProto the box of the first-line fragment or null / 첫 줄 조각의 상자 또는 null
         * @param cw      content width / content 너비
         * @param dry     true for measurement only / 측정만 하면 true
         * @param probe   true for the first-line probe / 첫 줄 탐색이면 true
         * @param left    x of the content box / content box 의 x
         * @param top     y of the content box / content box 의 y
         */
        Flow(LayoutBox b, List<InlineItem> items, LayoutBox flProto, double cw, boolean dry, boolean probe, double left, double top) {
            this.b = b;
            this.items = items;
            this.probe = probe;
            this.firstStrut = flProto == null ? null : bsOverlay(b);
            if (flProto != null) {
                // 첫 줄 조각은 가장자리(margin, border, padding)를 갖지 않는다: 원형의 가장자리를 계산하지 않고 0 으로 둔다.
                pm.put(flProto, null);
            }
            this.bs = b.style;
            this.vert = b.frame != Css.FRAME_H;
            this.uprightBlock = vert && VerticalText.orientationOf(bs, Css.kw(bs, Prop.WRITING_MODE)) == VerticalText.ORIENT_UPRIGHT;
            this.cw = cw;
            this.pctBase = Double.isInfinite(cw) ? Double.NaN : cw;
            this.dry = dry;
            this.contentLeft = left;
            this.contentTop = top;
            double ind = bs.resolve(Prop.TEXT_INDENT, pctBase);
            this.indent = Double.isNaN(ind) ? 0 : ind;
            this.x = indent;
            this.fc = dry && !probe ? null : bl.fc;
            this.bfcX = bl.fcx;
            this.bfcY = bl.fcy;
            this.availL = 0;
            this.availW = cw;
        }

        /**
         * Runs the whole formatting.
         * <p>
         * 서식 전체를 실행한다.
         */
        void run() {
            prepare();
            // 첫 줄의 너비도 앞선 float 에 따라 정해진다.
            refreshBounds();
            // 세그먼트를 차례로 줄에 놓는다.
            for (int i = 0; i < segs.size(); i++) {
                segIndex = i;
                handle(segs.get(i));
            }
            flushUnit();
            endLine(false, true);
            flushPendingFloats();
        }

        /**
         * Tells whether a segment is an <code>img</code> that the table cell width quirk keeps together with its neighbors (an inline-level
         * replaced <code>img</code> in a measuring run with {@link #glueImages}).
         * <p>
         * 세그먼트가 표 셀 너비 quirk 가 이웃과 붙여 두는 <code>img</code> 인지 알려 준다({@link #glueImages} 가 켜진 측정 실행의 인라인 수준 치환 <code>img</code>).
         *
         * @param s the segment / 세그먼트
         * @return true for a glued image / 붙여 두는 이미지이면 true
         */
        boolean isGluedImage(Seg s) {
            return glueImages && s.kind == InlineItem.Kind.ATOMIC && s.box != null && s.box.type == LayoutBox.Type.REPLACED
                    && s.box.node instanceof org.duckdns.hjow.swingwebpanel.dom.Element && "img".equals(BoxBuilder.lower(((org.duckdns.hjow.swingwebpanel.dom.Element) s.box.node).getLocalName()));
        }

        /**
         * Tells whether the content that follows the segment being handled starts with a glued image (the inline element edges between
         * them do not count as content).
         * <p>
         * 처리 중인 세그먼트 다음의 내용이 붙여 두는 이미지로 시작하는지 알려 준다(둘 사이의 인라인 요소 가장자리는 내용이 아니다).
         *
         * @return true if the next content is a glued image / 다음 내용이 붙여 두는 이미지이면 true
         */
        boolean nextIsGluedImage() {
            if (!glueImages) {
                return false;
            }
            // 인라인 요소의 시작/끝 가장자리를 건너뛰고 다음 내용을 찾는다.
            for (int i = segIndex + 1; i < segs.size(); i++) {
                Seg n = segs.get(i);
                if (n.kind == InlineItem.Kind.OPEN || n.kind == InlineItem.Kind.CLOSE) {
                    continue;
                }
                return isGluedImage(n);
            }
            return false;
        }

        // ------------------------------------------------------------------------------------------ floats

        /**
         * Recomputes the left edge and the width of the current line from the floats beside the top of the line.
         * <p>
         * 현재 줄의 왼쪽과 너비를 줄 위쪽 옆의 float 로 다시 계산한다.
         */
        void refreshBounds() {
            baseBounds();
            // 첫 줄의 시작에 매다는 여는 문장 부호는 줄의 왼쪽 가장자리 밖에 놓이므로 그만큼 왼쪽으로 옮기고 너비를 늘린다(CSS Text 3 hanging-punctuation: first).
            if (first && hangFirst > 0) {
                availL -= hangFirst;
                availW += hangFirst;
            }
        }

        /**
         * Computes the left edge and the width of the current line from the floats beside it, without hanging punctuation.
         * <p>
         * 현재 줄의 왼쪽과 너비를 옆의 float 로 계산한다(매다는 문장 부호는 고려하지 않는다).
         */
        void baseBounds() {
            if (fc == null || fc.size() == 0) {
                availL = 0;
                // text-wrap: pretty 에서 줄인 줄은 지정한 너비를 쓴다.
                availW = lineCount == shrinkLine ? Math.min(cw, shrinkTo) : cw;
                return;
            }
            if (Double.isNaN(strutH)) {
                strutH = Math.max(1, metricsOf(null)[2]);
            }
            double top = bfcY + y;
            // 줄은 float 의 모양(shape-outside)을 따라 감싼다.
            double l = fc.leftEdgeShaped(top, strutH, bfcX);
            double r = fc.rightEdgeShaped(top, strutH, bfcX + cw);
            availL = l - bfcX;
            availW = Math.max(0, r - l);
        }

        /**
         * Lays out a float and places it beside the lines (CSS 2.1 9.5.1).
         * <p>
         * float 를 배치하고 줄 옆에 놓는다(CSS 2.1 9.5.1).
         *
         * @param fb the float box / float 상자
         */
        void placeFloat(LayoutBox fb) {
            bl.layoutAtomic(fb, cw);
            placeLaidOut(fb, true);
        }

        /**
         * Places a float whose size is known. It goes as high as possible, but not above the current line, not above an earlier float
         * and (with <code>clear</code>) below the floats it clears. A float found in the middle of a line that has content goes to the
         * top of the line only if the line still holds that content beside it; otherwise it waits for the next line.
         * <p>
         * 크기를 아는 float 를 놓는다. 가능한 한 높이 놓이지만 현재 줄보다 위, 앞선 float 보다 위, (<code>clear</code> 가 있으면) 비워야 할 float 보다 위에는
         * 놓이지 않는다. 내용이 있는 줄 중간에서 만난 float 는 그 줄이 내용을 float 옆에 그대로 담을 수 있을 때만 줄 위쪽에 놓고, 아니면 다음 줄을 기다린다.
         *
         * @param fb      the float box with its size / 크기가 정해진 float 상자
         * @param midLine true if the float was found in the middle of the line (false: the line is empty) / 줄 중간에서 만났으면 true(false: 줄이 비어 있음)
         */
        void placeLaidOut(LayoutBox fb, boolean midLine) {
            boolean left = fb.floatSide == LayoutBox.FLOAT_LEFT;
            double ow = fb.marginLeft + fb.width + fb.marginRight;
            double oh = fb.marginTop + fb.height + fb.marginBottom;
            double minY = bfcY + y;
            int sides = BlockLayouter.clearSides(fb);
            if (sides != 0) {
                double clearBottom = fc.clearY(sides);
                if (clearBottom > minY) {
                    minY = clearBottom;
                }
            }
            double[] p = fc.place(left, ow, oh, minY, bfcX, bfcX + cw);
            int before = fc.size();
            fc.add(left, p[0], p[1], ow, oh, FloatShapes.areaOf(fb, p[0], p[1], ow, oh, cw));
            if (midLine && (lineContent || unitW > EPS)) {
                refreshBounds();
                // 줄에 이미 내용(또는 대기 중인 단위)이 있으면 현재 줄 위쪽에 놓이고 내용이 새 너비에 그대로 들어갈 때만 지금 놓는다.
                if (p[1] > bfcY + y + EPS || x + unitW - unitTrail > availW + EPS) {
                    fc.truncate(before);
                    refreshBounds();
                    pendingFloats.add(fb);
                    return;
                }
            }
            fb.lx = contentLeft + (p[0] - bfcX) + fb.marginLeft;
            fb.ly = contentTop + (p[1] - bfcY) + fb.marginTop;
            b.add(fb);
            refreshBounds();
        }

        /**
         * Places the floats that waited for a new line, at the top of that line.
         * <p>
         * 새 줄을 기다리던 float 를 그 줄 위쪽에 놓는다.
         */
        void flushPendingFloats() {
            // 대기 중인 float 를 순서대로 놓는다.
            while (!pendingFloats.isEmpty()) {
                LayoutBox fb = pendingFloats.remove(0);
                placeLaidOut(fb, false);
            }
        }

        // ------------------------------------------------------------------------------------------ preparation

        /**
         * Converts the inline items into segments: white space is collapsed, text is transformed, fonts are chosen.
         * <p>
         * 인라인 항목을 세그먼트로 바꾼다: 공백을 접고 텍스트를 변환하며 글꼴을 고른다.
         */
        void prepare() {
            boolean prevSpace = true;
            int column = 0;
            // 항목마다 종류에 따라 세그먼트를 만든다.
            for (int i = 0; i < items.size(); i++) {
                InlineItem it = items.get(i);
                it.index = i;
                switch (it.kind) {
                case OPEN:
                case CLOSE:
                    Seg o = new Seg();
                    o.kind = it.kind;
                    o.box = it.box;
                    o.edge = it.edge;
                    segs.add(o);
                    break;
                case ATOMIC:
                    Seg a = new Seg();
                    a.kind = InlineItem.Kind.ATOMIC;
                    a.box = it.box;
                    a.owner = it.owner;
                    a.itemIndex = it.index;
                    a.nowrap = isNoWrap(it.owner == null ? bs : it.owner.style);
                    segs.add(a);
                    prevSpace = false;
                    column++;
                    break;
                case BREAK:
                    Seg br = new Seg();
                    br.kind = InlineItem.Kind.BREAK;
                    br.soft = it.soft;
                    br.itemIndex = it.index;
                    br.style = it.style;
                    segs.add(br);
                    prevSpace = true;
                    column = 0;
                    break;
                case OPPORTUNITY:
                    Seg opportunity = new Seg();
                    opportunity.kind = InlineItem.Kind.OPPORTUNITY;
                    opportunity.nowrap = isNoWrap(it.style);
                    segs.add(opportunity);
                    break;
                case FLOAT:
                    // 첫 줄 탐색은 컨테이너 안의 float 를 배치하지 않는다.
                    if (probe) {
                        break;
                    }
                    // 측정 실행에서는 float 가 줄 안의 원자적 상자처럼 너비를 더하고 배치 실행에서는 줄 옆에 놓인다.
                    Seg fl = new Seg();
                    fl.kind = dry ? InlineItem.Kind.ATOMIC : InlineItem.Kind.FLOAT;
                    fl.box = it.box;
                    fl.owner = it.owner;
                    segs.add(fl);
                    if (dry) {
                        prevSpace = false;
                        column++;
                    }
                    break;
                case ABS:
                    // 절대 배치 상자는 배치 실행에서만 정적 위치를 기록한다.
                    if (!dry) {
                        Seg ab = new Seg();
                        ab.kind = InlineItem.Kind.ABS;
                        ab.box = it.box;
                        segs.add(ab);
                    }
                    break;
                default:
                    int[] state = text(it, prevSpace, column);
                    prevSpace = state[0] != 0;
                    column = state[1];
                    break;
                }
            }
        }

        /**
         * Tells whether a style forbids soft wrapping.
         * <p>
         * 스타일이 부드러운 줄바꿈을 금지하는지 알려 준다.
         *
         * @param s the style / 스타일
         * @return true for <code>nowrap</code> and <code>pre</code> / <code>nowrap</code>, <code>pre</code> 이면 true
         */
        boolean isNoWrap(ComputedStyle s) {
            String w = Css.whiteSpace(s);
            return "nowrap".equals(w) || "pre".equals(w);
        }

        /**
         * Processes the text of one item into text segments and forced breaks.
         * <p>
         * 항목 하나의 텍스트를 텍스트 세그먼트와 강제 줄바꿈으로 처리한다.
         *
         * @param it        the item / 항목
         * @param prevSpace true if the previous output ended with a collapsible space / 앞선 출력이 접히는 공백으로 끝났으면 true
         * @param column    current column for tab stops / 탭 정지를 위한 현재 열
         * @return array: whether the output ends with a collapsible space (1/0), and the new column / 배열: 접히는 공백으로 끝나는지(1/0), 새 열
         */
        int[] text(InlineItem it, boolean prevSpace, int column) {
            ComputedStyle s = it.style;
            String ws = Css.whiteSpace(s);
            boolean collapse = Css.collapsesSpaces(s);
            boolean keepNewlines = !("normal".equals(ws) || "nowrap".equals(ws) || ws.length() == 0);
            String raw = transform(it.text, Css.kw(s, Prop.TEXT_TRANSFORM));
            double tab = s.number(Prop.TAB_SIZE);
            int tabSize = Double.isNaN(tab) || tab < 1 ? 8 : (int) tab;
            StringBuilder out = new StringBuilder();
            List<Integer> src = new ArrayList<Integer>();
            boolean ps = prevSpace;
            int col = column;
            // 대문자 변형은 font-variant-caps 와 font-feature-settings 의 우선순위를 적용한 결과를 쓰고, font-synthesis 가 허용할 때만 합성한다.
            FontFeatures features = FontFeatures.of(s);
            String caps = features.caps();
            boolean allSmall = "all-small-caps".equals(caps) || "all-petite-caps".equals(caps);
            boolean smallCaps = (allSmall || "small-caps".equals(caps) || "petite-caps".equals(caps)) && FontFeatures.mayRealize(s, "small-caps");
            // 전각 형태(font-variant-east-asian: full-width)는 ASCII 글자를 전각 글자로 바꾼다.
            if (features.fullWidth()) {
                raw = fullWidth(raw);
            }
            smallNow = false;
            // 원문 문자를 하나씩 보며 공백을 접거나 보존한다.
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c == '\r') {
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == '\n') {
                        continue;
                    }
                    c = '\n';
                }
                if (c == '\n' && keepNewlines) {
                    emit(it, s, out, src);
                    smallNow = false;
                    Seg br = new Seg();
                    br.kind = InlineItem.Kind.BREAK;
                    segs.add(br);
                    ps = true;
                    col = 0;
                    continue;
                }
                // 작은 대문자는 소문자(all-small-caps 는 모든 글자)를 줄인 대문자로 그린다: 종류가 바뀌면 세그먼트를 나눈다(CSS Fonts 3 font-variant-caps).
                if (smallCaps && c != SOFT_HYPHEN) {
                    boolean small = smallCapsLetter(c, allSmall);
                    if (small != smallNow) {
                        emit(it, s, out, src);
                        smallNow = small;
                    }
                }
                if (c == SOFT_HYPHEN) {
                    // 소프트 하이픈은 hyphens: none 이면 무시하고, 아니면 아무것도 그리지 않는 줄바꿈 기회(폭 없는 공백)로 두며 위치를 기록한다.
                    if (!"none".equals(Css.kw(s, Prop.HYPHENS))) {
                        out.append(ZERO_WIDTH_SPACE);
                        src.add(Integer.valueOf(i));
                        shyAt.add(Integer.valueOf(out.length() - 1));
                        col++;
                        ps = false;
                    }
                    continue;
                }
                boolean space = c == ' ' || c == '\t' || c == '\n' || c == '\f';
                if (space && collapse) {
                    if (!ps) {
                        out.append(' ');
                        src.add(Integer.valueOf(i));
                        ps = true;
                        col++;
                    }
                    continue;
                }
                if (c == '\t') {
                    int n = tabSize - (col % tabSize);
                    // 탭은 다음 탭 정지까지의 공백으로 바꾼다.
                    for (int k = 0; k < n; k++) {
                        out.append(' ');
                        src.add(Integer.valueOf(i));
                    }
                    col += n;
                    ps = false;
                    continue;
                }
                out.append(smallNow ? Character.toUpperCase(c) : c);
                src.add(Integer.valueOf(i));
                ps = space ? false : false;
                col++;
            }
            emit(it, s, out, src);
            smallNow = false;
            return new int[] { ps ? 1 : 0, col };
        }

        /**
         * Replaces the printable ASCII characters by their full width forms (U+FF01 to U+FF5E; the space is kept).
         * <p>
         * 출력 가능한 ASCII 문자를 전각 형태(U+FF01 ~ U+FF5E)로 바꾼다(공백은 그대로 둔다).
         *
         * @param s the text / 텍스트
         * @return the text with full width forms / 전각 형태로 바꾼 텍스트
         */
        String fullWidth(String s) {
            StringBuilder o = new StringBuilder(s.length());
            // 글자마다 ASCII 범위이면 전각 문자로 옮긴다.
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                o.append(c > 0x20 && c < 0x7F ? (char) (c + 0xFEE0) : c);
            }
            return o.toString();
        }

        /**
         * Tells whether a character is drawn as a small capital: a lower case letter (with <code>all-small-caps</code> any cased letter) whose capital
         * is one character long.
         * <p>
         * 글자를 작은 대문자로 그리는지 알려 준다: 소문자(<code>all-small-caps</code> 는 대소문자가 있는 모든 글자)이며 대문자가 한 글자인 경우.
         *
         * @param c        the character / 문자
         * @param allSmall true for <code>all-small-caps</code> / <code>all-small-caps</code> 이면 true
         * @return true for a small capital / 작은 대문자이면 true
         */
        boolean smallCapsLetter(char c, boolean allSmall) {
            if (!(Character.isLowerCase(c) || allSmall && Character.isUpperCase(c))) {
                return false;
            }
            return String.valueOf(c).toUpperCase(Locale.ROOT).length() == 1;
        }

        /**
         * Emits the collected text as a text segment and clears the buffers.
         * <p>
         * 모은 텍스트를 텍스트 세그먼트로 내보내고 버퍼를 비운다.
         *
         * @param it  the item / 항목
         * @param s   its style / 스타일
         * @param out the text / 텍스트
         * @param src the source offsets / 원본 위치
         */
        void emit(InlineItem it, ComputedStyle s, StringBuilder out, List<Integer> src) {
            if (out.length() == 0) {
                return;
            }
            Seg g = new Seg();
            g.kind = InlineItem.Kind.TEXT;
            g.text = out.toString();
            g.src = new int[src.size()];
            // 원본 위치 목록을 배열로 옮긴다.
            for (int i = 0; i < g.src.length; i++) {
                g.src[i] = src.get(i).intValue() + it.srcOffset;
            }
            g.style = s;
            g.node = it.node;
            g.owner = it.owner;
            g.itemIndex = it.index;
            g.deco = it.decoration;
            // 소프트 하이픈이었던 위치를 세그먼트에 옮긴다.
            if (!shyAt.isEmpty()) {
                g.shy = new boolean[g.text.length()];
                // 기록한 위치마다 표시한다.
                for (int i = 0; i < shyAt.size(); i++) {
                    if (shyAt.get(i).intValue() < g.shy.length) {
                        g.shy[shyAt.get(i).intValue()] = true;
                    }
                }
                shyAt.clear();
            }
            long[] blocked = new long[1];
            g.font = FontResolver.resolve(s, g.text, blocked);
            g.invisible = blocked[0] > 0;
            if (smallNow) {
                // 작은 대문자의 글자는 글꼴 크기의 80 % 로 그린다.
                g.font = g.font.deriveFont((float) (g.font.getSize2D() * SMALL_CAPS_SCALE));
            }
            String position = FontFeatures.of(s).position();
            // 위첨자와 아래첨자는 글자를 줄이고 기준선에서 옮긴다(font-variant-position 의 합성).
            if (!"normal".equals(position) && FontFeatures.mayRealize(s, "position")) {
                double base = g.font.getSize2D();
                g.font = g.font.deriveFont((float) (base * POSITION_SCALE));
                g.baseShift = "super".equals(position) ? base / 3 : -base / 5;
            }
            double[] fm = FontResolver.metrics(g.font);
            g.asc = fm[0];
            g.desc = fm[1];
            // 강조점(text-emphasis)은 ruby 텍스트처럼 줄에 공간을 더한다: 위쪽 점은 상자 위에, 아래쪽 점은 아래에(줄 높이 계산에서 더한다).
            TextEmphasis emphasis = TextEmphasis.of(s);
            if (emphasis != null) {
                if (emphasis.isOver()) {
                    g.emUp = emphasis.reserve();
                } else {
                    g.emDown = emphasis.reserve();
                }
            }
            double ls = s.px(Prop.LETTER_SPACING);
            double wsp = s.px(Prop.WORD_SPACING);
            g.ls = Double.isNaN(ls) ? 0 : ls;
            g.ws = Double.isNaN(wsp) ? 0 : wsp;
            g.nowrap = isNoWrap(s);
            g.collapse = Css.collapsesSpaces(s);
            String wb = Css.kw(s, Prop.WORD_BREAK);
            String ow = Css.kw(s, Prop.OVERFLOW_WRAP);
            boolean anywhere = "anywhere".equals(Css.kw(s, Prop.LINE_BREAK));
            g.breakAll = "break-all".equals(wb) || anywhere;
            g.keepAll = "keep-all".equals(wb);
            g.lineBreak = Css.kw(s, Prop.LINE_BREAK);
            // wbr가 덩어리 앞의 줄바꿈을 허용해도 nowrap 덩어리 내부의 긴 단어를 나누지는 않는다.
            g.breakWord = !g.nowrap && (g.breakAll || "break-word".equals(wb) || "break-word".equals(ow) || "anywhere".equals(ow) || anywhere);
            // 세로쓰기에서는 글자마다 눕힘/똑바로/조합을 정한다(CSS Writing Modes 5.1, 9.1).
            if (vert) {
                g.kinds = VerticalText.classify(g.text, VerticalText.orientationOf(s, Css.kw(bs, Prop.WRITING_MODE)), VerticalText.combineOf(s));
            }
            segs.add(g);
            out.setLength(0);
            src.clear();
        }

        /**
         * Applies <code>text-transform</code>.
         * <p>
         * <code>text-transform</code> 을 적용한다.
         *
         * @param s the text / 텍스트
         * @param t the transform keyword / 변환 키워드
         * @return transformed text / 변환된 텍스트
         */
        String transform(String s, String t) {
            if ("uppercase".equals(t)) {
                return s.toUpperCase(Locale.ROOT);
            }
            if ("lowercase".equals(t)) {
                return s.toLowerCase(Locale.ROOT);
            }
            if ("capitalize".equals(t)) {
                StringBuilder o = new StringBuilder(s.length());
                boolean start = true;
                // 단어의 첫 글자만 대문자로 바꾼다.
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    o.append(start && Character.isLetter(c) ? Character.toUpperCase(c) : c);
                    start = Character.isWhitespace(c);
                }
                return o.toString();
            }
            return s;
        }

        // ------------------------------------------------------------------------------------------ breaking

        /**
         * Handles one segment: text and atomic boxes become pieces of the pending unit, a break ends the line.
         * <p>
         * 세그먼트 하나를 처리한다: 텍스트와 원자적 상자는 대기 단위의 조각이 되고 줄바꿈은 줄을 끝낸다.
         *
         * @param s the segment / 세그먼트
         */
        void handle(Seg s) {
            switch (s.kind) {
            case OPEN:
            case CLOSE:
                Piece e = new Piece();
                e.kind = s.kind;
                e.seg = s;
                e.box = s.box;
                protoEdges(s.box);
                e.w = s.edge ? (s.kind == InlineItem.Kind.OPEN ? s.box.marginLeft + s.box.borderLeft + s.box.paddingLeft
                        : s.box.paddingRight + s.box.borderRight + s.box.marginRight) : 0;
                unit.add(e);
                unitW += e.w;
                break;
            case ATOMIC:
                boolean glued = isGluedImage(s);
                // 붙여 두는 이미지 앞에서는 단위를 내보내지 않는다(줄바꿈 기회가 없다).
                if (!glued) {
                    flushUnit();
                }
                Piece a = new Piece();
                a.kind = InlineItem.Kind.ATOMIC;
                a.seg = s;
                a.box = s.box;
                if (dry) {
                    a.w = bl.atomicOuterWidth(s.box, cw);
                } else {
                    bl.layoutAtomic(s.box, cw);
                    a.w = s.box.marginLeft + s.box.width + s.box.marginRight;
                }
                unit.add(a);
                unitW += a.w;
                if (!s.nowrap && !glued) {
                    flushUnit();
                }
                break;
            case BREAK:
                flushUnit();
                // 부드러운 줄바꿈(::first-line 의 끝)은 줄바꿈된 줄처럼 끝나 양쪽 정렬될 수 있고, 강제 줄바꿈은 빈 줄도 만든다.
                endLine(!s.soft, false);
                explicitWrap = false;
                // clear 가 있는 강제 줄바꿈(br clear)은 다음 줄을 해당 쪽 float 아래로 내린다(CSS 2.1 9.5.2 의 인라인 clear 설명).
                if (!s.soft && s.style != null) {
                    int clearSides = BlockLayouter.clearSidesOf(s.style);
                    double clearBottom = clearSides == 0 ? Double.NEGATIVE_INFINITY : fc.clearY(clearSides);
                    if (clearBottom > bfcY + y) {
                        y = clearBottom - bfcY;
                        refreshBounds();
                    }
                }
                break;
            case OPPORTUNITY:
                // 명시적 기회 앞에서 단위를 나누어 다음 nowrap 단위도 필요한 경우에만 줄바꿈할 수 있게 한다.
                if (!s.nowrap) {
                    flushUnit();
                    explicitWrap = true;
                }
                break;
            case FLOAT:
                placeFloat(s.box);
                break;
            case ABS:
                // 정적 위치는 지금 줄의 현재 끝(부모 border box 기준)이다.
                s.box.staticX = contentLeft + availL + x + unitW;
                s.box.staticY = contentTop + y;
                b.add(s.box);
                break;
            default:
                addText(s);
                break;
            }
        }

        /**
         * Resolves the horizontal edges of an inline element once per run.
         * <p>
         * 인라인 요소의 가로 가장자리를 실행마다 한 번 푼다.
         *
         * @param p the inline element box / 인라인 요소 상자
         */
        void protoEdges(LayoutBox p) {
            if (pm.containsKey(p)) {
                return;
            }
            bl.edges(p, pctBase);
            pm.put(p, null);
        }

        /**
         * Splits a text segment at its break opportunities and adds the chunks to the pending unit; the unit is flushed after every
         * opportunity.
         * <p>
         * 텍스트 세그먼트를 줄바꿈 기회에서 나누어 조각을 대기 단위에 더한다. 기회마다 단위를 내보낸다.
         *
         * @param s the text segment / 텍스트 세그먼트
         */
        void addText(Seg s) {
            String t = s.text;
            int n = t.length();
            int[] bounds = boundaries(s);
            int start = 0;
            // 조합한 묶음 앞은 줄바꿈 기회이다(묶음은 실제 내용을 가진 보통의 인라인처럼 줄을 나눈다. CSS Writing Modes 9.1.2).
            if (!s.nowrap && s.kinds != null && n > 0 && s.kinds[0] == VerticalText.COMBO) {
                flushUnit();
            }
            // 줄바꿈 기회 사이의 덩어리(낱말과 뒤따르는 공백)를 차례로 조각으로 만든다.
            for (int k = 0; k < bounds.length; k++) {
                int end = bounds[k];
                if (end <= start) {
                    continue;
                }
                int coreEnd = end;
                // 덩어리 끝의 공백을 하나씩 뺀다(조합한 묶음 안의 공백은 묶음의 일부이다).
                while (coreEnd > start && t.charAt(coreEnd - 1) == ' ' && !(s.kinds != null && s.kinds[coreEnd - 1] == VerticalText.COMBO)) {
                    coreEnd--;
                }
                Piece p = new Piece();
                p.kind = InlineItem.Kind.TEXT;
                p.seg = s;
                p.from = start;
                p.to = end;
                measure(p, coreEnd);
                unit.add(p);
                unitW += p.w;
                unitTrail = p.spaceW;
                // 덩어리 뒤는 줄바꿈 기회이지만 바로 뒤에 붙여 두는 이미지가 오면 그 기회는 없다(호환 모드 표준 3.8).
                if (!s.nowrap && (end < n || coreEnd < end) && !(end >= n && nextIsGluedImage())) {
                    flushUnit();
                }
                start = end;
            }
            // 조합한 묶음 뒤도 줄바꿈 기회이다.
            if (!s.nowrap && s.kinds != null && n > 0 && s.kinds[n - 1] == VerticalText.COMBO) {
                flushUnit();
            }
        }

        /**
         * Measures a text piece: the width of the text without trailing spaces and the width of those spaces.
         * <p>
         * 텍스트 조각을 측정한다: 뒤 공백을 뺀 텍스트 너비와 그 공백의 너비.
         *
         * @param p       the piece (from and to are set) / 조각(from, to 가 정해져 있음)
         * @param coreEnd end of the text without trailing spaces / 뒤 공백을 뺀 텍스트의 끝
         */
        void measure(Piece p, int coreEnd) {
            Seg s = p.seg;
            p.coreW = w(s, p.from, coreEnd);
            p.spaceW = coreEnd < p.to ? w(s, coreEnd, p.to) : 0;
            p.w = p.coreW + p.spaceW;
            p.text = s.text.substring(p.from, p.to);
            p.srcFrom = srcOf(s, p.from, true);
            p.srcTo = srcOf(s, Math.max(p.from, p.to - 1), false);
        }

        /**
         * Returns the width along the line of a range of the text of a segment. Horizontal text and vertical text without upright
         * characters are measured as a string; in vertical text with upright or combined characters the range is split into runs of equal
         * kind (sideways runs are measured as strings, upright characters take one cell of one em each, a combined group one em).
         * <p>
         * 세그먼트 텍스트의 범위가 줄을 따라 차지하는 너비를 반환한다. 가로쓰기 텍스트와 똑바로 선 글자가 없는 세로쓰기 텍스트는 문자열로 잰다. 똑바로 서거나 조합한
         * 글자가 있는 세로쓰기 텍스트는 범위를 종류가 같은 구간으로 나눈다(눕힌 구간은 문자열로 재고 똑바로 선 글자는 글자마다 1em 한 칸, 조합 묶음은 1em).
         *
         * @param s the segment / 세그먼트
         * @param a start offset / 시작 위치
         * @param b end offset (exclusive) / 끝 위치(제외)
         * @return width in pixels / 픽셀 너비
         */
        double w(Seg s, int a, int b) {
            if (a >= b) {
                return 0;
            }
            if (s.kinds == null) {
                return tm.width(s.font, s.text.substring(a, b), s.ls, s.ws);
            }
            double total = 0;
            int i = a;
            // 종류가 같은 구간마다 너비를 더한다.
            while (i < b) {
                int j = runEnd(s, i, b);
                total += runWidth(s, i, j);
                i = j;
            }
            return total;
        }

        /**
         * Returns the end of the run of equal kind that starts at an offset of a segment (at most the limit).
         * <p>
         * 세그먼트의 한 위치에서 시작하는 같은 종류 구간의 끝을 반환한다(한계를 넘지 않는다).
         *
         * @param s     the segment / 세그먼트
         * @param i     start offset / 시작 위치
         * @param limit upper limit (exclusive) / 상한(제외)
         * @return end of the run (exclusive) / 구간의 끝(제외)
         */
        int runEnd(Seg s, int i, int limit) {
            byte k = s.kinds == null ? VerticalText.SIDE : s.kinds[i];
            int j = i + 1;
            // 종류가 바뀌거나 한계에 이를 때까지 구간을 늘린다.
            while (j < limit && (s.kinds == null ? VerticalText.SIDE : s.kinds[j]) == k) {
                j++;
            }
            return j;
        }

        /**
         * Returns the width along the line of one run of equal kind: a sideways run is measured as a string, an upright run takes one cell
         * (the font size plus the letter spacing) per character cluster, a combined group takes one em (letter spacing is ignored for it).
         * <p>
         * 종류가 같은 구간 하나가 줄을 따라 차지하는 너비를 반환한다: 눕힌 구간은 문자열로 재고, 똑바로 선 구간은 글자 클러스터마다 한 칸(글꼴 크기와 글자 간격),
         * 조합 묶음은 1em(글자 간격은 무시)이다.
         *
         * @param s the segment / 세그먼트
         * @param a start offset / 시작 위치
         * @param b end offset (exclusive) / 끝 위치(제외)
         * @return width in pixels / 픽셀 너비
         */
        double runWidth(Seg s, int a, int b) {
            byte k = s.kinds == null ? VerticalText.SIDE : s.kinds[a];
            if (k == VerticalText.COMBO) {
                return s.font.getSize2D();
            }
            if (k == VerticalText.UP) {
                return VerticalText.clusterCount(s.text, a, b) * (s.font.getSize2D() + s.ls);
            }
            return tm.width(s.font, s.text.substring(a, b), s.ls, s.ws);
        }

        /**
         * Tells whether an offset lies inside a combined group (a line must not break there).
         * <p>
         * 위치가 조합 묶음의 안쪽인지 알려 준다(거기서는 줄을 나눌 수 없다).
         *
         * @param s   the segment / 세그먼트
         * @param pos the offset between two characters / 두 글자 사이의 위치
         * @return true inside a group / 묶음 안이면 true
         */
        boolean insideCombo(Seg s, int pos) {
            return s.kinds != null && pos > 0 && pos < s.kinds.length && s.kinds[pos] == VerticalText.COMBO && s.kinds[pos - 1] == VerticalText.COMBO;
        }

        /**
         * Creates the boxes of a text piece and adds them to the line body: one box normally, and in vertical text one box for every run
         * of equal kind (sideways, upright or combined) of the piece. The runs of a right-to-left piece are placed in reverse order.
         * <p>
         * 텍스트 조각의 상자를 만들어 줄 본문에 더한다: 보통은 상자 하나이고, 세로쓰기에서는 조각 안의 같은 종류(눕힘, 똑바로, 조합) 구간마다 상자 하나이다.
         * 오른쪽에서 왼쪽 조각의 구간은 거꾸로 놓는다.
         *
         * @param p    the piece (<code>from</code> and <code>to</code> are the range in the segment text) / 조각(<code>from</code>, <code>to</code> 는 세그먼트 텍스트 안의 범위)
         * @param x    x of the piece on the line / 줄에서 조각의 x
         * @param body receives the boxes / 상자를 받는다
         */
        void addTextBoxes(Piece p, double x, List<LayoutBox> body) {
            Seg s = p.seg;
            if (s.kinds == null || p.to <= p.from) {
                body.add(textBox(p, x, p.w));
                return;
            }
            List<int[]> runs = new ArrayList<int[]>();
            int i = p.from;
            // 조각을 종류가 같은 구간으로 나눈다.
            while (i < p.to) {
                int j = runEnd(s, i, p.to);
                runs.add(new int[] { i, j });
                i = j;
            }
            if ((p.level & 1) == 1) {
                java.util.Collections.reverse(runs);
            }
            double cx = x;
            // 구간마다 상자를 만들어 이어서 놓는다.
            for (int k = 0; k < runs.size(); k++) {
                int[] r = runs.get(k);
                Piece q = new Piece();
                q.kind = InlineItem.Kind.TEXT;
                q.seg = s;
                q.level = p.level;
                q.from = r[0];
                q.to = r[1];
                q.text = s.text.substring(r[0], r[1]);
                q.w = runWidth(s, r[0], r[1]);
                q.srcFrom = srcOf(s, r[0], true);
                q.srcTo = srcOf(s, Math.max(r[0], r[1] - 1), false);
                LayoutBox t = textBox(q, cx, q.w);
                byte kind = s.kinds[r[0]];
                t.glyphMode = kind == VerticalText.UP ? LayoutBox.GLYPHS_UPRIGHT : (kind == VerticalText.COMBO ? LayoutBox.GLYPHS_COMBINED : LayoutBox.GLYPHS_SIDEWAYS);
                body.add(t);
                cx += q.w;
            }
        }

        /**
         * Returns the break opportunities of a segment as sorted offsets ending with the text length.
         * <p>
         * 세그먼트의 줄바꿈 기회를 텍스트 길이로 끝나는 정렬된 위치로 반환한다.
         *
         * @param s the text segment / 텍스트 세그먼트
         * @return offsets / 위치
         */
        int[] boundaries(Seg s) {
            String t = s.text;
            if (s.nowrap) {
                return new int[] { t.length() };
            }
            BreakIterator bi = s.breakAll ? BreakIterator.getCharacterInstance(Locale.ENGLISH) : BreakIterator.getLineInstance(Locale.ENGLISH);
            bi.setText(t);
            List<Integer> list = new ArrayList<Integer>();
            int pos = bi.next();
            // 줄바꿈 기회 위치를 모은다.
            while (pos != BreakIterator.DONE) {
                if (pos < t.length() && s.keepAll && t.charAt(pos - 1) != ' ' && isCjk(t.codePointAt(pos - 1)) && isCjk(t.codePointAt(pos))) {
                    pos = bi.next();
                    continue;
                }
                // 조합한 묶음 안에서는 줄을 나누지 않는다(묶음은 한 칸의 글자이다).
                if (insideCombo(s, pos)) {
                    pos = bi.next();
                    continue;
                }
                list.add(Integer.valueOf(pos));
                pos = bi.next();
            }
            // 한글 음절 사이는 줄바꿈 기회이다(UAX #14 는 H2/H3 사이에 금지 규칙이 없다). keep-all 이면 단어 안이므로 제외한다.
            if (!s.keepAll && !s.breakAll) {
                java.util.TreeSet<Integer> set = new java.util.TreeSet<Integer>(list);
                // 이웃한 두 글자가 모두 한글 음절이면 그 사이를 기회로 더한다.
                for (int i = 1; i < t.length(); i++) {
                    if (isHangulSyllable(t.charAt(i - 1)) && isHangulSyllable(t.charAt(i))) {
                        set.add(Integer.valueOf(i));
                    }
                }
                list = new ArrayList<Integer>(set);
            }
            // line-break 의 엄격도에 따라 중국어와 일본어 텍스트의 줄바꿈 기회를 더하거나 뺀다(CSS Text 3 5.3 절).
            if (!s.breakAll && s.lineBreak != null && s.lineBreak.length() > 0 && !"anywhere".equals(s.lineBreak)) {
                java.util.TreeSet<Integer> set = new java.util.TreeSet<Integer>(list);
                CjkLineBreak.adjust(t, set, s.lineBreak);
                list = new ArrayList<Integer>(set);
            }
            // 조합한 묶음과 똑바로 선 글자 사이도 줄바꿈 기회이다(묶음은 실제 내용을 가진 보통의 인라인이다).
            if (s.kinds != null && !s.nowrap) {
                java.util.TreeSet<Integer> set = new java.util.TreeSet<Integer>(list);
                // 묶음과 똑바로 선 글자가 맞닿은 자리를 찾는다.
                for (int i = 1; i < t.length(); i++) {
                    byte a = s.kinds[i - 1];
                    byte c = s.kinds[i];
                    if ((a == VerticalText.COMBO && c == VerticalText.UP) || (a == VerticalText.UP && c == VerticalText.COMBO)) {
                        set.add(Integer.valueOf(i));
                    }
                }
                list = new ArrayList<Integer>(set);
            }
            if (list.isEmpty() || list.get(list.size() - 1).intValue() != t.length()) {
                list.add(Integer.valueOf(t.length()));
            }
            int[] r = new int[list.size()];
            // 목록을 배열로 옮긴다.
            for (int i = 0; i < r.length; i++) {
                r[i] = list.get(i).intValue();
            }
            return r;
        }

        /**
         * Tells whether a character is a precomposed Hangul syllable (U+AC00 to U+D7A3).
         * <p>
         * 문자가 완성형 한글 음절(U+AC00~U+D7A3)인지 알려 준다.
         *
         * @param c the character / 문자
         * @return true for a Hangul syllable / 한글 음절이면 true
         */
        boolean isHangulSyllable(char c) {
            return c >= 0xAC00 && c <= 0xD7A3;
        }

        /**
         * Tells whether a code point is a Hangul, kana or ideograph letter.
         * <p>
         * 코드 포인트가 한글, 가나, 한자 글자인지 알려 준다.
         *
         * @param cp the code point / 코드 포인트
         * @return true for CJK letters / CJK 글자이면 true
         */
        boolean isCjk(int cp) {
            return (cp >= 0xAC00 && cp <= 0xD7AF) || (cp >= 0x1100 && cp <= 0x11FF) || (cp >= 0x3040 && cp <= 0x30FF)
                    || (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF);
        }

        /**
         * Places the pending unit: on the current line if it fits, else on a new line (an overlong unit that may be broken is split).
         * <p>
         * 대기 중인 단위를 놓는다: 맞으면 현재 줄에, 아니면 새 줄에(쪼갤 수 있는 긴 단위는 나눈다).
         */
        void flushUnit() {
            if (unit.isEmpty()) {
                return;
            }
            // 줄 맨 앞의 접히는 공백만 있는 텍스트 조각은 버린다.
            if (!lineContent) {
                dropLeadingSpaces();
            }
            // 첫 줄의 맨 앞 여는 문장 부호가 매달리면 줄의 왼쪽 가장자리를 그만큼 넓힌다.
            if (!lineContent && first && !hangFirstDone) {
                hangFirstDone = true;
                hangFirst = openingHang(unit);
                if (hangFirst > 0) {
                    refreshBounds();
                }
            }
            double fit = unitW - unitTrail - closingHang(unit);
            boolean wrapOk = consumeWrapBefore();
            // 소프트 하이픈으로 끝나는 단위가 줄의 끝이 되면 하이픈이 붙으므로 그 너비도 맞는지 본다.
            if (lineContent && wrapOk && x + fit + hyphenWidth(unit) > availW + EPS) {
                endLine(false, false);
                dropLeadingSpaces();
                fit = unitW - unitTrail;
            }
            // 빈 줄이 float 때문에 좁아져 단위가 들어가지 않으면 float 아래쪽으로 줄을 내린다(CSS 2.1 9.5).
            while (!lineContent && fc != null && availW < cw - EPS && x + fit > availW + EPS) {
                double nb = fc.nextBottom(bfcY + y, strutH);
                if (Double.isInfinite(nb)) {
                    break;
                }
                y = nb - bfcY;
                refreshBounds();
            }
            if (!lineContent && wrapOk && x + fit > availW + EPS && hasBreakable()) {
                placeSplit();
            } else {
                // 단위의 조각을 모두 현재 줄에 놓는다.
                for (int i = 0; i < unit.size(); i++) {
                    addToLine(unit.get(i));
                }
            }
            unit.clear();
            unitW = 0;
            unitTrail = 0;
        }

        /**
         * Tells whether the style of a text asks for a kind of hanging punctuation (the keywords of <code>hanging-punctuation</code>).
         * <p>
         * 텍스트의 스타일이 어떤 종류의 매다는 문장 부호를 요구하는지 알려 준다(<code>hanging-punctuation</code> 의 키워드).
         *
         * @param s       the style / 스타일
         * @param keyword <code>first</code>, <code>last</code>, <code>force-end</code> or <code>allow-end</code> / 키워드
         * @return true if the property has it / 속성에 있으면 true
         */
        boolean hangs(ComputedStyle s, String keyword) {
            org.duckdns.hjow.swingwebpanel.css.values.CssValue v = s.get(Prop.HANGING_PUNCTUATION);
            return v != null && (" " + v.toCssText() + " ").indexOf(" " + keyword + " ") >= 0;
        }

        /**
         * Returns the width of the opening punctuation at the start of a unit that hangs outside the first line (<code>hanging-punctuation: first</code>): an opening
         * bracket or quote (the categories Ps, Pi, Pf), the ASCII quotes and the ideographic space; 0 if there is none.
         * <p>
         * 단위의 시작에 있는 여는 문장 부호 중 첫 줄 밖에 매달리는 것의 너비를 반환한다(<code>hanging-punctuation: first</code>): 여는 괄호나 따옴표(Ps, Pi, Pf), ASCII 따옴표,
         * 표의 문자 공백. 없으면 0.
         *
         * @param pieces the pieces of the unit / 단위의 조각
         * @return the advance of the character / 글자의 advance
         */
        double openingHang(List<Piece> pieces) {
            // 단위에서 처음 나오는 텍스트 조각을 찾는다.
            for (int i = 0; i < pieces.size(); i++) {
                Piece p = pieces.get(i);
                if (p.kind != InlineItem.Kind.TEXT) {
                    if (p.kind == InlineItem.Kind.OPEN || p.kind == InlineItem.Kind.CLOSE) {
                        continue;
                    }
                    return 0;
                }
                if (p.text == null || p.text.length() == 0 || !hangs(p.seg.style, "first")) {
                    return 0;
                }
                char c = p.text.charAt(0);
                int t = Character.getType(c);
                boolean opening = t == Character.START_PUNCTUATION || t == Character.INITIAL_QUOTE_PUNCTUATION || t == Character.FINAL_QUOTE_PUNCTUATION
                        || c == '\'' || c == '"' || c == (char) 0x3000;
                return opening ? tm.advance(p.seg.font, String.valueOf(c)) + p.seg.ls : 0;
            }
            return 0;
        }

        /**
         * Returns the width of the punctuation at the end of a unit that may hang outside the end of the line: a stop or a comma with
         * <code>hanging-punctuation: force-end</code> or <code>allow-end</code>, and a closing bracket or quote (Pe, Pf, Pi and the ASCII quotes) with <code>last</code>
         * if the unit ends the text. A hanging character does not count when the unit is fitted into the line, so that the line may be as long as the box plus that
         * character. At most one character hangs.
         * <p>
         * 단위의 끝에 있는 문장 부호 중 줄 끝 밖에 매달릴 수 있는 것의 너비를 반환한다: <code>hanging-punctuation: force-end</code> 나 <code>allow-end</code> 의 마침표와 쉼표, 그리고
         * 단위가 텍스트의 끝이면 <code>last</code> 의 닫는 괄호와 따옴표(Pe, Pf, Pi, ASCII 따옴표). 매달리는 글자는 단위를 줄에 맞출 때 세지 않아 줄이 상자 너비에 그 글자를 더한 길이까지
         * 될 수 있다. 매달리는 글자는 하나뿐이다.
         *
         * @param pieces the pieces of the unit / 단위의 조각
         * @return the advance of the character, 0 for none / 글자의 advance. 없으면 0
         */
        double closingHang(List<Piece> pieces) {
            // 뒤에서부터 텍스트 조각을 찾는다(여닫는 조각은 건너뛴다).
            for (int i = pieces.size() - 1; i >= 0; i--) {
                Piece p = pieces.get(i);
                if (p.kind == InlineItem.Kind.OPEN || p.kind == InlineItem.Kind.CLOSE) {
                    continue;
                }
                if (p.kind != InlineItem.Kind.TEXT || p.text == null) {
                    return 0;
                }
                int n = trimmedLength(p.text);
                if (n == 0) {
                    return 0;
                }
                char c = p.text.charAt(n - 1);
                boolean stop = isStopOrComma(c) && (hangs(p.seg.style, "force-end") || hangs(p.seg.style, "allow-end"));
                int t = Character.getType(c);
                boolean closing = (t == Character.END_PUNCTUATION || t == Character.FINAL_QUOTE_PUNCTUATION || t == Character.INITIAL_QUOTE_PUNCTUATION
                        || c == '\'' || c == '"') && hangs(p.seg.style, "last") && endsText();
                return stop || closing ? tm.advance(p.seg.font, String.valueOf(c)) + p.seg.ls : 0;
            }
            return 0;
        }

        /**
         * Tells whether a character is a stop or a comma that may hang at the end of a line (CSS Text 3 hanging-punctuation).
         * <p>
         * 글자가 줄 끝에 매달릴 수 있는 마침표나 쉼표인지 알려 준다(CSS Text 3 hanging-punctuation).
         *
         * @param c the character / 글자
         * @return true for a stop or a comma / 마침표나 쉼표이면 true
         */
        boolean isStopOrComma(char c) {
            return ",.،۔、。，．﹐﹑﹒｡､".indexOf(c) >= 0;
        }

        /**
         * Tells whether the segment that is being handled is the last one that has content: nothing but closing tags follows it.
         * <p>
         * 처리 중인 세그먼트가 내용이 있는 마지막 것인지 알려 준다: 뒤에 닫는 태그밖에 없다.
         *
         * @return true if the text ends here / 텍스트가 여기서 끝나면 true
         */
        boolean endsText() {
            // 뒤따르는 세그먼트 중 닫는 태그가 아닌 것이 있는지 본다.
            for (int i = segIndex + 1; i < segs.size(); i++) {
                if (segs.get(i).kind != InlineItem.Kind.CLOSE) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Tells whether a text piece ends with a soft hyphen.
         * <p>
         * 텍스트 조각이 소프트 하이픈으로 끝나는지 알려 준다.
         *
         * @param p the piece / 조각
         * @return true if its last character was a soft hyphen / 마지막 글자가 소프트 하이픈이었으면 true
         */
        boolean endsWithSoftHyphen(Piece p) {
            return p.kind == InlineItem.Kind.TEXT && p.seg.shy != null && p.to > p.from && p.seg.shy[p.to - 1];
        }

        /**
         * Returns the width of the hyphen that is drawn if the line ends after the last text piece of a list of pieces, which is the case when that
         * piece ends with a soft hyphen; otherwise 0.
         * <p>
         * 조각 목록의 마지막 텍스트 조각 뒤에서 줄이 끝날 때 그려지는 하이픈의 너비를 반환한다. 그 조각이 소프트 하이픈으로 끝나는 경우이며, 아니면 0.
         *
         * @param pieces the pieces / 조각
         * @return the width / 너비
         */
        double hyphenWidth(List<Piece> pieces) {
            // 뒤에서부터 여는/닫는 조각을 건너뛰어 마지막 조각을 찾는다.
            for (int i = pieces.size() - 1; i >= 0; i--) {
                Piece p = pieces.get(i);
                if (p.kind == InlineItem.Kind.OPEN || p.kind == InlineItem.Kind.CLOSE) {
                    continue;
                }
                return endsWithSoftHyphen(p) ? tm.advance(p.seg.font, "-") + p.seg.ls : 0;
            }
            return 0;
        }

        /**
         * Draws the hyphen of a soft hyphen at the end of the current line: the zero width space that stands for it becomes a hyphen and the piece
         * grows by its width (a soft hyphen in the middle of a line stays invisible).
         * <p>
         * 현재 줄의 끝에 있는 소프트 하이픈의 하이픈을 그린다: 그것을 대신한 폭 없는 공백이 하이픈이 되고 조각이 그 너비만큼 커진다(줄 중간의 소프트 하이픈은 보이지
         * 않는다).
         */
        void hyphenateEnd() {
            // 줄의 끝에서부터 여는/닫는 조각을 건너뛰어 마지막 조각을 찾는다.
            for (int i = line.size() - 1; i >= 0; i--) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.OPEN || p.kind == InlineItem.Kind.CLOSE) {
                    continue;
                }
                if (endsWithSoftHyphen(p) && p.text.length() > 0 && p.text.charAt(p.text.length() - 1) == ZERO_WIDTH_SPACE) {
                    double hw = tm.advance(p.seg.font, "-") + p.seg.ls;
                    p.text = p.text.substring(0, p.text.length() - 1) + "-";
                    p.w += hw;
                    p.coreW += hw;
                    x += hw;
                }
                return;
            }
        }

        /**
         * Removes the collapsible spaces at the start of the pending unit (they never start a line): space-only text pieces before the
         * first piece that has content.
         * <p>
         * 대기 단위 맨 앞의 접히는 공백을 없앤다(줄은 공백으로 시작하지 않는다): 내용이 있는 첫 조각 앞의 공백만 있는 텍스트 조각.
         */
        void dropLeadingSpaces() {
            int i = 0;
            // 여는/닫는 조각은 건너뛰고 공백뿐인 텍스트 조각을 앞에서부터 지운다.
            while (i < unit.size()) {
                Piece p = unit.get(i);
                if (p.kind == InlineItem.Kind.OPEN || p.kind == InlineItem.Kind.CLOSE) {
                    i++;
                    continue;
                }
                if (p.kind == InlineItem.Kind.TEXT && p.coreW == 0 && p.to > p.from && p.seg.collapse && p.seg.text.charAt(p.from) == ' ') {
                    unitW -= p.w;
                    unit.remove(i);
                    continue;
                }
                break;
            }
        }

        /**
         * Resolves whether the line may be broken before the pending unit, consuming an explicit opportunity only for actual content.
         * <p>
         * 대기 단위 앞에서 줄을 나눌 수 있는지 판단하며 실제 내용이 있을 때만 명시적 기회를 소모한다.
         *
         * @return true if allowed / 허용되면 true
         */
        boolean consumeWrapBefore() {
            // 단위의 첫 텍스트나 원자적 조각이 줄바꿈을 금지하는지 본다.
            for (int i = 0; i < unit.size(); i++) {
                Piece p = unit.get(i);
                if (p.kind == InlineItem.Kind.TEXT || p.kind == InlineItem.Kind.ATOMIC) {
                    boolean allowed = explicitWrap || !p.seg.nowrap;
                    explicitWrap = false;
                    return allowed;
                }
            }
            return true;
        }

        /**
         * Tells whether the pending unit has a text piece that may be broken at any character.
         * <p>
         * 대기 단위에 아무 글자에서나 끊을 수 있는 텍스트 조각이 있는지 알려 준다.
         *
         * @return true if so / 있으면 true
         */
        boolean hasBreakable() {
            // 단위의 텍스트 조각 중 break-word 가 허용된 것이 있는지 본다.
            for (int i = 0; i < unit.size(); i++) {
                Piece p = unit.get(i);
                if (p.kind == InlineItem.Kind.TEXT && p.seg.breakWord && p.coreW > 0) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Places an overlong unit piece by piece, breaking text pieces that do not fit at character (cluster) boundaries.
         * <p>
         * 긴 단위를 조각별로 놓으며 맞지 않는 텍스트 조각은 글자(클러스터) 경계에서 끊는다.
         */
        void placeSplit() {
            List<Piece> work = new ArrayList<Piece>(unit);
            // 조각을 차례로 놓되 넘치는 텍스트 조각은 앞부분만 놓고 나머지를 다시 처리한다.
            for (int i = 0; i < work.size(); i++) {
                Piece p = work.get(i);
                if (p.kind != InlineItem.Kind.TEXT || !p.seg.breakWord || x + p.coreW <= availW + EPS) {
                    addToLine(p);
                    continue;
                }
                int cut = fitPrefix(p, availW - x, lineContent);
                if (cut <= 0) {
                    endLine(false, false);
                    work.add(i + 1, p);
                    continue;
                }
                Piece head = new Piece();
                head.kind = InlineItem.Kind.TEXT;
                head.seg = p.seg;
                head.from = p.from;
                head.to = p.from + cut;
                measure(head, head.to);
                Piece tail = new Piece();
                tail.kind = InlineItem.Kind.TEXT;
                tail.seg = p.seg;
                tail.from = p.from + cut;
                tail.to = p.to;
                int coreEnd = tail.to;
                // 꼬리 조각의 뒤 공백을 하나씩 뺀다.
                while (coreEnd > tail.from && p.seg.text.charAt(coreEnd - 1) == ' ') {
                    coreEnd--;
                }
                measure(tail, coreEnd);
                addToLine(head);
                endLine(false, false);
                work.add(i + 1, tail);
            }
        }

        /**
         * Returns the length of the longest prefix of a text piece that fits into a width; at least one cluster when the line is empty.
         * <p>
         * 텍스트 조각의 앞부분 중 주어진 너비에 들어가는 가장 긴 길이를 반환한다. 줄이 비어 있으면 최소 한 클러스터.
         *
         * @param p       the piece / 조각
         * @param room    available width / 남은 너비
         * @param hasText true if the line already has content (then 0 is allowed) / 줄에 이미 내용이 있으면 true(그러면 0 허용)
         * @return prefix length in chars / 글자 수로 센 앞부분 길이
         */
        int fitPrefix(Piece p, double room, boolean hasText) {
            String t = p.seg.text.substring(p.from, p.to);
            BreakIterator bi = BreakIterator.getCharacterInstance(Locale.ENGLISH);
            bi.setText(t);
            int best = 0;
            int pos = bi.next();
            // 클러스터 경계마다 앞부분 너비를 재어 들어가는 가장 긴 것을 찾는다.
            while (pos != BreakIterator.DONE) {
                // 조합한 묶음 안에서는 끊지 않는다.
                if (insideCombo(p.seg, p.from + pos)) {
                    pos = bi.next();
                    continue;
                }
                double w = w(p.seg, p.from, p.from + pos);
                if (w > room + EPS) {
                    break;
                }
                best = pos;
                pos = bi.next();
            }
            if (best == 0 && !hasText) {
                int first = bi.first();
                best = bi.next();
                if (best == BreakIterator.DONE || first == BreakIterator.DONE) {
                    best = t.length();
                }
            }
            return best;
        }

        /**
         * Adds a piece to the current line.
         * <p>
         * 조각을 현재 줄에 더한다.
         *
         * @param p the piece / 조각
         */
        void addToLine(Piece p) {
            line.add(p);
            x += p.w;
            if (p.kind == InlineItem.Kind.ATOMIC || (p.kind == InlineItem.Kind.TEXT && p.coreW > 0)
                    || (p.kind == InlineItem.Kind.TEXT && !p.seg.collapse && p.to > p.from)) {
                lineContent = true;
            }
        }

        // ------------------------------------------------------------------------------------------ lines

        /**
         * Finishes the current line (a forced line always makes a line, even an empty one).
         * <p>
         * 현재 줄을 마친다(강제 줄바꿈은 빈 줄이라도 줄을 만든다).
         *
         * @param forced true for a forced break / 강제 줄바꿈이면 true
         */
        void endLine(boolean forced, boolean last) {
            trimTrailing();
            if (!forced && !last) {
                hyphenateEnd();
            }
            boolean visible = false;
            // 줄에 보이는 내용(텍스트나 원자적 상자)이 있는지 본다.
            for (int i = 0; i < line.size(); i++) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.ATOMIC || p.kind == InlineItem.Kind.TEXT) {
                    visible = true;
                }
            }
            if (visible || forced) {
                lineCount++;
                lastLineWidth = x;
                widest = Math.max(widest, x);
                if (!dry) {
                    buildLine(forced || last);
                }
                // 첫 줄 탐색은 첫 줄이 끝나는 곳(마지막 텍스트나 원자적 조각의 끝)을 기록한다.
                if (probe && first && probeEnd == null && !last) {
                    int[] pe = firstLineEnd();
                    if (pe != null) {
                        probeEnd = new int[] { pe[0], pe[1], forced ? 1 : 0 };
                    }
                }
                first = false;
            }
            // 줄에 있던 여는/닫는 조각으로 다음 줄에 열려 있는 요소를 갱신한다.
            List<LayoutBox> stack = new ArrayList<LayoutBox>(startStack);
            for (int i = 0; i < line.size(); i++) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.OPEN) {
                    stack.add(p.box);
                } else if (p.kind == InlineItem.Kind.CLOSE) {
                    stack.remove(p.box);
                }
            }
            startStack = stack;
            line.clear();
            x = first ? indent : 0;
            lineContent = false;
            // 새 줄의 너비는 새 높이의 float 에 따라 달라지고, 기다리던 float 는 새 줄 위쪽에 놓인다.
            refreshBounds();
            if (!pendingFloats.isEmpty() && !last) {
                flushPendingFloats();
            }
        }

        /**
         * Returns where the current (first) line ends in terms of the items: the item of the last text or atomic piece of the line and,
         * for text, the offset after its last character in the raw text of the item. If the line is the last one of the content (the
         * caller passes last), the whole content is the first line and this is not asked.
         * <p>
         * 현재(첫) 줄이 항목 기준으로 어디서 끝나는지 반환한다: 줄의 마지막 텍스트나 원자적 조각의 항목, 텍스트이면 그 항목 원문에서 마지막 글자 뒤의 위치. 줄이 내용의 마지막
         * 줄이면(호출자가 last 를 넘긴다) 내용 전체가 첫 줄이며 이것을 묻지 않는다.
         *
         * @return {item index, offset or -1 for the whole item}, or null if the line has no text or atomic piece / {항목 위치, 위치 또는 항목 전체이면 -1}. 텍스트나 원자적 조각이 없으면 null
         */
        int[] firstLineEnd() {
            // 줄의 끝에서부터 마지막 텍스트나 원자적 조각을 찾는다.
            for (int i = line.size() - 1; i >= 0; i--) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.ATOMIC) {
                    return new int[] { p.seg.itemIndex, -1 };
                }
                if (p.kind == InlineItem.Kind.TEXT) {
                    InlineItem it = items.get(p.seg.itemIndex);
                    return new int[] { p.seg.itemIndex, p.srcTo - it.srcOffset };
                }
            }
            return null;
        }

        /**
         * Removes the collapsible trailing spaces of the current line (they hang and are not shown).
         * <p>
         * 현재 줄의 접히는 뒤 공백을 없앤다(매달려 보이지 않는다).
         */
        void trimTrailing() {
            // 줄 끝에서부터 닫는/여는 조각을 건너뛰고 마지막 텍스트 조각의 뒤 공백을 지운다.
            for (int i = line.size() - 1; i >= 0; i--) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.OPEN || p.kind == InlineItem.Kind.CLOSE) {
                    continue;
                }
                if (p.kind == InlineItem.Kind.TEXT && p.seg.collapse && p.spaceW > 0) {
                    x -= p.spaceW;
                    p.w = p.coreW;
                    p.spaceW = 0;
                    p.to = p.from + trimmedLength(p.text);
                    p.text = p.seg.text.substring(p.from, p.to);
                    p.srcTo = srcOf(p.seg, Math.max(p.from, p.to - 1), false);
                    if (p.coreW == 0) {
                        line.remove(i);
                        continue;
                    }
                }
                break;
            }
        }

        /**
         * Returns the length of a text without its trailing spaces.
         * <p>
         * 뒤 공백을 뺀 텍스트의 길이를 반환한다.
         *
         * @param t the text / 텍스트
         * @return length / 길이
         */
        int trimmedLength(String t) {
            int n = t.length();
            // 뒤쪽 공백을 하나씩 뺀다.
            while (n > 0 && t.charAt(n - 1) == ' ') {
                n--;
            }
            return n;
        }

        /**
         * Returns {ascent, descent, line height} of an inline element or, for null, of the block (the strut).
         * <p>
         * 인라인 요소(null 이면 블록의 strut)의 {ascent, descent, 줄 높이}를 반환한다.
         *
         * @param proto the inline element box or null / 인라인 요소 상자 또는 null
         * @return metrics / 메트릭
         */
        double[] metricsOf(LayoutBox proto) {
            // 첫 줄의 strut 은 ::first-line 의 글꼴과 줄 높이를 따른다(P10-04-f).
            ComputedStyle s = proto == null ? (first && firstStrut != null ? firstStrut : bs) : proto.style;
            Font f = FontResolver.resolve(s, null);
            double[] fm = FontResolver.metrics(f);
            double lh = s.lineHeightPx();
            if (Double.isNaN(lh) || lh < 0) {
                lh = fm[0] + fm[1] + fm[2];
            }
            return new double[] { fm[0], fm[1], lh };
        }

        /**
         * Computes the vertical metrics of an inline element relative to the baseline of the line:
         * {shift, top, bottom, alignment kind (0 baseline-relative, 1 top, 2 bottom)} (upwards is positive).
         * <p>
         * 인라인 요소의 줄 기준선에 대한 세로 메트릭을 계산한다: {이동, 위, 아래, 정렬 종류(0 기준선 상대, 1 top, 2 bottom)}(위쪽이 양수).
         *
         * @param proto the inline element box / 인라인 요소 상자
         * @param memo  cache of this line / 이 줄의 캐시
         * @return the metrics / 메트릭
         */
        double[] vm(LayoutBox proto, Map<LayoutBox, double[]> memo) {
            double[] r = memo.get(proto);
            if (r != null) {
                return r;
            }
            double[] parent = proto.inlineParent == null ? new double[] { 0, 0, 0, 0 } : vm(proto.inlineParent, memo);
            double[] pmet = metricsOf(proto.inlineParent);
            ComputedStyle ps = proto.inlineParent == null ? bs : proto.inlineParent.style;
            double[] m = metricsOf(proto);
            double hl = (m[2] - (m[0] + m[1])) / 2;
            double top = m[0] + hl;
            double bottom = -(m[1] + hl);
            double[] rel = alignment(proto.style, top, bottom, pmet[0], pmet[1], ps.fontSizePx(), m[2]);
            r = new double[] { parent[0] + rel[0], top, bottom, rel[1] };
            memo.put(proto, r);
            return r;
        }

        /**
         * Resolves <code>vertical-align</code> of a box: the shift relative to the parent baseline and the alignment kind.
         * <p>
         * 상자의 <code>vertical-align</code> 을 푼다: 부모 기준선에 대한 이동량과 정렬 종류.
         *
         * @param s       style of the box / 상자의 스타일
         * @param top     extent above the own baseline / 자기 기준선 위쪽 범위
         * @param bottom  extent below the own baseline (negative) / 자기 기준선 아래쪽 범위(음수)
         * @param pAsc    ascent of the parent / 부모 ascent
         * @param pDesc   descent of the parent / 부모 descent
         * @param pSize   font size of the parent / 부모 글꼴 크기
         * @param ownLh   line height of the box (base of percentages) / 상자의 줄 높이(백분율 기준)
         * @return array of shift and kind / 이동량과 종류 배열
         */
        double[] alignment(ComputedStyle s, double top, double bottom, double pAsc, double pDesc, double pSize, double ownLh) {
            String k = Css.kw(s, Prop.VERTICAL_ALIGN);
            if ("top".equals(k)) {
                return new double[] { 0, 1 };
            }
            if ("bottom".equals(k)) {
                return new double[] { 0, 2 };
            }
            if ("sub".equals(k)) {
                return new double[] { -(pSize / 5 + 1), 0 };
            }
            if ("super".equals(k)) {
                return new double[] { pSize / 3 + 1, 0 };
            }
            if ("text-top".equals(k)) {
                return new double[] { pAsc - top, 0 };
            }
            if ("text-bottom".equals(k)) {
                return new double[] { -pDesc - bottom, 0 };
            }
            if ("middle".equals(k)) {
                return new double[] { pSize * 0.25 - (top + bottom) / 2, 0 };
            }
            // 이전 HTML 의 align=middle/center 용 키워드: 상자의 세로 가운데를 부모의 기준선에 둔다(x 높이의 절반을 더하지 않는 middle).
            if ("-webkit-baseline-middle".equals(k)) {
                return new double[] { -(top + bottom) / 2, 0 };
            }
            if (k.length() == 0) {
                double v = s.resolve(Prop.VERTICAL_ALIGN, ownLh);
                return new double[] { Double.isNaN(v) ? 0 : v, 0 };
            }
            return new double[] { 0, 0 };
        }

        /**
         * Creates the line box and its fragments for the pieces of the current line and advances {@link #y}.
         * <p>
         * 현재 줄의 조각으로 줄 상자와 그 조각을 만들고 {@link #y} 를 진행한다.
         *
         * @param noJustify true if the line must not be justified (last line or before a forced break) / 양쪽 정렬을 하지 않을 줄(마지막 줄이거나 강제 줄바꿈 앞)이면 true
         */
        void buildLine(boolean noJustify) {
            // text-orientation: upright 는 방향을 ltr 로 강제하고 모든 글자를 강한 ltr 로 본다(CSS Writing Modes 5.1).
            boolean rtl = !uprightBlock && "rtl".equals(Css.kw(bs, Prop.DIRECTION));
            StringBuilder lt = new StringBuilder();
            // 줄의 텍스트를 이어 붙여 양방향 재배열이 필요한지 판단한다.
            for (int i = 0; i < line.size(); i++) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.TEXT) {
                    lt.append(p.text);
                }
            }
            boolean bidi = !uprightBlock && (rtl || (lt.length() > 0 && Bidi.requiresBidi(lt.toString().toCharArray(), 0, lt.length())));
            String ta = Css.kw(bs, Prop.TEXT_ALIGN);
            boolean startAlign = ta.length() == 0 || "start".equals(ta) || "match-parent".equals(ta) || "justify".equals(ta);
            boolean alignRight = "right".equals(ta) || (rtl && startAlign) || ("end".equals(ta) && !rtl);
            boolean center = "center".equals(ta);
            boolean justify = "justify".equals(ta) && !noJustify && !bidi && !"none".equals(Css.kw(bs, Prop.TEXT_JUSTIFY));
            double ind = first ? indent : 0;
            double lo = rtl ? 0 : ind;
            double hi = rtl ? availW - ind : availW;
            double lineW = x - ind;
            double room = hi - lo - lineW;
            double off = center ? lo + room / 2 : (alignRight ? lo + room : lo);
            if (room < 0 && !rtl) {
                off = lo;
            }
            double extraPerGap = 0;
            String tj = Css.kw(bs, Prop.TEXT_JUSTIFY);
            justifyByChar = justify && ("inter-character".equals(tj) || "distribute".equals(tj));
            charsLeft = 0;
            if (justify) {
                int gaps = 0;
                int cjk = 0;
                // 양쪽 정렬은 줄 안의 공백 수(글자 사이 정렬이면 글자 사이의 수)로 남는 너비를 나눈다. 공백, 글자(클러스터), 한중일 글자 수를 함께 센다.
                for (int i = 0; i < line.size(); i++) {
                    Piece p = line.get(i);
                    if (p.kind == InlineItem.Kind.TEXT) {
                        // 글자(클러스터)를 센다. 마지막 글자 뒤에는 간격이 없으므로 나중에 하나를 뺀다.
                        for (int k = 0; k < p.text.length(); k = nextCluster(p.text, k)) {
                            charsLeft++;
                            if (isCjk(p.text.codePointAt(k))) {
                                cjk++;
                            }
                        }
                        // 텍스트의 공백 문자를 센다.
                        for (int k = 0; k < p.text.length(); k++) {
                            if (p.text.charAt(k) == ' ') {
                                gaps++;
                            }
                        }
                    }
                }
                // auto 는 공백이 없는 줄에서 한중일 글자가 둘 이상이면 글자 사이를 늘인다(CSS Text 3 6.4.3 의 예: 낱말 구분자와 한중일 글자 사이를 늘린다).
                if (!justifyByChar && gaps == 0 && cjk >= 2 && (tj.length() == 0 || "auto".equals(tj))) {
                    justifyByChar = true;
                }
                if (justifyByChar) {
                    gaps = Math.max(0, charsLeft - 1);
                }
                if (gaps > 0 && room > EPS) {
                    extraPerGap = room / gaps;
                    off = lo;
                } else {
                    justify = false;
                }
            }
            List<Piece> content = new ArrayList<Piece>();
            Map<LayoutBox, Span> spans = new LinkedHashMap<LayoutBox, Span>();
            // 이 줄 시작에 열려 있는 요소는 줄 처음부터 이어진다.
            for (int i = 0; i < startStack.size(); i++) {
                Span sp = new Span();
                sp.used = false;
                spans.put(startStack.get(i), sp);
            }
            List<LayoutBox> frags = new ArrayList<LayoutBox>();
            if (!bidi) {
                placeLtr(off, extraPerGap, justify, content, spans, frags);
            } else {
                placeBidi(rtl, lo, hi, center, alignRight, content, spans, frags);
            }
            // 세로 메트릭: strut 와 인라인 요소, 원자적 상자의 범위로 줄 높이와 기준선을 구한다.
            Map<LayoutBox, double[]> memo = new IdentityHashMap<LayoutBox, double[]>();
            double[] strut = metricsOf(null);
            double hlS = (strut[2] - (strut[0] + strut[1])) / 2;
            // 호환 모드의 줄 높이 quirk(호환 모드 표준 3.3, 3.4): 블록 자신의 텍스트가 없는 줄에는 strut 가 없고, 텍스트가 없는 인라인 상자는 줄 높이 0 으로 본다.
            boolean quirks = bl.lineQuirks && !vert;
            boolean useStrut = !quirks || content.isEmpty();
            // 블록 바로 안의 텍스트(둘러싼 인라인 요소가 없는 텍스트)가 하나라도 있으면 strut 를 쓴다.
            for (int i = 0; quirks && !useStrut && i < content.size(); i++) {
                if (content.get(i).kind == InlineItem.Kind.TEXT && content.get(i).seg.owner == null) {
                    useStrut = true;
                }
            }
            double maxTop = useStrut ? strut[0] + hlS : Double.NEGATIVE_INFINITY;
            double minBottom = useStrut ? -(strut[1] + hlS) : Double.POSITIVE_INFINITY;
            Map<LayoutBox, Boolean> hasText = new IdentityHashMap<LayoutBox, Boolean>();
            List<Double> topAligned = new ArrayList<Double>();
            List<Double> bottomAligned = new ArrayList<Double>();
            Map<LayoutBox, Boolean> seen = new IdentityHashMap<LayoutBox, Boolean>();
            Map<LayoutBox, double[]> atomicVm = new IdentityHashMap<LayoutBox, double[]>();
            // 내용 조각마다 둘러싼 인라인 요소와 원자적 상자의 범위를 더한다.
            for (int i = 0; i < content.size(); i++) {
                Piece p = content.get(i);
                LayoutBox owner = p.seg.owner;
                // 둘러싼 인라인 요소를 안쪽에서 바깥쪽으로 훑는다.
                for (LayoutBox pr = owner; pr != null; pr = pr.inlineParent) {
                    if (seen.containsKey(pr)) {
                        break;
                    }
                    seen.put(pr, Boolean.TRUE);
                    double[] v = vm(pr, memo);
                    if (v[3] == 0) {
                        // 텍스트가 없고 좌우 가장자리가 0 인 인라인 상자는 줄 높이 0 으로 본다(호환 모드 표준 3.3).
                        if (!(quirks && withoutText(pr, content, hasText))) {
                            maxTop = Math.max(maxTop, v[0] + v[1]);
                            minBottom = Math.min(minBottom, v[0] + v[2]);
                        }
                    } else if (v[3] == 1) {
                        topAligned.add(Double.valueOf(v[1] - v[2]));
                    } else {
                        bottomAligned.add(Double.valueOf(v[1] - v[2]));
                    }
                }
                if (p.kind == InlineItem.Kind.ATOMIC) {
                    LayoutBox ab = p.box;
                    boolean noBase = Double.isNaN(ab.baseline);
                    double above = noBase ? ab.marginTop + ab.height + ab.marginBottom : ab.marginTop + ab.baseline;
                    double below = noBase ? 0 : ab.height - ab.baseline + ab.marginBottom;
                    double[] pv = owner == null ? new double[] { 0, 0, 0, 0 } : vm(owner, memo);
                    double[] pmet = metricsOf(owner);
                    ComputedStyle ps = owner == null ? bs : owner.style;
                    double[] rel = alignment(ab.style, above, -below, pmet[0], pmet[1], ps.fontSizePx(), ab.height);
                    double[] av = new double[] { pv[0] + rel[0], above, -below, rel[1] };
                    atomicVm.put(ab, av);
                    if (av[3] == 0) {
                        maxTop = Math.max(maxTop, av[0] + av[1]);
                        minBottom = Math.min(minBottom, av[0] + av[2]);
                    } else if (av[3] == 1) {
                        topAligned.add(Double.valueOf(above + below));
                    } else {
                        bottomAligned.add(Double.valueOf(above + below));
                    }
                }
                // 강조점이 있는 텍스트는 ruby 텍스트처럼 자신의 상자 위나 아래에 점의 공간을 더한다(기준선 정렬 상자일 때).
                if (p.kind == InlineItem.Kind.TEXT && (p.seg.emUp > 0 || p.seg.emDown > 0)) {
                    double[] ov = owner == null ? null : vm(owner, memo);
                    if (ov == null || ov[3] == 0) {
                        double shift = ov == null ? 0 : ov[0];
                        double boxTop = ov == null ? strut[0] + hlS : ov[1];
                        double boxBottom = ov == null ? -(strut[1] + hlS) : ov[2];
                        maxTop = Math.max(maxTop, shift + boxTop + p.seg.emUp);
                        minBottom = Math.min(minBottom, shift + boxBottom - p.seg.emDown);
                    }
                }
            }
            // 아무것도 줄 높이에 기여하지 않았으면(예: 텍스트 없는 인라인 요소뿐인 줄) strut 로 돌아간다.
            if (maxTop == Double.NEGATIVE_INFINITY) {
                maxTop = strut[0] + hlS;
                minBottom = -(strut[1] + hlS);
            }
            double span = maxTop - minBottom;
            // top 정렬 상자가 줄보다 크면 아래로, bottom 정렬 상자가 크면 위로 줄을 키운다.
            for (int i = 0; i < topAligned.size(); i++) {
                double h = topAligned.get(i).doubleValue();
                if (h > span) {
                    minBottom -= h - span;
                    span = h;
                }
            }
            // bottom 정렬 상자의 높이를 하나씩 확인한다.
            for (int i = 0; i < bottomAligned.size(); i++) {
                double h = bottomAligned.get(i).doubleValue();
                if (h > span) {
                    maxTop += h - span;
                    span = h;
                }
            }
            LayoutBox lb = new LayoutBox(LayoutBox.Type.LINE, null, bs);
            lb.lx = contentLeft + availL;
            lb.ly = contentTop + y;
            lb.width = availW;
            lb.height = span;
            lb.baseline = maxTop;
            // 조각마다 세로 위치를 정하고 줄에 붙인다.
            for (int i = 0; i < frags.size(); i++) {
                LayoutBox f = frags.get(i);
                Object tag = fragTag.get(f);
                if (f.type == LayoutBox.Type.TEXT) {
                    Piece p = (Piece) tag;
                    double shift = p.seg.owner == null ? 0 : finalShift(p.seg.owner, memo, maxTop, minBottom);
                    f.ly = maxTop - shift - p.seg.baseShift - f.ascent;
                    f.baseline = f.ascent;
                } else if (f.type == LayoutBox.Type.INLINE) {
                    LayoutBox proto = (LayoutBox) tag;
                    double[] m = metricsOf(proto);
                    double shift = finalShift(proto, memo, maxTop, minBottom);
                    f.ly = maxTop - shift - m[0] - f.borderTop - f.paddingTop;
                    f.height = m[0] + m[1] + f.borderTop + f.borderBottom + f.paddingTop + f.paddingBottom;
                    f.baseline = f.borderTop + f.paddingTop + m[0];
                } else {
                    double[] av = atomicVm.get(f);
                    if (av[3] == 1) {
                        f.ly = f.marginTop;
                    } else if (av[3] == 2) {
                        f.ly = span - f.height - f.marginBottom;
                    } else {
                        f.ly = maxTop - av[0] - av[1] + f.marginTop;
                    }
                }
                lb.add(f);
            }
            fragTag.clear();
            b.add(lb);
            y += span;
        }

        /**
         * Tells whether an inline element has no text on a line and no horizontal padding or border (the inline boxes that the line height
         * quirk of quirks mode treats as if their line height were zero, Quirks Mode Standard 3.3).
         * <p>
         * 인라인 요소가 줄에 텍스트가 없고 가로 padding 과 border 도 없는지 알려 준다(호환 모드의 줄 높이 quirk 가 줄 높이 0 으로 보는 인라인 상자, 호환 모드 표준 3.3).
         *
         * @param element the inline element box / 인라인 요소 상자
         * @param content the content pieces of the line / 줄의 내용 조각
         * @param memo    cache of the answers of this line / 이 줄의 답 캐시
         * @return true if it has no text and no horizontal edges / 텍스트와 가로 가장자리가 없으면 true
         */
        boolean withoutText(LayoutBox element, List<Piece> content, Map<LayoutBox, Boolean> memo) {
            Boolean known = memo.get(element);
            if (known != null) {
                return known.booleanValue();
            }
            boolean empty = element.paddingLeft == 0 && element.paddingRight == 0 && element.borderLeft == 0 && element.borderRight == 0;
            // 줄의 텍스트 조각마다 이 요소가 안쪽에서 바깥쪽으로 둘러싸는지 확인한다.
            for (int i = 0; empty && i < content.size(); i++) {
                Piece p = content.get(i);
                if (p.kind != InlineItem.Kind.TEXT) {
                    continue;
                }
                // 조각을 둘러싼 인라인 요소를 바깥으로 올라가며 찾는다.
                for (LayoutBox o = p.seg.owner; o != null; o = o.inlineParent) {
                    if (o == element) {
                        empty = false;
                        break;
                    }
                }
            }
            memo.put(element, Boolean.valueOf(empty));
            return empty;
        }

        /**
         * Returns the final vertical shift of an inline element, resolving <code>top</code> and <code>bottom</code> alignment against the
         * finished line.
         * <p>
         * 인라인 요소의 최종 세로 이동량을 반환한다. <code>top</code>, <code>bottom</code> 정렬은 완성된 줄에 맞춰 푼다.
         *
         * @param proto     the inline element box / 인라인 요소 상자
         * @param memo      metrics of this line / 이 줄의 메트릭
         * @param maxTop    distance of the baseline from the line top / 줄 위쪽에서 기준선까지 거리
         * @param minBottom distance of the line bottom below the baseline (negative) / 기준선 아래 줄 아래쪽까지 거리(음수)
         * @return the shift / 이동량
         */
        double finalShift(LayoutBox proto, Map<LayoutBox, double[]> memo, double maxTop, double minBottom) {
            double[] v = vm(proto, memo);
            if (v[3] == 1) {
                return maxTop - v[1];
            }
            if (v[3] == 2) {
                return minBottom - v[2];
            }
            return v[0];
        }

        /** Maps fragments to what they came from while a line is built. 줄을 만드는 동안 조각을 출처에 대응시킨다. */
        final Map<LayoutBox, Object> fragTag = new IdentityHashMap<LayoutBox, Object>();

        /**
         * Positions the pieces of a left-to-right line and creates the fragment boxes (without their vertical position).
         * <p>
         * 왼쪽에서 오른쪽 줄의 조각을 배치하고 조각 상자를 만든다(세로 위치는 제외).
         *
         * @param off     x of the line start / 줄 시작 x
         * @param extra   extra width per space for justification / 양쪽 정렬에서 공백마다 더하는 너비
         * @param justify true if justified / 양쪽 정렬이면 true
         * @param content receives the content pieces / 내용 조각을 받는다
         * @param spans   receives the extents of inline elements / 인라인 요소의 범위를 받는다
         * @param out     receives the fragment boxes in paint order / 그리기 순서의 조각 상자를 받는다
         * @return x of the end of the content / 내용 끝의 x
         */
        double placeLtr(double off, double extra, boolean justify, List<Piece> content, Map<LayoutBox, Span> spans, List<LayoutBox> out) {
            double cur = off;
            List<LayoutBox> body = new ArrayList<LayoutBox>();
            // 이어지는 요소의 시작은 줄 처음이다. box-decoration-break: clone 이면 조각마다 시작 가장자리(border, padding)를 다시 가진다.
            for (Map.Entry<LayoutBox, Span> e : spans.entrySet()) {
                Span cs = e.getValue();
                cs.start = cur;
                if (clonesEdges(e.getKey())) {
                    cs.startEdge = true;
                    cs.used = true;
                    cur += e.getKey().borderLeft + e.getKey().paddingLeft;
                }
            }
            // 줄의 조각을 논리 순서로 훑으며 x 를 정한다.
            for (int i = 0; i < line.size(); i++) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.OPEN) {
                    Span sp = new Span();
                    sp.used = p.seg.edge;
                    if (p.seg.edge) {
                        cur += p.box.marginLeft;
                        sp.startEdge = true;
                    }
                    sp.start = cur;
                    cur += p.seg.edge ? p.box.borderLeft + p.box.paddingLeft : 0;
                    spans.put(p.box, sp);
                } else if (p.kind == InlineItem.Kind.CLOSE) {
                    Span sp = spans.get(p.box);
                    if (sp != null) {
                        cur += p.seg.edge ? p.box.paddingRight + p.box.borderRight : 0;
                        sp.end = cur;
                        sp.closed = true;
                        sp.endEdge = p.seg.edge;
                        if (p.seg.edge) {
                            sp.used = true;
                        }
                        cur += p.seg.edge ? p.box.marginRight : 0;
                    }
                } else if (p.kind == InlineItem.Kind.ATOMIC) {
                    content.add(p);
                    markUsed(p.seg.owner, spans);
                    cur += p.box.marginLeft;
                    p.box.lx = cur;
                    p.box.inlineLevel = true;
                    p.box.inlineOwner = p.seg.owner;
                    cur += p.box.width + p.box.marginRight;
                    body.add(p.box);
                    fragTag.put(p.box, p);
                } else {
                    content.add(p);
                    markUsed(p.seg.owner, spans);
                    if (justify && justifyByChar) {
                        cur = placeChars(p, cur, extra, body);
                    } else if (justify) {
                        cur = placeWords(p, cur, extra, body);
                    } else {
                        addTextBoxes(p, cur, body);
                        cur += p.w;
                    }
                }
            }
            List<Map.Entry<LayoutBox, Span>> open = new ArrayList<Map.Entry<LayoutBox, Span>>(spans.entrySet());
            // box-decoration-break: clone 이면 줄 끝에서 아직 열린 요소도 끝 가장자리를 가진다(안쪽 요소부터 닫아 바깥 요소의 끝이 안쪽 가장자리를 포함하게 한다).
            for (int i = open.size() - 1; i >= 0; i--) {
                Span os = open.get(i).getValue();
                LayoutBox proto = open.get(i).getKey();
                if (!os.closed && clonesEdges(proto)) {
                    cur += proto.paddingRight + proto.borderRight;
                    os.end = cur;
                    os.closed = true;
                    os.endEdge = true;
                    os.used = true;
                }
            }
            closeSpans(spans, cur, out);
            out.addAll(body);
            return cur;
        }

        /**
         * Tells whether the fragments of an inline element each get the edges of both ends (<code>box-decoration-break: clone</code>, CSS Fragmentation Module
         * Level 3 section 5.3, https://www.w3.org/TR/css-break-3/#break-decoration): the element has a border or padding at an end and its style asks for clone.
         * Lines that are placed from right to left do not support it. The line breaking does not reserve the width of the cloned edges, so a line can be wider
         * than its box by that width.
         * <p>
         * 인라인 요소의 조각이 양쪽 끝의 가장자리를 각각 가지는지 알려 준다(<code>box-decoration-break: clone</code>, CSS Fragmentation Module Level 3 5.3 절): 요소가 끝에
         * border 나 padding 을 가지고 스타일이 clone 을 요구한다. 오른쪽에서 왼쪽으로 놓는 줄은 지원하지 않는다. 줄바꿈은 복제한 가장자리의 너비를 따로 확보하지 않으므로 줄이
         * 그 너비만큼 상자보다 넓어질 수 있다.
         *
         * @param proto the inline element / 인라인 요소
         * @return true for clone with edges / 가장자리가 있는 clone 이면 true
         */
        boolean clonesEdges(LayoutBox proto) {
            return proto.style != null && "clone".equals(Css.kw(proto.style, Prop.BOX_DECORATION_BREAK))
                    && (proto.borderLeft + proto.paddingLeft + proto.borderRight + proto.paddingRight > 0);
        }

        /**
         * Places the characters of a text piece separately and puts the extra width after every character but the last one of the line (justification between
         * characters, <code>text-justify: inter-character</code>). A character is a cluster of a base and its combining marks, a surrogate pair or a joined sequence.
         * <p>
         * 텍스트 조각의 글자를 따로 놓고 줄의 마지막 글자만 빼고 모든 글자 뒤에 추가 너비를 둔다(글자 사이 양쪽 정렬, <code>text-justify: inter-character</code>).
         * 글자는 기본 글자와 결합 부호의 묶음, 서로게이트 쌍, 이어 붙인 열이다.
         *
         * @param p     the piece / 조각
         * @param start x of the piece / 조각의 x
         * @param extra extra width after a character / 글자 뒤에 더하는 너비
         * @param body  receives the fragments / 조각을 받는다
         * @return x after the piece / 조각 뒤의 x
         */
        double placeChars(Piece p, double start, double extra, List<LayoutBox> body) {
            double cur = start;
            String t = p.text;
            int i = 0;
            // 글자(클러스터)를 차례로 놓는다. 줄의 마지막 글자가 아니면 뒤에 간격을 더한다.
            while (i < t.length()) {
                int j = nextCluster(t, i);
                double w = w(p.seg, p.from + i, p.from + j);
                charsLeft--;
                if (t.charAt(i) != ' ') {
                    Piece q = new Piece();
                    q.kind = InlineItem.Kind.TEXT;
                    q.seg = p.seg;
                    q.text = t.substring(i, j);
                    q.from = p.from + i;
                    q.to = p.from + j;
                    q.srcFrom = srcOf(p.seg, p.from + i, true);
                    q.srcTo = srcOf(p.seg, p.from + j - 1, false);
                    q.w = w;
                    q.level = 0;
                    addTextBoxes(q, cur, body);
                }
                cur += w + (charsLeft > 0 ? extra : 0);
                i = j;
            }
            return cur;
        }

        /**
         * Places the words of a text piece separately and widens the gaps (justification).
         * <p>
         * 텍스트 조각의 낱말을 따로 놓고 간격을 넓힌다(양쪽 정렬).
         *
         * @param p     the piece / 조각
         * @param start x of the piece / 조각의 x
         * @param extra extra width per space / 공백마다 더하는 너비
         * @param body  receives the fragments / 조각을 받는다
         * @return x after the piece / 조각 뒤의 x
         */
        double placeWords(Piece p, double start, double extra, List<LayoutBox> body) {
            double cur = start;
            String t = p.text;
            int i = 0;
            // 텍스트를 공백 단위로 훑어 낱말과 공백을 번갈아 놓는다.
            while (i < t.length()) {
                int j = i;
                boolean sp = t.charAt(i) == ' ';
                // 공백인지 아닌지가 같은 동안 구간 끝을 늘린다.
                while (j < t.length() && (t.charAt(j) == ' ') == sp) {
                    j++;
                }
                String piece = t.substring(i, j);
                double w = w(p.seg, p.from + i, p.from + j);
                if (sp) {
                    cur += w + extra * piece.length();
                } else {
                    Piece q = new Piece();
                    q.kind = InlineItem.Kind.TEXT;
                    q.seg = p.seg;
                    q.text = piece;
                    q.from = p.from + i;
                    q.to = p.from + j;
                    q.srcFrom = srcOf(p.seg, p.from + i, true);
                    q.srcTo = srcOf(p.seg, p.from + j - 1, false);
                    q.w = w;
                    q.level = 0;
                    addTextBoxes(q, cur, body);
                    cur += w;
                }
                i = j;
            }
            return cur;
        }

        /**
         * Positions the pieces of a line that needs bidirectional reordering: the text is analysed with {@link Bidi}, split where the
         * embedding level changes and the parts are placed in visual order. The horizontal margin, border and padding of inline
         * elements are not applied in such lines.
         * <p>
         * 양방향 재배열이 필요한 줄의 조각을 배치한다: 텍스트를 {@link Bidi} 로 분석해 임베딩 수준이 바뀌는 곳에서 나누고 시각 순서로 놓는다. 이런 줄에서는
         * 인라인 요소의 가로 margin, border, padding 을 적용하지 않는다.
         *
         * @param rtl        true if the base direction is right to left / 기본 방향이 오른쪽에서 왼쪽이면 true
         * @param lo         left limit of the content area / 내용 영역의 왼쪽 한계
         * @param hi         right limit of the content area / 내용 영역의 오른쪽 한계
         * @param center     true for centered text / 가운데 정렬이면 true
         * @param alignRight true for text aligned to the right / 오른쪽 정렬이면 true
         * @param content    receives the content pieces / 내용 조각을 받는다
         * @param spans      receives the extents / 범위를 받는다
         * @param out        receives the fragments / 조각을 받는다
         * @return x of the end of the content / 내용 끝 x
         */
        double placeBidi(boolean rtl, double lo, double hi, boolean center, boolean alignRight, List<Piece> content,
                Map<LayoutBox, Span> spans, List<LayoutBox> out) {
            StringBuilder lt = new StringBuilder();
            // 줄의 텍스트를 이어 붙여 양방향 분석 대상으로 만든다.
            for (int i = 0; i < line.size(); i++) {
                if (line.get(i).kind == InlineItem.Kind.TEXT) {
                    lt.append(line.get(i).text);
                }
            }
            Bidi bd = new Bidi(lt.toString(), rtl ? Bidi.DIRECTION_RIGHT_TO_LEFT : Bidi.DIRECTION_LEFT_TO_RIGHT);
            int base = rtl ? 1 : 0;
            List<Piece> parts = new ArrayList<Piece>();
            int pos = 0;
            // 텍스트 조각은 수준이 바뀌는 곳에서 나누고 원자적 상자는 기본 수준으로 둔다.
            for (int i = 0; i < line.size(); i++) {
                Piece p = line.get(i);
                if (p.kind == InlineItem.Kind.ATOMIC) {
                    p.level = base;
                    parts.add(p);
                } else if (p.kind == InlineItem.Kind.TEXT) {
                    int k = 0;
                    // 텍스트 조각을 끝까지 훑으며 수준이 바뀌는 곳에서 나눈다.
                    while (k < p.text.length()) {
                        int lv = bd.getLevelAt(pos + k);
                        int m = k;
                        // 같은 수준이 이어지는 동안 구간 끝을 늘린다.
                        while (m < p.text.length() && bd.getLevelAt(pos + m) == lv) {
                            m++;
                        }
                        Piece q = new Piece();
                        q.kind = InlineItem.Kind.TEXT;
                        q.seg = p.seg;
                        q.text = p.text.substring(k, m);
                        q.level = lv;
                        q.from = p.from + k;
                        q.to = p.from + m;
                        q.w = w(p.seg, p.from + k, p.from + m);
                        q.srcFrom = srcOf(p.seg, p.from + k, true);
                        q.srcTo = srcOf(p.seg, p.from + m - 1, false);
                        parts.add(q);
                        k = m;
                    }
                    pos += p.text.length();
                }
            }
            int n = parts.size();
            byte[] levels = new byte[n];
            Piece[] arr = new Piece[n];
            double total = 0;
            // 수준 배열과 조각 배열을 만들고 전체 너비를 센다.
            for (int i = 0; i < n; i++) {
                levels[i] = (byte) parts.get(i).level;
                arr[i] = parts.get(i);
                total += arr[i].w;
            }
            if (n > 1) {
                Bidi.reorderVisually(levels, 0, arr, 0, n);
            }
            double room = hi - lo - total;
            double cur = center ? lo + room / 2 : (alignRight ? lo + room : lo);
            if (room < 0 && !rtl) {
                cur = lo;
            }
            List<LayoutBox> body = new ArrayList<LayoutBox>();
            // 시각 순서로 조각의 x 를 정하고 둘러싼 인라인 요소의 범위를 넓힌다.
            for (int i = 0; i < n; i++) {
                Piece p = arr[i];
                content.add(p);
                double x0 = cur;
                if (p.kind == InlineItem.Kind.ATOMIC) {
                    p.box.lx = cur + p.box.marginLeft;
                    p.box.inlineOwner = p.seg.owner;
                    body.add(p.box);
                    fragTag.put(p.box, p);
                } else {
                    addTextBoxes(p, cur, body);
                }
                cur += p.w;
                // 조각을 둘러싼 인라인 요소를 바깥으로 올라가며 범위를 넓힌다.
                for (LayoutBox pr = p.seg.owner; pr != null; pr = pr.inlineParent) {
                    Span s = spans.get(pr);
                    if (s == null) {
                        s = new Span();
                        spans.put(pr, s);
                    }
                    s.used = true;
                    if (!s.closed) {
                        s.start = x0;
                        s.end = cur;
                        s.closed = true;
                    } else {
                        s.start = Math.min(s.start, x0);
                        s.end = Math.max(s.end, cur);
                    }
                }
            }
            closeSpans(spans, cur, out);
            out.addAll(body);
            return cur;
        }

        /**
         * Marks the inline elements around a piece as having content on this line.
         * <p>
         * 조각을 둘러싼 인라인 요소가 이 줄에 내용을 가졌다고 표시한다.
         *
         * @param owner innermost inline element or null / 가장 안쪽 인라인 요소 또는 null
         * @param spans the extents / 범위
         */
        void markUsed(LayoutBox owner, Map<LayoutBox, Span> spans) {
            // 바깥으로 올라가며 각 인라인 요소를 사용됨으로 표시한다.
            for (LayoutBox pr = owner; pr != null; pr = pr.inlineParent) {
                Span s = spans.get(pr);
                if (s != null) {
                    s.used = true;
                }
            }
        }

        /**
         * Closes the extents of elements that stay open at the end of the line and creates the fragment boxes of inline elements.
         * <p>
         * 줄 끝에서 열린 채 남은 요소의 범위를 닫고 인라인 요소의 조각 상자를 만든다.
         *
         * @param spans the extents / 범위
         * @param end   x of the end of the content / 내용 끝 x
         * @param out   receives the boxes / 상자를 받는다
         */
        void closeSpans(Map<LayoutBox, Span> spans, double end, List<LayoutBox> out) {
            // 요소마다 조각 상자를 만든다(내용이나 가장자리가 있는 것만).
            for (Map.Entry<LayoutBox, Span> e : spans.entrySet()) {
                Span s = e.getValue();
                if (!s.used) {
                    continue;
                }
                if (!s.closed) {
                    s.end = end;
                }
                LayoutBox proto = e.getKey();
                LayoutBox f = new LayoutBox(LayoutBox.Type.INLINE, proto.node, proto.style);
                f.pseudo = proto.pseudo;
                f.inlineLevel = true;
                f.inlineParent = proto.inlineParent;
                f.inlineOwner = proto;
                f.lx = s.start;
                f.width = Math.max(0, s.end - s.start);
                f.decoration = proto.decoration;
                f.borderLeft = s.startEdge ? proto.borderLeft : 0;
                f.paddingLeft = s.startEdge ? proto.paddingLeft : 0;
                f.borderRight = s.endEdge ? proto.borderRight : 0;
                f.paddingRight = s.endEdge ? proto.paddingRight : 0;
                f.borderTop = proto.borderTop;
                f.borderBottom = proto.borderBottom;
                f.paddingTop = proto.paddingTop;
                f.paddingBottom = proto.paddingBottom;
                fragTag.put(f, proto);
                out.add(f);
            }
        }

        /**
         * Creates the box of a text fragment (its vertical position is set later).
         * <p>
         * 텍스트 조각 상자를 만든다(세로 위치는 나중에 정한다).
         *
         * @param p the piece / 조각
         * @param x x on the line / 줄에서의 x
         * @param w width / 너비
         * @return the box / 상자
         */
        LayoutBox textBox(Piece p, double x, double w) {
            LayoutBox t = new LayoutBox(LayoutBox.Type.TEXT, p.seg.node, p.seg.style);
            t.inlineLevel = true;
            t.text = p.text;
            t.font = p.seg.font;
            t.fontBlocked = p.seg.invisible;
            t.ascent = p.seg.asc;
            t.descent = p.seg.desc;
            t.letterSpacing = p.seg.ls;
            t.decoration = p.seg.deco;
            t.inlineOwner = p.seg.owner;
            t.lx = x;
            t.width = w;
            t.height = p.seg.asc + p.seg.desc;
            t.bidiLevel = p.level;
            if (p.srcTo > p.srcFrom || p.srcFrom > 0) {
                t.srcStart = p.srcFrom;
                t.srcEnd = p.srcTo;
            } else {
                t.srcStart = srcOf(p.seg, p.from, true);
                t.srcEnd = srcOf(p.seg, Math.max(p.from, p.to - 1), false);
            }
            fragTag.put(t, p);
            return t;
        }

        /**
         * Returns the source offset in the Text node for a character index of a segment.
         * <p>
         * 세그먼트의 글자 위치에 해당하는 Text 노드의 원본 위치를 반환한다.
         *
         * @param s     the segment / 세그먼트
         * @param index character index / 글자 위치
         * @param start true for the start offset, false for the end offset (exclusive) of that character / 시작 위치이면 true, 그 글자의 끝(제외) 위치이면 false
         * @return source offset / 원본 위치
         */
        int srcOf(Seg s, int index, boolean start) {
            if (s.src == null || s.src.length == 0) {
                return start ? index : index + 1;
            }
            int i = Math.max(0, Math.min(index, s.src.length - 1));
            return start ? s.src[i] : s.src[i] + 1;
        }
    }
}
