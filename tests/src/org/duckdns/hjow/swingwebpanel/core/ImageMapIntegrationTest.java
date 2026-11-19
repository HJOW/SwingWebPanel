package org.duckdns.hjow.swingwebpanel.core;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Image maps through the real component and real HTTP (plan item P07-07-c, LEG-07): a click on an <code>area</code> follows its link, the
 * pointer over an <code>area</code> gives the hyperlink events with its address, a dead area catches the click, a click that no shape
 * covers falls back to the link around the image, a click on a server-side image map (<code>ismap</code>) requests the address with
 * <code>?x,y</code>, and a script reads the areas through the DOM.
 * <p>
 * 실제 컴포넌트와 실제 HTTP 로 확인하는 이미지 맵(계획 P07-07-c, LEG-07): <code>area</code> 클릭은 그 링크를 따라가고, <code>area</code> 위의 포인터는 그 주소의 하이퍼링크
 * 이벤트를 주며, 죽은 영역은 클릭을 잡고, 모양이 덮지 않는 클릭은 이미지를 감싼 링크로 돌아가며, 서버 쪽 이미지 맵(<code>ismap</code>) 클릭은 <code>?x,y</code> 가 붙은 주소를
 * 요청하고, 스크립트는 DOM 으로 area 를 읽는다.
 */
public class ImageMapIntegrationTest {

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The components that the case created.
     * <p>
     * 사례가 만든 컴포넌트.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ImageMapIntegrationTest() {
    }

