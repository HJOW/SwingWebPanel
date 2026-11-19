package org.duckdns.hjow.swingwebpanel.download;

/**
 * Receives the short messages of the download manager (started, completed, failed, cancelled, refused in a headless environment) so the
 * browser component can show them in its status text. May be called on any thread.
 * <p>
 * 다운로드 관리자의 짧은 메시지(시작, 완료, 실패, 취소, 헤드리스 환경에서의 거부)를 받아 브라우저 컴포넌트가 상태 메시지로 보여 줄 수 있게 한다.
 * 어느 스레드에서나 호출될 수 있다.
 */
public interface DownloadStatusSink {

    /**
     * Reports a message.
     * <p>
     * 메시지를 보고한다.
     *
     * @param message the message / 메시지
     */
    void downloadStatus(String message);
}
