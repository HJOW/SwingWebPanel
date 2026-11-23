package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext;

/**
 * The rendering of an <code>OfflineAudioContext</code> (Web Audio API 1.3.3, "begin offline rendering"): a daemon thread named
 * <code>swingwebpanel-audio-offline-N</code> that renders the render quanta one after the other, as fast as it can, so that neither the script thread of the page
 * nor the event thread of the user interface waits for it. Between two quanta it looks for a <em>suspension</em>: <code>suspend(time)</code> asks for the rendering to stop at the
 * first quantum boundary at or after a time; the thread then tells the page (a task of the page resolves the promise of the suspension, sets the state to
 * <code>suspended</code> and fires <code>statechange</code>) and waits for <code>resume()</code>, so a script can change the graph while the rendering stands still, which is the
 * purpose of the feature. When the last quantum was rendered the thread hands the result to the page (a task of the page sets the state to <code>closed</code>, resolves the
 * promise of <code>startRendering()</code> with the buffer and fires <code>complete</code>). A cancelled rendering (the page is replaced or disposed) ends the thread without telling anyone.
 * <p>
 * The thread never calls the script. It uses the engine directly (the engine accepts control calls of the script thread while it renders) and the page only through
 * {@link WebAudioApi#postTask(Runnable)}, which drops what comes after the disposal.
 * <p>
 * <code>OfflineAudioContext</code> 의 렌더링(Web Audio API 1.3.3, "begin offline rendering"): <code>swingwebpanel-audio-offline-N</code> 이라는 이름의 데몬 스레드가 render quantum 을 연달아
 * 가능한 한 빠르게 렌더링한다. 페이지의 스크립트 스레드도 사용자 인터페이스의 이벤트 스레드도 이것을 기다리지 않는다. quantum 사이에 <em>일시 중단</em>을 찾는다:
 * <code>suspend(time)</code> 은 그 시간 이후의 첫 quantum 경계에서 렌더링이 멈추도록 요청한다. 그러면 스레드가 페이지에 알리고(페이지의 작업이 일시 중단의 프라미스를
 * resolve 하고 상태를 <code>suspended</code> 로 정하고 <code>statechange</code> 를 발생시킨다) <code>resume()</code> 을 기다린다. 그래서 렌더링이 서 있는 동안 스크립트가 그래프를 바꿀 수
 * 있으며 이것이 이 기능의 목적이다. 마지막 quantum 을 렌더링하면 스레드가 결과를 페이지에 넘긴다(페이지의 작업이 상태를 <code>closed</code> 로 정하고, <code>startRendering()</code> 의 프라미스를
 * 버퍼로 resolve 하고, <code>complete</code> 를 발생시킨다). 취소된 렌더링(페이지가 교체되거나 폐기됨)은 아무에게도 알리지 않고 스레드를 끝낸다.
 * <p>
 * 스레드는 스크립트를 부르지 않는다. 엔진은 직접 쓰고(엔진은 렌더링 중에도 스크립트 스레드의 제어 호출을 받는다) 페이지는 {@link WebAudioApi#postTask(Runnable)} 로만 쓰며, 이것은 폐기 뒤에
 * 오는 것을 버린다.
 */
final class OfflineRender implements Runnable {

    /**
     * The owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * The owner of the completion event.
     * <p>
     * 완료 이벤트를 만드는 쪽.
     */
    private final ContextApi contexts;

    /**
     * The context.
     * <p>
     * 컨텍스트.
     */
    private final ContextValue ctx;

    /**
     * The engine context that is rendered.
     * <p>
     * 렌더링하는 엔진 컨텍스트.
     */
    private final OfflineAudioContext engine;

    /**
     * Guards the fields below and wakes the thread.
     * <p>
     * 아래 필드를 지키고 스레드를 깨운다.
     */
    private final Object lock = new Object();

    /**
     * The suspensions by the frame at which the rendering stops.
     * <p>
     * 렌더링이 멈추는 프레임별 일시 중단.
     */
    private final TreeMap<Long, Promises.Deferred> suspends = new TreeMap<Long, Promises.Deferred>();