    /**
     * Starts the server with the page, the pictures and the targets.
     * <p>
     * 페이지, 그림, 이동 대상이 있는 서버를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.start();
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        server.routeFixed("/pic.png", new TestHttpServer.Response(200).bytes("image/png", out.toByteArray()));
        server.routeFixed("/maps", TestHttpServer.Response.html("<!doctype html><style>html,body{margin:0}</style>"
                + "<img id=i src='/pic.png' usemap='#m' style='position:absolute;left:0;top:0;width:100px;height:50px'>"
                + "<map name=m>"
                + "<area id=dead shape=rect coords='0,0,10,10'>"
                + "<area id=red shape=rect coords='0,0,50,50' href='/red.html' alt='Red'>"
                + "<area id=green shape=circle coords='75,25,20' href='/green.html' alt='Green'>"
                + "</map>"
                + "<a id=w href='/around.html' style='position:absolute;left:0;top:80px'><img id=j src='/pic.png' usemap='#m' style='display:block;width:100px;height:50px'></a>"
                + "<a id=s href='/cgi/map' style='position:absolute;left:0;top:160px'><img id=k src='/pic.png' ismap style='display:block;width:100px;height:50px'></a>"
                + "<script>var map = document.getElementsByName('m')[0]; var names = [];"
                + "for (var n = 0; n < map.areas.length; n++) { names.push(map.areas[n].id); }"
                + "sessionStorage.setItem('areas', names.join(',') + '|' + map.images.length + '|' + (map.images[0] === document.getElementById('i'))"
                + " + '|' + (map.areas instanceof HTMLCollection));"
                + "</script>"));
        server.routeFixed("/red.html", TestHttpServer.Response.html("<p>red</p>"));
        server.routeFixed("/green.html", TestHttpServer.Response.html("<p>green</p>"));
        server.routeFixed("/around.html", TestHttpServer.Response.html("<p>around</p>"));
        server.routeFixed("/cgi/map", TestHttpServer.Response.html("<p>cgi</p>"));
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
     * Opens the page in a component and waits until the images are laid out.
     * <p>
     * 컴포넌트에서 페이지를 열고 이미지가 배치될 때까지 기다린다.
     *
     * @return the component / 컴포넌트
     * @throws Exception on failure / 실패 시
     */
    private SwingWebPanel open() throws Exception {
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component.
             * <p>
             * 컴포넌트를 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(300, 300);
            }
        });
        final SwingWebPanel p = holder[0];
        panels.add(p);
        p.setPage(server.url("/maps"));
        await(p, server.url("/maps"));
        long end = System.currentTimeMillis() + 30000;
        // 그림을 요청할 때까지 기다린다(같은 주소의 이미지는 한 번만 요청한다).
        while (server.count("/pic.png") < 1 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Thread.sleep(600);
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
     * Sends one mouse event through the event dispatch of the component.
     * <p>
     * 컴포넌트의 이벤트 전달로 마우스 이벤트 하나를 보낸다.
     *
     * @param p    the component / 컴포넌트
     * @param id   the event id / 이벤트 종류
     * @param x    the x / x
     * @param y    the y / y
     * @throws Exception on failure / 실패 시
     */
    private static void mouse(final SwingWebPanel p, final int id, final int x, final int y) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the event.
             * <p>
             * 이벤트를 전달한다.
             */
            public void run() {
                p.dispatchEvent(new MouseEvent(p, id, System.currentTimeMillis(), 0, x, y, id == MouseEvent.MOUSE_CLICKED ? 1 : 0, false,
                        id == MouseEvent.MOUSE_CLICKED ? MouseEvent.BUTTON1 : MouseEvent.NOBUTTON));
            }
        });
    }

    /**
     * A click on an area follows the link of that area; a click on a dead area, which is the top-most shape there, does nothing even
     * though a shape with a link lies under it.
     * <p>
     * area 클릭은 그 area 의 링크를 따라간다. 그 자리에서 맨 위 모양인 죽은 영역의 클릭은 그 아래에 링크가 있는 모양이 있어도 아무것도 하지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testClickFollowsTheArea() throws Exception {
        SwingWebPanel p = open();
        mouse(p, MouseEvent.MOUSE_CLICKED, 5, 5);
        Thread.sleep(400);
        Check.equal(server.url("/maps"), p.getPage(), "the dead area catches the click");
        Check.equal(0, server.count("/red.html"), "and nothing is requested");
        mouse(p, MouseEvent.MOUSE_CLICKED, 25, 25);
        await(p, server.url("/red.html"));
        Check.equal(1, server.count("/red.html"), "the rectangle: red.html is requested once");
        p.back();
        await(p, server.url("/maps"));
        Thread.sleep(300);
        mouse(p, MouseEvent.MOUSE_CLICKED, 75, 25);
        await(p, server.url("/green.html"));
        p.back();
        await(p, server.url("/maps"));
        Thread.sleep(300);
        mouse(p, MouseEvent.MOUSE_CLICKED, 95, 5);
        Thread.sleep(400);
        Check.equal(server.url("/maps"), p.getPage(), "a point outside every shape of an image without a link does nothing");
    }

    /**
     * A click that no shape covers goes to the link around the image, and the area still wins where it covers.
     * <p>
     * 모양이 덮지 않는 클릭은 이미지를 감싼 링크로 가고, area 가 덮는 곳에서는 여전히 area 가 이긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPointOutsideEveryShapeFollowsTheLinkAroundTheImage() throws Exception {
        SwingWebPanel p = open();
        mouse(p, MouseEvent.MOUSE_CLICKED, 95, 80 + 5);
        await(p, server.url("/around.html"));
        p.back();
        await(p, server.url("/maps"));
        Thread.sleep(300);
        mouse(p, MouseEvent.MOUSE_CLICKED, 25, 80 + 25);
        await(p, server.url("/red.html"));
    }

    /**
     * A click on an image with <code>ismap</code> in a link requests the address with the position of the click.
     * <p>
     * <code>ismap</code> 이미지를 링크 안에서 클릭하면 클릭 위치가 붙은 주소를 요청한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testServerSideImageMapSendsTheClickPosition() throws Exception {
        SwingWebPanel p = open();
        mouse(p, MouseEvent.MOUSE_CLICKED, 37, 160 + 12);
        await(p, server.url("/cgi/map?37,12"));
        Check.equal(1, server.count("/cgi/map"), "the server got one request");
        Check.equal("37,12", server.requests().get(server.requests().size() - 1).query, "with the query x,y");
    }

    /**
     * The pointer over an area gives the hyperlink events of the link of that area: entered with its address, exited when the pointer
     * leaves; the dead area and the outside are not links.
     * <p>
     * area 위의 포인터는 그 area 링크의 하이퍼링크 이벤트를 준다: 그 주소로 진입하고 포인터가 벗어나면 이탈한다. 죽은 영역과 바깥은 링크가 아니다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHoverGivesHyperlinkEvents() throws Exception {
        SwingWebPanel p = open();
        final List<String> events = Collections.synchronizedList(new ArrayList<String>());
        p.addHyperlinkListener(new HyperlinkListener() {
            /**
             * Records the event.
             * <p>
             * 이벤트를 기록한다.
             *
             * @param e the event / 이벤트
             */
            public void hyperlinkUpdate(HyperlinkEvent e) {
                events.add(e.getEventType() + " " + e.getDescription());
            }
        });
        mouse(p, MouseEvent.MOUSE_MOVED, 25, 25);
        awaitEvents(events, 1);
        Check.equal("ENTERED " + server.url("/red.html"), events.get(0), "entered the rectangle");
        mouse(p, MouseEvent.MOUSE_MOVED, 5, 5);
        awaitEvents(events, 2);
        Check.equal("EXITED " + server.url("/red.html"), events.get(1), "the dead area is not a link: exited");
        mouse(p, MouseEvent.MOUSE_MOVED, 75, 25);
        awaitEvents(events, 3);
        Check.equal("ENTERED " + server.url("/green.html"), events.get(2), "entered the circle");
    }

    /**
     * Waits until the recorder has a number of events.
     * <p>
     * 기록기가 일정 수의 이벤트를 가질 때까지 기다린다.
     *
     * @param events the recorder / 기록기
     * @param count  the number / 개수
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitEvents(List<String> events, int count) throws Exception {
        long end = System.currentTimeMillis() + 10000;
        // 이벤트가 기록될 때까지 짧게 쉬며 기다린다.
        while (events.size() < count && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(events.size() >= count, "events: " + events);
    }

    /**
     * A script reads the areas of the map in tree order through <code>HTMLMapElement.areas</code> and the images that use the map
     * through <code>HTMLMapElement.images</code>, both live collections.
     * <p>
     * 스크립트가 <code>HTMLMapElement.areas</code> 로 맵의 area 를 트리 순서로, <code>HTMLMapElement.images</code> 로 맵을 쓰는 이미지를 읽는다. 둘 다 라이브 컬렉션이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testScriptReadsTheAreasAndImages() throws Exception {
        SwingWebPanel p = open();
        String value = "";
        // 복사한 저장소 항목에서 스크립트가 저장한 값을 찾는다.
        for (org.duckdns.hjow.swingwebpanel.storage.StorageEntry entry : p.getSessionStorage()) {
            if ("areas".equals(entry.getKey())) {
                value = entry.getValue();
            }
        }
        Check.equal("dead,red,green|2|true|true", value, "the areas in tree order, the two images that use the map, the first one, a collection");
    }
}
