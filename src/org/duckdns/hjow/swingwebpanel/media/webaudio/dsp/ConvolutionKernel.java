package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * The frequency-domain form of one impulse response channel for a {@link PartitionedConvolver}: the impulse response is cut into
 * partitions, and the spectrum of every partition is computed once. The object is immutable after construction, so several convolvers
 * (several channels of one node) can share it.
 * <p>
 * Layout (non-uniform partitioned convolution, so that the output has no latency beyond the render quantum and a long response is cheap):
 * level 0 uses blocks of one render quantum <code>Q</code> and covers the first <code>4 Q</code> taps (4 partitions) with the current
 * input block included, so the output block needs only input that has arrived. Level <code>j</code> uses blocks of <code>Q 4^j</code>,
 * starts at the tap <code>Q 4^j</code> (never earlier than one block, which is what allows its output block to be computed from the input
 * that arrived before it) and covers three partitions, except for the last level, which uses blocks of at most
 * {@value #MAX_BLOCK} frames and covers all the remaining taps. A block of <code>B</code> frames is transformed with an FFT of
 * <code>2B</code> points (overlap-save).
 * <p>
 * {@link PartitionedConvolver} 를 위한 임펄스 응답 채널 하나의 주파수 영역 형태: 임펄스 응답을 파티션으로 자르고 파티션마다 스펙트럼을 한 번 계산한다.
 * 객체는 만든 뒤에는 불변이므로 여러 컨볼버(한 노드의 여러 채널)가 공유할 수 있다.
 * <p>
 * 배치(균일하지 않은 분할 합성곱. 출력이 render quantum 이상의 지연이 없고 긴 응답이 싸다): 레벨 0 은 render quantum <code>Q</code> 크기의 블록을 쓰고
 * 현재 입력 블록을 포함해 앞쪽 <code>4 Q</code> 탭(파티션 4개)을 맡는다. 그래서 출력 블록은 이미 도착한 입력만 있으면 계산된다. 레벨 <code>j</code> 는
 * <code>Q 4^j</code> 크기의 블록을 쓰고 탭 <code>Q 4^j</code> 에서 시작하며(블록 하나보다 앞서지 않으므로 그 출력 블록을 그 전에 도착한 입력만으로 계산할 수
 * 있다) 파티션 셋을 맡는다. 마지막 레벨만 예외로 최대 {@value #MAX_BLOCK} 프레임의 블록을 쓰고 남은 탭 전체를 맡는다. <code>B</code> 프레임의 블록은
 * <code>2B</code> 점 FFT 로 변환한다(overlap-save).
 */
public final class ConvolutionKernel {

    /**
     * The largest block of a level, in frames (the FFT then has twice as many points).
     * <p>
     * 레벨의 최대 블록 크기(프레임). 이때 FFT 는 점이 두 배이다.
     */
    public static final int MAX_BLOCK = 8192;

    /**
     * The longest impulse response that a kernel accepts, in frames (about 47 seconds at 44100 Hz): the spectra take 16 bytes per frame.
     * <p>
     * 커널이 받는 가장 긴 임펄스 응답의 프레임 수(44100 Hz 에서 약 47초). 스펙트럼은 프레임당 16바이트를 쓴다.
     */
    public static final int MAX_LENGTH = 1 << 21;

    /**
     * The block size ratio between neighbouring levels.
     * <p>
     * 이웃한 레벨 사이의 블록 크기 비.
     */
    static final int RATIO = 4;

    /**
     * One level of the partition layout.
     * <p>
     * 분할 배치의 한 레벨.
     */
    static final class Level {

        /**
         * The block size in frames.
         * <p>
         * 블록 크기(프레임).
         */
        final int block;

        /**
         * The index of the first tap that the level covers.
         * <p>
         * 레벨이 맡는 첫 탭의 인덱스.
         */
        final int offset;

        /**
         * The number of partitions.
         * <p>
         * 파티션 수.
         */
        final int partitions;

        /**
         * The real parts of the spectra of the partitions, bins <code>0 .. block</code>, already multiplied by the scale and divided by
         * the FFT size.
         * <p>
         * 파티션 스펙트럼의 실수부(빈 <code>0 .. block</code>). 크기 배율을 곱하고 FFT 크기로 나눠 둔 값이다.
         */
        final float[][] re;

        /**
         * The imaginary parts of the spectra of the partitions.
         * <p>
         * 파티션 스펙트럼의 허수부.
         */
        final float[][] im;

        /**
         * The FFT of the level, <code>2 * block</code> points.
         * <p>
         * 레벨의 FFT(<code>2 * block</code> 점).
         */
        final Fft fft;

        /**
         * Creates a level and computes the spectra.
         * <p>
         * 레벨을 만들고 스펙트럼을 계산한다.
         *
         * @param block      the block size / 블록 크기
         * @param offset     the first tap / 첫 탭
         * @param partitions the number of partitions / 파티션 수
         * @param ir         the impulse response / 임펄스 응답
         * @param length     the number of frames of the response / 응답의 프레임 수
         * @param scale      the gain applied to the response / 응답에 곱하는 이득
         */
        Level(int block, int offset, int partitions, float[] ir, int length, double scale) {
            this.block = block;
            this.offset = offset;
            this.partitions = partitions;
            this.fft = new Fft(2 * block);
            this.re = new float[partitions][block + 1];
            this.im = new float[partitions][block + 1];
            double[] tr = new double[2 * block];
            double[] ti = new double[2 * block];
            double norm = scale / (2.0 * block);
            // 파티션마다 탭을 0 으로 채운 2B 점에 넣고 변환해 0 ~ B 빈을 보관한다.
            for (int p = 0; p < partitions; p++) {
                java.util.Arrays.fill(tr, 0.0);
                java.util.Arrays.fill(ti, 0.0);
                int first = offset + p * block;
                // 파티션이 맡는 탭을 앞쪽에 복사한다.
                for (int k = 0; k < block && first + k < length; k++) {
                    tr[k] = ir[first + k];
                }
                fft.transform(tr, ti, false);
                // 크기 배율을 반영해 float 로 저장한다.
                for (int k = 0; k <= block; k++) {
                    re[p][k] = (float) (tr[k] * norm);
                    im[p][k] = (float) (ti[k] * norm);
                }
            }
        }
    }

    /**
     * The render quantum that the kernel was built for.
     * <p>
     * 커널을 만든 render quantum 크기.
     */
    private final int quantum;

    /**
     * The number of frames of the impulse response.
     * <p>
     * 임펄스 응답의 프레임 수.
     */
    private final int length;

    /**
     * The levels, from the smallest block.
     * <p>
     * 가장 작은 블록부터의 레벨.
     */
    final Level[] levels;

    /**
     * Builds the kernel of an impulse response.
     * <p>
     * 임펄스 응답의 커널을 만든다.
     *
     * @param ir      the samples of the response / 응답의 샘플
     * @param length  the number of frames of the response, from 1 to {@link #MAX_LENGTH} / 응답의 프레임 수(1 ~ {@link #MAX_LENGTH})
     * @param scale   the gain that is applied to the response / 응답에 곱하는 이득
     * @param quantum the render quantum size: a power of two / render quantum 크기. 2 의 거듭제곱
     * @throws IllegalArgumentException if an argument is out of range / 인수가 범위 밖이면
     */
    public ConvolutionKernel(float[] ir, int length, double scale, int quantum) {
        if (length < 1 || length > MAX_LENGTH || length > ir.length) {
            throw new IllegalArgumentException("length " + length);
        }
        if (quantum < 1 || (quantum & (quantum - 1)) != 0) {
            throw new IllegalArgumentException("quantum " + quantum + " is not a power of two");
        }
        this.quantum = quantum;
        this.length = length;
        java.util.ArrayList<Level> list = new java.util.ArrayList<Level>();
        int pos = 0;
        int block = quantum;
        int index = 0;
        // 응답 전체를 덮을 때까지 레벨을 쌓는다. 마지막 레벨은 남은 탭을 전부 맡는다.
        while (pos < length) {
            boolean last = block * RATIO > MAX_BLOCK;
            int need = (length - pos + block - 1) / block;
            int p = last ? need : Math.min(need, index == 0 ? RATIO : RATIO - 1);
            list.add(new Level(block, pos, p, ir, length, scale));
            pos += p * block;
            block *= RATIO;
            index++;
        }
        this.levels = list.toArray(new Level[list.size()]);
    }

    /**
     * Returns the render quantum size of the kernel.
     * <p>
     * 커널의 render quantum 크기를 반환한다.
     *
     * @return the quantum size / quantum 크기
     */
    public int getQuantum() {
        return quantum;
    }

    /**
     * Returns the number of frames of the impulse response.
     * <p>
     * 임펄스 응답의 프레임 수를 반환한다.
     *
     * @return the length / 길이
     */
    public int getLength() {
        return length;
    }

    /**
     * Returns the number of levels (for tests).
     * <p>
     * 레벨 수를 반환한다(시험용).
     *
     * @return the number of levels / 레벨 수
     */
    public int getLevelCount() {
        return levels.length;
    }
}
