package org.duckdns.hjow.swingwebpanel.css;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * A tiny markup reader for tests: it builds a DOM tree from simple well-formed markup (tags with quoted or bare attributes, text, comments,
 * void elements) without needing the real HTML parser, which belongs to another work package. It is not an HTML parser and does no error recovery.
 * <p>
 * 시험용 작은 마크업 읽기: 실제 HTML 파서(다른 작업 범위) 없이 단순하고 올바른 마크업(따옴표 있는 속성이나 없는 속성, 글, 주석, 빈 요소)에서 DOM 트리를 만든다.
 * HTML 파서가 아니며 오류 복구를 하지 않는다.
 */
public final class MiniHtml {

    /** Names of void elements. / 빈 요소 이름. */
    private static final Set<String> VOID = new HashSet<String>(Arrays.asList("input", "br", "img", "hr", "meta", "link", "col"));

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private MiniHtml() {
    }

    /**
     * Builds an HTML document whose document element is the first element of the markup.
     * <p>
     * 마크업의 첫 요소가 문서 요소인 HTML 문서를 만든다.
     *
     * @param markup well-formed markup with one root element / 루트 요소가 하나인 올바른 마크업
     * @return the document / 문서
     */
    public static Document parse(String markup) {
        Document doc = Document.createHtmlDocument();
        fill(doc, doc, markup);
        return doc;
    }

    /**
     * Parses markup into children of a node.
     * <p>
     * 마크업을 노드의 자식으로 파싱한다.
     *
     * @param doc    the owner document / 소유 문서
     * @param parent the node that receives the nodes / 노드를 받을 노드
     * @param markup the markup / 마크업
     */
    public static void fill(Document doc, Node parent, String markup) {
        Node cur = parent;
        int i = 0;
        int n = markup.length();
        // 마크업을 끝까지 읽으며 태그, 주석, 글을 만든다.
        while (i < n) {
            char c = markup.charAt(i);
            if (c != '<') {
                int j = markup.indexOf('<', i);
                if (j < 0) {
                    j = n;
                }
                String text = markup.substring(i, j);
                if (cur != doc || text.trim().length() > 0) {
                    cur.appendChild(doc.createTextNode(text));
                }
                i = j;
            } else if (markup.startsWith("<!--", i)) {
                int j = markup.indexOf("-->", i);
                cur.appendChild(doc.createComment(markup.substring(i + 4, j)));
                i = j + 3;
            } else if (markup.startsWith("</", i)) {
                int j = markup.indexOf('>', i);
                cur = cur.getParentNode();
                i = j + 1;
            } else {
                int j = markup.indexOf('>', i);
                boolean selfClose = markup.charAt(j - 1) == '/';
                String inner = markup.substring(i + 1, selfClose ? j - 1 : j).trim();
                int sp = 0;
                // 태그 이름의 끝(첫 공백)을 찾는다.
                while (sp < inner.length() && !Character.isWhitespace(inner.charAt(sp))) {
                    sp++;
                }
                String name = inner.substring(0, sp);
                Element e = doc.createElement(name);
                attributes(e, inner.substring(sp));
                cur.appendChild(e);
                if (!selfClose && !VOID.contains(name)) {
                    cur = e;
                }
                i = j + 1;
            }
        }
    }

    /**
     * Reads the attributes of a start tag.
     * <p>
     * 시작 태그의 속성을 읽는다.
     *
     * @param e    the element / 요소
     * @param text the text after the tag name / 태그 이름 뒤의 문자열
     */
    private static void attributes(Element e, String text) {
        int i = 0;
        int n = text.length();
        // 속성을 하나씩 읽는다: 이름, 선택적인 = 값(따옴표 있거나 없음).
        while (i < n) {
            // 속성 앞의 공백을 건너뛴다.
            while (i < n && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            int s = i;
            // 속성 이름의 끝(= 또는 공백)을 찾는다.
            while (i < n && text.charAt(i) != '=' && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            if (i == s) {
                break;
            }
            String name = text.substring(s, i);
            String value = "";
            if (i < n && text.charAt(i) == '=') {
                i++;
                if (i < n && (text.charAt(i) == '"' || text.charAt(i) == '\'')) {
                    char q = text.charAt(i++);
                    int j = text.indexOf(q, i);
                    value = text.substring(i, j);
                    i = j + 1;
                } else {
                    int j = i;
                    // 따옴표 없는 값의 끝(공백)을 찾는다.
                    while (j < n && !Character.isWhitespace(text.charAt(j))) {
                        j++;
                    }
                    value = text.substring(i, j);
                    i = j;
                }
            }
            e.setAttribute(name, value);
        }
    }
}
