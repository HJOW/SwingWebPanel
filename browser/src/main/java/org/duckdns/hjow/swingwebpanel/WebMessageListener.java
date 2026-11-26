package org.duckdns.hjow.swingwebpanel;

import java.util.EventListener;

/**
 * Receives a page's message on the Swing event dispatch thread.
 * Check the sender origin and data before acting on it.
 * <p>
 * Swing 이벤트 디스패치 스레드에서 페이지 메시지를 받는다. 처리하기 전에 송신 오리진과 데이터를 검사한다.
 */
public interface WebMessageListener extends EventListener {

    /**
     * Receives one copied message from a browser document.
     * <p>
     * 브라우저 문서가 보낸 복사 메시지 하나를 받는다.
     *
     * @param event the received message / 받은 메시지
     */
    void messageReceived(WebMessageEvent event);
}
