package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.css.selector.SimpleElementState;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.layout.LayoutBox;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the document mode dependent rendering (plan item P07-07-b-10): the quirks that HTML5 2014 section 10 and the Quirks Mode
 * Standard (WHATWG Living Standard of 2026-10-19) define, with the mode that the DOCTYPE selects. Covered: the margin of <code>form</code>,
 * the margin collapsing quirks, the unitless length and the hashless hex color, the table color from the body, the <code>:hover</code> and
 * <code>:active</code> quirk, the html and body elements that fill the viewport, the line height quirks (the gap below images),
 * the collapsing table and the decoration that does not propagate into tables.
 * <p>
 * 문서 모드에 따른 렌더링 시험(계획 P07-07-b-10): HTML5 2014 §10 과 호환 모드 표준(WHATWG Living Standard 2026-10-19)이 정한 quirk 와 DOCTYPE 이 고르는 모드.
 * 다루는 것: <code>form</code> 의 margin, 여백 병합 quirk, 단위 없는 길이와 # 없는 16진수 색, 본문에서 받는 표의 색, <code>:hover</code> 와
 * <code>:active</code> quirk, 뷰포트를 채우는 html 과 body, 줄 높이 quirk(이미지 아래 간격), collapsing table, 표로 전파되지 않는 장식.
 */
public class LegacyQuirksTest {

    /** The shared fixture. / 공용 도구. */
    private final LegacyFixture fx = new LegacyFixture();

    /** DOCTYPE of the limited-quirks mode (XHTML 1.0 Transitional). / 제한적 호환 모드의 DOCTYPE(XHTML 1.0 Transitional). */
    private static final String LIMITED = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyQuirksTest() {
    }

    /**
     * Releases the CSS engine after each case.
     * <p>
     * 각 사례 뒤에 CSS 엔진을 해제한다.
     */
    public void tearDown() {
        fx.dispose();
    }

    /**
     * The DOCTYPE selects the document mode that the other tests rely on.
     * <p>
     * DOCTYPE 이 다른 시험이 의존하는 문서 모드를 고른다.
     */
    public void testDocumentModes() {
        String[] prefixes = { "", "<!doctype html>", LIMITED, "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">",
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">",
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">",
                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">", "<!DOCTYPE html SYSTEM \"about:legacy-compat\">" };
        int[] expected = { Document.QUIRKS_MODE, Document.NO_QUIRKS_MODE, Document.LIMITED_QUIRKS_MODE, Document.QUIRKS_MODE,
                Document.LIMITED_QUIRKS_MODE, Document.NO_QUIRKS_MODE, Document.QUIRKS_MODE, Document.NO_QUIRKS_MODE };
        // DOCTYPE 마다 문서를 해석해 모드를 확인한다.
        for (int i = 0; i < prefixes.length; i++) {
            fx.parse(prefixes[i], "<p>x</p>", "");
            Check.equal(expected[i], fx.document().getCompatibilityMode(), "mode of " + prefixes[i]);
        }
    }

    /**
     * In quirks mode a form has a bottom margin of 1em (HTML5 2014 section 10.3.3).
     * <p>
     * 호환 모드의 폼은 아래 margin 이 1em 이다(HTML5 2014 §10.3.3).
     */
    public void testFormMargin() {
        String markup = "<form id=f>x</form><div id=d><form id=g>y</form></div>";
        fx.initQuirks(markup, "");
        Check.near(20, fx.style("f").px(Prop.MARGIN_BOTTOM), 0.001, "quirks: form margin");
        Check.near(0, fx.style("f").px(Prop.MARGIN_TOP), 0.001, "quirks: no top margin");
        Check.near(20, fx.style("g").px(Prop.MARGIN_BOTTOM), 0.001, "quirks: nested form");
        fx.init(markup, "");
        Check.near(0, fx.style("f").px(Prop.MARGIN_BOTTOM), 0.001, "standards: no margin");
        fx.parse(LIMITED, markup, "");
        Check.near(0, fx.style("f").px(Prop.MARGIN_BOTTOM), 0.001, "limited quirks: no margin");
    }

    /**
     * In quirks mode text fields and text areas use the border box (HTML5 2014 section 10.3.11), and image buttons keep the content box.
     * <p>
     * 호환 모드에서 텍스트 필드와 텍스트 영역은 border box 를 쓰고(HTML5 2014 §10.3.11) 이미지 버튼은 content box 를 유지한다.
     */
    public void testControlBoxSizing() {
        String markup = "<input id=i type=text><textarea id=t></textarea><input id=g type=image>";
        fx.initQuirks(markup, "");
        Check.equal("border-box", fx.style("i").keyword(Prop.BOX_SIZING), "quirks: text input");
        Check.equal("border-box", fx.style("t").keyword(Prop.BOX_SIZING), "quirks: textarea");
        Check.equal("content-box", fx.style("g").keyword(Prop.BOX_SIZING), "quirks: image button");
        fx.init(markup, "");
        Check.equal("content-box", fx.style("i").keyword(Prop.BOX_SIZING), "standards: text input");
    }

