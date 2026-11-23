package org.duckdns.hjow.swingwebpanel.media.image.avif;

/**
 * The wedge masks of the AV1 decoder (plan item P18-03; AV1 specification section 7.11.3.11): the table of the blending masks that split a
 * block along a line, for the block sizes that allow wedge prediction. The table is built once, when it is first used, from the master masks
 * and the code book of the specification.
 * <p>
 * AV1 디코더의 쐐기 마스크(계획 P18-03; AV1 규격 7.11.3.11절): 블록을 직선으로 나누는 혼합 마스크의 표이며 쐐기 예측을 허용하는 블록 크기마다 있다.
 * 표는 규격의 기본 마스크와 코드북으로 처음 쓸 때 한 번 만든다.
 */
final class Av1WedgeMasks {

    /**
     * The size of the master masks.
     * <p>
     * 기본 마스크의 크기.
     */
    private static final int MASK_MASTER_SIZE = 64;

    /**
     * The wedge direction: horizontal.
     * <p>
     * 쐐기 방향: 수평.
     */
    private static final int HORIZONTAL = 0;

    /**
     * The wedge direction: vertical.
     * <p>
     * 쐐기 방향: 수직.
     */
    private static final int VERTICAL = 1;

    /**
     * The wedge direction: 27 degrees.
     * <p>
     * 쐐기 방향: 27도.
     */
    private static final int OBLIQUE27 = 2;

    /**
     * The wedge direction: 63 degrees.
     * <p>
     * 쐐기 방향: 63도.
     */
    private static final int OBLIQUE63 = 3;

    /**
     * The wedge direction: 117 degrees.
     * <p>
     * 쐐기 방향: 117도.
     */
    private static final int OBLIQUE117 = 4;

    /**
     * The wedge direction: 153 degrees.
     * <p>
     * 쐐기 방향: 153도.
     */
    private static final int OBLIQUE153 = 5;

    /**
     * The code book: [block shape][wedge][direction, horizontal offset, vertical offset] flattened.
     * <p>
     * 코드북: [블록 모양][쐐기][방향, 가로 오프셋, 세로 오프셋]을 편 것.
     */
    private static final int[] CODEBOOK = {
        // 높이가 너비보다 큰 블록
        OBLIQUE27, 4, 4, OBLIQUE63, 4, 4, OBLIQUE117, 4, 4, OBLIQUE153, 4, 4,
        HORIZONTAL, 4, 2, HORIZONTAL, 4, 4, HORIZONTAL, 4, 6, VERTICAL, 4, 4,
        OBLIQUE27, 4, 2, OBLIQUE27, 4, 6, OBLIQUE153, 4, 2, OBLIQUE153, 4, 6,
        OBLIQUE63, 2, 4, OBLIQUE63, 6, 4, OBLIQUE117, 2, 4, OBLIQUE117, 6, 4,
        // 너비가 높이보다 큰 블록
        OBLIQUE27, 4, 4, OBLIQUE63, 4, 4, OBLIQUE117, 4, 4, OBLIQUE153, 4, 4,
        VERTICAL, 2, 4, VERTICAL, 4, 4, VERTICAL, 6, 4, HORIZONTAL, 4, 4,
        OBLIQUE27, 4, 2, OBLIQUE27, 4, 6, OBLIQUE153, 4, 2, OBLIQUE153, 4, 6,
        OBLIQUE63, 2, 4, OBLIQUE63, 6, 4, OBLIQUE117, 2, 4, OBLIQUE117, 6, 4,
        // 정사각형 블록
        OBLIQUE27, 4, 4, OBLIQUE63, 4, 4, OBLIQUE117, 4, 4, OBLIQUE153, 4, 4,
        HORIZONTAL, 4, 2, HORIZONTAL, 4, 6, VERTICAL, 2, 4, VERTICAL, 6, 4,
        OBLIQUE27, 4, 2, OBLIQUE27, 4, 6, OBLIQUE153, 4, 2, OBLIQUE153, 4, 6,
        OBLIQUE63, 2, 4, OBLIQUE63, 6, 4, OBLIQUE117, 2, 4, OBLIQUE117, 6, 4 };

    /**
     * The masks: [block size][sign][wedge index] each of Block_Width x Block_Height values; null for sizes without wedges.
     * <p>
     * 마스크: [블록 크기][부호][쐐기 번호] 각각 Block_Width x Block_Height 개의 값. 쐐기가 없는 크기는 null.
     */
    private static byte[][][][] table;

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 가진다.
     */
    private Av1WedgeMasks() {
    }

