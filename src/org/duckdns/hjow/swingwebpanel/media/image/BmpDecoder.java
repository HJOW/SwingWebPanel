package org.duckdns.hjow.swingwebpanel.media.image;

import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.media.image.color.ColorProfile;

/**
 * A pure Java decoder for Windows and OS/2 bitmaps (plan items P18-02, P18-03; FEATURE_MATRIX IMG-05). The same code decodes the device
 * independent bitmaps inside ICO/CUR files (see {@link IcoDecoder}).
 * <p>
 * Supported: BITMAPCOREHEADER (12 bytes), BITMAPINFOHEADER (40), V2/V3 (52, 56), OS/2 v2 (64, without its special compressions), V4 (108)
 * and V5 (124) headers; 1, 4, 8, 16, 24 and 32 bits per pixel; palettes; BI_RGB, BI_BITFIELDS and BI_ALPHABITFIELDS masks of any layout;
 * RLE4 and RLE8; bottom-up and top-down row order. Pixels that an RLE bitmap skips stay transparent. For formats with an alpha channel
 * (32 bits per pixel BI_RGB, or an alpha mask) the alpha values are used, but if every alpha value is 0 the bitmap is treated as opaque
 * (many encoders leave the fourth byte at 0). Compressions BI_JPEG, BI_PNG, Huffman 1D and RLE24 are reported as UNSUPPORTED_FORMAT. The
 * colour space of a V4/V5 header converts the pixels of a BMP file to sRGB ({@link ColorProfile}): the calibrated RGB endpoints and gammas
 * (<code>LCS_CALIBRATED_RGB</code>) and an embedded ICC profile (<code>PROFILE_EMBEDDED</code>) of the matrix/TRC or gray kind; sRGB,
 * Windows colour space, a linked profile (a file name) and every unsupported profile leave the samples as sRGB. The bitmaps inside ICO/CUR
 * files are not converted.
 * <p>
 * Windows 와 OS/2 비트맵을 위한 순수 Java 디코더(계획 P18-02, P18-03; FEATURE_MATRIX IMG-05). ICO/CUR 파일 안의 장치 독립 비트맵도 같은 코드로
 * 디코딩한다({@link IcoDecoder} 참조).
 * <p>
 * 지원: BITMAPCOREHEADER(12바이트), BITMAPINFOHEADER(40), V2/V3(52, 56), OS/2 v2(64, 특수 압축 제외), V4(108), V5(124) 헤더, 픽셀당 1, 4, 8, 16,
 * 24, 32비트, 팔레트, BI_RGB/BI_BITFIELDS/BI_ALPHABITFIELDS 의 임의 배치 마스크, RLE4, RLE8, 아래에서 위/위에서 아래 행 순서. RLE 비트맵이 건너뛴
 * 픽셀은 투명으로 남는다. 알파 채널이 있는 형식(32비트 BI_RGB 또는 알파 마스크)은 알파 값을 쓰되 모든 알파가 0 이면 불투명으로 본다(많은 인코더가
 * 네 번째 바이트를 0 으로 둔다). BI_JPEG, BI_PNG, Huffman 1D, RLE24 압축은 UNSUPPORTED_FORMAT 으로 보고한다. V4/V5 헤더의 색 공간은 BMP 파일의
 * 픽셀을 sRGB 로 바꾼다({@link ColorProfile}): 보정된 RGB 끝점과 감마(<code>LCS_CALIBRATED_RGB</code>), 행렬/TRC 나 회색조 종류의 내장 ICC
 * 프로파일(<code>PROFILE_EMBEDDED</code>). sRGB, Windows 색 공간, 연결된 프로파일(파일 이름), 지원하지 않는 모든 프로파일은 샘플을 sRGB 로
 * 남긴다. ICO/CUR 안의 비트맵은 바꾸지 않는다.
 */
public final class BmpDecoder implements ImageDecoder {

    /**
     * Compression value: no compression.
     * <p>
     * 압축 값: 압축 없음.
     */
    private static final int BI_RGB = 0;

    /**
     * Compression value: 8-bit run length encoding.
     * <p>
     * 압축 값: 8비트 런 길이 부호화.
     */
    private static final int BI_RLE8 = 1;

    /**
     * Compression value: 4-bit run length encoding.
     * <p>
     * 압축 값: 4비트 런 길이 부호화.
     */
    private static final int BI_RLE4 = 2;

    /**
     * Compression value: colour masks follow.
     * <p>
     * 압축 값: 색 마스크가 뒤따른다.
     */
    private static final int BI_BITFIELDS = 3;

