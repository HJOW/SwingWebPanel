package netstormrun;

import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.ConsoleEvent;
import org.duckdns.hjow.swingwebpanel.ConsoleListener;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.WebMessageEvent;
import org.duckdns.hjow.swingwebpanel.WebMessageListener;

/**
 * Measures the supplied NetStorm game through the actual SwingWebPanel.
 * <p>
 * 제공한 NetStorm 게임을 실제 SwingWebPanel로 실행하며 측정한다.
 */
public final class NetstormRun implements ConsoleListener, WebMessageListener {
    /** Component width and height in desktop pixels. / 컴포넌트 너비와 높이(픽셀). */
    private static final int WIDTH = 900, HEIGHT = 650;
    /** The component and its offscreen window-sized target, both used on the EDT. / EDT에서 쓰는 컴포넌트와 화면 밖 그림. */
    private SwingWebPanel panel;
    /** Live test window, if requested. / 요청했으면 실제 시험 창. */
    private JFrame window;
    /** Reusable buffer for ordinary component painting. / 일반 컴포넌트 그리기에 반복 사용할 버퍼. */
    private final BufferedImage screen = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
    /** Most recent game state, from the instrumented document. / 계측 문서가 마지막으로 보낸 게임 상태. */
    private volatile String state = "boot";
    /** Latest in-game dialog key. / 게임 내부 대화창의 최신 키. */
    private volatile String tell = "";
    /** Number of effects started by the game audio system. / 게임 오디오가 재생을 시작한 효과음 수. */
    private volatile int soundStarts;
    /** True when page console reported an error. / 페이지 콘솔에 오류가 나오면 true. */
    private volatile boolean error;
    /** Output directory for captured Swing painting. / Swing 그림을 저장할 디렉터리. */
    private final File output;

    /**
     * Overrides only the selected HTML page in memory, with all its dependencies read from the external classpath root.
     * <p>
     * 선택한 HTML만 메모리에서 바꾸며 의존 자료는 모두 외부 클래스패스 루트에서 읽는다.
     */
    private static final class ProbeResources extends ClassLoader {
        /** Selected classpath resource name. / 선택한 클래스패스 리소스 이름. */
        private final String page;
        /** UTF-8 HTML bytes with the measuring script installed. / 계측 스크립트를 넣은 UTF-8 HTML 바이트. */
        private final byte[] instrumented;

        /**
         * Wraps the external classpath and installs the probe in the selected file's head.
         * <p>
         * 외부 클래스패스를 감싸고 선택한 파일의 head에 계측 스크립트를 설치한다.
         *
         * @param web the external www directory / 외부 www 디렉터리
         * @param entry index.html or debug.html / index.html 또는 debug.html
         * @throws IOException if a probe or page cannot be read / 계측 스크립트나 페이지를 읽지 못하면
         */
        ProbeResources(File web, String entry) throws IOException {
            super(new URLClassLoader(new URL[] { web.getParentFile().toURI().toURL() }, NetstormRun.class.getClassLoader()));
            page = "www/" + entry;
            String html = read(new File(web, entry));
            String probe = read(new File("tools/netstormrun/probe.js"));
            int end = html.indexOf("</head>");
            if (end < 0) {
                throw new IOException("The selected page lacks a closing head element");
            }
            instrumented = (html.substring(0, end) + "<script>" + probe + "</script>" + html.substring(end)).getBytes("UTF-8");
        }

        /**
         * Returns the in-memory entry page or the original dependent resource.
         * <p>
         * 메모리의 진입 페이지 또는 원래 의존 리소스를 반환한다.
         *
         * @param name requested classpath path / 요청한 클래스패스 경로
         * @return stream or null if unavailable / 스트림. 없으면 null
         */
        public InputStream getResourceAsStream(String name) {
            return page.equals(name) ? new ByteArrayInputStream(instrumented) : super.getResourceAsStream(name);
        }
    }

    /**
     * Sets the output destination.
     * <p>
     * 출력 디렉터리를 지정한다.
     *
     * @param output output directory / 출력 디렉터리
     */
    private NetstormRun(File output) {
        this.output = output;
    }

