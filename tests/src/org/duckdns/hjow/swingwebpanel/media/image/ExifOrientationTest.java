package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ExifOrientation}, of {@link DecodedImage#unoriented()} and of the <code>eXIf</code> chunk of PNG and APNG (plan item
 * P18-03; FEATURE_MATRIX IMG-01, IMG-02): the parser of the EXIF block in both byte orders and with the optional prefix, damaged blocks, the
 * eight orientations against their definition ({@link ExifBlocks#forward(int, int, int, int, int)}), the inverse that
 * <code>image-orientation: none</code> uses, and the PNG decoder with the chunk in front of and behind the image data. The WebP chunk is
 * tested in {@code org.duckdns.hjow.swingwebpanel.media.image.webp.WebpOrientationTest}, JPEG in {@link JpegDecoderTest}.
 * <p>
 * {@link ExifOrientation}, {@link DecodedImage#unoriented()}, PNG 와 APNG 의 <code>eXIf</code> 청크 시험(계획 P18-03; FEATURE_MATRIX IMG-01, IMG-02):
 * 두 바이트 순서와 선택적 머리말이 있는 EXIF 블록 해석, 손상된 블록, 여덟 방향을 정의({@link ExifBlocks#forward(int, int, int, int, int)})와 대조,
 * <code>image-orientation: none</code> 이 쓰는 역변환, 이미지 데이터 앞뒤에 청크가 있는 PNG 디코더. WebP 청크는
 * {@code org.duckdns.hjow.swingwebpanel.media.image.webp.WebpOrientationTest}, JPEG 은 {@link JpegDecoderTest} 에서 시험한다.
 */
public class ExifOrientationTest {

    /**
     * Width of the test image.
     * <p>
     * 시험 이미지의 가로.
     */
    private static final int W = 3;

    /**
     * Height of the test image.
     * <p>
     * 시험 이미지의 세로.
     */
    private static final int H = 2;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ExifOrientationTest() {
    }

    /**
     * Returns the 3 x 2 test pixels: six different opaque colours, so that no orientation maps the image onto itself.
     * <p>
     * 3 x 2 시험 픽셀을 반환한다: 서로 다른 불투명 색 여섯 개이므로 어떤 방향도 이미지를 자기 자신으로 옮기지 않는다.
     *
     * @return pixels / 픽셀
     */
    private static int[] pixels() {
        return new int[] { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF, 0xFFFF00FF };
    }

    /**
     * Builds a PNG of the test pixels that carries an <code>eXIf</code> chunk right after IHDR.
     * <p>
     * IHDR 바로 뒤에 <code>eXIf</code> 청크가 있는 시험 픽셀의 PNG 를 만든다.
     *
     * @param exif the chunk data, or null for none / 청크 데이터. 없으면 null
     * @return the file / 파일
     */
    static byte[] png(byte[] exif) {
        PngBuilder.Spec s = new PngBuilder.Spec(W, H);
        s.colorType = 6;
        s.depth = 8;
        int[] p = pixels();
        s.samples = new int[W * H * 4];
        // 픽셀마다 R, G, B, A 샘플로 푼다.
        for (int i = 0; i < p.length; i++) {
            s.samples[i * 4] = (p[i] >> 16) & 0xFF;
            s.samples[i * 4 + 1] = (p[i] >> 8) & 0xFF;
            s.samples[i * 4 + 2] = p[i] & 0xFF;
            s.samples[i * 4 + 3] = (p[i] >>> 24) & 0xFF;
        }
        if (exif != null) {
            s.extraChunks.add(PngBuilder.chunk("eXIf", exif));
        }
        return PngBuilder.encode(s);
    }

    /**
     * Returns a copy of a PNG file with a chunk inserted in front of its last chunk (IEND), that is behind the image data.
     * <p>
     * PNG 파일의 마지막 청크(IEND) 앞, 즉 이미지 데이터 뒤에 청크를 끼워 넣은 복사본을 반환한다.
     *
     * @param file  the file / 파일
     * @param chunk a complete chunk / 완전한 청크
     * @return the new file / 새 파일
     */
    private static byte[] insertBeforeIend(byte[] file, byte[] chunk) {
        byte[] out = new byte[file.length + chunk.length];
        int at = file.length - 12;
        System.arraycopy(file, 0, out, 0, at);
        System.arraycopy(chunk, 0, out, at, chunk.length);
        System.arraycopy(file, at, out, at + chunk.length, 12);
        return out;
    }

    /**
     * Every orientation is read from the block in both byte orders, with and without the <code>Exif\0\0</code> prefix.
     * <p>
     * 모든 방향이 두 바이트 순서와 <code>Exif\0\0</code> 머리말의 유무에 상관없이 블록에서 읽힌다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReadBothByteOrdersAndPrefix() throws Exception {
        // 방향 1~8 과 모든 조합을 확인한다.
        for (int o = 1; o <= 8; o++) {
            for (int e = 0; e < 2; e++) {
                byte[] t = ExifBlocks.tiff(o, e == 1);
                Check.equal(o, ExifOrientation.read(t, 0, t.length), "orientation " + o + (e == 1 ? " MM" : " II"));
                byte[] p = ExifBlocks.prefixed(o, e == 1);
                Check.equal(o, ExifOrientation.read(p, 0, p.length), "orientation " + o + " with prefix");
                // 데이터 앞뒤에 다른 바이트가 있어도 위치와 길이만 맞으면 읽힌다.
                byte[] padded = new byte[t.length + 10];
                System.arraycopy(t, 0, padded, 4, t.length);
                Check.equal(o, ExifOrientation.read(padded, 4, t.length), "orientation " + o + " at an offset");
            }
        }
    }

    /**
     * A damaged block gives 1 (upright) and never an exception.
     * <p>
     * 손상된 블록은 예외 없이 1(정상)을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReadIgnoresDamagedBlocks() throws Exception {
        byte[] good = ExifBlocks.tiff(6, true);
        Check.equal(6, ExifOrientation.read(good, 0, good.length), "control");
        byte[] magic = good.clone();
        magic[3] = 43;
        Check.equal(1, ExifOrientation.read(magic, 0, magic.length), "the number 42 is wrong");
        byte[] order = good.clone();
        order[0] = 'X';
        Check.equal(1, ExifOrientation.read(order, 0, order.length), "unknown byte order");
        byte[] offset = good.clone();
        offset[4] = 0x7F;
        Check.equal(1, ExifOrientation.read(offset, 0, offset.length), "directory offset outside the block");
        byte[] type = good.clone();
        type[12] = 0;
        type[13] = 4;
        Check.equal(1, ExifOrientation.read(type, 0, type.length), "the tag is not a SHORT");
        byte[] zero = ExifBlocks.tiff(0, false);
        Check.equal(1, ExifOrientation.read(zero, 0, zero.length), "value 0");
        byte[] nine = ExifBlocks.tiff(9, false);
        Check.equal(1, ExifOrientation.read(nine, 0, nine.length), "value 9");
        Check.equal(1, ExifOrientation.read(good, 0, good.length - 12), "the block is cut inside the entry");
        Check.equal(1, ExifOrientation.read(good, 0, 4), "only the start of the header");
        Check.equal(1, ExifOrientation.read(good, 0, 0), "empty block");
        Check.equal(1, ExifOrientation.read(null, 0, 10), "null data");
        Check.equal(1, ExifOrientation.read(good, -1, 5), "negative position");
        Check.equal(1, ExifOrientation.read(good, 4, good.length), "length beyond the data");
        // 항목이 아주 많다고 주장하지만 블록이 짧으면 범위를 벗어나지 않고 1 을 준다.
        byte[] many = good.clone();
        many[8] = (byte) 0xFF;
        many[9] = (byte) 0xFF;
        many[11] = 0x13;
        Check.equal(1, ExifOrientation.read(many, 0, many.length), "huge entry count");
    }

    /**
     * The inverse undoes the orientation: only the two quarter turns differ from their inverse.
     * <p>
     * 역방향은 방향을 되돌린다. 4분의 1 회전 둘만 자기 자신과 다르다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testInverse() throws Exception {
        Check.equal(8, ExifOrientation.inverse(6), "inverse of 6");
        Check.equal(6, ExifOrientation.inverse(8), "inverse of 8");
        Check.equal(1, ExifOrientation.inverse(9), "invalid value");
        Check.equal(1, ExifOrientation.inverse(0), "zero");
        int[] src = pixels();
        // 방향마다 적용한 뒤 역을 적용하면 원래 픽셀이 된다.
        for (int o = 1; o <= 8; o++) {
            int[] once = ExifBlocks.reference(src, W, H, o);
            int ow = o >= 5 ? H : W;
            int oh = o >= 5 ? W : H;
            int[] back = ExifBlocks.reference(once, ow, oh, ExifOrientation.inverse(o));
            ImageAsserts.pixels(new DecodedImage(W, H, back), W, H, src, "orientation " + o + " then its inverse");
            if (o != 6 && o != 8) {
                Check.equal(o, ExifOrientation.inverse(o), "orientation " + o + " undoes itself");
            }
        }
    }

    /**
     * {@link ExifOrientation#apply(DecodedImage, int)} gives the pixels and the size of the definition, keeps the frames of an animation,
     * and returns the same image for orientation 1.
     * <p>
     * {@link ExifOrientation#apply(DecodedImage, int)} 가 정의대로의 픽셀과 크기를 주고, 애니메이션의 프레임을 유지하며, 방향 1 에는 같은 이미지를
     * 반환한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testApplyMatchesDefinition() throws Exception {
        int[] src = pixels();
        DecodedImage img = new DecodedImage(W, H, src);
        Check.same(img, ExifOrientation.apply(img, 1), "orientation 1 returns the same image");
        Check.same(img, ExifOrientation.apply(img, 9), "an invalid orientation returns the same image");
        // 여덟 방향 모두를 정의와 대조한다.
        for (int o = 2; o <= 8; o++) {
            DecodedImage r = ExifOrientation.apply(img, o);
            int ow = o >= 5 ? H : W;
            int oh = o >= 5 ? W : H;
            Check.equal(ow, r.getWidth(), "orientation " + o + ": width");
            Check.equal(oh, r.getHeight(), "orientation " + o + ": height");
            Check.equal(o, r.getAppliedOrientation(), "orientation " + o + ": applied value kept");
            ImageAsserts.pixels(r, ow, oh, ExifBlocks.reference(src, W, H, o), "orientation " + o);
        }
        // 애니메이션은 모든 프레임에 같은 방향이 적용되고 지연과 반복 횟수는 그대로이다.
        List<DecodedFrame> frames = new ArrayList<DecodedFrame>();
        frames.add(new DecodedFrame(src, 40));
        int[] second = new int[src.length];
        // 둘째 프레임은 첫 프레임의 색을 뒤집은 값으로 한다.
        for (int i = 0; i < src.length; i++) {
            second[i] = src[i] ^ 0x00FFFFFF;
        }
        frames.add(new DecodedFrame(second, 70));
        DecodedImage anim = new DecodedImage(W, H, frames, 3);
        DecodedImage turned = ExifOrientation.apply(anim, 6);
        Check.equal(2, turned.getFrames().size(), "frame count");
        Check.equal(3, turned.getLoopCount(), "loop count");
        Check.equal(40, turned.getFrames().get(0).getDelayMillis(), "first delay");
        Check.equal(70, turned.getFrames().get(1).getDelayMillis(), "second delay");
        ImageAsserts.framePixels(turned.getFrames().get(0), ExifBlocks.reference(src, W, H, 6), "first frame");
        ImageAsserts.framePixels(turned.getFrames().get(1), ExifBlocks.reference(second, W, H, 6), "second frame");
    }

    /**
     * {@link DecodedImage#unoriented()} gives the stored pixels back for every orientation, keeps them for later calls and leaves an image
     * without an orientation alone.
     * <p>
     * {@link DecodedImage#unoriented()} 가 모든 방향에서 저장된 픽셀을 돌려주고 다음 호출을 위해 보관하며, 방향이 없는 이미지는 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnorientedRestoresTheStoredPixels() throws Exception {
        int[] src = pixels();
        DecodedImage plain = new DecodedImage(W, H, src);
        Check.same(plain, plain.unoriented(), "an upright image is its own unoriented image");
        Check.equal(1, plain.getAppliedOrientation(), "an upright image has orientation 1");
        // 방향마다 적용한 이미지에서 저장된 모양을 되찾는다.
        for (int o = 2; o <= 8; o++) {
            DecodedImage shown = ExifOrientation.apply(plain, o);
            DecodedImage stored = shown.unoriented();
            Check.equal(W, stored.getWidth(), "orientation " + o + ": stored width");
            Check.equal(H, stored.getHeight(), "orientation " + o + ": stored height");
            Check.equal(1, stored.getAppliedOrientation(), "orientation " + o + ": nothing applied any more");
            ImageAsserts.pixels(stored, W, H, src, "orientation " + o + " unoriented");
            Check.same(stored, shown.unoriented(), "orientation " + o + ": the second call returns the kept image");
        }
    }

    /**
     * The PNG decoder applies the orientation of an <code>eXIf</code> chunk in front of the image data, in both byte orders and with the
     * prefix, and the result equals the definition of the eight orientations.
     * <p>
     * PNG 디코더가 이미지 데이터 앞의 <code>eXIf</code> 청크의 방향을 두 바이트 순서와 머리말에서 적용하며 결과가 여덟 방향의 정의와 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPngOrientationBeforeTheImageData() throws Exception {
        int[] src = pixels();
        // 방향 1~8 과 변형(빅 엔디언, 리틀 엔디언, 머리말)마다 확인한다.
        for (int o = 1; o <= 8; o++) {
            for (int v = 0; v < 3; v++) {
                byte[] block = v == 0 ? ExifBlocks.tiff(o, true) : v == 1 ? ExifBlocks.tiff(o, false) : ExifBlocks.prefixed(o, true);
                DecodedImage img = new PngDecoder().decode(png(block), DecodeLimits.DEFAULT);
                int ow = o >= 5 ? H : W;
                int oh = o >= 5 ? W : H;
                String what = "PNG orientation " + o + " variant " + v;
                Check.equal(ow, img.getWidth(), what + ": width");
                Check.equal(oh, img.getHeight(), what + ": height");
                Check.equal(o, img.getAppliedOrientation(), what + ": applied value");
                ImageAsserts.pixels(img, ow, oh, ExifBlocks.reference(src, W, H, o), what);
            }
        }
    }

    /**
     * The chunk may also come behind the image data; only the first chunk counts; an invalid value, a truncated chunk and a chunk with a
     * wrong CRC leave the image upright.
     * <p>
     * 청크는 이미지 데이터 뒤에 와도 된다. 첫 청크만 인정한다. 잘못된 값, 잘린 청크, CRC 가 틀린 청크는 이미지를 그대로 둔다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPngOrientationBehindTheImageDataAndOddChunks() throws Exception {
        int[] src = pixels();
        byte[] late = insertBeforeIend(png(null), PngBuilder.chunk("eXIf", ExifBlocks.tiff(3, true)));
        DecodedImage img = new PngDecoder().decode(late, DecodeLimits.DEFAULT);
        ImageAsserts.pixels(img, W, H, ExifBlocks.reference(src, W, H, 3), "eXIf behind IDAT");
        // 첫 청크(6)만 인정하고 뒤의 청크(3)는 무시한다.
        byte[] two = insertBeforeIend(png(ExifBlocks.tiff(6, true)), PngBuilder.chunk("eXIf", ExifBlocks.tiff(3, true)));
        DecodedImage first = new PngDecoder().decode(two, DecodeLimits.DEFAULT);
        Check.equal(6, first.getAppliedOrientation(), "the first eXIf chunk counts");
        ImageAsserts.pixels(first, H, W, ExifBlocks.reference(src, W, H, 6), "first chunk wins");
        // 잘못된 값은 이미지를 돌리지 않는다.
        DecodedImage invalid = new PngDecoder().decode(png(ExifBlocks.tiff(9, true)), DecodeLimits.DEFAULT);
        ImageAsserts.pixels(invalid, W, H, src, "invalid value");
        Check.equal(1, invalid.getAppliedOrientation(), "invalid value: nothing applied");
        // CRC 가 틀린 보조 청크는 건너뛴다.
        byte[] bad = png(ExifBlocks.tiff(6, true));
        int at = 33 + 8 + ExifBlocks.tiff(6, true).length;
        bad[at] ^= 0x55;
        DecodedImage skipped = new PngDecoder().decode(bad, DecodeLimits.DEFAULT);
        ImageAsserts.pixels(skipped, W, H, src, "eXIf with a wrong CRC is skipped");
        // 블록이 아닌 것이 든 청크도 이미지를 해치지 않는다.
        DecodedImage junk = new PngDecoder().decode(png(new byte[] { 1, 2, 3 }), DecodeLimits.DEFAULT);
        ImageAsserts.pixels(junk, W, H, src, "eXIf with junk");
    }

    /**
     * The orientation of an APNG applies to every frame of the animation, and the delays stay.
     * <p>
     * APNG 의 방향은 애니메이션의 모든 프레임에 적용되고 지연은 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testApngOrientationAppliesToEveryFrame() throws Exception {
        int[] src = pixels();
        PngBuilder.Spec s = new PngBuilder.Spec(W, H);
        s.colorType = 6;
        s.depth = 8;
        s.samples = new int[W * H * 4];
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        int[][] frameSamples = new int[2][W * H * 4];
        // 첫 프레임은 시험 픽셀, 둘째 프레임은 색을 뒤집은 값이다.
        for (int i = 0; i < src.length; i++) {
            for (int f = 0; f < 2; f++) {
                int c = f == 0 ? src[i] : src[i] ^ 0x00FFFFFF;
                frameSamples[f][i * 4] = (c >> 16) & 0xFF;
                frameSamples[f][i * 4 + 1] = (c >> 8) & 0xFF;
                frameSamples[f][i * 4 + 2] = c & 0xFF;
                frameSamples[f][i * 4 + 3] = 255;
            }
        }
        frames.add(new PngBuilder.FrameSpec(W, H, 0, 0, frameSamples[0]));
        frames.add(new PngBuilder.FrameSpec(W, H, 0, 0, frameSamples[1]));
        byte[] apng = PngBuilder.encodeApng(s, 0, true, frames, false);
        // IHDR(33바이트) 바로 뒤에 eXIf 청크를 끼워 넣는다.
        byte[] chunk = PngBuilder.chunk("eXIf", ExifBlocks.tiff(8, false));
        byte[] with = new byte[apng.length + chunk.length];
        System.arraycopy(apng, 0, with, 0, 33);
        System.arraycopy(chunk, 0, with, 33, chunk.length);
        System.arraycopy(apng, 33, with, 33 + chunk.length, apng.length - 33);
        DecodedImage img = new PngDecoder().decode(with, DecodeLimits.DEFAULT);
        Check.equal(2, img.getFrames().size(), "frame count");
        Check.equal(H, img.getWidth(), "width after a quarter turn");
        Check.equal(W, img.getHeight(), "height after a quarter turn");
        int[] second = new int[src.length];
        // 둘째 프레임의 기대 픽셀을 만든다.
        for (int i = 0; i < src.length; i++) {
            second[i] = src[i] ^ 0x00FFFFFF;
        }
        ImageAsserts.framePixels(img.getFrames().get(0), ExifBlocks.reference(src, W, H, 8), "first frame");
        ImageAsserts.framePixels(img.getFrames().get(1), ExifBlocks.reference(second, W, H, 8), "second frame");
    }
}
