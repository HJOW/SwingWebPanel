package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * Convolves one input channel with one impulse response channel in blocks of a render quantum, without any latency and with the cost of
 * a partitioned FFT convolution (see {@link ConvolutionKernel} for the layout). The object keeps the input history and the frequency
 * domain delay lines of the levels; the kernel with the spectra of the response is shared and is not changed.
 * <p>
 * At every quantum the new input block is appended to the history. Level 0 transforms the last <code>2Q</code> input frames (the
 * current block included), multiplies the spectra with the spectra of its partitions and adds them over the delay line. A level with a
 * block of <code>B &gt; Q</code> frames does that only when a block of <code>B</code> frames starts: it transforms the last
 * <code>2B</code> frames that arrived before the block (the response of the level starts at least one block late, so the block does not
 * depend on later input) and delivers the result block of <code>B</code> frames in pieces of one quantum.
 * <p>
 * 입력 채널 하나를 임펄스 응답 채널 하나와 render quantum 블록 단위로, 지연 없이, 분할 FFT 합성곱의 비용으로 합성곱한다(배치는
 * {@link ConvolutionKernel} 참고). 객체는 입력 이력과 레벨의 주파수 영역 지연선을 가지며, 응답의 스펙트럼을 가진 커널은 공유되고 바뀌지 않는다.
 * <p>
 * quantum 마다 새 입력 블록을 이력에 덧붙인다. 레벨 0 은 마지막 <code>2Q</code> 입력 프레임(현재 블록 포함)을 변환해 파티션 스펙트럼과 곱하고 지연선 위에서
 * 더한다. 블록이 <code>B &gt; Q</code> 프레임인 레벨은 <code>B</code> 프레임의 블록이 시작될 때만 그렇게 한다. 블록보다 앞에 도착한 마지막 <code>2B</code>
 * 프레임을 변환하고(레벨의 응답이 블록 하나 이상 늦게 시작하므로 블록은 이후 입력에 의존하지 않는다) <code>B</code> 프레임의 결과 블록을 quantum 한
 * 조각씩 내보낸다.
 */
public final class PartitionedConvolver {

    /**
     * The state of one level.
     * <p>
     * 레벨 하나의 상태.
     */
    private static final class LevelState {

        /**
         * The level of the kernel.
         * <p>
         * 커널의 레벨.
         */
        final ConvolutionKernel.Level level;

        /**
         * The real parts of the frequency domain delay line: one spectrum of <code>block + 1</code> bins per partition.
         * <p>
         * 주파수 영역 지연선의 실수부: 파티션마다 <code>block + 1</code> 빈의 스펙트럼 하나.
         */
        final double[][] lineRe;

        /**
         * The imaginary parts of the delay line.
         * <p>
         * 지연선의 허수부.
         */
        final double[][] lineIm;

        /**
         * The index of the newest spectrum in the delay line.
         * <p>
         * 지연선에서 가장 새 스펙트럼의 인덱스.
         */
        int head;

        /**
         * The result block of the level (only for levels with a block larger than the quantum).
         * <p>
         * 레벨의 결과 블록(블록이 quantum 보다 큰 레벨만).
         */
        final double[] result;

        /**
         * The accumulated product, real part.
         * <p>
         * 누적한 곱의 실수부.
         */
        final double[] accRe;

        /**
         * The accumulated product, imaginary part.
         * <p>
         * 누적한 곱의 허수부.
         */
        final double[] accIm;

        /**
         * Creates the state of a level.
         * <p>
         * 레벨의 상태를 만든다.
         *
         * @param level the level / 레벨
         */
        LevelState(ConvolutionKernel.Level level) {
            this.level = level;
            this.lineRe = new double[level.partitions][level.block + 1];
            this.lineIm = new double[level.partitions][level.block + 1];
            this.result = new double[level.block];
            this.accRe = new double[level.block + 1];
            this.accIm = new double[level.block + 1];
        }
    }

    /**
     * The kernel.
     * <p>
     * 커널.
     */
    private final ConvolutionKernel kernel;

    /**
     * The render quantum size.
     * <p>
     * render quantum 크기.
     */
    private final int quantum;

    /**
     * The states of the levels.
     * <p>
     * 레벨의 상태.
     */
    private final LevelState[] states;

