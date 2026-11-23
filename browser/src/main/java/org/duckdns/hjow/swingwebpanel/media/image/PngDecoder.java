package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;

/**
 * A pure Java decoder for PNG and APNG (plan items P18-02, P18-03; FEATURE_MATRIX IMG-01, IMG-02).
 * <p>
 * Supported: all colour types (0, 2, 3, 4, 6) with bit depths 1, 2, 4, 8, 16 as allowed by the specification, Adam7 interlacing, a palette
 * with <code>tRNS</code> transparency for every colour type, and APNG animation (<code>acTL</code>, <code>fcTL</code>, <code>fdAT</code>,
 * dispose and blend operations, composed full-canvas frames). 16-bit samples are reduced to their high byte. The orientation of an
 * <code>eXIf</code> chunk is applied ({@link ExifOrientation}) and the colour chunks <code>cICP</code>, <code>iCCP</code>, <code>sRGB</code>,
 * <code>gAMA</code> and <code>cHRM</code> are applied in the order of precedence of the PNG specification, third edition: the pixels are
 * converted to sRGB with {@link ColorProfile} (an <code>iCCP</code> profile that is not of the matrix/TRC or gray kinds is ignored and the
 * samples stay sRGB); other ancillary chunks are ignored. Critical chunks with a wrong CRC make the image CORRUPT; ancillary chunks with a wrong
 * CRC are skipped. Missing data in the image stream makes the image CORRUPT; a missing <code>IEND</code> is tolerated. A palette index that
 * is outside the palette gives opaque black. If the APNG part is inconsistent (wrong sequence numbers, frames outside the canvas, damaged
 * frame data) the default image is returned as a still image. Frame delays of 10 ms or less are replaced by 100 ms like browsers do.
 * <p>
 * PNG 와 APNG 를 위한 순수 Java 디코더(계획 P18-02, P18-03; FEATURE_MATRIX IMG-01, IMG-02).
 * <p>
 * 지원: 규격이 허용하는 모든 색 유형(0, 2, 3, 4, 6)과 비트 심도(1, 2, 4, 8, 16), Adam7 인터레이스, 모든 색 유형의 <code>tRNS</code> 투명도,
 * APNG 애니메이션(<code>acTL</code>, <code>fcTL</code>, <code>fdAT</code>, dispose/blend 연산, 합성된 전체 캔버스 프레임).
 * 16비트 샘플은 상위 바이트만 쓴다. <code>eXIf</code> 청크의 방향을 적용하고({@link ExifOrientation}) 색 청크 <code>cICP</code>,
 * <code>iCCP</code>, <code>sRGB</code>, <code>gAMA</code>, <code>cHRM</code> 을 PNG 규격 3판의 우선순위대로 적용한다: {@link ColorProfile} 로 픽셀을
 * sRGB 로 바꾼다(행렬/TRC 나 회색조가 아닌 <code>iCCP</code> 프로파일은 무시하고 샘플을 sRGB 로 남긴다). 다른 보조 청크는 무시한다.
 * CRC 가 틀린 필수 청크는 이미지를 CORRUPT 로 만들고 CRC 가 틀린 보조 청크는 건너뛴다. 이미지 스트림에
 * 데이터가 모자라면 CORRUPT 이며 <code>IEND</code> 가 없는 것은 허용한다. 팔레트 밖의 인덱스는 불투명 검정이다. APNG 부분이 일관되지 않으면(잘못된
 * 순서 번호, 캔버스 밖 프레임, 손상된 프레임 데이터) 기본 이미지를 정지 이미지로 반환한다. 10 ms 이하의 프레임 지연은 브라우저처럼 100 ms 로 바꾼다.
 */
public final class PngDecoder implements ImageDecoder {

