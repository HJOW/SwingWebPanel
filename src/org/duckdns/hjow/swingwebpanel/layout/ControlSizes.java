package org.duckdns.hjow.swingwebpanel.layout;

import java.awt.Font;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.html.ControlDisplay;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlTextAreaElement;

/**
 * The intrinsic (natural) sizes and baselines of the form controls that the layout treats as replaced elements (<code>input</code>,
 * <code>textarea</code>, <code>select</code>, <code>progress</code>, <code>meter</code>): the size of a text field from its <code>size</code>
 * and the width of the digit zero in its font, a text area from <code>cols</code> and <code>rows</code>, a button from its label, a select
 * from its longest option label. An author's <code>width</code>, <code>height</code>, <code>padding</code>, <code>border</code> and
 * <code>font</code> take precedence over these sizes through the normal replaced element rules. The classes are used on the document thread.
 * <p>
 * 레이아웃이 치환 요소로 다루는 폼 컨트롤(<code>input</code>, <code>textarea</code>, <code>select</code>, <code>progress</code>, <code>meter</code>)의
 * 고유(자연) 크기와 기준선: 텍스트 필드는 <code>size</code> 와 글꼴의 숫자 0 너비로, 텍스트 영역은 <code>cols</code> 와 <code>rows</code> 로, 버튼은
 * 라벨로, select 는 가장 긴 option 라벨로 정한다. 작성자의 <code>width</code>, <code>height</code>, <code>padding</code>, <code>border</code>,
 * <code>font</code> 는 일반 치환 요소 규칙에 따라 이 크기보다 우선한다. 문서 스레드에서 쓴다.
 */
final class ControlSizes {

    /** The size of a check box and a radio button. 체크박스와 라디오 단추의 크기. */
    static final double CHECK_SIZE = 13;

    /** The width reserved for a scroll bar (a text area, a list box). 스크롤 막대를 위해 비워 두는 너비(텍스트 영역, 리스트 박스). */
    static final double SCROLLBAR = 15;

    /** The width of the arrow part of a drop-down select element. 드롭다운 select 의 화살표 부분 너비. */
    static final double ARROW_WIDTH = 18;

    /** The padding left of the text of a drop-down. 드롭다운 글 왼쪽 여백. */
    static final double SELECT_PAD = 4;

    /** The indentation of the options inside an option group. optgroup 안의 option 들여쓰기. */
    static final double INDENT = 12;

    /** The default size of a range control. 슬라이더의 기본 크기. */
    private static final double[] RANGE = { 129, 16 };

    /** The horizontal space between the button and the file name of a file control. 파일 컨트롤의 단추와 파일 이름 사이 간격. */
    static final double FILE_GAP = 6;

    /** The horizontal padding of the button part of a file control. 파일 컨트롤의 단추 부분 가로 안쪽 여백. */
    static final double FILE_BUTTON_PAD = 8;

    /** The text of the button part of a file control. 파일 컨트롤의 단추 부분 글. */
    static final String FILE_BUTTON_TEXT = "Choose File";

    /** The text of a file control without a chosen file. 선택한 파일이 없는 파일 컨트롤의 글. */
    static final String FILE_NONE_TEXT = "No file chosen";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ControlSizes() {
    }

    /**
     * Finds the rendered legend of a fieldset box: the first <code>legend</code> child of the element, if it makes a block box in the normal flow
     * (not a float or an absolutely positioned box). The box then keeps it in {@link LayoutBox#legendChild}.
     * <p>
     * fieldset 상자의 렌더링되는 legend 를 찾는다: 요소의 첫 <code>legend</code> 자식이 정상 흐름의 블록 상자(float 나 절대 배치 상자가 아님)를 만들면 그것이다.
     * 상자는 그것을 {@link LayoutBox#legendChild} 에 기억한다.
     *
     * @param b the box of the element, whose children are built / 요소의 상자. 자식이 만들어져 있다
     * @param e the element / 요소
     */
    static void markLegend(LayoutBox b, Element e) {
        if (!(e instanceof org.duckdns.hjow.swingwebpanel.dom.html.HtmlFieldSetElement)) {
            return;
        }
        Element legend = ((org.duckdns.hjow.swingwebpanel.dom.html.HtmlFieldSetElement) e).getFirstLegend();
        if (legend == null) {
            return;
        }
        // 자식 상자 가운데 그 legend 요소의 정상 흐름 블록 상자를 찾는다.
        for (int i = 0; i < b.children.size(); i++) {
            LayoutBox c = b.children.get(i);
            if (c.node == legend && c.type == LayoutBox.Type.BLOCK && !c.isFloat() && !c.isAbsolutelyPositioned()) {
                b.legendChild = c;
                return;
            }
        }
    }

