package org.duckdns.hjow.swingwebpanel.media.image.avif;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The overlay image of an AVIF file (<code>iovl</code>, ISO/IEC 23008-12 6.6.2.3; plan item P18-03): a canvas that is filled with a colour
 * and receives the referenced images at offsets, in the order of the references. The layers are real AV1 streams (the fixtures of the
 * decoder tests) wrapped by the test HEIF writer, and the expected pixels are the pixels that the same streams give when they are decoded
 * on their own, so no reference decoder is needed. The tests cover the placement and the order of the layers, the fill colour and its
 * alpha, offsets that are negative or reach outside of the canvas, the 32-bit field size, the properties of the overlay item (rotation),
 * and the damaged descriptions.
 * <p>
 * AVIF 파일의 오버레이 이미지(<code>iovl</code>, ISO/IEC 23008-12 6.6.2.3; 계획 P18-03): 한 색으로 채운 캔버스에 참조한 이미지를 참조 순서대로 오프셋에
 * 놓는다. 계층은 시험용 HEIF 작성기로 감싼 실제 AV1 스트림(디코더 시험의 자료)이고, 기대하는 픽셀은 같은 스트림을 단독으로 디코딩했을 때의 픽셀이므로
 * 기준 디코더가 필요 없다. 계층의 위치와 순서, 채우기 색과 그 알파, 음수이거나 캔버스 밖에 닿는 오프셋, 32비트 필드 크기, 오버레이 항목의 속성(회전), 손상된
 * 설명을 시험한다.
 */
public class AvifOverlayTest {

    /**
     * The fill colour opaque blue as ARGB.
     * <p>
     * 채우기 색인 불투명 파랑(ARGB).
     */
    private static final int BLUE = 0xFF0000FF;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public AvifOverlayTest() {
    }

    /**
     * Reads the AV1 stream of a fixture.
     * <p>
     * 자료의 AV1 스트림을 읽는다.
     *
     * @param name the name / 이름
     * @return the stream / 스트림
     * @throws Exception on a failure / 실패 시
     */
    private static byte[] obu(String name) throws Exception {
        return AvifTestKit.resource("/media/avif/av1/" + name + ".obu");
    }

    /**
     * Decodes an AVIF file.
     * <p>
     * AVIF 파일을 디코딩한다.
     *
     * @param file the file / 파일
     * @return the image / 이미지
     * @throws Exception on a failure / 실패 시
     */
    private static DecodedImage decode(byte[] file) throws Exception {
        return new AvifDecoder().decode(file, DecodeLimits.DEFAULT);
    }

    /**
     * Returns the pixels that a stream gives when it is decoded on its own.
     * <p>
     * 스트림을 단독으로 디코딩했을 때의 픽셀을 반환한다.
     *
     * @param name the name of the fixture / 자료의 이름
     * @return the ARGB pixels / ARGB 픽셀
     * @throws Exception on a failure / 실패 시
     */
    private static int[] expected(String name) throws Exception {
        byte[] d = obu(name);
        return AvifColor.toArgb(Av1Decoder.decode(d, 0, d.length, DecodeLimits.DEFAULT, null), null, false);
    }

    /**
     * Builds a file whose primary item is an overlay of 96 x 64 tiles.
     * <p>
     * 기본 항목이 96 x 64 타일의 오버레이인 파일을 만든다.
     *
     * @param tiles   the names of the tile fixtures, bottom layer first / 타일 자료의 이름(아래 계층이 먼저)
     * @param outW    the canvas width / 캔버스 너비
     * @param outH    the canvas height / 캔버스 높이
     * @param fill    the fill colour as red, green, blue, alpha of 16 bits / 16비트 빨강, 초록, 파랑, 알파인 채우기 색
     * @param offsets the offsets of the layers as x, y pairs / 계층의 오프셋(x, y 쌍)
     * @param wide    true for 32-bit fields / 32비트 필드이면 true
     * @return the builder / 작성기
     * @throws Exception on a failure / 실패 시
     */
    private static AvifTestKit.Builder overlay(String[] tiles, int outW, int outH, int[] fill, int[][] offsets, boolean wide) throws Exception {
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
        int size = wide ? 4 : 2;
        byte[] desc = AvifTestKit.cat(new byte[] { 0, (byte) (wide ? 1 : 0) }, AvifTestKit.be(fill[0], 2), AvifTestKit.be(fill[1], 2),
                AvifTestKit.be(fill[2], 2), AvifTestKit.be(fill[3], 2), AvifTestKit.be(outW, size), AvifTestKit.be(outH, size));
        // 계층마다 오프셋(부호 있는 값, 음수는 2의 보수)을 덧붙인다.
        for (int[] o : offsets) {
            long mask = wide ? 0xFFFFFFFFL : 0xFFFFL;
            desc = AvifTestKit.cat(desc, AvifTestKit.be(o[0] & mask, size), AvifTestKit.be(o[1] & mask, size));
        }
        b.item(10, "iovl", desc);
        b.assoc(10, b.prop(AvifTestKit.ispe(outW, outH)), false);
        b.ref("dimg", 10, ids);
        return b;
    }