    /**
     * The margin collapsing quirks (HTML5 2014 section 10.3.10): the first element with default margins in a body or a cell loses its top
     * margin, a blank one also its bottom margin, and the paragraph at the end of a cell loses its bottom margin.
     * <p>
     * 여백 병합 quirk(HTML5 2014 §10.3.10): body 나 셀의 첫 기본 여백 요소는 위쪽 margin 을 잃고, 비어 있으면 아래쪽도 잃으며, 셀 끝의 문단은 아래쪽 margin 을 잃는다.
     */
    public void testMarginCollapsingQuirks() {
        String markup = "<p id=a>x</p><p id=b>y</p><div><p id=i>x</p></div><table><tr><td id=td1><p id=d>x</p></td>"
                + "<td><p id=e>x</p>text</td><td><h1 id=f>x</h1> <p id=g>y</p></td><td>text<ul id=h><li>x</ul></td>"
                + "<td><p id=j>x</p><p id=k> </p></td></tr></table>";
        fx.initQuirks(markup, "");
        Check.near(0, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "first paragraph of the body: top");
        Check.near(20, fx.style("a").px(Prop.MARGIN_BOTTOM), 0.001, "first paragraph of the body: bottom");
        Check.near(20, fx.style("b").px(Prop.MARGIN_TOP), 0.001, "second paragraph keeps its top margin");
        Check.near(20, fx.style("i").px(Prop.MARGIN_TOP), 0.001, "a paragraph in a div is not a child of the body");
        Check.near(0, fx.style("d").px(Prop.MARGIN_TOP), 0.001, "only paragraph of a cell: top");
        Check.near(0, fx.style("d").px(Prop.MARGIN_BOTTOM), 0.001, "only paragraph of a cell: bottom");
        Check.near(0, fx.style("e").px(Prop.MARGIN_TOP), 0.001, "first paragraph of a cell: top");
        Check.near(16, fx.style("e").px(Prop.MARGIN_BOTTOM), 0.001, "a substantial text follows: bottom stays (1em of the medium font of a quirks table)");
        Check.near(0, fx.style("f").px(Prop.MARGIN_TOP), 0.001, "first heading of a cell: top");
        Check.near(fx.style("f").fontSizePx() * 0.67, fx.style("f").px(Prop.MARGIN_BOTTOM), 0.01, "heading bottom stays");
        Check.near(16, fx.style("g").px(Prop.MARGIN_TOP), 0.001, "an element precedes: top stays");
        Check.near(0, fx.style("g").px(Prop.MARGIN_BOTTOM), 0.001, "last paragraph of a cell: bottom");
        Check.near(16, fx.style("h").px(Prop.MARGIN_TOP), 0.001, "a text precedes the list: top stays");
        Check.near(16, fx.style("h").px(Prop.MARGIN_BOTTOM), 0.001, "a list at the end of a cell keeps its bottom margin");
        Check.near(0, fx.style("k").px(Prop.MARGIN_TOP), 0.001, "blank last paragraph of a cell: top");
        Check.near(0, fx.style("k").px(Prop.MARGIN_BOTTOM), 0.001, "blank last paragraph of a cell: bottom");
        Check.near(16, fx.style("j").px(Prop.MARGIN_BOTTOM), 0.001, "not the last one");
        fx.init(markup, "");
        Check.near(20, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "standards: no quirk");
        Check.near(20, fx.style("d").px(Prop.MARGIN_BOTTOM), 0.001, "standards: no quirk in cells");
        fx.parse(LIMITED, markup, "");
        Check.near(20, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "limited quirks: no quirk");
        fx.initQuirks(markup, "p{margin-top:9px}");
        Check.near(9, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "author CSS wins over the quirk");
    }

    /**
     * The margin quirks follow DOM changes: text before the first paragraph makes its top margin return, and emptying a paragraph
     * removes its bottom margin.
     * <p>
     * 여백 quirk 는 DOM 변경을 따른다: 첫 문단 앞에 텍스트가 오면 위쪽 margin 이 돌아오고 문단을 비우면 아래쪽 margin 이 없어진다.
     */
    public void testMarginQuirksFollowMutation() {
        fx.initQuirks("<p id=a>x</p><p id=b>y</p>", "");
        Check.near(0, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "before");
        Check.near(20, fx.style("a").px(Prop.MARGIN_BOTTOM), 0.001, "bottom before");
        Element body = fx.element("a").getParentElement();
        body.insertBefore(fx.document().createTextNode("text"), fx.element("a"));
        Check.near(20, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "a substantial text precedes now");
        body.removeChild(body.getFirstChild());
        Check.near(0, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "the text is gone");
        fx.element("a").setTextContent("");
        Check.near(0, fx.style("a").px(Prop.MARGIN_BOTTOM), 0.001, "an emptied paragraph is blank: bottom");
        fx.element("a").setTextContent("again");
        Check.near(20, fx.style("a").px(Prop.MARGIN_BOTTOM), 0.001, "filled again: bottom returns");
    }

