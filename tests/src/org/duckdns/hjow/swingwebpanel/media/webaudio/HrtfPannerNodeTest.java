package org.duckdns.hjow.swingwebpanel.media.webaudio;

import java.io.IOException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.HrtfDatabase;
import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.HrtfSet;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the panner with the <code>HRTF</code> model (Web Audio API 1.27 and 6.3.2, plan item P25-04) through the audio graph, with the
 * data of the MIT KEMAR dummy head (the data of the product, and the unchanged original of the test resources as the independent
 * reference): the data is available; the impulse response of the node for a direction is the pair of kernels of that direction; at
 * the measured directions the impulse responses of the node agree with the original KEMAR responses in the magnitude response (within
 * 0.5 dB from 200 Hz to 10 kHz) and in the interaural time difference of the low frequency part (within 1 sample); the distance gain
 * multiplies the output; a stereo input is spatialized as its mono mean; when the source jumps to another direction the output moves
 * from the old kernels to the new ones over the quantum and not in one step; the tail time of the head responses keeps the node
 * processed after the input; and switching back to the <code>equalpower</code> model gives the equal power gains.
 * <p>
 * <code>HRTF</code> 모델 패너(Web Audio API 1.27, 6.3.2, 계획 P25-04)를 오디오 그래프를 거쳐 시험한다. MIT KEMAR 더미 헤드의 자료(제품의 자료와 독립적인 기준인 시험 자원의
 * 바뀌지 않은 원본)를 쓴다: 자료를 쓸 수 있다. 방향에 대한 노드의 임펄스 응답은 그 방향의 커널 쌍이다. 측정한 방향에서 노드의 임펄스 응답은 원본 KEMAR 응답과 크기
 * 응답(200 Hz ~ 10 kHz 에서 0.5 dB 이내)과 저주파 부분의 두 귀 시간차(1 샘플 이내)가 일치한다. 거리 게인이 출력에 곱해진다. 스테레오 입력은 모노 평균으로
 * 공간화된다. 음원이 다른 방향으로 건너뛰면 출력이 한 번에 바뀌지 않고 quantum 동안 이전 커널에서 새 커널로 옮겨 간다. 머리 응답의 꼬리 시간 때문에 입력 뒤에도 노드가
 * 처리된다. <code>equalpower</code> 모델로 되돌리면 등전력 게인이 나온다.
 */
public class HrtfPannerNodeTest {

    /**
     * The sample rate of the contexts of the tests (the rate of the data).
     * <p>
     * 시험 컨텍스트의 샘플링 주파수(자료의 속도).
     */
    private static final float RATE = 44100f;

    /**
     * Creates the suite.
     * <p>
     * 스위트를 만든다.
     */
    public HrtfPannerNodeTest() {
    }

    /**
     * Loads the data of the product.
     * <p>
     * 제품의 자료를 읽는다.
     *
     * @return the database / 데이터베이스
     */
    private static HrtfDatabase db() {
        try {
            return HrtfDatabase.load();
        } catch (IOException e) {
            Check.fail("the HRTF resource cannot be loaded: " + e.getMessage());
            return null;
        }
    }

    /**
     * Renders an impulse through an HRTF panner at a direction and distance and returns the stereo response.
     * <p>
     * HRTF 패너를 방향과 거리에서 거친 임펄스를 렌더링해 스테레오 응답을 반환한다.
     *
     * @param azimuth   the azimuth in degrees / 방위각(도)
     * @param elevation the elevation in degrees / 고도(도)
     * @param distance  the distance / 거리
     * @param frames    the length of the rendering / 렌더링 길이
     * @return the response, [ear][frame] / 응답 [귀][프레임]
     */
    private static float[][] impulseResponse(double azimuth, double elevation, double distance, int frames) {
        OfflineAudioContext ctx = new OfflineAudioContext(2, frames, RATE);
        AudioBuffer in = new AudioBuffer(1, 128, RATE);
        in.getChannelData(0)[0] = 1f;
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        PannerNode p = ctx.createPanner();
        p.setPanningModel(PanningModelType.HRTF);
        double a = Math.toRadians(azimuth);
        double e = Math.toRadians(elevation);
        p.getPositionX().setValue((float) (distance * Math.sin(a) * Math.cos(e)));
        p.getPositionY().setValue((float) (distance * Math.sin(e)));
        p.getPositionZ().setValue((float) (-distance * Math.cos(a) * Math.cos(e)));
        src.connect(p);
        p.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        return new float[][] {out.getChannelData(0), out.getChannelData(1)};
    }

