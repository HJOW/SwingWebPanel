package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * The boolean entropy decoder of VP8 (RFC 6386 section 7). It reads one bit at a time with a given probability that the bit is 0 (an
 * 8-bit number: 0 is never given, 128 means "equally likely"), keeps a bit buffer of up to 56 bits so that most bits need no byte access,
 * and treats the bytes after the end of its data as zeros, as the specification allows. How far the decoder had to go past the end of its
 * data is counted, so that the caller can tell a truncated partition from a complete one with {@link #isOverrun()}.
 * The arithmetic is exactly the one of the specification; only the way the bits are buffered differs from the sample code.
 * <p>
 * VP8 의 불리언 엔트로피 디코더(RFC 6386 7절). 비트가 0 일 확률(8비트 수. 0 은 주어지지 않으며 128 은 "같은 확률")을 받아 비트를 하나씩 읽는다.
 * 56비트까지의 비트 버퍼를 두어 대부분의 비트는 바이트 접근이 필요 없고, 규격이 허용하듯 데이터 끝 뒤의 바이트는 0 으로 본다.
 * 데이터 끝을 넘어 간 정도를 세므로 호출자가 {@link #isOverrun()} 으로 잘린 파티션과 온전한 파티션을 구별할 수 있다.
 * 산술은 규격과 정확히 같으며 비트를 버퍼링하는 방식만 예제 코드와 다르다.
 */
final class Vp8BoolDecoder {

    /**
     * Number of bytes past the end of the data that a valid stream may still request (the decoder looks ahead a little).
     * <p>
     * 올바른 스트림이 데이터 끝 뒤에서도 요청할 수 있는 바이트 수(디코더가 조금 앞서 읽는다).
     */
    private static final int ALLOWED_OVERRUN = 3;

    /**
     * The data.
     * <p>
     * 데이터.
     */
    private final byte[] data;

    /**
     * Position of the next byte to load.
     * <p>
     * 다음에 읽을 바이트의 위치.
     */
    private int pos;

    /**
     * End of the data of this decoder (exclusive).
     * <p>
     * 이 디코더의 데이터 끝(포함하지 않음).
     */
    private final int end;

    /**
     * The bit buffer: the not yet consumed bits, the next one at bit position {@link #bits} + 7.
     * <p>
     * 비트 버퍼: 아직 쓰지 않은 비트들. 다음 비트는 {@link #bits} + 7 위치에 있다.
     */
    private long value;

    /**
     * The current range minus one (127..254 between two calls).
     * <p>
     * 현재 범위에서 1 을 뺀 값(호출 사이에는 127..254).
     */
    private int range = 255 - 1;

    /**
     * Number of buffered bits minus 8; below zero the buffer must be refilled.
     * <p>
     * 버퍼에 든 비트 수에서 8 을 뺀 값. 0 미만이면 버퍼를 다시 채워야 한다.
     */
    private int bits = -8;

    /**
     * Number of bytes that were requested beyond the end of the data.
     * <p>
     * 데이터 끝을 넘어 요청한 바이트 수.
     */
    private int overrun;

    /**
     * Creates a decoder over a part of an array.
     * <p>
     * 배열의 일부에 대한 디코더를 만든다.
     *
     * @param data   array / 배열
     * @param offset first byte of the data / 데이터의 첫 바이트
     * @param length number of bytes / 바이트 수
     */
    Vp8BoolDecoder(byte[] data, int offset, int length) {
        this.data = data;
        this.pos = offset;
        this.end = offset + length;
    }

    /**
     * Refills the bit buffer with up to 7 bytes, or with zero bytes past the end.
     * <p>
     * 비트 버퍼를 최대 7바이트로, 끝을 넘으면 0 바이트로 다시 채운다.
     */
    private void load() {
        // 남은 바이트가 7개 이상이면 한 번에 7바이트를 읽는다.
        if (end - pos >= 7) {
            long v = 0;
            // 7바이트를 빅 엔디언 순서로 이어 붙여 하나의 값으로 만든다.
            for (int i = 0; i < 7; i++) {
                v = (v << 8) | (data[pos++] & 0xff);
            }
            value = (value << 56) | v;
            bits += 56;
        } else if (pos < end) {
            // 몇 바이트 안 남았으면 한 바이트씩 읽는다.
            value = (value << 8) | (data[pos++] & 0xff);
            bits += 8;
        } else {
            // 끝을 넘으면 0 바이트를 공급하고 넘어선 횟수를 센다.
            value <<= 8;
            bits += 8;
            if (overrun < Integer.MAX_VALUE) {
                overrun++;
            }
        }
    }

    /**
     * Reads one bit whose probability of being 0 is prob/256.
     * <p>
     * 0 일 확률이 prob/256 인 비트 하나를 읽는다.
     *
     * @param prob probability of a zero bit, 1..255 / 0 비트의 확률, 1..255
     * @return the bit, 0 or 1 / 비트(0 또는 1)
     */
    int getBit(int prob) {
        int r = range;
        if (bits < 0) {
            load();
        }
        int shiftPos = bits;
        int split = (r * prob) >> 8;
        int v = (int) (value >>> shiftPos);
        int bit;
        if (v > split) {
            r -= split;
            value -= (long) (split + 1) << shiftPos;
            bit = 1;
        } else {
            r = split + 1;
            bit = 0;
        }
        // 범위가 128 미만이 되지 않도록 한 번에 정규화한다.
        int shift = 7 ^ (31 - Integer.numberOfLeadingZeros(r));
        r <<= shift;
        bits -= shift;
        range = r - 1;
        return bit;
    }

    /**
     * Reads one bit with the probability 1/2 (a "flag" bit of the headers).
     * <p>
     * 확률이 1/2 인 비트 하나를 읽는다(헤더의 "플래그" 비트).
     *
     * @return the bit / 비트
     */
    int getFlag() {
        return getBit(128);
    }

    /**
     * Reads an unsigned number of n bits, most significant bit first (the function L(n) of the specification).
     * <p>
     * 상위 비트부터 n 비트의 부호 없는 수를 읽는다(규격의 L(n) 함수).
     *
     * @param n number of bits / 비트 수
     * @return the number / 수
     */
    int getLiteral(int n) {
        int v = 0;
        // 상위 비트부터 한 비트씩 읽어 모은다.
        for (int i = 0; i < n; i++) {
            v = (v << 1) | getBit(128);
        }
        return v;
    }

    /**
     * Reads an n-bit magnitude followed by a sign bit.
     * <p>
     * n 비트 크기와 그 뒤의 부호 비트를 읽는다.
     *
     * @param n number of magnitude bits / 크기의 비트 수
     * @return the signed number / 부호 있는 수
     */
    int getSigned(int n) {
        int v = getLiteral(n);
        return getBit(128) != 0 ? -v : v;
    }

    /**
     * Reads a flag; if it is set, a signed n-bit number follows, otherwise the result is 0.
     * <p>
     * 플래그를 읽고 켜져 있으면 n 비트 부호 있는 수가 따르며 아니면 결과는 0 이다.
     *
     * @param n number of magnitude bits / 크기의 비트 수
     * @return the number or 0 / 수 또는 0
     */
    int getOptionalSigned(int n) {
        return getBit(128) != 0 ? getSigned(n) : 0;
    }

    /**
     * Reads a value from a tree (the function treed_read of the specification). A node value above 0 is the index of the next node pair;
     * a value of 0 or less is a leaf that holds the negated result.
     * <p>
     * 트리에서 값을 읽는다(규격의 treed_read 함수). 0 보다 큰 노드 값은 다음 노드 쌍의 색인이고 0 이하는 결과의 음수를 담은 잎이다.
     *
     * @param tree     tree array / 트리 배열
     * @param probs    probabilities, the node at index i uses probs[(i >> 1) + probOffset] / 확률. 색인 i 의 노드는 probs[(i >> 1) + probOffset] 를 쓴다
     * @param probOffset offset of the first probability / 첫 확률의 위치
     * @return the leaf value / 잎의 값
     */
    int getTree(int[] tree, int[] probs, int probOffset) {
        int i = 0;
        // 잎(0 이하)에 닿을 때까지 비트를 읽으며 트리를 내려간다.
        do {
            i = tree[i + getBit(probs[probOffset + (i >> 1)])];
        } while (i > 0);
        return -i;
    }

    /**
     * Tells whether the decoder needed more bytes than the data holds (beyond the small look-ahead that a complete stream may use).
     * <p>
     * 데이터에 든 것보다 더 많은 바이트가 필요했는지 알려 준다(온전한 스트림이 쓸 수 있는 작은 앞서 읽기는 제외).
     *
     * @return true if the data was too short / 데이터가 너무 짧았으면 true
     */
    boolean isOverrun() {
        return overrun > ALLOWED_OVERRUN;
    }
}
