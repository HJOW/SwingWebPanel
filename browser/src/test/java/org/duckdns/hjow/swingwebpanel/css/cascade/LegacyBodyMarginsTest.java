package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.media.MediaEnvironment;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.PageRenderer;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * HTML5 2014 body margin hint regression tests (P07-07-b-2): precedence, integer parsing, mutation, cascade and geometry.
 * <p>HTML5 2014 body 여백 힌트 회귀 시험: 우선순위·정수 파싱·변경·캐스케이드·기하.
 */
public class LegacyBodyMarginsTest {
    /** Current document. / 현재 문서. */
    private Document document;
    /** Current body element. / 현재 본문 요소. */
    private Element body;
    /** Current CSS engine. / 현재 CSS 엔진. */
    private CssEngine css;

    /** Creates the suite. / 시험 스위트를 만든다. */
    public LegacyBodyMarginsTest() {
    }

    /** Releases listeners after each case. / 각 사례 후 리스너를 해제한다. */
    public void tearDown() {
        if (css != null) {
            css.dispose();
            css = null;
        }
    }

    /**
     * Creates an actual parsed HTML/CSS document, releasing the previous one in table-driven cases.
     * <p>실제 HTML·CSS 문서를 만들고 표 기반 사례에서는 이전 문서를 해제한다.
     * @param attributes body attributes / 본문 속성
     * @param rules author rules / 작성자 규칙
     */
    private void init(String attributes, String rules) {
        tearDown();
        document = HtmlParser.parseDocument("<!doctype html><style>" + rules + "</style><body " + attributes
                + "><div style='height:40px;background:#123456'></div></body>", null, null, true);
        body = (Element) document.getElementsByTagName("body").item(0);
        css = new CssEngine(document, null, new MediaEnvironment(200, 120));
        css.loadStyleSheets();
    }

    /** Checks the four computed margins. / 네 계산 여백을 확인한다.
     * @param top top pixels / 위 픽셀
     * @param right right pixels / 오른쪽 픽셀
     * @param bottom bottom pixels / 아래 픽셀
     * @param left left pixels / 왼쪽 픽셀 */
    private void margins(double top, double right, double bottom, double left) {
        ComputedStyle style = css.getComputedStyle(body);
        Check.near(top, style.px(Prop.MARGIN_TOP), 0.001, "margin-top");
        Check.near(right, style.px(Prop.MARGIN_RIGHT), 0.001, "margin-right");
        Check.near(bottom, style.px(Prop.MARGIN_BOTTOM), 0.001, "margin-bottom");
        Check.near(left, style.px(Prop.MARGIN_LEFT), 0.001, "margin-left");
    }

    /**
     * Priority is based on existence, not successful parsing, with default 8px instead of a lower-priority valid attribute.
     * <p>우선순위는 파싱 성공이 아닌 존재 여부이며 상위 값이 잘못되면 유효한 하위 속성 대신 기본 8px이다.
     */
    public void testAttributePrecedence() {
        init("", "");
        margins(8, 8, 8, 8);
        init("leftmargin=1 rightmargin=2 topmargin=3 bottommargin=4", "");
        margins(3, 2, 4, 1);
        init("marginwidth=10 marginheight=20 leftmargin=1 rightmargin=2 topmargin=3 bottommargin=4", "");
        margins(20, 10, 20, 10);
        init("marginwidth=bad marginheight='' leftmargin=1 rightmargin=2 topmargin=3 bottommargin=4", "");
        margins(8, 8, 8, 8);
        init("marginwidth=-2 marginheight=+0 leftmargin=1 topmargin=3", "");
        margins(0, 8, 0, 8);
    }

    /**
     * Parsing follows HTML integer prefixes rather than Java trim/parseInt or a nine-digit truncation.
     * <p>Java trim·parseInt나 아홉 자리 잘림 대신 HTML 정수 접두사 파싱을 따른다.
     */
    public void testIntegerPrefixes() {
        init("marginwidth='\t+12px' marginheight='-0'", "");
        margins(0, 12, 0, 12);
        init("marginwidth='0000000000012' marginheight='3.7'", "");
        margins(3, 12, 3, 12);
        init("marginwidth=2147483648 marginheight=1234567890", "");
        margins(1234567890, 2147483648L, 1234567890, 2147483648L);
        init("marginwidth='&nbsp;12' marginheight='+x'", "");
        margins(8, 8, 8, 8);
        init("marginwidth='" + (char) 11 + "12' marginheight='.5'", "");
        margins(8, 8, 8, 8);
    }

    /**
     * Changing/removing the primary attribute exposes only the appropriate fallback; author CSS always overrides hints.
     * <p>상위 속성 변경·제거가 적절한 하위 값만 드러내며 작성자 CSS는 항상 힌트를 덮어쓴다.
     */
    public void testMutationAndAuthorStyle() {
        init("marginwidth=9 leftmargin=3", "");
        margins(8, 9, 8, 9);
        body.setAttribute("marginwidth", "bad");
        margins(8, 8, 8, 8);
        body.removeAttribute("marginwidth");
        margins(8, 8, 8, 3);
        body.setAttribute("leftmargin", "5");
        margins(8, 8, 8, 5);
        body.removeAttribute("leftmargin");
        margins(8, 8, 8, 8);
        init("marginwidth=9 marginheight=10", "body{margin:1px 2px 3px 4px}");
        margins(1, 2, 3, 4);
    }

    /**
     * The resulting margins move actual boxes and painted content, rather than only changing a computed-style string.
     * <p>결과 여백이 계산 스타일 문자열만 바꾸는 대신 실제 상자와 그리는 내용을 옮긴다.
     */
    public void testLayoutAndPixels() {
        init("marginwidth=20 marginheight=15", "");
        RenderedPage page = PageRenderer.render(document, css, null, 200, 120);
        LayoutBox box = page.getLayout().getBoxes(body).get(0);
        Check.near(20, box.getX(), 0.01, "body x");
        Check.near(15, box.getY(), 0.01, "body y");
        BufferedImage image = new BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            page.paint(graphics, null, 0, 0);
        } finally {
            graphics.dispose();
        }
        Check.equal(0x123456, image.getRGB(21, 16) & 0xffffff, "inside shifted content");
        Check.isTrue((image.getRGB(2, 2) & 0xffffff) != 0x123456, "outside margins not content");
    }
}
