package org.duckdns.hjow.swingwebpanel.core;

import java.io.File;

/**
 * Lets the user choose the files of a file upload control. The default implementation of the component shows a Swing file chooser; a host or
 * a test replaces it to choose files without a dialog. It is called on the event dispatch thread.
 * <p>
 * 사용자가 파일 업로드 컨트롤의 파일을 고르게 한다. 컴포넌트의 기본 구현은 Swing 파일 선택기를 보여 주며, 호스트나 시험은 대화상자 없이 파일을 고르도록 이것을
 * 바꿀 수 있다. 이벤트 디스패치 스레드에서 호출된다.
 */
public interface UploadChooser {

    /**
     * Asks the user for files.
     * <p>
     * 사용자에게 파일을 묻는다.
     *
     * @param multiple true if the control accepts several files / 컨트롤이 여러 파일을 받으면 true
     * @param accept   the <code>accept</code> attribute of the control (a list of extensions and MIME types), may be empty / 컨트롤의 <code>accept</code> 속성(확장자와
     *                 MIME 타입 목록). 비어 있을 수 있다
     * @return the chosen files, or null if the user cancelled / 고른 파일. 사용자가 취소했으면 null
     */
    File[] choose(boolean multiple, String accept);
}
