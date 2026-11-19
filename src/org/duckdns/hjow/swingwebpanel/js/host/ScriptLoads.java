package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * The external script fetches of one page. A fetch starts when the parser finds the script (the HTML Standard starts fetching at "prepare a
 * script"), runs on its own daemon thread so that several scripts load in parallel, and reports its completion to the thread that owns the
 * page's DOM. That owner never runs a script on a fetch thread: it takes finished async loads with {@link #pollReady()} at its own task
 * boundaries and waits for progress with {@link #awaitProgress(Load, long)} while a parser-blocking script is loading. Closing the set (page
 * replaced, scripts disabled or disposed) wakes the owner and makes every later completion invisible, so a late response never runs.
 * <p>
 * 한 페이지의 외부 스크립트 가져오기 모음. 가져오기는 파서가 스크립트를 발굴할 때 시작되고(HTML 표준도 "prepare a script" 에서 가져오기를 시작한다),
 * 스크립트마다 자기 데몬 스레드에서 실행되어 여러 스크립트가 병렬로 로드된다. 완료는 페이지 DOM 의 소유 스레드에 알려 준다. 소유 스레드는 가져오기
 * 스레드에서 스크립트를 실행하지 않는다. 자기 작업 경계에서 {@link #pollReady()} 로 끝난 async 로드를 가져오고, 파서를 막는 스크립트를 불러오는 동안
 * {@link #awaitProgress(Load, long)} 로 진행을 기다린다. 모음을 닫으면(페이지 교체, 스크립트 비활성화, 폐기) 소유 스레드가 깨어나고 이후의 모든 완료가
 * 보이지 않게 되어 늦은 응답은 실행되지 않는다.
 */
final class ScriptLoads {

    /**
     * Maximum number of background (async and deferred) fetches that run at the same time for one page; further ones wait for a slot. A
     * parser-blocking fetch is never limited by it so that it cannot starve behind slow background loads.
     * <p>
     * 한 페이지에서 동시에 실행하는 배경(async, defer) 가져오기의 최대 수. 초과분은 빈자리를 기다린다. 파서를 막는 가져오기는 느린 배경 로드 뒤에서
     * 굶지 않도록 이 제한을 받지 않는다.
     */
    static final int MAX_BACKGROUND_FETCHES = 6;

    /**
     * One started fetch and its result.
     * <p>
     * 시작한 가져오기 하나와 그 결과.
     */
    static final class Load {
        /**
         * The absolute URL.
         * <p>
         * 절대 URL.
         */
        final String url;

        /**
         * The owner's entry for this load (opaque to this class).
         * <p>
         * 소유자가 이 로드에 붙인 항목(이 클래스에는 불투명).
         */
        final Object tag;

        /**
         * True if a completion is queued for {@link ScriptLoads#pollReady()} (async scripts).
         * <p>
         * 완료를 {@link ScriptLoads#pollReady()} 에 줄 세우면 true(async 스크립트).
         */
        final boolean queued;

        /**
         * True for a module script, which is fetched with the module rules (CORS, JavaScript MIME type, final URL).
         * <p>
         * 모듈 스크립트이면 true. 모듈 규칙(CORS, JavaScript MIME 타입, 최종 URL)으로 가져온다.
         */
        boolean module;

        /**
         * True if a module is fetched with cookies for every origin (<code>crossorigin="use-credentials"</code>).
         * <p>
         * 모듈을 모든 오리진에 쿠키를 보내며 가져오면(<code>crossorigin="use-credentials"</code>) true.
         */
        boolean credentials;

        /**
         * The loads of other script elements that asked for the same module while this one was in flight; they get this load's result.
         * Guarded by the set's monitor.
         * <p>
         * 이 로드가 진행 중일 때 같은 모듈을 요청한 다른 script 요소의 로드. 이 로드의 결과를 받는다. 모음의 모니터로 보호한다.
         */
        java.util.List<Load> followers;

        /**
         * The fetched text, valid when {@link #done} is true and {@link #error} is null; guarded by the set's monitor.
         * <p>
         * 가져온 텍스트. {@link #done} 이 true 이고 {@link #error} 가 null 일 때 유효하며 모음의 모니터로 보호한다.
         */
        private ScriptText text;

        /**
         * The failure, or null; guarded by the set's monitor.
         * <p>
         * 실패 원인. 없으면 null. 모음의 모니터로 보호한다.
         */
        private IOException error;

        /**
         * True after the fetch finished or failed; guarded by the set's monitor.
         * <p>
         * 가져오기가 끝나거나 실패한 뒤 true. 모음의 모니터로 보호한다.
         */
        private boolean done;

        /**
         * Creates a load record.
         * <p>
         * 로드 기록을 만든다.
         *
         * @param url    the URL / URL
         * @param tag    the owner's entry / 소유자의 항목
         * @param queued whether completion is queued / 완료를 줄 세울지 여부
         */
        Load(String url, Object tag, boolean queued) {
            this.url = url;
            this.tag = tag;
            this.queued = queued;
        }
    }

    /**
     * The monitor that guards all loads and signals every completion.
     * <p>
     * 모든 로드를 보호하고 모든 완료를 알리는 모니터.
     */
    private final Object monitor = new Object();

    /**
     * Finished queued loads in completion order, not yet taken by the owner.
     * <p>
     * 끝났지만 소유자가 아직 가져가지 않은 줄 세운 로드(완료 순서).
     */
    private final LinkedList<Load> ready = new LinkedList<Load>();

    /**
     * Limits the parallel background fetches.
     * <p>
     * 병렬 배경 가져오기 수를 제한한다.
     */
    private final Semaphore slots = new Semaphore(MAX_BACKGROUND_FETCHES, true);

    /**
     * True after {@link #close()}; guarded by the monitor.
     * <p>
     * {@link #close()} 뒤에는 true. 모니터로 보호한다.
     */
    private boolean closed;

    /**
     * The wake-up task of an owner that works from a task queue instead of waiting (null while the owner thread waits); guarded by the
     * monitor.
     * <p>
     * 기다리는 대신 작업 큐로 일하는 소유자의 깨우기 작업(소유 스레드가 기다리는 동안은 null). 모니터로 보호한다.
     */
    private Runnable notifier;

    /**
     * The first load of each module URL and credentials mode (the "module map" of the HTML Standard): a second script element for the same
     * module shares the one fetch instead of requesting it again. Guarded by the monitor.
     * <p>
     * 모듈 URL과 자격 증명 모드별 첫 번째 로드(HTML 표준의 "module map"). 같은 모듈의 두 번째 script 요소는 다시 요청하지 않고 하나의 가져오기를
     * 공유한다. 모니터로 보호한다.
     */
    private final java.util.Map<String, Load> moduleMap = new java.util.HashMap<String, Load>();

    /**
     * Starts fetching a script on its own thread (or fails at once when there is no source).
     * <p>
     * 스크립트 가져오기를 자기 스레드에서 시작한다(공급원이 없으면 즉시 실패한다).
     *
     * @param source     the source of script texts, or null / 스크립트 텍스트 공급원. 없으면 null
     * @param url        the absolute URL / 절대 URL
     * @param tag        the owner's entry / 소유자의 항목
     * @param queued     true to queue the completion for the owner (async scripts) / 완료를 소유자에게 줄 세우려면 true(async 스크립트)
     * @param background true if the fetch does not block the parser (it then uses a background slot) / 파서를 막지 않는 가져오기이면 true(배경 칸을 쓴다)
     * @return the load / 로드
     */
    Load start(final ScriptSource source, String url, Object tag, boolean queued, final boolean background) {
        return start(source, url, tag, queued, background, false, false);
    }

    /**
     * Starts fetching a script or a module on its own thread (or fails at once when there is no source). A module is fetched through
     * {@link ModuleScriptSource#fetchModule(String, boolean)} when the source offers it.
     * <p>
     * 스크립트나 모듈의 가져오기를 자기 스레드에서 시작한다(공급원이 없으면 즉시 실패한다). 모듈은 공급원이 제공하면
     * {@link ModuleScriptSource#fetchModule(String, boolean)} 로 가져온다.
     *
     * @param source      the source of script texts, or null / 스크립트 텍스트 공급원. 없으면 null
     * @param url         the absolute URL / 절대 URL
     * @param tag         the owner's entry / 소유자의 항목
     * @param queued      true to queue the completion for the owner / 완료를 소유자에게 줄 세우려면 true
     * @param background  true if the fetch does not block the parser / 파서를 막지 않는 가져오기이면 true
     * @param module      true for a module script / 모듈 스크립트이면 true
     * @param credentials true to send cookies to every origin (modules only) / 모든 오리진에 쿠키를 보내려면 true(모듈에만 해당)
     * @return the load / 로드
     */
    Load start(final ScriptSource source, String url, Object tag, boolean queued, final boolean background, boolean module,
            boolean credentials) {
        final Load load = new Load(url, tag, queued);
        load.module = module;
        load.credentials = credentials;
        if (source == null) {
            complete(load, null, new IOException("scripts cannot be loaded"));
            return load;
        }
        Runnable wake = null;
        boolean shared = false;
        synchronized (monitor) {
            if (closed) {
                load.done = true;
                load.error = new IOException("page closed");
                return load;
            }
            // 같은 모듈을 이미 가져오고 있거나 가져왔으면 새로 요청하지 않고 그 결과를 공유한다.
            if (module) {
                String mapKey = url + (credentials ? "|credentials" : "|same-origin");
                Load leader = moduleMap.get(mapKey);
                if (leader == null) {
                    moduleMap.put(mapKey, load);
                } else {
                    shared = true;
                    if (!leader.done) {
                        if (leader.followers == null) {
                            leader.followers = new java.util.ArrayList<Load>();
                        }
                        leader.followers.add(load);
                    } else if (finish(load, leader.text, leader.error)) {
                        // 이미 끝난 결과를 받아 줄 세워졌으면 작업 큐의 소유자에게 알려야 한다.
                        wake = notifier;
                        monitor.notifyAll();
                    }
                }
            }
        }
        if (shared) {
            if (wake != null) {
                wake.run();
            }
            return load;
        }
        Thread t = new Thread("swingwebpanel-script-fetch") {
            /**
             * Fetches the script and reports the result.
             * <p>
             * 스크립트를 가져와 결과를 알린다.
             */
            public void run() {
                boolean held = false;
                try {
                    if (background) {
                        // 배경 가져오기는 동시 실행 수 제한의 빈자리를 기다린다.
                        slots.acquire();
                        held = true;
                    }
                    if (isClosed()) {
                        complete(load, null, new IOException("page closed"));
                        return;
                    }
                    // 모듈은 모듈 규칙을 아는 공급원이면 그 규칙으로, 아니면 일반 가져오기로 가져온다.
                    if (load.module && source instanceof ModuleScriptSource) {
                        complete(load, ((ModuleScriptSource) source).fetchModule(load.url, load.credentials), null);
                    } else {
                        complete(load, new ScriptText(source.fetch(load.url), load.url), null);
                    }
                } catch (IOException e) {
                    complete(load, null, e);
                } catch (InterruptedException e) {
                    complete(load, null, new IOException("interrupted"));
                } catch (RuntimeException e) {
                    complete(load, null, new IOException(String.valueOf(e)));
                } finally {
                    if (held) {
                        slots.release();
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
        return load;
    }

    /**
     * Records the result of a load and wakes the owner; ignored after {@link #close()} or a second completion.
     * <p>
     * 로드의 결과를 기록하고 소유자를 깨운다. {@link #close()} 뒤나 두 번째 완료는 무시한다.
     *
     * @param load  the load / 로드
     * @param text  the text, or null on failure / 텍스트. 실패하면 null
     * @param error the failure, or null / 실패 원인. 없으면 null
     */
    private void complete(Load load, ScriptText text, IOException error) {
        Runnable wake = null;
        synchronized (monitor) {
            if (load.done) {
                return;
            }
            boolean woke = finish(load, text, error);
            // 같은 모듈을 기다리던 다른 script 요소의 로드도 같은 결과로 끝낸다.
            if (load.followers != null) {
                for (Load follower : load.followers) {
                    woke = finish(follower, text, error) || woke;
                }
                load.followers = null;
            }
            if (woke) {
                wake = notifier;
            }
            monitor.notifyAll();
        }
        // 소유자가 이미 작업 큐로 넘어갔으면 모니터 밖에서 소유자에게 깨우기를 요청한다.
        if (wake != null) {
            wake.run();
        }
    }

    /**
     * Begins a load that is completed from outside instead of by a fetch thread of this class (the image loads of <code>img</code>
     * elements). Its completion is queued for {@link #pollReady()} like an async script, and it wakes a waiting owner or the notifier.
     * <p>
     * 이 클래스의 가져오기 스레드가 아니라 바깥에서 완료하는 로드를 시작한다(<code>img</code> 요소의 이미지 로드). 완료는 async 스크립트처럼
     * {@link #pollReady()} 에 줄 세워지고, 기다리는 소유자나 깨우기 작업을 깨운다.
     *
     * @param tag the owner's entry (see {@link Load#tag}) / 소유자의 항목({@link Load#tag} 참고)
     * @return the load / 로드
     */
    Load beginExternal(Object tag) {
        Load load = new Load("", tag, true);
        synchronized (monitor) {
            // 이미 닫힌 모음이면 완료하지 않을 로드이므로 끝난 것으로 표시해 둔다(줄 세워지지 않는다).
            if (closed) {
                load.done = true;
                load.error = new IOException("page closed");
            }
        }
        return load;
    }

    /**
     * Completes a load that {@link #beginExternal(Object)} began. Callable from any thread, once per load; later calls are ignored.
     * <p>
     * {@link #beginExternal(Object)} 로 시작한 로드를 완료한다. 어느 스레드에서나 로드마다 한 번 부를 수 있고 이후 호출은 무시된다.
     *
     * @param load  the load / 로드
     * @param error the failure or null / 실패 원인 또는 null
     */
    void finishExternal(Load load, IOException error) {
        complete(load, null, error);
    }

    /**
     * Marks a load done with a result and queues it for the owner when it asks for that. The caller holds the monitor.
     * <p>
     * 로드를 결과와 함께 끝난 것으로 표시하고, 소유자가 원하면 줄 세운다. 호출자가 모니터를 쥐고 있다.
     *
     * @param load  the load / 로드
     * @param text  the text or null / 텍스트 또는 null
     * @param error the failure or null / 실패 원인 또는 null
     * @return true if the load was queued for the owner / 소유자에게 줄 세웠으면 true
     */
    private boolean finish(Load load, ScriptText text, IOException error) {
        load.done = true;
        load.text = text;
        load.error = error;
        if (load.queued && !closed) {
            ready.add(load);
            return true;
        }
        return false;
    }

    /**
     * Hands the delivery of finished loads over from a waiting owner thread to a task queue: from now on every finished queued load calls
     * the wake-up task (on the fetch thread, so it must only post work), and the call is made at once if loads are already waiting. The
     * wake-up stays set until {@link #close()}.
     * <p>
     * 끝난 로드의 전달을 기다리는 소유 스레드에서 작업 큐로 넘긴다. 이후 끝나는 줄 세운 로드마다 깨우기 작업을 호출하며(가져오기 스레드에서
     * 호출하므로 작업을 올리기만 해야 한다), 이미 기다리는 로드가 있으면 즉시 호출한다. 깨우기는 {@link #close()} 까지 유지된다.
     *
     * @param wake the wake-up task / 깨우기 작업
     */
    void setNotifier(Runnable wake) {
        boolean waiting;
        synchronized (monitor) {
            if (closed) {
                return;
            }
            notifier = wake;
            waiting = !ready.isEmpty();
        }
        if (waiting) {
            wake.run();
        }
    }

    /**
     * Takes the first finished queued load, or returns null when none is ready.
     * <p>
     * 끝난 줄 세운 로드 중 첫 번째를 가져온다. 준비된 것이 없으면 null.
     *
     * @return the load or null / 로드 또는 null
     */
    Load pollReady() {
        synchronized (monitor) {
            return ready.isEmpty() ? null : ready.removeFirst();
        }
    }

    /**
     * Waits until the given load is done, a queued load is ready, the set is closed or the time passes (whichever comes first). Spurious
     * returns are fine: the owner checks its conditions again.
     * <p>
     * 주어진 로드가 끝나거나, 줄 세운 로드가 준비되거나, 모음이 닫히거나, 시간이 지날 때까지(먼저 오는 것) 기다린다. 이유 없이 돌아와도
     * 된다. 소유자가 조건을 다시 확인한다.
     *
     * @param load      the load to wait for, or null to wait for any queued load only / 기다릴 로드. 줄 세운 로드만 기다리려면 null
     * @param maxMillis the longest wait / 최대 대기 시간(밀리초)
     * @throws InterruptedException if the owner thread is interrupted / 소유 스레드가 인터럽트되면
     */
    void awaitProgress(Load load, long maxMillis) throws InterruptedException {
        synchronized (monitor) {
            if ((load != null && load.done) || !ready.isEmpty() || closed) {
                return;
            }
            monitor.wait(maxMillis);
        }
    }

    /**
     * Tells whether a load finished (successfully or not).
     * <p>
     * 로드가 끝났는지(성공이든 실패든) 알려 준다.
     *
     * @param load the load / 로드
     * @return true when done / 끝났으면 true
     */
    boolean isDone(Load load) {
        synchronized (monitor) {
            return load.done;
        }
    }

    /**
     * Returns the text of a finished load.
     * <p>
     * 끝난 로드의 텍스트를 반환한다.
     *
     * @param load the load that {@link #isDone(Load)} reported as done / {@link #isDone(Load)} 가 끝났다고 한 로드
     * @return the text and the final URL / 텍스트와 최종 URL
     * @throws IOException if the fetch failed / 가져오기가 실패했으면
     */
    ScriptText result(Load load) throws IOException {
        synchronized (monitor) {
            if (load.error != null) {
                throw load.error;
            }
            return load.text;
        }
    }

    /**
     * Tells whether the set was closed.
     * <p>
     * 모음이 닫혔는지 알려 준다.
     *
     * @return true after {@link #close()} / {@link #close()} 뒤에는 true
     */
    boolean isClosed() {
        synchronized (monitor) {
            return closed;
        }
    }

    /**
     * Closes the set: the owner wakes up, nothing is queued any more and every unfinished fetch is forgotten (its thread ends when its
     * request ends, which the component cancels through its own token). Safe to call from any thread and more than once.
     * <p>
     * 모음을 닫는다: 소유자가 깨어나고 더 이상 줄 세우지 않으며 끝나지 않은 가져오기는 잊는다(스레드는 요청이 끝나면 끝나며, 요청은 컴포넌트가
     * 자기 토큰으로 취소한다). 어느 스레드에서나 여러 번 불러도 안전하다.
     */
    void close() {
        synchronized (monitor) {
            closed = true;
            ready.clear();
            notifier = null;
            monitor.notifyAll();
        }
    }
}
