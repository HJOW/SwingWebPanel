package org.duckdns.hjow.swingwebpanel.render;

import java.awt.image.BufferedImage;
import java.util.List;

import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of <code>::file-selector-button</code> (CSS Pseudo-Elements 4 section 5.1, WD 27 June 2025: any property applies to the button of a file control)
 * and of the top layer with <code>::backdrop</code> (CSS Positioned Layout 4 sections 3.1 and 3.2, WD 7 October 2025, and the dialog rules of the WHATWG HTML
 * rendering section): a modal dialog is centered in the viewport above everything, even above <code>z-index: 9999</code>, over its backdrop that covers
 * the viewport with <code>rgba(0,0,0,0.1)</code>; <code>:modal</code>; a dialog that is shown without <code>showModal()</code> has no backdrop.
 * <p>
 * <code>::file-selector-button</code>(CSS Pseudo-Elements 4 5.1 절: 파일 컨트롤의 단추에 모든 속성이 적용됨)과 <code>::backdrop</code> 이 있는 최상위 계층(CSS Positioned Layout 4
 * 3.1, 3.2 절과 WHATWG HTML 렌더링 절의 dialog 규칙)의 시험: 모달 대화상자는 뷰포트 가운데에 <code>z-index: 9999</code> 보다도 위에 그려지고, 그 아래의 배경막은
 * <code>rgba(0,0,0,0.1)</code> 로 뷰포트를 덮는다. <code>:modal</code>. <code>showModal()</code> 없이 보인 대화상자에는 배경막이 없다.
 */
public class PseudoElementsMorePaintTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public PseudoElementsMorePaintTest() {
    }

    /** The document of the last page. 마지막 페이지의 문서. */
    private Document doc;

    /**
     * Parses a page and lets the test change the document before it is painted.
     * <p>
     * 페이지를 파싱하고 그리기 전에 시험이 문서를 바꿀 수 있게 한다.
     *
     * @param body the body / body
     * @return the document / 문서
     */
    private Document parse(String body) {
        doc = HtmlParser.parseDocument("<!DOCTYPE html><html><head>" + RenderTestSupport.RESET + "</head><body>" + body + "</body></html>", "http://t.test/dir/page.html",
                null, true);
        return doc;
    }

    /**
     * Renders the parsed page at 200 by 150.
     * <p>
     * 파싱한 페이지를 200x150 으로 렌더링한다.
     *
     * @return the page / 페이지
     */
    private RenderedPage render() {
        CssEngine css = new CssEngine(doc, null, new MediaEnvironment(200, 150));
        css.loadStyleSheets();
        return PageRenderer.render(doc, css, null, 200, 150);
    }

    /**
     * Opens a dialog as a modal dialog the way <code>showModal()</code> does (top layer, then the <code>open</code> attribute).
     * <p>
     * <code>showModal()</code> 처럼 대화상자를 모달로 연다(최상위 계층, 그다음 <code>open</code> 속성).
     *
     * @param id the id / id
     */
    private void showModal(String id) {
        Element d = doc.getElementById(id);
        doc.addToTopLayer(d);
        d.setAttribute("open", "");
    }

    /**
     * Tells whether two colors are close.
     * <p>
     * 두 색이 가까운지 알려 준다.
     *
     * @param img the image / 이미지
     * @param x   x / x
     * @param y   y / y
     * @param rgb the color / 색
     * @param what the description / 설명
     */
    private static void color(BufferedImage img, int x, int y, int rgb, String what) {
        int got = RenderTestSupport.rgb(img, x, y);
        Check.isTrue(RenderTestSupport.near(got, rgb, 4), what + ": expected " + Integer.toHexString(rgb) + " but was " + Integer.toHexString(got));
    }

    /**
     * The button of a file control keeps its own look without a rule; a rule changes what it declares (background, text color, border, padding, radius,
     * the space after the button) and keeps the rest; <code>display: none</code> hides the button.
     * <p>
     * 파일 컨트롤의 단추는 규칙이 없으면 자신의 모양을 지킨다. 규칙은 선언한 것(배경, 글자색, border, padding, 반지름, 단추 뒤의 간격)만 바꾸고 나머지는 지킨다.
     * <code>display: none</code> 은 단추를 숨긴다.
     */
    public void testFileSelectorButton() {
        parse("<input id=f type=file style='width:180px;height:24px;font:12px monospace'>");
        RenderedPage plain = render();
        LayoutBox b = plain.getLayout().getBoxes(doc.getElementById("f")).get(0);
        int bx = (int) b.getContentX();
        int by = (int) (b.getContentY() + b.getContentHeight() / 2);
        BufferedImage img = RenderTestSupport.paint(plain, 200, 150);
        color(img, bx + 3, by - 7, 0xEFEFEF, "without a rule the button has its own fill");
        parse("<style>input::file-selector-button{background-color:red;border:3px solid lime;border-radius:0;padding:0 20px;margin-inline-end:30px}</style>"
                + "<input id=f type=file style='width:180px;height:24px;font:12px monospace'>");
        RenderedPage styled = render();
        BufferedImage s = RenderTestSupport.paint(styled, 200, 150);
        color(s, bx + 1, by, 0x00FF00, "the declared border is lime and 3px wide");
        color(s, bx + 5, by - 7, 0xFF0000, "the declared background is red");
        Check.isTrue(RenderTestSupport.near(RenderTestSupport.rgb(s, bx + 20 + 66 + 6, by - 7), 0xFF0000, 4), "the padding of 20px makes the button wider: still red");
        parse("<style>::file-selector-button{background-color:blue}</style><input id=f type=file style='width:180px;height:24px;font:12px monospace'>");
        BufferedImage only = RenderTestSupport.paint(render(), 200, 150);
        color(only, bx + 5, by - 7, 0x0000FF, "only the background is declared");
        color(only, bx, by, 0x767676, "the border keeps its own color");
        parse("<style>input::file-selector-button{display:none}</style><input id=f type=file style='width:180px;height:24px;font:12px monospace;color:black'>");
        BufferedImage hidden = RenderTestSupport.paint(render(), 200, 150);
        color(hidden, bx + 3, by - 7, 0xFFFFFF, "display:none hides the button");
        Check.isTrue(RenderTestSupport.ink(hidden, bx, by - 8, bx + 12, by + 8) > 0, "the file name starts at the left edge");
    }

    /**
     * A modal dialog is in the top layer: the UA rules center it with a border and padding, its backdrop covers the viewport with
     * <code>rgba(0,0,0,0.1)</code>, and both are painted above an element with <code>z-index: 9999</code>.
     * <p>
     * 모달 대화상자는 최상위 계층에 있다: UA 규칙이 border 와 padding 을 주어 가운데에 놓고, 배경막은 뷰포트를 <code>rgba(0,0,0,0.1)</code> 로 덮으며, 둘 다
     * <code>z-index: 9999</code> 인 요소보다 위에 그려진다.
     */
    public void testModalDialogAndBackdrop() {
        parse("<div style='position:absolute;left:0;top:0;width:200px;height:150px;background:red;z-index:9999'></div>"
                + "<dialog id=d style='width:60px;height:40px;padding:0;border:2px solid black'></dialog>");
        showModal("d");
        RenderedPage p = render();
        BufferedImage img = RenderTestSupport.paint(p, 200, 150);
        color(img, 2, 2, 0xE60000, "the backdrop darkens the red element with z-index 9999 by 10%");
        LayoutBox box = p.getLayout().getBoxes(doc.getElementById("d")).get(0);
        Check.isTrue(Math.abs(box.getX() - 68) <= 1 && Math.abs(box.getY() - 53) <= 1, "the dialog is centered: " + box.getX() + "," + box.getY());
        color(img, 100, 75, 0xFFFFFF, "the dialog paints its Canvas background above everything");
        color(img, 69, 75, 0x000000, "and its border");
        List<LayoutBox> boxes = p.getLayout().getBoxes(doc.getElementById("d"));
        Check.equal(1, boxes.size(), "the backdrop is not a box of the dialog");
    }

    /**
     * <code>:modal</code> and <code>::backdrop</code> can be styled; a dialog shown without <code>showModal()</code> has no backdrop; a modal dialog inside
     * a <code>display: none</code> ancestor is not rendered.
     * <p>
     * <code>:modal</code> 과 <code>::backdrop</code> 을 꾸밀 수 있다. <code>showModal()</code> 없이 보인 대화상자에는 배경막이 없다. <code>display: none</code> 조상 안의 모달 대화상자는
     * 그려지지 않는다.
     */
    public void testStylesAndNonModalDialog() {
        parse("<style>dialog:modal{background:yellow} dialog::backdrop{background:blue}</style><dialog id=d style='width:60px;height:40px'></dialog>");
        showModal("d");
        BufferedImage img = RenderTestSupport.paint(render(), 200, 150);
        color(img, 2, 2, 0x0000FF, "a styled backdrop");
        color(img, 100, 75, 0xFFFF00, "a styled modal dialog");
        parse("<style>dialog:modal{background:yellow}</style><dialog id=d open style='width:60px;height:40px'>x</dialog>");
        BufferedImage shown = RenderTestSupport.paint(render(), 200, 150);
        color(shown, 2, 140, 0xFFFFFF, "a non-modal dialog has no backdrop");
        Check.isFalse(RenderTestSupport.near(RenderTestSupport.rgb(shown, 100, 20), 0xFFFF00, 4), "and is not :modal");
        color(shown, 58, 10, 0x000000, "the UA rules center it horizontally with a solid border: (200 - 86) / 2 = 57");
        parse("<div style='display:none'><dialog id=d style='width:60px;height:40px'></dialog></div>");
        showModal("d");
        BufferedImage none = RenderTestSupport.paint(render(), 200, 150);
        color(none, 2, 2, 0xFFFFFF, "a modal dialog in a display:none ancestor and its backdrop are not rendered");
    }
}
