package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;

/**
 * The drop-down list of a <code>select</code> element: a Swing list in a scroll pane that is a child of the page component (not a window, so it
 * works wherever the page works), placed under the control, with the rows of the options. The mouse picks a row (the row under the pointer is
 * highlighted), the arrow keys move, Enter and the space key pick, Escape and Tab close the list without a choice, and so does the loss of the
 * keyboard focus. Rows that are disabled or are group labels cannot be picked. It runs on the EDT.
 * <p>
 * <code>select</code> 요소의 드롭다운 목록: 페이지 컴포넌트의 자식인 스크롤 창 안의 Swing 목록(창이 아니므로 페이지가 동작하는 곳이면 어디서든 동작한다)이며
 * 컨트롤 아래에 option 행과 함께 놓인다. 마우스가 행을 고르고(포인터 아래 행이 강조된다) 화살표 키가 움직이고 Enter 와 스페이스 키가 고르며, Escape 와 Tab 은 선택 없이
 * 목록을 닫고 키보드 포커스를 잃어도 닫힌다. 비활성이거나 그룹 라벨인 행은 고를 수 없다. EDT 에서 실행된다.
 */
final class ControlPicker {

    /** The most rows that the list shows without scrolling. 스크롤 없이 목록이 보여 주는 최대 행 수. */
    private static final int MAX_ROWS = 12;

    /** What to choose and whom to tell. 고를 대상과 알릴 곳. */
    private final PickSpec spec;

    /** The list of rows. 행의 목록. */
    private final JList list;

    /** The scroll pane that is added to the page. 페이지에 더해지는 스크롤 창. */
    private final JScrollPane root;

    /** True once the list was closed or a row was picked (it reports once). 목록이 닫혔거나 행을 골랐으면 true(한 번만 알린다). */
    private boolean finished;

    /**
     * Creates the list of a select element; it is not added to the page yet.
     * <p>
     * select 요소의 목록을 만든다. 아직 페이지에 더하지 않는다.
     *
     * @param spec  what to choose / 고를 대상
     * @param focus receives the focus events of the list (they are focus events of the page) / 목록의 포커스 이벤트(페이지의 포커스 이벤트이다)를 받는다
     */
    ControlPicker(final PickSpec spec, FocusListener focus) {
        this(spec, focus, 1.0);
    }