    /**
     * Compression value: colour masks including alpha follow.
     * <p>
     * 압축 값: 알파를 포함한 색 마스크가 뒤따른다.
     */
    private static final int BI_ALPHABITFIELDS = 6;

    /**
     * Creates a decoder.
     * <p>
     * 디코더를 만든다.
     */
    public BmpDecoder() {
    }

    /**
     * Returns the format name.
     * <p>
     * 형식 이름을 반환한다.
     *
     * @return <code>bmp</code>
     */
    public String getFormatName() {
        return "bmp";
    }

    /**
     * Returns the MIME types.
     * <p>
     * MIME 타입을 반환한다.
     *
     * @return MIME types / MIME 타입
     */
    public String[] getMimeTypes() {
        return new String[] { "image/bmp", "image/x-bmp", "image/x-ms-bmp" };
    }

    /**
     * Checks the <code>BM</code> signature and a plausible header size.
     * <p>
     * <code>BM</code> 시그니처와 그럴듯한 헤더 크기를 확인한다.
     *
     * @param head first bytes / 첫 바이트
     * @return true for a bitmap file / 비트맵 파일이면 true
     */
    public boolean accepts(byte[] head) {
        if (head == null || head.length < 18 || head[0] != 'B' || head[1] != 'M') {
            return false;
        }
        int hs = (head[14] & 0xFF) | ((head[15] & 0xFF) << 8) | ((head[16] & 0xFF) << 16) | ((head[17] & 0xFF) << 24);
        return hs == 12 || (hs >= 40 && hs <= 256);
    }

    /**
     * Decodes a bitmap file.
     * <p>
     * 비트맵 파일을 디코딩한다.
     *
     * @param data   file data / 파일 데이터
     * @param limits limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on damaged, unsupported or too large data / 손상, 미지원, 과대 데이터일 때
     */
    public DecodedImage decode(byte[] data, DecodeLimits limits) throws MediaException {
        try {
            if (!accepts(data)) {
                throw new MediaException(MediaException.Kind.CORRUPT, "not a BMP file");
            }
            if (data.length > limits.getMaxBytes()) {
                throw new MediaException(MediaException.Kind.TOO_LARGE, "BMP data is too large");
            }
            long pixelOffset = PixelOps.s32le(data, 10) & 0xFFFFFFFFL;
            return decodeDib(data, 14, pixelOffset, data.length, false, limits);
        } catch (RuntimeException e) {
            throw new MediaException(MediaException.Kind.CORRUPT, "damaged BMP data: " + e, e);
        } catch (OutOfMemoryError e) {
            throw new MediaException(MediaException.Kind.TOO_LARGE, "not enough memory to decode the BMP image");
        }
    }

