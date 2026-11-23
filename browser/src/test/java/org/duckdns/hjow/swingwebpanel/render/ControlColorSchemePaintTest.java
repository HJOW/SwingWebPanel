package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the form controls in the dark color scheme (plan item P10-08, CSS Color Adjustment Module Level 1, https://www.w3.org/TR/css-color-adjust-1/): a control whose style
 * uses <code>color-scheme: dark</code> draws the parts that the painter draws itself (the fill and the edge of a check box and a radio button, the arrow of a drop-down,
 * the track of a progress bar, a meter and a slider, the scroll bar, the button of a file control, the edge of a color well) with the dark colors, and the controls of a
 * light page are painted as before.
 * <p>
 * 어두운 색 구성표의 폼 컨트롤 시험(계획 P10-08, CSS Color Adjustment Module Level 1): 스타일이 <code>color-scheme: dark</code> 인 컨트롤은 그리는 쪽이 직접 그리는 부분(체크박스와
 * 라디오 단추의 채움과 가장자리, 드롭다운의 화살표, 진행 막대/미터/슬라이더의 트랙, 스크롤 막대, 파일 컨트롤의 단추, 색 선택 단추의 가장자리)을 어두운 색으로 그리고, 밝은
 * 페이지의 컨트롤은 이전처럼 그려진다.
 */
public class ControlColorSchemePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ControlColorSchemePaintTest() {
    }

    /**
     * Renders a page and returns it.
     * <p>
     * 페이지를 렌더링해 반환한다.
     *
     * @param scheme the style of the root, such as <code>color-scheme:dark</code>, or empty / 루트의 스타일. 비어 있을 수 있음
     * @param body   the body markup / 본문 마크업
     * @return the page / 페이지
     */
    private static RenderedPage page(String scheme, String body) {
        return RenderTestSupport.page("<style>html{" + scheme + "} body{margin:0}</style>" + body, 400, 300, null);
    }

    /**
     * Returns the snapshot of an element.
     * <p>
     * 요소의 스냅샷을 반환한다.
     *
     * @param p  the page / 페이지
     * @param id the id / id
     * @return the snapshot / 스냅샷
     */
    private static ControlSnapshot snap(RenderedPage p, String id) {
        Element e = RenderTestSupport.lastDocument.getElementById(id);
        ControlSnapshot s = p.getControl(e);
        Check.notNull(s, "control #" + id + " has a snapshot");
        return s;
    }

    /**
     * Checks a pixel to a tolerance per channel.
     * <p>
     * 픽셀을 채널별 허용 오차로 확인한다.
     *
     * @param expected the expected RGB / 기대 RGB
     * @param img      the image / 이미지
     * @param x        x / x
     * @param y        y / y
     * @param tol      the tolerance / 허용 오차
     * @param what     the message / 메시지
     */
    private static void pixel(int expected, BufferedImage img, int x, int y, int tol, String what) {
        int got = RenderTestSupport.rgb(img, x, y);
        Check.isTrue(RenderTestSupport.near(expected, got, tol), what + ": expected " + Integer.toHexString(expected) + " but was " + Integer.toHexString(got));
    }

    /**
     * The snapshot knows the color scheme of the control: dark for <code>color-scheme: dark</code>, light by default, and a child of a dark page that says
     * <code>light</code> is light.
     * <p>
     * 스냅샷은 컨트롤의 색 구성표를 안다: <code>color-scheme: dark</code> 이면 어둡고 기본은 밝으며, 어두운 페이지 안의 <code>light</code> 자식은 밝다.
     */
    public void testSnapshotKnowsTheScheme() {
        RenderedPage dark = page("color-scheme:dark", "<input id=a type=checkbox><input id=b type=checkbox style='color-scheme:light'>");
        Check.isTrue(snap(dark, "a").dark, "a control of a dark page is dark");
        Check.isFalse(snap(dark, "b").dark, "a control that says light is light");
        RenderedPage light = page("", "<input id=a type=checkbox>");
        Check.isFalse(snap(light, "a").dark, "a control of an ordinary page is light");
    }

    /**
     * A check box and a radio button: the unchecked ones are dark gray with a gray edge in the dark scheme (white with a darker gray edge in the light one), a checked box
     * is filled with the light accent and its tick is dark.
     * <p>
     * 체크박스와 라디오 단추: 체크되지 않은 것은 어두운 구성표에서 어두운 회색 채움과 회색 가장자리(밝은 구성표는 흰색과 더 어두운 회색 가장자리), 체크된 상자는 밝은
     * 강조색으로 채워지고 체크 표시는 어둡다.
     */
    public void testCheckBoxAndRadio() {
        String body = "<input id=a type=checkbox><input id=b type=checkbox checked><input id=c type=radio><input id=d type=radio checked>";
        RenderedPage dark = page("color-scheme:dark", body);
        BufferedImage img = RenderTestSupport.paint(dark, 400, 300);
        LayoutBox a = RenderTestSupport.box(dark, "a");
        LayoutBox b = RenderTestSupport.box(dark, "b");
        LayoutBox c = RenderTestSupport.box(dark, "c");
        LayoutBox d = RenderTestSupport.box(dark, "d");
        pixel(0x3B3B3B, img, (int) a.getContentX() + 3, (int) a.getContentY() + 10, 3, "an unchecked box is dark gray inside");
        pixel(0x858585, img, (int) a.getContentX() + 6, (int) a.getContentY(), 40, "and has a gray edge");
        pixel(0x99C8FF, img, (int) b.getContentX() + 2, (int) b.getContentY() + 11, 12, "a checked box is the light accent");
        boolean tick = false;
        // 체크된 상자 안에서 어두운 체크 표시 화소를 찾는다.
        for (int y = (int) b.getContentY() + 3; y < (int) b.getContentY() + 11 && !tick; y++) {
            for (int x = (int) b.getContentX() + 3; x < (int) b.getContentX() + 11; x++) {
                if (RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), 0x101010, 60)) {
                    tick = true;
                    break;
                }
            }
        }
        Check.isTrue(tick, "the tick of a checked box is dark");
        pixel(0x3B3B3B, img, (int) c.getContentX() + 4, (int) c.getContentY() + 7, 3, "an unchecked radio button is dark gray inside");
        pixel(0x99C8FF, img, (int) d.getContentX() + 6, (int) d.getContentY() + 6, 12, "the dot of a checked radio button is the light accent");
        RenderedPage light = page("", body);
        BufferedImage w = RenderTestSupport.paint(light, 400, 300);
        LayoutBox la = RenderTestSupport.box(light, "a");
        pixel(0xFFFFFF, w, (int) la.getContentX() + 3, (int) la.getContentY() + 10, 0, "in the light scheme the box is white");
        pixel(0x0075FF, w, (int) RenderTestSupport.box(light, "b").getContentX() + 2, (int) RenderTestSupport.box(light, "b").getContentY() + 11, 8, "and the accent is blue");
    }

    /**
     * The bar controls: the track of a progress bar, a meter and a slider is dark in the dark scheme, and an <code>accent-color</code> still wins.
     * <p>
     * 막대 컨트롤: 진행 막대, 미터, 슬라이더의 트랙은 어두운 구성표에서 어둡고 <code>accent-color</code> 는 여전히 우선한다.
     */
    public void testBarControls() {
        String body = "<progress id=a value=0 max=1 style='width:100px'></progress><br><meter id=b value=0 min=0 max=1 style='width:100px'></meter><br>"
                + "<input id=c type=range value=0 style='width:100px'><br><progress id=d value=0.5 max=1 style='width:100px;accent-color:#f0f'></progress>";
        RenderedPage dark = page("color-scheme:dark", body);
        BufferedImage img = RenderTestSupport.paint(dark, 400, 300);
        LayoutBox a = RenderTestSupport.box(dark, "a");
        LayoutBox b = RenderTestSupport.box(dark, "b");
        LayoutBox c = RenderTestSupport.box(dark, "c");
        LayoutBox d = RenderTestSupport.box(dark, "d");
        pixel(0x3B3B3B, img, (int) a.getContentX() + 60, (int) (a.getContentY() + a.getContentHeight() / 2), 3, "the track of a progress bar is dark");
        pixel(0x3B3B3B, img, (int) b.getContentX() + 60, (int) (b.getContentY() + b.getContentHeight() / 2), 3, "the track of a meter is dark");
        pixel(0x5A5A5A, img, (int) c.getContentX() + 70, (int) (c.getContentY() + c.getContentHeight() / 2), 6, "the track of a slider is gray");
        pixel(0xFF00FF, img, (int) d.getContentX() + 10, (int) (d.getContentY() + d.getContentHeight() / 2), 3, "an accent color of the style still wins");
        RenderedPage light = page("", body);
        BufferedImage w = RenderTestSupport.paint(light, 400, 300);
        LayoutBox la = RenderTestSupport.box(light, "a");
        pixel(0xE9E9E9, w, (int) la.getContentX() + 60, (int) (la.getContentY() + la.getContentHeight() / 2), 3, "in the light scheme the track is light");
    }

    /**
     * A drop-down, a file control and a color well: the arrow, the button and the edge follow the scheme; the light scheme keeps its colors.
     * <p>
     * 드롭다운, 파일 컨트롤, 색 선택 단추: 화살표, 단추, 가장자리가 구성표를 따르고 밝은 구성표는 색을 그대로 유지한다.
     */
    public void testSelectFileAndColor() {
        String body = "<select id=a style='width:100px'><option>x</option></select><br><input id=b type=file style='width:200px'><br><input id=c type=color value='#ff0000'>";
        RenderedPage dark = page("color-scheme:dark", body);
        BufferedImage img = RenderTestSupport.paint(dark, 400, 300);
        LayoutBox a = RenderTestSupport.box(dark, "a");
        LayoutBox b = RenderTestSupport.box(dark, "b");
        boolean light = false;
        // 드롭다운의 화살표 영역에서 밝은 회색(화살표) 화소를 찾는다.
        for (int y = (int) a.getContentY(); y < (int) (a.getContentY() + a.getContentHeight()) && !light; y++) {
            for (int x = (int) (a.getContentX() + a.getContentWidth() - 18); x < (int) (a.getContentX() + a.getContentWidth()); x++) {
                if (RenderTestSupport.near(RenderTestSupport.rgb(img, x, y), 0xE0E0E0, 10)) {
                    light = true;
                    break;
                }
            }
        }
        Check.isTrue(light, "the arrow of a drop-down is light on the dark scheme");
        pixel(0x3B3B3B, img, (int) b.getContentX() + 3, (int) (b.getContentY() + b.getContentHeight() / 2), 6, "the button of a file control is dark");
        RenderedPage lp = page("", body);
        BufferedImage w = RenderTestSupport.paint(lp, 400, 300);
        LayoutBox lb = RenderTestSupport.box(lp, "b");
        pixel(0xEFEFEF, w, (int) lb.getContentX() + 3, (int) (lb.getContentY() + lb.getContentHeight() / 2), 6, "the button of a file control is light in the light scheme");
    }

    /**
     * The scroll bar of a text area and of a list box that have more content than they show: dark track and gray thumb in the dark scheme.
     * <p>
     * 보이는 것보다 내용이 많은 텍스트 영역과 리스트 박스의 스크롤 막대: 어두운 구성표에서 어두운 트랙과 회색 손잡이.
     */
    public void testScrollBars() {
        StringBuilder rows = new StringBuilder();
        // 줄이 넘치도록 옵션을 많이 만든다.
        for (int i = 0; i < 20; i++) {
            rows.append("<option>item ").append(i).append("</option>");
        }
        String body = "<select id=a size=3 style='width:100px'>" + rows + "</select>";
        RenderedPage dark = page("color-scheme:dark", body);
        BufferedImage img = RenderTestSupport.paint(dark, 400, 300);
        LayoutBox a = RenderTestSupport.box(dark, "a");
        int x = (int) (a.getContentX() + a.getContentWidth()) - 2;
        pixel(0x2B2B2B, img, x, (int) (a.getContentY() + a.getContentHeight()) - 3, 4, "the track of the scroll bar is dark");
        pixel(0x6B6B6B, img, x - 5, (int) a.getContentY() + 6, 8, "the thumb is gray");
        RenderedPage light = page("", body);
        BufferedImage w = RenderTestSupport.paint(light, 400, 300);
        LayoutBox la = RenderTestSupport.box(light, "a");
        pixel(0xF0F0F0, w, (int) (la.getContentX() + la.getContentWidth()) - 2, (int) (la.getContentY() + la.getContentHeight()) - 3, 4, "the light track");
    }
}
