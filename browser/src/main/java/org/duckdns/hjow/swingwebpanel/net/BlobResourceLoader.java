package org.duckdns.hjow.swingwebpanel.net;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Supplies registered blob URLs to the component's request layer without a network connection.
 * <p>
 * 네트워크에 연결하지 않고 컴포넌트의 요청 계층에 등록된 blob URL을 제공한다.
 */
final class BlobResourceLoader {
    /** URL store of this component. / 이 컴포넌트의 URL 저장소. */
    private final BlobUrlStore store;
    /** Limits for buffered responses. / 버퍼 응답 제한. */
    private final FetchLimits limits;

    /**
     * Associates the local loader with a component's store and response limits.
     * <p>
     * 로컬 로더를 컴포넌트의 저장소와 응답 제한에 연결한다.
     *
     * @param store registered URL store / 등록 URL 저장소
     * @param limits limits for buffered responses / 버퍼 응답 제한
     */
    BlobResourceLoader(BlobUrlStore store, FetchLimits limits) {
        this.store = store;
        this.limits = limits;
    }

    /**
     * Loads a GET response from an accepted blob entry, or reports a load failure.
     * <p>
     * 허용한 blob 항목에서 GET 응답을 읽거나 로드 실패를 보고한다.
     *
     * @param request the resource request / 리소스 요청
     * @param handler optional body destination / 선택 본문 목적지
     * @return local response / 로컬 응답
     */
    ResourceResponse load(ResourceRequest request, ResponseHandler handler) {
        String url = request.getUrl();
        CancelToken token = request.getCancelToken();
        if (token.isCancelled()) {
            return ResourceResponse.failure(url, FailureKind.CANCELLED, "request cancelled");
        }
        if (!"GET".equals(request.getMethod())) {
            return ResourceResponse.failure(url, FailureKind.BLOCKED_SCHEME, "blob URLs require GET");
        }
        BlobUrlStore.Entry entry = request.getBlobUrlEntry();
        // 시작할 때 고정한 항목은 같은 저장소와 같은 주소에서만 사용할 수 있다.
        if (!store.owns(entry, url)) {
            entry = store.resolve(url, request.getBlobUrlScope(), request.getInitiator(), request.getPurpose() == RequestPurpose.DOCUMENT);
        }
        if (entry == null) {
            return ResourceResponse.failure(url, FailureKind.NOT_FOUND, "object URL is unavailable");
        }
        RequestHeaders headers = request.getExtraHeaders();
        String range = headers == null ? null : headers.get("Range");
        int[] interval = range == null ? new int[] { 0, entry.length() } : interval(range, entry.length());
        if (interval == null) {
            return ResourceResponse.failure(url, FailureKind.IO, "invalid or unsatisfiable blob byte range");
        }
        int start = interval[0];
        int count = interval[1];
        ResourceResponse response = new ResourceResponse(url);
        response.setFinalUrl(UrlUtil.normalize(url));
        response.setBlobUrlEntry(entry);
        response.setStatus(range == null ? 200 : 206, range == null ? "OK" : "Partial Content");
        response.setDeclaredLength(count);
        String type = entry.getMimeType().length() == 0 ? "application/octet-stream" : entry.getMimeType();
        ContentType parsed = ContentType.parse(type);
        response.setContentType(parsed.getMime(), parsed.getCharset());
        response.addHeader("Content-Type", type);
        response.addHeader("Content-Length", Integer.toString(count));
        if (range != null) {
            response.addHeader("Content-Range", "bytes " + start + "-" + (start + count - 1) + "/" + entry.length());
        }
        if (!CrossOriginPolicy.permits(request, response)) {
            return CrossOriginPolicy.blocked(response);
        }
        try {
            token.throwIfCancelled();
            OutputStream sink = handler == null ? null : handler.onResponse(response);
            // 사용자가 정한 파일로 전송할 때는 메모리 응답 제한이 적용되지 않는다.
            if (sink != null) {
                try {
                    entry.write(sink, start, count, token);
                    sink.flush();
                    response.setStreamed(count);
                } finally {
                    sink.close();
                }
            } else if (count <= limits.getMaxMemoryBytes()) {
                response.setBody(entry.copy(start, count));
            } else {
                return ResourceResponse.failure(url, FailureKind.TOO_LARGE, "blob body exceeds the memory limit");
            }
            token.throwIfCancelled();
            ProgressListener progress = request.getProgressListener();
            if (progress != null) {
                progress.onProgress(count, count);
            }
            return response;
        } catch (IOException e) {
            return ResourceResponse.failure(url, token.isCancelled() ? FailureKind.CANCELLED : FailureKind.IO, e.getMessage());
        }
    }

    /**
     * Parses a single byte range, including an open-ended range or a suffix.
     * <p>
     * 끝이 열린 범위나 접미 바이트를 포함한 단일 바이트 구간을 해석한다.
     *
     * @param value Range header / Range 헤더
     * @param length object length / 객체 길이
     * @return start and count, or null for invalid or unsatisfiable range / 시작점과 길이. 잘못되거나 범위를 넘으면 null
     */
    private static int[] interval(String value, int length) {
        String range = value.trim();
        if (length == 0 || !range.matches("bytes=(?:[0-9]+-[0-9]*|-[0-9]+)")) {
            return null;
        }
        int dash = range.indexOf('-', 6);
        try {
            long start;
            long end = length - 1L;
            if (dash == 6) {
                start = Math.max(0L, length - Long.parseLong(range.substring(dash + 1)));
            } else {
                start = Long.parseLong(range.substring(6, dash));
                if (dash + 1 < range.length()) {
                    end = Math.min(end, Long.parseLong(range.substring(dash + 1)));
                }
            }
            return start <= end ? new int[] { (int) start, (int) (end - start + 1) } : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
