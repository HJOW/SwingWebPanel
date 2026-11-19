package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Pages and servers that are too big, broken, circular, endless or slow, run through the real component (plan item P22-09): every limit
 * that is exceeded must end in an explicit error page, a cancelled page or a stopped script, never in a hang, a crash of a thread or a
 * frozen interface. While each case runs, the test keeps asking the event dispatch thread for a round trip and fails if the interface does
 * not answer within a few seconds.
 * <p>
 * 너무 크거나 깨졌거나 순환하거나 끝없거나 느린 페이지와 서버를 실제 컴포넌트로 실행한다(계획 P22-09): 초과한 한도는 모두 명시적인 오류 화면, 취소된
 * 페이지, 멈춘 스크립트로 끝나야 하며 멈춤, 스레드 충돌, 얼어붙은 화면이 있어서는 안 된다. 사례를 실행하는 동안 시험은 이벤트 디스패치 스레드에 계속
 * 왕복을 요청하고 몇 초 안에 답하지 않으면 실패한다.
 */
public class RobustnessIntegrationTest {

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
    public RobustnessIntegrationTest() {
    }

    /**
     * Creates the server.
     * <p>
     * 서버를 만든다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void setUp() throws Exception {
        server = new TestHttpServer();
    }

    /**
     * Disposes the components and stops the server.
     * <p>
     * 컴포넌트를 폐기하고 서버를 멈춘다.
     */
    public void tearDown() {
        // 사례가 만든 컴포넌트를 모두 폐기한다.
        for (SwingWebPanel p : panels) {
            p.dispose();
        }
        server.stop();
    }

