package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Color;
import java.awt.MultipleGradientPaint.CycleMethod;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.values.CssCalc;
import org.duckdns.hjow.swingwebpanel.css.values.CssColor;
import org.duckdns.hjow.swingwebpanel.css.values.CssDimension;
import org.duckdns.hjow.swingwebpanel.css.values.CssFunctionValue;
import org.duckdns.hjow.swingwebpanel.css.values.CssKeyword;
import org.duckdns.hjow.swingwebpanel.css.values.CssLength;
import org.duckdns.hjow.swingwebpanel.css.values.CssList;
import org.duckdns.hjow.swingwebpanel.css.values.CssNumber;
import org.duckdns.hjow.swingwebpanel.css.values.CssPercentage;
import org.duckdns.hjow.swingwebpanel.css.values.CssValue;
import org.duckdns.hjow.swingwebpanel.css.values.LengthContext;

/**
 * A CSS gradient image (plan item P10-04, CSS Images Level 3 section 3): <code>linear-gradient()</code>, <code>radial-gradient()</code>,
 * <code>conic-gradient()</code> and their <code>repeating-</code> forms. The specification is read from the structured computed value
 * of <code>background-image</code> ({@link #parse}); {@link #createPaint} turns it into a Java2D {@link Paint} for an image of a given
 * size. Linear and radial gradients use {@link LinearGradientPaint} and {@link RadialGradientPaint} (the standard Java2D paints, no
 * native code), conic gradients a small raster {@link Paint} of this class.
 * <p>
 * Implemented: angles, <code>to</code> side and corner keywords (the magic corners), color stop positions as lengths or percentages with
 * the fix-up of missing and decreasing positions (3.4.3), two-position stops, color hints (3.4.4), interpolation in premultiplied
 * color space so that a transparent stop does not turn gray (3.4.2), the shape and size keywords and explicit radii and the
 * <code>at</code> position of radial gradients (3.2.1), <code>from</code> and <code>at</code> of conic gradients, repeating gradients.
 * Documented limits: a repeating radial gradient whose first stop is not at zero shows the plain pad color inside the first stop
 * instead of the repeated pattern, a color stop position of a conic gradient that is a length is ignored, no dithering.
 * <p>
 * CSS 그라디언트 이미지(계획 P10-04, CSS Images Level 3 3 절): <code>linear-gradient()</code>, <code>radial-gradient()</code>, <code>conic-gradient()</code> 와
 * <code>repeating-</code> 형태. 명세는 <code>background-image</code> 의 구조화된 계산 값에서 읽고({@link #parse}) {@link #createPaint} 가 주어진 크기의
 * 이미지를 위한 Java2D {@link Paint} 로 바꾼다. 선형과 방사형은 표준 Java2D 페인트인 {@link LinearGradientPaint}, {@link RadialGradientPaint}
 * (네이티브 코드 없음), 원뿔형은 이 클래스의 작은 래스터 {@link Paint} 를 쓴다.
 * <p>
 * 구현: 각도, <code>to</code> 변과 모서리 키워드(마법 모서리), 길이나 백분율의 색 정지점 위치와 빠진/줄어드는 위치의 보정(3.4.3), 위치 둘의 정지점, 색 힌트(3.4.4),
 * 투명 정지점이 회색으로 변하지 않도록 사전 곱셈 색 공간에서의 보간(3.4.2), 방사형의 모양과 크기 키워드, 명시적 반지름, <code>at</code> 위치(3.2.1), 원뿔형의
 * <code>from</code> 과 <code>at</code>, 반복 그라디언트. 문서화된 한계: 첫 정지점이 0 이 아닌 반복 방사형은 첫 정지점 안쪽을 반복 무늬가 아니라 단순 채움 색으로 보이고,
 * 원뿔형 정지점 위치가 길이이면 무시하며, 디더링은 없다.
 */
final class GradientSpec {

    /** Type: linear gradient. 종류: 선형. */
    static final int LINEAR = 0;
    /** Type: radial gradient. 종류: 방사형. */
    static final int RADIAL = 1;
    /** Type: conic gradient. 종류: 원뿔형. */
    static final int CONIC = 2;

    /** A color stop or a color hint of the specification. 명세의 색 정지점 또는 색 힌트. */
    private static final class Stop {
        /** The color as ARGB (unused for hints). ARGB 색(힌트에서는 쓰지 않음). */
        int argb;
        /** The position value, or null if it is not given. 위치 값. 없으면 null. */
        CssValue pos;
        /** True for a color hint (it has a position but no color). 색 힌트(위치만 있고 색은 없음)이면 true. */
        boolean hint;
    }

