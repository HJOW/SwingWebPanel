package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * The colour conversion of an AVIF picture (plan item P18-03): it converts the decoded YUV (or monochrome) planes to 8 bit sRGB ARGB pixels. It
 * handles the matrix coefficients of ISO/IEC 23091-2 that matter for still images (identity, BT.709, BT.601 and its relatives, SMPTE 240M, BT.2020
 * non-constant luminance, YCgCo), the full and limited sample ranges, any bit depth of 8, 10 and 12, the chroma subsampling 4:2:0, 4:2:2 and 4:4:4
 * (with bilinear chroma upsampling) and an optional alpha plane. Colour primaries and transfer functions are not converted here: the pixels
 * that this class makes are still in the colour space that the file describes, and {@link AvifDecoder} converts them to sRGB afterwards
 * ({@link org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile}: an embedded ICC profile, or the primaries and transfer characteristics of the
 * <code>nclx</code> colour description).
 * <p>
 * AVIF 화면의 색 변환(계획 P18-03): 디코딩된 YUV(또는 단색) 평면을 8비트 sRGB ARGB 픽셀로 바꾼다. 정지 영상에서 중요한 ISO/IEC 23091-2 의 행렬
 * 계수(항등, BT.709, BT.601과 그 친척, SMPTE 240M, BT.2020 비정상 휘도, YCgCo), 전체/제한 샘플 범위, 8/10/12비트 깊이, 색차 하위 표본화
 * 4:2:0/4:2:2/4:4:4(이중 선형 색차 업샘플링), 선택적 알파 평면을 처리한다. 색 원색과 전달 함수는 여기서 변환하지 않는다. 이 클래스가 만드는
 * 픽셀은 아직 파일이 기술하는 색 공간에 있으며 {@link AvifDecoder} 가 그 뒤에 sRGB 로 바꾼다
 * ({@link org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile}: 내장 ICC 프로파일, 또는 <code>nclx</code> 색 설명의 원색과 전달 특성).
 */
final class AvifColor {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스.
     */
    private AvifColor() {
    }

