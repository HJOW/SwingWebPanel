package org.duckdns.hjow.swingwebpanel.net;

/**
 * The single entry point of the resource layer. It dispatches by scheme to {@link HttpFetcher} (http and https),
 * {@link FtpFetcher} (ftp) and {@link ClasspathLoader} (classpath) and enforces the allowed-scheme policy of
 * {@link SchemePolicy}: any other scheme is answered with {@link FailureKind#BLOCKED_SCHEME} without contacting anything.
 * <ul>
 * <li><code>about:blank</code> gives an empty HTML response; <code>about:text</code> and every other address are refused here
 * (the component handles them itself).</li>
 * <li>A document served from the network (http, https, ftp initiator) may not load <code>classpath://</code> resources:
 * such a request is blocked, so remote pages cannot read the application's class path.</li>
 * </ul>
 * One instance can be shared by all requests of a component. {@link #fetch(ResourceRequest, ResponseHandler)} never throws for
 * network or file problems and may be called from any thread; the call blocks the calling thread, so call it from the
 * document owner thread or a worker, never from the EDT.
 * <p>
 * Use: <code>new ResourceFetcher(sslConfigurer, FetchLimits.DEFAULT, null).fetch(new ResourceRequest(url))</code>.
 * <p>
 * 리소스 계층의 단일 진입점. 스킴에 따라 {@link HttpFetcher}(http, https), {@link FtpFetcher}(ftp), {@link ClasspathLoader}(classpath)로
 * 분기하며 {@link SchemePolicy} 의 허용 스킴 정책을 강제한다. 그 밖의 스킴은 어디에도 연결하지 않고
 * {@link FailureKind#BLOCKED_SCHEME} 로 답한다.
 * <ul>
 * <li><code>about:blank</code> 는 빈 HTML 응답을 돌려주고, <code>about:text</code> 와 그 밖의 주소는 여기서 거부한다(컴포넌트가 직접 처리).</li>
 * <li>네트워크에서 온 문서(http, https, ftp 를 initiator 로 가진 요청)는 <code>classpath://</code> 리소스를 읽을 수 없다. 그런 요청은
 * 차단되므로 원격 페이지가 애플리케이션의 클래스패스를 읽을 수 없다.</li>
 * </ul>
 * 인스턴스 하나를 컴포넌트의 모든 요청이 공유할 수 있다. {@link #fetch(ResourceRequest, ResponseHandler)} 는 네트워크나 파일 문제로
 * 예외를 던지지 않으며 어느 스레드에서나 호출할 수 있다. 호출한 스레드를 막으므로 EDT 가 아니라 문서 소유 스레드나 작업 스레드에서
 * 호출한다.
 * <p>
 * 사용: <code>new ResourceFetcher(sslConfigurer, FetchLimits.DEFAULT, null).fetch(new ResourceRequest(url))</code>.
 */
public final class ResourceFetcher {

    /**
     * The http and https fetcher.
     * <p>
     * http/https 가져오기 객체.
     */
    private final HttpFetcher http;
    /**
     * The ftp fetcher.
     * <p>
     * ftp 가져오기 객체.
     */
    private final FtpFetcher ftp;
    /**
     * The class path loader.
     * <p>
     * 클래스패스 로더.
     */
    private final ClasspathLoader classpath;
    /** Reader of local file URLs. / 로컬 파일 URL 읽기 객체. */
    private final FileResourceLoader files;
    /** Registered object URLs for this component. / 이 컴포넌트에 등록된 객체 URL. */
    private final BlobUrlStore blobUrls = new BlobUrlStore();
    /** Local source of a resolved Blob response. / 조회한 Blob 응답의 로컬 소스. */
    private final BlobResourceLoader blobs;
    /**
     * The HTTP cache of the component, or null for no caching (the default).
     * <p>
     * 컴포넌트의 HTTP 캐시. 캐시를 쓰지 않으면 null(기본값).
     */
    private volatile HttpCache cache;

    /**
     * Creates a fetcher.
     * <p>
     * 가져오기 객체를 만든다.
     *
     * @param ssl    SSL hook for https, null for {@link DefaultSslConfigurer} / https 용 SSL 훅. null 이면 {@link DefaultSslConfigurer}
     * @param limits limits, null for the defaults / 제한. null 이면 기본값
     * @param loader class loader for classpath URLs, null for the context class loader / 클래스패스 URL 용 클래스 로더. null 이면 컨텍스트 클래스 로더
     */
    public ResourceFetcher(SslConfigurer ssl, FetchLimits limits, ClassLoader loader) {
        FetchLimits l = limits == null ? FetchLimits.DEFAULT : limits;
        this.http = new HttpFetcher(ssl, l);
        this.ftp = new FtpFetcher(l);
        this.classpath = new ClasspathLoader(loader, l);
        this.files = new FileResourceLoader(l);
        this.blobs = new BlobResourceLoader(blobUrls, l);
    }

    /** Returns the component's object URL store. / 컴포넌트의 객체 URL 저장소를 반환한다. */
    public BlobUrlStore getBlobUrlStore() {
        return blobUrls;
    }

    /**
     * Returns the http fetcher (for switching the http retry on or off).
     * <p>
     * http 가져오기 객체를 돌려준다(http 재시도를 켜고 끄는 데 쓴다).
     *
     * @return the http fetcher / http 가져오기 객체
     */
    public HttpFetcher getHttpFetcher() {
        return http;
    }