    /**
     * In quirks mode the properties of the Quirks Mode Standard (CSS Values 4 appendix C) accept a unitless number as a length, in style
     * sheets and in the <code>style</code> attribute; other properties and shorthands do not.
     * <p>
     * 호환 모드에서는 호환 모드 표준(CSS Values 4 부록 C)의 속성이 스타일 시트와 <code>style</code> 속성에서 단위 없는 숫자를 길이로 받고 다른 속성과 단축 속성은 받지 않는다.
     */
    public void testUnitlessLengths() {
        String markup = "<div id=a style='width:100;height:50;margin:5 6;padding:3;font-size:12;border-width:2;border-style:solid;letter-spacing:1;"
                + "text-indent:9;border-spacing:3'></div><div id=b></div><div id=c></div>";
        String rules = "#b{margin-left:-4.5;vertical-align:3;top:7;max-width:90;min-height:11;word-spacing:2;line-height:2;z-index:3}"
                + "#c{border:1 solid red;outline-width:3;flex-basis:10;margin-top:5;width:50%;height:12px}";
        fx.initQuirks(markup, rules);
        ComputedStyle a = fx.style("a");
        Check.near(100, a.px(Prop.WIDTH), 0.001, "width");
        Check.near(50, a.px(Prop.HEIGHT), 0.001, "height");
        Check.near(5, a.px(Prop.MARGIN_TOP), 0.001, "margin top");
        Check.near(6, a.px(Prop.MARGIN_RIGHT), 0.001, "margin right");
        Check.near(3, a.px(Prop.PADDING_LEFT), 0.001, "padding");
        Check.near(12, a.fontSizePx(), 0.001, "font-size");
        Check.near(2, a.px(Prop.BORDER_TOP_WIDTH), 0.001, "border-width");
        Check.near(1, a.px(Prop.LETTER_SPACING), 0.001, "letter-spacing");
        Check.near(9, a.px(Prop.TEXT_INDENT), 0.001, "text-indent");
        Check.equal("3px", a.getPropertyValue("border-spacing"), "border-spacing");
        ComputedStyle b = fx.style("b");
        Check.near(-4.5, b.px(Prop.MARGIN_LEFT), 0.001, "negative decimal margin");
        Check.near(90, b.px(Prop.MAX_WIDTH), 0.001, "max-width");
        Check.near(11, b.px(Prop.MIN_HEIGHT), 0.001, "min-height");
        Check.near(2, b.px(Prop.WORD_SPACING), 0.001, "word-spacing");
        Check.near(2, b.number(Prop.LINE_HEIGHT), 0.001, "line-height stays a multiplier");
        Check.near(3, b.number(Prop.Z_INDEX), 0.001, "z-index is a number");
        ComputedStyle c = fx.style("c");
        Check.equal("none", c.keyword(Prop.BORDER_TOP_STYLE), "the border shorthand does not take quirky lengths");
        Check.near(0, c.px(Prop.OUTLINE_WIDTH), 0.001, "outline-width is not in the list");
        Check.near(5, c.px(Prop.MARGIN_TOP), 0.001, "margin-top");
        Check.equal("50%", c.getPropertyValue("width"), "percentages are untouched");
        Check.near(12, c.px(Prop.HEIGHT), 0.001, "lengths with units are untouched");
        fx.init(markup, rules);
        Check.equal("auto", fx.style("a").getPropertyValue("width"), "standards: a unitless width is ignored");
        Check.near(20, fx.style("a").fontSizePx(), 0.001, "standards: a unitless font-size is ignored (inherited 20px)");
        Check.near(0, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "standards: a unitless margin is ignored");
        Check.near(0, fx.style("b").px(Prop.MARGIN_LEFT), 0.001, "standards: sheet rule ignored");
        fx.parse(LIMITED, markup, rules);
        Check.equal("auto", fx.style("a").getPropertyValue("width"), "limited quirks: ignored");
    }

