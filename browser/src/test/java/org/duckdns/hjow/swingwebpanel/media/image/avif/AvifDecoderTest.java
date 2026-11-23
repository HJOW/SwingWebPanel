package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the AVIF decoder as a whole (plan item P18-03; FEATURE_MATRIX IMG-06): files written by libavif (through Pillow) are compared with
 * the pixels that libavif itself decodes, small files built around real AV1 payloads exercise the container features (item data box, extents,
 * rotation, mirroring, clean aperture, grids, alpha, premultiplied alpha), and malformed files must fail with the documented kinds of
 * {@link MediaException}.
 * <p>
 * AVIF 디코더 전체에 대한 시험(계획 P18-03; FEATURE_MATRIX IMG-06): libavif 가 (Pillow 를 통해) 쓴 파일을 libavif 가 직접 디코딩한 픽셀과 비교하고,
 * 실제 AV1 페이로드를 감싼 작은 파일로 컨테이너 기능(항목 데이터 상자, 범위, 회전, 거울, clean aperture, 격자, 알파, 미리 곱한 알파)을 시험하며,
 * 잘못된 파일은 문서화된 종류의 {@link MediaException} 으로 실패해야 한다.
 */
public class AvifDecoderTest {

    /**
     * The payload of a 64x48 4:2:0 stream.
     * <p>
     * 64x48 4:2:0 스트림의 페이로드.
     */
    private static final String MAIN = "aom_420_q30";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AvifDecoderTest() {
    }

    /**
     * Reads a stream of the AV1 fixtures.
     * <p>
     * AV1 시험 자료의 스트림을 읽는다.
     *
     * @param name the name / 이름
     * @return the bytes / 바이트
     * @throws Exception on a failure / 실패 시
     */
    private static byte[] obu(String name) throws Exception {
        return AvifTestKit.resource("/media/avif/av1/" + name + ".obu");
    }

    /**
     * Decodes a file with the decoder under test.
     * <p>
     * 시험 대상 디코더로 파일을 디코딩한다.
     *
     * @param file the file / 파일
     * @return the image / 이미지
     * @throws Exception on a failure / 실패 시
     */
    private static DecodedImage decode(byte[] file) throws Exception {
        return new AvifDecoder().decode(file, DecodeLimits.DEFAULT);
    }

    /**
     * The pixels of an image.
     * <p>
     * 이미지의 픽셀.
     *
     * @param img the image / 이미지
     * @return the ARGB pixels / ARGB 픽셀
     */
    private static int[] argb(DecodedImage img) {
        return img.getFrames().get(0).getArgb();
    }

    /**
     * A file with one AV1 item and the usual properties.
     * <p>
     * AV1 항목 하나와 일반적인 속성을 가진 파일.
     *
     * @param payload the AV1 data / AV1 데이터
     * @param w       the width / 너비
     * @param h       the height / 높이
     * @return the builder (not yet built) / 작성기(아직 만들지 않음)
     */
    private static AvifTestKit.Builder simple(byte[] payload, int w, int h) {
        AvifTestKit.Builder b = new AvifTestKit.Builder();
        b.item(1, "av01", payload);
        int ispe = b.prop(AvifTestKit.ispe(w, h));
        int cfg = b.prop(AvifTestKit.av1C());
        b.assoc(1, ispe, false).assoc(1, cfg, true);
        return b;
    }

    /**
     * The pixels that the pipeline gives for an AV1 stream (decoder and colour conversion) without any container property.
     * <p>
     * 컨테이너 속성 없이 AV1 스트림에 대해 파이프라인(디코더와 색 변환)이 주는 픽셀.
     *
     * @param name the stream / 스트림
     * @return the pixels / 픽셀
     * @throws Exception on a failure / 실패 시
     */
    private static int[] expected(String name) throws Exception {
        byte[] d = obu(name);
        return AvifColor.toArgb(Av1Decoder.decode(d, 0, d.length, DecodeLimits.DEFAULT, null), null, false);
    }

