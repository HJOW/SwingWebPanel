package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * The bit reader of the WebP lossless bitstream (VP8L; plan item P18-03). Bits are read starting at the least significant bit of each byte and
 * a value of several bits has its first bit in the lowest position. Reading beyond the end of the data is an error (reported as
 * {@link LosslessException}), while {@link #peek} may look past the end and then sees zero bits, which lets the Huffman decoder ask for more
 * bits than the last code needs.
 * <p>
 * WebP 무손실 비트스트림(VP8L)의 비트 리더(계획 P18-03). 비트는 각 바이트의 최하위 비트부터 읽으며 여러 비트 값은 첫 비트가 가장 낮은 자리에 온다.
 * 데이터 끝을 넘어 읽는 것은 오류({@link LosslessException})이지만 {@link #peek} 는 끝 너머를 0 비트로 보므로 허프만 디코더가 마지막 부호에 필요한
 * 것보다 많은 비트를 요청해도 된다.
 */
final class Vp8lBits {

    /**
     * The data that contains the bitstream.
     * <p>
     * 비트스트림이 들어 있는 데이터.
     */
    private final byte[] data;

    /**
     * Position of the next byte that is not in the buffer yet.
     * <p>
     * 아직 버퍼에 들어오지 않은 다음 바이트의 위치.
     */
    private int pos;

    /**
     * End of the bitstream in the data (exclusive).
     * <p>
     * 데이터 안에서 비트스트림의 끝(포함하지 않음).
     */
    private final int end;

    /**
     * The bit buffer; the next bit to read is bit 0.
     * <p>
     * 비트 버퍼. 다음에 읽을 비트가 0번 비트이다.
     */
    private long buffer;

    /**
     * Number of valid bits in the buffer.
     * <p>
     * 버퍼에 있는 유효한 비트 수.
     */
    private int count;

    /**
     * Creates a reader over a range of the data.
     * <p>
     * 데이터의 한 범위를 읽는 리더를 만든다.
     *
     * @param data   data / 데이터
     * @param offset first byte of the bitstream / 비트스트림의 첫 바이트
     * @param length number of bytes / 바이트 수
     */
    Vp8lBits(byte[] data, int offset, int length) {
        this.data = data;
        this.pos = offset;
        this.end = offset + length;
    }

    /**
     * Fills the buffer with as many whole bytes as fit.
     * <p>
     * 버퍼에 들어가는 만큼 온전한 바이트를 채운다.
     */
    private void fill() {
        // 버퍼에 한 바이트가 더 들어갈 수 있는 동안 데이터에서 가져온다.
        while (count <= 56 && pos < end) {
            buffer |= (long) (data[pos++] & 0xff) << count;
            count += 8;
        }
    }

    /**
     * Returns the next bits without consuming them; missing bits past the end of the data read as 0.
     * <p>
     * 다음 비트들을 소비하지 않고 반환한다. 데이터 끝 너머의 없는 비트는 0 으로 읽힌다.
     *
     * @param n number of bits, 0..32 / 비트 수(0..32)
     * @return the bits / 비트들
     */
    int peek(int n) {
        if (count < n) {
            fill();
        }
        return (int) (buffer & ((1L << n) - 1));
    }

    /**
     * Consumes bits that were looked at with {@link #peek}.
     * <p>
     * {@link #peek} 로 본 비트들을 소비한다.
     *
     * @param n number of bits / 비트 수
     */
    void skip(int n) {
        if (n > count) {
            fill();
            if (n > count) {
                throw new LosslessException("the lossless bitstream is truncated");
            }
        }
        buffer >>>= n;
        count -= n;
    }

    /**
     * Reads a value of the given number of bits.
     * <p>
     * 주어진 비트 수의 값을 읽는다.
     *
     * @param n number of bits, 0..32 / 비트 수(0..32)
     * @return the value / 값
     */
    int read(int n) {
        if (n == 0) {
            return 0;
        }
        int v = peek(n);
        skip(n);
        return v;
    }

    /**
     * The error of a damaged or truncated lossless bitstream; the decoder turns it into a CORRUPT media failure at its entry point.
     * <p>
     * 손상되었거나 잘린 무손실 비트스트림의 오류. 디코더가 진입점에서 이를 CORRUPT 미디어 실패로 바꾼다.
     */
    static final class LosslessException extends RuntimeException {

        /**
         * Serialization version id.
         * <p>
         * 직렬화 버전 식별자.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Creates the exception.
         * <p>
         * 예외를 만든다.
         *
         * @param message description / 설명
         */
        LosslessException(String message) {
            super(message);
        }
    }
}
