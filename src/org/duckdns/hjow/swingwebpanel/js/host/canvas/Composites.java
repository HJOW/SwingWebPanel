package org.duckdns.hjow.swingwebpanel.js.host.canvas;

/**
 * The compositing operators of <code>globalCompositeOperation</code>: the Porter-Duff operators (<code>source-over</code>,
 * <code>source-in</code>, <code>source-out</code>, <code>source-atop</code>, <code>destination-over</code>, <code>destination-in</code>,
 * <code>destination-out</code>, <code>destination-atop</code>, <code>xor</code>, <code>copy</code>), <code>lighter</code> (plus), the separable
 * blend modes (<code>multiply</code>, <code>screen</code>, <code>overlay</code>, <code>darken</code>, <code>lighten</code>,
 * <code>color-dodge</code>, <code>color-burn</code>, <code>hard-light</code>, <code>soft-light</code>, <code>difference</code>,
 * <code>exclusion</code>) and the non-separable ones (<code>hue</code>, <code>saturation</code>, <code>color</code>, <code>luminosity</code>),
 * computed pixel by pixel on non-premultiplied ARGB values with the formulas of the Compositing and Blending specification. All 26 operators of
 * the canvas specification are implemented.
 * <p>
 * <code>globalCompositeOperation</code> 의 합성 연산자: Porter-Duff 연산자(<code>source-over</code>, <code>source-in</code>, <code>source-out</code>,
 * <code>source-atop</code>, <code>destination-over</code>, <code>destination-in</code>, <code>destination-out</code>, <code>destination-atop</code>,
 * <code>xor</code>, <code>copy</code>), <code>lighter</code>(덧셈), 분리 가능한 블렌드 모드(<code>multiply</code>, <code>screen</code>,
 * <code>overlay</code>, <code>darken</code>, <code>lighten</code>, <code>color-dodge</code>, <code>color-burn</code>, <code>hard-light</code>,
 * <code>soft-light</code>, <code>difference</code>, <code>exclusion</code>)와 분리할 수 없는 모드(<code>hue</code>, <code>saturation</code>,
 * <code>color</code>, <code>luminosity</code>)를 Compositing and Blending 규격의 공식으로 알파를 곱하지 않은 ARGB 값에 화소별로 계산한다. 캔버스
 * 규격의 연산자 26 개를 모두 구현한다.
 */
public final class Composites {

    /**
     * The names of the operators; the index is the operator number.
     * <p>
     * 연산자의 이름. 위치가 연산자 번호이다.
     */
    private static final String[] NAMES = { "source-over", "source-in", "source-out", "source-atop", "destination-over", "destination-in",
        "destination-out", "destination-atop", "lighter", "copy", "xor", "multiply", "screen", "overlay", "darken", "lighten", "color-dodge",
        "color-burn", "hard-light", "soft-light", "difference", "exclusion", "hue", "saturation", "color", "luminosity" };

    /**
     * The operator <code>source-over</code>, the default.
     * <p>
     * 기본 연산자 <code>source-over</code>.
     */
    public static final int SOURCE_OVER = 0;

    /**
     * The operator <code>destination-out</code>, which <code>clearRect</code> uses.
     * <p>
     * <code>clearRect</code> 가 쓰는 연산자 <code>destination-out</code>.
     */
    public static final int DESTINATION_OUT = 6;

    /**
     * The first blend mode number; the operators from here on are blend modes.
     * <p>
     * 첫 블렌드 모드 번호. 여기서부터는 블렌드 모드이다.
     */
    private static final int FIRST_BLEND = 11;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Composites() {
    }

