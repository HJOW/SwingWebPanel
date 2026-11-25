package org.duckdns.hjow.swingwebpanel.js.text;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Unicode 18 normalization using UAX #15 canonical ordering and composition.
 * <p>
 * UAX #15의 정준 순서와 재조합을 따르는 Unicode 18 정규화.
 */
final class UnicodeNormalization {
    /** First modern Hangul syllable. / 현대 한글 음절의 시작 코드 포인트. */
    private static final int SYLLABLE_BASE = 0xac00;

    /** First leading Hangul jamo. / 초성 자모의 시작 코드 포인트. */
    private static final int LEADING_BASE = 0x1100;

    /** First vowel Hangul jamo. / 중성 자모의 시작 코드 포인트. */
    private static final int VOWEL_BASE = 0x1161;

    /** Trailing jamo base for the absent final consonant. / 종성이 없는 항목에 해당하는 종성 기준 코드 포인트. */
    private static final int TRAILING_BASE = 0x11a7;

    /** Number of leading jamo in the algorithm. / 알고리즘에서 다루는 초성 수. */
    private static final int LEADING_COUNT = 19;

    /** Number of vowel jamo in the algorithm. / 알고리즘에서 다루는 중성 수. */
    private static final int VOWEL_COUNT = 21;

    /** Number of trailing choices, including no final consonant. / 종성이 없는 경우를 포함한 종성 선택 수. */
    private static final int TRAILING_COUNT = 28;

    /** Number of vowel and trailing combinations. / 중성과 종성의 조합 수. */
    private static final int JAMOS_PER_LEADING = VOWEL_COUNT * TRAILING_COUNT;

    /** Number of precomposed modern syllables. / 미리 조합된 현대 한글 음절 수. */
    private static final int SYLLABLE_COUNT = LEADING_COUNT * JAMOS_PER_LEADING;

    /** Static normalizer. / 정적 정규화 구현. */
    private UnicodeNormalization() {
    }

    /**
     * Normalizes a string, preserving any unpaired surrogate code units.
     * <p>
     * 짝 없는 서로게이트 코드 단위를 보존하면서 문자열을 정규화한다.
     *
     * @param input original UTF-16 string / 원본 UTF-16 문자열
     * @param compatibility whether compatibility mappings participate / 호환 분해를 포함할지 여부
     * @param compose whether to recompose canonically / 정준 재조합을 할지 여부
     * @return the normalized text / 정규화한 문자열
     */
    static String normalize(String input, boolean compatibility, boolean compose) {
        int ascii = 0;
        // ASCII로만 된 문자열은 네 정규화 형식에서 모두 동일하다.
        while (ascii < input.length() && input.charAt(ascii) < 128) {
            ascii++;
        }
        if (ascii == input.length()) {
            return input;
        }
        Buffer text = new Buffer(input.length());
        // 짝 없는 서로게이트를 포함한 코드 포인트를 원문 순서대로 분해한다.
        for (int i = 0; i < input.length();) {
            int cp = CodePoints.codePointAt(input, i);
            decompose(cp, compatibility, text);
            i += Character.charCount(cp);
        }
        text.order();
        if (compose) {
            text.compose();
        }
        return text.toString();
    }

    /**
     * Expands a character recursively using its selected decomposition mapping.
     * <p>
     * 선택한 분해 매핑을 이용해 문자를 재귀적으로 확장한다.
     *
     * @param cp the code point / 코드 포인트
     * @param compatibility whether compatibility mappings participate / 호환 분해를 포함할지 여부
     * @param text destination / 저장할 위치
     */
    private static void decompose(int cp, boolean compatibility, Buffer text) {
        int syllable = cp - SYLLABLE_BASE;
        // 한글 음절의 초성, 중성, 선택적 종성은 표 없이 계산한다.
        if (syllable >= 0 && syllable < SYLLABLE_COUNT) {
            text.append(LEADING_BASE + syllable / JAMOS_PER_LEADING);
            text.append(VOWEL_BASE + syllable % JAMOS_PER_LEADING / TRAILING_COUNT);
            int trailing = syllable % TRAILING_COUNT;
            if (trailing != 0) {
                text.append(TRAILING_BASE + trailing);
            }
            return;
        }
        int[] entry = Tables.DECOMPOSE.get(Integer.valueOf(cp));
        if (entry == null || entry[0] < 0 && !compatibility) {
            text.append(cp);
            return;
        }
        // 호환 분해 표시를 건너뛰고 분해에 나타난 문자를 다시 분해한다.
        for (int i = entry[0] < 0 ? 1 : 0; i < entry.length; i++) {
            decompose(entry[i], compatibility, text);
        }
    }

    /**
     * Returns a code point's canonical combining class, which defaults to zero.
     * <p>
     * 코드 포인트의 정준 결합 클래스를 반환한다. 기본값은 0이다.
     *
     * @param cp the code point / 코드 포인트
     * @return the class / 결합 클래스
     */
    private static int combiningClass(int cp) {
        int[] entry = Tables.COMBINING.get(Integer.valueOf(cp));
        return entry == null ? 0 : entry[0];
    }

