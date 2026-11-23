package org.duckdns.hjow.swingwebpanel.media.image.webp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedFrame;

/**
 * The WebP files that were produced by the WebP encoder of Chrome (libwebp) for the tests of {@link WebpDecoder}, together with the pixels that
 * Chrome decoded from them (stored as a hash of every pixel row). The fixture files are text: one fixture per line with the fields
 * <code>name|width|height|length:fnv|base64 data|row hashes</code>. Chrome encoded images drawn on a canvas (<code>toDataURL("image/webp")</code>,
 * quality 1.0 gives the lossless format), the colour profile chunk was removed, and the reference rows were read back through WebGL without
 * premultiplying the alpha, so they are exactly what libwebp decodes. The length and the FNV-1a hash of the data detect a damaged copy of a
 * fixture.
 * <p>
 * {@link WebpDecoder} 시험을 위해 Chrome 의 WebP 인코더(libwebp)가 만든 WebP 파일과 Chrome 이 그 파일에서 디코딩한 픽셀(픽셀 행마다 해시로 저장).
 * 자료 파일은 텍스트이며 한 줄에 하나씩 <code>이름|가로|세로|길이:fnv|base64 데이터|행 해시</code> 필드를 가진다. Chrome 이 캔버스에 그린 그림을
 * 인코딩했고(<code>toDataURL("image/webp")</code>, 품질 1.0 이면 무손실 형식), 색 프로파일 청크는 제거했으며, 기준 행은 알파를 미리 곱하지 않는
 * WebGL 로 읽었으므로 libwebp 가 디코딩한 값 그대로이다. 데이터의 길이와 FNV-1a 해시로 자료의 복사 오류를 찾아낸다.
 */
final class WebpFixtures {

    /**
     * Resource files that hold the fixtures.
     * <p>
     * 자료가 들어 있는 리소스 파일.
     */
    private static final String[] FILES = { "/org/duckdns/hjow/swingwebpanel/testdata/webp/chrome-fixtures.txt",
        "/org/duckdns/hjow/swingwebpanel/testdata/webp/chrome-fixtures-b.txt" };

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private WebpFixtures() {
    }

    /**
     * One fixture.
     * <p>
     * 자료 하나.
     */
    static final class Fixture {

        /**
         * Name of the fixture.
         * <p>
         * 자료 이름.
         */
        final String name;

        /**
         * Width of the image.
         * <p>
         * 이미지 가로.
         */
        final int width;

        /**
         * Height of the image.
         * <p>
         * 이미지 세로.
         */
        final int height;

        /**
         * The WebP file.
         * <p>
         * WebP 파일.
         */
        final byte[] data;

        /**
         * FNV-1a hash of every row of RGBA bytes that Chrome decoded.
         * <p>
         * Chrome 이 디코딩한 RGBA 바이트의 행별 FNV-1a 해시.
         */
        final long[] rowHashes;