    /**
     * Gives the box of a form control its font
 (chosen to display the text of the control) and its font metrics, which the renderer reads
     * with {@link LayoutBox#getFont()}. Other elements are left alone.
     * <p>
     * 폼 컨트롤의 상자에 글꼴(컨트롤의 글을 표시할 수 있게 고른 것)과 글꼴 메트릭을 준다. 렌더러가 {@link LayoutBox#getFont()} 로 읽는다. 다른 요소는
     * 건드리지 않는다.
     *
     * @param box the replaced box / 치환 상자
     * @param e   its element / 그 요소
     * @param s   its style / 스타일
     */
    static void prepare(LayoutBox box, Element e, ComputedStyle s) {
        if (ControlDisplay.kind(e) == null) {
            return;
        }
        box.font = FontResolver.resolve(s, ControlDisplay.sample(e));
        double[] m = FontResolver.metrics(box.font);
        box.ascent = m[0];
        box.descent = m[1];
    }

    /**
     * Returns the natural content size of a form control, or null if the element is not one that this class sizes.
     * <p>
     * 폼 컨트롤의 자연 content 크기를 반환한다. 이 클래스가 크기를 정하는 요소가 아니면 null.
     *
     * @param b the replaced box / 치환 상자
     * @param e its element / 그 요소
     * @return array of width and height, or null / 너비와 높이 배열 또는 null
     */
    static double[] natural(LayoutBox b, Element e) {
        String kind = ControlDisplay.kind(e);
        if (kind == null) {
            return null;
        }
        Font f = b.font != null ? b.font : FontResolver.resolve(b.style, ControlDisplay.sample(e));
        double lh = lineHeight(b.style, f);
        double zero = advance(f, "0");
        if ("hidden".equals(kind)) {
            return new double[] { 0, 0 };
        }
        if ("checkbox".equals(kind) || "radio".equals(kind)) {
            return new double[] { CHECK_SIZE, CHECK_SIZE };
        }
        if ("range".equals(kind)) {
            return RANGE.clone();
        }
        if ("color".equals(kind)) {
            return new double[] { 36, 21 };
        }
        if ("file".equals(kind)) {
            double nameWidth = advance(f, fileText(e));
            return new double[] { advance(f, FILE_BUTTON_TEXT) + 2 * FILE_BUTTON_PAD + FILE_GAP + nameWidth, lh + 4 };
        }
        if (ControlDisplay.isPushButton(kind) || "image".equals(kind)) {
            return new double[] { advance(f, ControlDisplay.buttonLabel(e)), lh };
        }
        if (ControlDisplay.TEXTAREA.equals(kind)) {
            HtmlTextAreaElement t = (HtmlTextAreaElement) e;
            double bar = hidesOverflow(b.style) ? 0 : SCROLLBAR;
            return new double[] { t.getCols() * zero + bar, t.getRows() * lh };
        }
        if (ControlDisplay.SELECT.equals(kind)) {
            return selectSize((HtmlSelectElement) e, f, lh);
        }
        if (ControlDisplay.PROGRESS.equals(kind)) {
            double em = Math.max(1, b.style.fontSizePx());
            return new double[] { 10 * em, em };
        }
        if (ControlDisplay.METER.equals(kind)) {
            double em = Math.max(1, b.style.fontSizePx());
            return new double[] { 5 * em, em };
        }
        // 나머지는 한 줄 텍스트 필드이다: size 개의 숫자 0 너비와 한 줄 높이.
        HtmlInputElement in = (HtmlInputElement) e;
        String sample = sampleText(kind);
        double width = sample != null && in.getAttribute("size") == null ? advance(f, sample) + 2 : (in.getSize() - 1) * zero + maxChar(f, zero);
        return new double[] { width, lh };
    }

