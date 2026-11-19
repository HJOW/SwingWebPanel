package org.duckdns.hjow.swingwebpanel.media.font;

import java.io.IOException;
import java.util.Arrays;

/**
 * The decoder of WOFF 2.0 web fonts (plan item P10-08-e, W3C Recommendation "WOFF File Format 2.0", https://www.w3.org/TR/WOFF2/, last modified 2024-08-05,
 * read with <code>tools/webfetch</code> on 2026-11-18): a WOFF2 file holds the tables of an sfnt font in one Brotli stream ({@link Brotli}) after a table
 * directory with known-tag codes and variable-length numbers. {@link #toSfnt(byte[])} reads the header, the table directory (the 63 known tags, the
 * <code>UIntBase128</code> lengths, the transform versions), checks the structure of the file (lengths, padding, no extraneous data), decompresses the stream and
 * rebuilds an ordinary sfnt file that the JRE can read with <code>Font.createFont</code>: the transformed <code>glyf</code> table (section 5.1: the seven
 * streams, the triplet coordinates of section 5.2 with their 128 flag values, composite glyphs, instructions, the bounding box bitmap and the overlap bitmap),
 * the <code>loca</code> table that is rebuilt with it (5.3) and the transformed <code>hmtx</code> table (5.4, the left side bearings come from the glyph boxes)
 * are reconstructed; every table gets a new checksum and the <code>head</code> table a new <code>checkSumAdjustment</code>. The tables are sorted by tag as an
 * sfnt requires. Everything that the specification says a decoder must reject (a wrong signature, extraneous data, overlapping or out-of-range data, a
 * <code>UIntBase128</code> with leading zeros or longer than five bytes, an unknown transform version, a transformed <code>loca</code> with a length, an empty
 * glyph with a box, a composite glyph without a box, a size that does not match the sum of the table lengths, a Brotli failure) is rejected with an
 * {@link IOException}, and so is what this decoder cannot represent safely (font collections <code>ttcf</code>, output over {@link #MAX_SFNT_SIZE}, a glyph
 * point that leaves the 16 bit range, a contour without points). The extended metadata and the private data blocks are checked for their place and ignored.
 * <p>
 * <b>Verification:</b> there was no WOFF2 file made by a browser or another independent encoder on the machine of the author, so the decoder is tested with files
 * that were made by encoders written for the tests from the same specification text (a script for real JRE fonts and a builder for synthetic fonts and invalid
 * files): an error in how the specification was read would be in both the encoder and the decoder and stay unseen. What protects against that is that the
 * encoders use the table of section 5.2 as data (the decoder uses the closed formulas), that the result of real fonts is compared with the original font table
 * by table and glyph by glyph (also with Java2D), and that the Brotli part is verified against the reference encoder separately.
 * <p>
 * WOFF 2.0 웹 글꼴 디코더(계획 P10-08-e, W3C 권고안 "WOFF File Format 2.0", https://www.w3.org/TR/WOFF2/, 최종 수정 2024-08-05, 2026-11-18 에 <code>tools/webfetch</code> 로 읽음):
 * WOFF2 파일은 알려진 태그 코드와 가변 길이 수가 있는 테이블 디렉터리 뒤에 sfnt 글꼴의 테이블을 Brotli 스트림({@link Brotli}) 하나로 담는다. {@link #toSfnt(byte[])} 는 헤더,
 * 테이블 디렉터리(알려진 태그 63 개, <code>UIntBase128</code> 길이, 변환 버전)를 읽고 파일 구조(길이, 패딩, 불필요한 데이터 없음)를 검사하고 스트림의 압축을 풀어 JRE 가
 * <code>Font.createFont</code> 로 읽을 수 있는 평범한 sfnt 파일을 다시 만든다. 변환된 <code>glyf</code> 테이블(5.1 절: 스트림 일곱 개, 5.2 절의 삼중 좌표와 128 개 플래그 값,
 * 합성 글리프, 명령, 경계 상자 비트맵과 겹침 비트맵), 그것과 함께 다시 만드는 <code>loca</code> 테이블(5.3), 변환된 <code>hmtx</code> 테이블(5.4, 왼쪽 여백은 글리프 상자에서 온다)을
 * 복원하고, 모든 테이블에 새 체크섬을, <code>head</code> 테이블에 새 <code>checkSumAdjustment</code> 를 준다. 테이블은 sfnt 가 요구하는 대로 태그 순으로 정렬한다. 규격이 디코더가
 * 거부해야 한다고 한 모든 것(틀린 서명, 불필요한 데이터, 겹치거나 범위를 벗어난 데이터, 앞이 0 이거나 다섯 바이트보다 긴 <code>UIntBase128</code>, 모르는 변환 버전, 길이가 있는
 * 변환된 <code>loca</code>, 상자가 있는 빈 글리프, 상자가 없는 합성 글리프, 테이블 길이의 합과 다른 크기, Brotli 실패)과 이 디코더가 안전하게 나타낼 수 없는 것(글꼴 컬렉션
 * <code>ttcf</code>, {@link #MAX_SFNT_SIZE} 를 넘는 출력, 16 비트 범위를 벗어난 글리프 점, 점이 없는 윤곽선)은 {@link IOException} 으로 거부한다. 확장 메타데이터와 비공개 데이터
 * 블록은 위치만 검사하고 무시한다.
 * <p>
 * <b>검증:</b> 작성자의 PC 에는 브라우저나 다른 독립 인코더가 만든 WOFF2 파일이 없었으므로 이 디코더는 같은 규격 텍스트로 시험용으로 쓴 인코더(실제 JRE 글꼴용 스크립트와 합성 글꼴·
 * 잘못된 파일용 생성기)가 만든 파일로 시험한다. 규격을 잘못 읽었다면 그 오류는 인코더와 디코더에 함께 들어 보이지 않을 수 있다. 이를 줄이는 것은 인코더가 5.2 절의 표를
 * 자료로 쓰고(디코더는 닫힌 식을 쓴다), 실제 글꼴의 결과를 원본 글꼴과 테이블별, 글리프별(Java2D 로도) 비교하고, Brotli 부분을 참조 인코더로 따로 검증한 것이다.
 */
public final class Woff2 {

    /** The largest sfnt file that is rebuilt, 16 MiB (also the largest size of the decompressed table data). 다시 만드는 sfnt 파일의 최대 크기. 16 MiB(풀린 테이블 데이터의 최대 크기이기도 하다). */
    public static final int MAX_SFNT_SIZE = 16 * 1024 * 1024;

    /** The largest number of tables of a font that is read. 읽는 글꼴의 최대 테이블 수. */
    public static final int MAX_TABLES = 1024;

    /** The size of the WOFF2 header in bytes. WOFF2 헤더의 크기(바이트). */
    private static final int HEADER_SIZE = 48;

