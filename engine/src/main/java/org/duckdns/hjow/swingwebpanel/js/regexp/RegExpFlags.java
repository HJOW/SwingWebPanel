package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * Bit constants and parsing of regular expression flags. The matcher itself only uses {@link #IGNORE_CASE},
 * {@link #MULTILINE}, {@link #UNICODE} and {@link #DOT_ALL}; {@link #GLOBAL} and {@link #STICKY} are carried for the
 * caller (<code>RegExp.prototype.exec</code> decides where matching starts and whether it may scan forward).
 * {@link #DOT_ALL} (flag s) and {@link #HAS_INDICES} (flag d) are later extensions and are accepted as such.
 * <p>
 * 정규식 플래그의 비트 상수와 해석. 일치 검색기 자체는 {@link #IGNORE_CASE}, {@link #MULTILINE}, {@link #UNICODE}, {@link #DOT_ALL} 만 쓰고
 * {@link #GLOBAL}, {@link #STICKY} 는 호출자를 위해 보관한다(<code>RegExp.prototype.exec</code> 가 시작 위치와 앞쪽 탐색 여부를 정한다).
 * {@link #DOT_ALL}(플래그 s)과 {@link #HAS_INDICES}(플래그 d)는 이후 판 확장이며 그렇게 받아들인다.
 */
public final class RegExpFlags {

    /**
     * Flag g: global.
     * <p>
     * 플래그 g: 전역.
     */
    public static final int GLOBAL = 1;

    /**
     * Flag i: ignore case.
     * <p>
     * 플래그 i: 대소문자 무시.
     */
    public static final int IGNORE_CASE = 2;

    /**
     * Flag m: multiline.
     * <p>
     * 플래그 m: 여러 줄.
     */
    public static final int MULTILINE = 4;

    /**
     * Flag u: unicode (code point matching).
     * <p>
     * 플래그 u: 유니코드(코드 포인트 단위 일치).
     */
    public static final int UNICODE = 8;

    /**
     * Flag y: sticky.
     * <p>
     * 플래그 y: 고정 위치.
     */
    public static final int STICKY = 16;

    /**
     * Flag s: dot matches line terminators (ES2018 extension).
     * <p>
     * 플래그 s: 점이 줄 끝 문자와도 일치(ES2018 확장).
     */
    public static final int DOT_ALL = 32;

    /**
     * Flag d: expose matched substring indices (ES2022).
     * <p>
     * 플래그 d: 일치한 부분 문자열의 위치를 제공한다(ES2022).
     */
    public static final int HAS_INDICES = 64;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RegExpFlags() {
    }

    /**
     * Parses a flags string such as "gi". Each of g, i, m, s, u, y may appear at most once.
     * <p>
     * "gi" 같은 플래그 문자열을 해석한다. g, i, m, s, u, y 는 각각 최대 한 번만 나올 수 있다.
     *
     * @param flags flags text (null means none) / 플래그 문자열(null 은 없음)
     * @return flag bits / 플래그 비트
     * @throws RegExpSyntaxException on an unknown or repeated flag / 알 수 없거나 반복된 플래그이면
     */
    public static int parse(String flags) throws RegExpSyntaxException {
        int bits = 0;
        if (flags == null) {
            return 0;
        }
        // 문자마다 해당 플래그 비트를 켜고 중복이나 모르는 문자는 오류로 한다.
        for (int i = 0; i < flags.length(); i++) {
            int bit;
            switch (flags.charAt(i)) {
            case 'g':
                bit = GLOBAL;
                break;
            case 'i':
                bit = IGNORE_CASE;
                break;
            case 'm':
                bit = MULTILINE;
                break;
            case 'u':
                bit = UNICODE;
                break;
            case 'y':
                bit = STICKY;
                break;
            case 's':
                bit = DOT_ALL;
                break;
            case 'd':
                bit = HAS_INDICES;
                break;
            default:
                throw new RegExpSyntaxException(flags, "Invalid flags supplied to RegExp constructor '" + flags + "'", i);
            }
            if ((bits & bit) != 0) {
                throw new RegExpSyntaxException(flags, "Invalid flags supplied to RegExp constructor '" + flags + "'", i);
            }
            bits |= bit;
        }
        return bits;
    }

    /**
     * Converts flag bits to the canonical flags text in the order g i m s u y.
     * <p>
     * 플래그 비트를 g i m s u y 순서의 표준 플래그 문자열로 바꾼다.
     *
     * @param bits flag bits / 플래그 비트
     * @return flags text / 플래그 문자열
     */
    public static String toString(int bits) {
        StringBuilder sb = new StringBuilder(7);
        if ((bits & HAS_INDICES) != 0) {
            sb.append('d');
        }
        if ((bits & GLOBAL) != 0) {
            sb.append('g');
        }
        if ((bits & IGNORE_CASE) != 0) {
            sb.append('i');
        }
        if ((bits & MULTILINE) != 0) {
            sb.append('m');
        }
        if ((bits & DOT_ALL) != 0) {
            sb.append('s');
        }
        if ((bits & UNICODE) != 0) {
            sb.append('u');
        }
        if ((bits & STICKY) != 0) {
            sb.append('y');
        }
        return sb.toString();
    }
}
