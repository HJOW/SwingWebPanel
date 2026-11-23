package org.duckdns.hjow.swingwebpanel.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of clean, dispose and thread-safety of {@link BrowserSession} (contract cases C-CLEAN and C-DISPOSE, plan items
 * P01-15, P01-16, P03-04, P03-07).
 * <p>
 * {@link BrowserSession} 의 clean, dispose, 스레드 안전성 시험(계약 사례 C-CLEAN, C-DISPOSE, 계획 P01-15, P01-16, P03-04, P03-07).
 */
public class LifecycleTest {

    /**
     * Loader used by the tests of this suite.
     * <p>
     * 이 시험들이 쓰는 로더.
     */
    private FakeLoader loader;

    /**
     * Notification recorder.
     * <p>
     * 통지 기록기.
     */
    private TestSupport.Recorder recorder;

    /**
     * Session under test.
     * <p>
     * 시험 대상 세션.
     */
    private BrowserSession session;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LifecycleTest() {
    }

    /**
     * Creates a fresh session before each case.
     * <p>
     * 사례마다 새 세션을 만든다.
     */
    public void setUp() {
        loader = new FakeLoader();
        recorder = new TestSupport.Recorder();
        session = new BrowserSession(loader, recorder);
    }

    /**
     * Disposes the session after each case.
     * <p>
     * 사례가 끝나면 세션을 폐기한다.
     */
    public void tearDown() {
        session.dispose();
    }

    /**
     * C-CLEAN-1..2: clean shows the blank page and keeps the settings.
     * <p>
     * C-CLEAN-1..2: clean 은 빈 페이지를 보여 주고 설정은 유지한다.
     */
    public void testCleanResetsPageAndKeepsSettings() {
        session.setUserAgent("Agent/9");
        Map<String, String> h = new HashMap<String, String>();
        h.put("X-Keep", "yes");
        session.addHeaders(h);
        session.setJavascriptEnabled(false);
        session.setPage("http://a.test/");
        TestSupport.settle(session);
        session.setContent("text", "text/plain");
        TestSupport.settle(session);
        session.clean();
        TestSupport.settle(session);
        Check.equal("about:blank", session.getPage(), "page");
        Check.equal("", session.getContent(), "content");
        Check.equal(1, session.getHistories().size(), "history has the blank entry only");
        Check.equal("about:blank", session.getHistories().get(0), "history entry");
        Check.equal(0, session.getHistoryIndex(), "index");
        Check.equal("Agent/9", session.getUserAgent(), "user agent is kept");
        Check.equal(1, session.getHeaders().size(), "headers are kept");
        Check.isFalse(session.isJavascriptEnabled(), "JavaScript setting is kept");
        Check.equal(100, session.getProgress(), "progress");
        Check.equal("", session.getStatusText(), "status text");
    }

    /**
     * C-CLEAN-3: clean cancels a running load; a late completion cannot bring the old page back.
     * <p>
     * C-CLEAN-3: clean 은 진행 중인 읽기를 취소하고, 늦은 완료가 이전 페이지를 되살릴 수 없다.
     */
    public void testCleanCancelsRunningLoad() {
        loader.ignoreCancel = true;
        loader.gate("http://slow.test/");
        session.setPage("http://slow.test/");
        session.clean();
        loader.release("http://slow.test/");
        TestSupport.settle(session);
        Check.equal("about:blank", session.getPage(), "the cancelled load must not appear");
        Check.equal(1, session.getHistories().size(), "history stays reset");
    }

    /**
     * The page that was visible is disposed by clean.
     * <p>
     * 화면에 있던 페이지는 clean 으로 해제된다.
     */
    public void testCleanDisposesVisiblePage() {
        session.setPage("http://a.test/");
        TestSupport.settle(session);
        int before = loader.disposed.get();
        session.clean();
        Check.equal(before + 1, loader.disposed.get(), "visible page disposed once");
    }

