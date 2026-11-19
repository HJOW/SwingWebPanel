package org.duckdns.hjow.swingwebpanel.render;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.layout.FilterSpec;

/**
 * Applies the filter functions of <code>filter</code> to the pixels of a buffer (CSS Filter Effects Module Level 1,
 * https://www.w3.org/TR/filter-effects-1/, Working Draft 2018-12-18, retrieved 2026-11-17, section 13.1 "Filter functions in SVG terms" and the
 * <code>feGaussianBlur</code> and <code>feColorMatrix</code> primitives of section 9). The functions run in the sRGB color space, in order,
 * each on the output of the previous one, and every result is limited to the range 0..1. The color functions work on colors that are
 * not premultiplied (the pixels are converted and back), <code>blur()</code> and the shadow of <code>drop-shadow()</code> are Gaussian
 * blurs of premultiplied pixels: a true convolution with a separable kernel for a deviation below 2 device pixels and three successive
 * box blurs for larger deviations (the approximation that the specification gives, with the box sizes of the odd and the even case),
 * and the area outside the buffer counts as transparent black. The deviation and the offset of a shadow are lengths of the local
 * coordinate system of the element, so they are scaled by the transform from the document to the device. Java2D public API only (the
 * arithmetic is done on <code>int[]</code> arrays).
 * <p>
 * 버퍼의 픽셀에 <code>filter</code> 의 필터 함수를 적용한다(CSS Filter Effects Module Level 1, https://www.w3.org/TR/filter-effects-1/, 워킹 드래프트 2018-12-18,
 * 조회 2026-11-17, 13.1 절 "Filter functions in SVG terms"와 9 절의 <code>feGaussianBlur</code>, <code>feColorMatrix</code> 프리미티브). 함수는 sRGB 색 공간에서
 * 순서대로 앞 함수의 출력에 적용되며 모든 결과는 0~1 로 제한된다. 색 함수는 미리 곱하지 않은 색에 작동하고(픽셀을 바꿨다가 되돌린다) <code>blur()</code> 와
 * <code>drop-shadow()</code> 의 그림자는 미리 곱한 픽셀의 가우스 흐림이다: 편차가 기기 픽셀 2 미만이면 분리 가능한 커널의 참 합성곱, 그보다 크면 상자 흐림 세 번(규격이
 * 주는 근사. 홀수와 짝수 경우의 상자 크기 포함)이며 버퍼 밖은 투명한 검정으로 본다. 그림자의 편차와 오프셋은 요소의 지역 좌표계 길이이므로 문서에서 기기로의 변환으로
 * 배율을 곱한다. Java2D 공개 API 만 쓴다(계산은 <code>int[]</code> 배열에서 한다).
 */
final class FilterRenderer {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FilterRenderer() {
    }

    /**
     * Applies the filter functions to an image in place.
     * <p>
     * 이미지에 필터 함수를 제자리에서 적용한다.
     *
     * @param img  the buffer (ARGB, not premultiplied) / 버퍼(ARGB, 미리 곱하지 않음)
     * @param spec the filter functions / 필터 함수
     * @param at   the transform from the local coordinates of the element to the pixels of the buffer, only its scale and its direction
     *             are used / 요소의 지역 좌표에서 버퍼 픽셀로의 변환. 배율과 방향만 쓴다
     */
    static void apply(BufferedImage img, FilterSpec spec, AffineTransform at) {
        apply(img, spec, at, null);
    }

