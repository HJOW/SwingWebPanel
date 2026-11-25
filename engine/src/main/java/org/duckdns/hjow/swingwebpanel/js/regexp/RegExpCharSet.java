package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.text.CharClass;

/**
 * A set of characters (code units, or code points with the u flag) stored as sorted, merged, inclusive ranges,
 * optionally negated. A set is mutable while the pattern is parsed and must be {@link #normalize()}d before
 * {@link #contains(int)} is used; afterwards it is only read, so it can be shared by threads.
 * <p>
 * 정렬되고 병합된 닫힌 구간으로 저장하는 문자 집합(코드 단위, u 플래그면 코드 포인트). 부정(negation) 여부를 가질 수 있다.
 * 패턴을 해석하는 동안은 변경할 수 있고 {@link #contains(int)} 를 쓰기 전에 {@link #normalize()} 해야 한다.
 * 그 뒤에는 읽기만 하므로 여러 스레드가 공유할 수 있다.
 */
final class RegExpCharSet {

    /**
     * Cached Unicode property sets, shared only after normalization.
     * <p>
     * 정규화한 뒤 공유하는 유니코드 속성 집합 캐시.
     */
    private static final Map<String, RegExpCharSet> UNICODE_PROPERTIES = new HashMap<String, RegExpCharSet>();

    /**
     * Range storage: lo0, hi0, lo1, hi1 ...
     * <p>
     * 구간 저장소: lo0, hi0, lo1, hi1 ...
     */
    private int[] ranges = new int[8];

    /**
     * Number of ints used in {@link #ranges} (twice the range count).
     * <p>
     * {@link #ranges} 에서 쓰는 int 개수(구간 수의 두 배).
     */
    private int used;

    /**
     * True if the set matches characters that are NOT in the ranges (the class was written as [^...]).
     * <p>
     * 구간에 없는 문자와 일치하면 true([^...] 로 쓴 클래스).
     */
    boolean negated;

    /**
     * Creates an empty set.
     * <p>
     * 빈 집합을 만든다.
     */
    RegExpCharSet() {
    }

    /**
     * Adds a single character.
     * <p>
     * 문자 하나를 추가한다.
     *
     * @param c character / 문자
     */
    void addChar(int c) {
        addRange(c, c);
    }

    /**
     * Adds an inclusive range.
     * <p>
     * 닫힌 구간을 추가한다.
     *
     * @param lo first character / 첫 문자
     * @param hi last character / 마지막 문자
     */
    void addRange(int lo, int hi) {
        if (used + 2 > ranges.length) {
            ranges = Arrays.copyOf(ranges, ranges.length * 2);
        }
        ranges[used++] = lo;
        ranges[used++] = hi;
    }

    /**
     * Adds all ranges of another set (its negation flag is ignored).
     * <p>
     * 다른 집합의 모든 구간을 추가한다(그 집합의 부정 여부는 무시).
     *
     * @param other normalized or raw set / 집합
     */
    void addAll(RegExpCharSet other) {
        // 다른 집합의 구간을 하나씩 복사한다.
        for (int i = 0; i < other.used; i += 2) {
            addRange(other.ranges[i], other.ranges[i + 1]);
        }
    }

    /**
     * Sorts and merges the ranges. Must be called once building is finished.
     * <p>
     * 구간을 정렬하고 병합한다. 구성이 끝나면 한 번 호출해야 한다.
     */
    void normalize() {
        int count = used / 2;
        if (count <= 1) {
            return;
        }
        long[] pairs = new long[count];
        // 구간을 (시작, 끝) 하나의 long 으로 묶어 시작 순으로 정렬한다.
        for (int i = 0; i < count; i++) {
            pairs[i] = (((long) ranges[2 * i]) << 32) | (ranges[2 * i + 1] & 0xFFFFFFFFL);
        }
        Arrays.sort(pairs);
        int w = 0;
        int curLo = (int) (pairs[0] >> 32);
        int curHi = (int) pairs[0];
        // 겹치거나 맞닿은 구간을 하나로 합친다.
        for (int i = 1; i < count; i++) {
            int lo = (int) (pairs[i] >> 32);
            int hi = (int) pairs[i];
            if (lo <= curHi + 1) {
                if (hi > curHi) {
                    curHi = hi;
                }
            } else {
                ranges[w++] = curLo;
                ranges[w++] = curHi;
                curLo = lo;
                curHi = hi;
            }
        }
        ranges[w++] = curLo;
        ranges[w++] = curHi;
        used = w;
    }

