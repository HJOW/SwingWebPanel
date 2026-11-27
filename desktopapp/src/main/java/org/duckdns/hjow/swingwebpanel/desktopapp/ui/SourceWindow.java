package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * The window that shows the source of a page (plan item P31-09): the text that the browser received, exactly as
 * <code>SwingWebPanel.getContent()</code> returns it, in a read-only text area with a fixed-width font. The text is never interpreted as
 * HTML, so markup and scripts in it are only shown. The window holds a copy of the text of the moment it was opened and does not follow
 * the page.
 * <p>
 * 페이지의 원본을 보이는 창(계획 P31-09): 브라우저가 받은 글을 <code>SwingWebPanel.getContent()</code> 가 반환하는 그대로, 고정폭 글꼴의 읽기 전용
 * 텍스트 영역에 보인다. 글을 HTML 로 해석하지 않으므로 그 안의 마크업과 스크립트는 보이기만 한다. 창은 열 때의 글의 복사본을 가지며 페이지를
 * 따라가지 않는다.
 */
final class SourceWindow extends JDialog {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The text area with the source. / 원본이 든 텍스트 영역. */
    private final JTextArea area = new JTextArea();

    /** The line above the text: address, type and size. / 글 위의 줄: 주소, 형식, 크기. */
    private final JLabel info = new JLabel();

    /**
     * Creates the window; it is not shown yet.
     * <p>
     * 창을 만든다. 아직 보이지 않는다.
     *
     * @param owner  the browser window / 브라우저 창
     * @param url    the address of the page / 페이지의 주소
     * @param mime   the type of the page / 페이지의 형식
     * @param source the received text / 수신한 글
     */
    SourceWindow(Window owner, String url, String mime, String source) {
        super(owner, Texts.of("Source of ", "원본: ") + url, ModalityType.MODELESS);
        String text = source == null ? "" : source;
        setName("source-window");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        area.setName("source-text");
        area.setEditable(false);
        area.setLineWrap(false);
        area.setTabSize(4);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        // 글은 평문으로만 넣는다(JTextArea 는 마크업을 해석하지 않는다).
        area.setText(text);
        area.setCaretPosition(0);
        info.setName("source-info");
        info.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        info.setText(url + "   " + (mime == null ? "" : mime) + "   " + text.length() + Texts.of(" characters", "자"));
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(info, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(area), BorderLayout.CENTER);
        setSize(760, 560);
        setLocationRelativeTo(owner);
    }

    /**
     * Returns the text that is shown; for tests.
     * <p>
     * 보이는 글을 반환한다. 시험용이다.
     *
     * @return the text of the area / 영역의 글
     */
    String getSourceText() {
        return area.getText();
    }

    /**
     * Returns the text area; for tests.
     * <p>
     * 텍스트 영역을 반환한다. 시험용이다.
     *
     * @return the area / 영역
     */
    JTextArea getArea() {
        return area;
    }

    /**
     * Returns the line above the text; for tests.
     * <p>
     * 글 위의 줄을 반환한다. 시험용이다.
     *
     * @return the address, the type and the size / 주소, 형식, 크기
     */
    String getInfoText() {
        return info.getText();
    }
}
