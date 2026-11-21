package org.duckdns.hjow.swingwebpanel.media.webaudio;

import org.duckdns.hjow.swingwebpanel.media.webaudio.dsp.BiquadCoefficients;

/**
 * The <code>BiquadFilterType</code> of a {@link BiquadFilterNode} (Web Audio API 1.13).
 * <p>
 * {@link BiquadFilterNode} 의 필터 종류 <code>BiquadFilterType</code>(Web Audio API 1.13).
 */
public enum BiquadFilterType {
    /**
     * A second-order resonant low-pass filter (12 dB/octave).
     * <p>
     * 2차 공진 저역 통과 필터(12 dB/옥타브).
     */
    LOWPASS("lowpass", BiquadCoefficients.LOWPASS),
    /**
     * A second-order resonant high-pass filter (12 dB/octave).
     * <p>
     * 2차 공진 고역 통과 필터(12 dB/옥타브).
     */
    HIGHPASS("highpass", BiquadCoefficients.HIGHPASS),
    /**
     * A second-order band-pass filter.
     * <p>
     * 2차 대역 통과 필터.
     */
    BANDPASS("bandpass", BiquadCoefficients.BANDPASS),
    /**
     * A second-order low shelf filter.
     * <p>
     * 2차 저역 선반 필터.
     */
    LOWSHELF("lowshelf", BiquadCoefficients.LOWSHELF),
    /**
     * A second-order high shelf filter.
     * <p>
     * 2차 고역 선반 필터.
     */
    HIGHSHELF("highshelf", BiquadCoefficients.HIGHSHELF),
    /**
     * A peaking filter.
     * <p>
     * 피킹 필터.
     */
    PEAKING("peaking", BiquadCoefficients.PEAKING),
    /**
     * A notch (band-reject) filter.
     * <p>
     * 노치(대역 저지) 필터.
     */
    NOTCH("notch", BiquadCoefficients.NOTCH),
    /**
     * A second-order all-pass filter.
     * <p>
     * 2차 전역 통과 필터.
     */
    ALLPASS("allpass", BiquadCoefficients.ALLPASS);

    /**
     * The name used by the script layer.
     * <p>
     * 스크립트 계층이 쓰는 이름.
     */
    private final String scriptName;

    /**
     * The type constant of {@link BiquadCoefficients}.
     * <p>
     * {@link BiquadCoefficients} 의 종류 상수.
     */
    private final int kernelType;

    /**
     * Creates a constant.
     * <p>
     * 상수를 만든다.
     *
     * @param scriptName the name used by scripts / 스크립트가 쓰는 이름
     * @param kernelType the constant of the coefficient class / 계수 클래스의 상수
     */
    BiquadFilterType(String scriptName, int kernelType) {
        this.scriptName = scriptName;
        this.kernelType = kernelType;
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
     * Returns the type constant of {@link BiquadCoefficients}.
     * <p>
     * {@link BiquadCoefficients} 의 종류 상수를 반환한다.
     *
     * @return the constant / 상수
     */
    int getKernelType() {
        return kernelType;
    }

    /**
     * Finds the constant of a script name.
     * <p>
     * 스크립트 이름에 해당하는 상수를 찾는다.
     *
     * @param name the script name / 스크립트 이름
     * @return the constant, or null if the name is unknown / 상수. 이름을 모르면 null
     */
    public static BiquadFilterType fromScriptName(String name) {
        BiquadFilterType[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