    /**
     * Encodes a pair of Unicode code points without collisions.
     * <p>
     * 두 유니코드 코드 포인트를 충돌 없이 인코딩한다.
     *
     * @param first the starter / 시작 문자
     * @param second the next character / 다음 문자
     * @return the pair key / 조합 키
     */
    private static long pair(int first, int second) {
        return ((long) first << 21) | second;
    }

    /**
     * Finds an eligible canonical composition, including algorithmic Hangul syllables.
     * <p>
     * 알고리즘으로 조합하는 한글 음절을 포함하여 가능한 정준 재조합을 찾는다.
     *
     * @param first the starter / 시작 문자
     * @param second the following code point / 다음 코드 포인트
     * @return the composed code point, or -1 if there is none / 조합한 코드 포인트. 없으면 -1
     */
    private static int composite(int first, int second) {
        int leading = first - LEADING_BASE;
        int vowel = second - VOWEL_BASE;
        if (leading >= 0 && leading < LEADING_COUNT && vowel >= 0 && vowel < VOWEL_COUNT) {
            return SYLLABLE_BASE + (leading * VOWEL_COUNT + vowel) * TRAILING_COUNT;
        }
        int syllable = first - SYLLABLE_BASE;
        int trailing = second - TRAILING_BASE;
        if (syllable >= 0 && syllable < SYLLABLE_COUNT && syllable % TRAILING_COUNT == 0 && trailing > 0 && trailing < TRAILING_COUNT) {
            return first + trailing;
        }
        Integer joined = Tables.COMPOSE.get(Long.valueOf(pair(first, second)));
        return joined == null ? -1 : joined.intValue();
    }

    /**
     * Decodes the generated tables only when non-ASCII input needs them.
     * <p>
     * ASCII가 아닌 입력에 필요할 때만 생성한 표를 해석한다.
     */
    private static final class Tables {
        /** Nonzero canonical combining classes. / 0이 아닌 정준 결합 클래스. */
        static final Map<Integer, int[]> COMBINING = decode(UnicodePropertyData.COMBINING_CLASS);

        /** Canonical and compatibility decompositions. / 정준 및 호환 분해. */
        static final Map<Integer, int[]> DECOMPOSE = decode(UnicodePropertyData.DECOMPOSITION);

        /** Inverse pairs usable for canonical composition. / 정준 재조합에 사용할 역방향 분해 쌍. */
        static final Map<Long, Integer> COMPOSE = compositions();

        /**
         * Reads hexadecimal columns, storing -1 as the tag for a compatibility mapping.
         * <p>
         * 16진수 열을 읽고 호환 분해 표시를 -1로 저장한다.
         *
         * @param chunks packed rows / 묶어 저장한 자료 행
         * @return decoded rows by code point / 코드 포인트별 해석한 행
         */
        private static Map<Integer, int[]> decode(String[] chunks) {
            Map<Integer, int[]> map = new HashMap<Integer, int[]>();
            // 상수 길이 제한으로 나뉜 여러 묶음의 행을 모두 읽는다.
            for (int i = 0; i < chunks.length; i++) {
                String[] rows = chunks[i].split("\\|");
                // 각 키에 정수 열을 연결한다.
                for (int j = 0; j < rows.length; j++) {
                    String row = rows[j];
                    int split = row.indexOf('=');
                    boolean tagged = row.charAt(split + 1) == '!';
                    int offset = tagged ? 1 : 0;
                    String[] items = row.substring(split + 1 + offset).split(" +");
                    int[] values = new int[items.length + offset];
                    if (tagged) {
                        values[0] = -1;
                    }
                    // 매핑의 각 코드 또는 결합 클래스를 읽는다.
                    for (int k = 0; k < items.length; k++) {
                        values[k + offset] = Integer.parseInt(items[k], 16);
                    }
                    map.put(Integer.valueOf(Integer.parseInt(row.substring(0, split), 16)), values);
                }
            }
            return map;
        }

        /**
         * Inverts the eligible two-character mappings declared by the generator.
         * <p>
         * 생성기가 선택한 두 문자 매핑의 방향을 뒤집는다.
         *
         * @return the canonical composition lookup / 정준 재조합 조회 표
         */
        private static Map<Long, Integer> compositions() {
            Map<Long, Integer> map = new HashMap<Long, Integer>();
            // 각 조합 쌍에 원래 합성된 코드 포인트를 연결한다.
            for (Map.Entry<Integer, int[]> entry : decode(UnicodePropertyData.COMPOSITION).entrySet()) {
                int[] parts = entry.getValue();
                map.put(Long.valueOf(pair(parts[0], parts[1])), entry.getKey());
            }
            return map;
        }
    }

