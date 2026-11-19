package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.PageRenderer;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;

/**
 * Shared fixture of the legacy HTML presentation tests (plan item P07-07-b): parses real markup with scripting disabled, runs the real CSS
 * engine with predictable font metrics (20px monospace, no default body margin) and gives access to computed styles, layout boxes and
 * painted pixels. A page can be parsed in no-quirks mode (with a doctype) or in quirks mode (without one).
 * <p>
 * 과거 HTML 표시 시험(계획 P07-07-b)의 공용 도구: 스크립트를 끄고 실제 마크업을 해석하며 예측 가능한 글꼴(20px 고정폭, 기본 본문 여백 없음)로 실제 CSS
 * 엔진을 돌려 계산 스타일, 배치 상자, 그려진 화소를 제공한다. 문서 형식이 있으면 표준 모드, 없으면 호환 모드로 해석한다.
 * <p>
 * Usage: <code>LegacyFixture fx = new LegacyFixture(); fx.init("&lt;p id=a&gt;x", ""); fx.style("a"); fx.dispose();</code>
 * <p>
 * 사용법: <code>LegacyFixture fx = new LegacyFixture(); fx.init("&lt;p id=a&gt;x", ""); fx.style("a"); fx.dispose();</code>
 */
public final class LegacyFixture {

    /** Viewport width of rendered pages. / 렌더링 페이지의 뷰포트 너비. */
    public static final int WIDTH = 200;

    /** Viewport height of rendered pages. / 렌더링 페이지의 뷰포트 높이. */
    public static final int HEIGHT = 160;

    /** Current parsed document. / 현재 해석한 문서. */
    private Document document;

    /** Current CSS engine. / 현재 CSS 엔진. */
    private CssEngine css;

    /**
     * Creates an empty fixture.
     * <p>
     * 빈 도구를 만든다.
     */
    public LegacyFixture() {
    }

    /**
     * Releases the CSS engine of the current case.
     * <p>
     * 현재 사례의 CSS 엔진을 해제한다.
     */
    public void dispose() {
        if (css != null) {
            css.dispose();
            css = null;
        }
    }

    /**
     * Parses a no-quirks (doctype) page.
     * <p>
     * 표준 모드(문서 형식 있음) 페이지를 해석한다.
     *
     * @param markup body content / 본문 내용
     * @param rules  extra author CSS / 추가 작성자 CSS
     */
    public void init(String markup, String rules) {
        parse("<!doctype html>", markup, rules);
    }

    /**
     * Parses a quirks-mode page (no doctype).
     * <p>
     * 호환 모드(문서 형식 없음) 페이지를 해석한다.
     *
     * @param markup body content / 본문 내용
     * @param rules  extra author CSS / 추가 작성자 CSS
     */
    public void initQuirks(String markup, String rules) {
        parse("", markup, rules);
    }

    /**
     * Parses a page with an arbitrary prefix (a doctype or nothing).
     * <p>
     * 임의의 앞부분(문서 형식 또는 없음)을 붙여 페이지를 해석한다.
     *
     * @param prefix  text before the style element / style 요소 앞의 문자열
     * @param markup  body content / 본문 내용
     * @param rules   extra author CSS / 추가 작성자 CSS
     */
    public void parse(String prefix, String markup, String rules) {
        dispose();
        document = HtmlParser.parseDocument(prefix + "<style>html,body{margin:0;padding:0}" + "body{font:20px monospace;line-height:normal}" + rules
                + "</style><body>" + markup, null, null, false);
        css = new CssEngine(document, null, new MediaEnvironment(WIDTH, HEIGHT));
        css.loadStyleSheets();
    }

    /**
     * Returns the current document.
     * <p>
     * 현재 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document document() {
        return document;
    }

    /**
     * Returns the current CSS engine.
     * <p>
     * 현재 CSS 엔진을 반환한다.
     *
     * @return the engine / 엔진
     */
    public CssEngine css() {
        return css;
    }

    /**
     * Returns an element by id.
     * <p>
     * id 로 요소를 반환한다.
     *
     * @param id the id / id
     * @return the element, never null / 요소(null 이 아님)
     */
    public Element element(String id) {
        Element e = document.getElementById(id);
        if (e == null) {
            throw new IllegalArgumentException("no element #" + id);
        }
        return e;
    }

    /**
     * Returns the computed style of an element by id.
     * <p>
     * id 로 요소의 계산 스타일을 반환한다.
     *
     * @param id the id / id
     * @return the computed style / 계산 스타일
     */
    public ComputedStyle style(String id) {
        return css.getComputedStyle(element(id));
    }

    /**
     * Lays out the current document.
     * <p>
     * 현재 문서를 배치한다.
     *
     * @return the rendered page / 렌더링한 페이지
     */
    public RenderedPage render() {
        return PageRenderer.render(document, css, null, WIDTH, HEIGHT);
    }

    /**
     * Returns the first layout box of an element.
     * <p>
     * 요소의 첫 배치 상자를 반환한다.
     *
     * @param page the rendered page / 렌더링한 페이지
     * @param id   the id / id
     * @return the box / 상자
     */
    public LayoutBox box(RenderedPage page, String id) {
        return page.getLayout().getBoxes(element(id)).get(0);
    }

    /**
     * Paints a page on a white canvas.
     * <p>
     * 페이지를 흰 캔버스에 그린다.
     *
     * @param page the rendered page / 렌더링한 페이지
     * @return the painted image / 그린 이미지
     */
    public static BufferedImage paint(RenderedPage page) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            page.paint(graphics, null, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /**
     * Formats one border side of a computed style as <code>"3px outset"</code>.
     * <p>
     * 계산 스타일의 한 변 border 를 <code>"3px outset"</code> 형태로 만든다.
     *
     * @param s    the style / 스타일
     * @param side <code>top</code>, <code>right</code>, <code>bottom</code> or <code>left</code> / 변 이름
     * @return width and style / 두께와 스타일
     */
    public static String border(ComputedStyle s, String side) {
        String w = s.getPropertyValue("border-" + side + "-width");
        String st = s.getPropertyValue("border-" + side + "-style");
        return w + " " + st;
    }

    /**
     * Formats the four borders of a style as one string in top, right, bottom, left order.
     * <p>
     * 스타일의 네 border 를 위, 오른쪽, 아래, 왼쪽 순서의 한 문자열로 만든다.
     *
     * @param s the style / 스타일
     * @return the four sides separated by commas / 쉼표로 구분한 네 변
     */
    public static String borders(ComputedStyle s) {
        return border(s, "top") + "," + border(s, "right") + "," + border(s, "bottom") + "," + border(s, "left");
    }
}
