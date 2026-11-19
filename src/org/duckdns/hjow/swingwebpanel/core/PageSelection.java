package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardEvent;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Event;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.Selection;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.HitResult;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.render.SelectionHighlight;
import org.duckdns.hjow.swingwebpanel.render.TextSelections;

/**
 * The selection of the text of the page by the user (plan item P11-08): the mouse gestures that select (drag, double click for a word,
 * triple click for a paragraph, a drag that goes on from those, Shift+click), Select All, the copy of the selection, and the highlight that
 * the page shows. The model is the {@link Selection} of the DOM document, the same one that <code>window.getSelection()</code> gives to
 * scripts, so a script sees what the user selected and the highlight shows what a script selected with <code>addRange</code>. All work on
 * the DOM (and the {@link TextSelections} geometry of the rendered page) happens on the document queue; the event dispatch thread only
 * paints the immutable {@link SelectionHighlight} that the document thread published, and writes to the clipboard.
 * <p>
 * Every change of the selection (whoever makes it) fires <code>selectionchange</code> at the document once per burst of changes, as a task
 * of the document queue, and rebuilds the highlight; a new layout of the page rebuilds the highlight as well.
 * <p>
 * 사용자의 페이지 텍스트 선택(계획 P11-08): 선택하는 마우스 동작(드래그, 더블 클릭의 낱말, 트리플 클릭의 문단, 그 뒤에 이어지는 드래그, Shift+클릭), 전체 선택,
 * 선택의 복사, 페이지가 보여 주는 강조. 모델은 DOM 문서의 {@link Selection} 이며 <code>window.getSelection()</code> 이 스크립트에 주는 것과 같다. 그래서
 * 스크립트는 사용자가 선택한 것을 보고, 강조는 스크립트가 <code>addRange</code> 로 선택한 것을 보여 준다. DOM(과 렌더링된 페이지의 {@link TextSelections} 기하)에
 * 대한 모든 일은 문서 큐에서 일어나고, 이벤트 디스패치 스레드는 문서 스레드가 게시한 불변 {@link SelectionHighlight} 를 그리고 클립보드에 쓰기만 한다.
 * <p>
 * 선택이 바뀔 때마다(누가 바꾸든) 문서에 <code>selectionchange</code> 를 변경 묶음마다 한 번 문서 큐의 작업으로 보내고 강조를 다시 만든다. 페이지의 새 레이아웃도
 * 강조를 다시 만든다.
 */
public final class PageSelection {

    /** Selection unit: one character position. 선택 단위: 문자 위치 하나. */
    private static final int CHARACTER = 0;

    /** Selection unit: a word. 선택 단위: 낱말. */
    private static final int WORD = 1;

    /** Selection unit: a paragraph (a block). 선택 단위: 문단(블록). */
    private static final int PARAGRAPH = 2;

    /** The page component, which is repainted. 다시 그릴 페이지 컴포넌트. */
    private final JComponent component;

    /** The session that owns the document queue. 문서 큐를 소유한 세션. */
    private final BrowserSession session;

    /** The view that knows the rendering and the zoom. 렌더링과 배율을 아는 뷰. */
    private final PageView view;

    /** The clipboard, set by the component; null if copying is not wanted. 컴포넌트가 정하는 클립보드. 복사를 원하지 않으면 null. */
    private volatile ClipboardSink clipboard;

    /** The highlight that the page paints; replaced by the document thread. 페이지가 그리는 강조. 문서 스레드가 교체한다. */
    private volatile SelectionHighlight highlight;

    /** The mark of the editing caret that the page paints (a collapsed selection in an editable place); replaced by the document thread. 페이지가 그리는 편집 캐럿 표시(편집 가능한 곳의 접힌 선택). 문서 스레드가 교체한다. */
    private volatile SelectionHighlight caret;

    /** False in the hidden half of the blink period of the caret (written on the EDT). 캐럿 깜박임 주기에서 숨겨진 절반에는 false(EDT 에서 쓴다). */
    private volatile boolean caretOn = true;

    /** True if the caret must be scrolled into view after the next layout (an edit or a caret key moved it). 다음 레이아웃 뒤 캐럿이 보이도록 스크롤해야 하면 true(편집이나 캐럿 키가 옮겼다). */
    private volatile boolean revealCaret;

    /** True while the keyboard focus is in an editing host (see {@link #isEditableFocus()}). 키보드 포커스가 편집 호스트 안에 있는 동안 true({@link #isEditableFocus()} 참조). */
    private volatile boolean editableFocus;

    /** The blink timer of the caret (used on the EDT only). 캐럿의 깜박임 타이머(EDT 에서만 쓴다). */
    private javax.swing.Timer blinkTimer;

    /** What turns the input methods on and off, or null. 입력기를 켜고 끄는 쪽. 없으면 null. */
    private volatile InputMethodSwitch inputMethods;

    /** What else forgets the page when it is closed (the drag and drop), or null. 페이지가 닫힐 때 그 페이지를 함께 잊는 쪽(끌어다 놓기). 없으면 null. */
    private volatile Runnable closedHook;

    /**
     * Sets what runs when the page that is shown is closed (replaced, cleaned or disposed), for the controllers that keep state of the page.
     * <p>
     * 보이던 페이지가 닫힐 때(교체, clean, dispose) 실행할 일을 정한다. 페이지의 상태를 가진 제어기를 위한 것이다.
     *
     * @param hook the task or null / 할 일. 없으면 null
     */
    void setClosedHook(Runnable hook) {
        this.closedHook = hook;
    }

    /** True after {@link #dispose()}. {@link #dispose()} 뒤에는 true. */
    private volatile boolean disposed;

    /** True while one change task is queued (changes are merged). 변경 작업 하나가 대기 중이면 true(변경은 합친다). */
    private final AtomicBoolean changeQueued = new AtomicBoolean();

    /** True while one drag task is queued (drags are merged). 드래그 작업 하나가 대기 중이면 true(드래그는 합친다). */
    private final AtomicBoolean dragQueued = new AtomicBoolean();

    /** The last pointer position of a drag in document coordinates, written on the EDT. 드래그의 마지막 포인터 위치(문서 좌표). EDT 에서 쓴다. */
    private volatile double dragX;

    /** The last pointer y of a drag in document coordinates, written on the EDT. 드래그의 마지막 포인터 y(문서 좌표). EDT 에서 쓴다. */
    private volatile double dragY;

    /** The page of the last drag request. 마지막 드래그 요청의 페이지. */
    private volatile PageSnapshot dragPage;

    /** The geometry of the rendered page, used on the document thread (volatile because {@link #pageClosed()} drops it from another thread). 렌더링된 페이지의 기하. 문서 스레드에서 쓴다({@link #pageClosed()} 가 다른 스레드에서 버리므로 volatile). */
    private volatile TextSelections geometry;

