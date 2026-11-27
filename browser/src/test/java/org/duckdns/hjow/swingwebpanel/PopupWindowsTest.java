package org.duckdns.hjow.swingwebpanel;

import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Windows that pages open (API contract 5.28, cases C-POPUP-1 to C-POPUP-8): the popup blocker and the action of the user that it
 * needs, the window object that <code>window.open</code> returns and what the opener can do with it, <code>window.opener</code>, named
 * windows, closing, windows without a connection to their opener, links and forms that ask for a new window, the choice of the user
 * with Ctrl, Shift and the middle button, and the life of the windows. The host of the tests is a component that keeps the requested
 * windows in lists instead of showing them.
 * <p>
 * 페이지가 여는 창(API 계약 5.28, 사례 C-POPUP-1 ~ C-POPUP-8): 팝업 차단과 그것이 요구하는 사용자의 조작, <code>window.open</code> 이 돌려주는 창
 * 객체와 여는 창이 그것으로 할 수 있는 일, <code>window.opener</code>, 이름 있는 창, 닫기, 여는 창과 관계가 없는 창, 새 창을 요청하는 링크와 폼,
 * Ctrl·Shift·가운데 단추로 하는 사용자의 선택, 창의 수명. 시험의 호스트는 요청된 창을 보이는 대신 목록에 보관하는 컴포넌트이다.
 */
public class PopupWindowsTest {

    /**
     * A component that records what its pages ask for instead of showing windows.
     * <p>
     * 창을 보이는 대신 페이지가 요청한 것을 기록하는 컴포넌트.
     */
    static final class Host extends SwingWebPanel {

        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /** The windows that scripts opened. / 스크립트가 연 창. */
        final List<WindowRequest> opened = Collections.synchronizedList(new ArrayList<WindowRequest>());

        /** The windows that the popup blocker refused. / 팝업 차단이 거부한 창. */
        final List<WindowRequest> blocked = Collections.synchronizedList(new ArrayList<WindowRequest>());

        /** The windows that links, forms and the user asked for. / 링크, 폼, 사용자가 요청한 창. */
        final List<WindowRequest> offered = Collections.synchronizedList(new ArrayList<WindowRequest>());

        /** The components of this class that scripts closed. / 스크립트가 닫은 이 클래스의 컴포넌트. */
        final List<SwingWebPanel> closed;

        /** True to open the windows that links, forms and the user ask for. / 링크, 폼, 사용자가 요청한 창을 열려면 true. */
        volatile boolean takeLinks;

        /** True to decline windows that scripts open. / 스크립트가 여는 창을 거절하려면 true. */
        volatile boolean rejectScripts;

        /** The number of times that the window of the component itself was closed by a script. / 컴포넌트 자신의 창이 스크립트에 의해 닫힌 횟수. */
        volatile int rootClosed;

        /** True if the component cannot show windows. / 컴포넌트가 창을 보일 수 없으면 true. */
        volatile boolean cannotShow;

        /**
         * Creates a host with a size.
         * <p>
         * 크기가 있는 호스트를 만든다.
         */
        Host() {
            closed = Collections.synchronizedList(new ArrayList<SwingWebPanel>());
            setSize(400, 300);
        }

        /**
         * Creates the component of a window that a page of a host opened.
         * <p>
         * 호스트의 페이지가 연 창의 컴포넌트를 만든다.
         *
         * @param seed   the seed of the window / 창의 씨앗
         * @param closed the list of the first host, shared by its windows / 첫 호스트의 목록. 그 창들이 함께 쓴다
         */
        Host(PopupSeed seed, List<SwingWebPanel> closed) {
            super(seed);
            this.closed = closed;
        }

        /** Creates a window of this class. / 이 클래스의 창을 만든다. */
        protected SwingWebPanel createPopupPanel(PopupSeed seed) {
            return new Host(seed, closed);
        }

        /** Windows can be opened although nothing is shown. / 아무것도 보이지 않지만 창을 열 수 있다. */
        protected boolean canOpenPopupWindows() {
            return !cannotShow;
        }

        /** Keeps the window instead of showing it. / 창을 보이는 대신 보관한다. */
        protected void openPopupWindow(WindowRequest request) {
            if (rejectScripts) {
                request.reject();
            }
            opened.add(request);
        }

        /** Keeps the request and opens its window if the test says so. / 요청을 보관하고, 시험이 그렇게 정했으면 그 창을 연다. */
        protected boolean openNewWindow(WindowRequest request) {
            offered.add(request);
            if (takeLinks) {
                request.getPanel();
                return true;
            }
            return false;
        }

        /** Keeps the blocked window. / 차단한 창을 보관한다. */
        protected void popupBlocked(WindowRequest request) {
            blocked.add(request);
        }

        /** Records the close and disposes the component. / 닫기를 기록하고 컴포넌트를 폐기한다. */
        protected void closeOpenedWindow() {
            closed.add(this);
            disposeKeepingStorage();
        }

        /** Counts the close of the window of the component. / 컴포넌트의 창 닫기를 센다. */
        protected void closeRootWindow() {
            rootClosed++;
        }
    }

    /** Script text that lets the test run code in a page through a message of the host (see {@link #runScript}). / 시험이 호스트의 메시지로 페이지에서 코드를 실행하게 하는 스크립트 글({@link #runScript} 참조). */
    private static final String RUNNER = "window.addEventListener('message', function (e) {"
            + " if (typeof e.data === 'string' && e.data.indexOf('run:') === 0) { eval(e.data.substring(4)); } });";

    /** The server of the pages. / 페이지의 서버. */
    private TestHttpServer server;

    /** The component whose pages open windows. / 페이지가 창을 여는 컴포넌트. */
    private Host host;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PopupWindowsTest() {
    }

