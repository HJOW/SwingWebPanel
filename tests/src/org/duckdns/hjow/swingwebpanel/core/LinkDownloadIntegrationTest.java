package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Links with the <code>download</code> attribute, clicked through the real component and real HTTP (plan items P15-08-c-2, P01-18): a
 * same-origin link saves the response whatever its type and leaves the page, the URL and the history alone, the value of the attribute
 * is the suggested file name unless the server names the file, a link to another origin ignores the attribute and navigates, and an error
 * response is shown as an error page instead of being saved.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 클릭하는 <code>download</code> 속성이 있는 링크 시험(계획 P15-08-c-2, P01-18): 같은 오리진 링크는 응답의 형식과
 * 관계없이 응답을 저장하고 페이지, URL, 이력은 그대로 두며, 속성 값은 서버가 파일 이름을 주지 않았을 때의 제안 이름이고, 다른 오리진 링크는 속성을
 * 무시하고 이동하며, 오류 응답은 저장하지 않고 오류 페이지로 보여 준다.
 */
public class LinkDownloadIntegrationTest {

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * The downloads that the handler received: the final address and the Content-Disposition.
     * <p>
     * 핸들러가 받은 다운로드: 최종 주소와 Content-Disposition.
     */
    private final List<String> saved = new ArrayList<String>();

    /**
     * Server of the case.
     * <p>
     * 사례의 서버.
     */
    private TestHttpServer server;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LinkDownloadIntegrationTest() {
    }

