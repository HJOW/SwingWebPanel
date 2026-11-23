package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.HrtfRenderer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.HrtfSet;

/**
 * Positions an incoming audio stream in three-dimensional space in relation to the {@link AudioListener} of the context (Web Audio API
 * 1.27). The input has one or two channels; the output is always stereo (one silent channel while the node is not processing).
 * <p>
 * The geometry (azimuth, elevation, distance, cone) is {@link Spatial}. With the <code>equalpower</code> model the azimuth is
 * panned between the two channels with equal power (the elevation is ignored): a mono input is panned across the image, a stereo input
 * is balanced as in the {@link StereoPannerNode}; the position and orientation parameters of the panner and of the listener are a-rate
 * if any of them is automated at that rate. With the <code>HRTF</code> model all parameters are read once per render quantum and the
 * source (a stereo input is first down-mixed to mono) is convolved with the measured head-related impulse responses of the direction
 * (see {@link org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.HrtfDatabase} for the data and its credit); if the HRTF data cannot be loaded the node falls back to the
 * <code>equalpower</code> processing. The gain of the distance model and of the sound cone multiplies the result in both models.
 * <p>
 * 입력 오디오 스트림을 컨텍스트의 {@link AudioListener} 에 대한 3차원 공간에 배치한다(Web Audio API 1.27). 입력은 채널이 하나나 둘이고 출력은 항상
 * 스테레오이다(노드가 처리 중이 아니면 무음 채널 하나).
 * <p>
 * 기하(방위각, 고도, 거리, 원뿔)는 {@link Spatial} 이다. <code>equalpower</code> 모델은 방위각을 두 채널 사이에 등전력으로 나눈다(고도는 무시한다). 모노 입력은 이미지
 * 전체에 패닝되고 스테레오 입력은 {@link StereoPannerNode} 처럼 균형이 맞춰진다. 패너와 청취자의 위치·방향 파라미터는 그중 하나라도 그 속도로 자동화되면 a-rate 이다.
 * <code>HRTF</code> 모델은 모든 파라미터를 render quantum 마다 한 번 읽고, 음원(스테레오 입력은 먼저 모노로 다운믹스한다)을 방향의 측정한 머리 임펄스 응답과 합성곱한다(자료와
 * 출처 표기는 {@link org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.HrtfDatabase} 참고). HRTF 자료를 읽을 수 없으면 노드는 <code>equalpower</code> 처리로 대신한다. 거리 모델과 음향 원뿔의 게인은 두 모델 모두에서
 * 결과에 곱해진다.
 */
public final class PannerNode extends AudioNode {

    /**
     * The x of the position of the source (a-rate, default 0).
     * <p>
     * 음원 위치의 x(a-rate, 기본 0).
     */
    private final AudioParam positionX;

    /**
     * The y of the position of the source.
     * <p>
     * 음원 위치의 y.
     */
    private final AudioParam positionY;

    /**
     * The z of the position of the source.
     * <p>
     * 음원 위치의 z.
     */
    private final AudioParam positionZ;

    /**
     * The x of the direction that the source points in (default 1).
     * <p>
     * 음원이 향하는 방향의 x(기본 1).
     */
    private final AudioParam orientationX;

    /**
     * The y of the direction that the source points in (default 0).
     * <p>
     * 음원이 향하는 방향의 y(기본 0).
     */
    private final AudioParam orientationY;

    /**
     * The z of the direction that the source points in (default 0).
     * <p>
     * 음원이 향하는 방향의 z(기본 0).
     */
    private final AudioParam orientationZ;

    /**
     * The spatialization algorithm.
     * <p>
     * 공간화 알고리즘.
     */
    private volatile PanningModelType panningModel = PanningModelType.EQUAL_POWER;

    /**
     * The distance model.
     * <p>
     * 거리 모델.
     */
    private volatile DistanceModelType distanceModel = DistanceModelType.INVERSE;

    /**
     * The reference distance: up to it the volume is not reduced (default 1).
     * <p>
     * 기준 거리: 이 거리까지는 음량이 줄지 않는다(기본 1).
     */
    private volatile double refDistance = 1.0;

    /**
     * The largest distance: from it the volume is not reduced any further (default 10000).
     * <p>
     * 최대 거리: 이 거리부터는 음량이 더 줄지 않는다(기본 10000).
     */
    private volatile double maxDistance = 10000.0;

