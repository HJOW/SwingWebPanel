package org.duckdns.hjow.swingwebpanel.js.worker;

import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Exercises shared browser memory across workers, ports and broadcast channels.
 * <p>
 * Worker, 포트 및 브로드캐스트 채널을 통해 브라우저 공유 메모리를 시험한다.
 */
public class WorkerSharedMemoryTest {

    /**
     * Tests a worker's shared writes, growth, notification and round trips on the message transports.
     * <p>
     * Worker의 공유 쓰기, 성장, 통지와 여러 메시지 경로의 왕복을 확인한다.
     *
     * @throws Exception on test infrastructure failure / 시험 환경에 실패하면
     */
    public void testSharedRoundTripAndAtomics() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        try {
            server.routeFixed("/shared.js", WorkerTestSupport.js(
                    "onmessage=e=>{const local=structuredClone(e.data.b); const a=new Int32Array(local);"
                    + "const old=Atomics.add(a,1,1); local.grow(12);"
                    + "e.data.p.postMessage({buffer:local,tracking:a,old});"
                    + "const bc=new BroadcastChannel('sab'); bc.postMessage(local); bc.close();"
                    + "postMessage([local instanceof SharedArrayBuffer,Atomics.notify(a,0)].join('|'));"
                    + "postMessage(Atomics.wait(a,2,0,3000));};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "const buffer=new SharedArrayBuffer(8,{maxByteLength:16}), a=new Int32Array(buffer);"
                    + "a[1]=41; Atomics.waitAsync(a,0,0).value.then(v=>put('mainWait',v));"
                    + "const ab=new ArrayBuffer(4);"
                    + "try{structuredClone(buffer,{transfer:[ab,buffer]});}catch(e){put('transfer',e.name+'|'+ab.byteLength);}"
                    + "try{history.pushState({buffer},'');}catch(e){put('history',e.name);}"
                    + "const bc=new BroadcastChannel('sab');"
                    + "bc.onmessage=e=>{put('broadcast',[e.data!==buffer,e.data.byteLength,new Int32Array(e.data)[1]].join('|'));bc.close();};"
                    + "const ch=new MessageChannel();"
                    + "ch.port1.onmessage=e=>{const x=e.data;put('viaPort',[x.old,x.buffer!==buffer,x.tracking.buffer===x.buffer,"
                    + "x.tracking.length,a.length,a[1]].join('|'));};"
                    + "const w=new Worker('/shared.js');"
                    + "function wake(i){ if(!Atomics.notify(a,2,1) && i<1500)setTimeout(()=>wake(i+1),1); }"
                    + "w.onmessage=e=>{if(e.data==='true|1'){put('wokePage',e.data);wake(0);}else put('workerWait',e.data);};"
                    + "w.onerror=e=>put('workerWait',e.message);"
                    + "w.postMessage({b:buffer,p:ch.port2},[ch.port2]);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("DataCloneError|4", WorkerTestSupport.await(panel, "transfer"), "failed transfer did not detach the ordinary buffer");
            Check.equal("DataCloneError", WorkerTestSupport.await(panel, "history"), "history uses the storage serialization boundary");
            Check.equal("41|true|true|3|3|42", WorkerTestSupport.await(panel, "viaPort"), "the port preserves shared data and view metadata");
            Check.equal("true|12|42", WorkerTestSupport.await(panel, "broadcast"), "the broadcast delivers a new wrapper for the shared bytes");
            Check.equal("true|1", WorkerTestSupport.await(panel, "wokePage"), "worker notification selected the waiting page");
            Check.equal("ok", WorkerTestSupport.await(panel, "mainWait"), "worker notification wakes the idle page");
            Check.equal("ok", WorkerTestSupport.await(panel, "workerWait"), "the document wakes the suspended worker");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Termination drops a worker's pending waits while the page keeps the shared buffer.
     * <p>
     * 페이지가 공유 버퍼를 유지하고 있어도 Worker를 종료하면 대기 항목을 정리한다.
     *
     * @throws Exception on test infrastructure failure / 시험 환경에 실패하면
     */
    public void testTerminateReleasesSharedWaiters() throws Exception {
        TestHttpServer server = new TestHttpServer();
        SwingWebPanel panel = WorkerTestSupport.newPanel();
        int before = WorkerTestSupport.workerThreads();
        try {
            server.routeFixed("/block.js", WorkerTestSupport.js(
                    "onmessage=e=>{const a=new Int32Array(e.data);"
                    + "Atomics.waitAsync(a,0,0).value.then(v=>postMessage(v));"
                    + "postMessage('waiting'); Atomics.wait(a,1,0);};"));
            server.routeFixed("/page", TestHttpServer.Response.html(WorkerTestSupport.page(
                    "const a=new Int32Array(new SharedArrayBuffer(8)), w=new Worker('/block.js');"
                    + "w.onmessage=e=>{if(e.data==='waiting'){w.terminate();"
                    + "put('remaining',[Atomics.notify(a,0),Atomics.notify(a,1)].join());}};"
                    + "w.postMessage(a.buffer);")));
            server.start();
            panel.setPage(server.url("/page"));
            Check.equal("0,0", WorkerTestSupport.await(panel, "remaining"), "terminated worker left no waiter at either byte position");
            Check.isTrue(WorkerTestSupport.awaitThreads("swingwebpanel-worker-", before, 5000) <= before, "blocked worker thread ended");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
