package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * One local page that uses everything at once, run through the real component and a real HTTP server (plan items P22-05 and P15-12): an
 * external style sheet, an external script with <code>const</code>, arrow functions, a Promise and a timer, DOM events and DOM changes,
 * <code>sessionStorage</code> and <code>localStorage</code>, an image, an audio element, links (one cancelled by the script), a form, and
 * the cookie of the server. The test checks the requests on the wire, the cookie and the headers, the source that
 * <code>getContent()</code> returns, the pixels that the page paints, real AWT clicks, history moves, refresh, a switched-off JavaScript, and
 * the disposal at the end.
 * <p>
 * 모든 것을 한꺼번에 쓰는 로컬 페이지 하나를 실제 컴포넌트와 실제 HTTP 서버로 실행한다(계획 P22-05, P15-12): 외부 스타일시트, <code>const</code>, 화살표
 * 함수, Promise, 타이머가 있는 외부 스크립트, DOM 이벤트와 DOM 변경, <code>sessionStorage</code> 와 <code>localStorage</code>, 이미지, 오디오 요소, 링크(하나는
 * 스크립트가 취소), 폼, 서버의 쿠키. 선로의 요청, 쿠키와 헤더, <code>getContent()</code> 가 돌려주는 원문, 페이지가 그리는 화소, 실제 AWT 클릭, 이력 이동,
 * 새로고침, 꺼진 JavaScript, 마지막의 폐기를 확인한다.
 */
public class IntegratedPageIntegrationTest {

    /**
     * The markup of the page, exactly as the server sends it (the test compares it with <code>getContent()</code>).
     * <p>
     * 페이지의 마크업. 서버가 보내는 그대로이다(시험이 <code>getContent()</code> 와 비교한다).
     */
    private static final String APP_HTML = "<!doctype html>\n<html><head><meta charset=\"utf-8\"><title>Integrated</title>\n"
            + "<link rel=\"stylesheet\" href=\"/app.css\"></head>\n<body style=\"margin:0\">\n"
            + "<button id=\"btn\" style=\"position:absolute;left:0;top:0;width:100px;height:40px\">go</button>\n"
            + "<div id=\"out\">idle</div>\n"
            + "<a id=\"nolink\" href=\"/never\" style=\"position:absolute;left:0;top:100px;display:block;width:120px;height:30px\">cancelled</a>\n"
            + "<a id=\"second\" href=\"/second\" style=\"position:absolute;left:0;top:140px;display:block;width:120px;height:30px\">second</a>\n"
            + "<form action=\"/result\" method=\"get\" style=\"position:absolute;left:0;top:200px\"><input type=\"hidden\" name=\"q\" value=\"1\">"
            + "<input type=\"submit\" value=\"send\" style=\"width:100px;height:30px\"></form>\n"
            + "<img src=\"/pic.png\" alt=\"pic\" style=\"position:absolute;left:200px;top:0\">\n"
            + "<audio src=\"/a.wav\" preload=\"auto\"></audio>\n"
            + "<div style=\"position:absolute;left:0;top:1000px;width:50px;height:20px\">end</div>\n"
            + "<script src=\"/app.js\"></script>\n</body></html>\n";

    /**
     * The style sheet.
     * <p>
     * 스타일시트.
     */
    private static final String APP_CSS = "#out{position:absolute;left:0;top:50px;height:30px;color:#f00}.hot{background:#0000ff}.cold{background:#00ff00}";

    /**
     * The script: const, arrow functions, a Promise and a timer, DOM events, DOM changes and storage.
     * <p>
     * 스크립트: const, 화살표 함수, Promise, 타이머, DOM 이벤트, DOM 변경, 저장소.
     */
    private static final String APP_JS = "const state = { clicks: 0 };\nconst out = document.getElementById('out');\nconst log = [];\n"
            + "const render = () => {\n  out.textContent = 'clicks=' + state.clicks;\n  out.className = state.clicks % 2 ? 'hot' : 'cold';\n"
            + "  out.style.width = (40 + state.clicks * 10) + 'px';\n  sessionStorage.setItem('clicks', String(state.clicks));\n"
            + "  localStorage.setItem('last', String(state.clicks));\n};\n"
            + "document.getElementById('btn').addEventListener('click', () => { state.clicks++; render(); });\n"
            + "document.getElementById('nolink').addEventListener('click', e => { e.preventDefault(); sessionStorage.setItem('cancelled', '1'); });\n"
            + "log.push('sync');\nPromise.resolve().then(() => log.push('micro'));\n"
            + "setTimeout(() => { log.push('timer'); sessionStorage.setItem('order', log.join(',')); }, 0);\n";

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
    public IntegratedPageIntegrationTest() {
    }

