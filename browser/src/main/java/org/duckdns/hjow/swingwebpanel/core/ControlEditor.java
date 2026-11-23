package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Color;
import java.awt.Insets;
import java.awt.event.FocusListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.AttributedCharacterIterator;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;

/**
 * The Swing editor that sits over a focused text control (a text field, a password field or a text area) while the control has the focus. A
 * Swing text component does the editing that a browser does natively: the caret, the selection with the mouse and the keyboard, copy and
 * paste, undo and the input methods of Korean and other languages (the composition text arrives in the document as it is composed). This class
 * mirrors the text and the selection to a {@link EditSpec.Listener} and takes text from the DOM back (see {@link #sync}); the listener decides
 * what the DOM sees. Merged document events become one text edit. All methods run on the EDT.
 * <p>
 * 텍스트 컨트롤(텍스트 필드, 암호 필드, 텍스트 영역)이 포커스를 가진 동안 그 위에 놓이는 Swing 편집기. 브라우저가 기본으로 해 주는 편집(캐럿, 마우스와 키보드로
 * 하는 선택, 복사와 붙여넣기, 실행 취소, 한글 등 입력기)을 Swing 텍스트 컴포넌트가 한다(조합 중인 글은 조합되는 대로 문서에 들어온다). 이 클래스는 글과 선택을
 * {@link EditSpec.Listener} 에 비추고 DOM 의 글을 다시 받아 오며({@link #sync} 참조), DOM 이 무엇을 보는지는 리스너가 정한다. 합쳐진 문서 이벤트는 글 편집
 * 하나가 된다. 모든 메서드는 EDT 에서 실행된다.
 */
final class ControlEditor {

    /** The bullet that a password field shows. 암호 필드가 보여 주는 검은 점. */
    private static final char BULLET = (char) 0x2022;

    /** The element of the control. 컨트롤의 요소. */
    private final EditSpec spec;

    /** The text component that edits. 편집하는 텍스트 컴포넌트. */
    private final JTextComponent text;

    /**
     * The receiver of the key events of the editor; if it is the keyboard of the page it is told when an input method composes, so that the key
     * events of the page say <code>isComposing</code>.
     * <p>
     * 편집기의 키 이벤트를 받는 쪽. 페이지의 키보드이면 입력기가 조합하는 동안을 알려 페이지의 키 이벤트가 <code>isComposing</code> 을 알리게 한다.
     */
    private final KeyListener keys;

    /** The component that is added to the page: the text component or the scroll pane around a text area. 페이지에 더해지는 컴포넌트. */
    private final JComponent root;

    /** True while the editor itself changes the text or the selection (the changes are not reported). 편집기가 글이나 선택을 직접 바꾸는 동안 true. */
    private boolean updating;

    /** True while an input method composes text. 입력기가 글을 조합하는 동안 true. */
    private boolean composing;

    /** True if a flush of the merged edits is queued. 합쳐진 편집을 알리는 일이 대기 중이면 true. */
    private boolean flushQueued;

    /** The input type of the pending edit. 대기 중인 편집의 입력 종류. */
    private String pendingType = "insertText";

    /** The inserted text of the pending edit. 대기 중인 편집에서 삽입한 글. */
    private String pendingData;

    /** The text that was reported last. 마지막으로 알린 글. */
    private String reportedText;

    /** The selection that was reported last. 마지막으로 알린 선택. */
    private int reportedStart = -1, reportedEnd = -1;

    /** The maximum length, applied to the edits of the user. 사용자의 편집에 적용하는 최대 길이. */
    private int maxLength;

    /** True once the editor was removed. 편집기를 없앴으면 true. */
    private boolean disposed;

    /** Where copied text is written, or null for the system clipboard of Swing. 복사한 글을 쓰는 곳. 없으면 Swing 의 시스템 클립보드. */
    private ClipboardSink sink;

    /** Where pasted data is read, or null for the system clipboard of Swing. 붙여넣을 데이터를 읽는 곳. 없으면 Swing 의 시스템 클립보드. */
    private ClipboardSource source;

