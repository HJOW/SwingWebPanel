package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The range decoder of RFC 6716 section 4.1 (entropy decoder shared by SILK and CELT). It reads symbols from the front of a packet
 * with an arithmetic decoder and raw bits from the back, exactly as the reference decoder does: the state after the last symbol
 * ({@link #getFinalRange()}) is a checksum of the whole decoding that the encoder stores next to every packet in the test vectors.
 * Reading beyond the end of the data yields zero bytes (never an exception). Not thread safe.
 * <p>
 * RFC 6716 4.1 의 범위 디코더(SILK 와 CELT 가 함께 쓰는 엔트로피 디코더). 산술 디코더로 패킷의 앞에서 기호를 읽고 뒤에서 원시 비트를 읽는다.
 * 참조 디코더와 정확히 같게 동작하며, 마지막 기호 뒤의 상태({@link #getFinalRange()})는 전체 디코딩의 체크섬이어서 시험 벡터에서 인코더가 패킷마다
 * 함께 저장한다. 데이터의 끝을 넘어 읽으면 0 바이트를 돌려주며 예외를 던지지 않는다. 스레드에 안전하지 않다.
 */
final class OpusRangeDecoder {

    /**
     * Number of bits of a symbol read at once (a byte).
     * <p>
     * 한 번에 읽는 기호의 비트 수(바이트).
     */
    private static final int SYM_BITS = 8;

    /**
     * Number of bits of the arithmetic code registers.
     * <p>
     * 산술 부호 레지스터의 비트 수.
     */
    private static final int CODE_BITS = 32;

    /**
     * Largest value of a symbol.
     * <p>
     * 기호의 최댓값.
     */
    private static final int SYM_MAX = 255;

    /**
     * Value of one beyond the top of the code range (2^31).
     * <p>
     * 부호 범위 꼭대기의 한 칸 위 값(2^31).
     */
    private static final long CODE_TOP = 1L << (CODE_BITS - 1);

    /**
     * Lower bound below which the range is renormalized (2^23).
     * <p>
     * 이 값 이하가 되면 범위를 재정규화하는 하한(2^23).
     */
    private static final long CODE_BOT = CODE_TOP >> SYM_BITS;

    /**
     * Number of extra bits of the first byte.
     * <p>
     * 첫 바이트의 여분 비트 수.
     */
    private static final int CODE_EXTRA = (CODE_BITS - 2) % SYM_BITS + 1;

    /**
     * Largest number of bits that {@link #readUint(long)} reads with the arithmetic decoder before it switches to raw bits.
     * <p>
     * {@link #readUint(long)} 가 산술 디코더로 읽다가 원시 비트로 넘어가는 최대 비트 수.
     */
    private static final int UINT_BITS = 8;

    /**
     * The buffer (the packet payload).
     * <p>
     * 버퍼(패킷 페이로드).
     */
    private final byte[] buf;

    /**
     * Start of the data in the buffer.
     * <p>
     * 버퍼에서 데이터가 시작하는 위치.
     */
    private final int base;

    /**
     * Number of valid bytes.
     * <p>
     * 유효한 바이트 수.
     */
    private int storage;

    /**
     * Number of bytes consumed from the end by raw bit reads.
     * <p>
     * 원시 비트 읽기로 뒤에서부터 소비한 바이트 수.
     */
    private int endOffs;

    /**
     * Window of the raw bits (32 bits, unsigned).
     * <p>
     * 원시 비트 창(32비트, 부호 없음).
     */
    private long endWindow;

    /**
     * Number of valid bits in the raw-bit window.
     * <p>
     * 원시 비트 창의 유효 비트 수.
     */
    private int nendBits;

    /**
     * Total number of bits consumed (as counted by {@link #tell()}).
     * <p>
     * 소비한 전체 비트 수({@link #tell()} 이 세는 값).
     */
    private int nbitsTotal;

    /**
     * Offset of the next byte read from the front.
     * <p>
     * 앞에서 읽을 다음 바이트의 위치.
     */
    private int offs;

    /**
     * Size of the current range (unsigned 32 bits).
     * <p>
     * 현재 범위의 크기(부호 없는 32비트).
     */
    private long rng;

    /**
     * Difference between the top of the range and the code value (unsigned 32 bits).
     * <p>
     * 범위의 꼭대기와 부호 값의 차(부호 없는 32비트).
     */
    private long val;

    /**
     * Saved divisor of the last {@link #decode(long)} (unsigned 32 bits).
     * <p>
     * 마지막 {@link #decode(long)} 가 저장한 나눗셈 몫(부호 없는 32비트).
     */
    private long ext;

    /**
     * The byte read ahead.
     * <p>
     * 미리 읽은 바이트.
     */
    private int rem;

    /**
     * Set when the data is inconsistent (an out-of-range uint).
     * <p>
     * 데이터가 모순될 때(범위를 벗어난 uint) 설정된다.
     */
    private boolean error;

    /**
     * Creates a decoder over a byte range.
     * <p>
     * 바이트 범위를 읽는 디코더를 만든다.
     *
     * @param data   the buffer / 버퍼
     * @param offset start of the data / 데이터 시작 위치
     * @param length number of bytes / 바이트 수
     */
    OpusRangeDecoder(byte[] data, int offset, int length) {
        this.buf = data;
        this.base = offset;
        this.storage = length;
        this.endOffs = 0;
        this.endWindow = 0;
        this.nendBits = 0;
        this.nbitsTotal = CODE_BITS + 1 - ((CODE_BITS - CODE_EXTRA) / SYM_BITS) * SYM_BITS;
        this.offs = 0;
        this.rng = 1L << CODE_EXTRA;
        this.rem = readByte();
        this.val = rng - 1 - (rem >> (SYM_BITS - CODE_EXTRA));
        this.error = false;
        normalize();
    }

    /**
     * Reads the next byte from the front (zero past the end).
     * <p>
     * 앞에서 다음 바이트를 읽는다(끝을 넘으면 0).
     *
     * @return the byte value 0..255 / 바이트 값
     */
    private int readByte() {
        return offs < storage ? (buf[base + offs++] & 0xFF) : 0;
    }

    /**
     * Reads the next byte from the back (zero past the end).
     * <p>
     * 뒤에서 다음 바이트를 읽는다(끝을 넘으면 0).
     *
     * @return the byte value 0..255 / 바이트 값
     */
    private int readByteFromEnd() {
        if (endOffs < storage) {
            endOffs++;
            return buf[base + storage - endOffs] & 0xFF;
        }
        return 0;
    }

    /**
     * Renormalizes the range after a symbol was decoded.
     * <p>
     * 기호를 디코딩한 뒤 범위를 재정규화한다.
     */
    private void normalize() {
        // 범위가 너무 작아진 동안 한 바이트씩 읽어 범위를 넓힌다.
        while (rng <= CODE_BOT) {
            nbitsTotal += SYM_BITS;
            rng = (rng << SYM_BITS) & 0xFFFFFFFFL;
            int sym = rem;
            rem = readByte();
            sym = ((sym << SYM_BITS) | rem) >> (SYM_BITS - CODE_EXTRA);
            val = ((val << SYM_BITS) + (SYM_MAX & ~sym)) & (CODE_TOP - 1);
        }
    }

    /**
     * Decodes the cumulative frequency of the next symbol, for a total of <code>ft</code>; the caller must then call
     * {@link #update(long, long, long)}.
     * <p>
     * 합계가 <code>ft</code> 인 다음 기호의 누적 빈도를 디코딩한다. 호출자는 이어서 {@link #update(long, long, long)} 를 호출해야 한다.
     *
     * @param ft total frequency (at least 2, below 2^16) / 전체 빈도(2 이상 2^16 미만)
     * @return the cumulative frequency / 누적 빈도
     */
    long decode(long ft) {
        ext = rng / ft;
        long s = val / ext;
        return ft - Math.min(s + 1, ft);
    }

    /**
     * Same as {@link #decode(long)} with a total of 2^bits.
     * <p>
     * 전체 빈도가 2^bits 인 {@link #decode(long)}.
     *
     * @param bits log2 of the total frequency / 전체 빈도의 log2
     * @return the cumulative frequency / 누적 빈도
     */
    long decodeBin(int bits) {
        ext = rng >> bits;
        long s = val / ext;
        return (1L << bits) - Math.min(s + 1, 1L << bits);
    }

    /**
     * Consumes a symbol that was found with {@link #decode(long)} or {@link #decodeBin(int)}.
     * <p>
     * {@link #decode(long)} 나 {@link #decodeBin(int)} 로 찾은 기호를 소비한다.
     *
     * @param fl low end of the symbol's frequency range / 기호의 빈도 구간 하한
     * @param fh high end of the range / 구간 상한
     * @param ft total frequency / 전체 빈도
     */
    void update(long fl, long fh, long ft) {
        long s = (ext * (ft - fh)) & 0xFFFFFFFFL;
        val = (val - s) & 0xFFFFFFFFL;
        rng = fl > 0 ? (ext * (fh - fl)) & 0xFFFFFFFFL : (rng - s) & 0xFFFFFFFFL;
        normalize();
    }

    /**
     * Decodes one bit whose probability of being 1 is 2^-logp.
     * <p>
     * 1 일 확률이 2^-logp 인 비트 하나를 디코딩한다.
     *
     * @param logp log2 of the inverse probability of a 1 / 1 이 나올 확률의 역수의 log2
     * @return the bit, 0 or 1 / 비트(0 또는 1)
     */
    int bitLogp(int logp) {
        long r = rng;
        long d = val;
        long s = r >> logp;
        boolean ret = d < s;
        if (!ret) {
            val = (d - s) & 0xFFFFFFFFL;
        }
        rng = ret ? s : r - s;
        normalize();
        return ret ? 1 : 0;
    }

    /**
     * Decodes a symbol with an inverse cumulative distribution table in units of 2^-ftb.
     * <p>
     * 2^-ftb 단위의 역 누적 분포 표로 기호를 디코딩한다.
     *
     * @param icdf the table (values 0..255 decreasing to 0) / 표(255 이하의 값, 0 까지 감소)
     * @param ftb  log2 of the total / 합계의 log2
     * @return the index of the symbol / 기호의 번호
     */
    int icdf(int[] icdf, int ftb) {
        return icdf(icdf, 0, ftb);
    }

    /**
     * Same as {@link #icdf(int[], int)} with the table starting at an offset.
     * <p>
     * 표가 오프셋에서 시작하는 {@link #icdf(int[], int)}.
     *
     * @param icdf   the table / 표
     * @param offset start of the table / 표의 시작
     * @param ftb    log2 of the total / 합계의 log2
     * @return the index of the symbol / 기호의 번호
     */
    int icdf(int[] icdf, int offset, int ftb) {
        long s = rng;
        long d = val;
        long r = s >> ftb;
        long t;
        int ret = offset - 1;
        // 값이 표의 경계 아래로 내려갈 때까지 기호 번호를 늘린다.
        do {
            t = s;
            s = (r * icdf[++ret]) & 0xFFFFFFFFL;
        } while (d < s);
        val = (d - s) & 0xFFFFFFFFL;
        rng = (t - s) & 0xFFFFFFFFL;
        normalize();
        return ret - offset;
    }

    /**
     * Decodes a uniformly distributed integer in [0, ft).
     * <p>
     * [0, ft) 에서 균등하게 분포한 정수를 디코딩한다.
     *
     * @param ftIn the number of values (at least 2) / 값의 개수(2 이상)
     * @return the value / 값
     */
    long readUint(long ftIn) {
        long ft = ftIn - 1;
        int ftb = ilog(ft);
        if (ftb > UINT_BITS) {
            ftb -= UINT_BITS;
            long f = (ft >> ftb) + 1;
            long s = decode(f);
            update(s, s + 1, f);
            long t = (s << ftb) | readBits(ftb);
            if (t <= ft) {
                return t;
            }
            error = true;
            return ft;
        }
        ft++;
        long s = decode(ft);
        update(s, s + 1, ft);
        return s;
    }

    /**
     * Reads raw bits from the back of the data.
     * <p>
     * 데이터의 뒤쪽에서 원시 비트를 읽는다.
     *
     * @param bits number of bits, 0..25 / 비트 수(0..25)
     * @return the bits / 읽은 비트
     */
    long readBits(int bits) {
        long window = endWindow;
        int available = nendBits;
        // 창에 비트가 모자라면 뒤에서 바이트를 더 가져온다.
        if (available < bits) {
            do {
                window |= ((long) readByteFromEnd()) << available;
                available += SYM_BITS;
            } while (available <= CODE_BITS - SYM_BITS);
        }
        long ret = window & ((1L << bits) - 1);
        window >>>= bits;
        available -= bits;
        endWindow = window & 0xFFFFFFFFL;
        nendBits = available;
        nbitsTotal += bits;
        return ret;
    }

    /**
     * Decodes a Laplace-distributed integer (RFC 6716 4.3.2.1: coarse band energies).
     * <p>
     * 라플라스 분포 정수를 디코딩한다(RFC 6716 4.3.2.1: 대역 에너지의 거친 값).
     *
     * @param fs0   probability of zero in units of 2^-15 / 0 의 확률(2^-15 단위)
     * @param decay decay rate of the distribution in units of 2^-14 / 분포의 감쇠율(2^-14 단위)
     * @return the value / 값
     */
    int laplace(int fs0, int decay) {
        int fs = fs0;
        int val0 = 0;
        long fm = decodeBin(15);
        long fl = 0;
        // 0 이 아니면 확률을 점점 줄여 가며 값의 크기를 찾는다.
        if (fm >= fs) {
            val0++;
            fl = fs;
            fs = laplaceFreq1(fs, decay) + 1;
            // 조건이 만족되는 동안 반복해서 처리한다.
            while (fs > 1 && fm >= fl + 2L * fs) {
                fs *= 2;
                fl += fs;
                fs = (int) (((long) (fs - 2) * decay) >> 15);
                fs += 1;
                val0++;
            }
            if (fs <= 1) {
                int di = (int) ((fm - fl) >> 1);
                val0 += di;
                fl += 2L * di;
            }
            if (fm < fl + fs) {
                val0 = -val0;
            } else {
                fl += fs;
            }
        }
        update(fl, Math.min(fl + fs, 32768), 32768);
        return val0;
    }

    /**
     * Frequency of the value +/-1 of the Laplace distribution.
     * <p>
     * 라플라스 분포에서 값 +/-1 의 빈도.
     *
     * @param fs0   probability of zero / 0 의 확률
     * @param decay decay rate / 감쇠율
     * @return the frequency / 빈도
     */
    private static int laplaceFreq1(int fs0, int decay) {
        long ft = 32768 - 32 - fs0;
        return (int) ((ft * (16384 - decay)) >> 15);
    }

    /**
     * Number of bits of a value (the position of the highest set bit plus one; 0 for 0).
     * <p>
     * 값의 비트 수(가장 높은 1 비트의 위치 + 1. 0 이면 0).
     *
     * @param v the value (unsigned 32 bits) / 값(부호 없는 32비트)
     * @return the bit count / 비트 수
     */
    static int ilog(long v) {
        return 64 - Long.numberOfLeadingZeros(v & 0xFFFFFFFFL);
    }

    /**
     * Returns the number of whole bits consumed so far.
     * <p>
     * 지금까지 소비한 비트 수(정수 비트)를 반환한다.
     *
     * @return the bits / 비트 수
     */
    int tell() {
        return nbitsTotal - ilog(rng);
    }

    /**
     * Returns the number of bits consumed so far in units of 1/8 bit.
     * <p>
     * 지금까지 소비한 비트 수를 1/8 비트 단위로 반환한다.
     *
     * @return the 1/8 bits / 1/8 비트 수
     */
    int tellFrac() {
        long nbits = ((long) nbitsTotal) << 3;
        int l = ilog(rng);
        long r = rng >> (l - 16);
        // 로그의 소수부를 제곱을 되풀이하며 3비트 구한다.
        for (int i = 3; i-- > 0;) {
            r = (r * r) >> 15;
            int b = (int) (r >> 16);
            l = (l << 1) | b;
            r >>= b;
        }
        return (int) (nbits - l);
    }

    /**
     * Returns the final range coder state, the checksum compared with the value that the encoder stored.
     * <p>
     * 최종 범위 디코더 상태를 반환한다. 인코더가 저장한 값과 비교하는 체크섬이다.
     *
     * @return the 32-bit state / 32비트 상태
     */
    int getFinalRange() {
        return (int) rng;
    }

    /**
     * Declares the rest of the data as consumed, so that {@link #tell()} reports <code>totalBits</code> (used for silence frames).
     * <p>
     * 나머지 데이터를 소비한 것으로 처리하여 {@link #tell()} 이 <code>totalBits</code> 를 보고하게 한다(무음 프레임에 쓴다).
     *
     * @param totalBits the number of bits to report / 보고할 비트 수
     */
    void skipTo(int totalBits) {
        nbitsTotal += totalBits - tell();
    }

    /**
     * Returns whether the decoder met inconsistent data.
     * <p>
     * 디코더가 모순된 데이터를 만났는지 반환한다.
     *
     * @return true after an error / 오류가 있었으면 true
     */
    boolean hasError() {
        return error;
    }

    /**
     * Returns the number of bytes of the data.
     * <p>
     * 데이터의 바이트 수를 반환한다.
     *
     * @return the size / 크기
     */
    int getStorage() {
        return storage;
    }

    /**
     * Shortens the data (used to cut off the redundancy bytes of a frame).
     * <p>
     * 데이터를 줄인다(프레임의 중복 바이트를 잘라 낼 때 쓴다).
     *
     * @param bytes number of bytes to remove from the end / 끝에서 제거할 바이트 수
     */
    void shrink(int bytes) {
        storage -= bytes;
    }
}
