package uicheck;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

/**
 * Opens a real Swing window (a {@link JFrame} with a {@link JScrollPane} around a {@link SwingWebPanel}), runs a list of steps given on the
 * command line and saves screenshots of the window that <code>java.awt.Robot</code> reads from the real screen. It checks the layer that the
 * off-screen tests cannot reach: the heavyweight window, the platform fonts and the real scroll bars. Mouse clicks are posted into the AWT
 * event queue by default, so the user's real mouse pointer is not moved; the option <code>--realmouse</code> uses the Robot instead (the pointer is
 * put back to where it was after each click).
 * <p>
 * 실제 Swing 창({@link SwingWebPanel} 을 {@link JScrollPane} 으로 감싼 {@link JFrame})을 열고 명령행의 단계를 차례로 실행하며
 * <code>java.awt.Robot</code> 이 실제 화면에서 읽은 창의 스크린샷을 저장한다. 화면 밖 시험이 닿지 못하는 계층(무거운 창, 플랫폼 글꼴, 실제 스크롤 막대)을
 * 확인한다. 마우스 클릭은 기본적으로 AWT 이벤트 큐에 넣으므로 사용자의 실제 마우스 포인터는 움직이지 않는다. <code>--realmouse</code> 를 주면 Robot 을 쓰며
 * 클릭이 끝나면 포인터를 원래 위치로 되돌린다.
 * <p>
 * Usage / 사용법: <code>UiCheck --out DIR [--size WxH] STEP...</code>. Steps / 단계: <code>--url U</code>, <code>--shot NAME</code>,
 * <code>--scroll Y</code>, <code>--click X Y</code>, <code>--back</code>, <code>--forward</code>, <code>--refresh</code>, <code>--wait MS</code>,
 * <code>--expect-title TEXT</code>, <code>--dispose</code>. Exit codes / 종료 코드: 0 ok, 1 a step or an expectation failed, 2 usage,
 * 3 no display (headless).
 */
public final class UiCheck {

    /** Longest wait for a page to finish loading in milliseconds. / 페이지 로딩을 기다리는 가장 긴 시간(밀리초). */
    private static final int LOAD_TIMEOUT_MS = 30000;

    /** Time that the progress must stay at 100 before the page counts as settled, in milliseconds. / 진행률이 100 으로 머물러야 안정된 것으로 보는 시간(밀리초). */
    private static final int SETTLE_MS = 400;

    /** The window / 최상위 창(프레임). */
    private JFrame frame;

    /** The scroll pane around the panel / 패널을 감싼 스크롤 창. */
    private JScrollPane scroll;

    /** The browser panel / 브라우저 패널. */
    private SwingWebPanel panel;

    /** The robot that reads the screen / 화면을 읽는 로봇. */
    private Robot robot;

    /** The directory of the screenshots / 스크린샷 디렉터리. */
    private File outDir;

    /** True if clicks use the real mouse / 클릭에 실제 마우스를 쓰면 true. */
    private boolean realMouse;

    /** Number of failed expectations / 실패한 기대의 수. */
    private int failures;

    /** The built-in HTTP server, or null if <code>--serve</code> was not given / 내장 HTTP 서버. <code>--serve</code> 가 없으면 null. */
    private EchoServer server;

    /** The helper that presses real keys, made with the robot / 실제 키를 누르는 도우미(로봇으로 만든다). */
    private RealKeys keys;

    /** Where the user's pointer was before the first real mouse step, put back at the end / 첫 실제 마우스 단계 전의 사용자 포인터 위치(끝에 되돌린다). */
    private Point pointerHome;

    /**
     * Prevents instantiation from outside; use {@link #main(String[])}.
     * <p>
     * 바깥에서 만들지 못하게 한다. {@link #main(String[])} 을 쓴다.
     */
    private UiCheck() {
    }

