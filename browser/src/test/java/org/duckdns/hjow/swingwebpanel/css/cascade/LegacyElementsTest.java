package org.duckdns.hjow.swingwebpanel.css.cascade;

import java.awt.image.BufferedImage;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.render.RenderedPage;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the obsolete elements with their own parsing or display rules (plan item P07-07-b-9, HTML5 2014 sections 8.2.5.4.7,
 * 10.3.1, 10.3.3 and 10.5.10): <code>plaintext</code>, <code>listing</code>, <code>xmp</code>, <code>isindex</code>,
 * <code>basefont</code>, <code>marquee</code> and <code>pre wrap</code>. The parsed trees are checked first, then computed styles, boxes
 * and painted pixels.
 * <p>
 * 고유한 파싱 또는 표시 규칙이 있는 폐기된 요소의 시험(계획 P07-07-b-9, HTML5 2014 §8.2.5.4.7, §10.3.1, §10.3.3, §10.5.10):
 * <code>plaintext</code>, <code>listing</code>, <code>xmp</code>, <code>isindex</code>, <code>basefont</code>, <code>marquee</code>, <code>pre wrap</code>.
 * 파싱한 트리를 먼저 확인하고 계산 스타일, 상자, 그려진 화소를 확인한다.
 */
public class LegacyElementsTest {

    /** The shared fixture. / 공용 도구. */
    private final LegacyFixture fx = new LegacyFixture();

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyElementsTest() {
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
     * Lists the names of the child nodes of an element, with <code>#text</code> for text nodes.
     * <p>
     * 요소의 자식 노드 이름을 나열한다. 텍스트 노드는 <code>#text</code> 이다.
     *
     * @param e the element / 요소
     * @return the names separated by spaces / 공백으로 구분한 이름
     */
    private static String children(Element e) {
        StringBuilder sb = new StringBuilder();
        // 자식을 차례로 훑으며 이름을 모은다.
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(n instanceof Element ? ((Element) n).getLocalName() : n.getNodeName());
        }
        return sb.toString();
    }

    /**
     * <code>plaintext</code> switches the tokenizer to the PLAINTEXT state for the rest of the input: everything after it is one text
     * node, including markup, character references and the end tag, and it closes an open <code>p</code> element.
     * <p>
     * <code>plaintext</code> 는 입력의 나머지에 대해 토큰화기를 PLAINTEXT 상태로 바꾼다: 그 뒤의 모든 것(마크업, 문자 참조, 종료 태그 포함)이 하나의 텍스트 노드이며
     * 열려 있는 <code>p</code> 요소를 닫는다.
     */
    public void testPlaintextTakesTheRestOfTheInput() {
        fx.init("<p id=p>open<plaintext id=pt><b>bold</b> &amp; </plaintext><div id=d>x</div>", "");
        Element pt = fx.element("pt");
        Check.equal("#text", children(pt), "one text node");
        Check.equal("<b>bold</b> &amp; </plaintext><div id=d>x</div>", pt.getTextContent(), "the rest of the input is raw text");
        Check.isNull(fx.document().getElementById("d"), "markup after plaintext is not parsed");
        Check.equal("body", pt.getParentElement().getLocalName(), "the open p element was closed");
        Check.equal("open", fx.element("p").getTextContent(), "the paragraph keeps its own text");
        ComputedStyle s = fx.style("pt");
        Check.equal("block", s.display(), "display");
        Check.equal("pre", s.keyword(Prop.WHITE_SPACE), "white-space");
        Check.equal("monospace", s.fontFamilies().get(0), "font family");
        Check.near(20, s.px(Prop.MARGIN_TOP), 0.001, "margin top is 1em");
        Check.near(20, s.px(Prop.MARGIN_BOTTOM), 0.001, "margin bottom is 1em");
    }

    /**
     * <code>listing</code> is parsed like <code>pre</code> (a leading line feed is dropped, markup inside is parsed) and shows like it.
     * <p>
     * <code>listing</code> 은 <code>pre</code> 처럼 파싱하고(맨 앞 줄바꿈 하나를 버리고 안의 마크업을 해석한다) 표시도 같다.
     */
    public void testListingLikePre() {
        fx.init("<p id=p>a<listing id=l>\nline1\n<b id=b>x</b></listing><listing id=l2>\n\nsecond</listing>", "");
        Element l = fx.element("l");
        Check.equal("#text b", children(l), "markup inside listing is parsed");
        Check.equal("line1\nx", l.getTextContent(), "the first line feed is dropped");
        Check.equal("\nsecond", fx.element("l2").getTextContent(), "only the first line feed is dropped");
        Check.equal("body", l.getParentElement().getLocalName(), "the open p element was closed");
        ComputedStyle s = fx.style("l");
        Check.equal("block", s.display(), "display");
        Check.equal("pre", s.keyword(Prop.WHITE_SPACE), "white-space");
        Check.equal("monospace", s.fontFamilies().get(0), "font family");
        Check.near(20, s.px(Prop.MARGIN_TOP), 0.001, "margin top");
    }

