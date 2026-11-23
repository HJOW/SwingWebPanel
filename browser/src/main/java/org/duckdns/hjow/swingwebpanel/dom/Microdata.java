package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The microdata model of the W3C Working Group Note "HTML Microdata" (29 October 2013; the HTML5 2014 Recommendation has no microdata chapter
 * and the Note is the last published form of the feature): which elements are items, the item types, the property names, the properties of an
 * item (the "crawl" of the Note, section 5.2, with <code>itemref</code>), the top-level items of a document and the value of a property
 * element (<code>itemValue</code>). The functions only read the tree. Microdata attributes are defined for HTML elements only, so an element
 * of another namespace (SVG, MathML) is never an item and has no property names. Use: <code>Microdata.topLevelItems(document, types)</code>,
 * <code>Microdata.properties(itemElement)</code>.
 * <p>
 * W3C 작업 그룹 노트 "HTML Microdata"(2013-10-29; HTML5 2014 권고안에는 마이크로데이터 장이 없고 이 노트가 이 기능의 마지막 공개 형태이다)의 마이크로데이터
 * 모델: 어떤 요소가 아이템인지, 아이템 타입, 프로퍼티 이름, 아이템의 프로퍼티(노트 5.2절의 "crawl", <code>itemref</code> 포함), 문서의 최상위 아이템, 프로퍼티 요소의
 * 값(<code>itemValue</code>). 함수는 트리를 읽기만 한다. 마이크로데이터 속성은 HTML 요소에만 정의되므로 다른 네임스페이스(SVG, MathML)의 요소는 아이템이 아니고
 * 프로퍼티 이름도 없다. 사용: <code>Microdata.topLevelItems(document, types)</code>, <code>Microdata.properties(itemElement)</code>.
 */
public final class Microdata {

    /**
     * Not instantiable.
     * <p>
     * 인스턴스를 만들 수 없다.
     */
    private Microdata() {
    }

    /**
     * Tells whether an element is an HTML element.
     * <p>
     * 요소가 HTML 요소인지 알려 준다.
     *
     * @param e the element / 요소
     * @return true for an HTML element / HTML 요소이면 true
     */
    private static boolean isHtml(Element e) {
        return Namespaces.HTML.equals(e.getNamespaceURI());
    }

    /**
     * Tells whether an element creates an item: an HTML element with the <code>itemscope</code> attribute.
     * <p>
     * 요소가 아이템을 만드는지 알려 준다: <code>itemscope</code> 속성이 있는 HTML 요소.
     *
     * @param e the element / 요소
     * @return true if the element is an item / 아이템이면 true
     */
    public static boolean isItem(Element e) {
        return isHtml(e) && e.hasAttribute("itemscope");
    }

