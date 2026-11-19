package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Font;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * An immutable copy of everything that is needed to draw a form control or to put an editor over it: the kind of control, its boxes in
 * document coordinates, its font and colors, and its state (the shown text, checked, disabled, the rows of a list ...). The display list
 * builder makes it on the document thread; the EDT reads it when it paints the control or places the Swing editor of the text controls.
 * <p>
 * 폼 컨트롤을 그리거나 그 위에 편집기를 놓는 데 필요한 모든 것의 불변 복사본: 컨트롤의 종류, 문서 좌표의 상자, 글꼴과 색, 상태(보이는 글, 체크, 비활성,
 * 목록의 행 등). 디스플레이 리스트 빌더가 문서 스레드에서 만들고, EDT 가 컨트롤을 그리거나 텍스트 컨트롤의 Swing 편집기를 놓을 때 읽는다.
 */
public final class ControlSnapshot {

    /**
     * The kinds of drawn control.
     * <p>
     * 그려지는 컨트롤의 종류.
     */
    public enum Kind {
        /** A one-line text field (text, password, search, number, date ...). 한 줄 텍스트 필드. */
        TEXT_FIELD,
        /** A multi-line text area. 여러 줄 텍스트 영역. */
        TEXT_AREA,
        /** A check box. 체크박스. */
        CHECKBOX,
        /** A radio button. 라디오 단추. */
        RADIO,
        /** A push button (button, submit, reset). 누름 단추. */
        PUSH_BUTTON,
        /** An image button without a usable image (its alternative text). 쓸 수 있는 이미지가 없는 이미지 단추(대체 글). */
        IMAGE_BUTTON,
        /** A file upload control. 파일 업로드 컨트롤. */
        FILE,
        /** A drop-down select element. 드롭다운 select. */
        DROPDOWN,
        /** A list box (a select element that shows several rows). 리스트 박스(여러 행을 보여 주는 select). */
        LIST_BOX,
        /** A progress bar. 진행 막대. */
        PROGRESS,
        /** A meter that shows a value in a range. 범위 안의 값을 보여 주는 미터. */
        METER,
        /** A slider that picks a value in a range. 범위 안의 값을 고르는 슬라이더. */
        RANGE,
        /** A color well. 색 선택 단추. */
        COLOR
    }

    /**
     * One row of a list box.
     * <p>
     * 리스트 박스의 한 행.
     */
    public static final class Row {

        /** The text of the row. 행의 글. */
        private final String text;

        /** True if the row is selected. 선택된 행이면 true. */
        private final boolean selected;

        /** True if the option is disabled. option 이 비활성이면 true. */
        private final boolean disabled;

        /** True if the row is the label of an option group. 행이 optgroup 의 라벨이면 true. */
        private final boolean group;

        /** The indentation level (1 inside an option group). 들여쓰기 수준(optgroup 안은 1). */
        private final int indent;

        /**
         * Creates a row.
         * <p>
         * 행을 만든다.
         *
         * @param text     the text / 글
         * @param selected whether selected / 선택 여부
         * @param disabled whether disabled / 비활성 여부
         * @param group    whether it is a group label / 그룹 라벨 여부
         * @param indent   the indentation level / 들여쓰기 수준
         */
        public Row(String text, boolean selected, boolean disabled, boolean group, int indent) {
            this.text = text;
            this.selected = selected;
            this.disabled = disabled;
            this.group = group;
            this.indent = indent;
        }

        /**
         * Returns the text.
         * <p>
         * 글을 반환한다.
         *
         * @return the text / 글
         */
        public String getText() {
            return text;
        }

        /**
         * Tells whether the row is selected.
         * <p>
         * 행이 선택되었는지 알려 준다.
         *
         * @return true if selected / 선택되었으면 true
         */
        public boolean isSelected() {
            return selected;
        }

        /**
         * Tells whether the option is disabled.
         * <p>
         * option 이 비활성인지 알려 준다.
         *
         * @return true if disabled / 비활성이면 true
         */
        public boolean isDisabled() {
            return disabled;
        }

