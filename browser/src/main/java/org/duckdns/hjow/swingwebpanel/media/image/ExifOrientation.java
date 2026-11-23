package org.duckdns.hjow.swingwebpanel.media.image;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Reads the orientation tag of an EXIF block and applies it to a decoded image (plan item P18-03). The EXIF block is a TIFF structure
 * (CIPA DC-008, Exif 2.32, section 4.5: the TIFF header, then the first image file directory with 12-byte entries); the orientation is the
 * SHORT tag <code>0x0112</code> with the values 1 to 8 (1 upright, 2 mirrored horizontally, 3 rotated by 180 degrees, 4 mirrored vertically,
 * 5 transposed, 6 rotated by 90 degrees clockwise, 7 transversed, 8 rotated by 270 degrees clockwise). JPEG carries the block in an APP1
 * segment, PNG in an <code>eXIf</code> chunk and WebP in an <code>EXIF</code> chunk; this class reads the block of all three the same way,
 * so one description of the byte layout serves every format. Browsers apply the orientation by default (CSS Images 3,
 * <code>image-orientation: from-image</code>), so the decoders apply it to the pixels and keep the applied value in the image
 * ({@link DecodedImage#getAppliedOrientation()}); the style <code>image-orientation: none</code> then asks for the inverse.
 * <p>
 * EXIF 블록의 방향 태그를 읽고 디코딩된 이미지에 적용한다(계획 P18-03). EXIF 블록은 TIFF 구조이다(CIPA DC-008, Exif 2.32, 4.5절: TIFF 헤더 뒤에
 * 12바이트 항목이 있는 첫 이미지 파일 디렉터리). 방향은 SHORT 태그 <code>0x0112</code> 이고 값은 1~8 이다(1 정상, 2 좌우 반전, 3 180도 회전, 4 상하
 * 반전, 5 전치, 6 시계 방향 90도 회전, 7 역전치, 8 시계 방향 270도 회전). JPEG 은 APP1 세그먼트에, PNG 는 <code>eXIf</code> 청크에, WebP 는
 * <code>EXIF</code> 청크에 이 블록을 담는데, 이 클래스가 셋을 같은 방식으로 읽으므로 바이트 배치의 설명이 모든 형식에 하나이다. 브라우저는 방향을
 * 기본으로 적용하므로(CSS Images 3, <code>image-orientation: from-image</code>) 디코더가 픽셀에 적용하고 적용한 값을 이미지에 남기며
 * ({@link DecodedImage#getAppliedOrientation()}), 스타일 <code>image-orientation: none</code> 이 오면 그 역변환을 쓴다.
 */
public final class ExifOrientation {

    /**
     * The tag number of the orientation in the first directory.
     * <p>
     * 첫 디렉터리에서 방향 태그의 번호.
     */
    private static final int TAG_ORIENTATION = 0x0112;

    /**
     * The TIFF type SHORT (unsigned 16-bit).
     * <p>
     * TIFF 형식 SHORT(부호 없는 16비트).
     */
    private static final int TYPE_SHORT = 3;

    /**
     * The size of the TIFF header (byte order, the number 42 and the offset of the first directory).
     * <p>
     * TIFF 헤더의 크기(바이트 순서, 숫자 42, 첫 디렉터리의 위치).
     */
    private static final int HEADER = 8;

    /**
     * The size of one directory entry.
     * <p>
     * 디렉터리 항목 하나의 크기.
     */
    private static final int ENTRY = 12;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ExifOrientation() {
    }

    /**
     * Reads the orientation from an EXIF block that starts at the TIFF header. A block that carries the six bytes <code>Exif\0\0</code>
     * in front of the header (JPEG always has them; some PNG and WebP writers add them although the formats do not) is accepted too.
     * Everything that is damaged (a missing header, the number 42 wrong, offsets outside the block, a tag of another type, a value outside
     * 1 to 8) gives 1, which means upright, never an exception.
     * <p>
     * TIFF 헤더에서 시작하는 EXIF 블록에서 방향을 읽는다. 헤더 앞에 여섯 바이트 <code>Exif\0\0</code> 가 있는 블록(JPEG 은 항상 있고 일부 PNG 와
     * WebP 작성기는 형식에 없는데도 붙인다)도 받는다. 손상된 모든 것(헤더 없음, 숫자 42 가 다름, 블록 밖을 가리키는 위치, 다른 형식의 태그, 1~8 밖의
     * 값)은 예외 없이 1(정상)을 준다.
     *
     * @param d   data / 데이터
     * @param pos start of the block / 블록의 시작
     * @param len number of bytes of the block / 블록의 바이트 수
     * @return orientation 1 to 8, 1 if absent or invalid / 방향 1~8. 없거나 잘못되면 1
     */
    public static int read(byte[] d, int pos, int len) {
        if (d == null || pos < 0 || len < 0 || pos > d.length || len > d.length - pos) {
            return 1;
        }
        // 'Exif\0\0' 머리말이 있으면 건너뛴다.
        if (len >= 6 && d[pos] == 'E' && d[pos + 1] == 'x' && d[pos + 2] == 'i' && d[pos + 3] == 'f' && d[pos + 4] == 0 && d[pos + 5] == 0) {
            pos += 6;
            len -= 6;
        }
        if (len < HEADER + 2) {
            return 1;
        }
        boolean big;
        if (d[pos] == 'M' && d[pos + 1] == 'M') {
            big = true;
        } else if (d[pos] == 'I' && d[pos + 1] == 'I') {
            big = false;
        } else {
            return 1;
        }
        if (u16(d, pos + 2, big) != 42) {
            return 1;
        }
        long ifd = u32(d, pos + 4, big);
        if (ifd < HEADER || ifd + 2 > len) {
            return 1;
        }
        int count = u16(d, pos + (int) ifd, big);
        // 첫 디렉터리의 항목(각 12바이트)에서 방향 태그를 찾는다.
        for (int i = 0; i < count; i++) {
            long e = ifd + 2 + (long) ENTRY * i;
            if (e + ENTRY > len) {
                return 1;
            }
            int p = pos + (int) e;
            if (u16(d, p, big) == TAG_ORIENTATION && u16(d, p + 2, big) == TYPE_SHORT) {
                int v = u16(d, p + 8, big);
                return v >= 1 && v <= 8 ? v : 1;
            }
        }
        return 1;
    }

    /**
     * Returns the orientation that undoes the given one: applying an orientation and then its inverse gives the original pixels. Only
     * the two rotations by a quarter turn differ from their inverse (6 and 8); all other orientations (the mirror images, the half turn and
     * the two diagonal flips) undo themselves.
     * <p>
     * 주어진 방향을 되돌리는 방향을 반환한다. 방향을 적용한 뒤 그 역을 적용하면 원래 픽셀이 된다. 4분의 1 회전 둘(6, 8)만 자기 자신과 다르고, 나머지
     * 방향(거울상, 반 바퀴, 대각선 반전 둘)은 자기 자신이 역이다.
     *
     * @param orientation orientation 1 to 8 / 방향 1~8
     * @return the inverse orientation; 1 for a value outside 1 to 8 / 역방향. 1~8 밖의 값이면 1
     */
    public static int inverse(int orientation) {
        if (orientation == 6) {
            return 8;
        }
        if (orientation == 8) {
            return 6;
        }
        return orientation >= 1 && orientation <= 8 ? orientation : 1;
    }

    /**
     * Applies an orientation to an image: the pixels of every frame are turned into display orientation, the size is swapped for 5 to 8 and
     * the applied value is kept in the result. Orientation 1 returns the same object.
     * <p>
     * 이미지에 방향을 적용한다. 모든 프레임의 픽셀을 표시 방향으로 돌리고 5~8 이면 크기를 바꾸며 적용한 값을 결과에 남긴다. 방향 1 은 같은
     * 객체를 반환한다.
     *
     * @param image       the image as stored in the file / 파일에 저장된 대로의 이미지
     * @param orientation orientation 1 to 8 / 방향 1~8
     * @return the image in display orientation / 표시 방향의 이미지
     * @throws MediaException CANCELLED if the thread is interrupted / 스레드가 인터럽트되면 CANCELLED
     */
    public static DecodedImage apply(DecodedImage image, int orientation) throws MediaException {
        if (orientation < 2 || orientation > 8) {
            return image;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        int[] size = new int[2];
        java.util.List<DecodedFrame> out = new java.util.ArrayList<DecodedFrame>(image.getFrames().size());
        // 프레임마다 같은 방향을 적용한다.
        for (DecodedFrame f : image.getFrames()) {
            int[] pixels = PixelOps.orient(f.getArgb(), w, h, orientation, size);
            out.add(new DecodedFrame(pixels, f.getDelayMillis()));
        }
        return new DecodedImage(size[0], size[1], out, image.getLoopCount(), orientation);
    }

    /**
     * Reads an unsigned 16-bit value in the given byte order (the caller checked the bounds).
     * <p>
     * 지정한 바이트 순서로 부호 없는 16비트 값을 읽는다(범위는 호출자가 확인했다).
     *
     * @param d   data / 데이터
     * @param p   position / 위치
     * @param big true for big-endian / 빅 엔디언이면 true
     * @return value / 값
     */
    private static int u16(byte[] d, int p, boolean big) {
        return big ? ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF) : ((d[p + 1] & 0xFF) << 8) | (d[p] & 0xFF);
    }

    /**
     * Reads an unsigned 32-bit value in the given byte order (the caller checked the bounds).
     * <p>
     * 지정한 바이트 순서로 부호 없는 32비트 값을 읽는다(범위는 호출자가 확인했다).
     *
     * @param d   data / 데이터
     * @param p   position / 위치
     * @param big true for big-endian / 빅 엔디언이면 true
     * @return value / 값
     */
    private static long u32(byte[] d, int p, boolean big) {
        long hi = u16(d, big ? p : p + 2, big);
        long lo = u16(d, big ? p + 2 : p, big);
        return (hi << 16) | lo;
    }
}
