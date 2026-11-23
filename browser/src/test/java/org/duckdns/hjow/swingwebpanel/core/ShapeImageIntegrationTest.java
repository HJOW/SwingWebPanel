package org.duckdns.hjow.swingwebpanel.core;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Panel;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * The component-level checks of the image shapes of <code>shape-outside</code> with real HTTP (plan item P10-08-d, CSS Shapes Level 1 section 4): while the
 * image is on its way the float is wrapped around by its margin box, and once the decoded image arrived the layout runs again and the text follows the
 * alpha channel of the image; a cross-origin image without permission is never used (the shape must be CORS-same-origin) and with
 * <code>Access-Control-Allow-Origin</code> it is. The page measures where the text begins with a script and reports it through the sessionStorage.
 * <p>
 * 실제 HTTP 로 <code>shape-outside</code> 의 이미지 모양을 컴포넌트 수준에서 확인한다(계획 P10-08-d, CSS Shapes Level 1 4 절): 이미지가 오는 동안 float 는 margin box 로 감싸고,
 * 디코딩한 이미지가 도착하면 레이아웃을 다시 하여 텍스트가 이미지의 알파 채널을 따른다. 허락 없는 다른 오리진의 이미지는 쓰지 않고(모양은 CORS 같은 오리진이어야 함)
 * <code>Access-Control-Allow-Origin</code> 이 있으면 쓴다. 페이지는 스크립트로 텍스트가 시작하는 곳을 재어 sessionStorage 로 알린다.
 */
public class ShapeImageIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ShapeImageIntegrationTest() {
    }

    /**
     * Makes the bytes of a 100 by 100 PNG that is opaque red in its left half and transparent in its right half.
     * <p>
     * 왼쪽 절반은 불투명한 빨강이고 오른쪽 절반은 투명한 100x100 PNG 의 바이트를 만든다.
     *
     * @return the file bytes / 파일 바이트
     * @throws Exception if encoding fails / 인코딩이 실패하면
     */
    private static byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        // 왼쪽 절반만 불투명한 빨강으로 채운다(나머지는 투명하다).
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 50; x++) {
                image.setRGB(x, y, 0xFFFF0000);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Makes a page with a 100 by 100 float shaped by an image and a paragraph whose first word is measured.
     * <p>
     * 이미지로 모양을 낸 100x100 float 와, 첫 단어를 재는 문단이 있는 페이지를 만든다.
     *
     * @param imageUrl the address of the shape image / 모양 이미지의 주소
     * @return the page / 페이지
     */
    private static String page(String imageUrl) {
        StringBuilder words = new StringBuilder();
        // 첫 줄이 float 옆에서 시작하도록 짧은 단어를 많이 만든다.
        for (int i = 0; i < 200; i++) {
            words.append("ab ");
        }
        return "<!doctype html><html><head><style>body{margin:0;font:10px monospace;line-height:10px}"
                + "#f{float:left;width:100px;height:100px;shape-outside:url(" + imageUrl + ")}</style></head><body><div id=f></div>"
                + "<p style='margin:0'><span id=s>ab</span> " + words + "</p>"
                + "<script>setInterval(function(){sessionStorage.setItem('left',String(Math.round(document.getElementById('s').getBoundingClientRect().left)))},20)</script>"
                + "</body></html>";
    }

    /**
     * Waits until the page reports where the first word is.
     * <p>
     * 페이지가 첫 단어의 위치를 알릴 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param expected the expected left in pixels / 기대하는 왼쪽(픽셀)
     * @param what     the description / 설명
     * @throws Exception on failure / 실패 시
     */
    private static void awaitLeft(Panel panel, String expected, String what) throws Exception {
        long deadline = System.currentTimeMillis() + 20000;
        // 스크립트가 위치를 알릴 때까지 반복해서 확인한다.
        while (!expected.equals(TextSelectionFixture.stored(panel, "left")) && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        Check.equal(expected, TextSelectionFixture.stored(panel, "left"), what);
    }

    /**
     * The margin box shapes the text while the image is loading, the alpha of the image after it arrived.
     * <p>
     * 이미지를 가져오는 동안은 margin box 가, 도착한 뒤에는 이미지의 알파가 텍스트의 모양을 정한다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testShapeFollowsTheImageOnceItArrived() throws Exception {
        final TestHttpServer server = new TestHttpServer();
        final CountDownLatch release = new CountDownLatch(1);
        final byte[] data = png();
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            server.route("/shape.png", new TestHttpServer.Handler() {
                /**
                 * Answers when the test opens the gate.
                 * <p>
                 * 시험이 문을 열 때 응답한다.
                 *
                 * @param request the request / 요청
                 * @return the image / 이미지
                 * @throws Exception if interrupted / 인터럽트되면
                 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    release.await(30, TimeUnit.SECONDS);
                    return new TestHttpServer.Response(200).bytes("image/png", data);
                }
            });
            server.routeFixed("/page", TestHttpServer.Response.html(page("shape.png")));
            server.start();
            panel.setPage(server.url("/page"));
            awaitLeft(panel, "100", "while the image is loading the text starts at the margin box");
            release.countDown();
            awaitLeft(panel, "50", "once the image arrived the text starts at the edge of its opaque half");
            Check.equal(1, server.count("/shape.png"), "one request for the image");
            Check.equal(page("shape.png"), panel.getContent(), "the received source is unchanged");
        } finally {
            release.countDown();
            TextSelectionFixture.close(panel);
            server.stop();
        }
    }

    /**
     * A cross-origin image is used only if the server allows the origin of the page (CORS-same-origin).
     * <p>
     * 다른 오리진의 이미지는 서버가 페이지의 오리진을 허락할 때만 쓴다(CORS 같은 오리진).
     *
     * @throws Exception on failure / 실패 시
     */
    public void testCrossOriginImageNeedsPermission() throws Exception {
        final TestHttpServer pageServer = new TestHttpServer();
        final TestHttpServer denied = new TestHttpServer();
        final TestHttpServer allowed = new TestHttpServer();
        final byte[] data = png();
        Panel panel = TextSelectionFixture.create(400, 300);
        try {
            denied.routeFixed("/s.png", new TestHttpServer.Response(200).bytes("image/png", data));
            allowed.routeFixed("/s.png", new TestHttpServer.Response(200).bytes("image/png", data).header("Access-Control-Allow-Origin", "*"));
            denied.start();
            allowed.start();
            pageServer.routeFixed("/denied", TestHttpServer.Response.html(page(denied.url("/s.png"))));
            pageServer.routeFixed("/allowed", TestHttpServer.Response.html(page(allowed.url("/s.png"))));
            pageServer.start();
            panel.setPage(pageServer.url("/denied"));
            awaitLeft(panel, "100", "the page runs");
            long deadline = System.currentTimeMillis() + 20000;
            // 이미지 요청이 서버에 도착해 응답을 처리할 시간을 준다.
            while (denied.count("/s.png") == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            Check.isTrue(denied.count("/s.png") > 0, "the image was requested");
            Thread.sleep(500);
            Check.equal("100", TextSelectionFixture.stored(panel, "left"), "without permission the shape stays the margin box");
            panel.setPage(pageServer.url("/allowed"));
            awaitLeft(panel, "50", "with Access-Control-Allow-Origin the image shapes the text");
        } finally {
            TextSelectionFixture.close(panel);
            pageServer.stop();
            denied.stop();
            allowed.stop();
        }
    }
}