        /**
         * Creates a fixture.
         * <p>
         * 자료를 만든다.
         *
         * @param name      name / 이름
         * @param width     width / 가로
         * @param height    height / 세로
         * @param data      file data / 파일 데이터
         * @param rowHashes row hashes / 행 해시
         */
        Fixture(String name, int width, int height, byte[] data, long[] rowHashes) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.data = data;
            this.rowHashes = rowHashes;
        }
    }

    /**
     * Loads all fixtures and verifies that none was damaged when it was copied into the resource files.
     * <p>
     * 모든 자료를 읽고 리소스 파일로 옮기는 중에 손상된 것이 없는지 검증한다.
     *
     * @return the fixtures / 자료들
     * @throws IOException if a resource cannot be read or a fixture is damaged / 리소스를 읽을 수 없거나 자료가 손상되었을 때
     */
    static List<Fixture> load() throws IOException {
        List<Fixture> all = new ArrayList<Fixture>();
        // 자료 파일마다 한 줄씩 읽는다.
        for (int f = 0; f < FILES.length; f++) {
            InputStream in = WebpFixtures.class.getResourceAsStream(FILES[f]);
            if (in == null) {
                throw new IOException("missing resource " + FILES[f]);
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "US-ASCII"));
            try {
                String line;
                // 빈 줄은 건너뛰고 필드를 나눠 자료를 만든다.
                while ((line = r.readLine()) != null) {
                    if (line.trim().length() > 0) {
                        all.add(parse(line));
                    }
                }
            } finally {
                r.close();
            }
        }
        return all;
    }

    /**
     * Reads a class path resource completely.
     * <p>
     * 클래스패스 리소스를 끝까지 읽는다.
     *
     * @param name resource name starting with a slash / 슬래시로 시작하는 리소스 이름
     * @return the bytes / 바이트
     * @throws IOException if the resource is missing / 리소스가 없을 때
     */
    static byte[] readResource(String name) throws IOException {
        InputStream in = WebpFixtures.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException("missing resource " + name);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        try {
            // 끝까지 읽어 모은다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return out.toByteArray();
    }

    /**
     * Returns a fixture by name.
     * <p>
     * 이름으로 자료를 찾는다.
     *
     * @param name fixture name / 자료 이름
     * @return the fixture / 자료
     * @throws IOException if there is none / 없을 때
     */
    static Fixture get(String name) throws IOException {
        List<Fixture> all = load();
        // 이름이 같은 자료를 찾는다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).name.equals(name)) {
                return all.get(i);
            }
        }
        throw new IOException("no fixture " + name);
    }

    /**
     * Parses one line of a fixture file.
     * <p>
     * 자료 파일의 한 줄을 해석한다.
     *
     * @param line the line / 줄
     * @return the fixture / 자료
     * @throws IOException if the data does not match its length and hash / 데이터가 길이와 해시에 맞지 않을 때
     */
    private static Fixture parse(String line) throws IOException {
        String[] p = line.split("\\|", -1);
        byte[] data = base64(p[4]);
        String[] check = p[3].split(":");
        if (data.length != Integer.parseInt(check[0]) || fnv(data) != Long.parseLong(check[1], 16)) {
            throw new IOException("fixture " + p[0] + " is damaged: length " + data.length + " hash " + Long.toHexString(fnv(data)));
        }
        String[] rows = p[5].split(",");
        long[] hashes = new long[rows.length];
        // 행 해시를 16진수에서 읽는다.
        for (int i = 0; i < rows.length; i++) {
            hashes[i] = Long.parseLong(rows[i], 16);
        }
        int height = Integer.parseInt(p[2]);
        if (hashes.length != height) {
            throw new IOException("fixture " + p[0] + " has " + hashes.length + " row hashes for " + height + " rows");
        }
        return new Fixture(p[0], Integer.parseInt(p[1]), height, data, hashes);
    }

    /**
     * Decodes standard Base64 (Java 6 has no public Base64 class).
     * <p>
     * 표준 Base64 를 디코딩한다(Java 6 에는 공개 Base64 클래스가 없다).
     *
     * @param s the text / 문자열
     * @return the bytes / 바이트
     */
    static byte[] base64(String s) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        int pad = s.endsWith("==") ? 2 : (s.endsWith("=") ? 1 : 0);
        byte[] out = new byte[s.length() / 4 * 3 - pad];
        int acc = 0;
        int bits = 0;
        int o = 0;
        // 문자마다 6비트씩 모아 바이트가 되면 내보낸다.
        for (int i = 0; i < s.length() && s.charAt(i) != '='; i++) {
            acc = (acc << 6) | alphabet.indexOf(s.charAt(i));
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[o++] = (byte) (acc >> bits);
            }
        }
        return out;
    }

    /**
     * Computes the FNV-1a hash of bytes (32 bits).
     * <p>
     * 바이트의 FNV-1a 해시(32비트)를 계산한다.
     *
     * @param b bytes / 바이트
     * @return the hash / 해시
     */
    static long fnv(byte[] b) {
        int h = 0x811c9dc5;
        // 바이트마다 해시를 갱신한다.
        for (int i = 0; i < b.length; i++) {
            h ^= b[i] & 0xff;
            h *= 16777619;
        }
        return h & 0xffffffffL;
    }

    /**
     * Computes the row hashes of a frame in the form of the reference (RGBA bytes, FNV-1a per row).
     * <p>
     * 프레임의 행 해시를 기준과 같은 형식(RGBA 바이트, 행별 FNV-1a)으로 계산한다.
     *
     * @param frame  the frame / 프레임
     * @param width  width / 가로
     * @param height height / 세로
     * @return one hash per row / 행마다 해시 하나
     */
    static long[] rowHashes(DecodedFrame frame, int width, int height) {
        int[] argb = frame.getArgb();
        long[] out = new long[height];
        // 행마다 RGBA 순서의 바이트에 FNV-1a 를 적용한다.
        for (int y = 0; y < height; y++) {
            int h = 0x811c9dc5;
            // 행의 모든 픽셀을 처리한다.
            for (int x = 0; x < width; x++) {
                int p = argb[y * width + x];
                int[] comps = { (p >> 16) & 0xff, (p >> 8) & 0xff, p & 0xff, p >>> 24 };
                // 픽셀의 네 성분을 차례로 해시에 섞는다.
                for (int c = 0; c < 4; c++) {
                    h ^= comps[c];
                    h *= 16777619;
                }
            }
            out[y] = h & 0xffffffffL;
        }
        return out;
    }
}
