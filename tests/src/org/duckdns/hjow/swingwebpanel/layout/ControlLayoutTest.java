package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the sizes and baselines of form controls (plan items P11-05 and P09-02): the intrinsic size of text fields, text areas, check
 * boxes, buttons, drop-downs, list boxes, progress bars and meters, the precedence of the author's CSS over them, the missing box of hidden
 * inputs and the alignment of the text baseline of a control with the text around it. Expected values are computed from the font that the
 * control box reports.
 * <p>
 * 폼 컨트롤의 크기와 기준선 시험(계획 P11-05, P09-02): 텍스트 필드, 텍스트 영역, 체크박스, 단추, 드롭다운, 리스트 박스, 진행 막대, 미터의 고유 크기, 작성자 CSS 의
 * 우선, hidden input 의 상자 없음, 컨트롤의 글 기준선과 주변 글의 정렬. 기대값은 컨트롤 상자가 알려 주는 글꼴로 계산한다.
 */
public class ControlLayoutTest {

    /** Tolerance for coordinates. 좌표 허용 오차. */
    private static final double T = 0.01;

    /** A style that removes the spacing of a control and fixes its font. 컨트롤의 여백을 없애고 글꼴을 고정하는 스타일. */
    private static final String BARE = "font:10px monospace;margin:0;padding:0;border:0;";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ControlLayoutTest() {
    }

    /**
     * Returns the height of one line of a font.
     * <p>
     * 글꼴 한 줄의 높이를 반환한다.
     *
     * @param f the font / 글꼴
     * @return the height / 높이
     */
    private static double lineHeight(Font f) {
        double[] m = FontResolver.metrics(f);
        return Math.ceil(m[0] + m[1] + m[2]);
    }

    /**
     * Returns the width of a string in the font of a box.
     * <p>
     * 상자의 글꼴로 문자열의 너비를 반환한다.
     *
     * @param b the control box / 컨트롤 상자
     * @param s the string / 문자열
     * @return the width / 너비
     */
    private static double advance(LayoutBox b, String s) {
        return LayoutTestSupport.advance(b.getFont(), s);
    }

