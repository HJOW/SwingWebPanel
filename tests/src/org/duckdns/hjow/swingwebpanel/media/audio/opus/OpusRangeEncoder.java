package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * Test-only range encoder, the counterpart of {@link OpusRangeDecoder} (RFC 6716 section 5.1). It is a translation of
 * <code>celt/entenc.c</code> and the encoder half of <code>celt/laplace.c</code> of the reference decoder source in RFC 6716
 * appendix A (BSD license, Xiph.Org Foundation and contributors; see docs/progress/media-opus.md). The tests use it to produce
 * entropy-coded data whose symbols are known, and to check that the decoder returns them and ends in the same state.
 * <p>
 * 시험 전용 범위 인코더로 {@link OpusRangeDecoder} 의 짝이다(RFC 6716 5.1). RFC 6716 부록 A 참조 디코더 소스의 <code>celt/entenc.c</code> 와
 * <code>celt/laplace.c</code> 의 인코더 부분을 옮긴 것이다(BSD 라이선스, Xiph.Org Foundation 등. docs/progress/media-opus.md 참조). 시험은
 * 기호를 아는 엔트로피 부호화 데이터를 만들고, 디코더가 그 기호를 돌려주며 같은 상태로 끝나는지 확인하는 데 쓴다.
 */
final class OpusRangeEncoder {

    /**
     * The output buffer.
     * <p>
     * 출력 버퍼.
     */
    private final byte[] buf;

    /**
     * Size of the buffer.
     * <p>
     * 버퍼 크기.
     */
    private final int storage;

    /**
     * Bytes written from the front.
     * <p>
     * 앞에서부터 쓴 바이트 수.
     */
    private int offs;

    /**
     * Bytes written from the end.
     * <p>
     * 뒤에서부터 쓴 바이트 수.
     */
    private int endOffs;

    /**
     * Window of the raw bits.
     * <p>
     * 원시 비트 창.
     */
    private long endWindow;

    /**
     * Number of valid bits in the raw-bit window.
     * <p>
     * 원시 비트 창의 유효 비트 수.
     */
    private int nendBits;

    /**
     * Range size (unsigned 32 bits).
     * <p>
     * 범위 크기(부호 없는 32비트).
     */
    private long rng;

    /**
     * Low end of the range (unsigned 32 bits).
     * <p>
     * 범위의 하한(부호 없는 32비트).
     */
    private long val;

    /**
     * Count of pending 0xFF bytes.
     * <p>
     * 보류 중인 0xFF 바이트 수.
     */
    private long ext;

    /**
     * The byte that may still receive a carry, or -1.
     * <p>
     * 올림이 더해질 수 있는 바이트. 없으면 -1.
     */
    private int rem;

    /**
     * True if the buffer overflowed.
     * <p>
     * 버퍼가 넘쳤으면 true.
     */
    private boolean error;

    /**
     * Creates an encoder with a buffer.
     * <p>
     * 버퍼를 가진 인코더를 만든다.
     *
     * @param size size of the buffer in bytes / 버퍼 바이트 수
     */
    OpusRangeEncoder(int size) {
        buf = new byte[size];
        storage = size;
        rng = 1L << 31;
        rem = -1;
    }

    /**
     * Writes a byte at the front.
     * <p>
     * 앞에 바이트를 쓴다.
     *
     * @param value the byte / 바이트
     */
    private void writeByte(int value) {
        if (offs + endOffs >= storage) {
            error = true;
            return;
        }
        buf[offs++] = (byte) value;
    }

    /**
     * Writes a byte at the end.
     * <p>
     * 뒤에 바이트를 쓴다.
     *
     * @param value the byte / 바이트
     */
    private void writeByteAtEnd(int value) {
        if (offs + endOffs >= storage) {
            error = true;
            return;
        }
        buf[storage - ++endOffs] = (byte) value;
    }