    /** True while the button is down after a press that started a selection (document thread). 선택을 시작한 누름 뒤 버튼이 눌려 있는 동안 true(문서 스레드). */
    private volatile boolean dragging;

    /** The unit that the press selected: {@link #CHARACTER}, {@link #WORD} or {@link #PARAGRAPH} (document thread). 누름이 선택한 단위(문서 스레드). */
    private int unit;

    /** The page that the press belongs to (document thread). 누름이 속한 페이지(문서 스레드). */
    private volatile PageSnapshot pressPage;

    /** Start of the unit that the press selected (document thread; dropped by {@link #pageClosed()}). 누름이 선택한 단위의 시작(문서 스레드. {@link #pageClosed()} 가 버린다). */
    private volatile Node unitStartNode;

    /** The offset in the start node of the unit that the press selected. 누름이 선택한 단위의 시작 노드 안의 오프셋. */
    private int unitStartOffset;

    /** End of the unit that the press selected (document thread; dropped by {@link #pageClosed()}). 누름이 선택한 단위의 끝(문서 스레드. {@link #pageClosed()} 가 버린다). */
    private volatile Node unitEndNode;

    /** The offset in the end node of the unit that the press selected. 누름이 선택한 단위의 끝 노드 안의 오프셋. */
    private int unitEndOffset;

    /**
     * Creates the controller of a page component.
     * <p>
     * 페이지 컴포넌트의 제어기를 만든다.
     *
     * @param component the page component / 페이지 컴포넌트
     * @param session   the session / 세션
     * @param view      the view / 뷰
     */
    public PageSelection(JComponent component, BrowserSession session, PageView view) {
        this.component = component;
        this.session = session;
        this.view = view;
    }

    /**
     * Sets where copied text goes (null: nowhere, copying only fires the <code>copy</code> event).
     * <p>
     * 복사한 텍스트가 갈 곳을 정한다(null 이면 어디로도 가지 않고, 복사는 <code>copy</code> 이벤트만 보낸다).
     *
     * @param sink the clipboard or null / 클립보드. 없으면 null
     */
    public void setClipboard(ClipboardSink sink) {
        this.clipboard = sink;
    }

    /**
     * Stops all work (the component was disposed).
     * <p>
     * 모든 일을 멈춘다(컴포넌트가 폐기되었다).
     */
    public void dispose() {
        disposed = true;
        pageClosed();
        clipboard = null;
        SwingUtilities.invokeLater(new Runnable() {
            /** Stops the blink timer on the EDT. / EDT 에서 깜박임 타이머를 멈춘다. */
            public void run() {
                if (blinkTimer != null) {
                    blinkTimer.stop();
                }
            }
        });
    }

    /**
     * Returns the mark of the editing caret that is drawn over the page, or null (for the event dispatch thread).
     * <p>
     * 페이지 위에 그려지는 편집 캐럿 표시를 반환한다. 없으면 null(이벤트 디스패치 스레드용).
     *
     * @return the mark or null / 표시. 없으면 null
     */
    public SelectionHighlight getCaret() {
        return caret;
    }

    /**
     * Tells whether the caret is in the visible half of its blink period.
     * <p>
     * 캐럿이 깜박임 주기의 보이는 절반에 있는지 알려 준다.
     *
     * @return true if it is to be drawn now / 지금 그려야 하면 true
     */
    public boolean isCaretOn() {
        return caretOn;
    }

    /**
     * Tells whether the keyboard focus of the page is in an editing host, so that the caret keys belong to the caret (they must not scroll
     * the scroll pane). It is updated on the document thread when the selection, the layout or the focus changed.
     * <p>
     * 페이지의 키보드 포커스가 편집 호스트 안에 있어서 캐럿 키가 캐럿의 것인지(스크롤 창을 스크롤하면 안 되는지) 알려 준다. 선택, 레이아웃, 포커스가 바뀔 때 문서
     * 스레드에서 갱신된다.
     *
     * @return true if the caret keys edit / 캐럿 키가 편집이면 true
     */
    public boolean isEditableFocus() {
        return editableFocus;
    }

    /**
     * What turns the input methods of the component on and off (see {@link #setInputMethodSwitch}).
     * <p>
     * 컴포넌트의 입력기를 켜고 끄는 쪽({@link #setInputMethodSwitch} 참조).
     */
    public interface InputMethodSwitch {
        /**
         * Turns the input methods on or off; it is called on the event dispatch thread.
         * <p>
         * 입력기를 켜거나 끈다. 이벤트 디스패치 스레드에서 호출된다.
         *
         * @param on true to turn them on / 켜려면 true
         */
        void setEnabled(boolean on);
    }

    /**
     * Sets what turns the input methods on and off: they are on while an editing host of the page has the keyboard focus, so that the user
     * can compose Korean and other text there, and off otherwise.
     * <p>
     * 입력기를 켜고 끄는 쪽을 정한다: 페이지의 편집 호스트가 키보드 포커스를 가진 동안에는 켜져 사용자가 거기서 한글 등을 조합할 수 있고, 아니면 꺼진다.
     *
     * @param inputMethods the switch or null / 스위치. 없으면 null
     */
    public void setInputMethodSwitch(InputMethodSwitch inputMethods) {
        this.inputMethods = inputMethods;
    }

