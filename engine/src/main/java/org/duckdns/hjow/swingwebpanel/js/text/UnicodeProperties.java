package org.duckdns.hjow.swingwebpanel.js.text;

import java.util.HashMap;
import java.util.Map;

/**
 * Exact ECMAScript Unicode property names and ranges shared by the lexer and RegExp engine.
 * <p>
 * 렉서와 정규식 엔진이 공유하는 정확한 ECMAScript 유니코드 속성 이름과 범위.
 */
public final class UnicodeProperties {

    /** Range arrays keyed by the exact accepted spelling. / 허용하는 정확한 철자별 범위 배열. */
    private static final Map<String, int[]> PROPERTIES = load();

    /** Unicode ID_Start ranges. / 유니코드 ID_Start 범위. */
    private static final int[] ID_START = PROPERTIES.get("ID_Start");

    /** Unicode ID_Continue ranges. / 유니코드 ID_Continue 범위. */
    private static final int[] ID_CONTINUE = PROPERTIES.get("ID_Continue");

    /** Unicode Space_Separator ranges. / 유니코드 Space_Separator 범위. */
    private static final int[] SPACE_SEPARATOR = PROPERTIES.get("Zs");

    /** Unicode Cased ranges. / 유니코드 Cased 범위. */
    private static final int[] CASED = PROPERTIES.get("Cased");

    /** Unicode Case_Ignorable ranges. / 유니코드 Case_Ignorable 범위. */
    private static final int[] CASE_IGNORABLE = PROPERTIES.get("Case_Ignorable");

    /** Static helper class. / 정적 도우미 클래스. */
    private UnicodeProperties() {
    }

    /**
     * Returns inclusive start and end pairs for an exact property name, or null for unknown names.
     * <p>
     * 속성 이름의 정확한 철자에 대응하는 닫힌 시작·끝 쌍을 반환한다. 알 수 없는 이름이면 null이다.
     *
     * @param name property name, including an optional prefix / 접두어를 포함할 수 있는 속성 이름
     * @return copy of the inclusive ranges, or null / 닫힌 범위의 사본, 또는 null
     */
    public static int[] ranges(String name) {
        int[] found = PROPERTIES.get(name);
        return found == null ? null : found.clone();
    }

    /**
     * Recognizes an unqualified General_Category value or alias.
     * <p>
     * 접두어 없는 일반 범주 값 또는 그 별칭을 확인한다.
     *
     * @param name property expression / 속성 표현식
     * @return whether the expression names a category without a property prefix / 속성 접두어 없이 일반 범주를 지정하는지 여부
     */
    public static boolean isBareGeneralCategory(String name) {
        return PROPERTIES.containsKey("gc=" + name);
    }

    /**
     * Tests ID_Start in the generated Unicode data.
     * <p>
     * 생성한 유니코드 데이터에서 ID_Start를 검사한다.
     *
     * @param cp Unicode code point / 유니코드 코드 포인트
     * @return true when included / 포함하면 true
     */
    static boolean isIdStart(int cp) {
        return contains(ID_START, cp);
    }

    /**
     * Tests ID_Continue in the generated Unicode data.
     * <p>
     * 생성한 유니코드 데이터에서 ID_Continue를 검사한다.
     *
     * @param cp Unicode code point / 유니코드 코드 포인트
     * @return true when included / 포함하면 true
     */
    static boolean isIdContinue(int cp) {
        return contains(ID_CONTINUE, cp);
    }

    /**
     * Tests Unicode General_Category Space_Separator.
     * <p>
     * 유니코드 일반 범주 Space_Separator를 검사한다.
     *
     * @param cp Unicode code point / 유니코드 코드 포인트
     * @return true when included / 포함하면 true
     */
    static boolean isSpaceSeparator(int cp) {
        return contains(SPACE_SEPARATOR, cp);
    }

    /**
     * Tests the Unicode Cased property.
     * <p>
     * 유니코드 Cased 속성을 검사한다.
     *
     * @param cp Unicode code point / 유니코드 코드 포인트
     * @return true when included / 포함하면 true
     */
    static boolean isCased(int cp) {
        return contains(CASED, cp);
    }

    /**
     * Tests the Unicode Case_Ignorable property.
     * <p>
     * 유니코드 Case_Ignorable 속성을 검사한다.
     *
     * @param cp Unicode code point / 유니코드 코드 포인트
     * @return true when included / 포함하면 true
     */
    static boolean isCaseIgnorable(int cp) {
        return contains(CASE_IGNORABLE, cp);
    }

    /**
     * Finds membership in a sorted inclusive range table.
     * <p>
     * 정렬한 닫힌 범위 표에서 포함 여부를 찾는다.
     *
     * @param ranges inclusive pairs / 닫힌 범위 쌍
     * @param cp Unicode code point / 유니코드 코드 포인트
     * @return true when included / 포함하면 true
     */
    private static boolean contains(int[] ranges, int cp) {
        int lo = 0;
        int hi = ranges.length / 2;
        // 코드 포인트를 포함하는 범위가 있을 때까지 이진 탐색한다.
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (cp < ranges[2 * mid]) {
                hi = mid;
            } else if (cp > ranges[2 * mid + 1]) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads canonical ranges and links exact property aliases to their shared arrays.
     * <p>
     * 표준 범위를 읽고 정확한 속성 별칭을 공유 배열에 연결한다.
     *
     * @return property lookup table / 속성 조회 표
     */
    private static Map<String, int[]> load() {
        Map<String, int[]> tables = new HashMap<String, int[]>();
        // 길이 제한 때문에 같은 범위 이름이 여러 조각에 등장할 수 있다.
        for (String chunk : UnicodePropertyData.RANGES) {
            // 하나의 조각에 들어 있는 범위 행을 읽는다.
            for (String row : chunk.split("\\|")) {
                int sep = row.indexOf('=');
                String key = row.substring(0, sep);
                tables.put(key, decodeRanges(tables.get(key), row.substring(sep + 1)));
            }
        }
        Map<String, int[]> result = new HashMap<String, int[]>();
        // 별칭과 표준 이름이 같은 배열을 가리키게 한다.
        for (String chunk : UnicodePropertyData.ALIASES) {
            // 하나의 조각에 들어 있는 별칭 행을 읽는다.
            for (String row : chunk.split("\\|")) {
                int sep = row.indexOf('=');
                result.put(row.substring(0, sep).replace('~', '='), tables.get(row.substring(sep + 1)));
            }
        }
        return result;
    }

    /**
     * Appends hexadecimal range pairs to any previously decoded fragment.
     * <p>
     * 이미 해석한 조각 뒤에 16진수 범위 쌍을 추가한다.
     *
     * @param previous earlier pairs, or null / 이전 범위 쌍, 또는 null
     * @param data comma-separated pairs / 쉼표로 구분한 범위 쌍
     * @return decoded pairs / 해석한 범위 쌍
     */
    private static int[] decodeRanges(int[] previous, String data) {
        String[] parts = data.length() == 0 ? new String[0] : data.split(",");
        int offset = previous == null ? 0 : previous.length;
        int[] ranges = new int[offset + parts.length * 2];
        if (offset != 0) {
            System.arraycopy(previous, 0, ranges, 0, offset);
        }
        // 조각의 시작·끝을 코드 포인트로 바꿔 배열에 연이어 쓴다.
        for (String part : parts) {
            int sep = part.indexOf('-');
            ranges[offset++] = Integer.parseInt(part.substring(0, sep), 16);
            ranges[offset++] = Integer.parseInt(part.substring(sep + 1), 16);
        }
        return ranges;
    }
}
