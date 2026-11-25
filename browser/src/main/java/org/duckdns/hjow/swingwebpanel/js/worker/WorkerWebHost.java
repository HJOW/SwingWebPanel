package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;
import org.duckdns.hjow.swingwebpanel.js.host.StreamingWebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequest;
import org.duckdns.hjow.swingwebpanel.js.host.WebRequestSource;
import org.duckdns.hjow.swingwebpanel.js.host.web.WebApis;

/**
 * The {@link WebApis.Host} of a worker: the request APIs (<code>fetch</code>, <code>XMLHttpRequest</code> ...) of the worker go through the very
 * same request service of the component as those of the page, so the user agent, the additional headers, the cookies (by credentials mode),
 * the SSL hook, the scheme limits, the cache, the same-origin and CORS rules and the cancellation apply to them too. The origin of a worker is
 * the origin of its script, which is the origin of the page; requests that name no referrer of their own use the URL of the worker script as
 * the referrer. Results are delivered on the thread of the worker, as tasks of the worker.
 * <p>
 * worker 의 {@link WebApis.Host}: worker 의 요청 API(<code>fetch</code>, <code>XMLHttpRequest</code> 등)는 페이지와 똑같은 컴포넌트의 요청 서비스를
 * 거치므로 User-Agent, 추가 헤더, 쿠키(자격 증명 모드에 따라), SSL 훅, 스킴 제한, 캐시, 동일 출처와 CORS 규칙, 취소가 그대로 적용된다. worker 의
 * 오리진은 스크립트의 오리진이며 곧 페이지의 오리진이다. 자기 참조를 지정하지 않은 요청은 worker 스크립트의 URL을 참조로 쓴다. 결과는 worker 의 스레드에서
 * worker 의 작업으로 전달된다.
 */
final class WorkerWebHost implements WebApis.Host {

    /**
     * The worker.
     * <p>
     * 이 호스트가 속한 worker.
     */
    private final WorkerScope scope;

    /**
     * The request service of the component, wrapped to set the referrer.
     * <p>
     * 컴포넌트의 요청 서비스. 참조를 지정하도록 감쌌다.
     */
    private final WebRequestSource source;

    /**
     * The scheduler that runs raw tasks on the worker thread.
     * <p>
     * worker 스레드에서 원시 작업을 실행하는 스케줄러.
     */
    private final Scheduler loop;

    /**
     * Creates the host of a worker.
     * <p>
     * worker 의 호스트를 만든다.
     *
     * @param scope the worker / worker
     * @param env   the environment of the page / 페이지의 환경
     * @param loop  the scheduler of the worker thread / worker 스레드의 스케줄러
     */
    WorkerWebHost(WorkerScope scope, HostEnvironment env, Scheduler loop) {
        this.scope = scope;
        this.loop = loop;
        final WebRequestSource delegate = env.getWebRequestSource();
        // 페이지의 서비스가 스트리밍도 하면 worker 의 EventSource 도 같은 서비스를 거치도록 스트리밍 짝도 감싼다.
        if (delegate == null) {
            this.source = null;
        } else if (delegate instanceof StreamingWebRequestSource) {
            this.source = new StreamingWrapper(delegate);
        } else {
            this.source = new Wrapper(delegate);
        }
    }

    /**
     * Sets the worker's response policy and supplies the script URL when no explicit referrer was given.
     * <p>
     * 워커의 응답 정책을 설정하고 참조를 명시하지 않은 요청에는 워커 스크립트의 URL을 사용한다.
     *
     * @param request the request / 요청
     */
    private void applyClientSettings(WebRequest request) {
        request.setEmbedderPolicy(scope.getEmbedderPolicy());
        if (WebRequest.REFERRER_CLIENT.equals(request.getReferrer())) {
            request.setReferrer(scope.getScriptUrl());
        }
    }

