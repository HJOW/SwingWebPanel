package org.duckdns.hjow.swingwebpanel;

import java.util.EventListener;

/**
 * Receives the output of the page scripts' <code>console</code> object: <code>console.log</code>, <code>console.error</code> and the other
 * console methods of the main document, of its frames and of its workers, plus the problems the browser reports for the scripts (uncaught
 * exceptions, unhandled promise rejections). Register it with {@link SwingWebPanel#addConsoleListener(ConsoleListener)}. The method is
 * always called on the event dispatch thread, in the order the messages were written; an exception it throws does not reach the other
 * listeners or the page. Without a listener the messages are discarded. Typical use: an anonymous class that appends
 * {@link ConsoleEvent#getMessage()} to a log area or to standard output.
 * <p>
 * 페이지 스크립트의 <code>console</code> 객체의 출력을 받는다: 주 문서, 그 프레임, 그 worker 의 <code>console.log</code>,
 * <code>console.error</code> 등 콘솔 메서드와, 브라우저가 스크립트에 대해 보고하는 문제(잡히지 않은 예외, 처리되지 않은 promise 거부).
 * {@link SwingWebPanel#addConsoleListener(ConsoleListener)} 로 등록한다. 메서드는 항상 이벤트 디스패치 스레드에서, 메시지가 쓰인 순서대로
 * 호출되며, 던진 예외는 다른 리스너나 페이지에 전달되지 않는다. 리스너가 없으면 메시지는 버려진다. 보통 {@link ConsoleEvent#getMessage()} 를
 * 로그 영역이나 표준 출력에 덧붙이는 익명 클래스로 사용한다.
 */
public interface ConsoleListener extends EventListener {

    /**
     * Called for each console message, on the event dispatch thread.
     * <p>
     * 콘솔 메시지마다 이벤트 디스패치 스레드에서 호출된다.
     *
     * @param event the message, not null / 메시지(null 아님)
     */
    void consoleMessage(ConsoleEvent event);
}
