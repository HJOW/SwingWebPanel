package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Link input policy tests using real AWT event dispatch, DOM listeners and the common navigation controller (P09-08-a).
 * <p>실제 AWT 이벤트 전달·DOM 리스너·공통 이동 제어기를 사용하는 링크 입력 정책 시험.
 */
public class PageInputTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public PageInputTest() {
    }

    /** Isolated rendered session with a fake transport and real pointer listener registration. / 가짜 전송기와 실제 포인터 리스너 등록을 갖춘 독립 렌더링 세션. */
    private static final class Fixture implements PageView.Host {
        /** Real AWT event source. / 실제 AWT 이벤트 출처. */
        final JPanel component = new JPanel();
        /** Navigation transport recording every request. / 모든 요청을 기록하는 이동 전송기. */
        final FakeLoader loader = new FakeLoader();
        /** Owning session. / 소유 세션. */
        final BrowserSession session = new BrowserSession(new ParsingPageLoader(loader), null);
        /** User hyperlink listeners. / 사용자 하이퍼링크 리스너. */
        final HyperlinkDispatcher links = new HyperlinkDispatcher();
        /** View built on the document queue. / 문서 큐에서 구성하는 뷰. */
        final PageView view = new PageView(session, new ViewMetrics(), this);
        /** Input controller under test. / 시험할 입력 제어기. */
        final PageInput input = new PageInput(component, session, view, links);
        /** Ordered observations across both queues. / 두 큐에 걸친 순서 관측값. */
        final List<String> order = Collections.synchronizedList(new ArrayList<String>());
        /** Last Swing event. / 마지막 Swing 이벤트. */
        volatile HyperlinkEvent last;

        /** Registers real mouse listeners and event observations. / 실제 마우스 리스너와 이벤트 관측을 등록한다. */
        Fixture() {
            component.addMouseListener(input);
            component.addMouseMotionListener(input);
            links.add(new HyperlinkListener() {
                /** Records event order and verifies EDT notification. / 이벤트 순서를 기록하고 EDT 통지를 확인한다.
                 * @param event link event / 링크 이벤트 */
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    Check.isTrue(SwingUtilities.isEventDispatchThread(), "Swing notification on EDT");
                    last = event;
                    order.add(event.getEventType().toString());
                }
            });
        }

        /** Receives view updates without creating a window. / 창을 만들지 않고 뷰 갱신을 받는다. */
        public void viewChanged() {
        }

        /**
         * Publishes a link document with a predictable box and optional DOM click listener.
         * <p>예측 가능한 상자와 선택적 DOM click 리스너를 가진 링크 문서를 게시한다.
         * @param href target address / 대상 주소
         * @param listener optional DOM listener / 선택적 DOM 리스너
         * @throws Exception on failure / 실패 시
         */
        void open(String href, final EventListener listener) throws Exception {
            session.setContent("<!doctype html><base href='http://links.test/'><a id=a href='" + href
                    + "' style='display:block;width:100px;height:40px'><span>link</span></a>", "text/html");
            TestSupport.settle(session);
            if (listener != null) {
                session.submitDocumentTask(new Runnable() {
                    /** Registers a DOM handler on the owning document queue. / 소유 문서 큐에서 DOM 처리기를 등록한다. */
                    public void run() {
                        document().getElementById("a").addEventListener("click", listener, false);
                    }
                });
                TestSupport.settle(session);
            }
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Publishes the initial view on the EDT. / EDT에서 초기 뷰를 게시한다. */
                public void run() {
                    view.pageConfirmed(session.getSnapshot());
                }
            });
            TestSupport.settle(session);
            Check.notNull(view.getRenderedPage(), "link page rendered");
            order.clear();
        }

        /** Returns the current DOM while the document queue is idle or owned. / 문서 큐가 유휴이거나 소유한 동안 현재 DOM을 반환한다.
         * @return current DOM / 현재 DOM */
        Document document() {
            return ((HtmlPageDocument) session.getSnapshot().getDocument()).getDocument();
        }

        /**
         * Dispatches a real component MouseEvent at the laid-out link, then drains document/EDT/navigation work.
         * <p>배치된 링크 위치에서 실제 컴포넌트 MouseEvent를 전달하고 문서·EDT·이동 작업을 처리한다.
         * @param type AWT event ID / AWT 이벤트 ID
         * @throws Exception on failure / 실패 시
         */
        void pointer(final int type) throws Exception {
            Element link = document().getElementById("a");
            LayoutBox box = view.getRenderedPage().getLayout().getBoxes(link).get(0);
            final int x = (int) box.getX() + 5;
            final int y = (int) box.getY() + 5;
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Delivers the pointer through Component.dispatchEvent. / Component.dispatchEvent로 포인터를 전달한다. */
                public void run() {
                    component.dispatchEvent(new MouseEvent(component, type, System.currentTimeMillis(), 0, x, y,
                            type == MouseEvent.MOUSE_CLICKED ? 1 : 0, false,
                            type == MouseEvent.MOUSE_CLICKED ? MouseEvent.BUTTON1 : MouseEvent.NOBUTTON));
                }
            });
            TestSupport.settle(session);
            TestSupport.settle(session);
        }

        /** Closes input, view and session in ownership order. / 입력·뷰·세션을 소유 순서대로 닫는다. */
        void close() {
            input.dispose();
            view.dispose();
            session.awaitIdle(10000);
            session.dispose();
            links.clear();
            TestSupport.flushEdt();
        }
    }

    /**
     * Hover reports full addresses on the EDT, restores status on exit and uses null URL for classpath addresses.
     * <p>호버가 EDT에서 전체 주소를 알리고 이탈 시 상태를 복원하며 classpath 주소는 URL 객체 없이 전달한다.
     * @throws Exception on failure / 실패 시
     */
    public void testHoverStatusAndSpecialUrl() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open("classpath://pages/next.html", null);
            String status = f.session.getStatusText();
            f.pointer(MouseEvent.MOUSE_MOVED);
            Check.equal(HyperlinkEvent.EventType.ENTERED, f.last.getEventType(), "entered");
            Check.isNull(f.last.getURL(), "classpath has no URL handler");
            Check.equal("classpath://pages/next.html", f.last.getDescription(), "special address preserved");
            Check.equal(f.last.getDescription(), f.session.getStatusText(), "hover status");
            f.pointer(MouseEvent.MOUSE_EXITED);
            Check.equal(HyperlinkEvent.EventType.EXITED, f.last.getEventType(), "exited");
            Check.equal(status, f.session.getStatusText(), "underlying status restored");
            Check.equal(2, f.order.size(), "one enter and one exit");
        } finally {
            f.close();
        }
    }

    /**
     * DOM click runs before Swing ACTIVATED; preventDefault keeps URL and history unchanged while notifying the host.
     * <p>DOM click이 Swing ACTIVATED보다 먼저 실행된다. preventDefault는 호스트에 알리면서 URL과 이력을 보존한다.
     * @throws Exception on failure / 실패 시
     */
    public void testDomCancellationAndNotificationOrder() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open("target", new EventListener() {
                /** Cancels the default action on the document queue. / 문서 큐에서 기본 동작을 취소한다.
                 * @param event DOM click / DOM click */
                public void handleEvent(Event event) {
                    Check.isTrue(!SwingUtilities.isEventDispatchThread(), "DOM listener off EDT");
                    Check.isTrue(event.isTrusted(), "user agent click is trusted");
                    f.order.add("DOM");
                    event.preventDefault();
                }
            });
            int histories = f.session.getHistories().size();
            f.pointer(MouseEvent.MOUSE_CLICKED);
            Check.equal("DOM", f.order.get(0), "DOM first");
            Check.equal("ACTIVATED", f.order.get(1), "Swing second");
            Check.equal("about:text", f.session.getPage(), "cancelled navigation");
            Check.equal(histories, f.session.getHistories().size(), "cancelled history unchanged");
        } finally {
            f.close();
        }
    }

    /**
     * A listener's explicit navigation wins; clearing listeners still leaves exactly one internal navigation.
     * <p>리스너의 명시적 이동이 우선하며 리스너를 모두 지워도 내부 이동은 정확히 한 번 남는다.
     * @throws Exception on failure / 실패 시
     */
    public void testListenerNavigationAndRemoval() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open("target", null);
            f.links.add(new HyperlinkListener() {
                /** Starts a competing navigation from the user listener. / 사용자 리스너에서 우선할 이동을 시작한다.
                 * @param event Swing link event / Swing 링크 이벤트 */
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        f.session.setPage("http://links.test/listener");
                    }
                }
            });
            long before = f.session.getNavigationCount();
            f.pointer(MouseEvent.MOUSE_CLICKED);
            Check.equal("http://links.test/listener", f.session.getPage(), "listener navigation wins");
            Check.equal(before + 1, f.session.getNavigationCount(), "no stale default request");
            f.open("target", null);
            f.links.clear();
            before = f.session.getNavigationCount();
            f.pointer(MouseEvent.MOUSE_CLICKED);
            Check.equal("http://links.test/target", f.session.getPage(), "default navigation survives listener removal");
            Check.equal(before + 1, f.session.getNavigationCount(), "one default request");
        } finally {
            f.close();
        }
    }

    /**
     * The default action uses the href after DOM dispatch, and DOM-initiated navigation also suppresses the old default.
     * <p>기본 동작이 DOM 전달 후 href를 쓰며 DOM에서 시작한 이동도 이전 기본 이동을 억제한다.
     * @throws Exception on failure / 실패 시
     */
    public void testDomChangesHrefOrNavigates() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open("old", new EventListener() {
                /** Changes href during dispatch. / 전달 중 href를 바꾼다.
                 * @param event DOM event / DOM 이벤트 */
                public void handleEvent(Event event) {
                    f.document().getElementById("a").setAttribute("href", "changed");
                }
            });
            f.pointer(MouseEvent.MOUSE_CLICKED);
            Check.equal("http://links.test/changed", f.session.getPage(), "updated href used");
            f.open("old", new EventListener() {
                /** Starts a navigation during DOM dispatch. / DOM 전달 중 이동을 시작한다.
                 * @param event DOM event / DOM 이벤트 */
                public void handleEvent(Event event) {
                    f.session.setPage("http://links.test/dom");
                }
            });
            long before = f.session.getNavigationCount();
            f.pointer(MouseEvent.MOUSE_CLICKED);
            Check.equal("http://links.test/dom", f.session.getPage(), "DOM navigation wins");
            Check.equal(before + 1, f.session.getNavigationCount(), "DOM navigation not followed by stale default");
        } finally {
            f.close();
        }
    }

    /**
     * Unsupported schemes notify the host but never enter the navigation loader.
     * <p>미지원 스키마는 호스트에 알리지만 이동 로더에 진입하지 않는다.
     * @throws Exception on failure / 실패 시
     */
    public void testBlockedSchemes() throws Exception {
        Fixture f = new Fixture();
        try {
            String[] urls = {"javascript:alert(1)", "file:///secret", "mailto:user@example.test", "data:text/html,x", "about:text"};
            // 금지 스키마 각각에서 실제 입력을 전달해 로더 호출이 늘지 않는지 확인한다.
            for (int i = 0; i < urls.length; i++) {
                f.open(urls[i], null);
                long before = f.session.getNavigationCount();
                f.pointer(MouseEvent.MOUSE_CLICKED);
                Check.equal(before, f.session.getNavigationCount(), "blocked default for " + urls[i]);
                Check.equal("about:text", f.session.getPage(), "page preserved");
                Check.equal(HyperlinkEvent.EventType.ACTIVATED, f.last.getEventType(), "host can observe blocked target");
            }
        } finally {
            f.close();
        }
    }
}
