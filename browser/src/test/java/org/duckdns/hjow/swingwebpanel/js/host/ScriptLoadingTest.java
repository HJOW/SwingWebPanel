package org.duckdns.hjow.swingwebpanel.js.host;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.ConsoleHook;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the parallel loading of external scripts (plan item P15-04-a): fetches start when the parser finds a script, async scripts run
 * when they are ready (even while the parser waits for a slow parser-blocking script), defer scripts keep their order, the load event
 * waits for the async scripts but DOMContentLoaded does not, and nothing runs after the page is closed. Every fetch is gated by a latch
 * that the test opens, so the orders that the HTML Standard allows and forbids are driven deterministically.
 * <p>
 * 외부 스크립트 병렬 로딩의 시험(계획 P15-04-a): 파서가 스크립트를 발굴할 때 가져오기가 시작되고, async 스크립트는 준비되는 즉시(파서가 느린
 * 파서 차단 스크립트를 기다리는 중에도) 실행되며, defer 스크립트는 순서를 지키고, load 이벤트는 async 를 기다리지만 DOMContentLoaded 는 기다리지
 * 않으며, 페이지가 닫힌 뒤에는 아무것도 실행되지 않는다. 모든 가져오기는 시험이 여는 래치로 막아 두므로 표준이 허용하거나 금지하는 순서를 결정적으로
 * 재현한다.
 */
public class ScriptLoadingTest {

    /**
     * The longest time that a test waits for something that must happen.
     * <p>
     * 시험이 반드시 일어날 일을 기다리는 최대 시간.
     */
    private static final long WAIT_MILLIS = 20000;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ScriptLoadingTest() {
    }

    /**
     * A script source whose fetches wait for a latch per URL and record when they started.
     * <p>
     * URL 마다 래치를 기다리고 시작 시점을 기록하는 스크립트 공급원.
     */
    private static final class GatedSource implements ScriptSource {
        /**
         * The script texts by URL.
         * <p>
         * URL 별 스크립트 텍스트.
         */
        final Map<String, String> texts = new HashMap<String, String>();

        /**
         * The gates by URL; a URL without a gate does not wait.
         * <p>
         * URL 별 대문. 대문이 없는 URL 은 기다리지 않는다.
         */
        final Map<String, CountDownLatch> gates = Collections.synchronizedMap(new HashMap<String, CountDownLatch>());

        /**
         * The URLs in the order that their fetches started.
         * <p>
         * 가져오기가 시작된 순서의 URL.
         */
        final List<String> started = Collections.synchronizedList(new ArrayList<String>());

        /**
         * Registers a script with a closed gate.
         * <p>
         * 닫힌 대문과 함께 스크립트를 등록한다.
         *
         * @param name the file name below the document directory / 문서 디렉터리 아래 파일 이름
         * @param text the script / 스크립트
         */
        void gated(String name, String text) {
            texts.put("http://example.test/dir/" + name, text);
            gates.put("http://example.test/dir/" + name, new CountDownLatch(1));
        }

        /**
         * Registers a script without a gate.
         * <p>
         * 대문 없이 스크립트를 등록한다.
         *
         * @param name the file name / 파일 이름
         * @param text the script / 스크립트
         */
        void open(String name, String text) {
            texts.put("http://example.test/dir/" + name, text);
        }

        /**
         * Opens the gate of a script.
         * <p>
         * 스크립트의 대문을 연다.
         *
         * @param name the file name / 파일 이름
         */
        void release(String name) {
            gates.get("http://example.test/dir/" + name).countDown();
        }

        /**
         * Tells whether the fetch of a script started.
         * <p>
         * 스크립트의 가져오기가 시작되었는지 알려 준다.
         *
         * @param name the file name / 파일 이름
         * @return true if started / 시작되었으면 true
         */
        boolean hasStarted(String name) {
            return started.contains("http://example.test/dir/" + name);
        }

