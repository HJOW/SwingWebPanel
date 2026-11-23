package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.testing.Block;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of the attribute, class list, live collection, id lookup and hook APIs of the DOM (plan items P06-03, P06-04, P06-05, P06-09).
 * <p>
 * DOM 의 속성, 클래스 목록, 라이브 컬렉션, id 조회, 확장 지점 API 시험(계획 P06-03, P06-04, P06-05, P06-09).
 */
public class NodeApiTest {

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
    public NodeApiTest() {
    }

    /**
     * Creates a fresh HTML document with html/head/body before each case.
     * <p>
     * 사례마다 html/head/body 가 있는 새 HTML 문서를 만든다.
     */
    public void setUp() {
        doc = Document.createHtmlDocument().getImplementation().createHtmlDocument("t");
    }

    /**
     * Returns the body element of the case document.
     * <p>
     * 사례 문서의 body 요소를 반환한다.
     *
     * @return body / body
     */
    private Element body() {
        return (Element) doc.getDocumentElement().getLastChild();
    }

    /**
     * P06-03: attribute access, HTML name normalization, ordering and the attribute map view.
     * <p>
     * P06-03: 속성 접근, HTML 이름 정규화, 순서, 속성 맵 뷰.
     */
    public void testAttributes() {
        Element e = doc.createElement("div");
        Check.isNull(e.getAttribute("a"), "absent attribute");
        e.setAttribute("DATA-X", "1");
        e.setAttribute("title", "t");
        Check.equal("1", e.getAttribute("data-x"), "HTML attribute names are lower-cased");
        Check.equal("1", e.getAttribute("DATA-X"), "and looked up in lower case");
        Check.isTrue(e.hasAttribute("Title"), "hasAttribute is case-insensitive in HTML");
        Check.equal(Arrays.asList("data-x", "title"), e.getAttributeNames(), "names keep insertion order");
        e.setAttribute("data-x", "2");
        Check.equal(Arrays.asList("data-x", "title"), e.getAttributeNames(), "changing a value keeps the position");
        Check.equal(2, e.getAttributes().getLength(), "attribute map length");
        Check.equal("t", e.getAttributes().item(1).getValue(), "attribute map item");
        Check.equal("2", e.getAttributes().getNamedItem("data-x").getValue(), "attribute map lookup");
        e.removeAttribute("DATA-X");
        e.removeAttribute("missing");
        Check.equal(1, e.getAttributes().getLength(), "after removal");
        Check.isTrue(e.hasAttributes(), "has attributes");
        Check.isTrue(e.toggleAttribute("hidden", null), "toggle adds");
        Check.equal("", e.getAttribute("hidden"), "toggle adds an empty value");
        Check.isFalse(e.toggleAttribute("hidden", null), "toggle removes");
        Check.isTrue(e.toggleAttribute("hidden", Boolean.TRUE), "force true adds");
        Check.isTrue(e.toggleAttribute("hidden", Boolean.TRUE), "force true keeps");
        Check.isFalse(e.toggleAttribute("hidden", Boolean.FALSE), "force false removes");
        Element x = doc.createElementNS("urn:x", "x:Item");
        x.setAttribute("Mixed", "v");
        Check.equal("v", x.getAttribute("Mixed"), "non-HTML elements keep attribute name case");
        Check.isNull(x.getAttribute("mixed"), "and match case-sensitively");
        e.setAttributeNS(Namespaces.XLINK, "xlink:href", "u");
        Check.equal("u", e.getAttributeNS(Namespaces.XLINK, "href"), "namespaced attribute by local name");
        Check.equal("u", e.getAttribute("xlink:href"), "and by qualified name");
        Check.isTrue(e.hasAttributeNS(Namespaces.XLINK, "href"), "hasAttributeNS");
        e.removeAttributeNS(Namespaces.XLINK, "href");
        Check.isFalse(e.hasAttributeNS(Namespaces.XLINK, "href"), "removeAttributeNS");
        final Element bad = e;
        DomTestSupport.throwsDom("InvalidCharacterError", new Block() {
            /**
             * Sets an attribute with an invalid name.
             * <p>
             * 잘못된 이름의 속성을 지정한다.
             */
            public void run() {
                bad.setAttribute("a b", "x");
            }
        }, "invalid attribute name");
    }