    /** The signature <code>wOF2</code>. 서명 <code>wOF2</code>. */
    private static final long SIGNATURE = 0x774F4632L;

    /** The flavor <code>ttcf</code> of a font collection (not supported). 글꼴 컬렉션의 flavor <code>ttcf</code>(지원하지 않음). */
    private static final long FLAVOR_COLLECTION = 0x74746366L;

    /** The table tags of the known tag codes 0 to 62 (section 4.1). 알려진 태그 코드 0~62 의 테이블 태그(4.1 절). */
    private static final String[] KNOWN_TAGS = { "cmap", "head", "hhea", "hmtx", "maxp", "name", "OS/2", "post", "cvt ", "fpgm", "glyf", "loca", "prep", "CFF ", "VORG", "EBDT",
            "EBLC", "gasp", "hdmx", "kern", "LTSH", "PCLT", "VDMX", "vhea", "vmtx", "BASE", "GDEF", "GPOS", "GSUB", "EBSC", "JSTF", "MATH", "CBDT", "CBLC", "COLR", "CPAL",
            "SVG ", "sbix", "acnt", "avar", "bdat", "bloc", "bsln", "cvar", "fdsc", "feat", "fmtx", "fvar", "gvar", "hsty", "just", "lcar", "mort", "morx", "opbd", "prop",
            "trak", "Zapf", "Silf", "Glat", "Gloc", "Feat", "Sill" };

    /** The tag <code>glyf</code>. 태그 <code>glyf</code>. */
    private static final long TAG_GLYF = tag("glyf");

    /** The tag <code>loca</code>. 태그 <code>loca</code>. */
    private static final long TAG_LOCA = tag("loca");

    /** The tag <code>hmtx</code>. 태그 <code>hmtx</code>. */
    private static final long TAG_HMTX = tag("hmtx");

    /** The tag <code>head</code>. 태그 <code>head</code>. */
    private static final long TAG_HEAD = tag("head");

    /** The tag <code>hhea</code>. 태그 <code>hhea</code>. */
    private static final long TAG_HHEA = tag("hhea");

    /** The tag <code>maxp</code>. 태그 <code>maxp</code>. */
    private static final long TAG_MAXP = tag("maxp");

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Woff2() {
    }

    /**
     * One entry of the table directory.
     * <p>
     * 테이블 디렉터리의 항목 하나.
     */
    private static final class Entry {
        /** The tag as a 32 bit number. 32비트 수로 나타낸 태그. */
        long tag;
        /** The length of the original table (a reference value for a transformed table). 원래 테이블의 길이(변환된 테이블에는 참고 값). */
        long origLength;
        /** The length of the transformed table, or the original length of a table without transform. 변환된 테이블의 길이, 변환이 없으면 원래 길이. */
        long dataLength;
        /** Whether a transform other than the null transform was applied. 널 변환이 아닌 변환을 적용했는지 여부. */
        boolean transformed;
        /** The offset of the data in the decompressed block. 풀린 블록 안의 데이터 오프셋. */
        int dataOffset;
        /** The rebuilt table. 다시 만든 테이블. */
        byte[] table;
    }

    /**
     * A reader of big-endian numbers and of the variable-length numbers of the format over a part of an array; reading beyond the end fails.
     * <p>
     * 배열의 한 부분에서 빅 엔디언 수와 형식의 가변 길이 수를 읽는 읽기 도구. 끝을 넘어 읽으면 실패한다.
     */
    private static final class Reader {
        /** The array. 배열. */
        final byte[] b;
        /** The next position. 다음 위치. */
        int pos;
        /** The end (exclusive). 끝(미포함). */
        final int end;

        /**
         * Creates a reader.
         * <p>
         * 읽기 도구를 만든다.
         *
         * @param b   the array / 배열
         * @param pos the first position / 첫 위치
         * @param end the end (exclusive) / 끝(미포함)
         */
        Reader(byte[] b, int pos, int end) {
            this.b = b;
            this.pos = pos;
            this.end = end;
        }

        /**
         * Reads an unsigned byte.
         * <p>
         * 부호 없는 바이트를 읽는다.
         *
         * @return the value / 값
         * @throws IOException if the data ends / 데이터가 끝나면
         */
        int u8() throws IOException {
            if (pos >= end) {
                throw new IOException("WOFF2 data is cut off");
            }
            return b[pos++] & 0xFF;
        }

        /**
         * Reads an unsigned 16 bit number.
         * <p>
         * 부호 없는 16비트 수를 읽는다.
         *
         * @return the value / 값
         * @throws IOException if the data ends / 데이터가 끝나면
         */
        int u16() throws IOException {
            int hi = u8();
            return (hi << 8) | u8();
        }

        /**
         * Reads a signed 16 bit number.
         * <p>
         * 부호 있는 16비트 수를 읽는다.
         *
         * @return the value / 값
         * @throws IOException if the data ends / 데이터가 끝나면
         */
        int i16() throws IOException {
            return (short) u16();
        }

        /**
         * Reads an unsigned 32 bit number.
         * <p>
         * 부호 없는 32비트 수를 읽는다.
         *
         * @return the value / 값
         * @throws IOException if the data ends / 데이터가 끝나면
         */
        long u32() throws IOException {
            long hi = u16();
            return (hi << 16) | u16();
        }

        /**
         * Reads a <code>UIntBase128</code> (section 3.1): seven bits per byte, the high bit marks that more follow; a leading 0x80 byte, a value above 2^32-1 and
         * more than five bytes are errors.
         * <p>
         * <code>UIntBase128</code>(3.1 절)을 읽는다: 바이트당 7 비트이고 높은 비트는 더 이어짐을 뜻한다. 앞의 0x80 바이트, 2^32-1 을 넘는 값, 다섯 바이트를 넘는 것은 오류이다.
         *
         * @return the value / 값
         * @throws IOException if the number is invalid / 수가 올바르지 않으면
         */
        long base128() throws IOException {
            long accum = 0;
            // 최대 다섯 바이트까지 읽는다.
            for (int i = 0; i < 5; i++) {
                int data = u8();
                if (i == 0 && data == 0x80) {
                    throw new IOException("WOFF2 UIntBase128 with a leading zero");
                }
                if ((accum & 0xFE000000L) != 0) {
                    throw new IOException("WOFF2 UIntBase128 overflows 32 bits");
                }
                accum = (accum << 7) | (data & 0x7F);
                if ((data & 0x80) == 0) {
                    return accum;
                }
            }
            throw new IOException("WOFF2 UIntBase128 is longer than five bytes");
        }

