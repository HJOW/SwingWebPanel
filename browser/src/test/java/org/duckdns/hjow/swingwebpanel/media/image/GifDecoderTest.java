package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
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
 * Tests of {@link GifDecoder} (plan items P18-02, P18-03; FEATURE_MATRIX IMG-03): LZW with code size growth and table resets (cross-checked
 * with the JRE's GIF reader and writer), colour tables, transparency, interlacing, disposal methods, delays, loop counts, damaged and hostile
 * data.
 * <p>
 * {@link GifDecoder} 시험(계획 P18-02, P18-03; FEATURE_MATRIX IMG-03): 코드 크기 증가와 표 초기화를 포함한 LZW(JRE 의 GIF 리더/라이터와 교차
 * 확인), 색 표, 투명도, 인터레이스, 처리 방법, 지연, 반복 횟수, 손상되거나 악의적인 데이터.
 */
public class GifDecoderTest {

    /**
     * A four colour palette: red, green, blue, white.
     * <p>
     * 네 가지 색 팔레트: 빨강, 초록, 파랑, 흰색.
     */
    private static final int[] PAL4 = { 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF };

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public GifDecoderTest() {
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
        return new GifDecoder().decode(d, DecodeLimits.DEFAULT);
    }

    /**
     * Returns an indexed bytes array filled with a value.
     * <p>
     * 한 값으로 채운 인덱스 배열을 반환한다.
     *
     * @param n     length / 길이
     * @param value index / 인덱스
     * @return indexes / 인덱스들
     */
    private static byte[] fill(int n, int value) {
        byte[] b = new byte[n];
        Arrays.fill(b, (byte) value);
        return b;
    }

    /**
     * Converts palette indexes to expected opaque pixels.
     * <p>
     * 팔레트 인덱스를 기대하는 불투명 픽셀로 바꾼다.
     *
     * @param idx     indexes / 인덱스
     * @param palette palette as 0xRRGGBB / 0xRRGGBB 팔레트
     * @return pixels / 픽셀
     */
    private static int[] colours(byte[] idx, int[] palette) {
        int[] out = new int[idx.length];
        // 인덱스마다 팔레트 색을 불투명으로 바꾼다.
        for (int i = 0; i < idx.length; i++) {
            out[i] = 0xFF000000 | palette[idx[i] & 0xFF];
        }
        return out;
    }

    /**
     * Random indexes below a limit.
     * <p>
     * 한도 미만의 임의 인덱스.
     *
     * @param n     length / 길이
     * @param limit exclusive limit / 미포함 한도
     * @param seed  random seed / 난수 시드
     * @return indexes / 인덱스들
     */
    private static byte[] random(int n, int limit, long seed) {
        Random r = new Random(seed);
        byte[] b = new byte[n];
        // 위치마다 임의 인덱스를 만든다.
        for (int i = 0; i < n; i++) {
            b[i] = (byte) r.nextInt(limit);
        }
        return b;
    }

    /**
     * A small image and large random images decode exactly, which exercises LZW code size growth (9 to 12 bits) and table resets; the files
     * are also read by the JRE's own GIF reader, to prove the test encoder is right.
     * <p>
     * 작은 이미지와 큰 임의 이미지가 정확히 디코딩되며 LZW 코드 크기 증가(9~12비트)와 표 초기화가 실행된다. 시험 인코더가 올바름을 증명하려고 JRE 의
     * GIF 리더로도 같은 파일을 읽는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLzwGrowthAndReset() throws Exception {
        byte[] small = new byte[16 * 16];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < small.length; i++) {
            small[i] = (byte) ((i / 3 + i / 16) % 4);
        }
        byte[] gif = new GifBuilder(16, 16, PAL4).frame(0, 0, 16, 16, small, null, false, 0, 0, -1, 2).build();
        ImageAsserts.pixels(decode(gif), 16, 16, colours(small, PAL4), "16x16 with 2-bit codes");
        int[] pal256 = new int[256];
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < 256; i++) {
            pal256[i] = (i * 0x010203) & 0xFFFFFF;
        }
        // 코드 크기와 데이터 종류를 바꿔 가며 같은 검사를 한다.
        int[][] cases = { { 200, 150, 8, 256 }, { 300, 100, 4, 16 }, { 120, 120, 2, 4 }, { 64, 64, 8, 2 }, { 1, 1, 2, 4 }, { 500, 1, 8, 256 } };
        for (int[] c : cases) {
            byte[] idx = random(c[0] * c[1], c[3], c[0] * 31 + c[1]);
            int[] pal = c[3] == 4 ? PAL4 : Arrays.copyOf(pal256, Math.max(2, c[3]));
            byte[] big = new GifBuilder(c[0], c[1], pal).frame(0, 0, c[0], c[1], idx, null, false, 0, 0, -1, c[2]).build();
            String what = c[0] + "x" + c[1] + " min code " + c[2];
            ImageAsserts.pixels(decode(big), c[0], c[1], colours(idx, pal), what);
            BufferedImage ref = ImageIO.read(new ByteArrayInputStream(big));
            Raster r = ref.getRaster();
            int[] got = new int[1];
            // JRE 의 읽기 결과와 우리가 넣은 인덱스가 같은지 확인한다.
            for (int y = 0; y < c[1]; y++) {
                for (int x = 0; x < c[0]; x++) {
                    Check.equal(idx[y * c[0] + x] & 0xFF, r.getPixel(x, y, got)[0], what + ": ImageIO index at " + x + "," + y);
                }
            }
        }
    }

    /**
     * A GIF written by the JRE's ImageIO decodes to the same pixels as the JRE's reader gives.
     * <p>
     * JRE 의 ImageIO 가 쓴 GIF 가 JRE 리더가 주는 것과 같은 픽셀로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGifWrittenByImageIo() throws Exception {
        BufferedImage img = new BufferedImage(37, 23, BufferedImage.TYPE_BYTE_INDEXED);
        // 위치마다 다른 색을 칠한다(양자화는 ImageIO 가 한다).
        for (int y = 0; y < 23; y++) {
            for (int x = 0; x < 37; x++) {
                img.setRGB(x, y, ((x * 7) << 16) | ((y * 11) << 8) | ((x + y) * 3));
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Check.isTrue(ImageIO.write(img, "gif", bos), "ImageIO writes GIF");
        byte[] gif = bos.toByteArray();
        BufferedImage ref = ImageIO.read(new ByteArrayInputStream(gif));
        int[] exp = new int[37 * 23];
        // 각 항목을 차례로 처리한다.
        for (int y = 0; y < 23; y++) {
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < 37; x++) {
                exp[y * 37 + x] = ref.getRGB(x, y);
            }
        }
        ImageAsserts.pixels(decode(gif), 37, 23, exp, "ImageIO GIF");
    }

    /**
     * Transparent indexes leave the canvas transparent; local colour tables replace the global table; interlaced rows are put in place.
     * <p>
     * 투명 인덱스는 캔버스를 투명하게 두고 지역 색 표는 전역 색 표를 대체하며 인터레이스 행은 제자리에 놓인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTransparencyLocalPaletteInterlace() throws Exception {
        byte[] idx = { 0, 1, 2, 3, 3, 2, 1, 0 };
        byte[] t = new GifBuilder(4, 2, PAL4).frame(0, 0, 4, 2, idx, null, false, 0, 0, 1, 2).build();
        int[] e = colours(idx, PAL4);
        e[1] = 0;
        e[6] = 0;
        ImageAsserts.pixels(decode(t), 4, 2, e, "transparent index 1");
        int[] local = { 0x112233, 0x445566, 0x778899, 0xAABBCC };
        byte[] l = new GifBuilder(4, 2, PAL4).frame(0, 0, 4, 2, idx, local, false, 0, 0, -1, 2).build();
        ImageAsserts.pixels(decode(l), 4, 2, colours(idx, local), "local colour table");
        byte[] noGlobal = new GifBuilder(4, 2, null).frame(0, 0, 4, 2, idx, local, false, 0, 0, -1, 2).build();
        ImageAsserts.pixels(decode(noGlobal), 4, 2, colours(idx, local), "local table without a global one");
        // 높이가 다른 인터레이스 이미지를 모두 확인한다.
        for (int h : new int[] { 1, 2, 3, 5, 8, 9, 13, 17 }) {
            byte[] px = random(9 * h, 4, h);
            byte[] il = new GifBuilder(9, h, PAL4).frame(0, 0, 9, h, px, null, true, 0, 0, -1, 2).build();
            ImageAsserts.pixels(decode(il), 9, h, colours(px, PAL4), "interlaced height " + h);
        }
    }

    /**
     * Builds the four frame animation used by the disposal tests.
     * <p>
     * 처리 방법 시험에 쓰는 네 프레임 애니메이션을 만든다.
     *
     * @param loop loop count to write, or -1 for no loop extension / 쓸 반복 횟수. -1 이면 반복 확장 없음
     * @return the builder / 빌더
     */
    private static GifBuilder animation(int loop) {
        GifBuilder b = new GifBuilder(4, 4, PAL4);
        if (loop >= 0) {
            b.loop(loop);
        }
        b.frame(0, 0, 4, 4, fill(16, 0), null, false, 1, 10, -1, 2);
        b.frame(1, 1, 2, 2, fill(4, 1), null, false, 2, 20, -1, 2);
        b.frame(0, 0, 2, 2, fill(4, 2), null, false, 3, 0, -1, 2);
        b.frame(3, 3, 1, 1, fill(1, 3), null, false, 0, 5, -1, 2);
        return b;
    }

    /**
     * Fills a rectangle of an expected 4 x 4 canvas.
     * <p>
     * 기대하는 4 x 4 캔버스의 사각형을 채운다.
     *
     * @param c    canvas / 캔버스
     * @param x    left / 왼쪽
     * @param y    top / 위쪽
     * @param w    width / 가로
     * @param h    height / 세로
     * @param argb colour / 색
     */
    private static void rect(int[] c, int x, int y, int w, int h, int argb) {
        // 사각형 안의 모든 픽셀을 채운다.
        for (int yy = y; yy < y + h; yy++) {
            for (int xx = x; xx < x + w; xx++) {
                c[yy * 4 + xx] = argb;
            }
        }
    }

    /**
     * Disposal methods 1, 2 and 3 compose frames correctly, delays become milliseconds (10 ms or less becomes 100 ms).
     * <p>
     * 처리 방법 1, 2, 3 이 프레임을 올바르게 합성하며 지연은 밀리초가 된다(10 ms 이하는 100 ms).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDisposalAndDelays() throws Exception {
        DecodedImage img = decode(animation(-1).build());
        Check.equal(4, img.getFrames().size(), "four frames");
        Check.isTrue(img.isAnimated(), "animated");
        int red = 0xFFFF0000;
        int[] e0 = new int[16];
        Arrays.fill(e0, red);
        ImageAsserts.framePixels(img.getFrames().get(0), e0, "frame 0");
        int[] e1 = e0.clone();
        rect(e1, 1, 1, 2, 2, 0xFF00FF00);
        ImageAsserts.framePixels(img.getFrames().get(1), e1, "frame 1");
        // 프레임 1 이 배경 처리이므로 그 영역은 투명해진 뒤 프레임 2 가 그려진다.
        int[] cleared = e0.clone();
        rect(cleared, 1, 1, 2, 2, 0);
        int[] e2 = cleared.clone();
        rect(e2, 0, 0, 2, 2, 0xFF0000FF);
        ImageAsserts.framePixels(img.getFrames().get(2), e2, "frame 2 after the background disposal of frame 1");
        // 프레임 2 가 이전 상태 복원이므로 프레임 3 은 프레임 2 이전 상태 위에 그려진다.
        int[] e3 = cleared.clone();
        e3[15] = 0xFFFFFFFF;
        ImageAsserts.framePixels(img.getFrames().get(3), e3, "frame 3 after restoring the previous state");
        Check.equal(100, img.getFrames().get(0).getDelayMillis(), "10 cs");
        Check.equal(200, img.getFrames().get(1).getDelayMillis(), "20 cs");
        Check.equal(100, img.getFrames().get(2).getDelayMillis(), "0 cs becomes 100 ms");
        Check.equal(50, img.getFrames().get(3).getDelayMillis(), "5 cs");
        byte[] fast = new GifBuilder(2, 2, PAL4).frame(0, 0, 2, 2, fill(4, 0), null, false, 0, 1, -1, 2)
                .frame(0, 0, 2, 2, fill(4, 1), null, false, 0, 2, -1, 2).build();
        Check.equal(100, decode(fast).getFrames().get(0).getDelayMillis(), "1 cs becomes 100 ms");
        Check.equal(20, decode(fast).getFrames().get(1).getDelayMillis(), "2 cs stays 20 ms");
    }

    /**
     * The NETSCAPE2.0 loop count: absent means one play, 0 forever, N means N + 1 plays.
     * <p>
     * NETSCAPE2.0 반복 횟수: 없으면 한 번, 0 이면 무한, N 이면 N + 1 번 재생한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testLoopCounts() throws Exception {
        Check.equal(1, decode(animation(-1).build()).getLoopCount(), "no extension");
        Check.equal(0, decode(animation(0).build()).getLoopCount(), "loop forever");
        Check.equal(3, decode(animation(2).build()).getLoopCount(), "loop count 2");
        Check.equal(1, decode(new GifBuilder(2, 2, PAL4).comment("hello").frame(0, 0, 2, 2, fill(4, 1), null, false, 0, 0, -1, 2).build())
                .getLoopCount(), "a comment does not matter");
    }

    /**
     * Frame positions: a first frame larger than the screen enlarges the canvas, a screen size of 0 takes the frame extent, later frames are
     * clipped.
     * <p>
     * 프레임 위치: 화면보다 큰 첫 프레임은 캔버스를 넓히고 화면 크기 0 은 프레임 범위를 쓰며 이후 프레임은 잘린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCanvasSize() throws Exception {
        DecodedImage a = decode(new GifBuilder(2, 2, PAL4).frame(0, 0, 4, 3, fill(12, 2), null, false, 0, 0, -1, 2).build());
        Check.equal(4, a.getWidth(), "enlarged width");
        Check.equal(3, a.getHeight(), "enlarged height");
        DecodedImage b = decode(new GifBuilder(0, 0, PAL4).frame(1, 1, 3, 2, fill(6, 1), null, false, 0, 0, -1, 2).build());
        Check.equal(4, b.getWidth(), "width from the frame extent");
        Check.equal(3, b.getHeight(), "height from the frame extent");
        Check.equal(0, b.getFrames().get(0).getArgb()[0], "the area outside the frame is transparent");
        DecodedImage c = decode(new GifBuilder(3, 3, PAL4).frame(0, 0, 3, 3, fill(9, 0), null, false, 0, 0, -1, 2)
                .frame(2, 2, 3, 3, fill(9, 1), null, false, 0, 0, -1, 2).build());
        Check.equal(3, c.getWidth(), "the later frame does not enlarge the canvas");
        Check.equal(0xFF00FF00, c.getFrames().get(1).getArgb()[8], "the visible corner of the clipped frame");
        Check.equal(0xFFFF0000, c.getFrames().get(1).getArgb()[0], "the rest keeps the first frame");
    }

    /**
     * A file cut in the middle of a later frame keeps the complete frames, a file cut inside the first frame is CORRUPT, and a missing
     * trailer is fine.
     * <p>
     * 이후 프레임 중간에서 잘린 파일은 완전한 프레임을 유지하고 첫 프레임 안에서 잘린 파일은 CORRUPT 이며 트레일러가 없는 것은 허용한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncation() throws Exception {
        GifBuilder b = new GifBuilder(8, 8, PAL4);
        b.frame(0, 0, 8, 8, random(64, 4, 1), null, false, 0, 10, -1, 2);
        int firstEnd = b.buildWithoutTrailer().length;
        b.frame(0, 0, 8, 8, random(64, 4, 2), null, false, 0, 10, -1, 2);
        byte[] full = b.build();
        Check.equal(2, decode(full).getFrames().size(), "both frames");
        Check.equal(2, decode(b.buildWithoutTrailer()).getFrames().size(), "no trailer");
        DecodedImage one = decode(ImageAsserts.prefix(full, firstEnd + 25));
        Check.equal(1, one.getFrames().size(), "cut in the second frame keeps the first");
        final byte[] cutFirst = ImageAsserts.prefix(full, 13 + 12 + 30);
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes a file cut inside its first frame.
             * <p>
             * 첫 프레임 안에서 잘린 파일을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(cutFirst);
            }
        }, "cut inside the first frame");
        // 아주 짧은 앞부분은 모두 CORRUPT 이어야 한다.
        for (final int n : new int[] { 0, 5, 6, 12, 13, 14, 20 }) {
            ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
                /**
                 * Decodes a very short prefix.
                 * <p>
                 * 아주 짧은 앞부분을 디코딩한다.
                 *
                 * @throws Exception always MediaException / 항상 MediaException
                 */
                public void run() throws Exception {
                    decode(ImageAsserts.prefix(new GifBuilder(2, 2, PAL4).frame(0, 0, 2, 2, fill(4, 1), null, false, 0, 0, -1, 2).build(), n));
                }
            }, "prefix of " + n + " bytes");
        }
    }

    /**
     * Invalid structures are CORRUPT; oversize claims are TOO_LARGE before anything is allocated; frame limits are enforced.
     * <p>
     * 올바르지 않은 구조는 CORRUPT 이고 과대한 크기 주장은 할당 전에 TOO_LARGE 이며 프레임 제한이 지켜진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInvalidAndLimits() throws Exception {
        final byte[] ok = new GifBuilder(4, 4, PAL4).frame(0, 0, 4, 4, fill(16, 1), null, false, 0, 0, -1, 2).build();
        final byte[] noTable = new GifBuilder(4, 4, null).frame(0, 0, 4, 4, fill(16, 1), null, false, 0, 0, -1, 2).build();
        ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
            /**
             * Decodes a frame without any colour table.
             * <p>
             * 색 표가 전혀 없는 프레임을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(noTable);
            }
        }, "no colour table");
        // LZW 최소 코드 크기가 범위(2~8)를 벗어나면 CORRUPT 이다.
        for (final int bad : new int[] { 1, 9, 12 }) {
            final byte[] g = new GifBuilder(4, 4, PAL4).frame(0, 0, 4, 4, fill(16, 1), null, false, 0, 0, -1, 2).build();
            int pos = indexOfImage(g) + 10;
            g[pos] = (byte) bad;
            ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
                /**
                 * Decodes a frame with an invalid LZW code size.
                 * <p>
                 * LZW 코드 크기가 잘못된 프레임을 디코딩한다.
                 *
                 * @throws Exception always MediaException / 항상 MediaException
                 */
                public void run() throws Exception {
                    decode(g);
                }
            }, "LZW code size " + bad);
        }
        final byte[] hugeScreen = ImageAsserts.patched(ok, 6, 0xFF, 0xFF, 0xFF, 0xFF);
        long start = System.currentTimeMillis();
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes a file that claims a 65535 x 65535 screen.
             * <p>
             * 65535 x 65535 화면을 주장하는 파일을 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                decode(hugeScreen);
            }
        }, "65535 x 65535 screen");
        Check.isTrue(System.currentTimeMillis() - start < 5000, "rejected at once");
        final byte[] manyFrames = animation(0).build();
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes four frames with a limit of two.
             * <p>
             * 제한이 2 일 때 프레임 네 개를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new GifDecoder().decode(manyFrames, new DecodeLimits(1000, 1 << 20, 2, 100000));
            }
        }, "frame limit");
        ImageAsserts.fails(MediaException.Kind.TOO_LARGE, new Block() {
            /**
             * Decodes four 16-pixel frames with a total limit of 40 pixels.
             * <p>
             * 전체 제한이 40 픽셀일 때 16 픽셀 프레임 네 개를 디코딩한다.
             *
             * @throws Exception always MediaException / 항상 MediaException
             */
            public void run() throws Exception {
                new GifDecoder().decode(manyFrames, new DecodeLimits(1000, 1 << 20, 100, 40));
            }
        }, "total pixel limit");
    }

    /**
     * Returns the position of the first image separator (0x2C) after the header of a file written by {@link GifBuilder} with a global table
     * of four colours and one graphic control extension.
     * <p>
     * 전역 색 표 4색과 그래픽 제어 확장 하나로 {@link GifBuilder} 가 쓴 파일에서 헤더 뒤 첫 이미지 구분자(0x2C)의 위치를 반환한다.
     *
     * @param g file / 파일
     * @return index of the separator byte / 구분자 바이트의 위치
     */
    private static int indexOfImage(byte[] g) {
        // 헤더(13)와 전역 색 표(12) 뒤의 확장(8바이트)을 지나 구분자를 찾는다.
        for (int i = 13 + 12; i < g.length; i++) {
            if ((g[i] & 0xFF) == 0x2C && (g[i - 1] & 0xFF) == 0 && (g[i - 8] & 0xFF) == 0x21) {
                return i;
            }
        }
        throw new IllegalStateException("no image separator");
    }

    /**
     * Mutated and truncated GIF files never fail with anything but a {@link MediaException}.
     * <p>
     * 변형되거나 잘린 GIF 파일은 {@link MediaException} 말고 다른 것으로 실패하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMutationsNeverEscape() throws Exception {
        DecodeLimits small = new DecodeLimits(1000000, 1 << 22, 100, 4000000);
        Random rnd = new Random(99);
        byte[][] seeds = { animation(0).build(),
                new GifBuilder(30, 20, PAL4).frame(0, 0, 30, 20, random(600, 4, 5), null, true, 0, 3, 1, 2).build() };
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
                    DecodedImage img = new GifDecoder().decode(m, small);
                    Check.isTrue(img.getFrames().size() > 0, "a decoded image has a frame");
                } catch (MediaException expected) {
                    Check.notNull(expected.getKind(), "every failure has a kind");
                }
            }
        }
    }

    /**
     * An interrupted thread stops the decoder with CANCELLED and keeps the interrupt flag.
     * <p>
     * 인터럽트된 스레드는 디코더를 CANCELLED 로 멈추게 하며 인터럽트 플래그를 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancellation() throws Exception {
        final byte[] gif = animation(0).build();
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
                    decode(gif);
                }
            }, "interrupted decode");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is still set");
        } finally {
            Thread.interrupted();
        }
    }
}
