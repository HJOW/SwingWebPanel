package org.duckdns.hjow.swingwebpanel.media.image.svg;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The noise of <code>feTurbulence</code> (plan item P18-04), implemented from the reference code of the Filter Effects specification
 * (the Perlin noise with a seeded pseudo random generator, the optional stitching of tiles, <code>turbulence</code> and
 * <code>fractalNoise</code>). The four channels are returned as premultiplied ARGB pixels.
 * <p>
 * <code>feTurbulence</code> 의 잡음(계획 P18-04). 필터 효과 규격의 참조 코드로 구현했다(씨앗을 가진 의사 난수 발생기의 퍼린 잡음, 선택적인 타일
 * 이음, <code>turbulence</code> 와 <code>fractalNoise</code>). 네 채널을 알파를 미리 곱한 ARGB 픽셀로 반환한다.
 */
final class SvgTurbulence {

    /**
     * Modulus of the random generator.
     * <p>
     * 난수 발생기의 법.
     */
    private static final long RAND_M = 2147483647L;

    /**
     * Multiplier of the random generator.
     * <p>
     * 난수 발생기의 승수.
     */
    private static final long RAND_A = 16807L;

    /**
     * Quotient m / a of the random generator.
     * <p>
     * 난수 발생기의 몫 m / a.
     */
    private static final long RAND_Q = 127773L;

    /**
     * Remainder m % a of the random generator.
     * <p>
     * 난수 발생기의 나머지 m % a.
     */
    private static final long RAND_R = 2836L;

    /**
     * The size of the lattice tables.
     * <p>
     * 격자 표의 크기.
     */
    private static final int BSIZE = 0x100;

    /**
     * Mask for the lattice indices.
     * <p>
     * 격자 인덱스의 마스크.
     */
    private static final int BM = 0xff;

    /**
     * The offset that keeps coordinates positive.
     * <p>
     * 좌표를 양수로 유지하는 오프셋.
     */
    private static final int PERLIN_N = 0x1000;

    /**
     * The lattice permutation table.
     * <p>
     * 격자 순열 표.
     */
    private final int[] lattice = new int[BSIZE + BSIZE + 2];

    /**
     * The gradient vectors per colour channel.
     * <p>
     * 색 채널별 기울기 벡터.
     */
    private final double[][][] gradient = new double[4][BSIZE + BSIZE + 2][2];

    /**
     * Creates the noise tables for a seed.
     * <p>
     * 씨앗으로 잡음 표를 만든다.
     *
     * @param seedIn the seed / 씨앗
     */
    private SvgTurbulence(long seedIn) {
        long seed = seedIn;
        if (seed <= 0) {
            seed = -(seed % (RAND_M - 1)) + 1;
        }
        if (seed > RAND_M - 1) {
            seed = RAND_M - 1;
        }
        int i;
        // 채널마다 기울기 벡터를 만들어 정규화한다.
        for (int k = 0; k < 4; k++) {
            for (i = 0; i < BSIZE; i++) {
                lattice[i] = i;
                // 기울기 벡터의 두 성분을 채운다.
                for (int j = 0; j < 2; j++) {
                    seed = random(seed);
                    gradient[k][i][j] = (double) ((seed % (BSIZE + BSIZE)) - BSIZE) / BSIZE;
                }
                double s = Math.sqrt(gradient[k][i][0] * gradient[k][i][0] + gradient[k][i][1] * gradient[k][i][1]);
                gradient[k][i][0] /= s;
                gradient[k][i][1] /= s;
            }
        }
        // 격자 순열을 섞는다.
        for (i = BSIZE - 1; i > 0; i--) {
            int k = lattice[i];
            seed = random(seed);
            int j = (int) (seed % BSIZE);
            lattice[i] = lattice[j];
            lattice[j] = k;
        }
        // 표를 한 번 더 이어 붙여 인덱스가 넘쳐도 되게 한다.
        for (i = 0; i < BSIZE + 2; i++) {
            lattice[BSIZE + i] = lattice[i];
            // 네 채널을 차례로 처리한다.
            for (int k = 0; k < 4; k++) {
                // 기울기 벡터의 두 성분을 채운다.
                for (int j = 0; j < 2; j++) {
                    gradient[k][BSIZE + i][j] = gradient[k][i][j];
                }
            }
        }
    }

