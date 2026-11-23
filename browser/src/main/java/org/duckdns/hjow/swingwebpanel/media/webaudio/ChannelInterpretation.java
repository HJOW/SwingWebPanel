package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>ChannelInterpretation</code> of an audio node: whether channels are mixed with the speaker layout rules or one by one
 * (Web Audio API 1.5, <code>ChannelInterpretation</code>).
 * <p>
 * 오디오 노드의 <code>ChannelInterpretation</code>: 채널을 스피커 배치 규칙으로 섞는지 하나씩 대응시키는지(Web Audio API 1.5,
 * <code>ChannelInterpretation</code>).
 */
public enum ChannelInterpretation {
    /**
     * Up-mix and down-mix with the equations of the speaker layouts (mono, stereo, quad, 5.1); other counts are mixed as discrete.
     * <p>
     * 스피커 배치(모노, 스테레오, 쿼드, 5.1)의 식으로 업믹스/다운믹스한다. 다른 채널 수는 discrete 로 섞는다.
     */
    SPEAKERS("speakers"),
    /**
     * Fill the channels one by one and drop or zero the rest.
     * <p>
     * 채널을 하나씩 채우고 남는 채널은 버리거나 0 으로 둔다.
     */
    DISCRETE("discrete");

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
    ChannelInterpretation(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name used by scripts (<code>"speakers"</code>, <code>"discrete"</code>).
     * <p>
     * 스크립트가 쓰는 이름(<code>"speakers"</code>, <code>"discrete"</code>)을 반환한다.
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
    public static ChannelInterpretation fromScriptName(String name) {
        ChannelInterpretation[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