    /** The type, one of {@link #LINEAR}, {@link #RADIAL}, {@link #CONIC}. 종류. */
    final int type;
    /** True for the repeating forms. 반복 형태이면 true. */
    final boolean repeating;
    /** Linear: the angle in degrees (0 = up, clockwise), NaN for corner keywords. 선형: 각도(도, 0 은 위쪽, 시계 방향). 모서리 키워드이면 NaN. */
    private double angle = 180;
    /** Linear: the corner or side of <code>to</code>: x is -1 left, 1 right, 0 none. 선형: <code>to</code> 의 변이나 모서리 x. */
    private int toX;
    /** Linear: y is -1 top, 1 bottom, 0 none. 선형: y. */
    private int toY;
    /** Radial: true for a circle. 방사형: 원이면 true. */
    private boolean circle;
    /** Radial: the size keyword, or null if radii are given. 방사형: 크기 키워드. 반지름이 주어졌으면 null. */
    private String sizeKeyword = "farthest-corner";
    /** Radial: the explicit radii or null. 방사형: 명시적 반지름 또는 null. */
    private CssValue radiusX;
    /** Radial: the explicit vertical radius or null. 방사형: 명시적 세로 반지름 또는 null. */
    private CssValue radiusY;
    /** Radial and conic: the tokens of the position, or null for the center. 방사형, 원뿔형: 위치 토큰. 가운데이면 null. */
    private List<CssValue> position;
    /** Conic: the start angle in degrees. 원뿔형: 시작 각도(도). */
    private double from;
    /** The color stops and hints. 색 정지점과 힌트. */
    private final List<Stop> stops = new ArrayList<Stop>();

    /**
     * Creates an empty specification.
     * <p>
     * 빈 명세를 만든다.
     *
     * @param type      the type / 종류
     * @param repeating true for a repeating gradient / 반복 그라디언트이면 true
     */
    private GradientSpec(int type, boolean repeating) {
        this.type = type;
        this.repeating = repeating;
    }

    /**
     * Reads a gradient from its computed value.
     * <p>
     * 계산 값에서 그라디언트를 읽는다.
     *
     * @param f            the function value / 함수 값
     * @param currentColor the ARGB color that <code>currentcolor</code> stands for / <code>currentcolor</code> 가 뜻하는 ARGB 색
     * @return the specification, or null if the function is not a (valid) gradient / 명세. 그라디언트가 아니거나 잘못되었으면 null
     */
    static GradientSpec parse(CssFunctionValue f, int currentColor) {
        String name = f.getName().toLowerCase();
        boolean rep = name.startsWith("repeating-");
        String base = rep ? name.substring(10) : name;
        int type;
        if ("linear-gradient".equals(base)) {
            type = LINEAR;
        } else if ("radial-gradient".equals(base)) {
            type = RADIAL;
        } else if ("conic-gradient".equals(base)) {
            type = CONIC;
        } else {
            return null;
        }
        GradientSpec g = new GradientSpec(type, rep);
        List<CssValue> args = f.getArguments();
        int i = 0;
        if (!args.isEmpty() && g.readConfig(args.get(0))) {
            i = 1;
        }
        // 나머지 인수는 색 정지점이나 색 힌트이다.
        for (; i < args.size(); i++) {
            if (!g.readStop(args.get(i), currentColor)) {
                return null;
            }
        }
        int colors = 0;
        // 힌트가 아닌 정지점이 둘 이상이어야 한다.
        for (int k = 0; k < g.stops.size(); k++) {
            if (!g.stops.get(k).hint) {
                colors++;
            }
        }
        return colors >= 2 ? g : null;
    }

