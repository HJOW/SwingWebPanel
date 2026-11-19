package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * Keyboard and focus input of the rendered component (plan items P11-01 and P11-02): AWT key events become DOM <code>keydown</code>,
 * <code>keypress</code> and <code>keyup</code> events for the element that has the focus (the body if none has it), a key that is not
 * cancelled does what the browser does (Tab moves the focus along the tab order and leaves the component after the last element, Enter
 * follows a link or presses a button, Space releases a button), and the focus of the component itself is passed on to the document so
 * that its focused element gets <code>focus</code> and <code>blur</code>. The scrolling keys are not consumed: the scroll pane that holds
 * the component handles them.
 * <p>
 * 렌더링 컴포넌트의 키보드와 포커스 입력(계획 P11-01, P11-02): AWT 키 이벤트가 포커스를 가진 요소(없으면 body)의 DOM <code>keydown</code>,
 * <code>keypress</code>, <code>keyup</code> 이벤트가 되고, 취소되지 않은 키는 브라우저가 하는 일을 한다(Tab 은 Tab 순서대로 포커스를 옮기고 마지막 요소
 * 다음에서는 컴포넌트를 떠나며, Enter 는 링크를 따라가거나 단추를 누르고, Space 는 단추를 뗀다). 컴포넌트 자신의 포커스는 문서에 전달되어 포커스를 가진
 * 요소가 <code>focus</code> 와 <code>blur</code> 를 받는다. 스크롤 키는 소비하지 않으며 컴포넌트를 담은 스크롤 창이 처리한다.
 */
public final class PageKeyboard extends KeyAdapter implements FocusListener {

    /**
     * What the DOM events need from one AWT key event, copied on the EDT.
     * <p>
     * 한 AWT 키 이벤트에서 DOM 이벤트가 필요로 하는 값. EDT 에서 복사해 둔다.
     */
    private static final class Key {
        /** The DOM key value (for example "a" or "Enter"). / DOM key 값(예: "a", "Enter"). */
        final String key;
        /** The DOM physical key. / DOM 물리적 키. */
        final String code;
        /** The DOM location. / DOM 위치. */
        final int location;
        /** The legacy key code. / 예전 키 코드. */
        final int keyCode;
        /** The legacy character code (for keypress). / 예전 문자 코드(keypress 용). */
        final int charCode;
        /** The extended modifiers. / 확장 보조 값. */
        final int modifiersEx;
        /** True if the key repeats. / 키가 반복되면 true. */
        final boolean repeat;
        /** True if an input method was composing text when the key arrived (<code>isComposing</code>). / 키가 도착했을 때 입력기가 텍스트를 조합 중이었으면 true(<code>isComposing</code>). */
        final boolean composing;
        /** The clipboard contents read on the EDT for a paste key, else null. / 붙여넣기 키에 대해 EDT 에서 읽은 클립보드 내용. 아니면 null. */
        org.duckdns.hjow.swingwebpanel.dom.ClipboardData clipboard;

        /**
         * Copies the values of an AWT event.
         * <p>
         * AWT 이벤트의 값을 복사한다.
         *
         * @param e      the event / 이벤트
         * @param again  true if the key was already down / 키가 이미 눌려 있었으면 true
         * @param composing true if an input method composes text now / 입력기가 지금 텍스트를 조합 중이면 true
         */
        Key(KeyEvent e, boolean again, boolean composing) {
            int vk = e.getKeyCode();
            char ch = e.getKeyChar();
            int loc = KeyNames.location(e.getKeyLocation());
            if (e.getID() == KeyEvent.KEY_TYPED) {
                key = ch == '\r' || ch == '\n' ? "Enter" : String.valueOf(ch);
                code = "";
                keyCode = 0;
                charCode = ch == '\n' ? 13 : ch;
            } else {
                String name = KeyNames.key(vk, ch);
                // Ctrl 과 함께 누른 글자 키는 플랫폼이 제어 문자를 알리거나 문자를 알리지 않는다. 브라우저는 이때도 글자("v")를 key 로 준다(Shift 이면 대문자).
                if ("Unidentified".equals(name) && vk >= KeyEvent.VK_A && vk <= KeyEvent.VK_Z) {
                    name = String.valueOf((char) ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0 ? vk : vk - 'A' + 'a'));
                }
                key = name;
                code = KeyNames.code(vk, loc);
                keyCode = KeyNames.legacyKeyCode(vk);
                charCode = 0;
            }
            location = loc;
            modifiersEx = e.getModifiersEx();
            repeat = again;
            this.composing = composing;
        }