    /**
     * How quickly the volume falls with the distance (default 1).
     * <p>
     * 거리에 따라 음량이 얼마나 빨리 줄어드는지(기본 1).
     */
    private volatile double rolloffFactor = 1.0;

    /**
     * The inner cone angle in degrees (default 360).
     * <p>
     * 안쪽 원뿔 각(도, 기본 360).
     */
    private volatile double coneInnerAngle = 360.0;

    /**
     * The outer cone angle in degrees (default 360).
     * <p>
     * 바깥 원뿔 각(도, 기본 360).
     */
    private volatile double coneOuterAngle = 360.0;

    /**
     * The gain outside of the outer cone, from 0 to 1 (default 0).
     * <p>
     * 바깥 원뿔 밖의 게인(0 ~ 1, 기본 0).
     */
    private volatile double coneOuterGain = 0.0;

    /**
     * The HRTF renderer of this source (rendering thread).
     * <p>
     * 이 음원의 HRTF 렌더러(렌더링 스레드).
     */
    private HrtfRenderer hrtf;

    /**
     * The set of kernels that the renderer was made for (rendering thread).
     * <p>
     * 렌더러를 만든 커널 세트(렌더링 스레드).
     */
    private HrtfSet hrtfSet;

    /**
     * The mono mix of a stereo input for the HRTF model (rendering thread).
     * <p>
     * HRTF 모델을 위한 스테레오 입력의 모노 혼합(렌더링 스레드).
     */
    private final float[] monoMix;

    /**
     * The azimuth that {@link #compute} found (rendering thread).
     * <p>
     * {@link #compute} 가 구한 방위각(렌더링 스레드).
     */
    private double azimuth;

    /**
     * The elevation that {@link #compute} found (rendering thread).
     * <p>
     * {@link #compute} 가 구한 고도(렌더링 스레드).
     */
    private double elevation;

    /**
     * The gain of the distance and the cone that {@link #compute} found (rendering thread).
     * <p>
     * {@link #compute} 가 구한 거리와 원뿔의 게인(렌더링 스레드).
     */
    private double totalGain;

    /**
     * The parameters that decide whether the quantum needs per-sample geometry, built on first use (rendering thread).
     * <p>
     * quantum 에 샘플별 기하가 필요한지 정하는 파라미터. 처음 쓸 때 만든다(렌더링 스레드).
     */
    private AudioParam[] watched;

    /**
     * The work array of the azimuth and elevation (rendering thread).
     * <p>
     * 방위각과 고도의 작업 배열(렌더링 스레드).
     */
    private final double[] ae = new double[2];

