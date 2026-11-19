package org.duckdns.hjow.swingwebpanel.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Fetches <code>ftp://</code> URLs for the browser through {@link FtpClient} (anonymous access only).
 * <ul>
 * <li><b>Directory or file?</b> A URL whose path is empty, "/" or ends with "/" is a directory. For any other path the server's
 * <code>CWD</code> decides: if the change succeeds the URL is a directory (the reported final URL then ends with "/" so that
 * relative links resolve correctly), otherwise the path is retrieved as a file.</li>
 * <li><b>Directory</b>: the answer is the generated HTML listing ({@link FtpListing}) with media type <code>text/html</code>,
 * charset UTF-8, flagged by {@link ResourceResponse#isDirectoryListing()}.</li>
 * <li><b>File</b>: the media type is guessed from the file name ({@link MimeTypes}); unknown files are
 * <code>application/octet-stream</code>, which the content layer routes to the download popup instead of showing them.
 * The body goes to the {@link ResponseHandler} stream when it asks for one, else into memory (size limited).</li>
 * <li><b>Failures</b> (connect, login, missing path, unparsable or failed listing, failed transfer) are reported as status 404 with
 * {@link FailureKind#FTP_FAILURE} - the project shows a 404 page for FTP problems. Cancellation, memory limit and invalid
 * paths have their own kinds. A path with CR, LF or NUL is refused with {@link FailureKind#INVALID_URL} before any connection.</li>
 * </ul>
 * <p>
 * <code>ftp://</code> URL 을 {@link FtpClient} 로 가져온다(익명 접근만).
 * <ul>
 * <li><b>디렉터리인가 파일인가?</b> 경로가 비었거나 "/" 이거나 "/" 로 끝나면 디렉터리이다. 그 밖의 경로는 서버의 <code>CWD</code> 가
 * 정한다. 이동이 성공하면 디렉터리이고(상대 링크가 올바르게 풀리도록 보고하는 최종 URL 은 "/" 로 끝난다), 실패하면 파일로 받는다.</li>
 * <li><b>디렉터리</b>: 생성한 HTML 목록({@link FtpListing})을 미디어 타입 <code>text/html</code>, UTF-8 로 답하며
 * {@link ResourceResponse#isDirectoryListing()} 으로 표시한다.</li>
 * <li><b>파일</b>: 미디어 타입은 파일 이름으로 추정하고({@link MimeTypes}) 모르면 <code>application/octet-stream</code> 이며 콘텐츠 계층은
 * 이를 표시하지 않고 다운로드 팝업으로 보낸다. {@link ResponseHandler} 가 스트림을 요구하면 본문을 그쪽으로, 아니면 메모리(크기 제한)로
 * 받는다.</li>
 * <li><b>실패</b>(연결, 로그인, 없는 경로, 해석 못한/실패한 목록, 전송 실패)는 상태 404 와 {@link FailureKind#FTP_FAILURE} 로 보고한다.
 * 프로젝트는 FTP 문제를 404 화면으로 보여 준다. 취소, 메모리 제한, 잘못된 경로는 각자의 종류를 가진다. CR, LF, NUL 이 든 경로는
 * 연결하기 전에 {@link FailureKind#INVALID_URL} 로 거부한다.</li>
 * </ul>
 */
public final class FtpFetcher {

    /**
     * Timeouts and size limits.
     * <p>
     * 제한 시간과 크기 제한.
     */
    private final FetchLimits limits;

    /**
     * Creates a fetcher.
     * <p>
     * 가져오기 객체를 만든다.
     *
     * @param limits limits, null for the defaults / 제한. null 이면 기본값
     */
    public FtpFetcher(FetchLimits limits) {
        this.limits = limits == null ? FetchLimits.DEFAULT : limits;
    }

    /**
     * Fetches an ftp URL. Never throws for network problems.
     * <p>
     * ftp URL 을 가져온다. 네트워크 문제로 예외를 던지지 않는다.
     *
     * @param req     the request / 요청
     * @param handler body handler for files, may be null / 파일용 본문 처리기. null 가능
     * @return the response / 응답
     */
    public ResourceResponse fetch(ResourceRequest req, ResponseHandler handler) {
        ResourceResponse res = new ResourceResponse(req.getUrl());
        if (SchemePolicy.classify(req.getUrl()) != SchemePolicy.Kind.FTP) {
            res.setFailure(FailureKind.BLOCKED_SCHEME, "not an ftp url: " + req.getUrl());
            return res;
        }
        UrlUtil.Parts p = UrlUtil.parse(req.getUrl());
        String rawPath = p.path.length() == 0 ? "/" : p.path;
        String path = UrlUtil.percentDecode(rawPath);
        try {
            FtpClient.checkSafe(path);
        } catch (IOException e) {
            res.setFailure(FailureKind.INVALID_URL, "illegal control character in the FTP path");
            return res;
        }
        int port = p.port >= 0 ? p.port : 21;
        String base = "ftp://" + p.host + (p.port >= 0 && p.port != 21 ? ":" + p.port : "");
        CancelToken token = req.getCancelToken();
        FtpClient client = new FtpClient(p.host, port, limits.getConnectTimeoutMs(), limits.getReadTimeoutMs(), token);
        try {
            client.connect();
            boolean dirByName = path.endsWith("/");
            boolean isDir;
            if (dirByName) {
                isDir = true;
                if (!client.changeDirectory(path)) {
                    return fail(res, FailureKind.FTP_FAILURE, "directory not found: " + path);
                }
            } else {
                isDir = client.changeDirectory(path);
            }
            if (isDir) {
                return directory(res, client, base, rawPath, path);
            }
            return file(res, client, req, handler, base + rawPath, path);
        } catch (CancelledException e) {
            res.setFailure(FailureKind.CANCELLED, "request cancelled");
            return res;
        } catch (IOException e) {
            if (token.isCancelled()) {
                res.setFailure(FailureKind.CANCELLED, "request cancelled");
                return res;
            }
            return fail(res, kindOf(e), describe(e));
        } finally {
            client.close();
        }
    }

    /**
     * Maps an I/O failure to the failure kind: everything is an FTP failure except local limits.
     * <p>
     * 입출력 실패를 실패 종류로 바꾼다. 지역 제한을 뺀 모든 것이 FTP 실패이다.
     *
     * @param e failure / 실패
     * @return kind / 종류
     */
    private static FailureKind kindOf(IOException e) {
        String m = String.valueOf(e.getMessage());
        if (m.startsWith("FTP data exceeds")) {
            return FailureKind.TOO_LARGE;
        }
        if (m.startsWith("short transfer")) {
            return FailureKind.TRUNCATED;
        }
        return FailureKind.FTP_FAILURE;
    }

    /**
     * Describes a failure for the error page.
     * <p>
     * 오류 화면용으로 실패를 설명한다.
     *
     * @param e failure / 실패
     * @return description / 설명
     */
    private static String describe(IOException e) {
        if (e instanceof UnknownHostException) {
            return "unknown host " + e.getMessage();
        }
        if (e instanceof SocketTimeoutException) {
            return "FTP timeout: " + e.getMessage();
        }
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }

    /**
     * Records a failure; FTP failures are shown as 404 by the project.
     * <p>
     * 실패를 기록한다. 프로젝트는 FTP 실패를 404 로 보여 준다.
     *
     * @param res     response / 응답
     * @param kind    failure kind / 실패 종류
     * @param message message / 메시지
     * @return the response / 응답
     */
    private static ResourceResponse fail(ResourceResponse res, FailureKind kind, String message) {
        res.setFailure(kind, message);
        if (kind == FailureKind.FTP_FAILURE) {
            res.setStatus(404, "Not Found");
        }
        return res;
    }

    /**
     * Lists the current directory and builds the HTML page.
     * <p>
     * 현재 디렉터리의 목록을 받아 HTML 페이지를 만든다.
     *
     * @param res     response / 응답
     * @param client  connected client / 연결된 클라이언트
     * @param base    scheme and authority of the URL / URL 의 스킴과 authority
     * @param rawPath encoded path of the request / 요청의 인코딩된 경로
     * @param path    decoded path / 디코딩된 경로
     * @return the response / 응답
     * @throws IOException on failure / 실패 시
     */
    private ResourceResponse directory(ResourceResponse res, FtpClient client, String base, String rawPath, String path) throws IOException {
        List<FtpEntry> entries = client.list();
        String shown = path.endsWith("/") ? path : path + "/";
        String html = FtpListing.toHtml(shown, entries);
        byte[] body = html.getBytes("UTF-8");
        res.setFinalUrl(base + (rawPath.endsWith("/") ? rawPath : rawPath + "/"));
        res.setStatus(200, "OK");
        res.addHeader("Content-Type", "text/html; charset=UTF-8");
        res.setContentType("text/html", "UTF-8");
        res.setBody(body);
        res.setDeclaredLength(body.length);
        res.setDirectoryListing();
        return res;
    }

    /**
     * Retrieves a file.
     * <p>
     * 파일을 받는다.
     *
     * @param res     response / 응답
     * @param client  connected client / 연결된 클라이언트
     * @param req     request / 요청
     * @param handler body handler or null / 본문 처리기 또는 null
     * @param url     final URL / 최종 URL
     * @param path    decoded path / 디코딩된 경로
     * @return the response / 응답
     * @throws IOException on failure / 실패 시
     */
    private ResourceResponse file(ResourceResponse res, FtpClient client, ResourceRequest req, ResponseHandler handler,
            String url, String path) throws IOException {
        long size = client.size(path);
        String mime = MimeTypes.fromFileName(path);
        res.setFinalUrl(url);
        res.setStatus(200, "OK");
        res.addHeader("Content-Type", mime);
        res.setContentType(mime, null);
        res.setDeclaredLength(size);
        OutputStream sink = handler == null ? null : handler.onResponse(res);
        ByteArrayOutputStream mem = sink == null ? new ByteArrayOutputStream() : null;
        long max = sink == null ? limits.getMaxMemoryBytes() : -1;
        if (sink == null && size > max) {
            res.setStatus(0, "");
            return fail(res, FailureKind.TOO_LARGE, "file size " + size + " exceeds " + max);
        }
        long received;
        try {
            received = client.retrieve(path, sink != null ? sink : mem, max, req.getProgressListener(), size);
            if (sink != null) {
                sink.flush();
            }
        } finally {
            if (sink != null) {
                sink.close();
            }
        }
        if (size >= 0 && received != size) {
            throw new IOException("short transfer: received " + received + " of " + size + " bytes");
        }
        if (sink != null) {
            res.setStreamed(received);
        } else {
            res.setBody(mem.toByteArray());
        }
        return res;
    }
}
