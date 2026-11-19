package org.duckdns.hjow.swingwebpanel.net;

/**
 * Receives transfer progress of a resource body. It is called from the fetching thread (not the EDT), so
 * implementations must hand over to the right thread themselves and return quickly.
 * <p>
 * 리소스 본문 전송의 진행 상황을 받는다. 가져오기 스레드(EDT 가 아님)에서 호출하므로 구현은 알맞은 스레드로 넘기는 일을
 * 스스로 하고 빨리 반환해야 한다.
 */
public interface ProgressListener {

    /**
     * Reports progress.
     * <p>
     * 진행 상황을 알린다.
     *
     * @param received bytes of the (decoded) body received so far / 지금까지 받은(해독된) 본문 바이트
     * @param total    expected total bytes of the transfer, or -1 if unknown (indeterminate) / 전송 전체 예상 바이트. 모르면 -1(불확정)
     */
    void onProgress(long received, long total);
}
