package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.CompositionEvent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.EventTarget;
import org.duckdns.hjow.swingwebpanel.dom.MouseEvent;
import org.duckdns.hjow.swingwebpanel.dom.html.ControlDisplay;
import org.duckdns.hjow.swingwebpanel.dom.html.FileEntry;
import org.duckdns.hjow.swingwebpanel.dom.html.FormHost;
import org.duckdns.hjow.swingwebpanel.dom.html.FormSubmission;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlControl;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlFormElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlInputElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlOptionElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlSelectElement;
import org.duckdns.hjow.swingwebpanel.dom.html.HtmlTextAreaElement;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.ControlSnapshot;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * The host of the form controls of one page: the bridge between the DOM of the controls and the Swing component. It
 * <ul>
 * <li>hands the submissions of forms to the {@link FormSubmitSink} (it only passes them on; the navigation is the sink's work),</li>
 * <li>shows the validation message of an invalid control in a bubble next to it,</li>
 * <li>asks the user for the files of a file upload control (a file chooser) and reads the chosen files,</li>
 * <li>tells the page that the state of a control changed, so that its style and drawing are made again, and</li>
 * <li>opens the Swing editor over the text field or text area that gets the focus (see {@link ControlOverlays}), mirrors the text and the
 * selection between the editor and the DOM, fires the <code>input</code>, <code>change</code> and composition events, and closes the editor
 * when the control loses the focus.</li>
 * </ul>
 * The methods that the DOM calls run on the document thread of the page; the Swing parts are posted to the event dispatch thread.
 * <p>
 * 페이지 하나의 폼 컨트롤 호스트: 컨트롤의 DOM 과 Swing 컴포넌트를 잇는 다리이다.
 * <ul>
 * <li>폼 제출을 {@link FormSubmitSink} 에 넘긴다(넘기기만 하며 이동은 싱크의 일이다),</li>
 * <li>올바르지 않은 컨트롤의 검증 메시지를 그 옆의 말풍선에 보여 준다,</li>
 * <li>파일 업로드 컨트롤의 파일을 사용자에게 묻고(파일 선택기) 고른 파일을 읽는다,</li>
 * <li>컨트롤의 상태가 바뀌었음을 페이지에 알려 스타일과 그림이 다시 만들어지게 한다,</li>
 * <li>포커스를 받은 텍스트 필드나 텍스트 영역 위에 Swing 편집기를 열고({@link ControlOverlays} 참조), 편집기와 DOM 사이에 글과 선택을 비추고,
 * <code>input</code>, <code>change</code>, 조합 이벤트를 발생시키며, 컨트롤이 포커스를 잃으면 편집기를 닫는다.</li>
 * </ul>
 * DOM 이 부르는 메서드는 페이지의 문서 스레드에서 실행되고, Swing 쪽 일은 이벤트 디스패치 스레드에 넘긴다.
 */
final class PageFormHost implements FormHost, EventListener {

    /** The largest file that is read for an upload, in bytes. 업로드를 위해 읽는 파일의 최대 크기(바이트). */
    static final long MAX_FILE_BYTES = 64L * 1024 * 1024;

    /** The document of the page. 페이지의 문서. */
    private final Document document;

    /** The presentation of the page (styles and drawing). 페이지의 표현(스타일과 그림). */
    private final PagePresentation presentation;

    /** The view that shows the page. 페이지를 보여 주는 뷰. */
    private final PageView view;

    /** The session that owns the document thread. 문서 스레드를 소유한 세션. */
    private final BrowserSession session;

    /** The receiver of the form submissions, or null. 폼 제출을 받는 쪽. 없으면 null. */
    private final FormSubmitSink sink;

    /** The Swing overlays, or null if the page has no component (no editors and no dialogs). Swing 오버레이. 컴포넌트가 없으면 null. */
    private final ControlOverlays overlays;

    /** The text control that has the focus and an editor (or is waiting for one). 포커스를 가지고 편집기를 가졌거나 기다리는 텍스트 컨트롤. */
    private volatile Element editing;

    /** The value of the focused control when it got the focus (the <code>change</code> event compares with it). 컨트롤이 포커스를 받았을 때의 값. */
    private String valueAtFocus = "";

    /** True once the user changed the value since the focus. 포커스를 받은 뒤 사용자가 값을 바꿨으면 true. */
    private boolean editedSinceFocus;

    /** The value that the editor and the DOM have in common. 편집기와 DOM 이 공통으로 가진 값. */
    private String lastValue = "";

    /** The selection of the DOM that the editor knows. 편집기가 아는 DOM 의 선택. */
    private int knownStart = -1, knownEnd = -1;

    /** Above 0 while the host changes the DOM for the editor (the change is not sent back to the editor). 호스트가 편집기를 위해 DOM 을 바꾸는 동안 0 보다 크다. */
    private int userDepth;

    /** True once the page is gone. 페이지가 없어졌으면 true. */
    private volatile boolean closed;

    /** The focused text control that is waiting for its box. 상자를 기다리는, 포커스를 가진 텍스트 컨트롤. */
    private volatile Element pending;

    /**
     * Creates the host and listens to the focus events of the document.
     * <p>
     * 호스트를 만들고 문서의 포커스 이벤트를 듣는다.
     *
     * @param document     the document / 문서
     * @param presentation the presentation of the page / 페이지의 표현
     * @param view         the view that shows the page / 페이지를 보여 주는 뷰
     * @param session      the session that owns the document thread / 문서 스레드를 소유한 세션
     * @param sink         the receiver of form submissions, or null / 폼 제출을 받는 쪽. 없으면 null
     * @param overlays     the Swing overlays, or null / Swing 오버레이. 없으면 null
     */
    PageFormHost(Document document, PagePresentation presentation, PageView view, BrowserSession session, FormSubmitSink sink,
            ControlOverlays overlays) {
        this.document = document;
        this.presentation = presentation;
        this.view = view;
        this.session = session;
        this.sink = sink;
        this.overlays = overlays;
        if (overlays != null) {
            document.addEventListener("focusin", this, true);
            document.addEventListener("focusout", this, true);
            document.addEventListener("click", this, false);
            document.addEventListener("mousedown", this, true);
            document.addEventListener("mousemove", this, true);
            document.addEventListener("mouseup", this, true);
            // 새 렌더링이 게시될 때, 상자가 없어서 기다리던 컨트롤의 편집기를 다시 시도한다(관찰자는 이 호스트를 약하게만 가리킨다).
            overlays.addLayoutObserver(new RenderObserver(this, overlays));
        }
    }

    /**
     * Stops the host: the page is gone.
     * <p>
     * 호스트를 멈춘다: 페이지가 없어졌다.
     */
    void close() {
        closed = true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // FormHost
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Hands a submission to the sink.
     * <p>
     * 제출을 싱크에 넘긴다.
     *
     * @param submission what to send / 보낼 내용
     * @param form       the form / 폼
     */
    public void submit(FormSubmission submission, Element form) {
        FormSubmitSink s = sink;
        if (s != null && !closed) {
            s.submit(submission);
        }
    }

    /**
     * Shows the validation message in a bubble under the control (or in the status text if the control has no box).
     * <p>
     * 검증 메시지를 컨트롤 아래 말풍선에 보여 준다(컨트롤에 상자가 없으면 상태 텍스트에 보인다).
     *
     * @param control the invalid control / 올바르지 않은 컨트롤
     * @param message the message / 메시지
     */
    public void reportInvalid(Element control, final String message) {
        if (closed) {
            return;
        }
        RenderedPage page = view.getRenderedPage();
        List<LayoutBox> boxes = page == null ? null : page.getLayout().getBoxes(control);
        if (overlays == null || boxes == null || boxes.isEmpty()) {
            session.setStatusMessage(message);
            return;
        }
        LayoutBox box = boxes.get(0);
        final Rectangle where = new Rectangle((int) Math.round(box.getX()), (int) Math.round(box.getY()), (int) Math.round(box.getWidth()),
                (int) Math.round(box.getHeight()));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!closed && view.isCurrent(presentation)) {
                    overlays.showMessage(where, message);
                }
            }
        });
    }

    /**
     * Asks the user for files and reads them: only the files that the user chose, up to {@link #MAX_FILE_BYTES} each.
     * <p>
     * 사용자에게 파일을 묻고 읽는다. 사용자가 고른 파일만, 파일마다 {@link #MAX_FILE_BYTES} 까지 읽는다.
     *
     * @param input    the file control / 파일 컨트롤
     * @param multiple true if several files are accepted / 여러 파일을 받으면 true
     * @param accept   the accept attribute / accept 속성
     * @return the files, or null if the user cancelled (or nothing could be read) / 파일. 사용자가 취소했으면(또는 읽을 수 없으면) null
     */
    public FileEntry[] chooseFiles(Element input, boolean multiple, String accept) {
        if (overlays == null || closed) {
            return null;
        }
        File[] files = overlays.chooseFiles(multiple, accept);
        if (files == null) {
            return null;
        }
        List<FileEntry> out = new ArrayList<FileEntry>();
        // 고른 파일을 하나씩 읽는다. 크기 한도를 넘거나 읽을 수 없는 파일은 건너뛴다.
        for (int i = 0; i < files.length; i++) {
            FileEntry entry = read(files[i]);
            if (entry != null) {
                out.add(entry);
            }
        }
        return out.isEmpty() ? null : out.toArray(new FileEntry[out.size()]);
    }

    /**
     * Reads a chosen file.
     * <p>
     * 고른 파일을 읽는다.
     *
     * @param f the file / 파일
     * @return the entry, or null if the file is not a readable regular file within the size limit / 항목. 읽을 수 있는 일반 파일이 아니거나 크기 한도를 넘으면 null
     */
    private static FileEntry read(File f) {
        if (f == null || !f.isFile() || f.length() > MAX_FILE_BYTES) {
            return null;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            byte[] data = new byte[(int) f.length()];
            int at = 0;
            // 파일의 끝까지 읽는다.
            while (at < data.length) {
                int n = in.read(data, at, data.length - at);
                if (n < 0) {
                    break;
                }
                at += n;
            }
            byte[] exact = at == data.length ? data : java.util.Arrays.copyOf(data, at);
            String type = URLConnection.guessContentTypeFromName(f.getName());
            return new FileEntry(f.getName(), type == null ? "" : type, f.lastModified(), exact);
        } catch (IOException e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // 닫기 실패는 읽은 결과에 영향이 없다.
                    in = null;
                }
            }
        }
    }

    /**
     * Makes the style and the drawing of the page up to date with the state of a control, and sends changes that script made to the editor.
     * <p>
     * 컨트롤의 상태에 맞게 페이지의 스타일과 그림을 다시 만들게 하고, 스크립트가 바꾼 내용은 편집기에 보낸다.
     *
     * @param element the control / 컨트롤
     */
    public void stateChanged(Element element) {
        if (closed) {
            return;
        }
        presentation.elementStateChanged();
        if (element == editing && userDepth == 0) {
            syncEditor(element);
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 포커스와 편집기
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Sees the <code>focusin</code> and <code>focusout</code> events of the document (capturing).
     * <p>
     * 문서의 <code>focusin</code>, <code>focusout</code> 이벤트를 본다(캡처).
     *
     * @param event the event / 이벤트
     */
    public void handleEvent(Event event) {
        EventTarget t = event.getTarget();
        if (closed || !(t instanceof Element)) {
            return;
        }
        String type = event.getType();
        if ("focusin".equals(type)) {
            focusIn((Element) t);
        } else if ("focusout".equals(type)) {
            focusOut((Element) t);
        } else if (event instanceof MouseEvent) {
            mouse((Element) t, (MouseEvent) event);
        }
    }

    /**
     * Tells whether an element is a text control that the host edits with an editor.
     * <p>
     * 요소가 호스트가 편집기로 편집하는 텍스트 컨트롤인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a text field or a text area / 텍스트 필드나 텍스트 영역이면 true
     */
    static boolean isTextControl(Element e) {
        String kind = ControlDisplay.kind(e);
        return ControlDisplay.isTextField(kind) || ControlDisplay.TEXTAREA.equals(kind);
    }

    /**
     * Returns the value of a text control.
     * <p>
     * 텍스트 컨트롤의 값을 반환한다.
     *
     * @param e the control / 컨트롤
     * @return the value / 값
     */
    private static String value(Element e) {
        return e instanceof HtmlTextAreaElement ? ((HtmlTextAreaElement) e).getValue() : ((HtmlInputElement) e).getValue();
    }

    /**
     * A control got the focus: a text control gets an editor.
     * <p>
     * 컨트롤이 포커스를 받았다: 텍스트 컨트롤은 편집기를 얻는다.
     *
     * @param t the control / 컨트롤
     */
    private void focusIn(Element t) {
        if (editing != null && editing != t) {
            focusOut(editing);
        }
        if (!isTextControl(t) || editing == t) {
            return;
        }
        editing = t;
        valueAtFocus = value(t);
        lastValue = valueAtFocus;
        editedSinceFocus = false;
        knownStart = -1;
        knownEnd = -1;
        openEditor(t);
    }

    /**
     * A control lost the focus: its editor closes, and the <code>change</code> event is fired if the user changed the value.
     * <p>
     * 컨트롤이 포커스를 잃었다: 편집기가 닫히고, 사용자가 값을 바꿨으면 <code>change</code> 이벤트가 발생한다.
     *
     * @param t the control / 컨트롤
     */
    private void focusOut(final Element t) {
        if (editing != t) {
            return;
        }
        editing = null;
        if (editedSinceFocus && !value(t).equals(valueAtFocus)) {
            ((HtmlControl) t).fireChangeEvent();
        }
        editedSinceFocus = false;
        if (overlays != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    overlays.close(t);
                    overlays.hideMessage();
                }
            });
        }
    }

    /**
     * Called on the EDT after every rendering of the page: the attributes of the focused control may have changed (read-only, disabled, maxlength), and
     * a control that waited for its box can get its editor now.
     * <p>
     * 페이지가 렌더링될 때마다 EDT 에서 호출된다. 포커스를 가진 컨트롤의 속성(읽기 전용, 비활성, maxlength)이 바뀌었을 수 있고, 상자를 기다리던 컨트롤은 이제 편집기를
     * 얻을 수 있다.
     */
    void afterRendering() {
        if (closed || (editing == null && pending == null)) {
            return;
        }
        session.submitDocumentTask(new Runnable() {
            public void run() {
                Element t = editing;
                if (closed || t == null) {
                    return;
                }
                if (pending == t) {
                    openEditor(t);
                } else {
                    syncEditor(t);
                }
            }
        });
    }

    /**
     * Opens the editor of a text control over the box that the last rendering gave it (or waits for the next rendering).
     * <p>
     * 텍스트 컨트롤의 편집기를 마지막 렌더링이 준 상자 위에 연다(상자가 없으면 다음 렌더링을 기다린다).
     *
     * @param t the control / 컨트롤
     */
    private void openEditor(final Element t) {
        RenderedPage page = view.getRenderedPage();
        ControlSnapshot c = page == null ? null : page.getControl(t);
        if (c == null) {
            pending = t;
            return;
        }
        pending = null;
        String text = value(t);
        int start = 0;
        int end = 0;
        boolean backward = false;
        String direction = "none";
        if (t instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) t;
            Integer s = in.getSelectionStart();
            start = s == null ? text.length() : s.intValue();
            end = s == null ? text.length() : in.getSelectionEnd().intValue();
            direction = in.getSelectionDirection();
        } else {
            HtmlTextAreaElement ta = (HtmlTextAreaElement) t;
            start = ta.getSelectionStart();
            end = ta.getSelectionEnd();
            direction = ta.getSelectionDirection();
        }
        backward = "backward".equals(direction);
        knownStart = start;
        knownEnd = end;
        boolean password = t instanceof HtmlInputElement && "password".equals(((HtmlInputElement) t).getType());
        boolean noWrap = t instanceof HtmlTextAreaElement && "off".equals(((HtmlTextAreaElement) t).getWrap());
        final EditSpec spec = new EditSpec(t, c, password, ControlDisplay.isReadOnly(t), ControlDisplay.isDisabled(t), ControlDisplay.maxLength(t),
                noWrap, text, start, end, backward, new EditEvents(t));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!closed && overlays != null) {
                    overlays.open(spec);
                }
            }
        });
    }

    /**
     * Sends the state of the DOM to the editor of a control that script changed: the text if it is not the text that the editor and the DOM
     * had in common, the selection if the DOM selection moved, and the flags.
     * <p>
     * 스크립트가 바꾼 컨트롤의 DOM 상태를 편집기에 보낸다: 편집기와 DOM 이 공통으로 가진 글과 다르면 글, DOM 선택이 움직였으면 선택, 그리고 플래그.
     *
     * @param t the control / 컨트롤
     */
    private void syncEditor(final Element t) {
        if (overlays == null) {
            return;
        }
        final String now = value(t);
        final String text = now.equals(lastValue) ? null : now;
        lastValue = now;
        int start = 0;
        int end = 0;
        String direction;
        if (t instanceof HtmlInputElement) {
            HtmlInputElement in = (HtmlInputElement) t;
            Integer s = in.getSelectionStart();
            start = s == null ? -1 : s.intValue();
            end = s == null ? -1 : in.getSelectionEnd().intValue();
            direction = in.getSelectionDirection();
        } else {
            HtmlTextAreaElement ta = (HtmlTextAreaElement) t;
            start = ta.getSelectionStart();
            end = ta.getSelectionEnd();
            direction = ta.getSelectionDirection();
        }
        final boolean moved = start >= 0 && (start != knownStart || end != knownEnd);
        knownStart = start;
        knownEnd = end;
        final int s = moved || text != null ? start : -1;
        final int e = end;
        final boolean backward = "backward".equals(direction);
        final boolean readOnly = ControlDisplay.isReadOnly(t);
        final boolean disabled = ControlDisplay.isDisabled(t);
        final int max = ControlDisplay.maxLength(t);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!closed) {
                    overlays.sync(t, text, s, e, backward, readOnly, disabled, max);
                }
            }
        });
    }

    /**
     * Drops text into a text control (the default action of a <code>drop</code> that no listener cancelled): the text is appended to the
     * value (the specification allows inserting it where the pointer is or at the end of the field, and this one does the latter), the
     * <code>input</code> event with the input type <code>insertFromDrop</code> follows, and an open editor shows the new text. Read-only and
     * disabled controls take nothing.
     * <p>
     * 텍스트 컨트롤에 텍스트를 놓는다(리스너가 취소하지 않은 <code>drop</code> 의 기본 동작): 텍스트를 값의 끝에 붙이고(규격은 포인터 위치나 필드 끝에 넣는 것을 허용하며 여기서는
     * 뒤쪽을 따른다), 입력 종류 <code>insertFromDrop</code> 의 <code>input</code> 이벤트가 이어지며, 열려 있는 편집기는 새 텍스트를 보인다. 읽기 전용이나 비활성
     * 컨트롤은 받지 않는다.
     *
     * @param t    the control / 컨트롤
     * @param text the dropped text / 놓은 텍스트
     * @return true if the control took the text / 컨트롤이 텍스트를 받았으면 true
     */
    static boolean insertDropped(Element t, String text) {
        FormHost h = t.getOwnerDocument().getFormHost();
        if (!(h instanceof PageFormHost) || !isTextControl(t) || ControlDisplay.isReadOnly(t) || ControlDisplay.isDisabled(t) || text.length() == 0) {
            return false;
        }
        PageFormHost host = (PageFormHost) h;
        String next = value(t) + text;
        host.applyText(t, next, "insertFromDrop", text);
        // 열려 있는 편집기도 새 텍스트를 보이게 한다.
        host.syncEditor(t, next);
        return true;
    }

    /**
     * Gives the open editor of a control the text that the DOM has now (the selection goes to the end of the text).
     * <p>
     * 컨트롤의 열려 있는 편집기에 DOM 이 지금 가진 텍스트를 준다(선택은 텍스트의 끝으로 간다).
     *
     * @param t    the control / 컨트롤
     * @param text the text / 텍스트
     */
    private void syncEditor(final Element t, final String text) {
        final int end = text.length();
        final boolean readOnly = ControlDisplay.isReadOnly(t);
        final boolean disabled = ControlDisplay.isDisabled(t);
        final int max = ControlDisplay.maxLength(t);
        SwingUtilities.invokeLater(new Runnable() {
            /** Updates the editor on the EDT. / EDT 에서 편집기를 갱신한다. */
            public void run() {
                if (!closed) {
                    overlays.sync(t, text, end, end, false, readOnly, disabled, max);
                }
            }
        });
    }

    /**
     * Applies an edit of the user to the DOM: the new value, then the <code>input</code> event.
     * <p>
     * 사용자의 편집을 DOM 에 적용한다: 새 값, 이어서 <code>input</code> 이벤트.
     *
     * @param t         the control / 컨트롤
     * @param text      the new text / 새 글
     * @param inputType the input type of the event / 이벤트의 입력 종류
     * @param data      the inserted text or null / 삽입한 글 또는 null
     */
    private void applyText(Element t, String text, String inputType, String data) {
        if (closed || !t.isConnected() || !isTextControl(t) || ControlDisplay.isReadOnly(t) || ControlDisplay.isDisabled(t)) {
            return;
        }
        userDepth++;
        try {
            if (t instanceof HtmlTextAreaElement) {
                ((HtmlTextAreaElement) t).setValueFromUser(text);
            } else {
                ((HtmlInputElement) t).setValueFromUser(text);
            }
        } finally {
            userDepth--;
        }
        if (t == editing) {
            lastValue = value(t);
            editedSinceFocus = true;
        }
        ((HtmlControl) t).fireInputEvent(inputType, data);
    }

    /**
     * Applies an edit that the user made in the editor of a control: the <code>beforeinput</code> event comes first (Input Events). If a listener
     * cancels it, the DOM keeps its value and the editor is given that value back, because the Swing editor has already shown the edit; otherwise
     * the value and the <code>input</code> event follow ({@link #applyText(Element, String, String, String)}). The text of an input method that is
     * being composed cannot be cancelled.
     * <p>
     * 사용자가 컨트롤의 편집기에서 한 편집을 적용한다: <code>beforeinput</code> 이벤트가 먼저이다(Input Events). 리스너가 취소하면 DOM 은 값을 그대로 두고, Swing
     * 편집기가 이미 편집을 보였으므로 편집기에 그 값을 되돌려 준다. 아니면 값과 <code>input</code> 이벤트가 이어진다({@link #applyText(Element, String, String, String)}).
     * 입력기가 조합 중인 글은 취소할 수 없다.
     *
     * @param t         the control / 컨트롤
     * @param text      the new text / 새 글
     * @param inputType the input type of the events / 이벤트의 입력 종류
     * @param data      the inserted text or null / 삽입한 글 또는 null
     */
    private void applyUserEdit(Element t, String text, String inputType, String data) {
        if (closed || !t.isConnected() || !isTextControl(t) || ControlDisplay.isReadOnly(t) || ControlDisplay.isDisabled(t)) {
            return;
        }
        if (!((HtmlControl) t).fireBeforeInputEvent(inputType, data)) {
            // 취소: DOM 값은 그대로 두고 열려 있는 편집기를 DOM 값으로 되돌린다.
            if (!closed && t == editing && t.isConnected()) {
                syncEditor(t, value(t));
            }
            return;
        }
        applyText(t, text, inputType, data);
    }

    /**
     * Records the selection of the user in the DOM.
     * <p>
     * 사용자의 선택을 DOM 에 기록한다.
     *
     * @param t        the control / 컨트롤
     * @param start    the selection start / 선택 시작
     * @param end      the selection end / 선택 끝
     * @param backward true for a backward selection / 뒤쪽 방향 선택이면 true
     */
    private void applySelection(Element t, int start, int end, boolean backward) {
        if (closed || !t.isConnected()) {
            return;
        }
        String direction = start == end ? "none" : (backward ? "backward" : "forward");
        if (t instanceof HtmlTextAreaElement) {
            ((HtmlTextAreaElement) t).setSelectionFromUser(start, end, direction);
        } else if (t instanceof HtmlInputElement) {
            ((HtmlInputElement) t).setSelectionFromUser(start, end, direction);
        }
        if (t == editing) {
            knownStart = start;
            knownEnd = end;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 마우스와 키보드로 하는 선택
    // ---------------------------------------------------------------------------------------------------------------------------

    /** The slider that the mouse is dragging, or null. 마우스로 끌고 있는 슬라이더. 없으면 null. */
    private HtmlInputElement dragging;

    /** The select element whose drop-down list is open, or null. 드롭다운 목록이 열려 있는 select. 없으면 null. */
    private HtmlSelectElement pickerOwner;

    /** The option that was chosen last in a list box (the anchor of a range). 리스트 박스에서 마지막으로 고른 option(범위의 기준). */
    private HtmlOptionElement listAnchor;

    /** The characters typed in a row in a select element. select 에 연달아 입력한 글자. */
    private String typed = "";

    /** The time of the last typed character. 마지막 글자를 입력한 시각. */
    private long typedAt;

    /**
     * Sees the mouse events of the document: a click on a select element opens its list (or chooses a row), a click on a color control asks for
     * a color, and a press on a slider moves its thumb (the drag follows the mouse).
     * <p>
     * 문서의 마우스 이벤트를 본다: select 를 누르면 목록이 열리거나(또는 행이 골라지고), 색 컨트롤을 누르면 색을 묻고, 슬라이더를 누르면 손잡이가 움직인다(끌기는 마우스를
     * 따른다).
     *
     * @param target the target of the event / 이벤트 대상
     * @param event  the mouse event / 마우스 이벤트
     */
    private void mouse(Element target, MouseEvent event) {
        String type = event.getType();
        if ("mousemove".equals(type)) {
            // 단추를 누르지 않은 채 움직이면 끌기가 끝난 것이다.
            if (dragging != null && (event.getButtons() & 1) == 0) {
                dragging = null;
            } else if (dragging != null) {
                slideFromMouse(dragging, event.getPageX());
            }
            return;
        }
        if ("mouseup".equals(type)) {
            if (dragging != null) {
                HtmlInputElement range = dragging;
                dragging = null;
                range.fireChangeEvent();
            }
            return;
        }
        if (!event.isTrusted() || event.isDefaultPrevented() || event.getButton() != 0) {
            return;
        }
        if ("mousedown".equals(type)) {
            if (target instanceof HtmlInputElement && "range".equals(((HtmlInputElement) target).getType()) && !ControlDisplay.isDisabled(target)) {
                dragging = (HtmlInputElement) target;
                slideFromMouse(dragging, event.getPageX());
            }
            return;
        }
        // click
        if (target instanceof HtmlSelectElement && !ControlDisplay.isDisabled(target)) {
            HtmlSelectElement select = (HtmlSelectElement) target;
            if (ControlDisplay.isListBox(select)) {
                clickList(select, event);
            } else if (pickerOwner == select) {
                closePicker();
            } else {
                openDropdown(select);
            }
        } else if (target instanceof HtmlInputElement && "color".equals(((HtmlInputElement) target).getType()) && !ControlDisplay.isDisabled(target)) {
            chooseColor((HtmlInputElement) target);
        }
    }

    /**
     * Moves the thumb of a slider to the horizontal position of the mouse (a position in the page).
     * <p>
     * 슬라이더의 손잡이를 마우스의 가로 위치(페이지의 위치)로 옮긴다.
     *
     * @param range the slider / 슬라이더
     * @param pageX the x of the mouse in page coordinates / 페이지 좌표의 마우스 x
     */
    private void slideFromMouse(HtmlInputElement range, double pageX) {
        RenderedPage page = view.getRenderedPage();
        ControlSnapshot c = page == null ? null : page.getControl(range);
        if (c == null || c.getWidth() <= 0) {
            return;
        }
        double thumb = Math.min(c.getHeight(), 15);
        double ratio = (pageX - (c.getX() + thumb / 2)) / Math.max(1, c.getWidth() - thumb);
        ControlActions.slideTo(range, ratio);
    }

    /**
     * A click on a list box: chooses the row under the mouse (with the Ctrl key it toggles, with the Shift key it extends from the last choice).
     * <p>
     * 리스트 박스를 누름: 마우스 아래의 행을 고른다(Ctrl 키는 토글하고 Shift 키는 마지막으로 고른 곳에서 넓힌다).
     *
     * @param select the list box / 리스트 박스
     * @param event  the click / 클릭
     */
    private void clickList(HtmlSelectElement select, MouseEvent event) {
        RenderedPage page = view.getRenderedPage();
        ControlSnapshot c = page == null ? null : page.getControl(select);
        if (c == null) {
            return;
        }
        int row = (int) Math.floor((event.getPageY() - c.getY()) / c.getLineHeight()) + c.getTopRow();
        List<Element> rows = ControlDisplay.rowElements(select);
        if (row < 0 || row >= rows.size() || !(rows.get(row) instanceof HtmlOptionElement)) {
            return;
        }
        HtmlOptionElement option = (HtmlOptionElement) rows.get(row);
        if (event.isShiftKey() && select.isMultiple() && listAnchor != null && listAnchor.isConnected()) {
            ControlActions.chooseRange(select, listAnchor, option);
        } else {
            ControlActions.choose(select, option, event.isCtrlKey() || event.isMetaKey());
            listAnchor = option;
        }
    }

    /**
     * Opens the drop-down list of a select element under its control.
     * <p>
     * select 의 드롭다운 목록을 컨트롤 아래에 연다.
     *
     * @param select the select element / select 요소
     */
    private void openDropdown(final HtmlSelectElement select) {
        RenderedPage page = view.getRenderedPage();
        ControlSnapshot c = page == null || overlays == null ? null : page.getControl(select);
        if (c == null) {
            return;
        }
        final List<Element> rows = ControlDisplay.rowElements(select);
        String[] labels = new String[rows.size()];
        boolean[] enabled = new boolean[rows.size()];
        boolean[] group = new boolean[rows.size()];
        int[] indent = new int[rows.size()];
        int selected = -1;
        // 행마다 글, 고를 수 있는지, 그룹 라벨인지, 들여쓰기를 모은다.
        for (int i = 0; i < rows.size(); i++) {
            Element e = rows.get(i);
            if (e instanceof HtmlOptionElement) {
                HtmlOptionElement o = (HtmlOptionElement) e;
                labels[i] = o.getLabel();
                enabled[i] = !o.isDisabled();
                indent[i] = e.getParentNode() instanceof Element && "optgroup".equals(((Element) e.getParentNode()).getLocalName()) ? 1 : 0;
                if (o.isSelected() && selected < 0) {
                    selected = i;
                }
            } else {
                String label = e.getAttribute("label");
                labels[i] = label == null ? "" : label;
                group[i] = true;
            }
        }
        pickerOwner = select;
        final PickSpec spec = new PickSpec(select, c, labels, enabled, group, indent, selected, new PickEvents(select, rows));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (!closed && overlays != null) {
                    overlays.openPicker(spec);
                }
            }
        });
    }

    /**
     * Closes the drop-down list.
     * <p>
     * 드롭다운 목록을 닫는다.
     */
    private void closePicker() {
        pickerOwner = null;
        if (overlays != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    overlays.closePicker();
                }
            });
        }
    }

    /**
     * Asks the user for the color of a color control and takes the answer as the value: <code>input</code> and <code>change</code> follow.
     * <p>
     * 색 컨트롤의 색을 사용자에게 묻고 답을 값으로 취한다: <code>input</code> 과 <code>change</code> 가 이어서 발생한다.
     *
     * @param in the color control / 색 컨트롤
     */
    private void chooseColor(HtmlInputElement in) {
        if (overlays == null) {
            return;
        }
        int current = 0;
        try {
            current = Integer.parseInt(in.getValue().substring(1), 16);
        } catch (RuntimeException e) {
            // 값이 #rrggbb 형식이 아니면 검정에서 시작한다.
            current = 0;
        }
        Integer chosen = overlays.chooseColor(current);
        if (chosen == null) {
            return;
        }
        String hex = Integer.toHexString(0x1000000 | (chosen.intValue() & 0xFFFFFF)).substring(1);
        String before = in.getValue();
        in.setValueFromUser("#" + hex);
        if (!in.getValue().equals(before)) {
            in.fireInputEvent(null, null);
            in.fireChangeEvent();
        }
    }

    /**
     * The default action of a key press on a control that has no text to edit: a select element, a slider or a radio button. Called on the
     * document thread when the key event was not cancelled.
     * <p>
     * 편집할 글이 없는 컨트롤(select, 슬라이더, 라디오 단추)에서 키를 눌렀을 때의 기본 동작. 키 이벤트가 취소되지 않았을 때 문서 스레드에서 호출된다.
     *
     * @param target the element that has the focus / 포커스를 가진 요소
     * @param key    the <code>key</code> value / <code>key</code> 값
     * @param alt    true if the Alt key is down / Alt 키가 눌려 있으면 true
     * @param ctrl   true if the Ctrl key is down / Ctrl 키가 눌려 있으면 true
     */
    static void keyDown(Element target, String key, boolean alt, boolean ctrl) {
        FormHost h = target.getOwnerDocument().getFormHost();
        PageFormHost host = h instanceof PageFormHost ? (PageFormHost) h : null;
        if (target instanceof HtmlSelectElement && !ControlDisplay.isDisabled(target)) {
            if (host != null) {
                host.selectKey((HtmlSelectElement) target, key, alt, ctrl);
            } else {
                selectMove((HtmlSelectElement) target, key);
            }
        } else if (target instanceof HtmlInputElement && !ControlDisplay.isDisabled(target)) {
            HtmlInputElement in = (HtmlInputElement) target;
            if ("range".equals(in.getType())) {
                ControlActions.slide(in, key);
            } else if ("radio".equals(in.getType())) {
                if ("ArrowDown".equals(key) || "ArrowRight".equals(key)) {
                    ControlActions.moveRadio(in, 1);
                } else if ("ArrowUp".equals(key) || "ArrowLeft".equals(key)) {
                    ControlActions.moveRadio(in, -1);
                }
            }
        }
    }

    /**
     * Moves the selection of a select element with an arrow, Home, End, Page Up or Page Down key.
     * <p>
     * 화살표, Home, End, Page Up, Page Down 키로 select 의 선택을 옮긴다.
     *
     * @param select the select element / select 요소
     * @param key    the key / 키
     * @return true if the key was a key that moves the selection / 선택을 옮기는 키였으면 true
     */
    private static boolean selectMove(HtmlSelectElement select, String key) {
        if ("ArrowDown".equals(key) || "ArrowRight".equals(key)) {
            ControlActions.move(select, 1);
        } else if ("ArrowUp".equals(key) || "ArrowLeft".equals(key)) {
            ControlActions.move(select, -1);
        } else if ("Home".equals(key)) {
            ControlActions.move(select, Integer.MIN_VALUE);
        } else if ("End".equals(key)) {
            ControlActions.move(select, Integer.MAX_VALUE);
        } else if ("PageDown".equals(key)) {
            ControlActions.move(select, Math.max(1, select.getDisplaySize() - 1));
        } else if ("PageUp".equals(key)) {
            ControlActions.move(select, -Math.max(1, select.getDisplaySize() - 1));
        } else {
            return false;
        }
        return true;
    }

    /**
     * The keys of a select element: the arrows, Home, End, Page Up and Page Down move the selection; Enter, the space key and Alt+Down open
     * the list of a drop-down; letters find an option by its first characters.
     * <p>
     * select 의 키: 화살표, Home, End, Page Up, Page Down 은 선택을 옮기고, Enter, 스페이스 키, Alt+아래쪽은 드롭다운의 목록을 열며, 글자는 option 을 첫 글자로 찾는다.
     *
     * @param select the select element / select 요소
     * @param key    the key / 키
     * @param alt    true if the Alt key is down / Alt 키가 눌려 있으면 true
     * @param ctrl   true if the Ctrl key is down / Ctrl 키가 눌려 있으면 true
     */
    private void selectKey(HtmlSelectElement select, String key, boolean alt, boolean ctrl) {
        boolean dropdown = !ControlDisplay.isListBox(select);
        boolean open = alt && "ArrowDown".equals(key) || "Enter".equals(key) || " ".equals(key) && typed.length() == 0 || "F4".equals(key);
        if (dropdown && open) {
            if (pickerOwner == select) {
                closePicker();
            } else {
                openDropdown(select);
            }
        } else if (selectMove(select, key)) {
            typed = "";
        } else if (key.length() == 1 && !ctrl && !alt) {
            long now = System.currentTimeMillis();
            typed = now - typedAt > 1000 ? key : typed + key;
            typedAt = now;
            ControlActions.find(select, typed);
        }
    }

    /**
     * Receives what the user chose in the drop-down list of one select element and passes it to the document thread.
     * <p>
     * select 하나의 드롭다운 목록에서 사용자가 고른 것을 받아 문서 스레드에 넘긴다.
     */
    private final class PickEvents implements PickSpec.Listener {

        /** The select element. select 요소. */
        private final HtmlSelectElement select;

        /** The rows of the list: options and option groups. 목록의 행: option 과 optgroup. */
        private final List<Element> rows;

        /**
         * Creates the receiver.
         * <p>
         * 받는 쪽을 만든다.
         *
         * @param select the select element / select 요소
         * @param rows   the rows / 행
         */
        PickEvents(HtmlSelectElement select, List<Element> rows) {
            this.select = select;
            this.rows = rows;
        }

        /**
         * Chooses the option of the row.
         * <p>
         * 행의 option 을 고른다.
         *
         * @param row the row / 행
         */
        public void picked(final int row) {
            overlays.closePicker();
            session.submitDocumentTask(new Runnable() {
                public void run() {
                    pickerOwner = null;
                    Element e = rows.get(row);
                    if (!closed && e instanceof HtmlOptionElement && e.isConnected()) {
                        ControlActions.choose(select, (HtmlOptionElement) e, false);
                    }
                }
            });
        }

        /**
         * Closes the list.
         * <p>
         * 목록을 닫는다.
         */
        public void cancelled() {
            overlays.closePicker();
            session.submitDocumentTask(new Runnable() {
                public void run() {
                    pickerOwner = null;
                }
            });
        }

        /**
         * Tells whether the page is the page that the component shows.
         * <p>
         * 페이지가 컴포넌트가 보여 주는 페이지인지 알려 준다.
         *
         * @return true if current / 현재 페이지이면 true
         */
        public boolean isCurrent() {
            return !closed && view.isCurrent(presentation);
        }
    }

    /**
     * Calls {@link PageFormHost#afterRendering()} after every rendering without keeping the host alive: the overlays outlive the pages, so a strong
     * reference would keep the document of every page that was shown.
     * <p>
     * 렌더링이 끝날 때마다 {@link PageFormHost#afterRendering()} 을 호출하되 호스트를 붙잡지 않는다. 오버레이는 페이지보다 오래 살므로 강한 참조는 보였던 모든 페이지의
     * 문서를 붙잡게 된다.
     */
    private static final class RenderObserver implements Runnable {

        /** The host, weakly held. 호스트(약한 참조). */
        private final java.lang.ref.WeakReference<PageFormHost> host;

        /** The overlays that this observer is registered with. 이 관찰자가 등록된 오버레이. */
        private final ControlOverlays overlays;

        /**
         * Creates the observer.
         * <p>
         * 관찰자를 만든다.
         *
         * @param host     the host / 호스트
         * @param overlays the overlays / 오버레이
         */
        RenderObserver(PageFormHost host, ControlOverlays overlays) {
            this.host = new java.lang.ref.WeakReference<PageFormHost>(host);
            this.overlays = overlays;
        }

        /**
         * Retries the pending editor, or unregisters when the host is gone.
         * <p>
         * 대기 중인 편집기를 다시 시도한다. 호스트가 없어졌으면 등록을 해제한다.
         */
        public void run() {
            PageFormHost h = host.get();
            if (h == null || h.closed) {
                overlays.removeLayoutObserver(this);
            } else {
                h.afterRendering();
            }
        }
    }

    /**
     * Receives the edits of the editor of one control and passes them to the document thread.
     * <p>
     * 컨트롤 하나의 편집기가 하는 편집을 받아 문서 스레드에 넘긴다.
     */
    private final class EditEvents implements EditSpec.Listener {

        /** The control. 컨트롤. */
        private final Element control;

        /**
         * Creates the receiver.
         * <p>
         * 받는 쪽을 만든다.
         *
         * @param control the control / 컨트롤
         */
        EditEvents(Element control) {
            this.control = control;
        }

        /**
         * Passes a text edit on.
         * <p>
         * 글 편집을 넘긴다.
         *
         * @param text      the text / 글
         * @param inputType the input type / 입력 종류
         * @param data      the inserted text / 삽입한 글
         */
        public void textEdited(final String text, final String inputType, final String data) {
            if (overlays != null) {
                overlays.hideMessage();
            }
        session.submitDocumentTask(new Runnable() {
                public void run() {
                    applyUserEdit(control, text, inputType, data);
                }
            });
        }

        /**
         * Passes a selection change on.
         * <p>
         * 선택 변경을 넘긴다.
         *
         * @param start    the start / 시작
         * @param end      the end / 끝
         * @param backward true if backward / 뒤쪽이면 true
         */
        public void selectionEdited(final int start, final int end, final boolean backward) {
            session.submitDocumentTask(new Runnable() {
                public void run() {
                    applySelection(control, start, end, backward);
                }
            });
        }

        /**
         * Passes a composition event on.
         * <p>
         * 조합 이벤트를 넘긴다.
         *
         * @param type the event type / 이벤트 종류
         * @param data the composed text / 조합 중인 글
         */
        public void composition(final String type, final String data) {
            session.submitDocumentTask(new Runnable() {
                public void run() {
                    if (closed || !control.isConnected()) {
                        return;
                    }
                    CompositionEvent ev = new CompositionEvent(type, true, false);
                    ev.setData(data);
                    control.dispatchTrustedEvent(ev);
                }
            });
        }

        /**
         * Sends the <code>cut</code>, <code>copy</code> or <code>paste</code> event to the control with the text as its
         * <code>text/plain</code> data, and gives the answer to the callback: whether a listener cancelled the event and the data, which a
         * script may have changed.
         * <p>
         * 텍스트를 <code>text/plain</code> 데이터로 가진 <code>cut</code>, <code>copy</code>, <code>paste</code> 이벤트를 컨트롤에 보내고, 리스너가 이벤트를 취소했는지와
         * (스크립트가 바꾸었을 수 있는) 데이터를 콜백에 알린다.
         *
         * @param type     the event type / 이벤트 종류
         * @param plain    the text / 글
         * @param html     the HTML of the clipboard for a paste, or null / 붙여넣기의 클립보드 HTML 또는 null
         * @param callback receives the answer / 답을 받는다
         */
        public void clipboard(final String type, final String plain, final String html, final EditSpec.ClipboardCallback callback) {
            session.submitDocumentTask(new Runnable() {
                public void run() {
                    org.duckdns.hjow.swingwebpanel.dom.ClipboardData data = new org.duckdns.hjow.swingwebpanel.dom.ClipboardData();
                    if (plain.length() > 0) {
                        data.put("text/plain", plain);
                    }
                    // 붙여넣기는 클립보드에 있는 HTML 도 보여 준다(컨트롤에 넣는 것은 글뿐이다, Clipboard API).
                    if (html != null && html.length() > 0) {
                        data.put("text/html", html);
                    }
                    // 붙여넣기 이벤트의 clipboardData 는 읽기 전용이다.
                    if ("paste".equals(type)) {
                        data.setMode(org.duckdns.hjow.swingwebpanel.dom.ClipboardData.READ_ONLY);
                    }
                    // 페이지가 이미 닫혔거나 컨트롤이 떼어졌으면 이벤트 없이 기본 동작을 하도록 알린다.
                    boolean notCancelled = closed || !control.isConnected()
                            || control.dispatchTrustedEvent(new org.duckdns.hjow.swingwebpanel.dom.ClipboardEvent(type, true, true, data));
                    callback.done(!notCancelled, data);
                }
            });
        }

        /**
         * Tells whether the page of the control is the page that the component shows.
         * <p>
         * 컨트롤의 페이지가 컴포넌트가 보여 주는 페이지인지 알려 준다.
         *
         * @return true if current / 현재 페이지이면 true
         */
        public boolean isCurrent() {
            return !closed && view.isCurrent(presentation);
        }
    }

    /**
     * The Enter key in a text field: commits the value (the <code>change</code> event if the user changed it) and submits the form of the field
     * (implicit submission). Called on the document thread.
     * <p>
     * 텍스트 필드에서 Enter 키: 값을 확정하고(사용자가 바꿨으면 <code>change</code> 이벤트) 필드의 폼을 제출한다(암묵적 제출). 문서 스레드에서 호출한다.
     *
     * @param field the focused text field / 포커스를 가진 텍스트 필드
     */
    static void enterPressed(Element field) {
        if (!(field instanceof HtmlInputElement) || !isTextControl(field)) {
            return;
        }
        FormHost h = field.getOwnerDocument().getFormHost();
        if (h instanceof PageFormHost) {
            PageFormHost host = (PageFormHost) h;
            // 사용자가 바꾼 값이 있으면 제출 전에 change 이벤트로 확정한다.
            if (host.editing == field && host.editedSinceFocus && !value(field).equals(host.valueAtFocus)) {
                ((HtmlControl) field).fireChangeEvent();
                host.valueAtFocus = value(field);
                host.editedSinceFocus = false;
            }
        }
        HtmlFormElement form = ((HtmlInputElement) field).getForm();
        if (form != null) {
            form.implicitSubmit();
        }
    }
}
