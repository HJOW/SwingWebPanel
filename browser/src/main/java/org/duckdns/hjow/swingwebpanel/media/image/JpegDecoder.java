package org.duckdns.hjow.swingwebpanel.media.image;

import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;

/**
 * A JPEG decoder (plan items P18-02, P18-03; FEATURE_MATRIX IMG-04) that combines its own parsing of the markers with the sample decoding
 * of the JRE's <code>javax.imageio</code> JPEG reader. Our code reads the headers first (size, precision, process, component count, JFIF,
 * Adobe and EXIF segments), checks the size limits <em>before</em> any decoding, asks ImageIO only for the raw component samples
 * (<code>readRaster</code>, no colour conversion, so no ICC surprises), and does the colour conversion itself.
 * <p>
 * Supported: baseline, extended sequential and progressive Huffman JPEG with 8-bit samples; 1 component (grey), 3 components (YCbCr or
 * RGB, chosen as libjpeg does: Adobe transform 0 = RGB, JFIF = YCbCr, component ids R, G, B = RGB, otherwise YCbCr), 4 components (CMYK
 * or YCCK; with an Adobe marker the values are inverted as Photoshop writes them, without it they are plain ink amounts); the EXIF orientation
 * (1 to 8) is applied to the pixels and the size ({@link ExifOrientation}). An embedded ICC profile (the APP2 segments, possibly in several
 * pieces) of the matrix/TRC or gray kind converts the pixels of grey and RGB images to sRGB ({@link ColorProfile}); other profiles
 * (look-up table profiles, CMYK) are ignored and the samples are taken as sRGB. Not supported and reported as
 * UNSUPPORTED_FORMAT: arithmetic coding, lossless and hierarchical processes, 12-bit samples, 2 components. A truncated file gives the part that
 * could be decoded (like browsers; the rest is grey); damaged headers give CORRUPT.
 * <p>
 * 마커 해석은 직접 하고 샘플 디코딩은 JRE 의 <code>javax.imageio</code> JPEG 리더에 맡기는 JPEG 디코더(계획 P18-02, P18-03; FEATURE_MATRIX
 * IMG-04). 먼저 헤더(크기, 정밀도, 방식, 성분 수, JFIF, Adobe, EXIF 세그먼트)를 직접 읽고, 디코딩 <em>전에</em> 크기 제한을 확인하며, ImageIO 에는
 * 성분 원시 샘플(<code>readRaster</code>, 색 변환 없음이므로 ICC 문제 없음)만 요청하고, 색 변환은 직접 한다.
 * <p>
 * 지원: 8비트 샘플의 baseline, extended sequential, progressive 허프만 JPEG. 성분 1개(회색), 3개(YCbCr 또는 RGB, libjpeg 와 같은 규칙: Adobe 변환
 * 0 = RGB, JFIF = YCbCr, 성분 id 가 R, G, B = RGB, 그 밖에는 YCbCr), 4개(CMYK 또는 YCCK. Adobe 표지가 있으면 Photoshop 처럼 값이 반전되어 있고
 * 없으면 일반 잉크량으로 본다). EXIF 방향(1~8)을 픽셀과 크기에 적용한다({@link ExifOrientation}). 내장 ICC 프로파일(APP2 세그먼트, 여러 조각일 수
 * 있음)이 행렬/TRC 나 회색조 종류이면 회색/RGB 이미지의 픽셀을 sRGB 로 바꾼다({@link ColorProfile}). 다른 프로파일(룩업 테이블 프로파일, CMYK)은
 * 무시하고 샘플을 sRGB 로 본다. 지원하지 않아 UNSUPPORTED_FORMAT 로
 * 보고하는 것: 산술 부호화, 무손실/계층 방식, 12비트 샘플, 성분 2개. 잘린 파일은 디코딩된 부분만 반환하고(브라우저처럼. 나머지는 회색) 헤더가
 * 손상되면 CORRUPT 이다.
 */
public final class JpegDecoder implements ImageDecoder {

    /**
     * Information read from the marker segments.
     * <p>
     * 마커 세그먼트에서 읽은 정보.
     */
    private static final class Info {
        /**
         * Frame width.
         * <p>
         * 프레임 가로.
         */
        int width;

        /**
         * Frame height.
         * <p>
         * 프레임 세로.
         */
        int height;

        /**
         * Number of components.
         * <p>
         * 성분 수.
         */
        int components;