    /**
     * In quirks mode <code>color</code>, <code>background-color</code> and the border colors accept a hex color without the hash:
     * an identifier of three or six hex digits, and the integer numbers and dimensions that serialize into digits (padded to six).
     * <p>
     * 호환 모드에서 <code>color</code>, <code>background-color</code>, border 색은 # 없는 16진수 색을 받는다: 16진수 3자리나 6자리 식별자, 숫자로 풀어 쓰이는
     * 정수와 차원(6자리로 채움).
     */
    public void testHashlessColors() {
        String markup = "<p id=a style='color:ff0000'>x</p><p id=b style='color:f00'>x</p><p id=c style='background-color:00ff00'>x</p>"
                + "<p id=d style='color:123456'>x</p><p id=e style='color:123'>x</p><p id=f style='background-color:abcdef;border-color:0000ff'>x</p>"
                + "<p id=g style='color:ffff'>x</p><p id=h style='color:1234567'>x</p><p id=i style='color:-1'>x</p><p id=j style='color:red'>x</p>"
                + "<p id=k style='background:ff0000'>x</p><p id=l style='color:12px'>x</p><p id=m style='color:rgb(1 2 3)'>x</p>";
        fx.initQuirks(markup, "#n{color:00ff00}");
        Check.equal(0xFFFF0000, fx.style("a").color(Prop.COLOR), "six digit identifier");
        Check.equal(0xFFFF0000, fx.style("b").color(Prop.COLOR), "three digit identifier: #f00");
        Check.equal(0xFF00FF00, fx.style("c").color(Prop.BACKGROUND_COLOR), "dimension 00 + ff00");
        Check.equal(0xFF123456, fx.style("d").color(Prop.COLOR), "integer number of six digits");
        Check.equal(0xFF000123, fx.style("e").color(Prop.COLOR), "a number with fewer digits is padded to six");
        Check.equal(0xFFABCDEF, fx.style("f").color(Prop.BACKGROUND_COLOR), "background-color");
        Check.equal(0xFF0000FF, fx.style("f").color(Prop.BORDER_TOP_COLOR), "border color 0000ff");
        Check.equal(0xFF000000, fx.style("g").color(Prop.COLOR), "four digits are not a color");
        Check.equal(0xFF000000, fx.style("h").color(Prop.COLOR), "seven digits are not a color");
        Check.equal(0xFF000000, fx.style("i").color(Prop.COLOR), "a negative number is not a color");
        Check.equal(0xFFFF0000, fx.style("j").color(Prop.COLOR), "keywords still work");
        Check.equal(0x00000000, fx.style("k").color(Prop.BACKGROUND_COLOR), "the background shorthand does not take it");
        Check.equal(0xFF000000, fx.style("l").color(Prop.COLOR), "12px is not hexadecimal");
        Check.equal(0xFF010203, fx.style("m").color(Prop.COLOR), "functions are untouched");
        fx.init(markup, "");
        Check.equal(0xFF000000, fx.style("a").color(Prop.COLOR), "standards: ignored");
        Check.equal(0x00000000, fx.style("c").color(Prop.BACKGROUND_COLOR), "standards: ignored (background)");
    }

    /**
     * In quirks mode a table without its own color takes the color of the body element, not the color of its surroundings, and its cells
     * inherit that (Quirks Mode Standard 3.12).
     * <p>
     * 호환 모드에서 자기 색이 없는 표는 주변이 아니라 body 요소의 색을 받고 그 셀이 그것을 상속한다(호환 모드 표준 3.12).
     */
    public void testTableColorFromBody() {
        String markup = "<div style='color:#00ff00'><table id=t><tr><td id=c>x</td></tr></table><table id=u style='color:#0000ff'><tr><td id=d>y</td></tr></table></div>";
        fx.initQuirks(markup, "body{color:#ff0000}");
        Check.equal(0xFFFF0000, fx.style("t").color(Prop.COLOR), "the table takes the body color");
        Check.equal(0xFFFF0000, fx.style("c").color(Prop.COLOR), "its cell inherits it");
        Check.equal(0xFF0000FF, fx.style("u").color(Prop.COLOR), "an own color wins");
        Check.equal(0xFF0000FF, fx.style("d").color(Prop.COLOR), "cell of the own color");
        fx.element("t").getParentElement().setAttribute("style", "color:#ffff00");
        Check.equal(0xFFFF0000, fx.style("t").color(Prop.COLOR), "the table still takes the body color after a change of the surroundings");
        fx.init(markup, "body{color:#ff0000}");
        Check.equal(0xFF00FF00, fx.style("t").color(Prop.COLOR), "standards: inherited from the div");
    }

