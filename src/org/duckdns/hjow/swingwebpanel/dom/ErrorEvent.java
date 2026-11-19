package org.duckdns.hjow.swingwebpanel.dom;

/**
 * The <code>error</code> event of a runtime script error (<code>ErrorEvent</code>, HTML Standard "Runtime script errors"): it carries the
 * <code>message</code>, the <code>filename</code> of the script, the line and the column (<code>lineno</code>, <code>colno</code>) and the
 * thrown value (<code>error</code>). A worker fires one at its own global scope for an uncaught error, and then at the <code>Worker</code>
 * object of the page (with a null <code>error</code>, because the thrown value cannot leave the worker). The thrown value is an arbitrary
 * object; the JavaScript layer keeps a script value in it.
 * <p>
 * 실행 중 스크립트 오류의 <code>error</code> 이벤트(<code>ErrorEvent</code>, HTML 표준 "Runtime script errors"): <code>message</code>, 스크립트의
 * <code>filename</code>, 줄과 열(<code>lineno</code>, <code>colno</code>), 던져진 값(<code>error</code>)을 가진다. worker 는 잡히지 않은 오류에 대해
 * 자기 전역 범위에 하나를 발생시키고, 이어서 페이지의 <code>Worker</code> 객체에도 발생시킨다(던져진 값은 worker 밖으로 나갈 수 없으므로
 * <code>error</code> 는 null). 던져진 값은 임의의 객체이며 JavaScript 계층은 스크립트 값을 담는다.
 */
public class ErrorEvent extends Event {

    /**
     * The error message.
     * <p>
     * 오류 메시지.
     */
    private String message = "";

    /**
     * The name of the script file (the URL) in which the error happened.
     * <p>
     * 오류가 일어난 스크립트 파일의 이름(URL).
     */
    private String filename = "";

    /**
     * The line number of the error (1 based, 0 if unknown).
     * <p>
     * 오류의 줄 번호(1부터, 모르면 0).
     */
    private int lineno;

    /**
     * The column number of the error (1 based, 0 if unknown).
     * <p>
     * 오류의 열 번호(1부터, 모르면 0).
     */
    private int colno;

    /**
     * The thrown value, or null.
     * <p>
     * 던져진 값. 없으면 null.
     */
    private Object error;

    /**
     * Creates an event without any error information.
     * <p>
     * 오류 정보가 없는 이벤트를 만든다.
     *
     * @param type       event type (normally <code>error</code>) / 이벤트 종류(보통 <code>error</code>)
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public ErrorEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Sets the content of the event (before it is dispatched).
     * <p>
     * 이벤트의 내용을 지정한다(전달하기 전에).
     *
     * @param newMessage  the error message / 오류 메시지
     * @param newFilename the script file name / 스크립트 파일 이름
     * @param newLine     the line number / 줄 번호
     * @param newColumn   the column number / 열 번호
     * @param newError    the thrown value or null / 던져진 값. 없으면 null
     */
    public void setContent(String newMessage, String newFilename, int newLine, int newColumn, Object newError) {
        this.message = newMessage == null ? "" : newMessage;
        this.filename = newFilename == null ? "" : newFilename;
        this.lineno = newLine;
        this.colno = newColumn;
        this.error = newError;
    }

    /**
     * Returns the error message.
     * <p>
     * 오류 메시지를 반환한다.
     *
     * @return the message, empty if there is none / 메시지. 없으면 빈 문자열
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the name of the script file in which the error happened.
     * <p>
     * 오류가 일어난 스크립트 파일의 이름을 반환한다.
     *
     * @return the file name or URL, empty if unknown / 파일 이름이나 URL. 모르면 빈 문자열
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the line number.
     * <p>
     * 줄 번호를 반환한다.
     *
     * @return the line (1 based), 0 if unknown / 줄(1부터). 모르면 0
     */
    public int getLineno() {
        return lineno;
    }

    /**
     * Returns the column number.
     * <p>
     * 열 번호를 반환한다.
     *
     * @return the column (1 based), 0 if unknown / 열(1부터). 모르면 0
     */
    public int getColno() {
        return colno;
    }

    /**
     * Returns the thrown value.
     * <p>
     * 던져진 값을 반환한다.
     *
     * @return the value (a script value in the JavaScript layer), or null / 값(JavaScript 계층에서는 스크립트 값). 없으면 null
     */
    public Object getError() {
        return error;
    }
}