        /**
         * Records the start, waits for the gate and returns the text.
         * <p>
         * 시작을 기록하고 대문을 기다린 뒤 텍스트를 반환한다.
         *
         * @param url the URL / URL
         * @return the text / 텍스트
         * @throws IOException if the URL is unknown or the gate stays closed / URL 을 모르거나 대문이 계속 닫혀 있으면
         */
        public String fetch(String url) throws IOException {
            started.add(url);
            CountDownLatch gate = gates.get(url);
            try {
                if (gate != null && !gate.await(WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                    throw new IOException("gate stayed closed " + url);
                }
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
            String t = texts.get(url);
            if (t == null) {
                throw new IOException("404 " + url);
            }
            return t;
        }
    }

    /**
     * A page that is loaded on its own thread so that the test thread can watch it while it waits for gated scripts.
     * <p>
     * 막힌 스크립트를 기다리는 동안 시험 스레드가 지켜볼 수 있도록 자기 스레드에서 로드하는 페이지.
     */
    private static final class Page {
        /**
         * The gated source.
         * <p>
         * 막아 둔 공급원.
         */
        final GatedSource source = new GatedSource();

        /**
         * The stand-in for the document queue, or null: the page then waits on the loading thread for its scripts.
         * <p>
         * 문서 큐 대역. 없으면 null이며 페이지는 로딩 스레드에서 스크립트를 기다린다.
         */
        QueueScheduler queue;

        /**
         * The thread that wrote each log line, in the order of the lines.
         * <p>
         * 각 로그 줄을 쓴 스레드(줄 순서).
         */
        final List<String> threads = Collections.synchronizedList(new ArrayList<String>());

        /**
         * The console lines (level log only), written by the page thread.
         * <p>
         * 콘솔 줄(log 수준만). 페이지 스레드가 쓴다.
         */
        final List<String> log = Collections.synchronizedList(new ArrayList<String>());

        /**
         * The scripts of the page.
         * <p>
         * 페이지의 스크립트.
         */
        PageScripts scripts;

        /**
         * The loading thread.
         * <p>
         * 로딩 스레드.
         */
        Thread thread;

        /**
         * True once the loading thread left {@link PageScripts#completeParsing()}.
         * <p>
         * 로딩 스레드가 {@link PageScripts#completeParsing()} 를 떠난 뒤 true.
         */
        volatile boolean finished;

        /**
         * Starts loading a page on a new thread.
         * <p>
         * 새 스레드에서 페이지 로딩을 시작한다.
         *
         * @param html the source / 원문
         */
        void start(final String html) {
            final Document document = Document.createHtmlDocument();
            document.setMarkupSupport(new HtmlMarkupSupport());
            CssSelectorEngine.install(document);
            document.setUrl("http://example.test/dir/page.html");
            HostEnvironment env = new HostEnvironment() {
                /**
                 * Returns the gated source.
                 * <p>
                 * 막아 둔 공급원을 반환한다.
                 *
                 * @return the source / 공급원
                 */
                public ScriptSource getScriptSource() {
                    return source;
                }

                /**
                 * Returns the document queue stand-in, or null for a page without a queue.
                 * <p>
                 * 문서 큐 대역을 반환한다. 큐가 없는 페이지이면 null.
                 *
                 * @return the scheduler or null / 스케줄러 또는 null
                 */
                public Scheduler getScheduler() {
                    return queue;
                }
            };
            scripts = new PageScripts(document, env);
            scripts.getRealm().setConsole(new ConsoleHook() {
                /**
                 * Records a log line.
                 * <p>
                 * log 줄을 기록한다.
                 *
                 * @param level the level / 수준
                 * @param text  the text / 문자열
                 */
                public void message(String level, String text) {
                    if ("log".equals(level)) {
                        log.add(text);
                        threads.add(Thread.currentThread().getName());
                    }
                }
            });
            thread = new Thread("script-loading-test-page") {
                /**
                 * Parses the page and completes the loading.
                 * <p>
                 * 페이지를 해석하고 로딩을 마무리한다.
                 */
                public void run() {
                    HtmlParser parser = new HtmlParser(document, html);
                    parser.setScripting(true);
                    parser.setListener(scripts.createParserListener());
                    scripts.parsingStarted();
                    parser.run();
                    scripts.completeParsing();
                    finished = true;
                }
            };
            thread.setDaemon(true);
            thread.start();
        }

        /**
         * Waits until the log text equals the expected one.
         * <p>
         * 로그 텍스트가 기대한 것과 같아질 때까지 기다린다.
         *
         * @param expected the comma separated log / 쉼표로 이은 로그
         * @param message  the failure message / 실패 메시지
         */
        void awaitLog(String expected, String message) {
            long end = System.currentTimeMillis() + WAIT_MILLIS;
            // 기대한 로그가 될 때까지, 또는 시간이 다 될 때까지 짧게 쉬며 확인한다.
            while (!joined().equals(expected) && System.currentTimeMillis() < end) {
                sleep(5);
            }
            Check.equal(expected, joined(), message);
        }

        /**
         * Waits until a fetch started.
         * <p>
         * 가져오기가 시작될 때까지 기다린다.
         *
         * @param name the file name / 파일 이름
         */
        void awaitStarted(String name) {
            long end = System.currentTimeMillis() + WAIT_MILLIS;
            // 가져오기가 시작될 때까지, 또는 시간이 다 될 때까지 짧게 쉬며 확인한다.
            while (!source.hasStarted(name) && System.currentTimeMillis() < end) {
                sleep(5);
            }
            Check.isTrue(source.hasStarted(name), "the fetch of " + name + " started");
        }

        /**
         * Waits until at least a number of fetches started.
         * <p>
         * 가져오기가 적어도 주어진 수만큼 시작될 때까지 기다린다.
         *
         * @param count the number / 수
         */
        void awaitStartedCount(int count) {
            long end = System.currentTimeMillis() + WAIT_MILLIS;
            // 시작한 가져오기 수가 충분해질 때까지, 또는 시간이 다 될 때까지 짧게 쉬며 확인한다.
            while (source.started.size() < count && System.currentTimeMillis() < end) {
                sleep(5);
            }
            Check.isTrue(source.started.size() >= count, count + " fetches started");
        }

        /**
         * Waits until the loading thread finished.
         * <p>
         * 로딩 스레드가 끝날 때까지 기다린다.
         *
         * @param message the failure message / 실패 메시지
         */
        void awaitFinished(String message) {
            try {
                thread.join(WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Check.isTrue(finished, message);
        }

        /**
         * Returns the log as one comma separated text.
         * <p>
         * 로그를 쉼표로 이은 텍스트로 반환한다.
         *
         * @return the text / 텍스트
         */
        String joined() {
            StringBuilder sb = new StringBuilder();
            synchronized (log) {
                // 기록된 줄을 쉼표로 이어 붙인다.
                for (String s : log) {
                    if (sb.length() > 0) {
                        sb.append(',');
                    }
                    sb.append(s);
                }
            }
            return sb.toString();
        }
    }

    /**
     * Sleeps without throwing.
     * <p>
     * 예외 없이 잠시 쉰다.
     *
     * @param ms milliseconds / 밀리초
     */
    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * P15-04-a: async and defer fetches start when the parser finds them, in parallel with a parser-blocking script that is still loading;
     * the ready async script runs while the parser waits, the blocking script keeps its place, defer runs after parsing.
     * <p>
     * P15-04-a: async 와 defer 가져오기는 파서가 발굴할 때 시작되어 아직 로딩 중인 파서 차단 스크립트와 병렬로 진행된다. 준비된 async 스크립트는
     * 파서가 기다리는 동안 실행되고, 차단 스크립트는 자리를 지키며, defer 는 파싱이 끝난 뒤 실행된다.
     */
    public void testFetchesStartAtDiscoveryAndAsyncRunsWhileParserWaits() {
        Page page = new Page();
        page.source.gated("a.js", "console.log('async')");
        page.source.gated("d.js", "console.log('defer')");
        page.source.gated("b.js", "console.log('blocking')");
        page.start("<script async src=a.js></script><script defer src=d.js></script><script src=b.js></script>"
                + "<script>console.log('inline')</script>");
        page.awaitStarted("b.js");
        Check.isTrue(page.source.hasStarted("a.js"), "the async fetch started at discovery, before the parser reached b.js's end");
        Check.isTrue(page.source.hasStarted("d.js"), "the defer fetch started at discovery");
        Check.equal("", page.joined(), "nothing ran while all three are loading");
        page.source.release("a.js");
        page.awaitLog("async", "the ready async script ran while the parser still waited for b.js");
        Check.isFalse(page.finished, "the parser is still blocked");
        page.source.release("b.js");
        page.awaitLog("async,blocking,inline", "the blocking script ran in its place, then the inline one");
        Check.isFalse(page.finished, "the page waits for the defer script");
        page.source.release("d.js");
        page.awaitFinished("the page finished");
        Check.equal("async,blocking,inline,defer", page.joined(), "defer ran after the parser");
    }

    /**
     * P15-04-a: defer scripts keep the document order whatever order their responses arrive in.
     * <p>
     * P15-04-a: defer 스크립트는 응답이 어떤 순서로 도착하든 문서 순서를 지킨다.
     */
    public void testDeferKeepsDocumentOrder() {
        Page page = new Page();
        page.source.gated("one.js", "console.log('one')");
        page.source.gated("two.js", "console.log('two')");
        page.start("<script defer src=one.js></script><script defer src=two.js></script>");
        page.awaitStarted("one.js");
        page.awaitStarted("two.js");
        page.source.release("two.js");
        sleep(150);
        Check.equal("", page.joined(), "the second defer script waits for the first");
        page.source.release("one.js");
        page.awaitFinished("the page finished");
        Check.equal("one,two", page.joined(), "document order");
    }

    /**
     * P15-04-a: DOMContentLoaded does not wait for an async script, load does.
     * <p>
     * P15-04-a: DOMContentLoaded 는 async 스크립트를 기다리지 않고 load 는 기다린다.
     */
    public void testDomContentLoadedDoesNotWaitForAsyncButLoadDoes() {
        Page page = new Page();
        page.source.gated("slow.js", "console.log('async')");
        page.start("<script>document.addEventListener('DOMContentLoaded', function () { console.log('dcl'); });"
                + "window.addEventListener('load', function () { console.log('load'); });</script><script async src=slow.js></script>");
        page.awaitLog("dcl", "DOMContentLoaded fired while the async script was loading");
        Check.isFalse(page.finished, "load waits for the async script");
        page.source.release("slow.js");
        page.awaitFinished("the page finished");
        Check.equal("dcl,async,load", page.joined(), "async script, then load");
    }

    /**
     * P15-04-a: a failing async fetch fires error on the element and does not hold the page back.
     * <p>
     * P15-04-a: 실패한 async 가져오기는 요소에 error 를 보내고 페이지를 붙잡지 않는다.
     */
    public void testFailedAsyncFiresErrorAndLoadStillFires() {
        Page page = new Page();
        page.start("<script async src=missing.js onerror=\"console.log('error')\"></script>"
                + "<script>window.addEventListener('load', function () { console.log('load'); });</script>");
        page.awaitFinished("the page finished");
        Check.equal("error,load", page.joined(), "error event, then load");
    }

    /**
     * P15-04-a: background fetches are limited to {@link ScriptLoads#MAX_BACKGROUND_FETCHES} at the same time; a finished one frees a slot.
     * <p>
     * P15-04-a: 배경 가져오기는 동시에 {@link ScriptLoads#MAX_BACKGROUND_FETCHES} 개로 제한되며 끝난 것이 빈자리를 만든다.
     */
    public void testBackgroundFetchesAreLimited() {
        Page page = new Page();
        StringBuilder html = new StringBuilder();
        int total = ScriptLoads.MAX_BACKGROUND_FETCHES + 2;
        // 제한보다 두 개 많은 async 스크립트를 만든다.
        for (int i = 0; i < total; i++) {
            page.source.gated("s" + i + ".js", "console.log('s" + i + "')");
            html.append("<script async src=s").append(i).append(".js></script>");
        }
        page.start(html.toString());
        page.awaitStartedCount(ScriptLoads.MAX_BACKGROUND_FETCHES);
        sleep(200);
        Check.equal(ScriptLoads.MAX_BACKGROUND_FETCHES, page.source.started.size(), "only the limit of fetches runs at once");
        // 시작한 것 중 하나를 끝내면 대기 중이던 가져오기 하나가 빈자리를 얻는다. 어느 것이 먼저 시작했는지는 스레드 일정에 달려 있다.
        String first = page.source.started.get(0);
        page.source.release(first.substring(first.lastIndexOf('/') + 1));
        page.awaitStartedCount(ScriptLoads.MAX_BACKGROUND_FETCHES + 1);
        // 나머지를 모두 열어 페이지를 끝낸다.
        for (int i = 0; i < total; i++) {
            page.source.release("s" + i + ".js");
        }
        page.awaitFinished("the page finished");
        Check.equal(total, page.log.size(), "every async script ran");
    }

    /**
     * P15-04-a: closing the page while the parser waits for a blocking script ends the wait at once; nothing runs, not even after the late
     * response arrives.
     * <p>
     * P15-04-a: 파서가 차단 스크립트를 기다리는 동안 페이지를 닫으면 대기가 즉시 끝난다. 늦은 응답이 도착해도 아무것도 실행되지 않는다.
     */
    public void testClosingEndsTheWaitAndSuppressesLateResponses() {
        Page page = new Page();
        page.source.gated("b.js", "console.log('blocking')");
        page.source.gated("a.js", "console.log('async')");
        page.start("<script async src=a.js></script><script src=b.js></script><script>console.log('inline')</script>");
        page.awaitStarted("b.js");
        page.scripts.requestDisposal();
        page.awaitFinished("the wait ended when the page was closed");
        page.source.release("b.js");
        page.source.release("a.js");
        sleep(200);
        Check.equal("", page.joined(), "late responses never ran");
        page.scripts.dispose();
    }

    /**
     * P15-04-a: closing the page while it waits for an async script after parsing ends the wait, and the load event does not fire.
     * <p>
     * P15-04-a: 파싱 뒤 async 스크립트를 기다리는 동안 페이지를 닫으면 대기가 끝나고 load 이벤트는 발생하지 않는다.
     */
    public void testClosingWhileWaitingForAsyncAfterParsing() {
        Page page = new Page();
        page.source.gated("slow.js", "console.log('async')");
        page.start("<script>window.addEventListener('load', function () { console.log('load'); });</script><script async src=slow.js></script>");
        page.awaitStarted("slow.js");
        sleep(100);
        Check.isFalse(page.finished, "the page waits for the async script");
        page.scripts.requestDisposal();
        page.awaitFinished("the wait ended when the page was closed");
        page.source.release("slow.js");
        sleep(200);
        Check.equal("", page.joined(), "neither the late script nor load ran");
        page.scripts.dispose();
    }

    /**
     * A stand-in for the document queue: tasks wait in a queue until the test thread (playing the document thread) runs them.
     * <p>
     * 문서 큐의 대역: 작업은 시험 스레드(문서 스레드 역할)가 실행할 때까지 큐에서 기다린다.
     */
    private static final class QueueScheduler implements Scheduler {
        /**
         * The posted tasks.
         * <p>
         * 올라온 작업.
         */
        final java.util.concurrent.LinkedBlockingQueue<Runnable> tasks = new java.util.concurrent.LinkedBlockingQueue<Runnable>();

        /**
         * Returns the wall clock.
         * <p>
         * 벽시계를 반환한다.
         *
         * @return milliseconds / 밀리초
         */
        public long now() {
            return System.currentTimeMillis();
        }

        /**
         * Queues a task; the delay is ignored.
         * <p>
         * 작업을 큐에 올린다. 지연은 무시한다.
         *
         * @param task    the task / 작업
         * @param delayMs ignored / 무시
         * @return the handle / 핸들
         */
        public Handle post(final Runnable task, long delayMs) {
            final boolean[] cancelled = new boolean[1];
            tasks.add(new Runnable() {
                /**
                 * Runs the task unless it was cancelled.
                 * <p>
                 * 취소되지 않았으면 작업을 실행한다.
                 */
                public void run() {
                    if (!cancelled[0]) {
                        task.run();
                    }
                }
            });
            return new Handle() {
                /**
                 * Cancels the task.
                 * <p>
                 * 작업을 취소한다.
                 */
                public void cancel() {
                    cancelled[0] = true;
                }
            };
        }
    }

    /**
     * Plays the document thread: runs queued tasks on the calling thread until the log equals the expected text.
     * <p>
     * 문서 스레드 역할: 로그가 기대한 텍스트가 될 때까지 큐의 작업을 호출한 스레드에서 실행한다.
     *
     * @param page     the page / 페이지
     * @param expected the expected log / 기대한 로그
     * @param message  the failure message / 실패 메시지
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    private static void runQueueUntilLog(Page page, String expected, String message) throws InterruptedException {
        long end = System.currentTimeMillis() + WAIT_MILLIS;
        // 기대한 로그가 될 때까지 큐에서 작업을 하나씩 꺼내 실행한다.
        while (!page.joined().equals(expected) && System.currentTimeMillis() < end) {
            Runnable task = page.queue.tasks.poll(20, TimeUnit.MILLISECONDS);
            if (task != null) {
                task.run();
            }
        }
        Check.equal(expected, page.joined(), message);
    }

    /**
     * P15-04-b: with a document queue the page does not wait for a slow async script: loading ends after DOMContentLoaded, the script runs
     * on the queue thread when its response arrives, and the load event follows it.
     * <p>
     * P15-04-b: 문서 큐가 있으면 페이지는 느린 async 스크립트를 기다리지 않는다. 로딩은 DOMContentLoaded 뒤에 끝나고, 스크립트는 응답이 도착하면
     * 큐 스레드에서 실행되며 load 이벤트가 그 뒤를 따른다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQueueModeLetsThePageAppearBeforeSlowAsyncScript() throws Exception {
        Page page = new Page();
        page.queue = new QueueScheduler();
        page.source.gated("slow.js", "console.log('async:' + document.readyState)");
        page.start("<script>document.addEventListener('DOMContentLoaded', function () { console.log('dcl'); });"
                + "window.addEventListener('load', function () { console.log('load:' + document.readyState); });</script>"
                + "<script async src=slow.js></script>");
        page.awaitFinished("the loading thread returned without waiting for the async script");
        Check.equal("dcl", page.joined(), "DOMContentLoaded fired, load did not");
        page.source.release("slow.js");
        runQueueUntilLog(page, "dcl,async:interactive,load:complete", "async script, then load, on the queue");
        String queueThread = Thread.currentThread().getName();
        Check.equal(queueThread, page.threads.get(1), "the async script ran on the queue thread");
        Check.equal(queueThread, page.threads.get(2), "load fired on the queue thread");
        page.scripts.dispose();
    }

    /**
     * P15-04-b: closing the page cancels the delivery to the queue; a late async response does not run and load does not fire.
     * <p>
     * P15-04-b: 페이지를 닫으면 큐로의 전달이 취소된다. 늦은 async 응답은 실행되지 않고 load 도 발생하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testQueueModeSuppressesLateAsyncAfterClosing() throws Exception {
        Page page = new Page();
        page.queue = new QueueScheduler();
        page.source.gated("slow.js", "console.log('async')");
        page.start("<script>window.addEventListener('load', function () { console.log('load'); });</script><script async src=slow.js></script>");
        page.awaitFinished("the loading thread returned");
        page.scripts.requestDisposal();
        page.source.release("slow.js");
        sleep(200);
        Runnable task;
        // 닫은 뒤에 큐에 올라온 작업이 있더라도 실행해 봐야 아무것도 일어나지 않아야 한다.
        while ((task = page.queue.tasks.poll()) != null) {
            task.run();
        }
        Check.equal("", page.joined(), "nothing ran after the page was closed");
        page.scripts.dispose();
    }

    /**
     * P15-04-b: scripts that a script inserts run when inserted (inline) or when fetched (external), a nested script does not run the
     * microtask checkpoint early, and load waits for the external one.
     * <p>
     * P15-04-b: 스크립트가 삽입한 스크립트는 삽입될 때(인라인) 또는 가져오는 대로(외부) 실행되고, 중첩된 스크립트는 마이크로태스크 검사점을 일찍
     * 실행하지 않으며, load 는 외부 스크립트를 기다린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDynamicScripts() throws Exception {
        Page page = new Page();
        page.queue = new QueueScheduler();
        page.source.gated("dyn.js", "console.log('external')");
        page.start("<body><script>window.addEventListener('load', function () { console.log('load'); });"
                + "Promise.resolve().then(function () { console.log('microtask'); });"
                + "var a = document.createElement('script'); a.text = \"console.log('inline')\"; document.body.appendChild(a);"
                + "var b = document.createElement('script'); b.src = 'dyn.js'; document.body.appendChild(b);"
                + "console.log('end of script');</script></body>");
        page.awaitFinished("the loading thread returned");
        Check.equal("inline,end of script,microtask", page.joined(),
                "the inline script ran at insertion, the microtask after the outer script");
        page.source.release("dyn.js");
        runQueueUntilLog(page, "inline,end of script,microtask,external,load", "the external script ran when fetched, then load");
        page.scripts.dispose();
    }

    /**
     * P15-04-b: dynamic scripts with <code>async = false</code> keep the order of insertion even if a later one is fetched first, and the
     * <code>async</code> property reflects the force-async default.
     * <p>
     * P15-04-b: <code>async = false</code> 인 동적 스크립트는 나중 것이 먼저 가져와져도 삽입 순서를 지키며, <code>async</code> 프로퍼티는 force-async
     * 기본값을 반영한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOrderedDynamicScripts() throws Exception {
        Page page = new Page();
        page.queue = new QueueScheduler();
        page.source.gated("o1.js", "console.log('o1')");
        page.source.gated("o2.js", "console.log('o2')");
        page.start("<body><script>var x = document.createElement('script'); console.log('default:' + x.async); x.async = false;"
                + "console.log('cleared:' + x.async);"
                + "var s1 = document.createElement('script'); s1.async = false; s1.src = 'o1.js'; document.body.appendChild(s1);"
                + "var s2 = document.createElement('script'); s2.async = false; s2.src = 'o2.js'; document.body.appendChild(s2);</script></body>");
        page.awaitFinished("the loading thread returned");
        page.source.release("o2.js");
        sleep(150);
        Runnable task;
        // 두 번째 스크립트의 가져오기가 먼저 끝났어도 첫 번째가 끝나기 전에는 실행되지 않아야 한다.
        while ((task = page.queue.tasks.poll()) != null) {
            task.run();
        }
        Check.equal("default:true,cleared:false", page.joined(), "the second script waits for the first one");
        page.source.release("o1.js");
        runQueueUntilLog(page, "default:true,cleared:false,o1,o2", "insertion order");
        page.scripts.dispose();
    }

    /**
     * P15-04-b: a dynamic script that a parser-blocking script inserts does not drop the text that the outer script wrote with
     * <code>document.write</code>.
     * <p>
     * P15-04-b: 파서를 막는 스크립트가 삽입한 동적 스크립트가 바깥 스크립트가 <code>document.write</code> 로 쓴 텍스트를 버리지 않는다.
     */
    public void testNestedDynamicScriptKeepsOuterDocumentWrite() {
        Page page = new Page();
        page.start("<body><script>document.write('<p id=w1>x</p>'); var s = document.createElement('script');"
                + "s.text = \"console.log('inner')\"; document.body.appendChild(s); document.write('<p id=w2>y</p>');</script>"
                + "<script>console.log(document.getElementById('w1') !== null && document.getElementById('w2') !== null);</script></body>");
        page.awaitFinished("the page finished");
        Check.equal("inner,true", page.joined(), "both written paragraphs exist");
    }
}
