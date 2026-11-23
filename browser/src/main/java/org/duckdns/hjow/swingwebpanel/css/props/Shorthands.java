package org.duckdns.hjow.swingwebpanel.css.props;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssOperator;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.ParseEnv;
import org.duckdns.hjow.swingwebpanel.css.values.ValueGrammar;

/**
 * The shorthand properties of the engine and their parsers: the four-sided boxes (<code>margin</code>, <code>padding</code>, <code>border-width</code>,
 * <code>border-style</code>, <code>border-color</code>, <code>inset</code>), the any-order shorthands (<code>border</code> and its sides,
 * <code>outline</code>, <code>list-style</code>, <code>text-decoration</code>, <code>column-rule</code>, <code>columns</code>, <code>flex-flow</code>),
 * the pairs (<code>overflow</code>, <code>gap</code>, <code>place-content</code>, <code>place-items</code>, <code>place-self</code>) and the special
 * ones (<code>border-radius</code>, <code>font</code>, <code>flex</code>, <code>background</code>, <code>transition</code>, <code>animation</code>).
 * <p>
 * 엔진의 단축 속성과 그 파서: 네 방향 상자(<code>margin</code>, <code>padding</code>, <code>border-width</code>, <code>border-style</code>, <code>border-color</code>,
 * <code>inset</code>), 순서 무관 단축 속성(<code>border</code> 와 각 변, <code>outline</code>, <code>list-style</code>, <code>text-decoration</code>,
 * <code>column-rule</code>, <code>columns</code>, <code>flex-flow</code>), 쌍(<code>overflow</code>, <code>gap</code>, <code>place-content</code>,
 * <code>place-items</code>, <code>place-self</code>), 특수한 것(<code>border-radius</code>, <code>font</code>, <code>flex</code>, <code>background</code>,
 * <code>transition</code>, <code>animation</code>).
 */
final class Shorthands {

    /** Grammar of a time. / 시간 문법. */
    private static final ValueGrammar G_TIME = ValueGrammar.compile("<time>");
    /** Grammar of an easing function. / 이징 함수 문법. */
    private static final ValueGrammar G_EASING = ValueGrammar.compile("<easing>");
    /** Grammar of a transition property. / 전환 속성 문법. */
    private static final ValueGrammar G_TRANSITION_PROPERTY = ValueGrammar.compile("none | all | <custom-ident>");
    /** The grammar of one value of <code>transition-behavior</code> in the shorthand. 단축 속성 안에서 <code>transition-behavior</code> 의 값 하나의 문법. */
    private static final ValueGrammar G_TRANSITION_BEHAVIOR = ValueGrammar.compile("normal | allow-discrete");
    /** Grammar of an animation name. / 애니메이션 이름 문법. */
    private static final ValueGrammar G_ANIMATION_NAME = ValueGrammar.compile("none | <custom-ident> | <string>");
    /** Grammar of an iteration count. / 반복 횟수 문법. */
    private static final ValueGrammar G_ITERATION = ValueGrammar.compile("infinite | <number [0,inf]>");
    /** Grammar of an animation direction. / 애니메이션 방향 문법. */
    private static final ValueGrammar G_DIRECTION = ValueGrammar.compile("normal | reverse | alternate | alternate-reverse");
    /** Grammar of an animation fill mode. / 애니메이션 채움 모드 문법. */
    private static final ValueGrammar G_FILL = ValueGrammar.compile("none | forwards | backwards | both");
    /** Grammar of an animation play state. / 애니메이션 재생 상태 문법. */
    private static final ValueGrammar G_PLAY = ValueGrammar.compile("running | paused");
    /** Grammar of a background position with an optional size. / 선택적 크기가 붙은 배경 위치 문법. */
    private static final ValueGrammar G_POSITION_SIZE = ValueGrammar.compile("<position> [ / <bg-size> ]?");
    /** Grammar of one or two background boxes. / 배경 상자 하나 또는 둘의 문법. */
    private static final ValueGrammar G_BOXES = ValueGrammar.compile("<box>{1,2}");
    /** Grammar of the font shorthand style part. / font 단축 속성의 스타일 부분 문법. */
    private static final ValueGrammar G_FONT_VARIANT = ValueGrammar.compile("normal | small-caps");
    /** Grammar of the font shorthand stretch part. / font 단축 속성의 폭 부분 문법. */
    private static final ValueGrammar G_FONT_STRETCH = ValueGrammar.compile("normal | ultra-condensed | extra-condensed | condensed | semi-condensed"
            + " | semi-expanded | expanded | extra-expanded | ultra-expanded");

    /**
     * Private constructor: static factory class.
     * <p>
     * 비공개 생성자: 정적 팩토리 클래스이다.
     */
    private Shorthands() {
    }

    /**
     * Creates all shorthands.
     * <p>
     * 모든 단축 속성을 만든다.
     *
     * @return the shorthand list / 단축 속성 목록
     */
    static List<Shorthand> create() {
        List<Shorthand> l = new ArrayList<Shorthand>();
        l.add(new Box4("margin", Prop.MARGIN_TOP, Prop.MARGIN_RIGHT, Prop.MARGIN_BOTTOM, Prop.MARGIN_LEFT));
        l.add(new Box4("padding", Prop.PADDING_TOP, Prop.PADDING_RIGHT, Prop.PADDING_BOTTOM, Prop.PADDING_LEFT));
        l.add(new Box4("border-width", Prop.BORDER_TOP_WIDTH, Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_LEFT_WIDTH));
        l.add(new Box4("border-style", Prop.BORDER_TOP_STYLE, Prop.BORDER_RIGHT_STYLE, Prop.BORDER_BOTTOM_STYLE, Prop.BORDER_LEFT_STYLE));
        l.add(new Box4("border-color", Prop.BORDER_TOP_COLOR, Prop.BORDER_RIGHT_COLOR, Prop.BORDER_BOTTOM_COLOR, Prop.BORDER_LEFT_COLOR));
        l.add(new Box4("inset", Prop.TOP, Prop.RIGHT, Prop.BOTTOM, Prop.LEFT));
        int[] sideIds = {Prop.BORDER_TOP_WIDTH, Prop.BORDER_TOP_STYLE, Prop.BORDER_TOP_COLOR};
        int[][] allSides = {{Prop.BORDER_TOP_WIDTH, Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_LEFT_WIDTH},
            {Prop.BORDER_TOP_STYLE, Prop.BORDER_RIGHT_STYLE, Prop.BORDER_BOTTOM_STYLE, Prop.BORDER_LEFT_STYLE},
            {Prop.BORDER_TOP_COLOR, Prop.BORDER_RIGHT_COLOR, Prop.BORDER_BOTTOM_COLOR, Prop.BORDER_LEFT_COLOR}};
        int[] imageReset = {Prop.BORDER_IMAGE_SOURCE, Prop.BORDER_IMAGE_SLICE, Prop.BORDER_IMAGE_WIDTH, Prop.BORDER_IMAGE_OUTSET,
            Prop.BORDER_IMAGE_REPEAT};
        l.add(new AnyOrder("border", sideIds, allSides, imageReset));
        l.add(new AnyOrder("border-top", sideIds, side(Prop.BORDER_TOP_WIDTH, Prop.BORDER_TOP_STYLE, Prop.BORDER_TOP_COLOR), new int[0]));
        l.add(new AnyOrder("border-right", sideIds, side(Prop.BORDER_RIGHT_WIDTH, Prop.BORDER_RIGHT_STYLE, Prop.BORDER_RIGHT_COLOR), new int[0]));
        l.add(new AnyOrder("border-bottom", sideIds, side(Prop.BORDER_BOTTOM_WIDTH, Prop.BORDER_BOTTOM_STYLE, Prop.BORDER_BOTTOM_COLOR),
                new int[0]));
        l.add(new AnyOrder("border-left", sideIds, side(Prop.BORDER_LEFT_WIDTH, Prop.BORDER_LEFT_STYLE, Prop.BORDER_LEFT_COLOR), new int[0]));
        l.add(new AnyOrder("outline", new int[] {Prop.OUTLINE_COLOR, Prop.OUTLINE_STYLE, Prop.OUTLINE_WIDTH}));
        l.add(new AnyOrder("list-style", new int[] {Prop.LIST_STYLE_TYPE, Prop.LIST_STYLE_POSITION, Prop.LIST_STYLE_IMAGE}));
        l.add(new AnyOrder("text-decoration", new int[] {Prop.TEXT_DECORATION_LINE, Prop.TEXT_DECORATION_STYLE, Prop.TEXT_DECORATION_COLOR,
            Prop.TEXT_DECORATION_THICKNESS}));
        l.add(new AnyOrder("column-rule", new int[] {Prop.COLUMN_RULE_WIDTH, Prop.COLUMN_RULE_STYLE, Prop.COLUMN_RULE_COLOR}));
        l.add(new AnyOrder("columns", new int[] {Prop.COLUMN_WIDTH, Prop.COLUMN_COUNT}));
        l.add(new AnyOrder("flex-flow", new int[] {Prop.FLEX_DIRECTION, Prop.FLEX_WRAP}));
        l.add(new Pair("overflow", Prop.OVERFLOW_X, Prop.OVERFLOW_Y));
        l.add(new Pair("gap", Prop.ROW_GAP, Prop.COLUMN_GAP));
        l.add(new Pair("place-content", Prop.ALIGN_CONTENT, Prop.JUSTIFY_CONTENT));
        l.add(new Pair("place-items", Prop.ALIGN_ITEMS, Prop.JUSTIFY_ITEMS));
        l.add(new Pair("place-self", Prop.ALIGN_SELF, Prop.JUSTIFY_SELF));
        l.add(new BorderRadius());
        l.add(new Font());
        l.add(new Flex());
        l.add(new GridPlacement("grid-row", new int[] {Prop.GRID_ROW_START, Prop.GRID_ROW_END}, false));
        l.add(new GridPlacement("grid-column", new int[] {Prop.GRID_COLUMN_START, Prop.GRID_COLUMN_END}, false));
        l.add(new GridPlacement("grid-area", new int[] {Prop.GRID_ROW_START, Prop.GRID_COLUMN_START, Prop.GRID_ROW_END, Prop.GRID_COLUMN_END}, true));
        l.add(new GridTemplate());
        l.add(new Grid());
        l.add(new Background());
        l.add(new Mask());
        l.add(new Transition());
        l.add(new Animation());
        // 논리 단축 속성(CSS Logical Properties 1 4 절): 값 하나나 둘이 start 와 end 의 개별 속성에 간다.
        l.add(new Pair("margin-block", Prop.MARGIN_BLOCK_START, Prop.MARGIN_BLOCK_END));
        l.add(new Pair("margin-inline", Prop.MARGIN_INLINE_START, Prop.MARGIN_INLINE_END));
        l.add(new Pair("padding-block", Prop.PADDING_BLOCK_START, Prop.PADDING_BLOCK_END));
        l.add(new Pair("padding-inline", Prop.PADDING_INLINE_START, Prop.PADDING_INLINE_END));
        l.add(new Pair("inset-block", Prop.INSET_BLOCK_START, Prop.INSET_BLOCK_END));
        l.add(new Pair("inset-inline", Prop.INSET_INLINE_START, Prop.INSET_INLINE_END));
        l.add(new Pair("border-block-width", Prop.BORDER_BLOCK_START_WIDTH, Prop.BORDER_BLOCK_END_WIDTH));
        l.add(new Pair("border-inline-width", Prop.BORDER_INLINE_START_WIDTH, Prop.BORDER_INLINE_END_WIDTH));
        l.add(new Pair("border-block-style", Prop.BORDER_BLOCK_START_STYLE, Prop.BORDER_BLOCK_END_STYLE));
        l.add(new Pair("border-inline-style", Prop.BORDER_INLINE_START_STYLE, Prop.BORDER_INLINE_END_STYLE));
        l.add(new Pair("border-block-color", Prop.BORDER_BLOCK_START_COLOR, Prop.BORDER_BLOCK_END_COLOR));
        l.add(new Pair("border-inline-color", Prop.BORDER_INLINE_START_COLOR, Prop.BORDER_INLINE_END_COLOR));
        l.add(new AnyOrder("border-block-start", sideIds, side(Prop.BORDER_BLOCK_START_WIDTH, Prop.BORDER_BLOCK_START_STYLE, Prop.BORDER_BLOCK_START_COLOR),
                new int[0]));
        l.add(new AnyOrder("border-block-end", sideIds, side(Prop.BORDER_BLOCK_END_WIDTH, Prop.BORDER_BLOCK_END_STYLE, Prop.BORDER_BLOCK_END_COLOR),
                new int[0]));
        l.add(new AnyOrder("border-inline-start", sideIds, side(Prop.BORDER_INLINE_START_WIDTH, Prop.BORDER_INLINE_START_STYLE,
                Prop.BORDER_INLINE_START_COLOR), new int[0]));
        l.add(new AnyOrder("border-inline-end", sideIds, side(Prop.BORDER_INLINE_END_WIDTH, Prop.BORDER_INLINE_END_STYLE, Prop.BORDER_INLINE_END_COLOR),
                new int[0]));
        l.add(new AnyOrder("border-block", sideIds, new int[][] {{Prop.BORDER_BLOCK_START_WIDTH, Prop.BORDER_BLOCK_END_WIDTH},
            {Prop.BORDER_BLOCK_START_STYLE, Prop.BORDER_BLOCK_END_STYLE}, {Prop.BORDER_BLOCK_START_COLOR, Prop.BORDER_BLOCK_END_COLOR}}, new int[0]));
        l.add(new AnyOrder("border-inline", sideIds, new int[][] {{Prop.BORDER_INLINE_START_WIDTH, Prop.BORDER_INLINE_END_WIDTH},
            {Prop.BORDER_INLINE_START_STYLE, Prop.BORDER_INLINE_END_STYLE}, {Prop.BORDER_INLINE_START_COLOR, Prop.BORDER_INLINE_END_COLOR}}, new int[0]));
        // 컨테이너, 스크롤, 텍스트 단축 속성(CSS-H): 컨테이너 질의, 스크롤 스냅, 오버스크롤, 텍스트 줄바꿈과 강조
        l.add(new Container());
        l.add(new ContainIntrinsicSize());
        l.add(new Box4("scroll-padding", Prop.SCROLL_PADDING_TOP, Prop.SCROLL_PADDING_RIGHT, Prop.SCROLL_PADDING_BOTTOM, Prop.SCROLL_PADDING_LEFT));
        l.add(new Box4("scroll-margin", Prop.SCROLL_MARGIN_TOP, Prop.SCROLL_MARGIN_RIGHT, Prop.SCROLL_MARGIN_BOTTOM, Prop.SCROLL_MARGIN_LEFT));
        // 논리 scroll-margin/scroll-padding 의 단축 속성(CSS Scroll Snap 1): 값 하나 또는 둘(start, end).
        l.add(new Pair("scroll-margin-block", Prop.SCROLL_MARGIN_BLOCK_START, Prop.SCROLL_MARGIN_BLOCK_END));
        l.add(new Pair("scroll-margin-inline", Prop.SCROLL_MARGIN_INLINE_START, Prop.SCROLL_MARGIN_INLINE_END));
        l.add(new Pair("scroll-padding-block", Prop.SCROLL_PADDING_BLOCK_START, Prop.SCROLL_PADDING_BLOCK_END));
        l.add(new Pair("scroll-padding-inline", Prop.SCROLL_PADDING_INLINE_START, Prop.SCROLL_PADDING_INLINE_END));
        l.add(new Pair("overscroll-behavior", Prop.OVERSCROLL_BEHAVIOR_X, Prop.OVERSCROLL_BEHAVIOR_Y));
        l.add(new AnyOrder("text-wrap", new int[] {Prop.TEXT_WRAP_MODE, Prop.TEXT_WRAP_STYLE}));
        l.add(new AnyOrder("text-emphasis", new int[] {Prop.TEXT_EMPHASIS_STYLE, Prop.TEXT_EMPHASIS_COLOR}));
        // font-variant 는 여섯 개의 font-variant-* 개별 속성의 단축 속성이다(CSS Fonts 4 6.12 절): normal 은 모두 normal, none 은 ligatures 만 none.
        l.add(new FontVariant());
        l.add(new Offset());
        l.add(new FontSynthesis());
        l.add(new PositionTry());
        l.add(new MaskBorder());
        l.add(new MaskBorder("border-image", new int[] {Prop.BORDER_IMAGE_SOURCE, Prop.BORDER_IMAGE_SLICE, Prop.BORDER_IMAGE_WIDTH, Prop.BORDER_IMAGE_OUTSET,
            Prop.BORDER_IMAGE_REPEAT}));
        return l;
    }