    /**
     * Returns the wedge mask of a block size.
     * <p>
     * 블록 크기의 쐐기 마스크를 반환한다.
     *
     * @param bsize the block size / 블록 크기
     * @param sign  the sign (0 or 1) / 부호(0 또는 1)
     * @param wedge the wedge index (0..15) / 쐐기 번호(0..15)
     * @return the mask, row by row with the width of the block / 마스크, 블록 너비로 행마다
     */
    static synchronized byte[] get(int bsize, int sign, int wedge) {
        if (table == null) {
            table = build();
        }
        return table[bsize][sign][wedge];
    }

    /**
     * Builds all masks (<code>initialise_wedge_mask_table</code>).
     * <p>
     * 모든 마스크를 만든다(<code>initialise_wedge_mask_table</code>).
     *
     * @return the table / 표
     */
    private static byte[][][][] build() {
        int n = MASK_MASTER_SIZE;
        int[][][] master = new int[6][n][n];
        int[] even = AvifInterTables.WEDGE_MASTER_OBLIQUE_EVEN;
        int[] odd = AvifInterTables.WEDGE_MASTER_OBLIQUE_ODD;
        int[] vert = AvifInterTables.WEDGE_MASTER_VERTICAL;
        // 열마다 63도 사선과 수직선의 기본 마스크를 만든다.
        for (int j = 0; j < n; j++) {
            int shift = n / 4;
            // 63도 사선과 수직선의 두 행을 한 번에 만든다.
            for (int i = 0; i < n; i += 2) {
                master[OBLIQUE63][i][j] = even[clip(j - shift)];
                shift -= 1;
                master[OBLIQUE63][i + 1][j] = odd[clip(j - shift)];
                master[VERTICAL][i][j] = vert[j];
                master[VERTICAL][i + 1][j] = vert[j];
            }
        }
        // 나머지 방향은 63도 사선과 수직선을 뒤집고 돌려서 만든다.
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int msk = master[OBLIQUE63][i][j];
                master[OBLIQUE27][j][i] = msk;
                master[OBLIQUE117][i][n - 1 - j] = 64 - msk;
                master[OBLIQUE153][n - 1 - j][i] = 64 - msk;
                master[HORIZONTAL][j][i] = master[VERTICAL][i][j];
            }
        }
        byte[][][][] t = new byte[22][][][];
        // 쐐기를 허용하는 블록 크기마다 코드북으로 마스크 32개를 자른다.
        for (int bsize = 3; bsize < 22; bsize++) {
            if (AvifInterTables.WEDGE_BITS[bsize] == 0) {
                continue;
            }
            int w = AvifTables.NUM_4X4_BLOCKS_WIDE[bsize] * 4;
            int h = AvifTables.NUM_4X4_BLOCKS_HIGH[bsize] * 4;
            int shape = h > w ? 0 : (h < w ? 1 : 2);
            t[bsize] = new byte[2][16][];
            // 쐐기 번호마다 코드북의 방향과 오프셋으로 마스크를 자른다.
            for (int wedge = 0; wedge < 16; wedge++) {
                int dir = CODEBOOK[(shape * 16 + wedge) * 3];
                int xoff = n / 2 - ((CODEBOOK[(shape * 16 + wedge) * 3 + 1] * w) >> 3);
                int yoff = n / 2 - ((CODEBOOK[(shape * 16 + wedge) * 3 + 2] * h) >> 3);
                int sum = 0;
                // 마스크 윗줄의 평균을 구하려고 합을 낸다.
                for (int i = 0; i < w; i++) {
                    sum += master[dir][yoff][xoff + i];
                }
                // 마스크 왼쪽 줄의 합을 더한다.
                for (int i = 1; i < h; i++) {
                    sum += master[dir][yoff + i][xoff];
                }
                int avg = (sum + (w + h - 1) / 2) / (w + h - 1);
                int flipSign = avg < 32 ? 1 : 0;
                byte[] a = new byte[w * h];
                byte[] b = new byte[w * h];
                // 마스크와 그 보수(64 에서 뺀 값)를 만든다.
                for (int i = 0; i < h; i++) {
                    for (int j = 0; j < w; j++) {
                        int m = master[dir][yoff + i][xoff + j];
                        a[i * w + j] = (byte) m;
                        b[i * w + j] = (byte) (64 - m);
                    }
                }
                t[bsize][flipSign][wedge] = a;
                t[bsize][1 - flipSign][wedge] = b;
            }
        }
        return t;
    }

    /**
     * Clamps an index into the master mask.
     * <p>
     * 기본 마스크의 색인을 범위 안으로 자른다.
     *
     * @param v the index / 색인
     * @return the clamped index / 자른 색인
     */
    private static int clip(int v) {
        return Math.max(0, Math.min(MASK_MASTER_SIZE - 1, v));
    }
}
