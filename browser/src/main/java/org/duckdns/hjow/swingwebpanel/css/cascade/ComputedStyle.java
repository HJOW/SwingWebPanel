package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.props.LogicalProps;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.props.Property;
import org.duckdns.hjow.swingwebpanel.css.props.PropertyRegistry;
import org.duckdns.hjow.swingwebpanel.css.props.Shorthand;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssRaw;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;

/**
 * The computed style of one element (or pseudo-element): the result of the cascade and of the computation of values. This is the contract between
 * the style engine and the layout and rendering code.
 * <p>
 * <b>What a value is.</b> Every longhand property (see {@link Prop}) has a <em>computed value</em> that is never null: relative lengths are
 * absolute pixels ({@link CssLength} with unit <code>px</code>), <code>calc()</code> without percentages is folded, colors are {@link CssColor}
 * (or the keyword <code>currentcolor</code> for properties other than <code>color</code>, see {@link #color(int)}), keywords are
 * {@link CssKeyword}, and lists keep the structure the grammar of the property gives them. Percentages stay {@link CssPercentage} (or a
 * {@link CssCalc} that contains one): the layout turns them into used values with {@link #resolve(int, double)}. Custom properties are available as
 * raw component values.
 * <p>
 * <b>How to read it.</b> Use the typed helpers: {@link #px(int)}, {@link #number(int)}, {@link #keyword(int)}, {@link #isAuto(int)},
 * {@link #resolve(int, double)}, {@link #color(int)}; the few most used ones have a name ({@link #display()}, {@link #position()},
 * {@link #fontSizePx()}, {@link #fontWeight()}, {@link #lineHeightPx()}). A computed style is immutable and may be shared between elements.
 * <p>
 * 요소(또는 의사 요소) 하나의 계산 스타일: 캐스케이드와 값 계산의 결과. 스타일 엔진과 레이아웃, 렌더링 코드 사이의 계약이다.
 * <p>
 * <b>값이란.</b> 모든 개별 속성({@link Prop} 참조)은 null 이 아닌 <em>계산 값</em>을 가진다: 상대 길이는 절대 픽셀({@link CssLength}, 단위 <code>px</code>), 백분율 없는
 * <code>calc()</code> 는 접히고, 색은 {@link CssColor}(<code>color</code> 가 아닌 속성은 키워드 <code>currentcolor</code> 일 수 있음, {@link #color(int)} 참조), 키워드는
 * {@link CssKeyword}, 목록은 속성 문법이 준 구조를 유지한다. 백분율은 {@link CssPercentage}(또는 그것을 담은 {@link CssCalc})로 남으며 레이아웃이
 * {@link #resolve(int, double)} 로 실제 값을 만든다. 사용자 정의 속성은 원본 컴포넌트 값으로 제공한다.
 * <p>
 * <b>읽는 방법.</b> 형식별 도우미를 쓴다: {@link #px(int)}, {@link #number(int)}, {@link #keyword(int)}, {@link #isAuto(int)}, {@link #resolve(int, double)},
 * {@link #color(int)}. 가장 많이 쓰는 몇 개는 이름이 있다({@link #display()}, {@link #position()}, {@link #fontSizePx()}, {@link #fontWeight()},
 * {@link #lineHeightPx()}). 계산 스타일은 불변이며 요소끼리 공유할 수 있다.
 */
public final class ComputedStyle {

    /** The computed values by property id (index 0 unused). / 속성 id 별 계산 값(0번은 쓰지 않음). */
    private final CssValue[] values;

    /** The custom properties (always inherited), name to raw value. / 사용자 정의 속성(항상 상속), 이름에서 원본 값으로. */
    private final Map<String, CssRaw> custom;

    /** Font size of the root element in pixels (for rem). / 루트 요소의 글꼴 크기(픽셀, rem 용). */
    private final double rootFontSize;

    /**
     * Creates a computed style (used by the style computer).
     * <p>
     * 계산 스타일을 만든다(스타일 계산기가 쓴다).
     *
     * @param values       computed values by id / id 별 계산 값
     * @param custom       custom properties / 사용자 정의 속성
     * @param rootFontSize root font size / 루트 글꼴 크기
     */
    ComputedStyle(CssValue[] values, Map<String, CssRaw> custom, double rootFontSize) {
        this(values, custom, rootFontSize, false);
    }