    /**
     * Two layers at offsets over a fill colour: pixels that no layer covers show the fill, the pixels of a layer are those of its stream, and
     * where two layers overlap the later one wins.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testLayersAreOrderedAndPlaced() throws Exception {
        String[] tiles = { "rav1e_420", "svt_420" };
        int[] fill = { 0, 0, 0xFFFF, 0xFFFF };
        DecodedImage img = decode(overlay(tiles, 144, 96, fill, new int[][] { { 0, 0 }, { 48, 32 } }, false).build());
        Check.equal(144, img.getWidth(), "the canvas width");
        Check.equal(96, img.getHeight(), "the canvas height");
        int[] got = img.getFrames().get(0).getArgb();
        int[] a = expected(tiles[0]);
        int[] b = expected(tiles[1]);
        Check.equal(BLUE, got[5 * 144 + 140], "a pixel that no layer covers shows the fill colour");
        Check.equal(BLUE, got[90 * 144 + 10], "another pixel below the first layer");
        Check.equal(a[10 * 96 + 10], got[10 * 144 + 10], "a pixel of the first layer alone");
        Check.equal(b[(80 - 32) * 96 + (100 - 48)], got[80 * 144 + 100], "a pixel of the second layer alone");
        Check.equal(b[(40 - 32) * 96 + (60 - 48)], got[40 * 144 + 60], "where the layers overlap, the later layer wins");
        Check.isTrue(a[40 * 96 + 60] != b[8 * 96 + 12], "the test needs different pixels in the two layers");
    }

    /**
     * A layer with negative offsets is cut off at the top left of the canvas; a layer that reaches beyond the right and bottom edge is cut
     * off there; a layer that lies completely outside of the canvas changes nothing.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testNegativeAndOutsideOffsets() throws Exception {
        String[] tiles = { "rav1e_420", "svt_420", "aom_qm" };
        int[] fill = { 0, 0, 0xFFFF, 0xFFFF };
        DecodedImage img = decode(overlay(tiles, 100, 60, fill, new int[][] { { -20, -10 }, { 70, 40 }, { 500, 500 } }, false).build());
        int[] got = img.getFrames().get(0).getArgb();
        int[] a = expected(tiles[0]);
        int[] b = expected(tiles[1]);
        Check.equal(a[10 * 96 + 20], got[0], "the pixel at the corner is the pixel (20, 10) of the first layer");
        Check.equal(a[(30 + 10) * 96 + (50 + 20)], got[30 * 100 + 50], "a pixel inside of the cut layer");
        Check.equal(b[(55 - 40) * 96 + (95 - 70)], got[55 * 100 + 95], "a pixel of the layer that reaches beyond the edge");
        Check.equal(BLUE, got[59 * 100 + 0], "a pixel that no layer covers");
    }

    /**
     * The fill colour keeps its alpha (the canvas is partly transparent where nothing covers it), the fields may be 32 bits wide, and an
     * overlay of translucent layers is not tested here (the layers of these fixtures are opaque).
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testFillAlphaAndWideFields() throws Exception {
        String[] tiles = { "rav1e_420" };
        int[] fill = { 0xFFFF, 0, 0, 0x8000 };
        DecodedImage img = decode(overlay(tiles, 120, 64, fill, new int[][] { { 0, 0 } }, true).build());
        int[] got = img.getFrames().get(0).getArgb();
        Check.equal(0x80FF0000, got[10 * 120 + 110], "the fill colour is half transparent red");
        Check.equal(expected(tiles[0])[20 * 96 + 20], got[20 * 120 + 20], "the layer is opaque over it");
    }

    /**
     * The transformative properties of the overlay item apply to the composed canvas: a rotation by 90 degrees swaps its size.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testPropertiesOfTheOverlayItem() throws Exception {
        String[] tiles = { "rav1e_420" };
        int[] fill = { 0, 0, 0xFFFF, 0xFFFF };
        AvifTestKit.Builder b = overlay(tiles, 120, 80, fill, new int[][] { { 0, 0 } }, false);
        b.assoc(10, b.prop(AvifTestKit.irot(1)), false);
        DecodedImage img = decode(b.build());
        Check.equal(80, img.getWidth(), "the rotated width is the canvas height");
        Check.equal(120, img.getHeight(), "the rotated height is the canvas width");
        int[] got = img.getFrames().get(0).getArgb();
        // 반시계 방향으로 90도 돌린 그림에서 캔버스의 오른쪽 위 모서리(채우기 색)는 왼쪽 위 모서리에 온다.
        Check.equal(BLUE, got[0], "the top right corner of the canvas (fill colour) is now at the top left");
    }

    /**
     * Builds the description of an overlay item with a black opaque canvas.
     * <p>
     * 불투명 검정 캔버스인 오버레이 항목의 설명을 만든다.
     *
     * @param w       the canvas width / 캔버스 너비
     * @param h       the canvas height / 캔버스 높이
     * @param offsets the offsets of the layers as x, y pairs / 계층의 오프셋(x, y 쌍)
     * @return the description / 설명
     */
    private static byte[] blackOverlay(int w, int h, int[][] offsets) {
        byte[] desc = AvifTestKit.cat(new byte[] { 0, 0 }, AvifTestKit.be(0, 2), AvifTestKit.be(0, 2), AvifTestKit.be(0, 2), AvifTestKit.be(0xFFFF, 2),
                AvifTestKit.be(w, 2), AvifTestKit.be(h, 2));
        // 계층마다 오프셋(부호 있는 16비트)을 덧붙인다.
        for (int[] o : offsets) {
            desc = AvifTestKit.cat(desc, AvifTestKit.be(o[0] & 0xFFFFL, 2), AvifTestKit.be(o[1] & 0xFFFFL, 2));
        }
        return desc;
    }

