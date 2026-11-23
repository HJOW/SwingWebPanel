package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the DOM tree structure and mutation algorithms (plan items P06-01, P06-02, P06-08, P06-09): links, insertion, replacement,
 * removal, document and fragment rules, adoption, cloning, comparison, text content, character data and mutation records.
 * <p>
 * DOM 트리 구조와 변경 알고리즘 시험(계획 P06-01, P06-02, P06-08, P06-09): 연결, 삽입, 교체, 제거, 문서와 조각 규칙, adopt, 복제, 비교,
 * 텍스트 내용, 문자 데이터, 변경 기록.
 */
public class TreeMutationTest {

    /**
     * Document of the current case.
     * <p>
     * 현재 사례의 문서.
     */
    private Document doc;

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public TreeMutationTest() {
    }

    /**
     * Creates a fresh HTML document before each case.
     * <p>
     * 사례마다 새 HTML 문서를 만든다.
     */
    public void setUp() {
        doc = Document.createHtmlDocument();
    }

    /**
     * Creates an element with an id.
     * <p>
     * id 가 있는 요소를 만든다.
     *
     * @param name tag name / 태그 이름
     * @param id   id or null / id 또는 null
     * @return the element / 요소
     */
    private Element el(String name, String id) {
        Element e = doc.createElement(name);
        if (id != null) {
            e.setId(id);
        }
        return e;
    }

    /**
     * P06-01: parent, child and sibling links, child lists and owner documents after appending.
     * <p>
     * P06-01: 추가한 뒤의 부모, 자식, 형제 연결, 자식 목록, 소유 문서.
     */
    public void testLinksAndOwners() {
        Element a = el("div", "a");
        Element b = el("p", "b");
        Text t = doc.createTextNode("x");
        Comment c = doc.createComment("c");
        a.appendChild(b);
        a.appendChild(t);
        a.appendChild(c);
        Check.same(a, b.getParentNode(), "parent");
        Check.same(a, b.getParentElement(), "parent element");
        Check.same(b, a.getFirstChild(), "first child");
        Check.same(c, a.getLastChild(), "last child");
        Check.same(t, b.getNextSibling(), "next sibling");
        Check.same(b, t.getPreviousSibling(), "previous sibling");
        Check.isNull(b.getPreviousSibling(), "no previous sibling of the first child");
        Check.isNull(c.getNextSibling(), "no next sibling of the last child");
        Check.equal(3, a.getChildNodes().getLength(), "child count");
        Check.same(t, a.getChildNodes().item(1), "child item");
        Check.same(a.getChildNodes(), a.getChildNodes(), "the child list object is stable");
        Check.same(doc, a.getOwnerDocument(), "owner document");
        Check.isNull(doc.getOwnerDocument(), "a document has no owner document");
        Check.same(b, a.getFirstElementChild(), "first element child");
        Check.same(b, a.getLastElementChild(), "last element child");
        Check.equal(1, a.getChildElementCount(), "child element count");
        Check.same(b, t.getPreviousElementSibling(), "previous element sibling of the text");
        Check.equal("div#a(p#b,\"x\",#c)", DomTestSupport.describe(a), "tree description");
    }

