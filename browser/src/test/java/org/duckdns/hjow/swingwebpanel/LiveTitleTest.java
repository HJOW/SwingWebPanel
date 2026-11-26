package org.duckdns.hjow.swingwebpanel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The title of the component follows the title of the live document (plan item P31-04, API contract 4 and 5.25, cases C-TITLE-1 to 3):
 * a script that changes <code>document.title</code> or the <code>title</code> element changes {@link SwingWebPanel#getTitle()} and is
 * announced as the <code>title</code> property on the event dispatch thread. A browser needs this for its title bar and its tabs.
 * <p>
 * 컴포넌트의 제목은 살아 있는 문서의 제목을 따라간다(계획 P31-04, API 계약 4절과 5.25, 사례 C-TITLE-1~3): 스크립트가 <code>document.title</code>
 * 이나 <code>title</code> 요소를 바꾸면 {@link SwingWebPanel#getTitle()} 이 바뀌고 이벤트 디스패치 스레드에서 <code>title</code> 속성으로 알린다.
 * 브라우저의 제목 표시줄과 탭에 필요하다.
 */
public class LiveTitleTest {

    /** The test server. / 시험 서버. */
    private TestHttpServer server;

    /** The component of the case. / 사례의 컴포넌트. */
    private SwingWebPanel panel;

    /** The title changes "old>new" in arrival order, with "@other" appended when one did not arrive on the EDT. / 도착 순서대로의 제목 변경 "이전>새것". EDT 가 아닌 곳에서 도착했으면 "@other" 가 붙는다. */
    private final List<String> changes = new ArrayList<String>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LiveTitleTest() {
    }

    /**
     * Creates the server and a component that records its title changes.
     * <p>
     * 서버와, 제목 변경을 기록하는 컴포넌트를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        final SwingWebPanel[] made = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component on the EDT. / EDT 에서 컴포넌트를 만든다. */
            public void run() {
                made[0] = new SwingWebPanel();
                made[0].setSize(300, 200);
            }
        });
        panel = made[0];
        panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_TITLE, new PropertyChangeListener() {
            /** Records the change and its thread. / 변경과 그 스레드를 기록한다. */
            public void propertyChange(PropertyChangeEvent e) {
                synchronized (changes) {
                    changes.add(e.getOldValue() + ">" + e.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "" : "@other"));
                }
            }
        });
    }

    /**
     * Disposes the component and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        panel.dispose();
        server.stop();
    }

    /**
     * Waits until the component reports a title.
     * <p>
     * 컴포넌트가 제목을 알려 줄 때까지 기다린다.
     *
     * @param title the expected title / 기대하는 제목
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitTitle(String title) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 제목이 될 때까지 짧게 쉬며 기다린다.
        while (!title.equals(panel.getTitle()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(title, panel.getTitle(), "title of the component");
    }

    /**
     * Returns the recorded title changes after the queued events were delivered.
     * <p>
     * 대기 중인 이벤트가 전달된 뒤 기록된 제목 변경을 반환한다.
     *
     * @return the changes joined by "|" / "|" 로 이은 변경
     * @throws Exception on failure / 실패 시
     */
    private String recorded() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Lets the queued events run. / 대기 중인 이벤트가 실행되게 한다. */
            public void run() {
            }
        });
        synchronized (changes) {
            StringBuilder sb = new StringBuilder();
            // 변경마다 구분자를 넣어 이어 붙인다.
            for (int i = 0; i < changes.size(); i++) {
                sb.append(i > 0 ? "|" : "").append(changes.get(i));
            }
            return sb.toString();
        }
    }

    /**
     * C-TITLE-1: a script that sets <code>document.title</code> after the page was loaded, changes the text node of the title element,
     * and removes the title element changes the title of the component each time, announced on the event dispatch thread in order.
     * <p>
     * C-TITLE-1: 페이지를 읽은 뒤 스크립트가 <code>document.title</code> 을 정하고, title 요소의 텍스트 노드를 바꾸고, title 요소를 없애면 그때마다
     * 컴포넌트의 제목이 바뀌고 이벤트 디스패치 스레드에서 순서대로 알린다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_TITLE_1_ScriptChangesAfterLoad() throws Exception {
        // 페이지를 읽은 뒤 타이머가 단계마다 제목을 바꾼다.
        server.routeFixed("/t", TestHttpServer.Response.html("<!doctype html><title>one</title><body><script>"
                + "var steps = [function () { document.title = 'two'; },"
                + "  function () { document.querySelector('title').firstChild.data = '  three   words '; },"
                + "  function () { var t = document.querySelector('title'); t.parentNode.removeChild(t); },"
                + "  function () { document.title = 'created again'; }];"
                + "var i = 0;"
                + "function run() { steps[i++](); if (i < steps.length) { setTimeout(run, 150); } }"
                + "window.addEventListener('load', function () { setTimeout(run, 150); });"
                + "</" + "script>"));
        server.start();
        panel.setPage(server.url("/t"));
        awaitTitle("created again");
        String seen = recorded();
        Check.isTrue(seen.indexOf("@other") < 0, "every change arrived on the EDT: " + seen);
        Check.contains(seen, "one>two", "document.title");
        Check.contains(seen, "two>three words", "the text node of the title element, with white space collapsed");
        Check.contains(seen, "three words>", "removing the title element gives the empty title");
        Check.contains(seen, ">created again", "document.title creates the element again");
    }

    /**
     * C-TITLE-2: a title that a script sets while the page is still being parsed is the title of the component once the page is
     * confirmed.
     * <p>
     * C-TITLE-2: 페이지를 아직 파싱하는 동안 스크립트가 정한 제목이, 페이지가 확정된 뒤 컴포넌트의 제목이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_TITLE_2_ChangedWhileParsing() throws Exception {
        server.routeFixed("/p", TestHttpServer.Response.html("<!doctype html><title>source</title>"
                + "<script>document.title = 'set while parsing';</" + "script><p>x</p>"));
        server.start();
        panel.setPage(server.url("/p"));
        awaitTitle("set while parsing");
        Check.equal(server.url("/p"), panel.getPage(), "the page is the confirmed one");
    }

    /**
     * C-TITLE-3: the title of a document in a frame does not change the title of the component, and the next page brings its own title.
     * <p>
     * C-TITLE-3: 프레임 안 문서의 제목은 컴포넌트의 제목을 바꾸지 않고, 다음 페이지는 자기 제목을 가져온다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testC_TITLE_3_FramesAndNextPage() throws Exception {
        server.routeFixed("/frame", TestHttpServer.Response.html("<!doctype html><title>inner</title>"
                + "<script>setTimeout(function () { document.title = 'inner changed'; parent.document.title = 'outer after frame'; }, 50);</" + "script>"));
        server.routeFixed("/outer", TestHttpServer.Response.html("<!doctype html><title>outer</title><iframe src='/frame'></iframe>"));
        server.routeFixed("/next", TestHttpServer.Response.html("<!doctype html><title>next page</title><p>x</p>"));
        server.start();
        panel.setPage(server.url("/outer"));
        // 프레임이 자기 제목을 바꾼 뒤 부모의 제목을 바꾼다. 컴포넌트에는 부모의 제목만 보여야 한다.
        awaitTitle("outer after frame");
        String seen = recorded();
        Check.isTrue(seen.indexOf("inner") < 0, "the title of the frame never reaches the component: " + seen);
        panel.setPage(server.url("/next"));
        awaitTitle("next page");
    }
}