    /**
     * Finds the number of an operator by its name.
     * <p>
     * 이름으로 연산자 번호를 찾는다.
     *
     * @param name the operator name / 연산자 이름
     * @return the number, or -1 if the name is not an operator / 번호. 연산자 이름이 아니면 -1
     */
    public static int parse(String name) {
        // 이름 표를 훑어 같은 이름의 위치를 찾는다.
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the name of an operator.
     * <p>
     * 연산자의 이름을 반환한다.
     *
     * @param op the operator number / 연산자 번호
     * @return the name / 이름
     */
    public static String name(int op) {
        return NAMES[op];
    }

    /**
     * Tells whether an operator changes pixels where the source is transparent, so that it must be applied to the whole clipped canvas and not
     * only to the area of the shape (<code>source-in</code>, <code>source-out</code>, <code>destination-in</code>,
     * <code>destination-atop</code> and <code>copy</code>).
     * <p>
     * 연산자가 원본이 투명한 곳의 화소도 바꾸는지, 즉 도형 영역뿐 아니라 클립된 캔버스 전체에 적용해야 하는지 알려 준다(<code>source-in</code>,
     * <code>source-out</code>, <code>destination-in</code>, <code>destination-atop</code>, <code>copy</code>).
     *
     * @param op the operator number / 연산자 번호
     * @return true if the operator affects the whole canvas / 캔버스 전체에 영향을 주면 true
     */
    public static boolean affectsWholeCanvas(int op) {
        return op == 1 || op == 2 || op == 5 || op == 7 || op == 9;
    }

    /**
     * Composites one source pixel onto one destination pixel.
     * <p>
     * 원본 화소 하나를 대상 화소 하나에 합성한다.
     *
     * @param op  the operator number / 연산자 번호
     * @param src the source pixel, non-premultiplied ARGB / 원본 화소(알파를 곱하지 않은 ARGB)
     * @param dst the destination pixel, non-premultiplied ARGB / 대상 화소(알파를 곱하지 않은 ARGB)
     * @return the resulting pixel, non-premultiplied ARGB / 결과 화소(알파를 곱하지 않은 ARGB)
     */
    public static int apply(int op, int src, int dst) {
        double as = (src >>> 24) / 255.0;
        double ab = (dst >>> 24) / 255.0;
        double[] cs = { ((src >> 16) & 0xFF) / 255.0, ((src >> 8) & 0xFF) / 255.0, (src & 0xFF) / 255.0 };
        double[] cb = { ((dst >> 16) & 0xFF) / 255.0, ((dst >> 8) & 0xFF) / 255.0, (dst & 0xFF) / 255.0 };
        double ao;
        double[] co = new double[3];
        if (op >= FIRST_BLEND) {
            // 블렌드 모드는 source-over 합성에 섞은 색 B(cb, cs)를 넣는다.
            double[] mixed = blend(op, cb, cs);
            ao = as + ab * (1 - as);
            // 세 채널을 알파를 곱한 값으로 계산한다.
            for (int i = 0; i < 3; i++) {
                co[i] = as * (1 - ab) * cs[i] + as * ab * mixed[i] + (1 - as) * ab * cb[i];
            }
        } else if (op == 8) {
            // lighter 는 알파를 곱한 값끼리 더하고 1 로 자른다.
            ao = Math.min(1, as + ab);
            for (int i = 0; i < 3; i++) {
                co[i] = Math.min(1, cs[i] * as + cb[i] * ab);
            }
        } else {
            double fa;
            double fb;
            switch (op) {
            case 1:
                fa = ab;
                fb = 0;
                break;
            case 2:
                fa = 1 - ab;
                fb = 0;
                break;
            case 3:
                fa = ab;
                fb = 1 - as;
                break;
            case 4:
                fa = 1 - ab;
                fb = 1;
                break;
            case 5:
                fa = 0;
                fb = as;
                break;
            case 6:
                fa = 0;
                fb = 1 - as;
                break;
            case 7:
                fa = 1 - ab;
                fb = as;
                break;
            case 9:
                fa = 1;
                fb = 0;
                break;
            case 10:
                fa = 1 - ab;
                fb = 1 - as;
                break;
            default:
                fa = 1;
                fb = 1 - as;
                break;
            }
            ao = as * fa + ab * fb;
            // 세 채널을 알파를 곱한 값으로 계산한다.
            for (int i = 0; i < 3; i++) {
                co[i] = cs[i] * as * fa + cb[i] * ab * fb;
            }
        }
        if (ao <= 0) {
            return 0;
        }
        int a = clamp(ao * 255);
        return (a << 24) | (clamp(co[0] / ao * 255) << 16) | (clamp(co[1] / ao * 255) << 8) | clamp(co[2] / ao * 255);
    }

    /**
     * Mixes a changed pixel with the old one by a coverage (the clip mask), in premultiplied space.
     * <p>
     * 바뀐 화소와 옛 화소를 덮는 비율(클립 마스크)로 섞는다. 알파를 곱한 공간에서 계산한다.
     *
     * @param old      the old pixel / 옛 화소
     * @param changed  the new pixel / 새 화소
     * @param coverage the coverage 0..255 / 비율 0..255
     * @return the mixed pixel / 섞은 화소
     */
    public static int mix(int old, int changed, int coverage) {
        if (coverage >= 255) {
            return changed;
        }
        if (coverage <= 0) {
            return old;
        }
        double m = coverage / 255.0;
        double ao = (old >>> 24) / 255.0;
        double an = (changed >>> 24) / 255.0;
        double a = ao * (1 - m) + an * m;
        if (a <= 0) {
            return 0;
        }
        int out = clamp(a * 255) << 24;
        // 세 채널을 알파를 곱한 값으로 섞은 뒤 알파로 나눈다.
        for (int shift = 16; shift >= 0; shift -= 8) {
            double co = ((old >> shift) & 0xFF) * ao * (1 - m) + ((changed >> shift) & 0xFF) * an * m;
            out |= clamp(co / a) << shift;
        }
        return out;
    }

    /**
     * Rounds and clamps a channel value to 0..255.
     * <p>
     * 채널 값을 반올림하고 0..255 로 자른다.
     *
     * @param v the value / 값
     * @return the integer / 정수
     */
    private static int clamp(double v) {
        int i = (int) Math.floor(v + 0.5);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /**
     * Computes the blend function B(cb, cs) of a blend mode for the three channels.
     * <p>
     * 블렌드 모드의 블렌드 함수 B(cb, cs)를 세 채널에 대해 계산한다.
     *
     * @param op the blend mode number / 블렌드 모드 번호
     * @param cb the backdrop color / 배경 색
     * @param cs the source color / 원본 색
     * @return the mixed color / 섞은 색
     */
    private static double[] blend(int op, double[] cb, double[] cs) {
        double[] r = new double[3];
        if (op >= 22) {
            return nonSeparable(op, cb, cs);
        }
        // 분리 가능한 모드는 채널마다 따로 계산한다.
        for (int i = 0; i < 3; i++) {
            r[i] = separable(op, cb[i], cs[i]);
        }
        return r;
    }

    /**
     * Computes a separable blend function for one channel.
     * <p>
     * 분리 가능한 블렌드 함수를 한 채널에 대해 계산한다.
     *
     * @param op the blend mode number / 블렌드 모드 번호
     * @param b  the backdrop channel / 배경 채널
     * @param s  the source channel / 원본 채널
     * @return the result / 결과
     */
    private static double separable(int op, double b, double s) {
        switch (op) {
        case 11:
            return b * s;
        case 12:
            return b + s - b * s;
        case 13:
            return separable(18, s, b);
        case 14:
            return Math.min(b, s);
        case 15:
            return Math.max(b, s);
        case 16:
            if (b == 0) {
                return 0;
            }
            return s >= 1 ? 1 : Math.min(1, b / (1 - s));
        case 17:
            if (b >= 1) {
                return 1;
            }
            return s <= 0 ? 0 : 1 - Math.min(1, (1 - b) / s);
        case 18:
            return s <= 0.5 ? b * (2 * s) : (b + (2 * s - 1) - b * (2 * s - 1));
        case 19:
            return softLight(b, s);
        case 20:
            return Math.abs(b - s);
        default:
            return b + s - 2 * b * s;
        }
    }

    /**
     * Computes the soft-light blend function of one channel.
     * <p>
     * soft-light 블렌드 함수를 한 채널에 대해 계산한다.
     *
     * @param b the backdrop channel / 배경 채널
     * @param s the source channel / 원본 채널
     * @return the result / 결과
     */
    private static double softLight(double b, double s) {
        if (s <= 0.5) {
            return b - (1 - 2 * s) * b * (1 - b);
        }
        double d = b <= 0.25 ? ((16 * b - 12) * b + 4) * b : Math.sqrt(b);
        return b + (2 * s - 1) * (d - b);
    }

    /**
     * Computes a non-separable blend function.
     * <p>
     * 분리할 수 없는 블렌드 함수를 계산한다.
     *
     * @param op the blend mode number (hue, saturation, color or luminosity) / 블렌드 모드 번호(hue, saturation, color, luminosity)
     * @param cb the backdrop color / 배경 색
     * @param cs the source color / 원본 색
     * @return the result / 결과
     */
    private static double[] nonSeparable(int op, double[] cb, double[] cs) {
        switch (op) {
        case 22:
            return setLum(setSat(cs, sat(cb)), lum(cb));
        case 23:
            return setLum(setSat(cb, sat(cs)), lum(cb));
        case 24:
            return setLum(cs, lum(cb));
        default:
            return setLum(cb, lum(cs));
        }
    }

    /**
     * Computes the luminosity of a color.
     * <p>
     * 색의 휘도를 계산한다.
     *
     * @param c the color / 색
     * @return the luminosity / 휘도
     */
    private static double lum(double[] c) {
        return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2];
    }

    /**
     * Computes the saturation of a color.
     * <p>
     * 색의 채도를 계산한다.
     *
     * @param c the color / 색
     * @return the saturation / 채도
     */
    private static double sat(double[] c) {
        return Math.max(c[0], Math.max(c[1], c[2])) - Math.min(c[0], Math.min(c[1], c[2]));
    }

    /**
     * Changes the luminosity of a color and clips the result into the gamut.
     * <p>
     * 색의 휘도를 바꾸고 결과를 색 범위 안으로 자른다.
     *
     * @param c the color / 색
     * @param l the new luminosity / 새 휘도
     * @return the new color / 새 색
     */
    private static double[] setLum(double[] c, double l) {
        double d = l - lum(c);
        double[] r = { c[0] + d, c[1] + d, c[2] + d };
        double ll = lum(r);
        double n = Math.min(r[0], Math.min(r[1], r[2]));
        double x = Math.max(r[0], Math.max(r[1], r[2]));
        // 범위를 벗어난 색을 휘도를 유지하며 안으로 모은다.
        if (n < 0) {
            for (int i = 0; i < 3; i++) {
                r[i] = ll + (r[i] - ll) * ll / (ll - n);
            }
        }
        if (x > 1) {
            // 1 을 넘는 색도 같은 방식으로 안쪽으로 모은다.
            for (int i = 0; i < 3; i++) {
                r[i] = ll + (r[i] - ll) * (1 - ll) / (x - ll);
            }
        }
        return r;
    }

    /**
     * Changes the saturation of a color.
     * <p>
     * 색의 채도를 바꾼다.
     *
     * @param c the color / 색
     * @param s the new saturation / 새 채도
     * @return the new color / 새 색
     */
    private static double[] setSat(double[] c, double s) {
        int max = 0;
        int min = 0;
        // 가장 큰 채널과 가장 작은 채널의 위치를 찾는다.
        for (int i = 1; i < 3; i++) {
            if (c[i] > c[max]) {
                max = i;
            }
            if (c[i] < c[min]) {
                min = i;
            }
        }
        double[] r = new double[3];
        if (max == min) {
            return r;
        }
        int mid = 3 - max - min;
        double cmax = c[max];
        double cmin = c[min];
        double cmid = c[mid];
        r[mid] = (cmid - cmin) * s / (cmax - cmin);
        r[max] = s;
        r[min] = 0;
        return r;
    }
}
