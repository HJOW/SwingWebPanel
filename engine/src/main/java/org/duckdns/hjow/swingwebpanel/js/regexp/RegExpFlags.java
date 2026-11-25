package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * Bit constants and parsing of regular expression flags. The matcher itself only uses {@link #IGNORE_CASE},
 * {@link #MULTILINE}, {@link #UNICODE}, {@link #UNICODE_SETS} and {@link #DOT_ALL}; {@link #GLOBAL} and {@link #STICKY} are carried for the
 * caller (<code>RegExp.prototype.exec</code> decides where matching starts and whether it may scan forward).
 * {@link #DOT_ALL} (flag s) and {@link #HAS_INDICES} (flag d) are later extensions and are accepted as such.
 * <p>
 * 정규식 플래그의 비트 상수와 해석. 일치 검색기 자체는 {@link #IGNORE_CASE}, {@link #MULTILINE}, {@link #UNICODE}, {@link #UNICODE_SETS}, {@link #DOT_ALL}을 쓰고
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

    /** Flag v for Unicode Sets matching. / Unicode Sets 일치를 지정하는 v 플래그. */
    public static final int UNICODE_SETS = 128;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private RegExpFlags() {
    }

    /**
     * Parses flags such as "gi". Every flag may appear once and u and v are mutually exclusive.
     * <p>
     * "gi" 같은 플래그 문자열을 해석한다. 각 플래그는 최대 한 번 나오며 u와 v는 함께 쓸 수 없다.
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
            case 'v':
                bit = UNICODE_SETS;
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
            // u와 v 중 하나를 읽은 뒤에는 나머지 플래그를 추가할 수 없다.
            if ((bits & bit) != 0 || (isUnicodeMode(bit) && isUnicodeMode(bits))) {
                throw new RegExpSyntaxException(flags, "Invalid flags supplied to RegExp constructor '" + flags + "'", i);
            }
            bits |= bit;
        }
        return bits;
    }

    /**
     * Reports whether matching uses whole Unicode code points.
     * <p>
     * 검색이 전체 유니코드 코드 포인트를 사용하는지 알려 준다.
     *
     * @param bits flag bits / 플래그 비트
     * @return true for either Unicode mode / 유니코드 모드 중 하나면 true
     */
    public static boolean isUnicodeMode(int bits) {
        return (bits & (UNICODE | UNICODE_SETS)) != 0;
    }

    /**
     * Converts flag bits to the canonical flags text in the order d g i m s u v y.
     * <p>
     * 플래그 비트를 d g i m s u v y 순서의 표준 플래그 문자열로 바꾼다.
     *
     * @param bits flag bits / 플래그 비트
     * @return flags text / 플래그 문자열
     */
    public static String toString(int bits) {
        StringBuilder sb = new StringBuilder(8);
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
        if ((bits & UNICODE_SETS) != 0) {
            sb.append('v');
        }
        if ((bits & STICKY) != 0) {
            sb.append('y');
        }
        return sb.toString();
    }
}
