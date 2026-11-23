package org.duckdns.hjow.swingwebpanel.core;

import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.awaitSelection;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.awaitReport;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.awaitSelection;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.click;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.close;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.count;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.create;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.drag;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.key;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.open;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.page;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.paint;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.reported;
import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.wheel;

import static org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.join;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Panel;
import org.duckdns.hjow.swingwebpanel.core.TextSelectionFixture.Rects;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the page zoom with the real component (plan item P11-08): what the user sees is scaled (painting, the scroll size), the layout
 * and the scripts see the smaller viewport (<code>innerWidth</code>, the <code>resize</code> event), the mouse coordinates are converted so
 * that a drag selects the text under the pointer, Ctrl+plus, Ctrl+minus, Ctrl+0 and Ctrl+wheel change the zoom, the viewport keeps the
 * point that the user zoomed around, and the Swing editors of the form controls are scaled too.
 * <p>
 * 실제 컴포넌트로 하는 페이지 확대의 시험(계획 P11-08): 사용자가 보는 것이 확대되고(그리기, 스크롤 크기), 레이아웃과 스크립트는 더 작은 뷰포트를 보며
 * (<code>innerWidth</code>, <code>resize</code> 이벤트), 마우스 좌표가 변환되어 드래그가 포인터 아래의 텍스트를 선택하고, Ctrl+플러스, Ctrl+마이너스, Ctrl+0,
 * Ctrl+휠이 확대를 바꾸며, 뷰포트가 사용자가 확대의 기준으로 삼은 점을 유지하고, 폼 컨트롤의 Swing 편집기도 확대된다.
 */