        /**
         * Reads a <code>255UInt16</code> (section 3.1): a code below 253 is the value, 253 is followed by a 16 bit value, 254 by a byte that is added to 506 and 255 by a
         * byte that is added to 253.
         * <p>
         * <code>255UInt16</code>(3.1 절)을 읽는다: 253 미만의 코드는 그 값이고, 253 뒤에는 16비트 값이, 254 뒤에는 506 에 더하는 바이트가, 255 뒤에는 253 에 더하는 바이트가 온다.
         *
         * @return the value / 값
         * @throws IOException if the data ends / 데이터가 끝나면
         */
        int read255() throws IOException {
            int code = u8();
            if (code == 253) {
                return u16();
            }
            if (code == 255) {
                return u8() + 253;
            }
            if (code == 254) {
                return u8() + 506;
            }
            return code;
        }

        /**
         * Cuts the next <code>size</code> bytes off as another reader.
         * <p>
         * 다음 <code>size</code> 바이트를 다른 읽기 도구로 잘라 낸다.
         *
         * @param size the number of bytes / 바이트 수
         * @return the reader of these bytes / 그 바이트의 읽기 도구
         * @throws IOException if fewer bytes are left / 남은 바이트가 더 적으면
         */
        Reader cut(long size) throws IOException {
            if (size < 0 || size > end - pos) {
                throw new IOException("WOFF2 stream sizes exceed the table");
            }
            Reader r = new Reader(b, pos, pos + (int) size);
            pos += (int) size;
            return r;
        }

        /**
         * Reads the next <code>n</code> bytes.
         * <p>
         * 다음 <code>n</code> 바이트를 읽는다.
         *
         * @param n the number of bytes / 바이트 수
         * @return a copy of the bytes / 바이트의 사본
         * @throws IOException if the data ends / 데이터가 끝나면
         */
        byte[] bytes(int n) throws IOException {
            if (n < 0 || n > end - pos) {
                throw new IOException("WOFF2 data is cut off");
            }
            byte[] r = new byte[n];
            System.arraycopy(b, pos, r, 0, n);
            pos += n;
            return r;
        }
    }

    /**
     * A growing byte buffer with a size limit, for the glyph table.
     * <p>
     * 글리프 테이블을 위한 크기 한도가 있는 늘어나는 바이트 버퍼.
     */
    private static final class Out {
        /** The bytes. 바이트. */
        byte[] buf = new byte[1024];
        /** The number of bytes written. 쓴 바이트 수. */
        int len;

        /**
         * Makes room for more bytes.
         * <p>
         * 더 많은 바이트를 위한 공간을 만든다.
         *
         * @param more the number of bytes to add / 더할 바이트 수
         * @throws IOException if the limit is exceeded / 한도를 넘으면
         */
        void ensure(int more) throws IOException {
            long need = (long) len + more;
            if (need > MAX_SFNT_SIZE) {
                throw new IOException("WOFF2 glyph table is larger than the limit");
            }
            if (need > buf.length) {
                buf = Arrays.copyOf(buf, (int) Math.min(MAX_SFNT_SIZE, Math.max(need, (long) buf.length * 2)));
            }
        }

        /**
         * Writes a byte.
         * <p>
         * 바이트를 쓴다.
         *
         * @param v the value / 값
         * @throws IOException if the limit is exceeded / 한도를 넘으면
         */
        void put8(int v) throws IOException {
            ensure(1);
            buf[len++] = (byte) v;
        }

        /**
         * Writes a big-endian 16 bit number.
         * <p>
         * 빅 엔디언 16비트 수를 쓴다.
         *
         * @param v the value / 값
         * @throws IOException if the limit is exceeded / 한도를 넘으면
         */
        void put16(int v) throws IOException {
            ensure(2);
            buf[len++] = (byte) (v >> 8);
            buf[len++] = (byte) v;
        }

        /**
         * Writes bytes.
         * <p>
         * 바이트들을 쓴다.
         *
         * @param v the bytes / 바이트
         * @throws IOException if the limit is exceeded / 한도를 넘으면
         */
        void put(byte[] v) throws IOException {
            ensure(v.length);
            System.arraycopy(v, 0, buf, len, v.length);
            len += v.length;
        }

        /**
         * Pads with zero bytes to a multiple of four.
         * <p>
         * 4 의 배수가 되도록 0 바이트로 채운다.
         *
         * @throws IOException if the limit is exceeded / 한도를 넘으면
         */
        void align4() throws IOException {
            // 4 바이트 경계까지 0 을 쓴다.
            while ((len & 3) != 0) {
                put8(0);
            }
        }

        /**
         * Returns the bytes written.
         * <p>
         * 쓴 바이트를 반환한다.
         *
         * @return a copy / 사본
         */
        byte[] toBytes() {
            return Arrays.copyOf(buf, len);
        }
    }

    /**
     * The result of the glyph table reconstruction.
     * <p>
     * 글리프 테이블 복원의 결과.
     */
    private static final class Glyphs {
        /** The glyph table. 글리프 테이블. */
        byte[] glyf;
        /** The location table. 위치 테이블. */
        byte[] loca;
        /** The xMin of every glyph (0 for an empty glyph). 글리프별 xMin(빈 글리프는 0). */
        int[] xMin;
    }

    /**
     * Returns the tag of four ASCII characters as a number.
     * <p>
     * 아스키 문자 네 개로 된 태그를 수로 반환한다.
     *
     * @param s the tag / 태그
     * @return the number / 수
     */
    private static long tag(String s) {
        return ((long) (s.charAt(0) & 0xFF) << 24) | ((s.charAt(1) & 0xFF) << 16) | ((s.charAt(2) & 0xFF) << 8) | (s.charAt(3) & 0xFF);
    }

    /**
     * Tells whether the bytes start with the WOFF2 signature <code>wOF2</code>.
     * <p>
     * 바이트가 WOFF2 서명 <code>wOF2</code> 로 시작하는지 알려 준다.
     *
     * @param b the file / 파일
     * @return true for WOFF2 / WOFF2 이면 true
     */
    public static boolean isWoff2(byte[] b) {
        return b != null && b.length >= 4 && b[0] == 'w' && b[1] == 'O' && b[2] == 'F' && b[3] == '2';
    }