    /** True if the element uses the dark color scheme (<code>color-scheme</code>, CSS Color Adjustment 1). 요소가 어두운 색 구성표를 쓰면 true(<code>color-scheme</code>, CSS Color Adjustment 1). */
    private final boolean dark;

    /**
     * Creates a computed style that knows the color scheme of its element.
     * <p>
     * 요소의 색 구성표를 아는 계산 스타일을 만든다.
     *
     * @param values       computed values by id / id 별 계산 값
     * @param custom       custom properties / 사용자 정의 속성
     * @param rootFontSize root font size / 루트 글꼴 크기
     * @param dark         true if the element uses the dark color scheme / 요소가 어두운 색 구성표를 쓰면 true
     */
    ComputedStyle(CssValue[] values, Map<String, CssRaw> custom, double rootFontSize, boolean dark) {
        this.values = values;
        this.custom = Collections.unmodifiableMap(custom);
        this.rootFontSize = rootFontSize;
        this.dark = dark;
    }

    /**
     * Tells whether the element uses the dark color scheme: its <code>color-scheme</code> names <code>dark</code> and not <code>light</code>, or both
     * while the user agent prefers dark. The system colors and <code>light-dark()</code> of the style have been resolved for that scheme.
     * <p>
     * 요소가 어두운 색 구성표를 쓰는지 알려 준다: <code>color-scheme</code> 에 <code>dark</code> 가 있고 <code>light</code> 가 없거나, 둘 다 있고 사용자 에이전트가 어둡게를
     * 선호하는 경우. 스타일의 시스템 색과 <code>light-dark()</code> 는 그 구성표에 맞게 풀려 있다.
     *
     * @return true for the dark scheme / 어두운 구성표이면 true
     */
    public boolean usesDarkScheme() {
        return dark;
    }

    /**
     * Creates the style an element would have without any declaration and without a parent: all initial values (a 16px font).
     * <p>
     * 선언도 부모도 없는 요소가 가질 스타일을 만든다: 모든 초기값(16px 글꼴).
     *
     * @return the initial style / 초기 스타일
     */
    public static ComputedStyle initial() {
        return StyleComputer.compute(new CssValue[Prop.COUNT + 1], Collections.<String, java.util.List<org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue>>emptyMap(), null,
                LengthContext.defaults(), false);
    }

    /**
     * Returns the custom properties for inheritance by a child.
     * <p>
     * 자식이 상속할 사용자 정의 속성을 반환한다.
     *
     * @return the custom properties / 사용자 정의 속성
     */
    Map<String, CssRaw> customProperties() {
        return custom;
    }

    /**
     * Returns the computed value of a longhand property.
     * <p>
     * 개별 속성의 계산 값을 반환한다.
     *
     * @param id a constant of {@link Prop} / {@link Prop} 의 상수
     * @return the computed value, never null / 계산 값. null 이 아니다
     */
    public CssValue get(int id) {
        return values[id];
    }

    /**
     * Returns the computed value of a longhand property by name.
     * <p>
     * 이름으로 개별 속성의 계산 값을 반환한다.
     *
     * @param name a longhand property name (aliases are resolved) / 개별 속성 이름(별칭은 해석한다)
     * @return the computed value, or null if the name is not a longhand / 계산 값. 개별 속성이 아니면 null
     */
    public CssValue get(String name) {
        Property p = PropertyRegistry.getInstance().byName(name);
        return p == null ? null : values[physicalId(p.getId())];
    }

    /**
     * Returns the physical longhand that a property id stands for in this style: a logical longhand maps to the physical one that the
     * <code>writing-mode</code> and the <code>direction</code> of the element name, any other id is returned unchanged.
     * <p>
     * 이 스타일에서 속성 id 가 가리키는 물리 개별 속성을 반환한다: 논리 개별 속성은 요소의 <code>writing-mode</code> 와 <code>direction</code> 이 가리키는 물리 속성으로,
     * 다른 id 는 그대로 반환한다.
     *
     * @param id a property id / 속성 id
     * @return the physical id / 물리 id
     */
    private int physicalId(int id) {
        return LogicalProps.isLogical(id) ? LogicalProps.physical(id, keyword(Prop.WRITING_MODE), keyword(Prop.DIRECTION)) : id;
    }

