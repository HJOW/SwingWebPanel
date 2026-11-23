package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The truncation of the lines of a block container that holds inline content: the ellipsis of <code>text-overflow</code> (CSS Basic User Interface
 * Level 3, <a href="https://www.w3.org/TR/css-ui-3/#text-overflow">https://www.w3.org/TR/css-ui-3/#text-overflow</a>), which replaces the end of a line
 * that overflows the content box of a box that clips its overflow, and <code>line-clamp</code> (CSS Overflow Level 4, with the older prefixed form
 * <code>-webkit-line-clamp</code>), which keeps the first N lines and puts an ellipsis at the end of the last one. It works on the finished line boxes: the
 * text fragments beyond the cut are removed, the fragment that crosses the cut loses characters until the ellipsis fits after it, an atomic box that does
 * not fit is removed whole, and a fragment of the ellipsis is added with the style of the last text. Only the left-to-right direction is handled (the
 * ellipsis of a right-to-left line would be at its left end).
 * <p>
 * 인라인 콘텐츠를 가진 블록 컨테이너의 줄 자르기: 넘침을 자르는 상자에서 content box 를 넘는 줄의 끝을 대체하는 <code>text-overflow</code> 의 줄임표(CSS Basic User
 * Interface Level 3, <a href="https://www.w3.org/TR/css-ui-3/#text-overflow">https://www.w3.org/TR/css-ui-3/#text-overflow</a>), 그리고 처음 N 줄을 남기고 마지막 줄 끝에
 * 줄임표를 붙이는 <code>line-clamp</code>(CSS Overflow Level 4, 예전 접두사 형태 <code>-webkit-line-clamp</code>). 완성된 줄 상자 위에서 동작한다: 자르는 곳 뒤의 텍스트
 * 조각은 지우고, 자르는 곳에 걸친 조각은 줄임표가 뒤에 들어갈 때까지 글자를 잃으며, 들어가지 않는 원자적 상자는 통째로 지우고, 마지막 텍스트의 스타일로 줄임표 조각을 더한다.
 * 왼쪽에서 오른쪽 방향만 다룬다(오른쪽에서 왼쪽 줄의 줄임표는 줄의 왼쪽 끝에 있어야 한다).
 */
final class TextOverflow {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TextOverflow() {
    }

    /**
     * Truncates the lines of a block container: first the lines beyond <code>line-clamp</code>, then the overflow of the lines that remain, and
     * returns the height of the lines that remain.
     * <p>
     * 블록 컨테이너의 줄을 자른다: 먼저 <code>line-clamp</code> 를 넘는 줄을, 그 다음 남은 줄의 넘침을 자르며, 남은 줄의 높이를 반환한다.
     *
     * @param b           the block container whose line boxes have been created / 줄 상자를 만든 블록 컨테이너
     * @param cw          the content width / content 너비
     * @param ch          the height of all the lines / 모든 줄의 높이
     * @param contentLeft x of the content box in the border box / border box 안 content box 의 x
     * @param contentTop  y of the content box in the border box / border box 안 content box 의 y
     * @param tm          the text measure / 텍스트 측정기
     * @return the height of the lines that remain / 남은 줄의 높이
     */
    static double apply(LayoutBox b, double cw, double ch, double contentLeft, double contentTop, TextMeasure tm) {
        // text-overflow 는 넘침을 자르는 상자에만 적용된다.
        String ellipsis = Css.clips(b.style) ? ellipsisOf(b.style) : null;
        int clamp = clampOf(b.style);
        if (ellipsis == null && clamp == 0 || "rtl".equals(Css.kw(b.style, Prop.DIRECTION))) {
            return ch;
        }
        List<LayoutBox> lines = new ArrayList<LayoutBox>();
        // 줄 상자를 순서대로 모은다.
        for (int i = 0; i < b.children.size(); i++) {
            if (b.children.get(i).type == LayoutBox.Type.LINE) {
                lines.add(b.children.get(i));
            }
        }
        double limit = contentLeft + cw;
        boolean clamped = clamp > 0 && lines.size() > clamp;
        double height = ch;
        if (clamped) {
            // 허용한 줄 수를 넘는 줄은 지우고 높이를 마지막으로 남은 줄까지로 줄인다.
            for (int i = lines.size() - 1; i >= clamp; i--) {
                b.children.remove(lines.get(i));
            }
            lines = new ArrayList<LayoutBox>(lines.subList(0, clamp));
            LayoutBox last = lines.get(clamp - 1);
            height = last.ly + last.height - contentTop;
        }
        // 넘치는 줄의 끝을 줄임표로 바꾼다(line-clamp 가 자른 마지막 줄은 넘치지 않아도 줄임표를 붙인다).
        for (int i = 0; i < lines.size(); i++) {
            boolean forced = clamped && i == lines.size() - 1;
            if (ellipsis != null || forced) {
                truncate(b, lines.get(i), limit, ellipsis != null ? ellipsis : "…", forced, tm);
            }
        }
        return height;
    }

    /**
     * Returns the ellipsis string of <code>text-overflow</code>: the end value (the second of two, otherwise the only one) when it is
     * <code>ellipsis</code> or a string, null for <code>clip</code>.
     * <p>
     * <code>text-overflow</code> 의 줄임표 문자열을 반환한다: 끝 값(둘이면 둘째, 아니면 유일한 값)이 <code>ellipsis</code> 나 문자열일 때. <code>clip</code> 이면 null.
     *
     * @param s the style / 스타일
     * @return the string or null / 문자열. 없으면 null
     */
    static String ellipsisOf(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.TEXT_OVERFLOW);
        if (v instanceof CssList && ((CssList) v).size() > 0) {
            v = ((CssList) v).get(((CssList) v).size() - 1);
        }
        if (v instanceof CssString) {
            return ((CssString) v).getValue();
        }
        return v instanceof CssKeyword && "ellipsis".equals(((CssKeyword) v).getName()) ? "…" : null;
    }

    /**
     * Returns the number of lines <code>line-clamp</code> allows, 0 for <code>none</code>.
     * <p>
     * <code>line-clamp</code> 가 허용하는 줄 수를 반환한다. <code>none</code> 이면 0.
     *
     * @param s the style / 스타일
     * @return the number of lines or 0 / 줄 수. 없으면 0
     */
    static int clampOf(ComputedStyle s) {
        CssValue v = s == null ? null : s.get(Prop.LINE_CLAMP);
        return v instanceof CssNumber ? (int) Math.max(0, Math.min(1000000, ((CssNumber) v).getValue())) : 0;
    }

    /**
     * Cuts a line at the content edge and puts the ellipsis at the end of what is kept; a line that does not overflow is left alone unless the
     * ellipsis is forced (the last line of a clamped block), in which case the ellipsis is added after the content.
     * <p>
     * 줄을 content 가장자리에서 자르고 남은 것의 끝에 줄임표를 둔다. 넘치지 않는 줄은 줄임표가 강제되지 않는 한 그대로 두며(clamp 된 블록의 마지막 줄), 강제이면
     * 내용 뒤에 줄임표를 더한다.
     *
     * @param b        the block container / 블록 컨테이너
     * @param line     the line box / 줄 상자
     * @param limit    x of the end of the content box in the border box / border box 안 content box 끝의 x
     * @param ellipsis the string / 문자열
     * @param forced   true to add the ellipsis even if the line does not overflow / 줄이 넘치지 않아도 줄임표를 더하려면 true
     * @param tm       the text measure / 텍스트 측정기
     */
    private static void truncate(LayoutBox b, LayoutBox line, double limit, String ellipsis, boolean forced, TextMeasure tm) {
        List<LayoutBox> frags = new ArrayList<LayoutBox>(line.children);
        double right = 0;
        LayoutBox template = null;
        // 줄의 조각을 훑어 가장 오른쪽 끝과 줄임표의 본보기가 될 마지막 텍스트 조각을 찾는다.
        for (int i = 0; i < frags.size(); i++) {
            LayoutBox f = frags.get(i);
            if (f.type != LayoutBox.Type.INLINE) {
                right = Math.max(right, f.lx + f.width + (f.type == LayoutBox.Type.TEXT ? 0 : f.marginRight));
            }
            if (f.type == LayoutBox.Type.TEXT && (template == null || f.lx >= template.lx)) {
                template = f;
            }
        }
        boolean overflow = line.lx + right > limit + 0.01;
        if (!overflow && !forced) {
            return;
        }
        ComputedStyle ts = template != null ? template.style : b.style;
        Font font = FontResolver.resolve(ts, ellipsis);
        double ellW = tm.advance(font, ellipsis);
        double room = limit - line.lx - ellW;
        // 줄임표가 들어갈 자리조차 없으면 자르지 않고 둔다(넘침 자르기가 줄임표까지 자른다).
        if (room < 0) {
            return;
        }
        double cutX = overflow || right > room ? room : right;
        double end = 0;
        boolean cut = false;
        // 조각을 왼쪽에서 오른쪽으로 보며 자르는 위치 앞의 것만 남긴다.
        List<LayoutBox> ordered = new ArrayList<LayoutBox>(frags);
        java.util.Collections.sort(ordered, new java.util.Comparator<LayoutBox>() {
            /**
             * Orders fragments by their x on the line.
             * <p>
             * 줄에서의 x 로 조각을 정렬한다.
             *
             * @param p first / 첫째
             * @param q second / 둘째
             * @return the order / 순서
             */
            public int compare(LayoutBox p, LayoutBox q) {
                return p.lx < q.lx ? -1 : p.lx > q.lx ? 1 : 0;
            }
        });
        // 정렬한 조각을 하나씩 보며 자르는 곳 앞의 것만 남기고 뒤의 것은 지운다.
        for (int i = 0; i < ordered.size(); i++) {
            LayoutBox f = ordered.get(i);
            if (f.type == LayoutBox.Type.INLINE) {
                // 인라인 요소의 상자(배경, 테두리)는 자르는 곳까지로 줄이거나 아예 없앤다.
                if (cut && f.lx >= cutX) {
                    line.children.remove(f);
                } else if (f.lx + f.width > cutX) {
                    f.width = Math.max(0, cutX - f.lx);
                }
                continue;
            }
            if (cut) {
                line.children.remove(f);
            } else if (f.lx + f.width <= cutX + 0.01) {
                end = Math.max(end, f.lx + f.width);
            } else if (f.type == LayoutBox.Type.TEXT && f.lx < cutX) {
                end = Math.max(end, cutText(f, cutX, tm));
                if (f.text.length() == 0) {
                    line.children.remove(f);
                }
                cut = true;
            } else {
                line.children.remove(f);
                cut = true;
            }
        }
        LayoutBox e = new LayoutBox(LayoutBox.Type.TEXT, template != null ? template.node : b.node, ts);
        e.inlineLevel = true;
        e.text = ellipsis;
        e.font = font;
        double[] fm = FontResolver.metrics(font);
        e.ascent = template != null ? template.ascent : fm[0];
        e.descent = template != null ? template.descent : fm[1];
        e.height = e.ascent + e.descent;
        e.lx = end;
        e.ly = template != null ? template.ly : line.baseline - e.ascent;
        e.baseline = e.ascent;
        e.width = ellW;
        e.decoration = template != null ? template.decoration : 0;
        e.inlineOwner = template != null ? template.inlineOwner : null;
        e.srcStart = template != null ? template.srcEnd : 0;
        e.srcEnd = e.srcStart;
        line.add(e);
    }

    /**
     * Shortens the text of a fragment so that it ends at or before an x, dropping trailing white space, and returns the x at which it ends.
     * <p>
     * 조각의 텍스트가 주어진 x 에서 끝나거나 그 앞에서 끝나도록 줄이고(뒤의 공백은 버린다) 끝나는 x 를 반환한다.
     *
     * @param f    the text fragment / 텍스트 조각
     * @param cutX the x on the line where the kept text must end / 남는 텍스트가 끝나야 하는 줄 위의 x
     * @param tm   the text measure / 텍스트 측정기
     * @return the x after the kept text / 남는 텍스트 뒤의 x
     */
    private static double cutText(LayoutBox f, double cutX, TextMeasure tm) {
        String t = f.text;
        int n = 0;
        // 앞에서부터 한 글자(서로게이트 쌍은 한 글자)씩 늘려 가며 들어가는 가장 긴 접두사를 찾는다.
        for (int i = 1; i <= t.length(); i++) {
            if (i < t.length() && Character.isLowSurrogate(t.charAt(i))) {
                continue;
            }
            double w = tm.advance(f.font, t.substring(0, i)) + f.letterSpacing * i;
            if (f.lx + w <= cutX + 0.01) {
                n = i;
            } else {
                break;
            }
        }
        // 뒤의 공백을 버려 줄임표가 단어에 바로 붙게 한다.
        while (n > 0 && Character.isWhitespace(t.charAt(n - 1))) {
            n--;
        }
        f.text = t.substring(0, n);
        f.width = n == 0 ? 0 : tm.advance(f.font, f.text) + f.letterSpacing * n;
        f.srcEnd = Math.min(f.srcEnd, f.srcStart + n);
        return f.lx + f.width;
    }
}
