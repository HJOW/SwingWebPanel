package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>OverSampleType</code> of a wave shaper: whether the shaping curve is applied to the input directly or to a signal that is
 * up-sampled to two or four times the sample rate (Web Audio API 1.31, <code>OverSampleType</code>).
 * <p>
 * 웨이브셰이퍼의 <code>OverSampleType</code>: 변환 곡선을 입력에 바로 적용하는지, 샘플링 주파수의 두 배나 네 배로 업샘플링한 신호에 적용하는지(Web Audio
 * API 1.31, <code>OverSampleType</code>).
 */
public enum OverSampleType {
    /**
     * No oversampling.
     * <p>
     * 오버샘플링을 하지 않는다.
     */
    NONE("none", 1),
    /**
     * The curve is applied at two times the sample rate.
     * <p>
     * 곡선을 샘플링 주파수의 두 배에서 적용한다.
     */
    TWO_X("2x", 2),
    /**
     * The curve is applied at four times the sample rate.
     * <p>
     * 곡선을 샘플링 주파수의 네 배에서 적용한다.
     */
    FOUR_X("4x", 4);

    /**
     * The name used by the script layer.
     * <p>
     * 스크립트 계층이 쓰는 이름.
     */
    private final String scriptName;

    /**
     * The oversampling factor.
     * <p>
     * 오버샘플링 배수.
     */
    private final int factor;

    /**
     * Creates a constant.
     * <p>
     * 상수를 만든다.
     *
     * @param scriptName the name used by scripts / 스크립트가 쓰는 이름
     * @param factor     the oversampling factor / 오버샘플링 배수
     */
    OverSampleType(String scriptName, int factor) {
        this.scriptName = scriptName;
        this.factor = factor;
    }

    /**
     * Returns the name used by scripts (<code>"none"</code>, <code>"2x"</code>, <code>"4x"</code>).
     * <p>
     * 스크립트가 쓰는 이름(<code>"none"</code>, <code>"2x"</code>, <code>"4x"</code>)을 반환한다.
     *
     * @return the name / 이름
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Returns the oversampling factor: 1, 2 or 4.
     * <p>
     * 오버샘플링 배수(1, 2, 4)를 반환한다.
     *
     * @return the factor / 배수
     */
    public int getFactor() {
        return factor;
    }

    /**
     * Finds the constant of a script name.
     * <p>
     * 스크립트 이름에 해당하는 상수를 찾는다.
     *
     * @param name the script name / 스크립트 이름
     * @return the constant, or null if the name is unknown / 상수. 이름을 모르면 null
     */
    public static OverSampleType fromScriptName(String name) {
        OverSampleType[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