    /**
     * Builds a file with a chain of overlays: the primary item (item 10) is an overlay whose only layer is the overlay 11, and so on; the last
     * overlay has the stream of a fixture as its layer. Every overlay places its layer at the offset (5, 5) on a black canvas that is 10 pixels
     * narrower and 6 pixels lower than the one outside of it.
     * <p>
     * 오버레이의 사슬로 파일을 만든다: 기본 항목(항목 10)은 계층이 오버레이 11 하나뿐인 오버레이이고 그런 식으로 이어지며, 마지막 오버레이의 계층이 자료
     * 스트림이다. 오버레이마다 바깥보다 10 픽셀 좁고 6 픽셀 낮은 검정 캔버스의 오프셋 (5, 5) 에 계층을 놓는다.
     *
     * @param levels the number of overlays / 오버레이 수
     * @param tile   the name of the fixture / 자료 이름
     * @return the file / 파일
     * @throws Exception on a failure / 실패 시
     */
    private static byte[] chain(int levels, String tile) throws Exception {
        AvifTestKit.Builder b = new AvifTestKit.Builder();
        b.primary = 10;
        int ispe = b.prop(AvifTestKit.ispe(96, 64));
        int cfg = b.prop(AvifTestKit.av1C());
        b.item(1, "av01", obu(tile));
        b.assoc(1, ispe, false).assoc(1, cfg, true);
        // 바깥 오버레이부터 안쪽으로 항목을 만들고 각 오버레이가 다음 항목(마지막은 타일)을 계층으로 삼게 한다.
        for (int k = 0; k < levels; k++) {
            b.item(10 + k, "iovl", blackOverlay(100 - 10 * k, 60 - 6 * k, new int[][] { { 5, 5 } }));
            b.ref("dimg", 10 + k, new int[] { k == levels - 1 ? 1 : 10 + k + 1 });
        }
        return b.build();
    }

