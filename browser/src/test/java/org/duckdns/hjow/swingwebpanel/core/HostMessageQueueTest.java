package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.WebMessageListener;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Verifies Java host message ordering, exception isolation and backpressure.
 * <p>
 * Java 호스트 메시지의 순서, 리스너 오류 격리와 대기 한도를 검사한다.
 */
public final class HostMessageQueueTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HostMessageQueueTest() {
    }

    /**
     * A click and its Promise job finish before the EDT listener sees either message.
     * <p>
     * 클릭과 프라미스 작업이 끝난 뒤 EDT 리스너가 두 메시지를 받는다.
     *
     * @throws Exception on setup or waiting failure / 준비 또는 대기 실패 시
     */
    public void testClickMicrotasksAndBadListener() throws Exception {
        TestHttpServer site = new TestHttpServer();
        final SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        try {
            site.routeFixed("/click", TestHttpServer.Response.html("<p>Click test</p><script>"
                    + "const host=opener;window.addEventListener('click',()=>{"
                    + "host.postMessage('click','*');"
                    + "Promise.resolve().then(()=>{host.postMessage('promise','*');sessionStorage.setItem('phase','microtask');});"
                    + "sessionStorage.setItem('phase','handler');});sessionStorage.setItem('phase','ready');</script>"));
            site.start();
            final ArrayBlockingQueue<String> states = new ArrayBlockingQueue<String>(4);
            panel.addWebMessageListener(new WebMessageListener() {
                /** Throws during delivery while the next listener still receives the message. / 오류를 던져도 다음 리스너에 전달되어야 한다. */
                public void messageReceived(WebMessageEvent event) {
                    throw new AssertionError("listener fault for the isolation test");
                }
            });
            panel.addWebMessageListener(new WebMessageListener() {
                /** Captures the microtask state at EDT delivery. / EDT 전달 시 마이크로태스크 상태를 기록한다. */
                public void messageReceived(WebMessageEvent event) {
                    states.add(event.getData() + ":" + FrameTestSupport.stored(panel, "phase"));
                }
            });
            panel.setPage(site.url("/click"));
            FrameTestSupport.awaitStored(panel, "phase", "ready");
            FrameTestSupport.awaitProgress(panel, 100);
            FrameTestSupport.click(panel, 16, 16);
            Check.equal("click:microtask", states.poll(15, TimeUnit.SECONDS), "click delivery follows its Promise checkpoint");
            Check.equal("promise:microtask", states.poll(15, TimeUnit.SECONDS), "the next message keeps its send order");
        } finally {
            panel.dispose();
            site.stop();
        }
    }

    /**
     * Large numbers of small messages stay bounded by the queue count.
     * <p>
     * 작은 메시지가 많아져도 큐 대기 수를 제한한다.
     *
     * @throws Exception on setup or waiting failure / 준비 또는 대기 실패 시
     */
    public void testPendingCountLimit() throws Exception {
        int expected = WebMessageDispatcher.MAX_PENDING_EVENTS;
        exerciseFlood("i", expected + 200, expected);
    }

    /**
     * Larger messages stay bounded by the estimated total bytes.
     * <p>
     * 큰 메시지는 예상 총 바이트 한도를 적용한다.
     *
     * @throws Exception on setup or waiting failure / 준비 또는 대기 실패 시
     */
    public void testPendingSizeLimit() throws Exception {
        int expected = (int) (WebMessageDispatcher.MAX_PENDING_BYTES / (8L * (32768 + 1)));
        exerciseFlood("new ArrayBuffer(32768)", expected + 17, expected);
    }

    /**
     * Floods a component while EDT delivery is paused.
     * <p>
     * EDT 전달이 멈춰 있는 동안 컴포넌트에 여러 메시지를 보낸다.
     *
     * @param expression JavaScript payload expression / JavaScript 데이터 식
     * @param sent number of attempted posts / 시도할 메시지 수
     * @param delivered expected accepted posts / 접수될 메시지 수
     * @throws Exception on setup or waiting failure / 준비 또는 대기 실패 시
     */
    private static void exerciseFlood(String expression, int sent, int delivered) throws Exception {
        TestHttpServer site = new TestHttpServer();
        SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch received = new CountDownLatch(delivered);
        final AtomicInteger count = new AtomicInteger();
        final ArrayBlockingQueue<String> warning = new ArrayBlockingQueue<String>(2);
        try {
            site.routeFixed("/flood", TestHttpServer.Response.html("<script>const h=opener;for(let i=0;i<" + sent + ";i++){"
                    + "h.postMessage(" + expression + ",'*');}sessionStorage.setItem('flood-finished','yes');</script>"));
            site.start();
            panel.addWebMessageListener(new WebMessageListener() {
                /** Counts delivered callbacks. / 전달된 호출을 센다. */
                public void messageReceived(WebMessageEvent event) {
                    count.incrementAndGet();
                    received.countDown();
                }
            });
            panel.addConsoleListener(new ConsoleListener() {
                /** Records the overflow report. / 초과 알림을 기록한다. */
                public void consoleMessage(ConsoleEvent event) {
                    if (event.getKind() == ConsoleEvent.Kind.HOST_ERROR && event.getMessage().contains("web messages were dropped")) {
                        warning.offer(event.getMessage());
                    }
                }
            });
            SwingUtilities.invokeLater(new Runnable() {
                /** Stops EDT processing until the test has exercised the backlog. / 대기량을 시험하는 동안 EDT 처리를 멈춘다. */
                public void run() {
                    started.countDown();
                    try {
                        release.await(25, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            Check.isTrue(started.await(10, TimeUnit.SECONDS), "EDT pause started");
            panel.setPage(site.url("/flood"));
            FrameTestSupport.awaitStored(panel, "flood-finished", "yes");
            Check.equal(0, count.get(), "script finished while the EDT was paused");
            release.countDown();
            Check.isTrue(received.await(15, TimeUnit.SECONDS), "the allowed batch drained");
            String report = warning.poll(15, TimeUnit.SECONDS);
            Check.notNull(report, "overflow was reported to the console");
            Check.contains(report, (sent - delivered) + " web messages were dropped", "overflow count");
            Check.equal(delivered, count.get(), "only the allowed backlog is delivered");
        } finally {
            release.countDown();
            panel.dispose();
            site.stop();
        }
    }
}
