package org.duckdns.hjow.swingwebpanel.download;

import java.io.File;

/**
 * What a popup may ask the download manager to do. The manager passes an instance to the popup it creates; the methods may be called on
 * any thread.
 * <p>
 * 팝업이 다운로드 관리자에게 요청할 수 있는 것. 관리자는 만든 팝업에 이 객체를 넘기며 메서드는 어느 스레드에서 호출해도 된다.
 */
public interface DownloadControl {

    /**
     * Cancels the download; a partial file is deleted.
     * <p>
     * 다운로드를 취소한다. 부분 파일은 지운다.
     */
    void cancel();

    /**
     * Changes the file the download is saved to (takes effect when the transfer finishes).
     * <p>
     * 다운로드가 저장될 파일을 바꾼다(전송이 끝날 때 적용된다).
     *
     * @param target the new target file / 새 대상 파일
     */
    void changeTarget(File target);

    /**
     * Returns the current target file.
     * <p>
     * 현재 대상 파일을 반환한다.
     *
     * @return target / 대상
     */
    File getTarget();

    /**
     * Tells the download that the user is choosing another target now (<code>true</code>) or has finished choosing (<code>false</code>). While it
     * holds, a transfer that finishes waits before it moves the file to the target, so that the choice of the user applies even if the data
     * arrived meanwhile; a popup calls it around its file chooser (call {@link #changeTarget(File)} before releasing the hold). Cancelling
     * ends the wait as well.
     * <p>
     * 사용자가 지금 다른 대상을 고르고 있음(<code>true</code>) 또는 고르기를 마쳤음(<code>false</code>)을 다운로드에 알린다. 붙잡혀 있는 동안 끝난 전송은 파일을
     * 대상으로 옮기기 전에 기다리므로, 그 사이에 데이터가 다 와도 사용자의 선택이 적용된다. 팝업이 파일 선택기 앞뒤로 부른다(붙잡기를 풀기 전에
     * {@link #changeTarget(File)} 을 부른다). 취소해도 기다림이 끝난다.
     *
     * @param holding true while the user chooses / 사용자가 고르는 동안 true
     */
    void holdForChoice(boolean holding);
}
