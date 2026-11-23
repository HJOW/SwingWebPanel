package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>ChannelCountMode</code> of an audio node: how the number of channels of an input is decided from its connections and the
 * <code>channelCount</code> of the node (Web Audio API 1.5, <code>ChannelCountMode</code>).
 * <p>
 * 오디오 노드의 <code>ChannelCountMode</code>: 입력의 채널 수를 연결과 노드의 <code>channelCount</code> 로 정하는 방식(Web Audio API 1.5,
 * <code>ChannelCountMode</code>).
 */
public enum ChannelCountMode {
    /**
     * The maximum of the channels of all connections; <code>channelCount</code> is ignored.
     * <p>
     * 모든 연결의 채널 수 중 최댓값이며 <code>channelCount</code> 는 무시한다.
     */
    MAX("max"),
    /**
     * The maximum of the connections, clamped to <code>channelCount</code>.
     * <p>
     * 연결의 최댓값을 <code>channelCount</code> 로 제한한 값.
     */
    CLAMPED_MAX("clamped-max"),
    /**
     * Exactly the number of channels that <code>channelCount</code> gives.
     * <p>
     * 정확히 <code>channelCount</code> 가 정하는 채널 수.
     */
    EXPLICIT("explicit");

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
    ChannelCountMode(String scriptName) {
        this.scriptName = scriptName;
    }

    /**
     * Returns the name used by scripts (<code>"max"</code>, <code>"clamped-max"</code>, <code>"explicit"</code>).
     * <p>
     * 스크립트가 쓰는 이름(<code>"max"</code>, <code>"clamped-max"</code>, <code>"explicit"</code>)을 반환한다.
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
    public static ChannelCountMode fromScriptName(String name) {
        ChannelCountMode[] all = values();
        // 이름이 같은 상수를 찾는다.
        for (int i = 0; i < all.length; i++) {
            if (all[i].scriptName.equals(name)) {
                return all[i];
            }
        }
        return null;
    }
}
