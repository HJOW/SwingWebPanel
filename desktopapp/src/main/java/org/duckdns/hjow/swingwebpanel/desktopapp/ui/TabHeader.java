package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * What a tab shows in the tab strip (plan item P31-04): the title of its page, cut when it is long and written in italics while the page
 * loads, and a small button that closes the tab. The label has no mouse listener of its own, so a click on it selects the tab as usual.
 * <p>
 * 탭이 탭 줄에 보이는 것(계획 P31-04): 페이지의 제목(길면 자르고, 페이지를 읽는 동안은 기울임꼴로 쓴다)과 탭을 닫는 작은 단추. 레이블에는 자체
 * 마우스 리스너가 없으므로 레이블을 누르면 평소처럼 탭이 선택된다.
 */
final class TabHeader extends JPanel {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The largest number of characters of a title in the tab strip. / 탭 줄에 보이는 제목의 최대 글자 수. */
    static final int MAX_TITLE = 24;

    /** Shows the title. / 제목을 보인다. */
    private final JLabel label = new JLabel();

    /** Closes the tab. / 탭을 닫는다. */
    private final JButton close = new JButton(Icons.of(Icons.CLOSE));

    /**
     * Creates the header.
     * <p>
     * 탭 머리를 만든다.
     *
     * @param onClose runs when the close button is pressed / 닫기 단추를 누르면 실행된다
     */
    TabHeader(final Runnable onClose) {
        super(new FlowLayout(FlowLayout.LEFT, 4, 0));
        setOpaque(false);
        close.setMargin(new Insets(1, 1, 1, 1));
        close.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        close.setContentAreaFilled(false);
        close.setFocusable(false);
        close.setToolTipText(Texts.of("Close tab (Ctrl+W)", "탭 닫기 (Ctrl+W)"));
        close.addActionListener(new ActionListener() {
            /** Closes the tab. / 탭을 닫는다. */
            public void actionPerformed(ActionEvent e) {
                onClose.run();
            }
        });
        add(label);
        add(close);
    }

    /**
     * Shows the title and the loading state of the tab.
     * <p>
     * 탭의 제목과 읽는 중 상태를 보인다.
     *
     * @param title   the full title / 전체 제목
     * @param loading true while the page loads / 페이지를 읽는 동안 true
     */
    void show(String title, boolean loading) {
        label.setText(shorten(title));
        Font base = label.getFont();
        int style = loading ? Font.ITALIC : Font.PLAIN;
        // 글꼴 모양이 바뀔 때만 새 글꼴을 만든다.
        if (base != null && base.getStyle() != style) {
            label.setFont(base.deriveFont(style));
        }
    }

    /**
     * Cuts a long title for the tab strip.
     * <p>
     * 탭 줄에 맞게 긴 제목을 자른다.
     *
     * @param title the full title / 전체 제목
     * @return the title, cut with an ellipsis when it is long / 제목. 길면 줄임표로 자른다
     */
    static String shorten(String title) {
        String t = title == null ? "" : title;
        return t.length() <= MAX_TITLE ? t : t.substring(0, MAX_TITLE - 3) + "...";
    }

    /**
     * Returns the text that the header shows.
     * <p>
     * 탭 머리가 보이는 글을 반환한다.
     *
     * @return the shown title / 보이는 제목
     */
    String getShownTitle() {
        return label.getText();
    }

    /**
     * Tells whether the header shows the loading state.
     * <p>
     * 탭 머리가 읽는 중 상태를 보이는지 알려 준다.
     *
     * @return true while the title is written in italics / 제목을 기울임꼴로 쓰는 동안 true
     */
    boolean isShownLoading() {
        return label.getFont() != null && label.getFont().isItalic();
    }

    /**
     * Returns the close button; for tests.
     * <p>
     * 닫기 단추를 반환한다. 시험용이다.
     *
     * @return the button / 단추
     */
    JButton getCloseButton() {
        return close;
    }
}
