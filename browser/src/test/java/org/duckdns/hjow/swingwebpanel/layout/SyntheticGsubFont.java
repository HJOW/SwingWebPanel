package org.duckdns.hjow.swingwebpanel.layout;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.TreeMap;

/**
 * A small synthetic TrueType font made by the test itself (no font file of anybody is copied), with a <code>GSUB</code> table of glyph alternates for the tests
 * of <code>font-variant-alternates</code> and <code>@font-feature-values</code>. Units per em are 1000 and every glyph is one filled rectangle, so the
 * advance tells which glyph was used:
 * <ul>
 * <li>glyph 1 for <code>A</code>: advance 1000; glyph 2 (alternate 1 of A): advance 300; glyph 3 (alternate 2 of A): advance 500;</li>
 * <li>glyph 4 for <code>B</code>: advance 700; glyph 5 (the <code>ss02</code> form of B): advance 200;</li>
 * <li>features of the default language system of <code>DFLT</code>: <code>ss01</code> (lookup 0, single substitution format 2: 1 to 2), <code>salt</code>
 * (lookup 1, alternate substitution: 1 to {2, 3}), <code>ss02</code> (lookup 2, an extension lookup of a single substitution format 1: 4 to 5 by delta 1),
 * <code>swsh</code> (lookup 1 as well).</li>
 * </ul>
 * <p>
 * 시험이 직접 만드는 작은 합성 TrueType 글꼴(누구의 글꼴 파일도 복사하지 않음)이며, <code>font-variant-alternates</code> 와 <code>@font-feature-values</code> 시험을 위한 대체 글리프
 * <code>GSUB</code> 표를 가진다. em 당 단위는 1000 이고 모든 글리프는 채운 사각형 하나이므로 advance 로 어느 글리프를 썼는지 안다(위 목록).
 */
final class SyntheticGsubFont {

    /** The advances of the glyphs 0 to 5. 글리프 0~5 의 advance. */
    static final int[] ADVANCES = { 500, 1000, 300, 500, 700, 200 };

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private SyntheticGsubFont() {
    }

    /**
     * Builds the font file.
     * <p>
     * 글꼴 파일을 만든다.
     *
     * @return the TrueType bytes / TrueType 바이트
     */
    static byte[] build() {
        Map<String, byte[]> t = new TreeMap<String, byte[]>();
        int n = ADVANCES.length;
        // glyf 와 loca: 글리프 0 은 비어 있고 나머지는 사각형 하나이다.
        Out glyf = new Out();
        // 글리프마다 loca 위치를 적고 사각형 윤곽을 쓴다.
        int[] loca = new int[n + 1];
        for (int g = 0; g < n; g++) {
            loca[g] = glyf.size();
            if (g > 0) {
                int w = ADVANCES[g] - 100;
                glyf.u16(1);
                glyf.u16(50);
                glyf.u16(0);
                glyf.u16(50 + w);
                glyf.u16(700);
                glyf.u16(3);
                glyf.u16(0);
                // 네 점 모두 곡선 위 점이며 좌표는 16 비트 차이로 적는다.
                for (int k = 0; k < 4; k++) {
                    glyf.u8(1);
                }
                // 점 (50,0), (50,700), (50+w,700), (50+w,0) 의 x 차이와 y 차이.
                glyf.u16(50);
                glyf.u16(0);
                glyf.u16(w);
                glyf.u16(0);
                glyf.u16(0);
                glyf.u16(700);
                glyf.u16(0);
                glyf.u16(-700 & 0xFFFF);
                glyf.pad();
            }
        }
        loca[n] = glyf.size();
        t.put("glyf", glyf.bytes());
        Out lo = new Out();
        // 긴 형식 loca 를 적는다.
        for (int g = 0; g <= n; g++) {
            lo.u32(loca[g]);
        }
        t.put("loca", lo.bytes());
        Out head = new Out();
        head.u32(0x00010000);
        head.u32(0x00010000);
        head.u32(0);
        head.u32(0x5F0F3CF5);
        head.u16(0x000B);
        head.u16(1000);
        head.u32(0);
        head.u32(0);
        head.u32(0);
        head.u32(0);
        head.u16(0);
        head.u16(0);
        head.u16(1000);
        head.u16(700);
        head.u16(0);
        head.u16(8);
        head.u16(2);
        head.u16(1);
        head.u16(0);
        t.put("head", head.bytes());
        Out hhea = new Out();
        hhea.u32(0x00010000);
        hhea.u16(800);
        hhea.u16(-200 & 0xFFFF);
        hhea.u16(0);
        hhea.u16(1000);
        hhea.u16(0);
        hhea.u16(0);
        hhea.u16(1000);
        hhea.u16(1);
        hhea.u16(0);
        hhea.u16(0);
        // 예약된 네 칸.
        for (int k = 0; k < 4; k++) {
            hhea.u16(0);
        }
        hhea.u16(0);
        hhea.u16(n);
        t.put("hhea", hhea.bytes());
        Out hmtx = new Out();
        // 글리프마다 advance 와 왼쪽 여백을 적는다.
        for (int g = 0; g < n; g++) {
            hmtx.u16(ADVANCES[g]);
            hmtx.u16(g == 0 ? 0 : 50);
        }
        t.put("hmtx", hmtx.bytes());
        Out maxp = new Out();
        maxp.u32(0x00010000);
        maxp.u16(n);
        maxp.u16(4);
        maxp.u16(1);
        maxp.u16(0);
        maxp.u16(0);
        maxp.u16(2);
        // 나머지 maxp 필드는 0 이다.
        for (int k = 0; k < 8; k++) {
            maxp.u16(0);
        }
        t.put("maxp", maxp.bytes());
        t.put("cmap", cmap());
        t.put("name", name("Gsub Test"));
        t.put("post", post());
        t.put("OS/2", os2());
        t.put("GSUB", gsub());
        return assemble(t);
    }

