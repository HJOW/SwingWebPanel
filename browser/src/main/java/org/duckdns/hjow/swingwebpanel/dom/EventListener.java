package org.duckdns.hjow.swingwebpanel.dom;

/**
 * A callback that receives DOM events. The JavaScript layer wraps script functions and objects with a <code>handleEvent</code> method
 * in implementations of this interface; Java code (the library itself, tests, hosts) can implement it directly. Equality of listeners
 * uses <code>equals</code>, so wrappers of the same script function must be equal to each other for duplicate registrations to be ignored.
 * <p>
 * DOM 이벤트를 받는 콜백. JavaScript 계층은 스크립트 함수와 <code>handleEvent</code> 메서드를 가진 객체를 이 인터페이스의 구현으로 감싼다.
 * Java 코드(라이브러리 자신, 시험, 호스트)가 직접 구현해도 된다. 리스너의 동등성은 <code>equals</code> 를 쓰므로, 같은 스크립트 함수를 감싼
 * 객체들은 서로 같아야 중복 등록이 무시된다.
 */
public interface EventListener {

    /**
     * Handles an event. A failure (exception) is reported to the document's {@link ListenerErrorHandler} and does not stop the
     * remaining listeners.
     * <p>
     * 이벤트를 처리한다. 실패(예외)는 문서의 {@link ListenerErrorHandler} 에 보고되며 나머지 리스너의 실행을 막지 않는다.
     *
     * @param event the event being dispatched / 전달 중인 이벤트
     */
    void handleEvent(Event event);
}