    /**
     * Rebuilds the sfnt font of a WOFF2 file.
     * <p>
     * WOFF2 파일의 sfnt 글꼴을 다시 만든다.
     *
     * @param file the WOFF2 file / WOFF2 파일
     * @return the sfnt file (TrueType or OpenType) / sfnt 파일(TrueType 또는 OpenType)
     * @throws IOException if the file is not a valid WOFF2 file, uses something that is not supported or exceeds a limit / 올바른 WOFF2 파일이 아니거나 지원하지 않는 것을 쓰거나 한도를 넘으면
     */
    public static byte[] toSfnt(byte[] file) throws IOException {
        if (!isWoff2(file) || file.length < HEADER_SIZE) {
            throw new IOException("not a WOFF2 file");
        }
        Reader h = new Reader(file, 4, file.length);
        long flavor = h.u32();
        long length = h.u32();
        int numTables = h.u16();
        h.u16();
        h.u32();
        long compressedSize = h.u32();
        h.u16();
        h.u16();
        long metaOffset = h.u32();
        long metaLength = h.u32();
        h.u32();
        long privOffset = h.u32();
        long privLength = h.u32();
        if (flavor == FLAVOR_COLLECTION) {
            throw new IOException("WOFF2 font collections are not supported");
        }
        if (flavor != 0x00010000L && flavor != 0x4F54544FL && flavor != 0x74727565L) {
            throw new IOException("WOFF2 flavor is not an sfnt version");
        }
        if (length != file.length) {
            throw new IOException("WOFF2 length field differs from the file size");
        }
        if (numTables == 0 || numTables > MAX_TABLES) {
            throw new IOException("WOFF2 table count is out of range: " + numTables);
        }
        Entry[] entries = readDirectory(h, numTables);
        long dataStart = h.pos;
        long compressedEnd = dataStart + compressedSize;
        if (compressedSize == 0 || compressedEnd > file.length) {
            throw new IOException("WOFF2 compressed data exceeds the file");
        }
        checkBlocks(file, compressedEnd, metaOffset, metaLength, privOffset, privLength);
        long total = 0;
        // 디렉터리가 알리는 풀린 블록의 크기를 합한다.
        for (int i = 0; i < entries.length; i++) {
            total += entries[i].dataLength;
            if (total > MAX_SFNT_SIZE) {
                throw new IOException("WOFF2 tables are larger than the limit");
            }
        }
        byte[] block = Brotli.decode(file, (int) dataStart, (int) compressedSize, (int) total);
        if (block.length != total) {
            throw new IOException("WOFF2 decompressed size differs from the sum of the table lengths");
        }
        int cursor = 0;
        // 풀린 블록에서 테이블별 데이터의 위치를 정한다.
        for (int i = 0; i < entries.length; i++) {
            entries[i].dataOffset = cursor;
            cursor += (int) entries[i].dataLength;
        }
        reconstruct(entries, block);
        return assemble(flavor, entries);
    }

    /**
     * Reads the table directory (section 4.1) and checks the transform versions and the order of <code>glyf</code> and <code>loca</code>.
     * <p>
     * 테이블 디렉터리(4.1 절)를 읽고 변환 버전과 <code>glyf</code>, <code>loca</code> 의 순서를 검사한다.
     *
     * @param r         the reader at the directory / 디렉터리의 읽기 도구
     * @param numTables the number of entries / 항목 수
     * @return the entries / 항목
     * @throws IOException if the directory is invalid / 디렉터리가 올바르지 않으면
     */
    private static Entry[] readDirectory(Reader r, int numTables) throws IOException {
        Entry[] entries = new Entry[numTables];
        int glyfIndex = -1;
        int locaIndex = -1;
        // 항목을 차례로 읽는다.
        for (int i = 0; i < numTables; i++) {
            Entry e = new Entry();
            int flags = r.u8();
            int code = flags & 0x3F;
            int version = flags >> 6;
            if (code == 63) {
                e.tag = r.u32();
            } else {
                e.tag = tag(KNOWN_TAGS[code]);
            }
            // 태그와 변환 버전 조합이 규격이 정한 변환인지 가린다. 모르는 버전이면 글꼴 전체를 거부한다.
            if (e.tag == TAG_GLYF || e.tag == TAG_LOCA) {
                if (version == 0) {
                    e.transformed = true;
                } else if (version != 3) {
                    throw new IOException("WOFF2 unknown transform version " + version + " of glyf or loca");
                }
            } else if (e.tag == TAG_HMTX) {
                if (version == 1) {
                    e.transformed = true;
                } else if (version != 0) {
                    throw new IOException("WOFF2 unknown transform version " + version + " of hmtx");
                }
            } else if (version != 0) {
                throw new IOException("WOFF2 unknown transform version " + version);
            }
            e.origLength = r.base128();
            e.dataLength = e.transformed ? r.base128() : e.origLength;
            if (e.tag == TAG_GLYF) {
                glyfIndex = i;
            } else if (e.tag == TAG_LOCA) {
                locaIndex = i;
            }
            // 같은 태그의 테이블이 둘이면 sfnt 로 만들 수 없다.
            for (int k = 0; k < i; k++) {
                if (entries[k].tag == e.tag) {
                    throw new IOException("WOFF2 table directory has a table twice");
                }
            }
            entries[i] = e;
        }
        if (glyfIndex >= 0 || locaIndex >= 0) {
            if (glyfIndex < 0 || locaIndex < 0 || entries[glyfIndex].transformed != entries[locaIndex].transformed) {
                throw new IOException("WOFF2 glyf and loca must both be present, both transformed or both not");
            }
            if (entries[glyfIndex].transformed && locaIndex < glyfIndex) {
                throw new IOException("WOFF2 loca must follow glyf in the table directory");
            }
            if (entries[locaIndex].transformed && entries[locaIndex].dataLength != 0) {
                throw new IOException("WOFF2 transformed loca must have the transform length zero");
            }
        }
        return entries;
    }

    /**
     * Checks that the compressed data, the extended metadata and the private data follow each other with at most three zero padding bytes and nothing else
     * (section 3: extraneous data and ranges beyond the file are errors).
     * <p>
     * 압축 데이터, 확장 메타데이터, 비공개 데이터가 0 인 패딩 바이트 최대 세 개만 두고 이어지고 다른 것은 없는지 검사한다(3 절: 불필요한 데이터와 파일 밖의 범위는 오류).
     *
     * @param file          the file / 파일
     * @param compressedEnd the end of the compressed data / 압축 데이터의 끝
     * @param metaOffset    the offset of the metadata, or 0 / 메타데이터의 오프셋 또는 0
     * @param metaLength    the length of the metadata / 메타데이터의 길이
     * @param privOffset    the offset of the private data, or 0 / 비공개 데이터의 오프셋 또는 0
     * @param privLength    the length of the private data / 비공개 데이터의 길이
     * @throws IOException if the layout is not valid / 배치가 올바르지 않으면
     */
    private static void checkBlocks(byte[] file, long compressedEnd, long metaOffset, long metaLength, long privOffset, long privLength) throws IOException {
        long end = compressedEnd;
        if (metaOffset != 0 || metaLength != 0) {
            checkPadding(file, end, metaOffset);
            if (metaLength == 0 || metaOffset + metaLength > file.length) {
                throw new IOException("WOFF2 metadata block exceeds the file");
            }
            end = metaOffset + metaLength;
        }
        if (privOffset != 0 || privLength != 0) {
            checkPadding(file, end, privOffset);
            if (privLength == 0 || privOffset + privLength > file.length) {
                throw new IOException("WOFF2 private data block exceeds the file");
            }
            end = privOffset + privLength;
        }
        if (file.length - end > 3) {
            throw new IOException("WOFF2 file has extraneous data after the last block");
        }
        // 마지막 블록 뒤의 패딩은 0 이어야 한다.
        for (long i = end; i < file.length; i++) {
            if (file[(int) i] != 0) {
                throw new IOException("WOFF2 file has extraneous data after the last block");
            }
        }
    }

