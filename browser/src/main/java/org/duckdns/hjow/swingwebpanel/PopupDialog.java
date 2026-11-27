package org.duckdns.hjow.swingwebpanel;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * The window that {@link SwingWebPanel#openPopupWindow(WindowRequest)} shows by default for a window that a script opened (API contract
 * 5.28): a modeless dialog with the component of the new window in a scroll pane. The dialog belongs to the window of the component
 * whose page opened it, as the download popup does, so it stays in front of that window, can be used even when that window is a modal
 * dialog, and closes with it. The component is disposed (keeping the process-wide localStorage) when the dialog is closed by the user,
 * by a script or together with its owner.
 * <p>
 * 스크립트가 연 창에 대해 {@link SwingWebPanel#openPopupWindow(WindowRequest)} 가 기본으로 보이는 창(API 계약 5.28): 새 창의 컴포넌트를 스크롤
 * 창에 담은 비모달 대화 상자. 대화 상자는 다운로드 팝업처럼 페이지가 그것을 연 컴포넌트의 창에 속하므로, 그 창 앞에 머물고, 그 창이 모달 대화
 * 상자여도 쓸 수 있으며, 그 창과 함께 닫힌다. 대화 상자가 사용자에 의해, 스크립트에 의해, 또는 소유자와 함께 닫힐 때 컴포넌트를 폐기한다(프로세스
 * 공용 localStorage 는 남긴다).
 */
final class PopupDialog extends JDialog {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The width of the content when the page asked for none. / 페이지가 폭을 요청하지 않았을 때의 내용 폭. */
    private static final int DEFAULT_WIDTH = 640;

    /** The height of the content when the page asked for none. / 페이지가 높이를 요청하지 않았을 때의 내용 높이. */
    private static final int DEFAULT_HEIGHT = 480;

    /** The smallest side of the content. / 내용의 가장 작은 변. */
    private static final int MIN_SIDE = 100;

    /** The component of the window. / 창의 컴포넌트. */
    private final transient SwingWebPanel panel;

    /** True once the component was disposed with the dialog. / 대화 상자와 함께 컴포넌트를 폐기한 뒤 true. */
    private boolean released;

    /**
     * Creates the dialog.
     * <p>
     * 대화 상자를 만든다.
     *
     * @param owner the window that the dialog belongs to, or null / 대화 상자가 속한 창. 없으면 null
     * @param panel the component of the new window / 새 창의 컴포넌트
     */
    private PopupDialog(Window owner, SwingWebPanel panel) {
        super(owner, "", Dialog.ModalityType.MODELESS);
        this.panel = panel;
        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(null);
        setContentPane(scroll);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            /**
             * Disposes the component when the dialog was closed, by the user or together with its owner.
             * <p>
             * 대화 상자가 사용자에 의해, 또는 소유자와 함께 닫히면 컴포넌트를 폐기한다.
             *
             * @param e the event / 이벤트
             */
            public void windowClosed(WindowEvent e) {
                release();
            }
        });
        panel.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * Shows the title or the address of the page in the title bar.
             * <p>
             * 페이지의 제목이나 주소를 제목 표시줄에 보인다.
             *
             * @param e the change / 변경
             */
            public void propertyChange(PropertyChangeEvent e) {
                if (SwingWebPanel.PROPERTY_TITLE.equals(e.getPropertyName()) || SwingWebPanel.PROPERTY_PAGE.equals(e.getPropertyName())) {
                    showTitle();
                }
            }
        });
        panel.setWindowCloser(new Runnable() {
            /** Closes the dialog after a script closed the window. / 스크립트가 창을 닫은 뒤 대화 상자를 닫는다. */
            public void run() {
                dispose();
                release();
            }
        });
        showTitle();
    }

    /**
     * Shows the title of the page, or its address when it has no title, in the title bar.
     * <p>
     * 페이지의 제목을 제목 표시줄에 보인다. 제목이 없으면 주소를 보인다.
     */
    private void showTitle() {
        String title = panel.getTitle();
        setTitle(title != null && title.trim().length() > 0 ? title.trim() : panel.getPage());
    }

    /**
     * Disposes the component once.
     * <p>
     * 컴포넌트를 한 번 폐기한다.
     */
    private void release() {
        if (!released) {
            released = true;
            panel.setWindowCloser(null);
            panel.disposeKeepingStorage();
        }
    }

    /**
     * Shows the window of a request: the content has the size that the page asked for (or a default), cut to the screen, and the
     * dialog is put where the page asked or beside the window of the component that opened it. Call it on the event dispatch thread.
     * <p>
     * 요청의 창을 보인다: 내용은 페이지가 요청한 크기(없으면 기본 크기)를 화면에 맞게 자른 것이고, 대화 상자는 페이지가 요청한 곳이나 그것을 연
     * 컴포넌트의 창 옆에 둔다. 이벤트 디스패치 스레드에서 부른다.
     *
     * @param opener  the component whose page opened the window / 페이지가 창을 연 컴포넌트
     * @param request the window to show / 보일 창
     * @return the dialog, or null if the request has no component / 대화 상자. 요청에 컴포넌트가 없으면 null
     */
    static PopupDialog show(SwingWebPanel opener, WindowRequest request) {
        SwingWebPanel panel = request.getPanel();
        if (panel == null || panel.isDisposed()) {
            return null;
        }
        Window owner = SwingUtilities.getWindowAncestor(opener);
        PopupDialog dialog = new PopupDialog(owner, panel);
        GraphicsConfiguration screenOf = owner != null ? owner.getGraphicsConfiguration() : dialog.getGraphicsConfiguration();
        Rectangle screen = screenOf == null ? new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()) : screenOf.getBounds();
        Insets edges = screenOf == null ? new Insets(0, 0, 0, 0) : Toolkit.getDefaultToolkit().getScreenInsets(screenOf);
        Rectangle usable = new Rectangle(screen.x + edges.left, screen.y + edges.top, screen.width - edges.left - edges.right,
                screen.height - edges.top - edges.bottom);
        int w = request.getWidth() > 0 ? request.getWidth() : DEFAULT_WIDTH;
        int h = request.getHeight() > 0 ? request.getHeight() : DEFAULT_HEIGHT;
        dialog.getContentPane().setPreferredSize(new Dimension(Math.max(MIN_SIDE, w), Math.max(MIN_SIDE, h)));
        dialog.pack();
        // 창이 화면보다 크면 화면에 맞춘다.
        Dimension size = dialog.getSize();
        dialog.setSize(Math.min(size.width, usable.width), Math.min(size.height, usable.height));
        if (request.getLeft() >= 0 || request.getTop() >= 0) {
            int x = usable.x + Math.max(0, request.getLeft());
            int y = usable.y + Math.max(0, request.getTop());
            // 요청한 위치가 화면 밖으로 나가면 화면 안으로 당긴다.
            x = Math.max(usable.x, Math.min(x, usable.x + usable.width - dialog.getWidth()));
            y = Math.max(usable.y, Math.min(y, usable.y + usable.height - dialog.getHeight()));
            dialog.setLocation(x, y);
        } else {
            dialog.setLocationRelativeTo(owner);
        }
        dialog.setVisible(true);
        return dialog;
    }
}
