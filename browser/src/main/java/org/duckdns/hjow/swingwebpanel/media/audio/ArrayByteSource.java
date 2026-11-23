package org.duckdns.hjow.swingwebpanel.media.audio;

/**
 * A {@link ByteSource} over a byte array that is already in memory (a short resource, a test, the derived Ogg stream of a small WebM file).
 * The array is not copied and must not be changed while the source is used. Thread-safe (reads only).
 * <p>
 * 이미 메모리에 있는 바이트 배열 위의 {@link ByteSource}(짧은 리소스, 시험, 작은 WebM 파일에서 만든 Ogg 스트림). 배열은 복사하지 않으며 원천을 쓰는
 * 동안 바꾸면 안 된다. 읽기만 하므로 스레드 안전하다.
 */
public final class ArrayByteSource implements ByteSource {

    /**
     * The bytes.
     * <p>
     * 바이트.
     */
    private final byte[] data;

    /**
     * Wraps an array.
     * <p>
     * 배열을 감싼다.
     *
     * @param data the bytes, not null / 바이트(null 아님)
     */
    public ArrayByteSource(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        this.data = data;
    }

    /**
     * Returns the array length.
     * <p>
     * 배열 길이를 반환한다.
     *
     * @return the length / 길이
     */
    public long length() {
        return data.length;
    }

    /**
     * Returns the unsigned byte at a position, like an array access.
     * <p>
     * 배열 접근처럼 위치의 부호 없는 바이트를 반환한다.
     *
     * @param position the position / 위치
     * @return the byte / 바이트
     */
    public int get(long position) {
        if (position < 0 || position >= data.length) {
            throw new ArrayIndexOutOfBoundsException("position " + position + " outside 0.." + data.length);
        }
        return data[(int) position] & 0xFF;
    }

    /**
     * Copies bytes.
     * <p>
     * 바이트를 복사한다.
     *
     * @param position the first position / 첫 위치
     * @param buffer   the destination / 대상
     * @param offset   the start in the destination / 대상의 시작
     * @param length   the bytes wanted / 원하는 바이트 수
     * @return the bytes copied / 복사한 바이트 수
     */
    public int read(long position, byte[] buffer, int offset, int length) {
        if (position < 0 || offset < 0 || length < 0 || (long) offset + length > buffer.length) {
            throw new IndexOutOfBoundsException("invalid read range");
        }
        if (position >= data.length) {
            return 0;
        }
        int n = (int) Math.min(length, data.length - position);
        System.arraycopy(data, (int) position, buffer, offset, n);
        return n;
    }

    /**
     * Nothing to release.
     * <p>
     * 놓을 것이 없다.
     */
    public void close() {
        return;
    }
}