    /**
     * Creates a panner at the origin with the <code>equalpower</code> model.
     * <p>
     * 원점에 있는 <code>equalpower</code> 모델의 패너를 만든다.
     *
     * @param context the context / 컨텍스트
     */
    public PannerNode(BaseAudioContext context) {
        super(context, 1, 1, 2, ChannelCountMode.CLAMPED_MAX, ChannelInterpretation.SPEAKERS);
        positionX = registerParam(new AudioParam(context, this, "positionX", 0f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        positionY = registerParam(new AudioParam(context, this, "positionY", 0f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        positionZ = registerParam(new AudioParam(context, this, "positionZ", 0f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        orientationX = registerParam(new AudioParam(context, this, "orientationX", 1f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        orientationY = registerParam(new AudioParam(context, this, "orientationY", 0f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        orientationZ = registerParam(new AudioParam(context, this, "orientationZ", 0f, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT, AutomationRate.A_RATE, false));
        monoMix = new float[context.getRenderQuantumSize()];
    }

    /**
     * Returns the <code>positionX</code> parameter.
     * <p>
     * <code>positionX</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getPositionX() {
        return positionX;
    }

    /**
     * Returns the <code>positionY</code> parameter.
     * <p>
     * <code>positionY</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getPositionY() {
        return positionY;
    }

    /**
     * Returns the <code>positionZ</code> parameter.
     * <p>
     * <code>positionZ</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getPositionZ() {
        return positionZ;
    }

    /**
     * Returns the <code>orientationX</code> parameter.
     * <p>
     * <code>orientationX</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getOrientationX() {
        return orientationX;
    }

    /**
     * Returns the <code>orientationY</code> parameter.
     * <p>
     * <code>orientationY</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getOrientationY() {
        return orientationY;
    }

    /**
     * Returns the <code>orientationZ</code> parameter.
     * <p>
     * <code>orientationZ</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getOrientationZ() {
        return orientationZ;
    }

    /**
     * Sets the position (deprecated: the same as setting the <code>value</code> of the three position parameters).
     * <p>
     * 위치를 설정한다(폐기 예정: 위치 파라미터 셋의 <code>value</code> 를 설정하는 것과 같다).
     *
     * @param x the x / x
     * @param y the y / y
     * @param z the z / z
     * @throws AudioException <code>NotSupportedError</code> if one of the parameters has a value curve / 파라미터 중 하나에 값 곡선이 있으면
     *                        <code>NotSupportedError</code>
     */
    public void setPosition(float x, float y, float z) {
        AudioListener.checkNoCurve(new AudioParam[] {positionX, positionY, positionZ});
        positionX.setValue(x);
        positionY.setValue(y);
        positionZ.setValue(z);
    }

    /**
     * Sets the orientation (deprecated: the same as setting the <code>value</code> of the three orientation parameters).
     * <p>
     * 방향을 설정한다(폐기 예정: 방향 파라미터 셋의 <code>value</code> 를 설정하는 것과 같다).
     *
     * @param x the x / x
     * @param y the y / y
     * @param z the z / z
     * @throws AudioException <code>NotSupportedError</code> if one of the parameters has a value curve / 파라미터 중 하나에 값 곡선이 있으면
     *                        <code>NotSupportedError</code>
     */
    public void setOrientation(float x, float y, float z) {
        AudioListener.checkNoCurve(new AudioParam[] {orientationX, orientationY, orientationZ});
        orientationX.setValue(x);
        orientationY.setValue(y);
        orientationZ.setValue(z);
    }

    /**
     * Returns the panning model.
     * <p>
     * 패닝 모델을 반환한다.
     *
     * @return the model / 모델
     */
    public PanningModelType getPanningModel() {
        return panningModel;
    }

    /**
     * Sets the panning model. Choosing <code>HRTF</code> loads the HRTF data of the context if that has not happened yet (this may take
     * a moment, and is why it is done here and not while rendering).
     * <p>
     * 패닝 모델을 설정한다. <code>HRTF</code> 를 고르면 컨텍스트의 HRTF 자료를 아직 읽지 않았을 때 읽는다(시간이 조금 걸릴 수 있으며 렌더링 중이 아니라 여기서
     * 하는 이유이다).
     *
     * @param model the model / 모델
     * @throws AudioException <code>TypeError</code> if the model is null / 모델이 null 이면 <code>TypeError</code>
     */
    public void setPanningModel(PanningModelType model) {
        if (model == null) {
            throw new AudioException(AudioException.Kind.TYPE, "panningModel is null");
        }
        if (model == PanningModelType.HRTF) {
            getContext().prepareHrtf();
        }
        panningModel = model;
    }

    /**
     * Returns the distance model.
     * <p>
     * 거리 모델을 반환한다.
     *
     * @return the model / 모델
     */
    public DistanceModelType getDistanceModel() {
        return distanceModel;
    }

    /**
     * Sets the distance model.
     * <p>
     * 거리 모델을 설정한다.
     *
     * @param model the model / 모델
     * @throws AudioException <code>TypeError</code> if the model is null / 모델이 null 이면 <code>TypeError</code>
     */
    public void setDistanceModel(DistanceModelType model) {
        if (model == null) {
            throw new AudioException(AudioException.Kind.TYPE, "distanceModel is null");
        }
        distanceModel = model;
    }

    /**
     * Throws <code>TypeError</code> if a value is not finite.
     * <p>
     * 값이 유한하지 않으면 <code>TypeError</code> 를 던진다.
     *
     * @param v    the value / 값
     * @param what the name of the attribute / 속성 이름
     */
    private static void finite(double v, String what) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw new AudioException(AudioException.Kind.TYPE, what + " is not a finite number");
        }
    }

    /**
     * Returns the reference distance.
     * <p>
     * 기준 거리를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getRefDistance() {
        return refDistance;
    }

    /**
     * Sets the reference distance, 0 or more.
     * <p>
     * 기준 거리를 설정한다(0 이상).
     *
     * @param value the distance / 거리
     * @throws AudioException <code>RangeError</code> if the value is negative / 값이 음수이면 <code>RangeError</code>
     */
    public void setRefDistance(double value) {
        finite(value, "refDistance");
        if (value < 0) {
            throw new AudioException(AudioException.Kind.RANGE, "refDistance " + value + " is negative");
        }
        refDistance = value;
    }

    /**
     * Returns the largest distance.
     * <p>
     * 최대 거리를 반환한다.
     *
     * @return the distance / 거리
     */
    public double getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets the largest distance, greater than 0.
     * <p>
     * 최대 거리를 설정한다(0 보다 커야 한다).
     *
     * @param value the distance / 거리
     * @throws AudioException <code>RangeError</code> if the value is not positive / 값이 양수가 아니면 <code>RangeError</code>
     */
    public void setMaxDistance(double value) {
        finite(value, "maxDistance");
        if (value <= 0) {
            throw new AudioException(AudioException.Kind.RANGE, "maxDistance " + value + " is not positive");
        }
        maxDistance = value;
    }

    /**
     * Returns the rolloff factor as it was set.
     * <p>
     * 설정한 롤오프 계수를 반환한다.
     *
     * @return the factor / 계수
     */
    public double getRolloffFactor() {
        return rolloffFactor;
    }

    /**
     * Sets the rolloff factor, 0 or more. The linear model clamps it to 0..1 while it computes the gain; the attribute keeps the value
     * that was set.
     * <p>
     * 롤오프 계수를 설정한다(0 이상). 선형 모델은 게인을 계산할 때 0..1 로 제한하며 속성은 설정한 값을 유지한다.
     *
     * @param value the factor / 계수
     * @throws AudioException <code>RangeError</code> if the value is negative / 값이 음수이면 <code>RangeError</code>
     */
    public void setRolloffFactor(double value) {
        finite(value, "rolloffFactor");
        if (value < 0) {
            throw new AudioException(AudioException.Kind.RANGE, "rolloffFactor " + value + " is negative");
        }
        rolloffFactor = value;
    }

    /**
     * Returns the inner cone angle in degrees.
     * <p>
     * 안쪽 원뿔 각(도)을 반환한다.
     *
     * @return the angle / 각
     */
    public double getConeInnerAngle() {
        return coneInnerAngle;
    }

    /**
     * Sets the inner cone angle in degrees (the whole angle; the behavior is undefined outside 0..360).
     * <p>
     * 안쪽 원뿔 각(도, 전체 각. 0 ~ 360 밖에서는 동작이 정해져 있지 않다)을 설정한다.
     *
     * @param value the angle / 각
     */
    public void setConeInnerAngle(double value) {
        finite(value, "coneInnerAngle");
        coneInnerAngle = value;
    }

    /**
     * Returns the outer cone angle in degrees.
     * <p>
     * 바깥 원뿔 각(도)을 반환한다.
     *
     * @return the angle / 각
     */
    public double getConeOuterAngle() {
        return coneOuterAngle;
    }

    /**
     * Sets the outer cone angle in degrees (the whole angle).
     * <p>
     * 바깥 원뿔 각(도, 전체 각)을 설정한다.
     *
     * @param value the angle / 각
     */
    public void setConeOuterAngle(double value) {
        finite(value, "coneOuterAngle");
        coneOuterAngle = value;
    }

    /**
     * Returns the gain outside of the outer cone.
     * <p>
     * 바깥 원뿔 밖의 게인을 반환한다.
     *
     * @return the gain / 게인
     */
    public double getConeOuterGain() {
        return coneOuterGain;
    }

    /**
     * Sets the gain outside of the outer cone, a linear value from 0 to 1.
     * <p>
     * 바깥 원뿔 밖의 게인(0 ~ 1 의 선형 값)을 설정한다.
     *
     * @param value the gain / 게인
     * @throws AudioException <code>InvalidStateError</code> if the value is outside 0..1 / 값이 0~1 밖이면 <code>InvalidStateError</code>
     */
    public void setConeOuterGain(double value) {
        finite(value, "coneOuterGain");
        if (value < 0.0 || value > 1.0) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "coneOuterGain " + value + " is outside 0..1");
        }
        coneOuterGain = value;
    }

    /**
     * Rejects a <code>channelCount</code> above 2.
     * <p>
     * 2 를 넘는 <code>channelCount</code> 를 거부한다.
     *
     * @param count the new value / 새 값
     */
    @Override
    void checkChannelCount(int count) {
        if (count > 2) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCount of a PannerNode is at most 2");
        }
    }

    /**
     * Rejects the mode <code>max</code>.
     * <p>
     * <code>max</code> 모드를 거부한다.
     *
     * @param mode the new value / 새 값
     */
    @Override
    void checkChannelCountMode(ChannelCountMode mode) {
        if (mode == ChannelCountMode.MAX) {
            throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the channelCountMode of a PannerNode cannot be max");
        }
    }

    /**
     * Returns the tail time: the length of the head-related impulse responses with the HRTF model (the node outputs the response of a
     * silent input), 0 with the <code>equalpower</code> model.
     * <p>
     * 꼬리 시간을 반환한다. HRTF 모델에서는 머리 임펄스 응답의 길이(노드가 무음 입력의 응답을 출력한다), <code>equalpower</code> 모델에서는 0.
     *
     * @return the tail in frames / 꼬리(프레임)
     */
    @Override
    long tailFrames() {
        if (panningModel == PanningModelType.HRTF) {
            HrtfSet set = getContext().getLoadedHrtfSet();
            if (set != null) {
                return set.getKernelLength();
            }
        }
        return 0;
    }

    /**
     * Clears the memory of the HRTF renderer.
     * <p>
     * HRTF 렌더러의 기억을 지운다.
     */
    @Override
    void resetState() {
        if (hrtf != null) {
            hrtf.reset();
        }
    }

    /**
     * Computes the azimuth, the elevation and the gain of the distance and the cone for the frame <code>i</code> of the quantum from
     * the values of the parameters of the panner and of the listener at that frame; the results are in the fields of the node.
     * <p>
     * quantum 의 프레임 <code>i</code> 에서 패너와 청취자의 파라미터 값으로 방위각, 고도, 거리와 원뿔의 게인을 계산한다. 결과는 노드의 필드에 담긴다.
     *
     * @param i the frame / 프레임
     */
    private void compute(int i) {
        AudioListener l = getContext().getListener();
        double px = positionX.valueAt(i);
        double py = positionY.valueAt(i);
        double pz = positionZ.valueAt(i);
        double lx = l.getPositionX().valueAt(i);
        double ly = l.getPositionY().valueAt(i);
        double lz = l.getPositionZ().valueAt(i);
        Spatial.azimuthElevation(px, py, pz, lx, ly, lz, l.getForwardX().valueAt(i), l.getForwardY().valueAt(i), l.getForwardZ().valueAt(i), l.getUpX().valueAt(i),
                l.getUpY().valueAt(i), l.getUpZ().valueAt(i), ae);
        azimuth = ae[0];
        elevation = ae[1];
        double distance = Spatial.distance(px, py, pz, lx, ly, lz);
        double dg = Spatial.distanceGain(distanceModel, distance, refDistance, maxDistance, rolloffFactor);
        double cg = Spatial.coneGain(orientationX.valueAt(i), orientationY.valueAt(i), orientationZ.valueAt(i), px, py, pz, lx, ly, lz, coneInnerAngle, coneOuterAngle,
                coneOuterGain);
        totalGain = dg * cg;
    }

    /**
     * Tells whether the position or orientation of the panner or the listener changes within the quantum.
     * <p>
     * 패너나 청취자의 위치나 방향이 quantum 안에서 바뀌는지 알려 준다.
     *
     * @return true if one of the parameters is not constant over the quantum / 파라미터 중 하나가 quantum 동안 일정하지 않으면 true
     */
    private boolean varies() {
        if (watched == null) {
            AudioListener l = getContext().getListener();
            watched = new AudioParam[] {positionX, positionY, positionZ, orientationX, orientationY, orientationZ, l.getPositionX(), l.getPositionY(), l.getPositionZ(),
                    l.getForwardX(), l.getForwardY(), l.getForwardZ(), l.getUpX(), l.getUpY(), l.getUpZ()};
        }
        AudioParam[] ps = watched;
        // 파라미터마다 quantum 동안 값이 일정한지 본다.
        for (int i = 0; i < ps.length; i++) {
            if (!ps[i].isConstantBlock()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Spatializes the input.
     * <p>
     * 입력을 공간화한다.
     *
     * @param frames the number of frames / 프레임 수
     */
    @Override
    void process(int frames) {
        AudioBus in = inputBus(0);
        AudioBus out = outputBus(0);
        out.setNumberOfChannels(2);
        HrtfSet set = panningModel == PanningModelType.HRTF ? getContext().getLoadedHrtfSet() : null;
        if (set != null) {
            processHrtf(in, out, frames, set);
        } else {
            processEqualPower(in, out, frames);
        }
    }

    /**
     * The equal power panning of the specification (6.3.1) with the distance and cone gain, per sample if the parameters change within
     * the quantum.
     * <p>
     * 거리와 원뿔 게인을 곱한 규격의 등전력 패닝(6.3.1). 파라미터가 quantum 안에서 바뀌면 샘플마다 계산한다.
     *
     * @param in     the input / 입력
     * @param out    the output / 출력
     * @param frames the number of frames / 프레임 수
     */
    private void processEqualPower(AudioBus in, AudioBus out, int frames) {
        boolean mono = in.getNumberOfChannels() == 1;
        float[] inL = in.channel(0);
        float[] inR = mono ? inL : in.channel(1);
        float[] left = out.channel(0);
        float[] right = out.channel(1);
        boolean perFrame = varies();
        double gl = 0.0;
        double gr = 0.0;
        double az = 0.0;
        // 프레임마다(값이 일정하면 처음 한 번만) 방위각에서 좌우 이득을 구해 출력한다.
        for (int i = 0; i < frames; i++) {
            if (perFrame || i == 0) {
                compute(i);
                az = Math.max(-180.0, Math.min(180.0, azimuth));
                // 방위각을 [-90, 90] 으로 접는다.
                if (az < -90.0) {
                    az = -180.0 - az;
                } else if (az > 90.0) {
                    az = 180.0 - az;
                }
                double x;
                if (mono) {
                    x = (az + 90.0) / 180.0;
                } else if (az <= 0) {
                    x = (az + 90.0) / 90.0;
                } else {
                    x = az / 90.0;
                }
                gl = Math.cos(x * Math.PI / 2.0);
                gr = Math.sin(x * Math.PI / 2.0);
            }
            if (mono) {
                float s = inL[i];
                left[i] = (float) (s * gl * totalGain);
                right[i] = (float) (s * gr * totalGain);
            } else if (az <= 0) {
                left[i] = (float) ((inL[i] + inR[i] * gl) * totalGain);
                right[i] = (float) (inR[i] * gr * totalGain);
            } else {
                left[i] = (float) (inL[i] * gl * totalGain);
                right[i] = (float) ((inR[i] + inL[i] * gr) * totalGain);
            }
        }
    }

    /**
     * The HRTF panning: the parameters are read at the first frame of the quantum; a stereo input is down-mixed to mono first.
     * <p>
     * HRTF 패닝. 파라미터는 quantum 의 첫 프레임에서 읽고, 스테레오 입력은 먼저 모노로 다운믹스한다.
     *
     * @param in     the input / 입력
     * @param out    the output / 출력
     * @param frames the number of frames / 프레임 수
     * @param set    the kernels / 커널
     */
    private void processHrtf(AudioBus in, AudioBus out, int frames, HrtfSet set) {
        if (hrtf == null || hrtfSet != set) {
            hrtf = new HrtfRenderer(set, getContext().getRenderQuantumSize());
            hrtfSet = set;
        }
        compute(0);
        float[] source = in.channel(0);
        if (in.getNumberOfChannels() > 1) {
            float[] r = in.channel(1);
            // 스테레오 입력은 두 채널의 평균을 단일 원천으로 쓴다.
            for (int i = 0; i < frames; i++) {
                monoMix[i] = 0.5f * (source[i] + r[i]);
            }
            source = monoMix;
        }
        hrtf.process(source, frames, azimuth, elevation, totalGain, out.channel(0), out.channel(1));
    }
}
