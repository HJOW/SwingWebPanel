package org.duckdns.hjow.swingwebpanel;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.core.FakeLoader;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the public component: initial state, scroll pane use, property change notification, listener handling, dispose and
 * the <code>Scrollable</code> policy (plan items P03-01, P03-05, P03-06, P03-07; contract cases C-INIT, C-LINK, C-DISPOSE, C-SCROLL).
 * <p>
 * 공개 컴포넌트 시험: 초기 상태, 스크롤 창 사용, 속성 변경 통지, 리스너 처리, dispose, <code>Scrollable</code> 정책
 * (계획 P03-01, P03-05, P03-06, P03-07, 계약 사례 C-INIT, C-LINK, C-DISPOSE, C-SCROLL).
 */
public class SwingWebPanelTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SwingWebPanelTest() {
    }

    /**
     * Waits until the panel's session finished loading and the EDT notifications ran.
     * <p>
     * 패널의 세션 로딩이 끝나고 EDT 통지가 실행될 때까지 기다린다.
     *
     * @param panel the panel / 패널
     */
    private static void settle(SwingWebPanel panel) {
        long end = System.currentTimeMillis() + 5000;
        // 상태가 100 이 되어 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((panel.getProgress() != 100 || panel.getStatusText().startsWith("Loading")) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
    }

    /**
     * C-INIT-1: the state of a new component (contract section 3).
     * <p>
     * C-INIT-1: 새 컴포넌트의 상태(계약 3절).
     */
    public void testInitialState() {
        SwingWebPanel p = new SwingWebPanel();
        try {
            Check.equal("about:blank", p.getPage(), "getPage");
            Check.equal("", p.getContent(), "getContent");
            Check.equal(1, p.getHistories().size(), "getHistories size");
            Check.equal("about:blank", p.getHistories().get(0), "getHistories entry");
            Check.equal("swingwebpanel/1.0", p.getUserAgent(), "getUserAgent");
            Check.isTrue(p.getHeaders().isEmpty(), "getHeaders");
            Check.equal("", p.getCookies(), "getCookies");
            Check.isTrue(p.isJavascriptEnabled(), "JavaScript is enabled by default");
            Check.equal(100, p.getProgress(), "getProgress");
            Check.equal("", p.getStatusText(), "getStatusText");
            Check.isTrue(p.getLocalStorage().isEmpty(), "getLocalStorage");
            Check.isTrue(p.getSessionStorage().isEmpty(), "getSessionStorage");
        } finally {
            p.dispose();
        }
    }

    /**
     * P03-07: an empty page wrapped in a JScrollPane works and the component creates no scroll pane of its own.
     * <p>
     * P03-07: 빈 페이지를 JScrollPane 으로 감싸도 동작하며 컴포넌트는 자체 스크롤 창을 만들지 않는다.
     */
    public void testWrapsInScrollPane() {
        SwingWebPanel p = new SwingWebPanel();
        try {
            JScrollPane scroll = new JScrollPane(p);
            Check.same(p, scroll.getViewport().getView(), "the panel is the view of the scroll pane");
            Check.equal(0, p.getComponentCount(), "no component (no inner scroll pane) inside the panel");
            Check.isTrue(p.getPreferredSize().width > 0 && p.getPreferredSize().height > 0, "positive preferred size");
            Check.isTrue(p.isOpaque(), "opaque");
        } finally {
            p.dispose();
        }
    }

    /**
     * C-PAGE-1: setPage returns immediately even if loading takes long.
     * <p>
     * C-PAGE-1: 로딩이 오래 걸려도 setPage 는 즉시 반환한다.
     */
    public void testSetPageReturnsImmediately() {
        FakeLoader loader = new FakeLoader();
        loader.gate("http://slow.test/");
        SwingWebPanel p = new SwingWebPanel(loader) {
            private static final long serialVersionUID = 1L;
        };
        try {
            long t0 = System.currentTimeMillis();
            p.setPage("http://slow.test/");
            Check.isTrue(System.currentTimeMillis() - t0 < 1000, "setPage must not wait for the load");
            Check.equal("about:blank", p.getPage(), "still the previous page");
            Check.equal(0, p.getProgress(), "loading has started");
            loader.release("http://slow.test/");
            settle(p);
            Check.equal("http://slow.test/", p.getPage(), "confirmed after the load");
        } finally {
            p.dispose();
        }
    }

    /**
     * P03-06: property change events "page", "statusText", "progress" arrive on the EDT in order and only for real changes.
     * <p>
     * P03-06: "page", "statusText", "progress" 속성 변경 이벤트는 EDT 에서 순서대로, 실제 변경에만 도착한다.
     */
    public void testPropertyChangeEvents() {
        SwingWebPanel p = new SwingWebPanel(new FakeLoader()) {
            private static final long serialVersionUID = 1L;
        };
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());
        final boolean[] offEdt = new boolean[1];
        p.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * Records the event and its thread type.
             * <p>
             * 이벤트와 스레드 종류를 기록한다.
             *
             * @param e event / 이벤트
             */
            public void propertyChange(PropertyChangeEvent e) {
                if (!SwingUtilities.isEventDispatchThread()) {
                    offEdt[0] = true;
                }
                String n = e.getPropertyName();
                if (SwingWebPanel.PROPERTY_PAGE.equals(n) || SwingWebPanel.PROPERTY_STATUS_TEXT.equals(n)
                        || SwingWebPanel.PROPERTY_PROGRESS.equals(n)) {
                    events.add(n + ":" + e.getNewValue());
                }
            }
        });
        try {
            p.setPage("http://a.test/");
            settle(p);
            Check.isFalse(offEdt[0], "notifications on the EDT");
            Object[] expected = { "statusText:Loading http://a.test/", "progress:0", "page:http://a.test/", "statusText:Done", "progress:100" };
            Check.equal(java.util.Arrays.asList(expected), new ArrayList<String>(events), "event sequence");
        } finally {
            p.dispose();
        }
    }

    /**
     * C-LINK-5: null listeners are rejected; unknown listeners can be removed; removeHyperlinkEvents is safe.
     * <p>
     * C-LINK-5: null 리스너는 거부되고, 모르는 리스너를 제거해도 되며, removeHyperlinkEvents 는 안전하다.
     */
    public void testHyperlinkListenerApi() {
        final SwingWebPanel p = new SwingWebPanel();
        try {
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /**
                 * Adds a null listener.
                 * <p>
                 * null 리스너를 등록한다.
                 */
                public void run() {
                    p.addHyperlinkListener(null);
                }
            }, "addHyperlinkListener(null)");
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /**
                 * Removes a null listener.
                 * <p>
                 * null 리스너를 제거한다.
                 */
                public void run() {
                    p.removeHyperlinkListener(null);
                }
            }, "removeHyperlinkListener(null)");
            HyperlinkListener l = new HyperlinkListener() {
                /**
                 * Ignores events.
                 * <p>
                 * 이벤트를 무시한다.
                 *
                 * @param e event / 이벤트
                 */
                public void hyperlinkUpdate(HyperlinkEvent e) {
                }
            };
            p.removeHyperlinkListener(l);
            p.addHyperlinkListener(l);
            p.removeHyperlinkListener(l);
            p.removeHyperlinkEvents();
            p.removeHyperlinkEvents();
        } finally {
            p.dispose();
        }
    }

    /**
     * C-DISPOSE-5: dispose removes listeners registered by the host and by the component itself; Swing's own stay.
     * <p>
     * C-DISPOSE-5: dispose 는 호스트와 컴포넌트 자신이 등록한 리스너를 제거하고 Swing 자체의 리스너는 남긴다.
     */
    public void testDisposeRemovesAttachedListeners() {
        SwingWebPanel p = new SwingWebPanel();
        MouseAdapter mouse = new MouseAdapter() {
        };
        KeyAdapter key = new KeyAdapter() {
        };
        FocusAdapter focus = new FocusAdapter() {
        };
        PropertyChangeListener all = new PropertyChangeListener() {
            /**
             * Ignores events.
             * <p>
             * 이벤트를 무시한다.
             *
             * @param e event / 이벤트
             */
            public void propertyChange(PropertyChangeEvent e) {
            }
        };
        p.addMouseListener(mouse);
        p.addMouseMotionListener(mouse);
        p.addMouseWheelListener(mouse);
        p.addKeyListener(key);
        p.addFocusListener(focus);
        p.addPropertyChangeListener(all);
        p.addPropertyChangeListener("page", all);
        Check.isTrue(hostListeners(p) > 0, "the component has listeners before dispose");
        p.dispose();
        p.dispose();
        Check.equal(0, hostListeners(p), "no host or internal listener remains after dispose");
    }

    /**
     * Counts the listeners on a component that do not belong to Swing/AWT classes.
     * <p>
     * 컴포넌트에 붙은 리스너 중 Swing/AWT 클래스 소유가 아닌 것의 수를 센다.
     *
     * @param p the panel / 패널
     * @return number of non-Swing listeners / Swing 소유가 아닌 리스너 수
     */
    private static int hostListeners(SwingWebPanel p) {
        List<Object> all = new ArrayList<Object>();
        all.addAll(java.util.Arrays.asList((Object[]) p.getComponentListeners()));
        all.addAll(java.util.Arrays.asList((Object[]) p.getMouseListeners()));
        all.addAll(java.util.Arrays.asList((Object[]) p.getMouseMotionListeners()));
        all.addAll(java.util.Arrays.asList((Object[]) p.getMouseWheelListeners()));
        all.addAll(java.util.Arrays.asList((Object[]) p.getKeyListeners()));
        all.addAll(java.util.Arrays.asList((Object[]) p.getFocusListeners()));
        all.addAll(java.util.Arrays.asList((Object[]) p.getPropertyChangeListeners()));
        int n = 0;
        // 리스너의 클래스 이름으로 Swing/AWT 소유 여부를 판단해 센다.
        for (Object o : all) {
            Object l = o;
            if (l instanceof java.beans.PropertyChangeListenerProxy) {
                l = ((java.beans.PropertyChangeListenerProxy) l).getListener();
            }
            String name = l.getClass().getName();
            if (!(name.startsWith("javax.swing.") || name.startsWith("java.awt.") || name.startsWith("sun."))) {
                n++;
            }
        }
        return n;
    }

    /**
     * C-DISPOSE-1..3 on the component: mutators throw, queries return the empty state, dispose is repeatable.
     * <p>
     * 컴포넌트에 대한 C-DISPOSE-1..3: 변경 메서드는 예외, 조회는 빈 상태, dispose 는 반복 가능.
     */
    public void testDisposedComponentBehavior() {
        final SwingWebPanel p = new SwingWebPanel();
        p.setContent("x", "text/plain");
        settle(p);
        p.dispose();
        Check.isTrue(p.isDisposed(), "disposed");
        Check.equal("about:blank", p.getPage(), "getPage");
        Check.equal("", p.getContent(), "getContent");
        Check.equal(1, p.getHistories().size(), "getHistories");
        Check.throwsException(IllegalStateException.class, new Block() {
            /**
             * Calls setPage after dispose.
             * <p>
             * dispose 후 setPage 를 호출한다.
             */
            public void run() {
                p.setPage("about:blank");
            }
        }, "setPage after dispose");
        Check.throwsException(IllegalStateException.class, new Block() {
            /**
             * Calls addHyperlinkListener after dispose.
             * <p>
             * dispose 후 addHyperlinkListener 를 호출한다.
             */
            public void run() {
                p.addHyperlinkListener(new HyperlinkListener() {
                    /**
                     * Ignores events.
                     * <p>
                     * 이벤트를 무시한다.
                     *
                     * @param e event / 이벤트
                     */
                    public void hyperlinkUpdate(HyperlinkEvent e) {
                    }
                });
            }
        }, "addHyperlinkListener after dispose");
        Check.throwsException(IllegalStateException.class, new Block() {
            /**
             * Calls clean after dispose.
             * <p>
             * dispose 후 clean 을 호출한다.
             */
            public void run() {
                p.clean();
            }
        }, "clean after dispose");
        p.removeHyperlinkEvents();
        p.dispose();
    }

    /**
     * P03-05, C-SCROLL: the width tracking, height tracking and increments follow the documented policy.
     * <p>
     * P03-05, C-SCROLL: 폭 추적, 높이 추적, 스크롤 단위가 문서화한 정책을 따른다.
     */
    public void testScrollablePolicy() {
        final SwingWebPanel p = new SwingWebPanel() {
            private static final long serialVersionUID = 1L;
            {
                getViewMetrics().setContentSize(900, 100);
            }
        };
        try {
            JScrollPane scroll = new JScrollPane(p);
            scroll.setSize(300, 200);
            scroll.doLayout();
            Check.isFalse(p.getScrollableTracksViewportWidth(), "content wider than the viewport: scroll horizontally");
            Check.isTrue(p.getScrollableTracksViewportHeight(), "content lower than the viewport: fill the height");
            Check.equal(16, p.getScrollableUnitIncrement(new Rectangle(0, 0, 300, 200), SwingConstants.VERTICAL, 1), "default unit increment");
            Check.equal(184, p.getScrollableBlockIncrement(new Rectangle(0, 0, 300, 200), SwingConstants.VERTICAL, 1), "vertical block increment");
            Check.equal(284, p.getScrollableBlockIncrement(new Rectangle(0, 0, 300, 200), SwingConstants.HORIZONTAL, 1), "horizontal block increment");
            Dimension view = p.getPreferredScrollableViewportSize();
            Check.isTrue(view.width > 0 && view.height > 0, "positive viewport size");
        } finally {
            p.dispose();
        }
    }

    /**
     * C-SCROLL: a narrow page tracks the viewport width and a tall page scrolls vertically.
     * <p>
     * C-SCROLL: 좁은 페이지는 뷰포트 폭을 따라가고 긴 페이지는 세로로 스크롤한다.
     */
    public void testNarrowAndTallContent() {
        final SwingWebPanel p = new SwingWebPanel() {
            private static final long serialVersionUID = 1L;
            {
                getViewMetrics().setContentSize(100, 5000);
                getViewMetrics().setUnitIncrement(20);
            }
        };
        try {
            JScrollPane scroll = new JScrollPane(p);
            scroll.setSize(300, 200);
            scroll.doLayout();
            Check.isTrue(p.getScrollableTracksViewportWidth(), "content that fits tracks the viewport width");
            Check.isFalse(p.getScrollableTracksViewportHeight(), "content taller than the viewport scrolls");
            Check.equal(20, p.getScrollableUnitIncrement(new Rectangle(0, 0, 300, 200), SwingConstants.VERTICAL, -1), "custom unit increment");
            Check.equal(5000, p.getPreferredSize().height, "preferred height follows the content height");
        } finally {
            p.dispose();
        }
    }

    /**
     * P03-07: every public method can be called on the event dispatch thread without blocking or failing.
     * <p>
     * P03-07: 모든 공개 메서드를 이벤트 디스패치 스레드에서 막힘이나 실패 없이 호출할 수 있다.
     *
     * @throws Exception if the EDT call cannot be made / EDT 호출을 할 수 없을 때
     */
    public void testApiCanBeCalledOnTheEventDispatchThread() throws Exception {
        final SwingWebPanel p = new SwingWebPanel(new FakeLoader()) {
            private static final long serialVersionUID = 1L;
        };
        final Throwable[] error = new Throwable[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Calls many methods on the EDT.
                 * <p>
                 * EDT 에서 여러 메서드를 호출한다.
                 */
                public void run() {
                    try {
                        p.setPage("http://a.test/");
                        p.setContent("x", "text/plain");
                        p.back();
                        p.forward();
                        p.refresh();
                        p.refresh(true);
                        p.stop();
                        p.getHistories();
                        p.getStatusText();
                        p.getProgress();
                        p.getContent();
                        p.getPage();
                    } catch (Throwable t) {
                        error[0] = t;
                    }
                }
            });
            Check.isNull(error[0], "no failure when called on the EDT");
            settle(p);
        } finally {
            p.dispose();
        }
    }

    /**
     * P03-07: creating and disposing many components in a row works.
     * <p>
     * P03-07: 컴포넌트를 연속해서 많이 만들고 폐기해도 동작한다.
     */
    public void testRepeatedCreationAndDisposal() {
        // 컴포넌트를 반복해서 만들고, 사용하고, 폐기한다.
        for (int i = 0; i < 100; i++) {
            SwingWebPanel p = new SwingWebPanel();
            p.setContent("c" + i, "text/plain");
            p.setPage("about:blank");
            p.dispose();
        }
    }
}