    /**
     * Creates the server and the component.
     * <p>
     * 서버와 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        final Host[] made = new Host[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                made[0] = new Host();
            }
        });
        host = made[0];
    }

    /**
     * Disposes the component and the windows that it opened, and stops the server.
     * <p>
     * 컴포넌트와 그것이 연 창을 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        List<WindowRequest> all = new ArrayList<WindowRequest>(host.opened);
        all.addAll(host.offered);
        // 시험이 연 창을 모두 폐기한다.
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).hasPanel()) {
                all.get(i).getPanel().dispose();
            }
        }
        host.dispose();
        server.stop();
    }

    /**
     * Waits until the title of a component is the expected text.
     * <p>
     * 컴포넌트의 제목이 기대한 글이 될 때까지 기다린다.
     *
     * @param p        the component / 컴포넌트
     * @param expected the expected title / 기대하는 제목
     * @param message  what the title means / 제목이 뜻하는 것
     * @throws Exception on failure / 실패 시
     */
    private static void awaitTitle(SwingWebPanel p, String expected, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 제목이 될 때까지 짧게 쉬며 기다린다.
        while (!expected.equals(p.getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(expected, p.getTitle(), message);
    }

    /**
     * Waits until a list has a number of entries, then lets the event dispatch thread finish what it has queued.
     * <p>
     * 목록의 항목 수가 정해진 수가 될 때까지 기다린 뒤, 이벤트 디스패치 스레드가 줄 세운 일을 마치게 한다.
     *
     * @param list    the list / 목록
     * @param count   the number of entries / 항목 수
     * @param message what the entries are / 항목이 무엇인지
     * @throws Exception on failure / 실패 시
     */
    private static void awaitSize(List<?> list, int count, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 항목 수가 될 때까지 짧게 쉬며 기다린다.
        while (list.size() < count && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        settle();
        Check.equal(count, list.size(), message);
    }

    /**
     * Lets the event dispatch thread run what is queued.
     * <p>
     * 이벤트 디스패치 스레드가 줄 선 것을 실행하게 한다.
     *
     * @throws Exception on failure / 실패 시
     */
    private static void settle() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Nothing to do: the wait is the point. / 할 일은 없다. 기다리는 것이 목적이다. */
            public void run() {
            }
        });
    }

