package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the serializer and of the <code>innerHTML</code> family of the DOM that {@link HtmlMarkupSupport} serves (plan items
 * P07-05, P07-06, P07-09): escaping, void and raw text elements, template contents, foreign elements, round trips of parsed markup,
 * <code>setInnerHtml</code>, <code>setOuterHtml</code>, <code>insertAdjacentHtml</code> and very deep trees.
 * <p>
 * 직렬화기와 {@link HtmlMarkupSupport} 가 지원하는 DOM 의 <code>innerHTML</code> 계열 시험(계획 P07-05, P07-06, P07-09): 이스케이프, void 와 원문 텍스트
 * 요소, template 내용, 외부 요소, 파싱한 마크업의 왕복, <code>setInnerHtml</code>, <code>setOuterHtml</code>, <code>insertAdjacentHtml</code>, 아주 깊은 트리.
 */
public class HtmlMarkupSupportTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public HtmlMarkupSupportTest() {
    }

    /**
     * Parses a document and returns its body.
     * <p>
     * 문서를 파싱해 body 를 반환한다.
     *
     * @param markup markup / 마크업
     * @return the body element / body 요소
     */
    private static Element body(String markup) {
        Document doc = HtmlParser.parseDocument(markup, null, null, true);
        return (Element) doc.getElementsByTagName("body").item(0);
    }

    /**
     * Parses markup as the body content and serializes it again.
     * <p>
     * 마크업을 body 내용으로 파싱해 다시 직렬화한다.
     *
     * @param markup markup / 마크업
     * @return the serialization of the body's children / body 자식의 직렬화
     */
    private static String roundTrip(String markup) {
        return body(markup).getInnerHtml();
    }

    /**
     * Escaping of text and attribute values, and the elements that are written without escaping or without end tag.
     * <p>
     * 텍스트와 속성 값의 이스케이프, 이스케이프 없이 또는 종료 태그 없이 쓰는 요소.
     */
    public void testEscapingAndSpecialElements() {
        Check.equal("<p class=\"a\">x &amp; y &lt; z &gt; w</p>", roundTrip("<p class=a>x &amp; y &lt; z &gt; w</p>"), "text");
        Check.equal("<a title=\"a&quot;b&amp;c<d>\"></a>", roundTrip("<a title='a\"b&amp;c<d>'></a>"), "attribute");
        Check.equal("<br><img src=\"a\"><input>", roundTrip("<br><img src=a><input>"), "void elements");
        Check.equal("<div></div><script>a<b&c</script><style>p>a{}</style>", roundTrip("<div></div><script>a<b&c</script><style>p>a{}</style>"), "raw text");
        Check.equal("<textarea>a&lt;b</textarea>", roundTrip("<textarea>a&lt;b</textarea>"), "textarea is escaped");
        Check.equal("<xmp><b>&amp;</xmp>", roundTrip("<xmp><b>&amp;</xmp>"), "xmp is raw");
        Check.equal("<p>y<!--x--></p>", roundTrip("<p>y<!--x-->"), "comment");
        Check.equal("<p>a&nbsp;b</p>", roundTrip("<p>a" + (char) 0xA0 + "b"), "no-break space");
        Check.equal("<div></div><template><p>a</p></template>", roundTrip("<div></div><template><p>a</template>"), "template contents");
        Check.equal("<table><tbody><tr><td>x</td></tr></tbody></table>", roundTrip("<table><tr><td>x"), "implied table elements");
    }

    /**
     * SVG and MathML elements keep their case and namespaced attributes keep their prefix.
     * <p>
     * SVG 와 MathML 요소는 대소문자를 유지하고 네임스페이스가 있는 속성은 접두사를 유지한다.
     */
    public void testForeignSerialization() {
        Check.equal("<svg viewBox=\"0 0 1 1\"><circle></circle><foreignObject><p>x</p></foreignObject></svg>",
                roundTrip("<svg viewbox='0 0 1 1'><circle/><foreignobject><p>x</svg>"), "svg");
        Check.equal("<svg><use xlink:href=\"#a\" xml:lang=\"ko\"></use></svg>", roundTrip("<svg><use xlink:href=#a xml:lang=ko></svg>"), "namespaced attributes");
        Check.equal("<math definitionURL=\"x\"><mi>a</mi></math>", roundTrip("<math definitionurl=x><mi>a</math>"), "mathml");
    }

    /**
     * Misnested markup comes out repaired, and the repaired markup is stable under another round trip.
     * <p>
     * 잘못 중첩된 마크업은 고쳐진 채로 나오고, 고쳐진 마크업은 한 번 더 왕복해도 같다.
     */
    public void testRoundTrips() {
        Check.equal("<b>1<i>2</i></b><i>3</i>", roundTrip("<b>1<i>2</b>3</i>"), "adoption agency result");
        String[] samples = {"<p>a<p>b", "<ul><li>1<li>2</ul>", "<table><tr><td>a<td>b<tr><td>c</table>", "<b><p>x</b>y", "<select><option>a<option>b</select>",
                "<dl><dt>a<dd>b</dl>", "<a href=x>l<div>m</a>n", "<h1>t<h2>u", "<svg><g><circle r=1/></g></svg><p>x", "<div><span>1</span> 2 <em>3</em></div>",
                "x<br>y<hr>z", "<form><input name=a><select><option>1</select></form>", "<table><b>x<tr><td>y</table>"};
        // 각 예제를 두 번 왕복해 두 번째 결과가 첫 번째와 같은지 확인한다.
        for (int i = 0; i < samples.length; i++) {
            String once = roundTrip(samples[i]);
            String twice = roundTrip(once);
            Check.equal(once, twice, "stable after one round trip: " + samples[i]);
        }
    }

    /**
     * Documents and fragments serialize their children, DOCTYPE and processing instructions have their own syntax.
     * <p>
     * 문서와 조각은 자식을 직렬화하고 DOCTYPE 과 처리 명령은 자기 문법이 있다.
     */
    public void testWholeDocument() {
        Document doc = HtmlParser.parseDocument("<!DOCTYPE html><title>t</title><p>x", null, null, true);
        Check.equal("<!DOCTYPE html><html><head><title>t</title></head><body><p>x</p></body></html>", new HtmlMarkupSupport().serialize(doc, false),
                "document");
        Check.equal("<!DOCTYPE html>", new HtmlMarkupSupport().serialize(doc.getDoctype(), true), "doctype");
        Check.equal("<html><head><title>t</title></head><body><p>x</p></body></html>", doc.getDocumentElement().getOuterHtml(), "outerHTML");
        Check.equal("<head><title>t</title></head><body><p>x</p></body>", doc.getDocumentElement().getInnerHtml(), "innerHTML");
        Check.equal("<?t d>", new HtmlMarkupSupport().serialize(doc.createProcessingInstruction("t", "d"), true), "processing instruction");
        DocumentFragment f = HtmlParser.parseFragment(doc.createElement("div"), "<i>a</i>b");
        Check.equal("<i>a</i>b", new HtmlMarkupSupport().serialize(f, true), "fragment");
        Check.equal("ab", new HtmlMarkupSupport().serialize(doc.createTextNode("ab"), true), "text node");
    }

    /**
     * The setters of the DOM: innerHTML, outerHTML and insertAdjacentHTML use the markup service.
     * <p>
     * DOM 의 설정자: innerHTML, outerHTML, insertAdjacentHTML 이 마크업 서비스를 쓴다.
     */
    public void testDomApis() {
        Document doc = HtmlParser.parseDocument("<div id=d><b>old</b></div><table id=t></table><template id=tpl></template>", null, null, true);
        Element d = doc.getElementById("d");
        d.setInnerHtml("<i>new</i> text");
        Check.equal("<i>new</i> text", d.getInnerHtml(), "innerHTML setter");
        d.insertAdjacentHtml("beforebegin", "<p>1</p>");
        d.insertAdjacentHtml("afterbegin", "<p>2</p>");
        d.insertAdjacentHtml("beforeend", "<p>3</p>");
        d.insertAdjacentHtml("afterend", "<p>4</p>");
        Check.equal("<p>1</p><div id=\"d\"><p>2</p><i>new</i> text<p>3</p></div><p>4</p><table id=\"t\"></table><template id=\"tpl\"></template>",
                ((Element) doc.getElementsByTagName("body").item(0)).getInnerHtml(), "insertAdjacentHTML");
        Element t = doc.getElementById("t");
        t.setInnerHtml("<tr><td>x");
        Check.equal("<tbody><tr><td>x</td></tr></tbody>", t.getInnerHtml(), "innerHTML of a table is parsed in the table context");
        d.setOuterHtml("<section>s</section>");
        Check.isNull(doc.getElementById("d"), "the old element is gone");
        Check.equal("<section>s</section>", ((Element) doc.getElementsByTagName("section").item(0)).getOuterHtml(), "outerHTML setter");
        Element tpl = doc.getElementById("tpl");
        tpl.setInnerHtml("<p>in</p>");
        Check.isNull(tpl.getFirstChild(), "template has no children");
        Check.equal("<p>in</p>", tpl.getInnerHtml(), "template contents are serialized");
        Check.equal("p", ((Element) ParserNodeData.getTemplateContents(tpl).getFirstChild()).getLocalName(), "template contents hold the nodes");
        Element script = doc.createElement("div");
        script.setInnerHtml("<script>x</script>");
        Check.isTrue(ParserNodeData.scriptFlags((Element) script.getFirstChild()).isAlreadyStarted(), "scripts from innerHTML are already started");
    }

    /**
     * A very deep tree is serialized without a stack overflow.
     * <p>
     * 아주 깊은 트리도 스택 오버플로 없이 직렬화된다.
     */
    public void testDeepTree() {
        StringBuilder sb = new StringBuilder();
        // 20000 겹의 div 를 만든다.
        for (int i = 0; i < 20000; i++) {
            sb.append("<div>");
        }
        Element b = body(sb.toString());
        String s = b.getInnerHtml();
        Check.equal(20000 * 11, s.length(), "length of the serialization");
        Node n = b;
        Check.notNull(n, "body");
    }
}
