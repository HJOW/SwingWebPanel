package org.duckdns.hjow.swingwebpanel.core;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.duckdns.hjow.swingwebpanel.css.cssom.LoadedStyleSheet;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetDecoder;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheetLoader;
import org.duckdns.hjow.swingwebpanel.js.host.MediaRequestSource;
import org.duckdns.hjow.swingwebpanel.media.DecodeLimits;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaData;
import org.duckdns.hjow.swingwebpanel.media.audio.MediaSpool;
import org.duckdns.hjow.swingwebpanel.media.image.DecodedImage;
import org.duckdns.hjow.swingwebpanel.media.image.ImageDecoders;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.CancelToken;
import org.duckdns.hjow.swingwebpanel.net.CookieStore;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.OriginPolicy;
import org.duckdns.hjow.swingwebpanel.net.RequestPurpose;
import org.duckdns.hjow.swingwebpanel.net.ResourceFetcher;
import org.duckdns.hjow.swingwebpanel.net.ResourceRequest;
import org.duckdns.hjow.swingwebpanel.net.ResourceResponse;
import org.duckdns.hjow.swingwebpanel.net.ResponseHandler;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.CanvasSource;
import org.duckdns.hjow.swingwebpanel.render.ImageResource;
import org.duckdns.hjow.swingwebpanel.render.CorsImageSource;
import org.duckdns.hjow.swingwebpanel.render.ImageSource;

/**
 * Nonblocking per-page resource states on the common request layer. Reads schedule bounded background work and return
 * pending states; completion asks the document queue to rebuild. No worker reads or modifies the DOM. Encoded responses
 * use the component HTTP cache, while decoded states live only as long as this page (P09-07-b).
 * <p>공통 요청 계층 위의 페이지별 비동기 리소스 상태. 조회는 제한된 백그라운드 작업을 예약하고 대기 상태를 돌려준다. 완료 시 문서 큐에 재배치를 요청한다.
 * 작업자는 DOM을 읽거나 바꾸지 않는다. 인코딩 응답은 컴포넌트 HTTP 캐시를 쓰며 디코딩 상태는 이 페이지 수명에만 보관한다.
 */
