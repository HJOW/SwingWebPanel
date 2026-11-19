package org.duckdns.hjow.swingwebpanel.css.props;

/**
 * The numeric ids of the longhand properties. The constants are generated from the rows of {@link PropertyTable}: constant N is the
 * N-th row, and the name is the property name in upper case with hyphens replaced by underscores. Layout and rendering code switches on
 * these ids and reads them with <code>ComputedStyle.get(Prop.WIDTH)</code>.
 * <p>
 * 개별 속성의 숫자 id. 상수는 {@link PropertyTable} 의 행에서 생성한다: 상수 N 은 N 번째 행이며 이름은 속성 이름을 대문자로 하고 하이픈을 밑줄로 바꾼 것이다.
 * 레이아웃과 렌더링 코드는 이 id 로 분기하고 <code>ComputedStyle.get(Prop.WIDTH)</code> 로 읽는다.
 */
public final class Prop {

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있다.
     */
    private Prop() {
    }

    /** Property id of <code>display</code>. / <code>display</code> 속성의 id. */
    public static final int DISPLAY = 1;
    /** Property id of <code>position</code>. / <code>position</code> 속성의 id. */
    public static final int POSITION = 2;
    /** Property id of <code>top</code>. / <code>top</code> 속성의 id. */
    public static final int TOP = 3;
    /** Property id of <code>right</code>. / <code>right</code> 속성의 id. */
    public static final int RIGHT = 4;
    /** Property id of <code>bottom</code>. / <code>bottom</code> 속성의 id. */
    public static final int BOTTOM = 5;
    /** Property id of <code>left</code>. / <code>left</code> 속성의 id. */
    public static final int LEFT = 6;
    /** Property id of <code>float</code>. / <code>float</code> 속성의 id. */
    public static final int FLOAT = 7;
    /** Property id of <code>clear</code>. / <code>clear</code> 속성의 id. */
    public static final int CLEAR = 8;
    /** Property id of <code>z-index</code>. / <code>z-index</code> 속성의 id. */
    public static final int Z_INDEX = 9;
    /** Property id of <code>overflow-x</code>. / <code>overflow-x</code> 속성의 id. */
    public static final int OVERFLOW_X = 10;
    /** Property id of <code>overflow-y</code>. / <code>overflow-y</code> 속성의 id. */
    public static final int OVERFLOW_Y = 11;
    /** Property id of <code>overflow-wrap</code>. / <code>overflow-wrap</code> 속성의 id. */
    public static final int OVERFLOW_WRAP = 12;
    /** Property id of <code>visibility</code>. / <code>visibility</code> 속성의 id. */
    public static final int VISIBILITY = 13;
    /** Property id of <code>opacity</code>. / <code>opacity</code> 속성의 id. */
    public static final int OPACITY = 14;
    /** Property id of <code>box-sizing</code>. / <code>box-sizing</code> 속성의 id. */
    public static final int BOX_SIZING = 15;
    /** Property id of <code>width</code>. / <code>width</code> 속성의 id. */
    public static final int WIDTH = 16;
    /** Property id of <code>height</code>. / <code>height</code> 속성의 id. */
    public static final int HEIGHT = 17;
    /** Property id of <code>min-width</code>. / <code>min-width</code> 속성의 id. */
    public static final int MIN_WIDTH = 18;
    /** Property id of <code>min-height</code>. / <code>min-height</code> 속성의 id. */
    public static final int MIN_HEIGHT = 19;
    /** Property id of <code>max-width</code>. / <code>max-width</code> 속성의 id. */
    public static final int MAX_WIDTH = 20;
    /** Property id of <code>max-height</code>. / <code>max-height</code> 속성의 id. */
    public static final int MAX_HEIGHT = 21;
    /** Property id of <code>clip</code>. / <code>clip</code> 속성의 id. */
    public static final int CLIP = 22;
    /** Property id of <code>margin-top</code>. / <code>margin-top</code> 속성의 id. */
    public static final int MARGIN_TOP = 23;
    /** Property id of <code>margin-right</code>. / <code>margin-right</code> 속성의 id. */
    public static final int MARGIN_RIGHT = 24;
    /** Property id of <code>margin-bottom</code>. / <code>margin-bottom</code> 속성의 id. */
    public static final int MARGIN_BOTTOM = 25;
    /** Property id of <code>margin-left</code>. / <code>margin-left</code> 속성의 id. */
    public static final int MARGIN_LEFT = 26;
    /** Property id of <code>padding-top</code>. / <code>padding-top</code> 속성의 id. */
    public static final int PADDING_TOP = 27;
    /** Property id of <code>padding-right</code>. / <code>padding-right</code> 속성의 id. */
    public static final int PADDING_RIGHT = 28;
    /** Property id of <code>padding-bottom</code>. / <code>padding-bottom</code> 속성의 id. */
    public static final int PADDING_BOTTOM = 29;
    /** Property id of <code>padding-left</code>. / <code>padding-left</code> 속성의 id. */
    public static final int PADDING_LEFT = 30;
    /** Property id of <code>border-top-width</code>. / <code>border-top-width</code> 속성의 id. */
    public static final int BORDER_TOP_WIDTH = 31;
    /** Property id of <code>border-right-width</code>. / <code>border-right-width</code> 속성의 id. */
    public static final int BORDER_RIGHT_WIDTH = 32;
    /** Property id of <code>border-bottom-width</code>. / <code>border-bottom-width</code> 속성의 id. */
    public static final int BORDER_BOTTOM_WIDTH = 33;
    /** Property id of <code>border-left-width</code>. / <code>border-left-width</code> 속성의 id. */
    public static final int BORDER_LEFT_WIDTH = 34;
    /** Property id of <code>border-top-style</code>. / <code>border-top-style</code> 속성의 id. */
    public static final int BORDER_TOP_STYLE = 35;
    /** Property id of <code>border-right-style</code>. / <code>border-right-style</code> 속성의 id. */
    public static final int BORDER_RIGHT_STYLE = 36;
    /** Property id of <code>border-bottom-style</code>. / <code>border-bottom-style</code> 속성의 id. */
    public static final int BORDER_BOTTOM_STYLE = 37;
    /** Property id of <code>border-left-style</code>. / <code>border-left-style</code> 속성의 id. */
    public static final int BORDER_LEFT_STYLE = 38;
    /** Property id of <code>border-top-color</code>. / <code>border-top-color</code> 속성의 id. */
    public static final int BORDER_TOP_COLOR = 39;
    /** Property id of <code>border-right-color</code>. / <code>border-right-color</code> 속성의 id. */
    public static final int BORDER_RIGHT_COLOR = 40;
    /** Property id of <code>border-bottom-color</code>. / <code>border-bottom-color</code> 속성의 id. */
    public static final int BORDER_BOTTOM_COLOR = 41;
    /** Property id of <code>border-left-color</code>. / <code>border-left-color</code> 속성의 id. */
    public static final int BORDER_LEFT_COLOR = 42;
    /** Property id of <code>border-top-left-radius</code>. / <code>border-top-left-radius</code> 속성의 id. */
    public static final int BORDER_TOP_LEFT_RADIUS = 43;
    /** Property id of <code>border-top-right-radius</code>. / <code>border-top-right-radius</code> 속성의 id. */
    public static final int BORDER_TOP_RIGHT_RADIUS = 44;
    /** Property id of <code>border-bottom-right-radius</code>. / <code>border-bottom-right-radius</code> 속성의 id. */
    public static final int BORDER_BOTTOM_RIGHT_RADIUS = 45;
    /** Property id of <code>border-bottom-left-radius</code>. / <code>border-bottom-left-radius</code> 속성의 id. */
    public static final int BORDER_BOTTOM_LEFT_RADIUS = 46;
    /** Property id of <code>border-collapse</code>. / <code>border-collapse</code> 속성의 id. */
    public static final int BORDER_COLLAPSE = 47;
    /** Property id of <code>border-spacing</code>. / <code>border-spacing</code> 속성의 id. */
    public static final int BORDER_SPACING = 48;
    /** Property id of <code>border-image-source</code>. / <code>border-image-source</code> 속성의 id. */
    public static final int BORDER_IMAGE_SOURCE = 49;
    /** Property id of <code>border-image-slice</code>. / <code>border-image-slice</code> 속성의 id. */
    public static final int BORDER_IMAGE_SLICE = 50;
    /** Property id of <code>border-image-width</code>. / <code>border-image-width</code> 속성의 id. */
    public static final int BORDER_IMAGE_WIDTH = 51;
    /** Property id of <code>border-image-outset</code>. / <code>border-image-outset</code> 속성의 id. */
    public static final int BORDER_IMAGE_OUTSET = 52;
    /** Property id of <code>border-image-repeat</code>. / <code>border-image-repeat</code> 속성의 id. */
    public static final int BORDER_IMAGE_REPEAT = 53;
    /** Property id of <code>outline-width</code>. / <code>outline-width</code> 속성의 id. */
    public static final int OUTLINE_WIDTH = 54;
    /** Property id of <code>outline-style</code>. / <code>outline-style</code> 속성의 id. */
    public static final int OUTLINE_STYLE = 55;
    /** Property id of <code>outline-color</code>. / <code>outline-color</code> 속성의 id. */
    public static final int OUTLINE_COLOR = 56;
    /** Property id of <code>outline-offset</code>. / <code>outline-offset</code> 속성의 id. */
    public static final int OUTLINE_OFFSET = 57;
    /** Property id of <code>box-shadow</code>. / <code>box-shadow</code> 속성의 id. */
    public static final int BOX_SHADOW = 58;
    /** Property id of <code>background-color</code>. / <code>background-color</code> 속성의 id. */
    public static final int BACKGROUND_COLOR = 59;
    /** Property id of <code>background-image</code>. / <code>background-image</code> 속성의 id. */
    public static final int BACKGROUND_IMAGE = 60;
    /** Property id of <code>background-repeat</code>. / <code>background-repeat</code> 속성의 id. */
    public static final int BACKGROUND_REPEAT = 61;
    /** Property id of <code>background-attachment</code>. / <code>background-attachment</code> 속성의 id. */
    public static final int BACKGROUND_ATTACHMENT = 62;
    /** Property id of <code>background-position</code>. / <code>background-position</code> 속성의 id. */
    public static final int BACKGROUND_POSITION = 63;
    /** Property id of <code>background-size</code>. / <code>background-size</code> 속성의 id. */
    public static final int BACKGROUND_SIZE = 64;
    /** Property id of <code>background-origin</code>. / <code>background-origin</code> 속성의 id. */
    public static final int BACKGROUND_ORIGIN = 65;
    /** Property id of <code>background-clip</code>. / <code>background-clip</code> 속성의 id. */
    public static final int BACKGROUND_CLIP = 66;
    /** Property id of <code>color</code>. / <code>color</code> 속성의 id. */
    public static final int COLOR = 67;
    /** Property id of <code>font-family</code>. / <code>font-family</code> 속성의 id. */
    public static final int FONT_FAMILY = 68;
    /** Property id of <code>font-size</code>. / <code>font-size</code> 속성의 id. */
    public static final int FONT_SIZE = 69;
    /** Property id of <code>font-style</code>. / <code>font-style</code> 속성의 id. */
    public static final int FONT_STYLE = 70;
    /** Property id of <code>font-variant-caps</code> (the <code>font-variant</code> shorthand sets it and five more). / <code>font-variant-caps</code> 속성의 id(<code>font-variant</code> 단축 속성이 이것과 다섯 개를 더 정한다). */
    public static final int FONT_VARIANT_CAPS = 71;
    /** Property id of <code>font-weight</code>. / <code>font-weight</code> 속성의 id. */
    public static final int FONT_WEIGHT = 72;
    /** Property id of <code>font-stretch</code>. / <code>font-stretch</code> 속성의 id. */
    public static final int FONT_STRETCH = 73;
    /** Property id of <code>line-height</code>. / <code>line-height</code> 속성의 id. */
    public static final int LINE_HEIGHT = 74;
    /** Property id of <code>font-size-adjust</code>. / <code>font-size-adjust</code> 속성의 id. */
    public static final int FONT_SIZE_ADJUST = 75;
    /** Property id of <code>font-kerning</code>. / <code>font-kerning</code> 속성의 id. */
    public static final int FONT_KERNING = 76;
    /** Property id of <code>font-feature-settings</code>. / <code>font-feature-settings</code> 속성의 id. */
    public static final int FONT_FEATURE_SETTINGS = 77;
    /** Property id of <code>text-align</code>. / <code>text-align</code> 속성의 id. */
    public static final int TEXT_ALIGN = 78;
    /** Property id of <code>text-align-last</code>. / <code>text-align-last</code> 속성의 id. */
    public static final int TEXT_ALIGN_LAST = 79;
    /** Property id of <code>text-indent</code>. / <code>text-indent</code> 속성의 id. */
    public static final int TEXT_INDENT = 80;
    /** Property id of <code>text-transform</code>. / <code>text-transform</code> 속성의 id. */
    public static final int TEXT_TRANSFORM = 81;
    /** Property id of <code>text-decoration-line</code>. / <code>text-decoration-line</code> 속성의 id. */
    public static final int TEXT_DECORATION_LINE = 82;
    /** Property id of <code>text-decoration-style</code>. / <code>text-decoration-style</code> 속성의 id. */
    public static final int TEXT_DECORATION_STYLE = 83;
    /** Property id of <code>text-decoration-color</code>. / <code>text-decoration-color</code> 속성의 id. */
    public static final int TEXT_DECORATION_COLOR = 84;
    /** Property id of <code>text-decoration-thickness</code>. / <code>text-decoration-thickness</code> 속성의 id. */
    public static final int TEXT_DECORATION_THICKNESS = 85;
    /** Property id of <code>text-underline-offset</code>. / <code>text-underline-offset</code> 속성의 id. */
    public static final int TEXT_UNDERLINE_OFFSET = 86;
    /** Property id of <code>text-overflow</code>. / <code>text-overflow</code> 속성의 id. */
    public static final int TEXT_OVERFLOW = 87;
    /** Property id of <code>text-shadow</code>. / <code>text-shadow</code> 속성의 id. */
    public static final int TEXT_SHADOW = 88;
    /** Property id of <code>letter-spacing</code>. / <code>letter-spacing</code> 속성의 id. */
    public static final int LETTER_SPACING = 89;
    /** Property id of <code>word-spacing</code>. / <code>word-spacing</code> 속성의 id. */
    public static final int WORD_SPACING = 90;
    /** Property id of <code>white-space</code>. / <code>white-space</code> 속성의 id. */
    public static final int WHITE_SPACE = 91;
    /** Property id of <code>word-break</code>. / <code>word-break</code> 속성의 id. */
    public static final int WORD_BREAK = 92;
    /** Property id of <code>line-break</code>. / <code>line-break</code> 속성의 id. */
    public static final int LINE_BREAK = 93;
    /** Property id of <code>hyphens</code>. / <code>hyphens</code> 속성의 id. */
    public static final int HYPHENS = 94;
    /** Property id of <code>tab-size</code>. / <code>tab-size</code> 속성의 id. */
    public static final int TAB_SIZE = 95;
    /** Property id of <code>vertical-align</code>. / <code>vertical-align</code> 속성의 id. */
    public static final int VERTICAL_ALIGN = 96;
    /** Property id of <code>direction</code>. / <code>direction</code> 속성의 id. */
    public static final int DIRECTION = 97;
    /** Property id of <code>unicode-bidi</code>. / <code>unicode-bidi</code> 속성의 id. */
    public static final int UNICODE_BIDI = 98;
    /** Property id of <code>writing-mode</code>. / <code>writing-mode</code> 속성의 id. */
    public static final int WRITING_MODE = 99;
    /** Property id of <code>text-orientation</code>. / <code>text-orientation</code> 속성의 id. */
    public static final int TEXT_ORIENTATION = 100;
    /** Property id of <code>orphans</code>. / <code>orphans</code> 속성의 id. */
    public static final int ORPHANS = 101;
    /** Property id of <code>widows</code>. / <code>widows</code> 속성의 id. */
    public static final int WIDOWS = 102;
    /** Property id of <code>list-style-type</code>. / <code>list-style-type</code> 속성의 id. */
    public static final int LIST_STYLE_TYPE = 103;
    /** Property id of <code>list-style-position</code>. / <code>list-style-position</code> 속성의 id. */
    public static final int LIST_STYLE_POSITION = 104;
    /** Property id of <code>list-style-image</code>. / <code>list-style-image</code> 속성의 id. */
    public static final int LIST_STYLE_IMAGE = 105;
    /** Property id of <code>counter-reset</code>. / <code>counter-reset</code> 속성의 id. */
    public static final int COUNTER_RESET = 106;
    /** Property id of <code>counter-increment</code>. / <code>counter-increment</code> 속성의 id. */
    public static final int COUNTER_INCREMENT = 107;
    /** Property id of <code>counter-set</code>. / <code>counter-set</code> 속성의 id. */
    public static final int COUNTER_SET = 108;
    /** Property id of <code>quotes</code>. / <code>quotes</code> 속성의 id. */
    public static final int QUOTES = 109;
    /** Property id of <code>content</code>. / <code>content</code> 속성의 id. */
    public static final int CONTENT = 110;
    /** Property id of <code>table-layout</code>. / <code>table-layout</code> 속성의 id. */
    public static final int TABLE_LAYOUT = 111;
    /** Property id of <code>caption-side</code>. / <code>caption-side</code> 속성의 id. */
    public static final int CAPTION_SIDE = 112;
    /** Property id of <code>empty-cells</code>. / <code>empty-cells</code> 속성의 id. */
    public static final int EMPTY_CELLS = 113;
    /** Property id of <code>flex-grow</code>. / <code>flex-grow</code> 속성의 id. */
    public static final int FLEX_GROW = 114;
    /** Property id of <code>flex-shrink</code>. / <code>flex-shrink</code> 속성의 id. */
    public static final int FLEX_SHRINK = 115;
    /** Property id of <code>flex-basis</code>. / <code>flex-basis</code> 속성의 id. */
    public static final int FLEX_BASIS = 116;
    /** Property id of <code>flex-direction</code>. / <code>flex-direction</code> 속성의 id. */
    public static final int FLEX_DIRECTION = 117;
    /** Property id of <code>flex-wrap</code>. / <code>flex-wrap</code> 속성의 id. */
    public static final int FLEX_WRAP = 118;
    /** Property id of <code>order</code>. / <code>order</code> 속성의 id. */
    public static final int ORDER = 119;
    /** Property id of <code>justify-content</code>. / <code>justify-content</code> 속성의 id. */
    public static final int JUSTIFY_CONTENT = 120;
    /** Property id of <code>align-items</code>. / <code>align-items</code> 속성의 id. */
    public static final int ALIGN_ITEMS = 121;
    /** Property id of <code>align-self</code>. / <code>align-self</code> 속성의 id. */
    public static final int ALIGN_SELF = 122;
    /** Property id of <code>align-content</code>. / <code>align-content</code> 속성의 id. */
    public static final int ALIGN_CONTENT = 123;
    /** Property id of <code>justify-items</code>. / <code>justify-items</code> 속성의 id. */
    public static final int JUSTIFY_ITEMS = 124;
    /** Property id of <code>justify-self</code>. / <code>justify-self</code> 속성의 id. */
    public static final int JUSTIFY_SELF = 125;
    /** Property id of <code>row-gap</code>. / <code>row-gap</code> 속성의 id. */
    public static final int ROW_GAP = 126;
    /** Property id of <code>column-gap</code>. / <code>column-gap</code> 속성의 id. */
    public static final int COLUMN_GAP = 127;
    /** Property id of <code>column-count</code>. / <code>column-count</code> 속성의 id. */
    public static final int COLUMN_COUNT = 128;
    /** Property id of <code>column-width</code>. / <code>column-width</code> 속성의 id. */
    public static final int COLUMN_WIDTH = 129;
    /** Property id of <code>column-rule-width</code>. / <code>column-rule-width</code> 속성의 id. */
    public static final int COLUMN_RULE_WIDTH = 130;
    /** Property id of <code>column-rule-style</code>. / <code>column-rule-style</code> 속성의 id. */
    public static final int COLUMN_RULE_STYLE = 131;
    /** Property id of <code>column-rule-color</code>. / <code>column-rule-color</code> 속성의 id. */
    public static final int COLUMN_RULE_COLOR = 132;
    /** Property id of <code>column-span</code>. / <code>column-span</code> 속성의 id. */
    public static final int COLUMN_SPAN = 133;
    /** Property id of <code>column-fill</code>. / <code>column-fill</code> 속성의 id. */
    public static final int COLUMN_FILL = 134;
    /** Property id of <code>grid-template-columns</code>. / <code>grid-template-columns</code> 속성의 id. */
    public static final int GRID_TEMPLATE_COLUMNS = 135;
    /** Property id of <code>grid-template-rows</code>. / <code>grid-template-rows</code> 속성의 id. */
    public static final int GRID_TEMPLATE_ROWS = 136;
    /** Property id of <code>grid-template-areas</code>. / <code>grid-template-areas</code> 속성의 id. */
    public static final int GRID_TEMPLATE_AREAS = 137;
    /** Property id of <code>grid-auto-flow</code>. / <code>grid-auto-flow</code> 속성의 id. */
    public static final int GRID_AUTO_FLOW = 138;
    /** Property id of <code>grid-auto-columns</code>. / <code>grid-auto-columns</code> 속성의 id. */
    public static final int GRID_AUTO_COLUMNS = 139;
    /** Property id of <code>grid-auto-rows</code>. / <code>grid-auto-rows</code> 속성의 id. */
    public static final int GRID_AUTO_ROWS = 140;
    /** Property id of <code>grid-row-start</code>. / <code>grid-row-start</code> 속성의 id. */
    public static final int GRID_ROW_START = 141;
    /** Property id of <code>grid-row-end</code>. / <code>grid-row-end</code> 속성의 id. */
    public static final int GRID_ROW_END = 142;
    /** Property id of <code>grid-column-start</code>. / <code>grid-column-start</code> 속성의 id. */
    public static final int GRID_COLUMN_START = 143;
    /** Property id of <code>grid-column-end</code>. / <code>grid-column-end</code> 속성의 id. */
    public static final int GRID_COLUMN_END = 144;
    /** Property id of <code>transform</code>. / <code>transform</code> 속성의 id. */
    public static final int TRANSFORM = 145;
    /** Property id of <code>transform-origin</code>. / <code>transform-origin</code> 속성의 id. */
    public static final int TRANSFORM_ORIGIN = 146;
    /** Property id of <code>transition-property</code>. / <code>transition-property</code> 속성의 id. */
    public static final int TRANSITION_PROPERTY = 147;
    /** Property id of <code>transition-duration</code>. / <code>transition-duration</code> 속성의 id. */
    public static final int TRANSITION_DURATION = 148;
    /** Property id of <code>transition-timing-function</code>. / <code>transition-timing-function</code> 속성의 id. */
    public static final int TRANSITION_TIMING_FUNCTION = 149;
    /** Property id of <code>transition-delay</code>. / <code>transition-delay</code> 속성의 id. */
    public static final int TRANSITION_DELAY = 150;
    /** Property id of <code>animation-name</code>. / <code>animation-name</code> 속성의 id. */
    public static final int ANIMATION_NAME = 151;
    /** Property id of <code>animation-duration</code>. / <code>animation-duration</code> 속성의 id. */
    public static final int ANIMATION_DURATION = 152;
    /** Property id of <code>animation-timing-function</code>. / <code>animation-timing-function</code> 속성의 id. */
    public static final int ANIMATION_TIMING_FUNCTION = 153;
    /** Property id of <code>animation-delay</code>. / <code>animation-delay</code> 속성의 id. */
    public static final int ANIMATION_DELAY = 154;
    /** Property id of <code>animation-iteration-count</code>. / <code>animation-iteration-count</code> 속성의 id. */
    public static final int ANIMATION_ITERATION_COUNT = 155;
    /** Property id of <code>animation-direction</code>. / <code>animation-direction</code> 속성의 id. */
    public static final int ANIMATION_DIRECTION = 156;
    /** Property id of <code>animation-fill-mode</code>. / <code>animation-fill-mode</code> 속성의 id. */
    public static final int ANIMATION_FILL_MODE = 157;
    /** Property id of <code>animation-play-state</code>. / <code>animation-play-state</code> 속성의 id. */
    public static final int ANIMATION_PLAY_STATE = 158;
    /** Property id of <code>cursor</code>. / <code>cursor</code> 속성의 id. */
    public static final int CURSOR = 159;
    /** Property id of <code>pointer-events</code>. / <code>pointer-events</code> 속성의 id. */
    public static final int POINTER_EVENTS = 160;
    /** Property id of <code>user-select</code>. / <code>user-select</code> 속성의 id. */
    public static final int USER_SELECT = 161;
    /** Property id of <code>resize</code>. / <code>resize</code> 속성의 id. */
    public static final int RESIZE = 162;
    /** Property id of <code>appearance</code>. / <code>appearance</code> 속성의 id. */
    public static final int APPEARANCE = 163;
    /** Property id of <code>accent-color</code>. / <code>accent-color</code> 속성의 id. */
    public static final int ACCENT_COLOR = 164;
    /** Property id of <code>caret-color</code>. / <code>caret-color</code> 속성의 id. */
    public static final int CARET_COLOR = 165;
    /** Property id of <code>scroll-behavior</code>. / <code>scroll-behavior</code> 속성의 id. */
    public static final int SCROLL_BEHAVIOR = 166;
    /** Property id of <code>object-fit</code>. / <code>object-fit</code> 속성의 id. */
    public static final int OBJECT_FIT = 167;
    /** Property id of <code>object-position</code>. / <code>object-position</code> 속성의 id. */
    public static final int OBJECT_POSITION = 168;
    /** Property id of <code>image-rendering</code>. / <code>image-rendering</code> 속성의 id. */
    public static final int IMAGE_RENDERING = 169;
    /** Property id of <code>filter</code>. / <code>filter</code> 속성의 id. */
    public static final int FILTER = 170;
    /** Property id of <code>mix-blend-mode</code>. / <code>mix-blend-mode</code> 속성의 id. */
    public static final int MIX_BLEND_MODE = 171;
    /** Property id of <code>isolation</code>. / <code>isolation</code> 속성의 id. */
    public static final int ISOLATION = 172;
    /** Property id of <code>will-change</code>. / <code>will-change</code> 속성의 id. */
    public static final int WILL_CHANGE = 173;
    /** Property id of <code>contain</code>. / <code>contain</code> 속성의 id. */
    public static final int CONTAIN = 174;
    /** Property id of <code>text-rendering</code>. / <code>text-rendering</code> 속성의 id. */
    public static final int TEXT_RENDERING = 175;
    /** Property id of <code>aspect-ratio</code>. / <code>aspect-ratio</code> 속성의 id. */
    public static final int ASPECT_RATIO = 176;
    /** Property id of <code>text-combine-upright</code>. / <code>text-combine-upright</code> 속성의 id. */
    public static final int TEXT_COMBINE_UPRIGHT = 177;
    /** Property id of <code>clip-path</code>. / <code>clip-path</code> 속성의 id. */
    public static final int CLIP_PATH = 178;
    /** Property id of <code>translate</code>. / <code>translate</code> 속성의 id. */
    public static final int TRANSLATE = 179;
    /** Property id of <code>rotate</code>. / <code>rotate</code> 속성의 id. */
    public static final int ROTATE = 180;
    /** Property id of <code>scale</code>. / <code>scale</code> 속성의 id. */
    public static final int SCALE = 181;
    /** Property id of <code>background-blend-mode</code>. / <code>background-blend-mode</code> 속성의 id. */
    public static final int BACKGROUND_BLEND_MODE = 182;
    /** Property id of <code>mask-image</code>. / <code>mask-image</code> 속성의 id. */
    public static final int MASK_IMAGE = 183;
    /** Property id of <code>mask-mode</code>. / <code>mask-mode</code> 속성의 id. */
    public static final int MASK_MODE = 184;
    /** Property id of <code>mask-repeat</code>. / <code>mask-repeat</code> 속성의 id. */
    public static final int MASK_REPEAT = 185;
    /** Property id of <code>mask-position</code>. / <code>mask-position</code> 속성의 id. */
    public static final int MASK_POSITION = 186;
    /** Property id of <code>mask-clip</code>. / <code>mask-clip</code> 속성의 id. */
    public static final int MASK_CLIP = 187;
    /** Property id of <code>mask-origin</code>. / <code>mask-origin</code> 속성의 id. */
    public static final int MASK_ORIGIN = 188;
    /** Property id of <code>mask-size</code>. / <code>mask-size</code> 속성의 id. */
    public static final int MASK_SIZE = 189;
    /** Property id of <code>mask-composite</code>. / <code>mask-composite</code> 속성의 id. */
    public static final int MASK_COMPOSITE = 190;
    /** Property id of <code>clip-rule</code>. / <code>clip-rule</code> 속성의 id. */
    public static final int CLIP_RULE = 191;
    /** Property id of <code>margin-block-start</code>. / <code>margin-block-start</code> 속성의 id. */
    public static final int MARGIN_BLOCK_START = 192;
    /** Property id of <code>margin-block-end</code>. / <code>margin-block-end</code> 속성의 id. */
    public static final int MARGIN_BLOCK_END = 193;
    /** Property id of <code>margin-inline-start</code>. / <code>margin-inline-start</code> 속성의 id. */
    public static final int MARGIN_INLINE_START = 194;
    /** Property id of <code>margin-inline-end</code>. / <code>margin-inline-end</code> 속성의 id. */
    public static final int MARGIN_INLINE_END = 195;
    /** Property id of <code>padding-block-start</code>. / <code>padding-block-start</code> 속성의 id. */
    public static final int PADDING_BLOCK_START = 196;
    /** Property id of <code>padding-block-end</code>. / <code>padding-block-end</code> 속성의 id. */
    public static final int PADDING_BLOCK_END = 197;
    /** Property id of <code>padding-inline-start</code>. / <code>padding-inline-start</code> 속성의 id. */
    public static final int PADDING_INLINE_START = 198;
    /** Property id of <code>padding-inline-end</code>. / <code>padding-inline-end</code> 속성의 id. */
    public static final int PADDING_INLINE_END = 199;
    /** Property id of <code>inset-block-start</code>. / <code>inset-block-start</code> 속성의 id. */
    public static final int INSET_BLOCK_START = 200;
    /** Property id of <code>inset-block-end</code>. / <code>inset-block-end</code> 속성의 id. */
    public static final int INSET_BLOCK_END = 201;
    /** Property id of <code>inset-inline-start</code>. / <code>inset-inline-start</code> 속성의 id. */
    public static final int INSET_INLINE_START = 202;
    /** Property id of <code>inset-inline-end</code>. / <code>inset-inline-end</code> 속성의 id. */
    public static final int INSET_INLINE_END = 203;
    /** Property id of <code>border-block-start-width</code>. / <code>border-block-start-width</code> 속성의 id. */
    public static final int BORDER_BLOCK_START_WIDTH = 204;
    /** Property id of <code>border-block-end-width</code>. / <code>border-block-end-width</code> 속성의 id. */
    public static final int BORDER_BLOCK_END_WIDTH = 205;
    /** Property id of <code>border-inline-start-width</code>. / <code>border-inline-start-width</code> 속성의 id. */
    public static final int BORDER_INLINE_START_WIDTH = 206;
    /** Property id of <code>border-inline-end-width</code>. / <code>border-inline-end-width</code> 속성의 id. */
    public static final int BORDER_INLINE_END_WIDTH = 207;
    /** Property id of <code>border-block-start-style</code>. / <code>border-block-start-style</code> 속성의 id. */
    public static final int BORDER_BLOCK_START_STYLE = 208;
    /** Property id of <code>border-block-end-style</code>. / <code>border-block-end-style</code> 속성의 id. */
    public static final int BORDER_BLOCK_END_STYLE = 209;
    /** Property id of <code>border-inline-start-style</code>. / <code>border-inline-start-style</code> 속성의 id. */
    public static final int BORDER_INLINE_START_STYLE = 210;
    /** Property id of <code>border-inline-end-style</code>. / <code>border-inline-end-style</code> 속성의 id. */
    public static final int BORDER_INLINE_END_STYLE = 211;
    /** Property id of <code>border-block-start-color</code>. / <code>border-block-start-color</code> 속성의 id. */
    public static final int BORDER_BLOCK_START_COLOR = 212;
    /** Property id of <code>border-block-end-color</code>. / <code>border-block-end-color</code> 속성의 id. */
    public static final int BORDER_BLOCK_END_COLOR = 213;
    /** Property id of <code>border-inline-start-color</code>. / <code>border-inline-start-color</code> 속성의 id. */
    public static final int BORDER_INLINE_START_COLOR = 214;
    /** Property id of <code>border-inline-end-color</code>. / <code>border-inline-end-color</code> 속성의 id. */
    public static final int BORDER_INLINE_END_COLOR = 215;
    /** Property id of <code>border-start-start-radius</code>. / <code>border-start-start-radius</code> 속성의 id. */
    public static final int BORDER_START_START_RADIUS = 216;
    /** Property id of <code>border-start-end-radius</code>. / <code>border-start-end-radius</code> 속성의 id. */
    public static final int BORDER_START_END_RADIUS = 217;
    /** Property id of <code>border-end-start-radius</code>. / <code>border-end-start-radius</code> 속성의 id. */
    public static final int BORDER_END_START_RADIUS = 218;
    /** Property id of <code>border-end-end-radius</code>. / <code>border-end-end-radius</code> 속성의 id. */
    public static final int BORDER_END_END_RADIUS = 219;
    /** Property id of <code>inline-size</code>. / <code>inline-size</code> 속성의 id. */
    public static final int INLINE_SIZE = 220;
    /** Property id of <code>block-size</code>. / <code>block-size</code> 속성의 id. */
    public static final int BLOCK_SIZE = 221;
    /** Property id of <code>min-inline-size</code>. / <code>min-inline-size</code> 속성의 id. */
    public static final int MIN_INLINE_SIZE = 222;
    /** Property id of <code>min-block-size</code>. / <code>min-block-size</code> 속성의 id. */
    public static final int MIN_BLOCK_SIZE = 223;
    /** Property id of <code>max-inline-size</code>. / <code>max-inline-size</code> 속성의 id. */
    public static final int MAX_INLINE_SIZE = 224;
    /** Property id of <code>max-block-size</code>. / <code>max-block-size</code> 속성의 id. */
    public static final int MAX_BLOCK_SIZE = 225;
    /** Property id of <code>break-before</code>. / <code>break-before</code> 속성의 id. */
    public static final int BREAK_BEFORE = 226;
    /** Property id of <code>break-after</code>. / <code>break-after</code> 속성의 id. */
    public static final int BREAK_AFTER = 227;
    /** Property id of <code>break-inside</code>. / <code>break-inside</code> 속성의 id. */
    public static final int BREAK_INSIDE = 228;
    /** Property id of <code>box-decoration-break</code>. / <code>box-decoration-break</code> 속성의 id. */
    public static final int BOX_DECORATION_BREAK = 229;
    /** Property id of <code>shape-outside</code>. / <code>shape-outside</code> 속성의 id. */
    public static final int SHAPE_OUTSIDE = 230;
    /** Property id of <code>shape-margin</code>. / <code>shape-margin</code> 속성의 id. */
    public static final int SHAPE_MARGIN = 231;
    /** Property id of <code>shape-image-threshold</code>. / <code>shape-image-threshold</code> 속성의 id. */
    public static final int SHAPE_IMAGE_THRESHOLD = 232;
    /** Property id of <code>container-type</code>. / <code>container-type</code> 속성의 id. */
    public static final int CONTAINER_TYPE = 233;
    /** Property id of <code>container-name</code>. / <code>container-name</code> 속성의 id. */
    public static final int CONTAINER_NAME = 234;
    /** Property id of <code>content-visibility</code>. / <code>content-visibility</code> 속성의 id. */
    public static final int CONTENT_VISIBILITY = 235;
    /** Property id of <code>contain-intrinsic-width</code>. / <code>contain-intrinsic-width</code> 속성의 id. */
    public static final int CONTAIN_INTRINSIC_WIDTH = 236;
    /** Property id of <code>contain-intrinsic-height</code>. / <code>contain-intrinsic-height</code> 속성의 id. */
    public static final int CONTAIN_INTRINSIC_HEIGHT = 237;
    /** Property id of <code>scroll-snap-type</code>. / <code>scroll-snap-type</code> 속성의 id. */
    public static final int SCROLL_SNAP_TYPE = 238;
    /** Property id of <code>scroll-snap-align</code>. / <code>scroll-snap-align</code> 속성의 id. */
    public static final int SCROLL_SNAP_ALIGN = 239;
    /** Property id of <code>scroll-snap-stop</code>. / <code>scroll-snap-stop</code> 속성의 id. */
    public static final int SCROLL_SNAP_STOP = 240;
    /** Property id of <code>scroll-padding-top</code>. / <code>scroll-padding-top</code> 속성의 id. */
    public static final int SCROLL_PADDING_TOP = 241;
    /** Property id of <code>scroll-padding-right</code>. / <code>scroll-padding-right</code> 속성의 id. */
    public static final int SCROLL_PADDING_RIGHT = 242;
    /** Property id of <code>scroll-padding-bottom</code>. / <code>scroll-padding-bottom</code> 속성의 id. */
    public static final int SCROLL_PADDING_BOTTOM = 243;
    /** Property id of <code>scroll-padding-left</code>. / <code>scroll-padding-left</code> 속성의 id. */
    public static final int SCROLL_PADDING_LEFT = 244;
    /** Property id of <code>scroll-margin-top</code>. / <code>scroll-margin-top</code> 속성의 id. */
    public static final int SCROLL_MARGIN_TOP = 245;
    /** Property id of <code>scroll-margin-right</code>. / <code>scroll-margin-right</code> 속성의 id. */
    public static final int SCROLL_MARGIN_RIGHT = 246;
    /** Property id of <code>scroll-margin-bottom</code>. / <code>scroll-margin-bottom</code> 속성의 id. */
    public static final int SCROLL_MARGIN_BOTTOM = 247;
    /** Property id of <code>scroll-margin-left</code>. / <code>scroll-margin-left</code> 속성의 id. */
    public static final int SCROLL_MARGIN_LEFT = 248;
    /** Property id of <code>overscroll-behavior-x</code>. / <code>overscroll-behavior-x</code> 속성의 id. */
    public static final int OVERSCROLL_BEHAVIOR_X = 249;
    /** Property id of <code>overscroll-behavior-y</code>. / <code>overscroll-behavior-y</code> 속성의 id. */
    public static final int OVERSCROLL_BEHAVIOR_Y = 250;
    /** Property id of <code>scrollbar-width</code>. / <code>scrollbar-width</code> 속성의 id. */
    public static final int SCROLLBAR_WIDTH = 251;
    /** Property id of <code>scrollbar-color</code>. / <code>scrollbar-color</code> 속성의 id. */
    public static final int SCROLLBAR_COLOR = 252;
    /** Property id of <code>scrollbar-gutter</code>. / <code>scrollbar-gutter</code> 속성의 id. */
    public static final int SCROLLBAR_GUTTER = 253;
    /** Property id of <code>touch-action</code>. / <code>touch-action</code> 속성의 id. */
    public static final int TOUCH_ACTION = 254;
    /** Property id of <code>color-scheme</code>. / <code>color-scheme</code> 속성의 id. */
    public static final int COLOR_SCHEME = 255;
    /** Property id of <code>forced-color-adjust</code>. / <code>forced-color-adjust</code> 속성의 id. */
    public static final int FORCED_COLOR_ADJUST = 256;
    /** Property id of <code>print-color-adjust</code>. / <code>print-color-adjust</code> 속성의 id. */
    public static final int PRINT_COLOR_ADJUST = 257;
    /** Property id of <code>text-wrap-mode</code>. / <code>text-wrap-mode</code> 속성의 id. */
    public static final int TEXT_WRAP_MODE = 258;
    /** Property id of <code>text-wrap-style</code>. / <code>text-wrap-style</code> 속성의 id. */
    public static final int TEXT_WRAP_STYLE = 259;
    /** Property id of <code>hanging-punctuation</code>. / <code>hanging-punctuation</code> 속성의 id. */
    public static final int HANGING_PUNCTUATION = 260;
    /** Property id of <code>hyphenate-character</code>. / <code>hyphenate-character</code> 속성의 id. */
    public static final int HYPHENATE_CHARACTER = 261;
    /** Property id of <code>text-justify</code>. / <code>text-justify</code> 속성의 id. */
    public static final int TEXT_JUSTIFY = 262;
    /** Property id of <code>text-emphasis-style</code>. / <code>text-emphasis-style</code> 속성의 id. */
    public static final int TEXT_EMPHASIS_STYLE = 263;
    /** Property id of <code>text-emphasis-color</code>. / <code>text-emphasis-color</code> 속성의 id. */
    public static final int TEXT_EMPHASIS_COLOR = 264;
    /** Property id of <code>text-emphasis-position</code>. / <code>text-emphasis-position</code> 속성의 id. */
    public static final int TEXT_EMPHASIS_POSITION = 265;
    /** Property id of <code>text-underline-position</code>. / <code>text-underline-position</code> 속성의 id. */
    public static final int TEXT_UNDERLINE_POSITION = 266;
    /** Property id of <code>text-decoration-skip-ink</code>. / <code>text-decoration-skip-ink</code> 속성의 id. */
    public static final int TEXT_DECORATION_SKIP_INK = 267;
    /** Property id of <code>font-variant-ligatures</code>. / <code>font-variant-ligatures</code> 속성의 id. */
    public static final int FONT_VARIANT_LIGATURES = 268;
    /** Property id of <code>font-variant-numeric</code>. / <code>font-variant-numeric</code> 속성의 id. */
    public static final int FONT_VARIANT_NUMERIC = 269;
    /** Property id of <code>font-variant-east-asian</code>. / <code>font-variant-east-asian</code> 속성의 id. */
    public static final int FONT_VARIANT_EAST_ASIAN = 270;
    /** Property id of <code>font-variant-position</code>. / <code>font-variant-position</code> 속성의 id. */
    public static final int FONT_VARIANT_POSITION = 271;
    /** Property id of <code>font-variant-alternates</code>. / <code>font-variant-alternates</code> 속성의 id. */
    public static final int FONT_VARIANT_ALTERNATES = 272;
    /** Property id of <code>font-synthesis-weight</code> (<code>font-synthesis</code> is a shorthand of this and the three that follow at 304 to 306). <code>font-synthesis-weight</code> 속성의 id(<code>font-synthesis</code> 는 이것과 304~306 의 세 속성의 단축 속성이다). */
    public static final int FONT_SYNTHESIS_WEIGHT = 273;
    /** Property id of <code>font-optical-sizing</code>. / <code>font-optical-sizing</code> 속성의 id. */
    public static final int FONT_OPTICAL_SIZING = 274;
    /** Property id of <code>font-variation-settings</code>. / <code>font-variation-settings</code> 속성의 id. */
    public static final int FONT_VARIATION_SETTINGS = 275;
    /** Property id of <code>font-language-override</code>. / <code>font-language-override</code> 속성의 id. */
    public static final int FONT_LANGUAGE_OVERRIDE = 276;
    /** Property id of <code>line-clamp</code>. / <code>line-clamp</code> 속성의 id. */
    public static final int LINE_CLAMP = 277;
    /** Property id of <code>size</code> (the <code>@page</code> descriptor). / <code>size</code> 속성(<code>@page</code> 서술자)의 id. */
    public static final int SIZE = 278;
    /** Property id of <code>page-orientation</code>. / <code>page-orientation</code> 속성의 id. */
    public static final int PAGE_ORIENTATION = 279;
    /** Property id of <code>marks</code>. / <code>marks</code> 속성의 id. */
    public static final int MARKS = 280;
    /** Property id of <code>bleed</code>. / <code>bleed</code> 속성의 id. */
    public static final int BLEED = 281;
    /** Property id of <code>page</code>. / <code>page</code> 속성의 id. */
    public static final int PAGE = 282;
    /** Property id of <code>backdrop-filter</code>. / <code>backdrop-filter</code> 속성의 id. */
    public static final int BACKDROP_FILTER = 283;
    /** Property id of <code>initial-letter</code>. / <code>initial-letter</code> 속성의 id. */
    public static final int INITIAL_LETTER = 284;
    /** Property id of <code>mask-border-source</code>. / <code>mask-border-source</code> 속성의 id. */
    public static final int MASK_BORDER_SOURCE = 285;
    /** Property id of <code>mask-border-mode</code>. / <code>mask-border-mode</code> 속성의 id. */
    public static final int MASK_BORDER_MODE = 286;
    /** Property id of <code>mask-border-slice</code>. / <code>mask-border-slice</code> 속성의 id. */
    public static final int MASK_BORDER_SLICE = 287;
    /** Property id of <code>mask-border-width</code>. / <code>mask-border-width</code> 속성의 id. */
    public static final int MASK_BORDER_WIDTH = 288;
    /** Property id of <code>mask-border-outset</code>. / <code>mask-border-outset</code> 속성의 id. */
    public static final int MASK_BORDER_OUTSET = 289;
    /** Property id of <code>mask-border-repeat</code>. / <code>mask-border-repeat</code> 속성의 id. */
    public static final int MASK_BORDER_REPEAT = 290;
    /** Property id of <code>offset-path</code>. / <code>offset-path</code> 속성의 id. */
    public static final int OFFSET_PATH = 291;
    /** Property id of <code>offset-distance</code>. / <code>offset-distance</code> 속성의 id. */
    public static final int OFFSET_DISTANCE = 292;
    /** Property id of <code>offset-rotate</code>. / <code>offset-rotate</code> 속성의 id. */
    public static final int OFFSET_ROTATE = 293;
    /** Property id of <code>offset-anchor</code>. / <code>offset-anchor</code> 속성의 id. */
    public static final int OFFSET_ANCHOR = 294;
    /** Property id of <code>offset-position</code>. / <code>offset-position</code> 속성의 id. */
    public static final int OFFSET_POSITION = 295;
    /** Property id of <code>scroll-margin-block-start</code> (the logical scroll margins 296 to 299: block-start, block-end, inline-start, inline-end). <code>scroll-margin-block-start</code> 속성의 id(논리 scroll-margin 296~299: block-start, block-end, inline-start, inline-end). */
    public static final int SCROLL_MARGIN_BLOCK_START = 296;
    /** Property id of <code>scroll-margin-block-end</code>. <code>scroll-margin-block-end</code> 속성의 id. */
    public static final int SCROLL_MARGIN_BLOCK_END = 297;
    /** Property id of <code>scroll-margin-inline-start</code>. <code>scroll-margin-inline-start</code> 속성의 id. */
    public static final int SCROLL_MARGIN_INLINE_START = 298;
    /** Property id of <code>scroll-margin-inline-end</code>. <code>scroll-margin-inline-end</code> 속성의 id. */
    public static final int SCROLL_MARGIN_INLINE_END = 299;
    /** Property id of <code>scroll-padding-block-start</code> (the logical scroll paddings 300 to 303). <code>scroll-padding-block-start</code> 속성의 id(논리 scroll-padding 300~303). */
    public static final int SCROLL_PADDING_BLOCK_START = 300;
    /** Property id of <code>scroll-padding-block-end</code>. <code>scroll-padding-block-end</code> 속성의 id. */
    public static final int SCROLL_PADDING_BLOCK_END = 301;
    /** Property id of <code>scroll-padding-inline-start</code>. <code>scroll-padding-inline-start</code> 속성의 id. */
    public static final int SCROLL_PADDING_INLINE_START = 302;
    /** Property id of <code>scroll-padding-inline-end</code>. <code>scroll-padding-inline-end</code> 속성의 id. */
    public static final int SCROLL_PADDING_INLINE_END = 303;
    /** Property id of <code>font-synthesis-style</code>. <code>font-synthesis-style</code> 속성의 id. */
    public static final int FONT_SYNTHESIS_STYLE = 304;
    /** Property id of <code>font-synthesis-small-caps</code>. <code>font-synthesis-small-caps</code> 속성의 id. */
    public static final int FONT_SYNTHESIS_SMALL_CAPS = 305;
    /** Property id of <code>font-synthesis-position</code>. <code>font-synthesis-position</code> 속성의 id. */
    public static final int FONT_SYNTHESIS_POSITION = 306;
    /** Property id of <code>anchor-name</code> (CSS Anchor Positioning 1). <code>anchor-name</code> 속성의 id(CSS Anchor Positioning 1). */
    public static final int ANCHOR_NAME = 307;
    /** Property id of <code>position-anchor</code>. <code>position-anchor</code> 속성의 id. */
    public static final int POSITION_ANCHOR = 308;
    /** Property id of <code>position-area</code> (CSS Anchor Positioning 1 section 3.1). <code>position-area</code> 속성의 id(CSS Anchor Positioning 1 3.1 절). */
    public static final int POSITION_AREA = 310;
    /** Property id of <code>transition-behavior</code> (CSS Transitions Level 2). <code>transition-behavior</code> 속성의 id(CSS Transitions Level 2). */
    public static final int TRANSITION_BEHAVIOR = 309;