    /**
     * Outputs a byte with its carry.
     * <p>
     * 올림과 함께 바이트를 내보낸다.
     *
     * @param c the byte plus carry (9 bits) / 바이트와 올림(9비트)
     */
    private void carryOut(int c) {
        if (c != 255) {
            int carry = c >> 8;
            if (rem >= 0) {
                writeByte(rem + carry);
            }
            if (ext > 0) {
                int sym = (255 + carry) & 255;
                // 보류된 0xFF 바이트를 모두 쓴다.
                do {
                    writeByte(sym);
                } while (--ext > 0);
            }
            rem = c & 255;
        } else {
            ext++;
        }
    }

    /**
     * Renormalizes the range.
     * <p>
     * 범위를 재정규화한다.
     */
    private void normalize() {
        // 범위가 너무 작아진 동안 바이트를 내보내고 범위를 넓힌다.
        while (rng <= (1L << 23)) {
            carryOut((int) (val >> 23));
            val = (val << 8) & ((1L << 31) - 1);
            rng = (rng << 8) & 0xFFFFFFFFL;
        }
    }

    /**
     * Encodes a symbol given by its frequency range.
     * <p>
     * 빈도 구간으로 지정한 기호를 부호화한다.
     *
     * @param fl low end / 하한
     * @param fh high end / 상한
     * @param ft total / 합계
     */
    void encode(long fl, long fh, long ft) {
        long r = rng / ft;
        if (fl > 0) {
            val = (val + rng - r * (ft - fl)) & 0xFFFFFFFFL;
            rng = r * (fh - fl);
        } else {
            rng = rng - r * (ft - fh);
        }
        normalize();
    }

    /**
     * Same as {@link #encode(long, long, long)} with a total of 2^bits.
     * <p>
     * 합계가 2^bits 인 {@link #encode(long, long, long)}.
     *
     * @param fl low end / 하한
     * @param fh high end / 상한
     * @param bits log2 of the total / 합계의 log2
     */
    void encodeBin(long fl, long fh, int bits) {
        long r = rng >> bits;
        if (fl > 0) {
            val = (val + rng - r * ((1L << bits) - fl)) & 0xFFFFFFFFL;
            rng = r * (fh - fl);
        } else {
            rng = rng - r * ((1L << bits) - fh);
        }
        normalize();
    }

    /**
     * Encodes a bit with probability 2^-logp of being 1.
     * <p>
     * 1 일 확률이 2^-logp 인 비트를 부호화한다.
     *
     * @param value the bit / 비트
     * @param logp  log2 of the inverse probability of a 1 / 1 의 확률의 역수의 log2
     */
    void bitLogp(int value, int logp) {
        long r = rng;
        long l = val;
        long s = r >> logp;
        r -= s;
        if (value != 0) {
            val = (l + r) & 0xFFFFFFFFL;
        }
        rng = value != 0 ? s : r;
        normalize();
    }

    /**
     * Encodes a symbol with an inverse cumulative distribution table.
     * <p>
     * 역 누적 분포 표로 기호를 부호화한다.
     *
     * @param s    the symbol / 기호
     * @param icdf the table / 표
     * @param ftb  log2 of the total / 합계의 log2
     */
    void icdf(int s, int[] icdf, int ftb) {
        long r = rng >> ftb;
        if (s > 0) {
            val = (val + rng - r * icdf[s - 1]) & 0xFFFFFFFFL;
            rng = r * (icdf[s - 1] - icdf[s]);
        } else {
            rng = rng - r * icdf[s];
        }
        normalize();
    }

    /**
     * Encodes a uniformly distributed integer in [0, ft).
     * <p>
     * [0, ft) 의 균등 분포 정수를 부호화한다.
     *
     * @param fl the value / 값
     * @param ftIn the number of values / 값의 개수
     */
    void uint(long fl, long ftIn) {
        long ft = ftIn - 1;
        int ftb = OpusRangeDecoder.ilog(ft);
        if (ftb > 8) {
            ftb -= 8;
            long f = (ft >> ftb) + 1;
            long l = fl >> ftb;
            encode(l, l + 1, f);
            bits(fl & ((1L << ftb) - 1), ftb);
        } else {
            encode(fl, fl + 1, ft + 1);
        }
    }