    /**
     * Creates the editor of a control; it is not added to the page yet.
     * <p>
     * 컨트롤의 편집기를 만든다. 아직 페이지에 더하지 않는다.
     *
     * @param spec  what to edit / 편집할 대상
     * @param keys  receives the key events of the editor (they are the key events of the page) / 편집기의 키 이벤트(페이지의 키 이벤트이다)를 받는다
     * @param focus receives the focus events of the editor / 편집기의 포커스 이벤트를 받는다
     */
    ControlEditor(final EditSpec spec, KeyListener keys, FocusListener focus) {
        this.spec = spec;
        this.keys = keys;
        this.maxLength = spec.maxLength;
        if (spec.multiline) {
            JTextArea area = new JTextArea() {
                /** Serialization identifier. / 직렬화 식별자. */
                private static final long serialVersionUID = 1L;

                /** Cuts through the page: the DOM event first. / 페이지를 거쳐 잘라낸다: DOM 이벤트가 먼저이다. */
                public void cut() {
                    userClipboard("cut");
                }

                /** Copies through the page: the DOM event first. / 페이지를 거쳐 복사한다: DOM 이벤트가 먼저이다. */
                public void copy() {
                    userClipboard("copy");
                }

                /** Pastes through the page: the DOM event first. / 페이지를 거쳐 붙여넣는다: DOM 이벤트가 먼저이다. */
                public void paste() {
                    userClipboard("paste");
                }
            };
            area.setLineWrap(!spec.noWrap);
            area.setWrapStyleWord(true);
            area.setTabSize(8);
            text = area;
            JScrollPane pane = new JScrollPane(area, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    spec.noWrap ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            pane.setBorder(BorderFactory.createEmptyBorder());
            pane.getViewport().setOpaque(true);
            root = pane;
        } else if (spec.password) {
            JPasswordField field = new JPasswordField() {
                /** Serialization identifier. / 직렬화 식별자. */
                private static final long serialVersionUID = 1L;

                /** Pastes through the page: the DOM event first (a password field neither cuts nor copies). / 페이지를 거쳐 붙여넣는다: DOM 이벤트가 먼저이다(암호 필드는 잘라내기도 복사도 하지 않는다). */
                public void paste() {
                    userClipboard("paste");
                }
            };
            field.setEchoChar(BULLET);
            text = field;
            root = field;
        } else {
            text = new JTextField() {
                /** Serialization identifier. / 직렬화 식별자. */
                private static final long serialVersionUID = 1L;

                /** Cuts through the page: the DOM event first. / 페이지를 거쳐 잘라낸다: DOM 이벤트가 먼저이다. */
                public void cut() {
                    userClipboard("cut");
                }

                /** Copies through the page: the DOM event first. / 페이지를 거쳐 복사한다: DOM 이벤트가 먼저이다. */
                public void copy() {
                    userClipboard("copy");
                }

                /** Pastes through the page: the DOM event first. / 페이지를 거쳐 붙여넣는다: DOM 이벤트가 먼저이다. */
                public void paste() {
                    userClipboard("paste");
                }
            };
            root = text;
        }
        text.setBorder(BorderFactory.createEmptyBorder());
        text.setMargin(new Insets(0, 0, 0, 0));
        text.setOpaque(true);
        text.setFocusTraversalKeysEnabled(false);
        style(spec);
        updating = true;
        try {
            text.setText(spec.text);
            select(spec.selectionStart, spec.selectionEnd, spec.backward);
        } finally {
            updating = false;
        }
        reportedText = spec.text;
        reportedStart = text.getSelectionStart();
        reportedEnd = text.getSelectionEnd();
        text.addKeyListener(keys);
        text.addKeyListener(new TabKeeper());
        text.addFocusListener(focus);
        text.getDocument().addDocumentListener(new Changes());
        text.addCaretListener(new Carets());
        text.addInputMethodListener(new Composition());
        text.addMouseListener(new java.awt.event.MouseAdapter() {
            /**
             * Passes a click on to the page, so that the DOM gets the <code>click</code> event of a control that the editor covers.
             * <p>
             * 클릭을 페이지에 넘겨, 편집기가 덮은 컨트롤의 DOM 이 <code>click</code> 이벤트를 받게 한다.
             *
             * @param e the event / 이벤트
             */
            public void mouseClicked(java.awt.event.MouseEvent e) {
                java.awt.Container page = root.getParent();
                if (page != null && !disposed) {
                    page.dispatchEvent(SwingUtilities.convertMouseEvent(text, e, page));
                }
            }
        });
        if (text.getDocument() instanceof AbstractDocument) {
            ((AbstractDocument) text.getDocument()).setDocumentFilter(new Limit());
        }
        place(spec.control);
        applyFlags(spec.readOnly, spec.disabled);
    }

    /**
     * Gives the component the font, colors and alignment of the control.
     * <p>
     * 컴포넌트에 컨트롤의 글꼴, 색, 정렬을 준다.
     *
     * @param s what to edit / 편집할 대상
     */
    private void style(EditSpec s) {
        ControlSnapshot c = s.control;
        Color fg = new Color(c.getColor(), true);
        text.setFont(c.getFont());
        text.setForeground(fg);
        text.setCaretColor(fg);
        Color bg = opaque(c.getBackground());
        text.setBackground(bg);
        root.setBackground(bg);
        if (text instanceof JTextField) {
            String align = c.getTextAlign();
            int h = "center".equals(align) || "-webkit-center".equals(align) ? JTextField.CENTER
                    : ("right".equals(align) || "end".equals(align) ? JTextField.RIGHT : JTextField.LEFT);
            ((JTextField) text).setHorizontalAlignment(h);
        }
    }

    /**
     * Turns a color that may be translucent into an opaque one by laying it on white (the editor covers the control, so it must not show the
     * text drawn under it).
     * <p>
     * 반투명일 수 있는 색을 흰색 위에 올려 불투명하게 만든다(편집기가 컨트롤을 덮으므로 그 아래에 그려진 글이 비치면 안 된다).
     *
     * @param argb the color / 색
     * @return the opaque color / 불투명한 색
     */
    private static Color opaque(int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a == 0xFF) {
            return new Color(argb);
        }
        int r = (((argb >> 16) & 0xFF) * a + 255 * (255 - a)) / 255;
        int g = (((argb >> 8) & 0xFF) * a + 255 * (255 - a)) / 255;
        int b = ((argb & 0xFF) * a + 255 * (255 - a)) / 255;
        return new Color(r, g, b);
    }

