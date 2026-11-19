package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.duckdns.hjow.swingwebpanel.dom.ClipboardData;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DragEvent;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.Range;
import org.duckdns.hjow.swingwebpanel.dom.edit.EditingHosts;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.render.TextSelections;

/**
 * HTML drag and drop of the page (plan item P20-06, HTML Standard 6.11): the user presses the main mouse button on a draggable element
 * (an element with <code>draggable="true"</code>, an image or a link) or on the selected text, moves the pointer, and releases it over
 * another place of the same page. The drag begins when the pointer has moved a few pixels from the press; the component then fires
 * <code>dragstart</code> at the source, and on every pointer move (and every 350 milliseconds while it stands still) <code>drag</code> at the
 * source, <code>dragenter</code>, <code>dragover</code> and <code>dragleave</code> at the elements under the pointer, following the processing
 * model of the specification (a cancelled <code>dragenter</code> or <code>dragover</code> accepts the element, the <code>dropEffect</code> and the
 * <code>effectAllowed</code> choose the drag operation, a text field or an editing host accepts text without any script). At the release it fires
 * <code>drop</code> at the current target (a text control or an editing host gets the dragged text as its default action, and a drag that
 * moves a selection out of an editing host deletes it), then <code>dragend</code> at the source. Escape cancels the drag. The mouse and key
 * events are suppressed while the drag runs. Every <code>DragEvent</code> gets its own <code>DataTransfer</code> on top of the drag data store, in the
 * mode that the specification gives that event, and it is disabled when the event is over.
 * <p>
 * Limits: the drag is simulated inside the component (it does not start a drag of the operating system, so text cannot be dragged to
 * another application, and nothing is dropped from outside), files are not dragged, the picture of <code>setDragImage</code> is not drawn (the
 * mouse cursor shows the operation), and the events go to the document under the pointer (frames included) without a drag across two
 * frames being treated specially.
 * <p>
 * 페이지의 HTML 끌어다 놓기(계획 P20-06, HTML 표준 6.11): 사용자가 끌 수 있는 요소(<code>draggable="true"</code> 요소, 이미지, 링크)나 선택한 텍스트 위에서 주 마우스 버튼을
 * 누르고, 포인터를 옮기고, 같은 페이지의 다른 곳에서 뗀다. 포인터가 누른 곳에서 몇 픽셀 움직이면 끌기가 시작하고, 컴포넌트는 원본에 <code>dragstart</code> 를 보낸 뒤
 * 포인터가 움직일 때마다(서 있는 동안은 350 밀리초마다) 원본에 <code>drag</code>, 포인터 아래 요소에 <code>dragenter</code>, <code>dragover</code>, <code>dragleave</code> 를
 * 규격의 처리 모델대로 보낸다(<code>dragenter</code> 나 <code>dragover</code> 를 취소하면 그 요소가 받아들이는 것이고, <code>dropEffect</code> 와 <code>effectAllowed</code> 가 끌기 동작을
 * 정하며, 텍스트 필드와 편집 호스트는 스크립트 없이도 텍스트를 받는다). 뗄 때 현재 대상에 <code>drop</code> 을 보내고(텍스트 컨트롤이나 편집 호스트는 기본 동작으로 끌어온
 * 텍스트를 받고, 편집 호스트에서 선택을 옮기는 끌기는 그것을 지운다) 원본에 <code>dragend</code> 를 보낸다. Esc 는 끌기를 취소한다. 끌기가 도는 동안 마우스와 키
 * 이벤트는 막는다. 모든 <code>DragEvent</code> 는 드래그 데이터 저장소 위에 자기 <code>DataTransfer</code> 를 가지며 그 이벤트에 규격이 정한 모드이고 이벤트가 끝나면
 * 무효화된다.
 * <p>
 * 한계: 끌기는 컴포넌트 안에서 흉내 낸다(운영체제의 끌기를 시작하지 않으므로 다른 응용 프로그램으로 텍스트를 끌 수 없고 바깥에서 놓는 것도 없다). 파일은 끌지 않는다.
 * <code>setDragImage</code> 의 그림은 그리지 않는다(마우스 커서가 동작을 보인다). 이벤트는 포인터 아래 문서(프레임 포함)로 가며 두 프레임에 걸친 끌기를 따로 다루지 않는다.
 */
public final class PageDragDrop {

    /**
     * How far the pointer must move from the press, in CSS pixels, before a drag begins.
     * <p>
     * 끌기가 시작하려면 포인터가 누른 곳에서 움직여야 하는 거리(CSS 픽셀).
     */
    static final int THRESHOLD = 4;

    /**
     * The period of the drag iterations while the pointer stands still, in milliseconds (the specification says 350 plus or minus 200).
     * <p>
     * 포인터가 서 있는 동안 끌기 반복의 주기(밀리초. 규격은 350 ± 200 이라 한다).
     */
    static final int REPEAT_MS = 350;

    /**
     * What a press may turn into: a drag of the selected text or of an element.
     * <p>
     * 누름이 될 수 있는 것: 선택한 텍스트나 요소의 끌기.
     */
    private static final class Candidate {
        /** The page. 페이지. */
        final PageSnapshot snapshot;
        /** The element to drag, or null for a drag of the selection. 끌 요소. 선택의 끌기이면 null. */
        final Element element;
        /** The text node under the press (for the selection drag), or null. 누른 곳의 텍스트 노드(선택의 끌기용). 없으면 null. */
        final Node textNode;
        /** The pointer of the press. 누른 포인터. */
        final PageInput.Pointer press;
        /** The link address of the pressed element, if the press was on a link. 링크 위의 누름이면 그 주소. */
        final String href;

        /**
         * Creates a candidate.
         * <p>
         * 후보를 만든다.
         *
         * @param snapshot the page / 페이지
         * @param element  the element to drag or null / 끌 요소. 없으면 null
         * @param textNode the text node under the press or null / 누른 곳의 텍스트 노드. 없으면 null
         * @param press    the pointer of the press / 누른 포인터
         * @param href     the address of the link under the press or null / 누른 곳 링크의 주소. 없으면 null
         */
        Candidate(PageSnapshot snapshot, Element element, Node textNode, PageInput.Pointer press, String href) {
            this.snapshot = snapshot;
            this.element = element;
            this.textNode = textNode;
            this.press = press;
            this.href = href;
        }
    }

