package org.duckdns.hjow.swingwebpanel.css.cascade;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * The presentational hints of tables (plan item P07-07-b-7): the "following rules are also expected to apply, as presentational hints"
 * and the attribute mappings of HTML5 2014 section 10.3.9 for <code>table</code>, <code>caption</code>, <code>colgroup</code>,
 * <code>col</code>, <code>thead</code>, <code>tbody</code>, <code>tfoot</code>, <code>tr</code>, <code>td</code> and <code>th</code>:
 * <code>align</code> and <code>valign</code>, <code>bgcolor</code> and <code>background</code>, <code>width</code> and <code>height</code>,
 * <code>hspace</code> and <code>vspace</code>, <code>cellspacing</code> and <code>cellpadding</code>, <code>border</code>,
 * <code>bordercolor</code>, <code>frame</code> and <code>rules</code>, <code>nowrap</code> and the quirks-mode exception of
 * <code>nowrap</code> with a length <code>width</code>. The rules that name a table in their selector (cells, rows, row groups and column
 * groups of a table with <code>border</code> or <code>rules</code>) look at the table through the structure that the selector requires, so
 * a nested table never receives the hints of the outer one. The style-sheet rules of the section (border colors, <code>th</code>
 * alignment, forms in tables) are in {@link org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet}. Source:
 * https://www.w3.org/TR/2014/REC-html5-20141028/rendering.html#tables-0 (retrieved 2026-11-17). Not mapped, because the section does
 * not: <code>align</code>, <code>char</code> and <code>charoff</code> of <code>col</code>; <code>height</code> of row groups;
 * <code>summary</code>, <code>axis</code>, <code>abbr</code>, <code>headers</code>, <code>scope</code>.
 * <p>
 * 표의 표현 힌트(계획 P07-07-b-7): HTML5 2014 §10.3.9 의 "다음 규칙도 표현 힌트로 적용된다"와 <code>table</code>, <code>caption</code>,
 * <code>colgroup</code>, <code>col</code>, <code>thead</code>, <code>tbody</code>, <code>tfoot</code>, <code>tr</code>, <code>td</code>, <code>th</code> 의
 * 속성 대응: <code>align</code>, <code>valign</code>, <code>bgcolor</code>, <code>background</code>, <code>width</code>, <code>height</code>,
 * <code>hspace</code>, <code>vspace</code>, <code>cellspacing</code>, <code>cellpadding</code>, <code>border</code>, <code>bordercolor</code>,
 * <code>frame</code>, <code>rules</code>, <code>nowrap</code>, 길이 <code>width</code> 가 있는 <code>nowrap</code> 의 호환 모드 예외. 선택자에 표가 나오는
 * 규칙(<code>border</code> 나 <code>rules</code> 가 있는 표의 셀, 행, 행 그룹, 열 그룹)은 선택자가 요구하는 구조로 표를 찾으므로 중첩된 표는 바깥
 * 표의 힌트를 받지 않는다. 절의 스타일 시트 규칙(border 색, <code>th</code> 정렬, 표 안의 폼)은 {@link org.duckdns.hjow.swingwebpanel.css.cssom.UaStyleSheet} 에 있다.
 * 절이 말하지 않는 것은 대응하지 않는다: <code>col</code> 의 <code>align</code>, <code>char</code>, <code>charoff</code>, 행 그룹의 <code>height</code>,
 * <code>summary</code>, <code>axis</code>, <code>abbr</code>, <code>headers</code>, <code>scope</code>.
 */
final class TableHints {

    /** Values of the <code>frame</code> attribute and their <code>border-style</code> in top, right, bottom, left order. / <code>frame</code> 값과 위, 오른쪽, 아래, 왼쪽 순서의 <code>border-style</code>. */
    private static final String[][] FRAMES = {
        {"void", "hidden"},
        {"above", "outset hidden hidden hidden"},
        {"below", "hidden hidden outset hidden"},
        {"hsides", "outset hidden outset hidden"},
        {"lhs", "hidden hidden hidden outset"},
        {"rhs", "hidden outset hidden hidden"},
        {"vsides", "hidden outset"},
        {"box", "outset"},
        {"border", "outset"}
    };

    /** The values of the <code>frame</code> attribute. / <code>frame</code> 속성의 값. */
    private static final String[] FRAME_NAMES = {"void", "above", "below", "hsides", "lhs", "rhs", "vsides", "box", "border"};

    /** The values of the <code>rules</code> attribute. / <code>rules</code> 속성의 값. */
    private static final String[] RULES = {"none", "groups", "rows", "cols", "all"};

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TableHints() {
    }

    /**
     * Returns the <code>rules</code> value of a table, or null if it is absent or not one of the five keywords.
     * <p>
     * 표의 <code>rules</code> 값을 반환한다. 없거나 다섯 키워드가 아니면 null.
     *
     * @param table the table or null / 표 또는 null
     * @return the lower-case keyword or null / 소문자 키워드. 없으면 null
     */
    static String rules(Element table) {
        return table == null ? null : LegacyAttributes.keyword(table, "rules", RULES);
    }