    /**
     * Returns the component to add to the page.
     * <p>
     * 페이지에 더할 컴포넌트를 반환한다.
     *
     * @return the component / 컴포넌트
     */
    JComponent getComponent() {
        return root;
    }

    /**
     * Returns the text component.
     * <p>
     * 텍스트 컴포넌트를 반환한다.
     *
     * @return the text component / 텍스트 컴포넌트
     */
    JTextComponent getTextComponent() {
        return text;
    }

    /**
     * Returns the specification that the editor was made from.
     * <p>
     * 편집기를 만든 명세를 반환한다.
     *
     * @return the specification / 명세
     */
    EditSpec getSpec() {
        return spec;
    }

    /**
     * Puts the editor over the content box of the control and gives it the font and colors that the control has in the new page.
     * <p>
     * 편집기를 컨트롤의 content box 위에 놓고, 새 페이지에서 컨트롤이 가진 글꼴과 색을 준다.
     *
     * @param c the drawn control / 그려진 컨트롤
     */
    void place(ControlSnapshot c) {
        place(c, 1.0);
    }

    /**
     * Puts the editor over the content box of the control of a page that is zoomed: the bounds and the font are as many times larger as the
     * zoom factor says (the coordinates of the editor are those of the component, not of the document).
     * <p>
     * 확대된 페이지의 컨트롤의 content box 위에 편집기를 놓는다: 위치와 크기와 글꼴은 확대 배율만큼 커진다(편집기의 좌표는 문서가 아니라 컴포넌트의 좌표이다).
     *
     * @param c    the drawn control / 그려진 컨트롤
     * @param zoom the zoom factor of the page / 페이지의 확대 배율
     */
    void place(ControlSnapshot c, double zoom) {
        int x = (int) Math.round(c.getX() * zoom);
        int y = (int) Math.round(c.getY() * zoom);
        int w = Math.max(1, (int) Math.round(c.getWidth() * zoom));
        int h = Math.max(1, (int) Math.round(c.getHeight() * zoom));
        if (!root.getBounds().equals(new java.awt.Rectangle(x, y, w, h))) {
            root.setBounds(x, y, w, h);
            root.validate();
        }
        java.awt.Font font = zoom == 1.0 ? c.getFont() : c.getFont().deriveFont((float) (c.getFont().getSize2D() * zoom));
        if (!font.equals(text.getFont())) {
            text.setFont(font);
        }
        Color fg = new Color(c.getColor(), true);
        if (!fg.equals(text.getForeground())) {
            text.setForeground(fg);
            text.setCaretColor(fg);
        }
        Color bg = opaque(c.getBackground());
        if (!bg.equals(text.getBackground())) {
            text.setBackground(bg);
            root.setBackground(bg);
        }
    }