    /**
     * Converts a picture (and an optional alpha picture) to ARGB pixels.
     * <p>
     * 화면(과 선택적 알파 화면)을 ARGB 픽셀로 변환한다.
     *
     * @param pic           the colour picture / 색 화면
     * @param alpha         the alpha picture with the same size or null / 크기가 같은 알파 화면 또는 null
     * @param premultiplied true if the colour samples are premultiplied by the alpha / 색 샘플에 알파가 미리 곱해져 있으면 true
     * @return the pixels, row by row / 행 순서의 픽셀
     * @throws MediaException UNSUPPORTED_FORMAT for an unsupported matrix, CORRUPT for an alpha picture of another size, CANCELLED if interrupted / 지원하지 않는 행렬이면 UNSUPPORTED_FORMAT, 알파 화면 크기가 다르면 CORRUPT, 인터럽트되면 CANCELLED
     */
    static int[] toArgb(Av1Picture pic, Av1Picture alpha, boolean premultiplied) throws MediaException {
        int w = pic.width;
        int h = pic.height;
        if (alpha != null && (alpha.width != w || alpha.height != h)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the alpha image has another size");
        }
        int mc = pic.matrixCoefficients;
        double kr = 0.299;
        double kb = 0.114;
        boolean identity = false;
        boolean ycgco = false;
        if (mc == 0) {
            identity = true;
        } else if (mc == 1) {
            kr = 0.2126;
            kb = 0.0722;
        } else if (mc == 4) {
            kr = 0.30;
            kb = 0.11;
        } else if (mc == 7) {
            kr = 0.212;
            kb = 0.087;
        } else if (mc == 9) {
            kr = 0.2627;
            kb = 0.0593;
        } else if (mc == 8) {
            ycgco = true;
        } else if (mc != 2 && mc != 5 && mc != 6) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "AVIF: unsupported matrix coefficients " + mc);
        }
        if (identity && (pic.numPlanes < 3 || pic.subX != 0 || pic.subY != 0)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "AVIF: the identity matrix needs 4:4:4 samples");
        }
        int bd = pic.bitDepth;
        int shift = bd - 8;
        double yOff;
        double yRange;
        double cOff;
        double cRange;
        if (pic.fullRange) {
            yOff = 0;
            yRange = (1 << bd) - 1;
            cOff = 1 << (bd - 1);
            cRange = (1 << bd) - 1;
        } else {
            yOff = 16 << shift;
            yRange = 219 << shift;
            cOff = 128 << shift;
            cRange = 224 << shift;
        }
        double crCoef = 2 * (1 - kr);
        double cbCoef = 2 * (1 - kb);
        double kg = 1 - kr - kb;
        int[] argb = new int[w * h];
        int cw = pic.planeWidth(1 < pic.numPlanes ? 1 : 0);
        int ch = pic.planeHeight(1 < pic.numPlanes ? 1 : 0);
        int[] uRow = new int[w];
        int[] vRow = new int[w];
        int[] mix = new int[Math.max(cw, 1)];
        // 행마다 색차를 보간하고 픽셀을 변환한다.
        for (int y = 0; y < h; y++) {
            if ((y & 63) == 0 && Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "AVIF: conversion was cancelled");
            }
            short[] yp = pic.planes[0];
            int yBase = y * pic.stride[0];
            if (pic.numPlanes >= 3) {
                chromaRow(pic, 1, y, uRow, mix, cw, ch);
                chromaRow(pic, 2, y, vRow, mix, cw, ch);
            }
            int alphaBase = alpha == null ? 0 : y * alpha.stride[0];
            // 한 행의 픽셀을 차례로 변환한다.
            for (int x = 0; x < w; x++) {
                int yv = yp[yBase + x];
                double r;
                double g;
                double b;
                if (pic.numPlanes < 3) {
                    double v = (yv - yOff) / yRange;
                    r = v;
                    g = v;
                    b = v;
                } else if (identity) {
                    g = (yv - yOff) / yRange;
                    b = (uRow[x] / 16.0 - yOff) / yRange;
                    r = (vRow[x] / 16.0 - yOff) / yRange;
                } else {
                    double yy = (yv - yOff) / yRange;
                    double cb = (uRow[x] / 16.0 - cOff) / cRange;
                    double cr = (vRow[x] / 16.0 - cOff) / cRange;
                    if (ycgco) {
                        double tmp = yy - cb;
                        g = yy + cb;
                        r = tmp + cr;
                        b = tmp - cr;
                    } else {
                        r = yy + crCoef * cr;
                        b = yy + cbCoef * cb;
                        g = (yy - kr * r - kb * b) / kg;
                    }
                }
                int ri = clamp8(r);
                int gi = clamp8(g);
                int bi = clamp8(b);
                int a = 255;
                if (alpha != null) {
                    a = alphaValue(alpha, alpha.planes[0][alphaBase + x]);
                    if (premultiplied && a > 0 && a < 255) {
                        ri = Math.min(255, (ri * 255 + a / 2) / a);
                        gi = Math.min(255, (gi * 255 + a / 2) / a);
                        bi = Math.min(255, (bi * 255 + a / 2) / a);
                    }
                }
                argb[y * w + x] = (a << 24) | (ri << 16) | (gi << 8) | bi;
            }
        }
        return argb;
    }

    /**
     * Converts a sample to the 0..255 range with rounding and clipping.
     * <p>
     * 샘플을 반올림과 제한을 거쳐 0~255 범위로 바꾼다.
     *
     * @param v the normalized value (0..1) / 정규화된 값(0~1)
     * @return the byte value / 바이트 값
     */
    private static int clamp8(double v) {
        int i = (int) Math.floor(v * 255.0 + 0.5);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /**
     * Converts an alpha sample to 0..255.
     * <p>
     * 알파 샘플을 0~255 로 바꾼다.
     *
     * @param alpha the alpha picture / 알파 화면
     * @param v     the sample / 샘플
     * @return the alpha value / 알파 값
     */
    private static int alphaValue(Av1Picture alpha, int v) {
        int bd = alpha.bitDepth;
        double a;
        if (alpha.fullRange) {
            a = v / (double) ((1 << bd) - 1);
        } else {
            a = (v - (16 << (bd - 8))) / (double) (219 << (bd - 8));
        }
        return clamp8(a);
    }

    /**
     * Computes one row of a chroma plane at full resolution with bilinear interpolation. The result is scaled by 16.
     * <p>
     * 색차 평면의 한 행을 이중 선형 보간으로 전체 해상도로 구한다. 결과는 16 배 한 값이다.
     *
     * @param pic   the picture / 화면
     * @param plane the chroma plane (1 or 2) / 색차 평면(1 또는 2)
     * @param y     the luma row / 휘도 행
     * @param out   receives the interpolated row (scaled by 16) / 보간한 행을 받는다(16 배)
     * @param mix   scratch row of the plane width / 평면 너비의 임시 행
     * @param cw    the chroma plane width / 색차 평면 너비
     * @param ch    the chroma plane height / 색차 평면 높이
     */
    private static void chromaRow(Av1Picture pic, int plane, int y, int[] out, int[] mix, int cw, int ch) {
        short[] cp = pic.planes[plane];
        int stride = pic.stride[plane];
        int w = pic.width;
        int near;
        int far;
        // 세로 방향으로는 가까운 행에 3, 먼 행에 1 의 가중치를 준다.
        if (pic.subY != 0) {
            near = y >> 1;
            far = (y & 1) == 0 ? near - 1 : near + 1;
            far = Math.max(0, Math.min(ch - 1, far));
            near = Math.min(ch - 1, near);
            // 색차 두 행을 세로 가중치(3:1)로 섞는다.
            for (int i = 0; i < cw; i++) {
                mix[i] = 3 * cp[near * stride + i] + cp[far * stride + i];
            }
        } else {
            // 수직 하위 표본화가 없으면 같은 행을 4배로 한다.
            for (int i = 0; i < cw; i++) {
                mix[i] = 4 * cp[y * stride + i];
            }
        }
        // 가로 방향으로도 같은 가중치로 보간하여 16 배 값을 만든다.
        if (pic.subX != 0) {
            for (int x = 0; x < w; x++) {
                int cx = x >> 1;
                int fx = (x & 1) == 0 ? cx - 1 : cx + 1;
                fx = Math.max(0, Math.min(cw - 1, fx));
                cx = Math.min(cw - 1, cx);
                out[x] = 3 * mix[cx] + mix[fx];
            }
        } else {
            // 수평 하위 표본화가 없으면 같은 값을 4배로 한다.
            for (int x = 0; x < w; x++) {
                out[x] = 4 * mix[x];
            }
        }
    }
}
