package org.duckdns.hjow.swingwebpanel.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.duckdns.hjow.swingwebpanel.css.cssom.CounterStyleRule;
import org.duckdns.hjow.swingwebpanel.css.syntax.ComponentValue;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssBlock;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssFunction;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssParser;
import org.duckdns.hjow.swingwebpanel.css.syntax.CssToken;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssString;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;

/**
 * The counter styles of a document (CSS Counter Styles Level 3): the predefined ones (<code>decimal</code>, <code>decimal-leading-zero</code>,
 * <code>lower-roman</code>, <code>upper-roman</code>, <code>lower-alpha</code>/<code>lower-latin</code>, <code>upper-alpha</code>/<code>upper-latin</code>,
 * <code>lower-greek</code>, <code>disc</code>, <code>circle</code>, <code>square</code>, <code>disclosure-open</code>, <code>disclosure-closed</code>,
 * <code>cjk-decimal</code>, the digits of the Indic, Arabic, Thai, Tibetan, Khmer, Lao and Myanmar scripts, <code>hiragana</code>,
 * <code>katakana</code>, <code>hiragana-iroha</code>, <code>katakana-iroha</code>, <code>korean-hangul-formal</code>, <code>korean-hanja-informal</code>), the
 * styles that the style sheets define with <code>@counter-style</code> (the descriptors are checked here: a definition that is not valid for its system is
 * ignored), and the anonymous styles of <code>symbols()</code>. It writes the marker of a list item (<code>list-style-type</code>, with the prefix and the
 * suffix) and the number of <code>counter()</code> and <code>counters()</code> (without them), and goes through the fallback styles for the values a
 * style cannot write; a cycle of fallbacks ends in <code>decimal</code>.
 * <p>
 * 문서의 카운터 스타일(CSS Counter Styles Level 3): 미리 정의된 것(<code>decimal</code>, <code>decimal-leading-zero</code>, <code>lower-roman</code>,
 * <code>upper-roman</code>, <code>lower-alpha</code>/<code>lower-latin</code>, <code>upper-alpha</code>/<code>upper-latin</code>, <code>lower-greek</code>,
 * <code>disc</code>, <code>circle</code>, <code>square</code>, <code>disclosure-open</code>, <code>disclosure-closed</code>, <code>cjk-decimal</code>, 인도계,
 * 아랍, 태국, 티베트, 크메르, 라오, 미얀마 문자의 숫자, <code>hiragana</code>, <code>katakana</code>, <code>hiragana-iroha</code>, <code>katakana-iroha</code>,
 * <code>korean-hangul-formal</code>, <code>korean-hanja-informal</code>), 스타일 시트가 <code>@counter-style</code> 로 정의한 것(서술자는 여기서 검사하며 시스템에 맞지 않는
 * 정의는 무시한다), <code>symbols()</code> 의 이름 없는 스타일. 목록 항목의 표식(<code>list-style-type</code>, 접두사와 접미사 포함)과 <code>counter()</code>,
 * <code>counters()</code> 의 숫자(접두사, 접미사 없이)를 쓰며, 스타일이 쓸 수 없는 값은 대체 스타일을 거친다. 대체의 순환은 <code>decimal</code> 에서 끝난다.
 */
final class CounterStyles {

    /** The counter styles that every document has, by name. / 모든 문서가 가진 카운터 스타일. 이름별. */
    private static final Map<String, CounterStyle> PREDEFINED = new HashMap<String, CounterStyle>();

    /** The default set, which has no style sheet definitions. / 스타일 시트 정의가 없는 기본 집합. */
    static final CounterStyles DEFAULT = new CounterStyles(null);

    /** The definitions of the style sheets by name, or null. / 스타일 시트의 이름별 정의. 없으면 null. */
    private final Map<String, CounterStyleRule> rules;

    /** The styles built from the definitions, by name (null values for invalid definitions). / 정의에서 만든 스타일. 이름별(올바르지 않은 정의는 null 값). */
    private final Map<String, CounterStyle> custom = new HashMap<String, CounterStyle>();

