package org.duckdns.hjow.swingwebpanel.examples;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import javax.swing.SwingUtilities;
import org.duckdns.hjow.swingwebpanel.SwingWebPanel;

/**
 * Opens the bundled static HTML/CSS pages in the browser window. Run examples/run.sh StaticPageExample, or use --smoke for
 * an offscreen Java 6 rendering check that also works without a display. Closing the window releases the component.
 * <p>동봉한 정적 HTML·CSS 페이지를 브라우저 창에서 연다. examples/run.sh StaticPageExample로 실행하며 --smoke는 화면 없이도 동작하는 Java 6 그리기 점검이다.
 * 창을 닫으면 컴포넌트를 해제한다.
 */
public final class StaticPageExample {
    /** Bundled start address. / 동봉한 시작 주소. */
    private static final String START = "classpath://org/duckdns/hjow/swingwebpanel/examples/pages/index.html";
    /** Sample background shared by the stylesheet and smoke check. / 스타일시트와 점검이 공유하는 예제 배경색. */
    private static final int BACKGROUND = 0xf4f6f8;

    /** Prevents constructing the entry-point helper. / 진입점 도우미의 생성을 막는다. */
    private StaticPageExample() {
    }

    /**
     * Opens the example, or runs the offscreen smoke check. No external URL or library is required.
     * <p>예제를 열거나 오프스크린 점검을 실행하며 외부 URL·라이브러리가 필요 없다.
     * @param args optional --smoke / 선택적인 --smoke
     * @throws Exception on rendering or EDT failure / 그리기 또는 EDT 실패 시
     */
    public static void main(String[] args) throws Exception {
        boolean smoke = GraphicsEnvironment.isHeadless();
        // 명시적인 점검 옵션이 있으면 창 대신 실제 그림을 검증한다.
        for (int i = 0; i < args.length; i++) {
            smoke |= "--smoke".equals(args[i]);
        }
        if (smoke) {
            smoke();
        } else {
            BlankPageExample.main(new String[] {START});
        }
    }

    /** Exposes rendered height for the smoke check. / 점검에 렌더링 높이를 노출한다. */
    private static final class SamplePanel extends SwingWebPanel {
        /** Serialization identifier. / 직렬화 식별자. */
        private static final long serialVersionUID = 1L;
        /** Creates the standard browser. / 표준 브라우저를 만든다. */
        SamplePanel() {
        }
        /** Returns laid-out document height. / 배치된 문서 높이를 반환한다.
         * @return content height / 내용 높이 */
        int contentHeight() {
            return getViewMetrics().getHeight();
        }
    }

    /**
     * Checks source, classpath navigation, CSS pixels and nonempty layout, and always disposes the component.
     * <p>원문·클래스패스 이동·CSS 픽셀·빈 상태가 아닌 배치를 확인하고 컴포넌트를 반드시 해제한다.
     * @throws Exception on failed expectations / 기대 결과 실패 시
     */
    private static void smoke() throws Exception {
        final SamplePanel[] panels = new SamplePanel[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Creates the offscreen component on the EDT. / EDT에서 오프스크린 컴포넌트를 만든다. */
            public void run() {
                panels[0] = new SamplePanel();
                panels[0].setSize(640, 480);
            }
        });
        SamplePanel panel = panels[0];
        try {
            panel.setPage(START);
            long deadline = System.currentTimeMillis() + 15000;
            int color = 0;
            // 주 문서뿐 아니라 외부 CSS의 픽셀이 게시될 때까지 확인한다.
            while (System.currentTimeMillis() < deadline) {
                color = background(panel);
                if (START.equals(panel.getPage()) && color == BACKGROUND && panel.contentHeight() > 0) {
                    break;
                }
                Thread.sleep(20);
            }
            if (!START.equals(panel.getPage()) || color != BACKGROUND || panel.contentHeight() == 0
                    || panel.getContent().indexOf("<title>Static SwingWebPanel</title>") < 0) {
                throw new IllegalStateException("Static page smoke failed: " + panel.getPage() + " RGB=" + Integer.toHexString(color));
            }
            System.out.println("smoke OK page=" + panel.getPage() + " renderedHeight=" + panel.contentHeight()
                    + " sourceLength=" + panel.getContent().length());
        } finally {
            panel.dispose();
        }
    }

    /**
     * Samples a canvas-background pixel through the real component paint path on the EDT.
     * <p>EDT에서 실제 컴포넌트 그리기 경로로 캔버스 배경 픽셀을 확인한다.
     * @param panel sample component / 예제 컴포넌트
     * @return sampled RGB / 샘플 RGB
     * @throws Exception on EDT failure / EDT 실패 시
     */
    private static int background(final SamplePanel panel) throws Exception {
        final int[] color = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            /** Paints into an image without creating an OS window. / OS 창 없이 이미지에 그린다. */
            public void run() {
                BufferedImage image = new BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    panel.paint(graphics);
                } finally {
                    graphics.dispose();
                }
                color[0] = image.getRGB(2, 2) & 0xffffff;
            }
        });
        return color[0];
    }
}