    /**
     * The <code>cmap</code> table: A to glyph 1, B to glyph 4 (format 4 of platform 3, encoding 1).
     * <p>
     * <code>cmap</code> 표: A 는 글리프 1, B 는 글리프 4(플랫폼 3, 인코딩 1 의 형식 4).
     *
     * @return the table / 표
     */
    private static byte[] cmap() {
        Out o = new Out();
        o.u16(0);
        o.u16(1);
        o.u16(3);
        o.u16(1);
        o.u32(12);
        int seg = 3;
        o.u16(4);
        o.u16(16 + 8 * seg);
        o.u16(0);
        o.u16(seg * 2);
        o.u16(4);
        o.u16(1);
        o.u16(2);
        o.u16(0x41);
        o.u16(0x42);
        o.u16(0xFFFF);
        o.u16(0);
        o.u16(0x41);
        o.u16(0x42);
        o.u16(0xFFFF);
        o.u16((1 - 0x41) & 0xFFFF);
        o.u16((4 - 0x42) & 0xFFFF);
        o.u16(1);
        o.u16(0);
        o.u16(0);
        o.u16(0);
        return o.bytes();
    }

    /**
     * The <code>GSUB</code> table of the features described in the class comment.
     * <p>
     * 클래스 설명의 기능을 담은 <code>GSUB</code> 표.
     *
     * @return the table / 표
     */
    private static byte[] gsub() {
        // ScriptList: DFLT 하나, 기본 언어 체계가 기능 0~3 을 가리킨다.
        Out script = new Out();
        script.u16(1);
        script.tag("DFLT");
        script.u16(8);
        script.u16(4);
        script.u16(0);
        script.u16(0);
        script.u16(0xFFFF);
        script.u16(4);
        script.u16(0);
        script.u16(1);
        script.u16(2);
        script.u16(3);
        // FeatureList: ss01 -> 0, salt -> 1, ss02 -> 2, swsh -> 1.
        String[] tags = { "ss01", "salt", "ss02", "swsh" };
        int[] lookupOf = { 0, 1, 2, 1 };
        Out feature = new Out();
        feature.u16(tags.length);
        int base = 2 + 6 * tags.length;
        // 기능 기록마다 태그와 기능 표의 위치를 적는다.
        for (int i = 0; i < tags.length; i++) {
            feature.tag(tags[i]);
            feature.u16(base + 6 * i);
        }
        // 기능 표마다 조회 번호 하나를 적는다.
        for (int i = 0; i < tags.length; i++) {
            feature.u16(0);
            feature.u16(1);
            feature.u16(lookupOf[i]);
        }
        // LookupList: 세 조회.
        byte[] l0 = lookup(1, single2());
        byte[] l1 = lookup(3, alternate());
        byte[] l2 = lookup(7, extension(1, single1()));
        Out lookups = new Out();
        lookups.u16(3);
        int at = 2 + 2 * 3;
        lookups.u16(at);
        lookups.u16(at + l0.length);
        lookups.u16(at + l0.length + l1.length);
        lookups.raw(l0);
        lookups.raw(l1);
        lookups.raw(l2);
        Out g = new Out();
        g.u32(0x00010000);
        int s = 10;
        byte[] sb = script.bytes();
        byte[] fb = feature.bytes();
        g.u16(s);
        g.u16(s + sb.length);
        g.u16(s + sb.length + fb.length);
        g.raw(sb);
        g.raw(fb);
        g.raw(lookups.bytes());
        return g.bytes();
    }

