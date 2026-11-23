package org.duckdns.hjow.swingwebpanel.core;

/**
 * Receives the status text and progress of a running navigation. The browser session ignores reports of requests that
 * are no longer current, so a late report of a cancelled navigation never changes the status bar.
 * <p>
 * 진행 중인 이동의 상태 메시지와 진행률을 받는다. 브라우저 세션은 더 이상 현재가 아닌 요청의 보고를 무시하므로
 * 취소된 이동의 늦은 보고가 상태바를 바꾸지 못한다.
 */
public interface ProgressSink {

    /**
     * Reports status and progress.
     * <p>
     * 상태와 진행률을 보고한다.
     *
     * @param requestId  id of the navigation request / 이동 요청 번호
     * @param statusText status bar text / 상태바 메시지
     * @param progress   0 to 100, or -1 when the total is unknown / 0~100, 전체 크기를 모르면 -1
     */
    void report(int requestId, String statusText, int progress);
}
