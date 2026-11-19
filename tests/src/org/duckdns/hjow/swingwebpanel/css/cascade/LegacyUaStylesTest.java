package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Collections;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.cssom.StyleSheet;
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
 * Verifies static legacy UA styles and explicit wrap opportunities from HTML5 2014 (P07-07-b-4).
 * <p>HTML5 2014의 과거 UA 정적 스타일과 명시적 줄바꿈 기회를 검증한다.
 */
public class LegacyUaStylesTest {
    /** Current parsed document. / 현재 파싱한 문서. */
    private Document document;
    /** Current CSS engine, disposed between cases. / 사례 사이에 해제하는 현재 CSS 엔진. */
    private CssEngine css;

    /** Creates the suite. / 시험 스위트를 만든다. */
    public LegacyUaStylesTest() {
    }

    /** Releases CSS listeners after each case. / 각 사례 후 CSS 리스너를 해제한다. */
    public void tearDown() {
        if (css != null) {
            css.dispose();
            css = null;
        }
    }

    /** Parses real HTML with scripting disabled and predictable font metrics.
     * <p>스크립트를 끄고 예측 가능한 글꼴 메트릭으로 실제 HTML을 파싱한다.
     * @param markup body content / 본문 내용
     * @param rules extra author CSS / 추가 작성자 CSS */
    private void init(String markup, String rules) {
        tearDown();
        document = HtmlParser.parseDocument("<!doctype html><style>html,body{margin:0;padding:0}"
                + "body{font:20px monospace;line-height:normal}" + rules + "</style><body>" + markup, null, null, false);
        css = new CssEngine(document, null, new MediaEnvironment(200, 160));
        css.loadStyleSheets();
    }

    /** Returns the computed style of an element by ID. / ID로 요소의 계산 스타일을 반환한다.
     * @param id element ID / 요소 ID
     * @return computed style / 계산 스타일 */
    private ComputedStyle style(String id) {
        return css.getComputedStyle(document.getElementById(id));
    }

    /** Renders the current document. / 현재 문서를 렌더링한다.
     * @return rendered page / 렌더링한 페이지 */
    private RenderedPage render() {
        return PageRenderer.render(document, css, null, 200, 160);
    }

    /** Returns the first element box. / 요소의 첫 상자를 반환한다.
     * @param page rendered page / 렌더링한 페이지
     * @param id element ID / 요소 ID
     * @return layout box / 레이아웃 상자 */
    private LayoutBox box(RenderedPage page, String id) {
        return page.getLayout().getBoxes(document.getElementById(id)).get(0);
    }

    /** Counts line boxes recursively under a container. / 컨테이너 아래 줄 상자를 재귀적으로 센다.
     * @param box container / 컨테이너
     * @return line count / 줄 개수 */
    private static int lines(LayoutBox box) {
        int count = box.getType() == LayoutBox.Type.LINE ? 1 : 0;
        // 익명 블록도 포함하여 실제 줄 상자를 센다.
        for (LayoutBox child : box.getChildren()) {
            count += lines(child);
        }
        return count;
    }