    /**
     * Writes raw bits at the end of the data.
     * <p>
     * 데이터의 끝에 원시 비트를 쓴다.
     *
     * @param fl   the bits / 비트
     * @param nbits number of bits, 1..25 / 비트 수(1..25)
     */
    void bits(long fl, int nbits) {
        long window = endWindow;
        int used = nendBits;
        if (used + nbits > 32) {
            // 창이 넘치기 전에 가득 찬 바이트를 뒤에 쓴다.
            do {
                writeByteAtEnd((int) (window & 255));
                window >>= 8;
                used -= 8;
            } while (used >= 8);
        }
        window |= fl << used;
        used += nbits;
        endWindow = window & 0xFFFFFFFFL;
        nendBits = used;
    }

    /**
     * Encodes a Laplace distributed integer, the counterpart of {@link OpusRangeDecoder#laplace(int, int)}.
     * <p>
     * 라플라스 분포 정수를 부호화한다. {@link OpusRangeDecoder#laplace(int, int)} 의 짝이다.
     *
     * @param value the value / 값
     * @param fsIn  probability of zero in units of 2^-15 / 0 의 확률(2^-15 단위)
     * @param decay decay rate in units of 2^-14 / 감쇠율(2^-14 단위)
     * @return the value actually coded (large values are clipped) / 실제로 부호화한 값(큰 값은 잘린다)
     */
    int laplace(int value, int fsIn, int decay) {
        long fs = fsIn;
        long fl = 0;
        int val0 = value;
        if (val0 != 0) {
            int s = val0 < 0 ? -1 : 0;
            int v = (val0 + s) ^ s;
            fl = fs;
            long ft = 32768 - 32 - fs;
            fs = (ft * (16384 - decay)) >> 15;
            int i;
            // 값의 크기만큼 확률을 줄여 가며 하한을 쌓는다.
            for (i = 1; fs > 0 && i < v; i++) {
                fs *= 2;
                fl += fs + 2;
                fs = (fs * decay) >> 15;
            }
            if (fs == 0) {
                long ndiMax = (32768 - fl + 1 - 1);
                ndiMax = (ndiMax - s) >> 1;
                long di = Math.min(v - i, ndiMax - 1);
                fl += (2 * di + 1 + s);
                fs = Math.min(1, 32768 - fl);
                val0 = (int) ((i + di + s) ^ s);
            } else {
                fs += 1;
                fl += s != 0 ? 0 : fs;
            }
        }
        encodeBin(fl, fl + fs, 15);
        return val0;
    }

    /**
     * Finishes the stream and returns the data.
     * <p>
     * 스트림을 끝내고 데이터를 반환한다.
     *
     * @return the buffer of the declared size; bytes after the front part and before the end part are zero / 선언한 크기의 버퍼
     */
    byte[] done() {
        int l = 32 - OpusRangeDecoder.ilog(rng);
        long msk = ((1L << 31) - 1) >> l;
        long end = (val + msk) & ~msk & 0xFFFFFFFFL;
        if ((end | msk) >= val + rng) {
            l++;
            msk >>= 1;
            end = (val + msk) & ~msk & 0xFFFFFFFFL;
        }
        // 남은 상위 비트를 모두 내보낸다.
        while (l > 0) {
            carryOut((int) (end >> 23));
            end = (end << 8) & ((1L << 31) - 1);
            l -= 8;
        }
        if (rem >= 0 || ext > 0) {
            carryOut(0);
        }
        long window = endWindow;
        int used = nendBits;
        // 원시 비트의 가득 찬 바이트를 뒤에 쓴다.
        while (used >= 8) {
            writeByteAtEnd((int) (window & 255));
            window >>= 8;
            used -= 8;
        }
        if (!error && used > 0) {
            if (endOffs >= storage) {
                error = true;
            } else {
                buf[storage - endOffs - 1] |= (byte) window;
            }
        }
        if (error) {
            throw new IllegalStateException("range encoder overflow");
        }
        return buf;
    }

    /**
     * Returns the current range, which equals the final range of the decoder after the same symbols.
     * <p>
     * 현재 범위를 반환한다. 같은 기호를 디코딩한 디코더의 최종 범위와 같다.
     *
     * @return the 32-bit range / 32비트 범위
     */
    int getRange() {
        return (int) rng;
    }
}
