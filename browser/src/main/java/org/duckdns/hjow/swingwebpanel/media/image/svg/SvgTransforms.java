package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.geom.AffineTransform;
import java.util.Locale;

/**
 * Parses transform lists (plan item P18-04): the SVG <code>transform</code> attribute syntax (<code>matrix</code>, <code>translate</code>,
 * <code>scale</code>, <code>rotate(a [cx cy])</code>, <code>skewX</code>, <code>skewY</code>, separated by white space and commas) and the
 * CSS <code>transform</code> property syntax (units on lengths and angles, <code>translateX</code>, <code>scaleY</code>, <code>skew</code> and
 * so on). Three dimensional functions are not supported and make the value invalid, as 3D acceleration is not available.
 * <p>
 * 변환 목록 해석(계획 P18-04): SVG <code>transform</code> 속성 문법(<code>matrix</code>, <code>translate</code>, <code>scale</code>,
 * <code>rotate(a [cx cy])</code>, <code>skewX</code>, <code>skewY</code>, 공백과 쉼표로 구분)과 CSS <code>transform</code> 속성 문법(길이와 각도의 단위,
 * <code>translateX</code>, <code>scaleY</code>, <code>skew</code> 등). 3차원 함수는 3D 가속이 없으므로 지원하지 않으며 값을 무효로 만든다.
 */
final class SvgTransforms {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private SvgTransforms() {
    }

    /**
     * Parses a transform list.
     * <p>
     * 변환 목록을 해석한다.
     *
     * @param text  the text / 텍스트
     * @param css   true for the CSS syntax (units allowed), false for the attribute syntax / CSS 문법(단위 허용)이면 true, 속성 문법이면 false
     * @param refW  the width that percentages of CSS translations refer to (0 if unknown) / CSS 이동의 백분율이 가리키는 너비(모르면 0)
     * @param refH  the height that percentages refer to (0 if unknown) / 백분율이 가리키는 높이(모르면 0)
     * @return the combined matrix, an identity for an empty list, or null if the text is invalid / 합친 행렬. 빈 목록은 단위 행렬, 잘못된 텍스트는 null
     */
    static AffineTransform parse(String text, boolean css, double refW, double refH) {
        AffineTransform result = new AffineTransform();
        if (text == null) {
            return result;
        }
        String t = text.trim();
        if (t.length() == 0 || "none".equals(t)) {
            return result;
        }
        SvgScanner sc = new SvgScanner(t);
        sc.skipWsAndCommas();
        // 함수를 앞에서부터 하나씩 읽어 왼쪽에서 오른쪽으로 곱한다.
        while (!sc.atEnd()) {
            int st = sc.p;
            // 함수 이름은 영문자로만 이루어진다.
            while (sc.p < t.length() && (Character.isLetter(t.charAt(sc.p)) || Character.isDigit(t.charAt(sc.p)))) {
                sc.p++;
            }
            String fn = t.substring(st, sc.p);
            sc.skipWs();
            if (fn.length() == 0 || !sc.accept('(')) {
                return null;
            }
            int close = t.indexOf(')', sc.p);
            if (close < 0) {
                return null;
            }
            String args = t.substring(sc.p, close);
            sc.p = close + 1;
            AffineTransform f = function(fn, args, css, refW, refH);
            if (f == null) {
                return null;
            }
            result.concatenate(f);
            sc.skipWsAndCommas();
        }
        return result;
    }

