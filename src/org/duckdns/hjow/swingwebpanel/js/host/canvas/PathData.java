package org.duckdns.hjow.swingwebpanel.js.host.canvas;

import java.awt.geom.AffineTransform;

/**
 * The parser of SVG path data (<code>d</code> attribute syntax) for <code>new Path2D(string)</code>: the commands <code>M m L l H h V v C c S s
 * Q q T t A a Z z</code> with absolute and relative coordinates, implicit repetition of a command (a second pair after <code>M</code> is a
 * <code>L</code>), numbers with signs, fractions and exponents separated by white space or commas (or by nothing where the sign or the dot
 * makes it clear), and the compact flags of arcs. Elliptical arcs are converted from the endpoint parameterization to the center
 * parameterization of the SVG specification (appendix F.6) and drawn as Bezier curves. The first error ends the parsing and keeps what was
 * built before it.
 * <p>
 * <code>new Path2D(string)</code> 을 위한 SVG 경로 데이터(<code>d</code> 속성 문법) 해석기: 명령 <code>M m L l H h V v C c S s Q q T t A a Z z</code>
 * (절대/상대 좌표), 명령의 암묵적 반복(<code>M</code> 뒤의 두 번째 쌍은 <code>L</code>), 부호/소수/지수가 있는 숫자(공백이나 쉼표로, 부호나 점이 구분을 분명히 하면
 * 아무것도 없이도 구분), 호의 붙여 쓴 플래그. 타원호는 끝점 매개변수화에서 SVG 규격(부록 F.6)의 중심 매개변수화로 바꿔 베지어 곡선으로 그린다. 첫 오류에서
 * 해석을 끝내고 그 앞까지 만든 것은 남긴다.
 */
final class PathData {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PathData() {
    }

    /**
     * Reads the text character by character.
     * <p>
     * 텍스트를 문자 하나씩 읽는다.
     */
    private static final class Reader {
        /**
         * The text.
         * <p>
         * 텍스트.
         */
        private final String s;

        /**
         * The position of the next character.
         * <p>
         * 다음 문자의 위치.
         */
        private int i;

        /**
         * Creates the reader.
         * <p>
         * 읽는 쪽을 만든다.
         *
         * @param s the text / 텍스트
         */
        Reader(String s) {
            this.s = s;
        }

        /**
         * Skips white space.
         * <p>
         * 공백을 건너뛴다.
         */
        void skipSpaces() {
            // 공백 문자를 건너뛴다.
            while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\n' || s.charAt(i) == '\r' || s.charAt(i) == '\f')) {
                i++;
            }
        }

        /**
         * Skips white space and at most one comma (the separator between two numbers).
         * <p>
         * 공백과 많아야 하나의 쉼표(숫자 사이의 구분자)를 건너뛴다.
         */
        void skipSeparator() {
            skipSpaces();
            if (i < s.length() && s.charAt(i) == ',') {
                i++;
                skipSpaces();
            }
        }

        /**
         * Tells whether the text is used up.
         * <p>
         * 텍스트를 다 썼는지 알려 준다.
         *
         * @return true at the end / 끝이면 true
         */
        boolean atEnd() {
            return i >= s.length();
        }

        /**
         * Returns the next character without consuming it.
         * <p>
         * 다음 문자를 소비하지 않고 반환한다.
         *
         * @return the character / 문자
         */
        char peek() {
            return s.charAt(i);
        }

        /**
         * Consumes the next character.
         * <p>
         * 다음 문자를 소비한다.
         */
        void next() {
            i++;
        }

