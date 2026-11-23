package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>PanningModelType</code> of a panner: the spatialization algorithm (Web Audio API 1.27).
 * <p>
 * 패너의 <code>PanningModelType</code>: 공간화 알고리즘(Web Audio API 1.27).
 */
public enum PanningModelType {
    /**
     * Simple and cheap equal power panning between the two channels, from the azimuth; the elevation is ignored.
     * <p>
     * 방위각으로 두 채널 사이를 나누는 단순하고 값싼 등전력 패닝. 고도는 무시한다.
     */
    EQUAL_POWER("equalpower"),
    /**
     * Spatialization with measured head-related impulse responses (stereo output; all parameters are read once per quantum).
     * <p>
     * 측정한 머리 임펄스 응답으로 하는 공간화(스테레오 출력. 모든 파라미터를 quantum 마다 한 번 읽는다).
     */
    HRTF("HRTF");

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
    PanningModelType(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name used by scripts (<code>"equalpower"</code>, <code>"HRTF"</code>).
     * <p>
     * 스크립트가 쓰는 이름(<code>"equalpower"</code>, <code>"HRTF"</code>)을 반환한다.
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
    public static PanningModelType fromScriptName(String name) {
        PanningModelType[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
