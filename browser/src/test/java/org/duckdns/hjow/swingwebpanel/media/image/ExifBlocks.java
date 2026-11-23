package org.duckdns.hjow.swingwebpanel.media.image;

/**
 * Builds EXIF (TIFF) blocks for tests: a block with one directory that holds the orientation tag, in either byte order, with or without the
 * six-byte <code>Exif\0\0</code> prefix that JPEG uses. Also maps a pixel through each EXIF orientation by the definition of the orientations,
 * the reference that the decoders are checked against.
 * <p>
 * 시험용 EXIF(TIFF) 블록을 만든다: 방향 태그 하나가 든 디렉터리 하나짜리 블록이며 어느 바이트 순서로든, JPEG 가 쓰는 여섯 바이트
 * <code>Exif\0\0</code> 머리말의 유무와 상관없이 만든다. 또 각 EXIF 방향의 정의대로 픽셀의 위치를 옮겨 준다. 디코더를 이 기준과 대조한다.
 */
public final class ExifBlocks {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ExifBlocks() {
    }

    /**
     * Builds the TIFF structure of an EXIF block with only the orientation tag: the header, one directory of one entry (tag 0x0112, type
     * SHORT, count 1) and the end of the directory chain.
     * <p>
     * 방향 태그만 가진 EXIF 블록의 TIFF 구조를 만든다: 헤더, 항목 하나(태그 0x0112, 형식 SHORT, 개수 1)의 디렉터리 하나, 디렉터리 사슬의 끝.
     *
     * @param orientation the value of the tag (not checked, so that invalid values can be tested) / 태그의 값(잘못된 값도 시험할 수 있게 검사하지 않는다)
     * @param big         true for big-endian (MM), false for little-endian (II) / 빅 엔디언(MM)이면 true, 리틀 엔디언(II)이면 false
     * @return the block / 블록
     */
    public static byte[] tiff(int orientation, boolean big) {
        Bytes b = new Bytes();
        if (big) {
            b.ascii("MM").be16(42).be32(8).be16(1).be16(0x0112).be16(3).be32(1).be16(orientation).be16(0).be32(0);
        } else {
            b.ascii("II").le16(42).le32(8).le16(1).le16(0x0112).le16(3).le32(1).le16(orientation).le16(0).le32(0);
        }
        return b.toBytes();
    }

    /**
     * The same block with the <code>Exif\0\0</code> prefix in front (some PNG and WebP writers add it).
     * <p>
     * 앞에 <code>Exif\0\0</code> 머리말이 붙은 같은 블록(일부 PNG 와 WebP 작성기가 붙인다).
     *
     * @param orientation the value of the tag / 태그의 값
     * @param big         true for big-endian / 빅 엔디언이면 true
     * @return the block / 블록
     */
    public static byte[] prefixed(int orientation, boolean big) {
        byte[] t = tiff(orientation, big);
        byte[] out = new byte[t.length + 6];
        out[0] = 'E';
        out[1] = 'x';
        out[2] = 'i';
        out[3] = 'f';
        System.arraycopy(t, 0, out, 6, t.length);
        return out;
    }

    /**
     * Maps a point of the stored image to its place after an EXIF orientation (the definition of the eight orientations: 2 mirrors left to
     * right, 3 turns by 180 degrees, 4 mirrors top to bottom, 5 transposes, 6 turns 90 degrees clockwise, 7 transverses, 8 turns 270 degrees
     * clockwise).
     * <p>
     * 저장된 이미지의 점이 EXIF 방향을 적용한 뒤 놓이는 곳을 계산한다(여덟 방향의 정의: 2 좌우 반전, 3 180도 회전, 4 상하 반전, 5 전치, 6 시계 방향
     * 90도 회전, 7 역전치, 8 시계 방향 270도 회전).
     *
     * @param o orientation 1 to 8 / 방향 1~8
     * @param x stored column / 저장된 열
     * @param y stored row / 저장된 행
     * @param w stored width / 저장된 가로
     * @param h stored height / 저장된 세로
     * @return {column, row} after the orientation / 방향 적용 뒤의 {열, 행}
     */
    public static int[] forward(int o, int x, int y, int w, int h) {
        switch (o) {
        case 2:
            return new int[] { w - 1 - x, y };
        case 3:
            return new int[] { w - 1 - x, h - 1 - y };
        case 4:
            return new int[] { x, h - 1 - y };
        case 5:
            return new int[] { y, x };
        case 6:
            return new int[] { h - 1 - y, x };
        case 7:
            return new int[] { h - 1 - y, w - 1 - x };
        case 8:
            return new int[] { y, w - 1 - x };
        default:
            return new int[] { x, y };
        }
    }

    /**
     * Applies an orientation to a pixel array by {@link #forward(int, int, int, int, int)} (the reference).
     * <p>
     * {@link #forward(int, int, int, int, int)} 로 픽셀 배열에 방향을 적용한다(기준).
     *
     * @param src stored pixels / 저장된 픽셀
     * @param w   stored width / 저장된 가로
     * @param h   stored height / 저장된 세로
     * @param o   orientation 1 to 8 / 방향 1~8
     * @return the pixels after the orientation (width and height swapped for 5 to 8) / 방향 적용 뒤의 픽셀(5~8 은 가로와 세로가 바뀜)
     */
    public static int[] reference(int[] src, int w, int h, int o) {
        int ow = o >= 5 ? h : w;
        int[] out = new int[src.length];
        // 저장된 점마다 가야 할 곳으로 옮긴다.
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int[] p = forward(o, x, y, w, h);
                out[p[1] * ow + p[0]] = src[y * w + x];
            }
        }
        return out;
    }
}