    /**
     * Creates the list of a select element for a page that is zoomed: the font and the rows are as many times larger as the zoom factor says.
     * <p>
     * 확대된 페이지를 위해 select 요소의 목록을 만든다: 글꼴과 행은 확대 배율만큼 커진다.
     *
     * @param spec  what to choose / 고를 대상
     * @param focus receives the focus events of the list / 목록의 포커스 이벤트를 받는다
     * @param zoom  the zoom factor of the page / 페이지의 확대 배율
     */
    ControlPicker(final PickSpec spec, FocusListener focus, double zoom) {
        this.spec = spec;
        final DefaultListModel model = new DefaultListModel();
        // 행의 글을 모델에 넣는다.
        for (int i = 0; i < spec.labels.length; i++) {
            model.addElement(spec.labels[i]);
        }
        list = new JList(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFocusTraversalKeysEnabled(false);
        ControlSnapshot c = spec.control;
        Font font = zoom == 1.0 ? c.getFont() : c.getFont().deriveFont((float) (c.getFont().getSize2D() * zoom));
        list.setFont(font);
        list.setForeground(new Color(c.getColor(), true));
        list.setBackground(Color.WHITE);
        int rowHeight = (int) Math.ceil(Math.max(c.getLineHeight() * zoom, font.getSize2D() * 1.2)) + 2;
        list.setFixedCellHeight(rowHeight);
        list.setCellRenderer(new Rows());
        if (spec.selected >= 0) {
            list.setSelectedIndex(spec.selected);
            list.ensureIndexIsVisible(spec.selected);
        }
        list.addFocusListener(focus);
        list.addFocusListener(new FocusListener() {
            /**
             * The list got the focus (nothing to do).
             * <p>
             * 목록이 포커스를 얻었다(할 일이 없다).
             *
             * @param e the event / 이벤트
             */
            public void focusGained(FocusEvent e) {
                // 포커스를 얻을 때는 처리할 것이 없다.
            }

            /**
             * Closes the list when the focus leaves the page component and its children.
             * <p>
             * 포커스가 페이지 컴포넌트와 그 자식을 떠나면 목록을 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void focusLost(FocusEvent e) {
                Component other = e.getOppositeComponent();
                if (!e.isTemporary() && (other == null || !SwingUtilities.isDescendingFrom(other, root))) {
                    cancel();
                }
            }
        });
        list.addMouseMotionListener(new MouseAdapter() {
            /**
             * Highlights the row under the pointer.
             * <p>
             * 포인터 아래의 행을 강조한다.
             *
             * @param e the event / 이벤트
             */
            public void mouseMoved(MouseEvent e) {
                int row = rowAt(e);
                if (row >= 0) {
                    list.setSelectedIndex(row);
                }
            }
        });
        list.addMouseListener(new MouseAdapter() {
            /**
             * Picks the row under the pointer.
             * <p>
             * 포인터 아래의 행을 고른다.
             *
             * @param e the event / 이벤트
             */
            public void mouseReleased(MouseEvent e) {
                int row = rowAt(e);
                if (row >= 0) {
                    pick(row);
                }
            }
        });
        list.addKeyListener(new KeyAdapter() {
            /**
             * Enter and space pick the highlighted row; Escape and Tab close the list.
             * <p>
             * Enter 와 스페이스는 강조된 행을 고르고, Escape 와 Tab 은 목록을 닫는다.
             *
             * @param e the event / 이벤트
             */
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
                    pick(list.getSelectedIndex());
                    e.consume();
                } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_TAB) {
                    cancel();
                    e.consume();
                }
            }
        });
        root = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        root.setBorder(BorderFactory.createLineBorder(new Color(0x767676)));
        root.setName("select-list");
        Dimension pref = list.getPreferredSize();
        int rows = Math.min(MAX_ROWS, Math.max(1, spec.labels.length));
        int width = Math.max((int) Math.ceil(c.getBorderBox()[2] * zoom), pref.width + 8 + (spec.labels.length > MAX_ROWS ? 18 : 0));
        root.setSize(width, rows * rowHeight + 2);
        // 아직 화면에 보이지 않는 컴포넌트도 행의 위치를 알 수 있게 미리 배치해 둔다.
        root.validate();
    }

    /**
     * Returns the row under a mouse event that can be picked, or -1.
     * <p>
     * 마우스 이벤트 아래에 있는 고를 수 있는 행을 반환한다. 없으면 -1.
     *
     * @param e the event / 이벤트
     * @return the row or -1 / 행. 없으면 -1
     */
    private int rowAt(MouseEvent e) {
        int row = list.locationToIndex(e.getPoint());
        java.awt.Rectangle cell = row < 0 ? null : list.getCellBounds(row, row);
        if (cell == null || e.getY() < cell.y || e.getY() >= cell.y + cell.height || !spec.enabled[row] || spec.group[row]) {
            return -1;
        }
        return row;
    }

    /**
     * Picks a row, if it can be picked, and tells the listener once.
     * <p>
     * 고를 수 있는 행이면 고르고 리스너에 한 번 알린다.
     *
     * @param row the row, or -1 for none / 행. 없으면 -1
     */
    private void pick(int row) {
        if (finished || row < 0 || row >= spec.labels.length || !spec.enabled[row] || spec.group[row]) {
            return;
        }
        finished = true;
        spec.listener.picked(row);
    }

    /**
     * Closes the list without a choice and tells the listener once.
     * <p>
     * 선택 없이 목록을 닫고 리스너에 한 번 알린다.
     */
    private void cancel() {
        if (!finished) {
            finished = true;
            spec.listener.cancelled();
        }
    }

    /**
     * Returns the component to add to the page.
     * <p>
     * 페이지에 더할 컴포넌트를 반환한다.
     *
     * @return the scroll pane / 스크롤 창
     */
    JComponent getComponent() {
        return root;
    }

    /**
     * Returns the list of rows (for tests and hosts).
     * <p>
     * 행의 목록을 반환한다(시험과 호스트용).
     *
     * @return the list / 목록
     */
    JList getList() {
        return list;
    }

    /**
     * Returns the specification of the list.
     * <p>
     * 목록의 명세를 반환한다.
     *
     * @return the specification / 명세
     */
    PickSpec getSpec() {
        return spec;
    }

    /**
     * Marks the list as closed so that it reports nothing any more (the page removes it).
     * <p>
     * 목록을 닫힌 것으로 표시해 더는 아무것도 알리지 않게 한다(페이지가 없앤다).
     */
    void dispose() {
        finished = true;
    }

    /**
     * Draws the rows: group labels in bold, disabled rows in gray, an indentation inside option groups.
     * <p>
     * 행을 그린다: 그룹 라벨은 굵게, 비활성 행은 회색, optgroup 안은 들여쓰기.
     */
    private final class Rows extends javax.swing.DefaultListCellRenderer {

        /** The serial version id. 직렬화 버전 식별자. */
        private static final long serialVersionUID = 1L;

        /**
         * Creates the renderer.
         * <p>
         * 렌더러를 만든다.
         */
        Rows() {
        }

        /**
         * Makes the component that draws a row.
         * <p>
         * 행을 그리는 컴포넌트를 만든다.
         *
         * @param l        the list / 목록
         * @param value    the text of the row / 행의 글
         * @param index    the index of the row / 행의 인덱스
         * @param selected true if the row is highlighted / 행이 강조되었으면 true
         * @param focus    true if the row has the focus / 행이 포커스를 가졌으면 true
         * @return the component / 컴포넌트
         */
        public Component getListCellRendererComponent(JList l, Object value, int index, boolean selected, boolean focus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(l, value, index, selected && spec.enabled[index], false);
            label.setBorder(BorderFactory.createEmptyBorder(0, 4 + 12 * spec.indent[index], 0, 4));
            if (spec.group[index]) {
                label.setFont(l.getFont().deriveFont(Font.BOLD));
            } else {
                label.setFont(l.getFont());
            }
            if (!spec.enabled[index] && !spec.group[index]) {
                label.setForeground(new Color(0x808080));
            }
            return label;
        }
    }
}
