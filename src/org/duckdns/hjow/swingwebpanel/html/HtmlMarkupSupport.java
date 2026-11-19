package org.duckdns.hjow.swingwebpanel.html;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.DocumentType;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MarkupSupport;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.ProcessingInstruction;

/**
 * The HTML implementation of the DOM's {@link MarkupSupport} service: it gives <code>innerHTML</code>, <code>outerHTML</code>,
 * <code>insertAdjacentHTML</code> their meaning. Serialization follows "serializing HTML fragments" of the HTML Standard (escaping of
 * text and attribute values, void elements, raw text elements, template contents, namespaced attribute names) and parsing uses the
 * fragment parsing algorithm of {@link HtmlParser}. Install it with <code>document.setMarkupSupport(new HtmlMarkupSupport())</code>.
 * The serializer walks the tree iteratively, so very deep trees do not overflow the Java stack.
 * <p>
 * The fragment parser and the serializer do not insert a line feed after the start tag of <code>pre</code>, <code>textarea</code> and
 * <code>listing</code> (current browsers and the standard do not either).
 * <p>
 * DOM 의 {@link MarkupSupport} 서비스의 HTML 구현: <code>innerHTML</code>, <code>outerHTML</code>, <code>insertAdjacentHTML</code> 에 의미를 준다.
 * 직렬화는 HTML 표준의 "serializing HTML fragments"(텍스트와 속성 값 이스케이프, void 요소, 원문 텍스트 요소, template 내용, 네임스페이스가 있는 속성
 * 이름)를 따르고 파싱은 {@link HtmlParser} 의 조각 파싱 알고리즘을 쓴다. <code>document.setMarkupSupport(new HtmlMarkupSupport())</code> 로 설치한다.
 * 직렬화기는 트리를 반복문으로 훑으므로 아주 깊은 트리에서도 자바 스택이 넘치지 않는다.
 * <p>
 * 조각 파서와 직렬화기는 <code>pre</code>, <code>textarea</code>, <code>listing</code> 시작 태그 뒤에 줄바꿈을 넣지 않는다(현재 브라우저와 표준도 그렇다).
 */
public final class HtmlMarkupSupport implements MarkupSupport {

    /**
     * Names of the void elements, which are serialized without an end tag and without children.
     * <p>
     * void 요소의 이름. 종료 태그와 자식 없이 직렬화된다.
     */
    private static final String VOID_ELEMENTS = ",area,base,basefont,bgsound,br,col,embed,frame,hr,img,input,keygen,link,meta,param,source,track,wbr,";

    /**
     * Names of the elements whose text children are serialized without escaping.
     * <p>
     * 텍스트 자식을 이스케이프 없이 직렬화하는 요소의 이름.
     */
    private static final String RAW_TEXT_ELEMENTS = ",style,script,xmp,iframe,noembed,noframes,plaintext,";

    /**
     * Creates the service.
     * <p>
     * 서비스를 만든다.
     */
    public HtmlMarkupSupport() {
    }

    /**
     * Serializes a node.
     * <p>
     * 노드를 직렬화한다.
     *
     * @param node  node / 노드
     * @param outer true to include the node itself (<code>outerHTML</code>), false for its children (<code>innerHTML</code>) / 노드 자신도
     *              포함하면(<code>outerHTML</code>) true, 자식만이면(<code>innerHTML</code>) false
     * @return HTML text / HTML 텍스트
     */
    public String serialize(Node node, boolean outer) {
        StringBuilder sb = new StringBuilder();
        // 문서와 조각은 자기 자신이 쓸 것이 없으므로 항상 자식만 직렬화한다.
        boolean self = outer && node.getNodeType() != Node.DOCUMENT_NODE && node.getNodeType() != Node.DOCUMENT_FRAGMENT_NODE;
        serializeTree(sb, node, self);
        return sb.toString();
    }

    /**
     * Replaces the children of an element by the nodes parsed from markup (<code>innerHTML</code> setter); for a
     * <code>template</code> the template contents are replaced.
     * <p>
     * 요소의 자식을 마크업에서 파싱한 노드로 바꾼다(<code>innerHTML</code> 설정). <code>template</code> 이면 template contents 를 바꾼다.
     *
     * @param context element / 요소
     * @param markup  markup text / 마크업 텍스트
     */
    public void setInnerHtml(Element context, String markup) {
        DocumentFragment f = parseFragment(context, markup);
        Node target = context;
        if (isHtml(context, "template")) {
            target = ParserNodeData.ensureTemplateContents(context);
        }
        // 기존 자식을 모두 지운 뒤 조각을 붙인다.
        while (target.getFirstChild() != null) {
            target.removeChild(target.getFirstChild());
        }
        target.appendChild(f);
    }