    /**
     * Checks that a block starts at the next multiple of four after the previous block and that the bytes in between are zero.
     * <p>
     * 블록이 앞 블록 다음의 4 의 배수에서 시작하고 그 사이 바이트가 0 인지 검사한다.
     *
     * @param file  the file / 파일
     * @param end   the end of the previous block / 앞 블록의 끝
     * @param start the start of the block / 블록의 시작
     * @throws IOException if the position is wrong / 위치가 틀리면
     */
    private static void checkPadding(byte[] file, long end, long start) throws IOException {
        long aligned = (end + 3) & ~3L;
        if (start != aligned || start > file.length) {
            throw new IOException("WOFF2 block does not start at the next 4-byte boundary after the previous block");
        }
        // 앞 블록과 이 블록 사이의 패딩은 0 이어야 한다.
        for (long i = end; i < start; i++) {
            if (file[(int) i] != 0) {
                throw new IOException("WOFF2 padding between blocks is not zero");
            }
        }
    }

    /**
     * Fills the <code>table</code> of every entry: copies the tables without transform and reconstructs the transformed ones.
     * <p>
     * 모든 항목의 <code>table</code> 을 채운다: 변환 없는 테이블은 복사하고 변환된 테이블은 복원한다.
     *
     * @param entries the entries / 항목
     * @param block   the decompressed table data / 풀린 테이블 데이터
     * @throws IOException if a transformed table is invalid / 변환된 테이블이 올바르지 않으면
     */
    private static void reconstruct(Entry[] entries, byte[] block) throws IOException {
        Entry glyf = null;
        Entry loca = null;
        Entry hmtx = null;
        Entry hhea = null;
        Entry maxp = null;
        // 변환 없는 테이블을 복사하고 필요한 테이블을 찾아 둔다.
        for (int i = 0; i < entries.length; i++) {
            Entry e = entries[i];
            if (!e.transformed) {
                e.table = Arrays.copyOfRange(block, e.dataOffset, e.dataOffset + (int) e.dataLength);
            }
            if (e.tag == TAG_GLYF) {
                glyf = e;
            } else if (e.tag == TAG_LOCA) {
                loca = e;
            } else if (e.tag == TAG_HMTX) {
                hmtx = e;
            } else if (e.tag == TAG_HHEA) {
                hhea = e;
            } else if (e.tag == TAG_MAXP) {
                maxp = e;
            }
        }
        Glyphs glyphs = null;
        if (glyf != null && glyf.transformed) {
            glyphs = reconstructGlyf(block, glyf.dataOffset, glyf.dataOffset + (int) glyf.dataLength);
            long expectedLoca = glyphs.loca.length;
            if (loca.origLength != expectedLoca) {
                throw new IOException("WOFF2 loca length does not match the number of glyphs");
            }
            glyf.table = glyphs.glyf;
            loca.table = glyphs.loca;
        }
        if (hmtx != null && hmtx.transformed) {
            if (glyphs == null || hhea == null || maxp == null || hhea.table == null || maxp.table == null) {
                throw new IOException("WOFF2 transformed hmtx needs a transformed glyf, hhea and maxp");
            }
            hmtx.table = reconstructHmtx(block, hmtx.dataOffset, hmtx.dataOffset + (int) hmtx.dataLength, hhea.table, maxp.table, glyphs.xMin);
        }
    }

    /**
     * Rebuilds the glyph table and the location table from the transformed <code>glyf</code> table (section 5.1).
     * <p>
     * 변환된 <code>glyf</code> 테이블에서 글리프 테이블과 위치 테이블을 다시 만든다(5.1 절).
     *
     * @param data  the decompressed data / 풀린 데이터
     * @param start the start of the transformed table / 변환된 테이블의 시작
     * @param end   the end of the transformed table / 변환된 테이블의 끝
     * @return the tables and the xMin values / 테이블과 xMin 값
     * @throws IOException if the table is invalid / 테이블이 올바르지 않으면
     */
    private static Glyphs reconstructGlyf(byte[] data, int start, int end) throws IOException {
        Reader t = new Reader(data, start, end);
        t.u16();
        int optionFlags = t.u16();
        int numGlyphs = t.u16();
        int indexFormat = t.u16();
        if (indexFormat > 1) {
            throw new IOException("WOFF2 glyf index format is not 0 or 1");
        }
        long nContourSize = t.u32();
        long nPointsSize = t.u32();
        long flagSize = t.u32();
        long glyphSize = t.u32();
        long compositeSize = t.u32();
        long bboxSize = t.u32();
        long instructionSize = t.u32();
        Reader nContour = t.cut(nContourSize);
        Reader nPoints = t.cut(nPointsSize);
        Reader flagStream = t.cut(flagSize);
        Reader glyph = t.cut(glyphSize);
        Reader composite = t.cut(compositeSize);
        Reader bbox = t.cut(bboxSize);
        Reader instruction = t.cut(instructionSize);
        Reader overlap = null;
        if ((optionFlags & 1) != 0) {
            overlap = t.cut((numGlyphs + 7) / 8);
        }
        long bitmapSize = 4L * ((numGlyphs + 31) / 32);
        Reader bboxBitmap = bbox.cut(bitmapSize);
        Out out = new Out();
        int[] offsets = new int[numGlyphs + 1];
        int[] xMin = new int[numGlyphs];
        // 글리프를 하나씩 복원한다.
        for (int g = 0; g < numGlyphs; g++) {
            offsets[g] = out.len;
            int nc = nContour.i16();
            boolean hasBox = (bboxBitmap.b[bboxBitmap.pos + (g >> 3)] & (0x80 >> (g & 7))) != 0;
            if (nc == 0) {
                // 빈 글리프: 상자가 있으면 안 되고 데이터를 만들지 않는다.
                if (hasBox) {
                    throw new IOException("WOFF2 empty glyph " + g + " has a bounding box");
                }
            } else if (nc > 0) {
                boolean overlapFlag = overlap != null && (overlap.b[overlap.pos + (g >> 3)] & (0x80 >> (g & 7))) != 0;
                xMin[g] = simpleGlyph(out, nc, hasBox, overlapFlag, nPoints, flagStream, glyph, bbox, instruction);
            } else if (nc == -1) {
                // 합성 글리프: 상자가 반드시 있어야 한다.
                if (!hasBox) {
                    throw new IOException("WOFF2 composite glyph " + g + " has no bounding box");
                }
                xMin[g] = compositeGlyph(out, composite, glyph, bbox, instruction);
            } else {
                throw new IOException("WOFF2 glyph " + g + " has an invalid contour count " + nc);
            }
            out.align4();
        }
        offsets[numGlyphs] = out.len;
        Glyphs r = new Glyphs();
        r.glyf = out.toBytes();
        r.xMin = xMin;
        int entry = indexFormat == 0 ? 2 : 4;
        r.loca = new byte[(numGlyphs + 1) * entry];
        // 위치 테이블을 선택한 형식으로 쓴다. 짧은 형식은 오프셋을 2 로 나눠 16비트에 담는다.
        for (int i = 0; i <= numGlyphs; i++) {
            if (indexFormat == 0) {
                if (offsets[i] > 2 * 0xFFFF) {
                    throw new IOException("WOFF2 glyph table is too large for the short loca format");
                }
                int v = offsets[i] / 2;
                r.loca[2 * i] = (byte) (v >> 8);
                r.loca[2 * i + 1] = (byte) v;
            } else {
                int v = offsets[i];
                r.loca[4 * i] = (byte) (v >> 24);
                r.loca[4 * i + 1] = (byte) (v >> 16);
                r.loca[4 * i + 2] = (byte) (v >> 8);
                r.loca[4 * i + 3] = (byte) v;
            }
        }
        return r;
    }

