package org.duckdns.hjow.swingwebpanel.media.audio.opus;

/**
 * The resampler that converts the SILK output (8, 12 or 16 kHz) to the output rate of the Opus decoder
 * (<code>silk/resampler*.c</code>): a two-times upsampler made of three all-pass sections followed by a polyphase FIR
 * interpolator. It handles the input in blocks of 10 ms and delays the input by a few samples so that the output of different
 * internal rates lines up in time with the CELT output. Integer arithmetic exactly as in the reference decoder. Only the
 * upsampling and the pass-through configurations that the decoder needs are implemented.
 * <p>
 * SILK 출력(8, 12, 16 kHz)을 Opus 디코더의 출력 주파수로 바꾸는 리샘플러(<code>silk/resampler*.c</code>): 올패스 구간 셋으로 만든 2배
 * 업샘플러 뒤에 다상 FIR 보간기를 둔다. 입력을 10 ms 블록으로 처리하고 몇 샘플 지연시켜, 내부 주파수가 달라도 출력이 CELT 출력과 시간상
 * 맞게 한다. 참조 디코더와 똑같은 정수 연산을 쓴다. 디코더에 필요한 업샘플링과 통과 구성만 구현했다.
 */
final class SilkResampler {

    /**
     * Largest batch size in milliseconds.
     * <p>
     * 한 번에 처리하는 최대 길이(밀리초).
     */
    private static final int MAX_BATCH_SIZE_MS = 10;

    /**
     * Order of the interpolating FIR filter.
     * <p>
     * 보간 FIR 필터의 차수.
     */
    private static final int ORDER_FIR_12 = 8;

    /**
     * Delay of the input in samples per (input rate, output rate) pair of the decoder: rows 8, 12 and 16 kHz; columns 8, 12, 16, 24 and 48 kHz.
     * <p>
     * 디코더의 (입력 주파수, 출력 주파수) 쌍별 입력 지연(샘플): 행은 8, 12, 16 kHz, 열은 8, 12, 16, 24, 48 kHz.
     */
    private static final int[][] DELAY_MATRIX_DEC = { { 4, 0, 2, 0, 0 }, { 0, 9, 4, 7, 4 }, { 0, 3, 12, 7, 7 } };

    /**
     * Coefficients of the three all-pass sections of the first output phase.
     * <p>
     * 첫 출력 위상의 올패스 구간 세 개의 계수.
     */
    private static final int[] UP2_HQ_0 = { 1746, 14986, 39083 - 65536 };

    /**
     * Coefficients of the three all-pass sections of the second output phase.
     * <p>
     * 둘째 출력 위상의 올패스 구간 세 개의 계수.
     */
    private static final int[] UP2_HQ_1 = { 6854, 25769, 55542 - 65536 };

    /**
     * The polyphase interpolation filters, 12 phases of 4 taps (half of the symmetric 8-tap filter).
     * <p>
     * 다상 보간 필터: 4탭 12위상(대칭 8탭 필터의 절반).
     */
    private static final int[] FRAC_FIR_12 = { 189, -600, 617, 30567, 117, -159, -1070, 29704, 52, 221, -2392, 28276, -4, 529, -3350, 26341, -48, 758,
            -3956, 23973, -80, 905, -4235, 21254, -99, 972, -4222, 18278, -107, 967, -3957, 15143, -103, 896, -3487, 11950, -91, 773, -2865, 8798, -71,
            611, -2143, 5784, -46, 425, -1375, 2996 };

    /**
     * State of the all-pass sections.
     * <p>
     * 올패스 구간의 상태.
     */
    private final int[] sIir = new int[6];

    /**
     * History of the FIR filter (16 values are kept as in the reference, only the first 8 matter).
     * <p>
     * FIR 필터의 이력(참조 디코더처럼 16개를 보관하나 앞의 8개만 쓰인다).
     */
    private final short[] sFir = new short[16];

    /**
     * Samples held back from the previous call.
     * <p>
     * 이전 호출에서 지연시켜 둔 샘플.
     */
    private final short[] delayBuf = new short[48];

    /**
     * Resampling method: 0 copy, 2 upsampling by an interpolating FIR.
     * <p>
     * 리샘플링 방식: 0 은 복사, 2 는 보간 FIR 업샘플링.
     */
    private int function;

    /**
     * Number of input samples per batch.
     * <p>
     * 한 번에 처리하는 입력 샘플 수.
     */
    private int batchSize;

    /**
     * Input step per output sample in Q16.
     * <p>
     * Q16 출력 샘플당 입력 이동량.
     */
    private int invRatioQ16;

    /**
     * Input rate in kHz.
     * <p>
     * 입력 주파수(kHz).
     */
    private int fsInKHz;

    /**
     * Output rate in kHz.
     * <p>
     * 출력 주파수(kHz).
     */
    private int fsOutKHz;

