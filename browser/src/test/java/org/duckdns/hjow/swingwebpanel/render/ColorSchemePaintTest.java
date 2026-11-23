package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the painting of the dark color scheme (plan item P10-08, CSS Color Adjustment Module Level 1): the canvas of a page whose root element has
 * <code>color-scheme: dark</code> is the dark <code>Canvas</code> color and its text is light, while an ordinary page keeps the white canvas and the
 * black text; a background of the root element still wins over the scheme.
 * <p>
 * 어두운 색 구성표의 그리기 시험(계획 P10-08, CSS Color Adjustment Module Level 1): 루트 요소가 <code>color-scheme: dark</code> 인 페이지의 캔버스는 어두운 <code>Canvas</code>
 * 색이고 글자는 밝으며, 보통 페이지는 흰 캔버스와 검은 글자를 유지하고, 루트 요소의 배경은 여전히 구성표보다 우선한다.
 */
public class ColorSchemePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ColorSchemePaintTest() {
    }

    /**
     * Counts the pixels of a region that are lighter than a gray level.
     * <p>
     * 영역에서 회색 수준보다 밝은 화소의 수를 센다.
     *
     * @param img   the image / 이미지
     * @param level the gray level 0..255 / 회색 수준
     * @return the number of lighter pixels / 더 밝은 화소 수
     */
    private static int lighter(BufferedImage img, int level) {
        int n = 0;
        // 모든 화소의 밝기를 본다.
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y);
                int g = (((c >> 16) & 0xFF) + ((c >> 8) & 0xFF) + (c & 0xFF)) / 3;
                if (g > level) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * A dark root gets the dark canvas and light text.
     * <p>
     * 어두운 루트는 어두운 캔버스와 밝은 글자를 가진다.
     */
    public void testDarkCanvas() {
        RenderedPage p = RenderTestSupport.page("<style>html{color-scheme:dark}</style><p style='margin:2px'>Hello world</p>", 120, 40, null);
        BufferedImage img = RenderTestSupport.paint(p, 120, 40);
        Check.equal(0x121212, RenderTestSupport.rgb(img, 115, 35), "the empty canvas is the dark Canvas color");
        Check.isTrue(lighter(img, 160) > 20, "the text is light on the dark canvas");
        RenderedPage plain = RenderTestSupport.page("<p style='margin:2px'>Hello world</p>", 120, 40, null);
        BufferedImage w = RenderTestSupport.paint(plain, 120, 40);
        Check.equal(0xFFFFFF, RenderTestSupport.rgb(w, 115, 35), "an ordinary page has the white canvas");
        RenderedPage own = RenderTestSupport.page("<style>html{color-scheme:dark;background:#336699}</style><p style='margin:2px'>Hello world</p>", 120, 40, null);
        Check.equal(0x336699, RenderTestSupport.rgb(RenderTestSupport.paint(own, 120, 40), 115, 35), "the background of the root wins");
    }
}
