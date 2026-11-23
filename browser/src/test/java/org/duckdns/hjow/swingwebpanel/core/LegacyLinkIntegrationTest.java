package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The links of old pages through the real component (plan item P07-07-c, LEG-15): the anchor that is named with <code>&lt;a name&gt;</code>
 * is the target of a fragment, the element with that <code>id</code> comes first and the name is compared with regard to case, and the
 * <code>target</code> of a <code>base</code> element is the default target of the links of the page.
 * <p>
 * 실제 컴포넌트로 확인하는 옛 페이지의 링크(계획 P07-07-c, LEG-15): <code>&lt;a name&gt;</code> 으로 이름을 붙인 앵커는 프래그먼트의 대상이고, 그 <code>id</code> 를 가진 요소가
 * 먼저이며, 이름은 대소문자를 구분해 비교하고, <code>base</code> 요소의 <code>target</code> 은 페이지 링크의 기본 대상이다.
 */
public class LegacyLinkIntegrationTest {

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The components of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyLinkIntegrationTest() {
    }

    /**
     * Starts the server with the pages.
     * <p>
     * 페이지가 있는 서버를 시작한다.
     *
     * @throws IOException on failure / 실패 시
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.start();
        String tall = "<div style='height:1200px'>space</div>";
        String go = "<a id=l href='%s' style='display:block;height:40px;width:200px'>go</a>";
        server.routeFixed("/name", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>" + go.replace("%s", "#end") + tall
                + "<a name='end' style='display:block;height:50px'>End</a><div style='height:600px'>after</div>"));
        server.routeFixed("/id-first", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>" + go.replace("%s", "#end")
                + "<a name='end' style='display:block;height:50px'>name</a>" + tall + "<h2 id='end' style='margin:0;height:50px'>id</h2><div style='height:600px'>after</div>"));
        server.routeFixed("/case", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>" + go.replace("%s", "#END") + tall
                + "<a name='end' style='display:block;height:50px'>End</a><div style='height:600px'>after</div>"));
        server.routeFixed("/empty", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>" + go.replace("%s", "#end") + tall
                + "<p style='margin:0;height:50px'>before<a name='end'></a>after</p><div style='height:600px'>after</div>"));
        server.routeFixed("/basetarget", TestHttpServer.Response.html("<!doctype html><base target=\"f\"><style>html,body{margin:0}</style>"
                + "<a id=l href='/c' style='display:block;height:40px;width:200px'>go</a><iframe name=\"f\" src=\"/frame0\" width=\"100\" height=\"50\"></iframe>"));
        server.routeFixed("/ownTarget", TestHttpServer.Response.html("<!doctype html><base target=\"f\"><style>html,body{margin:0}</style>"
                + "<a id=l href='/c' target=\"_top\" style='display:block;height:40px;width:200px'>go</a><iframe name=\"f\" src=\"/frame0\" width=\"100\" height=\"50\"></iframe>"));
        server.routeFixed("/frame0", TestHttpServer.Response.html("<!doctype html><p>frame</p>"));
        server.routeFixed("/c", TestHttpServer.Response.html("<!doctype html><title>C</title><p>c</p>"));
    }

    /**
     * Releases the components and the server.
     * <p>
     * 컴포넌트와 서버를 해제한다.
     */
    public void tearDown() {
        // 만든 패널을 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Opens a page in a component in a scroll pane with a small viewport, and waits until it is laid out.
     * <p>
     * 작은 뷰포트의 스크롤 창에 든 컴포넌트에서 페이지를 열고 배치될 때까지 기다린다.
     *
     * @param path   the path of the page / 페이지 경로
     * @param pane   one-element array that receives the scroll pane / 스크롤 창을 받는 원소 하나짜리 배열
     * @param height the least height of the laid out page / 배치된 페이지의 최소 높이
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private SwingWebPanel open(String path, final JScrollPane[] pane, int height) throws Exception {
        final SwingWebPanel panel = new SwingWebPanel();
        panels.add(panel);
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Puts the component into a scroll pane with a small viewport.
             * <p>
             * 컴포넌트를 작은 뷰포트의 스크롤 창에 넣는다.
             */
            public void run() {
                pane[0] = new JScrollPane(panel);
                pane[0].setSize(300, 100);
                pane[0].doLayout();
            }
        });
        panel.setPage(server.url(path));
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 높이가 충분히 배치될 때까지 기다린다.
        while ((!server.url(path).equals(panel.getPage()) || panel.getPreferredSize().height < height) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return panel;
    }

