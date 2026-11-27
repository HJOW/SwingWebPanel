package org.duckdns.hjow.swingwebpanel.tls13.proto;

import org.duckdns.hjow.swingwebpanel.tls13.Tls13Exception;

/**
 * Reads TLS structures from a byte range with bounds checks: reading past the end is a <code>decode_error</code>
 * ({@link Tls13Exception}), never an index error. Example:
 * <pre>
 * ByteReader r = new ByteReader(body);
 * int version = r.u16();
 * byte[] random = r.bytes(32);
 * ByteReader extensions = r.sub16();   // a nested reader over a vector with a two byte length
 * r.expectEnd();
 * </pre>
 * <p>
 * 바이트 범위에서 TLS 구조를 범위 검사와 함께 읽는다: 끝을 넘어 읽으면 인덱스 오류가 아니라 <code>decode_error</code>({@link Tls13Exception})이다.
 */
public final class ByteReader {

    /** The array. / 배열. */
    private final byte[] buf;

    /** The current position. / 현재 위치. */
    private int pos;

    /** The end of the range. / 범위의 끝. */
    private final int end;

    /**
     * Creates a reader over a whole array.
     * <p>
     * 배열 전체를 읽는 해석기를 만든다.
     *
     * @param data the array / 배열
     */
    public ByteReader(byte[] data) {
        this(data, 0, data.length);
    }

    /**
     * Creates a reader over a range.
     * <p>
     * 범위를 읽는 해석기를 만든다.
     *
     * @param data the array / 배열
     * @param off  the start / 시작
     * @param end  the end / 끝
     */
    public ByteReader(byte[] data, int off, int end) {
        this.buf = data;
        this.pos = off;
        this.end = end;
    }

    /**
     * Checks that a number of bytes is left.
     * <p>
     * 그만큼의 바이트가 남았는지 확인한다.
     *
     * @param n the number of bytes / 바이트 수
     * @throws Tls13Exception decode_error if fewer bytes are left / 남은 바이트가 적으면 decode_error
     */
    private void need(int n) throws Tls13Exception {
        if (n < 0 || end - pos < n) {
            throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "message is truncated");
        }
    }

    /**
     * Returns the number of bytes left.
     * <p>
     * 남은 바이트 수를 반환한다.
     *
     * @return the count / 개수
     */
    public int remaining() {
        return end - pos;
    }

    /**
     * Reads one byte.
     * <p>
     * 한 바이트를 읽는다.
     *
     * @return the value / 값
     * @throws Tls13Exception decode_error at the end / 끝이면 decode_error
     */
    public int u8() throws Tls13Exception {
        need(1);
        return buf[pos++] & 0xff;
    }

    /**
     * Reads a two byte integer.
     * <p>
     * 2바이트 정수를 읽는다.
     *
     * @return the value / 값
     * @throws Tls13Exception decode_error at the end / 끝이면 decode_error
     */
    public int u16() throws Tls13Exception {
        need(2);
        int v = ((buf[pos] & 0xff) << 8) | (buf[pos + 1] & 0xff);
        pos += 2;
        return v;
    }

    /**
     * Reads a three byte integer.
     * <p>
     * 3바이트 정수를 읽는다.
     *
     * @return the value / 값
     * @throws Tls13Exception decode_error at the end / 끝이면 decode_error
     */
    public int u24() throws Tls13Exception {
        need(3);
        int v = ((buf[pos] & 0xff) << 16) | ((buf[pos + 1] & 0xff) << 8) | (buf[pos + 2] & 0xff);
        pos += 3;
        return v;
    }

    /**
     * Reads a fixed number of bytes.
     * <p>
     * 정해진 수의 바이트를 읽는다.
     *
     * @param n the number of bytes / 바이트 수
     * @return the bytes / 바이트
     * @throws Tls13Exception decode_error if fewer bytes are left / 남은 바이트가 적으면 decode_error
     */
    public byte[] bytes(int n) throws Tls13Exception {
        need(n);
        byte[] out = new byte[n];
        System.arraycopy(buf, pos, out, 0, n);
        pos += n;
        return out;
    }

    /**
     * Reads a vector with a one byte length.
     * <p>
     * 1바이트 길이의 벡터를 읽는다.
     *
     * @return the content / 내용
     * @throws Tls13Exception decode_error if the vector is truncated / 벡터가 잘렸으면 decode_error
     */
    public byte[] vector8() throws Tls13Exception {
        return bytes(u8());
    }

    /**
     * Reads a vector with a two byte length.
     * <p>
     * 2바이트 길이의 벡터를 읽는다.
     *
     * @return the content / 내용
     * @throws Tls13Exception decode_error if the vector is truncated / 벡터가 잘렸으면 decode_error
     */
    public byte[] vector16() throws Tls13Exception {
        return bytes(u16());
    }

    /**
     * Reads a vector with a three byte length.
     * <p>
     * 3바이트 길이의 벡터를 읽는다.
     *
     * @return the content / 내용
     * @throws Tls13Exception decode_error if the vector is truncated / 벡터가 잘렸으면 decode_error
     */
    public byte[] vector24() throws Tls13Exception {
        return bytes(u24());
    }

    /**
     * Returns a reader over a vector with a two byte length.
     * <p>
     * 2바이트 길이의 벡터를 읽는 해석기를 반환한다.
     *
     * @return the nested reader / 안쪽 해석기
     * @throws Tls13Exception decode_error if the vector is truncated / 벡터가 잘렸으면 decode_error
     */
    public ByteReader sub16() throws Tls13Exception {
        int n = u16();
        need(n);
        ByteReader r = new ByteReader(buf, pos, pos + n);
        pos += n;
        return r;
    }

    /**
     * Returns a reader over a vector with a three byte length.
     * <p>
     * 3바이트 길이의 벡터를 읽는 해석기를 반환한다.
     *
     * @return the nested reader / 안쪽 해석기
     * @throws Tls13Exception decode_error if the vector is truncated / 벡터가 잘렸으면 decode_error
     */
    public ByteReader sub24() throws Tls13Exception {
        int n = u24();
        need(n);
        ByteReader r = new ByteReader(buf, pos, pos + n);
        pos += n;
        return r;
    }

    /**
     * Checks that all bytes were read.
     * <p>
     * 모든 바이트를 읽었는지 확인한다.
     *
     * @throws Tls13Exception decode_error if bytes are left / 바이트가 남았으면 decode_error
     */
    public void expectEnd() throws Tls13Exception {
        if (pos != end) {
            throw Tls13Exception.protocol(Tls13Exception.DECODE_ERROR, "message has trailing data");
        }
    }
}
