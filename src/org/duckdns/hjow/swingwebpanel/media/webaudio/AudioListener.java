package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The listener of a context: its position and the two vectors that give its orientation, in the right-handed coordinate system of the
 * spatialization (Web Audio API 1.11). Every {@link PannerNode} of the context spatializes in relation to this one listener. The nine
 * parameters are a-rate by default (a node with the <code>HRTF</code> model reads them once per quantum) and are computed by the
 * context before its nodes, so a node sees consistent values within a quantum. By default the listener is at the origin, looks along
 * the negative z axis (<code>forward</code> is (0, 0, -1)) and has the positive y axis above it (<code>up</code> is (0, 1, 0)); then
 * the positive x axis is on its right.
 * <p>
 * Limitation: because the parameters are computed before the nodes, an audio node cannot drive a parameter of the listener (a
 * connection to it would act one quantum late).
 * <p>
 * 컨텍스트의 청취자: 공간화의 오른손 좌표계에서 위치와 방향을 주는 두 벡터(Web Audio API 1.11). 컨텍스트의 모든 {@link PannerNode} 가 이 청취자 하나에 대해
 * 공간화한다. 아홉 파라미터는 기본이 a-rate 이고(<code>HRTF</code> 모델의 노드는 quantum 마다 한 번 읽는다) 컨텍스트가 노드보다 먼저 계산하므로 노드는 한
 * quantum 안에서 일관된 값을 본다. 기본 청취자는 원점에서 -z 방향을 보며(<code>forward</code> 는 (0, 0, -1)) 위쪽이 +y 이다(<code>up</code> 은 (0, 1, 0)). 그러면 +x 가
 * 오른쪽이다.
 * <p>
 * 제한: 파라미터를 노드보다 먼저 계산하므로 오디오 노드는 청취자의 파라미터를 구동할 수 없다(연결해도 한 quantum 늦게 작동한다).
 */
public final class AudioListener {

    /**
     * The x of the position (default 0).
     * <p>
     * 위치의 x(기본 0).
     */
    private final AudioParam positionX;

    /**
     * The y of the position (default 0).
     * <p>
     * 위치의 y(기본 0).
     */
    private final AudioParam positionY;

    /**
     * The z of the position (default 0).
     * <p>
     * 위치의 z(기본 0).
     */
    private final AudioParam positionZ;

    /**
     * The x of the forward vector (default 0).
     * <p>
     * forward 벡터의 x(기본 0).
     */
    private final AudioParam forwardX;

    /**
     * The y of the forward vector (default 0).
     * <p>
     * forward 벡터의 y(기본 0).
     */
    private final AudioParam forwardY;

    /**
     * The z of the forward vector (default -1).
     * <p>
     * forward 벡터의 z(기본 -1).
     */
    private final AudioParam forwardZ;

    /**
     * The x of the up vector (default 0).
     * <p>
     * up 벡터의 x(기본 0).
     */
    private final AudioParam upX;

    /**
     * The y of the up vector (default 1).
     * <p>
     * up 벡터의 y(기본 1).
     */
    private final AudioParam upY;

    /**
     * The z of the up vector (default 0).
     * <p>
     * up 벡터의 z(기본 0).
     */
    private final AudioParam upZ;

    /**
     * Creates the listener of a context and registers its parameters with the context.
     * <p>
     * 컨텍스트의 청취자를 만들고 파라미터를 컨텍스트에 등록한다.
     *
     * @param context the context / 컨텍스트
     */
    AudioListener(BaseAudioContext context) {
        positionX = param(context, "positionX", 0f);
        positionY = param(context, "positionY", 0f);
        positionZ = param(context, "positionZ", 0f);
        forwardX = param(context, "forwardX", 0f);
        forwardY = param(context, "forwardY", 0f);
        forwardZ = param(context, "forwardZ", -1f);
        upX = param(context, "upX", 0f);
        upY = param(context, "upY", 1f);
        upZ = param(context, "upZ", 0f);
    }

