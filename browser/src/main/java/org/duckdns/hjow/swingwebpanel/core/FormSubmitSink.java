package org.duckdns.hjow.swingwebpanel.core;

import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;

/**
 * The receiver of the form submissions of a page. The form controls of a document build a {@link FormSubmission} (method, URL, encoded body,
 * target) when the user or a script submits a form that passed validation; the form host of the page only hands it to this sink, which turns it
 * into a navigation of the component (the history entry, the request body of a POST, the cache and refresh policy). The browser session of
 * the component is the intended implementation.
 * <p>
 * 페이지의 폼 제출을 받는 쪽. 문서의 폼 컨트롤은 사용자나 스크립트가 검증을 통과한 폼을 제출하면 {@link FormSubmission}(메서드, URL, 인코딩된 본문, 대상)을
 * 만든다. 페이지의 폼 호스트는 그것을 이 싱크에 넘기기만 하고, 싱크가 컴포넌트의 이동(이력 항목, POST 요청 본문, 캐시와 새로고침 정책)으로 바꾼다. 컴포넌트의
 * 브라우저 세션이 구현하도록 의도되었다.
 */
public interface FormSubmitSink {

    /**
     * Receives a submission. It is called on the document thread of the page that submitted the form; an implementation must not block it.
     * <p>
     * 제출을 받는다. 폼을 제출한 페이지의 문서 스레드에서 호출되며, 구현은 이 스레드를 막으면 안 된다.
     *
     * @param submission what to send, never null / 보낼 내용(null 아님)
     */
    void submit(FormSubmission submission);
}