    /**
     * Installs the HTTP cache that every http/https GET goes through (plan item P19-03). A component uses one cache per fetcher so that
     * responses are never shared between components. Responses that the body handler streams (downloads), unsafe methods and non-HTTP schemes are not stored.
     * <p>
     * 모든 http/https GET 이 거치는 HTTP 캐시를 설치한다(계획 P19-03). 컴포넌트는 가져오기 객체마다 캐시 하나를 쓰므로 응답이 컴포넌트 사이에
     * 공유되지 않는다. 본문 처리기가 스트림으로 보낸 응답(다운로드), 안전하지 않은 메서드, HTTP 가 아닌 스킴은 저장하지 않는다.
     *
     * @param newCache the cache, or null to switch caching off / 캐시. null 이면 캐시를 끈다
     */
    public void setCache(HttpCache newCache) {
        this.cache = newCache;
    }

    /**
     * Returns the installed HTTP cache.
     * <p>
     * 설치된 HTTP 캐시를 반환한다.
     *
     * @return the cache or null / 캐시 또는 null
     */
    public HttpCache getCache() {
        return cache;
    }

    /**
     * Fetches an http or https resource, through the cache when one is installed.
     * <p>
     * http/https 리소스를 가져온다. 캐시가 설치되어 있으면 캐시를 거친다.
     *
     * @param req     the request / 요청
     * @param handler body handler or null / 본문 처리기 또는 null
     * @return the response / 응답
     */
    private ResourceResponse fetchHttp(final ResourceRequest req, ResponseHandler handler) {
        HttpCache c = cache;
        if (c == null) {
            return http.fetch(req, handler);
        }
        String method = req.getMethod();
        // GET 이 아닌 요청은 캐시에서 답하지 않으며, 안전하지 않은 메서드는 성공하면 저장본을 무효화한다.
        if (!"GET".equals(method)) {
            ResourceResponse r = http.fetch(req, handler);
            if (!"HEAD".equals(method) && !"OPTIONS".equals(method)) {
                c.invalidateAfterUnsafe(req, r);
            }
            return r;
        }
        // 처리기가 스트림을 고르면 본문은 캐시에 저장되지 않고(스트리밍 응답), 처리기가 메모리에 두면 저장 대상이 된다.
        return c.fetch(req, handler, new HttpCache.Network() {
            /**
             * Sends a request to the network.
             * <p>
             * 요청을 네트워크로 보낸다.
             *
             * @param request the request / 요청
             * @param h       body handler or null / 본문 처리기 또는 null
             * @return the response / 응답
             */
            public ResourceResponse fetch(ResourceRequest request, ResponseHandler h) {
                return http.fetch(request, h);
            }
        });
    }

    /**
     * Fetches a resource into memory.
     * <p>
     * 리소스를 메모리로 가져온다.
     *
     * @param req the request / 요청
     * @return the response / 응답
     */
    public ResourceResponse fetch(ResourceRequest req) {
        return fetch(req, null);
    }

    /**
     * Fetches a resource, optionally streaming successful bodies to the stream chosen by the handler.
     * <p>
     * 리소스를 가져온다. 핸들러가 고른 스트림으로 성공한 본문을 흘려 보낼 수도 있다.
     *
     * @param req     the request / 요청
     * @param handler body handler or null / 본문 처리기 또는 null
     * @return the response / 응답
     */
    public ResourceResponse fetch(ResourceRequest req, ResponseHandler handler) {
        String url = req.getUrl();
        SchemePolicy.Kind kind = SchemePolicy.classify(url);
        switch (kind) {
        case HTTP:
        case HTTPS:
            return fetchHttp(req, handler);
        case FTP:
            // FTP 응답은 HTTP 삽입 정책 헤더를 제공할 수 없다.
            ResourceResponse ftpResponse = new ResourceResponse(url);
            if (!CrossOriginPolicy.permits(req, ftpResponse)) {
                return CrossOriginPolicy.blocked(ftpResponse);
            }
            return ftp.fetch(req, handler);
        case CLASSPATH:
            Origin initiator = req.getInitiator();
            if (initiator != null && !initiator.isOpaque() && !UrlUtil.SCHEME_CLASSPATH.equals(initiator.getScheme())) {
                return ResourceResponse.failure(url, FailureKind.BLOCKED_SCHEME,
                        "a document from " + initiator.serialize() + " may not load classpath resources");
            }
            return classpath.load(req, handler);
        case BLOB:
            return blobs.load(req, handler);
        case FILE:
            Origin fileInitiator = req.getInitiator();
            // 로컬 출처의 요청만 허용한다. 시작 오리진이 없는 요청은 Java 호스트나 이미 승인한 저장 요청에만 해당한다.
            if (fileInitiator != null ? !UrlUtil.SCHEME_FILE.equals(fileInitiator.getScheme()) : req.getPurpose() == RequestPurpose.FRAME) {
                return ResourceResponse.failure(url, FailureKind.BLOCKED_SCHEME, "only local documents and the host can read file URLs");
            }
            return files.load(req, handler);
        case ABOUT_BLANK:
            ResourceResponse blank = new ResourceResponse(url);
            blank.setStatus(200, "OK");
            blank.addHeader("Content-Type", "text/html");
            blank.setContentType("text/html", null);
            return blank;
        case INVALID:
            return ResourceResponse.failure(url, FailureKind.INVALID_URL, "not an absolute url: " + url);
        default:
            return ResourceResponse.failure(url, FailureKind.BLOCKED_SCHEME, "scheme is not allowed: " + url);
        }
    }
}