    /**
     * C-DISPOSE-1..3: dispose twice is safe; changing methods then throw, query methods return the empty state.
     * <p>
     * C-DISPOSE-1..3: dispose 를 두 번 호출해도 안전하다. 이후 변경 메서드는 예외, 조회 메서드는 빈 상태를 반환한다.
     */
    public void testDisposeIsIdempotentAndLeavesEmptyState() {
        session.setUserAgent("Agent/1");
        Map<String, String> h = new HashMap<String, String>();
        h.put("X-A", "1");
        session.addHeaders(h);
        session.setPage("http://a.test/");
        TestSupport.settle(session);
        session.dispose();
        session.dispose();
        Check.isTrue(session.isDisposed(), "disposed");
        Check.equal("about:blank", session.getPage(), "page");
        Check.equal("", session.getContent(), "content");
        Check.equal(1, session.getHistories().size(), "history");
        Check.equal(100, session.getProgress(), "progress");
        Check.equal("", session.getStatusText(), "status text");
        Check.isTrue(session.getHeaders().isEmpty(), "headers are forgotten");
        Check.equal("Agent/1", session.getUserAgent(), "the last user agent is still reported");
        Block[] mutators = { new Block() {
            /**
             * Calls setPage on a disposed session.
             * <p>
             * 폐기된 세션에서 setPage 를 호출한다.
             */
            public void run() {
                session.setPage("http://a.test/");
            }
        }, new Block() {
            /**
             * Calls setContent on a disposed session.
             * <p>
             * 폐기된 세션에서 setContent 를 호출한다.
             */
            public void run() {
                session.setContent("x", "text/plain");
            }
        }, new Block() {
            /**
             * Calls back on a disposed session.
             * <p>
             * 폐기된 세션에서 back 을 호출한다.
             */
            public void run() {
                session.back(1);
            }
        }, new Block() {
            /**
             * Calls refresh on a disposed session.
             * <p>
             * 폐기된 세션에서 refresh 를 호출한다.
             */
            public void run() {
                session.refresh(false);
            }
        }, new Block() {
            /**
             * Calls setUserAgent on a disposed session.
             * <p>
             * 폐기된 세션에서 setUserAgent 를 호출한다.
             */
            public void run() {
                session.setUserAgent("x");
            }
        }, new Block() {
            /**
             * Calls clean on a disposed session.
             * <p>
             * 폐기된 세션에서 clean 을 호출한다.
             */
            public void run() {
                session.clean();
            }
        } };
        // 모든 변경 메서드는 IllegalStateException 이어야 한다.
        for (int i = 0; i < mutators.length; i++) {
            Check.throwsException(IllegalStateException.class, mutators[i], "mutator #" + i + " after dispose");
        }
    }

    /**
     * C-DISPOSE-4: dispose cancels a running load; a late completion changes nothing and nobody is notified afterwards.
     * <p>
     * C-DISPOSE-4: dispose 는 진행 중인 읽기를 취소하고, 늦은 완료는 아무것도 바꾸지 못하며 이후 통지도 없다.
     */
    public void testDisposeCancelsRunningLoadAndStopsNotifications() {
        loader.ignoreCancel = true;
        loader.gate("http://slow.test/");
        session.setPage("http://slow.test/");
        TestSupport.flushEdt();
        session.dispose();
        TestSupport.flushEdt();
        int seen = recorder.events.size();
        loader.release("http://slow.test/");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        TestSupport.flushEdt();
        Check.equal(seen, recorder.events.size(), "no notification after dispose");
        Check.equal("about:blank", session.getPage(), "page");
    }

