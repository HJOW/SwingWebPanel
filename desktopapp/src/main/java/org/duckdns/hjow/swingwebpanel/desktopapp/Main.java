package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.ui.BrowserWindow;

/**
 * The entry point of the browser application. <code>java -jar swingwebpanel-desktopapp.jar [address]</code> opens a browser window that
 * shows the address (the empty page without one). The option <code>--smoke</code> runs a check without showing a window: it loads a page,
 * prints one line and ends with exit code 0 on success and 1 on failure; the build uses it to run the packaged JAR on JRE 6.
 * <p>
 * 브라우저 응용 프로그램의 진입점. <code>java -jar swingwebpanel-desktopapp.jar [주소]</code> 는 그 주소를 보여 주는 브라우저 창을 연다(주소가 없으면
 * 빈 페이지). <code>--smoke</code> 옵션은 창을 보이지 않고 점검을 실행한다: 페이지를 읽고 한 줄을 출력하며 성공하면 종료 코드 0, 실패하면 1 로 끝난다.
 * 빌드가 패키징한 JAR 을 JRE 6 에서 실행해 보는 데 쓴다.
 */
public final class Main {

    /** The longest time in milliseconds that the smoke check waits for its page. / 점검이 페이지를 기다리는 가장 긴 시간(밀리초). */
    private static final long SMOKE_WAIT_MS = 30000;

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들지 않는다.
     */
    private Main() {
    }

    /**
     * Starts the application.
     * <p>
     * 응용 프로그램을 시작한다.
     *
     * @param args an optional address, or <code>--smoke</code> / 선택적인 주소, 또는 <code>--smoke</code>
     * @throws Exception if the start is interrupted / 시작이 중단되면
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && "--smoke".equals(args[0])) {
            boolean ok = smoke();
            System.out.println(ok ? "desktopapp smoke ok" : "desktopapp smoke FAILED");
            System.exit(ok ? 0 : 1);
            return;
        }
        final String url = args.length > 0 ? args[0] : null;
        SwingUtilities.invokeLater(new Runnable() {
            /** Opens the first window on the event dispatch thread. / 이벤트 디스패치 스레드에서 첫 창을 연다. */
            public void run() {
                new BrowserApp(null).openWindow(url).setVisible(true);
            }
        });
    }

    /**
     * Loads a page without showing a window and tells whether its title arrived. With a display it goes through a real browser window
     * that is never made visible; without one (a headless JVM cannot create a frame) it uses the component alone.
     * <p>
     * 창을 보이지 않고 페이지를 읽어 그 제목이 도착했는지 알려 준다. 화면이 있으면 보이지 않게 둔 실제 브라우저 창을 거치고, 화면이 없으면(headless
     * JVM 은 프레임을 만들 수 없다) 컴포넌트만 쓴다.
     *
     * @return true if the page was shown / 페이지가 표시되었으면 true
     * @throws Exception if interrupted / 인터럽트되면
     */
    static boolean smoke() throws Exception {
        final boolean headless = GraphicsEnvironment.isHeadless();
        final BrowserWindow[] window = new BrowserWindow[1];
        final SwingWebPanel[] panel = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the window or the bare component. / 창 또는 컴포넌트만 만든다. */
            public void run() {
                // 화면이 있으면 실제 창의 구성을 그대로 쓴다.
                if (headless) {
                    panel[0] = new SwingWebPanel();
                } else {
                    window[0] = new BrowserApp(new Runnable() {
                        /** Keeps the JVM alive so that the result can be printed. / 결과를 출력할 수 있게 JVM 을 끝내지 않는다. */
                        public void run() {
                        }
                    }).openWindow(null);
                    panel[0] = window[0].getPanel();
                }
                panel[0].setContent("<!doctype html><title>smoke</title><p>desktopapp</p>", "text/html");
            }
        });
        long end = System.currentTimeMillis() + SMOKE_WAIT_MS;
        // 제목이 도착할 때까지 짧게 쉬며 기다린다.
        while (!"smoke".equals(panel[0].getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        final boolean shown = "smoke".equals(panel[0].getTitle());
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Closes what was created. / 만든 것을 닫는다. */
            public void run() {
                if (window[0] != null) {
                    window[0].closeWindow();
                } else {
                    panel[0].dispose();
                }
            }
        });
        return shown && (window[0] == null || window[0].isClosed());
    }
}
