package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * What the integration tests of the selection, the copy and the zoom share (plan item P11-08): the test page, a component that records
 * what is copied, the parsed rectangles that the page reports, and the helpers that send real AWT events, wait for the asynchronous
 * results and read the painted pixels.
 * <p>
 * 선택, 복사, 확대의 통합 시험이 공유하는 것(계획 P11-08): 시험 페이지, 복사된 것을 기록하는 컴포넌트, 페이지가 알린 사각형을 해석한 것, 실제 AWT 이벤트를
 * 보내고 비동기 결과를 기다리고 그려진 픽셀을 읽는 도우미.
 */
final class TextSelectionFixture {

    /**
     * The script of the page: it reports the rectangles of the spans once through <code>sessionStorage</code> (the key <code>report</code>,
     * which {@link #reported(SwingWebPanel)} reads with <code>getSessionStorage()</code>), then the selected text on every
     * <code>selectionchange</code> (<code>sel:text|count</code>) and the size of the viewport on every <code>resize</code>
     * (<code>size:WxH</code>). <code>%EXTRA%</code> is replaced by more script of a case.
     * <p>
     * 페이지의 스크립트: span 의 사각형을 <code>sessionStorage</code>(키 <code>report</code>. {@link #reported(SwingWebPanel)} 가 <code>getSessionStorage()</code> 로
     * 읽는다)로 한 번 알리고, 그 뒤 <code>selectionchange</code> 마다 선택된 텍스트(<code>sel:텍스트|횟수</code>)를, <code>resize</code> 마다 뷰포트
     * 크기(<code>size:WxH</code>)를 알린다. <code>%EXTRA%</code> 는 사례의 추가 스크립트로 바뀐다.
     */
    private static final String PAGE_TEMPLATE = "<!doctype html><style>html,body{margin:0;padding:0} body{font:16px monospace;line-height:24px} p{margin:0 0 10px 0}"
            + " #box{width:50px;height:50px;background:#123456} #n{user-select:none}</style>"
            + "<div id=box></div><p><span id=a>hello world</span></p><p><span id=b>second line</span></p><p><span id=n>locked text</span></p>"
            + "<div style='height:1000px'></div>"
            + "<script>setTimeout(function () {"
            + " function report(v) { sessionStorage.setItem('report', v); }"
            + " function r(id) { var b = document.getElementById(id).getBoundingClientRect(); return [b.left, b.top, b.width, b.height].join(','); }"
            + " var count = 0;"
            + " document.addEventListener('selectionchange', function () { count++; report('sel:' + window.getSelection().toString() + '|' + count); });"
            + " window.addEventListener('resize', function () { report('size:' + window.innerWidth + 'x' + window.innerHeight); });"
            + " %EXTRA%"
            + " sessionStorage.setItem('rects', 'rects:' + r('a') + ';' + r('b') + ';' + r('n'));"
            + " }, 0);</script>";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TextSelectionFixture() {
    }

    /**
     * Returns the test page.
     * <p>
     * 시험 페이지를 반환한다.
     *
     * @param extra more script that runs once the page is shown, or an empty text / 페이지가 보인 뒤 한 번 실행되는 추가 스크립트. 없으면 빈 문자열
     * @return the page / 페이지
     */
    static String page(String extra) {
        return PAGE_TEMPLATE.replace("%EXTRA%", extra);
    }

    /**
     * A component that records what is copied instead of using the system clipboard.
     * <p>
     * 시스템 클립보드 대신 복사된 것을 기록하는 컴포넌트.
     */
    static class Panel extends SwingWebPanel {
        /** Serialization identifier. / 직렬화 식별자. */
        private static final long serialVersionUID = 1L;
        /** The copied texts. 복사된 텍스트. */
        final List<String> texts = Collections.synchronizedList(new ArrayList<String>());
        /** The copied markup (null entries for none). 복사된 마크업(없으면 null). */
        final List<String> markups = Collections.synchronizedList(new ArrayList<String>());
        /** True to record the copies (the default), false to use the system clipboard. 복사를 기록하면 true(기본), 시스템 클립보드를 쓰려면 false. */
        volatile boolean recording = true;
        /** The server that gives the pages of the cases (a page over HTTP has an origin, so its script can use sessionStorage). 사례의 페이지를 주는 서버(HTTP 페이지는 오리진이 있으므로 스크립트가 sessionStorage 를 쓸 수 있다). */
        private TestHttpServer server;
        /** The number of pages that were shown. 보여 준 페이지 수. */
        private int pages;