    /**
     * A running drag and drop operation (used on the document queue only).
     * <p>
     * 진행 중인 끌어다 놓기 작업(문서 큐에서만 쓴다).
     */
    private static final class Drag {
        /** The page. 페이지. */
        final PageSnapshot snapshot;
        /** The source node: the element that is dragged, or the parent of the text node where a selection drag began. 원본 노드: 끄는 요소, 또는 선택 끌기가 시작한 텍스트 노드의 부모. */
        final Element source;
        /** The selection that is dragged (a live copy of its range), or null for the drag of an element. 끌고 있는 선택(범위의 살아 있는 복사본). 요소의 끌기이면 null. */
        final Range sourceRange;
        /** True if the dragged selection lies inside one editing host. 끄는 선택이 한 편집 호스트 안에 있으면 true. */
        final boolean sourceEditable;
        /** The drag data store: the items and the allowed effects. 드래그 데이터 저장소: 항목과 허용된 효과. */
        ClipboardData store;
        /** The element that gets the drop if it happens now, or null. 지금 놓으면 drop 을 받을 요소. 없으면 null. */
        Element target;
        /** The element that was under the pointer in the last iteration. 마지막 반복에서 포인터 아래 있던 요소. */
        Element immediate;
        /** True once the first iteration ran (the first one always looks at the element under the pointer). 첫 반복이 이미 돌았으면 true. */
        boolean started;
        /** The current drag operation: none, copy, link or move. 현재 끌기 동작: none, copy, link, move. */
        String operation = "none";
        /** The last pointer. 마지막 포인터. */
        PageInput.Pointer pointer;
        /** What the last pointer hit. 마지막 포인터가 맞은 것. */
        PageInput.Located location;

        /**
         * Creates the state of a drag.
         * <p>
         * 끌기의 상태를 만든다.
         *
         * @param snapshot       the page / 페이지
         * @param source         the source node / 원본 노드
         * @param sourceRange    the selection that is dragged or null / 끄는 선택. 없으면 null
         * @param sourceEditable true if the selection is inside an editing host / 선택이 편집 호스트 안이면 true
         * @param store          the drag data store / 드래그 데이터 저장소
         */
        Drag(PageSnapshot snapshot, Element source, Range sourceRange, boolean sourceEditable, ClipboardData store) {
            this.snapshot = snapshot;
            this.source = source;
            this.sourceRange = sourceRange;
            this.sourceEditable = sourceEditable;
            this.store = store;
        }
    }

    /**
     * The listener of the repeat timer; it knows the controller only weakly, so the shared timer queue of Swing never keeps a disposed
     * component alive, and it stops its timer when the controller is gone.
     * <p>
     * 반복 타이머의 리스너. 제어기를 약하게만 알므로 Swing 의 공유 타이머 큐가 폐기된 컴포넌트를 붙잡지 않으며, 제어기가 사라지면 타이머를 멈춘다.
     */
    private static final class Ticker implements ActionListener {
        /** The controller, weakly held. 약하게 붙든 제어기. */
        private final WeakReference<PageDragDrop> owner;

        /**
         * Creates the listener.
         * <p>
         * 리스너를 만든다.
         *
         * @param owner the controller / 제어기
         */
        Ticker(PageDragDrop owner) {
            this.owner = new WeakReference<PageDragDrop>(owner);
        }

        /**
         * Asks for one more iteration of the drag, or stops the timer when the drag is over.
         * <p>
         * 끌기를 한 번 더 반복해 달라고 요청한다. 끌기가 끝났으면 타이머를 멈춘다.
         *
         * @param e the timer event / 타이머 이벤트
         */
        public void actionPerformed(ActionEvent e) {
            PageDragDrop d = owner.get();
            if (d == null || d.disposed || !d.active) {
                ((Timer) e.getSource()).stop();
                return;
            }
            d.queueStep();
        }
    }

    /** The component. 컴포넌트. */
    private final JComponent component;
    /** The session that owns the document queue. 문서 큐를 소유한 세션. */
    private final BrowserSession session;
    /** The view that knows the zoom and the rendering of the page. 확대 배율과 페이지의 렌더링을 아는 뷰. */
    private final PageView view;
    /** The pointer input that locates the elements and makes mouse members. 요소를 찾고 마우스 멤버를 만드는 포인터 입력. */
    private final PageInput input;
    /** The selection controller. 선택 제어기. */
    private final PageSelection selection;
    /** The editing controller that inserts dropped text. 놓은 텍스트를 넣는 편집 제어기. */
    private final PageEditing editing;

    /** True from a press that may become a drag until it is clear that it will not. 끌기가 될 수 있는 누름부터 아니라고 정해질 때까지 true. */
    private volatile boolean armed;
    /** True while a drag runs (from the first <code>dragstart</code> that was not cancelled to the end). 끌기가 도는 동안 true(취소되지 않은 dragstart 부터 끝까지). */
    private volatile boolean active;
    /** True after {@link #dispose()}. {@link #dispose()} 뒤에는 true. */
    private volatile boolean disposed;
    /** The last pointer that the EDT saw during the drag. 끌기 중 EDT 가 본 마지막 포인터. */
    private volatile PageInput.Pointer lastPointer;
    /** True while an iteration is queued on the document queue. 반복이 문서 큐에 예약되어 있으면 true. */
    private final AtomicBoolean stepQueued = new AtomicBoolean();
    /** The press that may become a drag (document queue). 끌기가 될 수 있는 누름(문서 큐). */
    private Candidate candidate;
    /** The running drag (document queue). 진행 중인 끌기(문서 큐). */
    private Drag drag;
    /** The repeat timer (EDT). 반복 타이머(EDT). */
    private Timer timer;