    /**
     * Rebuilds a simple glyph (section 5.1 and 5.2) and appends it to the glyph table.
     * <p>
     * 단순 글리프를 다시 만들어(5.1, 5.2 절) 글리프 테이블에 덧붙인다.
     *
     * @param out         the glyph table / 글리프 테이블
     * @param contours    the number of contours / 윤곽선 수
     * @param hasBox      whether the bounding box is in the box stream / 경계 상자가 상자 스트림에 있는지 여부
     * @param overlapFlag whether the overlap flag is set / 겹침 플래그가 켜졌는지 여부
     * @param nPoints     the stream of the point counts / 점 수 스트림
     * @param flags       the flag stream / 플래그 스트림
     * @param glyph       the glyph stream / 글리프 스트림
     * @param bbox        the box stream / 상자 스트림
     * @param instruction the instruction stream / 명령 스트림
     * @return the xMin of the glyph / 글리프의 xMin
     * @throws IOException if the data is invalid / 데이터가 올바르지 않으면
     */
    private static int simpleGlyph(Out out, int contours, boolean hasBox, boolean overlapFlag, Reader nPoints, Reader flags, Reader glyph, Reader bbox, Reader instruction)
            throws IOException {
        int[] endPts = new int[contours];
        int total = 0;
        // 윤곽선마다 점 수를 읽어 끝 점 인덱스로 바꾼다.
        for (int c = 0; c < contours; c++) {
            int n = nPoints.read255();
            if (n == 0) {
                throw new IOException("WOFF2 contour without points");
            }
            total += n;
            if (total > 0xFFFF) {
                throw new IOException("WOFF2 glyph has too many points");
            }
            endPts[c] = total - 1;
        }
        int[] xs = new int[total];
        int[] ys = new int[total];
        boolean[] on = new boolean[total];
        int x = 0;
        int y = 0;
        // 점마다 플래그와 좌표 바이트를 읽어 삼중 부호를 푼다.
        for (int i = 0; i < total; i++) {
            int flag = flags.u8();
            on[i] = (flag & 0x80) == 0;
            flag &= 0x7F;
            int dx;
            int dy;
            if (flag < 10) {
                dx = 0;
                dy = withSign(flag, ((flag & 14) << 7) + glyph.u8());
            } else if (flag < 20) {
                dx = withSign(flag, (((flag - 10) & 14) << 7) + glyph.u8());
                dy = 0;
            } else if (flag < 84) {
                int b0 = flag - 20;
                int b1 = glyph.u8();
                dx = withSign(flag, 1 + (b0 & 0x30) + (b1 >> 4));
                dy = withSign(flag >> 1, 1 + ((b0 & 0x0C) << 2) + (b1 & 0x0F));
            } else if (flag < 120) {
                int b0 = flag - 84;
                int b1 = glyph.u8();
                int b2 = glyph.u8();
                dx = withSign(flag, 1 + ((b0 / 12) << 8) + b1);
                dy = withSign(flag >> 1, 1 + (((b0 % 12) >> 2) << 8) + b2);
            } else if (flag < 124) {
                int b1 = glyph.u8();
                int b2 = glyph.u8();
                int b3 = glyph.u8();
                dx = withSign(flag, (b1 << 4) + (b2 >> 4));
                dy = withSign(flag >> 1, ((b2 & 0x0F) << 8) + b3);
            } else {
                int b1 = glyph.u8();
                int b2 = glyph.u8();
                int b3 = glyph.u8();
                int b4 = glyph.u8();
                dx = withSign(flag, (b1 << 8) + b2);
                dy = withSign(flag >> 1, (b3 << 8) + b4);
            }
            x += dx;
            y += dy;
            if (x < -32768 || x > 32767 || y < -32768 || y > 32767 || dx < -32768 || dx > 32767 || dy < -32768 || dy > 32767) {
                throw new IOException("WOFF2 glyph point leaves the 16 bit range");
            }
            xs[i] = x;
            ys[i] = y;
        }
        int instructionLength = glyph.read255();
        byte[] instructions = instruction.bytes(instructionLength);
        int xMin = Integer.MAX_VALUE;
        int yMin = Integer.MAX_VALUE;
        int xMax = Integer.MIN_VALUE;
        int yMax = Integer.MIN_VALUE;
        // 점 전체의 경계 상자를 구한다.
        for (int i = 0; i < total; i++) {
            xMin = Math.min(xMin, xs[i]);
            xMax = Math.max(xMax, xs[i]);
            yMin = Math.min(yMin, ys[i]);
            yMax = Math.max(yMax, ys[i]);
        }
        if (hasBox) {
            xMin = bbox.i16();
            yMin = bbox.i16();
            xMax = bbox.i16();
            yMax = bbox.i16();
        }
        out.put16(contours);
        out.put16(xMin);
        out.put16(yMin);
        out.put16(xMax);
        out.put16(yMax);
        // 윤곽선 끝 점 인덱스를 쓴다.
        for (int c = 0; c < contours; c++) {
            out.put16(endPts[c]);
        }
        out.put16(instructionLength);
        out.put(instructions);
        writePoints(out, xs, ys, on, overlapFlag);
        return xMin;
    }