    /**
     * Returns the sample text whose width gives the width of a date or time field (the formats of those types are fixed), or null for the other
     * text fields, whose width is a number of digits.
     * <p>
     * 날짜/시간 필드의 너비를 정하는 표본 글을 반환한다(그 종류의 형식은 정해져 있다). 너비가 숫자 개수로 정해지는 다른 텍스트 필드이면 null.
     *
     * @param kind the input type / input 종류
     * @return the sample or null / 표본 또는 null
     */
    private static String sampleText(String kind) {
        if ("date".equals(kind)) {
            return "0000-00-00";
        }
        if ("month".equals(kind)) {
            return "0000-00";
        }
        if ("week".equals(kind)) {
            return "0000-W00";
        }
        if ("time".equals(kind)) {
            return "00:00:00";
        }
        return "datetime-local".equals(kind) ? "0000-00-00T00:00" : null;
    }

    /**
     * Returns the width of the widest character of a font as far as it can be measured: the wider of the capital letters W and M and the digit
     * (the maximum character width of the algorithm "converting a character width to pixels" of the HTML standard). For a monospace font it is
     * the width of every character.
     * <p>
     * 글꼴에서 가장 넓은 문자의 너비를 잴 수 있는 만큼 반환한다: 대문자 W 와 M, 숫자 가운데 넓은 것(HTML 표준의 "문자 너비를 픽셀로 바꾸기" 알고리즘의 최대 문자 너비).
     * 고정폭 글꼴에서는 모든 문자의 너비이다.
     *
     * @param f    the font / 글꼴
     * @param zero the width of the digit zero / 숫자 0 의 너비
     * @return the width / 너비
     */
    private static double maxChar(Font f, double zero) {
        return Math.max(zero, Math.max(advance(f, "W"), advance(f, "M")));
    }

