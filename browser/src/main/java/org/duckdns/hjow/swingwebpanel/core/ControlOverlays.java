package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JColorChooser;
import javax.swing.JList;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * The Swing components that the page puts over its form controls and the dialogs that the controls need: the editor over the text field or
 * text area that has the focus ({@link ControlEditor}), the bubble that shows a validation message next to a control, and the file chooser of
 * a file upload control. The components are children of the page component, so they scroll with the page and their coordinates are those of
 * the document. Everything here runs on the event dispatch thread (the form host of the document posts its work to it), and
 * {@link #clear()} removes every component when the page is replaced, cleaned or disposed.
 * <p>
 * 페이지가 폼 컨트롤 위에 얹는 Swing 컴포넌트와 컨트롤이 필요로 하는 대화상자: 포커스를 가진 텍스트 필드나 텍스트 영역 위의 편집기({@link ControlEditor}), 컨트롤
 * 옆에 검증 메시지를 보여 주는 말풍선, 파일 업로드 컨트롤의 파일 선택기. 컴포넌트는 페이지 컴포넌트의 자식이므로 페이지와 함께 스크롤되고 좌표는 문서의 좌표이다.
 * 여기의 모든 것은 이벤트 디스패치 스레드에서 실행되며(문서의 폼 호스트가 작업을 그곳에 넘긴다), 페이지가 교체, clean, dispose 될 때 {@link #clear()} 가 모든
 * 컴포넌트를 없앤다.
 */
public final class ControlOverlays {

    /** How long a validation bubble stays, in milliseconds. 검증 말풍선이 머무는 시간(밀리초). */
    private static final int BUBBLE_MILLIS = 5000;

    /** How long a mouse press counts as the cause of an editor that opens, in milliseconds. 마우스 누름이 열리는 편집기의 원인으로 인정되는 시간(밀리초). */
    private static final int PRESS_MILLIS = 1000;

    /** The page component that the overlays are children of. 오버레이가 자식으로 속하는 페이지 컴포넌트. */
    private final JComponent panel;

    /** The editors by element. 요소별 편집기. */
    private final Map<Element, ControlEditor> editors = new IdentityHashMap<Element, ControlEditor>();

    /** The listener of the key and focus events of the editors (the page keyboard). 편집기의 키와 포커스 이벤트 리스너(페이지 키보드). */
    private PageKeyboard keyboard;

    /** Where the editors write copied text, or null. 편집기가 복사한 글을 쓰는 곳. 없으면 null. */
    private ClipboardSink clipboardSink;

    /** Where the editors read pasted data, or null. 편집기가 붙여넣을 데이터를 읽는 곳. 없으면 null. */
    private ClipboardSource clipboardSource;

    /** Lets the user choose files. 사용자가 파일을 고르게 한다. */
    private UploadChooser chooser = new SwingChooser();

    /** Lets the user choose a color. 사용자가 색을 고르게 한다. */
    private ColorChooser colors = new SwingColors();

    /** The drop-down list that is open, or null. 열려 있는 드롭다운 목록. 없으면 null. */
    private ControlPicker picker;

    /** The bubble that shows a validation message, or null. 검증 메시지를 보여 주는 말풍선. 없으면 null. */
    private JLabel bubble;

    /** Hides the bubble after a while. 잠시 뒤 말풍선을 숨긴다. */
    private Timer bubbleTimer;

    /** Where the mouse was pressed last (in page coordinates), or null. 마우스를 마지막으로 누른 곳(페이지 좌표). 없으면 null. */
    private Point lastPress;

    /** When the mouse was pressed last. 마우스를 마지막으로 누른 시각. */
    private long lastPressTime;

    /** Called after every layout of the overlays. 오버레이를 다시 놓을 때마다 호출된다. */
    private final List<Runnable> observers = new ArrayList<Runnable>();

    /** The zoom factor of the page: the overlays are placed in component pixels, which are the CSS pixels times this factor. 페이지의 확대 배율: 오버레이는 컴포넌트 픽셀로 놓이며 이는 CSS 픽셀에 이 배율을 곱한 것이다. */
    private double zoom = 1.0;

    /**
     * Creates the overlays of a page component.
     * <p>
     * 페이지 컴포넌트의 오버레이를 만든다.
     *
     * @param panel the page component, whose layout manager must be null (the children are placed by their bounds) / 페이지 컴포넌트. 배치 관리자가 null 이어야 한다
     *              (자식은 bounds 로 놓인다)
     */
    public ControlOverlays(JComponent panel) {
        if (panel == null) {
            throw new IllegalArgumentException("panel must not be null");
        }
        this.panel = panel;
    }

    /**
     * Sets the zoom factor of the page; the editors that are open are placed again by the next {@link #layout(RenderedPage)}.
     * <p>
     * 페이지의 확대 배율을 정한다. 열려 있는 편집기는 다음 {@link #layout(RenderedPage)} 에서 다시 놓인다.
     *
     * @param factor the factor, positive / 배율. 양수
     */
    public void setZoom(double factor) {
        this.zoom = factor;
    }

    /**
     * Sets the page keyboard that receives the key and focus events of the editors: they are the events of the page.
     * <p>
     * 편집기의 키와 포커스 이벤트를 받는 페이지 키보드를 지정한다. 그 이벤트는 페이지의 이벤트이다.
     *
     * @param keyboard the page keyboard / 페이지 키보드
     */
    public void setKeyboard(PageKeyboard keyboard) {
        this.keyboard = keyboard;
    }

    /**
     * Sets the clipboard of the editors: where they write what the user copies or cuts, and where they read what the user pastes (the
     * clipboard of the component, which a host may replace). Without it the editors use the system clipboard of Swing.
     * <p>
     * 편집기의 클립보드를 정한다: 사용자가 복사하거나 잘라낸 것을 쓰는 곳과 붙여넣을 것을 읽는 곳(컴포넌트의 클립보드이며 호스트가 바꿀 수 있다). 없으면 편집기는
     * Swing 의 시스템 클립보드를 쓴다.
     *
     * @param to   where copied text is written, or null / 복사한 텍스트를 쓰는 곳. 없으면 null
     * @param from where pasted data is read, or null / 붙여넣을 데이터를 읽는 곳. 없으면 null
     */
    public void setClipboard(ClipboardSink to, ClipboardSource from) {
        this.clipboardSink = to;
        this.clipboardSource = from;
    }

    /**
     * Replaces the chooser of upload files (the default shows a Swing file chooser). A host or a test chooses files without a dialog with it.
     * <p>
     * 업로드 파일의 선택기를 바꾼다(기본은 Swing 파일 선택기를 보여 준다). 호스트나 시험은 이것으로 대화상자 없이 파일을 고른다.
     *
     * @param chooser the chooser, or null for the default / 선택기. 기본을 쓰려면 null
     */
    public void setUploadChooser(UploadChooser chooser) {
        this.chooser = chooser == null ? new SwingChooser() : chooser;
    }

    /**
     * Replaces the chooser of colors (the default shows a Swing color chooser dialog).
     * <p>
     * 색 선택기를 바꾼다(기본은 Swing 색 선택 대화상자를 보여 준다).
     *
     * @param chooser the chooser, or null for the default / 선택기. 기본을 쓰려면 null
     */
    public void setColorChooser(ColorChooser chooser) {
        this.colors = chooser == null ? new SwingColors() : chooser;
    }

    /**
     * Asks the user for a color; the calling thread (the document thread) waits for the answer.
     * <p>
     * 사용자에게 색을 묻는다. 호출한 스레드(문서 스레드)는 답을 기다린다.
     *
     * @param rgb the color that the control has now / 컨트롤의 현재 색
     * @return the chosen color (0xRRGGBB) or null if the user cancelled / 고른 색(0xRRGGBB). 사용자가 취소했으면 null
     */
    Integer chooseColor(final int rgb) {
        final ColorChooser c = colors;
        if (SwingUtilities.isEventDispatchThread()) {
            return c.choose(rgb);
        }
        final Integer[] result = new Integer[1];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    result[0] = c.choose(rgb);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
        return result[0];
    }

    /**
     * Opens the drop-down list of a select element under its control, with the keyboard focus. A list that is already open closes first.
     * <p>
     * select 요소의 드롭다운 목록을 컨트롤 아래에 키보드 포커스와 함께 연다. 이미 열려 있는 목록은 먼저 닫는다.
     *
     * @param spec what to choose / 고를 대상
     */
    void openPicker(PickSpec spec) {
        if (!spec.listener.isCurrent()) {
            return;
        }
        closePicker();
        double z = zoom;
        ControlPicker p = new ControlPicker(spec, keyboard, z);
        picker = p;
        double[] b = spec.control.getBorderBox();
        JComponent c = p.getComponent();
        int x = (int) Math.round(b[0] * z);
        int y = (int) Math.round((b[1] + b[3]) * z);
        // 아래에 자리가 모자라면 컨트롤 위로, 위에도 모자라면 페이지 아래쪽에 맞춘다.
        if (y + c.getHeight() > panel.getHeight()) {
            y = b[1] * z - c.getHeight() >= 0 ? (int) Math.round(b[1] * z) - c.getHeight() : Math.max(0, panel.getHeight() - c.getHeight());
        }
        c.setLocation(x, y);
        panel.add(c);
        panel.setComponentZOrder(c, 0);
        panel.repaint(c.getBounds());
        p.getList().requestFocusInWindow();
    }

    /**
     * Closes the drop-down list that is open; the keyboard focus goes back to the page component if the list had it.
     * <p>
     * 열려 있는 드롭다운 목록을 닫는다. 목록이 키보드 포커스를 가졌으면 페이지 컴포넌트로 돌려준다.
     */
    void closePicker() {
        ControlPicker p = picker;
        if (p != null) {
            picker = null;
            boolean focused = p.getList().isFocusOwner();
            p.dispose();
            Rectangle bounds = p.getComponent().getBounds();
            panel.remove(p.getComponent());
            panel.repaint(bounds);
            if (focused) {
                panel.requestFocusInWindow();
            }
        }
    }

    /**
     * Returns the list of the drop-down that is open, or null (for tests).
     * <p>
     * 열려 있는 드롭다운의 목록을 반환한다. 없으면 null(시험용).
     *
     * @return the list or null / 목록 또는 null
     */
    public JList getPicker() {
        return picker == null ? null : picker.getList();
    }

    /**
     * Shows the default file chooser
 (a Swing dialog over the window of the page component) and returns the files that the user chose. It
     * must be called on the event dispatch thread and does nothing (returns null) without a screen.
     * <p>
     * 기본 파일 선택기(페이지 컴포넌트의 창 위의 Swing 대화상자)를 보여 주고 사용자가 고른 파일을 반환한다. 이벤트 디스패치 스레드에서 호출해야 하며 화면이 없으면
     * 아무것도 하지 않는다(null 반환).
     *
     * @param multiple true if several files are accepted / 여러 파일을 받으면 true
     * @param accept   the <code>accept</code> attribute / <code>accept</code> 속성
     * @return the files or null if the user cancelled / 파일. 사용자가 취소했으면 null
     */
    public File[] showFileChooser(boolean multiple, String accept) {
        return new SwingChooser().choose(multiple, accept);
    }

    /**
     * Returns a mouse listener that the page component must have: it records where the user pressed the mouse button, so that the editor that
     * opens because of the press puts its caret there.
     * <p>
     * 페이지 컴포넌트가 가져야 하는 마우스 리스너를 반환한다. 사용자가 마우스 버튼을 누른 곳을 기록해, 그 누름으로 열리는 편집기가 캐럿을 그곳에 놓게 한다.
     *
     * @return the listener / 리스너
     */
    public MouseListener getMouseListener() {
        return new MouseAdapter() {
            /**
             * Records the press.
             * <p>
             * 누름을 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void mousePressed(MouseEvent e) {
                lastPress = e.getPoint();
                lastPressTime = System.currentTimeMillis();
                hideMessage();
            }
        };
    }

    /**
     * Adds a function that is called after every layout of the overlays (that is, after every new rendering of the page).
     * <p>
     * 오버레이를 다시 놓을 때마다(곧 페이지가 새로 렌더링될 때마다) 호출되는 함수를 더한다.
     *
     * @param observer the function / 함수
     */
    void addLayoutObserver(Runnable observer) {
        observers.add(observer);
    }

    /**
     * Removes a function that was added with {@link #addLayoutObserver(Runnable)}.
     * <p>
     * {@link #addLayoutObserver(Runnable)} 로 더한 함수를 뺀다.
     *
     * @param observer the function / 함수
     */
    void removeLayoutObserver(Runnable observer) {
        observers.remove(observer);
    }

    /**
     * Opens the editor of a text control over its box and gives it the keyboard focus.
     * <p>
     * 텍스트 컨트롤의 편집기를 그 상자 위에 열고 키보드 포커스를 준다.
     *
     * @param spec what to edit / 편집할 대상
     */
    void open(EditSpec spec) {
        if (!spec.listener.isCurrent()) {
            return;
        }
        ControlEditor old = editors.get(spec.element);
        if (old != null) {
            remove(old, false);
        }
        ControlEditor editor = new ControlEditor(spec, keyboard, keyboard);
        editor.setClipboard(clipboardSink, clipboardSource);
        editors.put(spec.element, editor);
        editor.place(spec.control, zoom);
        panel.add(editor.getComponent());
        panel.setComponentZOrder(editor.getComponent(), 0);
        // 방금 마우스를 눌러서 열렸다면 누른 곳에 캐럿을 놓는다.
        if (lastPress != null && System.currentTimeMillis() - lastPressTime < PRESS_MILLIS && editor.getComponent().getBounds().contains(lastPress)) {
            editor.placeCaretAt(lastPress.x, lastPress.y);
        }
        lastPress = null;
        editor.getTextComponent().requestFocusInWindow();
        panel.repaint(editor.getComponent().getBounds());
    }

    /**
     * Gives an open editor the new state of the DOM.
     * <p>
     * 열려 있는 편집기에 DOM 의 새 상태를 준다.
     *
     * @param element   the element / 요소
     * @param text      the text of the DOM or null to keep the text of the editor / DOM 의 글. 편집기의 글을 유지하려면 null
     * @param start     the selection start or -1 / 선택 시작. 없으면 -1
     * @param end       the selection end / 선택 끝
     * @param backward  true for a backward selection / 뒤쪽 방향 선택이면 true
     * @param readOnly  the read-only flag / 읽기 전용 플래그
     * @param disabled  the disabled flag / 비활성 플래그
     * @param maxLength the maximum length or -1 / 최대 길이 또는 -1
     */
    void sync(Element element, String text, int start, int end, boolean backward, boolean readOnly, boolean disabled, int maxLength) {
        ControlEditor editor = editors.get(element);
        if (editor != null) {
            editor.sync(text, start, end, backward, readOnly, disabled, maxLength);
        }
    }

    /**
     * Closes the editor of an element if it is open; the keyboard focus goes back to the page component if the editor had it.
     * <p>
     * 요소의 편집기가 열려 있으면 닫는다. 편집기가 키보드 포커스를 가지고 있었으면 페이지 컴포넌트로 돌려준다.
     *
     * @param element the element / 요소
     */
    void close(Element element) {
        ControlEditor editor = editors.get(element);
        if (editor != null) {
            remove(editor, true);
        }
    }

    /**
     * Removes an editor from the page.
     * <p>
     * 편집기를 페이지에서 없앤다.
     *
     * @param editor   the editor / 편집기
     * @param refocus  true to give the focus back to the page component if the editor had it / 편집기가 포커스를 가졌으면 페이지 컴포넌트로 돌려주려면 true
     */
    private void remove(ControlEditor editor, boolean refocus) {
        editors.remove(editor.getSpec().element);
        boolean focused = editor.getTextComponent().isFocusOwner();
        editor.dispose();
        Rectangle bounds = editor.getComponent().getBounds();
        panel.remove(editor.getComponent());
        panel.repaint(bounds);
        if (focused && refocus) {
            panel.requestFocusInWindow();
        }
    }

    /**
     * Removes every editor and the bubble (the page was replaced, cleaned or disposed).
     * <p>
     * 모든 편집기와 말풍선을 없앤다(페이지가 교체, clean, dispose 되었다).
     */
    public void clear() {
        // 열려 있는 편집기를 모두 닫는다(맵을 바꾸므로 복사본을 돈다).
        List<ControlEditor> all = new ArrayList<ControlEditor>(editors.values());
        for (ControlEditor editor : all) {
            remove(editor, true);
        }
        closePicker();
        hideMessage();
    }

    /**
     * Clears the overlays on the event dispatch thread (from any thread).
     * <p>
     * 이벤트 디스패치 스레드에서 오버레이를 없앤다(어느 스레드에서든 호출할 수 있다).
     */
    public void clearLater() {
        if (SwingUtilities.isEventDispatchThread()) {
            clear();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    clear();
                }
            });
        }
    }

    /**
     * Puts the editors over the controls as the new rendering of the page has them (a reflow moved them, the font or the color changed, or the
     * control has no box any more), then calls the observers.
     * <p>
     * 편집기를 새로 렌더링된 페이지에서 컨트롤이 가진 위치에 놓는다(다시 배치로 움직였거나, 글꼴이나 색이 바뀌었거나, 컨트롤에 상자가 없어졌다). 그다음 관찰자를
     * 호출한다.
     *
     * @param page the new rendering, may be null / 새 렌더링. null 가능
     */
    public void layout(RenderedPage page) {
        if (page != null) {
            // 열려 있는 편집기마다 새 렌더링에서 컨트롤의 상자를 찾아 그 위에 놓는다.
            for (ControlEditor editor : new ArrayList<ControlEditor>(editors.values())) {
                ControlSnapshot c = page.getControl(editor.getSpec().element);
                JComponent root = editor.getComponent();
                if (c == null) {
                    root.setVisible(false);
                } else {
                    root.setVisible(true);
                    editor.place(c, zoom);
                }
            }
        }
        // 관찰자 목록이 호출 중에 바뀔 수 있으므로 복사본을 호출한다.
        for (Runnable observer : new ArrayList<Runnable>(observers)) {
            observer.run();
        }
    }

    /**
     * Returns the text component of the editor that is open over an element (for tests and hosts).
     * <p>
     * 요소 위에 열려 있는 편집기의 텍스트 컴포넌트를 반환한다(시험과 호스트용).
     *
     * @param element the element / 요소
     * @return the text component or null / 텍스트 컴포넌트 또는 null
     */
    public JTextComponent getEditor(Element element) {
        ControlEditor editor = editors.get(element);
        return editor == null ? null : editor.getTextComponent();
    }

    /**
     * Returns the number of editors that are open.
     * <p>
     * 열려 있는 편집기의 수를 반환한다.
     *
     * @return the number / 수
     */
    public int getEditorCount() {
        return editors.size();
    }

    /**
     * Shows a validation message in a bubble under a box of the page and hides it after a few seconds, or when the user presses the mouse
     * button in the page.
     * <p>
     * 페이지의 상자 아래 말풍선에 검증 메시지를 보여 주고, 몇 초 뒤나 사용자가 페이지에서 마우스 버튼을 누르면 숨긴다.
     *
     * @param documentBox the border box of the control in document coordinates (CSS pixels) / 문서 좌표(CSS 픽셀)의 컨트롤 border box
     * @param message     the message / 메시지
     */
    void showMessage(Rectangle documentBox, String message) {
        hideMessage();
        double z = zoom;
        Rectangle box = z == 1.0 ? documentBox : new Rectangle((int) Math.round(documentBox.x * z), (int) Math.round(documentBox.y * z),
                (int) Math.round(documentBox.width * z), (int) Math.round(documentBox.height * z));
        JLabel label = new JLabel(message);
        label.setOpaque(true);
        label.setBackground(new Color(0xFFFBE6));
        label.setForeground(new Color(0x333333));
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(0xD4B106)),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        label.setName("validation-message");
        Dimension size = label.getPreferredSize();
        int x = Math.max(0, Math.min(box.x, Math.max(0, panel.getWidth() - size.width)));
        label.setBounds(x, box.y + box.height + 4, size.width, size.height);
        bubble = label;
        panel.add(label);
        panel.setComponentZOrder(label, 0);
        panel.repaint(label.getBounds());
        bubbleTimer = new Timer(BUBBLE_MILLIS, new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                hideMessage();
            }
        });
        bubbleTimer.setRepeats(false);
        bubbleTimer.start();
    }

    /**
     * Hides the validation bubble.
     * <p>
     * 검증 말풍선을 숨긴다.
     */
    void hideMessage() {
        if (bubbleTimer != null) {
            bubbleTimer.stop();
            bubbleTimer = null;
        }
        if (bubble != null) {
            Rectangle bounds = bubble.getBounds();
            panel.remove(bubble);
            panel.repaint(bounds);
            bubble = null;
        }
    }

    /**
     * Returns the text of the validation bubble that is shown, or null (for tests).
     * <p>
     * 보이는 검증 말풍선의 글을 반환한다. 없으면 null(시험용).
     *
     * @return the message or null / 메시지 또는 null
     */
    public String getMessage() {
        return bubble == null ? null : bubble.getText();
    }

    /**
     * Asks the user for files with the chooser; the calling thread (the document thread) waits for the answer.
     * <p>
     * 선택기로 사용자에게 파일을 묻는다. 호출한 스레드(문서 스레드)는 답을 기다린다.
     *
     * @param multiple true if several files are accepted / 여러 파일을 받으면 true
     * @param accept   the <code>accept</code> attribute / <code>accept</code> 속성
     * @return the files or null if the user cancelled / 파일. 사용자가 취소했으면 null
     */
    File[] chooseFiles(final boolean multiple, final String accept) {
        final UploadChooser c = chooser;
        if (SwingUtilities.isEventDispatchThread()) {
            return c.choose(multiple, accept);
        }
        final File[][] result = new File[1][];
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    result[0] = c.choose(multiple, accept);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
        return result[0];
    }

    /**
     * The default color chooser: a Swing color chooser dialog over the window of the page component. It does nothing without a screen.
     * <p>
     * 기본 색 선택기: 페이지 컴포넌트의 창 위에 Swing 색 선택 대화상자를 띄운다. 화면이 없으면 아무것도 하지 않는다.
     */
    private final class SwingColors implements ColorChooser {

        /**
         * Creates the chooser.
         * <p>
         * 선택기를 만든다.
         */
        SwingColors() {
        }

        /**
         * Shows the color chooser.
         * <p>
         * 색 선택기를 보여 준다.
         *
         * @param rgb the current color / 현재 색
         * @return the chosen color or null / 고른 색 또는 null
         */
        public Integer choose(int rgb) {
            if (GraphicsEnvironment.isHeadless()) {
                return null;
            }
            Color chosen = JColorChooser.showDialog(SwingUtilities.getWindowAncestor(panel), "", new Color(rgb));
            return chosen == null ? null : Integer.valueOf(chosen.getRGB() & 0xFFFFFF);
        }
    }

    /**
     * The default chooser: a Swing file chooser
 over the window of the page component. It does nothing (the user cancelled) without a screen.
     * <p>
     * 기본 선택기: 페이지 컴포넌트의 창 위에 Swing 파일 선택기를 띄운다. 화면이 없으면 아무것도 하지 않는다(사용자가 취소한 것으로 본다).
     */
    private final class SwingChooser implements UploadChooser {

        /**
         * Creates the chooser.
         * <p>
         * 선택기를 만든다.
         */
        SwingChooser() {
        }

        /**
         * Shows the file chooser.
         * <p>
         * 파일 선택기를 보여 준다.
         *
         * @param multiple true if several files are accepted / 여러 파일을 받으면 true
         * @param accept   the accept attribute / accept 속성
         * @return the files or null / 파일 또는 null
         */
        public File[] choose(boolean multiple, String accept) {
            if (GraphicsEnvironment.isHeadless()) {
                return null;
            }
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(multiple);
            FileFilter filter = accept == null || accept.trim().length() == 0 ? null : new AcceptFilter(accept);
            if (filter != null) {
                fc.addChoosableFileFilter(filter);
                fc.setFileFilter(filter);
            }
            Component parent = SwingUtilities.getWindowAncestor(panel);
            if (fc.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
                return null;
            }
            File[] files = multiple ? fc.getSelectedFiles() : new File[] { fc.getSelectedFile() };
            return files == null || files.length == 0 || files[0] == null ? null : files;
        }
    }

    /**
     * The file filter of an <code>accept</code> attribute: extensions (<code>.png</code>), MIME types (<code>image/png</code>) and wildcards
     * (<code>image/*</code>).
     * <p>
     * <code>accept</code> 속성의 파일 필터: 확장자(<code>.png</code>), MIME 타입(<code>image/png</code>), 와일드카드(<code>image/*</code>).
     */
    static final class AcceptFilter extends FileFilter {

        /** The lower-case tokens of the attribute. 속성의 소문자 토큰. */
        private final String[] tokens;

        /**
         * Creates the filter.
         * <p>
         * 필터를 만든다.
         *
         * @param accept the attribute value / 속성 값
         */
        AcceptFilter(String accept) {
            this.tokens = accept.toLowerCase().split(",");
            // 토큰마다 앞뒤 공백을 지운다.
            for (int i = 0; i < tokens.length; i++) {
                tokens[i] = tokens[i].trim();
            }
        }

        /**
         * Tells whether a file is accepted (directories always are, so that the user can browse).
         * <p>
         * 파일이 받아들여지는지 알려 준다(탐색할 수 있도록 디렉터리는 항상 받아들인다).
         *
         * @param f the file / 파일
         * @return true if accepted / 받아들이면 true
         */
        public boolean accept(File f) {
            return f.isDirectory() || accepts(f.getName());
        }

        /**
         * Tells whether a file name is accepted.
         * <p>
         * 파일 이름이 받아들여지는지 알려 준다.
         *
         * @param name the file name / 파일 이름
         * @return true if accepted / 받아들이면 true
         */
        boolean accepts(String name) {
            String lower = name.toLowerCase();
            String type = java.net.URLConnection.guessContentTypeFromName(name);
            // 토큰을 하나씩 보며 확장자나 MIME 타입이 맞는지 확인한다.
            for (int i = 0; i < tokens.length; i++) {
                String t = tokens[i];
                if (t.length() == 0) {
                    continue;
                }
                if (t.startsWith(".") && lower.endsWith(t)) {
                    return true;
                }
                if (type != null && t.endsWith("/*") && type.startsWith(t.substring(0, t.length() - 1))) {
                    return true;
                }
                if (type != null && type.equals(t)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns the description shown in the chooser.
         * <p>
         * 선택기에 보이는 설명을 반환한다.
         *
         * @return the description / 설명
         */
        public String getDescription() {
            StringBuilder b = new StringBuilder();
            // 토큰을 쉼표로 이어 설명을 만든다.
            for (int i = 0; i < tokens.length; i++) {
                if (i > 0) {
                    b.append(", ");
                }
                b.append(tokens[i]);
            }
            return b.toString();
        }
    }
}
