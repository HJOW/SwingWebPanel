package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.UiEvent;
import org.duckdns.hjow.swingwebpanel.dom.WheelEvent;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * The DOM mouse events that real AWT pointer events produce (plan item P11-01): <code>mouseover</code>/<code>mouseenter</code> and
 * <code>mouseout</code>/<code>mouseleave</code> when the pointer moves between elements, <code>mousemove</code>, <code>mousedown</code>,
 * <code>mouseup</code>, <code>click</code>, <code>dblclick</code>, <code>auxclick</code>, <code>contextmenu</code> and <code>wheel</code>, with
 * their targets, related targets, buttons, detail and coordinates, in the order that the UI Events specification gives, and as trusted
 * events on the document queue.
 * <p>
 * 실제 AWT 포인터 이벤트가 만드는 DOM 마우스 이벤트의 시험(계획 P11-01): 포인터가 요소 사이를 움직일 때의
 * <code>mouseover</code>/<code>mouseenter</code>, <code>mouseout</code>/<code>mouseleave</code>, 그리고 <code>mousemove</code>,
 * <code>mousedown</code>, <code>mouseup</code>, <code>click</code>, <code>dblclick</code>, <code>auxclick</code>, <code>contextmenu</code>,
 * <code>wheel</code>. 대상, 상대 대상, 버튼, 상세, 좌표가 UI Events 명세의 순서대로 문서 큐에서 신뢰 이벤트로 전달된다.
 */
public class DomMouseEventTest {