    /**
     * Decodes a device independent bitmap (header, palette and pixel data) found at a position.
     * <p>
     * 어떤 위치에 있는 장치 독립 비트맵(헤더, 팔레트, 픽셀 데이터)을 디코딩한다.
     *
     * @param d           data / 데이터
     * @param dib         position of the DIB header / DIB 헤더의 위치
     * @param pixelOffset absolute position of the pixel data, or -1 to place it right after the header and the palette / 픽셀 데이터의 절대 위치. -1 이면 헤더와 팔레트 바로 뒤
     * @param end         end of the usable data (exclusive) / 사용할 수 있는 데이터의 끝(미포함)
     * @param ico         true for a DIB inside an ICO/CUR file: the height field counts the AND mask too, and the mask supplies transparency / ICO/CUR 안의 DIB 이면 true: 높이 필드에 AND 마스크가 포함되며 마스크가 투명도를 정한다
     * @param limits      limits / 제한
     * @return decoded image / 디코딩된 이미지
     * @throws MediaException on damaged, unsupported or too large data / 손상, 미지원, 과대 데이터일 때
     */
    static DecodedImage decodeDib(byte[] d, int dib, long pixelOffset, int end, boolean ico, DecodeLimits limits) throws MediaException {
        long hs = PixelOps.s32le(d, dib) & 0xFFFFFFFFL;
        int width;
        long rawHeight;
        int bpp;
        int compression = BI_RGB;
        int clrUsed = 0;
        int entrySize;
        if (hs == 12) {
            width = PixelOps.u16le(d, dib + 4);
            rawHeight = PixelOps.u16le(d, dib + 6);
            bpp = PixelOps.u16le(d, dib + 10);
            entrySize = 3;
        } else if (hs >= 40 && hs <= 256) {
            width = PixelOps.s32le(d, dib + 4);
            rawHeight = PixelOps.s32le(d, dib + 8);
            bpp = PixelOps.u16le(d, dib + 14);
            compression = PixelOps.s32le(d, dib + 16);
            clrUsed = PixelOps.s32le(d, dib + 32);
            entrySize = 4;
        } else {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported BMP header size " + hs);
        }
        if (width <= 0 || rawHeight == 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid bitmap size");
        }
        boolean topDown = rawHeight < 0;
        long height = Math.abs(rawHeight);
        if (ico) {
            height = height / 2;
        }
        if (compression == 4 || compression == 5 || (hs == 64 && compression == 3)) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unsupported BMP compression " + compression);
        }
        if (compression != BI_RGB && compression != BI_RLE8 && compression != BI_RLE4 && compression != BI_BITFIELDS
                && compression != BI_ALPHABITFIELDS) {
            throw new MediaException(MediaException.Kind.UNSUPPORTED_FORMAT, "unknown BMP compression " + compression);
        }
        if (bpp != 1 && bpp != 4 && bpp != 8 && bpp != 16 && bpp != 24 && bpp != 32) {
            throw new MediaException(MediaException.Kind.CORRUPT, "unsupported bits per pixel " + bpp);
        }
        if ((compression == BI_RLE8 && bpp != 8) || (compression == BI_RLE4 && bpp != 4) || (compression == BI_RGB && bpp == 0)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "compression does not match the bits per pixel");
        }
        // 크기를 확인한 뒤에야 버퍼를 할당할 수 있다.
        limits.checkImageSize(width, height);
        int h = (int) height;
        // 색 마스크를 정한다.
        int rMask = 0;
        int gMask = 0;
        int bMask = 0;
        int aMask = 0;
        long paletteStart = dib + hs;
        boolean bitfields = compression == BI_BITFIELDS || compression == BI_ALPHABITFIELDS;
        if (bitfields) {
            if (hs >= 52) {
                rMask = PixelOps.s32le(d, dib + 40);
                gMask = PixelOps.s32le(d, dib + 44);
                bMask = PixelOps.s32le(d, dib + 48);
                if (hs >= 56 || compression == BI_ALPHABITFIELDS) {
                    aMask = PixelOps.s32le(d, dib + 52);
                }
            } else {
                rMask = PixelOps.s32le(d, dib + 40);
                gMask = PixelOps.s32le(d, dib + 44);
                bMask = PixelOps.s32le(d, dib + 48);
                paletteStart += 12;
                if (compression == BI_ALPHABITFIELDS) {
                    aMask = PixelOps.s32le(d, dib + 52);
                    paletteStart += 4;
                }
            }
        } else if (bpp == 16) {
            rMask = 0x7C00;
            gMask = 0x03E0;
            bMask = 0x001F;
        } else if (bpp == 32) {
            rMask = 0x00FF0000;
            gMask = 0x0000FF00;
            bMask = 0x000000FF;
            aMask = 0xFF000000;
        }
        // 팔레트를 읽는다(인덱스 형식만).
        int[] palette = null;
        long pixels = pixelOffset;
        if (bpp <= 8) {
            int max = 1 << bpp;
            int count = (clrUsed > 0 && clrUsed <= max) ? clrUsed : max;
            long avail = (end - paletteStart) / entrySize;
            if (pixelOffset >= 0 && pixelOffset > paletteStart) {
                avail = Math.min(avail, (pixelOffset - paletteStart) / entrySize);
            }
            palette = new int[Math.max(count, 0)];
            // 팔레트 항목마다 BGR(X)를 읽고 모자라는 항목은 검정으로 둔다.
            for (int i = 0; i < palette.length; i++) {
                if (i < avail) {
                    int p = (int) (paletteStart + (long) i * entrySize);
                    palette[i] = 0xFF000000 | ((d[p + 2] & 0xFF) << 16) | ((d[p + 1] & 0xFF) << 8) | (d[p] & 0xFF);
                } else {
                    palette[i] = 0xFF000000;
                }
            }
            if (pixelOffset < 0) {
                pixels = paletteStart + (long) count * entrySize;
            }
        } else if (pixelOffset < 0) {
            pixels = paletteStart + (clrUsed > 0 ? (long) clrUsed * entrySize : 0);
        }
        if (pixels < 0 || pixels > end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "bitmap pixel data lies outside the file");
        }
        int[] out = new int[width * h];
        // ICO/CUR 안의 비트맵이 아니면 V4/V5 헤더의 색 공간을 읽는다(변환은 픽셀을 다 만든 뒤에 한다).
        ColorProfile profile = ico ? ColorProfile.SRGB : colorSpace(d, dib, hs, end).forImage(bpp <= 8 && isGrayPalette(palette));
        if (compression == BI_RLE8 || compression == BI_RLE4) {
            decodeRle(d, (int) pixels, end, width, h, compression == BI_RLE8, palette, out);
            profile.convert(out);
            return new DecodedImage(width, h, out);
        }
        long stride = ((long) width * bpp + 31) / 32 * 4;
        long needed = stride * h;
        if (pixels + needed > end) {
            throw new MediaException(MediaException.Kind.CORRUPT, "bitmap pixel data is truncated");
        }
        boolean anyAlpha = false;
        // 행마다 픽셀을 읽는다(기본은 아래에서 위로 저장).
        for (int row = 0; row < h; row++) {
            if ((row & 63) == 0) {
                PixelOps.checkCancelled();
            }
            int srcRow = topDown ? row : h - 1 - row;
            int rp = (int) (pixels + stride * srcRow);
            int dp = row * width;
            if (bpp <= 8) {
                // 인덱스 형식: 비트 단위로 팔레트 인덱스를 꺼낸다.
                for (int x = 0; x < width; x++) {
                    int bit = x * bpp;
                    int idx = ((d[rp + (bit >> 3)] & 0xFF) >> (8 - bpp - (bit & 7))) & ((1 << bpp) - 1);
                    out[dp + x] = idx < palette.length ? palette[idx] : 0xFF000000;
                }
            } else if (bpp == 24) {
                // 각 항목을 차례로 처리한다.
                for (int x = 0; x < width; x++) {
                    int p = rp + 3 * x;
                    out[dp + x] = 0xFF000000 | ((d[p + 2] & 0xFF) << 16) | ((d[p + 1] & 0xFF) << 8) | (d[p] & 0xFF);
                }
            } else {
                // 16/32비트: 마스크로 성분을 꺼내 8비트로 맞춘다.
                for (int x = 0; x < width; x++) {
                    int v;
                    if (bpp == 16) {
                        v = (d[rp + 2 * x] & 0xFF) | ((d[rp + 2 * x + 1] & 0xFF) << 8);
                    } else {
                        v = (d[rp + 4 * x] & 0xFF) | ((d[rp + 4 * x + 1] & 0xFF) << 8) | ((d[rp + 4 * x + 2] & 0xFF) << 16)
                                | ((d[rp + 4 * x + 3] & 0xFF) << 24);
                    }
                    int a = aMask == 0 ? 255 : component(v, aMask);
                    if (a != 0) {
                        anyAlpha = true;
                    }
                    out[dp + x] = (a << 24) | (component(v, rMask) << 16) | (component(v, gMask) << 8) | component(v, bMask);
                }
            }
        }
        // 알파 값이 하나도 없는 알파 형식은 불투명으로 본다.
        if (aMask != 0 && !anyAlpha) {
            for (int i = 0; i < out.length; i++) {
                out[i] |= 0xFF000000;
            }
        }
        if (ico) {
            applyAndMask(d, (int) (pixels + needed), end, width, h, out, bpp == 32 && anyAlpha);
        }
        profile.convert(out);
        return new DecodedImage(width, h, out);
    }

    /**
     * Tells whether a palette has gray entries only (so that it is a gray image for the colour management).
     * <p>
     * 팔레트가 회색 항목만 가졌는지(그래서 색 관리에서 회색 이미지인지) 알려 준다.
     *
     * @param palette the palette, or null / 팔레트. 없으면 null
     * @return true if every entry is gray / 모든 항목이 회색이면 true
     */
    private static boolean isGrayPalette(int[] palette) {
        if (palette == null) {
            return false;
        }
        // 항목마다 세 성분이 같은지 본다.
        for (int i = 0; i < palette.length; i++) {
            int c = palette[i];
            if (((c >> 16) & 0xFF) != ((c >> 8) & 0xFF) || ((c >> 8) & 0xFF) != (c & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads the colour space of a V4 or V5 header: the colour space type at offset 56, then (V4 and V5) the CIE XYZ endpoints at 60 (nine
     * numbers of 2.30 fixed point) and the gammas of red, green and blue at 96 (16.16 fixed point), and (V5) the offset and the size of an
     * embedded profile at 112 and 116 (the offset counts from the start of the header). Anything that is not
     * <code>LCS_CALIBRATED_RGB</code> or <code>PROFILE_EMBEDDED</code>, and damaged values, give sRGB.
     * <p>
     * V4/V5 헤더의 색 공간을 읽는다: 56번 위치의 색 공간 종류, 이어서(V4, V5) 60번의 CIE XYZ 끝점(2.30 고정 소수점 수 아홉 개)과 96번의 빨강, 초록, 파랑
     * 감마(16.16 고정 소수점), (V5) 112번과 116번의 내장 프로파일 오프셋과 크기(오프셋은 헤더의 시작에서부터 센다).
     * <code>LCS_CALIBRATED_RGB</code> 나 <code>PROFILE_EMBEDDED</code> 가 아니거나 값이 손상되었으면 sRGB 이다.
     *
     * @param d   data / 데이터
     * @param dib position of the DIB header / DIB 헤더의 위치
     * @param hs  the header size / 헤더 크기
     * @param end end of the usable data / 사용할 수 있는 데이터의 끝
     * @return the conversion to sRGB, never null / sRGB 로의 변환. null 이 아님
     * @throws MediaException CORRUPT if a field lies outside the data / 필드가 데이터 밖이면 CORRUPT
     */
    private static ColorProfile colorSpace(byte[] d, int dib, long hs, int end) throws MediaException {
        if (hs < 108 || dib + hs > end) {
            return ColorProfile.SRGB;
        }
        int type = PixelOps.s32le(d, dib + 56);
        ColorProfile p = null;
        if (type == 0) {
            // LCS_CALIBRATED_RGB: 세 끝점의 XYZ 와 채널별 감마.
            double[] xyz = new double[9];
            // 끝점 아홉 값을 2.30 고정 소수점에서 실수로 바꾼다.
            for (int i = 0; i < 9; i++) {
                xyz[i] = PixelOps.s32le(d, dib + 60 + 4 * i) / 1073741824.0;
            }
            double gr = (PixelOps.s32le(d, dib + 96) & 0xFFFFFFFFL) / 65536.0;
            double gg = (PixelOps.s32le(d, dib + 100) & 0xFFFFFFFFL) / 65536.0;
            double gb = (PixelOps.s32le(d, dib + 104) & 0xFFFFFFFFL) / 65536.0;
            p = ColorProfile.fromCalibrated(xyz, gr, gg, gb);
        } else if (type == 0x4D424544 && hs >= 124) {
            // PROFILE_EMBEDDED('MBED'): 프로파일이 헤더 시작에서 오프셋만큼 떨어져 있다.
            long off = PixelOps.s32le(d, dib + 112) & 0xFFFFFFFFL;
            long size = PixelOps.s32le(d, dib + 116) & 0xFFFFFFFFL;
            if (size > 0 && dib + off + size <= end) {
                p = ColorProfile.fromIcc(java.util.Arrays.copyOfRange(d, (int) (dib + off), (int) (dib + off + size)));
            }
        }
        return p == null ? ColorProfile.SRGB : p;
    }

    /**
     * Extracts one colour component of a pixel with a mask and scales it to 8 bits.
     * <p>
     * 마스크로 픽셀의 색 성분 하나를 꺼내 8비트로 맞춘다.
     *
     * @param v    the pixel value / 픽셀 값
     * @param mask the component mask / 성분 마스크
     * @return component 0 to 255 (0 if the mask is empty) / 성분 0~255(마스크가 비었으면 0)
     */
    private static int component(int v, int mask) {
        if (mask == 0) {
            return 0;
        }
        int shift = Integer.numberOfTrailingZeros(mask);
        int bits = Integer.bitCount(mask);
        int x = (v & mask) >>> shift;
        if (bits == 8) {
            return x;
        }
        if (bits > 8) {
            return x >>> (bits - 8);
        }
        return x * 255 / ((1 << bits) - 1);
    }

    /**
     * Applies the 1-bit AND mask of an icon image: a set bit makes the pixel transparent. Ignored if the bitmap itself carries alpha.
     * <p>
     * 아이콘 이미지의 1비트 AND 마스크를 적용한다: 비트가 켜지면 픽셀이 투명해진다. 비트맵 자체에 알파가 있으면 무시한다.
     *
     * @param d        data / 데이터
     * @param maskPos  position of the mask rows / 마스크 행의 위치
     * @param end      end of the usable data / 사용할 수 있는 데이터의 끝
     * @param w        width / 가로
     * @param h        height / 세로
     * @param out      pixels / 픽셀
     * @param hasAlpha true if the bitmap has its own alpha channel / 비트맵에 자체 알파 채널이 있으면 true
     */
    private static void applyAndMask(byte[] d, int maskPos, int end, int w, int h, int[] out, boolean hasAlpha) {
        long stride = ((long) w + 31) / 32 * 4;
        if (hasAlpha || (long) maskPos + stride * h > end) {
            return;
        }
        // 마스크는 아래에서 위로 저장되어 있다.
        for (int row = 0; row < h; row++) {
            int rp = (int) (maskPos + stride * (h - 1 - row));
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < w; x++) {
                boolean transparent = ((d[rp + (x >> 3)] >> (7 - (x & 7))) & 1) != 0;
                if (transparent) {
                    out[row * w + x] = 0;
                } else {
                    out[row * w + x] |= 0xFF000000;
                }
            }
        }
    }

    /**
     * Decodes run length encoded pixel data (RLE8 or RLE4) into ARGB; pixels that are never written stay transparent.
     * <p>
     * 런 길이 부호화된 픽셀 데이터(RLE8 또는 RLE4)를 ARGB 로 푼다. 쓰이지 않은 픽셀은 투명으로 남는다.
     *
     * @param d       data / 데이터
     * @param start   position of the pixel data / 픽셀 데이터의 위치
     * @param end     end of the usable data / 사용할 수 있는 데이터의 끝
     * @param w       width / 가로
     * @param h       height / 세로
     * @param rle8    true for RLE8, false for RLE4 / RLE8 이면 true, RLE4 이면 false
     * @param palette colours / 색
     * @param out     pixels / 픽셀
     * @throws MediaException CANCELLED if interrupted / 인터럽트되면 CANCELLED
     */
    private static void decodeRle(byte[] d, int start, int end, int w, int h, boolean rle8, int[] palette, int[] out) throws MediaException {
        int x = 0;
        int y = 0;
        int p = start;
        // 명령을 하나씩 읽는다: 반복(n>0), 줄 끝/비트맵 끝/이동/절대 모드(n==0).
        while (p + 1 < end && y < h) {
            int n = d[p++] & 0xFF;
            int v = d[p++] & 0xFF;
            if (n > 0) {
                // 같은 값(RLE8) 또는 두 니블 번갈아(RLE4)를 n 픽셀 반복한다.
                for (int i = 0; i < n; i++) {
                    int idx = rle8 ? v : ((i & 1) == 0 ? v >> 4 : v & 15);
                    put(out, w, h, x + i, y, palette, idx);
                }
                x += n;
            } else if (v == 0) {
                x = 0;
                y++;
                if ((y & 63) == 0) {
                    PixelOps.checkCancelled();
                }
            } else if (v == 1) {
                break;
            } else if (v == 2) {
                if (p + 1 >= end) {
                    break;
                }
                x += d[p++] & 0xFF;
                y += d[p++] & 0xFF;
            } else {
                // 절대 모드: v 개의 픽셀이 그대로 들어 있고 바이트 수는 짝수로 맞춘다.
                int bytes = rle8 ? v : (v + 1) / 2;
                if (p + bytes > end) {
                    break;
                }
                // 각 항목을 차례로 처리한다.
                for (int i = 0; i < v; i++) {
                    int idx = rle8 ? (d[p + i] & 0xFF) : ((i & 1) == 0 ? (d[p + i / 2] & 0xFF) >> 4 : d[p + i / 2] & 15);
                    put(out, w, h, x + i, y, palette, idx);
                }
                x += v;
                p += bytes + (bytes & 1);
            }
        }
    }

    /**
     * Stores one RLE pixel (clipped to the bitmap); y counts from the bottom row.
     * <p>
     * RLE 픽셀 하나를 저장한다(비트맵 밖은 무시). y 는 맨 아래 행부터 센다.
     *
     * @param out     pixels / 픽셀
     * @param w       width / 가로
     * @param h       height / 세로
     * @param x       column / 열
     * @param y       row counted from the bottom / 아래에서부터 센 행
     * @param palette colours / 색
     * @param idx     palette index / 팔레트 인덱스
     */
    private static void put(int[] out, int w, int h, int x, int y, int[] palette, int idx) {
        if (x < w && y < h) {
            out[(h - 1 - y) * w + x] = idx < palette.length ? palette[idx] : 0xFF000000;
        }
    }
}
