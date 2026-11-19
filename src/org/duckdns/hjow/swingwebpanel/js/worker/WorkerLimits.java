package org.duckdns.hjow.swingwebpanel.js.worker;

/**
 * The limits of workers and broadcast channels. A page that spawns workers in a loop gets an error instead of exhausting the threads of the
 * process, and a sender that outruns a receiver cannot grow a queue without bound.
 * <p>
 * worker 와 브로드캐스트 채널의 한도. 반복문으로 worker 를 만드는 페이지는 프로세스의 스레드를 고갈시키는 대신 오류를 받고, 받는 쪽보다 빠른 보내는
 * 쪽은 큐를 끝없이 키울 수 없다.
 */
public final class WorkerLimits {

    /**
     * The largest number of live workers of one page; the next <code>new Worker()</code> throws a <code>QuotaExceededError</code>. A worker
     * that was terminated or that closed itself does not count.
     * <p>
     * 페이지 하나의 살아 있는 worker 의 최대 수. 그다음 <code>new Worker()</code> 는 <code>QuotaExceededError</code> 를 던진다. 종료했거나 스스로
     * 닫은 worker 는 세지 않는다.
     */
    public static final int MAX_WORKERS_PER_PAGE = 16;

    /**
     * The largest number of messages that wait for delivery in one direction of one worker (the page to the worker, or the worker to the
     * page); more messages are dropped.
     * <p>
     * worker 하나의 한 방향(페이지에서 worker 로, worker 에서 페이지로)에서 전달을 기다리는 메시지의 최대 수. 더 오는 메시지는 버린다.
     */
    public static final int MAX_PENDING_MESSAGES = 10000;

    /**
     * The largest number of pending timers inside one worker; <code>setTimeout</code> then throws a <code>RangeError</code>.
     * <p>
     * worker 하나 안에서 대기 중인 타이머의 최대 수. 그 이상이면 <code>setTimeout</code> 이 <code>RangeError</code> 를 던진다.
     */
    public static final int MAX_TIMERS = 10000;

    /**
     * The deepest nesting of <code>importScripts()</code> calls (an imported script that imports again).
     * <p>
     * <code>importScripts()</code> 호출의 최대 중첩 깊이(가져온 스크립트가 다시 가져오는 경우).
     */
    public static final int MAX_IMPORT_DEPTH = 32;

    /**
     * The largest number of open <code>BroadcastChannel</code> objects that one page and its workers share.
     * <p>
     * 한 페이지와 그 worker 가 함께 가질 수 있는 열린 <code>BroadcastChannel</code> 객체의 최대 수.
     */
    public static final int MAX_BROADCAST_CHANNELS = 1000;

    /**
     * Private constructor: constants only.
     * <p>
     * 비공개 생성자: 상수만 있다.
     */
    private WorkerLimits() {
    }
}
