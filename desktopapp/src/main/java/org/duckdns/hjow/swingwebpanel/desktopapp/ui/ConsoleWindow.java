package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;
import org.duckdns.hjow.swingwebpanel.desktopapp.model.ConsoleLog;

/**
 * The console window of a browser window (plan item P31-09): it shows the console messages of the active tab, one line per message with
 * its level, colored by level, and follows the tab while it is open. A list chooses the lowest level that is shown and a button empties
 * the log of the tab. Messages are plain text; nothing in them is interpreted.
 * <p>
 * 브라우저 창의 콘솔 창(계획 P31-09): 활성 탭의 콘솔 메시지를 메시지마다 한 줄로, 수준과 함께 수준별 색으로 보이고, 열려 있는 동안 탭을 따라간다.
 * 목록으로 보일 최소 수준을 고르고 단추로 탭의 로그를 비운다. 메시지는 평문이며 그 안의 어떤 것도 해석하지 않는다.
 */
final class ConsoleWindow extends JDialog implements ConsoleLog.Listener {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The messages. / 메시지. */
    private final JTextPane pane = new JTextPane();

    /** Chooses the lowest level that is shown. / 보일 최소 수준을 고른다. */
    private final JComboBox levels = new JComboBox(new Object[] { Texts.of("All levels", "모든 수준"), Texts.of("Log and above", "log 이상"),
            Texts.of("Warnings and errors", "경고와 오류"), Texts.of("Errors only", "오류만") });

    /** Tells which page the messages belong to. / 메시지가 어느 페이지의 것인지 알린다. */
    private final JLabel owner = new JLabel(" ");

    /** The log that is shown, or null. / 보이는 로그. 없으면 null. */
    private transient ConsoleLog log;

    /**
     * Creates the window; it is not shown yet.
     * <p>
     * 창을 만든다. 아직 보이지 않는다.
     *
     * @param parent the browser window / 브라우저 창
     */
    ConsoleWindow(Window parent) {
        super(parent, Texts.of("Console", "콘솔"), ModalityType.MODELESS);
        setName("console-window");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        pane.setName("console-text");
        pane.setEditable(false);
        pane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        levels.setName("console-levels");
        levels.addActionListener(new ActionListener() {
            /**
             * Shows the log again with the chosen level.
             * <p>
             * 고른 수준으로 로그를 다시 보인다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                refill();
            }
        });
        JButton clear = new JButton(Texts.of("Clear", "지우기"));
        clear.setName("console-clear");
        clear.addActionListener(new ActionListener() {
            /**
             * Empties the log of the tab.
             * <p>
             * 탭의 로그를 비운다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                if (log != null) {
                    log.clear();
                }
            }
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        bar.add(levels);
        bar.add(clear);
        bar.add(owner);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(bar, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(pane), BorderLayout.CENTER);
        setSize(720, 320);
        setLocationRelativeTo(parent);
    }

    /**
     * Shows the log of a tab and follows it from now on.
     * <p>
     * 탭의 로그를 보이고 이제부터 그것을 따라간다.
     *
     * @param next  the log, or null for none / 로그. 없으면 null
     * @param title what names the page of the log / 로그의 페이지를 부르는 글
     */
    void showLog(ConsoleLog next, String title) {
        if (log != next) {
            if (log != null) {
                log.removeListener(this);
            }
            log = next;
            if (log != null) {
                log.addListener(this);
            }
            refill();
        }
        owner.setText(title == null || title.length() == 0 ? " " : title);
    }

    /**
     * Returns the lowest rank that the list chooses.
     * <p>
     * 목록이 고른 최소 순위를 반환한다.
     *
     * @return {@link ConsoleLog#DEBUG}, {@link ConsoleLog#LOG}, {@link ConsoleLog#WARN} or {@link ConsoleLog#ERROR} /
     *         {@link ConsoleLog#DEBUG}, {@link ConsoleLog#LOG}, {@link ConsoleLog#WARN}, {@link ConsoleLog#ERROR} 중 하나
     */
    private int minimumRank() {
        return Math.max(0, levels.getSelectedIndex());
    }

    /**
     * Writes the whole log into the pane again.
     * <p>
     * 로그 전체를 창에 다시 쓴다.
     */
    private void refill() {
        pane.setText("");
        if (log == null) {
            return;
        }
        if (log.getDroppedCount() > 0) {
            append(new ConsoleLog.Entry("info", Texts.of("(" + log.getDroppedCount() + " older messages were dropped)",
                    "(오래된 메시지 " + log.getDroppedCount() + "개를 버렸습니다)"), null));
        }
        List<ConsoleLog.Entry> entries = log.getEntries(minimumRank());
        // 항목마다 한 줄을 쓴다.
        for (int i = 0; i < entries.size(); i++) {
            append(entries.get(i));
        }
    }

    /**
     * Appends one message as a line in the color of its level.
     * <p>
     * 메시지 하나를 그 수준의 색으로 한 줄 덧붙인다.
     *
     * @param entry the message / 메시지
     */
    private void append(ConsoleLog.Entry entry) {
        SimpleAttributeSet style = new SimpleAttributeSet();
        int rank = entry.getRank();
        // 수준에 따라 글자색을 고른다.
        if (rank == ConsoleLog.ERROR) {
            StyleConstants.setForeground(style, new Color(0xB00020));
        } else if (rank == ConsoleLog.WARN) {
            StyleConstants.setForeground(style, new Color(0x9A5B00));
        } else if (rank == ConsoleLog.DEBUG) {
            StyleConstants.setForeground(style, Color.GRAY);
        }
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), entry.toString() + "\n", style);
        } catch (BadLocationException e) {
            // 문서의 끝에 넣으므로 일어나지 않는다. 일어나면 그 줄만 빠진다.
            return;
        }
        pane.setCaretPosition(doc.getLength());
    }

    /**
     * Adds the line of a new message if its level is shown.
     * <p>
     * 새 메시지의 수준을 보이는 중이면 그 줄을 더한다.
     *
     * @param source the log / 로그
     * @param entry  the new message / 새 메시지
     */
    public void entryAdded(ConsoleLog source, ConsoleLog.Entry entry) {
        // 로그가 가득 차서 옛 메시지를 버리기 시작했으면 처음부터 다시 쓴다.
        if (source.getDroppedCount() > 0) {
            refill();
        } else if (entry.getRank() >= minimumRank()) {
            append(entry);
        }
    }

    /**
     * Empties the pane with the log.
     * <p>
     * 로그와 함께 창을 비운다.
     *
     * @param source the log / 로그
     */
    public void logCleared(ConsoleLog source) {
        refill();
    }

    /**
     * Stops following the log and closes the window.
     * <p>
     * 로그를 따라가기를 멈추고 창을 닫는다.
     */
    void close() {
        showLog(null, null);
        dispose();
    }

    /**
     * Returns the text that is shown; for tests.
     * <p>
     * 보이는 글을 반환한다. 시험용이다.
     *
     * @return the lines of the pane / 창의 줄들
     */
    String getShownText() {
        StyledDocument doc = pane.getStyledDocument();
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return "";
        }
    }

    /**
     * Returns the list of levels; for tests.
     * <p>
     * 수준 목록을 반환한다. 시험용이다.
     *
     * @return the list / 목록
     */
    JComboBox getLevelBox() {
        return levels;
    }

    /**
     * Returns the text that names the page; for tests.
     * <p>
     * 페이지를 부르는 글을 반환한다. 시험용이다.
     *
     * @return the text / 글
     */
    String getOwnerText() {
        return owner.getText().trim();
    }
}