    /**
     * The circular input history: four times the largest block (two blocks of the segment plus the newest quantum, rounded up to a power of two).
     * <p>
     * 원형 입력 이력: 가장 큰 블록의 네 배(구간의 두 블록과 가장 새 quantum 을 2 의 거듭제곱으로 올림한 크기).
     */
    private final float[] history;

    /**
     * The mask of the history indices.
     * <p>
     * 이력 인덱스의 마스크.
     */
    private final int mask;

    /**
     * The number of input frames that were processed since the last reset.
     * <p>
     * 마지막 초기화 뒤에 처리한 입력 프레임 수.
     */
    private long count;

    /**
     * The scratch arrays of the transforms, real and imaginary part.
     * <p>
     * 변환의 작업 배열(실수부, 허수부).
     */
    private final double[] tr;

    /**
     * The scratch array of the transforms, imaginary part.
     * <p>
     * 변환의 작업 배열(허수부).
     */
    private final double[] ti;

    /**
     * The sum of the results of the levels for the quantum.
     * <p>
     * quantum 의 레벨별 결과 합.
     */
    private final double[] sum;

    /**
     * Creates a convolver with an empty state.
     * <p>
     * 상태가 빈 컨볼버를 만든다.
     *
     * @param kernel the kernel of the impulse response / 임펄스 응답의 커널
     */
    public PartitionedConvolver(ConvolutionKernel kernel) {
        this.kernel = kernel;
        this.quantum = kernel.getQuantum();
        this.states = new LevelState[kernel.levels.length];
        int largest = quantum;
        // 레벨마다 상태를 만들고 가장 큰 블록을 찾는다.
        for (int j = 0; j < states.length; j++) {
            states[j] = new LevelState(kernel.levels[j]);
            largest = Math.max(largest, kernel.levels[j].block);
        }
        this.history = new float[4 * largest];
        this.mask = history.length - 1;
        this.tr = new double[2 * largest];
        this.ti = new double[2 * largest];
        this.sum = new double[quantum];
    }

    /**
     * Returns the kernel.
     * <p>
     * 커널을 반환한다.
     *
     * @return the kernel / 커널
     */
    public ConvolutionKernel getKernel() {
        return kernel;
    }

    /**
     * Clears the input history and the delay lines.
     * <p>
     * 입력 이력과 지연선을 지운다.
     */
    public void reset() {
        java.util.Arrays.fill(history, 0f);
        count = 0;
        // 레벨마다 지연선과 결과 블록을 지운다.
        for (int j = 0; j < states.length; j++) {
            LevelState s = states[j];
            // 파티션마다 지연선의 스펙트럼을 0 으로 채운다.
            for (int p = 0; p < s.lineRe.length; p++) {
                java.util.Arrays.fill(s.lineRe[p], 0.0);
                java.util.Arrays.fill(s.lineIm[p], 0.0);
            }
            java.util.Arrays.fill(s.result, 0.0);
            s.head = 0;
        }
    }

    /**
     * Convolves one quantum. The input and the output may be the same array.
     * <p>
     * quantum 하나를 합성곱한다. 입력과 출력은 같은 배열이어도 된다.
     *
     * @param in     the input block, of exactly one quantum / 입력 블록(정확히 quantum 하나)
     * @param out    the output block / 출력 블록
     * @param frames the number of frames, which must be the render quantum size / 프레임 수. render quantum 크기와 같아야 한다
     * @throws IllegalArgumentException if the number of frames is not the quantum size / 프레임 수가 quantum 크기가 아니면
     */
    public void process(float[] in, float[] out, int frames) {
        if (frames != quantum) {
            throw new IllegalArgumentException("frames " + frames + " is not the quantum " + quantum);
        }
        long blockStart = count;
        // 새 입력 블록을 이력에 덧붙인다.
        for (int i = 0; i < frames; i++) {
            history[(int) ((blockStart + i) & mask)] = in[i];
        }
        count += frames;
        java.util.Arrays.fill(sum, 0.0);
        // 레벨마다 이번 quantum 의 결과를 합에 더한다.
        for (int j = 0; j < states.length; j++) {
            LevelState s = states[j];
            int block = s.level.block;
            if (block == quantum && s.level.offset == 0) {
                // 레벨 0 은 현재 블록을 포함한 입력 2B 프레임을 변환해 이번 quantum 의 결과를 바로 만든다.
                transformInput(s, count);
                multiplyAndInvert(s);
                // 결과 블록의 뒤쪽 B 점이 유효한 출력이다.
                for (int i = 0; i < block; i++) {
                    sum[i] += tr[block + i];
                }
            } else {
                // 큰 블록 레벨은 블록이 시작할 때만 새로 계산하고, 그 사이에는 결과 블록의 해당 위치를 내보낸다.
                if ((blockStart & (block - 1)) == 0) {
                    transformInput(s, blockStart);
                    multiplyAndInvert(s);
                    // 결과 블록의 뒤쪽 B 점을 보관한다.
                    for (int i = 0; i < block; i++) {
                        s.result[i] = tr[block + i];
                    }
                }
                int offset = (int) (blockStart & (block - 1));
                // 결과 블록에서 이번 quantum 에 해당하는 조각을 더한다.
                for (int i = 0; i < frames; i++) {
                    sum[i] += s.result[offset + i];
                }
            }
        }
        // 합을 float 출력으로 옮긴다.
        for (int i = 0; i < frames; i++) {
            out[i] = (float) sum[i];
        }
    }

