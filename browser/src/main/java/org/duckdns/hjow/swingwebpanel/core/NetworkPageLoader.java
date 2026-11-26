package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.io.OutputStream;
import org.duckdns.hjow.swingwebpanel.content.ContentClassifier;
import org.duckdns.hjow.swingwebpanel.content.ContentDecision;
import org.duckdns.hjow.swingwebpanel.content.ContentKind;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPage;
import org.duckdns.hjow.swingwebpanel.content.error.ErrorPageBuilder;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XmlDecoder;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XmlText;
import org.duckdns.hjow.swingwebpanel.html.DecodedText;
import org.duckdns.hjow.swingwebpanel.html.EncodingSniffer;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.CrossOriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.FailureKind;
import org.duckdns.hjow.swingwebpanel.net.HttpCache;
import org.duckdns.hjow.swingwebpanel.net.ProgressListener;
import org.duckdns.hjow.swingwebpanel.net.RequestPurpose;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.ResourceRequest;
import org.duckdns.hjow.swingwebpanel.net.ResourceResponse;
import org.duckdns.hjow.swingwebpanel.net.ResponseHandler;
import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;

/**
 * The page loader of the component: it connects the navigation machinery to the network layer. Addresses of the kinds http, https, ftp and
 * classpath are fetched with the component's user agent, extra headers and cookies through a {@link ResourceFetcher}; the result becomes
 * a {@link PageSnapshot} that keeps the received source text unchanged. Failures become error pages (server errors keep the server's
 * own body), responses that must be saved as files are reported with {@link DownloadRequiredException}, and images, audio and video
 * addresses are wrapped into a tiny generated document (as desktop browsers do). <code>about:blank</code>, <code>about:text</code> and
 * addresses with forbidden schemes are answered by the {@link DefaultPageLoader} without any connection.
 * <p>
 * 컴포넌트의 페이지 로더: 이동 장치를 네트워크 계층에 연결한다. http, https, ftp, classpath 주소는 컴포넌트의 User-Agent, 추가 헤더,
 * 쿠키로 {@link ResourceFetcher} 를 통해 가져오며, 결과는 수신 원문을 그대로 보관하는 {@link PageSnapshot} 이 된다. 실패는 오류 페이지가 되고
 * (서버 오류는 서버 자신의 본문을 유지), 파일로 저장해야 하는 응답은 {@link DownloadRequiredException} 으로 알리며, 이미지/오디오/비디오 주소는
 * (데스크톱 브라우저처럼) 작은 생성 문서로 감싼다. <code>about:blank</code>, <code>about:text</code>, 금지된 스킴의 주소는 연결 없이
 * {@link DefaultPageLoader} 가 처리한다.
 */
public final class NetworkPageLoader implements PageLoader, CacheOwner {

    /**
     * Share of the progress bar used for receiving the document body; the rest is reserved for parsing and sub-resources.
     * <p>
     * 문서 본문 수신에 쓰는 진행 막대의 비율. 나머지는 파싱과 하위 리소스를 위해 남겨 둔다.
     */
    private static final int BODY_PROGRESS_SHARE = 50;

    /**
     * Number of leading body bytes that the content decision looks at (enough to see an XHTML root element behind a long DOCTYPE).
     * <p>
     * 콘텐츠 판정이 보는 본문 앞부분의 바이트 수(긴 DOCTYPE 뒤의 XHTML 루트 요소를 볼 수 있는 크기).
     */
    private static final int HEAD_LENGTH = 4096;

    /**
     * Fetcher for all schemes.
     * <p>
     * 모든 스킴을 가져오는 가져오기 객체.
     */
    private final ResourceFetcher fetcher;

    /**
     * Cookie store of the component.
     * <p>
     * 컴포넌트의 쿠키 저장소.
     */
    private final CookieStore cookies;

    /**
     * Loader for the addresses that need no network.
     * <p>
     * 네트워크가 필요 없는 주소를 처리하는 로더.
     */
    private final DefaultPageLoader local = new DefaultPageLoader();

    /**
     * Creates the loader.
     * <p>
     * 로더를 만든다.
     *
     * @param fetcher fetcher with the component's SSL hook, not null / 컴포넌트의 SSL 훅이 연결된 가져오기 객체(null 불가)
     * @param cookies cookie store of the component, not null / 컴포넌트의 쿠키 저장소(null 불가)
     */
    public NetworkPageLoader(ResourceFetcher fetcher, CookieStore cookies) {
        if (fetcher == null || cookies == null) {
            throw new IllegalArgumentException("fetcher and cookies must not be null");
        }
        this.fetcher = fetcher;
        this.cookies = cookies;
    }

