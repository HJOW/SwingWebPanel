package org.duckdns.hjow.swingwebpanel.core;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The status and progress of a page after it was confirmed, through the real component and real HTTP (plan items P19-08, P15-04-b-2 for
 * stylesheets and images): a slow stylesheet or image keeps the progress between 50 and 94 and delays the window <code>load</code> event,
 * a failed resource counts as ended, a page without scripts waits for the images that its layout starts, a replaced page never changes the
 * progress of the new one, and the progress never decreases while a page loads.
 * <p>
 * 확정된 뒤 페이지의 상태와 진행률을 실제 컴포넌트와 실제 HTTP 로 확인하는 시험(계획 P19-08, 스타일시트와 이미지에 대한 P15-04-b-2): 느린
 * 스타일시트나 이미지는 진행률을 50~94 에 두고 window <code>load</code> 이벤트를 늦추며, 실패한 리소스는 끝난 것으로 세고, 스크립트가 없는 페이지는
 * 배치가 시작한 이미지를 기다리며, 교체된 페이지는 새 페이지의 진행률을 바꾸지 못하고, 진행률은 페이지를 로딩하는 동안 줄어들지 않는다.
 */
public class LoadProgressIntegrationTest {

    /**
     * Script of the test pages: it stores the order of the events of the page in the sessionStorage of the component.
     * <p>
     * 시험 페이지의 스크립트: 페이지의 이벤트 순서를 컴포넌트의 sessionStorage 에 저장한다.
     */
    private static final String SCRIPT = "<script>function mark(n){sessionStorage.setItem('order',(sessionStorage.getItem('order')||'')+n+',');}"
            + "window.addEventListener('load',function(){mark('win');});</script>";

    /**
     * A server that answers one request with the first half of the body, waits for the gate, and then sends the rest. It lets a test see
     * the progress while a body is only partly received.
     * <p>
     * 요청 하나에 본문의 앞쪽 절반으로 응답하고, 문이 열릴 때까지 기다린 뒤 나머지를 보내는 서버. 본문을 일부만 받은 동안의 진행률을 시험이 볼 수
     * 있게 한다.
     */
    private static final class HalfBodyServer implements Runnable {
        /** The listening socket. / 대기 소켓. */
        private final ServerSocket socket;
        /** Whether the response declares its length. / 응답이 길이를 선언하는지 여부. */
        private final boolean withLength;
        /** Opened by the test to let the second half go. / 시험이 열면 뒷부분을 보낸다. */
        private final CountDownLatch gate;
        /** The body. / 본문. */
        private final byte[] body;

