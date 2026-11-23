package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link PngDecoder} for still images (plan items P18-02, P18-03; FEATURE_MATRIX IMG-01): every colour type and bit depth with and
 * without interlacing, all row filters, transparency, damaged and hostile data. The inputs are built by {@link PngBuilder}, an independent
 * encoder, and by the JRE's ImageIO writer.
 * <p>
 * 정지 이미지에 대한 {@link PngDecoder} 시험(계획 P18-02, P18-03; FEATURE_MATRIX IMG-01): 모든 색 유형과 비트 심도(인터레이스 여부 포함), 모든 행 필터,
 * 투명도, 손상되거나 악의적인 데이터. 입력은 독립된 인코더 {@link PngBuilder} 와 JRE 의 ImageIO 작성기로 만든다.
 */
public class PngDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PngDecoderTest() {
    }

    /**
     * Computes the expected pixel straight from the PNG specification.
     * <p>
     * PNG 규격에서 곧바로 기대 픽셀을 계산한다.
     *
     * @param s the image description / 이미지 설명
     * @param i pixel index / 픽셀 위치
     * @return expected ARGB / 기대 ARGB
     */
    private static int expected(PngBuilder.Spec s, int i) {
        int ch = s.channels();
        int max = (1 << s.depth) - 1;
        int[] v = new int[ch];
        // 픽셀의 채널 샘플을 꺼낸다.
        for (int c = 0; c < ch; c++) {
            v[c] = s.samples[i * ch + c];
        }
        int shift = s.depth == 16 ? 8 : 0;
        switch (s.colorType) {
        case 0: {
            int g = s.depth == 16 ? v[0] >> 8 : v[0] * 255 / max;
            int key = s.trns == null ? -1 : (((s.trns[0] & 0xFF) << 8) | (s.trns[1] & 0xFF));
            return ((v[0] == key ? 0 : 255) << 24) | (g << 16) | (g << 8) | g;
        }
        case 2: {
            boolean hit = false;
            if (s.trns != null) {
                hit = v[0] == (((s.trns[0] & 0xFF) << 8) | (s.trns[1] & 0xFF)) && v[1] == (((s.trns[2] & 0xFF) << 8) | (s.trns[3] & 0xFF))
                        && v[2] == (((s.trns[4] & 0xFF) << 8) | (s.trns[5] & 0xFF));
            }
            return ((hit ? 0 : 255) << 24) | ((v[0] >> shift) << 16) | ((v[1] >> shift) << 8) | (v[2] >> shift);
        }
        case 3: {
            int idx = v[0];
            int a = (s.trns != null && idx < s.trns.length) ? (s.trns[idx] & 0xFF) : 255;
            return (a << 24) | ((s.plte[idx * 3] & 0xFF) << 16) | ((s.plte[idx * 3 + 1] & 0xFF) << 8) | (s.plte[idx * 3 + 2] & 0xFF);
        }
        case 4: {
            int g = v[0] >> shift;
            return ((v[1] >> shift) << 24) | (g << 16) | (g << 8) | g;
        }
        default:
            return ((v[3] >> shift) << 24) | ((v[0] >> shift) << 16) | ((v[1] >> shift) << 8) | (v[2] >> shift);
        }
    }

    /**
     * Builds a deterministic image description with sample values that exercise the whole range.
     * <p>
     * 값의 전체 범위를 쓰는 결정적 이미지 설명을 만든다.
     *
     * @param w         width / 가로
     * @param h         height / 세로
     * @param colorType colour type / 색 유형
     * @param depth     bit depth / 비트 심도
     * @param interlace interlacing / 인터레이스
     * @return the description / 설명
     */
    private static PngBuilder.Spec spec(int w, int h, int colorType, int depth, boolean interlace) {
        PngBuilder.Spec s = new PngBuilder.Spec(w, h);
        s.colorType = colorType;
        s.depth = depth;
        s.interlace = interlace;
        int ch = s.channels();
        int max = (1 << depth) - 1;
        s.samples = new int[w * h * ch];
        // 위치마다 다른 값을 만들어 필터와 인터레이스의 오류가 드러나게 한다.
        for (int i = 0; i < w * h; i++) {
            for (int c = 0; c < ch; c++) {
                s.samples[i * ch + c] = (i * 37 + c * 91 + (i / Math.max(1, w)) * 13 + 5) % (max + 1);
            }
        }
        if (colorType == 3) {
            int n = Math.min(1 << depth, 256);
            s.plte = new byte[n * 3];
            // 팔레트 색은 항목마다 다르게 한다.
            for (int i = 0; i < n; i++) {
                s.plte[i * 3] = (byte) (i * 53 + 7);
                s.plte[i * 3 + 1] = (byte) (i * 29 + 100);
                s.plte[i * 3 + 2] = (byte) (255 - i * 17);
            }
        }
        return s;
    }

    /**
     * Every combination of colour type and bit depth allowed by the specification round-trips exactly, with and without Adam7, for several
     * sizes (including images smaller than one interlace pass) and with all filters.
     * <p>
     * 규격이 허용하는 모든 색 유형과 비트 심도의 조합이 Adam7 여부, 여러 크기(인터레이스 한 단계보다 작은 이미지 포함), 모든 필터와 함께 정확히
     * 왕복한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAllColourTypesAndDepths() throws Exception {
        int[][] combos = { { 0, 1 }, { 0, 2 }, { 0, 4 }, { 0, 8 }, { 0, 16 }, { 2, 8 }, { 2, 16 }, { 3, 1 }, { 3, 2 }, { 3, 4 }, { 3, 8 },
                { 4, 8 }, { 4, 16 }, { 6, 8 }, { 6, 16 } };
        int[][] sizes = { { 1, 1 }, { 3, 2 }, { 8, 8 }, { 17, 13 }, { 5, 1 }, { 1, 9 } };
        // 모든 조합, 크기, 인터레이스 여부를 확인한다.
        for (int[] c : combos) {
            for (int[] sz : sizes) {
                // 각 항목을 차례로 처리한다.
                for (int il = 0; il < 2; il++) {
                    PngBuilder.Spec s = spec(sz[0], sz[1], c[0], c[1], il == 1);
                    byte[] png = PngBuilder.encode(s);
                    DecodedImage img = new PngDecoder().decode(png, DecodeLimits.DEFAULT);
                    int[] exp = new int[sz[0] * sz[1]];
                    // 각 항목을 차례로 처리한다.
                    for (int i = 0; i < exp.length; i++) {
                        exp[i] = expected(s, i);
                    }
                    String what = "colour type " + c[0] + " depth " + c[1] + " size " + sz[0] + "x" + sz[1] + (il == 1 ? " interlaced" : "");
                    ImageAsserts.pixels(img, sz[0], sz[1], exp, what);
                    Check.isFalse(img.isAnimated(), what + ": still image");
                }
            }
        }
    }

    /**
     * Each of the five row filters decodes correctly on its own (the cycle in the other tests mixes them).
     * <p>
     * 다섯 가지 행 필터가 각각 따로도 올바르게 디코딩된다(다른 시험은 번갈아 쓴다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEachRowFilter() throws Exception {
        // 필터 유형 0~4 를 하나씩 고정해 시험한다.
        for (int f = 0; f < 5; f++) {
            PngBuilder.Spec s = spec(19, 11, 6, 8, false);
            s.filter = f;
            DecodedImage img = new PngDecoder().decode(PngBuilder.encode(s), DecodeLimits.DEFAULT);
            int[] exp = new int[19 * 11];
            // 각 항목을 차례로 처리한다.
            for (int i = 0; i < exp.length; i++) {
                exp[i] = expected(s, i);
            }
            ImageAsserts.pixels(img, 19, 11, exp, "filter " + f);
            PngBuilder.Spec t = spec(19, 11, 2, 16, true);
            t.filter = f;
            DecodedImage img2 = new PngDecoder().decode(PngBuilder.encode(t), DecodeLimits.DEFAULT);
            int[] exp2 = new int[19 * 11];
            // 각 항목을 차례로 처리한다.
            for (int i = 0; i < exp2.length; i++) {
                exp2[i] = expected(t, i);
            }
            ImageAsserts.pixels(img2, 19, 11, exp2, "16-bit interlaced filter " + f);
        }
    }

    /**
     * tRNS gives transparency for grey (a key value), RGB (a key colour) and palette images (alpha per entry, shorter than the palette).
     * <p>
     * tRNS 는 회색(키 값), RGB(키 색), 팔레트(항목별 알파, 팔레트보다 짧을 수 있음) 이미지에 투명도를 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTransparency() throws Exception {
        PngBuilder.Spec g = spec(6, 4, 0, 8, false);
        g.trns = new byte[] { 0, (byte) g.samples[3] };
        check(g, "grey 8-bit key");
        PngBuilder.Spec g16 = spec(6, 4, 0, 16, true);
        g16.trns = new byte[] { (byte) (g16.samples[5] >> 8), (byte) g16.samples[5] };
        check(g16, "grey 16-bit key");
        PngBuilder.Spec g2 = spec(6, 4, 0, 2, false);
        g2.trns = new byte[] { 0, (byte) g2.samples[2] };
        check(g2, "grey 2-bit key");
        PngBuilder.Spec rgb = spec(6, 4, 2, 8, false);
        rgb.trns = new byte[] { 0, (byte) rgb.samples[6], 0, (byte) rgb.samples[7], 0, (byte) rgb.samples[8] };
        check(rgb, "RGB 8-bit key");
        PngBuilder.Spec rgb16 = spec(6, 4, 2, 16, false);
        rgb16.trns = new byte[] { (byte) (rgb16.samples[3] >> 8), (byte) rgb16.samples[3], (byte) (rgb16.samples[4] >> 8),
                (byte) rgb16.samples[4], (byte) (rgb16.samples[5] >> 8), (byte) rgb16.samples[5] };
        check(rgb16, "RGB 16-bit key");
        PngBuilder.Spec pal = spec(6, 4, 3, 4, true);
        pal.trns = new byte[] { 0, (byte) 128, 64 };
        check(pal, "palette alpha shorter than the palette");
        PngBuilder.Spec pal8 = spec(9, 7, 3, 8, false);
        pal8.trns = new byte[256];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 256; i++) {
            pal8.trns[i] = (byte) (i * 3);
        }
        check(pal8, "palette alpha for 256 entries");
    }

    /**
     * Encodes a description, decodes it and compares every pixel with the specification.
     * <p>
     * 설명을 인코딩하고 디코딩해 모든 픽셀을 규격과 비교한다.
     *
     * @param s    the description / 설명
     * @param what name of the case / 사례 이름
     * @throws Exception on failure / 실패 시
     */
    private static void check(PngBuilder.Spec s, String what) throws Exception {
        DecodedImage img = new PngDecoder().decode(PngBuilder.encode(s), DecodeLimits.DEFAULT);
        int[] exp = new int[s.width * s.height];
        // 픽셀마다 규격에서 계산한 기대값을 만든다.
        for (int i = 0; i < exp.length; i++) {
            exp[i] = expected(s, i);
        }
        ImageAsserts.pixels(img, s.width, s.height, exp, what);
    }

    /**
     * A palette index that is outside the palette gives opaque black; a PLTE with fewer entries than the bit depth allows is fine.
     * <p>
     * 팔레트 밖의 인덱스는 불투명 검정이 되며 비트 심도가 허용하는 것보다 항목이 적은 PLTE 도 문제없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPaletteIndexOutsidePalette() throws Exception {
        PngBuilder.Spec s = new PngBuilder.Spec(4, 1);
        s.colorType = 3;
        s.depth = 8;
        s.samples = new int[] { 0, 1, 2, 200 };
        s.plte = new byte[] { (byte) 255, 0, 0, 0, (byte) 255, 0 };
        DecodedImage img = new PngDecoder().decode(PngBuilder.encode(s), DecodeLimits.DEFAULT);
        ImageAsserts.pixels(img, 4, 1, new int[] { 0xFFFF0000, 0xFF00FF00, 0xFF000000, 0xFF000000 }, "palette overflow");
    }

    /**
     * The image data may be split into many IDAT chunks of any size, even one byte each.
     * <p>
     * 이미지 데이터는 임의 크기(심지어 1바이트)의 여러 IDAT 청크로 나뉠 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testManyIdatChunks() throws Exception {
        // 청크 크기를 바꿔 가며 같은 이미지를 인코딩한다.
        for (int size : new int[] { 1, 2, 7, 64, 1000 }) {
            PngBuilder.Spec s = spec(23, 17, 6, 8, true);
            s.idatSize = size;
            check(s, "IDAT size " + size);
        }
    }

    /**
     * Colour management chunks are ignored: gAMA, cHRM, sRGB and iCCP do not change the pixels.
     * <p>
     * 색 관리 청크는 무시한다: gAMA, cHRM, sRGB, iCCP 는 픽셀을 바꾸지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testColourChunksAreIgnored() throws Exception {
        PngBuilder.Spec s = spec(5, 5, 2, 8, false);
        s.extraChunks.add(PngBuilder.chunk("gAMA", new Bytes().be32(100000).toBytes()));
        s.extraChunks.add(PngBuilder.chunk("cHRM", new byte[32]));
        s.extraChunks.add(PngBuilder.chunk("sRGB", new byte[] { 0 }));
        s.extraChunks.add(PngBuilder.chunk("iCCP", new byte[] { 'x', 0, 0, 1, 2, 3 }));
        s.extraChunks.add(PngBuilder.chunk("tEXt", new Bytes().ascii("Comment").u8(0).ascii("hello").toBytes()));
        check(s, "colour chunks");
    }

    /**
     * A wrong CRC in a critical chunk makes the image CORRUPT; a wrong CRC in an ancillary chunk only skips that chunk.
     * <p>
     * 필수 청크의 CRC 가 틀리면 이미지가 CORRUPT 이고 보조 청크의 CRC 가 틀리면 그 청크만 건너뛴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testChunkCrc() throws Exception {
        final PngBuilder.Spec s = spec(9, 9, 6, 8, false);
        byte[] png = PngBuilder.encode(s);
        // IDAT 데이터 한 바이트를 바꾼다(CRC 는 그대로).
        int idat = indexOf(png, "IDAT");
        final byte[] bad = png.clone();
        bad[idat + 6] ^= 0x55;
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes the image with a damaged IDAT chunk.
             * <p>
             * IDAT 청크가 손상된 이미지를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(bad, DecodeLimits.DEFAULT);
            }
        }, "IDAT CRC");
        // 보조 청크의 CRC 를 깨뜨려도 이미지는 그대로 디코딩된다.
        PngBuilder.Spec t = spec(9, 9, 6, 8, false);
        byte[] text = PngBuilder.chunk("tEXt", new Bytes().ascii("k").u8(0).ascii("v").toBytes());
        text[text.length - 1] ^= 0x0F;
        t.extraChunks.add(text);
        check(t, "ancillary chunk with a wrong CRC");
        // tRNS 의 CRC 가 틀리면 그 청크를 건너뛰므로 투명도가 없어진다.
        PngBuilder.Spec u = spec(3, 3, 2, 8, false);
        u.trns = new byte[] { 0, (byte) u.samples[0], 0, (byte) u.samples[1], 0, (byte) u.samples[2] };
        byte[] withTrns = PngBuilder.encode(u);
        int tr = indexOf(withTrns, "tRNS");
        withTrns[tr + 1] ^= 0x01;
        DecodedImage noTrns = new PngDecoder().decode(withTrns, DecodeLimits.DEFAULT);
        Check.equal(0xFF, noTrns.getFrames().get(0).getArgb()[0] >>> 24, "a skipped tRNS gives an opaque pixel");
        // 필수 IHDR 의 CRC 가 틀려도 CORRUPT.
        final byte[] badHdr = png.clone();
        badHdr[8 + 8 + 2] ^= 0x01;
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes the image with a damaged IHDR chunk.
             * <p>
             * IHDR 청크가 손상된 이미지를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(badHdr, DecodeLimits.DEFAULT);
            }
        }, "IHDR CRC");
    }

    /**
     * Finds a chunk type in a PNG file and returns the position of its data (after length and type).
     * <p>
     * PNG 파일에서 청크 유형을 찾아 데이터의 위치(길이와 유형 뒤)를 반환한다.
     *
     * @param d    file / 파일
     * @param type chunk type / 청크 유형
     * @return position of the chunk data / 청크 데이터의 위치
     */
    private static int indexOf(byte[] d, String type) {
        // 청크 구조를 따라가며 유형을 찾는다.
        int pos = 8;
        while (pos + 8 <= d.length) {
            int len = ((d[pos] & 0xFF) << 24) | ((d[pos + 1] & 0xFF) << 16) | ((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF);
            String t = new String(d, pos + 4, 4);
            if (t.equals(type)) {
                return pos + 8;
            }
            pos += 12 + len;
        }
        throw new IllegalStateException("chunk not found: " + type);
    }

    /**
     * Data that ends in the middle of the image stream is CORRUPT; a missing IEND chunk is tolerated; a missing end of the stream with
     * enough data is fine.
     * <p>
     * 이미지 스트림 중간에서 끝나는 데이터는 CORRUPT 이고 IEND 가 없는 것은 허용하며 데이터가 충분하면 스트림 끝이 없어도 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncation() throws Exception {
        final PngBuilder.Spec s = spec(40, 40, 6, 8, false);
        final byte[] png = PngBuilder.encode(s);
        int idat = indexOf(png, "IDAT");
        // IDAT 중간에서 자르면 CORRUPT 이다.
        final byte[] half = ImageAsserts.prefix(png, idat + 30);
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes a file cut in the middle of IDAT.
             * <p>
             * IDAT 중간에서 잘린 파일을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(half, DecodeLimits.DEFAULT);
            }
        }, "cut inside IDAT");
        // IEND 가 없어도 이미지 데이터가 완전하면 디코딩된다.
        byte[] noIend = ImageAsserts.prefix(png, png.length - 12);
        check(s, "baseline");
        DecodedImage img = new PngDecoder().decode(noIend, DecodeLimits.DEFAULT);
        Check.equal(40, img.getWidth(), "no IEND: width");
        // 파일 앞부분만 있는 경우들도 CORRUPT 로 끝나야 한다.
        for (final int n : new int[] { 0, 3, 8, 12, 20, 33, 41 }) {
            ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
                /**
                 * Decodes a very short prefix of the file.
                 * <p>
                 * 파일의 아주 짧은 앞부분을 디코딩한다.
                 *
                 * @throws Exception always MediaException / 항상 MediaException
                 */
                public void run() throws Exception {
                    new PngDecoder().decode(ImageAsserts.prefix(png, n), DecodeLimits.DEFAULT);
                }
            }, "prefix of " + n + " bytes");
        }
    }

    /**
     * Invalid headers are CORRUPT: bad colour type or bit depth, missing palette, unknown filter, missing image data, IHDR not first.
     * <p>
     * 올바르지 않은 헤더는 CORRUPT 이다: 잘못된 색 유형이나 비트 심도, 팔레트 없음, 알 수 없는 필터, 이미지 데이터 없음, IHDR 가 첫 청크가 아님.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidStructures() throws Exception {
        corrupt(rawPng(5, 5, 8, 1, 0, new byte[0]), "colour type 1");
        corrupt(rawPng(5, 5, 3, 0, 0, new byte[0]), "grey 3 bits");
        corrupt(rawPng(5, 5, 16, 3, 0, new byte[0]), "16-bit palette");
        corrupt(rawPng(5, 5, 4, 2, 0, new byte[0]), "4-bit RGB");
        corrupt(rawPng(5, 5, 8, 2, 2, new byte[0]), "interlace method 2");
        // 팔레트 유형인데 PLTE 가 없다.
        PngBuilder.Spec s = spec(4, 4, 3, 8, false);
        s.plte = null;
        corrupt(PngBuilder.encode(s), "palette image without PLTE");
        // 알 수 없는 필터 유형(7).
        PngBuilder.Spec f = spec(4, 4, 0, 8, false);
        byte[] raw = PngBuilder.rawStream(f, f.samples, 4, 4);
        raw[0] = 7;
        corrupt(rawPng(4, 4, 8, 0, 0, PngBuilder.deflate(raw)), "filter type 7");
        // IDAT 가 없다.
        Bytes noIdat = new Bytes();
        noIdat.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        noIdat.put(PngBuilder.chunk("IHDR", new Bytes().be32(2).be32(2).u8(8).u8(0).u8(0).u8(0).u8(0).toBytes()));
        noIdat.put(PngBuilder.chunk("IEND", new byte[0]));
        corrupt(noIdat.toBytes(), "no IDAT");
        // IHDR 가 첫 청크가 아니다.
        Bytes late = new Bytes();
        late.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        late.put(PngBuilder.chunk("tEXt", new Bytes().ascii("a").u8(0).ascii("b").toBytes()));
        late.put(PngBuilder.chunk("IHDR", new Bytes().be32(2).be32(2).u8(8).u8(0).u8(0).u8(0).u8(0).toBytes()));
        corrupt(late.toBytes(), "IHDR not first");
        // 압축 스트림이 깨졌다.
        corrupt(rawPng(4, 4, 8, 0, 0, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8 }), "garbage zlib stream");
        // 크기가 0 이다.
        corrupt(rawPng(0, 5, 8, 0, 0, new byte[0]), "zero width");
        corrupt(rawPng(5, 0, 8, 0, 0, new byte[0]), "zero height");
    }

    /**
     * Builds a PNG with a given header and a single IDAT chunk holding the given bytes.
     * <p>
     * 주어진 헤더와 주어진 바이트를 담은 IDAT 청크 하나로 PNG 를 만든다.
     *
     * @param w         width / 가로
     * @param h         height / 세로
     * @param depth     bit depth / 비트 심도
     * @param ct        colour type / 색 유형
     * @param interlace interlace method / 인터레이스 방식
     * @param idat      IDAT data / IDAT 데이터
     * @return PNG bytes / PNG 바이트
     */
    private static byte[] rawPng(int w, int h, int depth, int ct, int interlace, byte[] idat) {
        Bytes b = new Bytes();
        b.put(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        b.put(PngBuilder.chunk("IHDR", new Bytes().be32(w).be32(h).u8(depth).u8(ct).u8(0).u8(0).u8(interlace).toBytes()));
        b.put(PngBuilder.chunk("IDAT", idat));
        b.put(PngBuilder.chunk("IEND", new byte[0]));
        return b.toBytes();
    }

    /**
     * Checks that data is rejected as CORRUPT.
     * <p>
     * 데이터가 CORRUPT 로 거부되는지 검사한다.
     *
     * @param data the data / 데이터
     * @param what description / 설명
     */
    private static void corrupt(final byte[] data, String what) {
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes the data.
             * <p>
             * 데이터를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(data, DecodeLimits.DEFAULT);
            }
        }, what);
    }

    /**
     * A header that claims a huge size is rejected as TOO_LARGE before any buffer is allocated (fast, no memory); the limits can be changed.
     * <p>
     * 아주 큰 크기를 주장하는 헤더는 버퍼를 할당하기 전에 TOO_LARGE 로 거부된다(빠르고 메모리를 쓰지 않음). 제한은 바꿀 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHugeSizeClaims() throws Exception {
        final byte[] huge = rawPng(100000, 100000, 8, 6, 0, PngBuilder.deflate(new byte[10]));
        long start = System.currentTimeMillis();
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes the header that claims 100000 x 100000 pixels.
             * <p>
             * 100000 x 100000 픽셀을 주장하는 헤더를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(huge, DecodeLimits.DEFAULT);
            }
        }, "100000 x 100000");
        Check.isTrue(System.currentTimeMillis() - start < 5000, "the rejection is immediate");
        final byte[] max = rawPng(0x7FFFFFFF, 0x7FFFFFFF, 8, 0, 0, new byte[0]);
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes the header that claims the largest allowed size.
             * <p>
             * 허용되는 가장 큰 크기를 주장하는 헤더를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(max, DecodeLimits.DEFAULT);
            }
        }, "2147483647 x 2147483647");
        // 제한을 낮추면 정상 이미지도 거부된다.
        final byte[] ok = PngBuilder.encode(spec(20, 20, 6, 8, false));
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes a 20 x 20 image with a limit of 100 pixels.
             * <p>
             * 100 픽셀 제한으로 20 x 20 이미지를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(ok, new DecodeLimits(100, 1 << 20, 10, 1000));
            }
        }, "pixel limit");
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes data that is larger than the byte limit.
             * <p>
             * 바이트 제한보다 큰 데이터를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new PngDecoder().decode(ok, new DecodeLimits(1000000, 50, 10, 1000000));
            }
        }, "byte limit");
    }

    /**
     * A PNG written by the JRE's ImageIO (RGB, RGBA, palette, interlaced) decodes to the same pixels: a cross-check of the builder and the
     * decoder against a third implementation.
     * <p>
     * JRE 의 ImageIO 가 쓴 PNG(RGB, RGBA, 팔레트, 인터레이스)가 같은 픽셀로 디코딩된다: 빌더와 디코더를 세 번째 구현과 교차 확인한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPngWrittenByImageIo() throws Exception {
        BufferedImage rgba = new BufferedImage(13, 9, BufferedImage.TYPE_INT_ARGB);
        BufferedImage rgb = new BufferedImage(13, 9, BufferedImage.TYPE_INT_RGB);
        // 위치마다 다른 색을 칠한다.
        for (int y = 0; y < 9; y++) {
            for (int x = 0; x < 13; x++) {
                int c = ((x * 19) << 16) | ((y * 27) << 8) | ((x * y * 5) & 0xFF);
                rgba.setRGB(x, y, ((x * 20 + 5) << 24) | c);
                rgb.setRGB(x, y, c);
            }
        }
        BufferedImage indexed = new BufferedImage(13, 9, BufferedImage.TYPE_BYTE_INDEXED);
        // 각 항목을 차례로 처리한다.
        for (int y = 0; y < 9; y++) {
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < 13; x++) {
                indexed.setRGB(x, y, rgb.getRGB(x, y));
            }
        }
        BufferedImage[] imgs = { rgba, rgb, indexed };
        String[] names = { "ARGB", "RGB", "indexed" };
        // 세 종류의 이미지를 ImageIO 로 쓰고 읽어 본 뒤 우리 디코더와 비교한다.
        for (int i = 0; i < imgs.length; i++) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Check.isTrue(ImageIO.write(imgs[i], "png", bos), "ImageIO writes PNG: " + names[i]);
            byte[] png = bos.toByteArray();
            BufferedImage ref = ImageIO.read(new ByteArrayInputStream(png));
            DecodedImage img = new PngDecoder().decode(png, DecodeLimits.DEFAULT);
            int[] exp = new int[13 * 9];
            // 각 항목을 차례로 처리한다.
            for (int y = 0; y < 9; y++) {
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < 13; x++) {
                    exp[y * 13 + x] = ref.getRGB(x, y);
                }
            }
            ImageAsserts.pixels(img, 13, 9, exp, "ImageIO " + names[i]);
        }
    }

    /**
     * An interrupted thread makes the decoder stop with CANCELLED and keep the interrupt flag.
     * <p>
     * 인터럽트된 스레드는 디코더를 CANCELLED 로 멈추게 하며 인터럽트 플래그는 유지된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        final byte[] png = PngBuilder.encode(spec(64, 64, 6, 8, false));
        Thread.currentThread().interrupt();
        try {
            ImageAsserts.fails(MediaException.Kind.CANCELLED, new Block() {
                /**
                 * Decodes while the thread is interrupted.
                 * <p>
                 * 스레드가 인터럽트된 상태에서 디코딩한다.
                 *
                 * @throws Exception always MediaException / 항상 MediaException
                 */
                public void run() throws Exception {
                    new PngDecoder().decode(png, DecodeLimits.DEFAULT);
                }
            }, "interrupted decode");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is still set");
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Garbage input never makes the decoder fail with anything but a {@link MediaException}: random mutations and truncations of valid files.
     * <p>
     * 쓰레기 입력은 디코더가 {@link MediaException} 말고 다른 것으로 실패하게 하지 않는다: 올바른 파일의 임의 변형과 잘라냄.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMutationsNeverEscape() throws Exception {
        DecodeLimits small = new DecodeLimits(1000000, 1 << 22, 100, 4000000);
        java.util.Random rnd = new java.util.Random(12345);
        byte[][] seeds = { PngBuilder.encode(spec(21, 15, 6, 8, true)), PngBuilder.encode(spec(21, 15, 3, 4, false)),
                PngBuilder.encode(spec(21, 15, 2, 16, true)) };
        // 시드 파일마다 여러 번 변형해서 디코딩한다.
        for (byte[] seed : seeds) {
            for (int round = 0; round < 300; round++) {
                byte[] m = seed.clone();
                int flips = 1 + rnd.nextInt(4);
                // 각 항목을 차례로 처리한다.
                for (int k = 0; k < flips; k++) {
                    m[rnd.nextInt(m.length)] = (byte) rnd.nextInt(256);
                }
                if (round % 3 == 0) {
                    m = java.util.Arrays.copyOf(m, rnd.nextInt(m.length));
                }
                try {
                    DecodedImage img = new PngDecoder().decode(m, small);
                    Check.isTrue(img.getWidth() > 0, "a decoded image has a size");
                } catch (MediaException expected) {
                    Check.notNull(expected.getKind(), "every failure has a kind");
                }
            }
        }
    }

    /**
     * The CRC helper of this test agrees with {@link java.util.zip.CRC32} (guards the builder itself).
     * <p>
     * 이 시험의 CRC 도우미가 {@link java.util.zip.CRC32} 와 같다(빌더 자체를 지킨다).
     */
    public void testBuilderChunkFormat() {
        byte[] c = PngBuilder.chunk("IEND", new byte[0]);
        Check.equal(12, c.length, "an empty chunk has 12 bytes");
        CRC32 crc = new CRC32();
        crc.update(new byte[] { 'I', 'E', 'N', 'D' }, 0, 4);
        long v = ((long) (c[8] & 0xFF) << 24) | ((c[9] & 0xFF) << 16) | ((c[10] & 0xFF) << 8) | (c[11] & 0xFF);
        Check.equal(crc.getValue(), v, "CRC of IEND is the well known value");
        Check.equal(0xAE426082L, v, "CRC of IEND");
    }
}