    /**
     * Writes the flags and the coordinates of the points of a simple glyph in the format of the TrueType glyph table (repeated flags are packed).
     * <p>
     * 단순 글리프 점의 플래그와 좌표를 TrueType 글리프 테이블 형식으로 쓴다(반복되는 플래그는 묶는다).
     *
     * @param out         the glyph table / 글리프 테이블
     * @param xs          the x coordinates / x 좌표
     * @param ys          the y coordinates / y 좌표
     * @param on          the on-curve markers / 곡선 위 점 표시
     * @param overlapFlag whether the first flag gets the overlap bit / 첫 플래그에 겹침 비트를 줄지 여부
     * @throws IOException if the size limit is exceeded / 크기 한도를 넘으면
     */
    private static void writePoints(Out out, int[] xs, int[] ys, boolean[] on, boolean overlapFlag) throws IOException {
        int n = xs.length;
        int[] fl = new int[n];
        int px = 0;
        int py = 0;
        // 점마다 TrueType 플래그를 정한다: 0x01 곡선 위, 0x02/0x04 짧은 x/y, 0x10/0x20 같은 값이거나 양수.
        for (int i = 0; i < n; i++) {
            int dx = xs[i] - px;
            int dy = ys[i] - py;
            int f = on[i] ? 1 : 0;
            if (dx == 0) {
                f |= 0x10;
            } else if (dx >= -255 && dx <= 255) {
                f |= 0x02 | (dx > 0 ? 0x10 : 0);
            }
            if (dy == 0) {
                f |= 0x20;
            } else if (dy >= -255 && dy <= 255) {
                f |= 0x04 | (dy > 0 ? 0x20 : 0);
            }
            if (i == 0 && overlapFlag) {
                f |= 0x40;
            }
            fl[i] = f;
            px = xs[i];
            py = ys[i];
        }
        int i = 0;
        // 같은 플래그가 이어지면 반복 플래그(0x08)로 묶는다.
        while (i < n) {
            int f = fl[i];
            int j = i + 1;
            // 같은 플래그가 이어지는 길이를 센다(반복 플래그가 나타낼 수 있는 최대 256 개).
            while (j < n && fl[j] == f && j - i < 256) {
                j++;
            }
            if (j - i >= 2) {
                out.put8(f | 0x08);
                out.put8(j - i - 1);
            } else {
                out.put8(f);
            }
            i = j;
        }
        px = 0;
        // x 좌표 차이를 쓴다.
        for (int k = 0; k < n; k++) {
            int dx = xs[k] - px;
            if (dx != 0) {
                if (dx >= -255 && dx <= 255) {
                    out.put8(Math.abs(dx));
                } else {
                    out.put16(dx);
                }
            }
            px = xs[k];
        }
        py = 0;
        // y 좌표 차이를 쓴다.
        for (int k = 0; k < n; k++) {
            int dy = ys[k] - py;
            if (dy != 0) {
                if (dy >= -255 && dy <= 255) {
                    out.put8(Math.abs(dy));
                } else {
                    out.put16(dy);
                }
            }
            py = ys[k];
        }
    }

    /**
     * Applies the sign of the lowest bit of a triplet flag to a magnitude (section 5.2: an odd flag is positive).
     * <p>
     * 삼중 플래그의 가장 낮은 비트가 정한 부호를 크기에 적용한다(5.2 절: 홀수 플래그는 양수).
     *
     * @param flag the flag (or the flag shifted right for the y value) / 플래그(y 값에는 오른쪽으로 민 플래그)
     * @param v    the magnitude / 크기
     * @return the signed value / 부호 있는 값
     */
    private static int withSign(int flag, int v) {
        return (flag & 1) != 0 ? v : -v;
    }

    /**
     * Rebuilds a composite glyph (section 5.1) and appends it to the glyph table.
     * <p>
     * 합성 글리프를 다시 만들어(5.1 절) 글리프 테이블에 덧붙인다.
     *
     * @param out         the glyph table / 글리프 테이블
     * @param composite   the composite stream / 합성 스트림
     * @param glyph       the glyph stream (the instruction length) / 글리프 스트림(명령 길이)
     * @param bbox        the box stream / 상자 스트림
     * @param instruction the instruction stream / 명령 스트림
     * @return the xMin of the glyph / 글리프의 xMin
     * @throws IOException if the data is invalid / 데이터가 올바르지 않으면
     */
    private static int compositeGlyph(Out out, Reader composite, Reader glyph, Reader bbox, Reader instruction) throws IOException {
        int xMin = bbox.i16();
        int yMin = bbox.i16();
        int xMax = bbox.i16();
        int yMax = bbox.i16();
        out.put16(0xFFFF);
        out.put16(xMin);
        out.put16(yMin);
        out.put16(xMax);
        out.put16(yMax);
        boolean haveInstructions = false;
        int flags;
        // 구성 요소를 MORE_COMPONENTS 플래그가 꺼질 때까지 복사한다.
        do {
            flags = composite.u16();
            int index = composite.u16();
            out.put16(flags);
            out.put16(index);
            int size = (flags & 0x0001) != 0 ? 4 : 2;
            if ((flags & 0x0008) != 0) {
                size += 2;
            } else if ((flags & 0x0040) != 0) {
                size += 4;
            } else if ((flags & 0x0080) != 0) {
                size += 8;
            }
            out.put(composite.bytes(size));
            if ((flags & 0x0100) != 0) {
                haveInstructions = true;
            }
        } while ((flags & 0x0020) != 0);
        if (haveInstructions) {
            int n = glyph.read255();
            out.put16(n);
            out.put(instruction.bytes(n));
        }
        return xMin;
    }