        /**
         * Tells whether the row is the label of an option group.
         * <p>
         * 행이 optgroup 라벨인지 알려 준다.
         *
         * @return true for a group label / 그룹 라벨이면 true
         */
        public boolean isGroup() {
            return group;
        }

        /**
         * Returns the indentation level.
         * <p>
         * 들여쓰기 수준을 반환한다.
         *
         * @return 0 or 1 / 0 또는 1
         */
        public int getIndent() {
            return indent;
        }
    }

    /** The element. 요소. */
    final Element element;

    /** The kind. 종류. */
    final Kind kind;

    /** Content box left in document coordinates. 문서 좌표의 content box 왼쪽. */
    final double x;

    /** Content box top. content box 위쪽. */
    final double y;

    /** Content box width. content box 너비. */
    final double w;

    /** Content box height. content box 높이. */
    final double h;

    /** Border box left. border box 왼쪽. */
    final double borderX;

    /** Border box top. border box 위쪽. */
    final double borderY;

    /** Border box width. border box 너비. */
    final double borderW;

    /** Border box height. border box 높이. */
    final double borderH;

    /** The font. 글꼴. */
    final Font font;

    /** The text color (ARGB). 글자색(ARGB). */
    final int color;

    /** The background color (ARGB, 0 if none). 배경색(ARGB, 없으면 0). */
    final int background;

    /** The computed <code>text-align</code>. 계산된 <code>text-align</code>. */
    final String textAlign;

    /** The height of one line. 한 줄의 높이. */
    final double lineHeight;

    /** True if the control is disabled. 컨트롤이 비활성이면 true. */
    final boolean disabled;

    /** True if the control is read-only. 컨트롤이 읽기 전용이면 true. */
    final boolean readOnly;

    /** True if the control uses the dark color scheme (<code>color-scheme: dark</code>), so that the parts that the painter draws itself get the dark colors. 컨트롤이 어두운 색 구성표(<code>color-scheme: dark</code>)를 쓰면 true. 그리는 쪽이 직접 그리는 부분이 어두운 색을 갖는다. */
    final boolean dark;

    /** True if a check box or radio button is checked. 체크박스나 라디오 단추가 체크되었으면 true. */
    final boolean checked;

    /** True if a check box is indeterminate. 체크박스가 indeterminate 이면 true. */
    final boolean indeterminate;

    /** True if the control has the focus (a list box shows its selection in the highlight color then). 컨트롤이 포커스를 가졌으면 true. */
    final boolean focused;

    /** The text that the control shows: the value (bullets for a password), the label of a button, the selected option of a drop-down. 컨트롤이 보여 주는 글. */
    final String text;

    /** The placeholder shown while the text is empty. 글이 비어 있을 때 보이는 placeholder. */
    final String placeholder;

    /** The color of the placeholder from <code>::placeholder</code> (ARGB). <code>::placeholder</code> 에서 온 placeholder 의 색(ARGB). */
    final int placeholderColor;

    /** The accent color from <code>accent-color</code> (ARGB), 0 for <code>auto</code>. <code>accent-color</code> 의 강조색(ARGB). <code>auto</code> 이면 0. */
    final int accent;

    /** The wrapped lines of a text area. 텍스트 영역의 줄 바꿈한 줄들. */
    final String[] lines;

    /** The rows of a list box. 리스트 박스의 행. */
    final Row[] rows;

    /** The index of the first visible row of a list box. 리스트 박스에서 처음 보이는 행의 인덱스. */
    final int topRow;

    /** The position of a progress bar or slider between 0 and 1, or -1 if the bar is indeterminate. 진행 막대나 슬라이더의 0~1 위치. 불확정이면 -1. */
    final double ratio;

    /** The region of a meter: 0 optimum, 1 suboptimal, 2 even less good. 미터의 영역: 0 최적, 1 차선, 2 더 나쁨. */
    final int region;

