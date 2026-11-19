package org.duckdns.hjow.swingwebpanel;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.duckdns.hjow.swingwebpanel.core.TestSupport;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Offscreen Swing integration tests of the actual component paint path and JScrollPane dimensions (P09-07-a).
 * Pixel colors and content sizes are checked without assuming any OS-specific glyph rasterization.
 * <p>Swing 컴포넌트의 실제 그리기 경로와 JScrollPane 크기를 확인하는 오프스크린 통합 시험. OS별 글리프 픽셀 모양에 의존하지 않는다.
 */
public class SwingRenderingTest {
    /** Creates the suite. / 시험 스위트를 만든다. */
    public SwingRenderingTest() {
    }

    /**
     * Legacy font hints reach actual Swing painting with scripting disabled and preserve the original content.
     * <p>JavaScript를 끈 실제 Swing 그리기에서 과거 font 힌트와 원문 보존을 확인한다.
     * @throws Exception on asynchronous loading or EDT failure / 비동기 로딩 또는 EDT 실패 시
     */
    public void testLegacyFontHintsWithoutScript() throws Exception {
        Panel panel = createPanel();
        String source = "<!doctype html><style>html,body{margin:0;padding:0}"
                + "font{display:block;width:2em;height:1em;background:currentColor}</style>"
                + "<font size='+00000000000000000002' face='Arial/*' color=f></font>";
        try {
            panel.setJavascriptEnabled(false);
            panel.setContent(source, "text/html");
            awaitPixel(panel, 0x0f0000);
            Check.equal(0xffffff, pixel(panel, 49, 20), "outside the 48px font box");
            Check.equal(0xffffff, pixel(panel, 20, 25), "outside the 24px font box");
            Check.equal(source, panel.getContent(), "original attributes remain in received content");
        } finally {
            panel.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * Center text alignment and explicit nobr/wbr wrapping reach the actual Swing paint path without JavaScript.
     * <p>JavaScript 없이 center 텍스트 정렬과 nobr/wbr의 명시적 줄바꿈이 실제 Swing 그리기에 도달하는지 확인한다.
     * @throws Exception on asynchronous loading or EDT failure / 비동기 로딩 또는 EDT 실패 시
     */
    public void testLegacyUaAlignmentAndWrappingWithoutScript() throws Exception {
        Panel panel = createPanel();
        try {
            panel.setJavascriptEnabled(false);
            panel.setContent("<!doctype html><style>html,body{margin:0;padding:0}"
                    + "span{display:inline-block;width:280px;height:40px;background:#123456}</style>"
                    + "<center><span></span></center>", "text/html");
            awaitPixel(panel, 0x123456);
            Check.equal(0xffffff, pixel(panel, 5, 20), "center leaves a 10px empty left edge");
            Check.equal(0xffffff, pixel(panel, 295, 20), "center leaves a 10px empty right edge");
            String source = "<!doctype html><style>html,body{margin:0;padding:0}"
                    + "body{font:10px monospace;line-height:30px}div{width:30px}"
                    + "span{display:inline-block;width:25px;height:30px}</style>"
                    + "<div><nobr><span style='background:#445566'></span><wbr>"
                    + "<span style='background:#667788'></span></nobr></div>";
            panel.setContent(source, "text/html");
            awaitPixel(panel, 0x445566);
            Check.equal(0x667788, pixel(panel, 20, 55), "wbr wraps the second atomic box");
            Check.equal(source, panel.getContent(), "original legacy markup preserved");
        } finally {
            panel.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * Fixed-width blocks are aligned by <code>center</code> and <code>div align</code> in the actual Swing paint path (P07-07-b-5):
     * pixels of the block, its empty sides, a nested nearest alignment and the preserved original markup, without JavaScript.
     * <p>고정 너비 블록이 실제 Swing 그리기에서 <code>center</code> 와 <code>div align</code> 으로 정렬되는지 확인한다(P07-07-b-5): 블록과 빈 양옆의 픽셀,
     * 가장 가까운 정렬 조상, 보존된 원문. JavaScript 는 쓰지 않는다.
     * @throws Exception on asynchronous loading or EDT failure / 비동기 로딩 또는 EDT 실패 시
     */
    public void testAlignDescendantsPaintsAlignedBlocks() throws Exception {
        Panel panel = createPanel();
        try {
            panel.setJavascriptEnabled(false);
            String block = "<div style='width:100px;height:40px;background:#a1b2c3'></div>";
            String head = "<!doctype html><style>html,body{margin:0;padding:0}</style>";
            String centered = head + "<center>" + block + "</center>";
            panel.setContent(centered, "text/html");
            awaitPixelAt(panel, 150, 20, 0xa1b2c3);
            Check.equal(0xffffff, pixel(panel, 90, 20), "empty left side of the centered block");
            Check.equal(0xffffff, pixel(panel, 210, 20), "empty right side of the centered block");
            Check.equal(centered, panel.getContent(), "original markup preserved");
            String right = head + "<div align=right><center><div align=left>" + block + "</div></center></div>";
            panel.setContent(right, "text/html");
            awaitPixelAt(panel, 50, 20, 0xa1b2c3);
            Check.equal(0xffffff, pixel(panel, 150, 20), "the nearest aligning ancestor (div align=left) keeps the block at the start");
            String rightOnly = head + "<div align=right>" + block + "</div>";
            panel.setContent(rightOnly, "text/html");
            awaitPixelAt(panel, 250, 20, 0xa1b2c3);
            Check.equal(0xffffff, pixel(panel, 150, 20), "right alignment leaves the middle empty");
            Check.equal(rightOnly, panel.getContent(), "original markup preserved after replacement");
        } finally {
            panel.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * A real component paints the legacy table attributes without scripting (P07-07-b-7): <code>bgcolor</code>, <code>width</code> and
     * <code>height</code> of the table and a cell, the colored <code>border</code> and the black line of <code>rules=all</code>.
     * <p>
     * 실제 컴포넌트가 스크립트 없이 이전 방식의 표 속성을 그린다(P07-07-b-7): 표와 셀의 <code>bgcolor</code>, <code>width</code>, <code>height</code>,
     * 색 있는 <code>border</code>, <code>rules=all</code> 의 검은 선.
     * @throws Exception on asynchronous loading or EDT failure / 비동기 로딩 또는 EDT 실패 시
     */
    public void testLegacyTableAttributesPaint() throws Exception {
        Panel panel = createPanel();
        try {
            panel.setJavascriptEnabled(false);
            String head = "<!doctype html><style>html,body{margin:0;padding:0}</style>";
            String plain = head + "<table cellspacing=0 cellpadding=0 bgcolor=#112233 width=100 height=40><tr><td bgcolor=#a1b2c3 width=30></td><td></td></tr></table>";
            panel.setContent(plain, "text/html");
            awaitPixelAt(panel, 15, 20, 0xa1b2c3);
            Check.equal(0x112233, pixel(panel, 60, 20), "the table background shows through the second cell");
            Check.equal(0xffffff, pixel(panel, 120, 20), "nothing to the right of the 100px wide table");
            Check.equal(plain, panel.getContent(), "original markup preserved");
            String bordered = head + "<table border=4 cellspacing=0 cellpadding=0 bordercolor=#ff0000 width=100 height=40><tr><td></td></tr></table>"
                    + "<table rules=all cellspacing=0 cellpadding=0 style='margin-top:20px'><tr><td width=30 height=20></td><td width=30 height=20></td></tr></table>";
            panel.setContent(bordered, "text/html");
            long deadline = System.currentTimeMillis() + 15000;
            // 새 문서가 그려져 왼쪽 가장자리에 border 가 나타날 때까지 기다린다.
            int edge = pixel(panel, 1, 20);
            while ((edge == 0xffffff || edge == 0xa1b2c3) && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
                edge = pixel(panel, 1, 20);
            }
            Check.isTrue(((edge >> 16) & 0xff) > 0x80 && ((edge >> 8) & 0xff) < 0x40, "the border is drawn in the bordercolor: " + Integer.toHexString(edge));
            Check.equal(0xffffff, pixel(panel, 50, 20), "the cell inside the border is empty");
            boolean line = false;
            // rules=all: 두 셀 사이(x=30 근처)의 세로선이 검정으로 그려진다.
            for (int x = 27; x <= 33; x++) {
                if (pixel(panel, x, 70) == 0x000000) {
                    line = true;
                }
            }
            Check.isTrue(line, "the inner line of rules=all is black");
            Check.equal(0xffffff, pixel(panel, 0, 60), "no outer line on the left edge of the rules table");
            Check.equal(bordered, panel.getContent(), "original markup preserved after replacement");
        } finally {
            panel.dispose();
            TestSupport.flushEdt();
        }
    }

    /**
     * Waits until a pixel has the expected color.
     * <p>픽셀이 기대한 색이 될 때까지 기다린다.
     * @param panel component / 컴포넌트
     * @param x sample x / 샘플 x
     * @param y sample y / 샘플 y
     * @param color expected RGB / 기대 RGB
     * @throws Exception on EDT failure or interruption / EDT 실패 또는 인터럽트 시
     */
    private static void awaitPixelAt(Panel panel, int x, int y, int color) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        int actual;
        // 문서 작업과 EDT 게시가 끝날 때까지 실제 그림을 반복해서 확인한다.
        do {
            actual = pixel(panel, x, y);
            if (actual == color) {
                return;
            }
            Thread.sleep(20);
        } while (System.currentTimeMillis() < deadline);
        Check.equal(color, actual, "rendered RGB at (" + x + "," + y + ")");
    }

    /** Exposes the existing protected scroll metrics to the tests. / 기존 protected 스크롤 정보를 시험에 노출한다. */
    private static final class Panel extends SwingWebPanel {
        /** Serialization identifier. / 직렬화 식별자. */
        private static final long serialVersionUID = 1L;
        /** Creates the standard component. / 표준 컴포넌트를 만든다. */
        Panel() {
        }
        /** Returns the actual document height. / 실제 문서 높이를 반환한다.
         * @return document height / 문서 높이 */
        int contentHeight() {
            return getViewMetrics().getHeight();
        }
    }

    /**
     * Paints the component on the EDT and reads one interior pixel.
     * <p>EDT에서 컴포넌트를 그리고 내부 픽셀 하나를 읽는다.
     * @param panel component / 컴포넌트
     * @param x sample x / 샘플 x
     * @param y sample y / 샘플 y
     * @return RGB color / RGB 색
     * @throws Exception on EDT failure / EDT 실패 시
     */
    private static int pixel(final Panel panel, final int x, final int y) throws Exception {
        final int[] result = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints into an ARGB image without a native window. / 네이티브 창 없이 ARGB 이미지에 그린다. */
            public void run() {
                BufferedImage image = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    panel.paint(graphics);
                } finally {
                    graphics.dispose();
                }
                result[0] = image.getRGB(x, y) & 0xffffff;
            }
        });
        return result[0];
    }

    /**
     * Waits for asynchronous drawing to reach an expected pixel color.
     * <p>비동기 그리기가 기대 픽셀 색에 도달할 때까지 기다린다.
     * @param panel component / 컴포넌트
     * @param color expected RGB / 기대 RGB
     * @throws Exception on failure / 실패 시
     */
    private static void awaitPixel(Panel panel, int color) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        int actual;
        // 문서 작업과 EDT 게시가 모두 끝날 기회를 주며 실제 그림을 확인한다.
        do {
            actual = pixel(panel, 20, 20);
            if (actual == color) {
                return;
            }
            Thread.sleep(20);
        } while (System.currentTimeMillis() < deadline);
        Check.equal(color, actual, "rendered RGB at (20,20)");
    }

    /**
     * Waits for main-document confirmation, then lets pending EDT notifications run.
     * <p>주 문서 확정을 기다린 뒤 대기 중인 EDT 통지를 실행한다.
     * @param panel component / 컴포넌트
     * @throws Exception when interrupted / 인터럽트 시
     */
    private static void awaitConfirmation(Panel panel) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        // 하위 리소스 완료는 픽셀 기대값으로 별도 확인한다.
        while (panel.getProgress() != 100 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(100, panel.getProgress(), "main document confirmed");
        TestSupport.flushEdt();
    }

    /**
     * Encodes a small solid PNG using the standard test-side writer.
     * <p>표준 시험용 기록기로 작은 단색 PNG를 인코딩한다.
     * @param rgb image color / 이미지 색
     * @return PNG bytes / PNG 바이트
     * @throws Exception on encoding failure / 인코딩 실패 시
     */
    private static byte[] png(int rgb) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        // 모든 픽셀을 같은 색으로 만들어 배율에 의존하지 않는 기대값을 둔다.
        for (int y = 0; y < 2; y++) {
            // 각 행의 픽셀을 채운다.
            for (int x = 0; x < 2; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Check.isTrue(ImageIO.write(image, "png", bytes), "PNG test writer");
        return bytes.toByteArray();
    }

    /**
     * Reads a UTF-8 fixture also served over HTTP, making protocol comparisons use identical HTML/CSS.
     * <p>HTTP로도 제공할 UTF-8 자료를 읽어 프로토콜 비교에 같은 HTML·CSS를 사용한다.
     * @param name fixture file name / 자료 파일 이름
     * @return source text / 원문
     * @throws Exception on missing resource / 자원 누락 시
     */
    private static String fixture(String name) throws Exception {
        InputStream input = SwingRenderingTest.class.getResourceAsStream("/org/duckdns/hjow/swingwebpanel/testdata/static/" + name);
        Check.notNull(input, "static fixture " + name);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            // 자료 전체를 읽되 원문의 개행과 인코딩을 보존한다.
            while ((count = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
            return new String(bytes.toByteArray(), "UTF-8");
        } finally {
            input.close();
        }
    }

    /**
     * Creates a standard sized panel on the EDT.
     * <p>EDT에서 표준 크기 패널을 만든다.
     * @return real component / 실제 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시
     */
    private static Panel createPanel() throws Exception {
        final Panel[] panels = new Panel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Constructs the component on its UI thread. / UI 스레드에서 컴포넌트를 구성한다. */
            public void run() {
                panels[0] = new Panel();
                panels[0].setSize(300, 180);
            }
        });
        return panels[0];
    }

    /**
     * Clicks the fixture link through Component.dispatchEvent.
     * <p>Component.dispatchEvent로 자료의 링크를 클릭한다.
     * @param panel component / 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시
     */
    private static void click(final Panel panel) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Delivers primary-button input to the installed internal listener. / 설치된 내부 리스너에 주 버튼 입력을 전달한다. */
            public void run() {
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 20, 20, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Exercises a relative link, history and force refresh using actual paint completion and preserved received sources.
     * <p>실제 그리기 완료와 수신 원문 보존을 기준으로 상대 링크·이력·강제 새로고침을 확인한다.
     * @param panel real browser / 실제 브라우저
     * @param first first page address / 첫 페이지 주소
     * @param second second page address / 두 번째 페이지 주소
     * @throws Exception on failure / 실패 시
     */
    private static void roundTrip(Panel panel, String first, String second) throws Exception {
        panel.setPage(first);
        awaitPixel(panel, 0x445566);
        Check.equal("Classpath first", panel.getTitle(), "first title");
        Check.equal(fixture("index.html"), panel.getContent(), "first source");
        click(panel);
        awaitPixel(panel, 0x667788);
        Check.equal(second, panel.getPage(), "relative link target");
        Check.equal("Classpath second", panel.getTitle(), "second title");
        Check.equal(fixture("second.html"), panel.getContent(), "second source");
        panel.back();
        awaitPixel(panel, 0x445566);
        Check.equal(first, panel.getPage(), "back URL");
        Check.equal(fixture("index.html"), panel.getContent(), "back source");
        panel.forward();
        awaitPixel(panel, 0x667788);
        Check.equal(second, panel.getPage(), "forward URL");
        panel.refresh(true);
        awaitConfirmation(panel);
        awaitPixel(panel, 0x667788);
        Check.equal(fixture("second.html"), panel.getContent(), "refresh source");
        Check.equal(3, panel.getHistories().size(), "back/forward/refresh did not add entries");
    }

    /**
     * HTTP title/paragraph/list/table/fallback content follows links and restores exact sources through history.
     * <p>HTTP 제목·문단·목록·표·대체 내용이 링크를 따라가며 이력으로 원문을 정확히 복원한다.
     * @throws Exception on failure / 실패 시
     */
    public void testHttpStaticLinksAndHistory() throws Exception {
        TestHttpServer server = new TestHttpServer();
        Panel panel = createPanel();
        try {
            server.routeFixed("/static/index.html", TestHttpServer.Response.html(fixture("index.html")).header("Cache-Control", "max-age=60"));
            server.routeFixed("/static/second.html", TestHttpServer.Response.html(fixture("second.html")).header("Cache-Control", "max-age=60"));
            server.routeFixed("/static/style.css", TestHttpServer.Response.ok("text/css", fixture("style.css")).header("Cache-Control", "max-age=60"));
            server.start();
            roundTrip(panel, server.url("/static/index.html"), server.url("/static/second.html"));
            Check.equal(1, server.count("/static/index.html"), "history used cached first page");
            Check.equal(2, server.count("/static/second.html"), "force reload reached second page");
        } finally {
            panel.dispose();
            server.stop();
        }
    }

    /**
     * Classpath relative CSS and links use null Swing URLs; removing user notifications preserves internal link navigation.
     * <p>클래스패스 상대 CSS·링크는 null Swing URL을 쓰며 사용자 통지를 지워도 내부 링크 이동은 유지된다.
     * @throws Exception on failure / 실패 시
     */
    public void testClasspathStaticLinksAndHistory() throws Exception {
        Panel panel = createPanel();
        final AtomicReference<HyperlinkEvent> activation = new AtomicReference<HyperlinkEvent>();
        try {
            panel.addHyperlinkListener(new HyperlinkListener() {
                /** Records activation from the actual public component. / 실제 공개 컴포넌트의 활성화를 기록한다.
                 * @param event Swing hyperlink event / Swing 하이퍼링크 이벤트 */
                public void hyperlinkUpdate(HyperlinkEvent event) {
                    if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        activation.set(event);
                    }
                }
            });
            String first = "classpath://org/duckdns/hjow/swingwebpanel/testdata/static/index.html";
            String second = "classpath://org/duckdns/hjow/swingwebpanel/testdata/static/second.html";
            roundTrip(panel, first, second);
            Check.notNull(activation.get(), "public listener activation");
            Check.isNull(activation.get().getURL(), "classpath URL handler absent");
            Check.equal(second, activation.get().getDescription(), "full classpath address");
            panel.removeHyperlinkEvents();
            panel.back();
            awaitPixel(panel, 0x445566);
            click(panel);
            awaitPixel(panel, 0x667788);
            Check.equal(second, panel.getPage(), "link still works without user listeners");
        } finally {
            panel.dispose();
        }
    }

    /**
     * Linked CSS/imports/images share headers, cookies and cache; force refresh reaches every resource and changes real pixels.
     * <p>외부 CSS·import·이미지가 헤더·쿠키·캐시를 공유하며 force 새로고침은 모든 리소스를 재요청하여 실제 픽셀을 바꾼다.
     * @throws Exception on failure / 실패 시
     */
    public void testExternalCssImagesCacheAndForce() throws Exception {
        TestHttpServer server = new TestHttpServer();
        final Panel[] panels = new Panel[1];
        try {
            String source = "<!doctype html><link rel=stylesheet href='sheet.css'><img id=pic src='pixel.png' alt='sample'>";
            server.routeFixed("/page", TestHttpServer.Response.html(source).header("Cache-Control", "max-age=60")
                    .header("Set-Cookie", "resource=yes; Path=/"));
            server.routeFixed("/sheet.css", TestHttpServer.Response.ok("text/css", "@import 'nested.css'; #pic{width:60px;height:60px}")
                    .header("Cache-Control", "max-age=60"));
            server.routeFixed("/nested.css", TestHttpServer.Response.ok("text/css", "html,body{margin:0;padding:0;background:#abcdef}")
                    .header("Cache-Control", "max-age=60"));
            server.routeFixed("/pixel.png", new TestHttpServer.Response(200).bytes("image/png", png(0x118833)).header("Cache-Control", "max-age=60"));
            server.start();
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Creates the standard panel for HTTP resources. / HTTP 리소스용 표준 패널을 만든다. */
                public void run() {
                    panels[0] = new Panel();
                    panels[0].setSize(300, 180);
                }
            });
            Panel panel = panels[0];
            panel.setUserAgent("resource-test/1.0");
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("X-Resource-Test", "yes");
            panel.addHeaders(headers);
            panel.setPage(server.url("/page"));
            awaitPixel(panel, 0x118833);
            long deadline = System.currentTimeMillis() + 15000;
            // 이미지와 스타일의 완료 순서는 독립적이므로 import의 배경색도 기다린다.
            while (pixel(panel, 200, 20) != 0xabcdef && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.equal(0xabcdef, pixel(panel, 200, 20), "imported background painted");
            Check.equal(source, panel.getContent(), "HTTP source unchanged");
            panel.refresh();
            awaitConfirmation(panel);
            awaitPixel(panel, 0x118833);
            Check.equal(1, server.count("/pixel.png"), "normal refresh uses fresh image cache");
            Check.equal(1, server.count("/sheet.css"), "normal refresh uses fresh stylesheet cache");
            Check.equal(1, server.count("/nested.css"), "import remains cached");
            server.routeFixed("/pixel.png", new TestHttpServer.Response(200).bytes("image/png", png(0x2255aa)).header("Cache-Control", "max-age=60"));
            panel.refresh(true);
            awaitConfirmation(panel);
            awaitPixel(panel, 0x2255aa);
            deadline = System.currentTimeMillis() + 15000;
            // 마지막 import 요청까지 완료될 기회를 준다.
            while (server.count("/nested.css") < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.equal(2, server.count("/page"), "force reload requests main document");
            Check.equal(2, server.count("/pixel.png"), "force reload requests image");
            Check.equal(2, server.count("/sheet.css"), "force reload requests linked CSS");
            Check.equal(2, server.count("/nested.css"), "force reload requests imported CSS");
            // 모든 실제 요청이 같은 UA·추가 헤더를 쓰며 하위 요청에는 쿠키가 전달된다.
            for (TestHttpServer.Request request : server.requests()) {
                Check.equal("resource-test/1.0", request.header("user-agent"), "shared UA");
                Check.equal("yes", request.header("x-resource-test"), "shared extra headers");
                if (!request.path.equals("/page")) {
                    Check.contains(request.header("cookie"), "resource=yes", "shared cookies");
                }
            }
        } finally {
            if (panels[0] != null) {
                panels[0].dispose();
            }
            server.stop();
        }
    }

    /**
     * A stalled stylesheet never occupies the document queue, and its late result cannot repaint the replacement page.
     * <p>멈춘 스타일시트가 문서 큐를 차지하지 않으며 늦은 결과도 교체된 페이지를 다시 그리지 못한다.
     * @throws Exception on failure / 실패 시
     */
    public void testSlowStylesheetDoesNotDelayNavigation() throws Exception {
        TestHttpServer server = new TestHttpServer();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Panel[] panels = new Panel[1];
        try {
            server.routeFixed("/page", TestHttpServer.Response.html("<!doctype html><link rel=stylesheet href='slow.css'><p>old</p>"));
            server.route("/slow.css", new TestHttpServer.Handler() {
                /** Holds a resource response until navigation finishes. / 이동 완료까지 리소스 응답을 대기시킨다.
                 * @param request incoming request / 받은 요청
                 * @return old page CSS / 이전 페이지 CSS
                 * @throws Exception when interrupted / 인터럽트 시 */
                public TestHttpServer.Response handle(TestHttpServer.Request request) throws Exception {
                    entered.countDown();
                    release.await(15, TimeUnit.SECONDS);
                    return TestHttpServer.Response.ok("text/css", "body{background:red}");
                }
            });
            server.start();
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Creates the panel on the EDT. / EDT에서 패널을 만든다. */
                public void run() {
                    panels[0] = new Panel();
                    panels[0].setSize(300, 180);
                }
            });
            Panel panel = panels[0];
            panel.setPage(server.url("/page"));
            Check.isTrue(entered.await(10, TimeUnit.SECONDS), "stylesheet request started");
            panel.setContent("<!doctype html><style>html,body{margin:0;background:#345678}</style><div style='height:100px'></div>", "text/html");
            awaitPixel(panel, 0x345678);
            Check.equal(1, release.getCount(), "navigation completed while CSS server is still blocked");
            release.countDown();
            TestSupport.flushEdt();
            Check.equal(0x345678, pixel(panel, 20, 20), "late old stylesheet cannot repaint new document");
        } finally {
            release.countDown();
            if (panels[0] != null) {
                panels[0].dispose();
            }
            server.stop();
        }
    }

    /**
     * The standard panel paints its own CSS boxes, preserves source and discards old commands on clean/dispose.
     * <p>표준 패널이 자체 CSS 상자를 그리고 원문을 보존하며 clean/dispose에서 이전 명령을 버린다.
     * @throws Exception on failure / 실패 시
     */
    public void testOwnRendererPaintsAndClean() throws Exception {
        final Panel[] holder = new Panel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates and sizes the real Swing component. / 실제 Swing 컴포넌트를 만들고 크기를 정한다. */
            public void run() {
                holder[0] = new Panel();
                holder[0].setSize(300, 180);
            }
        });
        Panel panel = holder[0];
        try {
            String source = "<!doctype html>\r\n<style>html,body{margin:0;padding:0}"
                    + "#box{width:120px;height:1000px;background:#123456}</style><div id=box></div>";
            panel.setContent(source, "text/html");
            awaitPixel(panel, 0x123456);
            Check.equal(source, panel.getContent(), "received source remains unchanged");
            TestSupport.flushEdt();
            Check.isTrue(panel.getPreferredSize().height >= 1000, "scroll height follows document");
            Check.equal(0xffffff, pixel(panel, 200, 20), "box width is respected");
            panel.clean();
            awaitPixel(panel, 0xffffff);
            Check.equal("about:blank", panel.getPage(), "clean URL");
            panel.dispose();
            TestSupport.flushEdt();
            Check.equal(0xffffff, pixel(panel, 20, 20), "disposed page stays blank");
        } finally {
            panel.dispose();
        }
    }

    /**
     * A horizontal scrollbar does not prevent reflow when the surrounding viewport becomes narrower.
     * <p>가로 스크롤 막대가 생겨도 바깥 뷰포트가 좁아지면 다시 배치한다.
     * @throws Exception on failure / 실패 시
     */
    public void testViewportResizeWithOverflow() throws Exception {
        final Panel[] panels = new Panel[1];
        final JScrollPane[] panes = new JScrollPane[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates an overflowing scrollable component. / 넘침이 있는 스크롤 컴포넌트를 만든다. */
            public void run() {
                panels[0] = new Panel();
                panes[0] = new JScrollPane(panels[0]);
                panes[0].setSize(420, 240);
                panes[0].doLayout();
                panes[0].getViewport().doLayout();
            }
        });
        Panel panel = panels[0];
        try {
            panel.setContent("<!doctype html><style>body{margin:0}p{margin:0;background:#234567}</style>"
                    + "<p>one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen eighteen</p>"
                    + "<div style='width:700px;height:50px'></div>", "text/html");
            awaitPixel(panel, 0x234567);
            TestSupport.flushEdt();
            final int before = panel.contentHeight();
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Resizes only the viewport while content remains wider. / 내용이 넓게 남아 있는 상태에서 뷰포트만 줄인다. */
                public void run() {
                    panes[0].setSize(180, 240);
                    panes[0].doLayout();
                    panes[0].getViewport().doLayout();
                }
            });
            long deadline = System.currentTimeMillis() + 15000;
            // 조상 크기 변경 통지가 재배치를 일으켜 문단 높이가 증가할 때까지 기다린다.
            while (panel.contentHeight() <= before && System.currentTimeMillis() < deadline) {
                TestSupport.flushEdt();
                Thread.sleep(20);
            }
            Check.isTrue(panel.contentHeight() > before, "viewport, not overflowing component width, controls wrapping");
            Check.isTrue(!panel.getScrollableTracksViewportWidth(), "horizontal overflow remains scrollable");
        } finally {
            panel.dispose();
        }
    }
}