    /**
     * The pseudo random number generator of the specification.
     * <p>
     * 규격의 의사 난수 발생기.
     *
     * @param seed the current state / 현재 상태
     * @return the next state / 다음 상태
     */
    private static long random(long seed) {
        long result = RAND_A * (seed % RAND_Q) - RAND_R * (seed / RAND_Q);
        if (result <= 0) {
            result += RAND_M;
        }
        return result;
    }

    /**
     * The stitching information of one octave.
     * <p>
     * 옥타브 하나의 이음 정보.
     */
    private static final class Stitch {

        /**
         * The lattice width.
         * <p>
         * 격자 너비.
         */
        int width;

        /**
         * The lattice height.
         * <p>
         * 격자 높이.
         */
        int height;

        /**
         * The x wrap position.
         * <p>
         * x 감김 위치.
         */
        int wrapX;

        /**
         * The y wrap position.
         * <p>
         * y 감김 위치.
         */
        int wrapY;
    }

    /**
     * One octave of noise at a point for a colour channel.
     * <p>
     * 색 채널 하나의 한 점에서의 옥타브 하나 잡음.
     *
     * @param ch   the channel 0..3 / 채널 0..3
     * @param vx   x / x
     * @param vy   y / y
     * @param st   stitching or null / 이음 정보. 없으면 null
     * @return the noise value / 잡음 값
     */
    private double noise2(int ch, double vx, double vy, Stitch st) {
        double t = vx + PERLIN_N;
        int bx0 = ((int) t) & BM;
        int bx1 = (bx0 + 1) & BM;
        double rx0 = t - (int) t;
        double rx1 = rx0 - 1.0;
        t = vy + PERLIN_N;
        int by0 = ((int) t) & BM;
        int by1 = (by0 + 1) & BM;
        double ry0 = t - (int) t;
        double ry1 = ry0 - 1.0;
        if (st != null) {
            if (bx0 >= st.wrapX) {
                bx0 -= st.width;
            }
            if (bx1 >= st.wrapX) {
                bx1 -= st.width;
            }
            if (by0 >= st.wrapY) {
                by0 -= st.height;
            }
            if (by1 >= st.wrapY) {
                by1 -= st.height;
            }
        }
        bx0 &= BM;
        bx1 &= BM;
        by0 &= BM;
        by1 &= BM;
        int i = lattice[bx0];
        int j = lattice[bx1];
        int b00 = lattice[i + by0];
        int b10 = lattice[j + by0];
        int b01 = lattice[i + by1];
        int b11 = lattice[j + by1];
        double sx = rx0 * rx0 * (3.0 - 2.0 * rx0);
        double sy = ry0 * ry0 * (3.0 - 2.0 * ry0);
        double[] q = gradient[ch][b00];
        double u = rx0 * q[0] + ry0 * q[1];
        q = gradient[ch][b10];
        double v = rx1 * q[0] + ry0 * q[1];
        double a = u + sx * (v - u);
        q = gradient[ch][b01];
        u = rx0 * q[0] + ry1 * q[1];
        q = gradient[ch][b11];
        v = rx1 * q[0] + ry1 * q[1];
        double b = u + sx * (v - u);
        return a + sy * (b - a);
    }

    /**
     * The sum of the octaves at a point for a channel.
     * <p>
     * 채널 하나의 한 점에서의 옥타브 합.
     *
     * @param ch      the channel / 채널
     * @param px      point x / 점 x
     * @param py      point y / 점 y
     * @param fx      base frequency x / 기본 주파수 x
     * @param fy      base frequency y / 기본 주파수 y
     * @param octaves the number of octaves / 옥타브 수
     * @param fractal true for fractal noise / 프랙털 잡음이면 true
     * @param stitch  true to stitch tiles / 타일을 이으면 true
     * @param tile    the tile rectangle / 타일 사각형
     * @return the sum / 합
     */
    private double turbulence(int ch, double px, double py, double fx, double fy, int octaves, boolean fractal, boolean stitch, Rectangle2D tile) {
        Stitch st = null;
        double bx = fx;
        double by = fy;
        // 이음이 필요하면 타일 경계가 이어지도록 기본 주파수를 조정한다.
        if (stitch) {
            if (bx != 0) {
                double lo = Math.floor(tile.getWidth() * bx) / tile.getWidth();
                double hi = Math.ceil(tile.getWidth() * bx) / tile.getWidth();
                bx = (bx / lo < hi / bx) ? lo : hi;
            }
            if (by != 0) {
                double lo = Math.floor(tile.getHeight() * by) / tile.getHeight();
                double hi = Math.ceil(tile.getHeight() * by) / tile.getHeight();
                by = (by / lo < hi / by) ? lo : hi;
            }
            st = new Stitch();
            st.width = (int) (tile.getWidth() * bx + 0.5);
            st.wrapX = (int) (tile.getX() * bx + PERLIN_N + st.width);
            st.height = (int) (tile.getHeight() * by + 0.5);
            st.wrapY = (int) (tile.getY() * by + PERLIN_N + st.height);
        }
        double sum = 0;
        double vx = px * bx;
        double vy = py * by;
        double ratio = 1;
        // 옥타브마다 주파수를 두 배로 하며 잡음을 더한다.
        for (int o = 0; o < octaves; o++) {
            double n = noise2(ch, vx, vy, st);
            sum += (fractal ? n : Math.abs(n)) / ratio;
            vx *= 2;
            vy *= 2;
            ratio *= 2;
            if (st != null) {
                st.width *= 2;
                st.wrapX = 2 * st.wrapX - PERLIN_N;
                st.height *= 2;
                st.wrapY = 2 * st.wrapY - PERLIN_N;
            }
        }
        return sum;
    }

