package org.duckdns.hjow.swingwebpanel.js.worker;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The worker API of one page (plan item P20-09-b; HTML Standard "Web workers", checked on 2026-11-17): it installs <code>Worker</code>
 * (<code>new Worker(url [, {name, type}])</code>, <code>postMessage</code>, <code>terminate</code>, <code>onmessage</code>,
 * <code>onmessageerror</code>, <code>onerror</code>) and <code>BroadcastChannel</code> into the page, keeps the list of the live workers of
 * the page, and terminates all of them when the page ends. A dedicated worker runs the script of a same-origin URL (a <code>SecurityError</code>
 * otherwise) in a realm and on a thread of its own, see {@link WorkerScope}. At most {@link WorkerLimits#MAX_WORKERS_PER_PAGE} workers can live
 * at the same time. <code>SharedWorker</code> (<code>new SharedWorker(url [, name | options])</code>, <code>port</code>) connects the page to the
 * shared worker of the component that has the same origin, script URL and name, which it starts if it does not exist; the shared worker lives as
 * long as one page owns it, see {@link SharedEntry} and {@link WorkerRegistry}. Module workers (<code>type: "module"</code> fires an
 * <code>error</code> event), workers inside workers, and <code>blob:</code> or <code>data:</code> worker scripts are not supported.
 * <p>
 * Page life cycle: {@link #terminateAll()} (called when the page is asked to stop: navigation, <code>clean()</code>, <code>dispose()</code>,
 * JavaScript switched off) terminates every worker at once without waiting; {@link #dispose()} forgets them afterwards.
 * <p>
 * 한 페이지의 worker API(계획 P20-09-b. HTML 표준 "Web workers", 2026-11-17 확인): 페이지에 <code>Worker</code>(<code>new Worker(url [, {name, type}])</code>,
 * <code>postMessage</code>, <code>terminate</code>, <code>onmessage</code>, <code>onmessageerror</code>, <code>onerror</code>)와 <code>BroadcastChannel</code> 을
 * 설치하고, 페이지의 살아 있는 worker 목록을 관리하며, 페이지가 끝나면 모두 종료한다. 전용 worker 는 같은 오리진 URL의 스크립트를(아니면
 * <code>SecurityError</code>) 자기 realm 과 스레드에서 실행한다({@link WorkerScope} 참고). 동시에 살아 있는 worker 는 최대
 * {@link WorkerLimits#MAX_WORKERS_PER_PAGE} 개이다. <code>SharedWorker</code>(<code>new SharedWorker(url [, 이름 | 옵션])</code>, <code>port</code>)는 페이지를
 * 컴포넌트에서 오리진, 스크립트 URL, 이름이 같은 공유 worker 에 연결하며(없으면 시작한다), 공유 worker 는 페이지 하나라도 소유하는 동안 산다({@link SharedEntry},
 * {@link WorkerRegistry} 참고). 모듈 worker(<code>type: "module"</code> 은 <code>error</code> 이벤트가 발생함), worker 안의 worker,
 * <code>blob:</code>·<code>data:</code> worker 스크립트는 지원하지 않는다.
 * <p>
 * 페이지 수명: {@link #terminateAll()}(페이지에 중지를 요청할 때 호출: 이동, <code>clean()</code>, <code>dispose()</code>, JavaScript 끄기)는 기다리지 않고
 * 모든 worker 를 즉시 종료하고, {@link #dispose()} 는 그 뒤에 이들을 잊는다.
 */
public final class WorkerApi {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The page as a script context.
     * <p>
     * 스크립트 문맥으로서의 페이지.
     */
    private final ScriptContext context;

    /**
     * The hub of the broadcast channels of the origin of the page: the channels of the pages of the component with this origin and of their
     * workers hear each other.
     * <p>
     * 페이지 오리진의 브로드캐스트 채널 허브: 이 오리진을 가진 컴포넌트의 페이지와 그 worker 의 채널은 서로 듣는다.
     */
    private final BroadcastHub hub;

    /**
     * What the pages of the component share (the hubs and the shared workers).
     * <p>
     * 컴포넌트의 페이지들이 공유하는 것(허브와 공유 worker).
     */
    private final WorkerRegistry registry;

    /**
     * The workers that were created and not terminated by the page; guarded by itself.
     * <p>
     * 만들어졌고 페이지가 종료하지 않은 worker. 자기 자신으로 보호한다.
     */
    private final List<WorkerHandle> live = new ArrayList<WorkerHandle>();

    /**
     * True once the page ended (all workers were terminated); guarded by {@link #live}.
     * <p>
     * 페이지가 끝났으면(모든 worker 를 종료했으면) true. {@link #live} 로 보호한다.
     */
    private boolean closed;

    /**
     * Creates the worker API of a page; call {@link #install()} once.
     * <p>
     * 페이지의 worker API를 만든다. {@link #install()} 을 한 번 부른다.
     *
     * @param page the page, not null / 페이지(null 불가)
     */
    public WorkerApi(PageScripts page) {
        this.page = page;
        this.context = new PageContext(page);
        this.registry = WorkerRegistry.of(page.getEnvironment());
        String url = page.getDocument().getUrl();
        this.hub = registry.hubFor(Origin.of(url == null ? "about:blank" : url));
    }

    /**
     * Installs <code>Worker</code> and <code>BroadcastChannel</code> into the realm of the page (the event interfaces must already be in the
     * DOM bridge). Call it on the thread that creates the page.
     * <p>
     * 페이지의 realm 에 <code>Worker</code> 와 <code>BroadcastChannel</code> 을 설치한다(이벤트 인터페이스가 이미 DOM 브리지에 있어야 한다). 페이지를
     * 만드는 스레드에서 부른다.
     */
    public void install() {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a worker handle.
             * <p>
             * worker 핸들의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof SharedHandle) {
                    return "SharedWorker";
                }
                return o instanceof WorkerHandle ? "Worker" : null;
            }
        });
        final DomBridge.InterfaceInfo info = bridge.defineInterface("Worker", "EventTarget", WorkerHandle.class, new String[] { "m:terminate()" });
        bridge.getEventHandlers().define(info.getPrototype(), WorkerHandle.class, new String[] { "message", "messageerror", "error" });
        info.getPrototype().defineData("postMessage", realm.newFunction("postMessage", 1, new NativeFunction.Call() {
            /**
             * Posts a message to the worker.
             * <p>
             * worker 에 메시지를 보낸다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the message and the optional transfer list / 인수: 메시지와 선택적 전달 목록
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                WorkerHandle h = (WorkerHandle) DomBridge.targetOf(self, WorkerHandle.class);
                if (h == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                ScriptUtil.requireArgs(args, 1, "execute 'postMessage' on 'Worker'");
                h.postToWorker(Messages.serialize(context, args[0], Js.arg(args, 1)));
                return Js.UNDEFINED;
            }
        }), true, true, true);
        bridge.setConstructor("Worker", new NativeFunction(realm, realm.getFunctionPrototype(), "Worker", 1, ScriptUtil.refuse("Worker"),
                new NativeFunction.Construct() {
            /**
             * Creates a worker and starts it.
             * <p>
             * worker 를 만들고 시작한다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the script URL and optional options / 인수: 스크립트 URL과 선택적 옵션
             * @param newTarget the new target / new 대상
             * @return the worker wrapper / worker 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                WorkerHandle h = create(args);
                HostObject w = bridge.wrap(h);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
        installShared(bridge, realm);
        BroadcastApi.install(context, hub);
    }

    /**
     * Installs <code>SharedWorker</code> (<code>new SharedWorker(url [, name | {name, type, credentials, extendedLifetime}])</code>,
     * <code>port</code>, <code>onerror</code>).
     * <p>
     * <code>SharedWorker</code>(<code>new SharedWorker(url [, name | {name, type, credentials, extendedLifetime}])</code>, <code>port</code>,
     * <code>onerror</code>)를 설치한다.
     *
     * @param bridge the DOM bridge of the page / 페이지의 DOM 브리지
     * @param realm  the realm of the page / 페이지의 realm
     */
    private void installShared(final DomBridge bridge, final JsRealm realm) {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("SharedWorker", "EventTarget", SharedHandle.class, new String[] { "a:port" });
        bridge.getEventHandlers().define(info.getPrototype(), SharedHandle.class, new String[] { "error" });
        bridge.setConstructor("SharedWorker", new NativeFunction(realm, realm.getFunctionPrototype(), "SharedWorker", 1, ScriptUtil.refuse("SharedWorker"),
                new NativeFunction.Construct() {
            /**
             * Creates the connection to a shared worker (and the shared worker if it does not exist yet).
             * <p>
             * 공유 worker 로의 연결을 만든다(공유 worker 가 아직 없으면 그것도 만든다).
             *
             * @param r         the realm / realm
             * @param args      arguments: the script URL and the name or options / 인수: 스크립트 URL과 이름 또는 옵션
             * @param newTarget the new target / new 대상
             * @return the wrapper of the <code>SharedWorker</code> object / <code>SharedWorker</code> 객체의 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(createShared(args));
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * The steps of <code>new SharedWorker(url, options)</code>: validates the arguments, resolves the URL against the base URL of the
     * document, refuses another origin, finds the shared worker with the same origin, script URL and name (the arguments that do not fit it
     * are an <code>error</code> event) or starts a new one, makes the channel of the connection and tells the shared worker about it with a
     * <code>connect</code> event.
     * <p>
     * <code>new SharedWorker(url, options)</code> 의 단계: 인수를 검사하고, 문서의 기준 URL로 URL을 해석하고, 다른 오리진을 거부하고, 오리진, 스크립트 URL,
     * 이름이 같은 공유 worker 를 찾거나(맞지 않는 인수는 <code>error</code> 이벤트) 새로 시작하고, 연결의 채널을 만들고, 공유 worker 에 <code>connect</code>
     * 이벤트로 알린다.
     *
     * @param args the script arguments / 스크립트 인수
     * @return the object of the connection / 연결의 객체
     */
    private SharedHandle createShared(JsValue[] args) {
        final DomBridge bridge = page.getBridge();
        ScriptUtil.requireArgs(args, 1, "construct 'SharedWorker'");
        String text = Ops.toString(args[0]);
        JsValue options = Js.arg(args, 1);
        String name = "";
        String type = "classic";
        String credentials = "same-origin";
        boolean extended = false;
        // 둘째 인수는 이름(문자열)이거나 옵션 사전이다.
        if (options.isObject()) {
            name = ScriptUtil.text(options, "name", "");
            type = ScriptUtil.text(options, "type", "classic");
            credentials = ScriptUtil.text(options, "credentials", "same-origin");
            extended = Ops.toBoolean(ScriptUtil.member(options, "extendedLifetime"));
        } else if (!options.isUndefined()) {
            name = Ops.toString(options);
        }
        if (!"classic".equals(type) && !"module".equals(type)) {
            throw JsException.typeError("Failed to construct 'SharedWorker': The provided value '" + type + "' is not a valid enum value of type WorkerType.");
        }
        String absolute;
        try {
            absolute = UrlUtil.stripFragment(UrlUtil.resolve(page.getDocument().getBaseUri(), text));
        } catch (RuntimeException e) {
            throw bridge.domError(DomException.syntax("Failed to construct 'SharedWorker': Script at '" + text + "' cannot be resolved."));
        }
        Origin mine = Origin.of(page.getDocument().getUrl() == null ? "about:blank" : page.getDocument().getUrl());
        Origin theirs = Origin.of(absolute);
        // 스크립트는 페이지와 같은 오리진에서만 가져올 수 있다(불투명 오리진 페이지와 data:, blob: 주소는 허용하지 않는다).
        if (mine.isOpaque() || theirs.isOpaque() || !mine.isSameOrigin(theirs)) {
            throw bridge.domError(DomException.security("Failed to construct 'SharedWorker': Script at '" + absolute + "' cannot be accessed from origin '"
                    + (mine.isOpaque() ? "null" : mine.serialize()) + "'."));
        }
        synchronized (live) {
            if (closed) {
                throw bridge.domError(DomException.invalidState("Failed to construct 'SharedWorker': the page is closed."));
            }
        }
        PortCore[] ends = PortCore.pair();
        PortTarget outside = new PortTarget(context);
        outside.bind(ends[0]);
        SharedHandle handle = new SharedHandle(this, page, context, outside);
        if ("module".equals(type)) {
            // 모듈 공유 worker 는 지원하지 않는다: 연결하지 않고 error 이벤트로 알린다.
            handle.loadFailed("Failed to construct 'SharedWorker': module workers are not supported");
            return handle;
        }
        String originText = mine.serialize();
        SharedEntry entry = registry.findShared(originText, absolute, name);
        if (entry != null && (!"same-origin".equals(credentials) || extended)) {
            // 이미 있는 공유 worker 와 type, credentials, extendedLifetime 이 맞지 않으면 연결하지 않고 error 이벤트로 알린다.
            handle.loadFailed("Failed to construct 'SharedWorker': the options do not fit the shared worker that already exists");
            return handle;
        }
        if (entry == null) {
            entry = new SharedEntry(originText, absolute, name);
            WorkerScope scope = new WorkerScope(page.getEnvironment(), entry, hub, absolute, name, true);
            entry.attach(scope);
            if (!registry.addShared(entry)) {
                throw bridge.domError(new DomException("QuotaExceededError", DomException.QUOTA_EXCEEDED_ERR,
                        "Failed to construct 'SharedWorker': the component already has " + WorkerRegistry.MAX_SHARED_WORKERS + " shared workers."));
            }
            entry.addConnection(this, handle);
            scope.start();
        } else {
            entry.addConnection(this, handle);
        }
        entry.getScope().connect(ends[1]);
        return handle;
    }

    /**
     * The steps of <code>new Worker(url, options)</code>: validates the arguments, resolves the URL against the base URL of the document,
     * refuses another origin, enforces the limit of live workers, and starts the worker.
     * <p>
     * <code>new Worker(url, options)</code> 의 단계: 인수를 검사하고, 문서의 기준 URL로 URL을 해석하고, 다른 오리진을 거부하고, 살아 있는 worker 수의
     * 한도를 지키고, worker 를 시작한다.
     *
     * @param args the script arguments / 스크립트 인수
     * @return the handle / 핸들
     */
    private WorkerHandle create(JsValue[] args) {
        final DomBridge bridge = page.getBridge();
        ScriptUtil.requireArgs(args, 1, "construct 'Worker'");
        String text = Ops.toString(args[0]);
        JsValue options = Js.arg(args, 1);
        String type = ScriptUtil.text(options, "type", "classic");
        if (!"classic".equals(type) && !"module".equals(type)) {
            throw JsException.typeError("Failed to construct 'Worker': The provided value '" + type + "' is not a valid enum value of type WorkerType.");
        }
        String name = ScriptUtil.text(options, "name", "");
        String absolute;
        try {
            absolute = UrlUtil.stripFragment(UrlUtil.resolve(page.getDocument().getBaseUri(), text));
        } catch (RuntimeException e) {
            throw bridge.domError(DomException.syntax("Failed to construct 'Worker': Script at '" + text + "' cannot be resolved."));
        }
        Origin mine = Origin.of(page.getDocument().getUrl() == null ? "about:blank" : page.getDocument().getUrl());
        Origin theirs = Origin.of(absolute);
        // 스크립트는 페이지와 같은 오리진에서만 가져올 수 있다(불투명 오리진 페이지와 data:, blob: 주소는 허용하지 않는다).
        if (mine.isOpaque() || theirs.isOpaque() || !mine.isSameOrigin(theirs)) {
            throw bridge.domError(DomException.security("Failed to construct 'Worker': Script at '" + absolute + "' cannot be accessed from origin '"
                    + (mine.isOpaque() ? "null" : mine.serialize()) + "'."));
        }
        WorkerHandle handle = new WorkerHandle(this, page, context);
        WorkerScope scope = new WorkerScope(page.getEnvironment(), handle, hub, absolute, name, false);
        handle.attach(scope);
        synchronized (live) {
            if (closed) {
                throw bridge.domError(DomException.invalidState("Failed to construct 'Worker': the page is closed."));
            }
            // 이미 끝난 worker 를 목록에서 정리한 뒤 한도를 확인한다.
            for (int i = live.size() - 1; i >= 0; i--) {
                if (!live.get(i).isAlive()) {
                    live.remove(i);
                }
            }
            if (live.size() >= WorkerLimits.MAX_WORKERS_PER_PAGE) {
                throw bridge.domError(new DomException("QuotaExceededError", DomException.QUOTA_EXCEEDED_ERR,
                        "Failed to construct 'Worker': the page already has " + WorkerLimits.MAX_WORKERS_PER_PAGE + " workers."));
            }
            if (!"module".equals(type)) {
                live.add(handle);
            }
        }
        if ("module".equals(type)) {
            // 모듈 worker 는 지원하지 않는다: 시작하지 않고 error 이벤트로 알린다.
            scope.terminate();
            handle.loadFailed("Failed to construct 'Worker': module workers are not supported");
        } else {
            scope.start();
        }
        return handle;
    }

    /**
     * Forgets a worker that the page terminated.
     * <p>
     * 페이지가 종료한 worker 를 잊는다.
     *
     * @param handle the worker handle / worker 핸들
     */
    void forget(WorkerHandle handle) {
        synchronized (live) {
            live.remove(handle);
        }
    }

    /**
     * Returns the number of workers of the page that are still alive.
     * <p>
     * 페이지의 worker 중 아직 살아 있는 것의 수를 반환한다.
     *
     * @return the count / 개수
     */
    public int liveCount() {
        int n = 0;
        synchronized (live) {
            // 목록에서 아직 살아 있는 worker 를 센다.
            for (int i = 0; i < live.size(); i++) {
                if (live.get(i).isAlive()) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Terminates every worker of the page at once, without waiting for anything, from any thread (also from the EDT while a script runs): the
     * scripts of the workers are cancelled, their requests are cancelled and their threads end. No worker can be created afterwards and the
     * broadcast channels are dropped.
     * <p>
     * 페이지의 모든 worker 를 아무것도 기다리지 않고 어느 스레드에서나(스크립트가 실행되는 동안의 EDT 에서도) 즉시 종료한다: worker 의 스크립트와 요청이
     * 취소되고 스레드가 끝난다. 그 뒤에는 worker 를 만들 수 없고 브로드캐스트 채널은 버려진다.
     */
    public void terminateAll() {
        List<WorkerHandle> copy;
        synchronized (live) {
            closed = true;
            copy = new ArrayList<WorkerHandle>(live);
            live.clear();
        }
        // 목록의 worker 를 하나씩 종료한다. 한 worker 의 실패가 다른 worker 의 종료를 막지 않는다.
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).terminate();
            } catch (RuntimeException e) {
                continue;
            }
        }
        // 이 페이지의 채널을 허브에서 빼고, 이 페이지가 소유한 공유 worker 에서 빠진다(마지막 소유자이면 공유 worker 가 종료된다).
        hub.removeContext(context);
        registry.release(this);
    }

    /**
     * Ends the worker API of the page: everything is terminated (see {@link #terminateAll()}) and forgotten.
     * <p>
     * 페이지의 worker API를 끝낸다: 모든 것을 종료({@link #terminateAll()} 참고)하고 잊는다.
     */
    public void dispose() {
        terminateAll();
    }
}
