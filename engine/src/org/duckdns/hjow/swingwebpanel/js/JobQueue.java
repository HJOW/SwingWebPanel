package org.duckdns.hjow.swingwebpanel.js;

/**
 * The queue that receives jobs (microtasks) created by the engine, for example Promise reaction jobs (ECMA-262 6th edition 8.4).
 * The engine only <em>enqueues</em> jobs; the host decides when they run. A browser host drains the queue after each task of its
 * event loop; tests can use {@link SimpleJobQueue}. Jobs must run on the thread that owns the realm.
 * <p>
 * 엔진이 만드는 작업(마이크로태스크)을 받는 큐. 예: Promise 반응 작업(ECMA-262 6판 8.4). 엔진은 작업을 <em>넣기만</em> 하고
 * 실행 시점은 호스트가 정한다. 브라우저 호스트는 이벤트 루프의 작업이 끝날 때마다 큐를 비우며, 시험에서는 {@link SimpleJobQueue} 를 쓸 수 있다.
 * 작업은 realm 을 소유한 스레드에서 실행해야 한다.
 */
public interface JobQueue {

    /**
     * Adds a job to the end of the queue.
     * <p>
     * 큐의 끝에 작업을 추가한다.
     *
     * @param job the job / 작업
     */
    void enqueue(Runnable job);
}
