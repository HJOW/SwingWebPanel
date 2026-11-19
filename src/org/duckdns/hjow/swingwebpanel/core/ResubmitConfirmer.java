package org.duckdns.hjow.swingwebpanel.core;

/**
 * Asks the user whether a form may be sent again. A page that a form submission (<code>POST</code>) produced is never sent again
 * automatically: showing it again from the history uses the stored source, and a refresh sends the form a second time only if this
 * confirmer says yes. The component installs one that shows a confirmation dialog; without a confirmer (or in a headless environment) the
 * answer is no.
 * <p>
 * 폼을 다시 보내도 되는지 사용자에게 묻는다. 폼 제출(<code>POST</code>)이 만든 페이지는 자동으로 다시 보내지 않는다. 이력에서 다시 보여 줄 때는 저장한
 * 원문을 쓰고, 새로고침은 이 확인기가 승낙할 때만 폼을 두 번째로 보낸다. 컴포넌트는 확인 대화상자를 보이는 확인기를 설치하며, 확인기가 없거나 헤드리스
 * 환경이면 대답은 아니오이다.
 */
public interface ResubmitConfirmer {

    /**
     * Asks whether the form that produced the shown page may be sent again. Called on the event dispatch thread.
     * <p>
     * 지금 보이는 페이지를 만든 폼을 다시 보내도 되는지 묻는다. 이벤트 디스패치 스레드에서 호출된다.
     *
     * @param url the address that the form was sent to / 폼을 보낸 주소
     * @return true to send the form again / 폼을 다시 보내려면 true
     */
    boolean confirmResubmit(String url);
}
