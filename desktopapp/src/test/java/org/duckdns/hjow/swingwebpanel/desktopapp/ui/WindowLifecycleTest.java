package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * What stays behind after tabs and windows of the browser were opened and closed many times (plan item P31-11): the document threads
 * of the closed pages end, the components of the closed tabs can be collected, and closing a window that is not the last one does not
 * end the application.
 * <p>
 * 브라우저의 탭과 창을 여러 번 열고 닫은 뒤에 남는 것(계획 P31-11): 닫은 페이지의 문서 스레드가 끝나고, 닫은 탭의 컴포넌트를 수거할 수 있으며,
 * 마지막이 아닌 창을 닫아도 응용 프로그램은 끝나지 않는다.
 */
public class WindowLifecycleTest extends WindowCase {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public WindowLifecycleTest() {
    }

    /**
     * Counts the live document threads of all components of the JVM.
     * <p>
     * JVM 의 모든 컴포넌트의 살아 있는 문서 스레드를 센다.
     *
     * @return the number of threads / 스레드 수
     */
    private static int documentThreads() {
        int n = 0;
        // 살아 있는 스레드마다 문서 스레드의 이름인지 본다.
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("swingwebpanel-document-")) {
                n++;
            }
        }
        return n;
    }

    /**
     * Opening and closing windows with several tabs and their helper windows leaves no document thread and no component behind.
     * <p>
     * 여러 탭과 보조 창이 있는 창을 열고 닫아도 문서 스레드와 컴포넌트가 남지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOpeningAndClosingLeavesNothingBehind() throws Exception {
        if (skipped()) {
            return;
        }
        final String a = pages.page("life-a.html", "Life A", "<p>alpha page with words</p><script>console.log('a');"
                + "setInterval(function () { document.body.setAttribute('data-tick', String(Date.now())); }, 20);</" + "script>");
        final String b = pages.page("life-b.html", "Life B", "<p>beta page</p>");
        onEdt(new Runnable() {
            /** Shows a page in the first window, so that its thread exists before the count. / 세기 전에 스레드가 있도록 첫 창에 페이지를 보인다. */
            public void run() {
                window.navigate(b);
            }
        });
        awaitTab(0, "Life B");
        final int before = documentThreads();
        final List<WeakReference<SwingWebPanel>> closed = new ArrayList<WeakReference<SwingWebPanel>>();
        // 창을 열고, 탭과 보조 창을 쓰고, 닫기를 여러 번 되풀이한다.
        for (int round = 0; round < 4; round++) {
            final BrowserWindow[] extra = new BrowserWindow[1];
            onEdt(new Runnable() {
                /** Opens a window with three tabs. / 탭 세 개가 있는 창을 연다. */
                public void run() {
                    extra[0] = window.newWindow(a);
                    extra[0].newTab(b, false);
                    extra[0].newTab(a, false);
                }
            });
            awaitTab(extra[0], 0, "Life A");
            awaitTab(extra[0], 1, "Life B");
            awaitTab(extra[0], 2, "Life A");
            onEdt(new Runnable() {
                /** Uses the helper windows, closes a tab and then the window. / 보조 창을 쓰고, 탭 하나를 닫은 다음 창을 닫는다. */
                public void run() {
                    BrowserWindow w = extra[0];
                    w.perform(Commands.FIND);
                    w.getFindBar().getField().setText("page");
                    w.perform(Commands.CONSOLE);
                    w.perform(Commands.HISTORY);
                    w.perform(Commands.VIEW_SOURCE);
                    w.perform(Commands.PRINT_PREVIEW);
                    // 닫는 컴포넌트를 약하게만 기억한다.
                    for (int i = 0; i < w.getTabs().size(); i++) {
                        closed.add(new WeakReference<SwingWebPanel>(w.getTabs().get(i).getPanel()));
                    }
                    w.closeTab(w.getTabs().get(1));
                    Check.equal(2, w.getTabs().size(), "the tab is closed");
                    w.closeWindow();
                    Check.isTrue(w.isClosed(), "the window is closed");
                }
            });
        }
        Check.equal(0, ended, "closing windows that are not the last one does not end the application");
        Check.equal(1, app.getWindows().size(), "only the first window is left");
        long end = System.currentTimeMillis() + 20000;
        // 닫은 페이지의 문서 스레드가 끝날 때까지 기다린다.
        while (documentThreads() > before && System.currentTimeMillis() < end) {
            Thread.sleep(50);
        }
        Check.isTrue(documentThreads() <= before, "the document threads of the closed tabs end: " + documentThreads() + " left, " + before + " before");
        int alive = closed.size();
        end = System.currentTimeMillis() + 20000;
        // 수거는 시점이 정해져 있지 않으므로 여러 번 요청하며 기다린다.
        while (alive > 0 && System.currentTimeMillis() < end) {
            System.gc();
            Thread.sleep(100);
            alive = 0;
            // 아직 수거되지 않은 컴포넌트를 센다.
            for (int i = 0; i < closed.size(); i++) {
                if (closed.get(i).get() != null) {
                    alive++;
                }
            }
        }
        Check.equal(0, alive, "the components of the closed tabs can be collected (" + closed.size() + " were closed)");
        onEdt(new Runnable() {
            /** The first window still works. / 첫 창은 여전히 동작한다. */
            public void run() {
                Check.equal("Life B", window.getPanel().getTitle(), "the page of the first window is untouched");
                window.navigate(a);
            }
        });
        awaitTab(0, "Life A");
    }
}