    /**
     * Removes everything from the HTTP cache of the component (used by clean and dispose).
     * <p>
     * 컴포넌트의 HTTP 캐시를 모두 비운다(clean 과 dispose 가 쓴다).
     */
    public void clearCache() {
        HttpCache c = fetcher.getCache();
        if (c != null) {
            c.clear();
        }
    }

    /**
     * Chooses the cache mode of a navigation: a forced refresh bypasses the cache (P01-09), a move in the history prefers the stored copy
     * (P19-01), everything else follows the HTTP caching rules.
     * <p>
     * 이동의 캐시 모드를 정한다. 강제 새로고침은 캐시를 우회하고(P01-09), 이력 이동은 저장본을 우선하며(P19-01), 나머지는 HTTP 캐시 규칙을 따른다.
     *
     * @param request the navigation / 이동 요청
     * @return the cache mode / 캐시 모드
     */
    static ResourceRequest.CacheMode cacheModeFor(NavigationRequest request) {
        if (request.isForce()) {
            return ResourceRequest.CacheMode.FORCE_RELOAD;
        }
        if (request.getType() == NavigationType.HISTORY) {
            return ResourceRequest.CacheMode.PREFER_CACHE;
        }
        return ResourceRequest.CacheMode.DEFAULT;
    }

    /**
     * Loads a page.
     * <p>
     * 페이지를 읽는다.
     *
     * @param request what to load / 읽을 대상
     * @return the confirmed page / 확정된 페이지
     * @throws DownloadRequiredException if the response must be downloaded / 응답을 내려받아야 할 때
     * @throws Exception                 on cancellation / 취소되었을 때
     */
    public PageSnapshot load(final NavigationRequest request) throws Exception {
        // 폼 제출(POST)이 만든 페이지는 이력 이동에서 폼을 다시 보내지 않고 저장한 원문으로 다시 보여 준다.
        PageSnapshot stored = request.getRestore();
        if (request.getType() == NavigationType.HISTORY && request.getPost() == null && stored != null && stored.getPost() != null) {
            return restoreStored(stored);
        }
        final String url = request.getUrl();
        if (!SchemePolicy.isFetchAllowed(url)) {
            return local.load(request);
        }
        final DownloadRequiredException[] download = new DownloadRequiredException[1];
        // 객체 URL의 최상위 이동만 File API의 파티션 예외를 적용한다.
        RequestPurpose purpose = request.getFrame() == null ? RequestPurpose.DOCUMENT : RequestPurpose.FRAME;
        ResourceRequest req = new ResourceRequest(url).setUserAgent(request.getUserAgent()).setExtraHeaders(request.getHeaders())
                .setCookies(cookies).setPurpose(purpose)
                .setBlobUrlEntry(request.getBlobUrlEntry())
                .setCacheMode(cacheModeFor(request))
                .setCancelToken(request.getCancelToken()).setProgressListener(new ProgressListener() {
                    /**
                     * Maps body progress to the first half of the progress bar; unknown totals are indeterminate.
                     * <p>
                     * 본문 진행률을 진행 막대의 앞쪽 절반으로 바꾼다. 전체를 모르면 불확정이다.
                     *
                     * @param received bytes received / 받은 바이트
                     * @param total    total bytes or -1 / 전체 바이트 또는 -1
                     */
                    public void onProgress(long received, long total) {
                        int percent = total > 0 ? (int) Math.min(BODY_PROGRESS_SHARE, received * BODY_PROGRESS_SHARE / total) : -1;
                        request.report("Loading " + url, percent);
                    }
                });
        FrameContext frame = request.getFrame();
        if (frame != null) {
            req.setPurpose(RequestPurpose.FRAME).setEmbedderOrigin(Origin.of(frame.owner.document.getOriginUrl()))
                    .setEmbedderPolicy(frame.owner.presentation.embedderPolicy());
        }
        applyPost(req, request.getPost());
        ResourceResponse response = fetcher.fetch(req, new ResponseHandler() {
            /**
             * Stops the request when the response must be downloaded instead of shown.
             * <p>
             * 응답을 보여 주는 대신 내려받아야 하면 요청을 중단한다.
             *
             * @param head response head / 응답 머리
             * @return always null (read into memory) / 항상 null(메모리로 읽음)
             * @throws IOException to abort when a download is required / 내려받아야 할 때 중단하려고 던진다
             */
            public OutputStream onResponse(ResourceResponse head) throws IOException {
                // download 속성이 있는 같은 오리진 링크는 응답의 형식과 관계없이 저장한다. 오류 응답은 저장하지 않고 오류 페이지로 보인다.
                DownloadRequiredException.Reason reason = request.isDownload() && head.getStatus() < 400
                        ? DownloadRequiredException.Reason.LINK_DOWNLOAD : reasonFor(head);
                if (reason != null) {
                    download[0] = new DownloadRequiredException(url, head.getFinalUrl(), head.getMimeType(),
                            dispositionFor(head, request), head.getDeclaredLength(), reason);
                    throw new IOException("download required");
                }
                return null;
            }
        });
        request.getCancelToken().throwIfCancelled();
        if (download[0] != null) {
            throw download[0];
        }
        PageSnapshot loaded = toSnapshot(url, response).withCrossOriginPolicy(CrossOriginPolicy.from(response));
        // 리디렉션을 거치지 않은 응답만 폼 제출이 만든 페이지로 기억한다. 리디렉션 뒤의 페이지는 일반 GET 페이지이다.
        if (request.getPost() != null && response.getRedirects().isEmpty() && response.getFailureKind() == FailureKind.NONE) {
            loaded = loaded.withPost(request.getPost());
        }
        return loaded;
    }