    /**
     * The promise of <code>startRendering()</code>.
     * <p>
     * <code>startRendering()</code> 의 프라미스.
     */
    private Promises.Deferred renderPromise;

    /**
     * True while the thread waits for <code>resume()</code>.
     * <p>
     * 스레드가 <code>resume()</code> 을 기다리는 동안 true.
     */
    private boolean paused;

    /**
     * The rendering thread, null before the start.
     * <p>
     * 렌더링 스레드. 시작 전에는 null.
     */
    private Thread thread;

    /**
     * True after {@link #cancel()}.
     * <p>
     * {@link #cancel()} 뒤에는 true.
     */
    private volatile boolean cancelled;

    /**
     * Creates the renderer of an offline context (the thread starts with {@link #begin}).
     * <p>
     * 오프라인 컨텍스트의 렌더러를 만든다(스레드는 {@link #begin} 으로 시작한다).
     *
     * @param api      the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param contexts the context interfaces, which build the completion event / 완료 이벤트를 만드는 컨텍스트 인터페이스
     * @param ctx      the context / 컨텍스트
     */
    OfflineRender(WebAudioApi api, ContextApi contexts, ContextValue ctx) {
        this.api = api;
        this.contexts = contexts;
        this.ctx = ctx;
        this.engine = ctx.offline();
    }

    /**
     * Schedules a suspension at a frame (a multiple of the render quantum size).
     * <p>
     * 프레임(render quantum 크기의 배수)에 일시 중단을 예약한다.
     *
     * @param frame   the frame / 프레임
     * @param promise the promise of the suspension / 일시 중단의 프라미스
     * @return false if a suspension is scheduled at this frame already / 이 프레임에 이미 일시 중단이 예약되어 있으면 false
     */
    boolean schedule(long frame, Promises.Deferred promise) {
        synchronized (lock) {
            Long key = Long.valueOf(frame);
            if (suspends.containsKey(key)) {
                return false;
            }
            suspends.put(key, promise);
            return true;
        }
    }

