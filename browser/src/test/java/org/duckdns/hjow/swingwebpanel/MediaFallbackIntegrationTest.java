package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * What the page shows in the place of media that cannot be shown, through the real component and a real server (plan items P18-06 and
 * P18-09 at the level of the component): an image and an audio source that the server answers with 404, an image without a source but with
 * an <code>alt</code> text, an audio element with controls, and a <code>video</code> element, which is never played and shows its notice.
 * The test checks the requests, the <code>error</code> events, the answers of the video API and, through the painted pixels, that every one of
 * these places is not left empty.
 * <p>
 * 보여 줄 수 없는 미디어 자리에 페이지가 무엇을 보여 주는지를 실제 컴포넌트와 실제 서버로 확인한다(계획 P18-06, P18-09 의 컴포넌트 수준): 서버가
 * 404 로 답하는 이미지와 오디오 소스, 소스는 없고 <code>alt</code> 만 있는 이미지, 컨트롤이 있는 오디오 요소, 재생하지 않고 안내를 보여 주는
 * <code>video</code> 요소. 요청, <code>error</code> 이벤트, 비디오 API 의 답, 그려진 화소로 이 자리들이 비어 있지 않음을 확인한다.
 */
public class MediaFallbackIntegrationTest {

    /**
     * The markup of the page.
     * <p>
     * 페이지의 마크업.
     */
    private static final String PAGE = "<!doctype html><meta charset=\"utf-8\"><body style=\"margin:0;background:#ffffff\">"
            + "<img id=\"i1\" src=\"/missing.png\" alt=\"broken alt\" onerror=\"sessionStorage.setItem('imgerror', '1')\" style=\"position:absolute;left:0;top:0;width:200px;height:50px\">"
            + "<img id=\"i2\" alt=\"no source alt\" style=\"position:absolute;left:0;top:60px;width:200px;height:50px\">"
            + "<audio id=\"a1\" src=\"/missing.wav\" alt=\"audio alt\" controls onerror=\"sessionStorage.setItem('audioerror', String(this.error && this.error.code))\" style=\"position:absolute;left:0;top:120px;width:200px;height:40px\"></audio>"
            + "<video id=\"v1\" style=\"position:absolute;left:0;top:170px;width:200px;height:100px\"></video>"
            + "<script>"
            + "var v = document.getElementById('v1');"
            + "sessionStorage.setItem('canplay', v.canPlayType('video/mp4') + '|' + v.canPlayType('video/webm') + '|' + v.canPlayType('video/ogg'));"
            + "try { var p = v.play(); sessionStorage.setItem('vplay', typeof p); if (p && p.then) { p.then(function () { sessionStorage.setItem('vplayed', '1'); },"
            + " function (e) { sessionStorage.setItem('vplayerr', e.name); }); } } catch (e) { sessionStorage.setItem('vplay', 'threw:' + e.name); }"
            + "</" + "script>";

    /**
     * The test server.
     * <p>
     * 시험 서버.
     */
    private TestHttpServer server;

    /**
     * The component of the case.
     * <p>
     * 사례의 컴포넌트.
     */
    private SwingWebPanel panel;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public MediaFallbackIntegrationTest() {
    }

