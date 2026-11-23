package org.duckdns.hjow.swingwebpanel.media.webaudio.dsp;

/**
 * Renders a mono source into the two ears with the HRTF of a direction: the signal is convolved with the left and the right ear
 * kernels of {@link HrtfSet} (one FIR filter per ear, with the interaural delay inside the kernel). When the direction changes from one
 * render quantum to the next the output of the old kernels is cross-faded into the output of the new kernels over the quantum, so a
 * moving source makes no clicks; the gain (the distance and cone attenuation) is ramped linearly over the quantum for the same reason.
 * The object keeps the input history, so one renderer belongs to one source.
 * <p>
 * 단일 채널 원천을 방향의 HRTF 로 두 귀에 렌더링한다. 신호를 {@link HrtfSet} 의 왼쪽과 오른쪽 귀 커널로 합성곱한다(귀마다 FIR 필터 하나이며 귀 사이의 지연은
 * 커널 안에 있다). render quantum 사이에 방향이 바뀌면 이전 커널의 출력을 quantum 동안 새 커널의 출력으로 크로스페이드하므로 움직이는 음원이 딸깍 소리를 내지
 * 않는다. 같은 이유로 게인(거리와 원뿔 감쇠)도 quantum 동안 선형으로 바뀐다. 객체가 입력 이력을 가지므로 렌더러 하나는 음원 하나에 속한다.
 */
public final class HrtfRenderer {

    /**
     * The set of kernels.
     * <p>
     * 커널 세트.
     */
    private final HrtfSet set;

    /**
     * The length of the kernels.
     * <p>
     * 커널 길이.
     */
    private final int length;

    /**
     * The input history followed by the current block.
     * <p>
     * 입력 이력과 그 뒤의 현재 블록.
     */
    private float[] buf;

    /**
     * The kernels that were used in the last quantum (left, right) and the kernels that are computed for the new direction.
     * <p>
     * 지난 quantum 에 쓴 커널(왼쪽, 오른쪽)과 새 방향에 대해 계산하는 커널.
     */
    private float[] usedL;

    /**
     * The right ear kernel that was used in the last quantum.
     * <p>
     * 지난 quantum 에 쓴 오른쪽 귀 커널.
     */
    private float[] usedR;

    /**
     * The left ear kernel of the new direction.
     * <p>
     * 새 방향의 왼쪽 귀 커널.
     */
    private float[] nextL;

    /**
     * The right ear kernel of the new direction.
     * <p>
     * 새 방향의 오른쪽 귀 커널.
     */
    private float[] nextR;

    /**
     * The work arrays of the interpolation of a direction.
     * <p>
     * 방향 보간의 작업 배열.
     */
    private final float[] workL;

    /**
     * The second work array of the interpolation.
     * <p>
     * 보간의 두 번째 작업 배열.
     */
    private final float[] workR;

    /**
     * The output of the old kernels during a cross-fade, left and right.
     * <p>
     * 크로스페이드 동안 이전 커널의 출력(왼쪽, 오른쪽).
     */
    private float[] oldL = new float[0];

    /**
     * The output of the old kernels during a cross-fade, right.
     * <p>
     * 크로스페이드 동안 이전 커널의 출력(오른쪽).
     */
    private float[] oldR = new float[0];

    /**
     * True once a direction was rendered.
     * <p>
     * 방향을 한 번이라도 렌더링했으면 true.
     */
    private boolean started;

    /**
     * The azimuth that the used kernels belong to.
     * <p>
     * 쓴 커널이 속한 방위각.
     */
    private double lastAzimuth;

    /**
     * The elevation that the used kernels belong to.
     * <p>
     * 쓴 커널이 속한 고도.
     */
    private double lastElevation;

    /**
     * The gain of the last quantum.
     * <p>
     * 지난 quantum 의 게인.
     */
    private double lastGain = 1.0;

    /**
     * Creates a renderer for a set of kernels.
     * <p>
     * 커널 세트의 렌더러를 만든다.
     *
     * @param set     the kernels / 커널
     * @param quantum the largest number of frames of a call / 호출 하나의 최대 프레임 수
     */
    public HrtfRenderer(HrtfSet set, int quantum) {
        this.set = set;
        this.length = set.getKernelLength();
        this.buf = new float[length - 1 + quantum];
        this.usedL = new float[length];
        this.usedR = new float[length];
        this.nextL = new float[length];
        this.nextR = new float[length];
        this.workL = new float[length];
        this.workR = new float[length];
    }

