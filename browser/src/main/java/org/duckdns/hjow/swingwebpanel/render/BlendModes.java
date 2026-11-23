package org.duckdns.hjow.swingwebpanel.render;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * The blend modes of CSS Compositing and Blending Level 1 (https://www.w3.org/TR/compositing-1/, Candidate Recommendation Draft 2024-03-21,
 * retrieved 2026-11-17, section 10 "Blending"): the separable modes <code>multiply</code>, <code>screen</code>, <code>overlay</code>,
 * <code>darken</code>, <code>lighten</code>, <code>color-dodge</code>, <code>color-burn</code>, <code>hard-light</code>,
 * <code>soft-light</code>, <code>difference</code> and <code>exclusion</code>, the non-separable modes <code>hue</code>,
 * <code>saturation</code>, <code>color</code> and <code>luminosity</code>, and <code>plus-lighter</code> (the sum of the premultiplied
 * colors, limited to 1). {@link #blend} computes the result of drawing one source pixel over a backdrop pixel with the formula of section
 * 9 (source over with the blended color): <code>Cr = (1 - as/ar) * Cb + as/ar * ((1 - ab) * Cs + ab * B(Cb, Cs))</code>,
 * <code>ar = as + ab * (1 - as)</code>. {@link #composite} wraps that in a {@link Composite} that is used to draw the buffer of a group
 * onto the graphics, so that the group is blended with what has been painted so far. Java2D public API only.
 * <p>
 * CSS Compositing and Blending Level 1 의 혼합 모드(https://www.w3.org/TR/compositing-1/, Candidate Recommendation Draft 2024-03-21, 조회 2026-11-17, 10 절 "Blending"):
 * 분리 가능한 모드 <code>multiply</code>, <code>screen</code>, <code>overlay</code>, <code>darken</code>, <code>lighten</code>, <code>color-dodge</code>,
 * <code>color-burn</code>, <code>hard-light</code>, <code>soft-light</code>, <code>difference</code>, <code>exclusion</code>, 분리 불가능한 모드 <code>hue</code>,
 * <code>saturation</code>, <code>color</code>, <code>luminosity</code>, 그리고 <code>plus-lighter</code>(미리 곱한 색의 합, 1 로 제한). {@link #blend} 는 9 절의 식(혼합한 색을 쓰는
 * source over)으로 원본 픽셀 하나를 배경 픽셀 위에 그린 결과를 계산한다. {@link #composite} 는 이것을 {@link Composite} 로 감싸 그룹의 버퍼를 그래픽스에 그릴 때 쓰며,
 * 그룹은 지금까지 그려진 것과 혼합된다. Java2D 공개 API 만 쓴다.
 */
public final class BlendModes {

    /** Constant of the blend mode <code>normal</code>. 혼합 모드 <code>normal</code> 의 상수. */
    public static final int NORMAL = 0;
    /** Constant of the blend mode <code>multiply</code>. 혼합 모드 <code>multiply</code> 의 상수. */
    public static final int MULTIPLY = 1;
    /** Constant of the blend mode <code>screen</code>. 혼합 모드 <code>screen</code> 의 상수. */
    public static final int SCREEN = 2;
    /** Constant of the blend mode <code>overlay</code>. 혼합 모드 <code>overlay</code> 의 상수. */
    public static final int OVERLAY = 3;
    /** Constant of the blend mode <code>darken</code>. 혼합 모드 <code>darken</code> 의 상수. */
    public static final int DARKEN = 4;
    /** Constant of the blend mode <code>lighten</code>. 혼합 모드 <code>lighten</code> 의 상수. */
    public static final int LIGHTEN = 5;
    /** Constant of the blend mode <code>color-dodge</code>. 혼합 모드 <code>color-dodge</code> 의 상수. */
    public static final int COLOR_DODGE = 6;
    /** Constant of the blend mode <code>color-burn</code>. 혼합 모드 <code>color-burn</code> 의 상수. */
    public static final int COLOR_BURN = 7;
    /** Constant of the blend mode <code>hard-light</code>. 혼합 모드 <code>hard-light</code> 의 상수. */
    public static final int HARD_LIGHT = 8;
    /** Constant of the blend mode <code>soft-light</code>. 혼합 모드 <code>soft-light</code> 의 상수. */
    public static final int SOFT_LIGHT = 9;
    /** Constant of the blend mode <code>difference</code>. 혼합 모드 <code>difference</code> 의 상수. */
    public static final int DIFFERENCE = 10;
    /** Constant of the blend mode <code>exclusion</code>. 혼합 모드 <code>exclusion</code> 의 상수. */
    public static final int EXCLUSION = 11;
    /** Constant of the blend mode <code>hue</code>. 혼합 모드 <code>hue</code> 의 상수. */
    public static final int HUE = 12;
    /** Constant of the blend mode <code>saturation</code>. 혼합 모드 <code>saturation</code> 의 상수. */
    public static final int SATURATION = 13;
    /** Constant of the blend mode <code>color</code>. 혼합 모드 <code>color</code> 의 상수. */
    public static final int COLOR = 14;
    /** Constant of the blend mode <code>luminosity</code>. 혼합 모드 <code>luminosity</code> 의 상수. */
    public static final int LUMINOSITY = 15;
    /** Constant of the blend mode <code>plus-lighter</code>. 혼합 모드 <code>plus-lighter</code> 의 상수. */
    public static final int PLUS_LIGHTER = 16;

    /** The names of the modes by constant. 상수별 모드 이름. */
    private static final String[] NAMES = { "normal", "multiply", "screen", "overlay", "darken", "lighten", "color-dodge", "color-burn", "hard-light",
        "soft-light", "difference", "exclusion", "hue", "saturation", "color", "luminosity", "plus-lighter" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BlendModes() {
    }

    /**
     * Returns the constant of a mode name.
     * <p>
     * 모드 이름의 상수를 반환한다.
     *
     * @param name the keyword of <code>mix-blend-mode</code> / <code>mix-blend-mode</code> 키워드
     * @return the constant, {@link #NORMAL} for an unknown name / 상수. 모르는 이름이면 {@link #NORMAL}
     */
    public static int modeOf(String name) {
        // 이름 목록에서 같은 이름을 찾는다.
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return NORMAL;
    }

    /**
     * Creates the composite that draws with a blend mode and an opacity.
     * <p>
     * 혼합 모드와 불투명도로 그리는 합성기를 만든다.
     *
     * @param mode  the mode constant / 모드 상수
     * @param alpha the opacity of the group between 0 and 1 / 0~1 의 그룹 불투명도
     * @return the composite / 합성기
     */
    public static Composite composite(final int mode, final float alpha) {
        return new Composite() {
            /**
             * Creates the context that composes rasters.
             * <p>
             * 래스터를 합성하는 컨텍스트를 만든다.
             *
             * @param srcColorModel color model of the source / 원본의 색 모델
             * @param dstColorModel color model of the destination / 대상의 색 모델
             * @param hints         rendering hints / 렌더링 힌트
             * @return the context / 컨텍스트
             */
            public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, RenderingHints hints) {
                return new CompositeContext() {
                    /** Nothing to release. 해제할 것이 없다. */
                    public void dispose() {
                    }

                    /**
                     * Blends the source raster over the backdrop raster pixel by pixel.
                     * <p>
                     * 원본 래스터를 배경 래스터 위에 픽셀 단위로 혼합한다.
                     *
                     * @param src    the source / 원본
                     * @param dstIn  the backdrop / 배경
                     * @param dstOut receives the result / 결과를 받는다
                     */
                    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                        int w = Math.min(src.getWidth(), Math.min(dstIn.getWidth(), dstOut.getWidth()));
                        int h = Math.min(src.getHeight(), Math.min(dstIn.getHeight(), dstOut.getHeight()));
                        Object sp = null;
                        Object dp = null;
                        Object op = null;
                        // 모든 픽셀을 훑으며 원본과 배경을 혼합해 결과를 쓴다.
                        for (int y = 0; y < h; y++) {
                            for (int x = 0; x < w; x++) {
                                sp = src.getDataElements(src.getMinX() + x, src.getMinY() + y, sp);
                                dp = dstIn.getDataElements(dstIn.getMinX() + x, dstIn.getMinY() + y, dp);
                                int s = srcColorModel.getRGB(sp);
                                int d = dstColorModel.getRGB(dp);
                                int r = (s >>> 24) == 0 && alpha > 0 ? d : blend(mode, s, d, alpha);
                                op = dstColorModel.getDataElements(r, op);
                                dstOut.setDataElements(dstOut.getMinX() + x, dstOut.getMinY() + y, op);
                            }
                        }
                    }
                };
            }
        };
    }

    /**
     * Draws one source pixel over a backdrop pixel with a blend mode and a group opacity (CSS Compositing 1 section 9).
     * <p>
     * 원본 픽셀 하나를 혼합 모드와 그룹 불투명도로 배경 픽셀 위에 그린다(CSS Compositing 1 9 절).
     *
     * @param mode  the mode constant / 모드 상수
     * @param src   the source color, ARGB not premultiplied / 원본 색(ARGB, 미리 곱하지 않음)
     * @param dst   the backdrop color, ARGB not premultiplied / 배경 색(ARGB, 미리 곱하지 않음)
     * @param alpha the opacity of the group / 그룹 불투명도
     * @return the result, ARGB not premultiplied / 결과(ARGB, 미리 곱하지 않음)
     */
    public static int blend(int mode, int src, int dst, float alpha) {
        float as = ((src >>> 24) / 255f) * alpha;
        float ab = (dst >>> 24) / 255f;
        float[] cs = { ((src >> 16) & 0xFF) / 255f, ((src >> 8) & 0xFF) / 255f, (src & 0xFF) / 255f };
        float[] cb = { ((dst >> 16) & 0xFF) / 255f, ((dst >> 8) & 0xFF) / 255f, (dst & 0xFF) / 255f };
        float ar = as + ab * (1 - as);
        if (ar <= 0) {
            return 0;
        }
        float[] cr = new float[3];
        if (mode == PLUS_LIGHTER) {
            // 미리 곱한 색을 더하고 1 로 제한한다.
            float pa = Math.min(1f, as + ab);
            for (int i = 0; i < 3; i++) {
                float p = Math.min(1f, cs[i] * as + cb[i] * ab);
                cr[i] = pa > 0 ? Math.min(1f, p / pa) : 0;
            }
            return pack(pa, cr);
        }
        float[] b = blended(mode, cb, cs);
        float k = as / ar;
        // 채널마다 Cr = (1 - as/ar) * Cb + as/ar * ((1 - ab) * Cs + ab * B(Cb, Cs)) 로 계산한다.
        for (int i = 0; i < 3; i++) {
            cr[i] = (1 - k) * cb[i] + k * ((1 - ab) * cs[i] + ab * b[i]);
        }
        return pack(ar, cr);
    }

    /**
     * Packs a color into ARGB.
     * <p>
     * 색을 ARGB 로 묶는다.
     *
     * @param a  alpha / 알파
     * @param c  the three channels / 세 채널
     * @return the ARGB value / ARGB 값
     */
    private static int pack(float a, float[] c) {
        return (to8(a) << 24) | (to8(c[0]) << 16) | (to8(c[1]) << 8) | to8(c[2]);
    }

    /**
     * Converts a channel in 0..1 to 0..255 with rounding.
     * <p>
     * 0~1 의 채널을 0~255 로 반올림하여 바꾼다.
     *
     * @param v the value / 값
     * @return 0..255 / 0~255
     */
    private static int to8(float v) {
        int i = (int) (v * 255f + 0.5f);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /**
     * Computes the blend function B(Cb, Cs) of a mode for the three channels.
     * <p>
     * 모드의 혼합 함수 B(Cb, Cs) 를 세 채널에 대해 계산한다.
     *
     * @param mode the mode / 모드
     * @param cb   the backdrop color / 배경 색
     * @param cs   the source color / 원본 색
     * @return the blended color / 혼합된 색
     */
    static float[] blended(int mode, float[] cb, float[] cs) {
        if (mode >= HUE && mode <= LUMINOSITY) {
            return nonSeparable(mode, cb, cs);
        }
        float[] r = new float[3];
        // 분리 가능한 모드는 채널마다 같은 식을 적용한다.
        for (int i = 0; i < 3; i++) {
            r[i] = separable(mode, cb[i], cs[i]);
        }
        return r;
    }

    /**
     * Computes the blend function of a separable mode for one channel (section 10.1).
     * <p>
     * 분리 가능한 모드의 혼합 함수를 채널 하나에 대해 계산한다(10.1 절).
     *
     * @param mode the mode / 모드
     * @param cb   the backdrop channel / 배경 채널
     * @param cs   the source channel / 원본 채널
     * @return the result / 결과
     */
    static float separable(int mode, float cb, float cs) {
        switch (mode) {
        case MULTIPLY:
            return cb * cs;
        case SCREEN:
            return cb + cs - cb * cs;
        case OVERLAY:
            return hardLight(cs, cb);
        case DARKEN:
            return Math.min(cb, cs);
        case LIGHTEN:
            return Math.max(cb, cs);
        case COLOR_DODGE:
            if (cb == 0) {
                return 0;
            }
            return cs >= 1 ? 1 : Math.min(1f, cb / (1 - cs));
        case COLOR_BURN:
            if (cb >= 1) {
                return 1;
            }
            return cs <= 0 ? 0 : 1 - Math.min(1f, (1 - cb) / cs);
        case HARD_LIGHT:
            return hardLight(cb, cs);
        case SOFT_LIGHT:
            if (cs <= 0.5f) {
                return cb - (1 - 2 * cs) * cb * (1 - cb);
            }
            float d = cb <= 0.25f ? ((16 * cb - 12) * cb + 4) * cb : (float) Math.sqrt(cb);
            return cb + (2 * cs - 1) * (d - cb);
        case DIFFERENCE:
            return Math.abs(cb - cs);
        case EXCLUSION:
            return cb + cs - 2 * cb * cs;
        default:
            return cs;
        }
    }

    /**
     * The hard-light function: multiplies or screens depending on the source channel.
     * <p>
     * hard-light 함수: 원본 채널에 따라 곱하거나 스크린한다.
     *
     * @param cb the backdrop channel / 배경 채널
     * @param cs the source channel / 원본 채널
     * @return the result / 결과
     */
    private static float hardLight(float cb, float cs) {
        if (cs <= 0.5f) {
            return cb * (2 * cs);
        }
        float s = 2 * cs - 1;
        return cb + s - cb * s;
    }

    /**
     * Computes the blend function of a non-separable mode (section 10.2).
     * <p>
     * 분리 불가능한 모드의 혼합 함수를 계산한다(10.2 절).
     *
     * @param mode the mode / 모드
     * @param cb   the backdrop color / 배경 색
     * @param cs   the source color / 원본 색
     * @return the blended color / 혼합된 색
     */
    private static float[] nonSeparable(int mode, float[] cb, float[] cs) {
        if (mode == HUE) {
            return setLum(setSat(cs, sat(cb)), lum(cb));
        }
        if (mode == SATURATION) {
            return setLum(setSat(cb, sat(cs)), lum(cb));
        }
        if (mode == COLOR) {
            return setLum(cs, lum(cb));
        }
        return setLum(cb, lum(cs));
    }

    /**
     * Returns the luminosity of a color: <code>0.3 R + 0.59 G + 0.11 B</code>.
     * <p>
     * 색의 밝기를 반환한다: <code>0.3 R + 0.59 G + 0.11 B</code>.
     *
     * @param c the color / 색
     * @return the luminosity / 밝기
     */
    private static float lum(float[] c) {
        return 0.3f * c[0] + 0.59f * c[1] + 0.11f * c[2];
    }

    /**
     * Returns the saturation of a color: the largest minus the smallest channel.
     * <p>
     * 색의 채도를 반환한다: 가장 큰 채널에서 가장 작은 채널을 뺀 값.
     *
     * @param c the color / 색
     * @return the saturation / 채도
     */
    private static float sat(float[] c) {
        return Math.max(c[0], Math.max(c[1], c[2])) - Math.min(c[0], Math.min(c[1], c[2]));
    }

    /**
     * Clips a color into the gamut while keeping its luminosity (ClipColor).
     * <p>
     * 밝기를 유지하며 색을 허용 범위 안으로 자른다(ClipColor).
     *
     * @param c the color / 색
     * @return the clipped color (a new array) / 자른 색(새 배열)
     */
    private static float[] clipColor(float[] c) {
        float l = lum(c);
        float n = Math.min(c[0], Math.min(c[1], c[2]));
        float x = Math.max(c[0], Math.max(c[1], c[2]));
        float[] r = { c[0], c[1], c[2] };
        if (n < 0) {
            // 음수 채널을 밝기 쪽으로 당긴다.
            for (int i = 0; i < 3; i++) {
                r[i] = l + (r[i] - l) * l / (l - n);
            }
        }
        if (x > 1) {
            // 1 을 넘는 채널을 밝기 쪽으로 당긴다.
            for (int i = 0; i < 3; i++) {
                r[i] = l + (r[i] - l) * (1 - l) / (x - l);
            }
        }
        return r;
    }

    /**
     * Gives a color a luminosity (SetLum).
     * <p>
     * 색에 밝기를 준다(SetLum).
     *
     * @param c the color / 색
     * @param l the luminosity / 밝기
     * @return the new color / 새 색
     */
    private static float[] setLum(float[] c, float l) {
        float d = l - lum(c);
        return clipColor(new float[] { c[0] + d, c[1] + d, c[2] + d });
    }

    /**
     * Gives a color a saturation (SetSat).
     * <p>
     * 색에 채도를 준다(SetSat).
     *
     * @param c the color / 색
     * @param s the saturation / 채도
     * @return the new color / 새 색
     */
    private static float[] setSat(float[] c, float s) {
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
        float[] r = new float[3];
        if (c[max] > c[min]) {
            int mid = 3 - max - min;
            if (max == min) {
                mid = 0;
            }
            r[mid] = (c[mid] - c[min]) * s / (c[max] - c[min]);
            r[max] = s;
            r[min] = 0;
        }
        return r;
    }
}