    /**
     * Reads the first argument if it configures the gradient (direction, shape and size, position).
     * <p>
     * 첫 인수가 그라디언트를 설정하는 것(방향, 모양과 크기, 위치)이면 읽는다.
     *
     * @param v the argument / 인수
     * @return true if it was a configuration / 설정이었으면 true
     */
    private boolean readConfig(CssValue v) {
        List<CssValue> t = new ArrayList<CssValue>();
        if (v instanceof CssList && !((CssList) v).isComma()) {
            // 공백 목록은 토큰으로 풀어 낸다. 색으로 시작하면 설정이 아니라 정지점이다.
            for (int i = 0; i < ((CssList) v).size(); i++) {
                t.add(((CssList) v).get(i));
            }
        } else {
            t.add(v);
        }
        CssValue first = t.get(0);
        if (type == LINEAR) {
            if (first instanceof CssDimension && ((CssDimension) first).getKind() == org.duckdns.hjow.swingwebpanel.css.values.CssUnits.ANGLE) {
                angle = ((CssDimension) first).canonical();
                return true;
            }
            if (first.isKeyword("to")) {
                angle = Double.NaN;
                // to 뒤의 변과 모서리 키워드를 읽는다.
                for (int i = 1; i < t.size(); i++) {
                    if (t.get(i).isKeyword("left")) {
                        toX = -1;
                    } else if (t.get(i).isKeyword("right")) {
                        toX = 1;
                    } else if (t.get(i).isKeyword("top")) {
                        toY = -1;
                    } else if (t.get(i).isKeyword("bottom")) {
                        toY = 1;
                    }
                }
                return true;
            }
            return false;
        }
        if (type == RADIAL) {
            boolean config = first.isKeyword("circle") || first.isKeyword("ellipse") || first.isKeyword("closest-side") || first.isKeyword("farthest-side")
                    || first.isKeyword("closest-corner") || first.isKeyword("farthest-corner") || first.isKeyword("at")
                    || ((first instanceof CssLength || first instanceof CssPercentage || first instanceof CssCalc) && t.size() > 1);
            if (!config) {
                return false;
            }
            int i = 0;
            List<CssValue> lens = new ArrayList<CssValue>();
            // 모양, 크기 키워드, 명시적 반지름을 at 이전까지 읽는다.
            for (; i < t.size() && !t.get(i).isKeyword("at"); i++) {
                CssValue c = t.get(i);
                if (c.isKeyword("circle")) {
                    circle = true;
                } else if (c.isKeyword("ellipse")) {
                    circle = false;
                } else if (c instanceof CssKeyword) {
                    sizeKeyword = ((CssKeyword) c).getName();
                } else {
                    lens.add(c);
                }
            }
            if (!lens.isEmpty()) {
                sizeKeyword = null;
                radiusX = lens.get(0);
                radiusY = lens.size() > 1 ? lens.get(1) : null;
                circle = lens.size() == 1;
            }
            if (i < t.size()) {
                position = new ArrayList<CssValue>(t.subList(i + 1, t.size()));
            }
            return true;
        }
        if (first.isKeyword("from") || first.isKeyword("at")) {
            int i = 0;
            // from 각도와 at 위치를 읽는다.
            while (i < t.size()) {
                if (t.get(i).isKeyword("from") && i + 1 < t.size() && t.get(i + 1) instanceof CssDimension) {
                    from = ((CssDimension) t.get(i + 1)).canonical();
                    i += 2;
                } else if (t.get(i).isKeyword("at")) {
                    position = new ArrayList<CssValue>(t.subList(i + 1, t.size()));
                    break;
                } else {
                    i++;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Reads a color stop or a color hint.
     * <p>
     * 색 정지점이나 색 힌트를 읽는다.
     *
     * @param v            the argument / 인수
     * @param currentColor the color of <code>currentcolor</code> / <code>currentcolor</code> 의 색
     * @return false if the argument is not valid / 인수가 잘못되었으면 false
     */
    private boolean readStop(CssValue v, int currentColor) {
        if (v instanceof CssList && !((CssList) v).isComma()) {
            CssList l = (CssList) v;
            int c = colorOf(l.get(0), currentColor);
            if (c == NO_COLOR) {
                return false;
            }
            // 색 뒤의 위치(하나나 둘)마다 정지점을 만든다.
            for (int i = 1; i < l.size(); i++) {
                Stop s = new Stop();
                s.argb = c;
                s.pos = l.get(i);
                stops.add(s);
            }
            if (l.size() == 1) {
                Stop s = new Stop();
                s.argb = c;
                stops.add(s);
            }
            return true;
        }
        int c = colorOf(v, currentColor);
        if (c != NO_COLOR) {
            Stop s = new Stop();
            s.argb = c;
            stops.add(s);
            return true;
        }
        if (v instanceof CssLength || v instanceof CssPercentage || v instanceof CssCalc || v instanceof CssNumber || v instanceof CssDimension) {
            Stop s = new Stop();
            s.hint = true;
            s.pos = v;
            stops.add(s);
            return true;
        }
        return false;
    }

    /** Marker: the value is not a color. 표지: 값이 색이 아님. */
    private static final int NO_COLOR = 0x12345678;

    /**
     * Returns the ARGB value of a color value (<code>currentcolor</code> is resolved).
     * <p>
     * 색 값의 ARGB 를 반환한다(<code>currentcolor</code> 는 해석한다).
     *
     * @param v            the value / 값
     * @param currentColor the color of <code>currentcolor</code> / <code>currentcolor</code> 의 색
     * @return the color, or {@link #NO_COLOR} / 색. 아니면 {@link #NO_COLOR}
     */
    private static int colorOf(CssValue v, int currentColor) {
        if (v instanceof CssColor) {
            return ((CssColor) v).getArgb();
        }
        if (v.isKeyword("currentcolor")) {
            return currentColor;
        }
        return NO_COLOR;
    }

    // ----------------------------------------------------------------------------------------------------- stops

    /**
     * Resolves a stop position against a total length.
     * <p>
     * 정지점 위치를 전체 길이에 대해 해석한다.
     *
     * @param v     the value / 값
     * @param total the length that 100% stands for (an angle gradient: 360) / 100% 가 뜻하는 길이(각도 그라디언트는 360)
     * @return the position, NaN if it is not given or not valid / 위치. 없거나 잘못되었으면 NaN
     */
    private double position(CssValue v, double total) {
        if (v == null) {
            return Double.NaN;
        }
        if (v instanceof CssPercentage) {
            return ((CssPercentage) v).getValue() * total / 100.0;
        }
        if (type == CONIC) {
            if (v instanceof CssDimension && ((CssDimension) v).getKind() == org.duckdns.hjow.swingwebpanel.css.values.CssUnits.ANGLE) {
                return ((CssDimension) v).canonical();
            }
            return v instanceof CssNumber && ((CssNumber) v).getValue() == 0 ? 0 : Double.NaN;
        }
        if (v instanceof CssLength) {
            return ((CssLength) v).getValue();
        }
        if (v instanceof CssNumber && ((CssNumber) v).getValue() == 0) {
            return 0;
        }
        if (v instanceof CssCalc) {
            return ((CssCalc) v).resolve(LengthContext.defaults(), total);
        }
        return Double.NaN;
    }

    /**
     * Builds the resolved stop list: positions fixed up (3.4.3), hints replaced by the transition they describe (3.4.4), and the
     * transitions between colors of different transparency subdivided so that the interpolation of the paint approximates the
     * interpolation in premultiplied space (3.4.2). The two-position stops have already become two stops.
     * <p>
     * 해석된 정지점 목록을 만든다: 위치를 보정하고(3.4.3), 힌트는 그것이 뜻하는 전이로 바꾸며(3.4.4), 투명도가 다른 색 사이의 전이는 쪼개어 페인트의 보간이 사전 곱셈
     * 공간의 보간에 가깝게 만든다(3.4.2). 위치 둘의 정지점은 이미 정지점 둘이 되어 있다.
     *
     * @param total the length that 100% stands for / 100% 가 뜻하는 길이
     * @return two parallel arrays: the positions and the ARGB colors / 나란한 배열 둘: 위치와 ARGB 색
     */
    private Object[] resolve(double total) {
        int n = stops.size();
        double[] pos = new double[n];
        // 위치를 읽는다. 없는 것은 NaN 이다.
        for (int i = 0; i < n; i++) {
            pos[i] = position(stops.get(i).pos, total);
        }
        int first = -1;
        int last = -1;
        // 힌트가 아닌 첫 정지점과 마지막 정지점을 찾는다.
        for (int i = 0; i < n; i++) {
            if (!stops.get(i).hint) {
                if (first < 0) {
                    first = i;
                }
                last = i;
            }
        }
        if (Double.isNaN(pos[first])) {
            pos[first] = 0;
        }
        if (Double.isNaN(pos[last])) {
            pos[last] = total;
        }
        double max = -Double.MAX_VALUE;
        // 앞선 정지점보다 작은 위치는 앞선 최댓값으로 올린다(3.4.3).
        for (int i = 0; i < n; i++) {
            if (stops.get(i).hint || Double.isNaN(pos[i])) {
                continue;
            }
            if (pos[i] < max) {
                pos[i] = max;
            }
            max = pos[i];
        }
        // 위치가 없는 정지점은 이웃한 위치가 있는 정지점 사이에 같은 간격으로 놓는다.
        int i = 0;
        while (i < n) {
            if (stops.get(i).hint || !Double.isNaN(pos[i])) {
                i++;
                continue;
            }
            int a = i - 1;
            // 앞쪽으로 위치가 정해진 정지점을 찾는다.
            while (a >= 0 && (stops.get(a).hint || Double.isNaN(pos[a]))) {
                a--;
            }
            int b = i;
            // 뒤쪽으로 위치가 정해진 정지점을 찾는다.
            while (b < n && (stops.get(b).hint || Double.isNaN(pos[b]))) {
                b++;
            }
            int count = 0;
            // 사이의 정지점(힌트 제외) 수를 센다.
            for (int k = a + 1; k < b; k++) {
                if (!stops.get(k).hint) {
                    count++;
                }
            }
            int step = 1;
            // 사이의 정지점마다 같은 간격으로 위치를 준다.
            for (int k = a + 1; k < b; k++) {
                if (!stops.get(k).hint) {
                    pos[k] = pos[a] + (pos[b] - pos[a]) * step / (count + 1);
                    step++;
                }
            }
            i = b;
        }
        List<Double> outPos = new ArrayList<Double>();
        List<Integer> outCol = new ArrayList<Integer>();
        int prev = -1;
        // 정지점을 차례로 훑으며 힌트와 투명도가 다른 전이를 쪼갠다.
        for (int k = 0; k < n; k++) {
            Stop s = stops.get(k);
            if (s.hint) {
                continue;
            }
            if (prev >= 0) {
                double hint = Double.NaN;
                // 두 정지점 사이의 힌트 위치를 찾는다.
                for (int q = prev + 1; q < k; q++) {
                    if (stops.get(q).hint) {
                        hint = position(stops.get(q).pos, total);
                    }
                }
                int ca = stops.get(prev).argb;
                int cb = s.argb;
                boolean differentAlpha = (ca >>> 24) != (cb >>> 24);
                if (!Double.isNaN(hint) || differentAlpha) {
                    double span = pos[k] - pos[prev];
                    double exp = 1;
                    if (!Double.isNaN(hint) && span > 0) {
                        double hf = Math.max(1e-6, Math.min(1 - 1e-6, (hint - pos[prev]) / span));
                        exp = Math.log(0.5) / Math.log(hf);
                    }
                    int steps = 12;
                    // 두 정지점 사이를 쪼개 사전 곱셈 보간(힌트가 있으면 지수 곡선)으로 중간 색을 더한다.
                    for (int q = 1; q < steps; q++) {
                        double t = (double) q / steps;
                        outPos.add(Double.valueOf(pos[prev] + span * t));
                        outCol.add(Integer.valueOf(mix(ca, cb, Math.pow(t, exp))));
                    }
                }
            }
            outPos.add(Double.valueOf(pos[k]));
            outCol.add(Integer.valueOf(s.argb));
            prev = k;
        }
        double[] rp = new double[outPos.size()];
        int[] rc = new int[outPos.size()];
        // 목록을 배열로 옮긴다.
        for (int k = 0; k < rp.length; k++) {
            rp[k] = outPos.get(k).doubleValue();
            rc[k] = outCol.get(k).intValue();
        }
        return new Object[] { rp, rc };
    }

    /**
     * Interpolates two colors in premultiplied color space.
     * <p>
     * 두 색을 사전 곱셈 색 공간에서 보간한다.
     *
     * @param a the first ARGB color / 첫 ARGB 색
     * @param b the second ARGB color / 둘째 ARGB 색
     * @param t the weight of the second color (0..1) / 둘째 색의 비중(0..1)
     * @return the ARGB color (not premultiplied) / ARGB 색(사전 곱셈 아님)
     */
    static int mix(int a, int b, double t) {
        double aa = (a >>> 24) / 255.0;
        double ab = (b >>> 24) / 255.0;
        double alpha = aa + (ab - aa) * t;
        if (alpha <= 0) {
            return 0;
        }
        int out = (int) Math.round(alpha * 255) << 24;
        // 색 채널마다 사전 곱셈 값을 보간하고 알파로 나눠 원래대로 돌린다.
        for (int shift = 16; shift >= 0; shift -= 8) {
            double ca = ((a >> shift) & 255) * aa;
            double cb = ((b >> shift) & 255) * ab;
            int c = (int) Math.round((ca + (cb - ca) * t) / alpha);
            out |= Math.max(0, Math.min(255, c)) << shift;
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------------- paints

    /**
     * Creates the paint of this gradient for an image of a given size whose top left corner is the origin.
     * <p>
     * 이 그라디언트의 페인트를 왼쪽 위가 원점인 주어진 크기의 이미지에 대해 만든다.
     *
     * @param w image width / 이미지 너비
     * @param h image height / 이미지 높이
     * @return the paint (a solid color if the gradient degenerates) / 페인트(그라디언트가 퇴화하면 단색)
     */
    Paint createPaint(double w, double h) {
        if (w <= 0 || h <= 0) {
            return new Color(0, true);
        }
        if (type == LINEAR) {
            return linear(w, h);
        }
        if (type == RADIAL) {
            return radial(w, h);
        }
        return conic(w, h);
    }

    /**
     * Builds the paint of a linear gradient (3.1).
     * <p>
     * 선형 그라디언트의 페인트를 만든다(3.1).
     *
     * @param w image width / 이미지 너비
     * @param h image height / 이미지 높이
     * @return the paint / 페인트
     */
    private Paint linear(double w, double h) {
        double dx;
        double dy;
        if (Double.isNaN(angle)) {
            if (toX != 0 && toY != 0) {
                // 마법 모서리: 반대쪽 두 모서리를 잇는 대각선에 수직인 방향이다.
                double len = Math.hypot(h, w);
                dx = toX * h / len;
                dy = toY * w / len;
            } else {
                dx = toX;
                dy = toY == 0 && toX == 0 ? 1 : toY;
            }
        } else {
            double rad = Math.toRadians(angle);
            dx = Math.sin(rad);
            dy = -Math.cos(rad);
        }
        double length = Math.abs(w * dx) + Math.abs(h * dy);
        double cx = w / 2;
        double cy = h / 2;
        double sx = cx - dx * length / 2;
        double sy = cy - dy * length / 2;
        Object[] r = resolve(length);
        double[] pos = (double[]) r[0];
        int[] col = (int[]) r[1];
        double p0 = pos[0];
        double pn = pos[pos.length - 1];
        if (pn - p0 < 1e-3) {
            // 위치가 모두 같으면 그 지점에서 색이 바뀌는 딱딱한 경계이다(3.4.3).
            return new LinearGradientPaint(new Point2D.Double(sx + dx * p0, sy + dy * p0), new Point2D.Double(sx + dx * (p0 + 0.01), sy + dy * (p0 + 0.01)),
                    new float[] { 0f, 1f }, new Color[] { new Color(col[0], true), new Color(col[col.length - 1], true) }, CycleMethod.NO_CYCLE);
        }
        Point2D s = new Point2D.Double(sx + dx * p0, sy + dy * p0);
        Point2D e = new Point2D.Double(sx + dx * pn, sy + dy * pn);
        return new LinearGradientPaint(s, e, fractions(pos, p0, pn), colors(col), repeating ? CycleMethod.REPEAT : CycleMethod.NO_CYCLE);
    }

    /**
     * Converts positions to strictly increasing fractions between 0 and 1.
     * <p>
     * 위치를 0 과 1 사이의 엄격하게 증가하는 비율로 바꾼다.
     *
     * @param pos the positions / 위치
     * @param p0  the first position / 첫 위치
     * @param pn  the last position / 마지막 위치
     * @return the fractions / 비율
     */
    private static float[] fractions(double[] pos, double p0, double pn) {
        float[] f = new float[pos.length];
        // 위치를 비율로 바꾸고 같은 값이 이어지면 아주 조금씩 올린다.
        for (int i = 0; i < pos.length; i++) {
            f[i] = Math.max(0f, (float) ((pos[i] - p0) / (pn - p0)));
            if (i > 0 && f[i] <= f[i - 1]) {
                f[i] = Math.nextUp(f[i - 1]);
            }
        }
        f[0] = 0f;
        // 마지막 값이 1 을 넘으면 앞쪽을 눌러 맞춘다.
        if (f[f.length - 1] > 1f) {
            f[f.length - 1] = 1f;
            // 뒤에서부터 엄격 증가가 깨진 곳을 줄여 나간다.
            for (int i = f.length - 2; i >= 0 && f[i] >= f[i + 1]; i--) {
                f[i] = Math.nextAfter(f[i + 1], 0.0);
            }
        }
        return f;
    }

    /**
     * Converts ARGB values to colors.
     * <p>
     * ARGB 값을 색으로 바꾼다.
     *
     * @param col the ARGB values / ARGB 값
     * @return the colors / 색
     */
    private static Color[] colors(int[] col) {
        Color[] c = new Color[col.length];
        // 값마다 알파를 가진 색을 만든다.
        for (int i = 0; i < col.length; i++) {
            c[i] = new Color(col[i], true);
        }
        return c;
    }

    /**
     * Resolves the center given by the <code>at</code> tokens of a radial or conic gradient.
     * <p>
     * 방사형이나 원뿔형 그라디언트의 <code>at</code> 토큰이 정하는 중심을 해석한다.
     *
     * @param w image width / 이미지 너비
     * @param h image height / 이미지 높이
     * @return {x, y} / {x, y}
     */
    private double[] center(double w, double h) {
        if (position == null || position.isEmpty()) {
            return new double[] { w / 2, h / 2 };
        }
        return BackgroundLayers.resolvePosition(position, w, h);
    }

    /**
     * Builds the paint of a radial gradient (3.2).
     * <p>
     * 방사형 그라디언트의 페인트를 만든다(3.2).
     *
     * @param w image width / 이미지 너비
     * @param h image height / 이미지 높이
     * @return the paint / 페인트
     */
    private Paint radial(double w, double h) {
        double[] c = center(w, h);
        double cx = c[0];
        double cy = c[1];
        double dl = cx;
        double dr = w - cx;
        double dt = cy;
        double db = h - cy;
        double rx;
        double ry;
        if (sizeKeyword == null) {
            rx = BackgroundLayers.lengthOf(radiusX, w);
            ry = radiusY == null ? rx : BackgroundLayers.lengthOf(radiusY, h);
            if (circle) {
                ry = rx;
            }
        } else {
            double sideX;
            double sideY;
            double cornerX;
            double cornerY;
            if ("closest-side".equals(sizeKeyword) || "closest-corner".equals(sizeKeyword)) {
                sideX = Math.min(Math.abs(dl), Math.abs(dr));
                sideY = Math.min(Math.abs(dt), Math.abs(db));
                cornerX = Math.min(Math.abs(dl), Math.abs(dr));
                cornerY = Math.min(Math.abs(dt), Math.abs(db));
            } else {
                sideX = Math.max(Math.abs(dl), Math.abs(dr));
                sideY = Math.max(Math.abs(dt), Math.abs(db));
                cornerX = sideX;
                cornerY = sideY;
            }
            boolean corner = sizeKeyword.endsWith("corner");
            if (circle) {
                if (corner) {
                    rx = Math.hypot(cornerX, cornerY);
                } else {
                    rx = "closest-side".equals(sizeKeyword) ? Math.min(sideX, sideY) : Math.max(sideX, sideY);
                }
                ry = rx;
            } else if (corner) {
                // 타원은 같은 변 비율로 모서리를 지난다.
                double sx = Math.max(sideX, 1e-6);
                double sy = Math.max(sideY, 1e-6);
                double scale = Math.sqrt((cornerX / sx) * (cornerX / sx) + (cornerY / sy) * (cornerY / sy));
                rx = sx * scale;
                ry = sy * scale;
            } else {
                rx = sideX;
                ry = sideY;
            }
        }
        if (rx <= 0 || ry <= 0 || Double.isNaN(rx) || Double.isNaN(ry)) {
            // 반지름이 0 이면 마지막 색 하나로 채운다(3.2.1).
            Object[] z = resolve(Math.max(1, rx));
            int[] zc = (int[]) z[1];
            return new Color(zc[zc.length - 1], true);
        }
        Object[] r = resolve(rx);
        double[] pos = (double[]) r[0];
        int[] col = (int[]) r[1];
        double p0 = repeating ? pos[0] : 0;
        double pn = pos[pos.length - 1];
        if (!repeating && pos[0] > 0) {
            // 첫 정지점 안쪽은 첫 색으로 채운다.
            double[] np = new double[pos.length + 1];
            int[] nc = new int[col.length + 1];
            np[0] = 0;
            nc[0] = col[0];
            System.arraycopy(pos, 0, np, 1, pos.length);
            System.arraycopy(col, 0, nc, 1, col.length);
            pos = np;
            col = nc;
        }
        if (pn - p0 < 1e-3) {
            return new Color(col[col.length - 1], true);
        }
        AffineTransform at = new AffineTransform();
        at.translate(cx, cy);
        at.scale(1, ry / rx);
        at.translate(-cx, -cy);
        return new RadialGradientPaint(new Point2D.Double(cx, cy), (float) (pn - p0), new Point2D.Double(cx, cy), fractions(pos, p0, pn), colors(col),
                repeating ? CycleMethod.REPEAT : CycleMethod.NO_CYCLE, java.awt.MultipleGradientPaint.ColorSpaceType.SRGB, at);
    }

    /**
     * Builds the paint of a conic gradient (3.3).
     * <p>
     * 원뿔형 그라디언트의 페인트를 만든다(3.3).
     *
     * @param w image width / 이미지 너비
     * @param h image height / 이미지 높이
     * @return the paint / 페인트
     */
    private Paint conic(double w, double h) {
        double[] c = center(w, h);
        Object[] r = resolve(360);
        double[] pos = (double[]) r[0];
        int[] col = (int[]) r[1];
        int n = 2048;
        int[] lut = new int[n];
        double p0 = pos[0];
        double pn = pos[pos.length - 1];
        // 0..360 도를 표본으로 나눠 위치에 맞는 색을 찾는다(반복 형태는 구간을 되풀이한다).
        for (int i = 0; i < n; i++) {
            double deg = 360.0 * i / n;
            if (repeating && pn - p0 > 1e-6) {
                deg = p0 + ((deg - p0) % (pn - p0) + (pn - p0)) % (pn - p0);
            }
            lut[i] = colorAt(pos, col, deg);
        }
        return new ConicPaint(c[0], c[1], Math.toRadians(from), lut);
    }

    /**
     * Returns the color of a sorted stop list at a position (the end colors extend).
     * <p>
     * 정렬된 정지점 목록에서 한 위치의 색을 반환한다(양 끝 색은 연장된다).
     *
     * @param pos the positions / 위치
     * @param col the colors / 색
     * @param p   the position / 위치
     * @return the ARGB color / ARGB 색
     */
    private static int colorAt(double[] pos, int[] col, double p) {
        if (p <= pos[0]) {
            return col[0];
        }
        // 위치가 속한 구간을 찾아 보간한다.
        for (int i = 1; i < pos.length; i++) {
            if (p <= pos[i]) {
                double span = pos[i] - pos[i - 1];
                return span <= 0 ? col[i] : mix(col[i - 1], col[i], (p - pos[i - 1]) / span);
            }
        }
        return col[col.length - 1];
    }

    /**
     * A conic gradient as a paint: the color of a device pixel follows the angle of the pixel around the center.
     * <p>
     * 원뿔형 그라디언트 페인트: 장치 화소의 색은 중심 둘레의 그 화소의 각도를 따른다.
     */
    private static final class ConicPaint implements Paint {
        /** Center x of the cone in user space. 원뿔 중심의 x 좌표(사용자 공간). */
        private final double cx;
        /** Center y of the cone in user space. 원뿔 중심의 y 좌표(사용자 공간). */
        private final double cy;
        /** Start angle in radians. 시작 각도(라디안). */
        private final double from;
        /** The colors over a full turn. 한 바퀴의 색. */
        private final int[] lut;

        /**
         * Creates the paint.
         * <p>
         * 페인트를 만든다.
         *
         * @param cx   center x / 중심 x
         * @param cy   center y / 중심 y
         * @param from start angle in radians / 시작 각도(라디안)
         * @param lut  colors over a full turn / 한 바퀴의 색
         */
        ConicPaint(double cx, double cy, double from, int[] lut) {
            this.cx = cx;
            this.cy = cy;
            this.from = from;
            this.lut = lut;
        }

        /**
         * Returns the transparency (translucent).
         * <p>
         * 투명도(반투명)를 반환한다.
         *
         * @return the transparency / 투명도
         */
        public int getTransparency() {
            return TRANSLUCENT;
        }

        /**
         * Creates the context that fills rasters.
         * <p>
         * 래스터를 채우는 컨텍스트를 만든다.
         *
         * @param cm           the destination color model / 대상 색 모델
         * @param deviceBounds device bounds / 장치 경계
         * @param userBounds   user bounds / 사용자 경계
         * @param xform        the user to device transform / 사용자 → 장치 변환
         * @param hints        rendering hints / 렌더링 힌트
         * @return the context / 컨텍스트
         */
        public java.awt.PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
            AffineTransform inv;
            try {
                inv = xform.createInverse();
            } catch (NoninvertibleTransformException e) {
                inv = new AffineTransform();
            }
            final AffineTransform fi = inv;
            return new java.awt.PaintContext() {
                /**
                 * Nothing to release.
                 * <p>
                 * 해제할 것이 없다.
                 */
                public void dispose() {
                }

                /**
                 * Returns the color model of the rasters.
                 * <p>
                 * 래스터의 색 모델을 반환한다.
                 *
                 * @return the model / 모델
                 */
                public ColorModel getColorModel() {
                    return ColorModel.getRGBdefault();
                }

                /**
                 * Fills a raster with the colors of the pixels.
                 * <p>
                 * 래스터를 화소의 색으로 채운다.
                 *
                 * @param x left device x / 장치 x
                 * @param y top device y / 장치 y
                 * @param w width / 너비
                 * @param h height / 높이
                 * @return the raster / 래스터
                 */
                public Raster getRaster(int x, int y, int w, int h) {
                    WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);
                    int[] data = ((DataBufferInt) raster.getDataBuffer()).getData();
                    double[] pt = new double[2];
                    // 화소마다 중심에서의 각도로 색을 찾는다.
                    for (int j = 0; j < h; j++) {
                        for (int i = 0; i < w; i++) {
                            pt[0] = x + i + 0.5;
                            pt[1] = y + j + 0.5;
                            fi.transform(pt, 0, pt, 0, 1);
                            double a = Math.atan2(pt[0] - cx, -(pt[1] - cy)) - from;
                            double t = a / (2 * Math.PI);
                            t -= Math.floor(t);
                            data[j * w + i] = lut[Math.min(lut.length - 1, (int) (t * lut.length))];
                        }
                    }
                    return raster;
                }
            };
        }
    }
}
