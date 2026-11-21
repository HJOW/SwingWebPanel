package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The <code>AudioContextState</code> of a context (Web Audio API 1.1, <code>AudioContextState</code>).
 * <p>
 * 컨텍스트의 <code>AudioContextState</code>(Web Audio API 1.1, <code>AudioContextState</code>).
 */
public enum ContextState {
    /**
     * The context time is not proceeding; a new context starts here.
     * <p>
     * 컨텍스트 시간이 진행하지 않는다. 새 컨텍스트는 여기서 시작한다.
     */
    SUSPENDED("suspended"),
    /**
     * Audio is being processed.
     * <p>
     * 오디오를 처리하고 있다.
     */
    RUNNING("running"),
    /**
     * The context has been released and can no longer be used to process audio.
     * <p>
     * 컨텍스트가 해제되어 더 이상 오디오를 처리할 수 없다.
     */
    CLOSED("closed"),
    /**
     * The context is halted by the system until the interruption ends.
     * <p>
     * 시스템이 중단 중이며 중단이 끝날 때까지 오디오를 처리할 수 없다.
     */
    INTERRUPTED("interrupted");

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
    ContextState(String scriptName) {
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
}
