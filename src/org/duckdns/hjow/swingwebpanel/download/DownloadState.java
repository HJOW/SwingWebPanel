package org.duckdns.hjow.swingwebpanel.download;

/**
 * The state of one download as shown in its popup.
 * <p>
 * 팝업에 보이는 다운로드 하나의 상태.
 */
public enum DownloadState {
    /** Connecting or waiting for the response. 연결 중이거나 응답을 기다리는 중. */
    STARTING,
    /** Receiving data. 데이터를 받는 중. */
    RUNNING,
    /** Finished and saved to the target file. 끝나서 대상 파일에 저장됨. */
    COMPLETED,
    /** Ended with an error; no file was written. 오류로 끝남. 파일은 쓰이지 않음. */
    FAILED,
    /** Cancelled by the user, by clean/dispose or by the environment. 사용자, clean/dispose, 환경에 의해 취소됨. */
    CANCELLED
}