    /**
     * Transforms the last <code>2B</code> input frames before the position <code>end</code> and pushes the spectrum into the delay line
     * of the level.
     * <p>
     * 위치 <code>end</code> 앞의 마지막 <code>2B</code> 입력 프레임을 변환해 레벨의 지연선에 스펙트럼을 넣는다.
     *
     * @param s   the level state / 레벨 상태
     * @param end the number of input frames that the segment ends at / 구간이 끝나는 입력 프레임 수
     */
    private void transformInput(LevelState s, long end) {
        int block = s.level.block;
        int size = 2 * block;
        // 구간의 입력을 변환 배열에 복사한다(아직 도착하지 않은 앞쪽은 0 이다).
        for (int i = 0; i < size; i++) {
            long idx = end - size + i;
            tr[i] = idx < 0 ? 0.0 : history[(int) (idx & mask)];
            ti[i] = 0.0;
        }
        s.level.fft.transform(tr, ti, false);
        int partitions = s.level.partitions;
        s.head = (s.head - 1 + partitions) % partitions;
        double[] re = s.lineRe[s.head];
        double[] im = s.lineIm[s.head];
        // 0 ~ B 빈을 지연선의 가장 새 위치에 보관한다.
        for (int k = 0; k <= block; k++) {
            re[k] = tr[k];
            im[k] = ti[k];
        }
    }

    /**
     * Multiplies the delay line with the spectra of the partitions, sums the products and transforms the sum back. The real result is
     * left in the first <code>2B</code> elements of <code>tr</code>.
     * <p>
     * 지연선에 파티션 스펙트럼을 곱하고 곱을 더한 뒤 역변환한다. 실수 결과는 <code>tr</code> 의 앞 <code>2B</code> 요소에 남는다.
     *
     * @param s the level state / 레벨 상태
     */
    private void multiplyAndInvert(LevelState s) {
        ConvolutionKernel.Level lv = s.level;
        int block = lv.block;
        java.util.Arrays.fill(s.accRe, 0.0);
        java.util.Arrays.fill(s.accIm, 0.0);
        int partitions = lv.partitions;
        // 파티션마다 지연선의 해당 스펙트럼과 곱해 누적한다.
        for (int p = 0; p < partitions; p++) {
            int slot = s.head + p;
            if (slot >= partitions) {
                slot -= partitions;
            }
            double[] xr = s.lineRe[slot];
            double[] xi = s.lineIm[slot];
            float[] hr = lv.re[p];
            float[] hi = lv.im[p];
            double[] ar = s.accRe;
            double[] ai = s.accIm;
            // 빈마다 복소수 곱을 더한다.
            for (int k = 0; k <= block; k++) {
                ar[k] += hr[k] * xr[k] - hi[k] * xi[k];
                ai[k] += hr[k] * xi[k] + hi[k] * xr[k];
            }
        }
        int size = 2 * block;
        // 0 ~ B 빈은 그대로, B+1 ~ 2B-1 빈은 켤레 대칭으로 채운다.
        for (int k = 0; k <= block; k++) {
            tr[k] = s.accRe[k];
            ti[k] = s.accIm[k];
        }
        // 나머지 빈은 켤레 대칭으로 채운다.
        for (int k = block + 1; k < size; k++) {
            tr[k] = s.accRe[size - k];
            ti[k] = -s.accIm[size - k];
        }
        lv.fft.transform(tr, ti, true);
    }
}