    /**
     * Clicks a row of a page whose rows are 40 pixels high, as the mouse does.
     * <p>
     * 행의 높이가 40픽셀인 페이지의 한 행을 마우스처럼 클릭한다.
     *
     * @param p         the component / 컴포넌트
     * @param row       the row, starting at 0 / 0 부터 시작하는 행
     * @param button    the mouse button / 마우스 단추
     * @param modifiers the modifier mask / 보조 키 마스크
     * @throws Exception on failure / 실패 시
     */
    private static void click(final SwingWebPanel p, final int row, final int button, final int modifiers) throws Exception {
        // 렌더링이 게시될 때까지 기다린다(클릭은 렌더링된 페이지에서 요소를 찾는다).
        long end = System.currentTimeMillis() + 30000;
        while (p.getProgress() != 100 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(150);
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Sends the click. / 클릭을 보낸다. */
            public void run() {
                p.dispatchEvent(new MouseEvent(p, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), modifiers, 20, 20 + 40 * row, 1, false, button));
            }
        });
    }

    /**
     * Returns markup for clickable rows of 40 pixels, one per entry.
     * <p>
     * 항목마다 하나씩, 누를 수 있는 40픽셀 행의 마크업을 반환한다.
     *
     * @param rows the content of the rows / 행의 내용
     * @return the markup with a style that fixes the row height / 행 높이를 고정하는 스타일이 든 마크업
     */
    private static String rows(String... rows) {
        StringBuilder out = new StringBuilder("<style>body { margin: 0 } .row { display: block; height: 40px; overflow: hidden }</style>");
        // 행마다 블록 하나를 만든다.
        for (int i = 0; i < rows.length; i++) {
            out.append(rows[i]);
        }
        return out.toString();
    }

    /**
     * C-POPUP-1: with the popup blocker on, a window that a script asks for without an action of the user is not opened: the call
     * gives null and the host is told. A click allows one window; a second call in the same click is blocked. With the blocker off
     * every call opens a window. The setting announces its changes.
     * <p>
     * C-POPUP-1: 팝업 차단이 켜져 있으면 스크립트가 사용자의 조작 없이 요청한 창은 열리지 않는다: 호출은 null 을 주고 호스트에 알린다. 클릭 한
     * 번은 창 하나를 허용하고, 같은 클릭 안의 두 번째 호출은 차단된다. 차단을 끄면 모든 호출이 창을 연다. 설정은 변경을 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_1_BlockerAndUserActivation() throws Exception {
        server.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>child</title><p>child</p>"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title>" + rows(
                "<div class=row id=one onclick=\"var a = window.open('/child', '_blank', 'width=320,height=200,left=30,top=40');"
                        + " var b = window.open('/child'); document.title = 'click:' + (a !== null) + ':' + (b === null);\">open</div>")
                + "<script>window.addEventListener('load', function () { setTimeout(function () {"
                + " var w = window.open('/child'); document.title = 'timer:' + (w === null); }, 50); });</" + "script>"));
        server.start();
        Check.isTrue(host.isPopupBlockingEnabled(), "the popup blocker is on at first");
        host.setPage(server.url("/p"));
        awaitTitle(host, "timer:true", "without an action of the user window.open gives null");
        awaitSize(host.blocked, 1, "the host is told about the blocked window");
        Check.equal(server.url("/child"), host.blocked.get(0).getUrl(), "the blocked address");
        Check.equal(server.url("/p"), host.blocked.get(0).getDocumentUrl(), "the document that asked");
        Check.isFalse(host.blocked.get(0).hasPanel(), "a blocked window has no component");
        Check.equal(0, host.opened.size(), "no window was opened");
        click(host, 0, MouseEvent.BUTTON1, 0);
        awaitTitle(host, "click:true:true", "a click allows one window; the second call is blocked");
        awaitSize(host.opened, 1, "the host gets the window to show");
        awaitSize(host.blocked, 2, "and is told about the second one");
        WindowRequest request = host.opened.get(0);
        Check.equal(WindowRequest.SCRIPT, request.getKind(), "the kind of the request");
        Check.equal(320, request.getWidth(), "the requested width");
        Check.equal(200, request.getHeight(), "the requested height");
        Check.equal(30, request.getLeft(), "the requested left edge");
        Check.equal(40, request.getTop(), "the requested top edge");
        Check.equal("", request.getTargetName(), "_blank is no name");
        Check.same(host, request.getSource(), "the component that asked");
        Check.isTrue(request.getPanel() instanceof Host, "the component of the window comes from the factory of the host");
        awaitTitle(request.getPanel(), "child", "the window loads the address");
        Check.equal(server.url("/child"), request.getPanel().getPage(), "the address of the window");
        Check.equal(1, request.getPanel().getHistories().size(), "the first empty document is not in the history");
        final List<String> changes = Collections.synchronizedList(new ArrayList<String>());
        host.addPropertyChangeListener(SwingWebPanel.PROPERTY_POPUP_BLOCKING_ENABLED, new PropertyChangeListener() {
            /** Remembers the change. / 변경을 기억한다. */
            public void propertyChange(PropertyChangeEvent e) {
                changes.add(e.getOldValue() + ">" + e.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "" : "@other"));
            }
        });
        host.setPopupBlockingEnabled(false);
        host.setPopupBlockingEnabled(false);
        settle();
        Check.equal("[true>false]", changes.toString(), "the change is announced once on the event dispatch thread");
        Check.isFalse(host.isPopupBlockingEnabled(), "the blocker is off");
        host.refresh();
        awaitTitle(host, "timer:false", "with the blocker off a script opens a window without an action of the user");
        awaitSize(host.opened, 2, "the host gets that window");
        Check.isFalse(host.opened.get(1).getPanel().isPopupBlockingEnabled(), "a window takes the setting of its opener");
    }

    /**
     * C-POPUP-2: the opener holds the window object: it writes into the first empty document of the window at once, reads and changes
     * its document, navigates it, and the page of the window reaches its opener through <code>window.opener</code>. The first
     * document has the origin of the opener. The session storage of the window starts as a copy.
     * <p>
     * C-POPUP-2: 여는 창이 창 객체를 쥔다: 창의 처음 빈 문서에 곧바로 쓰고, 그 문서를 읽고 바꾸고, 이동시키며, 창의 페이지는
     * <code>window.opener</code> 로 여는 창에 닿는다. 처음 문서는 여는 창의 오리진을 가진다. 창의 sessionStorage 는 복사본으로 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_2_OpenerControlsTheWindow() throws Exception {
        server.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>child</title><p id=c>child</p><script>"
                + "var stored = sessionStorage.getItem('k'); sessionStorage.setItem('k', 'changed in child');"
                + "opener.document.title = 'child:' + (opener.w === window) + ':' + stored + ':' + opener.document.getElementById('o').textContent;"
                + "</" + "script>"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p id=o>opener text</p><script>"
                + "var w; sessionStorage.setItem('k', 'from opener');"
                + "window.addEventListener('load', function () { setTimeout(function () {"
                + " w = window.open('', 'named');"
                + " var first = [w !== null, w.closed, w.location.href, w.document.URL, w.opener === window, w.name, w.document.body !== null].join(',');"
                + " w.document.write('<title>written</title><p id=x>hello</p>'); w.document.close();"
                + " var x = w.document.getElementById('x'); x.textContent = 'changed by opener';"
                + " document.title = 'first:' + first + ':' + w.document.title + ':' + w.document.getElementById('x').textContent;"
                + "}, 50); });"
                + RUNNER + "function go() { w.location.href = '/child'; }"
                + "function report() { document.title = 'after:' + sessionStorage.getItem('k') + ':' + w.document.getElementById('c').textContent"
                + "   + ':' + (window.open('', 'named') === w) + ':' + w.length; }"
                + "</" + "script>"));
        server.start();
        host.setPopupBlockingEnabled(false);
        host.setPage(server.url("/p"));
        awaitTitle(host, "first:true,false,about:blank,about:blank,true,named,true:written:changed by opener",
                "the opener uses the first empty document of the window as soon as window.open returns");
        awaitSize(host.opened, 1, "the host gets the window");
        final SwingWebPanel popup = host.opened.get(0).getPanel();
        Check.equal("named", host.opened.get(0).getTargetName(), "the target name of the request");
        awaitTitle(popup, "written", "the component of the window shows what the opener wrote");
        Check.equal("about:blank", popup.getPage(), "the address of the first document");
        runScript(host, "go()");
        awaitTitle(host, "child:true:from opener:opener text", "the page of the window reaches its opener; its session storage started as a copy");
        awaitTitle(popup, "child", "the opener navigated the window");
        runScript(host, "report()");
        awaitTitle(host, "after:from opener:child:true:0", "the storages are separate afterwards; the name finds the same window again");
        Check.equal(1, host.opened.size(), "a second window.open with the same name opens no second window");
    }

    /**
     * C-POPUP-3: a window that a page opened is closed by its opener and by itself: <code>closed</code> is true at once, the host is
     * asked to close it, and the unload events run once. A window can be closed while it still loads. Closing such a window never
     * goes through the policy for the window of the component, and a popup cannot close the window of its opener through
     * <code>opener.close()</code> even when the host allowed scripts to close that window.
     * <p>
     * C-POPUP-3: 페이지가 연 창은 여는 창이, 그리고 자기 자신이 닫는다: <code>closed</code> 가 곧바로 true 이고, 호스트에 닫으라고 요청하며,
     * 언로드 이벤트가 한 번 실행된다. 창은 아직 읽는 중에도 닫을 수 있다. 그런 창을 닫는 일은 컴포넌트의 본 창에 대한 정책을 거치지 않고, 호스트가
     * 스크립트의 본 창 닫기를 허용했더라도 팝업이 <code>opener.close()</code> 로 여는 창을 닫을 수는 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_3_Closing() throws Exception {
        server.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>child</title><p>child</p><script>" + RUNNER
                + "window.addEventListener('pagehide', function () { opener.unloads = (opener.unloads || 0) + 1; });</" + "script>"));
        server.routeFixed("/slow", TestHttpServer.Response.html("<!doctype html><title>slow</title><p>slow</p>").delay(1500));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>" + RUNNER
                + "var a, b, c;"
                + "function openAll() { a = window.open('/child', 'a'); b = window.open('/child', 'b'); c = window.open('/slow', 'c');"
                + "  c.close(); document.title = 'opened:' + [a.closed, b.closed, c.closed].join(','); }"
                + "function closeA() { a.close(); a.close(); document.title = 'closedA:' + a.closed + ':' + b.closed; }"
                + "function report() { document.title = 'report:' + [a.closed, b.closed, c.closed, window.closed, window.unloads].join(','); }"
                + "</" + "script>"));
        server.start();
        host.setPopupBlockingEnabled(false);
        host.setRootWindowClosingEnabled(true);
        host.setPage(server.url("/p"));
        awaitTitle(host, "start", "the opener is loaded");
        runScript(host, "openAll()");
        awaitTitle(host, "opened:false,false,true", "a window that is closed while it loads is closed at once");
        awaitSize(host.opened, 3, "the host got all three windows");
        awaitSize(host.closed, 1, "the host is asked to close the window that was closed while loading");
        SwingWebPanel a = host.opened.get(0).getPanel();
        SwingWebPanel b = host.opened.get(1).getPanel();
        Check.same(host.opened.get(2).getPanel(), host.closed.get(0), "it is the third window");
        awaitTitle(a, "child", "the first window is loaded");
        awaitTitle(b, "child", "the second window is loaded");
        runScript(host, "closeA()");
        awaitTitle(host, "closedA:true:false", "the opener closes a window: closed is true at once, the other window stays");
        awaitSize(host.closed, 2, "the host is asked to close it, once");
        Check.same(a, host.closed.get(1), "it is the first window");
        runScript(b, "opener.close(); window.close();");
        awaitSize(host.closed, 3, "a window closes itself");
        Check.same(b, host.closed.get(2), "it is the second window");
        runScript(host, "report()");
        awaitTitle(host, "report:true,true,true,false,2", "all windows are closed, the opener is not, and each loaded window ran its unload events once");
        Check.equal(0, host.rootClosed, "closing such windows never uses the policy for the window of the component");
        Check.isTrue(a.isDisposed() && b.isDisposed(), "the host disposed the components");
    }

    /**
     * C-POPUP-4: a window opened with <code>noopener</code> has no connection to the page that opened it: the call gives null, but
     * the window is opened. A window of another origin is reached only through the members that are allowed across origins:
     * <code>closed</code>, <code>postMessage</code>, <code>close</code>, and its page reaches its opener the same way.
     * <p>
     * C-POPUP-4: <code>noopener</code> 로 연 창은 그것을 연 페이지와 관계가 없다: 호출은 null 을 주지만 창은 열린다. 다른 오리진의 창에는 교차
     * 오리진에 허용된 멤버로만 닿는다: <code>closed</code>, <code>postMessage</code>, <code>close</code>. 그 창의 페이지도 같은 방식으로 여는
     * 창에 닿는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_4_NoOpenerAndOtherOrigins() throws Exception {
        TestHttpServer other = new TestHttpServer();
        try {
            other.routeFixed("/far", TestHttpServer.Response.html("<!doctype html><title>far</title><p>far</p><script>"
                    + "var seen = 'none'; try { seen = String(opener.document); } catch (e) { seen = e.name; }"
                    + "window.addEventListener('message', function (e) { document.title = 'far got:' + e.data + ':' + (e.source === opener);"
                    + "  e.source.postMessage('answer ' + seen, e.origin); });"
                    + "opener.postMessage('far ready', '*');</" + "script>"));
            other.start();
            server.routeFixed("/alone", TestHttpServer.Response.html("<!doctype html><title>alone</title><script>"
                    + "var seen; try { seen = window.opener === null ? 'null' : typeof window.opener.postMessage + String(window.opener.document); }"
                    + "catch (e) { seen = e.name; } document.title = 'alone:' + seen;</" + "script>"));
            server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>" + RUNNER
                    + "var far, log = [];"
                    + "window.addEventListener('message', function (e) { if (typeof e.data === 'string' && e.data.indexOf('run:') === 0) { return; }"
                    + "  log.push(e.data + '@' + (e.origin === '" + originOf(other) + "') + ':' + (e.source === far));"
                    + "  if (e.data === 'far ready') { far.postMessage('hello far', '*'); } else { finish(); } });"
                    + "function alone() { var w = window.open('/alone', '_blank', 'noopener'); document.title = 'noopener:' + (w === null); }"
                    + "function openFar() { far = window.open('" + other.url("/far") + "'); }"
                    + "function finish() { var doc = 'none'; try { doc = String(far.document); } catch (e) { doc = e.name; }"
                    + "  var loc = 'none'; try { loc = String(far.location.href); } catch (e) { loc = e.name; }"
                    + "  document.title = 'done:' + log.join('|') + ':' + doc + ':' + loc + ':' + far.closed; }"
                    + "</" + "script>"));
            server.start();
            host.setPopupBlockingEnabled(false);
            host.setPage(server.url("/p"));
            awaitTitle(host, "start", "the opener is loaded");
            runScript(host, "alone()");
            awaitTitle(host, "noopener:true", "window.open with noopener gives null");
            awaitSize(host.opened, 1, "but the window is opened");
            awaitTitle(host.opened.get(0).getPanel(), "alone:SecurityError",
                    "its page does not reach the page that opened it: its opener is the window of the Java host, as in a component of the host");
            runScript(host, "openFar()");
            awaitTitle(host, "done:far ready@true:true|answer SecurityError@true:true:SecurityError:SecurityError:false",
                    "messages go both ways between the origins; the documents and the address of the other window are not readable");
            awaitTitle(host.opened.get(1).getPanel(), "far got:hello far:true", "the page of the other origin got the message from its opener");
            runScript(host, "far.close(); document.title = 'closing:' + far.closed");
            awaitTitle(host, "closing:true", "the opener may close a window of another origin that it opened");
            awaitSize(host.closed, 1, "the host is asked to close it");
        } finally {
            other.stop();
        }
    }

    /**
     * Returns the origin of a server as scripts see it.
     * <p>
     * 스크립트가 보는 서버의 오리진을 반환한다.
     *
     * @param s the server / 서버
     * @return the origin / 오리진
     */
    private static String originOf(TestHttpServer s) {
        String url = s.url("/");
        return url.substring(0, url.length() - 1);
    }

    /**
     * C-POPUP-5: a link or a form whose target needs a new window is offered to the host. A host that does not take it gets the
     * behavior of before: the link or form uses the context that it is in. A host that takes it gets a component that loads the
     * address or receives the form; a form that a script submits without an action of the user is blocked then.
     * <p>
     * C-POPUP-5: 대상이 새 창을 필요로 하는 링크나 폼은 호스트에 제안된다. 받지 않는 호스트는 전과 같은 동작을 얻는다: 링크나 폼이 자기가 있는
     * 문맥을 쓴다. 받는 호스트는 주소를 읽거나 폼을 받는 컴포넌트를 얻는다. 그때 스크립트가 사용자의 조작 없이 제출한 폼은 차단된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_5_LinksAndFormsThatNeedAWindow() throws Exception {
        server.routeFixed("/target", TestHttpServer.Response.html("<!doctype html><title>target</title><p>target</p>"));
        server.route("/echo", new TestHttpServer.Handler() {
            /** Shows the method and the body of the request in the title. / 요청의 메서드와 본문을 제목에 보인다. */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                return TestHttpServer.Response.html("<!doctype html><title>echo:" + request.method + ":" + request.bodyText() + "</title><p>echo</p>");
            }
        });
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title>" + rows(
                "<a class=row href='/target' target='_blank'>new window</a>",
                "<form class=row method=post action='/echo' target='_blank'><input type=hidden name=k value=v><input type=submit value=send></form>")
                + "<script>" + RUNNER + "function sendByScript() { document.forms[0].submit(); document.title = 'sent'; }</" + "script>"));
        server.start();
        host.setPage(server.url("/p"));
        awaitTitle(host, "start", "the page is loaded");
        click(host, 0, MouseEvent.BUTTON1, 0);
        awaitTitle(host, "target", "a host that opens no windows gets the old behavior: the link navigates its own context");
        Check.equal(1, host.offered.size(), "the link was offered to the host first");
        Check.equal(WindowRequest.LINK, host.offered.get(0).getKind(), "as a link request");
        Check.isFalse(host.offered.get(0).hasPanel(), "no component was created for a request that was not taken");
        host.takeLinks = true;
        host.back();
        awaitTitle(host, "start", "back on the first page");
        click(host, 0, MouseEvent.BUTTON1, 0);
        awaitSize(host.offered, 2, "the link is offered again");
        SwingWebPanel linked = host.offered.get(1).getPanel();
        Check.notNull(linked, "the host got a component for the new window");
        awaitTitle(linked, "target", "the new window loads the address of the link");
        Check.equal("start", host.getTitle(), "the page of the link stays where it is");
        Check.equal(1, linked.getHistories().size(), "the new window starts its history with that address");
        click(host, 1, MouseEvent.BUTTON1, 0);
        awaitSize(host.offered, 3, "the form is offered");
        awaitTitle(host.offered.get(2).getPanel(), "echo:POST:k=v", "the new window receives the form with its body");
        Check.equal("start", host.getTitle(), "the page of the form stays where it is");
        runScript(host, "sendByScript()");
        awaitTitle(host, "sent", "the script submitted the form");
        awaitSize(host.blocked, 1, "a form that a script submits into a new window without an action of the user is blocked");
        Check.isTrue(host.offered.get(3).isBlocked() && !host.offered.get(3).hasPanel(), "the request is marked as blocked and has no component");
        Thread.sleep(300);
        Check.equal("sent", host.getTitle(), "and the page is not navigated instead");
        host.takeLinks = false;
        runScript(host, "sendByScript()");
        awaitTitle(host, "echo:POST:k=v", "a host that opens no windows gets the old behavior for the form as well");
    }

    /**
     * C-POPUP-6: the user chooses where a link opens: Ctrl+click and a click of the middle button ask for a new tab, Shift+click for a
     * new window. A host that takes the request opens the address itself and the page stays; otherwise a click with a modifier is an
     * ordinary click and a middle click does nothing. Such a window was made by the host: its page cannot close it.
     * <p>
     * C-POPUP-6: 사용자가 링크를 열 곳을 고른다: Ctrl+클릭과 가운데 단추 클릭은 새 탭을, Shift+클릭은 새 창을 요청한다. 요청을 받는 호스트는
     * 주소를 직접 열고 페이지는 그대로이다. 아니면 보조 키를 누른 클릭은 보통의 클릭이고 가운데 클릭은 아무 일도 하지 않는다. 그런 창은 호스트가
     * 만든 것이다: 그 페이지가 닫을 수 없다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_6_ChoiceOfTheUser() throws Exception {
        server.routeFixed("/target", TestHttpServer.Response.html("<!doctype html><title>target</title><p>target</p><script>"
                + "window.close(); document.title = 'target:' + window.closed;</" + "script>"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title>"
                + rows("<a class=row href='/target'>plain link</a>", "<a class=row href='/target' onclick='event.preventDefault()'>cancelled</a>")
                + "<script>document.links[1].addEventListener('auxclick', function (e) { e.preventDefault(); });</" + "script>"));
        server.start();
        host.takeLinks = true;
        host.setPage(server.url("/p"));
        awaitTitle(host, "start", "the page is loaded");
        click(host, 0, MouseEvent.BUTTON1, InputEvent.CTRL_MASK);
        awaitSize(host.offered, 1, "Ctrl+click offers the link");
        Check.equal(WindowRequest.USER, host.offered.get(0).getKind(), "as a choice of the user");
        Check.equal(WindowRequest.PLACE_TAB, host.offered.get(0).getPlace(), "for a new tab");
        Check.equal(server.url("/target"), host.offered.get(0).getUrl(), "with the address of the link");
        awaitTitle(host.offered.get(0).getPanel(), "target:false", "the window loads the address; its page cannot close a window that the host made");
        click(host, 0, MouseEvent.BUTTON1, InputEvent.SHIFT_MASK);
        awaitSize(host.offered, 2, "Shift+click offers the link");
        Check.equal(WindowRequest.PLACE_WINDOW, host.offered.get(1).getPlace(), "for a new window");
        click(host, 0, MouseEvent.BUTTON2, 0);
        awaitSize(host.offered, 3, "a click of the middle button offers the link");
        Check.equal(WindowRequest.PLACE_TAB, host.offered.get(2).getPlace(), "for a new tab");
        click(host, 1, MouseEvent.BUTTON1, InputEvent.CTRL_MASK);
        click(host, 1, MouseEvent.BUTTON2, 0);
        Thread.sleep(300);
        settle();
        Check.equal(3, host.offered.size(), "a cancelled click is not offered: click for Ctrl+click, auxclick for the middle button");
        Check.equal("start", host.getTitle(), "the page stayed through all of it");
        host.takeLinks = false;
        click(host, 0, MouseEvent.BUTTON2, 0);
        awaitSize(host.offered, 4, "the middle click is offered to a host that does not take it");
        Thread.sleep(300);
        Check.equal("start", host.getTitle(), "and then does nothing");
        click(host, 0, MouseEvent.BUTTON1, InputEvent.CTRL_MASK);
        awaitTitle(host, "target:false", "a Ctrl+click that the host does not take is an ordinary click");
    }

    /**
     * C-POPUP-7: the life of the windows. A window lives on when its opener is disposed, and sees its opener as closed. A window that
     * the host declines is closed for the script. Closing a window keeps the process-wide localStorage. A component that cannot show
     * windows gives null without a blocked notice. The document threads end when all windows of a group are gone.
     * <p>
     * C-POPUP-7: 창의 수명. 여는 창이 폐기되어도 창은 남고, 여는 창을 닫힌 것으로 본다. 호스트가 거절한 창은 스크립트에게 닫힌 창이다. 창을
     * 닫아도 프로세스 공용 localStorage 는 남는다. 창을 보일 수 없는 컴포넌트는 차단 알림 없이 null 을 준다. 그룹의 모든 창이 사라지면 문서
     * 스레드가 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_7_LifeOfTheWindows() throws Exception {
        server.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>child</title><p>child</p><script>" + RUNNER
                + "var ticks = 0; setInterval(function () { ticks++; document.title = 'child:' + (ticks > 3) + ':' + opener.closed; }, 30);"
                + "</" + "script>"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>" + RUNNER
                + "var w; localStorage.setItem('popup-test', 'kept');"
                + "function open1() { w = window.open('/child'); document.title = 'open:' + (w !== null); }"
                + "function check() { document.title = 'check:' + w.closed + ':' + localStorage.getItem('popup-test'); }"
                + "</" + "script>"));
        server.start();
        final int before = documentThreads();
        host.setPopupBlockingEnabled(false);
        host.rejectScripts = true;
        host.setPage(server.url("/p"));
        awaitTitle(host, "start", "the opener is loaded");
        runScript(host, "open1()");
        awaitTitle(host, "open:true", "window.open returns the window before the host decides");
        awaitSize(host.opened, 1, "the host got the window");
        Check.isTrue(host.opened.get(0).isRejected() && host.opened.get(0).getPanel() == null, "the host declined it");
        runScript(host, "check()");
        awaitTitle(host, "check:true:kept", "a declined window is closed for the script; closing it kept localStorage");
        host.rejectScripts = false;
        runScript(host, "open1()");
        awaitSize(host.opened, 2, "a second window is opened");
        final SwingWebPanel popup = host.opened.get(1).getPanel();
        awaitTitle(popup, "child:true:false", "the window runs its timers and sees its opener");
        final Host second = createHost();
        try {
            second.setPopupBlockingEnabled(false);
            second.setPage(server.url("/p"));
            awaitTitle(second, "start", "another component is loaded");
            host.dispose();
            awaitTitle(popup, "child:true:true", "the window lives on when its opener is disposed and sees it as closed");
            popup.setPage(server.url("/p"));
            awaitTitle(popup, "start", "and can still navigate");
            Check.isTrue(popup.getLocalStorage().size() > 0, "disposing the opener with dispose() clears localStorage, the new page stored again");
            popup.disposeKeepingStorage();
            runScript(second, "document.title = 'second:' + localStorage.getItem('popup-test')");
            awaitTitle(second, "second:kept", "disposeKeepingStorage leaves localStorage to the other components");
            Check.throwsException(IllegalStateException.class, new Block() {
                /** Changes a disposed component. / 폐기된 컴포넌트를 바꾼다. */
                public void run() {
                    popup.setPopupBlockingEnabled(true);
                }
            }, "a disposed component refuses the setting");
        } finally {
            second.dispose();
        }
        long end = System.currentTimeMillis() + 15000;
        // 문서 스레드는 유휴 시간이 지나면 끝난다. 시험 전의 수로 돌아올 때까지 기다린다.
        while (documentThreads() > before && System.currentTimeMillis() < end) {
            Thread.sleep(100);
        }
        Check.isTrue(documentThreads() <= before, "the document threads of the windows ended: " + documentThreads() + " > " + before);
    }

    /**
     * C-POPUP-8: the limits. A component that cannot show windows gives null without a blocked notice. A script that opens a window
     * while its document is still parsed gets null, but the window opens without a connection to it. A sandboxed frame opens a window
     * only if its windows may leave the sandbox; otherwise the call gives null and a link uses its own frame, as before.
     * <p>
     * C-POPUP-8: 한계. 창을 보일 수 없는 컴포넌트는 차단 알림 없이 null 을 준다. 문서를 아직 읽는 중에 창을 연 스크립트는 null 을 받지만, 창은
     * 그것과 관계없이 열린다. 샌드박스 프레임은 그 창이 샌드박스를 벗어나도 될 때만 창을 연다. 아니면 호출은 null 을 주고 링크는 전처럼 자기
     * 프레임을 쓴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_8_Limits() throws Exception {
        server.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>child</title><p>child</p><script>"
                + "var seen; try { seen = String(opener.document); } catch (e) { seen = e.name; } document.title = 'child:' + seen;</" + "script>"));
        server.routeFixed("/parse", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>"
                + "var w = window.open('/child'); document.title = 'parse:' + (w === null);</" + "script>"));
        server.routeFixed("/inner", TestHttpServer.Response.html("<!doctype html><p>inner</p><script>"
                + "var w = window.open('/child'); parent.postMessage('inner:' + (w === null), '*');</" + "script>"));
        server.routeFixed("/kept", TestHttpServer.Response.html("<!doctype html><title>start</title>"
                + "<iframe sandbox='allow-scripts allow-popups' src='/inner'></iframe>"
                + "<script>window.addEventListener('message', function (e) { document.title = String(e.data); });</" + "script>"));
        server.routeFixed("/escape", TestHttpServer.Response.html("<!doctype html><title>start</title>"
                + "<iframe sandbox='allow-scripts allow-popups allow-popups-to-escape-sandbox' src='/inner'></iframe>"
                + "<script>window.addEventListener('message', function (e) { document.title = String(e.data); });</" + "script>"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>" + RUNNER
                + "function open1() { var w = window.open('/child'); document.title = 'open:' + (w === null); }</" + "script>"));
        server.start();
        host.setPopupBlockingEnabled(false);
        host.cannotShow = true;
        host.setPage(server.url("/p"));
        awaitTitle(host, "start", "the page is loaded");
        runScript(host, "open1()");
        awaitTitle(host, "open:true", "a component that cannot show windows gives null");
        settle();
        Check.isTrue(host.opened.isEmpty() && host.blocked.isEmpty(), "no window, and no blocked notice either");
        host.cannotShow = false;
        host.setPage(server.url("/parse"));
        awaitTitle(host, "parse:true", "a script that runs while its document is parsed gets no window object");
        awaitSize(host.opened, 1, "but the window is opened");
        awaitTitle(host.opened.get(0).getPanel(), "child:SecurityError", "without a connection to the page that opened it");
        host.setPage(server.url("/kept"));
        awaitTitle(host, "inner:true", "a sandboxed frame whose windows would have to stay sandboxed gets no window");
        settle();
        Check.equal(1, host.opened.size(), "and none is opened");
        host.setPage(server.url("/escape"));
        awaitTitle(host, "inner:false", "a sandboxed frame whose windows may leave the sandbox gets a window");
        awaitSize(host.opened, 2, "the host got it");
        awaitTitle(host.opened.get(1).getPanel(), "child:SecurityError",
                "the window is not sandboxed: its script runs; its opener has an opaque origin, so it is not readable");
    }

    /**
     * C-POPUP-9: the Cross-Origin-Opener-Policy of a response cuts the connection between a window and its opener: the opener sees
     * the window as closed and cannot message, navigate or close it, the page of the window has no opener, and a target name does not
     * find the window any more. A window without such a response keeps its connection.
     * <p>
     * C-POPUP-9: 응답의 Cross-Origin-Opener-Policy 가 창과 여는 창의 관계를 끊는다: 여는 창은 그 창을 닫힌 것으로 보고 메시지를 보내거나
     * 이동시키거나 닫을 수 없으며, 그 창의 페이지에는 opener 가 없고, 대상 이름으로도 그 창을 찾지 못한다. 그런 응답이 없는 창은 관계를 유지한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_9_OpenerPolicyCutsTheConnection() throws Exception {
        String page = "<!doctype html><title>loading</title><p>window</p><script>" + RUNNER
                + "var got = 'none'; window.addEventListener('message', function (e) { if (String(e.data).indexOf('run:') !== 0) { got = e.data; } });"
                + "var seen; try { seen = String(opener.document !== undefined); } catch (e) { seen = e.name; } document.title = 'window:' + seen;"
                + "</" + "script>";
        server.routeFixed("/plain", TestHttpServer.Response.html(page));
        server.routeFixed("/isolated", TestHttpServer.Response.html(page).header("Cross-Origin-Opener-Policy", "same-origin"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>" + RUNNER
                + "var a, b;"
                + "function openBoth() { a = window.open('/plain', 'a'); b = window.open('/isolated', 'b'); document.title = 'opened'; }"
                + "function look() { var again = window.open('', 'a') === a; b.postMessage('hello', '*'); a.postMessage('hello', '*');"
                + "  try { b.location = '/plain'; } catch (e) { } b.close();"
                + "  document.title = 'look:' + a.closed + ':' + b.closed + ':' + again; }"
                + "</" + "script>"));
        server.start();
        host.setPopupBlockingEnabled(false);
        host.setPage(server.url("/p"));
        awaitTitle(host, "start", "the opener is loaded");
        runScript(host, "openBoth()");
        awaitSize(host.opened, 2, "both windows are opened");
        SwingWebPanel plain = host.opened.get(0).getPanel();
        SwingWebPanel isolated = host.opened.get(1).getPanel();
        awaitTitle(plain, "window:true", "a window without the policy reads its opener");
        awaitTitle(isolated, "window:SecurityError", "a window with the policy has no opener but the window of the Java host");
        runScript(host, "look()");
        awaitTitle(host, "look:false:true:true", "the opener sees the window with the policy as closed; the other one is found by its name");
        runScript(plain, "document.title = 'plain got:' + got");
        awaitTitle(plain, "plain got:hello", "the window without the policy gets the message");
        runScript(isolated, "document.title = 'isolated got:' + got + ':' + location.pathname");
        awaitTitle(isolated, "isolated got:none:/isolated", "the window with the policy gets no message and is not navigated");
        settle();
        Check.isTrue(host.closed.isEmpty() && !isolated.isDisposed(), "and it is not closed either");
    }

    /**
     * C-POPUP-10: a component whose host changes nothing shows a window that a script opened in a modeless dialog of its own: the
     * dialog belongs to the window of the component, its content has the size that the page asked for, its title follows the page,
     * and it is closed by <code>window.close()</code>, by the user and together with its owner; the component of the window is
     * disposed then, and the opener sees the window as closed. It needs a display.
     * <p>
     * C-POPUP-10: 호스트가 아무것도 바꾸지 않은 컴포넌트는 스크립트가 연 창을 자기 비모달 대화 상자에 보인다: 대화 상자는 컴포넌트의 창에 속하고,
     * 내용은 페이지가 요청한 크기이며, 제목은 페이지를 따르고, <code>window.close()</code>, 사용자, 그리고 소유자와 함께 닫힌다. 그때 창의
     * 컴포넌트가 폐기되고 여는 창은 그 창을 닫힌 것으로 본다. 화면이 필요하다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_POPUP_10_DefaultPopupWindow() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            Check.skip("the default popup window needs a display");
            return;
        }
        server.routeFixed("/child", TestHttpServer.Response.html("<!doctype html><title>child</title><p>child</p><script>" + RUNNER + "</" + "script>"));
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>start</title><p>opener</p><script>" + RUNNER
                + "var list = [];"
                + "function open1() { list.push(window.open('/child', '_blank', 'width=300,height=200,left=40,top=40')); document.title = 'open:' + list.length; }"
                + "function look() { var out = []; for (var i = 0; i < list.length; i++) { out.push(list[i].closed); } document.title = 'look:' + out.join(','); }"
                + "</" + "script>"));
        server.start();
        final SwingWebPanel[] made = new SwingWebPanel[1];
        final JFrame[] frames = new JFrame[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Shows a plain component in a frame. / 보통의 컴포넌트를 프레임에 보인다. */
            public void run() {
                made[0] = new SwingWebPanel();
                JFrame frame = new JFrame("popup test");
                frame.setFocusableWindowState(false);
                frame.getContentPane().add(new JScrollPane(made[0]));
                frame.setSize(360, 240);
                frame.setLocation(20, 20);
                frame.setVisible(true);
                frames[0] = frame;
            }
        });
        final SwingWebPanel plain = made[0];
        final JFrame frame = frames[0];
        try {
            plain.setPopupBlockingEnabled(false);
            plain.setPage(server.url("/p"));
            awaitTitle(plain, "start", "the opener is loaded");
            runScript(plain, "open1()");
            awaitTitle(plain, "open:1", "the script opened a window");
            final JDialog first = awaitDialog(frame, 1);
            final SwingWebPanel inner = panelOf(first);
            awaitTitle(inner, "child", "the window loads its page");
            long end = System.currentTimeMillis() + 5000;
            // 제목 표시줄이 페이지의 제목을 따를 때까지 기다린다.
            while (!"child".equals(first.getTitle()) && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.equal("child", first.getTitle(), "the title bar shows the title of the page");
            Check.equal(Dialog.ModalityType.MODELESS, first.getModalityType(), "the dialog is modeless");
            Check.same(frame, first.getOwner(), "it belongs to the window of the component");
            Check.equal(300, first.getContentPane().getWidth(), "the content has the width that the page asked for");
            Check.equal(200, first.getContentPane().getHeight(), "and the height");
            runScript(inner, "window.close()");
            end = System.currentTimeMillis() + 5000;
            // 스크립트의 닫기가 대화 상자를 닫을 때까지 기다린다.
            while (first.isDisplayable() && System.currentTimeMillis() < end) {
                Thread.sleep(20);
            }
            Check.isFalse(first.isDisplayable(), "window.close() closes the dialog");
            Check.isTrue(inner.isDisposed(), "and disposes the component of the window");
            runScript(plain, "open1()");
            awaitTitle(plain, "open:2", "a second window");
            final JDialog second = awaitDialog(frame, 1);
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Closes the dialog as the user does. / 사용자가 하듯 대화 상자를 닫는다. */
                public void run() {
                    second.dispatchEvent(new java.awt.event.WindowEvent(second, java.awt.event.WindowEvent.WINDOW_CLOSING));
                }
            });
            runScript(plain, "open1()");
            awaitTitle(plain, "open:3", "a third window");
            final JDialog third = awaitDialog(frame, 1);
            Check.isTrue(panelOf(second).isDisposed(), "closing the dialog disposes the component of the window");
            runScript(plain, "look()");
            awaitTitle(plain, "look:true,true,false", "the opener sees the closed windows as closed");
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Closes the window of the opener. / 여는 창의 창을 닫는다. */
                public void run() {
                    frame.dispose();
                }
            });
            settle();
            Check.isFalse(third.isDisplayable(), "the dialog is closed together with the window that owns it");
            Check.isTrue(panelOf(third).isDisposed(), "and its component is disposed");
            Check.isFalse(plain.isDisposed(), "the component of the opener is left to its host");
        } finally {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Removes the windows of the test. / 시험의 창을 치운다. */
                public void run() {
                    frame.dispose();
                    plain.dispose();
                }
            });
        }
    }

    /**
     * Waits until a window owns a number of showing dialogs and returns the newest one.
     * <p>
     * 창이 보이는 대화 상자를 정해진 수만큼 가질 때까지 기다리고 가장 새 것을 반환한다.
     *
     * @param owner the owning window / 소유한 창
     * @param count the number of showing dialogs / 보이는 대화 상자의 수
     * @return the newest showing dialog / 가장 새로 보인 대화 상자
     * @throws Exception on failure / 실패 시
     */
    private static JDialog awaitDialog(final Window owner, final int count) throws Exception {
        final JDialog[] found = new JDialog[1];
        long end = System.currentTimeMillis() + 10000;
        // 대화 상자가 보일 때까지 짧게 쉬며 기다린다.
        while (found[0] == null && System.currentTimeMillis() < end) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Looks at the owned windows. / 소유한 창을 본다. */
                public void run() {
                    Window[] owned = owner.getOwnedWindows();
                    int showing = 0;
                    JDialog last = null;
                    // 보이는 대화 상자를 센다.
                    for (int i = 0; i < owned.length; i++) {
                        if (owned[i] instanceof JDialog && owned[i].isShowing()) {
                            showing++;
                            last = (JDialog) owned[i];
                        }
                    }
                    if (showing == count) {
                        found[0] = last;
                    }
                }
            });
            if (found[0] == null) {
                Thread.sleep(20);
            }
        }
        Check.notNull(found[0], "the component shows " + count + " popup dialog(s)");
        return found[0];
    }

    /**
     * Returns the component that a popup dialog shows.
     * <p>
     * 팝업 대화 상자가 보이는 컴포넌트를 반환한다.
     *
     * @param dialog the dialog / 대화 상자
     * @return the component / 컴포넌트
     */
    private static SwingWebPanel panelOf(JDialog dialog) {
        return (SwingWebPanel) ((JScrollPane) dialog.getContentPane()).getViewport().getView();
    }

    /**
     * Creates another host on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 다른 호스트를 만든다.
     *
     * @return the host / 호스트
     * @throws Exception on failure / 실패 시
     */
    private static Host createHost() throws Exception {
        final Host[] made = new Host[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                made[0] = new Host();
            }
        });
        return made[0];
    }

    /**
     * Counts the live document threads of the process.
     * <p>
     * 프로세스의 살아 있는 문서 스레드를 센다.
     *
     * @return the number of threads / 스레드 수
     */
    private static int documentThreads() {
        int n = 0;
        // 스레드마다 이름을 본다.
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (t.isAlive() && t.getName().startsWith("swingwebpanel-document-")) {
                n++;
            }
        }
        return n;
    }

    /**
     * Runs a script in the page of a component through a message that the page evaluates.
     * <p>
     * 페이지가 평가하는 메시지를 통해 컴포넌트의 페이지에서 스크립트를 실행한다.
     *
     * @param p    the component / 컴포넌트
     * @param code the script / 스크립트
     * @throws Exception on failure / 실패 시
     */
    private static void runScript(final SwingWebPanel p, final String code) throws Exception {
        p.postMessage("run:" + code, "*");
    }
}
