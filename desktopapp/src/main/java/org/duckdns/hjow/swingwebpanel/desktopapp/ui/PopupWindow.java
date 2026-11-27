package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import org.duckdns.hjow.swingwebpanel.WindowRequest;
import org.duckdns.hjow.swingwebpanel.desktopapp.BrowserApp;

/**
 * A popup window of the browser (plan item P31-08-g): a small window for a page that a script opened, with a title bar, an address
 * that cannot be edited, the page and a status bar; it has no tabs, no toolbar and no menu. It is a modeless dialog that belongs to
 * the browser window whose page opened it, so it stays above that window and is closed together with it. What its own page asks
 * for (further windows, the context menu) goes to that browser window. Use it on the event dispatch thread.
 * <p>
 * 브라우저의 팝업 창(계획 P31-08-g): 스크립트가 연 페이지를 위한 작은 창으로, 제목 표시줄, 편집할 수 없는 주소, 페이지, 상태 표시줄이 있다. 탭,
 * 도구 모음, 메뉴는 없다. 페이지가 그것을 연 브라우저 창에 속한 비모달 대화 상자이므로 그 창 위에 머물고 그 창과 함께 닫힌다. 팝업의 페이지가
 * 요청하는 것(또 다른 창, 문맥 메뉴)은 그 브라우저 창으로 간다. 이벤트 디스패치 스레드에서 쓴다.
 */
final class PopupWindow extends JDialog implements BrowserTab.Listener {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The width of the page when none was asked for. / 요청한 폭이 없을 때의 페이지 폭. */
    static final int DEFAULT_WIDTH = 640;

    /** The height of the page when none was asked for. / 요청한 높이가 없을 때의 페이지 높이. */
    static final int DEFAULT_HEIGHT = 480;

    /** The smallest side of the page. / 페이지의 가장 작은 변. */
    private static final int MIN_SIDE = 120;

    /** The browser window that the popup belongs to. / 팝업이 속한 브라우저 창. */
    private final transient BrowserWindow owner;

    /** The page of the popup. / 팝업의 페이지. */
    private final transient BrowserTab tab;

    /** Shows the address of the page. / 페이지의 주소를 보인다. */
    private final JTextField address = new JTextField();

    /** The status bar. / 상태 표시줄. */
    private final StatusBar status = new StatusBar();

    /** True once the popup was closed. / 팝업이 닫힌 뒤 true. */
    private boolean closed;