    /** Paints a display list on a white canvas. / 흰 캔버스에 디스플레이 리스트를 그린다.
     * @param page rendered page / 렌더링한 페이지
     * @return painted image / 그린 이미지 */
    private static BufferedImage paint(RenderedPage page) {
        BufferedImage image = new BufferedImage(200, 160, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setColor(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, 200, 160);
            page.paint(graphics, null, 0, 0);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    /** Relative size, monospace and decoration defaults remain overridable UA styles.
     * <p>상대 크기·고정폭·장식 기본값은 덮어쓸 수 있는 UA 스타일이다. */
    public void testPhrasingDefaultsAndAuthorOverrides() {
        init("<big id=b><big id=bb>x</big></big><small id=m>x</small><tt id=t>x</tt>"
                + "<strike id=k>x</strike><s id=s>x</s><u id=u>x</u><blink id=l>x</blink><nobr id=n>x</nobr>", "");
        Check.near(24, style("b").fontSizePx(), 0.001, "big is relative to parent");
        Check.near(28.8, style("bb").fontSizePx(), 0.001, "nested big scales again");
        Check.near(20 / 1.2, style("m").fontSizePx(), 0.001, "small is relative to parent");
        Check.equal("monospace", style("t").fontFamilies().get(0), "tt monospace");
        Check.equal("line-through", style("k").keyword(Prop.TEXT_DECORATION_LINE), "strike");
        Check.equal("line-through", style("s").keyword(Prop.TEXT_DECORATION_LINE), "s");
        Check.equal("underline", style("u").keyword(Prop.TEXT_DECORATION_LINE), "u");
        Check.equal("blink", style("l").keyword(Prop.TEXT_DECORATION_LINE), "static blink still has the UA decoration value");
        Check.equal("nowrap", style("n").keyword(Prop.WHITE_SPACE), "nobr");
        document.getElementById("l").setAttribute("style", "text-decoration:none");
        document.getElementById("b").setAttribute("style", "font-size:10px");
        Check.equal("none", style("l").keyword(Prop.TEXT_DECORATION_LINE), "author decoration wins");
        Check.near(12, style("bb").fontSizePx(), 0.001, "nested size follows mutation");
    }

    /** Center uses an author hint even without attributes, above normal user CSS and below author CSS.
     * <p>속성이 없는 center도 일반 사용자 CSS보다 높고 작성자 CSS보다 낮은 힌트를 쓴다. */
    public void testCenterHintCascade() {
        init("<center>x</center>", "");
        Element center = (Element) document.getElementsByTagName("center").item(0);
        StyleSheet user = StyleSheet.parse("center{text-align:right}", null, StyleSheet.ORIGIN_USER, null, null);
        css.getResolver().setUserStyleSheets(Collections.singletonList(user));
        Check.equal("center", css.getComputedStyle(center).keyword(Prop.TEXT_ALIGN), "hint wins normal user sheet without attributes");
        center.setAttribute("style", "text-align:left");
        Check.equal("left", css.getComputedStyle(center).keyword(Prop.TEXT_ALIGN), "author rule wins hint");
        center.removeAttribute("style");
        Check.equal("center", css.getComputedStyle(center).keyword(Prop.TEXT_ALIGN), "removing author rule restores hint");
        user = StyleSheet.parse("center{text-align:right!important}", null, StyleSheet.ORIGIN_USER, null, null);
        css.getResolver().setUserStyleSheets(Collections.singletonList(user));
        Check.equal("right", css.getComputedStyle(center).keyword(Prop.TEXT_ALIGN), "important user rule wins hint");
    }

    /** Centering moves an actual inline box and its painted pixels, and an author rule can move it back.
     * <p>가운데 정렬이 실제 인라인 상자와 화소를 옮기고 작성자 규칙으로 되돌릴 수 있다. */
    public void testCenterGeometryAndPixels() {
        init("<center id=c><span id=marker></span></center>",
                "#marker{display:inline-block;width:20px;height:10px;background:#123456}");
        RenderedPage page = render();
        Check.near(90, box(page, "marker").getX(), 0.001, "inline box centered within 200px");
        BufferedImage image = paint(page);
        int y = (int) box(page, "marker").getY() + 2;
        Check.equal(0x123456, image.getRGB(95, y) & 0xffffff, "center pixel");
        Check.equal(0xffffff, image.getRGB(5, y) & 0xffffff, "left area stays empty");
        document.getElementById("c").setAttribute("style", "text-align:right");
        Check.near(180, box(render(), "marker").getX(), 0.001, "author right alignment");
    }

    /** Basefont has no box under HTML5 UA rules; its historical sizing effect remains a separate compatibility task.
     * <p>HTML5 UA 규칙에서 basefont는 상자가 없으며 과거 크기 효과는 별도 호환 작업으로 남긴다. */
    public void testBasefontHiddenDoesNotChangeFontSize() {
        init("<basefont id=base size=7 color=red><span id=text>x</span><font id=relative size=+1>x</font>", "");
        Check.equal("none", style("base").display(), "basefont hidden");
        Check.isTrue(render().getLayout().getBoxes(document.getElementById("base")).isEmpty(), "hidden basefont has no box");
        Check.near(20, style("text").fontSizePx(), 0.001, "following text inherits body");
        Check.near(18, style("relative").fontSizePx(), 0.001, "HTML5 relative size has fixed base three");
    }

    /** Nobr suppresses spaces as wrap points, but its explicit wbr is honored only when needed.
     * <p>nobr는 공백 줄바꿈을 억제하지만 명시적 wbr에서는 필요한 경우에만 줄을 나눈다. */
    public void testNobrExplicitOpportunity() {
        init("<div id=container><nobr><span id=before>abcd</span><wbr id=w><span id=after>efgh</span></nobr></div>",
                "body{font-size:10px}#container{width:30px}");
        Check.equal("normal", style("w").keyword(Prop.WHITE_SPACE), "nobr wbr restores wrapping");
        RenderedPage page = render();
        Check.equal(2, lines(box(page, "container")), "explicit opportunity splits two unbreakable chunks");
        Check.isTrue(box(page, "after").getY() > box(page, "before").getY(), "second chunk below first");
        document.getElementById("container").setAttribute("style", "width:150px");
        Check.equal(1, lines(box(render(), "container")), "wbr does not force a new line when content fits");
    }

    /** Wbr follows its own CSS, is zero-width, and a removed style or display:none changes the opportunity.
     * <p>wbr는 자신의 CSS를 따르고 폭이 없으며 스타일 제거·display:none이 줄바꿈 기회를 바꾼다. */
    public void testWbrStyleMutationAndVisibility() {
        init("<div id=container><nobr>abcd<wbr id=w>efgh</nobr></div>", "body{font-size:10px}#container{width:30px}");
        Element wbr = document.getElementById("w");
        wbr.setAttribute("style", "white-space:nowrap");
        Check.equal(1, lines(box(render(), "container")), "author nowrap suppresses explicit opportunity");
        wbr.removeAttribute("style");
        Check.equal(2, lines(box(render(), "container")), "removal restores UA rule");
        wbr.setAttribute("style", "display:none");
        Check.equal(1, lines(box(render(), "container")), "hidden wbr has no opportunity");
        wbr.removeAttribute("style");
        Check.isTrue(render().getLayout().getBoxes(wbr).isEmpty(), "opportunity has no glyph box");
    }

    /** A regular wbr inherits white-space, while a nobr without wbr stays on one overflowing line.
     * <p>일반 wbr는 white-space를 상속하며 wbr 없는 nobr는 넘치는 한 줄을 유지한다. */
    public void testWbrOutsideNobrAndPlainNoWrap() {
        init("<div id=container>abcd<wbr id=w>efgh</div><div id=plain><nobr>abcd efgh ijkl</nobr></div>",
                "body{font-size:10px}div{width:30px}");
        Check.equal(2, lines(box(render(), "container")), "regular wbr allows breaking");
        Check.equal(1, lines(box(render(), "plain")), "nobr spaces do not wrap");
        document.getElementById("container").setAttribute("style", "white-space:nowrap");
        Check.equal(1, lines(box(render(), "container")), "regular wbr respects inherited nowrap");
    }

    /** Leading, trailing and consecutive opportunities neither force empty lines nor create visible content.
     * <p>처음·마지막·연속 줄바꿈 기회는 빈 줄을 강제하거나 보이는 내용을 만들지 않는다. */
    public void testWbrEdgesAndIntrinsicSize() {
        init("<div id=empty><nobr><wbr><wbr></nobr></div><div id=container><nobr><wbr>abcd<wbr><wbr>efgh<wbr></nobr></div>"
                + "<span id=atomic style='display:inline-block'><nobr>abcd<wbr>efgh</nobr></span>",
                "body{font-size:10px}div{width:30px}#atomic{max-width:30px}");
        RenderedPage page = render();
        Check.equal(0, lines(box(page, "empty")), "only opportunities create no line");
        Check.equal(2, lines(box(page, "container")), "edge opportunities create no extra lines");
        Check.isTrue(box(page, "atomic").getContentWidth() <= 30.001, "min-content respects opportunity during shrink-to-fit");
        Check.equal(2, lines(box(page, "atomic")), "shrink-to-fit box wraps");
    }

    /** Empty inline edges after an opportunity do not consume it before the following atomic box.
     * <p>줄바꿈 기회 뒤의 빈 인라인 가장자리가 다음 원자 상자 전에 기회를 소모하지 않는다. */
    public void testWbrBeforeAtomicAndEmptyInline() {
        init("<div id=container><nobr><span id=before>abcd</span><wbr><em></em><span><i id=after></i></span></nobr></div>",
                "body{font-size:10px}#container{width:30px}#after{display:inline-block;width:24px;height:10px}");
        RenderedPage page = render();
        Check.equal(2, lines(box(page, "container")), "opportunity survives empty inline edges");
        Check.isTrue(box(page, "after").getY() > box(page, "before").getY(), "atomic content on second line");
    }

    /** First-line probing preserves explicit opportunities and keeps the second line's ordinary style.
     * <p>첫 줄 탐색이 명시적 줄바꿈 기회를 유지하고 둘째 줄은 보통 스타일을 쓴다. */
    public void testWbrWithFirstLine() {
        init("<div id=container><nobr><span id=before>abcd</span><wbr><span id=after>efgh</span></nobr></div>",
                "body{font-size:10px}#container{width:30px}#container::first-line{color:red}");
        RenderedPage page = render();
        Check.equal(2, lines(box(page, "container")), "first-line probe retains wbr");
        LayoutBox before = page.getLayout().getBoxes(document.getElementById("before").getFirstChild()).get(0);
        LayoutBox after = page.getLayout().getBoxes(document.getElementById("after").getFirstChild()).get(0);
        Check.equal("rgb(255, 0, 0)", before.getStyle().getPropertyValue("color"), "first line color");
        Check.equal("rgb(0, 0, 0)", after.getStyle().getPropertyValue("color"), "second line color");
    }

    /** Wbr allows a break before a nowrap chunk, without enabling emergency breaks inside that chunk.
     * <p>wbr는 nowrap 덩어리 앞의 줄바꿈을 허용하지만 그 내부의 강제 단어 나누기는 허용하지 않는다. */
    public void testWbrKeepsFollowingNoWrapChunkIntact() {
        init("<div id=container><nobr><span id=before>ab</span><wbr><span id=after>abcdefghijk</span></nobr></div>",
                "body{font-size:10px;overflow-wrap:break-word}#container{width:30px}");
        RenderedPage page = render();
        Check.equal(2, lines(box(page, "container")), "only the explicit opportunity may break nowrap");
        Check.equal(1, page.getLayout().getBoxes(document.getElementById("after").getFirstChild()).size(),
                "long nowrap chunk stays one text fragment");
        Check.isTrue(box(page, "after").getWidth() > 30, "long chunk overflows intact");
    }

    /** Monospace and relative font sizes affect real text and em-based boxes, not only computed strings.
     * <p>고정폭과 상대 글꼴 크기가 계산 문자열뿐 아니라 실제 텍스트와 em 기반 상자에 적용된다. */
    public void testFontGeometryAndMutation() {
        init("<big id=big><span id=marker></span></big><tt id=t>iiii</tt>",
                "body{font-family:serif}#marker{display:inline-block;width:2em;height:1em}");
        RenderedPage page = render();
        Check.near(48, box(page, "marker").getContentWidth(), 0.001, "two em relative to big");
        LayoutBox text = page.getLayout().getBoxes(document.getElementById("t").getFirstChild()).get(0);
        Check.equal("Monospaced", text.getFont().getFamily(), "tt uses the public JRE monospace font");
        document.getElementById("big").setAttribute("style", "font-size:10px");
        Check.near(20, box(render(), "marker").getContentWidth(), 0.001, "updated em geometry");
    }

    /** Checks for a solid colored decoration row spanning text, including gaps between glyphs.
     * <p>글리프 사이 공백까지 텍스트 전체에 걸친 단색 장식 행을 확인한다.
     * @param image painted image / 그린 이미지
     * @param text text box / 텍스트 상자
     * @param from first row / 첫 행
     * @param to exclusive last row / 마지막 행의 다음
     * @return true if a full decoration row exists / 전체 장식 행이 있으면 true */
    private static boolean decorationRow(BufferedImage image, LayoutBox text, int from, int to) {
        // 후보 행마다 글자 사이 빈 공간까지 장식 색으로 채워져 있는지 확인한다.
        for (int y = Math.max(0, from); y < Math.min(image.getHeight(), to); y++) {
            boolean solid = true;
            // 끝 가장자리의 래스터화 차이를 피하고 내부 구간만 비교한다.
            for (int x = (int) Math.ceil(text.getX()) + 1; x < (int) Math.floor(text.getX() + text.getWidth()) - 1; x++) {
                if ((image.getRGB(x, y) & 0xffffff) != 0x123456) {
                    solid = false;
                    break;
                }
            }
            if (solid) {
                return true;
            }
        }
        return false;
    }

    /** Legacy underline and strike tags paint decoration rows; blink remains visible static text under project policy.
     * <p>과거 밑줄·취소선 태그가 장식 행을 그리고 blink는 프로젝트 정책대로 보이는 정적 텍스트를 유지한다. */
    public void testDecorationPixelsAndStaticBlink() {
        init("<u id=u>i i i i</u><strike id=k>i i i i</strike><s id=s>i i i i</s><blink id=l>i i i i</blink>",
                "u,strike,s,blink{display:block;white-space:pre;color:#123456;line-height:32px}");
        RenderedPage page = render();
        BufferedImage image = paint(page);
        String[] ids = {"u", "k", "s"};
        // 각 태그의 실제 텍스트 조각 장식 비트와 장식 위치 화소를 함께 확인한다.
        for (int i = 0; i < ids.length; i++) {
            LayoutBox text = page.getLayout().getBoxes(document.getElementById(ids[i]).getFirstChild()).get(0);
            int expected = i == 0 ? LayoutBox.DECORATION_UNDERLINE : LayoutBox.DECORATION_LINE_THROUGH;
            Check.equal(expected, text.getTextDecoration(), "decoration reaches layout " + ids[i]);
            int baseline = (int) Math.round(text.getBaseline());
            Check.isTrue(decorationRow(image, text, i == 0 ? baseline + 1 : baseline - (int) Math.ceil(text.getAscent()),
                    i == 0 ? baseline + 8 : baseline - 1), "decoration pixels " + ids[i]);
        }
        LayoutBox blink = page.getLayout().getBoxes(document.getElementById("l").getFirstChild()).get(0);
        Check.isTrue(blink.getWidth() > 0, "static blink has text");
        document.getElementById("l").setAttribute("style", "text-decoration:none");
        BufferedImage withoutBlink = paint(render());
        // blink 장식을 제거해도 정적 그림은 같아야 하며 장식만으로 타이머를 만들지 않는다.
        for (int y = 0; y < image.getHeight(); y++) {
            // 모든 열을 비교하여 정적 blink가 일반 텍스트와 같은 그림인지 확인한다.
            for (int x = 0; x < image.getWidth(); x++) {
                Check.equal(image.getRGB(x, y), withoutBlink.getRGB(x, y), "static blink pixel");
            }
        }
    }
}
