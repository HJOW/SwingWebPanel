package org.duckdns.hjow.swingwebpanel.dom;

import java.util.Collections;
import java.util.List;

/**
 * The <code>message</code> event (<code>MessageEvent</code>, W3C Web Messaging 3): fired for a message of cross-document messaging
 * (<code>window.postMessage</code>) and of channel messaging (<code>MessagePort</code>). Its <code>data</code> is the cloned message, its
 * <code>origin</code> the origin of the sender (empty for channel messaging), its <code>source</code> the sending window, and its
 * <code>ports</code> the ports that came with the message. The data is an arbitrary object; the JavaScript layer keeps a script value in it.
 * <p>
 * <code>message</code> 이벤트(<code>MessageEvent</code>, W3C Web Messaging 3): 문서 간 메시징(<code>window.postMessage</code>)과 채널
 * 메시징(<code>MessagePort</code>)의 메시지에 발생한다. <code>data</code> 는 복제된 메시지, <code>origin</code> 은 보낸 쪽의 오리진(채널 메시징은 빈
 * 문자열), <code>source</code> 는 보낸 window, <code>ports</code> 는 메시지와 함께 온 포트이다. 데이터는 임의의 객체이며 JavaScript 계층은 스크립트
 * 값을 담는다.
 */
public class MessageEvent extends Event {

    /**
     * The message.
     * <p>
     * 메시지.
     */
    private Object data;

    /**
     * The origin of the sender.
     * <p>
     * 보낸 쪽의 오리진.
     */
    private String origin = "";

    /**
     * The last event id (for server-sent events; always empty here).
     * <p>
     * 마지막 이벤트 id(서버 전송 이벤트용이며 여기서는 항상 빈 문자열).
     */
    private String lastEventId = "";

    /**
     * The sender (a window or a port), or null.
     * <p>
     * 보낸 쪽(window 나 포트). 없으면 null.
     */
    private Object source;

    /**
     * The ports that came with the message.
     * <p>
     * 메시지와 함께 온 포트.
     */
    private List<?> ports = Collections.<Object>emptyList();

    /**
     * Creates an event with a null message.
     * <p>
     * 메시지가 null 인 이벤트를 만든다.
     *
     * @param type       event type / 이벤트 종류
     * @param bubbles    whether it bubbles / 버블 여부
     * @param cancelable whether it can be cancelled / 취소 가능 여부
     */
    public MessageEvent(String type, boolean bubbles, boolean cancelable) {
        super(type, bubbles, cancelable);
    }

    /**
     * Returns the message.
     * <p>
     * 메시지를 반환한다.
     *
     * @return the message, or null / 메시지. 없으면 null
     */
    public Object getData() {
        return data;
    }

    /**
     * Returns the origin of the sender.
     * <p>
     * 보낸 쪽의 오리진을 반환한다.
     *
     * @return the origin, empty if there is none / 오리진. 없으면 빈 문자열
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Returns the last event id.
     * <p>
     * 마지막 이벤트 id 를 반환한다.
     *
     * @return the id, empty if there is none / id. 없으면 빈 문자열
     */
    public String getLastEventId() {
        return lastEventId;
    }

    /**
     * Returns the sender.
     * <p>
     * 보낸 쪽을 반환한다.
     *
     * @return the window or port, or null / window 나 포트. 없으면 null
     */
    public Object getSource() {
        return source;
    }

    /**
     * Returns the ports that came with the message.
     * <p>
     * 메시지와 함께 온 포트를 반환한다.
     *
     * @return the ports (empty if there are none) / 포트(없으면 빈 목록)
     */
    public List<?> getPorts() {
        return ports;
    }

    /**
     * Sets the content of the event (before it is dispatched).
     * <p>
     * 이벤트의 내용을 지정한다(전달하기 전에).
     *
     * @param newData        the message / 메시지
     * @param newOrigin      the origin of the sender / 보낸 쪽의 오리진
     * @param newLastEventId the last event id / 마지막 이벤트 id
     * @param newSource      the sender / 보낸 쪽
     * @param newPorts       the ports, or null for none / 포트. 없으면 null
     */
    public void setContent(Object newData, String newOrigin, String newLastEventId, Object newSource, List<?> newPorts) {
        this.data = newData;
        this.origin = newOrigin == null ? "" : newOrigin;
        this.lastEventId = newLastEventId == null ? "" : newLastEventId;
        this.source = newSource;
        if (newPorts == null) {
            this.ports = Collections.<Object>emptyList();
        } else {
            this.ports = newPorts;
        }
    }
}