    /**
     * Applies the editable and enabled flags.
     * <p>
     * 편집 가능과 활성 플래그를 적용한다.
     *
     * @param readOnly true if the text cannot be edited / 글을 편집할 수 없으면 true
     * @param disabled true if the control is disabled / 컨트롤이 비활성이면 true
     */
    void applyFlags(boolean readOnly, boolean disabled) {
        text.setEditable(!readOnly && !disabled);
        text.setEnabled(!disabled);
    }

    /**
     * Takes the state of the DOM: a new text (if it differs from the text that was last reported to the DOM), the selection and the limits.
     * The changes are not reported back.
     * <p>
     * DOM 의 상태를 받는다: 새 글(DOM 에 마지막으로 알린 글과 다를 때), 선택, 제한. 바뀐 내용은 다시 알리지 않는다.
     *
     * @param newText   the text of the DOM, or null to keep the text of the editor / DOM 의 글. 편집기의 글을 유지하려면 null
     * @param start     the selection start of the DOM, or -1 / DOM 의 선택 시작. 없으면 -1
     * @param end       the selection end of the DOM / DOM 의 선택 끝
     * @param backward  true for a backward selection / 뒤쪽 방향 선택이면 true
     * @param readOnly  the read-only flag / 읽기 전용 플래그
     * @param disabled  the disabled flag / 비활성 플래그
     * @param maxLength the maximum length or -1 / 최대 길이 또는 -1
     */
    void sync(String newText, int start, int end, boolean backward, boolean readOnly, boolean disabled, int maxLength) {
        this.maxLength = maxLength;
        applyFlags(readOnly, disabled);
        updating = true;
        try {
            if (newText != null && !newText.equals(text.getText())) {
                text.setText(newText);
                reportedText = newText;
                if (start < 0) {
                    text.setCaretPosition(newText.length());
                }
            }
            if (start >= 0) {
                select(start, end, backward);
            }
            reportedStart = text.getSelectionStart();
            reportedEnd = text.getSelectionEnd();
        } finally {
            updating = false;
        }
    }

    /**
     * Selects a range.
     * <p>
     * 범위를 선택한다.
     *
     * @param start    the start / 시작
     * @param end      the end / 끝
     * @param backward true if the caret is at the start / 캐럿이 시작에 있으면 true
     */
    private void select(int start, int end, boolean backward) {
        int length = text.getDocument().getLength();
        int s = Math.max(0, Math.min(start, length));
        int e = Math.max(s, Math.min(end, length));
        if (backward) {
            text.setCaretPosition(e);
            text.moveCaretPosition(s);
        } else {
            text.setCaretPosition(s);
            text.moveCaretPosition(e);
        }
    }

    /**
     * Puts the caret at the model position that is under a point of the editor (where the user pressed the mouse button before the editor
     * existed).
     * <p>
     * 캐럿을 편집기의 한 점 아래에 있는 모델 위치에 놓는다(편집기가 생기기 전에 사용자가 마우스 버튼을 누른 곳).
     *
     * @param x x in the coordinates of the page / 페이지 좌표의 x
     * @param y y in the coordinates of the page / 페이지 좌표의 y
     */
    void placeCaretAt(int x, int y) {
        java.awt.Point p = SwingUtilities.convertPoint(root.getParent(), x, y, text);
        int pos = text.viewToModel(p);
        if (pos >= 0) {
            updating = false;
            text.setCaretPosition(pos);
        }
    }

    /**
     * Returns the text of the editor.
     * <p>
     * 편집기의 글을 반환한다.
     *
     * @return the text / 글
     */
    String getText() {
        if (text instanceof JPasswordField) {
            return new String(((JPasswordField) text).getPassword());
        }
        return text.getText();
    }

    /**
     * Stops the reports and detaches the listeners.
     * <p>
     * 알림을 멈추고 리스너를 뗀다.
     */
    void dispose() {
        disposed = true;
        updating = true;
        // 조합 중에 편집기가 사라지면 페이지 키보드의 조합 상태도 끝낸다.
        if (composing) {
            composing = false;
            tellKeyboard(false);
        }
    }