    /**
     * P06-03: attribute objects, ownership and the InUse error.
     * <p>
     * P06-03: 속성 객체, 소유, InUse 오류.
     */
    public void testAttributeNodes() {
        final Element a = doc.createElement("div");
        final Element b = doc.createElement("div");
        Attr attr = doc.createAttribute("Foo");
        Check.equal("foo", attr.getName(), "createAttribute lower-cases in HTML documents");
        attr.setValue("v");
        Check.isNull(a.setAttributeNode(attr), "no attribute was replaced");
        Check.same(a, attr.getOwnerElement(), "owner element");
        Check.equal("v", a.getAttribute("foo"), "attribute node value is visible");
        attr.setValue("w");
        Check.equal("w", a.getAttribute("foo"), "changing the attribute node changes the element");
        final Attr taken = attr;
        DomTestSupport.throwsDom("InUseAttributeError", new Block() {
            /**
             * Adds an attribute that belongs to another element.
             * <p>
             * 다른 요소의 속성을 추가한다.
             */
            public void run() {
                b.setAttributeNode(taken);
            }
        }, "attribute of another element");
        Attr other = doc.createAttribute("foo");
        other.setValue("z");
        Check.same(attr, a.setAttributeNode(other), "the replaced attribute is returned");
        Check.isNull(attr.getOwnerElement(), "the replaced attribute has no owner");
        Check.same(other, a.removeAttributeNode(other), "removeAttributeNode");
        DomTestSupport.throwsDom("NotFoundError", new Block() {
            /**
             * Removes an attribute that is not on the element.
             * <p>
             * 요소에 없는 속성을 제거한다.
             */
            public void run() {
                a.removeAttributeNode(taken);
            }
        }, "removing a foreign attribute");
    }

    /**
     * P06-03: id and className accessors and the live class list.
     * <p>
     * P06-03: id 와 className 접근자와 라이브 클래스 목록.
     */
    public void testClassListAndIdAccessors() {
        Element e = doc.createElement("p");
        Check.equal("", e.getId(), "no id");
        e.setId("x");
        Check.equal("x", e.getAttribute("id"), "id is the attribute");
        Check.equal("", e.getClassName(), "no class");
        final ClassList cl = e.getClassList();
        Check.same(cl, e.getClassList(), "the class list object is stable");
        cl.add("a", "b", "a");
        Check.equal("a b", e.getClassName(), "add keeps order without duplicates");
        Check.isTrue(cl.contains("b"), "contains");
        Check.equal(2, cl.getLength(), "length");
        Check.equal("b", cl.item(1), "item");
        Check.isNull(cl.item(5), "item out of range");
        e.setClassName("  x   y\tx  ");
        Check.equal(Arrays.asList("x", "y"), cl.toList(), "the list follows the attribute and ignores duplicates and whitespace");
        Check.equal("  x   y\tx  ", cl.getValue(), "value is the raw attribute");
        cl.remove("x");
        Check.equal("y", e.getClassName(), "remove re-serializes");
        Check.isTrue(cl.toggle("z"), "toggle adds");
        Check.isFalse(cl.toggle("z"), "toggle removes");
        Check.isTrue(cl.toggle("w", Boolean.TRUE), "force true");
        Check.isFalse(cl.toggle("w", Boolean.FALSE), "force false");
        Check.equal("y", e.getClassName(), "state after toggles");
        Check.isTrue(cl.replace("y", "q"), "replace");
        Check.isFalse(cl.replace("nope", "r"), "replace of a missing token");
        Check.equal("q", e.getClassName(), "after replace");
        Element f = doc.createElement("p");
        f.getClassList().remove("a");
        Check.isFalse(f.hasAttribute("class"), "removing from an absent attribute does not create it");
        DomTestSupport.throwsDom("SyntaxError", new Block() {
            /**
             * Adds an empty token.
             * <p>
             * 빈 토큰을 추가한다.
             */
            public void run() {
                cl.add("");
            }
        }, "empty token");
        DomTestSupport.throwsDom("InvalidCharacterError", new Block() {
            /**
             * Adds a token with a space.
             * <p>
             * 공백이 든 토큰을 추가한다.
             */
            public void run() {
                cl.add("a b");
            }
        }, "token with whitespace");
        Check.equal("q", e.getClassName(), "invalid tokens change nothing");
    }