    /**
     * Parses markup in the context of an element.
     * <p>
     * 요소를 컨텍스트로 마크업을 파싱한다.
     *
     * @param context element / 요소
     * @param markup  markup text / 마크업 텍스트
     * @return the fragment / 조각
     */
    public DocumentFragment parseFragment(Element context, String markup) {
        return HtmlParser.parseFragment(context, markup);
    }

    /**
     * Tells whether an element is an HTML element with a local name.
     * <p>
     * 요소가 해당 로컬 이름의 HTML 요소인지 알려 준다.
     *
     * @param e    element / 요소
     * @param name local name / 로컬 이름
     * @return true if so / 그러면 true
     */
    private static boolean isHtml(Element e, String name) {
        return Namespaces.HTML.equals(e.getNamespaceURI()) && name.equals(e.getLocalName());
    }

    /**
     * Returns the first child that is serialized inside a node (inside the template contents for a <code>template</code>).
     * <p>
     * 노드 안에서 직렬화되는 첫 자식을 반환한다(<code>template</code> 이면 template contents 안의 첫 자식).
     *
     * @param n node / 노드
     * @return first child or null / 첫 자식 또는 null
     */
    private static Node firstContent(Node n) {
        if (n instanceof Element && isHtml((Element) n, "template")) {
            DocumentFragment c = ParserNodeData.getTemplateContents((Element) n);
            if (c != null) {
                return c.getFirstChild();
            }
        }
        return n.getFirstChild();
    }

    /**
     * Serializes a tree without recursion: an explicit stack remembers the open elements and the sibling to continue with.
     * <p>
     * 재귀 없이 트리를 직렬화한다: 명시적인 스택이 열린 요소와 이어서 방문할 형제를 기억한다.
     *
     * @param sb    output / 출력
     * @param root  node to serialize / 직렬화할 노드
     * @param outer true to include the root itself / 루트 자신도 포함하면 true
     */
    private static void serializeTree(StringBuilder sb, Node root, boolean outer) {
        List<Element> open = new ArrayList<Element>();
        List<Node> resume = new ArrayList<Node>();
        Node node = outer ? root : firstContent(root);
        // 한 단계의 노드들을 차례로 방문하고, 자식이 있는 요소는 스택에 넣고 안으로 들어간다.
        while (true) {
            while (node != null) {
                Node sibling = outer && open.isEmpty() && node == root ? null : node.getNextSibling();
                if (node instanceof Element) {
                    Element e = (Element) node;
                    writeStartTag(sb, e);
                    if (!(Namespaces.HTML.equals(e.getNamespaceURI()) && VOID_ELEMENTS.indexOf("," + e.getLocalName() + ",") >= 0)) {
                        Node child = firstContent(e);
                        if (child != null) {
                            open.add(e);
                            resume.add(sibling);
                            node = child;
                            continue;
                        }
                        writeEndTag(sb, e);
                    }
                } else {
                    writeLeaf(sb, node);
                }
                node = sibling;
            }
            if (open.isEmpty()) {
                return;
            }
            Element e = open.remove(open.size() - 1);
            writeEndTag(sb, e);
            node = resume.remove(resume.size() - 1);
        }
    }

    /**
     * Writes a node that has no children in the serialization: text, comment, processing instruction, DOCTYPE. A document or
     * fragment inside the tree writes nothing of its own.
     * <p>
     * 직렬화에서 자식이 없는 노드를 쓴다: 텍스트, 주석, 처리 명령, DOCTYPE. 트리 안의 문서나 조각은 자기 몫으로 쓰는 것이 없다.
     *
     * @param sb output / 출력
     * @param n  node / 노드
     */
    private static void writeLeaf(StringBuilder sb, Node n) {
        switch (n.getNodeType()) {
        case Node.TEXT_NODE:
        case Node.CDATA_SECTION_NODE:
            Node p = n.getParentNode();
            if (p instanceof Element && isRawTextParent((Element) p)) {
                sb.append(((CharacterData) n).getData());
            } else {
                escapeText(sb, ((CharacterData) n).getData(), false);
            }
            break;
        case Node.COMMENT_NODE:
            sb.append("<!--").append(((CharacterData) n).getData()).append("-->");
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            sb.append("<?").append(((ProcessingInstruction) n).getTarget()).append(' ').append(((CharacterData) n).getData()).append('>');
            break;
        case Node.DOCUMENT_TYPE_NODE:
            sb.append("<!DOCTYPE ").append(((DocumentType) n).getName()).append('>');
            break;
        default:
            break;
        }
    }