    /**
     * Tells the keyboard of the page (if it receives the key events of this editor) whether an input method composes text now.
     * <p>
     * 이 편집기의 키 이벤트를 받는 쪽이 페이지의 키보드이면 지금 입력기가 글을 조합하는지 알린다.
     *
     * @param value true while composing / 조합 중이면 true
     */
    private void tellKeyboard(boolean value) {
        if (keys instanceof PageKeyboard) {
            ((PageKeyboard) keys).setComposing(value);
        }
    }

    /**
     * Sets where the editor writes what the user copies or cuts and where it reads what the user pastes: the clipboard of the component
     * (which a host may replace). Without it the editor uses the system clipboard of Swing.
     * <p>
     * 사용자가 복사하거나 잘라낸 것을 쓰는 곳과 붙여넣을 것을 읽는 곳을 정한다: 컴포넌트의 클립보드(호스트가 바꿀 수 있다). 없으면 편집기가 Swing 의 시스템
     * 클립보드를 쓴다.
     *
     * @param to   where copied text is written, or null / 복사한 텍스트를 쓰는 곳. 없으면 null
     * @param from where pasted data is read, or null / 붙여넣을 데이터를 읽는 곳. 없으면 null
     */
    void setClipboard(ClipboardSink to, ClipboardSource from) {
        this.sink = to;
        this.source = from;
    }

    /**
     * Cuts, copies or pastes for the user (the Swing actions of the editor call it on the EDT): the page gets the <code>cut</code>,
     * <code>copy</code> or <code>paste</code> event first, and what the user asked for happens unless a listener cancelled the event (a
     * script that cancels a cut or a copy and sets its own data puts that on the clipboard instead).
     * <p>
     * 사용자를 위해 잘라내기, 복사, 붙여넣기를 한다(편집기의 Swing 동작이 EDT 에서 부른다): 페이지가 <code>cut</code>, <code>copy</code>, <code>paste</code> 이벤트를
     * 먼저 받고, 리스너가 이벤트를 취소하지 않았으면 사용자가 요청한 일이 일어난다(잘라내기나 복사를 취소하고 자기 데이터를 정한 스크립트는 그것을 클립보드에 넣는다).
     *
     * @param type <code>cut</code>, <code>copy</code> or <code>paste</code> / 종류
     */
    private void userClipboard(final String type) {
        final boolean paste = "paste".equals(type);
        // 편집할 수 없는 컨트롤에는 붙여넣기도 잘라내기의 삭제도 없다.
        if (disposed || (paste && (!text.isEditable() || !text.isEnabled()))) {
            return;
        }
        final String plain;
        String html = null;
        if (paste) {
            String[] both = readClipboard();
            plain = both[0];
            html = both[1];
        } else {
            plain = text.getSelectedText();
        }
        // 붙여넣을 글이 없거나 선택이 없으면 할 일이 없다.
        if (plain == null || plain.length() == 0) {
            return;
        }
        spec.listener.clipboard(type, plain, html, new EditSpec.ClipboardCallback() {
            /** Goes back to the EDT to do what the user asked for. / EDT 로 돌아와 사용자가 요청한 일을 한다. */
            public void done(final boolean cancelled, final org.duckdns.hjow.swingwebpanel.dom.ClipboardData data) {
                SwingUtilities.invokeLater(new Runnable() {
                    /** Does the action. / 동작을 한다. */
                    public void run() {
                        finishClipboard(type, plain, cancelled, data);
                    }
                });
            }
        });
    }

    /**
     * Does the default action of a cut, a copy or a paste after its event (EDT).
     * <p>
     * 잘라내기, 복사, 붙여넣기의 이벤트 뒤에 기본 동작을 한다(EDT).
     *
     * @param type      the event type / 이벤트 종류
     * @param plain     the selected text or the text of the clipboard / 선택한 글 또는 클립보드의 글
     * @param cancelled true if a listener cancelled the event / 리스너가 이벤트를 취소했으면 true
     * @param data      the data of the event / 이벤트의 데이터
     */
    private void finishClipboard(String type, String plain, boolean cancelled, org.duckdns.hjow.swingwebpanel.dom.ClipboardData data) {
        if (disposed) {
            return;
        }
        if ("paste".equals(type)) {
            // 붙여넣기는 취소되지 않았을 때만 선택을 클립보드의 글로 바꾼다.
            if (!cancelled) {
                text.replaceSelection(plain);
            }
            return;
        }
        if (cancelled) {
            // 취소한 스크립트가 자기 글을 정했으면 그것이 클립보드에 간다. 아니면 클립보드를 건드리지 않는다.
            if (data.isChanged() && data.hasData("text/plain")) {
                writeClipboard(data.getData("text/plain"));
            }
            return;
        }
        writeClipboard(plain);
        // 잘라내기는 쓴 뒤에 선택을 지운다(읽기 전용이나 비활성이면 복사만 한다).
        if ("cut".equals(type) && text.isEditable() && text.isEnabled()) {
            text.replaceSelection("");
        }
    }

