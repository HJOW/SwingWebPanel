package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Checks Atomics suspension policy and wakeup of idle browser event loops.
 * <p>
 * Atomics의 차단 허용 규칙과 유휴 브라우저 이벤트 루프의 작업 실행을 확인한다.
 */
public class WorkerAtomicsHostTest {

    /**
     * A page rejects synchronous waits and delivers a timed Promise without another script or user event.
     * <p>
     * 페이지가 동기 대기를 거부하고, 다른 스크립트 실행이나 사용자 이벤트 없이 시간 초과 Promise를 해결한다.
     *
     * @throws Exception on test infrastructure failure / 시험 환경에 실패하면
     */
    public void testPageWaitPolicyAndIdleTimeout() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "window.onload=function(){const a=new Int32Array(new SharedArrayBuffer(4));"
                    + "try{Atomics.wait(a,0,0,0);put('policy','missing');}catch(e){put('policy',e.name);}"
                    + "Atomics.waitAsync(a,0,0,20).value.then(v=>put('pageTimeout',v));};")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("TypeError", WorkerTestSupport.await(panel, "policy"), "document agent cannot block");
            Check.equal("timed-out", WorkerTestSupport.await(panel, "pageTimeout"), "idle document processed the external timeout job");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * A worker may suspend and also delivers an asynchronous timeout when no message wakes its task queue.
     * <p>
     * Worker는 동기 대기를 할 수 있고, 다른 메시지가 작업 큐를 깨우지 않아도 비동기 시간 초과를 전달한다.
     *
     * @throws Exception on test infrastructure failure / 시험 환경에 실패하면
     */
    public void testWorkerMaySuspendAndDeliverIdleTimeout() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/worker.js", WorkerTestSupport.js(
                    "const a=new Int32Array(new SharedArrayBuffer(4));"
                    + "postMessage(['policy',Atomics.wait(a,0,0,5)]);"
                    + "Atomics.waitAsync(a,0,0,20).value.then(v=>postMessage(['workerTimeout',v]));"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "const worker=new Worker('/worker.js');"
                    + "worker.onmessage=e=>put(e.data[0],e.data[1]);"
                    + "worker.onerror=e=>put('policy',e.message);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("timed-out", WorkerTestSupport.await(panel, "policy"), "worker agent can suspend");
            Check.equal("timed-out", WorkerTestSupport.await(panel, "workerTimeout"), "idle worker processed the external timeout job");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
