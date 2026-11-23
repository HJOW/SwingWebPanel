package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>OscillatorType</code> of an {@link OscillatorNode}: the shape of the periodic waveform (Web Audio API 1.26).
 * <p>
 * {@link OscillatorNode} 의 <code>OscillatorType</code>: 주기 파형의 모양(Web Audio API 1.26).
 */
public enum OscillatorType {
    /**
     * A sine wave.
     * <p>
     * 사인파.
     */
    SINE("sine"),
    /**
     * A square wave of duty period 0.5.
     * <p>
     * 듀티 0.5 의 사각파.
     */
    SQUARE("square"),
    /**
     * A sawtooth wave.
     * <p>
     * 톱니파.
     */
    SAWTOOTH("sawtooth"),
    /**
     * A triangle wave.
     * <p>
     * 삼각파.
     */
    TRIANGLE("triangle"),
    /**
     * A custom periodic wave set with {@link OscillatorNode#setPeriodicWave(PeriodicWave)}.
     * <p>
     * {@link OscillatorNode#setPeriodicWave(PeriodicWave)} 로 정한 임의의 주기파.
     */
    CUSTOM("custom");

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
    OscillatorType(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name used by scripts.
     * <p>
     * 스크립트가 쓰는 이름을 반환한다.
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
    public static OscillatorType fromScriptName(String name) {
        OscillatorType[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