    /**
     * P06-04: live collections update after insertions, removals and attribute changes, and keep their identity.
     * <p>
     * P06-04: 라이브 컬렉션은 삽입, 제거, 속성 변경 뒤에 갱신되고 동일성을 유지한다.
     */
    public void testLiveCollections() {
        Element body = body();
        ElementCollection divs = doc.getElementsByTagName("DIV");
        ElementCollection all = body.getElementsByTagName("*");
        ElementCollection kids = body.getChildren();
        Check.same(kids, body.getChildren(), "children collection is stable");
        Check.equal(0, divs.getLength(), "initially empty");
        Element d1 = doc.createElement("div");
        d1.setId("d1");
        body.appendChild(d1);
        Element span = doc.createElement("span");
        d1.appendChild(span);
        Element d2 = doc.createElement("div");
        d2.setId("d2");
        body.appendChild(d2);
        Check.equal(2, divs.getLength(), "live: two divs now (tag name matching is case-insensitive in HTML)");
        Check.same(d1, divs.item(0), "tree order");
        Check.equal(3, all.getLength(), "all descendants of body");
        Check.equal(2, kids.getLength(), "element children only");
        Check.isNull(divs.item(2), "item out of range");
        body.removeChild(d1);
        Check.equal(1, divs.getLength(), "live: after removal");
        Check.same(d2, divs.item(0), "remaining div");
        Check.same(d2, kids.namedItem("d2"), "namedItem by id");
        Element named = doc.createElement("form");
        named.setAttribute("name", "f");
        body.appendChild(named);
        Check.same(named, kids.namedItem("f"), "namedItem by name for HTML elements");
        Check.equal(2, ((List<Element>) ElementCollections.snapshot(kids)).size(), "snapshot size");
    }

    /**
     * P06-04: class name collections react to class attribute changes and honor quirks mode.
     * <p>
     * P06-04: 클래스 이름 컬렉션은 class 속성 변경에 반응하고 호환 모드를 따른다.
     */
    public void testClassNameCollections() {
        Element body = body();
        Element a = doc.createElement("p");
        a.setAttribute("class", "alpha beta");
        Element b = doc.createElement("p");
        b.setAttribute("class", "Beta");
        body.appendChild(a);
        body.appendChild(b);
        ElementCollection both = doc.getElementsByClassName("alpha beta");
        ElementCollection beta = doc.getElementsByClassName("beta");
        Check.equal(1, both.getLength(), "all class names must be present");
        Check.equal(1, beta.getLength(), "class names are case-sensitive in standards mode");
        b.setClassName("beta alpha");
        Check.equal(2, both.getLength(), "live: after the class attribute changed");
        Check.equal(0, doc.getElementsByClassName("").getLength(), "no class names gives an empty collection");
        Check.equal(0, doc.getElementsByClassName("  ").getLength(), "only whitespace gives an empty collection");
        b.setClassName("BETA");
        doc.setCompatibilityMode(Document.QUIRKS_MODE);
        Check.equal(2, doc.getElementsByClassName("beta").getLength(), "case-insensitive in quirks mode");
    }