    /** The color of a color well (ARGB). 색 선택 단추의 색(ARGB). */
    final int swatch;

    /** The text next to the button of a file control. 파일 컨트롤에서 단추 옆의 글. */
    final String fileText;

    /** What <code>::file-selector-button</code> sets for the button of a file control, or null. 파일 컨트롤의 단추에 대해 <code>::file-selector-button</code> 이 정한 것. 없으면 null. */
    final FileButton fileButton;

    /**
     * The look of the button of a file control that the author set with <code>::file-selector-button</code> (CSS Pseudo-Elements 4 section 5.1): every value
     * that is null or NaN was not declared, and the painter uses its own look for it.
     * <p>
     * 작성자가 <code>::file-selector-button</code> 으로 정한 파일 컨트롤 단추의 모양(CSS Pseudo-Elements 4 5.1 절): null 이거나 NaN 인 값은 선언되지 않은 것이며 그리는 쪽은
     * 자신의 모양을 쓴다.
     */
    static final class FileButton {
        /** The background color (ARGB) or null. 배경색(ARGB) 또는 null. */
        Integer fill;
        /** The text color (ARGB) or null. 글자색(ARGB) 또는 null. */
        Integer text;
        /** The border color (ARGB) or null. border 색(ARGB) 또는 null. */
        Integer edge;
        /** The border width (0 for <code>border-style: none</code>) or NaN. border 너비(<code>border-style: none</code> 이면 0) 또는 NaN. */
        double edgeWidth = Double.NaN;
        /** The corner radius or NaN. 모서리 반지름 또는 NaN. */
        double radius = Double.NaN;
        /** The left padding or NaN. 왼쪽 padding 또는 NaN. */
        double padLeft = Double.NaN;
        /** The right padding or NaN. 오른쪽 padding 또는 NaN. */
        double padRight = Double.NaN;
        /** The space after the button (<code>margin-inline-end</code>) or NaN. 단추 뒤의 간격(<code>margin-inline-end</code>) 또는 NaN. */
        double gap = Double.NaN;
        /** The font of the label, or null for the font of the control. 라벨의 글꼴. 컨트롤의 글꼴이면 null. */
        Font font;
        /** True if the button is not shown (<code>display: none</code> or <code>visibility: hidden</code>). 단추를 보이지 않으면 true. */
        boolean hidden;

        /**
         * Creates an empty look (nothing declared).
         * <p>
         * 빈 모양(아무것도 선언되지 않음)을 만든다.
         */
        FileButton() {
        }
    }

    /**
     * Creates a snapshot; see {@link ControlPainter#snapshot} for the use.
     * <p>
     * 스냅샷을 만든다. 쓰임은 {@link ControlPainter#snapshot} 참조.
     *
     * @param b the filled builder / 채워진 빌더
     */
    ControlSnapshot(Builder b) {
        element = b.element;
        kind = b.kind;
        x = b.x;
        y = b.y;
        w = b.w;
        h = b.h;
        borderX = b.borderX;
        borderY = b.borderY;
        borderW = b.borderW;
        borderH = b.borderH;
        font = b.font;
        color = b.color;
        background = b.background;
        textAlign = b.textAlign;
        lineHeight = b.lineHeight;
        disabled = b.disabled;
        readOnly = b.readOnly;
        dark = b.dark;
        checked = b.checked;
        indeterminate = b.indeterminate;
        focused = b.focused;
        text = b.text;
        placeholder = b.placeholder;
        placeholderColor = b.placeholderColor;
        accent = b.accent;
        lines = b.lines;
        rows = b.rows;
        topRow = b.topRow;
        ratio = b.ratio;
        region = b.region;
        swatch = b.swatch;
        fileText = b.fileText;
        fileButton = b.fileButton;
    }