    /**
     * Input delay in samples.
     * <p>
     * 입력 지연(샘플).
     */
    private int inputDelay;

    /**
     * Creates an uninitialized resampler.
     * <p>
     * 초기화되지 않은 리샘플러를 만든다.
     */
    SilkResampler() {
    }

    /**
     * Copies the complete state of another resampler.
     * <p>
     * 다른 리샘플러의 상태를 모두 복사한다.
     *
     * @param o the source / 원본
     */
    void copyFrom(SilkResampler o) {
        System.arraycopy(o.sIir, 0, sIir, 0, sIir.length);
        System.arraycopy(o.sFir, 0, sFir, 0, sFir.length);
        System.arraycopy(o.delayBuf, 0, delayBuf, 0, delayBuf.length);
        function = o.function;
        batchSize = o.batchSize;
        invRatioQ16 = o.invRatioQ16;
        fsInKHz = o.fsInKHz;
        fsOutKHz = o.fsOutKHz;
        inputDelay = o.inputDelay;
    }

    /**
     * Index of a sampling rate in the delay matrix.
     * <p>
     * 지연 행렬에서 주파수의 번호.
     *
     * @param r rate in Hz / 주파수(Hz)
     * @return the index / 번호
     */
    private static int rateId(int r) {
        return ((((r >> 12) - (r > 16000 ? 1 : 0)) >> (r > 24000 ? 1 : 0)) - 1);
    }

    /**
     * Initializes the resampler and clears its state.
     * <p>
     * 리샘플러를 초기화하고 상태를 지운다.
     *
     * @param fsHzIn  input rate: 8000, 12000 or 16000 / 입력 주파수(8000, 12000, 16000)
     * @param fsHzOut output rate: 8000, 12000, 16000, 24000 or 48000 (not below the input rate) / 출력 주파수(입력보다 낮지 않아야 함)
     * @return 0 on success, -1 for an unsupported combination / 성공이면 0, 지원하지 않는 조합이면 -1
     */
    int init(int fsHzIn, int fsHzOut) {
        java.util.Arrays.fill(sIir, 0);
        java.util.Arrays.fill(sFir, (short) 0);
        java.util.Arrays.fill(delayBuf, (short) 0);
        if ((fsHzIn != 8000 && fsHzIn != 12000 && fsHzIn != 16000)
                || (fsHzOut != 8000 && fsHzOut != 12000 && fsHzOut != 16000 && fsHzOut != 24000 && fsHzOut != 48000)) {
            return -1;
        }
        inputDelay = DELAY_MATRIX_DEC[rateId(fsHzIn)][rateId(fsHzOut)];
        fsInKHz = fsHzIn / 1000;
        fsOutKHz = fsHzOut / 1000;
        batchSize = fsInKHz * MAX_BATCH_SIZE_MS;
        int up2x = 0;
        if (fsHzOut > fsHzIn) {
            if (fsHzOut == fsHzIn * 2) {
                return -1;
            }
            function = 2;
            up2x = 1;
        } else if (fsHzOut < fsHzIn) {
            return -1;
        } else {
            function = 0;
        }
        invRatioQ16 = ((fsHzIn << (14 + up2x)) / fsHzOut) << 2;
        // 비율을 올림하여 출력 샘플 수가 모자라지 않게 한다.
        while (SilkMath.smulww(invRatioQ16, fsHzOut) < (fsHzIn << up2x)) {
            invRatioQ16++;
        }
        return 0;
    }

    /**
     * Resamples one frame (at least 1 ms).
     * <p>
     * 프레임 하나(1 ms 이상)를 리샘플링한다.
     *
     * @param out   receives the output samples / 출력 샘플을 받는 배열
     * @param outOff start in <code>out</code> / <code>out</code> 의 시작
     * @param in    the input samples / 입력 샘플
     * @param inOff start in <code>in</code> / <code>in</code> 의 시작
     * @param inLen number of input samples / 입력 샘플 수
     */
    void resample(short[] out, int outOff, short[] in, int inOff, int inLen) {
        int nSamples = fsInKHz - inputDelay;
        System.arraycopy(in, inOff, delayBuf, inputDelay, nSamples);
        if (function == 2) {
            iirFir(out, outOff, delayBuf, 0, fsInKHz);
            iirFir(out, outOff + fsOutKHz, in, inOff + nSamples, inLen - fsInKHz);
        } else {
            System.arraycopy(delayBuf, 0, out, outOff, fsInKHz);
            System.arraycopy(in, inOff + nSamples, out, outOff + fsOutKHz, inLen - fsInKHz);
        }
        System.arraycopy(in, inOff + inLen - inputDelay, delayBuf, 0, inputDelay);
    }

