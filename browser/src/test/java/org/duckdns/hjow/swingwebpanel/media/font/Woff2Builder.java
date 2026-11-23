package org.duckdns.hjow.swingwebpanel.media.font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A WOFF2 encoder for the tests (plan item P10-08-e): it turns a model of a font (glyphs with points, composite glyphs, instructions, boxes, overlap flags, metrics)
 * into a WOFF2 file by the rules of the W3C Recommendation "WOFF File Format 2.0" (https://www.w3.org/TR/WOFF2/): the table directory with known-tag codes and
 * variable-length numbers, the transformed <code>glyf</code> table with its seven streams, the triplet coordinates (the 128 rows of section 5.2 are generated here as
 * a table of byte counts, bits, deltas and signs, while the decoder under test uses closed formulas), the null-transformed <code>loca</code>, and the transformed
 * <code>hmtx</code>. The table data is stored in a Brotli stream that consists of uncompressed meta-blocks only (a valid stream that every Brotli decoder accepts), so
 * that the tests can change single bytes of the tables and of the directory and so that the compressed data of the real fonts (made with the reference Brotli
 * encoder by the script <code>make-woff2-vectors.js</code>) is tested apart from the structure of WOFF2. The pieces (directory, table stream) can be taken and put
 * together again with changes to build invalid files. The encoder and the decoder were both written from the same specification text, so this is no independent proof
 * of the reading of the specification (see {@link Woff2}).
 * <p>
 * 시험용 WOFF2 인코더(계획 P10-08-e): 글꼴의 모델(점이 있는 글리프, 합성 글리프, 명령, 상자, 겹침 플래그, 지표)을 W3C 권고안 "WOFF File Format 2.0"
 * (https://www.w3.org/TR/WOFF2/)의 규칙대로 WOFF2 파일로 만든다: 알려진 태그 코드와 가변 길이 수가 있는 테이블 디렉터리, 스트림 일곱 개의 변환된 <code>glyf</code>
 * 테이블, 삼중 좌표(5.2 절의 128 개 행을 여기서는 바이트 수, 비트, 변화량, 부호의 표로 만들고 시험 대상 디코더는 닫힌 식을 쓴다), 널 변환한 <code>loca</code>, 변환된
 * <code>hmtx</code>. 테이블 데이터는 압축하지 않은 메타 블록만으로 된 Brotli 스트림(모든 Brotli 디코더가 받아들이는 올바른 스트림)에 담으므로 시험이 테이블과 디렉터리의 바이트를
 * 하나씩 바꿀 수 있고, 실제 글꼴의 압축 데이터(스크립트 <code>make-woff2-vectors.js</code> 가 참조 Brotli 인코더로 만든 것)는 WOFF2 구조와 따로 시험된다. 조각(디렉터리, 테이블
 * 스트림)을 꺼내 바꿔서 다시 맞추면 잘못된 파일을 만들 수 있다. 인코더와 디코더를 같은 규격 텍스트로 썼으므로 규격 해석의 독립적인 증명은 아니다({@link Woff2} 참조).
 */
final class Woff2Builder {

    /** The model of a glyph. 글리프의 모델. */
    static final class Glyph {
        /** 0 empty, 1 simple, 2 composite. 0 빈 글리프, 1 단순, 2 합성. */
        int kind;
        /** The index of the last point of each contour (simple glyphs). 윤곽선별 마지막 점의 인덱스(단순 글리프). */
        int[] endPts;
        /** The absolute x coordinates. 절대 x 좌표. */
        int[] xs;
        /** The absolute y coordinates. 절대 y 좌표. */
        int[] ys;
        /** Whether a point is on the curve. 점이 곡선 위에 있는지 여부. */
        boolean[] on;
        /** The triplet row to use for each point, or null for the shortest; an entry below 0 means the shortest. 점별로 쓸 삼중 행. 가장 짧은 것이면 null 이고, 0 미만의 항목은 가장 짧은 행. */
        int[] forced;
        /** The instructions. 명령. */
        byte[] instr = new byte[0];
        /** The overlap flag of a simple glyph. 단순 글리프의 겹침 플래그. */
        boolean overlap;
        /** An explicit bounding box {xMin, yMin, xMax, yMax}, or null (a simple glyph then gets the box of its points). 명시한 경계 상자 {xMin, yMin, xMax, yMax}. 없으면 null(단순 글리프는 점의 상자를 쓴다). */
        int[] box;
        /** The component records of a composite glyph (flags, glyph index, arguments, scale of each component). 합성 글리프의 구성 요소 기록. */
        byte[] comp;

        /**
         * Creates the model of an empty glyph.
         * <p>
         * 빈 글리프의 모델을 만든다.
         *
         * @return the glyph / 글리프
         */
        static Glyph empty() {
            return new Glyph();
        }

        /**
         * Creates the model of a simple glyph.
         * <p>
         * 단순 글리프의 모델을 만든다.
         *
         * @param endPts the last point of each contour / 윤곽선별 마지막 점
         * @param xs     the x coordinates / x 좌표
         * @param ys     the y coordinates / y 좌표
         * @param on     the on-curve markers / 곡선 위 점 표시
         * @return the glyph / 글리프
         */
        static Glyph simple(int[] endPts, int[] xs, int[] ys, boolean[] on) {
            Glyph g = new Glyph();
            g.kind = 1;
            g.endPts = endPts;
            g.xs = xs;
            g.ys = ys;
            g.on = on;
            return g;
        }

        /**
         * Creates the model of a composite glyph.
         * <p>
         * 합성 글리프의 모델을 만든다.
         *
         * @param comp  the component records / 구성 요소 기록
         * @param box   the box / 상자
         * @param instr the instructions (used when a component has the instruction flag) / 명령(구성 요소가 명령 플래그를 가질 때 쓴다)
         * @return the glyph / 글리프
         */
        static Glyph composite(byte[] comp, int[] box, byte[] instr) {
            Glyph g = new Glyph();
            g.kind = 2;
            g.comp = comp;
            g.box = box;
            g.instr = instr;
            return g;
        }

        /**
         * Returns the box that the decoder must produce: the explicit one, or the box of the points.
         * <p>
         * 디코더가 만들어야 하는 상자를 반환한다: 명시한 상자 또는 점의 상자.
         *
         * @return {xMin, yMin, xMax, yMax}, all zero for an empty glyph / {xMin, yMin, xMax, yMax}. 빈 글리프는 모두 0
         */
        int[] effectiveBox() {
            if (kind == 0) {
                return new int[4];
            }
            if (box != null) {
                return box;
            }
            int[] b = { Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE };
            // 점 전체의 최소/최대를 구한다.
            for (int i = 0; i < xs.length; i++) {
                b[0] = Math.min(b[0], xs[i]);
                b[1] = Math.min(b[1], ys[i]);
                b[2] = Math.max(b[2], xs[i]);
                b[3] = Math.max(b[3], ys[i]);
            }
            return b;
        }
    }

    /** One row of the triplet table of section 5.2. 5.2 절 삼중 표의 행 하나. */
    static final class Row {
        /** The flag value. 플래그 값. */
        int index;
        /** The number of data bytes after the flag. 플래그 뒤의 데이터 바이트 수. */
        int data;
        /** The number of bits of the X value (0: the delta is zero). X 값의 비트 수(0 이면 변화량이 0). */
        int xBits;
        /** The number of bits of the Y value. Y 값의 비트 수. */
        int yBits;
        /** The amount added to the X value. X 값에 더하는 양. */
        int dxBase;
        /** The amount added to the Y value. Y 값에 더하는 양. */
        int dyBase;
        /** The sign of X (0 not applicable). X 부호(0 이면 해당 없음). */
        int xSign;
        /** The sign of Y. Y 부호. */
        int ySign;
    }

    /** The 128 rows of the table. 표의 128 개 행. */
    static final Row[] ROWS = rows();

    /** The glyphs of the font. 글꼴의 글리프. */
    final List<Glyph> glyphs = new ArrayList<Glyph>();
    /** The format of loca: 0 short, 1 long. loca 형식: 0 짧은 형식, 1 긴 형식. */
    int indexFormat = 1;
    /** Whether glyf and loca are transformed (version 0) or not (version 3). glyf 와 loca 를 변환했는지(버전 0) 아닌지(버전 3). */
    boolean transformGlyf = true;
    /** 0: hmtx without transform; 1..3: transformed, the flags say which arrays are left out (bit 0 the lsb array, bit 1 the leftSideBearing array). 0 이면 hmtx 는 변환 없음, 1~3 이면 변환하고 플래그가 생략한 배열(비트 0 lsb 배열, 비트 1 leftSideBearing 배열)을 정한다. */
    int hmtxFlags;
    /** The number of long horizontal metrics, or -1 for all glyphs. 긴 가로 지표 수. -1 이면 모든 글리프. */
    int numMetrics = -1;
    /** The sfnt version. sfnt 버전. */
    long flavor = 0x00010000L;
    /** Whether the loca table comes before the glyf table in the directory (invalid). loca 가 디렉터리에서 glyf 앞에 오는지 여부(올바르지 않음). */
    boolean locaFirst;
    /** Whether the loca table is left out (invalid). loca 테이블을 생략하는지 여부(올바르지 않음). */
    boolean omitLoca;
    /** The real <code>head</code> table (bit 11 of its flags is set when it is written), or null for a synthetic one. 실제 <code>head</code> 테이블(쓸 때 flags 의 11 번 비트를 켠다). 합성이면 null. */
    byte[] headTable;
    /** The real <code>hhea</code> table, or null. 실제 <code>hhea</code> 테이블. 없으면 null. */
    byte[] hheaTable;
    /** The real <code>maxp</code> table, or null. 실제 <code>maxp</code> 테이블. 없으면 null. */
    byte[] maxpTable;
    /** The advance width of every glyph of the real <code>hmtx</code> table, or null for 500 plus the index. 실제 <code>hmtx</code> 의 글리프별 진행 폭. 없으면 500 + 인덱스. */
    int[] advances;
    /** The left side bearing of every glyph of the real <code>hmtx</code> table, or null for the xMin of the glyph. 실제 <code>hmtx</code> 의 글리프별 왼쪽 여백. 없으면 글리프의 xMin. */
    int[] lsbs;
    /** The original <code>glyf</code> table that is copied when the glyph table is not transformed, or null (then it is written from the model). 글리프 테이블을 변환하지 않을 때 그대로 복사하는 원래 <code>glyf</code> 테이블. 없으면 모델에서 쓴다. */
    byte[] glyfOriginal;
    /** The original <code>loca</code> table that goes with {@link #glyfOriginal}, or null. {@link #glyfOriginal} 에 맞는 원래 <code>loca</code> 테이블. 없으면 null. */
    byte[] locaOriginal;
    /** The tags of the additional tables (after the standard ones). 추가 테이블의 태그(표준 테이블 뒤). */
    final List<String> extraTags = new ArrayList<String>();
    /** The data of the additional tables. 추가 테이블의 데이터. */
    final List<byte[]> extraData = new ArrayList<byte[]>();
    /** The tags of the standard tables that are written with the explicit tag (flag value 63). 명시한 태그(플래그 값 63)로 쓰는 표준 테이블의 태그. */
    final List<String> explicitTags = new ArrayList<String>();

    /**
     * The pieces of a built file.
     * <p>
     * 만든 파일의 조각.
     */
    static final class Result {
        /** The table directory. 테이블 디렉터리. */
        byte[] dir;
        /** The table data before the compression (the concatenation of the data of all tables). 압축 전 테이블 데이터(모든 테이블 데이터의 이음). */
        byte[] stream;
        /** The number of tables. 테이블 수. */
        int numTables;
        /** The offset of the data of a table in the stream by tag. 스트림 안의 테이블 데이터 오프셋(태그별). */
        java.util.Map<String, Integer> offsets = new java.util.HashMap<String, Integer>();
        /** The length of the data of a table in the stream by tag. 스트림 안의 테이블 데이터 길이(태그별). */
        java.util.Map<String, Integer> lengths = new java.util.HashMap<String, Integer>();
        /** The whole file. 파일 전체. */
        byte[] file;
    }

    /**
     * Creates the rows of the table of section 5.2 from their description: byte count, bits, added deltas and signs.
     * <p>
     * 5.2 절 표의 행을 설명(바이트 수, 비트, 더하는 변화량, 부호)에서 만든다.
     *
     * @return the 128 rows / 128 개 행
     */
    private static Row[] rows() {
        Row[] r = new Row[128];
        int[][] signs = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        int[] base10 = { 0, 0, 256, 256, 512, 512, 768, 768, 1024, 1024 };
        // 0~9: Y 만 8 비트, 10~19: X 만 8 비트.
        for (int i = 0; i < 20; i++) {
            Row w = new Row();
            w.index = i;
            w.data = 1;
            if (i < 10) {
                w.yBits = 8;
                w.dyBase = base10[i];
                w.ySign = i % 2 == 0 ? -1 : 1;
            } else {
                w.xBits = 8;
                w.dxBase = base10[i - 10];
                w.xSign = i % 2 == 0 ? -1 : 1;
            }
            r[i] = w;
        }
        // 20~83: 4 비트씩, 변화량 1/17/33/49 에서 시작한다.
        for (int i = 20; i < 84; i++) {
            int j = i - 20;
            Row w = new Row();
            w.index = i;
            w.data = 1;
            w.xBits = 4;
            w.yBits = 4;
            w.dxBase = 1 + 16 * (j / 16);
            w.dyBase = 1 + 16 * ((j / 4) % 4);
            w.xSign = signs[j % 4][0];
            w.ySign = signs[j % 4][1];
            r[i] = w;
        }
        // 84~119: 8 비트씩, 변화량 1/257/513 에서 시작한다.
        for (int i = 84; i < 120; i++) {
            int j = i - 84;
            Row w = new Row();
            w.index = i;
            w.data = 2;
            w.xBits = 8;
            w.yBits = 8;
            w.dxBase = 1 + 256 * (j / 12);
            w.dyBase = 1 + 256 * ((j % 12) / 4);
            w.xSign = signs[j % 4][0];
            w.ySign = signs[j % 4][1];
            r[i] = w;
        }
        // 120~123: 12 비트씩, 124~127: 16 비트씩.
        for (int i = 120; i < 128; i++) {
            Row w = new Row();
            w.index = i;
            w.data = i < 124 ? 3 : 4;
            w.xBits = i < 124 ? 12 : 16;
            w.yBits = w.xBits;
            w.xSign = signs[i % 4][0];
            w.ySign = signs[i % 4][1];
            r[i] = w;
        }
        return r;
    }

    /**
     * Tells whether a delta on one axis can be written with a row.
     * <p>
     * 한 축의 변화량을 행으로 쓸 수 있는지 알려 준다.
     *
     * @param bits the bits of the axis / 축의 비트 수
     * @param base the amount added / 더하는 양
     * @param sign the sign of the row / 행의 부호
     * @param v    the delta / 변화량
     * @return true if it fits / 맞으면 true
     */
    private static boolean fits(int bits, int base, int sign, int v) {
        if (bits == 0) {
            return v == 0;
        }
        if (v != 0 && (v < 0 ? -1 : 1) != sign) {
            return false;
        }
        int mag = Math.abs(v);
        return mag >= base && mag <= base + (1 << bits) - 1;
    }

    /**
     * Tells whether a row can write a delta.
     * <p>
     * 행이 변화량을 쓸 수 있는지 알려 준다.
     *
     * @param r  the row / 행
     * @param dx the x delta / x 변화량
     * @param dy the y delta / y 변화량
     * @return true if it fits / 맞으면 true
     */
    static boolean rowFits(Row r, int dx, int dy) {
        return fits(r.xBits, r.dxBase, r.xSign, dx) && fits(r.yBits, r.dyBase, r.ySign, dy);
    }

    /**
     * Encodes a delta: the flag value and the data bytes.
     * <p>
     * 변화량을 인코딩한다: 플래그 값과 데이터 바이트.
     *
     * @param dx     the x delta / x 변화량
     * @param dy     the y delta / y 변화량
     * @param forced the row to use, or below 0 for the row with the fewest bytes / 쓸 행. 0 미만이면 바이트가 가장 적은 행
     * @return {flag, data bytes...} / {플래그, 데이터 바이트...}
     */
    static int[] encode(int dx, int dy, int forced) {
        Row best = null;
        // 맞는 행 중 데이터 바이트가 가장 적은 것을 고른다(강제한 행이 있으면 그것만).
        for (int i = 0; i < ROWS.length; i++) {
            Row r = ROWS[i];
            if (forced >= 0 && r.index != forced) {
                continue;
            }
            if (rowFits(r, dx, dy) && (best == null || r.data < best.data)) {
                best = r;
            }
        }
        if (best == null) {
            throw new IllegalArgumentException("no triplet row for " + dx + "," + dy + " forced " + forced);
        }
        int xv = best.xBits == 0 ? 0 : Math.abs(dx) - best.dxBase;
        int yv = best.yBits == 0 ? 0 : Math.abs(dy) - best.dyBase;
        int i = best.index;
        if (i < 10) {
            return new int[] { i, yv };
        }
        if (i < 20) {
            return new int[] { i, xv };
        }
        if (i < 84) {
            return new int[] { i, (xv << 4) | yv };
        }
        if (i < 120) {
            return new int[] { i, xv, yv };
        }
        if (i < 124) {
            return new int[] { i, xv >> 4, ((xv & 15) << 4) | (yv >> 8), yv & 255 };
        }
        return new int[] { i, xv >> 8, xv & 255, yv >> 8, yv & 255 };
    }

    /**
     * Encodes a number as <code>UIntBase128</code>.
     * <p>
     * 수를 <code>UIntBase128</code> 로 인코딩한다.
     *
     * @param v the number / 수
     * @return the bytes / 바이트
     */
    static byte[] base128(long v) {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        List<Integer> parts = new ArrayList<Integer>();
        parts.add((int) (v & 0x7F));
        v >>= 7;
        // 7 비트씩 낮은 쪽부터 모은다.
        while (v > 0) {
            parts.add((int) ((v & 0x7F) | 0x80));
            v >>= 7;
        }
        // 높은 쪽부터 쓴다.
        for (int i = parts.size() - 1; i >= 0; i--) {
            tmp.write(parts.get(i));
        }
        return tmp.toByteArray();
    }

    /**
     * Encodes a number as <code>255UInt16</code> with the shortest form.
     * <p>
     * 수를 가장 짧은 형태의 <code>255UInt16</code> 로 인코딩한다.
     *
     * @param v the number / 수
     * @return the bytes / 바이트
     */
    static byte[] u255(int v) {
        if (v < 253) {
            return new byte[] { (byte) v };
        }
        if (v < 506) {
            return new byte[] { (byte) 255, (byte) (v - 253) };
        }
        if (v < 759) {
            return new byte[] { (byte) 254, (byte) (v - 506) };
        }
        return new byte[] { (byte) 253, (byte) (v >> 8), (byte) v };
    }

    /**
     * Wraps data in a Brotli stream that consists of uncompressed meta-blocks (RFC 7932 section 9.2) followed by an empty last meta-block.
     * <p>
     * 데이터를 압축하지 않은 메타 블록(RFC 7932 9.2 절)과 빈 마지막 메타 블록으로 된 Brotli 스트림에 담는다.
     *
     * @param data the data / 데이터
     * @return the stream / 스트림
     */
    static byte[] stored(byte[] data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int pos = 0;
        boolean first = true;
        // 65536 바이트씩 압축하지 않은 메타 블록으로 쓴다.
        while (pos < data.length) {
            int n = Math.min(65536, data.length - pos);
            long header;
            if (first) {
                // 비트: WBITS 0(1 비트), ISLAST 0, MNIBBLES 0 (4 니블), MLEN-1 (16 비트), ISUNCOMPRESSED 1.
                header = ((long) (n - 1) << 4) | (1L << 20);
            } else {
                header = ((long) (n - 1) << 3) | (1L << 19);
            }
            out.write((int) header);
            out.write((int) (header >> 8));
            out.write((int) (header >> 16));
            out.write(data, pos, n);
            pos += n;
            first = false;
        }
        // 마지막 빈 메타 블록: 처음이면 WBITS 0 + ISLAST 1 + ISLASTEMPTY 1, 아니면 ISLAST 1 + ISLASTEMPTY 1.
        out.write(first ? 0x06 : 0x03);
        return out.toByteArray();
    }

    /**
     * Puts a file together from its pieces.
     * <p>
     * 조각으로 파일을 맞춘다.
     *
     * @param flavor   the sfnt version / sfnt 버전
     * @param numTables the number of tables in the header / 헤더의 테이블 수
     * @param dir      the table directory / 테이블 디렉터리
     * @param brotli   the compressed block / 압축 블록
     * @param meta     the metadata block or null / 메타데이터 블록 또는 null
     * @param priv     the private data block or null / 비공개 데이터 블록 또는 null
     * @return the file / 파일
     */
    static byte[] assemble(long flavor, int numTables, byte[] dir, byte[] brotli, byte[] meta, byte[] priv) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(dir, 0, dir.length);
        body.write(brotli, 0, brotli.length);
        long offset = 48 + dir.length + brotli.length;
        long metaOffset = 0;
        long privOffset = 0;
        if (meta != null) {
            // 압축 블록 뒤를 4 바이트 경계까지 0 으로 채우고 메타데이터를 놓는다.
            while ((offset & 3) != 0) {
                body.write(0);
                offset++;
            }
            metaOffset = offset;
            body.write(meta, 0, meta.length);
            offset += meta.length;
        }
        if (priv != null) {
            // 앞 블록 뒤를 4 바이트 경계까지 0 으로 채운다.
            while ((offset & 3) != 0) {
                body.write(0);
                offset++;
            }
            privOffset = offset;
            body.write(priv, 0, priv.length);
            offset += priv.length;
        }
        // 파일 끝도 4 바이트 경계까지 채운다.
        while ((offset & 3) != 0) {
            body.write(0);
            offset++;
        }
        byte[] h = new byte[48];
        h[0] = 'w';
        h[1] = 'O';
        h[2] = 'F';
        h[3] = '2';
        put32(h, 4, flavor);
        put32(h, 8, offset);
        put16(h, 12, numTables);
        put32(h, 20, brotli.length);
        put16(h, 24, 1);
        put32(h, 28, metaOffset);
        put32(h, 32, meta == null ? 0 : meta.length);
        put32(h, 36, meta == null ? 0 : meta.length);
        put32(h, 40, privOffset);
        put32(h, 44, priv == null ? 0 : priv.length);
        byte[] b = body.toByteArray();
        byte[] f = new byte[48 + b.length];
        System.arraycopy(h, 0, f, 0, 48);
        System.arraycopy(b, 0, f, 48, b.length);
        return f;
    }

    /**
     * Builds the file from the model.
     * <p>
     * 모델에서 파일을 만든다.
     *
     * @return the pieces and the file / 조각과 파일
     */
    Result build() {
        int n = glyphs.size();
        int metrics = numMetrics < 0 ? n : numMetrics;
        int[] xMin = new int[n];
        // 글리프별 xMin 을 구한다.
        for (int i = 0; i < n; i++) {
            xMin[i] = glyphs.get(i).effectiveBox()[0];
        }
        byte[] head;
        // 실제 head 가 있으면 그것에 flags 의 11 번 비트(무손실 변환을 거침)를 켜서 쓰고, 없으면 합성 head 를 만든다.
        if (headTable != null) {
            head = headTable.clone();
            put16(head, 16, u16(head, 16) | 0x0800);
        } else {
            head = new byte[54];
            put16(head, 16, 0x0800);
            put16(head, 50, indexFormat);
            put32(head, 8, 0xDEADBEEFL);
        }
        byte[] hhea = hheaTable;
        if (hhea == null) {
            hhea = new byte[36];
            put16(hhea, 34, metrics);
        }
        byte[] maxp = maxpTable;
        if (maxp == null) {
            maxp = new byte[32];
            put32(maxp, 0, 0x00010000L);
            put16(maxp, 4, n);
        }
        byte[] hmtxOrig = new byte[4 * metrics + 2 * (n - metrics)];
        int hp = 0;
        // 원래 hmtx: 진행 폭은 500 + 인덱스(실제 지표가 있으면 그것), 왼쪽 여백은 글리프의 xMin(실제 지표가 있으면 그것).
        for (int i = 0; i < n; i++) {
            if (i < metrics) {
                put16(hmtxOrig, hp, advanceOf(i));
                hp += 2;
            }
            put16(hmtxOrig, hp, lsbOf(i));
            hp += 2;
        }
        byte[] glyfData = transformGlyf ? glyfTransform() : (glyfOriginal != null ? glyfOriginal : glyfPlain());
        byte[] locaOrig = new byte[(n + 1) * (indexFormat == 0 ? 2 : 4)];
        List<String> tags = new ArrayList<String>();
        List<byte[]> orig = new ArrayList<byte[]>();
        List<byte[]> data = new ArrayList<byte[]>();
        List<Integer> versions = new ArrayList<Integer>();
        List<Boolean> hasTransformLength = new ArrayList<Boolean>();
        add(tags, orig, data, versions, hasTransformLength, "head", head, null, 0);
        add(tags, orig, data, versions, hasTransformLength, "hhea", hhea, null, 0);
        add(tags, orig, data, versions, hasTransformLength, "maxp", maxp, null, 0);
        if (hmtxFlags != 0) {
            add(tags, orig, data, versions, hasTransformLength, "hmtx", hmtxOrig, hmtxTransform(metrics, hmtxFlags), 1);
        } else {
            add(tags, orig, data, versions, hasTransformLength, "hmtx", hmtxOrig, null, 0);
        }
        if (transformGlyf) {
            if (locaFirst) {
                add(tags, orig, data, versions, hasTransformLength, "loca", locaOrig, new byte[0], 0);
            }
            add(tags, orig, data, versions, hasTransformLength, "glyf", glyfData, glyfData, 0);
            if (!locaFirst && !omitLoca) {
                add(tags, orig, data, versions, hasTransformLength, "loca", locaOrig, new byte[0], 0);
            }
        } else {
            // 변환 없는 glyf 와 loca 는 버전 3: 여기서는 단순 인코딩한 glyf 와 거기에 맞춘 loca 를 쓴다.
            byte[] plainLoca = locaOriginal != null ? locaOriginal : plainLoca(glyfData);
            add(tags, orig, data, versions, hasTransformLength, "glyf", glyfData, null, 3);
            add(tags, orig, data, versions, hasTransformLength, "loca", plainLoca, null, 3);
        }
        // 추가 테이블을 뒤에 붙인다.
        for (int i = 0; i < extraTags.size(); i++) {
            add(tags, orig, data, versions, hasTransformLength, extraTags.get(i), extraData.get(i), null, 0);
        }
        Result r = new Result();
        ByteArrayOutputStream dir = new ByteArrayOutputStream();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        // 디렉터리 항목과 테이블 스트림을 쓴다.
        for (int i = 0; i < tags.size(); i++) {
            String tag = tags.get(i);
            int code = knownCode(tag);
            if (explicitTags.contains(tag)) {
                code = -1;
            }
            dir.write((code >= 0 ? code : 63) | (versions.get(i) << 6));
            if (code < 0) {
                // 명시한 태그의 네 글자를 쓴다.
                for (int k = 0; k < 4; k++) {
                    dir.write(tag.charAt(k));
                }
            }
            byte[] ob = base128(orig.get(i).length);
            dir.write(ob, 0, ob.length);
            if (hasTransformLength.get(i)) {
                byte[] tb = base128(data.get(i).length);
                dir.write(tb, 0, tb.length);
            }
            r.offsets.put(tag, stream.size());
            r.lengths.put(tag, data.get(i).length);
            stream.write(data.get(i), 0, data.get(i).length);
        }
        r.dir = dir.toByteArray();
        r.stream = stream.toByteArray();
        r.numTables = tags.size();
        r.file = assemble(flavor, tags.size(), r.dir, stored(r.stream), null, null);
        return r;
    }

    /**
     * Adds a table to the lists.
     * <p>
     * 목록에 테이블을 더한다.
     *
     * @param tags        the tags / 태그
     * @param orig        the original tables / 원래 테이블
     * @param data        the data in the stream / 스트림의 데이터
     * @param versions    the transform versions / 변환 버전
     * @param hasLength   whether the entry has a transform length / 항목에 변환 길이가 있는지 여부
     * @param tag         the tag / 태그
     * @param original    the original table / 원래 테이블
     * @param transformed the transformed data, or null if the table has no transform / 변환된 데이터. 변환이 없으면 null
     * @param version     the transform version / 변환 버전
     */
    private static void add(List<String> tags, List<byte[]> orig, List<byte[]> data, List<Integer> versions, List<Boolean> hasLength, String tag, byte[] original,
            byte[] transformed, int version) {
        tags.add(tag);
        orig.add(original);
        data.add(transformed != null ? transformed : original);
        versions.add(version);
        hasLength.add(transformed != null);
    }

    /**
     * Returns the known-tag code of a tag, or -1.
     * <p>
     * 태그의 알려진 태그 코드를 반환한다. 없으면 -1.
     *
     * @param tag the tag / 태그
     * @return the code / 코드
     */
    static int knownCode(String tag) {
        String[] known = { "cmap", "head", "hhea", "hmtx", "maxp", "name", "OS/2", "post", "cvt ", "fpgm", "glyf", "loca", "prep", "CFF ", "VORG", "EBDT", "EBLC", "gasp", "hdmx",
                "kern", "LTSH", "PCLT", "VDMX", "vhea", "vmtx", "BASE", "GDEF", "GPOS", "GSUB", "EBSC", "JSTF", "MATH", "CBDT", "CBLC", "COLR", "CPAL", "SVG ", "sbix", "acnt",
                "avar", "bdat", "bloc", "bsln", "cvar", "fdsc", "feat", "fmtx", "fvar", "gvar", "hsty", "just", "lcar", "mort", "morx", "opbd", "prop", "trak", "Zapf", "Silf",
                "Glat", "Gloc", "Feat", "Sill" };
        // 알려진 태그 표에서 찾는다.
        for (int i = 0; i < known.length; i++) {
            if (known[i].equals(tag)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Builds the transformed hmtx table (section 5.4): the arrays that the flags leave out are not written.
     * <p>
     * 변환된 hmtx 테이블(5.4 절)을 만든다: 플래그가 생략하는 배열은 쓰지 않는다.
     *
     * @param metrics the number of long metrics / 긴 지표 수
     * @param flags   the flags / 플래그
     * @return the table / 테이블
     */
    private byte[] hmtxTransform(int metrics, int flags) {
        int n = glyphs.size();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(flags);
        // 진행 폭 배열은 항상 쓴다.
        for (int i = 0; i < metrics; i++) {
            out.write(advanceOf(i) >> 8);
            out.write(advanceOf(i));
        }
        // 비트 0 이 꺼져 있으면 lsb 배열을 쓴다.
        if ((flags & 1) == 0) {
            for (int i = 0; i < metrics; i++) {
                int v = lsbOf(i);
                out.write(v >> 8);
                out.write(v);
            }
        }
        // 비트 1 이 꺼져 있으면 leftSideBearing 배열을 쓴다.
        if ((flags & 2) == 0) {
            for (int i = metrics; i < n; i++) {
                int v = lsbOf(i);
                out.write(v >> 8);
                out.write(v);
            }
        }
        return out.toByteArray();
    }

    /**
     * Returns the advance width of a glyph in the original <code>hmtx</code>.
     * <p>
     * 원래 <code>hmtx</code> 에서 글리프의 진행 폭을 반환한다.
     *
     * @param i the glyph / 글리프
     * @return the advance width / 진행 폭
     */
    private int advanceOf(int i) {
        return advances != null ? advances[i] : 500 + i;
    }

    /**
     * Returns the left side bearing of a glyph in the original <code>hmtx</code>: the real one, else the xMin of the glyph.
     * <p>
     * 원래 <code>hmtx</code> 에서 글리프의 왼쪽 여백을 반환한다: 실제 값, 없으면 글리프의 xMin.
     *
     * @param i the glyph / 글리프
     * @return the left side bearing / 왼쪽 여백
     */
    private int lsbOf(int i) {
        return lsbs != null ? lsbs[i] : glyphs.get(i).effectiveBox()[0];
    }

    /**
     * Reads a big-endian unsigned 16 bit number.
     * <p>
     * 빅 엔디언 부호 없는 16비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    static int u16(byte[] b, int i) {
        return ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
    }

    /**
     * Reads a big-endian signed 16 bit number.
     * <p>
     * 빅 엔디언 부호 있는 16비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    static int i16(byte[] b, int i) {
        return (short) u16(b, i);
    }

    /**
     * Reads a big-endian unsigned 32 bit number.
     * <p>
     * 빅 엔디언 부호 없는 32비트 수를 읽는다.
     *
     * @param b the array / 배열
     * @param i the position / 위치
     * @return the number / 수
     */
    static long u32(byte[] b, int i) {
        return ((long) u16(b, i) << 16) | u16(b, i + 2);
    }

    /**
     * Parses the glyph record at a place of a glyph table into the model (the overlap flag of a simple glyph is the bit of its first flag; the box is the stored one).
     * <p>
     * 글리프 테이블의 한 곳의 글리프 기록을 모델로 파싱한다(단순 글리프의 겹침 플래그는 첫 플래그의 비트이고 상자는 저장된 값).
     *
     * @param b   the glyph table / 글리프 테이블
     * @param off the start of the glyph / 글리프의 시작
     * @param len the length of the record / 기록의 길이
     * @return the glyph / 글리프
     * @throws IllegalArgumentException if a glyph without contours has a box / 윤곽선이 없는 글리프에 상자가 있으면
     */
    static Glyph parseGlyph(byte[] b, int off, int len) {
        Glyph g = new Glyph();
        if (len == 0) {
            return g;
        }
        int nc = i16(b, off);
        int[] box = { i16(b, off + 2), i16(b, off + 4), i16(b, off + 6), i16(b, off + 8) };
        int p = off + 10;
        if (nc == 0) {
            if (box[0] != 0 || box[1] != 0 || box[2] != 0 || box[3] != 0) {
                throw new IllegalArgumentException("a glyph without contours has a box");
            }
            return g;
        }
        g.box = box;
        if (nc > 0) {
            g.kind = 1;
            g.endPts = new int[nc];
            // 윤곽선 끝 점을 읽는다.
            for (int c = 0; c < nc; c++) {
                g.endPts[c] = u16(b, p);
                p += 2;
            }
            int total = g.endPts[nc - 1] + 1;
            int il = u16(b, p);
            p += 2;
            g.instr = java.util.Arrays.copyOfRange(b, p, p + il);
            p += il;
            int[] flags = new int[total];
            int k = 0;
            // 반복 플래그를 풀어 점마다 플래그를 읽는다.
            while (k < total) {
                int f = b[p++] & 0xFF;
                flags[k++] = f;
                if ((f & 8) != 0) {
                    int rep = b[p++] & 0xFF;
                    // 반복 횟수만큼 같은 플래그를 채운다.
                    for (int r = 0; r < rep; r++) {
                        flags[k++] = f;
                    }
                }
            }
            g.xs = new int[total];
            g.ys = new int[total];
            g.on = new boolean[total];
            int x = 0;
            // x 좌표 차이를 읽는다.
            for (int i = 0; i < total; i++) {
                int f = flags[i];
                g.on[i] = (f & 1) != 0;
                if ((f & 2) != 0) {
                    int d = b[p++] & 0xFF;
                    x += (f & 0x10) != 0 ? d : -d;
                } else if ((f & 0x10) == 0) {
                    x += i16(b, p);
                    p += 2;
                }
                g.xs[i] = x;
            }
            int y = 0;
            // y 좌표 차이를 읽는다.
            for (int i = 0; i < total; i++) {
                int f = flags[i];
                if ((f & 4) != 0) {
                    int d = b[p++] & 0xFF;
                    y += (f & 0x20) != 0 ? d : -d;
                } else if ((f & 0x20) == 0) {
                    y += i16(b, p);
                    p += 2;
                }
                g.ys[i] = y;
            }
            g.overlap = (flags[0] & 0x40) != 0;
            return g;
        }
        g.kind = 2;
        int start = p;
        boolean instr = false;
        int flags;
        // 구성 요소를 MORE_COMPONENTS 가 꺼질 때까지 읽는다.
        do {
            flags = u16(b, p);
            int size = 4 + ((flags & 1) != 0 ? 4 : 2);
            if ((flags & 8) != 0) {
                size += 2;
            } else if ((flags & 0x40) != 0) {
                size += 4;
            } else if ((flags & 0x80) != 0) {
                size += 8;
            }
            instr |= (flags & 0x100) != 0;
            p += size;
        } while ((flags & 0x20) != 0);
        g.comp = java.util.Arrays.copyOfRange(b, start, p);
        if (instr) {
            int il = u16(b, p);
            g.instr = java.util.Arrays.copyOfRange(b, p + 2, p + 2 + il);
        }
        return g;
    }

    /**
     * Makes the builder of a WOFF2 file from a TrueType font: the glyph table is parsed into the model, the other tables are taken as they are (<code>DSIG</code> is
     * dropped: the specification requires the encoder to remove it), the <code>hmtx</code> transform is chosen when the left side bearings of the proportional and the
     * monospaced glyphs equal the xMin of the glyphs (arrays that do not are written).
     * <p>
     * TrueType 글꼴에서 WOFF2 파일의 빌더를 만든다: 글리프 테이블은 모델로 파싱하고 다른 테이블은 그대로 가져온다(<code>DSIG</code> 는 뺀다: 규격이 인코더에게 지우라고 한다). 비례 글리프와
     * 고정폭 글리프의 왼쪽 여백이 글리프의 xMin 과 같으면 <code>hmtx</code> 변환을 고른다(같지 않은 배열은 쓴다).
     *
     * @param sfnt      the TrueType font (version 0x00010000 or <code>true</code>) / TrueType 글꼴(버전 0x00010000 또는 <code>true</code>)
     * @param transform true to transform <code>glyf</code>, <code>loca</code> and where possible <code>hmtx</code>, false to copy all tables / <code>glyf</code>,
     *                  <code>loca</code> 와 가능하면 <code>hmtx</code> 를 변환하려면 true, 모든 테이블을 복사하려면 false
     * @return the builder / 빌더
     * @throws IOException if the font is not a TrueType font that has the tables that WOFF2 needs / 글꼴이 WOFF2 에 필요한 테이블이 있는 TrueType 글꼴이 아니면
     */
    static Woff2Builder fromSfnt(byte[] sfnt, boolean transform) throws IOException {
        if (sfnt == null || sfnt.length < 12) {
            throw new IOException("not an sfnt font");
        }
        long version = u32(sfnt, 0);
        if (version != 0x00010000L && version != 0x74727565L) {
            throw new IOException("a TrueType font is needed");
        }
        int count = u16(sfnt, 4);
        java.util.Map<String, byte[]> t = new java.util.LinkedHashMap<String, byte[]>();
        // 테이블 디렉터리를 읽어 테이블마다 사본을 만든다.
        for (int i = 0; i < count; i++) {
            int e = 12 + 16 * i;
            String tag = new String(sfnt, e, 4, "ISO-8859-1");
            int off = (int) u32(sfnt, e + 8);
            int len = (int) u32(sfnt, e + 12);
            t.put(tag, java.util.Arrays.copyOfRange(sfnt, off, off + len));
        }
        String[] needed = { "head", "hhea", "maxp", "hmtx", "glyf", "loca" };
        // WOFF2 변환에 필요한 테이블이 모두 있는지 본다.
        for (int i = 0; i < needed.length; i++) {
            if (!t.containsKey(needed[i])) {
                throw new IOException("the font has no " + needed[i] + " table");
            }
        }
        Woff2Builder b = new Woff2Builder();
        b.flavor = version;
        b.transformGlyf = transform;
        b.headTable = t.get("head");
        b.hheaTable = t.get("hhea");
        b.maxpTable = t.get("maxp");
        b.indexFormat = u16(b.headTable, 50);
        int n = u16(b.maxpTable, 4);
        int metrics = u16(b.hheaTable, 34);
        byte[] glyf = t.get("glyf");
        byte[] loca = t.get("loca");
        byte[] hmtx = t.get("hmtx");
        // 글리프마다 위치 테이블의 인접한 두 오프셋으로 글리프를 모델로 파싱한다.
        for (int i = 0; i < n; i++) {
            long a = b.indexFormat == 0 ? 2L * u16(loca, 2 * i) : u32(loca, 4 * i);
            long z = b.indexFormat == 0 ? 2L * u16(loca, 2 * i + 2) : u32(loca, 4 * i + 4);
            b.glyphs.add(parseGlyph(glyf, (int) a, (int) (z - a)));
        }
        b.numMetrics = metrics;
        b.advances = new int[n];
        b.lsbs = new int[n];
        boolean proportional = true;
        boolean monospaced = true;
        // 원래 hmtx 의 진행 폭과 왼쪽 여백을 읽고 왼쪽 여백이 xMin 과 같은지 본다.
        for (int i = 0; i < n; i++) {
            b.advances[i] = i < metrics ? u16(hmtx, 4 * i) : b.advances[metrics - 1];
            b.lsbs[i] = i < metrics ? i16(hmtx, 4 * i + 2) : i16(hmtx, 4 * metrics + 2 * (i - metrics));
            if (b.lsbs[i] != b.glyphs.get(i).effectiveBox()[0]) {
                if (i < metrics) {
                    proportional = false;
                } else {
                    monospaced = false;
                }
            }
        }
        b.hmtxFlags = transform ? (proportional ? 1 : 0) | (monospaced ? 2 : 0) : 0;
        if (!transform) {
            b.glyfOriginal = glyf;
            b.locaOriginal = loca;
        }
        // 나머지 테이블은 그대로 더한다(DSIG 는 뺀다).
        for (java.util.Map.Entry<String, byte[]> e : t.entrySet()) {
            String tag = e.getKey();
            if (!tag.equals("head") && !tag.equals("hhea") && !tag.equals("maxp") && !tag.equals("hmtx") && !tag.equals("glyf") && !tag.equals("loca") && !tag.equals("DSIG")) {
                b.extraTags.add(tag);
                b.extraData.add(e.getValue());
            }
        }
        return b;
    }

    /**
     * Builds the transformed glyf table (section 5.1) from the model.
     * <p>
     * 모델에서 변환된 glyf 테이블(5.1 절)을 만든다.
     *
     * @return the table / 테이블
     */
    private byte[] glyfTransform() {
        int n = glyphs.size();
        ByteArrayOutputStream nContour = new ByteArrayOutputStream();
        ByteArrayOutputStream nPoints = new ByteArrayOutputStream();
        ByteArrayOutputStream flags = new ByteArrayOutputStream();
        ByteArrayOutputStream glyph = new ByteArrayOutputStream();
        ByteArrayOutputStream composite = new ByteArrayOutputStream();
        ByteArrayOutputStream bboxData = new ByteArrayOutputStream();
        ByteArrayOutputStream instruction = new ByteArrayOutputStream();
        byte[] bitmap = new byte[4 * ((n + 31) / 32)];
        byte[] overlapBitmap = new byte[(n + 7) / 8];
        boolean anyOverlap = false;
        // 글리프를 하나씩 스트림으로 나눠 쓴다.
        for (int g = 0; g < n; g++) {
            Glyph gl = glyphs.get(g);
            int nc = gl.kind == 0 ? 0 : (gl.kind == 2 ? -1 : gl.endPts.length);
            nContour.write(nc >> 8);
            nContour.write(nc);
            if (gl.kind == 0) {
                continue;
            }
            boolean explicit = false;
            if (gl.kind == 1) {
                int start = 0;
                // 윤곽선마다 점 수를 쓴다.
                for (int c = 0; c < gl.endPts.length; c++) {
                    byte[] u = u255(gl.endPts[c] - start + 1);
                    nPoints.write(u, 0, u.length);
                    start = gl.endPts[c] + 1;
                }
                int px = 0;
                int py = 0;
                // 점마다 플래그와 좌표 바이트를 쓴다.
                for (int i = 0; i < gl.xs.length; i++) {
                    int[] t = encode(gl.xs[i] - px, gl.ys[i] - py, gl.forced == null ? -1 : gl.forced[i]);
                    flags.write((gl.on[i] ? 0 : 0x80) | t[0]);
                    // 삼중의 데이터 바이트를 글리프 스트림에 쓴다.
                    for (int k = 1; k < t.length; k++) {
                        glyph.write(t[k]);
                    }
                    px = gl.xs[i];
                    py = gl.ys[i];
                }
                byte[] il = u255(gl.instr.length);
                glyph.write(il, 0, il.length);
                instruction.write(gl.instr, 0, gl.instr.length);
                if (gl.box != null) {
                    int[] pb = boxOfPoints(gl);
                    explicit = pb[0] != gl.box[0] || pb[1] != gl.box[1] || pb[2] != gl.box[2] || pb[3] != gl.box[3];
                }
                if (gl.overlap) {
                    overlapBitmap[g >> 3] |= (byte) (0x80 >> (g & 7));
                    anyOverlap = true;
                }
            } else {
                explicit = true;
                composite.write(gl.comp, 0, gl.comp.length);
                if (hasInstructions(gl.comp)) {
                    byte[] il = u255(gl.instr.length);
                    glyph.write(il, 0, il.length);
                    instruction.write(gl.instr, 0, gl.instr.length);
                }
            }
            if (explicit) {
                bitmap[g >> 3] |= (byte) (0x80 >> (g & 7));
                int[] b = gl.effectiveBox();
                // 명시한 상자의 네 값을 쓴다.
                for (int k = 0; k < 4; k++) {
                    bboxData.write(b[k] >> 8);
                    bboxData.write(b[k]);
                }
            }
        }
        byte[] bbox = concat(bitmap, bboxData.toByteArray());
        byte[][] parts = { nContour.toByteArray(), nPoints.toByteArray(), flags.toByteArray(), glyph.toByteArray(), composite.toByteArray(), bbox, instruction.toByteArray() };
        byte[] header = new byte[36];
        put16(header, 2, anyOverlap ? 1 : 0);
        put16(header, 4, n);
        put16(header, 6, indexFormat);
        // 스트림 크기를 헤더에 쓴다.
        for (int i = 0; i < parts.length; i++) {
            put32(header, 8 + 4 * i, parts[i].length);
        }
        byte[] result = header;
        // 스트림을 차례로 이어 붙인다.
        for (int i = 0; i < parts.length; i++) {
            result = concat(result, parts[i]);
        }
        if (anyOverlap) {
            result = concat(result, overlapBitmap);
        }
        return result;
    }

    /**
     * Builds a glyf table in the plain TrueType format (the null transform): every glyph is written without repeated flags.
     * <p>
     * 일반 TrueType 형식(널 변환)의 glyf 테이블을 만든다: 모든 글리프를 반복 플래그 없이 쓴다.
     *
     * @return the table / 테이블
     */
    private byte[] glyfPlain() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        locaOffsets = new int[glyphs.size() + 1];
        // 글리프를 하나씩 TrueType 형식으로 쓴다.
        for (int g = 0; g < glyphs.size(); g++) {
            locaOffsets[g] = out.size();
            Glyph gl = glyphs.get(g);
            if (gl.kind == 0) {
                continue;
            }
            int[] b = gl.effectiveBox();
            int nc = gl.kind == 2 ? -1 : gl.endPts.length;
            w16(out, nc);
            // 상자 네 값.
            for (int k = 0; k < 4; k++) {
                w16(out, b[k]);
            }
            if (gl.kind == 2) {
                out.write(gl.comp, 0, gl.comp.length);
                if (hasInstructions(gl.comp)) {
                    w16(out, gl.instr.length);
                    out.write(gl.instr, 0, gl.instr.length);
                }
            } else {
                // 윤곽선 끝 점과 명령.
                for (int c = 0; c < nc; c++) {
                    w16(out, gl.endPts[c]);
                }
                w16(out, gl.instr.length);
                out.write(gl.instr, 0, gl.instr.length);
                int px = 0;
                int py = 0;
                ByteArrayOutputStream xb = new ByteArrayOutputStream();
                ByteArrayOutputStream yb = new ByteArrayOutputStream();
                // 점마다 반복 없는 플래그를 쓰고 좌표 바이트를 모은다.
                for (int i = 0; i < gl.xs.length; i++) {
                    int dx = gl.xs[i] - px;
                    int dy = gl.ys[i] - py;
                    int f = gl.on[i] ? 1 : 0;
                    if (dx == 0) {
                        f |= 0x10;
                    } else if (Math.abs(dx) <= 255) {
                        f |= 0x02 | (dx > 0 ? 0x10 : 0);
                        xb.write(Math.abs(dx));
                    } else {
                        xb.write(dx >> 8);
                        xb.write(dx);
                    }
                    if (dy == 0) {
                        f |= 0x20;
                    } else if (Math.abs(dy) <= 255) {
                        f |= 0x04 | (dy > 0 ? 0x20 : 0);
                        yb.write(Math.abs(dy));
                    } else {
                        yb.write(dy >> 8);
                        yb.write(dy);
                    }
                    if (i == 0 && gl.overlap) {
                        f |= 0x40;
                    }
                    out.write(f);
                    px = gl.xs[i];
                    py = gl.ys[i];
                }
                byte[] xs = xb.toByteArray();
                byte[] ys = yb.toByteArray();
                out.write(xs, 0, xs.length);
                out.write(ys, 0, ys.length);
            }
            // 글리프는 4 바이트 경계에 맞춘다.
            while ((out.size() & 3) != 0) {
                out.write(0);
            }
        }
        locaOffsets[glyphs.size()] = out.size();
        return out.toByteArray();
    }

    /** The offsets of the glyphs of the last plain glyph table. 마지막 일반 글리프 테이블의 글리프 오프셋. */
    private int[] locaOffsets;

    /**
     * Builds the loca table that goes with the plain glyph table.
     * <p>
     * 일반 글리프 테이블에 맞는 loca 테이블을 만든다.
     *
     * @param glyf the plain glyph table (its offsets were stored by the builder) / 일반 글리프 테이블(오프셋은 빌더가 저장했다)
     * @return the loca table / loca 테이블
     */
    private byte[] plainLoca(byte[] glyf) {
        byte[] loca = new byte[locaOffsets.length * (indexFormat == 0 ? 2 : 4)];
        // 선택한 형식으로 오프셋을 쓴다.
        for (int i = 0; i < locaOffsets.length; i++) {
            if (indexFormat == 0) {
                put16(loca, 2 * i, locaOffsets[i] / 2);
            } else {
                put32(loca, 4 * i, locaOffsets[i]);
            }
        }
        return loca;
    }

    /**
     * Tells whether a sequence of component records has a component with the instruction flag.
     * <p>
     * 구성 요소 기록열에 명령 플래그를 가진 구성 요소가 있는지 알려 준다.
     *
     * @param comp the component records / 구성 요소 기록
     * @return true if instructions follow / 명령이 따르면 true
     */
    static boolean hasInstructions(byte[] comp) {
        int p = 0;
        boolean have = false;
        int flags;
        // 구성 요소를 MORE_COMPONENTS 플래그가 꺼질 때까지 건너뛴다.
        do {
            flags = ((comp[p] & 0xFF) << 8) | (comp[p + 1] & 0xFF);
            int size = 4 + ((flags & 1) != 0 ? 4 : 2);
            if ((flags & 0x0008) != 0) {
                size += 2;
            } else if ((flags & 0x0040) != 0) {
                size += 4;
            } else if ((flags & 0x0080) != 0) {
                size += 8;
            }
            if ((flags & 0x0100) != 0) {
                have = true;
            }
            p += size;
        } while ((flags & 0x0020) != 0);
        return have;
    }

    /**
     * Returns the box of the points of a simple glyph.
     * <p>
     * 단순 글리프 점의 상자를 반환한다.
     *
     * @param gl the glyph / 글리프
     * @return the box / 상자
     */
    private static int[] boxOfPoints(Glyph gl) {
        Glyph c = new Glyph();
        c.kind = 1;
        c.xs = gl.xs;
        c.ys = gl.ys;
        return c.effectiveBox();
    }

    /**
     * Concatenates two arrays.
     * <p>
     * 배열 둘을 잇는다.
     *
     * @param a the first / 첫째
     * @param b the second / 둘째
     * @return the result / 결과
     */
    static byte[] concat(byte[] a, byte[] b) {
        byte[] r = new byte[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /**
     * Writes a big-endian 16 bit number to a stream.
     * <p>
     * 빅 엔디언 16비트 수를 스트림에 쓴다.
     *
     * @param out the stream / 스트림
     * @param v   the number / 수
     */
    private static void w16(ByteArrayOutputStream out, int v) {
        out.write(v >> 8);
        out.write(v);
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
    static void put16(byte[] b, int i, int v) {
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
    static void put32(byte[] b, int i, long v) {
        b[i] = (byte) (v >> 24);
        b[i + 1] = (byte) (v >> 16);
        b[i + 2] = (byte) (v >> 8);
        b[i + 3] = (byte) v;
    }

    /**
     * Rebuilds a file with other table data (the directory is the one of the first build).
     * <p>
     * 다른 테이블 데이터로 파일을 다시 맞춘다(디렉터리는 처음 만든 것).
     *
     * @param r      the pieces of the first build / 처음 만든 조각
     * @param stream the changed table stream / 바뀐 테이블 스트림
     * @return the file / 파일
     * @throws IOException never; declared for callers that chain it / 발생하지 않음. 이어 부르는 쪽을 위해 선언
     */
    byte[] withStream(Result r, byte[] stream) throws IOException {
        return assemble(flavor, r.numTables, r.dir, stored(stream), null, null);
    }
}