    /**
     * Generates the pixels of a <code>feTurbulence</code> primitive.
     * <p>
     * <code>feTurbulence</code> 기본 요소의 픽셀을 만든다.
     *
     * @param w          the width of the buffer / 버퍼 너비
     * @param h          the height of the buffer / 버퍼 높이
     * @param p          the primitive element / 기본 요소
     * @param region     the device rectangle that the buffer covers / 버퍼가 덮는 장치 사각형
     * @param ctm        the transform from user space to device pixels / 사용자 공간에서 장치 픽셀로의 변환
     * @param userRegion the filter region in user space (the stitching tile) / 사용자 공간의 필터 영역(이음 타일)
     * @return the pixels (premultiplied ARGB) / 픽셀(알파 미리 곱한 ARGB)
     */
    static int[] generate(int w, int h, SvgElement p, Rectangle region, AffineTransform ctm, Rectangle2D userRegion) {
        double[] bf = SvgScanner.numberList(p.attrs.get("baseFrequency"));
        double fx = bf != null && bf.length >= 1 ? bf[0] : 0;
        double fy = bf != null && bf.length >= 2 ? bf[1] : fx;
        int[] out = new int[w * h];
        if (fx < 0 || fy < 0) {
            return out;
        }
        String no = p.attrs.get("numOctaves");
        int octaves = 1;
        if (no != null) {
            try {
                octaves = Integer.parseInt(no.trim());
            } catch (NumberFormatException e) {
                octaves = 1;
            }
        }
        if (octaves <= 0) {
            return out;
        }
        octaves = Math.min(octaves, 16);
        long seed = 0;
        String sd = p.attrs.get("seed");
        if (sd != null) {
            double[] v = SvgScanner.numberList(sd);
            if (v != null && v.length == 1) {
                seed = Math.round(v[0]);
            }
        }
        boolean fractal = "fractalNoise".equals(p.attrs.get("type"));
        boolean stitch = "stitch".equals(p.attrs.get("stitchTiles"));
        SvgTurbulence t = new SvgTurbulence(seed);
        AffineTransform inv;
        try {
            inv = ctm.createInverse();
        } catch (NoninvertibleTransformException e) {
            return out;
        }
        Point2D.Double pt = new Point2D.Double();
        // 버퍼의 모든 픽셀에 대해 사용자 공간의 점을 구해 네 채널의 잡음을 계산한다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pt.setLocation(region.x + x, region.y + y);
                inv.transform(pt, pt);
                double[] c = new double[4];
                // 채널마다 합을 구하고 규격의 식으로 0..1 로 옮긴다.
                for (int ch = 0; ch < 4; ch++) {
                    double sum = t.turbulence(ch, pt.x, pt.y, fx, fy, octaves, fractal, stitch, userRegion);
                    c[ch] = fractal ? (sum + 1) / 2 : sum;
                    c[ch] = Math.max(0, Math.min(1, c[ch]));
                }
                int a = (int) Math.round(c[3] * 255);
                int r = (int) Math.round(c[0] * c[3] * 255);
                int g = (int) Math.round(c[1] * c[3] * 255);
                int b = (int) Math.round(c[2] * c[3] * 255);
                out[y * w + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }
        return out;
    }
}
