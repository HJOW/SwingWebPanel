package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link JpegDecoder} and the EXIF orientation helper (plan items P18-02, P18-03; FEATURE_MATRIX IMG-04): baseline and progressive
 * files, grey, RGB, CMYK and YCCK colour rules, all eight EXIF orientations in both byte orders, truncated, unsupported and hostile data. The
 * inputs come from the JRE's JPEG writer, with APP segments inserted or removed by this test.
 * <p>
 * {@link JpegDecoder} 와 EXIF 방향 도우미 시험(계획 P18-02, P18-03; FEATURE_MATRIX IMG-04): baseline/progressive 파일, 회색, RGB, CMYK, YCCK 색 규칙,
 * 두 가지 바이트 순서의 EXIF 방향 여덟 가지, 잘리거나 지원하지 않거나 악의적인 데이터. 입력은 JRE 의 JPEG 작성기에서 얻고 APP 세그먼트는 이
 * 시험이 넣거나 뺀다.
 */
public class JpegDecoderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public JpegDecoderTest() {
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
        return new JpegDecoder().decode(d, DecodeLimits.DEFAULT);
    }

    /**
     * Builds a 64 x 32 test image with four colour quadrants: red, green (top), blue, white (bottom).
     * <p>
     * 색이 네 구역(위쪽 빨강, 초록, 아래쪽 파랑, 흰색)인 64 x 32 시험 이미지를 만든다.
     *
     * @return the image / 이미지
     */
    private static BufferedImage quadrants() {
        BufferedImage img = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
        int[] colours = { 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF };
        // 픽셀이 속한 구역의 색으로 칠한다.
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                img.setRGB(x, y, colours[(y / 16) * 2 + x / 32]);
            }
        }
        return img;
    }

    /**
     * Writes an image as JPEG with the JRE's writer.
     * <p>
     * JRE 의 작성기로 이미지를 JPEG 로 쓴다.
     *
     * @param img         the image or a raster holder / 이미지
     * @param raster      a raster to write instead (may be null) / 대신 쓸 래스터(null 가능)
     * @param progressive true for a progressive file / progressive 파일이면 true
     * @return file bytes / 파일 바이트
     * @throws Exception on failure / 실패 시
     */
    private static byte[] write(BufferedImage img, WritableRaster raster, boolean progressive) throws Exception {
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
     * Returns the position of the first marker segment of a type before the scan, or -1.
     * <p>
     * 스캔 앞에서 주어진 마커의 첫 세그먼트 위치를 반환하며 없으면 -1.
     *
     * @param d      file / 파일
     * @param marker marker byte (0xE0 and so on) / 마커 바이트
     * @return position of the 0xFF byte, or -1 / 0xFF 바이트의 위치. 없으면 -1
     */
    private static int findSegment(byte[] d, int marker) {
        int pos = 2;
        // 스캔(SOS) 앞까지 세그먼트를 따라간다.
        while (pos + 4 <= d.length && (d[pos] & 0xFF) == 0xFF) {
            int m = d[pos + 1] & 0xFF;
            if (m == marker) {
                return pos;
            }
            if (m == 0xDA) {
                return -1;
            }
            pos += 2 + (((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF));
        }
        return -1;
    }

    /**
     * Removes the first segment of a marker type.
     * <p>
     * 마커 유형의 첫 세그먼트를 제거한다.
     *
     * @param d      file / 파일
     * @param marker marker byte / 마커 바이트
     * @return the file without the segment / 세그먼트를 뺀 파일
     */
    private static byte[] strip(byte[] d, int marker) {
        int pos = findSegment(d, marker);
        if (pos < 0) {
            return d;
        }
        int len = 2 + (((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF));
        byte[] r = new byte[d.length - len];
        System.arraycopy(d, 0, r, 0, pos);
        System.arraycopy(d, pos + len, r, pos, d.length - pos - len);
        return r;
    }

    /**
     * Inserts a segment right after the start-of-image marker.
     * <p>
     * 이미지 시작 마커 바로 뒤에 세그먼트를 넣는다.
     *
     * @param d       file / 파일
     * @param segment complete segment including marker and length / 마커와 길이를 포함한 완전한 세그먼트
     * @return the new file / 새 파일
     */
    private static byte[] insert(byte[] d, byte[] segment) {
        byte[] r = new byte[d.length + segment.length];
        System.arraycopy(d, 0, r, 0, 2);
        System.arraycopy(segment, 0, r, 2, segment.length);
        System.arraycopy(d, 2, r, 2 + segment.length, d.length - 2);
        return r;
    }

    /**
     * Builds an Adobe APP14 segment.
     * <p>
     * Adobe APP14 세그먼트를 만든다.
     *
     * @param transform colour transform 0, 1 or 2 / 색 변환 0, 1, 2
     * @return the segment / 세그먼트
     */
    private static byte[] adobe(int transform) {
        return new Bytes().u8(0xFF).u8(0xEE).be16(14).ascii("Adobe").be16(100).be16(0).be16(0).u8(transform).toBytes();
    }

    /**
     * Builds an EXIF APP1 segment with only the orientation tag.
     * <p>
     * 방향 태그만 가진 EXIF APP1 세그먼트를 만든다.
     *
     * @param orientation orientation value / 방향 값
     * @param big         true for big-endian (MM) / 빅 엔디언(MM)이면 true
     * @return the segment / 세그먼트
     */
    private static byte[] exif(int orientation, boolean big) {
        Bytes b = new Bytes();
        b.u8(0xFF).u8(0xE1).be16(34).ascii("Exif").u8(0).u8(0);
        if (big) {
            b.ascii("MM").be16(42).be32(8).be16(1).be16(0x0112).be16(3).be32(1).be16(orientation).be16(0).be32(0);
        } else {
            b.ascii("II").le16(42).le32(8).le16(1).le16(0x0112).le16(3).le32(1).le16(orientation).le16(0).le32(0);
        }
        return b.toBytes();
    }

    /**
     * Checks that a decoded pixel is near an opaque colour.
     * <p>
     * 디코딩된 픽셀이 불투명 색에 가까운지 검사한다.
     *
     * @param img    image / 이미지
     * @param x      column / 열
     * @param y      row / 행
     * @param rgb    expected 0xRRGGBB / 기대 0xRRGGBB
     * @param tol    tolerance per channel / 채널별 허용 오차
     * @param what   description / 설명
     */
    private static void near(DecodedImage img, int x, int y, int rgb, int tol, String what) {
        ImageAsserts.near(0xFF000000 | rgb, img.getFrames().get(0).getArgb()[y * img.getWidth() + x], tol, what + " at " + x + "," + y);
    }

    /**
     * Baseline and progressive RGB files decode to the colours of the four quadrants.
     * <p>
     * baseline 과 progressive RGB 파일이 네 구역의 색으로 디코딩된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBaselineAndProgressive() throws Exception {
        // 두 방식의 파일을 같은 방식으로 확인한다.
        for (int prog = 0; prog < 2; prog++) {
            byte[] jpg = write(quadrants(), null, prog == 1);
            Check.equal(prog == 1 ? 0xC2 : 0xC0, sofMarker(jpg), "process of the written file");
            DecodedImage img = decode(jpg);
            Check.equal(64, img.getWidth(), "width");
            Check.equal(32, img.getHeight(), "height");
            Check.isFalse(img.isAnimated(), "still image");
            String what = prog == 1 ? "progressive" : "baseline";
            near(img, 16, 8, 0xFF0000, 40, what + " red");
            near(img, 48, 8, 0x00FF00, 40, what + " green");
            near(img, 16, 24, 0x0000FF, 40, what + " blue");
            near(img, 48, 24, 0xFFFFFF, 40, what + " white");
        }
    }

    /**
     * Returns the marker byte of the start-of-frame segment.
     * <p>
     * 프레임 시작 세그먼트의 마커 바이트를 반환한다.
     *
     * @param d file / 파일
     * @return marker byte, or -1 / 마커 바이트. 없으면 -1
     */
    private static int sofMarker(byte[] d) {
        // 가능한 SOF 마커를 차례로 찾는다.
        for (int m = 0xC0; m <= 0xCF; m++) {
            if (m != 0xC4 && m != 0xC8 && m != 0xCC && findSegment(d, m) >= 0) {
                return m;
            }
        }
        return -1;
    }

    /**
     * Grey files give grey pixels equal to the stored samples.
     * <p>
     * 회색 파일은 저장된 샘플과 같은 회색 픽셀을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testGrey() throws Exception {
        BufferedImage g = new BufferedImage(32, 16, BufferedImage.TYPE_BYTE_GRAY);
        // 가로로 0 에서 248 까지 변하는 회색 띠를 래스터에 직접 쓴다.
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 32; x++) {
                g.getRaster().setSample(x, y, 0, x * 8);
            }
        }
        DecodedImage img = decode(write(g, null, false));
        Check.equal(32, img.getWidth(), "width");
        // 중간 열은 저장한 샘플과 같아야 한다(양자화 오차 허용).
        for (int x = 2; x < 30; x += 4) {
            int v = x * 8;
            near(img, x, 8, (v << 16) | (v << 8) | v, 12, "grey sample " + v);
        }
    }

    /**
     * The colour rules of four component files: plain CMYK, Adobe inverted CMYK and Adobe YCCK.
     * <p>
     * 성분 네 개 파일의 색 규칙: 일반 CMYK, Adobe 반전 CMYK, Adobe YCCK.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCmykAndYcck() throws Exception {
        WritableRaster r = java.awt.image.Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, 16, 16, 4, null);
        int[] v = { 100, 90, 160, 50 };
        // 모든 픽셀을 같은 네 성분 값으로 채운다.
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                // 각 항목을 차례로 처리한다.
                for (int b = 0; b < 4; b++) {
                    r.setSample(x, y, b, v[b]);
                }
            }
        }
        byte[] raw = strip(write(null, r, false), 0xEE);
        // 표지가 없으면 일반 잉크량으로 본다.
        int rr = (255 - v[0]) * (255 - v[3]) / 255;
        int gg = (255 - v[1]) * (255 - v[3]) / 255;
        int bb = (255 - v[2]) * (255 - v[3]) / 255;
        near(decode(raw), 8, 8, (rr << 16) | (gg << 8) | bb, 10, "CMYK without Adobe marker");
        // Adobe 변환 0 은 Photoshop 처럼 반전된 CMYK 이다.
        near(decode(insert(raw, adobe(0))), 8, 8, ((v[0] * v[3] / 255) << 16) | ((v[1] * v[3] / 255) << 8) | (v[2] * v[3] / 255), 10,
                "Adobe CMYK (inverted)");
        // Adobe 변환 2 는 YCCK 이다: YCC 를 RGB 로 바꾼 값의 보수가 잉크이고 반전되어 있다.
        double cr = v[0] + 1.402 * (v[2] - 128);
        double cg = v[0] - 0.344136 * (v[1] - 128) - 0.714136 * (v[2] - 128);
        double cb = v[0] + 1.772 * (v[1] - 128);
        int er = (int) ((255 - clamp(cr)) * v[3] / 255);
        int eg = (int) ((255 - clamp(cg)) * v[3] / 255);
        int eb = (int) ((255 - clamp(cb)) * v[3] / 255);
        near(decode(insert(raw, adobe(2))), 8, 8, (er << 16) | (eg << 8) | eb, 10, "Adobe YCCK");
    }

    /**
     * Clamps a value to 0 to 255.
     * <p>
     * 값을 0~255 로 제한한다.
     *
     * @param v value / 값
     * @return clamped value / 제한된 값
     */
    private static double clamp(double v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    /**
     * Three component files: the Adobe transform decides between RGB and YCbCr; JFIF means YCbCr; component ids R, G, B without JFIF or
     * Adobe mean RGB.
     * <p>
     * 성분 세 개 파일: Adobe 변환이 RGB 와 YCbCr 를 정하고 JFIF 는 YCbCr 이며 JFIF/Adobe 가 없고 성분 id 가 R, G, B 이면 RGB 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testThreeComponentColourRules() throws Exception {
        BufferedImage red = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        // 각 항목을 차례로 처리한다.
        for (int y = 0; y < 16; y++) {
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < 16; x++) {
                red.setRGB(x, y, 0xFF0000);
            }
        }
        byte[] jfif = write(red, null, false);
        near(decode(jfif), 8, 8, 0xFF0000, 12, "JFIF means YCbCr");
        near(decode(insert(jfif, adobe(1))), 8, 8, 0xFF0000, 12, "Adobe transform 1 means YCbCr");
        // 변환 0 이면 저장된 값 (Y, Cb, Cr) = (76, 85, 255) 를 그대로 RGB 로 읽는다.
        near(decode(insert(jfif, adobe(0))), 8, 8, (76 << 16) | (85 << 8) | 255, 12, "Adobe transform 0 means RGB");
        // JFIF 와 Adobe 를 모두 빼고 성분 id 를 R, G, B 로 바꾸면 RGB 이다.
        byte[] ids = strip(jfif, 0xE0);
        int sof = findSegment(ids, 0xC0);
        int sos = indexOfScan(ids);
        // 각 항목을 차례로 처리한다.
        for (int c = 0; c < 3; c++) {
            ids[sof + 10 + 3 * c] = (byte) "RGB".charAt(c);
            ids[sos + 5 + 2 * c] = (byte) "RGB".charAt(c);
        }
        near(decode(ids), 8, 8, (76 << 16) | (85 << 8) | 255, 12, "component ids R, G, B mean RGB");
    }

    /**
     * Returns the position of the start-of-scan marker.
     * <p>
     * 스캔 시작 마커의 위치를 반환한다.
     *
     * @param d file / 파일
     * @return position of the 0xFF byte of SOS / SOS 의 0xFF 바이트 위치
     */
    private static int indexOfScan(byte[] d) {
        int pos = 2;
        // 세그먼트 길이를 따라 SOS 까지 간다.
        while ((d[pos + 1] & 0xFF) != 0xDA) {
            pos += 2 + (((d[pos + 2] & 0xFF) << 8) | (d[pos + 3] & 0xFF));
        }
        return pos;
    }

    /**
     * Maps a point of the source image to its place after an EXIF orientation (the forward description of each orientation).
     * <p>
     * EXIF 방향을 적용한 뒤 원본 이미지의 점이 놓이는 곳을 계산한다(각 방향의 정방향 설명).
     *
     * @param o orientation 1 to 8 / 방향 1~8
     * @param x source column / 원본 열
     * @param y source row / 원본 행
     * @param w source width / 원본 가로
     * @param h source height / 원본 세로
     * @return {column, row} after the orientation / 방향 적용 뒤의 {열, 행}
     */
    private static int[] forward(int o, int x, int y, int w, int h) {
        switch (o) {
        case 2:
            return new int[] { w - 1 - x, y };
        case 3:
            return new int[] { w - 1 - x, h - 1 - y };
        case 4:
            return new int[] { x, h - 1 - y };
        case 5:
            return new int[] { y, x };
        case 6:
            return new int[] { h - 1 - y, x };
        case 7:
            return new int[] { h - 1 - y, w - 1 - x };
        case 8:
            return new int[] { y, w - 1 - x };
        default:
            return new int[] { x, y };
        }
    }

    /**
     * Every EXIF orientation, in both byte orders, turns the four quadrants to the expected places and swaps the size for 5 to 8.
     * <p>
     * 모든 EXIF 방향이 두 가지 바이트 순서에서 네 구역을 기대한 위치로 돌리며 5~8 은 크기를 바꾼다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testExifOrientations() throws Exception {
        byte[] base = write(quadrants(), null, false);
        int[][] centres = { { 16, 8 }, { 48, 8 }, { 16, 24 }, { 48, 24 } };
        int[] colours = { 0xFF0000, 0x00FF00, 0x0000FF, 0xFFFFFF };
        // 방향 1~8 과 두 바이트 순서의 모든 조합을 확인한다.
        for (int o = 1; o <= 8; o++) {
            for (int e = 0; e < 2; e++) {
                DecodedImage img = decode(insert(base, exif(o, e == 1)));
                boolean swap = o >= 5;
                String what = "orientation " + o + (e == 1 ? " MM" : " II");
                Check.equal(swap ? 32 : 64, img.getWidth(), what + ": width");
                Check.equal(swap ? 64 : 32, img.getHeight(), what + ": height");
                // 각 항목을 차례로 처리한다.
                for (int q = 0; q < 4; q++) {
                    int[] p = forward(o, centres[q][0], centres[q][1], 64, 32);
                    near(img, p[0], p[1], colours[q], 45, what + " quadrant " + q);
                }
            }
        }
    }

    /**
     * The orientation helper is exact: every orientation of a 3 x 2 array equals the forward mapping of each element.
     * <p>
     * 방향 도우미는 정확하다: 3 x 2 배열의 모든 방향이 각 요소의 정방향 변환과 같다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOrientHelperIsExact() throws Exception {
        int w = 3;
        int h = 2;
        int[] src = { 10, 11, 12, 20, 21, 22 };
        // 방향마다 모든 요소의 위치를 확인한다.
        for (int o = 1; o <= 8; o++) {
            int[] size = new int[2];
            int[] out = PixelOps.orient(src, w, h, o, size);
            int ow = o >= 5 ? h : w;
            int oh = o >= 5 ? w : h;
            Check.equal(ow, size[0], "orientation " + o + ": width");
            Check.equal(oh, size[1], "orientation " + o + ": height");
            // 각 항목을 차례로 처리한다.
            for (int y = 0; y < h; y++) {
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < w; x++) {
                    int[] p = forward(o, x, y, w, h);
                    Check.equal(src[y * w + x], out[p[1] * ow + p[0]], "orientation " + o + " element " + x + "," + y);
                }
            }
        }
        int[] size = new int[2];
        Check.same(src, PixelOps.orient(src, w, h, 1, size), "orientation 1 returns the same array");
        Check.same(src, PixelOps.orient(src, w, h, 9, size), "an invalid orientation is ignored");
        Check.same(src, PixelOps.orient(src, w, h, 0, size), "orientation 0 is ignored");
        Check.equal(3, size[0], "size kept");
    }

    /**
     * An orientation tag that is damaged or points outside the EXIF block is ignored.
     * <p>
     * 손상되었거나 EXIF 블록 밖을 가리키는 방향 태그는 무시된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenExifIsIgnored() throws Exception {
        byte[] base = write(quadrants(), null, false);
        byte[] bad = exif(6, true);
        bad[10 + 4] = 0x7F;
        Check.equal(64, decode(insert(base, bad)).getWidth(), "IFD offset outside the block");
        byte[] value = exif(6, false);
        value[value.length - 8] = 99;
        Check.equal(64, decode(insert(base, value)).getWidth(), "orientation value 99");
        byte[] type = exif(6, true);
        type[type.length - 15] = 4;
        Check.equal(64, decode(insert(base, type)).getWidth(), "a tag other than the orientation is ignored");
        Check.equal(64, decode(insert(base, new Bytes().u8(0xFF).u8(0xE1).be16(8).ascii("Exif").u8(0).u8(0).toBytes())).getWidth(), "empty EXIF");
    }

    /**
     * A truncated file gives the part that could be decoded (the size is right); a file cut before the image data is CORRUPT.
     * <p>
     * 잘린 파일은 디코딩할 수 있는 부분을 주며(크기는 맞다) 이미지 데이터 앞에서 잘린 파일은 CORRUPT 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTruncation() throws Exception {
        final byte[] jpg = write(quadrants(), null, false);
        DecodedImage part = decode(ImageAsserts.prefix(jpg, jpg.length - 40));
        Check.equal(64, part.getWidth(), "partial width");
        Check.equal(32, part.getHeight(), "partial height");
        near(part, 16, 4, 0xFF0000, 40, "the decoded part is correct");
        // 헤더 안에서 자른 파일들은 모두 CORRUPT 이다.
        for (final int n : new int[] { 0, 2, 3, 4, 10, 20, 100 }) {
            ImageAsserts.fails(MediaException.Kind.CORRUPT, new Block() {
                /**
                 * Decodes a prefix of the file.
                 * <p>
                 * 파일의 앞부분을 디코딩한다.
                 *
                 * @throws Exception always MediaException / 항상 MediaException
                 */
                public void run() throws Exception {
                    decode(ImageAsserts.prefix(jpg, n));
                }
            }, "prefix of " + n + " bytes");
        }
    }

    /**
     * Unsupported processes and sizes are reported with the right kind: arithmetic and lossless processes, 12-bit samples, two components,
     * huge or zero sizes.
     * <p>
     * 지원하지 않는 방식과 크기는 올바른 종류로 보고된다: 산술/무손실 방식, 12비트 샘플, 성분 두 개, 아주 큰 크기와 0 크기.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnsupportedAndHugeSizes() throws Exception {
        byte[] jpg = write(quadrants(), null, false);
        int sof = findSegment(jpg, 0xC0);
        jpgFails(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(jpg, sof + 1, 0xC9), "arithmetic coding");
        jpgFails(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(jpg, sof + 1, 0xC3), "lossless");
        jpgFails(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(jpg, sof + 1, 0xC5), "differential");
        jpgFails(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(jpg, sof + 4, 12), "12-bit samples");
        jpgFails(MediaException.Kind.UNSUPPORTED_FORMAT, ImageAsserts.patched(jpg, sof + 9, 2), "two components");
        jpgFails(MediaException.Kind.CORRUPT, ImageAsserts.patched(jpg, sof + 7, 0, 0), "width 0");
        jpgFails(MediaException.Kind.CORRUPT, ImageAsserts.patched(jpg, sof + 5, 0, 0), "height 0");
        long start = System.currentTimeMillis();
        jpgFails(MediaException.Kind.TOO_LARGE, ImageAsserts.patched(jpg, sof + 5, 0xFF, 0xFF, 0xFF, 0xFF), "65535 x 65535");
        Check.isTrue(System.currentTimeMillis() - start < 5000, "rejected at once");
        jpgFails(MediaException.Kind.CORRUPT, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 2, (byte) 0xFF, (byte) 0xD9 },
                "no frame header");
        jpgFails(MediaException.Kind.CORRUPT, new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 1, 2, 3, 4, 5, 6 }, "garbage after SOI");
        jpgFails(MediaException.Kind.CORRUPT, ImageAsserts.patched(jpg, 2, 0xFF, 0xE0, 0, 0), "segment length below 2");
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
    private static void jpgFails(MediaException.Kind kind, final byte[] data, String what) {
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
     * Mutated and truncated files never fail with anything but a {@link MediaException}; an interrupted thread stops the decoder with
     * CANCELLED.
     * <p>
     * 변형되거나 잘린 파일은 {@link MediaException} 말고 다른 것으로 실패하지 않고 인터럽트된 스레드는 디코더를 CANCELLED 로 멈추게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHostileDataAndCancellation() throws Exception {
        final byte[] jpg = write(quadrants(), null, true);
        DecodeLimits small = new DecodeLimits(1000000, 1 << 22, 100, 4000000);
        Random rnd = new Random(2024);
        // 변형한 파일을 여러 번 디코딩한다.
        for (int round = 0; round < 300; round++) {
            byte[] m = jpg.clone();
            int flips = 1 + rnd.nextInt(4);
            // 각 항목을 차례로 처리한다.
            for (int k = 0; k < flips; k++) {
                m[rnd.nextInt(m.length)] = (byte) rnd.nextInt(256);
            }
            if (round % 3 == 0) {
                m = Arrays.copyOf(m, rnd.nextInt(m.length));
            }
            try {
                DecodedImage img = new JpegDecoder().decode(m, small);
                Check.isTrue(img.getWidth() > 0, "a decoded image has a size");
            } catch (MediaException expected) {
                Check.notNull(expected.getKind(), "every failure has a kind");
            }
        }
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
                    decode(jpg);
                }
            }, "interrupted decode");
            Check.isTrue(Thread.currentThread().isInterrupted(), "the interrupt flag is still set");
        } finally {
            Thread.interrupted();
        }
    }
}