    /**
     * P06-02: insertBefore, replaceChild and removeChild results and the move of an existing child.
     * <p>
     * P06-02: insertBefore, replaceChild, removeChild 의 결과와 기존 자식의 이동.
     */
    public void testInsertReplaceRemoveAndMove() {
        Element root = el("div", "r");
        Element a = el("a", "a");
        Element b = el("b", "b");
        Element c = el("i", "c");
        root.appendChild(a);
        root.appendChild(c);
        Check.same(b, root.insertBefore(b, c), "insertBefore returns the node");
        Check.equal("div#r(a#a,b#b,i#c)", DomTestSupport.describe(root), "after insertBefore");
        root.insertBefore(el("u", "u"), null);
        Check.equal("div#r(a#a,b#b,i#c,u#u)", DomTestSupport.describe(root), "a null reference appends");
        root.appendChild(a);
        Check.equal("div#r(b#b,i#c,u#u,a#a)", DomTestSupport.describe(root), "appending a child moves it");
        Element n = el("s", "n");
        Check.same(b, root.replaceChild(n, b), "replaceChild returns the old child");
        Check.equal("div#r(s#n,i#c,u#u,a#a)", DomTestSupport.describe(root), "after replaceChild");
        Check.isNull(b.getParentNode(), "the replaced child has no parent");
        Check.same(c, root.removeChild(c), "removeChild returns the child");
        Check.equal("div#r(s#n,u#u,a#a)", DomTestSupport.describe(root), "after removeChild");
        root.insertBefore(a, a);
        Check.equal("div#r(s#n,u#u,a#a)", DomTestSupport.describe(root), "inserting a node before itself keeps the place");
        root.replaceChild(u(root), n);
        Check.equal("div#r(u#u,a#a)", DomTestSupport.describe(root), "replacing with a sibling moves the sibling");
        a.remove();
        Check.equal("div#r(u#u)", DomTestSupport.describe(root), "ChildNode.remove");
        a.remove();
    }

