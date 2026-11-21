package org.duckdns.hjow.swingwebpanel.media.webaudio;

/**
 * The failure of an audio worklet processor (Web Audio API 1.32): the constructor or the <code>process()</code> method of the processor threw, or a
 * limit stopped it. It carries what the <code>processorerror</code> event reports (the <code>ErrorEvent</code> attributes <code>message</code>,
 * <code>filename</code>, <code>lineno</code> and <code>colno</code>) as plain text and numbers, so that the script layer can build the event on the
 * control thread without touching the realm of the processor, which belongs to the rendering side.
 * <p>
 * 오디오 worklet 프로세서의 실패(Web Audio API 1.32): 프로세서의 생성자나 <code>process()</code> 메서드가 예외를 던졌거나 한도가 중단시켰다.
 * <code>processorerror</code> 이벤트가 보고하는 것(<code>ErrorEvent</code> 의 <code>message</code>, <code>filename</code>, <code>lineno</code>,
 * <code>colno</code>)을 문자열과 숫자로 담고 있으므로, 스크립트 계층은 렌더링 쪽이 소유한 프로세서의 realm 을 건드리지 않고 제어 스레드에서 이벤트를 만들 수 있다.
 */
public final class WorkletProcessorException extends RuntimeException {

    /**
     * The version of the serialized form (the class is not meant to be serialized).
     * <p>
     * 직렬화 형식의 버전(이 클래스는 직렬화하려는 것이 아니다).
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the script that threw, or an empty string.
     * <p>
     * 던진 스크립트의 이름. 없으면 빈 문자열.
     */
    private final String filename;

    /**
     * The line, 0 if unknown.
     * <p>
     * 줄. 모르면 0.
     */
    private final int line;

    /**
     * The column, 0 if unknown.
     * <p>
     * 열. 모르면 0.
     */
    private final int column;

    /**
     * Creates the failure.
     * <p>
     * 실패를 만든다.
     *
     * @param message  the message of the error / 오류 메시지
     * @param filename the name of the script that threw, null for none / 던진 스크립트의 이름. 없으면 null
     * @param line     the line, 0 if unknown / 줄. 모르면 0
     * @param column   the column, 0 if unknown / 열. 모르면 0
     */
    public WorkletProcessorException(String message, String filename, int line, int column) {
        super(message == null ? "" : message);
        this.filename = filename == null ? "" : filename;
        this.line = line;
        this.column = column;
    }

    /**
     * Returns the name of the script that threw.
     * <p>
     * 던진 스크립트의 이름을 반환한다.
     *
     * @return the name, an empty string for none / 이름. 없으면 빈 문자열
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the line of the error.
     * <p>
     * 오류의 줄을 반환한다.
     *
     * @return the line, 0 if unknown / 줄. 모르면 0
     */
    public int getLine() {
        return line;
    }

    /**
     * Returns the column of the error.
     * <p>
     * 오류의 열을 반환한다.
     *
     * @return the column, 0 if unknown / 열. 모르면 0
     */
    public int getColumn() {
        return column;
    }
}
