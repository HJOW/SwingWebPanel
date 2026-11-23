package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.Locale;

/**
 * Case-insensitive matching support: the ECMAScript <code>Canonicalize</code> operation and the inverse lookup of all
 * characters that canonicalize to the same value.
 * <p>
 * Without the u flag (ES2015 21.2.2.8.2): the character is converted with the full String upper-case mapping; if the
 * result is not a single code unit, or it would map a non-ASCII character to ASCII, the character is unchanged.
 * With the u flag the specification uses simple case folding; Java has no case folding API, so it is approximated by
 * <code>toLowerCase(toUpperCase(cp))</code> with U+0130 and U+0131 kept as they are (which are special in the folding data).
 * Unicode data comes from the running JRE (Unicode 4.0 in JRE 6). The tables are built lazily once per mode; the methods
 * are static and thread-safe.
 * <p>
 * 대소문자 무시 일치 지원: ECMAScript <code>Canonicalize</code> 연산과, 같은 값으로 정규화되는 모든 문자를 찾는 역방향 조회.
 * <p>
 * u 플래그가 없을 때(ES2015 21.2.2.8.2): 문자를 문자열의 전체 대문자 매핑으로 바꾸고, 결과가 코드 단위 하나가 아니거나 ASCII 가 아닌 문자가 ASCII 로
 * 바뀌면 문자를 그대로 둔다. u 플래그가 있으면 규격은 단순 대소문자 접기를 쓰는데 Java 에는 접기 API 가 없어
 * <code>toLowerCase(toUpperCase(cp))</code> 로 근사하며 U+0130, U+0131 은 그대로 둔다(접기 데이터에서 특수한 문자이기 때문).
 * 유니코드 정보는 실행 중인 JRE 에서 오며(JRE 6 은 유니코드 4.0) 표는 모드마다 한 번 지연 생성한다. 메서드는 정적이며 스레드 안전하다.
 */
final class CaseFold {

    /**
     * Largest code point covered by the unicode-mode table (the supplementary planes that have case mappings
     * in Unicode 4.0 are all below this).
     * <p>
     * 유니코드 모드 표가 다루는 최대 코드 포인트(유니코드 4.0 에서 대소문자 매핑이 있는 보충 평면 문자는 모두 이 값 미만).
     */
    private static final int UNICODE_TABLE_SIZE = 0x20000;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CaseFold() {
    }

    /**
     * Lazily built table for matching without the u flag.
     * <p>
     * u 플래그가 없는 일치용으로 지연 생성되는 표.
     */
    private static final class LegacyTable {
        /**
         * canon[c] is Canonicalize(c) for every UTF-16 code unit c.
         * <p>
         * canon[c] 는 모든 UTF-16 코드 단위 c 에 대한 Canonicalize(c) 이다.
         */
        static final char[] CANON = new char[0x10000];

        /**
         * first[k] is the head of the linked list of characters whose canonical form is k (-1 if none).
         * <p>
         * first[k] 는 정규형이 k 인 문자 연결 리스트의 머리(없으면 -1).
         */
        static final int[] FIRST = new int[0x10000];

        /**
         * next[c] is the next character of the same list (-1 at the end).
         * <p>
         * next[c] 는 같은 리스트의 다음 문자(끝이면 -1).
         */
        static final int[] NEXT = new int[0x10000];

        static {
            java.util.Arrays.fill(FIRST, -1);
            // 모든 코드 단위의 정규형을 구하고 정규형별 연결 리스트를 만든다.
            for (int c = 0; c < 0x10000; c++) {
                CANON[c] = (char) legacyCanonical(c);
            }
            // 정규형이 같은 문자끼리 연결 리스트로 묶는다.
            for (int c = 0; c < 0x10000; c++) {
                int k = CANON[c];
                NEXT[c] = FIRST[k];
                FIRST[k] = c;
            }
        }
    }

    /**
     * Lazily built table for matching with the u flag.
     * <p>
     * u 플래그가 있는 일치용으로 지연 생성되는 표.
     */
    private static final class UnicodeTable {
        /**
         * canon[cp] is the folded form of code point cp.
         * <p>
         * canon[cp] 는 코드 포인트 cp 의 접은 형태이다.
         */
        static final int[] CANON = new int[UNICODE_TABLE_SIZE];