    /**
     * Builds the page of a history entry that a form submission produced, from the stored source: the form is not sent again.
     * <p>
     * 폼 제출이 만든 이력 항목의 페이지를 저장한 원문으로 만든다. 폼을 다시 보내지 않는다.
     *
     * @param stored the stored entry / 저장한 항목
     * @return a new page with the same source / 같은 원문의 새 페이지
     */
    private static PageSnapshot restoreStored(PageSnapshot stored) {
        PageSnapshot page = stored.getKind() == PageKind.ERROR
                ? PageSnapshot.error(stored.getUrl(), stored.getContent(), stored.getMimeType(), stored.getStatusCode())
                : PageSnapshot.loaded(stored.getUrl(), stored.getContent(), stored.getMimeType(), stored.getBaseUrl(), stored.getStatusCode());
        return page.withEncoding(stored.getEncoding()).withTitle(stored.getTitle()).withPost(stored.getPost())
                .withCrossOriginPolicy(stored.getCrossOriginPolicy());
    }

    /**
     * Makes a request a <code>POST</code> of the stored form data (the method, the body with its media type, and the page that contained
     * the form as the referrer); a request without form data stays a <code>GET</code>.
     * <p>
     * 요청을 저장한 폼 데이터의 <code>POST</code> 로 만든다(메서드, 미디어 타입이 있는 본문, 폼이 있던 페이지를 참조 주소로). 폼 데이터가 없는 요청은
     * <code>GET</code> 으로 남는다.
     *
     * @param req  the request / 요청
     * @param post the form data or null / 폼 데이터 또는 null
     */
    private static void applyPost(ResourceRequest req, PostData post) {
        if (post == null || !post.isResendable()) {
            return;
        }
        req.setMethod("POST").setBody(post.getBody(), post.getContentType());
        if (post.getReferrer() != null) {
            req.setReferrer(post.getReferrer());
        }
    }


    /**
     * Returns the Content-Disposition for a download: the header of the response, or, when the response has none that names a file and the
     * link suggested a name with its <code>download</code> attribute, an attachment header that carries that name (the server's name wins).
     * <p>
     * 다운로드의 Content-Disposition 을 반환한다: 응답의 헤더. 응답에 파일 이름이 있는 헤더가 없고 링크가 <code>download</code> 속성으로 이름을
     * 제안했으면 그 이름을 담은 attachment 헤더(서버가 준 이름이 우선한다).
     *
     * @param head    the response head / 응답 머리
     * @param request the navigation / 이동
     * @return the header value or null / 헤더 값 또는 null
     */
    private static String dispositionFor(ResourceResponse head, NavigationRequest request) {
        String header = head.getHeader("Content-Disposition");
        String name = request.getDownloadName();
        if (name.length() == 0 || header != null && header.toLowerCase().indexOf("filename") >= 0) {
            return header;
        }
        return attachmentName(name);
    }

