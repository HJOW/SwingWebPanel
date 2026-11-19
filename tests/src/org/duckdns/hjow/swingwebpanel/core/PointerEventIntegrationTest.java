package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.PointerEvent;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests the pointer events that real AWT mouse events cause (W3C Pointer Events Level 3, fetched 2026-11-17, see
 * <code>docs/progress/webapi2.md</code>): the order of the pointer events and the compatibility mouse events, the boundary events, the properties of the events
 * of the mouse pointer, the suppression of the mouse events by a cancelled <code>pointerdown</code>, and the pointer capture with its
 * <code>gotpointercapture</code> and <code>lostpointercapture</code> events. The page is a fixed layout; a capture listener on the document logs every event.
 * <p>
 * 실제 AWT 마우스 이벤트가 일으키는 포인터 이벤트를 시험한다(W3C Pointer Events Level 3, 2026-11-17 조회, <code>docs/progress/webapi2.md</code> 참조):
 * 포인터 이벤트와 호환 마우스 이벤트의 순서, 경계 이벤트, 마우스 포인터 이벤트의 속성, 취소된 <code>pointerdown</code> 의 마우스 이벤트 억제, 포인터 캡처와
 * <code>gotpointercapture</code>, <code>lostpointercapture</code> 이벤트. 페이지는 고정 레이아웃이고 문서의 캡처 리스너가 모든 이벤트를 기록한다.
 */
public class PointerEventIntegrationTest {

    /**
     * A page with three boxes: <code>inner</code> and <code>other</code> in <code>outer</code>, and <code>away</code> below it.
     * <p>
     * 상자 셋이 있는 페이지: <code>outer</code> 안의 <code>inner</code> 와 <code>other</code>, 그 아래의 <code>away</code>.
     */
    private static final String PAGE = "<!doctype html><style>html,body{margin:0}</style><div id=outer style='width:200px;height:100px'>"
            + "<div id=inner style='width:100px;height:40px'>x</div><div id=other style='width:100px;height:40px'>y</div></div>"
            + "<div id=away style='width:200px;height:50px'>z</div>";

    /**
     * Creates the test (the runner needs a public constructor).
     * <p>
     * 시험을 만든다(실행기는 public 생성자가 필요하다).
     */
    public PointerEventIntegrationTest() {
    }

    /**
     * A component with a session, a view and the input handler, and the log of the events.
     * <p>
     * 세션, 뷰, 입력 처리기가 있는 컴포넌트와 이벤트 기록.
     */
    private static final class Fixture implements PageView.Host {
        /**
         * The component that receives the AWT events.
         * <p>
         * AWT 이벤트를 받는 컴포넌트.
         */
        final JPanel component = new JPanel();

        /**
         * The loader that parses the page.
         * <p>
         * 페이지를 해석하는 로더.
         */
        final FakeLoader loader = new FakeLoader();

        /**
         * The session.
         * <p>
         * 세션.
         */
        final BrowserSession session = new BrowserSession(new ParsingPageLoader(loader), null);

        /**
         * The user link notifications (none here).
         * <p>
         * 사용자 링크 통지(여기서는 없음).
         */
        final HyperlinkDispatcher links = new HyperlinkDispatcher();

        /**
         * The view that renders the page and finds the elements under the pointer.
         * <p>
         * 페이지를 그리고 포인터 아래의 요소를 찾는 뷰.
         */
        final PageView view = new PageView(session, new ViewMetrics(), this);

        /**
         * The input handler under test.
         * <p>
         * 시험 대상 입력 처리기.
         */
        final PageInput input = new PageInput(component, session, view, links);

        /**
         * The events in the order of their dispatch.
         * <p>
         * 전달된 순서의 이벤트.
         */
        final List<String> log = Collections.synchronizedList(new ArrayList<String>());

        /**
         * The elements by id, for the listeners and for the capture.
         * <p>
         * 리스너와 캡처용 id 별 요소.
         */
        Document document;

        /**
         * Connects the input handler to the component.
         * <p>
         * 입력 처리기를 컴포넌트에 연결한다.
         */
        Fixture() {
            component.addMouseListener(input);
            component.addMouseMotionListener(input);
        }

        /**
         * Does nothing (the view needs no repaint here).
         * <p>
         * 아무것도 하지 않는다(여기서는 뷰를 다시 그릴 필요가 없다).
         */
        public void viewChanged() {
        }

