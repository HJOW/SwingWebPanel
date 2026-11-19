package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory of the live element collections of the DOM Standard: by tag name, by namespace and local name, by class names, and the
 * element children of a node. The matching rules follow the standard, including the case rules of HTML documents (HTML elements match
 * a lower-cased tag name) and quirks mode (class names compare case-insensitively).
 * <p>
 * DOM 표준의 라이브 요소 컬렉션 팩터리: 태그 이름별, 네임스페이스와 로컬 이름별, 클래스 이름별, 노드의 요소 자식. 일치 규칙은 HTML 문서의 대소문자
 * 규칙(HTML 요소는 소문자로 바꾼 태그 이름과 일치)과 호환 모드(클래스 이름을 대소문자 구분 없이 비교)를 포함해 표준을 따른다.
 */
public final class ElementCollections {

    /**
     * Private constructor: static factory class.
     * <p>
     * 비공개 생성자: 정적 팩터리 클래스이다.
     */
    private ElementCollections() {
    }

    /**
     * Creates the live collection of the descendants with a qualified name (<code>getElementsByTagName</code>); <code>*</code>
     * matches all.
     * <p>
     * 정규화된 이름을 가진 후손의 라이브 컬렉션을 만든다(<code>getElementsByTagName</code>). <code>*</code> 은 모두 일치한다.
     *
     * @param root          search root / 검색 루트
     * @param qualifiedName qualified name or * / 정규화된 이름 또는 *
     * @return the collection / 컬렉션
     */
    public static ElementCollection byTagName(final Node root, final String qualifiedName) {
        final boolean all = "*".equals(qualifiedName);
        final String lower = Namespaces.asciiLower(qualifiedName);
        return new ElementCollection(root) {
            /**
             * Applies the tag name rules.
             * <p>
             * 태그 이름 규칙을 적용한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                if (all) {
                    return true;
                }
                String name = e.getTagNameQualified();
                if (root.document().isHtmlDocument() && Namespaces.HTML.equals(e.getNamespaceURI())) {
                    return name.equals(lower);
                }
                return name.equals(qualifiedName);
            }
        };
    }

    /**
     * Creates the live collection of the descendants with a namespace and local name (<code>getElementsByTagNameNS</code>); a null or
     * <code>*</code> namespace and <code>*</code> local name match all.
     * <p>
     * 네임스페이스와 로컬 이름을 가진 후손의 라이브 컬렉션을 만든다(<code>getElementsByTagNameNS</code>). null 또는 <code>*</code> 인
     * 네임스페이스와 <code>*</code> 인 로컬 이름은 모두 일치한다.
     *
     * @param root      search root / 검색 루트
     * @param namespace namespace, empty string means no namespace, * means any / 네임스페이스. 빈 문자열은 없음, *는 모두
     * @param localName local name or * / 로컬 이름 또는 *
     * @return the collection / 컬렉션
     */
    public static ElementCollection byTagNameNS(Node root, final String namespace, final String localName) {
        final String ns = namespace != null && namespace.length() == 0 ? null : namespace;
        final boolean anyNs = "*".equals(namespace);
        final boolean anyLocal = "*".equals(localName);
        return new ElementCollection(root) {
            /**
             * Applies the namespace and local name rules.
             * <p>
             * 네임스페이스와 로컬 이름 규칙을 적용한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                boolean nsOk = anyNs || (ns == null ? e.getNamespaceURI() == null : ns.equals(e.getNamespaceURI()));
                return nsOk && (anyLocal || localName.equals(e.getLocalName()));
            }
        };
    }

    /**
     * Creates the live collection of the HTML elements whose <code>name</code> attribute has a value (<code>document.getElementsByName</code>).
     * <p>
     * <code>name</code> 속성이 어떤 값인 HTML 요소의 라이브 컬렉션을 만든다(<code>document.getElementsByName</code>).
     *
     * @param root search root / 검색 루트
     * @param name the value of the <code>name</code> attribute / <code>name</code> 속성의 값
     * @return the collection / 컬렉션
     */
    public static ElementCollection byNameAttribute(final Node root, final String name) {
        return new ElementCollection(root) {
            /**
             * Matches an HTML element with the name.
             * <p>
             * 이름이 같은 HTML 요소와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                return Namespaces.HTML.equals(e.getNamespaceURI()) && name.equals(e.getAttribute("name"));
            }
        };
    }

    /**
     * Creates the live collection of the hyperlinks of a document (<code>document.links</code>): the <code>a</code> and <code>area</code>
     * elements that have an <code>href</code> attribute.
     * <p>
     * 문서의 하이퍼링크 라이브 컬렉션을 만든다(<code>document.links</code>): <code>href</code> 속성이 있는 <code>a</code> 와 <code>area</code> 요소.
     *
     * @param root search root / 검색 루트
     * @return the collection / 컬렉션
     */
    public static ElementCollection links(Node root) {
        return new ElementCollection(root) {
            /**
             * Matches an <code>a</code> or <code>area</code> element with an <code>href</code>.
             * <p>
             * <code>href</code> 가 있는 <code>a</code> 또는 <code>area</code> 요소와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                String n = e.getLocalName();
                return Namespaces.HTML.equals(e.getNamespaceURI()) && ("a".equals(n) || "area".equals(n)) && e.hasAttribute("href");
            }
        };
    }

    /**
     * Creates the live collection of the named anchors of a document (<code>document.anchors</code>): the <code>a</code> elements that have
     * a <code>name</code> attribute.
     * <p>
     * 문서의 이름 있는 앵커 라이브 컬렉션을 만든다(<code>document.anchors</code>): <code>name</code> 속성이 있는 <code>a</code> 요소.
     *
     * @param root search root / 검색 루트
     * @return the collection / 컬렉션
     */
    public static ElementCollection anchors(Node root) {
        return new ElementCollection(root) {
            /**
             * Matches an <code>a</code> element with a <code>name</code>.
             * <p>
             * <code>name</code> 이 있는 <code>a</code> 요소와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                return Namespaces.HTML.equals(e.getNamespaceURI()) && "a".equals(e.getLocalName()) && e.hasAttribute("name");
            }
        };
    }

    /**
     * Creates the live collection of the HTML element children of a node that have one of the given local names (the <code>tBodies</code>,
     * <code>rows</code> and <code>cells</code> of the table interfaces).
     * <p>
     * 노드의 자식 중 주어진 로컬 이름 가운데 하나를 가진 HTML 요소의 라이브 컬렉션을 만든다(표 인터페이스의 <code>tBodies</code>, <code>rows</code>,
     * <code>cells</code>).
     *
     * @param parent the parent whose children are listed / 자식을 나열할 부모
     * @param names  the local names / 로컬 이름
     * @return the collection / 컬렉션
     */
    public static ElementCollection childrenByTagName(final Node parent, final String... names) {
        return new ElementCollection(parent) {
            /**
             * Matches a child of the parent with one of the names.
             * <p>
             * 이름 가운데 하나를 가진 부모의 자식과 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                if (e.getParentNode() != parent || !Namespaces.HTML.equals(e.getNamespaceURI())) {
                    return false;
                }
                // 이름 목록에서 요소의 로컬 이름과 같은 것이 있는지 확인한다.
                for (int i = 0; i < names.length; i++) {
                    if (names[i].equals(e.getLocalName())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * Creates the live collection of the rows of a table (<code>HTMLTableElement.rows</code>): the <code>tr</code> children of the table
     * and of its <code>thead</code>, <code>tbody</code> and <code>tfoot</code> children, in tree order.
     * <p>
     * 표의 행(<code>HTMLTableElement.rows</code>)의 라이브 컬렉션을 만든다: 표와 그 <code>thead</code>, <code>tbody</code>, <code>tfoot</code> 자식의
     * <code>tr</code> 자식을 트리 순서로.
     *
     * @param table the table / 표
     * @return the collection / 컬렉션
     */
    public static ElementCollection tableRows(final Node table) {
        return new ElementCollection(table) {
            /**
             * Matches a row of the table or of one of its sections.
             * <p>
             * 표 또는 그 구역의 행과 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                if (!Namespaces.HTML.equals(e.getNamespaceURI()) || !"tr".equals(e.getLocalName())) {
                    return false;
                }
                Node p = e.getParentNode();
                if (p == table) {
                    return true;
                }
                if (p instanceof Element && p.getParentNode() == table) {
                    String n = ((Element) p).getLocalName();
                    return Namespaces.HTML.equals(((Element) p).getNamespaceURI()) && ("thead".equals(n) || "tbody".equals(n) || "tfoot".equals(n));
                }
                return false;
            }
        };
    }

    /**
     * Creates the live collection of the descendants that have all the given class names (<code>getElementsByClassName</code>). An
     * argument without any class name gives an empty collection.
     * <p>
     * 주어진 클래스 이름을 모두 가진 후손의 라이브 컬렉션을 만든다(<code>getElementsByClassName</code>). 클래스 이름이 하나도 없는 인수는
     * 빈 컬렉션이 된다.
     *
     * @param root       search root / 검색 루트
     * @param classNames space separated class names / 공백으로 구분한 클래스 이름
     * @return the collection / 컬렉션
     */
    public static ElementCollection byClassName(final Node root, String classNames) {
        final List<String> wanted = ClassList.parseTokens(classNames == null ? "" : classNames);
        return new ElementCollection(root) {
            /**
             * Applies the class name rules (case-insensitive in quirks mode).
             * <p>
             * 클래스 이름 규칙을 적용한다(호환 모드에서는 대소문자 구분 없음).
             *
             * @param e candidate element / 후보 요소
             * @return true if it has all class names / 모든 클래스 이름이 있으면 true
             */
            boolean matches(Element e) {
                if (wanted.isEmpty()) {
                    return false;
                }
                List<String> have = ClassList.parseTokens(e.getAttribute("class") == null ? "" : e.getAttribute("class"));
                boolean quirks = root.document().getCompatibilityMode() == Document.QUIRKS_MODE;
                // 요구한 클래스 이름이 요소의 클래스 목록에 모두 있는지 확인한다.
                for (int i = 0; i < wanted.size(); i++) {
                    if (!containsName(have, wanted.get(i), quirks)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Tells whether a token list contains a name.
     * <p>
     * 토큰 목록에 이름이 있는지 알려 준다.
     *
     * @param list       tokens / 토큰
     * @param name       name to find / 찾을 이름
     * @param ignoreCase true to compare ASCII case-insensitively / ASCII 대소문자를 구분하지 않으려면 true
     * @return true if found / 찾으면 true
     */
    private static boolean containsName(List<String> list, String name, boolean ignoreCase) {
        // 목록을 훑으며 이름이 같은 것을 찾는다.
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i);
            if (ignoreCase ? Namespaces.asciiLower(s).equals(Namespaces.asciiLower(name)) : s.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates the live collection of the element children of a node (<code>children</code>).
     * <p>
     * 노드의 요소 자식의 라이브 컬렉션을 만든다(<code>children</code>).
     *
     * @param parent parent node / 부모 노드
     * @return the collection / 컬렉션
     */
    public static ElementCollection children(Node parent) {
        return new ElementCollection(parent) {
            /**
             * Accepts every child element.
             * <p>
             * 모든 자식 요소를 받아들인다.
             *
             * @param e child element / 자식 요소
             * @return always true / 항상 true
             */
            boolean matches(Element e) {
                return true;
            }

            /**
             * Only the children are candidates.
             * <p>
             * 자식만 후보이다.
             *
             * @return true / true
             */
            boolean childrenOnly() {
                return true;
            }
        };
    }

    /**
     * Returns the map that a hash-name reference (the <code>usemap</code> of an image) names (HTML5 REC 2014-10-28 section 2.4.9): the
     * first <code>map</code> element in tree order that has an <code>id</code> equal to the text after the first "#", or a
     * <code>name</code> that matches it without regard to case. A value without "#", or with "#" at its end, names no map.
     * <p>
     * 해시 이름 참조(이미지의 <code>usemap</code>)가 가리키는 맵을 반환한다(HTML5 REC 2014-10-28 §2.4.9): 첫 "#" 뒤의 문자열과 <code>id</code> 가 같거나
     * <code>name</code> 이 대소문자 무시로 같은 트리 순서의 첫 <code>map</code> 요소. "#" 이 없거나 맨 끝에 있는 값은 맵을 가리키지 않는다.
     *
     * @param doc    the document / 문서
     * @param useMap the value of the <code>usemap</code> attribute, may be null / <code>usemap</code> 속성 값. null 일 수 있다
     * @return the map element, or null / map 요소. 없으면 null
     */
    public static Element findMap(Document doc, String useMap) {
        if (doc == null || useMap == null) {
            return null;
        }
        int hash = useMap.indexOf('#');
        // "#" 이 없거나 맨 끝에 있으면 맵을 가리키지 않는다.
        if (hash < 0 || hash == useMap.length() - 1) {
            return null;
        }
        String s = useMap.substring(hash + 1);
        ElementCollection maps = byTagName(doc, "map");
        // map 요소를 트리 순서로 보며 id 가 같거나 name 이 대소문자 무시로 같은 첫 요소를 찾는다.
        for (int i = 0; i < maps.getLength(); i++) {
            Element m = maps.item(i);
            if (s.equals(m.getAttribute("id")) || (m.hasAttribute("name") && s.equalsIgnoreCase(m.getAttribute("name")))) {
                return m;
            }
        }
        return null;
    }

    /**
     * Creates the live collection of the <code>area</code> elements that are descendants of a <code>map</code>
     * (<code>HTMLMapElement.areas</code>).
     * <p>
     * <code>map</code> 의 후손인 <code>area</code> 요소의 라이브 컬렉션을 만든다(<code>HTMLMapElement.areas</code>).
     *
     * @param map the map element / map 요소
     * @return the collection / 컬렉션
     */
    public static ElementCollection mapAreas(Node map) {
        return new ElementCollection(map) {
            /**
             * Matches the HTML <code>area</code> elements.
             * <p>
             * HTML <code>area</code> 요소와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                return Namespaces.HTML.equals(e.getNamespaceURI()) && "area".equals(e.getLocalName());
            }
        };
    }

    /**
     * Creates the live collection of the <code>img</code> and <code>object</code> elements of the document that use a map
     * (<code>HTMLMapElement.images</code>): those with a <code>usemap</code> that names this map by the rules of {@link #findMap}.
     * <p>
     * 문서에서 맵을 쓰는 <code>img</code> 와 <code>object</code> 요소의 라이브 컬렉션을 만든다(<code>HTMLMapElement.images</code>): {@link #findMap} 의 규칙으로 이 맵을
     * 가리키는 <code>usemap</code> 을 가진 요소.
     *
     * @param map the map element / map 요소
     * @return the collection / 컬렉션
     */
    public static ElementCollection mapImages(final Element map) {
        return new ElementCollection(map.document()) {
            /**
             * Matches the images that belong to the map.
             * <p>
             * 맵에 속한 이미지와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                String name = e.getLocalName();
                return Namespaces.HTML.equals(e.getNamespaceURI()) && ("img".equals(name) || "object".equals(name)) && e.hasAttribute("usemap")
                        && findMap(e.document(), e.getAttribute("usemap")) == map;
            }
        };
    }

    /**
     * Returns the names under which an element is a named property of an HTML document (HTML5 REC 2014-10-28 section 5.3, named access on
     * the <code>Document</code> object, https://www.w3.org/TR/2014/REC-html5-20141028/dom.html#dom-document-nameditem, retrieved
     * 2026-11-17): the <code>name</code> of an <code>applet</code>, <code>embed</code>, <code>form</code>, <code>img</code> or
     * <code>object</code> element, the <code>id</code> of an <code>applet</code> or <code>object</code> element, and the <code>id</code> of
     * an <code>img</code> element that also has a name. An <code>embed</code> or <code>object</code> element inside an
     * <code>object</code> element is not exposed. The names are not empty and appear in the order name, id.
     * <p>
     * 요소가 HTML 문서의 이름 있는 프로퍼티가 되는 이름들을 반환한다(HTML5 REC 2014-10-28 §5.3, <code>Document</code> 객체의 이름 접근): <code>applet</code>, <code>embed</code>,
     * <code>form</code>, <code>img</code>, <code>object</code> 요소의 <code>name</code>, <code>applet</code>, <code>object</code> 요소의 <code>id</code>, 이름도 있는
     * <code>img</code> 요소의 <code>id</code>. <code>object</code> 요소 안의 <code>embed</code>, <code>object</code> 요소는 노출되지 않는다. 이름은 비어 있지 않으며 name, id 순서이다.
     *
     * @param e the element / 요소
     * @return the names, empty for most elements / 이름. 대부분의 요소는 비어 있다
     */
    public static List<String> documentNames(Element e) {
        List<String> out = new ArrayList<String>(2);
        if (!Namespaces.HTML.equals(e.getNamespaceURI())) {
            return out;
        }
        String tag = e.getLocalName();
        boolean named = "applet".equals(tag) || "embed".equals(tag) || "form".equals(tag) || "img".equals(tag) || "object".equals(tag);
        if (!named) {
            return out;
        }
        // object 요소 안에 있는 embed 와 object 는 노출되지 않는다.
        if ("embed".equals(tag) || "object".equals(tag)) {
            for (Node p = e.getParentNode(); p instanceof Element; p = p.getParentNode()) {
                if ("object".equals(((Element) p).getLocalName()) && Namespaces.HTML.equals(((Element) p).getNamespaceURI())) {
                    return out;
                }
            }
        }
        String name = e.getAttribute("name");
        String id = e.getAttribute("id");
        boolean hasName = name != null && name.length() > 0;
        boolean hasId = id != null && id.length() > 0;
        if (hasName) {
            out.add(name);
        }
        if (hasId && ("applet".equals(tag) || "object".equals(tag) || ("img".equals(tag) && hasName))) {
            out.add(id);
        }
        return out;
    }

    /**
     * Returns the names under which an element is a named property of the <code>Window</code> object (HTML5 REC 2014-10-28 section 5.1.4,
     * named access on the Window object, https://www.w3.org/TR/2014/REC-html5-20141028/browsers.html#named-access-on-the-window-object,
     * retrieved 2026-11-17): the <code>id</code> of every HTML element that has one, and the <code>name</code> of an <code>a</code>,
     * <code>applet</code>, <code>area</code>, <code>embed</code>, <code>form</code>, <code>frameset</code>, <code>img</code> or
     * <code>object</code> element. The names are not empty and appear in the order name, id.
     * <p>
     * 요소가 <code>Window</code> 객체의 이름 있는 프로퍼티가 되는 이름들을 반환한다(HTML5 REC 2014-10-28 §5.1.4, Window 객체의 이름 접근): id 가 있는 모든 HTML 요소의
     * <code>id</code> 와 <code>a</code>, <code>applet</code>, <code>area</code>, <code>embed</code>, <code>form</code>, <code>frameset</code>, <code>img</code>,
     * <code>object</code> 요소의 <code>name</code>. 이름은 비어 있지 않으며 name, id 순서이다.
     *
     * @param e the element / 요소
     * @return the names, empty for an element that has none / 이름. 없는 요소는 비어 있다
     */
    public static List<String> windowNames(Element e) {
        List<String> out = new ArrayList<String>(2);
        if (!Namespaces.HTML.equals(e.getNamespaceURI())) {
            return out;
        }
        String tag = e.getLocalName();
        boolean named = "a".equals(tag) || "applet".equals(tag) || "area".equals(tag) || "embed".equals(tag) || "form".equals(tag)
                || "frameset".equals(tag) || "img".equals(tag) || "object".equals(tag);
        String name = e.getAttribute("name");
        String id = e.getAttribute("id");
        if (named && name != null && name.length() > 0) {
            out.add(name);
        }
        if (id != null && id.length() > 0) {
            out.add(id);
        }
        return out;
    }

    /**
     * Creates the live collection of the elements of a document that are named properties of the <code>Window</code> under a name (see
     * {@link #windowNames}).
     * <p>
     * 이름 하나로 <code>Window</code> 의 이름 있는 프로퍼티가 되는 문서의 요소의 라이브 컬렉션을 만든다({@link #windowNames} 참조).
     *
     * @param doc  the document / 문서
     * @param name the name / 이름
     * @return the collection / 컬렉션
     */
    public static ElementCollection windowNamed(Document doc, final String name) {
        return new ElementCollection(doc) {
            /**
             * Matches the elements that the window exposes under the name.
             * <p>
             * window 가 그 이름으로 노출하는 요소와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                return windowNames(e).contains(name);
            }
        };
    }

    /**
     * Creates the live collection of the elements of a document that are named properties of the document under a name (see
     * {@link #documentNames}).
     * <p>
     * 이름 하나로 문서의 이름 있는 프로퍼티가 되는 문서의 요소의 라이브 컬렉션을 만든다({@link #documentNames} 참조).
     *
     * @param doc  the document / 문서
     * @param name the name / 이름
     * @return the collection / 컬렉션
     */
    public static ElementCollection documentNamed(Document doc, final String name) {
        return new ElementCollection(doc) {
            /**
             * Matches the elements that the document exposes under the name.
             * <p>
             * 문서가 그 이름으로 노출하는 요소와 일치한다.
             *
             * @param e candidate element / 후보 요소
             * @return true if it matches / 일치하면 true
             */
            boolean matches(Element e) {
                return documentNames(e).contains(name);
            }
        };
    }

    /**
     * Converts a collection to a plain element list (convenience for callers that need a snapshot).
     * <p>
     * 컬렉션을 일반 요소 목록으로 바꾼다(스냅샷이 필요한 호출자를 위한 편의).
     *
     * @param c collection / 컬렉션
     * @return copy of the elements / 요소의 복사본
     */
    public static List<Element> snapshot(ElementCollection c) {
        return new ArrayList<Element>(c.toList());
    }
}