    /**
     * Creates a parameter of the listener and registers it with the context.
     * <p>
     * 청취자의 파라미터를 만들고 컨텍스트에 등록한다.
     *
     * @param context      the context / 컨텍스트
     * @param name         the name / 이름
     * @param defaultValue the default value / 기본 값
     * @return the parameter / 파라미터
     */
    private static AudioParam param(BaseAudioContext context, String name, float defaultValue) {
        return context.registerContextParam(new AudioParam(context, null, name, defaultValue, -AudioParam.MAX_FLOAT, AudioParam.MAX_FLOAT,
                AutomationRate.A_RATE, false));
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
     * Returns the <code>forwardX</code> parameter.
     * <p>
     * <code>forwardX</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getForwardX() {
        return forwardX;
    }

    /**
     * Returns the <code>forwardY</code> parameter.
     * <p>
     * <code>forwardY</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getForwardY() {
        return forwardY;
    }

    /**
     * Returns the <code>forwardZ</code> parameter.
     * <p>
     * <code>forwardZ</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getForwardZ() {
        return forwardZ;
    }

    /**
     * Returns the <code>upX</code> parameter.
     * <p>
     * <code>upX</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getUpX() {
        return upX;
    }

    /**
     * Returns the <code>upY</code> parameter.
     * <p>
     * <code>upY</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getUpY() {
        return upY;
    }

    /**
     * Returns the <code>upZ</code> parameter.
     * <p>
     * <code>upZ</code> 파라미터를 반환한다.
     *
     * @return the parameter / 파라미터
     */
    public AudioParam getUpZ() {
        return upZ;
    }

    /**
     * Throws <code>NotSupportedError</code> if one of the parameters has a value curve.
     * <p>
     * 파라미터 중 하나에 값 곡선이 있으면 <code>NotSupportedError</code> 를 던진다.
     *
     * @param params the parameters / 파라미터
     */
    static void checkNoCurve(AudioParam[] params) {
        // 파라미터마다 값 곡선이 있는지 본다.
        for (int i = 0; i < params.length; i++) {
            if (params[i].hasValueCurve()) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "the parameter " + params[i].getName() + " has a value curve");
            }
        }
    }

    /**
     * Sets the position (deprecated: the same as setting the <code>value</code> of <code>positionX</code>, <code>positionY</code> and
     * <code>positionZ</code>).
     * <p>
     * 위치를 설정한다(폐기 예정: <code>positionX</code>, <code>positionY</code>, <code>positionZ</code> 의 <code>value</code> 를 설정하는 것과 같다).
     *
     * @param x the x / x
     * @param y the y / y
     * @param z the z / z
     * @throws AudioException <code>NotSupportedError</code> if one of the parameters has a value curve / 파라미터 중 하나에 값 곡선이 있으면
     *                        <code>NotSupportedError</code>
     */
    public void setPosition(float x, float y, float z) {
        checkNoCurve(new AudioParam[] {positionX, positionY, positionZ});
        positionX.setValue(x);
        positionY.setValue(y);
        positionZ.setValue(z);
    }

    /**
     * Sets the orientation (deprecated: the same as setting the <code>value</code> of the forward and up parameters).
     * <p>
     * 방향을 설정한다(폐기 예정: forward 와 up 파라미터의 <code>value</code> 를 설정하는 것과 같다).
     *
     * @param x   the x of the forward vector / forward 벡터의 x
     * @param y   the y of the forward vector / forward 벡터의 y
     * @param z   the z of the forward vector / forward 벡터의 z
     * @param xUp the x of the up vector / up 벡터의 x
     * @param yUp the y of the up vector / up 벡터의 y
     * @param zUp the z of the up vector / up 벡터의 z
     * @throws AudioException <code>NotSupportedError</code> if one of the parameters has a value curve / 파라미터 중 하나에 값 곡선이 있으면
     *                        <code>NotSupportedError</code>
     */
    public void setOrientation(float x, float y, float z, float xUp, float yUp, float zUp) {
        checkNoCurve(new AudioParam[] {forwardX, forwardY, forwardZ, upX, upY, upZ});
        forwardX.setValue(x);
        forwardY.setValue(y);
        forwardZ.setValue(z);
        upX.setValue(xUp);
        upY.setValue(yUp);
        upZ.setValue(zUp);
    }
}
