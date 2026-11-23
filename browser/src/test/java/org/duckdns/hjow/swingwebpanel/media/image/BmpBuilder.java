package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.List;

/**
 * Builds BMP, DIB and ICO files for tests: several header sizes, bit depths, bit masks, palettes, RLE data and icon directories.
 * <p>
 * 시험용 BMP, DIB, ICO 파일을 만든다: 여러 헤더 크기, 비트 심도, 비트 마스크, 팔레트, RLE 데이터, 아이콘 디렉터리.
 */
final class BmpBuilder {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private BmpBuilder() {
    }

    /**
     * Packs pixel values into bitmap rows (4-byte aligned).
     * <p>
     * 픽셀 값을 비트맵 행(4바이트 정렬)으로 채운다.
     *
     * @param w       width / 가로
     * @param h       height / 세로
     * @param bpp     bits per pixel (1, 4, 8, 16, 24, 32) / 픽셀당 비트 수
     * @param values  values in display order, top row first: palette indexes, 16-bit raw values, 0xRRGGBB for 24, raw 32-bit values for 32 / 표시 순서(맨 위 행부터)의 값: 팔레트 인덱스, 16비트 원시 값, 24비트는 0xRRGGBB, 32비트는 원시 32비트 값
     * @param topDown true to store the top row first / 맨 위 행을 먼저 저장하려면 true
     * @return the pixel rows / 픽셀 행
     */
    static byte[] pack(int w, int h, int bpp, int[] values, boolean topDown) {
        int stride = (w * bpp + 31) / 32 * 4;
        byte[] out = new byte[stride * h];
        // 저장 순서의 행마다 값을 채운다.
        for (int r = 0; r < h; r++) {
            int y = topDown ? r : h - 1 - r;
            // 각 항목을 차례로 처리한다.
            for (int x = 0; x < w; x++) {
                int v = values[y * w + x];
                int base = r * stride;
                if (bpp == 24) {
                    out[base + 3 * x] = (byte) v;
                    out[base + 3 * x + 1] = (byte) (v >> 8);
                    out[base + 3 * x + 2] = (byte) (v >> 16);
                } else if (bpp == 32) {
                    out[base + 4 * x] = (byte) v;
                    out[base + 4 * x + 1] = (byte) (v >> 8);
                    out[base + 4 * x + 2] = (byte) (v >> 16);
                    out[base + 4 * x + 3] = (byte) (v >> 24);
                } else if (bpp == 16) {
                    out[base + 2 * x] = (byte) v;
                    out[base + 2 * x + 1] = (byte) (v >> 8);
                } else {
                    int bit = x * bpp;
                    out[base + bit / 8] |= (byte) (v << (8 - bpp - (bit & 7)));
                }
            }
        }
        return out;
    }

