package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
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