    /**
     * <code>xmp</code> is a raw text element: markup and character references inside it stay text until the end tag, and it closes an
     * open <code>p</code>.
     * <p>
     * <code>xmp</code> 는 원문 텍스트 요소이다: 안의 마크업과 문자 참조는 종료 태그까지 텍스트로 남고 열려 있는 <code>p</code> 를 닫는다.
     */
    public void testXmpIsRawText() {
        fx.init("<p id=p>a<xmp id=x><b>&amp;</b>\n</xmp>after", "");
        Element x = fx.element("x");
        Check.equal("#text", children(x), "one text node");
        Check.equal("<b>&amp;</b>\n", x.getTextContent(), "raw text, including the character reference");
        Check.equal("body", x.getParentElement().getLocalName(), "the open p element was closed");
        Check.equal("after", x.getNextSibling().getTextContent(), "the text after the end tag is a sibling");
        ComputedStyle s = fx.style("x");
        Check.equal("block", s.display(), "display");
        Check.equal("pre", s.keyword(Prop.WHITE_SPACE), "white-space");
        Check.equal("monospace", s.fontFamilies().get(0), "font family");
        Check.near(20, s.px(Prop.MARGIN_BOTTOM), 0.001, "margin bottom");
    }

    /**
     * The three raw elements paint their text verbatim: the line feeds and the spaces of the source remain, and the text is not
     * interpreted as markup.
     * <p>
     * 세 원문 요소는 글을 그대로 그린다: 원본의 줄바꿈과 공백이 남고 글은 마크업으로 해석되지 않는다.
     */
    public void testRawElementsLayout() {
        fx.init("<xmp id=x>aa\n  bb\n<i>c</i></xmp><listing id=l>\nxx\nyy</listing><plaintext id=p>1\n2\n3", "");
        RenderedPage page = fx.render();
        double line = fx.box(page, "p").getHeight() / 3;
        Check.isTrue(line > 5, "three lines of plaintext: " + fx.box(page, "p").getHeight());
        Check.near(3 * line, fx.box(page, "x").getHeight(), 1.0, "xmp has three lines with the markup as text");
        Check.near(2 * line, fx.box(page, "l").getHeight(), 1.0, "listing has two lines");
        Check.isTrue(fx.box(page, "x").getContentWidth() > 0, "xmp has a width");
    }

    /**
     * <code>isindex</code> expands to a form with two rules, a label with a prompt and an input named <code>isindex</code> (HTML5 2014
     * section 8.2.5.4.7).
     * <p>
     * <code>isindex</code> 는 두 줄, 안내 문구가 있는 label, <code>isindex</code> 라는 이름의 input 이 있는 폼으로 펼쳐진다(HTML5 2014 §8.2.5.4.7).
     */
    public void testIsindexExpands() {
        fx.init("<p id=p>text<isindex id=ignored class=q>after", "");
        Element p = fx.element("p");
        Check.equal("text", p.getTextContent(), "the paragraph was closed before the form");
        Element form = (Element) p.getNextSibling();
        Check.equal("form", form.getLocalName(), "the form follows the paragraph");
        Check.isNull(form.getAttribute("action"), "no action");
        Check.equal("hr label hr", children(form), "form children");
        Element label = (Element) form.getFirstChild().getNextSibling();
        Check.equal("#text input", children(label), "label children");
        Check.equal("This is a searchable index. Enter search keywords: ", label.getFirstChild().getTextContent(), "default prompt");
        Element input = (Element) label.getLastChild();
        Check.equal("isindex", input.getAttribute("name"), "the input is named isindex");
        Check.equal("q", input.getAttribute("class"), "other attributes are kept");
        Check.equal("ignored", input.getAttribute("id"), "the id moves to the input");
        Check.equal("after", form.getNextSibling().getTextContent(), "the text after the isindex is a sibling of the form");
        Check.isNull(input.getAttribute("prompt"), "no prompt attribute");
        Check.equal("block", css(form).display(), "the form is a block");
        Check.equal("block", css(form.getFirstChild()).display(), "the rule is a block");
        Check.equal("inline", css(label).display(), "the label is inline");
        Check.equal("inline-block", css(input).display(), "the input is inline-block");
    }