public class TextZoomTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TextZoomTest() {
    }

    /**
     * Zooming to 200 percent doubles the painted page and the scroll size, and the page is laid out for the viewport that is half as large, which
     * its scripts see (<code>innerWidth</code> and the <code>resize</code> event); 100 percent restores everything.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testZoomScalesPaintingScrollSizeAndTheViewportOfScripts() throws Exception {
        Panel panel = create(300, 200);
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            BufferedImage normal = paint(panel);
            Check.equal(0x123456, normal.getRGB(40, 40) & 0xffffff, "100 percent: inside the box");
            Check.equal(0xffffff, normal.getRGB(80, 40) & 0xffffff, "100 percent: outside the box");
            Dimension before = panel.getPreferredSize();
            drag(panel, rects.left(0) + 1, rects.middle(0), rects.left(0) + 5 * adv + 1, rects.middle(0));
            awaitSelection(panel, "hello");
            panel.setZoom(2.0);
            awaitReport(panel, "size:");
            TestSupport.flushEdt();
            Check.equal("size:150x100", reported(panel), "the script sees half of the viewport");
            BufferedImage zoomed = paint(panel);
            Check.equal(0x123456, zoomed.getRGB(90, 90) & 0xffffff, "200 percent: the 50 pixel box covers 100 pixels");
            Check.equal(0xffffff, zoomed.getRGB(110, 90) & 0xffffff, "200 percent: and not more");
            Dimension after = panel.getPreferredSize();
            Check.isTrue(Math.abs(after.height - 2 * before.height) <= 2, "the scroll height doubles: " + before.height + " to " + after.height);
            int[] highlighted = count(zoomed, (int) (2 * (rects.left(0) + 0.5 * adv)), (int) (2 * (rects.left(0) + 4.5 * adv)), (int) (2 * rects.bottom(0)));
            Check.isTrue(highlighted[0] > 6 * adv, "the selection that was made at 100 percent is highlighted at the scaled place: " + highlighted[0]);
            panel.setZoom(1.0);
            long deadline = System.currentTimeMillis() + 10000;
            // 100 % 로 돌아가면 스크립트가 다시 원래 뷰포트를 본다.
            while (!"size:300x200".equals(reported(panel)) && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.equal("size:300x200", reported(panel), "100 percent restores the viewport");
            TestSupport.flushEdt();
            Check.equal(0xffffff, paint(panel).getRGB(80, 40) & 0xffffff, "and the drawing");
        } finally {
            close(panel);
        }
    }

    /**
     * The zoom factor is kept inside 0.25 to 5.0, an invalid number is refused, the property change is announced on the EDT, and the zoom
     * stays when another page is shown or the component is cleaned.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testZoomFactorRulesAndProperty() throws Exception {
        final Panel panel = create(400, 300);
        try {
            final List<String> changes = Collections.synchronizedList(new ArrayList<String>());
            panel.addPropertyChangeListener(SwingWebPanel.PROPERTY_ZOOM, new PropertyChangeListener() {
                /** Records the change. / 변경을 기록한다.
                 * @param event the event / 이벤트 */
                public void propertyChange(PropertyChangeEvent event) {
                    changes.add(event.getOldValue() + ">" + event.getNewValue() + (SwingUtilities.isEventDispatchThread() ? "" : " off the EDT"));
                }
            });
            Check.equal(1.0, panel.getZoom(), "100 percent at the start");
            panel.setZoom(10);
            Check.equal(5.0, panel.getZoom(), "cut to the largest");
            panel.setZoom(0.1);
            Check.equal(0.25, panel.getZoom(), "cut to the smallest");
            panel.setZoom(1.5);
            Check.equal(1.5, panel.getZoom(), "a factor in the range");
            TestSupport.flushEdt();
            Check.equal("1.0>5.0,5.0>0.25,0.25>1.5", join(changes), "each change is announced on the EDT");
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /** Sets NaN. / NaN 을 정한다. */
                public void run() {
                    panel.setZoom(Double.NaN);
                }
            }, "NaN");
            Check.throwsException(IllegalArgumentException.class, new Block() {
                /** Sets a negative factor. / 음수 배율을 정한다. */
                public void run() {
                    panel.setZoom(-1);
                }
            }, "not positive");
            Check.equal(1.5, panel.getZoom(), "a refused factor changes nothing");
            panel.setContent("<!doctype html><p>another page</p>", "text/html");
            TestSupport.flushEdt();
            Check.equal(1.5, panel.getZoom(), "the zoom belongs to the component, not to the page");
            panel.clean();
            Check.equal(1.5, panel.getZoom(), "and clean() keeps it");
        } finally {
            close(panel);
        }
        Check.throwsException(IllegalStateException.class, new Block() {
            /** Zooms a disposed component. / 폐기된 컴포넌트를 확대한다. */
            public void run() {
                panel.setZoom(2);
            }
        }, "after dispose");
    }

    /**
     * The mouse coordinates are converted with the zoom factor: a drag at 200 percent and one at 50 percent select the same text as at 100
     * percent, and the highlight is drawn at the scaled place.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testMouseCoordinatesFollowTheZoom() throws Exception {
        Panel panel = create(400, 300);
        try {
            Rects rects = open(panel, page(""));
            double adv = rects.advance(0, 11);
            double[] factors = { 2.0, 0.5 };
            // 두 가지 배율에서 같은 글자를 끌어 같은 텍스트가 선택되는지 확인한다.
            for (int i = 0; i < factors.length; i++) {
                double z = factors[i];
                panel.setZoom(z);
                TestSupport.flushEdt();
                Thread.sleep(100);
                drag(panel, z * (rects.left(0) + 1), z * rects.middle(0), z * (rects.left(0) + 5 * adv + 1), z * rects.middle(0));
                awaitSelection(panel, "hello");
                TestSupport.flushEdt();
                BufferedImage image = paint(panel);
                int y = (int) Math.round(z * rects.bottom(0));
                int[] inside = count(image, (int) (z * (rects.left(0) + 0.5 * adv)), (int) (z * (rects.left(0) + 4.5 * adv)), y);
                int[] outside = count(image, (int) (z * (rects.left(0) + 6 * adv)), (int) (z * (rects.left(0) + 10 * adv)), y);
                Check.isTrue(inside[0] > z * 3 * adv, "at " + z + " the selected characters are highlighted at the scaled place: " + inside[0]);
                Check.equal(0, outside[0], "at " + z + " the rest of the line is not highlighted");
                // 다음 배율을 위해 선택을 비운다.
                click(panel, z * (rects.left(1) + 2), z * rects.middle(1), 1, false);
                awaitSelection(panel, "");
            }
        } finally {
            close(panel);
        }
    }

    /**
     * Ctrl+plus and Ctrl+minus go through the steps, Ctrl+0 and Ctrl+0 on the number pad restore 100 percent, Ctrl+wheel zooms (up in, down
     * out) and the wheel alone does not.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testZoomKeysAndWheel() throws Exception {
        Panel panel = create(300, 200);
        try {
            open(panel, page(""));
            key(panel, KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK);
            Check.near(1.1, panel.getZoom(), 1e-9, "Ctrl+= zooms in one step");
            key(panel, KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK);
            Check.near(1.25, panel.getZoom(), 1e-9, "Ctrl+plus of the number pad");
            key(panel, KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK);
            Check.near(1.1, panel.getZoom(), 1e-9, "Ctrl+minus zooms out one step");
            key(panel, KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK);
            Check.near(1.0, panel.getZoom(), 1e-9, "Ctrl+0 restores 100 percent");
            key(panel, KeyEvent.VK_EQUALS, 0);
            Check.near(1.0, panel.getZoom(), 1e-9, "the key without Ctrl does nothing");
            wheel(panel, 50, 50, -1, InputEvent.CTRL_DOWN_MASK);
            Check.near(1.1, panel.getZoom(), 1e-9, "Ctrl+wheel up zooms in");
            wheel(panel, 50, 50, 1, InputEvent.CTRL_DOWN_MASK);
            Check.near(1.0, panel.getZoom(), 1e-9, "Ctrl+wheel down zooms out");
            wheel(panel, 50, 50, -1, 0);
            Check.near(1.0, panel.getZoom(), 1e-9, "the wheel alone does not zoom");
            // 가장 큰 단계를 넘을 만큼 확대 키를 반복해 누른다.
            for (int i = 0; i < 30; i++) {
                key(panel, KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK);
            }
            Check.near(5.0, panel.getZoom(), 1e-9, "the steps end at 500 percent");
            // 가장 작은 단계를 넘을 만큼 축소 키를 반복해 누른다.
            for (int i = 0; i < 30; i++) {
                key(panel, KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK);
            }
            Check.near(0.25, panel.getZoom(), 1e-9, "and at 25 percent");
        } finally {
            close(panel);
        }
    }

    /**
     * In a scroll pane the zoom keeps the point of the page at the top of the viewport (or under the pointer of Ctrl+wheel) where it is.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testZoomKeepsThePointOfTheViewport() throws Exception {
        final Panel panel = create(300, 200);
        final JScrollPane[] holder = new JScrollPane[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Puts the component into a scroll pane. / 컴포넌트를 스크롤 창에 넣는다. */
            public void run() {
                holder[0] = new JScrollPane(panel);
                holder[0].setSize(320, 220);
                holder[0].validate();
            }
        });
        final JScrollPane pane = holder[0];
        try {
            open(panel, page(""));
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Lays out the pane for the new content size and scrolls down. / 새 내용 크기에 맞춰 창을 배치하고 아래로 스크롤한다. */
                public void run() {
                    pane.validate();
                    pane.getViewport().validate();
                    pane.getViewport().setViewPosition(new Point(0, 300));
                }
            });
            Check.equal(300, viewY(pane), "the viewport is scrolled down by 300 pixels");
            panel.setZoom(2.0);
            waitForViewY(pane, 600);
            Check.near(600, viewY(pane), 2, "at 200 percent the top of the viewport is the same point of the page");
            panel.setZoom(1.0);
            waitForViewY(pane, 300);
            // 포인터 위치(뷰포트 안 y=100)를 기준으로 1.1 배로 확대하면 그 아래의 문서 점이 제자리에 남는다.
            wheel(panel, 100, 400, -1, InputEvent.CTRL_DOWN_MASK);
            waitForViewY(pane, (int) Math.round(400 * 1.1 - 100));
            Check.near(340, viewY(pane), 2, "the point under the pointer stays under the pointer");
        } finally {
            close(panel);
        }
    }

    /**
     * Returns the vertical scroll position of a pane.
     * <p>
     * 스크롤 창의 세로 스크롤 위치를 반환한다.
     *
     * @param pane the pane / 스크롤 창
     * @return the position / 위치
     */
    private static int viewY(JScrollPane pane) {
        return pane.getViewport().getViewPosition().y;
    }

    /**
     * Waits until the vertical scroll position is (about) a value, re-validating the pane as Swing would on its event loop.
     * <p>
     * 세로 스크롤 위치가 (대략) 한 값이 될 때까지 기다린다. Swing 이 이벤트 루프에서 하듯 창을 다시 검증한다.
     *
     * @param pane     the pane / 스크롤 창
     * @param expected the position / 위치
     * @throws Exception on failure / 실패 시
     */
    private static void waitForViewY(final JScrollPane pane, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        // 새 레이아웃이 게시되어 위치가 맞춰질 때까지 창을 검증하며 기다린다.
        while (Math.abs(viewY(pane) - expected) > 2 && System.currentTimeMillis() < deadline) {
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Validates the pane. / 창을 검증한다. */
                public void run() {
                    pane.validate();
                    pane.getViewport().validate();
                }
            });
            Thread.sleep(20);
        }
    }

    /**
     * The Swing editor of a text field that has the focus is as many times larger as the zoom factor says, in position, size and font.
     *
     * @throws Exception on failure / 실패 시
     */
    public void testEditorsAreScaled() throws Exception {
        final Panel panel = create(400, 300);
        try {
            String html = "<!doctype html><style>html,body{margin:0}</style><input id=i value='abc' style='position:absolute;left:30px;top:40px;width:120px;height:20px;font:12px monospace;padding:0;border:0'>"
                    + "<script>window.addEventListener('load', function () { document.getElementById('i').focus(); sessionStorage.setItem('report', 'ready'); });</script>";
            panel.show(html);
            awaitReport(panel, "ready");
            SwingUtilities.invokeAndWait(new Runnable() {
                /** Gives the focus to the page as AWT does. / AWT 가 하듯 페이지에 포커스를 준다. */
                public void run() {
                    FocusEvent e = new FocusEvent(panel, FocusEvent.FOCUS_GAINED);
                    FocusListener[] l = panel.getFocusListeners();
                    // 컴포넌트에 달린 포커스 리스너(페이지 키보드)에 포커스 획득을 알린다.
                    for (int i = 0; i < l.length; i++) {
                        l[i].focusGained(e);
                    }
                }
            });
            long deadline = System.currentTimeMillis() + 10000;
            // 포커스를 가진 입력 위에 편집기가 생길 때까지 기다린다.
            while (panel.getComponentCount() == 0 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10);
            }
            Check.equal(1, panel.getComponentCount(), "the editor of the focused field");
            final JTextField editor = (JTextField) panel.getComponent(0);
            int x = editor.getX();
            int w = editor.getWidth();
            float size = editor.getFont().getSize2D();
            panel.setZoom(2.0);
            TestSupport.flushEdt();
            Check.near(2 * x, editor.getX(), 2, "the editor moves with the zoom");
            Check.near(2 * w, editor.getWidth(), 2, "and is as many times wider");
            Check.near(2 * size, editor.getFont().getSize2D(), 0.6, "and its font is as many times larger");
            panel.setZoom(1.0);
            TestSupport.flushEdt();
            Check.near(x, editor.getX(), 2, "back at 100 percent");
            Check.near(size, editor.getFont().getSize2D(), 0.6, "with the original font");
        } finally {
            close(panel);
        }
    }
}
