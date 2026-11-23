package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.HashMap;
import java.util.Map;

/**
 * The unit tables of CSS Values and Units: which unit names are lengths, angles, times, frequencies, resolutions or flex fractions, and the
 * conversion of the absolute units to their canonical unit (pixels, degrees, seconds, hertz, dots per pixel).
 * <p>
 * CSS Values and Units 의 단위 표: 어떤 단위 이름이 길이, 각도, 시간, 주파수, 해상도, flex 비율인지, 그리고 절대 단위를 기준 단위(픽셀, 도, 초, 헤르츠,
 * 픽셀당 점)로 바꾸는 변환.
 */
public final class CssUnits {

    /** Kind: not a known unit. / 종류: 알 수 없는 단위. */
    public static final int UNKNOWN = 0;
    /** Kind: absolute length (px, cm, mm, q, in, pt, pc). / 종류: 절대 길이. */
    public static final int ABSOLUTE_LENGTH = 1;
    /** Kind: font relative length (em, ex, ch, rem, lh, rlh, cap, ic, rex, rch, ...). / 종류: 글꼴 상대 길이. */
    public static final int FONT_RELATIVE_LENGTH = 2;
    /** Kind: viewport relative length (vw, vh, vmin, vmax, vi, vb and the s/l/d variants). / 종류: 뷰포트 상대 길이. */
    public static final int VIEWPORT_LENGTH = 3;
    /** Kind: angle. / 종류: 각도. */
    public static final int ANGLE = 4;
    /** Kind: time. / 종류: 시간. */
    public static final int TIME = 5;
    /** Kind: frequency. / 종류: 주파수. */
    public static final int FREQUENCY = 6;
    /** Kind: resolution. / 종류: 해상도. */
    public static final int RESOLUTION = 7;
    /** Kind: flexible length (fr). / 종류: 유연한 길이(fr). */
    public static final int FLEX = 8;
    /** Kind: container query length (cqw, cqh, cqi, cqb, cqmin, cqmax). / 종류: 컨테이너 질의 길이(cqw, cqh, cqi, cqb, cqmin, cqmax). */
    public static final int CONTAINER_LENGTH = 9;

    /** Unit name to kind. / 단위 이름에서 종류로. */
    private static final Map<String, Integer> KINDS = new HashMap<String, Integer>();

    static {
        String[] abs = {"px", "cm", "mm", "q", "in", "pt", "pc"};
        String[] font = {"em", "ex", "ch", "rem", "lh", "rlh", "cap", "ic", "rex", "rch", "rcap", "ric"};
        String[] vp = {"vw", "vh", "vmin", "vmax", "vi", "vb", "svw", "svh", "svmin", "svmax", "lvw", "lvh", "lvmin", "lvmax", "dvw", "dvh",
            "dvmin", "dvmax"};
        String[] ang = {"deg", "grad", "rad", "turn"};
        String[] time = {"s", "ms"};
        String[] freq = {"hz", "khz"};
        String[] res = {"dpi", "dpcm", "dppx", "x"};
        register(abs, ABSOLUTE_LENGTH);
        register(font, FONT_RELATIVE_LENGTH);
        register(vp, VIEWPORT_LENGTH);
        register(ang, ANGLE);
        register(time, TIME);
        register(freq, FREQUENCY);
        register(res, RESOLUTION);
        register(new String[] {"cqw", "cqh", "cqi", "cqb", "cqmin", "cqmax"}, CONTAINER_LENGTH);
        KINDS.put("fr", Integer.valueOf(FLEX));
    }

    /**
     * Registers unit names of one kind.
     * <p>
     * 한 종류의 단위 이름들을 등록한다.
     *
     * @param names unit names / 단위 이름
     * @param kind  kind constant / 종류 상수
     */
    private static void register(String[] names, int kind) {
        // 단위 이름마다 종류를 표에 넣는다.
        for (int i = 0; i < names.length; i++) {
            KINDS.put(names[i], Integer.valueOf(kind));
        }
    }

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private CssUnits() {
    }

    /**
     * Returns the kind of a unit (the name is matched ASCII case-insensitively).
     * <p>
     * 단위의 종류를 반환한다(이름은 ASCII 대소문자를 무시하고 비교).
     *
     * @param unit unit name / 단위 이름
     * @return kind constant / 종류 상수
     */
    public static int kindOf(String unit) {
        Integer k = KINDS.get(CssKeyword.lower(unit));
        return k == null ? UNKNOWN : k.intValue();
    }

    /**
     * Tells whether a unit is a length unit of any family.
     * <p>
     * 단위가 어느 계열이든 길이 단위인지 알려 준다.
     *
     * @param unit unit name / 단위 이름
     * @return true for lengths / 길이이면 true
     */
    public static boolean isLength(String unit) {
        int k = kindOf(unit);
        return k == ABSOLUTE_LENGTH || k == FONT_RELATIVE_LENGTH || k == VIEWPORT_LENGTH || k == CONTAINER_LENGTH;
    }

    /**
     * Converts an absolute length to pixels.
     * <p>
     * 절대 길이를 픽셀로 바꾼다.
     *
     * @param value number / 숫자
     * @param unit  lower-case absolute unit / 소문자 절대 단위
     * @return pixels, or NaN if the unit is not absolute / 픽셀. 절대 단위가 아니면 NaN
     */
    public static double absoluteToPx(double value, String unit) {
        if (unit.equals("px")) {
            return value;
        }
        if (unit.equals("in")) {
            return value * 96.0;
        }
        if (unit.equals("cm")) {
            return value * 96.0 / 2.54;
        }
        if (unit.equals("mm")) {
            return value * 96.0 / 25.4;
        }
        if (unit.equals("q")) {
            return value * 96.0 / 101.6;
        }
        if (unit.equals("pt")) {
            return value * 96.0 / 72.0;
        }
        if (unit.equals("pc")) {
            return value * 16.0;
        }
        return Double.NaN;
    }

    /**
     * Converts an angle to degrees.
     * <p>
     * 각도를 도로 바꾼다.
     *
     * @param value number / 숫자
     * @param unit  lower-case angle unit / 소문자 각도 단위
     * @return degrees / 도
     */
    public static double angleToDegrees(double value, String unit) {
        if (unit.equals("rad")) {
            return value * 180.0 / Math.PI;
        }
        if (unit.equals("grad")) {
            return value * 0.9;
        }
        if (unit.equals("turn")) {
            return value * 360.0;
        }
        return value;
    }

    /**
     * Converts a time to seconds.
     * <p>
     * 시간을 초로 바꾼다.
     *
     * @param value number / 숫자
     * @param unit  lower-case time unit / 소문자 시간 단위
     * @return seconds / 초
     */
    public static double timeToSeconds(double value, String unit) {
        return unit.equals("ms") ? value / 1000.0 : value;
    }

    /**
     * Converts a resolution to dots per pixel.
     * <p>
     * 해상도를 픽셀당 점 수로 바꾼다.
     *
     * @param value number / 숫자
     * @param unit  lower-case resolution unit / 소문자 해상도 단위
     * @return dppx / dppx
     */
    public static double resolutionToDppx(double value, String unit) {
        if (unit.equals("dpi")) {
            return value / 96.0;
        }
        if (unit.equals("dpcm")) {
            return value * 2.54 / 96.0;
        }
        return value;
    }
}
