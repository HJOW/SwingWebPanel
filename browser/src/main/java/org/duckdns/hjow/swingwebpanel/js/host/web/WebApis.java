package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.net.BlobUrlStore;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The asynchronous web request APIs of one page (plan item P20-08): <code>XMLHttpRequest</code>, <code>fetch()</code> with
 * <code>Request</code>, <code>Response</code> and <code>Headers</code>, <code>AbortController</code>/<code>AbortSignal</code>,
 * <code>FormData</code>, <code>Blob</code>/<code>File</code>/<code>FileReader</code>, <code>TextEncoder</code>/<code>TextDecoder</code>, <code>URL</code> and
 * <code>URLSearchParams</code>. This object installs them on the page's
 * global object and owns what they share: the delivery of request results to the thread that owns the page's JavaScript, the list of
 * requests that are still running, and the cancellation of all of them when the page is replaced, its scripts are switched off or it is
 * disposed. The network itself is never touched here: every request goes through the {@link WebRequestSource} of the
 * {@link HostEnvironment}, which is the component's common request layer.
 * <p>
 * Threads: a request runs on a request thread of the component. Its results are queued here and run one by one as page tasks
 * ({@link PageScripts#runTask(Runnable)}) through the {@link Scheduler} of the page, so script code runs only on the page's own thread,
 * in order, followed by the microtask checkpoint. A result that arrives after the page was disposed or its scripts were stopped never
 * runs.
 * <p>
 * 한 페이지의 비동기 웹 요청 API(계획 P20-08): <code>XMLHttpRequest</code>, <code>Request</code>/<code>Response</code>/<code>Headers</code> 를 가진
 * <code>fetch()</code>, <code>AbortController</code>/<code>AbortSignal</code>, <code>FormData</code>, <code>Blob</code>/<code>File</code>, <code>URL</code>,
 * <code>URLSearchParams</code>(<code>FileReader</code>, <code>TextEncoder</code>, <code>TextDecoder</code> 포함). 이 객체가 이들을 페이지의 전역 객체에 설치하고 공유하는 것을 소유한다: 요청 결과를 페이지 JavaScript 소유 스레드에
 * 전달하는 일, 아직 실행 중인 요청의 목록, 페이지가 교체되거나 스크립트가 꺼지거나 폐기될 때 모두 취소하는 일. 네트워크는 여기서 건드리지 않는다. 모든
 * 요청은 {@link HostEnvironment} 의 {@link WebRequestSource}, 곧 컴포넌트의 공통 요청 계층을 거친다.
 * <p>
 * 스레드: 요청은 컴포넌트의 요청 스레드에서 실행된다. 결과는 여기에 줄 세워졌다가 페이지의 {@link Scheduler} 를 통해 하나씩 페이지 작업
 * ({@link PageScripts#runTask(Runnable)})으로 실행되므로 스크립트 코드는 페이지 자신의 스레드에서만 순서대로 실행되고 마이크로태스크 검사점이 뒤따른다.
 * 페이지가 폐기되거나 스크립트가 중단된 뒤에 도착한 결과는 실행되지 않는다.
 */
public final class WebApis {

    /**
     * What these APIs need from the script context that they are installed in: a page or a worker (plan item P20-09-b). Everything the
     * request APIs used to read from the page goes through this interface, so the same code runs in a dedicated worker, which has its own
     * realm and its own thread but no document.
     * <p>
     * 이 API가 설치되는 스크립트 문맥(페이지 또는 worker. 계획 P20-09-b)에서 필요로 하는 것. 요청 API가 페이지에서 읽던 모든 것이 이 인터페이스를
     * 거치므로, 같은 코드가 자기 realm 과 스레드는 있으나 문서가 없는 전용 worker 에서도 실행된다.
     */
    public interface Host {
        /**
         * Returns the realm that the APIs are installed in.
         * <p>
         * API가 설치되는 realm 을 반환한다.
         *
         * @return the realm / realm
         */
        JsRealm getRealm();

        /**
         * Returns the DOM bridge of the realm (it holds the event interfaces that the APIs build on).
         * <p>
         * realm 의 DOM 연결을 반환한다(API가 기반으로 삼는 이벤트 인터페이스가 들어 있다).
         *
         * @return the bridge / 연결
         */
        DomBridge getBridge();

        /**
         * Returns the request service of the component, or null if requests cannot be made.
         * <p>
         * 컴포넌트의 요청 서비스를 반환한다. 요청을 할 수 없으면 null.
         *
         * @return the service or null / 서비스 또는 null
         */
        WebRequestSource getWebRequestSource();

        /**
         * Returns the scheduler on which results are delivered (it runs tasks on the thread that owns the realm), or null for none.
         * <p>
         * 결과를 전달하는 스케줄러(realm 을 소유한 스레드에서 작업을 실행한다)를 반환한다. 없으면 null.
         *
         * @return the scheduler or null / 스케줄러 또는 null
         */
        Scheduler getScheduler();

        /**
         * Tells whether results may still be delivered: the context lives and its scripts run.
         * <p>
         * 결과를 아직 전달할 수 있는지 알려 준다. 문맥이 살아 있고 스크립트가 실행되는 중이어야 한다.
         *
         * @return true while active / 활성이면 true
         */
        boolean isActive();

        /**
         * Runs a task of the context: under its lock and limits, followed by the microtask checkpoint.
         * <p>
         * 문맥의 작업을 실행한다: 문맥의 잠금과 제한 아래에서 실행하고 마이크로태스크 검사점이 뒤따른다.
         *
         * @param task the work / 작업
         */
        void runTask(Runnable task);

        /**
         * Returns the URL of the context: the document URL of a page, the script URL of a worker.
         * <p>
         * 문맥의 URL을 반환한다. 페이지는 문서 URL, worker 는 스크립트 URL.
         *
         * @return the URL / URL
         */
        String getDocumentUrl();

        /**
         * Returns the URL that relative request URLs resolve against.
         * <p>
         * 상대 요청 URL을 해석하는 기준 URL을 반환한다.
         *
         * @return the base URL or null / 기준 URL 또는 null
         */
        String getBaseUrl();

        /**
         * Tells whether the context has documents, so that <code>XMLHttpRequest</code> may build a response document (a worker has not).
         * <p>
         * 문맥에 문서가 있어 <code>XMLHttpRequest</code> 가 응답 문서를 만들 수 있는지 알려 준다(worker 는 없다).
         *
         * @return true for a page / 페이지이면 true
         */
        boolean hasDocuments();
    }

    /**
     * The {@link Host} of a page: everything is read from the {@link PageScripts}.
     * <p>
     * 페이지의 {@link Host}: 모든 것을 {@link PageScripts} 에서 읽는다.
     */
    private static final class PageHost implements ObjectUrlHost {
        /** The page. / 페이지. */
        private final PageScripts page;

        /**
         * Creates the host.
         * <p>
         * 호스트를 만든다.
         *
         * @param page the page / 페이지
         */
        PageHost(PageScripts page) {
            this.page = page;
        }

        /**
         * Returns the realm of the page.
         * <p>
         * 페이지의 realm 을 반환한다.
         *
         * @return the realm / realm
         */
        public JsRealm getRealm() {
            return page.getRealm();
        }

        /**
         * Returns the DOM bridge of the page.
         * <p>
         * 페이지의 DOM 연결을 반환한다.
         *
         * @return the bridge / 연결
         */
        public DomBridge getBridge() {
            return page.getBridge();
        }

        /**
         * Returns the request service of the component.
         * <p>
         * 컴포넌트의 요청 서비스를 반환한다.
         *
         * @return the service or null / 서비스 또는 null
         */
        public WebRequestSource getWebRequestSource() {
            return page.getEnvironment().getWebRequestSource();
        }

        /**
         * Returns the scheduler of the page.
         * <p>
         * 페이지의 스케줄러를 반환한다.
         *
         * @return the scheduler or null / 스케줄러 또는 null
         */
        public Scheduler getScheduler() {
            return page.getEnvironment().getScheduler();
        }

        /**
         * Tells whether the page lives and its scripts are enabled.
         * <p>
         * 페이지가 살아 있고 스크립트가 켜져 있는지 알려 준다.
         *
         * @return true while active / 활성이면 true
         */
        public boolean isActive() {
            return !page.isDisposed() && page.getEnvironment().isScriptingEnabled();
        }

        /**
         * Runs a page task.
         * <p>
         * 페이지 작업을 실행한다.
         *
         * @param task the work / 작업
         */
        public void runTask(Runnable task) {
            page.runTask(task);
        }

        /**
         * Returns the document URL.
         * <p>
         * 문서 URL을 반환한다.
         *
         * @return the URL / URL
         */
        public String getDocumentUrl() {
            Document d = page.getDocument();
            String u = d.getUrl();
            return u == null ? "about:blank" : u;
        }

        /**
         * Returns the base URL of the document.
         * <p>
         * 문서의 기준 URL을 반환한다.
         *
         * @return the base URL or null / 기준 URL 또는 null
         */
        public String getBaseUrl() {
            return page.getDocument().getBaseUri();
        }

        /**
         * A page has documents.
         * <p>
         * 페이지에는 문서가 있다.
         *
         * @return true / true
         */
        public boolean hasDocuments() {
            return true;
        }

        /** Returns the scope supplied by the component. / 컴포넌트가 제공한 범위를 반환한다. */
        public BlobUrlStore.Scope getBlobUrlScope() {
            return page.getEnvironment().getBlobUrlScope();
        }
    }

    /** A host that supplies the component's object URL scope to a realm. / 렐름에 컴포넌트의 객체 URL 범위를 제공하는 호스트. */
    public interface ObjectUrlHost extends Host {
        /** Returns the realm's object URL scope, if attached. / 연결되어 있으면 렐름의 객체 URL 범위를 반환한다. */
        BlobUrlStore.Scope getBlobUrlScope();
    }

    /** The context that the APIs are installed in. / API가 설치된 문맥. */
    private final Host host;

    /** The realm of the context. / 문맥의 realm. */
    private final JsRealm realm;

    /** The DOM bridge of the context. / 문맥의 DOM 연결. */
    private final DomBridge bridge;

    /** The results that wait for the page thread, in arrival order. / 페이지 스레드를 기다리는 결과(도착 순서). */
    private final ConcurrentLinkedQueue<Runnable> inbox = new ConcurrentLinkedQueue<Runnable>();

    /** The requests that have not ended yet; guarded by itself. / 아직 끝나지 않은 요청. 자기 자신으로 보호한다. */
    private final Set<WebRequestSource.Call> calls = new LinkedHashSet<WebRequestSource.Call>();

    /** True after disposal. / 폐기된 뒤에는 true. */
    private volatile boolean closed;

    /** Takes one result from the inbox and runs it as a page task. / 받은 결과 하나를 꺼내 페이지 작업으로 실행한다. */
    private final Runnable drain = new Runnable() {
        public void run() {
            final Runnable next = inbox.poll();
            if (next == null || closed) {
                return;
            }
            host.runTask(next);
        }
    };

    /** The Abort API installer. / 중단(Abort) API 설치기. */
    AbortApi abort;

    /** The Blob and File installer. / Blob 과 File 설치기. */
    BlobApi blobs;

    /** The URLSearchParams installer. / URLSearchParams 설치기. */
    SearchParamsApi params;

    /** The FormData installer. / FormData 설치기. */
    FormDataApi forms;

    /** The Headers installer. / Headers 설치기. */
    HeadersApi headers;

    /** The readable streams for this realm. / 이 렐름의 읽기 스트림. */
    ReadableStreams streams;

    /** Scope of object URLs owned by this realm. / 이 렐름이 보유한 객체 URL 범위. */
    final BlobUrlStore.Scope blobUrls;

    /**
     * Creates the object; use {@link #install(PageScripts)} or {@link #install(Host)}.
     * <p>
     * 객체를 만든다. {@link #install(PageScripts)} 나 {@link #install(Host)} 를 쓴다.
     *
     * @param host the context / 문맥
     */
    private WebApis(Host host) {
        this.host = host;
        this.realm = host.getRealm();
        this.bridge = host.getBridge();
        BlobUrlStore.Scope scope = host instanceof ObjectUrlHost ? ((ObjectUrlHost) host).getBlobUrlScope() : null;
        blobUrls = scope == null ? new BlobUrlStore().open(host.getDocumentUrl()) : scope;
    }

    /**
     * Installs all web request APIs on the global object of a page. Call it once while the page is created (it is part of the window
     * installation).
     * <p>
     * 페이지의 전역 객체에 모든 웹 요청 API를 설치한다. 페이지를 만드는 동안(window 설치의 일부로) 한 번 부른다.
     *
     * @param page the page, not null / 페이지(null 불가)
     * @return the installed APIs / 설치된 API
     */
    public static WebApis install(PageScripts page) {
        return install(new PageHost(page));
    }

    /**
     * Installs all web request APIs on the global object of any script context, for example a dedicated worker (plan item P20-09-b). The
     * context must have the event interfaces in its DOM bridge.
     * <p>
     * 임의의 스크립트 문맥(예: 전용 worker)의 전역 객체에 모든 웹 요청 API를 설치한다(계획 P20-09-b). 문맥의 DOM 연결에 이벤트 인터페이스가 있어야 한다.
     *
     * @param host the context, not null / 문맥(null 불가)
     * @return the installed APIs / 설치된 API
     */
    public static WebApis install(Host host) {
        WebApis w = new WebApis(host);
        JsObject g = w.realm.getGlobalObject();
        w.abort = new AbortApi(w);
        w.abort.install(g);
        w.blobs = new BlobApi(w);
        w.blobs.install(g);
        w.params = new SearchParamsApi(w);
        w.params.install(g);
        new UrlApi(w).install(g);
        w.forms = new FormDataApi(w);
        w.forms.install(g);
        w.headers = new HeadersApi(w);
        w.headers.install(g);
        new EncodingApi(w).install(g);
        new FileReaderApi(w).install(g);
        w.streams = new ReadableStreams(w.realm);
        w.streams.install(g);
        new FetchApi(w).install(g);
        new XhrApi(w).install(g);
        new WebSocketApi(w).install(g);
        new EventSourceApi(w).install(g);
        return w;
    }

    /**
     * Tells whether the context has documents (a response document of <code>XMLHttpRequest</code> can only be built then).
     * <p>
     * 문맥에 문서가 있는지 알려 준다(<code>XMLHttpRequest</code> 의 응답 문서는 있을 때만 만들 수 있다).
     *
     * @return true for a page / 페이지이면 true
     */
    boolean hasDocuments() {
        return host.hasDocuments();
    }

    /**
     * Returns the realm.
     * <p>
     * realm 을 반환한다.
     *
     * @return the realm / realm
     */
    JsRealm realm() {
        return realm;
    }

    /**
     * Returns the DOM bridge.
     * <p>
     * DOM 연결을 반환한다.
     *
     * @return the bridge / 연결
     */
    DomBridge bridge() {
        return bridge;
    }

    /**
     * Returns the request service of the component.
     * <p>
     * 컴포넌트의 요청 서비스를 반환한다.
     *
     * @return the service or null / 서비스 또는 null
     */
    WebRequestSource source() {
        return host.getWebRequestSource();
    }

    /**
     * Returns the scheduler of the page.
     * <p>
     * 페이지의 스케줄러를 반환한다.
     *
     * @return the scheduler or null / 스케줄러 또는 null
     */
    Scheduler scheduler() {
        return host.getScheduler();
    }

    /**
     * Tells whether requests may still deliver results: the page lives and its scripts are enabled.
     * <p>
     * 요청이 결과를 아직 전달할 수 있는지 알려 준다. 페이지가 살아 있고 스크립트가 켜져 있어야 한다.
     *
     * @return true while active / 활성이면 true
     */
    boolean active() {
        return !closed && host.isActive();
    }

    /**
     * Returns the URL of the document.
     * <p>
     * 문서의 URL을 반환한다.
     *
     * @return the URL / URL
     */
    String documentUrl() {
        return host.getDocumentUrl();
    }

    /**
     * Resolves a URL against the base URL of the document and normalizes it.
     * <p>
     * URL을 문서의 기준 URL로 해석하고 정규화한다.
     *
     * @param url the URL text / URL 문자열
     * @return the absolute URL / 절대 URL
     * @throws IllegalArgumentException if it cannot be resolved / 해석할 수 없으면
     */
    String resolve(String url) {
        String base = host.getBaseUrl();
        if (base == null || base.length() == 0) {
            base = documentUrl();
        }
        return UrlUtil.resolve(base, url);
    }

    /**
     * Creates a <code>DOMException</code> as a throwable script exception.
     * <p>
     * <code>DOMException</code> 을 던질 수 있는 스크립트 예외로 만든다.
     *
     * @param name    the error name / 오류 이름
     * @param message the message / 메시지
     * @return the exception / 예외
     */
    JsException domError(String name, String message) {
        return new JsException(DomExceptionObjects.create(bridge, name, message));
    }

    /**
     * Queues a result for the page thread. Callable from any thread; the task runs as a page task after the previously queued ones, and it
     * is dropped when the page is not active any more.
     * <p>
     * 결과를 페이지 스레드에 줄 세운다. 어느 스레드에서나 부를 수 있다. 작업은 먼저 줄 선 것들 뒤에 페이지 작업으로 실행되며 페이지가 더 이상 활성이 아니면
     * 버려진다.
     *
     * @param task the work / 작업
     */
    void post(final Runnable task) {
        Scheduler s = scheduler();
        if (closed || s == null) {
            return;
        }
        inbox.add(new Runnable() {
            public void run() {
                if (active()) {
                    task.run();
                }
            }
        });
        try {
            s.post(drain, 0);
        } catch (RuntimeException e) {
            // 스케줄러가 더 받지 못하면 이 결과는 다음 전달 때까지 기다린다.
        }
    }

    /**
     * Schedules a task for the page thread after a delay; the task runs as a page task and not after the page became inactive.
     * <p>
     * 지연 뒤에 실행할 작업을 페이지 스레드에 예약한다. 작업은 페이지 작업으로 실행되며 페이지가 비활성이 된 뒤에는 실행되지 않는다.
     *
     * @param task    the work / 작업
     * @param delayMs the delay / 지연(밀리초)
     * @return the handle, or null if nothing could be scheduled / 핸들. 예약할 수 없으면 null
     */
    Scheduler.Handle postLater(final Runnable task, long delayMs) {
        Scheduler s = scheduler();
        if (closed || s == null) {
            return null;
        }
        try {
            return s.post(new Runnable() {
                public void run() {
                    host.runTask(new Runnable() {
                        public void run() {
                            if (active()) {
                                task.run();
                            }
                        }
                    });
                }
            }, delayMs);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Registers a started request so that disposal can cancel it.
     * <p>
     * 시작한 요청을 등록하여 폐기할 때 취소할 수 있게 한다.
     *
     * @param call the request / 요청
     */
    void track(WebRequestSource.Call call) {
        boolean cancelNow;
        synchronized (calls) {
            cancelNow = closed;
            if (!cancelNow) {
                calls.add(call);
            }
        }
        if (cancelNow) {
            call.cancel();
        }
    }

    /**
     * Forgets a request that ended.
     * <p>
     * 끝난 요청을 잊는다.
     *
     * @param call the request / 요청
     */
    void untrack(WebRequestSource.Call call) {
        synchronized (calls) {
            calls.remove(call);
        }
    }

    /**
     * Cancels every running request without waiting for anything (safe from any thread, also while the page lock is held by a script).
     * <p>
     * 아무것도 기다리지 않고 실행 중인 모든 요청을 취소한다(어느 스레드에서나, 스크립트가 페이지 잠금을 쥔 동안에도 안전하다).
     */
    public void cancelAll() {
        List<WebRequestSource.Call> copy;
        synchronized (calls) {
            closed = true;
            copy = new ArrayList<WebRequestSource.Call>(calls);
            calls.clear();
        }
        blobUrls.close();
        // 복사본의 요청을 하나씩 취소한다. 취소는 기다리지 않고 요청 스레드가 알아서 끝난다.
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).cancel();
            } catch (RuntimeException e) {
                // 한 요청의 취소 실패가 다른 요청의 취소를 막지 않는다.
            }
        }
        inbox.clear();
    }

    /**
     * Cancels everything and forgets all queued results; the page is gone.
     * <p>
     * 모두 취소하고 줄 선 결과를 잊는다. 페이지가 사라졌다.
     */
    public void dispose() {
        cancelAll();
    }
}
