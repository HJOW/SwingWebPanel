package org.duckdns.hjow.swingwebpanel.media.audio;

/**
 * Helpers to read integers of a given byte order from a byte array, with overflow-safe range checks. Decoders use them for headers;
 * reading outside the array is never silently allowed: callers check {@link #has(byte[], int, int)} first.
 * <p>
 * 바이트 배열에서 지정한 바이트 순서의 정수를 읽는 도우미이며 범위 검사는 오버플로 없이 한다. 디코더가 헤더를 읽을 때 쓴다. 배열 밖을 조용히
 * 읽지 않으므로 호출자는 먼저 {@link #has(byte[], int, int)} 로 확인한다.
 */
public final class Bytes {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private Bytes() {
    }

    /**
     * Tells whether <code>count</code> bytes starting at <code>offset</code> lie inside the array (no int overflow).
     * <p>
     * <code>offset</code> 부터 <code>count</code> 바이트가 배열 안에 있는지 알려 준다(int 오버플로 없음).
     *
     * @param d      array / 배열
     * @param offset start / 시작 위치
     * @param count  number of bytes / 바이트 수
     * @return true if the range is inside the array / 범위가 배열 안이면 true
     */
    public static boolean has(byte[] d, int offset, int count) {
        return offset >= 0 && count >= 0 && (long) offset + count <= d.length;
    }