    /**
     * In quirks mode a selector made only of <code>:hover</code> or <code>:active</code> matches links only; selectors that name a type,
     * class, id or attribute, and the arguments of functional pseudo-classes, are not affected (Quirks Mode Standard 4.1).
     * <p>
     * 호환 모드에서 <code>:hover</code> 나 <code>:active</code> 만으로 된 선택자는 링크에만 일치한다. 타입, 클래스, id, 속성을 말하는 선택자와 함수형 의사 클래스의 인수는
     * 영향받지 않는다(호환 모드 표준 4.1).
     */
    public void testHoverActiveQuirk() {
        String markup = "<a id=a href=x>x</a><span id=s class=k>y</span><span id=t>z</span><div id=w class=w><b id=hb>h</b></div>";
        String rules = ":hover{color:#ff0000} .k:hover{background-color:#00ff00} :active{border-top-color:#0000ff} :is(:hover){text-decoration:underline}"
                + " *:hover{border-bottom-color:#ffff00} .w:has(:hover){border-left-color:#ff00ff}";
        fx.initQuirks(markup, rules);
        SimpleElementState state = new SimpleElementState();
        fx.css().getResolver().setElementState(state);
        Element link = fx.element("a");
        Element span = fx.element("s");
        Element plain = fx.element("t");
        state.setHover(link);
        fx.css().getResolver().stateChanged(link);
        Check.equal(0xFFFF0000, fx.style("a").color(Prop.COLOR), "a link matches :hover");
        state.setHover(span);
        fx.css().getResolver().stateChanged(span);
        Check.equal(0xFF000000, fx.style("s").color(Prop.COLOR), "a non-link does not match :hover in quirks mode");
        Check.equal(0xFF00FF00, fx.style("s").color(Prop.BACKGROUND_COLOR), "a selector with a class is not affected");
        Check.equal("underline", fx.style("s").keyword(Prop.TEXT_DECORATION_LINE), "the argument of :is() is not affected");
        Check.equal(0xFF000000, fx.style("s").color(Prop.BORDER_BOTTOM_COLOR), "*:hover is a compound of :hover only");
        state.setActive(plain);
        fx.css().getResolver().stateChanged(plain);
        Check.equal(0xFF000000, fx.style("t").color(Prop.BORDER_TOP_COLOR), ":active does not match a non-link either");
        Element inner = fx.element("hb");
        state.setHover(inner);
        fx.css().getResolver().stateChanged(inner);
        Check.equal(0xFFFF00FF, fx.style("w").color(Prop.BORDER_LEFT_COLOR), "the argument of :has() is not affected");
        fx.init(markup, rules);
        state = new SimpleElementState();
        fx.css().getResolver().setElementState(state);
        span = fx.element("s");
        state.setHover(span);
        fx.css().getResolver().stateChanged(span);
        Check.equal(0xFFFF0000, fx.style("s").color(Prop.COLOR), "standards: the non-link matches");
    }

    /**
     * Returns the border box of the first box of an element.
     * <p>
     * 요소의 첫 상자를 반환한다.
     *
     * @param page the rendered page / 렌더링한 페이지
     * @param e    the element / 요소
     * @return the box / 상자
     */
    private static LayoutBox boxOf(RenderedPage page, Element e) {
        return page.getLayout().getBoxes(e).get(0);
    }

    /**
     * In quirks mode the html element fills the viewport and the body fills the html element (Quirks Mode Standard 3.6 and 3.7); in
     * standards mode both are as high as their content.
     * <p>
     * 호환 모드에서 html 요소가 뷰포트를 채우고 body 가 html 을 채운다(호환 모드 표준 3.6, 3.7). 표준 모드에서는 둘 다 내용만큼의 높이이다.
     */
    public void testHtmlAndBodyFillTheViewport() {
        String rules = "body{margin:8px;border:3px solid #ff0000}";
        fx.initQuirks("<p style='margin:0'>x</p>", rules);
        RenderedPage page = fx.render();
        LayoutBox html = boxOf(page, fx.document().getDocumentElement());
        LayoutBox body = boxOf(page, fx.document().getElementsByTagName("body").item(0));
        Check.near(LegacyFixture.HEIGHT, html.getHeight(), 0.5, "quirks: html fills the viewport");
        Check.near(LegacyFixture.HEIGHT - 16, body.getHeight(), 0.5, "quirks: body fills html minus its margins");
        BufferedImage image = LegacyFixture.paint(page);
        // 호환 모드의 body 는 아래쪽 border 가 뷰포트 아래쪽 가까이에 그려진다.
        Check.equal(0xFF0000, image.getRGB(60, LegacyFixture.HEIGHT - 8 - 2) & 0xFFFFFF, "quirks: the bottom border of the body is painted near the bottom");
        fx.init("<p style='margin:0'>x</p>", rules);
        page = fx.render();
        body = boxOf(page, fx.document().getElementsByTagName("body").item(0));
        Check.isTrue(body.getHeight() < 60, "standards: body is as high as its content: " + body.getHeight());
        fx.initQuirks("<p style='margin:0'>x</p>", "body{height:50px;margin:0}");
        page = fx.render();
        Check.near(50, boxOf(page, fx.document().getElementsByTagName("body").item(0)).getHeight(), 0.5, "quirks: a specified height wins");
        fx.initQuirks("<p style='margin:0'>x</p>", "body{position:absolute;margin:0}");
        page = fx.render();
        Check.isTrue(boxOf(page, fx.document().getElementsByTagName("body").item(0)).getHeight() < 60, "quirks: an absolutely positioned body is not stretched");
        fx.parse(LIMITED, "<p style='margin:0'>x</p>", rules);
        page = fx.render();
        Check.isTrue(boxOf(page, fx.document().getElementsByTagName("body").item(0)).getHeight() < 60, "limited quirks: body is not stretched");
    }

