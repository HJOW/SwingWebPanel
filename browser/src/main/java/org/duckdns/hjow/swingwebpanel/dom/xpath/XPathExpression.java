package org.duckdns.hjow.swingwebpanel.dom.xpath;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.dom.Attr;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.NamedAttributeMap;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * A compiled XPath 1.0 expression (XML Path Language 1.0, W3C Recommendation 16 November 1999, Last-Modified 2 October 2017, fetched 2026-11-18) over the DOM of this
 * project, as the DOM XPath interfaces use it (WHATWG DOM section 8). It has the whole syntax of the Recommendation: location paths with all the axes (the
 * <code>namespace</code> axis is always empty: the DOM has no namespace nodes), node tests and predicates, the abbreviated syntax, filter expressions, unions, all the
 * operators with the comparison rules of node sets, and the core function library (node-set, string, boolean and number functions). Values are {@link Double},
 * {@link String}, {@link Boolean} and a node set, which is a {@link List} of {@link Node} and {@link Attr} objects without duplicates in document order. Variable
 * references are a {@link XPathException#TYPE} error (a DOM XPath expression has no variables). Choices for HTML documents (as browsers do): a name test without a
 * prefix also matches elements in the HTML namespace, and it matches their lower case names, so that <code>//DIV</code> finds <code>div</code> elements. An expression
 * is immutable and may be evaluated many times, also from different threads.
 * <p>
 * 컴파일된 XPath 1.0 식(XML Path Language 1.0, W3C 권고안 1999-11-16, Last-Modified 2017-10-02, 2026-11-18 조회)이며 DOM XPath 인터페이스가 쓰는 이 프로젝트의 DOM 위에서
 * 동작한다(WHATWG DOM 8절). 권고안의 구문 전체를 갖춘다: 모든 축이 있는 위치 경로(<code>namespace</code> 축은 DOM 에 네임스페이스 노드가 없으므로 항상 비어 있다), 노드 테스트와
 * 술어, 약식 구문, 필터 식, 합집합, 노드 집합의 비교 규칙이 있는 모든 연산자, 핵심 함수 라이브러리(노드 집합, 문자열, 불리언, 숫자 함수). 값은 {@link Double}, {@link String},
 * {@link Boolean}, 그리고 노드 집합이며 노드 집합은 중복 없이 문서 순서로 놓인 {@link Node} 와 {@link Attr} 의 {@link List} 이다. 변수 참조는 {@link XPathException#TYPE} 오류이다
 * (DOM XPath 식에는 변수가 없다). HTML 문서에서의 선택(브라우저와 같다): 접두사 없는 이름 테스트는 HTML 네임스페이스의 요소도 일치시키며 소문자 이름으로 비교하므로
 * <code>//DIV</code> 가 <code>div</code> 요소를 찾는다. 식은 불변이고 여러 번, 여러 스레드에서 평가할 수 있다.
 */
public final class XPathExpression {

    /**
     * The root of the syntax tree.
     * <p>
     * 구문 트리의 루트.
     */
    private final Expr root;

    /**
     * The text that was compiled (for messages).
     * <p>
     * 컴파일한 문자열(메시지용).
     */
    private final String text;

    /**
     * Creates an expression from its syntax tree.
     * <p>
     * 구문 트리로 식을 만든다.
     *
     * @param root the tree / 트리
     * @param text the text / 문자열
     */
    private XPathExpression(Expr root, String text) {
        this.root = root;
        this.text = text;
    }

    /**
     * Compiles an XPath 1.0 expression.
     * <p>
     * XPath 1.0 식을 컴파일한다.
     *
     * @param text the expression / 식
     * @return the compiled expression / 컴파일된 식
     * @throws XPathException of kind {@link XPathException#SYNTAX} if the text is not an expression / 식이 아니면 {@link XPathException#SYNTAX} 종류
     */
    public static XPathExpression compile(String text) {
        try {
            Parser p = new Parser(text);
            Expr e = p.parseExpression();
            p.expectEnd();
            return new XPathExpression(e, text);
        } catch (XPathException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new XPathException(XPathException.SYNTAX, "The string '" + text + "' is not a valid XPath expression.");
        }
    }

    /**
     * Evaluates the expression.
     * <p>
     * 식을 평가한다.
     *
     * @param context  the context node (a node or an attribute) / 컨텍스트 노드(노드나 속성)
     * @param resolver resolves the prefixes of the name tests, or null for none / 이름 테스트의 접두사를 해석한다. 없으면 null
     * @return a {@link Double}, a {@link String}, a {@link Boolean} or a node set (a {@link List} in document order) / {@link Double}, {@link String}, {@link Boolean} 또는 노드 집합(문서 순서의 {@link List})
     * @throws XPathException of kind {@link XPathException#NAMESPACE} or {@link XPathException#TYPE} / {@link XPathException#NAMESPACE} 또는 {@link XPathException#TYPE} 종류
     */
    public Object evaluate(Object context, NamespaceResolver resolver) {
        Ctx c = new Ctx();
        c.node = context;
        c.position = 1;
        c.size = 1;
        c.resolver = resolver;
        Document doc = documentOf(context);
        c.html = doc != null && doc.isHtmlDocument();
        return root.eval(c);
    }

    /**
     * Returns the text that was compiled.
     * <p>
     * 컴파일한 문자열을 반환한다.
     *
     * @return the text / 문자열
     */
    public String getText() {
        return text;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // 평가 문맥과 값의 변환
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * The evaluation context: the context node with its position and size, the namespace resolver and whether the document is an HTML document.
     * <p>
     * 평가 문맥: 위치와 크기가 있는 컨텍스트 노드, 네임스페이스 리졸버, 문서가 HTML 문서인지 여부.
     */
    private static final class Ctx {
        /** The context node. / 컨텍스트 노드. */
        Object node;
        /** The context position (from 1). / 컨텍스트 위치(1부터). */
        int position;
        /** The context size. / 컨텍스트 크기. */
        int size;
        /** The resolver of prefixes. / 접두사 리졸버. */
        NamespaceResolver resolver;
        /** True for an HTML document. / HTML 문서이면 true. */
        boolean html;
        /** The document order numbers by tree root, made when first needed. / 트리 루트별 문서 순서 번호. 처음 필요할 때 만든다. */
        Map<Object, Map<Object, Integer>> numbering = new IdentityHashMap<Object, Map<Object, Integer>>();

        /**
         * Copies the context with another node, position and size.
         * <p>
         * 다른 노드, 위치, 크기로 문맥을 복사한다.
         *
         * @param n the node / 노드
         * @param p the position / 위치
         * @param s the size / 크기
         * @return the copy / 복사본
         */
        Ctx with(Object n, int p, int s) {
            Ctx c = new Ctx();
            c.node = n;
            c.position = p;
            c.size = s;
            c.resolver = resolver;
            c.html = html;
            c.numbering = numbering;
            return c;
        }
    }

    /**
     * Returns the document of a node or an attribute.
     * <p>
     * 노드나 속성의 문서를 반환한다.
     *
     * @param n the node / 노드
     * @return the document, or null / 문서. 없으면 null
     */
    private static Document documentOf(Object n) {
        if (n instanceof Document) {
            return (Document) n;
        }
        return n instanceof Attr ? ((Attr) n).getOwnerDocument() : n instanceof Node ? ((Node) n).getOwnerDocument() : null;
    }

    /**
     * Returns the node type of a node or an attribute.
     * <p>
     * 노드나 속성의 노드 종류를 반환한다.
     *
     * @param n the node / 노드
     * @return the node type / 노드 종류
     */
    private static short typeOf(Object n) {
        return n instanceof Attr ? Node.ATTRIBUTE_NODE : ((Node) n).getNodeType();
    }

    /**
     * Returns the parent of a node as XPath sees it (the owner element for an attribute).
     * <p>
     * XPath 가 보는 노드의 부모를 반환한다(속성은 소유 요소).
     *
     * @param n the node / 노드
     * @return the parent or null / 부모. 없으면 null
     */
    private static Object parentOf(Object n) {
        return n instanceof Attr ? ((Attr) n).getOwnerElement() : ((Node) n).getParentNode();
    }

    /**
     * Returns the string-value of a node (XPath 5): the text of an element or a document, the value of an attribute, the data of the other nodes.
     * <p>
     * 노드의 문자열 값을 반환한다(XPath 5): 요소와 문서는 텍스트, 속성은 값, 그 밖의 노드는 데이터.
     *
     * @param n the node / 노드
     * @return the string-value / 문자열 값
     */
    private static String stringValue(Object n) {
        if (n instanceof Attr) {
            String v = ((Attr) n).getValue();
            return v == null ? "" : v;
        }
        Node node = (Node) n;
        short t = node.getNodeType();
        if (t == Node.DOCUMENT_NODE) {
            Element root = ((Document) node).getDocumentElement();
            return root == null ? "" : stringValue(root);
        }
        String v = t == Node.ELEMENT_NODE || t == Node.DOCUMENT_FRAGMENT_NODE ? node.getTextContent() : node.getNodeValue();
        return v == null ? "" : v;
    }

    /**
     * Converts a value to a string (the function <code>string()</code>).
     * <p>
     * 값을 문자열로 바꾼다(<code>string()</code> 함수).
     *
     * @param v the value / 값
     * @return the string / 문자열
     */
    public static String toStr(Object v) {
        if (v instanceof String) {
            return (String) v;
        } else if (v instanceof Double) {
            return numberToString(((Double) v).doubleValue());
        } else if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue() ? "true" : "false";
        }
        List<?> set = (List<?>) v;
        return set.isEmpty() ? "" : stringValue(set.get(0));
    }

    /**
     * Converts a number to a string the way XPath 4.2 says: <code>NaN</code>, <code>Infinity</code>, no exponent, no trailing zeros, an integer without a point.
     * <p>
     * XPath 4.2 가 정한 대로 숫자를 문자열로 바꾼다: <code>NaN</code>, <code>Infinity</code>, 지수 없음, 끝의 0 없음, 정수는 소수점 없음.
     *
     * @param d the number / 숫자
     * @return the string / 문자열
     */
    static String numberToString(double d) {
        if (Double.isNaN(d)) {
            return "NaN";
        } else if (Double.isInfinite(d)) {
            return d > 0 ? "Infinity" : "-Infinity";
        } else if (d == 0) {
            return "0";
        }
        return new BigDecimal(Double.toString(d)).stripTrailingZeros().toPlainString();
    }

    /**
     * Converts a value to a number (the function <code>number()</code>).
     * <p>
     * 값을 숫자로 바꾼다(<code>number()</code> 함수).
     *
     * @param v the value / 값
     * @return the number / 숫자
     */
    public static double toNum(Object v) {
        if (v instanceof Double) {
            return ((Double) v).doubleValue();
        } else if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue() ? 1 : 0;
        }
        return parseNumber(toStr(v));
    }

    /**
     * Parses the lexical form of an XPath number: optional white space, an optional minus, digits with an optional fraction, optional white space; else NaN.
     * <p>
     * XPath 숫자의 어휘 형식을 해석한다: 선택적 공백, 선택적 빼기, 선택적 소수부가 있는 숫자, 선택적 공백. 아니면 NaN.
     *
     * @param s the string / 문자열
     * @return the number or NaN / 숫자. 아니면 NaN
     */
    private static double parseNumber(String s) {
        String t = s.trim();
        int i = 0;
        int n = t.length();
        if (i < n && t.charAt(i) == '-') {
            i++;
        }
        int digits = 0;
        // 정수부의 숫자를 센다.
        while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
            i++;
            digits++;
        }
        if (i < n && t.charAt(i) == '.') {
            i++;
            // 소수부의 숫자를 센다.
            while (i < n && t.charAt(i) >= '0' && t.charAt(i) <= '9') {
                i++;
                digits++;
            }
        }
        if (i != n || digits == 0) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /**
     * Converts a value to a boolean (the function <code>boolean()</code>).
     * <p>
     * 값을 불리언으로 바꾼다(<code>boolean()</code> 함수).
     *
     * @param v the value / 값
     * @return the boolean / 불리언
     */
    public static boolean toBool(Object v) {
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        } else if (v instanceof Double) {
            double d = ((Double) v).doubleValue();
            return d != 0 && !Double.isNaN(d);
        } else if (v instanceof String) {
            return ((String) v).length() > 0;
        }
        return !((List<?>) v).isEmpty();
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // 문서 순서
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the root of the tree of a node (the owner element's tree for an attribute).
     * <p>
     * 노드의 트리의 루트를 반환한다(속성은 소유 요소의 트리).
     *
     * @param n the node / 노드
     * @return the root / 루트
     */
    private static Object rootOf(Object n) {
        Object cur = n;
        // 부모가 없을 때까지 올라간다.
        while (parentOf(cur) != null) {
            cur = parentOf(cur);
        }
        return cur;
    }

    /**
     * Numbers the nodes of a tree in document order (an element, then its attributes, then its children).
     * <p>
     * 트리의 노드에 문서 순서로 번호를 매긴다(요소, 그 속성, 그 자식 순서).
     *
     * @param n   the node to number / 번호를 매길 노드
     * @param map the numbers / 번호
     */
    private static void number(Node n, Map<Object, Integer> map) {
        map.put(n, Integer.valueOf(map.size()));
        if (n instanceof Element) {
            NamedAttributeMap attrs = ((Element) n).getAttributes();
            // 속성은 요소 바로 뒤에 온다.
            for (int i = 0; i < attrs.getLength(); i++) {
                map.put(attrs.item(i), Integer.valueOf(map.size()));
            }
        }
        // 자식을 차례로 번호 매긴다.
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            number(c, map);
        }
    }

    /**
     * Returns the ordinal of a tree among the trees that this evaluation met (the order of different trees is stable, not meaningful).
     * <p>
     * 이 평가에서 만난 트리 중 트리의 순번을 반환한다(서로 다른 트리의 순서는 안정적일 뿐 의미는 없다).
     *
     * @param c    the context / 문맥
     * @param root the root of the tree / 트리의 루트
     * @return the ordinal / 순번
     */
    private static int treeOrdinal(Ctx c, Object root) {
        int index = 0;
        // 번호를 매긴 트리를 만난 순서대로 세어 같은 루트의 순번을 찾는다.
        for (Object r : c.numbering.keySet()) {
            if (r == root) {
                return index;
            }
            index++;
        }
        return index;
    }

    /**
     * Sorts a node set into document order and removes the duplicates.
     * <p>
     * 노드 집합을 문서 순서로 정렬하고 중복을 없앤다.
     *
     * @param c    the context (it keeps the numbering) / 문맥(번호를 가지고 있다)
     * @param list the nodes / 노드
     * @return the sorted list without duplicates / 중복 없이 정렬된 목록
     */
    private static List<Object> sorted(Ctx c, List<Object> list) {
        if (list.size() < 2) {
            return list;
        }
        final Map<Object, Long> numbers = new IdentityHashMap<Object, Long>();
        // 노드가 속한 트리마다 번호를 한 번만 만든다.
        for (Object o : list) {
            Object root = rootOf(o);
            Map<Object, Integer> m = c.numbering.get(root);
            if (m == null) {
                m = new IdentityHashMap<Object, Integer>();
                number((Node) root, m);
                c.numbering.put(root, m);
            }
            Integer k = m.get(o);
            // 트리의 순번을 큰 자리에 두어 서로 다른 트리의 노드도 일관되게 정렬한다.
            numbers.put(o, Long.valueOf(treeOrdinal(c, root) * 1000000000L + (k == null ? 0 : k.intValue())));
        }
        List<Object> copy = new ArrayList<Object>(list);
        Collections.sort(copy, new Comparator<Object>() {
            /**
             * Orders by the document order number.
             * <p>
             * 문서 순서 번호로 정렬한다.
             *
             * @param a one node / 노드 하나
             * @param b the other node / 다른 노드
             * @return the order / 순서
             */
            public int compare(Object a, Object b) {
                return numbers.get(a).compareTo(numbers.get(b));
            }
        });
        List<Object> out = new ArrayList<Object>();
        // 같은 노드가 이웃해 있으므로 앞의 것과 같으면 건너뛴다.
        for (Object o : copy) {
            if (out.isEmpty() || out.get(out.size() - 1) != o) {
                out.add(o);
            }
        }
        return out;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // 축
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /** The names of the axes of XPath 1.0. / XPath 1.0 의 축 이름 목록. */
    private static final String[] AXES = { "ancestor", "ancestor-or-self", "attribute", "child", "descendant", "descendant-or-self", "following", "following-sibling",
        "namespace", "parent", "preceding", "preceding-sibling", "self" };

    /**
     * Tells whether an axis runs backwards in the document (its proximity positions count from the context node outwards).
     * <p>
     * 축이 문서에서 거꾸로 가는지 알려 준다(근접 위치는 컨텍스트 노드에서 바깥쪽으로 센다).
     *
     * @param axis the axis name / 축 이름
     * @return true for a reverse axis / 역방향 축이면 true
     */
    private static boolean reverse(String axis) {
        return axis.equals("ancestor") || axis.equals("ancestor-or-self") || axis.equals("preceding") || axis.equals("preceding-sibling");
    }

    /**
     * Adds a node and all its descendants in document order.
     * <p>
     * 노드와 모든 자손을 문서 순서로 더한다.
     *
     * @param n   the node / 노드
     * @param out the list / 목록
     */
    private static void addTree(Node n, List<Object> out) {
        out.add(n);
        // 자식마다 재귀로 더한다.
        for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            addTree(c, out);
        }
    }

    /**
     * Returns the nodes of an axis from a node, in the order of the axis (reverse axes from the nearest outwards).
     * <p>
     * 노드에서 축의 노드를 축의 순서로 반환한다(역방향 축은 가까운 쪽부터 바깥으로).
     *
     * @param axis the axis name / 축 이름
     * @param from the context node / 컨텍스트 노드
     * @return the nodes / 노드
     */
    private static List<Object> axis(String axis, Object from) {
        List<Object> out = new ArrayList<Object>();
        boolean isAttr = from instanceof Attr;
        Node node = isAttr ? null : (Node) from;
        if (axis.equals("self")) {
            out.add(from);
        } else if (axis.equals("child")) {
            // 속성은 자식이 없다.
            for (Node c = isAttr ? null : node.getFirstChild(); c != null; c = c.getNextSibling()) {
                out.add(c);
            }
        } else if (axis.equals("descendant") || axis.equals("descendant-or-self")) {
            if (axis.equals("descendant-or-self")) {
                out.add(from);
            }
            // 자식 하위 트리를 문서 순서로 더한다.
            for (Node c = isAttr ? null : node.getFirstChild(); c != null; c = c.getNextSibling()) {
                addTree(c, out);
            }
        } else if (axis.equals("parent")) {
            Object p = parentOf(from);
            if (p != null) {
                out.add(p);
            }
        } else if (axis.equals("ancestor") || axis.equals("ancestor-or-self")) {
            if (axis.equals("ancestor-or-self")) {
                out.add(from);
            }
            // 가까운 조상부터 루트까지 올라간다.
            for (Object p = parentOf(from); p != null; p = parentOf(p)) {
                out.add(p);
            }
        } else if (axis.equals("attribute")) {
            if (node instanceof Element) {
                NamedAttributeMap attrs = ((Element) node).getAttributes();
                // 속성을 순서대로 더한다.
                for (int i = 0; i < attrs.getLength(); i++) {
                    out.add(attrs.item(i));
                }
            }
        } else if (axis.equals("following-sibling")) {
            // 속성에는 형제가 없다.
            for (Node s = isAttr ? null : node.getNextSibling(); s != null; s = s.getNextSibling()) {
                out.add(s);
            }
        } else if (axis.equals("preceding-sibling")) {
            // 가까운 앞 형제부터 거슬러 간다.
            for (Node s = isAttr ? null : node.getPreviousSibling(); s != null; s = s.getPreviousSibling()) {
                out.add(s);
            }
        } else if (axis.equals("following")) {
            Object cur = from;
            // 속성이면 소유 요소의 자손이 뒤따르는 노드에 든다.
            if (isAttr) {
                cur = ((Attr) from).getOwnerElement();
                if (cur != null) {
                    // 소유 요소의 자식 하위 트리를 문서 순서로 더한다.
                    for (Node c = ((Node) cur).getFirstChild(); c != null; c = c.getNextSibling()) {
                        addTree(c, out);
                    }
                }
            }
            // 자신과 조상의 뒤 형제와 그 하위 트리를 문서 순서로 모은다.
            for (Object a = cur; a != null; a = parentOf(a)) {
                for (Node s = ((Node) a).getNextSibling(); s != null; s = s.getNextSibling()) {
                    addTree(s, out);
                }
            }
        } else if (axis.equals("preceding")) {
            Object cur = isAttr ? ((Attr) from).getOwnerElement() : from;
            // 자신과 조상의 앞 형제와 그 하위 트리를 가까운 쪽부터(문서 역순으로) 모은다.
            for (Object a = cur; a != null; a = parentOf(a)) {
                for (Node s = ((Node) a).getPreviousSibling(); s != null; s = s.getPreviousSibling()) {
                    List<Object> tree = new ArrayList<Object>();
                    addTree(s, tree);
                    Collections.reverse(tree);
                    out.addAll(tree);
                }
            }
        }
        // namespace 축은 DOM 에 네임스페이스 노드가 없으므로 비어 있다.
        return out;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // 식
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * A node of the syntax tree.
     * <p>
     * 구문 트리의 노드.
     */
    private abstract static class Expr {
        /**
         * Evaluates the node.
         * <p>
         * 노드를 평가한다.
         *
         * @param c the context / 문맥
         * @return the value / 값
         */
        abstract Object eval(Ctx c);
    }

    /** A number or a string literal. / 숫자나 문자열 리터럴. */
    private static final class Literal extends Expr {
        /** The value of the literal: a Double or a String. / 리터럴의 값(Double 또는 String). */
        final Object value;

        /**
         * Creates the literal.
         * <p>
         * 리터럴을 만든다.
         *
         * @param value the value / 값
         */
        Literal(Object value) {
            this.value = value;
        }

        /**
         * Returns the value.
         * <p>
         * 값을 반환한다.
         *
         * @param c the context / 문맥
         * @return the value / 값
         */
        Object eval(Ctx c) {
            return value;
        }
    }

    /** A unary minus. / 단항 빼기. */
    private static final class Negate extends Expr {
        /** The operand. / 피연산자. */
        final Expr operand;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param operand the operand / 피연산자
         */
        Negate(Expr operand) {
            this.operand = operand;
        }

        /**
         * Returns the negated number.
         * <p>
         * 부호를 바꾼 숫자를 반환한다.
         *
         * @param c the context / 문맥
         * @return the number / 숫자
         */
        Object eval(Ctx c) {
            return Double.valueOf(-toNum(operand.eval(c)));
        }
    }

    /** A binary operator. / 이항 연산자. */
    private static final class Binary extends Expr {
        /** The operator: or, and, =, !=, <, <=, >, >=, +, -, *, div, mod. / 연산자. */
        final String op;
        /** The left operand. / 왼쪽 피연산자. */
        final Expr left;
        /** The right operand. / 오른쪽 피연산자. */
        final Expr right;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param op    the operator / 연산자
         * @param left  the left operand / 왼쪽 피연산자
         * @param right the right operand / 오른쪽 피연산자
         */
        Binary(String op, Expr left, Expr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        /**
         * Evaluates the operator (<code>or</code> and <code>and</code> evaluate the right side only if they have to).
         * <p>
         * 연산자를 평가한다(<code>or</code> 와 <code>and</code> 는 필요할 때만 오른쪽을 평가한다).
         *
         * @param c the context / 문맥
         * @return the value / 값
         */
        Object eval(Ctx c) {
            if (op.equals("or")) {
                return Boolean.valueOf(toBool(left.eval(c)) || toBool(right.eval(c)));
            } else if (op.equals("and")) {
                return Boolean.valueOf(toBool(left.eval(c)) && toBool(right.eval(c)));
            }
            Object a = left.eval(c);
            Object b = right.eval(c);
            if (op.equals("=") || op.equals("!=") || op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=")) {
                return Boolean.valueOf(compare(op, a, b));
            }
            double x = toNum(a);
            double y = toNum(b);
            if (op.equals("+")) {
                return Double.valueOf(x + y);
            } else if (op.equals("-")) {
                return Double.valueOf(x - y);
            } else if (op.equals("*")) {
                return Double.valueOf(x * y);
            } else if (op.equals("div")) {
                return Double.valueOf(x / y);
            }
            // mod 는 피제수의 부호를 따르는 나머지이다(Java 의 % 와 같다).
            return Double.valueOf(x % y);
        }
    }

    /**
     * Compares two values the way XPath 3.4 says: node sets compare by their nodes, then booleans, numbers and strings.
     * <p>
     * XPath 3.4 가 정한 대로 두 값을 비교한다: 노드 집합은 노드로, 그 다음 불리언, 숫자, 문자열.
     *
     * @param op the operator / 연산자
     * @param a  the left value / 왼쪽 값
     * @param b  the right value / 오른쪽 값
     * @return the result / 결과
     */
    private static boolean compare(String op, Object a, Object b) {
        boolean sa = a instanceof List;
        boolean sb = b instanceof List;
        if (sa && sb) {
            // 두 노드 집합은 짝을 이루는 노드 쌍이 하나라도 문자열 값으로 만족하면 참이다.
            for (Object x : (List<?>) a) {
                for (Object y : (List<?>) b) {
                    if (compareAtoms(op, stringValue(x), stringValue(y))) {
                        return true;
                    }
                }
            }
            return false;
        } else if (sa || sb) {
            Object set = sa ? a : b;
            Object other = sa ? b : a;
            if (other instanceof Boolean) {
                boolean s = !((List<?>) set).isEmpty();
                return sa ? compareAtoms(op, Boolean.valueOf(s), other) : compareAtoms(op, other, Boolean.valueOf(s));
            }
            // 노드 집합과 숫자나 문자열은 노드마다 문자열 값을 그 형식으로 바꿔 비교한다.
            for (Object x : (List<?>) set) {
                Object v = other instanceof Double ? (Object) Double.valueOf(parseNumber(stringValue(x))) : stringValue(x);
                if (sa ? compareAtoms(op, v, other) : compareAtoms(op, other, v)) {
                    return true;
                }
            }
            return false;
        }
        return compareAtoms(op, a, b);
    }

    /**
     * Compares two values that are not node sets.
     * <p>
     * 노드 집합이 아닌 두 값을 비교한다.
     *
     * @param op the operator / 연산자
     * @param a  the left value / 왼쪽 값
     * @param b  the right value / 오른쪽 값
     * @return the result / 결과
     */
    private static boolean compareAtoms(String op, Object a, Object b) {
        if (op.equals("=") || op.equals("!=")) {
            boolean eq;
            if (a instanceof Boolean || b instanceof Boolean) {
                eq = toBool(a) == toBool(b);
            } else if (a instanceof Double || b instanceof Double) {
                eq = toNum(a) == toNum(b);
            } else {
                eq = toStr(a).equals(toStr(b));
            }
            return op.equals("=") ? eq : !eq;
        }
        double x = toNum(a);
        double y = toNum(b);
        if (op.equals("<")) {
            return x < y;
        } else if (op.equals("<=")) {
            return x <= y;
        } else if (op.equals(">")) {
            return x > y;
        }
        return x >= y;
    }

    /** A union of node sets. / 노드 집합의 합집합. */
    private static final class Union extends Expr {
        /** The operands. / 피연산자. */
        final List<Expr> parts;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param parts the operands / 피연산자
         */
        Union(List<Expr> parts) {
            this.parts = parts;
        }

        /**
         * Returns the union in document order.
         * <p>
         * 문서 순서의 합집합을 반환한다.
         *
         * @param c the context / 문맥
         * @return the node set / 노드 집합
         */
        Object eval(Ctx c) {
            List<Object> all = new ArrayList<Object>();
            // 피연산자는 모두 노드 집합이어야 한다.
            for (Expr e : parts) {
                Object v = e.eval(c);
                if (!(v instanceof List)) {
                    throw new XPathException(XPathException.TYPE, "The operands of '|' must be node sets.");
                }
                all.addAll((List<?>) v);
            }
            return sorted(c, all);
        }
    }

    /** A primary expression with predicates (a filter expression). / 술어가 붙은 기본 식(필터 식). */
    private static final class Filter extends Expr {
        /** The primary expression. / 기본 식. */
        final Expr primary;
        /** The predicates. / 술어. */
        final List<Expr> predicates;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param primary    the primary expression / 기본 식
         * @param predicates the predicates / 술어
         */
        Filter(Expr primary, List<Expr> predicates) {
            this.primary = primary;
            this.predicates = predicates;
        }

        /**
         * Filters the node set of the primary expression (positions count in document order).
         * <p>
         * 기본 식의 노드 집합을 거른다(위치는 문서 순서로 센다).
         *
         * @param c the context / 문맥
         * @return the node set / 노드 집합
         */
        Object eval(Ctx c) {
            Object v = primary.eval(c);
            if (!(v instanceof List)) {
                throw new XPathException(XPathException.TYPE, "A predicate can only filter a node set.");
            }
            List<Object> set = sorted(c, new ArrayList<Object>((List<?>) v));
            // 술어를 차례로 적용한다.
            for (Expr p : predicates) {
                set = applyPredicate(c, set, p);
            }
            return set;
        }
    }

    /**
     * Keeps the nodes for which a predicate is true (a number is compared with the position).
     * <p>
     * 술어가 참인 노드만 남긴다(숫자는 위치와 비교한다).
     *
     * @param c    the context / 문맥
     * @param set  the nodes in the order of the axis / 축의 순서의 노드
     * @param pred the predicate / 술어
     * @return the nodes that stay / 남은 노드
     */
    private static List<Object> applyPredicate(Ctx c, List<Object> set, Expr pred) {
        List<Object> out = new ArrayList<Object>();
        // 노드마다 위치와 크기를 넣은 문맥에서 술어를 평가한다.
        for (int i = 0; i < set.size(); i++) {
            Object v = pred.eval(c.with(set.get(i), i + 1, set.size()));
            boolean keep = v instanceof Double ? ((Double) v).doubleValue() == i + 1 : toBool(v);
            if (keep) {
                out.add(set.get(i));
            }
        }
        return out;
    }

    /** A step of a location path. / 위치 경로의 단계. */
    private static final class Step {
        /** The axis of the step, such as child or descendant. / 단계의 축(child, descendant 등). */
        final String axis;
        /** The node test kind: name, text, comment, pi, node. / 노드 테스트 종류. */
        final String test;
        /** The prefix of a name test, or null. / 이름 테스트의 접두사. 없으면 null. */
        final String prefix;
        /** The local name of a name test, or "*", or the target of processing-instruction(). / 이름 테스트의 로컬 이름이나 "*" 또는 processing-instruction() 의 대상. */
        final String name;
        /** The predicates. / 술어. */
        final List<Expr> predicates;

        /**
         * Creates the step.
         * <p>
         * 단계를 만든다.
         *
         * @param axis       the axis / 축
         * @param test       the kind of the test / 테스트 종류
         * @param prefix     the prefix or null / 접두사. 없으면 null
         * @param name       the name or null / 이름. 없으면 null
         * @param predicates the predicates / 술어
         */
        Step(String axis, String test, String prefix, String name, List<Expr> predicates) {
            this.axis = axis;
            this.test = test;
            this.prefix = prefix;
            this.name = name;
            this.predicates = predicates;
        }

        /**
         * Tells whether a node passes the node test of the step.
         * <p>
         * 노드가 단계의 노드 테스트를 통과하는지 알려 준다.
         *
         * @param c the context / 문맥
         * @param n the node / 노드
         * @return true if it passes / 통과하면 true
         */
        boolean matches(Ctx c, Object n) {
            short t = typeOf(n);
            if (test.equals("node")) {
                return true;
            } else if (test.equals("text")) {
                return t == Node.TEXT_NODE || t == Node.CDATA_SECTION_NODE;
            } else if (test.equals("comment")) {
                return t == Node.COMMENT_NODE;
            } else if (test.equals("pi")) {
                return t == Node.PROCESSING_INSTRUCTION_NODE && (name == null || name.equals(((Node) n).getNodeName()));
            }
            // 이름 테스트의 주 노드 종류는 attribute 축에서는 속성, 그 밖에는 요소이다.
            boolean attribute = axis.equals("attribute");
            if (attribute ? t != Node.ATTRIBUTE_NODE : t != Node.ELEMENT_NODE) {
                return false;
            }
            String ns = n instanceof Attr ? ((Attr) n).getNamespaceURI() : ((Element) n).getNamespaceURI();
            String local = n instanceof Attr ? ((Attr) n).getLocalName() : ((Element) n).getLocalName();
            if (prefix == null && name.equals("*")) {
                return true;
            }
            String wanted;
            if (prefix != null) {
                wanted = c.resolver == null ? null : c.resolver.lookupNamespaceURI(prefix);
                if (wanted == null) {
                    throw new XPathException(XPathException.NAMESPACE, "The prefix '" + prefix + "' cannot be resolved.");
                }
                if (name.equals("*")) {
                    return wanted.equals(ns);
                }
                return wanted.equals(ns) && name.equals(local);
            }
            // 접두사 없는 이름은 네임스페이스 없는 이름이다. HTML 문서의 HTML 요소는 HTML 네임스페이스에 있고 소문자 이름으로 비교한다.
            boolean htmlElement = c.html && !attribute && Namespaces.HTML.equals(ns);
            if (ns != null && !htmlElement) {
                return false;
            }
            return htmlElement || (c.html && attribute) ? local.equals(name.toLowerCase(Locale.ENGLISH)) : local.equals(name);
        }
    }

    /** A location path. / 위치 경로. */
    private static final class Path extends Expr {
        /** The expression that gives the start (a filter expression), or null. / 시작을 주는 식(필터 식). 없으면 null. */
        final Expr start;
        /** True for a path that starts at the root. / 루트에서 시작하는 경로이면 true. */
        final boolean absolute;
        /** The steps. / 단계. */
        final List<Step> steps;

        /**
         * Creates the path.
         * <p>
         * 경로를 만든다.
         *
         * @param start    the start expression or null / 시작 식. 없으면 null
         * @param absolute true if it starts at the root / 루트에서 시작하면 true
         * @param steps    the steps / 단계
         */
        Path(Expr start, boolean absolute, List<Step> steps) {
            this.start = start;
            this.absolute = absolute;
            this.steps = steps;
        }

        /**
         * Walks the steps from the start.
         * <p>
         * 시작에서 단계를 따라간다.
         *
         * @param c the context / 문맥
         * @return the node set / 노드 집합
         */
        Object eval(Ctx c) {
            List<Object> current = new ArrayList<Object>();
            if (start != null) {
                Object v = start.eval(c);
                if (!(v instanceof List)) {
                    throw new XPathException(XPathException.TYPE, "The expression before '/' must be a node set.");
                }
                current.addAll((List<?>) v);
            } else if (absolute) {
                current.add(rootOf(c.node));
            } else {
                current.add(c.node);
            }
            // 단계마다 현재 노드 집합의 각 노드에서 축을 따라가 노드 테스트와 술어를 적용한 결과를 합친다.
            for (Step s : steps) {
                List<Object> next = new ArrayList<Object>();
                // 현재 노드 집합의 노드마다 축을 따라 후보를 만든다.
                for (Object from : current) {
                    List<Object> candidates = new ArrayList<Object>();
                    // 축의 노드 중 노드 테스트를 통과하는 것만 후보로 모은다.
                    for (Object n : axis(s.axis, from)) {
                        if (s.matches(c, n)) {
                            candidates.add(n);
                        }
                    }
                    // 술어는 축의 방향대로 센 위치로 평가한다.
                    for (Expr p : s.predicates) {
                        candidates = applyPredicate(c, candidates, p);
                    }
                    next.addAll(candidates);
                }
                current = sorted(c, next);
            }
            return current;
        }
    }

    /** A function call. / 함수 호출. */
    private static final class Call extends Expr {
        /** The function name. / 함수 이름. */
        final String name;
        /** The arguments. / 인수. */
        final List<Expr> args;

        /**
         * Creates the node.
         * <p>
         * 노드를 만든다.
         *
         * @param name the name / 이름
         * @param args the arguments / 인수
         */
        Call(String name, List<Expr> args) {
            this.name = name;
            this.args = args;
        }

        /**
         * Calls the function of the core library.
         * <p>
         * 핵심 라이브러리의 함수를 부른다.
         *
         * @param c the context / 문맥
         * @return the value / 값
         */
        Object eval(Ctx c) {
            return callFunction(name, args, c);
        }
    }

    /**
     * The numbers of the arguments that each function of the core library takes: the minimum and the maximum (-1 for any number).
     * <p>
     * 핵심 라이브러리의 함수마다 받는 인수의 개수: 최소와 최대(-1 은 제한 없음).
     */
    private static final Object[][] ARITY = { { "last", 0, 0 }, { "position", 0, 0 }, { "count", 1, 1 }, { "id", 1, 1 }, { "local-name", 0, 1 }, { "namespace-uri", 0, 1 },
        { "name", 0, 1 }, { "string", 0, 1 }, { "concat", 2, -1 }, { "starts-with", 2, 2 }, { "contains", 2, 2 }, { "substring-before", 2, 2 }, { "substring-after", 2, 2 },
        { "substring", 2, 3 }, { "string-length", 0, 1 }, { "normalize-space", 0, 1 }, { "translate", 3, 3 }, { "boolean", 1, 1 }, { "not", 1, 1 }, { "true", 0, 0 },
        { "false", 0, 0 }, { "lang", 1, 1 }, { "number", 0, 1 }, { "sum", 1, 1 }, { "floor", 1, 1 }, { "ceiling", 1, 1 }, { "round", 1, 1 } };

    /**
     * Checks at compile time that a function exists and gets a valid number of arguments.
     * <p>
     * 함수가 있고 인수 개수가 맞는지 컴파일할 때 검사한다.
     *
     * @param name  the function name / 함수 이름
     * @param count the number of arguments / 인수 개수
     */
    private static void checkArity(String name, int count) {
        // 라이브러리의 함수 목록에서 이름을 찾는다.
        for (Object[] a : ARITY) {
            if (a[0].equals(name)) {
                int min = ((Integer) a[1]).intValue();
                int max = ((Integer) a[2]).intValue();
                if (count < min || (max >= 0 && count > max)) {
                    throw new XPathException(XPathException.SYNTAX, "The function " + name + "() got " + count + " arguments.");
                }
                return;
            }
        }
        throw new XPathException(XPathException.SYNTAX, "The function " + name + "() does not exist.");
    }

    /**
     * Returns the node set that an argument must be.
     * <p>
     * 인수가 반드시 그래야 하는 노드 집합을 반환한다.
     *
     * @param v    the value / 값
     * @param what the function name for the message / 메시지의 함수 이름
     * @return the node set / 노드 집합
     */
    @SuppressWarnings("unchecked")
    private static List<Object> nodeSet(Object v, String what) {
        if (!(v instanceof List)) {
            throw new XPathException(XPathException.TYPE, "The argument of " + what + "() must be a node set.");
        }
        return (List<Object>) v;
    }

    /**
     * Returns the first node of a node set in document order (the argument of <code>name()</code> and its kind), or the context node without an argument.
     * <p>
     * 노드 집합의 문서 순서 첫 노드를 반환한다(<code>name()</code> 류의 인수). 인수가 없으면 컨텍스트 노드.
     *
     * @param name the function name / 함수 이름
     * @param args the arguments / 인수
     * @param c    the context / 문맥
     * @return the node, or null for an empty node set / 노드. 노드 집합이 비었으면 null
     */
    private static Object firstNode(String name, List<Expr> args, Ctx c) {
        if (args.isEmpty()) {
            return c.node;
        }
        List<Object> set = sorted(c, new ArrayList<Object>(nodeSet(args.get(0).eval(c), name)));
        return set.isEmpty() ? null : set.get(0);
    }

    /**
     * Returns the string that an optional first argument gives (the string-value of the context node without one).
     * <p>
     * 선택적인 첫 인수가 주는 문자열을 반환한다(인수가 없으면 컨텍스트 노드의 문자열 값).
     *
     * @param args the arguments / 인수
     * @param c    the context / 문맥
     * @return the string / 문자열
     */
    private static String optString(List<Expr> args, Ctx c) {
        return args.isEmpty() ? stringValue(c.node) : toStr(args.get(0).eval(c));
    }

    /**
     * XPath <code>round()</code>: the closest integer, halves toward positive infinity (NaN, infinities and the signed zeros stay).
     * <p>
     * XPath <code>round()</code>: 가장 가까운 정수이고 절반은 양의 무한대 쪽이다(NaN, 무한대, 부호 있는 0 은 그대로).
     *
     * @param d the number / 숫자
     * @return the rounded number / 반올림한 숫자
     */
    private static double round(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d) || d == 0) {
            return d;
        }
        return Math.floor(d + 0.5);
    }

    /**
     * Calls a function of the core library (XPath 4).
     * <p>
     * 핵심 라이브러리의 함수를 부른다(XPath 4).
     *
     * @param name the function name / 함수 이름
     * @param args the arguments / 인수
     * @param c    the context / 문맥
     * @return the value / 값
     */
    private static Object callFunction(String name, List<Expr> args, Ctx c) {
        if (name.equals("last")) {
            return Double.valueOf(c.size);
        } else if (name.equals("position")) {
            return Double.valueOf(c.position);
        } else if (name.equals("count")) {
            return Double.valueOf(nodeSet(args.get(0).eval(c), name).size());
        } else if (name.equals("id")) {
            return idFunction(args.get(0).eval(c), c);
        } else if (name.equals("local-name") || name.equals("namespace-uri") || name.equals("name")) {
            Object n = firstNode(name, args, c);
            if (n == null) {
                return "";
            }
            return nameOf(name, n);
        } else if (name.equals("string")) {
            return optString(args, c);
        } else if (name.equals("concat")) {
            StringBuilder sb = new StringBuilder();
            // 모든 인수를 문자열로 바꿔 이어 붙인다.
            for (Expr e : args) {
                sb.append(toStr(e.eval(c)));
            }
            return sb.toString();
        } else if (name.equals("starts-with")) {
            return Boolean.valueOf(toStr(args.get(0).eval(c)).startsWith(toStr(args.get(1).eval(c))));
        } else if (name.equals("contains")) {
            return Boolean.valueOf(toStr(args.get(0).eval(c)).indexOf(toStr(args.get(1).eval(c))) >= 0);
        } else if (name.equals("substring-before") || name.equals("substring-after")) {
            String s = toStr(args.get(0).eval(c));
            String t = toStr(args.get(1).eval(c));
            int i = s.indexOf(t);
            if (i < 0) {
                return "";
            }
            return name.equals("substring-before") ? s.substring(0, i) : s.substring(i + t.length());
        } else if (name.equals("substring")) {
            return substring(toStr(args.get(0).eval(c)), toNum(args.get(1).eval(c)), args.size() > 2 ? toNum(args.get(2).eval(c)) : Double.POSITIVE_INFINITY);
        } else if (name.equals("string-length")) {
            return Double.valueOf(optString(args, c).codePointCount(0, optString(args, c).length()));
        } else if (name.equals("normalize-space")) {
            String[] words = optString(args, c).trim().split("[ \\t\\r\\n]+");
            StringBuilder sb = new StringBuilder();
            // 연속된 공백을 한 칸으로 줄여 다시 이어 붙인다.
            for (String w : words) {
                if (w.length() > 0) {
                    sb.append(sb.length() > 0 ? " " : "").append(w);
                }
            }
            return sb.toString();
        } else if (name.equals("translate")) {
            String s = toStr(args.get(0).eval(c));
            String from = toStr(args.get(1).eval(c));
            String to = toStr(args.get(2).eval(c));
            StringBuilder sb = new StringBuilder();
            // 글자마다 from 의 첫 위치를 찾아 to 의 같은 위치 글자로 바꾸고 to 가 짧으면 지운다.
            for (int i = 0; i < s.length(); i++) {
                int k = from.indexOf(s.charAt(i));
                if (k < 0) {
                    sb.append(s.charAt(i));
                } else if (k < to.length()) {
                    sb.append(to.charAt(k));
                }
            }
            return sb.toString();
        } else if (name.equals("boolean")) {
            return Boolean.valueOf(toBool(args.get(0).eval(c)));
        } else if (name.equals("not")) {
            return Boolean.valueOf(!toBool(args.get(0).eval(c)));
        } else if (name.equals("true")) {
            return Boolean.TRUE;
        } else if (name.equals("false")) {
            return Boolean.FALSE;
        } else if (name.equals("lang")) {
            return Boolean.valueOf(lang(toStr(args.get(0).eval(c)), c.node));
        } else if (name.equals("number")) {
            return Double.valueOf(args.isEmpty() ? parseNumber(stringValue(c.node)) : toNum(args.get(0).eval(c)));
        } else if (name.equals("sum")) {
            double sum = 0;
            // 노드마다 문자열 값을 숫자로 바꿔 더한다.
            for (Object n : nodeSet(args.get(0).eval(c), name)) {
                sum += parseNumber(stringValue(n));
            }
            return Double.valueOf(sum);
        } else if (name.equals("floor")) {
            return Double.valueOf(Math.floor(toNum(args.get(0).eval(c))));
        } else if (name.equals("ceiling")) {
            return Double.valueOf(Math.ceil(toNum(args.get(0).eval(c))));
        }
        return Double.valueOf(round(toNum(args.get(0).eval(c))));
    }

    /**
     * Returns the name of a node for <code>local-name()</code>, <code>namespace-uri()</code> or <code>name()</code>.
     * <p>
     * 노드의 이름을 <code>local-name()</code>, <code>namespace-uri()</code>, <code>name()</code> 용으로 반환한다.
     *
     * @param function the function name / 함수 이름
     * @param n        the node / 노드
     * @return the name / 이름
     */
    private static String nameOf(String function, Object n) {
        String local = "";
        String ns = "";
        String qname = "";
        if (n instanceof Attr) {
            Attr a = (Attr) n;
            local = a.getLocalName() == null ? a.getName() : a.getLocalName();
            ns = a.getNamespaceURI() == null ? "" : a.getNamespaceURI();
            qname = a.getName();
        } else if (n instanceof Element) {
            Element e = (Element) n;
            local = e.getLocalName();
            ns = e.getNamespaceURI() == null ? "" : e.getNamespaceURI();
            qname = e.getPrefix() == null || e.getPrefix().length() == 0 ? local : e.getPrefix() + ":" + local;
        } else if (((Node) n).getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
            local = ((Node) n).getNodeName();
            qname = local;
        }
        return function.equals("local-name") ? local : function.equals("namespace-uri") ? ns : qname;
    }

    /**
     * XPath <code>substring()</code> with its rounding rules: the characters at positions from <code>round(start)</code> for <code>round(length)</code>.
     * <p>
     * 반올림 규칙이 있는 XPath <code>substring()</code>: <code>round(start)</code> 위치부터 <code>round(length)</code> 개의 글자.
     *
     * @param s      the string / 문자열
     * @param start  the start position (from 1) / 시작 위치(1부터)
     * @param length the length / 길이
     * @return the substring / 부분 문자열
     */
    private static String substring(String s, double start, double length) {
        double from = round(start);
        double to = from + round(length);
        StringBuilder sb = new StringBuilder();
        // 위치가 [from, to) 에 드는 글자만 모은다(NaN 이면 비교가 거짓이라 비어 있다).
        for (int i = 0; i < s.length(); i++) {
            double p = i + 1;
            if (p >= from && p < to) {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    /**
     * XPath <code>id()</code>: the elements whose ID is one of the white space separated tokens of the argument, in document order.
     * <p>
     * XPath <code>id()</code>: ID 가 인수의 공백으로 구분한 토큰 중 하나인 요소를 문서 순서로 반환한다.
     *
     * @param arg the argument value / 인수 값
     * @param c   the context / 문맥
     * @return the node set / 노드 집합
     */
    private static Object idFunction(Object arg, Ctx c) {
        List<String> tokens = new ArrayList<String>();
        if (arg instanceof List) {
            // 노드 집합이면 노드마다 문자열 값을 토큰으로 나눈다.
            for (Object n : (List<?>) arg) {
                tokens.addAll(split(stringValue(n)));
            }
        } else {
            tokens.addAll(split(toStr(arg)));
        }
        Document doc = documentOf(c.node);
        List<Object> out = new ArrayList<Object>();
        // 토큰마다 문서에서 ID 가 같은 요소를 찾는다.
        for (String t : tokens) {
            Element e = doc == null ? null : doc.getElementById(t);
            if (e != null && !out.contains(e)) {
                out.add(e);
            }
        }
        return sorted(c, out);
    }

    /**
     * Splits a string at white space.
     * <p>
     * 문자열을 공백에서 나눈다.
     *
     * @param s the string / 문자열
     * @return the tokens / 토큰
     */
    private static List<String> split(String s) {
        List<String> out = new ArrayList<String>();
        // 공백으로 나눈 조각 중 비지 않은 것만 담는다.
        for (String t : s.trim().split("[ \\t\\r\\n]+")) {
            if (t.length() > 0) {
                out.add(t);
            }
        }
        return out;
    }

    /**
     * XPath <code>lang()</code>: the language of the nearest ancestor-or-self with an <code>xml:lang</code> (or, in HTML, a <code>lang</code>) attribute equals the
     * argument or starts with it followed by a hyphen, ignoring case.
     * <p>
     * XPath <code>lang()</code>: <code>xml:lang</code>(HTML 에서는 <code>lang</code>) 속성이 있는 가장 가까운 조상 또는 자신의 언어가 인수와 같거나 인수 뒤에 하이픈이
     * 이어지는 것으로 시작하는지(대소문자 무시).
     *
     * @param wanted the language / 언어
     * @param node   the context node / 컨텍스트 노드
     * @return true if it matches / 일치하면 true
     */
    private static boolean lang(String wanted, Object node) {
        // 컨텍스트 노드에서 위로 올라가며 언어 속성이 있는 첫 요소를 찾는다.
        for (Object n = node; n != null; n = parentOf(n)) {
            if (n instanceof Element) {
                Element e = (Element) n;
                String v = e.hasAttributeNS(Namespaces.XML, "lang") ? e.getAttributeNS(Namespaces.XML, "lang") : e.hasAttribute("lang") ? e.getAttribute("lang") : null;
                if (v != null) {
                    String l = v.toLowerCase(Locale.ENGLISH);
                    String w = wanted.toLowerCase(Locale.ENGLISH);
                    return l.equals(w) || l.startsWith(w + "-");
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------
    // 구문 분석
    // ------------------------------------------------------------------------------------------------------------------------------------------------

    /**
     * The recursive descent parser of XPath 1.0 (the grammar of section 3 and the lexical rules of section 3.7, including the rule that decides whether
     * <code>*</code> and <code>and</code>, <code>or</code>, <code>mod</code>, <code>div</code> are operators or names).
     * <p>
     * XPath 1.0 의 재귀 하강 구문 분석기(3절의 문법과 3.7절의 어휘 규칙, <code>*</code> 와 <code>and</code>, <code>or</code>, <code>mod</code>, <code>div</code> 가 연산자인지
     * 이름인지 정하는 규칙 포함).
     */
    private static final class Parser {
        /** The text. / 문자열. */
        private final String s;
        /** The position of the next character. / 다음 글자의 위치. */
        private int i;
        /** The kind of the last token: 0 none, 1 operator or opening token (so that * is a name), 2 operand (so that * is multiplication). / 마지막 토큰의 종류. */
        private int last;

        /**
         * Creates a parser.
         * <p>
         * 분석기를 만든다.
         *
         * @param s the text / 문자열
         */
        Parser(String s) {
            this.s = s;
        }

        /**
         * Reports a syntax error.
         * <p>
         * 구문 오류를 보고한다.
         *
         * @return the exception to throw / 던질 예외
         */
        private XPathException fail() {
            return new XPathException(XPathException.SYNTAX, "The string '" + s + "' is not a valid XPath expression.");
        }

        /**
         * Skips white space.
         * <p>
         * 공백을 건너뛴다.
         */
        private void ws() {
            // 공백 글자를 건너뛴다.
            while (i < s.length() && (s.charAt(i) == ' ' || s.charAt(i) == '\t' || s.charAt(i) == '\r' || s.charAt(i) == '\n')) {
                i++;
            }
        }

        /**
         * Tells whether the text continues with a string (after white space), and consumes it if it does.
         * <p>
         * 문자열이 (공백 뒤에) 이어지는지 알려 주고 그렇다면 소비한다.
         *
         * @param t the string / 문자열
         * @return true if it was there / 있었으면 true
         */
        private boolean eat(String t) {
            ws();
            if (s.startsWith(t, i)) {
                i += t.length();
                last = 1;
                return true;
            }
            return false;
        }

        /**
         * Tells whether the text continues with a string, without consuming.
         * <p>
         * 문자열이 이어지는지 소비하지 않고 알려 준다.
         *
         * @param t the string / 문자열
         * @return true if it is there / 있으면 true
         */
        private boolean peek(String t) {
            ws();
            return s.startsWith(t, i);
        }

        /**
         * Fails unless the whole text was parsed.
         * <p>
         * 문자열 전체를 해석하지 않았으면 실패한다.
         */
        void expectEnd() {
            ws();
            if (i != s.length()) {
                throw fail();
            }
        }

        /**
         * Parses an Expr (the lowest precedence: <code>or</code>).
         * <p>
         * Expr 을 해석한다(가장 낮은 우선순위: <code>or</code>).
         *
         * @return the expression / 식
         */
        Expr parseExpression() {
            Expr e = parseAnd();
            // or 가 이어지는 동안 왼쪽 결합으로 쌓는다.
            while (peekWord("or")) {
                i += 2;
                last = 1;
                e = new Binary("or", e, parseAnd());
            }
            return e;
        }

        /**
         * Tells whether an operator word (<code>or</code>, <code>and</code>, <code>div</code>, <code>mod</code>) is next: it is an operator only after an operand and
         * when no name character follows.
         * <p>
         * 연산자 단어(<code>or</code>, <code>and</code>, <code>div</code>, <code>mod</code>)가 다음에 오는지 알려 준다: 피연산자 뒤이고 이름 글자가 이어지지 않을 때만 연산자이다.
         *
         * @param w the word / 단어
         * @return true if it is next / 다음에 오면 true
         */
        private boolean peekWord(String w) {
            ws();
            if (last != 2 || !s.startsWith(w, i)) {
                return false;
            }
            int end = i + w.length();
            return end >= s.length() || !isNameChar(s.charAt(end));
        }

        /**
         * Parses an AndExpr.
         * <p>
         * AndExpr 을 해석한다.
         *
         * @return the expression / 식
         */
        private Expr parseAnd() {
            Expr e = parseEquality();
            // and 가 이어지는 동안 왼쪽 결합으로 쌓는다.
            while (peekWord("and")) {
                i += 3;
                last = 1;
                e = new Binary("and", e, parseEquality());
            }
            return e;
        }

        /**
         * Parses an EqualityExpr.
         * <p>
         * EqualityExpr 을 해석한다.
         *
         * @return the expression / 식
         */
        private Expr parseEquality() {
            Expr e = parseRelational();
            // = 와 != 가 이어지는 동안 왼쪽 결합으로 쌓는다.
            for (;;) {
                if (eat("!=")) {
                    e = new Binary("!=", e, parseRelational());
                } else if (eat("=")) {
                    e = new Binary("=", e, parseRelational());
                } else {
                    return e;
                }
            }
        }

        /**
         * Parses a RelationalExpr.
         * <p>
         * RelationalExpr 을 해석한다.
         *
         * @return the expression / 식
         */
        private Expr parseRelational() {
            Expr e = parseAdditive();
            // 비교 연산자가 이어지는 동안 왼쪽 결합으로 쌓는다(두 글자 연산자를 먼저 본다).
            for (;;) {
                if (eat("<=")) {
                    e = new Binary("<=", e, parseAdditive());
                } else if (eat(">=")) {
                    e = new Binary(">=", e, parseAdditive());
                } else if (eat("<")) {
                    e = new Binary("<", e, parseAdditive());
                } else if (eat(">")) {
                    e = new Binary(">", e, parseAdditive());
                } else {
                    return e;
                }
            }
        }

        /**
         * Parses an AdditiveExpr.
         * <p>
         * AdditiveExpr 을 해석한다.
         *
         * @return the expression / 식
         */
        private Expr parseAdditive() {
            Expr e = parseMultiplicative();
            // + 와 - 가 이어지는 동안 왼쪽 결합으로 쌓는다.
            for (;;) {
                if (eat("+")) {
                    e = new Binary("+", e, parseMultiplicative());
                } else if (last == 2 && eat("-")) {
                    e = new Binary("-", e, parseMultiplicative());
                } else {
                    return e;
                }
            }
        }

        /**
         * Parses a MultiplicativeExpr: <code>*</code> multiplies after an operand, <code>div</code> and <code>mod</code> are operators after an operand.
         * <p>
         * MultiplicativeExpr 을 해석한다: 피연산자 뒤의 <code>*</code> 는 곱셈이고 피연산자 뒤의 <code>div</code>, <code>mod</code> 는 연산자이다.
         *
         * @return the expression / 식
         */
        private Expr parseMultiplicative() {
            Expr e = parseUnary();
            // 곱셈과 나눗셈 연산자가 이어지는 동안 왼쪽 결합으로 쌓는다.
            for (;;) {
                ws();
                if (last == 2 && i < s.length() && s.charAt(i) == '*') {
                    i++;
                    last = 1;
                    e = new Binary("*", e, parseUnary());
                } else if (peekWord("div")) {
                    i += 3;
                    last = 1;
                    e = new Binary("div", e, parseUnary());
                } else if (peekWord("mod")) {
                    i += 3;
                    last = 1;
                    e = new Binary("mod", e, parseUnary());
                } else {
                    return e;
                }
            }
        }

        /**
         * Parses a UnaryExpr (any number of minus signs).
         * <p>
         * UnaryExpr 을 해석한다(빼기 부호는 몇 개든 된다).
         *
         * @return the expression / 식
         */
        private Expr parseUnary() {
            ws();
            if (i < s.length() && s.charAt(i) == '-' && last != 2) {
                i++;
                last = 1;
                return new Negate(parseUnary());
            }
            return parseUnion();
        }

        /**
         * Parses a UnionExpr.
         * <p>
         * UnionExpr 을 해석한다.
         *
         * @return the expression / 식
         */
        private Expr parseUnion() {
            Expr first = parsePath();
            if (!peek("|")) {
                return first;
            }
            List<Expr> parts = new ArrayList<Expr>();
            parts.add(first);
            // | 로 이어지는 경로를 모두 모은다.
            while (eat("|")) {
                parts.add(parsePath());
            }
            return new Union(parts);
        }

        /**
         * Tells whether a character can be part of a name.
         * <p>
         * 글자가 이름의 일부가 될 수 있는지 알려 준다.
         *
         * @param ch the character / 글자
         * @return true if it can / 될 수 있으면 true
         */
        private static boolean isNameChar(char ch) {
            return Character.isLetterOrDigit(ch) || ch == '_' || ch == '-' || ch == '.' || ch > 127;
        }

        /**
         * Tells whether a character can start a name.
         * <p>
         * 글자가 이름을 시작할 수 있는지 알려 준다.
         *
         * @param ch the character / 글자
         * @return true if it can / 될 수 있으면 true
         */
        private static boolean isNameStart(char ch) {
            return Character.isLetter(ch) || ch == '_' || ch > 127;
        }

        /**
         * Reads an NCName at the position (or null).
         * <p>
         * 위치의 NCName 을 읽는다(없으면 null).
         *
         * @return the name or null / 이름 또는 null
         */
        private String ncName() {
            if (i >= s.length() || !isNameStart(s.charAt(i))) {
                return null;
            }
            int start = i;
            // 이름 글자가 이어지는 동안 읽는다.
            while (i < s.length() && isNameChar(s.charAt(i))) {
                i++;
            }
            return s.substring(start, i);
        }

        /**
         * Parses a PathExpr: a location path, or a filter expression that may continue with <code>/</code> or <code>//</code> and relative steps.
         * <p>
         * PathExpr 을 해석한다: 위치 경로, 또는 <code>/</code> 나 <code>//</code> 와 상대 단계가 이어질 수 있는 필터 식.
         *
         * @return the expression / 식
         */
        private Expr parsePath() {
            ws();
            if (i >= s.length()) {
                throw fail();
            }
            char ch = s.charAt(i);
            boolean primary = ch == '(' || ch == '"' || ch == '\'' || ch == '$' || Character.isDigit(ch) || (ch == '.' && i + 1 < s.length() && Character.isDigit(s.charAt(i + 1)));
            // 이름 다음에 ( 가 오면 함수 호출인데 노드 종류 테스트(node, text, comment, processing-instruction)는 제외한다.
            if (!primary && isNameStart(ch)) {
                int save = i;
                int saveLast = last;
                String n = ncName();
                ws();
                if (i < s.length() && s.charAt(i) == '(' && !isNodeType(n)) {
                    primary = true;
                }
                i = save;
                last = saveLast;
            }
            if (primary) {
                List<Expr> preds = new ArrayList<Expr>();
                Expr e = parsePrimary();
                // 술어가 이어지는 동안 모은다.
                while (peek("[")) {
                    preds.add(parsePredicate());
                }
                Expr filtered = preds.isEmpty() ? e : new Filter(e, preds);
                if (peek("//") || peek("/")) {
                    List<Step> steps = new ArrayList<Step>();
                    parseRelativeAfterSlash(steps);
                    parseMoreSteps(steps);
                    return new Path(filtered, false, steps);
                }
                return filtered;
            }
            List<Step> steps = new ArrayList<Step>();
            if (peek("//")) {
                parseRelativeAfterSlash(steps);
                parseMoreSteps(steps);
                return new Path(null, true, steps);
            } else if (peek("/")) {
                i++;
                last = 1;
                ws();
                // 슬래시 하나만이면 루트이다. 단계가 이어지면 파싱한다.
                if (i < s.length() && startsStep()) {
                    steps.add(parseStep());
                    parseMoreSteps(steps);
                } else {
                    last = 2;
                }
                return new Path(null, true, steps);
            }
            steps.add(parseStep());
            parseMoreSteps(steps);
            return new Path(null, false, steps);
        }

        /**
         * Tells whether the position starts a step.
         * <p>
         * 위치에서 단계가 시작하는지 알려 준다.
         *
         * @return true if a step starts here / 단계가 시작하면 true
         */
        private boolean startsStep() {
            char ch = s.charAt(i);
            return isNameStart(ch) || ch == '*' || ch == '@' || ch == '.';
        }

        /**
         * Parses the steps that follow a first step: <code>/step</code> and <code>//step</code>.
         * <p>
         * 첫 단계 뒤의 단계를 해석한다: <code>/step</code> 과 <code>//step</code>.
         *
         * @param steps the steps / 단계
         */
        private void parseMoreSteps(List<Step> steps) {
            // 슬래시가 이어지는 동안 단계를 더한다.
            while (peek("/")) {
                parseRelativeAfterSlash(steps);
            }
        }

        /**
         * Parses a <code>/</code> or <code>//</code> and the step after it (<code>//</code> adds <code>descendant-or-self::node()</code>).
         * <p>
         * <code>/</code> 나 <code>//</code> 와 그 뒤의 단계를 해석한다(<code>//</code> 는 <code>descendant-or-self::node()</code> 를 더한다).
         *
         * @param steps the steps / 단계
         */
        private void parseRelativeAfterSlash(List<Step> steps) {
            if (eat("//")) {
                steps.add(new Step("descendant-or-self", "node", null, null, new ArrayList<Expr>()));
            } else if (!eat("/")) {
                throw fail();
            }
            steps.add(parseStep());
        }

        /**
         * Tells whether a name is a node type test.
         * <p>
         * 이름이 노드 종류 테스트인지 알려 준다.
         *
         * @param n the name / 이름
         * @return true if it is / 그렇다면 true
         */
        private static boolean isNodeType(String n) {
            return n.equals("node") || n.equals("text") || n.equals("comment") || n.equals("processing-instruction");
        }

        /**
         * Parses a Step: an abbreviation (<code>.</code>, <code>..</code>), or an optional axis (<code>axis::</code> or <code>@</code>), a node test and predicates.
         * <p>
         * Step 을 해석한다: 약어(<code>.</code>, <code>..</code>), 또는 선택적 축(<code>axis::</code>, <code>@</code>), 노드 테스트, 술어.
         *
         * @return the step / 단계
         */
        private Step parseStep() {
            ws();
            if (s.startsWith("..", i)) {
                i += 2;
                last = 2;
                return new Step("parent", "node", null, null, new ArrayList<Expr>());
            } else if (s.startsWith(".", i)) {
                i++;
                last = 2;
                return new Step("self", "node", null, null, new ArrayList<Expr>());
            }
            String axis = "child";
            if (s.startsWith("@", i)) {
                i++;
                axis = "attribute";
            } else {
                int save = i;
                String n = ncName();
                ws();
                if (n != null && s.startsWith("::", i)) {
                    boolean known = false;
                    // 축 이름 목록에 있는지 확인한다.
                    for (String a : AXES) {
                        known = known || a.equals(n);
                    }
                    if (!known) {
                        throw fail();
                    }
                    axis = n;
                    i += 2;
                } else {
                    i = save;
                }
            }
            last = 1;
            ws();
            String test;
            String prefix = null;
            String name = null;
            if (i < s.length() && s.charAt(i) == '*') {
                i++;
                test = "name";
                name = "*";
            } else {
                String n = ncName();
                if (n == null) {
                    throw fail();
                }
                if (i < s.length() && s.charAt(i) == '(' && isNodeType(n)) {
                    i++;
                    ws();
                    test = n.equals("processing-instruction") ? "pi" : n;
                    // processing-instruction 은 대상 리터럴을 받을 수 있다.
                    if (test.equals("pi") && i < s.length() && (s.charAt(i) == '"' || s.charAt(i) == '\'')) {
                        name = literal();
                        ws();
                    }
                    if (i >= s.length() || s.charAt(i) != ')') {
                        throw fail();
                    }
                    i++;
                } else {
                    test = "name";
                    name = n;
                    // 콜론이 이어지면 접두사이다(콜론 두 개는 축이므로 제외).
                    if (i + 1 < s.length() && s.charAt(i) == ':' && s.charAt(i + 1) != ':') {
                        i++;
                        prefix = n;
                        if (i < s.length() && s.charAt(i) == '*') {
                            i++;
                            name = "*";
                        } else {
                            name = ncName();
                            if (name == null) {
                                throw fail();
                            }
                        }
                    }
                }
            }
            last = 2;
            List<Expr> preds = new ArrayList<Expr>();
            // 술어가 이어지는 동안 모은다.
            while (peek("[")) {
                preds.add(parsePredicate());
            }
            return new Step(axis, test, prefix, name, preds);
        }

        /**
         * Parses a predicate: <code>[</code> Expr <code>]</code>.
         * <p>
         * 술어를 해석한다: <code>[</code> Expr <code>]</code>.
         *
         * @return the expression inside / 안의 식
         */
        private Expr parsePredicate() {
            eat("[");
            Expr e = parseExpression();
            if (!eat("]")) {
                throw fail();
            }
            last = 2;
            return e;
        }

        /**
         * Reads a quoted literal.
         * <p>
         * 따옴표로 묶은 리터럴을 읽는다.
         *
         * @return the text inside the quotes / 따옴표 안의 문자열
         */
        private String literal() {
            char q = s.charAt(i);
            int end = s.indexOf(q, i + 1);
            if (end < 0) {
                throw fail();
            }
            String v = s.substring(i + 1, end);
            i = end + 1;
            return v;
        }

        /**
         * Parses a PrimaryExpr: a variable (an error), a parenthesized expression, a literal, a number or a function call.
         * <p>
         * PrimaryExpr 을 해석한다: 변수(오류), 괄호 식, 리터럴, 숫자, 함수 호출.
         *
         * @return the expression / 식
         */
        private Expr parsePrimary() {
            ws();
            char ch = s.charAt(i);
            if (ch == '$') {
                throw new XPathException(XPathException.TYPE, "A variable reference is not supported: the expression has no variables.");
            } else if (ch == '(') {
                i++;
                last = 1;
                Expr e = parseExpression();
                if (!eat(")")) {
                    throw fail();
                }
                last = 2;
                return e;
            } else if (ch == '"' || ch == '\'') {
                Expr e = new Literal(literal());
                last = 2;
                return e;
            } else if (Character.isDigit(ch) || ch == '.') {
                int start = i;
                // 숫자와 소수점을 읽는다(지수 표기는 XPath 1.0 에 없다).
                while (i < s.length() && Character.isDigit(s.charAt(i))) {
                    i++;
                }
                if (i < s.length() && s.charAt(i) == '.') {
                    i++;
                    // 소수부의 숫자를 읽는다.
                    while (i < s.length() && Character.isDigit(s.charAt(i))) {
                        i++;
                    }
                }
                last = 2;
                return new Literal(Double.valueOf(Double.parseDouble(s.substring(start, i))));
            }
            String name = ncName();
            if (name == null) {
                throw fail();
            }
            // 접두사가 있는 함수 이름은 핵심 라이브러리에 없다.
            if (i < s.length() && s.charAt(i) == ':') {
                throw new XPathException(XPathException.SYNTAX, "The function " + name + ":...() does not exist.");
            }
            ws();
            if (i >= s.length() || s.charAt(i) != '(') {
                throw fail();
            }
            i++;
            last = 1;
            List<Expr> args = new ArrayList<Expr>();
            ws();
            if (!peek(")")) {
                args.add(parseExpression());
                // 쉼표로 이어지는 인수를 모은다.
                while (eat(",")) {
                    args.add(parseExpression());
                }
            }
            if (!eat(")")) {
                throw fail();
            }
            last = 2;
            checkArity(name, args.size());
            return new Call(name, args);
        }
    }
}
