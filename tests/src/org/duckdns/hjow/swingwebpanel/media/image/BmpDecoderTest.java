package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link BmpDecoder} (plan items P18-02, P18-03; FEATURE_MATRIX IMG-05): header variants, every bit depth, palettes, bit masks, RLE4 and
 * RLE8, row order, alpha handling, damaged and hostile data.
 * <p>
 * {@link BmpDecoder} 시험(계획 P18-02, P18-03; FEATURE_MATRIX IMG-05): 헤더 변형, 모든 비트 심도, 팔레트, 비트 마스크, RLE4/RLE8, 행 순서, 알파 처리,
 * 손상되거나 악의적인 데이터.
 */
public class BmpDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BmpDecoderTest() {
    }

    /**
     * Decodes with the default limits.
     * <p>
     * 기본 제한으로 디코딩한다.
     *
     * @param d data / 데이터
     * @return image / 이미지
     * @throws MediaException on failure / 실패 시
     */
    private static DecodedImage decode(byte[] d) throws MediaException {
        return new BmpDecoder().decode(d, DecodeLimits.DEFAULT);
    }

    /**
     * Returns pseudo random values below a limit.
     * <p>
     * 한도 미만의 의사 난수 값을 반환한다.
     *
     * @param n     count / 개수
     * @param limit exclusive limit (0 for the full int range) / 미포함 한도(0 이면 int 전체 범위)
     * @param seed  seed / 시드
     * @return values / 값
     */
    private static int[] values(int n, int limit, long seed) {
        Random r = new Random(seed);
        int[] v = new int[n];
        // 위치마다 임의 값을 만든다.
        for (int i = 0; i < n; i++) {
            v[i] = limit == 0 ? r.nextInt() : r.nextInt(limit);
        }
        return v;
    }

    /**
     * Opaque palette colours (0xRRGGBB) of the given size.
     * <p>
     * 주어진 크기의 팔레트 색(0xRRGGBB).
     *
     * @param n size / 크기
     * @return palette / 팔레트
     */
    private static int[] palette(int n) {
        int[] p = new int[n];
        // 항목마다 서로 다른 색을 만든다.
        for (int i = 0; i < n; i++) {
            p[i] = ((i * 37 + 11) & 0xFF) << 16 | ((i * 91 + 3) & 0xFF) << 8 | ((i * 17 + 200) & 0xFF);
        }
        return p;
    }

    /**
     * 24-bit bitmaps decode exactly in bottom-up and top-down order, with a file header or core header.
     * <p>
     * 24비트 비트맵은 아래에서 위/위에서 아래 순서, 파일 헤더/코어 헤더 모두 정확히 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTrueColour24() throws Exception {
        int w = 7;
        int h = 5;
        int[] v = values(w * h, 0x1000000, 1);
        int[] exp = new int[v.length];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < v.length; i++) {
            exp[i] = 0xFF000000 | v[i];
        }
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 24, 0, 0, null, null, BmpBuilder.pack(w, h, 24, v, false))), w, h, exp,
                "bottom-up");
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, -h, 24, 0, 0, null, null, BmpBuilder.pack(w, h, 24, v, true))), w, h, exp,
                "top-down");
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 12, w, h, 24, 0, 0, null, null, BmpBuilder.pack(w, h, 24, v, false))), w, h, exp,
                "core header");
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 108, w, h, 24, 0, 0, null, null, BmpBuilder.pack(w, h, 24, v, false))), w, h, exp,
                "V4 header");
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 124, w, h, 24, 0, 0, null, null, BmpBuilder.pack(w, h, 24, v, false))), w, h, exp,
                "V5 header");
    }

    /**
     * Palette bitmaps of 1, 4 and 8 bits (also with the core header and with fewer colours than the depth allows) decode exactly; an index
     * beyond the palette is black.
     * <p>
     * 1, 4, 8비트 팔레트 비트맵(코어 헤더, 비트 심도가 허용하는 것보다 적은 색 포함)이 정확히 디코딩되며 팔레트 밖의 인덱스는 검정이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPalettes() throws Exception {
        // 비트 심도마다 헤더 종류를 바꿔 가며 시험한다.
        for (int bpp : new int[] { 1, 4, 8 }) {
            for (int hs : new int[] { 40, 12 }) {
                int w = 13;
                int h = 6;
                int n = 1 << bpp;
                int[] pal = palette(n);
                int[] idx = values(w * h, n, bpp * 100 + hs);
                int[] exp = new int[idx.length];
                // 각 항목을 차례로 처리한다.
                for (int i = 0; i < idx.length; i++) {
                    exp[i] = 0xFF000000 | pal[idx[i]];
                }
                byte[] bmp = BmpBuilder.build(true, hs, w, h, bpp, 0, 0, null, pal, BmpBuilder.pack(w, h, bpp, idx, false));
                ImageAsserts.pixels(decode(bmp), w, h, exp, bpp + " bits, header " + hs);
            }
        }
        // 팔레트가 5색뿐인 8비트 비트맵: 5 이상의 인덱스는 검정이다.
        int[] pal5 = palette(5);
        int[] idx = { 0, 1, 2, 3, 4, 5, 200, 4 };
        byte[] small = BmpBuilder.build(true, 40, 8, 1, 8, 0, 5, null, pal5, BmpBuilder.pack(8, 1, 8, idx, false));
        int[] exp = new int[8];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 8; i++) {
            exp[i] = idx[i] < 5 ? (0xFF000000 | pal5[idx[i]]) : 0xFF000000;
        }
        ImageAsserts.pixels(decode(small), 8, 1, exp, "5 colours used");
    }

    /**
     * 32-bit bitmaps: the fourth byte is alpha, but a bitmap whose alpha values are all 0 is opaque; bit masks of any layout are honoured and
     * a missing alpha mask means opaque.
     * <p>
     * 32비트 비트맵: 네 번째 바이트는 알파이지만 알파가 모두 0 이면 불투명이다. 임의 배치의 비트 마스크를 지키며 알파 마스크가 없으면 불투명이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThirtyTwoBit() throws Exception {
        int w = 5;
        int h = 4;
        int[] v = values(w * h, 0, 5);
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < v.length; i++) {
            v[i] = (v[i] & 0x00FFFFFF) | (((i % 5) + 1) * 40 << 24);
        }
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 32, 0, 0, null, null, BmpBuilder.pack(w, h, 32, v, false))), w, h, v,
                "BI_RGB with alpha");
        int[] zero = new int[v.length];
        int[] expOpaque = new int[v.length];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < v.length; i++) {
            zero[i] = v[i] & 0x00FFFFFF;
            expOpaque[i] = zero[i] | 0xFF000000;
        }
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 32, 0, 0, null, null, BmpBuilder.pack(w, h, 32, zero, false))), w, h,
                expOpaque, "all alpha 0 is opaque");
        // RGB 순서가 바뀐 마스크(R 이 맨 아래 바이트)와 알파 마스크 없음.
        int[] masks = { 0x000000FF, 0x0000FF00, 0x00FF0000 };
        int[] exp = new int[v.length];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < v.length; i++) {
            exp[i] = 0xFF000000 | ((v[i] & 0xFF) << 16) | (v[i] & 0xFF00) | ((v[i] >> 16) & 0xFF);
        }
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 32, 3, 0, masks, null, BmpBuilder.pack(w, h, 32, v, false))), w, h, exp,
                "BI_BITFIELDS without alpha mask");
        // 알파 마스크가 있는 V4 헤더.
        int[] masksA = { 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000 };
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 108, w, h, 32, 3, 0, masksA, null, BmpBuilder.pack(w, h, 32, v, false))), w, h, v,
                "V4 with an alpha mask");
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 32, 6, 0, masksA, null, BmpBuilder.pack(w, h, 32, v, false))), w, h, v,
                "BI_ALPHABITFIELDS");
    }

    /**
     * 16-bit bitmaps: the default is 5-5-5; bit masks give 5-6-5 and other layouts.
     * <p>
     * 16비트 비트맵: 기본은 5-5-5 이며 비트 마스크로 5-6-5 와 다른 배치를 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSixteenBit() throws Exception {
        int w = 6;
        int h = 3;
        int[] v = values(w * h, 0x10000, 9);
        int[] e555 = new int[v.length];
        int[] e565 = new int[v.length];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < v.length; i++) {
            e555[i] = 0xFF000000 | ((((v[i] >> 10) & 31) * 255 / 31) << 16) | ((((v[i] >> 5) & 31) * 255 / 31) << 8) | ((v[i] & 31) * 255 / 31);
            e565[i] = 0xFF000000 | ((((v[i] >> 11) & 31) * 255 / 31) << 16) | ((((v[i] >> 5) & 63) * 255 / 63) << 8) | ((v[i] & 31) * 255 / 31);
        }
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 16, 0, 0, null, null, BmpBuilder.pack(w, h, 16, v, false))), w, h, e555,
                "5-5-5");
        int[] masks = { 0xF800, 0x07E0, 0x001F };
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, w, h, 16, 3, 0, masks, null, BmpBuilder.pack(w, h, 16, v, false))), w, h, e565,
                "5-6-5");
    }

    /**
     * RLE8 and RLE4 streams with runs, absolute runs (with their padding), delta jumps, end-of-line and end-of-bitmap codes; pixels that are
     * skipped stay transparent.
     * <p>
     * 반복, 절대 모드(패딩 포함), 이동, 줄 끝, 비트맵 끝 코드를 쓴 RLE8/RLE4 스트림. 건너뛴 픽셀은 투명으로 남는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRle() throws Exception {
        int[] pal = palette(256);
        // RLE8: 너비 6, 높이 3. 맨 아래 행부터 쓴다.
        // 첫 행: 반복 3 + 절대 3(홀수라 패딩 1바이트가 필요하다) 후 EOL.
        byte[] rle8 = { 3, 1, 0, 3, 10, 11, 12, 0, 0, 0, 0, 2, 2, 0, 2, 2, 0, 0, 6, 3, 0, 1 };
        int[] e8 = new int[18];
        // 각 항목을 차례로 처리한다.
        for (int x = 0; x < 6; x++) {
            e8[12 + x] = 0xFF000000 | pal[x < 3 ? 1 : 10 + x - 3];
            e8[x] = 0xFF000000 | pal[3];
        }
        e8[6 + 2] = 0xFF000000 | pal[2];
        e8[6 + 3] = 0xFF000000 | pal[2];
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, 6, 3, 8, 1, 0, null, pal, rle8)), 6, 3, e8, "RLE8");
        // RLE4: 너비 5, 높이 2.
        int[] pal16 = palette(16);
        byte[] rle4 = { 5, 0x12, 0, 0, 0, 5, 0x34, 0x56, 0x70, 0, 0, 1 };
        int[] e4 = new int[10];
        int[] row0 = { 1, 2, 1, 2, 1 };
        int[] row1 = { 3, 4, 5, 6, 7 };
        // 각 항목을 차례로 처리한다.
        for (int x = 0; x < 5; x++) {
            e4[5 + x] = 0xFF000000 | pal16[row0[x]];
            e4[x] = 0xFF000000 | pal16[row1[x]];
        }
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, 5, 2, 4, 2, 0, null, pal16, rle4)), 5, 2, e4, "RLE4");
        // 데이터가 일찍 끝나도 지금까지의 픽셀은 유지되고 나머지는 투명하다.
        byte[] early = { 2, 1 };
        int[] eEarly = new int[6];
        eEarly[3] = 0xFF000000 | pal[1];
        eEarly[4] = 0xFF000000 | pal[1];
        ImageAsserts.pixels(decode(BmpBuilder.build(true, 40, 3, 2, 8, 1, 0, null, pal, early)), 3, 2, eEarly, "RLE data that ends early");
    }

    /**
     * A BMP written by the JRE's ImageIO decodes to the pixels that the JRE's reader gives.
     * <p>
     * JRE 의 ImageIO 가 쓴 BMP 가 JRE 리더가 주는 픽셀로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBmpWrittenByImageIo() throws Exception {
        BufferedImage img = new BufferedImage(11, 7, BufferedImage.TYPE_INT_RGB);
        // 위치마다 다른 색을 칠한다.
        for (int y = 0; y < 7; y++) {
            for (int x = 0; x < 11; x++) {
                img.setRGB(x, y, ((x * 20) << 16) | ((y * 30) << 8) | (x * y));
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Check.isTrue(ImageIO.write(img, "bmp", bos), "ImageIO writes BMP");
        byte[] bmp = bos.toByteArray();
        BufferedImage ref = ImageIO.read(new ByteArrayInputStream(bmp));
        int[] exp = new int[11 * 7];
        // 각 항목을 차례로 처리한다.
        for (int y = 0; y < 7; y++) {
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < 11; x++) {
                exp[y * 11 + x] = ref.getRGB(x, y);
            }
        }
        ImageAsserts.pixels(decode(bmp), 11, 7, exp, "ImageIO BMP");
    }

    /**
     * Damaged and unsupported bitmaps: truncated data, unsupported compressions, invalid fields and huge sizes.
     * <p>
     * 손상되거나 지원하지 않는 비트맵: 잘린 데이터, 지원하지 않는 압축, 잘못된 필드, 아주 큰 크기.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalid() throws Exception {
        final byte[] ok = BmpBuilder.build(true, 40, 8, 8, 24, 0, 0, null, null, BmpBuilder.pack(8, 8, 24, values(64, 0x1000000, 2), false));
        expect(MediaException.Kind.CORRUPT, ImageAsserts.prefix(ok, ok.length - 5), "truncated pixel data");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.prefix(ok, 30), "truncated header");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.prefix(ok, 3), "three bytes");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.patched(ok, 18, 0, 0, 0, 0), "width 0");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.patched(ok, 22, 0, 0, 0, 0), "height 0");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.patched(ok, 28, 3, 0), "3 bits per pixel");
        expect(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(ok, 30, 4, 0, 0, 0), "BI_JPEG");
        expect(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(ok, 30, 5, 0, 0, 0), "BI_PNG");
        expect(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(ok, 30, 9, 0, 0, 0), "unknown compression");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.patched(ok, 30, 1, 0, 0, 0), "RLE8 with 24 bits");
        expect(MediaException.Kind.CORRUPT, ImageAsserts.patched(ok, 10, 0xFF, 0xFF, 0xFF, 0x7F), "pixel data offset outside the file");
        long start = System.currentTimeMillis();
        expect(MediaException.Kind.TOO_LARGE, ImageAsserts.patched(ok, 18, 0xA0, 0x86, 0x01, 0, 0xA0, 0x86, 0x01, 0), "100000 x 100000");
        expect(MediaException.Kind.TOO_LARGE, ImageAsserts.patched(ok, 18, 0xFF, 0xFF, 0xFF, 0x7F, 0xFF, 0xFF, 0xFF, 0x7F), "2147483647 square");
        expect(MediaException.Kind.TOO_LARGE, ImageAsserts.patched(ok, 18, 0x01, 0, 0, 0, 0, 0, 0, 0x80), "height -2147483648");
        Check.isTrue(System.currentTimeMillis() - start < 5000, "huge sizes are rejected at once");
    }

    /**
     * Checks that data fails with a kind.
     * <p>
     * 데이터가 주어진 종류로 실패하는지 검사한다.
     *
     * @param kind expected kind / 기대하는 종류
     * @param data data / 데이터
     * @param what description / 설명
     */
    private static void expect(MediaException.Kind kind, final byte[] data, String what) {
        ImageAsserts.fails(kind, new Block() {
            /**
             * Decodes the data.
             * <p>
             * 데이터를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(data);
            }
        }, what);
    }

    /**
     * Mutated and truncated bitmaps never fail with anything but a {@link MediaException}.
     * <p>
     * 변형되거나 잘린 비트맵은 {@link MediaException} 말고 다른 것으로 실패하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMutationsNeverEscape() throws Exception {
        DecodeLimits small = new DecodeLimits(1000000, 1 << 22, 100, 4000000);
        Random rnd = new Random(31);
        byte[][] seeds = { BmpBuilder.build(true, 40, 9, 7, 24, 0, 0, null, null, BmpBuilder.pack(9, 7, 24, values(63, 0x1000000, 4), false)),
                BmpBuilder.build(true, 40, 6, 3, 8, 1, 0, null, palette(256), new byte[] { 3, 1, 0, 3, 10, 11, 12, 0, 0, 2, 2, 0, 0, 6, 3, 0, 1 }),
                BmpBuilder.build(true, 108, 5, 4, 32, 3, 0, new int[] { 0xFF0000, 0xFF00, 0xFF, 0xFF000000 }, null,
                        BmpBuilder.pack(5, 4, 32, values(20, 0, 8), false)) };
        // 시드 파일마다 여러 번 변형해서 디코딩한다.
        for (byte[] seed : seeds) {
            for (int round = 0; round < 400; round++) {
                byte[] m = seed.clone();
                int flips = 1 + rnd.nextInt(4);
                // 각 항목을 차례로 처리한다.
                for (int k = 0; k < flips; k++) {
                    m[rnd.nextInt(m.length)] = (byte) rnd.nextInt(256);
                }
                if (round % 3 == 0) {
                    m = Arrays.copyOf(m, rnd.nextInt(m.length));
                }
                try {
                    DecodedImage img = new BmpDecoder().decode(m, small);
                    Check.isTrue(img.getWidth() > 0, "a decoded image has a size");
                } catch (MediaException expected) {
                    Check.notNull(expected.getKind(), "every failure has a kind");
                }
            }
        }
    }
}
