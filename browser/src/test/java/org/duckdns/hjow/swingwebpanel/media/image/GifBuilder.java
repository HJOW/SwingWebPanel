package org.duckdns.hjow.swingwebpanel.media.image;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds GIF files for tests with a real LZW encoder (so code size growth and table resets are exercised): global and local colour tables,
 * several frames, transparency, disposal methods, delays, interlacing and the NETSCAPE2.0 loop extension.
 * <p>
 * 시험용 GIF 파일을 실제 LZW 인코더로 만든다(코드 크기 증가와 표 초기화가 실행되도록): 전역/지역 색 표, 여러 프레임, 투명도, 처리 방법, 지연,
 * 인터레이스, NETSCAPE2.0 반복 확장.
 */
final class GifBuilder {

    /**
     * The output.
     * <p>
     * 출력.
     */
    private final Bytes out = new Bytes();

    /**
     * Creates a builder and writes the header and the logical screen.
     * <p>
     * 빌더를 만들고 헤더와 논리 화면을 쓴다.
     *
     * @param w       screen width / 화면 가로
     * @param h       screen height / 화면 세로
     * @param palette global colour table as 0xRRGGBB values (size a power of two from 2 to 256) or null / 0xRRGGBB 값의 전역 색 표(크기는 2~256 의 2 의 거듭제곱). 없으면 null
     */
    GifBuilder(int w, int h, int[] palette) {
        out.ascii("GIF89a").le16(w).le16(h);
        if (palette == null) {
            out.u8(0).u8(0).u8(0);
        } else {
            out.u8(0x80 | (sizeBits(palette.length) << 4) | sizeBits(palette.length)).u8(0).u8(0);
            writePalette(palette);
        }
    }

    /**
     * Returns the table size exponent (palette size is 2^(n+1)).
     * <p>
     * 표 크기 지수를 반환한다(팔레트 크기는 2^(n+1)).
     *
     * @param count palette entries / 팔레트 항목 수
     * @return n / n
     */
    private static int sizeBits(int count) {
        int n = 0;
        // 항목 수가 2^(n+1) 이 될 때까지 지수를 올린다.
        while ((2 << n) < count) {
            n++;
        }
        return n;
    }

    /**
     * Writes a colour table.
     * <p>
     * 색 표를 쓴다.
     *
     * @param palette 0xRRGGBB values / 0xRRGGBB 값
     */
    private void writePalette(int[] palette) {
        int size = 2 << sizeBits(palette.length);
        // 표 크기까지 RGB 를 쓰고 모자라면 0 으로 채운다.
        for (int i = 0; i < size; i++) {
            int c = i < palette.length ? palette[i] : 0;
            out.u8(c >> 16).u8(c >> 8).u8(c);
        }
    }

    /**
     * Writes the NETSCAPE2.0 loop extension.
     * <p>
     * NETSCAPE2.0 반복 확장을 쓴다.
     *
     * @param count loop count (0 forever) / 반복 횟수(0 이면 무한)
     * @return this builder / 이 빌더
     */
    GifBuilder loop(int count) {
        out.u8(0x21).u8(0xFF).u8(11).ascii("NETSCAPE2.0").u8(3).u8(1).le16(count).u8(0);
        return this;
    }

    /**
     * Writes a comment extension.
     * <p>
     * 주석 확장을 쓴다.
     *
     * @param text comment / 주석
     * @return this builder / 이 빌더
     */
    GifBuilder comment(String text) {
        out.u8(0x21).u8(0xFE).u8(text.length()).ascii(text).u8(0);
        return this;
    }

    /**
     * Writes an image with a graphic control extension.
     * <p>
     * 그래픽 제어 확장과 함께 이미지를 쓴다.
     *
     * @param left        left / 왼쪽
     * @param top         top / 위쪽
     * @param w           width / 가로
     * @param h           height / 세로
     * @param index       palette indexes row by row (in display order) / 팔레트 인덱스(행 순서, 표시 순서)
     * @param local       local colour table or null / 지역 색 표 또는 null
     * @param interlace   true to store the rows interlaced / 행을 인터레이스로 저장하려면 true
     * @param disposal    disposal method 0 to 3 / 처리 방법 0~3
     * @param delay       delay in 1/100 s / 지연(1/100초)
     * @param transparent transparent index or -1 / 투명 인덱스 또는 -1
     * @param minCode     LZW minimum code size (2 to 8) / LZW 최소 코드 크기(2~8)
     * @return this builder / 이 빌더
     */
    GifBuilder frame(int left, int top, int w, int h, byte[] index, int[] local, boolean interlace, int disposal, int delay,
            int transparent, int minCode) {
        out.u8(0x21).u8(0xF9).u8(4).u8((disposal << 2) | (transparent >= 0 ? 1 : 0)).le16(delay).u8(transparent >= 0 ? transparent : 0).u8(0);
        out.u8(0x2C).le16(left).le16(top).le16(w).le16(h);
        int flags = (interlace ? 0x40 : 0);
        if (local != null) {
            flags |= 0x80 | sizeBits(local.length);
        }
        out.u8(flags);
        if (local != null) {
            writePalette(local);
        }
        byte[] stored = interlace ? interlaceRows(index, w, h) : index;
        out.u8(minCode);
        byte[] lzw = lzw(stored, minCode);
        // 압축 데이터를 255바이트 이하의 하위 블록으로 나누어 쓴다.
        for (int p = 0; p < lzw.length; p += 255) {
            int n = Math.min(255, lzw.length - p);
            out.u8(n);
            byte[] part = new byte[n];
            System.arraycopy(lzw, p, part, 0, n);
            out.put(part);
        }
        out.u8(0);
        return this;
    }

