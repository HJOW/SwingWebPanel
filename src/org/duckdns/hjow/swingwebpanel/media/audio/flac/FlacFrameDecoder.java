package org.duckdns.hjow.swingwebpanel.media.audio.flac;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Decoder of single FLAC frames (plan item P18-07, FEATURE_MATRIX AUD-07), shared by the native FLAC and the Ogg FLAC readers: the
 * frame header with its CRC-8, the subframes (constant, verbatim, fixed and linear predictors, wasted bits), the Rice-coded residual,
 * the inter-channel decorrelation (left/side, side/right, mid/side) and the frame CRC-16. Samples are decoded to 64-bit integers so
 * that 32-bit audio and its 33-bit side channel are handled without overflow. A frame that is cut off by the end of the data raises
 * {@link Truncated}; a frame that is complete but invalid raises a CORRUPT {@link MediaException}.
 * <p>
 * 단일 FLAC 프레임의 디코더(계획 P18-07, FEATURE_MATRIX AUD-07). 네이티브 FLAC 과 Ogg FLAC 읽기가 공유한다: CRC-8 이 있는 프레임 헤더, 서브프레임
 * (상수, 그대로, 고정/선형 예측, 버려진 비트), Rice 부호화된 잔차, 채널 간 비상관(left/side, side/right, mid/side), 프레임 CRC-16. 32비트 오디오와
 * 33비트 side 채널이 넘치지 않도록 샘플은 64비트 정수로 디코딩한다. 데이터 끝에서 잘린 프레임은 {@link Truncated} 를 일으키고, 완전하지만
 * 올바르지 않은 프레임은 CORRUPT {@link MediaException} 을 일으킨다.
 */
final class FlacFrameDecoder {

    /**
     * Raised when the data ends inside a frame.
     * <p>
     * 프레임 도중에 데이터가 끝났을 때 일으킨다.
     */
    static final class Truncated extends RuntimeException {
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
         */
        Truncated() {
            super("FLAC frame is cut off");
        }
    }

    /**
     * The parsed header of a frame.
     * <p>
     * 해석한 프레임 헤더.
     */
    static final class Header {
        /**
         * Samples per channel in the frame.
         * <p>
         * 프레임의 채널당 샘플 수.
         */
        int blockSize;

        /**
         * Sample rate in Hz.
         * <p>
         * 샘플링 주파수(Hz).
         */
        int sampleRate;

        /**
         * Number of channels.
         * <p>
         * 채널 수.
         */
        int channels;

        /**
         * Bits per sample.
         * <p>
         * 샘플당 비트 수.
         */
        int bps;

        /**
         * Channel assignment code, 0..10.
         * <p>
         * 채널 배정 코드(0~10).
         */
        int assignment;

        /**
         * True if the frame header carries a sample number (variable block size) instead of a frame number.
         * <p>
         * 프레임 번호 대신 샘플 번호를 담고 있으면(가변 블록 크기) true.
         */
        boolean variable;

        /**
         * Frame number or sample number from the header.
         * <p>
         * 헤더의 프레임 번호 또는 샘플 번호.
         */
        long number;

        /**
         * Length of the header in bytes including its CRC-8.
         * <p>
         * CRC-8 을 포함한 헤더의 바이트 길이.
         */
        int headerLength;
    }

    /**
     * CRC-8 table (polynomial 0x07).
     * <p>
     * CRC-8 표(다항식 0x07).
     */
    private static final int[] CRC8 = new int[256];

    /**
     * CRC-16 table (polynomial 0x8005).
     * <p>
     * CRC-16 표(다항식 0x8005).
     */
    private static final int[] CRC16 = new int[256];

    static {
        // 바이트 값마다 8번 시프트하며 두 표를 만든다.
        for (int i = 0; i < 256; i++) {
            int c8 = i;
            int c16 = i << 8;
            // 비트마다 다항식을 적용한다.
            for (int k = 0; k < 8; k++) {
                c8 = (c8 & 0x80) != 0 ? ((c8 << 1) ^ 0x07) & 0xFF : (c8 << 1) & 0xFF;
                c16 = (c16 & 0x8000) != 0 ? ((c16 << 1) ^ 0x8005) & 0xFFFF : (c16 << 1) & 0xFFFF;
            }
            CRC8[i] = c8;
            CRC16[i] = c16;
        }
    }