    /**
     * Tells whether the text children of an element are serialized raw (script, style and so on).
     * <p>
     * 요소의 텍스트 자식을 그대로 직렬화하는지(script, style 등) 알려 준다.
     *
     * @param e parent element / 부모 요소
     * @return true if raw / 그대로이면 true
     */
    private static boolean isRawTextParent(Element e) {
        return Namespaces.HTML.equals(e.getNamespaceURI()) && RAW_TEXT_ELEMENTS.indexOf("," + e.getLocalName() + ",") >= 0;
    }

    /**
     * Returns the tag name that is serialized for an element: the local name for HTML, SVG and MathML elements, otherwise the qualified
     * name.
     * <p>
     * 요소에 대해 직렬화되는 태그 이름을 반환한다: HTML, SVG, MathML 요소는 로컬 이름, 그 밖에는 정규화된 이름.
     *
     * @param e element / 요소
     * @return tag name / 태그 이름
     */
    private static String tagName(Element e) {
        String ns = e.getNamespaceURI();
        boolean known = Namespaces.HTML.equals(ns) || Namespaces.SVG.equals(ns) || Namespaces.MATHML.equals(ns);
        return known ? e.getLocalName() : e.getTagNameQualified();
    }

    /**
     * Writes the start tag of an element with its attributes.
     * <p>
     * 요소의 시작 태그를 속성과 함께 쓴다.
     *
     * @param sb output / 출력
     * @param e  element / 요소
     */
    private static void writeStartTag(StringBuilder sb, Element e) {
        sb.append('<').append(tagName(e));
        List<String> names = e.getAttributeNames();
        // 속성을 소스 순서대로 `이름="값"` 으로 쓴다.
        for (int i = 0; i < names.size(); i++) {
            Attr a = e.getAttributeNode(names.get(i));
            if (a == null) {
                continue;
            }
            sb.append(' ').append(attributeName(a)).append("=\"");
            escapeText(sb, a.getValue(), true);
            sb.append('"');
        }
        sb.append('>');
    }

    /**
     * Writes the end tag of an element.
     * <p>
     * 요소의 종료 태그를 쓴다.
     *
     * @param sb output / 출력
     * @param e  element / 요소
     */
    private static void writeEndTag(StringBuilder sb, Element e) {
        sb.append("</").append(tagName(e)).append('>');
    }

    /**
     * Returns the serialized name of an attribute.
     * <p>
     * 속성의 직렬화된 이름을 반환한다.
     *
     * @param a attribute / 속성
     * @return name / 이름
     */
    private static String attributeName(Attr a) {
        String ns = a.getNamespaceURI();
        if (ns == null) {
            return a.getLocalName();
        }
        if (Namespaces.XML.equals(ns)) {
            return "xml:" + a.getLocalName();
        }
        if (Namespaces.XMLNS.equals(ns)) {
            return "xmlns".equals(a.getLocalName()) ? "xmlns" : "xmlns:" + a.getLocalName();
        }
        if (Namespaces.XLINK.equals(ns)) {
            return "xlink:" + a.getLocalName();
        }
        return a.getName();
    }

    /**
     * Escapes text for serialization: ampersand and no-break space always, less-than and greater-than in text, the double quote in
     * attribute values.
     * <p>
     * 직렬화를 위해 텍스트를 이스케이프한다: 앰퍼샌드와 줄 바꿈 없는 공백은 항상, 텍스트에서는 꺾쇠 괄호, 속성 값에서는 큰따옴표.
     *
     * @param sb        output / 출력
     * @param s         text / 문자열
     * @param attribute true for an attribute value / 속성 값이면 true
     */
    private static void escapeText(StringBuilder sb, String s, boolean attribute) {
        // 문자마다 이스케이프가 필요한지 확인한다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case 0xA0:
                sb.append("&nbsp;");
                break;
            case '"':
                sb.append(attribute ? "&quot;" : "\"");
                break;
            case '<':
                sb.append(attribute ? "<" : "&lt;");
                break;
            case '>':
                sb.append(attribute ? ">" : "&gt;");
                break;
            default:
                sb.append(c);
                break;
            }
        }
    }

    /**
     * Returns the document of a node (helper for callers that only have the service).
     * <p>
     * 노드의 문서를 반환한다(서비스만 가진 호출자를 위한 도우미).
     *
     * @param n node / 노드
     * @return owner document / 소유 문서
     */
    static Document documentOf(Node n) {
        return n.getOwnerDocument();
    }
}