    /**
     * Applies the filter functions to an image in place; the references to SVG <code>filter</code> elements of the list are applied with the
     * border box of the element as their user space (CSS Filter Effects 1 section 5), which is why the transform has to include the offset of the
     * image.
     * <p>
     * 이미지에 필터 함수를 제자리에서 적용한다. 목록의 SVG <code>filter</code> 요소 참조는 요소의 border box 를 사용자 공간으로 하여 적용한다(CSS Filter Effects 1 5 절). 그래서
     * 변환에 이미지의 오프셋이 들어 있어야 한다.
     *
     * @param img  the buffer (ARGB, not premultiplied) / 버퍼(ARGB, 미리 곱하지 않음)
     * @param spec the filter functions / 필터 함수
     * @param at   the transform from the document coordinates to the pixels of the buffer / 문서 좌표에서 버퍼 픽셀로의 변환
     * @param box  the border box of the element in document coordinates, or null if the list has no resolved reference / 요소의 border box(문서 좌표). 풀린 참조가 없으면 null
     */
    static void apply(BufferedImage img, FilterSpec spec, AffineTransform at, java.awt.geom.Rectangle2D box) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        premultiply(px);
        double sx = Math.hypot(at.getScaleX(), at.getShearY());
        double sy = Math.hypot(at.getShearX(), at.getScaleY());
        List<FilterSpec.Op> ops = spec.getOps();
        // 함수를 순서대로 적용한다(각 함수는 앞 함수의 출력을 입력으로 받는다).
        for (int i = 0; i < ops.size(); i++) {
            FilterSpec.Op o = ops.get(i);
            switch (o.kind) {
            case FilterSpec.BLUR:
                blur(px, w, h, o.amount * sx, o.amount * sy);
                break;
            case FilterSpec.DROP_SHADOW:
                dropShadow(px, w, h, o, sx, sy, at);
                break;
            case FilterSpec.OPACITY:
                scaleAll(px, o.amount);
                break;
            case FilterSpec.REFERENCE:
                // SVG filter 요소 참조는 지금까지의 결과를 버퍼에 돌려놓고 SVG 필터 엔진이 적용한 뒤 다시 읽는다(풀리지 않은 참조는 건너뛴다).
                if (o.svg != null && box != null) {
                    int[] flat = px.clone();
                    unpremultiply(flat);
                    img.setRGB(0, 0, w, h, flat, 0, w);
                    AffineTransform ctm = new AffineTransform(at);
                    ctm.translate(box.getX(), box.getY());
                    o.svg.applyFilter(o.ref, img, ctm, box.getWidth(), box.getHeight());
                    px = img.getRGB(0, 0, w, h, null, 0, w);
                    premultiply(px);
                }
                break;
            default:
                colorFunction(px, o);
                break;
            }
        }
        unpremultiply(px);
        img.setRGB(0, 0, w, h, px, 0, w);
    }

    /**
     * Converts pixels to premultiplied alpha in place.
     * <p>
     * 픽셀을 미리 곱한 알파로 제자리에서 바꾼다.
     *
     * @param px the pixels (ARGB) / 픽셀(ARGB)
     */
    static void premultiply(int[] px) {
        // 모든 픽셀의 색 채널에 알파를 곱한다.
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            if (a == 255) {
                continue;
            }
            if (a == 0) {
                px[i] = 0;
                continue;
            }
            int r = (((p >> 16) & 0xFF) * a + 127) / 255;
            int g = (((p >> 8) & 0xFF) * a + 127) / 255;
            int b = ((p & 0xFF) * a + 127) / 255;
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Converts pixels back to non-premultiplied alpha in place.
     * <p>
     * 픽셀을 미리 곱하지 않은 알파로 제자리에서 되돌린다.
     *
     * @param px the pixels (premultiplied ARGB) / 픽셀(미리 곱한 ARGB)
     */
    static void unpremultiply(int[] px) {
        // 모든 픽셀의 색 채널을 알파로 나눈다.
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            if (a == 255 || a == 0) {
                if (a == 0) {
                    px[i] = 0;
                }
                continue;
            }
            int r = Math.min(255, (((p >> 16) & 0xFF) * 255 + a / 2) / a);
            int g = Math.min(255, (((p >> 8) & 0xFF) * 255 + a / 2) / a);
            int b = Math.min(255, ((p & 0xFF) * 255 + a / 2) / a);
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Multiplies every channel of premultiplied pixels with a factor (the <code>opacity()</code> function).
     * <p>
     * 미리 곱한 픽셀의 모든 채널에 배율을 곱한다(<code>opacity()</code> 함수).
     *
     * @param px     the pixels / 픽셀
     * @param factor the factor between 0 and 1 / 0~1 의 배율
     */
    private static void scaleAll(int[] px, double factor) {
        int f = (int) Math.round(Math.max(0, Math.min(1, factor)) * 256);
        // 모든 채널에 배율을 곱한다(256 이 1 이다).
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            if (p == 0 || f == 256) {
                continue;
            }
            int a = (((p >>> 24) * f) + 128) >> 8;
            int r = ((((p >> 16) & 0xFF) * f) + 128) >> 8;
            int g = ((((p >> 8) & 0xFF) * f) + 128) >> 8;
            int b = (((p & 0xFF) * f) + 128) >> 8;
            px[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    /**
     * Applies a color function (brightness, contrast, grayscale, hue-rotate, invert, saturate, sepia) to premultiplied pixels.
     * <p>
     * 미리 곱한 픽셀에 색 함수(brightness, contrast, grayscale, hue-rotate, invert, saturate, sepia)를 적용한다.
     *
     * @param px the pixels / 픽셀
     * @param o  the function / 함수
     */
    private static void colorFunction(int[] px, FilterSpec.Op o) {
        float[] m = matrixOf(o);
        float amount = (float) o.amount;
        // 모든 픽셀을 미리 곱하지 않은 색으로 바꿔 함수를 적용하고 다시 미리 곱한다.
        for (int i = 0; i < px.length; i++) {
            int p = px[i];
            int a = p >>> 24;
            if (a == 0) {
                continue;
            }
            float r;
            float g;
            float b;
            if (a == 255) {
                r = ((p >> 16) & 0xFF) / 255f;
                g = ((p >> 8) & 0xFF) / 255f;
                b = (p & 0xFF) / 255f;
            } else {
                r = Math.min(1f, ((p >> 16) & 0xFF) / (float) a);
                g = Math.min(1f, ((p >> 8) & 0xFF) / (float) a);
                b = Math.min(1f, (p & 0xFF) / (float) a);
            }
            float nr;
            float ng;
            float nb;
            if (m != null) {
                nr = m[0] * r + m[1] * g + m[2] * b;
                ng = m[3] * r + m[4] * g + m[5] * b;
                nb = m[6] * r + m[7] * g + m[8] * b;
            } else if (o.kind == FilterSpec.BRIGHTNESS) {
                nr = amount * r;
                ng = amount * g;
                nb = amount * b;
            } else if (o.kind == FilterSpec.CONTRAST) {
                float c = -(0.5f * amount) + 0.5f;
                nr = amount * r + c;
                ng = amount * g + c;
                nb = amount * b + c;
            } else {
                // invert: 표 [amount, 1 - amount] 의 선형 보간이다.
                nr = amount + r * (1 - 2 * amount);
                ng = amount + g * (1 - 2 * amount);
                nb = amount + b * (1 - 2 * amount);
            }
            int ir = clamp8(nr);
            int ig = clamp8(ng);
            int ib = clamp8(nb);
            if (a != 255) {
                ir = (ir * a + 127) / 255;
                ig = (ig * a + 127) / 255;
                ib = (ib * a + 127) / 255;
            }
            px[i] = (a << 24) | (ir << 16) | (ig << 8) | ib;
        }
    }

    /**
     * Converts a channel in 0..1 to 0..255 with clamping and rounding.
     * <p>
     * 0~1 의 채널을 한정하고 반올림하여 0~255 로 바꾼다.
     *
     * @param v the value / 값
     * @return 0..255 / 0~255
     */
    private static int clamp8(float v) {
        int i = (int) (v * 255f + 0.5f);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /**
     * Returns the 3 by 3 matrix of a color function (section 13.1), or null for the functions that are transfer functions of a single
     * channel.
     * <p>
     * 색 함수의 3×3 행렬을 반환한다(13.1 절). 채널 하나의 전달 함수인 함수이면 null.
     *
     * @param o the function / 함수
     * @return the matrix in row order, or null / 행 순서의 행렬. 또는 null
     */
    static float[] matrixOf(FilterSpec.Op o) {
        float a = (float) o.amount;
        switch (o.kind) {
        case FilterSpec.GRAYSCALE: {
            float k = 1 - a;
            return new float[] { 0.2126f + 0.7874f * k, 0.7152f - 0.7152f * k, 0.0722f - 0.0722f * k,
                0.2126f - 0.2126f * k, 0.7152f + 0.2848f * k, 0.0722f - 0.0722f * k,
                0.2126f - 0.2126f * k, 0.7152f - 0.7152f * k, 0.0722f + 0.9278f * k };
        }
        case FilterSpec.SEPIA: {
            float k = 1 - a;
            return new float[] { 0.393f + 0.607f * k, 0.769f - 0.769f * k, 0.189f - 0.189f * k,
                0.349f - 0.349f * k, 0.686f + 0.314f * k, 0.168f - 0.168f * k,
                0.272f - 0.272f * k, 0.534f - 0.534f * k, 0.131f + 0.869f * k };
        }
        case FilterSpec.SATURATE: {
            return new float[] { 0.213f + 0.787f * a, 0.715f - 0.715f * a, 0.072f - 0.072f * a,
                0.213f - 0.213f * a, 0.715f + 0.285f * a, 0.072f - 0.072f * a,
                0.213f - 0.213f * a, 0.715f - 0.715f * a, 0.072f + 0.928f * a };
        }
        case FilterSpec.HUE_ROTATE: {
            double rad = Math.toRadians(o.amount);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            return new float[] { 0.213f + cos * 0.787f - sin * 0.213f, 0.715f - cos * 0.715f - sin * 0.715f, 0.072f - cos * 0.072f + sin * 0.928f,
                0.213f - cos * 0.213f + sin * 0.143f, 0.715f + cos * 0.285f + sin * 0.140f, 0.072f - cos * 0.072f - sin * 0.283f,
                0.213f - cos * 0.213f - sin * 0.787f, 0.715f - cos * 0.715f + sin * 0.715f, 0.072f + cos * 0.928f + sin * 0.072f };
        }
        default:
            return null;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 흐림과 그림자
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Applies <code>drop-shadow()</code>: a blurred, offset copy of the alpha mask in the shadow color is composited below the image.
     * <p>
     * <code>drop-shadow()</code> 를 적용한다: 알파 마스크를 흐리게 하고 옮긴 그림자 색의 복사본을 이미지 아래에 합성한다.
     *
     * @param px the premultiplied pixels (modified) / 미리 곱한 픽셀(바뀜)
     * @param w  width / 너비
     * @param h  height / 높이
     * @param o  the function / 함수
     * @param sx the horizontal scale of the transform / 변환의 가로 배율
     * @param sy the vertical scale of the transform / 세로 배율
     * @param at the transform (for the direction of the offset) / 변환(오프셋 방향용)
     */
    private static void dropShadow(int[] px, int w, int h, FilterSpec.Op o, double sx, double sy, AffineTransform at) {
        int[] sh = new int[px.length];
        // 그림자는 입력의 알파만 쓴다.
        for (int i = 0; i < px.length; i++) {
            sh[i] = px[i] & 0xFF000000;
        }
        blur(sh, w, h, o.deviation * sx, o.deviation * sy);
        Point2D off = at.deltaTransform(new Point2D.Double(o.dx, o.dy), null);
        int ox = (int) Math.round(off.getX());
        int oy = (int) Math.round(off.getY());
        int ca = o.color >>> 24;
        int cr = (o.color >> 16) & 0xFF;
        int cg = (o.color >> 8) & 0xFF;
        int cb = o.color & 0xFF;
        // 그림자를 옮긴 자리에 색을 입혀 이미지 아래에 합성한다(미리 곱한 source-over).
        int[] out = new int[px.length];
        for (int y = 0; y < h; y++) {
            // 한 줄의 모든 픽셀을 훑으며 그림자 색을 입혀 합성한다.
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int p = px[i];
                int sxp = x - ox;
                int syp = y - oy;
                int s = 0;
                if (sxp >= 0 && sxp < w && syp >= 0 && syp < h) {
                    int sa = ((sh[syp * w + sxp] >>> 24) * ca + 127) / 255;
                    if (sa > 0) {
                        s = (sa << 24) | (((cr * sa + 127) / 255) << 16) | (((cg * sa + 127) / 255) << 8) | ((cb * sa + 127) / 255);
                    }
                }
                int pa = p >>> 24;
                if (pa == 255 || s == 0) {
                    out[i] = pa == 255 ? p : (p == 0 ? s : over(p, s));
                } else {
                    out[i] = over(p, s);
                }
            }
        }
        System.arraycopy(out, 0, px, 0, px.length);
    }

    /**
     * Composites a premultiplied source over a premultiplied backdrop.
     * <p>
     * 미리 곱한 원본을 미리 곱한 배경 위에 합성한다.
     *
     * @param src the source / 원본
     * @param dst the backdrop / 배경
     * @return the result / 결과
     */
    private static int over(int src, int dst) {
        int ia = 255 - (src >>> 24);
        int a = (src >>> 24) + (((dst >>> 24) * ia + 127) / 255);
        int r = ((src >> 16) & 0xFF) + ((((dst >> 16) & 0xFF) * ia + 127) / 255);
        int g = ((src >> 8) & 0xFF) + ((((dst >> 8) & 0xFF) * ia + 127) / 255);
        int b = (src & 0xFF) + (((dst & 0xFF) * ia + 127) / 255);
        return (Math.min(255, a) << 24) | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
    }

    /**
     * Blurs premultiplied pixels with a Gaussian of the given deviations (in pixels) in place.
     * <p>
     * 미리 곱한 픽셀을 주어진 편차(픽셀)의 가우스로 제자리에서 흐리게 한다.
     *
     * @param px the pixels / 픽셀
     * @param w  width / 너비
     * @param h  height / 높이
     * @param sx the horizontal standard deviation / 가로 표준 편차
     * @param sy the vertical standard deviation / 세로 표준 편차
     */
    static void blur(int[] px, int w, int h, double sx, double sy) {
        int[] tmp = new int[px.length];
        if (sx > 0) {
            blurAxis(px, tmp, w, h, true, sx);
        }
        if (sy > 0) {
            blurAxis(px, tmp, w, h, false, sy);
        }
    }

    /**
     * Blurs along one axis in place.
     * <p>
     * 한 축을 따라 제자리에서 흐리게 한다.
     *
     * @param px         the pixels / 픽셀
     * @param tmp        a work array of the same size / 같은 크기의 작업 배열
     * @param w          width / 너비
     * @param h          height / 높이
     * @param horizontal true for the horizontal axis / 가로 축이면 true
     * @param s          the standard deviation in pixels / 표준 편차(픽셀)
     */
    private static void blurAxis(int[] px, int[] tmp, int w, int h, boolean horizontal, double s) {
        if (s < 2.0) {
            gaussian(px, tmp, w, h, horizontal, s);
            return;
        }
        int d = (int) Math.floor(s * 3 * Math.sqrt(2 * Math.PI) / 4 + 0.5);
        if (d < 1) {
            return;
        }
        if (d % 2 == 1) {
            // 홀수이면 크기 d 인 상자 흐림 세 번이며 출력 픽셀이 가운데이다.
            box(px, tmp, w, h, horizontal, (d - 1) / 2, (d - 1) / 2);
            box(tmp, px, w, h, horizontal, (d - 1) / 2, (d - 1) / 2);
            box(px, tmp, w, h, horizontal, (d - 1) / 2, (d - 1) / 2);
            System.arraycopy(tmp, 0, px, 0, px.length);
        } else {
            // 짝수이면 크기 d 인 흐림 둘(왼쪽 경계, 오른쪽 경계가 가운데)과 크기 d+1 인 흐림 하나이다.
            box(px, tmp, w, h, horizontal, d / 2, d / 2 - 1);
            box(tmp, px, w, h, horizontal, d / 2 - 1, d / 2);
            box(px, tmp, w, h, horizontal, d / 2, d / 2);
            System.arraycopy(tmp, 0, px, 0, px.length);
        }
    }

    /**
     * One box blur pass: every output pixel is the average of the source pixels from <code>left</code> pixels before to
     * <code>right</code> pixels after it (outside the buffer counts as transparent).
     * <p>
     * 상자 흐림 한 번: 출력 픽셀은 앞쪽 <code>left</code> 픽셀부터 뒤쪽 <code>right</code> 픽셀까지 원본 픽셀의 평균이다(버퍼 밖은 투명으로 본다).
     *
     * @param src        source / 원본
     * @param dst        receives the result / 결과를 받는다
     * @param w          width / 너비
     * @param h          height / 높이
     * @param horizontal true for rows / 가로이면 true
     * @param left       pixels before / 앞쪽 픽셀 수
     * @param right      pixels after / 뒤쪽 픽셀 수
     */
    private static void box(int[] src, int[] dst, int w, int h, boolean horizontal, int left, int right) {
        int lines = horizontal ? h : w;
        int len = horizontal ? w : h;
        int step = horizontal ? 1 : w;
        int size = left + right + 1;
        int half = size / 2;
        // 줄(가로이면 행, 세로이면 열)마다 이동 합으로 평균을 계산한다.
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line;
            int sa = 0;
            int sr = 0;
            int sg = 0;
            int sb = 0;
            // 첫 출력 픽셀의 창(0..right)을 채운다.
            for (int k = 0; k <= right && k < len; k++) {
                int p = src[base + k * step];
                sa += p >>> 24;
                sr += (p >> 16) & 0xFF;
                sg += (p >> 8) & 0xFF;
                sb += p & 0xFF;
            }
            // 출력 픽셀을 하나씩 계산하며 창을 한 칸씩 민다.
            for (int i = 0; i < len; i++) {
                dst[base + i * step] = (((sa + half) / size) << 24) | (((sr + half) / size) << 16) | (((sg + half) / size) << 8) | ((sb + half) / size);
                int add = i + right + 1;
                if (add < len) {
                    int p = src[base + add * step];
                    sa += p >>> 24;
                    sr += (p >> 16) & 0xFF;
                    sg += (p >> 8) & 0xFF;
                    sb += p & 0xFF;
                }
                int sub = i - left;
                if (sub >= 0) {
                    int p = src[base + sub * step];
                    sa -= p >>> 24;
                    sr -= (p >> 16) & 0xFF;
                    sg -= (p >> 8) & 0xFF;
                    sb -= p & 0xFF;
                }
            }
        }
    }

    /**
     * A true Gaussian convolution along one axis for small deviations, in place.
     * <p>
     * 작은 편차를 위한 한 축의 참 가우스 합성곱(제자리).
     *
     * @param px         the pixels / 픽셀
     * @param tmp        a work array / 작업 배열
     * @param w          width / 너비
     * @param h          height / 높이
     * @param horizontal true for rows / 가로이면 true
     * @param s          the standard deviation (below 2) / 표준 편차(2 미만)
     */
    private static void gaussian(int[] px, int[] tmp, int w, int h, boolean horizontal, double s) {
        int radius = (int) Math.ceil(3 * s);
        float[] k = new float[radius * 2 + 1];
        float total = 0;
        // 가우스 가중치를 계산하고 합이 1 이 되게 맞춘다.
        for (int i = -radius; i <= radius; i++) {
            k[i + radius] = (float) Math.exp(-(i * (double) i) / (2 * s * s));
            total += k[i + radius];
        }
        // 가중치를 합이 1 이 되도록 나눈다.
        for (int i = 0; i < k.length; i++) {
            k[i] /= total;
        }
        int lines = horizontal ? h : w;
        int len = horizontal ? w : h;
        int step = horizontal ? 1 : w;
        // 줄마다 모든 출력 픽셀을 이웃의 가중합으로 계산한다.
        for (int line = 0; line < lines; line++) {
            int base = horizontal ? line * w : line;
            // 출력 픽셀마다 이웃 픽셀의 가중합을 구한다.
            for (int i = 0; i < len; i++) {
                float a = 0;
                float r = 0;
                float g = 0;
                float b = 0;
                // 커널 안의 이웃 픽셀을 하나씩 더한다(버퍼 밖은 투명으로 본다).
                for (int j = -radius; j <= radius; j++) {
                    int q = i + j;
                    if (q < 0 || q >= len) {
                        continue;
                    }
                    int p = px[base + q * step];
                    float wt = k[j + radius];
                    a += wt * (p >>> 24);
                    r += wt * ((p >> 16) & 0xFF);
                    g += wt * ((p >> 8) & 0xFF);
                    b += wt * (p & 0xFF);
                }
                tmp[base + i * step] = (Math.min(255, (int) (a + 0.5f)) << 24) | (Math.min(255, (int) (r + 0.5f)) << 16) | (Math.min(255, (int) (g + 0.5f)) << 8)
                        | Math.min(255, (int) (b + 0.5f));
            }
        }
        System.arraycopy(tmp, 0, px, 0, px.length);
    }
}