    /**
     * Writes text to the clipboard (the clipboard of the component, or the system clipboard of Swing).
     * <p>
     * 텍스트를 클립보드에 쓴다(컴포넌트의 클립보드 또는 Swing 의 시스템 클립보드).
     *
     * @param s the text / 글
     */
    private void writeClipboard(String s) {
        try {
            if (sink != null) {
                sink.write(s, null);
            } else {
                text.getToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(s), null);
            }
        } catch (Exception e) {
            // 클립보드를 쓸 수 없으면(화면 없음, 접근 거부) 복사는 조용히 포기한다.
            return;
        }
    }

    /**
     * Reads the text and the HTML of the clipboard (the clipboard of the component, or the system clipboard of Swing). The HTML is the
     * fragment without the document frame that the platform adds; it is only shown to the <code>paste</code> event, the control takes the text.
     * <p>
     * 클립보드의 글과 HTML 을 읽는다(컴포넌트의 클립보드 또는 Swing 의 시스템 클립보드). HTML 은 플랫폼이 더한 문서 틀을 뺀 조각이며
     * <code>paste</code> 이벤트에만 보이고 컨트롤에는 글을 넣는다.
     *
     * @return the text (index 0) and the HTML (index 1), each null if there is none / 글(0)과 HTML(1). 없으면 각각 null
     */
    private String[] readClipboard() {
        String[] both = new String[2];
        try {
            java.awt.datatransfer.Transferable t = source != null ? source.read()
                    : text.getToolkit().getSystemClipboard().getContents(null);
            if (t != null && t.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                both[0] = (String) t.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
            }
            java.awt.datatransfer.DataFlavor html = new java.awt.datatransfer.DataFlavor("text/html;class=java.lang.String");
            // HTML 이 있으면 플랫폼이 더한 문서 틀을 뺀 조각만 이벤트에 보인다.
            if (t != null && t.isDataFlavorSupported(html)) {
                both[1] = PageEditing.fragmentOf((String) t.getTransferData(html));
            }
        } catch (Exception e) {
            // 클립보드를 읽을 수 없으면 읽은 것까지만 쓴다(글이 없으면 붙여넣을 것이 없다).
            return both;
        }
        return both;
    }

    /**
     * Queues the report of the merged text edits.
     * <p>
     * 합쳐진 글 편집의 알림을 대기시킨다.
     *
     * @param type the input type / 입력 종류
     * @param data the inserted text or null / 삽입한 글 또는 null
     */
    private void edited(String type, String data) {
        if (updating || disposed) {
            return;
        }
        pendingType = type;
        pendingData = data;
        if (!flushQueued) {
            flushQueued = true;
            // 캐럿이 갱신되고 합쳐질 편집이 모두 끝난 뒤에 한 번만 알린다.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    flush();
                }
            });
        }
    }

    /**
     * Reports the merged edit and the selection.
     * <p>
     * 합쳐진 편집과 선택을 알린다.
     */
    private void flush() {
        flushQueued = false;
        if (disposed || !spec.listener.isCurrent()) {
            return;
        }
        String now = getText();
        if (!now.equals(reportedText)) {
            reportedText = now;
            spec.listener.textEdited(now, pendingType, pendingData);
        }
        reportSelection();
    }

    /**
     * Reports the selection if it changed.
     * <p>
     * 선택이 바뀌었으면 알린다.
     */
    private void reportSelection() {
        if (disposed || updating || !spec.listener.isCurrent()) {
            return;
        }
        int s = text.getSelectionStart();
        int e = text.getSelectionEnd();
        if (s != reportedStart || e != reportedEnd) {
            reportedStart = s;
            reportedEnd = e;
            spec.listener.selectionEdited(s, e, text.getCaretPosition() < text.getCaret().getMark());
        }
    }

    /**
     * Sees the changes of the document.
     * <p>
     * 문서의 변경을 본다.
     */
    private final class Changes implements DocumentListener {

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         */
        Changes() {
        }

        /**
         * Text was inserted.
         * <p>
         * 글이 삽입되었다.
         *
         * @param e the event / 이벤트
         */
        public void insertUpdate(DocumentEvent e) {
            String inserted = null;
            try {
                inserted = e.getDocument().getText(e.getOffset(), e.getLength());
            } catch (BadLocationException ex) {
                // 범위가 이미 사라졌으면 삽입한 글을 알 수 없는 것으로 둔다.
                inserted = null;
            }
            edited(composing ? "insertCompositionText" : "insertText", inserted);
        }

        /**
         * Text was removed.
         * <p>
         * 글이 지워졌다.
         *
         * @param e the event / 이벤트
         */
        public void removeUpdate(DocumentEvent e) {
            edited(composing ? "insertCompositionText" : "deleteContentBackward", null);
        }

        /**
         * Attributes changed (nothing to report).
         * <p>
         * 속성이 바뀌었다(알릴 것이 없다).
         *
         * @param e the event / 이벤트
         */
        public void changedUpdate(DocumentEvent e) {
            // 글 내용이 아닌 속성 변경이므로 알리지 않는다.
        }
    }

    /**
     * Sees the movements of the caret.
     * <p>
     * 캐럿의 움직임을 본다.
     */
    private final class Carets implements CaretListener {

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         */
        Carets() {
        }

        /**
         * The caret or the selection changed.
         * <p>
         * 캐럿이나 선택이 바뀌었다.
         *
         * @param e the event / 이벤트
         */
        public void caretUpdate(CaretEvent e) {
            // 글 편집이 곧 알려지므로 그 뒤에 선택을 알린다. 글 편집이 없으면 바로 알린다.
            if (!flushQueued) {
                reportSelection();
            }
        }
    }

    /**
     * Keeps the Tab key from being typed into the text (it moves the focus of the page).
     * <p>
     * Tab 키가 글로 입력되지 않게 한다(페이지의 포커스를 옮긴다).
     */
    private static final class TabKeeper implements KeyListener {

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         */
        TabKeeper() {
        }

        /**
         * Consumes Tab.
         * <p>
         * Tab 을 소비한다.
         *
         * @param e the event / 이벤트
         */
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_TAB) {
                e.consume();
            }
        }

        /**
         * Consumes the character of Tab.
         * <p>
         * Tab 의 문자를 소비한다.
         *
         * @param e the event / 이벤트
         */
        public void keyTyped(KeyEvent e) {
            if (e.getKeyChar() == '\t') {
                e.consume();
            }
        }

        /**
         * Nothing to do.
         * <p>
         * 할 일이 없다.
         *
         * @param e the event / 이벤트
         */
        public void keyReleased(KeyEvent e) {
            // 키를 뗄 때는 처리할 것이 없다.
        }
    }

    /**
     * Turns the events of the input method into the composition events of the DOM.
     * <p>
     * 입력기의 이벤트를 DOM 의 조합 이벤트로 바꾼다.
     */
    private final class Composition implements InputMethodListener {

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         */
        Composition() {
        }

        /**
         * True if the input method emptied the composed text and the <code>compositionend</code> event waits one turn of the event queue for a
         * commit that may follow: the Korean input method of Windows commits a syllable with the Space key in two events, first an empty
         * composition and then the committed text, and the end of the composition must carry that text.
         * <p>
         * 입력기가 조합 중인 글을 비웠고 <code>compositionend</code> 이벤트가 뒤따를 수 있는 확정을 이벤트 큐 한 바퀴 동안 기다리면 true. Windows 의 한국어 입력기는
         * Space 키로 음절을 확정할 때 빈 조합과 확정된 글을 두 이벤트로 보내며, 조합의 끝은 그 글을 가져야 한다.
         */
        private boolean endPending;

        /**
         * The composed text changed.
         * <p>
         * 조합 중인 글이 바뀌었다.
         *
         * @param event the event / 이벤트
         */
        public void inputMethodTextChanged(InputMethodEvent event) {
            AttributedCharacterIterator it = event.getText();
            int total = it == null ? 0 : it.getEndIndex() - it.getBeginIndex();
            int committed = Math.min(event.getCommittedCharacterCount(), total);
            StringBuilder done = new StringBuilder();
            StringBuilder composed = new StringBuilder();
            // 앞부분은 이번에 확정된 글이고, 확정되지 않은 뒷부분이 지금 조합 중인 글이다.
            for (int i = 0; i < total && it != null; i++) {
                it.setIndex(it.getBeginIndex() + i);
                (i < committed ? done : composed).append(it.current());
            }
            boolean wasComposing = composing;
            // 끝을 기다리는 조합도 뒤따르는 확정으로 끝날 수 있으므로 진행 중인 조합으로 본다.
            boolean was = composing || endPending;
            boolean pending = endPending;
            endPending = false;
            composing = composed.length() > 0;
            // 조합 중에 도착하는 키 이벤트가 isComposing 을 알리도록 페이지 키보드에 알린다.
            if (composing != wasComposing) {
                tellKeyboard(composing);
            }
            if (disposed || !spec.listener.isCurrent()) {
                return;
            }
            // 조합이 확정 없이 비워졌으면 끝을 한 바퀴 미뤄 바로 뒤의 확정 이벤트가 data 를 줄 수 있게 한다.
            if (was && !pending && done.length() == 0 && !composing) {
                endPending = true;
                SwingUtilities.invokeLater(new Runnable() {
                    /** Ends the emptied composition if no commit came. / 확정이 오지 않았으면 비워진 조합을 끝낸다. */
                    public void run() {
                        if (endPending) {
                            endPending = false;
                            if (!disposed && spec.listener.isCurrent()) {
                                spec.listener.composition("compositionend", "");
                            }
                        }
                    }
                });
                return;
            }
            // 진행 중이던 조합이 확정되거나 취소되면 compositionend 의 data 는 확정된 글이다(취소면 빈 글, UI Events).
            boolean ended = was && (done.length() > 0 || !composing);
            if (ended) {
                spec.listener.composition("compositionend", done.toString());
            }
            // 확정 뒤에 이어지는 조합(한글 음절 넘김)은 새 조합으로 시작한다.
            if (composing && (!was || ended)) {
                spec.listener.composition("compositionstart", "");
            }
            if (composing) {
                spec.listener.composition("compositionupdate", composed.toString());
            }
        }

        /**
         * The caret of the input method moved (nothing to report).
         * <p>
         * 입력기의 캐럿이 움직였다(알릴 것이 없다).
         *
         * @param event the event / 이벤트
         */
        public void caretPositionChanged(InputMethodEvent event) {
            // 조합 중인 글 안의 캐럿 이동은 DOM 에 알리지 않는다.
        }
    }

    /**
     * Limits the length of the text that the user types or pastes.
     * <p>
     * 사용자가 입력하거나 붙여 넣는 글의 길이를 제한한다.
     */
    private final class Limit extends DocumentFilter {

        /**
         * Creates the filter.
         * <p>
         * 필터를 만든다.
         */
        Limit() {
        }

        /**
         * Inserts text within the limit.
         * <p>
         * 제한 안에서 글을 삽입한다.
         *
         * @param fb     the bypass / 우회 객체
         * @param offset the position / 위치
         * @param string the text / 글
         * @param attr   the attributes / 속성
         * @throws BadLocationException for a wrong position / 위치가 잘못되면
         */
        public void insertString(FilterBypass fb, int offset, String string, javax.swing.text.AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, string, attr);
        }

        /**
         * Replaces text within the limit.
         * <p>
         * 제한 안에서 글을 바꾼다.
         *
         * @param fb     the bypass / 우회 객체
         * @param offset the position / 위치
         * @param length the length to replace / 바꿀 길이
         * @param string the new text / 새 글
         * @param attrs  the attributes / 속성
         * @throws BadLocationException for a wrong position / 위치가 잘못되면
         */
        public void replace(FilterBypass fb, int offset, int length, String string, javax.swing.text.AttributeSet attrs)
                throws BadLocationException {
            String s = string;
            // 프로그램이 직접 바꾸는 동안과 최대 길이가 없을 때는 제한하지 않는다.
            if (s != null && !updating && maxLength >= 0) {
                int room = maxLength - (fb.getDocument().getLength() - length);
                if (room < s.length()) {
                    int cut = Math.max(0, room);
                    // 서로게이트 쌍 가운데서 자르지 않는다.
                    if (cut > 0 && cut < s.length() && Character.isHighSurrogate(s.charAt(cut - 1))) {
                        cut--;
                    }
                    s = s.substring(0, cut);
                }
            }
            super.replace(fb, offset, length, s, attrs);
        }
    }
}