    /**
     * Returns the number of frames that the output lasts after the input became silent: the length of the kernels.
     * <p>
     * 입력이 무음이 된 뒤 출력이 이어지는 프레임 수(커널 길이)를 반환한다.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    public int getTailFrames() {
        return length;
    }

    /**
     * Clears the input history and forgets the direction (the next call starts a new direction without a cross-fade).
     * <p>
     * 입력 이력을 지우고 방향을 잊는다(다음 호출은 크로스페이드 없이 새 방향으로 시작한다).
     */
    public void reset() {
        java.util.Arrays.fill(buf, 0f);
        started = false;
        lastGain = 1.0;
    }

    /**
     * Renders a block.
     * <p>
     * 블록을 렌더링한다.
     *
     * @param mono      the input / 입력
     * @param frames    the number of frames / 프레임 수
     * @param azimuth   the azimuth in degrees (positive to the right) / 방위각(도, 오른쪽이 양수)
     * @param elevation the elevation in degrees / 고도(도)
     * @param gain      the gain of the distance and the cone / 거리와 원뿔의 게인
     * @param outL      the left output / 왼쪽 출력
     * @param outR      the right output / 오른쪽 출력
     */
    public void process(float[] mono, int frames, double azimuth, double elevation, double gain, float[] outL, float[] outR) {
        if (buf.length < length - 1 + frames) {
            float[] bigger = new float[length - 1 + frames];
            System.arraycopy(buf, 0, bigger, 0, length - 1);
            buf = bigger;
        }
        System.arraycopy(mono, 0, buf, length - 1, frames);
        boolean crossfade = false;
        if (!started) {
            // 처음에는 방향의 커널을 바로 쓴다.
            set.direction(azimuth, elevation, usedL, usedR, workL, workR);
            lastAzimuth = azimuth;
            lastElevation = elevation;
            lastGain = gain;
            started = true;
        } else if (azimuth != lastAzimuth || elevation != lastElevation) {
            // 방향이 바뀌었으면 새 커널을 계산하고 이전 커널의 출력과 크로스페이드한다.
            set.direction(azimuth, elevation, nextL, nextR, workL, workR);
            crossfade = true;
        }
        if (crossfade) {
            if (oldL.length < frames) {
                oldL = new float[frames];
                oldR = new float[frames];
            }
            fir(usedL, frames, oldL);
            fir(usedR, frames, oldR);
            fir(nextL, frames, outL);
            fir(nextR, frames, outR);
            // 프레임마다 가중치를 0 에서 1 로 올리며 두 출력을 섞는다.
            for (int n = 0; n < frames; n++) {
                float w = (n + 1) / (float) frames;
                outL[n] = (1f - w) * oldL[n] + w * outL[n];
                outR[n] = (1f - w) * oldR[n] + w * outR[n];
            }
            float[] t = usedL;
            usedL = nextL;
            nextL = t;
            t = usedR;
            usedR = nextR;
            nextR = t;
            lastAzimuth = azimuth;
            lastElevation = elevation;
        } else {
            fir(usedL, frames, outL);
            fir(usedR, frames, outR);
        }
        // 게인을 지난 quantum 의 값에서 새 값으로 선형으로 바꿔 곱한다.
        for (int n = 0; n < frames; n++) {
            float g = (float) (lastGain + (gain - lastGain) * (n + 1) / frames);
            outL[n] *= g;
            outR[n] *= g;
        }
        lastGain = gain;
        System.arraycopy(buf, frames, buf, 0, length - 1);
    }

    /**
     * Filters the current block with a kernel: <code>out[n] = sum_k kernel[k] x[n - k]</code>.
     * <p>
     * 현재 블록을 커널로 필터링한다: <code>out[n] = sum_k kernel[k] x[n - k]</code>.
     *
     * @param kernel the kernel / 커널
     * @param frames the number of frames / 프레임 수
     * @param out    the output / 출력
     */
    private void fir(float[] kernel, int frames, float[] out) {
        // 출력 프레임마다 커널과 입력 이력의 곱을 더한다.
        for (int n = 0; n < frames; n++) {
            double s = 0.0;
            int base = length - 1 + n;
            // 커널 탭마다 지연된 입력과 곱해 더한다.
            for (int k = 0; k < length; k++) {
                s += kernel[k] * buf[base - k];
            }
            out[n] = (float) s;
        }
    }
}