    /**
     * Builds a DIB (header, optional masks, palette, pixel data), optionally preceded by a file header.
     * <p>
     * DIB(헤더, 선택적 마스크, 팔레트, 픽셀 데이터)를 만든다. 선택적으로 파일 헤더가 앞에 붙는다.
     *
     * @param fileHeader  true to prepend the 14-byte BMP file header / 14바이트 BMP 파일 헤더를 앞에 붙이려면 true
     * @param headerSize  12, 40, 56 or 108 / 12, 40, 56, 108
     * @param w           width / 가로
     * @param h           signed height (negative for top-down; doubled for icons) / 부호 있는 세로(위에서 아래면 음수. 아이콘은 두 배)
     * @param bpp         bits per pixel / 픽셀당 비트 수
     * @param compression compression code / 압축 코드
     * @param clrUsed     colours used or 0 / 사용 색 수 또는 0
     * @param masks       R, G, B (and A) masks for bit field images, or null / 비트 필드 이미지의 R, G, B(와 A) 마스크. 없으면 null
     * @param palette     palette as 0xRRGGBB values or null / 0xRRGGBB 값의 팔레트. 없으면 null
     * @param pixels      pixel data / 픽셀 데이터
     * @return the bytes / 바이트
     */
    static byte[] build(boolean fileHeader, int headerSize, int w, int h, int bpp, int compression, int clrUsed, int[] masks, int[] palette,
            byte[] pixels) {
        Bytes dib = new Bytes();
        if (headerSize == 12) {
            dib.le32(12).le16(w).le16(h).le16(1).le16(bpp);
        } else {
            dib.le32(headerSize).le32(w).le32(h).le16(1).le16(bpp).le32(compression).le32(pixels.length).le32(2835).le32(2835).le32(clrUsed)
                    .le32(0);
            if (headerSize >= 52) {
                int[] m = masks != null ? masks : new int[] { 0, 0, 0, 0 };
                dib.le32(m[0]).le32(m[1]).le32(m[2]);
                if (headerSize >= 56) {
                    dib.le32(m.length > 3 ? m[3] : 0);
                }
                // V4 이상 헤더의 나머지 필드(색 공간, 끝점, 감마)는 0 으로 채운다.
                while (dib.size() < headerSize) {
                    dib.u8(0);
                }
            } else if (masks != null && (compression == 3 || compression == 6)) {
                dib.le32(masks[0]).le32(masks[1]).le32(masks[2]);
                if (compression == 6) {
                    dib.le32(masks.length > 3 ? masks[3] : 0);
                }
            }
        }
        if (palette != null) {
            // 팔레트 항목마다 BGRX(코어 헤더는 BGR)로 쓴다.
            for (int i = 0; i < palette.length; i++) {
                dib.u8(palette[i]).u8(palette[i] >> 8).u8(palette[i] >> 16);
                if (headerSize != 12) {
                    dib.u8(0);
                }
            }
        }
        int pixelStart = (fileHeader ? 14 : 0) + dib.size();
        Bytes all = new Bytes();
        if (fileHeader) {
            all.ascii("BM").le32(pixelStart + pixels.length).le32(0).le32(pixelStart);
        }
        all.put(dib.toBytes()).put(pixels);
        return all.toBytes();
    }

    /**
     * Builds the DIB of an icon image: a 40-byte header with the height doubled, a palette, XOR pixel rows and the 1-bit AND mask.
     * <p>
     * 아이콘 이미지의 DIB 를 만든다: 높이가 두 배인 40바이트 헤더, 팔레트, XOR 픽셀 행, 1비트 AND 마스크.
     *
     * @param w       width / 가로
     * @param h       height of the icon image / 아이콘 이미지의 세로
     * @param bpp     bits per pixel / 픽셀당 비트 수
     * @param palette palette or null / 팔레트 또는 null
     * @param values  pixel values in display order / 표시 순서의 픽셀 값
     * @param mask    one entry per pixel in display order, 1 = transparent / 표시 순서의 픽셀마다 하나, 1 이면 투명
     * @return the DIB bytes / DIB 바이트
     */
    static byte[] iconDib(int w, int h, int bpp, int[] palette, int[] values, int[] mask) {
        byte[] xor = pack(w, h, bpp, values, false);
        byte[] and = pack(w, h, 1, mask, false);
        byte[] pixels = new byte[xor.length + and.length];
        System.arraycopy(xor, 0, pixels, 0, xor.length);
        System.arraycopy(and, 0, pixels, xor.length, and.length);
        return build(false, 40, w, h * 2, bpp, 0, 0, null, palette, pixels);
    }

    /**
     * Builds an ICO or CUR file from images.
     * <p>
     * 이미지들로 ICO 또는 CUR 파일을 만든다.
     *
     * @param type    1 for icons, 2 for cursors / 아이콘은 1, 커서는 2
     * @param images  the image data (DIBs or PNG files) / 이미지 데이터(DIB 또는 PNG 파일)
     * @param widths  directory widths (0 means 256) / 디렉터리의 가로(0 은 256)
     * @param heights directory heights (0 means 256) / 디렉터리의 세로(0 은 256)
     * @return the file bytes / 파일 바이트
     */
    static byte[] ico(int type, List<byte[]> images, int[] widths, int[] heights) {
        Bytes out = new Bytes();
        out.le16(0).le16(type).le16(images.size());
        int offset = 6 + 16 * images.size();
        // 디렉터리 항목을 쓴다.
        for (int i = 0; i < images.size(); i++) {
            out.u8(widths[i]).u8(heights[i]).u8(0).u8(0).le16(1).le16(32).le32(images.get(i).length).le32(offset);
            offset += images.get(i).length;
        }
        // 이미지 데이터를 차례로 붙인다.
        for (byte[] img : images) {
            out.put(img);
        }
        return out.toBytes();
    }
}