    /**
     * Two-times upsampling with three all-pass sections per phase.
     * <p>
     * 위상마다 올패스 구간 셋을 쓰는 2배 업샘플링.
     *
     * @param out    receives 2 * len samples / 샘플 2 * len 개를 받는 배열
     * @param outOff start in <code>out</code> / <code>out</code> 의 시작
     * @param in     the input / 입력
     * @param inOff  start in <code>in</code> / <code>in</code> 의 시작
     * @param len    number of input samples / 입력 샘플 수
     */
    private void up2Hq(short[] out, int outOff, short[] in, int inOff, int len) {
        int[] s = sIir;
        // 입력 샘플마다 출력 샘플 둘을 만든다.
        for (int k = 0; k < len; k++) {
            int in32 = in[inOff + k] << 10;
            int y = in32 - s[0];
            int x = SilkMath.smulwb(y, UP2_HQ_0[0]);
            int out1 = s[0] + x;
            s[0] = in32 + x;
            y = out1 - s[1];
            x = SilkMath.smulwb(y, UP2_HQ_0[1]);
            int out2 = s[1] + x;
            s[1] = out1 + x;
            y = out2 - s[2];
            x = SilkMath.smlawb(y, y, UP2_HQ_0[2]);
            out1 = s[2] + x;
            s[2] = out2 + x;
            out[outOff + 2 * k] = (short) SilkMath.sat16(SilkMath.rshiftRound(out1, 10));
            y = in32 - s[3];
            x = SilkMath.smulwb(y, UP2_HQ_1[0]);
            out1 = s[3] + x;
            s[3] = in32 + x;
            y = out1 - s[4];
            x = SilkMath.smulwb(y, UP2_HQ_1[1]);
            out2 = s[4] + x;
            s[4] = out1 + x;
            y = out2 - s[5];
            x = SilkMath.smlawb(y, y, UP2_HQ_1[2]);
            out1 = s[5] + x;
            s[5] = out2 + x;
            out[outOff + 2 * k + 1] = (short) SilkMath.sat16(SilkMath.rshiftRound(out1, 10));
        }
    }

    /**
     * Upsampling by two followed by polyphase interpolation to the output rate.
     * <p>
     * 2배 업샘플링한 뒤 다상 보간으로 출력 주파수에 맞춘다.
     *
     * @param out    receives the output / 출력을 받는 배열
     * @param outOff start in <code>out</code> / <code>out</code> 의 시작
     * @param in     the input / 입력
     * @param inOff  start in <code>in</code> / <code>in</code> 의 시작
     * @param inLenIn number of input samples / 입력 샘플 수
     */
    private void iirFir(short[] out, int outOff, short[] in, int inOff, int inLenIn) {
        int inLen = inLenIn;
        int inPos = inOff;
        int outPos = outOff;
        short[] buf = new short[480 + ORDER_FIR_12 + 16];
        System.arraycopy(sFir, 0, buf, 0, 16);
        int nSamplesIn;
        // 입력을 배치 크기로 잘라 처리한다.
        while (true) {
            nSamplesIn = Math.min(inLen, batchSize);
            up2Hq(buf, ORDER_FIR_12, in, inPos, nSamplesIn);
            int maxIndexQ16 = nSamplesIn << (16 + 1);
            // 출력 샘플마다 보간 위치를 계산해 8탭 FIR 로 보간한다.
            for (int indexQ16 = 0; indexQ16 < maxIndexQ16; indexQ16 += invRatioQ16) {
                int tableIndex = SilkMath.smulwb(indexQ16 & 0xFFFF, 12);
                int bp = indexQ16 >> 16;
                int res = SilkMath.smulbb(buf[bp], FRAC_FIR_12[4 * tableIndex]);
                res = SilkMath.smlabb(res, buf[bp + 1], FRAC_FIR_12[4 * tableIndex + 1]);
                res = SilkMath.smlabb(res, buf[bp + 2], FRAC_FIR_12[4 * tableIndex + 2]);
                res = SilkMath.smlabb(res, buf[bp + 3], FRAC_FIR_12[4 * tableIndex + 3]);
                res = SilkMath.smlabb(res, buf[bp + 4], FRAC_FIR_12[4 * (11 - tableIndex) + 3]);
                res = SilkMath.smlabb(res, buf[bp + 5], FRAC_FIR_12[4 * (11 - tableIndex) + 2]);
                res = SilkMath.smlabb(res, buf[bp + 6], FRAC_FIR_12[4 * (11 - tableIndex) + 1]);
                res = SilkMath.smlabb(res, buf[bp + 7], FRAC_FIR_12[4 * (11 - tableIndex)]);
                out[outPos++] = (short) SilkMath.sat16(SilkMath.rshiftRound(res, 15));
            }
            inPos += nSamplesIn;
            inLen -= nSamplesIn;
            if (inLen > 0) {
                System.arraycopy(buf, nSamplesIn << 1, buf, 0, 16);
            } else {
                break;
            }
        }
        System.arraycopy(buf, nSamplesIn << 1, sFir, 0, 16);
    }
}
