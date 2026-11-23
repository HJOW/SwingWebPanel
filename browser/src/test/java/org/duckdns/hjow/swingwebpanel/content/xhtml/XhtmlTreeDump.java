package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.CharacterData;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.DocumentType;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.ProcessingInstruction;
import org.duckdns.hjow.swingwebpanel.html.ParserNodeData;

/**
 * Writes a DOM tree as text for the tests of the XHTML parser: one node per line with two spaces of indentation per level. Elements are
 * <code>&lt;ns|qualified-name&gt;</code> (the namespace is abbreviated: <code>h</code> for XHTML, <code>svg</code>, <code>math</code>; no
 * <code>ns|</code> for no namespace), their attributes follow sorted by name as <code>@name{ns}="value"</code>, text as
 * <code>"text"</code>, CDATA sections, comments, processing instructions and the document type in their markup form. The contents of a
 * <code>template</code> are written under a line <code>content</code>. Line feeds are written as <code>\n</code>.
 * <p>
 * XHTML 파서 시험을 위해 DOM 트리를 문자열로 만든다: 노드마다 한 줄, 단계마다 공백 두 칸 들여쓰기. 요소는 <code>&lt;ns|정규화된 이름&gt;</code>
 * (네임스페이스는 줄여 쓴다: XHTML 은 <code>h</code>, <code>svg</code>, <code>math</code>. 네임스페이스가 없으면 <code>ns|</code> 없음), 속성은 이름 순으로
 * <code>@이름{ns}="값"</code>, 텍스트는 <code>"텍스트"</code>, CDATA 섹션, 주석, 처리 명령, 문서 유형은 마크업 형태로 쓴다. <code>template</code> 의 내용은
 * <code>content</code> 줄 아래에 쓴다. 줄 바꿈은 <code>\n</code> 으로 쓴다.
 */
public final class XhtmlTreeDump {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private XhtmlTreeDump() {
    }

    /**
     * Dumps the children of a node.
     * <p>
     * 노드의 자식을 덤프한다.
     *
     * @param parent a document or fragment / 문서 또는 조각
     * @return the dump / 덤프
     */
    public static String dump(Node parent) {
        return dump(parent, false);
    }

    /**
     * Dumps the children of a node, optionally leaving out the namespace declaration attributes.
     * <p>
     * 노드의 자식을 덤프한다. 네임스페이스 선언 속성은 빼 줄 수 있다.
     *
     * @param parent    a document or fragment / 문서 또는 조각
     * @param skipXmlns true to leave out <code>xmlns</code> attributes / <code>xmlns</code> 속성을 빼려면 true
     * @return the dump / 덤프
     */
    public static String dump(Node parent, boolean skipXmlns) {
        StringBuilder sb = new StringBuilder();
        // 자식을 순서대로 덤프한다.
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            dump(sb, c, 0, skipXmlns);
        }
        return sb.toString();
    }

    /**
     * Abbreviates a namespace URI.
     * <p>
     * 네임스페이스 URI 를 줄여 쓴다.
     *
     * @param ns the namespace / 네임스페이스
     * @return the abbreviation / 줄임말
     */
    private static String abbr(String ns) {
        if (Namespaces.HTML.equals(ns)) {
            return "h";
        }
        if (Namespaces.SVG.equals(ns)) {
            return "svg";
        }
        if (Namespaces.MATHML.equals(ns)) {
            return "math";
        }
        if (Namespaces.XML.equals(ns)) {
            return "xml";
        }
        if (Namespaces.XMLNS.equals(ns)) {
            return "xmlns";
        }
        if (Namespaces.XLINK.equals(ns)) {
            return "xlink";
        }
        return ns;
    }

    /**
     * Escapes line feeds.
     * <p>
     * 줄 바꿈을 이스케이프한다.
     *
     * @param s the text / 텍스트
     * @return the escaped text / 이스케이프한 텍스트
     */
    private static String esc(String s) {
        return s.replace("\n", "\\n");
    }

    /**
     * Writes the indentation.
     * <p>
     * 들여쓰기를 쓴다.
     *
     * @param sb    output / 출력
     * @param depth depth / 깊이
     */
    private static void indent(StringBuilder sb, int depth) {
        // 깊이마다 공백 두 칸을 더한다.
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
    }

    /**
     * Dumps a node and its descendants.
     * <p>
     * 노드와 후손을 덤프한다.
     *
     * @param sb        output / 출력
     * @param n         node / 노드
     * @param depth     depth / 깊이
     * @param skipXmlns true to leave out xmlns attributes / xmlns 속성을 빼려면 true
     */
    private static void dump(StringBuilder sb, Node n, int depth, boolean skipXmlns) {
        indent(sb, depth);
        switch (n.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element e = (Element) n;
            String qn = e.getPrefix() == null ? e.getLocalName() : e.getPrefix() + ":" + e.getLocalName();
            sb.append('<').append(e.getNamespaceURI() == null ? "" : abbr(e.getNamespaceURI()) + "|").append(qn).append(">\n");
            List<String> attrs = new ArrayList<String>();
            List<String> names = e.getAttributeNames();
            // 속성을 이름 순으로 정렬하기 위해 줄을 모은다.
            for (int i = 0; i < names.size(); i++) {
                Attr a = e.getAttributeNode(names.get(i));
                if (skipXmlns && Namespaces.XMLNS.equals(a.getNamespaceURI())) {
                    continue;
                }
                String ns = a.getNamespaceURI() == null ? "" : "{" + abbr(a.getNamespaceURI()) + "}";
                attrs.add("@" + a.getName() + ns + "=\"" + esc(a.getValue()) + "\"");
            }
            Collections.sort(attrs);
            // 정렬한 속성을 한 줄씩 쓴다.
            for (int i = 0; i < attrs.size(); i++) {
                indent(sb, depth + 1);
                sb.append(attrs.get(i)).append('\n');
            }
            DocumentFragment content = ParserNodeData.getTemplateContents(e);
            if (content != null) {
                indent(sb, depth + 1);
                sb.append("content\n");
                // 내용 조각의 자식을 차례로 덤프한다.
                for (Node c = content.getFirstChild(); c != null; c = c.getNextSibling()) {
                    dump(sb, c, depth + 2, skipXmlns);
                }
            }
            // 자식을 차례로 덤프한다.
            for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
                dump(sb, c, depth + 1, skipXmlns);
            }
            break;
        case Node.TEXT_NODE:
            sb.append('"').append(esc(((CharacterData) n).getData())).append("\"\n");
            break;
        case Node.CDATA_SECTION_NODE:
            sb.append("<![CDATA[").append(esc(((CharacterData) n).getData())).append("]]>\n");
            break;
        case Node.COMMENT_NODE:
            sb.append("<!--").append(esc(((CharacterData) n).getData())).append("-->\n");
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            sb.append("<?").append(((ProcessingInstruction) n).getTarget()).append(' ').append(esc(((CharacterData) n).getData())).append("?>\n");
            break;
        case Node.DOCUMENT_TYPE_NODE:
            DocumentType dt = (DocumentType) n;
            sb.append("<!DOCTYPE ").append(dt.getName()).append(" \"").append(dt.getPublicId()).append("\" \"").append(dt.getSystemId()).append("\">\n");
            break;
        default:
            sb.append("?node").append(n.getNodeType()).append('\n');
            break;
        }
    }
}