    /**
     * The page: an outer box with two stacked boxes of 100 by 40 pixels.
     * <p>
     * 페이지: 100×40 픽셀 상자 둘이 쌓인 바깥 상자.
     */
    private static final String PAGE = "<!doctype html><style>html,body{margin:0}</style><div id=outer style='width:200px;height:100px'>"
            + "<div id=inner style='width:100px;height:40px'>x</div><div id=other style='width:100px;height:40px'>y</div></div>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public DomMouseEventTest() {
    }

    /**
     * A page with the input controller installed as the real listener of a Swing component and a recorder of every DOM mouse event.
     * <p>
     * 입력 제어기를 실제 Swing 컴포넌트의 리스너로 설치한 페이지와 모든 DOM 마우스 이벤트의 기록기.
     */
    private static final class Fixture implements PageView.Host {
        /**
         * The Swing component that receives the pointer events.
         * <p>
         * 포인터 이벤트를 받는 Swing 컴포넌트.
         */
        final JPanel component = new JPanel();

        /**
         * The page loader.
         * <p>
         * 페이지 로더.
         */
        final FakeLoader loader = new FakeLoader();

        /**
         * The session.
         * <p>
         * 세션.
         */
        final BrowserSession session = new BrowserSession(new ParsingPageLoader(loader), null);

        /**
         * The link listeners.
         * <p>
         * 링크 리스너.
         */
        final HyperlinkDispatcher links = new HyperlinkDispatcher();

        /**
         * The view that draws the page and answers the hit tests.
         * <p>
         * 페이지를 그리고 좌표 조회에 답하는 뷰.
         */
        final PageView view = new PageView(session, new ViewMetrics(), this);

        /**
         * The input controller.
         * <p>
         * 입력 제어기.
         */
        final PageInput input = new PageInput(component, session, view, links);

        /**
         * The recorded DOM events.
         * <p>
         * 기록된 DOM 이벤트.
         */
        final List<String> log = Collections.synchronizedList(new ArrayList<String>());

        /**
         * Registers the controller as the listener of the component.
         * <p>
         * 제어기를 컴포넌트의 리스너로 등록한다.
         */
        Fixture() {
            component.addMouseListener(input);
            component.addMouseMotionListener(input);
            component.addMouseWheelListener(input);
        }

        /**
         * Receives view updates without a window.
         * <p>
         * 창 없이 뷰 갱신을 받는다.
         */
        public void viewChanged() {
        }

        /**
         * Opens the page and records the DOM mouse events with capturing listeners on the document.
         * <p>
         * 페이지를 열고 문서의 캡처 리스너로 DOM 마우스 이벤트를 기록한다.
         *
         * @throws Exception on failure / 실패 시
         */
        void open() throws Exception {
            session.setContent(PAGE, "text/html");
            TestSupport.settle(session);
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Publishes the view.
                 * <p>
                 * 뷰를 게시한다.
                 */
                public void run() {
                    view.pageConfirmed(session.getSnapshot());
                }
            });
            TestSupport.settle(session);
            session.submitDocumentTask(new Runnable() {
                /**
                 * Adds the recorders on the document queue.
                 * <p>
                 * 문서 큐에서 기록기를 더한다.
                 */
                public void run() {
                    Document d = ((HtmlPageDocument) session.getSnapshot().getDocument()).getDocument();
                    String[] types = { "mouseover", "mouseenter", "mouseout", "mouseleave", "mousemove", "mousedown", "mouseup", "click",
                        "dblclick", "auxclick", "contextmenu", "wheel" };
                    // 모든 마우스 이벤트 종류에 캡처 리스너를 달아 비버블링 이벤트도 기록한다.
                    for (int i = 0; i < types.length; i++) {
                        d.addEventListener(types[i], new EventListener() {
                            /**
                             * Records one event.
                             * <p>
                             * 이벤트 하나를 기록한다.
                             *
                             * @param event the event / 이벤트
                             */
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
         * Describes an event in one line: type, target, related target, button, buttons, detail, client coordinates, trust.
         * <p>
         * 이벤트를 한 줄로 기술한다: 종류, 대상, 상대 대상, 버튼, 버튼들, 상세, client 좌표, 신뢰 여부.
         *
         * @param event the event / 이벤트
         * @return the text / 문자열
         */
        String describe(org.duckdns.hjow.swingwebpanel.dom.Event event) {
            org.duckdns.hjow.swingwebpanel.dom.MouseEvent m = (org.duckdns.hjow.swingwebpanel.dom.MouseEvent) event;
            StringBuilder sb = new StringBuilder(event.getType()).append(':').append(id(m.getTarget()));
            if (m.getRelatedTarget() != null) {
                sb.append("<>").append(id(m.getRelatedTarget()));
            }
            sb.append(" b").append(m.getButton()).append(" bs").append(m.getButtons()).append(" d").append(m.getDetail());
            if (m instanceof WheelEvent) {
                sb.append(" dy").append((int) ((WheelEvent) m).getDeltaY()).append(" mode").append(((WheelEvent) m).getDeltaMode());
            } else {
                sb.append(" @").append((int) m.getClientX()).append(',').append((int) m.getClientY());
            }
            if (!event.isTrusted()) {
                sb.append(" untrusted");
            }
            return sb.toString();
        }

        /**
         * Names an event target by its id.
         * <p>
         * 이벤트 대상을 id 로 부른다.
         *
         * @param target the target / 대상
         * @return the id, or the node name / id. 없으면 노드 이름
         */
        private String id(org.duckdns.hjow.swingwebpanel.dom.EventTarget target) {
            if (target instanceof org.duckdns.hjow.swingwebpanel.dom.Element) {
                String id = ((org.duckdns.hjow.swingwebpanel.dom.Element) target).getAttribute("id");
                return id == null || id.length() == 0 ? ((org.duckdns.hjow.swingwebpanel.dom.Element) target).getLocalName() : id;
            }
            return String.valueOf(target);
        }

        /**
         * Delivers an AWT pointer event through the component and drains the document queue and the EDT.
         * <p>
         * AWT 포인터 이벤트를 컴포넌트로 전달하고 문서 큐와 EDT 를 비운다.
         *
         * @param id         the AWT event id / AWT 이벤트 ID
         * @param modifiers  the extended modifiers / 확장 보조 값
         * @param x          x / x
         * @param y          y / y
         * @param clickCount the click count / 클릭 횟수
         * @param popup      the pop-up trigger flag / 팝업 트리거 표시
         * @param button     the AWT button / AWT 버튼
         * @throws Exception on failure / 실패 시
         */
        void send(final int id, final int modifiers, final int x, final int y, final int clickCount, final boolean popup, final int button)
                throws Exception {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Dispatches the event.
                 * <p>
                 * 이벤트를 전달한다.
                 */
                public void run() {
                    component.dispatchEvent(new MouseEvent(component, id, System.currentTimeMillis(), modifiers, x, y, clickCount, popup, button));
                }
            });
            TestSupport.settle(session);
            TestSupport.settle(session);
        }

        /**
         * Closes everything.
         * <p>
         * 모든 것을 닫는다.
         */
        void close() {
            input.dispose();
            view.dispose();
            session.awaitIdle(10000);
            session.dispose();
            TestFlush.edt();
        }
    }

    /**
     * Drains the EDT (kept small so that the fixture does not depend on more than the test support offers).
     * <p>
     * EDT 를 비운다(시험 도구가 주는 것 이상에 의존하지 않도록 작게 유지한다).
     */
    private static final class TestFlush {
        /**
         * Waits until the EDT has handled everything queued before.
         * <p>
         * 이전에 대기한 것을 EDT 가 모두 처리할 때까지 기다린다.
         */
        static void edt() {
            TestSupport.flushEdt();
        }
    }

    /**
     * Returns the recorded events up to now as one text, one event per line, and clears the record.
     * <p>
     * 지금까지 기록된 이벤트를 한 줄에 하나씩 이은 문자열로 반환하고 기록을 지운다.
     *
     * @param f the fixture / 시험 도구
     * @return the lines / 줄
     */
    private static String take(Fixture f) {
        StringBuilder sb = new StringBuilder();
        synchronized (f.log) {
            // 기록된 줄을 순서대로 이어 붙인다.
            for (String line : f.log) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(line);
            }
            f.log.clear();
        }
        return sb.toString();
    }

    /**
     * P11-01: moving into an element sends over and enter events from the outside in, then move; moving to a sibling sends out and leave
     * from the inside out, then over, enter and move; leaving the component sends out and leave to everything under the pointer.
     * <p>
     * P11-01: 요소로 들어가면 바깥에서 안쪽으로 over 와 enter, 그다음 move 를 보내고, 형제로 가면 안쪽에서 바깥으로 out 과 leave, 이어서 over, enter, move 를
     * 보내며, 컴포넌트를 떠나면 포인터 아래의 모든 것에 out 과 leave 를 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEnterLeaveOverOutOrder() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            Check.equal("mouseover:inner b0 bs0 d0 @10,10\nmouseenter:html b0 bs0 d0 @10,10\nmouseenter:body b0 bs0 d0 @10,10\n"
                    + "mouseenter:outer b0 bs0 d0 @10,10\nmouseenter:inner b0 bs0 d0 @10,10\nmousemove:inner b0 bs0 d0 @10,10", take(f),
                    "entering the page");
            f.send(MouseEvent.MOUSE_MOVED, 0, 20, 50, 0, false, MouseEvent.NOBUTTON);
            Check.equal("mouseout:inner<>other b0 bs0 d0 @20,50\nmouseleave:inner<>other b0 bs0 d0 @20,50\nmouseover:other<>inner b0 bs0 d0 @20,50\n"
                    + "mouseenter:other<>inner b0 bs0 d0 @20,50\nmousemove:other b0 bs0 d0 @20,50", take(f), "moving to the sibling");
            f.send(MouseEvent.MOUSE_MOVED, 0, 25, 55, 0, false, MouseEvent.NOBUTTON);
            Check.equal("mousemove:other b0 bs0 d0 @25,55", take(f), "moving inside the same element");
            f.send(MouseEvent.MOUSE_EXITED, 0, 25, 55, 0, false, MouseEvent.NOBUTTON);
            Check.equal("mouseout:other b0 bs0 d0 @25,55\nmouseleave:other b0 bs0 d0 @25,55\nmouseleave:outer b0 bs0 d0 @25,55\n"
                    + "mouseleave:body b0 bs0 d0 @25,55\nmouseleave:html b0 bs0 d0 @25,55", take(f), "leaving the component");
        } finally {
            f.close();
        }
    }

    /**
     * P11-01: press, release and click give mousedown, mouseup and click with the button, the buttons that are down and the click count; the
     * second click of a double click adds dblclick; the other buttons give auxclick, and a pop-up trigger gives contextmenu.
     * <p>
     * P11-01: 누르기, 떼기, 클릭은 버튼, 눌린 버튼, 클릭 횟수와 함께 mousedown, mouseup, click 이 되고, 더블 클릭의 두 번째 클릭은 dblclick 을 더하며, 다른
     * 버튼은 auxclick, 팝업 트리거는 contextmenu 를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testButtonsClicksAndContextMenu() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            take(f);
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON1);
            f.send(MouseEvent.MOUSE_RELEASED, 0, 10, 10, 1, false, MouseEvent.BUTTON1);
            f.send(MouseEvent.MOUSE_CLICKED, 0, 10, 10, 1, false, MouseEvent.BUTTON1);
            Check.equal("mousedown:inner b0 bs1 d1 @10,10\nmouseup:inner b0 bs0 d1 @10,10\nclick:inner b0 bs0 d1 @10,10", take(f), "one click");
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, 10, 10, 2, false, MouseEvent.BUTTON1);
            f.send(MouseEvent.MOUSE_RELEASED, 0, 10, 10, 2, false, MouseEvent.BUTTON1);
            f.send(MouseEvent.MOUSE_CLICKED, 0, 10, 10, 2, false, MouseEvent.BUTTON1);
            Check.equal("mousedown:inner b0 bs1 d2 @10,10\nmouseup:inner b0 bs0 d2 @10,10\nclick:inner b0 bs0 d2 @10,10\ndblclick:inner b0 bs0 d2 @10,10",
                    take(f), "double click");
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON3_DOWN_MASK, 10, 10, 1, true, MouseEvent.BUTTON3);
            f.send(MouseEvent.MOUSE_RELEASED, 0, 10, 10, 1, false, MouseEvent.BUTTON3);
            f.send(MouseEvent.MOUSE_CLICKED, 0, 10, 10, 1, false, MouseEvent.BUTTON3);
            Check.equal("mousedown:inner b2 bs2 d1 @10,10\ncontextmenu:inner b2 bs2 d0 @10,10\nmouseup:inner b2 bs0 d1 @10,10\n"
                    + "auxclick:inner b2 bs0 d1 @10,10", take(f), "secondary button: contextmenu on the press, auxclick on the click");
            f.send(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON2_DOWN_MASK, 10, 10, 1, false, MouseEvent.BUTTON2);
            Check.equal("mousedown:inner b1 bs4 d1 @10,10", take(f), "the wheel button is button 1 and bit 4");
        } finally {
            f.close();
        }
    }

    /**
     * P11-01: the wheel gives a <code>wheel</code> event with its amount in lines, and the pointer events are trusted.
     * <p>
     * P11-01: 휠은 줄 단위의 양을 가진 <code>wheel</code> 이벤트가 되며 포인터 이벤트는 신뢰 이벤트이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testWheelAndTrust() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open();
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Delivers a wheel event.
                 * <p>
                 * 휠 이벤트를 전달한다.
                 */
                public void run() {
                    f.component.dispatchEvent(new MouseWheelEvent(f.component, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 10, 10, 0,
                            false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 1));
                }
            });
            TestSupport.settle(f.session);
            TestSupport.settle(f.session);
            Check.equal("wheel:inner b0 bs0 d0 dy3 mode1", take(f), "wheel");
            f.send(MouseEvent.MOUSE_MOVED, 0, 10, 10, 0, false, MouseEvent.NOBUTTON);
            String moves = take(f);
            Check.isTrue(moves.indexOf("untrusted") < 0, "user events are trusted: " + moves);
            Check.isTrue(UiEvent.class.isAssignableFrom(org.duckdns.hjow.swingwebpanel.dom.MouseEvent.class), "mouse events are UI events");
        } finally {
            f.close();
        }
    }
}