    /**
     * Returns the keyword of a property, or null if the value is not a keyword.
     * <p>
     * 속성의 키워드를 반환한다. 값이 키워드가 아니면 null.
     *
     * @param id property id / 속성 id
     * @return lower-case keyword or null / 소문자 키워드 또는 null
     */
    public String keyword(int id) {
        CssValue v = values[id];
        return v instanceof CssKeyword ? ((CssKeyword) v).getName() : null;
    }

    /**
     * Tells whether a property has the given keyword as its computed value.
     * <p>
     * 속성의 계산 값이 주어진 키워드인지 알려 준다.
     *
     * @param id property id / 속성 id
     * @param kw lower-case keyword / 소문자 키워드
     * @return true if equal / 같으면 true
     */
    public boolean isKeyword(int id, String kw) {
        return values[id].isKeyword(kw);
    }

    /**
     * Tells whether a property computes to <code>auto</code>.
     * <p>
     * 속성이 <code>auto</code> 로 계산되는지 알려 준다.
     *
     * @param id property id / 속성 id
     * @return true for auto / auto 이면 true
     */
    public boolean isAuto(int id) {
        return values[id].isKeyword("auto");
    }

    /**
     * Returns an absolute length in pixels.
     * <p>
     * 절대 길이를 픽셀로 반환한다.
     *
     * @param id property id / 속성 id
     * @return pixels, or NaN if the value is not an absolute length (a keyword, a percentage, a calc with a percentage) / 픽셀. 절대 길이가 아니면(키워드, 백분율, 백분율이 든 calc) NaN
     */
    public double px(int id) {
        CssValue v = values[id];
        return v instanceof CssLength ? ((CssLength) v).getValue() : Double.NaN;
    }

    /**
     * Returns a plain number value (opacity, flex-grow, z-index, order, a unitless line-height ...).
     * <p>
     * 일반 숫자 값을 반환한다(opacity, flex-grow, z-index, order, 단위 없는 line-height 등).
     *
     * @param id property id / 속성 id
     * @return the number, or NaN if the value is not a number / 숫자. 숫자가 아니면 NaN
     */
    public double number(int id) {
        CssValue v = values[id];
        return v instanceof CssNumber ? ((CssNumber) v).getValue() : Double.NaN;
    }

