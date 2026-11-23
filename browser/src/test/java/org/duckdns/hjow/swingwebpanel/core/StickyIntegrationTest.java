package org.duckdns.hjow.swingwebpanel.core;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>position:sticky</code> and <code>position:fixed</code> through the real component in a <code>JScrollPane</code> (plan item
 * P10-08): the scroll pane moves the component, the painter is told the position of the viewport, and the sticky header and the fixed box are
 * painted at the viewport while the page behind them scrolls; the pixels are read from the real paint path of the viewport after the viewport was
 * scrolled, as the page must be painted again after a scroll.
 * <p>
 * <code>JScrollPane</code> 안의 실제 컴포넌트로 <code>position:sticky</code> 와 <code>position:fixed</code> 를 확인하는 시험(계획 P10-08): 스크롤 창이 컴포넌트를 옮기고
 * 그리기 단계는 뷰포트의 위치를 받으며, sticky 머리글과 fixed 상자는 뒤의 페이지가 스크롤되는 동안 뷰포트에 그려진다. 스크롤한 뒤에는 페이지를 다시 그려야 하므로
 * 뷰포트의 실제 그리기 경로에서 픽셀을 읽는다.
 */
public class StickyIntegrationTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public StickyIntegrationTest() {
    }

    /**
     * Reads one pixel of the viewport of a scroll pane, painting it on the EDT.
     * <p>
     * 스크롤 창 뷰포트의 픽셀 하나를 EDT 에서 그려 읽는다.
     *
     * @param pane the scroll pane / 스크롤 창
     * @param x    x in viewport coordinates / 뷰포트 좌표의 x
     * @param y    y in viewport coordinates / 뷰포트 좌표의 y
     * @return the RGB value / RGB 값
     * @throws Exception on EDT failure / EDT 실패 시
     */
    private static int pixel(final JScrollPane pane, final int x, final int y) throws Exception {
        final int[] result = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints the viewport into an image. 뷰포트를 이미지에 그린다. */
            public void run() {
                int w = Math.max(1, pane.getViewport().getWidth());
                int h = Math.max(1, pane.getViewport().getHeight());
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = image.createGraphics();
                try {
                    g.setColor(java.awt.Color.WHITE);
                    g.fillRect(0, 0, w, h);
                    pane.getViewport().paint(g);
                } finally {
                    g.dispose();
                }
                result[0] = image.getRGB(x, y) & 0xFFFFFF;
            }
        });
        return result[0];
    }

    /**
     * Waits until a pixel of the viewport has a color.
     * <p>
     * 뷰포트의 픽셀이 어떤 색이 될 때까지 기다린다.
     *
     * @param pane  the scroll pane / 스크롤 창
     * @param x     x / x
     * @param y     y / y
     * @param color the expected RGB / 기대 RGB
     * @param what  the description / 설명
     * @throws Exception on failure / 실패 시
     */
    private static void awaitPixel(JScrollPane pane, int x, int y, int color, String what) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        int actual;
        // 문서 작업과 EDT 게시가 끝날 때까지 실제 그림을 반복해서 확인한다.
        do {
            TestSupport.flushEdt();
            actual = pixel(pane, x, y);
            if (actual == color) {
                return;
            }
            Thread.sleep(20);
        } while (System.currentTimeMillis() < deadline);
        Check.equal(color, actual, what);
    }

    /**
     * The header sticks to the viewport top and the fixed box stays at its place of the viewport while the viewport scrolls.
     * <p>
     * 뷰포트가 스크롤되는 동안 머리글은 뷰포트 위쪽에 붙고 fixed 상자는 뷰포트의 제자리에 머문다.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testStickyHeaderAndFixedBoxInAScrollPane() throws Exception {
        final SwingWebPanel[] panels = new SwingWebPanel[1];
        final JScrollPane[] panes = new JScrollPane[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the component in a scroll pane. 스크롤 창 안에 컴포넌트를 만든다. */
            public void run() {
                panels[0] = new SwingWebPanel();
                panes[0] = new JScrollPane(panels[0]);
                panes[0].setSize(220, 120);
                panes[0].doLayout();
                panes[0].getViewport().doLayout();
            }
        });
        SwingWebPanel panel = panels[0];
        final JScrollPane pane = panes[0];
        try {
            panel.setContent("<!doctype html><style>body{margin:0}</style><div style='height:50px;background:#eee'></div>"
                    + "<div style='height:1000px;background:#ddd'><div style='position:sticky;top:10px;height:20px;width:60px;background:#f00'></div></div>"
                    + "<div style='position:fixed;left:100px;top:60px;width:30px;height:30px;background:#00f'></div>", "text/html");
            // 스크롤 전: 머리글은 제자리(문서 y 50..70)에 있다.
            awaitPixel(pane, 10, 60, 0xFF0000, "sticky header at its place before the scroll");
            awaitPixel(pane, 110, 70, 0x0000FF, "fixed box at the viewport position before the scroll");
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Lays the scroll pane out again for the new content size, then scrolls the viewport down by 300. 새 내용 크기로 스크롤 창을 다시 배치한 뒤 뷰포트를 300 아래로 스크롤한다. */
                public void run() {
                    pane.doLayout();
                    pane.getViewport().doLayout();
                    pane.getViewport().setViewPosition(new Point(0, 300));
                }
            });
            // 스크롤 뒤: 머리글은 뷰포트 위에서 10px 아래, fixed 상자는 같은 자리.
            awaitPixel(pane, 10, 20, 0xFF0000, "sticky header at the viewport top after the scroll");
            awaitPixel(pane, 110, 70, 0x0000FF, "fixed box at the same viewport position after the scroll");
            Check.equal(0xDDDDDD, pixel(pane, 150, 20), "the page behind them");
            Check.isFalse(pixel(pane, 10, 60) == 0xFF0000, "the header is not at its place any more");
            // 한 번 더 스크롤해서 화면 갱신이 되풀이되는지 확인한다.
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Scrolls to 500. 500 으로 스크롤한다. */
                public void run() {
                    pane.getViewport().setViewPosition(new Point(0, 500));
                }
            });
            awaitPixel(pane, 10, 15, 0xFF0000, "sticky header at the viewport top after the second scroll");
            awaitPixel(pane, 110, 70, 0x0000FF, "fixed box after the second scroll");
        } finally {
            panel.dispose();
        }
    }
}
