package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Parses the string form of a matrix, a CSS <code>transform</code> list, into a {@link DomMatrix} (section 6.2 of the Geometry Interfaces
 * Module, "parse a string into an abstract matrix"; used by <code>new DOMMatrix(string)</code> and <code>setMatrixValue()</code>). The grammar
 * is that of the CSS <code>transform</code> property: <code>none</code>, or a whitespace separated list of the functions
 * <code>matrix</code>, <code>matrix3d</code>, <code>translate</code>, <code>translateX/Y/Z</code>, <code>translate3d</code>, <code>scale</code>,
 * <code>scaleX/Y/Z</code>, <code>scale3d</code>, <code>rotate</code>, <code>rotateX/Y/Z</code>, <code>rotate3d</code>, <code>skew</code>,
 * <code>skewX/Y</code> and <code>perspective</code> with comma separated arguments. Lengths must have an absolute unit (<code>px</code>,
 * <code>cm</code>, <code>mm</code>, <code>q</code>, <code>in</code>, <code>pt</code>, <code>pc</code>) or be a plain 0; relative units and
 * <code>calc()</code> make the string invalid, as the specification demands for units that need a context. Angles are <code>deg</code>,
 * <code>grad</code>, <code>rad</code>, <code>turn</code> or a plain 0. The result is 2D when only the 2D functions are used; the 3D functions
 * (including <code>rotateZ()</code>, which the module counts as 3D) produce a 3D matrix. The list is multiplied left to right.
 * <p>
 * 행렬의 문자열 형태인 CSS <code>transform</code> 목록을 {@link DomMatrix} 로 해석한다(Geometry Interfaces Module 6.2 절 "parse a string into an abstract
 * matrix", <code>new DOMMatrix(문자열)</code> 과 <code>setMatrixValue()</code> 가 쓴다). 문법은 CSS <code>transform</code> 속성과 같다:
 * <code>none</code> 이거나, 쉼표로 구분한 인수를 가진 함수 <code>matrix</code>, <code>matrix3d</code>, <code>translate</code>, <code>translateX/Y/Z</code>,
 * <code>translate3d</code>, <code>scale</code>, <code>scaleX/Y/Z</code>, <code>scale3d</code>, <code>rotate</code>, <code>rotateX/Y/Z</code>,
 * <code>rotate3d</code>, <code>skew</code>, <code>skewX/Y</code>, <code>perspective</code> 를 공백으로 구분한 목록이다. 길이는 절대 단위(<code>px</code>,
 * <code>cm</code>, <code>mm</code>, <code>q</code>, <code>in</code>, <code>pt</code>, <code>pc</code>)이거나 단위 없는 0 이어야 한다. 문맥이 필요한 상대 단위와
 * <code>calc()</code> 는 규격이 요구하듯 문자열을 무효로 만든다. 각도는 <code>deg</code>, <code>grad</code>, <code>rad</code>, <code>turn</code> 이거나 단위 없는 0
 * 이다. 2D 함수만 쓰면 결과는 2D 이고, 3D 함수(모듈이 3D 로 세는 <code>rotateZ()</code> 포함)를 쓰면 3D 행렬이다. 목록은 왼쪽에서 오른쪽으로 곱한다.
 */
public final class DomMatrixParser {

    /**
     * The kind of a number without a unit.
     * <p>
     * 단위가 없는 숫자의 종류.
     */
    private static final int KIND_NUMBER = 0;

    /**
     * The kind of a number with a length unit (the value is in pixels).
     * <p>
     * 길이 단위가 붙은 숫자의 종류(값은 픽셀).
     */
    private static final int KIND_LENGTH = 1;

    /**
     * The kind of a number with an angle unit (the value is in degrees).
     * <p>
     * 각도 단위가 붙은 숫자의 종류(값은 도).
     */
    private static final int KIND_ANGLE = 2;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private DomMatrixParser() {
    }

    /**
     * Parses a transform list.
     * <p>
     * 변환 목록을 해석한다.
     *
     * @param text the string, not null / 문자열(null 불가)
     * @return the matrix, or null if the string is not a valid transform list with absolute units / 행렬. 절대 단위의 올바른 변환 목록이 아니면 null
     */
    public static DomMatrix parse(String text) {
        String s = text.trim();
        DomMatrix result = new DomMatrix();
        if (s.length() == 0 || "none".equalsIgnoreCase(s)) {
            return result;
        }
        boolean threeD = false;
        int i = 0;
        int n = s.length();
        // 함수를 하나씩 읽어 왼쪽에서 오른쪽으로 곱한다.
        while (i < n) {
            char ch = s.charAt(i);
            if (Character.isWhitespace(ch)) {
                i++;
                continue;
            }
            int nameStart = i;
            // 함수 이름(글자와 숫자)을 읽는다.
            while (i < n && (Character.isLetterOrDigit(s.charAt(i)))) {
                i++;
            }
            if (i == nameStart || i >= n || s.charAt(i) != '(') {
                return null;
            }
            String name = s.substring(nameStart, i).toLowerCase();
            int close = s.indexOf(')', i);
            if (close < 0) {
                return null;
            }
            String body = s.substring(i + 1, close);
            // 중첩 괄호(calc 등)는 지원하지 않는다.
            if (body.indexOf('(') >= 0) {
                return null;
            }
            i = close + 1;
            // 닫는 괄호 바로 뒤에는 공백이나 끝이 와야 한다.
            if (i < n && !Character.isWhitespace(s.charAt(i))) {
                return null;
            }
            int status = apply(result, name, body);
            if (status < 0) {
                return null;
            }
            threeD |= status == 1;
        }
        if (threeD) {
            result.twoD = false;
        }
        return result;
    }

    /**
     * Applies one function to the result.
     * <p>
     * 함수 하나를 결과에 적용한다.
     *
     * @param m    the matrix to multiply / 곱할 행렬
     * @param name the lower case function name / 소문자 함수 이름
     * @param body the text between the parentheses / 괄호 사이의 문자열
     * @return -1 for an invalid function, 0 for a 2D function, 1 for a 3D function / 잘못된 함수면 -1, 2D 함수면 0, 3D 함수면 1
     */
    private static int apply(DomMatrix m, String name, String body) {
        String[] parts = body.split(",", -1);
        double[] value = new double[parts.length];
        int[] kind = new int[parts.length];
        // 인수를 하나씩 숫자와 종류로 읽는다.
        for (int k = 0; k < parts.length; k++) {
            double[] d = dimension(parts[k].trim());
            if (d == null) {
                return -1;
            }
            value[k] = d[0];
            kind[k] = (int) d[1];
        }
        int count = parts.length;
        if ("matrix".equals(name)) {
            if (count != 6 || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.multiplySelf(new DomMatrixReadOnly(value[0], value[1], value[2], value[3], value[4], value[5]));
            return 0;
        } else if ("matrix3d".equals(name)) {
            if (count != 16 || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.multiplySelf(new DomMatrixReadOnly(value));
            return 1;
        } else if ("translate".equals(name)) {
            if ((count != 1 && count != 2) || !lengths(value, kind)) {
                return -1;
            }
            m.translateSelf(value[0], count == 2 ? value[1] : 0, 0);
            return 0;
        } else if ("translatex".equals(name)) {
            if (count != 1 || !lengths(value, kind)) {
                return -1;
            }
            m.translateSelf(value[0], 0, 0);
            return 0;
        } else if ("translatey".equals(name)) {
            if (count != 1 || !lengths(value, kind)) {
                return -1;
            }
            m.translateSelf(0, value[0], 0);
            return 0;
        } else if ("translatez".equals(name)) {
            if (count != 1 || !lengths(value, kind)) {
                return -1;
            }
            m.translateSelf(0, 0, value[0]);
            return 1;
        } else if ("translate3d".equals(name)) {
            if (count != 3 || !lengths(value, kind)) {
                return -1;
            }
            m.translateSelf(value[0], value[1], value[2]);
            return 1;
        } else if ("scale".equals(name)) {
            if ((count != 1 && count != 2) || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.scaleSelf(value[0], count == 2 ? value[1] : value[0], 1, 0, 0, 0);
            return 0;
        } else if ("scalex".equals(name)) {
            if (count != 1 || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.scaleSelf(value[0], 1, 1, 0, 0, 0);
            return 0;
        } else if ("scaley".equals(name)) {
            if (count != 1 || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.scaleSelf(1, value[0], 1, 0, 0, 0);
            return 0;
        } else if ("scalez".equals(name)) {
            if (count != 1 || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.scaleSelf(1, 1, value[0], 0, 0, 0);
            return 1;
        } else if ("scale3d".equals(name)) {
            if (count != 3 || !all(kind, KIND_NUMBER)) {
                return -1;
            }
            m.scaleSelf(value[0], value[1], value[2], 0, 0, 0);
            return 1;
        }
        return applyRotation(m, name, value, kind);
    }

    /**
     * Applies a rotation, skew or perspective function (the second half of {@link #apply}).
     * <p>
     * 회전, 기울이기, 원근 함수를 적용한다({@link #apply} 의 나머지 절반).
     *
     * @param m     the matrix to multiply / 곱할 행렬
     * @param name  the lower case function name / 소문자 함수 이름
     * @param value the argument values / 인수 값
     * @param kind  the argument kinds / 인수 종류
     * @return -1 for an invalid function, 0 for a 2D function, 1 for a 3D function / 잘못된 함수면 -1, 2D 함수면 0, 3D 함수면 1
     */
    private static int applyRotation(DomMatrix m, String name, double[] value, int[] kind) {
        int count = value.length;
        if ("rotate".equals(name)) {
            if (count != 1 || !angles(value, kind)) {
                return -1;
            }
            m.rotateSelf(0, 0, value[0]);
            return 0;
        } else if ("rotatez".equals(name)) {
            if (count != 1 || !angles(value, kind)) {
                return -1;
            }
            m.rotateSelf(0, 0, value[0]);
            return 1;
        } else if ("rotatex".equals(name)) {
            if (count != 1 || !angles(value, kind)) {
                return -1;
            }
            m.rotateSelf(value[0], 0, 0);
            return 1;
        } else if ("rotatey".equals(name)) {
            if (count != 1 || !angles(value, kind)) {
                return -1;
            }
            m.rotateSelf(0, value[0], 0);
            return 1;
        } else if ("rotate3d".equals(name)) {
            if (count != 4 || kind[0] != KIND_NUMBER || kind[1] != KIND_NUMBER || kind[2] != KIND_NUMBER || !angle(value[3], kind[3])) {
                return -1;
            }
            m.rotateAxisAngleSelf(value[0], value[1], value[2], value[3]);
            return 1;
        } else if ("skew".equals(name)) {
            if ((count != 1 && count != 2) || !angles(value, kind)) {
                return -1;
            }
            // skew(ax, ay) 는 한 번에 곱하는 행렬 (1, tan(ay), tan(ax), 1) 이다.
            double[] k = { 1, Math.tan(count == 2 ? value[1] * Math.PI / 180.0 : 0), Math.tan(value[0] * Math.PI / 180.0), 1, 0, 0 };
            m.multiplySelf(new DomMatrixReadOnly(k[0], k[1], k[2], k[3], k[4], k[5]));
            return 0;
        } else if ("skewx".equals(name)) {
            if (count != 1 || !angles(value, kind)) {
                return -1;
            }
            m.skewXSelf(value[0]);
            return 0;
        } else if ("skewy".equals(name)) {
            if (count != 1 || !angles(value, kind)) {
                return -1;
            }
            m.skewYSelf(value[0]);
            return 0;
        } else if ("perspective".equals(name)) {
            if (count != 1 || !lengths(value, kind) || value[0] < 0) {
                return -1;
            }
            double[] p = new double[16];
            p[0] = 1;
            p[5] = 1;
            p[10] = 1;
            p[15] = 1;
            // 1px 보다 작은 거리는 1px 로 올린다.
            p[11] = -1.0 / Math.max(1.0, value[0]);
            m.multiplySelf(new DomMatrixReadOnly(p));
            return 1;
        }
        return -1;
    }

    /**
     * Tells whether every argument has a kind.
     * <p>
     * 모든 인수가 어떤 종류인지 알려 준다.
     *
     * @param kind the kinds / 종류들
     * @param want the wanted kind / 원하는 종류
     * @return true if all have it / 모두 그 종류이면 true
     */
    private static boolean all(int[] kind, int want) {
        // 인수의 종류를 하나씩 확인한다.
        for (int i = 0; i < kind.length; i++) {
            if (kind[i] != want) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether every argument is a length: a length with an absolute unit or a plain zero.
     * <p>
     * 모든 인수가 길이인지 알려 준다: 절대 단위의 길이이거나 단위 없는 0.
     *
     * @param value the values / 값들
     * @param kind  the kinds / 종류들
     * @return true if all are lengths / 모두 길이이면 true
     */
    private static boolean lengths(double[] value, int[] kind) {
        // 길이이거나 단위 없는 0 인지 하나씩 확인한다.
        for (int i = 0; i < kind.length; i++) {
            if (kind[i] != KIND_LENGTH && !(kind[i] == KIND_NUMBER && value[i] == 0)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether every argument is an angle.
     * <p>
     * 모든 인수가 각도인지 알려 준다.
     *
     * @param value the values / 값들
     * @param kind  the kinds / 종류들
     * @return true if all are angles / 모두 각도이면 true
     */
    private static boolean angles(double[] value, int[] kind) {
        // 각도이거나 단위 없는 0 인지 하나씩 확인한다.
        for (int i = 0; i < kind.length; i++) {
            if (!angle(value[i], kind[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether one argument is an angle: with an angle unit, or a plain zero.
     * <p>
     * 인수 하나가 각도인지 알려 준다: 각도 단위가 붙었거나 단위 없는 0.
     *
     * @param value the value / 값
     * @param kind  the kind / 종류
     * @return true for an angle / 각도이면 true
     */
    private static boolean angle(double value, int kind) {
        return kind == KIND_ANGLE || (kind == KIND_NUMBER && value == 0);
    }

    /**
     * Reads a number with an optional unit.
     * <p>
     * 단위가 붙을 수 있는 숫자를 읽는다.
     *
     * @param s the text, trimmed / 문자열(앞뒤 공백 제거됨)
     * @return {value, kind} where the value of a length is in pixels and that of an angle in degrees, or null / {값, 종류}(길이는 픽셀, 각도는 도). 잘못되면 null
     */
    private static double[] dimension(String s) {
        int n = s.length();
        int i = 0;
        if (i < n && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            i++;
        }
        int digits = 0;
        // 정수 부분의 숫자를 읽는다.
        while (i < n && Character.isDigit(s.charAt(i)) && s.charAt(i) < 128) {
            i++;
            digits++;
        }
        if (i < n && s.charAt(i) == '.') {
            i++;
            // 소수 부분의 숫자를 읽는다.
            while (i < n && Character.isDigit(s.charAt(i)) && s.charAt(i) < 128) {
                i++;
                digits++;
            }
        }
        if (digits == 0) {
            return null;
        }
        // 지수가 있으면 읽는다(e 뒤에 숫자가 없으면 단위로 본다).
        if (i < n && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
            int j = i + 1;
            if (j < n && (s.charAt(j) == '+' || s.charAt(j) == '-')) {
                j++;
            }
            int expDigits = 0;
            // 지수의 숫자를 읽는다.
            while (j < n && s.charAt(j) >= '0' && s.charAt(j) <= '9') {
                j++;
                expDigits++;
            }
            if (expDigits > 0) {
                i = j;
            }
        }
        double number;
        try {
            number = Double.parseDouble(s.substring(0, i));
        } catch (NumberFormatException e) {
            return null;
        }
        String unit = s.substring(i).toLowerCase();
        if (unit.length() == 0) {
            return new double[] { number, KIND_NUMBER };
        }
        double px = lengthUnit(unit);
        if (px > 0) {
            return new double[] { number * px, KIND_LENGTH };
        }
        double deg = angleUnit(unit);
        if (deg > 0) {
            return new double[] { number * deg, KIND_ANGLE };
        }
        return null;
    }

    /**
     * Returns the size in pixels of an absolute length unit.
     * <p>
     * 절대 길이 단위의 픽셀 크기를 반환한다.
     *
     * @param unit the lower case unit / 소문자 단위
     * @return the pixels per unit, or 0 for an unknown or relative unit / 단위당 픽셀. 모르거나 상대 단위이면 0
     */
    private static double lengthUnit(String unit) {
        if ("px".equals(unit)) {
            return 1;
        } else if ("in".equals(unit)) {
            return 96;
        } else if ("cm".equals(unit)) {
            return 96 / 2.54;
        } else if ("mm".equals(unit)) {
            return 96 / 25.4;
        } else if ("q".equals(unit)) {
            return 96 / 101.6;
        } else if ("pt".equals(unit)) {
            return 96 / 72.0;
        } else if ("pc".equals(unit)) {
            return 16;
        }
        return 0;
    }

    /**
     * Returns the size in degrees of an angle unit.
     * <p>
     * 각도 단위의 도 크기를 반환한다.
     *
     * @param unit the lower case unit / 소문자 단위
     * @return the degrees per unit, or 0 for an unknown unit / 단위당 도. 모르는 단위이면 0
     */
    private static double angleUnit(String unit) {
        if ("deg".equals(unit)) {
            return 1;
        } else if ("grad".equals(unit)) {
            return 0.9;
        } else if ("rad".equals(unit)) {
            return 180.0 / Math.PI;
        } else if ("turn".equals(unit)) {
            return 360;
        }
        return 0;
    }
}
