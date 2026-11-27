package org.duckdns.hjow.swingwebpanel.desktopapp;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.desktopapp.ui.BrowserWindow;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The skeleton of the browser application (plan item P31-02): the check that the packaged JAR runs, a window that shows a page and its
 * title, and the end of the program when the last window is closed. The cases that need a frame are skipped in a headless JVM.
 * <p>
 * 브라우저 응용 프로그램의 골격(계획 P31-02): 패키징한 JAR 이 실행하는 점검, 페이지와 그 제목을 보여 주는 창, 마지막 창이 닫힐 때의 프로그램 종료.
 * 프레임이 필요한 사례는 headless JVM 에서 건너뛴다.
 */
public class BrowserAppTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public BrowserAppTest() {
    }

    /** The directory of the user data of the case. / 사례의 사용자 데이터 디렉터리. */
    private java.io.File home;

    /** The value of the user data property before the case. / 사례 전의 사용자 데이터 속성 값. */
    private String oldHome;

    /**
     * Points the user data to a temporary directory, so that a case never touches the data of the real user.
     * <p>
     * 사용자 데이터를 임시 디렉터리로 돌려, 사례가 실제 사용자의 데이터를 건드리지 않게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        home = java.io.File.createTempFile("swp-desktopapp-home", "");
        home.delete();
        oldHome = System.getProperty(BrowserApp.HOME_PROPERTY);
        System.setProperty(BrowserApp.HOME_PROPERTY, home.getPath());
    }

    /**
     * Removes the temporary user data and restores the property.
     * <p>
     * 임시 사용자 데이터를 지우고 속성을 되돌린다.
     */
    public void tearDown() {
        java.io.File[] files = home.listFiles();
        // 사례가 쓴 파일을 지운다.
        for (int i = 0; files != null && i < files.length; i++) {
            files[i].delete();
        }
        home.delete();
        if (oldHome == null) {
            System.clearProperty(BrowserApp.HOME_PROPERTY);
        } else {
            System.setProperty(BrowserApp.HOME_PROPERTY, oldHome);
        }
    }

    /**
     * The check behind <code>--smoke</code> loads its page and closes what it opened.
     * <p>
     * <code>--smoke</code> 의 점검이 페이지를 읽고 연 것을 닫는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSmokeCheckLoadsAPage() throws Exception {
        Check.isTrue(Main.smoke(), "the smoke check must succeed");
    }

    /**
     * A window shows the title of its page followed by the name of the application, and closing the last window runs the end action of
     * the application exactly once and disposes the component.
     * <p>
     * 창은 페이지의 제목 뒤에 응용 프로그램의 이름을 붙여 보이고, 마지막 창을 닫으면 응용 프로그램의 종료 동작이 정확히 한 번 실행되며 컴포넌트가
     * 폐기된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWindowTitleAndLastWindow() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            Check.skip("a browser window needs a display");
            return;
        }
        final int[] ended = new int[1];
        final BrowserWindow[] made = new BrowserWindow[2];
        final BrowserApp[] app = new BrowserApp[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Opens two windows without showing them. / 창 두 개를 보이지 않게 연다. */
            public void run() {
                app[0] = new BrowserApp(new Runnable() {
                    /** Counts the end of the program. / 프로그램 종료를 센다. */
                    public void run() {
                        ended[0]++;
                    }
                });
                made[0] = app[0].openWindow(null);
                made[1] = app[0].openWindow(null);
                made[0].getPanel().setContent("<!doctype html><title>First page</title><p>x</p>", "text/html");
            }
        });
        long end = System.currentTimeMillis() + 30000;
        // 제목이 창에 반영될 때까지 짧게 쉬며 기다린다.
        while (!("First page - " + BrowserApp.NAME).equals(made[0].getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.equal("First page - " + BrowserApp.NAME, made[0].getTitle(), "the title bar shows the page title and the application name");
        Check.equal(BrowserApp.NAME, made[1].getTitle(), "an empty page shows the application name only");
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Closes the first window twice. / 첫 창을 두 번 닫는다. */
            public void run() {
                made[0].closeWindow();
                made[0].closeWindow();
            }
        });
        Check.equal(0, ended[0], "the program goes on while a window is open");
        Check.equal(1, app[0].getWindows().size(), "one window is left");
        Check.isTrue(made[0].getPanel().isDisposed(), "closing a window disposes its component");
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Closes the last window. / 마지막 창을 닫는다. */
            public void run() {
                made[1].closeWindow();
            }
        });
        Check.equal(1, ended[0], "closing the last window ends the program once");
        Check.equal(0, app[0].getWindows().size(), "no window is left");
    }
}