        /**
         * Tells whether a modifier is down.
         * <p>
         * 보조 키가 눌려 있는지 알려 준다.
         *
         * @param mask the AWT extended mask / AWT 확장 마스크
         * @return true if down / 눌려 있으면 true
         */
        boolean has(int mask) {
            return (modifiersEx & mask) != 0;
        }
    }

    /**
     * The Swing component that has the keyboard focus.
     * <p>
     * 키보드 포커스를 가진 Swing 컴포넌트.
     */
    private final JComponent component;

    /**
     * The session that owns the document work.
     * <p>
     * 문서 작업을 소유한 세션.
     */
    private final BrowserSession session;

    /**
     * The view whose rendering tells which elements are displayed.
     * <p>
     * 렌더링으로 어떤 요소가 표시되는지 알려 주는 뷰.
     */
    private final PageView view;

    /**
     * The pointer input, which knows how to follow a link and keeps the element state.
     * <p>
     * 링크를 따라가는 법을 알고 요소 상태를 유지하는 포인터 입력.
     */
    private final PageInput input;

    /**
     * The key codes that are down, to recognize repeats; used on the EDT only.
     * <p>
     * 반복을 알아보기 위한 눌려 있는 키 코드. EDT 에서만 쓴다.
     */
    private final Set<Integer> down = new HashSet<Integer>();

    /**
     * True while the keydown of the key that is being typed was cancelled, so that the typed character gives no keypress and no input;
     * used on the document queue only.
     * <p>
     * 입력 중인 키의 keydown 이 취소되었으면 true. 입력한 글자는 keypress 도 입력도 만들지 않는다. 문서 큐에서만 쓴다.
     */
    private boolean typedSuppressed;

    /**
     * Changes the page zoom for the zoom keys, or null.
     * <p>
     * 확대 키로 페이지 확대를 바꾸는 쪽. 없으면 null.
     */
    private volatile ZoomControl zoomControl;

    /**
     * True after disposal.
     * <p>
     * 폐기 뒤에는 true.
     */
    private volatile boolean disposed;

    /**
     * True while an input method composes text (<code>isComposing</code> of the key events that arrive meanwhile); the composition handler keeps it.
     * <p>
     * 입력기가 텍스트를 조합하는 동안 true(그동안 도착하는 키 이벤트의 <code>isComposing</code>). 조합 처리기가 갱신한다.
     */
    private volatile boolean composing;

    /**
     * Tells the keyboard whether an input method composes text now, so that the key events that reach the page meanwhile say <code>isComposing</code>.
     * <p>
     * 입력기가 지금 텍스트를 조합하는지 키보드에 알려, 그동안 페이지에 닿는 키 이벤트가 <code>isComposing</code> 을 알리게 한다.
     *
     * @param value true while composing / 조합 중이면 true
     */
    void setComposing(boolean value) {
        composing = value;
    }

    /**
     * Creates the controller and turns off the focus traversal keys of the component, so that the Tab key reaches the page.
     * <p>
     * 제어기를 만들고 Tab 키가 페이지에 닿도록 컴포넌트의 포커스 이동 키를 끈다.
     *
     * @param component the component / 컴포넌트
     * @param session   the session / 세션
     * @param view      the view / 뷰
     * @param input     the pointer input / 포인터 입력
     */
    public PageKeyboard(JComponent component, BrowserSession session, PageView view, PageInput input) {
        this.component = component;
        this.session = session;
        this.view = view;
        this.input = input;
        component.setFocusTraversalKeysEnabled(false);
    }

    /**
     * Sends <code>keydown</code> and runs the default action of the key if no listener cancelled the event.
     * <p>
     * <code>keydown</code> 을 보내고 리스너가 이벤트를 취소하지 않았으면 키의 기본 동작을 한다.
     *
     * @param event the AWT event / AWT 이벤트
     */
    public void keyPressed(KeyEvent event) {
        // 끌어다 놓기가 도는 동안 키 입력은 막는다. Esc 만 끌기를 취소한다.
        PageDragDrop dnd = input.getDragDrop();
        if (dnd.isActive()) {
            if (event.getKeyCode() == KeyEvent.VK_ESCAPE) {
                dnd.cancel();
            }
            event.consume();
            return;
        }
        boolean again = !down.add(Integer.valueOf(event.getKeyCode()));
        zoomKey(event);
        Key key = new Key(event, again, composing);
        // 붙여넣기 키이면 클립보드를 이벤트 디스패치 스레드에서 읽어 둔다(페이지가 편집 가능한 곳이면 문서 큐에서 붙여넣는다).
        if (isPasteKey(event)) {
            key.clipboard = input.getEditing().readClipboard();
        }
        // 편집 호스트에 포커스가 있으면 캐럿 키를 스크롤 창의 키 바인딩이 처리하지 않게 소비한다(캐럿만 움직인다).
        if (input.getSelection().isEditableFocus() && PageEditing.isCaretKey(key.key)) {
            event.consume();
        }
        send("keydown", key, true);
    }

    /**
     * Tells whether a key event is the paste key: Ctrl+V or Shift+Insert.
     * <p>
     * 키 이벤트가 붙여넣기 키인지 알려 준다: Ctrl+V 또는 Shift+Insert.
     *
     * @param event the AWT event / AWT 이벤트
     * @return true for a paste key / 붙여넣기 키이면 true
     */
    private static boolean isPasteKey(KeyEvent event) {
        int code = event.getKeyCode();
        boolean ctrlV = code == KeyEvent.VK_V && event.isControlDown() && !event.isAltDown() && !event.isShiftDown();
        boolean shiftInsert = code == KeyEvent.VK_INSERT && event.isShiftDown() && !event.isControlDown() && !event.isAltDown();
        return ctrlV || shiftInsert;
    }

    /**
     * Sets the object that changes the page zoom for Ctrl+plus, Ctrl+minus and Ctrl+0 (null: these keys do nothing special).
     * <p>
     * Ctrl+플러스, Ctrl+마이너스, Ctrl+0 이 페이지 확대를 바꾸는 데 쓰는 객체를 정한다(null 이면 이 키들은 특별한 일을 하지 않는다).
     *
     * @param control the zoom control or null / 확대 제어. 없으면 null
     */
    public void setZoomControl(ZoomControl control) {
        this.zoomControl = control;
    }

    /**
     * Changes the page zoom for the zoom keys: Ctrl with plus (or equals) zooms in one step, Ctrl with minus zooms out, Ctrl with 0 restores
     * 100 percent. The key is also sent to the page as <code>keydown</code>, as for every key.
     * <p>
     * 확대 키로 페이지 확대를 바꾼다: Ctrl 과 플러스(또는 등호)는 한 단계 확대, Ctrl 과 마이너스는 축소, Ctrl 과 0 은 100 % 로 되돌린다. 다른 모든 키처럼
     * 이 키도 <code>keydown</code> 으로 페이지에 보낸다.
     *
     * @param event the AWT event / AWT 이벤트
     */
    private void zoomKey(KeyEvent event) {
        ZoomControl zoomer = zoomControl;
        if (zoomer == null || !event.isControlDown() || event.isAltDown()) {
            return;
        }
        int code = event.getKeyCode();
        // 플러스(등호 포함), 마이너스, 0 키를 구분해 한 단계 확대, 축소, 원래 크기로 바꾼다.
        if (code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD || code == KeyEvent.VK_EQUALS) {
            zoomer.setZoom(PageZoom.stepUp(zoomer.getZoom()));
        } else if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT) {
            zoomer.setZoom(PageZoom.stepDown(zoomer.getZoom()));
        } else if (code == KeyEvent.VK_0 || code == KeyEvent.VK_NUMPAD0) {
            zoomer.setZoom(PageZoom.DEFAULT);
        }
    }

    /**
     * Sends <code>keypress</code> for a key that produces a character.
     * <p>
     * 글자를 만드는 키에는 <code>keypress</code> 를 보낸다.
     *
     * @param event the AWT event / AWT 이벤트
     */
    public void keyTyped(KeyEvent event) {
        if (input.getDragDrop().isActive()) {
            return;
        }
        char ch = event.getKeyChar();
        // 제어 문자(Enter 와 탭 제외)는 글자가 아니므로 keypress 를 보내지 않는다. Ctrl 이나 Alt 조합도 글자가 아니다(Ctrl 과 Alt 가 함께이면 AltGr 로 만든 글자이다).
        boolean altGr = event.isControlDown() && event.isAltDown();
        if (ch == KeyEvent.CHAR_UNDEFINED || (ch < 32 && ch != '\r' && ch != '\n') || ch == 127 || ((event.isControlDown() || event.isAltDown()) && !altGr)) {
            return;
        }
        send("keypress", new Key(event, false, composing), true);
    }

    /**
     * Sends <code>keyup</code> and runs the default action that a key has when it is released (Space on a button).
     * <p>
     * <code>keyup</code> 을 보내고 키를 뗄 때 하는 기본 동작(단추의 Space)을 한다.
     *
     * @param event the AWT event / AWT 이벤트
     */
    public void keyReleased(KeyEvent event) {
        down.remove(Integer.valueOf(event.getKeyCode()));
        send("keyup", new Key(event, false, composing), true);
    }

    /**
     * Tells the document that the component gained the keyboard focus: the focused element gets <code>focus</code>.
     * <p>
     * 컴포넌트가 키보드 포커스를 얻었음을 문서에 알린다. 포커스를 가진 요소가 <code>focus</code> 를 받는다.
     *
     * @param event the AWT event / AWT 이벤트
     */
    public void focusGained(FocusEvent event) {
        // 페이지와 그 위의 편집기 사이에서 포커스가 옮겨 가는 것은 창 포커스의 변화가 아니다.
        if (insidePage(event.getOppositeComponent())) {
            return;
        }
        windowFocus(true);
    }

    /**
     * Tells the document that the component lost the keyboard focus: the focused element gets <code>blur</code> and keeps being the
     * focused element.
     * <p>
     * 컴포넌트가 키보드 포커스를 잃었음을 문서에 알린다. 포커스를 가진 요소가 <code>blur</code> 를 받고 계속 포커스를 가진 요소로 남는다.
     *
     * @param event the AWT event / AWT 이벤트
     */
    public void focusLost(FocusEvent event) {
        // 포커스가 페이지의 편집기로 옮겨 가는 것은 창 포커스를 잃는 것이 아니다.
        if (insidePage(event.getOppositeComponent())) {
            return;
        }
        down.clear();
        // 끌기 중에 창이 포커스를 잃으면(다른 창으로 전환) 끌기는 취소된다.
        input.getDragDrop().cancel();
        windowFocus(false);
    }

    /**
     * Tells whether a component is the page component or one of its children (an editor of a form control).
     * <p>
     * 컴포넌트가 페이지 컴포넌트이거나 그 자식(폼 컨트롤의 편집기)인지 알려 준다.
     *
     * @param c the component, may be null / 컴포넌트. null 가능
     * @return true if it belongs to the page / 페이지에 속하면 true
     */
    private boolean insidePage(java.awt.Component c) {
        return c != null && (c == component || SwingUtilities.isDescendingFrom(c, component));
    }

    /**
     * Permanently stops the keyboard input.
     * <p>
     * 키보드 입력을 영구히 멈춘다.
     */
    public void dispose() {
        disposed = true;
    }

    /**
     * Queues the focus change of the component for the document.
     * <p>
     * 컴포넌트의 포커스 변화를 문서에 전달하도록 예약한다.
     *
     * @param gained true if the component gained the focus / 포커스를 얻었으면 true
     */
    private void windowFocus(final boolean gained) {
        if (disposed) {
            return;
        }
        final PageSnapshot snapshot = session.getSnapshot();
        session.submitDocumentTask(new Runnable() {
            /** Sends the events on the document queue. / 문서 큐에서 이벤트를 보낸다. */
            public void run() {
                Document doc = topDocument(snapshot);
                if (doc == null) {
                    return;
                }
                doc.setWindowFocused(gained);
                // window 의 focus 는 요소의 focus 보다 먼저, window 의 blur 는 요소의 blur 보다 나중에 간다(HTML 의 포커스 갱신 순서: 가장 바깥의 window 가 가장 먼저 얻고 가장 나중에 잃는다).
                HtmlPageDocument top = snapshot.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) snapshot.getDocument() : null;
                if (gained && top != null) {
                    top.windowFocus(true);
                }
                FocusManager.windowFocusChanged(doc, gained);
                if (!gained && top != null) {
                    top.windowFocus(false);
                }
                // 키보드 포커스를 가진 프레임의 문서도 창 포커스를 함께 얻고 잃는다.
                Document inFrame = document(snapshot);
                if (inFrame != null && inFrame != doc) {
                    inFrame.setWindowFocused(gained);
                    FocusManager.windowFocusChanged(inFrame, gained);
                }
                input.focusChanged(snapshot);
            }
        });
    }

    /**
     * Returns the DOM document of a page that is still the current one, or null.
     * <p>
     * 아직 현재인 페이지의 DOM 문서를 반환한다. 아니면 null.
     *
     * @param snapshot the page / 페이지
     * @return the document or null / 문서. 없으면 null
     */
    private Document document(PageSnapshot snapshot) {
        if (disposed || session.isDisposed() || session.getSnapshot() != snapshot || !(snapshot.getDocument() instanceof HtmlPageDocument)) {
            return null;
        }
        HtmlPageDocument top = (HtmlPageDocument) snapshot.getDocument();
        FrameSet roots = top.frames();
        FrameContext focused = roots == null ? null : roots.focusedFrame();
        HtmlPageDocument focusedPage = focused == null ? null : focused.page();
        // 프레임 안을 클릭한 뒤에는 키보드 입력을 그 프레임의 문서가 받는다.
        return focusedPage != null && focusedPage.getDocument() != null ? focusedPage.getDocument() : top.getDocument();
    }

    /**
     * Returns the DOM document of the top-level page, whatever frame has the keyboard focus.
     * <p>
     * 어느 프레임이 키보드 포커스를 가졌든 최상위 페이지의 DOM 문서를 반환한다.
     *
     * @param snapshot the page / 페이지
     * @return the document or null / 문서. 없으면 null
     */
    private Document topDocument(PageSnapshot snapshot) {
        if (disposed || session.isDisposed() || session.getSnapshot() != snapshot || !(snapshot.getDocument() instanceof HtmlPageDocument)) {
            return null;
        }
        return ((HtmlPageDocument) snapshot.getDocument()).getDocument();
    }

    /**
     * Returns the element that has the keyboard focus in a document, else the body (the document element if there is no body), or null.
     * <p>
     * 문서에서 키보드 포커스를 가진 요소를 반환한다. 없으면 body(body 가 없으면 문서 요소). 그것도 없으면 null.
     *
     * @param doc the document / 문서
     * @return the element or null / 요소. 없으면 null
     */
    private static Element focusedOrBody(Document doc) {
        Element target = FocusManager.getFocused(doc);
        if (target == null) {
            // 포커스를 가진 요소가 없으면 키 이벤트는 body(없으면 문서 요소)로 간다.
            org.duckdns.hjow.swingwebpanel.dom.ElementCollection bodies = doc.getElementsByTagName("body");
            target = bodies.getLength() > 0 ? bodies.item(0) : doc.getDocumentElement();
        }
        return target;
    }

    /**
     * What to do for the element that has the keyboard focus (see {@link #queueForFocused}).
     * <p>
     * 키보드 포커스를 가진 요소에 대해 할 일({@link #queueForFocused} 참조).
     */
    interface FocusedTask {
        /**
         * Runs on the document queue.
         * <p>
         * 문서 큐에서 실행된다.
         *
         * @param snapshot the page / 페이지
         * @param doc      the document that has the keyboard focus (a frame's, if a frame has it) / 키보드 포커스를 가진 문서(프레임이 가졌으면 그 프레임의 것)
         * @param target   the focused element, else the body / 포커스를 가진 요소. 없으면 body
         */
        void run(PageSnapshot snapshot, Document doc, Element target);
    }

    /**
     * Queues a task on the document queue for the element that has the keyboard focus, if the page is still the current one when it runs
     * (the input method uses this for the text that it composes).
     * <p>
     * 키보드 포커스를 가진 요소에 대한 일을 문서 큐에 예약한다. 실행할 때 페이지가 아직 현재일 때만 실행한다(입력기가 조합하는 텍스트에 쓴다).
     *
     * @param snapshot the page when the input happened / 입력이 일어난 때의 페이지
     * @param task     the task / 할 일
     */
    void queueForFocused(final PageSnapshot snapshot, final FocusedTask task) {
        if (disposed) {
            return;
        }
        session.submitDocumentTask(new Runnable() {
            /** Finds the focused element and runs the task on the document queue. / 문서 큐에서 포커스를 가진 요소를 찾아 일을 실행한다. */
            public void run() {
                Document doc = document(snapshot);
                Element target = doc == null ? null : focusedOrBody(doc);
                if (target != null) {
                    task.run(snapshot, doc, target);
                }
            }
        });
    }

    /**
     * Queues one DOM keyboard event for the focused element.
     * <p>
     * 포커스를 가진 요소에 DOM 키보드 이벤트 하나를 예약한다.
     *
     * @param type      the event type / 이벤트 종류
     * @param key       the key / 키
     * @param defaultOk true if the default action of the key may follow / 키의 기본 동작이 뒤따를 수 있으면 true
     */
    private void send(final String type, final Key key, final boolean defaultOk) {
        if (disposed) {
            return;
        }
        final PageSnapshot snapshot = session.getSnapshot();
        session.submitDocumentTask(new Runnable() {
            /** Dispatches the event and the default action on the document queue. / 문서 큐에서 이벤트와 기본 동작을 전달한다. */
            public void run() {
                Document doc = document(snapshot);
                if (doc == null) {
                    return;
                }
                Element target = focusedOrBody(doc);
                if (target == null) {
                    return;
                }
                // keydown 을 취소했으면 그 키가 만드는 글자(keypress 와 입력)도 없다.
                if ("keypress".equals(type) && typedSuppressed) {
                    return;
                }
                KeyboardEvent event = new KeyboardEvent(type, true, true);
                event.setKey(key.key, key.code, key.location, key.keyCode, key.charCode);
                event.setStates(key.repeat, key.composing);
                event.setModifiers(key.has(KeyEvent.CTRL_DOWN_MASK), key.has(KeyEvent.SHIFT_DOWN_MASK), key.has(KeyEvent.ALT_DOWN_MASK),
                        key.has(KeyEvent.META_DOWN_MASK));
                boolean allowed = PageInput.fire(snapshot, target, event);
                if ("keydown".equals(type)) {
                    typedSuppressed = !allowed;
                } else if ("keyup".equals(type)) {
                    typedSuppressed = false;
                }
                if (allowed && defaultOk && session.getSnapshot() == snapshot) {
                    defaultAction(snapshot, doc, target, type, key);
                }
            }
        });
    }

    /**
     * Does what the browser does for a key that no listener cancelled.
     * <p>
     * 어떤 리스너도 취소하지 않은 키에 브라우저가 하는 일을 한다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param target   the element that the event went to / 이벤트를 받은 요소
     * @param type     <code>keydown</code> or <code>keyup</code> / <code>keydown</code> 또는 <code>keyup</code>
     * @param key      the key / 키
     */
    private void defaultAction(PageSnapshot snapshot, Document doc, Element target, String type, Key key) {
        // 편집 호스트 안의 키(글자 입력, 지우기, 캐럿 이동, 편집 단축키)와 붙여넣기는 편집이 처리한다. 폼 컨트롤은 자기 편집기가 있다.
        if (!isFormControl(target) && !PageFormHost.isTextControl(target)) {
            PageEditing editing = input.getEditing();
            boolean ctrl = key.has(KeyEvent.CTRL_DOWN_MASK);
            boolean shift = key.has(KeyEvent.SHIFT_DOWN_MASK);
            if ("keypress".equals(type)) {
                if (editing.keyPress(snapshot, doc, target, key.key, shift)) {
                    return;
                }
            } else if ("keydown".equals(type)) {
                if (key.clipboard != null) {
                    editing.paste(snapshot, doc, target, key.clipboard);
                    return;
                }
                if (editing.keyDown(snapshot, doc, target, key.key, key.keyCode, ctrl, shift, key.has(KeyEvent.ALT_DOWN_MASK), key.has(KeyEvent.META_DOWN_MASK))) {
                    return;
                }
            }
        }
        boolean plain = !key.has(KeyEvent.CTRL_DOWN_MASK) && !key.has(KeyEvent.ALT_DOWN_MASK) && !key.has(KeyEvent.META_DOWN_MASK);
        if ("keydown".equals(type) && "Tab".equals(key.key) && plain) {
            moveFocus(snapshot, doc, !key.has(KeyEvent.SHIFT_DOWN_MASK));
        } else if ("keydown".equals(type) && "Enter".equals(key.key) && plain && (isLink(target) || isButton(target))) {
            input.clickElement(snapshot, target);
        } else if ("keydown".equals(type) && "Enter".equals(key.key) && plain && PageFormHost.isTextControl(target)) {
            // 텍스트 필드에서 Enter 는 값을 확정하고 폼을 제출한다(암묵적 제출).
            PageFormHost.enterPressed(target);
        } else if ("keyup".equals(type) && " ".equals(key.key) && plain && isToggle(target)) {
            // 스페이스 키는 체크박스와 라디오 단추를 누른다.
            input.clickElement(snapshot, target);
        } else if ("keyup".equals(type) && " ".equals(key.key) && plain && isButton(target)) {
            input.clickElement(snapshot, target);
        } else if ("keydown".equals(type) && key.has(KeyEvent.CTRL_DOWN_MASK) && !key.has(KeyEvent.ALT_DOWN_MASK) && !PageFormHost.isTextControl(target)
                && (key.keyCode == 'A' || key.keyCode == 'C' || key.keyCode == 45)) {
            // Ctrl+A 는 페이지 전체 선택, Ctrl+C 와 Ctrl+Insert 는 선택 복사이다. 텍스트 컨트롤의 편집기는 Swing 이 자기 키로 처리한다.
            if (key.keyCode == 'A') {
                input.getSelection().selectAll(snapshot);
            } else if (key.keyCode == 'C' || !key.has(KeyEvent.SHIFT_DOWN_MASK)) {
                input.getSelection().copy(snapshot);
            }
        } else if ("keydown".equals(type) && key.has(KeyEvent.SHIFT_DOWN_MASK) && !key.has(KeyEvent.ALT_DOWN_MASK) && !key.has(KeyEvent.META_DOWN_MASK)
                && selectionKey(key.key) && !isFormControl(target)) {
            // Shift+화살표, Shift+Home/End 는 선택의 focus 를 옮긴다(Ctrl 이 함께면 낱말 단위). 폼 컨트롤은 자기 키 동작을 쓴다.
            input.getSelection().extendByKey(snapshot, key.key, key.has(KeyEvent.CTRL_DOWN_MASK));
        } else if ("keydown".equals(type) && !key.has(KeyEvent.META_DOWN_MASK)) {
            // select, 슬라이더, 라디오 단추의 화살표와 글자 키는 컨트롤의 기본 동작이다.
            PageFormHost.keyDown(target, key.key, key.has(KeyEvent.ALT_DOWN_MASK), key.has(KeyEvent.CTRL_DOWN_MASK));
        }
    }

    /**
     * Tells whether a key value extends the selection together with Shift (the arrow keys, Home and End).
     * <p>
     * 키 값이 Shift 와 함께 선택을 확장하는지 알려 준다(화살표 키, Home, End).
     *
     * @param key the DOM key value / DOM key 값
     * @return true for such a key / 그런 키이면 true
     */
    private static boolean selectionKey(String key) {
        return "ArrowLeft".equals(key) || "ArrowRight".equals(key) || "ArrowUp".equals(key) || "ArrowDown".equals(key) || "Home".equals(key)
                || "End".equals(key);
    }

    /**
     * Tells whether an element is a form control that has keys of its own (a text field, a list, a button ...).
     * <p>
     * 요소가 자기 키 동작을 가진 폼 컨트롤(텍스트 필드, 목록, 단추 등)인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a form control / 폼 컨트롤이면 true
     */
    private static boolean isFormControl(Element e) {
        String name = e.getLocalName();
        return "input".equals(name) || "textarea".equals(name) || "select".equals(name) || "button".equals(name);
    }

    /**
     * Tells whether an element is an enabled check box or radio button (the space key presses it).
     * <p>
     * 요소가 활성 체크박스나 라디오 단추인지 알려 준다(스페이스 키가 누른다).
     *
     * @param e the element / 요소
     * @return true for a check box or radio button / 체크박스나 라디오 단추이면 true
     */
    private static boolean isToggle(Element e) {
        String type = e.getAttribute("type");
        return "input".equals(e.getLocalName()) && type != null && ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type))
                && !e.hasAttribute("disabled");
    }

    /**
     * Tells whether an element is a link that Enter follows.
     * <p>
     * Enter 가 따라가는 링크인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a link with <code>href</code> / <code>href</code> 가 있는 링크이면 true
     */
    private static boolean isLink(Element e) {
        return "a".equals(e.getLocalName()) && e.hasAttribute("href");
    }

    /**
     * Tells whether an element is a button that Enter and Space press.
     * <p>
     * Enter 와 Space 가 누르는 단추인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for a button / 단추이면 true
     */
    private static boolean isButton(Element e) {
        String type = e.getAttribute("type");
        boolean inputButton = "input".equals(e.getLocalName()) && type != null
                && ("button".equalsIgnoreCase(type) || "submit".equalsIgnoreCase(type) || "reset".equalsIgnoreCase(type));
        return ("button".equals(e.getLocalName()) || inputButton) && !e.hasAttribute("disabled");
    }

    /**
     * Moves the focus along the tab order, and leaves the component after the last (or before the first) element.
     * <p>
     * Tab 순서대로 포커스를 옮기고, 마지막 요소 다음(또는 첫 요소 이전)이면 컴포넌트를 떠난다.
     *
     * @param snapshot the page / 페이지
     * @param doc      the document / 문서
     * @param forward  true for Tab / Tab 이면 true
     */
    private void moveFocus(PageSnapshot snapshot, Document doc, boolean forward) {
        // 프레임 문서의 요소는 그 프레임이 그린 레이아웃으로 표시 여부를 판단한다.
        HtmlPageDocument top = (HtmlPageDocument) snapshot.getDocument();
        FrameContext inFrame = top.frames() == null ? null : top.frames().frameOf(doc);
        final RenderedPage page = inFrame != null ? inFrame.lastRendered() : view.getRenderedPage();
        Element next = FocusManager.next(doc, FocusManager.getFocused(doc), forward, new FocusManager.Visibility() {
            /**
             * An element is displayed if the layout made a box for it.
             * <p>
             * 레이아웃이 상자를 만든 요소이면 표시되는 것이다.
             *
             * @param e the element / 요소
             * @return true if displayed / 표시되면 true
             */
            public boolean isDisplayed(Element e) {
                return page == null || !page.getLayout().getBoxes(e).isEmpty();
            }
        });
        if (next != null) {
            FocusManager.focus(next, doc.isWindowFocused());
            input.focusChanged(snapshot);
            return;
        }
        final boolean fwd = forward;
        SwingUtilities.invokeLater(new Runnable() {
            /** Hands the keyboard focus on to the next component of the host. / 키보드 포커스를 호스트의 다음 컴포넌트로 넘긴다. */
            public void run() {
                if (fwd) {
                    component.transferFocus();
                } else {
                    component.transferFocusBackward();
                }
            }
        });
    }

    /**
     * Returns the nearest ancestor-or-self that can have the focus (the element that a click focuses), or null.
     * <p>
     * 포커스를 가질 수 있는 가장 가까운 자기 자신 또는 조상(클릭이 포커스를 주는 요소)을 반환한다. 없으면 null.
     *
     * @param start the clicked element / 클릭한 요소
     * @return the focusable element or null / 포커스를 가질 수 있는 요소. 없으면 null
     */
    static Element focusableAncestor(Element start) {
        // 클릭한 요소에서 위로 올라가며 포커스를 가질 수 있는 첫 요소를 찾는다.
        for (Node n = start; n instanceof Element; n = n.getParentNode()) {
            if (FocusManager.isFocusable((Element) n)) {
                return (Element) n;
            }
        }
        return null;
    }
}