    /**
     * Builds the matrix of one transform function.
     * <p>
     * 변환 함수 하나의 행렬을 만든다.
     *
     * @param fn   the function name / 함수 이름
     * @param args the text between the parentheses / 괄호 사이의 텍스트
     * @param css  true for the CSS syntax / CSS 문법이면 true
     * @param refW the reference width for percentages / 백분율의 기준 너비
     * @param refH the reference height for percentages / 백분율의 기준 높이
     * @return the matrix or null if invalid / 행렬. 올바르지 않으면 null
     */
    private static AffineTransform function(String fn, String args, boolean css, double refW, double refH) {
        String name = css ? fn.toLowerCase(Locale.ENGLISH) : fn;
        double[] v = new double[6];
        int n = 0;
        // 인수를 모두 읽는다(CSS 이면 단위가 있는 길이와 각도도 읽는다).
        SvgScanner sc = new SvgScanner(args);
        sc.skipWsAndCommas();
        boolean[] pct = new boolean[6];
        // 인수를 끝까지 읽는다.
        while (!sc.atEnd()) {
            if (n >= 6) {
                return null;
            }
            if (!sc.number()) {
                return null;
            }
            double x = sc.value;
            if (css) {
                String unit = sc.letters().toLowerCase(Locale.ENGLISH);
                boolean angle = name.startsWith("rotate") || name.startsWith("skew");
                if (angle) {
                    x = angleToDegrees(x, unit);
                    if (Double.isNaN(x)) {
                        return null;
                    }
                } else if (name.startsWith("translate")) {
                    SvgLength len = new SvgLength(x, unit);
                    if (!isKnownLength(unit)) {
                        return null;
                    }
                    pct[n] = len.isPercent();
                    x = len.toPx(16, 0, 0, 0);
                    if (pct[n]) {
                        x = len.value;
                    }
                } else if (unit.length() > 0) {
                    return null;
                }
            }
            v[n++] = x;
            sc.skipWsAndCommas();
        }
        if ("matrix".equals(name) && n == 6) {
            return new AffineTransform(v[0], v[1], v[2], v[3], v[4], v[5]);
        }
        if ("translate".equals(name) && (n == 1 || n == 2)) {
            double tx = pct[0] ? v[0] * refW / 100 : v[0];
            double ty = n == 2 ? (pct[1] ? v[1] * refH / 100 : v[1]) : 0;
            return AffineTransform.getTranslateInstance(tx, ty);
        }
        if (css && "translatex".equals(name) && n == 1) {
            return AffineTransform.getTranslateInstance(pct[0] ? v[0] * refW / 100 : v[0], 0);
        }
        if (css && "translatey".equals(name) && n == 1) {
            return AffineTransform.getTranslateInstance(0, pct[0] ? v[0] * refH / 100 : v[0]);
        }
        if ("scale".equals(name) && (n == 1 || n == 2)) {
            return AffineTransform.getScaleInstance(v[0], n == 2 ? v[1] : v[0]);
        }
        if (css && "scalex".equals(name) && n == 1) {
            return AffineTransform.getScaleInstance(v[0], 1);
        }
        if (css && "scaley".equals(name) && n == 1) {
            return AffineTransform.getScaleInstance(1, v[0]);
        }
        if ("rotate".equals(name) && (n == 1 || (!css && n == 3))) {
            double rad = Math.toRadians(v[0]);
            if (n == 3) {
                return AffineTransform.getRotateInstance(rad, v[1], v[2]);
            }
            return AffineTransform.getRotateInstance(rad);
        }
        if ("skewx".equals(name.toLowerCase(Locale.ENGLISH)) && n == 1 && (css || "skewX".equals(name))) {
            return new AffineTransform(1, 0, Math.tan(Math.toRadians(v[0])), 1, 0, 0);
        }
        if ("skewy".equals(name.toLowerCase(Locale.ENGLISH)) && n == 1 && (css || "skewY".equals(name))) {
            return new AffineTransform(1, Math.tan(Math.toRadians(v[0])), 0, 1, 0, 0);
        }
        if (css && "skew".equals(name) && (n == 1 || n == 2)) {
            return new AffineTransform(1, n == 2 ? Math.tan(Math.toRadians(v[1])) : 0, Math.tan(Math.toRadians(v[0])), 1, 0, 0);
        }
        return null;
    }

    /**
     * Tells whether a unit text is allowed on a translation length.
     * <p>
     * 단위 텍스트가 이동 길이에 허용되는지 알려 준다.
     *
     * @param unit unit text in lower case / 소문자 단위 텍스트
     * @return true if allowed / 허용되면 true
     */
    private static boolean isKnownLength(String unit) {
        return SvgLength.parse("1" + unit) != null;
    }

    /**
     * Converts an angle with a CSS unit to degrees.
     * <p>
     * CSS 단위가 붙은 각도를 도(degree)로 바꾼다.
     *
     * @param x    the number / 수
     * @param unit the unit text (<code>deg</code>, <code>rad</code>, <code>grad</code>, <code>turn</code> or empty) / 단위 텍스트
     * @return degrees, or NaN for an unknown unit / 도. 알 수 없는 단위이면 NaN
     */
    static double angleToDegrees(double x, String unit) {
        if (unit.length() == 0 || "deg".equals(unit)) {
            return x;
        }
        if ("rad".equals(unit)) {
            return Math.toDegrees(x);
        }
        if ("grad".equals(unit)) {
            return x * 0.9;
        }
        if ("turn".equals(unit)) {
            return x * 360;
        }
        return Double.NaN;
    }
}
