package org.duckdns.hjow.swingwebpanel.media.image.color;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds ICC profiles for the tests, byte by byte from ICC.1:2010 (the 128-byte header, the tag table and the tag data, every tag aligned to
 * four bytes): matrix/TRC RGB profiles, gray TRC profiles and profiles that the reader must refuse (a look-up table profile, a Lab profile, a
 * CMYK profile). The curves are encoded as the <code>curv</code> type (identity, u8Fixed8 gamma, 16-bit table) and the <code>para</code>
 * type. The profile reader of the product is thereby checked against profiles that were written independently of it.
 * <p>
 * 시험용 ICC 프로파일을 ICC.1:2010 에서 바이트 단위로 만든다(128바이트 헤더, 태그 표, 4바이트 정렬된 태그 데이터): 행렬/TRC RGB 프로파일, 회색조 TRC
 * 프로파일, 읽기가 거부해야 하는 프로파일(룩업 테이블 프로파일, Lab 프로파일, CMYK 프로파일). 곡선은 <code>curv</code> 형식(항등, u8Fixed8 감마,
 * 16비트 표)과 <code>para</code> 형식으로 인코딩한다. 제품의 프로파일 읽기를 그것과 독립적으로 쓴 프로파일에 대조하게 된다.
 */
public final class IccBuilder {

    /**
     * The tags collected so far: {signature, data}.
     * <p>
     * 지금까지 모은 태그: {시그니처, 데이터}.
     */
    private final List<Object[]> tags = new ArrayList<Object[]>();

    /**
     * The colour space signature of the header.
     * <p>
     * 헤더의 색 공간 시그니처.
     */
    private String space = "RGB ";

    /**
     * The profile connection space signature of the header.
     * <p>
     * 헤더의 프로파일 연결 공간 시그니처.
     */
    private String pcs = "XYZ ";

    /**
     * The profile class signature of the header.
     * <p>
     * 헤더의 프로파일 등급 시그니처.
     */
    private String cls = "mntr";

    /**
     * The major version number.
     * <p>
     * 주 버전 번호.
     */
    private int major = 4;

    /**
     * Creates a builder for an RGB display profile (version 4).
     * <p>
     * RGB 화면 프로파일(버전 4)의 빌더를 만든다.
     */
    public IccBuilder() {
    }

    /**
     * Sets the data colour space, the connection space and the class.
     * <p>
     * 데이터 색 공간, 연결 공간, 등급을 정한다.
     *
     * @param space the data colour space (for example <code>GRAY</code>) / 데이터 색 공간(예: <code>GRAY</code>)
     * @param pcs   the connection space (<code>XYZ </code> or <code>Lab </code>) / 연결 공간(<code>XYZ </code> 또는 <code>Lab </code>)
     * @param cls   the class (<code>mntr</code>, <code>link</code>, ...) / 등급(<code>mntr</code>, <code>link</code> 등)
     * @return this builder / 이 빌더
     */
    public IccBuilder spaces(String space, String pcs, String cls) {
        this.space = space;
        this.pcs = pcs;
        this.cls = cls;
        return this;
    }

    /**
     * Sets the major version (2 or 4).
     * <p>
     * 주 버전(2 또는 4)을 정한다.
     *
     * @param v the version / 버전
     * @return this builder / 이 빌더
     */
    public IccBuilder version(int v) {
        this.major = v;
        return this;
    }

    /**
     * Adds a tag with raw data.
     * <p>
     * 원시 데이터가 든 태그를 더한다.
     *
     * @param sig  the four-character signature / 네 글자 시그니처
     * @param data the data / 데이터
     * @return this builder / 이 빌더
     */
    public IccBuilder tag(String sig, byte[] data) {
        tags.add(new Object[] { sig, data });
        return this;
    }