    /**
     * Creates a component of 400 by 300 pixels.
     * <p>
     * 400x300 화소의 컴포넌트를 만든다.
     *
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private SwingWebPanel panel() throws Exception {
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
        panels.add(holder[0]);
        return holder[0];
    }

    /**
     * Asks the EDT for a round trip and fails if it does not answer in time.
     * <p>
     * EDT 에 왕복을 요청하고 제때 답하지 않으면 실패한다.
     *
     * @param millis the longest wait / 최대 대기 시간
     * @param what   what was running / 실행 중이던 것
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void edtAnswers(long millis, String what) throws Exception {
        final boolean[] answered = new boolean[1];
        SwingUtilities.invokeLater(new Runnable() {
            /**
             * Marks that the EDT ran.
             * <p>
             * EDT 가 실행되었음을 표시한다.
             */
            public void run() {
                answered[0] = true;
            }
        });
        long end = System.currentTimeMillis() + millis;
        // EDT 가 답할 때까지 짧게 쉬며 기다린다.
        while (!answered[0] && System.currentTimeMillis() < end) {
            Thread.sleep(5);
        }
        Check.isTrue(answered[0], "the interface answers while " + what);
    }

    /**
     * Waits until the progress is 100 and the page is not the blank page, keeping the interface under watch.
     * <p>
     * 진행률이 100 이고 페이지가 빈 페이지가 아닐 때까지 인터페이스를 지켜보며 기다린다.
     *
     * @param p    the component / 컴포넌트
     * @param what what runs / 실행 중인 것
     * @throws Exception if interrupted / 인터럽트되면
     */
    private static void awaitDone(SwingWebPanel p, String what) throws Exception {
        long end = System.currentTimeMillis() + 90000;
        Thread.sleep(100);
        // 로딩이 끝날 때까지 기다리며 매번 EDT 가 답하는지 확인한다.
        while ((p.getProgress() != 100 || "about:blank".equals(p.getPage())) && System.currentTimeMillis() < end) {
            edtAnswers(5000, what);
            Thread.sleep(50);
        }
        Check.equal(100, p.getProgress(), "finished: " + what);
    }

    /**
     * Paints the component on the EDT (the layout of a huge page must not freeze the interface either).
     * <p>
     * 컴포넌트를 EDT 에서 그린다(거대한 페이지의 배치도 화면을 얼리면 안 된다).
     *
     * @param p the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    private static void paint(final SwingWebPanel p) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /**
             * Paints into an image.
             * <p>
             * 이미지에 그린다.
             */
            public void run() {
                BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                try {
                    p.paint(g);
                } finally {
                    g.dispose();
                }
            }
        });
    }

    /**
     * A page with a very deep nesting of elements (twenty thousand levels) is parsed, laid out and painted without a stack overflow, and
     * the source is kept.
     * <p>
     * 요소가 아주 깊게 중첩된 페이지(2만 단계)를 스택 오버플로 없이 파싱, 배치, 그리고 원문이 유지된다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testVeryDeepNesting() throws Exception {
        StringBuilder sb = new StringBuilder("<!doctype html><body>");
        // 요소를 2만 단계 깊이로 중첩한다.
        for (int i = 0; i < 20000; i++) {
            sb.append("<div>");
        }
        sb.append("deep text");
        String html = sb.toString();
        server.routeFixed("/deep", TestHttpServer.Response.html(html));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/deep"));
        awaitDone(p, "a very deep page loads");
        Check.equal(server.url("/deep"), p.getPage(), "the page was confirmed");
        Check.equal(html, p.getContent(), "the source is kept");
        paint(p);
        edtAnswers(5000, "a very deep page is painted");
    }

    /**
     * A page with a huge number of elements and style rules (fifty thousand paragraphs, ten thousand rules) loads and paints while the
     * interface keeps answering.
     * <p>
     * 요소와 스타일 규칙이 아주 많은 페이지(문단 5만 개, 규칙 1만 개)가 인터페이스가 계속 답하는 동안 로드되고 그려진다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testHugeNumberOfElementsAndRules() throws Exception {
        StringBuilder css = new StringBuilder();
        // 클래스 규칙을 1만 개 만든다.
        for (int i = 0; i < 10000; i++) {
            css.append(".c").append(i).append("{color:#").append(Integer.toHexString(0x100000 + i)).append("}\n");
        }
        StringBuilder html = new StringBuilder("<!doctype html><style>" + css + "</style><body>");
        // 문단을 5만 개 만든다.
        for (int i = 0; i < 50000; i++) {
            html.append("<p class=c").append(i % 10000).append(">line ").append(i).append("</p>");
        }
        server.routeFixed("/huge", TestHttpServer.Response.html(html.toString()));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/huge"));
        awaitDone(p, "a huge page loads");
        Check.equal(server.url("/huge"), p.getPage(), "the page was confirmed");
        paint(p);
        edtAnswers(5000, "a huge page is painted");
        long height = p.getPreferredSize().height;
        Check.isTrue(height > 100000, "the page is laid out in full: " + height);
    }

    /**
     * Circular redirects end in an error page with the loop named, an endless chain of redirects ends at the limit, and neither hangs.
     * <p>
     * 순환 리디렉션은 순환을 밝히는 오류 화면으로, 끝없는 리디렉션 연쇄는 한도에서 끝나며 어느 쪽도 멈추지 않는다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testRedirectLoopsAndEndlessChains() throws Exception {
        server.routeFixed("/loop-a", TestHttpServer.Response.redirect(302, "/loop-b"));
        server.routeFixed("/loop-b", TestHttpServer.Response.redirect(302, "/loop-a"));
        server.route("/chain/*", new TestHttpServer.Handler() {
            /**
             * Redirects to the next number of the chain.
             * <p>
             * 연쇄의 다음 번호로 리디렉션한다.
             *
             * @param request the request / 요청
             * @return the response / 응답
             */
            public TestHttpServer.Response handle(TestHttpServer.Request request) {
                int n = Integer.parseInt(request.path.substring("/chain/".length()));
                return TestHttpServer.Response.redirect(302, "/chain/" + (n + 1));
            }
        });
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/loop-a"));
        awaitDone(p, "a redirect loop is detected");
        Check.contains(p.getContent().toLowerCase(), "loop", "the error page names the loop");
        Check.isTrue(server.count("/loop-a") + server.count("/loop-b") <= 4, "the loop was cut at once: " + server.totalRequests());
        p.setPage(server.url("/chain/0"));
        long end = System.currentTimeMillis() + 60000;
        // 연쇄가 한도에서 끝날 때까지 기다린다.
        while ((p.getProgress() != 100 || p.getPage().indexOf("/chain/") < 0 && !p.getContent().toLowerCase().contains("redirect"))
                && System.currentTimeMillis() < end) {
            edtAnswers(5000, "an endless redirect chain runs");
            Thread.sleep(50);
        }
        Check.equal(100, p.getProgress(), "the chain ended");
        Check.isTrue(server.totalRequests() < 100, "the chain was cut at the limit: " + server.totalRequests());
        Check.contains(p.getContent().toLowerCase(), "redirect", "the error page names the redirects");
    }

    /**
     * A script that never ends is stopped, the interface keeps answering and the next navigation works at once; a script that fills memory
     * is stopped by the limits.
     * <p>
     * 끝나지 않는 스크립트는 멈추고, 인터페이스는 계속 답하며, 다음 이동이 바로 동작한다. 메모리를 채우는 스크립트는 한도가 멈춘다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEndlessAndGreedyScripts() throws Exception {
        server.routeFixed("/endless", TestHttpServer.Response.html("<p>endless</p><script>sessionStorage.setItem('before', '1'); while (true) { }</"
                + "script>"));
        server.routeFixed("/greedy", TestHttpServer.Response.html("<p>greedy</p><script>var a = []; while (true) { a.push(new Array(100000).join('x')); }</"
                + "script>"));
        server.routeFixed("/ok", TestHttpServer.Response.html("<p>fine</p>"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/endless"));
        awaitDone(p, "an endless script runs");
        edtAnswers(5000, "an endless script was stopped");
        p.setPage(server.url("/greedy"));
        awaitDone(p, "a greedy script runs");
        edtAnswers(5000, "a greedy script was stopped");
        p.setPage(server.url("/ok"));
        awaitDone(p, "the page after the scripts loads");
        Check.equal(server.url("/ok"), p.getPage(), "navigation works after the scripts were stopped");
        Check.contains(p.getContent(), "fine", "the next page is shown");
    }

    /**
     * A response that is bigger than the memory limit becomes an error page, not an out-of-memory error, and a very slow response does
     * not block the next navigation, which wins.
     * <p>
     * 메모리 한도보다 큰 응답은 메모리 부족 오류가 아니라 오류 화면이 되고, 아주 느린 응답은 다음 이동을 막지 않으며 다음 이동이 이긴다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testTooLargeAndTooSlowResponses() throws Exception {
        byte[] big = new byte[20 * 1024 * 1024];
        // 큰 응답 본문을 반복되는 글자로 채운다.
        for (int i = 0; i < big.length; i++) {
            big[i] = (byte) ('a' + i % 26);
        }
        server.routeFixed("/big", new TestHttpServer.Response(200).bytes("text/html", big));
        server.routeFixed("/slow", TestHttpServer.Response.html("slow page").delay(20000));
        server.routeFixed("/fast", TestHttpServer.Response.html("fast page"));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/big"));
        awaitDone(p, "a response beyond the limit is received");
        Check.isTrue(p.getContent().length() < big.length, "the oversized body was not accepted");
        p.setPage(server.url("/slow"));
        Thread.sleep(200);
        edtAnswers(5000, "a slow response is awaited");
        p.setPage(server.url("/fast"));
        awaitDone(p, "the fast page loads");
        Check.equal(server.url("/fast"), p.getPage(), "the newer navigation won");
        Check.equal("fast page", p.getContent(), "the fast page is shown");
        Thread.sleep(300);
        Check.equal(server.url("/fast"), p.getPage(), "the slow page never replaced it");
    }
    /**
     * Writes one PNG chunk (length, type, data, CRC).
     * <p>
     * PNG 덩어리 하나(길이, 종류, 자료, CRC)를 쓴다.
     *
     * @param out  the output / 출력
     * @param type the four-letter type / 네 글자 종류
     * @param data the data / 자료
     * @throws IOException never for a byte array / 바이트 배열이면 발생하지 않는다
     */
    private static void chunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] t = type.getBytes("US-ASCII");
        out.writeInt(data.length);
        out.write(t);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(t);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }

    /**
     * Builds a PNG whose header promises an image of the given size but whose data is a few bytes long.
     * <p>
     * 머리가 주어진 크기의 이미지를 약속하지만 자료는 몇 바이트뿐인 PNG 를 만든다.
     *
     * @param width  the promised width / 약속한 너비
     * @param height the promised height / 약속한 높이
     * @return the bytes / 바이트
     * @throws IOException never for a byte array / 바이트 배열이면 발생하지 않는다
     */
    private static byte[] hugePng(int width, int height) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write(new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A });
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        DataOutputStream h = new DataOutputStream(header);
        h.writeInt(width);
        h.writeInt(height);
        h.write(new byte[] { 8, 2, 0, 0, 0 });
        chunk(out, "IHDR", header.toByteArray());
        chunk(out, "IDAT", new byte[] { 0x78, (byte) 0x9c, 0x63, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01 });
        chunk(out, "IEND", new byte[0]);
        return bytes.toByteArray();
    }

    /**
     * Returns the value of a session storage key, or null.
     * <p>
     * 세션 저장소 키의 값을 반환한다. 없으면 null.
     *
     * @param p   the component / 컴포넌트
     * @param key the key / 키
     * @return the value or null / 값 또는 null
     */
    private static String session(SwingWebPanel p, String key) {
        Collection<StorageEntry> entries = p.getSessionStorage();
        // 항목 중 키가 같은 것의 값을 돌려준다.
        for (StorageEntry e : entries) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Broken, empty, mislabelled and oversized media in a page end in <code>error</code> events and the places of the failed media, not
     * in a hang, an out-of-memory error or a frozen interface, and a valid image beside them still loads (P22-09, media).
     * <p>
     * 깨졌거나 비었거나 종류가 틀렸거나 너무 큰 미디어가 든 페이지는 멈춤, 메모리 부족, 얼어붙은 화면이 아니라 <code>error</code> 이벤트와 실패한 미디어의
     * 자리로 끝나고, 그 옆의 올바른 이미지는 그대로 로딩된다(P22-09, 미디어).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testBrokenAndOversizedMedia() throws Exception {
        BufferedImage good = new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(good, "png", png);
        byte[] whole = png.toByteArray();
        byte[] truncated = new byte[whole.length / 2];
        System.arraycopy(whole, 0, truncated, 0, truncated.length);
        byte[] noise = new byte[5000];
        new Random(7).nextBytes(noise);
        server.routeFixed("/good.png", new TestHttpServer.Response(200).bytes("image/png", whole));
        server.routeFixed("/truncated.png", new TestHttpServer.Response(200).bytes("image/png", truncated));
        server.routeFixed("/huge.png", new TestHttpServer.Response(200).bytes("image/png", hugePng(100000, 100000)));
        server.routeFixed("/noise.png", new TestHttpServer.Response(200).bytes("image/png", noise));
        server.routeFixed("/empty.png", new TestHttpServer.Response(200).bytes("image/png", new byte[0]));
        server.routeFixed("/text.png", TestHttpServer.Response.html("<b>not an image</b>"));
        server.routeFixed("/noise.wav", new TestHttpServer.Response(200).bytes("audio/wav", noise));
        String count = "sessionStorage.setItem('e', String((parseInt(sessionStorage.getItem('e'), 10) || 0) + 1))";
        StringBuilder page = new StringBuilder("<!doctype html><body style=\"margin:0\">");
        String[] bad = { "truncated", "huge", "noise", "empty", "text" };
        // 깨진 이미지마다 error 이벤트가 오면 개수를 센다.
        for (int i = 0; i < bad.length; i++) {
            page.append("<img src=\"/").append(bad[i]).append(".png\" alt=\"").append(bad[i]).append("\" onerror=\"").append(count)
                    .append("\" style=\"width:60px;height:40px\">");
        }
        page.append("<audio src=\"/noise.wav\" onerror=\"").append(count).append("\"></audio>");
        page.append("<img src=\"/good.png\" onload=\"sessionStorage.setItem('good','1')\" onerror=\"sessionStorage.setItem('goodError','1')\">");
        server.routeFixed("/page", TestHttpServer.Response.html(page.toString()));
        server.start();
        SwingWebPanel p = panel();
        p.setPage(server.url("/page"));
        awaitDone(p, "a page with broken media is loaded");
        long end = System.currentTimeMillis() + 60000;
        // 모든 실패 이벤트(이미지 다섯 개와 오디오 하나)와 올바른 이미지의 load 를 짧게 쉬며 기다린다.
        while ((!"6".equals(session(p, "e")) || session(p, "good") == null) && System.currentTimeMillis() < end) {
            edtAnswers(5000, "broken media fails");
            Thread.sleep(50);
        }
        Check.equal("6", session(p, "e"), "five images and one audio failed with an error event");
        Check.equal("1", session(p, "good"), "the valid image beside them loaded");
        Check.isNull(session(p, "goodError"), "the valid image did not fail");
        paint(p);
        edtAnswers(5000, "the places of the failed media are painted");
        Check.equal(100, p.getProgress(), "the page finished");
    }
}
