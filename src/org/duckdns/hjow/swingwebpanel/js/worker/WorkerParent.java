package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.js.host.structured.ClonedValue;

/**
 * What a worker tells the page that created it. Every method is called on the thread of the worker and must only hand the data over: the
 * implementation posts a task to the page, which runs it on the thread that owns the page's realm. Nothing crosses but immutable text, numbers
 * and the serialized message.
 * <p>
 * worker 가 자신을 만든 페이지에 알리는 것. 모든 메서드는 worker 의 스레드에서 호출되며 데이터를 넘기기만 해야 한다. 구현은 페이지에 작업을 올리고
 * 페이지 realm 을 소유한 스레드가 그것을 실행한다. 불변의 문자열, 숫자, 직렬화된 메시지 외에는 아무것도 건너가지 않는다.
 */
interface WorkerParent {

    /**
     * A message that the worker posted with <code>self.postMessage</code>.
     * <p>
     * worker 가 <code>self.postMessage</code> 로 보낸 메시지.
     *
     * @param data the serialized message / 직렬화된 메시지
     */
    void messageFromWorker(ClonedValue data);

    /**
     * An uncaught error that nobody handled inside the worker; the page fires an <code>error</code> event at the <code>Worker</code> object.
     * <p>
     * worker 안에서 아무도 처리하지 않은 잡히지 않은 오류. 페이지는 <code>Worker</code> 객체에 <code>error</code> 이벤트를 발생시킨다.
     *
     * @param message  the message / 메시지
     * @param filename the script URL / 스크립트 URL
     * @param line     the line number or 0 / 줄 번호. 없으면 0
     * @param column   the column number or 0 / 열 번호. 없으면 0
     */
    void errorFromWorker(String message, String filename, int line, int column);

    /**
     * The worker script could not be fetched (or the worker could not start): the page fires a plain <code>error</code> event.
     * <p>
     * worker 스크립트를 가져올 수 없다(또는 worker 를 시작할 수 없다). 페이지는 일반 <code>error</code> 이벤트를 발생시킨다.
     *
     * @param reason a short description for the console / 콘솔용 짧은 설명
     */
    void loadFailed(String reason);

    /**
     * Console output of the worker; it goes to the console of the page.
     * <p>
     * worker 의 콘솔 출력. 페이지의 콘솔로 간다.
     *
     * @param level the level (<code>log</code>, <code>warn</code>, <code>error</code> ...) / 수준
     * @param text  the text / 문자열
     */
    void console(String level, String text);
}