    /**
     * Adds the colorant tags of a matrix profile from three XYZ columns relative to D50.
     * <p>
     * D50 기준의 XYZ 열 셋으로 행렬 프로파일의 색소 태그를 더한다.
     *
     * @param r red colorant {X, Y, Z} / 빨강 색소 {X, Y, Z}
     * @param g green colorant / 초록 색소
     * @param b blue colorant / 파랑 색소
     * @return this builder / 이 빌더
     */
    public IccBuilder colorants(double[] r, double[] g, double[] b) {
        return tag("rXYZ", xyz(r)).tag("gXYZ", xyz(g)).tag("bXYZ", xyz(b));
    }

    /**
     * Adds the three curve tags of a matrix profile.
     * <p>
     * 행렬 프로파일의 곡선 태그 셋을 더한다.
     *
     * @param r red curve data / 빨강 곡선 데이터
     * @param g green curve data / 초록 곡선 데이터
     * @param b blue curve data / 파랑 곡선 데이터
     * @return this builder / 이 빌더
     */
    public IccBuilder curves(byte[] r, byte[] g, byte[] b) {
        return tag("rTRC", r).tag("gTRC", g).tag("bTRC", b);
    }

    /**
     * Writes a big-endian 32-bit value.
     * <p>
     * 빅 엔디언 32비트 값을 쓴다.
     *
     * @param out the destination / 목적지
     * @param v   the value / 값
     */
    private static void u32(ByteArrayOutputStream out, long v) {
        out.write((int) (v >> 24) & 0xFF);
        out.write((int) (v >> 16) & 0xFF);
        out.write((int) (v >> 8) & 0xFF);
        out.write((int) v & 0xFF);
    }

    /**
     * Converts a number to the s15Fixed16 format.
     * <p>
     * 수를 s15Fixed16 형식으로 바꾼다.
     *
     * @param v the value / 값
     * @return the 32-bit pattern / 32비트 패턴
     */
    private static int fixed(double v) {
        return (int) Math.round(v * 65536.0);
    }