    /**
     * The mutable collection of the values of a snapshot while it is built.
     * <p>
     * 스냅샷을 만드는 동안 값을 모으는 가변 객체.
     */
    static final class Builder {
        /** The element. 요소. */
        Element element;
        /** The kind. 종류. */
        Kind kind;
        /** Content box: left, top, width and height. content box 의 왼쪽, 위쪽, 너비, 높이. */
        double x, y, w, h;
        /** Border box: left, top, width and height. border box 의 왼쪽, 위쪽, 너비, 높이. */
        double borderX, borderY, borderW, borderH;
        /** The font. 글꼴. */
        Font font;
        /** The text color and the background color (ARGB). 글자색과 배경색(ARGB). */
        int color, background;
        /** The computed text-align. 계산된 text-align. */
        String textAlign = "start";
        /** The line height. 줄 높이. */
        double lineHeight;
        /** State flags. 상태 플래그. */
        boolean disabled, readOnly, checked, indeterminate, focused;
        /** True for the dark color scheme. 어두운 색 구성표이면 true. */
        boolean dark;
        /** The shown text, the placeholder and the file text. 보이는 글, placeholder, 파일 글. */
        String text = "", placeholder = "", fileText = "";
        /** The placeholder color and the accent color (ARGB, 0 accent for auto). placeholder 색과 강조색(ARGB. auto 이면 강조색 0). */
        int placeholderColor = 0xFF757575, accent;
        /** The text area lines. 텍스트 영역 줄. */
        String[] lines = new String[0];
        /** The list rows. 리스트 행. */
        Row[] rows = new Row[0];
        /** The first visible row. 처음 보이는 행. */
        int topRow;
        /** The ratio, -1 for indeterminate. 비율. 불확정이면 -1. */
        double ratio;
        /** The meter region. 미터 영역. */
        int region;
        /** The color well color. 색 선택 색. */
        int swatch;
        /** The look of the button of a file control from ::file-selector-button, or null. ::file-selector-button 에서 온 파일 컨트롤 단추의 모양. 없으면 null. */
        FileButton fileButton;

        /**
         * Creates an empty builder.
         * <p>
         * 빈 빌더를 만든다.
         */
        Builder() {
        }
    }

    /**
     * Returns the element of the control.
     * <p>
     * 컨트롤의 요소를 반환한다.
     *
     * @return the element / 요소
     */
    public Element getElement() {
        return element;
    }

    /**
     * Returns the kind of the control.
     * <p>
     * 컨트롤의 종류를 반환한다.
     *
     * @return the kind / 종류
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the left of the content box in document coordinates.
     * <p>
     * 문서 좌표에서 content box 의 왼쪽을 반환한다.
     *
     * @return the left / 왼쪽
     */
    public double getX() {
        return x;
    }

    /**
     * Returns the top of the content box in document coordinates.
     * <p>
     * 문서 좌표에서 content box 의 위쪽을 반환한다.
     *
     * @return the top / 위쪽
     */
    public double getY() {
        return y;
    }

    /**
     * Returns the width of the content box.
     * <p>
     * content box 의 너비를 반환한다.
     *
     * @return the width / 너비
     */
    public double getWidth() {
        return w;
    }

    /**
     * Returns the height of the content box.
     * <p>
     * content box 의 높이를 반환한다.
     *
     * @return the height / 높이
     */
    public double getHeight() {
        return h;
    }

    /**
     * Returns the border box of the control in document coordinates: left, top, width and height.
     * <p>
     * 문서 좌표에서 컨트롤의 border box 를 반환한다: 왼쪽, 위쪽, 너비, 높이.
     *
     * @return the array of left, top, width and height / 왼쪽, 위쪽, 너비, 높이 배열
     */
    public double[] getBorderBox() {
        return new double[] { borderX, borderY, borderW, borderH };
    }

    /**
     * Returns the font of the control.
     * <p>
     * 컨트롤의 글꼴을 반환한다.
     *
     * @return the font / 글꼴
     */
    public Font getFont() {
        return font;
    }

