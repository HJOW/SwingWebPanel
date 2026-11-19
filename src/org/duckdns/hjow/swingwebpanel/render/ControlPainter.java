package org.duckdns.hjow.swingwebpanel.render;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.LineMetrics;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.html.ControlDisplay;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlMeterElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptGroupElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlProgressElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;

/**
 * Makes the {@link ControlSnapshot} of a form control from its live element and its layout box (on the document thread), and draws a snapshot
 * with Java2D (on the EDT): check boxes, radio buttons, push buttons, the text of text fields and text areas, drop-downs, list boxes, file
 * controls, progress bars, meters, sliders and color wells. The backgrounds and borders that the style of the control asks for are painted
 * by the box painting before; this class draws what is inside the content box.
 * <p>
 * 폼 컨트롤의 살아 있는 요소와 레이아웃 상자에서 {@link ControlSnapshot} 을 만들고(문서 스레드), 스냅샷을 Java2D 로 그린다(EDT): 체크박스, 라디오 단추, 누름
 * 단추, 텍스트 필드와 텍스트 영역의 글, 드롭다운, 리스트 박스, 파일 컨트롤, 진행 막대, 미터, 슬라이더, 색 선택 단추. 컨트롤의 스타일이 요구하는 배경과
 * 경계선은 앞선 상자 그리기가 그리며, 이 클래스는 content box 안을 그린다.
 */
final class ControlPainter {

    /**
     * The colors of the parts that the painter draws itself (a check box, the arrow of a drop-down, a scroll bar, the track of a progress bar ...) for one color scheme
     * (CSS Color Adjustment Level 1: a control follows the <code>color-scheme</code> of its style; the colors of the dark scheme follow the dark appearance of the
     * browser: lighter accent, gray edges, dark fills). The text, the background and the border of the control come from its style, as before.
     * <p>
     * 한 색 구성표에서, 그리는 쪽이 직접 그리는 부분(체크박스, 드롭다운 화살표, 스크롤 막대, 진행 막대의 트랙 등)의 색(CSS Color Adjustment Level 1: 컨트롤은 자신의 스타일의
     * <code>color-scheme</code> 을 따르며 어두운 구성표의 색은 브라우저의 어두운 모양을 따른다: 더 밝은 강조색, 회색 가장자리, 어두운 채움). 컨트롤의 글자, 배경, 테두리는
     * 이전처럼 스타일에서 온다.
     */
    private static final class Palette {
        /** The accent (a checked box, a progress bar, a slider). 강조색. */
        final Color accent;
        /** The accent when the control is disabled. 비활성일 때의 강조색. */
        final Color accentOff;
        /** The border of a box. 상자 테두리. */
        final Color edge;
        /** The border of a disabled box. 비활성 상자 테두리. */
        final Color edgeOff;
        /** The fill of an unchecked box. 체크되지 않은 상자의 채움. */
        final Color boxFill;
        /** The fill of a disabled box. 비활성 상자의 채움. */
        final Color boxFillOff;
        /** The tick inside a checked box. 체크된 상자 안의 표시. */
        final Color tick;
        /** The arrow of a drop-down. 드롭다운의 화살표. */
        final Color arrow;
        /** The track of a scroll bar. 스크롤 막대의 트랙. */
        final Color barTrack;
        /** The thumb of a scroll bar. 스크롤 막대의 손잡이. */
        final Color barThumb;
        /** The fill of the button of a file control. 파일 컨트롤 단추의 채움. */
        final Color buttonFill;
        /** The text of the button of a file control. 파일 컨트롤 단추의 글자. */
        final Color buttonText;
        /** The track of a progress bar. 진행 막대의 트랙. */
        final Color track;
        /** The background of the stripes of an indeterminate progress bar. 불확정 진행 막대의 줄무늬 바탕. */
        final Color stripe;
        /** The outline of a progress bar or a meter. 진행 막대와 미터의 윤곽. */
        final Color outline;
        /** The track of a meter. 미터의 트랙. */
        final Color meterTrack;
        /** The unfilled track of a slider. 슬라이더의 채우지 않은 트랙. */
        final Color rangeTrack;
        /** The border of a color well. 색 선택 단추의 테두리. */
        final Color swatchEdge;
        /** The selection of a list box with the focus. 포커스가 있는 리스트 박스의 선택. */
        final Color selection;
        /** The selection of a list box without the focus. 포커스가 없는 리스트 박스의 선택. */
        final Color selectionIdle;
        /** The text of a disabled row or control. 비활성 행이나 컨트롤의 글자. */
        final Color disabledText;

        /**
         * Creates a palette.
         * <p>
         * 팔레트를 만든다.
         *
         * @param v the colors in the order of the fields (RGB integers) / 필드 순서의 색(RGB 정수)
         */
        Palette(int[] v) {
            accent = new Color(v[0]);
            accentOff = new Color(v[1]);
            edge = new Color(v[2]);
            edgeOff = new Color(v[3]);
            boxFill = new Color(v[4]);
            boxFillOff = new Color(v[5]);
            tick = new Color(v[6]);
            arrow = new Color(v[7]);
            barTrack = new Color(v[8]);
            barThumb = new Color(v[9]);
            buttonFill = new Color(v[10]);
            buttonText = new Color(v[11]);
            track = new Color(v[12]);
            stripe = new Color(v[13]);
            outline = new Color(v[14]);
            meterTrack = new Color(v[15]);
            rangeTrack = new Color(v[16]);
            swatchEdge = new Color(v[17]);
            selection = new Color(v[18]);
            selectionIdle = new Color(v[19]);
            disabledText = new Color(v[20]);
        }
    }

    /** The colors of the light color scheme (the colors that the painter always used). 밝은 색 구성표의 색(그리는 쪽이 늘 쓰던 색). */
    private static final Palette LIGHT = new Palette(new int[] {0x0075FF, 0xA5A5A5, 0x767676, 0xBFBFBF, 0xFFFFFF, 0xF0F0F0, 0xFFFFFF, 0x333333, 0xF0F0F0, 0xC1C1C1,
        0xEFEFEF, 0x000000, 0xE9E9E9, 0xA8CCF8, 0xB0B0B0, 0xEFEFEF, 0xC4C4C4, 0x767676, 0x3584E4, 0xC8C8C8, 0x808080});

