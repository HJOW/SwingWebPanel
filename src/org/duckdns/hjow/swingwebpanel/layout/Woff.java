package org.duckdns.hjow.swingwebpanel.layout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * The decoder of WOFF 1.0 web fonts (plan item P10-05, https://www.w3.org/TR/WOFF/, W3C Recommendation 2012-12-13): a
 * WOFF file is an sfnt (TrueType or OpenType) font whose tables are compressed one by one with zlib. {@link #toSfnt(byte[])} rebuilds the
 * plain sfnt file with {@link Inflater} (a public JRE class), so that the font can be read like a TrueType font. The header, the table
 * directory (offsets, lengths, the order of the tables, the size limits, the decompressed lengths) are checked; a table whose compressed
 * length equals the original length is stored without compression. The metadata and private blocks are ignored. WOFF 2 (Brotli) is another format with another signature: it is read by {@link org.duckdns.hjow.swingwebpanel.media.font.Woff2}
 * and {@link #isWoff2(byte[])} only detects it.
 * <p>
 * WOFF 1.0 웹 글꼴 디코더(계획 P10-05, https://www.w3.org/TR/WOFF/, W3C 권고안 2012-12-13): WOFF 파일은 테이블마다 zlib 로 압축한 sfnt(TrueType 또는
 * OpenType) 글꼴이다. {@link #toSfnt(byte[])} 는 JRE 공개 클래스 {@link Inflater} 로 압축을 풀어 평범한 sfnt 파일을 다시 만들므로 TrueType 글꼴처럼 읽을 수 있다.
 * 헤더와 테이블 디렉터리(오프셋, 길이, 테이블 순서, 크기 한도, 풀린 길이)를 검사한다. 압축 길이가 원래 길이와 같은 테이블은 압축하지 않고 저장한 것이다. 메타데이터와 비공개
 * 블록은 무시한다. WOFF 2(Brotli)는 서명이 다른 별개의 형식이며 {@link org.duckdns.hjow.swingwebpanel.media.font.Woff2} 가 읽고 {@link #isWoff2(byte[])} 는 알아내기만 한다.
 */
public final class Woff {

    /** The largest sfnt file that is rebuilt, 16 MiB. 다시 만드는 sfnt 파일의 최대 크기. 16 MiB. */
    public static final int MAX_SFNT_SIZE = 16 * 1024 * 1024;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Woff() {
    }

    /**
     * Tells whether the bytes start with the WOFF 1.0 signature <code>wOFF</code>.
     * <p>
     * 바이트가 WOFF 1.0 서명 <code>wOFF</code> 로 시작하는지 알려 준다.
     *
     * @param b the file / 파일
     * @return true for WOFF 1.0 / WOFF 1.0 이면 true
     */
    public static boolean isWoff(byte[] b) {
        return b != null && b.length >= 4 && b[0] == 'w' && b[1] == 'O' && b[2] == 'F' && b[3] == 'F';
    }

    /**
     * Tells whether the bytes start with the WOFF 2 signature <code>wOF2</code> (read by {@link org.duckdns.hjow.swingwebpanel.media.font.Woff2}).
     * <p>
     * 바이트가 WOFF 2 서명 <code>wOF2</code> 로 시작하는지 알려 준다({@link org.duckdns.hjow.swingwebpanel.media.font.Woff2} 가 읽음).
     *
     * @param b the file / 파일
     * @return true for WOFF 2 / WOFF 2 이면 true
     */
    public static boolean isWoff2(byte[] b) {
        return b != null && b.length >= 4 && b[0] == 'w' && b[1] == 'O' && b[2] == 'F' && b[3] == '2';
    }

    /**
     * Tells whether the bytes look like an sfnt font (TrueType <code>0x00010000</code> or <code>true</code>, OpenType <code>OTTO</code>).
     * <p>
     * 바이트가 sfnt 글꼴(TrueType <code>0x00010000</code> 또는 <code>true</code>, OpenType <code>OTTO</code>)처럼 보이는지 알려 준다.
     *
     * @param b the file / 파일
     * @return true for an sfnt / sfnt 이면 true
     */
    public static boolean isSfnt(byte[] b) {
        if (b == null || b.length < 12) {
            return false;
        }
        return (b[0] == 0 && b[1] == 1 && b[2] == 0 && b[3] == 0) || (b[0] == 't' && b[1] == 'r' && b[2] == 'u' && b[3] == 'e')
                || (b[0] == 'O' && b[1] == 'T' && b[2] == 'T' && b[3] == 'O');
    }

    /**
     * Rebuilds the sfnt font of a WOFF 1.0 file.
     * <p>
     * WOFF 1.0 파일의 sfnt 글꼴을 다시 만든다.
     *
     * @param woff the WOFF file / WOFF 파일
     * @return the sfnt file / sfnt 파일
     * @throws IOException if the file is not a valid WOFF 1.0 file or a limit is exceeded / 올바른 WOFF 1.0 파일이 아니거나 한도를 넘으면
     */
    public static byte[] toSfnt(byte[] woff) throws IOException {
        if (!isWoff(woff) || woff.length < 44) {
            throw new IOException("not a WOFF 1.0 file");
        }
        long declaredLength = u32(woff, 8);
        int numTables = u16(woff, 12);
        long totalSfntSize = u32(woff, 16);
        if (declaredLength != woff.length || numTables == 0 || totalSfntSize > MAX_SFNT_SIZE || totalSfntSize < 12 + 16L * numTables) {
            throw new IOException("bad WOFF header");
        }
        if (44L + 20L * numTables > woff.length) {
            throw new IOException("WOFF table directory is cut off");
        }
        int out = (int) totalSfntSize;
        byte[] sfnt = new byte[out];
        System.arraycopy(woff, 4, sfnt, 0, 4);
        put16(sfnt, 4, numTables);
        int entrySelector = 0;
        // 테이블 수 이하의 가장 큰 2 의 거듭제곱을 구한다.
        while ((1 << (entrySelector + 1)) <= numTables) {
            entrySelector++;
        }
        int searchRange = (1 << entrySelector) * 16;
        put16(sfnt, 6, searchRange);
        put16(sfnt, 8, entrySelector);
        put16(sfnt, 10, numTables * 16 - searchRange);
        long pos = 12L + 16L * numTables;
        long lastTag = -1;
        // 테이블마다 데이터를 풀어 sfnt 에 4 바이트 경계로 놓고 디렉터리 항목을 쓴다.
        for (int i = 0; i < numTables; i++) {
            int d = 44 + 20 * i;
            long tag = u32(woff, d);
            long offset = u32(woff, d + 4);
            long compLength = u32(woff, d + 8);
            long origLength = u32(woff, d + 12);
            long checksum = u32(woff, d + 16);
            if (tag <= lastTag || compLength > origLength || offset + compLength > woff.length || offset < 44L + 20L * numTables) {
                throw new IOException("bad WOFF table entry " + i);
            }
            lastTag = tag;
            if (pos + origLength > out) {
                throw new IOException("WOFF tables are larger than the declared sfnt size");
            }
            byte[] data = origLength == compLength ? slice(woff, (int) offset, (int) compLength) : inflate(woff, (int) offset, (int) compLength, (int) origLength);
            int e = 12 + 16 * i;
            System.arraycopy(woff, d, sfnt, e, 4);
            put32(sfnt, e + 4, checksum);
            put32(sfnt, e + 8, pos);
            put32(sfnt, e + 12, origLength);
            System.arraycopy(data, 0, sfnt, (int) pos, data.length);
            pos += (origLength + 3) & ~3L;
        }
        return sfnt;
    }

    /**
     * Compresses the tables of an sfnt font into a WOFF 1.0 file (for tests and tools: the product only reads WOFF). A table is stored
     * compressed only if that is smaller.
     * <p>
     * sfnt 글꼴의 테이블을 압축해 WOFF 1.0 파일로 만든다(시험과 도구용. 제품은 WOFF 를 읽기만 한다). 테이블은 더 작아질 때만 압축해 저장한다.
     *
     * @param sfnt the sfnt file / sfnt 파일
     * @return the WOFF file / WOFF 파일
     * @throws IOException if the sfnt is cut off / sfnt 가 잘렸으면
     */
    public static byte[] fromSfnt(byte[] sfnt) throws IOException {
        if (!isSfnt(sfnt)) {
            throw new IOException("not an sfnt font");
        }
        int n = u16(sfnt, 4);
        byte[][] comp = new byte[n][];
        long dataStart = 44 + 20L * n;
        long pos = dataStart;
        // 테이블마다 압축해서 더 작으면 압축본을 쓴다.
        for (int i = 0; i < n; i++) {
            int e = 12 + 16 * i;
            int offset = (int) u32(sfnt, e + 8);
            int length = (int) u32(sfnt, e + 12);
            if (offset < 0 || length < 0 || (long) offset + length > sfnt.length) {
                throw new IOException("sfnt table is cut off");
            }
            byte[] raw = slice(sfnt, offset, length);
            java.util.zip.Deflater def = new java.util.zip.Deflater(9);
            def.setInput(raw);
            def.finish();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            // 압축기가 끝날 때까지 출력을 모은다.
            while (!def.finished()) {
                int k = def.deflate(buf);
                bos.write(buf, 0, k);
            }
            def.end();
            comp[i] = bos.size() < raw.length ? bos.toByteArray() : raw;
        }
        long total = dataStart;
        // 압축한 테이블의 총 크기(4 바이트 경계)를 구한다.
        for (int i = 0; i < n; i++) {
            total += (comp[i].length + 3) & ~3L;
        }
        byte[] out = new byte[(int) total];
        System.arraycopy(new byte[] { 'w', 'O', 'F', 'F' }, 0, out, 0, 4);
        System.arraycopy(sfnt, 0, out, 4, 4);
        put32(out, 8, total);
        put16(out, 12, n);
        put32(out, 16, sfnt.length);
        // 테이블 디렉터리 항목과 데이터를 쓴다.
        for (int i = 0; i < n; i++) {
            int e = 12 + 16 * i;
            int d = 44 + 20 * i;
            System.arraycopy(sfnt, e, out, d, 4);
            put32(out, d + 4, pos);
            put32(out, d + 8, comp[i].length);
            put32(out, d + 12, u32(sfnt, e + 12));
            put32(out, d + 16, u32(sfnt, e + 4));
            System.arraycopy(comp[i], 0, out, (int) pos, comp[i].length);
            pos += (comp[i].length + 3) & ~3L;
        }
        return out;
    }

    /**
     * Inflates one zlib-compressed table.
     * <p>
     * zlib 로 압축한 테이블 하나를 푼다.
     *
     * @param b      the file / 파일
     * @param offset the start / 시작
     * @param length the compressed length / 압축 길이
     * @param orig   the expected length / 기대하는 길이
     * @return the data / 데이터
     * @throws IOException if the data is invalid or has another length / 데이터가 올바르지 않거나 길이가 다르면
     */
    private static byte[] inflate(byte[] b, int offset, int length, int orig) throws IOException {
        Inflater inf = new Inflater();
        try {
            inf.setInput(b, offset, length);
            byte[] out = new byte[orig];
            int n = 0;
            // 필요한 길이를 채울 때까지 푼다.
            while (n < orig) {
                int k = inf.inflate(out, n, orig - n);
                if (k == 0 && (inf.finished() || inf.needsInput() || inf.needsDictionary())) {
                    break;
                }
                n += k;
            }
            if (n != orig || !inf.finished()) {
                throw new IOException("WOFF table has another decompressed length");
            }
            return out;
        } catch (DataFormatException e) {
            throw new IOException("WOFF table data is corrupt: " + e.getMessage());
        } finally {
            inf.end();
        }
    }

    /**
     * Copies a part of an array.
     * <p>
     * 배열의 일부를 복사한다.
     *
     * @param b      the array / 배열
     * @param offset the start / 시작
     * @param length the length / 길이
     * @return the copy / 사본
     */
    private static byte[] slice(byte[] b, int offset, int length) {
        byte[] r = new byte[length];
        System.arraycopy(b, offset, r, 0, length);
        return r;
    }

    /**
     * Reads a big-endian 16 bit number.
     * <p>
     * 빅 엔디언 16비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    private static int u16(byte[] b, int i) {
        return ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
    }

    /**
     * Reads a big-endian 32 bit unsigned number.
     * <p>
     * 빅 엔디언 부호 없는 32비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    private static long u32(byte[] b, int i) {
        return ((long) (b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
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