    /**
     * Turns the input methods on or off on the event dispatch thread.
     * <p>
     * 입력기를 이벤트 디스패치 스레드에서 켜거나 끈다.
     *
     * @param on true to turn them on / 켜려면 true
     */
    private void switchInputMethods(final boolean on) {
        final InputMethodSwitch target = inputMethods;
        if (target == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /** Switches on the EDT unless the component was disposed. / 컴포넌트가 폐기되지 않았으면 EDT 에서 바꾼다. */
            public void run() {
                if (!disposed) {
                    target.setEnabled(on);
                }
            }
        });
    }

    /**
     * Asks to scroll the caret into view after the next layout (edits and caret keys call it).
     * <p>
     * 다음 레이아웃 뒤 캐럿이 보이도록 스크롤해 달라고 요청한다(편집과 캐럿 키가 부른다).
     */
    void requestReveal() {
        revealCaret = true;
    }

    /**
     * Returns the blink period of the caret in milliseconds: the <code>TextField.caretBlinkRate</code> value of the look and feel (500 if
     * it has none), 0 for a caret that does not blink.
     * <p>
     * 캐럿의 깜박임 주기(밀리초)를 반환한다: 룩앤필의 <code>TextField.caretBlinkRate</code> 값(없으면 500). 0 이면 깜박이지 않는다.
     *
     * @return the period / 주기
     */
    private static int blinkRate() {
        Object v = javax.swing.UIManager.get("TextField.caretBlinkRate");
        return v instanceof Integer ? Math.max(0, ((Integer) v).intValue()) : 500;
    }

    /**
     * The listener of the blink timer; it knows the controller only weakly, so the shared timer queue of Swing never keeps a disposed
     * component alive, and it stops its timer when the controller is gone.
     * <p>
     * 깜박임 타이머의 리스너. 제어기를 약하게만 알므로 Swing 의 공유 타이머 큐가 폐기된 컴포넌트를 붙잡지 않으며, 제어기가 사라지면 타이머를 멈춘다.
     */
    private static final class Blinker implements java.awt.event.ActionListener {
        /** The controller, weakly held. 약하게 붙든 제어기. */
        private final java.lang.ref.WeakReference<PageSelection> owner;

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         *
         * @param owner the controller / 제어기
         */
        Blinker(PageSelection owner) {
            this.owner = new java.lang.ref.WeakReference<PageSelection>(owner);
        }

        /**
         * Toggles the caret and repaints it; stops the timer when there is nothing to blink.
         * <p>
         * 캐럿을 토글하고 다시 그린다. 깜박일 것이 없으면 타이머를 멈춘다.
         *
         * @param e the timer event / 타이머 이벤트
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            PageSelection s = owner.get();
            if (s == null || s.disposed || s.caret == null) {
                ((javax.swing.Timer) e.getSource()).stop();
                return;
            }
            s.caretOn = !s.caretOn;
            s.repaintAround(s.caret, null);
        }
    }

    /**
     * Starts or stops the blinking of the caret on the EDT, and shows the caret at once (a caret that moved is visible).
     * <p>
     * EDT 에서 캐럿의 깜박임을 시작하거나 멈추고 캐럿을 바로 보인다(옮겨진 캐럿은 보인다).
     *
     * @param present true if there is a caret / 캐럿이 있으면 true
     */
    private void updateBlink(final boolean present) {
        SwingUtilities.invokeLater(new Runnable() {
            /** Restarts the timer on the EDT. / EDT 에서 타이머를 다시 시작한다. */
            public void run() {
                caretOn = true;
                int rate = blinkRate();
                if (disposed || !present || rate <= 0) {
                    if (blinkTimer != null) {
                        blinkTimer.stop();
                    }
                    return;
                }
                if (blinkTimer == null) {
                    blinkTimer = new javax.swing.Timer(rate, new Blinker(PageSelection.this));
                }
                blinkTimer.setDelay(rate);
                blinkTimer.restart();
            }
        });
    }

    /**
     * Returns the highlight that is drawn over the page, or null (for the event dispatch thread).
     * <p>
     * 페이지 위에 그려지는 강조를 반환한다. 없으면 null(이벤트 디스패치 스레드용).
     *
     * @return the highlight or null / 강조. 없으면 null
     */
    public SelectionHighlight getHighlight() {
        return highlight;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 작은 도우미
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Tells whether a page is still the current one of the component.
     * <p>
     * 페이지가 컴포넌트의 현재 페이지인지 알려 준다.
     *
     * @param snapshot the page / 페이지
     * @return true while current / 현재이면 true
     */
    private boolean current(PageSnapshot snapshot) {
        return !disposed && !session.isDisposed() && snapshot != null && session.getSnapshot() == snapshot;
    }

    /**
     * Returns the document holder of a page, or null if it is not an HTML page.
     * <p>
     * 페이지의 문서 보관 객체를 반환한다. HTML 페이지가 아니면 null.
     *
     * @param snapshot the page / 페이지
     * @return the holder or null / 보관 객체. 없으면 null
     */
    private static HtmlPageDocument holder(PageSnapshot snapshot) {
        return snapshot != null && snapshot.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) snapshot.getDocument() : null;
    }

    /**
     * Returns the geometry of a rendered page (made once per page).
     * <p>
     * 렌더링된 페이지의 기하를 반환한다(페이지마다 한 번 만든다).
     *
     * @param page the rendered page / 렌더링된 페이지
     * @return the geometry / 기하
     */
    private TextSelections geometry(RenderedPage page) {
        if (geometry == null || geometry.getPage() != page) {
            geometry = new TextSelections(page);
        }
        return geometry;
    }

    /**
     * Tells whether an element is a form control (a click on it edits or presses it; it does not start a selection of the page text).
     * <p>
     * 요소가 폼 컨트롤인지 알려 준다(그 위의 클릭은 편집하거나 누르는 것이며 페이지 텍스트의 선택을 시작하지 않는다).
     *
     * @param e the element, may be null / 요소. null 가능
     * @return true for a form control / 폼 컨트롤이면 true
     */
    private static boolean isFormControl(Element e) {
        // 클릭한 요소에서 위로 올라가며 폼 컨트롤이 있는지 본다(단추 안의 텍스트도 단추의 일부이다).
        for (Node n = e; n instanceof Element; n = n.getParentNode()) {
            String name = ((Element) n).getLocalName();
            if ("input".equals(name) || "textarea".equals(name) || "select".equals(name) || "button".equals(name)) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 마우스로 선택
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles the press of the main mouse button on the document queue, after the <code>mousedown</code> event was not cancelled: starts a
     * selection (a caret where the press is, a word for a double click, a paragraph for a triple click), or extends the selection with
     * Shift. A press on a form control clears the selection; a press on text with <code>user-select: none</code> changes nothing.
     * <p>
     * <code>mousedown</code> 이벤트가 취소되지 않은 뒤 문서 큐에서 주 마우스 버튼의 누름을 처리한다: 선택을 시작하거나(누른 곳의 캐럿, 더블 클릭이면 낱말,
     * 트리플 클릭이면 문단) Shift 가 눌려 있으면 선택을 확장한다. 폼 컨트롤을 누르면 선택을 비우고 <code>user-select: none</code> 텍스트를 누르면 아무것도
     * 바꾸지 않는다.
     *
     * @param snapshot   the page / 페이지
     * @param page       the rendering of the page / 페이지의 렌더링
     * @param docX       x in document coordinates / 문서 좌표 x
     * @param docY       y in document coordinates / 문서 좌표 y
     * @param clickCount the click count of the press (1, 2, 3 ...) / 누름의 클릭 횟수
     * @param shift      true if Shift is down / Shift 가 눌려 있으면 true
     */
    public void pressed(PageSnapshot snapshot, RenderedPage page, double docX, double docY, int clickCount, boolean shift) {
        dragging = false;
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        if (!current(snapshot) || page == null || doc == null) {
            return;
        }
        // 정확한 (소수) 좌표로 다시 조회해 글자 경계를 정확히 고른다.
        java.awt.Point scroll = view.viewportPosition();
        HitResult hit = page.getHitTester().hit(docX, docY, scroll.x, scroll.y);
        Selection selection = doc.getSelection();
        // 폼 컨트롤 위의 누름은 선택을 시작하지 않고 페이지의 선택을 비운다(편집은 Swing 편집기가 맡는다).
        if (isFormControl(hit.getElement())) {
            selection.removeAllRanges();
            return;
        }
        TextSelections geo = geometry(page);
        LayoutBox box = hit.getBox();
        boolean overText = box != null && box.getType() == LayoutBox.Type.TEXT && hit.getTextNode() != null;
        // user-select 가 none 인 텍스트 위의 누름은 선택을 바꾸지 않는다. all 이면 그 요소의 내용 전체를 선택한다.
        if (overText) {
            if (!TextSelections.isSelectable(box)) {
                return;
            }
            Element all = TextSelections.allElement(box);
            if (all != null) {
                selection.selectAllChildren(all);
                return;
            }
        }
        TextSelections.Caret caret = geo.caretAt(docX, docY, hit);
        if (caret == null) {
            if (!shift) {
                selection.removeAllRanges();
            }
            return;
        }
        pressPage = snapshot;
        // Shift 를 누른 클릭은 기존 선택의 anchor 를 두고 focus 만 새 위치로 옮긴다.
        if (shift && selection.getRangeCount() > 0) {
            unit = CHARACTER;
            try {
                selection.extend(caret.getNode(), caret.getOffset());
                dragging = true;
            } catch (DomException e) {
                dragging = false;
            }
            return;
        }
        // 선택을 시작하기 전에 취소할 수 있는 selectstart 를 보낸다.
        if (!holder.dispatchInput(caret.getNode(), new Event("selectstart", true, true)) || !current(snapshot)) {
            return;
        }
        try {
            if (clickCount >= 3) {
                Element block = TextSelections.paragraphElement(caret);
                if (block != null) {
                    unit = PARAGRAPH;
                    setUnit(block, 0, block, block.getChildNodes().getLength());
                    selection.setBaseAndExtent(unitStartNode, unitStartOffset, unitEndNode, unitEndOffset);
                    dragging = true;
                    return;
                }
            }
            if (clickCount == 2 && caret.getNode() instanceof CharacterData) {
                int[] word = TextSelections.wordRange(((CharacterData) caret.getNode()).getData(), caret.getOffset());
                unit = WORD;
                setUnit(caret.getNode(), word[0], caret.getNode(), word[1]);
                selection.setBaseAndExtent(unitStartNode, unitStartOffset, unitEndNode, unitEndOffset);
                dragging = true;
                return;
            }
            unit = CHARACTER;
            selection.collapse(caret.getNode(), caret.getOffset());
            dragging = true;
        } catch (DomException e) {
            // 스크립트가 selectstart 에서 DOM 을 바꿔 위치가 사라졌으면 선택하지 않는다.
            dragging = false;
        }
    }

    /**
     * Remembers the unit that a press selected.
     * <p>
     * 누름이 선택한 단위를 기억한다.
     *
     * @param startNode   the start node / 시작 노드
     * @param startOffset the start offset / 시작 오프셋
     * @param endNode     the end node / 끝 노드
     * @param endOffset   the end offset / 끝 오프셋
     */
    private void setUnit(Node startNode, int startOffset, Node endNode, int endOffset) {
        unitStartNode = startNode;
        unitStartOffset = startOffset;
        unitEndNode = endNode;
        unitEndOffset = endOffset;
    }

    /**
     * Asks to extend the selection to a pointer position while the button is down (from the event dispatch thread). Several requests that
     * arrive before the document thread is free are merged into the last one.
     * <p>
     * 버튼이 눌린 채 포인터 위치까지 선택을 확장해 달라고 요청한다(이벤트 디스패치 스레드에서). 문서 스레드가 한가해지기 전에 온 요청은 마지막 것으로 합친다.
     *
     * @param snapshot the page / 페이지
     * @param docX     x in document coordinates / 문서 좌표 x
     * @param docY     y in document coordinates / 문서 좌표 y
     */
    public void dragged(PageSnapshot snapshot, double docX, double docY) {
        dragX = docX;
        dragY = docY;
        dragPage = snapshot;
        if (disposed || !dragQueued.compareAndSet(false, true)) {
            return;
        }
        if (!session.submitDocumentTask(new Runnable() {
            /** Extends the selection to the last position on the document queue. / 문서 큐에서 마지막 위치까지 선택을 확장한다. */
            public void run() {
                dragQueued.set(false);
                extendTo(dragPage, dragX, dragY);
            }
        })) {
            dragQueued.set(false);
        }
    }

    /**
     * Extends the selection that a press started to a position (document queue).
     * <p>
     * 누름이 시작한 선택을 한 위치까지 확장한다(문서 큐).
     *
     * @param snapshot the page / 페이지
     * @param docX     x in document coordinates / 문서 좌표 x
     * @param docY     y in document coordinates / 문서 좌표 y
     */
    private void extendTo(PageSnapshot snapshot, double docX, double docY) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        RenderedPage page = view.getRenderedPage();
        // 누른 페이지가 여전히 현재이고 누름이 선택을 시작한 경우에만 확장한다.
        if (!dragging || snapshot != pressPage || !current(snapshot) || doc == null || page == null) {
            return;
        }
        Selection selection = doc.getSelection();
        if (selection.getRangeCount() == 0) {
            dragging = false;
            return;
        }
        java.awt.Point scroll = view.viewportPosition();
        TextSelections geo = geometry(page);
        TextSelections.Caret caret = geo.caretAt(docX, docY, page.getHitTester().hit(docX, docY, scroll.x, scroll.y));
        if (caret == null) {
            return;
        }
        Node startNode = unitStartNode;
        Node endNode = unitEndNode;
        int startOffset = unitStartOffset;
        int endOffset = unitEndOffset;
        try {
            if (unit == CHARACTER) {
                selection.extend(caret.getNode(), caret.getOffset());
                return;
            }
            // 페이지가 닫혀 단위가 사라졌으면 확장하지 않는다.
            if (startNode == null || endNode == null) {
                dragging = false;
                return;
            }
            Node[] nodes = new Node[2];
            int[] offsets = new int[2];
            unitAround(caret, nodes, offsets);
            // 처음 선택한 단위보다 앞이면 단위의 끝을 anchor 로, 아니면 단위의 시작을 anchor 로 해 현재 위치의 단위까지 확장한다.
            if (Range.comparePointOrder(caret.getNode(), caret.getOffset(), startNode, startOffset) < 0) {
                selection.setBaseAndExtent(endNode, endOffset, nodes[0], offsets[0]);
            } else {
                selection.setBaseAndExtent(startNode, startOffset, nodes[1], offsets[1]);
            }
        } catch (DomException e) {
            dragging = false;
        }
    }

    /**
     * Forgets everything that refers to the page that was shown (the page was replaced, cleaned or disposed): the highlight, the geometry
     * of its layout and the state of a drag. It may be called from any thread; without it the controller would keep the old page alive.
     * <p>
     * 보이던 페이지를 가리키는 모든 것을 잊는다(페이지가 교체, clean, dispose 되었다): 강조, 그 레이아웃의 기하, 드래그의 상태. 어느 스레드에서도 부를 수 있다.
     * 이것이 없으면 제어기가 옛 페이지를 계속 붙잡는다.
     */
    void pageClosed() {
        Runnable hook = closedHook;
        if (hook != null) {
            hook.run();
        }
        SelectionHighlight old = highlight;
        highlight = null;
        SelectionHighlight oldCaret = caret;
        caret = null;
        revealCaret = false;
        // 옛 페이지의 편집 호스트가 사라졌으므로 캐럿 키 소유와 입력기도 끈다.
        if (editableFocus) {
            editableFocus = false;
            switchInputMethods(false);
        }
        geometry = null;
        dragging = false;
        pressPage = null;
        dragPage = null;
        unitStartNode = null;
        unitEndNode = null;
        if (old != null || oldCaret != null) {
            repaintAround(old, oldCaret);
            updateBlink(false);
        }
    }

    /**
     * Returns the start and the end of the unit (word or paragraph) around a caret position.
     * <p>
     * 캐럿 위치 둘레의 단위(낱말 또는 문단)의 시작과 끝을 반환한다.
     *
     * @param caret   the caret position / 캐럿 위치
     * @param nodes   receives the start node and the end node / 시작 노드와 끝 노드를 받는다
     * @param offsets receives the start offset and the end offset / 시작 오프셋과 끝 오프셋을 받는다
     */
    private void unitAround(TextSelections.Caret caret, Node[] nodes, int[] offsets) {
        // 문단 단위이면 블록 요소의 내용 전체, 아니면 낱말이다.
        if (unit == PARAGRAPH) {
            Element block = TextSelections.paragraphElement(caret);
            if (block != null) {
                nodes[0] = block;
                offsets[0] = 0;
                nodes[1] = block;
                offsets[1] = block.getChildNodes().getLength();
                return;
            }
        }
        int[] word = TextSelections.wordRange(caret.getNode() instanceof CharacterData ? ((CharacterData) caret.getNode()).getData() : "",
                caret.getOffset());
        nodes[0] = caret.getNode();
        offsets[0] = word[0];
        nodes[1] = caret.getNode();
        offsets[1] = word[1];
    }

    /**
     * Ends a drag (the main mouse button was released; document queue).
     * <p>
     * 드래그를 끝낸다(주 마우스 버튼을 뗐다. 문서 큐).
     */
    public void released() {
        dragging = false;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 전체 선택과 복사
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Selects all the content of the page (document queue): everything inside the body (the document element if there is no body).
     * <p>
     * 페이지의 모든 내용을 선택한다(문서 큐): body 안의 모든 것(body 가 없으면 문서 요소).
     *
     * @param snapshot the page / 페이지
     * @return true if something was selected / 선택했으면 true
     */
    public boolean selectAll(PageSnapshot snapshot) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        if (!current(snapshot) || doc == null) {
            return false;
        }
        org.duckdns.hjow.swingwebpanel.dom.ElementCollection bodies = doc.getElementsByTagName("body");
        Element root = bodies.getLength() > 0 ? bodies.item(0) : doc.getDocumentElement();
        if (root == null) {
            return false;
        }
        doc.getSelection().selectAllChildren(root);
        return true;
    }

    /**
     * Extends the selection with a key (Shift+arrow keys, Shift+Home, Shift+End; document queue): the focus of the selection moves one
     * character (or one word with Ctrl) to the left or right, one line up or down, or to the start or the end of the line, while the anchor
     * stays. Nothing happens if the page has no selection (a click makes a collapsed one) or if the focus cannot be located.
     * <p>
     * 키로 선택을 확장한다(Shift+화살표, Shift+Home, Shift+End. 문서 큐): 선택의 focus 가 anchor 는 그대로인 채 왼쪽이나 오른쪽으로 글자 하나(Ctrl 이면 낱말 하나),
     * 위나 아래로 한 줄, 줄의 처음이나 끝으로 움직인다. 페이지에 선택이 없거나(클릭하면 접힌 선택이 생긴다) focus 의 위치를 찾을 수 없으면 아무 일도 없다.
     *
     * @param snapshot the page / 페이지
     * @param key      the DOM key value: <code>ArrowLeft</code>, <code>ArrowRight</code>, <code>ArrowUp</code>, <code>ArrowDown</code>,
     *                 <code>Home</code> or <code>End</code> / DOM key 값
     * @param byWord   true to move by words (Ctrl) / 낱말 단위(Ctrl)이면 true
     * @return true if the focus moved / focus 가 움직였으면 true
     */
    public boolean extendByKey(PageSnapshot snapshot, String key, boolean byWord) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        RenderedPage page = view.getRenderedPage();
        if (!current(snapshot) || doc == null || page == null) {
            return false;
        }
        Selection selection = doc.getSelection();
        if (selection.getRangeCount() == 0 || selection.getFocusNode() == null) {
            return false;
        }
        TextSelections.Caret next = nextPosition(geometry(page), selection.getFocusNode(), selection.getFocusOffset(), key, byWord);
        if (next == null) {
            return false;
        }
        try {
            selection.extend(next.getNode(), next.getOffset());
        } catch (DomException e) {
            return false;
        }
        return true;
    }

    /**
     * Returns the position that a caret key leads to from a position, or null if the key is not a caret key or there is no such place.
     * <p>
     * 위치에서 캐럿 키가 이끄는 위치를 반환한다. 캐럿 키가 아니거나 그런 곳이 없으면 null.
     *
     * @param geo    the geometry of the rendered page / 렌더링된 페이지의 기하
     * @param node   the node of the position / 위치의 노드
     * @param offset the offset of the position / 위치의 오프셋
     * @param key    the DOM key value / DOM key 값
     * @param byWord true to move by words (Ctrl) / 낱말 단위(Ctrl)이면 true
     * @return the position or null / 위치. 없으면 null
     */
    private static TextSelections.Caret nextPosition(TextSelections geo, Node node, int offset, String key, boolean byWord) {
        // 키에 따라 이동할 위치를 계산한다.
        if ("ArrowLeft".equals(key) || "ArrowRight".equals(key)) {
            return geo.step(node, offset, "ArrowRight".equals(key), byWord);
        } else if ("ArrowUp".equals(key) || "ArrowDown".equals(key)) {
            return geo.line(node, offset, "ArrowDown".equals(key));
        } else if ("Home".equals(key) || "End".equals(key)) {
            return geo.lineEdge(node, offset, "End".equals(key));
        }
        return null;
    }

    /**
     * Moves the caret with a key (the arrow keys, Home and End without Shift; document queue): a selection with a range collapses to its
     * start (left) or its end (right) for the horizontal keys, as in browsers; otherwise the caret moves one character (or one word), one
     * line or to the edge of the line. The caret is scrolled into view afterwards.
     * <p>
     * 키로 캐럿을 옮긴다(Shift 없는 화살표 키, Home, End. 문서 큐): 범위가 있는 선택은 가로 키에서 브라우저처럼 시작(왼쪽)이나 끝(오른쪽)으로 접히고, 아니면
     * 캐럿이 글자 하나(또는 낱말 하나), 한 줄, 줄의 가장자리로 움직인다. 그 뒤 캐럿이 보이도록 스크롤한다.
     *
     * @param snapshot the page / 페이지
     * @param key      the DOM key value / DOM key 값
     * @param byWord   true to move by words (Ctrl) / 낱말 단위(Ctrl)이면 true
     * @return true if the selection changed / 선택이 바뀌었으면 true
     */
    public boolean moveByKey(PageSnapshot snapshot, String key, boolean byWord) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        RenderedPage page = view.getRenderedPage();
        if (!current(snapshot) || doc == null || page == null) {
            return false;
        }
        Selection selection = doc.getSelection();
        if (selection.getRangeCount() == 0 || selection.getFocusNode() == null) {
            return false;
        }
        // 범위가 있는 선택에서 왼쪽/오른쪽 키는 선택의 시작/끝으로 접는다(낱말 단위가 아닐 때).
        if (!selection.isCollapsed() && !byWord && ("ArrowLeft".equals(key) || "ArrowRight".equals(key))) {
            if ("ArrowLeft".equals(key)) {
                selection.collapseToStart();
            } else {
                selection.collapseToEnd();
            }
            revealCaret = true;
            return true;
        }
        TextSelections.Caret next = nextPosition(geometry(page), selection.getFocusNode(), selection.getFocusOffset(), key, byWord);
        if (next == null) {
            return false;
        }
        try {
            selection.collapse(next.getNode(), next.getOffset());
        } catch (DomException e) {
            return false;
        }
        revealCaret = true;
        return true;
    }

    /**
     * Empties the selection of the page (document queue).
     * <p>
     * 페이지의 선택을 비운다(문서 큐).
     *
     * @param snapshot the page / 페이지
     */
    public void clear(PageSnapshot snapshot) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        if (current(snapshot) && doc != null) {
            doc.getSelection().removeAllRanges();
        }
    }

    /**
     * Returns the text of the selection as the user would copy it (document queue): the rules of {@link TextSelections}; the text of
     * {@link Range#toString()} if the page has no rendering yet.
     * <p>
     * 사용자가 복사할 때의 선택 텍스트를 반환한다(문서 큐): {@link TextSelections} 의 규칙. 페이지에 렌더링이 아직 없으면 {@link Range#toString()} 의 텍스트.
     *
     * @param snapshot the page / 페이지
     * @return the text, empty if nothing is selected / 텍스트. 선택이 없으면 빈 문자열
     */
    public String selectedText(PageSnapshot snapshot) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        if (!current(snapshot) || doc == null) {
            return "";
        }
        Selection selection = doc.getSelection();
        if (selection.getRangeCount() == 0 || selection.isCollapsed()) {
            return "";
        }
        RenderedPage page = view.getRenderedPage();
        return page == null ? selection.toString() : geometry(page).text(selection.getRangeAt(0));
    }

    /**
     * Returns the markup of the selection (the serialized content of its range), or an empty text if nothing is selected (document queue).
     * <p>
     * 선택의 마크업(범위 내용의 직렬화)을 반환한다. 선택이 없으면 빈 문자열(문서 큐).
     *
     * @param snapshot the page / 페이지
     * @return the markup / 마크업
     */
    public String selectedMarkup(PageSnapshot snapshot) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        if (!current(snapshot) || doc == null) {
            return "";
        }
        Selection selection = doc.getSelection();
        if (selection.getRangeCount() == 0 || selection.isCollapsed()) {
            return "";
        }
        return markup(doc, selection.getRangeAt(0));
    }

    /**
     * Tells whether a point of the page is inside the highlight of the selection, as when a press can start dragging the selected text
     * (document queue).
     * <p>
     * 페이지의 한 점이 선택의 강조 안에 있는지 알려 준다. 누름이 선택한 텍스트를 끌기 시작할 수 있는지 볼 때 쓴다(문서 큐).
     *
     * @param snapshot the page / 페이지
     * @param docX     x in document coordinates / 문서 좌표 x
     * @param docY     y in document coordinates / 문서 좌표 y
     * @return true if the selection is drawn there / 선택이 거기 그려져 있으면 true
     */
    public boolean isInsideSelection(PageSnapshot snapshot, double docX, double docY) {
        SelectionHighlight h = highlight;
        return current(snapshot) && h != null && h.getPage() == view.getRenderedPage() && !h.isEmpty() && h.getShape().contains(docX, docY);
    }

    /**
     * Returns the caret position (a text node and an offset) that a point of the page stands for, as a drop there puts its text (document
     * queue), or null if the page has no selectable text there.
     * <p>
     * 페이지의 한 점이 가리키는 캐럿 위치(텍스트 노드와 오프셋)를 반환한다. 거기에 놓은 텍스트가 들어가는 자리이다(문서 큐). 거기에 선택할 수 있는 텍스트가 없으면 null.
     *
     * @param snapshot the page / 페이지
     * @param docX     x in document coordinates / 문서 좌표 x
     * @param docY     y in document coordinates / 문서 좌표 y
     * @return the position or null / 위치. 없으면 null
     */
    public TextSelections.Caret caretAtPoint(PageSnapshot snapshot, double docX, double docY) {
        RenderedPage page = view.getRenderedPage();
        if (!current(snapshot) || page == null) {
            return null;
        }
        java.awt.Point scroll = view.viewportPosition();
        return geometry(page).caretAt(docX, docY, page.getHitTester().hit(docX, docY, scroll.x, scroll.y));
    }

    /**
     * Copies the selection (document queue): fires the cancelable <code>copy</code> event at the focused element (the body if none has the
     * focus) with the selected text (<code>text/plain</code>) and markup (<code>text/html</code>) in its <code>clipboardData</code>; if no
     * listener cancelled it, the selection goes to the clipboard, otherwise what the listener put into <code>clipboardData</code> does.
     * Nothing is written when there is nothing to copy.
     * <p>
     * 선택을 복사한다(문서 큐): 취소할 수 있는 <code>copy</code> 이벤트를 포커스를 가진 요소(없으면 body)에 선택한 텍스트(<code>text/plain</code>)와
     * 마크업(<code>text/html</code>)을 <code>clipboardData</code> 에 담아 보낸다. 리스너가 취소하지 않았으면 선택이, 취소했으면 리스너가
     * <code>clipboardData</code> 에 넣은 것이 클립보드로 간다. 복사할 것이 없으면 아무것도 쓰지 않는다.
     *
     * @param snapshot the page / 페이지
     * @return true if data was written (handed to the clipboard sink) / 데이터를 썼으면(클립보드 쪽에 넘겼으면) true
     */
    public boolean copy(PageSnapshot snapshot) {
        return transfer(snapshot, "copy", null);
    }

    /**
     * Cuts the selection of an editing host (document queue): like {@link #copy}, but the event is <code>cut</code>, and the selected
     * content is removed after it was written to the clipboard if no listener cancelled the event.
     * <p>
     * 편집 호스트의 선택을 잘라낸다(문서 큐): {@link #copy} 와 같으나 이벤트가 <code>cut</code> 이고, 리스너가 이벤트를 취소하지 않았으면 클립보드에 쓴 뒤 선택한
     * 내용을 지운다.
     *
     * @param snapshot the page / 페이지
     * @param remove   removes the selected content (run on the document queue) / 선택한 내용을 지우는 작업(문서 큐에서 실행)
     * @return true if data was written / 데이터를 썼으면 true
     */
    public boolean cut(PageSnapshot snapshot, Runnable remove) {
        return transfer(snapshot, "cut", remove);
    }

    /**
     * Copies or cuts the selection: fires the clipboard event, writes the clipboard, and removes the content for a cut.
     * <p>
     * 선택을 복사하거나 잘라낸다: 클립보드 이벤트를 보내고 클립보드에 쓰며 잘라내기이면 내용을 지운다.
     *
     * @param snapshot the page / 페이지
     * @param type     <code>copy</code> or <code>cut</code> / <code>copy</code> 또는 <code>cut</code>
     * @param remove   the removal of a cut, or null / 잘라내기의 삭제 작업. 없으면 null
     * @return true if data was written / 데이터를 썼으면 true
     */
    private boolean transfer(PageSnapshot snapshot, String type, Runnable remove) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        if (!current(snapshot) || doc == null) {
            return false;
        }
        Selection selection = doc.getSelection();
        boolean has = selection.getRangeCount() > 0 && !selection.isCollapsed();
        String plain = has ? selectedText(snapshot) : "";
        String html = has ? markup(doc, selection.getRangeAt(0)) : "";
        ClipboardData data = new ClipboardData();
        if (has) {
            data.put("text/plain", plain);
            if (html.length() > 0) {
                data.put("text/html", html);
            }
        }
        Element target = FocusManager.getFocused(doc);
        // 포커스를 가진 요소가 없으면 body(없으면 문서 요소)가 copy 이벤트를 받는다.
        if (target == null) {
            org.duckdns.hjow.swingwebpanel.dom.ElementCollection bodies = doc.getElementsByTagName("body");
            target = bodies.getLength() > 0 ? bodies.item(0) : doc.getDocumentElement();
        }
        if (target == null) {
            return false;
        }
        boolean notCancelled = holder.dispatchInput(target, new ClipboardEvent(type, true, true, data));
        if (!current(snapshot)) {
            return false;
        }
        String outPlain;
        String outHtml;
        // 리스너가 취소했으면 리스너가 넣은 데이터를 쓰고(없으면 클립보드를 건드리지 않는다), 아니면 선택을 쓴다.
        if (!notCancelled) {
            // 취소했는데 스크립트가 데이터를 정하지 않았거나 지웠으면 클립보드에 쓸 것이 없다(선택을 대신 쓰지 않는다).
            if (!data.isChanged() || (!data.hasData("text/plain") && !data.hasData("text/html"))) {
                return false;
            }
            outPlain = data.getData("text/plain");
            outHtml = data.hasData("text/html") ? data.getData("text/html") : null;
        } else {
            if (!has) {
                return false;
            }
            outPlain = plain;
            outHtml = html.length() > 0 ? html : null;
        }
        write(outPlain, outHtml);
        // 잘라내기이고 리스너가 취소하지 않았으면 선택한 내용을 지운다.
        if (remove != null && notCancelled) {
            remove.run();
        }
        return true;
    }

    /**
     * Serializes the content of a range as markup (empty if the document cannot serialize).
     * <p>
     * 범위의 내용을 마크업으로 직렬화한다(문서가 직렬화할 수 없으면 빈 문자열).
     *
     * @param doc   the document / 문서
     * @param range the range / 범위
     * @return the markup / 마크업
     */
    private static String markup(Document doc, Range range) {
        try {
            DocumentFragment fragment = range.cloneContents();
            Element holder = doc.createElement("div");
            holder.appendChild(fragment);
            return holder.getInnerHtml();
        } catch (RuntimeException e) {
            // 직렬화를 지원하지 않는 문서이면 html 형식은 없이 텍스트만 복사한다.
            return "";
        }
    }

    /**
     * Hands the data to the clipboard on the event dispatch thread; a failure becomes a status message.
     * <p>
     * 이벤트 디스패치 스레드에서 클립보드에 데이터를 넘긴다. 실패는 상태 메시지가 된다.
     *
     * @param plain the text / 텍스트
     * @param html  the markup or null / 마크업. 없으면 null
     */
    private void write(final String plain, final String html) {
        final ClipboardSink sink = clipboard;
        if (sink == null || disposed) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /** Writes to the clipboard, or shows why not. / 클립보드에 쓰거나 못 쓴 이유를 보인다. */
            public void run() {
                try {
                    sink.write(plain, html);
                } catch (Exception e) {
                    // 헤드리스 환경이나 접근 거부 등으로 쓸 수 없으면 조용히 두고 상태 메시지만 보인다.
                    session.setStatusMessage("Copy failed: the clipboard cannot be used");
                } catch (java.awt.AWTError e) {
                    session.setStatusMessage("Copy failed: the clipboard cannot be used");
                }
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 선택 변경과 강조
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Called when the selection of a document changed (on the thread that changed it). If the document belongs to the page that is shown, a
     * task is queued that rebuilds the highlight and fires <code>selectionchange</code>; changes that happen before it runs make one task.
     * <p>
     * 문서의 선택이 바뀌었을 때(바꾼 스레드에서) 호출된다. 그 문서가 보이는 페이지의 것이면 강조를 다시 만들고 <code>selectionchange</code> 를 보내는 작업을
     * 큐에 넣는다. 그 작업이 실행되기 전의 변경은 작업 하나가 된다.
     *
     * @param document the document whose selection changed / 선택이 바뀐 문서
     */
    void documentSelectionChanged(Document document) {
        final PageSnapshot snapshot = session.getSnapshot();
        HtmlPageDocument holder = holder(snapshot);
        // 보이는 페이지의 문서가 아니면(파싱 중이거나 이미 교체된 페이지) 무시한다. 페이지가 보이면 새 레이아웃에서 강조를 만든다.
        if (disposed || holder == null || holder.getDocument() != document || !changeQueued.compareAndSet(false, true)) {
            return;
        }
        if (!session.submitDocumentTask(new Runnable() {
            /** Rebuilds the highlight and fires the event. / 강조를 다시 만들고 이벤트를 보낸다. */
            public void run() {
                changeQueued.set(false);
                if (!current(snapshot)) {
                    return;
                }
                rebuild(snapshot);
                HtmlPageDocument h = holder(snapshot);
                Document d = h == null ? null : h.getDocument();
                if (d != null) {
                    h.dispatchInput(d, new Event("selectionchange", false, false));
                }
            }
        })) {
            changeQueued.set(false);
        }
    }

    /**
     * Called on the document thread after the page got a new rendering: the highlight is computed again for the new layout.
     * <p>
     * 페이지가 새 렌더링을 얻은 뒤 문서 스레드에서 호출된다: 새 레이아웃으로 강조를 다시 계산한다.
     *
     * @param snapshot the page / 페이지
     */
    void layoutChanged(PageSnapshot snapshot) {
        if (current(snapshot)) {
            rebuild(snapshot);
        }
    }

    /**
     * Computes the highlight of the selection for the current rendering, replaces the published one and repaints the places that changed.
     * <p>
     * 현재 렌더링에 대한 선택의 강조를 계산하고 게시된 것을 교체하며 바뀐 곳을 다시 그린다.
     *
     * @param snapshot the page / 페이지
     */
    private void rebuild(PageSnapshot snapshot) {
        HtmlPageDocument holder = holder(snapshot);
        Document doc = holder == null ? null : holder.getDocument();
        RenderedPage page = view.getRenderedPage();
        SelectionHighlight next = null;
        // 렌더링과 선택 범위가 모두 있을 때만 강조를 만든다.
        if (doc != null && page != null) {
            Selection selection = doc.getSelection();
            if (selection.getRangeCount() > 0 && !selection.isCollapsed()) {
                next = geometry(page).highlight(selection.getRangeAt(0));
            }
        }
        SelectionHighlight old = highlight;
        highlight = next;
        if (old != null || next != null) {
            repaintAround(old, next);
        }
        rebuildCaret(doc, page);
    }

    /**
     * Computes the mark of the editing caret (a collapsed selection inside an editable place of a document that has the keyboard focus),
     * replaces the published one, repaints, restarts the blinking, and scrolls the caret into view if an edit asked for it.
     * <p>
     * 편집 캐럿 표시를 계산한다(키보드 포커스를 가진 문서의 편집 가능한 곳 안에 있는 접힌 선택). 게시된 것을 교체하고 다시 그리며 깜박임을 다시 시작하고, 편집이
     * 요청했으면 캐럿이 보이도록 스크롤한다.
     *
     * @param doc  the document or null / 문서. 없으면 null
     * @param page the rendering or null / 렌더링. 없으면 null
     */
    private void rebuildCaret(Document doc, RenderedPage page) {
        SelectionHighlight next = null;
        // 키보드 포커스가 편집 호스트 안(폼 컨트롤이 아닌 곳)에 있는지 기록한다. 포커스를 가진 요소가 없으면 body 이다.
        boolean editable = false;
        if (doc != null && doc.isWindowFocused()) {
            Element focused = org.duckdns.hjow.swingwebpanel.dom.FocusManager.getFocused(doc);
            org.duckdns.hjow.swingwebpanel.dom.ElementCollection bodies = doc.getElementsByTagName("body");
            Element basis = focused != null ? focused : bodies.getLength() > 0 ? bodies.item(0) : doc.getDocumentElement();
            editable = basis != null && !isFormControl(basis) && org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts.hostOf(basis) != null;
        }
        // 편집 호스트가 포커스를 가졌는지가 바뀌면 입력기를 켜거나 끈다(편집할 곳이 없으면 입력기가 필요 없다).
        if (editable != editableFocus) {
            editableFocus = editable;
            switchInputMethods(editable);
        }
        // 접힌 선택이 편집 가능한 곳에 있고 문서가 키보드 포커스를 가졌을 때만 캐럿을 보인다.
        if (doc != null && page != null && doc.isWindowFocused()) {
            Selection selection = doc.getSelection();
            if (selection.getRangeCount() > 0 && selection.isCollapsed() && selection.getFocusNode() != null
                    && org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts.isEditable(selection.getFocusNode())) {
                next = geometry(page).caretMark(selection.getFocusNode(), selection.getFocusOffset());
            }
        }
        SelectionHighlight old = caret;
        caret = next;
        boolean moved = old == null ? next != null : next == null || !old.getBounds().equals(next.getBounds());
        if (moved) {
            repaintAround(old, next);
            updateBlink(next != null);
        }
        // 편집이나 캐럿 키가 옮긴 캐럿이 보이는 영역 밖이면 스크롤한다.
        if (revealCaret) {
            revealCaret = false;
            if (next != null) {
                final double zoom = view.getZoom();
                final Rectangle area = new Rectangle((int) Math.floor(next.getBounds().getX() * zoom) - 2,
                        (int) Math.floor(next.getBounds().getY() * zoom) - 2, (int) Math.ceil(next.getBounds().getWidth() * zoom) + 5,
                        (int) Math.ceil(next.getBounds().getHeight() * zoom) + 5);
                SwingUtilities.invokeLater(new Runnable() {
                    /** Scrolls the caret into view on the EDT. / EDT 에서 캐럿이 보이게 스크롤한다. */
                    public void run() {
                        if (!disposed) {
                            component.scrollRectToVisible(area);
                        }
                    }
                });
            }
        }
    }

    /**
     * Called on the document thread when the keyboard focus of the page changed: the caret is shown for a focused page and hidden for a
     * page that lost the focus.
     * <p>
     * 페이지의 키보드 포커스가 바뀌었을 때 문서 스레드에서 호출된다: 포커스를 가진 페이지에는 캐럿을 보이고 포커스를 잃은 페이지에서는 숨긴다.
     *
     * @param snapshot the page / 페이지
     */
    void focusChanged(PageSnapshot snapshot) {
        if (current(snapshot)) {
            HtmlPageDocument holder = holder(snapshot);
            rebuildCaret(holder == null ? null : holder.getDocument(), view.getRenderedPage());
        }
    }

    /**
     * Repaints the area that two highlights cover (on the event dispatch thread).
     * <p>
     * 두 강조가 덮는 영역을 다시 그린다(이벤트 디스패치 스레드에서).
     *
     * @param a one highlight or null / 강조 하나. 없으면 null
     * @param b the other highlight or null / 다른 강조. 없으면 null
     */
    private void repaintAround(SelectionHighlight a, SelectionHighlight b) {
        Rectangle2D union = null;
        // 두 강조의 경계를 합친다.
        if (a != null) {
            union = a.getBounds();
        }
        if (b != null) {
            union = union == null ? b.getBounds() : union.createUnion(b.getBounds());
        }
        final double zoom = view.getZoom();
        final Rectangle area = union == null ? null : new Rectangle((int) Math.floor(union.getX() * zoom) - 2,
                (int) Math.floor(union.getY() * zoom) - 2, (int) Math.ceil(union.getWidth() * zoom) + 5, (int) Math.ceil(union.getHeight() * zoom) + 5);
        if (disposed) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {
            /** Repaints the changed area. / 바뀐 영역을 다시 그린다. */
            public void run() {
                if (area == null) {
                    component.repaint();
                } else {
                    component.repaint(area);
                }
            }
        });
    }
}