    /** The colors of the dark color scheme. 어두운 색 구성표의 색. */
    private static final Palette DARK = new Palette(new int[] {0x99C8FF, 0x6B6B6B, 0x858585, 0x545454, 0x3B3B3B, 0x2B2B2B, 0x101010, 0xE0E0E0, 0x2B2B2B, 0x6B6B6B,
        0x3B3B3B, 0xFFFFFF, 0x3B3B3B, 0x4A6A94, 0x5A5A5A, 0x3B3B3B, 0x5A5A5A, 0x858585, 0x3584E4, 0x505050, 0x7F7F7F});

    /**
     * Returns the palette of the color scheme of a control.
     * <p>
     * 컨트롤의 색 구성표에 맞는 팔레트를 반환한다.
     *
     * @param s the snapshot / 스냅샷
     * @return the palette / 팔레트
     */
    private static Palette palette(ControlSnapshot s) {
        return s.dark ? DARK : LIGHT;
    }

    /** The width of the scroll bar that the list of a list box and a text area draws. 리스트 박스와 텍스트 영역이 그리는 스크롤 막대의 너비. */
    private static final double BAR = 15;

    /** The left padding of the text of a select element. select 의 글 왼쪽 안쪽 여백. */
    static final double SELECT_PAD = 4;

    /** The width of the arrow part of a drop-down. 드롭다운의 화살표 부분 너비. */
    static final double ARROW = 18;

    /** The limit of the characters of a text area that are wrapped (more is cut). 줄 바꿈하는 텍스트 영역 글자 수 한계(더 많으면 자른다). */
    private static final int WRAP_LIMIT = 20000;

    /** The limit of the lines of a text area. 텍스트 영역의 줄 수 한계. */
    private static final int LINE_LIMIT = 2000;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ControlPainter() {
    }

    /**
     * Makes the snapshot of a form control.
     * <p>
     * 폼 컨트롤의 스냅샷을 만든다.
     *
     * @param e the element / 요소
     * @param c the layout box of the element (a replaced box) / 요소의 레이아웃 상자(치환 상자)
     * @return the snapshot, or null if the element is not a control that is drawn here / 스냅샷. 여기서 그리는 컨트롤이 아니면 null
     */
    static ControlSnapshot snapshot(Element e, LayoutBox c) {
        String kind = ControlDisplay.kind(e);
        ControlSnapshot.Kind k = kindOf(e, kind);
        if (k == null) {
            return null;
        }
        ControlSnapshot.Builder b = new ControlSnapshot.Builder();
        ComputedStyle s = c.getStyle();
        b.element = e;
        b.kind = k;
        b.x = c.getContentX();
        b.y = c.getContentY();
        b.w = c.getContentWidth();
        b.h = c.getContentHeight();
        b.borderX = c.getX();
        b.borderY = c.getY();
        b.borderW = c.getWidth();
        b.borderH = c.getHeight();
        b.font = c.getFont() != null ? c.getFont() : new Font("Dialog", Font.PLAIN, 13);
        LineMetrics lm = b.font.getLineMetrics("Ag", TextRenderer.FRC);
        double lh = s == null ? Double.NaN : s.lineHeightPx();
        b.lineHeight = Double.isNaN(lh) ? Math.ceil(lm.getAscent() + lm.getDescent() + lm.getLeading()) : lh;
        b.color = s == null ? 0xFF000000 : s.color(Prop.COLOR);
        // accent-color 가 색이면 체크 표시, 라디오, 진행 막대, 슬라이더의 강조색으로 쓴다(auto 이면 기본 강조색).
        b.accent = s == null || s.colorValue(Prop.ACCENT_COLOR) == null || s.isAuto(Prop.ACCENT_COLOR) ? 0 : s.color(Prop.ACCENT_COLOR);
        if (c.getPlaceholderStyle() != null) {
            b.placeholderColor = c.getPlaceholderStyle().color(Prop.COLOR);
        }
        b.background = s == null ? 0 : s.color(Prop.BACKGROUND_COLOR);
        String align = s == null ? "start" : s.keyword(Prop.TEXT_ALIGN);
        b.textAlign = align == null ? "start" : align;
        // 컨트롤의 스타일이 어두운 색 구성표이면 직접 그리는 부분도 어두운 색을 쓴다.
        b.dark = s != null && s.usesDarkScheme();
        b.disabled = ControlDisplay.isDisabled(e);
        b.readOnly = ControlDisplay.isReadOnly(e);
        b.focused = e.getOwnerDocument() != null && FocusManager.getFocused(e.getOwnerDocument()) == e;
        if (e instanceof HtmlInputElement) {
            b.checked = ((HtmlInputElement) e).isChecked();
            b.indeterminate = ((HtmlInputElement) e).isIndeterminate();
        }
        fill(b, e, kind, k);
        if (k == ControlSnapshot.Kind.FILE && c.getFileButtonStyle() != null) {
            b.fileButton = fileButtonLook(c.getFileButtonStyle(), c.getFileButtonDeclared(), b.font);
        }
        return new ControlSnapshot(b);
    }

