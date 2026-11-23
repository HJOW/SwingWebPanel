package org.duckdns.hjow.swingwebpanel.core.frames;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.storage.StorageEntry;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Helpers shared by the frame tests: creating a component of a known size, painting it offscreen, reading pixels, and waiting for
 * conditions that the document queue and the EDT establish asynchronously. Nothing here is part of the product.
 * <p>
 * 프레임 시험이 함께 쓰는 도우미: 크기를 아는 컴포넌트 만들기, 화면 밖에 그리기, 화소 읽기, 문서 큐와 EDT 가 비동기로 이루는 조건 기다리기. 제품의 일부가
 * 아니다.
 */
public final class FrameTestSupport {

    /**
     * The longest wait for an asynchronous condition in milliseconds.
     * <p>
     * 비동기 조건을 기다리는 최대 시간(밀리초).
     */
    private static final long TIMEOUT = 40000;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private FrameTestSupport() {
    }

    /**
     * Creates a component of a size on the EDT.
     * <p>
     * EDT 에서 크기가 정해진 컴포넌트를 만든다.
     *
     * @param width  the width / 너비
     * @param height the height / 높이
     * @return the component / 컴포넌트
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static SwingWebPanel newPanel(final int width, final int height) throws Exception {
        final SwingWebPanel[] result = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component. / 컴포넌트를 만든다. */
            public void run() {
                result[0] = new SwingWebPanel();
                result[0].setSize(width, height);
            }
        });
        return result[0];
    }

    /**
     * Paints the component into an image on the EDT.
     * <p>
     * EDT 에서 컴포넌트를 이미지에 그린다.
     *
     * @param panel the component / 컴포넌트
     * @return the picture / 그림
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static BufferedImage snapshot(final SwingWebPanel panel) throws Exception {
        final BufferedImage[] result = new BufferedImage[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints offscreen. / 화면 밖에 그린다. */
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
     * Waits until a pixel of the painted component has the color.
     * <p>
     * 그려진 컴포넌트의 화소가 그 색이 될 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param x       x / x
     * @param y       y / y
     * @param rgb     the expected color / 기대한 색
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static void awaitPixel(SwingWebPanel panel, int x, int y, int rgb, String message) throws Exception {
        long end = System.currentTimeMillis() + TIMEOUT;
        int actual;
        // 문서 작업과 EDT 게시가 끝날 때까지 짧게 쉬며 화소를 확인한다.
        do {
            actual = snapshot(panel).getRGB(x, y) & 0xFFFFFF;
            if (actual == rgb) {
                return;
            }
            Thread.sleep(15);
        } while (System.currentTimeMillis() < end);
        Check.equal(Integer.toHexString(rgb), Integer.toHexString(actual), message + " at " + x + "," + y);
    }

    /**
     * Waits until a pixel of the painted component stops having the color.
     * <p>
     * 그려진 컴포넌트의 화소가 그 색이 아니게 될 때까지 기다린다.
     *
     * @param panel   the component / 컴포넌트
     * @param x       x / x
     * @param y       y / y
     * @param rgb     the color that must go away / 사라져야 하는 색
     * @param message the message of a failure / 실패 메시지
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static void awaitPixelNot(SwingWebPanel panel, int x, int y, int rgb, String message) throws Exception {
        long end = System.currentTimeMillis() + TIMEOUT;
        int actual;
        // 그림이 바뀔 때까지 짧게 쉬며 화소를 확인한다.
        do {
            actual = snapshot(panel).getRGB(x, y) & 0xFFFFFF;
            if (actual != rgb) {
                return;
            }
            Thread.sleep(15);
        } while (System.currentTimeMillis() < end);
        Check.fail(message + ": pixel " + x + "," + y + " still " + Integer.toHexString(actual));
    }

    /**
     * Sends a mouse event to the component on the EDT.
     * <p>
     * EDT 에서 컴포넌트에 마우스 이벤트를 보낸다.
     *
     * @param panel  the component / 컴포넌트
     * @param id     the AWT event id / AWT 이벤트 ID
     * @param x      x / x
     * @param y      y / y
     * @param clicks the click count / 클릭 횟수
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static void mouse(final SwingWebPanel panel, final int id, final int x, final int y, final int clicks) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 보낸다. */
            public void run() {
                int modifiers = id == java.awt.event.MouseEvent.MOUSE_PRESSED || id == java.awt.event.MouseEvent.MOUSE_DRAGGED
                        ? java.awt.event.InputEvent.BUTTON1_MASK : 0;
                panel.dispatchEvent(new java.awt.event.MouseEvent(panel, id, System.currentTimeMillis(), modifiers, x, y, clicks, false,
                        java.awt.event.MouseEvent.BUTTON1));
            }
        });
    }

    /**
     * Clicks at a point: press, release and click.
     * <p>
     * 한 점을 클릭한다: 누르기, 떼기, 클릭.
     *
     * @param panel the component / 컴포넌트
     * @param x     x / x
     * @param y     y / y
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static void click(SwingWebPanel panel, int x, int y) throws Exception {
        mouse(panel, java.awt.event.MouseEvent.MOUSE_MOVED, x, y, 0);
        mouse(panel, java.awt.event.MouseEvent.MOUSE_PRESSED, x, y, 1);
        mouse(panel, java.awt.event.MouseEvent.MOUSE_RELEASED, x, y, 1);
        mouse(panel, java.awt.event.MouseEvent.MOUSE_CLICKED, x, y, 1);
    }

    /**
     * Turns the mouse wheel at a point.
     * <p>
     * 한 점에서 마우스 휠을 돌린다.
     *
     * @param panel the component / 컴포넌트
     * @param x     x / x
     * @param y     y / y
     * @param units the number of lines, positive for down / 줄 수(양수는 아래로)
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static void wheel(final SwingWebPanel panel, final int x, final int y, final int units) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the event. / 이벤트를 보낸다. */
            public void run() {
                panel.dispatchEvent(new java.awt.event.MouseWheelEvent(panel, java.awt.event.MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, x, y,
                        0, false, java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, units));
            }
        });
    }

    /**
     * Types a key (pressed and released) into the component.
     * <p>
     * 컴포넌트에 키를 입력한다(누름과 뗌).
     *
     * @param panel   the component / 컴포넌트
     * @param keyCode the AWT key code / AWT 키 코드
     * @param ch      the character / 문자
     * @throws Exception if the EDT fails / EDT 가 실패하면
     */
    public static void key(final SwingWebPanel panel, final int keyCode, final char ch) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Dispatches the events. / 이벤트를 보낸다. */
            public void run() {
                long when = System.currentTimeMillis();
                java.awt.event.KeyEvent down = new java.awt.event.KeyEvent(panel, java.awt.event.KeyEvent.KEY_PRESSED, when, 0, keyCode, ch);
                java.awt.event.KeyEvent up = new java.awt.event.KeyEvent(panel, java.awt.event.KeyEvent.KEY_RELEASED, when, 0, keyCode, ch);
                java.awt.event.KeyListener[] listeners = panel.getKeyListeners();
                // 컴포넌트가 포커스를 갖지 않아도 키 리스너에 직접 이벤트를 전달한다(AWT 는 포커스 소유자에게만 보낸다).
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].keyPressed(down);
                }
                // 뗌 이벤트도 같은 방식으로 전달한다.
                for (int i = 0; i < listeners.length; i++) {
                    listeners[i].keyReleased(up);
                }
            }
        });
    }

    /**
     * Returns a value that a script stored in the sessionStorage of the component.
     * <p>
     * 스크립트가 컴포넌트의 sessionStorage 에 저장한 값을 반환한다.
     *
     * @param panel the component / 컴포넌트
     * @param key   the key / 키
     * @return the value or null / 값 또는 null
     */
    public static String stored(SwingWebPanel panel, String key) {
        // 복사한 저장소 항목에서 키를 찾는다.
        for (StorageEntry entry : panel.getSessionStorage()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Waits until the stored value equals the expected one.
     * <p>
     * 저장된 값이 기대한 값이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param key      the key / 키
     * @param expected the value / 기대 값
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static void awaitStored(SwingWebPanel panel, String key, String expected) throws Exception {
        long end = System.currentTimeMillis() + TIMEOUT;
        // 스크립트가 값을 저장할 때까지 짧게 쉬며 확인한다.
        while (!expected.equals(stored(panel, key)) && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(expected, stored(panel, key), "stored value of " + key);
    }

    /**
     * Waits until the component shows a progress.
     * <p>
     * 컴포넌트가 어떤 진행률이 될 때까지 기다린다.
     *
     * @param panel    the component / 컴포넌트
     * @param progress the progress / 진행률
     * @throws Exception if interrupted / 인터럽트되면
     */
    public static void awaitProgress(SwingWebPanel panel, int progress) throws Exception {
        long end = System.currentTimeMillis() + TIMEOUT;
        // 진행률이 그 값이 될 때까지 짧게 쉬며 확인한다.
        while (panel.getProgress() != progress && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        Check.equal(progress, panel.getProgress(), "progress");
    }
}