    /**
     * Rebuilds the horizontal metrics table from the transformed <code>hmtx</code> table (section 5.4): the left side bearings that were left out are the xMin
     * values of the glyph boxes.
     * <p>
     * 변환된 <code>hmtx</code> 테이블에서 가로 지표 테이블을 다시 만든다(5.4 절): 생략된 왼쪽 여백은 글리프 상자의 xMin 값이다.
     *
     * @param data  the decompressed data / 풀린 데이터
     * @param start the start of the transformed table / 변환된 테이블의 시작
     * @param end   the end of the transformed table / 변환된 테이블의 끝
     * @param hhea  the <code>hhea</code> table / <code>hhea</code> 테이블
     * @param maxp  the <code>maxp</code> table / <code>maxp</code> 테이블
     * @param xMin  the xMin of the glyphs / 글리프의 xMin
     * @return the <code>hmtx</code> table / <code>hmtx</code> 테이블
     * @throws IOException if the table is invalid / 테이블이 올바르지 않으면
     */
    private static byte[] reconstructHmtx(byte[] data, int start, int end, byte[] hhea, byte[] maxp, int[] xMin) throws IOException {
        if (hhea.length < 36 || maxp.length < 6) {
            throw new IOException("WOFF2 hhea or maxp is too short for the hmtx transform");
        }
        int numGlyphs = ((maxp[4] & 0xFF) << 8) | (maxp[5] & 0xFF);
        int numMetrics = ((hhea[34] & 0xFF) << 8) | (hhea[35] & 0xFF);
        if (numGlyphs != xMin.length || numMetrics == 0 || numMetrics > numGlyphs) {
            throw new IOException("WOFF2 hmtx transform: glyph and metric counts do not match");
        }
        Reader r = new Reader(data, start, end);
        int flags = r.u8();
        if ((flags & 3) == 0 || (flags & 0xFC) != 0) {
            throw new IOException("WOFF2 hmtx transform flags are invalid");
        }
        byte[] out = new byte[4 * numMetrics + 2 * (numGlyphs - numMetrics)];
        int[] advance = new int[numMetrics];
        // 먼저 진행 폭 배열을 읽는다.
        for (int i = 0; i < numMetrics; i++) {
            advance[i] = r.u16();
        }
        int[] lsb = new int[numGlyphs];
        // 비트 0: 비례 글리프의 lsb 배열이 없으면 xMin 을 쓴다.
        for (int i = 0; i < numMetrics; i++) {
            lsb[i] = (flags & 1) != 0 ? xMin[i] : r.i16();
        }
        // 비트 1: 나머지 글리프의 왼쪽 여백 배열이 없으면 xMin 을 쓴다.
        for (int i = numMetrics; i < numGlyphs; i++) {
            lsb[i] = (flags & 2) != 0 ? xMin[i] : r.i16();
        }
        if (r.pos != end) {
            throw new IOException("WOFF2 hmtx transform has extra data");
        }
        int p = 0;
        // 지표를 sfnt hmtx 형식으로 쓴다.
        for (int i = 0; i < numGlyphs; i++) {
            if (i < numMetrics) {
                out[p++] = (byte) (advance[i] >> 8);
                out[p++] = (byte) advance[i];
            }
            out[p++] = (byte) (lsb[i] >> 8);
            out[p++] = (byte) lsb[i];
        }
        return out;
    }

    /**
     * Builds the sfnt file: the table directory sorted by tag, the tables on 4-byte boundaries, the checksums and the <code>checkSumAdjustment</code>.
     * <p>
     * sfnt 파일을 만든다: 태그 순으로 정렬한 테이블 디렉터리, 4 바이트 경계의 테이블, 체크섬, <code>checkSumAdjustment</code>.
     *
     * @param flavor  the sfnt version / sfnt 버전
     * @param entries the entries with their tables / 테이블이 있는 항목
     * @return the sfnt file / sfnt 파일
     * @throws IOException if the file would be larger than the limit / 파일이 한도보다 커지면
     */
    private static byte[] assemble(long flavor, Entry[] entries) throws IOException {
        int n = entries.length;
        Entry[] sorted = entries.clone();
        Arrays.sort(sorted, new java.util.Comparator<Entry>() {
            /**
             * Compares the tags as unsigned numbers.
             * <p>
             * 태그를 부호 없는 수로 비교한다.
             *
             * @param a the first / 첫째
             * @param b the second / 둘째
             * @return the order / 순서
             */
            public int compare(Entry a, Entry b) {
                return a.tag < b.tag ? -1 : (a.tag > b.tag ? 1 : 0);
            }
        });
        long size = 12L + 16L * n;
        // 파일 전체 크기를 구해 한도를 확인한다.
        for (int i = 0; i < n; i++) {
            size += (sorted[i].table.length + 3L) & ~3L;
        }
        if (size > MAX_SFNT_SIZE) {
            throw new IOException("WOFF2 font is larger than the limit");
        }
        byte[] f = new byte[(int) size];
        put32(f, 0, flavor);
        put16(f, 4, n);
        int entrySelector = 0;
        // 테이블 수 이하의 가장 큰 2 의 거듭제곱을 구한다.
        while ((1 << (entrySelector + 1)) <= n) {
            entrySelector++;
        }
        int searchRange = (1 << entrySelector) * 16;
        put16(f, 6, searchRange);
        put16(f, 8, entrySelector);
        put16(f, 10, n * 16 - searchRange);
        int pos = 12 + 16 * n;
        int headOffset = -1;
        // 테이블을 놓고 디렉터리 항목을 쓴다. head 의 checkSumAdjustment 는 0 으로 두고 체크섬을 구한다.
        for (int i = 0; i < n; i++) {
            Entry e = sorted[i];
            byte[] t = e.table;
            System.arraycopy(t, 0, f, pos, t.length);
            if (e.tag == TAG_HEAD && t.length >= 12) {
                headOffset = pos;
                put32(f, pos + 8, 0);
            }
            int d = 12 + 16 * i;
            put32(f, d, e.tag);
            put32(f, d + 4, checksum(f, pos, t.length));
            put32(f, d + 8, pos);
            put32(f, d + 12, t.length);
            pos += (t.length + 3) & ~3;
        }
        if (headOffset >= 0) {
            long adjustment = (0xB1B0AFBAL - checksum(f, 0, f.length)) & 0xFFFFFFFFL;
            put32(f, headOffset + 8, adjustment);
        }
        return f;
    }

    /**
     * Computes the sfnt checksum of a range: the sum of its 32 bit big-endian words, the last one padded with zeros, modulo 2^32.
     * <p>
     * 범위의 sfnt 체크섬을 계산한다: 32비트 빅 엔디언 단어의 합이며 마지막 단어는 0 으로 채우고 2^32 로 나눈 나머지이다.
     *
     * @param b      the array / 배열
     * @param offset the start / 시작
     * @param length the length / 길이
     * @return the checksum / 체크섬
     */
    static long checksum(byte[] b, int offset, int length) {
        long sum = 0;
        // 4 바이트씩 더한다. 마지막 단어는 모자란 바이트를 0 으로 본다.
        for (int i = 0; i < length; i += 4) {
            long w = 0;
            // 단어의 네 바이트를 모은다(범위를 넘는 바이트는 0 으로 본다).
            for (int k = 0; k < 4; k++) {
                w <<= 8;
                if (i + k < length) {
                    w |= b[offset + i + k] & 0xFF;
                }
            }
            sum += w;
        }
        return sum & 0xFFFFFFFFL;
    }

    /**
     * Writes a big-endian 16 bit number.
     * <p>
     * 빅 엔디언 16비트 수를 쓴다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @param v the number / 수
     */
    private static void put16(byte[] b, int i, int v) {
        b[i] = (byte) (v >> 8);
        b[i + 1] = (byte) v;
    }

    /**
     * Writes a big-endian 32 bit number.
     * <p>
     * 빅 엔디언 32비트 수를 쓴다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @param v the number / 수
     */
    private static void put32(byte[] b, int i, long v) {
        b[i] = (byte) (v >> 24);
        b[i + 1] = (byte) (v >> 16);
        b[i + 2] = (byte) (v >> 8);
        b[i + 3] = (byte) v;
    }
}