    /**
     * Reads the look of the button of a file control from the style of <code>::file-selector-button</code> (CSS Pseudo-Elements 4 section 5.1: any property
     * applies): only the declared properties are taken (background, text color, border width, style and color of the top side, top left radius, the left and
     * right padding, <code>margin-inline-end</code>, the font size and weight, <code>display: none</code> and <code>visibility: hidden</code>); the others keep
     * the look that the painter draws.
     * <p>
     * <code>::file-selector-button</code> 의 스타일에서 파일 컨트롤 단추의 모양을 읽는다(CSS Pseudo-Elements 4 5.1 절: 모든 속성이 적용됨): 선언된 속성(배경, 글자색, 위쪽 border 의
     * 너비·스타일·색, 왼쪽 위 반지름, 왼쪽·오른쪽 padding, <code>margin-inline-end</code>, 글꼴 크기와 굵기, <code>display: none</code>, <code>visibility: hidden</code>)만
     * 가져오고 나머지는 그리는 쪽의 모양을 지킨다.
     *
     * @param ps       the style of the pseudo-element / 의사 요소의 스타일
     * @param declared the declared properties / 선언된 속성
     * @param font     the font of the control / 컨트롤의 글꼴
     * @return the look / 모양
     */
    static ControlSnapshot.FileButton fileButtonLook(ComputedStyle ps, java.util.BitSet declared, Font font) {
        ControlSnapshot.FileButton f = new ControlSnapshot.FileButton();
        java.util.BitSet d = declared == null ? new java.util.BitSet() : declared;
        if (d.get(Prop.BACKGROUND_COLOR)) {
            f.fill = Integer.valueOf(ps.color(Prop.BACKGROUND_COLOR));
        }
        if (d.get(Prop.COLOR)) {
            f.text = Integer.valueOf(ps.color(Prop.COLOR));
        }
        if (d.get(Prop.BORDER_TOP_COLOR)) {
            f.edge = Integer.valueOf(ps.color(Prop.BORDER_TOP_COLOR));
        }
        if (d.get(Prop.BORDER_TOP_STYLE) || d.get(Prop.BORDER_TOP_WIDTH)) {
            // border 스타일이 none 이나 hidden 이면 너비는 0 이다(계산 값 규칙).
            String style = ps.keyword(Prop.BORDER_TOP_STYLE);
            double w = ps.px(Prop.BORDER_TOP_WIDTH);
            f.edgeWidth = "none".equals(style) || "hidden".equals(style) || Double.isNaN(w) ? 0 : w;
        }
        if (d.get(Prop.BORDER_TOP_LEFT_RADIUS)) {
            double r = ps.px(Prop.BORDER_TOP_LEFT_RADIUS);
            f.radius = Double.isNaN(r) ? 0 : r;
        }
        if (d.get(Prop.PADDING_LEFT)) {
            f.padLeft = Math.max(0, ps.px(Prop.PADDING_LEFT));
        }
        if (d.get(Prop.PADDING_RIGHT)) {
            f.padRight = Math.max(0, ps.px(Prop.PADDING_RIGHT));
        }
        if (d.get(Prop.MARGIN_RIGHT)) {
            double m = ps.px(Prop.MARGIN_RIGHT);
            f.gap = Double.isNaN(m) ? 0 : m;
        }
        if (d.get(Prop.FONT_SIZE) || d.get(Prop.FONT_WEIGHT)) {
            // 글꼴 크기나 굵기를 정했으면 컨트롤의 글꼴에서 그것만 바꾼다.
            float size = d.get(Prop.FONT_SIZE) && !Double.isNaN(ps.fontSizePx()) ? (float) ps.fontSizePx() : font.getSize2D();
            int style = d.get(Prop.FONT_WEIGHT) ? (ps.fontWeight() >= 600 ? (font.getStyle() | Font.BOLD) : (font.getStyle() & ~Font.BOLD)) : font.getStyle();
            f.font = font.deriveFont(style, size);
        }
        f.hidden = (d.get(Prop.DISPLAY) && "none".equals(ps.display())) || (d.get(Prop.VISIBILITY) && !"visible".equals(ps.keyword(Prop.VISIBILITY)));
        return f;
    }

    /**
     * Maps the kind of an element to the kind of snapshot.
     * <p>
     * 요소의 종류를 스냅샷 종류로 바꾼다.
     *
     * @param e    the element / 요소
     * @param kind the kind from {@link ControlDisplay#kind(Element)} / {@link ControlDisplay#kind(Element)} 의 종류
     * @return the snapshot kind or null / 스냅샷 종류 또는 null
     */
    private static ControlSnapshot.Kind kindOf(Element e, String kind) {
        if (kind == null || "hidden".equals(kind)) {
            return null;
        }
        if (ControlDisplay.isTextField(kind)) {
            return ControlSnapshot.Kind.TEXT_FIELD;
        }
        if (ControlDisplay.isPushButton(kind)) {
            return ControlSnapshot.Kind.PUSH_BUTTON;
        }
        if (ControlDisplay.TEXTAREA.equals(kind)) {
            return ControlSnapshot.Kind.TEXT_AREA;
        }
        if (ControlDisplay.SELECT.equals(kind)) {
            return ControlDisplay.isListBox(e) ? ControlSnapshot.Kind.LIST_BOX : ControlSnapshot.Kind.DROPDOWN;
        }
        if (ControlDisplay.PROGRESS.equals(kind)) {
            return ControlSnapshot.Kind.PROGRESS;
        }
        if (ControlDisplay.METER.equals(kind)) {
            return ControlSnapshot.Kind.METER;
        }
        if ("checkbox".equals(kind)) {
            return ControlSnapshot.Kind.CHECKBOX;
        }
        if ("radio".equals(kind)) {
            return ControlSnapshot.Kind.RADIO;
        }
        if ("image".equals(kind)) {
            return ControlSnapshot.Kind.IMAGE_BUTTON;
        }
        if ("file".equals(kind)) {
            return ControlSnapshot.Kind.FILE;
        }
        if ("range".equals(kind)) {
            return ControlSnapshot.Kind.RANGE;
        }
        return "color".equals(kind) ? ControlSnapshot.Kind.COLOR : null;
    }

