package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.ListenerErrorHandler;

/**
 * The Java side of <code>self</code> inside a worker (<code>DedicatedWorkerGlobalScope</code> or <code>SharedWorkerGlobalScope</code>): the
 * event target at which the <code>message</code>, <code>connect</code> and <code>error</code> events of the worker are dispatched. In scripts
 * the global object stands for it (the bridge maps this Java object to the global object), like <code>window</code> in a page.
 * <p>
 * worker 안의 <code>self</code>(<code>DedicatedWorkerGlobalScope</code> 또는 <code>SharedWorkerGlobalScope</code>)의 Java 쪽: worker 의 <code>message</code>,
 * <code>connect</code>, <code>error</code> 이벤트가 전달되는 이벤트 대상. 스크립트에서는 전역 객체가 이것을 나타낸다(브리지가 이 Java 객체를 전역 객체에 대응시킨다).
 * 페이지의 <code>window</code> 와 같다.
 */
final class WorkerGlobal extends EventTarget {

    /**
     * The worker that owns this target.
     * <p>
     * 이 대상을 소유한 worker.
     */
    private final ScriptContext context;

    /**
     * Creates the target.
     * <p>
     * 대상을 만든다.
     *
     * @param context the worker that owns the target / 대상을 소유한 worker
     */
    WorkerGlobal(ScriptContext context) {
        super();
        this.context = context;
    }

    /**
     * The failures of the listeners of the global scope go to the worker.
     * <p>
     * 전역 범위의 리스너 실패는 worker 로 간다.
     *
     * @return the handler of the worker / worker 의 핸들러
     */
    protected ListenerErrorHandler getListenerErrorHandler() {
        return context.getListenerErrors();
    }
}
