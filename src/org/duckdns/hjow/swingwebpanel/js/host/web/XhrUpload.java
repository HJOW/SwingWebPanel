package org.duckdns.hjow.swingwebpanel.js.host.web;

/**
 * The Java side of <code>XMLHttpRequestUpload</code> (<code>xhr.upload</code>): the event target for the progress of sending the request
 * body. The component's request layer does not report the progress of sending, so the object receives one <code>progress</code>, one
 * <code>load</code> and one <code>loadend</code> event (and <code>loadstart</code> when the request starts) when the server starts to answer.
 * <p>
 * <code>XMLHttpRequestUpload</code>(<code>xhr.upload</code>)의 Java 쪽: 요청 본문 전송 진행을 위한 이벤트 대상. 컴포넌트의 요청 계층은 전송 진행을
 * 알리지 않으므로 서버가 응답을 시작할 때 <code>progress</code>, <code>load</code>, <code>loadend</code> 이벤트를 한 번씩(요청 시작 때는 <code>loadstart</code>)
 * 받는다.
 */
final class XhrUpload extends XhrEventTarget {

    /**
     * Creates the upload target.
     * <p>
     * 업로드 대상을 만든다.
     */
    XhrUpload() {
        super();
    }
}
