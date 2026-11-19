package org.duckdns.hjow.swingwebpanel.core;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Whether a page that was replaced is released while its component lives on (plan items P19-05, P21-01, P21-06): the decoded resources,
 * the layout and the DOM of the old page must not stay reachable from the component, its history or any thread. Only weak references are
 * used, and nothing is claimed about when the garbage collector runs.
 * <p>
 * 교체된 페이지가 컴포넌트가 살아 있는 동안 해제되는지 확인하는 시험(계획 P19-05, P21-01, P21-06): 이전 페이지의 디코딩된 리소스, 배치, DOM 은
 * 컴포넌트, 이력, 어떤 스레드에서도 도달 가능하게 남으면 안 된다. 약한 참조만 쓰며 가비지 컬렉터가 언제 실행되는지는 주장하지 않는다.
 */
public class PageReleaseIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PageReleaseIntegrationTest() {
    }

    /**
     * Reads the session of a component (the component does not expose it).
     * <p>
     * 컴포넌트의 세션을 읽는다(컴포넌트가 노출하지 않는다).
     *
     * @param panel the component / 컴포넌트
     * @return the session / 세션
     * @throws Exception if the field cannot be read / 필드를 읽을 수 없으면
     */
    private static BrowserSession sessionOf(SwingWebPanel panel) throws Exception {
        Field f = SwingWebPanel.class.getDeclaredField("session");
        f.setAccessible(true);
        return (BrowserSession) f.get(panel);
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
        while ((!url.equals(p.getPage()) || p.getProgress() != 100 || !"Done".equals(p.getStatusText())) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, p.getPage(), "the page was confirmed");
        Check.equal("Done", p.getStatusText(), "and finished");
    }

    /**
     * Takes weak references to the presentation and the DOM of the page that the component shows now.
     * <p>
     * 컴포넌트가 지금 보여 주는 페이지의 표현 상태와 DOM 에 대한 약한 참조를 얻는다.
     *
     * @param panel the component / 컴포넌트
     * @return the weak references: presentation, DOM document / 약한 참조: 표현 상태, DOM 문서
     * @throws Exception on failure / 실패 시
     */
    private static WeakReference<?>[] watch(SwingWebPanel panel) throws Exception {
        HtmlPageDocument holder = (HtmlPageDocument) sessionOf(panel).getSnapshot().getDocument();
        Check.isTrue(holder.getPresentation() != null, "the page has a presentation");
        return new WeakReference<?>[] { new WeakReference<Object>(holder.getPresentation()), new WeakReference<Object>(holder.getDocument()) };
    }

    /**
     * After the component moved on to another page, its old page (presentation with its decoded images and layout, and the DOM) can be
     * collected although the component is still referenced and the old page is still an entry of the history.
     * <p>
     * 컴포넌트가 다른 페이지로 넘어가면, 컴포넌트가 아직 참조되고 이전 페이지가 아직 이력의 항목이어도 이전 페이지(디코딩한 이미지와 배치가 있는 표현
     * 상태, DOM)는 수거될 수 있다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testReplacedPageIsReleasedWhileTheComponentLives() throws Exception {
        TestHttpServer server = new TestHttpServer();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB), "png", png);
        server.routeFixed("/rich", TestHttpServer.Response.html("<!doctype html><html><head><link rel=stylesheet href='a.css'></head><body>"
                + "<img src='i.png'><script>setInterval(function () { document.title = String(Date.now()); }, 20);</script></body></html>"));
        server.routeFixed("/a.css", TestHttpServer.Response.ok("text/css", "body { color: green }"));
        server.routeFixed("/i.png", new TestHttpServer.Response(200).bytes("image/png", png.toByteArray()));
        server.routeFixed("/plain", TestHttpServer.Response.html("<p>plain</p>"));
        server.start();
        SwingWebPanel panel = new SwingWebPanel();
        try {
            panel.setPage(server.url("/rich"));
            await(panel, server.url("/rich"));
            Thread.sleep(150);
            WeakReference<?>[] old = watch(panel);
            panel.setPage(server.url("/plain"));
            await(panel, server.url("/plain"));
            Check.equal(3, panel.getHistories().size(), "the old page is still an entry of the history");
            long end = System.currentTimeMillis() + 40000;
            boolean released = false;
            // 이전 페이지의 표현 상태와 DOM 이 모두 지워질 때까지 GC 를 요청하며 짧게 쉰다. GC 의 시점은 보장되지 않으므로 시간 제한을 둔다.
            while (!released && System.currentTimeMillis() < end) {
                System.gc();
                System.runFinalization();
                Thread.sleep(100);
                released = old[0].get() == null && old[1].get() == null;
            }
            Check.isTrue(old[0].get() == null, "the presentation of the replaced page was released");
            Check.isTrue(old[1].get() == null, "the DOM of the replaced page was released");
            Check.equal(server.url("/plain"), panel.getPage(), "the component itself is alive and shows the new page");
        } finally {
            panel.dispose();
            server.stop();
        }
    }
}