    /**
     * P06-04: namespace based collections and XML case sensitivity.
     * <p>
     * P06-04: 네임스페이스 기반 컬렉션과 XML 의 대소문자 구분.
     */
    public void testTagNameNsAndXmlCase() {
        Document xml = Document.createXmlDocument("application/xml");
        Element root = xml.createElement("Root");
        xml.appendChild(root);
        root.appendChild(xml.createElementNS("urn:a", "a:Item"));
        root.appendChild(xml.createElementNS("urn:b", "b:Item"));
        root.appendChild(xml.createElement("item"));
        Check.equal(1, xml.getElementsByTagName("item").getLength(), "XML tag names are case-sensitive");
        Check.equal(1, xml.getElementsByTagName("a:Item").getLength(), "qualified name match");
        Check.equal(2, xml.getElementsByTagNameNS("*", "Item").getLength(), "any namespace");
        Check.equal(1, xml.getElementsByTagNameNS("urn:a", "Item").getLength(), "one namespace");
        Check.equal(1, xml.getElementsByTagNameNS("", "item").getLength(), "empty namespace means no namespace");
        Check.equal(3, xml.getElementsByTagNameNS("*", "*").getLength() - 1, "all elements below the root plus the root");
    }

    /**
     * P06-04: getElementById returns the first element in tree order and follows id changes and removals.
     * <p>
     * P06-04: getElementById 는 트리 순서의 첫 요소를 반환하고 id 변경과 제거를 따른다.
     */
    public void testGetElementById() {
        Element body = body();
        Element a = doc.createElement("div");
        a.setId("same");
        Element b = doc.createElement("div");
        b.setId("same");
        body.appendChild(a);
        body.appendChild(b);
        Check.same(a, doc.getElementById("same"), "the first element in tree order");
        body.removeChild(a);
        Check.same(b, doc.getElementById("same"), "after removing the first one");
        b.setId("other");
        Check.isNull(doc.getElementById("same"), "after the id changed");
        Check.same(b, doc.getElementById("other"), "new id");
        Check.isNull(doc.getElementById(""), "empty id");
        Check.isNull(doc.getElementById(null), "null id");
        Element detached = doc.createElement("div");
        detached.setId("lost");
        Check.isNull(doc.getElementById("lost"), "detached elements are not found");
        DocumentFragment f = doc.createDocumentFragment();
        f.appendChild(detached);
        Check.same(detached, f.getElementById("lost"), "a fragment can search its own descendants");
    }