    /**
     * Sample rates of the header codes 1 to 11.
     * <p>
     * 헤더 코드 1~11 의 샘플링 주파수.
     */
    private static final int[] RATES = { 0, 88200, 176400, 192000, 8000, 16000, 22050, 24000, 32000, 44100, 48000, 96000 };

    /**
     * The stream parameters.
     * <p>
     * 스트림 파라미터.
     */
    private final FlacStreamInfo info;

    /**
     * Decoded samples of the last frame, one array per channel.
     * <p>
     * 마지막 프레임의 디코딩된 샘플. 채널마다 배열 하나.
     */
    final long[][] samples = new long[8][];

    /**
     * Creates the decoder.
     * <p>
     * 디코더를 만든다.
     *
     * @param info stream parameters / 스트림 파라미터
     */
    FlacFrameDecoder(FlacStreamInfo info) {
        this.info = info;
    }

    /**
     * Computes the CRC-8 of a byte range.
     * <p>
     * 바이트 범위의 CRC-8 을 계산한다.
     *
     * @param d   data / 데이터
     * @param off first byte / 첫 바이트
     * @param end end (exclusive) / 끝(미포함)
     * @return the checksum / 체크섬
     */
    static int crc8(byte[] d, int off, int end) {
        int c = 0;
        // 바이트마다 표로 갱신한다.
        for (int i = off; i < end; i++) {
            c = CRC8[(c ^ d[i]) & 0xFF];
        }
        return c;
    }

    /**
     * Computes the CRC-16 of a byte range.
     * <p>
     * 바이트 범위의 CRC-16 을 계산한다.
     *
     * @param d   data / 데이터
     * @param off first byte / 첫 바이트
     * @param end end (exclusive) / 끝(미포함)
     * @return the checksum / 체크섬
     */
    static int crc16(byte[] d, int off, int end) {
        int c = 0;
        // 바이트마다 표로 갱신한다.
        for (int i = off; i < end; i++) {
            c = ((c << 8) & 0xFFFF) ^ CRC16[((c >> 8) ^ d[i]) & 0xFF];
        }
        return c;
    }

    /**
     * Tells whether two bytes carry the frame sync code.
     * <p>
     * 두 바이트가 프레임 동기 코드인지 알려 준다.
     *
     * @param b0 first byte / 첫 바이트
     * @param b1 second byte / 둘째 바이트
     * @return true for 0xFFF8 or 0xFFF9 / 0xFFF8 또는 0xFFF9 이면 true
     */
    static boolean isSync(int b0, int b1) {
        return (b0 & 0xFF) == 0xFF && (b1 & 0xFE) == 0xF8;
    }