    /**
     * Encodes an <code>XYZ </code> tag.
     * <p>
     * <code>XYZ </code> 태그를 인코딩한다.
     *
     * @param v {X, Y, Z} / {X, Y, Z}
     * @return the tag data / 태그 데이터
     */
    public static byte[] xyz(double[] v) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write('X');
        o.write('Y');
        o.write('Z');
        o.write(' ');
        u32(o, 0);
        // 세 값을 s15Fixed16 으로 쓴다.
        for (int i = 0; i < 3; i++) {
            u32(o, fixed(v[i]) & 0xFFFFFFFFL);
        }
        return o.toByteArray();
    }

    /**
     * Encodes a <code>curv</code> tag: no entry is the identity, one entry a u8Fixed8 gamma, more entries a table of 16-bit values.
     * <p>
     * <code>curv</code> 태그를 인코딩한다: 항목이 없으면 항등, 하나이면 u8Fixed8 감마, 더 많으면 16비트 값의 표.
     *
     * @param entries the entries as written (gamma times 256 for the single entry, 0 to 65535 for a table) / 쓰는 그대로의 항목(항목 하나는 감마 곱하기 256, 표는 0~65535)
     * @return the tag data / 태그 데이터
     */
    public static byte[] curv(int... entries) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write('c');
        o.write('u');
        o.write('r');
        o.write('v');
        u32(o, 0);
        u32(o, entries.length);
        // 항목을 16비트씩 쓴다.
        for (int i = 0; i < entries.length; i++) {
            o.write((entries[i] >> 8) & 0xFF);
            o.write(entries[i] & 0xFF);
        }
        return o.toByteArray();
    }

    /**
     * Encodes a <code>para</code> tag.
     * <p>
     * <code>para</code> 태그를 인코딩한다.
     *
     * @param type   the function type 0 to 4 / 함수 형식 0~4
     * @param params the parameters g, a, b, c, d, e, f as needed by the type / 형식에 필요한 매개변수 g, a, b, c, d, e, f
     * @return the tag data / 태그 데이터
     */
    public static byte[] para(int type, double... params) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write('p');
        o.write('a');
        o.write('r');
        o.write('a');
        u32(o, 0);
        o.write((type >> 8) & 0xFF);
        o.write(type & 0xFF);
        o.write(0);
        o.write(0);
        // 매개변수를 s15Fixed16 으로 쓴다.
        for (int i = 0; i < params.length; i++) {
            u32(o, fixed(params[i]) & 0xFFFFFFFFL);
        }
        return o.toByteArray();
    }

    /**
     * Builds the profile: the header, the tag table and the data of all tags.
     * <p>
     * 프로파일을 만든다: 헤더, 태그 표, 모든 태그의 데이터.
     *
     * @return the profile bytes / 프로파일 바이트
     */
    public byte[] build() {
        int dataStart = 128 + 4 + 12 * tags.size();
        ByteArrayOutputStream table = new ByteArrayOutputStream();
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        u32(table, tags.size());
        // 태그마다 4바이트 경계에 데이터를 놓고 표에 시그니처, 위치, 크기를 쓴다.
        for (int i = 0; i < tags.size(); i++) {
            String sig = (String) tags.get(i)[0];
            byte[] d = (byte[]) tags.get(i)[1];
            // 시그니처 네 글자를 쓴다.
            for (int k = 0; k < 4; k++) {
                table.write(sig.charAt(k));
            }
            u32(table, dataStart + data.size());
            u32(table, d.length);
            data.write(d, 0, d.length);
            // 다음 태그를 위해 4바이트 경계까지 0 으로 채운다.
            while (data.size() % 4 != 0) {
                data.write(0);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        u32(out, dataStart + data.size());
        u32(out, 0);
        out.write(major);
        out.write(major == 4 ? 0x20 : 0x40);
        out.write(0);
        out.write(0);
        // 등급, 데이터 색 공간, 연결 공간을 쓴다.
        String[] names = { cls, space, pcs };
        for (int n = 0; n < names.length; n++) {
            // 이름 네 글자를 쓴다.
            for (int k = 0; k < 4; k++) {
                out.write(names[n].charAt(k));
            }
        }
        // 날짜(12바이트)를 0 으로 채우고 'acsp' 시그니처를 36번 위치에 쓴다.
        for (int i = 0; i < 12; i++) {
            out.write(0);
        }
        out.write('a');
        out.write('c');
        out.write('s');
        out.write('p');
        // 플랫폼, 플래그, 제조사, 모델, 속성, 의도(40~67)는 0 이다.
        for (int i = 0; i < 28; i++) {
            out.write(0);
        }
        // 조명(D50)을 68번 위치에 쓰고 나머지 헤더는 0 으로 채운다.
        u32(out, fixed(0.9642) & 0xFFFFFFFFL);
        u32(out, fixed(1.0) & 0xFFFFFFFFL);
        u32(out, fixed(0.8249) & 0xFFFFFFFFL);
        // 헤더가 128바이트가 될 때까지 0 으로 채운다.
        while (out.size() < 128) {
            out.write(0);
        }
        byte[] t = table.toByteArray();
        out.write(t, 0, t.length);
        byte[] dd = data.toByteArray();
        out.write(dd, 0, dd.length);
        return out.toByteArray();
    }

    /**
     * Builds a matrix/TRC RGB profile.
     * <p>
     * 행렬/TRC RGB 프로파일을 만든다.
     *
     * @param version the major version 2 or 4 / 주 버전 2 또는 4
     * @param r       red colorant relative to D50 / D50 기준 빨강 색소
     * @param g       green colorant / 초록 색소
     * @param b       blue colorant / 파랑 색소
     * @param curve   the curve data used for all three channels / 세 채널에 모두 쓰는 곡선 데이터
     * @return the profile / 프로파일
     */
    public static byte[] rgb(int version, double[] r, double[] g, double[] b, byte[] curve) {
        return new IccBuilder().version(version).colorants(r, g, b).curves(curve, curve, curve).build();
    }

    /**
     * Builds a gray TRC profile.
     * <p>
     * 회색조 TRC 프로파일을 만든다.
     *
     * @param curve the curve data / 곡선 데이터
     * @return the profile / 프로파일
     */
    public static byte[] gray(byte[] curve) {
        return new IccBuilder().spaces("GRAY", "XYZ ", "mntr").tag("kTRC", curve).build();
    }
}