        /**
         * Creates the component.
         * <p>
         * 컴포넌트를 만든다.
         */
        Panel() {
        }

        /**
         * Starts the navigation to a page that the test server gives (it starts the server at the first call).
         * <p>
         * 시험 서버가 주는 페이지로의 이동을 시작한다(첫 호출에서 서버를 시작한다).
         *
         * @param html the page / 페이지
         * @throws Exception if the server cannot start / 서버를 시작할 수 없으면
         */
        void show(String html) throws Exception {
            if (server == null) {
                server = new TestHttpServer();
                server.start();
            }
            pages++;
            String path = "/page" + pages;
            server.routeFixed(path, TestHttpServer.Response.html(html));
            setPage(server.url(path));
        }

        /**
         * Stops the server of the cases.
         * <p>
         * 사례의 서버를 멈춘다.
         */
        void stopServer() {
            if (server != null) {
                server.stop();
                server = null;
            }
        }

        /**
         * Records the copy.
         * <p>
         * 복사를 기록한다.
         *
         * @param plainText    the text / 텍스트
         * @param htmlFragment the markup or null / 마크업. 없으면 null
         */
        protected void copyToClipboard(String plainText, String htmlFragment) throws Exception {
            // 기록하지 않는 설정이면 기본 구현(시스템 클립보드)을 쓴다.
            if (!recording) {
                super.copyToClipboard(plainText, htmlFragment);
                return;
            }
            texts.add(plainText);
            markups.add(htmlFragment);
        }
    }

    /**
     * The rectangles of the three spans as the script reported them: x, y, width, height of each, in CSS pixels.
     * <p>
     * 스크립트가 알린 span 세 개의 사각형: 각각 x, y, 너비, 높이(CSS 픽셀).
     */
    static final class Rects {
        /** The rectangles of the spans a, b and n. span a, b, n 의 사각형. */
        final double[][] r = new double[3][];

        /**
         * Parses the title that the script set.
         * <p>
         * 스크립트가 정한 제목을 해석한다.
         *
         * @param title the title / 제목
         */
        Rects(String title) {
            String[] parts = title.substring("rects:".length()).split(";");
            // 세 span 의 사각형을 하나씩 해석한다.
            for (int i = 0; i < 3; i++) {
                String[] n = parts[i].split(",");
                r[i] = new double[] { Double.parseDouble(n[0]), Double.parseDouble(n[1]), Double.parseDouble(n[2]), Double.parseDouble(n[3]) };
            }
        }

        /**
         * Returns the x of the left edge of a span.
         * <p>
         * span 의 왼쪽 가장자리 x 를 반환한다.
         *
         * @param i the span / span
         * @return x / x
         */
        double left(int i) {
            return r[i][0];
        }

        /**
         * Returns the y of the middle of a span.
         * <p>
         * span 의 가운데 y 를 반환한다.
         *
         * @param i the span / span
         * @return y / y
         */
        double middle(int i) {
            return r[i][1] + r[i][3] / 2;
        }

        /**
         * Returns the y of the bottom row of a span (a row that only descenders touch).
         * <p>
         * span 의 맨 아래 행의 y 를 반환한다(내림 글자만 닿는 행).
         *
         * @param i the span / span
         * @return y / y
         */
        double bottom(int i) {
            return r[i][1] + r[i][3] - 2;
        }

