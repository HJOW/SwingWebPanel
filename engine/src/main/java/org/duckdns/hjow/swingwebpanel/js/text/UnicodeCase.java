package org.duckdns.hjow.swingwebpanel.js.text;

import java.util.HashMap;
import java.util.Map;

/**
 * Default Unicode case conversion and simple case folding for ECMAScript.
 * <p>
 * ECMAScript의 기본 유니코드 대소문자 변환과 단순 대소문자 접기.
 */
public final class UnicodeCase {

    /** Greek capital sigma. / 그리스어 대문자 시그마. */
    private static final int SIGMA = 0x03A3;

    /** Greek small final sigma. / 그리스어 어말 소문자 시그마. */
    private static final int FINAL_SIGMA = 0x03C2;

    /** Static case operations. / 정적 대소문자 연산. */
    private UnicodeCase() {
    }

    /**
     * The default full mapping tables are decoded on first use outside ASCII.
     * <p>
     * 기본 전체 매핑 표는 ASCII 밖의 문자를 처음 변환할 때 해석한다.
     */
    private static final class FullMappings {
        /** Unconditional uppercase mappings. / 무조건 적용하는 대문자 매핑. */
        static final Map<Integer, String> UPPER = decodeMappings(UnicodePropertyData.UPPER_CASE);

        /** Unconditional lowercase mappings. / 무조건 적용하는 소문자 매핑. */
        static final Map<Integer, String> LOWER = decodeMappings(UnicodePropertyData.LOWER_CASE);
    }

    /**
     * Decodes simple case folds as sorted source, target pairs.
     * <p>
     * 단순 대소문자 접기를 정렬한 원본·대상 코드 포인트 쌍으로 해석한다.
     *
     * @return new array of source, target pairs / 원본·대상 쌍을 담은 새 배열
     */
    public static int[] simpleFoldMappings() {
        Map<Integer, String> map = decodeMappings(UnicodePropertyData.SIMPLE_FOLD);
        Integer[] codePoints = map.keySet().toArray(new Integer[map.size()]);
        java.util.Arrays.sort(codePoints);
        int[] pairs = new int[codePoints.length * 2];
        // 원본 순서로 원본과 접은 코드 포인트를 연달아 저장한다.
        for (int i = 0; i < codePoints.length; i++) {
            pairs[2 * i] = codePoints[i].intValue();
            pairs[2 * i + 1] = map.get(codePoints[i]).codePointAt(0);
        }
        return pairs;
    }

    /**
     * Uppercases one non-Unicode RegExp code unit using the full default mapping and the legacy ASCII boundary.
     * <p>
     * 전체 기본 매핑과 레거시 ASCII 경계를 적용해 비유니코드 정규식 코드 단위를 대문자로 바꾼다.
     *
     * @param unit UTF-16 code unit / UTF-16 코드 단위
     * @return canonical code unit / 정규형 코드 단위
     */
    public static int legacyUpper(int unit) {
        if (unit < 0x80) {
            return unit >= 'a' && unit <= 'z' ? unit - ('a' - 'A') : unit;
        }
        String mapped = FullMappings.UPPER.get(Integer.valueOf(unit));
        if (mapped == null || mapped.length() != 1 || mapped.charAt(0) < 0x80) {
            return unit;
        }
        return mapped.charAt(0);
    }

    /**
     * Converts a string with default full Unicode casing, including the contextual final sigma.
     * <p>
     * 문맥에 따른 어말 시그마를 포함해 문자열에 기본 전체 유니코드 대소문자 매핑을 적용한다.
     *
     * @param s source string / 원본 문자열
     * @param upper true for uppercase, false for lowercase / 대문자면 true, 소문자면 false
     * @return converted UTF-16 string / 변환한 UTF-16 문자열
     */
    static String convert(String s, boolean upper) {
        StringBuilder out = new StringBuilder(s.length());
        // 유효한 쌍은 코드 포인트 하나로 읽고 고립 서로게이트는 그대로 보존한다.
        for (int start = 0; start < s.length();) {
            int cp = s.codePointAt(start);
            int next = start + Character.charCount(cp);
            if (cp < 0x80) {
                if (upper && cp >= 'a' && cp <= 'z') {
                    cp -= 'a' - 'A';
                } else if (!upper && cp >= 'A' && cp <= 'Z') {
                    cp += 'a' - 'A';
                }
                out.append((char) cp);
            } else if (!upper && cp == SIGMA && hasCasedNeighbour(s, start, false) && !hasCasedNeighbour(s, next, true)) {
                out.append((char) FINAL_SIGMA);
            } else {
                Map<Integer, String> maps = upper ? FullMappings.UPPER : FullMappings.LOWER;
                String mapped = maps.get(Integer.valueOf(cp));
                if (mapped == null) {
                    out.appendCodePoint(cp);
                } else {
                    out.append(mapped);
                }
            }
            start = next;
        }
        return out.toString();
    }

    /**
     * Tests whether the next non-ignorable code point in one direction is cased.
     * <p>
     * 한 방향에서 대소문자 판정 시 무시하는 문자를 건너뛴 뒤 대소문자 쌍을 갖는 문자가 있는지 검사한다.
     *
     * @param s source string / 원본 문자열
     * @param index starting UTF-16 boundary / 탐색을 시작하는 UTF-16 경계
     * @param forward true to look ahead, false to look behind / 뒤로 이어지는 내용을 보면 true, 앞을 보면 false
     * @return true if the next applicable code point is cased / 조건에 맞는 다음 문자가 Cased면 true
     */
    private static boolean hasCasedNeighbour(String s, int index, boolean forward) {
        // 결합 문자 등 Case_Ignorable이 끝날 때까지 앞이나 뒤를 확인한다.
        while (forward ? index < s.length() : index > 0) {
            int cp = forward ? s.codePointAt(index) : s.codePointBefore(index);
            if (!UnicodeProperties.isCaseIgnorable(cp)) {
                return UnicodeProperties.isCased(cp);
            }
            int length = Character.charCount(cp);
            index += forward ? length : -length;
        }
        return false;
    }

    /**
     * Decodes generated full case mapping rows.
     * <p>
     * 생성한 전체 대소문자 매핑 행을 해석한다.
     *
     * @param chunks data chunks / 자료 조각
     * @return code point to converted string mapping / 코드 포인트와 변환 문자열의 대응
     */
    private static Map<Integer, String> decodeMappings(String[] chunks) {
        Map<Integer, String> result = new HashMap<Integer, String>();
        // 조각마다 여러 코드 포인트 매핑이 들어 있다.
        for (String chunk : chunks) {
            // 매핑을 원본 코드 포인트와 변환 문자열로 분리한다.
            for (String row : chunk.split("\\|")) {
                int sep = row.indexOf('=');
                int cp = Integer.parseInt(row.substring(0, sep), 16);
                StringBuilder mapped = new StringBuilder(3);
                // 변환 결과에 코드 포인트가 여러 개 들어갈 수 있다.
                for (String code : row.substring(sep + 1).split(",")) {
                    mapped.appendCodePoint(Integer.parseInt(code, 16));
                }
                result.put(Integer.valueOf(cp), mapped.toString());
            }
        }
        return result;
    }
}
