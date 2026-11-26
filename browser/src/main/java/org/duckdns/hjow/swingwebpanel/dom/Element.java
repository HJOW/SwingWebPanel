package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.List;

/**
 * An element: a node with a namespace, a name, attributes and children. The names follow the DOM Standard: the tag name of an element
 * in the HTML namespace of an HTML document is upper case and attribute names of such elements are matched in lower case. Attribute
 * changes are reported to the document's listeners; the HTML layer overrides {@link #attributeChanged(String, String, String, String)} in its
 * element classes (created by the {@link ElementFactory}) to react to changes.
 * <p>
 * 요소: 네임스페이스, 이름, 속성, 자식을 가진 노드. 이름은 DOM 표준을 따른다: HTML 문서의 HTML 네임스페이스 요소는 태그 이름이 대문자이고
 * 그런 요소의 속성 이름은 소문자로 맞춰 비교한다. 속성 변경은 문서의 리스너에 보고된다. HTML 계층은 {@link ElementFactory} 가 만드는 요소
 * 클래스에서 {@link #attributeChanged(String, String, String, String)} 를 재정의해 변경에 반응한다.
 */
public class Element extends Node {

    /**
     * Namespace URI or null.
     * <p>
     * 네임스페이스 URI 또는 null.
     */
    private final String namespaceURI;

    /**
     * Prefix or null.
     * <p>
     * 접두사 또는 null.
     */
    private final String prefix;

    /**
     * Local name.
     * <p>
     * 로컬 이름.
     */
    private final String localName;

    /**
     * Attributes in the order they were added.
     * <p>
     * 추가된 순서의 속성.
     */
    private final List<Attr> attributes = new ArrayList<Attr>(4);

    /**
     * Cached class list view.
     * <p>
     * 캐시된 클래스 목록 뷰.
     */
    private ClassList classList;

    /**
     * The token lists of other attributes that were asked for (null until the first), by attribute name.
     * <p>
     * 요청된 다른 속성의 토큰 목록(처음 요청 전에는 null). 속성 이름별.
     */
    private java.util.Map<String, ClassList> tokenLists;

    /**
     * Cached attribute map view.
     * <p>
     * 캐시된 속성 맵 뷰.
     */
    private NamedAttributeMap attributeMap;

    /**
     * Cached live collection of the element children.
     * <p>
     * 캐시된 요소 자식 라이브 컬렉션.
     */
    private ElementCollection childElements;

    /**
     * Template contents owned by this element, outside its ordinary child tree. Keeping them here avoids a static map whose
     * values retain the owner document and, through it, the map keys. Access only on the document thread.
     * <p>
     * 일반 자식 트리 밖에서 이 요소가 소유하는 template 내용. 전역 표의 값이 소유 문서를 거쳐 키를 붙잡는 일을 피한다. 문서 스레드에서만 접근한다.
     */
    private DocumentFragment templateContents;

    /**
     * Returns the template fragment installed by the HTML layer, or null when none was installed. It is not a child node.
     * <p>
     * HTML 계층이 설치한 template 내용 조각을 반환한다. 아직 없으면 null이며 일반 자식 노드가 아니다.
     * @return installed contents or null / 설치된 내용 또는 null
     */
    public final DocumentFragment getTemplateContents() {
        return templateContents;
    }

    /**
     * Installs template contents for parser integration. The fragment must belong to this document; null removes it.
     * Call only on the document thread. Ordinary elements must not use this parser service.
     * <p>
     * 파서 연결용 template 내용을 설치한다. 조각은 같은 문서 소유여야 하며 null은 제거한다. 문서 스레드에서만 호출하고 일반 요소에는 쓰지 않는다.
     * @param contents contents fragment, or null / 내용 조각 또는 null
     * @throws IllegalArgumentException for a non-template element or a foreign fragment / template이 아니거나 다른 문서의 조각이면
     */
    public final void setTemplateContents(DocumentFragment contents) {
        if (!Namespaces.HTML.equals(namespaceURI) || !"template".equals(localName)
                || (contents != null && contents.getOwnerDocument() != doc)) {
            throw new IllegalArgumentException("template contents must belong to the template's document");
        }
        templateContents = contents;
    }

    /**
     * Creates an element. Elements are normally created through {@link Document#createElementNS(String, String)}.
     * <p>
     * 요소를 만든다. 요소는 보통 {@link Document#createElementNS(String, String)} 로 만든다.
     *
     * @param owner        owner document / 소유 문서
     * @param namespaceURI namespace or null / 네임스페이스 또는 null
     * @param prefix       prefix or null / 접두사 또는 null
     * @param localName    local name / 로컬 이름
     */
    protected Element(Document owner, String namespaceURI, String prefix, String localName) {
        super(owner);
        this.namespaceURI = namespaceURI;
        this.prefix = prefix;
        this.localName = localName;
    }

