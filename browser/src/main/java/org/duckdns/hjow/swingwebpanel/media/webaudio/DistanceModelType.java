package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>DistanceModelType</code> of a panner: how the volume of a source falls with the distance from the listener (Web Audio API
 * 1.27).
 * <p>
 * 패너의 <code>DistanceModelType</code>: 청취자와의 거리에 따라 음원의 음량이 줄어드는 방식(Web Audio API 1.27).
 */
public enum DistanceModelType {
    /**
     * Linear model: the gain falls in a straight line between the two distances, <code>1 - f (clamp(d, dref', dmax') - dref') / (dmax' - dref')</code>.
     * <p>
     * 선형: <code>1 - f (clamp(d, dref', dmax') - dref') / (dmax' - dref')</code>.
     */
    LINEAR("linear"),
    /**
     * Inverse model: the gain is the reference distance over the distance, <code>dref / (dref + f (max(d, dref) - dref))</code>.
     * <p>
     * 역수: <code>dref / (dref + f (max(d, dref) - dref))</code>.
     */
    INVERSE("inverse"),
    /**
     * Exponential model: the gain falls with a power of the distance, <code>(max(d, dref) / dref) ^ -f</code>.
     * <p>
     * 지수: <code>(max(d, dref) / dref) ^ -f</code>.
     */
    EXPONENTIAL("exponential");

    /**
     * The name used by the script layer.
     * <p>
     * 스크립트 계층이 쓰는 이름.
     */
    private final String scriptName;

    /**
     * Creates a constant.
     * <p>
     * 상수를 만든다.
     *
     * @param scriptName the name used by scripts / 스크립트가 쓰는 이름
     */
    DistanceModelType(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name used by scripts (<code>"linear"</code>, <code>"inverse"</code>, <code>"exponential"</code>).
     * <p>
     * 스크립트가 쓰는 이름(<code>"linear"</code>, <code>"inverse"</code>, <code>"exponential"</code>)을 반환한다.
     *
     * @return the name / 이름
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Finds the constant of a script name.
     * <p>
     * 스크립트 이름에 해당하는 상수를 찾는다.
     *
     * @param name the script name / 스크립트 이름
     * @return the constant, or null if the name is unknown / 상수. 이름을 모르면 null
     */
    public static DistanceModelType fromScriptName(String name) {
        DistanceModelType[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
