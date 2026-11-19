package org.duckdns.hjow.swingwebpanel.css.values;

import java.util.ArrayList;
import java.util.List;

/**
 * Interpolation of offset paths (plan item P10-08-h, CSS Motion Path 1 section 2.1, "Animation type: by computed value", with the rules of CSS Shapes 1 for
 * <code>&lt;basic-shape&gt;</code>): two shape functions of the same kind with the same structure interpolate their numbers (lengths with lengths,
 * percentages with percentages, angles, numbers; keywords and the reference box must be equal), two <code>path()</code> values with the same commands
 * interpolate their coordinates (an arc keeps its flags only if they are equal), and two <code>ray()</code> values interpolate their angle and position
 * when the size and <code>contain</code> are equal. Anything else (another function, another reference box, a <code>url()</code>, a length mixed with a
 * percentage) is not interpolated, and the animation is discrete.
 * <p>
 * 오프셋 경로의 보간(계획 P10-08-h, CSS Motion Path 1 2.1 절의 "계산 값으로", <code>&lt;basic-shape&gt;</code> 에 대한 CSS Shapes 1 의 규칙): 같은 종류이고 구조가 같은 두 도형 함수는
 * 수를 보간하고(길이는 길이와, 백분율은 백분율과, 각도, 수. 키워드와 기준 상자는 같아야 한다), 명령이 같은 두 <code>path()</code> 는 좌표를 보간하며(호는 깃발이 같을 때만),
 * 크기와 <code>contain</code> 이 같은 두 <code>ray()</code> 는 각도와 위치를 보간한다. 그 밖의 경우(다른 함수, 다른 기준 상자, <code>url()</code>, 길이와 백분율이 섞임)는 보간하지
 * 않으며 애니메이션은 이산이다.
 */
public final class ShapeInterpolation {

    /**
     * Not instantiable; the class only offers static methods.
     * <p>
     * 인스턴스를 만들지 않는다. 정적 메서드만 제공한다.
     */
    private ShapeInterpolation() {
    }

    /**
     * Interpolates two computed values of <code>offset-path</code>.
     * <p>
     * <code>offset-path</code> 의 계산 값 둘을 보간한다.
     *
     * @param from the start / 시작
     * @param to   the end / 끝
     * @param p    the progress / 진행도
     * @return the value, or null if the values cannot be interpolated / 값. 보간할 수 없으면 null
     */
    public static CssValue interpolate(CssValue from, CssValue to, double p) {
        if (from == null || to == null) {
            return null;
        }
        return mix(from, to, p, 0);
    }

    /**
     * Interpolates two values of the same structure.
     * <p>
     * 구조가 같은 두 값을 보간한다.
     *
     * @param a     the start / 시작
     * @param b     the end / 끝
     * @param p     the progress / 진행도
     * @param depth the nesting depth / 중첩 깊이
     * @return the value or null / 값 또는 null
     */
    private static CssValue mix(CssValue a, CssValue b, double p, int depth) {
        if (depth > 8) {
            return null;
        }
        if (a instanceof CssLength && b instanceof CssLength) {
            double x = ((CssLength) a).isPx() ? ((CssLength) a).getValue() : ((CssLength) a).absolutePx();
            double y = ((CssLength) b).isPx() ? ((CssLength) b).getValue() : ((CssLength) b).absolutePx();
            return Double.isNaN(x) || Double.isNaN(y) ? null : CssLength.px(x + (y - x) * p);
        }
        if (a instanceof CssPercentage && b instanceof CssPercentage) {
            double x = ((CssPercentage) a).getValue();
            return new CssPercentage(x + (((CssPercentage) b).getValue() - x) * p);
        }
        if (a instanceof CssNumber && b instanceof CssNumber) {
            double x = ((CssNumber) a).getValue();
            return new CssNumber(x + (((CssNumber) b).getValue() - x) * p, false);
        }
        if (a instanceof CssDimension && b instanceof CssDimension && ((CssDimension) a).getKind() == CssUnits.ANGLE && ((CssDimension) b).getKind() == CssUnits.ANGLE) {
            double x = ((CssDimension) a).canonical();
            return new CssDimension(x + (((CssDimension) b).canonical() - x) * p, "deg");
        }
        if (a instanceof CssList && b instanceof CssList) {
            List<CssValue> x = ((CssList) a).getItems();
            List<CssValue> y = ((CssList) b).getItems();
            if (x.size() != y.size() || ((CssList) a).isComma() != ((CssList) b).isComma()) {
                return null;
            }
            List<CssValue> out = new ArrayList<CssValue>();
            // 항목 쌍마다 보간한다(같은 항목은 그대로).
            for (int i = 0; i < x.size(); i++) {
                CssValue v = x.get(i).equals(y.get(i)) ? x.get(i) : mix(x.get(i), y.get(i), p, depth + 1);
                if (v == null) {
                    return null;
                }
                out.add(v);
            }
            return new CssList(out, ((CssList) a).isComma());
        }
        if (a instanceof CssFunctionValue && b instanceof CssFunctionValue) {
            CssFunctionValue f = (CssFunctionValue) a;
            CssFunctionValue g = (CssFunctionValue) b;
            if (!f.getName().equalsIgnoreCase(g.getName())) {
                return null;
            }
            if (f.isNamed("path")) {
                return path(f, g, p);
            }
            if (f.isNamed("url")) {
                return null;
            }
            List<CssValue> x = f.getArguments();
            List<CssValue> y = g.getArguments();
            if (x.size() != y.size()) {
                return null;
            }
            List<CssValue> out = new ArrayList<CssValue>();
            // 인수 쌍마다 보간한다(같은 인수는 그대로).
            for (int i = 0; i < x.size(); i++) {
                CssValue v = x.get(i).equals(y.get(i)) ? x.get(i) : mix(x.get(i), y.get(i), p, depth + 1);
                if (v == null) {
                    return null;
                }
                out.add(v);
            }
            return new CssFunctionValue(f.getName(), out);
        }
        return a.equals(b) ? a : null;
    }