    /**
     * The fill quirks are part of the layout, not a correction after it: the descendants that depend on the filled size of the body (an
     * absolutely positioned box at its bottom edge, a flex item that grows) see it, and the vertical writing modes fill the width of the
     * viewport (Quirks Mode Standard 3.6 and 3.7 define the size in the block flow direction).
     * <p>
     * 채우기 quirk 는 배치 뒤의 보정이 아니라 배치의 일부이다: body 의 채워진 크기에 의존하는 자손(아래 가장자리에 붙은 절대 배치 상자, 늘어나는 플렉스 항목)이 그 크기를
     * 보고, 세로쓰기는 뷰포트의 너비를 채운다(호환 모드 표준 3.6, 3.7 은 블록 흐름 방향의 크기를 정한다).
     */
    public void testFillReachesDependantsAndVerticalModes() {
        String abs = "<div id=a style='position:absolute;left:0;bottom:0;height:10px;width:10px'></div><p style='margin:0'>x</p>";
        String rules = "body{position:relative}";
        fx.initQuirks(abs, rules);
        RenderedPage page = fx.render();
        LayoutBox a = fx.box(page, "a");
        Check.near(LegacyFixture.HEIGHT, a.getY() + a.getHeight(), 0.5, "quirks: the bottom of the positioned box is the bottom of the filled body");
        fx.init(abs, rules);
        page = fx.render();
        a = fx.box(page, "a");
        Check.isTrue(a.getY() + a.getHeight() < 60, "standards: the body is as high as its content: " + (a.getY() + a.getHeight()));
        String flex = "<div id=f style='flex:1 1 auto'></div>";
        String flexRules = "body{display:flex;flex-direction:column}";
        fx.initQuirks(flex, flexRules);
        page = fx.render();
        Check.near(LegacyFixture.HEIGHT, fx.box(page, "f").getHeight(), 0.5, "quirks: the flex item grows to the filled body");
        fx.init(flex, flexRules);
        page = fx.render();
        Check.near(0, fx.box(page, "f").getHeight(), 0.5, "standards: the flex item has no content");
        fx.initQuirks("<p style='margin:0'>x</p>", "html{padding:5px 0;margin:7px 0} body{margin:4px 0;border-bottom:2px solid red}");
        page = fx.render();
        LayoutBox html = boxOf(page, fx.document().getDocumentElement());
        LayoutBox body = boxOf(page, fx.document().getElementsByTagName("body").item(0));
        Check.near(LegacyFixture.HEIGHT - 14, html.getHeight(), 0.5, "html: the viewport minus the margins of html");
        Check.near(LegacyFixture.HEIGHT - 14 - 10 - 8, body.getHeight(), 0.5, "body: the content box of html minus the margins of body");
        fx.initQuirks("<p style='margin:0'>x</p>", "html{writing-mode:vertical-rl} body{margin:8px}");
        page = fx.render();
        html = boxOf(page, fx.document().getDocumentElement());
        body = boxOf(page, fx.document().getElementsByTagName("body").item(0));
        Check.near(LegacyFixture.WIDTH, html.getWidth(), 0.5, "vertical: html fills the width of the viewport");
        Check.near(LegacyFixture.WIDTH - 16, body.getWidth(), 0.5, "vertical: body fills the html element in the block flow direction");
        fx.init("<p style='margin:0'>x</p>", "html{writing-mode:vertical-rl} body{margin:8px}");
        page = fx.render();
        body = boxOf(page, fx.document().getElementsByTagName("body").item(0));
        Check.isTrue(body.getWidth() < 60, "vertical, standards: body is as wide as its content: " + body.getWidth());
        fx.initQuirks("<p style='margin:0'>x</p>", "html{writing-mode:vertical-rl} body{width:40px;margin:0}");
        page = fx.render();
        Check.near(40, boxOf(page, fx.document().getElementsByTagName("body").item(0)).getWidth(), 0.5, "vertical: a specified width wins");
    }

    /**
     * The height of the content box of a block container.
     * <p>
     * 블록 컨테이너 content box 의 높이.
     *
     * @param markup the body markup / 본문 마크업
     * @param limited true for limited-quirks mode, false for quirks mode / 제한적 호환 모드이면 true, 호환 모드이면 false
     * @param id     the container id / 컨테이너 id
     * @return the height / 높이
     */
    private double heightIn(String markup, boolean limited, String id) {
        if (limited) {
            fx.parse(LIMITED, markup, "");
        } else {
            fx.initQuirks(markup, "");
        }
        return boxOf(fx.render(), fx.element(id)).getContentHeight();
    }