    /**
     * Clicks at a point of the component through its own event dispatch.
     * <p>
     * 컴포넌트 자신의 이벤트 전달로 한 점을 클릭한다.
     *
     * @param panel the component / 컴포넌트
     * @param x     the x / x
     * @param y     the y / y
     * @throws Exception on failure / 실패 시
     */
    private static void click(final SwingWebPanel panel, final int x, final int y) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the click.
             * <p>
             * 클릭을 전달한다.
             */
            public void run() {
                panel.setSize(300, panel.getPreferredSize().height);
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Clicks the link at the top of a tall page and returns the vertical scroll position that the viewport reached (or the one that it
     * stayed at if the viewport did not move).
     * <p>
     * 긴 페이지 맨 위의 링크를 클릭하고 뷰포트가 도달한 세로 스크롤 위치를 반환한다(움직이지 않으면 머문 위치).
     *
     * @param path   the page / 페이지
     * @param expect the expected URL after the click / 클릭 뒤 기대하는 URL
     * @return the scroll position / 스크롤 위치
     * @throws Exception on failure / 실패 시
     */
    private int clickAndScroll(String path, String expect) throws Exception {
        JScrollPane[] pane = new JScrollPane[1];
        SwingWebPanel panel = open(path, pane, 1500);
        click(panel, 20, 20);
        long end = System.currentTimeMillis() + 30000;
        // 주소가 바뀔 때까지 기다린다.
        while (!expect.equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        long settle = System.currentTimeMillis() + 2500;
        // 뷰포트가 움직일 때까지 기다린다(움직이지 않는 사례는 시간이 다하면 그만둔다).
        while (pane[0].getViewport().getViewPosition().y < 1000 && System.currentTimeMillis() < settle) {
            Thread.sleep(10);
        }
        Thread.sleep(200);
        Check.equal(expect, panel.getPage(), "the address after the click of " + path);
        return pane[0].getViewport().getViewPosition().y;
    }

    /**
     * An anchor with a <code>name</code> is the target of a fragment, and the viewport scrolls to it without a new request.
     * <p>
     * <code>name</code> 이 있는 앵커는 프래그먼트의 대상이며 뷰포트가 새 요청 없이 그곳으로 스크롤된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNamedAnchorIsAFragmentTarget() throws Exception {
        int y = clickAndScroll("/name", server.url("/name") + "#end");
        Check.isTrue(y >= 1200 && y <= 1260, "scrolled to the named anchor: y=" + y);
        Check.equal(1, server.count("/name"), "no new request");
    }

    /**
     * An element with the <code>id</code> comes before an anchor with that <code>name</code>, even if the anchor is first in the document.
     * <p>
     * <code>id</code> 가 같은 요소는 문서에서 앞서 있는 그 <code>name</code> 의 앵커보다 먼저이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testIdComesBeforeName() throws Exception {
        int y = clickAndScroll("/id-first", server.url("/id-first") + "#end");
        Check.isTrue(y >= 1240 && y <= 1300, "scrolled to the element with the id (after the anchor that is first in the document): y=" + y);
    }

    /**
     * The name of an anchor is compared with regard to case: <code>#END</code> is not the anchor named <code>end</code>, so the viewport
     * stays where it is while the address changes.
     * <p>
     * 앵커의 이름은 대소문자를 구분해 비교한다: <code>#END</code> 는 <code>end</code> 라는 앵커가 아니므로 주소는 바뀌어도 뷰포트는 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testAnchorNameIsCaseSensitive() throws Exception {
        int y = clickAndScroll("/case", server.url("/case") + "#END");
        Check.equal(0, y, "the viewport did not move: y=" + y);
    }

    /**
     * An empty named anchor inside a paragraph is a target as well.
     * <p>
     * 문단 안의 빈 이름 앵커도 대상이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEmptyNamedAnchorIsATarget() throws Exception {
        int y = clickAndScroll("/empty", server.url("/empty") + "#end");
        Check.isTrue(y >= 1200 && y <= 1260, "scrolled to the empty anchor: y=" + y);
    }

    /**
     * The <code>target</code> of the <code>base</code> element is the default target of the links: a link without a target navigates the
     * frame that the base names, a link with its own target (here <code>_top</code>) navigates the page.
     * <p>
     * <code>base</code> 요소의 <code>target</code> 은 링크의 기본 대상이다: target 이 없는 링크는 base 가 가리키는 프레임을 이동시키고, 자기 target(여기서는
     * <code>_top</code>)이 있는 링크는 페이지를 이동시킨다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBaseTargetIsTheDefaultTarget() throws Exception {
        JScrollPane[] pane = new JScrollPane[1];
        SwingWebPanel panel = open("/basetarget", pane, 40);
        long end = System.currentTimeMillis() + 30000;
        // 프레임 문서를 요청할 때까지 기다린다.
        while (server.count("/frame0") < 1 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(300);
        click(panel, 20, 20);
        end = System.currentTimeMillis() + 30000;
        // 링크의 대상이 요청될 때까지 기다린다.
        while (server.count("/c") < 1 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(300);
        Check.equal(1, server.count("/c"), "the link target was requested (by the frame)");
        Check.equal(server.url("/basetarget"), panel.getPage(), "the page stayed: the base named the frame");
        SwingWebPanel other = open("/ownTarget", new JScrollPane[1], 40);
        Thread.sleep(500);
        click(other, 20, 20);
        end = System.currentTimeMillis() + 30000;
        // 링크가 최상위 문서를 이동시킬 때까지 기다린다.
        while (!server.url("/c").equals(other.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(server.url("/c"), other.getPage(), "its own target _top wins over the base: the page navigated");
    }
}
