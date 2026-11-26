package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import org.duckdns.hjow.swingwebpanel.net.Origin;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.UiEvent;
import org.duckdns.hjow.swingwebpanel.dom.WheelEvent;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;
import org.duckdns.hjow.swingwebpanel.render.FrameScroll;
import org.duckdns.hjow.swingwebpanel.render.HitResult;
import org.duckdns.hjow.swingwebpanel.render.ImageMaps;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.render.TextSelections;

/**
 * Basic pointer/link input for the rendered component. Component coordinates already include JScrollPane's view translation.
 * Hit testing and DOM click dispatch run on the document queue; Swing link notifications and default navigation run on the EDT.
 * User notifications are separate from internal navigation, so removing listeners never disables links (P09-08-a/P11-03/04).
 * <p>렌더링 컴포넌트의 기본 포인터·링크 입력. 컴포넌트 좌표에는 이미 JScrollPane의 뷰 이동이 반영된다. 좌표 조회·DOM click은 문서 큐,
 * Swing 링크 통지·기본 이동은 EDT에서 처리한다. 사용자 통지와 내부 이동을 분리하므로 리스너를 모두 제거해도 링크는 작동한다.
 */
public final class PageInput extends MouseAdapter implements MouseMotionListener, MouseWheelListener {
    /** Swing event source and cursor owner. / Swing 이벤트 출처와 커서 소유자. */
    private final JComponent component;
    /** Session owning navigation and document work. / 이동과 문서 작업을 소유한 세션. */
    private final BrowserSession session;
    /** Immutable rendered snapshots. / 불변 렌더링 스냅샷. */
    private final PageView view;
    /** User link notifications, independent of default actions. / 기본 동작과 독립된 사용자 링크 통지. */
    private final HyperlinkDispatcher links;
    /** Suppresses obsolete hover results. / 오래된 호버 결과를 억제하는 번호. */
    private final AtomicInteger pointerVersion = new AtomicInteger();
    /** Coalesces pointer moves to one queued hit test. / 포인터 이동을 대기 중인 좌표 조회 하나로 합친다. */
    private final AtomicBoolean hoverQueued = new AtomicBoolean();
    /** Latest pointer x coordinate. / 최신 포인터 x좌표. */
    private volatile int pointerX;
    /** Latest pointer y coordinate. / 최신 포인터 y좌표. */
    private volatile int pointerY;
    /** Whether the pointer is over the component. / 포인터가 컴포넌트 위에 있는지 여부. */
    private volatile boolean inside;
    /** Stops further input after disposal. / 폐기 후 추가 입력을 막는 표시. */
    private volatile boolean disposed;
    /** The last pointer event, for the DOM events that the hover lookup sends. / 호버 조회가 보내는 DOM 이벤트를 위한 마지막 포인터 이벤트. */
    private volatile Pointer lastPointer;
    /** The element under the pointer for mouseover, mouseout, mouseenter and mouseleave; used on the document queue only. / mouseover, mouseout, mouseenter, mouseleave 를 위한 포인터 아래의 요소. 문서 큐에서만 쓴다. */
    private Element overElement;
    /** The page that {@link #overElement} belongs to. / {@link #overElement} 가 속한 페이지. */
    private PageSnapshot overSnapshot;
    /** The state of the mouse pointer that the scripts of the pages share (buttons, active document, pointer capture). / 페이지의 스크립트가 공유하는 마우스 포인터 상태(버튼, 활성 문서, 포인터 캡처). */
    private final PagePointers pointers;
    /** The element under the pointer for pointerover, pointerout, pointerenter and pointerleave (the capture target while one holds the pointer); used on the document queue only. / pointerover, pointerout, pointerenter, pointerleave 를 위한 포인터 아래의 요소(캡처가 있으면 캡처 대상). 문서 큐에서만 쓴다. */
    private Element pointerOver;
    /** The page that {@link #pointerOver} belongs to. / {@link #pointerOver} 가 속한 페이지. */
    private PageSnapshot pointerOverSnapshot;
    /** The PREVENT MOUSE EVENT flag of the specification: set when <code>pointerdown</code> was cancelled, it suppresses <code>mousedown</code>, <code>mousemove</code> and <code>mouseup</code> until the buttons are up; used on the document queue only. / 규격의 PREVENT MOUSE EVENT 표시: <code>pointerdown</code> 이 취소되면 정해지며 버튼을 뗄 때까지 <code>mousedown</code>, <code>mousemove</code>, <code>mouseup</code> 을 막는다. 문서 큐에서만 쓴다. */
    private boolean preventMouse;
    /** The buttons that were down before the event that is being handled, as the DOM bit mask; used on the document queue only. / 처리 중인 이벤트 전에 눌려 있던 버튼(DOM 비트 마스크). 문서 큐에서만 쓴다. */
    private int downButtons;
    /** The element on which a button went down and has not come up yet (for <code>:active</code>); used on the document queue only. / 버튼이 눌린 채 아직 떼지 않은 요소(<code>:active</code> 용). 문서 큐에서만 쓴다. */
    private Element pressedElement;
    /** Hovered DOM link identity, compared only on the EDT. / EDT에서만 비교하는 호버 DOM 링크 동일성. */
    private Element hoveredLink;
    /** Hovered absolute address, read only on the EDT. / EDT에서만 읽는 호버 절대 주소. */
    private String hoveredHref;
    /** True while the pointer is over selectable text, read only on the EDT. / 포인터가 선택할 수 있는 텍스트 위에 있는 동안 true. EDT에서만 읽는다. */
    private boolean hoveredText;
    /** The keyword of the CSS <code>cursor</code> under the pointer, read only on the EDT. 포인터 아래의 CSS <code>cursor</code> 키워드. EDT에서만 읽는다. */
    private String hoveredCursor = "auto";
    /** The selection by mouse and keyboard of this component. / 이 컴포넌트의 마우스·키보드 선택. */
    private final PageSelection selection;
    /** The editing by keys inside an editing host. / 편집 호스트 안의 키 편집. */
    private final PageEditing editing;
    /** The drag and drop of elements and of the selected text. / 요소와 선택한 텍스트의 끌어다 놓기. */
    private final PageDragDrop dragDrop;
    /** The resizing of boxes with <code>resize</code> by their handle. / <code>resize</code> 가 있는 상자를 핸들로 크기 조절하는 상태. */
    private final PageResize resize = new PageResize();
    /** Changes the zoom for Ctrl+wheel, or null for none. / Ctrl+휠의 확대를 바꾸는 쪽. 없으면 null. */
    private volatile ZoomControl zoomControl;
    /** Runs on the event dispatch thread before a mouse press is handled (it ends a running composition of an input method), or null. / 마우스 누름을 처리하기 전에 이벤트 디스패치 스레드에서 실행된다(입력기의 진행 중인 조합을 끝낸다). 없으면 null. */
    private volatile Runnable pressHook;

    /**
     * Creates a handler; the host installs it as its own mouse and mouse-motion listener.
     * <p>처리기를 만들며 호스트가 자체 마우스·마우스 이동 리스너로 설치한다.
     * @param component Swing event source / Swing 이벤트 출처
     * @param session owning session / 소유 세션
     * @param view rendered snapshots / 렌더링 스냅샷
     * @param links user notification registry / 사용자 통지 등록부
     */
    public PageInput(JComponent component, BrowserSession session, PageView view, HyperlinkDispatcher links) {
        this.component = component;
        this.session = session;
        this.view = view;
        this.links = links;
        this.pointers = session.getPointers();
        selection = new PageSelection(component, session, view);
        view.setSelection(selection);
        editing = new PageEditing(session, view, selection);
        dragDrop = new PageDragDrop(component, session, view, this, selection, editing);
        // 페이지가 닫히면 진행 중인 끌기도 이벤트 없이 끝난다(이 참조는 약하지 않아도 제어기와 같은 수명이다).
        selection.setClosedHook(new Runnable() {
            /** Forgets the drag of the page that was closed. / 닫힌 페이지의 끌기를 잊는다. */
            public void run() {
                dragDrop.pageClosed();
            }
        });
    }

    /**
     * Returns the drag and drop controller of the component (a drag of an element or of the selected text inside the page).
     * <p>
     * 컴포넌트의 끌어다 놓기 제어기를 반환한다(페이지 안의 요소나 선택한 텍스트의 끌기).
     *
     * @return the controller / 제어기
     */
    public PageDragDrop getDragDrop() {
        return dragDrop;
    }

    /**
     * Returns the editing controller (keys inside an editing host, cut and paste) of the component.
     * <p>
     * 컴포넌트의 편집 제어기(편집 호스트 안의 키, 잘라내기, 붙여넣기)를 반환한다.
     *
     * @return the controller / 제어기
     */
    public PageEditing getEditing() {
        return editing;
    }

    /**
     * Returns the selection controller (mouse selection, select all, copy, highlight) of the component.
     * <p>
     * 컴포넌트의 선택 제어기(마우스 선택, 전체 선택, 복사, 강조)를 반환한다.
     *
     * @return the controller / 제어기
     */
    public PageSelection getSelection() {
        return selection;
    }

    /**
     * Sets what runs on the event dispatch thread before a mouse press is handled (null for nothing): the component ends the composition
     * of an input method there, so that the composed text stays where it was composed and not where the press puts the caret.
     * <p>
     * 마우스 누름을 처리하기 전에 이벤트 디스패치 스레드에서 실행할 일을 정한다(null 이면 없음). 컴포넌트는 여기서 입력기의 조합을 끝내, 조합 중인 텍스트가
     * 누름이 캐럿을 옮기는 곳이 아니라 조합하던 자리에 남게 한다.
     *
     * @param hook the task or null / 할 일. 없으면 null
     */
    public void setPressHook(Runnable hook) {
        this.pressHook = hook;
    }

    /**
     * Sets the object that changes the page zoom for Ctrl+wheel (null: the wheel with Ctrl only scrolls).
     * <p>
     * Ctrl+휠이 페이지 확대를 바꾸는 데 쓰는 객체를 정한다(null 이면 Ctrl+휠도 스크롤만 한다).
     *
     * @param control the zoom control or null / 확대 제어. 없으면 null
     */
    public void setZoomControl(ZoomControl control) {
        this.zoomControl = control;
    }

    /**
     * Coalesces a pointer move without reading the DOM on the EDT.
     * <p>EDT에서 DOM을 읽지 않고 포인터 이동을 합친다.
     * @param event pointer event / 포인터 이벤트
     */
    public void mouseMoved(MouseEvent event) {
        Pointer pointer = new Pointer(event, view.getZoom());
        pointerX = pointer.x;
        pointerY = pointer.y;
        lastPointer = pointer;
        inside = true;
        pointers.setPresent(true);
        pointerVersion.incrementAndGet();
        requestHover();
    }