    /**
     * Checks the size of one-line text fields from <code>size</code>, and the boxes of the author's CSS.
     * <p>
     * <code>size</code> 로 정해지는 한 줄 텍스트 필드의 크기와 작성자 CSS 상자를 확인한다.
     */
    public void testTextFieldSizes() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a style='" + BARE + "' size=10><input id=b style='" + BARE + "'>"
                + "<input id=c style='" + BARE + "padding:3px;border:2px solid;width:100px;height:30px'>"
                + "<input id=d style='" + BARE + "padding:3px;border:2px solid;width:100px;height:30px;box-sizing:border-box'>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        double zero = advance(a, "0");
        Check.near(10 * zero, a.getWidth(), T, "size=10 gives ten digit widths");
        Check.near(lineHeight(a.getFont()), a.getHeight(), T, "one line high");
        Check.near(20 * zero, LayoutTestSupport.box(r, "b").getWidth(), T, "the default size is 20");
        LayoutBox c = LayoutTestSupport.box(r, "c");
        Check.near(110, c.getWidth(), T, "the author's width is the content width plus padding and border");
        Check.near(40, c.getHeight(), T, "the author's height is the content height plus padding and border");
        LayoutBox d = LayoutTestSupport.box(r, "d");
        Check.near(100, d.getWidth(), T, "box-sizing: border-box makes the width the border box width");
        Check.near(30, d.getHeight(), T, "and the height too");
    }

    /**
     * Checks that the UA style gives a text field its inset border and padding around the intrinsic size.
     * <p>
     * UA 스타일이 고유 크기 둘레에 inset 경계선과 안쪽 여백을 주는지 확인한다.
     */
    public void testDefaultBoxOfATextField() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a style='font:10px monospace' size=5>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(2, a.getBorderLeft(), T, "the UA border is 2px");
        Check.near(2, a.getPaddingLeft(), T, "the UA padding is 2px");
        Check.near(5 * advance(a, "0") + 8, a.getWidth(), T, "content + padding + border");
        Check.near(lineHeight(a.getFont()) + 6, a.getHeight(), T, "content + padding + border");
    }

    /**
     * Checks the size of date and time fields (a sample of their format) and number fields.
     * <p>
     * 날짜와 시간 필드(형식 표본)와 숫자 필드의 크기를 확인한다.
     */
    public void testDateAndNumberFields() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a type=date style='" + BARE + "'><input id=b type=number style='" + BARE
                + "' size=4><input id=c type=time style='" + BARE + "'>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(advance(a, "0000-00-00") + 2, a.getWidth(), T, "a date field fits its format");
        Check.near(4 * advance(a, "0"), LayoutTestSupport.box(r, "b").getWidth(), T, "a number field follows size");
        Check.near(advance(a, "00:00:00") + 2, LayoutTestSupport.box(r, "c").getWidth(), T, "a time field fits its format");
    }

    /**
     * Checks <code>cols</code> and <code>rows</code> of a text area, with and without a scroll bar.
     * <p>
     * 텍스트 영역의 <code>cols</code> 와 <code>rows</code> 를 스크롤 막대가 있는 경우와 없는 경우로 확인한다.
     */
    public void testTextAreaSize() {
        LayoutResult r = LayoutTestSupport.layout("<textarea id=a style='" + BARE + "' cols=8 rows=3></textarea>"
                + "<textarea id=b style='" + BARE + "overflow:hidden' cols=8 rows=3></textarea>"
                + "<textarea id=c style='" + BARE + "'></textarea>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        double zero = advance(a, "0");
        Check.near(8 * zero + 15, a.getWidth(), T, "an auto-overflow text area reserves a scroll bar");
        Check.near(3 * lineHeight(a.getFont()), a.getHeight(), T, "rows lines high");
        Check.near(8 * zero, LayoutTestSupport.box(r, "b").getWidth(), T, "no scroll bar room if the overflow is hidden");
        LayoutBox c = LayoutTestSupport.box(r, "c");
        Check.near(20 * zero + 15, c.getWidth(), T, "the default is 20 columns");
        Check.near(2 * lineHeight(c.getFont()), c.getHeight(), T, "and 2 rows");
    }

    /**
     * Checks check boxes and radio buttons: 13 by 13 pixels with the UA margin.
     * <p>
     * 체크박스와 라디오 단추를 확인한다: UA margin 과 함께 13 x 13 픽셀.
     */
    public void testCheckBoxAndRadio() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a type=checkbox><input id=b type=radio>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(13, a.getWidth(), T, "a check box is 13 wide");
        Check.near(13, a.getHeight(), T, "and 13 high");
        Check.near(4, a.getMarginLeft(), T, "with the margin of browsers");
        Check.near(3, a.getMarginTop(), T, "top margin");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(13, b.getWidth(), T, "a radio button is 13 wide");
        Check.isTrue(b.getX() >= a.getX() + 13 + 3 + 4 - T, "the radio button follows the check box and the margins between them");
    }

    /**
     * Checks that the width of a button is the width of its label, and the default labels.
     * <p>
     * 단추의 너비가 라벨의 너비이고 기본 라벨이 있는지 확인한다.
     */
    public void testButtonLabelSizes() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a type=submit value=OK style='" + BARE + "'>"
                + "<input id=b type=submit style='" + BARE + "'><input id=c type=reset style='" + BARE + "'>"
                + "<input id=d type=button style='" + BARE + "'>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        Check.near(advance(a, "OK"), a.getWidth(), T, "the label is the value");
        Check.near(advance(a, "Submit"), LayoutTestSupport.box(r, "b").getWidth(), T, "a submit button without a value says Submit");
        Check.near(advance(a, "Reset"), LayoutTestSupport.box(r, "c").getWidth(), T, "a reset button says Reset");
        Check.near(0, LayoutTestSupport.box(r, "d").getWidth(), T, "a plain button without a value has no label");
        Check.near(lineHeight(a.getFont()), a.getHeight(), T, "one line high");
    }

    /**
     * Checks the sizes of drop-downs and list boxes.
     * <p>
     * 드롭다운과 리스트 박스의 크기를 확인한다.
     */
    public void testSelectSizes() {
        String opts = "<option>a</option><option>bbb</option><option>cc</option>";
        LayoutResult r = LayoutTestSupport.layout("<select id=a style='" + BARE + "'>" + opts + "</select>"
                + "<select id=b style='" + BARE + "' size=3>" + opts + "</select>"
                + "<select id=c style='" + BARE + "' multiple>" + opts + "</select>"
                + "<select id=d style='" + BARE + "' size=2>" + opts + "</select>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        double zero = advance(a, "0");
        double lh = lineHeight(a.getFont());
        Check.near(3 * zero + ControlSizes.SELECT_PAD + ControlSizes.ARROW_WIDTH, a.getWidth(), T, "a drop-down is as wide as the longest label and its arrow");
        Check.near(lh, a.getHeight(), T, "and one line high");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        Check.near(3 * lh, b.getHeight(), T, "a list box is size rows high");
        Check.near(3 * zero + 8 + ControlSizes.SCROLLBAR, b.getWidth(), T, "plus the width of a scroll bar, as the standard says, even when all options fit");
        Check.near(4 * lh, LayoutTestSupport.box(r, "c").getHeight(), T, "a multiple select shows 4 rows by default");
        Check.near(3 * zero + 8 + ControlSizes.SCROLLBAR, LayoutTestSupport.box(r, "d").getWidth(), T, "the same when the options do not fit");
    }

    /**
     * Checks that progress bars and meters are as wide as ten and five times their font size.
     * <p>
     * 진행 막대와 미터의 너비가 글꼴 크기의 10배, 5배인지 확인한다.
     */
    public void testProgressAndMeter() {
        LayoutResult r = LayoutTestSupport.layout("<progress id=a style='font-size:20px'></progress><meter id=b style='font-size:20px'></meter>", 1000);
        Check.near(200, LayoutTestSupport.box(r, "a").getWidth(), T, "progress is 10em wide");
        Check.near(20, LayoutTestSupport.box(r, "a").getHeight(), T, "and 1em high");
        Check.near(100, LayoutTestSupport.box(r, "b").getWidth(), T, "meter is 5em wide");
        Check.near(20, LayoutTestSupport.box(r, "b").getHeight(), T, "and 1em high");
    }

    /**
     * Checks that a hidden input has no box.
     * <p>
     * hidden input 에 상자가 없는지 확인한다.
     */
    public void testHiddenInputHasNoBox() {
        LayoutResult r = LayoutTestSupport.layout("<p id=p>x<input id=h type=hidden value=1>y</p>", 1000);
        org.duckdns.hjow.swingwebpanel.dom.Element h = ((org.duckdns.hjow.swingwebpanel.dom.Element) r.getRoot().getNode()).getOwnerDocument().getElementById("h");
        Check.notNull(h, "the hidden input exists");
        Check.isTrue(r.getBoxes(h).isEmpty(), "a hidden input has no box");
        Check.notNull(LayoutTestSupport.box(r, "p"), "the paragraph around it has a box");
    }

    /**
     * Checks the legend of a fieldset: it sits over the top border (the middle of the legend is on the middle of the border), it is as wide
     * as its text, the content of the fieldset starts below it, and the room that it needs above the fieldset is left empty.
     * <p>
     * fieldset 의 legend 를 확인한다: 위쪽 경계선 위에 놓이고(legend 가운데가 경계선 가운데에 있다), 너비는 글 너비이며, fieldset 의 내용은 그 아래에서 시작하고, 위로
     * 필요한 자리는 비워 둔다.
     */
    public void testFieldsetLegendSitsOnTheBorder() {
        LayoutResult r = LayoutTestSupport.layout("<div style='height:20px'></div><fieldset id=f style='margin:0;padding:10px;border:4px solid black'>"
                + "<legend id=l style='padding:0;font:10px monospace'>Title</legend><p id=p style='margin:0'>body</p></fieldset>"
                + "<fieldset id=g style='margin:0;padding:10px;border:4px solid black'><p id=q style='margin:0'>no legend</p></fieldset>", 400);
        LayoutBox f = LayoutTestSupport.box(r, "f");
        LayoutBox l = LayoutTestSupport.box(r, "l");
        LayoutBox p = LayoutTestSupport.box(r, "p");
        Check.near(f.getY() + 2, l.getY() + l.getHeight() / 2, T, "the middle of the legend is on the middle of the top border");
        Check.near(f.getX() + 14, l.getX(), T, "the legend starts at the content edge of the fieldset");
        List<LayoutBox> texts = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(l, LayoutBox.Type.TEXT, texts);
        Check.near(texts.get(0).getWidth(), l.getWidth(), 0.5, "the legend is as wide as its text");
        Check.near(f.getY() + Math.max(l.getHeight(), 4) + 10, p.getY(), T, "the content starts below the legend");
        Check.isTrue(f.getY() >= 20 + (l.getHeight() - 4) / 2 - T, "the part of the legend above the fieldset is not over the content before it");
        Check.notNull(f.getRenderedLegend(), "the box knows its rendered legend");
        LayoutBox g = LayoutTestSupport.box(r, "g");
        Check.isNull(g.getRenderedLegend(), "a fieldset without a legend has none");
        Check.near(g.getY() + 14, LayoutTestSupport.box(r, "q").getY(), T, "and its content starts after the border and the padding");
    }

    /**
     * Checks the width of a text field in a proportional font: (size - 1) times the average character width plus the maximum character width
     * (the algorithm "converting a character width to pixels" of HTML5 10.5.3), and that the width of a drop-down and a list box covers
     * the option groups (their labels and the indentation of the options inside).
     * <p>
     * 비례 글꼴에서 텍스트 필드의 너비를 확인한다: (size - 1) 곱하기 평균 문자 너비에 최대 문자 너비를 더한 값(HTML5 10.5.3 의 "문자 너비를 픽셀로 바꾸기" 알고리즘).
     * 그리고 드롭다운과 리스트 박스의 너비가 optgroup(라벨과 안쪽 option 의 들여쓰기)을 포함하는지 확인한다.
     */
    public void testProportionalFontAndOptionGroups() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a style='font:13px sans-serif;margin:0;padding:0;border:0' size=10>"
                + "<select id=b style='font:10px monospace;margin:0;padding:0;border:0'><optgroup label='Group label'><option>ab</option></optgroup></select>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        double zero = advance(a, "0");
        double max = Math.max(zero, Math.max(advance(a, "W"), advance(a, "M")));
        Check.near(9 * zero + max, a.getWidth(), T, "(size - 1) * avg + max");
        LayoutBox b = LayoutTestSupport.box(r, "b");
        double bold = LayoutTestSupport.advance(b.getFont().deriveFont(java.awt.Font.BOLD), "Group label");
        Check.near(Math.max(bold, advance(b, "ab") + ControlSizes.INDENT) + ControlSizes.SELECT_PAD + ControlSizes.ARROW_WIDTH, b.getWidth(), T,
                "the widest of the group label and the indented option");
    }

    /**
     * Checks that the buttons, check boxes and drop-downs have <code>box-sizing: border-box</code> as the standard says, and that the text
     * fields have <code>content-box</code>.
     * <p>
     * 표준대로 단추, 체크박스, 드롭다운이 <code>box-sizing: border-box</code> 이고 텍스트 필드는 <code>content-box</code> 인지 확인한다.
     */
    public void testBoxSizingOfControls() {
        LayoutResult r = LayoutTestSupport.layout("<input id=a type=submit value=x style='font:10px monospace;margin:0;width:100px;padding:0 10px;border:2px solid'>"
                + "<select id=b style='font:10px monospace;margin:0;width:100px;padding:0 10px;border:2px solid'><option>x</option></select>"
                + "<input id=c style='font:10px monospace;margin:0;width:100px;padding:0 10px;border:2px solid'>", 1000);
        Check.near(100, LayoutTestSupport.box(r, "a").getWidth(), T, "a submit button: the width is the border box width");
        Check.near(100, LayoutTestSupport.box(r, "b").getWidth(), T, "a drop-down: the same");
        Check.near(124, LayoutTestSupport.box(r, "c").getWidth(), T, "a text field: the width is the content width");
    }

    /**
     * Checks the <code>align</code> attribute of a legend: the legend is at the left, the middle or the right of the fieldset, and at the
     * right of a right-to-left fieldset.
     * <p>
     * legend 의 <code>align</code> 속성을 확인한다: legend 가 fieldset 의 왼쪽, 가운데, 오른쪽에 있고, 오른쪽에서 왼쪽 fieldset 에서는 오른쪽에 있다.
     */
    public void testLegendAlign() {
        String set = "<fieldset id=%s style='margin:0;padding:10px;border:4px solid black;width:300px%s'><legend id=%s %s style='padding:0;font:10px monospace'>Title</legend>x</fieldset>";
        LayoutResult r = LayoutTestSupport.layout(String.format(set, "f1", "", "l1", "") + String.format(set, "f2", "", "l2", "align=center")
                + String.format(set, "f3", "", "l3", "align=right") + String.format(set, "f4", ";direction:rtl", "l4", ""), 1000);
        LayoutBox l1 = LayoutTestSupport.box(r, "l1");
        double inner = 300;
        Check.near(LayoutTestSupport.box(r, "f1").getX() + 14, l1.getX(), T, "left by default");
        Check.near(LayoutTestSupport.box(r, "f2").getX() + 14 + (inner - l1.getWidth()) / 2, LayoutTestSupport.box(r, "l2").getX(), 0.6, "center");
        Check.near(LayoutTestSupport.box(r, "f3").getX() + 14 + inner - l1.getWidth(), LayoutTestSupport.box(r, "l3").getX(), 0.6, "right");
        Check.near(LayoutTestSupport.box(r, "f4").getX() + 14 + inner - l1.getWidth(), LayoutTestSupport.box(r, "l4").getX(), 0.6, "right in a right-to-left fieldset");
    }

    /**
     * Checks the baseline of a one-line control: the text of the control lines up with the text next to it.
     * <p>
     * 한 줄 컨트롤의 기준선을 확인한다: 컨트롤의 글이 옆의 글과 같은 줄에 정렬된다.
     */
    public void testBaselineAlignsControlText() {
        LayoutResult r = LayoutTestSupport.layout("<p id=p style='margin:0;font:10px monospace'><span>text </span><input id=a style='font:10px monospace;"
                + "margin:0;padding:4px 2px;border:1px solid' value=x><select id=s style='font:10px monospace;padding:6px 0'><option>q</option></select></p>", 1000);
        LayoutBox a = LayoutTestSupport.box(r, "a");
        List<LayoutBox> texts = new ArrayList<LayoutBox>();
        LayoutTestSupport.collect(LayoutTestSupport.lines(LayoutTestSupport.box(r, "p")).get(0), LayoutBox.Type.TEXT, texts);
        LayoutBox text = texts.get(0);
        Check.near(text.getBaseline(), a.getBaseline(), 0.51, "the baseline of the field is the baseline of the text next to it");
        double content = a.getHeight() - 10;
        double expected = 1 + 4 + (content - (a.ascent + a.descent)) / 2 + a.ascent;
        Check.near(expected, a.getBaseline() - a.getY(), T, "the baseline offset is the text centered in the content box");
        LayoutBox s = LayoutTestSupport.box(r, "s");
        Check.near(text.getBaseline(), s.getBaseline(), 0.51, "a drop-down lines up too");
    }
}