    /**
     * The <code>prompt</code> and <code>action</code> attributes of <code>isindex</code> and the attributes that the input does not take.
     * <p>
     * <code>isindex</code> 의 <code>prompt</code>, <code>action</code> 속성과 input 이 받지 않는 속성.
     */
    public void testIsindexAttributes() {
        fx.init("<isindex prompt='Find: ' action='/search' name=zz size=9 disabled>", "");
        Element form = (Element) fx.document().getElementsByTagName("form").item(0);
        Check.equal("/search", form.getAttribute("action"), "action goes to the form");
        Element label = (Element) form.getFirstChild().getNextSibling();
        Check.equal("Find: ", label.getFirstChild().getTextContent(), "prompt text");
        Element input = (Element) label.getLastChild();
        Check.equal("isindex", input.getAttribute("name"), "name is replaced");
        Check.equal("9", input.getAttribute("size"), "size is kept");
        Check.isTrue(input.hasAttribute("disabled"), "disabled is kept");
        Check.isNull(input.getAttribute("action"), "no action on the input");
        Check.isNull(input.getAttribute("prompt"), "no prompt on the input");
        Check.equal(3, input.getAttributes().getLength(), "name, size and disabled");
    }

    /**
     * An <code>isindex</code> inside an open form is ignored, and a document without a form can have two of them one after the other.
     * <p>
     * 열린 폼 안의 <code>isindex</code> 는 무시되고, 폼이 없는 문서에는 차례로 둘이 있을 수 있다.
     */
    public void testIsindexInsideForm() {
        fx.init("<form id=f><isindex>x</form><isindex><isindex>", "");
        Element f = fx.element("f");
        Check.equal("#text", children(f), "the isindex inside the form was ignored");
        Check.equal(3, fx.document().getElementsByTagName("form").getLength(), "the form and two expanded forms");
        Check.equal(4, fx.document().getElementsByTagName("hr").getLength(), "two rules for each expanded form");
        Check.equal(2, fx.document().getElementsByTagName("label").getLength(), "one label for each expanded form");
    }

    /**
     * <code>basefont</code> is hidden and has no effect on the font size of the following text, nor on the size that
     * <code>font size="+1"</code> is relative to (HTML5 2014 section 10.3.1 and 10.3.4); author CSS can show it.
     * <p>
     * <code>basefont</code> 는 숨겨지고 뒤따르는 글의 글꼴 크기에도, <code>font size="+1"</code> 이 기준으로 삼는 크기에도 영향이 없다(HTML5 2014 §10.3.1, §10.3.4).
     * 작성자 CSS 는 보이게 할 수 있다.
     */
    public void testBasefontIsHiddenWithoutEffect() {
        fx.init("<basefont id=b size=6 color=red face=Arial>text<font id=f size=+1>x</font><span id=s>y</span>", "basefont{color:blue}");
        Check.equal("none", fx.style("b").display(), "basefont is not displayed");
        Check.near(20, fx.style("s").fontSizePx(), 0.001, "following text keeps its size");
        Check.near(18, fx.style("f").fontSizePx(), 0.001, "font size=+1 is relative to the fixed base 3, not to basefont");
        fx.init("<basefont id=b size=6>x", "basefont{display:inline}");
        Check.equal("inline", fx.style("b").display(), "author CSS can show it");
        Check.near(20, fx.style("b").fontSizePx(), 0.001, "basefont has no font size hint");
    }

    /**
     * <code>source</code> and <code>track</code> are hidden, and a hidden <code>embed</code> keeps a zero sized inline box (HTML5 2014
     * section 10.3.1).
     * <p>
     * <code>source</code> 와 <code>track</code> 은 숨겨지고 숨겨진 <code>embed</code> 는 크기 0 의 인라인 상자를 유지한다(HTML5 2014 §10.3.1).
     */
    public void testHiddenElements() {
        fx.init("<video><source id=s><track id=t></video><embed id=e hidden><embed id=e2><div id=d hidden>x</div>", "");
        Check.equal("none", fx.style("s").display(), "source");
        Check.equal("none", fx.style("t").display(), "track");
        Check.equal("inline", fx.style("e").display(), "embed[hidden] is inline");
        Check.near(0, fx.style("e").px(Prop.WIDTH), 0.001, "embed[hidden] width");
        Check.near(0, fx.style("e").px(Prop.HEIGHT), 0.001, "embed[hidden] height");
        Check.equal("inline", fx.style("e2").display(), "embed without hidden");
        Check.equal("none", fx.style("d").display(), "[hidden]");
    }

    /**
     * <code>pre[wrap]</code> wraps its lines; the other raw elements and a <code>pre</code> without the attribute do not.
     * <p>
     * <code>pre[wrap]</code> 는 줄을 접고, 다른 원문 요소와 속성 없는 <code>pre</code> 는 접지 않는다.
     */
    public void testPreWrap() {
        fx.init("<pre id=a wrap>x</pre><pre id=b>x</pre><listing id=c wrap>x</listing><pre id=d wrap=''>x</pre>", "");
        Check.equal("pre-wrap", fx.style("a").keyword(Prop.WHITE_SPACE), "pre wrap");
        Check.equal("pre", fx.style("b").keyword(Prop.WHITE_SPACE), "pre");
        Check.equal("pre", fx.style("c").keyword(Prop.WHITE_SPACE), "listing wrap has no effect");
        Check.equal("pre-wrap", fx.style("d").keyword(Prop.WHITE_SPACE), "an empty attribute counts");
    }