    /**
     * The 8-byte PNG signature.
     * <p>
     * 8바이트 PNG 시그니처.
     */
    private static final byte[] SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };

    /**
     * Chunk type IHDR.
     * <p>
     * 청크 유형 IHDR.
     */
    private static final int IHDR = 0x49484452;

    /**
     * Chunk type PLTE.
     * <p>
     * 청크 유형 PLTE.
     */
    private static final int PLTE = 0x504C5445;

    /**
     * Chunk type IDAT.
     * <p>
     * 청크 유형 IDAT.
     */
    private static final int IDAT = 0x49444154;

    /**
     * Chunk type IEND.
     * <p>
     * 청크 유형 IEND.
     */
    private static final int IEND = 0x49454E44;

    /**
     * Chunk type tRNS.
     * <p>
     * 청크 유형 tRNS.
     */
    private static final int TRNS = 0x74524E53;

    /**
     * Chunk type acTL (APNG animation control).
     * <p>
     * 청크 유형 acTL(APNG 애니메이션 제어).
     */
    private static final int ACTL = 0x6163544C;

    /**
     * Chunk type fcTL (APNG frame control).
     * <p>
     * 청크 유형 fcTL(APNG 프레임 제어).
     */
    private static final int FCTL = 0x6663544C;

    /**
     * Chunk type fdAT (APNG frame data).
     * <p>
     * 청크 유형 fdAT(APNG 프레임 데이터).
     */
    private static final int FDAT = 0x66644154;

    /**
     * Chunk type eXIf (Exif metadata, PNG 3rd edition section 11.3.4.6): a TIFF structure that carries the orientation.
     * <p>
     * 청크 유형 eXIf(Exif 메타데이터, PNG 3판 11.3.4.6절): 방향을 담은 TIFF 구조.
     */
    private static final int EXIF = 0x65584966;

    /**
     * Chunk type sRGB (the image is sRGB).
     * <p>
     * 청크 유형 sRGB(이미지가 sRGB 이다).
     */
    private static final int SRGB_CHUNK = 0x73524742;

    /**
     * Chunk type gAMA (the file gamma).
     * <p>
     * 청크 유형 gAMA(파일 감마).
     */
    private static final int GAMA = 0x67414D41;

    /**
     * Chunk type cHRM (the chromaticities of the primaries and the white point).
     * <p>
     * 청크 유형 cHRM(원색과 백색점의 색도 좌표).
     */
    private static final int CHRM = 0x6348524D;

    /**
     * Chunk type cICP (coding-independent code points of ITU-T H.273).
     * <p>
     * 청크 유형 cICP(ITU-T H.273 의 코딩 독립 코드 값).
     */
    private static final int CICP = 0x63494350;

    /**
     * Chunk type iCCP (an embedded ICC profile).
     * <p>
     * 청크 유형 iCCP(내장 ICC 프로파일).
     */
    private static final int ICCP = 0x69434350;

    /**
     * The largest ICC profile that is inflated from an <code>iCCP</code> chunk (a larger one is ignored).
     * <p>
     * <code>iCCP</code> 청크에서 압축을 푸는 ICC 프로파일의 최대 크기(더 크면 무시한다).
     */
    private static final int MAX_PROFILE_BYTES = 8 * 1024 * 1024;

    /**
     * Adam7 pass start column.
     * <p>
     * Adam7 단계의 시작 열.
     */
    private static final int[] ADAM_X0 = { 0, 4, 0, 2, 0, 1, 0 };

    /**
     * Adam7 pass start row.
     * <p>
     * Adam7 단계의 시작 행.
     */
    private static final int[] ADAM_Y0 = { 0, 0, 4, 0, 2, 0, 1 };

    /**
     * Adam7 pass column step.
     * <p>
     * Adam7 단계의 열 간격.
     */
    private static final int[] ADAM_DX = { 8, 8, 4, 4, 2, 2, 1 };

    /**
     * Adam7 pass row step.
     * <p>
     * Adam7 단계의 행 간격.
     */
    private static final int[] ADAM_DY = { 8, 8, 8, 4, 4, 2, 2 };

    /**
     * APNG dispose operation: leave the canvas as it is.
     * <p>
     * APNG 처리 방법: 캔버스를 그대로 둔다.
     */
    private static final int DISPOSE_NONE = 0;

    /**
     * APNG dispose operation: clear the frame region to transparent black.
     * <p>
     * APNG 처리 방법: 프레임 영역을 투명한 검정으로 지운다.
     */
    private static final int DISPOSE_BACKGROUND = 1;

    /**
     * APNG dispose operation: restore the canvas to its state before the frame.
     * <p>
     * APNG 처리 방법: 캔버스를 프레임을 그리기 전 상태로 되돌린다.
     */
    private static final int DISPOSE_PREVIOUS = 2;

    /**
     * Replacement delay for frames whose delay is 10 ms or less.
     * <p>
     * 지연이 10 ms 이하인 프레임에 쓰는 대체 지연.
     */
    private static final int MIN_DELAY_REPLACEMENT_MS = 100;

    /**
     * A growable list of (offset, length) pairs that describe the pieces of a compressed image stream inside the file array.
     * <p>
     * 파일 배열 안에서 압축된 이미지 스트림 조각을 나타내는 (위치, 길이) 쌍의 늘어나는 목록.
     */
    private static final class Segments {
        /**
         * Offsets of the pieces in the file array.
         * <p>
         * 위치들.
         */
        int[] off = new int[4];

        /**
         * Lengths of the pieces in bytes.
         * <p>
         * 길이들.
         */
        int[] len = new int[4];

        /**
         * Number of pieces.
         * <p>
         * 조각 수.
         */
        int count;

        /**
         * Adds a piece.
         * <p>
         * 조각을 추가한다.
         *
         * @param o offset / 위치
         * @param l length / 길이
         */
        void add(int o, int l) {
            if (count == off.length) {
                off = java.util.Arrays.copyOf(off, count * 2);
                len = java.util.Arrays.copyOf(len, count * 2);
            }
            off[count] = o;
            len[count] = l;
            count++;
        }
    }

    /**
     * One APNG frame description with the location of its data.
     * <p>
     * 데이터 위치를 포함한 APNG 프레임 하나의 설명.
     */
    private static final class Frame {
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
         * Horizontal offset on the canvas.
         * <p>
         * 캔버스에서의 가로 위치.
         */
        int x;

        /**
         * Vertical offset on the canvas.
         * <p>
         * 캔버스에서의 세로 위치.
         */
        int y;

        /**
         * Delay numerator.
         * <p>
         * 지연 분자.
         */
        int delayNum;

        /**
         * Delay denominator (0 means 100).
         * <p>
         * 지연 분모(0 이면 100).
         */
        int delayDen;

        /**
         * Dispose operation.
         * <p>
         * 처리 방법.
         */
        int dispose;

        /**
         * Blend operation (0 source, 1 over).
         * <p>
         * 합성 방법(0 source, 1 over).
         */
        int blend;

        /**
         * Compressed data pieces of the frame (the IDAT pieces for a frame that is the default image).
         * <p>
         * 프레임의 압축 데이터 조각(기본 이미지인 프레임은 IDAT 조각).
         */
        Segments data = new Segments();
    }

    /**
     * Everything found while scanning the chunks.
     * <p>
     * 청크를 훑으며 찾은 모든 정보.
     */
    private static final class Parsed {
        /**
         * Image width.
         * <p>
         * 이미지 가로.
         */
        int width;

        /**
         * Image height.
         * <p>
         * 이미지 세로.
         */
        int height;

        /**
         * Bit depth.
         * <p>
         * 비트 심도.
         */
        int depth;

        /**
         * Colour type.
         * <p>
         * 색 유형.
         */
        int colorType;

        /**
         * Interlace method (0 none, 1 Adam7).
         * <p>
         * 인터레이스 방식(0 없음, 1 Adam7).
         */
        int interlace;

        /**
         * Bits per pixel of the raw data.
         * <p>
         * 원시 데이터의 픽셀당 비트 수.
         */
        int bitsPerPixel;

        /**
         * Palette as RGB triples, or null.
         * <p>
         * RGB 3개씩의 팔레트. 없으면 null.
         */
        byte[] palette;

        /**
         * tRNS data, or null.
         * <p>
         * tRNS 데이터. 없으면 null.
         */
        byte[] trns;

        /**
         * Pieces of the default image stream (IDAT).
         * <p>
         * 기본 이미지 스트림(IDAT)의 조각.
         */
        Segments idat = new Segments();

        /**
         * True if an acTL chunk was found before the image data.
         * <p>
         * 이미지 데이터 앞에서 acTL 청크를 찾았으면 true.
         */
        boolean hasActl;

        /**
         * Number of plays from acTL (0 forever).
         * <p>
         * acTL 의 재생 횟수(0 이면 무한).
         */
        int numPlays;

        /**
         * APNG frames in order.
         * <p>
         * 순서대로의 APNG 프레임.
         */
        List<Frame> frames = new ArrayList<Frame>();

        /**
         * True if the default image is the first animation frame.
         * <p>
         * 기본 이미지가 첫 애니메이션 프레임이면 true.
         */
        boolean defaultIsFrame;

        /**
         * True if the APNG chunks are inconsistent.
         * <p>
         * APNG 청크가 일관되지 않으면 true.
         */
        boolean apngBroken;

        /**
         * The EXIF orientation (1 to 8) from the first <code>eXIf</code> chunk, 1 if there is none.
         * <p>
         * 첫 <code>eXIf</code> 청크의 EXIF 방향(1~8). 없으면 1.
         */
        int orientation = 1;

        /**
         * True once an <code>eXIf</code> chunk was read (only the first one counts).
         * <p>
         * <code>eXIf</code> 청크를 읽었으면 true(첫 청크만 인정한다).
         */
        boolean sawExif;

        /**
         * The ICC profile of an <code>iCCP</code> chunk (inflated), or null.
         * <p>
         * <code>iCCP</code> 청크의 ICC 프로파일(압축을 푼 것). 없으면 null.
         */
        byte[] iccp;

        /**
         * True if there is an <code>sRGB</code> chunk.
         * <p>
         * <code>sRGB</code> 청크가 있으면 true.
         */
        boolean srgb;

        /**
         * The file gamma of a <code>gAMA</code> chunk (the value divided by 100000), or 0 if there is none.
         * <p>
         * <code>gAMA</code> 청크의 파일 감마(값을 100000 으로 나눈 것). 없으면 0.
         */
        double gamma;

        /**
         * The chromaticities of a <code>cHRM</code> chunk {white x, y, red x, y, green x, y, blue x, y}, or null.
         * <p>
         * <code>cHRM</code> 청크의 색도 좌표 {백색 x, y, 빨강 x, y, 초록 x, y, 파랑 x, y}. 없으면 null.
         */
        double[] chrm;

        /**
         * The code points {primaries, transfer} of a <code>cICP</code> chunk, or null.
         * <p>
         * <code>cICP</code> 청크의 코드 값 {원색, 전달 특성}. 없으면 null.
         */
        int[] cicp;
    }

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     */
    public PngDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>png</code>
     */
    public String getFormatName() {
        return "png";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "image/png", "image/apng", "image/x-png" };
    }

    /**
     * Checks the PNG signature.
     * <p>
     * PNG 시그니처를 확인한다.
     *
     * @param head first bytes / 첫 바이트
     * @return true for a PNG signature / PNG 시그니처이면 true
     */
    public boolean accepts(byte[] head) {
        if (head == null || head.length < SIGNATURE.length) {
            return false;
        }
        // 시그니처 8바이트를 모두 비교한다.
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (head[i] != SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Decodes a PNG or APNG file.
     * <p>
     * PNG 또는 APNG 파일을 디코딩한다.
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
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged PNG data: " + e, e);
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to decode the PNG image");
        }
    }

    /**
     * The actual decoding.
     * <p>
     * 실제 디코딩.
     *
     * @param data   file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on failure / 실패 시
     */
    private DecodedImage decodeImpl(byte[] data, DecodeLimits limits) throws MediaException {
        if (!accepts(data)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "not a PNG file");
        }
        if (data.length > limits.getMaxBytes()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "PNG data is too large");
        }
        Parsed p = parse(data, limits);
        boolean animated = p.hasActl && !p.apngBroken && p.frames.size() > 0;
        if (animated) {
            try {
                // 색 관리(sRGB 로 변환)를 한 뒤 방향을 적용한다.
                return ExifOrientation.apply(colorProfile(p).convert(compose(data, p, limits)), p.orientation);
            } catch (MediaException e) {
                // 취소와 크기 초과는 그대로 전달하고 그 밖의 애니메이션 오류는 기본 이미지로 대체한다.
                if (e.getKind() == MediaException.Kind.CANCELLED || e.getKind() == MediaException.Kind.TOO_LARGE) {
                    throw e;
                }
            }
        }
        int[] argb = new int[p.width * p.height];
        decodePixels(data, p, p.idat, p.width, p.height, argb);
        return ExifOrientation.apply(colorProfile(p).convert(new DecodedImage(p.width, p.height, argb)), p.orientation);
    }

    /**
     * Chooses the colour space of the image from its colour chunks. The order of precedence is the one of the PNG specification (third
     * edition, section 11.3.2): <code>cICP</code>, then <code>iCCP</code>, then <code>sRGB</code>, then <code>gAMA</code> with
     * <code>cHRM</code>; a chunk whose content cannot be used (an unsupported profile, an unknown code point) is passed over. Without any of
     * them, and for a gray profile on a colour image, the samples are sRGB.
     * <p>
     * 이미지의 색 공간을 색 관련 청크에서 고른다. 우선순위는 PNG 규격(3판, 11.3.2절)의 것이다: <code>cICP</code>, <code>iCCP</code>, <code>sRGB</code>,
     * <code>gAMA</code> 와 <code>cHRM</code>. 내용을 쓸 수 없는 청크(지원하지 않는 프로파일, 모르는 코드 값)는 건너뛴다. 아무것도 없거나 컬러
     * 이미지에 회색조 프로파일이 있으면 샘플은 sRGB 이다.
     *
     * @param p the parsed chunks / 해석한 청크
     * @return the conversion to sRGB, never null / sRGB 로의 변환. null 이 아님
     */
    private static ColorProfile colorProfile(Parsed p) {
        boolean gray = p.colorType == 0 || p.colorType == 4;
        ColorProfile cp;
        if (p.cicp != null) {
            cp = ColorProfile.fromCicp(p.cicp[0], p.cicp[1]);
            if (cp != null) {
                return cp;
            }
        }
        if (p.iccp != null) {
            cp = ColorProfile.fromIcc(p.iccp);
            if (cp != null) {
                return cp.forImage(gray);
            }
        }
        if (p.srgb) {
            return ColorProfile.SRGB;
        }
        if (p.gamma > 0 || p.chrm != null) {
            cp = ColorProfile.fromGamma(p.gamma, p.chrm);
            if (cp != null) {
                return cp;
            }
        }
        return ColorProfile.SRGB;
    }

    /**
     * Scans the chunks and collects header, palette, transparency and the locations of the image data.
     * <p>
     * 청크를 훑어 헤더, 팔레트, 투명도, 이미지 데이터의 위치를 모은다.
     *
     * @param d      file data / 파일 데이터
     * @param limits limits / 제한
     * @return collected information / 모은 정보
     * @throws MediaException CORRUPT for an invalid structure / 구조가 올바르지 않으면 CORRUPT
     */
    private static Parsed parse(byte[] d, DecodeLimits limits) throws MediaException {
        Parsed p = new Parsed();
        int pos = SIGNATURE.length;
        boolean sawIhdr = false;
        boolean sawIdat = false;
        boolean idatClosed = false;
        int expectedSeq = 0;
        Frame current = null;
        CRC32 crc = new CRC32();
        // 청크를 하나씩 읽는다: 길이(4) 유형(4) 데이터 CRC(4).
        while (pos + 8 <= d.length) {
            long declared = PixelOps.u32be(d, pos);
            int type = (int) PixelOps.u32be(d, pos + 4);
            int start = pos + 8;
            boolean truncated = declared > (long) d.length - start;
            int len = truncated ? d.length - start : (int) declared;
            boolean critical = (type & 0x20000000) == 0;
            boolean crcOk = true;
            // 온전한 청크는 CRC 를 확인한다.
            if (!truncated && (long) start + len + 4 <= d.length) {
                crc.reset();
                crc.update(d, pos + 4, 4 + len);
                crcOk = crc.getValue() == PixelOps.u32be(d, start + len);
            } else if (!truncated) {
                truncated = true;
            }
            if (!crcOk && critical) {
                throw new MediaException(MediaException.Kind.CORRUPT, "critical PNG chunk has a wrong CRC");
            }
            if (!sawIhdr && type != IHDR) {
                throw new MediaException(MediaException.Kind.CORRUPT, "IHDR must be the first chunk");
            }
            if (crcOk) {
                switch (type) {
                case IHDR:
                    if (sawIhdr || len != 13 || truncated) {
                        throw new MediaException(MediaException.Kind.CORRUPT, "invalid IHDR chunk");
                    }
                    readHeader(d, start, p, limits);
                    sawIhdr = true;
                    break;
                case PLTE:
                    if (!sawIdat && len > 0 && len % 3 == 0 && len <= 768) {
                        p.palette = java.util.Arrays.copyOfRange(d, start, start + len);
                    } else if (!sawIdat && p.colorType == 3) {
                        throw new MediaException(MediaException.Kind.CORRUPT, "invalid PLTE chunk");
                    }
                    break;
                case TRNS:
                    if (!sawIdat && !truncated) {
                        p.trns = java.util.Arrays.copyOfRange(d, start, start + len);
                    }
                    break;
                case IDAT:
                    if (!idatClosed) {
                        sawIdat = true;
                        p.idat.add(start, len);
                        if (current != null && p.defaultIsFrame) {
                            current.data.add(start, len);
                        }
                    }
                    break;
                case SRGB_CHUNK:
                    // 색 관련 청크는 팔레트와 이미지 데이터 앞에 있어야 하며 길이가 맞고 처음 나온 것만 인정한다.
                    if (!sawIdat && !truncated && len == 1) {
                        p.srgb = true;
                    }
                    break;
                case GAMA:
                    if (!sawIdat && !truncated && len == 4 && p.gamma == 0) {
                        p.gamma = PixelOps.u32be(d, start) / 100000.0;
                    }
                    break;
                case CHRM:
                    if (!sawIdat && !truncated && len == 32 && p.chrm == null) {
                        double[] c = new double[8];
                        // 여덟 값(백색점, 빨강, 초록, 파랑의 x, y)을 100000 으로 나눈다.
                        for (int i = 0; i < 8; i++) {
                            c[i] = PixelOps.u32be(d, start + 4 * i) / 100000.0;
                        }
                        p.chrm = c;
                    }
                    break;
                case CICP:
                    if (!sawIdat && !truncated && len == 4 && p.cicp == null && (d[start + 2] & 0xFF) == 0) {
                        p.cicp = new int[] { d[start] & 0xFF, d[start + 1] & 0xFF };
                    }
                    break;
                case ICCP:
                    if (!sawIdat && !truncated && p.iccp == null) {
                        p.iccp = readIccp(d, start, len);
                    }
                    break;
                case EXIF:
                    // 방향은 첫 eXIf 청크의 것만 쓴다(이 청크는 IHDR 뒤라면 IDAT 앞뒤 어디에나 올 수 있다).
                    if (!p.sawExif && !truncated) {
                        p.sawExif = true;
                        p.orientation = ExifOrientation.read(d, start, len);
                    }
                    break;
                case ACTL:
                    if (!sawIdat && len == 8 && !truncated && !p.hasActl) {
                        long frames = PixelOps.u32be(d, start);
                        p.numPlays = (int) Math.min(PixelOps.u32be(d, start + 4), Integer.MAX_VALUE);
                        p.hasActl = frames > 0;
                    }
                    break;
                case FCTL:
                    if (p.hasActl && len == 26 && !truncated) {
                        long seq = PixelOps.u32be(d, start);
                        if (seq != expectedSeq) {
                            p.apngBroken = true;
                        }
                        expectedSeq++;
                        current = new Frame();
                        current.width = (int) Math.min(PixelOps.u32be(d, start + 4), Integer.MAX_VALUE);
                        current.height = (int) Math.min(PixelOps.u32be(d, start + 8), Integer.MAX_VALUE);
                        current.x = (int) Math.min(PixelOps.u32be(d, start + 12), Integer.MAX_VALUE);
                        current.y = (int) Math.min(PixelOps.u32be(d, start + 16), Integer.MAX_VALUE);
                        current.delayNum = PixelOps.u16be(d, start + 20);
                        current.delayDen = PixelOps.u16be(d, start + 22);
                        current.dispose = d[start + 24] & 0xFF;
                        current.blend = d[start + 25] & 0xFF;
                        if (current.dispose > 2 || current.blend > 1) {
                            p.apngBroken = true;
                        }
                        // IDAT 앞의 fcTL 이면 기본 이미지가 첫 프레임이다.
                        if (!sawIdat) {
                            if (p.frames.size() == 0) {
                                p.defaultIsFrame = true;
                            } else {
                                p.apngBroken = true;
                            }
                        }
                        p.frames.add(current);
                    } else if (p.hasActl) {
                        p.apngBroken = true;
                    }
                    break;
                case FDAT:
                    if (p.hasActl && current != null && len >= 4 && sawIdat) {
                        long seq = PixelOps.u32be(d, start);
                        if (seq != expectedSeq) {
                            p.apngBroken = true;
                        }
                        expectedSeq++;
                        current.data.add(start + 4, len - 4);
                    } else if (p.hasActl) {
                        p.apngBroken = true;
                    }
                    break;
                default:
                    break;
                }
            }
            if (type != IDAT && sawIdat) {
                idatClosed = true;
            }
            if (type == IEND && crcOk) {
                break;
            }
            if (truncated) {
                break;
            }
            pos = start + len + 4;
        }
        if (!sawIhdr) {
            throw new MediaException(MediaException.Kind.CORRUPT, "missing IHDR chunk");
        }
        if (!sawIdat) {
            throw new MediaException(MediaException.Kind.CORRUPT, "missing image data");
        }
        if (p.colorType == 3 && p.palette == null) {
            throw new MediaException(MediaException.Kind.CORRUPT, "missing palette");
        }
        return p;
    }

    /**
     * Reads the profile of an <code>iCCP</code> chunk: a profile name of 1 to 79 characters, a zero byte, the compression method (0, zlib)
     * and the zlib stream of the profile. Anything damaged, too large or of another compression method gives null (the chunk is ignored).
     * <p>
     * <code>iCCP</code> 청크의 프로파일을 읽는다: 1~79 글자의 프로파일 이름, 0 바이트, 압축 방식(0, zlib), 프로파일의 zlib 스트림. 손상되었거나
     * 너무 크거나 다른 압축 방식이면 null 을 반환한다(청크를 무시한다).
     *
     * @param d     file data / 파일 데이터
     * @param start start of the chunk data / 청크 데이터의 시작
     * @param len   length of the chunk data / 청크 데이터의 길이
     * @return the profile bytes, or null / 프로파일 바이트. 없으면 null
     */
    private static byte[] readIccp(byte[] d, int start, int len) {
        int nameEnd = start;
        int end = start + len;
        // 이름은 0 바이트로 끝나며 79 글자를 넘지 않는다.
        while (nameEnd < end && d[nameEnd] != 0 && nameEnd - start <= 79) {
            nameEnd++;
        }
        if (nameEnd >= end || d[nameEnd] != 0 || nameEnd == start || nameEnd + 2 > end || d[nameEnd + 1] != 0) {
            return null;
        }
        Inflater inflater = new Inflater();
        try {
            inflater.setInput(d, nameEnd + 2, end - nameEnd - 2);
            byte[] out = new byte[4096];
            int size = 0;
            // 압축을 풀며 출력 상한을 넘으면 중단한다.
            while (!inflater.finished()) {
                if (size == out.length) {
                    if (size >= MAX_PROFILE_BYTES) {
                        return null;
                    }
                    out = java.util.Arrays.copyOf(out, Math.min(out.length * 2, MAX_PROFILE_BYTES));
                }
                int n = inflater.inflate(out, size, out.length - size);
                if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
                    break;
                }
                size += n;
            }
            return inflater.finished() ? java.util.Arrays.copyOf(out, size) : null;
        } catch (DataFormatException e) {
            return null;
        } finally {
            inflater.end();
        }
    }

    /**
     * Reads and validates the IHDR chunk.
     * <p>
     * IHDR 청크를 읽고 검증한다.
     *
     * @param d      file data / 파일 데이터
     * @param s      start of the chunk data / 청크 데이터의 시작
     * @param p      receives the values / 값을 받는다
     * @param limits limits / 제한
     * @throws MediaException CORRUPT for invalid values, TOO_LARGE for a large size / 값이 잘못되면 CORRUPT, 크기가 크면 TOO_LARGE
     */
    private static void readHeader(byte[] d, int s, Parsed p, DecodeLimits limits) throws MediaException {
        long w = PixelOps.u32be(d, s);
        long h = PixelOps.u32be(d, s + 4);
        int depth = d[s + 8] & 0xFF;
        int ct = d[s + 9] & 0xFF;
        int compression = d[s + 10] & 0xFF;
        int filter = d[s + 11] & 0xFF;
        int interlace = d[s + 12] & 0xFF;
        if (compression != 0 || filter != 0 || interlace > 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "unknown PNG compression, filter or interlace method");
        }
        boolean ok;
        switch (ct) {
        case 0:
            ok = depth == 1 || depth == 2 || depth == 4 || depth == 8 || depth == 16;
            break;
        case 2:
        case 4:
        case 6:
            ok = depth == 8 || depth == 16;
            break;
        case 3:
            ok = depth == 1 || depth == 2 || depth == 4 || depth == 8;
            break;
        default:
            ok = false;
            break;
        }
        if (!ok) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid PNG colour type " + ct + " / bit depth " + depth);
        }
        // 크기를 확인한 뒤에야 버퍼를 할당할 수 있다.
        limits.checkImageSize(w, h);
        p.width = (int) w;
        p.height = (int) h;
        p.depth = depth;
        p.colorType = ct;
        p.interlace = interlace;
        int channels = ct == 0 ? 1 : ct == 2 ? 3 : ct == 3 ? 1 : ct == 4 ? 2 : 4;
        p.bitsPerPixel = channels * depth;
    }

    /**
     * Composes the animation frames onto a canvas.
     * <p>
     * 애니메이션 프레임을 캔버스에 합성한다.
     *
     * @param d      file data / 파일 데이터
     * @param p      parsed information / 해석한 정보
     * @param limits limits / 제한
     * @return animated image / 애니메이션 이미지
     * @throws MediaException on inconsistent or damaged frames / 프레임이 일관되지 않거나 손상되었을 때
     */
    private static DecodedImage compose(byte[] d, Parsed p, DecodeLimits limits) throws MediaException {
        int n = p.frames.size();
        if (n > limits.getMaxFrames() || (long) n * p.width * p.height > limits.getMaxTotalPixels()) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "the animation has too many frames or pixels");
        }
        int cw = p.width;
        int ch = p.height;
        int[] canvas = new int[cw * ch];
        int[] saved = null;
        Frame prev = null;
        int prevDispose = DISPOSE_NONE;
        List<DecodedFrame> out = new ArrayList<DecodedFrame>(n);
        // 프레임을 순서대로 그린다.
        for (int i = 0; i < n; i++) {
            PixelOps.checkCancelled();
            Frame f = p.frames.get(i);
            if (f.width < 1 || f.height < 1 || (long) f.x + f.width > cw || (long) f.y + f.height > ch) {
                throw new MediaException(MediaException.Kind.CORRUPT, "APNG frame lies outside the canvas");
            }
            if (i == 0 && p.defaultIsFrame && (f.width != cw || f.height != ch || f.x != 0 || f.y != 0)) {
                throw new MediaException(MediaException.Kind.CORRUPT, "the first APNG frame must cover the canvas");
            }
            // 직전 프레임의 처리 방법을 적용한다.
            if (prev != null) {
                if (prevDispose == DISPOSE_BACKGROUND) {
                    clearRegion(canvas, cw, prev);
                } else if (prevDispose == DISPOSE_PREVIOUS && saved != null) {
                    System.arraycopy(saved, 0, canvas, 0, canvas.length);
                }
            }
            // 첫 프레임의 PREVIOUS 는 BACKGROUND 와 같게 다룬다.
            int dispose = (i == 0 && f.dispose == DISPOSE_PREVIOUS) ? DISPOSE_BACKGROUND : f.dispose;
            saved = dispose == DISPOSE_PREVIOUS ? canvas.clone() : null;
            limits.checkImageSize(f.width, f.height);
            int[] fp = new int[f.width * f.height];
            decodePixels(d, p, f.data, f.width, f.height, fp);
            // 프레임 픽셀을 캔버스의 해당 영역에 합성한다.
            for (int y = 0; y < f.height; y++) {
                int di = (f.y + y) * cw + f.x;
                int si = y * f.width;
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < f.width; x++) {
                    canvas[di + x] = f.blend == 1 ? PixelOps.over(fp[si + x], canvas[di + x]) : fp[si + x];
                }
            }
            out.add(new DecodedFrame(canvas.clone(), delayMillis(f)));
            prev = f;
            prevDispose = dispose;
        }
        return new DecodedImage(cw, ch, out, p.numPlays);
    }

    /**
     * Clears a frame region of the canvas to transparent black.
     * <p>
     * 캔버스의 프레임 영역을 투명한 검정으로 지운다.
     *
     * @param canvas canvas pixels / 캔버스 픽셀
     * @param cw     canvas width / 캔버스 가로
     * @param f      the frame / 프레임
     */
    private static void clearRegion(int[] canvas, int cw, Frame f) {
        // 프레임이 차지한 행마다 영역을 0 으로 채운다.
        for (int y = 0; y < f.height; y++) {
            int di = (f.y + y) * cw + f.x;
            java.util.Arrays.fill(canvas, di, di + f.width, 0);
        }
    }

    /**
     * Converts the delay of a frame to milliseconds (a denominator of 0 means 100; 10 ms or less becomes 100 ms).
     * <p>
     * 프레임 지연을 밀리초로 바꾼다(분모 0 은 100 으로 본다. 10 ms 이하는 100 ms 가 된다).
     *
     * @param f the frame / 프레임
     * @return delay in milliseconds / 지연(밀리초)
     */
    private static int delayMillis(Frame f) {
        int den = f.delayDen == 0 ? 100 : f.delayDen;
        long ms = (long) f.delayNum * 1000L / den;
        if (ms <= 10) {
            return MIN_DELAY_REPLACEMENT_MS;
        }
        return (int) Math.min(ms, Integer.MAX_VALUE);
    }

    /**
     * Inflates one image stream and writes the pixels (all Adam7 passes when interlaced).
     * <p>
     * 이미지 스트림 하나를 풀어 픽셀을 쓴다(인터레이스이면 Adam7 의 모든 단계).
     *
     * @param d    file data / 파일 데이터
     * @param p    parsed information / 해석한 정보
     * @param segs pieces of the compressed stream / 압축 스트림의 조각
     * @param w    width of this image or frame / 이 이미지 또는 프레임의 가로
     * @param h    height / 세로
     * @param out  pixel output of w times h entries / w × h 개의 픽셀 출력
     * @throws MediaException CORRUPT if the stream is damaged or too short / 스트림이 손상되었거나 짧으면 CORRUPT
     */
    private static void decodePixels(byte[] d, Parsed p, Segments segs, int w, int h, int[] out) throws MediaException {
        ZStream z = new ZStream(d, segs);
        try {
            int[] palette = buildPalette(p);
            if (p.interlace == 0) {
                decodePass(z, p, palette, w, h, 0, 0, 1, 1, w, out);
            } else {
                // Adam7 의 일곱 단계를 차례로 풀어 제자리에 흩뿌린다.
                for (int pass = 0; pass < 7; pass++) {
                    int pw = (w - ADAM_X0[pass] + ADAM_DX[pass] - 1) / ADAM_DX[pass];
                    int ph = (h - ADAM_Y0[pass] + ADAM_DY[pass] - 1) / ADAM_DY[pass];
                    if (w > ADAM_X0[pass] && h > ADAM_Y0[pass] && pw > 0 && ph > 0) {
                        decodePass(z, p, palette, pw, ph, ADAM_X0[pass], ADAM_Y0[pass], ADAM_DX[pass], ADAM_DY[pass], w, out);
                    }
                }
            }
        } finally {
            z.close();
        }
    }

    /**
     * Builds the 256-entry colour table for palette images (opaque black for entries outside the palette), or null for other colour types.
     * <p>
     * 팔레트 이미지의 256개 색 표를 만든다(팔레트 밖 항목은 불투명 검정). 다른 색 유형이면 null.
     *
     * @param p parsed information / 해석한 정보
     * @return colour table or null / 색 표 또는 null
     */
    private static int[] buildPalette(Parsed p) {
        if (p.colorType != 3) {
            return null;
        }
        int[] pal = new int[256];
        int count = p.palette.length / 3;
        // 팔레트 항목마다 tRNS 의 알파를 합쳐 ARGB 로 만든다.
        for (int i = 0; i < 256; i++) {
            if (i < count) {
                int a = (p.trns != null && i < p.trns.length) ? (p.trns[i] & 0xFF) : 255;
                pal[i] = (a << 24) | ((p.palette[i * 3] & 0xFF) << 16) | ((p.palette[i * 3 + 1] & 0xFF) << 8) | (p.palette[i * 3 + 2] & 0xFF);
            } else {
                pal[i] = 0xFF000000;
            }
        }
        return pal;
    }

    /**
     * Decodes one pass (or the whole image when not interlaced): reads, unfilters and converts every row.
     * <p>
     * 한 단계(인터레이스가 아니면 이미지 전체)를 디코딩한다: 모든 행을 읽고 필터를 되돌리고 변환한다.
     *
     * @param z       the inflater stream / 압축 해제 스트림
     * @param p       parsed information / 해석한 정보
     * @param palette colour table or null / 색 표 또는 null
     * @param pw      pass width in pixels / 단계의 가로 픽셀 수
     * @param ph      pass height in rows / 단계의 세로 행 수
     * @param x0      first column in the image / 이미지에서의 첫 열
     * @param y0      first row in the image / 이미지에서의 첫 행
     * @param dx      column step / 열 간격
     * @param dy      row step / 행 간격
     * @param fullW   width of the whole image / 이미지 전체의 가로
     * @param out     pixel output / 픽셀 출력
     * @throws MediaException CORRUPT on bad data / 데이터가 잘못되면 CORRUPT
     */
    private static void decodePass(ZStream z, Parsed p, int[] palette, int pw, int ph, int x0, int y0, int dx, int dy, int fullW, int[] out)
            throws MediaException {
        int rowBytes = (int) (((long) pw * p.bitsPerPixel + 7) / 8);
        int bpp = Math.max(1, p.bitsPerPixel / 8);
        byte[] prev = new byte[rowBytes];
        byte[] cur = new byte[rowBytes];
        byte[] ft = new byte[1];
        // 단계의 행을 위에서 아래로 처리한다.
        for (int y = 0; y < ph; y++) {
            if ((y & 15) == 0) {
                PixelOps.checkCancelled();
            }
            z.readFully(ft, 0, 1);
            z.readFully(cur, 0, rowBytes);
            unfilter(ft[0] & 0xFF, cur, prev, bpp);
            convertRow(cur, pw, p, palette, out, (y0 + y * dy) * fullW + x0, dx);
            byte[] t = prev;
            prev = cur;
            cur = t;
        }
    }

    /**
     * Reverses a PNG row filter in place.
     * <p>
     * PNG 행 필터를 제자리에서 되돌린다.
     *
     * @param type filter type 0 to 4 / 필터 유형 0~4
     * @param cur  the current row (filtered on entry) / 현재 행(들어올 때는 필터가 적용된 상태)
     * @param prev the previous row (unfiltered) / 이전 행(필터를 되돌린 상태)
     * @param bpp  bytes per pixel, at least 1 / 픽셀당 바이트 수(최소 1)
     * @throws MediaException CORRUPT for an unknown filter type / 알 수 없는 필터 유형이면 CORRUPT
     */
    private static void unfilter(int type, byte[] cur, byte[] prev, int bpp) throws MediaException {
        int n = cur.length;
        switch (type) {
        case 0:
            break;
        case 1:
            // Sub: 왼쪽 픽셀과의 차이를 더한다.
            for (int i = bpp; i < n; i++) {
                cur[i] = (byte) (cur[i] + cur[i - bpp]);
            }
            break;
        case 2:
            // Up: 위쪽 행과의 차이를 더한다.
            for (int i = 0; i < n; i++) {
                cur[i] = (byte) (cur[i] + prev[i]);
            }
            break;
        case 3:
            // Average: 왼쪽과 위쪽의 평균과의 차이를 더한다.
            for (int i = 0; i < n; i++) {
                int left = i >= bpp ? (cur[i - bpp] & 0xFF) : 0;
                cur[i] = (byte) (cur[i] + ((left + (prev[i] & 0xFF)) >> 1));
            }
            break;
        case 4:
            // Paeth: 왼쪽, 위쪽, 왼쪽 위 중 예측값에 가까운 것과의 차이를 더한다.
            for (int i = 0; i < n; i++) {
                int a = i >= bpp ? (cur[i - bpp] & 0xFF) : 0;
                int b = prev[i] & 0xFF;
                int c = i >= bpp ? (prev[i - bpp] & 0xFF) : 0;
                int pa = Math.abs(b - c);
                int pb = Math.abs(a - c);
                int pc = Math.abs(a + b - 2 * c);
                int pred = (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                cur[i] = (byte) (cur[i] + pred);
            }
            break;
        default:
            throw new MediaException(MediaException.Kind.CORRUPT, "unknown PNG filter type " + type);
        }
    }

    /**
     * Extracts a sample of 1, 2 or 4 bits (or 8 bits) from a row.
     * <p>
     * 행에서 1, 2, 4비트(또는 8비트) 샘플 하나를 꺼낸다.
     *
     * @param row   the row / 행
     * @param i     pixel index / 픽셀 위치
     * @param depth bit depth 1, 2, 4 or 8 / 비트 심도 1, 2, 4, 8
     * @return sample value / 샘플 값
     */
    private static int sample(byte[] row, int i, int depth) {
        if (depth == 8) {
            return row[i] & 0xFF;
        }
        int bit = i * depth;
        int shift = 8 - depth - (bit & 7);
        return ((row[bit >> 3] & 0xFF) >> shift) & ((1 << depth) - 1);
    }

    /**
     * Converts one unfiltered row to ARGB pixels and stores them with a column step.
     * <p>
     * 필터를 되돌린 행 하나를 ARGB 픽셀로 바꿔 열 간격에 맞춰 저장한다.
     *
     * @param row     the row / 행
     * @param n       pixels in the row / 행의 픽셀 수
     * @param p       parsed information / 해석한 정보
     * @param palette colour table or null / 색 표 또는 null
     * @param out     pixel output / 픽셀 출력
     * @param pos     output index of the first pixel / 첫 픽셀의 출력 위치
     * @param step    output step between pixels / 픽셀 사이의 출력 간격
     */
    private static void convertRow(byte[] row, int n, Parsed p, int[] palette, int[] out, int pos, int step) {
        int depth = p.depth;
        byte[] t = p.trns;
        switch (p.colorType) {
        case 0: {
            boolean hasKey = t != null && t.length >= 2;
            int key = hasKey ? (((t[0] & 0xFF) << 8) | (t[1] & 0xFF)) : -1;
            int max = (1 << Math.min(depth, 8)) - 1;
            // 회색 샘플을 8비트로 맞추고 tRNS 키와 같으면 투명하게 한다.
            for (int i = 0; i < n; i++) {
                int raw;
                int v;
                if (depth == 16) {
                    raw = ((row[2 * i] & 0xFF) << 8) | (row[2 * i + 1] & 0xFF);
                    v = raw >> 8;
                } else {
                    raw = sample(row, i, depth);
                    v = depth == 8 ? raw : raw * 255 / max;
                }
                int a = raw == key ? 0 : 255;
                out[pos + i * step] = (a << 24) | (v << 16) | (v << 8) | v;
            }
            break;
        }
        case 2: {
            boolean hasKey = t != null && t.length >= 6;
            int kr = hasKey ? (((t[0] & 0xFF) << 8) | (t[1] & 0xFF)) : -1;
            int kg = hasKey ? (((t[2] & 0xFF) << 8) | (t[3] & 0xFF)) : -1;
            int kb = hasKey ? (((t[4] & 0xFF) << 8) | (t[5] & 0xFF)) : -1;
            // 세 성분이 모두 tRNS 키와 같으면 투명하게 한다.
            for (int i = 0; i < n; i++) {
                int r;
                int g;
                int b;
                int rr;
                int gg;
                int bb;
                if (depth == 16) {
                    rr = ((row[6 * i] & 0xFF) << 8) | (row[6 * i + 1] & 0xFF);
                    gg = ((row[6 * i + 2] & 0xFF) << 8) | (row[6 * i + 3] & 0xFF);
                    bb = ((row[6 * i + 4] & 0xFF) << 8) | (row[6 * i + 5] & 0xFF);
                    r = rr >> 8;
                    g = gg >> 8;
                    b = bb >> 8;
                } else {
                    r = rr = row[3 * i] & 0xFF;
                    g = gg = row[3 * i + 1] & 0xFF;
                    b = bb = row[3 * i + 2] & 0xFF;
                }
                int a = (rr == kr && gg == kg && bb == kb) ? 0 : 255;
                out[pos + i * step] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            break;
        }
        case 3:
            // 팔레트 인덱스를 색 표에서 찾는다.
            for (int i = 0; i < n; i++) {
                out[pos + i * step] = palette[sample(row, i, depth)];
            }
            break;
        case 4:
            // 회색 + 알파.
            for (int i = 0; i < n; i++) {
                int v;
                int a;
                if (depth == 16) {
                    v = row[4 * i] & 0xFF;
                    a = row[4 * i + 2] & 0xFF;
                } else {
                    v = row[2 * i] & 0xFF;
                    a = row[2 * i + 1] & 0xFF;
                }
                out[pos + i * step] = (a << 24) | (v << 16) | (v << 8) | v;
            }
            break;
        default:
            // RGBA.
            // 각 항목을 차례로 처리한다.
            for (int i = 0; i < n; i++) {
                int s = depth == 16 ? 8 * i : 4 * i;
                int inc = depth == 16 ? 2 : 1;
                int r = row[s] & 0xFF;
                int g = row[s + inc] & 0xFF;
                int b = row[s + 2 * inc] & 0xFF;
                int a = row[s + 3 * inc] & 0xFF;
                out[pos + i * step] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            break;
        }
    }

    /**
     * Inflates a zlib stream that is spread over several pieces of the file array.
     * <p>
     * 파일 배열의 여러 조각에 나뉜 zlib 스트림을 푼다.
     */
    private static final class ZStream {
        /**
         * The file data.
         * <p>
         * 파일 데이터.
         */
        private final byte[] data;

        /**
         * The pieces.
         * <p>
         * 조각들.
         */
        private final Segments segs;

        /**
         * Index of the next piece to feed.
         * <p>
         * 다음에 넣을 조각의 위치.
         */
        private int next;

        /**
         * The inflater.
         * <p>
         * 압축 해제기.
         */
        private final Inflater inflater = new Inflater();

        /**
         * Creates the stream.
         * <p>
         * 스트림을 만든다.
         *
         * @param data file data / 파일 데이터
         * @param segs pieces / 조각
         */
        ZStream(byte[] data, Segments segs) {
            this.data = data;
            this.segs = segs;
        }

        /**
         * Reads exactly n bytes of inflated data.
         * <p>
         * 압축을 푼 데이터를 정확히 n 바이트 읽는다.
         *
         * @param buf buffer / 버퍼
         * @param off start / 시작
         * @param n   byte count / 바이트 수
         * @throws MediaException CORRUPT if the stream is damaged or ends early / 스트림이 손상되었거나 일찍 끝나면 CORRUPT
         */
        void readFully(byte[] buf, int off, int n) throws MediaException {
            int pos = off;
            int left = n;
            // 필요한 바이트를 모두 채울 때까지 풀고, 입력이 떨어지면 다음 조각을 넣는다.
            while (left > 0) {
                int got;
                try {
                    got = inflater.inflate(buf, pos, left);
                } catch (DataFormatException e) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "damaged PNG image data: " + e.getMessage());
                }
                if (got > 0) {
                    pos += got;
                    left -= got;
                } else if (inflater.finished() || inflater.needsDictionary()) {
                    throw new MediaException(MediaException.Kind.CORRUPT, "PNG image data ends before the image is complete");
                } else if (inflater.needsInput()) {
                    // 길이가 0 인 조각은 건너뛴다.
                    while (next < segs.count && segs.len[next] == 0) {
                        next++;
                    }
                    if (next >= segs.count) {
                        throw new MediaException(MediaException.Kind.CORRUPT, "truncated PNG image data");
                    }
                    inflater.setInput(data, segs.off[next], segs.len[next]);
                    next++;
                }
            }
        }

        /**
         * Releases the inflater.
         * <p>
         * 압축 해제기를 해제한다.
         */
        void close() {
            inflater.end();
        }
    }
}