    /**
     * Creates the controller of a page component.
     * <p>
     * 페이지 컴포넌트의 제어기를 만든다.
     *
     * @param component the component / 컴포넌트
     * @param session   the session / 세션
     * @param view      the view / 뷰
     * @param input     the pointer input / 포인터 입력
     * @param selection the selection controller / 선택 제어기
     * @param editing   the editing controller / 편집 제어기
     */
    public PageDragDrop(JComponent component, BrowserSession session, PageView view, PageInput input, PageSelection selection, PageEditing editing) {
        this.component = component;
        this.session = session;
        this.view = view;
        this.input = input;
        this.selection = selection;
        this.editing = editing;
    }

    /**
     * Tells whether a press may still turn into a drag (the pointer movements must be given to {@link #moved}).
     * <p>
     * 누름이 아직 끌기가 될 수 있는지 알려 준다(포인터 이동을 {@link #moved} 에 주어야 한다).
     *
     * @return true if armed / 준비되었으면 true
     */
    public boolean isArmed() {
        return armed;
    }

    /**
     * Tells whether a drag runs; mouse hover and key input are suppressed meanwhile.
     * <p>
     * 끌기가 도는지 알려 준다. 그동안 마우스 호버와 키 입력은 막는다.
     *
     * @return true while dragging / 끄는 동안 true
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Forgets a drag and a press that may become one because the page they belong to was replaced or cleaned (it may be called from any
     * thread): they end without events, the timer stops and the cursor is restored.
     * <p>
     * 속한 페이지가 교체되거나 clean 되어 끌기와 끌기가 될 수 있는 누름을 잊는다(어느 스레드에서나 부를 수 있다). 이벤트 없이 끝나고 타이머가 멈추며 커서가 복원된다.
     */
    void pageClosed() {
        boolean was = active;
        active = false;
        armed = false;
        lastPointer = null;
        stopTimer();
        if (was) {
            restoreCursor();
        }
    }