    /**
     * Interpolates two <code>path()</code> values whose path data have the same commands (the same letters in the same order, with the same fill rule).
     * <p>
     * 경로 데이터의 명령이 같은(같은 글자가 같은 순서로, 같은 채우기 규칙) 두 <code>path()</code> 값을 보간한다.
     *
     * @param f the start / 시작
     * @param g the end / 끝
     * @param p the progress / 진행도
     * @return the value or null / 값 또는 null
     */
    private static CssValue path(CssFunctionValue f, CssFunctionValue g, double p) {
        List<CssValue> x = f.getArguments();
        List<CssValue> y = g.getArguments();
        if (x.size() != y.size() || x.isEmpty() || !(x.get(x.size() - 1) instanceof CssString) || !(y.get(y.size() - 1) instanceof CssString)) {
            return null;
        }
        if (x.size() == 2 && !x.get(0).equals(y.get(0))) {
            return null;
        }
        List<Object> a = tokens(((CssString) x.get(x.size() - 1)).getValue());
        List<Object> b = tokens(((CssString) y.get(y.size() - 1)).getValue());
        if (a == null || b == null || a.size() != b.size()) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        char command = 0;
        int index = 0;
        // 토큰 쌍마다 명령 글자는 같아야 하고 수는 보간한다(호의 깃발은 같아야 한다).
        for (int i = 0; i < a.size(); i++) {
            Object s = a.get(i);
            Object t = b.get(i);
            if (s instanceof Character || t instanceof Character) {
                if (!s.equals(t)) {
                    return null;
                }
                command = ((Character) s).charValue();
                index = 0;
                out.append(out.length() == 0 ? "" : " ").append(command);
                continue;
            }
            double u = ((Double) s).doubleValue();
            double v = ((Double) t).doubleValue();
            boolean flag = (command == 'a' || command == 'A') && (index % 7 == 3 || index % 7 == 4);
            index++;
            if (flag && u != v) {
                return null;
            }
            out.append(' ').append(number(u + (v - u) * p));
        }
        List<CssValue> args = new ArrayList<CssValue>();
        if (x.size() == 2) {
            args.add(x.get(0));
        }
        args.add(new CssString(out.toString()));
        return new CssFunctionValue("path", args);
    }

    /**
     * Splits path data into command letters and numbers.
     * <p>
     * 경로 데이터를 명령 글자와 수로 나눈다.
     *
     * @param d the path data / 경로 데이터
     * @return the tokens (Character or Double), or null if the data is not well formed / 토큰(Character 나 Double). 형식이 맞지 않으면 null
     */
    static List<Object> tokens(String d) {
        List<Object> out = new ArrayList<Object>();
        int i = 0;
        int n = d.length();
        // 글자를 하나씩 보며 명령, 수, 구분자를 나눈다.
        while (i < n) {
            char c = d.charAt(i);
            if (c == ' ' || c == ',' || c == '\t' || c == '\n' || c == '\r' || c == '\f') {
                i++;
            } else if ("MmLlHhVvCcSsQqTtAaZz".indexOf(c) >= 0) {
                out.add(Character.valueOf(c));
                i++;
            } else {
                int start = i;
                if (c == '+' || c == '-') {
                    i++;
                }
                boolean digits = false;
                // 정수 부분.
                while (i < n && Character.isDigit(d.charAt(i))) {
                    i++;
                    digits = true;
                }
                if (i < n && d.charAt(i) == '.') {
                    i++;
                    // 소수 부분.
                    while (i < n && Character.isDigit(d.charAt(i))) {
                        i++;
                        digits = true;
                    }
                }
                if (!digits) {
                    return null;
                }
                if (i < n && (d.charAt(i) == 'e' || d.charAt(i) == 'E')) {
                    int e = i + 1;
                    if (e < n && (d.charAt(e) == '+' || d.charAt(e) == '-')) {
                        e++;
                    }
                    if (e < n && Character.isDigit(d.charAt(e))) {
                        i = e;
                        // 지수 부분.
                        while (i < n && Character.isDigit(d.charAt(i))) {
                            i++;
                        }
                    }
                }
                try {
                    out.add(Double.valueOf(Double.parseDouble(d.substring(start, i))));
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
        }
        return out;
    }

    /**
     * Formats a number without a needless fraction.
     * <p>
     * 쓸데없는 소수부 없이 수를 쓴다.
     *
     * @param v the number / 수
     * @return the text / 텍스트
     */
    private static String number(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        return String.valueOf(Math.round(v * 1e6) / 1e6);
    }
}
