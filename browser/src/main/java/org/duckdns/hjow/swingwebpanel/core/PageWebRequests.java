package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.duckdns.hjow.swingwebpanel.js.host.StreamingWebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebResponse;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.FailureKind;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.OriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.ProgressListener;
import org.duckdns.hjow.swingwebpanel.net.RequestHeaders;
import org.duckdns.hjow.swingwebpanel.net.RequestPurpose;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.ResourceRequest;
import org.duckdns.hjow.swingwebpanel.net.ResourceResponse;
import org.duckdns.hjow.swingwebpanel.net.ResponseHandler;
import org.duckdns.hjow.swingwebpanel.net.SchemePolicy;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The request service that a page gets for <code>XMLHttpRequest</code> and <code>fetch()</code> (plan item P20-08). Every request goes through the
 * same {@link ResourceFetcher} as the main document, scripts, styles and images, so the User-Agent of the component, its additional
 * headers, the SSL hook, the HTTP cache, the redirect and size limits and the cancellation apply, and the component's cookie store is
 * used according to the credentials mode of the request. This class adds the rules of the web platform that belong to a page-made request:
 * <ul>
 * <li>only <code>http:</code> and <code>https:</code> (and <code>classpath:</code> from a class path page) may be requested; <code>ftp:</code>, <code>file:</code>,
 * <code>data:</code> and the other schemes are refused;</li>
 * <li>the same-origin policy: mode <code>same-origin</code> refuses other origins; mode <code>cors</code> needs the server's
 * <code>Access-Control-Allow-Origin</code> (and <code>Access-Control-Allow-Credentials</code> when cookies were allowed); mode <code>no-cors</code>
 * returns an opaque response; a cross-origin request with a non-safelisted method or header is announced with a CORS preflight
 * (<code>OPTIONS</code>, made by {@link ResourceFetcher}) and fails like a network error if the server does not grant it;</li>
 * <li>credentials: <code>omit</code> never sends or stores cookies, <code>same-origin</code> only for hops to the page's own origin,
 * <code>include</code> for every origin;</li>
 * <li>the filtering of the response (no <code>Set-Cookie</code>; for a CORS response only the safelisted and exposed headers) and the
 * Referer policy.</li>
 * </ul>
 * Requests run on daemon threads, at most {@link #MAX_CONCURRENT} at a time and at most {@link #MAX_PENDING} waiting; the results go to a
 * {@link WebRequestSource.Callback}. Cancelling the page's resource token cancels every request. Methods other than GET, HEAD, POST, PUT,
 * PATCH, DELETE and OPTIONS fail (PATCH goes over the socket connection of the net layer, because the JRE class refuses it), and a body is only
 * sent with POST, PUT and PATCH.
 * <p>
 * The same service streams (<code>EventSource</code>, {@link StreamingWebRequestSource}): a stream goes through the same layer and the same page rules, but
 * it is not read into memory, it needs none of the concurrent slots while it stays open (at most {@link #MAX_STREAMS} streams are open at a time) and
 * its read timeout is {@link #STREAM_READ_TIMEOUT_MS}.
 * <p>
 * 페이지가 <code>XMLHttpRequest</code> 와 <code>fetch()</code> 를 위해 받는 요청 서비스(계획 P20-08). 모든 요청은 주 문서, 스크립트, 스타일, 이미지와 같은
 * {@link ResourceFetcher} 를 거치므로 컴포넌트의 User-Agent, 추가 헤더, SSL 훅, HTTP 캐시, 리디렉션·크기 제한, 취소가 적용되고 요청의 자격 증명 모드에 따라
 * 컴포넌트의 쿠키 저장소를 쓴다. 이 클래스는 페이지가 만든 요청에 속하는 웹 플랫폼 규칙을 더한다.
 * <ul>
 * <li><code>http:</code> 와 <code>https:</code>(클래스패스 페이지에서는 <code>classpath:</code> 도)만 요청할 수 있다. <code>ftp:</code>, <code>file:</code>,
 * <code>data:</code> 등 다른 스킴은 거부한다.</li>
 * <li>동일 출처 정책: <code>same-origin</code> 모드는 다른 오리진을 거부하고, <code>cors</code> 모드는 서버의 <code>Access-Control-Allow-Origin</code>(쿠키를
 * 허용했다면 <code>Access-Control-Allow-Credentials</code> 도)이 필요하며, <code>no-cors</code> 모드는 불투명 응답을 돌려준다. 안전 목록 밖의 메서드나
 * 헤더를 가진 교차 오리진 요청은 CORS 사전 요청(<code>OPTIONS</code>, {@link ResourceFetcher} 가 보낸다)으로 미리 알리며 서버가 허락하지 않으면 네트워크 오류처럼
 * 실패한다.</li>
 * <li>자격 증명: <code>omit</code> 은 쿠키를 보내거나 저장하지 않고, <code>same-origin</code> 은 페이지 자신의 오리진으로 가는 구간에만, <code>include</code> 는 모든
 * 오리진에 쓴다.</li>
 * <li>응답 걸러내기(<code>Set-Cookie</code> 제외, CORS 응답은 안전 목록과 노출한 헤더만)와 Referer 정책.</li>
 * </ul>
 * 요청은 데몬 스레드에서 실행되며 동시에 최대 {@link #MAX_CONCURRENT} 개, 대기는 최대 {@link #MAX_PENDING} 개이다. 결과는 {@link WebRequestSource.Callback}
 * 으로 간다. 페이지의 리소스 토큰을 취소하면 모든 요청이 취소된다. GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS 이외의 메서드는
 * 실패하며(PATCH 는 JRE 클래스가 거부하므로 net 계층의 소켓 연결로 보낸다), 본문은 POST, PUT, PATCH 에만 보낸다.
 * <p>
 * 같은 서비스가 스트리밍도 한다(<code>EventSource</code>, {@link StreamingWebRequestSource}). 스트림도 같은 계층과 같은 페이지 규칙을 거치지만 메모리에 읽지 않고,
 * 열려 있는 동안 동시 요청 칸을 쓰지 않으며(동시에 열린 스트림은 최대 {@link #MAX_STREAMS} 개), 읽기 제한 시간은 {@link #STREAM_READ_TIMEOUT_MS} 이다.
 */
final class PageWebRequests implements WebRequestSource, StreamingWebRequestSource {

    /** The most requests that run at the same time. / 동시에 실행하는 최대 요청 수. */
    static final int MAX_CONCURRENT = 6;

    /** The most requests that exist (running and waiting) before new ones fail. / 새 요청이 실패하기 전까지 존재할 수 있는(실행 중과 대기) 최대 요청 수. */
    static final int MAX_PENDING = 256;

    /** The most streams that stay open at the same time. / 동시에 열려 있을 수 있는 최대 스트림 수. */
    static final int MAX_STREAMS = 6;

    /** The read timeout of a stream in milliseconds: an idle connection is dropped after this and the page reconnects. / 스트림의 읽기 제한 시간(밀리초). 이만큼 조용한 연결은 끊기고 페이지가 다시 연결한다. */
    static final int STREAM_READ_TIMEOUT_MS = 300000;

    /** The methods that the request layer can send. / 요청 계층이 보낼 수 있는 메서드. */
    private static final Set<String> METHODS = new HashSet<String>();

    static {
        String[] m = { "GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS" };
        // 보낼 수 있는 메서드를 집합에 넣는다.
        for (int i = 0; i < m.length; i++) {
            METHODS.add(m[i]);
        }
    }

    /** The component's common request layer. / 컴포넌트의 공통 요청 계층. */
    private final ResourceFetcher fetcher;

    /** The component's cookie store. / 컴포넌트의 쿠키 저장소. */
    private final CookieStore cookies;

    /** The copied request settings (User-Agent, extra headers, force). / 복사한 요청 설정(User-Agent, 추가 헤더, 강제). */
    private final PageRequestContext context;

    /** The URL of the page. / 페이지의 URL. */
    private final String pageUrl;

    /** The cancellation token of the page's resources. / 페이지 리소스의 취소 토큰. */
    private final CancelToken resources;

    /** Limits the running requests. / 실행 중인 요청 수를 제한한다. */
    private final Semaphore slots = new Semaphore(MAX_CONCURRENT, true);

    /** Counts the requests that exist. / 존재하는 요청 수를 센다. */
    private final AtomicInteger pending = new AtomicInteger();

    /** Counts the streams that are open. / 열려 있는 스트림 수를 센다. */
    private final AtomicInteger streams = new AtomicInteger();

    /** The document's object URL access scope. / 문서의 객체 URL 접근 범위. */
    private final BlobUrlStore.Scope blobUrls;

    /**
     * Creates the service of a page.
     * <p>
     * 페이지의 서비스를 만든다.
     *
     * @param fetcher   the common request layer / 공통 요청 계층
     * @param cookies   the component's cookies / 컴포넌트의 쿠키
     * @param context   the copied request settings / 복사한 요청 설정
     * @param pageUrl   the URL of the page / 페이지의 URL
     * @param resources the cancellation token of the page's resources / 페이지 리소스의 취소 토큰
     * @param blobUrls the document's object URL scope / 문서의 객체 URL 범위
     */
    PageWebRequests(ResourceFetcher fetcher, CookieStore cookies, PageRequestContext context, String pageUrl, CancelToken resources,
            BlobUrlStore.Scope blobUrls) {
        this.fetcher = fetcher;
        this.cookies = cookies;
        this.context = context;
        this.pageUrl = pageUrl;
        this.resources = resources;
        this.blobUrls = blobUrls;
    }

    /**
     * Starts a streaming GET request on its own daemon thread and returns at once. The listener hears about the head, every piece of the body
     * and the end; the stream is not subject to the concurrent request slots.
     * <p>
     * 스트리밍 GET 요청을 자기 데몬 스레드에서 시작하고 곧바로 돌아온다. 받는 쪽은 머리, 본문의 모든 조각, 끝을 듣는다. 스트림은 동시 요청 칸의 제한을
     * 받지 않는다.
     *
     * @param request  the request / 요청
     * @param listener the receiver / 받는 쪽
     * @return the handle for cancelling / 취소용 핸들
     */
    public WebRequestSource.Call startStream(WebRequest request, StreamingWebRequestSource.Listener listener) {
        final Job job = new Job(request, null, listener);
        // 열려 있는 스트림 수와 존재하는 요청 수를 모두 세어 한도를 넘으면 곧바로 오류로 끝낸다.
        if (streams.incrementAndGet() > MAX_STREAMS) {
            streams.decrementAndGet();
            listener.onEnd(WebResponse.futileError("too many open event streams"));
            return job;
        }
        if (pending.incrementAndGet() > MAX_PENDING) {
            pending.decrementAndGet();
            streams.decrementAndGet();
            listener.onEnd(WebResponse.error("too many pending requests", false));
            return job;
        }
        Thread t = new Thread(job, "swingwebpanel-web-stream");
        t.setDaemon(true);
        t.start();
        return job;
    }

    /**
     * Starts a request on its own daemon thread and returns at once.
     * <p>
     * 요청을 자기 데몬 스레드에서 시작하고 곧바로 돌아온다.
     *
     * @param request  the request / 요청
     * @param callback the receiver / 받는 쪽
     * @return the handle for cancelling / 취소용 핸들
     */
    public WebRequestSource.Call start(WebRequest request, WebRequestSource.Callback callback) {
        final Job job = new Job(request, callback, null);
        if (pending.incrementAndGet() > MAX_PENDING) {
            pending.decrementAndGet();
            callback.onComplete(WebResponse.error("too many pending requests", false));
            return job;
        }
        Thread t = new Thread(job, "swingwebpanel-web-request");
        t.setDaemon(true);
        t.start();
        return job;
    }

    /**
     * One running request: the thread body, the cancellation handle and the processing of its response.
     * <p>
     * 실행 중인 요청 하나: 스레드 본문, 취소 핸들, 응답 처리.
     */
    private final class Job implements Runnable, WebRequestSource.Call, CancelToken.Hook {
        /** The request. / 요청. */
        private final WebRequest request;

        /** The receiver of an ordinary request, null for a stream. / 보통 요청의 받는 쪽. 스트림이면 null. */
        private final WebRequestSource.Callback callback;

        /** The receiver of a stream, null for an ordinary request. / 스트림의 받는 쪽. 보통 요청이면 null. */
        private final StreamingWebRequestSource.Listener stream;

        /** The cancellation token of this request. / 이 요청의 취소 토큰. */
        private final CancelToken token = new CancelToken();

        /** The page origin. / 페이지의 오리진. */
        private final Origin initiator = Origin.of(pageUrl);

        /** The object URL entry accepted when the caller started the request. / 호출자가 요청을 시작할 때 받아들인 객체 URL 항목. */
        private final BlobUrlStore.Entry blob;

        /** The answer that the stream listener refused or that failed the page rules (set on the request thread). / 스트림의 받는 쪽이 거부했거나 페이지 규칙에 어긋난 응답(요청 스레드에서 정한다). */
        private WebResponse refusal;

        /** Whether a Fetch receiver accepted the streamed body. / Fetch 수신자가 스트림 본문을 받아들였는지 여부. */
        private boolean observedBody;

        /**
         * Creates the job.
         * <p>
         * 작업을 만든다.
         *
         * @param request  the request / 요청
         * @param callback the receiver of an ordinary request or null / 보통 요청의 받는 쪽 또는 null
         * @param stream   the receiver of a stream or null / 스트림의 받는 쪽 또는 null
         */
        Job(WebRequest request, WebRequestSource.Callback callback, StreamingWebRequestSource.Listener stream) {
            this.request = request;
            this.callback = callback;
            this.stream = stream;
            blob = "blob".equals(UrlUtil.getScheme(request.getUrl())) ? blobUrls.resolve(request.getUrl()) : null;
        }

        /**
         * Cancels this request (called through the page token as well).
         * <p>
         * 이 요청을 취소한다(페이지 토큰을 통해서도 호출된다).
         */
        public void onCancel() {
            token.cancel();
        }

        /**
         * Cancels the request.
         * <p>
         * 요청을 취소한다.
         */
        public void cancel() {
            token.cancel();
        }

        /**
         * Runs the request and reports the result.
         * <p>
         * 요청을 실행하고 결과를 알린다.
         */
        public void run() {
            WebResponse result;
            boolean held = false;
            resources.addHook(this);
            try {
                // 스트림은 오래 열려 있으므로 동시 요청 칸을 차지하지 않는다.
                if (stream == null) {
                    slots.acquire();
                    held = true;
                }
                result = execute();
            } catch (InterruptedException e) {
                result = WebResponse.error("interrupted", true);
            } catch (RuntimeException e) {
                result = WebResponse.error("internal error: " + e, false);
            } finally {
                if (held) {
                    slots.release();
                }
                resources.removeHook(this);
                pending.decrementAndGet();
                if (stream != null) {
                    streams.decrementAndGet();
                }
            }
            try {
                if (stream != null) {
                    stream.onEnd(result);
                } else {
                    callback.onComplete(result);
                }
            } catch (RuntimeException e) {
                // 받는 쪽의 오류가 요청 스레드를 죽이지 않게 한다.
            }
        }

        /**
         * Applies the page-request rules, fetches and filters the response.
         * <p>
         * 페이지 요청 규칙을 적용하고 가져와서 응답을 거른다.
         *
         * @return the filtered response or a network error / 걸러진 응답 또는 네트워크 오류
         */
        private WebResponse execute() {
            if (token.isCancelled()) {
                return WebResponse.error("request cancelled", true);
            }
            String url = request.getUrl();
            SchemePolicy.Kind kind = SchemePolicy.classify(url);
            boolean classpathPage = UrlUtil.SCHEME_CLASSPATH.equals(initiator.getScheme());
            boolean filePage = UrlUtil.SCHEME_FILE.equals(initiator.getScheme());
            if (!(kind == SchemePolicy.Kind.HTTP || kind == SchemePolicy.Kind.HTTPS || (kind == SchemePolicy.Kind.CLASSPATH && classpathPage)
                    || (kind == SchemePolicy.Kind.FILE && filePage)
                    || (kind == SchemePolicy.Kind.BLOB && blob != null))) {
                return WebResponse.futileError("scheme is not allowed for scripted requests: " + url);
            }
            String method = request.getMethod();
            if (!METHODS.contains(method)) {
                return WebResponse.futileError("the request layer cannot send the method " + method);
            }
            if (request.getBody() != null && !("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                return WebResponse.futileError("the request layer sends a body only with POST, PUT and PATCH, not with " + method);
            }
            Origin target = Origin.of(url);
            boolean same = blob != null || initiator.isSameOrigin(target);
            String mode = request.getMode();
            if (WebRequest.MODE_SAME_ORIGIN.equals(mode) && !same) {
                return WebResponse.futileError("cross-origin request to " + url + " is blocked by the same-origin mode");
            }
            ResourceRequest rr = build(url, same);
            ResponseHandler early;
            if (stream != null) {
                rr.setReadTimeoutMs(STREAM_READ_TIMEOUT_MS).setStreaming(true);
                early = streamHandler();
            } else if (callback instanceof WebRequestSource.BodyCallback && (kind == SchemePolicy.Kind.HTTP || kind == SchemePolicy.Kind.HTTPS)) {
                early = bodyObserver((WebRequestSource.BodyCallback) callback);
            } else {
                early = new ResponseHandler() {
                    public OutputStream onResponse(ResourceResponse head) {
                        WebResponse w = filter(head, false);
                        if (!w.isError() && !token.isCancelled()) {
                            callback.onHeaders(w);
                        }
                        return null;
                    }
                };
            }
            ResourceResponse r = fetcher.fetch(rr, early);
            // 스트림의 받는 쪽이 거부했거나 페이지 규칙에 어긋난 응답은 그 이유가 실패 종류보다 정확하다.
            if (stream != null && refusal != null) {
                return refusal;
            }
            if (r.getFailureKind() != FailureKind.NONE) {
                return WebResponse.error(r.getFailureKind() + ": " + r.getFailureMessage(), r.getFailureKind() == FailureKind.CANCELLED);
            }
            if (token.isCancelled()) {
                return WebResponse.error("request cancelled", true);
            }
            return filter(r, stream == null && !observedBody);
        }

        /**
         * Applies the scripted request rules to the head before exposing any live HTTP bytes.
         * <p>
         * 도착한 HTTP 바이트를 노출하기 전에 응답 머리에 스크립트 요청 정책을 적용한다.
         *
         * @param receiver optional body consumer / 선택 본문 소비자
         * @return HTTP observer / HTTP 관찰자
         */
        private ResponseHandler bodyObserver(final WebRequestSource.BodyCallback receiver) {
            return new ResponseHandler.BodyObserver() {
                /** Passes the filtered response and selects the body destination. / 걸러진 응답을 건네고 본문의 목적지를 선택한다. */
                public OutputStream onResponse(ResourceResponse head) {
                    WebResponse response = filter(head, false);
                    if (response.isError() || token.isCancelled()) {
                        return null;
                    }
                    OutputStream sink = receiver.onBody(response);
                    observedBody = sink != null;
                    return sink;
                }
            };
        }

        /**
         * Creates the handler that streams the body of the final response to the stream listener: a response that fails the page rules or that
         * the listener refuses is aborted before its body is read (the reason is kept in <code>refusal</code>), anything else flows to
         * <code>onData</code> without being kept in memory.
         * <p>
         * 최종 응답의 본문을 스트림의 받는 쪽으로 흘리는 처리기를 만든다. 페이지 규칙에 어긋나거나 받는 쪽이 거부한 응답은 본문을 읽기 전에 중단하고(이유는
         * <code>refusal</code> 에 둔다), 그 밖의 것은 메모리에 두지 않고 <code>onData</code> 로 흘린다.
         *
         * @return the handler / 처리기
         */
        private ResponseHandler streamHandler() {
            return new ResponseHandler() {
                public OutputStream onResponse(ResourceResponse head) throws IOException {
                    WebResponse w = filter(head, false);
                    if (w.isError()) {
                        refusal = w;
                        throw new IOException("the page rules refuse the response: " + w.getErrorMessage());
                    }
                    if (token.isCancelled()) {
                        throw new IOException("request cancelled");
                    }
                    if (!stream.onHeaders(w)) {
                        refusal = w;
                        throw new IOException("the receiver refuses the response");
                    }
                    return new OutputStream() {
                        public void write(int b) {
                            write(new byte[] { (byte) b }, 0, 1);
                        }

                        public void write(byte[] b, int off, int len) {
                            // 도착한 조각을 복사해 넘긴다. 호출자의 버퍼는 곧 다시 쓰이기 때문이다.
                            if (len > 0 && !token.isCancelled()) {
                                byte[] copy = new byte[len];
                                System.arraycopy(b, off, copy, 0, len);
                                stream.onData(copy, len);
                            }
                        }
                    };
                }
            };
        }

        /**
         * Builds the request of the common layer.
         * <p>
         * 공통 계층의 요청을 만든다.
         *
         * @param url  the URL / URL
         * @param same true if the URL has the page's origin / URL 이 페이지의 오리진이면 true
         * @return the request / 요청
         */
        private ResourceRequest build(String url, boolean same) {
            RequestHeaders extra = context.headers();
            Map<String, String> author = new java.util.LinkedHashMap<String, String>();
            List<String[]> list = request.getHeaders();
            // 작성자 헤더를 모은다. 같은 이름의 컴포넌트 추가 헤더는 작성자 값이 덮어쓴다.
            for (int i = 0; i < list.size(); i++) {
                author.put(list.get(i)[0], list.get(i)[1]);
            }
            if (author.size() > 0) {
                extra.addAll(author);
            }
            String credentials = request.getCredentials();
            boolean include = WebRequest.CREDENTIALS_INCLUDE.equals(credentials);
            boolean omit = WebRequest.CREDENTIALS_OMIT.equals(credentials);
            boolean noCors = WebRequest.MODE_NO_CORS.equals(request.getMode());
            ResourceRequest rr = new ResourceRequest(url).setMethod(request.getMethod()).setUserAgent(context.userAgent).setExtraHeaders(extra)
                    .setCookies(cookies).setCancelToken(token).setInitiator(initiator)
                    .setBlobUrlEntry(blob)
                    .setEmbedderPolicy(request.getEmbedderPolicy() == null ? context.crossOriginPolicy.getEmbedder() : request.getEmbedderPolicy())
                    .setPurpose(noCors ? RequestPurpose.OTHER : RequestPurpose.XHR).setSendCredentials(!omit)
                    .setSameOriginCredentialsOnly(!omit && !include).setCorsAuthorHeaders(corsDeciding(author));
            if (request.getBody() != null) {
                rr.setBody(request.getBody(), request.getBodyType());
            }
            rr.setReferrer(referrer(url, same));
            rr.setCacheMode(cacheMode());
            // 스트림의 진행은 본문 조각으로 알리므로 진행 리스너를 달지 않는다.
            if (callback != null) {
                rr.setProgressListener(new ProgressListener() {
                    public void onProgress(long received, long total) {
                        callback.onProgress(received, total);
                    }
                });
            }
            return rr;
        }

        /**
         * Returns the headers that decide whether a cross-origin request needs a CORS preflight. A stream sets <code>Last-Event-ID</code> itself
         * on a reconnection, as the user agent and not as the page, so that header never causes a preflight (as in the browsers).
         * <p>
         * 교차 오리진 요청에 CORS 사전 요청이 필요한지를 정하는 헤더를 반환한다. 스트림은 재연결 때 <code>Last-Event-ID</code> 를 페이지가 아니라 사용자 에이전트로서
         * 스스로 붙이므로 그 헤더는 사전 요청을 일으키지 않는다(브라우저와 같다).
         *
         * @param author the headers of the request / 요청의 헤더
         * @return the headers that count / 사전 요청 판정에 쓰는 헤더
         */
        private Map<String, String> corsDeciding(Map<String, String> author) {
            if (stream == null) {
                return author;
            }
            Map<String, String> counted = new java.util.LinkedHashMap<String, String>(author);
            counted.remove("Last-Event-ID");
            return counted;
        }

        /**
         * Chooses the cache mode from the Fetch cache mode and the force flag of the page.
         * <p>
         * Fetch 캐시 모드와 페이지의 강제 표시로 캐시 모드를 고른다.
         *
         * @return the cache mode / 캐시 모드
         */
        private ResourceRequest.CacheMode cacheMode() {
            String c = request.getCache();
            if ("no-store".equals(c) || "reload".equals(c) || "no-cache".equals(c)) {
                return ResourceRequest.CacheMode.FORCE_RELOAD;
            }
            if ("force-cache".equals(c)) {
                return ResourceRequest.CacheMode.PREFER_CACHE;
            }
            return context.force ? ResourceRequest.CacheMode.FORCE_RELOAD : ResourceRequest.CacheMode.DEFAULT;
        }

        /**
         * Computes the Referer to send from the referrer and the referrer policy of the request (the default policy is
         * strict-origin-when-cross-origin).
         * <p>
         * 요청의 참조와 참조 정책으로 보낼 Referer 를 계산한다(기본 정책은 strict-origin-when-cross-origin).
         *
         * @param url  the target URL / 대상 URL
         * @param same true if the target has the page's origin / 대상이 페이지의 오리진이면 true
         * @return the Referer value or null for none / Referer 값. 없으면 null
         */
        private String referrer(String url, boolean same) {
            String ref = request.getReferrer();
            if (WebRequest.REFERRER_NONE.equals(ref)) {
                return null;
            }
            String full = WebRequest.REFERRER_CLIENT.equals(ref) ? pageUrl : ref;
            String policy = request.getReferrerPolicy();
            String originOnly = originReferrer();
            boolean downgrade = pageUrl.startsWith("https:") && url.startsWith("http:");
            if ("no-referrer".equals(policy)) {
                return null;
            } else if ("origin".equals(policy)) {
                return originOnly;
            } else if ("same-origin".equals(policy)) {
                return same ? full : null;
            } else if ("origin-when-cross-origin".equals(policy)) {
                return same ? full : originOnly;
            } else if ("strict-origin".equals(policy)) {
                return downgrade ? null : originOnly;
            } else if ("no-referrer-when-downgrade".equals(policy)) {
                return downgrade ? null : full;
            } else if ("unsafe-url".equals(policy)) {
                return full;
            }
            return same ? full : (downgrade ? null : originOnly);
        }

        /**
         * Returns the origin of the page as a referrer (<code>scheme://host[:port]/</code>), or null for pages without a web origin.
         * <p>
         * 페이지의 오리진을 참조로 반환한다(<code>scheme://host[:port]/</code>). 웹 오리진이 없는 페이지는 null.
         *
         * @return the referrer or null / 참조 또는 null
         */
        private String originReferrer() {
            if (initiator.isOpaque() || UrlUtil.SCHEME_CLASSPATH.equals(initiator.getScheme()) || UrlUtil.SCHEME_FILE.equals(initiator.getScheme())) {
                return null;
            }
            return initiator.serialize() + "/";
        }

        /**
         * Turns a response of the common layer into a filtered web response: checks the same-origin policy and CORS, removes the headers
         * that the page may not see and hides everything of an opaque response.
         * <p>
         * 공통 계층의 응답을 걸러진 웹 응답으로 바꾼다: 동일 출처 정책과 CORS 를 검사하고, 페이지가 볼 수 없는 헤더를 없애며, 불투명 응답은 모든 것을 숨긴다.
         *
         * @param r    the response / 응답
         * @param full true to include the body / 본문을 포함하려면 true
         * @return the filtered response or a network error / 걸러진 응답 또는 네트워크 오류
         */
        private WebResponse filter(ResourceResponse r, boolean full) {
            String finalUrl = r.getFinalUrl() == null ? request.getUrl() : r.getFinalUrl();
            boolean redirected = !r.getRedirects().isEmpty();
            if ("error".equals(request.getRedirect()) && redirected) {
                return WebResponse.futileError("the request was redirected to " + finalUrl + " but the redirect mode is error");
            }
            Origin finalOrigin = Origin.of(finalUrl);
            boolean same = (blob != null && r.getBlobUrlEntry() == blob) || initiator.isSameOrigin(finalOrigin);
            String mode = request.getMode();
            String credentials = request.getCredentials();
            boolean withCredentials = WebRequest.CREDENTIALS_INCLUDE.equals(credentials);
            String type;
            if (same) {
                type = WebResponse.TYPE_BASIC;
            } else if (WebRequest.MODE_SAME_ORIGIN.equals(mode)) {
                return WebResponse.futileError("the request was redirected to the other origin " + finalUrl + " but the mode is same-origin");
            } else if (WebRequest.MODE_NO_CORS.equals(mode)) {
                WebResponse opaque = new WebResponse(WebResponse.TYPE_OPAQUE);
                return opaque;
            } else {
                if (!OriginPolicy.mayScriptReadBody(initiator, finalOrigin, RequestPurpose.XHR, r, withCredentials)) {
                    return WebResponse.futileError("blocked by CORS policy: no suitable Access-Control-Allow-Origin header for " + finalUrl);
                }
                type = WebResponse.TYPE_CORS;
            }
            WebResponse w = new WebResponse(type);
            w.setStatus(r.getStatus(), r.getReason());
            w.setUrl(finalUrl);
            w.setRedirected(redirected);
            w.setDeclaredLength(r.getDeclaredLength());
            copyHeaders(r, w, type, withCredentials);
            if (full) {
                w.setBody(r.getBody());
            }
            return w;
        }

        /**
         * Copies the headers that the page may see.
         * <p>
         * 페이지가 볼 수 있는 헤더를 복사한다.
         *
         * @param r               the response / 응답
         * @param w               receives the headers / 헤더를 받는다
         * @param type            the response type / 응답 종류
         * @param withCredentials true if credentials were allowed (a wildcard does not expose headers then) / 자격 증명을 허용했으면 true(그러면 와일드카드가 헤더를 노출하지 않는다)
         */
        private void copyHeaders(ResourceResponse r, WebResponse w, String type, boolean withCredentials) {
            List<String> exposed = new ArrayList<String>();
            boolean all = false;
            if (WebResponse.TYPE_CORS.equals(type)) {
                List<String> lines = r.getHeaders("access-control-expose-headers");
                // 노출할 헤더 이름 목록을 모은다. 자격 증명이 없으면 "*" 는 모든 헤더를 뜻한다.
                for (int i = 0; i < lines.size(); i++) {
                    String[] names = lines.get(i).split(",");
                    // 쉼표로 나눈 이름마다 노출 목록에 넣는다.
                    for (int j = 0; j < names.length; j++) {
                        String n = names[j].trim().toLowerCase();
                        if ("*".equals(n) && !withCredentials) {
                            all = true;
                        } else if (n.length() > 0) {
                            exposed.add(n);
                        }
                    }
                }
            }
            List<String> names = r.getHeaderNames();
            // 응답 헤더를 하나씩 보며 Set-Cookie 계열과 CORS 가 가린 헤더를 뺀다.
            for (int i = 0; i < names.size(); i++) {
                String n = names.get(i).toLowerCase();
                if ("set-cookie".equals(n) || "set-cookie2".equals(n)) {
                    continue;
                }
                if (WebResponse.TYPE_CORS.equals(type) && !all && !exposed.contains(n) && !safelistedResponse(n)) {
                    continue;
                }
                List<String> values = r.getHeaders(n);
                // 같은 이름의 값을 모두 응답에 옮긴다.
                for (int j = 0; j < values.size(); j++) {
                    w.addHeader(n, values.get(j));
                }
            }
        }

        /**
         * Tells whether a response header is CORS-safelisted.
         * <p>
         * 응답 헤더가 CORS 안전 목록인지 알려 준다.
         *
         * @param n the lower-case name / 소문자 이름
         * @return true if exposed to every origin / 모든 오리진에 노출되면 true
         */
        private boolean safelistedResponse(String n) {
            return "cache-control".equals(n) || "content-language".equals(n) || "content-length".equals(n) || "content-type".equals(n)
                    || "expires".equals(n) || "last-modified".equals(n) || "pragma".equals(n);
        }
    }
}