    /**
     * Parses the frame header at an offset.
     * <p>
     * 위치의 프레임 헤더를 해석한다.
     *
     * @param d   data / 데이터
     * @param off offset of the frame / 프레임의 위치
     * @param end end of the data (exclusive) / 데이터의 끝(미포함)
     * @return the header, or null if there is no valid header (sync code, reserved values, CRC-8) / 헤더. 올바른 헤더가 없으면(동기 코드, 예약 값, CRC-8) null
     * @throws Truncated if the data ends inside the header / 헤더 도중에 데이터가 끝나면
     */
    Header parseHeader(byte[] d, int off, int end) {
        if (end - off < 2 || !isSync(d[off], d[off + 1])) {
            return null;
        }
        if (end - off < 6) {
            throw new Truncated();
        }
        Header h = new Header();
        h.variable = (d[off + 1] & 1) != 0;
        int bsCode = (d[off + 2] & 0xFF) >> 4;
        int rateCode = d[off + 2] & 0xF;
        int chCode = (d[off + 3] & 0xFF) >> 4;
        int sizeCode = (d[off + 3] >> 1) & 7;
        if ((d[off + 3] & 1) != 0 || bsCode == 0 || rateCode == 15 || chCode > 10 || sizeCode == 3) {
            return null;
        }
        int pos = off + 4;
        int first = d[pos] & 0xFF;
        int extra = first < 0x80 ? 0 : first < 0xC0 ? -1 : first < 0xE0 ? 1 : first < 0xF0 ? 2 : first < 0xF8 ? 3 : first < 0xFC ? 4 : first < 0xFE ? 5 : first == 0xFE ? 6 : -1;
        if (extra < 0) {
            return null;
        }
        if (end - pos < 1 + extra) {
            throw new Truncated();
        }
        long number = extra == 0 ? first : first & (0x3F >> extra);
        // 이어지는 바이트마다 6비트씩 붙인다(10xxxxxx 형태여야 한다).
        for (int i = 1; i <= extra; i++) {
            int b = d[pos + i] & 0xFF;
            if ((b & 0xC0) != 0x80) {
                return null;
            }
            number = (number << 6) | (b & 0x3F);
        }
        h.number = number;
        pos += 1 + extra;
        if (bsCode == 1) {
            h.blockSize = 192;
        } else if (bsCode <= 5) {
            h.blockSize = 576 << (bsCode - 2);
        } else if (bsCode >= 8) {
            h.blockSize = 256 << (bsCode - 8);
        } else {
            int n = bsCode == 6 ? 1 : 2;
            if (end - pos < n + 1) {
                throw new Truncated();
            }
            h.blockSize = (n == 1 ? d[pos] & 0xFF : ((d[pos] & 0xFF) << 8) | (d[pos + 1] & 0xFF)) + 1;
            pos += n;
        }
        if (rateCode == 0) {
            h.sampleRate = info.sampleRate;
        } else if (rateCode <= 11) {
            h.sampleRate = RATES[rateCode];
        } else {
            int n = rateCode == 12 ? 1 : 2;
            if (end - pos < n + 1) {
                throw new Truncated();
            }
            int v = n == 1 ? d[pos] & 0xFF : ((d[pos] & 0xFF) << 8) | (d[pos + 1] & 0xFF);
            h.sampleRate = rateCode == 14 ? v * 10 : rateCode == 12 ? v * 1000 : v;
            pos += n;
        }
        if (end - pos < 1) {
            throw new Truncated();
        }
        if ((d[pos] & 0xFF) != crc8(d, off, pos)) {
            return null;
        }
        h.bps = sizeCode == 0 ? info.bitsPerSample : sizeCode == 1 ? 8 : sizeCode == 2 ? 12 : sizeCode == 4 ? 16 : sizeCode == 5 ? 20 : sizeCode == 6 ? 24 : 32;
        h.assignment = chCode;
        h.channels = chCode < 8 ? chCode + 1 : 2;
        h.headerLength = pos + 1 - off;
        return h;
    }

    /**
     * Decodes the frame whose valid header was parsed, including its CRC-16.
     * <p>
     * 올바른 헤더를 해석한 프레임을 CRC-16 까지 포함해 디코딩한다.
     *
     * @param d   data / 데이터
     * @param off offset of the frame / 프레임의 위치
     * @param end end of the data (exclusive) / 데이터의 끝(미포함)
     * @param h   the header from {@link #parseHeader(byte[], int, int)} / {@link #parseHeader(byte[], int, int)} 의 헤더
     * @return the length of the frame in bytes / 프레임의 바이트 길이
     * @throws MediaException CORRUPT if the frame is invalid or its CRC-16 does not match / 프레임이 올바르지 않거나 CRC-16 이 맞지 않으면 CORRUPT
     * @throws Truncated      if the data ends inside the frame / 프레임 도중에 데이터가 끝나면
     */
    int decodeFrame(byte[] d, int off, int end, Header h) throws MediaException {
        if (h.blockSize > 65535 || h.channels != info.channels) {
            throw new MediaException(MediaException.Kind.CORRUPT, "FLAC frame does not match the stream parameters");
        }
        FlacBitReader r = new FlacBitReader(d, off + h.headerLength, end);
        // 채널마다 서브프레임을 디코딩한다(side 채널은 비트가 하나 더 많다).
        for (int c = 0; c < h.channels; c++) {
            if (samples[c] == null || samples[c].length < h.blockSize) {
                samples[c] = new long[Math.max(h.blockSize, 4096)];
            }
            boolean side = (h.assignment == 8 && c == 1) || (h.assignment == 9 && c == 0) || (h.assignment == 10 && c == 1);
            subframe(r, samples[c], h.blockSize, h.bps + (side ? 1 : 0));
            if (Thread.currentThread().isInterrupted()) {
                throw new MediaException(MediaException.Kind.CANCELLED, "audio decoding was interrupted");
            }
        }
        r.align();
        int crcPos = r.bytePosition();
        if (end - crcPos < 2) {
            throw new Truncated();
        }
        int stored = ((d[crcPos] & 0xFF) << 8) | (d[crcPos + 1] & 0xFF);
        if (stored != crc16(d, off, crcPos)) {
            throw new MediaException(MediaException.Kind.CORRUPT, "FLAC frame CRC-16 mismatch");
        }
        decorrelate(h);
        return crcPos + 2 - off;
    }