        /**
         * Component identifiers.
         * <p>
         * 성분 식별자.
         */
        int[] ids = new int[4];

        /**
         * True if a JFIF segment was found.
         * <p>
         * JFIF 세그먼트가 있으면 true.
         */
        boolean jfif;

        /**
         * Adobe colour transform (0, 1, 2) or -1 if there is no Adobe segment.
         * <p>
         * Adobe 색 변환(0, 1, 2). Adobe 세그먼트가 없으면 -1.
         */
        int adobeTransform = -1;

        /**
         * EXIF orientation 1 to 8.
         * <p>
         * EXIF 방향 1~8.
         */
        int orientation = 1;

        /**
         * The pieces of the embedded ICC profile by sequence number (index 1 to 255), from the APP2 segments.
         * <p>
         * APP2 세그먼트에서 모은 내장 ICC 프로파일의 조각(순서 번호 1~255가 색인).
         */
        byte[][] iccPieces = new byte[256][];

        /**
         * The number of pieces that the APP2 segments announce (0 if there is no profile).
         * <p>
         * APP2 세그먼트가 알리는 조각 수(프로파일이 없으면 0).
         */
        int iccCount;

        /**
         * True if the pieces of the profile do not fit together (a missing piece, a different count).
         * <p>
         * 프로파일 조각이 서로 맞지 않으면(빠진 조각, 다른 개수) true.
         */
        boolean iccBroken;

        /**
         * The places of the ICC segments in the file: {position of the marker, end of the segment}.
         * <p>
         * 파일에서 ICC 세그먼트의 위치: {마커의 위치, 세그먼트의 끝}.
         */
        java.util.List<int[]> iccSpans = new java.util.ArrayList<int[]>();

        /**
         * Returns the file without its ICC segments. ImageIO checks the ICC segments too, even if only the raw samples are wanted, and fails
         * for pieces that do not fit together; a browser shows such a picture anyway, so the reader never sees them (this class reads the
         * profile itself).
         * <p>
         * ICC 세그먼트를 뺀 파일을 반환한다. ImageIO 는 원시 샘플만 원해도 ICC 세그먼트를 검사하고 조각이 맞지 않으면 실패한다. 브라우저는 그런
         * 그림도 보여 주므로 리더가 ICC 세그먼트를 보지 못하게 한다(프로파일은 이 클래스가 직접 읽는다).
         *
         * @param d file data / 파일 데이터
         * @return the same array if there is no ICC segment, else a copy without them / ICC 세그먼트가 없으면 같은 배열, 있으면 뺀 복사본
         */
        byte[] withoutIcc(byte[] d) {
            if (iccSpans.isEmpty()) {
                return d;
            }
            int removed = 0;
            // 빼는 바이트 수를 센다.
            for (int[] s : iccSpans) {
                removed += s[1] - s[0];
            }
            byte[] out = new byte[d.length - removed];
            int from = 0;
            int to = 0;
            // 세그먼트 사이의 구간을 차례로 복사한다.
            for (int[] s : iccSpans) {
                System.arraycopy(d, from, out, to, s[0] - from);
                to += s[0] - from;
                from = s[1];
            }
            System.arraycopy(d, from, out, to, d.length - from);
            return out;
        }