    /**
     * Returns the child with the id u of a node.
     * <p>
     * 노드의 id 가 u 인 자식을 반환한다.
     *
     * @param root parent / 부모
     * @return the child / 자식
     */
    private Node u(Node root) {
        // 자식 중 id 가 u 인 요소를 찾는다.
        for (Node c = root.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element && "u".equals(((Element) c).getId())) {
                return c;
            }
        }
        return null;
    }

    /**
     * P06-02: invalid insertions throw the exceptions of the standard and change nothing.
     * <p>
     * P06-02: 잘못된 삽입은 표준이 정한 예외를 던지고 아무것도 바꾸지 않는다.
     */
    public void testHierarchyAndNotFoundErrors() {
        final Element a = el("div", "a");
        final Element b = el("div", "b");
        final Element c = el("div", "c");
        a.appendChild(b);
        b.appendChild(c);
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Appends an ancestor to its descendant.
             * <p>
             * 조상을 후손에 추가한다.
             */
            public void run() {
                c.appendChild(a);
            }
        }, "cycle through an ancestor");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Appends a node to itself.
             * <p>
             * 노드를 자기 자신에게 추가한다.
             */
            public void run() {
                a.appendChild(a);
            }
        }, "a node into itself");
        DomTestSupport.throwsDom("NotFoundError", new Block() {
            /**
             * Uses a reference child that is not a child.
             * <p>
             * 자식이 아닌 기준 자식을 쓴다.
             */
            public void run() {
                a.insertBefore(el("p", null), c);
            }
        }, "reference node that is not a child");
        DomTestSupport.throwsDom("NotFoundError", new Block() {
            /**
             * Removes a node that is not a child.
             * <p>
             * 자식이 아닌 노드를 제거한다.
             */
            public void run() {
                a.removeChild(c);
            }
        }, "removing a non-child");
        DomTestSupport.throwsDom("NotFoundError", new Block() {
            /**
             * Replaces a node that is not a child.
             * <p>
             * 자식이 아닌 노드를 교체한다.
             */
            public void run() {
                a.replaceChild(el("p", null), c);
            }
        }, "replacing a non-child");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Gives a text node a child.
             * <p>
             * 텍스트 노드에 자식을 준다.
             */
            public void run() {
                doc.createTextNode("t").appendChild(el("p", null));
            }
        }, "children of a text node");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Inserts a text node into a document.
             * <p>
             * 텍스트 노드를 문서에 삽입한다.
             */
            public void run() {
                doc.appendChild(doc.createTextNode("t"));
            }
        }, "text in a document");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Inserts a document type into an element.
             * <p>
             * 문서 유형을 요소에 삽입한다.
             */
            public void run() {
                a.appendChild(doc.createDocumentType("html", null, null));
            }
        }, "doctype in an element");
        Check.equal("div#a(div#b(div#c))", DomTestSupport.describe(a), "failed operations change nothing");
    }

    /**
     * P06-02: a document accepts one element and one doctype in the right order.
     * <p>
     * P06-02: 문서는 요소 하나와 문서 유형 하나를 올바른 순서로만 받는다.
     */
    public void testDocumentChildRules() {
        final Document d = Document.createHtmlDocument();
        final Element html = d.createElement("html");
        d.appendChild(html);
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Adds a second element child.
             * <p>
             * 두 번째 요소 자식을 추가한다.
             */
            public void run() {
                d.appendChild(d.createElement("other"));
            }
        }, "second element");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Adds a doctype after the element.
             * <p>
             * 요소 뒤에 문서 유형을 추가한다.
             */
            public void run() {
                d.appendChild(d.createDocumentType("html", null, null));
            }
        }, "doctype after the element");
        DocumentType dt = d.createDocumentType("html", null, null);
        d.insertBefore(dt, html);
        Check.same(dt, d.getDoctype(), "doctype before the element is fine");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Adds a second doctype.
             * <p>
             * 두 번째 문서 유형을 추가한다.
             */
            public void run() {
                d.insertBefore(d.createDocumentType("x", null, null), html);
            }
        }, "second doctype");
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Inserts an element before the doctype.
             * <p>
             * 문서 유형 앞에 요소를 삽입한다.
             */
            public void run() {
                Document d2 = Document.createHtmlDocument();
                d2.appendChild(d2.createDocumentType("html", null, null));
                d2.insertBefore(d2.createElement("p"), d2.getFirstChild());
            }
        }, "element before the doctype");
        DocumentFragment f = d.createDocumentFragment();
        f.appendChild(d.createElement("a"));
        f.appendChild(d.createElement("b"));
        final DocumentFragment two = f;
        DomTestSupport.throwsDom("HierarchyRequestError", new Block() {
            /**
             * Inserts a fragment with two elements.
             * <p>
             * 요소 둘이 든 조각을 삽입한다.
             */
            public void run() {
                Document d3 = Document.createHtmlDocument();
                d3.appendChild(d3.importNode(two, true));
            }
        }, "fragment with two elements");
        Check.same(html, d.getDocumentElement(), "document element");
    }

    /**
     * P06-02: inserting a fragment moves its children and empties it; replacing with a fragment works.
     * <p>
     * P06-02: 조각을 삽입하면 그 자식이 옮겨지고 조각은 비며, 조각으로 교체할 수도 있다.
     */
    public void testFragments() {
        Element root = el("div", "r");
        root.appendChild(el("a", "a"));
        root.appendChild(el("z", "z"));
        DocumentFragment f = doc.createDocumentFragment();
        f.appendChild(el("b", "b"));
        f.appendChild(el("c", "c"));
        root.insertBefore(f, root.getLastChild());
        Check.equal("div#r(a#a,b#b,c#c,z#z)", DomTestSupport.describe(root), "fragment children are inserted");
        Check.equal(0, f.getChildNodes().getLength(), "the fragment is empty afterwards");
        DocumentFragment g = doc.createDocumentFragment();
        g.appendChild(el("x", "x"));
        g.appendChild(el("y", "y"));
        root.replaceChild(g, root.getFirstChild());
        Check.equal("div#r(x#x,y#y,b#b,c#c,z#z)", DomTestSupport.describe(root), "replace with a fragment");
        root.insertBefore(doc.createDocumentFragment(), null);
        Check.equal(5, root.getChildNodes().getLength(), "an empty fragment inserts nothing");
    }

    /**
     * P06-02: nodes moved between documents change their owner, including the whole subtree and attributes.
     * <p>
     * P06-02: 문서 사이로 옮긴 노드는 하위 트리 전체와 속성을 포함해 소유자가 바뀐다.
     */
    public void testAdoptionAndImport() {
        Document other = Document.createHtmlDocument();
        Element e = other.createElement("div");
        e.setAttribute("title", "t");
        Element child = other.createElement("span");
        e.appendChild(child);
        doc.appendChild(doc.createElement("html"));
        doc.getDocumentElement().appendChild(e);
        Check.same(doc, e.getOwnerDocument(), "the node moved to the new document");
        Check.same(doc, child.getOwnerDocument(), "the whole subtree moved");
        Check.same(doc, e.getAttributeNode("title").getOwnerDocument(), "attributes moved");
        Check.isNull(other.getFirstChild(), "nothing is left in the old tree");
        Element source = other.createElement("p");
        Node copy = doc.importNode(source, true);
        Check.same(doc, copy.getOwnerDocument(), "an imported copy belongs to the new document");
        Check.same(other, source.getOwnerDocument(), "the source stays where it was");
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Imports a document.
             * <p>
             * 문서를 가져온다.
             */
            public void run() {
                doc.importNode(Document.createHtmlDocument(), true);
            }
        }, "importing a document");
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Adopts a document.
             * <p>
             * 문서를 옮긴다.
             */
            public void run() {
                doc.adoptNode(Document.createHtmlDocument());
            }
        }, "adopting a document");
    }

    /**
     * P06-02: clones copy names, attributes and (deep) the subtree, and are independent of the original.
     * <p>
     * P06-02: 복제는 이름, 속성, (deep 이면) 하위 트리를 복사하고 원본과 독립적이다.
     */
    public void testCloneNode() {
        Element root = el("div", "r");
        root.setAttribute("class", "k1 k2");
        root.appendChild(el("p", "p"));
        root.appendChild(doc.createTextNode("text"));
        root.appendChild(doc.createComment("c"));
        Element shallow = (Element) root.cloneNode(false);
        Check.equal("div#r", DomTestSupport.describe(shallow), "shallow clone has no children");
        Check.equal("k1 k2", shallow.getAttribute("class"), "attributes are copied");
        Element deep = (Element) root.cloneNode(true);
        Check.equal(DomTestSupport.describe(root), DomTestSupport.describe(deep), "deep clone has the same tree");
        Check.isTrue(deep.isEqualNode(root), "the deep clone is equal");
        Check.isFalse(deep == root, "but not the same object");
        Check.isNull(deep.getParentNode(), "a clone has no parent");
        Check.same(doc, deep.getOwnerDocument(), "a clone belongs to the same document");
        deep.setAttribute("class", "changed");
        Check.equal("k1 k2", root.getAttribute("class"), "changing the clone does not change the original");
        Check.isFalse(deep.isEqualNode(root), "the changed clone is no longer equal");
        Document dc = (Document) doc.cloneNode(false);
        Check.isTrue(dc.isHtmlDocument(), "a cloned document keeps its type");
        Check.isFalse(dc == doc, "a cloned document is a new object");
    }

    /**
     * P06-02: contains, root, connected state and document position.
     * <p>
     * P06-02: contains, 루트, 연결 상태, 문서 내 위치.
     */
    public void testPositionsAndConnectedState() {
        Element html = el("html", null);
        doc.appendChild(html);
        Element body = el("body", null);
        Element a = el("a", null);
        Element b = el("b", null);
        html.appendChild(body);
        body.appendChild(a);
        body.appendChild(b);
        Element loose = el("div", null);
        Check.isTrue(html.contains(a), "contains a descendant");
        Check.isTrue(a.contains(a), "contains itself");
        Check.isFalse(a.contains(html), "does not contain its ancestor");
        Check.same(doc, a.getRootNode(), "root of a connected node");
        Check.same(loose, loose.getRootNode(), "root of a detached node");
        Check.isTrue(a.isConnected(), "connected");
        Check.isFalse(loose.isConnected(), "detached");
        Check.equal(Node.DOCUMENT_POSITION_FOLLOWING, a.compareDocumentPosition(b), "b follows a");
        Check.equal(Node.DOCUMENT_POSITION_PRECEDING, b.compareDocumentPosition(a), "a precedes b");
        Check.equal(Node.DOCUMENT_POSITION_CONTAINED_BY | Node.DOCUMENT_POSITION_FOLLOWING, html.compareDocumentPosition(a), "a is inside html");
        Check.equal(Node.DOCUMENT_POSITION_CONTAINS | Node.DOCUMENT_POSITION_PRECEDING, a.compareDocumentPosition(html), "html contains a");
        Check.equal(0, a.compareDocumentPosition(a), "same node");
        Check.isTrue((a.compareDocumentPosition(loose) & Node.DOCUMENT_POSITION_DISCONNECTED) != 0, "different trees are disconnected");
        Check.same(body, a.getParentElement(), "parent element");
    }

    /**
     * P06-03: text content is never parsed as markup; getting it concatenates the text descendants only.
     * <p>
     * P06-03: 텍스트 내용은 마크업으로 해석되지 않으며, 읽을 때는 텍스트 후손만 이어 붙인다.
     */
    public void testTextContent() {
        Element e = el("div", "d");
        e.appendChild(doc.createTextNode("a"));
        Element inner = el("b", null);
        inner.appendChild(doc.createTextNode("b"));
        e.appendChild(inner);
        e.appendChild(doc.createComment("no"));
        e.appendChild(doc.createTextNode("c"));
        Check.equal("abc", e.getTextContent(), "text of the descendants without comments");
        e.setTextContent("<b>bold</b> & more");
        Check.equal(1, e.getChildNodes().getLength(), "setting the text creates one text node");
        Check.equal(Node.TEXT_NODE, e.getFirstChild().getNodeType(), "the child is a text node, not an element");
        Check.equal("<b>bold</b> & more", e.getTextContent(), "the text is stored as given");
        e.setTextContent("");
        Check.isFalse(e.hasChildNodes(), "an empty text removes all children");
        Check.isNull(doc.getTextContent(), "a document has no text content");
        Check.isNull(doc.createDocumentType("html", null, null).getTextContent(), "a doctype has no text content");
        Comment c = doc.createComment("note");
        Check.equal("note", c.getTextContent(), "text content of a comment is its data");
        c.setTextContent("changed");
        Check.equal("changed", c.getData(), "setting the text content of a comment changes its data");
    }

    /**
     * P06-03: character data operations use UTF-16 code units and report IndexSizeError.
     * <p>
     * P06-03: 문자 데이터 연산은 UTF-16 코드 단위를 쓰고 IndexSizeError 를 보고한다.
     */
    public void testCharacterData() {
        final Text t = doc.createTextNode("hello");
        Check.equal("ell", t.substringData(1, 3), "substringData");
        Check.equal("ello", t.substringData(1, 100), "the count is clamped");
        t.appendData("!");
        t.insertData(0, ">");
        Check.equal(">hello!", t.getData(), "append and insert");
        t.deleteData(0, 1);
        t.replaceData(0, 5, "HELLO");
        Check.equal("HELLO!", t.getData(), "delete and replace");
        Check.equal(6, t.getLength(), "length");
        DomTestSupport.throwsDom("IndexSizeError", new Block() {
            /**
             * Reads beyond the data.
             * <p>
             * 데이터 밖을 읽는다.
             */
            public void run() {
                t.substringData(7, 1);
            }
        }, "offset beyond the data");
        Text emoji = doc.createTextNode("a😀b");
        Check.equal(4, emoji.getLength(), "a surrogate pair counts as two code units");
        Check.equal("b", emoji.substringData(3, 1), "offsets are code units");
        Text split = doc.createTextNode("abcdef");
        Element p = el("p", null);
        p.appendChild(split);
        Text rest = split.splitText(2);
        Check.equal("ab", split.getData(), "the first part stays");
        Check.equal("cdef", rest.getData(), "the rest moves to a new sibling");
        Check.same(rest, split.getNextSibling(), "the new node follows");
        Check.equal("abcdef", split.getWholeText(), "wholeText joins adjacent text nodes");
        p.normalize();
        Check.equal(1, p.getChildNodes().getLength(), "normalize merges adjacent text nodes");
        Check.equal("abcdef", ((Text) p.getFirstChild()).getData(), "merged data");
        p.appendChild(doc.createTextNode(""));
        p.normalize();
        Check.equal(1, p.getChildNodes().getLength(), "normalize removes empty text nodes");
    }

    /**
     * P06-02: ParentNode and ChildNode convenience methods accept nodes and strings.
     * <p>
     * P06-02: ParentNode 와 ChildNode 의 편의 메서드는 노드와 문자열을 받는다.
     */
    public void testParentAndChildNodeMethods() {
        Element root = el("div", "r");
        Element b = el("b", "b");
        root.append(b, "tail");
        root.prepend("head", el("a", "a"));
        Check.equal("div#r(\"head\",a#a,b#b,\"tail\")", DomTestSupport.describe(root), "append and prepend");
        b.before(el("x", "x"), "mid");
        b.after(el("y", "y"));
        Check.equal("div#r(\"head\",a#a,x#x,\"mid\",b#b,y#y,\"tail\")", DomTestSupport.describe(root), "before and after");
        b.replaceWith(el("z", "z"), "r1");
        Check.equal("div#r(\"head\",a#a,x#x,\"mid\",z#z,\"r1\",y#y,\"tail\")", DomTestSupport.describe(root), "replaceWith");
        el("orphan", null).before("ignored");
    }

    /**
     * P06-01: HTML and XML naming rules for elements and attributes, and the name validation errors.
     * <p>
     * P06-01: 요소와 속성의 HTML/XML 이름 규칙과 이름 검증 오류.
     */
    public void testNamesAndNamespaces() {
        Element h = doc.createElement("DiV");
        Check.equal("div", h.getLocalName(), "HTML documents lower-case element names");
        Check.equal("DIV", h.getTagName(), "the tag name of an HTML element is upper case");
        Check.equal("DIV", h.getNodeName(), "the node name is the tag name");
        Check.equal(Namespaces.HTML, h.getNamespaceURI(), "HTML namespace");
        Document xml = Document.createXmlDocument("application/xml");
        Element x = xml.createElement("DiV");
        Check.equal("DiV", x.getTagName(), "XML documents keep the case");
        Check.isNull(x.getNamespaceURI(), "no namespace in a plain XML document");
        Document xhtml = Document.createXmlDocument("application/xhtml+xml");
        Check.equal(Namespaces.HTML, xhtml.createElement("p").getNamespaceURI(), "XHTML elements are in the HTML namespace");
        Check.equal("p", xhtml.createElement("p").getTagName(), "but their tag name keeps its case");
        Element svg = doc.createElementNS(Namespaces.SVG, "svg:Rect");
        Check.equal("svg", svg.getPrefix(), "prefix");
        Check.equal("Rect", svg.getLocalName(), "local name");
        Check.equal("svg:Rect", svg.getTagName(), "SVG tag name keeps its case");
        DomTestSupport.throwsDom("InvalidCharacterError", new Block() {
            /**
             * Creates an element with a space in the name.
             * <p>
             * 이름에 공백이 있는 요소를 만든다.
             */
            public void run() {
                doc.createElement("a b");
            }
        }, "invalid element name");
        DomTestSupport.throwsDom("InvalidCharacterError", new Block() {
            /**
             * Creates an element with a name starting with a digit.
             * <p>
             * 숫자로 시작하는 이름의 요소를 만든다.
             */
            public void run() {
                doc.createElement("1a");
            }
        }, "name starting with a digit");
        DomTestSupport.throwsDom("NamespaceError", new Block() {
            /**
             * Uses a prefix without namespace.
             * <p>
             * 네임스페이스 없이 접두사를 쓴다.
             */
            public void run() {
                doc.createElementNS(null, "p:a");
            }
        }, "prefix without namespace");
        DomTestSupport.throwsDom("NamespaceError", new Block() {
            /**
             * Uses the xml prefix with another namespace.
             * <p>
             * 다른 네임스페이스로 xml 접두사를 쓴다.
             */
            public void run() {
                doc.createElementNS("urn:x", "xml:a");
            }
        }, "xml prefix with a wrong namespace");
        DomTestSupport.throwsDom("NamespaceError", new Block() {
            /**
             * Uses xmlns with a wrong namespace.
             * <p>
             * 잘못된 네임스페이스로 xmlns 를 쓴다.
             */
            public void run() {
                doc.createElementNS("urn:x", "xmlns:a");
            }
        }, "xmlns with a wrong namespace");
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Creates a CDATA section in an HTML document.
             * <p>
             * HTML 문서에서 CDATA 섹션을 만든다.
             */
            public void run() {
                doc.createCDATASection("x");
            }
        }, "CDATA in HTML");
        Check.equal(Node.CDATA_SECTION_NODE, xml.createCDATASection("x").getNodeType(), "CDATA in XML");
        DomTestSupport.throwsDom("InvalidCharacterError", new Block() {
            /**
             * Creates a processing instruction with ?> in the data.
             * <p>
             * 데이터에 ?> 가 든 처리 명령을 만든다.
             */
            public void run() {
                doc.createProcessingInstruction("t", "a?>b");
            }
        }, "processing instruction data");
        ProcessingInstruction pi = doc.createProcessingInstruction("xml-stylesheet", "href='a.css'");
        Check.equal("xml-stylesheet", pi.getNodeName(), "processing instruction name");
    }

    /**
     * P06-08: every change creates a record with the right data; changes in detached subtrees are marked as detached.
     * <p>
     * P06-08: 모든 변경이 올바른 데이터의 기록을 만들고, 분리된 하위 트리의 변경은 분리됨으로 표시된다.
     */
    public void testMutationRecords() {
        DomTestSupport.RecordingMutations rec = new DomTestSupport.RecordingMutations();
        doc.addMutationListener(rec);
        Element html = el("html", null);
        doc.appendChild(html);
        Element body = el("body", null);
        html.appendChild(body);
        body.setAttribute("class", "a");
        body.setAttribute("class", "b");
        body.removeAttribute("class");
        Text t = doc.createTextNode("x");
        body.appendChild(t);
        t.setData("y");
        Element loose = el("div", null);
        loose.appendChild(el("span", null));
        loose.setAttribute("id", "z");
        body.replaceChild(loose, t);
        body.removeChild(loose);
        List<String> expected = new ArrayList<String>();
        expected.add("childList #doc +1 -0 [connected]");
        expected.add("childList html +1 -0 [connected]");
        expected.add("attributes body class old=null [connected]");
        expected.add("attributes body class old=a [connected]");
        expected.add("attributes body class old=b [connected]");
        expected.add("childList body +1 -0 [connected]");
        expected.add("characterData \"y\" old=x [connected]");
        expected.add("childList div +1 -0 [detached]");
        expected.add("attributes div#z id old=null [detached]");
        expected.add("childList body +1 -1 [connected]");
        expected.add("childList body +0 -1 [connected]");
        Check.equal(expected, rec.events, "records in order");
        long version = doc.getMutationVersion();
        body.appendChild(el("p", null));
        Check.isTrue(doc.getMutationVersion() > version, "the mutation version grows with structure changes");
        doc.removeMutationListener(rec);
        int before = rec.events.size();
        body.appendChild(el("p", null));
        Check.equal(before, rec.events.size(), "a removed listener gets nothing");
    }

    /**
     * P06-08: a failing mutation listener does not break the DOM operation and is reported to the error handler.
     * <p>
     * P06-08: 실패하는 변경 리스너가 DOM 연산을 깨뜨리지 않고 오류 핸들러에 보고된다.
     */
    public void testFailingMutationListenerIsReported() {
        final List<Throwable> errors = new ArrayList<Throwable>();
        doc.setListenerErrorHandler(new ListenerErrorHandler() {
            /**
             * Collects the failure.
             * <p>
             * 실패를 모은다.
             *
             * @param target  target / 대상
             * @param event   event / 이벤트
             * @param failure failure / 실패
             */
            public void listenerFailed(EventTarget target, Event event, Throwable failure) {
                errors.add(failure);
            }
        });
        doc.addMutationListener(new DomMutationListener() {
            /**
             * Always fails.
             * <p>
             * 항상 실패한다.
             *
             * @param record    record / 기록
             * @param connected connected flag / 연결 플래그
             */
            public void mutated(MutationRecord record, boolean connected) {
                throw new IllegalStateException("expected");
            }
        });
        Element e = el("div", null);
        doc.appendChild(e);
        Check.same(e, doc.getFirstChild(), "the operation succeeded");
        Check.equal(1, errors.size(), "the failure was reported");
    }
}