        /**
         * Returns the width of one character of a span (the font is monospace) given its number of characters.
         * <p>
         * 글자 수가 주어진 span 의 글자 하나의 너비를 반환한다(글꼴이 고정폭이다).
         *
         * @param i     the span / span
         * @param chars the number of characters / 글자 수
         * @return the width / 너비
         */
        double advance(int i, int chars) {
            return r[i][2] / chars;
        }
    }

    /**
     * Creates a component of a given size on the EDT.
     * <p>
     * EDT 에서 주어진 크기의 컴포넌트를 만든다.
     *
     * @param width  width / 너비
     * @param height height / 높이
     * @return the component / 컴포넌트
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static Panel create(final int width, final int height) throws Exception {
        final Panel[] result = new Panel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                result[0] = new Panel();
                result[0].setSize(width, height);
            }
        });
        return result[0];
    }

    /**
     * Shows a page and waits until the script reported the rectangles of the spans.
     * <p>
     * 페이지를 보여 주고 스크립트가 span 의 사각형을 알릴 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param html  the page / 페이지
     * @return the rectangles / 사각형
     * @throws Exception on failure / 실패 시
     */
    static Rects open(Panel panel, String html) throws Exception {
        panel.show(html);
        long deadline = System.currentTimeMillis() + 15000;
        // 스크립트가 사각형을 알릴 때까지 sessionStorage 의 rects 항목을 반복해서 확인한다.
        while (stored(panel, "rects").length() == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.isTrue(stored(panel, "rects").startsWith("rects:"), "the page reported the rectangles of its spans");
        TestSupport.flushEdt();
        return new Rects(stored(panel, "rects"));
    }

    /**
     * Returns an item of the sessionStorage of the component, or an empty text.
     * <p>
     * 컴포넌트의 sessionStorage 항목을 반환한다. 없으면 빈 문자열.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value / 값
     */
    static String stored(SwingWebPanel panel, String key) {
        // 컴포넌트의 sessionStorage 항목 중 키가 같은 것을 찾는다.
        for (StorageEntry e : panel.getSessionStorage()) {
            if (key.equals(e.getKey())) {
                return e.getValue();
            }
        }
        return "";
    }

    /**
     * Returns what the script of the page reported last (the <code>report</code> item of its sessionStorage), or an empty text.
     * <p>
     * 페이지의 스크립트가 마지막으로 알린 것(sessionStorage 의 <code>report</code> 항목)을 반환한다. 없으면 빈 문자열.
     *
     * @param panel the component / 컴포넌트
     * @return the report / 알림
     */
    static String reported(SwingWebPanel panel) {
        return stored(panel, "report");
    }

    /**
     * Waits until the report of the page starts with a prefix.
     * <p>
     * 페이지의 알림이 접두사로 시작할 때까지 기다린다.
     *
     * @param panel  the component / 컴포넌트
     * @param prefix the prefix / 접두사
     * @throws Exception on interruption / 인터럽트 시
     */
    static void awaitReport(SwingWebPanel panel, String prefix) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        // 문서 큐의 작업이 끝나 스크립트가 알릴 때까지 알림을 반복해서 확인한다.
        while (System.currentTimeMillis() < deadline) {
            if (reported(panel).startsWith(prefix)) {
                return;
            }
            Thread.sleep(10);
        }
        Check.fail("the report should start with " + prefix + " but is '" + reported(panel) + "'");
    }