    /**
     * Splits an attribute value into its unique space separated tokens (case sensitive, in order of the first occurrence).
     * <p>
     * 속성 값을 공백으로 구분된 고유 토큰으로 나눈다(대소문자 구분, 처음 나타난 순서).
     *
     * @param value the value or null / 값 또는 null
     * @return the tokens / 토큰
     */
    public static List<String> tokens(String value) {
        List<String> out = new ArrayList<String>();
        if (value == null) {
            return out;
        }
        Set<String> seen = new HashSet<String>();
        // 공백(ASCII 공백 문자)으로 나눠 중복 없이 모은다.
        String[] parts = value.split("[ \t\n\f\r]+");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].length() > 0 && seen.add(parts[i])) {
                out.add(parts[i]);
            }
        }
        return out;
    }

    /**
     * Returns the property names of an element (the tokens of its <code>itemprop</code> attribute; empty for an element that is not HTML).
     * <p>
     * 요소의 프로퍼티 이름(<code>itemprop</code> 속성의 토큰)을 반환한다. HTML 요소가 아니면 비어 있다.
     *
     * @param e the element / 요소
     * @return the names / 이름
     */
    public static List<String> propertyNames(Element e) {
        return isHtml(e) ? tokens(e.getAttribute("itemprop")) : new ArrayList<String>();
    }

    /**
     * Returns the item types of an item (the tokens of its <code>itemtype</code> attribute).
     * <p>
     * 아이템의 아이템 타입(<code>itemtype</code> 속성의 토큰)을 반환한다.
     *
     * @param item the item / 아이템
     * @return the types / 타입
     */
    public static List<String> itemTypes(Element item) {
        return isItem(item) ? tokens(item.getAttribute("itemtype")) : new ArrayList<String>();
    }

    /**
     * Returns the top-level microdata items of a document in tree order: the items that have no <code>itemprop</code> attribute and, when
     * types are given, whose item types include all of them.
     * <p>
     * 문서의 최상위 마이크로데이터 아이템을 트리 순서로 반환한다: <code>itemprop</code> 속성이 없는 아이템이며, 타입이 주어지면 아이템 타입에 그 타입이 모두 들어
     * 있는 것.
     *
     * @param doc   the document / 문서
     * @param types the wanted types (may be empty) / 원하는 타입(비어 있을 수 있음)
     * @return the items / 아이템
     */
    public static List<Element> topLevelItems(Document doc, List<String> types) {
        List<Element> out = new ArrayList<Element>();
        Element root = doc.getDocumentElement();
        Node n = root;
        // 문서 전체를 트리 순서로 훑으며 조건에 맞는 아이템을 모은다.
        while (n != null) {
            if (n instanceof Element) {
                Element e = (Element) n;
                if (isItem(e) && !(isHtml(e) && e.hasAttribute("itemprop")) && itemTypes(e).containsAll(types)) {
                    out.add(e);
                }
            }
            n = Node.nextInTree(n, root);
        }
        return out;
    }

    /**
     * Returns the properties of an item in tree order ("the properties of an item", section 5.2 of the Note): the elements with
     * <code>itemprop</code> that are descendants of the item, or of an element named by its <code>itemref</code>, without descending into
     * nested items.
     * <p>
     * 아이템의 프로퍼티를 트리 순서로 반환한다(노트 5.2절 "the properties of an item"): 아이템의 후손이거나 <code>itemref</code> 가 가리키는 요소의 후손인
     * <code>itemprop</code> 요소이며 중첩된 아이템 안으로는 들어가지 않는다.
     *
     * @param item the item (an element that is not an item gives an empty list) / 아이템(아이템이 아닌 요소는 빈 목록)
     * @return the property elements / 프로퍼티 요소
     */
    public static List<Element> properties(Element item) {
        List<Element> results = new ArrayList<Element>();
        if (!isItem(item)) {
            return results;
        }
        List<Element> pending = new ArrayList<Element>();
        Set<Element> memory = new HashSet<Element>();
        memory.add(item);
        addChildren(pending, item);
        // itemref 의 id 마다 같은 트리에서 그 id 의 첫 요소를 대기열에 더한다.
        List<String> refs = tokens(item.getAttribute("itemref"));
        Node root = item;
        // 아이템이 속한 트리의 루트를 찾는다.
        while (root.getParentNode() != null) {
            root = root.getParentNode();
        }
        // itemref 의 id 를 하나씩 찾아 대기열에 더한다.
        for (int i = 0; i < refs.size(); i++) {
            Element ref = findById(root, refs.get(i));
            if (ref != null) {
                pending.add(ref);
            }
        }
        // 대기열이 빌 때까지 요소를 꺼내어 처리한다.
        while (!pending.isEmpty()) {
            Element current = pending.remove(0);
            // 이미 처리한 요소(순환 참조 등)는 건너뛴다.
            if (!memory.add(current)) {
                continue;
            }
            // 아이템이 아닌 요소는 자식을 대기열 앞에 이어 붙인다(트리 순서를 유지하기 위해 맨 뒤에 더하고 마지막에 정렬한다).
            if (!isItem(current)) {
                addChildren(pending, current);
            }
            if (!propertyNames(current).isEmpty()) {
                results.add(current);
            }
        }
        sortInTreeOrder(results, root);
        return results;
    }

    /**
     * Adds the child elements of an element to a list in tree order.
     * <p>
     * 요소의 자식 요소를 트리 순서로 목록에 더한다.
     *
     * @param list the list / 목록
     * @param e    the element / 요소
     */
    private static void addChildren(List<Element> list, Element e) {
        // 자식 노드 중 요소만 차례로 더한다.
        for (Node c = e.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c instanceof Element) {
                list.add((Element) c);
            }
        }
    }

    /**
     * Finds the first element with an id in the tree below a root.
     * <p>
     * 루트 아래 트리에서 id 가 같은 첫 요소를 찾는다.
     *
     * @param root the root / 루트
     * @param id   the id / id
     * @return the element or null / 요소 또는 null
     */
    private static Element findById(Node root, String id) {
        Node n = Node.nextInTree(root, root);
        // 트리 순서로 훑으며 id 가 같은 첫 요소를 찾는다.
        while (n != null) {
            if (n instanceof Element && id.equals(((Element) n).getId())) {
                return (Element) n;
            }
            n = Node.nextInTree(n, root);
        }
        return null;
    }

    /**
     * Sorts elements of one tree in tree order.
     * <p>
     * 한 트리의 요소를 트리 순서로 정렬한다.
     *
     * @param list the elements / 요소
     * @param root the root of the tree / 트리의 루트
     */
    private static void sortInTreeOrder(List<Element> list, Node root) {
        if (list.size() < 2) {
            return;
        }
        Set<Element> wanted = new HashSet<Element>(list);
        List<Element> ordered = new ArrayList<Element>();
        Node n = root;
        // 트리를 한 번 훑으며 고른 요소를 만나는 순서대로 모은다.
        while (n != null) {
            if (n instanceof Element && wanted.contains(n)) {
                ordered.add((Element) n);
            }
            n = Node.nextInTree(n, root);
        }
        list.clear();
        list.addAll(ordered);
    }

    /**
     * Returns the value of a property element, as the <code>itemValue</code> attribute reads it: <code>null</code> for an element without
     * <code>itemprop</code>, the element itself for an item, the URL or text of the content attribute that belongs to the element type, and the
     * text content otherwise.
     * <p>
     * 프로퍼티 요소의 값을 <code>itemValue</code> 속성이 읽는 대로 반환한다: <code>itemprop</code> 가 없는 요소는 <code>null</code>, 아이템이면 요소 자신, 요소 종류에
     * 맞는 콘텐츠 속성의 URL 이나 텍스트, 그 밖에는 텍스트 콘텐츠.
     *
     * @param e the element / 요소
     * @return null, the element (an Element) or a String / null, 요소(Element) 또는 문자열
     */
    public static Object itemValue(Element e) {
        if (!isHtml(e) || !e.hasAttribute("itemprop")) {
            return null;
        }
        if (e.hasAttribute("itemscope")) {
            return e;
        }
        String tag = e.getLocalName();
        if ("meta".equals(tag)) {
            return text(e.getAttribute("content"));
        } else if (isOneOf(tag, "audio", "embed", "iframe", "img", "source", "track", "video")) {
            return url(e, "src");
        } else if (isOneOf(tag, "a", "area", "link")) {
            return url(e, "href");
        } else if ("object".equals(tag)) {
            return url(e, "data");
        } else if ("data".equals(tag) || "meter".equals(tag)) {
            return text(e.getAttribute("value"));
        } else if ("time".equals(tag)) {
            return e.hasAttribute("datetime") ? e.getAttribute("datetime") : e.getTextContent();
        }
        return e.getTextContent();
    }

    /**
     * Sets the value of a property element (the setter of <code>itemValue</code>): the content attribute that belongs to the element type, or
     * the text content.
     * <p>
     * 프로퍼티 요소의 값을 지정한다(<code>itemValue</code> 의 setter): 요소 종류에 맞는 콘텐츠 속성이나 텍스트 콘텐츠.
     *
     * @param e     the element / 요소
     * @param value the new value / 새 값
     * @throws DomException InvalidAccessError for an element without <code>itemprop</code> and for an item / <code>itemprop</code> 가 없는 요소와 아이템은 InvalidAccessError
     */
    public static void setItemValue(Element e, String value) throws DomException {
        if (!isHtml(e) || !e.hasAttribute("itemprop") || e.hasAttribute("itemscope")) {
            throw new DomException("InvalidAccessError", (short) 15, "the value of this element cannot be set");
        }
        String tag = e.getLocalName();
        if ("meta".equals(tag)) {
            e.setAttribute("content", value);
        } else if (isOneOf(tag, "audio", "embed", "iframe", "img", "source", "track", "video")) {
            e.setAttribute("src", value);
        } else if (isOneOf(tag, "a", "area", "link")) {
            e.setAttribute("href", value);
        } else if ("object".equals(tag)) {
            e.setAttribute("data", value);
        } else if ("data".equals(tag) || "meter".equals(tag)) {
            e.setAttribute("value", value);
        } else if ("time".equals(tag)) {
            e.setAttribute("datetime", value);
        } else {
            e.setTextContent(value);
        }
    }

    /**
     * Returns the text of an attribute, empty if it is missing.
     * <p>
     * 속성의 텍스트를 반환한다. 없으면 빈 문자열.
     *
     * @param v the attribute value or null / 속성 값 또는 null
     * @return the text / 텍스트
     */
    private static String text(String v) {
        return v == null ? "" : v;
    }

    /**
     * Returns an attribute that holds a URL as an absolute URL against the base URL of the document (empty if missing, the value itself if
     * it cannot be resolved), like a reflecting attribute of the type URL.
     * <p>
     * URL 을 담은 속성을 문서의 기준 URL 로 절대 URL 로 만들어 반환한다(없으면 빈 문자열, 해석할 수 없으면 값 그대로). URL 형 반영 속성과 같다.
     *
     * @param e    the element / 요소
     * @param name the attribute name / 속성 이름
     * @return the URL / URL
     */
    private static String url(Element e, String name) {
        String v = e.getAttribute(name);
        if (v == null) {
            return "";
        }
        try {
            return UrlUtil.resolve(e.getOwnerDocument().getBaseUri(), v);
        } catch (RuntimeException ex) {
            return v;
        }
    }

    /**
     * Tells whether a string is one of the others.
     * <p>
     * 문자열이 다른 문자열들 중 하나인지 알려 준다.
     *
     * @param s      the string / 문자열
     * @param others the candidates / 후보
     * @return true if found / 있으면 true
     */
    private static boolean isOneOf(String s, String... others) {
        // 후보를 하나씩 비교한다.
        for (int i = 0; i < others.length; i++) {
            if (others[i].equals(s)) {
                return true;
            }
        }
        return false;
    }
}