    /**
     * Mutable code-point buffer for one normalization call.
     * <p>
     * 정규화 한 번에 쓰는 가변 코드 포인트 버퍼.
     */
    private static final class Buffer {
        /** Code points in their current order. / 현재 순서의 코드 포인트. */
        private int[] values;

        /** Active number of code points. / 사용 중인 코드 포인트 수. */
        private int size;

        /**
         * Reserves a small initial buffer based on the UTF-16 input.
         * <p>
         * UTF-16 입력에 따라 작은 초기 버퍼를 확보한다.
         *
         * @param units input length in UTF-16 code units / UTF-16 단위 입력 길이
         */
        Buffer(int units) {
            values = new int[Math.max(4, Math.min(units, 4096))];
        }

        /**
         * Appends one decomposed code point.
         * <p>
         * 분해한 코드 포인트 하나를 붙인다.
         *
         * @param cp the code point / 코드 포인트
         */
        void append(int cp) {
            if (size == values.length) {
                int next = size + size / 2;
                if (next <= size) {
                    throw new TextRangeException("String too long");
                }
                values = Arrays.copyOf(values, next);
            }
            values[size++] = cp;
        }

        /**
         * Stably orders combining characters between starters.
         * <p>
         * 시작 문자 사이의 결합 문자를 안정적으로 재정렬한다.
         */
        void order() {
            int start = 0;
            int last = 0;
            boolean disordered = false;
            // 시작 문자와 버퍼 끝은 정준 순서 구간의 경계이다.
            for (int i = 0; i <= size; i++) {
                int current = i == size ? 0 : combiningClass(values[i]);
                if (current == 0) {
                    if (disordered) {
                        orderRun(start, i);
                    }
                    start = i + 1;
                    disordered = false;
                } else {
                    disordered |= current < last;
                }
                last = current;
            }
        }

        /**
         * Orders one disordered run with insertion for short runs and stable counting for long ones.
         * <p>
         * 어긋난 짧은 구간은 삽입 정렬하고 긴 구간은 안정 계수 정렬한다.
         *
         * @param start the first combining character / 첫 결합 문자 위치
         * @param end exclusive end of the run / 구간의 끝 위치
         */
        private void orderRun(int start, int end) {
            if (end - start <= 32) {
                // 짧은 구간에서 같은 클래스의 문자는 앞뒤를 바꾸지 않는다.
                for (int i = start + 1; i < end; i++) {
                    int cp = values[i];
                    int rank = combiningClass(cp);
                    int j = i;
                    // 더 큰 클래스만 뒤로 밀어 넣을 위치를 확보한다.
                    while (j > start && combiningClass(values[j - 1]) > rank) {
                        values[j] = values[j - 1];
                        j--;
                    }
                    values[j] = cp;
                }
                return;
            }
            int[] indices = new int[256];
            // 긴 구간에서 클래스별 개수를 센다.
            for (int i = start; i < end; i++) {
                indices[combiningClass(values[i])]++;
            }
            int next = 0;
            // 각 클래스를 넣을 첫 위치로 개수를 누적한다.
            for (int i = 0; i < indices.length; i++) {
                int count = indices[i];
                indices[i] = next;
                next += count;
            }
            int[] sorted = new int[end - start];
            // 같은 클래스는 입력에 나온 순서대로 채운다.
            for (int i = start; i < end; i++) {
                sorted[indices[combiningClass(values[i])]++] = values[i];
            }
            System.arraycopy(sorted, 0, values, start, sorted.length);
        }

        /**
         * Recomposes every unblocked pair into the same buffer.
         * <p>
         * 가로막히지 않은 각 문자 쌍을 같은 버퍼에서 재조합한다.
         */
        void compose() {
            int next = 0;
            int starter = -1;
            int last = 0;
            // 마지막으로 남긴 문자의 클래스가 현재 조합을 막는지 확인한다.
            for (int i = 0; i < size; i++) {
                int cp = values[i];
                int current = combiningClass(cp);
                int joined = starter >= 0 && (last == 0 || last < current) ? composite(values[starter], cp) : -1;
                if (joined >= 0) {
                    values[starter] = joined;
                } else {
                    if (current == 0) {
                        starter = next;
                    }
                    values[next++] = cp;
                    last = current;
                }
            }
            size = next;
        }

        /**
         * Encodes the result as a JavaScript-sized UTF-16 string.
         * <p>
         * 결과를 JavaScript 문자열 크기 한도에 맞춰 UTF-16으로 인코딩한다.
         *
         * @return normalized text / 정규화한 문자열
         */
        @Override
        public String toString() {
            StringBuilder out = new StringBuilder(Math.min(4096, size));
            // 각 코드 포인트를 하나 또는 두 개의 UTF-16 단위로 쓴다.
            for (int i = 0; i < size; i++) {
                int cp = values[i];
                if (out.length() > TextTransform.MAX_STRING_LENGTH - Character.charCount(cp)) {
                    throw new TextRangeException("String too long");
                }
                out.appendCodePoint(cp);
            }
            return out.toString();
        }
    }
}
