package org.duckdns.hjow.swingwebpanel.media.audio.vorbis;

import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * Vorbis floor type 1 (Vorbis I specification, section 7): a piecewise linear envelope in the logarithmic domain defined by integer
 * points whose positions are given in the setup header and whose heights are coded in every packet, predicted from the neighbours.
 * <p>
 * Vorbis 플로어 유형 1(Vorbis I 규격 7절): 로그 영역의 구간별 선형 포락선. 점의 위치는 설정 헤더에 있고 높이는 패킷마다 이웃으로부터 예측해
 * 부호화한다.
 */
final class VorbisFloor1 implements VorbisFloor {

    /**
     * Amplitude ranges for the multiplier values 1 to 4.
     * <p>
     * 곱셈 계수 1~4 에 대한 진폭 범위.
     */
    private static final int[] RANGES = { 256, 128, 86, 64 };

    /**
     * The 256 values that convert the integer heights to linear amplitudes: 10^((i - 255) * 7 / 256).
     * <p>
     * 정수 높이를 선형 진폭으로 바꾸는 256개의 값: 10^((i - 255) * 7 / 256).
     */
    private static final float[] DB_TABLE = new float[256];

    static {
        // 높이마다 약 0.55 dB 씩 올라가는 표를 만든다.
        for (int i = 0; i < 256; i++) {
            DB_TABLE[i] = (float) Math.pow(10.0, (i - 255) * 7.0 / 256.0);
        }
    }

    /**
     * The codebooks of the setup header.
     * <p>
     * 설정 헤더의 코드북.
     */
    private final VorbisCodebook[] books;

    /**
     * Class number of every partition.
     * <p>
     * 파티션마다의 클래스 번호.
     */
    private final int[] partitionClass;

    /**
     * Dimensions of every class.
     * <p>
     * 클래스마다의 차원 수.
     */
    private final int[] classDimensions;

    /**
     * Subclass bits of every class.
     * <p>
     * 클래스마다의 서브클래스 비트 수.
     */
    private final int[] classSubclasses;

    /**
     * Master codebook of every class.
     * <p>
     * 클래스마다의 마스터 코드북.
     */
    private final int[] classMasterbook;

    /**
     * Subclass codebooks of every class, -1 for none.
     * <p>
     * 클래스마다의 서브클래스 코드북. 없으면 -1.
     */
    private final int[][] subclassBooks;

    /**
     * Multiplier 1 to 4.
     * <p>
     * 곱셈 계수 1~4.
     */
    private final int multiplier;

    /**
     * Positions of the points.
     * <p>
     * 점의 위치.
     */
    private final int[] xList;

    /**
     * Number of points.
     * <p>
     * 점의 수.
     */
    private final int values;

    /**
     * Point numbers sorted by position.
     * <p>
     * 위치 순으로 정렬한 점 번호.
     */
    private final int[] sorted;

    /**
     * Low neighbour of every point (the nearest earlier point to the left).
     * <p>
     * 점마다의 낮은 이웃(앞선 점 중 왼쪽으로 가장 가까운 점).
     */
    private final int[] lowNeighbor;

    /**
     * High neighbour of every point (the nearest earlier point to the right).
     * <p>
     * 점마다의 높은 이웃(앞선 점 중 오른쪽으로 가장 가까운 점).
     */
    private final int[] highNeighbor;

    /**
     * Scratch: the coded heights.
     * <p>
     * 작업 공간: 부호화된 높이.
     */
    private final int[] yList;

    /**
     * Scratch: the reconstructed heights.
     * <p>
     * 작업 공간: 복원한 높이.
     */
    private final int[] finalY;

    /**
     * Scratch: which points take part in the curve.
     * <p>
     * 작업 공간: 곡선에 쓰이는 점.
     */
    private final boolean[] step2;

