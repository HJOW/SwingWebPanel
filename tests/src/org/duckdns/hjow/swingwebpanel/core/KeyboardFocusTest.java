package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.EventListener;
import org.duckdns.hjow.swingwebpanel.dom.FocusManager;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Keyboard and focus input with real AWT events (plan items P11-01 and P11-02): the keyboard events with their key values and codes, the
 * focus events in the order of the specification, the Tab order, the focus that a click gives, the default actions of Enter, the effect of
 * <code>preventDefault</code>, the focus of the component itself, and the <code>:focus</code>, <code>:hover</code> and <code>:active</code>
 * state that reaches the style engine and the layout.
 * <p>
 * 실제 AWT 이벤트로 하는 키보드와 포커스 입력의 시험(계획 P11-01, P11-02): 키 값과 코드를 가진 키보드 이벤트, 명세 순서의 포커스 이벤트, Tab 순서,
 * 클릭이 주는 포커스, Enter 의 기본 동작, <code>preventDefault</code> 의 효과, 컴포넌트 자신의 포커스, 스타일 엔진과 레이아웃에 닿는
 * <code>:focus</code>, <code>:hover</code>, <code>:active</code> 상태.
 */
public class KeyboardFocusTest {

    /**
     * The page: a link, a field, a button, an element with a positive <code>tabindex</code> that is first in the Tab order, one that only
     * a script or a click can focus, a plain box, and a link that is styled by its states.
     * <p>
     * 페이지: 링크, 입력란, 단추, Tab 순서에서 첫째인 양수 <code>tabindex</code> 요소, 스크립트나 클릭만 포커스를 줄 수 있는 요소, 보통 상자, 상태로 꾸미는 링크.
     */
    private static final String PAGE = "<!doctype html><style>html,body{margin:0} #s{display:block;width:100px;height:20px} "
            + "#s:focus{width:150px} #s:hover{height:30px} #s:active{height:40px}</style>"
            + "<a id=a href='http://links.test/next'>link</a><input id=b><button id=c>go</button><div id=d tabindex=3>d</div>"
            + "<div id=e tabindex=-1>e</div><div id=plain style='width:50px;height:20px'>p</div><a id=s href='#top'>styled</a>"
            + "<button id=off disabled>off</button>";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public KeyboardFocusTest() {
    }

    /**
     * A page with the pointer and keyboard controllers installed as the real listeners of a Swing component, and a recorder.
     * <p>
     * 포인터와 키보드 제어기를 실제 Swing 컴포넌트의 리스너로 설치한 페이지와 기록기.
     */
    private static final class Fixture implements PageView.Host {
        /**
         * The Swing component.
         * <p>
         * Swing 컴포넌트.
         */
        final JPanel component = new JPanel();

        /**
         * The page loader.
         * <p>
         * 페이지 로더.
         */
        final FakeLoader loader = new FakeLoader();

        /**
         * The cookies and the request layer that the standard component gives its pages.
         * <p>
         * 표준 컴포넌트가 페이지에 주는 쿠키와 요청 계층.
         */
        final org.duckdns.hjow.swingwebpanel.net.CookieStore cookies = new org.duckdns.hjow.swingwebpanel.net.CookieStore();

        /**
         * The request layer.
         * <p>
         * 요청 계층.
         */
        final org.duckdns.hjow.swingwebpanel.net.ResourceFetcher fetcher = new org.duckdns.hjow.swingwebpanel.net.ResourceFetcher(org.duckdns.hjow.swingwebpanel.net.DefaultSslConfigurer.INSTANCE,
                org.duckdns.hjow.swingwebpanel.net.FetchLimits.DEFAULT, getClass().getClassLoader());

        /**
         * The loader that parses the pages.
         * <p>
         * 페이지를 해석하는 로더.
         */
        final ParsingPageLoader parsing = new ParsingPageLoader(loader);

        /**
         * The session.
         * <p>
         * 세션.
         */
        final BrowserSession session = new BrowserSession(parsing, null, cookies);

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
        final PageView view = new PageView(session, new ViewMetrics(), this, fetcher, cookies);

