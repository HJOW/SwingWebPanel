package org.duckdns.hjow.swingwebpanel.media.audio.aac;

/**
 * Huffman decoding of AAC: the scale factor codebook and the spectral codebooks 1 to 11 including the sign bits of the unsigned codebooks
 * and the escape sequences of codebook 11. The code tables of {@link AacCodebooks} are turned into binary trees once.
 * <p>
 * AAC 의 허프만 해독: 스케일팩터 코드북과 스펙트럼 코드북 1~11(부호 없는 코드북의 부호 비트와 코드북 11 의 이스케이프 열 포함).
 * {@link AacCodebooks} 의 부호 표를 한 번 이진 트리로 만든다.
 */
final class AacHuffman {

    /** Codebook rows by index 0 to 10 spectral and 11 for the scale factors. / 인덱스 0~10 스펙트럼, 11 은 스케일팩터 코드북의 행. */
    private static final int[][][] ROWS = new int[12][][];
    /** Binary trees by codebook: child[2 * node + bit] = next node, or -(row + 1) for a leaf, 0 = invalid. / 코드북별 이진 트리. */
    private static final int[][] TREES = new int[12][];
    /** True for the codebooks whose values are unsigned and followed by sign bits (codebooks 3, 4, 7 to 11). / 부호 없는 값 뒤에 부호 비트가 오는 코드북. */
    private static final boolean[] UNSIGNED = { false, false, true, true, false, false, true, true, true, true, true };

    static {
        for (int i = 0; i < 11; i++) { // 스펙트럼 코드북 11개와 스케일팩터 코드북으로 트리를 만든다
            ROWS[i] = AacCodebooks.spectral(i);
        }
        ROWS[11] = AacCodebooks.hcb_sf();
        for (int c = 0; c < 12; c++) { // 코드북마다 트리를 만든다
            TREES[c] = build(ROWS[c]);
        }
    }

    /**
     * Private constructor: static helpers. / 비공개 생성자: 정적 도우미.
     */
    private AacHuffman() {
    }

    /**
     * Builds the tree of a codebook.
     * <p>
     * 코드북의 트리를 만든다.
     *
     * @param rows codebook rows / 코드북 행
     * @return tree / 트리
     */
    private static int[] build(int[][] rows) {
        int[] tree = new int[2 * (rows.length * 2 + 2)];
        int nodes = 1;
        for (int r = 0; r < rows.length; r++) { // 행마다 부호어를 따라 내려가며 노드를 만든다
            int len = rows[r][0];
            int code = rows[r][1];
            int node = 0;
            for (int b = len - 1; b >= 0; b--) { // 부호어의 비트를 앞에서부터 따라간다
                int bit = (code >> b) & 1;
                int idx = 2 * node + bit;
                if (b == 0) {
                    tree[idx] = -(r + 1);
                } else {
                    if (tree[idx] == 0) {
                        tree[idx] = nodes++;
                    }
                    node = tree[idx];
                }
            }
        }
        return tree;
    }

    /**
     * Reads one code word and returns the row index.
     * <p>
     * 부호어 하나를 읽고 행 번호를 반환한다.
     *
     * @param in    input / 입력
     * @param which codebook (0 to 10 spectral, 11 scale factors) / 코드북
     * @return row index / 행 번호
     * @throws AacException on an invalid code / 올바르지 않은 부호일 때
     */
    private static int walk(AacBits in, int which) throws AacException {
        int[] tree = TREES[which];
        int node = 0;
        for (int i = 0; i < 32; i++) { // 잎에 닿을 때까지 비트를 읽는다
            int c = tree[2 * node + in.bit()];
            if (c < 0) {
                return -c - 1;
            }
            if (c == 0) {
                break;
            }
            node = c;
        }
        throw new AacException("invalid Huffman code");
    }

    /**
     * Decodes one scale factor difference (the value 0 to 120, which the caller reduces by 60).
     * <p>
     * 스케일팩터 차분 하나(값 0~120, 호출자가 60 을 뺀다)를 해독한다.
     *
     * @param in input / 입력
     * @return the value / 값
     * @throws AacException on an invalid code / 올바르지 않은 부호일 때
     */
    static int scaleFactor(AacBits in) throws AacException {
        return ROWS[11][walk(in, 11)][2];
    }

    /**
     * Decodes four (codebooks 1 to 4) or two (5 to 11) spectral values.
     * <p>
     * 스펙트럼 값 4개(코드북 1~4) 또는 2개(5~11)를 해독한다.
     *
     * @param in  input / 입력
     * @param cb  codebook number 1 to 11 / 코드북 번호
     * @param out output values / 출력 값
     * @param off first output index / 첫 출력 위치
     * @throws AacException on an invalid code / 올바르지 않은 부호일 때
     */
    static void spectral(AacBits in, int cb, int[] out, int off) throws AacException {
        int[] row = ROWS[cb - 1][walk(in, cb - 1)];
        int n = cb < 5 ? 4 : 2;
        for (int i = 0; i < n; i++) { // 코드북의 값을 복사한다
            out[off + i] = row[2 + i];
        }
        if (UNSIGNED[cb - 1]) {
            for (int i = 0; i < n; i++) { // 0 이 아닌 값마다 부호 비트를 읽는다
                if (out[off + i] != 0 && in.bit() == 1) {
                    out[off + i] = -out[off + i];
                }
            }
        }
        if (cb == 11) {
            for (int i = 0; i < 2; i++) { // 크기가 16 인 값은 이스케이프 열을 읽는다
                if (Math.abs(out[off + i]) == 16) {
                    int bits = 4;
                    while (in.flag() && bits < 24) { // 1 이 이어지는 만큼 비트 수를 늘린다
                        bits++;
                    }
                    int mag = in.bits(bits) | (1 << bits);
                    out[off + i] = out[off + i] < 0 ? -mag : mag;
                }
            }
        }
    }
}