    static {
        add("decimal", CounterStyle.NUMERIC, "0123456789", ". ");
        CounterStyle z = PREDEFINED.get("decimal");
        CounterStyle lz = copy(z);
        lz.padLength = 2;
        lz.padSymbol = "0";
        PREDEFINED.put("decimal-leading-zero", lz);
        add("lower-alpha", CounterStyle.ALPHABETIC, "abcdefghijklmnopqrstuvwxyz", ". ");
        add("upper-alpha", CounterStyle.ALPHABETIC, "ABCDEFGHIJKLMNOPQRSTUVWXYZ", ". ");
        PREDEFINED.put("lower-latin", PREDEFINED.get("lower-alpha"));
        PREDEFINED.put("upper-latin", PREDEFINED.get("upper-alpha"));
        add("lower-greek", CounterStyle.ALPHABETIC, "αβγδεζηθικλμνξοπρστυφχψω", ". ");
        addRoman("lower-roman", new String[] {"m", "cm", "d", "cd", "c", "xc", "l", "xl", "x", "ix", "v", "iv", "i"});
        addRoman("upper-roman", new String[] {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"});
        add("disc", CounterStyle.CYCLIC, "•", " ");
        add("circle", CounterStyle.CYCLIC, "◦", " ");
        add("square", CounterStyle.CYCLIC, "▪", " ");
        add("disclosure-open", CounterStyle.CYCLIC, "▾", " ");
        add("disclosure-closed", CounterStyle.CYCLIC, "▸", " ");
        add("cjk-decimal", CounterStyle.NUMERIC, "〇一二三四五六七八九", "、");
        add("arabic-indic", CounterStyle.NUMERIC, "٠١٢٣٤٥٦٧٨٩", ". ");
        add("persian", CounterStyle.NUMERIC, "۰۱۲۳۴۵۶۷۸۹", ". ");
        add("devanagari", CounterStyle.NUMERIC, "०१२३४५६७८९", ". ");
        add("bengali", CounterStyle.NUMERIC, "০১২৩৪৫৬৭৮৯", ". ");
        add("gujarati", CounterStyle.NUMERIC, "૦૧૨૩૪૫૬૭૮૯", ". ");
        add("gurmukhi", CounterStyle.NUMERIC, "੦੧੨੩੪੫੬੭੮੯", ". ");
        add("kannada", CounterStyle.NUMERIC, "೦೧೨೩೪೫೬೭೮೯", ". ");
        add("khmer", CounterStyle.NUMERIC, "០១២៣៤៥៦៧៨៩", ". ");
        add("lao", CounterStyle.NUMERIC, "໐໑໒໓໔໕໖໗໘໙", ". ");
        add("malayalam", CounterStyle.NUMERIC, "൦൧൨൩൪൫൬൭൮൯", ". ");
        add("myanmar", CounterStyle.NUMERIC, "၀၁၂၃၄၅၆၇၈၉", ". ");
        add("oriya", CounterStyle.NUMERIC, "୦୧୨୩୪୫୬୭୮୯", ". ");
        add("tamil", CounterStyle.NUMERIC, "௦௧௨௩௪௫௬௭௮௯", ". ");
        add("telugu", CounterStyle.NUMERIC, "౦౧౨౩౪౫౬౭౮౯", ". ");
        add("thai", CounterStyle.NUMERIC, "๐๑๒๓๔๕๖๗๘๙", ". ");
        add("tibetan", CounterStyle.NUMERIC, "༠༡༢༣༤༥༦༧༨༩", ". ");
        add("hiragana", CounterStyle.ALPHABETIC, "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわゐゑをん", "、");
        add("katakana", CounterStyle.ALPHABETIC, "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヰヱヲン", "、");
        add("hiragana-iroha", CounterStyle.ALPHABETIC, "いろはにほへとちりぬるをわかよたれそつねならむうゐのおくやまけふこえてあさきゆめみしゑひもせす", "、");
        add("katakana-iroha", CounterStyle.ALPHABETIC, "イロハニホヘトチリヌルヲワカヨタレソツネナラムウヰノオクヤマケフコエテアサキユメミシヱヒモセス", "、");
        addKorean("korean-hangul-formal", "영일이삼사오육칠팔구", "십백천", "마이너스 ", false);
        addKorean("korean-hanja-informal", "零一二三四五六七八九", "十百千", "負 ", true);
    }

    /**
     * Creates the set of counter styles of a document.
     * <p>
     * 문서의 카운터 스타일 집합을 만든다.
     *
     * @param rules the definitions of the style sheets by name, or null / 스타일 시트의 이름별 정의. 없으면 null
     */
    CounterStyles(Map<String, CounterStyleRule> rules) {
        this.rules = rules;
    }

    /**
     * Makes the counter styles of a style source: those of the style sheets if it knows them, the predefined ones otherwise.
     * <p>
     * 스타일 공급자의 카운터 스타일을 만든다: 공급자가 알면 스타일 시트의 것, 아니면 미리 정의된 것.
     *
     * @param source the style source / 스타일 공급자
     * @return the counter styles / 카운터 스타일
     */
    static CounterStyles of(StyleSource source) {
        if (source instanceof CounterStyleSource) {
            Map<String, CounterStyleRule> r = ((CounterStyleSource) source).getCounterStyleRules();
            if (r != null && !r.isEmpty()) {
                return new CounterStyles(r);
            }
        }
        return DEFAULT;
    }

    /**
     * Returns the text of the marker of a list item for a <code>list-style-type</code> value, with the prefix and the suffix of the style
     * (<code>"3. "</code>, <code>"• "</code>): a string is the marker itself.
     * <p>
     * <code>list-style-type</code> 값에 대한 목록 항목의 표식 텍스트를 스타일의 접두사와 접미사와 함께 반환한다(<code>"3. "</code>, <code>"• "</code>). 문자열은 그
     * 자체가 표식이다.
     *
     * @param type    the computed value of <code>list-style-type</code> / <code>list-style-type</code> 의 계산 값
     * @param ordinal the item number / 항목 번호
     * @return the text, or null for <code>none</code> / 텍스트. <code>none</code> 이면 null
     */
    String marker(CssValue type, int ordinal) {
        if (type instanceof CssString) {
            return ((CssString) type).getValue();
        }
        CounterStyle s = styleOf(type);
        if (s == null) {
            return null;
        }
        return s.prefix + write(s, ordinal, 0) + s.suffix;
    }

    /**
     * Returns the number of a counter in a style (<code>counter()</code>, <code>counters()</code>), without the prefix and the suffix.
     * <p>
     * 카운터의 번호를 스타일로 반환한다(<code>counter()</code>, <code>counters()</code>). 접두사와 접미사는 없다.
     *
     * @param style the computed value of the style argument, or null for <code>decimal</code> / 스타일 인수의 계산 값. <code>decimal</code> 이면 null
     * @param value the counter value / 카운터 값
     * @return the text, empty for <code>none</code> / 텍스트. <code>none</code> 이면 빈 문자열
     */
    String number(CssValue style, long value) {
        CounterStyle s = style == null ? PREDEFINED.get("decimal") : styleOf(style);
        return s == null ? "" : write(s, value, 0);
    }

    /**
     * Resolves a style value: a name, or an anonymous style of <code>symbols()</code>.
     * <p>
     * 스타일 값을 푼다: 이름 또는 <code>symbols()</code> 의 이름 없는 스타일.
     *
     * @param v the value / 값
     * @return the style, null for <code>none</code> / 스타일. <code>none</code> 이면 null
     */
    private CounterStyle styleOf(CssValue v) {
        if (v instanceof CssFunctionValue && ((CssFunctionValue) v).isNamed("symbols")) {
            CounterStyle s = symbols((CssFunctionValue) v);
            return s == null ? PREDEFINED.get("decimal") : s;
        }
        if (v instanceof CssKeyword) {
            String name = ((CssKeyword) v).getName();
            if ("none".equals(name)) {
                return null;
            }
            return lookup(name);
        }
        return PREDEFINED.get("decimal");
    }

    /**
     * Writes a value in a style, going through the fallback styles if the style cannot write it.
     * <p>
     * 값을 스타일로 쓰며, 스타일이 쓸 수 없으면 대체 스타일을 거친다.
     *
     * @param s     the style / 스타일
     * @param value the value / 값
     * @param depth the number of fallbacks taken so far / 지금까지 거친 대체의 수
     * @return the representation / 표현
     */
    private String write(CounterStyle s, long value, int depth) {
        String r = s.represent(value);
        if (r != null) {
            return r;
        }
        CounterStyle f = depth >= 16 || s.fallback == null ? PREDEFINED.get("decimal") : lookup(s.fallback);
        if (f == s) {
            f = PREDEFINED.get("decimal");
        }
        return write(f, value, depth + 1);
    }

    /**
     * Finds a style by name: the definitions of the style sheets first (a name that an author defines is case sensitive), then the predefined ones
     * (case-insensitive); an unknown name is <code>decimal</code>.
     * <p>
     * 이름으로 스타일을 찾는다: 스타일 시트의 정의가 먼저(저자가 정한 이름은 대소문자를 구분한다), 그 다음 미리 정의된 것(대소문자 무시). 모르는 이름은
     * <code>decimal</code> 이다.
     *
     * @param name the name / 이름
     * @return the style / 스타일
     */
    CounterStyle lookup(String name) {
        if (rules != null && rules.containsKey(name)) {
            if (!custom.containsKey(name)) {
                custom.put(name, null);
                custom.put(name, build(rules.get(name), 0));
            }
            CounterStyle c = custom.get(name);
            if (c != null) {
                return c;
            }
        }
        CounterStyle p = PREDEFINED.get(name.toLowerCase());
        return p == null ? PREDEFINED.get("decimal") : p;
    }

    /**
     * Builds a style from a <code>@counter-style</code> rule, or returns null if the descriptors are not valid for the system: the cyclic, fixed and
     * symbolic systems need one symbol, the alphabetic and numeric systems two, the additive system needs <code>additive-symbols</code> with decreasing
     * weights, and <code>extends</code> must name a style that exists and is not part of a cycle of extensions (a style that extends another one takes
     * its system and the descriptors it does not write itself).
     * <p>
     * <code>@counter-style</code> 규칙에서 스타일을 만든다. 서술자가 시스템에 맞지 않으면 null: cyclic, fixed, symbolic 은 기호 하나, alphabetic 과 numeric 은 둘이
     * 필요하고, additive 는 가중치가 감소하는 <code>additive-symbols</code> 가 필요하며, <code>extends</code> 는 존재하고 확장의 순환에 들지 않는 스타일을 가리켜야
     * 한다(다른 스타일을 확장한 스타일은 그 시스템과 자신이 쓰지 않은 서술자를 가져온다).
     *
     * @param rule  the rule / 규칙
     * @param depth the depth of extension / 확장의 깊이
     * @return the style or null / 스타일. 올바르지 않으면 null
     */
    private CounterStyle build(CounterStyleRule rule, int depth) {
        Map<String, List<ComponentValue>> d = rule.getDescriptors();
        List<ComponentValue> sys = items(d.get("system"));
        CounterStyle s;
        if (!sys.isEmpty() && word(sys.get(0)).equals("extends")) {
            if (sys.size() != 2 || depth > 8) {
                return null;
            }
            String base = word(sys.get(1));
            CounterStyle b;
            if (rules != null && rules.containsKey(base) && !base.equals(rule.getName())) {
                b = build(rules.get(base), depth + 1);
            } else {
                b = PREDEFINED.get(base.toLowerCase());
            }
            if (b == null) {
                return null;
            }
            s = copy(b);
        } else {
            s = new CounterStyle();
            s.system = CounterStyle.SYMBOLIC;
            if (!sys.isEmpty()) {
                String w = word(sys.get(0));
                if (w.equals("cyclic")) {
                    s.system = CounterStyle.CYCLIC;
                } else if (w.equals("numeric")) {
                    s.system = CounterStyle.NUMERIC;
                } else if (w.equals("alphabetic")) {
                    s.system = CounterStyle.ALPHABETIC;
                } else if (w.equals("symbolic")) {
                    s.system = CounterStyle.SYMBOLIC;
                } else if (w.equals("additive")) {
                    s.system = CounterStyle.ADDITIVE;
                } else if (w.equals("fixed")) {
                    s.system = CounterStyle.FIXED;
                    if (sys.size() == 2 && sys.get(1) instanceof CssToken && ((CssToken) sys.get(1)).is(CssToken.NUMBER)) {
                        s.first = (int) ((CssToken) sys.get(1)).getNumber();
                    } else if (sys.size() != 1) {
                        return null;
                    }
                } else {
                    return null;
                }
            }
            s.fallback = null;
        }
        if (d.containsKey("symbols")) {
            List<String> sym = symbolList(d.get("symbols"));
            if (sym == null) {
                return null;
            }
            s.symbols = sym.toArray(new String[sym.size()]);
        }
        if (d.containsKey("additive-symbols")) {
            if (!additiveSymbols(d.get("additive-symbols"), s)) {
                return null;
            }
        }
        boolean extending = !sys.isEmpty() && word(sys.get(0)).equals("extends");
        if (!extending && !valid(s)) {
            return null;
        }
        negative(d, s);
        if (d.containsKey("prefix")) {
            List<String> p = symbolList(d.get("prefix"));
            s.prefix = p == null || p.size() != 1 ? s.prefix : p.get(0);
        }
        if (d.containsKey("suffix")) {
            List<String> p = symbolList(d.get("suffix"));
            s.suffix = p == null || p.size() != 1 ? s.suffix : p.get(0);
        }
        range(d, s, extending);
        pad(d, s);
        if (d.containsKey("fallback")) {
            List<ComponentValue> f = items(d.get("fallback"));
            s.fallback = f.size() == 1 ? word(f.get(0)) : s.fallback;
        }
        return s;
    }

    /**
     * Tells whether a style has what its system needs.
     * <p>
     * 스타일이 시스템이 필요로 하는 것을 가졌는지 알려 준다.
     *
     * @param s the style / 스타일
     * @return true if it is complete / 완전하면 true
     */
    private static boolean valid(CounterStyle s) {
        switch (s.system) {
        case CounterStyle.ADDITIVE:
            return s.additive.length > 0;
        case CounterStyle.ALPHABETIC:
        case CounterStyle.NUMERIC:
            return s.symbols.length >= 2;
        default:
            return s.symbols.length >= 1;
        }
    }

    /**
     * Reads <code>negative</code>: one or two symbols, the prefix and the suffix of a negative value.
     * <p>
     * <code>negative</code> 를 읽는다: 기호 하나 또는 둘, 음수의 접두사와 접미사.
     *
     * @param d the descriptors / 서술자
     * @param s the style / 스타일
     */
    private static void negative(Map<String, List<ComponentValue>> d, CounterStyle s) {
        if (!d.containsKey("negative")) {
            return;
        }
        List<String> n = symbolList(d.get("negative"));
        if (n != null && (n.size() == 1 || n.size() == 2)) {
            s.negativePrefix = n.get(0);
            s.negativeSuffix = n.size() == 2 ? n.get(1) : "";
        }
    }

    /**
     * Reads <code>range</code>: <code>auto</code> or the pairs of bounds (the style writes the values in the union; <code>infinite</code> is no bound);
     * without it, the range follows the system.
     * <p>
     * <code>range</code> 를 읽는다: <code>auto</code> 또는 경계 쌍(스타일은 합집합의 값을 쓴다. <code>infinite</code> 는 경계 없음). 없으면 범위는 시스템을 따른다.
     *
     * @param d         the descriptors / 서술자
     * @param s         the style / 스타일
     * @param extending true if the style extends another one, which then keeps the range of that one / 다른 스타일을 확장하면 true. 확장한 스타일의 범위를 유지한다
     */
    private static void range(Map<String, List<ComponentValue>> d, CounterStyle s, boolean extending) {
        List<ComponentValue> r = items(d.get("range"));
        if (r.isEmpty() || word(r.get(0)).equals("auto")) {
            if (!extending || d.containsKey("range")) {
                s.lower = Long.MIN_VALUE;
                s.upper = Long.MAX_VALUE;
                if (s.system == CounterStyle.ALPHABETIC || s.system == CounterStyle.SYMBOLIC) {
                    s.lower = 1;
                } else if (s.system == CounterStyle.ADDITIVE) {
                    s.lower = 0;
                }
            }
            return;
        }
        long lo = Long.MAX_VALUE;
        long hi = Long.MIN_VALUE;
        boolean open = false;
        // 쉼표로 나뉜 경계 쌍을 읽어 값을 쓸 수 있는 구간을 합친다(구간은 하나의 범위로 근사한다).
        List<List<ComponentValue>> pairs = CssParser.splitCommas(d.get("range"));
        for (int i = 0; i < pairs.size(); i++) {
            List<ComponentValue> p = items(pairs.get(i));
            if (p.size() != 2) {
                return;
            }
            long a = bound(p.get(0), Long.MIN_VALUE);
            long b = bound(p.get(1), Long.MAX_VALUE);
            if (a > b) {
                return;
            }
            lo = Math.min(lo, a);
            hi = Math.max(hi, b);
            open = true;
        }
        if (open) {
            s.lower = lo;
            s.upper = hi;
        }
    }

    /**
     * Reads one bound of <code>range</code>.
     * <p>
     * <code>range</code> 의 경계 하나를 읽는다.
     *
     * @param v        the value / 값
     * @param infinite the value of <code>infinite</code> / <code>infinite</code> 의 값
     * @return the bound / 경계
     */
    private static long bound(ComponentValue v, long infinite) {
        if (v instanceof CssToken && ((CssToken) v).is(CssToken.NUMBER)) {
            return (long) ((CssToken) v).getNumber();
        }
        return infinite;
    }

    /**
     * Reads <code>pad</code>: a length in symbols and the padding symbol.
     * <p>
     * <code>pad</code> 를 읽는다: 기호 수로 센 길이와 채움 기호.
     *
     * @param d the descriptors / 서술자
     * @param s the style / 스타일
     */
    private static void pad(Map<String, List<ComponentValue>> d, CounterStyle s) {
        if (!d.containsKey("pad")) {
            return;
        }
        List<ComponentValue> p = items(d.get("pad"));
        int len = -1;
        String sym = null;
        // 정수와 기호를 순서에 상관없이 하나씩 찾는다.
        for (int i = 0; i < p.size(); i++) {
            ComponentValue c = p.get(i);
            if (c instanceof CssToken && ((CssToken) c).is(CssToken.NUMBER)) {
                len = (int) Math.max(0, Math.min(64, ((CssToken) c).getNumber()));
            } else if (c instanceof CssToken && ((CssToken) c).is(CssToken.STRING)) {
                sym = ((CssToken) c).getValue();
            } else if (c instanceof CssToken && ((CssToken) c).is(CssToken.IDENT)) {
                sym = ((CssToken) c).getValue();
            }
        }
        if (len >= 0 && sym != null) {
            s.padLength = len;
            s.padSymbol = sym;
        }
    }

    /**
     * Reads <code>additive-symbols</code>: weight and symbol pairs separated by commas, with the weights in decreasing order.
     * <p>
     * <code>additive-symbols</code> 를 읽는다: 쉼표로 구분한 가중치와 기호 쌍. 가중치는 내림차순이다.
     *
     * @param v the value / 값
     * @param s the style that receives the tuples / 쌍을 받을 스타일
     * @return true if the value is valid / 올바르면 true
     */
    private static boolean additiveSymbols(List<ComponentValue> v, CounterStyle s) {
        List<List<ComponentValue>> parts = CssParser.splitCommas(v);
        int[] w = new int[parts.size()];
        String[] t = new String[parts.size()];
        // 쌍마다 가중치(정수)와 기호를 읽는다.
        for (int i = 0; i < parts.size(); i++) {
            List<ComponentValue> p = items(parts.get(i));
            if (p.size() != 2) {
                return false;
            }
            ComponentValue num = p.get(0) instanceof CssToken && ((CssToken) p.get(0)).is(CssToken.NUMBER) ? p.get(0) : p.get(1);
            ComponentValue sym = num == p.get(0) ? p.get(1) : p.get(0);
            if (!(num instanceof CssToken) || !((CssToken) num).is(CssToken.NUMBER) || !((CssToken) num).isInteger() || !(sym instanceof CssToken)) {
                return false;
            }
            w[i] = (int) ((CssToken) num).getNumber();
            t[i] = ((CssToken) sym).getValue();
            if (w[i] < 0 || i > 0 && w[i] >= w[i - 1]) {
                return false;
            }
        }
        s.weights = w;
        s.additive = t;
        return true;
    }

    /**
     * Reads a list of symbols: strings and identifiers.
     * <p>
     * 기호 목록을 읽는다: 문자열과 식별자.
     *
     * @param v the value / 값
     * @return the symbols, or null if something else is in the list / 기호. 다른 것이 있으면 null
     */
    private static List<String> symbolList(List<ComponentValue> v) {
        List<ComponentValue> it = items(v);
        List<String> out = new ArrayList<String>();
        // 항목을 하나씩 기호로 바꾼다(이미지 기호는 지원하지 않으므로 무효로 본다).
        for (int i = 0; i < it.size(); i++) {
            ComponentValue c = it.get(i);
            if (c instanceof CssToken && (((CssToken) c).is(CssToken.STRING) || ((CssToken) c).is(CssToken.IDENT))) {
                out.add(((CssToken) c).getValue());
            } else {
                return null;
            }
        }
        return out;
    }

    /**
     * Makes the anonymous style of <code>symbols([type] symbols)</code>: the type is <code>symbolic</code> if it is not written.
     * <p>
     * <code>symbols([type] 기호들)</code> 의 이름 없는 스타일을 만든다. 종류를 쓰지 않으면 <code>symbolic</code> 이다.
     *
     * @param f the function / 함수
     * @return the style, or null if the arguments are invalid / 스타일. 인수가 올바르지 않으면 null
     */
    private static CounterStyle symbols(CssFunctionValue f) {
        List<CssValue> a = new ArrayList<CssValue>();
        // 공백으로 구분한 인수는 하나의 목록으로 오므로 펼쳐서 인수 하나씩으로 만든다.
        for (int i = 0; i < f.getArguments().size(); i++) {
            CssValue v = f.getArguments().get(i);
            if (v instanceof CssList && !((CssList) v).isComma()) {
                a.addAll(((CssList) v).getItems());
            } else {
                a.add(v);
            }
        }
        CounterStyle s = new CounterStyle();
        s.system = CounterStyle.SYMBOLIC;
        s.suffix = " ";
        int from = 0;
        // 첫 인수가 종류 키워드이면 시스템을 정한다.
        if (!a.isEmpty() && a.get(0) instanceof CssKeyword) {
            String t = ((CssKeyword) a.get(0)).getName();
            if (t.equals("cyclic") || t.equals("numeric") || t.equals("alphabetic") || t.equals("symbolic") || t.equals("fixed")) {
                s.system = t.equals("cyclic") ? CounterStyle.CYCLIC : t.equals("numeric") ? CounterStyle.NUMERIC
                        : t.equals("alphabetic") ? CounterStyle.ALPHABETIC : t.equals("fixed") ? CounterStyle.FIXED : CounterStyle.SYMBOLIC;
                from = 1;
            }
        }
        List<String> sym = new ArrayList<String>();
        // 나머지 인수는 기호(문자열)이다.
        for (int i = from; i < a.size(); i++) {
            if (a.get(i) instanceof CssString) {
                sym.add(((CssString) a.get(i)).getValue());
            } else {
                return null;
            }
        }
        s.symbols = sym.toArray(new String[sym.size()]);
        if (s.system == CounterStyle.ALPHABETIC || s.system == CounterStyle.SYMBOLIC) {
            s.lower = 1;
        }
        return valid(s) ? s : null;
    }

    /**
     * Returns the non-whitespace values, or an empty list for null.
     * <p>
     * 공백이 아닌 값을 반환한다. null 이면 빈 목록.
     *
     * @param v the values or null / 값. null 일 수 있다
     * @return the items / 항목
     */
    private static List<ComponentValue> items(List<ComponentValue> v) {
        List<ComponentValue> out = new ArrayList<ComponentValue>();
        // 공백이 아닌 항목만 모은다.
        for (int i = 0; v != null && i < v.size(); i++) {
            if (!CssParser.isWhitespace(v.get(i))) {
                out.add(v.get(i));
            }
        }
        return out;
    }

    /**
     * Returns the lower-case text of an identifier, or the empty string.
     * <p>
     * 식별자의 소문자 텍스트를 반환한다. 아니면 빈 문자열.
     *
     * @param v the value / 값
     * @return the word / 단어
     */
    private static String word(ComponentValue v) {
        if (v instanceof CssToken && ((CssToken) v).is(CssToken.IDENT)) {
            return ((CssToken) v).getValue().toLowerCase();
        }
        if (v instanceof CssFunction || v instanceof CssBlock) {
            return "";
        }
        return "";
    }

    /**
     * Adds a predefined style of a symbol list.
     * <p>
     * 기호 목록으로 된 미리 정의된 스타일을 더한다.
     *
     * @param name   the name / 이름
     * @param system the system / 시스템
     * @param chars  the symbols, one for each character / 기호. 글자마다 하나
     * @param suffix the suffix of the marker / 표식의 접미사
     */
    private static void add(String name, int system, String chars, String suffix) {
        CounterStyle s = new CounterStyle();
        s.system = system;
        s.symbols = new String[chars.length()];
        // 글자마다 기호 하나로 만든다(모든 기호는 BMP 안의 글자이다).
        for (int i = 0; i < chars.length(); i++) {
            s.symbols[i] = String.valueOf(chars.charAt(i));
        }
        s.suffix = suffix;
        if (system == CounterStyle.ALPHABETIC || system == CounterStyle.SYMBOLIC) {
            s.lower = 1;
        }
        PREDEFINED.put(name, s);
    }

    /**
     * Adds a predefined roman numeral style: additive, for 1 to 3999.
     * <p>
     * 미리 정의된 로마 숫자 스타일을 더한다: additive, 1 에서 3999.
     *
     * @param name    the name / 이름
     * @param symbols the symbols for 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 / 1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1 의 기호
     */
    private static void addRoman(String name, String[] symbols) {
        CounterStyle s = new CounterStyle();
        s.system = CounterStyle.ADDITIVE;
        s.weights = new int[] {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        s.additive = symbols;
        s.lower = 1;
        s.upper = 3999;
        PREDEFINED.put(name, s);
    }

    /**
     * Adds a predefined Korean numeral style: an additive style that writes a number below 10000 as digit and unit words (<code>이십삼</code>,
     * <code>一千二百</code>).
     * <p>
     * 미리 정의된 한국 숫자 스타일을 더한다: 10000 미만의 수를 숫자 단어와 단위 단어로 쓰는 additive 스타일(<code>이십삼</code>, <code>一千二百</code>).
     *
     * @param name     the name / 이름
     * @param digits   the words of 0 to 9 / 0 에서 9 의 단어
     * @param units    the words of 10, 100, 1000 / 10, 100, 1000 의 단어
     * @param negative the prefix of a negative number / 음수의 접두사
     * @param omitOne  true to write 10, 100 and 1000 as the unit word alone (the informal style) / 10, 100, 1000 을 단위 단어만으로 쓰면 true(비격식 스타일)
     */
    private static void addKorean(String name, String digits, String units, String negative, boolean omitOne) {
        CounterStyle s = new CounterStyle();
        s.system = CounterStyle.ADDITIVE;
        int n = 9 * 3 + 9 + 1;
        int[] w = new int[n];
        String[] t = new String[n];
        int k = 0;
        // 천, 백, 십의 자리마다 9 부터 1 까지 숫자 단어와 단위 단어를 이어 쓴다.
        for (int place = 2; place >= 0; place--) {
            int unit = place == 0 ? 10 : place == 1 ? 100 : 1000;
            // 자리의 숫자 9 에서 1 까지 숫자 단어와 단위 단어를 만든다.
            for (int d = 9; d >= 1; d--) {
                w[k] = d * unit;
                t[k] = omitOne && d == 1 ? String.valueOf(units.charAt(place)) : String.valueOf(digits.charAt(d)) + units.charAt(place);
                k++;
            }
        }
        // 일의 자리와 0 을 더한다.
        for (int d = 9; d >= 1; d--) {
            w[k] = d;
            t[k] = String.valueOf(digits.charAt(d));
            k++;
        }
        w[k] = 0;
        t[k] = String.valueOf(digits.charAt(0));
        s.weights = w;
        s.additive = t;
        s.lower = -9999;
        s.upper = 9999;
        s.negativePrefix = negative;
        s.suffix = ", ";
        s.fallback = "cjk-decimal";
        PREDEFINED.put(name, s);
    }

    /**
     * Copies a style.
     * <p>
     * 스타일을 복사한다.
     *
     * @param b the style / 스타일
     * @return the copy / 복사본
     */
    private static CounterStyle copy(CounterStyle b) {
        CounterStyle s = new CounterStyle();
        s.system = b.system;
        s.symbols = b.symbols;
        s.weights = b.weights;
        s.additive = b.additive;
        s.first = b.first;
        s.negativePrefix = b.negativePrefix;
        s.negativeSuffix = b.negativeSuffix;
        s.prefix = b.prefix;
        s.suffix = b.suffix;
        s.lower = b.lower;
        s.upper = b.upper;
        s.padLength = b.padLength;
        s.padSymbol = b.padSymbol;
        s.fallback = b.fallback;
        return s;
    }
}
