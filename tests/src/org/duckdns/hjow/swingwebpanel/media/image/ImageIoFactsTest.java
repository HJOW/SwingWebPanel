package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Probes what the running Java runtime's ImageIO supports (plan item P18-02: "check the real JRE"). It prints the facts as lines starting with
 * <code>[JRE fact]</code> so that they can be copied into the progress record and the feature matrix, and it asserts only the abilities that
 * {@link JpegDecoder} relies on. Run it on JDK 6u45 to get the facts of the minimum target runtime.
 * <p>
 * 실행 중인 Java 런타임의 ImageIO 가 지원하는 것을 조사한다(계획 P18-02: "실제 JRE 확인"). 결과를 진행 기록과 기능 표에 옮길 수 있도록
 * <code>[JRE fact]</code> 로 시작하는 줄로 출력하며 {@link JpegDecoder} 가 기대는 능력만 단언한다. 최소 대상 런타임의 사실을 얻으려면 JDK 6u45 로
 * 실행한다.
 */
public class ImageIoFactsTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageIoFactsTest() {
    }

    /**
     * Prints one fact line.
     * <p>
     * 사실 한 줄을 출력한다.
     *
     * @param s the fact / 사실
     */
    private static void fact(String s) {
        System.out.println("[JRE fact] " + s);
    }

    /**
     * Lists the formats of ImageIO and checks the readers that the decoders rely on.
     * <p>
     * ImageIO 의 형식을 나열하고 디코더가 기대는 리더를 확인한다.
     */
    public void testReadersAndWriters() {
        fact("java.version = " + System.getProperty("java.version") + ", java.vm.name = " + System.getProperty("java.vm.name"));
        fact("ImageIO reader formats = " + sorted(ImageIO.getReaderFormatNames()));
        fact("ImageIO reader MIME types = " + sorted(ImageIO.getReaderMIMETypes()));
        fact("ImageIO writer formats = " + sorted(ImageIO.getWriterFormatNames()));
        Check.isTrue(ImageIO.getImageReadersByFormatName("jpeg").hasNext(), "a JPEG reader exists (used by JpegDecoder)");
        Check.isTrue(ImageIO.getImageWritersByFormatName("jpeg").hasNext(), "a JPEG writer exists (used by the tests)");
        List<String> r = Arrays.asList(ImageIO.getReaderFormatNames());
        fact("readers for webp/avif/svg/ico: " + r.contains("webp") + "/" + r.contains("avif") + "/" + r.contains("svg") + "/" + r.contains("ico"));
    }

    /**
     * Returns the sorted, lower-cased and de-duplicated names.
     * <p>
     * 정렬하고 소문자로 바꿔 중복을 없앤 이름을 반환한다.
     *
     * @param names names / 이름
     * @return text / 문자열
     */
    private static String sorted(String[] names) {
        java.util.TreeSet<String> set = new java.util.TreeSet<String>();
        // 이름을 소문자로 바꿔 모은다.
        for (String n : names) {
            set.add(n.toLowerCase());
        }
        return set.toString();
    }

    /**
     * The JPEG reader gives raw component samples for baseline, progressive, grey and four component files, and reports dimensions it cannot
     * handle as errors.
     * <p>
     * JPEG 리더는 baseline, progressive, 회색, 성분 네 개 파일의 원시 성분 샘플을 주고 처리할 수 없는 크기는 오류로 보고한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testJpegRaster() throws Exception {
        BufferedImage rgb = new BufferedImage(16, 8, BufferedImage.TYPE_INT_RGB);
        BufferedImage grey = new BufferedImage(16, 8, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster four = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 16, 8, 4, null);
        byte[] baseline = jpeg(rgb, null, false);
        byte[] progressive = jpeg(rgb, null, true);
        byte[] gray = jpeg(grey, null, false);
        byte[] cmyk = jpeg(null, four, false);
        Check.equal(3, bands(baseline), "baseline readRaster bands");
        Check.equal(3, bands(progressive), "progressive readRaster bands");
        Check.equal(1, bands(gray), "grey readRaster bands");
        Check.equal(4, bands(cmyk), "four component readRaster bands");
        fact("JPEG: readRaster works for baseline (3 bands), progressive (3 bands), grey (1 band) and 4 components (4 bands, raw values, no colour conversion)");
        try {
            ImageIO.read(new ByteArrayInputStream(cmyk));
            fact("JPEG: ImageIO.read() of a 4 component file succeeded");
        } catch (Exception e) {
            fact("JPEG: ImageIO.read() of a 4 component file fails: " + e);
        }
        byte[] big = jpeg(rgb, null, false);
        int sof = -1;
        // 각 항목을 차례로 처리한다.
        for (int i = 0; i < big.length - 9; i++) {
            if ((big[i] & 0xFF) == 0xFF && (big[i + 1] & 0xFF) == 0xC0) {
                sof = i;
                break;
            }
        }
        // 각 항목을 차례로 처리한다.
        for (int i = 5; i <= 8; i++) {
            big[sof + i] = (byte) 0xFF;
        }
        try {
            bands(big);
            fact("JPEG: a 65535 x 65535 header is accepted by the reader");
        } catch (Exception e) {
            fact("JPEG: a 65535 x 65535 header makes the reader fail: " + e);
        }
    }

    /**
     * Writes a JPEG.
     * <p>
     * JPEG 를 쓴다.
     *
     * @param img         image or null / 이미지 또는 null
     * @param raster      raster or null / 래스터 또는 null
     * @param progressive progressive mode / progressive 방식
     * @return bytes / 바이트
     * @throws Exception on failure / 실패 시
     */
    private static byte[] jpeg(BufferedImage img, WritableRaster raster, boolean progressive) throws Exception {
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = w.getDefaultWriteParam();
        if (progressive) {
            p.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageOutputStream ios = new MemoryCacheImageOutputStream(bos);
        w.setOutput(ios);
        w.write(null, raster != null ? new IIOImage(raster, null, null) : new IIOImage(img, null, null), p);
        ios.close();
        w.dispose();
        return bos.toByteArray();
    }

    /**
     * Returns the number of bands that readRaster gives.
     * <p>
     * readRaster 가 주는 밴드 수를 반환한다.
     *
     * @param d JPEG file / JPEG 파일
     * @return bands / 밴드 수
     * @throws Exception on failure / 실패 시
     */
    private static int bands(byte[] d) throws Exception {
        ImageReader r = ImageIO.getImageReadersByFormatName("jpeg").next();
        try {
            r.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(d)), true, true);
            return r.readRaster(0, null).getNumBands();
        } finally {
            r.dispose();
        }
    }

    /**
     * Records what ImageIO does with PNG variants, animated PNG and GIF, RLE bitmaps and icons (facts only; our own decoders do not depend on
     * these abilities).
     * <p>
     * ImageIO 가 PNG 변형, 애니메이션 PNG/GIF, RLE 비트맵, 아이콘을 어떻게 다루는지 기록한다(사실 기록만. 자체 디코더는 이 능력에 기대지 않는다).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOtherFormatsFacts() throws Exception {
        BufferedImage g16 = new BufferedImage(5, 4, BufferedImage.TYPE_USHORT_GRAY);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(g16, "png", bos);
        fact("PNG: writing TYPE_USHORT_GRAY gives bit depth " + (bos.toByteArray()[24] & 0xFF) + " (IHDR) and ImageIO reads it back as type "
                + ImageIO.read(new ByteArrayInputStream(bos.toByteArray())).getType());
        PngBuilder.Spec s = new PngBuilder.Spec(4, 4);
        s.samples = new int[64];
        s.interlace = true;
        s.colorType = 6;
        ImageIO.read(new ByteArrayInputStream(PngBuilder.encode(s)));
        fact("PNG: ImageIO reads an Adam7 interlaced RGBA file");
        s.colorType = 3;
        s.depth = 2;
        s.interlace = false;
        s.samples = new int[16];
        s.plte = new byte[12];
        fact("PNG: ImageIO reads a 2-bit palette file as type " + ImageIO.read(new ByteArrayInputStream(PngBuilder.encode(s))).getType());
        List<PngBuilder.FrameSpec> frames = new ArrayList<PngBuilder.FrameSpec>();
        PngBuilder.Spec c = new PngBuilder.Spec(2, 2);
        c.samples = new int[16];
        frames.add(new PngBuilder.FrameSpec(2, 2, 0, 0, new int[16]));
        frames.add(new PngBuilder.FrameSpec(2, 2, 0, 0, new int[16]));
        BufferedImage first = ImageIO.read(new ByteArrayInputStream(PngBuilder.encodeApng(c, 0, true, frames, false)));
        fact("APNG: ImageIO reads only the default image (" + first.getWidth() + "x" + first.getHeight() + "), animation is ignored");
        byte[] gif = new GifBuilder(2, 2, new int[] { 0, 0xFFFFFF }).frame(0, 0, 2, 2, new byte[4], null, false, 0, 5, -1, 2)
                .frame(0, 0, 2, 2, new byte[] { 1, 1, 1, 1 }, null, false, 0, 5, -1, 2).build();
        ImageReader gr = ImageIO.getImageReadersByFormatName("gif").next();
        gr.setInput(new MemoryCacheImageInputStream(new ByteArrayInputStream(gif)));
        fact("GIF: ImageIO sees " + gr.getNumImages(true) + " images in a 2 frame file (frames are sub images, not composed)");
        gr.dispose();
        int[] pal = new int[256];
        byte[] rle = BmpBuilder.build(true, 40, 3, 1, 8, 1, 0, null, pal, new byte[] { 3, 1, 0, 1 });
        try {
            ImageIO.read(new ByteArrayInputStream(rle));
            fact("BMP: ImageIO reads an RLE8 bitmap");
        } catch (Exception e) {
            fact("BMP: ImageIO fails on an RLE8 bitmap: " + e);
        }
        List<byte[]> l = new ArrayList<byte[]>();
        l.add(BmpBuilder.iconDib(2, 2, 32, null, new int[4], new int[4]));
        byte[] icoBytes = BmpBuilder.ico(1, l, new int[] { 2 }, new int[] { 2 });
        try {
            BufferedImage ico = ImageIO.read(new ByteArrayInputStream(icoBytes));
            fact("ICO: ImageIO has no ICO reader (read() returns " + (ico == null ? "null" : "an image") + ")");
        } catch (Exception e) {
            fact("ICO: ImageIO has no ICO reader and its WBMP reader claims the file (type 0 header) and fails: " + e);
        }
        Check.isTrue(ImageDecoders.isSupportedMime("image/x-icon"), "our registry reads ICO even though ImageIO does not");
        Check.equal(2, ImageDecoders.decode(BmpBuilder.ico(1, l, new int[] { 2 }, new int[] { 2 }), null, DecodeLimits.DEFAULT).getWidth(),
                "ICO through the registry");
    }
}