    /**
     * Starts the server with the pages and the component.
     * <p>
     * 페이지가 있는 서버와 컴포넌트를 시작한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
        server.routeFixed("/app", TestHttpServer.Response.html(APP_HTML).header("Set-Cookie", "sid=7; Path=/"));
        server.routeFixed("/app.css", TestHttpServer.Response.ok("text/css", APP_CSS));
        server.routeFixed("/app.js", TestHttpServer.Response.ok("application/javascript", APP_JS));
        server.routeFixed("/pic.png", new TestHttpServer.Response(200).bytes("image/png", png(0xFF0000)));
        server.routeFixed("/a.wav", new TestHttpServer.Response(200).bytes("audio/wav", wav()));
        server.routeFixed("/second", TestHttpServer.Response.html("<p>second page</p>"));
        server.routeFixed("/result", TestHttpServer.Response.html("<p>result page</p>"));
        server.routeFixed("/never", TestHttpServer.Response.html("<p>must not be requested</p>"));
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
     * Creates a PNG of 8 by 8 pixels with one color.
     * <p>
     * 한 가지 색의 8x8 화소 PNG 를 만든다.
     *
     * @param rgb the color / 색
     * @return the file / 파일
     * @throws Exception on failure / 실패 시
     */
    private static byte[] png(int rgb) throws Exception {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        // 모든 화소를 같은 색으로 채운다.
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Creates a WAV file of one tenth of a second of silence (16 bit mono, 8000 Hz).
     * <p>
     * 0.1초의 무음 WAV 파일(16비트 모노, 8000 Hz)을 만든다.
     *
     * @return the file / 파일
     */
    private static byte[] wav() {
        int data = 1600;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeAscii(out, "RIFF");
        writeInt(out, 36 + data);
        writeAscii(out, "WAVEfmt ");
        writeInt(out, 16);
        out.write(1);
        out.write(0);
        out.write(1);
        out.write(0);
        writeInt(out, 8000);
        writeInt(out, 16000);
        out.write(2);
        out.write(0);
        out.write(16);
        out.write(0);
        writeAscii(out, "data");
        writeInt(out, data);
        // 데이터 부분은 0 바이트(무음)로 채운다.
        for (int i = 0; i < data; i++) {
            out.write(0);
        }
        return out.toByteArray();
    }

    /**
     * Writes ASCII text.
     * <p>
     * ASCII 문자열을 쓴다.
     *
     * @param out  the output / 출력
     * @param text the text / 문자열
     */
    private static void writeAscii(ByteArrayOutputStream out, String text) {
        // 문자를 하나씩 바이트로 쓴다.
        for (int i = 0; i < text.length(); i++) {
            out.write(text.charAt(i));
        }
    }

    /**
     * Writes a little endian 32 bit integer.
     * <p>
     * 리틀 엔디언 32비트 정수를 쓴다.
     *
     * @param out the output / 출력
     * @param v   the value / 값
     */
    private static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v & 0xff);
        out.write((v >> 8) & 0xff);
        out.write((v >> 16) & 0xff);
        out.write((v >> 24) & 0xff);
    }

    /**
     * Returns a value of the session storage by key, or null.
     * <p>
     * 세션 저장소에서 키로 값을 찾는다. 없으면 null.
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
     * Waits until the component confirmed an address, finished loading and has a text in its source.
     * <p>
     * 컴포넌트가 주소를 확정하고 로딩을 끝내며 원문에 텍스트가 있을 때까지 기다린다.
     *
     * @param url    the address / 주소
     * @param source a text of the source / 원문의 텍스트
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void await(String url, String source) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 주소가 확정되고 로딩이 끝나며 원문이 도착할 때까지 짧게 쉬며 기다린다.
        while ((!url.equals(panel.getPage()) || panel.getProgress() != 100 || panel.getContent().indexOf(source) < 0)
                && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(url, panel.getPage(), "confirmed page");
        Check.contains(panel.getContent(), source, "source of " + url);
    }

    /**
     * Waits until a session storage value has the wanted text.
     * <p>
     * 세션 저장소의 값이 원하는 문자열이 될 때까지 기다린다.
     *
     * @param key   the key / 키
     * @param value the wanted value / 원하는 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitSession(String key, String value) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 기다린다.
        while (!value.equals(session(key)) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(value, session(key), "session storage " + key);
    }

    /**
     * Clicks a point of the component through its event dispatch, as the AWT does.
     * <p>
     * AWT 가 하는 것처럼 컴포넌트의 이벤트 전달로 한 지점을 클릭한다.
     *
     * @param x x / x
     * @param y y / y
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private void click(final int x, final int y) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Dispatches the click.
             * <p>
             * 클릭을 전달한다.
             */
            public void run() {
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Paints the component on the EDT and waits until a pixel has a color.
     * <p>
     * 컴포넌트를 EDT 에서 그리고 화소가 어떤 색이 될 때까지 기다린다.
     *
     * @param x       x / x
     * @param y       y / y
     * @param rgb     the expected color / 기대한 색
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitPixel(int x, int y, int rgb, String message) throws Exception {
        long end = System.currentTimeMillis() + 30000;
        int actual = 0;
        // 문서 작업과 EDT 게시가 끝날 때까지 짧게 쉬며 그려진 화소를 확인한다.
        do {
            final BufferedImage[] image = new BufferedImage[1];
            SwingUtilities.invokeAndWait(new Runnable() {
                /**
                 * Paints into an image without a native window.
                 * <p>
                 * 네이티브 창 없이 이미지에 그린다.
                 */
                public void run() {
                    image[0] = new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = image[0].createGraphics();
                    try {
                        panel.paint(g);
                    } finally {
                        g.dispose();
                    }
                }
            });
            actual = image[0].getRGB(x, y) & 0xFFFFFF;
            if (actual == rgb) {
                return;
            }
            Thread.sleep(15);
        } while (System.currentTimeMillis() < end);
        Check.equal(Integer.toHexString(rgb), Integer.toHexString(actual), message);
    }

    /**
     * The whole page: requests, cookie, headers, source, order of the script work, pixels, clicks, cancelled link, history, refresh, form,
     * switched-off JavaScript and disposal.
     * <p>
     * 페이지 전체: 요청, 쿠키, 헤더, 원문, 스크립트 작업의 순서, 화소, 클릭, 취소된 링크, 이력, 새로고침, 폼, 꺼진 JavaScript, 폐기.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTheWholePage() throws Exception {
        panel.setUserAgent("Integrated/1");
        panel.setPage(server.url("/app"));
        await(server.url("/app"), "Integrated");
        // 스크립트가 끝나면 동기, 마이크로태스크, 타이머 순서가 저장된다.
        awaitSession("order", "sync,micro,timer");
        Check.equal(APP_HTML, panel.getContent(), "getContent() returns the received source, not the changed document");
        Check.equal("Integrated", panel.getTitle(), "the title");
        awaitPixel(203, 3, 0xFF0000, "the image was decoded and painted");
        awaitScrollExtent();
        awaitPixel(5, 55, 0xFFFFFF, "the style sheet applies to #out, which has no class yet: white background");
        Check.equal("Integrated/1", server.last("/app.js").header("User-Agent"), "the user agent reaches the sub resources");
        Check.equal("sid=7", server.last("/app.js").header("Cookie"), "the cookie of the page goes with the script request");
        Check.equal(server.url("/app"), server.last("/app.css").header("Referer"), "the page is the referrer of its style sheet");
        Check.isTrue(server.count("/pic.png") >= 1, "the image was requested");
        long end = System.currentTimeMillis() + 10000;
        // 오디오 요소의 소스 요청이 도착할 때까지 기다린다.
        while (server.count("/a.wav") < 1 && System.currentTimeMillis() < end) {
            Thread.sleep(20);
        }
        Check.isTrue(server.count("/a.wav") >= 1, "the audio source was requested");
        Check.equal("sid=7", panel.getCookies(), "the cookie is in the component");
        // 단추를 세 번 클릭하면 스크립트가 글자, class, 크기, 저장소를 바꾼다.
        for (int n = 1; n <= 3; n++) {
            click(20, 20);
            awaitSession("clicks", String.valueOf(n));
            awaitPixel(5, 55, n % 2 == 1 ? 0x0000FF : 0x00FF00, "the class of #out after click " + n);
        }
        Check.equal("3", valueOf(panel.getLocalStorage(), "last"), "the script wrote the local storage");
        Check.equal(APP_HTML, panel.getContent(), "the source is unchanged after the script changed the page");
        // 스크립트가 취소한 링크는 이동하지 않는다.
        click(20, 110);
        awaitSession("cancelled", "1");
        Thread.sleep(200);
        Check.equal(server.url("/app"), panel.getPage(), "the cancelled link did not navigate");
        Check.equal(0, server.count("/never"), "the cancelled link was never requested");
        // 취소하지 않은 링크는 이동하고 이력에 항목이 생긴다.
        click(20, 150);
        await(server.url("/second"), "second page");
        Check.equal(3, panel.getHistories().size(), "history: blank, app, second: " + panel.getHistories());
        panel.back();
        await(server.url("/app"), "Integrated");
        Check.equal(APP_HTML, panel.getContent(), "back restores the received source");
        awaitSession("order", "sync,micro,timer");
        panel.forward();
        await(server.url("/second"), "second page");
        int before = server.count("/second");
        panel.refresh(true);
        await(server.url("/second"), "second page");
        end = System.currentTimeMillis() + 10000;
        // 강제 새로고침의 요청이 서버에 닿을 때까지 기다린다.
        while (server.count("/second") <= before && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(server.count("/second") > before, "refresh(true) asked the server again");
        Check.equal(3, panel.getHistories().size(), "refresh adds no entry");
        // 폼 제출은 GET 이동이다.
        panel.back();
        await(server.url("/app"), "Integrated");
        click(20, 215);
        await(server.url("/result?q=1"), "result page");
        // JavaScript 를 끄면 스크립트는 멈추지만 CSS, 링크, 폼은 동작한다.
        panel.setJavascriptEnabled(false);
        panel.setPage(server.url("/app"));
        await(server.url("/app"), "Integrated");
        Thread.sleep(300);
        Check.equal("3", session("clicks"), "no script ran while JavaScript is off");
        click(20, 20);
        Thread.sleep(300);
        Check.equal("3", session("clicks"), "the click changed nothing without JavaScript");
        awaitPixel(203, 3, 0xFF0000, "the image still shows");
        click(20, 215);
        await(server.url("/result?q=1"), "result page");
        // 폐기한 뒤에는 요청이 더 오지 않고 상태가 비어 있다.
        panel.dispose();
        int total = server.totalRequests();
        Thread.sleep(500);
        Check.equal(total, server.totalRequests(), "no request after dispose");
        Check.equal("about:blank", panel.getPage(), "disposed: blank");
        Check.equal("", panel.getContent(), "disposed: empty");
    }

    /**
     * Waits until the height that the page asks for covers the box at the bottom, so that a scroll pane would scroll to it.
     * <p>
     * 페이지가 요구하는 높이가 아래쪽 상자를 덮을 때까지 기다린다. 그러면 스크롤 창이 거기까지 스크롤할 수 있다.
     *
     * @throws Exception if interrupted / 인터럽트되면
     */
    private void awaitScrollExtent() throws Exception {
        long end = System.currentTimeMillis() + 30000;
        // 배치가 끝나 선호 높이가 아래쪽 상자를 덮을 때까지 짧게 쉬며 기다린다.
        while (panel.getPreferredSize().height < 1020 && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.isTrue(panel.getPreferredSize().height >= 1020, "the page is taller than the view, a scroll pane can scroll: " + panel.getPreferredSize());
        Check.isFalse(panel.getScrollableTracksViewportHeight(), "the height is not forced to the viewport");
    }

    /**
     * Returns the value of a storage entry by key, or null.
     * <p>
     * 저장소 항목의 값을 키로 찾는다. 없으면 null.
     *
     * @param entries the entries / 항목
     * @param key     the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String valueOf(Collection<StorageEntry> entries, String key) {
        List<StorageEntry> list = new ArrayList<StorageEntry>(entries);
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : list) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }
}
