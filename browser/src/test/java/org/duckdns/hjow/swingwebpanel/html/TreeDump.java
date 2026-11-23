package org.duckdns.hjow.swingwebpanel.html;

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

/**
 * Writes a DOM tree as text in the notation of the html5lib tree construction tests: one node per line, <code>| </code> in front and
 * two spaces of indentation per level; elements as <code>&lt;name&gt;</code> (foreign elements as <code>&lt;svg name&gt;</code> or
 * <code>&lt;math name&gt;</code>), their attributes sorted by name on the following lines as <code>name="value"</code> (namespaced
 * attributes as <code>xlink href="..."</code>), text as <code>"text"</code>, comments as <code>&lt;!-- text --&gt;</code>, DOCTYPE as
 * <code>&lt;!DOCTYPE name "public" "system"&gt;</code>, and the contents of a <code>template</code> under a line <code>content</code>.
 * Line feeds are written as <code>\n</code> and backslashes as <code>\\</code> so that every node stays on one line.
 * <p>
 * DOM 트리를 html5lib 트리 생성 시험의 표기로 문자열로 만든다: 노드마다 한 줄, 앞에 <code>| </code>, 단계마다 공백 두 칸 들여쓰기. 요소는
 * <code>&lt;이름&gt;</code>(외부 요소는 <code>&lt;svg 이름&gt;</code>, <code>&lt;math 이름&gt;</code>), 속성은 이름 순으로 다음 줄들에
 * <code>이름="값"</code>(네임스페이스가 있으면 <code>xlink href="..."</code>), 텍스트는 <code>"텍스트"</code>, 주석은 <code>&lt;!-- 텍스트 --&gt;</code>,
 * DOCTYPE 은 <code>&lt;!DOCTYPE 이름 "공개" "시스템"&gt;</code>, <code>template</code> 의 내용은 <code>content</code> 줄 아래에 쓴다. 줄바꿈은
 * <code>\n</code>, 역슬래시는 <code>\\</code> 로 써서 노드 하나가 항상 한 줄이 되게 한다.
 */
public final class TreeDump {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private TreeDump() {
    }

    /**
     * Dumps the children of a node (the node itself is not written).
     * <p>
     * 노드의 자식을 덤프한다(노드 자신은 쓰지 않는다).
     *
     * @param parent document or fragment / 문서 또는 조각
     * @return the dump, one line per node, each line ending with a line feed / 덤프. 노드당 한 줄이며 각 줄은 줄바꿈으로 끝난다
     */
    public static String dumpChildren(Node parent) {
        StringBuilder sb = new StringBuilder();
        // 자식을 순서대로 덤프한다.
        for (Node c = parent.getFirstChild(); c != null; c = c.getNextSibling()) {
            dump(sb, c, 0);
        }
        return sb.toString();
    }

    /**
     * Escapes line feeds and backslashes.
     * <p>
     * 줄바꿈과 역슬래시를 이스케이프한다.
     *
     * @param s text / 문자열
     * @return escaped text / 이스케이프된 문자열
     */
    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

    /**
     * Writes the indentation of a line.
     * <p>
     * 한 줄의 들여쓰기를 쓴다.
     *
     * @param sb    output / 출력
     * @param depth depth / 깊이
     */
    private static void indent(StringBuilder sb, int depth) {
        sb.append("| ");
        // 깊이마다 공백 두 칸을 더한다.
        for (int i = 0; i < depth; i++) {
            sb.append("  ");
        }
    }

    /**
     * Dumps a node and its descendants.
     * <p>
     * 노드와 그 자손을 덤프한다.
     *
     * @param sb    output / 출력
     * @param n     node / 노드
     * @param depth depth / 깊이
     */
    private static void dump(StringBuilder sb, Node n, int depth) {
        indent(sb, depth);
        switch (n.getNodeType()) {
        case Node.ELEMENT_NODE:
            Element e = (Element) n;
            sb.append('<');
            String ns = e.getNamespaceURI();
            if (Namespaces.SVG.equals(ns)) {
                sb.append("svg ");
            } else if (Namespaces.MATHML.equals(ns)) {
                sb.append("math ");
            }
            sb.append(e.getLocalName()).append(">\n");
            List<String> attrs = new ArrayList<String>();
            List<String> names = e.getAttributeNames();
            // 속성을 이름 순으로 정렬하기 위해 "이름=값" 줄을 모은다.
            for (int i = 0; i < names.size(); i++) {
                Attr a = e.getAttributeNode(names.get(i));
                String display = a.getNamespaceURI() == null ? a.getLocalName() : (a.getPrefix() == null ? "" : a.getPrefix() + " ") + a.getLocalName();
                attrs.add(display + "=\"" + esc(a.getValue()) + "\"");
            }
            Collections.sort(attrs);
            // 정렬된 속성을 한 줄씩 쓴다.
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
                    dump(sb, c, depth + 2);
                }
            }
            // 자식을 차례로 덤프한다.
            for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
                dump(sb, c, depth + 1);
            }
            break;
        case Node.TEXT_NODE:
            sb.append('"').append(esc(((CharacterData) n).getData())).append("\"\n");
            break;
        case Node.COMMENT_NODE:
            sb.append("<!-- ").append(esc(((CharacterData) n).getData())).append(" -->\n");
            break;
        case Node.DOCUMENT_TYPE_NODE:
            DocumentType dt = (DocumentType) n;
            sb.append("<!DOCTYPE ").append(dt.getName());
            if (dt.getPublicId().length() > 0 || dt.getSystemId().length() > 0) {
                sb.append(" \"").append(dt.getPublicId()).append("\" \"").append(dt.getSystemId()).append('"');
            }
            sb.append(">\n");
            break;
        default:
            sb.append("?? ").append(n.getNodeName()).append('\n');
            break;
        }
    }
}