    /**
     * Runs the steps and exits with the code described in the class comment.
     * <p>
     * 단계를 실행하고 클래스 설명의 종료 코드로 끝낸다.
     *
     * @param args the command line / 명령행
     */
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("[ERROR] no display available (headless)");
            System.exit(3);
            return;
        }
        UiCheck check = new UiCheck();
        int code;
        try {
            code = check.run(args);
        } catch (IllegalArgumentException e) {
            System.err.println("[ERROR] " + e.getMessage());
            code = 2;
        } catch (Exception e) {
            System.err.println("[ERROR] " + e);
            e.printStackTrace();
            code = 1;
        }
        check.close();
        System.exit(code);
    }

    /**
     * Parses the options, creates the window and runs the steps in order.
     * <p>
     * 옵션을 해석하고 창을 만든 뒤 단계를 차례로 실행한다.
     *
     * @param args the command line / 명령행
     * @return the exit code / 종료 코드
     * @throws Exception if a step fails unexpectedly / 단계가 예기치 않게 실패한 경우
     */
    private int run(String[] args) throws Exception {
        int width = 800;
        int height = 600;
        List<String> steps = new ArrayList<String>();
        // 명령행을 앞에서부터 읽어 창 옵션과 단계로 나눈다.
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if ("--out".equals(a)) {
                outDir = new File(need(args, ++i, "--out"));
            } else if ("--size".equals(a)) {
                String[] wh = need(args, ++i, "--size").split("x");
                if (wh.length != 2) {
                    throw new IllegalArgumentException("--size needs WxH");
                }
                width = Integer.parseInt(wh[0]);
                height = Integer.parseInt(wh[1]);
            } else if ("--realmouse".equals(a)) {
                realMouse = true;
            } else {
                steps.add(a);
                // 값을 받는 단계는 그 값도 같은 목록에 넣어 둔다.
                int values = valuesOf(a);
                for (int k = 0; k < values; k++) {
                    steps.add(need(args, ++i, a));
                }
            }
        }
        if (outDir == null) {
            throw new IllegalArgumentException("--out DIR is required");
        }
        if (!outDir.isDirectory() && !outDir.mkdirs()) {
            throw new IllegalArgumentException("cannot create " + outDir);
        }
        // 예제 창을 쓰는 경우에는 자체 창을 만들지 않고, 그렇지 않으면 이 도구의 창을 연다.
        if (steps.contains("--example") || steps.contains("--window")) {
            robot = new Robot();
        } else {
            openWindow(width, height);
        }
        // 단계 목록을 차례로 실행한다.
        for (int i = 0; i < steps.size(); i++) {
            String s = steps.get(i);
            if ("--example".equals(s)) {
                openExample(steps.get(++i));
            } else if ("--window".equals(s)) {
                openPlainWindow(steps.get(++i));
            } else if ("--dialogshot".equals(s)) {
                dialogShot(steps.get(++i));
            } else if ("--wheel".equals(s)) {
                wheel(Integer.parseInt(steps.get(++i)));
            } else if ("--press".equals(s)) {
                press(steps.get(++i));
            } else if ("--serve".equals(s)) {
                serve();
            } else if ("--key".equals(s)) {
                key(steps.get(++i));
            } else if ("--type".equals(s)) {
                type(steps.get(++i));
            } else if ("--hangul".equals(s)) {
                hangul("on".equals(steps.get(++i)));
            } else if ("--hover".equals(s)) {
                int x = Integer.parseInt(steps.get(++i));
                int y = Integer.parseInt(steps.get(++i));
                hover(x, y);
            } else if ("--drag".equals(s)) {
                int x1 = Integer.parseInt(steps.get(++i));
                int y1 = Integer.parseInt(steps.get(++i));
                int x2 = Integer.parseInt(steps.get(++i));
                int y2 = Integer.parseInt(steps.get(++i));
                drag(x1, y1, x2, y2);
            } else if ("--mousedown".equals(s)) {
                int x = Integer.parseInt(steps.get(++i));
                int y = Integer.parseInt(steps.get(++i));
                mouseDown(x, y);
            } else if ("--mouseup".equals(s)) {
                mouseUp();
            } else if ("--realclick".equals(s)) {
                int x = Integer.parseInt(steps.get(++i));
                int y = Integer.parseInt(steps.get(++i));
                click(x, y, true);
            } else if ("--clickbutton".equals(s)) {
                clickButton(steps.get(++i));
            } else if ("--log".equals(s)) {
                printStorage();
            } else if ("--expect-storage".equals(s)) {
                String k = steps.get(++i);
                expectStorage(k, steps.get(++i));
            } else if ("--hyperlinks".equals(s)) {
                listenHyperlinks();
            } else if ("--clipboard".equals(s)) {
                printClipboard();
            } else if ("--focusinfo".equals(s)) {
                printFocus();
            } else if ("--expect-file".equals(s)) {
                expectFile(steps.get(++i));
            } else if ("--url".equals(s)) {
                navigate(withBase(steps.get(++i)));
            } else if ("--shot".equals(s)) {
                shot(steps.get(++i));
            } else if ("--scroll".equals(s)) {
                scrollTo(Integer.parseInt(steps.get(++i)));
            } else if ("--click".equals(s)) {
                int x = Integer.parseInt(steps.get(++i));
                int y = Integer.parseInt(steps.get(++i));
                click(x, y, realMouse);
            } else if ("--wait".equals(s)) {
                Thread.sleep(Integer.parseInt(steps.get(++i)));
            } else if ("--expect-title".equals(s)) {
                expectTitle(steps.get(++i));
            } else if ("--back".equals(s)) {
                invoke("back");
            } else if ("--forward".equals(s)) {
                invoke("forward");
            } else if ("--refresh".equals(s)) {
                invoke("refresh");
            } else if ("--dispose".equals(s)) {
                disposeAll();
            } else {
                throw new IllegalArgumentException("unknown step " + s);
            }
        }
        return failures == 0 ? 0 : 1;
    }

    /**
     * Returns how many values a step takes.
     * <p>
     * 단계가 받는 값의 개수를 반환한다.
     *
     * @param step the step / 단계
     * @return the number of values / 값의 개수
     */
    private static int valuesOf(String step) {
        if ("--drag".equals(step)) {
            return 4;
        }
        if ("--click".equals(step) || "--realclick".equals(step) || "--mousedown".equals(step) || "--hover".equals(step) || "--expect-storage".equals(step)) {
            return 2;
        }
        if ("--url".equals(step) || "--shot".equals(step) || "--scroll".equals(step) || "--wait".equals(step) || "--expect-title".equals(step)
                || "--example".equals(step) || "--press".equals(step) || "--window".equals(step) || "--dialogshot".equals(step) || "--wheel".equals(step)
                || "--key".equals(step) || "--type".equals(step) || "--hangul".equals(step) || "--clickbutton".equals(step)
                || "--expect-file".equals(step)) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns the argument at an index or fails with a usage error.
     * <p>
     * 인덱스의 인수를 반환하거나 사용법 오류로 실패한다.
     *
     * @param args  the arguments / 인수
     * @param index the index / 인덱스
     * @param name  the option that needs it / 그것을 필요로 하는 옵션
     * @return the argument / 인수
     */
    private static String need(String[] args, int index, String name) {
        if (index >= args.length) {
            throw new IllegalArgumentException(name + " needs a value");
        }
        return args[index];
    }

    /**
     * Creates and shows the window on the event dispatch thread.
     * <p>
     * 이벤트 디스패치 스레드에서 창을 만들어 보여 준다.
     *
     * @param width  the content width / 내용 폭
     * @param height the content height / 내용 높이
     * @throws Exception if the window cannot be shown / 창을 보일 수 없는 경우
     */
    private void openWindow(final int width, final int height) throws Exception {
        robot = new Robot();
        runOnEdt(new Runnable() {
            /** Builds the window. / 창을 만든다. */
            public void run() {
                panel = new SwingWebPanel();
                scroll = new JScrollPane(panel);
                frame = new JFrame("uicheck");
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.getContentPane().add(scroll);
                scroll.setPreferredSize(new Dimension(width, height));
                frame.pack();
                frame.setLocation(60, 60);
                frame.setAlwaysOnTop(true);
                frame.setVisible(true);
                frame.toFront();
            }
        });
        // 창이 실제로 화면에 나타날 때까지 잠깐 기다린다.
        Thread.sleep(800);
        robot.waitForIdle();
    }

    /**
     * Starts the <code>main</code> method of an example class (<code>org.duckdns.hjow.swingwebpanel.examples.NAME</code>), waits for its window and uses that window,
     * its panel and its scroll pane for the following steps. The example classes must be on the class path (see the AGENTS.md of this tool).
     * <p>
     * 예제 클래스(<code>org.duckdns.hjow.swingwebpanel.examples.NAME</code>)의 <code>main</code> 을 실행하고 그 창이 나타나기를 기다린 뒤, 이어지는 단계에서 그 창과 패널과
     * 스크롤 창을 쓴다. 예제 클래스는 클래스패스에 있어야 한다(이 도구의 AGENTS.md 참고).
     *
     * @param name the simple class name / 클래스 단순 이름
     * @throws Exception if the example or its window cannot be found / 예제나 그 창을 찾지 못한 경우
     */
    private void openExample(String name) throws Exception {
        Class<?> type = Class.forName("org.duckdns.hjow.swingwebpanel.examples." + name);
        type.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        long end = System.currentTimeMillis() + 15000;
        // 예제 창이 나타날 때까지 제한 시간 안에서 반복해서 찾는다.
        while (System.currentTimeMillis() < end && panel == null) {
            Thread.sleep(100);
            runOnEdt(new Runnable() {
                /** Looks for a showing frame that contains a browser panel. / 브라우저 패널이 든, 보이는 프레임을 찾는다. */
                public void run() {
                    java.awt.Frame[] frames = java.awt.Frame.getFrames();
                    // 열려 있는 모든 프레임을 훑는다.
                    for (int i = 0; i < frames.length; i++) {
                        if (frames[i] instanceof JFrame && frames[i].isShowing()) {
                            SwingWebPanel found = findPanel(frames[i]);
                            if (found != null) {
                                frame = (JFrame) frames[i];
                                panel = found;
                                scroll = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, found);
                                frame.setAlwaysOnTop(true);
                                frame.toFront();
                                return;
                            }
                        }
                    }
                }
            });
        }
        if (panel == null) {
            throw new IllegalStateException("no example window with a SwingWebPanel appeared");
        }
        // 창이 완전히 나타나고 입력을 받을 수 있게 되기를 잠깐 더 기다린다.
        Thread.sleep(1200);
        robot.waitForIdle();
        waitSettled();
    }

    /**
     * Starts the <code>main</code> method of an example class that shows a window without a browser panel (for example the download popup
     * example) and uses the first showing frame as the window of the following steps (<code>--shot</code>, <code>--press</code>,
     * <code>--dialogshot</code>).
     * <p>
     * 브라우저 패널 없이 창만 보여 주는 예제 클래스(예: 다운로드 팝업 예제)의 <code>main</code> 을 실행하고, 보이는 첫 프레임을 이어지는 단계
     * (<code>--shot</code>, <code>--press</code>, <code>--dialogshot</code>)의 창으로 쓴다.
     *
     * @param name the simple class name / 클래스 단순 이름
     * @throws Exception if no window appears / 창이 나타나지 않은 경우
     */
    private void openPlainWindow(String name) throws Exception {
        Class<?> type = Class.forName("org.duckdns.hjow.swingwebpanel.examples." + name);
        type.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        long end = System.currentTimeMillis() + 15000;
        // 예제 창이 나타날 때까지 제한 시간 안에서 반복해서 찾는다.
        while (System.currentTimeMillis() < end && frame == null) {
            Thread.sleep(100);
            runOnEdt(new Runnable() {
                /** Looks for the first showing frame. / 보이는 첫 프레임을 찾는다. */
                public void run() {
                    java.awt.Frame[] frames = java.awt.Frame.getFrames();
                    // 열려 있는 모든 프레임에서 보이는 JFrame 을 찾는다.
                    for (int i = 0; i < frames.length && frame == null; i++) {
                        if (frames[i] instanceof JFrame && frames[i].isShowing()) {
                            frame = (JFrame) frames[i];
                            frame.setAlwaysOnTop(true);
                            frame.toFront();
                        }
                    }
                }
            });
        }
        if (frame == null) {
            throw new IllegalStateException("no example window appeared");
        }
        Thread.sleep(1200);
        robot.waitForIdle();
    }

    /**
     * Saves a screenshot of the content of the first showing dialog (for example the download popup) as <code>NAME.png</code>. It waits up to
     * ten seconds for the dialog to appear.
     * <p>
     * 보이는 첫 대화상자(예: 다운로드 팝업)의 내용을 <code>NAME.png</code> 로 저장한다. 대화상자가 나타나기를 최대 10초 기다린다.
     *
     * @param name the file name without extension / 확장자 없는 파일 이름
     * @throws Exception if no dialog appears / 대화상자가 나타나지 않은 경우
     */
    private void dialogShot(String name) throws Exception {
        final Rectangle[] bounds = new Rectangle[1];
        long end = System.currentTimeMillis() + 10000;
        // 대화상자가 나타날 때까지 제한 시간 안에서 반복해서 찾는다.
        while (System.currentTimeMillis() < end && bounds[0] == null) {
            Thread.sleep(150);
            runOnEdt(new Runnable() {
                /** Reads the content bounds of the first showing dialog. / 보이는 첫 대화상자의 내용 경계를 읽는다. */
                public void run() {
                    java.awt.Window[] windows = java.awt.Window.getWindows();
                    // 열려 있는 창 중 보이는 대화상자를 찾는다.
                    for (int i = 0; i < windows.length && bounds[0] == null; i++) {
                        if (windows[i] instanceof javax.swing.JDialog && windows[i].isShowing()) {
                            javax.swing.JDialog dialog = (javax.swing.JDialog) windows[i];
                            dialog.toFront();
                            java.awt.Point origin = dialog.getRootPane().getLocationOnScreen();
                            bounds[0] = new Rectangle(origin.x, origin.y, dialog.getRootPane().getWidth(), dialog.getRootPane().getHeight());
                        }
                    }
                }
            });
        }
        if (bounds[0] == null) {
            throw new IllegalStateException("no dialog appeared");
        }
        Thread.sleep(300);
        robot.waitForIdle();
        BufferedImage image = robot.createScreenCapture(bounds[0]);
        File file = new File(outDir, name + ".png");
        ImageIO.write(image, "png", file);
        System.out.println("[dialogshot] " + file.getPath() + " " + image.getWidth() + "x" + image.getHeight());
    }

    /**
     * Turns the real mouse wheel over the page by a number of notches (positive = down) with the Robot, and prints the vertical scroll position
     * before and after. The pointer is put back afterwards.
     * <p>
     * Robot 으로 페이지 위에서 실제 마우스 휠을 몇 칸(양수 = 아래) 돌리고 돌리기 전후의 세로 스크롤 위치를 출력한다. 끝나면 포인터를 되돌린다.
     *
     * @param notches the notches / 칸 수
     * @throws Exception if interrupted / 중단된 경우
     */
    private void wheel(final int notches) throws Exception {
        final Point[] center = new Point[1];
        final int[] before = new int[1];
        runOnEdt(new Runnable() {
            /** Reads the screen point of the viewport center and the scroll position. / 보이는 영역 가운데의 화면 좌표와 스크롤 위치를 읽는다. */
            public void run() {
                frame.toFront();
                Point p = new Point(scroll.getViewport().getWidth() / 2, scroll.getViewport().getHeight() / 2);
                SwingUtilities.convertPointToScreen(p, scroll.getViewport());
                center[0] = p;
                before[0] = scroll.getVerticalScrollBar().getValue();
            }
        });
        Point pointer = java.awt.MouseInfo.getPointerInfo().getLocation();
        robot.mouseMove(center[0].x, center[0].y);
        robot.waitForIdle();
        Thread.sleep(400);
        robot.mouseWheel(notches);
        robot.waitForIdle();
        Thread.sleep(500);
        robot.mouseMove(pointer.x, pointer.y);
        final int[] after = new int[1];
        runOnEdt(new Runnable() {
            /** Reads the scroll position again. / 스크롤 위치를 다시 읽는다. */
            public void run() {
                after[0] = scroll.getVerticalScrollBar().getValue();
            }
        });
        System.out.println("[wheel] notches=" + notches + " scroll " + before[0] + " -> " + after[0]);
    }

    /**
     * Finds a browser panel in a container tree.
     * <p>
     * 컨테이너 트리에서 브라우저 패널을 찾는다.
     *
     * @param parent the container to search / 찾을 컨테이너
     * @return the panel or null / 패널. 없으면 null
     */
    private static SwingWebPanel findPanel(java.awt.Container parent) {
        Component[] children = parent.getComponents();
        // 자식을 차례로 보면서 패널이면 반환하고 컨테이너면 안쪽을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof SwingWebPanel) {
                return (SwingWebPanel) children[i];
            }
            if (children[i] instanceof java.awt.Container) {
                SwingWebPanel inner = findPanel((java.awt.Container) children[i]);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * Presses a button of the example window (an <code>AbstractButton</code> whose text is the given text) and waits until the page has settled.
     * <p>
     * 예제 창의 단추(글이 주어진 글과 같은 <code>AbstractButton</code>)를 누르고 페이지가 안정될 때까지 기다린다.
     *
     * @param text the button text / 단추 글
     * @throws Exception if there is no such button / 그런 단추가 없는 경우
     */
    private void press(final String text) throws Exception {
        final boolean[] found = new boolean[1];
        runOnEdt(new Runnable() {
            /** Clicks the button. / 단추를 누른다. */
            public void run() {
                // 예제 창과 그 밖에 열려 있는 모든 창(팝업 대화상자 포함)에서 단추를 찾는다.
                java.awt.Window[] windows = java.awt.Window.getWindows();
                for (int i = 0; i < windows.length && !found[0]; i++) {
                    if (!windows[i].isShowing()) {
                        continue;
                    }
                    javax.swing.AbstractButton button = findButton(windows[i], text);
                    if (button != null) {
                        button.doClick();
                        found[0] = true;
                    }
                }
            }
        });
        if (!found[0]) {
            throw new IllegalStateException("no button with the text " + text);
        }
        Thread.sleep(300);
        if (panel != null) {
            waitSettled();
        }
    }

    /**
     * Finds a button with a text in a container tree.
     * <p>
     * 컨테이너 트리에서 글이 같은 단추를 찾는다.
     *
     * @param parent the container to search / 찾을 컨테이너
     * @param text   the button text / 단추 글
     * @return the button or null / 단추. 없으면 null
     */
    private static javax.swing.AbstractButton findButton(java.awt.Container parent, String text) {
        Component[] children = parent.getComponents();
        // 자식을 차례로 보면서 글이 같은 단추를 찾고 컨테이너면 안쪽을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof javax.swing.AbstractButton && text.equals(((javax.swing.AbstractButton) children[i]).getText())) {
                return (javax.swing.AbstractButton) children[i];
            }
            if (children[i] instanceof java.awt.Container) {
                javax.swing.AbstractButton inner = findButton((java.awt.Container) children[i], text);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * Runs a task on the event dispatch thread and waits for it.
     * <p>
     * 작업을 이벤트 디스패치 스레드에서 실행하고 끝나기를 기다린다.
     *
     * @param task the task / 작업
     * @throws InterruptedException if interrupted / 중단된 경우
     * @throws InvocationTargetException if the task throws / 작업이 예외를 던진 경우
     */
    private static void runOnEdt(Runnable task) throws InterruptedException, InvocationTargetException {
        if (EventQueue.isDispatchThread()) {
            task.run();
        } else {
            EventQueue.invokeAndWait(task);
        }
    }

    /**
     * Loads a page and waits until it has finished.
     * <p>
     * 페이지를 불러오고 끝날 때까지 기다린다.
     *
     * @param url the address / 주소
     * @throws Exception if the page does not finish in time / 제때 끝나지 않은 경우
     */
    private void navigate(final String url) throws Exception {
        runOnEdt(new Runnable() {
            /** Starts the navigation. / 이동을 시작한다. */
            public void run() {
                panel.setPage(url);
            }
        });
        waitSettled();
    }

    /**
     * Calls a no-argument navigation method of the panel (back, forward, refresh) and waits.
     * <p>
     * 패널의 인수 없는 이동 메서드(back, forward, refresh)를 부르고 기다린다.
     *
     * @param method the method name / 메서드 이름
     * @throws Exception if the page does not finish in time / 제때 끝나지 않은 경우
     */
    private void invoke(final String method) throws Exception {
        runOnEdt(new Runnable() {
            /** Calls the method. / 메서드를 부른다. */
            public void run() {
                if ("back".equals(method)) {
                    panel.back();
                } else if ("forward".equals(method)) {
                    panel.forward();
                } else {
                    panel.refresh();
                }
            }
        });
        waitSettled();
    }

    /**
     * Waits until the progress has been 100 for {@link #SETTLE_MS} milliseconds.
     * <p>
     * 진행률이 {@link #SETTLE_MS} 밀리초 동안 100 일 때까지 기다린다.
     *
     * @throws Exception if the timeout passes / 제한 시간이 지난 경우
     */
    private void waitSettled() throws Exception {
        long end = System.currentTimeMillis() + LOAD_TIMEOUT_MS;
        long since = -1;
        // 제한 시간까지 진행률을 반복해서 살핀다.
        while (System.currentTimeMillis() < end) {
            Thread.sleep(50);
            if (panel.getProgress() == 100) {
                if (since < 0) {
                    since = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - since >= SETTLE_MS) {
                    return;
                }
            } else {
                since = -1;
            }
        }
        throw new IllegalStateException("page did not finish loading: " + panel.getPage());
    }

    /**
     * Scrolls the viewport to a vertical offset.
     * <p>
     * 보이는 영역을 세로 위치로 스크롤한다.
     *
     * @param y the offset in pixels / 위치(화소)
     * @throws Exception if interrupted / 중단된 경우
     */
    private void scrollTo(final int y) throws Exception {
        runOnEdt(new Runnable() {
            /** Moves the vertical scroll bar. / 세로 스크롤 막대를 움직인다. */
            public void run() {
                scroll.getVerticalScrollBar().setValue(y);
            }
        });
        Thread.sleep(300);
        robot.waitForIdle();
    }

    /**
     * Clicks at a point of the panel (relative to the visible part of the panel) with a pressed, released and clicked mouse event.
     * <p>
     * 패널 위의 한 점(패널의 보이는 부분 기준)을 눌림, 뗌, 클릭 마우스 이벤트로 클릭한다.
     *
     * @param x the x coordinate in the viewport / 보이는 영역의 x 좌표
     * @param y the y coordinate in the viewport / 보이는 영역의 y 좌표
     * @param real true to use the real mouse of the system through the robot / 로봇으로 시스템의 실제 마우스를 쓰려면 true
     * @throws Exception if interrupted / 중단된 경우
     */
    private void click(final int x, final int y, boolean real) throws Exception {
        if (real) {
            final Point[] screen = new Point[1];
            runOnEdt(new Runnable() {
                /** Converts the viewport point to the screen. / 보이는 영역의 점을 화면 좌표로 바꾼다. */
                public void run() {
                    Point p = new Point(x, y);
                    SwingUtilities.convertPointToScreen(p, scroll.getViewport());
                    screen[0] = p;
                }
            });
            // 사용자의 마우스 위치를 기억했다가 클릭이 끝나면 되돌려 놓는다.
            Point before = java.awt.MouseInfo.getPointerInfo().getLocation();
            // 먼저 포인터를 올려 두어 컴포넌트가 이동·진입 이벤트를 받은 뒤에 누른다(창이 방금 앞으로 나온 경우에도 첫 클릭을 놓치지 않도록).
            robot.mouseMove(screen[0].x, screen[0].y);
            robot.waitForIdle();
            Thread.sleep(400);
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_MASK);
            Thread.sleep(80);
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);
            robot.waitForIdle();
            Thread.sleep(200);
            robot.mouseMove(before.x, before.y);
        } else {
            runOnEdt(new Runnable() {
                /** Posts the mouse events into the AWT event queue. / 마우스 이벤트를 AWT 이벤트 큐에 넣는다. */
                public void run() {
                    Point view = scroll.getViewport().getViewPosition();
                    int px = x + view.x;
                    int py = y + view.y;
                    long now = System.currentTimeMillis();
                    Component c = panel;
                    c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_MOVED, now, 0, px, py, 0, false));
                    c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_PRESSED, now + 1, MouseEvent.BUTTON1_MASK, px, py, 1, false, MouseEvent.BUTTON1));
                    c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_RELEASED, now + 2, MouseEvent.BUTTON1_MASK, px, py, 1, false, MouseEvent.BUTTON1));
                    c.dispatchEvent(new MouseEvent(c, MouseEvent.MOUSE_CLICKED, now + 3, MouseEvent.BUTTON1_MASK, px, py, 1, false, MouseEvent.BUTTON1));
                }
            });
        }
        Thread.sleep(300);
        waitSettled();
    }

    /**
     * Starts the built-in HTTP server ({@link EchoServer}) on a free loop-back port. It serves the pages of <code>tools/uicheck/pages</code>, and its
     * address replaces <code>{base}</code> in the values of later <code>--url</code> steps.
     * <p>
     * 내장 HTTP 서버({@link EchoServer})를 비어 있는 루프백 포트에서 시작한다. <code>tools/uicheck/pages</code> 의 페이지를 내주며, 그 주소가 뒤따르는
     * <code>--url</code> 단계 값의 <code>{base}</code> 를 대신한다.
     *
     * @throws Exception if the socket cannot be opened / 소켓을 열 수 없는 경우
     */
    private void serve() throws Exception {
        server = new EchoServer(new File("tools/uicheck/pages"));
        server.start();
        System.out.println("[serve] " + server.base());
    }

    /**
     * Replaces <code>{base}</code> in an address with the address of the built-in server.
     * <p>
     * 주소의 <code>{base}</code> 를 내장 서버의 주소로 바꾼다.
     *
     * @param url the address / 주소
     * @return the address with the server address / 서버 주소를 넣은 주소
     */
    private String withBase(String url) {
        if (url.indexOf("{base}") < 0) {
            return url;
        }
        if (server == null) {
            throw new IllegalArgumentException("{base} needs a --serve step before it");
        }
        return url.replace("{base}", server.base());
    }

    /**
     * Returns the helper that presses real keys, creating it on first use.
     * <p>
     * 실제 키를 누르는 도우미를 반환한다. 처음 쓸 때 만든다.
     *
     * @return the helper / 도우미
     */
    private RealKeys keys() {
        if (keys == null) {
            keys = new RealKeys(robot);
        }
        return keys;
    }

    /**
     * Presses a key specification with real key strokes (see {@link RealKeys#press(String)}). The keys go to the window that has the focus, which
     * may be a dialog of the page.
     * <p>
     * 키 지정을 실제 키 입력으로 누른다({@link RealKeys#press(String)} 참고). 키는 포커스를 가진 창으로 가며, 페이지의 대화상자일 수도 있다.
     *
     * @param spec the key specification / 키 지정
     * @throws Exception if interrupted / 중단된 경우
     */
    private void key(String spec) throws Exception {
        keys().press(spec);
        System.out.println("[key] " + spec);
        afterInput();
    }

    /**
     * Types ASCII text with real key strokes (see {@link RealKeys#type(String)}). With the Korean input method in Hangul mode, the letters are
     * composed into Hangul by the input method of the system (for example <code>gksrmf</code> becomes the word for "Hangul").
     * <p>
     * ASCII 글을 실제 키 입력으로 친다({@link RealKeys#type(String)} 참고). 한국어 입력기가 한글 모드이면 시스템 입력기가 글자를 한글로 조합한다
     * (예: <code>gksrmf</code> 는 "한글").
     *
     * @param text the text / 글
     * @throws Exception if interrupted / 중단된 경우
     */
    private void type(String text) throws Exception {
        keys().type(text);
        System.out.println("[type] " + text);
        afterInput();
    }

    /**
     * Lets the page handle real input and, if the input started a navigation, waits until the page has settled (a page that does not settle is
     * reported as a warning, not as a failure).
     * <p>
     * 페이지가 실제 입력을 처리하게 하고, 입력이 이동을 시작했으면 페이지가 안정될 때까지 기다린다(안정되지 않으면 실패가 아닌 경고로 알린다).
     *
     * @throws Exception if interrupted / 중단된 경우
     */
    private void afterInput() throws Exception {
        robot.waitForIdle();
        Thread.sleep(250);
        if (panel != null && frame != null) {
            try {
                waitSettled();
            } catch (IllegalStateException e) {
                System.out.println("[WARN] " + e.getMessage());
            }
        }
    }

    /**
     * Switches the input method of the component that has the keyboard focus to Korean Hangul mode (<code>on</code>) or to Latin mode
     * (<code>off</code>) through the public <code>java.awt.im.InputContext</code> API. It stands for the Hangul/English key of a Korean keyboard,
     * which the JDK 6 robot cannot press (<code>VK_KANA</code> is refused); the composition itself then comes from the input method of the system.
     * <p>
     * 키보드 포커스를 가진 컴포넌트의 입력기를 공개 API <code>java.awt.im.InputContext</code> 로 한글 모드(<code>on</code>) 또는 라틴 모드(<code>off</code>)로
     * 바꾼다. 한국어 자판의 한/영 키를 대신한다(JDK 6 의 로봇은 <code>VK_KANA</code> 를 거부해 그 키를 누를 수 없다). 조합 자체는 시스템 입력기가 한다.
     *
     * @param on true for Hangul mode / 한글 모드이면 true
     * @throws Exception if interrupted / 중단된 경우
     */
    private void hangul(final boolean on) throws Exception {
        final String[] info = new String[1];
        runOnEdt(new Runnable() {
            /** Changes the mode of the input context. / 입력 문맥의 모드를 바꾼다. */
            public void run() {
                Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                if (owner == null) {
                    info[0] = "no focus owner";
                    return;
                }
                java.awt.im.InputContext ic = owner.getInputContext();
                if (ic == null) {
                    info[0] = "no input context on " + owner.getClass().getName();
                    return;
                }
                ic.selectInputMethod(java.util.Locale.KOREA);
                Character.Subset subset = on ? Character.UnicodeBlock.HANGUL_SYLLABLES : Character.UnicodeBlock.BASIC_LATIN;
                ic.setCharacterSubsets(new Character.Subset[] {subset});
                String enabled;
                try {
                    enabled = String.valueOf(ic.isCompositionEnabled());
                } catch (UnsupportedOperationException e) {
                    enabled = "unknown";
                }
                info[0] = "owner=" + owner.getClass().getName() + " locale=" + ic.getLocale() + " compositionEnabled=" + enabled;
            }
        });
        System.out.println("[hangul] " + (on ? "on " : "off ") + info[0]);
        Thread.sleep(200);
    }

    /**
     * Converts a point of the visible part of the panel to the screen.
     * <p>
     * 패널의 보이는 영역 기준 점을 화면 좌표로 바꾼다.
     *
     * @param x the x coordinate in the viewport / 보이는 영역의 x 좌표
     * @param y the y coordinate in the viewport / 보이는 영역의 y 좌표
     * @return the screen point / 화면 좌표
     * @throws Exception if interrupted / 중단된 경우
     */
    private Point toScreen(final int x, final int y) throws Exception {
        final Point[] screen = new Point[1];
        runOnEdt(new Runnable() {
            /** Converts the point. / 점을 바꾼다. */
            public void run() {
                frame.toFront();
                Point p = new Point(x, y);
                SwingUtilities.convertPointToScreen(p, scroll.getViewport());
                screen[0] = p;
            }
        });
        return screen[0];
    }

    /**
     * Remembers where the user's pointer is before the first real mouse step, so that {@link #close()} can put it back.
     * <p>
     * 첫 실제 마우스 단계 전에 사용자의 포인터 위치를 기억해 {@link #close()} 가 되돌릴 수 있게 한다.
     */
    private void rememberPointer() {
        if (pointerHome == null) {
            pointerHome = java.awt.MouseInfo.getPointerInfo().getLocation();
        }
    }

    /**
     * Moves the real pointer over a point of the page and leaves it there (for hover styles and link entered events). The pointer is put back when
     * the tool ends.
     * <p>
     * 실제 포인터를 페이지의 한 점 위로 옮기고 그대로 둔다(호버 스타일과 링크 진입 이벤트용). 포인터는 도구가 끝날 때 되돌린다.
     *
     * @param x the x coordinate in the viewport / 보이는 영역의 x 좌표
     * @param y the y coordinate in the viewport / 보이는 영역의 y 좌표
     * @throws Exception if interrupted / 중단된 경우
     */
    private void hover(int x, int y) throws Exception {
        Point p = toScreen(x, y);
        rememberPointer();
        robot.mouseMove(p.x, p.y);
        robot.waitForIdle();
        Thread.sleep(500);
        System.out.println("[hover] " + x + "," + y);
    }

    /**
     * Moves the real pointer to a point of the page and presses the left button without releasing it (for <code>:active</code> styles); a later
     * <code>--mouseup</code> releases it.
     * <p>
     * 실제 포인터를 페이지의 한 점으로 옮기고 왼쪽 단추를 누른 채 둔다(<code>:active</code> 스타일용). 뒤의 <code>--mouseup</code> 이 뗀다.
     *
     * @param x the x coordinate in the viewport / 보이는 영역의 x 좌표
     * @param y the y coordinate in the viewport / 보이는 영역의 y 좌표
     * @throws Exception if interrupted / 중단된 경우
     */
    private void mouseDown(int x, int y) throws Exception {
        Point p = toScreen(x, y);
        rememberPointer();
        robot.mouseMove(p.x, p.y);
        robot.waitForIdle();
        Thread.sleep(300);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        Thread.sleep(400);
        System.out.println("[mousedown] " + x + "," + y);
    }

    /**
     * Releases the real left mouse button.
     * <p>
     * 실제 왼쪽 마우스 단추를 뗀다.
     *
     * @throws Exception if interrupted / 중단된 경우
     */
    private void mouseUp() throws Exception {
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        Thread.sleep(300);
        System.out.println("[mouseup]");
    }

    /**
     * Drags with the real left mouse button from one point of the page to another in small steps (for text selection).
     * <p>
     * 실제 왼쪽 마우스 단추로 페이지의 한 점에서 다른 점까지 조금씩 끈다(텍스트 선택용).
     *
     * @param x1 the start x in the viewport / 보이는 영역의 시작 x
     * @param y1 the start y in the viewport / 보이는 영역의 시작 y
     * @param x2 the end x in the viewport / 보이는 영역의 끝 x
     * @param y2 the end y in the viewport / 보이는 영역의 끝 y
     * @throws Exception if interrupted / 중단된 경우
     */
    private void drag(int x1, int y1, int x2, int y2) throws Exception {
        Point a = toScreen(x1, y1);
        Point b = toScreen(x2, y2);
        rememberPointer();
        robot.mouseMove(a.x, a.y);
        robot.waitForIdle();
        Thread.sleep(300);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_MASK);
        Thread.sleep(120);
        int steps = 12;
        // 끄는 길을 같은 간격의 작은 움직임으로 나눈다.
        for (int i = 1; i <= steps; i++) {
            robot.mouseMove(a.x + (b.x - a.x) * i / steps, a.y + (b.y - a.y) * i / steps);
            Thread.sleep(40);
        }
        robot.waitForIdle();
        Thread.sleep(150);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        Thread.sleep(300);
        System.out.println("[drag] " + x1 + "," + y1 + " -> " + x2 + "," + y2);
    }

    /**
     * Clicks with the real mouse on the first showing button whose text starts with a prefix, in any window (for example the approve button of a
     * file chooser or a button of a confirmation dialog). It waits up to ten seconds for the button.
     * <p>
     * 어느 창이든 글이 접두사로 시작하는 보이는 첫 단추를 실제 마우스로 누른다(예: 파일 선택기의 승인 단추, 확인 대화상자의 단추). 단추를 최대 10초 기다린다.
     *
     * @param prefix the text prefix / 글의 접두사
     * @throws Exception if no button appears / 단추가 나타나지 않은 경우
     */
    private void clickButton(final String prefix) throws Exception {
        final Point[] center = new Point[1];
        final String[] label = new String[1];
        long end = System.currentTimeMillis() + 10000;
        // 단추가 나타날 때까지 제한 시간 안에서 반복해서 찾는다.
        while (center[0] == null && System.currentTimeMillis() < end) {
            runOnEdt(new Runnable() {
                /** Looks for the button in all showing windows. / 보이는 모든 창에서 단추를 찾는다. */
                public void run() {
                    java.awt.Window[] windows = java.awt.Window.getWindows();
                    // 보이는 창마다 접두사가 맞는 단추를 찾는다.
                    for (int i = 0; i < windows.length && center[0] == null; i++) {
                        if (!windows[i].isShowing()) {
                            continue;
                        }
                        javax.swing.AbstractButton b = findButtonPrefix(windows[i], prefix);
                        if (b != null) {
                            Point p = b.getLocationOnScreen();
                            center[0] = new Point(p.x + b.getWidth() / 2, p.y + b.getHeight() / 2);
                            label[0] = b.getText();
                        }
                    }
                }
            });
            if (center[0] == null) {
                Thread.sleep(200);
            }
        }
        if (center[0] == null) {
            throw new IllegalStateException("no showing button starting with " + prefix);
        }
        rememberPointer();
        robot.mouseMove(center[0].x, center[0].y);
        robot.waitForIdle();
        Thread.sleep(300);
        robot.mousePress(java.awt.event.InputEvent.BUTTON1_MASK);
        Thread.sleep(80);
        robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);
        robot.waitForIdle();
        Thread.sleep(400);
        System.out.println("[clickbutton] " + label[0]);
    }

    /**
     * Finds a showing button whose text starts with a prefix in a container tree.
     * <p>
     * 컨테이너 트리에서 글이 접두사로 시작하는 보이는 단추를 찾는다.
     *
     * @param parent the container to search / 찾을 컨테이너
     * @param prefix the text prefix / 글의 접두사
     * @return the button or null / 단추. 없으면 null
     */
    private static javax.swing.AbstractButton findButtonPrefix(java.awt.Container parent, String prefix) {
        Component[] children = parent.getComponents();
        // 자식을 차례로 보면서 맞는 단추를 찾고 컨테이너면 안쪽을 찾는다.
        for (int i = 0; i < children.length; i++) {
            if (children[i] instanceof javax.swing.AbstractButton && children[i].isShowing()) {
                String t = ((javax.swing.AbstractButton) children[i]).getText();
                if (t != null && t.startsWith(prefix)) {
                    return (javax.swing.AbstractButton) children[i];
                }
            }
            if (children[i] instanceof java.awt.Container) {
                javax.swing.AbstractButton inner = findButtonPrefix((java.awt.Container) children[i], prefix);
                if (inner != null) {
                    return inner;
                }
            }
        }
        return null;
    }

    /**
     * Prints the session storage and local storage entries of the panel (the check pages write their event logs there). Values that have line
     * breaks are printed one line each.
     * <p>
     * 패널의 sessionStorage 와 localStorage 항목을 찍는다(확인용 페이지가 이벤트 기록을 거기에 쓴다). 줄바꿈이 있는 값은 한 줄씩 찍는다.
     */
    private void printStorage() {
        printEntries("session", panel.getSessionStorage());
        printEntries("local", panel.getLocalStorage());
    }

    /**
     * Prints storage entries.
     * <p>
     * 저장소 항목을 찍는다.
     *
     * @param area    the name of the storage area / 저장소 영역 이름
     * @param entries the entries / 항목
     */
    private static void printEntries(String area, java.util.Collection<org.duckdns.hjow.swingwebpanel.storage.StorageEntry> entries) {
        // 항목마다 이름을 찍고 값은 줄 단위로 찍는다.
        for (org.duckdns.hjow.swingwebpanel.storage.StorageEntry e : entries) {
            String[] lines = e.getValue().split("\n", -1);
            System.out.println("[storage] " + area + " " + e.getKey() + (lines.length == 1 ? " = " + lines[0] : ":"));
            if (lines.length > 1) {
                // 여러 줄 값은 줄마다 표시를 붙인다.
                for (int i = 0; i < lines.length; i++) {
                    System.out.println("[storage]   | " + lines[i]);
                }
            }
        }
    }

    /**
     * Checks that a session or local storage entry has a value.
     * <p>
     * sessionStorage 또는 localStorage 항목이 어떤 값인지 확인한다.
     *
     * @param key   the key / 키
     * @param value the expected value / 기대 값
     */
    private void expectStorage(String key, String value) {
        String actual = null;
        java.util.List<org.duckdns.hjow.swingwebpanel.storage.StorageEntry> all = new ArrayList<org.duckdns.hjow.swingwebpanel.storage.StorageEntry>(panel.getSessionStorage());
        all.addAll(panel.getLocalStorage());
        // 두 저장소의 항목에서 키가 같은 것을 찾는다.
        for (org.duckdns.hjow.swingwebpanel.storage.StorageEntry e : all) {
            if (e.getKey().equals(key)) {
                actual = e.getValue();
                break;
            }
        }
        boolean ok = value.equals(actual);
        System.out.println("[expect-storage] " + (ok ? "ok" : "FAIL") + " " + key + " expected '" + value + "' actual '" + actual + "'");
        if (!ok) {
            failures++;
        }
    }

    /**
     * Registers a hyperlink listener on the panel that prints every event (type, address and description).
     * <p>
     * 모든 이벤트(종류, 주소, 설명)를 찍는 하이퍼링크 리스너를 패널에 등록한다.
     *
     * @throws Exception if interrupted / 중단된 경우
     */
    private void listenHyperlinks() throws Exception {
        runOnEdt(new Runnable() {
            /** Adds the listener. / 리스너를 더한다. */
            public void run() {
                panel.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
                    /** Prints the event. / 이벤트를 찍는다. */
                    public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent e) {
                        System.out.println("[hyperlink] " + e.getEventType() + " url=" + e.getURL() + " desc=" + e.getDescription() + " edt="
                                + EventQueue.isDispatchThread());
                    }
                });
            }
        });
        System.out.println("[hyperlinks] listening");
    }

    /**
     * Prints what the system clipboard holds: its data flavors and the plain text and HTML text if there are any.
     * <p>
     * 시스템 클립보드에 있는 것을 찍는다: 데이터 형식들과, 있으면 평문 글과 HTML 글.
     *
     * @throws Exception if the clipboard cannot be read / 클립보드를 읽을 수 없는 경우
     */
    private void printClipboard() throws Exception {
        java.awt.datatransfer.Clipboard cb = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
        java.awt.datatransfer.Transferable t = null;
        // 다른 프로그램이 클립보드를 잠깐 열고 있을 수 있으므로 몇 번 다시 시도한다.
        for (int i = 0; i < 10 && t == null; i++) {
            try {
                t = cb.getContents(null);
            } catch (IllegalStateException e) {
                Thread.sleep(100);
            }
        }
        if (t == null) {
            System.out.println("[clipboard] empty or unavailable");
            return;
        }
        java.awt.datatransfer.DataFlavor[] flavors = t.getTransferDataFlavors();
        java.util.Set<String> types = new java.util.TreeSet<String>();
        // 형식의 MIME 형식만 모아 찍는다.
        for (int i = 0; i < flavors.length; i++) {
            types.add(flavors[i].getPrimaryType() + "/" + flavors[i].getSubType());
        }
        System.out.println("[clipboard] types " + types);
        if (t.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)) {
            System.out.println("[clipboard] text: " + t.getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor));
        }
        java.awt.datatransfer.DataFlavor html = new java.awt.datatransfer.DataFlavor("text/html;class=java.lang.String");
        if (t.isDataFlavorSupported(html)) {
            System.out.println("[clipboard] html: " + t.getTransferData(html));
        }
    }

    /**
     * Prints which component has the keyboard focus.
     * <p>
     * 키보드 포커스를 가진 컴포넌트를 찍는다.
     *
     * @throws Exception if interrupted / 중단된 경우
     */
    private void printFocus() throws Exception {
        final String[] info = new String[1];
        runOnEdt(new Runnable() {
            /** Reads the focus owner. / 포커스 소유자를 읽는다. */
            public void run() {
                Component owner = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                info[0] = owner == null ? "none" : owner.getClass().getName() + (owner == panel ? " (the panel)" : "");
            }
        });
        System.out.println("[focus] " + info[0]);
    }

    /**
     * Checks that a file exists and is not empty, and prints its size and its first bytes.
     * <p>
     * 파일이 있고 비어 있지 않은지 확인하고 크기와 첫 바이트들을 찍는다.
     *
     * @param path the path / 경로
     * @throws Exception if the file cannot be read / 파일을 읽을 수 없는 경우
     */
    private void expectFile(String path) throws Exception {
        File f = new File(path);
        boolean ok = f.isFile() && f.length() > 0;
        String head = "";
        if (ok) {
            java.io.InputStream in = new java.io.FileInputStream(f);
            try {
                byte[] b = new byte[26];
                int n = in.read(b);
                head = new String(b, 0, Math.max(0, n), "ISO-8859-1");
            } finally {
                in.close();
            }
        }
        System.out.println("[expect-file] " + (ok ? "ok" : "FAIL") + " " + f.getPath() + " size=" + f.length() + " head=" + head);
        if (!ok) {
            failures++;
        }
    }

    /**
     * Saves a screenshot of the window as <code>NAME.png</code> in the output directory and prints the page address and the size of the image.
     * <p>
     * 창의 스크린샷을 출력 디렉터리에 <code>NAME.png</code> 로 저장하고 페이지 주소와 이미지 크기를 출력한다.
     *
     * @param name the file name without extension / 확장자 없는 파일 이름
     * @throws Exception if the image cannot be written / 이미지를 쓸 수 없는 경우
     */
    private void shot(String name) throws Exception {
        final Rectangle[] bounds = new Rectangle[1];
        runOnEdt(new Runnable() {
            /** Reads the window bounds on the screen. / 화면에서의 창 경계를 읽는다. */
            public void run() {
                frame.toFront();
                // 제목 줄과 눈에 보이지 않는 창 테두리를 빼고 내용 영역만 찍는다(주변에 있는 다른 창의 화면 조각이 사진에 섞이지 않도록).
                java.awt.Point origin = frame.getRootPane().getLocationOnScreen();
                bounds[0] = new Rectangle(origin.x, origin.y, frame.getRootPane().getWidth(), frame.getRootPane().getHeight());
            }
        });
        Thread.sleep(250);
        robot.waitForIdle();
        BufferedImage image = robot.createScreenCapture(bounds[0]);
        File file = new File(outDir, name + ".png");
        ImageIO.write(image, "png", file);
        String pageInfo = panel == null ? "" : " page=" + panel.getPage() + " progress=" + panel.getProgress() + " status=" + panel.getStatusText();
        System.out.println("[shot] " + file.getPath() + " " + image.getWidth() + "x" + image.getHeight() + pageInfo);
    }

    /**
     * Checks that the document title that the panel reports (the text of the title element in the received content) contains a text.
     * <p>
     * 패널이 수신한 내용의 title 요소 글에 어떤 글이 들어 있는지 확인한다.
     *
     * @param text the expected text / 기대하는 글
     */
    private void expectTitle(String text) {
        String content = panel.getContent();
        boolean ok = content != null && content.indexOf("<title>") >= 0 && content.indexOf(text) >= 0;
        System.out.println("[expect-title] " + (ok ? "ok" : "FAIL") + " '" + text + "' page=" + panel.getPage());
        if (!ok) {
            failures++;
        }
    }

    /**
     * Disposes the panel and the window on the event dispatch thread and reports it.
     * <p>
     * 이벤트 디스패치 스레드에서 패널과 창을 폐기하고 알린다.
     *
     * @throws Exception if interrupted / 중단된 경우
     */
    private void disposeAll() throws Exception {
        runOnEdt(new Runnable() {
            /** Disposes the component and the window. / 컴포넌트와 창을 폐기한다. */
            public void run() {
                panel.dispose();
                frame.dispose();
                frame = null;
            }
        });
        System.out.println("[dispose] done");
    }

    /**
     * Disposes what is still open when the run ends.
     * <p>
     * 실행이 끝날 때 아직 열려 있는 것을 폐기한다.
     */
    private void close() {
        // 실제 마우스 단계가 움직인 포인터를 처음 자리로 되돌리고 내장 서버를 닫는다.
        if (pointerHome != null && robot != null) {
            robot.mouseMove(pointerHome.x, pointerHome.y);
        }
        if (server != null) {
            server.stop();
        }
        try {
            if (frame != null) {
                runOnEdt(new Runnable() {
                    /** Disposes the window. / 창을 폐기한다. */
                    public void run() {
                        panel.dispose();
                        frame.dispose();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[WARN] close: " + e);
        }
    }
}