    /**
     * The line height quirks (Quirks Mode Standard 3.3 and 3.4): a line with only an image-like box has no strut, so there is no gap
     * below it, and an inline element without text does not stretch the line; a line with text keeps its line height.
     * <p>
     * 줄 높이 quirk(호환 모드 표준 3.3, 3.4): 이미지 같은 상자뿐인 줄에는 strut 가 없어 아래 간격이 없고, 텍스트 없는 인라인 요소는 줄을 늘이지 않는다. 텍스트가 있는 줄은 줄 높이를 유지한다.
     */
    public void testLineHeightQuirks() {
        String box = "<span style='display:inline-block;width:10px;height:10px'></span>";
        String alone = "<div id=a>" + box + "</div>";
        String inLink = "<div id=b><a href=x>" + box + "</a></div>";
        String withText = "<div id=c>x" + box + "</div>";
        String linkText = "<div id=d><a href=x>link</a></div>";
        String lineBreak = "<div id=e><br></div>";
        String padded = "<div id=f><span style='padding-left:4px'>" + box + "</span></div>";
        String all = alone + inLink + withText + linkText + lineBreak + padded;
        fx.init(all, "");
        RenderedPage standards = fx.render();
        double sa = boxOf(standards, fx.element("a")).getContentHeight();
        double sb = boxOf(standards, fx.element("b")).getContentHeight();
        double sc = boxOf(standards, fx.element("c")).getContentHeight();
        double sd = boxOf(standards, fx.element("d")).getContentHeight();
        double se = boxOf(standards, fx.element("e")).getContentHeight();
        Check.isTrue(sa > 15, "standards: the strut makes the line higher than the box: " + sa);
        Check.near(sa, sb, 0.01, "standards: a link around the box makes no difference");
        // 호환 모드와 제한적 호환 모드에서 같은 기대값을 확인한다.
        for (int mode = 0; mode < 2; mode++) {
            boolean limited = mode == 1;
            String name = limited ? "limited quirks" : "quirks";
            if (limited) {
                fx.parse(LIMITED, all, "");
            } else {
                fx.initQuirks(all, "");
            }
            RenderedPage page = fx.render();
            Check.near(10, boxOf(page, fx.element("a")).getContentHeight(), 0.01, name + ": a line with only a box has no strut");
            Check.near(10, boxOf(page, fx.element("b")).getContentHeight(), 0.01, name + ": an inline element without text does not stretch the line");
            Check.near(sc, boxOf(page, fx.element("c")).getContentHeight(), 0.01, name + ": a line with text keeps its height");
            Check.near(sd, boxOf(page, fx.element("d")).getContentHeight(), 0.01, name + ": a link with text keeps its height");
            Check.near(se, boxOf(page, fx.element("e")).getContentHeight(), 0.01, name + ": an empty line keeps the strut");
            Check.near(sa, boxOf(page, fx.element("f")).getContentHeight(), 0.01, name + ": an inline box with a horizontal edge still counts");
        }
        Check.isTrue(heightIn(alone, false, "a") < sa, "quirks: smaller than standards");
    }

    /**
     * A table without row groups, captions and columns collapses in quirks mode: no border and no height.
     * <p>
     * 행 그룹, 캡션, 열이 없는 표는 호환 모드에서 접힌다: border 도 높이도 없다.
     */
    public void testCollapsingTable() {
        String markup = "<table id=a border=5 style='width:60px'></table><table id=b border=5 style='width:60px'><tbody></tbody></table>"
                + "<table id=c border=5 style='width:60px'><caption>x</caption></table><table id=d border=5 style='width:60px'><colgroup><col></colgroup></table>"
                + "<table id=e border=5 style='width:60px'><colgroup></colgroup></table>";
        fx.initQuirks(markup, "");
        RenderedPage page = fx.render();
        LayoutBox a = boxOf(page, fx.element("a"));
        Check.near(0, a.getHeight(), 0.01, "quirks: the empty table has no height");
        Check.near(0, a.getBorderTop(), 0.01, "quirks: and no border");
        Check.isTrue(boxOf(page, fx.element("b")).getHeight() >= 10, "a table with a row group is not collapsing");
        Check.isTrue(boxOf(page, fx.element("c")).getHeight() >= 10, "a table with a caption is not collapsing");
        Check.isTrue(boxOf(page, fx.element("d")).getHeight() >= 10, "a table with a column is not collapsing");
        Check.near(0, boxOf(page, fx.element("e")).getHeight(), 0.01, "a column group without columns does not count");
        fx.init(markup, "");
        page = fx.render();
        Check.isTrue(boxOf(page, fx.element("a")).getHeight() >= 10, "standards: the empty table keeps its border");
        Check.near(5, boxOf(page, fx.element("a")).getBorderTop(), 0.01, "standards: border");
    }

    /**
     * Collects the decoration bits of the text boxes below a box.
     * <p>
     * 상자 아래 텍스트 상자의 장식 비트를 모은다.
     *
     * @param box the box / 상자
     * @param out receives the bits of each text box / 텍스트 상자마다 비트를 받는다
     */
    private static void textDecorations(LayoutBox box, List<Integer> out) {
        if (box.getType() == LayoutBox.Type.TEXT) {
            out.add(Integer.valueOf(box.getTextDecoration()));
        }
        // 자식을 재귀적으로 훑는다.
        for (LayoutBox c : box.getChildren()) {
            textDecorations(c, out);
        }
    }