    /**
     * Tests membership in the ranges (the negation flag is not applied). The set must be normalized.
     * <p>
     * 구간에 속하는지 검사한다(부정 여부는 적용하지 않음). 집합은 정규화되어 있어야 한다.
     *
     * @param c character / 문자
     * @return true if inside one of the ranges / 구간 중 하나에 속하면 true
     */
    boolean contains(int c) {
        int lo = 0;
        int hi = used / 2 - 1;
        // 구간 배열에서 이분 탐색한다.
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (c < ranges[2 * mid]) {
                hi = mid - 1;
            } else if (c > ranges[2 * mid + 1]) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a new normalized, non-negated set holding every character in 0..max that is not in this set.
     * <p>
     * 0..max 범위에서 이 집합에 없는 모든 문자를 담은, 정규화되고 부정되지 않은 새 집합을 반환한다.
     *
     * @param max largest character to include / 포함할 최대 문자
     * @return complement set / 여집합
     */
    RegExpCharSet complement(int max) {
        normalize();
        RegExpCharSet out = new RegExpCharSet();
        int next = 0;
        // 정규화된 구간 사이의 빈 곳을 새 구간으로 만든다.
        for (int i = 0; i < used; i += 2) {
            if (ranges[i] > next) {
                out.addRange(next, Math.min(ranges[i] - 1, max));
            }
            next = ranges[i + 1] + 1;
            if (next > max) {
                break;
            }
        }
        if (next <= max) {
            out.addRange(next, max);
        }
        out.normalize();
        return out;
    }

    /**
     * Builds the set for \d.
     * <p>
     * \d 용 집합을 만든다.
     *
     * @return digits 0-9 / 숫자 0-9
     */
    static RegExpCharSet digits() {
        RegExpCharSet s = new RegExpCharSet();
        s.addRange('0', '9');
        s.normalize();
        return s;
    }

    /**
     * Builds the set for \w (ASCII word characters).
     * <p>
     * \w 용 집합을 만든다(ASCII 단어 문자).
     *
     * @return letters, digits and underscore / 글자, 숫자, 밑줄
     */
    static RegExpCharSet word() {
        RegExpCharSet s = new RegExpCharSet();
        s.addRange('0', '9');
        s.addRange('A', 'Z');
        s.addChar('_');
        s.addRange('a', 'z');
        s.normalize();
        return s;
    }

    /**
     * Builds the set for \s (WhiteSpace and LineTerminator of ECMAScript).
     * <p>
     * \s 용 집합을 만든다(ECMAScript 의 WhiteSpace 와 LineTerminator).
     *
     * @return space characters / 공백 문자
     */
    static RegExpCharSet space() {
        RegExpCharSet s = new RegExpCharSet();
        int start = -1;
        // 0..0xFFFF 의 문자를 훑으며 공백 문자의 연속 구간을 만든다.
        for (int c = 0; c <= 0x10000; c++) {
            boolean in = c < 0x10000 && CharClass.isWhiteSpaceOrLineTerminator(c);
            if (in && start < 0) {
                start = c;
            } else if (!in && start >= 0) {
                s.addRange(start, c - 1);
                start = -1;
            }
        }
        s.normalize();
        return s;
    }

    /**
     * Returns the Unicode property escape set for a name accepted by ECMAScript 2022.
     * The table is generated lazily from Java's public Unicode database; script values use explicit ranges.
     * <p>
     * ECMAScript 2022 가 받는 이름의 유니코드 속성 escape 집합을 돌려준다. Java 공개 유니코드 데이터베이스에서
     * 느리게 표를 만들고 Script 값은 명시적 범위를 쓴다.
     *
     * @param propertyName canonical or aliased property name / 표준 또는 별칭 속성 이름
     * @return set, or null for an unknown property / 집합. 모르면 null
     */
    static synchronized RegExpCharSet unicodeProperty(String propertyName) {
        String key = propertyName.replace('_', ' ').trim();
        String compact = key.replace(" ", "").toLowerCase();
        RegExpCharSet cached = UNICODE_PROPERTIES.get(compact);
        if (cached != null) {
            return cached;
        }
        final String category = categoryName(compact);
        String generatedKey;
        if ("Script=".length() <= compact.length() && (compact.startsWith("script=") || compact.startsWith("sc="))) {
            String script = compact.substring(compact.indexOf('=') + 1);
            generatedKey = "sc=" + script;
        } else if (compact.startsWith("scriptextensions=") || compact.startsWith("scx=")) {
            generatedKey = "scx=" + compact.substring(compact.indexOf('=') + 1);
        } else if (category != null) {
            generatedKey = "gc=" + category.toLowerCase();
        } else {
            generatedKey = compact;
        }
        int[] generated = UnicodePropertyData.ranges(generatedKey);
        RegExpCharSet result;
        if (generated != null) {
            result = fromRanges(generated);
        } else if ("Script=".length() <= compact.length() && (compact.startsWith("script=") || compact.startsWith("sc="))) {
            result = scriptSet(compact.substring(compact.indexOf('=') + 1));
        } else if (category != null) {
            result = buildCategory(category);
        } else {
            result = buildBinary(compact);
        }
        if (result != null) {
            result.normalize();
            UNICODE_PROPERTIES.put(compact, result);
        }
        return result;
    }

    /**
     * Builds a normalized set from generated inclusive ranges.
     * <p>
     * 생성된 닫힌 구간에서 정규화 집합을 만든다.
     *
     * @param ranges lo, hi pairs / lo, hi 쌍
     * @return set / 집합
     */
    private static RegExpCharSet fromRanges(int[] ranges) {
        RegExpCharSet set = new RegExpCharSet();
        // 생성 표의 닫힌 lo/hi 쌍을 집합 구간으로 차례로 복사한다.
        for (int i = 0; i < ranges.length; i += 2) {
            set.addRange(ranges[i], ranges[i + 1]);
        }
        set.normalize();
        return set;
    }

    /**
     * Resolves a General_Category property alias.
     * <p>
     * General_Category 속성 별칭을 해석한다.
     *
     * @param text normalized property / 정규화한 속성
     * @return short category code or null / 짧은 범주 코드 또는 null
     */
    private static String categoryName(String text) {
        String value = text;
        if (value.startsWith("generalcategory=") || value.startsWith("gc=")) {
            value = value.substring(value.indexOf('=') + 1);
        } else if (value.length() > 2 && value.indexOf('=') >= 0) {
            return null;
        }
        if ("lu".equals(value) || "uppercaseletter".equals(value)) return "Lu";
        if ("ll".equals(value) || "lowercaseletter".equals(value)) return "Ll";
        if ("lt".equals(value) || "titlecaseletter".equals(value)) return "Lt";
        if ("lm".equals(value) || "modifierletter".equals(value)) return "Lm";
        if ("lo".equals(value) || "otherletter".equals(value)) return "Lo";
        if ("mn".equals(value) || "nonspacingmark".equals(value)) return "Mn";
        if ("mc".equals(value) || "spacingmark".equals(value) || "spacingcombiningmark".equals(value)) return "Mc";
        if ("me".equals(value) || "enclosingmark".equals(value)) return "Me";
        if ("nd".equals(value) || "decimalnumber".equals(value)) return "Nd";
        if ("nl".equals(value) || "letternumber".equals(value)) return "Nl";
        if ("no".equals(value) || "othernumber".equals(value)) return "No";
        if ("pc".equals(value) || "connectorpunctuation".equals(value)) return "Pc";
        if ("pd".equals(value) || "dashpunctuation".equals(value)) return "Pd";
        if ("ps".equals(value) || "openpunctuation".equals(value)) return "Ps";
        if ("pe".equals(value) || "closepunctuation".equals(value)) return "Pe";
        if ("pi".equals(value) || "initialpunctuation".equals(value)) return "Pi";
        if ("pf".equals(value) || "finalpunctuation".equals(value)) return "Pf";
        if ("po".equals(value) || "otherpunctuation".equals(value)) return "Po";
        if ("sm".equals(value) || "mathsymbol".equals(value)) return "Sm";
        if ("sc".equals(value) || "currencysymbol".equals(value)) return "Sc";
        if ("sk".equals(value) || "modifiersymbol".equals(value)) return "Sk";
        if ("so".equals(value) || "othersymbol".equals(value)) return "So";
        if ("zs".equals(value) || "spaceseparator".equals(value)) return "Zs";
        if ("zl".equals(value) || "lineseparator".equals(value)) return "Zl";
        if ("zp".equals(value) || "paragraphseparator".equals(value)) return "Zp";
        if ("cc".equals(value) || "control".equals(value)) return "Cc";
        if ("cf".equals(value) || "format".equals(value)) return "Cf";
        if ("cs".equals(value) || "surrogate".equals(value)) return "Cs";
        if ("co".equals(value) || "privateuse".equals(value)) return "Co";
        if ("cn".equals(value) || "unassigned".equals(value)) return "Cn";
        if ("l".equals(value) || "letter".equals(value)) return "L";
        if ("m".equals(value) || "mark".equals(value)) return "M";
        if ("n".equals(value) || "number".equals(value)) return "N";
        if ("p".equals(value) || "punctuation".equals(value)) return "P";
        if ("s".equals(value) || "symbol".equals(value)) return "S";
        if ("z".equals(value) || "separator".equals(value)) return "Z";
        if ("c".equals(value) || "other".equals(value)) return "C";
        if ("lc".equals(value) || "casedletter".equals(value)) return "LC";
        return null;
    }

    /**
     * Builds a General_Category set.
     * <p>
     * General_Category 집합을 만든다.
     *
     * @param category short category / 짧은 범주
     * @return the set / 집합
     */
    private static RegExpCharSet buildCategory(final String category) {
        return buildSet(new UnicodePredicate() {
            public boolean includes(int codePoint, int type) {
                return categoryOf(type).startsWith(category) || "LC".equals(category)
                        && (type == Character.UPPERCASE_LETTER || type == Character.LOWERCASE_LETTER || type == Character.TITLECASE_LETTER);
            }
        });
    }

    /**
     * Converts a Java category constant to the short General_Category prefix.
     * <p>
     * Java 범주 상수를 짧은 General_Category 접두사로 바꾼다.
     *
     * @param type Character type / Character 형식
     * @return short prefix / 짧은 접두사
     */
    private static String categoryOf(int type) {
        switch (type) {
        case Character.UPPERCASE_LETTER: return "Lu";
        case Character.LOWERCASE_LETTER: return "Ll";
        case Character.TITLECASE_LETTER: return "Lt";
        case Character.MODIFIER_LETTER: return "Lm";
        case Character.OTHER_LETTER: return "Lo";
        case Character.NON_SPACING_MARK: return "Mn";
        case Character.COMBINING_SPACING_MARK: return "Mc";
        case Character.ENCLOSING_MARK: return "Me";
        case Character.DECIMAL_DIGIT_NUMBER: return "Nd";
        case Character.LETTER_NUMBER: return "Nl";
        case Character.OTHER_NUMBER: return "No";
        case Character.CONNECTOR_PUNCTUATION: return "Pc";
        case Character.DASH_PUNCTUATION: return "Pd";
        case Character.START_PUNCTUATION: return "Ps";
        case Character.END_PUNCTUATION: return "Pe";
        case Character.INITIAL_QUOTE_PUNCTUATION: return "Pi";
        case Character.FINAL_QUOTE_PUNCTUATION: return "Pf";
        case Character.OTHER_PUNCTUATION: return "Po";
        case Character.MATH_SYMBOL: return "Sm";
        case Character.CURRENCY_SYMBOL: return "Sc";
        case Character.MODIFIER_SYMBOL: return "Sk";
        case Character.OTHER_SYMBOL: return "So";
        case Character.SPACE_SEPARATOR: return "Zs";
        case Character.LINE_SEPARATOR: return "Zl";
        case Character.PARAGRAPH_SEPARATOR: return "Zp";
        case Character.CONTROL: return "Cc";
        case Character.FORMAT: return "Cf";
        case Character.SURROGATE: return "Cs";
        case Character.PRIVATE_USE: return "Co";
        default: return "Cn";
        }
    }

    /**
     * Builds one of the binary Unicode properties.
     * <p>
     * 이진 유니코드 속성 하나를 만든다.
     *
     * @param name normalized name / 정규화한 이름
     * @return the set or null / 집합 또는 null
     */
    private static RegExpCharSet buildBinary(final String name) {
        if ("any".equals(name)) {
            RegExpCharSet set = new RegExpCharSet();
            set.addRange(0, 0x10FFFF);
            return set;
        }
        if ("ascii".equals(name)) {
            RegExpCharSet set = new RegExpCharSet();
            set.addRange(0, 0x7F);
            return set;
        }
        UnicodePredicate test;
        if ("assigned".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) { return t != Character.UNASSIGNED; } };
        } else if ("alphabetic".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) { return Character.isLetter(c) || t == Character.LETTER_NUMBER; } };
        } else if ("lowercase".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) { return Character.isLowerCase(c); } };
        } else if ("uppercase".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) { return Character.isUpperCase(c); } };
        } else if ("cased".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) { return Character.isUpperCase(c) || Character.isLowerCase(c) || t == Character.TITLECASE_LETTER; } };
        } else if ("idstart".equals(name) || "xidstart".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) { return Character.isLetter(c) || t == Character.LETTER_NUMBER; } };
        } else if ("idcontinue".equals(name) || "xidcontinue".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) {
                return Character.isLetter(c) || Character.isDigit(c) || t == Character.NON_SPACING_MARK
                        || t == Character.COMBINING_SPACING_MARK || t == Character.CONNECTOR_PUNCTUATION;
            } };
        } else if ("whitespace".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) {
                return c == 0x0009 || c == 0x000A || c == 0x000B || c == 0x000C || c == 0x000D
                        || c == 0x0020 || c == 0x0085 || c == 0x00A0 || c == 0x1680
                        || c >= 0x2000 && c <= 0x200A || c == 0x2028 || c == 0x2029 || c == 0x202F
                        || c == 0x205F || c == 0x3000;
            } };
        } else if ("asciihexdigit".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) {
                return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f';
            } };
        } else if ("hexdigit".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) {
                return c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f'
                        || c >= 0xFF10 && c <= 0xFF19 || c >= 0xFF21 && c <= 0xFF26 || c >= 0xFF41 && c <= 0xFF46;
            } };
        } else if ("variationselector".equals(name)) {
            test = new UnicodePredicate() { public boolean includes(int c, int t) {
                return c >= 0xFE00 && c <= 0xFE0F || c >= 0xE0100 && c <= 0xE01EF;
            } };
        } else {
            return null;
        }
        return buildSet(test);
    }

    /**
     * Builds selected Unicode Script property sets.
     * <p>
     * 선택된 Unicode Script 속성 집합을 만든다.
     *
     * @param script normalized script / 정규화한 script
     * @return set or null / 집합 또는 null
     */
    private static RegExpCharSet scriptSet(final String script) {
        final int[] range;
        if ("latin".equals(script) || "latn".equals(script)) {
            range = new int[] { 0x0041, 0x024F, 0x1D00, 0x1D7F, 0x1D80, 0x1DBF, 0x1E00, 0x1EFF, 0x2C60, 0x2C7F, 0xA720, 0xA7FF, 0xAB30, 0xAB6F };
        } else if ("greek".equals(script) || "grek".equals(script)) {
            range = new int[] { 0x0370, 0x03FF, 0x1F00, 0x1FFF };
        } else if ("cyrillic".equals(script) || "cyrl".equals(script)) {
            range = new int[] { 0x0400, 0x052F, 0x1C80, 0x1C8F, 0x2DE0, 0x2DFF, 0xA640, 0xA69F };
        } else if ("hebrew".equals(script) || "hebr".equals(script)) {
            range = new int[] { 0x0590, 0x05FF };
        } else if ("arabic".equals(script) || "arab".equals(script)) {
            range = new int[] { 0x0600, 0x06FF, 0x0750, 0x077F, 0x08A0, 0x08FF, 0xFB50, 0xFDFF, 0xFE70, 0xFEFF };
        } else if ("devanagari".equals(script) || "deva".equals(script)) {
            range = new int[] { 0x0900, 0x097F, 0xA8E0, 0xA8FF };
        } else if ("hiragana".equals(script) || "hira".equals(script)) {
            range = new int[] { 0x3040, 0x309F, 0x1B001, 0x1B11F };
        } else if ("katakana".equals(script) || "kana".equals(script)) {
            range = new int[] { 0x30A0, 0x30FF, 0x31F0, 0x31FF, 0x32D0, 0x32FF, 0xFF66, 0xFF9F, 0x1B000, 0x1B120 };
        } else if ("han".equals(script) || "hani".equals(script)) {
            range = new int[] { 0x2E80, 0x2FDF, 0x3005, 0x3005, 0x3007, 0x3007, 0x3400, 0x4DBF, 0x4E00, 0x9FFF, 0xF900, 0xFAFF, 0x20000, 0x3134F };
        } else if ("hangul".equals(script) || "hang".equals(script)) {
            range = new int[] { 0x1100, 0x11FF, 0x3130, 0x318F, 0xA960, 0xA97F, 0xAC00, 0xD7AF, 0xD7B0, 0xD7FF };
        } else {
            return null;
        }
        return buildSet(new UnicodePredicate() {
            public boolean includes(int c, int t) {
                // Script 의 시작·끝 쌍을 순서대로 검사한다.
                for (int i = 0; i < range.length; i += 2) {
                    if (c >= range[i] && c <= range[i + 1]) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Builds a sorted range set by testing every Unicode scalar value.
     * <p>
     * 모든 유니코드 스칼라 값을 시험해 정렬된 구간 집합을 만든다.
     *
     * @param test membership predicate / 포함 조건
     * @return normalized set / 정규화한 집합
     */
    private static RegExpCharSet buildSet(UnicodePredicate test) {
        RegExpCharSet set = new RegExpCharSet();
        int start = -1;
        // 모든 스칼라 값을 훑어 조건이 참인 연속 구간을 모은다.
        for (int c = 0; c <= 0x10FFFF; c++) {
            boolean inside = !(c >= 0xD800 && c <= 0xDFFF) && test.includes(c, Character.getType(c));
            if (inside && start < 0) {
                start = c;
            } else if (!inside && start >= 0) {
                set.addRange(start, c - 1);
                start = -1;
            }
        }
        if (start >= 0) {
            set.addRange(start, 0x10FFFF);
        }
        set.normalize();
        return set;
    }

    /**
     * Predicate for Unicode property table generation.
     * <p>
     * 유니코드 속성 표 생성을 위한 조건.
     */
    private interface UnicodePredicate {
        /**
         * Tests one code point.
         * <p>
         * 코드 포인트 하나를 검사한다.
         *
         * @param codePoint code point / 코드 포인트
         * @param type Character.getType / Character.getType
         * @return true if included / 포함하면 true
         */
        boolean includes(int codePoint, int type);
    }
}
