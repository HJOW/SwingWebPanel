package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * The history window of a browser window (plan item P31-09): the addresses of the session history of the active tab, the oldest first,
 * with the entry that is shown marked. A double click or the button goes to the chosen entry by going back or forward that many
 * entries. It follows the active tab while it is open. Only the history of the session is shown; the browser keeps no history after it
 * ends.
 * <p>
 * 브라우저 창의 방문 기록 창(계획 P31-09): 활성 탭의 세션 이력 주소를 오래된 것부터 보이고, 지금 보이는 항목을 표시한다. 두 번 누르거나 단추를
 * 누르면 그만큼 뒤로 또는 앞으로 가서 고른 항목으로 간다. 열려 있는 동안 활성 탭을 따라간다. 세션의 이력만 보인다. 브라우저는 끝난 뒤의 방문
 * 기록을 남기지 않는다.
 */
final class HistoryDialog extends JDialog {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /** The mark in front of the entry that is shown. / 지금 보이는 항목 앞의 표시. */
    private static final String CURRENT = "> ";

    /** The space in front of the other entries. / 다른 항목 앞의 빈칸. */
    private static final String OTHER = "  ";

    /** The model of the list. / 목록의 모델. */
    private final DefaultListModel model = new DefaultListModel();

    /** The list of entries. / 항목 목록. */
    private final JList list = new JList(model);

    /** Tells which tab the history belongs to. / 이력이 어느 탭의 것인지 알린다. */
    private final JLabel owner = new JLabel(" ");

    /** The tab whose history is shown, or null. / 이력을 보이는 탭. 없으면 null. */
    private transient BrowserTab tab;

    /**
     * Creates the window; it is not shown yet.
     * <p>
     * 창을 만든다. 아직 보이지 않는다.
     *
     * @param parent the browser window / 브라우저 창
     */
    HistoryDialog(Window parent) {
        super(parent, Texts.of("History", "방문 기록"), ModalityType.MODELESS);
        setName("history-window");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        list.setName("history-list");
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addMouseListener(new MouseAdapter() {
            /**
             * Goes to the entry that was double clicked.
             * <p>
             * 두 번 누른 항목으로 간다.
             *
             * @param e the event / 이벤트
             */
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    go();
                }
            }
        });
        JButton go = new JButton(Texts.of("Go to page", "이 페이지로 이동"));
        go.setName("history-go");
        go.addActionListener(new ActionListener() {
            /**
             * Goes to the chosen entry.
             * <p>
             * 고른 항목으로 간다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                go();
            }
        });
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        bar.add(go);
        bar.add(owner);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JScrollPane(list), BorderLayout.CENTER);
        getContentPane().add(bar, BorderLayout.SOUTH);
        setSize(560, 360);
        setLocationRelativeTo(parent);
    }

    /**
     * Shows the history of a tab; called again whenever the tab changes.
     * <p>
     * 탭의 이력을 보인다. 탭이 바뀔 때마다 다시 호출된다.
     *
     * @param shown the tab, or null for none / 탭. 없으면 null
     */
    void showTab(BrowserTab shown) {
        tab = shown;
        int chosen = list.getSelectedIndex();
        model.clear();
        if (tab == null) {
            owner.setText(" ");
            return;
        }
        List<String> all = tab.getPanel().getHistories();
        int current = tab.getPanel().getHistoryIndex();
        // 항목마다 한 줄을 만들고 지금 보이는 항목을 표시한다.
        for (int i = 0; i < all.size(); i++) {
            model.addElement((i == current ? CURRENT : OTHER) + (i + 1) + ".  " + all.get(i));
        }
        owner.setText(tab.getTitleText());
        // 고른 줄이 여전히 있으면 그대로 두고, 아니면 지금 보이는 항목을 고른다.
        list.setSelectedIndex(chosen >= 0 && chosen < all.size() ? chosen : current);
    }

    /**
     * Goes to the chosen entry of the history.
     * <p>
     * 이력의 고른 항목으로 간다.
     */
    void go() {
        int index = list.getSelectedIndex();
        if (tab != null && index >= 0) {
            tab.goToHistory(index);
        }
    }

    /**
     * Returns the list; for tests.
     * <p>
     * 목록을 반환한다. 시험용이다.
     *
     * @return the list / 목록
     */
    JList getList() {
        return list;
    }

    /**
     * Returns the lines of the list; for tests.
     * <p>
     * 목록의 줄들을 반환한다. 시험용이다.
     *
     * @return one text per entry / 항목마다 글 하나
     */
    String[] getLines() {
        String[] out = new String[model.size()];
        // 모델의 줄을 차례로 옮긴다.
        for (int i = 0; i < out.length; i++) {
            out[i] = String.valueOf(model.get(i));
        }
        return out;
    }
}