    /**
     * Returns the text color.
     * <p>
     * 글자색을 반환한다.
     *
     * @return the ARGB color / ARGB 색
     */
    public int getColor() {
        return color;
    }

    /**
     * Returns the background color of the control (0 if it has none).
     * <p>
     * 컨트롤의 배경색을 반환한다(없으면 0).
     *
     * @return the ARGB color / ARGB 색
     */
    public int getBackground() {
        return background;
    }

    /**
     * Returns the computed <code>text-align</code> keyword.
     * <p>
     * 계산된 <code>text-align</code> 키워드를 반환한다.
     *
     * @return the keyword / 키워드
     */
    public String getTextAlign() {
        return textAlign;
    }

    /**
     * Returns the height of one line of text.
     * <p>
     * 글 한 줄의 높이를 반환한다.
     *
     * @return the height in pixels / 높이(px)
     */
    public double getLineHeight() {
        return lineHeight;
    }

    /**
     * Tells whether the control is disabled.
     * <p>
     * 컨트롤이 비활성인지 알려 준다.
     *
     * @return true if disabled / 비활성이면 true
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * Tells whether the control is read-only.
     * <p>
     * 컨트롤이 읽기 전용인지 알려 준다.
     *
     * @return true if read-only / 읽기 전용이면 true
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Tells whether a check box or radio button is checked.
     * <p>
     * 체크박스나 라디오 단추가 체크되었는지 알려 준다.
     *
     * @return true if checked / 체크되었으면 true
     */
    public boolean isChecked() {
        return checked;
    }

    /**
     * Returns the text that the control shows (the value, bullets for a password, the label of a button, the selected option of a drop-down).
     * <p>
     * 컨트롤이 보여 주는 글을 반환한다(값, 암호는 검은 점, 단추의 라벨, 드롭다운에서 선택된 option).
     *
     * @return the text / 글
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the rows of a list box (empty for the other controls).
     * <p>
     * 리스트 박스의 행을 반환한다(다른 컨트롤은 빈 배열).
     *
     * @return a copy of the rows / 행의 복사본
     */
    public Row[] getRows() {
        return rows.clone();
    }

    /**
     * Returns the index of the first visible row of a list box.
     * <p>
     * 리스트 박스에서 처음 보이는 행의 인덱스를 반환한다.
     *
     * @return the index / 인덱스
     */
    public int getTopRow() {
        return topRow;
    }

    /**
     * Returns the position of a progress bar or slider between 0 and 1, or -1 for an indeterminate progress bar.
     * <p>
     * 진행 막대나 슬라이더의 0~1 위치를 반환한다. 불확정 진행 막대는 -1.
     *
     * @return the ratio / 비율
     */
    public double getRatio() {
        return ratio;
    }

    /**
     * Returns the region of a meter: 0 optimum, 1 suboptimal, 2 even less good.
     * <p>
     * 미터의 영역을 반환한다: 0 최적, 1 차선, 2 더 나쁨.
     *
     * @return the region / 영역
     */
    public int getRegion() {
        return region;
    }

    /**
     * Returns the text of the wrapped lines of a text area.
     * <p>
     * 텍스트 영역의 줄 바꿈한 줄들을 반환한다.
     *
     * @return a copy of the lines / 줄의 복사본
     */
    public String[] getLines() {
        return lines.clone();
    }

    /**
     * Returns the text that a file control shows next to its button.
     * <p>
     * 파일 컨트롤이 단추 옆에 보여 주는 글을 반환한다.
     *
     * @return the text / 글
     */
    public String getFileText() {
        return fileText;
    }

    /**
     * Returns the placeholder text shown while the control is empty.
     * <p>
     * 컨트롤이 비어 있을 때 보이는 placeholder 글을 반환한다.
     *
     * @return the placeholder / placeholder
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     * Returns the color of a color well.
     * <p>
     * 색 선택 단추의 색을 반환한다.
     *
     * @return the ARGB color / ARGB 색
     */
    public int getSwatch() {
        return swatch;
    }
}