        /**
         * The pointer controller.
         * <p>
         * 포인터 제어기.
         */
        final PageInput input = new PageInput(component, session, view, links);

        /**
         * The keyboard controller.
         * <p>
         * 키보드 제어기.
         */
        final PageKeyboard keyboard = new PageKeyboard(component, session, view, input);

        /**
         * The recorded events.
         * <p>
         * 기록된 이벤트.
         */
        final List<String> log = Collections.synchronizedList(new ArrayList<String>());

        /**
         * Registers the controllers as the listeners of the component.
         * <p>
         * 제어기를 컴포넌트의 리스너로 등록한다.
         */
        Fixture() {
            parsing.bindScripts(session, fetcher, cookies, view);
            component.addMouseListener(input);
            component.addMouseMotionListener(input);
            component.addKeyListener(keyboard);
            component.addFocusListener(keyboard);
        }

        /**
         * Receives view updates without a window.
         * <p>
         * 창 없이 뷰 갱신을 받는다.
         */
        public void viewChanged() {
        }

        /**
         * Opens the page and records keyboard, focus and click events with capturing listeners on the document.
         * <p>
         * 페이지를 열고 문서의 캡처 리스너로 키보드, 포커스, 클릭 이벤트를 기록한다.
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
                    Document d = document();
                    String[] types = { "keydown", "keypress", "keyup", "focus", "blur", "focusin", "focusout", "click" };
                    // 모든 종류에 캡처 리스너를 달아 비버블링 이벤트도 기록한다.
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
         * Returns the DOM document.
         * <p>
         * DOM 문서를 반환한다.
         *
         * @return the document / 문서
         */
        Document document() {
            return ((HtmlPageDocument) session.getSnapshot().getDocument()).getDocument();
        }

        /**
         * Describes an event in one line.
         * <p>
         * 이벤트를 한 줄로 기술한다.
         *
         * @param event the event / 이벤트
         * @return the text / 문자열
         */
        String describe(org.duckdns.hjow.swingwebpanel.dom.Event event) {
            StringBuilder sb = new StringBuilder(event.getType()).append(':').append(id(event.getTarget()));
            if (event instanceof org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent) {
                org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent k = (org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent) event;
                sb.append(" key=").append(k.getKey()).append(" code=").append(k.getCode()).append(" kc=").append(k.getKeyCode()).append(" cc=")
                        .append(k.getCharCode());
                if (k.isShiftKey()) {
                    sb.append(" shift");
                }
                if (k.isCtrlKey()) {
                    sb.append(" ctrl");
                }
                if (k.isRepeat()) {
                    sb.append(" repeat");
                }
            } else if (event instanceof org.duckdns.hjow.swingwebpanel.dom.FocusEvent && ((org.duckdns.hjow.swingwebpanel.dom.FocusEvent) event).getRelatedTarget() != null) {
                sb.append(" related=").append(id(((org.duckdns.hjow.swingwebpanel.dom.FocusEvent) event).getRelatedTarget()));
            }
            return sb.toString();
        }

        /**
         * Names an event target by its id.
         * <p>
         * 이벤트 대상을 id 로 부른다.
         *
         * @param target the target / 대상
         * @return the id or the node name / id. 없으면 노드 이름
         */
        private String id(org.duckdns.hjow.swingwebpanel.dom.EventTarget target) {
            if (target instanceof Element) {
                String id = ((Element) target).getAttribute("id");
                return id == null || id.length() == 0 ? ((Element) target).getLocalName() : id;
            }
            return String.valueOf(target);
        }

