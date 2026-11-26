package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;

/**
 * A browser window: a frame that shows a page of a {@link SwingWebPanel} inside a scroll pane, with the title of the page in its title
 * bar. Closing the window disposes the component and tells the application. Use it on the event dispatch thread.
 * <p>
 * 브라우저 창: {@link SwingWebPanel} 의 페이지를 스크롤 창 안에 보여 주는 프레임이며 제목 표시줄에 페이지의 제목을 보인다. 창을 닫으면 컴포넌트를
 * 폐기하고 응용 프로그램에 알린다. 이벤트 디스패치 스레드에서 쓴다.
 */
public final class BrowserWindow extends JFrame {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The application that owns the window. / 창을 소유한 응용 프로그램. */
    private final transient BrowserApp app;

    /** The component that shows the page. / 페이지를 보여 주는 컴포넌트. */
    private final SwingWebPanel panel;

    /** True once the window was closed. / 창이 닫힌 뒤 true. */
    private boolean closed;

    /**
     * Creates a window with an empty page.
     * <p>
     * 빈 페이지가 있는 창을 만든다.
     *
     * @param app the application that owns the window / 창을 소유할 응용 프로그램
     */
    public BrowserWindow(BrowserApp app) {
        super(BrowserApp.NAME);
        this.app = app;
        panel = new SwingWebPanel();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(panel), BorderLayout.CENTER);
        setSize(1000, 700);
        setLocationByPlatform(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /**
             * Closes the window when the user asks for it.
             * <p>
             * 사용자가 요청하면 창을 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        PropertyChangeListener titles = new PropertyChangeListener() {
            /**
             * Shows the new title or address in the title bar.
             * <p>
             * 새 제목이나 주소를 제목 표시줄에 보인다.
             *
             * @param e the change / 변경
             */
            public void propertyChange(PropertyChangeEvent e) {
                updateTitle();
            }
        };
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_TITLE, titles);
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_PAGE, titles);
    }

    /**
     * Returns the component that shows the page of this window.
     * <p>
     * 이 창의 페이지를 보여 주는 컴포넌트를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    public SwingWebPanel getPanel() {
        return panel;
    }

    /**
     * Starts loading an address in this window.
     * <p>
     * 이 창에서 주소 읽기를 시작한다.
     *
     * @param url an absolute address / 절대 주소
     */
    public void navigate(String url) {
        panel.setPage(url);
    }

    /**
     * Shows the title of the page, or its address when it has no title, followed by the name of the application.
     * <p>
     * 페이지의 제목을, 제목이 없으면 주소를 보이고 그 뒤에 응용 프로그램의 이름을 붙인다.
     */
    private void updateTitle() {
        String title = panel.getTitle();
        String shown = title != null && title.trim().length() > 0 ? title.trim() : panel.getPage();
        // 빈 페이지는 응용 프로그램의 이름만 보인다.
        if (shown == null || shown.length() == 0 || BrowserApp.BLANK.equals(shown)) {
            setTitle(BrowserApp.NAME);
        } else {
            setTitle(shown + " - " + BrowserApp.NAME);
        }
    }

    /**
     * Closes the window: disposes the component and the frame and tells the application. Calling it again does nothing.
     * <p>
     * 창을 닫는다: 컴포넌트와 프레임을 폐기하고 응용 프로그램에 알린다. 다시 불러도 아무 일도 없다.
     */
    public void closeWindow() {
        if (closed) {
            return;
        }
        closed = true;
        panel.dispose();
        dispose();
        app.windowClosed(this);
    }

    /**
     * Tells whether the window was closed.
     * <p>
     * 창이 닫혔는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    public boolean isClosed() {
        return closed;
    }
}