    /**
     * P03-07: calls from many threads at once never throw and leave a consistent confirmed state.
     * <p>
     * P03-07: 여러 스레드가 동시에 호출해도 예외가 없고 확정된 상태가 일관되게 남는다.
     *
     * @throws Exception if a worker thread cannot be joined / 작업 스레드를 기다릴 수 없을 때
     */
    public void testConcurrentCallsKeepStateConsistent() throws Exception {
        final int threads = 8;
        final List<Throwable> errors = java.util.Collections.synchronizedList(new ArrayList<Throwable>());
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        // 스레드마다 서로 다른 난수열로 여러 종류의 호출을 섞어 실행한다.
        for (int t = 0; t < threads; t++) {
            final int seed = t;
            Thread th = new Thread(new Runnable() {
                /**
                 * Runs a random mix of public calls.
                 * <p>
                 * 공개 호출을 무작위로 섞어 실행한다.
                 */
                public void run() {
                    Random rnd = new Random(seed);
                    try {
                        start.await();
                        // 호출을 150 번 무작위로 섞어 실행한다.
                        for (int i = 0; i < 150; i++) {
                            switch (rnd.nextInt(8)) {
                            case 0:
                                session.setPage("http://t" + rnd.nextInt(4) + ".test/");
                                break;
                            case 1:
                                session.setContent("c" + i, "text/html");
                                break;
                            case 2:
                                session.back(1 + rnd.nextInt(2));
                                break;
                            case 3:
                                session.forward(1 + rnd.nextInt(2));
                                break;
                            case 4:
                                session.refresh(rnd.nextBoolean());
                                break;
                            case 5:
                                session.getHistories();
                                session.getPage();
                                session.getContent();
                                break;
                            case 6:
                                session.getProgress();
                                session.getStatusText();
                                break;
                            default:
                                session.stop();
                                break;
                            }
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        done.countDown();
                    }
                }
            }, "stress-" + t);
            th.start();
        }
        start.countDown();
        Check.isTrue(done.await(60, TimeUnit.SECONDS), "all threads must finish");
        TestSupport.settle(session);
        Check.isTrue(errors.isEmpty(), "no exception from concurrent calls: " + errors);
        int index = session.getHistoryIndex();
        List<String> urls = session.getHistories();
        Check.isTrue(index >= 0 && index < urls.size(), "history index inside the history");
        Check.equal(urls.get(index), session.getPage(), "the confirmed page is the current history entry");
    }

    /**
     * P03-07: creating and disposing many sessions does not leave loader threads behind.
     * <p>
     * P03-07: 세션을 많이 만들고 폐기해도 읽기 스레드가 남지 않는다.
     */
    public void testRepeatedCreateAndDisposeLeavesNoThreads() {
        // 세션을 반복해서 만들고, 읽고, 폐기한다.
        for (int i = 0; i < 100; i++) {
            BrowserSession s = new BrowserSession(new FakeLoader(), null);
            s.setPage("http://t.test/" + i);
            s.dispose();
        }
        long end = System.currentTimeMillis() + 30000;
        int count = loaderThreads();
        // 스레드가 정리될 시간을 주며 기다린다.
        while (count > 2 && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            count = loaderThreads();
        }
        Check.isTrue(count <= 2, "loader threads left after disposing all sessions: " + count + describeLoaderThreads());
    }


    /**
     * Describes up to two live loader threads (state and top stack frames) for failure messages.
     * <p>
     * 실패 메시지용으로 살아 있는 읽기 스레드 최대 두 개의 상태와 스택 위쪽 프레임을 설명한다.
     *
     * @return description / 설명
     */
    private String describeLoaderThreads() {
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        // 살아 있는 읽기 스레드 중 앞의 둘만 스택과 함께 적는다.
        for (java.util.Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
            Thread t = e.getKey();
            if (t.isAlive() && t.getName().startsWith("swingwebpanel-") && shown < 2) {
                shown++;
                sb.append(" [").append(t.getName()).append(' ').append(t.getState());
                StackTraceElement[] st = e.getValue();
                // 스택의 위쪽 다섯 프레임만 적는다.
                for (int i = 0; i < st.length && i < 5; i++) {
                    sb.append(" | ").append(st[i]);
                }
                sb.append(']');
            }
        }
        return sb.toString();
    }
    /**
     * Counts live loader threads of this library.
     * <p>
     * 이 라이브러리의 살아 있는 읽기 스레드 수를 센다.
     *
     * @return number of threads / 스레드 수
     */
    private int loaderThreads() {
        int n = 0;
        // 모든 스레드 중 이름이 읽기 스레드 규칙에 맞는 것을 센다.
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("swingwebpanel-")) {
                n++;
            }
        }
        return n;
    }
}