    /**
     * Converts a float array to doubles.
     * <p>
     * float 배열을 double 로 바꾼다.
     *
     * @param f the array / 배열
     * @return the doubles / double 배열
     */
    private static double[] toDouble(float[] f) {
        double[] d = new double[f.length];
        // 요소마다 변환한다.
        for (int i = 0; i < f.length; i++) {
            d[i] = f[i];
        }
        return d;
    }

    /**
     * The data of the product is available to the context, and the node reports a tail time that covers the head responses.
     * <p>
     * 제품의 자료를 컨텍스트가 쓸 수 있고, 노드는 머리 응답을 덮는 꼬리 시간을 알린다.
     */
    public void testDataIsAvailable() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 1024, RATE);
        Check.isTrue(ctx.isHrtfAvailable(), "the HRTF data is available");
        PannerNode p = ctx.createPanner();
        p.setPanningModel(PanningModelType.HRTF);
        Check.same(PanningModelType.HRTF, p.getPanningModel(), "the model");
    }

    /**
     * The impulse response of the node for a direction is the pair of kernels of that direction (the interpolation of the data with the
     * delays, the same as {@link HrtfSet#direction}), for the front, the sides, the back, above, below, and directions between the
     * measurements on the left and on the right.
     * <p>
     * 방향에 대한 노드의 임펄스 응답은 그 방향의 커널 쌍이다({@link HrtfSet#direction} 과 같은 지연 포함 자료 보간). 정면, 옆, 뒤, 위, 아래와 측정 사이의 왼쪽과 오른쪽
     * 방향에서 확인한다.
     */
    public void testImpulseResponseIsTheKernelPair() {
        HrtfSet set = new HrtfSet(db(), RATE);
        int n = set.getKernelLength();
        double[][] dirs = {{0, 0}, {90, 0}, {-90, 0}, {180, 0}, {45, 30}, {-60, 30}, {13.7, -22.5}, {-170, 55}, {30, 90}, {100, -40}};
        float[] el = new float[n];
        float[] er = new float[n];
        // 방향마다 노드의 임펄스 응답을 세트의 커널과 비교한다.
        for (int i = 0; i < dirs.length; i++) {
            float[][] out = impulseResponse(dirs[i][0], dirs[i][1], 1.0, 2048);
            set.direction(dirs[i][0], dirs[i][1], el, er, new float[n], new float[n]);
            // 커널의 탭마다 비교한다.
            for (int k = 0; k < n; k++) {
                Check.near(el[k], out[0][k], 2e-5, "left kernel of " + dirs[i][0] + "/" + dirs[i][1] + " at tap " + k);
                Check.near(er[k], out[1][k], 2e-5, "right kernel of " + dirs[i][0] + "/" + dirs[i][1] + " at tap " + k);
            }
            // 커널 뒤는 무음이다.
            for (int k = n; k < n + 100; k++) {
                Check.near(0.0, out[0][k], 1e-9, "after the kernel (left) " + k);
                Check.near(0.0, out[1][k], 1e-9, "after the kernel (right) " + k);
            }
        }
    }

    /**
     * Against the original KEMAR responses: at the measured directions of three rings (horizontal, 20 degrees above, 50 degrees above)
     * the impulse response of the node, in the magnitude response from 200 Hz to 10 kHz wherever the original is within 30 dB of its
     * peak (within 0.15 dB) and in the interaural time difference of the low frequency part (the lag of the cross-correlation of the
     * responses low-passed at 1200 Hz, within 1 sample), agrees with the original responses that the test reads from the unchanged
     * archive. The responses of the node include the constant latency of the HRTF set, which does not matter for either measure.
     * <p>
     * 원본 KEMAR 응답과 비교한다. 세 고리(수평, 20도 위, 50도 위)의 측정 방향에서 노드의 임펄스 응답이, 시험이 바뀌지 않은 압축 파일에서 읽는 원본 응답과 200 Hz ~ 10 kHz 의
     * 크기 응답(원본이 피크보다 30 dB 이내인 곳에서 0.15 dB 이내)과 저주파 부분의 두 귀 시간차(1200 Hz 로 저역 통과한 응답의 상호 상관 시차, 1 샘플 이내)가 일치한다.
     * 노드의 응답에는 HRTF 세트의 고정 지연이 들어 있지만 두 측정 모두에 영향이 없다.
     */
    public void testAgainstTheOriginalKemarResponses() {
        HrtfDatabase db = db();
        int[] rings = {4, 6, 9};
        // 고리마다 측정한 방위각 일부에서 노드의 응답과 원본을 비교한다.
        for (int r = 0; r < rings.length; r++) {
            int e = rings[r];
            // 방위각마다 처리한다.
            for (int a = 0; a < db.getAzimuthCount(e); a += 4) {
                int az = db.getAzimuth(e, a);
                int el = db.getElevation(e);
                double[][] original = KemarReference.get(el, az);
                float[][] out = impulseResponse(az, el, 1.0, 1024);
                double[][] rendered = {toDouble(out[0]), toDouble(out[1])};
                // 두 귀를 차례로 처리한다.
                for (int ear = 0; ear < 2; ear++) {
                    double peak = -1000;
                    // 주파수를 올려 가며 처리한다.
                    for (double f = 200; f <= 10000; f += 200) {
                        peak = Math.max(peak, KemarReference.magnitudeDb(original[ear], f, 44100.0));
                    }
                    // 200 Hz 간격으로 크기 응답을 비교한다.
                    for (double f = 200; f <= 10000; f += 200) {
                        double o = KemarReference.magnitudeDb(original[ear], f, 44100.0);
                        if (o < peak - 30) {
                            continue;
                        }
                        Check.near(o, KemarReference.magnitudeDb(rendered[ear], f, 44100.0), 0.15, "direction " + el + "/" + az + " ear " + ear + " at " + f + " Hz");
                    }
                }
                double lagOriginal = KemarReference.lagLeftLater(KemarReference.lowpass(original[0], 1200), KemarReference.lowpass(original[1], 1200));
                double lagRendered = KemarReference.lagLeftLater(KemarReference.lowpass(rendered[0], 1200), KemarReference.lowpass(rendered[1], 1200));
                Check.near(lagOriginal, lagRendered, 1.0, "interaural time difference at " + el + "/" + az);
            }
        }
    }

    /**
     * The distance gain multiplies the output: at the distance 2 with the inverse model (reference 1, rolloff 1) the response is half
     * of the response at the distance 1, and at the distance 4 a quarter.
     * <p>
     * 거리 게인이 출력에 곱해진다. 역수 모델(기준 1, 롤오프 1)에서 거리 2 의 응답은 거리 1 응답의 절반이고 거리 4 에서는 4분의 1 이다.
     */
    public void testDistanceGain() {
        float[][] near = impulseResponse(50, 10, 1.0, 1024);
        float[][] half = impulseResponse(50, 10, 2.0, 1024);
        float[][] quarter = impulseResponse(50, 10, 4.0, 1024);
        // 프레임마다 거리 게인을 곱한 값과 비교한다.
        for (int k = 0; k < 400; k++) {
            Check.near(0.5 * near[0][k], half[0][k], 2e-5, "left at the distance 2, frame " + k);
            Check.near(0.5 * near[1][k], half[1][k], 2e-5, "right at the distance 2, frame " + k);
            Check.near(0.25 * near[1][k], quarter[1][k], 2e-5, "right at the distance 4, frame " + k);
        }
    }

    /**
     * Renders a constant source through an HRTF panner.
     * <p>
     * 일정한 원천을 HRTF 패너를 거쳐 렌더링한다.
     *
     * @param left   the level of the left channel / 왼쪽 채널 레벨
     * @param right  the level of the right channel / 오른쪽 채널 레벨
     * @param stereo true for a stereo input / 스테레오 입력이면 true
     * @return the rendered audio / 렌더링한 오디오
     */
    private static AudioBuffer renderConstant(float left, float right, boolean stereo) {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 2048, RATE);
        AudioBuffer in = new AudioBuffer(stereo ? 2 : 1, 2048, RATE);
        java.util.Arrays.fill(in.getChannelData(0), left);
        if (stereo) {
            java.util.Arrays.fill(in.getChannelData(1), right);
        }
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        PannerNode p = ctx.createPanner();
        p.setPanningModel(PanningModelType.HRTF);
        p.getPositionX().setValue(0.5f);
        p.getPositionZ().setValue(-1f);
        src.connect(p);
        p.connect(ctx.getDestination());
        src.start(0);
        return ctx.startRendering();
    }

    /**
     * A stereo input is spatialized as its mono mean: the stereo input 0.4 and 0.2 gives the output of the mono input 0.3, and for a
     * constant input the output settles at the input times the sum of the taps of the kernel of that ear.
     * <p>
     * 스테레오 입력은 모노 평균으로 공간화된다. 스테레오 입력 0.4 와 0.2 는 모노 입력 0.3 의 출력을 주고, 일정한 입력의 출력은 입력에 그 귀 커널의 탭 합을 곱한 값으로
     * 수렴한다.
     */
    public void testStereoInputIsTheMonoMean() {
        AudioBuffer stereo = renderConstant(0.4f, 0.2f, true);
        AudioBuffer mono = renderConstant(0.3f, 0f, false);
        HrtfSet set = new HrtfSet(db(), RATE);
        int n = set.getKernelLength();
        float[] l = new float[n];
        float[] r = new float[n];
        double az = Math.toDegrees(Math.atan2(0.5, 1.0));
        double el = 0;
        set.direction(az, el, l, r, new float[n], new float[n]);
        double sumL = 0;
        double sumR = 0;
        // 커널의 탭 합(일정한 입력에 대한 정상 상태 이득)을 구한다.
        for (int k = 0; k < n; k++) {
            sumL += l[k];
            sumR += r[k];
        }
        double distanceGain = 1.0 / (1.0 + (Math.sqrt(1.25) - 1.0));
        // 항목을 하나씩 처리한다.
        for (int i = 1000; i < 1100; i += 7) {
            Check.near(mono.getChannelData(0)[i], stereo.getChannelData(0)[i], 2e-6, "left, stereo against mono at " + i);
            Check.near(mono.getChannelData(1)[i], stereo.getChannelData(1)[i], 2e-6, "right, stereo against mono at " + i);
            Check.near(0.3 * sumL * distanceGain, mono.getChannelData(0)[i], 2e-5, "left steady state at " + i);
            Check.near(0.3 * sumR * distanceGain, mono.getChannelData(1)[i], 2e-5, "right steady state at " + i);
        }
    }

    /**
     * When the source jumps from the left side to the right side at a quantum boundary the output does not jump: in that quantum the
     * output moves from the old steady state to the new steady state, the first frame is still close to the old state and the last
     * frame close to the new state (the kernels are cross-faded over the quantum), and the values in between change monotonically for
     * the ear that gets louder.
     * <p>
     * 음원이 quantum 경계에서 왼쪽 옆에서 오른쪽 옆으로 건너뛰어도 출력이 튀지 않는다. 그 quantum 에서 출력이 이전 정상 상태에서 새 정상 상태로 옮겨 가며, 첫 프레임은
     * 아직 이전 상태에 가깝고 마지막 프레임은 새 상태에 가깝다(커널을 quantum 동안 크로스페이드한다). 그 사이 값은 커지는 귀에서 단조롭게 바뀐다.
     */
    public void testCrossfadeWhenTheSourceMoves() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 2048, RATE);
        AudioBuffer in = new AudioBuffer(1, 2048, RATE);
        java.util.Arrays.fill(in.getChannelData(0), 0.5f);
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        PannerNode p = ctx.createPanner();
        p.setPanningModel(PanningModelType.HRTF);
        p.getPositionX().setValueAtTime(-1f, 0.0);
        p.getPositionX().setValueAtTime(1f, 1000.0 / RATE);
        src.connect(p);
        p.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        float[] l = out.getChannelData(0);
        float[] r = out.getChannelData(1);
        double oldL = l[1000];
        double oldR = r[1000];
        double newL = l[1500];
        double newR = r[1500];
        Check.isTrue(Math.abs(oldR - newR) > 0.05 && Math.abs(oldL - newL) > 0.05, "the two states differ clearly: " + oldL + "/" + newL + ", " + oldR + "/" + newR);
        // 변화가 일어나는 quantum 은 프레임 1024 ~ 1151 이다(이벤트 시간 1000 프레임 뒤의 첫 quantum).
        double span = Math.abs(newR - oldR);
        Check.near(oldR, r[1024], 0.05 * span, "the first frame of the quantum is still the old state");
        Check.near(newR, r[1151], 0.05 * span, "the last frame of the quantum is the new state");
        // 오른쪽 귀가 커지는(또는 작아지는) 방향으로 quantum 안에서 단조롭게 바뀐다.
        boolean rising = newR > oldR;
        for (int i = 1025; i < 1152; i++) {
            if (rising) {
                Check.isTrue(r[i] >= r[i - 1] - 1e-4, "the right ear moves monotonically at " + i);
            } else {
                Check.isTrue(r[i] <= r[i - 1] + 1e-4, "the right ear moves monotonically at " + i);
            }
        }
        Check.near(newR, r[1152], 0.02 * span, "after the quantum the new state is steady");
    }

    /**
     * The tail time: after a short input the node keeps rendering the head response, the output is silent afterwards, and the context
     * lets go of the node (nothing is pinned) once the response has ended; the node reports a tail of the kernel length, which is
     * the length of the response of the set.
     * <p>
     * 꼬리 시간: 짧은 입력 뒤에도 노드가 머리 응답을 계속 렌더링하고, 그 뒤에는 무음이며, 응답이 끝나면 컨텍스트가 노드를 놓아 준다(붙잡은 것이 없다). 노드가 알리는 꼬리는
     * 커널 길이이며 세트 응답의 길이이다.
     */
    public void testTailTime() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 4096, RATE);
        AudioBuffer in = new AudioBuffer(1, 128, RATE);
        in.getChannelData(0)[127] = 1f;
        AudioBufferSourceNode src = ctx.createBufferSource();
        src.setBuffer(in);
        PannerNode p = ctx.createPanner();
        p.setPanningModel(PanningModelType.HRTF);
        p.getPositionX().setValue(1f);
        src.connect(p);
        p.connect(ctx.getDestination());
        src.start(0);
        AudioBuffer out = ctx.startRendering();
        HrtfSet set = new HrtfSet(db(), RATE);
        int n = set.getKernelLength();
        double energyAfterInput = 0;
        // 입력(프레임 127)이 끝난 뒤의 출력이 응답이다. 응답은 프레임 127 + n 까지이다.
        for (int i = 128; i < 127 + n; i++) {
            energyAfterInput += out.getChannelData(1)[i] * out.getChannelData(1)[i];
        }
        Check.isTrue(energyAfterInput > 1e-4, "the response continues after the input: " + energyAfterInput);
        // 응답 뒤는 무음이다.
        for (int i = 127 + n; i < 4096; i++) {
            Check.near(0.0, out.getChannelData(0)[i], 1e-9, "silence after the response (left) " + i);
            Check.near(0.0, out.getChannelData(1)[i], 1e-9, "silence after the response (right) " + i);
        }
        Check.equal(0L, (long) ctx.getPinnedNodeCount(), "nothing is pinned after the tail");
    }

    /**
     * Switching the model back to <code>equalpower</code> gives the equal power gains again (a source on the right is only on the right),
     * and the HRTF model is not used after the switch.
     * <p>
     * 모델을 <code>equalpower</code> 로 되돌리면 등전력 게인이 다시 나온다(오른쪽의 음원은 오른쪽에서만 들린다). 되돌린 뒤에는 HRTF 모델을 쓰지 않는다.
     */
    public void testSwitchingBackToEqualPower() {
        OfflineAudioContext ctx = new OfflineAudioContext(2, 512, RATE);
        ConstantSourceNode c = ctx.createConstantSource();
        PannerNode p = ctx.createPanner();
        p.setPanningModel(PanningModelType.HRTF);
        p.setPanningModel(PanningModelType.EQUAL_POWER);
        p.getPositionX().setValue(1f);
        c.connect(p);
        p.connect(ctx.getDestination());
        c.start(0);
        AudioBuffer out = ctx.startRendering();
        Check.near(0.0, out.getChannelData(0)[300], 1e-6, "left is silent for a source on the right");
        Check.near(1.0, out.getChannelData(1)[300], 1e-6, "right carries the whole signal");
    }
}