    /**
     * Builds the fan-out table of a one-sided border shorthand.
     * <p>
     * 한 변 테두리 단축 속성의 분배 표를 만든다.
     *
     * @param w width id / 두께 id
     * @param s style id / 스타일 id
     * @param c color id / 색 id
     * @return the table / 표
     */
    private static int[][] side(int w, int s, int c) {
        return new int[][] {{w}, {s}, {c}};
    }

    /**
     * Matches component values against a longhand grammar.
     * <p>
     * 컴포넌트 값을 개별 속성 문법에 맞춘다.
     *
     * @param reg   registry / 등록부
     * @param id    longhand id / 개별 속성 id
     * @param items the values / 값
     * @param env   environment / 환경
     * @return the typed value or null / 값 또는 null
     */
    private static CssValue match(PropertyRegistry reg, int id, List<ComponentValue> items, ParseEnv env) {
        return reg.byId(id).getGrammar().match(items, env);
    }

    /**
     * Wraps one component value in a list.
     * <p>
     * 컴포넌트 값 하나를 목록으로 감싼다.
     *
     * @param v the value / 값
     * @return a one-item list / 한 항목 목록
     */
    private static List<ComponentValue> one(ComponentValue v) {
        List<ComponentValue> l = new ArrayList<ComponentValue>();
        l.add(v);
        return l;
    }

    /**
     * Returns the first layer value of a property's initial value (initial values of layered properties are comma lists).
     * <p>
     * 속성의 초기값에서 첫 층의 값을 반환한다(층 속성의 초기값은 쉼표 목록이다).
     *
     * @param reg registry / 등록부
     * @param id  longhand id / 개별 속성 id
     * @return the single-layer initial value / 한 층의 초기값
     */
    private static CssValue layerInitial(PropertyRegistry reg, int id) {
        CssValue v = reg.byId(id).getInitial();
        return v instanceof CssList && ((CssList) v).isComma() ? ((CssList) v).get(0) : v;
    }