    /**
     * A lookup table with one subtable.
     * <p>
     * 부표 하나를 가진 조회 표.
     *
     * @param type     the lookup type / 조회 종류
     * @param subtable the subtable / 부표
     * @return the lookup / 조회
     */
    private static byte[] lookup(int type, byte[] subtable) {
        Out o = new Out();
        o.u16(type);
        o.u16(0);
        o.u16(1);
        o.u16(8);
        o.raw(subtable);
        return o.bytes();
    }

    /**
     * A single substitution of format 2: glyph 1 to glyph 2.
     * <p>
     * 형식 2 의 단일 치환: 글리프 1 을 글리프 2 로.
     *
     * @return the subtable / 부표
     */
    private static byte[] single2() {
        Out o = new Out();
        o.u16(2);
        o.u16(8);
        o.u16(1);
        o.u16(2);
        o.u16(1);
        o.u16(1);
        o.u16(1);
        return o.bytes();
    }

    /**
     * A single substitution of format 1: glyph 4 to glyph 5 (delta 1), with a coverage of format 2.
     * <p>
     * 형식 1 의 단일 치환: 글리프 4 를 글리프 5 로(차이 1). 덮는 범위는 형식 2.
     *
     * @return the subtable / 부표
     */
    private static byte[] single1() {
        Out o = new Out();
        o.u16(1);
        o.u16(6);
        o.u16(1);
        o.u16(2);
        o.u16(1);
        o.u16(4);
        o.u16(4);
        o.u16(0);
        return o.bytes();
    }

    /**
     * An alternate substitution: glyph 1 to the alternates {2, 3}.
     * <p>
     * 대체 치환: 글리프 1 을 대체 {2, 3} 으로.
     *
     * @return the subtable / 부표
     */
    private static byte[] alternate() {
        Out o = new Out();
        o.u16(1);
        o.u16(14);
        o.u16(1);
        o.u16(8);
        o.u16(2);
        o.u16(2);
        o.u16(3);
        o.u16(1);
        o.u16(1);
        o.u16(1);
        return o.bytes();
    }

    /**
     * An extension subtable that points to a subtable of another type.
     * <p>
     * 다른 종류의 부표를 가리키는 확장 부표.
     *
     * @param type     the type of the real subtable / 실제 부표의 종류
     * @param subtable the real subtable / 실제 부표
     * @return the extension subtable / 확장 부표
     */
    private static byte[] extension(int type, byte[] subtable) {
        Out o = new Out();
        o.u16(1);
        o.u16(type);
        o.u32(8);
        o.raw(subtable);
        return o.bytes();
    }

    /**
     * A <code>name</code> table with the family, subfamily, full and PostScript names (platform 3, encoding 1, English).
     * <p>
     * 패밀리, 하위 패밀리, 전체, PostScript 이름을 가진 <code>name</code> 표(플랫폼 3, 인코딩 1, 영어).
     *
     * @param family the family / 패밀리
     * @return the table / 표
     */
    private static byte[] name(String family) {
        String[] values = { family, "Regular", family + " Regular", family.replace(' ', '-') + "-Regular" };
        int[] ids = { 1, 2, 4, 6 };
        Out strings = new Out();
        Out o = new Out();
        o.u16(0);
        o.u16(ids.length);
        o.u16(6 + 12 * ids.length);
        // 이름 기록마다 UTF-16 문자열의 위치를 적는다.
        for (int i = 0; i < ids.length; i++) {
            int off = strings.size();
            // 문자를 UTF-16BE 로 적는다.
            for (int k = 0; k < values[i].length(); k++) {
                strings.u16(values[i].charAt(k));
            }
            o.u16(3);
            o.u16(1);
            o.u16(0x409);
            o.u16(ids[i]);
            o.u16(strings.size() - off);
            o.u16(off);
        }
        o.raw(strings.bytes());
        return o.bytes();
    }

    /**
     * A <code>post</code> table of format 3 (no glyph names).
     * <p>
     * 형식 3 의 <code>post</code> 표(글리프 이름 없음).
     *
     * @return the table / 표
     */
    private static byte[] post() {
        Out o = new Out();
        o.u32(0x00030000);
        // 나머지 필드는 0 이다.
        for (int k = 0; k < 7; k++) {
            o.u32(0);
        }
        return o.bytes();
    }