    /**
     * Returns the natural size of a select element
: a drop-down is as wide as its longest option label plus the arrow and one line high; a list
     * box is as wide as the longest label (plus a scroll bar if the options do not all fit) and as high as its display size.
     * <p>
     * select 요소의 자연 크기를 반환한다: 드롭다운은 가장 긴 option 라벨에 화살표를 더한 너비와 한 줄 높이, 리스트 박스는 가장 긴 라벨(옵션이 다 보이지 않으면
     * 스크롤 막대 추가)의 너비와 표시 크기 만큼의 높이이다.
     *
     * @param s  the select element / select 요소
     * @param f  the font / 글꼴
     * @param lh the line height / 줄 높이
     * @return array of width and height / 너비와 높이 배열
     */
    private static double[] selectSize(HtmlSelectElement s, Font f, double lh) {
        List<Element> rows = ControlDisplay.rowElements(s);
        double widest = 0;
        Font bold = f.deriveFont(Font.BOLD);
        // 행을 보며 가장 넓은 optgroup 라벨과 가장 넓은 option 라벨(optgroup 안은 들여쓰기 포함)을 찾는다.
        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);
            if (row instanceof org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement) {
                boolean grouped = row.getParentNode() instanceof Element && "optgroup".equals(((Element) row.getParentNode()).getLocalName());
                widest = Math.max(widest, advance(f, ((org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement) row).getLabel()) + (grouped ? INDENT : 0));
            } else {
                String label = row.getAttribute("label");
                widest = Math.max(widest, advance(bold, label == null ? "" : label));
            }
        }
        if (ControlDisplay.isListBox(s)) {
            return new double[] { widest + 8 + SCROLLBAR, s.getDisplaySize() * lh };
        }
        return new double[] { widest + SELECT_PAD + ARROW_WIDTH, lh };
    }

    /**
     * Returns the text that a file control shows next to its button: the names of the chosen files or the text for none.
     * <p>
     * 파일 컨트롤이 단추 옆에 보여 주는 글을 반환한다: 선택한 파일의 이름, 없으면 없다는 글.
     *
     * @param e the input element / input 요소
     * @return the text / 글
     */
    static String fileText(Element e) {
        HtmlInputElement in = (HtmlInputElement) e;
        int n = in.getSelectedFiles().size();
        if (n == 0) {
            return FILE_NONE_TEXT;
        }
        return n == 1 ? in.getSelectedFiles().get(0).getName() : n + " files";
    }

    /**
     * Tells whether the style hides the overflow of a text area (then it does not reserve room for a scroll bar).
     * <p>
     * 스타일이 텍스트 영역의 넘침을 숨기는지 알려 준다(숨기면 스크롤 막대 자리를 비우지 않는다).
     *
     * @param s the style / 스타일
     * @return true if hidden / 숨기면 true
     */
    private static boolean hidesOverflow(ComputedStyle s) {
        String y = Css.kw(s, org.duckdns.hjow.swingwebpanel.css.props.Prop.OVERFLOW_Y);
        return "hidden".equals(y) || "clip".equals(y) || "visible".equals(y);
    }

    /**
     * Returns the baseline of a form control as the distance from the top of its border box, or NaN if the control has none (its bottom
     * margin edge is then the baseline, as for images). A text field, a button, a drop-down and a file control have the baseline of their
     * text; a text area and a list box have the baseline of their first line.
     * <p>
     * 폼 컨트롤의 기준선을 border box 위쪽에서의 거리로 반환한다. 없으면(그러면 아래쪽 margin 가장자리가 기준선이며 이미지와 같다) NaN. 텍스트 필드, 버튼,
     * 드롭다운, 파일 컨트롤은 글의 기준선을, 텍스트 영역과 리스트 박스는 첫 줄의 기준선을 가진다.
     *
     * @param b the replaced box whose size is known / 크기가 정해진 치환 상자
     * @return the baseline or NaN / 기준선 또는 NaN
     */
    static double baseline(LayoutBox b) {
        if (!(b.node instanceof Element) || b.font == null) {
            return Double.NaN;
        }
        Element e = (Element) b.node;
        String kind = ControlDisplay.kind(e);
        if (kind == null) {
            return Double.NaN;
        }
        double top = b.borderTop + b.paddingTop;
        double content = b.height - BlockLayouter.vExtras(b);
        double lh = lineHeight(b.style, b.font);
        double half = (lh - (b.ascent + b.descent)) / 2;
        boolean multiline = ControlDisplay.TEXTAREA.equals(kind) || ControlDisplay.isListBox(e);
        if (multiline) {
            return top + half + b.ascent;
        }
        if (ControlDisplay.isTextField(kind) || ControlDisplay.isPushButton(kind) || ControlDisplay.SELECT.equals(kind)
                || "file".equals(kind) || "image".equals(kind)) {
            // 한 줄 컨트롤은 글을 content 상자 안에서 세로 가운데에 놓는다.
            return top + (content - (b.ascent + b.descent)) / 2 + b.ascent;
        }
        return Double.NaN;
    }

    /**
     * Returns the line height of a style: <code>line-height</code> if it is a length or number, otherwise the height of a line of the font.
     * <p>
     * 스타일의 줄 높이를 반환한다: <code>line-height</code> 가 길이나 숫자이면 그 값, 아니면 글꼴 한 줄의 높이.
     *
     * @param s the style / 스타일
     * @param f the font / 글꼴
     * @return the height in pixels / 높이(px)
     */
    static double lineHeight(ComputedStyle s, Font f) {
        double lh = s.lineHeightPx();
        if (!Double.isNaN(lh)) {
            return lh;
        }
        double[] m = FontResolver.metrics(f);
        return Math.ceil(m[0] + m[1] + m[2]);
    }

    /**
     * Measures a string in a font.
     * <p>
     * 글꼴로 문자열의 너비를 잰다.
     *
     * @param f the font / 글꼴
     * @param s the string / 문자열
     * @return the advance width in pixels / advance 너비(px)
     */
    static double advance(Font f, String s) {
        if (s == null || s.length() == 0) {
            return 0;
        }
        return FontResolver.isComplex(s) ? FontResolver.simpleAdvance(f, s) : f.getStringBounds(s, FontResolver.FRC).getWidth();
    }
}