        /**
         * first[k] is the head of the list of code points that fold to k (-1 if none).
         * <p>
         * first[k] 는 k 로 접히는 코드 포인트 목록의 머리(없으면 -1).
         */
        static final int[] FIRST = new int[UNICODE_TABLE_SIZE];

        /**
         * next[cp] is the next code point of the same list.
         * <p>
         * next[cp] 는 같은 목록의 다음 코드 포인트.
         */
        static final int[] NEXT = new int[UNICODE_TABLE_SIZE];

        static {
            java.util.Arrays.fill(FIRST, -1);
            // 보충 평면까지 포함해 접은 형태를 구하고 같은 형태끼리 연결한다.
            for (int c = 0; c < UNICODE_TABLE_SIZE; c++) {
                CANON[c] = unicodeFold(c);
            }
            // 접은 형태가 같은 코드 포인트끼리 연결 리스트로 묶는다.
            for (int c = 0; c < UNICODE_TABLE_SIZE; c++) {
                int k = CANON[c];
                NEXT[c] = FIRST[k];
                FIRST[k] = c;
            }
        }
    }

    /**
     * Canonicalize for matching without the u flag (computed directly).
     * <p>
     * u 플래그가 없을 때의 Canonicalize(직접 계산).
     *
     * @param c code unit / 코드 단위
     * @return canonical code unit / 정규형 코드 단위
     */
    private static int legacyCanonical(int c) {
        if (c < 128) {
            return (c >= 'a' && c <= 'z') ? c - 32 : c;
        }
        String s = String.valueOf((char) c).toUpperCase(Locale.ROOT);
        if (s.length() != 1) {
            return c;
        }
        char cu = s.charAt(0);
        if (cu < 128) {
            return c;
        }
        return cu;
    }

    /**
     * Approximation of simple case folding.
     * <p>
     * 단순 대소문자 접기의 근사.
     *
     * @param cp code point / 코드 포인트
     * @return folded code point / 접은 코드 포인트
     */
    private static int unicodeFold(int cp) {
        if (cp == 0x130 || cp == 0x131) {
            return cp;
        }
        return Character.toLowerCase(Character.toUpperCase(cp));
    }

    /**
     * Canonicalize(ch) for a character of a pattern or input.
     * <p>
     * 패턴이나 입력의 문자에 대한 Canonicalize(ch).
     *
     * @param ch      code unit (or code point with the u flag) / 코드 단위(u 플래그면 코드 포인트)
     * @param unicode true if the u flag is set / u 플래그가 있으면 true
     * @return canonical form / 정규형
     */
    static int canonicalize(int ch, boolean unicode) {
        if (unicode) {
            if (ch < UNICODE_TABLE_SIZE) {
                return UnicodeTable.CANON[ch];
            }
            return unicodeFold(ch);
        }
        if (ch < 128) {
            return (ch >= 'a' && ch <= 'z') ? ch - 32 : ch;
        }
        if (ch > 0xFFFF) {
            return ch;
        }
        return LegacyTable.CANON[ch];
    }

    /**
     * Lists the characters other than <code>ch</code> that have the same canonical form.
     * <p>
     * <code>ch</code> 와 정규형이 같은 다른 문자들을 나열한다.
     *
     * @param ch      code unit or code point / 코드 단위 또는 코드 포인트
     * @param unicode true if the u flag is set / u 플래그가 있으면 true
     * @return equivalent characters, possibly empty / 같은 정규형의 문자들(비어 있을 수 있음)
     */
    static int[] equivalents(int ch, boolean unicode) {
        int[] first;
        int[] next;
        int key;
        if (unicode) {
            if (ch >= UNICODE_TABLE_SIZE) {
                return new int[0];
            }
            key = UnicodeTable.CANON[ch];
            first = UnicodeTable.FIRST;
            next = UnicodeTable.NEXT;
        } else {
            if (ch > 0xFFFF) {
                return new int[0];
            }
            key = LegacyTable.CANON[ch];
            first = LegacyTable.FIRST;
            next = LegacyTable.NEXT;
        }
        int[] buf = new int[8];
        int count = 0;
        // 같은 정규형의 연결 리스트를 따라가며 자기 자신을 뺀 문자를 모은다.
        for (int c = first[key]; c >= 0; c = next[c]) {
            if (c != ch) {
                if (count == buf.length) {
                    buf = java.util.Arrays.copyOf(buf, count * 2);
                }
                buf[count++] = c;
            }
        }
        return java.util.Arrays.copyOf(buf, count);
    }
}