    /**
     * Tells whether the ASCII text is found at the offset.
     * <p>
     * 오프셋에 ASCII 문자열이 있는지 알려 준다.
     *
     * @param d      array / 배열
     * @param offset position / 위치
     * @param ascii  text made of ASCII characters / ASCII 문자로 된 문자열
     * @return true if the bytes equal the text / 바이트가 문자열과 같으면 true
     */
    public static boolean matches(byte[] d, int offset, String ascii) {
        if (!has(d, offset, ascii.length())) {
            return false;
        }
        // 글자를 하나씩 비교한다.
        for (int i = 0; i < ascii.length(); i++) {
            if (d[offset + i] != (byte) ascii.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads an unsigned byte.
     * <p>
     * 부호 없는 바이트를 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return 0..255
     */
    public static int u8(byte[] d, int o) {
        return d[o] & 0xFF;
    }

    /**
     * Reads an unsigned 16-bit little-endian integer.
     * <p>
     * 부호 없는 16비트 리틀 엔디언 정수를 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return value / 값
     */
    public static int le16(byte[] d, int o) {
        return (d[o] & 0xFF) | ((d[o + 1] & 0xFF) << 8);
    }

    /**
     * Reads an unsigned 32-bit little-endian integer as a long.
     * <p>
     * 부호 없는 32비트 리틀 엔디언 정수를 long 으로 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return value 0..2^32-1 / 값
     */
    public static long le32(byte[] d, int o) {
        return (d[o] & 0xFFL) | ((d[o + 1] & 0xFFL) << 8) | ((d[o + 2] & 0xFFL) << 16) | ((d[o + 3] & 0xFFL) << 24);
    }

    /**
     * Reads a 64-bit little-endian integer.
     * <p>
     * 64비트 리틀 엔디언 정수를 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return value / 값
     */
    public static long le64(byte[] d, int o) {
        return le32(d, o) | (le32(d, o + 4) << 32);
    }

    /**
     * Reads an unsigned 16-bit big-endian integer.
     * <p>
     * 부호 없는 16비트 빅 엔디언 정수를 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return value / 값
     */
    public static int be16(byte[] d, int o) {
        return ((d[o] & 0xFF) << 8) | (d[o + 1] & 0xFF);
    }

    /**
     * Reads an unsigned 32-bit big-endian integer as a long.
     * <p>
     * 부호 없는 32비트 빅 엔디언 정수를 long 으로 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return value 0..2^32-1 / 값
     */
    public static long be32(byte[] d, int o) {
        return ((d[o] & 0xFFL) << 24) | ((d[o + 1] & 0xFFL) << 16) | ((d[o + 2] & 0xFFL) << 8) | (d[o + 3] & 0xFFL);
    }

    /**
     * Reads an unsigned 24-bit big-endian integer.
     * <p>
     * 부호 없는 24비트 빅 엔디언 정수를 읽는다.
     *
     * @param d array / 배열
     * @param o offset / 위치
     * @return value / 값
     */
    public static int be24(byte[] d, int o) {
        return ((d[o] & 0xFF) << 16) | ((d[o + 1] & 0xFF) << 8) | (d[o + 2] & 0xFF);
    }

    /**
     * Returns the data as an ASCII string of the given length for messages.
     * <p>
     * 메시지에 쓰도록 주어진 길이만큼을 ASCII 문자열로 반환한다.
     *
     * @param d   array / 배열
     * @param o   offset / 위치
     * @param len length / 길이
     * @return text with non-printable bytes replaced by '?' / 출력할 수 없는 바이트를 '?' 로 바꾼 문자열
     */
    public static String ascii(byte[] d, int o, int len) {
        StringBuilder sb = new StringBuilder();
        // 바이트마다 출력 가능한 ASCII 인지 확인해 글자로 바꾼다.
        for (int i = 0; i < len && o + i < d.length; i++) {
            int c = d[o + i] & 0xFF;
            sb.append(c >= 32 && c < 127 ? (char) c : '?');
        }
        return sb.toString();
    }

    /**
     * Tells whether <code>count</code> bytes starting at <code>offset</code> lie inside the source.
     * <p>
     * <code>offset</code> 부터 <code>count</code> 바이트가 원천 안에 있는지 알려 준다.
     *
     * @param d      source / 원천
     * @param offset start / 시작 위치
     * @param count  number of bytes / 바이트 수
     * @return true if the range is inside the source / 범위가 원천 안이면 true
     */
    public static boolean has(ByteSource d, long offset, int count) {
        return offset >= 0 && count >= 0 && offset + count <= d.length();
    }

    /**
     * Tells whether the ASCII text is found at the offset of a source.
     * <p>
     * 원천의 오프셋에 ASCII 문자열이 있는지 알려 준다.
     *
     * @param d      source / 원천
     * @param offset position / 위치
     * @param ascii  text made of ASCII characters / ASCII 문자로 된 문자열
     * @return true if the bytes equal the text / 바이트가 문자열과 같으면 true
     */
    public static boolean matches(ByteSource d, long offset, String ascii) {
        if (!has(d, offset, ascii.length())) {
            return false;
        }
        // 글자를 하나씩 비교한다.
        for (int i = 0; i < ascii.length(); i++) {
            if (d.get(offset + i) != (ascii.charAt(i) & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads an unsigned byte of a source.
     * <p>
     * 원천의 부호 없는 바이트를 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static int u8(ByteSource d, long o) {
        return d.get(o);
    }

    /**
     * Reads an unsigned little-endian 16-bit value of a source.
     * <p>
     * 원천의 부호 없는 리틀 엔디언 16비트 값을 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static int le16(ByteSource d, long o) {
        return d.get(o) | (d.get(o + 1) << 8);
    }

    /**
     * Reads an unsigned little-endian 32-bit value of a source.
     * <p>
     * 원천의 부호 없는 리틀 엔디언 32비트 값을 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static long le32(ByteSource d, long o) {
        return d.get(o) | ((long) d.get(o + 1) << 8) | ((long) d.get(o + 2) << 16) | ((long) d.get(o + 3) << 24);
    }

    /**
     * Reads a little-endian 64-bit value of a source.
     * <p>
     * 원천의 리틀 엔디언 64비트 값을 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static long le64(ByteSource d, long o) {
        return le32(d, o) | (le32(d, o + 4) << 32);
    }

    /**
     * Reads an unsigned big-endian 16-bit value of a source.
     * <p>
     * 원천의 부호 없는 빅 엔디언 16비트 값을 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static int be16(ByteSource d, long o) {
        return (d.get(o) << 8) | d.get(o + 1);
    }

    /**
     * Reads an unsigned big-endian 32-bit value of a source.
     * <p>
     * 원천의 부호 없는 빅 엔디언 32비트 값을 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static long be32(ByteSource d, long o) {
        return ((long) d.get(o) << 24) | ((long) d.get(o + 1) << 16) | ((long) d.get(o + 2) << 8) | d.get(o + 3);
    }

    /**
     * Reads an unsigned big-endian 24-bit value of a source.
     * <p>
     * 원천의 부호 없는 빅 엔디언 24비트 값을 읽는다.
     *
     * @param d source / 원천
     * @param o offset / 위치
     * @return value / 값
     */
    public static int be24(ByteSource d, long o) {
        return (d.get(o) << 16) | (d.get(o + 1) << 8) | d.get(o + 2);
    }

    /**
     * Reads ASCII text of a source (stops at its end; non-printable bytes become '?').
     * <p>
     * 원천의 ASCII 문자열을 읽는다(끝에서 멈추며 출력할 수 없는 바이트는 '?').
     *
     * @param d   source / 원천
     * @param o   offset / 위치
     * @param len maximum length / 최대 길이
     * @return text / 문자열
     */
    public static String ascii(ByteSource d, long o, int len) {
        StringBuilder sb = new StringBuilder();
        long end = d.length();
        // 바이트마다 출력 가능한 ASCII 인지 확인해 글자로 바꾼다.
        for (int i = 0; i < len && o + i < end; i++) {
            int c = d.get(o + i);
            sb.append(c >= 32 && c < 127 ? (char) c : '?');
        }
        return sb.toString();
    }

    /**
     * Copies a range of a source into a new array; the range must lie inside the source.
     * <p>
     * 원천의 범위를 새 배열로 복사한다. 범위는 원천 안에 있어야 한다.
     *
     * @param d      source / 원천
     * @param offset start / 시작
     * @param count  number of bytes / 바이트 수
     * @return the bytes / 바이트
     * @throws IndexOutOfBoundsException if the range is not inside the source / 범위가 원천 안이 아니면
     */
    public static byte[] copy(ByteSource d, long offset, int count) {
        if (!has(d, offset, count)) {
            throw new IndexOutOfBoundsException("range " + offset + "+" + count + " outside 0.." + d.length());
        }
        byte[] b = new byte[count];
        int n = d.read(offset, b, 0, count);
        if (n != count) {
            throw new IndexOutOfBoundsException("short read at " + offset);
        }
        return b;
    }

    /**
     * Copies a range of a source into an array; the range must lie inside the source.
     * <p>
     * 원천의 범위를 배열로 복사한다. 범위는 원천 안에 있어야 한다.
     *
     * @param d      source / 원천
     * @param offset start / 시작
     * @param buffer destination / 대상
     * @param at     start in the destination / 대상의 시작
     * @param count  number of bytes / 바이트 수
     * @throws IndexOutOfBoundsException if the range is not inside the source / 범위가 원천 안이 아니면
     */
    public static void readFully(ByteSource d, long offset, byte[] buffer, int at, int count) {
        if (!has(d, offset, count) || d.read(offset, buffer, at, count) != count) {
            throw new IndexOutOfBoundsException("range " + offset + "+" + count + " outside 0.." + d.length());
        }
    }
}