    /**
     * Tells whether the <code>border</code> attribute of a table is "not equivalent to zero": present and, parsed as a non-negative
     * integer, either an error or a number other than zero.
     * <p>
     * 표의 <code>border</code> 속성이 "0 과 같지 않은지" 알려 준다: 있고, 음이 아닌 정수로 해석하면 오류이거나 0 이 아닌 숫자이다.
     *
     * @param table the table or null / 표 또는 null
     * @return true if the border rules apply / border 규칙이 적용되면 true
     */
    static boolean hasBorder(Element table) {
        if (table == null) {
            return false;
        }
        String raw = table.getAttribute("border");
        return raw != null && LegacyAttributes.nonNegativeInteger(raw) != 0;
    }

    /**
     * Adds the hints of a <code>table</code>.
     * <p>
     * <code>table</code> 의 힌트를 더한다.
     *
     * @param e  the table / 표
     * @param sb the output / 출력
     */
    static void table(Element e, StringBuilder sb) {
        LegacyAttributes.color(e, "bgcolor", "background-color", sb);
        LegacyAttributes.background(e, sb);
        LegacyAttributes.dimension(e, "width", "width", sb);
        LegacyAttributes.dimension(e, "height", "height", sb);
        LegacyAttributes.dimension(e, "hspace", "margin-left", sb);
        LegacyAttributes.dimension(e, "hspace", "margin-right", sb);
        LegacyAttributes.dimension(e, "vspace", "margin-top", sb);
        LegacyAttributes.dimension(e, "vspace", "margin-bottom", sb);
        String raw = e.getAttribute("border");
        // border 속성은 두께가 되며 해석 오류이면 1px 이다.
        if (raw != null) {
            double border = LegacyAttributes.nonNegativeInteger(raw);
            LegacyAttributes.add(sb, "border-width", border < 0 ? "1px" : LegacyAttributes.px(border));
        }
        String color = HtmlPresentationalHints.legacyColor(e.getAttribute("bordercolor"));
        if (color != null) {
            LegacyAttributes.add(sb, "border-color", color);
        }
        String spacing = LegacyAttributes.pixels(e.getAttribute("cellspacing"));
        if (spacing != null) {
            LegacyAttributes.add(sb, "border-spacing", spacing);
        }
        String align = LegacyAttributes.keyword(e, "align", "left", "right", "center");
        if (align != null) {
            // 왼쪽과 오른쪽은 표를 띄우고 가운데는 좌우 margin 을 auto 로 한다. middle 은 표에는 효과가 없다.
            if (align.equals("center")) {
                LegacyAttributes.add(sb, "margin-left", "auto");
                LegacyAttributes.add(sb, "margin-right", "auto");
            } else {
                LegacyAttributes.add(sb, "float", align);
            }
        }
        // 규격의 나열 순서(rules, border, frame)대로 border-style 을 낸다. 나중 선언이 이긴다.
        if (rules(e) != null) {
            LegacyAttributes.add(sb, "border-style", "hidden");
            LegacyAttributes.add(sb, "border-collapse", "collapse");
        }
        if (hasBorder(e)) {
            LegacyAttributes.add(sb, "border-style", "outset");
        }
        String frame = LegacyAttributes.keyword(e, "frame", FRAME_NAMES);
        // frame 값 표에서 일치하는 값의 변별 스타일을 찾는다.
        if (frame != null) {
            for (int i = 0; i < FRAMES.length; i++) {
                if (FRAMES[i][0].equals(frame)) {
                    LegacyAttributes.add(sb, "border-style", FRAMES[i][1]);
                    break;
                }
            }
        }
    }

    /**
     * Adds the hints of a <code>td</code> or <code>th</code>.
     * <p>
     * <code>td</code> 또는 <code>th</code> 의 힌트를 더한다.
     *
     * @param e  the cell / 셀
     * @param sb the output / 출력
     */
    static void cell(Element e, StringBuilder sb) {
        Element table = LegacyAttributes.tableOfCell(e);
        if (table != null) {
            String pad = LegacyAttributes.pixels(table.getAttribute("cellpadding"));
            if (pad != null) {
                LegacyAttributes.add(sb, "padding", pad);
            }
            if (hasBorder(table)) {
                LegacyAttributes.add(sb, "border-width", "1px");
                LegacyAttributes.add(sb, "border-style", "inset");
            }
            String rules = rules(table);
            // rules 값마다 셀이 그리는 선이 다르다: none/groups/rows 는 없음, cols 는 세로선, all 은 모두.
            if (rules != null) {
                LegacyAttributes.add(sb, "border-width", "1px");
                LegacyAttributes.add(sb, "border-style", rules.equals("all") ? "solid" : rules.equals("cols") ? "none solid" : "none");
            }
        }
        LegacyAttributes.color(e, "bgcolor", "background-color", sb);
        LegacyAttributes.background(e, sb);
        LegacyAttributes.dimension(e, "width", "width", sb);
        LegacyAttributes.dimension(e, "height", "height", sb);
        LegacyAttributes.textAlign(e, sb, true, true);
        LegacyAttributes.valign(e, sb);
        if (e.hasAttribute("nowrap")) {
            // 호환 모드에서 길이 width 가 함께 있으면 nowrap 을 무시한다.
            Document d = e.getOwnerDocument();
            boolean quirks = d != null && d.getCompatibilityMode() == Document.QUIRKS_MODE;
            if (!(quirks && LegacyAttributes.isLength(e.getAttribute("width")))) {
                LegacyAttributes.add(sb, "white-space", "nowrap");
            }
        }
    }

