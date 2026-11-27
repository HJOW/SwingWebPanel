package org.duckdns.hjow.swingwebpanel.desktopapp.ui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.duckdns.hjow.swingwebpanel.FindResult;
import org.duckdns.hjow.swingwebpanel.desktopapp.Texts;

/**
 * The find bar of a browser window (plan item P31-09): a text field, buttons for the previous and the next match, a check box for
 * letter case, the number of the current match and the count, and a button that closes the bar. Typing searches at once; Enter goes
 * to the next match, Shift+Enter to the previous one and Esc closes the bar. The bar only reports what the user asks for; the window
 * runs the search in the page of the active tab.
 * <p>
 * 브라우저 창의 찾기 막대(계획 P31-09): 텍스트 필드, 이전·다음 일치 단추, 대소문자 구분 확인란, 현재 일치의 번호와 개수, 막대를 닫는 단추.
 * 입력하는 즉시 찾는다. Enter 는 다음 일치, Shift+Enter 는 이전 일치로 가고 Esc 는 막대를 닫는다. 막대는 사용자가 요청한 것을 알리기만 하고,
 * 찾기는 창이 활성 탭의 페이지에서 실행한다.
 */
final class FindBar extends JPanel {

    /** Serialization version. / 직렬화 버전. */
    private static final long serialVersionUID = 1L;

    /**
     * What the find bar asks its window to do.
     * <p>
     * 찾기 막대가 창에 요청하는 일.
     */
    interface Actions {

        /**
         * Searches the page, or goes to the next or previous match.
         * <p>
         * 페이지에서 찾거나, 다음 또는 이전 일치로 간다.
         *
         * @param text      the text of the field (may be empty, which ends the search) / 필드의 글(비어 있을 수 있고, 그러면 찾기가 끝난다)
         * @param matchCase true to tell upper and lower case apart / 대소문자를 구분하려면 true
         * @param forward   true for the next match / 다음 일치이면 true
         */
        void find(String text, boolean matchCase, boolean forward);

        /**
         * Called when the user closed the bar.
         * <p>
         * 사용자가 막대를 닫았을 때 호출된다.
         */
        void findClosed();
    }

    /** The window side. / 창 쪽. */
    private final transient Actions actions;

    /** The search text. / 검색어. */
    private final JTextField field = new JTextField(24);

    /** The check box for letter case. / 대소문자 구분 확인란. */
    private final JCheckBox matchCase = new JCheckBox(Texts.of("Match case", "대소문자 구분"));

    /** Shows the number of the current match and the count. / 현재 일치의 번호와 개수를 보인다. */
    private final JLabel count = new JLabel(" ");

    /** The background of the field when it is in its normal state. / 보통 상태일 때의 필드 배경. */
    private final Color normalBackground;

    /** True while the bar changes the field itself, so that the change does not start a search. / 막대가 스스로 필드를 바꾸는 동안 true. 그 변경은 찾기를 시작하지 않는다. */
    private boolean silent;