    /**
     * Returns {@link Node#ELEMENT_NODE}.
     * <p>
     * {@link Node#ELEMENT_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return ELEMENT_NODE;
    }

    /**
     * Returns the tag name as node name.
     * <p>
     * 태그 이름을 노드 이름으로 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return getTagName();
    }

    /**
     * Returns the namespace URI.
     * <p>
     * 네임스페이스 URI 를 반환한다.
     *
     * @return namespace or null / 네임스페이스 또는 null
     */
    public String getNamespaceURI() {
        return namespaceURI;
    }

    /**
     * Returns the prefix.
     * <p>
     * 접두사를 반환한다.
     *
     * @return prefix or null / 접두사 또는 null
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the local name.
     * <p>
     * 로컬 이름을 반환한다.
     *
     * @return local name / 로컬 이름
     */
    public String getLocalName() {
        return localName;
    }

    /**
     * Returns the qualified name without case conversion (prefix, colon and local name).
     * <p>
     * 대소문자 변환 없이 정규화된 이름(접두사, 콜론, 로컬 이름)을 반환한다.
     *
     * @return qualified name / 정규화된 이름
     */
    public String getTagNameQualified() {
        return prefix == null ? localName : prefix + ":" + localName;
    }

    /**
     * Returns the tag name: the qualified name, in upper case for HTML-namespace elements of HTML documents.
     * <p>
     * 태그 이름을 반환한다: 정규화된 이름이며 HTML 문서의 HTML 네임스페이스 요소는 대문자이다.
     *
     * @return tag name / 태그 이름
     */
    public String getTagName() {
        String q = getTagNameQualified();
        return Namespaces.HTML.equals(namespaceURI) && doc.isHtmlDocument() ? Namespaces.asciiUpper(q) : q;
    }