        /**
         * Loads the page and logs every pointer and mouse event.
         * <p>
         * 페이지를 불러오고 모든 포인터·마우스 이벤트를 기록한다.
         *
         * @throws Exception on failure / 실패 시
         */
        void open() throws Exception {
            session.setContent(PAGE, "text/html");
            TestSupport.settle(session);
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    view.pageConfirmed(session.getSnapshot());
                }
            });
            TestSupport.settle(session);
            session.submitDocumentTask(new Runnable() {
                public void run() {
                    document = ((HtmlPageDocument) session.getSnapshot().getDocument()).getDocument();
                    String[] types = { "pointerover", "pointerenter", "pointerdown", "pointermove", "pointerup", "pointercancel", "pointerout", "pointerleave",
                        "gotpointercapture", "lostpointercapture", "mouseover", "mouseenter", "mouseout", "mouseleave", "mousemove", "mousedown", "mouseup",
                        "click", "auxclick", "contextmenu" };
                    // 모든 종류에 캡처 리스너를 달아 버블링하지 않는 이벤트도 기록한다.
                    for (int i = 0; i < types.length; i++) {
                        document.addEventListener(types[i], new EventListener() {
                            public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                                log.add(describe(event));
                            }
                        }, true);
                    }
                }
            });
            TestSupport.settle(session);
            log.clear();
        }

        /**
         * Describes an event on one line: type, target (and related target), the button and buttons, and for a pointer event the pointer and the pressure.
         * <p>
         * 이벤트를 한 줄로 설명한다: 종류, 대상(과 상대 대상), 버튼, 그리고 포인터 이벤트이면 포인터와 압력.
         *
         * @param event the event / 이벤트
         * @return the line / 줄
         */
        String describe(org.duckdns.hjow.swingwebpanel.dom.Event event) {
            org.duckdns.hjow.swingwebpanel.dom.MouseEvent m = (org.duckdns.hjow.swingwebpanel.dom.MouseEvent) event;
            StringBuilder sb = new StringBuilder(event.getType()).append(':').append(id(m.getTarget()));
            if (m.getRelatedTarget() != null) {
                sb.append("<>").append(id(m.getRelatedTarget()));
            }
            sb.append(" b").append(m.getButton()).append(" bs").append(m.getButtons());
            if (m instanceof PointerEvent) {
                PointerEvent p = (PointerEvent) m;
                // 마우스 포인터(ID 1, 주 포인터)가 아니면 표시하고, 압력이 있으면 적으며, 버블링과 취소 가능 여부가 기본이 아니면 nb/nc 로 적는다.
                if (p.getPointerId() != 1 || !"mouse".equals(p.getPointerType()) || !p.isPrimary()) {
                    sb.append(" NOT-THE-MOUSE");
                }
                if (p.getPressure() > 0) {
                    sb.append(" p").append(p.getPressure());
                }
                if (!event.isBubbles()) {
                    sb.append(" nb");
                } else if (!event.isCancelable()) {
                    sb.append(" nc");
                }
            } else {
                sb.append(" d").append(m.getDetail());
            }
            return sb.toString();
        }

        /**
         * Returns the id of an element, its name otherwise.
         * <p>
         * 요소의 id 를 반환한다. 없으면 이름.
         *
         * @param target the target / 대상
         * @return the text / 텍스트
         */
        private String id(org.duckdns.hjow.swingwebpanel.dom.EventTarget target) {
            if (target instanceof Element) {
                String id = ((Element) target).getAttribute("id");
                return id == null || id.length() == 0 ? ((Element) target).getLocalName() : id;
            }
            return String.valueOf(target);
        }

        /**
         * Dispatches an AWT mouse event on the event dispatch thread and waits for the document queue to deliver its DOM events.
         * <p>
         * 이벤트 디스패치 스레드에서 AWT 마우스 이벤트를 전달하고 문서 큐가 DOM 이벤트를 전달할 때까지 기다린다.
         *
         * @param id         the AWT event id / AWT 이벤트 id
         * @param modifiers  the modifiers / 보조 값
         * @param x          x / x
         * @param y          y / y
         * @param clickCount the click count / 클릭 횟수
         * @param popup      true for a pop-up trigger / 팝업 트리거이면 true
         * @param button     the AWT button / AWT 버튼
         * @throws Exception on failure / 실패 시
         */
        void send(final int id, final int modifiers, final int x, final int y, final int clickCount, final boolean popup, final int button)
                throws Exception {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    component.dispatchEvent(new MouseEvent(component, id, System.currentTimeMillis(), modifiers, x, y, clickCount, popup, button));
                }
            });
            TestSupport.settle(session);
            TestSupport.settle(session);
        }

        /**
         * Returns the element with an id (call it only after {@link #open()}).
         * <p>
         * id 로 요소를 반환한다({@link #open()} 뒤에만 부른다).
         *
         * @param id the id / id
         * @return the element / 요소
         */
        Element element(String id) {
            return document.getElementById(id);
        }

        /**
         * Returns the logged lines and clears the log.
         * <p>
         * 기록된 줄을 반환하고 기록을 비운다.
         *
         * @return the lines joined with newlines / 줄바꿈으로 이은 줄
         */
        String take() {
            StringBuilder sb = new StringBuilder();
            synchronized (log) {
                // 기록된 줄을 순서대로 이어 붙인다.
                for (String line : log) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
                log.clear();
            }
            return sb.toString();
        }

        /**
         * Releases the session and the view.
         * <p>
         * 세션과 뷰를 해제한다.
         */
        void close() {
            input.dispose();
            view.dispose();
            session.awaitIdle(10000);
            session.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * Entering the page and moving between elements: the pointer boundary events come first, the compatibility mouse boundary events follow, then
     * <code>pointermove</code> and <code>mousemove</code>; the pointer is the primary mouse pointer with the id 1.
     * <p>
     * 페이지로 들어오고 요소 사이를 움직일 때: 포인터 경계 이벤트가 먼저, 호환 마우스 경계 이벤트가 그다음, 그리고 <code>pointermove</code> 와
     * <code>mousemove</code>. 포인터는 ID 1 인 주 마우스 포인터이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBoundaryAndMoveOrder() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointerover:inner b-1 bs0\npointerenter:html b-1 bs0 nb\npointerenter:body b-1 bs0 nb\npointerenter:outer b-1 bs0 nb\n"
                    + "pointerenter:inner b-1 bs0 nb\nmouseover:inner b0 bs0 d0\nmouseenter:html b0 bs0 d0\nmouseenter:body b0 bs0 d0\n"
                    + "mouseenter:outer b0 bs0 d0\nmouseenter:inner b0 bs0 d0\npointermove:inner b-1 bs0\nmousemove:inner b0 bs0 d0", f.take(), "entering the page");
            f.send(MouseEvent.MOUSE_MOVED, 0, 20, 50, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointerout:inner<>other b-1 bs0\npointerleave:inner<>other b-1 bs0 nb\npointerover:other<>inner b-1 bs0\n"
                    + "pointerenter:other<>inner b-1 bs0 nb\nmouseout:inner<>other b0 bs0 d0\nmouseleave:inner<>other b0 bs0 d0\n"
                    + "mouseover:other<>inner b0 bs0 d0\nmouseenter:other<>inner b0 bs0 d0\npointermove:other b-1 bs0\nmousemove:other b0 bs0 d0", f.take(),
                    "moving to the sibling");
            f.send(MouseEvent.MOUSE_MOVED, 0, 25, 55, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointermove:other b-1 bs0\nmousemove:other b0 bs0 d0", f.take(), "moving inside the same element");
            f.send(MouseEvent.MOUSE_EXITED, 0, 25, 55, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointerout:other b-1 bs0\npointerleave:other b-1 bs0 nb\npointerleave:outer b-1 bs0 nb\npointerleave:body b-1 bs0 nb\n"
                    + "pointerleave:html b-1 bs0 nb\nmouseout:other b0 bs0 d0\nmouseleave:other b0 bs0 d0\nmouseleave:outer b0 bs0 d0\n"
                    + "mouseleave:body b0 bs0 d0\nmouseleave:html b0 bs0 d0", f.take(), "leaving the component");
            Check.isFalse(f.session.getPointers().isActive(1), "the pointer is not active outside the component");
        } finally {
            f.close();
        }
    }

    /**
     * A press and a release: <code>pointerdown</code> and <code>pointerup</code> come before <code>mousedown</code> and <code>mouseup</code>; the pressure is 0.5 while a
     * button is down and 0 for <code>pointerup</code>; a second button is a <code>pointermove</code> with the button that changed (chorded buttons); the click is
     * a <code>PointerEvent</code> of the same pointer.
     * <p>
     * 누름과 뗌: <code>pointerdown</code> 과 <code>pointerup</code> 이 <code>mousedown</code> 과 <code>mouseup</code> 보다 먼저 온다. 버튼이 눌려 있는 동안 압력은 0.5,
     * <code>pointerup</code> 은 0. 두 번째 버튼은 바뀐 버튼을 가진 <code>pointermove</code>(동시 버튼). click 은 같은 포인터의 <code>PointerEvent</code> 이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPressAndRelease() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            f.take();
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerdown:inner b0 bs1 p0.5\nmousedown:inner b0 bs1 d1", f.take(), "press");
            Check.isTrue(f.session.getPointers().isActive(1), "the pointer is active");
            f.send(MouseEvent.MOUSE_RELEASED, 0, 10, 10, 1, false, MouseEvent.BUTTON1);
            f.send(MouseEvent.MOUSE_CLICKED, 0, 10, 10, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerup:inner b0 bs0\nmouseup:inner b0 bs0 d1\nclick:inner b0 bs0", f.take(), "release and click (a PointerEvent)");
            // 오른쪽 버튼: 팝업 트리거이면 contextmenu 도 PointerEvent 이다.
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON3_DOWN_MASK, 10, 10, 1, true, MouseEvent.BUTTON3);
            Check.equal("pointerdown:inner b2 bs2 p0.5\nmousedown:inner b2 bs2 d1\ncontextmenu:inner b2 bs2", f.take(), "secondary button");
            f.send(MouseEvent.MOUSE_RELEASED, 0, 10, 10, 1, false, MouseEvent.BUTTON3);
            f.send(MouseEvent.MOUSE_CLICKED, 0, 10, 10, 1, false, MouseEvent.BUTTON3);
            Check.equal("pointerup:inner b2 bs0\nmouseup:inner b2 bs0 d1\nauxclick:inner b2 bs0", f.take(), "release of the secondary button");
            // 동시 버튼: 첫 버튼만 pointerdown 이고 두 번째는 바뀐 버튼을 가진 pointermove 이다. 마지막 버튼을 뗄 때 pointerup 이 온다.
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1);
            f.take();
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON3);
            Check.equal("pointermove:inner b2 bs3 p0.5\nmousedown:inner b2 bs3 d1", f.take(), "a second button while one is down");
            f.send(MouseEvent.MOUSE_RELEASED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON3);
            Check.equal("pointermove:inner b2 bs1 p0.5\nmouseup:inner b2 bs1 d1", f.take(), "releasing one of two buttons");
            f.send(MouseEvent.MOUSE_RELEASED, 0, 10, 10, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerup:inner b0 bs0\nmouseup:inner b0 bs0 d1", f.take(), "the last button");
        } finally {
            f.close();
        }
    }

    /**
     * A cancelled <code>pointerdown</code> suppresses <code>mousedown</code>, <code>mousemove</code> and <code>mouseup</code> until the buttons are up, but not the mouse
     * boundary events and not <code>click</code>; the next press works again.
     * <p>
     * 취소된 <code>pointerdown</code> 은 버튼을 뗄 때까지 <code>mousedown</code>, <code>mousemove</code>, <code>mouseup</code> 을 막지만 마우스 경계 이벤트와
     * <code>click</code> 은 막지 않는다. 다음 누름은 다시 동작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCancelledPointerDownSuppressesMouseEvents() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.element("inner").addEventListener("pointerdown", new EventListener() {
                public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                    event.preventDefault();
                }
            });
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            f.take();
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerdown:inner b0 bs1 p0.5", f.take(), "the cancelled pointerdown: no mousedown");
            f.send(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 20, 50, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointerout:inner<>other b-1 bs1 p0.5\npointerleave:inner<>other b-1 bs1 p0.5 nb\npointerover:other<>inner b-1 bs1 p0.5\n"
                    + "pointerenter:other<>inner b-1 bs1 p0.5 nb\nmouseout:inner<>other b0 bs1 d0\nmouseleave:inner<>other b0 bs1 d0\n"
                    + "mouseover:other<>inner b0 bs1 d0\nmouseenter:other<>inner b0 bs1 d0\npointermove:other b-1 bs1 p0.5", f.take(),
                    "the boundary events and pointermove stay, mousemove does not");
            f.send(MouseEvent.MOUSE_RELEASED, 0, 20, 50, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerup:other b0 bs0", f.take(), "no mouseup");
            f.send(MouseEvent.MOUSE_CLICKED, 0, 20, 50, 1, false, MouseEvent.BUTTON1);
            Check.equal("click:other b0 bs0", f.take(), "click still fires");
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 20, 50, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerdown:other b0 bs1 p0.5\nmousedown:other b0 bs1 d1", f.take(), "the flag was cleared by the release: mousedown fires again");
            f.send(MouseEvent.MOUSE_RELEASED, 0, 20, 50, 1, false, MouseEvent.BUTTON1);
        } finally {
            f.close();
        }
    }

    /**
     * Pointer capture: a capture that <code>pointerdown</code> asks for starts with the next pointer event (<code>gotpointercapture</code>), retargets the pointer
     * events and the compatibility mouse events to the capturing element, and ends after <code>pointerup</code> (<code>lostpointercapture</code>), when the boundary
     * events bring the pointer to the element that is really under it.
     * <p>
     * 포인터 캡처: <code>pointerdown</code> 이 요청한 캡처는 다음 포인터 이벤트와 함께 시작하고(<code>gotpointercapture</code>), 포인터 이벤트와 호환 마우스 이벤트를
     * 캡처한 요소로 돌리며, <code>pointerup</code> 뒤에 끝나고(<code>lostpointercapture</code>) 그때 경계 이벤트가 포인터를 실제 그 아래의 요소로 옮긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPointerCapture() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open();
            f.element("inner").addEventListener("pointerdown", new EventListener() {
                public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                    f.session.getPointers().setCapture(1, f.element("inner"));
                    f.log.add("has:" + f.session.getPointers().hasCapture(1, f.element("inner")) + " other:"
                            + f.session.getPointers().hasCapture(1, f.element("other")));
                }
            });
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            f.take();
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerdown:inner b0 bs1 p0.5\nhas:true other:false\nmousedown:inner b0 bs1 d1", f.take(),
                    "hasPointerCapture is true at once; the capture is not announced yet");
            f.send(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 20, 50, 0, false, MouseEvent.NOBUTTON);
            Check.equal("gotpointercapture:inner b-1 bs1 p0.5 nc\npointermove:inner b-1 bs1 p0.5\nmousemove:inner b0 bs1 d0", f.take(),
                    "the next pointer event announces the capture and goes to the capturing element, although the pointer is over another element");
            f.send(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 25, 55, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointermove:inner b-1 bs1 p0.5\nmousemove:inner b0 bs1 d0", f.take(), "and so does every later event");
            f.send(MouseEvent.MOUSE_RELEASED, 0, 25, 55, 1, false, MouseEvent.BUTTON1);
            Check.equal("pointerup:inner b0 bs0\nmouseup:inner b0 bs0 d1\nlostpointercapture:inner b-1 bs0 nc\n"
                    + "pointerout:inner<>other b-1 bs0\npointerleave:inner<>other b-1 bs0 nb\npointerover:other<>inner b-1 bs0\n"
                    + "pointerenter:other<>inner b-1 bs0 nb\nmouseout:inner<>other b0 bs0 d0\nmouseleave:inner<>other b0 bs0 d0\n"
                    + "mouseover:other<>inner b0 bs0 d0\nmouseenter:other<>inner b0 bs0 d0", f.take(),
                    "pointerup goes to the capturing element; the capture is released after it; then the boundary events reach the element under the pointer");
            Check.isFalse(f.session.getPointers().hasCapture(1, f.element("inner")), "no capture is left");
            f.send(MouseEvent.MOUSE_MOVED, 0, 26, 56, 0, false, MouseEvent.NOBUTTON);
            Check.equal("pointermove:other b-1 bs0\nmousemove:other b0 bs0 d0", f.take(), "the next move follows the element under the pointer again");
        } finally {
            f.close();
        }
    }

    /**
     * A capture that is released before the next pointer event is never announced, and the capture methods ignore a pointer without a button down.
     * <p>
     * 다음 포인터 이벤트 전에 풀린 캡처는 알려지지 않으며, 캡처 메서드는 버튼이 눌리지 않은 포인터를 무시한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReleasedAndRejectedCapture() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open();
            f.element("inner").addEventListener("pointerdown", new EventListener() {
                public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                    f.session.getPointers().setCapture(1, f.element("inner"));
                    f.session.getPointers().releaseCapture(1, f.element("inner"));
                }
            });
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            f.session.submitDocumentTask(new Runnable() {
                public void run() {
                    f.session.getPointers().setCapture(1, f.element("inner"));
                }
            });
            TestSupport.settle(f.session);
            Check.isFalse(f.session.getPointers().hasCapture(1, f.element("inner")), "a pointer without a button down cannot capture");
            f.take();
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1);
            f.take();
            f.send(MouseEvent.MOUSE_DRAGGED, InputEvent.BUTTON1_DOWN_MASK, 20, 50, 0, false, MouseEvent.NOBUTTON);
            String moved = f.take();
            Check.isTrue(moved.indexOf("capture") < 0, "set and released in the same listener: no capture events: " + moved);
            Check.isTrue(moved.indexOf("pointermove:other") >= 0, "the pointer moved on over the element under it: " + moved);
            f.send(MouseEvent.MOUSE_RELEASED, 0, 20, 50, 1, false, MouseEvent.BUTTON1);
        } finally {
            f.close();
        }
    }
}
