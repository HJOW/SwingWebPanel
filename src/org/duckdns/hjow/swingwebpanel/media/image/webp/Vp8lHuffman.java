package org.duckdns.hjow.swingwebpanel.media.image.webp;

/**
 * A prefix (Huffman) code of the WebP lossless format, built from code lengths (plan item P18-03). The codes are canonical, assigned in the
 * order of increasing length and symbol value, and the first bit read from the stream is the most significant bit of the code. Codes of up to
 * {@link #FAST_BITS} bits are decoded with one table lookup; longer ones fall back to the canonical decoding of zlib's "puff" that compares
 * the code with the first code of each length. A code with a single symbol needs no bits at all. An incomplete or over-subscribed code is
 * rejected, as in the reference decoder.
 * <p>
 * WebP 무손실 형식의 접두 부호(허프만 부호). 부호 길이로 만든다(계획 P18-03). 부호는 정규 부호이며 길이가 짧은 것, 같으면 기호 값이 작은 것부터
 * 배정하고 스트림에서 처음 읽은 비트가 부호의 최상위 비트이다. {@link #FAST_BITS} 비트 이하 부호는 표 조회 한 번으로 디코딩하고 더 긴 것은 zlib
 * "puff" 의 정규 디코딩(부호를 길이별 첫 부호와 비교)으로 처리한다. 기호가 하나뿐인 부호는 비트를 읽지 않는다. 불완전하거나 초과 구독된 부호는
 * 참조 디코더처럼 거부한다.
 */
final class Vp8lHuffman {

    /**
     * Number of bits that the fast lookup table resolves.
     * <p>
     * 빠른 조회 표가 해결하는 비트 수.
     */
    static final int FAST_BITS = 8;

    /**
     * The longest code length that the format allows.
     * <p>
     * 형식이 허용하는 가장 긴 부호 길이.
     */
    static final int MAX_LENGTH = 15;

    /**
     * The only symbol if the code has a single symbol, otherwise -1.
     * <p>
     * 기호가 하나뿐인 부호의 그 기호. 아니면 -1.
     */
    private final int single;

    /**
     * Lookup table of the first {@link #FAST_BITS} bits: <code>(length &lt;&lt; 16) | symbol</code>, or 0 if the code is longer.
     * <p>
     * 처음 {@link #FAST_BITS} 비트의 조회 표: <code>(길이 &lt;&lt; 16) | 기호</code>. 부호가 더 길면 0.
     */
    private final int[] fast;

    /**
     * Number of codes of each length.
     * <p>
     * 길이별 부호 수.
     */
    private final int[] count;

    /**
     * Symbols ordered by code length and value.
     * <p>
     * 부호 길이와 값 순서로 정렬한 기호.
     */
    private final int[] sorted;

    /**
     * Creates a code; use {@link #build}.
     * <p>
     * 부호를 만든다. 대신 {@link #build} 를 쓴다.
     *
     * @param single the single symbol or -1 / 기호가 하나뿐이면 그 기호, 아니면 -1
     * @param fast   fast table or null / 빠른 표 또는 null
     * @param count  counts per length or null / 길이별 개수 또는 null
     * @param sorted sorted symbols or null / 정렬된 기호 또는 null
     */
    private Vp8lHuffman(int single, int[] fast, int[] count, int[] sorted) {
        this.single = single;
        this.fast = fast;
        this.count = count;
        this.sorted = sorted;
    }

    /**
     * Returns the number of ints that a code of this alphabet size occupies; the decoder uses it to bound the memory of hostile streams.
     * <p>
     * 이 알파벳 크기의 부호가 차지하는 int 수를 반환한다. 디코더가 악의적 스트림의 메모리를 제한하는 데 쓴다.
     *
     * @param alphabetSize number of symbols / 기호 수
     * @return number of ints / int 개수
     */
    static int cost(int alphabetSize) {
        return (1 << FAST_BITS) + MAX_LENGTH + 1 + alphabetSize;
    }