    /**
     * Fills the values that depend on the kind of control.
     * <p>
     * 컨트롤 종류에 따라 달라지는 값을 채운다.
     *
     * @param b    the builder / 빌더
     * @param e    the element / 요소
     * @param kind the element kind / 요소 종류
     * @param k    the snapshot kind / 스냅샷 종류
     */
    private static void fill(ControlSnapshot.Builder b, Element e, String kind, ControlSnapshot.Kind k) {
        switch (k) {
        case TEXT_FIELD:
            b.text = ControlDisplay.fieldText(e);
            b.placeholder = ControlDisplay.placeholder(e);
            break;
        case TEXT_AREA:
            b.text = ControlDisplay.fieldText(e);
            b.placeholder = ControlDisplay.placeholder(e);
            fillLines(b);
            break;
        case PUSH_BUTTON:
        case IMAGE_BUTTON:
            b.text = ControlDisplay.buttonLabel(e);
            break;
        case DROPDOWN:
            b.text = ControlDisplay.selectedLabel(e);
            break;
        case LIST_BOX:
            fillRows(b, (HtmlSelectElement) e);
            break;
        case FILE:
            b.fileText = fileText((HtmlInputElement) e);
            break;
        case PROGRESS:
            b.ratio = ((HtmlProgressElement) e).getPosition();
            break;
        case METER:
            fillMeter(b, (HtmlMeterElement) e);
            break;
        case RANGE:
            b.ratio = rangeRatio((HtmlInputElement) e);
            break;
        case COLOR:
            b.swatch = parseColor(((HtmlInputElement) e).getValue());
            break;
        default:
            break;
        }
    }

    /**
     * Returns the text that a file control shows next to its button.
     * <p>
     * 파일 컨트롤이 단추 옆에 보여 주는 글을 반환한다.
     *
     * @param in the input element / input 요소
     * @return the text / 글
     */
    static String fileText(HtmlInputElement in) {
        int n = in.getSelectedFiles().size();
        if (n == 0) {
            return "No file chosen";
        }
        return n == 1 ? in.getSelectedFiles().get(0).getName() : n + " files";
    }

    /**
     * Wraps the text of a text area into lines that fit its content width; a vertical scroll bar takes some width when the lines do not all fit.
     * <p>
     * 텍스트 영역의 글을 content 너비에 맞는 줄로 나눈다. 줄이 다 들어가지 않으면 세로 스크롤 막대가 너비를 차지한다.
     *
     * @param b the builder / 빌더
     */
    private static void fillLines(ControlSnapshot.Builder b) {
        String text = b.text.length() > WRAP_LIMIT ? b.text.substring(0, WRAP_LIMIT) : b.text;
        boolean noWrap = "off".equalsIgnoreCase(b.element.getAttribute("wrap"));
        String[] lines = noWrap ? text.split("\n", -1) : wrap(text, b.font, b.w);
        // 줄이 content 높이를 넘으면 스크롤 막대가 차지하는 너비를 빼고 다시 나눈다(wrap=off 는 줄 바꿈하지 않는다).
        if (!noWrap && lines.length * b.lineHeight > b.h + 0.5) {
            lines = wrap(text, b.font, Math.max(10, b.w - BAR));
        }
        b.lines = lines;
    }