        /**
         * Delivers an AWT key event and drains the document queue and the EDT.
         * <p>
         * AWT 키 이벤트를 전달하고 문서 큐와 EDT 를 비운다.
         *
         * @param id        the AWT event id / AWT 이벤트 ID
         * @param modifiers the extended modifiers / 확장 보조 값
         * @param vk        the virtual key / 가상 키
         * @param ch        the character / 문자
         * @throws Exception on failure / 실패 시
         */
        void key(final int id, final int modifiers, final int vk, final char ch) throws Exception {
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Dispatches the event through the component.
                 * <p>
                 * 컴포넌트로 이벤트를 전달한다.
                 */
                public void run() {
                    // KeyEvent 를 component.dispatchEvent 로 보내면 AWT 가 포커스 소유자에게 돌려보내므로 등록된 리스너를 직접 부른다.
                    KeyEvent e = new KeyEvent(component, id, System.currentTimeMillis(), modifiers, vk, ch);
                    for (java.awt.event.KeyListener l : component.getKeyListeners()) {
                        if (id == KeyEvent.KEY_PRESSED) {
                            l.keyPressed(e);
                        } else if (id == KeyEvent.KEY_TYPED) {
                            l.keyTyped(e);
                        } else {
                            l.keyReleased(e);
                        }
                    }
                }
            });
            settle();
        }

        /**
         * Presses and releases a key like a keyboard does (press, typed for a character, release).
         * <p>
         * 키보드처럼 키를 누르고 뗀다(누름, 글자이면 typed, 뗌).
         *
         * @param modifiers the extended modifiers / 확장 보조 값
         * @param vk        the virtual key / 가상 키
         * @param ch        the character or <code>KeyEvent.CHAR_UNDEFINED</code> / 문자. 없으면 <code>KeyEvent.CHAR_UNDEFINED</code>
         * @throws Exception on failure / 실패 시
         */
        void tap(int modifiers, int vk, char ch) throws Exception {
            key(KeyEvent.KEY_PRESSED, modifiers, vk, ch);
            if (ch != KeyEvent.CHAR_UNDEFINED) {
                key(KeyEvent.KEY_TYPED, modifiers, KeyEvent.VK_UNDEFINED, ch);
            }
            key(KeyEvent.KEY_RELEASED, modifiers, vk, ch);
        }

        /**
         * Delivers an AWT pointer event.
         * <p>
         * AWT 포인터 이벤트를 전달한다.
         *
         * @param id        the AWT event id / AWT 이벤트 ID
         * @param modifiers the extended modifiers / 확장 보조 값
         * @param box       the layout box that the pointer is over / 포인터가 위에 있는 레이아웃 상자
         * @param button    the AWT button / AWT 버튼
         * @throws Exception on failure / 실패 시
         */
        void pointer(final int id, final int modifiers, LayoutBox box, final int button) throws Exception {
            final int x = (int) box.getX() + 2;
            final int y = (int) box.getY() + 2;
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Dispatches the event through the component.
                 * <p>
                 * 컴포넌트로 이벤트를 전달한다.
                 */
                public void run() {
                    component.dispatchEvent(new MouseEvent(component, id, System.currentTimeMillis(), modifiers, x, y, 1, false, button));
                }
            });
            settle();
        }

        /**
         * Returns the first layout box of an element.
         * <p>
         * 요소의 첫 레이아웃 상자를 반환한다.
         *
         * @param elementId the id / id
         * @return the box / 상자
         */
        LayoutBox box(String elementId) {
            return view.getRenderedPage().getLayout().getBoxes(document().getElementById(elementId)).get(0);
        }

        /**
         * Returns the id of the focused element, or "-" if none has the focus.
         * <p>
         * 포커스를 가진 요소의 id 를 반환한다. 없으면 "-".
         *
         * @return the id / id
         */
        String focused() {
            final String[] result = new String[1];
            session.submitDocumentTask(new Runnable() {
                /**
                 * Reads the focus on the document queue.
                 * <p>
                 * 문서 큐에서 포커스를 읽는다.
                 */
                public void run() {
                    Element f = FocusManager.getFocused(document());
                    result[0] = f == null ? "-" : f.getAttribute("id");
                }
            });
            TestSupport.settle(session);
            return result[0];
        }

        /**
         * Drains the document queue and the EDT.
         * <p>
         * 문서 큐와 EDT 를 비운다.
         */
        void settle() {
            TestSupport.settle(session);
            TestSupport.settle(session);
        }

        /**
         * Takes and clears the recorded events as one text, one event per line.
         * <p>
         * 기록된 이벤트를 한 줄에 하나씩 이은 문자열로 가져오고 기록을 지운다.
         *
         * @return the text / 문자열
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
         * Closes everything.
         * <p>
         * 모든 것을 닫는다.
         */
        void close() {
            input.dispose();
            keyboard.dispose();
            view.dispose();
            session.awaitIdle(10000);
            session.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * P11-02: Tab visits the elements in the order of the HTML Standard (positive <code>tabindex</code> first, then the document order,
     * without <code>tabindex=-1</code>, disabled controls and hidden ones), Shift+Tab goes back, and every move sends the events in the order
     * blur, focusout, focus, focusin with the other element as the related target.
     * <p>
     * P11-02: Tab 은 HTML 표준의 순서로 요소를 방문하고(양수 <code>tabindex</code> 가 먼저, 그다음 문서 순서이며 <code>tabindex=-1</code>, 비활성 컨트롤,
     * 숨은 요소는 제외), Shift+Tab 은 뒤로 가며, 이동마다 blur, focusout, focus, focusin 순서로 상대 요소와 함께 이벤트를 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTabOrderAndFocusEvents() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("d", f.focused(), "the element with a positive tabindex is first");
            String first = f.take();
            Check.isTrue(first.indexOf("focus:d\nfocusin:d") >= 0, "focus then focusin: " + first);
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("a", f.focused(), "then the document order");
            String second = f.take();
            Check.isTrue(second.indexOf("blur:d related=a\nfocusout:d related=a\nfocus:a related=d\nfocusin:a related=d") >= 0,
                    "blur, focusout, focus, focusin with the related target: " + second);
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("b", f.focused(), "input");
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("c", f.focused(), "button");
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("s", f.focused(), "the styled link; plain boxes, tabindex=-1 and disabled controls are skipped");
            f.take();
            f.tap(InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_TAB, '\t');
            Check.equal("c", f.focused(), "Shift+Tab goes back");
            f.tap(0, KeyEvent.VK_TAB, '\t');
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("s", f.focused(), "after the last element the focus stays until the host moves it");
        } finally {
            f.close();
        }
    }

    /**
     * P11-01: a typed character gives keydown, keypress and keyup with the key, the physical key, the legacy codes and the modifiers; a
     * key that is held down repeats; keys without a character give no keypress; the events go to the focused element.
     * <p>
     * P11-01: 입력한 글자는 키, 물리적 키, 예전 코드, 보조 키와 함께 keydown, keypress, keyup 을 만들고, 누르고 있는 키는 반복되며, 글자 없는 키는 keypress 가
     * 없고, 이벤트는 포커스를 가진 요소로 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testKeyboardEventsCarryKeysAndModifiers() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            f.tap(InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_A, 'A');
            Check.equal("keydown:body key=A code=KeyA kc=65 cc=0 shift\nkeypress:body key=A code= kc=0 cc=65 shift\nkeyup:body key=A code=KeyA kc=65 cc=0 shift",
                    f.take(), "a letter with Shift on the body");
            f.tap(0, KeyEvent.VK_ENTER, '\n');
            Check.equal("keydown:body key=Enter code=Enter kc=13 cc=0\nkeypress:body key=Enter code= kc=0 cc=13\nkeyup:body key=Enter code=Enter kc=13 cc=0",
                    f.take(), "Enter");
            f.tap(InputEvent.CTRL_DOWN_MASK, KeyEvent.VK_LEFT, KeyEvent.CHAR_UNDEFINED);
            Check.equal("keydown:body key=ArrowLeft code=ArrowLeft kc=37 cc=0 ctrl\nkeyup:body key=ArrowLeft code=ArrowLeft kc=37 cc=0 ctrl", f.take(),
                    "an arrow key has no keypress");
            f.key(KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_F5, KeyEvent.CHAR_UNDEFINED);
            f.key(KeyEvent.KEY_PRESSED, 0, KeyEvent.VK_F5, KeyEvent.CHAR_UNDEFINED);
            Check.equal("keydown:body key=F5 code=F5 kc=116 cc=0\nkeydown:body key=F5 code=F5 kc=116 cc=0 repeat", f.take(), "the second press repeats");
            f.key(KeyEvent.KEY_RELEASED, 0, KeyEvent.VK_F5, KeyEvent.CHAR_UNDEFINED);
            f.take();
            f.tap(0, KeyEvent.VK_TAB, '\t');
            f.take();
            f.tap(0, KeyEvent.VK_X, 'x');
            Check.isTrue(f.take().startsWith("keydown:d key=x"), "the events go to the focused element");
        } finally {
            f.close();
        }
    }

    /**
     * P11-01, P11-02: <code>preventDefault</code> on keydown stops the Tab key from moving the focus, and Enter on a focused link sends a
     * trusted click to it.
     * <p>
     * P11-01, P11-02: keydown 의 <code>preventDefault</code> 는 Tab 키가 포커스를 옮기지 못하게 하고, 포커스를 가진 링크의 Enter 는 신뢰 click 을 보낸다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testDefaultActionsAndCancellation() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open();
            f.tap(0, KeyEvent.VK_TAB, '\t');
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("a", f.focused(), "the link is focused");
            f.take();
            // 링크를 따라가면 페이지가 바뀌므로, 이 시험은 click 을 취소해 이동을 막은 채 click 이 갔는지만 확인한다.
            f.session.submitDocumentTask(new Runnable() {
                /**
                 * Cancels click on the document queue.
                 * <p>
                 * 문서 큐에서 click 을 취소한다.
                 */
                public void run() {
                    f.document().addEventListener("click", new EventListener() {
                        /**
                         * Cancels the event.
                         * <p>
                         * 이벤트를 취소한다.
                         *
                         * @param event the event / 이벤트
                         */
                        public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                            event.preventDefault();
                        }
                    }, false);
                }
            });
            f.settle();
            f.tap(0, KeyEvent.VK_ENTER, '\n');
            String enter = f.take();
            Check.isTrue(enter.indexOf("keydown:a key=Enter") >= 0 && enter.indexOf("click:a") >= 0, "Enter on a link clicks it: " + enter);
            f.session.submitDocumentTask(new Runnable() {
                /**
                 * Cancels keydown on the document queue.
                 * <p>
                 * 문서 큐에서 keydown 을 취소한다.
                 */
                public void run() {
                    f.document().addEventListener("keydown", new EventListener() {
                        /**
                         * Cancels the Tab key.
                         * <p>
                         * Tab 키를 취소한다.
                         *
                         * @param event the event / 이벤트
                         */
                        public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                            if ("Tab".equals(((org.duckdns.hjow.swingwebpanel.dom.KeyboardEvent) event).getKey())) {
                                event.preventDefault();
                            }
                        }
                    }, false);
                }
            });
            f.settle();
            f.tap(0, KeyEvent.VK_TAB, '\t');
            Check.equal("a", f.focused(), "a cancelled Tab did not move the focus");
        } finally {
            f.close();
        }
    }

    /**
     * P11-02: a click gives the focus to the nearest element that can have it and takes it away on a plain area; a cancelled
     * <code>mousedown</code> does not move the focus; the focus of the component itself is passed on to the focused element.
     * <p>
     * P11-02: 클릭은 포커스를 가질 수 있는 가장 가까운 요소에 포커스를 주고 보통 영역에서는 거두며, 취소된 <code>mousedown</code> 은 포커스를 옮기지 않고,
     * 컴포넌트 자신의 포커스는 포커스를 가진 요소에 전달된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClickFocusAndComponentFocus() throws Exception {
        final Fixture f = new Fixture();
        try {
            f.open();
            f.pointer(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, f.box("c"), MouseEvent.BUTTON1);
            Check.equal("c", f.focused(), "a click on a button focuses it");
            f.pointer(MouseEvent.MOUSE_RELEASED, 0, f.box("c"), MouseEvent.BUTTON1);
            f.take();
            f.pointer(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, f.box("plain"), MouseEvent.BUTTON1);
            Check.equal("-", f.focused(), "a click on a plain area takes the focus away");
            Check.isTrue(f.take().indexOf("blur:c") >= 0, "the button got blur");
            f.pointer(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, f.box("e"), MouseEvent.BUTTON1);
            Check.equal("e", f.focused(), "tabindex=-1 can be focused by a click");
            f.take();
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Takes the keyboard focus away from the component.
                 * <p>
                 * 컴포넌트에서 키보드 포커스를 거둔다.
                 */
                public void run() {
                    // 등록된 포커스 리스너를 모두 직접 불러 포커스 상실을 흉내 낸다.
                    for (java.awt.event.FocusListener l : f.component.getFocusListeners()) {
                        l.focusLost(new FocusEvent(f.component, FocusEvent.FOCUS_LOST));
                    }
                }
            });
            f.settle();
            Check.equal("blur:e\nfocusout:e", f.take(), "the component lost the focus: the element gets blur");
            Check.equal("e", f.focused(), "and stays the focused element");
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Gives the keyboard focus back to the component.
                 * <p>
                 * 컴포넌트에 키보드 포커스를 돌려준다.
                 */
                public void run() {
                    // 등록된 포커스 리스너를 모두 직접 불러 포커스 획득을 흉내 낸다.
                    for (java.awt.event.FocusListener l : f.component.getFocusListeners()) {
                        l.focusGained(new FocusEvent(f.component, FocusEvent.FOCUS_GAINED));
                    }
                }
            });
            f.settle();
            Check.equal("focus:e\nfocusin:e", f.take(), "the component got the focus again");
            f.session.submitDocumentTask(new Runnable() {
                /**
                 * Cancels mousedown on the document queue.
                 * <p>
                 * 문서 큐에서 mousedown 을 취소한다.
                 */
                public void run() {
                    f.document().addEventListener("mousedown", new EventListener() {
                        /**
                         * Cancels the event.
                         * <p>
                         * 이벤트를 취소한다.
                         *
                         * @param event the event / 이벤트
                         */
                        public void handleEvent(org.duckdns.hjow.swingwebpanel.dom.Event event) {
                            event.preventDefault();
                        }
                    }, false);
                }
            });
            f.settle();
            f.pointer(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, f.box("c"), MouseEvent.BUTTON1);
            Check.equal("e", f.focused(), "a cancelled mousedown does not move the focus");
        } finally {
            f.close();
        }
    }

    /**
     * P11-02: <code>:focus</code>, <code>:hover</code> and <code>:active</code> reach the style engine, so the layout changes with them.
     * <p>
     * P11-02: <code>:focus</code>, <code>:hover</code>, <code>:active</code> 가 스타일 엔진에 닿으므로 레이아웃이 그에 따라 바뀐다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPseudoClassStateReachesTheLayout() throws Exception {
        Fixture f = new Fixture();
        try {
            f.open();
            LayoutBox idle = f.box("s");
            Check.equal(100, (int) idle.getWidth(), "idle width");
            Check.equal(20, (int) idle.getHeight(), "idle height");
            f.pointer(MouseEvent.MOUSE_MOVED, 0, idle, MouseEvent.NOBUTTON);
            Check.equal(30, (int) f.box("s").getHeight(), ":hover makes the element taller");
            f.pointer(MouseEvent.MOUSE_PRESSED, InputEvent.BUTTON1_DOWN_MASK, f.box("s"), MouseEvent.BUTTON1);
            Check.equal(40, (int) f.box("s").getHeight(), ":active while the button is down");
            Check.equal(150, (int) f.box("s").getWidth(), ":focus after the press focused the link");
            f.pointer(MouseEvent.MOUSE_RELEASED, 0, f.box("s"), MouseEvent.BUTTON1);
            Check.equal(30, (int) f.box("s").getHeight(), "the button came up: back to :hover");
            f.pointer(MouseEvent.MOUSE_EXITED, 0, f.box("s"), MouseEvent.NOBUTTON);
            Check.equal(20, (int) f.box("s").getHeight(), "the pointer left: idle height");
            Check.equal(150, (int) f.box("s").getWidth(), "the link keeps the focus");
        } finally {
            f.close();
        }
    }
}