    /**
     * Starts the server: a page with four stacked links of 40 pixels and the files they point to.
     * <p>
     * 서버를 시작한다: 40 픽셀 높이로 쌓인 링크 네 개가 있는 페이지와 그 링크가 가리키는 파일.
     *
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.start();
        String other = "http://127.0.0.1:" + server.getPort() + "/doc.html";
        String block = "display:block;height:40px;width:200px";
        server.routeFixed("/links", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>"
                + "<a id=a0 href='/doc.html' download='saved.html' style='" + block + "'>named</a>"
                + "<a id=a1 href='/doc.html' style='" + block + "'>plain</a>"
                + "<a id=a2 href='" + other + "' download style='" + block + "'>other origin</a>"
                + "<a id=a3 href='/named.html' download='mine.html' style='" + block + "'>server names it</a>"
                + "<a id=a4 href='/gone.html' download style='" + block + "'>missing</a>"
                + "<a id=a5 href='/doc.html' download style='" + block + "'>unnamed</a>"));
        server.routeFixed("/doc.html", TestHttpServer.Response.html("<p>document</p>"));
        server.routeFixed("/named.html", TestHttpServer.Response.html("<p>named</p>").header("Content-Disposition", "inline; filename=\"server.html\""));
        server.routeFixed("/gone.html", new TestHttpServer.Response(404).text("text/html", "<p>no such file</p>"));
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
     * Opens the page with the links in a component that records the downloads.
     * <p>
     * 다운로드를 기록하는 컴포넌트에서 링크가 있는 페이지를 연다.
     *
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private SwingWebPanel open() throws Exception {
        final SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        Field f = SwingWebPanel.class.getDeclaredField("session");
        f.setAccessible(true);
        ((BrowserSession) f.get(p)).setDownloadHandler(new DownloadHandler() {
            /**
             * Records the download.
             * <p>
             * 다운로드를 기록한다.
             *
             * @param request  the navigation / 이동
             * @param download the response / 응답
             */
            public void downloadRequired(NavigationRequest request, DownloadRequiredException download) {
                synchronized (saved) {
                    saved.add(download.getFinalUrl() + "|" + download.getContentDisposition() + "|" + download.getReason());
                }
            }
        });
        p.setPage(server.url("/links"));
        await(p, server.url("/links"));
        long end = System.currentTimeMillis() + 30000;
        // 문서가 배치되어 링크를 클릭할 수 있는 높이가 될 때까지 기다린다.
        while (p.getPreferredSize().height < 240 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        return p;
    }

    /**
     * Waits until the component shows an address and the load ended.
     * <p>
     * 컴포넌트가 주소를 보여 주고 로딩이 끝날 때까지 기다린다.
     *
     * @param p   the component / 컴포넌트
     * @param url the address / 주소
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void await(SwingWebPanel p, String url) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 로딩이 끝날 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || p.getStatusText().startsWith("Loading")) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "page");
    }

    /**
     * Clicks the link with the given index (each link is 40 pixels high) through the event dispatch of the component.
     * <p>
     * 컴포넌트의 이벤트 전달로 주어진 번호의 링크를 클릭한다(링크는 각각 높이 40 픽셀).
     *
     * @param p     the component / 컴포넌트
     * @param index the link number / 링크 번호
     * @throws Exception on failure / 실패 시
     */
    private static void click(final SwingWebPanel p, final int index) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the click.
             * <p>
             * 클릭을 전달한다.
             */
            public void run() {
                p.setSize(300, Math.max(300, p.getPreferredSize().height));
                p.dispatchEvent(new MouseEvent(p, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 20, 20 + 40 * index, 1, false,
                        MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Waits until the handler received a number of downloads.
     * <p>
     * 핸들러가 다운로드를 일정 수만큼 받을 때까지 기다린다.
     *
     * @param count the number of downloads / 다운로드 수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitSaved(int count) throws Exception {
        long end = System.currentTimeMillis() + 10000;
        // 다운로드가 기록될 때까지 짧게 쉬며 기다린다.
        while (saved.size() < count && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(count, saved.size(), "downloads received: " + saved);
    }

    /**
     * A same-origin link with the attribute saves an HTML response (which would otherwise be shown) under the suggested name and leaves the
     * page, the address and the history alone; a link without a value uses the file name of the address; the server's name wins.
     * <p>
     * 속성이 있는 같은 오리진 링크는 (그렇지 않으면 보였을) HTML 응답을 제안한 이름으로 저장하고 페이지, 주소, 이력을 그대로 둔다. 값이 없는 링크는
     * 주소의 파일 이름을 쓰고, 서버가 준 이름이 이긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSameOriginLinkSavesTheResponse() throws Exception {
        SwingWebPanel p = open();
        click(p, 0);
        awaitSaved(1);
        Check.equal(server.url("/doc.html") + "|attachment; filename=\"saved.html\"|LINK_DOWNLOAD", saved.get(0), "the suggested name");
        Thread.sleep(200);
        Check.equal(server.url("/links"), p.getPage(), "the page stays");
        Check.equal(2, p.getHistories().size(), "and so does the history: " + p.getHistories());
        click(p, 5);
        awaitSaved(2);
        Check.equal(server.url("/doc.html") + "|null|LINK_DOWNLOAD", saved.get(1), "no value: the name comes from the address");
        click(p, 3);
        awaitSaved(3);
        Check.equal(server.url("/named.html") + "|inline; filename=\"server.html\"|LINK_DOWNLOAD", saved.get(2), "the server's file name wins");
        Check.equal(server.url("/links"), p.getPage(), "still the same page");
    }

    /**
     * A link to another origin ignores the attribute and navigates like any link; a link without the attribute navigates too.
     * <p>
     * 다른 오리진 링크는 속성을 무시하고 다른 링크처럼 이동한다. 속성이 없는 링크도 이동한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testOtherOriginAndPlainLinksNavigate() throws Exception {
        SwingWebPanel p = open();
        click(p, 2);
        String other = "http://127.0.0.1:" + server.getPort() + "/doc.html";
        await(p, other);
        Check.contains(p.getContent(), "document", "the page of the other origin is shown");
        Check.equal(0, saved.size(), "nothing was saved");
        p.back();
        await(p, server.url("/links"));
        click(p, 1);
        await(p, server.url("/doc.html"));
        Check.equal(0, saved.size(), "a link without the attribute is shown, not saved");
    }

    /**
     * An error response to a link with the attribute is shown as an error page: nothing is saved.
     * <p>
     * download 속성이 있는 링크에 대한 오류 응답은 오류 페이지로 보이며 아무것도 저장되지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testErrorResponseIsShownNotSaved() throws Exception {
        SwingWebPanel p = open();
        click(p, 4);
        await(p, server.url("/gone.html"));
        Check.contains(p.getContent(), "no such file", "the error page shows the body of the server");
        Check.equal(0, saved.size(), "nothing was saved");
    }
}
