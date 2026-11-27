package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.io.File;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WindowRequest;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;
import org.duckdns.hjow.swingwebpanel.desktopapp.DesktopTls;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.Settings;

/**
 * The component of a tab: a {@link SwingWebPanel} whose extension points ask the application (plan items P31-07 and P31-08). The
 * download folder comes from the settings. A window that a page asks for, a blocked popup and a close by a script are passed to
 * the tab that shows the component, which passes them to its window. The component of a window that a script opened is created
 * before any tab exists for it (the script holds its window object at once), so the tab is bound later, when the window is shown.
 * <p>
 * 탭의 컴포넌트: 확장 지점이 응용 프로그램에 묻는 {@link SwingWebPanel}(계획 P31-07, P31-08). 다운로드 폴더는 설정에서 온다. 페이지가 요청한
 * 창, 차단된 팝업, 스크립트의 닫기는 컴포넌트를 보이는 탭에 넘기고, 탭은 그것을 자기 창에 넘긴다. 스크립트가 연 창의 컴포넌트는 그것을 위한 탭이
 * 생기기 전에 만들어지므로(스크립트가 창 객체를 곧바로 쥔다) 탭은 나중에, 창을 보일 때 묶는다.
 */
final class TabPanel extends SwingWebPanel {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The application. / 응용 프로그램. */
    private final transient BrowserApp app;

    /** The tab that shows this component, or null while it has none. / 이 컴포넌트를 보이는 탭. 아직 없으면 null. */
    private transient volatile BrowserTab tab;

    /**
     * Creates the component of a tab that the user or the application opens; it shares the cookie jar of the application.
     * <p>
     * 사용자나 응용 프로그램이 여는 탭의 컴포넌트를 만든다. 응용 프로그램의 쿠키 저장소를 함께 쓴다.
     *
     * @param app the application / 응용 프로그램
     */
    TabPanel(BrowserApp app) {
        super(app.getCookieJar());
        this.app = app;
        DesktopTls.configure(this);
    }

    /**
     * Creates the component of a window that a page opened. It may run on the thread of the script that opened the window.
     * <p>
     * 페이지가 연 창의 컴포넌트를 만든다. 창을 연 스크립트의 스레드에서 실행될 수 있다.
     *
     * @param app  the application / 응용 프로그램
     * @param seed what the component needs to become that window / 컴포넌트가 그 창이 되는 데 필요한 것
     */
    private TabPanel(BrowserApp app, PopupSeed seed) {
        super(seed);
        this.app = app;
        DesktopTls.configure(this);
    }

    /**
     * Binds the tab that shows this component.
     * <p>
     * 이 컴포넌트를 보이는 탭을 묶는다.
     *
     * @param tab the tab, or null when the tab is closed / 탭. 탭이 닫히면 null
     */
    void bind(BrowserTab tab) {
        this.tab = tab;
    }

    /**
     * Tells whether this component belongs to an application.
     * <p>
     * 이 컴포넌트가 어떤 응용 프로그램의 것인지 알려 준다.
     *
     * @param other the application / 응용 프로그램
     * @return true if it was created for that application / 그 응용 프로그램을 위해 만들어졌으면 true
     */
    boolean belongsTo(BrowserApp other) {
        return app == other;
    }

    /**
     * Returns the download folder of the settings, if one is set and exists.
     * <p>
     * 설정의 다운로드 폴더가 정해져 있고 실제로 있으면 그것을 반환한다.
     *
     * @return the folder, or the default of the component / 폴더. 없으면 컴포넌트의 기본값
     */
    protected File getDownloadDirectory() {
        String dir = app.getSettings().get(Settings.DOWNLOAD_DIRECTORY).trim();
        if (dir.length() > 0) {
            File f = new File(dir);
            if (f.isDirectory()) {
                return f;
            }
        }
        return super.getDownloadDirectory();
    }

    /**
     * Creates the component of a window that a page of this component opens: a component of the same kind, so that the window can
     * become a tab, a browser window or a popup window of the application.
     * <p>
     * 이 컴포넌트의 페이지가 여는 창의 컴포넌트를 만든다: 같은 종류의 컴포넌트이므로 그 창은 응용 프로그램의 탭, 브라우저 창, 팝업 창이 될 수
     * 있다.
     *
     * @param seed what the component needs / 컴포넌트에 필요한 것
     * @return the component / 컴포넌트
     */
    protected SwingWebPanel createPopupPanel(PopupSeed seed) {
        return new TabPanel(app, seed);
    }

    /**
     * Shows a window that a script opened where the settings say: a popup window, a browser window or a tab. A component without a
     * tab (its tab was closed in the meantime) declines the window.
     * <p>
     * 스크립트가 연 창을 설정이 정한 곳에 보인다: 팝업 창, 브라우저 창, 탭. 탭이 없는 컴포넌트(그 사이 탭이 닫혔다)는 창을 거절한다.
     *
     * @param request the window / 창
     */
    protected void openPopupWindow(WindowRequest request) {
        BrowserTab t = tab;
        if (t == null || !t.popupOpened(request)) {
            request.reject();
        }
    }

    /**
     * Opens what a link, a form or the user asks to open elsewhere, in a tab or a window of the application.
     * <p>
     * 링크, 폼, 사용자가 다른 곳에서 열어 달라고 한 것을 응용 프로그램의 탭이나 창에서 연다.
     *
     * @param request what was asked for / 요청한 것
     * @return true if the application opened it / 응용 프로그램이 열었으면 true
     */
    protected boolean openNewWindow(WindowRequest request) {
        BrowserTab t = tab;
        return t != null && t.windowRequested(request);
    }

    /**
     * Tells the window of the tab that a popup was blocked, so that the user can open it.
     * <p>
     * 팝업이 차단되었음을 탭의 창에 알려, 사용자가 그것을 열 수 있게 한다.
     *
     * @param request the blocked window / 차단된 창
     */
    protected void popupBlocked(WindowRequest request) {
        BrowserTab t = tab;
        if (t != null) {
            t.popupBlocked(request);
        }
    }

    /**
     * Passes the print request of a script of the page (<code>window.print()</code>) to the tab, which prints as the print command does.
     * <p>
     * 페이지 스크립트의 인쇄 요청(<code>window.print()</code>)을 탭에 넘긴다. 탭은 인쇄 명령과 같이 인쇄한다.
     */
    protected void printRequested() {
        BrowserTab t = tab;
        if (t != null) {
            t.requestPrint();
        }
    }

    /**
     * Closes only this tab when a script of its page closes the window (the settings allowed it); the program does not end unless it
     * was the last tab.
     * <p>
     * 페이지의 스크립트가 창을 닫으면(설정이 허용했다) 이 탭만 닫는다. 마지막 탭이 아니면 프로그램은 끝나지 않는다.
     */
    protected void closeRootWindow() {
        BrowserTab t = tab;
        if (t != null) {
            t.requestClose();
        }
    }

    /**
     * Closes the tab or the popup window of a window that a page opened, as if the user closed it. A component that is not shown
     * yet is disposed.
     * <p>
     * 페이지가 연 창의 탭이나 팝업 창을 사용자가 닫은 것처럼 닫는다. 아직 보이지 않은 컴포넌트는 폐기한다.
     */
    protected void closeOpenedWindow() {
        BrowserTab t = tab;
        if (t != null) {
            t.requestClose();
        } else {
            super.closeOpenedWindow();
        }
    }
}
