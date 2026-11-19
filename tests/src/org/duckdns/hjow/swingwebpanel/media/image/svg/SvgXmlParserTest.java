package org.duckdns.hjow.swingwebpanel.media.image.svg;

import org.duckdns.hjow.swingwebpanel.media.MediaException;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the XML parser of the SVG decoder (plan item P18-04): namespaces, character and entity references, CDATA, comments,
 * processing instructions, the DOCTYPE with an internal subset, encodings and the protection against entity expansion attacks.
 * <p>
 * SVG 디코더의 XML 파서 시험(계획 P18-04): 네임스페이스, 문자/엔티티 참조, CDATA, 주석, 처리 명령, 내부 부분집합을 가진 DOCTYPE, 인코딩, 엔티티
 * 확장 공격에 대한 방어.
 */
public class SvgXmlParserTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public SvgXmlParserTest() {
    }

    /**
     * Parses a text.
     * <p>
     * 텍스트를 해석한다.
     *
     * @param s the XML text / XML 텍스트
     * @return the root element / 루트 요소
     * @throws MediaException on malformed XML / 형식이 틀린 XML 일 때
     */
    private static SvgElement parse(String s) throws MediaException {
        return SvgXmlParser.parse(SvgTestKit.utf8(s));
    }

    /**
     * Elements, attributes, nesting and the mixed content in document order.
     * <p>
     * 요소, 속성, 중첩, 문서 순서의 혼합 내용.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testStructure() throws Exception {
        SvgElement r = parse("<svg xmlns=\"http://www.w3.org/2000/svg\" id=\"r\" width='10'><g/><text>a<tspan>b</tspan>c</text><!-- x --><?pi y?></svg>");
        Check.equal("svg", r.name, "root name");
        Check.isTrue(r.isSvg(), "svg namespace");
        Check.equal("r", r.id(), "id");
        Check.equal("10", r.attr("width"), "single quoted attribute");
        Check.equal(2, r.children.size(), "two child elements");
        Check.equal("text", r.children.get(1).name, "second child");
        SvgElement text = r.children.get(1);
        Check.equal(3, text.content.size(), "text, tspan, text");
        Check.equal("a", (String) text.content.get(0), "first text node");
        Check.same(text.children.get(0), text.content.get(1), "the tspan in between");
        Check.equal("c", (String) text.content.get(2), "last text node");
        Check.equal("abc", text.textContent(), "text content of the subtree");
        Check.same(r, text.parent, "parent link");
        Check.equal(1, text.indexInParent(), "index in the parent");
    }

    /**
     * Namespace handling: default namespace, prefixes, redeclaration, xlink and xml attributes, foreign namespaces.
     * <p>
     * 네임스페이스 처리: 기본 네임스페이스, 접두사, 재선언, xlink 와 xml 속성, 외부 네임스페이스.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testNamespaces() throws Exception {
        SvgElement r = parse("<s:svg xmlns:s=\"http://www.w3.org/2000/svg\" xmlns:x=\"http://www.w3.org/1999/xlink\" xmlns:i=\"http://inkscape\">"
                + "<s:use x:href=\"#a\" i:label=\"L\" xml:space=\"preserve\"/><i:meta/><g xmlns=\"http://www.w3.org/2000/svg\"/><g xmlns=\"\"/></s:svg>");
        Check.isTrue(r.isSvg(), "prefixed svg element");
        SvgElement use = r.children.get(0);
        Check.equal("#a", use.attr("xlink:href"), "xlink attribute under any prefix");
        Check.equal("#a", use.href(), "href() falls back to xlink:href");
        Check.equal("preserve", use.attr("xml:space"), "xml attribute");
        Check.equal("L", use.attr("{http://inkscape}label"), "foreign attribute keeps its namespace");
        Check.isFalse(r.children.get(1).isSvg(), "foreign element");
        Check.isTrue(r.children.get(2).isSvg(), "redeclared default namespace");
        Check.isFalse(r.children.get(3).isSvg(), "default namespace undeclared with an empty value");
        SvgTestKit.fails(MediaException.Kind.CORRUPT, "<s:svg/>", "undeclared prefix of an element");
        SvgTestKit.fails(MediaException.Kind.CORRUPT, "<svg xmlns=\"http://www.w3.org/2000/svg\" q:a=\"1\"/>", "undeclared prefix of an attribute");
        SvgElement h = parse("<svg xmlns=\"http://www.w3.org/2000/svg\" href=\"#x\"/>");
        Check.equal("#x", h.href(), "plain href");
    }

    /**
     * Character references, predefined entities, CDATA and attribute value normalization.
     * <p>
     * 문자 참조, 미리 정의된 엔티티, CDATA, 속성 값 정규화.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testReferencesAndCdata() throws Exception {
        SvgElement r = parse("<a xmlns=\"http://www.w3.org/2000/svg\" t=\"x&#65;&#x42;&lt;&gt;&amp;&quot;&apos;y\" w=\"a&#10;b\tc\nd\">"
                + "&#x1F600;&lt;t&gt;<![CDATA[<raw & text>]]>é</a>");
        Check.equal("xAB<>&\"'y", r.attr("t"), "references in an attribute");
        Check.equal("a\nb c d", r.attr("w"), "a character reference for a newline is kept, literal tab and newline become spaces");
        Check.equal("😀<t><raw & text>é", r.textContent(), "references and CDATA in the content (a supplementary character)");
    }

    /**
     * The DOCTYPE: public and system identifiers are ignored, internal entities are expanded, external ones are empty.
     * <p>
     * DOCTYPE: 공개/시스템 식별자는 무시하고 내부 엔티티는 확장하며 외부 엔티티는 비어 있다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testDoctypeAndEntities() throws Exception {
        String doc = "<?xml version=\"1.0\" standalone=\"no\"?>\n<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\" \"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\" [\n"
                + "<!ENTITY ns \"http://www.w3.org/2000/svg\"> <!ENTITY w '12'> <!ENTITY both \"&w;&w;\"> <!ENTITY ext SYSTEM \"file:///etc/passwd\">\n"
                + "<!ENTITY % pe \"ignored\"> <!ELEMENT svg ANY> <!ATTLIST svg a CDATA #IMPLIED> <!NOTATION n SYSTEM \"x\"> <!-- c > ] -->\n"
                + "<!ENTITY markup \"<g id='m'/>\"> ]>\n<svg xmlns=\"&ns;\" width=\"&w;\" v=\"&both;\">[&ext;]&both;&markup;</svg>";
        SvgElement r = parse(doc);
        Check.isTrue(r.isSvg(), "namespace given by an entity");
        Check.equal("12", r.attr("width"), "entity in an attribute");
        Check.equal("1212", r.attr("v"), "nested entities in an attribute");
        Check.equal("[]1212", r.textContent(), "external entity reads as empty, internal ones expand");
        Check.equal(1, r.children.size(), "an entity value with markup becomes content");
        Check.equal("m", r.children.get(0).id(), "the element of the entity");
        Check.same(r, r.children.get(0).parent, "its parent");
        SvgElement dup = parse("<!DOCTYPE svg [<!ENTITY a \"1\"><!ENTITY a \"2\">]><svg xmlns=\"http://www.w3.org/2000/svg\">&a;</svg>");
        Check.equal("1", dup.textContent(), "the first declaration of an entity wins");
    }

    /**
     * Entity attacks end with TOO_LARGE: exponential expansion and recursion.
     * <p>
     * 엔티티 공격은 TOO_LARGE 로 끝난다: 지수적 확장과 재귀.
     */
    public void testEntityAttacks() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE svg [<!ENTITY e0 \"aaaaaaaaaa\">");
        // 단계마다 앞 단계를 열 번 쓰는 엔티티를 열 단계 만든다.
        for (int i = 1; i < 10; i++) {
            sb.append("<!ENTITY e").append(i).append(" \"");
            // 조건이 참인 동안 반복해서 처리한다.
            for (int k = 0; k < 10; k++) {
                sb.append("&e").append(i - 1).append(";");
            }
            sb.append("\">");
        }
        sb.append("]><svg xmlns=\"http://www.w3.org/2000/svg\" a=\"&e9;\"/>");
        final String attrBomb = sb.toString();
        Check.throwsException(MediaException.class, new Block() {
            public void run() throws Exception {
                parse(attrBomb);
            }
        }, "expansion in an attribute");
        final String recursive = "<!DOCTYPE svg [<!ENTITY a \"&b;\"><!ENTITY b \"&a;\">]><svg xmlns=\"http://www.w3.org/2000/svg\" x=\"&a;\"/>";
        Throwable t = Check.throwsException(MediaException.class, new Block() {
            public void run() throws Exception {
                parse(recursive);
            }
        }, "recursive entities");
        Check.equal(MediaException.Kind.TOO_LARGE, ((MediaException) t).getKind(), "recursion is stopped by the nesting limit");
    }

    /**
     * Encodings are detected from the byte order mark and the XML declaration.
     * <p>
     * 인코딩은 바이트 순서 표시와 XML 선언에서 알아낸다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testEncodingDetection() throws Exception {
        Check.equal("<a/>", SvgXmlParser.decode(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '<', 'a', '/', '>' }), "UTF-8 byte order mark is removed");
        Check.equal("<a/>", SvgXmlParser.decode("<a/>".getBytes("UTF-16LE")), "UTF-16LE without byte order mark, found by the first bytes");
        Check.equal("<a/>", SvgXmlParser.decode("<a/>".getBytes("UTF-16BE")), "UTF-16BE without byte order mark");
        Check.equal("<?xml version=\"1.0\" encoding=\"windows-1252\"?><a>€</a>",
                SvgXmlParser.decode("<?xml version=\"1.0\" encoding=\"windows-1252\"?><a>€</a>".getBytes("windows-1252")), "declared encoding");
        Check.equal("<?xml encoding='x-bogus-9'?>é", SvgXmlParser.decode("<?xml encoding='x-bogus-9'?>é".getBytes("UTF-8")), "unknown encoding names mean UTF-8");
    }

    /**
     * Malformed documents fail with CORRUPT and a message with the line number.
     * <p>
     * 형식이 틀린 문서는 CORRUPT 로 실패하며 메시지에 줄 번호가 들어 있다.
     *
     * @throws Exception on a failure / 실패 시
     */
    public void testErrorMessage() throws Exception {
        try {
            parse("<svg xmlns=\"http://www.w3.org/2000/svg\">\n<g>\n</svg>");
            Check.fail("an unclosed element must fail");
        } catch (MediaException e) {
            Check.equal(MediaException.Kind.CORRUPT, e.getKind(), "kind");
            Check.contains(e.getMessage(), "line 3", "the line of the mismatch");
        }
    }
}