        /**
         * Joins the pieces of the profile.
         * <p>
         * 프로파일의 조각을 잇는다.
         *
         * @return the profile, or null if there is none or its pieces are incomplete / 프로파일. 없거나 조각이 모자라면 null
         */
        byte[] iccProfile() {
            if (iccCount == 0 || iccBroken) {
                return null;
            }
            int total = 0;
            // 모든 조각이 있는지 확인하며 크기를 더한다.
            for (int i = 1; i <= iccCount; i++) {
                if (iccPieces[i] == null) {
                    return null;
                }
                total += iccPieces[i].length;
            }
            byte[] out = new byte[total];
            int at = 0;
            // 순서 번호대로 이어 붙인다.
            for (int i = 1; i <= iccCount; i++) {
                System.arraycopy(iccPieces[i], 0, out, at, iccPieces[i].length);
                at += iccPieces[i].length;
            }
            return out;
        }
    }

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     */
    public JpegDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>jpeg</code>
     */
    public String getFormatName() {
        return "jpeg";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "image/jpeg", "image/jpg", "image/pjpeg" };
    }

    /**
     * Checks the start-of-image marker followed by another marker.
     * <p>
     * 이미지 시작 마커와 뒤따르는 다른 마커를 확인한다.
     *
     * @param head first bytes / 첫 바이트
     * @return true for a JPEG signature / JPEG 시그니처이면 true
     */
    public boolean accepts(byte[] head) {
        return head != null && head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
    }

    /**
     * Decodes a JPEG file.
     * <p>
     * JPEG 파일을 디코딩한다.
     *
     * @param data   file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on damaged, unsupported or too large data / 손상, 미지원, 과대 데이터일 때
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        try {
            return decodeImpl(data, limits);
        } catch (RuntimeException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged JPEG data: " + e, e);
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to decode the JPEG image");
        }
    }

    /**
     * The actual decoding.
     * <p>
     * 실제 디코딩.
     *
     * @param d      file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on failure / 실패 시
     */
    private DecodedImage decodeImpl(byte[] d, DecodeLimits limits) throws MediaException {
        if (!accepts(d)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "not a JPEG file");
        }
        if (d.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "JPEG data is too large");
        }
        Info info = readInfo(d);
        // 크기를 확인한 뒤에야 ImageIO 에게 디코딩을 시킬 수 있다.
        limits.checkImageSize(info.width, info.height);
        PixelOps.checkCancelled();
        Raster raster = readRaster(info.withoutIcc(d));
        if (raster.getWidth() != info.width || raster.getHeight() != info.height || raster.getNumBands() != info.components) {
            throw new MediaException(MediaException.Kind.CORRUPT, "the decoded JPEG does not match its header");
        }
        int w = info.width;
        int h = info.height;
        int[] argb = new int[w * h];
        convert(raster, info, argb);
        // 내장 ICC 프로파일이 있으면 sRGB 로 바꾼다. CMYK 는 룩업 테이블 프로파일이라 기존 변환을 그대로 쓴다.
        if (info.components != 4) {
            byte[] icc = info.iccProfile();
            ColorProfile profile = icc == null ? null : ColorProfile.fromIcc(icc);
            if (profile != null) {
                profile.forImage(info.components == 1).convert(argb);
            }
        }
        int[] size = new int[2];
        int[] oriented = PixelOps.orient(argb, w, h, info.orientation, size);
        return new DecodedImage(size[0], size[1], oriented, info.orientation);
    }

    /**
     * Lets ImageIO decode the raw component samples.
     * <p>
     * ImageIO 로 성분 원시 샘플을 디코딩한다.
     *
     * @param d file data / 파일 데이터
     * @return raster whose bands are the components / 성분이 밴드인 래스터
     * @throws MediaException CORRUPT if ImageIO cannot decode it / ImageIO 가 디코딩하지 못하면 CORRUPT
     */
    private static Raster readRaster(byte[] d) throws MediaException {
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("jpeg");
        if (!readers.hasNext()) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "this Java runtime has no JPEG reader");
        }
        ImageReader reader = readers.next();
        ImageInputStream in = null;
        try {
            in = new MemoryCacheImageInputStream(new ByteArrayInputStream(d));
            reader.setInput(in, true, true);
            return reader.readRaster(0, null);
        } catch (IOException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "cannot decode JPEG data: " + e.getMessage(), e);
        } finally {
            reader.dispose();
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // 이미 메모리 스트림이므로 닫기 실패는 무시한다.
                    in = null;
                }
            }
        }
    }

    /**
     * Reads the marker segments up to the first scan.
     * <p>
     * 첫 스캔까지의 마커 세그먼트를 읽는다.
     *
     * @param d file data / 파일 데이터
     * @return the collected information / 모은 정보
     * @throws MediaException CORRUPT for a broken structure, UNSUPPORTED_FORMAT for processes that are not supported / 구조가 깨졌으면 CORRUPT, 지원하지 않는 방식이면 UNSUPPORTED_FORMAT
     */
    private static Info readInfo(byte[] d) throws MediaException {
        Info info = new Info();
        boolean sawFrame = false;
        int pos = 2;
        // 마커를 하나씩 읽는다. 첫 스캔(SOS)이나 이미지 끝(EOI)을 만나면 멈춘다.
        while (pos + 1 < d.length) {
            if ((d[pos] & 0xFF) != 0xFF) {
                pos++;
                continue;
            }
            int m = d[pos + 1] & 0xFF;
            if (m == 0xFF) {
                pos++;
                continue;
            }
            pos += 2;
            if (m == 0x00 || m == 0x01 || (m >= 0xD0 && m <= 0xD8)) {
                continue;
            }
            if (m == 0xD9 || m == 0xDA) {
                break;
            }
            int len = PixelOps.u16be(d, pos);
            if (len < 2) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid JPEG segment length");
            }
            int seg = pos + 2;
            int segLen = Math.min(len - 2, d.length - seg);
            if (m >= 0xC0 && m <= 0xCF && m != 0xC4 && m != 0xC8 && m != 0xCC) {
                if (m != 0xC0 && m != 0xC1 && m != 0xC2) {
                    throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported JPEG process (marker " + Integer.toHexString(m) + ")");
                }
                if (!sawFrame) {
                    readFrame(d, seg, segLen, info);
                    sawFrame = true;
                }
            } else if (m == 0xE0 && segLen >= 5 && d[seg] == 'J' && d[seg + 1] == 'F' && d[seg + 2] == 'I' && d[seg + 3] == 'F') {
                info.jfif = true;
            } else if (m == 0xE1 && segLen >= 14 && d[seg] == 'E' && d[seg + 1] == 'x' && d[seg + 2] == 'i' && d[seg + 3] == 'f') {
                info.orientation = ExifOrientation.read(d, seg + 6, segLen - 6);
            } else if (m == 0xE2 && segLen >= 14 && isIccSegment(d, seg)) {
                // ICC 프로파일은 여러 APP2 세그먼트로 나뉘며 각각 순서 번호와 전체 개수를 가진다.
                info.iccSpans.add(new int[] { pos - 2, Math.min(d.length, pos + len) });
                int number = d[seg + 12] & 0xFF;
                int count = d[seg + 13] & 0xFF;
                if (number < 1 || count < 1 || number > count || (info.iccCount != 0 && info.iccCount != count) || info.iccPieces[number] != null) {
                    info.iccBroken = true;
                } else {
                    info.iccCount = count;
                    info.iccPieces[number] = java.util.Arrays.copyOfRange(d, seg + 14, seg + segLen);
                }
            } else if (m == 0xEE && segLen >= 12 && d[seg] == 'A' && d[seg + 1] == 'd' && d[seg + 2] == 'o' && d[seg + 3] == 'b' && d[seg + 4] == 'e') {
                info.adobeTransform = d[seg + 11] & 0xFF;
            }
            pos += len;
        }
        if (!sawFrame) {
            throw new MediaException(MediaException.Kind.CORRUPT, "JPEG frame header not found");
        }
        return info;
    }

    /**
     * Tells whether an APP2 segment carries a piece of an ICC profile: its data starts with <code>ICC_PROFILE</code> and a zero byte.
     * <p>
     * APP2 세그먼트가 ICC 프로파일 조각을 담고 있는지 알려 준다. 데이터가 <code>ICC_PROFILE</code> 과 0 바이트로 시작한다.
     *
     * @param d   file data / 파일 데이터
     * @param seg start of the segment data / 세그먼트 데이터의 시작
     * @return true for an ICC piece / ICC 조각이면 true
     */
    private static boolean isIccSegment(byte[] d, int seg) {
        byte[] id = { 'I', 'C', 'C', '_', 'P', 'R', 'O', 'F', 'I', 'L', 'E', 0 };
        // 12바이트 식별자를 하나씩 비교한다.
        for (int i = 0; i < id.length; i++) {
            if (d[seg + i] != id[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads a start-of-frame segment.
     * <p>
     * 프레임 시작 세그먼트를 읽는다.
     *
     * @param d    file data / 파일 데이터
     * @param seg  start of the segment data / 세그먼트 데이터의 시작
     * @param len  length of the segment data / 세그먼트 데이터의 길이
     * @param info receives the values / 값을 받는다
     * @throws MediaException CORRUPT or UNSUPPORTED_FORMAT / CORRUPT 또는 UNSUPPORTED_FORMAT
     */
    private static void readFrame(byte[] d, int seg, int len, Info info) throws MediaException {
        if (len < 6) {
            throw new MediaException(MediaException.Kind.CORRUPT, "JPEG frame header is too short");
        }
        int precision = d[seg] & 0xFF;
        info.height = PixelOps.u16be(d, seg + 1);
        info.width = PixelOps.u16be(d, seg + 3);
        info.components = d[seg + 5] & 0xFF;
        if (precision != 8) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported JPEG sample precision " + precision);
        }
        if (info.width == 0 || info.height == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid JPEG size");
        }
        if (info.components != 1 && info.components != 3 && info.components != 4) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported JPEG component count " + info.components);
        }
        if (len < 6 + 3 * info.components) {
            throw new MediaException(MediaException.Kind.CORRUPT, "JPEG frame header is too short");
        }
        // 성분 식별자를 읽는다.
        for (int i = 0; i < info.components; i++) {
            info.ids[i] = d[seg + 6 + 3 * i] & 0xFF;
        }
    }

    /**
     * Converts the component samples to ARGB.
     * <p>
     * 성분 샘플을 ARGB 로 변환한다.
     *
     * @param r    raster of component samples / 성분 샘플 래스터
     * @param info header information / 헤더 정보
     * @param out  pixel output / 픽셀 출력
     * @throws MediaException CANCELLED if interrupted / 인터럽트되면 CANCELLED
     */
    private static void convert(Raster r, Info info, int[] out) throws MediaException {
        int w = info.width;
        int h = info.height;
        int n = info.components;
        boolean ycc;
        if (n == 3) {
            if (info.adobeTransform >= 0) {
                ycc = info.adobeTransform != 0;
            } else if (info.jfif) {
                ycc = true;
            } else {
                ycc = !(info.ids[0] == 'R' && info.ids[1] == 'G' && info.ids[2] == 'B');
            }
        } else {
            ycc = n == 4 && info.adobeTransform == 2;
        }
        int[] row = new int[w * n];
        // 행마다 샘플을 읽어 픽셀로 바꾼다.
        for (int y = 0; y < h; y++) {
            if ((y & 31) == 0) {
                PixelOps.checkCancelled();
            }
            r.getPixels(0, y, w, 1, row);
            int o = y * w;
            if (n == 1) {
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < w; x++) {
                    int g = row[x];
                    out[o + x] = 0xFF000000 | (g << 16) | (g << 8) | g;
                }
            } else if (n == 3) {
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < w; x++) {
                    int a = row[3 * x];
                    int b = row[3 * x + 1];
                    int c = row[3 * x + 2];
                    out[o + x] = ycc ? yccToRgb(a, b, c) : (0xFF000000 | (a << 16) | (b << 8) | c);
                }
            } else {
                boolean inverted = info.adobeTransform >= 0;
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < w; x++) {
                    int c0 = row[4 * x];
                    int c1 = row[4 * x + 1];
                    int c2 = row[4 * x + 2];
                    int k = row[4 * x + 3];
                    int rr;
                    int gg;
                    int bb;
                    if (ycc) {
                        int rgb = yccToRgb(c0, c1, c2);
                        // YCCK 는 YCC 를 RGB 로 바꾼 값의 보수가 잉크 값이다(Adobe 는 반전되어 있다).
                        c0 = 255 - ((rgb >> 16) & 0xFF);
                        c1 = 255 - ((rgb >> 8) & 0xFF);
                        c2 = 255 - (rgb & 0xFF);
                    }
                    if (inverted) {
                        rr = c0 * k / 255;
                        gg = c1 * k / 255;
                        bb = c2 * k / 255;
                    } else {
                        rr = (255 - c0) * (255 - k) / 255;
                        gg = (255 - c1) * (255 - k) / 255;
                        bb = (255 - c2) * (255 - k) / 255;
                    }
                    out[o + x] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
                }
            }
        }
    }

    /**
     * Converts a JFIF YCbCr triple to an opaque RGB pixel (the libjpeg integer formulas).
     * <p>
     * JFIF YCbCr 세 값을 불투명 RGB 픽셀로 바꾼다(libjpeg 의 정수 공식).
     *
     * @param y  luma / 휘도
     * @param cb blue chroma / 청색 색차
     * @param cr red chroma / 적색 색차
     * @return ARGB pixel / ARGB 픽셀
     */
    static int yccToRgb(int y, int cb, int cr) {
        int r = y + ((91881 * (cr - 128) + 32768) >> 16);
        int g = y + ((-22554 * (cb - 128) - 46802 * (cr - 128) + 32768) >> 16);
        int b = y + ((116130 * (cb - 128) + 32768) >> 16);
        return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /**
     * Clamps a value to 0 to 255.
     * <p>
     * 값을 0~255 로 제한한다.
     *
     * @param v value / 값
     * @return clamped value / 제한된 값
     */
    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }
}