    /** Property id of <code>image-orientation</code>. / <code>image-orientation</code> 속성의 id. */
    public static final int IMAGE_ORIENTATION = 311;
    /** Property id of <code>position-try-fallbacks</code> (CSS Anchor Positioning 1 section 6.1). <code>position-try-fallbacks</code> 속성의 id(CSS Anchor Positioning 1 6.1 절). */
    public static final int POSITION_TRY_FALLBACKS = 312;
    /** Property id of <code>position-try-order</code> (section 6.2). <code>position-try-order</code> 속성의 id(6.2 절). */
    public static final int POSITION_TRY_ORDER = 313;
    /** Property id of <code>position-visibility</code> (section 6.6). <code>position-visibility</code> 속성의 id(6.6 절). */
    public static final int POSITION_VISIBILITY = 314;
    /** Property id of <code>anchor-scope</code> (section 2.2). <code>anchor-scope</code> 속성의 id(2.2 절). */
    public static final int ANCHOR_SCOPE = 315;
    /** Property id of <code>contain-intrinsic-inline-size</code> (CSS Box Sizing 4 section 5.2, a logical longhand). <code>contain-intrinsic-inline-size</code> 속성의 id(CSS Box Sizing 4 5.2 절, 논리 개별 속성). */
    public static final int CONTAIN_INTRINSIC_INLINE_SIZE = 316;
    /** Property id of <code>contain-intrinsic-block-size</code> (a logical longhand). <code>contain-intrinsic-block-size</code> 속성의 id(논리 개별 속성). */
    public static final int CONTAIN_INTRINSIC_BLOCK_SIZE = 317;

    /** Number of longhand properties; ids run from 1 to COUNT. / 개별 속성의 수. id 는 1 부터 COUNT 까지이다. */
    public static final int COUNT = 317;
}