    /**
     * Creates the popup window; the caller makes it visible.
     * <p>
     * 팝업 창을 만든다. 보이게 하는 것은 호출자이다.
     *
     * @param owner  the browser window that the popup belongs to / 팝업이 속할 브라우저 창
     * @param tab    the page of the popup / 팝업의 페이지
     * @param width  the width of the page that was asked for, or -1 / 요청한 페이지 폭. 없으면 -1
     * @param height the height of the page that was asked for, or -1 / 요청한 페이지 높이. 없으면 -1
     * @param left   the left edge on the screen that was asked for, or -1 / 요청한 화면의 왼쪽 가장자리. 없으면 -1
     * @param top    the top edge on the screen that was asked for, or -1 / 요청한 화면의 위쪽 가장자리. 없으면 -1
     */
    PopupWindow(BrowserWindow owner, BrowserTab tab, int width, int height, int left, int top) {
        super(owner, BrowserApp.NAME, Dialog.ModalityType.MODELESS);
        this.owner = owner;
        this.tab = tab;
        address.setName("popup-address");
        address.setEditable(false);
        address.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(address, BorderLayout.NORTH);
        getContentPane().add(tab.getComponent(), BorderLayout.CENTER);
        getContentPane().add(status, BorderLayout.SOUTH);
        tab.setListener(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /**
             * Closes the popup when the user asks for it.
             * <p>
             * 사용자가 요청하면 팝업을 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosing(WindowEvent e) {
                close();
            }

            /**
             * Disposes the page when the popup was closed together with its browser window.
             * <p>
             * 팝업이 브라우저 창과 함께 닫혔으면 페이지를 폐기한다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosed(WindowEvent e) {
                close();
            }
        });
        place(width, height, left, top);
        showState();
    }

    /**
     * Gives the popup its size and place: the page has the size that was asked for (or a default), cut to the screen; the popup is put
     * where it was asked for, or over its browser window.
     * <p>
     * 팝업에 크기와 위치를 준다: 페이지는 요청한 크기(없으면 기본 크기)를 화면에 맞게 자른 것이고, 팝업은 요청한 곳, 없으면 브라우저 창 위에 둔다.
     *
     * @param width  the width of the page, or -1 / 페이지 폭. 없으면 -1
     * @param height the height of the page, or -1 / 페이지 높이. 없으면 -1
     * @param left   the left edge on the screen, or -1 / 화면의 왼쪽 가장자리. 없으면 -1
     * @param top    the top edge on the screen, or -1 / 화면의 위쪽 가장자리. 없으면 -1
     */
    private void place(int width, int height, int left, int top) {
        int w = Math.max(MIN_SIDE, width > 0 ? width : DEFAULT_WIDTH);
        int h = Math.max(MIN_SIDE, height > 0 ? height : DEFAULT_HEIGHT);
        tab.getComponent().setPreferredSize(new Dimension(w, h));
        pack();
        tab.getComponent().setPreferredSize(null);
        GraphicsConfiguration screenOf = owner.getGraphicsConfiguration();
        Rectangle screen = screenOf == null ? new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()) : screenOf.getBounds();
        Insets edges = screenOf == null ? new Insets(0, 0, 0, 0) : Toolkit.getDefaultToolkit().getScreenInsets(screenOf);
        Rectangle usable = new Rectangle(screen.x + edges.left, screen.y + edges.top, screen.width - edges.left - edges.right,
                screen.height - edges.top - edges.bottom);
        // 화면보다 큰 팝업은 화면에 맞춘다.
        setSize(Math.min(getWidth(), usable.width), Math.min(getHeight(), usable.height));
        if (left >= 0 || top >= 0) {
            int x = usable.x + Math.max(0, left);
            int y = usable.y + Math.max(0, top);
            // 요청한 위치가 화면 밖으로 나가면 화면 안으로 당긴다.
            setLocation(Math.max(usable.x, Math.min(x, usable.x + usable.width - getWidth())),
                    Math.max(usable.y, Math.min(y, usable.y + usable.height - getHeight())));
        } else {
            setLocationRelativeTo(owner);
        }
    }

    /**
     * Shows the title, the address and the status of the page.
     * <p>
     * 페이지의 제목, 주소, 상태를 보인다.
     */
    private void showState() {
        if (closed) {
            return;
        }
        setTitle(tab.getTitleText() + " - " + BrowserApp.NAME);
        String url = tab.getAddressText();
        if (!url.equals(address.getText())) {
            address.setText(url);
            address.setCaretPosition(0);
        }
        status.show(tab.getStatusText(), tab.getPanel().getProgress(), tab.getPanel().getZoom());
        owner.offerBlockedPopup(status, tab);
    }

    /**
     * Shows the new state of the page.
     * <p>
     * 페이지의 새 상태를 보인다.
     *
     * @param changed the tab of the popup / 팝업의 탭
     */
    public void tabChanged(BrowserTab changed) {
        showState();
    }

    /**
     * Closes the popup because a script closed its window.
     * <p>
     * 스크립트가 창을 닫았으므로 팝업을 닫는다.
     *
     * @param closing the tab of the popup / 팝업의 탭
     */
    public void tabCloseRequested(BrowserTab closing) {
        close();
    }

    /**
     * Passes a window that a script of the popup opened to the browser window.
     * <p>
     * 팝업의 스크립트가 연 창을 브라우저 창에 넘긴다.
     *
     * @param source  the tab of the popup / 팝업의 탭
     * @param request the window / 창
     * @return true if it is shown / 보였으면 true
     */
    public boolean popupOpened(BrowserTab source, WindowRequest request) {
        return owner.popupOpened(source, request);
    }

    /**
     * Passes what a link, a form or the user asks to open elsewhere to the browser window.
     * <p>
     * 링크, 폼, 사용자가 다른 곳에서 열어 달라고 한 것을 브라우저 창에 넘긴다.
     *
     * @param source  the tab of the popup / 팝업의 탭
     * @param request what was asked for / 요청한 것
     * @return true if it was opened / 열었으면 true
     */
    public boolean windowRequested(BrowserTab source, WindowRequest request) {
        return owner.windowRequested(source, request);
    }

    /**
     * Passes the print request of a script of the popup to the browser window.
     * <p>
     * 팝업의 스크립트의 인쇄 요청을 브라우저 창에 넘긴다.
     *
     * @param source the tab of the popup / 팝업의 탭
     */
    public void printRequested(BrowserTab source) {
        owner.printRequested(source);
    }

    /**
     * Passes the request for the context menu to the browser window.
     * <p>
     * 문맥 메뉴 요청을 브라우저 창에 넘긴다.
     *
     * @param source the tab of the popup / 팝업의 탭
     * @param link   the address of the link under the mouse, or null / 마우스 아래 링크의 주소. 없으면 null
     * @param x      the horizontal position / 가로 위치
     * @param y      the vertical position / 세로 위치
     */
    public void contextMenuRequested(BrowserTab source, String link, int x, int y) {
        owner.contextMenuRequested(source, link, x, y);
    }

    /**
     * Returns the page of the popup.
     * <p>
     * 팝업의 페이지를 반환한다.
     *
     * @return the tab / 탭
     */
    BrowserTab getTab() {
        return tab;
    }

    /**
     * Returns the text of the address field.
     * <p>
     * 주소 칸의 글을 반환한다.
     *
     * @return the address that is shown / 보이는 주소
     */
    String getAddressText() {
        return address.getText();
    }

    /**
     * Returns the status bar of the popup.
     * <p>
     * 팝업의 상태 표시줄을 반환한다.
     *
     * @return the status bar / 상태 표시줄
     */
    StatusBar getStatusBar() {
        return status;
    }

    /**
     * Tells whether the popup was closed.
     * <p>
     * 팝업이 닫혔는지 알려 준다.
     *
     * @return true if closed / 닫혔으면 true
     */
    boolean isClosed() {
        return closed;
    }

    /**
     * Closes the popup: disposes its page and the dialog and tells the browser window. Calling it again does nothing.
     * <p>
     * 팝업을 닫는다: 페이지와 대화 상자를 폐기하고 브라우저 창에 알린다. 다시 불러도 아무 일도 없다.
     */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        owner.popupClosed(this);
        tab.close();
        dispose();
    }
}