        /**
         * Starts the server on a free port of the loopback address.
         * <p>
         * 루프백 주소의 빈 포트에서 서버를 시작한다.
         *
         * @param withLength whether to send Content-Length / Content-Length 를 보낼지 여부
         * @param gate       the gate of the second half / 뒷부분의 문
         * @param body       the body / 본문
         * @throws IOException if the socket cannot be opened / 소켓을 열 수 없으면
         */
        HalfBodyServer(boolean withLength, CountDownLatch gate, byte[] body) throws IOException {
            this.socket = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));
            this.withLength = withLength;
            this.gate = gate;
            this.body = body;
            Thread t = new Thread(this, "half-body-server");
            t.setDaemon(true);
            t.start();
        }

        /**
         * Returns the URL of the page.
         * <p>
         * 페이지의 URL 을 반환한다.
         *
         * @return the URL / URL
         */
        String url() {
            return "http://127.0.0.1:" + socket.getLocalPort() + "/page";
        }

        /**
         * Closes the socket.
         * <p>
         * 소켓을 닫는다.
         */
        void close() {
            try {
                socket.close();
            } catch (IOException e) {
                return;
            }
        }

        /**
         * Serves one request.
         * <p>
         * 요청 하나를 처리한다.
         */
        public void run() {
            try {
                Socket s = socket.accept();
                try {
                    InputStream in = s.getInputStream();
                    StringBuilder head = new StringBuilder();
                    int b;
                    // 요청 머리가 끝나는 빈 줄까지 읽는다.
                    while (!head.toString().endsWith("\r\n\r\n") && (b = in.read()) >= 0) {
                        head.append((char) b);
                    }
                    OutputStream out = s.getOutputStream();
                    String answer = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nConnection: close\r\n"
                            + (withLength ? "Content-Length: " + body.length + "\r\n" : "") + "\r\n";
                    out.write(answer.getBytes("US-ASCII"));
                    int half = body.length / 2;
                    out.write(body, 0, half);
                    out.flush();
                    gate.await(30, TimeUnit.SECONDS);
                    out.write(body, half, body.length - half);
                    out.flush();
                } finally {
                    s.close();
                }
            } catch (Exception e) {
                // 시험이 끝나 소켓이 닫히면 서버도 끝난다.
                return;
            } finally {
                close();
            }
        }
    }

    /**
     * Panels created by the case.
     * <p>
     * 사례가 만든 패널.
     */
    private final List<SwingWebPanel> panels = new ArrayList<SwingWebPanel>();

    /**
     * Raw servers created by the case.
     * <p>
     * 사례가 만든 날 서버.
     */
    private final List<HalfBodyServer> rawServers = new ArrayList<HalfBodyServer>();

    /**
     * Servers created by the case.
     * <p>
     * 사례가 만든 서버.
     */
    private final List<TestHttpServer> servers = new ArrayList<TestHttpServer>();

    /**
     * Gates that the case holds closed; they are opened in tearDown so no server thread waits for ever.
     * <p>
     * 사례가 닫아 둔 문. 서버 스레드가 영원히 기다리지 않도록 tearDown 에서 연다.
     */
    private final List<CountDownLatch> gates = new ArrayList<CountDownLatch>();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LoadProgressIntegrationTest() {
    }

    /**
     * Opens every gate and releases everything the case created.
     * <p>
     * 모든 문을 열고 사례가 만든 것을 모두 해제한다.
     */
    public void tearDown() {
        // 닫아 둔 문을 모두 열어 서버 스레드를 풀어 준다.
        for (CountDownLatch g : gates) {
            g.countDown();
        }
        // 만든 패널을 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        // 시작한 서버를 모두 중지한다.
        for (TestHttpServer s : servers) {
            s.stop();
        }
        // 시작한 날 서버를 모두 닫는다.
        for (HalfBodyServer s : rawServers) {
            s.close();
        }
    }

    /**
     * Starts the server of a case with the given pages, a slow stylesheet (<code>/slow.css</code>) and a slow image (<code>/slow.png</code>)
     * that answer when the returned gate is opened.
     * <p>
     * 사례의 서버를 주어진 페이지와, 돌려주는 문이 열릴 때 응답하는 느린 스타일시트(<code>/slow.css</code>), 느린 이미지(<code>/slow.png</code>)와
     * 함께 시작한다.
     *
     * @param server the server (not started) / 서버(시작 전)
     * @return the gate of the slow resources / 느린 리소스의 문
     * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
     */
    private CountDownLatch serve(TestHttpServer server) throws Exception {
        final CountDownLatch gate = new CountDownLatch(1);
        gates.add(gate);
        final byte[] image = png(6, 4);
        server.route("/slow.css", new TestHttpServer.Handler() {
            /**
             * Answers when the gate is opened.
             * <p>
             * 문이 열리면 응답한다.
             *
             * @param request the request / 요청
             * @return the stylesheet / 스타일시트
             * @throws Exception if interrupted / 인터럽트되면
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                gate.await(30, TimeUnit.SECONDS);
                return TestHttpServer.Response.ok("text/css", "p { color: red }");
            }
        });
        server.route("/slow.png", new TestHttpServer.Handler() {
            /**
             * Answers when the gate is opened.
             * <p>
             * 문이 열리면 응답한다.
             *
             * @param request the request / 요청
             * @return the image / 이미지
             * @throws Exception if interrupted / 인터럽트되면
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                gate.await(30, TimeUnit.SECONDS);
                return new TestHttpServer.Response(200).bytes("image/png", image);
            }
        });
        server.routeFixed("/missing.png", new TestHttpServer.Response(404).text("text/plain", "no"));
        server.routeFixed("/fast", TestHttpServer.Response.html("<p>fast</p>"));
        servers.add(server);
        return gate;
    }

    /**
     * Makes the bytes of a PNG image.
     * <p>
     * PNG 이미지의 바이트를 만든다.
     *
     * @param width  the width / 너비
     * @param height the height / 높이
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    /**
     * Reads the order that the scripts stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 순서를 읽는다.
     *
     * @param p the component / 컴포넌트
     * @return the order, empty if nothing was stored / 순서. 저장된 것이 없으면 빈 문자열
     */
    private static String order(SwingWebPanel p) {
        // 복사한 저장소 항목에서 순서 키를 찾는다.
        for (StorageEntry entry : p.getSessionStorage()) {
            if ("order".equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "";
    }

    /**
     * Waits until the status text of the component starts with a text.
     * <p>
     * 컴포넌트의 상태 메시지가 어떤 텍스트로 시작할 때까지 기다린다.
     *
     * @param p      the component / 컴포넌트
     * @param prefix the beginning of the status / 상태의 앞부분
     */
    private static void awaitStatus(SwingWebPanel p, String prefix) {
        long end = System.currentTimeMillis() + 30000;
        // 상태가 기대한 대로 될 때까지 짧게 쉬며 기다린다.
        while (!p.getStatusText().startsWith(prefix) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        Check.isTrue(p.getStatusText().startsWith(prefix), "the status starts with '" + prefix + "' but is '" + p.getStatusText() + "'");
    }

    /**
     * Waits until the load ended: "Done" and progress 100.
     * <p>
     * 로딩이 끝날 때까지(상태 "Done", 진행률 100) 기다린다.
     *
     * @param p the component / 컴포넌트
     */
    private static void awaitDone(SwingWebPanel p) {
        awaitStatus(p, "Done");
        long end = System.currentTimeMillis() + 30000;
        // 진행률이 100 이 될 때까지 짧게 쉬며 기다린다.
        while (p.getProgress() != 100 && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        TestSupport.flushEdt();
        Check.equal(100, p.getProgress(), "the progress of the finished load");
    }

    /**
     * Records every change of the progress property on the EDT.
     * <p>
     * 진행률 속성의 모든 변경을 EDT 에서 기록한다.
     *
     * @param p the component / 컴포넌트
     * @return the list that receives the values / 값을 받는 목록
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static List<Integer> record(final SwingWebPanel p) throws Exception {
        final List<Integer> values = new ArrayList<Integer>();
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Registers the listener.
             * <p>
             * 리스너를 등록한다.
             */
            public void run() {
                p.addPropertyChangeListener("progress", new PropertyChangeListener() {
                    /**
                     * Records a new value.
                     * <p>
                     * 새 값을 기록한다.
                     *
                     * @param e the event / 이벤트
                     */
                    public void propertyChange(PropertyChangeEvent e) {
                        synchronized (values) {
                            values.add((Integer) e.getNewValue());
                        }
                    }
                });
            }
        });
        return values;
    }

    /**
     * A slow stylesheet keeps the page loading: the status counts resources, the progress stays between 50 and 94, the window
     * <code>load</code> event waits, and "Done" (100) comes after the stylesheet arrived; the progress never decreased.
     * <p>
     * 느린 스타일시트는 페이지를 계속 로딩 중으로 둔다: 상태가 리소스를 세고, 진행률은 50~94 에 머물며, window <code>load</code> 이벤트가 기다리고,
     * 스타일시트가 도착한 뒤에 "Done"(100)이 된다. 진행률은 줄어들지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testSlowStylesheetKeepsTheLoadOpen() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CountDownLatch gate = serve(server);
        // 스크립트는 스타일시트보다 앞에 둔다. 스타일시트 뒤의 스크립트는 파서를 막아 페이지가 확정되기 전에 스타일시트를 기다린다.
        server.routeFixed("/css", TestHttpServer.Response.html("<!doctype html><html><head>" + SCRIPT
                + "<link rel=stylesheet href='slow.css'></head><body><p>text</p></body></html>"));
        server.start();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        List<Integer> values = record(p);
        p.setPage(server.url("/css"));
        awaitStatus(p, "Loading resources (0/1)");
        Check.equal(server.url("/css"), p.getPage(), "the page itself was confirmed");
        Check.isTrue(p.getProgress() >= 50 && p.getProgress() <= 94, "the progress is between 50 and 94: " + p.getProgress());
        Thread.sleep(300);
        Check.isTrue(p.getStatusText().startsWith("Loading resources (0/1)"), "still waiting for the stylesheet: " + p.getStatusText());
        Check.equal("", order(p), "the window load event waits for the stylesheet");
        gate.countDown();
        awaitDone(p);
        Check.equal("win,", order(p), "the load event fired once the stylesheet had arrived");
        TestSupport.flushEdt();
        synchronized (values) {
            int last = -2;
            // 50 이상이 된 뒤의 진행률은 줄어들지 않아야 한다.
            for (Integer v : values) {
                if (v.intValue() >= 50) {
                    Check.isTrue(v.intValue() >= last, "the progress never decreases once the page was confirmed: " + values);
                    last = v.intValue();
                }
            }
            Check.equal(100, last, "the last progress is 100: " + values);
        }
    }

    /**
     * A slow image and a failed image: the page waits for the slow one, the failed one counts as ended, the image events come before the
     * window <code>load</code> event, and the progress ends at 100.
     * <p>
     * 느린 이미지와 실패한 이미지: 페이지는 느린 이미지를 기다리고, 실패한 이미지는 끝난 것으로 세며, 이미지 이벤트는 window <code>load</code>
     * 이벤트보다 먼저 오고, 진행률은 100 에서 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testImagesKeepTheLoadOpenAndAFailureDoesNotStickIt() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CountDownLatch gate = serve(server);
        server.routeFixed("/img", TestHttpServer.Response.html("<!doctype html><html><body>" + SCRIPT
                + "<img src='slow.png' onload=\"mark('slow')\" onerror=\"mark('slow-error')\">"
                + "<img src='missing.png' onload=\"mark('missing-load')\" onerror=\"mark('missing-error')\"></body></html>"));
        server.start();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(server.url("/img"));
        awaitStatus(p, "Loading resources (1/2)");
        Check.isTrue(p.getProgress() >= 50 && p.getProgress() <= 94, "the progress is between 50 and 94: " + p.getProgress());
        Check.isTrue(order(p).indexOf("win") < 0, "no load event yet: " + order(p));
        gate.countDown();
        awaitDone(p);
        String order = order(p);
        Check.isTrue(order.endsWith("win,"), "the load event is the last event: " + order);
        Check.isTrue(order.indexOf("slow,") >= 0 && order.indexOf("missing-error,") >= 0, "both image events fired: " + order);
        Check.isTrue(order.indexOf("slow-error") < 0 && order.indexOf("missing-load") < 0, "no wrong event: " + order);
    }

    /**
     * Without scripts nothing starts the images in the document, so the layout of the page finds them: the page still waits for them, and
     * ends when they ended.
     * <p>
     * 스크립트가 없으면 문서의 이미지를 시작하는 것이 없으므로 페이지의 배치가 그것을 찾는다. 그래도 페이지는 이미지를 기다리고, 끝나면 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPageWithoutScriptsWaitsForTheImagesOfItsLayout() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CountDownLatch gate = serve(server);
        server.routeFixed("/plainimg", TestHttpServer.Response.html("<!doctype html><html><body><img src='slow.png'></body></html>"));
        server.start();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setJavascriptEnabled(false);
        p.setPage(server.url("/plainimg"));
        awaitStatus(p, "Loading resources (0/1)");
        Check.isTrue(p.getProgress() >= 50 && p.getProgress() <= 94, "the progress is between 50 and 94: " + p.getProgress());
        gate.countDown();
        awaitDone(p);
        Check.equal(1, server.count("/slow.png"), "the image was requested once");
    }

    /**
     * While a newer navigation loads, the resources that the old page still waits for do not touch the status or the progress: they belong
     * to the navigation in flight, and the old page's own progress does not come back when that navigation ends.
     * <p>
     * 더 새로운 이동이 로딩되는 동안 이전 페이지가 아직 기다리던 리소스는 상태나 진행률을 건드리지 않는다. 그것은 진행 중인 이동의 것이며, 그 이동이
     * 끝나도 이전 페이지의 진행률이 돌아오지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testResourcesOfTheOldPageDoNotTouchAPendingNavigation() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CountDownLatch cssGate = serve(server);
        final CountDownLatch pageGate = new CountDownLatch(1);
        gates.add(pageGate);
        server.routeFixed("/css", TestHttpServer.Response.html("<html><head><link rel=stylesheet href='slow.css'></head><body><p>x</p></body></html>"));
        server.route("/slowpage", new TestHttpServer.Handler() {
            /**
             * Answers when the page gate is opened.
             * <p>
             * 페이지 문이 열리면 응답한다.
             *
             * @param request the request / 요청
             * @return the page / 페이지
             * @throws Exception if interrupted / 인터럽트되면
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                pageGate.await(30, TimeUnit.SECONDS);
                return TestHttpServer.Response.html("<p>slow page</p>");
            }
        });
        server.start();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(server.url("/css"));
        awaitStatus(p, "Loading resources (0/1)");
        p.setPage(server.url("/slowpage"));
        Check.isTrue(p.getStatusText().startsWith("Loading " + server.url("/slowpage")), "the new navigation owns the status: " + p.getStatusText());
        cssGate.countDown();
        long end = System.currentTimeMillis() + 800;
        // 이전 페이지의 스타일시트가 도착해도 진행 중인 이동의 상태와 진행률이 그대로여야 한다.
        while (System.currentTimeMillis() < end) {
            Check.isTrue(p.getStatusText().startsWith("Loading " + server.url("/slowpage")), "the status stays with the navigation: " + p.getStatusText());
            Check.isTrue(p.getProgress() <= 50, "the progress is the one of the navigation in flight: " + p.getProgress());
            Thread.sleep(20);
        }
        pageGate.countDown();
        awaitDone(p);
        Check.equal(server.url("/slowpage"), p.getPage(), "the new page was confirmed");
    }

    /**
     * While the body of the main document is received, the progress is between 1 and 50 when the length is declared and -1 (indeterminate)
     * when it is not; after the body ended the page finishes with 100.
     * <p>
     * 주 문서의 본문을 받는 동안 진행률은 길이를 선언했으면 1~50, 선언하지 않았으면 -1(불확정)이고, 본문이 끝나면 페이지는 100 으로 끝난다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBodyProgressIsKnownOrIndeterminate() throws Exception {
        StringBuilder text = new StringBuilder("<!doctype html><html><body>");
        // 절반만 받은 상태가 분명하게 보이도록 본문을 어느 정도 크게 만든다.
        for (int i = 0; i < 400; i++) {
            text.append("<p>line ").append(i).append("</p>");
        }
        text.append("</body></html>");
        byte[] body = text.toString().getBytes("UTF-8");
        boolean[] modes = { true, false };
        // 길이를 선언하는 응답과 선언하지 않는 응답을 차례로 확인한다.
        for (boolean withLength : modes) {
            CountDownLatch gate = new CountDownLatch(1);
            gates.add(gate);
            HalfBodyServer raw = new HalfBodyServer(withLength, gate, body);
            rawServers.add(raw);
            SwingWebPanel p = new SwingWebPanel();
            panels.add(p);
            p.setPage(raw.url());
            long end = System.currentTimeMillis() + 30000;
            // 본문의 절반이 도착해 진행률이 0 에서 바뀔 때까지 기다린다.
            while (p.getProgress() == 0 && System.currentTimeMillis() < end) {
                Thread.sleep(5);
            }
            Thread.sleep(100);
            int progress = p.getProgress();
            if (withLength) {
                Check.isTrue(progress >= 1 && progress <= 50, "a declared length gives a progress between 1 and 50: " + progress);
            } else {
                Check.equal(-1, progress, "an unknown length is indeterminate");
            }
            Check.isTrue(p.getStatusText().startsWith("Loading " + raw.url()), "the status names the address: " + p.getStatusText());
            Check.equal("about:blank", p.getPage(), "the old page stays until the new one is complete");
            gate.countDown();
            awaitDone(p);
            Check.equal(raw.url(), p.getPage(), "the page was confirmed " + (withLength ? "with" : "without") + " a declared length");
        }
    }

    /**
     * A page without resources ends at once, an error page ends with its error status, and a page that is replaced while it loads leaves the
     * progress to the new page.
     * <p>
     * 리소스가 없는 페이지는 곧바로 끝나고, 오류 페이지는 오류 상태로 끝나며, 로딩 중에 교체된 페이지는 진행률을 새 페이지에 맡긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testPlainErrorAndReplacedPages() throws Exception {
        TestHttpServer server = new TestHttpServer();
        CountDownLatch gate = serve(server);
        server.routeFixed("/css", TestHttpServer.Response.html("<html><head><link rel=stylesheet href='slow.css'></head><body><p>x</p></body></html>"));
        server.start();
        SwingWebPanel p = new SwingWebPanel();
        panels.add(p);
        p.setPage(server.url("/fast"));
        awaitDone(p);
        Check.equal(server.url("/fast"), p.getPage(), "a page without resources");
        p.setPage(server.url("/nothing-here"));
        awaitStatus(p, "Error 404");
        Check.equal(100, p.getProgress(), "an error page ends with 100");
        // 느린 스타일시트를 기다리는 페이지를 빠른 페이지로 교체하면 새 페이지의 진행률만 남는다.
        p.setPage(server.url("/css"));
        awaitStatus(p, "Loading resources (0/1)");
        p.setPage(server.url("/fast"));
        awaitDone(p);
        gate.countDown();
        long end = System.currentTimeMillis() + 800;
        // 이전 페이지의 스타일시트가 도착해도 상태와 진행률은 그대로여야 한다.
        while (System.currentTimeMillis() < end) {
            Check.equal("Done", p.getStatusText(), "the replaced page does not change the status");
            Check.equal(100, p.getProgress(), "nor the progress");
            Thread.sleep(20);
        }
    }
}