    /**
     * Returns the file bytes (with the trailer).
     * <p>
     * 파일 바이트를 반환한다(트레일러 포함).
     *
     * @return bytes / 바이트
     */
    byte[] build() {
        return new Bytes().put(out.toBytes()).u8(0x3B).toBytes();
    }

    /**
     * Returns the file bytes without the trailer.
     * <p>
     * 트레일러 없는 파일 바이트를 반환한다.
     *
     * @return bytes / 바이트
     */
    byte[] buildWithoutTrailer() {
        return out.toBytes();
    }

    /**
     * Reorders display rows into the stored order of an interlaced frame.
     * <p>
     * 표시 순서의 행을 인터레이스 프레임의 저장 순서로 바꾼다.
     *
     * @param index display order indexes / 표시 순서의 인덱스
     * @param w     width / 가로
     * @param h     height / 세로
     * @return stored order indexes / 저장 순서의 인덱스
     */
    private static byte[] interlaceRows(byte[] index, int w, int h) {
        byte[] r = new byte[index.length];
        int[] start = { 0, 4, 2, 1 };
        int[] step = { 8, 8, 4, 2 };
        int dst = 0;
        // 네 단계의 행을 차례로 복사한다.
        for (int pass = 0; pass < 4; pass++) {
            for (int y = start[pass]; y < h; y += step[pass]) {
                System.arraycopy(index, y * w, r, dst * w, w);
                dst++;
            }
        }
        return r;
    }

    /**
     * Compresses indexes with GIF LZW.
     * <p>
     * 인덱스를 GIF LZW 로 압축한다.
     *
     * @param data    indexes / 인덱스
     * @param minCode minimum code size / 최소 코드 크기
     * @return compressed bytes (without sub-block framing) / 압축된 바이트(하위 블록 구조 없음)
     */
    static byte[] lzw(byte[] data, int minCode) {
        Bytes bytes = new Bytes();
        int[] bitState = new int[2];
        int clear = 1 << minCode;
        int eoi = clear + 1;
        int codeSize = minCode + 1;
        int next = eoi + 1;
        Map<Integer, Integer> dict = new HashMap<Integer, Integer>();
        emit(bytes, bitState, clear, codeSize);
        int prefix = data[0] & 0xFF;
        // 입력을 한 글자씩 보며 가장 긴 일치를 찾고 코드로 내보낸다.
        for (int i = 1; i < data.length; i++) {
            int c = data[i] & 0xFF;
            Integer found = dict.get((prefix << 8) | c);
            if (found != null) {
                prefix = found.intValue();
                continue;
            }
            emit(bytes, bitState, prefix, codeSize);
            dict.put((prefix << 8) | c, next);
            next++;
            if (next - 1 == (1 << codeSize) + 0 && codeSize < 12) {
                codeSize++;
            }
            if (next >= 4096) {
                emit(bytes, bitState, clear, codeSize);
                dict.clear();
                next = eoi + 1;
                codeSize = minCode + 1;
            }
            prefix = c;
        }
        emit(bytes, bitState, prefix, codeSize);
        next++;
        if (next - 1 == (1 << codeSize) && codeSize < 12) {
            codeSize++;
        }
        emit(bytes, bitState, eoi, codeSize);
        if (bitState[1] > 0) {
            bytes.u8(bitState[0]);
        }
        return bytes.toBytes();
    }

    /**
     * Appends a code to the bit stream (least significant bit first).
     * <p>
     * 비트 스트림(하위 비트 먼저)에 코드를 덧붙인다.
     *
     * @param out   output bytes / 출력 바이트
     * @param st    state: accumulated bits and their count / 상태: 쌓인 비트와 개수
     * @param code  the code / 코드
     * @param width code width in bits / 코드 폭(비트)
     */
    private static void emit(Bytes out, int[] st, int code, int width) {
        st[0] |= code << st[1];
        st[1] += width;
        // 8비트가 모이면 바이트로 내보낸다.
        while (st[1] >= 8) {
            out.u8(st[0]);
            st[0] >>>= 8;
            st[1] -= 8;
        }
    }
}