    /**
     * Undoes the stereo decorrelation of a frame.
     * <p>
     * 프레임의 스테레오 비상관을 되돌린다.
     *
     * @param h the header / 헤더
     */
    private void decorrelate(Header h) {
        long[] a = samples[0];
        long[] b = samples[1];
        // 샘플마다 채널 배정에 따라 left/right 로 복원한다.
        if (h.assignment == 8) {
            for (int i = 0; i < h.blockSize; i++) {
                b[i] = a[i] - b[i];
            }
        } else if (h.assignment == 9) {
            // side/right 에서 left 를 복원한다.
            for (int i = 0; i < h.blockSize; i++) {
                a[i] = a[i] + b[i];
            }
        } else if (h.assignment == 10) {
            // mid/side 를 left/right 로 복원한다.
            for (int i = 0; i < h.blockSize; i++) {
                long mid = (a[i] << 1) | (b[i] & 1);
                long side = b[i];
                a[i] = (mid + side) >> 1;
                b[i] = (mid - side) >> 1;
            }
        }
    }

    /**
     * Decodes one subframe.
     * <p>
     * 서브프레임 하나를 디코딩한다.
     *
     * @param r    bit reader / 비트 읽기 객체
     * @param out  output samples / 출력 샘플
     * @param n    block size / 블록 크기
     * @param bits bits per sample of this subframe / 이 서브프레임의 샘플당 비트 수
     * @throws MediaException CORRUPT for reserved or impossible values / 예약된 값이나 불가능한 값이면 CORRUPT
     */
    private void subframe(FlacBitReader r, long[] out, int n, int bits) throws MediaException {
        if (r.read(1) != 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC subframe header");
        }
        int type = (int) r.read(6);
        int wasted = 0;
        if (r.read(1) != 0) {
            long z = r.readUnary();
            if (z > 31) {
                throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC wasted bits");
            }
            wasted = (int) z + 1;
        }
        int b = bits - wasted;
        if (b < 1 || b > 33) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC sample size");
        }
        if (type == 0) {
            long v = r.readSigned(b);
            // 모든 샘플이 같은 값이다.
            for (int i = 0; i < n; i++) {
                out[i] = v;
            }
        } else if (type == 1) {
            // 샘플을 그대로 저장한 서브프레임이다.
            for (int i = 0; i < n; i++) {
                out[i] = r.readSigned(b);
            }
        } else if ((type & 0x38) == 0x08 && (type & 7) <= 4) {
            fixed(r, out, n, b, type & 7);
        } else if ((type & 0x20) != 0) {
            lpc(r, out, n, b, (type & 0x1F) + 1);
        } else {
            throw new MediaException(MediaException.Kind.CORRUPT, "reserved FLAC subframe type");
        }
        if (wasted > 0) {
            // 버려진 하위 비트를 되돌린다.
            for (int i = 0; i < n; i++) {
                out[i] <<= wasted;
            }
        }
    }

    /**
     * Decodes a subframe with a fixed polynomial predictor.
     * <p>
     * 고정 다항식 예측기를 쓰는 서브프레임을 디코딩한다.
     *
     * @param r     bit reader / 비트 읽기 객체
     * @param out   output / 출력
     * @param n     block size / 블록 크기
     * @param bits  sample size / 샘플 크기
     * @param order predictor order 0..4 / 예측 차수 0~4
     * @throws MediaException CORRUPT on invalid values / 올바르지 않은 값이면 CORRUPT
     */
    private void fixed(FlacBitReader r, long[] out, int n, int bits, int order) throws MediaException {
        // 예열 샘플을 읽는다.
        for (int i = 0; i < order; i++) {
            out[i] = r.readSigned(bits);
        }
        residual(r, out, n, order);
        // 차수에 따른 고정 예측을 더해 샘플을 복원한다.
        for (int i = order; i < n; i++) {
            switch (order) {
            case 1:
                out[i] += out[i - 1];
                break;
            case 2:
                out[i] += 2 * out[i - 1] - out[i - 2];
                break;
            case 3:
                out[i] += 3 * out[i - 1] - 3 * out[i - 2] + out[i - 3];
                break;
            case 4:
                out[i] += 4 * out[i - 1] - 6 * out[i - 2] + 4 * out[i - 3] - out[i - 4];
                break;
            default:
                break;
            }
        }
    }

    /**
     * Decodes a subframe with a linear predictor.
     * <p>
     * 선형 예측기를 쓰는 서브프레임을 디코딩한다.
     *
     * @param r     bit reader / 비트 읽기 객체
     * @param out   output / 출력
     * @param n     block size / 블록 크기
     * @param bits  sample size / 샘플 크기
     * @param order predictor order 1..32 / 예측 차수 1~32
     * @throws MediaException CORRUPT on invalid values / 올바르지 않은 값이면 CORRUPT
     */
    private void lpc(FlacBitReader r, long[] out, int n, int bits, int order) throws MediaException {
        // 예열 샘플을 읽는다.
        for (int i = 0; i < order; i++) {
            out[i] = r.readSigned(bits);
        }
        int precisionCode = (int) r.read(4);
        if (precisionCode == 15) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC LPC precision");
        }
        int precision = precisionCode + 1;
        int shift = (int) r.readSigned(5);
        if (shift < 0) {
            throw new MediaException(MediaException.Kind.CORRUPT, "negative FLAC LPC shift");
        }
        long[] coef = new long[order];
        // 계수를 읽는다.
        for (int i = 0; i < order; i++) {
            coef[i] = r.readSigned(precision);
        }
        residual(r, out, n, order);
        // 이전 샘플과 계수의 곱의 합을 더해 샘플을 복원한다.
        for (int i = order; i < n; i++) {
            long sum = 0;
            // 계수와 이전 샘플을 곱해 더한다.
            for (int j = 0; j < order; j++) {
                sum += coef[j] * out[i - 1 - j];
            }
            out[i] += sum >> shift;
        }
    }

    /**
     * Decodes the Rice coded residual into <code>out[order..n)</code>.
     * <p>
     * Rice 부호화된 잔차를 <code>out[order..n)</code> 에 디코딩한다.
     *
     * @param r     bit reader / 비트 읽기 객체
     * @param out   output / 출력
     * @param n     block size / 블록 크기
     * @param order predictor order / 예측 차수
     * @throws MediaException CORRUPT on invalid values / 올바르지 않은 값이면 CORRUPT
     */
    private void residual(FlacBitReader r, long[] out, int n, int order) throws MediaException {
        int method = (int) r.read(2);
        if (method > 1) {
            throw new MediaException(MediaException.Kind.CORRUPT, "reserved FLAC residual coding method");
        }
        int partitionOrder = (int) r.read(4);
        int partitions = 1 << partitionOrder;
        if (n % partitions != 0 || (n >> partitionOrder) < order) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid FLAC residual partition order");
        }
        int paramBits = method == 0 ? 4 : 5;
        int escape = (1 << paramBits) - 1;
        int pos = order;
        // 파티션마다 Rice 매개변수를 읽고 그 파티션의 잔차를 디코딩한다.
        for (int p = 0; p < partitions; p++) {
            int count = (n >> partitionOrder) - (p == 0 ? order : 0);
            int param = (int) r.read(paramBits);
            if (param == escape) {
                int bits = (int) r.read(5);
                // 이스케이프 파티션의 잔차를 고정 비트 수로 읽는다.
                for (int i = 0; i < count; i++) {
                    out[pos++] = r.readSigned(bits);
                }
            } else {
                // 나머지 파티션의 잔차를 Rice 부호로 읽는다.
                for (int i = 0; i < count; i++) {
                    long q = r.readUnary();
                    long v = (q << param) | r.read(param);
                    out[pos++] = (v >>> 1) ^ -(v & 1);
                }
            }
        }
    }
}
