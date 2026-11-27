package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * The status bar at the bottom of a browser window (plan item P31-03): the status text of the page (which is also the address of the link
 * under the mouse), a progress bar that is visible while the page loads, and the zoom factor.
 * <p>
 * 브라우저 창 아래쪽의 상태 표시줄(계획 P31-03): 페이지의 상태 문구(마우스 아래 링크의 주소이기도 하다), 페이지를 읽는 동안 보이는 진행 막대, 확대
 * 비율.
 */
final class StatusBar extends JPanel {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** Shows the status text. / 상태 문구를 보인다. */
    private final JLabel text = new JLabel(" ");

    /** Shows the loading progress. / 로딩 진행을 보인다. */
    private final JProgressBar progress = new JProgressBar(0, 100);

    /** Shows the zoom factor. / 확대 비율을 보인다. */
    private final JLabel zoom = new JLabel("100%");

    /** Does what the status text offers (opens a blocked popup); hidden while there is nothing to do. / 상태 문구가 제안하는 일을 한다(차단된 팝업 열기). 할 일이 없으면 숨는다. */
    private final JButton action = new JButton();

    /** What the button does, or null. / 단추가 하는 일. 없으면 null. */
    private transient Runnable actionWork;

    /**
     * Creates the status bar.
     * <p>
     * 상태 표시줄을 만든다.
     */
    StatusBar() {
        super(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        progress.setPreferredSize(new Dimension(140, 14));
        progress.setVisible(false);
        JPanel right = new JPanel(new BorderLayout(8, 0));
        action.setName("status-action");
        action.setFocusable(false);
        action.setMargin(new java.awt.Insets(0, 6, 0, 6));
        action.setVisible(false);
        action.addActionListener(new java.awt.event.ActionListener() {
            /**
             * Runs the offered work.
             * <p>
             * 제안한 일을 실행한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(java.awt.event.ActionEvent e) {
                Runnable work = actionWork;
                if (work != null) {
                    work.run();
                }
            }
        });
        right.add(action, BorderLayout.WEST);
        right.add(progress, BorderLayout.CENTER);
        right.add(zoom, BorderLayout.EAST);
        add(text, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    /**
     * Shows the state of a page.
     * <p>
     * 페이지의 상태를 보인다.
     *
     * @param status   the status text, or null / 상태 문구. 없으면 null
     * @param percent  the progress: 0 to 100, or -1 when the size is unknown; 100 means that nothing is loading / 진행률: 0~100, 크기를
     *                 모르면 -1. 100 이면 읽는 중이 아니다
     * @param zoomSize the zoom factor (1.0 is 100%) / 확대 비율(1.0 이 100%)
     */
    void show(String status, int percent, double zoomSize) {
        // 빈 문구에도 높이가 유지되도록 공백 하나를 둔다.
        text.setText(status == null || status.length() == 0 ? " " : status);
        boolean loading = percent != 100;
        progress.setVisible(loading);
        // 크기를 모르는 동안은 움직이는 막대를 보인다.
        if (loading) {
            progress.setIndeterminate(percent < 0);
            if (percent >= 0) {
                progress.setValue(percent);
            }
        }
        zoom.setText(Math.round(zoomSize * 100) + "%");
    }

    /**
     * Shows or hides the button beside the status text.
     * <p>
     * 상태 문구 옆의 단추를 보이거나 숨긴다.
     *
     * @param label the text of the button, or null to hide it / 단추의 글. 숨기려면 null
     * @param work  what the button does / 단추가 하는 일
     */
    void offer(String label, Runnable work) {
        actionWork = label == null ? null : work;
        if (label != null) {
            action.setText(label);
        }
        if (action.isVisible() != (label != null)) {
            action.setVisible(label != null);
            validate();
        }
    }

    /**
     * Returns the button beside the status text.
     * <p>
     * 상태 문구 옆의 단추를 반환한다.
     *
     * @return the button; it is not visible while there is nothing to offer / 단추. 제안할 것이 없으면 보이지 않는다
     */
    JButton getActionButton() {
        return action;
    }

    /**
     * Returns the status text that is shown.
     * <p>
     * 보이는 상태 문구를 반환한다.
     *
     * @return the text / 문구
     */
    String getStatusText() {
        return text.getText().trim();
    }

    /**
     * Tells whether the progress bar is visible.
     * <p>
     * 진행 막대가 보이는지 알려 준다.
     *
     * @return true while a page loads / 페이지를 읽는 동안 true
     */
    boolean isProgressVisible() {
        return progress.isVisible();
    }

    /**
     * Returns the text of the zoom factor.
     * <p>
     * 확대 비율의 글을 반환한다.
     *
     * @return the text, for example "100%" / 글. 예: "100%"
     */
    String getZoomText() {
        return zoom.getText();
    }
}
