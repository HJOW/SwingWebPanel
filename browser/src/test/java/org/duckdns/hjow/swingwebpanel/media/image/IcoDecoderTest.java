package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link IcoDecoder} (plan items P18-02, P18-03; FEATURE_MATRIX IMG-06): icons and cursors with bitmap and PNG images, AND masks,
 * 32-bit alpha, several sizes, damaged entries and hostile data.
 * <p>
 * {@link IcoDecoder} 시험(계획 P18-02, P18-03; FEATURE_MATRIX IMG-06): 비트맵/PNG 이미지를 가진 아이콘과 커서, AND 마스크, 32비트 알파, 여러 크기,
 * 손상된 항목, 악의적인 데이터.
 */
public class IcoDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public IcoDecoderTest() {
    }

    /**
     * Decodes with the default limits.
     * <p>
     * 기본 제한으로 디코딩한다.
     *
     * @param d data / 데이터
     * @return the largest image / 가장 큰 이미지
     * @throws MediaException on failure / 실패 시
     */
    private static DecodedImage decode(byte[] d) throws MediaException {
        return new IcoDecoder().decode(d, DecodeLimits.DEFAULT);
    }

    /**
     * Builds an ico file with one image.
     * <p>
     * 이미지 하나로 ico 파일을 만든다.
     *
     * @param type  1 or 2 / 1 또는 2
     * @param dib   image data / 이미지 데이터
     * @param w     directory width / 디렉터리 가로
     * @param h     directory height / 디렉터리 세로
     * @return file bytes / 파일 바이트
     */
    private static byte[] single(int type, byte[] dib, int w, int h) {
        List<byte[]> l = new ArrayList<byte[]>();
        l.add(dib);
        return BmpBuilder.ico(type, l, new int[] { w }, new int[] { h });
    }

    /**
     * A 32-bit icon image uses its alpha values and ignores the AND mask.
     * <p>
     * 32비트 아이콘 이미지는 알파 값을 쓰고 AND 마스크를 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThirtyTwoBitAlphaIgnoresMask() throws Exception {
        int[] v = new int[16];
        int[] mask = new int[16];
        // 픽셀마다 알파가 다른 색을 만들고 마스크는 모두 투명(1)으로 둔다.
        for (int i = 0; i < 16; i++) {
            v[i] = ((i * 16 + 15) << 24) | (i * 0x101010 & 0xFFFFFF);
            mask[i] = 1;
        }
        DecodedImage img = decode(single(1, BmpBuilder.iconDib(4, 4, 32, null, v, mask), 4, 4));
        ImageAsserts.pixels(img, 4, 4, v, "32-bit alpha");
    }

    /**
     * A 32-bit image with no alpha at all and the 1-bit AND mask for transparency; the same for 8-bit and 24-bit images.
     * <p>
     * 알파가 전혀 없는 32비트 이미지와 투명도를 정하는 1비트 AND 마스크. 8비트와 24비트 이미지도 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAndMask() throws Exception {
        Random r = new Random(3);
        int w = 6;
        int h = 5;
        int[] mask = new int[w * h];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < mask.length; i++) {
            mask[i] = r.nextInt(2);
        }
        int[] rgb = new int[w * h];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = r.nextInt(0x1000000);
        }
        int[] exp = new int[w * h];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < exp.length; i++) {
            exp[i] = mask[i] == 1 ? 0 : (0xFF000000 | rgb[i]);
        }
        ImageAsserts.pixels(decode(single(1, BmpBuilder.iconDib(w, h, 24, null, rgb, mask), w, h)), w, h, exp, "24-bit with mask");
        ImageAsserts.pixels(decode(single(1, BmpBuilder.iconDib(w, h, 32, null, rgb, mask), w, h)), w, h, exp, "32-bit, alpha 0, with mask");
        int[] pal = new int[256];
        int[] idx = new int[w * h];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 256; i++) {
            pal[i] = (i * 0x030507) & 0xFFFFFF;
        }
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < idx.length; i++) {
            idx[i] = r.nextInt(256);
            exp[i] = mask[i] == 1 ? 0 : (0xFF000000 | pal[idx[i]]);
        }
        ImageAsserts.pixels(decode(single(1, BmpBuilder.iconDib(w, h, 8, pal, idx, mask), w, h)), w, h, exp, "8-bit with mask");
        int[] pal2 = { 0x000000, 0xFFFFFF };
        int[] bits = new int[w * h];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < bits.length; i++) {
            bits[i] = r.nextInt(2);
            exp[i] = mask[i] == 1 ? 0 : (0xFF000000 | pal2[bits[i]]);
        }
        ImageAsserts.pixels(decode(single(1, BmpBuilder.iconDib(w, h, 1, pal2, bits, mask), w, h)), w, h, exp, "1-bit with mask");
    }

    /**
     * Several sizes in one file: the largest image is chosen, {@link IcoDecoder#decodeAll} returns all in directory order, PNG and
     * bitmap entries can be mixed, and a cursor file is read like an icon file.
     * <p>
     * 한 파일의 여러 크기: 가장 큰 이미지를 고르고 {@link IcoDecoder#decodeAll} 은 디렉터리 순서로 모두 반환하며 PNG 와 비트맵 항목을 섞을 수 있고
     * 커서 파일도 아이콘 파일처럼 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSeveralSizes() throws Exception {
        int[] v4 = new int[16];
        int[] v8 = new int[64];
        int[] v16 = new int[256];
        Arrays.fill(v4, 0xFF111111);
        Arrays.fill(v8, 0xFF222222);
        Arrays.fill(v16, 0xFF333333);
        PngBuilder.Spec ps = new PngBuilder.Spec(12, 12);
        ps.samples = new int[12 * 12 * 4];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 144; i++) {
            ps.samples[i * 4] = 10;
            ps.samples[i * 4 + 1] = 20;
            ps.samples[i * 4 + 2] = 30;
            ps.samples[i * 4 + 3] = 255;
        }
        List<byte[]> images = new ArrayList<byte[]>();
        images.add(BmpBuilder.iconDib(4, 4, 32, null, v4, new int[16]));
        images.add(PngBuilder.encode(ps));
        images.add(BmpBuilder.iconDib(16, 16, 32, null, v16, new int[256]));
        images.add(BmpBuilder.iconDib(8, 8, 32, null, v8, new int[64]));
        byte[] ico = BmpBuilder.ico(1, images, new int[] { 4, 12, 16, 8 }, new int[] { 4, 12, 16, 8 });
        DecodedImage largest = decode(ico);
        ImageAsserts.pixels(largest, 16, 16, v16, "the 16 x 16 image is the largest");
        List<DecodedImage> all = IcoDecoder.decodeAll(ico, DecodeLimits.DEFAULT);
        Check.equal(4, all.size(), "all images");
        Check.equal(4, all.get(0).getWidth(), "first entry");
        Check.equal(12, all.get(1).getWidth(), "embedded PNG");
        Check.equal(0xFF0A141E, all.get(1).getFrames().get(0).getArgb()[0], "PNG pixel");
        Check.equal(16, all.get(2).getWidth(), "third entry");
        Check.equal(8, all.get(3).getWidth(), "fourth entry");
        byte[] cur = BmpBuilder.ico(2, images, new int[] { 4, 12, 16, 8 }, new int[] { 4, 12, 16, 8 });
        Check.isTrue(new IcoDecoder().accepts(cur), "cursor files are accepted");
        ImageAsserts.pixels(decode(cur), 16, 16, v16, "cursor file");
        List<byte[]> onlyPng = new ArrayList<byte[]>();
        onlyPng.add(PngBuilder.encode(ps));
        DecodedImage p = decode(BmpBuilder.ico(1, onlyPng, new int[] { 12 }, new int[] { 12 }));
        Check.equal(12, p.getWidth(), "PNG only");
    }

    /**
     * Entries that cannot be decoded are skipped; a file without any usable image is CORRUPT; the signature check is strict.
     * <p>
     * 디코딩할 수 없는 항목은 건너뛰며 쓸 수 있는 이미지가 하나도 없는 파일은 CORRUPT 이고 시그니처 확인은 엄격하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDamagedEntries() throws Exception {
        int[] v = new int[16];
        Arrays.fill(v, 0xFF445566);
        List<byte[]> images = new ArrayList<byte[]>();
        images.add(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
                33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48 });
        images.add(BmpBuilder.iconDib(4, 4, 32, null, v, new int[16]));
        byte[] ico = BmpBuilder.ico(1, images, new int[] { 16, 4 }, new int[] { 16, 4 });
        ImageAsserts.pixels(decode(ico), 4, 4, v, "the damaged entry is skipped");
        Check.equal(1, IcoDecoder.decodeAll(ico, DecodeLimits.DEFAULT).size(), "one usable entry");
        // 디렉터리의 오프셋을 파일 밖으로 돌리면 항목이 사라진다.
        final byte[] outside = ImageAsserts.patched(ico, 6 + 16 + 12, 0xFF, 0xFF, 0xFF, 0x7F);
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes a file whose only usable entry points outside the file.
             * <p>
             * 쓸 수 있는 유일한 항목이 파일 밖을 가리키는 파일을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(outside);
            }
        }, "no usable entry");
        Check.isFalse(new IcoDecoder().accepts(new byte[] { 0, 0, 3, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }), "type 3");
        Check.isFalse(new IcoDecoder().accepts(new byte[] { 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }), "zero images");
        Check.isFalse(new IcoDecoder().accepts(new byte[] { 0, 0, 1 }), "too short");
        final byte[] cut = ImageAsserts.prefix(ico, 10);
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes a file cut inside the directory.
             * <p>
             * 디렉터리 안에서 잘린 파일을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(cut);
            }
        }, "cut inside the directory");
    }

    /**
     * An image that claims a huge size is TOO_LARGE (not skipped); mutated files never fail with anything but a {@link MediaException}.
     * <p>
     * 아주 큰 크기를 주장하는 이미지는 건너뛰지 않고 TOO_LARGE 이다. 변형된 파일은 {@link MediaException} 말고 다른 것으로 실패하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHostileData() throws Exception {
        int[] v = new int[16];
        Arrays.fill(v, 0xFF778899);
        final byte[] ico = single(1, BmpBuilder.iconDib(4, 4, 32, null, v, new int[16]), 4, 4);
        final byte[] huge = ImageAsserts.patched(ico, 22 + 4, 0xA0, 0x86, 0x01, 0, 0x40, 0x0D, 0x03, 0);
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes an icon image that claims 100000 x 100000 pixels.
             * <p>
             * 100000 x 100000 픽셀을 주장하는 아이콘 이미지를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(huge);
            }
        }, "huge icon image");
        DecodeLimits small = new DecodeLimits(1000000, 1 << 22, 100, 4000000);
        Random rnd = new Random(77);
        // 변형한 파일을 여러 번 디코딩한다.
        for (int round = 0; round < 400; round++) {
            byte[] m = ico.clone();
            int flips = 1 + rnd.nextInt(4);
            // 각 항목을 차례로 처리한다.
            for (int k = 0; k < flips; k++) {
                m[rnd.nextInt(m.length)] = (byte) rnd.nextInt(256);
            }
            if (round % 3 == 0) {
                m = Arrays.copyOf(m, rnd.nextInt(m.length));
            }
            try {
                DecodedImage img = new IcoDecoder().decode(m, small);
                Check.isTrue(img.getWidth() > 0, "a decoded image has a size");
            } catch (MediaException expected) {
                Check.notNull(expected.getKind(), "every failure has a kind");
            }
        }
    }
}
