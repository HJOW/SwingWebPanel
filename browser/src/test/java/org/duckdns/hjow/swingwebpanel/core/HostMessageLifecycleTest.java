package org.duckdns.hjow.swingwebpanel.core;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.WebMessageListener;
import org.duckdns.hjow.swingwebpanel.core.HostInboundMessageTest.Inbox;
import org.duckdns.hjow.swingwebpanel.core.frames.FrameTestSupport;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;
import org.duckdns.hjow.swingwebpanel.js.host.PageScripts;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Tests the lifetime of Java web messages and their weak source links.
 * <p>
 * Java 웹 메시지의 수명 및 약한 원천 연결을 검사한다.
 */
public final class HostMessageLifecycleTest {

    /** Creates the suite. / 시험 묶음을 만든다. */
    public HostMessageLifecycleTest() {
    }

    /**
     * Verifies listener registration, queuing across reset and cancellation of stale replies.
     * <p>
     * 리스너 등록, 초기화 전후 큐와 오래된 응답의 취소를 확인한다.
     *
     * @throws Exception on setup or waiting failure / 환경 구성 또는 대기 실패 시
     */
    public void testCleanDisableAndDispose() throws Exception {
        TestHttpServer site = new TestHttpServer();
        final SwingWebPanel panel = FrameTestSupport.newPanel(320, 240);
        final Inbox inbox = new Inbox();
        final CountDownLatch edtPaused = new CountDownLatch(1);
        final CountDownLatch edtRelease = new CountDownLatch(1);
        try {
            String body = "<script>const h=opener;h.postMessage('before registration','*');"
                    + "onmessage=e=>{h.postMessage(e.data,'*');sessionStorage.setItem('delivery-state',String(e.data));};"
                    + "sessionStorage.setItem('ready',location.pathname);</script>";
            site.routeFixed("/before", TestHttpServer.Response.html(body));
            site.routeFixed("/disabled", TestHttpServer.Response.html(body));
            site.routeFixed("/after", TestHttpServer.Response.html(body));
            site.start();
            panel.setPage(site.url("/before"));
            FrameTestSupport.awaitStored(panel, "ready", "/before");
            panel.addWebMessageListener(inbox);
            panel.addWebMessageListener(inbox);
            panel.removeWebMessageListener(inbox);
            panel.removeHyperlinkEvents();
            WebMessageListener[] listeners = panel.getWebMessageListeners();
            Check.equal(1, listeners.length, "one duplicate registration was removed");
            listeners[0] = null;
            Check.same(inbox, panel.getWebMessageListeners()[0], "the public array is a copy");
            panel.postMessage("retained");
            WebMessageEvent retained = inbox.next();
            Check.equal("retained", retained.getData(), "messages sent before registration were discarded");

            SwingUtilities.invokeLater(new Runnable() {
                /** Holds back the EDT to leave a notification pending. / 통지를 대기 상태로 두기 위해 EDT를 막는다. */
                public void run() {
                    edtPaused.countDown();
                    try {
                        edtRelease.await(25, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            Check.isTrue(edtPaused.await(10, TimeUnit.SECONDS), "EDT is held for clean");
            panel.postMessage("queued");
            FrameTestSupport.awaitStored(panel, "delivery-state", "queued");
            panel.clean();
            edtRelease.countDown();
            Check.same(inbox, panel.getWebMessageListeners()[0], "clean retains the message listener");
            panel.setJavascriptEnabled(false);
            panel.setPage(site.url("/disabled"));
            FrameTestSupport.awaitProgress(panel, 100);
            panel.postMessage("disabled");
            Check.isNull(FrameTestSupport.stored(panel, "ready"), "disabled scripts did not send");
            panel.setJavascriptEnabled(true);
            panel.setPage(site.url("/after"));
            FrameTestSupport.awaitStored(panel, "ready", "/after");
            Check.equal("before registration", inbox.next().getData(), "the new document is the next sender");
            retained.reply("stale");
            panel.postMessage("alive");
            Check.equal("alive", inbox.next().getData(), "pending and stale messages never reach the replacement");
        } finally {
            edtRelease.countDown();
            panel.dispose();
            site.stop();
        }
        Check.equal(0, panel.getWebMessageListeners().length, "dispose removes listeners");
        Check.throwsException(IllegalStateException.class, new Block() {
            public void run() {
                panel.addWebMessageListener(inbox);
            }
        }, "dispose forbids a new registration");
    }

    /**
     * Ensures that an event retained by Java does not keep the component reachable.
     * <p>
     * Java가 이벤트를 보관해도 컴포넌트 수거를 막지 않는지 확인한다.
     *
     * @throws Exception on setup or waiting failure / 환경 구성 또는 대기 실패 시
     */
    public void testRetainedEventDoesNotKeepComponent() throws Exception {
        HeldEvent held = retainOldEvent();
        awaitCollected(held.panel);
        Check.isNull(held.event.getSource(), "source getter reflects collection");
        Check.equal("remember", held.event.getData(), "message data remains available");
        Check.equal("null", held.event.getOrigin(), "setContent has an opaque origin");
        held.event.reply("expired");
    }

    /**
     * Ensures that the retained host object does not keep an old script realm alive.
     * <p>
     * 보관한 호스트 객체가 이전 스크립트 렐름의 수거를 막지 않는지 확인한다.
     *
     * @throws Exception on collection wait failure / 수거 대기 실패 시
     */
    public void testRetainedHostWindowDoesNotKeepDocument() throws Exception {
        HeldWindow held = retainOldWindow();
        awaitCollected(held.page);
        Check.equal("true", Ops.toString(held.window.get("closed")), "disposed host window still reports closed");
    }

    /**
     * Retains an actual component event after disposing the sender.
     * <p>
     * 송신 컴포넌트를 폐기하고 실제 이벤트를 보관한다.
     *
     * @return held event and weak component / 보관 이벤트와 약한 컴포넌트
     * @throws Exception on setup or waiting failure / 환경 구성 또는 대기 실패 시
     */
    private static HeldEvent retainOldEvent() throws Exception {
        SwingWebPanel panel = FrameTestSupport.newPanel(160, 120);
        try {
            Inbox inbox = new Inbox();
            panel.addWebMessageListener(inbox);
            panel.setContent("<script>opener.postMessage('remember','*');</script>", "text/html");
            return new HeldEvent(inbox.next(), panel);
        } finally {
            panel.dispose();
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Lets earlier EDT jobs release the component. / 이전 EDT 작업이 컴포넌트를 놓도록 한다. */
                public void run() {
                }
            });
        }
    }

    /**
     * Retains an opener object after closing its script host.
     * <p>
     * 스크립트 호스트를 닫고 opener 객체를 보관한다.
     *
     * @return held object and weak page / 보관 객체와 약한 페이지
     */
    private static HeldWindow retainOldWindow() {
        PageScripts page = new PageScripts(Document.createHtmlDocument(), new HostEnvironment() {
            /** Uses the same host origin as a real component. / 실제 컴포넌트와 같은 호스트 오리진을 사용한다. */
            public String getHostMessageOrigin() {
                return SwingWebPanel.HOST_ORIGIN;
            }
        });
        JsObject opener = (JsObject) page.getRealm().getGlobalObject().get("opener");
        Check.equal("function", page.evaluate("typeof opener.postMessage", "warm"), "the object cached a method for this realm");
        page.dispose();
        Check.equal("true", Ops.toString(opener.get("closed")), "a disposed page closes the host identity");
        return new HeldWindow(opener, page);
    }

    /**
     * Waits for the weak target to be reclaimed.
     * <p>
     * 약한 대상을 수거할 때까지 기다린다.
     *
     * @param reference weak target / 약한 대상
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    private static void awaitCollected(WeakReference<?> reference) throws InterruptedException {
        long deadline = System.nanoTime() + 10000000000L;
        // 대기 중이던 해제 작업도 끝날 시간을 주며 수거를 확인한다.
        while (reference.get() != null && System.nanoTime() - deadline < 0L) {
            System.gc();
            Thread.sleep(25);
        }
        Check.isNull(reference.get(), "retained message data does not hold a live document");
    }

    /** Event held independently of its component. / 컴포넌트와 별개로 보관한 이벤트. */
    private static final class HeldEvent {
        /** Retained event. / 보관 이벤트. */
        final WebMessageEvent event;
        /** Weak source reference. / 약한 원천 참조. */
        final WeakReference<SwingWebPanel> panel;

        /** Captures the event but only a weak source. / 이벤트와 약한 원천을 확보한다. */
        HeldEvent(WebMessageEvent event, SwingWebPanel panel) {
            this.event = event;
            this.panel = new WeakReference<SwingWebPanel>(panel);
        }
    }

    /** Host object held independently of its realm. / 렐름과 별개로 보관한 호스트 객체. */
    private static final class HeldWindow {
        /** Retained host object. / 보관한 호스트 객체. */
        final JsObject window;
        /** Weak page reference. / 약한 페이지 참조. */
        final WeakReference<PageScripts> page;

        /** Captures the object but only a weak page. / 객체와 약한 페이지를 확보한다. */
        HeldWindow(JsObject window, PageScripts page) {
            this.window = window;
            this.page = new WeakReference<PageScripts>(page);
        }
    }
}