    /**
     * Turns a length, a percentage or a calc expression into pixels: the used value for a given reference length.
     * <p>
     * 길이, 백분율, calc 식을 픽셀로 바꾼다: 주어진 기준 길이에 대한 실제 값.
     *
     * @param id          property id / 속성 id
     * @param percentBase the length that 100% stands for (the containing block width for most properties) / 100% 가 뜻하는 길이(대부분의 속성은 포함 블록의 폭)
     * @return pixels, or NaN if the value is a keyword such as <code>auto</code> / 픽셀. <code>auto</code> 같은 키워드이면 NaN
     */
    public double resolve(int id, double percentBase) {
        CssValue v = values[id];
        if (v instanceof CssLength) {
            return ((CssLength) v).getValue();
        }
        if (v instanceof CssPercentage) {
            return ((CssPercentage) v).getValue() * percentBase / 100.0;
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).resolve(LengthContext.defaults(), percentBase);
        }
        return Double.NaN;
    }

    /**
     * Returns the color of a color-valued property as 0xAARRGGBB, with <code>currentcolor</code> resolved against the <code>color</code> property.
     * <p>
     * 색 속성의 색을 0xAARRGGBB 로 반환한다. <code>currentcolor</code> 는 <code>color</code> 속성에 대해 해석한다.
     *
     * @param id property id / 속성 id
     * @return the color, or 0 (transparent) if the value is not a color (for example <code>auto</code> or <code>invert</code>) / 색. 색이 아니면(예: <code>auto</code>, <code>invert</code>) 0(투명)
     */
    public int color(int id) {
        CssColor c = colorValue(id);
        return c == null ? 0 : c.getArgb();
    }

    /**
     * Returns the color object of a color-valued property with <code>currentcolor</code> resolved.
     * <p>
     * <code>currentcolor</code> 를 해석한 색 속성의 색 객체를 반환한다.
     *
     * @param id property id / 속성 id
     * @return the color, or null if the value is not a color / 색. 색이 아니면 null
     */
    public CssColor colorValue(int id) {
        CssValue v = values[id];
        if (v.isKeyword("currentcolor")) {
            v = values[Prop.COLOR];
        }
        return v instanceof CssColor ? (CssColor) v : null;
    }

    /**
     * Returns the computed <code>display</code> keyword.
     * <p>
     * 계산된 <code>display</code> 키워드를 반환한다.
     *
     * @return for example <code>block</code>, <code>inline</code>, <code>none</code> / 예: <code>block</code>, <code>inline</code>, <code>none</code>
     */
    public String display() {
        return keyword(Prop.DISPLAY);
    }

    /**
     * Returns the computed <code>position</code> keyword.
     * <p>
     * 계산된 <code>position</code> 키워드를 반환한다.
     *
     * @return static, relative, absolute, fixed or sticky / static, relative, absolute, fixed, sticky 중 하나
     */
    public String position() {
        return keyword(Prop.POSITION);
    }

    /**
     * Returns the font size in pixels.
     * <p>
     * 글꼴 크기를 픽셀로 반환한다.
     *
     * @return pixels / 픽셀
     */
    public double fontSizePx() {
        return px(Prop.FONT_SIZE);
    }

    /**
     * Returns the root element's font size in pixels (the reference of <code>rem</code>).
     * <p>
     * 루트 요소의 글꼴 크기를 픽셀로 반환한다(<code>rem</code> 의 기준).
     *
     * @return pixels / 픽셀
     */
    public double rootFontSizePx() {
        return rootFontSize;
    }

    /**
     * Returns the numeric font weight (1 to 1000; normal is 400, bold is 700).
     * <p>
     * 숫자 글꼴 두께를 반환한다(1 에서 1000. normal 은 400, bold 는 700).
     *
     * @return weight / 두께
     */
    public int fontWeight() {
        double w = number(Prop.FONT_WEIGHT);
        return Double.isNaN(w) ? 400 : (int) Math.round(w);
    }

    /**
     * Returns the line height in pixels, or NaN for <code>normal</code> (the layout derives it from the font metrics).
     * <p>
     * 줄 높이를 픽셀로 반환한다. <code>normal</code> 이면 NaN(레이아웃이 글꼴 측정값에서 구한다).
     *
     * @return pixels or NaN / 픽셀 또는 NaN
     */
    public double lineHeightPx() {
        CssValue v = values[Prop.LINE_HEIGHT];
        if (v instanceof CssNumber) {
            return ((CssNumber) v).getValue() * fontSizePx();
        }
        return v instanceof CssLength ? ((CssLength) v).getValue() : Double.NaN;
    }

    /**
     * Returns the font family list in order: quoted names and unquoted name sequences as plain strings, generic families as their keyword.
     * <p>
     * 글꼴 목록을 순서대로 반환한다: 따옴표 친 이름과 따옴표 없는 이름 나열은 일반 문자열, 일반 글꼴 계열은 키워드 이름.
     *
     * @return the family names / 글꼴 이름
     */
    public List<String> fontFamilies() {
        List<String> out = new ArrayList<String>();
        CssValue v = values[Prop.FONT_FAMILY];
        List<CssValue> items = v instanceof CssList ? ((CssList) v).getItems() : Collections.singletonList(v);
        // 쉼표로 나뉜 각 항목을 이름 하나로 모은다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            if (it instanceof CssString) {
                out.add(((CssString) it).getValue());
            } else if (it instanceof CssKeyword) {
                out.add(((CssKeyword) it).getName());
            } else if (it instanceof CssList) {
                StringBuilder sb = new StringBuilder();
                List<CssValue> words = ((CssList) it).getItems();
                // 따옴표 없는 이름의 단어를 공백으로 이어 한 이름으로 만든다.
                for (int j = 0; j < words.size(); j++) {
                    if (j > 0) {
                        sb.append(' ');
                    }
                    sb.append(words.get(j) instanceof CssKeyword ? ((CssKeyword) words.get(j)).getName() : words.get(j).toCssText());
                }
                out.add(sb.toString());
            }
        }
        return out;
    }

    /**
     * Returns the raw value of a custom property.
     * <p>
     * 사용자 정의 속성의 원본 값을 반환한다.
     *
     * @param name the name including the two hyphens (case-sensitive) / 하이픈 두 개를 포함한 이름(대소문자 구분)
     * @return the value, or null if the property is not defined / 값. 정의되지 않았으면 null
     */
    public CssRaw getCustomProperty(String name) {
        return custom.get(name);
    }

    /**
     * Returns the names of the custom properties that are defined on the element (own or inherited).
     * <p>
     * 요소에 정의된(직접 또는 상속받은) 사용자 정의 속성의 이름을 반환한다.
     *
     * @return the names in a stable order / 안정된 순서의 이름
     */
    public List<String> customPropertyNames() {
        return new ArrayList<String>(custom.keySet());
    }

    /**
     * Returns the value as <code>getComputedStyle(element).getPropertyValue(name)</code> would: the computed value serialized, colors as
     * <code>rgb()</code> with <code>currentcolor</code> resolved. A shorthand is serialized when all its longhands allow it, else the result is the
     * empty string. Unknown names give the empty string.
     * <p>
     * <code>getComputedStyle(element).getPropertyValue(name)</code> 이 반환할 값을 반환한다: 직렬화한 계산 값이며 색은 <code>currentcolor</code> 를 해석한
     * <code>rgb()</code> 이다. 단축 속성은 모든 개별 속성이 허용할 때 직렬화하고 아니면 빈 문자열이다. 모르는 이름은 빈 문자열이다.
     *
     * @param name the property name / 속성 이름
     * @return the serialized value / 직렬화한 값
     */
    public String getPropertyValue(String name) {
        if (PropertyRegistry.isCustomProperty(name)) {
            CssRaw r = custom.get(name);
            return r == null ? "" : r.toCssText();
        }
        PropertyRegistry reg = PropertyRegistry.getInstance();
        Property p = reg.byName(name);
        if (p != null) {
            return resolvedText(p.getId());
        }
        Shorthand s = reg.getShorthand(name);
        if (s == null) {
            return "";
        }
        Map<Integer, CssValue> vals = new LinkedHashMap<Integer, CssValue>();
        int[] ids = s.getLonghands();
        // 단축 속성의 모든 개별 속성의 확정 값을 모은다.
        for (int i = 0; i < ids.length; i++) {
            vals.put(Integer.valueOf(ids[i]), new CssRaw(org.duckdns.hjow.swingwebpanel.css.syntax.CssParser.parseComponentValues(resolvedText(ids[i]))));
        }
        String t = s.serialize(vals);
        return t == null ? "" : t;
    }

    /**
     * Serializes the resolved value of one property.
     * <p>
     * 속성 하나의 확정 값을 직렬화한다.
     *
     * @param id property id / 속성 id
     * @return the text / 문자열
     */
    private String resolvedText(int id) {
        CssValue v = values[physicalId(id)];
        if (v.isKeyword("currentcolor") && values[Prop.COLOR] instanceof CssColor) {
            return ((CssColor) values[Prop.COLOR]).toRgbText();
        }
        if (v instanceof CssColor) {
            return ((CssColor) v).toRgbText();
        }
        return v.toCssText();
    }

    /**
     * Returns a copy of this style with one computed value replaced (used by animations, which compute the value of a property at a
     * moment, and by <code>::first-line</code>). The copy shares everything else with this style.
     * <p>
     * 계산 값 하나를 바꾼 이 스타일의 사본을 반환한다(어느 시각의 속성 값을 계산하는 애니메이션과 <code>::first-line</code> 이 쓴다). 나머지는 이 스타일과 공유한다.
     *
     * @param id a constant of {@link Prop} / {@link Prop} 의 상수
     * @param v  the new computed value, not null / 새 계산 값(null 불가)
     * @return the copy / 사본
     */
    public ComputedStyle with(int id, CssValue v) {
        if (v == null || id <= 0 || id >= values.length) {
            throw new IllegalArgumentException("bad property or value");
        }
        CssValue[] copy = values.clone();
        copy[id] = v;
        return new ComputedStyle(copy, custom, rootFontSize, dark);
    }

    /**
     * Returns a copy of this style on which the values that <code>top</code> sets over <code>base</code> are applied to the listed
     * properties. A property is taken from <code>top</code> only if this style still has the value of <code>base</code> (it inherited the
     * property unchanged, so an element inside the first line that sets a property itself keeps its own value) and <code>top</code>
     * differs from <code>base</code>. <code>::first-line</code> uses it: <code>base</code> is the style of the element, <code>top</code> the
     * style of its <code>::first-line</code>, this is the style of a text item or of an inline element in the first line.
     * <p>
     * <code>top</code> 이 <code>base</code> 위에 정한 값을 나열한 속성에 적용한 이 스타일의 사본을 반환한다. 이 스타일이 아직 <code>base</code> 의 값을 가질 때
     * (그 속성을 바꾸지 않고 상속했을 때. 스스로 속성을 정한 첫 줄 안의 요소는 자기 값을 유지한다)이고 <code>top</code> 이 <code>base</code> 와 다를 때만 그 속성을
     * <code>top</code> 에서 가져온다. <code>::first-line</code> 이 쓴다: <code>base</code> 는 요소의 스타일, <code>top</code> 은 그 <code>::first-line</code> 스타일이고,
     * 이 스타일은 첫 줄 안의 텍스트 항목이나 인라인 요소의 스타일이다.
     *
     * @param base the style the values are compared with / 값을 비교하는 기준 스타일
     * @param top  the style that is laid over it / 그 위에 덮는 스타일
     * @param ids  the property ids that may be taken / 가져올 수 있는 속성 id
     * @return this style when nothing changes, else a changed copy / 바뀌는 것이 없으면 이 스타일, 아니면 바뀐 사본
     */
    public ComputedStyle overlay(ComputedStyle base, ComputedStyle top, int[] ids) {
        CssValue[] copy = null;
        // 속성마다 상속한 값이 그대로이고 top 이 기준과 다르면 top 의 값을 가져온다.
        for (int i = 0; i < ids.length; i++) {
            int id = ids[i];
            CssValue t = top.values[id];
            if (t != null && !t.equals(base.values[id]) && values[id].equals(base.values[id])) {
                if (copy == null) {
                    copy = values.clone();
                }
                copy[id] = t;
            }
        }
        return copy == null ? this : new ComputedStyle(copy, custom, rootFontSize, dark);
    }

    /**
     * Tells whether two computed styles have exactly the same values (so a change from one to the other needs no relayout or repaint).
     * <p>
     * 두 계산 스타일의 값이 완전히 같은지 알려 준다(그러면 한쪽에서 다른 쪽으로의 변화에 재배치나 다시 그리기가 필요 없다).
     *
     * @param o the other style / 다른 스타일
     * @return true if all values and custom properties are equal / 모든 값과 사용자 정의 속성이 같으면 true
     */
    public boolean sameValues(ComputedStyle o) {
        return o != null && Arrays.equals(values, o.values) && customEquals(o) && rootFontSize == o.rootFontSize;
    }

    /**
     * Compares the custom properties of two styles.
     * <p>
     * 두 스타일의 사용자 정의 속성을 비교한다.
     *
     * @param o the other style / 다른 스타일
     * @return true if equal / 같으면 true
     */
    private boolean customEquals(ComputedStyle o) {
        if (custom.size() != o.custom.size()) {
            return false;
        }
        // 이름마다 값의 문자열이 같은지 확인한다.
        for (Map.Entry<String, CssRaw> e : custom.entrySet()) {
            CssRaw other = o.custom.get(e.getKey());
            if (other == null || !other.toCssText().equals(e.getValue().toCssText())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a debugging text with a few key properties.
     * <p>
     * 몇 가지 핵심 속성이 든 디버깅용 문자열을 반환한다.
     *
     * @return text / 문자열
     */
    public String toString() {
        return "ComputedStyle[display=" + display() + ", position=" + position() + ", color=" + values[Prop.COLOR] + ", font-size="
                + values[Prop.FONT_SIZE] + "]";
    }
}