    /**
     * The static box of a <code>marquee</code>: an inline block with the box attributes mapped, a natural height of 200px for the
     * vertical directions, clipping that author CSS cannot turn off and the text-align reset.
     * <p>
     * <code>marquee</code> 의 정적 상자: 상자 속성이 대응된 인라인 블록, 세로 방향의 자연 높이 200px, 작성자 CSS 가 끌 수 없는 클리핑, text-align 초기화.
     */
    public void testMarqueeStyle() {
        fx.init("<center><marquee id=a bgcolor=#112233 width=150 height=50 hspace=7 vspace=3>x</marquee></center>"
                + "<marquee id=b direction=up>x</marquee><marquee id=c direction=DOWN height=60>x</marquee><marquee id=d direction=left>x</marquee>"
                + "<marquee id=e direction=up height=abc>x</marquee><marquee id=f direction=sideways>x</marquee>"
                + "<marquee id=g width=40%>x</marquee>", "marquee{overflow:visible}");
        ComputedStyle a = fx.style("a");
        Check.equal("inline-block", a.display(), "display");
        Check.equal("hidden", a.keyword(Prop.OVERFLOW_X), "overflow is always hidden");
        Check.equal("hidden", a.keyword(Prop.OVERFLOW_Y), "overflow is always hidden (vertical)");
        Check.equal("start", a.keyword(Prop.TEXT_ALIGN), "text-align is reset");
        Check.equal(0xFF112233, a.color(Prop.BACKGROUND_COLOR), "bgcolor");
        Check.near(150, a.px(Prop.WIDTH), 0.001, "width");
        Check.near(50, a.px(Prop.HEIGHT), 0.001, "height");
        Check.near(7, a.px(Prop.MARGIN_LEFT), 0.001, "hspace left");
        Check.near(7, a.px(Prop.MARGIN_RIGHT), 0.001, "hspace right");
        Check.near(3, a.px(Prop.MARGIN_TOP), 0.001, "vspace top");
        Check.near(3, a.px(Prop.MARGIN_BOTTOM), 0.001, "vspace bottom");
        Check.near(200, fx.style("b").px(Prop.HEIGHT), 0.001, "direction=up has a natural height of 200px");
        Check.near(60, fx.style("c").px(Prop.HEIGHT), 0.001, "a height attribute overrides it");
        Check.equal("auto", fx.style("c").getPropertyValue("width"), "the width stays automatic");
        Check.equal("auto", fx.style("d").getPropertyValue("height"), "direction=left has no natural height");
        Check.near(200, fx.style("e").px(Prop.HEIGHT), 0.001, "an invalid height attribute leaves the natural height");
        Check.equal("auto", fx.style("f").getPropertyValue("height"), "an unknown direction is left");
        Check.equal("40%", fx.style("g").getPropertyValue("width"), "a percentage width");
    }

    /**
     * The painted static marquee shows its content from the start edge and clips what does not fit, whatever the author overflow says.
     * <p>
     * 그려진 정적 marquee 는 시작 쪽부터 내용을 보여 주고 작성자의 overflow 와 무관하게 넘치는 부분을 자른다.
     */
    public void testMarqueePixels() {
        fx.init("<marquee style='position:absolute;left:10px;top:10px;overflow:visible' width=50 height=20><div style='width:150px;height:20px;background:#ff0000'></div></marquee>"
                + "<marquee style='position:absolute;left:10px;top:50px' width=60 height=20 bgcolor=#00ff00></marquee>", "");
        BufferedImage image = LegacyFixture.paint(fx.render());
        Check.equal(0xFF0000, image.getRGB(12, 15) & 0xFFFFFF, "the content starts at the left edge of the marquee");
        Check.equal(0xFF0000, image.getRGB(58, 15) & 0xFFFFFF, "the content reaches the right edge");
        Check.equal(0xFFFFFF, image.getRGB(65, 15) & 0xFFFFFF, "the content that does not fit is clipped");
        Check.equal(0x00FF00, image.getRGB(40, 60) & 0xFFFFFF, "the background color of the second marquee");
        Check.equal(0xFFFFFF, image.getRGB(75, 60) & 0xFFFFFF, "outside the 60px wide marquee");
    }

    /**
     * Returns the computed style of a node that is an element.
     * <p>
     * 요소인 노드의 계산 스타일을 반환한다.
     *
     * @param n the element node / 요소 노드
     * @return the style / 스타일
     */
    private ComputedStyle css(Node n) {
        return fx.css().getComputedStyle((Element) n);
    }
}