    /**
     * Encodes a name suggested by an anchor as a safe attachment header.
     * <p>
     * 링크가 제안한 이름을 안전한 첨부 헤더로 인코딩한다.
     *
     * @param name the suggested name / 제안한 이름
     * @return the header, or null if no name was suggested / 헤더. 제안한 이름이 없으면 null
     */
    static String attachmentName(String name) {
        if (name == null || name.length() == 0) {
            return null;
        }
        StringBuilder safe = new StringBuilder();
        // 따옴표, 역슬래시, 제어 문자를 뺀 이름만 헤더에 넣는다. 경로 구분자는 다운로드 쪽 파일 이름 정리가 처리한다.
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c >= 0x20 && c != '"' && c != '\\' && c != 0x7f) {
                safe.append(c);
            }
        }
        return "attachment; filename=\"" + safe + "\"";
    }

    /**
     * Decides from the response head whether a response has to be downloaded. A response without a usable type is decided later, when
     * the body can be looked at.
     * <p>
     * 응답 머리만 보고 응답을 내려받아야 하는지 판단한다. 쓸 만한 타입이 없는 응답은 본문을 볼 수 있을 때 나중에 판정한다.
     *
     * @param head response head / 응답 머리
     * @return the reason, or null if the response can be shown or is decided later / 이유. 보여 줄 수 있거나 나중에 판정하면 null
     */
    private static DownloadRequiredException.Reason reasonFor(ResourceResponse head) {
        if (head.isAttachment()) {
            return DownloadRequiredException.Reason.ATTACHMENT;
        }
        if (ContentClassifier.kindOfMime(head.getMimeType()) == ContentKind.DOWNLOAD) {
            return DownloadRequiredException.Reason.NOT_DISPLAYABLE;
        }
        return null;
    }

    /**
     * Converts a finished response into a page. The content processor is chosen by {@link ContentClassifier} (also for responses without
     * a type, which are recognized from the first bytes of the body); a body that turns out not to be displayable is handed to the
     * download path.
     * <p>
     * 끝난 응답을 페이지로 바꾼다. 콘텐츠 처리기는 {@link ContentClassifier} 가 고른다(타입이 없는 응답도 본문의 첫 바이트로 알아본다). 보여 줄 수
     * 없는 것으로 밝혀진 본문은 다운로드 경로로 넘긴다.
     *
     * @param url requested address / 요청한 주소
     * @param r   the response / 응답
     * @return the page / 페이지
     * @throws DownloadRequiredException if the body can only be downloaded / 본문을 내려받을 수밖에 없을 때
     */
    private static PageSnapshot toSnapshot(String url, ResourceResponse r) throws DownloadRequiredException {
        FailureKind failure = r.getFailureKind();
        if (failure != FailureKind.NONE) {
            ErrorPage page = ErrorPageBuilder.forFailure(failure, url, r.getFailureMessage());
            return PageSnapshot.error(url, page.getHtml(), page.getMimeType(), page.getStatus()).withTitle(page.getHeading());
        }
        String finalUrl = r.getFinalUrl() == null || r.getFinalUrl().length() == 0 ? url : r.getFinalUrl();
        byte[] body = r.getBody() == null ? new byte[0] : r.getBody();
        String mime = r.getMimeType() == null ? "" : r.getMimeType();
        int status = r.getStatus();
        if (r.isHttpError()) {
            if (body.length == 0) {
                ErrorPage page = ErrorPageBuilder.http(finalUrl, status, r.getReason());
                return PageSnapshot.error(finalUrl, page.getHtml(), page.getMimeType(), page.getStatus()).withTitle(page.getHeading());
            }
            String[] text = decodeBody(body, r.getCharset(), mime.length() == 0 ? "text/html" : mime);
            return PageSnapshot.error(finalUrl, text[0], mime.length() == 0 ? "text/html" : mime, status).withEncoding(text[1]);
        }
        // FTP 디렉터리 목록은 우리가 만든 HTML 이다. 그 밖의 응답은 Content-Type 과(없으면) 본문의 첫 바이트로 처리기를 고른다.
        ContentDecision decision;
        if (r.isDirectoryListing()) {
            decision = ContentClassifier.classifyResponse("text/html", false, null);
        } else {
            String header = r.getHeader("Content-Type");
            if (header == null && mime.length() > 0) {
                header = mime;
            }
            decision = ContentClassifier.classifyResponse(header, r.isAttachment(), head(body));
        }
        switch (decision.getKind()) {
        case DOWNLOAD:
            throw new DownloadRequiredException(url, finalUrl, decision.getMime(), r.getHeader("Content-Disposition"), r.getDeclaredLength(),
                    decision.getReason() == ContentDecision.Reason.ATTACHMENT ? DownloadRequiredException.Reason.ATTACHMENT
                            : DownloadRequiredException.Reason.NOT_DISPLAYABLE);
        case IMAGE:
        case AUDIO:
        case VIDEO:
            return mediaPage(finalUrl, decision.getMime(), status);
        default:
            String shown = decision.getDisplayMime();
            String[] decoded = decodeBody(body, r.getCharset(), shown);
            return PageSnapshot.loaded(finalUrl, decoded[0], shown, finalUrl, status).withEncoding(decoded[1]);
        }
    }

    /**
     * Returns the first bytes of a body for the content decision.
     * <p>
     * 콘텐츠 판정에 쓸 본문의 첫 바이트를 반환한다.
     *
     * @param body the body / 본문
     * @return up to {@link #HEAD_LENGTH} bytes / 최대 {@link #HEAD_LENGTH} 바이트
     */
    private static byte[] head(byte[] body) {
        if (body.length <= HEAD_LENGTH) {
            return body;
        }
        byte[] head = new byte[HEAD_LENGTH];
        System.arraycopy(body, 0, head, 0, HEAD_LENGTH);
        return head;
    }

    /**
     * Decodes a response body into the received source text. HTML goes through the HTML encoding sniffing (BOM, HTTP charset,
     * <code>meta</code> prescan, UTF-8 guess; plan item P07-01), XHTML through the XML rules (BOM, HTTP charset, XML declaration; plan
     * item P17-05; a body with invalid bytes falls back to replacement characters here and the XML parser reports what it can), and
     * other types use the BOM and the HTTP charset only, so a <code>meta</code> tag inside a plain text file does not change its decoding.
     * <p>
     * 응답 본문을 수신 원문으로 디코딩한다. HTML 은 HTML 인코딩 판정(BOM, HTTP charset, <code>meta</code> 사전 검사, UTF-8 추측; 계획 P07-01)을,
     * XHTML 은 XML 규칙(BOM, HTTP charset, XML 선언; 계획 P17-05. 잘못된 바이트가 있으면 여기서는 대체 문자로 바꾸고 XML 파서가 알 수 있는 것을
     * 보고한다)을 거치며, 그 밖의 형식은 BOM 과 HTTP charset 만 쓰므로 일반 텍스트 안의 <code>meta</code> 태그가 디코딩을 바꾸지 않는다.
     *
     * @param body    body bytes / 본문 바이트
     * @param charset charset of the Content-Type header or null / Content-Type 헤더의 문자셋 또는 null
     * @param mime    the canonical media type of the processor / 처리기의 정규 미디어 타입
     * @return the decoded text and the name of the character encoding that was used / 디코딩한 텍스트와 사용한 문자 인코딩의 이름
     */
    private static String[] decodeBody(byte[] body, String charset, String mime) {
        if ("application/xhtml+xml".equals(mime)) {
            XmlText text = XmlDecoder.decode(body, charset);
            return text.hasError() ? new String[] { TextDecoder.decode(body, charset), "UTF-8" } : new String[] { text.getText(), text.getEncoding() };
        }
        if (mime.length() == 0 || "text/html".equals(mime)) {
            DecodedText decoded = EncodingSniffer.decode(body, charset);
            return new String[] { decoded.getText(), decoded.getEncoding() };
        }
        return new String[] { TextDecoder.decode(body, charset), "UTF-8" };
    }

    /**
     * Wraps an image, audio or video address into a small generated document.
     * <p>
     * 이미지, 오디오, 비디오 주소를 작은 생성 문서로 감싼다.
     *
     * @param finalUrl address of the media file / 미디어 파일의 주소
     * @param mime     MIME type / MIME 타입
     * @param status   HTTP status / HTTP 상태
     * @return the generated page / 생성한 페이지
     */
    private static PageSnapshot mediaPage(String finalUrl, String mime, int status) {
        String src = HtmlText.escape(finalUrl);
        String tag;
        if (mime.startsWith("image/")) {
            tag = "<img src=\"" + src + "\">";
        } else if (mime.startsWith("audio/")) {
            tag = "<audio controls src=\"" + src + "\"></audio>";
        } else {
            tag = "<video controls src=\"" + src + "\"></video>";
        }
        int slash = finalUrl.lastIndexOf('/');
        String name = slash >= 0 && slash < finalUrl.length() - 1 ? finalUrl.substring(slash + 1) : finalUrl;
        String html = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + HtmlText.escape(name)
                + "</title></head><body style=\"margin:0\">" + tag + "</body></html>";
        return PageSnapshot.loaded(finalUrl, html, "text/html", finalUrl, status).withTitle(name);
    }
}
