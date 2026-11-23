package org.duckdns.hjow.swingwebpanel.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the document-queue/EDT bridge: viewport reflow, connected DOM changes, coalescing and stale notifications (P09-07-a).
 * <p>문서 큐와 EDT의 연결 시험: 뷰포트 재배치, 연결된 DOM 변경, 요청 합치기와 오래된 통지 폐기.
 */
public class PageViewTest {
    /** Creates the test suite. / 시험 스위트를 만든다. */
    public PageViewTest() {
    }

    /** Records EDT callbacks and owns one isolated view. / EDT 콜백을 기록하고 독립 뷰 하나를 소유한다. */
    private static final class Fixture implements PageView.Host {
        /** Isolated browser session. / 독립 브라우저 세션. */
        final BrowserSession session = new BrowserSession(new ParsingPageLoader(new DefaultPageLoader()), null);
        /** Published scroll dimensions. / 게시된 스크롤 크기. */
        final ViewMetrics metrics = new ViewMetrics();
        /** View under test. / 시험할 뷰. */
        final PageView view = new PageView(session, metrics, this);
        /** Count of notifications. / 통지 횟수. */
        volatile int notifications;
        /** Thread condition of every notification. / 모든 통지의 스레드 조건. */
        volatile boolean edt = true;

        /** Creates an empty fixture. / 빈 시험 환경을 만든다. */
        Fixture() {
        }

        /** Records a notification on the EDT. / EDT 통지를 기록한다. */
        public void viewChanged() {
            edt &= SwingUtilities.isEventDispatchThread();
            notifications++;
        }

        /**
         * Loads and publishes a document, then drains the queues.
         * <p>문서를 읽고 게시한 뒤 큐가 끝날 때까지 기다린다.
         * @param html page source / 페이지 원문
         * @throws Exception on EDT failure / EDT 실패 시
         */
        void open(String html) throws Exception {
            session.setContent(html, "text/html");
            TestSupport.settle(session);
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Installs the page on the EDT. / EDT에서 페이지를 설치한다. */
                public void run() {
                    view.pageConfirmed(session.getSnapshot());
                }
            });
            TestSupport.settle(session);
            Check.notNull(view.getRenderedPage(), "rendered page");
        }

        /** Closes the view before shutting its document queue. / 문서 큐 종료 전에 뷰를 닫는다. */
        void close() {
            view.dispose();
            session.awaitIdle(10000);
            session.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * Narrower viewports wrap text and equal extents do not trigger a layout/repaint loop.
     * <p>좁은 뷰포트에서는 텍스트가 줄바꿈되며 같은 범위는 재배치·그리기 반복을 유발하지 않는다.
     * @throws Exception on failure / 실패 시
     */
    public void testReflowAndEqualExtent() throws Exception {
        Fixture f = new Fixture();
        try {
            f.view.setViewport(400, 200);
            f.open("<!doctype html><body style='margin:0'><p style='margin:0'>"
                    + "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen</p>");
            int wideHeight = f.metrics.getHeight();
            f.view.setViewport(100, 200);
            TestSupport.settle(f.session);
            Check.isTrue(f.metrics.getHeight() > wideHeight, "narrow viewport increases height");
            RenderedPage same = f.view.getRenderedPage();
            int callbacks = f.notifications;
            // 같은 범위를 반복 전달해도 배치 결과와 콜백 수는 변하지 않아야 한다.
            for (int i = 0; i < 50; i++) {
                f.view.setViewport(100, 200);
            }
            TestSupport.settle(f.session);
            Check.isTrue(f.view.getRenderedPage() == same, "no redundant layout");
            Check.equal(callbacks, f.notifications, "no repeated callbacks");
            Check.isTrue(f.edt, "all callbacks on EDT");
        } finally {
            f.close();
        }
    }

    /**
     * Connected style/DOM changes reflow while detached edits leave the rendered page alone; received source remains intact.
     * <p>연결된 스타일·DOM 변경은 재배치하고 분리된 편집은 화면을 바꾸지 않는다. 수신 원문은 그대로 보존한다.
     * @throws Exception on failure / 실패 시
     */
    public void testDomAndStyleChanges() throws Exception {
        final Fixture f = new Fixture();
        try {
            String source = "<!doctype html><style>#x{height:20px}</style><div id=x>text</div>";
            f.open(source);
            int before = f.metrics.getHeight();
            f.session.submitDocumentTask(new Runnable() {
                /** Changes the connected style on the owning document queue. / 소유 문서 큐에서 연결된 스타일을 바꾼다. */
                public void run() {
                    Document doc = ((HtmlPageDocument) f.session.getSnapshot().getDocument()).getDocument();
                    doc.getElementById("x").setAttribute("style", "height:700px");
                }
            });
            TestSupport.settle(f.session);
            Check.isTrue(f.metrics.getHeight() > before + 600, "connected style caused reflow");
            Check.equal(source, f.session.getContent(), "source preserved");
            final RenderedPage same = f.view.getRenderedPage();
            f.session.submitDocumentTask(new Runnable() {
                /** Edits a detached node that has no display box. / 표시 상자가 없는 분리된 노드를 편집한다. */
                public void run() {
                    Document doc = ((HtmlPageDocument) f.session.getSnapshot().getDocument()).getDocument();
                    doc.createElement("p").setTextContent("detached");
                }
            });
            TestSupport.settle(f.session);
            Check.isTrue(same == f.view.getRenderedPage(), "detached edit does not relayout");
        } finally {
            f.close();
        }
    }

    /**
     * Rendering waits on the document queue without blocking EDT delivery; obsolete page notifications cannot replace the new page.
     * <p>렌더링은 문서 큐에서 대기하며 EDT 통지를 막지 않는다. 오래된 페이지 통지는 새 페이지를 대체하지 못한다.
     * @throws Exception on failure / 실패 시
     */
    public void testQueueAndStaleNotifications() throws Exception {
        final Fixture f = new Fixture();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        try {
            f.open("<div style='height:80px'>old</div>");
            final PageSnapshot old = f.session.getSnapshot();
            f.session.submitDocumentTask(new Runnable() {
                /** Holds the document queue, leaving Swing free. / Swing은 자유롭게 두고 문서 큐를 대기시킨다. */
                public void run() {
                    started.countDown();
                    try {
                        release.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            Check.isTrue(started.await(5, TimeUnit.SECONDS), "document blocker started");
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Queues rendering while the document queue is held. / 문서 큐 대기 중 렌더링을 예약한다. */
                public void run() {
                    f.view.setViewport(200, 100);
                }
            });
            release.countDown();
            TestSupport.settle(f.session);
            f.open("<div style='height:700px'>new</div>");
            final RenderedPage latest = f.view.getRenderedPage();
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Delivers an obsolete completion. / 오래된 완료를 전달한다. */
                public void run() {
                    f.view.pageConfirmed(old);
                }
            });
            Check.isTrue(latest == f.view.getRenderedPage(), "old notification ignored");
            f.session.clean();
            Check.isNull(f.view.getRenderedPage(), "clean invalidates old drawing immediately");
            f.view.clear();
            TestSupport.settle(f.session);
            Check.equal(0, f.metrics.getHeight(), "blank scroll height");
        } finally {
            release.countDown();
            f.close();
        }
    }
}
