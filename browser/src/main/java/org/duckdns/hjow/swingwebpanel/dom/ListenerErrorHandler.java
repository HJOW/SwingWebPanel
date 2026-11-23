package org.duckdns.hjow.swingwebpanel.dom;

/**
 * Receives the failures of event listeners. The DOM Standard says that an error in one listener is reported and the remaining listeners
 * still run; a document therefore has a handler to which dispatch reports such errors (the JavaScript layer turns them into
 * <code>window.onerror</code> events and console messages). The default handler prints the stack trace to the standard error stream.
 * <p>
 * 이벤트 리스너의 실패를 받는다. DOM 표준에 따르면 한 리스너의 오류는 보고되고 나머지 리스너는 계속 실행된다. 그래서 문서에는 전달 중 그런 오류를
 * 보고하는 핸들러가 있다(JavaScript 계층은 이를 <code>window.onerror</code> 이벤트와 콘솔 메시지로 바꾼다). 기본 핸들러는 스택 추적을
 * 표준 오류 스트림에 출력한다.
 */
public interface ListenerErrorHandler {

    /**
     * Reports a listener failure.
     * <p>
     * 리스너 실패를 보고한다.
     *
     * @param target  the target whose listener failed / 리스너가 실패한 대상
     * @param event   the event being dispatched / 전달 중이던 이벤트
     * @param failure the failure / 실패
     */
    void listenerFailed(EventTarget target, Event event, Throwable failure);
}
