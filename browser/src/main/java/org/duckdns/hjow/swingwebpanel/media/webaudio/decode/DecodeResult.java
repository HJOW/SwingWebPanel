package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;

/**
 * The result of a decoding that succeeded: the {@link AudioBuffer} and, if the data was damaged or cut off, the warning that the script layer shows
 * to the author (the Web Audio API 1.1 draft encourages implementations to warn authors in case of a corrupted file whose decoding can otherwise proceed;
 * a browser shows it in its developer tools, this component has no log, so the caller learns it here). The buffer holds everything that could be
 * decoded; whether it is complete is told by {@link #isTruncated()}. A result is immutable.
 * <p>
 * 성공한 디코딩의 결과: {@link AudioBuffer} 와, 데이터가 손상되었거나 잘렸으면 스크립트 계층이 작성자에게 보여 줄 경고(Web Audio API 1.1 초안은 손상되었지만
 * 디코딩을 계속할 수 있는 파일에 대해 구현이 작성자에게 경고하도록 권장한다. 브라우저는 개발자 도구에 보여 주며, 이 컴포넌트에는 로그가 없으므로 호출한 쪽이 여기서
 * 알게 된다). 버퍼는 디코딩할 수 있었던 모든 것을 담는다. 완전한지는 {@link #isTruncated()} 가 알려 준다. 결과는 불변이다.
 */
public final class DecodeResult {

    /**
     * The decoded buffer.
     * <p>
     * 디코딩한 버퍼.
     */
    private final AudioBuffer buffer;

    /**
     * The warning, or null if the whole data was decoded.
     * <p>
     * 경고. 데이터 전체를 디코딩했으면 null.
     */
    private final String warning;

    /**
     * True if the decoding ended before the end of the data.
     * <p>
     * 데이터의 끝보다 일찍 디코딩이 끝났으면 true.
     */
    private final boolean truncated;

    /**
     * The failure that ended the decoding early, or null.
     * <p>
     * 디코딩을 일찍 끝낸 실패. 없으면 null.
     */
    private final Throwable cause;

    /**
     * Creates a result.
     * <p>
     * 결과를 만든다.
     *
     * @param buffer    the buffer / 버퍼
     * @param truncated true if the decoding ended before the end of the data / 데이터의 끝보다 일찍 디코딩이 끝났으면 true
     * @param warning   the warning or null / 경고 또는 null
     * @param cause     the failure that ended the decoding early or null / 디코딩을 일찍 끝낸 실패 또는 null
     */
    DecodeResult(AudioBuffer buffer, boolean truncated, String warning, Throwable cause) {
        this.buffer = buffer;
        this.truncated = truncated;
        this.warning = warning;
        this.cause = cause;
    }

    /**
     * Returns the decoded buffer (at least one frame).
     * <p>
     * 디코딩한 버퍼(프레임 1 개 이상)를 반환한다.
     *
     * @return the buffer / 버퍼
     */
    public AudioBuffer getBuffer() {
        return buffer;
    }

    /**
     * Tells whether the decoding ended before the end of the data: the decoder reported damaged data after it had delivered frames (the frames before
     * the damage are in the buffer), or fewer frames were delivered than the file announces (a cut-off file).
     * <p>
     * 데이터의 끝보다 일찍 디코딩이 끝났는지 알려 준다: 디코더가 프레임을 낸 뒤에 손상된 데이터를 보고했거나(손상 앞의 프레임은 버퍼에 있다) 파일이 알리는 것보다 적은
     * 프레임이 나왔다(잘린 파일).
     *
     * @return true if the buffer is only the part that could be decoded / 버퍼가 디코딩할 수 있었던 부분뿐이면 true
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Returns the warning for the author, or null if the data was decoded completely and cleanly: a truncated result has one, and so has a result in which the
     * decoder produced samples that are not finite numbers (damage; they were replaced by silence).
     * <p>
     * 작성자에게 줄 경고를 반환한다. 데이터를 완전하고 깨끗하게 디코딩했으면 null. 잘린 결과는 경고가 있고, 디코더가 유한하지 않은 수의 표본을 만든(손상. 무음으로 바꾸었다)
     * 결과도 경고가 있다.
     *
     * @return the warning or null / 경고 또는 null
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Returns the failure of the decoder that ended the decoding early (a <code>MediaException</code>), or null if the data was complete or only shorter
     * than announced.
     * <p>
     * 디코딩을 일찍 끝낸 디코더의 실패(<code>MediaException</code>)를 반환한다. 데이터가 완전했거나 알린 것보다 짧기만 했으면 null.
     *
     * @return the failure or null / 실패 또는 null
     */
    public Throwable getCause() {
        return cause;
    }
}
