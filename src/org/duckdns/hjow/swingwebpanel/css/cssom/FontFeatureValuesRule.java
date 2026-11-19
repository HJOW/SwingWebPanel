package org.duckdns.hjow.swingwebpanel.css.cssom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.syntax.CssEscape;

/**
 * An <code>@font-feature-values</code> rule (CSS Fonts Level 4 section 6.9, https://www.w3.org/TR/css-fonts-4/ WD 12 November 2026, and its CSSOM
 * interface <code>CSSFontFeatureValuesRule</code>): a list of named font families and, for each type of feature value block (<code>@swash</code>,
 * <code>@stylistic</code>, <code>@ornaments</code>, <code>@annotation</code>, <code>@character-variant</code>, <code>@styleset</code>,
 * <code>@historical-forms</code>), a map from case-sensitive names to lists of non-negative feature indexes, plus the <code>font-display</code> descriptor
 * that sets the default display policy of the faces of the families. The function values of <code>font-variant-alternates</code> look their names up
 * here for the family of the font in use ({@link #lookup}).
 * <p>
 * Use: the style sheet builder creates the rule from <code>@font-feature-values Family { @styleset { nice: 3; } }</code>; scripts change the maps through
 * {@link ValueMap} (the <code>CSSFontFeatureValuesMap</code> maplike); {@link #getCssText()} serializes the rule as the specification says.
 * <p>
 * <code>@font-feature-values</code> 규칙(CSS Fonts Level 4 6.9 절과 CSSOM 인터페이스 <code>CSSFontFeatureValuesRule</code>): 이름 있는 글꼴 패밀리의 목록과, 기능 값 블록 종류
 * (<code>@swash</code>, <code>@stylistic</code>, <code>@ornaments</code>, <code>@annotation</code>, <code>@character-variant</code>, <code>@styleset</code>,
 * <code>@historical-forms</code>)마다 대소문자를 구분하는 이름에서 음이 아닌 기능 번호 목록으로 가는 맵, 그리고 패밀리의 글꼴 면의 기본 표시 정책을 정하는 <code>font-display</code>
 * 서술자. <code>font-variant-alternates</code> 의 함수 값은 쓰는 글꼴의 패밀리에 대해 여기서 이름을 찾는다({@link #lookup}).
 * <p>
 * 사용법: 스타일 시트 빌더가 <code>@font-feature-values Family { @styleset { nice: 3; } }</code> 에서 규칙을 만들고, 스크립트는 {@link ValueMap}
 * (<code>CSSFontFeatureValuesMap</code> maplike)으로 맵을 바꾸며, {@link #getCssText()} 는 규격대로 규칙을 직렬화한다.
 */
public final class FontFeatureValuesRule extends CssRule {

    /**
     * The types of feature value blocks in the order of serialization (CSS Fonts 4 section 9.1, "serialize an @font-feature-values rule").
     * <p>
     * 직렬화 순서의 기능 값 블록 종류(CSS Fonts 4 9.1 절).
     */
    public static final String[] TYPES = { "swash", "stylistic", "ornaments", "annotation", "character-variant", "styleset", "historical-forms" };

    /** The font families (as written, without quotes). 글꼴 패밀리(쓴 그대로, 따옴표 없이). */
    private List<String> families;

    /** The last valid <code>font-display</code> value, or null. 마지막으로 올바른 <code>font-display</code> 값. 없으면 null. */
    private final String fontDisplay;

    /** The maps by type, in the order of {@link #TYPES}. {@link #TYPES} 순서의 종류별 맵. */
    private final ValueMap[] maps = new ValueMap[TYPES.length];

