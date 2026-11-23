package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Lets the caller decide what happens with a body after the response head is known, without loading it first.
 * The fetch layer calls {@link #onResponse(ResourceResponse)} once for the final non-error response (status below 400)
 * with a head-only response. Returning an {@link OutputStream} makes the body stream into it without any memory limit
 * (downloads); returning <code>null</code> keeps the normal in-memory handling (documents, images, ...). The stream is
 * closed by the fetch layer. Throwing {@link IOException} aborts the request.
 * <p>
 * 응답 머리를 안 뒤에, 본문을 먼저 읽지 않고도 호출자가 본문의 처리 방식을 정하게 한다. 가져오기 계층은 최종 비오류 응답
 * (상태 400 미만)마다 본문 없는 응답으로 {@link #onResponse(ResourceResponse)} 를 한 번 호출한다. {@link OutputStream} 을 돌려주면
 * 본문이 메모리 제한 없이 그 스트림으로 흐르고(다운로드), <code>null</code> 을 돌려주면 평소처럼 메모리에 읽는다(문서, 이미지 등).
 * 스트림은 가져오기 계층이 닫는다. {@link IOException} 을 던지면 요청을 중단한다.
 */
public interface ResponseHandler {

    /**
     * Decides where the body goes.
     * <p>
     * 본문을 어디로 보낼지 정한다.
     *
     * @param head response without body (status, headers, mime, final URL) / 본문 없는 응답(상태, 헤더, MIME, 최종 URL)
     * @return stream to receive the body, or null to read it into memory / 본문을 받을 스트림. 메모리로 읽으려면 null
     * @throws IOException to abort the request / 요청을 중단하려면 던진다
     */
    OutputStream onResponse(ResourceResponse head) throws IOException;
}