        /**
         * Reads a number: an optional sign, digits with an optional fraction (or only a fraction), and an optional exponent.
         * <p>
         * 숫자를 읽는다: 선택적인 부호, 소수가 있을 수 있는 숫자(또는 소수만), 선택적인 지수.
         *
         * @return the number, or null if there is none at this position / 숫자. 이 위치에 없으면 null
         */
        Double number() {
            skipSeparator();
            int start = i;
            if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                i++;
            }
            int digits = 0;
            // 정수부 숫자를 읽는다.
            while (i < s.length() && Character.isDigit(s.charAt(i))) {
                i++;
                digits++;
            }
            if (i < s.length() && s.charAt(i) == '.') {
                i++;
                // 소수부 숫자를 읽는다.
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                    digits++;
                }
            }
            if (digits == 0) {
                i = start;
                return null;
            }
            // 지수부가 있으면 읽는다. 숫자가 이어지지 않는 e 는 지수가 아니다.
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                int save = i;
                i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                    i++;
                }
                int expDigits = 0;
                // 지수의 숫자를 읽는다.
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                    expDigits++;
                }
                if (expDigits == 0) {
                    i = save;
                }
            }
            try {
                double v = Double.parseDouble(s.substring(start, i));
                return Double.isNaN(v) || Double.isInfinite(v) ? null : Double.valueOf(v);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        /**
         * Reads an arc flag: the single character <code>0</code> or <code>1</code>.
         * <p>
         * 호의 플래그를 읽는다: 문자 <code>0</code> 또는 <code>1</code> 하나.
         *
         * @return 0 or 1, or null if there is no flag / 0 또는 1. 플래그가 없으면 null
         */
        Double flag() {
            skipSeparator();
            if (i < s.length() && (s.charAt(i) == '0' || s.charAt(i) == '1')) {
                double v = s.charAt(i) - '0';
                i++;
                return Double.valueOf(v);
            }
            return null;
        }
    }

    /**
     * Returns the number of numbers that a command takes.
     * <p>
     * 명령이 받는 숫자의 수를 반환한다.
     *
     * @param c the upper case command letter / 대문자 명령 문자
     * @return the count, or -1 for an unknown letter / 개수. 모르는 문자이면 -1
     */
    private static int argCount(char c) {
        switch (c) {
        case 'M':
        case 'L':
        case 'T':
            return 2;
        case 'H':
        case 'V':
            return 1;
        case 'C':
            return 6;
        case 'S':
        case 'Q':
            return 4;
        case 'A':
            return 7;
        case 'Z':
            return 0;
        default:
            return -1;
        }
    }

    /**
     * Parses path data into a path.
     * <p>
     * 경로 데이터를 경로로 해석한다.
     *
     * @param d the path data / 경로 데이터
     * @param p the path to add to (in user space) / 더할 경로(사용자 좌표)
     */
    static void parse(String d, PathBuilder p) {
        AffineTransform id = new AffineTransform();
        Reader r = new Reader(d);
        char cmd = 0;
        char prev = 0;
        double cx = 0;
        double cy = 0;
        double sx = 0;
        double sy = 0;
        double lx = 0;
        double ly = 0;
        boolean started = false;
        // 명령과 그 인수를 끝이나 첫 오류까지 하나씩 읽는다.
        for (;;) {
            r.skipSpaces();
            if (r.atEnd()) {
                return;
            }
            char c = r.peek();
            if (Character.isLetter(c)) {
                if (argCount(Character.toUpperCase(c)) < 0) {
                    return;
                }
                cmd = c;
                r.next();
                if (!started && Character.toUpperCase(cmd) != 'M') {
                    return;
                }
                if (Character.toUpperCase(cmd) == 'Z') {
                    p.closePath();
                    cx = sx;
                    cy = sy;
                    prev = 'Z';
                    continue;
                }
            } else if (cmd == 0 || Character.toUpperCase(cmd) == 'Z') {
                return;
            }
            char up = Character.toUpperCase(cmd);
            boolean rel = Character.isLowerCase(cmd);
            int n = argCount(up);
            double[] a = new double[n];
            // 명령의 인수를 읽는다. 호의 셋째와 넷째 플래그는 한 글자이다.
            for (int k = 0; k < n; k++) {
                Double v = up == 'A' && (k == 3 || k == 4) ? r.flag() : r.number();
                if (v == null) {
                    return;
                }
                a[k] = v.doubleValue();
            }
            double ox = rel ? cx : 0;
            double oy = rel ? cy : 0;
            switch (up) {
            case 'M':
                cx = a[0] + ox;
                cy = a[1] + oy;
                sx = cx;
                sy = cy;
                p.moveTo(id, cx, cy);
                started = true;
                cmd = rel ? 'l' : 'L';
                break;
            case 'L':
                cx = a[0] + ox;
                cy = a[1] + oy;
                p.lineTo(id, cx, cy);
                break;
            case 'H':
                cx = a[0] + ox;
                p.lineTo(id, cx, cy);
                break;
            case 'V':
                cy = a[0] + oy;
                p.lineTo(id, cx, cy);
                break;
            case 'C':
                p.cubicTo(id, a[0] + ox, a[1] + oy, a[2] + ox, a[3] + oy, a[4] + ox, a[5] + oy);
                lx = a[2] + ox;
                ly = a[3] + oy;
                cx = a[4] + ox;
                cy = a[5] + oy;
                break;
            case 'S':
                double s1x = prev == 'C' || prev == 'S' ? 2 * cx - lx : cx;
                double s1y = prev == 'C' || prev == 'S' ? 2 * cy - ly : cy;
                p.cubicTo(id, s1x, s1y, a[0] + ox, a[1] + oy, a[2] + ox, a[3] + oy);
                lx = a[0] + ox;
                ly = a[1] + oy;
                cx = a[2] + ox;
                cy = a[3] + oy;
                break;
            case 'Q':
                p.quadTo(id, a[0] + ox, a[1] + oy, a[2] + ox, a[3] + oy);
                lx = a[0] + ox;
                ly = a[1] + oy;
                cx = a[2] + ox;
                cy = a[3] + oy;
                break;
            case 'T':
                double qx = prev == 'Q' || prev == 'T' ? 2 * cx - lx : cx;
                double qy = prev == 'Q' || prev == 'T' ? 2 * cy - ly : cy;
                p.quadTo(id, qx, qy, a[0] + ox, a[1] + oy);
                lx = qx;
                ly = qy;
                cx = a[0] + ox;
                cy = a[1] + oy;
                break;
            default:
                double ex = a[5] + ox;
                double ey = a[6] + oy;
                arc(p, id, cx, cy, a[0], a[1], a[2], a[3] != 0, a[4] != 0, ex, ey);
                cx = ex;
                cy = ey;
                break;
            }
            prev = up;
        }
    }

    /**
     * Adds an elliptical arc given by its end points (SVG appendix F.6).
     * <p>
     * 끝점으로 주어진 타원호를 더한다(SVG 부록 F.6).
     *
     * @param p    the path / 경로
     * @param id   the identity matrix / 단위 행렬
     * @param x1   start x / 시작 x
     * @param y1   start y / 시작 y
     * @param rx   x radius / x 반지름
     * @param ry   y radius / y 반지름
     * @param phi  rotation of the ellipse in degrees / 타원의 회전(도)
     * @param fa   the large-arc flag / large-arc 플래그
     * @param fs   the sweep flag / sweep 플래그
     * @param x2   end x / 끝 x
     * @param y2   end y / 끝 y
     */
    private static void arc(PathBuilder p, AffineTransform id, double x1, double y1, double rx, double ry, double phi, boolean fa, boolean fs,
            double x2, double y2) {
        if (x1 == x2 && y1 == y2) {
            return;
        }
        double ax = Math.abs(rx);
        double ay = Math.abs(ry);
        // 반지름이 0 이면 직선으로 대신한다.
        if (ax == 0 || ay == 0) {
            p.lineTo(id, x2, y2);
            return;
        }
        double rot = Math.toRadians(phi % 360);
        double cosP = Math.cos(rot);
        double sinP = Math.sin(rot);
        double dx2 = (x1 - x2) / 2;
        double dy2 = (y1 - y2) / 2;
        double x1p = cosP * dx2 + sinP * dy2;
        double y1p = -sinP * dx2 + cosP * dy2;
        double lambda = x1p * x1p / (ax * ax) + y1p * y1p / (ay * ay);
        // 반지름이 끝점을 잇기에 모자라면 필요한 만큼 키운다.
        if (lambda > 1) {
            double s = Math.sqrt(lambda);
            ax *= s;
            ay *= s;
        }
        double num = ax * ax * ay * ay - ax * ax * y1p * y1p - ay * ay * x1p * x1p;
        double den = ax * ax * y1p * y1p + ay * ay * x1p * x1p;
        double coef = (fa == fs ? -1 : 1) * Math.sqrt(Math.max(0, num / den));
        double cxp = coef * (ax * y1p / ay);
        double cyp = coef * -(ay * x1p / ax);
        double cx = cosP * cxp - sinP * cyp + (x1 + x2) / 2;
        double cy = sinP * cxp + cosP * cyp + (y1 + y2) / 2;
        double th1 = angle(1, 0, (x1p - cxp) / ax, (y1p - cyp) / ay);
        double dth = angle((x1p - cxp) / ax, (y1p - cyp) / ay, (-x1p - cxp) / ax, (-y1p - cyp) / ay);
        if (!fs && dth > 0) {
            dth -= 2 * Math.PI;
        } else if (fs && dth < 0) {
            dth += 2 * Math.PI;
        }
        p.ellipse(id, cx, cy, ax, ay, rot, th1, th1 + dth, dth < 0);
    }

    /**
     * Returns the signed angle from one vector to another.
     * <p>
     * 한 벡터에서 다른 벡터까지의 부호 있는 각도를 반환한다.
     *
     * @param ux x of the first vector / 첫 벡터의 x
     * @param uy y of the first vector / 첫 벡터의 y
     * @param vx x of the second vector / 둘째 벡터의 x
     * @param vy y of the second vector / 둘째 벡터의 y
     * @return the angle in radians / 각도(라디안)
     */
    private static double angle(double ux, double uy, double vx, double vy) {
        double dot = ux * vx + uy * vy;
        double len = Math.sqrt(ux * ux + uy * uy) * Math.sqrt(vx * vx + vy * vy);
        double a = Math.acos(Math.max(-1, Math.min(1, dot / len)));
        return ux * vy - uy * vx < 0 ? -a : a;
    }
}