    /**
     * An <code>OS/2</code> table of version 3.
     * <p>
     * 판 3 의 <code>OS/2</code> 표.
     *
     * @return the table / 표
     */
    private static byte[] os2() {
        Out o = new Out();
        o.u16(3);
        o.u16(600);
        o.u16(400);
        o.u16(5);
        o.u16(0);
        // 아래첨자, 위첨자, 취소선 필드(10 개).
        for (int k = 0; k < 10; k++) {
            o.u16(0);
        }
        o.u16(0);
        // PANOSE(10 바이트).
        for (int k = 0; k < 10; k++) {
            o.u8(0);
        }
        // 유니코드 범위 4 개.
        for (int k = 0; k < 4; k++) {
            o.u32(k == 0 ? 1 : 0);
        }
        o.tag("NONE");
        o.u16(0x40);
        o.u16(0x41);
        o.u16(0x42);
        o.u16(800);
        o.u16(-200 & 0xFFFF);
        o.u16(0);
        o.u16(800);
        o.u16(200);
        o.u32(1);
        o.u32(0);
        o.u16(700);
        o.u16(700);
        o.u16(0);
        o.u16(0x20);
        o.u16(0);
        return o.bytes();
    }

    /**
     * Assembles the tables into a font file with the table directory and the checksums.
     * <p>
     * 표를 표 목록과 체크섬이 있는 글꼴 파일로 묶는다.
     *
     * @param tables the tables by tag / 태그별 표
     * @return the font / 글꼴
     */
    private static byte[] assemble(Map<String, byte[]> tables) {
        int n = tables.size();
        Out o = new Out();
        o.u32(0x00010000);
        o.u16(n);
        o.u16(128);
        o.u16(3);
        o.u16(n * 16 - 128);
        int at = 12 + 16 * n;
        // 표 목록: 태그 순서로 위치와 길이를 적는다.
        for (Map.Entry<String, byte[]> e : tables.entrySet()) {
            byte[] b = e.getValue();
            o.tag(e.getKey());
            o.u32(checksum(b));
            o.u32(at);
            o.u32(b.length);
            at += (b.length + 3) & ~3;
        }
        // 표 내용을 4 바이트로 정렬해 적는다.
        for (byte[] b : tables.values()) {
            o.raw(b);
            o.pad();
        }
        return o.bytes();
    }

    /**
     * Computes a table checksum.
     * <p>
     * 표 체크섬을 계산한다.
     *
     * @param b the table / 표
     * @return the checksum / 체크섬
     */
    private static int checksum(byte[] b) {
        long sum = 0;
        // 4 바이트 낱말을 더한다.
        for (int i = 0; i < b.length; i += 4) {
            long w = 0;
            // 낱말의 바이트를 모은다.
            for (int k = 0; k < 4; k++) {
                w = (w << 8) | (i + k < b.length ? b[i + k] & 255 : 0);
            }
            sum = (sum + w) & 0xFFFFFFFFL;
        }
        return (int) sum;
    }

    /**
     * A big-endian byte writer.
     * <p>
     * 빅 엔디언 바이트 쓰는 쪽.
     */
    private static final class Out {
        /** The bytes. 바이트. */
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        /**
         * Writes a byte.
         * <p>
         * 바이트를 쓴다.
         *
         * @param v the value / 값
         */
        void u8(int v) {
            out.write(v);
        }

        /**
         * Writes 16 bits.
         * <p>
         * 16 비트를 쓴다.
         *
         * @param v the value / 값
         */
        void u16(int v) {
            out.write(v >>> 8);
            out.write(v);
        }

        /**
         * Writes 32 bits.
         * <p>
         * 32 비트를 쓴다.
         *
         * @param v the value / 값
         */
        void u32(int v) {
            u16(v >>> 16);
            u16(v & 0xFFFF);
        }

        /**
         * Writes a tag of four characters.
         * <p>
         * 네 글자 태그를 쓴다.
         *
         * @param t the tag / 태그
         */
        void tag(String t) {
            // 글자마다 한 바이트이다.
            for (int i = 0; i < 4; i++) {
                out.write(t.charAt(i));
            }
        }

        /**
         * Writes bytes.
         * <p>
         * 바이트를 쓴다.
         *
         * @param b the bytes / 바이트
         */
        void raw(byte[] b) {
            out.write(b, 0, b.length);
        }

        /**
         * Pads to a multiple of four.
         * <p>
         * 4 의 배수로 채운다.
         */
        void pad() {
            // 4 의 배수가 될 때까지 0 을 쓴다.
            while ((out.size() & 3) != 0) {
                out.write(0);
            }
        }

        /**
         * Returns the size.
         * <p>
         * 크기를 반환한다.
         *
         * @return the size / 크기
         */
        int size() {
            return out.size();
        }

        /**
         * Returns the bytes.
         * <p>
         * 바이트를 반환한다.
         *
         * @return the bytes / 바이트
         */
        byte[] bytes() {
            return out.toByteArray();
        }
    }
}
