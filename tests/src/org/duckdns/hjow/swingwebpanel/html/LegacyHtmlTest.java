package org.duckdns.hjow.swingwebpanel.html;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the handling of legacy (pre-HTML5) markup by the parser (plan item P07-07): obsolete elements are kept as ordinary
 * elements with their attributes, the legacy void elements have no children, frames and frame sets are parsed, legacy character
 * references work and the document mode of old DOCTYPEs is detected. What the elements <em>mean</em> (presentational attributes) is
 * the business of the style and layout stages; this test only checks that nothing is lost on the way into the DOM.
 * <p>
 * 파서의 옛(HTML5 이전) 마크업 처리 시험(계획 P07-07): 폐기된 요소는 속성과 함께 일반 요소로 보존되고, 옛 void 요소는 자식이 없으며, 프레임과
 * 프레임셋을 파싱하고, 옛 문자 참조가 동작하며, 옛 DOCTYPE 의 문서 모드가 판정된다. 요소의 <em>의미</em>(표현 속성)는 스타일과 레이아웃 단계의 몫이며,
 * 이 시험은 DOM 에 들어오는 과정에서 아무것도 사라지지 않는지만 확인한다.
 */
public class LegacyHtmlTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public LegacyHtmlTest() {
    }

    /**
     * Obsolete elements that have no special parsing rule: they become ordinary elements with their attributes and children.
     * <p>
     * 특별한 파싱 규칙이 없는 폐기된 요소: 속성과 자식이 있는 일반 요소가 된다.
     */
    public void testObsoleteElementsAreKept() {
        // isindex 는 HTML5 2014 의 "in body" 규칙이 form 으로 펼치므로(LegacyElementsTest) 일반 요소 목록에 없다.
        String[] names = {"acronym", "applet", "big", "blink", "center", "dir", "font", "layer", "marquee", "menu", "multicol", "nextid", "nobr",
                "spacer", "strike", "tt", "u", "s", "bdo", "cite", "dfn", "kbd", "samp", "var", "abbr", "address", "sub", "sup", "ins", "del", "q", "wbr2",
                "comment", "listing"};
        Document doc = Document.createHtmlDocument();
        // 이름마다 속성이 있는 요소를 파싱해 요소 이름, 속성, 자식이 보존되는지 확인한다.
        for (int i = 0; i < names.length; i++) {
            Element div = doc.createElement("div");
            DocumentFragment f = HtmlParser.parseFragment(div, "<" + names[i] + " a=1 b=\"x y\">t</" + names[i] + ">");
            Element e = (Element) f.getFirstChild();
            Check.notNull(e, "element for " + names[i]);
            Check.equal(names[i], e.getLocalName(), "name of " + names[i]);
            Check.equal("1", e.getAttribute("a"), "attribute a of " + names[i]);
            Check.equal("x y", e.getAttribute("b"), "attribute b of " + names[i]);
            Check.equal("t", e.getTextContent(), "content of " + names[i]);
            Check.isNull(f.getFirstChild().getNextSibling(), "nothing else for " + names[i]);
        }
    }

    /**
     * Legacy void elements: no children, no end tag needed.
     * <p>
     * 옛 void 요소: 자식이 없고 종료 태그가 필요 없다.
     */
    public void testLegacyVoidElements() {
        String[] names = {"basefont", "bgsound", "keygen", "param", "area", "embed", "frame2", "wbr", "col2"};
        Document doc = Document.createHtmlDocument();
        // 이름마다 뒤따르는 텍스트가 자식이 아니라 형제가 되는지 확인한다.
        for (int i = 0; i < names.length; i++) {
            if (names[i].endsWith("2")) {
                continue;
            }
            Element div = doc.createElement("div");
            DocumentFragment f = HtmlParser.parseFragment(div, "<" + names[i] + " a=1>x");
            Element e = (Element) f.getFirstChild();
            Check.equal(names[i], e.getLocalName(), "name of " + names[i]);
            Check.isNull(e.getFirstChild(), names[i] + " has no children");
            Check.equal("x", f.getLastChild().getTextContent(), "text after " + names[i]);
        }
    }

    /**
     * Frame sets with nested frames and a noframes alternative, as in HTML 4.
     * <p>
     * HTML 4 의 중첩된 프레임과 noframes 대체 내용이 있는 프레임셋.
     */
    public void testFramesets() {
        Document doc = HtmlParser.parseDocument("<html><head><title>f</title></head><frameset cols=\"20%,*\"><frame src=a name=nav>"
                + "<frameset rows=\"50%,50%\"><frame src=b><frame src=c></frameset><noframes><body>no frames</body></noframes></frameset></html>", null, null, true);
        Check.equal(0, doc.getElementsByTagName("body").getLength(), "a frameset document has no body");
        Check.equal(3, doc.getElementsByTagName("frame").getLength(), "three frames");
        Check.equal(2, doc.getElementsByTagName("frameset").getLength(), "two framesets");
        Element noframes = (Element) doc.getElementsByTagName("noframes").item(0);
        Check.equal("<body>no frames</body>", noframes.getTextContent(), "noframes content is raw text");
        Check.equal("20%,*", ((Element) doc.getElementsByTagName("frameset").item(0)).getAttribute("cols"), "cols");
        Check.equal("nav", ((Element) doc.getElementsByTagName("frame").item(0)).getAttribute("name"), "frame name");
    }

    /**
     * Presentational attributes of old documents survive parsing unchanged.
     * <p>
     * 옛 문서의 표현 속성은 파싱 뒤에도 그대로 남는다.
     */
    public void testPresentationalAttributes() {
        Document doc = HtmlParser.parseDocument("<body bgcolor=#fff text=black link=blue vlink=purple alink=red background=bg.gif leftmargin=0 topmargin=0>"
                + "<table border=1 cellspacing=0 cellpadding=2 width=\"100%\" align=center bgcolor=ccc><tr valign=top><td nowrap colspan=2 rowspan=1 "
                + "width=10 height=20 align=right>x<img src=a.gif hspace=3 vspace=3 border=0 align=left><hr noshade size=1 width=50%><br clear=all>"
                + "<font face=\"Arial, Helvetica\" size=+1 color=red>y</font>", null, null, true);
        Element body = (Element) doc.getElementsByTagName("body").item(0);
        Check.equal("#fff", body.getAttribute("bgcolor"), "bgcolor");
        Check.equal("bg.gif", body.getAttribute("background"), "background");
        Check.equal("0", body.getAttribute("leftmargin"), "leftmargin");
        Element table = (Element) doc.getElementsByTagName("table").item(0);
        Check.equal("100%", table.getAttribute("width"), "table width");
        Check.equal("center", table.getAttribute("align"), "table align");
        Element td = (Element) doc.getElementsByTagName("td").item(0);
        Check.equal("", td.getAttribute("nowrap"), "boolean attribute");
        Check.equal("2", td.getAttribute("colspan"), "colspan");
        Element font = (Element) doc.getElementsByTagName("font").item(0);
        Check.equal("Arial, Helvetica", font.getAttribute("face"), "face");
        Check.equal("+1", font.getAttribute("size"), "size");
        Check.equal("all", ((Element) doc.getElementsByTagName("br").item(0)).getAttribute("clear"), "clear");
        Check.equal("", ((Element) doc.getElementsByTagName("hr").item(0)).getAttribute("noshade"), "noshade");
    }

    /**
     * Old character references: Latin-1 names without semicolon, uppercase names and numeric references of the Windows range.
     * <p>
     * 옛 문자 참조: 세미콜론 없는 Latin-1 이름, 대문자 이름, Windows 영역의 숫자 참조.
     */
    public void testLegacyCharacterReferences() {
        Document doc = HtmlParser.parseDocument("<p>&copy &reg &nbsp&AMP &LT &GT &QUOT &eacute &szlig &#150 &#x93;&#147;", null, null, true);
        Element p = (Element) doc.getElementsByTagName("p").item(0);
        String text = p.getTextContent();
        Check.equal("© ® " + (char) 0xA0 + "& < > \" é ß – ““", text, "text of the references");
    }

    /**
     * Document modes of the DOCTYPEs of earlier HTML versions, as seen through the parser.
     * <p>
     * 이전 HTML 버전의 DOCTYPE 이 파서를 거쳐 보이는 문서 모드.
     */
    public void testOldDoctypes() {
        String[][] cases = {
                {"<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">", "2"},
                {"<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML//EN\">", "2"},
                {"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\">", "2"},
                {"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\" \"http://www.w3.org/TR/REC-html40/strict.dtd\">", "0"},
                {"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">", "2"},
                {"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">", "0"},
                {"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">", "2"},
                {"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">", "1"},
                {"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">", "0"},
                {"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">", "0"},
                {"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"x\">", "1"},
                {"<!DOCTYPE html>", "0"}, {"<p>no doctype", "2"}};
        // 사례마다 문서 모드를 확인한다.
        for (int i = 0; i < cases.length; i++) {
            Document doc = HtmlParser.parseDocument(cases[i][0], null, null, true);
            Check.equal(Integer.parseInt(cases[i][1]), doc.getCompatibilityMode(), "mode of " + cases[i][0]);
        }
    }

    /**
     * Misnested old-style markup: unclosed paragraphs, list items and table cells are repaired.
     * <p>
     * 잘못 중첩된 옛 방식 마크업: 닫지 않은 문단, 목록 항목, 표 셀이 고쳐진다.
     */
    public void testSloppyOldMarkup() {
        Document doc = HtmlParser.parseDocument("<HTML><BODY BGCOLOR=white><H1>Title<P>Para one<P>Para two<UL><LI>a<LI>b</UL><TABLE><TR><TD>1<TD>2<TR><TD>3</TABLE>"
                + "<DL><DT>t<DD>d<DT>u<DD>e</DL><FONT SIZE=2><B>bold<I>both</B>italic</I></FONT>", null, null, true);
        Check.equal(2, doc.getElementsByTagName("p").getLength(), "two paragraphs");
        Check.equal(2, doc.getElementsByTagName("li").getLength(), "two list items");
        Check.equal(3, doc.getElementsByTagName("td").getLength(), "three cells");
        Check.equal(2, doc.getElementsByTagName("tr").getLength(), "two rows");
        Check.equal(2, doc.getElementsByTagName("dt").getLength(), "two terms");
        Check.equal(2, doc.getElementsByTagName("dd").getLength(), "two definitions");
        Element h1 = (Element) doc.getElementsByTagName("h1").item(0);
        Check.equal("Title", h1.getFirstChild().getTextContent(), "heading text (a p does not close an h1)");
        Node n = doc.getDocumentElement();
        Check.notNull(n, "document element");
    }
}
