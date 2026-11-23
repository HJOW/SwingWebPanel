package org.duckdns.hjow.swingwebpanel.examples;

import java.awt.GraphicsEnvironment;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

/**
 * Opens a page with an editable area (<code>contenteditable</code>), draggable items, a drop zone and a text field, so that a person can try
 * what the automatic tests cannot: typing and Hangul input with a real input method, the real mouse for drag and drop, and the real system
 * clipboard (Ctrl+C, Ctrl+X, Ctrl+V). Run <code>examples/run.sh EditingExample</code>; <code>--smoke</code> loads the page offscreen (works without a
 * display) and checks that the editing elements are there. Closing the window releases the component.
 * <p>
 * 편집 영역(<code>contenteditable</code>), 끌 수 있는 항목, 놓는 곳, 텍스트 필드가 있는 페이지를 열어, 자동 시험이 못 하는 것을 사람이 해 볼 수 있게 한다: 실제 입력기로
 * 하는 입력과 한글 입력, 끌어다 놓기의 실제 마우스, 실제 시스템 클립보드(Ctrl+C, Ctrl+X, Ctrl+V). <code>examples/run.sh EditingExample</code> 로 실행하며
 * <code>--smoke</code> 는 화면 없이 페이지를 읽어 편집 요소가 있는지 확인한다. 창을 닫으면 컴포넌트를 해제한다.
 */
public final class EditingExample {

    /**
     * The bundled page.
     * <p>
     * 동봉한 페이지.
     */
    private static final String START = "classpath://org/duckdns/hjow/swingwebpanel/examples/pages/editing.html";

    /**
     * Prevents constructing the entry-point helper.
     * <p>
     * 진입점 도우미의 생성을 막는다.
     */
    private EditingExample() {
    }

    /**
     * Opens the window with the page, or runs the offscreen smoke check.
     * <p>
     * 페이지가 있는 창을 열거나 오프스크린 점검을 실행한다.
     *
     * @param args optional <code>--smoke</code> / 선택적인 <code>--smoke</code>
     * @throws Exception on EDT failure or a failed check / EDT 실패나 점검 실패 시
     */
    public static void main(String[] args) throws Exception {
        boolean smoke = GraphicsEnvironment.isHeadless();
        // 명시적인 점검 옵션이 있으면 창 대신 화면 없는 점검을 한다.
        for (int i = 0; i < args.length; i++) {
            smoke |= "--smoke".equals(args[i]);
        }
        if (smoke) {
            smoke();
        } else {
            BlankPageExample.main(new String[] {START});
        }
    }

    /**
     * Loads the page offscreen, waits until it is shown, and checks its source; the component is always disposed.
     * <p>
     * 페이지를 화면 없이 읽어 보일 때까지 기다리고 원문을 확인한다. 컴포넌트는 항상 해제한다.
     *
     * @throws Exception on a failed check / 점검 실패 시
     */
    private static void smoke() throws Exception {
        final SwingWebPanel[] panels = new SwingWebPanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the offscreen component on the EDT. / EDT 에서 오프스크린 컴포넌트를 만든다. */
            public void run() {
                panels[0] = new SwingWebPanel();
                panels[0].setSize(640, 600);
            }
        });
        SwingWebPanel panel = panels[0];
        try {
            panel.setPage(START);
            long deadline = System.currentTimeMillis() + 20000;
            // 페이지가 확정되고 원문이 도착할 때까지 기다린다.
            while (System.currentTimeMillis() < deadline && !(START.equals(panel.getPage()) && panel.getProgress() == 100)) {
                Thread.sleep(20);
            }
            String source = panel.getContent();
            if (!START.equals(panel.getPage()) || source.indexOf("contenteditable") < 0 || source.indexOf("draggable") < 0) {
                throw new IllegalStateException("Editing page smoke failed: " + panel.getPage() + " progress=" + panel.getProgress());
            }
            System.out.println("smoke OK page=" + panel.getPage() + " sourceLength=" + source.length());
        } finally {
            panel.dispose();
        }
    }
}