    /**
     * Files written by libavif decode to the pixels that libavif decodes (the colour conversion, alpha and all chroma formats).
     * <p>
     * libavif 가 쓴 파일이 libavif 가 디코딩하는 픽셀과 같게 디코딩된다(색 변환, 알파, 모든 색차 형식).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testLibavifFilesMatchLibavif() throws Exception {
        List<String> lines = AvifTestKit.indexLines("/media/avif/avif/index.txt");
        Check.isTrue(lines.size() >= 7, "the fixture index lists the files");
        // 색인의 파일마다 디코딩하여 libavif 의 결과(PNG)와 비교한다.
        for (String line : lines) {
            String[] f = line.split("\\|");
            DecodedImage img = decode(AvifTestKit.resource("/media/avif/avif/" + f[0] + ".avif"));
            BufferedImage ref = ImageIO.read(new ByteArrayInputStream(AvifTestKit.resource("/media/avif/avif/" + f[0] + ".png")));
            Check.equal(Integer.parseInt(f[1]), img.getWidth(), f[0] + ": width");
            Check.equal(Integer.parseInt(f[2]), img.getHeight(), f[0] + ": height");
            Check.equal(ref.getWidth(), img.getWidth(), f[0] + ": reference width");
            int[] px = argb(img);
            long sum = 0;
            int max = 0;
            int big = 0;
            // 모든 채널의 차이를 합산한다.
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    int a = px[y * img.getWidth() + x];
                    int b = ref.getRGB(x, y);
                    // 네 채널(알파, 빨강, 초록, 파랑)의 차이를 모두 센다.
                    for (int sh = 0; sh < 32; sh += 8) {
                        int d = Math.abs(((a >>> sh) & 255) - ((b >>> sh) & 255));
                        sum += d;
                        max = Math.max(max, d);
                        if (d > 2) {
                            big++;
                        }
                    }
                }
            }
            double mean = sum / (4.0 * img.getWidth() * img.getHeight());
            Check.isTrue(mean < 0.7, f[0] + ": mean difference to libavif " + mean + " (" + f[3] + ")");
            Check.isTrue(max <= 6, f[0] + ": largest difference to libavif " + max);
            Check.isTrue(big < 4.0 * img.getWidth() * img.getHeight() * 0.003, f[0] + ": pixels that differ by more than 2: " + big);
        }
    }

    /**
     * The registry finds the decoder by content, ignores a wrong MIME hint and lists the MIME type.
     * <p>
     * 등록부가 내용으로 디코더를 찾고, 잘못된 MIME 힌트를 무시하며, MIME 타입을 목록에 올린다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testRegistry() throws Exception {
        byte[] file = AvifTestKit.resource("/media/avif/avif/pil_420.avif");
        DecodedImage a = ImageDecoders.decode(file, "image/avif", null);
        DecodedImage b = ImageDecoders.decode(file, "image/png", null);
        DecodedImage c = ImageDecoders.decode(file, null, null);
        Check.equal(64, a.getWidth(), "width");
        Check.equal(48, b.getHeight(), "a wrong MIME hint does not decide the format");
        Check.equal(argb(a)[100], argb(c)[100], "no hint");
        Check.isTrue(ImageDecoders.isSupportedMime("image/avif"), "supported MIME");
        Check.isTrue(ImageDecoders.isSupportedMime("IMAGE/AVIF; q=1"), "MIME with parameters and letter case");
        Check.contains(ImageDecoders.getAcceptHeader(), "image/avif", "Accept header lists AVIF");
        Check.isTrue(ImageDecoders.find(file) instanceof AvifDecoder, "find by the first bytes");
        Check.equal("avif", new AvifDecoder().getFormatName(), "format name");
        Check.isFalse(a.isAnimated(), "a still image");
    }

    /**
     * The signature check looks at the brands of the file type box.
     * <p>
     * 시그니처 검사는 파일 종류 상자의 브랜드를 본다.
     */
    public void testAccepts() {
        AvifDecoder d = new AvifDecoder();
        byte[] ok = AvifTestKit.cat(AvifTestKit.be(24, 4), AvifTestKit.ascii("ftypavif"), AvifTestKit.be(0, 4), AvifTestKit.ascii("mif1avif"));
        Check.isTrue(d.accepts(ok), "major brand avif");
        byte[] compat = AvifTestKit.cat(AvifTestKit.be(24, 4), AvifTestKit.ascii("ftypmif1"), AvifTestKit.be(0, 4), AvifTestKit.ascii("miafavis"));
        Check.isTrue(d.accepts(compat), "compatible brand avis");
        byte[] heic = AvifTestKit.cat(AvifTestKit.be(24, 4), AvifTestKit.ascii("ftypheic"), AvifTestKit.be(0, 4), AvifTestKit.ascii("mif1heix"));
        Check.isFalse(d.accepts(heic), "a HEIC file");
        Check.isFalse(d.accepts(new byte[0]), "no data");
        Check.isFalse(d.accepts(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 }), "random data");
        Check.isFalse(d.accepts(null), "null");
        // 브랜드가 상자 끝(크기) 뒤에 있으면 무시한다.
        byte[] outside = AvifTestKit.cat(AvifTestKit.be(16, 4), AvifTestKit.ascii("ftypmif1"), AvifTestKit.be(0, 4), AvifTestKit.ascii("avif"));
        Check.isFalse(d.accepts(outside), "a brand after the end of the box");
    }

    /**
     * A file around an AV1 payload decodes like the payload itself, also with the item data box and with items split into extents.
     * <p>
     * AV1 페이로드를 감싼 파일은 페이로드 자체와 같게 디코딩되며, 항목 데이터 상자와 범위로 나뉜 항목에서도 같다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testItemLocations() throws Exception {
        byte[] payload = obu(MAIN);
        int[] want = expected(MAIN);
        Check.equal(64, decode(simple(payload, 64, 48).build()).getWidth(), "plain");
        Check.isTrue(java.util.Arrays.equals(want, argb(decode(simple(payload, 64, 48).build()))), "plain pixels");
        AvifTestKit.Builder idat = simple(payload, 64, 48);
        idat.useIdat = true;
        Check.isTrue(java.util.Arrays.equals(want, argb(decode(idat.build()))), "item data box (construction method 1)");
        AvifTestKit.Builder ext = simple(payload, 64, 48);
        ext.splitExtents = true;
        Check.isTrue(java.util.Arrays.equals(want, argb(decode(ext.build()))), "two extents");
        AvifTestKit.Builder both = simple(payload, 64, 48);
        both.useIdat = true;
        both.splitExtents = true;
        Check.isTrue(java.util.Arrays.equals(want, argb(decode(both.build()))), "item data box with two extents");
    }

    /**
     * The rotation property turns the image anti-clockwise by multiples of 90 degrees.
     * <p>
     * 회전 속성은 이미지를 반시계 방향으로 90도의 배수만큼 돌린다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testRotation() throws Exception {
        byte[] payload = obu(MAIN);
        int[] want = expected(MAIN);
        int w = 64;
        int h = 48;
        // 회전 횟수마다 파일을 만들어 네 모서리와 임의 위치의 픽셀을 비교한다.
        for (int angle = 1; angle <= 3; angle++) {
            AvifTestKit.Builder b = simple(payload, w, h);
            b.assoc(1, b.prop(AvifTestKit.irot(angle)), true);
            DecodedImage img = decode(b.build());
            int nw = angle == 2 ? w : h;
            int nh = angle == 2 ? h : w;
            Check.equal(nw, img.getWidth(), "rotated width " + angle);
            Check.equal(nh, img.getHeight(), "rotated height " + angle);
            int[] got = argb(img);
            Random r = new Random(angle);
            // 임의로 고른 원본 위치마다 회전 후 위치의 픽셀을 비교한다.
            for (int i = 0; i < 40; i++) {
                int x = r.nextInt(w);
                int y = r.nextInt(h);
                int nx;
                int ny;
                // 반시계 방향 회전에서 원본 (x, y) 가 가는 위치.
                if (angle == 1) {
                    nx = y;
                    ny = w - 1 - x;
                } else if (angle == 2) {
                    nx = w - 1 - x;
                    ny = h - 1 - y;
                } else {
                    nx = h - 1 - y;
                    ny = x;
                }
                Check.equal(want[y * w + x] & 0xFFFFFFFFL, got[ny * nw + nx] & 0xFFFFFFFFL, "rotation " + angle + " at " + x + "," + y);
            }
        }
        AvifTestKit.Builder zero = simple(payload, w, h);
        zero.assoc(1, zero.prop(AvifTestKit.irot(0)), true);
        Check.isTrue(java.util.Arrays.equals(want, argb(decode(zero.build()))), "angle 0 changes nothing");
    }

    /**
     * The mirror property swaps rows (axis 0) or columns (axis 1).
     * <p>
     * 거울 속성은 행(축 0) 또는 열(축 1)을 맞바꾼다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testMirror() throws Exception {
        byte[] payload = obu(MAIN);
        int[] want = expected(MAIN);
        int w = 64;
        int h = 48;
        // 축마다 파일을 만들어 모든 픽셀의 위치를 확인한다.
        for (int axis = 0; axis <= 1; axis++) {
            AvifTestKit.Builder b = simple(payload, w, h);
            b.assoc(1, b.prop(AvifTestKit.imir(axis)), true);
            DecodedImage img = decode(b.build());
            Check.equal(w, img.getWidth(), "mirrored width");
            int[] got = argb(img);
            // 모든 행을 확인한다.
            for (int y = 0; y < h; y++) {
                // 모든 열을 확인한다.
                for (int x = 0; x < w; x++) {
                    int src = axis == 0 ? (h - 1 - y) * w + x : y * w + (w - 1 - x);
                    if (want[src] != got[y * w + x]) {
                        Check.fail("mirror axis " + axis + " at " + x + "," + y);
                    }
                }
            }
        }
    }

    /**
     * The clean aperture property crops around the centre; the transformative properties apply in the order of the file.
     * <p>
     * clean aperture 속성은 중심 주위를 자르며, 변환 속성은 파일에 쓰인 순서대로 적용된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testCleanApertureAndOrder() throws Exception {
        byte[] payload = obu(MAIN);
        int[] want = expected(MAIN);
        int w = 64;
        AvifTestKit.Builder b = simple(payload, 64, 48);
        b.assoc(1, b.prop(AvifTestKit.clap(40, 30, 0, 0)), true);
        DecodedImage img = decode(b.build());
        Check.equal(40, img.getWidth(), "cropped width");
        Check.equal(30, img.getHeight(), "cropped height");
        Check.equal(want[(9 + 5) * w + 12 + 7] & 0xFFFFFFFFL, argb(img)[5 * 40 + 7] & 0xFFFFFFFFL, "the crop starts at (12, 9)");
        AvifTestKit.Builder off = simple(payload, 64, 48);
        off.assoc(1, off.prop(AvifTestKit.clap(40, 30, 4, -3)), true);
        DecodedImage shifted = decode(off.build());
        Check.equal(want[(6 + 5) * w + 16 + 7] & 0xFFFFFFFFL, argb(shifted)[5 * 40 + 7] & 0xFFFFFFFFL, "the offset moves the crop to (16, 6)");
        AvifTestKit.Builder order = simple(payload, 64, 48);
        order.assoc(1, order.prop(AvifTestKit.clap(40, 30, 0, 0)), true);
        order.assoc(1, order.prop(AvifTestKit.irot(1)), true);
        DecodedImage both = decode(order.build());
        Check.equal(30, both.getWidth(), "crop then rotate: width");
        Check.equal(40, both.getHeight(), "crop then rotate: height");
        final AvifTestKit.Builder bad = simple(payload, 64, 48);
        bad.assoc(1, bad.prop(AvifTestKit.clap(80, 30, 0, 0)), true);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an aperture outside the image", new Block() {
            public void run() throws Exception {
                decode(bad.build());
            }
        });
    }

    /**
     * Builds a grid file of four different tiles.
     * <p>
     * 서로 다른 타일 네 개의 격자 파일을 만든다.
     *
     * @param tiles  the tile streams / 타일 스트림
     * @param outW   the output width / 출력 너비
     * @param outH   the output height / 출력 높이
     * @param tileId the number of the tile items to reference / 참조할 타일 항목 수
     * @return the builder / 작성기
     * @throws Exception on a failure / 실패 시
     */
    private static AvifTestKit.Builder grid(String[] tiles, int outW, int outH, int tileId) throws Exception {
        AvifTestKit.Builder b = new AvifTestKit.Builder();
        b.primary = 10;
        int[] ids = new int[tiles.length];
        int ispe = b.prop(AvifTestKit.ispe(96, 64));
        int cfg = b.prop(AvifTestKit.av1C());
        // 타일마다 항목을 만들고 속성을 연결한다.
        for (int i = 0; i < tiles.length; i++) {
            ids[i] = i + 1;
            b.item(ids[i], "av01", obu(tiles[i]));
            b.assoc(ids[i], ispe, false).assoc(ids[i], cfg, true);
        }
        byte[] desc = AvifTestKit.cat(new byte[] { 0, 0, 1, 1 }, AvifTestKit.be(outW, 2), AvifTestKit.be(outH, 2));
        b.item(10, "grid", desc);
        b.assoc(10, b.prop(AvifTestKit.ispe(outW, outH)), false);
        b.ref("dimg", 10, java.util.Arrays.copyOf(ids, tileId));
        return b;
    }

    /**
     * A <code>colr</code> property of the type nclx takes precedence over the colour description of the AV1 sequence header (matrix
     * coefficients and range); other colour properties are ignored.
     * <p>
     * nclx 형식의 <code>colr</code> 속성이 AV1 시퀀스 헤더의 색 설명(행렬 계수와 범위)보다 우선하며, 다른 색 속성은 무시된다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testColrOverridesTheSequenceHeader() throws Exception {
        byte[] payload = obu(MAIN);
        Av1Picture pic = Av1Decoder.decode(payload, 0, payload.length, DecodeLimits.DEFAULT, null);
        int[] plain = AvifColor.toArgb(pic, null, false);
        pic.matrixCoefficients = 1;
        pic.fullRange = true;
        int[] want = AvifColor.toArgb(pic, null, false);
        Check.isFalse(java.util.Arrays.equals(plain, want), "the test needs a stream whose colours depend on the matrix and the range");
        AvifTestKit.Builder b = simple(payload, 64, 48);
        byte[] nclx = AvifTestKit.box("colr", AvifTestKit.ascii("nclx"), AvifTestKit.be(1, 2), AvifTestKit.be(13, 2), AvifTestKit.be(1, 2),
                new byte[] { (byte) 0x80 });
        b.assoc(1, b.prop(nclx), false);
        Check.isTrue(java.util.Arrays.equals(want, argb(decode(b.build()))), "nclx: BT.709 matrix with full range");
        AvifTestKit.Builder icc = simple(payload, 64, 48);
        icc.assoc(1, icc.prop(AvifTestKit.box("colr", AvifTestKit.ascii("prof"), new byte[16])), false);
        Check.isTrue(java.util.Arrays.equals(plain, argb(decode(icc.build()))), "a profile property is ignored");
    }

    /**
     * A grid joins its tiles in the YUV domain, crops to the output size and checks the tile count.
     * <p>
     * 격자는 타일을 YUV 영역에서 이어 붙이고, 출력 크기로 자르며, 타일 수를 검사한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testGrid() throws Exception {
        String[] tiles = { "rav1e_420", "svt_420", "aom_qm", "aom_reduced_tx" };
        DecodedImage img = decode(grid(tiles, 192, 128, 4).build());
        Check.equal(192, img.getWidth(), "grid width");
        Check.equal(128, img.getHeight(), "grid height");
        int[] got = argb(img);
        // 타일마다 가장자리에서 3픽셀 떨어진 안쪽 픽셀이 단독 디코딩 결과와 같은지 확인한다.
        for (int t = 0; t < 4; t++) {
            int[] want = expected(tiles[t]);
            int ox = (t % 2) * 96;
            int oy = (t / 2) * 64;
            // 타일 안쪽의 행을 확인한다.
            for (int y = 3; y < 61; y++) {
                // 타일 안쪽의 열을 확인한다.
                for (int x = 3; x < 93; x++) {
                    if (want[y * 96 + x] != got[(oy + y) * 192 + ox + x]) {
                        Check.fail("grid tile " + t + " at " + x + "," + y);
                    }
                }
            }
        }
        DecodedImage cropped = decode(grid(tiles, 150, 100, 4).build());
        Check.equal(150, cropped.getWidth(), "cropped grid width");
        Check.equal(100, cropped.getHeight(), "cropped grid height");
        final AvifTestKit.Builder few = grid(tiles, 192, 128, 3);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "a grid with too few tiles", new Block() {
            public void run() throws Exception {
                decode(few.build());
            }
        });
        final AvifTestKit.Builder mixed = grid(new String[] { "rav1e_420", "svt_420", "aom_444_q30", "aom_reduced_tx" }, 192, 128, 4);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "tiles with different chroma formats", new Block() {
            public void run() throws Exception {
                decode(mixed.build());
            }
        });
        final AvifTestKit.Builder tooBig = grid(tiles, 400, 128, 4);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an output size that the tiles do not cover", new Block() {
            public void run() throws Exception {
                decode(tooBig.build());
            }
        });
    }

    /**
     * The alpha value of a sample of the monochrome alpha stream.
     * <p>
     * 단색 알파 스트림 샘플의 알파 값.
     *
     * @param v         the sample / 샘플
     * @param fullRange true for the full range / 전체 범위면 true
     * @return the value 0..255 / 값 0~255
     */
    private static int alphaOf(int v, boolean fullRange) {
        double a = fullRange ? v / 255.0 : (v - 16) / 219.0;
        int i = (int) Math.floor(a * 255.0 + 0.5);
        return i < 0 ? 0 : (i > 255 ? 255 : i);
    }

    /**
     * An auxiliary alpha item gives the alpha channel; a premultiplied reference makes the decoder divide the colours by the alpha.
     * <p>
     * 보조 알파 항목이 알파 채널을 주며, 미리 곱한 참조가 있으면 디코더가 색을 알파로 나눈다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testAlpha() throws Exception {
        byte[] color = obu("rav1e_420");
        byte[] alpha = obu("aom_mono_q30");
        AvifTestKit.Builder b = simple(color, 96, 64);
        b.item(2, "av01", alpha);
        int ispe = b.prop(AvifTestKit.ispe(96, 64));
        int aux = b.prop(AvifTestKit.auxAlpha());
        b.assoc(2, ispe, false).assoc(2, aux, true);
        b.ref("auxl", 2, 1);
        DecodedImage img = decode(b.build());
        int[] opaque = expected("rav1e_420");
        Av1Picture ap = Av1Decoder.decode(alpha, 0, alpha.length, DecodeLimits.DEFAULT, null);
        int[] got = argb(img);
        boolean someTransparent = false;
        // 모든 픽셀에서 색은 불투명 변환과 같고 알파는 단색 평면에서 나와야 한다.
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 96; x++) {
                int want = alphaOf(ap.planes[0][y * ap.stride[0] + x], ap.fullRange);
                int a = got[y * 96 + x] >>> 24;
                Check.equal(want, a, "alpha at " + x + "," + y);
                Check.equal(opaque[y * 96 + x] & 0xFFFFFF, got[y * 96 + x] & 0xFFFFFF, "colour at " + x + "," + y);
                if (a < 255) {
                    someTransparent = true;
                }
            }
        }
        Check.isTrue(someTransparent, "the alpha stream has transparent pixels");
        AvifTestKit.Builder pre = simple(color, 96, 64);
        pre.item(2, "av01", alpha);
        pre.assoc(2, pre.prop(AvifTestKit.ispe(96, 64)), false).assoc(2, pre.prop(AvifTestKit.auxAlpha()), true);
        pre.ref("auxl", 2, 1).ref("prem", 1, 2);
        int[] p = argb(decode(pre.build()));
        int checked = 0;
        // 반투명 픽셀은 미리 곱한 색을 알파로 나눈 값이어야 한다.
        for (int i = 0; i < p.length; i++) {
            int a = p[i] >>> 24;
            if (a > 0 && a < 255) {
                int r = (opaque[i] >> 16) & 255;
                Check.equal(Math.min(255, (r * 255 + a / 2) / a), (p[i] >> 16) & 255, "premultiplied red at " + i);
                checked++;
            }
        }
        Check.isTrue(checked > 0, "premultiplied pixels were checked");
        AvifTestKit.Builder wrongSize = simple(color, 96, 64);
        wrongSize.item(2, "av01", obu(MAIN));
        wrongSize.assoc(2, wrongSize.prop(AvifTestKit.ispe(64, 48)), false).assoc(2, wrongSize.prop(AvifTestKit.auxAlpha()), true);
        wrongSize.ref("auxl", 2, 1);
        final byte[] file = wrongSize.build();
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an alpha image of another size", new Block() {
            public void run() throws Exception {
                decode(file);
            }
        });
    }

    /**
     * Files that are not usable fail with the documented kinds.
     * <p>
     * 쓸 수 없는 파일은 문서화된 종류로 실패한다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testStructureErrors() throws Exception {
        final byte[] payload = obu(MAIN);
        AvifTestKit.Builder noPrimary = simple(payload, 64, 48);
        noPrimary.primary = -1;
        final byte[] f1 = noPrimary.build();
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "no primary item", new Block() {
            public void run() throws Exception {
                decode(f1);
            }
        });
        AvifTestKit.Builder sequence = simple(payload, 64, 48);
        sequence.primary = -1;
        sequence.movie = true;
        sequence.brand = "avis";
        final byte[] f2 = sequence.build();
        AvifTestKit.fails(MediaException.Kind.UNSUPPORTED_FORMAT, "an image sequence", new Block() {
            public void run() throws Exception {
                decode(f2);
            }
        });
        AvifTestKit.Builder overlay = new AvifTestKit.Builder();
        overlay.item(1, "iovl", new byte[] { 0, 0, 0 });
        final byte[] f3 = overlay.build();
        // 오버레이는 지원한다(AvifOverlayTest). 참조도 내용도 없는 오버레이는 손상이다.
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an overlay without a description that fits (supported, see AvifOverlayTest)", new Block() {
            public void run() throws Exception {
                decode(f3);
            }
        });
        AvifTestKit.Builder essential = simple(payload, 64, 48);
        essential.assoc(1, essential.prop(AvifTestKit.box("abcd", new byte[] { 1 })), true);
        final byte[] f4 = essential.build();
        AvifTestKit.fails(MediaException.Kind.UNSUPPORTED_FORMAT, "an unknown essential property", new Block() {
            public void run() throws Exception {
                decode(f4);
            }
        });
        AvifTestKit.Builder optional = simple(payload, 64, 48);
        optional.assoc(1, optional.prop(AvifTestKit.box("abcd", new byte[] { 1 })), false);
        Check.equal(64, decode(optional.build()).getWidth(), "an unknown non-essential property is ignored");
        AvifTestKit.Builder bigger = simple(payload, 80, 60);
        final byte[] f5 = bigger.build();
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "ispe larger than the AV1 frame", new Block() {
            public void run() throws Exception {
                decode(f5);
            }
        });
        AvifTestKit.Builder smaller = simple(payload, 40, 30);
        DecodedImage cropped = decode(smaller.build());
        Check.equal(40, cropped.getWidth(), "ispe smaller than the AV1 frame crops");
        Check.equal(30, cropped.getHeight(), "ispe smaller than the AV1 frame crops");
        final byte[] noFtyp = AvifTestKit.box("meta", new byte[8]);
        AvifTestKit.fails(MediaException.Kind.UNSUPPORTED_FORMAT, "no file type box", new Block() {
            public void run() throws Exception {
                decode(noFtyp);
            }
        });
        final byte[] wrongType = AvifTestKit.cat(AvifTestKit.box("ftyp", AvifTestKit.ascii("avif"), new byte[4]), AvifTestKit.box("meta", new byte[4]));
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an empty meta box", new Block() {
            public void run() throws Exception {
                decode(wrongType);
            }
        });
    }

    /**
     * The decoder respects the limits and the interruption of the thread.
     * <p>
     * 디코더가 제한과 스레드 인터럽트를 지킨다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testLimitsAndCancellation() throws Exception {
        final byte[] file = AvifTestKit.resource("/media/avif/avif/pil_420.avif");
        AvifTestKit.fails(MediaException.Kind.TOO_LARGE, "pixel limit", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(file, new DecodeLimits(100, DecodeLimits.DEFAULT_MAX_BYTES, 10, 100));
            }
        });
        AvifTestKit.fails(MediaException.Kind.TOO_LARGE, "byte limit", new Block() {
            public void run() throws Exception {
                new AvifDecoder().decode(file, new DecodeLimits(DecodeLimits.DEFAULT_MAX_PIXELS, 100, 10, DecodeLimits.DEFAULT_MAX_TOTAL_PIXELS));
            }
        });
        Thread.currentThread().interrupt();
        try {
            AvifTestKit.fails(MediaException.Kind.CANCELLED, "interrupted thread", new Block() {
                public void run() throws Exception {
                    decode(file);
                }
            });
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * Cut and damaged files fail with a {@link MediaException} or decode to some picture; nothing else is thrown by the registry.
     * <p>
     * 잘리거나 손상된 파일은 {@link MediaException} 으로 실패하거나 어떤 화면으로 디코딩된다. 등록부는 그 밖의 것을 던지지 않는다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDamagedFiles() throws Exception {
        byte[] orig = AvifTestKit.resource("/media/avif/avif/pil_420.avif");
        // 잘라낸 길이마다 등록부로 디코딩을 시도한다.
        for (int len = 1; len < orig.length; len += 13) {
            byte[] cut = new byte[len];
            System.arraycopy(orig, 0, cut, 0, len);
            try {
                ImageDecoders.decode(cut, "image/avif", null);
            } catch (MediaException e) {
                Check.notNull(e.getKind(), "a kind");
            }
        }
        Random rnd = new Random(777);
        // 바이트를 임의로 바꾼 파일을 여러 번 디코딩한다.
        for (int round = 0; round < 150; round++) {
            byte[] d = orig.clone();
            // 임의 위치의 비트를 뒤집는다.
            for (int k = 0; k < 1 + rnd.nextInt(3); k++) {
                d[rnd.nextInt(d.length)] ^= (byte) (1 << rnd.nextInt(8));
            }
            try {
                ImageDecoders.decode(d, "image/avif", null);
            } catch (MediaException e) {
                Check.notNull(e.getKind(), "a kind");
            }
        }
    }
}