    /**
     * Stops everything (the component was disposed): a running drag ends without events.
     * <p>
     * 모든 것을 멈춘다(컴포넌트가 폐기되었다). 진행 중인 끌기는 이벤트 없이 끝난다.
     */
    public void dispose() {
        disposed = true;
        active = false;
        armed = false;
        lastPointer = null;
        stopTimer();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 누름과 뗌(문서 큐)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles the press of the main button after its <code>mousedown</code> was not cancelled (document queue): finds out whether it may
     * turn into a drag, either of the selected text (the press is on the highlight of the selection) or of a draggable element. Such a
     * press must not start a text selection, which the return value tells.
     * <p>
     * <code>mousedown</code> 이 취소되지 않은 주 버튼의 누름을 처리한다(문서 큐): 선택한 텍스트(누른 곳이 선택의 강조 위)나 끌 수 있는 요소의 끌기가 될 수 있는지
     * 알아낸다. 그런 누름은 텍스트 선택을 시작하면 안 되며 반환 값이 그것을 알려 준다.
     *
     * @param snapshot the page / 페이지
     * @param loc      what the press hit / 누름이 맞힌 것
     * @param pointer  the pointer of the press / 누른 포인터
     * @param shift    true if Shift is down (it extends a selection) / Shift 가 눌려 있으면 true(선택을 확장한다)
     * @return true if the press is a candidate for a drag (the caller must not start a text selection) / 끌기의 후보이면 true(호출자는 텍스트 선택을 시작하면 안 된다)
     */
    boolean pressed(PageSnapshot snapshot, PageInput.Located loc, PageInput.Pointer pointer, boolean shift) {
        candidate = null;
        armed = false;
        if (disposed || shift || loc == null || loc.bar != FrameContext.BAR_NONE || loc.hit == null || !input.current(snapshot)) {
            return false;
        }
        Element hit = loc.hit.getElement();
        // 폼 컨트롤 위의 누름은 컨트롤이 처리한다.
        if (hit == null || isInsideControl(hit)) {
            return false;
        }
        String href = loc.hit.getHref();
        // 선택한 글자 위를 한 번 누른 것이면 선택을 끌 수 있다(더블 클릭은 새 선택이다).
        if (loc.frame == null && pointer.clickCount == 1 && selection.isInsideSelection(snapshot, pointer.docX, pointer.docY)) {
            candidate = new Candidate(snapshot, null, loc.hit.getTextNode(), pointer, href);
            armed = true;
            return true;
        }
        Element draggable = draggableAncestor(hit);
        if (draggable == null) {
            return false;
        }
        candidate = new Candidate(snapshot, draggable, null, pointer, href);
        armed = true;
        return true;
    }

    /**
     * Handles the release of the main button (document queue). If a drag runs it ends (<code>drop</code> or the end without a drop, then
     * <code>dragend</code>) and the return value is true, so that the caller sends no <code>mouseup</code>. A press that was a candidate but
     * never moved far enough is a plain click: a press on the selected text then collapses the selection to the clicked place.
     * <p>
     * 주 버튼의 뗌을 처리한다(문서 큐). 끌기가 돌고 있으면 끝나고(<code>drop</code> 이나 놓지 않은 끝, 그리고 <code>dragend</code>) 반환 값이 true 이므로 호출자는
     * <code>mouseup</code> 을 보내지 않는다. 후보였지만 충분히 움직이지 않은 누름은 그냥 클릭이다. 선택한 텍스트 위의 누름은 선택을 클릭한 곳으로 접는다.
     *
     * @param snapshot the page / 페이지
     * @param page     the rendering of the page / 페이지의 렌더링
     * @param pointer  the pointer of the release / 뗀 포인터
     * @return true if a drag ended / 끌기가 끝났으면 true
     */
    boolean released(PageSnapshot snapshot, RenderedPage page, PageInput.Pointer pointer) {
        Drag d = drag;
        if (d != null) {
            lastPointer = pointer;
            // 뗀 자리로 마지막 반복을 한 뒤 끌기를 끝낸다(그 반복의 drag 이벤트를 취소했으면 놓기 없이 끝난다).
            step(pointer);
            end(pointer, false);
            return true;
        }
        Candidate c = candidate;
        candidate = null;
        armed = false;
        // 움직이지 않은 선택 위의 누름은 선택을 누른 곳의 캐럿으로 접는 보통 클릭이다.
        if (c != null && c.element == null && input.current(snapshot) && page != null) {
            selection.pressed(snapshot, page, c.press.docX, c.press.docY, 1, false);
            selection.released();
        }
        return false;
    }

    /**
     * Cancels the drag (the Escape key); it ends without a drop.
     * <p>
     * 끌기를 취소한다(Esc 키). 놓지 않고 끝난다.
     */
    public void cancel() {
        if (!active && !armed) {
            return;
        }
        session.submitDocumentTask(new Runnable() {
            /** Ends the drag on the document queue. / 문서 큐에서 끌기를 끝낸다. */
            public void run() {
                candidate = null;
                armed = false;
                if (drag != null) {
                    end(drag.pointer, true);
                }
            }
        });
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 포인터 이동(EDT)과 반복(문서 큐)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Receives a movement of the pointer while the main button is down (event dispatch thread): the document queue decides whether the drag
     * begins and, if it runs, does one iteration for the latest position (movements that arrive earlier are merged).
     * <p>
     * 주 버튼이 눌린 채 일어난 포인터 이동을 받는다(이벤트 디스패치 스레드). 문서 큐가 끌기가 시작하는지 정하고, 돌고 있으면 최신 위치로 반복 한 번을 한다(앞서 도착한
     * 이동은 합친다).
     *
     * @param event the AWT event / AWT 이벤트
     */
    public void moved(MouseEvent event) {
        if (disposed) {
            return;
        }
        lastPointer = new PageInput.Pointer(event, view.getZoom());
        // 누름이 아직 문서 큐에서 처리되기 전일 수 있으므로 준비되었는지는 문서 큐의 작업이 정한다(그 작업은 누름 작업 뒤에 실행된다).
        queueStep();
    }

    /**
     * Queues one iteration on the document queue (at most one is waiting).
     * <p>
     * 반복 한 번을 문서 큐에 예약한다(기다리는 것은 하나뿐이다).
     */
    private void queueStep() {
        if (disposed || !stepQueued.compareAndSet(false, true)) {
            return;
        }
        final PageSnapshot snapshot = session.getSnapshot();
        if (!session.submitDocumentTask(new Runnable() {
            /** Begins the drag or does an iteration on the document queue. / 문서 큐에서 끌기를 시작하거나 반복을 한다. */
            public void run() {
                stepQueued.set(false);
                PageInput.Pointer p = lastPointer;
                if (p != null && !disposed) {
                    iterate(snapshot, p);
                }
            }
        })) {
            stepQueued.set(false);
        }
    }

    /**
     * Does the work of one queued iteration: begins the drag if the pointer has left the threshold, else continues the running drag.
     * <p>
     * 예약된 반복 하나의 일을 한다: 포인터가 문턱을 넘었으면 끌기를 시작하고, 아니면 진행 중인 끌기를 이어 간다.
     *
     * @param snapshot the page / 페이지
     * @param p        the pointer / 포인터
     */
    private void iterate(PageSnapshot snapshot, PageInput.Pointer p) {
        Drag d = drag;
        if (d != null) {
            // 페이지가 바뀌었으면 끌기는 이벤트 없이 사라진다.
            if (!input.current(d.snapshot)) {
                abandon();
                return;
            }
            if (step(p)) {
                return;
            }
            end(p, false);
            return;
        }
        Candidate c = candidate;
        if (c == null || !input.current(c.snapshot) || !input.current(snapshot)) {
            return;
        }
        // 누른 곳에서 문턱만큼 움직이기 전에는 끌기가 시작하지 않는다.
        if (Math.abs(p.x - c.press.x) < THRESHOLD && Math.abs(p.y - c.press.y) < THRESHOLD) {
            return;
        }
        begin(c, p);
    }

    /**
     * Forgets the running drag without events (the page was replaced or the component was disposed).
     * <p>
     * 진행 중인 끌기를 이벤트 없이 잊는다(페이지가 교체되었거나 컴포넌트가 폐기되었다).
     */
    private void abandon() {
        drag = null;
        candidate = null;
        armed = false;
        active = false;
        stopTimer();
        restoreCursor();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 끌기의 시작
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Begins the drag of a candidate: makes the drag data store (the selected text, or the address of a link or an image), fires
     * <code>dragstart</code> at the source and, unless it is cancelled, starts the iterations.
     * <p>
     * 후보의 끌기를 시작한다: 드래그 데이터 저장소(선택한 텍스트, 링크나 이미지의 주소)를 만들고, 원본에 <code>dragstart</code> 를 보내며, 취소되지 않았으면 반복을
     * 시작한다.
     *
     * @param c the candidate / 후보
     * @param p the pointer / 포인터
     */
    private void begin(Candidate c, PageInput.Pointer p) {
        candidate = null;
        armed = false;
        PageSnapshot snapshot = c.snapshot;
        HtmlPageDocument holder = snapshot.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) snapshot.getDocument() : null;
        RenderedPage page = view.getRenderedPage();
        if (holder == null || page == null) {
            return;
        }
        Document doc = holder.getDocument();
        ClipboardData store = new ClipboardData();
        store.forceEffectAllowed("uninitialized");
        Element source;
        Range range = null;
        boolean editable = false;
        // 선택을 끄는 것이면 선택한 텍스트와 마크업이, 요소를 끄는 것이면 링크와 이미지의 주소가 데이터이다.
        if (c.element == null) {
            org.duckdns.hjow.swingwebpanel.dom.Selection sel = doc.getSelection();
            if (sel.getRangeCount() == 0 || sel.isCollapsed()) {
                return;
            }
            range = sel.getRangeAt(0).cloneRange();
            String text = selection.selectedText(snapshot);
            store.put("text/plain", text);
            String html = selection.selectedMarkup(snapshot);
            if (html.length() > 0) {
                store.put("text/html", html);
            }
            Node origin = c.textNode != null ? c.textNode : range.getStartContainer();
            source = origin instanceof Element ? (Element) origin : origin.getParentElement();
            Element h1 = EditingHosts.hostOf(range.getStartContainer());
            editable = h1 != null && h1 == EditingHosts.hostOf(range.getEndContainer());
        } else {
            source = c.element;
            addAddresses(store, source, c.href);
        }
        if (source == null) {
            return;
        }
        Drag d = new Drag(snapshot, source, range, editable, store);
        d.pointer = p;
        PageInput.Located loc = input.locate(snapshot, page, p.x, p.y);
        d.location = loc;
        // dragstart 는 읽기/쓰기 모드이다. 스크립트가 정한 데이터와 효과를 저장소로 가져온다.
        ClipboardData view1 = store.duplicate();
        view1.forceEffectAllowed("uninitialized");
        view1.setDropEffect("none");
        boolean notCancelled = fire(d, "dragstart", source, p, loc, null, view1, true);
        d.store = view1.duplicate();
        view1.disable();
        if (!notCancelled || !input.current(snapshot) || disposed) {
            return;
        }
        drag = d;
        active = true;
        // 끌기가 시작되면 그 포인터의 이벤트 스트림은 끝난다: pointercancel, pointerout, pointerleave 와 캡처 해제(Pointer Events 4.1.3.3).
        input.pointerCancelled(snapshot, p, loc);
        // lastPointer 는 EDT 가 그동안 더 새 위치로 바꾸었을 수 있으므로 여기서 덮어쓰지 않는다(첫 반복만 시작할 때의 포인터를 쓴다).
        startTimer();
        step(p);
    }

    /**
     * Adds the data that the specification gives to a dragged link or image: the absolute address as <code>text/uri-list</code> (and as
     * <code>text/plain</code>, as the browsers do, so that an editing host takes the address as text).
     * <p>
     * 규격이 끄는 링크나 이미지에 주는 데이터를 더한다: 절대 주소를 <code>text/uri-list</code> 로(그리고 브라우저처럼 <code>text/plain</code> 으로도. 편집 호스트가 주소를 텍스트로
     * 받는다).
     *
     * @param store  the drag data store / 드래그 데이터 저장소
     * @param source the dragged element / 끄는 요소
     * @param href   the address of the link under the press, or null / 누른 곳 링크의 주소. 없으면 null
     */
    private static void addAddresses(ClipboardData store, Element source, String href) {
        String url = null;
        String name = source.getLocalName();
        // 링크는 href, 이미지는 src 를 문서 기준으로 절대 주소로 바꾼다.
        if ("a".equals(name) && source.hasAttribute("href")) {
            url = href != null ? href : resolve(source, source.getAttribute("href"));
        } else if ("img".equals(name) && source.hasAttribute("src")) {
            url = resolve(source, source.getAttribute("src"));
        }
        if (url != null && url.length() > 0) {
            store.put("text/uri-list", url);
            store.put("text/plain", url);
        }
    }

    /**
     * Resolves an address against the base address of the document of an element.
     * <p>
     * 요소의 문서의 기준 주소로 주소를 해석한다.
     *
     * @param e   the element / 요소
     * @param ref the address as written / 쓰인 주소
     * @return the absolute address, or the address as written if it cannot be resolved / 절대 주소. 해석할 수 없으면 쓰인 그대로
     */
    private static String resolve(Element e, String ref) {
        try {
            return UrlUtil.resolve(e.getOwnerDocument().getBaseUri(), ref.trim());
        } catch (IllegalArgumentException ex) {
            return ref.trim();
        }
    }

    /**
     * Tells whether an element is, or is inside, a form control (which handles its own mouse input).
     * <p>
     * 요소가 폼 컨트롤이거나 그 안인지 알려 준다(컨트롤은 자기 마우스 입력을 처리한다).
     *
     * @param e the element / 요소
     * @return true if inside a control / 컨트롤 안이면 true
     */
    private static boolean isInsideControl(Element e) {
        // 위로 올라가며 폼 컨트롤이 있는지 본다.
        for (Node n = e; n instanceof Element; n = n.getParentNode()) {
            String name = ((Element) n).getLocalName();
            if ("input".equals(name) || "textarea".equals(name) || "select".equals(name) || "button".equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the element to drag for a press: the first element, going up from the pressed one, whose <code>draggable</code> is true, or null.
     * <p>
     * 누름으로 끌 요소를 반환한다: 누른 요소에서 위로 올라가며 <code>draggable</code> 이 true 인 첫 요소. 없으면 null.
     *
     * @param hit the pressed element / 누른 요소
     * @return the element or null / 요소. 없으면 null
     */
    static Element draggableAncestor(Element hit) {
        // 위로 올라가며 끌 수 있는 첫 요소를 찾는다(draggable="false" 도 위쪽 검색을 막지 않는다).
        for (Node n = hit; n instanceof Element; n = n.getParentNode()) {
            if (isDraggable((Element) n)) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * Tells whether the <code>draggable</code> of an element is true: the attribute says so, or it is absent or invalid and the element is an
     * image or a link with an <code>href</code>.
     * <p>
     * 요소의 <code>draggable</code> 이 true 인지 알려 준다: 속성이 그렇게 말하거나, 속성이 없거나 잘못되었고 요소가 이미지이거나 <code>href</code> 가 있는 링크이다.
     *
     * @param e the element / 요소
     * @return true if draggable / 끌 수 있으면 true
     */
    static boolean isDraggable(Element e) {
        String a = e.getAttribute("draggable");
        // 속성이 true 나 false 이면 그것을 따른다(대소문자 구분 없음).
        if (a != null && "true".equalsIgnoreCase(a.trim())) {
            return true;
        }
        if (a != null && "false".equalsIgnoreCase(a.trim())) {
            return false;
        }
        String name = e.getLocalName();
        return "img".equals(name) || ("a".equals(name) && e.hasAttribute("href"));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 반복(처리 모델)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Does one iteration of the drag (HTML Standard 6.11.5): <code>drag</code> at the source, then the choice of the current target element
     * with <code>dragenter</code> and <code>dragleave</code>, then <code>dragover</code>, and the drag operation that follows from the answers.
     *
     * @param p the pointer / 포인터
     * @return true if the drag goes on, false if the <code>drag</code> event was cancelled (the drag ends) / 끌기가 이어지면 true. <code>drag</code> 이벤트가 취소되어
     *         끌기가 끝나야 하면 false
     */
    private boolean step(PageInput.Pointer p) {
        Drag d = drag;
        RenderedPage page = view.getRenderedPage();
        if (d == null || page == null || !input.current(d.snapshot)) {
            return true;
        }
        d.pointer = p;
        PageInput.Located loc = input.locate(d.snapshot, page, p.x, p.y);
        d.location = loc;
        // 1. 원본에 drag 를 보낸다. 취소하면 끌기는 놓지 않고 끝난다.
        if (!fire(d, "drag", d.source, p, loc, null, protectedView(d, "none"), true)) {
            d.operation = "none";
            return false;
        }
        Element immediate = loc.bar != FrameContext.BAR_NONE ? null : loc.hit.getElement();
        Element previous = d.target;
        // 2. 포인터 아래 요소가 지난 반복과 다르고 지금 대상이 아니면 대상을 새로 정한다.
        if (!d.started || (immediate != d.immediate && immediate != d.target)) {
            d.started = true;
            d.immediate = immediate;
            if (immediate == null) {
                d.target = null;
            } else if (!fire(d, "dragenter", immediate, p, loc, null, protectedView(d, initialEffect(d)), true)) {
                // dragenter 를 취소하면 그 요소를 받아들인다.
                d.target = immediate;
            } else if (acceptsText(d, immediate)) {
                // 텍스트 컨트롤과 편집 호스트는 text/plain 이 있으면 스크립트 없이도 받아들인다.
                d.target = immediate;
            } else if (!"body".equals(immediate.getLocalName())) {
                // 그 밖에는 body 가 대상이 된다(body 위이면 그대로 둔다).
                Element body = bodyOf(immediate);
                if (body != null) {
                    fire(d, "dragenter", body, p, loc, null, protectedView(d, initialEffect(d)), true);
                    d.target = body;
                }
            }
        } else {
            d.immediate = immediate;
        }
        // 3. 대상이 바뀌었고 이전 대상이 있으면 이전 대상에 dragleave 를 보낸다(새 대상이 relatedTarget).
        if (previous != null && previous != d.target) {
            fire(d, "dragleave", previous, p, loc, d.target, protectedView(d, "none"), false);
        }
        // 4. 대상에 dragover 를 보내고 끌기 동작을 정한다.
        if (d.target != null) {
            ClipboardData over = protectedView(d, initialEffect(d));
            boolean notCancelled = fire(d, "dragover", d.target, p, loc, null, over, true);
            if (notCancelled) {
                // 취소하지 않았으면 텍스트를 받는 곳에서만 복사나 이동이 된다.
                d.operation = acceptsText(d, d.target) ? defaultTextOperation(d, p) : "none";
            } else {
                d.operation = operationOf(d.store.getEffectAllowed(), over.getDropEffect());
            }
        } else {
            d.operation = "none";
        }
        showOperation(d.operation, d.target != null);
        return true;
    }

    /**
     * Returns the <code>dropEffect</code> that <code>dragenter</code> and <code>dragover</code> start with, from the allowed effects and the
     * source (HTML Standard 6.11.4).
     * <p>
     * <code>dragenter</code> 와 <code>dragover</code> 가 시작하는 <code>dropEffect</code> 를 허용된 효과와 원본으로 정한다(HTML 표준 6.11.4).
     *
     * @param d the drag / 끌기
     * @return the effect / 효과
     */
    private static String initialEffect(Drag d) {
        String allowed = d.store.getEffectAllowed();
        if ("none".equals(allowed)) {
            return "none";
        }
        if ("copy".equals(allowed) || "copyLink".equals(allowed) || "copyMove".equals(allowed) || "all".equals(allowed)) {
            return "copy";
        }
        if ("link".equals(allowed) || "linkMove".equals(allowed)) {
            return "link";
        }
        if ("move".equals(allowed)) {
            return "move";
        }
        // uninitialized: 편집 호스트의 선택은 이동, 링크는 link, 그 밖은 복사이다.
        if (d.sourceRange != null && d.sourceEditable) {
            return "move";
        }
        if (d.sourceRange == null && "a".equals(d.source.getLocalName()) && d.source.hasAttribute("href")) {
            return "link";
        }
        return "copy";
    }

    /**
     * Returns the drag operation that follows when <code>dragover</code> was cancelled: the <code>dropEffect</code> if the allowed effects
     * include it, else none (HTML Standard 6.11.5).
     * <p>
     * <code>dragover</code> 를 취소했을 때의 끌기 동작을 반환한다: 허용된 효과가 <code>dropEffect</code> 를 포함하면 그것, 아니면 none(HTML 표준 6.11.5).
     *
     * @param allowed the allowed effects / 허용된 효과
     * @param drop    the <code>dropEffect</code> after the event / 이벤트 뒤의 <code>dropEffect</code>
     * @return the operation / 동작
     */
    static String operationOf(String allowed, String drop) {
        boolean any = "uninitialized".equals(allowed) || "all".equals(allowed);
        if ("copy".equals(drop) && (any || "copy".equals(allowed) || "copyLink".equals(allowed) || "copyMove".equals(allowed))) {
            return "copy";
        }
        if ("link".equals(drop) && (any || "link".equals(allowed) || "copyLink".equals(allowed) || "linkMove".equals(allowed))) {
            return "link";
        }
        if ("move".equals(drop) && (any || "move".equals(allowed) || "copyMove".equals(allowed) || "linkMove".equals(allowed))) {
            return "move";
        }
        return "none";
    }

    /**
     * Chooses the operation for a place that takes text without a script: move when the drag moves a selection inside editable text and
     * Ctrl is not down and the allowed effects permit it, else copy (or none if neither is allowed).
     * <p>
     * 스크립트 없이 텍스트를 받는 곳의 동작을 고른다: 편집 가능한 텍스트 안의 선택을 옮기는 끌기이고 Ctrl 이 눌려 있지 않으며 허용된 효과가 허락하면 이동, 아니면 복사
     * (둘 다 허용되지 않으면 none).
     *
     * @param d the drag / 끌기
     * @param p the pointer / 포인터
     * @return the operation / 동작
     */
    private static String defaultTextOperation(Drag d, PageInput.Pointer p) {
        String allowed = d.store.getEffectAllowed();
        boolean ctrl = (p.modifiersEx & MouseEvent.CTRL_DOWN_MASK) != 0;
        boolean canMove = operationOf(allowed, "move").equals("move");
        boolean canCopy = operationOf(allowed, "copy").equals("copy");
        if (d.sourceRange != null && d.sourceEditable && !ctrl && canMove) {
            return "move";
        }
        return canCopy ? "copy" : (canMove ? "move" : "none");
    }

    /**
     * Tells whether an element takes dragged text without any script: a text control or an editable place, when the drag carries text.
     * <p>
     * 요소가 스크립트 없이 끌어온 텍스트를 받는지 알려 준다: 끌기가 텍스트를 나를 때의 텍스트 컨트롤이나 편집 가능한 곳.
     *
     * @param d the drag / 끌기
     * @param e the element / 요소
     * @return true if it takes the text / 텍스트를 받으면 true
     */
    private static boolean acceptsText(Drag d, Element e) {
        return d.store.hasData("text/plain") && (PageFormHost.isTextControl(e) || EditingHosts.isEditable(e));
    }

    /**
     * Returns the body of the document of an element, or its document element.
     * <p>
     * 요소의 문서의 body 를 반환한다. 없으면 문서 요소.
     *
     * @param e the element / 요소
     * @return the body or null / body. 없으면 null
     */
    private static Element bodyOf(Element e) {
        Document doc = e.getOwnerDocument();
        org.duckdns.hjow.swingwebpanel.dom.ElementCollection bodies = doc.getElementsByTagName("body");
        return bodies.getLength() > 0 ? bodies.item(0) : doc.getDocumentElement();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 끝
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Ends the drag (HTML Standard 6.11.5): if there is no operation, or the user cancelled, or there is no target, the drag failed and the
     * target gets <code>dragleave</code>; otherwise the target gets <code>drop</code> (its default action, when it was not cancelled, puts the text
     * into a text control or an editing host); at last the source gets <code>dragend</code>, whose default action deletes a selection that
     * moved out of an editing host into a text control.
     *
     * @param p         the pointer / 포인터
     * @param cancelled true if the user cancelled the drag / 사용자가 끌기를 취소했으면 true
     */
    private void end(PageInput.Pointer p, boolean cancelled) {
        Drag d = drag;
        if (d == null) {
            return;
        }
        // 끝나는 동안 새 반복이 끼어들지 않게 상태를 먼저 닫는다.
        drag = null;
        active = false;
        stopTimer();
        PageSnapshot snapshot = d.snapshot;
        boolean dropped = false;
        PageInput.Located loc = d.location;
        PageInput.Pointer pointer = p == null ? d.pointer : p;
        Element target = d.target;
        String operation = d.operation;
        if (!input.current(snapshot) || pointer == null || loc == null) {
            restoreCursor();
            return;
        }
        // 동작이 없거나 취소되었거나 대상이 없으면 실패한 끌기이다. 대상에는 dragleave 를 보낸다.
        if ("none".equals(operation) || cancelled || target == null) {
            if (target != null) {
                fire(d, "dragleave", target, pointer, loc, null, protectedView(d, "none"), false);
            }
            operation = "none";
        } else {
            dropped = true;
            ClipboardData dropData = d.store.duplicate();
            dropData.setMode(ClipboardData.READ_ONLY);
            dropData.setDropEffect(operation);
            boolean notCancelled = fire(d, "drop", target, pointer, loc, null, dropData, true);
            if (!notCancelled) {
                // 취소했으면 끌기 동작은 이벤트 뒤의 dropEffect 이다.
                operation = operationOf(d.store.getEffectAllowed(), dropData.getDropEffect());
            } else if (!performDrop(d, target, pointer, loc, operation)) {
                // 기본 동작을 할 수 없는 곳이면 놓기가 되지 않은 것이다.
                operation = "none";
            }
        }
        d.operation = operation;
        // dragend 를 원본에 보낸다(dropEffect 는 최종 동작).
        ClipboardData endData = protectedView(d, operation);
        fire(d, "dragend", d.source, pointer, loc, null, endData, false);
        // dragend 의 기본 동작: 편집 호스트의 선택을 텍스트 컨트롤로 옮겼으면 원래 선택을 지운다.
        if (dropped && "move".equals(operation) && d.sourceRange != null && d.sourceEditable && target != null && PageFormHost.isTextControl(target)
                && input.current(snapshot) && snapshot.getDocument() instanceof HtmlPageDocument) {
            editing.deleteDragged(snapshot, ((HtmlPageDocument) snapshot.getDocument()).getDocument(), d.sourceRange);
        }
        restoreCursor();
    }

    /**
     * Does the default action of a <code>drop</code> that was not cancelled: a text control or an editable place takes the dragged text.
     * <p>
     * 취소되지 않은 <code>drop</code> 의 기본 동작을 한다: 텍스트 컨트롤이나 편집 가능한 곳이 끌어온 텍스트를 받는다.
     *
     * @param d         the drag / 끌기
     * @param target    the element that got the drop / drop 을 받은 요소
     * @param p         the pointer / 포인터
     * @param loc       what the pointer hit / 포인터가 맞힌 것
     * @param operation the drag operation / 끌기 동작
     * @return true if the text was taken / 텍스트를 받았으면 true
     */
    private boolean performDrop(Drag d, Element target, PageInput.Pointer p, PageInput.Located loc, String operation) {
        String text = d.store.peek("text/plain");
        if (text.length() == 0) {
            return false;
        }
        // 텍스트 컨트롤이면 값 끝에 붙인다.
        if (PageFormHost.isTextControl(target)) {
            return PageFormHost.insertDropped(target, text);
        }
        if (!EditingHosts.isEditable(target) || !(d.snapshot.getDocument() instanceof HtmlPageDocument) || loc.doc == null) {
            return false;
        }
        Document doc = loc.doc.getDocument();
        TextSelections.Caret caret = loc.frame == null ? selection.caretAtPoint(d.snapshot, p.docX, p.docY) : null;
        ClipboardData data = d.store.duplicate();
        data.setMode(ClipboardData.READ_ONLY);
        Range move = "move".equals(operation) && d.sourceRange != null && d.sourceEditable ? d.sourceRange : null;
        // 놓은 자리의 캐럿 위치가 없으면 대상 요소의 끝에 넣는다.
        Node node = caret != null ? caret.getNode() : target;
        int offset = caret != null ? caret.getOffset() : target.getChildNodes().getLength();
        return editing.drop(d.snapshot, doc, target, node, offset, data, move);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 이벤트 만들기와 전달
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Makes the data transfer of an event in the protected mode, on top of the drag data store, with the drop effect that the event starts
     * with.
     * <p>
     * 드래그 데이터 저장소 위에 이벤트의 데이터 전달 객체를 보호 모드로 만든다. 이벤트가 시작하는 드롭 효과를 가진다.
     *
     * @param d      the drag / 끌기
     * @param effect the <code>dropEffect</code> / <code>dropEffect</code>
     * @return the data transfer / 데이터 전달 객체
     */
    private static ClipboardData protectedView(Drag d, String effect) {
        ClipboardData view = d.store.duplicate();
        view.forceEffectAllowed(d.store.getEffectAllowed());
        view.setDropEffect(effect);
        view.setMode(ClipboardData.PROTECTED);
        return view;
    }

    /**
     * Fires a drag event at an element and ends the association of its data transfer with the drag.
     * <p>
     * 요소에 끌기 이벤트를 보내고 그 데이터 전달 객체와 끌기의 연결을 끊는다.
     *
     * @param d          the drag / 끌기
     * @param type       the event type / 이벤트 종류
     * @param target     the target / 대상
     * @param p          the pointer / 포인터
     * @param loc        what the pointer hit / 포인터가 맞힌 것
     * @param related    the related target or null / 상대 대상. 없으면 null
     * @param view       the data transfer of the event / 이벤트의 데이터 전달 객체
     * @param cancelable true if the event can be cancelled / 이벤트를 취소할 수 있으면 true
     * @return false if a listener cancelled the event / 리스너가 이벤트를 취소했으면 false
     */
    private boolean fire(Drag d, String type, Element target, PageInput.Pointer p, PageInput.Located loc, Element related, ClipboardData view,
            boolean cancelable) {
        DragEvent e = new DragEvent(type, true, cancelable);
        // 끌기 중에는 주 버튼이 눌려 있고 dragend 와 drop 때는 이미 뗐다.
        input.fillMouse(e, p, loc, related, 0, 0);
        e.setDataTransfer(view);
        boolean notCancelled = PageInput.fire(d.snapshot, target, e);
        // 이벤트가 끝나면 데이터 전달 객체는 끌기와 연결이 끊긴다. dragstart 의 결과는 호출자가 먼저 복사해 간다.
        if (!"dragstart".equals(type)) {
            view.disable();
        }
        return notCancelled;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 타이머와 커서(EDT)
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Starts the repeat timer on the EDT.
     * <p>
     * EDT 에서 반복 타이머를 시작한다.
     */
    private void startTimer() {
        final PageDragDrop self = this;
        SwingUtilities.invokeLater(new Runnable() {
            /** Starts the timer. / 타이머를 시작한다. */
            public void run() {
                if (self.disposed || !self.active) {
                    return;
                }
                if (self.timer == null) {
                    self.timer = new Timer(REPEAT_MS, new Ticker(self));
                }
                self.timer.restart();
            }
        });
    }

    /**
     * Stops the repeat timer on the EDT.
     * <p>
     * EDT 에서 반복 타이머를 멈춘다.
     */
    private void stopTimer() {
        SwingUtilities.invokeLater(new Runnable() {
            /** Stops the timer. / 타이머를 멈춘다. */
            public void run() {
                if (timer != null) {
                    timer.stop();
                }
            }
        });
    }

    /**
     * Shows the drag operation with the cursor of the component (the cursors of the platform for a drop, or the same with the sign of a
     * refusal where nothing can be dropped).
     * <p>
     * 컴포넌트의 커서로 끌기 동작을 보인다(놓을 수 있으면 플랫폼의 끌기 커서, 받는 곳이 없으면 금지 표시가 있는 같은 커서).
     *
     * @param operation the operation / 동작
     * @param hasTarget true if an element takes the drop / 놓을 요소가 있으면 true
     */
    private void showOperation(final String operation, final boolean hasTarget) {
        SwingUtilities.invokeLater(new Runnable() {
            /** Sets the cursor. / 커서를 정한다. */
            public void run() {
                if (disposed || !active) {
                    return;
                }
                boolean ok = hasTarget && !"none".equals(operation);
                Cursor c;
                try {
                    c = cursorOf(operation, ok);
                } catch (Throwable e) {
                    // 화면이 없는 환경에서는 플랫폼 커서를 만들 수 없으므로 기본 커서를 쓴다.
                    c = Cursor.getDefaultCursor();
                }
                component.setCursor(c);
            }
        });
    }

    /**
     * Returns the cursor of an operation from the drag and drop cursors of the platform.
     * <p>
     * 동작의 커서를 플랫폼의 끌어다 놓기 커서에서 고른다.
     *
     * @param operation the operation / 동작
     * @param ok        true if the drop would be accepted / 놓으면 받아들여지면 true
     * @return the cursor / 커서
     */
    private static Cursor cursorOf(String operation, boolean ok) {
        if ("move".equals(operation)) {
            return ok ? java.awt.dnd.DragSource.DefaultMoveDrop : java.awt.dnd.DragSource.DefaultMoveNoDrop;
        }
        if ("link".equals(operation)) {
            return ok ? java.awt.dnd.DragSource.DefaultLinkDrop : java.awt.dnd.DragSource.DefaultLinkNoDrop;
        }
        return ok ? java.awt.dnd.DragSource.DefaultCopyDrop : java.awt.dnd.DragSource.DefaultCopyNoDrop;
    }

    /**
     * Gives the component its normal cursor again and lets the hover logic choose it afresh.
     * <p>
     * 컴포넌트에 보통 커서를 되돌리고 호버 로직이 커서를 새로 고르게 한다.
     */
    private void restoreCursor() {
        SwingUtilities.invokeLater(new Runnable() {
            /** Restores the cursor. / 커서를 되돌린다. */
            public void run() {
                if (!disposed) {
                    component.setCursor(Cursor.getDefaultCursor());
                    input.requestHover();
                }
            }
        });
    }
}
