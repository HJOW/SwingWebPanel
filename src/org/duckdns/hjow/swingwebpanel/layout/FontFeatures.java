package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The OpenType features that a style asks for (CSS Fonts Level 4, <a href="https://www.w3.org/TR/css-fonts-4/#feature-precedence">https://www.w3.org/TR/css-fonts-4/#feature-precedence</a>):
 * the <code>font-variant-*</code> properties (and the <code>font-variant</code> shorthand) and <code>font-kerning</code> turn into feature tags, and the
 * tags of <code>font-feature-settings</code> are applied after them, so that the settings win (the order of precedence of the specification). Java2D of
 * JRE 6 cannot switch OpenType features of a font, so only the features that this engine can reproduce have an effect on the page, and the engine
 * reproduces them itself: small capitals (<code>smcp</code>, <code>c2sc</code>, <code>pcap</code>, <code>c2pc</code>: the lower case letters are drawn as smaller capitals),
 * superscript and subscript (<code>sups</code>, <code>subs</code>: the text is made smaller and moved) and full width forms (<code>fwid</code>). Everything else
 * (ligatures, numerals, alternates ...) is resolved here and can be asked for with {@link #value}, but changes nothing on the screen.
 * <p>
 * Usage: <code>FontFeatures.of(style).caps()</code> is the effective capitals variant, <code>FontFeatures.of(style).value("tnum")</code> the value of a tag.
 * <p>
 * 스타일이 요구하는 OpenType 기능(CSS Fonts Level 4 기능 우선순위): <code>font-variant-*</code> 속성(과 <code>font-variant</code> 단축 속성)과 <code>font-kerning</code> 이 기능
 * 태그가 되고, <code>font-feature-settings</code> 의 태그는 그 뒤에 적용되어 이긴다(규격의 우선순위). JRE 6 의 Java2D 는 글꼴의 OpenType 기능을 켜고 끌 수 없으므로 이 엔진이 직접
 * 재현할 수 있는 기능만 화면에 효과가 있다: 작은 대문자(<code>smcp</code>, <code>c2sc</code>, <code>pcap</code>, <code>c2pc</code>. 소문자를 더 작은 대문자로 그린다), 위첨자와 아래첨자
 * (<code>sups</code>, <code>subs</code>. 글자를 줄이고 옮긴다), 전각 형태(<code>fwid</code>). 나머지(합자, 숫자, 대체 형태 등)는 여기서 해석되어 {@link #value} 로 물을 수 있지만
 * 화면에는 아무 변화가 없다.
 * <p>
 * 사용법: <code>FontFeatures.of(style).caps()</code> 는 실제 대문자 변형, <code>FontFeatures.of(style).value("tnum")</code> 은 태그의 값이다.
 */
public final class FontFeatures {

    /** The result for a style that asks for nothing special: every default. / 특별한 것을 요구하지 않는 스타일의 결과: 모두 기본값. */
    private static final FontFeatures DEFAULT = new FontFeatures(new HashMap<String, Integer>());

    /** The features that are on by default (Fonts 4: the features that a user agent enables without being asked). / 기본으로 켜진 기능. */
    private static final String[] ON_BY_DEFAULT = {"liga", "clig", "calt", "kern", "ccmp", "locl", "mark", "mkmk", "rlig"};

    /** The value of every tag that has been set, in effect after the precedence is applied. / 우선순위를 적용한 뒤 효력이 있는, 정해진 모든 태그의 값. */
    private final Map<String, Integer> features;

    /**
     * Creates the result.
     * <p>
     * 결과를 만든다.
     *
     * @param features the tags and their values / 태그와 값
     */
    private FontFeatures(Map<String, Integer> features) {
        this.features = features;
    }

    /**
     * Resolves the features of a style. A style that sets none of the font feature properties gets a shared default result without any work.
     * <p>
     * 스타일의 기능을 해석한다. 글꼴 기능 속성을 하나도 정하지 않은 스타일은 아무 작업 없이 공유 기본 결과를 받는다.
     *
     * @param s the style, may be null / 스타일. null 가능
     * @return the features / 기능
     */
    public static FontFeatures of(ComputedStyle s) {
        if (s == null) {
            return DEFAULT;
        }
        int[] ids = {Prop.FONT_VARIANT_CAPS, Prop.FONT_VARIANT_LIGATURES, Prop.FONT_VARIANT_NUMERIC, Prop.FONT_VARIANT_EAST_ASIAN,
            Prop.FONT_VARIANT_POSITION, Prop.FONT_VARIANT_ALTERNATES, Prop.FONT_KERNING, Prop.FONT_FEATURE_SETTINGS};
        boolean any = false;
        // 기본값이 아닌 속성이 하나라도 있는지 본다.
        for (int i = 0; i < ids.length && !any; i++) {
            CssValue v = s.get(ids[i]);
            any = v != null && !v.isKeyword("normal") && !v.isKeyword("auto");
        }
        if (!any) {
            return DEFAULT;
        }
        Map<String, Integer> f = new HashMap<String, Integer>();
        words(s.get(Prop.FONT_VARIANT_CAPS), f, "caps");
        words(s.get(Prop.FONT_VARIANT_LIGATURES), f, "lig");
        words(s.get(Prop.FONT_VARIANT_NUMERIC), f, "num");
        words(s.get(Prop.FONT_VARIANT_EAST_ASIAN), f, "ea");
        words(s.get(Prop.FONT_VARIANT_POSITION), f, "pos");
        words(s.get(Prop.FONT_VARIANT_ALTERNATES), f, "alt");
        CssValue kerning = s.get(Prop.FONT_KERNING);
        if (kerning != null && kerning.isKeyword("none")) {
            f.put("kern", Integer.valueOf(0));
        } else if (kerning != null && kerning.isKeyword("normal")) {
            f.put("kern", Integer.valueOf(1));
        }
        settings(s.get(Prop.FONT_FEATURE_SETTINGS), f);
        return new FontFeatures(f);
    }

    /**
     * Turns the keywords of one <code>font-variant-*</code> property into feature tags.
     * <p>
     * <code>font-variant-*</code> 속성 하나의 키워드를 기능 태그로 바꾼다.
     *
     * @param v    the value or null / 값. 없으면 null
     * @param f    receives the tags / 태그를 받는다
     * @param kind which property: caps, lig, num, ea, pos or alt / 어느 속성인지
     */
    private static void words(CssValue v, Map<String, Integer> f, String kind) {
        if (v == null || v.isKeyword("normal")) {
            return;
        }
        String text = " " + v.toCssText().toLowerCase() + " ";
        if (kind.equals("caps")) {
            on(text, f, "small-caps", "smcp");
            on(text, f, "all-small-caps", "smcp", "c2sc");
            on(text, f, "petite-caps", "pcap");
            on(text, f, "all-petite-caps", "pcap", "c2pc");
            on(text, f, "unicase", "unic");
            on(text, f, "titling-caps", "titl");
        } else if (kind.equals("lig")) {
            // none 은 모든 합자를 끄고, 나머지 키워드는 각자의 기능을 켜거나 끈다.
            if (text.indexOf(" none ") >= 0) {
                String[] all = {"liga", "clig", "dlig", "hlig", "calt"};
                // 합자 기능 태그를 모두 끈다.
                for (int i = 0; i < all.length; i++) {
                    f.put(all[i], Integer.valueOf(0));
                }
            }
            on(text, f, "common-ligatures", "liga", "clig");
            off(text, f, "no-common-ligatures", "liga", "clig");
            on(text, f, "discretionary-ligatures", "dlig");
            off(text, f, "no-discretionary-ligatures", "dlig");
            on(text, f, "historical-ligatures", "hlig");
            off(text, f, "no-historical-ligatures", "hlig");
            on(text, f, "contextual", "calt");
            off(text, f, "no-contextual", "calt");
        } else if (kind.equals("num")) {
            on(text, f, "lining-nums", "lnum");
            on(text, f, "oldstyle-nums", "onum");
            on(text, f, "proportional-nums", "pnum");
            on(text, f, "tabular-nums", "tnum");
            on(text, f, "diagonal-fractions", "frac");
            on(text, f, "stacked-fractions", "afrc");
            on(text, f, "ordinal", "ordn");
            on(text, f, "slashed-zero", "zero");
        } else if (kind.equals("ea")) {
            on(text, f, "jis78", "jp78");
            on(text, f, "jis83", "jp83");
            on(text, f, "jis90", "jp90");
            on(text, f, "jis04", "jp04");
            on(text, f, "simplified", "smpl");
            on(text, f, "traditional", "trad");
            on(text, f, "full-width", "fwid");
            on(text, f, "proportional-width", "pwid");
            on(text, f, "ruby", "ruby");
        } else if (kind.equals("pos")) {
            on(text, f, "sub", "subs");
            on(text, f, "super", "sups");
        } else {
            on(text, f, "historical-forms", "hist");
        }
    }

    /**
     * Sets tags to 1 if a keyword occurs in the text.
     * <p>
     * 키워드가 텍스트에 있으면 태그를 1 로 정한다.
     *
     * @param text the property text with a space on each side / 앞뒤에 공백이 있는 속성 텍스트
     * @param f    receives the tags / 태그를 받는다
     * @param word the keyword / 키워드
     * @param tags the tags that it turns on / 켜지는 태그
     */
    private static void on(String text, Map<String, Integer> f, String word, String... tags) {
        if (text.indexOf(" " + word + " ") >= 0) {
            // 키워드가 켜는 태그를 모두 1 로 정한다.
            for (int i = 0; i < tags.length; i++) {
                f.put(tags[i], Integer.valueOf(1));
            }
        }
    }

    /**
     * Sets tags to 0 if a keyword occurs in the text.
     * <p>
     * 키워드가 텍스트에 있으면 태그를 0 으로 정한다.
     *
     * @param text the property text with a space on each side / 앞뒤에 공백이 있는 속성 텍스트
     * @param f    receives the tags / 태그를 받는다
     * @param word the keyword / 키워드
     * @param tags the tags that it turns off / 꺼지는 태그
     */
    private static void off(String text, Map<String, Integer> f, String word, String... tags) {
        if (text.indexOf(" " + word + " ") >= 0) {
            // 키워드가 끄는 태그를 모두 0 으로 정한다.
            for (int i = 0; i < tags.length; i++) {
                f.put(tags[i], Integer.valueOf(0));
            }
        }
    }

    /**
     * Applies <code>font-feature-settings</code>: a list of a tag (a string of four characters) with an optional integer or <code>on</code>/<code>off</code>
     * (<code>on</code> is the default). A later entry of the same tag replaces an earlier one.
     * <p>
     * <code>font-feature-settings</code> 를 적용한다: 태그(네 글자 문자열)와 선택적인 정수 또는 <code>on</code>/<code>off</code>(기본은 <code>on</code>)의 목록. 같은 태그가 나중에
     * 나오면 앞의 것을 대체한다.
     *
     * @param v the value or null / 값. 없으면 null
     * @param f receives the tags / 태그를 받는다
     */
    private static void settings(CssValue v, Map<String, Integer> f) {
        if (v == null || v.isKeyword("normal")) {
            return;
        }
        List<CssValue> items = v instanceof CssList && ((CssList) v).isComma() ? ((CssList) v).getItems() : java.util.Collections.singletonList(v);
        // 쉼표로 나눈 항목마다 태그와 값을 읽는다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            CssValue tag = it;
            CssValue val = null;
            if (it instanceof CssList && !((CssList) it).isComma() && ((CssList) it).size() >= 1) {
                tag = ((CssList) it).get(0);
                val = ((CssList) it).size() > 1 ? ((CssList) it).get(1) : null;
            }
            if (!(tag instanceof CssString) || ((CssString) tag).getValue().length() != 4) {
                continue;
            }
            int n = 1;
            if (val instanceof CssNumber) {
                n = (int) ((CssNumber) val).getValue();
            } else if (val != null && val.isKeyword("off")) {
                n = 0;
            }
            f.put(((CssString) tag).getValue(), Integer.valueOf(n));
        }
    }

    /**
     * Returns the glyph alternate features that a style asks for from a font of a family (CSS Fonts 4 sections 6.8 and 6.9): the function values of
     * <code>font-variant-alternates</code> with their names looked up in the <code>@font-feature-values</code> rules of the family (<code>stylistic(n)</code>
     * is <code>salt</code>, <code>styleset(n#)</code> turns on <code>ss01</code> to <code>ss99</code>, <code>character-variant(n#)</code> sets
     * <code>cv01</code> to <code>cv99</code>, <code>swash(n)</code> is <code>swsh</code> and <code>cswh</code>, <code>ornaments(n)</code> is <code>ornm</code>,
     * <code>annotation(n)</code> is <code>nalt</code>, <code>historical-forms</code> is <code>hist</code>; an undefined name does nothing), then the same tags
     * of <code>font-feature-settings</code>, which win (feature precedence).
     * <p>
     * 스타일이 패밀리의 글꼴에 요구하는 대체 글리프 기능을 반환한다(CSS Fonts 4 6.8, 6.9 절): <code>font-variant-alternates</code> 의 함수 값의 이름을 그 패밀리의
     * <code>@font-feature-values</code> 규칙에서 찾은 것(<code>stylistic(n)</code> 은 <code>salt</code>, <code>styleset(n#)</code> 은 <code>ss01</code>~<code>ss99</code> 를 켬,
     * <code>character-variant(n#)</code> 은 <code>cv01</code>~<code>cv99</code> 를 정함, <code>swash(n)</code> 은 <code>swsh</code> 와 <code>cswh</code>, <code>ornaments(n)</code> 은
     * <code>ornm</code>, <code>annotation(n)</code> 은 <code>nalt</code>, <code>historical-forms</code> 는 <code>hist</code>. 정의되지 않은 이름은 아무것도 하지 않는다), 그다음 이기는
     * <code>font-feature-settings</code> 의 같은 태그(기능 우선순위).
     *
     * @param s      the style / 스타일
     * @param rules  the <code>@font-feature-values</code> rules in document order, may be null / 문서 순서의 <code>@font-feature-values</code> 규칙. null 가능
     * @param family the family of the font / 글꼴의 패밀리
     * @return the tags and their values (empty if none) / 태그와 값(없으면 비어 있음)
     */
    public static Map<String, Integer> glyphFeatures(ComputedStyle s, List<org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule> rules, String family) {
        Map<String, Integer> f = new java.util.TreeMap<String, Integer>();
        if (s == null) {
            return f;
        }
        CssValue alt = s.get(Prop.FONT_VARIANT_ALTERNATES);
        List<CssValue> items = alt instanceof CssList ? ((CssList) alt).getItems() : java.util.Collections.singletonList(alt);
        // 값의 항목마다 함수의 이름을 패밀리의 기능 값에서 찾아 태그로 바꾼다.
        for (int i = 0; i < items.size(); i++) {
            CssValue it = items.get(i);
            if (it == null) {
                continue;
            }
            if (it.isKeyword("historical-forms")) {
                f.put("hist", Integer.valueOf(1));
                continue;
            }
            if (!(it instanceof org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue)) {
                continue;
            }
            org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue fn = (org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue) it;
            String type = fn.getName().toLowerCase(java.util.Locale.ENGLISH);
            List<CssValue> names = new ArrayList<CssValue>();
            // 인수(쉼표 목록이나 공백 목록)에서 이름을 모은다.
            for (int k = 0; k < fn.getArguments().size(); k++) {
                CssValue a = fn.getArguments().get(k);
                if (a instanceof CssList) {
                    names.addAll(((CssList) a).getItems());
                } else {
                    names.add(a);
                }
            }
            // 이름마다 기능 값을 찾아 태그를 정한다.
            for (int k = 0; k < names.size(); k++) {
                if (!(names.get(k) instanceof CssKeyword)) {
                    continue;
                }
                String name = ((CssKeyword) names.get(k)).getName();
                int[] v = org.duckdns.hjow.swingwebpanel.css.cssom.FontFeatureValuesRule.lookup(rules, family, type, name);
                if (v == null || v.length == 0) {
                    continue;
                }
                if ("stylistic".equals(type)) {
                    f.put("salt", Integer.valueOf(v[0]));
                } else if ("swash".equals(type)) {
                    f.put("swsh", Integer.valueOf(v[0]));
                    f.put("cswh", Integer.valueOf(v[0]));
                } else if ("ornaments".equals(type)) {
                    f.put("ornm", Integer.valueOf(v[0]));
                } else if ("annotation".equals(type)) {
                    f.put("nalt", Integer.valueOf(v[0]));
                } else if ("styleset".equals(type)) {
                    // 스타일 세트는 값마다 ss01~ss99 를 켠다(0 이나 99 보다 큰 값은 아무것도 켜지 않는다).
                    for (int j = 0; j < v.length; j++) {
                        if (v[j] >= 1 && v[j] <= 99) {
                            f.put(v[j] < 10 ? "ss0" + v[j] : "ss" + v[j], Integer.valueOf(1));
                        }
                    }
                } else if ("character-variant".equals(type) && v[0] >= 1 && v[0] <= 99) {
                    f.put(v[0] < 10 ? "cv0" + v[0] : "cv" + v[0], Integer.valueOf(v.length > 1 ? v[1] : 1));
                }
            }
        }
        Map<String, Integer> settings = new HashMap<String, Integer>();
        settings(s.get(Prop.FONT_FEATURE_SETTINGS), settings);
        // font-feature-settings 의 대체 글리프 태그가 마지막에 이긴다.
        for (Map.Entry<String, Integer> e : settings.entrySet()) {
            String t = e.getKey();
            if (t.equals("salt") || t.equals("swsh") || t.equals("cswh") || t.equals("ornm") || t.equals("nalt") || t.equals("hist") || t.matches("(ss|cv)[0-9][0-9]")) {
                f.put(t, e.getValue());
            }
        }
        return f;
    }

    /**
     * Returns the value of a feature: what was set, otherwise 1 for the features that are on by default and 0 for the rest.
     * <p>
     * 기능의 값을 반환한다: 정해진 값, 없으면 기본으로 켜진 기능은 1, 나머지는 0.
     *
     * @param tag the four character tag / 네 글자 태그
     * @return the value (0 is off) / 값(0 이면 꺼짐)
     */
    public int value(String tag) {
        Integer v = features.get(tag);
        if (v != null) {
            return v.intValue();
        }
        // 정해지지 않은 기능은 기본으로 켜지는 것만 1 이다.
        for (int i = 0; i < ON_BY_DEFAULT.length; i++) {
            if (ON_BY_DEFAULT[i].equals(tag)) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * Returns the capitals variant that the features give: <code>normal</code>, <code>small-caps</code>, <code>all-small-caps</code>, <code>petite-caps</code>,
     * <code>all-petite-caps</code>, <code>unicase</code> or <code>titling-caps</code>.
     * <p>
     * 기능이 주는 대문자 변형을 반환한다.
     *
     * @return the variant / 변형
     */
    public String caps() {
        if (value("c2sc") > 0 && value("smcp") > 0) {
            return "all-small-caps";
        }
        if (value("smcp") > 0) {
            return "small-caps";
        }
        if (value("c2pc") > 0 && value("pcap") > 0) {
            return "all-petite-caps";
        }
        if (value("pcap") > 0) {
            return "petite-caps";
        }
        if (value("unic") > 0) {
            return "unicase";
        }
        return value("titl") > 0 ? "titling-caps" : "normal";
    }

    /**
     * Returns the position variant: <code>normal</code>, <code>sub</code> or <code>super</code> (<code>sups</code> wins when both are on).
     * <p>
     * 위치 변형을 반환한다: <code>normal</code>, <code>sub</code>, <code>super</code>(둘 다 켜지면 <code>sups</code> 가 이긴다).
     *
     * @return the variant / 변형
     */
    public String position() {
        if (value("sups") > 0) {
            return "super";
        }
        return value("subs") > 0 ? "sub" : "normal";
    }

    /**
     * Tells whether the full width forms are asked for (<code>fwid</code>).
     * <p>
     * 전각 형태를 요구하는지(<code>fwid</code>) 알려 준다.
     *
     * @return true for full width forms / 전각이면 true
     */
    public boolean fullWidth() {
        return value("fwid") > 0;
    }

    /**
     * Tells whether the user agent may synthesize a face or a variant that the font does not have: the longhand of the kind (<code>font-synthesis-weight</code>,
     * <code>-style</code>, <code>-small-caps</code> or <code>-position</code>, set together by the shorthand <code>font-synthesis</code>) must not be <code>none</code>.
     * This engine has no OpenType glyphs for small capitals and the positions, so every one of them is synthesized; the bold and the italic are made by the JRE.
     * <p>
     * 글꼴에 없는 면이나 변형을 사용자 에이전트가 합성해도 되는지 알려 준다: 그 종류의 개별 속성(<code>font-synthesis-weight</code>, <code>-style</code>, <code>-small-caps</code>,
     * <code>-position</code>. 단축 속성 <code>font-synthesis</code> 가 함께 정한다)이 <code>none</code> 이 아니어야 한다. 이 엔진에는 작은 대문자와 위치 변형의 OpenType 글리프가 없으므로
     * 모두 합성하며, 굵게와 기울임은 JRE 가 만든다.
     *
     * @param s    the style / 스타일
     * @param kind <code>weight</code>, <code>style</code>, <code>small-caps</code> or <code>position</code> / 합성의 종류
     * @return true if the synthesis is allowed / 합성이 허용되면 true
     */
    public static boolean mayRealize(ComputedStyle s, String kind) {
        // 종류마다 개별 속성(font-synthesis-weight/-style/-small-caps/-position)이 none 이면 그 합성을 금지한다(CSS Fonts 4 2.8).
        int id;
        if ("weight".equals(kind)) {
            id = Prop.FONT_SYNTHESIS_WEIGHT;
        } else if ("style".equals(kind)) {
            id = Prop.FONT_SYNTHESIS_STYLE;
        } else if ("small-caps".equals(kind)) {
            id = Prop.FONT_SYNTHESIS_SMALL_CAPS;
        } else {
            id = Prop.FONT_SYNTHESIS_POSITION;
        }
        CssValue v = s.get(id);
        return v == null || !v.isKeyword("none");
    }
}