    /**
     * Runs the probe with the JVM and its heap settings selected by the caller.
     * <p>
     * 호출자가 선택한 JVM과 힙 설정으로 계측한다.
     *
     * @param args www directory, entry name, output directory, timeout, optional task limit and window flag / www 디렉터리, 진입 이름, 출력 디렉터리, 제한 시간, 선택 작업 한도 및 창 옵션
     * @throws Exception when setup or capture fails / 초기화나 캡처가 실패하면
     */
    public static void main(String[] args) throws Exception {
        boolean visible = args.length == 6 && "--window".equals(args[5]);
        if ((args.length != 4 && args.length != 5 && !visible) || !("index".equals(args[1]) || "debug".equals(args[1]))) {
            System.err.println("Usage: netstormrun <www directory> <index|debug> <output directory> <seconds> [task milliseconds [--window]]");
            System.exit(2);
        }
        System.setProperty("java.awt.headless", visible ? "false" : "true");
        File web = new File(args[0]).getCanonicalFile();
        File output = new File(args[2]).getCanonicalFile();
        if (!web.isDirectory() || !(output.isDirectory() || output.mkdirs())) {
            throw new IOException("Cannot open the sample or output directory");
        }
        String entry = args[1] + ".html";
        long taskMillis = args.length >= 5 ? Long.parseLong(args[4]) : 0L;
        System.out.println("runtime java=" + System.getProperty("java.version")
                + " max_heap_bytes=" + Runtime.getRuntime().maxMemory() + " page=" + entry + " custom_task_ms=" + taskMillis
                + " java_sound_mixers=" + AudioSystem.getMixerInfo().length);
        System.out.println("sample|state|loaded|total|page_ms|frames|callback_ms|max_callback_ms|interval_ms|responses|pending_headers|body_bytes|fetch_span_ms|audio|music|dialog|sound_starts");
        NetstormRun run = new NetstormRun(output);
        int exit = run.measure(new ProbeResources(web, entry), "classpath://www/" + entry + "?lang=ko", Integer.parseInt(args[3]),
                taskMillis, visible);
        System.exit(exit);
    }