    /**
     * Tells whether attribute names of this element are matched in lower case (HTML element in an HTML document).
     * <p>
     * 이 요소의 속성 이름을 소문자로 맞춰 비교하는지 알려 준다(HTML 문서의 HTML 요소).
     *
     * @return true for lower-case matching / 소문자로 비교하면 true
     */
    private boolean lowerCaseNames() {
        return Namespaces.HTML.equals(namespaceURI) && doc.isHtmlDocument();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 속성
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the number of attributes.
     * <p>
     * 속성의 수를 반환한다.
     *
     * @return count / 개수
     */
    final int attributeCount() {
        return attributes.size();
    }

    /**
     * Returns the attribute at an index.
     * <p>
     * 위치의 속성을 반환한다.
     *
     * @param index index / 위치
     * @return attribute or null / 속성 또는 null
     */
    final Attr attributeAt(int index) {
        return index >= 0 && index < attributes.size() ? attributes.get(index) : null;
    }

    /**
     * Returns the attribute object with a qualified name (matched in lower case for HTML elements of HTML documents).
     * <p>
     * 정규화된 이름의 속성 객체를 반환한다(HTML 문서의 HTML 요소는 소문자로 맞춰 비교).
     *
     * @param qualifiedName attribute name / 속성 이름
     * @return attribute or null / 속성 또는 null
     */
    public Attr getAttributeNode(String qualifiedName) {
        String n = lowerCaseNames() ? Namespaces.asciiLower(qualifiedName) : qualifiedName;
        // 속성 목록에서 이름이 같은 첫 속성을 찾는다.
        for (int i = 0; i < attributes.size(); i++) {
            Attr a = attributes.get(i);
            if (a.getName().equals(n)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Returns the attribute object with a namespace and local name.
     * <p>
     * 네임스페이스와 로컬 이름의 속성 객체를 반환한다.
     *
     * @param namespace namespace, null or empty for none / 네임스페이스. null 이나 빈 문자열이면 없음
     * @param local     local name / 로컬 이름
     * @return attribute or null / 속성 또는 null
     */
    public Attr getAttributeNodeNS(String namespace, String local) {
        String ns = namespace != null && namespace.length() == 0 ? null : namespace;
        // 속성 목록에서 네임스페이스와 로컬 이름이 같은 속성을 찾는다.
        for (int i = 0; i < attributes.size(); i++) {
            Attr a = attributes.get(i);
            if (a.getLocalName().equals(local) && (ns == null ? a.getNamespaceURI() == null : ns.equals(a.getNamespaceURI()))) {
                return a;
            }
        }
        return null;
    }

    /**
     * Returns the value of an attribute.
     * <p>
     * 속성의 값을 반환한다.
     *
     * @param qualifiedName attribute name / 속성 이름
     * @return value or null if absent / 값. 없으면 null
     */
    public String getAttribute(String qualifiedName) {
        Attr a = getAttributeNode(qualifiedName);
        return a == null ? null : a.getValue();
    }

    /**
     * Returns the value of an attribute with a namespace.
     * <p>
     * 네임스페이스가 있는 속성의 값을 반환한다.
     *
     * @param namespace namespace or null / 네임스페이스 또는 null
     * @param local     local name / 로컬 이름
     * @return value or null if absent / 값. 없으면 null
     */
    public String getAttributeNS(String namespace, String local) {
        Attr a = getAttributeNodeNS(namespace, local);
        return a == null ? null : a.getValue();
    }

    /**
     * Tells whether an attribute exists.
     * <p>
     * 속성이 있는지 알려 준다.
     *
     * @param qualifiedName attribute name / 속성 이름
     * @return true if it exists / 있으면 true
     */
    public boolean hasAttribute(String qualifiedName) {
        return getAttributeNode(qualifiedName) != null;
    }

    /**
     * Tells whether the user agent supplies controls for this HTML audio element.
     * <p>
     * 사용자 에이전트가 이 HTML 오디오 요소에 컨트롤을 제공하는지 알려 준다.
     * @return true for explicit controls or unscripted audio / 컨트롤을 명시했거나 스크립트가 없는 오디오이면 true
     */
    public boolean hasAudioControls() {
        if (!"audio".equals(getLocalName()) || !Namespaces.HTML.equals(getNamespaceURI())) {
            return false;
        }
        Document owner = getOwnerDocument();
        return hasAttribute("controls") || owner != null && !owner.isScriptingEnabled();
    }

    /**
     * Tells whether an attribute with a namespace exists.
     * <p>
     * 네임스페이스가 있는 속성이 있는지 알려 준다.
     *
     * @param namespace namespace or null / 네임스페이스 또는 null
     * @param local     local name / 로컬 이름
     * @return true if it exists / 있으면 true
     */
    public boolean hasAttributeNS(String namespace, String local) {
        return getAttributeNodeNS(namespace, local) != null;
    }

    /**
     * Tells whether the element has any attribute.
     * <p>
     * 요소에 속성이 하나라도 있는지 알려 준다.
     *
     * @return true if it has attributes / 속성이 있으면 true
     */
    public boolean hasAttributes() {
        return !attributes.isEmpty();
    }

    /**
     * Returns the qualified names of all attributes.
     * <p>
     * 모든 속성의 정규화된 이름을 반환한다.
     *
     * @return names in order / 순서대로의 이름
     */
    public List<String> getAttributeNames() {
        List<String> names = new ArrayList<String>(attributes.size());
        // 속성의 이름을 순서대로 모은다.
        for (int i = 0; i < attributes.size(); i++) {
            names.add(attributes.get(i).getName());
        }
        return names;
    }

    /**
     * Sets an attribute (<code>setAttribute</code>): changes the value if it exists, otherwise adds it.
     * <p>
     * 속성을 지정한다(<code>setAttribute</code>): 있으면 값을 바꾸고 없으면 추가한다.
     *
     * @param qualifiedName attribute name; lower-cased for HTML elements of HTML documents / 속성 이름. HTML 문서의 HTML 요소는 소문자로 바꾼다
     * @param value         value, null is treated as empty / 값(null 은 빈 문자열)
     * @throws DomException InvalidCharacterError if the name is not a valid name / 이름이 올바르지 않으면 InvalidCharacterError
     */
    public void setAttribute(String qualifiedName, String value) {
        if (!Namespaces.isValidName(qualifiedName)) {
            throw DomException.invalidCharacter("invalid attribute name: " + qualifiedName);
        }
        String n = lowerCaseNames() ? Namespaces.asciiLower(qualifiedName) : qualifiedName;
        String v = value == null ? "" : value;
        Attr existing = getAttributeNode(n);
        if (existing != null) {
            changeAttributeValue(existing, v);
        } else {
            appendAttribute(new Attr(doc, null, null, n, v));
        }
    }

    /**
     * Sets an attribute without checking that the name is a valid XML Name. The HTML parser uses this because the HTML syntax allows
     * attribute names such as <code>@click</code> or <code>:class</code> that <code>setAttribute</code> rejects (they stay readable with
     * <code>getAttribute</code>, as in browsers). Only an empty name is refused.
     * <p>
     * 이름이 올바른 XML Name 인지 검사하지 않고 속성을 지정한다. HTML 구문은 <code>setAttribute</code> 가 거부하는 <code>@click</code>,
     * <code>:class</code> 같은 속성 이름을 허용하므로 HTML 파서가 쓴다(브라우저처럼 <code>getAttribute</code> 로는 읽을 수 있다). 빈 이름만 거부한다.
     *
     * @param qualifiedName attribute name; lower-cased for HTML elements of HTML documents / 속성 이름. HTML 문서의 HTML 요소는 소문자로 바꾼다
     * @param value         value, null is treated as empty / 값(null 은 빈 문자열)
     * @throws DomException InvalidCharacterError if the name is empty / 이름이 비어 있으면 InvalidCharacterError
     */
    public void setAttributeUnchecked(String qualifiedName, String value) {
        if (qualifiedName == null || qualifiedName.length() == 0) {
            throw DomException.invalidCharacter("empty attribute name");
        }
        String n = lowerCaseNames() ? Namespaces.asciiLower(qualifiedName) : qualifiedName;
        String v = value == null ? "" : value;
        Attr existing = getAttributeNode(n);
        if (existing != null) {
            changeAttributeValue(existing, v);
        } else {
            appendAttribute(new Attr(doc, null, null, n, v));
        }
    }

    /**
     * Sets an attribute with a namespace (<code>setAttributeNS</code>).
     * <p>
     * 네임스페이스가 있는 속성을 지정한다(<code>setAttributeNS</code>).
     *
     * @param namespace     namespace or null / 네임스페이스 또는 null
     * @param qualifiedName qualified name / 정규화된 이름
     * @param value         value / 값
     * @throws DomException InvalidCharacterError or NamespaceError / InvalidCharacterError 또는 NamespaceError
     */
    public void setAttributeNS(String namespace, String qualifiedName, String value) {
        Namespaces.QualifiedName q = Namespaces.validateAndExtract(namespace, qualifiedName);
        String v = value == null ? "" : value;
        Attr existing = getAttributeNodeNS(q.namespace, q.localName);
        if (existing != null) {
            changeAttributeValue(existing, v);
        } else {
            appendAttribute(new Attr(doc, q.namespace, q.prefix, q.localName, v));
        }
    }

    /**
     * Adds an attribute that does not exist yet and reports the change.
     * <p>
     * 아직 없는 속성을 추가하고 변경을 보고한다.
     *
     * @param a new attribute / 새 속성
     */
    private void appendAttribute(Attr a) {
        a.ownerElement = this;
        attributes.add(a);
        doc.bumpVersion();
        doc.recordAttribute(this, a.getLocalName(), a.getNamespaceURI(), null);
        attributeChanged(a.getLocalName(), a.getNamespaceURI(), null, a.getValue());
    }

    /**
     * Changes the value of an attribute of this element and reports it.
     * <p>
     * 이 요소의 속성 값을 바꾸고 보고한다.
     *
     * @param a     attribute of this element / 이 요소의 속성
     * @param value new value / 새 값
     */
    final void changeAttributeValue(Attr a, String value) {
        String old = a.getValue();
        a.storeValue(value);
        doc.bumpVersion();
        doc.recordAttribute(this, a.getLocalName(), a.getNamespaceURI(), old);
        attributeChanged(a.getLocalName(), a.getNamespaceURI(), old, value);
    }

    /**
     * Removes an attribute by qualified name; nothing happens if it does not exist.
     * <p>
     * 정규화된 이름으로 속성을 제거한다. 없으면 아무 일도 없다.
     *
     * @param qualifiedName attribute name / 속성 이름
     */
    public void removeAttribute(String qualifiedName) {
        Attr a = getAttributeNode(qualifiedName);
        if (a != null) {
            removeAttributeNode(a);
        }
    }

    /**
     * Removes an attribute by namespace and local name; nothing happens if it does not exist.
     * <p>
     * 네임스페이스와 로컬 이름으로 속성을 제거한다. 없으면 아무 일도 없다.
     *
     * @param namespace namespace or null / 네임스페이스 또는 null
     * @param local     local name / 로컬 이름
     */
    public void removeAttributeNS(String namespace, String local) {
        Attr a = getAttributeNodeNS(namespace, local);
        if (a != null) {
            removeAttributeNode(a);
        }
    }

    /**
     * Toggles an attribute: adds it with an empty value or removes it; with force the presence is forced.
     * <p>
     * 속성을 토글한다: 빈 값으로 추가하거나 제거한다. force 가 있으면 있음/없음을 강제한다.
     *
     * @param qualifiedName attribute name / 속성 이름
     * @param force         TRUE to force presence, FALSE to force absence, null to toggle / TRUE 면 있게, FALSE 면 없게, null 이면 토글
     * @return true if the attribute is present afterwards / 호출 뒤에 속성이 있으면 true
     */
    public boolean toggleAttribute(String qualifiedName, Boolean force) {
        if (!Namespaces.isValidName(qualifiedName)) {
            throw DomException.invalidCharacter("invalid attribute name: " + qualifiedName);
        }
        Attr a = getAttributeNode(qualifiedName);
        if (a == null) {
            if (force == null || force.booleanValue()) {
                setAttribute(qualifiedName, "");
                return true;
            }
            return false;
        }
        if (force == null || !force.booleanValue()) {
            removeAttributeNode(a);
            return false;
        }
        return true;
    }

    /**
     * Sets an attribute object (<code>setAttributeNode</code>), replacing one with the same namespace and local name.
     * <p>
     * 속성 객체를 지정한다(<code>setAttributeNode</code>). 네임스페이스와 로컬 이름이 같은 것은 교체한다.
     *
     * @param attr attribute, not null / 속성(null 불가)
     * @return the replaced attribute or null / 교체된 속성 또는 null
     * @throws DomException InUseAttributeError if it belongs to another element, WrongDocumentError if it was created by another document
     */
    public Attr setAttributeNode(Attr attr) {
        if (attr == null) {
            throw new IllegalArgumentException("attr must not be null");
        }
        if (attr.ownerElement != null && attr.ownerElement != this) {
            throw DomException.inUseAttribute("the attribute belongs to another element");
        }
        if (attr.ownerElement == this) {
            return attr;
        }
        Attr old = getAttributeNodeNS(attr.getNamespaceURI(), attr.getLocalName());
        if (old != null) {
            int i = attributes.indexOf(old);
            old.ownerElement = null;
            attr.ownerElement = this;
            attributes.set(i, attr);
            doc.bumpVersion();
            doc.recordAttribute(this, attr.getLocalName(), attr.getNamespaceURI(), old.getValue());
            attributeChanged(attr.getLocalName(), attr.getNamespaceURI(), old.getValue(), attr.getValue());
            return old;
        }
        appendAttribute(attr);
        return null;
    }

    /**
     * Removes an attribute object of this element.
     * <p>
     * 이 요소의 속성 객체를 제거한다.
     *
     * @param attr attribute of this element / 이 요소의 속성
     * @return the removed attribute / 제거한 속성
     * @throws DomException NotFoundError if it is not an attribute of this element / 이 요소의 속성이 아니면 NotFoundError
     */
    public Attr removeAttributeNode(Attr attr) {
        int i = attributes.indexOf(attr);
        if (i < 0 || attr.ownerElement != this) {
            throw DomException.notFound("the attribute does not belong to this element");
        }
        String old = attr.getValue();
        attributes.remove(i);
        attr.ownerElement = null;
        doc.bumpVersion();
        doc.recordAttribute(this, attr.getLocalName(), attr.getNamespaceURI(), old);
        attributeChanged(attr.getLocalName(), attr.getNamespaceURI(), old, null);
        return attr;
    }


    /**
     * Moves the attributes to the new owner document.
     * <p>
     * 속성을 새 소유 문서로 옮긴다.
     *
     * @param oldDocument previous owner / 이전 소유 문서
     */
    protected void adoptedFrom(Document oldDocument) {
        // 요소가 가진 모든 속성의 소유 문서도 함께 바꾼다.
        for (int i = 0; i < attributes.size(); i++) {
            attributes.get(i).setOwnerDocument(doc);
        }
        // template 내용은 일반 후손 순회에 포함되지 않으므로 별도로 같은 문서에 옮긴다.
        if (templateContents != null) {
            doc.adoptInternal(templateContents);
        }
    }
    /**
     * Hook called after an attribute was added (old value null), changed or removed (new value null). The default does nothing; the
     * HTML element classes override it.
     * <p>
     * 속성이 추가(이전 값 null), 변경, 제거(새 값 null)된 뒤 호출되는 확장 지점. 기본은 아무것도 하지 않으며 HTML 요소 클래스가 재정의한다.
     *
     * @param local     local name of the attribute / 속성의 로컬 이름
     * @param namespace namespace of the attribute or null / 속성의 네임스페이스 또는 null
     * @param oldValue  previous value or null / 이전 값 또는 null
     * @param newValue  new value or null / 새 값 또는 null
     */
    protected void attributeChanged(String local, String namespace, String oldValue, String newValue) {
        // 기본: 속성 변화에 반응하지 않는다.
    }

    /**
     * Returns the live view of the attributes.
     * <p>
     * 속성의 라이브 뷰를 반환한다.
     *
     * @return attribute map / 속성 맵
     */
    public NamedAttributeMap getAttributes() {
        if (attributeMap == null) {
            attributeMap = new NamedAttributeMap(this);
        }
        return attributeMap;
    }

    /**
     * Returns the value of the id attribute (empty if absent).
     * <p>
     * id 속성의 값을 반환한다(없으면 빈 문자열).
     *
     * @return id / id
     */
    public String getId() {
        Attr a = getAttributeNodeNS(null, "id");
        return a == null ? "" : a.getValue();
    }

    /**
     * Sets the id attribute.
     * <p>
     * id 속성을 지정한다.
     *
     * @param id new id / 새 id
     */
    public void setId(String id) {
        setAttribute("id", id);
    }

    /**
     * Returns the value of the class attribute (empty if absent).
     * <p>
     * class 속성의 값을 반환한다(없으면 빈 문자열).
     *
     * @return class names text / 클래스 이름 문자열
     */
    public String getClassName() {
        Attr a = getAttributeNodeNS(null, "class");
        return a == null ? "" : a.getValue();
    }

    /**
     * Sets the class attribute.
     * <p>
     * class 속성을 지정한다.
     *
     * @param value new value / 새 값
     */
    public void setClassName(String value) {
        setAttribute("class", value);
    }

    /**
     * Returns the live token list of the class attribute; the same object is returned every time.
     * <p>
     * class 속성의 라이브 토큰 목록을 반환한다. 매번 같은 객체이다.
     *
     * @return class list / 클래스 목록
     */
    public ClassList getClassList() {
        if (classList == null) {
            classList = new ClassList(this);
        }
        return classList;
    }

    /**
     * Returns the live token list of another attribute (<code>relList</code> of <code>rel</code>, <code>sandbox</code>, and so on); the same
     * object is returned every time for the same attribute name.
     * <p>
     * 다른 속성의 라이브 토큰 목록을 반환한다(<code>rel</code> 의 <code>relList</code>, <code>sandbox</code> 등). 같은 속성 이름에는 매번 같은 객체이다.
     *
     * @param attribute the attribute name / 속성 이름
     * @return the token list / 토큰 목록
     */
    public ClassList getTokenList(String attribute) {
        if ("class".equals(attribute)) {
            return getClassList();
        }
        if (tokenLists == null) {
            tokenLists = new java.util.HashMap<String, ClassList>();
        }
        ClassList list = tokenLists.get(attribute);
        if (list == null) {
            list = new ClassList(this, attribute);
            tokenLists.put(attribute, list);
        }
        return list;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 조회와 선택자
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the live collection of the element children (<code>children</code>); the same object is returned every time.
     * <p>
     * 요소 자식의 라이브 컬렉션을 반환한다(<code>children</code>). 매번 같은 객체이다.
     *
     * @return collection / 컬렉션
     */
    public ElementCollection getChildren() {
        if (childElements == null) {
            childElements = ElementCollections.children(this);
        }
        return childElements;
    }

    /**
     * Returns the live collection of descendants with a tag name.
     * <p>
     * 태그 이름을 가진 후손의 라이브 컬렉션을 반환한다.
     *
     * @param qualifiedName name or * / 이름 또는 *
     * @return collection / 컬렉션
     */
    public ElementCollection getElementsByTagName(String qualifiedName) {
        return ElementCollections.byTagName(this, qualifiedName);
    }

    /**
     * Returns the live collection of descendants with a namespace and local name.
     * <p>
     * 네임스페이스와 로컬 이름을 가진 후손의 라이브 컬렉션을 반환한다.
     *
     * @param namespace namespace or * / 네임스페이스 또는 *
     * @param localName local name or * / 로컬 이름 또는 *
     * @return collection / 컬렉션
     */
    public ElementCollection getElementsByTagNameNS(String namespace, String localName) {
        return ElementCollections.byTagNameNS(this, namespace, localName);
    }

    /**
     * Returns the live collection of descendants that have all the class names.
     * <p>
     * 모든 클래스 이름을 가진 후손의 라이브 컬렉션을 반환한다.
     *
     * @param classNames space separated class names / 공백으로 구분한 클래스 이름
     * @return collection / 컬렉션
     */
    public ElementCollection getElementsByClassName(String classNames) {
        return ElementCollections.byClassName(this, classNames);
    }

    /**
     * Returns the first descendant that matches a selector list.
     * <p>
     * 선택자 목록에 일치하는 첫 후손을 반환한다.
     *
     * @param selectors selector list / 선택자 목록
     * @return element or null / 요소 또는 null
     * @throws DomException SyntaxError for invalid selectors, NotSupportedError if no selector engine is installed / 잘못된 선택자는 SyntaxError, 선택자 엔진이 없으면 NotSupportedError
     */
    public Element querySelector(String selectors) {
        return doc.requireSelectorEngine().querySelector(this, selectors);
    }

    /**
     * Returns all descendants that match a selector list as a static list.
     * <p>
     * 선택자 목록에 일치하는 모든 후손을 정적 목록으로 반환한다.
     *
     * @param selectors selector list / 선택자 목록
     * @return static node list / 정적 노드 목록
     * @throws DomException SyntaxError for invalid selectors, NotSupportedError if no selector engine is installed / 잘못된 선택자는 SyntaxError, 선택자 엔진이 없으면 NotSupportedError
     */
    public NodeList querySelectorAll(String selectors) {
        return new StaticNodeList(doc.requireSelectorEngine().querySelectorAll(this, selectors));
    }

    /**
     * Tells whether this element matches a selector list.
     * <p>
     * 이 요소가 선택자 목록에 일치하는지 알려 준다.
     *
     * @param selectors selector list / 선택자 목록
     * @return true if it matches / 일치하면 true
     * @throws DomException SyntaxError for invalid selectors, NotSupportedError if no selector engine is installed / 잘못된 선택자는 SyntaxError, 선택자 엔진이 없으면 NotSupportedError
     */
    public boolean matches(String selectors) {
        return doc.requireSelectorEngine().matches(this, selectors);
    }

    /**
     * Returns this element or its closest ancestor that matches a selector list.
     * <p>
     * 이 요소 또는 선택자 목록에 일치하는 가장 가까운 조상을 반환한다.
     *
     * @param selectors selector list / 선택자 목록
     * @return element or null / 요소 또는 null
     * @throws DomException SyntaxError for invalid selectors, NotSupportedError if no selector engine is installed / 잘못된 선택자는 SyntaxError, 선택자 엔진이 없으면 NotSupportedError
     */
    public Element closest(String selectors) {
        return doc.requireSelectorEngine().closest(this, selectors);
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 마크업
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the markup of the children (<code>innerHTML</code>).
     * <p>
     * 자식의 마크업을 반환한다(<code>innerHTML</code>).
     *
     * @return markup / 마크업
     * @throws DomException NotSupportedError if no markup support is installed / 마크업 지원이 없으면 NotSupportedError
     */
    public String getInnerHtml() {
        return doc.requireMarkupSupport().serialize(this, false);
    }

    /**
     * Replaces the children with the nodes parsed from markup (<code>innerHTML</code>).
     * <p>
     * 자식을 마크업에서 해석한 노드로 교체한다(<code>innerHTML</code>).
     *
     * @param markup markup text / 마크업 문자열
     * @throws DomException NotSupportedError if no markup support is installed / 마크업 지원이 없으면 NotSupportedError
     */
    public void setInnerHtml(String markup) {
        doc.requireMarkupSupport().setInnerHtml(this, markup == null ? "" : markup);
    }

    /**
     * Returns the markup of the element itself (<code>outerHTML</code>).
     * <p>
     * 요소 자신의 마크업을 반환한다(<code>outerHTML</code>).
     *
     * @return markup / 마크업
     * @throws DomException NotSupportedError if no markup support is installed / 마크업 지원이 없으면 NotSupportedError
     */
    public String getOuterHtml() {
        return doc.requireMarkupSupport().serialize(this, true);
    }

    /**
     * Replaces the element with the nodes parsed from markup (<code>outerHTML</code>).
     * <p>
     * 요소를 마크업에서 해석한 노드로 교체한다(<code>outerHTML</code>).
     *
     * @param markup markup text / 마크업 문자열
     * @throws DomException NoModificationAllowedError if the parent is a document, NotSupportedError if no markup support is installed
     */
    public void setOuterHtml(String markup) {
        Node p = parent;
        if (p == null) {
            return;
        }
        if (p.getNodeType() == DOCUMENT_NODE) {
            throw new DomException("NoModificationAllowedError", DomException.NO_MODIFICATION_ALLOWED_ERR,
                    "the element cannot be replaced in a document node");
        }
        Element context = p instanceof Element ? (Element) p : doc.createElement("body");
        DocumentFragment f = doc.requireMarkupSupport().parseFragment(context, markup == null ? "" : markup);
        p.replaceChild(f, this);
    }

    /**
     * Parses markup and inserts the nodes at a position relative to this element (<code>insertAdjacentHTML</code>).
     * <p>
     * 마크업을 해석하여 이 요소를 기준으로 한 위치에 노드를 삽입한다(<code>insertAdjacentHTML</code>).
     *
     * @param position <code>beforebegin</code>, <code>afterbegin</code>, <code>beforeend</code> or <code>afterend</code> (case-insensitive) / 위치 이름(대소문자 무시)
     * @param markup   markup text / 마크업 문자열
     * @throws DomException SyntaxError for an unknown position, NoModificationAllowedError for beforebegin/afterend without a parent
     */
    public void insertAdjacentHtml(String position, String markup) {
        String pos = Namespaces.asciiLower(position);
        MarkupSupport ms = doc.requireMarkupSupport();
        String text = markup == null ? "" : markup;
        if ("beforebegin".equals(pos) || "afterend".equals(pos)) {
            if (parent == null || parent.getNodeType() == DOCUMENT_NODE) {
                throw new DomException("NoModificationAllowedError", DomException.NO_MODIFICATION_ALLOWED_ERR, "the element has no parent element");
            }
            Element ctx = parent instanceof Element ? (Element) parent : doc.createElement("body");
            DocumentFragment f = ms.parseFragment(ctx, text);
            parent.insertBefore(f, "beforebegin".equals(pos) ? this : nextSibling);
        } else if ("afterbegin".equals(pos) || "beforeend".equals(pos)) {
            DocumentFragment f = ms.parseFragment(this, text);
            insertBefore(f, "afterbegin".equals(pos) ? firstChild : null);
        } else {
            throw DomException.syntax("unknown position: " + position);
        }
    }

    /**
     * Inserts a node at a position relative to this element and returns whether it was inserted (the work of <code>insertAdjacentElement</code>
     * and <code>insertAdjacentText</code>; DOM Standard "insert adjacent").
     * <p>
     * 이 요소를 기준으로 한 위치에 노드를 삽입하고 삽입했는지 반환한다(<code>insertAdjacentElement</code>, <code>insertAdjacentText</code> 의 작업.
     * DOM 표준 "insert adjacent").
     *
     * @param position <code>beforebegin</code>, <code>afterbegin</code>, <code>beforeend</code> or <code>afterend</code> (case-insensitive) / 위치 이름(대소문자 무시)
     * @param node     the node to insert / 삽입할 노드
     * @return false if the position is <code>beforebegin</code> or <code>afterend</code> and the element has no parent / 위치가 beforebegin 이나 afterend 인데 부모가 없으면 false
     * @throws DomException SyntaxError for an unknown position, HierarchyRequestError when the node cannot be inserted there
     */
    private boolean insertAdjacentNode(String position, Node node) {
        String pos = Namespaces.asciiLower(position);
        // 위치 이름에 따라 부모와 기준 자식을 정한다.
        if ("beforebegin".equals(pos) || "afterend".equals(pos)) {
            if (parent == null) {
                return false;
            }
            parent.insertBefore(node, "beforebegin".equals(pos) ? this : nextSibling);
        } else if ("afterbegin".equals(pos)) {
            insertBefore(node, firstChild);
        } else if ("beforeend".equals(pos)) {
            appendChild(node);
        } else {
            throw DomException.syntax("unknown position: " + position);
        }
        return true;
    }

    /**
     * Inserts an element at a position relative to this element (<code>insertAdjacentElement</code>).
     * <p>
     * 이 요소를 기준으로 한 위치에 요소를 삽입한다(<code>insertAdjacentElement</code>).
     *
     * @param position <code>beforebegin</code>, <code>afterbegin</code>, <code>beforeend</code> or <code>afterend</code> (case-insensitive) / 위치 이름(대소문자 무시)
     * @param element  the element to insert / 삽입할 요소
     * @return the inserted element, or null if there was no parent for <code>beforebegin</code>/<code>afterend</code> / 삽입한 요소. beforebegin/afterend 에 부모가 없으면 null
     * @throws DomException SyntaxError for an unknown position, HierarchyRequestError when the element cannot be inserted there
     */
    public Element insertAdjacentElement(String position, Element element) {
        return insertAdjacentNode(position, element) ? element : null;
    }

    /**
     * Inserts a text node with the given data at a position relative to this element (<code>insertAdjacentText</code>).
     * <p>
     * 주어진 데이터의 텍스트 노드를 이 요소를 기준으로 한 위치에 삽입한다(<code>insertAdjacentText</code>).
     *
     * @param position <code>beforebegin</code>, <code>afterbegin</code>, <code>beforeend</code> or <code>afterend</code> (case-insensitive) / 위치 이름(대소문자 무시)
     * @param data     the text / 텍스트
     * @throws DomException SyntaxError for an unknown position, HierarchyRequestError when the text cannot be inserted there
     */
    public void insertAdjacentText(String position, String data) {
        insertAdjacentNode(position, doc.createTextNode(data == null ? "" : data));
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 복제와 비교
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a copy with the same name and attributes (no children).
     * <p>
     * 같은 이름과 속성을 가진 복사본을 만든다(자식 없음).
     *
     * @param target owner of the copy / 복사본의 소유 문서
     * @return the copy / 복사본
     */
    Node shallowCopy(Document target) {
        Element copy = target.createElementInternal(namespaceURI, prefix, localName);
        // 속성을 보고 없이 복사본에 옮긴다(복사본은 아직 아무도 볼 수 없다).
        for (int i = 0; i < attributes.size(); i++) {
            Attr a = attributes.get(i);
            Attr c = new Attr(target, a.getNamespaceURI(), a.getPrefix(), a.getLocalName(), a.getValue());
            c.ownerElement = copy;
            copy.attributes.add(c);
        }
        copyStateTo(copy);
        return copy;
    }

    /**
     * Hook for subclasses to copy their own state to a clone. The default does nothing.
     * <p>
     * 하위 클래스가 자신의 상태를 복제본에 복사하는 확장 지점. 기본은 아무것도 하지 않는다.
     *
     * @param copy the new copy / 새 복사본
     */
    protected void copyStateTo(Element copy) {
        // 기본: 복사할 추가 상태가 없다.
    }

    /**
     * Compares namespace, prefix, local name and attributes (attribute order does not matter).
     * <p>
     * 네임스페이스, 접두사, 로컬 이름, 속성을 비교한다(속성 순서는 상관없다).
     *
     * @param other element / 요소
     * @return true if equal / 같으면 true
     */
    boolean equalOwnData(Node other) {
        Element o = (Element) other;
        if (!sameString(namespaceURI, o.namespaceURI) || !sameString(prefix, o.prefix) || !localName.equals(o.localName)
                || attributes.size() != o.attributes.size()) {
            return false;
        }
        // 한쪽의 속성이 다른 쪽에 같은 값으로 있는지 확인한다.
        for (int i = 0; i < attributes.size(); i++) {
            Attr a = attributes.get(i);
            Attr b = o.getAttributeNodeNS(a.getNamespaceURI(), a.getLocalName());
            if (b == null || !a.getValue().equals(b.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two strings that may be null.
     * <p>
     * null 일 수 있는 두 문자열을 비교한다.
     *
     * @param a first / 첫 번째
     * @param b second / 두 번째
     * @return true if equal / 같으면 true
     */
    private static boolean sameString(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