    /**
     * Starts the server and the component.
     * <p>
     * 서버와 컴포넌트를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/media", TestHttpServer.Response.html(PAGE));
        server.routeFixed("/missing.png", new TestHttpServer.Response(404).text("text/plain", "no image"));
        server.routeFixed("/missing.wav", new TestHttpServer.Response(404).text("text/plain", "no audio"));
        server.start();
        final SwingWebPanel[] holder = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Creates the component on the EDT.
             * <p>
             * EDT 에서 컴포넌트를 만든다.
             */
            public void run() {
                holder[0] = new SwingWebPanel();
                holder[0].setSize(400, 300);
            }
        });
        panel = holder[0];
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
     * Returns a value of the session storage, or null.
     * <p>
     * 세션 저장소의 값을 반환한다. 없으면 null.
     *
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private String session(String key) {
        Collection<StorageEntry> entries = panel.getSessionStorage();
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : entries) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until a session storage key has a value, at most 30 seconds.
     * <p>
     * 세션 저장소의 키에 값이 생길 때까지 최대 30초 기다린다.
     *
     * @param key the key / 키
     * @return the value / 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private String awaitSession(String key) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트나 이벤트가 값을 저장할 때까지 짧게 쉬며 기다린다.
        while (session(key) == null && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.notNull(session(key), "session storage " + key);
        return session(key);
    }

    /**
     * Paints the component on the EDT into an image.
     * <p>
     * 컴포넌트를 EDT 에서 이미지에 그린다.
     *
     * @return the picture / 그림
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private BufferedImage snapshot() throws Exception {
        final BufferedImage[] result = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints into an image.
             * <p>
             * 이미지에 그린다.
             */
            public void run() {
                result[0] = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = result[0].createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
            }
        });
        return result[0];
    }

    /**
     * Counts the pixels in a rectangle that are not white.
     * <p>
     * 사각형 안에서 흰색이 아닌 화소를 센다.
     *
     * @param image the picture / 그림
     * @param x     left / 왼쪽
     * @param y     top / 위쪽
     * @param w     width / 너비
     * @param h     height / 높이
     * @return the number / 개수
     */
    private static int inked(BufferedImage image, int x, int y, int w, int h) {
        int n = 0;
        // 사각형의 화소를 하나씩 보며 흰색이 아닌 것을 센다.
        for (int j = y; j < y + h; j++) {
            for (int i = x; i < x + w; i++) {
                if ((image.getRGB(i, j) & 0xFFFFFF) != 0xFFFFFF) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Waits until a rectangle of the painted component has at least a number of non-white pixels.
     * <p>
     * 그려진 컴포넌트의 사각형에 흰색이 아닌 화소가 일정 개수 이상이 될 때까지 기다린다.
     *
     * @param x       left / 왼쪽
     * @param y       top / 위쪽
     * @param w       width / 너비
     * @param h       height / 높이
     * @param atLeast the least number / 최소 개수
     * @param what    what the place shows / 그 자리가 보여 주는 것
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitInk(int x, int y, int w, int h, int atLeast, String what) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int n = 0;
        // 그려진 화소를 짧게 쉬며 다시 확인한다.
        do {
            n = inked(snapshot(), x, y, w, h);
            if (n >= atLeast) {
                return;
            }
            Thread.sleep(20);
        } while (System.currentTimeMillis() < end);
        Check.isTrue(n >= atLeast, what + " is not left empty: " + n + " non-white pixels");
    }

    /**
     * The places of broken media are not empty, the events and the video answers are explicit, and the source is untouched.
     * <p>
     * 깨진 미디어의 자리는 비어 있지 않고, 이벤트와 비디오의 답은 명시적이며, 원문은 그대로이다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlacesOfBrokenMediaAndTheVideoNotice() throws Exception {
        panel.setPage(server.url("/media"));
        long end = System.currentTimeMillis() + 30000;
        // 페이지가 확정되고 로딩이 끝날 때까지 기다린다.
        while ((!server.url("/media").equals(panel.getPage()) || panel.getProgress() != 100) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(PAGE, panel.getContent(), "the received source is returned");
        Check.equal("1", awaitSession("imgerror"), "the image failure reached the page as an error event");
        Check.notNull(awaitSession("audioerror"), "the audio failure reached the page as an error event");
        Check.isTrue(server.count("/missing.png") >= 1, "the image was requested");
        Check.isTrue(server.count("/missing.wav") >= 1, "the audio source was requested");
        awaitInk(0, 0, 200, 50, 40, "the failed image shows its address and alt text");
        awaitInk(0, 60, 200, 50, 20, "the image without source shows its alt text");
        awaitInk(0, 120, 200, 40, 20, "the failed audio shows its address and alt text");
        awaitInk(0, 170, 200, 100, 40, "the video shows the notice");
        Check.equal("||", awaitSession("canplay"), "a video type is never said to be playable");
        String played = awaitSession("vplay");
        Check.isTrue("undefined".equals(played) || "object".equals(played) || played.startsWith("threw:"), "play() answers explicitly: " + played);
        long wait = System.currentTimeMillis() + 5000;
        // 재생 약속이 있다면 거부되거나 아무 일도 하지 않을 때까지 짧게 기다린다.
        while (session("vplayerr") == null && "object".equals(played) && System.currentTimeMillis() < wait) {
            Thread.sleep(20);
        }
        Check.isNull(session("vplayed"), "the video was never played");
        Check.isFalse(server.count("/missing.png") == 0, "sanity: the server saw the image request");
    }
    /**
     * Images whose address has a scheme other than http, https and ftp are unsupported (P01-12, IMG-10): the component never requests
     * them, the page gets an <code>error</code> event and the place shows the address and the alt text, as for any failed image.
     * <p>
     * http, https, ftp 가 아닌 스킴의 주소를 가진 이미지는 지원하지 않는다(P01-12, IMG-10): 컴포넌트는 요청하지 않고 페이지는 <code>error</code> 이벤트를
     * 받으며 그 자리에는 다른 실패한 이미지처럼 주소와 alt 문구가 보인다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImagesOfUnsupportedSchemesFailWithTheirAddressAndAlt() throws Exception {
        String style = "position:absolute;left:0;width:200px;height:50px;top:";
        String page = "<!doctype html><meta charset=\"utf-8\"><body style=\"margin:0;background:#ffffff\">"
                + "<img src=\"data:image/png;base64,iVBORw0KGgo=\" alt=\"data alt\" onerror=\"sessionStorage.setItem('derr','1')\" style=\"" + style + "0px\">"
                + "<img src=\"file:///c:/windows/win.ini\" alt=\"file alt\" onerror=\"sessionStorage.setItem('ferr','1')\" style=\"" + style + "60px\">"
                + "<img src=\"blob:" + server.url("/blobbed.png") + "\" alt=\"blob alt\" onerror=\"sessionStorage.setItem('berr','1')\" style=\"" + style + "120px\">";
        server.routeFixed("/schemes", TestHttpServer.Response.html(page));
        server.routeFixed("/blobbed.png", new TestHttpServer.Response(200).text("text/plain", "must never be requested"));
        panel.setPage(server.url("/schemes"));
        Check.equal("1", awaitSession("derr"), "data: image failed with an error event");
        Check.equal("1", awaitSession("ferr"), "file: image failed with an error event");
        Check.equal("1", awaitSession("berr"), "blob: image failed with an error event");
        awaitInk(0, 0, 200, 50, 40, "the data: image shows its address and alt text");
        awaitInk(0, 60, 200, 50, 40, "the file: image shows its address and alt text");
        awaitInk(0, 120, 200, 50, 40, "the blob: image shows its address and alt text");
        Check.equal(0, server.count("/blobbed.png"), "the address inside the blob: URL was never requested");
    }
}