    /**
     * Updates hover during dragging and, while the main button is down, extends the text selection that the press started to the pointer
     * position; a pointer outside the visible area scrolls the viewport (the autoscroll of Swing repeats the call while the pointer is held).
     * <p>드래그 중 호버를 갱신하고, 주 버튼이 눌려 있으면 누름이 시작한 텍스트 선택을 포인터 위치까지 확장한다. 포인터가 보이는 영역 밖이면 뷰포트를
     * 스크롤한다(Swing 의 자동 스크롤이 포인터를 붙든 동안 호출을 반복한다).
     * @param event pointer event / 포인터 이벤트
     */
    public void mouseDragged(MouseEvent event) {
        final int px = event.getX();
        final int py = event.getY();
        // 프레임의 스크롤바 썸을 끄는 중이면 포인터를 따라 프레임을 스크롤한다. 누르기와 같은 문서 큐에서 순서대로 처리하므로 끌기 상태는 그 시점에 확인한다.
        session.submitDocumentTask(new Runnable() {
            /** Moves the scroll position of the frame with the thumb. / 썸을 따라 프레임의 스크롤 위치를 옮긴다. */
            public void run() {
                FrameContext frame = dragFrame;
                if (frame != null) {
                    double delta = (dragVertical ? py : px) - dragStart;
                    frame.scrollToThumb(dragVertical, dragThumb + delta);
                }
            }
        });
        boolean main = (event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
        // 크기 조절 핸들을 잡고 있으면 끌기는 상자의 크기를 바꾼다(호버, 선택, 끌어다 놓기가 받지 않는다). 누름은 문서 큐에서 처리되어 EDT 가 끌기를 받을 때 아직
        // 핸들을 잡았는지 모를 수 있으므로 끌기 위치는 언제나 문서 큐에 넘긴다(누름 뒤에 순서대로 처리되며 핸들을 잡지 않았으면 아무것도 하지 않는다).
        if (!disposed && main) {
            final Pointer resizePointer = new Pointer(event, view.getZoom());
            final PageSnapshot resizeSnapshot = session.getSnapshot();
            session.submitDocumentTask(new Runnable() {
                /** Sets the size of the element that the handle belongs to. / 핸들이 속한 요소의 크기를 정한다. */
                public void run() {
                    if (current(resizeSnapshot)) {
                        resize.dragged(resizePointer);
                    }
                }
            });
            if (resize.isActive()) {
                return;
            }
        }
        // 주 버튼으로 끄는 이동은 끌어다 놓기가 먼저 본다. 끌기가 도는 동안은 호버와 선택 확장 없이 끌기가 받는다.
        if (!disposed && main) {
            dragDrop.moved(event);
        }
        if (!disposed && main && dragDrop.isActive()) {
            component.scrollRectToVisible(new java.awt.Rectangle(event.getX(), event.getY(), 1, 1));
            return;
        }
        mouseMoved(event);
        if (disposed || !main) {
            return;
        }
        // 오디오 컨트롤 막대의 슬라이더를 잡고 있으면 끌기는 슬라이더의 것이다(텍스트 선택을 넓히거나 스크롤하지 않는다).
        if (dragMediaControls(event)) {
            return;
        }
        double zoom = view.getZoom();
        selection.dragged(session.getSnapshot(), event.getX() / zoom, event.getY() / zoom);
        component.scrollRectToVisible(new java.awt.Rectangle(event.getX(), event.getY(), 1, 1));
    }

    /** The frame whose scrollbar thumb is being dragged, or null. / 스크롤바 썸을 끌고 있는 프레임. 없으면 null. */
    private volatile FrameContext dragFrame;
    /** True if the dragged thumb belongs to the vertical scrollbar. / 끌고 있는 썸이 세로 스크롤바의 것이면 true. */
    private volatile boolean dragVertical;
    /** The pointer position along the axis when the drag began. / 끌기를 시작할 때 축을 따른 포인터 위치. */
    private volatile int dragStart;
    /** The start of the thumb along its track when the drag began. / 끌기를 시작할 때 트랙을 따른 썸의 시작 위치. */
    private volatile double dragThumb;

    /**
     * Clears hover when the pointer leaves the component.
     * <p>포인터가 컴포넌트를 떠나면 호버를 해제한다.
     * @param event pointer event / 포인터 이벤트
     */
    public void mouseExited(MouseEvent event) {
        pointers.setPresent(false);
        reset();
    }

    /**
     * Sends <code>mousedown</code> to the element under the pointer (and <code>contextmenu</code> if the platform starts its pop-up menu
     * on the press).
     * <p>포인터 아래 요소에 <code>mousedown</code> 을 보낸다(플랫폼이 누를 때 팝업 메뉴를 시작하면 <code>contextmenu</code> 도 보낸다).
     * @param event pointer event / 포인터 이벤트
     */
    public void mousePressed(MouseEvent event) {
        Runnable hook = pressHook;
        if (hook != null) {
            hook.run();
        }
        component.requestFocusInWindow();
        pointers.setPresent(true);
        sendMouse(new Pointer(event, view.getZoom()), "mousedown", true);
    }

    /**
     * Sends <code>mouseup</code> to the element under the pointer (and <code>contextmenu</code> if the platform starts its pop-up menu
     * on the release).
     * <p>포인터 아래 요소에 <code>mouseup</code> 을 보낸다(플랫폼이 뗄 때 팝업 메뉴를 시작하면 <code>contextmenu</code> 도 보낸다).
     * @param event pointer event / 포인터 이벤트
     */
    public void mouseReleased(MouseEvent event) {
        // 놓으면 오디오 컨트롤 막대의 슬라이더를 곧바로 놓는다(이후의 끌기 이벤트가 슬라이더를 움직이지 않도록 문서 큐를 기다리지 않는다).
        mediaDrag = null;
        sendMouse(new Pointer(event, view.getZoom()), "mouseup", true);
    }

    /**
     * Turns the wheel into a DOM <code>wheel</code> event and hands the AWT event to the scroll pane that contains the component, which
     * scrolls as before (a script cannot cancel the scrolling of the host's scroll pane).
     * <p>휠을 DOM <code>wheel</code> 이벤트로 바꾸고, AWT 이벤트는 컴포넌트를 담은 스크롤 창에 넘겨 전과 같이 스크롤되게 한다(스크립트는 호스트의
     * 스크롤 창의 스크롤을 취소할 수 없다).
     * @param event wheel event / 휠 이벤트
     */
    public void mouseWheelMoved(MouseWheelEvent event) {
        if (disposed) {
            return;
        }
        // Ctrl 을 누른 휠은 페이지 확대를 바꾼다(스크롤하지 않는다). DOM wheel 이벤트는 그대로 보낸다.
        boolean zooming = false;
        ZoomControl zoomer = zoomControl;
        if (zoomer != null && event.isControlDown() && !event.isAltDown()) {
            int direction = event.getWheelRotation() != 0 ? event.getWheelRotation() : (int) Math.signum(event.getUnitsToScroll());
            if (direction != 0) {
                zoomer.setZoomAnchor(event.getX(), event.getY());
                zoomer.setZoom(direction < 0 ? PageZoom.stepUp(zoomer.getZoom()) : PageZoom.stepDown(zoomer.getZoom()));
            }
            zooming = true;
        }
        final Pointer pointer = new Pointer(event, view.getZoom());
        final double amount = event.getUnitsToScroll();
        final boolean horizontal = event.isShiftDown();
        final PageSnapshot snapshot = session.getSnapshot();
        final MouseWheelEvent original = event;
        // 확대 중인 휠은 스크롤하지 않는다(프레임도 바깥 스크롤 창도).
        final boolean zoomOnly = zooming;
        // 페이지에 프레임이 있으면 휠을 프레임이 받을지 문서 큐에서 정한 뒤에 스크롤 창에 넘긴다.
        final boolean frames = hasFrames(snapshot);
        session.submitDocumentTask(new Runnable() {
            /** Dispatches the wheel event on the document queue. / 문서 큐에서 휠 이벤트를 전달한다. */
            public void run() {
                RenderedPage page = view.getRenderedPage();
                if (!current(snapshot) || page == null) {
                    if (frames && !zoomOnly) {
                        forwardWheel(original);
                    }
                    return;
                }
                Located loc = locate(snapshot, page, pointer.x, pointer.y);
                Element target = loc.bar != FrameContext.BAR_NONE ? null : loc.hit.getElement();
                boolean consumed = false;
                if (target != null) {
                    org.duckdns.hjow.swingwebpanel.dom.MouseEvent base = mouseEvent("wheel", pointer, loc, null, true, true, 0, 0);
                    WheelEvent wheel = new WheelEvent("wheel", true, true);
                    wheel.setCoordinates(base.getScreenX(), base.getScreenY(), base.getClientX(), base.getClientY(), base.getPageX(), base.getPageY(),
                            base.getOffsetX(), base.getOffsetY());
                    wheel.setButtons(0, pointer.buttons());
                    wheel.setModifiers(base.isCtrlKey(), base.isShiftKey(), base.isAltKey(), base.isMetaKey());
                    wheel.setDeltas(horizontal ? amount : 0, horizontal ? 0 : amount, 0, WheelEvent.DOM_DELTA_LINE);
                    boolean notCancelled = fire(snapshot, target, wheel);
                    // 프레임 위에서 스크립트가 휠을 취소하면 프레임도 바깥 스크롤 창도 스크롤하지 않는다. 프레임 밖에서는 전과 같이 취소할 수 없다.
                    consumed = !notCancelled && !loc.chain.isEmpty();
                }
                // 안쪽 프레임부터 바깥쪽으로, 이 방향으로 스크롤할 수 있는 첫 프레임이 휠을 받는다.
                for (int i = loc.chain.size() - 1; frames && !zoomOnly && i >= 0 && !consumed; i--) {
                    FrameContext f = loc.chain.get(i);
                    int distance = (int) Math.round(amount * FrameScroll.LINE);
                    int dx = horizontal ? distance : 0;
                    int dy = horizontal ? 0 : distance;
                    if (f.canScrollBy(dx, dy)) {
                        f.scrollBy(dx, dy);
                        consumed = true;
                    }
                }
                if (frames && !zoomOnly && !consumed) {
                    forwardWheel(original);
                }
            }
        });
        if (!frames && !zooming) {
            forwardWheel(event);
        }
    }

    /**
     * Tells whether the page of a snapshot has frames, so that the wheel must be offered to them first.
     * <p>
     * 스냅샷의 페이지에 프레임이 있는지 알려 준다. 있으면 휠을 먼저 프레임에 제안해야 한다.
     *
     * @param snapshot the page / 페이지
     * @return true if the page has at least one frame / 프레임이 하나라도 있으면 true
     */
    private static boolean hasFrames(PageSnapshot snapshot) {
        if (snapshot == null || !(snapshot.getDocument() instanceof HtmlPageDocument)) {
            return false;
        }
        FrameSet set = ((HtmlPageDocument) snapshot.getDocument()).frames();
        return set != null && !set.contexts().isEmpty();
    }

    /**
     * Gives a wheel event to the scroll pane that contains the component, which scrolls the page. The host scroll pane keeps the wheel when
     * no frame can use it. This component has a wheel listener, so AWT would not pass the event to the ancestors by itself.
     * <p>
     * 컴포넌트를 담은 스크롤 창에 휠 이벤트를 넘겨 페이지를 스크롤하게 한다. 프레임이 쓸 수 없는 휠은 호스트의 스크롤 창이 받는다. 이 컴포넌트는 휠 리스너를
     * 가지므로 AWT 가 이벤트를 조상에게 스스로 보내지 않는다.
     *
     * @param event the wheel event / 휠 이벤트
     */
    private void forwardWheel(final MouseWheelEvent event) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                /** Forwards the wheel on the EDT. / EDT 에서 휠을 넘긴다. */
                public void run() {
                    forwardWheel(event);
                }
            });
            return;
        }
        java.awt.Container pane = SwingUtilities.getAncestorOfClass(javax.swing.JScrollPane.class, component);
        if (pane != null && !disposed) {
            pane.dispatchEvent(SwingUtilities.convertMouseEvent(component, event, pane));
        }
    }

    /**
     * Queues the DOM events of a click: <code>click</code> (or <code>auxclick</code> for the other buttons) and, for the second click of a
     * double click, <code>dblclick</code>. DOM cancellation and listener-initiated navigation are checked before the default action of a
     * primary click.
     * <p>클릭의 DOM 이벤트를 예약한다: <code>click</code>(다른 버튼은 <code>auxclick</code>)과, 더블 클릭의 두 번째 클릭이면 <code>dblclick</code>.
     * 주 버튼 클릭의 기본 동작 전에 DOM 취소와 리스너가 시작한 이동을 확인한다.
     * @param event pointer event / 포인터 이벤트
     */
    public void mouseClicked(MouseEvent event) {
        if (disposed) {
            return;
        }
        final Pointer pointer = new Pointer(event, view.getZoom());
        final boolean primary = event.getButton() == MouseEvent.BUTTON1;
        final int x = pointer.x;
        final int y = pointer.y;
        final PageSnapshot snapshot = session.getSnapshot();
        session.submitDocumentTask(new Runnable() {
            /** Dispatches click before posting the default action. / 기본 동작 게시 전에 click을 전달한다. */
            public void run() {
                RenderedPage page = view.getRenderedPage();
                if (!current(snapshot) || page == null) {
                    return;
                }
                Located loc = locate(snapshot, page, x, y);
                Element target = loc.hit.getElement();
                final Element link = loc.hit.getLink();
                // 프레임의 스크롤바 위의 클릭은 스크롤바가 처리하며 문서에는 전달되지 않는다.
                if (target == null || loc.bar != FrameContext.BAR_NONE) {
                    return;
                }
                final long navigation = session.getNavigationCount();
                // click 과 auxclick 은 포인터 장치가 일으킨 것이면 PointerEvent 이다(Pointer Events 4.2.12).
                org.duckdns.hjow.swingwebpanel.dom.MouseEvent click = pointerClick(primary ? "click" : "auxclick", pointer, loc, pointer.domButton(), pointer.clickCount);
                fire(snapshot, target, click);
                if (primary && pointer.clickCount == 2) {
                    fire(snapshot, target, mouseEvent("dblclick", pointer, loc, null, true, true, 0, 2));
                }
                if (!primary) {
                    return;
                }
                // img 의 ismap 속성이 있는 서버 쪽 이미지 맵을 클릭했으면 주소 뒤에 ?x,y 를 붙인다(HTML5 2014 §4.5.1).
                String suffix = ImageMaps.serverSideSuffix(loc.hit.getBox(), target, link, loc.x, loc.y, loc.scrollX, loc.scrollY);
                followLink(snapshot, link, click.isDefaultPrevented(), navigation, suffix);
            }
        });
    }

    /**
     * Does what a browser does after a click on a link that no DOM listener cancelled: tells the hyperlink listeners and follows the link
     * once. A navigation that a listener started wins over the default one.
     * <p>
     * DOM 리스너가 취소하지 않은 링크 클릭 뒤에 브라우저가 하는 일을 한다: 하이퍼링크 리스너에 알리고 링크를 한 번 따라간다. 리스너가 시작한
     * 이동이 기본 이동보다 우선한다.
     *
     * @param snapshot   the page / 페이지
     * @param link       the link that was clicked, or null / 클릭한 링크. 없으면 null
     * @param prevented  true if a DOM listener cancelled the click / DOM 리스너가 클릭을 취소했으면 true
     * @param navigation the navigation count before the click was sent / 클릭을 보내기 전의 이동 횟수
     */
    private void followLink(final PageSnapshot snapshot, Element link, final boolean prevented, final long navigation) {
        followLink(snapshot, link, prevented, navigation, "");
    }

    /**
     * Does what a browser does after a click on a link, with the hyperlink suffix of a server-side image map: the suffix
     * (<code>?x,y</code> or the empty string) is appended to the address before its fragment.
     * <p>
     * 링크 클릭 뒤에 브라우저가 하는 일을 서버 쪽 이미지 맵의 하이퍼링크 접미사와 함께 한다: 접미사(<code>?x,y</code> 또는 빈 문자열)를 주소의 프래그먼트 앞에 붙인다.
     *
     * @param snapshot   the page / 페이지
     * @param link       the link that was clicked, or null / 클릭한 링크. 없으면 null
     * @param prevented  true if a DOM listener cancelled the click / DOM 리스너가 클릭을 취소했으면 true
     * @param navigation the navigation count before the click was sent / 클릭을 보내기 전의 이동 횟수
     * @param suffix     the hyperlink suffix / 하이퍼링크 접미사
     */
    private void followLink(final PageSnapshot snapshot, Element link, final boolean prevented, final long navigation, String suffix) {
        final String href = link == null ? null : withSuffix(resolve(link), suffix);
        if (href == null || !current(snapshot)) {
            return;
        }
        // download 속성이 있는 같은 오리진 링크는 이동 대신 저장한다(다른 오리진이면 속성을 무시하고 일반 이동). 속성 값은 제안 파일 이름이다.
        final String downloadName = link.hasAttribute("download") && Origin.of(link.getOwnerDocument().getOriginUrl()).isSameOrigin(Origin.of(href))
                ? link.getAttribute("download") : null;
        // 링크를 포함한 문서가 프레임의 것이면 그 프레임이 이동의 출발 문맥이다(문서 큐에서 구해 둔다).
        final HtmlPageDocument top = snapshot.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) snapshot.getDocument() : null;
        final FrameContext source = top == null || top.frames() == null ? null : top.frames().frameOf(link.getOwnerDocument());
        final Element anchor = link;
        SwingUtilities.invokeLater(new Runnable() {
            /** Notifies Swing, then runs at most one uncancelled default navigation.
 / Swing 통지 후 취소되지 않은 기본 이동을 최대 한 번 수행한다. */
            public void run() {
                if (!current(snapshot)) {
                    return;
                }
                links.fire(event(HyperlinkEvent.EventType.ACTIVATED, href));
                // DOM과 Swing 리스너가 시작한 이동·clean·dispose를 오래된 기본 동작이 덮어쓰지 못하게 한다.
                if (!prevented && current(snapshot) && navigation == session.getNavigationCount() && allowed(snapshot, href)) {
                    if (downloadName != null) {
                        session.downloadFromPage(href, downloadName);
                    } else if (source == null && FrameNavigator.targetOf(anchor).length() == 0) {
                        // 프레임이 없는 문서의 target 없는 링크는 전과 같이 컴포넌트의 이동이다.
                        session.navigateFromPage(href, false);
                    } else {
                        // target 이 가리키는 브라우징 문맥을 이동시킨다(프레임, 부모, 최상위).
                        final FrameContext from = source;
                        session.submitDocumentTask(new Runnable() {
                            /** Chooses and navigates the browsing context on the document queue. / 문서 큐에서 브라우징 문맥을 골라 이동시킨다. */
                            public void run() {
                                if (current(snapshot)) {
                                    FrameNavigator.followLink(session, top, from, anchor, href);
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    /**
     * Activates an element with the keyboard like a click does (Enter on a link, Enter or Space on a button): sends a trusted
     * <code>click</code> without coordinates and then runs the default action of a link. Call it on the document queue.
     * <p>
     * 키보드로 요소를 클릭처럼 활성화한다(링크의 Enter, 단추의 Enter 나 Space): 좌표 없는 신뢰 <code>click</code> 을 보내고 링크의 기본 동작을 한다.
     * 문서 큐에서 부른다.
     *
     * @param snapshot the page / 페이지
     * @param target   the element / 요소
     */
    void clickElement(PageSnapshot snapshot, Element target) {
        final long navigation = session.getNavigationCount();
        org.duckdns.hjow.swingwebpanel.dom.MouseEvent click = new org.duckdns.hjow.swingwebpanel.dom.MouseEvent("click", true, true);
        click.setCoordinates(0, 0, 0, 0, 0, 0, 0, 0);
        fire(snapshot, target, click);
        Element link = null;
        // 클릭한 요소에서 위로 올라가며 href 가 있는 링크를 찾는다.
        for (org.duckdns.hjow.swingwebpanel.dom.Node n = target; n instanceof Element; n = n.getParentNode()) {
            String name = ((Element) n).getLocalName();
            if (("a".equals(name) || "area".equals(name)) && ((Element) n).hasAttribute("href")) {
                link = (Element) n;
                break;
            }
        }
        followLink(snapshot, link, click.isDefaultPrevented(), navigation);
    }

    /**
     * Puts the dynamic state of the elements for <code>:hover</code>, <code>:active</code> and <code>:focus</code> into the style engine of the
     * page and redraws the page. Call it on the document queue.
     * <p>
     * <code>:hover</code>, <code>:active</code>, <code>:focus</code> 를 위한 요소의 동적 상태를 페이지의 스타일 엔진에 넣고 페이지를 다시 그린다.
     * 문서 큐에서 부른다.
     *
     * @param snapshot the page / 페이지
     * @param hover    the element under the pointer or null / 포인터 아래의 요소. 없으면 null
     * @param active   the element that the pressed button belongs to or null / 눌린 버튼이 속한 요소. 없으면 null
     */
    private void applyElementState(PageSnapshot snapshot, Element hover, Element active) {
        if (!(snapshot.getDocument() instanceof HtmlPageDocument)) {
            return;
        }
        HtmlPageDocument top = (HtmlPageDocument) snapshot.getDocument();
        // 요소가 프레임의 문서에 있으면 그 문서의 스타일 엔진에 상태를 넣는다.
        HtmlPageDocument hoverDoc = hover == null ? null : top.ownerOf(hover);
        HtmlPageDocument activeDoc = active == null ? null : top.ownerOf(active);
        List<HtmlPageDocument> previous = new ArrayList<HtmlPageDocument>(stateDocs);
        stateDocs.clear();
        // 이전에 상태를 넣었지만 이제 호버/활성 요소가 없는 프레임 문서의 상태를 지운다.
        for (int i = 0; i < previous.size(); i++) {
            HtmlPageDocument d = previous.get(i);
            if (d != top && d != hoverDoc && d != activeDoc) {
                applyDocumentState(d, null, null);
            }
        }
        applyDocumentState(top, hoverDoc == top ? hover : null, activeDoc == top ? active : null);
        if (hoverDoc != null && hoverDoc != top) {
            applyDocumentState(hoverDoc, hover, activeDoc == hoverDoc ? active : null);
            stateDocs.add(hoverDoc);
        }
        if (activeDoc != null && activeDoc != top && activeDoc != hoverDoc) {
            applyDocumentState(activeDoc, null, active);
            stateDocs.add(activeDoc);
        }
    }

    /** The frame documents that hold hover or active state; used on the document queue only. / 호버나 활성 상태를 가진 프레임 문서. 문서 큐에서만 쓴다. */
    private final List<HtmlPageDocument> stateDocs = new ArrayList<HtmlPageDocument>();

    /**
     * Puts the hover, active and focus state into the style engine of one document and redraws it.
     * <p>
     * 한 문서의 스타일 엔진에 호버, 활성, 포커스 상태를 넣고 다시 그린다.
     *
     * @param page   the document / 문서
     * @param hover  the hovered element of the document or null / 문서의 호버 요소. 없으면 null
     * @param active the active element of the document or null / 문서의 활성 요소. 없으면 null
     */
    private void applyDocumentState(HtmlPageDocument page, Element hover, Element active) {
        PagePresentation presentation = page.getPresentation();
        if (presentation == null) {
            return;
        }
        org.duckdns.hjow.swingwebpanel.css.selector.SimpleElementState state = presentation.elementState();
        state.setHover(hover);
        state.setActive(active);
        state.setFocus(page.getDocument() == null ? null : org.duckdns.hjow.swingwebpanel.dom.FocusManager.getFocused(page.getDocument()));
        presentation.elementStateChanged();
    }

    /**
     * Tells the style engine that the focus moved (called by the keyboard input and after a click moved the focus).
     * <p>
     * 포커스가 옮겨졌음을 스타일 엔진에 알린다(키보드 입력과 클릭이 포커스를 옮긴 뒤에 불린다).
     *
     * @param snapshot the page / 페이지
     */
    void focusChanged(PageSnapshot snapshot) {
        applyElementState(snapshot, overElement, pressedElement);
        // 편집 캐럿은 키보드 포커스를 가진 페이지에서만 보인다.
        selection.focusChanged(snapshot);
    }

    /**
     * What a point of the component is over, with the nested browsing contexts followed: the document that owns the deepest hit element, the
     * frame of that document (null for the top-level document), the hit in that document, the point in the document coordinates of that
     * document and the scroll position of its viewport. A point over the overlay scrollbar of a frame names the frame and the part
     * instead of an element.
     * <p>
     * 컴포넌트의 한 점 아래에 무엇이 있는지를 중첩 브라우징 문맥을 따라가며 구한 것: 가장 깊이 맞은 요소를 소유한 문서, 그 문서의 프레임(최상위 문서이면
     * null), 그 문서에서의 맞은 결과, 그 문서의 문서 좌표로 나타낸 점, 그 뷰포트의 스크롤 위치. 프레임의 오버레이 스크롤바 위의 점은 요소 대신 프레임과
     * 스크롤바의 부분을 가리킨다.
     */
    static final class Located {
        /** The document that owns the hit. / 맞은 것을 소유한 문서. */
        final HtmlPageDocument doc;
        /** The frame of that document, or null for the top-level document. / 그 문서의 프레임. 최상위 문서이면 null. */
        final FrameContext frame;
        /** The hit in that document. / 그 문서에서 맞은 결과. */
        final HitResult hit;
        /** The x coordinate in the document coordinates of that document. / 그 문서의 문서 좌표로 나타낸 x. */
        final int x;
        /** The y coordinate in the document coordinates of that document. / 그 문서의 문서 좌표로 나타낸 y. */
        final int y;
        /** The horizontal scroll position of its viewport. / 그 뷰포트의 가로 스크롤 위치. */
        final int scrollX;
        /** The vertical scroll position of its viewport. / 그 뷰포트의 세로 스크롤 위치. */
        final int scrollY;
        /** The part of the overlay scrollbar of a frame that was hit, or FrameContext.BAR_NONE. / 맞은 프레임의 오버레이 스크롤바 부분. 없으면 FrameContext.BAR_NONE. */
        final int bar;
        /** The frames from the outermost to the innermost that the point is inside of. / 점이 안에 있는 프레임들(가장 바깥부터 가장 안쪽까지). */
        final List<FrameContext> chain;

        /**
         * Creates the result.
         * <p>
         * 결과를 만든다.
         *
         * @param doc     the owner document / 소유 문서
         * @param frame   the frame or null / 프레임 또는 null
         * @param hit     the hit / 맞은 결과
         * @param x       x in document coordinates / 문서 좌표의 x
         * @param y       y in document coordinates / 문서 좌표의 y
         * @param scrollX horizontal scroll / 가로 스크롤
         * @param scrollY vertical scroll / 세로 스크롤
         * @param bar     the scrollbar part / 스크롤바 부분
         * @param chain   the frames the point is inside of / 점이 안에 있는 프레임들
         */
        Located(HtmlPageDocument doc, FrameContext frame, HitResult hit, int x, int y, int scrollX, int scrollY, int bar,
                List<FrameContext> chain) {
            this.doc = doc;
            this.frame = frame;
            this.hit = hit;
            this.x = x;
            this.y = y;
            this.scrollX = scrollX;
            this.scrollY = scrollY;
            this.bar = bar;
            this.chain = chain;
        }
    }

    /**
     * Hit tests a point of the component and follows the point into the frames that contain it: when the element that was hit is an
     * <code>iframe</code> or <code>frame</code> element with a document and the point is inside its content box, the point is tested again in
     * the document of the frame with the scroll position of the frame, and so on for nested frames.
     * <p>
     * 컴포넌트의 한 지점을 조회하고 그 점을 포함한 프레임 안으로 따라 들어간다: 맞은 요소가 문서가 있는 <code>iframe</code> 또는 <code>frame</code> 요소이고
     * 점이 콘텐츠 상자 안이면, 프레임의 스크롤 위치로 프레임의 문서에서 다시 조회하며, 중첩된 프레임도 마찬가지이다.
     *
     * @param snapshot the page / 페이지
     * @param page     the rendering of the top-level document / 최상위 문서의 렌더링
     * @param x        component x / 컴포넌트 x
     * @param y        component y / 컴포넌트 y
     * @return the result / 결과
     */
    Located locate(PageSnapshot snapshot, RenderedPage page, int x, int y) {
        java.awt.Point scroll = view.viewportPosition();
        HitResult hit = page.hitTest(x - scroll.x, y - scroll.y, scroll.x, scroll.y, 1.0);
        HtmlPageDocument doc = snapshot.getDocument() instanceof HtmlPageDocument ? (HtmlPageDocument) snapshot.getDocument() : null;
        List<FrameContext> chain = new ArrayList<FrameContext>();
        FrameContext frame = null;
        int px = x;
        int py = y;
        int sx = scroll.x;
        int sy = scroll.y;
        // 맞은 요소가 프레임 요소이고 점이 콘텐츠 상자 안이면 프레임의 문서에서 다시 조회한다(중첩된 프레임은 반복).
        while (doc != null && doc.frames() != null && FrameSet.isFrameElement(hit.getElement()) && hit.getBox() != null) {
            FrameContext f = doc.frames().contextOf(hit.getElement());
            RenderedPage inner = f == null ? null : f.lastRendered();
            HtmlPageDocument innerDoc = f == null ? null : f.page();
            LayoutBox box = hit.getBox();
            double cx = px - box.getContentX();
            double cy = py - box.getContentY();
            if (inner == null || innerDoc == null || cx < 0 || cy < 0 || cx >= box.getContentWidth() || cy >= box.getContentHeight()) {
                break;
            }
            chain.add(f);
            int bar = f.scrollbarAt(cx, cy);
            if (bar != FrameContext.BAR_NONE) {
                return new Located(innerDoc, f, hit, px, py, sx, sy, bar, chain);
            }
            Point fs = f.scrollPosition();
            hit = inner.hitTest(cx, cy, fs.x, fs.y, 1.0);
            doc = innerDoc;
            frame = f;
            px = (int) Math.floor(cx) + fs.x;
            py = (int) Math.floor(cy) + fs.y;
            sx = fs.x;
            sy = fs.y;
        }
        return new Located(doc, frame, hit, px, py, sx, sy, FrameContext.BAR_NONE, chain);
    }

    /** Queues at most one hover lookup; a later pointer version schedules the next lookup. / 호버 조회 하나를 예약하며 포인터 번호가 바뀌면 다음 조회를 예약한다. */
    void requestHover() {
        if (disposed || !hoverQueued.compareAndSet(false, true)) {
            return;
        }
        if (!session.submitDocumentTask(new Runnable() {
            /** Reads link identity and URL only on the document thread. / 문서 스레드에서만 링크 동일성과 URL을 읽는다. */
            public void run() {
                final int version = pointerVersion.get();
                final PageSnapshot snapshot = session.getSnapshot();
                Element link = null;
                String href = null;
                boolean text = false;
                String cursor = "auto";
                try {
                    RenderedPage page = view.getRenderedPage();
                    if (inside && current(snapshot) && page != null) {
                        Located loc = locate(snapshot, page, pointerX, pointerY);
                        link = loc.bar != FrameContext.BAR_NONE ? null : loc.hit.getLink();
                        href = loc.bar != FrameContext.BAR_NONE ? null : loc.hit.getHref();
                        moveDomPointer(snapshot, loc);
                        // 선택할 수 있는 텍스트 위이면 텍스트 커서를 쓴다.
                        text = loc.bar == FrameContext.BAR_NONE && loc.hit.getTextNode() != null && loc.hit.getBox() != null
                                && TextSelections.isSelectable(loc.hit.getBox());
                        // 맞은 상자의 CSS cursor(상속 속성)가 컴포넌트의 커서를 정한다.
                        if (loc.bar == FrameContext.BAR_NONE && loc.hit.getBox() != null) {
                            cursor = CssCursors.nameOf(loc.hit.getBox().getStyle());
                            // 크기 조절 핸들 위에서는 CSS cursor 에 상관없이 크기 조절 커서를 쓴다.
                            String handleCursor = PageResize.cursorAt(loc);
                            if (handleCursor != null) {
                                cursor = handleCursor;
                            }
                        }
                    }
                    final Element nextLink = link;
                    final String nextHref = href;
                    final boolean nextText = text;
                    final String nextCursor = cursor;
                    SwingUtilities.invokeLater(new Runnable() {
                        /** Publishes only the latest pointer lookup. / 최신 포인터 조회만 게시한다. */
                        public void run() {
                            if (version == pointerVersion.get() && current(snapshot)) {
                                applyHover(nextLink, nextHref, nextText, nextCursor);
                            }
                        }
                    });
                } finally {
                    hoverQueued.set(false);
                    if (version != pointerVersion.get()) {
                        requestHover();
                    }
                }
            }
        })) {
            hoverQueued.set(false);
        }
    }

    /** Clears pointer state after a page replacement or clean; safe from any thread. / 페이지 교체나 clean 후 포인터 상태를 지우며 어느 스레드에서나 안전하다. */
    public void reset() {
        inside = false;
        dragFrame = null;
        leaveDomPointer();
        final int version = pointerVersion.incrementAndGet();
        SwingUtilities.invokeLater(new Runnable() {
            /** Restores the normal cursor and underlying loading status. / 기본 커서와 원래 로딩 상태를 복원한다. */
            public void run() {
                if (version == pointerVersion.get()) {
                    applyHover(null, null, false, "auto");
                }
            }
        });
    }

    /** Permanently stops input; the component removes listener registrations. / 입력을 영구 중지하며 컴포넌트가 리스너 등록을 제거한다. */
    public void dispose() {
        disposed = true;
        pointers.setPresent(false);
        selection.dispose();
        editing.dispose();
        dragDrop.dispose();
        resize.release();
        reset();
    }

    /**
     * Checks page lifetime without reading the DOM.
     * <p>DOM을 읽지 않고 페이지 수명을 확인한다.
     * @param snapshot expected page / 기대 페이지
     * @return true while current / 현재이면 true
     */
    boolean current(PageSnapshot snapshot) {
        return !disposed && !session.isDisposed() && session.getSnapshot() == snapshot;
    }

    /**
     * Applies one hover transition on the EDT, sending EXITED before ENTERED.
     * <p>EDT에서 호버 전환을 적용하며 EXITED 뒤 ENTERED를 보낸다.
     * @param link link identity or null / 링크 동일성 또는 null
     * @param href absolute URL or null / 절대 URL 또는 null
     * @param text true if the pointer is over selectable text (text cursor) / 포인터가 선택할 수 있는 텍스트 위이면 true(텍스트 커서)
     * @param cursor the keyword of the CSS <code>cursor</code> under the pointer, <code>auto</code> for the cursor by link and text / 포인터 아래의 CSS <code>cursor</code> 키워드. 링크와 텍스트에 따르면 <code>auto</code>
     */
    private void applyHover(Element link, String href, boolean text, String cursor) {
        if (hoveredLink == link && hoveredText == text && hoveredCursor.equals(cursor) && (hoveredHref == null ? href == null : hoveredHref.equals(href))) {
            return;
        }
        if (!disposed && hoveredHref != null) {
            links.fire(event(HyperlinkEvent.EventType.EXITED, hoveredHref));
        }
        hoveredLink = link;
        hoveredHref = href;
        hoveredText = text;
        hoveredCursor = cursor;
        component.setCursor(CssCursors.cursorFor(cursor, href != null, text));
        session.setHoverText(href);
        if (!disposed && href != null) {
            links.fire(event(HyperlinkEvent.EventType.ENTERED, href));
        }
    }

    /**
     * Constructs a Swing event; special schemes may have no java.net.URL handler.
     * <p>Swing 이벤트를 만들며 특수 스키마는 java.net.URL 처리기가 없을 수 있다.
     * @param type event type / 이벤트 종류
     * @param href full address preserved as description / 설명에 보존할 전체 주소
     * @return Swing event / Swing 이벤트
     */
    private HyperlinkEvent event(HyperlinkEvent.EventType type, String href) {
        URL url = null;
        try {
            url = new URL(href);
        } catch (MalformedURLException e) {
            // classpath와 about 같은 주소는 설명 문자열만 전달한다.
        }
        return new HyperlinkEvent(component, type, url, href);
    }

    /**
     * Appends the hyperlink suffix of a server-side image map to an address, in front of its fragment.
     * <p>
     * 서버 쪽 이미지 맵의 하이퍼링크 접미사를 주소의 프래그먼트 앞에 붙인다.
     *
     * @param href   the address, may be null / 주소. null 일 수 있다
     * @param suffix the suffix such as <code>?3,4</code> or the empty string / <code>?3,4</code> 같은 접미사 또는 빈 문자열
     * @return the address with the suffix / 접미사를 붙인 주소
     */
    private static String withSuffix(String href, String suffix) {
        if (href == null || suffix == null || suffix.length() == 0) {
            return href;
        }
        int hash = href.indexOf('#');
        // 프래그먼트가 있으면 그 앞에 접미사를 붙인다.
        return hash < 0 ? href + suffix : href.substring(0, hash) + suffix + href.substring(hash);
    }

    /**
     * Resolves the current href after DOM listeners may have changed it.
     * <p>DOM 리스너가 바꿀 수 있는 현재 href를 이벤트 전달 후 해석한다.
     * @param link link element / 링크 요소
     * @return target or null / 대상 또는 null
     */
    private static String resolve(Element link) {
        String href = link.getAttribute("href");
        if (href == null) {
            return null;
        }
        try {
            return UrlUtil.resolve(link.getOwnerDocument().getBaseUri(), href.trim());
        } catch (IllegalArgumentException e) {
            return href.trim();
        }
    }

    /**
     * Applies navigation scheme policy and prevents network pages from entering the host application's classpath.
     * <p>이동 스키마 정책을 적용하고 네트워크 문서가 호스트 애플리케이션의 클래스패스에 진입하지 못하게 한다.
     * @param snapshot source page / 출처 페이지
     * @param href target address / 대상 주소
     * @return whether the default action may navigate / 기본 이동 허용 여부
     */
    private static boolean allowed(PageSnapshot snapshot, String href) {
        return NavigationPolicy.allowed(snapshot.getUrl(), href);
    }

    /**
     * What the DOM events need from one AWT pointer event, copied on the EDT so that the document queue never touches the AWT event.
     * <p>
     * 한 AWT 포인터 이벤트에서 DOM 이벤트가 필요로 하는 값. 문서 큐가 AWT 이벤트를 만지지 않도록 EDT 에서 복사해 둔다.
     */
    static final class Pointer {
        /** Document x in whole CSS pixels (the component x divided by the zoom factor), see {@link PageInput#hitAt}. / 문서 x(CSS 정수 픽셀. 컴포넌트 x 를 배율로 나눈 값). {@link PageInput#hitAt} 참조. */
        final int x;
        /** Document y in whole CSS pixels. / 문서 y(CSS 정수 픽셀). */
        final int y;
        /** Document x with its fraction (for selecting text). / 소수를 포함한 문서 x(텍스트 선택용). */
        final double docX;
        /** Document y with its fraction. / 소수를 포함한 문서 y. */
        final double docY;
        /** The x coordinate on the screen. / 화면에서의 x 좌표. */
        final int screenX;
        /** The y coordinate on the screen. / 화면에서의 y 좌표. */
        final int screenY;
        /** The extended modifiers (keys and buttons that were down). / 확장 보조 값(눌려 있던 키와 버튼). */
        final int modifiersEx;
        /** The AWT button that changed. / 바뀐 AWT 버튼. */
        final int button;
        /** The click count. / 클릭 횟수. */
        final int clickCount;
        /** True if the event starts a pop-up menu on this platform. / 이 플랫폼에서 팝업 메뉴를 시작하는 이벤트이면 true. */
        final boolean popup;

        /**
         * Copies the values of an AWT event.
         * <p>
         * AWT 이벤트의 값을 복사한다.
         *
         * @param e    the event / 이벤트
         * @param zoom the zoom factor that converts component coordinates to document coordinates / 컴포넌트 좌표를 문서 좌표로 바꾸는 확대 배율
         */
        Pointer(MouseEvent e, double zoom) {
            docX = e.getX() / zoom;
            docY = e.getY() / zoom;
            x = (int) Math.round(docX);
            y = (int) Math.round(docY);
            screenX = e.getXOnScreen();
            screenY = e.getYOnScreen();
            modifiersEx = e.getModifiersEx();
            button = e.getButton();
            clickCount = e.getClickCount();
            popup = e.isPopupTrigger();
        }

        /**
         * Returns the DOM button number of the AWT button (0 main, 1 auxiliary, 2 secondary).
         * <p>
         * AWT 버튼의 DOM 버튼 번호를 반환한다(0 주, 1 보조, 2 보조 버튼).
         *
         * @return the number / 번호
         */
        int domButton() {
            if (button == MouseEvent.BUTTON2) {
                return 1;
            }
            return button == MouseEvent.BUTTON3 ? 2 : 0;
        }

        /**
         * Returns the DOM bit mask of the buttons that are down.
         * <p>
         * 눌려 있는 버튼의 DOM 비트 마스크를 반환한다.
         *
         * @return the mask / 마스크
         */
        int buttons() {
            int mask = 0;
            if ((modifiersEx & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                mask |= 1;
            }
            if ((modifiersEx & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
                mask |= 2;
            }
            if ((modifiersEx & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
                mask |= 4;
            }
            return mask;
        }
    }

    /**
     * Builds a DOM mouse event for a pointer event. The client coordinates subtract the scroll position of the viewport, the page
     * coordinates are the document coordinates, and the offset coordinates are relative to the padding edge of the box that was hit.
     * <p>
     * 포인터 이벤트로 DOM 마우스 이벤트를 만든다. client 좌표는 뷰포트의 스크롤 위치를 빼고, page 좌표는 문서 좌표이며, offset 좌표는 맞은 상자의
     * padding 가장자리를 기준으로 한다.
     *
     * @param type       the event type / 이벤트 종류
     * @param p          the pointer / 포인터
     * @param loc        what was hit, with the coordinates in the document that owns it, or null / 맞은 것과 그것을 소유한 문서의 좌표. 없으면 null
     * @param related    the other element of over/out events, or null / over/out 이벤트의 상대 요소. 없으면 null
     * @param bubbles    whether the event bubbles / 버블링 여부
     * @param cancelable whether the event can be cancelled / 취소 가능 여부
     * @param button     the DOM button that changed / 바뀐 DOM 버튼
     * @param detail     the detail (click count) / 상세(클릭 횟수)
     * @return the event / 이벤트
     */
    private org.duckdns.hjow.swingwebpanel.dom.MouseEvent mouseEvent(String type, Pointer p, Located loc, Element related, boolean bubbles,
            boolean cancelable, int button, int detail) {
        org.duckdns.hjow.swingwebpanel.dom.MouseEvent e = new org.duckdns.hjow.swingwebpanel.dom.MouseEvent(type, bubbles, cancelable);
        fillMouse(e, p, loc, related, button, detail);
        return e;
    }

    /**
     * Builds the <code>click</code>, <code>auxclick</code> or <code>contextmenu</code> event that a pointing device caused: a
     * <code>PointerEvent</code> of the pointer (id 1, type <code>mouse</code>) whose other pointer properties have their defaults (a contact of 1 by 1 and
     * pressure 0), which bubbles and can be cancelled (W3C Pointer Events Level 3, section 4.2.12).
     * <p>
     * 포인터 장치가 일으킨 <code>click</code>, <code>auxclick</code>, <code>contextmenu</code> 이벤트를 만든다: 그 포인터(ID 1, 종류 <code>mouse</code>)의
     * <code>PointerEvent</code> 이며 다른 포인터 속성은 기본값(1 x 1 접촉, 압력 0)이고 버블링하며 취소할 수 있다(W3C Pointer Events Level 3, 4.2.12).
     *
     * @param type   the event type / 이벤트 종류
     * @param p      the pointer / 포인터
     * @param loc    what was hit / 맞은 것
     * @param button the DOM button / DOM 버튼
     * @param detail the click count / 클릭 횟수
     * @return the event / 이벤트
     */
    private org.duckdns.hjow.swingwebpanel.dom.PointerEvent pointerClick(String type, Pointer p, Located loc, int button, int detail) {
        org.duckdns.hjow.swingwebpanel.dom.PointerEvent e = org.duckdns.hjow.swingwebpanel.dom.PointerEvent.forMouse(type, true, true);
        fillMouse(e, p, loc, null, button, detail);
        e.setComposed(true);
        return e;
    }

    /**
     * Fills the mouse members of an event (a mouse event or a drag event) from a pointer: the client coordinates without the scroll
     * position of the viewport, the page coordinates in the document, the offset to the padding edge of the box that was hit, the buttons,
     * the keys, the detail and the related target.
     * <p>
     * 포인터로 이벤트(마우스 이벤트나 끌기 이벤트)의 마우스 멤버를 채운다: 뷰포트의 스크롤 위치를 뺀 client 좌표, 문서의 page 좌표, 맞은 상자의 padding 가장자리까지의
     * offset, 버튼, 키, 상세, 상대 대상.
     *
     * @param e       the event / 이벤트
     * @param p       the pointer / 포인터
     * @param loc     what was hit, or null / 맞은 것. 없으면 null
     * @param related the related target or null / 상대 대상. 없으면 null
     * @param button  the DOM button that changed / 바뀐 DOM 버튼
     * @param detail  the detail / 상세
     */
    void fillMouse(org.duckdns.hjow.swingwebpanel.dom.MouseEvent e, Pointer p, Located loc, Element related, int button, int detail) {
        // 프레임 안의 점은 그 프레임 문서의 좌표(문서 좌표와 그 뷰포트의 스크롤)로 나타낸다.
        java.awt.Point scroll = loc == null ? view.viewportPosition() : new java.awt.Point(loc.scrollX, loc.scrollY);
        int px = loc == null ? p.x : loc.x;
        int py = loc == null ? p.y : loc.y;
        double offsetX = px;
        double offsetY = py;
        LayoutBox box = loc == null || loc.hit == null ? null : loc.hit.getBox();
        if (box != null) {
            offsetX = px - (box.getX() + box.getBorderLeft());
            offsetY = py - (box.getY() + box.getBorderTop());
        }
        e.setCoordinates(p.screenX, p.screenY, px - scroll.x, py - scroll.y, px, py, offsetX, offsetY);
        e.setButtons(button, p.buttons());
        e.setModifiers((p.modifiersEx & MouseEvent.CTRL_DOWN_MASK) != 0, (p.modifiersEx & MouseEvent.SHIFT_DOWN_MASK) != 0,
                (p.modifiersEx & MouseEvent.ALT_DOWN_MASK) != 0, (p.modifiersEx & MouseEvent.META_DOWN_MASK) != 0);
        e.setDetail(detail);
        e.setRelatedTarget(related);
    }

    /**
     * Dispatches an event that the user caused, as a trusted event on the document queue (through the page's script host when there is
     * one, so that microtasks and observers run afterwards).
     * <p>
     * 사용자가 일으킨 이벤트를 문서 큐에서 신뢰 이벤트로 전달한다(스크립트 호스트가 있으면 그것을 거쳐 이후에 마이크로태스크와 관찰자가 실행된다).
     *
     * @param snapshot the page / 페이지
     * @param target   the target / 대상
     * @param event    the event / 이벤트
     * @return false if a listener cancelled it / 리스너가 취소했으면 false
     */
    static boolean fire(PageSnapshot snapshot, Element target, UiEvent event) {
        return fire(snapshot, target, event, false);
    }

    /**
     * Dispatches trusted input and passes a platform paste shortcut to the target's own document.
     * <p>
     * 신뢰 입력을 전달하며 플랫폼 붙여넣기 단축키를 대상의 문서에 알린다.
     *
     * @param snapshot active rendered page / 활성 렌더링 페이지
     * @param target event target / 이벤트 대상
     * @param event trusted input / 신뢰 입력
     * @param pasteAction whether this was a platform paste shortcut / 플랫폼 붙여넣기 단축키 여부
     * @return true if the event was not cancelled / 이벤트가 취소되지 않았으면 true
     */
    static boolean fire(PageSnapshot snapshot, Element target, UiEvent event, boolean pasteAction) {
        PageDocument document = snapshot.getDocument();
        if (document instanceof HtmlPageDocument) {
            // 대상 요소가 프레임의 문서에 있으면 그 문서의 스크립트 호스트로 보낸다.
            return ((HtmlPageDocument) document).ownerOf(target).dispatchInput(target, event, pasteAction);
        }
        return target.dispatchTrustedEvent(event);
    }

    /**
     * Queues a DOM mouse event (<code>mousedown</code> or <code>mouseup</code>) for the element under the pointer, followed by
     * <code>contextmenu</code> if this event opens the pop-up menu of the platform.
     * <p>
     * 포인터 아래 요소에 DOM 마우스 이벤트(<code>mousedown</code> 또는 <code>mouseup</code>)를 예약하고, 이 이벤트가 플랫폼의 팝업 메뉴를 여는
     * 것이면 <code>contextmenu</code> 를 이어서 보낸다.
     *
     * @param pointer     the pointer / 포인터
     * @param type        the event type / 이벤트 종류
     * @param withContext true to send <code>contextmenu</code> when the event is a pop-up trigger / 팝업 트리거이면 <code>contextmenu</code> 도 보내려면 true
     */
    private void sendMouse(final Pointer pointer, final String type, final boolean withContext) {
        if (disposed) {
            return;
        }
        final PageSnapshot snapshot = session.getSnapshot();
        session.submitDocumentTask(new Runnable() {
            /** Dispatches the event on the document queue. / 문서 큐에서 이벤트를 전달한다. */
            public void run() {
                RenderedPage page = view.getRenderedPage();
                if (!current(snapshot) || page == null) {
                    return;
                }
                Located loc = locate(snapshot, page, pointer.x, pointer.y);
                // 이 이벤트 전에 눌려 있던 버튼을 기억하고 새 상태를 기록한다(포인터 이벤트의 pointerdown/pointerup 판정과 캡처 서비스가 쓴다).
                final int before = downButtons;
                downButtons = pointer.buttons();
                pointers.setButtons(downButtons);
                // 프레임의 스크롤바 위에서 누르면 스크롤바가 처리하고(썸 끌기, 트랙 클릭으로 한 쪽씩) 문서에는 전달하지 않는다.
                if (loc.bar != FrameContext.BAR_NONE) {
                    scrollbarPress(type, pointer, loc);
                    return;
                }
                // 크기 조절 핸들을 누르면 사용자 인터페이스 동작이므로 페이지는 마우스 이벤트를 받지 않고 텍스트 선택도 시작하지 않는다.
                if ("mousedown".equals(type) && pointer.button == MouseEvent.BUTTON1 && resize.press(loc, pointer)) {
                    return;
                }
                // 핸들을 잡은 채 뗀 버튼은 크기 조절을 끝낸다(mouseup 도 click 도 보내지 않는다).
                if ("mouseup".equals(type) && pointer.button == MouseEvent.BUTTON1 && resize.isActive()) {
                    resize.release();
                    return;
                }
                if ("mouseup".equals(type)) {
                    dragFrame = null;
                    // 끌어다 놓기가 도는 동안의 뗌은 끌기를 끝낸다(mouseup 도 click 도 보내지 않는다). 포인터는 끌기가 시작될 때 이미 취소되었다.
                    if (pointer.button == MouseEvent.BUTTON1 && dragDrop.released(snapshot, page, pointer)) {
                        pressedElement = null;
                        preventMouse = false;
                        applyElementState(snapshot, overElement, null);
                        selection.released();
                        return;
                    }
                }
                Element hitTarget = loc.hit.getElement();
                // 포인터 이벤트가 먼저 간다(Pointer Events 11.2): 대기 중인 캡처를 처리하고 경계 이벤트를 맞춘 뒤 pointerdown(또는 연속 버튼의 pointermove)을 보낸다.
                Element target = hitTarget;
                if ("mousedown".equals(type) || "mouseup".equals(type)) {
                    pointers.setActiveDocument(loc.doc == null ? null : loc.doc.getDocument());
                    processPendingCapture(snapshot, pointer, loc);
                    target = pointerTarget(hitTarget);
                    if (target != null) {
                        pointerBoundary(snapshot, pointer, loc, target);
                        boolean first = "mousedown".equals(type) ? before == 0 : downButtons == 0;
                        String pointerType = first ? ("mousedown".equals(type) ? "pointerdown" : "pointerup") : "pointermove";
                        boolean pointerNotCancelled = fire(snapshot, target, pointerEvent(pointerType, pointer, loc, null, pointer.domButton()));
                        // 취소된 pointerdown 은 버튼을 뗄 때까지 호환 마우스 이벤트(mousedown, mousemove, mouseup)를 막는다.
                        if ("pointerdown".equals(pointerType) && !pointerNotCancelled) {
                            preventMouse = true;
                        }
                    }
                }
                // 어디를 누르든 그 곳의 프레임(또는 최상위 문서)이 이후 키보드 입력을 받는다. 빈 영역을 눌러도 마찬가지이다.
                if ("mousedown".equals(type) && snapshot.getDocument() instanceof HtmlPageDocument) {
                    FrameSet roots = ((HtmlPageDocument) snapshot.getDocument()).frames();
                    if (roots != null) {
                        roots.setFocusedFrame(loc.frame);
                    }
                }
                boolean released = "mouseup".equals(type) && downButtons == 0;
                if (target == null) {
                    // 대상이 없어도 마지막 버튼을 뗐으면 포인터 이벤트 상태는 정리한다.
                    if (released) {
                        endPointerPress(snapshot, pointer, loc, hitTarget);
                    }
                    return;
                }
                // 취소된 pointerdown 이 호환 마우스 이벤트를 막았으면 mousedown/mouseup 을 보내지 않는다. 기본 동작(포커스, 선택)은 그대로 한다.
                boolean notCancelled = true;
                if (!preventMouse) {
                    notCancelled = fire(snapshot, target, mouseEvent(type, pointer, loc, null, true, true, pointer.domButton(), pointer.clickCount));
                }
                if ("mousedown".equals(type)) {
                    pressedElement = target;
                    // 취소되지 않은 mousedown 은 클릭한 곳으로 포커스를 옮긴다: 포커스를 가질 수 있는 가장 가까운 요소로, 없으면 포커스를 거둔다.
                    // 프레임 안을 클릭하면 그 프레임의 문서에서 옮기며, 이후 키보드 입력도 그 문서가 받는다.
                    if (notCancelled && loc.doc != null) {
                        org.duckdns.hjow.swingwebpanel.dom.Document doc = loc.doc.getDocument();
                        Element focusable = PageKeyboard.focusableAncestor(target);
                        Element focused = org.duckdns.hjow.swingwebpanel.dom.FocusManager.getFocused(doc);
                        if (loc.frame != null && snapshot.getDocument() instanceof HtmlPageDocument) {
                            doc.setWindowFocused(((HtmlPageDocument) snapshot.getDocument()).getDocument().isWindowFocused());
                        }
                        if (focusable != null) {
                            org.duckdns.hjow.swingwebpanel.dom.FocusManager.focus(focusable, doc.isWindowFocused());
                        } else if (focused != null) {
                            org.duckdns.hjow.swingwebpanel.dom.FocusManager.blur(focused, doc.isWindowFocused());
                        }
                    }
                    applyElementState(snapshot, overElement, pressedElement);
                    // 포커스가 편집 호스트로 옮겨졌을 수 있으므로 캐럿과 캐럿 키의 소유를 다시 정한다.
                    selection.focusChanged(snapshot);
                    // 취소되지 않은 주 버튼의 mousedown 은 오디오 컨트롤 막대를 조작하거나, 끌기의 후보가 되거나, 텍스트 선택을 시작한다.
                    if (notCancelled && pointer.button == MouseEvent.BUTTON1 && current(snapshot)) {
                        boolean shift = (pointer.modifiersEx & MouseEvent.SHIFT_DOWN_MASK) != 0;
                        if (!pressMediaControls(snapshot, target, loc, pointer)) {
                            // 끌 수 있는 요소나 선택한 텍스트 위의 누름은 끌기의 후보이며 텍스트 선택을 시작하지 않는다.
                            if (!dragDrop.pressed(snapshot, loc, pointer, shift)) {
                                selection.pressed(snapshot, page, pointer.docX, pointer.docY, pointer.clickCount, shift);
                            }
                        }
                    }
                } else if ("mouseup".equals(type)) {
                    mediaDrag = null;
                    pressedElement = null;
                    applyElementState(snapshot, overElement, null);
                    if (pointer.button == MouseEvent.BUTTON1) {
                        selection.released();
                    }
                }
                if (withContext && pointer.popup && current(snapshot)) {
                    fire(snapshot, target, pointerClick("contextmenu", pointer, loc, 2, 0));
                }
                // 마지막 버튼을 뗐으면 캡처를 암묵적으로 풀고 호환 마우스 이벤트를 다시 허용한다.
                if (released) {
                    endPointerPress(snapshot, pointer, loc, hitTarget);
                }
            }
        });
    }

    /**
     * A slider of an audio control bar that the user holds with the mouse button down (the seek bar or the volume slider): while it is held,
     * every drag event moves the slider, also outside of the bar.
     * <p>
     * 사용자가 마우스 버튼을 누른 채 잡고 있는 오디오 컨트롤 막대의 슬라이더(탐색 막대 또는 음량 슬라이더). 잡고 있는 동안 끌기 이벤트마다 슬라이더가
     * 움직이며, 막대 밖에서도 그렇다.
     */
    private static final class MediaDrag {
        /** The snapshot of the page at the press. / 누를 때의 페이지 스냅샷. */
        final PageSnapshot snapshot;
        /** The audio element. / 오디오 요소. */
        final Element target;
        /** The slider part (<code>MediaControls.PART_SEEK</code> or <code>PART_VOLUME</code>). / 슬라이더 부분. */
        final int part;
        /** The width of the bar. / 막대의 너비. */
        final double width;
        /** The height of the bar. / 막대의 높이. */
        final double height;
        /** The x of the press inside the bar. / 막대 안에서 누른 곳의 x. */
        final double pressX;
        /** The document x of the pointer at the press. / 누를 때 포인터의 문서 x. */
        final double pointerX;

        /**
         * Creates the record.
         * <p>
         * 기록을 만든다.
         *
         * @param snapshot the page / 페이지
         * @param target   the audio element / 오디오 요소
         * @param part     the slider / 슬라이더
         * @param width    the width of the bar / 막대의 너비
         * @param height   the height of the bar / 막대의 높이
         * @param pressX   the x of the press inside the bar / 막대 안에서 누른 곳의 x
         * @param pointerX the document x of the pointer / 포인터의 문서 x
         */
        MediaDrag(PageSnapshot snapshot, Element target, int part, double width, double height, double pressX, double pointerX) {
            this.snapshot = snapshot;
            this.target = target;
            this.part = part;
            this.width = width;
            this.height = height;
            this.pressX = pressX;
            this.pointerX = pointerX;
        }
    }

    /** The slider that is held now, or null. / 지금 잡고 있는 슬라이더. 없으면 null. */
    private volatile MediaDrag mediaDrag;

    /**
     * Handles a press of the main button on the control bar of an <code>audio</code> element: it finds the part of the bar under the pointer
     * with {@link org.duckdns.hjow.swingwebpanel.render.MediaControls#partAt(double, double, double, double)} and applies the action to the element (play or
     * pause, seek, mute, volume); on a slider it starts holding it, so that dragging goes on moving it. Runs on the document queue.
     * <p>
     * <code>audio</code> 요소의 컨트롤 막대를 주 버튼으로 누른 것을 처리한다: {@link org.duckdns.hjow.swingwebpanel.render.MediaControls#partAt(double, double, double, double)}
     * 로 포인터 아래의 부분을 찾아 요소에 동작(재생이나 일시정지, 탐색, 음소거, 음량)을 적용하고, 슬라이더이면 잡기 시작하여 끌면 계속 움직이게 한다. 문서
     * 큐에서 실행된다.
     *
     * @param snapshot the page / 페이지
     * @param target   the element that was hit / 맞은 요소
     * @param loc      what was hit / 맞은 것
     * @param pointer  the pointer / 포인터
     * @return true if the press belongs to the control bar (so it must not start a text selection) / 누름이 컨트롤 막대의 것이면(텍스트 선택을 시작하면 안 되면) true
     */
    private boolean pressMediaControls(PageSnapshot snapshot, Element target, Located loc, Pointer pointer) {
        LayoutBox box = loc.hit.getBox();
        if (box == null || loc.doc == null || !"audio".equals(target.getLocalName()) || !target.hasAttribute("controls")) {
            return false;
        }
        double w = box.getContentWidth();
        double h = box.getContentHeight();
        double lx = loc.x - box.getContentX();
        double ly = loc.y - box.getContentY();
        // 막대 밖(여백이나 경계선)을 누른 것은 막대의 것이 아니다.
        if (lx < 0 || ly < 0 || lx >= w || ly >= h) {
            return false;
        }
        int part = org.duckdns.hjow.swingwebpanel.render.MediaControls.partAt(w, h, lx, ly);
        double fraction = 0;
        boolean slider = part == org.duckdns.hjow.swingwebpanel.render.MediaControls.PART_SEEK || part == org.duckdns.hjow.swingwebpanel.render.MediaControls.PART_VOLUME;
        // 슬라이더에서는 누른 위치가 값이고, 이후의 끌기가 이어 간다.
        if (slider) {
            fraction = org.duckdns.hjow.swingwebpanel.render.MediaControls.fractionAt(w, h, part, lx);
            mediaDrag = new MediaDrag(snapshot, target, part, w, h, lx, pointer.docX);
        }
        if (part != org.duckdns.hjow.swingwebpanel.render.MediaControls.PART_NONE && part != org.duckdns.hjow.swingwebpanel.render.MediaControls.PART_TIME) {
            loc.doc.controlMedia(target, part, fraction);
        }
        return true;
    }

    /**
     * Moves the slider of an audio control bar that is held, to the pointer of a drag event (on the document queue).
     * <p>
     * 잡고 있는 오디오 컨트롤 막대의 슬라이더를 끌기 이벤트의 포인터로 옮긴다(문서 큐에서).
     *
     * @param event the drag event / 끌기 이벤트
     * @return true if a slider is held (the drag belongs to it) / 슬라이더를 잡고 있으면(끌기가 그것의 것이면) true
     */
    private boolean dragMediaControls(MouseEvent event) {
        final MediaDrag drag = mediaDrag;
        if (drag == null) {
            return false;
        }
        final double x = drag.pressX + (event.getX() / view.getZoom() - drag.pointerX);
        session.submitDocumentTask(new Runnable() {
            /** Applies the new value of the slider. / 슬라이더의 새 값을 적용한다. */
            public void run() {
                // 끌기가 이미 끝났거나 페이지가 바뀌었으면 적용하지 않는다.
                if (mediaDrag != drag || !current(drag.snapshot) || !(drag.snapshot.getDocument() instanceof HtmlPageDocument)) {
                    return;
                }
                double fraction = org.duckdns.hjow.swingwebpanel.render.MediaControls.fractionAt(drag.width, drag.height, drag.part, x);
                ((HtmlPageDocument) drag.snapshot.getDocument()).ownerOf(drag.target).controlMedia(drag.target, drag.part, fraction);
            }
        });
        return true;
    }

    /**
     * Ends the press of the pointer after <code>pointerup</code> and its compatibility events: the PREVENT MOUSE EVENT flag is cleared, the pointer
     * capture is released implicitly (<code>lostpointercapture</code> fires) and, if a capture had held the pointer, the boundary events that bring the
     * pointer from the capture target to the element that is really under it are fired (Pointer Events 9.5).
     * <p>
     * <code>pointerup</code> 과 그 호환 이벤트 뒤에 포인터의 누름을 끝낸다: PREVENT MOUSE EVENT 표시를 지우고, 포인터 캡처를 암묵적으로 풀며(
     * <code>lostpointercapture</code> 발생), 캡처가 포인터를 붙들고 있었으면 포인터를 캡처 대상에서 실제 그 아래의 요소로 옮기는 경계 이벤트를 발생시킨다(Pointer
     * Events 9.5).
     *
     * @param snapshot  the page / 페이지
     * @param p         the pointer / 포인터
     * @param loc       what is under the pointer / 포인터 아래의 것
     * @param hitTarget the element under the pointer or null / 포인터 아래의 요소. 없으면 null
     */
    private void endPointerPress(PageSnapshot snapshot, Pointer p, Located loc, Element hitTarget) {
        preventMouse = false;
        boolean captured = pointers.getOverride() != null;
        pointers.clearPending();
        processPendingCapture(snapshot, p, loc);
        if (captured && current(snapshot)) {
            syncBoundaries(snapshot, p, loc, hitTarget);
        }
    }

    /**
     * Returns the element that the pointer events go to: the capture target override while it holds and is still in the document, otherwise the
     * element that was hit.
     * <p>
     * 포인터 이벤트가 가는 요소를 반환한다: 캡처 대상 재정의가 있고 아직 문서에 있으면 그것, 아니면 맞은 요소.
     *
     * @param hit the element under the pointer or null / 포인터 아래의 요소. 없으면 null
     * @return the target or null / 대상. 없으면 null
     */
    private Element pointerTarget(Element hit) {
        Element captured = pointers.getOverride();
        return captured != null && captured.isConnected() ? captured : hit;
    }

    /**
     * "Process pending pointer capture" (Pointer Events 4.1.3.2): if the pending capture differs from the capture target override,
     * <code>lostpointercapture</code> fires at the old override and <code>gotpointercapture</code> at the new one, and the pending capture becomes the
     * override. A capture target that left the document loses the capture (the event then fires at the document element).
     * <p>
     * "process pending pointer capture"(Pointer Events 4.1.3.2): 대기 중인 캡처가 캡처 대상 재정의와 다르면 이전 재정의에 <code>lostpointercapture</code>,
     * 새 것에 <code>gotpointercapture</code> 를 발생시키고 대기 중인 캡처를 재정의로 올린다. 문서를 떠난 캡처 대상은 캡처를 잃는다(이벤트는 문서 요소에서 발생한다).
     *
     * @param snapshot the page / 페이지
     * @param p        the pointer / 포인터
     * @param loc      what is under the pointer / 포인터 아래의 것
     */
    private void processPendingCapture(PageSnapshot snapshot, Pointer p, Located loc) {
        Element current = pointers.getOverride();
        Element pending = pointers.getPending();
        // 문서에서 떨어진 대기 중인 대상은 쓸 수 없다.
        if (pending != null && !pending.isConnected()) {
            pointers.clearPending();
            pending = null;
        }
        if (current == pending) {
            return;
        }
        if (current != null) {
            Element at = current;
            // 문서를 떠난 이전 대상에는 이벤트가 닿지 않으므로 문서 요소에서 발생시킨다.
            if (!current.isConnected() && loc != null && loc.doc != null) {
                at = loc.doc.getDocument().getDocumentElement();
            }
            // 재정의를 먼저 비워 리스너가 같은 이벤트를 다시 부르는 일을 막는다.
            pointers.promote(null);
            if (at != null) {
                fire(snapshot, at, pointerEvent("lostpointercapture", p, loc, null, -1));
            }
        }
        // lostpointercapture 의 리스너가 대기 중인 캡처를 바꿨을 수 있으므로 다시 읽는다.
        pending = pointers.getPending();
        pointers.promote(pending);
        if (pending != null) {
            fire(snapshot, pending, pointerEvent("gotpointercapture", p, loc, null, -1));
        }
    }

    /**
     * Builds a pointer event of the mouse pointer (the bubbling and cancelable flags of the table of Pointer Events 4.1.3.1, the coordinates
     * and buttons of the pointer, pressure 0.5 while a button is down and 0 for <code>pointerup</code>).
     * <p>
     * 마우스 포인터의 포인터 이벤트를 만든다(Pointer Events 4.1.3.1 표의 버블링·취소 가능 표시, 포인터의 좌표와 버튼, 버튼이 눌려 있는 동안 압력 0.5,
     * <code>pointerup</code> 은 0).
     *
     * @param type    the event type / 이벤트 종류
     * @param p       the pointer / 포인터
     * @param loc     what is under the pointer / 포인터 아래의 것
     * @param related the element that the pointer came from or goes to (boundary events), or null / 포인터가 온 곳이나 가는 곳의 요소(경계 이벤트). 없으면 null
     * @param button  the DOM button that changed, or -1 if no button changed / 바뀐 DOM 버튼. 바뀐 버튼이 없으면 -1
     * @return the event / 이벤트
     */
    private org.duckdns.hjow.swingwebpanel.dom.PointerEvent pointerEvent(String type, Pointer p, Located loc, Element related, int button) {
        boolean boundary = "pointerenter".equals(type) || "pointerleave".equals(type);
        boolean cancelable = !boundary && !"pointercancel".equals(type) && !"gotpointercapture".equals(type) && !"lostpointercapture".equals(type);
        org.duckdns.hjow.swingwebpanel.dom.PointerEvent e = org.duckdns.hjow.swingwebpanel.dom.PointerEvent.forMouse(type, !boundary, cancelable);
        fillMouse(e, p, loc, related, button, 0);
        e.setPressure(!"pointerup".equals(type) && p.buttons() != 0 ? 0.5 : 0);
        e.setComposed(!boundary);
        return e;
    }

    /**
     * Sends the pointer boundary events that bring the pointer to a target: <code>pointerout</code> and <code>pointerleave</code> (from the inside out) for the
     * element that the pointer was over and its ancestors that do not contain the new target, then <code>pointerover</code> and <code>pointerenter</code> (from
     * the outside in) for the new target and its ancestors that did not contain the old element.
     * <p>
     * 포인터를 대상으로 옮기는 포인터 경계 이벤트를 보낸다: 포인터가 있던 요소와 새 대상을 포함하지 않는 그 조상에 (안쪽부터) <code>pointerout</code>,
     * <code>pointerleave</code>, 새 대상과 이전 요소를 포함하지 않던 그 조상에 (바깥쪽부터) <code>pointerover</code>, <code>pointerenter</code>.
     *
     * @param snapshot the page / 페이지
     * @param p        the pointer / 포인터
     * @param loc      what is under the pointer / 포인터 아래의 것
     * @param target   the element that the pointer events now go to, or null / 포인터 이벤트가 이제 가는 요소. 없으면 null
     */
    private void pointerBoundary(PageSnapshot snapshot, Pointer p, Located loc, Element target) {
        if (target == pointerOver && pointerOverSnapshot == snapshot) {
            return;
        }
        Element old = pointerOverSnapshot == snapshot ? pointerOver : null;
        if (old != null) {
            fire(snapshot, old, pointerEvent("pointerout", p, loc, target, -1));
            // 새 대상을 포함하지 않는 이전 요소와 그 조상에 안쪽부터 pointerleave 를 보낸다(버블링 없음).
            for (org.duckdns.hjow.swingwebpanel.dom.Node n = old; n instanceof Element && (target == null || !n.contains(target)); n = n.getParentNode()) {
                fire(snapshot, (Element) n, pointerEvent("pointerleave", p, loc, target, -1));
            }
        }
        pointerOver = target;
        pointerOverSnapshot = snapshot;
        if (target != null) {
            fire(snapshot, target, pointerEvent("pointerover", p, loc, old, -1));
            java.util.ArrayList<Element> entered = new java.util.ArrayList<Element>();
            // 이전 요소를 포함하지 않는 새 대상과 그 조상을 모은 뒤 바깥쪽부터 pointerenter 를 보낸다(버블링 없음).
            for (org.duckdns.hjow.swingwebpanel.dom.Node n = target; n instanceof Element && (old == null || !n.contains(old)); n = n.getParentNode()) {
                entered.add((Element) n);
            }
            // 모은 요소를 바깥쪽(끝)부터 안쪽으로 pointerenter 를 보낸다.
            for (int i = entered.size() - 1; i >= 0; i--) {
                fire(snapshot, entered.get(i), pointerEvent("pointerenter", p, loc, old, -1));
            }
        }
    }

    /**
     * Brings both kinds of boundary events (pointer and compatibility mouse events) in line with the element that is really under the pointer, with
     * no move event (after a capture ended, Pointer Events 9.5).
     * <p>
     * 두 종류의 경계 이벤트(포인터와 호환 마우스 이벤트)를 포인터 아래의 실제 요소에 맞춘다. 이동 이벤트는 보내지 않는다(캡처가 끝난 뒤, Pointer Events 9.5).
     *
     * @param snapshot  the page / 페이지
     * @param p         the pointer / 포인터
     * @param loc       what is under the pointer / 포인터 아래의 것
     * @param hitTarget the element under the pointer or null / 포인터 아래의 요소. 없으면 null
     */
    private void syncBoundaries(PageSnapshot snapshot, Pointer p, Located loc, Element hitTarget) {
        Element target = pointerTarget(hitTarget);
        pointerBoundary(snapshot, p, loc, target);
        mouseBoundary(snapshot, p, loc, target);
    }

    /**
     * Ends the event stream of the pointer when a drag begins (Pointer Events 4.1.3.3): <code>pointercancel</code>, <code>pointerout</code> and
     * <code>pointerleave</code> fire, the capture is released and the compatibility mouse events stop until a button goes down again. Call it on the
     * document queue.
     * <p>
     * 끌기가 시작될 때 포인터의 이벤트 스트림을 끝낸다(Pointer Events 4.1.3.3): <code>pointercancel</code>, <code>pointerout</code>, <code>pointerleave</code> 가
     * 발생하고, 캡처를 풀며, 버튼이 다시 눌릴 때까지 호환 마우스 이벤트를 멈춘다. 문서 큐에서 호출한다.
     *
     * @param snapshot the page / 페이지
     * @param p        the pointer that caused the drag / 끌기를 일으킨 포인터
     * @param loc      what is under the pointer / 포인터 아래의 것
     */
    void pointerCancelled(PageSnapshot snapshot, Pointer p, Located loc) {
        // 포인터 이벤트를 보내기 전에 대기 중인 캡처를 처리한다(gotpointercapture). 그래야 pointerdown 에서 잡은 캡처가 pointercancel 과 lostpointercapture 로 끝난다.
        processPendingCapture(snapshot, p, loc);
        Element target = pointerOverSnapshot == snapshot ? pointerOver : null;
        if (target != null) {
            fire(snapshot, pointerTarget(target), pointerEvent("pointercancel", p, loc, null, -1));
            pointerBoundary(snapshot, p, loc, null);
        }
        pointers.clearPending();
        processPendingCapture(snapshot, p, loc);
        downButtons = 0;
        pointers.setButtons(0);
        preventMouse = false;
    }

    /**
     * Sends the events of a pointer move on the document queue: the pending capture is processed (<code>lostpointercapture</code>,
     * <code>gotpointercapture</code>), then, when the element under the pointer changed, the pointer boundary events (<code>pointerout</code>,
     * <code>pointerleave</code>, <code>pointerover</code>, <code>pointerenter</code>) and the mouse boundary events (<code>mouseout</code>, <code>mouseleave</code>,
     * <code>mouseover</code>, <code>mouseenter</code>), then <code>pointermove</code> and <code>mousemove</code>. A capture target stands in for the element under
     * the pointer.
     * <p>
     * 포인터 이동의 이벤트를 문서 큐에서 보낸다: 대기 중인 캡처를 처리하고(<code>lostpointercapture</code>, <code>gotpointercapture</code>), 포인터 아래 요소가 바뀌었으면
     * 포인터 경계 이벤트(<code>pointerout</code>, <code>pointerleave</code>, <code>pointerover</code>, <code>pointerenter</code>)와 마우스 경계
     * 이벤트(<code>mouseout</code>, <code>mouseleave</code>, <code>mouseover</code>, <code>mouseenter</code>), 그리고 <code>pointermove</code> 와
     * <code>mousemove</code>. 캡처 대상이 포인터 아래의 요소를 대신한다.
     *
     * @param snapshot the page / 페이지
     * @param hit      what is under the pointer / 포인터 아래의 것
     */
    private void moveDomPointer(PageSnapshot snapshot, Located hit) {
        Pointer p = lastPointer;
        // 프레임의 스크롤바 위에서는 문서의 요소가 포인터 아래에 있지 않은 것으로 본다.
        Element hitTarget = hit.bar != FrameContext.BAR_NONE ? null : hit.hit.getElement();
        if (p == null) {
            return;
        }
        // 포인터 이벤트가 마우스 이벤트보다 먼저 간다(Pointer Events 11.2). 캡처가 있으면 그 대상이 포인터 아래의 요소처럼 취급된다.
        pointers.setActiveDocument(hit.doc == null ? null : hit.doc.getDocument());
        processPendingCapture(snapshot, p, hit);
        Element target = pointerTarget(hitTarget);
        pointerBoundary(snapshot, p, hit, target);
        mouseBoundary(snapshot, p, hit, target);
        if (target != null) {
            fire(snapshot, target, pointerEvent("pointermove", p, hit, null, -1));
            // 취소된 pointerdown 이 막은 동안에는 mousemove 를 보내지 않는다.
            if (!preventMouse) {
                fire(snapshot, target, mouseEvent("mousemove", p, hit, null, true, true, 0, 0));
            }
        }
    }

    /**
     * Sends the compatibility mouse boundary events that bring the legacy mouse pointer to a target: <code>mouseout</code> and <code>mouseleave</code> for
     * the element that it was over, <code>mouseover</code> and <code>mouseenter</code> for the new one, and the new state of <code>:hover</code>.
     * <p>
     * 호환 마우스 포인터를 대상으로 옮기는 마우스 경계 이벤트를 보낸다: 있던 요소에 <code>mouseout</code>, <code>mouseleave</code>, 새 요소에
     * <code>mouseover</code>, <code>mouseenter</code>, 그리고 <code>:hover</code> 의 새 상태.
     *
     * @param snapshot the page / 페이지
     * @param hit      what is under the pointer / 포인터 아래의 것
     * @param p        the pointer / 포인터
     * @param target   the element that the events go to, or null / 이벤트가 가는 요소. 없으면 null
     */
    private void mouseBoundary(PageSnapshot snapshot, Pointer p, Located hit, Element target) {
        if (target != overElement) {
            Element old = overSnapshot == snapshot ? overElement : null;
            if (old != null) {
                fire(snapshot, old, mouseEvent("mouseout", p, hit, target, true, true, 0, 0));
                // 새 요소를 포함하지 않는 이전 요소와 그 조상에 안쪽부터 mouseleave 를 보낸다(버블링 없음).
                for (org.duckdns.hjow.swingwebpanel.dom.Node n = old; n instanceof Element && (target == null || !n.contains(target)); n = n.getParentNode()) {
                    fire(snapshot, (Element) n, mouseEvent("mouseleave", p, hit, target, false, false, 0, 0));
                }
            }
            overElement = target;
            overSnapshot = snapshot;
            applyElementState(snapshot, target, pressedElement);
            if (target != null) {
                fire(snapshot, target, mouseEvent("mouseover", p, hit, old, true, true, 0, 0));
                java.util.ArrayList<Element> entered = new java.util.ArrayList<Element>();
                // 이전 요소를 포함하지 않는 새 요소와 그 조상을 모은 뒤 바깥쪽부터 mouseenter 를 보낸다(버블링 없음).
                for (org.duckdns.hjow.swingwebpanel.dom.Node n = target; n instanceof Element && (old == null || !n.contains(old)); n = n.getParentNode()) {
                    entered.add((Element) n);
                }
                // 모은 요소를 바깥쪽(끝)부터 안쪽으로 mouseenter 를 보낸다.
                for (int i = entered.size() - 1; i >= 0; i--) {
                    fire(snapshot, entered.get(i), mouseEvent("mouseenter", p, hit, old, false, false, 0, 0));
                }
            }
        }
    }

    /**
     * Handles a press or release on an overlay scrollbar of a frame: pressing the thumb starts dragging it, pressing the track beside the thumb
     * scrolls the frame by one page towards the pointer, releasing ends the dragging.
     * <p>
     * 프레임의 오버레이 스크롤바 위에서 누르거나 뗄 때 처리한다: 썸을 누르면 끌기를 시작하고, 썸 옆의 트랙을 누르면 포인터 쪽으로 한 쪽 스크롤하고, 떼면 끌기가
     * 끝난다.
     *
     * @param type    <code>mousedown</code> or <code>mouseup</code> / <code>mousedown</code> 또는 <code>mouseup</code>
     * @param pointer the pointer / 포인터
     * @param loc     what was hit / 맞은 것
     */
    private void scrollbarPress(String type, Pointer pointer, Located loc) {
        if ("mouseup".equals(type)) {
            dragFrame = null;
            return;
        }
        FrameContext frame = loc.frame;
        boolean vertical = loc.bar == FrameContext.BAR_VERTICAL_THUMB || loc.bar == FrameContext.BAR_VERTICAL_TRACK;
        if (loc.bar == FrameContext.BAR_VERTICAL_THUMB || loc.bar == FrameContext.BAR_HORIZONTAL_THUMB) {
            dragVertical = vertical;
            dragStart = vertical ? pointer.y : pointer.x;
            dragThumb = frame.thumbStart(vertical);
            dragFrame = frame;
            return;
        }
        LayoutBox box = loc.hit.getBox();
        double along = box == null ? 0 : vertical ? loc.y - box.getContentY() : loc.x - box.getContentX();
        int direction = along < frame.thumbStart(vertical) ? -1 : 1;
        // 트랙을 누르면 포인터 쪽으로 뷰의 90 % 만큼 스크롤한다.
        if (vertical) {
            frame.scrollBy(0, direction * (int) (frame.viewportHeight() * 0.9));
        } else {
            frame.scrollBy(direction * (int) (frame.viewportWidth() * 0.9), 0);
        }
    }

    /**
     * Queues the events for a pointer that left the page (or a page that was replaced): <code>mouseout</code> and <code>mouseleave</code>
     * for the element that was under the pointer, only if that page is still the current one.
     * <p>
     * 페이지를 떠난 포인터(또는 교체된 페이지)의 이벤트를 예약한다: 포인터 아래에 있던 요소에 <code>mouseout</code> 과 <code>mouseleave</code> 를
     * 보내되, 그 페이지가 아직 현재일 때만 보낸다.
     */
    private void leaveDomPointer() {
        session.submitDocumentTask(new Runnable() {
            /** Sends the leaving events on the document queue. / 문서 큐에서 나가는 이벤트를 보낸다. */
            public void run() {
                Element old = overElement;
                PageSnapshot snapshot = overSnapshot;
                overElement = null;
                overSnapshot = null;
                if (snapshot != null && current(snapshot)) {
                    applyElementState(snapshot, null, pressedElement);
                }
                Pointer p = lastPointer;
                // 포인터 이벤트의 경계 상태도 떠난다: pointerout 과 pointerleave 가 mouseout 과 mouseleave 보다 먼저 간다. 캡처와 버튼 상태는 잊는다.
                Element pointerOld = pointerOver;
                PageSnapshot pointerSnapshot = pointerOverSnapshot;
                pointerOver = null;
                pointerOverSnapshot = null;
                pointers.reset();
                downButtons = 0;
                preventMouse = false;
                if (pointerOld != null && pointerSnapshot != null && p != null && current(pointerSnapshot)) {
                    fire(pointerSnapshot, pointerOld, pointerEvent("pointerout", p, null, null, -1));
                    // 포인터 아래에 있던 요소와 그 조상 모두에 안쪽부터 pointerleave 를 보낸다(버블링 없음).
                    for (org.duckdns.hjow.swingwebpanel.dom.Node n = pointerOld; n instanceof Element; n = n.getParentNode()) {
                        fire(pointerSnapshot, (Element) n, pointerEvent("pointerleave", p, null, null, -1));
                    }
                }
                if (old == null || snapshot == null || p == null || !current(snapshot)) {
                    return;
                }
                fire(snapshot, old, mouseEvent("mouseout", p, null, null, true, true, 0, 0));
                // 포인터 아래에 있던 요소와 그 조상 모두에 안쪽부터 mouseleave 를 보낸다(버블링 없음).
                for (org.duckdns.hjow.swingwebpanel.dom.Node n = old; n instanceof Element; n = n.getParentNode()) {
                    fire(snapshot, (Element) n, mouseEvent("mouseleave", p, null, null, false, false, 0, 0));
                }
            }
        });
    }
}
