package org.duckdns.hjow.swingwebpanel.dom.html;

import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * A {@link FormHost} whose methods do nothing (submissions are dropped, invalid controls are not shown, no file can be chosen, state changes
 * are ignored). Extend it and override what a host needs.
 * <p>
 * 모든 메서드가 아무 일도 하지 않는 {@link FormHost}(제출은 버려지고, 올바르지 않은 컨트롤은 보여 주지 않고, 파일을 고를 수 없고, 상태 변경은 무시된다).
 * 이것을 상속해 호스트가 필요로 하는 것만 재정의한다.
 */
public class FormHostAdapter implements FormHost {

    /**
     * Creates the adapter.
     * <p>
     * 어댑터를 만든다.
     */
    public FormHostAdapter() {
    }

    /**
     * Drops the submission.
     * <p>
     * 제출을 버린다.
     *
     * @param submission the submission / 제출 정보
     * @param form       the form / 폼
     */
    public void submit(FormSubmission submission, Element form) {
        // 기본: 이동하지 않는다.
    }

    /**
     * Shows nothing.
     * <p>
     * 아무것도 보여 주지 않는다.
     *
     * @param control the control / 컨트롤
     * @param message the message / 메시지
     */
    public void reportInvalid(Element control, String message) {
        // 기본: 메시지를 보여 주지 않는다.
    }

    /**
     * Offers no file chooser.
     * <p>
     * 파일 선택기를 제공하지 않는다.
     *
     * @param input    the input / input
     * @param multiple several files allowed / 여러 파일 허용
     * @param accept   the accept attribute / accept 속성
     * @return null / null
     */
    public FileEntry[] chooseFiles(Element input, boolean multiple, String accept) {
        return null;
    }

    /**
     * Ignores the state change.
     * <p>
     * 상태 변경을 무시한다.
     *
     * @param element the element / 요소
     */
    public void stateChanged(Element element) {
        // 기본: 통지를 받지 않는다.
    }
}