    /**
     * Creates a closed find bar.
     * <p>
     * 닫힌 찾기 막대를 만든다.
     *
     * @param actions the window side / 창 쪽
     */
    FindBar(Actions actions) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 2));
        this.actions = actions;
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        setName("find-bar");
        field.setName("find-text");
        BrowserTab.releaseCommandKeys(field);
        normalBackground = field.getBackground();
        JButton previous = button("find-previous", Texts.of("Previous", "이전"), new Runnable() {
            /** Goes to the previous match. / 이전 일치로 간다. */
            public void run() {
                search(false);
            }
        });
        JButton next = button("find-next", Texts.of("Next", "다음"), new Runnable() {
            /** Goes to the next match. / 다음 일치로 간다. */
            public void run() {
                search(true);
            }
        });
        JButton close = button("find-close", Texts.of("Close", "닫기"), new Runnable() {
            /** Closes the bar. / 막대를 닫는다. */
            public void run() {
                close();
            }
        });
        matchCase.setName("find-match-case");
        matchCase.setFocusable(false);
        matchCase.addActionListener(new ActionListener() {
            /**
             * Searches again with the new choice.
             * <p>
             * 새 선택으로 다시 찾는다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                search(true);
            }
        });
        count.setName("find-count");
        add(new JLabel(Texts.of("Find:", "찾기:")));
        add(field);
        add(previous);
        add(next);
        add(matchCase);
        add(count);
        add(close);
        field.getDocument().addDocumentListener(new DocumentListener() {
            /**
             * Searches for the longer text.
             * <p>
             * 길어진 글을 찾는다.
             *
             * @param e the event / 이벤트
             */
            public void insertUpdate(DocumentEvent e) {
                typed();
            }

            /**
             * Searches for the shorter text.
             * <p>
             * 짧아진 글을 찾는다.
             *
             * @param e the event / 이벤트
             */
            public void removeUpdate(DocumentEvent e) {
                typed();
            }

            /**
             * Does nothing: a plain text field has no attribute changes.
             * <p>
             * 아무것도 하지 않는다: 보통의 텍스트 필드에는 속성 변경이 없다.
             *
             * @param e the event / 이벤트
             */
            public void changedUpdate(DocumentEvent e) {
                return;
            }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "find-enter", new Runnable() {
            /** Goes to the next match. / 다음 일치로 간다. */
            public void run() {
                search(true);
            }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK), "find-shift-enter", new Runnable() {
            /** Goes to the previous match. / 이전 일치로 간다. */
            public void run() {
                search(false);
            }
        });
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "find-escape", new Runnable() {
            /** Closes the bar. / 막대를 닫는다. */
            public void run() {
                close();
            }
        });
        setVisible(false);
    }

    /**
     * Creates a small button of the bar.
     * <p>
     * 막대의 작은 단추를 만든다.
     *
     * @param name the name for the tests / 시험을 위한 이름
     * @param text the text / 글
     * @param work what the button does / 단추가 하는 일
     * @return the button / 단추
     */
    private static JButton button(String name, String text, final Runnable work) {
        JButton b = new JButton(text);
        b.setName(name);
        b.setFocusable(false);
        b.setMargin(new java.awt.Insets(1, 8, 1, 8));
        b.addActionListener(new ActionListener() {
            /**
             * Does the work of the button.
             * <p>
             * 단추의 일을 한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                work.run();
            }
        });
        return b;
    }

    /**
     * Binds a key of the text field.
     * <p>
     * 텍스트 필드의 키를 묶는다.
     *
     * @param key  the key / 키
     * @param name the name of the action / 동작의 이름
     * @param work what the key does / 키가 하는 일
     */
    private void bind(KeyStroke key, String name, final Runnable work) {
        field.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
        field.getActionMap().put(name, new AbstractAction() {
            /** Serialization version. / 직렬화 버전. */
            private static final long serialVersionUID = 1L;

            /**
             * Does the work of the key.
             * <p>
             * 키의 일을 한다.
             *
             * @param e the event / 이벤트
             */
            public void actionPerformed(ActionEvent e) {
                work.run();
            }
        });
    }

    /**
     * Searches for the text that the user just changed.
     * <p>
     * 사용자가 방금 바꾼 글을 찾는다.
     */
    private void typed() {
        if (!silent && isVisible()) {
            actions.find(field.getText(), matchCase.isSelected(), true);
        }
    }

    /**
     * Asks the window to search in a direction.
     * <p>
     * 한 방향으로 찾도록 창에 요청한다.
     *
     * @param forward true for the next match / 다음 일치이면 true
     */
    void search(boolean forward) {
        actions.find(field.getText(), matchCase.isSelected(), forward);
    }

    /**
     * Shows the bar and puts the keyboard focus into the text field with its text selected, so that typing replaces it.
     * <p>
     * 막대를 보이고, 글을 선택한 채 텍스트 필드에 키보드 포커스를 둔다. 그래서 입력하면 그 글이 바뀐다.
     */
    void open() {
        if (!isVisible()) {
            setVisible(true);
            revalidate();
        }
        field.requestFocusInWindow();
        field.selectAll();
        // 이전에 찾던 글이 남아 있으면 그 글로 바로 다시 찾는다.
        if (field.getText().length() > 0) {
            actions.find(field.getText(), matchCase.isSelected(), true);
        }
    }

    /**
     * Hides the bar and tells the window, which ends the search. The text stays for the next time.
     * <p>
     * 막대를 숨기고 창에 알린다. 창이 찾기를 끝낸다. 글은 다음을 위해 남는다.
     */
    void close() {
        if (isVisible()) {
            setVisible(false);
            revalidate();
            actions.findClosed();
        }
    }

    /**
     * Tells whether the bar is shown.
     * <p>
     * 막대가 보이는지 알려 준다.
     *
     * @return true while open / 열려 있는 동안 true
     */
    boolean isOpen() {
        return isVisible();
    }

    /**
     * Returns the search text.
     * <p>
     * 검색어를 반환한다.
     *
     * @return the text of the field / 필드의 글
     */
    String getText() {
        return field.getText();
    }

    /**
     * Sets the search text without starting a search.
     * <p>
     * 찾기를 시작하지 않고 검색어를 정한다.
     *
     * @param text the text / 글
     */
    void setText(String text) {
        silent = true;
        try {
            field.setText(text == null ? "" : text);
        } finally {
            silent = false;
        }
    }

    /**
     * Tells whether the search tells upper and lower case apart.
     * <p>
     * 찾기가 대소문자를 구분하는지 알려 준다.
     *
     * @return true if the check box is selected / 확인란이 선택되어 있으면 true
     */
    boolean isMatchCase() {
        return matchCase.isSelected();
    }

    /**
     * Shows the result of the search of the active page: the number of the current match and the count, or that nothing was found.
     * <p>
     * 활성 페이지의 찾기 결과를 보인다: 현재 일치의 번호와 개수, 또는 찾지 못했음.
     *
     * @param result the result / 결과
     */
    void showResult(FindResult result) {
        boolean missing = result.isActive() && result.getCount() == 0;
        if (!result.isActive()) {
            count.setText(" ");
        } else if (missing) {
            count.setText(Texts.of("No matches", "일치 없음"));
        } else {
            count.setText(result.getIndex() + " / " + result.getCount());
        }
        // 찾지 못한 글은 필드의 바탕색으로도 알린다.
        field.setBackground(missing ? new Color(0xFFD9D9) : normalBackground);
    }

    /**
     * Returns the text that shows the count; for tests.
     * <p>
     * 개수를 보이는 글을 반환한다. 시험용이다.
     *
     * @return the text / 글
     */
    String getCountText() {
        return count.getText().trim();
    }

    /**
     * Returns the text field; for the window and for tests.
     * <p>
     * 텍스트 필드를 반환한다. 창과 시험이 쓴다.
     *
     * @return the field / 필드
     */
    JTextField getField() {
        return field;
    }

    /**
     * Returns the check box for letter case; for tests.
     * <p>
     * 대소문자 구분 확인란을 반환한다. 시험용이다.
     *
     * @return the check box / 확인란
     */
    JCheckBox getMatchCaseBox() {
        return matchCase;
    }
}