    /**
     * P06-05: without installed services the selector and markup methods throw NotSupportedError; with a service they delegate to it.
     * <p>
     * P06-05: 서비스가 설치되지 않으면 선택자와 마크업 메서드는 NotSupportedError 를 던지고, 설치되면 서비스에 위임한다.
     */
    public void testSelectorAndMarkupHooks() {
        final Element body = body();
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Queries without a selector engine.
             * <p>
             * 선택자 엔진 없이 조회한다.
             */
            public void run() {
                body.querySelector("p");
            }
        }, "querySelector without an engine");
        DomTestSupport.throwsDom("NotSupportedError", new Block() {
            /**
             * Reads innerHTML without markup support.
             * <p>
             * 마크업 지원 없이 innerHTML 을 읽는다.
             */
            public void run() {
                body.getInnerHtml();
            }
        }, "innerHTML without markup support");
        final Element p = doc.createElement("p");
        body.appendChild(p);
        final List<String> calls = new ArrayList<String>();
        doc.setSelectorEngine(new SelectorEngine() {
            /**
             * Records the call and returns the paragraph.
             * <p>
             * 호출을 기록하고 문단을 반환한다.
             *
             * @param root      root / 루트
             * @param selectors selectors / 선택자
             * @return the paragraph / 문단
             */
            public Element querySelector(Node root, String selectors) {
                calls.add("qs:" + selectors);
                return p;
            }

            /**
             * Records the call and returns a one element list.
             * <p>
             * 호출을 기록하고 요소 하나의 목록을 반환한다.
             *
             * @param root      root / 루트
             * @param selectors selectors / 선택자
             * @return list with the paragraph / 문단이 든 목록
             */
            public List<Element> querySelectorAll(Node root, String selectors) {
                calls.add("qsa:" + selectors);
                List<Element> l = new ArrayList<Element>();
                l.add(p);
                return l;
            }

            /**
             * Records the call and returns true.
             * <p>
             * 호출을 기록하고 true 를 반환한다.
             *
             * @param element   element / 요소
             * @param selectors selectors / 선택자
             * @return true / true
             */
            public boolean matches(Element element, String selectors) {
                calls.add("m:" + selectors);
                return true;
            }

            /**
             * Records the call and returns the element itself.
             * <p>
             * 호출을 기록하고 요소 자신을 반환한다.
             *
             * @param element   element / 요소
             * @param selectors selectors / 선택자
             * @return the element / 요소
             */
            public Element closest(Element element, String selectors) {
                calls.add("c:" + selectors);
                return element;
            }
        });
        Check.same(p, body.querySelector("p"), "querySelector delegates");
        Check.equal(1, body.querySelectorAll("p").getLength(), "querySelectorAll delegates and returns a static list");
        Check.isTrue(p.matches("p"), "matches delegates");
        Check.same(p, p.closest("p"), "closest delegates");
        Check.same(p, doc.querySelector("p"), "the document delegates too");
        Check.equal(Arrays.asList("qs:p", "qsa:p", "m:p", "c:p", "qs:p"), calls, "calls reached the engine");
        doc.setMarkupSupport(new MarkupSupport() {
            /**
             * Returns a fixed string.
             * <p>
             * 고정된 문자열을 반환한다.
             *
             * @param node  node / 노드
             * @param outer outer flag / outer 플래그
             * @return markup / 마크업
             */
            public String serialize(Node node, boolean outer) {
                return outer ? "<outer/>" : "inner";
            }

            /**
             * Replaces the children with one text node.
             * <p>
             * 자식을 텍스트 노드 하나로 교체한다.
             *
             * @param context element / 요소
             * @param markup  markup / 마크업
             */
            public void setInnerHtml(Element context, String markup) {
                context.setTextContent("parsed:" + markup);
            }

            /**
             * Returns a fragment with one text node.
             * <p>
             * 텍스트 노드 하나의 조각을 반환한다.
             *
             * @param context element / 요소
             * @param markup  markup / 마크업
             * @return fragment / 조각
             */
            public DocumentFragment parseFragment(Element context, String markup) {
                DocumentFragment f = context.document().createDocumentFragment();
                f.appendChild(context.document().createTextNode("frag:" + markup));
                return f;
            }
        });
        Check.equal("inner", p.getInnerHtml(), "innerHTML delegates");
        Check.equal("<outer/>", p.getOuterHtml(), "outerHTML delegates");
        p.setInnerHtml("x");
        Check.equal("parsed:x", p.getTextContent(), "setInnerHtml delegates");
        p.setOuterHtml("y");
        Check.equal("frag:y", body.getTextContent(), "setOuterHtml replaces the element with the parsed fragment");
        Element q = doc.createElement("q");
        body.appendChild(q);
        q.insertAdjacentHtml("beforeend", "e");
        q.insertAdjacentHtml("AFTERBEGIN", "b");
        q.insertAdjacentHtml("afterend", "n");
        q.insertAdjacentHtml("beforebegin", "p");
        Check.equal("frag:yfrag:pfrag:bfrag:efrag:n", body.getTextContent(), "insertAdjacentHtml positions");
    }

    /**
     * P20-01: <code>insertAdjacentElement</code>, <code>insertAdjacentText</code> and <code>replaceChildren</code> (DOM Standard).
     * <p>
     * P20-01: <code>insertAdjacentElement</code>, <code>insertAdjacentText</code>, <code>replaceChildren</code> (DOM 표준).
     */
    public void testInsertAdjacentNodesAndReplaceChildren() {
        final Element body = body();
        final Element host = doc.createElement("div");
        body.appendChild(host);
        Element b = doc.createElement("b");
        Check.same(b, host.insertAdjacentElement("BEFOREEND", b), "the inserted element is returned");
        host.insertAdjacentText("afterbegin", "x");
        host.insertAdjacentText("beforebegin", "p");
        Element i = doc.createElement("i");
        Check.same(i, host.insertAdjacentElement("afterend", i), "afterend");
        Check.equal("p|div|i", names(body, "div"), "before and after the host");
        Check.equal("x", host.getFirstChild().getNodeValue(), "afterbegin puts the text first");
        Check.same(b, host.getLastChild(), "beforeend appends");
        final Element loose = doc.createElement("u");
        Check.isNull(loose.insertAdjacentElement("beforebegin", doc.createElement("s")), "no parent: null for beforebegin");
        loose.insertAdjacentText("afterend", "ignored");
        Check.equal(0, loose.getChildElementCount(), "nothing was inserted");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Uses an unknown position.
             * <p>
             * 알 수 없는 위치를 쓴다.
             */
            public void run() {
                host.insertAdjacentText("inside", "x");
            }
        }, "an unknown position is a SyntaxError");
        // replaceChildren 은 모든 자식을 교체하고 인수가 없으면 비운다.
        Element c1 = doc.createElement("em");
        host.replaceChildren(c1, "t", doc.createElement("strong"));
        Check.equal(3, host.getChildNodes().getLength(), "three new children");
        Check.same(c1, host.getFirstChild(), "the nodes keep their order");
        host.replaceChildren();
        Check.equal(0, host.getChildNodes().getLength(), "no arguments removes the children");
        Check.throwsException(DomException.class, new Block() {
            /**
             * Replaces the children of the host with its own parent (a cycle).
             * <p>
             * 호스트의 자식을 자신의 부모로 교체한다(순환).
             */
            public void run() {
                host.replaceChildren(body);
            }
        }, "a node cannot become the child of its own descendant");
    }

    /**
     * Returns the names of the child elements and text of an element up to and including the named child, as <code>a|b|c</code>: the
     * previous sibling text, the element name and the next sibling element name.
     * <p>
     * 이름 있는 자식을 중심으로 이전 형제 텍스트, 요소 이름, 다음 형제 요소 이름을 <code>a|b|c</code> 로 반환한다.
     *
     * @param parent the parent / 부모
     * @param name   the element name / 요소 이름
     * @return the text / 문자열
     */
    private static String names(Element parent, String name) {
        // 부모의 자식을 훑어 이름이 name 인 요소의 앞뒤를 찾는다.
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && name.equals(((Element) n).getLocalName())) {
                Node prev = n.getPreviousSibling();
                Node next = n.getNextSibling();
                return (prev == null ? "" : prev.getNodeValue()) + "|" + name + "|" + (next instanceof Element ? ((Element) next).getLocalName() : "");
            }
        }
        return "";
    }

    /**
     * An element factory creates the element objects of the document (the extension point of the HTML layer).
     * <p>
     * 요소 팩터리가 문서의 요소 객체를 만든다(HTML 계층의 확장 지점).
     */
    public void testElementFactoryAndAttributeHook() {
        final List<String> changes = new ArrayList<String>();
        doc.setElementFactory(new ElementFactory() {
            /**
             * Creates an element class that records attribute changes.
             * <p>
             * 속성 변경을 기록하는 요소 클래스를 만든다.
             *
             * @param document     owner document / 소유 문서
             * @param namespaceURI namespace / 네임스페이스
             * @param prefix       prefix / 접두사
             * @param localName    local name / 로컬 이름
             * @return the element / 요소
             */
            public Element createElement(Document document, String namespaceURI, String prefix, String localName) {
                return new Element(document, namespaceURI, prefix, localName) {
                    /**
                     * Records the change.
                     * <p>
                     * 변경을 기록한다.
                     *
                     * @param local     attribute name / 속성 이름
                     * @param namespace namespace / 네임스페이스
                     * @param oldValue  old value / 이전 값
                     * @param newValue  new value / 새 값
                     */
                    protected void attributeChanged(String local, String namespace, String oldValue, String newValue) {
                        changes.add(local + ":" + oldValue + ">" + newValue);
                    }
                };
            }
        });
        Element e = doc.createElement("a");
        e.setAttribute("href", "1");
        e.setAttribute("href", "2");
        e.removeAttribute("href");
        Check.equal(Arrays.asList("href:null>1", "href:1>2", "href:2>null"), changes, "the hook sees add, change and remove");
        Element clone = (Element) e.cloneNode(false);
        Check.isTrue(clone.getClass() == e.getClass(), "clones are created through the factory");
    }
}
