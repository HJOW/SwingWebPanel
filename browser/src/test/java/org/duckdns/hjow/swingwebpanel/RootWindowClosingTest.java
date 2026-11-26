package org.duckdns.hjow.swingwebpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The setting that lets a page close the window of the component (plan items P30-01 to P30-03, API contract 5.23, cases C-ROOTCLOSE-1 to
 * 6): a real component whose {@link SwingWebPanel#closeRootWindow()} is replaced by a recorder, real HTTP servers of two origins, and
 * pages that call <code>window.close()</code> from the top-level document and from frames. The default implementation, which ends the
 * program, is checked in a JVM of its own by <code>RootWindowClosingExitMain</code>.
 * <p>
 * 페이지가 컴포넌트의 창을 닫게 하는 설정의 시험(계획 P30-01~P30-03, API 계약 5.23, 사례 C-ROOTCLOSE-1~6): {@link SwingWebPanel#closeRootWindow()}
 * 를 기록기로 바꾼 실제 컴포넌트, 오리진이 다른 두 실제 HTTP 서버, 최상위 문서와 프레임에서 <code>window.close()</code> 를 부르는 페이지를 쓴다.
 * 프로그램을 끝내는 기본 구현은 <code>RootWindowClosingExitMain</code> 이 별도 JVM 에서 확인한다.
 */
public class RootWindowClosingTest {

    /**
     * A component that records the calls of {@link SwingWebPanel#closeRootWindow()} instead of ending the program.
     * <p>
     * 프로그램을 끝내는 대신 {@link SwingWebPanel#closeRootWindow()} 호출을 기록하는 컴포넌트.
     */
    private static final class Recorder extends SwingWebPanel {

        /** Serialization version. / 직렬화 버전. */
        private static final long serialVersionUID = 1L;

        /** The number of calls. / 호출 횟수. */
        private int calls;

        /** Whether every call ran on the event dispatch thread. / 모든 호출이 이벤트 디스패치 스레드에서 실행되었는지. */
        private boolean onEdt = true;

        /**
         * The cookies of the component at each call. The pages of this suite write what they did into cookies, because the cookie store
         * changes on the thread of the script at once, while console messages reach the event dispatch thread later.
         * <p>
         * 호출마다의 컴포넌트 쿠키. 이 스위트의 페이지는 한 일을 쿠키에 적는다. 쿠키 저장소는 스크립트의 스레드에서 곧바로 바뀌지만 콘솔 메시지는
         * 이벤트 디스패치 스레드에 나중에 도착하기 때문이다.
         */
        private final List<String> seenAtClose = new ArrayList<String>();

        /**
         * Records the call.
         * <p>
         * 호출을 기록한다.
         */
        protected void closeRootWindow() {
            String cookies = getCookies();
            synchronized (this) {
                calls++;
                onEdt = onEdt && SwingUtilities.isEventDispatchThread();
                seenAtClose.add(cookies);
            }
        }

        /**
         * Returns the number of calls so far.
         * <p>
         * 지금까지의 호출 횟수를 반환한다.
         *
         * @return the count / 횟수
         */
        synchronized int calls() {
            return calls;
        }

        /**
         * Tells whether every call ran on the event dispatch thread.
         * <p>
         * 모든 호출이 이벤트 디스패치 스레드에서 실행되었는지 알려 준다.
         *
         * @return true if so / 그렇다면 true
         */
        synchronized boolean allOnEdt() {
            return onEdt;
        }

        /**
         * Returns the cookies that the component had when the window was closed the n-th time.
         * <p>
         * 창이 n 번째로 닫힐 때 컴포넌트가 가지고 있던 쿠키를 반환한다.
         *
         * @param n the number of the call, from 1 / 호출 번호(1부터)
         * @return the cookies / 쿠키
         */
        synchronized String seenAtClose(int n) {
            return seenAtClose.get(n - 1);
        }
    }

    /**
     * Collects the console messages of the component with their levels.
     * <p>
     * 컴포넌트의 콘솔 메시지를 수준과 함께 모은다.
     */
    private static final class Collector implements ConsoleListener {

        /** The entries "level:message" in arrival order. / 도착 순서대로의 "수준:메시지" 항목. */
        private final List<String> entries = new ArrayList<String>();

        /**
         * Keeps the message.
         * <p>
         * 메시지를 보관한다.
         *
         * @param event the event / 이벤트
         */
        public void consoleMessage(ConsoleEvent event) {
            synchronized (this) {
                entries.add(event.getLevel() + ":" + event.getMessage());
            }
        }

        /**
         * Returns the entries joined by "|".
         * <p>
         * 항목을 "|" 로 이어 반환한다.
         *
         * @return the entries / 항목
         */
        synchronized String messages() {
            StringBuilder sb = new StringBuilder();
            // 항목마다 구분자를 넣어 이어 붙인다.
            for (int i = 0; i < entries.size(); i++) {
                sb.append(i > 0 ? "|" : "").append(entries.get(i));
            }
            return sb.toString();
        }

        /**
         * Counts the entries that contain a text.
         * <p>
         * 문자열을 포함한 항목의 수를 센다.
         *
         * @param text the text / 문자열
         * @return the count / 개수
         */
        synchronized int count(String text) {
            int n = 0;
            // 항목마다 문자열이 들어 있는지 본다.
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).indexOf(text) >= 0) {
                    n++;
                }
            }
            return n;
        }
    }

    /** The server of the first origin. / 첫째 오리진의 서버. */
    private TestHttpServer server;

    /** The server of the other origin. / 다른 오리진의 서버. */
    private TestHttpServer other;

    /** The component of the case. / 사례의 컴포넌트. */
    private Recorder panel;

    /** The console messages of the case. / 사례의 콘솔 메시지. */
    private Collector console;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public RootWindowClosingTest() {
    }

    /**
     * Creates the servers and the component.
     * <p>
     * 서버와 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        other = new TestHttpServer();
        final Recorder[] made = new Recorder[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component on the EDT. / EDT 에서 컴포넌트를 만든다. */
            public void run() {
                made[0] = new Recorder();
                made[0].setSize(400, 300);
            }
        });
        panel = made[0];
        console = new Collector();
        panel.addConsoleListener(console);
    }

    /**
     * Disposes the component and stops the servers.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        panel.dispose();
        server.stop();
        other.stop();
    }

    /**
     * Serves a page with a script on the first server.
     * <p>
     * 첫째 서버에서 스크립트가 든 페이지를 제공한다.
     *
     * @param path   the path / 경로
     * @param script the script text / 스크립트 텍스트
     */
    private void page(String path, String script) {
        server.routeFixed(path, TestHttpServer.Response.html(html("", script)));
    }

    /**
     * Builds a document with a body and a script that runs after the body was parsed.
     * <p>
     * 본문과, 본문을 파싱한 뒤 실행되는 스크립트가 든 문서를 만든다.
     *
     * @param body   the markup of the body / 본문 마크업
     * @param script the script text / 스크립트 텍스트
     * @return the document / 문서
     */
    private static String html(String body, String script) {
        return "<!doctype html>\n<title>t</title>\n<body>" + body + "\n<script>\n" + script + "\n</" + "script>";
    }

    /**
     * Loads a page of the first server and waits until the component confirmed it.
     * <p>
     * 첫째 서버의 페이지를 불러 컴포넌트가 확정할 때까지 기다린다.
     *
     * @param path the path / 경로
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void load(String path) throws Exception {
        String url = server.url(path);
        panel.setPage(url);
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정될 때까지 짧게 쉬며 기다린다.
        while (!url.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "confirmed page " + path);
    }

    /**
     * Waits until a console message that contains a text arrived.
     * <p>
     * 문자열을 포함한 콘솔 메시지가 도착할 때까지 기다린다.
     *
     * @param text the text / 문자열
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitMessage(String text) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 메시지가 올 때까지 짧게 쉬며 기다린다.
        while (console.count(text) == 0 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(console.count(text) > 0, "expected a console message with '" + text + "' but got: " + console.messages());
    }

    /**
     * Waits until the window was closed a number of times.
     * <p>
     * 창이 일정 횟수 닫힐 때까지 기다린다.
     *
     * @param n the number of calls / 호출 횟수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitCloses(int n) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 호출 횟수에 이를 때까지 짧게 쉬며 기다린다.
        while (panel.calls() < n && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(n, panel.calls(), "calls of closeRootWindow; console: " + console.messages());
    }

    /**
     * Lets the work that is queued now on the document thread and on the event dispatch thread finish, then checks that the window was
     * closed exactly a number of times.
     * <p>
     * 지금 문서 스레드와 이벤트 디스패치 스레드에 대기 중인 일이 끝나게 한 뒤, 창이 정확히 일정 횟수 닫혔는지 확인한다.
     *
     * @param n the expected number of calls / 기대하는 호출 횟수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void settleAndExpectCloses(int n) throws Exception {
        // 문서 큐와 EDT 를 몇 번 오가며 뒤따르는 작업(닫기 작업과 그 통지)이 끝날 시간을 준다.
        for (int i = 0; i < 5; i++) {
            Thread.sleep(60);
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Lets the queued events run. / 대기 중인 이벤트가 실행되게 한다. */
                public void run() {
                }
            });
        }
        Check.equal(n, panel.calls(), "calls of closeRootWindow; console: " + console.messages());
    }

    /**
     * C-ROOTCLOSE-1: the setting is off at first, a change is announced as a property on the event dispatch thread, the value survives
     * <code>clean()</code>, and after <code>dispose()</code> a change throws while the query gives the last value.
     * <p>
     * C-ROOTCLOSE-1: 설정은 처음에 꺼져 있고, 변경은 이벤트 디스패치 스레드에서 속성으로 알리며, 값은 <code>clean()</code> 뒤에도 유지되고,
     * <code>dispose()</code> 뒤에는 변경이 예외를 던지고 조회는 마지막 값을 준다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_1_SettingAndProperty() throws Exception {
        final List<String> seen = new ArrayList<String>();
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_ROOT_WINDOW_CLOSING_ENABLED, new PropertyChangeListener() {
            /** Records the change and its thread. / 변경과 그 스레드를 기록한다. */
            public void propertyChange(PropertyChangeEvent e) {
                synchronized (seen) {
                    seen.add(e.getOldValue() + ">" + e.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "@edt" : "@other"));
                }
            }
        });
        Check.isFalse(panel.isRootWindowClosingEnabled(), "off by default");
        panel.setRootWindowClosingEnabled(true);
        panel.setRootWindowClosingEnabled(true);
        Check.isTrue(panel.isRootWindowClosingEnabled(), "on after the call");
        settleAndExpectCloses(0);
        synchronized (seen) {
            Check.equal("[false>true@edt]", seen.toString(), "one change, announced on the EDT");
        }
        panel.clean();
        Check.isTrue(panel.isRootWindowClosingEnabled(), "clean() keeps the setting");
        panel.dispose();
        Check.isTrue(panel.isRootWindowClosingEnabled(), "the query gives the last value after dispose()");
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Changes the setting of a disposed component. / 폐기된 컴포넌트의 설정을 바꿔 본다. */
            public void run() throws Exception {
                panel.setRootWindowClosingEnabled(false);
            }
        }, "a change after dispose() throws");
    }

    /**
     * C-ROOTCLOSE-2: by default <code>window.close()</code> does nothing, <code>closed</code> stays false and the console of the page
     * gets a warning.
     * <p>
     * C-ROOTCLOSE-2: 기본값에서 <code>window.close()</code> 는 아무것도 하지 않고 <code>closed</code> 는 false 로 남으며 페이지의 콘솔에 경고가 간다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_2_IgnoredByDefault() throws Exception {
        page("/p", "console.log('before=' + window.closed + ',' + typeof window.close);\nwindow.close();\n"
                + "console.log('after=' + window.closed);\nsetTimeout(function () { console.log('later=' + window.closed); }, 30);");
        server.start();
        load("/p");
        awaitMessage("later=false");
        settleAndExpectCloses(0);
        Check.isTrue(console.count("LOG:before=false,function") == 1, "closed is a boolean member: " + console.messages());
        Check.isTrue(console.count("LOG:after=false") == 1, "closed stays false: " + console.messages());
        Check.isTrue(console.count("WARN:Scripts may close only the windows that were opened by them.") == 1,
                "one warning in the console: " + console.messages());
    }

    /**
     * C-ROOTCLOSE-3: when it is allowed, the window is closed once, on the event dispatch thread, after the calling task and its
     * microtasks; <code>closed</code> is true at once (also through <code>self</code> and <code>top</code>) and a second call does
     * nothing. A call from a timer, from a promise job and from an inline handler closes the window as well, and a host navigation
     * afterwards makes the window open again.
     * <p>
     * C-ROOTCLOSE-3: 허용했을 때 창은 호출한 작업과 그 마이크로태스크 뒤에 이벤트 디스패치 스레드에서 한 번 닫힌다. <code>closed</code> 는 곧바로
     * true 이고(<code>self</code>, <code>top</code> 으로 읽어도 같다) 두 번째 호출은 아무것도 하지 않는다. 타이머, 프라미스 작업, 인라인 핸들러에서 부른
     * 호출도 창을 닫으며, 그 뒤 호스트가 이동시키면 창은 다시 열린 상태가 된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_3_ClosesOnceAfterTheTask() throws Exception {
        // 이 스크립트는 파싱 중에(로더 스레드에서) 실행된다. 닫기는 스크립트의 나머지와 마이크로태스크, 그리고 바쁜 대기가 끝난 뒤에 와야 한다.
        page("/sync", "window.close();\nwindow.close();\n"
                + "Promise.resolve().then(function () { document.cookie = 'micro=' + window.closed; });\n"
                + "document.cookie = 'sync=' + window.closed + '-' + self.closed + '-' + top.closed;\n"
                + "var until = Date.now() + 300;\nwhile (Date.now() < until) { }\ndocument.cookie = 'end=1';");
        page("/timer", "console.log('timer-start=' + window.closed);\nsetTimeout(function () { window.close(); console.log('timer=' + closed); }, 10);");
        page("/promise", "Promise.resolve().then(function () { window.close(); console.log('promise=' + closed); });");
        server.routeFixed("/handler", TestHttpServer.Response.html("<!doctype html>\n<title>t</title>\n"
                + "<body onload=\"window.close(); console.log('handler=' + closed)\">x"));
        server.start();
        panel.setRootWindowClosingEnabled(true);
        load("/sync");
        awaitCloses(1);
        Check.isTrue(panel.allOnEdt(), "closeRootWindow runs on the EDT");
        String seen = panel.seenAtClose(1);
        Check.contains(seen, "sync=true-true-true", "closed is true at once, also through self and top");
        Check.contains(seen, "end=1", "the rest of the calling task ran before the closing");
        Check.contains(seen, "micro=true", "the microtasks of the task ran before the closing");
        Check.equal(0, console.count("WARN:"), "no warning for an accepted call or for the repeated call");
        settleAndExpectCloses(1);
        // 재정의한 닫기가 돌아온 뒤 호스트가 이동시키면 닫는 중 상태가 풀리고 다음 페이지가 다시 닫을 수 있다.
        load("/timer");
        awaitMessage("timer-start=false");
        awaitCloses(2);
        awaitMessage("timer=true");
        load("/promise");
        awaitCloses(3);
        awaitMessage("promise=true");
        load("/handler");
        awaitCloses(4);
        awaitMessage("handler=true");
        Check.isTrue(panel.allOnEdt(), "every call ran on the EDT");
    }

    /**
     * C-ROOTCLOSE-4: a frame of the same origin closes the window with <code>top.close()</code>; a frame of another origin, a
     * sandboxed frame without <code>allow-top-navigation</code> and the <code>close()</code> of a frame on itself do not.
     * <p>
     * C-ROOTCLOSE-4: 같은 오리진의 프레임은 <code>top.close()</code> 로 창을 닫는다. 다른 오리진의 프레임, <code>allow-top-navigation</code> 없는
     * 샌드박스 프레임, 프레임이 자기 자신에게 부른 <code>close()</code> 는 닫지 못한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_4_Frames() throws Exception {
        String frameScript = "top.close();\nconsole.log(NAME + '=' + top.closed);";
        server.routeFixed("/self-frame", TestHttpServer.Response.html(html("", "window.close();\nconsole.log('self-frame=' + window.closed + ',' + top.closed);")));
        server.routeFixed("/same-frame", TestHttpServer.Response.html(html("", "var NAME = 'same';\n" + frameScript)));
        server.routeFixed("/boxed-frame", TestHttpServer.Response.html(html("", "var NAME = 'boxed';\n" + frameScript)));
        server.routeFixed("/free-frame", TestHttpServer.Response.html(html("", "var NAME = 'free';\n" + frameScript)));
        other.routeFixed("/cross-frame", TestHttpServer.Response.html(html("", "var NAME = 'cross';\n" + frameScript)));
        other.start();
        server.routeFixed("/self", TestHttpServer.Response.html(html("<iframe src='/self-frame'></iframe>", "")));
        server.routeFixed("/cross", TestHttpServer.Response.html(html("<iframe src='" + other.url("/cross-frame") + "'></iframe>", "")));
        server.routeFixed("/boxed", TestHttpServer.Response.html(html("<iframe sandbox='allow-scripts allow-same-origin' src='/boxed-frame'></iframe>", "")));
        server.routeFixed("/free", TestHttpServer.Response.html(
                html("<iframe sandbox='allow-scripts allow-same-origin allow-top-navigation' src='/free-frame'></iframe>", "")));
        server.routeFixed("/same", TestHttpServer.Response.html(html("<iframe src='/same-frame'></iframe>", "")));
        server.start();
        panel.setRootWindowClosingEnabled(true);
        // 프레임 자기 자신의 close() 는 경고 없이 무시된다.
        load("/self");
        awaitMessage("self-frame=false,false");
        settleAndExpectCloses(0);
        Check.equal(0, console.count("WARN:"), "close() of a frame on itself is ignored silently: " + console.messages());
        // 다른 오리진의 프레임은 본 창을 닫지 못하고 경고를 받는다.
        load("/cross");
        awaitMessage("cross=false");
        settleAndExpectCloses(0);
        Check.equal(1, console.count("WARN:window.close() was ignored: a cross-origin frame"), "warning for the cross-origin frame: " + console.messages());
        // 최상위 이동이 막힌 샌드박스 프레임도 닫지 못한다.
        load("/boxed");
        awaitMessage("boxed=false");
        settleAndExpectCloses(0);
        Check.equal(1, console.count("WARN:window.close() was ignored: the sandbox of the frame"), "warning for the sandboxed frame: " + console.messages());
        // allow-top-navigation 이 있는 같은 오리진 샌드박스 프레임과 보통의 같은 오리진 프레임은 닫는다.
        load("/free");
        awaitMessage("free=true");
        awaitCloses(1);
        load("/same");
        awaitMessage("same=true");
        awaitCloses(2);
        Check.isTrue(panel.allOnEdt(), "every call ran on the EDT");
    }

    /**
     * Loads a page that calls <code>window.close()</code> and then keeps its task busy, and returns while that task still runs, so that
     * the caller can act on the component before the closing is performed.
     * <p>
     * <code>window.close()</code> 를 부른 뒤 작업을 붙잡고 있는 페이지를 불러, 그 작업이 아직 실행 중일 때 반환한다. 호출자는 닫기가 수행되기 전에
     * 컴포넌트를 조작할 수 있다.
     *
     * The call is made from a timer after the page was confirmed, so that the page is the current one of the component (a refresh
     * reloads it and a move back leaves it) and its task runs on the document thread.
     * <p>
     * 호출은 페이지가 확정된 뒤 타이머에서 한다. 그래서 그 페이지가 컴포넌트의 현재 페이지이고(새로고침은 그 페이지를 다시 읽고 뒤로 가기는 그 페이지를
     * 떠난다) 그 작업은 문서 스레드에서 실행된다.
     *
     * @param path the path of the page / 페이지 경로
     * @param tag  the tag of the message that the page writes after the call / 페이지가 호출 뒤에 쓰는 메시지의 꼬리표
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void loadBusyClosingPage(String path, String tag) throws Exception {
        page(path, "setTimeout(function () {\n  window.close();\n  console.log('" + tag + "-closing=' + window.closed);\n"
                + "  var until = Date.now() + 700;\n  while (Date.now() < until) { }\n"
                + "  document.cookie = '" + tag + "done=' + window.closed;\n  console.log('" + tag + "-done=' + window.closed);\n}, 10);");
        load(path);
        awaitMessage(tag + "-closing=true");
    }

    /**
     * C-ROOTCLOSE-5: a navigation that the page starts does not cancel the closing; an action of the host before the closing was
     * performed does (<code>setPage</code>, <code>setContent</code>, <code>back</code>, <code>refresh</code>, <code>clean</code>,
     * turning JavaScript off, turning the setting off), and <code>closed</code> is false again. A <code>forward</code> that does nothing
     * cancels nothing.
     * <p>
     * C-ROOTCLOSE-5: 페이지가 시작한 이동은 닫기를 취소하지 않는다. 닫기가 수행되기 전의 호스트 조작(<code>setPage</code>, <code>setContent</code>,
     * <code>back</code>, <code>refresh</code>, <code>clean</code>, JavaScript 끄기, 설정 끄기)은 취소하며 <code>closed</code> 는 다시 false 가 된다.
     * 아무 일도 하지 않는 <code>forward</code> 는 아무것도 취소하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_5_CancelOnlyByTheHost() throws Exception {
        page("/next", "console.log('next-loaded');");
        page("/start", "console.log('start-loaded');");
        // 이동이 시작되면 옛 페이지의 스크립트는 그 자리에서 중단되므로 이동을 마지막 문장으로 둔다.
        page("/nav", "window.close();\nconsole.log('nav=' + window.closed);\nlocation.href = '/next';");
        server.start();
        panel.setRootWindowClosingEnabled(true);
        // 페이지의 이동은 닫기를 취소하지 않는다. 이 페이지는 곧바로 다른 주소로 이동하므로 확정을 기다리지 않고 메시지로 확인한다.
        panel.setPage(server.url("/nav"));
        awaitMessage("nav=true");
        awaitCloses(1);
        awaitMessage("next-loaded");
        settleAndExpectCloses(1);
        // 아무 일도 하지 않는 forward 는 취소하지 않는다.
        load("/start");
        loadBusyClosingPage("/busy-forward", "forward");
        panel.forward();
        awaitCloses(2);
        Check.contains(panel.seenAtClose(2), "forwarddone=true", "the busy task ended before the window was closed");
        // 설정을 끄면 취소되고, 같은 작업의 뒤 문장이 closed 를 false 로 본다.
        loadBusyClosingPage("/busy-setting", "setting");
        panel.setRootWindowClosingEnabled(false);
        awaitMessage("setting-done=false");
        settleAndExpectCloses(2);
        panel.setRootWindowClosingEnabled(true);
        // 호스트가 시작한 이동과 정리는 취소한다.
        loadBusyClosingPage("/busy-page", "page");
        panel.setPage(server.url("/next"));
        settleAndExpectCloses(2);
        loadBusyClosingPage("/busy-content", "content");
        panel.setContent("<p>text</p>", "text/html");
        settleAndExpectCloses(2);
        load("/start");
        loadBusyClosingPage("/busy-back", "back");
        panel.back();
        settleAndExpectCloses(2);
        loadBusyClosingPage("/busy-refresh", "refresh");
        panel.refresh();
        // 새로고침한 페이지는 다시 닫기를 요청한다. 그 요청은 새 요청이므로 닫힌다.
        awaitCloses(3);
        loadBusyClosingPage("/busy-clean", "clean");
        panel.clean();
        settleAndExpectCloses(3);
        loadBusyClosingPage("/busy-script", "script");
        panel.setJavascriptEnabled(false);
        settleAndExpectCloses(3);
        Check.isTrue(panel.allOnEdt(), "every call ran on the EDT");
    }

    /**
     * C-ROOTCLOSE-6: the unloading events of the page run once before the window is closed, and a <code>clean()</code> afterwards does
     * not run them again. As for a navigation, only a page that was shown (<code>pageshow</code>) hears them.
     * <p>
     * C-ROOTCLOSE-6: 창이 닫히기 전에 페이지의 언로드 이벤트가 한 번 실행되고, 그 뒤의 <code>clean()</code> 은 다시 실행하지 않는다. 이동할 때와
     * 마찬가지로 한 번 보인(<code>pageshow</code>) 페이지만 이 이벤트를 듣는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_ROOTCLOSE_6_UnloadEventsRunOnce() throws Exception {
        page("/unload", "function count(name) {\n"
                + "  var m = document.cookie.match(new RegExp(name + '=(\\\\d+)'));\n"
                + "  document.cookie = name + '=' + ((m ? +m[1] : 0) + 1);\n"
                + "  console.log('event-' + name);\n}\n"
                + "window.addEventListener('beforeunload', function () { count('before'); });\n"
                + "window.addEventListener('pagehide', function () { count('hide'); });\n"
                + "window.addEventListener('unload', function () { count('unload'); });\n"
                // 언로드 이벤트는 한 번 보인(pageshow) 페이지만 듣는다. 그래서 pageshow 뒤의 작업에서 닫는다.
                + "window.addEventListener('pageshow', function () {\n"
                + "  setTimeout(function () { window.close(); console.log('requested'); }, 20);\n});");
        server.start();
        panel.setRootWindowClosingEnabled(true);
        load("/unload");
        awaitCloses(1);
        String seen = panel.seenAtClose(1);
        Check.contains(seen, "before=1", "beforeunload ran before the closing: " + seen);
        Check.contains(seen, "hide=1", "pagehide ran before the closing: " + seen);
        Check.contains(seen, "unload=1", "unload ran before the closing: " + seen);
        panel.clean();
        settleAndExpectCloses(1);
        String cookies = panel.getCookies();
        Check.contains(cookies, "before=1", "beforeunload ran once: " + cookies);
        Check.contains(cookies, "hide=1", "pagehide ran once: " + cookies);
        Check.contains(cookies, "unload=1", "unload ran once: " + cookies);
    }
}