    /**
     * The text decoration of an ancestor does not propagate into a table in quirks mode (Quirks Mode Standard 3.11), while text that is
     * not in a table keeps it.
     * <p>
     * 조상의 텍스트 장식은 호환 모드에서 표 안으로 전파되지 않지만(호환 모드 표준 3.11) 표 밖의 텍스트는 유지한다.
     */
    public void testDecorationDoesNotPropagateIntoTables() {
        String markup = "<div style='text-decoration:underline'><span id=o>out</span><table id=t><tr><td id=c>in</td></tr></table></div>";
        fx.initQuirks(markup, "");
        RenderedPage page = fx.render();
        List<Integer> inside = new ArrayList<Integer>();
        textDecorations(boxOf(page, fx.element("t")), inside);
        List<Integer> outside = new ArrayList<Integer>();
        textDecorations(boxOf(page, fx.element("o").getParentElement()), outside);
        Check.isFalse(inside.isEmpty(), "the cell text has a box");
        Check.equal(0, inside.get(0).intValue(), "quirks: no underline in the table");
        Check.isTrue(outside.get(0).intValue() != 0, "text outside the table is underlined");
    }

    /**
     * The arguments of the <code>rect()</code> of <code>clip</code> are quirky lengths in quirks mode: CSS Values 4 appendix C lists
     * <code>clip</code> among the properties whose function <code>rect()</code> is the one function that takes them.
     * <p>
     * 호환 모드에서 <code>clip</code> 의 <code>rect()</code> 인수는 quirky length 이다: CSS Values 4 부록 C 는 함수에는 적용하지 않는다면서
     * <code>clip</code> 의 <code>rect()</code> 만 예외로 꼽는다.
     */
    public void testClipRectQuirk() {
        String markup = "<div id=a style='clip:rect(1,2,3,4)'></div><div id=b style='clip:rect(1 2 3 auto)'></div>"
                + "<div id=c style='clip:rect(1px, 2, 3px, 4)'></div><div id=d style='clip:rect(1,2,3)'></div><div id=e style='clip:5'></div><div id=f></div>";
        fx.initQuirks(markup, "#f{clip:rect(7,8,9,10)}");
        Check.equal("rect(1px, 2px, 3px, 4px)", fx.style("a").getPropertyValue("clip"), "commas");
        Check.equal("rect(1px 2px 3px auto)", fx.style("b").getPropertyValue("clip"), "spaces and auto");
        Check.equal("rect(1px, 2px, 3px, 4px)", fx.style("c").getPropertyValue("clip"), "mixed with lengths that have units");
        Check.equal("auto", fx.style("d").getPropertyValue("clip"), "three arguments are invalid");
        Check.equal("auto", fx.style("e").getPropertyValue("clip"), "a bare number is not a quirky clip");
        Check.equal("rect(7px, 8px, 9px, 10px)", fx.style("f").getPropertyValue("clip"), "a rule of a style sheet");
        fx.init(markup, "");
        Check.equal("auto", fx.style("a").getPropertyValue("clip"), "standards: ignored");
        fx.parse(LIMITED, markup, "");
        Check.equal("auto", fx.style("a").getPropertyValue("clip"), "limited quirks: ignored");
    }

    /**
     * The quirky values are valid in the <code>@supports</code> rule of a quirks mode sheet (CSS Values 4 appendix C: "must be valid when
     * parsing the affected properties in the @supports rule"), so a rule that depends on a unitless length applies there only.
     * <p>
     * 호환 모드 시트의 <code>@supports</code> 규칙에서는 quirky 값이 올바르므로(CSS Values 4 부록 C: "@supports 규칙에서 해당 속성을 파싱할 때 올바라야 한다")
     * 단위 없는 길이에 의존하는 규칙은 호환 모드에서만 적용된다.
     */
    public void testSupportsRuleInQuirksSheet() {
        String markup = "<div id=a></div><div id=b></div>";
        String rules = "@supports (width: 10) { #a { margin-top: 7px } } @supports not (width: 10) { #b { margin-top: 8px } }"
                + "@supports (color: ff0000) { #a { margin-left: 3px } }";
        fx.initQuirks(markup, rules);
        Check.near(7, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "quirks: the unitless length is supported");
        Check.near(0, fx.style("b").px(Prop.MARGIN_TOP), 0.001, "quirks: the negation does not hold");
        Check.near(3, fx.style("a").px(Prop.MARGIN_LEFT), 0.001, "quirks: the hashless color is supported");
        fx.init(markup, rules);
        Check.near(0, fx.style("a").px(Prop.MARGIN_TOP), 0.001, "standards: not supported");
        Check.near(8, fx.style("b").px(Prop.MARGIN_TOP), 0.001, "standards: the negation holds");
        Check.near(0, fx.style("a").px(Prop.MARGIN_LEFT), 0.001, "standards: the hashless color is not supported");
    }
}
