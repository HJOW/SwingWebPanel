package org.duckdns.hjow.swingwebpanel.download;

import java.io.File;

/**
 * The popup of one download as seen by the download manager (the object returned by
 * {@link SwingDownloadHandler#createDownloadPopup}). The real implementation is a dialog; tests and hosts can provide their own. All
 * methods are called on the event dispatch thread, except that implementations must tolerate {@link #dispose()} being called more than once.
 * <p>
 * 다운로드 관리자가 보는 다운로드 하나의 팝업({@link SwingDownloadHandler#createDownloadPopup} 이 반환하는 객체). 실제 구현은 대화상자이며 시험과
 * 호스트는 자신의 구현을 줄 수 있다. 모든 메서드는 이벤트 디스패치 스레드에서 호출되며, 구현은 {@link #dispose()} 가 여러 번 호출되어도 견뎌야 한다.
 */
public interface DownloadHandle {

    /**
     * Shows the file the download will be saved to.
     * <p>
     * 다운로드가 저장될 파일을 보여 준다.
     *
     * @param target the target file / 대상 파일
     */
    void setTarget(File target);

    /**
     * Shows the progress.
     * <p>
     * 진행 상황을 보여 준다.
     *
     * @param done  bytes received so far / 지금까지 받은 바이트
     * @param total total size in bytes or -1 if unknown (indeterminate progress) / 전체 크기(바이트). 모르면 -1(불확정 진행률)
     */
    void setProgress(long done, long total);

    /**
     * Shows the state and a short result line.
     * <p>
     * 상태와 짧은 결과 줄을 보여 준다.
     *
     * @param state   the state / 상태
     * @param message result line (for example the saved path or the error) / 결과 줄(예: 저장된 경로나 오류)
     */
    void setState(DownloadState state, String message);

    /**
     * Lets the user choose another target file.
     * <p>
     * 사용자가 다른 대상 파일을 고르게 한다.
     *
     * @param current the current target / 현재 대상
     * @return the chosen file or null if the user gave up / 고른 파일. 포기했으면 null
     */
    File chooseTarget(File current);

    /**
     * Asks whether an existing file may be replaced.
     * <p>
     * 기존 파일을 바꿔도 되는지 묻는다.
     *
     * @param existing the existing file / 기존 파일
     * @return true to replace it / 바꾸려면 true
     */
    boolean confirmOverwrite(File existing);

    /**
     * Closes the popup and releases its resources.
     * <p>
     * 팝업을 닫고 자원을 해제한다.
     */
    void dispose();
}