final class PageResources implements CorsImageSource, CanvasSource, org.duckdns.hjow.swingwebpanel.render.FrameSource, org.duckdns.hjow.swingwebpanel.render.MediaControlSource,
        StyleSheetLoader {
    /**
     * Receives the final state of one image request (the script image loader of <code>img</code> elements). It is called on a worker thread, or
     * at once on the caller's thread when the state is already final, and never after the page resources were closed.
     * <p>
     * 이미지 요청 하나의 최종 상태를 받는다(<code>img</code> 요소의 스크립트 이미지 로더). 작업자 스레드에서 호출되며, 상태가 이미 확정되어 있으면
     * 호출한 스레드에서 곧바로 호출되고, 페이지 리소스를 닫은 뒤에는 호출되지 않는다.
     */
    interface ImageWaiter {
        /**
         * Reports the final state of the image.
         * <p>
         * 이미지의 최종 상태를 알린다.
         *
         * @param image the loaded or failed state (never pending) / 읽었거나 실패한 상태(진행 중이 아님)
         */
        void imageDone(ImageResource image);
    }

    /** Completion callback that only schedules document work. / 문서 작업 예약만 하는 완료 콜백. */
    interface Listener {
        /** Reports a new terminal state. / 새 최종 상태를 알린다.
         * @param stylesheet true when stylesheet collection must be repeated / 스타일시트 수집을 반복해야 하면 true */
        void resourceChanged(boolean stylesheet);
    }
    /** Maximum distinct resources per page. / 페이지별 최대 서로 다른 리소스 수. */
    private static final int MAX_ENTRIES = 256;
    /** Maximum retained decoded data per page, 64 MiB. / 페이지별 디코딩 보관 상한 64 MiB. */
    private static final long MAX_RETAINED_BYTES = 64L * 1024 * 1024;
    /** The largest web font file that a page keeps, 8 MiB. 페이지가 보관하는 웹 글꼴 파일의 최대 크기. 8 MiB. */
    private static final int MAX_FONT_BYTES = 8 * 1024 * 1024;
    /** Encoded input and decoded frame limits for browser images. / 브라우저 이미지의 입력과 디코딩 프레임 제한. */
    private static final DecodeLimits IMAGE_LIMITS = new DecodeLimits(8L * 1024 * 1024, 8L * 1024 * 1024, 256, 16L * 1024 * 1024);
    /** Shared component fetcher. / 컴포넌트 공용 가져오기 객체. */
    private final ResourceFetcher fetcher;
    /** Component cookies. / 컴포넌트 쿠키. */
    private final CookieStore cookies;
    /** Snapshot of resource settings. / 리소스 설정 스냅샷. */
    private final PageRequestContext context;
    /** Final document address used for origin and referrer. / 오리진과 참조 주소용 최종 문서 주소. */
    private final String pageUrl;
    /** Shared bounded worker executor. / 공용 제한 작업 실행기. */
    private final Executor workers;
    /** Rebuild callback without DOM access. / DOM 접근 없는 재배치 콜백. */
    private final Listener listener;
    /** Page cancellation closes network streams and interrupts decoding. / 페이지 취소 시 네트워크 스트림을 닫고 디코딩을 인터럽트한다. */
    private final CancelToken cancel = new CancelToken();
    /** Entries keyed by purpose and URL; guarded by this object. / 목적과 URL별 항목이며 이 객체로 잠근다. */
    private final Map<String, Entry> entries = new HashMap<String, Entry>();
    /** The state of the media resources by URL, from the media elements; guarded by this object. / 미디어 요소가 알려 준 미디어 리소스의 URL 별 상태. 이 객체로 잠근다. */
    private final Map<String, ImageResource> mediaStates = new HashMap<String, ImageResource>();
    /** Retained decoded byte estimate, guarded by this object. / 이 객체로 보호하는 디코딩 보관 바이트 추정치. */
    private long retainedBytes;
    /** Number of fetches that were started (stylesheets and images, each address once), guarded by this object. / 시작한 가져오기 수(스타일시트와 이미지, 주소마다 한 번). 이 객체로 보호한다. */
    private int started;
    /** Number of started fetches that ended, successfully or not, guarded by this object. / 시작한 가져오기 중 성공이든 실패든 끝난 수. 이 객체로 보호한다. */
    private int finished;

    /**
     * Creates page resources without submitting work.
     * <p>작업을 제출하지 않고 페이지 리소스를 만든다.
     * @param fetcher shared fetcher / 공용 가져오기 객체
     * @param cookies component cookies / 컴포넌트 쿠키
     * @param snapshot owning page / 소유 페이지
     * @param context copied request settings / 복사한 요청 설정
     * @param workers bounded executor / 제한 실행기
     * @param listener completion callback / 완료 콜백
     */
    PageResources(ResourceFetcher fetcher, CookieStore cookies, PageSnapshot snapshot, PageRequestContext context,
            Executor workers, Listener listener) {
        this(fetcher, cookies, snapshot.getUrl(), context, workers, listener);
    }

    /** Creates resource states before a page snapshot exists. / 페이지 스냅샷 생성 전 리소스 상태를 만든다.
     * @param fetcher common request layer / 공통 요청 계층
     * @param cookies component cookies / 컴포넌트 쿠키
     * @param pageUrl final document address / 최종 문서 주소
     * @param context copied settings / 복사한 설정
     * @param workers background executor / 백그라운드 실행기
     * @param listener completion callback / 완료 콜백 */
    PageResources(ResourceFetcher fetcher, CookieStore cookies, String pageUrl, PageRequestContext context,
            Executor workers, Listener listener) {
        this.fetcher = fetcher;
        this.cookies = cookies;
        this.pageUrl = pageUrl;
        this.context = context;
        this.workers = workers;
        this.listener = listener;
    }

    /**
     * The canvas pictures of the page (the script host), or null before the scripts exist.
     * <p>페이지의 캔버스 그림(스크립트 호스트). 스크립트가 생기기 전에는 null.
     */
    private volatile CanvasSource canvases;

    /**
     * Sets the source of the canvas pictures that the renderer paints.
     * <p>렌더러가 그리는 캔버스 그림의 공급원을 정한다.
     * @param source the source or null / 공급원 또는 null
     */
    void setCanvasSource(CanvasSource source) {
        // 이미 닫힌 리소스는 공급원을 받지 않는다.
        canvases = cancel.isCancelled() ? null : source;
    }

    /**
     * The state of the audio control bars (the script host), or null before the scripts exist.
     * <p>오디오 컨트롤 막대의 상태(스크립트 호스트). 스크립트가 생기기 전에는 null.
     */
    private volatile org.duckdns.hjow.swingwebpanel.render.MediaControlSource mediaControls;

    /**
     * Sets the source of the control bar states that the renderer paints.
     * <p>렌더러가 그리는 컨트롤 막대 상태의 공급원을 정한다.
     * @param source the source or null / 공급원 또는 null
     */
    void setMediaControlSource(org.duckdns.hjow.swingwebpanel.render.MediaControlSource source) {
        // 이미 닫힌 리소스는 공급원을 받지 않는다.
        mediaControls = cancel.isCancelled() ? null : source;
    }

    /** Object URL scope shared with the document script host. / 문서 스크립트 호스트와 공유하는 객체 URL 범위. */
    private volatile BlobUrlStore.Scope blobUrls;

    /**
     * Assigns the document's scope before fetching subresources.
     * <p>
     * 하위 리소스를 가져오기 전에 문서 범위를 지정한다.
     *
     * @param scope document scope / 문서 범위
     */
    void setBlobUrlScope(BlobUrlStore.Scope scope) {
        blobUrls = scope;
    }

    /**
     * Returns the state that the control bar of an audio element shows, or null if the script host has none.
     * <p>오디오 요소의 컨트롤 막대가 보여 주는 상태를 반환한다. 스크립트 호스트에 없으면 null.
     * @param media the audio element / 오디오 요소
     * @return the snapshot or null / 스냅샷 또는 null
     */
    public org.duckdns.hjow.swingwebpanel.render.MediaControlState getMediaControls(org.duckdns.hjow.swingwebpanel.dom.Element media) {
        org.duckdns.hjow.swingwebpanel.render.MediaControlSource source = mediaControls;
        return source == null ? null : source.getMediaControls(media);
    }

    /**
     * The frames of the page (the frame set of the document), or null for a page without frames.
     * <p>페이지의 프레임들(문서의 프레임 집합). 프레임이 없는 페이지는 null.
     */
    private volatile org.duckdns.hjow.swingwebpanel.render.FrameSource frames;

    /**
     * Sets the source of the frame pictures that the renderer paints.
     * <p>렌더러가 그리는 프레임 그림의 공급원을 정한다.
     * @param source the source or null / 공급원 또는 null
     */
    void setFrameSource(org.duckdns.hjow.swingwebpanel.render.FrameSource source) {
        // 이미 닫힌 리소스는 공급원을 받지 않는다.
        frames = cancel.isCancelled() ? null : source;
    }

    /**
     * Returns the picture of a frame element, or null if the page has no frame for it.
     * <p>프레임 요소의 그림을 반환한다. 페이지에 그 요소의 프레임이 없으면 null.
     * @param frameElement the iframe or frame element / iframe 또는 frame 요소
     * @param width width of the content box / 콘텐츠 상자의 너비
     * @param height height of the content box / 콘텐츠 상자의 높이
     * @return the picture or null / 그림 또는 null
     */
    public org.duckdns.hjow.swingwebpanel.render.FramePicture getFrame(org.duckdns.hjow.swingwebpanel.dom.Element frameElement, double width, double height) {
        org.duckdns.hjow.swingwebpanel.render.FrameSource source = frames;
        return source == null ? null : source.getFrame(frameElement, width, height);
    }

    /**
     * Returns the current picture of a canvas, or null if the script host has none.
     * <p>캔버스의 현재 그림을 반환한다. 스크립트 호스트에 없으면 null.
     * @param canvas the canvas element / 캔버스 요소
     * @return an immutable snapshot or null / 불변 스냅샷 또는 null
     */
    public org.duckdns.hjow.swingwebpanel.media.image.DecodedImage getCanvasImage(org.duckdns.hjow.swingwebpanel.dom.Element canvas) {
        CanvasSource source = canvases;
        return source == null ? null : source.getCanvasImage(canvas);
    }

    /**
     * Returns the state of an image that was already requested, without starting a request (null if it never was).
     * <p>이미 요청한 이미지의 상태를 요청을 시작하지 않고 반환한다(요청한 적이 없으면 null).
     * @param absoluteUrl image address / 이미지 주소
     * @return the state or null / 상태 또는 null
     */
    synchronized ImageResource peekImage(String absoluteUrl) {
        return peekImage(absoluteUrl, null);
    }

    /**
     * Returns the state of an image in a CORS mode that was already requested, without starting a request (null if it never was).
     * <p>
     * 이미 요청한 CORS 모드 이미지의 상태를 요청을 시작하지 않고 반환한다(요청한 적이 없으면 null).
     *
     * @param absoluteUrl image address / 이미지 주소
     * @param cors        <code>anonymous</code>, <code>use-credentials</code> or null for a plain image / <code>anonymous</code>, <code>use-credentials</code>. 일반 이미지는 null
     * @return the state or null / 상태 또는 null
     */
    synchronized ImageResource peekImage(String absoluteUrl, String cors) {
        Entry known = entries.get(imageKey(absoluteUrl, cors));
        return known == null ? null : known.image;
    }

    /**
     * Returns the key of the entry of an image: an image of an element with a <code>crossorigin</code> attribute is a different resource from
     * the same address fetched without CORS (the request carries <code>Origin</code> and the response is checked), and the two modes of the
     * attribute differ in the cookies, so each combination has its own entry.
     * <p>
     * 이미지 항목의 키를 반환한다: <code>crossorigin</code> 속성이 있는 요소의 이미지는 CORS 없이 가져온 같은 주소와 다른 리소스이고(요청이 <code>Origin</code> 을 싣고
     * 응답을 검사한다) 속성의 두 모드는 쿠키가 다르므로 조합마다 항목이 따로 있다.
     *
     * @param url  the image address / 이미지 주소
     * @param cors the CORS mode or null / CORS 모드. 없으면 null
     * @return the key / 키
     */
    private static String imageKey(String url, String cors) {
        return cors == null ? "img:" + url : "imgc:" + cors + ":" + url;
    }

    /**
     * Returns a cheap state of an image in a CORS mode (the renderer asks this for an <code>img</code> element with a <code>crossorigin</code>
     * attribute), starting a fetch only on its first lookup.
     * <p>
     * CORS 모드 이미지의 상태를 값싸게 반환한다(렌더러가 <code>crossorigin</code> 속성이 있는 <code>img</code> 요소에 대해 묻는다). 첫 조회에만 가져오기를 시작한다.
     *
     * @param absoluteUrl image address / 이미지 주소
     * @param cors        <code>anonymous</code> or <code>use-credentials</code> / <code>anonymous</code> 또는 <code>use-credentials</code>
     * @return immutable state / 불변 상태
     */
    public ImageResource getImage(String absoluteUrl, String cors) {
        return entry(absoluteUrl, false, false, cors).image;
    }

    /**
     * Returns a cheap image state, starting a fetch only on its first lookup.
     * <p>값싸게 이미지 상태를 반환하며 첫 조회에만 가져오기를 시작한다.
     * @param absoluteUrl image address / 이미지 주소
     * @return immutable state / 불변 상태
     */
    public ImageResource getImage(String absoluteUrl) {
        return entry(absoluteUrl, false).image;
    }

    /**
     * Reports the still-unconnected audio player explicitly; it never pretends that an address can be played.
     * <p>아직 연결하지 않은 오디오 재생기를 명시적으로 보고하며 재생 가능하다고 표시하지 않는다.
     * @param absoluteUrl audio address / 오디오 주소
     * @return unsupported state until P18 audio integration / P18 오디오 연결 전까지 미지원 상태
     */
    public ImageResource getMedia(String absoluteUrl) {
        synchronized (this) {
            ImageResource known = mediaStates.get(absoluteUrl);
            return known == null ? ImageResource.PENDING : known;
        }
    }

    /**
     * Records what a media element found out about a resource and asks the document queue to draw again: the renderer then shows the control
     * bar of a resource that can be played, and the unsupported URL with the alternative text for one that cannot.
     * <p>
     * 미디어 요소가 리소스에 대해 알아낸 것을 기록하고 문서 큐에 다시 그리게 한다: 그러면 렌더러는 재생할 수 있는 리소스에는 컨트롤 막대를, 그럴 수
     * 없는 리소스에는 미지원 URL 과 대체 텍스트를 보여 준다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param error       null if it can be played, else the reason / 재생할 수 있으면 null, 아니면 이유
     */
    void mediaState(String absoluteUrl, String error) {
        synchronized (this) {
            if (cancel.isCancelled()) {
                return;
            }
            mediaStates.put(absoluteUrl, error == null ? ImageResource.AVAILABLE : ImageResource.failed(error));
        }
        listener.resourceChanged(false);
    }

    /**
     * Returns an available stylesheet, or reports pending/failure without blocking the document queue. A terminal result
     * triggers another collection, including imports discovered in newly available sheets.
     * <p>사용 가능한 시트를 반환하거나 문서 큐를 막지 않고 대기·실패를 알린다. 최종 결과는 새 시트 안의 import까지 다시 수집하게 한다.
     * @param absoluteUrl stylesheet address / 스타일시트 주소
     * @return loaded stylesheet / 읽은 스타일시트
     * @throws IOException while pending or on failure / 대기 또는 실패 시
     */
    public LoadedStyleSheet load(String absoluteUrl) throws IOException {
        Entry state = entry(absoluteUrl, true);
        LoadedStyleSheet sheet = state.sheet;
        if (sheet != null) {
            return sheet;
        }
        throw new IOException(state.error == null ? "Stylesheet pending: " + absoluteUrl : state.error);
    }

    /**
     * Waits for one parser-blocking stylesheet on the loader thread. Disabling JS cancels only this wait; retiring the
     * page cancels its network resource as well. The EDT and confirmed document rendering never use this method.
     * <p>로더 스레드에서 파서를 막는 스타일시트 하나를 기다린다. JS 비활성화는 이 대기만 취소하며 페이지 폐기는 네트워크 리소스도 취소한다.
     * EDT와 확정 뒤 문서 렌더링은 이 메서드를 사용하지 않는다.
     * @param absoluteUrl stylesheet address / 스타일시트 주소
     * @param waiter script cancellation token / 스크립트 취소 토큰
     * @return completed stylesheet / 완료된 스타일시트
     * @throws IOException on failure, cancellation or interruption / 실패·취소·인터럽트 시
     */
    synchronized LoadedStyleSheet awaitStyleSheet(String absoluteUrl, CancelToken waiter) throws IOException {
        Entry state = entry(absoluteUrl, true);
        // CSS 완료 또는 페이지/스크립트 취소까지 기다리되 취소 토큰은 짧은 간격으로 다시 확인한다.
        while (state.sheet == null && state.error == null && !cancel.isCancelled() && !waiter.isCancelled()) {
            try {
                wait(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Stylesheet wait interrupted");
            }
        }
        cancel.throwIfCancelled();
        waiter.throwIfCancelled();
        if (state.sheet != null) {
            return state.sheet;
        }
        throw new IOException(state.error);
    }

    /**
     * Finds or reserves a state and submits at most one worker for each purpose/URL.
     * <p>목적·URL마다 상태를 조회하거나 예약하고 작업자를 최대 하나 제출한다.
     * @param url resource address / 리소스 주소
     * @param stylesheet whether it is a stylesheet / 스타일시트 여부
     * @return state / 상태
     */
    private synchronized Entry entry(final String url, final boolean stylesheet) {
        return entry(url, stylesheet, false);
    }

    /**
     * Finds or reserves the state of a stylesheet, an image or a web font (P10-05).
     * <p>스타일시트, 이미지, 웹 글꼴의 상태를 조회하거나 예약한다(P10-05).
     * @param url resource address / 리소스 주소
     * @param stylesheet whether it is a stylesheet / 스타일시트 여부
     * @param font whether it is a web font (then stylesheet is false) / 웹 글꼴 여부(그러면 stylesheet 는 false)
     * @return state / 상태
     */
    private synchronized Entry entry(final String url, final boolean stylesheet, final boolean font) {
        return entry(url, stylesheet, font, null);
    }

    /**
     * Finds or reserves the state of a stylesheet, an image (also in a CORS mode) or a web font.
     * <p>스타일시트, 이미지(CORS 모드 포함), 웹 글꼴의 상태를 조회하거나 예약한다.
     * @param url resource address / 리소스 주소
     * @param stylesheet whether it is a stylesheet / 스타일시트 여부
     * @param font whether it is a web font (then stylesheet is false) / 웹 글꼴 여부(그러면 stylesheet 는 false)
     * @param cors the CORS mode of an image or null / 이미지의 CORS 모드 또는 null
     * @return state / 상태
     */
    private synchronized Entry entry(final String url, final boolean stylesheet, final boolean font, final String cors) {
        String key = font ? "font:" + url : (stylesheet ? "css:" + url : imageKey(url, cors));
        Entry state = entries.get(key);
        if (state != null) {
            return state;
        }
        state = new Entry();
        // crossorigin 속성이 있는 요소의 이미지는 CORS 모드로 가져온다.
        if (!font && !stylesheet) {
            state.cors = cors;
        }
        if (cancel.isCancelled() || url == null || entries.size() >= MAX_ENTRIES) {
            state.fail("Resource cancelled or page resource limit exceeded");
            return state;
        }
        entries.put(key, state);
        final Entry pending = state;
        state.fetchStart = System.currentTimeMillis();
        try {
            workers.execute(new Runnable() {
                /** Fetches and decodes on a worker, never on the EDT or document queue. / EDT나 문서 큐 대신 작업자에서 가져오고 디코딩한다. */
                public void run() {
                    fetch(url, stylesheet, font, pending);
                }
            });
            // 작업자가 받아들여진 가져오기만 진행 중인 리소스로 센다. 이 메서드는 모니터를 잡고 있으므로 끝난 수가 먼저 늘지 않는다.
            started++;
        } catch (RejectedExecutionException e) {
            state.fail("Resource worker queue is full or closed");
        }
        return state;
    }

    /**
     * Fetches and decodes one resource, publishing a terminal state only while the page is alive.
     * <p>리소스 하나를 가져와 디코딩하고 페이지가 살아 있을 때만 최종 상태를 게시한다.
     * @param url resource URL / 리소스 URL
     * @param stylesheet stylesheet flag / 스타일시트 표시
     * @param font web font flag (P10-05) / 웹 글꼴 표시(P10-05)
     * @param target reserved state / 예약된 상태
     */
    private void fetch(String url, boolean stylesheet, boolean font, Entry target) {
        if (cancel.isCancelled()) {
            return;
        }
        final Thread worker = Thread.currentThread();
        CancelToken.Hook interrupt = new CancelToken.Hook() {
            /** Interrupts a decoder owned by this page. / 이 페이지의 디코더를 인터럽트한다. */
            public void onCancel() {
                worker.interrupt();
            }
        };
        cancel.addHook(interrupt);
        try {
            // 교차 오리진 웹 글꼴은 CORS 모드로 가져온다(CSS Fonts 4 4.3: 요청이 Origin 을 싣고 응답이 문서의 오리진을 허용해야 한다). 클래스패스 페이지는 클래스패스 글꼴만 읽는다.
            boolean crossOriginFont = font && !fontAllowed(url);
            if (crossOriginFont && pageUrl != null && pageUrl.startsWith("classpath:")) {
                throw new IOException("web fonts of a class path page must come from the class path: " + url);
            }
            ResourceRequest request = new ResourceRequest(url).setUserAgent(context.userAgent).setExtraHeaders(context.headers())
                    .setCookies(cookies).setCancelToken(cancel).setReferrer(pageUrl).setInitiator(Origin.of(pageUrl))
                    .setBlobUrlScope(blobUrls)
                    .setBlobUrlEntry(!font && !stylesheet ? context.documentMedia : null)
                    .setEmbedderPolicy(context.crossOriginPolicy.getEmbedder())
                    .setPurpose(font ? RequestPurpose.FONT : (stylesheet ? RequestPurpose.STYLESHEET : RequestPurpose.IMAGE))
                    .setCacheMode(context.force ? ResourceRequest.CacheMode.FORCE_RELOAD : ResourceRequest.CacheMode.DEFAULT);
            // 웹 글꼴은 항상 anonymous CORS 모드이다: 교차 오리진 요청에는 자격 증명(쿠키)을 보내지 않는다.
            if (font) {
                request.setSameOriginCredentialsOnly(true);
            }
            String cors = font || stylesheet ? null : target.cors;
            // crossorigin 속성이 있는 이미지는 CORS 모드로 가져온다: 교차 오리진이면 Origin 헤더를 싣고, anonymous 이면 쿠키를 같은 오리진에만 보낸다.
            if (cors != null) {
                request.setPurpose(RequestPurpose.IMAGE_CORS);
                request.setSameOriginCredentialsOnly(!"use-credentials".equals(cors));
            }
            ResourceResponse response = fetcher.fetch(request);
            if (response.isError()) {
                throw new IOException(response.getStatus() + " " + response.getFailureMessage());
            }
            cancel.throwIfCancelled();
            // CORS 모드의 교차 오리진 이미지는 응답이 문서의 오리진을 허용해야 한다(아니면 이미지는 로드되지 않고 error 이벤트가 난다).
            if (cors != null && !corsGranted(response, "use-credentials".equals(cors))) {
                throw new IOException("CORS: the image at " + response.getFinalUrl() + " does not allow the origin of this document");
            }
            // 교차 오리진 글꼴은 응답이 문서의 오리진을 허용해야 쓸 수 있다(아니면 글꼴은 적재되지 않고 다음 패밀리를 쓴다).
            if (crossOriginFont && !corsGranted(response, false)) {
                throw new IOException("CORS: the font at " + response.getFinalUrl() + " does not allow the origin of this document");
            }
            LoadedStyleSheet sheet = null;
            ImageResource image = null;
            long size;
            if (font) {
                // 웹 글꼴은 바이트만 보관한다(디코딩은 레이아웃이 글꼴을 쓸 때 WebFontSet 이 한다).
                byte[] bytes = response.getBody();
                if (bytes.length > MAX_FONT_BYTES) {
                    throw new IOException("web font is larger than the limit of " + MAX_FONT_BYTES + " bytes");
                }
                target.fetchDone = System.currentTimeMillis();
                target.bytes = bytes;
                size = bytes.length;
            } else if (stylesheet) {
                String text = StyleSheetDecoder.decode(response.getBody(), response.getCharset(), "UTF-8");
                sheet = new LoadedStyleSheet(text, response.getFinalUrl(), response.getBlobUrlEntry() != null);
                size = (long) text.length() * 2;
            } else {
                DecodedImage decoded = ImageDecoders.decode(response.getBody(), response.getMimeType(), IMAGE_LIMITS);
                image = ImageResource.loaded(decoded, response.getFinalUrl(), response.getHeader("Access-Control-Allow-Origin"),
                        response.getHeader("Access-Control-Allow-Credentials"));
                size = (long) decoded.getWidth() * decoded.getHeight() * decoded.getFrames().size() * 4;
            }
            synchronized (this) {
                if (cancel.isCancelled()) {
                    return;
                }
                // 페이지 전체 보관 한도를 넘는 결과는 화면에 설치하기 전에 거부한다.
                if (size > MAX_RETAINED_BYTES - retainedBytes) {
                    throw new IOException("Decoded page resource limit exceeded");
                }
                retainedBytes += size;
                target.sheet = sheet;
                if (image != null) {
                    target.image = image;
                }
            }
        } catch (Exception e) {
            target.fail(String.valueOf(e));
        } finally {
            cancel.removeHook(interrupt);
            synchronized (this) {
                // 성공이든 실패든 이 가져오기는 끝난 것으로 센다. 실패한 리소스 하나가 로딩 완료를 영원히 막지 않는다.
                // close() 가 이미 모두 끝난 것으로 센 뒤에 인터럽트된 작업자가 다시 세어 시작한 수를 넘지 않게 한다.
                if (finished < started) {
                    finished++;
                }
                notifyAll();
            }
        }
        if (!cancel.isCancelled()) {
            notifyWaiters(target);
            listener.resourceChanged(stylesheet && !font);
        }
    }

    /**
     * Tells whether the response of a CORS-mode image grants access to this document: a same-origin response always does; a cross-origin one
     * must name the origin of the document in <code>Access-Control-Allow-Origin</code> (or <code>*</code> without credentials) and, with
     * credentials, answer <code>Access-Control-Allow-Credentials: true</code>. A document with an opaque origin (a page made with
     * <code>setContent</code>) sends <code>Origin: null</code> and is granted by <code>*</code> (without credentials) and by <code>null</code>.
     * <p>
     * CORS 모드 이미지의 응답이 이 문서에 접근을 허용하는지 알려 준다: 같은 오리진의 응답은 늘 허용하고, 교차 오리진 응답은 <code>Access-Control-Allow-Origin</code> 에
     * 문서의 오리진(자격 증명이 없으면 <code>*</code> 도)을 적어야 하며 자격 증명이 있으면 <code>Access-Control-Allow-Credentials: true</code> 로 답해야 한다.
     * 불투명 오리진 문서(<code>setContent</code> 로 만든 페이지)는 <code>Origin: null</code> 을 보내며 <code>*</code>(자격 증명 없이)와 <code>null</code> 로 허용된다.
     *
     * @param response    the response / 응답
     * @param credentials true for <code>use-credentials</code> / <code>use-credentials</code> 이면 true
     * @return true if the image may be used and read / 이미지를 쓰고 읽어도 되면 true
     */
    private boolean corsGranted(ResourceResponse response, boolean credentials) {
        if (response.getBlobUrlEntry() != null) {
            return true;
        }
        Origin page = Origin.of(pageUrl);
        Origin from = Origin.of(response.getFinalUrl());
        if (!page.isOpaque() && page.isSameOrigin(from)) {
            return true;
        }
        return OriginPolicy.corsAllowsDocument(page, response.getHeader("Access-Control-Allow-Origin"), response.getHeader("Access-Control-Allow-Credentials"),
                credentials);
    }

    /**
     * Returns the bytes of a web font (P10-05): the request starts when the font is asked for the first time, the result is kept by the
     * page (the same cache, the same limits and the same headers as every other resource) and the page is laid out again when it
     * arrives.
     * <p>
     * 웹 글꼴의 바이트를 반환한다(P10-05): 글꼴을 처음 물을 때 요청이 시작되고, 결과는 페이지가 보관하며(다른 리소스와 같은 캐시, 한도, 헤더) 도착하면 페이지를 다시
     * 배치한다.
     *
     * @param absoluteUrl the font address / 글꼴 주소
     * @return the bytes, or null while the request runs / 바이트. 요청이 진행 중이면 null
     * @throws IOException if the request failed or is not allowed / 요청이 실패했거나 허용되지 않으면
     */
    byte[] fontBytes(String absoluteUrl) throws IOException {
        Entry state = entry(absoluteUrl, false, true);
        if (state.error != null) {
            throw new IOException(state.error);
        }
        return state.bytes;
    }

    /**
     * Returns when the request of a web font started and when its bytes arrived (<code>font-display</code> compares them with the block and swap periods).
     * <p>
     * 웹 글꼴 요청이 언제 시작되었고 바이트가 언제 도착했는지 반환한다(<code>font-display</code> 가 block 기간, swap 기간과 비교한다).
     *
     * @param absoluteUrl the font address / 글꼴 주소
     * @return {start, arrival} in ms of the system clock (arrival is 0 until the bytes are there) / 시스템 시계 ms 의 {시작, 도착}(도착은 바이트가 올 때까지 0)
     */
    long[] fontTimes(String absoluteUrl) {
        Entry state = entry(absoluteUrl, false, true);
        return new long[] { state.fetchStart, state.fetchDone };
    }

    /**
     * Tells whether a web font may be read from an address: only from the origin of the page (or from the class path for a class path
     * page), because a cross-origin font needs a CORS permission that this layer cannot ask for yet.
     * <p>
     * 웹 글꼴을 주소에서 읽어도 되는지 알려 준다: 페이지의 오리진(클래스패스 페이지는 클래스패스)에서만. 교차 오리진 글꼴은 이 계층이 아직 요청할 수 없는 CORS 허용이 필요하기
     * 때문이다.
     *
     * @param url the font address / 글꼴 주소
     * @return true if allowed / 허용되면 true
     */
    private boolean fontAllowed(String url) {
        if ("blob".equals(UrlUtil.getScheme(url))) {
            BlobUrlStore.Scope scope = blobUrls;
            return scope != null && scope.resolve(url) != null;
        }
        if (pageUrl != null && pageUrl.startsWith("classpath:")) {
            return url.startsWith("classpath:");
        }
        return Origin.of(pageUrl).isSameOrigin(Origin.of(url));
    }

    /**
     * Starts the load of an image (or joins the load that exists) and tells the waiter when the state is final. The same cached entry is used
     * by the renderer, so an image that the script loader fetched is not fetched again for painting.
     * <p>
     * 이미지 로드를 시작하고(이미 있으면 거기에 합류하고) 상태가 확정되면 기다리는 쪽에 알린다. 렌더러가 같은 캐시 항목을 쓰므로 스크립트 로더가
     * 가져온 이미지는 그리기를 위해 다시 가져오지 않는다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param waiter      the receiver / 받는 쪽
     */
    void requestImage(String absoluteUrl, ImageWaiter waiter) {
        requestImage(absoluteUrl, null, waiter);
    }

    /**
     * Starts the load of an image of an element that may have a <code>crossorigin</code> attribute. With a CORS mode the image is fetched in
     * CORS mode (it has an entry of its own, see {@link #imageKey}): a cross-origin response that does not grant access to this document ends
     * in an error state, so the <code>error</code> event fires and a canvas never receives its pixels; a granted one can be read back without
     * tainting a canvas.
     * <p>
     * <code>crossorigin</code> 속성이 있을 수 있는 요소의 이미지 로드를 시작한다. CORS 모드가 있으면 이미지를 CORS 모드로 가져온다(항목이 따로 있다.
     * {@link #imageKey} 참조): 이 문서에 접근을 허용하지 않는 교차 오리진 응답은 오류 상태로 끝나 <code>error</code> 이벤트가 나고 캔버스는 그 화소를 받지 못하며,
     * 허용한 응답은 캔버스를 오염시키지 않고 다시 읽을 수 있다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param cors        <code>anonymous</code> or <code>use-credentials</code>, null for no <code>crossorigin</code> attribute / <code>anonymous</code> 또는
     *                    <code>use-credentials</code>. <code>crossorigin</code> 속성이 없으면 null
     * @param waiter      the receiver / 받는 쪽
     */
    void requestImage(String absoluteUrl, String cors, ImageWaiter waiter) {
        Entry state = entry(absoluteUrl, false, false, cors);
        ImageResource done = null;
        synchronized (this) {
            // 이미 끝난 항목이면 바로 알리고, 아니면 완료 때 알릴 목록에 올린다.
            if (state.image.getState() != ImageResource.State.PENDING) {
                done = state.image;
            } else {
                state.waiters.add(waiter);
            }
        }
        if (done != null) {
            waiter.imageDone(done);
        }
    }

    /**
     * Fetches the resource of an <code>audio</code> element on a thread of its own with the common request layer (user agent, extra headers,
     * cookies, SSL hook, scheme restrictions, HTTP cache; the purpose is audio, so a cross-origin resource may be played but its bytes never
     * reach a script). The result is the whole response body and its MIME type, or the reason of the failure; it is reported once, unless the
     * request or the page was cancelled. Callable from any thread. Large or size-unknown audio bodies can be spooled for playback.
     * <p>
     * <code>audio</code> 요소의 리소스를 공통 요청 계층(User-Agent, 추가 헤더, 쿠키, SSL 훅, 스키마 제한, HTTP 캐시)으로 전용 스레드에서 가져온다
     * (목적은 오디오이므로 교차 오리진 리소스도 재생할 수 있지만 바이트는 스크립트에 전달되지 않는다). 결과는 응답 본문 전체와 MIME 타입 또는 실패
     * 이유이며, 요청이나 페이지가 취소되지 않는 한 한 번 알린다. 어느 스레드에서나 부를 수 있다. 크기가 크거나 알려지지 않은 오디오 본문은 재생용 스풀에 받을 수 있다.
     *
     * @param absoluteUrl the absolute URL / 절대 URL
     * @param callback    the receiver / 받는 쪽
     * @return the handle for cancelling / 취소용 핸들
     */
    MediaRequestSource.Request requestMedia(final String absoluteUrl, final MediaRequestSource.Callback callback) {
        final CancelToken token = new CancelToken();
        final AtomicBoolean reported = new AtomicBoolean();
        // 페이지가 닫히면 이 요청도 취소되도록 연결한다.
        final CancelToken.Hook link = new CancelToken.Hook() {
            /** Cancels the request together with the page. / 페이지와 함께 요청을 취소한다. */
            public void onCancel() {
                token.cancel();
            }
        };
        if (cancel.isCancelled()) {
            token.cancel();
        } else {
            cancel.addHook(link);
        }
        Thread thread = new Thread(new Runnable() {
            /** Fetches the resource and reports the result. / 리소스를 가져와 결과를 알린다. */
            public void run() {
                AudioBody audio = callback instanceof MediaRequestSource.AudioCallback ? new AudioBody() : null;
                byte[] data = null;
                MediaData encoded = null;
                String mime = null;
                String error = null;
                try {
                    ResourceRequest request = new ResourceRequest(absoluteUrl).setUserAgent(context.userAgent).setExtraHeaders(context.headers())
                            .setCookies(cookies).setCancelToken(token).setReferrer(pageUrl).setInitiator(Origin.of(pageUrl))
                            .setBlobUrlScope(blobUrls)
                            .setBlobUrlEntry(context.documentMedia)
                            .setEmbedderPolicy(context.crossOriginPolicy.getEmbedder())
                            .setPurpose(RequestPurpose.AUDIO)
                            .setCacheMode(context.force ? ResourceRequest.CacheMode.FORCE_RELOAD : ResourceRequest.CacheMode.DEFAULT);
                    // 받는 쪽이 진행 상황도 원하면 전송 중에 도착한 바이트 수를 알려 준다(취소되었거나 페이지가 닫혔으면 알리지 않는다).
                    if (callback instanceof MediaRequestSource.ProgressCallback) {
                        final MediaRequestSource.ProgressCallback watcher = (MediaRequestSource.ProgressCallback) callback;
                        request.setProgressListener(new org.duckdns.hjow.swingwebpanel.net.ProgressListener() {
                            /** Passes the progress on while the request is alive. / 요청이 살아 있는 동안 진행 상황을 전달한다. */
                            public void onProgress(long received, long total) {
                                if (!token.isCancelled() && !cancel.isCancelled() && !reported.get()) {
                                    watcher.progress(received, total);
                                }
                            }
                        });
                    }
                    ResourceResponse response = fetcher.fetch(request, audio);
                    if (response.isError()) {
                        error = response.getStatus() + " " + response.getFailureMessage();
                    } else {
                        if (audio == null) {
                            data = response.getBody();
                        } else {
                            encoded = audio.finish(response);
                        }
                        mime = response.getMimeType();
                    }
                } catch (Exception e) {
                    error = String.valueOf(e);
                } finally {
                    if (audio != null) {
                        audio.discard();
                    }
                    cancel.removeHook(link);
                }
                try {
                    // 요청과 페이지가 살아 있을 때만 결과를 알리고 오디오 저장소의 소유권을 넘긴다.
                    if (!token.isCancelled() && !cancel.isCancelled() && reported.compareAndSet(false, true)) {
                        if (audio == null) {
                            callback.done(error == null ? data : null, mime, error);
                        } else {
                            MediaData owned = encoded;
                            encoded = null;
                            ((MediaRequestSource.AudioCallback) callback).doneAudio(owned, mime, error);
                        }
                    }
                } finally {
                    if (encoded != null) {
                        encoded.release();
                    }
                }
            }
        }, "swingwebpanel-media-fetch");
        thread.setDaemon(true);
        thread.start();
        return new MediaRequestSource.Request() {
            /** Cancels the fetch; the callback is not called after this. / 가져오기를 취소한다. 이후 콜백은 호출되지 않는다. */
            public void cancel() {
                token.cancel();
                cancel.removeHook(link);
            }
        };
    }

    /**
     * Chooses the audio body destination after headers arrive. / 헤더가 도착한 뒤 오디오 본문의 저장소를 정한다.
     */
    private static final class AudioBody implements ResponseHandler {

        /** Body spool for the actual response, if selected. / 선택된 경우 실제 응답의 본문 스풀. */
        private MediaSpool spool;

        /** Creates the per-request handler. / 요청별 처리기를 만든다. */
        AudioBody() {
        }

        /**
         * Streams large, unknown-length, or transport-compressed bodies. / 크거나 길이 미상이거나 전송 중 압축된 본문을 스트림으로 받는다.
         * @param head response head / 응답 헤더
         * @return spool or null for the regular memory cache / 스풀 또는 일반 메모리 캐시를 쓰려면 null
         * @throws IOException if the declared uncompressed body is too large / 선언한 비압축 본문이 너무 크면
         */
        public OutputStream onResponse(ResourceResponse head) throws IOException {
            discard();
            long size = head.getDeclaredLength();
            String encoding = head.getHeader("Content-Encoding");
            boolean uncompressed = encoding == null || encoding.trim().length() == 0 || "identity".equalsIgnoreCase(encoding.trim());
            if (uncompressed && size > MediaSpool.MAX_BYTES) {
                throw new IOException("audio body exceeds the encoded file limit");
            }
            if (uncompressed && size >= 0 && size <= MediaSpool.MEMORY_LIMIT) {
                return null;
            }
            spool = new MediaSpool();
            return spool;
        }

        /**
         * Transfers the completed body to the caller. / 완료한 본문 소유권을 호출자에게 넘긴다.
         * @param response final response / 최종 응답
         * @return owned media data / 소유할 미디어 데이터
         * @throws IOException if the spool cannot finish / 스풀을 마무리하지 못하면
         */
        MediaData finish(ResourceResponse response) throws IOException {
            if (response.isStreamed()) {
                if (spool == null) {
                    throw new IOException("no storage for the streamed audio body");
                }
                return spool.finish();
            }
            return MediaData.of(response.getBody());
        }

        /** Removes the unfinished body, if any. / 미완성 본문이 있으면 없앤다. */
        void discard() {
            if (spool != null) {
                spool.discard();
                spool = null;
            }
        }
    }

    /**
     * Tells the waiters of an entry that its state is final; the waiters are called outside the monitor.
     * <p>
     * 항목의 상태가 확정되었음을 기다리던 쪽에 알린다. 모니터 밖에서 호출한다.
     *
     * @param state the entry / 항목
     */
    private void notifyWaiters(Entry state) {
        List<ImageWaiter> list;
        synchronized (this) {
            if (state.waiters.isEmpty()) {
                return;
            }
            list = new ArrayList<ImageWaiter>(state.waiters);
            state.waiters.clear();
        }
        // 기다리던 쪽을 하나씩 부른다. 한쪽의 예외가 다른 쪽에 영향을 주지 않게 한다.
        for (int i = 0; i < list.size(); i++) {
            try {
                list.get(i).imageDone(state.image);
            } catch (RuntimeException e) {
                // 받는 쪽의 오류는 다른 대기자에게 전파하지 않는다.
                continue;
            }
        }
    }

    /**
     * Returns how many fetches ended and how many were started (stylesheets and images of this page, each address once).
     * <p>
     * 끝난 가져오기 수와 시작한 가져오기 수를 반환한다(이 페이지의 스타일시트와 이미지, 주소마다 한 번).
     *
     * @return {finished, started} / {끝난 수, 시작한 수}
     */
    synchronized int[] activity() {
        return new int[] { finished, started };
    }

    /**
     * Tells whether a started fetch is still running.
     * <p>
     * 시작한 가져오기가 아직 진행 중인지 알려 준다.
     *
     * @return true while one is running / 진행 중인 것이 있으면 true
     */
    synchronized boolean hasPending() {
        return finished < started;
    }

    /** Cancels all page resources and drops retained states; safe from any thread. / 페이지 리소스를 모두 취소하고 보관 상태를 놓으며 어느 스레드에서나 안전하다. */
    void close() {
        cancel.cancel();
        // 폐기된 페이지의 스크립트 호스트(캔버스 그림 공급원)를 붙잡지 않는다.
        canvases = null;
        frames = null;
        synchronized (this) {
            entries.clear();
            retainedBytes = 0;
            // 폐기된 페이지는 더 기다릴 것이 없다.
            finished = started;
            notifyAll();
        }
    }

    /** Volatile terminal values published by a worker. / 작업자가 게시하는 volatile 최종 값. */
    private static final class Entry {
        /** Current immutable image state. / 현재 불변 이미지 상태. */
        volatile ImageResource image = ImageResource.PENDING;
        /** Loaded stylesheet or null. / 읽은 시트 또는 null. */
        volatile LoadedStyleSheet sheet;
        /** Bytes of a loaded web font or null. 읽은 웹 글꼴의 바이트 또는 null. */
        volatile byte[] bytes;
        /** The system time in ms at which the request of a web font was started (for <code>font-display</code>). 웹 글꼴 요청을 시작한 시스템 시각(ms). <code>font-display</code> 용. */
        volatile long fetchStart;
        /** The system time in ms at which the bytes of a web font arrived, 0 until then. 웹 글꼴의 바이트가 도착한 시스템 시각(ms). 그때까지는 0. */
        volatile long fetchDone;
        /** The CORS mode of an image (anonymous or use-credentials), null for a plain image; set before the fetch starts. / 이미지의 CORS 모드(anonymous 또는 use-credentials). 일반 이미지는 null. 가져오기를 시작하기 전에 정한다. */
        volatile String cors;
        /** Failure text, null while pending. / 실패 문자열이며 대기 중에는 null. */
        volatile String error;
        /** Receivers of the final image state; guarded by the monitor of the resources. / 최종 이미지 상태를 받을 쪽. 리소스의 모니터로 보호한다. */
        final List<ImageWaiter> waiters = new ArrayList<ImageWaiter>(2);
        /** Creates a pending entry. / 대기 항목을 만든다. */
        Entry() {
        }
        /** Publishes failure for either resource type. / 어느 리소스 종류든 실패를 게시한다.
         * @param message failure description / 실패 설명 */
        void fail(String message) {
            error = message;
            image = ImageResource.failed(message);
        }
    }
}