    /**
     * Creates a rule.
     * <p>
     * 규칙을 만든다.
     *
     * @param families    the font families / 글꼴 패밀리
     * @param fontDisplay the <code>font-display</code> value or null / <code>font-display</code> 값 또는 null
     */
    public FontFeatureValuesRule(List<String> families, String fontDisplay) {
        this.families = new ArrayList<String>(families);
        this.fontDisplay = fontDisplay;
        // 종류마다 빈 맵을 만든다.
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new ValueMap(this, TYPES[i]);
        }
    }

    /**
     * Returns the index of a type in {@link #TYPES}, or -1 (the type is lower-cased first).
     * <p>
     * {@link #TYPES} 에서 종류의 위치를 반환한다. 없으면 -1(먼저 소문자로 바꾼다).
     *
     * @param type the type without the <code>@</code> / <code>@</code> 를 뺀 종류
     * @return the index or -1 / 위치 또는 -1
     */
    public static int typeIndex(String type) {
        String t = type.toLowerCase(Locale.ENGLISH);
        // 종류 이름을 차례로 비교한다.
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(t)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Tells whether a list of indexes is valid for a type (CSS Fonts 4 section 6.9.1): one index for <code>@swash</code>, <code>@stylistic</code>,
     * <code>@ornaments</code>, <code>@annotation</code> and <code>@historical-forms</code>; one or two for <code>@character-variant</code>, the first at most
     * 99; one or more for <code>@styleset</code>.
     * <p>
     * 번호 목록이 종류에 올바른지 알려 준다(CSS Fonts 4 6.9.1 절): <code>@swash</code>, <code>@stylistic</code>, <code>@ornaments</code>, <code>@annotation</code>,
     * <code>@historical-forms</code> 는 하나, <code>@character-variant</code> 는 하나나 둘(첫째는 99 이하), <code>@styleset</code> 은 하나 이상.
     *
     * @param type    the type index / 종류 위치
     * @param indexes the indexes / 번호
     * @return true if valid / 올바르면 true
     */
    public static boolean validFor(int type, int[] indexes) {
        if (indexes.length == 0) {
            return false;
        }
        // 번호는 음이 아니어야 한다.
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] < 0) {
                return false;
            }
        }
        String t = TYPES[type];
        if ("styleset".equals(t)) {
            return true;
        }
        if ("character-variant".equals(t)) {
            return indexes.length <= 2;
        }
        return indexes.length == 1;
    }

    /**
     * Returns the map of a type.
     * <p>
     * 종류의 맵을 반환한다.
     *
     * @param type the type (<code>swash</code>, <code>character-variant</code> ...) / 종류
     * @return the map, or null for an unknown type / 맵. 모르는 종류이면 null
     */
    public ValueMap getMap(String type) {
        int i = typeIndex(type);
        return i < 0 ? null : maps[i];
    }

    /**
     * Returns the families.
     * <p>
     * 패밀리를 반환한다.
     *
     * @return an unmodifiable list / 수정할 수 없는 목록
     */
    public List<String> getFamilies() {
        return Collections.unmodifiableList(families);
    }

    /**
     * Returns the <code>fontFamily</code> attribute: the families serialized as a comma separated list (names that are not plain identifiers are quoted).
     * <p>
     * <code>fontFamily</code> 속성을 반환한다: 쉼표로 나눈 패밀리 목록(평범한 식별자가 아닌 이름은 따옴표로 감싼다).
     *
     * @return the text / 텍스트
     */
    public String getFontFamily() {
        StringBuilder b = new StringBuilder();
        // 패밀리마다 직렬화해 쉼표로 잇는다.
        for (int i = 0; i < families.size(); i++) {
            if (i > 0) {
                b.append(", ");
            }
            String f = families.get(i);
            b.append(f.matches("[A-Za-z_][A-Za-z0-9_-]*( [A-Za-z_][A-Za-z0-9_-]*)*") ? f : CssEscape.string(f));
        }
        return b.toString();
    }

    /**
     * Sets the <code>fontFamily</code> attribute: a comma separated list of family names; a text that is not such a list is ignored.
     * <p>
     * <code>fontFamily</code> 속성을 정한다: 쉼표로 나눈 패밀리 이름 목록. 그런 목록이 아닌 텍스트는 무시한다.
     *
     * @param text the text / 텍스트
     */
    public void setFontFamily(String text) {
        List<String> parsed = StyleSheetBuilder.parseFamilyNames(org.duckdns.hjow.swingwebpanel.css.syntax.CssParser.parseComponentValues(text));
        if (parsed != null) {
            families = parsed;
            changed();
        }
    }

    /**
     * Returns the <code>font-display</code> descriptor, or null if the rule has none.
     * <p>
     * <code>font-display</code> 서술자를 반환한다. 없으면 null.
     *
     * @return the keyword or null / 키워드 또는 null
     */
    public String getFontDisplay() {
        return fontDisplay;
    }

    /**
     * Returns the type of this rule.
     * <p>
     * 이 규칙의 종류를 반환한다.
     *
     * @return {@link CssRule#FONT_FEATURE_VALUES_RULE} / {@link CssRule#FONT_FEATURE_VALUES_RULE}
     */
    public int getType() {
        return FONT_FEATURE_VALUES_RULE;
    }

    /**
     * Serializes the rule (CSS Fonts 4 section 9.1): the families, the <code>font-display</code> descriptor and every non-empty map in the order of
     * {@link #TYPES}.
     * <p>
     * 규칙을 직렬화한다(CSS Fonts 4 9.1 절): 패밀리, <code>font-display</code> 서술자, {@link #TYPES} 순서로 비어 있지 않은 맵.
     *
     * @return the CSS text / CSS 텍스트
     */
    public String getCssText() {
        StringBuilder s = new StringBuilder("@font-feature-values ");
        s.append(getFontFamily()).append(" {");
        if (fontDisplay != null) {
            s.append(" font-display: ").append(fontDisplay).append(';');
        }
        // 종류마다 비어 있지 않은 맵을 블록으로 적는다.
        for (int i = 0; i < maps.length; i++) {
            Map<String, int[]> m = maps[i].entries;
            if (m.isEmpty()) {
                continue;
            }
            s.append(" @").append(TYPES[i]).append(" {");
            // 이름마다 번호를 공백으로 이어 적는다.
            for (Map.Entry<String, int[]> e : m.entrySet()) {
                s.append(' ').append(CssEscape.ident(e.getKey())).append(": ");
                int[] v = e.getValue();
                // 번호를 공백으로 잇는다.
                for (int k = 0; k < v.length; k++) {
                    if (k > 0) {
                        s.append(' ');
                    }
                    s.append(v[k]);
                }
                s.append(';');
            }
            s.append(" }");
        }
        return s.append(" }").toString();
    }

    /**
     * Looks a name up in the rules for a family (CSS Fonts 4 section 6.9.1): the rules are given in document order, a later definition of the same
     * (family, type, name) wins, and the families are compared without regard to ASCII case.
     * <p>
     * 패밀리에 대해 규칙에서 이름을 찾는다(CSS Fonts 4 6.9.1 절): 규칙은 문서 순서로 주며 같은 (패밀리, 종류, 이름)의 나중 정의가 이기고, 패밀리는 ASCII 대소문자를 구분하지 않고
     * 비교한다.
     *
     * @param rules  the rules in document order / 문서 순서의 규칙
     * @param family the family of the font / 글꼴의 패밀리
     * @param type   the type (<code>styleset</code> ...) / 종류
     * @param name   the name / 이름
     * @return the indexes, or null if the name is not defined for the family / 번호. 그 패밀리에 정의되지 않았으면 null
     */
    public static int[] lookup(List<FontFeatureValuesRule> rules, String family, String type, String name) {
        int t = typeIndex(type);
        if (rules == null || t < 0 || family == null) {
            return null;
        }
        int[] found = null;
        // 문서 순서로 훑어 나중 정의가 앞의 것을 대체하게 한다.
        for (int i = 0; i < rules.size(); i++) {
            FontFeatureValuesRule r = rules.get(i);
            boolean matches = false;
            // 규칙의 패밀리 중 같은 이름이 있는지 본다.
            for (int k = 0; k < r.families.size() && !matches; k++) {
                matches = r.families.get(k).equalsIgnoreCase(family);
            }
            int[] v = matches ? r.maps[t].entries.get(name) : null;
            if (v != null) {
                found = v;
            }
        }
        return found == null ? null : found.clone();
    }

    /**
     * Returns the <code>font-display</code> that the <code>@font-feature-values</code> rules give a family (CSS Fonts 4 section 4.9.1): the last rule in
     * document order that names the family and has the descriptor.
     * <p>
     * <code>@font-feature-values</code> 규칙이 패밀리에 주는 <code>font-display</code> 를 반환한다(CSS Fonts 4 4.9.1 절): 패밀리를 이름에 담고 서술자가 있는 문서 순서의 마지막 규칙.
     *
     * @param rules  the rules in document order, may be null / 문서 순서의 규칙. null 가능
     * @param family the family / 패밀리
     * @return the value, or null / 값. 없으면 null
     */
    public static String fontDisplay(List<FontFeatureValuesRule> rules, String family) {
        String found = null;
        // 문서 순서로 훑어 나중 규칙이 이기게 한다.
        for (int i = 0; rules != null && i < rules.size(); i++) {
            FontFeatureValuesRule r = rules.get(i);
            if (r.fontDisplay == null) {
                continue;
            }
            // 규칙의 패밀리 중 같은 이름이 있는지 본다.
            for (int k = 0; k < r.families.size(); k++) {
                if (r.families.get(k).equalsIgnoreCase(family)) {
                    found = r.fontDisplay;
                }
            }
        }
        return found;
    }

    /**
     * The map of one type of feature value block (the <code>CSSFontFeatureValuesMap</code> maplike of the CSSOM): names in the order of their first
     * definition, each with its list of indexes. Setting a name keeps its place and replaces its value.
     * <p>
     * 기능 값 블록 한 종류의 맵(CSSOM 의 <code>CSSFontFeatureValuesMap</code> maplike): 처음 정의한 순서의 이름과 각각의 번호 목록. 이름을 다시 정하면 자리를 지키고 값을 바꾼다.
     */
    public static final class ValueMap {
        /** The rule. 규칙. */
        private final FontFeatureValuesRule rule;
        /** The type of the block. 블록의 종류. */
        private final String type;
        /** The entries. 항목. */
        final LinkedHashMap<String, int[]> entries = new LinkedHashMap<String, int[]>();

        /**
         * Creates an empty map.
         * <p>
         * 빈 맵을 만든다.
         *
         * @param rule the rule / 규칙
         * @param type the type / 종류
         */
        ValueMap(FontFeatureValuesRule rule, String type) {
            this.rule = rule;
            this.type = type;
        }

        /**
         * Returns the type of the block (<code>styleset</code> ...).
         * <p>
         * 블록의 종류(<code>styleset</code> 등)를 반환한다.
         *
         * @return the type / 종류
         */
        public String getType() {
            return type;
        }

        /**
         * Defines a name while the rule is built (an invalid list is ignored, as an invalid declaration is).
         * <p>
         * 규칙을 만드는 동안 이름을 정의한다(올바르지 않은 목록은 올바르지 않은 선언처럼 무시한다).
         *
         * @param name    the name / 이름
         * @param indexes the indexes / 번호
         * @return true if it was defined / 정의했으면 true
         */
        boolean define(String name, int[] indexes) {
            if (!validFor(typeIndex(type), indexes)) {
                return false;
            }
            entries.put(name, indexes.clone());
            return true;
        }

        /**
         * Sets a name (the <code>set</code> method): the indexes are checked like a declaration of the block.
         * <p>
         * 이름을 정한다(<code>set</code> 메서드): 번호는 블록의 선언처럼 확인한다.
         *
         * @param name    the name / 이름
         * @param indexes the indexes / 번호
         * @throws IllegalArgumentException if the indexes are not valid for the type / 번호가 종류에 올바르지 않으면
         */
        public void set(String name, int[] indexes) {
            if (!define(name, indexes)) {
                throw new IllegalArgumentException("invalid feature indexes for @" + type);
            }
            rule.changed();
        }

        /**
         * Returns the indexes of a name, or null.
         * <p>
         * 이름의 번호를 반환한다. 없으면 null.
         *
         * @param name the name / 이름
         * @return a copy of the indexes or null / 번호의 복사본 또는 null
         */
        public int[] get(String name) {
            int[] v = entries.get(name);
            return v == null ? null : v.clone();
        }

        /**
         * Tells whether a name is defined.
         * <p>
         * 이름이 정의되어 있는지 알려 준다.
         *
         * @param name the name / 이름
         * @return true if it is / 있으면 true
         */
        public boolean has(String name) {
            return entries.containsKey(name);
        }

        /**
         * Removes a name.
         * <p>
         * 이름을 지운다.
         *
         * @param name the name / 이름
         * @return true if it was defined / 있었으면 true
         */
        public boolean delete(String name) {
            boolean had = entries.remove(name) != null;
            if (had) {
                rule.changed();
            }
            return had;
        }

        /**
         * Removes every name.
         * <p>
         * 모든 이름을 지운다.
         */
        public void clear() {
            if (!entries.isEmpty()) {
                entries.clear();
                rule.changed();
            }
        }

        /**
         * Returns the number of names.
         * <p>
         * 이름의 수를 반환한다.
         *
         * @return the size / 크기
         */
        public int getSize() {
            return entries.size();
        }

        /**
         * Returns the names in order.
         * <p>
         * 이름을 순서대로 반환한다.
         *
         * @return a new list / 새 목록
         */
        public List<String> names() {
            return new ArrayList<String>(entries.keySet());
        }
    }
}