    /**
     * The request service of the worker: the service of the component, with the referrer of the worker script.
     * <p>
     * worker 의 요청 서비스: worker 스크립트의 참조를 붙인 컴포넌트의 서비스.
     */
    private class Wrapper implements WebRequestSource {
        /**
         * The service of the component.
         * <p>
         * 컴포넌트의 서비스.
         */
        final WebRequestSource delegate;

        /**
         * Creates the wrapper.
         * <p>
         * 감싸개를 만든다.
         *
         * @param delegate the service of the component / 컴포넌트의 서비스
         */
        Wrapper(WebRequestSource delegate) {
            this.delegate = delegate;
        }

        /**
         * Starts a request of the worker.
         * <p>
         * worker 의 요청을 시작한다.
         *
         * @param request  the request / 요청
         * @param callback the receiver / 받는 쪽
         * @return the handle / 핸들
         */
        public Call start(WebRequest request, Callback callback) {
            applyClientSettings(request);
            return delegate.start(request, callback);
        }
    }

    /**
     * The same, for a service that also streams.
     * <p>
     * 스트리밍도 하는 서비스를 위한 같은 감싸개.
     */
    private final class StreamingWrapper extends Wrapper implements StreamingWebRequestSource {
        /**
         * Creates the wrapper.
         * <p>
         * 감싸개를 만든다.
         *
         * @param delegate the service of the component, which also streams / 스트리밍도 하는 컴포넌트의 서비스
         */
        StreamingWrapper(WebRequestSource delegate) {
            super(delegate);
        }

        /**
         * Starts a stream of the worker.
         * <p>
         * worker 의 스트림을 시작한다.
         *
         * @param request  the request / 요청
         * @param listener the receiver / 받는 쪽
         * @return the handle / 핸들
         */
        public Call startStream(WebRequest request, StreamingWebRequestSource.Listener listener) {
            applyClientSettings(request);
            return ((StreamingWebRequestSource) delegate).startStream(request, listener);
        }
    }

    /**
     * Returns the realm of the worker.
     * <p>
     * worker 의 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return scope.getRealm();
    }

    /**
     * Returns the DOM bridge of the worker.
     * <p>
     * worker 의 DOM 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    public DomBridge getBridge() {
        return scope.getBridge();
    }

    /**
     * Returns the request service of the component.
     * <p>
     * 컴포넌트의 요청 서비스를 반환한다.
     *
     * @return the service or null / 서비스 또는 null
     */
    public WebRequestSource getWebRequestSource() {
        return source;
    }

    /**
     * Returns the scheduler of the worker thread.
     * <p>
     * worker 스레드의 스케줄러를 반환한다.
     *
     * @return the scheduler / 스케줄러
     */
    public Scheduler getScheduler() {
        return loop;
    }

    /**
     * Tells whether the worker still runs scripts and the page allows them.
     * <p>
     * worker 가 아직 스크립트를 실행하고 페이지가 이를 허용하는지 알려 준다.
     *
     * @return true while active / 활성이면 true
     */
    public boolean isActive() {
        return scope.isActive() && scope.getEnvironment().isScriptingEnabled();
    }

    /**
     * Runs a task of the worker.
     * <p>
     * worker 의 작업을 실행한다.
     *
     * @param task the work / 작업
     */
    public void runTask(Runnable task) {
        scope.runTask(task);
    }

    /**
     * Returns the URL of the worker script.
     * <p>
     * worker 스크립트의 URL을 반환한다.
     *
     * @return the URL / URL
     */
    public String getDocumentUrl() {
        return scope.getScriptUrl();
    }

    /**
     * Returns the URL that relative request URLs resolve against: the URL of the worker script.
     * <p>
     * 상대 요청 URL의 기준이 되는 URL을 반환한다. worker 스크립트의 URL.
     *
     * @return the URL / URL
     */
    public String getBaseUrl() {
        return scope.getScriptUrl();
    }

    /**
     * A worker has no documents.
     * <p>
     * worker 에는 문서가 없다.
     *
     * @return false / false
     */
    public boolean hasDocuments() {
        return false;
    }
}