    /**
     * Splits prepared items at top-level commas.
     * <p>
     * 준비한 항목을 최상위 쉼표에서 나눈다.
     *
     * @param items prepared items / 준비한 항목
     * @return the layers / 층들
     */
    private static List<List<ComponentValue>> layers(List<ComponentValue> items) {
        List<List<ComponentValue>> out = new ArrayList<List<ComponentValue>>();
        List<ComponentValue> cur = new ArrayList<ComponentValue>();
        // 쉼표를 만날 때마다 지금까지 모은 항목을 한 층으로 확정한다.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof CssToken && ((CssToken) items.get(i)).is(CssToken.COMMA)) {
                out.add(cur);
                cur = new ArrayList<ComponentValue>();
            } else {
                cur.add(items.get(i));
            }
        }
        out.add(cur);
        return out;
    }

    /**
     * Tells whether an item is the given delimiter.
     * <p>
     * 항목이 주어진 구분 문자인지 알려 준다.
     *
     * @param v the item / 항목
     * @param c the delimiter / 구분 문자
     * @return true if it is / 맞으면 true
     */
    private static boolean isDelim(ComponentValue v, char c) {
        return v instanceof CssToken && ((CssToken) v).isDelim(c);
    }

    /**
     * Tells whether an item is a plain number token.
     * <p>
     * 항목이 일반 숫자 토큰인지 알려 준다.
     *
     * @param v the item / 항목
     * @return true for number tokens / 숫자 토큰이면 true
     */
    private static boolean isNumber(ComponentValue v) {
        return v instanceof CssToken && ((CssToken) v).is(CssToken.NUMBER);
    }

    /**
     * Joins the specified values of four sides into the shortest form (the CSSOM serialization of a box shorthand).
     * <p>
     * 네 방향의 지정 값을 가장 짧은 형태로 합친다(상자 단축 속성의 CSSOM 직렬화).
     *
     * @param t top / 위
     * @param r right / 오른쪽
     * @param b bottom / 아래
     * @param l left / 왼쪽
     * @return the text / 문자열
     */
    static String box4Text(String t, String r, String b, String l) {
        if (t.equals(b) && r.equals(l)) {
            return t.equals(r) ? t : t + " " + r;
        }
        return r.equals(l) ? t + " " + r + " " + b : t + " " + r + " " + b + " " + l;
    }

    /**
     * A four-sided box shorthand: one to four values for top, right, bottom and left.
     * <p>
     * 네 방향 상자 단축 속성: 위, 오른쪽, 아래, 왼쪽에 대한 값 하나에서 넷.
     */
    static final class Box4 extends Shorthand {
        /** The longhand ids in the order top, right, bottom, left. / 위, 오른쪽, 아래, 왼쪽 순서의 개별 속성 id. */
        private final int[] ids;

        /**
         * Creates a box shorthand.
         * <p>
         * 상자 단축 속성을 만든다.
         *
         * @param name   the name / 이름
         * @param top    top id / 위 id
         * @param right  right id / 오른쪽 id
         * @param bottom bottom id / 아래 id
         * @param left   left id / 왼쪽 id
         */
        Box4(String name, int top, int right, int bottom, int left) {
            super(name, new int[] {top, right, bottom, left});
            this.ids = new int[] {top, right, bottom, left};
        }

        /**
         * Distributes one to four values over the four sides.
         * <p>
         * 값 하나에서 넷을 네 방향에 나눈다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int n = items.size();
            if (n < 1 || n > 4) {
                return false;
            }
            CssValue[] v = new CssValue[4];
            // 각 값이 방향 속성의 문법을 만족해야 한다.
            for (int i = 0; i < n; i++) {
                v[i] = match(reg, ids[0], one(items.get(i)), env);
                if (v[i] == null) {
                    return false;
                }
            }
            CssValue t = v[0];
            CssValue r = n > 1 ? v[1] : t;
            CssValue b = n > 2 ? v[2] : t;
            CssValue l = n > 3 ? v[3] : r;
            out.put(Integer.valueOf(ids[0]), t);
            out.put(Integer.valueOf(ids[1]), r);
            out.put(Integer.valueOf(ids[2]), b);
            out.put(Integer.valueOf(ids[3]), l);
            return true;
        }

        /**
         * Serializes the four sides in the shortest form.
         * <p>
         * 네 방향을 가장 짧은 형태로 직렬화한다.
         *
         * @param values longhand values / 개별 속성 값
         * @return the text or null if a side is missing / 문자열. 한 방향이라도 없으면 null
         */
        public String serialize(Map<Integer, CssValue> values) {
            CssValue[] v = new CssValue[4];
            // 네 방향의 값이 모두 있어야 한다.
            for (int i = 0; i < 4; i++) {
                v[i] = values.get(Integer.valueOf(ids[i]));
                if (v[i] == null) {
                    return null;
                }
            }
            return box4Text(v[0].toCssText(), v[1].toCssText(), v[2].toCssText(), v[3].toCssText());
        }
    }

    /**
     * An any-order shorthand: its parts may be written in any order, each at most once. Parts that are not written are set to
     * <code>initial</code>. A part can fan out to several longhands (<code>border</code> sets four sides).
     * <p>
     * 순서 무관 단축 속성: 부분을 어떤 순서로든 한 번씩 쓸 수 있다. 쓰지 않은 부분은 <code>initial</code> 로 정한다. 한 부분이 여러 개별 속성으로 퍼질 수 있다
     * (<code>border</code> 는 네 변을 정한다).
     */
    static final class AnyOrder extends Shorthand {
        /** The longhand whose grammar matches each part. / 각 부분을 맞추는 문법의 개별 속성. */
        private final int[] parts;
        /** The longhands that receive each part. / 각 부분을 받는 개별 속성. */
        private final int[][] fanOut;
        /** Extra longhands reset to initial. / initial 로 되돌리는 추가 개별 속성. */
        private final int[] resets;

        /**
         * Creates a simple any-order shorthand where each part sets exactly its own longhand.
         * <p>
         * 각 부분이 자기 개별 속성만 정하는 단순한 순서 무관 단축 속성을 만든다.
         *
         * @param name  the name / 이름
         * @param parts the longhand ids / 개별 속성 id
         */
        AnyOrder(String name, int[] parts) {
            this(name, parts, single(parts), new int[0]);
        }

        /**
         * Creates an any-order shorthand with fan-out.
         * <p>
         * 분배가 있는 순서 무관 단축 속성을 만든다.
         *
         * @param name   the name / 이름
         * @param parts  grammar sources / 문법 출처
         * @param fanOut receivers per part / 부분별 받는 쪽
         * @param resets extra resets / 추가 초기화
         */
        AnyOrder(String name, int[] parts, int[][] fanOut, int[] resets) {
            super(name, all(fanOut, resets));
            this.parts = parts.clone();
            this.fanOut = fanOut;
            this.resets = resets.clone();
        }

        /**
         * Builds the one-to-one fan-out table.
         * <p>
         * 일대일 분배 표를 만든다.
         *
         * @param parts ids / id
         * @return the table / 표
         */
        private static int[][] single(int[] parts) {
            int[][] t = new int[parts.length][];
            // 부분마다 자기 자신만 받는 한 칸짜리 배열을 만든다.
            for (int i = 0; i < parts.length; i++) {
                t[i] = new int[] {parts[i]};
            }
            return t;
        }

        /**
         * Collects every longhand the shorthand sets.
         * <p>
         * 단축 속성이 정하는 모든 개별 속성을 모은다.
         *
         * @param fanOut receivers per part / 부분별 받는 쪽
         * @param resets extra resets / 추가 초기화
         * @return all ids / 모든 id
         */
        private static int[] all(int[][] fanOut, int[] resets) {
            List<Integer> l = new ArrayList<Integer>();
            // 분배 표와 추가 초기화의 id 를 모두 모은다.
            for (int i = 0; i < fanOut.length; i++) {
                for (int j = 0; j < fanOut[i].length; j++) {
                    l.add(Integer.valueOf(fanOut[i][j]));
                }
            }
            // resets 의 항목을 하나씩 처리한다.
            for (int i = 0; i < resets.length; i++) {
                l.add(Integer.valueOf(resets[i]));
            }
            int[] a = new int[l.size()];
            // a 의 항목을 하나씩 처리한다.
            for (int i = 0; i < a.length; i++) {
                a[i] = l.get(i).intValue();
            }
            return a;
        }

        /**
         * Matches the parts greedily in any order.
         * <p>
         * 부분을 순서에 상관없이 탐욕적으로 맞춘다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if every component was consumed / 모든 구성 요소를 소비했으면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            if (items.isEmpty()) {
                return false;
            }
            CssValue[] got = new CssValue[parts.length];
            int pos = 0;
            // 남은 구성 요소가 없을 때까지, 아직 쓰지 않은 부분 중 하나가 가장 앞에서부터 일치하는지 찾는다.
            while (pos < items.size()) {
                boolean progress = false;
                // parts 의 항목을 하나씩 처리한다.
                for (int i = 0; i < parts.length && !progress; i++) {
                    if (got[i] != null) {
                        continue;
                    }
                    ValueGrammar.Prefix p = reg.byId(parts[i]).getGrammar().matchPrefix(items, pos, env);
                    if (p != null && p.end > pos) {
                        got[i] = p.value;
                        pos = p.end;
                        progress = true;
                    }
                }
                if (!progress) {
                    return false;
                }
            }
            // 쓰인 부분은 값을, 쓰이지 않은 부분은 initial 을 모든 받는 쪽에 쓴다.
            for (int i = 0; i < parts.length; i++) {
                for (int j = 0; j < fanOut[i].length; j++) {
                    out.put(Integer.valueOf(fanOut[i][j]), got[i] != null ? got[i] : CssKeyword.INITIAL);
                }
            }
            // resets 의 항목을 하나씩 처리한다.
            for (int i = 0; i < resets.length; i++) {
                out.put(Integer.valueOf(resets[i]), CssKeyword.INITIAL);
            }
            return true;
        }
    }

    /**
     * A pair shorthand: one or two values for a first and a second longhand (<code>overflow</code>, <code>gap</code>, <code>place-*</code>).
     * <p>
     * 쌍 단축 속성: 첫째와 둘째 개별 속성에 대한 값 하나 또는 둘(<code>overflow</code>, <code>gap</code>, <code>place-*</code>).
     */
    static final class Pair extends Shorthand {
        /** First longhand. / 첫 개별 속성. */
        private final int first;
        /** Second longhand. / 둘째 개별 속성. */
        private final int second;

        /**
         * Creates a pair shorthand.
         * <p>
         * 쌍 단축 속성을 만든다.
         *
         * @param name   the name / 이름
         * @param first  first id / 첫 id
         * @param second second id / 둘째 id
         */
        Pair(String name, int first, int second) {
            super(name, new int[] {first, second});
            this.first = first;
            this.second = second;
        }

        /**
         * Reads one or two values.
         * <p>
         * 값 하나 또는 둘을 읽는다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int n = items.size();
            if (n < 1 || n > 2) {
                return false;
            }
            ComponentValue a = items.get(0);
            ComponentValue b = items.get(n - 1);
            CssValue v1 = match(reg, first, one(a), env);
            CssValue v2 = match(reg, second, one(b), env);
            if (v1 == null || v2 == null) {
                return false;
            }
            out.put(Integer.valueOf(first), v1);
            out.put(Integer.valueOf(second), v2);
            return true;
        }

        /**
         * Serializes the pair, collapsing equal values.
         * <p>
         * 쌍을 직렬화하며 같은 값은 하나로 합친다.
         *
         * @param values longhand values / 개별 속성 값
         * @return the text or null if a value is missing / 문자열. 값이 없으면 null
         */
        public String serialize(Map<Integer, CssValue> values) {
            CssValue a = values.get(Integer.valueOf(first));
            CssValue b = values.get(Integer.valueOf(second));
            if (a == null || b == null) {
                return null;
            }
            return a.toCssText().equals(b.toCssText()) ? a.toCssText() : a.toCssText() + " " + b.toCssText();
        }
    }

    /**
     * <code>contain-intrinsic-size</code> (CSS Box Sizing 4 section 5.2): <code>[ auto? [ none | &lt;length [0,&#8734;]&gt; ] ]{1,2}</code>, the first value for
     * <code>contain-intrinsic-width</code> and the second (or the first again) for <code>contain-intrinsic-height</code>; every value is one or two tokens.
     * <p>
     * <code>contain-intrinsic-size</code>(CSS Box Sizing 4 5.2 절): <code>[ auto? [ none | &lt;length [0,&#8734;]&gt; ] ]{1,2}</code>. 첫 값은
     * <code>contain-intrinsic-width</code>, 둘째 값(없으면 첫 값)은 <code>contain-intrinsic-height</code> 이며 값마다 토큰 하나나 둘이다.
     */
    static final class ContainIntrinsicSize extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        ContainIntrinsicSize() {
            super("contain-intrinsic-size", new int[] {Prop.CONTAIN_INTRINSIC_WIDTH, Prop.CONTAIN_INTRINSIC_HEIGHT});
        }

        /**
         * Reads one or two values, each an optional <code>auto</code> followed by <code>none</code> or a length.
         * <p>
         * 값 하나나 둘을 읽는다. 각각은 선택적인 <code>auto</code> 뒤의 <code>none</code> 이나 길이이다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            List<List<ComponentValue>> groups = new ArrayList<List<ComponentValue>>();
            List<ComponentValue> cur = new ArrayList<ComponentValue>();
            // 토큰을 차례로 보며 auto 는 다음 토큰과 묶고, none 이나 길이에서 값 하나를 끝낸다.
            for (int i = 0; i < items.size(); i++) {
                ComponentValue c = items.get(i);
                boolean auto = c instanceof org.duckdns.hjow.swingwebpanel.css.syntax.CssToken && ((org.duckdns.hjow.swingwebpanel.css.syntax.CssToken) c).isIdent("auto");
                if (auto && !cur.isEmpty()) {
                    return false;
                }
                cur.add(c);
                if (!auto) {
                    groups.add(cur);
                    cur = new ArrayList<ComponentValue>();
                }
            }
            if (!cur.isEmpty() || groups.isEmpty() || groups.size() > 2) {
                return false;
            }
            CssValue w = match(reg, Prop.CONTAIN_INTRINSIC_WIDTH, groups.get(0), env);
            CssValue h = match(reg, Prop.CONTAIN_INTRINSIC_HEIGHT, groups.get(groups.size() - 1), env);
            if (w == null || h == null) {
                return false;
            }
            out.put(Integer.valueOf(Prop.CONTAIN_INTRINSIC_WIDTH), w);
            out.put(Integer.valueOf(Prop.CONTAIN_INTRINSIC_HEIGHT), h);
            return true;
        }

        /**
         * Serializes the two values, collapsing equal values.
         * <p>
         * 두 값을 직렬화하며 같은 값은 하나로 합친다.
         *
         * @param values longhand values / 개별 속성 값
         * @return the text or null if a value is missing / 문자열. 값이 없으면 null
         */
        public String serialize(Map<Integer, CssValue> values) {
            CssValue a = values.get(Integer.valueOf(Prop.CONTAIN_INTRINSIC_WIDTH));
            CssValue b = values.get(Integer.valueOf(Prop.CONTAIN_INTRINSIC_HEIGHT));
            if (a == null || b == null) {
                return null;
            }
            return a.toCssText().equals(b.toCssText()) ? a.toCssText() : a.toCssText() + " " + b.toCssText();
        }
    }

    /**
     * <code>border-radius</code>: one to four horizontal radii, optionally followed by a slash and one to four vertical radii.
     * <p>
     * <code>border-radius</code>: 가로 반지름 하나에서 넷, 그 뒤에 슬래시와 세로 반지름 하나에서 넷이 올 수 있다.
     */
    static final class BorderRadius extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        BorderRadius() {
            super("border-radius", new int[] {Prop.BORDER_TOP_LEFT_RADIUS, Prop.BORDER_TOP_RIGHT_RADIUS, Prop.BORDER_BOTTOM_RIGHT_RADIUS,
                Prop.BORDER_BOTTOM_LEFT_RADIUS});
        }

        /**
         * Reads the radii and distributes them over the four corners.
         * <p>
         * 반지름을 읽어 네 모서리에 나눈다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int slash = -1;
            // 슬래시 위치를 찾는다.
            for (int i = 0; i < items.size(); i++) {
                if (isDelim(items.get(i), '/')) {
                    if (slash >= 0) {
                        return false;
                    }
                    slash = i;
                }
            }
            List<ComponentValue> hs = slash < 0 ? items : items.subList(0, slash);
            List<ComponentValue> vs = slash < 0 ? null : items.subList(slash + 1, items.size());
            CssValue[] h = corners(hs, reg, env);
            CssValue[] v = vs == null ? null : corners(vs, reg, env);
            if (h == null || (vs != null && v == null)) {
                return false;
            }
            int[] ids = getLonghands();
            // 모서리마다 가로 값과 (있으면) 세로 값을 묶는다.
            for (int i = 0; i < 4; i++) {
                if (v == null) {
                    out.put(Integer.valueOf(ids[i]), h[i]);
                } else {
                    List<CssValue> pair = new ArrayList<CssValue>();
                    pair.add(h[i]);
                    pair.add(v[i]);
                    out.put(Integer.valueOf(ids[i]), new CssList(pair, false));
                }
            }
            return true;
        }

        /**
         * Reads one to four radii and maps them to top-left, top-right, bottom-right, bottom-left.
         * <p>
         * 반지름 하나에서 넷을 읽어 왼쪽 위, 오른쪽 위, 오른쪽 아래, 왼쪽 아래에 대응시킨다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @return four values or null / 값 넷 또는 null
         */
        private static CssValue[] corners(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env) {
            int n = items.size();
            if (n < 1 || n > 4) {
                return null;
            }
            CssValue[] v = new CssValue[n];
            ValueGrammar g = reg.get("lp-nn");
            // 각 값이 음수 아닌 길이 또는 백분율이어야 한다.
            for (int i = 0; i < n; i++) {
                v[i] = g.match(one(items.get(i)), env);
                if (v[i] == null) {
                    return null;
                }
            }
            CssValue tl = v[0];
            CssValue tr = n > 1 ? v[1] : tl;
            CssValue br = n > 2 ? v[2] : tl;
            CssValue bl = n > 3 ? v[3] : tr;
            return new CssValue[] {tl, tr, br, bl};
        }
    }

    /**
     * The <code>font</code> shorthand: optional style, variant, weight and stretch keywords in any order, a required size, an optional
     * <code>/ line-height</code> and a required family list; or a system font keyword.
     * <p>
     * <code>font</code> 단축 속성: 순서 무관한 선택적 스타일, 변형, 두께, 폭 키워드, 필수 크기, 선택적 <code>/ line-height</code>, 필수 글꼴 목록. 또는 시스템 글꼴 키워드.
     */
    static final class Font extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Font() {
            super("font", new int[] {Prop.FONT_STYLE, Prop.FONT_VARIANT_CAPS, Prop.FONT_WEIGHT, Prop.FONT_STRETCH, Prop.FONT_SIZE, Prop.LINE_HEIGHT,
                Prop.FONT_FAMILY, Prop.FONT_SIZE_ADJUST, Prop.FONT_KERNING, Prop.FONT_FEATURE_SETTINGS, Prop.FONT_VARIANT_LIGATURES,
                Prop.FONT_VARIANT_NUMERIC, Prop.FONT_VARIANT_EAST_ASIAN, Prop.FONT_VARIANT_POSITION, Prop.FONT_VARIANT_ALTERNATES});
        }

        /**
         * Parses the font value.
         * <p>
         * font 값을 파싱한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int n = items.size();
            if (n == 0) {
                return false;
            }
            if (n == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).is(CssToken.IDENT)) {
                String k = CssKeyword.lower(((CssToken) items.get(0)).getValue());
                if (k.equals("caption") || k.equals("icon") || k.equals("menu") || k.equals("message-box") || k.equals("small-caption")
                        || k.equals("status-bar")) {
                    return system(reg, env, out);
                }
            }
            ValueGrammar[] pre = {reg.byId(Prop.FONT_STYLE).getGrammar(), G_FONT_VARIANT, reg.byId(Prop.FONT_WEIGHT).getGrammar(), G_FONT_STRETCH};
            int[] preIds = {Prop.FONT_STYLE, Prop.FONT_VARIANT_CAPS, Prop.FONT_WEIGHT, Prop.FONT_STRETCH};
            CssValue[] got = new CssValue[4];
            int pos = 0;
            boolean progress = true;
            // 글꼴 크기가 나오기 전의 선택적 키워드를 순서에 상관없이 읽는다.
            while (progress && pos < n) {
                progress = false;
                // 항목 의 항목을 하나씩 처리한다.
                for (int i = 0; i < 4 && !progress; i++) {
                    if (got[i] != null) {
                        continue;
                    }
                    ValueGrammar.Prefix p = pre[i].matchPrefix(items, pos, env);
                    if (p != null && p.end > pos) {
                        got[i] = p.value;
                        pos = p.end;
                        progress = true;
                    }
                }
            }
            if (pos >= n) {
                return false;
            }
            ValueGrammar.Prefix size = reg.byId(Prop.FONT_SIZE).getGrammar().matchPrefix(items, pos, env);
            if (size == null || size.end != pos + 1) {
                return false;
            }
            pos = size.end;
            CssValue lh = CssKeyword.INITIAL;
            if (pos < n && isDelim(items.get(pos), '/')) {
                pos++;
                if (pos >= n) {
                    return false;
                }
                CssValue l = match(reg, Prop.LINE_HEIGHT, one(items.get(pos)), env);
                if (l == null) {
                    return false;
                }
                lh = l;
                pos++;
            }
            if (pos >= n) {
                return false;
            }
            CssValue family = match(reg, Prop.FONT_FAMILY, items.subList(pos, n), env);
            if (family == null) {
                return false;
            }
            // 쓰지 않은 앞쪽 키워드는 initial 로 둔다.
            for (int i = 0; i < 4; i++) {
                out.put(Integer.valueOf(preIds[i]), got[i] != null ? got[i] : CssKeyword.INITIAL);
            }
            out.put(Integer.valueOf(Prop.FONT_SIZE), size.value);
            out.put(Integer.valueOf(Prop.LINE_HEIGHT), lh);
            out.put(Integer.valueOf(Prop.FONT_FAMILY), family);
            out.put(Integer.valueOf(Prop.FONT_SIZE_ADJUST), CssKeyword.INITIAL);
            out.put(Integer.valueOf(Prop.FONT_KERNING), CssKeyword.INITIAL);
            out.put(Integer.valueOf(Prop.FONT_FEATURE_SETTINGS), CssKeyword.INITIAL);
            // font 단축 속성은 font-variant 의 나머지 개별 속성도 initial 로 되돌린다(CSS Fonts 4 font 단축 속성).
            out.put(Integer.valueOf(Prop.FONT_VARIANT_LIGATURES), CssKeyword.INITIAL);
            out.put(Integer.valueOf(Prop.FONT_VARIANT_NUMERIC), CssKeyword.INITIAL);
            out.put(Integer.valueOf(Prop.FONT_VARIANT_EAST_ASIAN), CssKeyword.INITIAL);
            out.put(Integer.valueOf(Prop.FONT_VARIANT_POSITION), CssKeyword.INITIAL);
            out.put(Integer.valueOf(Prop.FONT_VARIANT_ALTERNATES), CssKeyword.INITIAL);
            return true;
        }

        /**
         * Sets the values of a system font keyword: a 13px sans-serif font.
         * <p>
         * 시스템 글꼴 키워드의 값을 정한다: 13px sans-serif 글꼴.
         *
         * @param reg registry / 등록부
         * @param env environment / 환경
         * @param out result / 결과
         * @return always true / 항상 true
         */
        private static boolean system(PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int[] ids = getIds();
            // 모든 하위 속성을 initial 로 둔 뒤 크기와 글꼴 목록만 정한다.
            for (int i = 0; i < ids.length; i++) {
                out.put(Integer.valueOf(ids[i]), CssKeyword.INITIAL);
            }
            out.put(Integer.valueOf(Prop.FONT_SIZE), match(reg, Prop.FONT_SIZE, one(parseToken("13px")), env));
            out.put(Integer.valueOf(Prop.FONT_FAMILY), match(reg, Prop.FONT_FAMILY, one(parseToken("sans-serif")), env));
            return true;
        }

        /**
         * Returns the ids of the longhands set by the font shorthand.
         * <p>
         * font 단축 속성이 정하는 개별 속성 id 를 반환한다.
         *
         * @return the ids / id
         */
        private static int[] getIds() {
            return new int[] {Prop.FONT_STYLE, Prop.FONT_VARIANT_CAPS, Prop.FONT_WEIGHT, Prop.FONT_STRETCH, Prop.FONT_SIZE, Prop.LINE_HEIGHT,
                Prop.FONT_FAMILY, Prop.FONT_SIZE_ADJUST, Prop.FONT_KERNING, Prop.FONT_FEATURE_SETTINGS, Prop.FONT_VARIANT_LIGATURES,
                Prop.FONT_VARIANT_NUMERIC, Prop.FONT_VARIANT_EAST_ASIAN, Prop.FONT_VARIANT_POSITION, Prop.FONT_VARIANT_ALTERNATES};
        }
    }

    /**
     * Parses text that is known to be a single token.
     * <p>
     * 토큰 하나임을 아는 문자열을 파싱한다.
     *
     * @param text token text / 토큰 문자열
     * @return the component value / 컴포넌트 값
     */
    private static ComponentValue parseToken(String text) {
        return org.duckdns.hjow.swingwebpanel.css.syntax.CssParser.parseComponentValues(text).get(0);
    }

    /**
     * The <code>flex</code> shorthand: <code>none</code>, <code>auto</code>, or flex factors and a basis.
     * <p>
     * <code>flex</code> 단축 속성: <code>none</code>, <code>auto</code>, 또는 flex 비율과 기준.
     */
    static final class Flex extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Flex() {
            super("flex", new int[] {Prop.FLEX_GROW, Prop.FLEX_SHRINK, Prop.FLEX_BASIS});
        }

        /**
         * Parses the flex value.
         * <p>
         * flex 값을 파싱한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int n = items.size();
            if (n < 1 || n > 3) {
                return false;
            }
            if (n == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).is(CssToken.IDENT)) {
                String k = CssKeyword.lower(((CssToken) items.get(0)).getValue());
                if (k.equals("none")) {
                    set(out, 0, 0, CssKeyword.AUTO);
                    return true;
                }
                if (k.equals("auto")) {
                    set(out, 1, 1, CssKeyword.AUTO);
                    return true;
                }
            }
            double[] factor = new double[2];
            int factors = 0;
            CssValue basis = null;
            // 숫자는 두 개까지 flex 비율로, 나머지는 flex-basis 로 읽는다.
            for (int i = 0; i < n; i++) {
                ComponentValue c = items.get(i);
                if (isNumber(c) && factors < 2 && ((CssToken) c).getNumber() >= 0) {
                    factor[factors++] = ((CssToken) c).getNumber();
                } else if (basis == null) {
                    basis = match(reg, Prop.FLEX_BASIS, one(c), env);
                    if (basis == null) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
            if (factors == 0) {
                set(out, 1, 1, basis);
            } else {
                set(out, factor[0], factors > 1 ? factor[1] : 1, basis != null ? basis : new CssPercentage(0));
            }
            return true;
        }

        /**
         * Writes the three longhand values.
         * <p>
         * 세 개별 속성 값을 쓴다.
         *
         * @param out    result / 결과
         * @param grow   flex-grow / flex-grow
         * @param shrink flex-shrink / flex-shrink
         * @param basis  flex-basis / flex-basis
         */
        private static void set(Map<Integer, CssValue> out, double grow, double shrink, CssValue basis) {
            out.put(Integer.valueOf(Prop.FLEX_GROW), new CssNumber(grow, grow == Math.rint(grow)));
            out.put(Integer.valueOf(Prop.FLEX_SHRINK), new CssNumber(shrink, shrink == Math.rint(shrink)));
            out.put(Integer.valueOf(Prop.FLEX_BASIS), basis);
        }
    }

    /**
     * The grid placement shorthands <code>grid-row</code> and <code>grid-column</code> (two grid lines separated by a slash; a single
     * identifier is used for both lines) and <code>grid-area</code> (up to four grid lines separated by slashes in the order row-start,
     * column-start, row-end, column-end; the omitted ones copy the first line of their axis if it is an identifier, else they are
     * <code>auto</code>). Added for the grid layout (P10-03).
     * <p>
     * 그리드 배치 단축 속성 <code>grid-row</code>, <code>grid-column</code>(슬래시로 나눈 그리드 선 둘. 식별자 하나는 두 선에 모두 쓴다)과 <code>grid-area</code>(슬래시로 나눈
     * 그리드 선 최대 넷. 순서는 row-start, column-start, row-end, column-end 이고 생략한 선은 같은 축의 첫 선이 식별자이면 그것을 따르고 아니면 <code>auto</code>). 그리드
     * 레이아웃(P10-03)을 위해 추가했다.
     */
    static final class GridPlacement extends Shorthand {
        /** True for <code>grid-area</code>. <code>grid-area</code> 이면 true. */
        private final boolean area;

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         *
         * @param name      the property name / 속성 이름
         * @param longhands the longhands in the order of the syntax / 문법 순서의 개별 속성
         * @param area      true for <code>grid-area</code> / <code>grid-area</code> 이면 true
         */
        GridPlacement(String name, int[] longhands, boolean area) {
            super(name, longhands);
            this.area = area;
        }

        /**
         * Splits the value at the slashes and distributes the lines over the longhands.
         * <p>
         * 값을 슬래시에서 나누어 그리드 선을 개별 속성에 나눈다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            List<List<ComponentValue>> parts = new ArrayList<List<ComponentValue>>();
            List<ComponentValue> cur = new ArrayList<ComponentValue>();
            // 슬래시마다 그리드 선을 나눈다.
            for (int i = 0; i < items.size(); i++) {
                if (isDelim(items.get(i), '/')) {
                    parts.add(cur);
                    cur = new ArrayList<ComponentValue>();
                } else {
                    cur.add(items.get(i));
                }
            }
            parts.add(cur);
            int[] ids = getLonghands();
            int max = area ? 4 : 2;
            if (parts.size() > max) {
                return false;
            }
            CssValue[] v = new CssValue[parts.size()];
            // 각 선을 첫 개별 속성의 문법으로 검사한다.
            for (int i = 0; i < v.length; i++) {
                if (parts.get(i).isEmpty()) {
                    return false;
                }
                v[i] = match(reg, ids[0], parts.get(i), env);
                if (v[i] == null) {
                    return false;
                }
            }
            if (!area) {
                out.put(Integer.valueOf(ids[0]), v[0]);
                out.put(Integer.valueOf(ids[1]), v.length > 1 ? v[1] : (isIdent(v[0]) ? v[0] : CssKeyword.AUTO));
                return true;
            }
            // grid-area: 생략한 선은 같은 축의 앞선 선이 식별자이면 그것을 따른다.
            CssValue rs = v[0];
            CssValue cs = v.length > 1 ? v[1] : (isIdent(rs) ? rs : CssKeyword.AUTO);
            CssValue re = v.length > 2 ? v[2] : (isIdent(rs) ? rs : CssKeyword.AUTO);
            CssValue ce = v.length > 3 ? v[3] : (isIdent(cs) ? cs : CssKeyword.AUTO);
            out.put(Integer.valueOf(ids[0]), rs);
            out.put(Integer.valueOf(ids[1]), cs);
            out.put(Integer.valueOf(ids[2]), re);
            out.put(Integer.valueOf(ids[3]), ce);
            return true;
        }

        /**
         * Tells whether a grid line value is a single identifier (a line name).
         * <p>
         * 그리드 선 값이 식별자 하나(선 이름)인지 알려 준다.
         *
         * @param v the value / 값
         * @return true for a name / 이름이면 true
         */
        private static boolean isIdent(CssValue v) {
            return v instanceof CssKeyword && !v.isKeyword("auto") && !v.isKeyword("span");
        }
    }

    /**
     * The <code>grid-template</code> shorthand: <code>none</code>, <code>&lt;rows&gt; / &lt;columns&gt;</code> and the form with
     * strings for the template areas (<code>[names]? "area row" size? [names]?</code> repeated, optionally followed by a slash and the
     * columns; a row without a size is <code>auto</code>). Added for the grid layout (P10-03).
     * <p>
     * <code>grid-template</code> 단축 속성: <code>none</code>, <code>&lt;행&gt; / &lt;열&gt;</code>, 템플릿 영역 문자열을 쓴 형태(<code>[이름]? "영역 행" 크기? [이름]?</code> 의
     * 반복에 슬래시와 열이 이어질 수 있음. 크기가 없는 행은 <code>auto</code>). 그리드 레이아웃(P10-03)을 위해 추가했다.
     */
    static final class GridTemplate extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        GridTemplate() {
            super("grid-template", new int[] {Prop.GRID_TEMPLATE_ROWS, Prop.GRID_TEMPLATE_COLUMNS, Prop.GRID_TEMPLATE_AREAS});
        }

        /**
         * Parses <code>none</code>, the rows and the columns separated by a slash, or the form with the strings of the areas.
         * <p>
         * <code>none</code>, 슬래시로 나뉜 행과 열, 영역 문자열을 쓴 형태를 해석한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int slash = -1;
            // 슬래시 위치를 찾는다.
            for (int i = 0; i < items.size(); i++) {
                if (isDelim(items.get(i), '/')) {
                    if (slash >= 0) {
                        return false;
                    }
                    slash = i;
                }
            }
            CssValue none = CssKeyword.of("none");
            if (slash < 0 && items.size() == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).is(CssToken.IDENT)
                    && "none".equals(CssKeyword.lower(((CssToken) items.get(0)).getValue()))) {
                out.put(Integer.valueOf(Prop.GRID_TEMPLATE_ROWS), none);
                out.put(Integer.valueOf(Prop.GRID_TEMPLATE_COLUMNS), none);
                out.put(Integer.valueOf(Prop.GRID_TEMPLATE_AREAS), none);
                return true;
            }
            List<ComponentValue> rows = slash < 0 ? items : items.subList(0, slash);
            List<ComponentValue> cols = slash < 0 ? new ArrayList<ComponentValue>() : items.subList(slash + 1, items.size());
            // 문자열이 있으면 영역을 쓴 형태이다.
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i) instanceof CssToken && ((CssToken) rows.get(i)).is(CssToken.STRING)) {
                    return expandAreas(rows, cols, slash >= 0, reg, env, out);
                }
            }
            if (slash < 0 || rows.isEmpty() || cols.isEmpty()) {
                return false;
            }
            CssValue r = match(reg, Prop.GRID_TEMPLATE_ROWS, rows, env);
            CssValue c = match(reg, Prop.GRID_TEMPLATE_COLUMNS, cols, env);
            if (r == null || c == null) {
                return false;
            }
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_ROWS), r);
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_COLUMNS), c);
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_AREAS), none);
            return true;
        }

        /**
         * Parses the form with the strings of the areas: every string is a row of the areas, an optional track size behind it is the
         * height of the row (<code>auto</code> if it is missing), and square brackets before and after name the lines.
         * <p>
         * 영역 문자열을 쓴 형태를 해석한다: 문자열 하나가 영역의 행 하나이고 그 뒤의 트랙 크기가 행의 높이이며(없으면 <code>auto</code>), 앞뒤의 대괄호가 선의 이름이다.
         *
         * @param rows    the values before the slash / 슬래시 앞의 값
         * @param cols    the values behind the slash / 슬래시 뒤의 값
         * @param hasCols true if there is a slash / 슬래시가 있으면 true
         * @param reg     registry / 등록부
         * @param env     environment / 환경
         * @param out     result / 결과
         * @return true if valid / 올바르면 true
         */
        private boolean expandAreas(List<ComponentValue> rows, List<ComponentValue> cols, boolean hasCols, PropertyRegistry reg, ParseEnv env,
                Map<Integer, CssValue> out) {
            List<CssValue> rowVals = new ArrayList<CssValue>();
            List<ComponentValue> strings = new ArrayList<ComponentValue>();
            boolean needSize = false;
            // 값을 차례로 보며 문자열은 영역 행으로, 대괄호는 선 이름으로, 나머지는 직전 행의 트랙 크기로 모은다.
            for (int i = 0; i < rows.size(); i++) {
                ComponentValue c = rows.get(i);
                if (c instanceof CssToken && ((CssToken) c).is(CssToken.STRING)) {
                    if (needSize) {
                        rowVals.add(CssKeyword.AUTO);
                    }
                    strings.add(c);
                    needSize = true;
                    continue;
                }
                CssValue v = match(reg, Prop.GRID_TEMPLATE_ROWS, one(c), env);
                if (v == null) {
                    return false;
                }
                if (!(c instanceof CssBlock)) {
                    // 트랙 크기는 문자열 뒤에 하나만 올 수 있다.
                    if (!needSize) {
                        return false;
                    }
                    needSize = false;
                }
                rowVals.add(v);
            }
            if (needSize) {
                rowVals.add(CssKeyword.AUTO);
            }
            CssValue areas = match(reg, Prop.GRID_TEMPLATE_AREAS, strings, env);
            CssValue c = hasCols ? (cols.isEmpty() ? null : match(reg, Prop.GRID_TEMPLATE_COLUMNS, cols, env)) : CssKeyword.of("none");
            if (areas == null || c == null) {
                return false;
            }
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_ROWS), new CssList(rowVals, false));
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_COLUMNS), c);
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_AREAS), areas);
            return true;
        }
    }

    /**
     * The <code>grid</code> shorthand: the forms <code>&lt;grid-template&gt;</code>, <code>&lt;rows&gt; / auto-flow dense?
     * &lt;auto-columns&gt;?</code> and <code>auto-flow dense? &lt;auto-rows&gt;? / &lt;columns&gt;</code>. It resets all the explicit and
     * implicit grid properties (not the gaps). Added for the grid layout (P10-03).
     * <p>
     * <code>grid</code> 단축 속성: <code>&lt;grid-template&gt;</code>, <code>&lt;행&gt; / auto-flow dense? &lt;auto-columns&gt;?</code>,
     * <code>auto-flow dense? &lt;auto-rows&gt;? / &lt;열&gt;</code> 형태. 명시적/암시적 그리드 속성을 모두 되돌린다(gap 은 제외). 그리드 레이아웃(P10-03)을 위해
     * 추가했다.
     */
    static final class Grid extends Shorthand {
        /** The template shorthand for the first form. 첫 형태를 위한 template 단축 속성. */
        private final GridTemplate template = new GridTemplate();

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Grid() {
            super("grid", new int[] {Prop.GRID_TEMPLATE_ROWS, Prop.GRID_TEMPLATE_COLUMNS, Prop.GRID_TEMPLATE_AREAS, Prop.GRID_AUTO_ROWS,
                Prop.GRID_AUTO_COLUMNS, Prop.GRID_AUTO_FLOW});
        }

        /**
         * Parses one of the three forms and resets the properties the form does not set.
         * <p>
         * 세 형태 중 하나를 해석하고 그 형태가 정하지 않는 속성을 되돌린다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int slash = -1;
            // 슬래시 위치를 찾는다.
            for (int i = 0; i < items.size(); i++) {
                if (isDelim(items.get(i), '/')) {
                    if (slash >= 0) {
                        return false;
                    }
                    slash = i;
                }
            }
            List<ComponentValue> left = slash < 0 ? items : items.subList(0, slash);
            List<ComponentValue> right = slash < 0 ? new ArrayList<ComponentValue>() : items.subList(slash + 1, items.size());
            boolean leftFlow = flowCount(left) > 0;
            boolean rightFlow = flowCount(right) > 0;
            if (slash >= 0 && (leftFlow || rightFlow)) {
                if (leftFlow && rightFlow) {
                    return false;
                }
                // 자동 흐름 형태: 한쪽은 템플릿이고 다른 쪽은 auto-flow 와 암시적 트랙 크기이다.
                if (!expandFlow(leftFlow ? left : right, leftFlow ? right : left, leftFlow, reg, env, out)) {
                    return false;
                }
            } else {
                if (!template.expand(items, reg, env, out)) {
                    return false;
                }
                // template 형태는 암시적 그리드 속성을 처음 값으로 되돌린다.
                out.put(Integer.valueOf(Prop.GRID_AUTO_ROWS), reg.byId(Prop.GRID_AUTO_ROWS).getInitial());
                out.put(Integer.valueOf(Prop.GRID_AUTO_COLUMNS), reg.byId(Prop.GRID_AUTO_COLUMNS).getInitial());
                out.put(Integer.valueOf(Prop.GRID_AUTO_FLOW), reg.byId(Prop.GRID_AUTO_FLOW).getInitial());
            }
            return true;
        }

        /**
         * Counts the leading values that form <code>auto-flow &amp;&amp; dense?</code> (the two keywords in either order): 0 if there is no
         * <code>auto-flow</code>, 1 for <code>auto-flow</code> alone, 2 for both.
         * <p>
         * <code>auto-flow &amp;&amp; dense?</code> 를 이루는 앞쪽 값의 수를 센다(두 키워드의 순서는 상관없음): <code>auto-flow</code> 가 없으면 0, 하나만 있으면 1,
         * 둘 다 있으면 2.
         *
         * @param v the values / 값
         * @return the count / 수
         */
        private static int flowCount(List<ComponentValue> v) {
            boolean auto = false;
            boolean dense = false;
            int n = 0;
            // 앞의 두 값이 auto-flow 와 dense 인 동안 센다.
            for (int i = 0; i < v.size() && i < 2; i++) {
                if (!(v.get(i) instanceof CssToken)) {
                    break;
                }
                CssToken t = (CssToken) v.get(i);
                if (t.isIdent("auto-flow") && !auto) {
                    auto = true;
                    n++;
                } else if (t.isIdent("dense") && !dense) {
                    dense = true;
                    n++;
                } else {
                    break;
                }
            }
            return auto ? n : 0;
        }

        /**
         * Parses the auto-flow form: <code>auto-flow dense? &lt;auto-rows&gt;?</code> on the flow side and the explicit track list on the
         * other side.
         * <p>
         * 자동 흐름 형태를 해석한다: 흐름 쪽의 <code>auto-flow dense? &lt;auto-rows&gt;?</code> 와 반대쪽의 명시적 트랙 목록.
         *
         * @param flow    the values of the side with <code>auto-flow</code> / <code>auto-flow</code> 가 있는 쪽의 값
         * @param tracks  the values of the other side / 반대쪽의 값
         * @param rowFlow true if the flow side is the rows (before the slash) / 흐름 쪽이 행(슬래시 앞)이면 true
         * @param reg     registry / 등록부
         * @param env     environment / 환경
         * @param out     result / 결과
         * @return true if valid / 올바르면 true
         */
        private boolean expandFlow(List<ComponentValue> flow, List<ComponentValue> tracks, boolean rowFlow, PropertyRegistry reg, ParseEnv env,
                Map<Integer, CssValue> out) {
            int pos = flowCount(flow);
            boolean dense = pos == 2;
            List<ComponentValue> sizes = flow.subList(pos, flow.size());
            if (tracks.isEmpty()) {
                return false;
            }
            int sizeId = rowFlow ? Prop.GRID_AUTO_ROWS : Prop.GRID_AUTO_COLUMNS;
            int trackId = rowFlow ? Prop.GRID_TEMPLATE_COLUMNS : Prop.GRID_TEMPLATE_ROWS;
            CssValue autoSize = sizes.isEmpty() ? reg.byId(sizeId).getInitial() : match(reg, sizeId, sizes, env);
            CssValue t = match(reg, trackId, tracks, env);
            if (autoSize == null || t == null) {
                return false;
            }
            Map<Integer, CssValue> flowOut = new java.util.LinkedHashMap<Integer, CssValue>();
            if (reg.parse("grid-auto-flow", (rowFlow ? "row" : "column") + (dense ? " dense" : ""), flowOut) != PropertyRegistry.OK) {
                return false;
            }
            out.put(Integer.valueOf(Prop.GRID_AUTO_FLOW), flowOut.get(Integer.valueOf(Prop.GRID_AUTO_FLOW)));
            out.put(Integer.valueOf(sizeId), autoSize);
            out.put(Integer.valueOf(rowFlow ? Prop.GRID_AUTO_COLUMNS : Prop.GRID_AUTO_ROWS), reg.byId(rowFlow ? Prop.GRID_AUTO_COLUMNS : Prop.GRID_AUTO_ROWS).getInitial());
            out.put(Integer.valueOf(trackId), t);
            out.put(Integer.valueOf(rowFlow ? Prop.GRID_TEMPLATE_ROWS : Prop.GRID_TEMPLATE_COLUMNS), CssKeyword.of("none"));
            out.put(Integer.valueOf(Prop.GRID_TEMPLATE_AREAS), CssKeyword.of("none"));
            return true;
        }
    }

    /**
     * The <code>background</code> shorthand: comma separated layers, each with an image, a position (and a size after a slash), a repeat style, an
     * attachment and boxes; the color is only allowed in the last layer.
     * <p>
     * <code>background</code> 단축 속성: 쉼표로 구분한 층. 층마다 이미지, 위치(슬래시 뒤에 크기), 반복 방식, 고정 방식, 상자가 올 수 있고 색은 마지막 층에만 쓸 수 있다.
     */
    static final class Background extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Background() {
            super("background", new int[] {Prop.BACKGROUND_IMAGE, Prop.BACKGROUND_POSITION, Prop.BACKGROUND_SIZE, Prop.BACKGROUND_REPEAT,
                Prop.BACKGROUND_ATTACHMENT, Prop.BACKGROUND_ORIGIN, Prop.BACKGROUND_CLIP, Prop.BACKGROUND_COLOR, Prop.BACKGROUND_BLEND_MODE});
        }

        /**
         * Parses the layers.
         * <p>
         * 층을 파싱한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            List<List<ComponentValue>> ls = layers(items);
            int[] ids = {Prop.BACKGROUND_IMAGE, Prop.BACKGROUND_POSITION, Prop.BACKGROUND_SIZE, Prop.BACKGROUND_REPEAT, Prop.BACKGROUND_ATTACHMENT,
                Prop.BACKGROUND_ORIGIN, Prop.BACKGROUND_CLIP};
            List<List<CssValue>> cols = new ArrayList<List<CssValue>>();
            // ids 의 항목을 하나씩 처리한다.
            for (int i = 0; i < ids.length; i++) {
                cols.add(new ArrayList<CssValue>());
            }
            CssValue color = CssKeyword.INITIAL;
            // 층마다 부분을 순서에 상관없이 읽는다.
            for (int li = 0; li < ls.size(); li++) {
                CssValue[] layer = new CssValue[ids.length];
                CssValue c = layerParse(ls.get(li), reg, env, layer, li == ls.size() - 1);
                if (c == null) {
                    return false;
                }
                if (c != CssKeyword.INITIAL || li == ls.size() - 1) {
                    color = c;
                }
                // 층에서 쓰이지 않은 부분은 그 속성의 한 층짜리 초기값으로 채운다.
                for (int i = 0; i < ids.length; i++) {
                    cols.get(i).add(layer[i] != null ? layer[i] : layerInitial(reg, ids[i]));
                }
            }
            // ids 의 항목을 하나씩 처리한다.
            for (int i = 0; i < ids.length; i++) {
                out.put(Integer.valueOf(ids[i]), new CssList(cols.get(i), true));
            }
            out.put(Integer.valueOf(Prop.BACKGROUND_COLOR), color);
            // background-blend-mode 도 이 단축 속성으로 초기값이 된다(CSS Compositing 1 3.4.3 절).
            out.put(Integer.valueOf(Prop.BACKGROUND_BLEND_MODE), reg.byId(Prop.BACKGROUND_BLEND_MODE).getInitial());
            return true;
        }

        /**
         * Parses one layer.
         * <p>
         * 층 하나를 파싱한다.
         *
         * @param items   the layer's values / 층의 값
         * @param reg     registry / 등록부
         * @param env     environment / 환경
         * @param layer   receives image, position, size, repeat, attachment, origin, clip / 이미지, 위치, 크기, 반복, 고정, 기준, 클립을 받는다
         * @param allowColor true for the last layer / 마지막 층이면 true
         * @return the color (INITIAL if none), or null if the layer is invalid / 색(없으면 INITIAL). 층이 올바르지 않으면 null
         */
        private static CssValue layerParse(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, CssValue[] layer, boolean allowColor) {
            if (items.isEmpty()) {
                return null;
            }
            CssValue color = CssKeyword.INITIAL;
            boolean haveColor = false;
            int pos = 0;
            // 층의 구성 요소를 모두 소비할 때까지 일치하는 부분을 찾는다.
            while (pos < items.size()) {
                ValueGrammar.Prefix p;
                if (layer[0] == null && (p = reg.byId(Prop.BACKGROUND_IMAGE).getGrammar().matchPrefix(one1(items, pos), 0, env)) != null && p.end == 1
                        && single(p.value) != null) {
                    layer[0] = single(p.value);
                    pos++;
                    continue;
                }
                if (layer[1] == null && (p = G_POSITION_SIZE.matchPrefix(items, pos, env)) != null && p.end > pos) {
                    splitPositionSize(p.value, layer);
                    pos = p.end;
                    continue;
                }
                if (layer[3] == null && (p = reg.get("repeat-style").matchPrefix(items, pos, env)) != null && p.end > pos) {
                    layer[3] = p.value;
                    pos = p.end;
                    continue;
                }
                if (layer[4] == null && (p = reg.byId(Prop.BACKGROUND_ATTACHMENT).getGrammar().matchPrefix(one1(items, pos), 0, env)) != null
                        && p.end == 1) {
                    layer[4] = single(p.value);
                    pos++;
                    continue;
                }
                if (layer[5] == null && (p = G_BOXES.matchPrefix(items, pos, env)) != null && p.end > pos) {
                    if (p.value instanceof CssList) {
                        layer[5] = ((CssList) p.value).get(0);
                        layer[6] = ((CssList) p.value).get(1);
                    } else {
                        layer[5] = p.value;
                        layer[6] = p.value;
                    }
                    pos = p.end;
                    continue;
                }
                if (allowColor && !haveColor) {
                    CssValue c = match(reg, Prop.BACKGROUND_COLOR, one(items.get(pos)), env);
                    if (c != null) {
                        color = c;
                        haveColor = true;
                        pos++;
                        continue;
                    }
                }
                return null;
            }
            return color;
        }

        /**
         * Returns the sub-list of one item at an index (as a list to match against).
         * <p>
         * 위치의 항목 하나를 목록으로 반환한다(맞출 대상).
         *
         * @param items the items / 항목
         * @param pos   the index / 위치
         * @return a one-item list / 한 항목 목록
         */
        private static List<ComponentValue> one1(List<ComponentValue> items, int pos) {
            return items.subList(pos, pos + 1);
        }

        /**
         * Unwraps a comma list with one item (the result of a <code>#</code> grammar on one component).
         * <p>
         * 항목이 하나인 쉼표 목록을 푼다(구성 요소 하나에 대한 <code>#</code> 문법의 결과).
         *
         * @param v the value / 값
         * @return the item, or the value itself / 항목 또는 값 자체
         */
        private static CssValue single(CssValue v) {
            return v instanceof CssList && ((CssList) v).isComma() && ((CssList) v).size() == 1 ? ((CssList) v).get(0) : v;
        }

        /**
         * Splits the value of <code>&lt;position&gt; [ / &lt;bg-size&gt; ]?</code> into position and size.
         * <p>
         * <code>&lt;position&gt; [ / &lt;bg-size&gt; ]?</code> 의 값을 위치와 크기로 나눈다.
         *
         * @param v     the matched value / 맞춘 값
         * @param layer receives position (index 1) and size (index 2) / 위치(1)와 크기(2)를 받는다
         */
        static void splitPositionSize(CssValue v, CssValue[] layer) {
            List<CssValue> flat = new ArrayList<CssValue>();
            flatten(v, flat);
            int slash = flat.indexOf(CssOperator.SLASH);
            List<CssValue> pos = slash < 0 ? flat : flat.subList(0, slash);
            layer[1] = pos.size() == 1 ? pos.get(0) : new CssList(pos, false);
            if (slash >= 0) {
                List<CssValue> size = flat.subList(slash + 1, flat.size());
                layer[2] = size.size() == 1 ? size.get(0) : new CssList(size, false);
            }
        }

        /**
         * Flattens nested space lists.
         * <p>
         * 중첩된 공백 목록을 편다.
         *
         * @param v   value / 값
         * @param out receives the items / 항목을 받는다
         */
        private static void flatten(CssValue v, List<CssValue> out) {
            if (v instanceof CssList && !((CssList) v).isComma()) {
                // 중첩 목록의 항목을 차례로 편다.
                for (int i = 0; i < ((CssList) v).size(); i++) {
                    flatten(((CssList) v).get(i), out);
                }
            } else {
                out.add(v);
            }
        }
    }

    /**
     * The <code>mask</code> shorthand (CSS Masking Level 1 section 7.9): comma separated layers, each with a mask reference (<code>none</code>
     * or an image), a position (and a size after a slash), a repeat style, one or two geometry boxes (the origin and the clip, or both),
     * <code>no-clip</code>, a compositing operator and a masking mode, in any order.
     * <p>
     * <code>mask</code> 단축 속성(CSS Masking Level 1 7.9 절): 쉼표로 구분한 층. 층마다 마스크 참조(<code>none</code> 이나 이미지), 위치(슬래시 뒤에 크기), 반복 방식,
     * 기하 상자 하나나 둘(origin 과 clip, 하나면 둘 다), <code>no-clip</code>, 합성 연산자, 마스크 모드가 순서 없이 올 수 있다.
     */
    static final class Mask extends Shorthand {
        /** The geometry box keywords. 기하 상자 키워드. */
        private static final String BOXES = " border-box padding-box content-box margin-box fill-box stroke-box view-box ";
        /** The compositing operator keywords. 합성 연산자 키워드. */
        private static final String OPS = " add subtract intersect exclude ";
        /** The masking mode keywords. 마스크 모드 키워드. */
        private static final String MODES = " alpha luminance match-source ";

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Mask() {
            super("mask", new int[] {Prop.MASK_IMAGE, Prop.MASK_POSITION, Prop.MASK_SIZE, Prop.MASK_REPEAT, Prop.MASK_ORIGIN, Prop.MASK_CLIP,
                Prop.MASK_COMPOSITE, Prop.MASK_MODE});
        }

        /**
         * Parses the layers.
         * <p>
         * 층을 파싱한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            List<List<ComponentValue>> ls = layers(items);
            int[] ids = {Prop.MASK_IMAGE, Prop.MASK_POSITION, Prop.MASK_SIZE, Prop.MASK_REPEAT, Prop.MASK_ORIGIN, Prop.MASK_CLIP, Prop.MASK_COMPOSITE,
                Prop.MASK_MODE};
            List<List<CssValue>> cols = new ArrayList<List<CssValue>>();
            // 속성마다 층 값을 모을 목록을 만든다.
            for (int i = 0; i < ids.length; i++) {
                cols.add(new ArrayList<CssValue>());
            }
            // 층마다 부분을 순서에 상관없이 읽는다.
            for (int li = 0; li < ls.size(); li++) {
                CssValue[] layer = new CssValue[ids.length];
                if (!layerParse(ls.get(li), reg, env, layer)) {
                    return false;
                }
                // 층에서 쓰이지 않은 부분은 그 속성의 한 층짜리 초기값으로 채운다.
                for (int i = 0; i < ids.length; i++) {
                    cols.get(i).add(layer[i] != null ? layer[i] : layerInitial(reg, ids[i]));
                }
            }
            // 속성마다 층 값의 쉼표 목록을 쓴다.
            for (int i = 0; i < ids.length; i++) {
                out.put(Integer.valueOf(ids[i]), new CssList(cols.get(i), true));
            }
            return true;
        }

        /**
         * Parses one layer into image, position, size, repeat, origin, clip, composite and mode.
         * <p>
         * 층 하나를 이미지, 위치, 크기, 반복, origin, clip, 합성, 모드로 파싱한다.
         *
         * @param items the layer's values / 층의 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param layer receives the eight parts in the order of the ids / id 순서대로 여덟 부분을 받는다
         * @return true if the layer is valid / 층이 올바르면 true
         */
        private static boolean layerParse(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, CssValue[] layer) {
            if (items.isEmpty()) {
                return false;
            }
            int pos = 0;
            CssValue firstBox = null;
            CssValue secondBox = null;
            CssValue noClip = null;
            // 층의 구성 요소를 모두 소비할 때까지 일치하는 부분을 찾는다.
            while (pos < items.size()) {
                ComponentValue cv = items.get(pos);
                String id = cv instanceof CssToken && ((CssToken) cv).is(CssToken.IDENT) ? CssKeyword.lower(((CssToken) cv).getValue()) : null;
                if (id != null && BOXES.indexOf(" " + id + " ") >= 0) {
                    CssValue k = CssKeyword.of(id);
                    if (firstBox == null) {
                        firstBox = k;
                    } else if (secondBox == null) {
                        secondBox = k;
                    } else {
                        return false;
                    }
                    pos++;
                    continue;
                }
                if (id != null && id.equals("no-clip") && noClip == null) {
                    noClip = CssKeyword.of(id);
                    pos++;
                    continue;
                }
                if (id != null && layer[6] == null && OPS.indexOf(" " + id + " ") >= 0) {
                    layer[6] = CssKeyword.of(id);
                    pos++;
                    continue;
                }
                if (id != null && layer[7] == null && MODES.indexOf(" " + id + " ") >= 0) {
                    layer[7] = CssKeyword.of(id);
                    pos++;
                    continue;
                }
                ValueGrammar.Prefix p;
                if (layer[0] == null && (p = reg.byId(Prop.MASK_IMAGE).getGrammar().matchPrefix(items.subList(pos, pos + 1), 0, env)) != null && p.end == 1) {
                    layer[0] = p.value instanceof CssList && ((CssList) p.value).isComma() && ((CssList) p.value).size() == 1 ? ((CssList) p.value).get(0) : p.value;
                    pos++;
                    continue;
                }
                if (layer[1] == null && (p = G_POSITION_SIZE.matchPrefix(items, pos, env)) != null && p.end > pos) {
                    Background.splitPositionSize(p.value, layer);
                    pos = p.end;
                    continue;
                }
                if (layer[3] == null && (p = reg.get("repeat-style").matchPrefix(items, pos, env)) != null && p.end > pos) {
                    layer[3] = p.value;
                    pos = p.end;
                    continue;
                }
                return false;
            }
            // 기하 상자가 하나이면 origin 과 clip 이 모두 그 값이고, 둘이면 첫째가 origin, 둘째가 clip 이다(no-clip 은 clip 만 바꾼다).
            if (firstBox != null) {
                layer[4] = firstBox;
                layer[5] = secondBox != null ? secondBox : firstBox;
            } else if (secondBox != null) {
                return false;
            }
            if (noClip != null) {
                if (secondBox != null) {
                    return false;
                }
                layer[5] = noClip;
            }
            return true;
        }
    }

    /**
     * The <code>transition</code> shorthand: comma separated layers of a property, a duration, a timing function and a delay in any order.
     * <p>
     * <code>transition</code> 단축 속성: 속성, 지속 시간, 타이밍 함수, 지연 시간을 순서 무관하게 가진 쉼표 구분 층.
     */
    static final class Transition extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Transition() {
            super("transition", new int[] {Prop.TRANSITION_PROPERTY, Prop.TRANSITION_DURATION, Prop.TRANSITION_TIMING_FUNCTION, Prop.TRANSITION_BEHAVIOR,
                Prop.TRANSITION_DELAY});
        }

        /**
         * Parses the layers.
         * <p>
         * 층을 파싱한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            List<List<ComponentValue>> ls = layers(items);
            List<CssValue> prop = new ArrayList<CssValue>();
            List<CssValue> dur = new ArrayList<CssValue>();
            List<CssValue> ease = new ArrayList<CssValue>();
            List<CssValue> delay = new ArrayList<CssValue>();
            List<CssValue> behavior = new ArrayList<CssValue>();
            // 층마다 구성 요소를 종류별로 나눈다.
            for (int li = 0; li < ls.size(); li++) {
                List<ComponentValue> layer = ls.get(li);
                CssValue p = null;
                CssValue d = null;
                CssValue e = null;
                CssValue dl = null;
                CssValue bh = null;
                if (layer.isEmpty()) {
                    return false;
                }
                // layer 의 항목을 하나씩 처리한다.
                for (int i = 0; i < layer.size(); i++) {
                    List<ComponentValue> c = one(layer.get(i));
                    // allow-discrete 는 transition-behavior 이다(CSS Transitions 2). normal 은 속성 이름이 이미 있을 때만 behavior 로 본다.
                    CssValue bk = G_TRANSITION_BEHAVIOR.match(c, env);
                    if (bk != null && bh == null && (bk.isKeyword("allow-discrete") || p != null)) {
                        bh = bk;
                        continue;
                    }
                    CssValue t = G_TIME.match(c, env);
                    if (t != null) {
                        if (d == null) {
                            d = t;
                        } else if (dl == null) {
                            dl = t;
                        } else {
                            return false;
                        }
                        continue;
                    }
                    CssValue ef = G_EASING.match(c, env);
                    if (ef != null && e == null) {
                        e = ef;
                        continue;
                    }
                    CssValue pr = G_TRANSITION_PROPERTY.match(c, env);
                    if (pr != null && p == null) {
                        p = pr;
                        continue;
                    }
                    return false;
                }
                prop.add(p != null ? p : CssKeyword.of("all"));
                dur.add(d != null ? d : parseValue(reg, Prop.TRANSITION_DURATION));
                ease.add(e != null ? e : CssKeyword.of("ease"));
                delay.add(dl != null ? dl : parseValue(reg, Prop.TRANSITION_DELAY));
                behavior.add(bh != null ? bh : CssKeyword.of("normal"));
            }
            out.put(Integer.valueOf(Prop.TRANSITION_PROPERTY), new CssList(prop, true));
            out.put(Integer.valueOf(Prop.TRANSITION_DURATION), new CssList(dur, true));
            out.put(Integer.valueOf(Prop.TRANSITION_TIMING_FUNCTION), new CssList(ease, true));
            out.put(Integer.valueOf(Prop.TRANSITION_DELAY), new CssList(delay, true));
            out.put(Integer.valueOf(Prop.TRANSITION_BEHAVIOR), new CssList(behavior, true));
            return true;
        }
    }

    /**
     * Returns the single-layer initial value of a layered longhand.
     * <p>
     * 층 개별 속성의 한 층짜리 초기값을 반환한다.
     *
     * @param reg registry / 등록부
     * @param id  longhand id / 개별 속성 id
     * @return the value / 값
     */
    private static CssValue parseValue(PropertyRegistry reg, int id) {
        return layerInitial(reg, id);
    }

    /**
     * The <code>animation</code> shorthand: comma separated layers of name, duration, timing function, delay, iteration count, direction, fill mode
     * and play state in any order.
     * <p>
     * <code>animation</code> 단축 속성: 이름, 지속 시간, 타이밍 함수, 지연 시간, 반복 횟수, 방향, 채움 모드, 재생 상태를 순서 무관하게 가진 쉼표 구분 층.
     */
    static final class Animation extends Shorthand {
        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Animation() {
            super("animation", new int[] {Prop.ANIMATION_NAME, Prop.ANIMATION_DURATION, Prop.ANIMATION_TIMING_FUNCTION, Prop.ANIMATION_DELAY,
                Prop.ANIMATION_ITERATION_COUNT, Prop.ANIMATION_DIRECTION, Prop.ANIMATION_FILL_MODE, Prop.ANIMATION_PLAY_STATE});
        }

        /**
         * Parses the layers.
         * <p>
         * 층을 파싱한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int[] ids = getLonghands();
            List<List<CssValue>> cols = new ArrayList<List<CssValue>>();
            // ids 의 항목을 하나씩 처리한다.
            for (int i = 0; i < ids.length; i++) {
                cols.add(new ArrayList<CssValue>());
            }
            List<List<ComponentValue>> ls = layers(items);
            // 층마다 구성 요소를 종류별로 나눈다.
            for (int li = 0; li < ls.size(); li++) {
                List<ComponentValue> layer = ls.get(li);
                if (layer.isEmpty()) {
                    return false;
                }
                CssValue[] got = new CssValue[ids.length];
                // layer 의 항목을 하나씩 처리한다.
                for (int i = 0; i < layer.size(); i++) {
                    if (!classify(layer.get(i), got, env)) {
                        return false;
                    }
                }
                // 쓰지 않은 부분은 한 층짜리 초기값으로 채운다.
                for (int i = 0; i < ids.length; i++) {
                    cols.get(i).add(got[i] != null ? got[i] : layerInitial(reg, ids[i]));
                }
            }
            // ids 의 항목을 하나씩 처리한다.
            for (int i = 0; i < ids.length; i++) {
                out.put(Integer.valueOf(ids[i]), new CssList(cols.get(i), true));
            }
            return true;
        }

        /**
         * Assigns one component to the part it belongs to.
         * <p>
         * 구성 요소 하나를 속한 부분에 정한다.
         *
         * @param c   the component / 구성 요소
         * @param got the layer's parts: name, duration, timing, delay, iterations, direction, fill, play state / 층의 부분
         * @param env environment / 환경
         * @return false if it fits nowhere / 어디에도 맞지 않으면 false
         */
        private static boolean classify(ComponentValue c, CssValue[] got, ParseEnv env) {
            List<ComponentValue> one = one(c);
            CssValue t = G_TIME.match(one, env);
            if (t != null) {
                if (got[1] == null) {
                    got[1] = t;
                    return true;
                }
                if (got[3] == null) {
                    got[3] = t;
                    return true;
                }
                return false;
            }
            ValueGrammar[] gs = {G_ANIMATION_NAME, null, G_EASING, null, G_ITERATION, G_DIRECTION, G_FILL, G_PLAY};
            int[] order = {2, 4, 5, 6, 7, 0};
            // 이징, 반복 횟수, 방향, 채움, 재생 상태, 이름 순서로 일치하는 아직 비어 있는 부분을 찾는다.
            for (int i = 0; i < order.length; i++) {
                int k = order[i];
                if (got[k] == null) {
                    CssValue v = gs[k].match(one, env);
                    if (v != null) {
                        got[k] = v;
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /**
     * <code>container</code>: the container names, optionally followed by a slash and the container type (CSS Conditional 5, section 3.3).
     * <p>
     * <code>container</code>: 컨테이너 이름들, 그 뒤에 슬래시와 컨테이너 종류가 올 수 있다(CSS Conditional 5, 3.3 절).
     */
    static final class Container extends Shorthand {

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Container() {
            super("container", new int[] {Prop.CONTAINER_NAME, Prop.CONTAINER_TYPE});
        }

        /**
         * Reads the names and the optional type.
         * <p>
         * 이름들과 선택적인 종류를 읽는다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int slash = -1;
            // 슬래시가 있으면 그 앞이 이름, 뒤가 종류이다.
            for (int i = 0; i < items.size() && slash < 0; i++) {
                if (isDelim(items.get(i), '/')) {
                    slash = i;
                }
            }
            List<ComponentValue> names = slash < 0 ? items : new ArrayList<ComponentValue>(items.subList(0, slash));
            CssValue name = names.isEmpty() ? null : match(reg, Prop.CONTAINER_NAME, names, env);
            CssValue type = CssKeyword.INITIAL;
            if (name == null) {
                return false;
            }
            if (slash >= 0) {
                type = match(reg, Prop.CONTAINER_TYPE, new ArrayList<ComponentValue>(items.subList(slash + 1, items.size())), env);
                if (type == null) {
                    return false;
                }
            }
            out.put(Integer.valueOf(Prop.CONTAINER_NAME), name);
            out.put(Integer.valueOf(Prop.CONTAINER_TYPE), type);
            return true;
        }

        /**
         * Serializes the names and, if it is not <code>normal</code>, the type.
         * <p>
         * 이름들과(<code>normal</code> 이 아니면) 종류를 직렬화한다.
         *
         * @param values longhand values / 개별 속성 값
         * @return the text or null / 문자열 또는 null
         */
        public String serialize(Map<Integer, CssValue> values) {
            CssValue n = values.get(Integer.valueOf(Prop.CONTAINER_NAME));
            CssValue t = values.get(Integer.valueOf(Prop.CONTAINER_TYPE));
            if (n == null || t == null) {
                return null;
            }
            return "normal".equals(t.toCssText()) ? n.toCssText() : n.toCssText() + " / " + t.toCssText();
        }
    }
    /**
     * The <code>font-variant</code> shorthand (CSS Fonts Level 4, section 6.12): <code>normal | none | [ the values of the six longhands in any order, mixed ]</code>. Each
     * value belongs to one of the longhands <code>font-variant-caps</code>, <code>-ligatures</code>, <code>-numeric</code>, <code>-east-asian</code>, <code>-position</code> and
     * <code>-alternates</code>; the values of one longhand do not have to be next to each other. A longhand that gets no value is reset to its initial value;
     * <code>normal</code> resets all six, and <code>none</code> sets <code>font-variant-ligatures: none</code> and resets the others.
     * <p>
     * <code>font-variant</code> 단축 속성(CSS Fonts Level 4 6.12 절): <code>normal | none | [ 여섯 개별 속성의 값을 순서 없이 섞어서 ]</code>. 값은 각각
     * <code>font-variant-caps</code>, <code>-ligatures</code>, <code>-numeric</code>, <code>-east-asian</code>, <code>-position</code>, <code>-alternates</code> 중 하나에 속하며
     * 한 개별 속성의 값이 서로 붙어 있을 필요는 없다. 값을 받지 못한 개별 속성은 초기값이 된다. <code>normal</code> 은 여섯을 모두 초기화하고 <code>none</code> 은
     * <code>font-variant-ligatures: none</code> 으로 정하고 나머지는 초기화한다.
     */
    static final class FontVariant extends Shorthand {

        /** The longhands in the order of the groups. / 그룹 순서의 개별 속성. */
        private static final int[] IDS = {Prop.FONT_VARIANT_CAPS, Prop.FONT_VARIANT_LIGATURES, Prop.FONT_VARIANT_NUMERIC, Prop.FONT_VARIANT_EAST_ASIAN,
            Prop.FONT_VARIANT_POSITION, Prop.FONT_VARIANT_ALTERNATES};

        /** The keywords of each group (the functions of the alternates are checked by name). / 그룹별 키워드(대체 형태의 함수는 이름으로 확인한다). */
        private static final String[][] WORDS = {
            {"small-caps", "all-small-caps", "petite-caps", "all-petite-caps", "unicase", "titling-caps"},
            {"common-ligatures", "no-common-ligatures", "discretionary-ligatures", "no-discretionary-ligatures", "historical-ligatures", "no-historical-ligatures",
                "contextual", "no-contextual"},
            {"lining-nums", "oldstyle-nums", "proportional-nums", "tabular-nums", "diagonal-fractions", "stacked-fractions", "ordinal", "slashed-zero"},
            {"jis78", "jis83", "jis90", "jis04", "simplified", "traditional", "full-width", "proportional-width", "ruby"},
            {"sub", "super"},
            {"historical-forms"}};

        /** The functions of the alternates group. / 대체 형태 그룹의 함수. */
        private static final String[] ALTERNATE_FUNCTIONS = {"stylistic", "styleset", "character-variant", "swash", "ornaments", "annotation"};

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        FontVariant() {
            super("font-variant", IDS);
        }

        /**
         * Finds the group that a component value belongs to.
         * <p>
         * 컴포넌트 값이 속한 그룹을 찾는다.
         *
         * @param v the component value / 컴포넌트 값
         * @return the group index, or -1 if it belongs to none / 그룹 인덱스. 어디에도 속하지 않으면 -1
         */
        private static int groupOf(ComponentValue v) {
            if (v instanceof CssToken && ((CssToken) v).is(CssToken.IDENT)) {
                String t = ((CssToken) v).getValue().toLowerCase();
                // 키워드 표에서 속한 그룹을 찾는다.
                for (int g = 0; g < WORDS.length; g++) {
                    for (int k = 0; k < WORDS[g].length; k++) {
                        if (WORDS[g][k].equals(t)) {
                            return g;
                        }
                    }
                }
            } else if (v instanceof CssFunction) {
                // 대체 형태의 함수는 이름으로 확인한다.
                for (int k = 0; k < ALTERNATE_FUNCTIONS.length; k++) {
                    if (((CssFunction) v).isNamed(ALTERNATE_FUNCTIONS[k])) {
                        return 5;
                    }
                }
            }
            return -1;
        }

        /**
         * Distributes the values to the longhands and checks each group with the grammar of its longhand.
         * <p>
         * 값을 개별 속성에 나누고 그룹마다 그 개별 속성의 문법으로 확인한다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            if (items.isEmpty()) {
                return false;
            }
            // normal 과 none 은 혼자일 때만 올바르다.
            if (items.size() == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).isIdent("normal")) {
                // 여섯 개별 속성을 모두 초기화한다.
                for (int i = 0; i < IDS.length; i++) {
                    out.put(Integer.valueOf(IDS[i]), CssKeyword.INITIAL);
                }
                return true;
            }
            if (items.size() == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).isIdent("none")) {
                // none 은 합자만 끄고 나머지는 초기화한다.
                for (int i = 0; i < IDS.length; i++) {
                    out.put(Integer.valueOf(IDS[i]), IDS[i] == Prop.FONT_VARIANT_LIGATURES ? CssKeyword.NONE : CssKeyword.INITIAL);
                }
                return true;
            }
            List<List<ComponentValue>> groups = new ArrayList<List<ComponentValue>>();
            // 그룹마다 빈 목록을 만든 뒤 값을 하나씩 알맞은 그룹에 나눈다.
            for (int i = 0; i < IDS.length; i++) {
                groups.add(new ArrayList<ComponentValue>());
            }
            // 값을 하나씩 알맞은 그룹에 나눈다.
            for (int i = 0; i < items.size(); i++) {
                int g = groupOf(items.get(i));
                if (g < 0) {
                    return false;
                }
                groups.get(g).add(items.get(i));
            }
            // 값을 받은 그룹은 개별 속성의 문법으로 확인하고, 받지 못한 그룹은 초기값으로 한다.
            for (int g = 0; g < IDS.length; g++) {
                if (groups.get(g).isEmpty()) {
                    out.put(Integer.valueOf(IDS[g]), CssKeyword.INITIAL);
                    continue;
                }
                CssValue v = match(reg, IDS[g], groups.get(g), env);
                if (v == null) {
                    return false;
                }
                out.put(Integer.valueOf(IDS[g]), v);
            }
            return true;
        }
    }
    /**
     * The <code>offset</code> shorthand (CSS Motion Path Level 1, section 2.6): <code>[ &lt;offset-position&gt;? [ &lt;offset-path&gt; [ &lt;offset-distance&gt; ||
     * &lt;offset-rotate&gt; ]? ]? ]! [ / &lt;offset-anchor&gt; ]?</code>. The longhands that the value does not mention are reset to their initial values.
     * <p>
     * <code>offset</code> 단축 속성(CSS Motion Path Level 1 2.6 절): <code>[ &lt;offset-position&gt;? [ &lt;offset-path&gt; [ &lt;offset-distance&gt; || &lt;offset-rotate&gt; ]? ]? ]!
     * [ / &lt;offset-anchor&gt; ]?</code>. 값이 언급하지 않은 개별 속성은 초기값이 된다.
     */
    static final class Offset extends Shorthand {

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        Offset() {
            super("offset", new int[] {Prop.OFFSET_POSITION, Prop.OFFSET_PATH, Prop.OFFSET_DISTANCE, Prop.OFFSET_ROTATE, Prop.OFFSET_ANCHOR});
        }

        /**
         * Tells whether a component value ends the offset path: a number, a dimension, a percentage, or the keywords of <code>offset-rotate</code>.
         * <p>
         * 컴포넌트 값이 오프셋 경로를 끝내는지 알려 준다: 숫자, 단위가 있는 수, 백분율, <code>offset-rotate</code> 의 키워드.
         *
         * @param v the component value / 컴포넌트 값
         * @return true if the path cannot continue with it / 경로가 이어질 수 없으면 true
         */
        private static boolean endsPath(ComponentValue v) {
            if (v instanceof CssToken) {
                CssToken t = (CssToken) v;
                return t.is(CssToken.NUMBER) || t.is(CssToken.DIMENSION) || t.is(CssToken.PERCENTAGE) || t.isIdent("auto") || t.isIdent("reverse");
            }
            return false;
        }

        /**
         * Reads the position, the path with the distance and the rotation in any order, and the anchor after the slash.
         * <p>
         * 위치, 경로와 거리와 회전(순서 자유), 슬래시 뒤의 앵커를 읽는다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            int slash = -1;
            // 슬래시가 있으면 그 뒤가 앵커이다.
            for (int i = 0; i < items.size() && slash < 0; i++) {
                if (isDelim(items.get(i), '/')) {
                    slash = i;
                }
            }
            List<ComponentValue> main = slash < 0 ? items : new ArrayList<ComponentValue>(items.subList(0, slash));
            CssValue anchor = CssKeyword.INITIAL;
            if (slash >= 0) {
                anchor = match(reg, Prop.OFFSET_ANCHOR, new ArrayList<ComponentValue>(items.subList(slash + 1, items.size())), env);
                if (anchor == null) {
                    return false;
                }
            }
            if (main.isEmpty()) {
                return false;
            }
            CssValue position = CssKeyword.INITIAL;
            CssValue path = CssKeyword.INITIAL;
            CssValue distance = CssKeyword.INITIAL;
            CssValue rotate = CssKeyword.INITIAL;
            int pos = 0;
            ValueGrammar.Prefix pp = reg.byId(Prop.OFFSET_POSITION).getGrammar().matchPrefix(main, 0, env);
            // 위치는 맨 앞에 있을 수 있다.
            if (pp != null && pp.end > 0) {
                position = pp.value;
                pos = pp.end;
            }
            if (pos < main.size()) {
                int lim = pos + 1;
                // 경로는 숫자나 회전 키워드 앞에서 끝난다.
                while (lim < main.size() && !endsPath(main.get(lim))) {
                    lim++;
                }
                CssValue pv = match(reg, Prop.OFFSET_PATH, new ArrayList<ComponentValue>(main.subList(pos, lim)), env);
                if (pv == null) {
                    return false;
                }
                path = pv;
                pos = lim;
                boolean haveDistance = false;
                boolean haveRotate = false;
                // 경로 뒤에는 거리와 회전이 어떤 순서로든 올 수 있다.
                while (pos < main.size()) {
                    ValueGrammar.Prefix d = haveDistance ? null : reg.byId(Prop.OFFSET_DISTANCE).getGrammar().matchPrefix(main, pos, env);
                    if (d != null && d.end > pos) {
                        distance = d.value;
                        haveDistance = true;
                        pos = d.end;
                        continue;
                    }
                    ValueGrammar.Prefix r = haveRotate ? null : reg.byId(Prop.OFFSET_ROTATE).getGrammar().matchPrefix(main, pos, env);
                    if (r != null && r.end > pos) {
                        rotate = r.value;
                        haveRotate = true;
                        pos = r.end;
                        continue;
                    }
                    return false;
                }
            }
            out.put(Integer.valueOf(Prop.OFFSET_POSITION), position);
            out.put(Integer.valueOf(Prop.OFFSET_PATH), path);
            out.put(Integer.valueOf(Prop.OFFSET_DISTANCE), distance);
            out.put(Integer.valueOf(Prop.OFFSET_ROTATE), rotate);
            out.put(Integer.valueOf(Prop.OFFSET_ANCHOR), anchor);
            return true;
        }
    }
    /**
     * The <code>position-try</code> shorthand (CSS Anchor Positioning Level 1, section 6.3): <code>&lt;'position-try-order'&gt;? &lt;'position-try-fallbacks'&gt;</code>. The
     * order is <code>normal</code> when it is left out; the rest of the value is the list of fallbacks.
     * <p>
     * <code>position-try</code> 단축 속성(CSS Anchor Positioning Level 1 6.3 절): <code>&lt;'position-try-order'&gt;? &lt;'position-try-fallbacks'&gt;</code>. 순서를 생략하면
     * <code>normal</code> 이고 나머지 값이 대안 목록이다.
     */
    static final class PositionTry extends Shorthand {

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        PositionTry() {
            super("position-try", new int[] {Prop.POSITION_TRY_ORDER, Prop.POSITION_TRY_FALLBACKS});
        }

        /**
         * Reads an optional <code>most-*</code> keyword followed by the fallbacks.
         * <p>
         * 선택적인 <code>most-*</code> 키워드와 그 뒤의 대안을 읽는다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            if (items.isEmpty()) {
                return false;
            }
            CssValue order = CssKeyword.NORMAL;
            List<ComponentValue> rest = items;
            // 첫 항목이 정렬 순서 키워드이면 그것을 순서로, 나머지를 대안 목록으로 본다.
            if (items.get(0) instanceof CssToken && ((CssToken) items.get(0)).is(CssToken.IDENT)) {
                String k = ((CssToken) items.get(0)).getValue().toLowerCase();
                if (k.equals("most-width") || k.equals("most-height") || k.equals("most-block-size") || k.equals("most-inline-size")) {
                    order = CssKeyword.of(k);
                    rest = items.subList(1, items.size());
                }
            }
            CssValue fallbacks = rest.isEmpty() ? null : match(reg, Prop.POSITION_TRY_FALLBACKS, rest, env);
            if (fallbacks == null) {
                return false;
            }
            out.put(Integer.valueOf(Prop.POSITION_TRY_ORDER), order);
            out.put(Integer.valueOf(Prop.POSITION_TRY_FALLBACKS), fallbacks);
            return true;
        }

        /**
         * Serializes the shorthand: the order (left out when it is <code>normal</code>) and the fallbacks.
         * <p>
         * 단축 속성을 직렬화한다: 순서(<code>normal</code> 이면 생략)와 대안.
         *
         * @param values longhand values by id / id 별 개별 속성 값
         * @return the text or null if a longhand is missing / 문자열. 개별 속성이 없으면 null
         */
        public String serialize(Map<Integer, CssValue> values) {
            CssValue o = values.get(Integer.valueOf(Prop.POSITION_TRY_ORDER));
            CssValue f = values.get(Integer.valueOf(Prop.POSITION_TRY_FALLBACKS));
            if (o == null || f == null) {
                return null;
            }
            String order = o.toCssText();
            return ("normal".equals(order) ? "" : order + " ") + f.toCssText();
        }
    }

    /**
     * The <code>font-synthesis</code> shorthand (CSS Fonts Level 4, section 2.8): <code>none | [ weight || style || small-caps || position ]</code>. Every kind that the
     * value lists gets <code>auto</code> in its longhand (<code>font-synthesis-weight</code>, <code>-style</code>, <code>-small-caps</code>, <code>-position</code>), every
     * other kind <code>none</code>; <code>none</code> alone turns all four off.
     * <p>
     * <code>font-synthesis</code> 단축 속성(CSS Fonts Level 4 2.8 절): <code>none | [ weight || style || small-caps || position ]</code>. 값이 나열한 종류는 그 개별 속성
     * (<code>font-synthesis-weight</code>, <code>-style</code>, <code>-small-caps</code>, <code>-position</code>)이 <code>auto</code> 가 되고 나머지 종류는 <code>none</code> 이며,
     * <code>none</code> 하나만 있으면 넷 모두 꺼진다.
     */
    static final class FontSynthesis extends Shorthand {

        /** The kinds in the order of the longhands. 개별 속성 순서의 종류. */
        private static final String[] KINDS = {"weight", "style", "small-caps", "position"};

        /** The longhands. 개별 속성. */
        private static final int[] IDS = {Prop.FONT_SYNTHESIS_WEIGHT, Prop.FONT_SYNTHESIS_STYLE, Prop.FONT_SYNTHESIS_SMALL_CAPS, Prop.FONT_SYNTHESIS_POSITION};

        /**
         * Creates the shorthand.
         * <p>
         * 단축 속성을 만든다.
         */
        FontSynthesis() {
            super("font-synthesis", IDS);
        }

        /**
         * Reads <code>none</code> or the kinds in any order, each at most once.
         * <p>
         * <code>none</code> 또는 종류들을 순서에 상관없이 읽는다. 종류마다 최대 한 번이다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            if (items.isEmpty()) {
                return false;
            }
            boolean[] listed = new boolean[KINDS.length];
            // none 은 혼자일 때만 올바르다.
            if (items.size() == 1 && items.get(0) instanceof CssToken && ((CssToken) items.get(0)).isIdent("none")) {
                for (int i = 0; i < IDS.length; i++) {
                    out.put(Integer.valueOf(IDS[i]), CssKeyword.NONE);
                }
                return true;
            }
            // 항목마다 종류 이름을 찾아 표시한다(모르는 이름이나 되풀이는 무효).
            for (int i = 0; i < items.size(); i++) {
                int k = -1;
                // 네 종류 중 항목의 이름과 같은 것을 찾는다.
                for (int j = 0; j < KINDS.length && k < 0; j++) {
                    if (items.get(i) instanceof CssToken && ((CssToken) items.get(i)).isIdent(KINDS[j])) {
                        k = j;
                    }
                }
                if (k < 0 || listed[k]) {
                    return false;
                }
                listed[k] = true;
            }
            // 나열한 종류는 auto, 나머지는 none.
            for (int i = 0; i < IDS.length; i++) {
                out.put(Integer.valueOf(IDS[i]), listed[i] ? CssKeyword.AUTO : CssKeyword.NONE);
            }
            return true;
        }
    }

    /**
     * The <code>mask-border</code> shorthand (CSS Masking Level 1, section 7.7): <code>&lt;mask-border-source&gt; || &lt;mask-border-slice&gt; [ / &lt;mask-border-width&gt;? [
     * / &lt;mask-border-outset&gt; ]? ]? || &lt;mask-border-repeat&gt; || &lt;mask-border-mode&gt;</code>. The longhands that the value does not mention are reset to their
     * initial values.
     * <p>
     * <code>mask-border</code> 단축 속성(CSS Masking Level 1 7.7 절): <code>&lt;mask-border-source&gt; || &lt;mask-border-slice&gt; [ / &lt;mask-border-width&gt;? [ /
     * &lt;mask-border-outset&gt; ]? ]? || &lt;mask-border-repeat&gt; || &lt;mask-border-mode&gt;</code>. 값이 언급하지 않은 개별 속성은 초기값이 된다.
     * <p>
     * The same class reads the <code>border-image</code> shorthand (CSS Backgrounds and Borders Level 3, section 5.7: <code>&lt;'border-image-source'&gt; ||
     * &lt;'border-image-slice'&gt; [ / &lt;'border-image-width'&gt; | / &lt;'border-image-width'&gt;? / &lt;'border-image-outset'&gt; ]? || &lt;'border-image-repeat'&gt;</code>),
     * which has the same grammar without the mode; its longhands are the first five of the list that the constructor gets.
     * <p>
     * 같은 클래스가 <code>border-image</code> 단축 속성(CSS Backgrounds and Borders Level 3 5.7 절)도 읽는다. 문법은 모드가 없는 것만 다르며 개별 속성은 생성자가 받는 목록의
     * 처음 다섯 개이다.
     */
    static final class MaskBorder extends Shorthand {

        /** The longhands of <code>mask-border</code>. <code>mask-border</code> 의 개별 속성. */
        private static final int[] MASK_IDS = {Prop.MASK_BORDER_SOURCE, Prop.MASK_BORDER_SLICE, Prop.MASK_BORDER_WIDTH, Prop.MASK_BORDER_OUTSET, Prop.MASK_BORDER_REPEAT,
            Prop.MASK_BORDER_MODE};

        /** The longhands: source, slice, width, outset, repeat and, for <code>mask-border</code>, the mode. 개별 속성: 원본, slice, width, outset, repeat 와 <code>mask-border</code> 이면 모드. */
        private final int[] ids;

        /**
         * Creates the <code>mask-border</code> shorthand.
         * <p>
         * <code>mask-border</code> 단축 속성을 만든다.
         */
        MaskBorder() {
            this("mask-border", MASK_IDS);
        }

        /**
         * Creates a shorthand of this grammar (<code>mask-border</code> or <code>border-image</code>).
         * <p>
         * 이 문법의 단축 속성(<code>mask-border</code> 나 <code>border-image</code>)을 만든다.
         *
         * @param name the name / 이름
         * @param ids  source, slice, width, outset, repeat and optionally the mode / 원본, slice, width, outset, repeat 와 있으면 모드
         */
        MaskBorder(String name, int[] ids) {
            super(name, ids);
            this.ids = ids;
        }

        /**
         * Reads the source, the slice with its slash separated width and outset, the repeat and the mode in any order.
         * <p>
         * 원본, 슬래시로 이어지는 너비와 outset 이 붙은 slice, 반복, 모드를 순서에 상관없이 읽는다.
         *
         * @param items values / 값
         * @param reg   registry / 등록부
         * @param env   environment / 환경
         * @param out   result / 결과
         * @return true if valid / 올바르면 true
         */
        public boolean expand(List<ComponentValue> items, PropertyRegistry reg, ParseEnv env, Map<Integer, CssValue> out) {
            CssValue[] got = new CssValue[ids.length];
            int pos = 0;
            // 쓰이지 않은 부분마다 앞에서부터 일치하는 것을 찾는다(slice 뒤의 슬래시 부분은 slice 와 함께 읽는다).
            while (pos < items.size()) {
                boolean progress = false;
                // 아직 쓰지 않은 부분(slice 뒤의 슬래시 부분은 제외)에서 처음으로 맞는 것을 찾는다.
                for (int i = 0; i < ids.length && !progress; i++) {
                    if (i == 2 || i == 3 || got[i] != null) {
                        continue;
                    }
                    ValueGrammar.Prefix p = reg.byId(ids[i]).getGrammar().matchPrefix(items, pos, env);
                    if (p == null || p.end <= pos) {
                        continue;
                    }
                    got[i] = p.value;
                    pos = p.end;
                    progress = true;
                    // slice 다음에 슬래시가 오면 너비와 outset 을 읽는다.
                    if (i == 1 && pos < items.size() && isDelim(items.get(pos), '/')) {
                        pos++;
                        ValueGrammar.Prefix w = pos < items.size() && !isDelim(items.get(pos), '/') ? reg.byId(ids[2]).getGrammar().matchPrefix(items, pos, env)
                                : null;
                        if (w != null && w.end > pos) {
                            got[2] = w.value;
                            pos = w.end;
                        }
                        if (pos < items.size() && isDelim(items.get(pos), '/')) {
                            pos++;
                            ValueGrammar.Prefix o = pos < items.size() ? reg.byId(ids[3]).getGrammar().matchPrefix(items, pos, env) : null;
                            if (o == null || o.end <= pos) {
                                return false;
                            }
                            got[3] = o.value;
                            pos = o.end;
                        } else if (w == null) {
                            return false;
                        }
                    }
                }
                if (!progress) {
                    return false;
                }
            }
            // 쓰인 부분은 값을, 쓰이지 않은 부분은 initial 을 쓴다.
            for (int i = 0; i < ids.length; i++) {
                out.put(Integer.valueOf(ids[i]), got[i] != null ? got[i] : CssKeyword.INITIAL);
            }
            return !items.isEmpty();
        }
    }
}