    /**
     * Parses the floor configuration.
     * <p>
     * 플로어 설정을 해석한다.
     *
     * @param br    packet bits / 패킷 비트
     * @param books the codebooks / 코드북
     * @throws MediaException CORRUPT for an invalid configuration / 올바르지 않은 설정이면 CORRUPT
     */
    VorbisFloor1(VorbisBitReader br, VorbisCodebook[] books) throws MediaException {
        this.books = books;
        int partitions = br.read(5);
        partitionClass = new int[partitions];
        int maxClass = -1;
        // 파티션마다 클래스 번호를 읽고 최대 클래스를 구한다.
        for (int i = 0; i < partitions; i++) {
            partitionClass[i] = br.read(4);
            maxClass = Math.max(maxClass, partitionClass[i]);
        }
        classDimensions = new int[maxClass + 1];
        classSubclasses = new int[maxClass + 1];
        classMasterbook = new int[maxClass + 1];
        subclassBooks = new int[maxClass + 1][];
        // 클래스마다 차원, 서브클래스, 코드북 번호를 읽는다.
        for (int c = 0; c <= maxClass; c++) {
            classDimensions[c] = br.read(3) + 1;
            classSubclasses[c] = br.read(2);
            if (classSubclasses[c] != 0) {
                classMasterbook[c] = br.read(8);
                check(classMasterbook[c] < books.length);
            }
            subclassBooks[c] = new int[1 << classSubclasses[c]];
            // 항목마다 반복해서 처리한다.
            for (int k = 0; k < subclassBooks[c].length; k++) {
                subclassBooks[c][k] = br.read(8) - 1;
                check(subclassBooks[c][k] < books.length);
            }
        }
        multiplier = br.read(2) + 1;
        int rangeBits = br.read(4);
        int[] x = new int[2 + partitions * 8];
        x[0] = 0;
        x[1] = 1 << rangeBits;
        int n = 2;
        // 파티션의 차원 수만큼 점의 위치를 읽는다.
        for (int i = 0; i < partitions; i++) {
            for (int j = 0; j < classDimensions[partitionClass[i]]; j++) {
                x[n++] = br.read(rangeBits);
            }
        }
        check(!br.isEndOfPacket() && n <= 65);
        values = n;
        xList = new int[n];
        System.arraycopy(x, 0, xList, 0, n);
        sorted = new int[n];
        // 위치가 같은 점이 없는지 확인하며 위치 순서 배열을 만든다(삽입 정렬).
        for (int i = 0; i < n; i++) {
            int p = i;
            // 조건이 끝날 때까지 반복해서 처리한다.
            while (p > 0 && xList[sorted[p - 1]] > xList[i]) {
                sorted[p] = sorted[p - 1];
                p--;
            }
            sorted[p] = i;
            if (p > 0) {
                check(xList[sorted[p - 1]] != xList[i]);
            }
            if (p < i) {
                check(xList[sorted[p + 1]] != xList[i]);
            }
        }
        lowNeighbor = new int[n];
        highNeighbor = new int[n];
        // 점마다 앞선 점 중 가장 가까운 왼쪽/오른쪽 이웃을 찾는다.
        for (int i = 2; i < n; i++) {
            int lo = 0;
            int hi = 1;
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j < i; j++) {
                if (xList[j] < xList[i] && xList[j] > xList[lo]) {
                    lo = j;
                }
                if (xList[j] > xList[i] && xList[j] < xList[hi]) {
                    hi = j;
                }
            }
            lowNeighbor[i] = lo;
            highNeighbor[i] = hi;
        }
        yList = new int[n];
        finalY = new int[n];
        step2 = new boolean[n];
    }

    /**
     * Fails with CORRUPT unless the condition holds.
     * <p>
     * 조건이 참이 아니면 CORRUPT 로 실패한다.
     *
     * @param ok the condition / 조건
     * @throws MediaException CORRUPT if false / false 이면 CORRUPT
     */
    private static void check(boolean ok) throws MediaException {
        if (!ok) {
            throw new MediaException(MediaException.Kind.CORRUPT, "invalid Vorbis floor 1 configuration");
        }
    }

    /**
     * Decodes the floor of a channel and renders the curve.
     * <p>
     * 채널의 플로어를 디코딩하고 곡선을 만든다.
     *
     * @param br   packet bits / 패킷 비트
     * @param out  receives the curve / 곡선을 받는다
     * @param half half of the block size / 블록 크기의 절반
     * @return true if the channel has a floor / 채널에 플로어가 있으면 true
     */
    public boolean decode(VorbisBitReader br, float[] out, int half) {
        if (br.read(1) == 0) {
            return false;
        }
        int range = RANGES[multiplier - 1];
        int bits = VorbisBitReader.ilog(range - 1);
        yList[0] = br.read(bits);
        yList[1] = br.read(bits);
        int offset = 2;
        // 파티션마다 클래스의 마스터 코드북으로 서브클래스를 정하고 높이를 읽는다.
        for (int i = 0; i < partitionClass.length; i++) {
            int c = partitionClass[i];
            int dim = classDimensions[c];
            int cbits = classSubclasses[c];
            int csub = (1 << cbits) - 1;
            int cval = 0;
            if (cbits > 0) {
                cval = books[classMasterbook[c]].decodeScalar(br);
                if (cval < 0) {
                    return false;
                }
            }
            // 항목마다 반복해서 처리한다.
            for (int j = 0; j < dim; j++) {
                int book = subclassBooks[c][cval & csub];
                cval >>= cbits;
                if (book >= 0) {
                    int y = books[book].decodeScalar(br);
                    if (y < 0) {
                        return false;
                    }
                    yList[offset + j] = y;
                } else {
                    yList[offset + j] = 0;
                }
            }
            offset += dim;
        }
        if (br.isEndOfPacket()) {
            return false;
        }
        reconstruct(range);
        render(out, half);
        return true;
    }

    /**
     * Reconstructs the heights from the coded values by prediction from the neighbours.
     * <p>
     * 부호화된 값에서 이웃으로부터의 예측으로 높이를 복원한다.
     *
     * @param range the amplitude range / 진폭 범위
     */
    private void reconstruct(int range) {
        step2[0] = true;
        step2[1] = true;
        finalY[0] = Math.min(yList[0], range - 1);
        finalY[1] = Math.min(yList[1], range - 1);
        // 세 번째 점부터 이웃 두 점 사이의 선으로 예측하고 부호화된 차이를 적용한다.
        for (int i = 2; i < values; i++) {
            int lo = lowNeighbor[i];
            int hi = highNeighbor[i];
            int predicted = renderPoint(xList[lo], finalY[lo], xList[hi], finalY[hi], xList[i]);
            int val = yList[i];
            int highroom = range - predicted;
            int lowroom = predicted;
            int room = (highroom < lowroom ? highroom : lowroom) * 2;
            if (val != 0) {
                step2[lo] = true;
                step2[hi] = true;
                step2[i] = true;
                if (val >= room) {
                    finalY[i] = highroom > lowroom ? val - lowroom + predicted : predicted - val + highroom - 1;
                } else {
                    finalY[i] = (val & 1) != 0 ? predicted - ((val + 1) >> 1) : predicted + (val >> 1);
                }
            } else {
                step2[i] = false;
                finalY[i] = predicted;
            }
            finalY[i] = Math.max(0, Math.min(range - 1, finalY[i]));
        }
    }

    /**
     * The value at x of the line through two points (integer arithmetic of the specification).
     * <p>
     * 두 점을 지나는 직선의 x 에서의 값(규격의 정수 연산).
     *
     * @param x0 left position / 왼쪽 위치
     * @param y0 left height / 왼쪽 높이
     * @param x1 right position / 오른쪽 위치
     * @param y1 right height / 오른쪽 높이
     * @param x  position / 위치
     * @return the height / 높이
     */
    private static int renderPoint(int x0, int y0, int x1, int y1, int x) {
        int dy = y1 - y0;
        int adx = x1 - x0;
        int ady = Math.abs(dy);
        int err = ady * (x - x0);
        int off = err / adx;
        return dy < 0 ? y0 - off : y0 + off;
    }

    /**
     * Renders the curve from the reconstructed heights.
     * <p>
     * 복원한 높이로 곡선을 만든다.
     *
     * @param out  receives the curve / 곡선을 받는다
     * @param half half of the block size / 블록 크기의 절반
     */
    private void render(float[] out, int half) {
        int lx = 0;
        int ly = finalY[sorted[0]] * multiplier;
        int hx = 0;
        int hy = 0;
        // 위치 순서대로 곡선에 쓰이는 점들을 이어 선분을 그린다.
        for (int i = 1; i < values; i++) {
            int idx = sorted[i];
            if (step2[idx]) {
                hy = finalY[idx] * multiplier;
                hx = xList[idx];
                renderLine(lx, ly, hx, hy, out, half);
                lx = hx;
                ly = hy;
            }
        }
        if (hx < half) {
            renderLine(hx, hy, half, hy, out, half);
        }
    }

    /**
     * Draws a line in the integer height domain into the curve (Bresenham style, as in the specification).
     * <p>
     * 정수 높이 영역의 선을 곡선에 그린다(규격과 같은 Bresenham 방식).
     *
     * @param x0   start position / 시작 위치
     * @param y0   start height / 시작 높이
     * @param x1   end position (exclusive) / 끝 위치(미포함)
     * @param y1   end height / 끝 높이
     * @param out  the curve / 곡선
     * @param half the length of the curve / 곡선의 길이
     */
    private static void renderLine(int x0, int y0, int x1, int y1, float[] out, int half) {
        int dy = y1 - y0;
        int adx = x1 - x0;
        if (adx <= 0) {
            return;
        }
        int ady = Math.abs(dy);
        int base = dy / adx;
        int sy = dy < 0 ? base - 1 : base + 1;
        ady -= Math.abs(base) * adx;
        int x = x0;
        int y = y0;
        int err = 0;
        if (x < half) {
            out[x] = DB_TABLE[Math.max(0, Math.min(255, y))];
        }
        // 위치를 하나씩 옮기며 오차를 누적해 높이를 갱신한다.
        while (++x < x1) {
            err += ady;
            if (err >= adx) {
                err -= adx;
                y += sy;
            } else {
                y += base;
            }
            if (x < half) {
                out[x] = DB_TABLE[Math.max(0, Math.min(255, y))];
            }
        }
    }
}
