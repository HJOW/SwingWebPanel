package org.duckdns.hjow.swingwebpanel.media.image.avif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers of the AVIF tests (plan item P18-03): reading test resources, the md5 of decoded planes, the index files of the fixtures and a
 * builder of small HEIF files around real AV1 payloads, so that container features (grid, alpha, rotation, mirroring, cropping, other
 * construction methods) can be tested without an encoder.
 * <p>
 * AVIF 시험의 도우미(계획 P18-03): 시험 자료 읽기, 디코딩한 평면의 md5, 시험 자료 색인 파일, 실제 AV1 페이로드를 감싸는 작은 HEIF 파일 작성기.
 * 인코더 없이 컨테이너 기능(격자, 알파, 회전, 거울, 자르기, 다른 구성 방법)을 시험할 수 있게 한다.
 */
final class AvifTestKit {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스.
     */
    private AvifTestKit() {
    }

    /**
     * Reads a test resource from the class path.
     * <p>
     * 클래스패스에서 시험 자료를 읽는다.
     *
     * @param name the absolute resource name / 절대 자료 이름
     * @return the bytes / 바이트
     * @throws IOException if the resource is missing or cannot be read / 자료가 없거나 읽을 수 없으면
     */
    static byte[] resource(String name) throws IOException {
        InputStream in = AvifTestKit.class.getResourceAsStream(name);
        if (in == null) {
            throw new IOException("missing test resource " + name);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            // 끝까지 읽어 모은다.
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    /**
     * Reads the lines of an index file without comments and empty lines.
     * <p>
     * 색인 파일의 줄을 주석과 빈 줄 없이 읽는다.
     *
     * @param name the absolute resource name / 절대 자료 이름
     * @return the lines / 줄
     * @throws IOException if the resource cannot be read / 자료를 읽을 수 없으면
     */
    static List<String> indexLines(String name) throws IOException {
        String text = new String(resource(name), "UTF-8");
        List<String> lines = new ArrayList<String>();
        // 줄마다 주석(#)과 빈 줄을 건너뛴다.
        for (String l : text.split("\n")) {
            String t = l.trim();
            if (t.length() > 0 && !t.startsWith("#")) {
                lines.add(t);
            }
        }
        return lines;
    }

    /**
     * The md5 of the visible samples of a picture: planes in order, row by row; 8 bit samples as bytes, other depths as 16 bit little endian
     * words (the layout of the raw video output of ffmpeg).
     * <p>
     * 화면의 보이는 샘플의 md5: 평면 순서, 행 순서. 8비트 샘플은 바이트로, 그 외 비트 깊이는 16비트 리틀 엔디언 워드로 쓴다(ffmpeg raw 비디오 출력의
     * 배치).
     *
     * @param p the picture / 화면
     * @return the hexadecimal md5 / 16진수 md5
     * @throws Exception if md5 is not available / md5 를 쓸 수 없으면
     */
    static String md5(Av1Picture p) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        // 평면과 행을 순서대로 해시에 넣는다.
        for (int pl = 0; pl < p.numPlanes; pl++) {
            int w = p.planeWidth(pl);
            int h = p.planeHeight(pl);
            byte[] row = new byte[p.bitDepth > 8 ? w * 2 : w];
            // 행을 순서대로 해시에 넣는다.
            for (int y = 0; y < h; y++) {
                // 행의 샘플을 바이트로 옮긴다.
                for (int x = 0; x < w; x++) {
                    int v = p.planes[pl][y * p.stride[pl] + x];
                    if (p.bitDepth > 8) {
                        row[2 * x] = (byte) v;
                        row[2 * x + 1] = (byte) (v >> 8);
                    } else {
                        row[x] = (byte) v;
                    }
                }
                md.update(row);
            }
        }
        StringBuilder sb = new StringBuilder();
        byte[] d = md.digest();
        // 바이트를 16진수 두 자리로 바꾼다.
        for (int i = 0; i < d.length; i++) {
            sb.append(Character.forDigit((d[i] >> 4) & 15, 16)).append(Character.forDigit(d[i] & 15, 16));
        }
        return sb.toString();
    }

    /**
     * Checks that a block fails with a {@link MediaException} of a kind.
     * <p>
     * 블록이 지정한 종류의 {@link MediaException} 으로 실패하는지 검사한다.
     *
     * @param kind  the expected kind / 기대하는 종류
     * @param what  the description / 설명
     * @param block the code / 코드
     */
    static void fails(MediaException.Kind kind, String what, Block block) {
        Throwable t = Check.throwsException(MediaException.class, block, what);
        Check.equal(kind, ((MediaException) t).getKind(), what + ": kind (message: " + t.getMessage() + ")");
    }

    /**
     * Concatenates byte arrays.
     * <p>
     * 바이트 배열을 이어 붙인다.
     *
     * @param parts the parts / 조각
     * @return the joined array / 이어 붙인 배열
     */
    static byte[] cat(byte[]... parts) {
        int n = 0;
        // 전체 길이를 구한다.
        for (byte[] p : parts) {
            n += p.length;
        }
        byte[] out = new byte[n];
        int w = 0;
        // 조각을 차례로 복사한다.
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, w, p.length);
            w += p.length;
        }
        return out;
    }

    /**
     * Big endian bytes of a number.
     * <p>
     * 수의 빅 엔디언 바이트.
     *
     * @param value the value / 값
     * @param size  the number of bytes / 바이트 수
     * @return the bytes / 바이트
     */
    static byte[] be(long value, int size) {
        byte[] b = new byte[size];
        // 낮은 자리부터 채운다.
        for (int i = size - 1; i >= 0; i--) {
            b[i] = (byte) value;
            value >>= 8;
        }
        return b;
    }

    /**
     * The bytes of a text (Latin-1).
     * <p>
     * 텍스트의 바이트(Latin-1).
     *
     * @param s the text / 텍스트
     * @return the bytes / 바이트
     */
    static byte[] ascii(String s) {
        byte[] b = new byte[s.length()];
        // 문자를 바이트로 옮긴다.
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) s.charAt(i);
        }
        return b;
    }

    /**
     * Builds a box.
     * <p>
     * 상자를 만든다.
     *
     * @param type  the four character type / 네 글자 종류
     * @param parts the payload parts / 페이로드 조각
     * @return the box / 상자
     */
    static byte[] box(String type, byte[]... parts) {
        byte[] payload = cat(parts);
        return cat(be(payload.length + 8, 4), ascii(type), payload);
    }

    /**
     * Builds a full box (with version and flags).
     * <p>
     * 전체 상자(버전과 플래그 포함)를 만든다.
     *
     * @param type    the four character type / 네 글자 종류
     * @param version the version / 버전
     * @param flags   the flags / 플래그
     * @param parts   the payload parts / 페이로드 조각
     * @return the box / 상자
     */
    static byte[] fullBox(String type, int version, int flags, byte[]... parts) {
        return box(type, cat(be(version, 1), be(flags, 3), cat(parts)));
    }

    /**
     * A builder of small HEIF files with AVIF items.
     * <p>
     * AVIF 항목을 가진 작은 HEIF 파일의 작성기.
     */
    static final class Builder {

        /**
         * The item identifiers.
         * <p>
         * 항목 식별자.
         */
        private final List<Integer> ids = new ArrayList<Integer>();

        /**
         * The item types.
         * <p>
         * 항목 종류.
         */
        private final List<String> types = new ArrayList<String>();

        /**
         * The item data (null for items without data).
         * <p>
         * 항목 데이터(데이터가 없는 항목은 null).
         */
        private final List<byte[]> data = new ArrayList<byte[]>();

        /**
         * The property boxes; the first has the index 1.
         * <p>
         * 속성 상자. 첫째가 색인 1.
         */
        private final List<byte[]> props = new ArrayList<byte[]>();

        /**
         * The associations: item id, property index, essential flag.
         * <p>
         * 연결: 항목 id, 속성 색인, 필수 플래그.
         */
        private final List<int[]> assoc = new ArrayList<int[]>();

        /**
         * The references: type, from item, then the target items.
         * <p>
         * 참조: 종류, 출발 항목, 대상 항목.
         */
        private final List<Object[]> refs = new ArrayList<Object[]>();

        /**
         * The primary item (-1 for none).
         * <p>
         * 기본 항목(-1 은 없음).
         */
        int primary = 1;

        /**
         * True to keep the item data in the item data box (construction method 1).
         * <p>
         * 항목 데이터를 항목 데이터 상자에 두려면(구성 방법 1) true.
         */
        boolean useIdat;

        /**
         * True to split each item into two extents.
         * <p>
         * 항목을 범위 둘로 나누려면 true.
         */
        boolean splitExtents;

        /**
         * The major brand.
         * <p>
         * 주 브랜드.
         */
        String brand = "avif";

        /**
         * True to add a movie box.
         * <p>
         * 무비 상자를 더하려면 true.
         */
        boolean movie;

        /**
         * Adds an item.
         * <p>
         * 항목을 더한다.
         *
         * @param id   the identifier / 식별자
         * @param type the four character type / 네 글자 종류
         * @param d    the item data or null / 항목 데이터 또는 null
         * @return this builder / 이 작성기
         */
        Builder item(int id, String type, byte[] d) {
            ids.add(Integer.valueOf(id));
            types.add(type);
            data.add(d);
            return this;
        }

        /**
         * Adds a property box and returns its index.
         * <p>
         * 속성 상자를 더하고 색인을 반환한다.
         *
         * @param b the property box / 속성 상자
         * @return the index (1 based) / 색인(1부터)
         */
        int prop(byte[] b) {
            props.add(b);
            return props.size();
        }

        /**
         * Associates a property with an item.
         * <p>
         * 속성을 항목에 연결한다.
         *
         * @param item      the item / 항목
         * @param index     the property index / 속성 색인
         * @param essential the essential flag / 필수 플래그
         * @return this builder / 이 작성기
         */
        Builder assoc(int item, int index, boolean essential) {
            assoc.add(new int[] { item, index, essential ? 1 : 0 });
            return this;
        }

        /**
         * Adds a reference.
         * <p>
         * 참조를 더한다.
         *
         * @param type the reference type / 참조 종류
         * @param from the referring item / 참조하는 항목
         * @param to   the referenced items / 참조되는 항목
         * @return this builder / 이 작성기
         */
        Builder ref(String type, int from, int... to) {
            refs.add(new Object[] { type, Integer.valueOf(from), to });
            return this;
        }

        /**
         * Builds the file.
         * <p>
         * 파일을 만든다.
         *
         * @return the file bytes / 파일 바이트
         */
        byte[] build() {
            byte[] ftyp = box("ftyp", ascii(brand), be(0, 4), ascii("avif"), ascii("mif1"), ascii("miaf"));
            // 첫 번째 계산은 오프셋을 0 으로 두고 meta 크기를 구하고, 두 번째에 실제 오프셋을 채운다.
            byte[] meta = meta(0);
            long mdatStart = ftyp.length + meta.length + 8;
            meta = meta(mdatStart);
            byte[] mdat = box("mdat", payloads(false));
            byte[] tail = movie ? box("moov", fullBox("mvhd", 0, 0, new byte[96])) : new byte[0];
            return cat(ftyp, meta, mdat, tail);
        }

        /**
         * The concatenated item data that goes to the media data box (empty if the item data box is used).
         * <p>
         * 미디어 데이터 상자로 가는 항목 데이터의 연결(항목 데이터 상자를 쓰면 비어 있다).
         *
         * @param idat true to return the data of the item data box / 항목 데이터 상자의 데이터를 돌려주려면 true
         * @return the data / 데이터
         */
        private byte[] payloads(boolean idat) {
            if (useIdat != idat) {
                return new byte[0];
            }
            byte[][] parts = new byte[data.size()][];
            // 데이터가 없는 항목은 빈 배열로 둔다.
            for (int i = 0; i < parts.length; i++) {
                parts[i] = data.get(i) == null ? new byte[0] : data.get(i);
            }
            return cat(parts);
        }

        /**
         * Builds the meta box.
         * <p>
         * meta 상자를 만든다.
         *
         * @param mdatStart the file offset of the first byte of the media data / 미디어 데이터 첫 바이트의 파일 오프셋
         * @return the box / 상자
         */
        private byte[] meta(long mdatStart) {
            byte[] hdlr = fullBox("hdlr", 0, 0, be(0, 4), ascii("pict"), new byte[12], new byte[] { 0 });
            byte[] pitm = primary >= 0 ? fullBox("pitm", 0, 0, be(primary, 2)) : new byte[0];
            byte[] infe = new byte[0];
            // 항목마다 infe 상자를 만든다.
            for (int i = 0; i < ids.size(); i++) {
                infe = cat(infe, fullBox("infe", 2, 0, be(ids.get(i).intValue(), 2), be(0, 2), ascii(types.get(i)), new byte[] { 0 }));
            }
            byte[] iinf = fullBox("iinf", 0, 0, be(ids.size(), 2), infe);
            byte[] iloc = iloc(mdatStart);
            byte[] iref = new byte[0];
            if (!refs.isEmpty()) {
                byte[] all = new byte[0];
                // 참조마다 상자를 만든다.
                for (Object[] r : refs) {
                    int[] to = (int[]) r[2];
                    byte[] t = new byte[0];
                    // 참조 대상 식별자를 이어 붙인다.
                    for (int x : to) {
                        t = cat(t, be(x, 2));
                    }
                    all = cat(all, box((String) r[0], be(((Integer) r[1]).intValue(), 2), be(to.length, 2), t));
                }
                iref = fullBox("iref", 0, 0, all);
            }
            byte[] ipco = new byte[0];
            // 속성 상자를 이어 붙인다.
            for (byte[] p : props) {
                ipco = cat(ipco, p);
            }
            // 항목별로 연결을 묶어 ipma 를 만든다.
            byte[] ipmaBody = new byte[0];
            int count = 0;
            // 항목마다 연결된 속성 색인을 모은다.
            for (int i = 0; i < ids.size(); i++) {
                int id = ids.get(i).intValue();
                byte[] list = new byte[0];
                int n = 0;
                // 항목의 연결만 고른다.
                for (int[] a : assoc) {
                    if (a[0] == id) {
                        list = cat(list, new byte[] { (byte) ((a[2] != 0 ? 0x80 : 0) | a[1]) });
                        n++;
                    }
                }
                if (n > 0) {
                    ipmaBody = cat(ipmaBody, be(id, 2), new byte[] { (byte) n }, list);
                    count++;
                }
            }
            byte[] ipma = fullBox("ipma", 0, 0, be(count, 4), ipmaBody);
            byte[] iprp = box("iprp", box("ipco", ipco), ipma);
            byte[] idat = useIdat ? box("idat", payloads(true)) : new byte[0];
            return fullBox("meta", 0, 0, hdlr, pitm, iloc, iinf, iref, iprp, idat);
        }

        /**
         * Builds the item location box.
         * <p>
         * 항목 위치 상자를 만든다.
         *
         * @param mdatStart the file offset of the first byte of the media data / 미디어 데이터 첫 바이트의 파일 오프셋
         * @return the box / 상자
         */
        private byte[] iloc(long mdatStart) {
            byte[] items = new byte[0];
            long offset = useIdat ? 0 : mdatStart;
            // 항목마다 위치를 기록한다. 데이터가 없는 항목은 건너뛴다.
            for (int i = 0; i < ids.size(); i++) {
                byte[] d = data.get(i);
                if (d == null) {
                    continue;
                }
                byte[] extents;
                int n;
                if (splitExtents && d.length > 1) {
                    int half = d.length / 2;
                    extents = cat(be(offset, 4), be(half, 4), be(offset + half, 4), be(d.length - half, 4));
                    n = 2;
                } else {
                    extents = cat(be(offset, 4), be(d.length, 4));
                    n = 1;
                }
                items = cat(items, be(ids.get(i).intValue(), 2), be(useIdat ? 1 : 0, 2), be(0, 2), be(n, 2), extents);
                offset += d.length;
            }
            int count = 0;
            // 데이터가 있는 항목 수를 센다.
            for (byte[] d : data) {
                if (d != null) {
                    count++;
                }
            }
            return fullBox("iloc", 1, 0, new byte[] { 0x44, 0x00 }, be(count, 2), items);
        }
    }

    /**
     * The property box ispe.
     * <p>
     * ispe 속성 상자.
     *
     * @param w the width / 너비
     * @param h the height / 높이
     * @return the box / 상자
     */
    static byte[] ispe(int w, int h) {
        return fullBox("ispe", 0, 0, be(w, 4), be(h, 4));
    }

    /**
     * The property box av1C without configuration OBUs.
     * <p>
     * 구성 OBU 가 없는 av1C 속성 상자.
     *
     * @return the box / 상자
     */
    static byte[] av1C() {
        return box("av1C", new byte[] { (byte) 0x81, 0, 0, 0 });
    }

    /**
     * The property box irot.
     * <p>
     * irot 속성 상자.
     *
     * @param angle the number of anti-clockwise quarter turns / 반시계 방향 90도 회전 횟수
     * @return the box / 상자
     */
    static byte[] irot(int angle) {
        return box("irot", new byte[] { (byte) angle });
    }

    /**
     * The property box imir.
     * <p>
     * imir 속성 상자.
     *
     * @param axis the axis 0 or 1 / 축 0 또는 1
     * @return the box / 상자
     */
    static byte[] imir(int axis) {
        return box("imir", new byte[] { (byte) axis });
    }

    /**
     * The property box clap.
     * <p>
     * clap 속성 상자.
     *
     * @param w  the width of the aperture / 개구 너비
     * @param h  the height of the aperture / 개구 높이
     * @param ho the horizontal offset of the centre / 중심의 가로 오프셋
     * @param vo the vertical offset of the centre / 중심의 세로 오프셋
     * @return the box / 상자
     */
    static byte[] clap(int w, int h, int ho, int vo) {
        return box("clap", be(w, 4), be(1, 4), be(h, 4), be(1, 4), be(ho, 4), be(1, 4), be(vo, 4), be(1, 4));
    }

    /**
     * The property box auxC of an alpha plane.
     * <p>
     * 알파 평면의 auxC 속성 상자.
     *
     * @return the box / 상자
     */
    static byte[] auxAlpha() {
        return fullBox("auxC", 0, 0, ascii("urn:mpeg:mpegB:cicp:systems:auxiliary:alpha"), new byte[] { 0 });
    }
}
