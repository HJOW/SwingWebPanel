package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The image button (<code>&lt;input type="image"&gt;</code>) of old forms through the real component and real HTTP (plan item
 * P07-07-c, LEG-14; HTML5 REC 2014-10-28 section 4.10.7.1.19 and the construction of the form data set): a click on the image submits the
 * form with the position of the click as <code>name.x</code> and <code>name.y</code> (<code>x</code> and <code>y</code> without a
 * name), and the other fields of the form.
 * <p>
 * 옛 폼의 이미지 버튼(<code>&lt;input type="image"&gt;</code>)을 실제 컴포넌트와 실제 HTTP 로 확인한다(계획 P07-07-c, LEG-14, HTML5 REC 2014-10-28 §4.10.7.1.19 와 폼 데이터
 * 집합 구성): 이미지를 클릭하면 클릭 위치가 <code>name.x</code>, <code>name.y</code>(이름이 없으면 <code>x</code>, <code>y</code>)로 폼과 함께 제출되며 폼의 다른 필드도 함께 간다.
 */
public class ImageButtonIntegrationTest {

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
    public ImageButtonIntegrationTest() {
    }

    /**
     * Starts the server with the page and the picture.
     * <p>
     * 페이지와 그림이 있는 서버를 시작한다.
     *
     * @throws IOException on failure / 실패 시
     */
    public void setUp() throws IOException {
        server = new TestHttpServer();
        server.start();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB), "png", out);
        server.routeFixed("/pic.png", new TestHttpServer.Response(200).bytes("image/png", out.toByteArray()));
        server.routeFixed("/form", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>"
                + "<form action=\"/named\" method=\"get\"><input type=\"hidden\" name=\"h\" value=\"1\">"
                + "<input type=\"image\" name=\"go\" src=\"/pic.png\" alt=\"Go\" style=\"position:absolute;left:10px;top:20px;width:100px;height:50px\"></form>"
                + "<form action=\"/unnamed\" method=\"get\"><input type=\"image\" src=\"/pic.png\" alt=\"Go\" style=\"position:absolute;left:10px;top:120px;width:100px;height:50px\"></form>"));
        server.routeFixed("/named", TestHttpServer.Response.html("<p>named</p>"));
        server.routeFixed("/unnamed", TestHttpServer.Response.html("<p>unnamed</p>"));
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
     * Opens the page and clicks a point.
     * <p>
     * 페이지를 열고 한 점을 클릭한다.
     *
     * @param x the x of the click / 클릭의 x
     * @param y the y of the click / 클릭의 y
     * @throws Exception on failure / 실패 시
     */
    private void openAndClick(final int x, final int y) throws Exception {
        final SwingWebPanel panel = new SwingWebPanel();
        panels.add(panel);
        panel.setPage(server.url("/form"));
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정될 때까지 기다린다(input type=image 의 그림은 로드하지 않으므로 그림 요청을 기다리지 않는다).
        while (!server.url("/form").equals(panel.getPage()) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(600);
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the click.
             * <p>
             * 클릭을 전달한다.
             */
            public void run() {
                panel.setSize(300, 300);
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
        end = System.currentTimeMillis() + 30000;
        // 폼 제출이 서버에 도착할 때까지 기다린다.
        while (server.count("/named") + server.count("/unnamed") < 1 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
    }

    /**
     * Returns the query of the last request of a path.
     * <p>
     * 경로의 마지막 요청의 쿼리를 반환한다.
     *
     * @param path the path / 경로
     * @return the query, or null if there was no request / 쿼리. 요청이 없으면 null
     */
    private String queryOf(String path) {
        String found = null;
        // 요청을 훑으며 경로가 같은 마지막 것의 쿼리를 찾는다.
        for (TestHttpServer.Request r : server.requests()) {
            if (path.equals(r.path)) {
                found = r.query;
            }
        }
        return found;
    }

    /**
     * A click on a named image button submits <code>name.x</code> and <code>name.y</code> after the other fields, relative to the image.
     * <p>
     * 이름이 있는 이미지 버튼의 클릭은 이미지 기준 <code>name.x</code>, <code>name.y</code> 를 다른 필드 뒤에 제출한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testNamedImageButton() throws Exception {
        openAndClick(10 + 37, 20 + 12);
        Check.equal("h=1&go.x=37&go.y=12", queryOf("/named"), "the fields and the position of the click");
        Check.equal(0, server.count("/unnamed"), "the other form was not submitted");
    }

    /**
     * A click on an image button without a name submits <code>x</code> and <code>y</code>.
     * <p>
     * 이름이 없는 이미지 버튼의 클릭은 <code>x</code>, <code>y</code> 를 제출한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testUnnamedImageButton() throws Exception {
        openAndClick(10 + 5, 120 + 49);
        Check.equal("x=5&y=49", queryOf("/unnamed"), "x and y");
    }
}