    /**
     * Splits text at line breaks and wraps every paragraph at spaces (or inside a word that is wider than the width), as
     * <code>white-space: pre-wrap</code> does.
     * <p>
     * 글을 줄 바꿈에서 나누고 문단마다 공백(너비보다 긴 낱말은 낱말 안)에서 줄을 바꾼다(<code>white-space: pre-wrap</code> 과 같다).
     *
     * @param text  the text / 글
     * @param font  the font / 글꼴
     * @param width the width in pixels / 너비(px)
     * @return the lines / 줄
     */
    static String[] wrap(String text, Font font, double width) {
        List<String> out = new ArrayList<String>();
        String[] paragraphs = text.split("\n", -1);
        // 문단마다 너비에 맞게 줄을 나눈다.
        for (int p = 0; p < paragraphs.length && out.size() < LINE_LIMIT; p++) {
            String para = paragraphs[p];
            if (para.length() == 0) {
                out.add("");
                continue;
            }
            int start = 0;
            // 한 줄에 들어가는 만큼 끊어 낸다.
            while (start < para.length() && out.size() < LINE_LIMIT) {
                int i = start;
                int lastBreak = -1;
                // 너비를 넘기 직전까지 글자를 하나씩 더해 가며 마지막 공백 위치를 기억한다.
                while (i < para.length()) {
                    int next = i + Character.charCount(para.codePointAt(i));
                    if (i > start && TextRenderer.advance(font, para.substring(start, next)) > width) {
                        break;
                    }
                    if (para.charAt(i) == ' ') {
                        lastBreak = next;
                    }
                    i = next;
                }
                int end = i >= para.length() ? para.length() : (lastBreak > start ? lastBreak : i);
                out.add(para.substring(start, end));
                start = end;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    /**
     * Collects the rows of a list box and the first row to show so that the first selected row is visible.
     * <p>
     * 리스트 박스의 행과, 처음 선택된 행이 보이도록 처음 보여 줄 행을 모은다.
     *
     * @param b      the builder / 빌더
     * @param select the select element / select 요소
     */
    private static void fillRows(ControlSnapshot.Builder b, HtmlSelectElement select) {
        // 선택 상태가 일관되도록 먼저 option 목록을 만든다.
        select.getOptionElements();
        List<ControlSnapshot.Row> rows = new ArrayList<ControlSnapshot.Row>();
        int firstSelected = -1;
        // select 의 자식을 보며 option 과 optgroup(라벨 행과 그 안의 option)을 차례로 모은다.
        for (Node n = select.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof HtmlOptionElement) {
                firstSelected = addOption(rows, (HtmlOptionElement) n, 0, firstSelected);
            } else if (n instanceof HtmlOptGroupElement) {
                String label = ((Element) n).getAttribute("label");
                rows.add(new ControlSnapshot.Row(label == null ? "" : label, false, true, true, 0));
                // 그룹 안의 option 을 들여 써서 모은다.
                for (Node g = n.getFirstChild(); g != null; g = g.getNextSibling()) {
                    if (g instanceof HtmlOptionElement) {
                        firstSelected = addOption(rows, (HtmlOptionElement) g, 1, firstSelected);
                    }
                }
            }
        }
        b.rows = rows.toArray(new ControlSnapshot.Row[rows.size()]);
        int visible = Math.max(1, (int) Math.floor(b.h / b.lineHeight));
        b.topRow = firstSelected >= visible ? Math.min(firstSelected - visible + 1, Math.max(0, rows.size() - visible)) : 0;
    }

    /**
     * Adds the row of an option.
     * <p>
     * option 의 행을 더한다.
     *
     * @param rows          receives the row / 행을 받는다
     * @param o             the option / option
     * @param indent        the indentation / 들여쓰기
     * @param firstSelected the row index of the first selected option so far / 지금까지 처음 선택된 option 의 행 인덱스
     * @return the row index of the first selected option / 처음 선택된 option 의 행 인덱스
     */
    private static int addOption(List<ControlSnapshot.Row> rows, HtmlOptionElement o, int indent, int firstSelected) {
        int first = firstSelected;
        if (o.isSelected() && first < 0) {
            first = rows.size();
        }
        rows.add(new ControlSnapshot.Row(o.getLabel(), o.isSelected(), o.isDisabled(), false, indent));
        return first;
    }

    /**
     * Decides the fill ratio and the region of a meter.
     * <p>
     * 미터의 채움 비율과 영역을 정한다.
     *
     * @param b the builder / 빌더
     * @param m the meter element / meter 요소
     */
    private static void fillMeter(ControlSnapshot.Builder b, HtmlMeterElement m) {
        double min = m.getMin();
        double max = m.getMax();
        double value = m.getValue();
        double low = m.getLow();
        double high = m.getHigh();
        double optimum = m.getOptimum();
        b.ratio = max > min ? Math.max(0, Math.min(1, (value - min) / (max - min))) : 0;
        // optimum 이 어느 구간에 있는지에 따라 value 가 속한 영역(최적, 차선, 더 나쁨)을 정한다.
        if (optimum < low) {
            b.region = value <= low ? 0 : (value <= high ? 1 : 2);
        } else if (high < optimum) {
            b.region = value >= high ? 0 : (value >= low ? 1 : 2);
        } else {
            b.region = value >= low && value <= high ? 0 : 1;
        }
    }

    /**
     * Returns the position of the thumb of a slider between 0 and 1.
     * <p>
     * 슬라이더 손잡이의 0~1 위치를 반환한다.
     *
     * @param in the input element / input 요소
     * @return the ratio / 비율
     */
    static double rangeRatio(HtmlInputElement in) {
        double min = number(in.getAttribute("min"), 0);
        double max = number(in.getAttribute("max"), 100);
        double v = in.getValueAsNumber();
        if (Double.isNaN(v)) {
            v = max < min ? min : min + (max - min) / 2;
        }
        return max > min ? Math.max(0, Math.min(1, (v - min) / (max - min))) : 0;
    }

    /**
     * Parses a number attribute.
     * <p>
     * 숫자 속성을 해석한다.
     *
     * @param s        the attribute value or null / 속성 값 또는 null
     * @param fallback the value for a missing or invalid attribute / 속성이 없거나 올바르지 않을 때 값
     * @return the number / 숫자
     */
    private static double number(String s, double fallback) {
        if (s == null) {
            return fallback;
        }
        try {
            double d = Double.parseDouble(s.trim());
            return Double.isNaN(d) || Double.isInfinite(d) ? fallback : d;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    /**
     * Parses the <code>#rrggbb</code> value of a color control.
     * <p>
     * 색 컨트롤의 <code>#rrggbb</code> 값을 해석한다.
     *
     * @param v the value / 값
     * @return the opaque ARGB color, black if the value is not valid / 불투명 ARGB 색. 올바르지 않으면 검정
     */
    private static int parseColor(String v) {
        if (v != null && v.length() == 7 && v.charAt(0) == '#') {
            try {
                return 0xFF000000 | Integer.parseInt(v.substring(1), 16);
            } catch (NumberFormatException ex) {
                return 0xFF000000;
            }
        }
        return 0xFF000000;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 그리기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Draws a control into its content box. The state of the graphics object (clip, color, font, stroke, antialiasing) is restored.
     * <p>
     * 컨트롤을 content box 안에 그린다. 그래픽스 객체의 상태(클립, 색, 글꼴, 획, 안티앨리어싱)는 복구된다.
     *
     * @param g the graphics in document coordinates / 문서 좌표의 그래픽스
     * @param s the snapshot / 스냅샷
     */
    static void paint(Graphics2D g, ControlSnapshot s) {
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        Shape clip = g.getClip();
        java.awt.Stroke stroke = g.getStroke();
        Font font = g.getFont();
        Color color = g.getColor();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            switch (s.kind) {
            case CHECKBOX:
                box(g, s, false);
                break;
            case RADIO:
                box(g, s, true);
                break;
            case PUSH_BUTTON:
            case IMAGE_BUTTON:
                label(g, s);
                break;
            case TEXT_FIELD:
                textField(g, s);
                break;
            case TEXT_AREA:
                textArea(g, s);
                break;
            case DROPDOWN:
                dropdown(g, s);
                break;
            case LIST_BOX:
                listBox(g, s);
                break;
            case FILE:
                file(g, s);
                break;
            case PROGRESS:
                progress(g, s);
                break;
            case METER:
                meter(g, s);
                break;
            case RANGE:
                range(g, s);
                break;
            case COLOR:
                swatch(g, s);
                break;
            default:
                break;
            }
        } finally {
            g.setClip(clip);
            g.setStroke(stroke);
            g.setFont(font);
            g.setColor(color);
            if (aa != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
            }
        }
    }

    /**
     * Returns the baseline that centers a line of the font vertically in a box.
     * <p>
     * 글꼴 한 줄을 상자 안에서 세로 가운데에 놓는 기준선을 반환한다.
     *
     * @param font   the font / 글꼴
     * @param top    the top of the box / 상자 위쪽
     * @param height the height of the box / 상자 높이
     * @return the baseline y / 기준선 y
     */
    private static double centeredBaseline(Font font, double top, double height) {
        LineMetrics lm = font.getLineMetrics("Ag", TextRenderer.FRC);
        return top + (height - (lm.getAscent() + lm.getDescent())) / 2 + lm.getAscent();
    }

    /**
     * Returns the left of a text with the given advance in a box according to <code>text-align</code>; text wider than the box starts at the
     * left.
     * <p>
     * <code>text-align</code> 에 따라 상자 안에서 주어진 너비의 글이 시작하는 왼쪽을 반환한다. 상자보다 넓은 글은 왼쪽에서 시작한다.
     *
     * @param align the keyword / 키워드
     * @param left  the left of the box / 상자 왼쪽
     * @param width the width of the box / 상자 너비
     * @param tw    the width of the text / 글 너비
     * @return the x of the text / 글의 x
     */
    private static double alignedX(String align, double left, double width, double tw) {
        if (tw >= width) {
            return left;
        }
        if ("center".equals(align) || "-webkit-center".equals(align) || "-moz-center".equals(align)) {
            return left + (width - tw) / 2;
        }
        return "right".equals(align) || "end".equals(align) ? left + width - tw : left;
    }

    /**
     * Draws the label of a push button or an image button, centered.
     * <p>
     * 누름 단추나 이미지 단추의 라벨을 가운데에 그린다.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void label(Graphics2D g, ControlSnapshot s) {
        g.clip(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
        g.setColor(new Color(s.color, true));
        double tw = TextRenderer.advance(s.font, s.text);
        double x = s.kind == ControlSnapshot.Kind.IMAGE_BUTTON ? s.x : s.x + Math.max(0, (s.w - tw) / 2);
        TextRenderer.draw(g, s.text, s.font, x, centeredBaseline(s.font, s.y, s.h), 0, 0, 0);
    }

    /**
     * Returns the accent color of a control: the computed <code>accent-color</code> if it is a color, otherwise the default accent.
     * <p>
     * 컨트롤의 강조색을 반환한다: 계산된 <code>accent-color</code> 가 색이면 그것, 아니면 기본 강조색.
     *
     * @param s the snapshot / 스냅샷
     * @return the accent color / 강조색
     */
    private static Color accentOf(ControlSnapshot s) {
        return s.accent != 0 ? new Color(s.accent, true) : palette(s).accent;
    }

    /**
     * Draws the value (or the placeholder) of a one-line text field.
     * <p>
     * 한 줄 텍스트 필드의 값(또는 placeholder)을 그린다.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void textField(Graphics2D g, ControlSnapshot s) {
        g.clip(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
        boolean empty = s.text.length() == 0;
        String shown = empty ? s.placeholder : s.text;
        if (shown.length() == 0) {
            return;
        }
        g.setColor(empty ? new Color(s.placeholderColor, true) : new Color(s.color, true));
        double tw = TextRenderer.advance(s.font, shown);
        TextRenderer.draw(g, shown, s.font, alignedX(s.textAlign, s.x, s.w, tw), centeredBaseline(s.font, s.y, s.h), 0, 0, 0);
    }

    /**
     * Draws the wrapped lines of a text area and a scroll bar if they do not all fit.
     * <p>
     * 텍스트 영역의 줄 바꿈한 줄들을 그리고, 다 들어가지 않으면 스크롤 막대를 그린다.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void textArea(Graphics2D g, ControlSnapshot s) {
        g.clip(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
        boolean empty = s.text.length() == 0;
        String[] lines = empty ? wrap(s.placeholder, s.font, s.w) : s.lines;
        g.setColor(empty ? new Color(s.placeholderColor, true) : new Color(s.color, true));
        LineMetrics lm = s.font.getLineMetrics("Ag", TextRenderer.FRC);
        double half = (s.lineHeight - (lm.getAscent() + lm.getDescent())) / 2;
        // 보이는 만큼의 줄을 위에서 아래로 그린다.
        for (int i = 0; i < lines.length; i++) {
            double top = s.y + i * s.lineHeight;
            if (top >= s.y + s.h) {
                break;
            }
            TextRenderer.draw(g, lines[i], s.font, s.x, top + half + lm.getAscent(), 0, 0, 0);
        }
        if (!empty && lines.length * s.lineHeight > s.h + 0.5) {
            scrollBar(g, palette(s), s.x + s.w - BAR, s.y, BAR, s.h, 0, Math.min(1, s.h / (lines.length * s.lineHeight)));
        }
    }

    /**
     * Draws a scroll bar with a thumb.
     * <p>
     * 손잡이가 있는 스크롤 막대를 그린다.
     *
     * @param g     the graphics / 그래픽스
     * @param p     the palette / 팔레트
     * @param x     the left / 왼쪽
     * @param y     the top / 위쪽
     * @param w     the width / 너비
     * @param h     the height / 높이
     * @param start the start of the thumb as a fraction of the track / 손잡이 시작(트랙에 대한 비율)
     * @param size  the size of the thumb as a fraction of the track / 손잡이 크기(트랙에 대한 비율)
     */
    private static void scrollBar(Graphics2D g, Palette p, double x, double y, double w, double h, double start, double size) {
        g.setColor(p.barTrack);
        g.fill(new Rectangle2D.Double(x, y, w, h));
        g.setColor(p.barThumb);
        g.fill(new Rectangle2D.Double(x + 3, y + 2 + start * (h - 4), w - 6, Math.max(8, size * (h - 4))));
    }

    /**
     * Draws a check box (a rounded square) or a radio button (a circle).
     * <p>
     * 체크박스(둥근 사각형)나 라디오 단추(원)를 그린다.
     *
     * @param g     the graphics / 그래픽스
     * @param s     the snapshot / 스냅샷
     * @param round true for a radio button / 라디오 단추이면 true
     */
    private static void box(Graphics2D g, ControlSnapshot s, boolean round) {
        double d = Math.max(1, Math.min(s.w, s.h));
        double x = s.x + (s.w - d) / 2;
        double y = s.y + (s.h - d) / 2;
        boolean on = s.checked || (!round && s.indeterminate);
        Palette p = palette(s);
        Color edge = s.disabled ? p.edgeOff : (on && !round ? accentOf(s) : p.edge);
        Color fill = s.disabled ? p.boxFillOff : p.boxFill;
        Color accent = s.disabled ? p.accentOff : accentOf(s);
        g.setStroke(new BasicStroke(1f));
        Shape shape = round ? new Ellipse2D.Double(x + 0.5, y + 0.5, d - 1, d - 1)
                : new RoundRectangle2D.Double(x + 0.5, y + 0.5, d - 1, d - 1, 3, 3);
        if (on && !round) {
            g.setColor(accent);
            g.fill(shape);
            g.setColor(p.tick);
            g.setStroke(new BasicStroke((float) Math.max(1.5, d / 7), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (s.indeterminate && !s.checked) {
                g.draw(new java.awt.geom.Line2D.Double(x + d * 0.25, y + d / 2, x + d * 0.75, y + d / 2));
            } else {
                GeneralPath tick = new GeneralPath();
                tick.moveTo((float) (x + d * 0.24), (float) (y + d * 0.52));
                tick.lineTo((float) (x + d * 0.43), (float) (y + d * 0.71));
                tick.lineTo((float) (x + d * 0.77), (float) (y + d * 0.30));
                g.draw(tick);
            }
            return;
        }
        g.setColor(fill);
        g.fill(shape);
        g.setColor(round && s.checked ? accent : edge);
        g.draw(shape);
        if (round && s.checked) {
            double r = d * 0.27;
            g.setColor(accent);
            g.fill(new Ellipse2D.Double(x + d / 2 - r, y + d / 2 - r, 2 * r, 2 * r));
        }
    }

    /**
     * Draws a drop-down select element: the selected label and the arrow.
     * <p>
     * 드롭다운 select 를 그린다: 선택된 라벨과 화살표.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void dropdown(Graphics2D g, ControlSnapshot s) {
        g.clip(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
        g.setColor(new Color(s.color, true));
        TextRenderer.draw(g, s.text, s.font, s.x + SELECT_PAD, centeredBaseline(s.font, s.y, s.h), 0, 0, 0);
        double cx = s.x + s.w - ARROW / 2;
        double cy = s.y + s.h / 2;
        g.setColor(s.disabled ? palette(s).edgeOff : palette(s).arrow);
        GeneralPath arrow = new GeneralPath();
        arrow.moveTo((float) (cx - 4), (float) (cy - 2));
        arrow.lineTo((float) (cx + 4), (float) (cy - 2));
        arrow.lineTo((float) cx, (float) (cy + 2.5));
        arrow.closePath();
        g.fill(arrow);
    }

    /**
     * Draws the rows of a list box that fit its height, with the selection highlighted.
     * <p>
     * 리스트 박스의 높이에 들어가는 행을 그리고 선택을 강조한다.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void listBox(Graphics2D g, ControlSnapshot s) {
        g.clip(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
        int visible = Math.max(1, (int) Math.ceil(s.h / s.lineHeight));
        boolean bar = s.rows.length * s.lineHeight > s.h + 0.5;
        double right = s.x + s.w - (bar ? BAR : 0);
        Font bold = s.font.deriveFont(Font.BOLD);
        // 처음 보이는 행부터 높이에 들어가는 만큼 한 행씩 그린다.
        for (int i = 0; i < visible && s.topRow + i < s.rows.length; i++) {
            ControlSnapshot.Row row = s.rows[s.topRow + i];
            double top = s.y + i * s.lineHeight;
            Color text = new Color(s.color, true);
            if (row.isSelected()) {
                g.setColor(s.focused ? palette(s).selection : palette(s).selectionIdle);
                g.fill(new Rectangle2D.Double(s.x, top, right - s.x, s.lineHeight));
                text = s.focused ? Color.WHITE : text;
            }
            if (row.isDisabled() && !row.isGroup() || s.disabled) {
                text = palette(s).disabledText;
            }
            g.setColor(text);
            TextRenderer.draw(g, row.getText(), row.isGroup() ? bold : s.font, s.x + SELECT_PAD + row.getIndent() * 12,
                    centeredBaseline(s.font, top, s.lineHeight), 0, 0, 0);
        }
        if (bar) {
            scrollBar(g, palette(s), s.x + s.w - BAR, s.y, BAR, s.h, (double) s.topRow / s.rows.length, Math.min(1, (double) visible / s.rows.length));
        }
    }

    /**
     * Draws a file control: a button with a fixed label and the name of the chosen file.
     * <p>
     * 파일 컨트롤을 그린다: 고정된 라벨의 단추와 선택한 파일의 이름.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void file(Graphics2D g, ControlSnapshot s) {
        g.clip(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
        String buttonText = "Choose File";
        double baseline = centeredBaseline(s.font, s.y, s.h);
        if (s.fileButton == null) {
            // ::file-selector-button 규칙이 없으면 그리는 쪽의 모양으로 단추를 그린다.
            double pad = 8;
            double bw = TextRenderer.advance(s.font, buttonText) + 2 * pad;
            RoundRectangle2D.Double button = new RoundRectangle2D.Double(s.x + 0.5, s.y + 0.5, bw - 1, s.h - 1, 3, 3);
            g.setColor(palette(s).buttonFill);
            g.fill(button);
            g.setColor(s.disabled ? palette(s).edgeOff : palette(s).edge);
            g.setStroke(new BasicStroke(1f));
            g.draw(button);
            g.setColor(s.disabled ? palette(s).disabledText : palette(s).buttonText);
            TextRenderer.draw(g, buttonText, s.font, s.x + pad, baseline, 0, 0, 0);
            g.setColor(s.disabled ? palette(s).disabledText : new Color(s.color, true));
            TextRenderer.draw(g, s.fileText, s.font, s.x + bw + 6, baseline, 0, 0, 0);
            return;
        }
        // ::file-selector-button 이 정한 값은 그것을, 정하지 않은 값은 그리는 쪽의 모양을 쓴다(CSS Pseudo-Elements 4 5.1 절).
        ControlSnapshot.FileButton look = s.fileButton;
        double textX = s.x;
        if (!look.hidden) {
            Font labelFont = look.font != null ? look.font : s.font;
            double padL = Double.isNaN(look.padLeft) ? 8 : look.padLeft;
            double padR = Double.isNaN(look.padRight) ? 8 : look.padRight;
            double edgeW = Double.isNaN(look.edgeWidth) ? 1 : look.edgeWidth;
            double radius = Double.isNaN(look.radius) ? 3 : look.radius;
            double bw = TextRenderer.advance(labelFont, buttonText) + padL + padR + (Double.isNaN(look.edgeWidth) ? 0 : 2 * edgeW);
            RoundRectangle2D.Double outer = new RoundRectangle2D.Double(s.x, s.y, bw, s.h, 2 * radius, 2 * radius);
            g.setColor(look.fill != null ? new Color(look.fill.intValue(), true) : palette(s).buttonFill);
            g.fill(outer);
            if (edgeW > 0) {
                Color edge = look.edge != null ? new Color(look.edge.intValue(), true) : (s.disabled ? palette(s).edgeOff : palette(s).edge);
                g.setColor(edge);
                g.setStroke(new BasicStroke((float) edgeW));
                g.draw(new RoundRectangle2D.Double(s.x + edgeW / 2, s.y + edgeW / 2, bw - edgeW, s.h - edgeW, 2 * radius, 2 * radius));
            }
            g.setColor(look.text != null ? new Color(look.text.intValue(), true) : (s.disabled ? palette(s).disabledText : palette(s).buttonText));
            TextRenderer.draw(g, buttonText, labelFont, s.x + padL + (Double.isNaN(look.edgeWidth) ? 0 : edgeW), centeredBaseline(labelFont, s.y, s.h), 0, 0, 0);
            textX = s.x + bw + (Double.isNaN(look.gap) ? 6 : look.gap);
        }
        g.setColor(s.disabled ? palette(s).disabledText : new Color(s.color, true));
        TextRenderer.draw(g, s.fileText, s.font, textX, baseline, 0, 0, 0);
    }

    /**
     * Draws a progress bar: the track and the filled part (a striped bar if the progress is indeterminate).
     * <p>
     * 진행 막대를 그린다: 트랙과 채운 부분(불확정이면 줄무늬 막대).
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void progress(Graphics2D g, ControlSnapshot s) {
        RoundRectangle2D.Double track = new RoundRectangle2D.Double(s.x + 0.5, s.y + 0.5, s.w - 1, s.h - 1, 4, 4);
        g.setColor(palette(s).track);
        g.fill(track);
        g.clip(track);
        Color fill = accentOf(s);
        if (s.ratio < 0) {
            // 불확정 진행 막대는 줄무늬로 채운다.
            g.setColor(palette(s).stripe);
            g.fill(new Rectangle2D.Double(s.x, s.y, s.w, s.h));
            g.setColor(fill);
            // 대각선 줄무늬를 일정 간격으로 그린다.
            for (double x = s.x - s.h; x < s.x + s.w; x += s.h) {
                GeneralPath stripe = new GeneralPath();
                stripe.moveTo((float) x, (float) (s.y + s.h));
                stripe.lineTo((float) (x + s.h / 2), (float) (s.y + s.h));
                stripe.lineTo((float) (x + s.h), (float) s.y);
                stripe.lineTo((float) (x + s.h / 2), (float) s.y);
                stripe.closePath();
                g.fill(stripe);
            }
        } else {
            g.setColor(fill);
            g.fill(new Rectangle2D.Double(s.x, s.y, s.w * s.ratio, s.h));
        }
        g.setClip(null);
        g.setColor(palette(s).outline);
        g.draw(track);
    }

    /**
     * Draws a meter: the track and the filled part in the color of its region (green, yellow, red).
     * <p>
     * 미터를 그린다: 트랙과 영역의 색(초록, 노랑, 빨강)으로 채운 부분.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void meter(Graphics2D g, ControlSnapshot s) {
        RoundRectangle2D.Double track = new RoundRectangle2D.Double(s.x + 0.5, s.y + 0.5, s.w - 1, s.h - 1, 4, 4);
        g.setColor(palette(s).meterTrack);
        g.fill(track);
        g.clip(track);
        g.setColor(s.region == 0 ? new Color(0x2FA84F) : (s.region == 1 ? new Color(0xF2C318) : new Color(0xDD4B39)));
        g.fill(new Rectangle2D.Double(s.x, s.y, s.w * s.ratio, s.h));
        g.setClip(null);
        g.setColor(palette(s).outline);
        g.draw(track);
    }

    /**
     * Draws a slider: the track, the part up to the thumb and the thumb.
     * <p>
     * 슬라이더를 그린다: 트랙, 손잡이까지의 부분, 손잡이.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void range(Graphics2D g, ControlSnapshot s) {
        double thumb = Math.min(s.h, 15);
        double trackY = s.y + s.h / 2 - 2;
        double left = s.x + thumb / 2;
        double span = Math.max(0, s.w - thumb);
        double at = left + span * s.ratio;
        Color accent = s.disabled ? palette(s).accentOff : accentOf(s);
        g.setColor(palette(s).rangeTrack);
        g.fill(new RoundRectangle2D.Double(left, trackY, span, 4, 4, 4));
        g.setColor(accent);
        g.fill(new RoundRectangle2D.Double(left, trackY, at - left, 4, 4, 4));
        g.fill(new Ellipse2D.Double(at - thumb / 2, s.y + (s.h - thumb) / 2, thumb, thumb));
    }

    /**
     * Draws the color of a color well.
     * <p>
     * 색 선택 단추의 색을 그린다.
     *
     * @param g the graphics / 그래픽스
     * @param s the snapshot / 스냅샷
     */
    private static void swatch(Graphics2D g, ControlSnapshot s) {
        Rectangle2D.Double r = new Rectangle2D.Double(s.x + 0.5, s.y + 0.5, s.w - 1, s.h - 1);
        g.setColor(new Color(s.swatch, true));
        g.fill(r);
        g.setColor(palette(s).swatchEdge);
        g.setStroke(new BasicStroke(1f));
        g.draw(r);
    }
}