    /**
     * Starts the thread that renders.
     * <p>
     * 렌더링하는 스레드를 시작한다.
     *
     * @param promise the promise of <code>startRendering()</code> / <code>startRendering()</code> 의 프라미스
     */
    void begin(Promises.Deferred promise) {
        synchronized (lock) {
            renderPromise = promise;
            Thread t = new Thread(this, "swingwebpanel-audio-offline-" + api.nextOfflineSerial());
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY - 1);
            thread = t;
            t.start();
        }
    }

    /**
     * Lets a suspended rendering go on.
     * <p>
     * 일시 중단된 렌더링을 이어 가게 한다.
     */
    void resume() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    /**
     * Stops the rendering for good without telling anyone.
     * <p>
     * 아무에게도 알리지 않고 렌더링을 완전히 멈춘다.
     */
    void cancel() {
        cancelled = true;
        Thread t;
        synchronized (lock) {
            lock.notifyAll();
            t = thread;
        }
        if (t != null) {
            t.interrupt();
        }
    }

    /**
     * The loop of the rendering thread: it stops at the suspensions, renders every quantum and hands the result to the page.
     * <p>
     * 렌더링 스레드의 루프: 일시 중단에서 멈추고, quantum 을 모두 렌더링하고, 결과를 페이지에 넘긴다.
     */
    public void run() {
        try {
            // 모든 quantum 을 렌더링할 때까지(또는 취소될 때까지) 되풀이한다.
            for (;;) {
                if (cancelled) {
                    return;
                }
                long frame = engine.getCurrentFrame();
                List<Promises.Deferred> due = null;
                synchronized (lock) {
                    // 이 프레임에 이르렀거나 이미 지난(예약이 늦은) 일시 중단을 모두 꺼낸다.
                    while (!suspends.isEmpty() && suspends.firstKey().longValue() <= frame) {
                        if (due == null) {
                            due = new ArrayList<Promises.Deferred>();
                        }
                        due.add(suspends.remove(suspends.firstKey()));
                    }
                    if (due != null) {
                        paused = true;
                    }
                }
                if (due != null) {
                    suspended(due);
                    synchronized (lock) {
                        // resume() 이나 취소가 깨울 때까지 기다린다.
                        while (paused && !cancelled) {
                            lock.wait();
                        }
                    }
                    if (cancelled) {
                        return;
                    }
                }
                if (engine.isComplete()) {
                    break;
                }
                engine.renderNextQuantum();
            }
            completed(engine.finishRendering());
        } catch (InterruptedException e) {
            // 취소: 아무에게도 알리지 않고 끝낸다.
            return;
        } catch (RuntimeException e) {
            failed(e);
        }
    }

    /**
     * Tells the page that the rendering stands still at a suspension.
     * <p>
     * 렌더링이 일시 중단에서 서 있음을 페이지에 알린다.
     *
     * @param due the promises of the suspensions that were reached / 이른 일시 중단의 프라미스
     */
    private void suspended(final List<Promises.Deferred> due) {
        api.postTask(new Runnable() {
            /**
             * Sets the state, fires the event and resolves the promises.
             * <p>
             * 상태를 정하고, 이벤트를 발생시키고, 프라미스를 resolve 한다.
             */
            public void run() {
                if (ctx.setState("suspended")) {
                    api.fire(ctx, "statechange");
                }
                // 일시 중단마다 프라미스를 resolve 한다.
                for (int i = 0; i < due.size(); i++) {
                    due.get(i).resolve(Js.UNDEFINED);
                }
            }
        });
    }

    /**
     * Hands the rendered audio to the page: the state becomes <code>closed</code>, the promise of <code>startRendering()</code> is resolved with the buffer and the
     * <code>complete</code> event follows in a task of its own.
     * <p>
     * 렌더링한 오디오를 페이지에 넘긴다: 상태가 <code>closed</code> 가 되고, <code>startRendering()</code> 의 프라미스가 버퍼로 resolve 되며, <code>complete</code> 이벤트가 자기 작업으로
     * 뒤따른다.
     *
     * @param result the rendered audio / 렌더링한 오디오
     */
    private void completed(AudioBuffer result) {
        final AudioBufferValue value = AudioBufferValue.adopt(result);
        api.postTask(new Runnable() {
            /**
             * Finishes the context in a task of the page.
             * <p>
             * 페이지의 작업에서 컨텍스트를 끝낸다.
             */
            public void run() {
                api.unregister(ctx);
                if (ctx.setState("closed")) {
                    api.fire(ctx, "statechange");
                }
                Promises.Deferred d;
                synchronized (lock) {
                    d = renderPromise;
                }
                d.resolve(api.bridge().toJs(value));
                // complete 이벤트는 프라미스를 확정한 뒤 별도의 작업으로 발생시킨다.
                api.postTask(new Runnable() {
                    /**
                     * Fires <code>complete</code>.
                     * <p>
                     * <code>complete</code> 를 발생시킨다.
                     */
                    public void run() {
                        api.fire(ctx, contexts.completionEvent(value));
                    }
                });
            }
        });
    }

    /**
     * Tells the page that the rendering failed unexpectedly: the promise of <code>startRendering()</code> is rejected.
     * <p>
     * 렌더링이 예기치 않게 실패했음을 페이지에 알린다: <code>startRendering()</code> 의 프라미스가 reject 된다.
     *
     * @param error the failure / 실패
     */
    private void failed(final RuntimeException error) {
        api.postTask(new Runnable() {
            /**
             * Rejects the promise and closes the context.
             * <p>
             * 프라미스를 reject 하고 컨텍스트를 닫는다.
             */
            public void run() {
                api.unregister(ctx);
                if (ctx.setState("closed")) {
                    api.fire(ctx, "statechange");
                }
                Promises.Deferred d;
                synchronized (lock) {
                    d = renderPromise;
                }
                d.reject(api.domError("UnknownError", "The rendering failed: " + error).getValue(api.realm()));
            }
        });
    }
}