    /**
     * Adds the hints of a <code>tr</code>.
     * <p>
     * <code>tr</code> 의 힌트를 더한다.
     *
     * @param e  the row / 행
     * @param sb the output / 출력
     */
    static void row(Element e, StringBuilder sb) {
        LegacyAttributes.color(e, "bgcolor", "background-color", sb);
        LegacyAttributes.background(e, sb);
        LegacyAttributes.dimension(e, "height", "height", sb);
        LegacyAttributes.textAlign(e, sb, true, true);
        LegacyAttributes.valign(e, sb);
        // rules=rows 는 행의 위아래에 선을 긋는다.
        if ("rows".equals(rules(LegacyAttributes.tableOfRow(e)))) {
            lines(sb, "top", "bottom");
        }
    }

    /**
     * Adds the hints of a <code>thead</code>, <code>tbody</code> or <code>tfoot</code>.
     * <p>
     * <code>thead</code>, <code>tbody</code>, <code>tfoot</code> 의 힌트를 더한다.
     *
     * @param e  the row group / 행 그룹
     * @param sb the output / 출력
     */
    static void section(Element e, StringBuilder sb) {
        LegacyAttributes.color(e, "bgcolor", "background-color", sb);
        LegacyAttributes.background(e, sb);
        LegacyAttributes.textAlign(e, sb, true, true);
        LegacyAttributes.valign(e, sb);
        // rules=groups 는 행 그룹의 위아래에 선을 긋는다.
        if ("groups".equals(rules(LegacyAttributes.tableOfGroup(e)))) {
            lines(sb, "top", "bottom");
        }
    }

    /**
     * Adds the hints of a <code>colgroup</code>, and of a <code>col</code> through its <code>width</code>.
     * The <code>width</code> of a <code>colgroup</code> is the HTML 4.01 default width of its columns, kept for old pages although the
     * 2014 rendering section names only <code>col</code>.
     * <p>
     * <code>colgroup</code> 의 힌트와 <code>col</code> 의 <code>width</code> 힌트를 더한다. <code>colgroup</code> 의 <code>width</code> 는 HTML 4.01 의
     * 열 기본 너비로, 2014 렌더링 절이 <code>col</code> 만 언급하지만 오래된 페이지를 위해 남긴다.
     *
     * @param e   the column or column group / 열 또는 열 그룹
     * @param sb  the output / 출력
     */
    static void column(Element e, StringBuilder sb) {
        LegacyAttributes.dimension(e, "width", "width", sb);
        // rules=groups 는 열 그룹의 좌우에 선을 긋는다.
        if (LegacyAttributes.is(e, "colgroup") && "groups".equals(rules(LegacyAttributes.tableOfGroup(e)))) {
            lines(sb, "left", "right");
        }
    }

    /**
     * Adds the hints of a <code>caption</code>: its text is centered unless <code>align</code> says <code>left</code>,
     * <code>right</code> or <code>justify</code>, and <code>align=bottom</code> moves it below the table.
     * <p>
     * <code>caption</code> 의 힌트를 더한다: <code>align</code> 이 <code>left</code>, <code>right</code>, <code>justify</code> 가 아니면 글을 가운데 정렬하고
     * <code>align=bottom</code> 은 표 아래로 옮긴다.
     *
     * @param e  the caption / 캡션
     * @param sb the output / 출력
     */
    static void caption(Element e, StringBuilder sb) {
        String align = LegacyAttributes.keyword(e, "align", "left", "right", "justify", "bottom");
        if (align != null && !align.equals("bottom")) {
            LegacyAttributes.add(sb, "text-align", align);
        } else {
            LegacyAttributes.add(sb, "text-align", "center");
        }
        if ("bottom".equals(align)) {
            LegacyAttributes.add(sb, "caption-side", "bottom");
        }
    }

    /**
     * Adds a one-pixel solid line on two opposite sides.
     * <p>
     * 마주 보는 두 변에 1px 실선을 더한다.
     *
     * @param sb     the output / 출력
     * @param first  the first side name / 첫 변 이름
     * @param second the second side name / 둘째 변 이름
     */
    private static void lines(StringBuilder sb, String first, String second) {
        LegacyAttributes.add(sb, "border-" + first + "-width", "1px");
        LegacyAttributes.add(sb, "border-" + first + "-style", "solid");
        LegacyAttributes.add(sb, "border-" + second + "-width", "1px");
        LegacyAttributes.add(sb, "border-" + second + "-style", "solid");
    }
}