    /**
     * Builds a code from code lengths.
     * <p>
     * 부호 길이로 부호를 만든다.
     *
     * @param lengths code length of each symbol, 0 for unused symbols / 기호별 부호 길이(쓰지 않는 기호는 0)
     * @param size    number of symbols to use from the array / 배열에서 쓸 기호 수
     * @param keep    whether to keep the tables; false only validates the lengths and returns a placeholder / 표를 보관할지 여부. false 이면 길이만 검증하고 자리 표시자를 반환한다
     * @return the code, or null if the lengths do not form a valid code / 부호. 길이가 올바른 부호를 이루지 않으면 null
     */
    static Vp8lHuffman build(int[] lengths, int size, boolean keep) {
        int[] cnt = new int[MAX_LENGTH + 1];
        int symbols = 0;
        int lastSymbol = 0;
        // 길이별 부호 개수를 세고 기호가 몇 개인지 확인한다.
        for (int i = 0; i < size; i++) {
            if (lengths[i] != 0) {
                cnt[lengths[i]]++;
                symbols++;
                lastSymbol = i;
            }
        }
        if (symbols == 0) {
            return null;
        }
        if (symbols == 1) {
            // 기호가 하나뿐이면 길이와 관계없이 비트를 읽지 않는 부호이다.
            return new Vp8lHuffman(lastSymbol, null, null, null);
        }
        int left = 1;
        // 부호 공간을 길이 순서로 나누어 쓰며 초과 구독인지 확인한다.
        for (int len = 1; len <= MAX_LENGTH; len++) {
            left <<= 1;
            left -= cnt[len];
            if (left < 0) {
                return null;
            }
        }
        if (left != 0) {
            // 부호 공간이 남았으면 불완전한 부호이다.
            return null;
        }
        if (!keep) {
            return new Vp8lHuffman(-1, null, null, null);
        }
        int[] offs = new int[MAX_LENGTH + 2];
        // 길이별 정렬 위치의 시작을 계산한다.
        for (int len = 1; len <= MAX_LENGTH; len++) {
            offs[len + 1] = offs[len] + cnt[len];
        }
        int[] srt = new int[symbols];
        // 기호를 길이별로, 같은 길이에서는 값 순서로 정렬해 담는다.
        for (int i = 0; i < size; i++) {
            if (lengths[i] != 0) {
                srt[offs[lengths[i]]++] = i;
            }
        }
        int[] table = new int[1 << FAST_BITS];
        int code = 0;
        int index = 0;
        // 정규 부호를 배정하며 짧은 부호는 비트를 뒤집은 위치에 빠른 표로 채운다.
        for (int len = 1; len <= MAX_LENGTH; len++) {
            for (int k = 0; k < cnt[len]; k++) {
                if (len <= FAST_BITS) {
                    int rev = Integer.reverse(code) >>> (32 - len);
                    int entry = (len << 16) | srt[index];
                    // 앞 8비트가 같은 모든 표 위치에 같은 항목을 채운다.
                    for (int i = rev; i < (1 << FAST_BITS); i += 1 << len) {
                        table[i] = entry;
                    }
                }
                code++;
                index++;
            }
            code <<= 1;
        }
        return new Vp8lHuffman(-1, table, cnt, srt);
    }

    /**
     * Reads one symbol from the bitstream.
     * <p>
     * 비트스트림에서 기호 하나를 읽는다.
     *
     * @param br the bit reader / 비트 리더
     * @return the symbol / 기호
     */
    int read(Vp8lBits br) {
        if (single >= 0) {
            return single;
        }
        int v = br.peek(MAX_LENGTH);
        int e = fast[v & ((1 << FAST_BITS) - 1)];
        if (e != 0) {
            br.skip(e >>> 16);
            return e & 0xffff;
        }
        int code = 0;
        int first = 0;
        int index = 0;
        // 긴 부호는 길이를 하나씩 늘리며 해당 길이의 첫 부호와 비교한다.
        for (int len = 1; len <= MAX_LENGTH; len++) {
            code |= (v >> (len - 1)) & 1;
            int c = count[len];
            if (code - c < first) {
                br.skip(len);
                return sorted[index + (code - first)];
            }
            index += c;
            first += c;
            first <<= 1;
            code <<= 1;
        }
        throw new Vp8lBits.LosslessException("invalid prefix code in the lossless bitstream");
    }
}