    /**
     * An overlay may have an overlay as a layer: the inner overlay is rendered at its own canvas size and placed like any other layer, and
     * the nesting may go three levels down (a fourth level is refused). The expected pixels are those of the streams decoded on their own.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testNestedOverlays() throws Exception {
        String[] tiles = { "rav1e_420", "svt_420" };
        AvifTestKit.Builder nested = new AvifTestKit.Builder();
        nested.primary = 10;
        int ispe = nested.prop(AvifTestKit.ispe(96, 64));
        int cfg = nested.prop(AvifTestKit.av1C());
        nested.item(1, "av01", obu(tiles[0]));
        nested.assoc(1, ispe, false).assoc(1, cfg, true);
        nested.item(2, "av01", obu(tiles[1]));
        nested.assoc(2, ispe, false).assoc(2, cfg, true);
        // 안쪽 오버레이(항목 11): 50 x 30 의 검정 캔버스 왼쪽 위에 둘째 타일. 바깥(항목 10): 100 x 60 검정 캔버스에 첫 타일과 안쪽 오버레이.
        nested.item(11, "iovl", blackOverlay(50, 30, new int[][] { { 0, 0 } }));
        nested.ref("dimg", 11, new int[] { 2 });
        nested.item(10, "iovl", blackOverlay(100, 60, new int[][] { { 0, 0 }, { 30, 20 } }));
        nested.ref("dimg", 10, new int[] { 1, 11 });
        DecodedImage img = decode(nested.build());
        Check.equal(100, img.getWidth(), "the outer canvas width");
        int[] got = img.getFrames().get(0).getArgb();
        int[] a = expected(tiles[0]);
        int[] b = expected(tiles[1]);
        Check.equal(a[10 * 96 + 10], got[10 * 100 + 10], "a pixel of the first layer");
        Check.equal(b[5 * 96 + 5], got[(20 + 5) * 100 + (30 + 5)], "a pixel of the inner overlay shows the second tile");
        Check.equal(b[29 * 96 + 49], got[(20 + 29) * 100 + (30 + 49)], "the last pixel of the inner canvas");
        Check.equal(a[50 * 96 + 80], got[50 * 100 + 80], "outside of the inner canvas the first layer shows");
        // 오버레이 네 개의 사슬(기본 항목 아래 세 단계)은 그려지고 가장 안쪽 타일이 가장 바깥 캔버스에서 5 픽셀씩 네 번 옮겨진 곳에 보인다.
        int[] d = decode(chain(4, tiles[0])).getFrames().get(0).getArgb();
        Check.equal(a[10 * 96 + 10], d[(20 + 10) * 100 + (20 + 10)], "the pixel of the innermost layer after four offsets of 5");
        // 오버레이 다섯 개의 사슬(기본 항목 아래 네 단계)은 거부된다.
        final byte[] tooDeep = chain(5, tiles[0]);
        AvifTestKit.fails(MediaException.Kind.UNSUPPORTED_FORMAT, "five levels of overlays", new Block() {
            public void run() throws Exception {
                decode(tooDeep);
            }
        });
    }

    /**
     * Damaged overlays fail with CORRUPT: a version that is not 0, a description that does not match the number of the references, an
     * overlay without references, a layer that is missing; and an overlay that is too large for the limits is TOO_LARGE.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDamagedOverlays() throws Exception {
        final String[] tiles = { "rav1e_420", "svt_420" };
        final int[] fill = { 0, 0, 0xFFFF, 0xFFFF };
        final AvifTestKit.Builder missingOffset = overlay(tiles, 100, 60, fill, new int[][] { { 0, 0 } }, false);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an offset is missing", new Block() {
            public void run() throws Exception {
                decode(missingOffset.build());
            }
        });
        final AvifTestKit.Builder tooMany = overlay(new String[] { "rav1e_420" }, 100, 60, fill, new int[][] { { 0, 0 }, { 1, 1 } }, false);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an offset too many", new Block() {
            public void run() throws Exception {
                decode(tooMany.build());
            }
        });
        final AvifTestKit.Builder none = overlay(new String[0], 100, 60, fill, new int[0][], false);
        AvifTestKit.fails(MediaException.Kind.CORRUPT, "an overlay without layers", new Block() {
            public void run() throws Exception {
                decode(none.build());
            }
        });
        // 오버레이가 자기 자신을 계층으로 가지면(참조 순환) 중첩 깊이 한도에서 끝난다.
        final AvifTestKit.Builder cycle = new AvifTestKit.Builder();
        cycle.primary = 10;
        cycle.item(10, "iovl", AvifTestKit.cat(new byte[] { 0, 0 }, AvifTestKit.be(0, 2), AvifTestKit.be(0, 2), AvifTestKit.be(0, 2),
                AvifTestKit.be(0xFFFF, 2), AvifTestKit.be(10, 2), AvifTestKit.be(10, 2), AvifTestKit.be(0, 2), AvifTestKit.be(0, 2)));
        cycle.ref("dimg", 10, new int[] { 10 });
        AvifTestKit.fails(MediaException.Kind.UNSUPPORTED_FORMAT, "an overlay that is its own layer", new Block() {
            public void run() throws Exception {
                decode(cycle.build());
            }
        });
        final AvifTestKit.Builder huge = overlay(tiles, 60000, 60000, fill, new int[][] { { 0, 0 }, { 0, 0 } }, false);
        AvifTestKit.fails(MediaException.Kind.TOO_LARGE, "a canvas over the pixel limit", new Block() {
            public void run() throws Exception {
                decode(huge.build());
            }
        });
    }
}
