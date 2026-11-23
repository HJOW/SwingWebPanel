package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The symbol decoder (range decoder) of AV1 (plan item P18-03; AV1 specification section 8.2): it decodes symbols with adaptive cumulative
 * distribution functions, booleans and literals from the bytes of a tile. A cumulative distribution is an int array of N + 1 entries:
 * the cumulative probabilities (scaled to 32768) of the symbols 0 to N - 1, the last of which is 32768, followed by the adaptation counter.
 * Reading past the end of the tile yields zero bits as the specification requires.
 * <p>
 * AV1 의 심볼 디코더(범위 디코더)(계획 P18-03; AV1 규격 8.2절): 타일의 바이트에서 적응형 누적 분포 함수로 심볼을, 그리고 불리언과 리터럴을 디코딩한다.
 * 누적 분포는 N + 1 개의 정수 배열이다: 심볼 0~N-1 의 누적 확률(32768 단위, 마지막은 32768)과 적응 카운터. 타일 끝을 넘어 읽으면 규격대로 0 비트를
 * 얻는다.
 */
final class Av1SymbolDecoder {

    /**
     * The tile data.
     * <p>
     * 타일 데이터.
     */
    private final byte[] data;

    /**
     * The index of the next byte to read.
     * <p>
     * 다음에 읽을 바이트의 위치.
     */
    private int bytePos;

    /**
     * The index after the last byte of the tile.
     * <p>
     * 타일의 마지막 바이트 다음 위치.
     */
    private final int end;

    /**
     * The bits that were read from the data but not yet used.
     * <p>
     * 데이터에서 읽었지만 아직 쓰지 않은 비트.
     */
    private long bitBuf;

    /**
     * The number of valid bits in {@link #bitBuf}.
     * <p>
     * {@link #bitBuf} 의 유효한 비트 수.
     */
    private int bitCount;

    /**
     * The symbol value of the specification (SymbolValue).
     * <p>
     * 규격의 심볼 값(SymbolValue).
     */
    private int symbolValue;

    /**
     * The symbol range of the specification (SymbolRange).
     * <p>
     * 규격의 심볼 범위(SymbolRange).
     */
    private int symbolRange;

    /**
     * The number of bits that may still be read (SymbolMaxBits); it may become negative.
     * <p>
     * 아직 읽을 수 있는 비트 수(SymbolMaxBits). 음수가 될 수 있다.
     */
    private int symbolMaxBits;

    /**
     * The scratch distribution of {@link #bool()}.
     * <p>
     * {@link #bool()} 가 쓰는 임시 분포.
     */
    private final int[] boolCdf = new int[3];

    /**
     * True if the adaptation of the distributions is disabled.
     * <p>
     * 분포의 적응을 끄면 true.
     */
    private final boolean disableUpdate;

    /**
     * Starts decoding a tile (<code>init_symbol</code>).
     * <p>
     * 타일의 디코딩을 시작한다(<code>init_symbol</code>).
     *
     * @param data          the data / 데이터
     * @param offset        the first byte of the tile / 타일의 첫 바이트
     * @param size          the size of the tile in bytes / 타일의 크기(바이트)
     * @param disableUpdate true if the distributions are not adapted / 분포를 적응시키지 않으면 true
     */
    Av1SymbolDecoder(byte[] data, int offset, int size, boolean disableUpdate) {
        this.data = data;
        this.bytePos = offset;
        this.end = offset + size;
        this.disableUpdate = disableUpdate;
        int numBits = Math.min(size * 8, 15);
        int buf = (int) readBits(numBits);
        int padded = buf << (15 - numBits);
        symbolValue = ((1 << 15) - 1) ^ padded;
        symbolRange = 1 << 15;
        symbolMaxBits = 8 * size - 15;
    }

    /**
     * Reads bits from the tile data (zero bits past the end).
     * <p>
     * 타일 데이터에서 비트를 읽는다(끝을 넘으면 0 비트).
     *
     * @param n the number of bits (0 to 24) / 비트 수(0~24)
     * @return the value / 값
     */
    private long readBits(int n) {
        // 필요한 만큼 바이트를 읽어 비트 버퍼에 채운다.
        while (bitCount < n) {
            int b = bytePos < end ? (data[bytePos] & 0xFF) : 0;
            bytePos++;
            bitBuf = (bitBuf << 8) | b;
            bitCount += 8;
        }
        long v = (bitBuf >> (bitCount - n)) & ((1L << n) - 1);
        bitCount -= n;
        bitBuf &= (1L << bitCount) - 1;
        return v;
    }