    /**
     * Opens the Korean page and starts the game on its click-to-start state.
     * <p>
     * 한국어 페이지를 열고 클릭하여 시작 상태에서 게임을 시작한다.
     *
     * @param resources classpath with the measuring page / 계측 페이지를 포함한 클래스패스
     * @param page absolute entry address / 진입 절대 주소
     * @param seconds maximum duration / 최대 실행 시간(초)
     * @param taskMillis time allowed per task of that page, 0 for default / 그 페이지의 작업별 허용 시간. 0이면 기본값
     * @param visible whether to show a Swing window and click the opening dialog / Swing 창과 시작 안내창 실제 클릭 여부
     * @return 0 if the main menu remains active without console errors, 1 otherwise / 오류 없이 메인 메뉴가 유지되면 0, 그 외 1
     * @throws Exception if Swing or output fails / Swing 또는 출력 실패 시
     */
    private int measure(final ClassLoader resources, final String page, int seconds, final long taskMillis, final boolean visible) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the browser with access to the external root. / 외부 루트를 볼 수 있는 브라우저를 만든다. */
            public void run() {
                Thread edt = Thread.currentThread();
                ClassLoader saved = edt.getContextClassLoader();
                try {
                    edt.setContextClassLoader(resources);
                    panel = new SwingWebPanel() {
                        /** Supplies the chosen time only to the sampled entry page. / 선택한 시간을 계측할 진입 페이지에만 적용한다. */
                        @Override
                        protected long getScriptTaskTimeLimitMillis(String url) {
                            return page.equals(url) ? taskMillis : super.getScriptTaskTimeLimitMillis(url);
                        }
                    };
                    panel.setSize(WIDTH, HEIGHT);
                    panel.addConsoleListener(NetstormRun.this);
                    panel.addWebMessageListener(NetstormRun.this);
                    if (visible) {
                        window = new JFrame("NetStorm - SwingWebPanel");
                        panel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
                        window.setContentPane(panel);
                        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        window.pack();
                        window.setLocationRelativeTo(null);
                        window.setVisible(true);
                    }
                } finally {
                    edt.setContextClassLoader(saved);
                }
            }
        });
        long start = System.nanoTime();
        long menu = 0;
        long welcomeClick = 0;
        int soundsBeforeClick = 0;
        boolean clicked = false;
        try {
            panel.setPage(page);
            // 일반 페인트와 상태 메시지를 처리하며 시작 또는 메뉴에 도달했을 때 그림을 저장한다.
            while (millisSince(start) < seconds * 1000L && !"error".equals(state)) {
                paint();
                if (!clicked && "clickToStart".equals(state)) {
                    capture("click-to-start");
                    click(WIDTH / 2, HEIGHT / 2);
                    clicked = true;
                }
                if ("mainMenu".equals(state)) {
                    if (menu == 0L) {
                        menu = System.nanoTime();
                    } else if (millisSince(menu) >= 5000L) {
                        // 실제 창에서는 초기 환영 창을 눌러 대화 내용이 바뀌는 입력까지 확인한다.
                        if (!visible || (welcomeClick != 0 && ("Welcome2".equals(tell) || millisSince(welcomeClick) >= 6000L))) {
                            break;
                        }
                        if (welcomeClick == 0 && "Welcome".equals(tell)) {
                            capture("welcome");
                            soundsBeforeClick = soundStarts;
                            click(WIDTH / 2, 381);
                            welcomeClick = System.nanoTime();
                        }
                    }
                }
                Thread.sleep(300L);
            }
            paint();
            capture("final");
            long heapBytes = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            boolean inputPassed = !visible || (welcomeClick != 0 && "Welcome2".equals(tell));
            System.out.println("result state=" + state + " error=" + error + " wall_ms=" + millisSince(start)
                    + " heap_used_bytes=" + heapBytes + " progress=" + panel.getProgress() + " dialog=" + tell + " input=" + inputPassed
                    + " click_sound_started=" + (welcomeClick != 0 && soundStarts > soundsBeforeClick));
            return "mainMenu".equals(state) && !error && inputPassed ? 0 : 1;
        } finally {
            panel.dispose();
            if (window != null) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    /** Closes the native window. / 실제 창을 닫는다. */
                    public void run() {
                        window.dispose();
                    }
                });
            }
        }
    }

    /** Paints the current component on the EDT. / EDT에서 현재 컴포넌트를 그린다. */
    private void paint() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints one offscreen image. / 화면 밖 그림 한 장을 그린다. */
            public void run() {
                Graphics2D graphics = screen.createGraphics();
                try {
                    panel.paint(graphics);
                } finally {
                    graphics.dispose();
                }
            }
        });
    }

    /** Captures the last image after the EDT has painted. / EDT가 그린 마지막 그림을 저장한다. */
    private void capture(String name) throws Exception {
        BufferedImage picture = screen;
        if (window != null) {
            Point p = windowOrigin();
            picture = new Robot().createScreenCapture(new Rectangle(p.x, p.y, WIDTH, HEIGHT));
        }
        ImageIO.write(picture, "png", new File(output, name + ".png"));
    }

    /** Returns the visible panel's origin. / 화면에 보이는 패널의 원점을 반환한다. */
    private Point windowOrigin() throws Exception {
        final Point[] p = new Point[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Reads the visible point on the EDT. / EDT에서 화면 위치를 읽는다. */
            public void run() {
                window.toFront();
                p[0] = panel.getLocationOnScreen();
            }
        });
        return p[0];
    }

    /** Delivers a left-click through the normal Swing input path. / Swing의 일반 입력 경로를 통해 왼쪽 클릭을 보낸다. */
    private void click(final int x, final int y) throws Exception {
        if (window != null) {
            Point base = windowOrigin();
            Point saved = MouseInfo.getPointerInfo().getLocation();
            Robot robot = new Robot();
            robot.mouseMove(base.x + x, base.y + y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();
            robot.mouseMove(saved.x, saved.y);
            return;
        }
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Sends the synthetic AWT events on the EDT. / EDT에서 합성 AWT 이벤트를 보낸다. */
            public void run() {
                long now = System.currentTimeMillis();
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_PRESSED, now, InputEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1));
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_RELEASED, now, 0, x, y, 1, false, MouseEvent.BUTTON1));
                panel.dispatchEvent(new MouseEvent(panel, MouseEvent.MOUSE_CLICKED, now, 0, x, y, 1, false, MouseEvent.BUTTON1));
            }
        });
    }

    /** Records page console messages with their script positions. / 페이지 콘솔과 스크립트 위치를 기록한다. */
    public void consoleMessage(ConsoleEvent event) {
        if (event.getLevel() == ConsoleEvent.Level.ERROR) {
            error = true;
        }
        System.out.println("console|" + event.getKind() + "|" + event.getLevel() + "|" + event.getScriptUrl() + ":"
                + event.getLineNumber() + ":" + event.getColumnNumber() + "|" + event.getMessage());
    }

    /** Records state and timing samples emitted by the page. / 페이지의 상태 및 시간 표본을 기록한다. */
    public void messageReceived(WebMessageEvent event) {
        Object data = event.getData();
        if (data instanceof String && ((String) data).startsWith("sample|")) {
            String line = (String) data;
            String[] fields = line.split("\\|", -1);
            if (fields.length == 17) {
                soundStarts = Integer.parseInt(fields[16]);
                tell = fields[15];
                state = fields[1];
                System.out.println(line);
            }
        }
    }

    /** Computes elapsed milliseconds using a monotonic clock. / 단조 시계로 경과 밀리초를 계산한다. */
    private static long millisSince(long start) {
        return (System.nanoTime() - start) / 1000000L;
    }

    /** Reads a small local UTF-8 text file in full. / 작은 로컬 UTF-8 텍스트 파일을 모두 읽는다. */
    private static String read(File source) throws IOException {
        InputStream in = new FileInputStream(source);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            // 파일 끝까지 열어 둔 읽기 스트림을 소비한다.
            while ((count = in.read(buffer)) != -1) {
                out.write(buffer, 0, count);
            }
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }
}