    /**
     * Waits until the script of the page reports that exactly this text is selected (and fails with what it reported otherwise).
     * <p>
     * 페이지의 스크립트가 정확히 이 텍스트가 선택되었다고 알릴 때까지 기다린다(그렇지 않으면 알린 내용과 함께 실패한다).
     *
     * @param panel    the component / 컴포넌트
     * @param expected the selected text (<code>Selection.toString()</code>) / 선택된 텍스트(<code>Selection.toString()</code>)
     * @throws Exception on interruption / 인터럽트 시
     */
    static void awaitSelection(SwingWebPanel panel, String expected) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        // 선택이 바뀌어 알림이 기대한 값이 될 때까지 기다린다.
        while (System.currentTimeMillis() < deadline) {
            if (reported(panel).startsWith("sel:" + expected + "|")) {
                return;
            }
            Thread.sleep(10);
        }
        Check.fail("the selected text should be '" + expected + "' but the page reported '" + reported(panel) + "'");
    }

    /**
     * Waits until a number of copies were recorded.
     * <p>
     * 복사가 주어진 수만큼 기록될 때까지 기다린다.
     *
     * @param panel the component / 컴포넌트
     * @param count the number of copies / 복사 수
     * @throws Exception on interruption / 인터럽트 시
     */
    static void awaitCopies(Panel panel, int count) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        // 복사가 EDT 로 넘어가 기록될 때까지 기다린다.
        while (panel.texts.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        Check.equal(count, panel.texts.size(), "the number of copies");
    }

    /**
     * Dispatches a mouse event on the EDT.
     * <p>
     * EDT 에서 마우스 이벤트를 전달한다.
     *
     * @param panel      the component / 컴포넌트
     * @param id         the AWT event id / AWT 이벤트 ID
     * @param x          component x / 컴포넌트 x
     * @param y          component y / 컴포넌트 y
     * @param clickCount the click count / 클릭 횟수
     * @param modifiers  the extended modifiers / 확장 보조 값
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void mouse(final SwingWebPanel panel, final int id, final double x, final double y, final int clickCount, final int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 전달한다. */
            public void run() {
                panel.dispatchEvent(new MouseEvent(panel, id, System.currentTimeMillis(), modifiers, (int) Math.round(x), (int) Math.round(y), clickCount,
                        false, id == MouseEvent.MOUSE_MOVED ? MouseEvent.NOBUTTON : MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Presses the main button, drags to another point and releases (all in component coordinates).
     * <p>
     * 주 버튼을 누르고 다른 점까지 끌고 뗀다(모두 컴포넌트 좌표).
     *
     * @param panel the component / 컴포넌트
     * @param x1    the x of the press / 누른 곳의 x
     * @param y1    the y of the press / 누른 곳의 y
     * @param x2    the x of the release / 뗀 곳의 x
     * @param y2    the y of the release / 뗀 곳의 y
     * @throws Exception on failure / 실패 시
     */
    static void drag(SwingWebPanel panel, double x1, double y1, double x2, double y2) throws Exception {
        mouse(panel, MouseEvent.MOUSE_PRESSED, x1, y1, 1, InputEvent.BUTTON1_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_DRAGGED, (x1 + x2) / 2, (y1 + y2) / 2, 1, InputEvent.BUTTON1_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_DRAGGED, x2, y2, 1, InputEvent.BUTTON1_DOWN_MASK);
        mouse(panel, MouseEvent.MOUSE_RELEASED, x2, y2, 1, 0);
    }

    /**
     * Presses and releases the main button at a point (a click).
     * <p>
     * 한 점에서 주 버튼을 누르고 뗀다(클릭).
     *
     * @param panel      the component / 컴포넌트
     * @param x          component x / 컴포넌트 x
     * @param y          component y / 컴포넌트 y
     * @param clickCount the click count / 클릭 횟수
     * @param shift      true to hold Shift / Shift 를 누르고 있으면 true
     * @throws Exception on failure / 실패 시
     */
    static void click(SwingWebPanel panel, double x, double y, int clickCount, boolean shift) throws Exception {
        int down = InputEvent.BUTTON1_DOWN_MASK | (shift ? InputEvent.SHIFT_DOWN_MASK : 0);
        mouse(panel, MouseEvent.MOUSE_PRESSED, x, y, clickCount, down);
        mouse(panel, MouseEvent.MOUSE_RELEASED, x, y, clickCount, shift ? InputEvent.SHIFT_DOWN_MASK : 0);
    }

    /**
     * Dispatches a key press on the EDT.
     * <p>
     * EDT 에서 키 누름을 전달한다.
     *
     * @param panel     the component / 컴포넌트
     * @param keyCode   the AWT key code / AWT 키 코드
     * @param modifiers the extended modifiers / 확장 보조 값
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void key(final SwingWebPanel panel, final int keyCode, final int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 전달한다. */
            public void run() {
                KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), modifiers, keyCode, KeyEvent.CHAR_UNDEFINED);
                // KeyEvent 를 dispatchEvent 로 보내면 AWT 가 포커스 소유자에게 돌려보내므로 등록된 리스너를 직접 부른다.
                for (java.awt.event.KeyListener l : panel.getKeyListeners()) {
                    l.keyPressed(event);
                }
            }
        });
    }

    /**
     * Dispatches a mouse wheel event on the EDT.
     * <p>
     * EDT 에서 마우스 휠 이벤트를 전달한다.
     *
     * @param panel     the component / 컴포넌트
     * @param x         component x / 컴포넌트 x
     * @param y         component y / 컴포넌트 y
     * @param rotation  the wheel rotation (negative: up) / 휠 회전(음수는 위)
     * @param modifiers the extended modifiers / 확장 보조 값
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static void wheel(final SwingWebPanel panel, final int x, final int y, final int rotation, final int modifiers) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 전달한다. */
            public void run() {
                panel.dispatchEvent(new MouseWheelEvent(panel, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), modifiers, x, y, 0, false,
                        MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, rotation));
            }
        });
    }

    /**
     * Paints the component into an image on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그린다.
     *
     * @param panel the component / 컴포넌트
     * @return the image / 이미지
     * @throws Exception on EDT failure / EDT 실패 시
     */
    static BufferedImage paint(final SwingWebPanel panel) throws Exception {
        final BufferedImage[] result = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints without a native window. / 네이티브 창 없이 그린다. */
            public void run() {
                BufferedImage image = new BufferedImage(Math.max(1, panel.getWidth()), Math.max(1, panel.getHeight()), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                try {
                    panel.paint(g);
                } finally {
                    g.dispose();
                }
                result[0] = image;
            }
        });
        return result[0];
    }

    /**
     * Counts the pixels of a row part that look highlighted (blue clearly stronger than red) and the white ones.
     * <p>
     * 한 행의 일부에서 강조된 것으로 보이는 픽셀(파랑이 빨강보다 확실히 강함)과 흰 픽셀의 수를 센다.
     *
     * @param image the image / 이미지
     * @param x0    the first x / 처음 x
     * @param x1    the last x (exclusive) / 마지막 x(제외)
     * @param y     the row / 행
     * @return {highlighted, white} / {강조된 수, 흰 수}
     */
    static int[] count(BufferedImage image, int x0, int x1, int y) {
        int blue = 0;
        int white = 0;
        // 구간의 픽셀을 하나씩 보며 분류한다.
        for (int x = x0; x < x1; x++) {
            int rgb = image.getRGB(x, y) & 0xffffff;
            int r = (rgb >> 16) & 0xff;
            int b = rgb & 0xff;
            if (rgb == 0xffffff) {
                white++;
            } else if (b - r > 40) {
                blue++;
            }
        }
        return new int[] { blue, white };
    }

    /**
     * Closes a component.
     * <p>
     * 컴포넌트를 닫는다.
     *
     * @param panel the component / 컴포넌트
     */
    static void close(SwingWebPanel panel) {
        panel.dispose();
        if (panel instanceof Panel) {
            ((Panel) panel).stopServer();
        }
        TestSupport.flushEdt();
    }

    /**
     * Joins texts with commas.
     * <p>
     * 문자열을 쉼표로 잇는다.
     *
     * @param list the texts / 문자열
     * @return the text / 문자열
     */
    static String join(List<String> list) {
        StringBuilder sb = new StringBuilder();
        synchronized (list) {
            // 목록의 문자열을 순서대로 이어 붙인다.
            for (String s : list) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(s);
            }
        }
        return sb.toString();
    }
}