    /**
     * Decodes a symbol and adapts the distribution (<code>read_symbol</code>).
     * <p>
     * 심볼을 디코딩하고 분포를 적응시킨다(<code>read_symbol</code>).
     *
     * @param cdf    the array that holds the distribution / 분포를 담은 배열
     * @param off    the offset of the distribution in the array / 배열 안의 분포 위치
     * @param n      the number of symbols / 심볼 수
     * @return the decoded symbol / 디코딩된 심볼
     */
    int symbol(int[] cdf, int off, int n) {
        int cur = symbolRange;
        int prev;
        int sym = -1;
        // 심볼 값이 구간 경계 아래로 내려갈 때까지 심볼을 하나씩 확인한다.
        do {
            sym++;
            prev = cur;
            int f = (1 << 15) - cdf[off + sym];
            cur = ((symbolRange >> 8) * (f >> 6)) >> 1;
            cur += 4 * (n - sym - 1);
        } while (symbolValue < cur);
        symbolRange = prev - cur;
        symbolValue -= cur;
        renormalize();
        if (!disableUpdate) {
            int count = cdf[off + n];
            int rate = 3 + (count > 15 ? 1 : 0) + (count > 31 ? 1 : 0) + Math.min(floorLog2(n), 2);
            int tmp = 0;
            // 확률을 읽은 심볼 쪽으로 조금 옮긴다.
            for (int i = 0; i < n - 1; i++) {
                if (i == sym) {
                    tmp = 1 << 15;
                }
                if (tmp < cdf[off + i]) {
                    cdf[off + i] -= (cdf[off + i] - tmp) >> rate;
                } else {
                    cdf[off + i] += (tmp - cdf[off + i]) >> rate;
                }
            }
            if (count < 32) {
                cdf[off + n] = count + 1;
            }
        }
        return sym;
    }

    /**
     * Keeps the range in 15 bits by moving new bits into the value.
     * <p>
     * 새 비트를 값으로 옮겨 범위를 15비트로 유지한다.
     */
    private void renormalize() {
        int bits = 15 - floorLog2(symbolRange);
        symbolRange <<= bits;
        int numBits = Math.min(bits, Math.max(0, symbolMaxBits));
        int newData = (int) readBits(numBits);
        int padded = newData << (bits - numBits);
        symbolValue = padded ^ (((symbolValue + 1) << bits) - 1);
        symbolMaxBits -= bits;
    }

    /**
     * The base 2 logarithm rounded down.
     * <p>
     * 내림한 밑 2 로그.
     *
     * @param x a positive number / 양수
     * @return the logarithm / 로그
     */
    private static int floorLog2(int x) {
        return 31 - Integer.numberOfLeadingZeros(x);
    }

    /**
     * Decodes an equiprobable bit (<code>read_bool</code>).
     * <p>
     * 확률이 같은 비트를 디코딩한다(<code>read_bool</code>).
     *
     * @return 0 or 1 / 0 또는 1
     */
    int bool() {
        boolCdf[0] = 1 << 14;
        boolCdf[1] = 1 << 15;
        boolCdf[2] = 0;
        return symbol(boolCdf, 0, 2);
    }

    /**
     * Decodes an unsigned literal of n bits (<code>read_literal</code>).
     * <p>
     * n 비트의 부호 없는 리터럴을 디코딩한다(<code>read_literal</code>).
     *
     * @param n the number of bits / 비트 수
     * @return the value / 값
     */
    int literal(int n) {
        int x = 0;
        // 불리언을 n 번 읽어 상위 비트부터 이어 붙인다.
        for (int i = 0; i < n; i++) {
            x = 2 * x + bool();
        }
        return x;
    }

    /**
     * Decodes an unsigned value less than n with a uniform distribution (<code>NS(n)</code> of the symbol decoder).
     * <p>
     * 균등 분포인 n 미만의 부호 없는 값을 디코딩한다(심볼 디코더의 <code>NS(n)</code>).
     *
     * @param n the number of values / 값의 수
     * @return the value / 값
     */
    int uniform(int n) {
        int w = 0;
        int x = n;
        // n 을 표현하는 비트 수 w 를 구한다.
        while (x != 0) {
            x >>= 1;
            w++;
        }
        int m = (1 << w) - n;
        int v = literal(w - 1);
        if (v < m) {
            return v;
        }
        int extra = literal(1);
        return (v << 1) - m + extra;
    }
}
