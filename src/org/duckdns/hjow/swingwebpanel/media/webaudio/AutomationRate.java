package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>AutomationRate</code> of an audio parameter: whether its value is computed for every sample frame or once per render quantum
 * (Web Audio API 1.6, <code>AutomationRate</code>).
 * <p>
 * 오디오 파라미터의 <code>AutomationRate</code>: 값을 샘플 프레임마다 계산하는지 render quantum 마다 한 번 계산하는지(Web Audio API 1.6,
 * <code>AutomationRate</code>).
 */
public enum AutomationRate {
    /**
     * The value is taken into account for every sample frame.
     * <p>
     * 값을 샘플 프레임마다 반영한다.
     */
    A_RATE("a-rate"),
    /**
     * The value at the first sample frame of the render quantum is used for the whole block.
     * <p>
     * render quantum 의 첫 샘플 프레임에서의 값을 블록 전체에 쓴다.
     */
    K_RATE("k-rate");

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
    AutomationRate(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name used by scripts (<code>"a-rate"</code>, <code>"k-rate"</code>).
     * <p>
     * 스크립트가 쓰는 이름(<code>"a-rate"</code>, <code>"k-rate"</code>)을 반환한다.
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
    public static AutomationRate fromScriptName(String name) {
        AutomationRate[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
