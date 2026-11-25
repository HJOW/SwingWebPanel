package org.duckdns.hjow.swingwebpanel.dom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A document: the root of a DOM tree and the factory of its nodes (DOM Standard <code>Document</code>). It also holds the state shared
 * by all nodes of the tree: the mutation version used by live collections, the listeners of DOM changes, the id index, and the services
 * that other layers install (element factory, selector engine, markup parser/serializer, error handler for event listeners and the
 * window that follows the document in the event path). All nodes of a document must be used on the same thread, the document
 * thread of the owning browser component.
 * <p>
 * 문서: DOM 트리의 루트이자 그 노드들의 팩터리(DOM 표준 <code>Document</code>). 트리의 모든 노드가 공유하는 상태도 가진다: 라이브 컬렉션이 쓰는
 * 변경 버전, DOM 변경 리스너, id 색인, 그리고 다른 계층이 설치하는 서비스(요소 팩터리, 선택자 엔진, 마크업 파서/직렬화기, 이벤트 리스너 오류
 * 핸들러, 이벤트 경로에서 문서 뒤에 오는 window). 문서의 모든 노드는 같은 스레드, 즉 소유 브라우저 컴포넌트의 문서 스레드에서 써야 한다.
 */
public class Document extends Node {

    /**
     * Compatibility mode value: standards mode (<code>CSS1Compat</code>).
     * <p>
     * 호환 모드 값: 표준 모드(<code>CSS1Compat</code>).
     */
    public static final int NO_QUIRKS_MODE = 0;

    /**
     * Compatibility mode value: limited quirks mode (almost standards).
     * <p>
     * 호환 모드 값: 제한 호환 모드(거의 표준).
     */
    public static final int LIMITED_QUIRKS_MODE = 1;

    /**
     * Compatibility mode value: quirks mode (<code>BackCompat</code>).
     * <p>
     * 호환 모드 값: 호환 모드(<code>BackCompat</code>).
     */
    public static final int QUIRKS_MODE = 2;

    /**
     * Factory that creates plain elements.
     * <p>
     * 일반 요소를 만드는 팩터리.
     */
    private static final ElementFactory PLAIN_ELEMENTS = new ElementFactory() {
        /**
         * Creates a plain {@link Element}.
         * <p>
         * 일반 {@link Element} 를 만든다.
         *
         * @param document     owner document / 소유 문서
         * @param namespaceURI namespace or null / 네임스페이스 또는 null
         * @param prefix       prefix or null / 접두사 또는 null
         * @param localName    local name / 로컬 이름
         * @return the element / 요소
         */
        public Element createElement(Document document, String namespaceURI, String prefix, String localName) {
            return new Element(document, namespaceURI, prefix, localName);
        }
    };

    /**
     * True for an HTML document (HTML parsing rules, case-insensitive names).
     * <p>
     * HTML 문서이면 true(HTML 해석 규칙, 대소문자 구분 없는 이름).
     */
    private final boolean htmlDocument;

    /**
     * MIME type of the document.
     * <p>
     * 문서의 MIME 타입.
     */
    private final String contentType;

    /**
     * Document URL.
     * <p>
     * 문서 URL.
     */
    private String url = "about:blank";

    /**
     * Base URL for relative addresses, or null to use the document URL.
     * <p>
     * 상대 주소의 기준 URL. null 이면 문서 URL 을 쓴다.
     */
    private String baseUrl;

    /**
     * Character set name of the document.
     * <p>
     * 문서의 문자셋 이름.
     */
    private String characterSet = "UTF-8";

    /**
     * The element that has the focus, or null (see {@link FocusManager}).
     * <p>
     * 포커스를 가진 요소. 없으면 null({@link FocusManager} 참조).
     */
    Element focusedElement;

    /**
     * Tells whether the window of this document has the focus; the focus events of elements are only sent while it has.
     * <p>
     * 이 문서의 창에 포커스가 있는지 알려 준다. 요소의 포커스 이벤트는 창에 포커스가 있는 동안에만 보낸다.
     */
    private volatile boolean windowFocused = true;

    /**
     * Compatibility mode.
     * <p>
     * 호환 모드.
     */
    private int compatibilityMode = NO_QUIRKS_MODE;

    /**
     * Counter that grows with every change of the tree structure or of an attribute.
     * <p>
     * 트리 구조나 속성이 바뀔 때마다 증가하는 카운터.
     */
    private long mutationVersion;

    /** The stable legacy collection, created on first access. / 첫 조회에서 생성하는 동일한 레거시 컬렉션. */
    private AllElements allElements;

    /**
     * Listeners of DOM changes (copied on write).
     * <p>
     * DOM 변경 리스너(쓰기 시 복사).
     */
    private List<DomMutationListener> mutationListeners = new ArrayList<DomMutationListener>(0);

    /**
     * Element factory. The default creates the specialized HTML form elements (see {@link org.duckdns.hjow.swingwebpanel.dom.html.HtmlElementFactory}) and
     * plain elements for everything else; {@link #setElementFactory(ElementFactory)} with null restores plain elements only.
     * <p>
     * 요소 팩터리. 기본값은 특화된 HTML 폼 요소({@link org.duckdns.hjow.swingwebpanel.dom.html.HtmlElementFactory} 참조)와 그 밖의 일반 요소를 만들며,
     * {@link #setElementFactory(ElementFactory)} 에 null 을 주면 일반 요소만 만드는 팩터리로 되돌린다.
     */
    private ElementFactory elementFactory = org.duckdns.hjow.swingwebpanel.dom.html.HtmlElementFactory.INSTANCE;

    /**
     * The per-document support of the HTML form controls (created on first use), or null.
     * <p>
     * 문서별 HTML 폼 컨트롤 지원 객체(처음 쓸 때 만든다). 없으면 null.
     */
    private org.duckdns.hjow.swingwebpanel.dom.html.FormSupport formSupport;

    /**
     * The live ranges and node iterators of this document (created when the first one is made), or null.
     * <p>
     * 이 문서의 살아 있는 범위와 노드 반복자(처음 만들 때 생긴다). 없으면 null.
     */
    private LiveRanges liveRanges;

    /**
     * The selection of this document (created when first asked for), or null.
     * <p>
     * 이 문서의 선택(처음 요청할 때 생긴다). 없으면 null.
     */
    private Selection selection;

    /**
     * The listener of the selection of this document, or null.
     * <p>
     * 이 문서의 선택의 리스너. 없으면 null.
     */
    private SelectionListener selectionListener;

    /**
     * Selector engine or null.
     * <p>
     * 선택자 엔진 또는 null.
     */
    private SelectorEngine selectorEngine;

    /**
     * Markup support or null.
     * <p>
     * 마크업 지원 또는 null.
     */
    private MarkupSupport markupSupport;

    /**
     * Handler for event listener failures.
     * <p>
     * 이벤트 리스너 실패 핸들러.
     */
    private ListenerErrorHandler errorHandler = PRINTING_HANDLER;

    /**
     * The window that follows this document in the event path, or null.
     * <p>
     * 이벤트 경로에서 이 문서 뒤에 오는 window. 없으면 null.
     */
    private EventTarget eventWindow;

    /**
     * Id index: first element of each id in tree order.
     * <p>
     * id 색인: 각 id 의 트리 순서 첫 요소.
     */
    private Map<String, Element> idIndex;

    /**
     * Document version of the id index.
     * <p>
     * id 색인의 문서 버전.
     */
    private long idIndexVersion = -1;

    /**
     * Creates an empty document.
     * <p>
     * 빈 문서를 만든다.
     *
     * @param html        true for an HTML document / HTML 문서이면 true
     * @param contentType MIME type, for example <code>text/html</code> or <code>application/xhtml+xml</code> / MIME 타입
     */
    public Document(boolean html, String contentType) {
        super(null);
        this.doc = this;
        this.htmlDocument = html;
        this.contentType = contentType == null ? (html ? "text/html" : "application/xml") : contentType;
    }

    /**
     * Creates an empty HTML document.
     * <p>
     * 빈 HTML 문서를 만든다.
     *
     * @return the document / 문서
     */
    public static Document createHtmlDocument() {
        return new Document(true, "text/html");
    }

    /**
     * Creates an empty XML document of a content type.
     * <p>
     * 콘텐츠 타입이 정해진 빈 XML 문서를 만든다.
     *
     * @param contentType content type such as <code>application/xhtml+xml</code> / 콘텐츠 타입
     * @return the document / 문서
     */
    public static Document createXmlDocument(String contentType) {
        return new Document(false, contentType);
    }

    /**
     * Returns {@link Node#DOCUMENT_NODE}.
     * <p>
     * {@link Node#DOCUMENT_NODE} 를 반환한다.
     *
     * @return node type / 노드 종류
     */
    public short getNodeType() {
        return DOCUMENT_NODE;
    }

    /**
     * Returns <code>#document</code>.
     * <p>
     * <code>#document</code> 를 반환한다.
     *
     * @return node name / 노드 이름
     */
    public String getNodeName() {
        return "#document";
    }

    /**
     * Tells whether this is an HTML document.
     * <p>
     * HTML 문서인지 알려 준다.
     *
     * @return true for HTML documents / HTML 문서이면 true
     */
    public boolean isHtmlDocument() {
        return htmlDocument;
    }

    /**
     * Returns the MIME type of the document.
     * <p>
     * 문서의 MIME 타입을 반환한다.
     *
     * @return content type / 콘텐츠 타입
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the document URL (<code>document.URL</code>).
     * <p>
     * 문서 URL 을 반환한다(<code>document.URL</code>).
     *
     * @return URL / URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the document URL.
     * <p>
     * 문서 URL 을 지정한다.
     *
     * @param value new URL / 새 URL
     */
    public void setUrl(String value) {
        this.url = value;
    }

    /**
     * The <code>iframe</code> or <code>frame</code> element of the parent document that this document is shown in, or null (HTML5 REC
     * 2014-10-28 10.3.2 "container frame element": the presentational hints of the page take the margins from it).
     * <p>
     * 이 문서를 보여 주는 부모 문서의 <code>iframe</code> 또는 <code>frame</code> 요소. 없으면 null(HTML5 REC 2014-10-28 10.3.2 "container frame
     * element": 페이지의 표현 힌트가 여백을 여기서 가져온다).
     */
    private Element containerFrame;

    /**
     * Returns the frame element that contains this document.
     * <p>
     * 이 문서를 포함한 프레임 요소를 반환한다.
     *
     * @return the <code>iframe</code> or <code>frame</code> element, or null / <code>iframe</code> 또는 <code>frame</code> 요소. 없으면 null
     */
    public Element getContainerFrame() {
        return containerFrame;
    }

    /**
     * Sets the frame element that contains this document (the host does it before the document is parsed).
     * <p>
     * 이 문서를 포함한 프레임 요소를 정한다(호스트가 문서를 파싱하기 전에 한다).
     *
     * @param frame the frame element or null / 프레임 요소. 없으면 null
     */
    public void setContainerFrame(Element frame) {
        this.containerFrame = frame;
    }

    /**
     * The address that the origin of the document is computed from, or null when it is the document URL. A frame whose document is
     * <code>about:blank</code> or <code>about:srcdoc</code> takes the address of its creator here (HTML5 REC 2014-10-28 5.1: the origin of
     * such a document is an alias of the origin of the creator), and a sandboxed frame without <code>allow-same-origin</code> takes an
     * address that gives an opaque origin.
     * <p>
     * 문서의 오리진을 계산하는 원천 주소. 문서 URL 과 같으면 null. 문서가 <code>about:blank</code> 나 <code>about:srcdoc</code> 인 프레임은 여기에 생성자의
     * 주소를 가지며(HTML5 REC 2014-10-28 5.1: 그런 문서의 오리진은 생성자 오리진의 별칭), <code>allow-same-origin</code> 이 없는 샌드박스 프레임은
     * 불투명 오리진을 주는 주소를 가진다.
     */
    private String originUrl;

    /**
     * Returns the address that the origin of the document is computed from: the origin address if one was set, otherwise the document URL.
     * <p>
     * 문서의 오리진을 계산하는 원천 주소를 반환한다: 지정했으면 그 주소, 아니면 문서 URL.
     *
     * @return the address / 주소
     */
    public String getOriginUrl() {
        return originUrl != null ? originUrl : url;
    }

    /**
     * Sets the address that the origin of the document is computed from, or null to use the document URL.
     * <p>
     * 문서의 오리진을 계산하는 원천 주소를 지정한다. null 이면 문서 URL 을 쓴다.
     *
     * @param value the address or null / 주소 또는 null
     */
    public void setOriginUrl(String value) {
        this.originUrl = value;
    }

    /**
     * Returns the base URL for relative addresses (<code>document.baseURI</code>): the URL of the first <code>base</code> element if the
     * parser set one, otherwise the document URL.
     * <p>
     * 상대 주소의 기준 URL 을 반환한다(<code>document.baseURI</code>): 파서가 정했다면 첫 <code>base</code> 요소의 URL, 아니면 문서 URL.
     *
     * @return base URL / 기준 URL
     */
    public String getBaseUri() {
        return baseUrl != null ? baseUrl : url;
    }

    /**
     * Sets the base URL, or null to use the document URL.
     * <p>
     * 기준 URL 을 지정한다. null 이면 문서 URL 을 쓴다.
     *
     * @param value base URL or null / 기준 URL 또는 null
     */
    public void setBaseUri(String value) {
        this.baseUrl = value;
    }

    /**
     * Returns the character set name.
     * <p>
     * 문자셋 이름을 반환한다.
     *
     * @return character set / 문자셋
     */
    public String getCharacterSet() {
        return characterSet;
    }

    /**
     * Sets the character set name.
     * <p>
     * 문자셋 이름을 지정한다.
     *
     * @param value character set / 문자셋
     */
    public void setCharacterSet(String value) {
        this.characterSet = value;
    }

    /**
     * Returns the compatibility mode ({@link #NO_QUIRKS_MODE}, {@link #LIMITED_QUIRKS_MODE} or {@link #QUIRKS_MODE}).
     * <p>
     * 호환 모드({@link #NO_QUIRKS_MODE}, {@link #LIMITED_QUIRKS_MODE}, {@link #QUIRKS_MODE})를 반환한다.
     *
     * @return mode / 모드
     */
    public int getCompatibilityMode() {
        return compatibilityMode;
    }

    /**
     * Sets the compatibility mode (done by the HTML parser from the doctype).
     * <p>
     * 호환 모드를 지정한다(HTML 파서가 문서 유형으로 정한다).
     *
     * @param mode new mode / 새 모드
     */
    public void setCompatibilityMode(int mode) {
        this.compatibilityMode = mode;
    }

    /**
     * Returns <code>CSS1Compat</code> for standards and limited quirks mode and <code>BackCompat</code> for quirks mode
     * (<code>document.compatMode</code>).
     * <p>
     * 표준과 제한 호환 모드는 <code>CSS1Compat</code>, 호환 모드는 <code>BackCompat</code> 를 반환한다(<code>document.compatMode</code>).
     *
     * @return compat mode string / 호환 모드 문자열
     */
    public String getCompatMode() {
        return compatibilityMode == QUIRKS_MODE ? "BackCompat" : "CSS1Compat";
    }

    /**
     * Returns the document element (the root element), or null.
     * <p>
     * 문서 요소(루트 요소)를 반환한다. 없으면 null.
     *
     * @return root element or null / 루트 요소 또는 null
     */
    public Element getDocumentElement() {
        return getFirstElementChild();
    }

    /**
     * Returns the document type child, or null.
     * <p>
     * 문서 유형 자식을 반환한다. 없으면 null.
     *
     * @return document type or null / 문서 유형 또는 null
     */
    public DocumentType getDoctype() {
        // 자식 중 문서 유형을 찾는다.
        for (Node c = firstChild; c != null; c = c.nextSibling) {
            if (c instanceof DocumentType) {
                return (DocumentType) c;
            }
        }
        return null;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 팩터리
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates an element (<code>createElement</code>). In HTML documents the name is lower-cased and the element is in the HTML
     * namespace; in XHTML documents the element is in the HTML namespace; otherwise it has no namespace.
     * <p>
     * 요소를 만든다(<code>createElement</code>). HTML 문서에서는 이름을 소문자로 바꾸고 HTML 네임스페이스에 속하며, XHTML 문서에서도 HTML
     * 네임스페이스에 속하고, 그 밖에는 네임스페이스가 없다.
     *
     * @param localName element name / 요소 이름
     * @return the element / 요소
     * @throws DomException InvalidCharacterError if the name is not valid / 이름이 올바르지 않으면 InvalidCharacterError
     */
    public Element createElement(String localName) {
        if (!Namespaces.isValidName(localName)) {
            throw DomException.invalidCharacter("invalid element name: " + localName);
        }
        String name = htmlDocument ? Namespaces.asciiLower(localName) : localName;
        String ns = htmlDocument || "application/xhtml+xml".equals(contentType) ? Namespaces.HTML : null;
        return createElementInternal(ns, null, name);
    }

    /**
     * Creates an element with a namespace (<code>createElementNS</code>).
     * <p>
     * 네임스페이스가 있는 요소를 만든다(<code>createElementNS</code>).
     *
     * @param namespace     namespace or null / 네임스페이스 또는 null
     * @param qualifiedName qualified name / 정규화된 이름
     * @return the element / 요소
     * @throws DomException InvalidCharacterError or NamespaceError / InvalidCharacterError 또는 NamespaceError
     */
    public Element createElementNS(String namespace, String qualifiedName) {
        Namespaces.QualifiedName q = Namespaces.validateAndExtract(namespace, qualifiedName);
        return createElementInternal(q.namespace, q.prefix, q.localName);
    }

    /**
     * Creates an element through the installed factory without any name validation.
     * <p>
     * 이름 검증 없이 설치된 팩터리로 요소를 만든다.
     *
     * @param namespace namespace or null / 네임스페이스 또는 null
     * @param prefix    prefix or null / 접두사 또는 null
     * @param local     local name / 로컬 이름
     * @return the element / 요소
     */
    public final Element createElementInternal(String namespace, String prefix, String local) {
        return elementFactory.createElement(this, namespace, prefix, local);
    }

    /**
     * Creates a text node.
     * <p>
     * 텍스트 노드를 만든다.
     *
     * @param data text / 텍스트
     * @return the node / 노드
     */
    public Text createTextNode(String data) {
        return new Text(this, data);
    }

    /**
     * Creates a comment.
     * <p>
     * 주석을 만든다.
     *
     * @param data comment text / 주석 텍스트
     * @return the node / 노드
     */
    public Comment createComment(String data) {
        return new Comment(this, data);
    }

    /**
     * Creates a CDATA section (XML documents only).
     * <p>
     * CDATA 섹션을 만든다(XML 문서만).
     *
     * @param data text, must not contain <code>]]&gt;</code> / 텍스트(<code>]]&gt;</code> 포함 불가)
     * @return the node / 노드
     * @throws DomException NotSupportedError in HTML documents, InvalidCharacterError for <code>]]&gt;</code> / HTML 문서에서는 NotSupportedError
     */
    public CDATASection createCDATASection(String data) {
        if (htmlDocument) {
            throw DomException.notSupported("CDATA sections are not supported in HTML documents");
        }
        if (data != null && data.indexOf("]]>") >= 0) {
            throw DomException.invalidCharacter("a CDATA section cannot contain ]]>");
        }
        return new CDATASection(this, data);
    }

    /**
     * Creates a processing instruction.
     * <p>
     * 처리 명령을 만든다.
     *
     * @param target target name / 대상 이름
     * @param data   data, must not contain <code>?&gt;</code> / 데이터(<code>?&gt;</code> 포함 불가)
     * @return the node / 노드
     * @throws DomException InvalidCharacterError for an invalid target or data / 대상이나 데이터가 올바르지 않으면 InvalidCharacterError
     */
    public ProcessingInstruction createProcessingInstruction(String target, String data) {
        if (!Namespaces.isValidName(target)) {
            throw DomException.invalidCharacter("invalid target: " + target);
        }
        if (data != null && data.indexOf("?>") >= 0) {
            throw DomException.invalidCharacter("the data cannot contain ?>");
        }
        return new ProcessingInstruction(this, target, data == null ? "" : data);
    }

    /**
     * Creates an empty document fragment.
     * <p>
     * 빈 문서 조각을 만든다.
     *
     * @return the fragment / 조각
     */
    public DocumentFragment createDocumentFragment() {
        return new DocumentFragment(this);
    }

    /**
     * Creates a range that is collapsed at the start of this document (DOM Standard, <code>document.createRange()</code>). The range is
     * live: it follows the changes of the tree.
     * <p>
     * 이 문서의 시작에 접힌 범위를 만든다(DOM 표준 <code>document.createRange()</code>). 범위는 살아 있어 트리의 변경을 따라간다.
     *
     * @return the range / 범위
     */
    public Range createRange() {
        return new Range(this);
    }

    /**
     * Creates a node iterator (DOM Standard, <code>document.createNodeIterator()</code>); it starts before the root.
     * <p>
     * 노드 반복자를 만든다(DOM 표준 <code>document.createNodeIterator()</code>). 루트 앞에서 시작한다.
     *
     * @param root       the root node, not null / 루트 노드(null 불가)
     * @param whatToShow the bit mask of the node kinds to show (see {@link NodeFilter}) / 보여 줄 노드 종류의 비트 마스크({@link NodeFilter} 참조)
     * @param filter     the filter or null / 필터 또는 null
     * @return the iterator / 반복자
     */
    public NodeIterator createNodeIterator(Node root, long whatToShow, NodeFilter filter) {
        return new NodeIterator(root, whatToShow, filter);
    }

    /**
     * Creates a tree walker (DOM Standard, <code>document.createTreeWalker()</code>); its current node is the root.
     * <p>
     * 트리 걷기 도구를 만든다(DOM 표준 <code>document.createTreeWalker()</code>). 현재 노드는 루트이다.
     *
     * @param root       the root node, not null / 루트 노드(null 불가)
     * @param whatToShow the bit mask of the node kinds to show (see {@link NodeFilter}) / 보여 줄 노드 종류의 비트 마스크({@link NodeFilter} 참조)
     * @param filter     the filter or null / 필터 또는 null
     * @return the walker / 걷는 도구
     */
    public TreeWalker createTreeWalker(Node root, long whatToShow, NodeFilter filter) {
        return new TreeWalker(root, whatToShow, filter);
    }

    /**
     * Returns the selection of this document (W3C Selection API, <code>document.getSelection()</code>); the same object every time.
     * <p>
     * 이 문서의 선택을 반환한다(W3C Selection API <code>document.getSelection()</code>). 언제나 같은 객체이다.
     *
     * @return the selection / 선택
     */
    public Selection getSelection() {
        if (selection == null) {
            selection = new Selection(this);
            selection.setListener(selectionListener);
        }
        return selection;
    }

    /**
     * Design mode of this document (<code>document.designMode</code>, HTML Standard "design mode enabled"): when on, the whole document is
     * editable (its root element is the editing host).
     * <p>
     * 이 문서의 디자인 모드(<code>document.designMode</code>, HTML Standard "design mode enabled"). 켜져 있으면 문서 전체가 편집 가능하다(루트 요소가 편집 호스트).
     */
    private boolean designMode;

    /**
     * The editor of this document, made when first needed.
     * <p>
     * 이 문서의 편집기. 처음 필요할 때 만든다.
     */
    private org.duckdns.hjow.swingwebpanel.dom.edit.Editor editor;

    /**
     * Tells whether the document is in design mode.
     * <p>
     * 문서가 디자인 모드인지 알려 준다.
     *
     * @return true if <code>designMode</code> is "on" / <code>designMode</code> 가 "on" 이면 true
     */
    public boolean isDesignMode() {
        return designMode;
    }

    /**
     * Switches design mode. Turning it on puts the selection at the start of the document (HTML Standard: "reset the active range's start and
     * end boundary points to be at the start of the document"; here inside the body so that the caret has a text position).
     * <p>
     * 디자인 모드를 바꾼다. 켜면 선택을 문서의 시작에 둔다(HTML Standard: "active range 의 시작과 끝을 문서의 시작으로 되돌린다". 여기서는 캐럿이 텍스트 위치를
     * 가지도록 body 안에 둔다).
     *
     * @param on true to switch it on / 켜려면 true
     */
    public void setDesignMode(boolean on) {
        if (on && !designMode) {
            designMode = true;
            ElementCollection bodies = getElementsByTagName("body");
            Node start = bodies.getLength() > 0 ? bodies.item(0) : getDocumentElement();
            getSelection().collapse(start == null ? this : start, 0);
        } else if (!on) {
            designMode = false;
        }
    }

    /**
     * Returns the editor of this document (the editing commands, the undo history and the input events); the same object every time.
     * <p>
     * 이 문서의 편집기(편집 명령, 되돌리기 이력, 입력 이벤트)를 반환한다. 언제나 같은 객체이다.
     *
     * @return the editor / 편집기
     */
    public org.duckdns.hjow.swingwebpanel.dom.edit.Editor getEditor() {
        if (editor == null) {
            editor = new org.duckdns.hjow.swingwebpanel.dom.edit.Editor(this);
        }
        return editor;
    }

    /**
     * Sets the listener that is told about every change of the selection of this document (null removes it). Install it before the page is
     * shared with other threads: the component of the page does it right after the document is created, so that scripts and the user act
     * on the same selection and the same notifications.
     * <p>
     * 이 문서의 선택이 바뀔 때마다 알리는 리스너를 지정한다(null 이면 제거). 페이지를 다른 스레드와 공유하기 전에 설치한다: 페이지의 컴포넌트는 문서를 만든
     * 직후 설치하므로, 스크립트와 사용자가 같은 선택과 같은 통지를 쓴다.
     *
     * @param listener the listener or null / 리스너. 없으면 null
     */
    public void setSelectionListener(SelectionListener listener) {
        this.selectionListener = listener;
        if (selection != null) {
            selection.setListener(listener);
        }
    }

    /**
     * The highlight registry of this document (CSS Custom Highlight API, <code>CSS.highlights</code>), created when it is first asked for, or null.
     * <p>
     * 이 문서의 하이라이트 등록부(CSS Custom Highlight API 의 <code>CSS.highlights</code>). 처음 요청할 때 생긴다. 없으면 null.
     */
    private HighlightRegistry highlights;

    /**
     * Returns the highlight registry of this document, creating it when needed (CSS Custom Highlight API Level 1, section 3.2).
     * <p>
     * 이 문서의 하이라이트 등록부를 반환하며 필요하면 만든다(CSS Custom Highlight API Level 1, 3.2 절).
     *
     * @return the registry / 등록부
     */
    public synchronized HighlightRegistry getHighlights() {
        if (highlights == null) {
            highlights = new HighlightRegistry();
        }
        return highlights;
    }

    /**
     * The top layer of this document (CSS Positioned Layout 4 section 3): the elements that are rendered above everything, in the order in which they were
     * added (a modal dialog opened with <code>showModal()</code>).
     * <p>
     * 이 문서의 최상위 계층(CSS Positioned Layout 4 3 절): 모든 것 위에 그려지는 요소를 더한 순서로 담는다(<code>showModal()</code> 로 연 모달 대화상자).
     */
    private final List<Element> topLayer = new ArrayList<Element>(0);

    /**
     * Adds an element to the top layer, moving it to the end if it is already there (CSS Positioned Layout 4 section 3.3, "add an element to the top layer").
     * <p>
     * 요소를 최상위 계층에 더한다. 이미 있으면 끝으로 옮긴다(CSS Positioned Layout 4 3.3 절의 "add an element to the top layer").
     *
     * @param e the element / 요소
     */
    public synchronized void addToTopLayer(Element e) {
        topLayer.remove(e);
        topLayer.add(e);
    }

    /**
     * Removes an element from the top layer.
     * <p>
     * 요소를 최상위 계층에서 뺀다.
     *
     * @param e the element / 요소
     * @return true if it was there / 있었으면 true
     */
    public synchronized boolean removeFromTopLayer(Element e) {
        return topLayer.remove(e);
    }

    /**
     * Tells whether an element is in the top layer of this document (an element that left the document is not).
     * <p>
     * 요소가 이 문서의 최상위 계층에 있는지 알려 준다(문서를 떠난 요소는 아니다).
     *
     * @param e the element / 요소
     * @return true if it is / 있으면 true
     */
    public synchronized boolean isInTopLayer(Element e) {
        return e != null && e.isConnected() && topLayer.contains(e);
    }

    /**
     * Returns the elements of the top layer in order (the last is on top); elements that left the document are dropped from the layer (the removing steps of
     * a dialog).
     * <p>
     * 최상위 계층의 요소를 순서대로 반환한다(마지막 것이 가장 위). 문서를 떠난 요소는 계층에서 뺀다(대화상자의 제거 단계).
     *
     * @return a new list / 새 목록
     */
    public synchronized List<Element> getTopLayer() {
        // 문서에 연결되지 않은 요소는 계층에서 지운다.
        for (int i = topLayer.size() - 1; i >= 0; i--) {
            if (!topLayer.get(i).isConnected() || topLayer.get(i).getOwnerDocument() != this) {
                topLayer.remove(i);
            }
        }
        return new ArrayList<Element>(topLayer);
    }

    /**
     * The last remembered sizes of the elements (CSS Box Sizing 4 section 5.2.1), by element, weakly held.
     * <p>
     * 요소의 마지막으로 기억한 크기(CSS Box Sizing 4 5.2.1 절). 요소별이며 약하게 잡는다.
     */
    private Map<Element, double[]> rememberedSizes;

    /**
     * Returns the last remembered size of an element: the inner width and height of its principal box when it last had
     * <code>contain-intrinsic-size: auto</code> without size containment (CSS Box Sizing 4 section 5.2.1), or null.
     * <p>
     * 요소의 마지막으로 기억한 크기를 반환한다: 크기 격리 없이 <code>contain-intrinsic-size: auto</code> 를 마지막으로 가졌을 때 주 상자의 안쪽 너비와 높이(CSS Box Sizing 4
     * 5.2.1 절). 없으면 null.
     *
     * @param e the element / 요소
     * @return {width, height} or null / {너비, 높이} 또는 null
     */
    public synchronized double[] getLastRememberedSize(Element e) {
        double[] v = rememberedSizes == null ? null : rememberedSizes.get(e);
        return v == null ? null : v.clone();
    }

    /**
     * Records or removes the last remembered size of an element (the layout does it after every layout, as at the time of the resize observations).
     * <p>
     * 요소의 마지막으로 기억한 크기를 기록하거나 지운다(배치가 배치마다, 크기 관찰 시점처럼 한다).
     *
     * @param e    the element / 요소
     * @param size {width, height}, or null to remove it / {너비, 높이}. 지우려면 null
     */
    public synchronized void setLastRememberedSize(Element e, double[] size) {
        if (size == null) {
            if (rememberedSizes != null) {
                rememberedSizes.remove(e);
            }
            return;
        }
        if (rememberedSizes == null) {
            rememberedSizes = new java.util.WeakHashMap<Element, double[]>();
        }
        rememberedSizes.put(e, size.clone());
    }

    /**
     * Returns the highlight registry of this document without creating it (the renderer asks this).
     * <p>
     * 이 문서의 하이라이트 등록부를 만들지 않고 반환한다(렌더러가 묻는다).
     *
     * @return the registry or null / 등록부. 없으면 null
     */
    public synchronized HighlightRegistry peekHighlights() {
        return highlights;
    }

    /**
     * Returns the registry of the live ranges and node iterators of this document, creating it when needed.
     * <p>
     * 이 문서의 살아 있는 범위와 노드 반복자의 등록부를 반환하며 필요하면 만든다.
     *
     * @return the registry / 등록부
     */
    final LiveRanges liveRegistry() {
        if (liveRanges == null) {
            liveRanges = new LiveRanges();
        }
        return liveRanges;
    }

    /**
     * Passes a replacement of character data to the live ranges (called by {@link CharacterData#replaceData}).
     * <p>
     * 문자 데이터의 교체를 살아 있는 범위에 전달한다({@link CharacterData#replaceData} 가 부른다).
     *
     * @param node       the node / 노드
     * @param offset     where the replacement started / 교체가 시작된 위치
     * @param count      the number of replaced characters / 교체된 문자 수
     * @param dataLength the number of inserted characters / 삽입된 문자 수
     */
    final void liveDataReplaced(Node node, int offset, int count, int dataLength) {
        if (liveRanges != null) {
            liveRanges.dataReplaced(node, offset, count, dataLength);
        }
    }

    /**
     * Passes the split of a text node to the live ranges (called by {@link Text#splitText}).
     * <p>
     * 텍스트 노드의 분할을 살아 있는 범위에 전달한다({@link Text#splitText} 가 부른다).
     *
     * @param node    the original node / 원래 노드
     * @param created the new node / 새 노드
     * @param offset  the split offset / 쪼갠 위치
     */
    final void liveTextSplit(Node node, Node created, int offset) {
        if (liveRanges != null) {
            liveRanges.textSplit(node, created, offset);
        }
    }

    /**
     * Passes the merge of a text node into the one before it to the live ranges (called by {@link Node#normalize()}).
     * <p>
     * 텍스트 노드를 앞의 텍스트 노드에 합치는 일을 살아 있는 범위에 전달한다({@link Node#normalize()} 가 부른다).
     *
     * @param target  the text node that receives the data / 데이터를 받는 텍스트 노드
     * @param current the merged text node / 합쳐지는 텍스트 노드
     * @param length  the length of the target before the data of the current node / 현재 노드의 데이터 앞의 대상 길이
     * @param index   the index of the current node in its parent / 현재 노드의 부모 안의 위치
     */
    final void liveTextMerged(Node target, Node current, int length, int index) {
        if (liveRanges != null) {
            liveRanges.textMerged(target, current, length, index);
        }
    }

    /**
     * Creates a document type node.
     * <p>
     * 문서 유형 노드를 만든다.
     *
     * @param name     name / 이름
     * @param publicId public identifier or null / 공개 식별자 또는 null
     * @param systemId system identifier or null / 시스템 식별자 또는 null
     * @return the node / 노드
     */
    public DocumentType createDocumentType(String name, String publicId, String systemId) {
        return new DocumentType(this, name, publicId, systemId);
    }

    /**
     * Creates an attribute without namespace (<code>createAttribute</code>); the name is lower-cased in HTML documents.
     * <p>
     * 네임스페이스 없는 속성을 만든다(<code>createAttribute</code>). HTML 문서에서는 이름을 소문자로 바꾼다.
     *
     * @param localName attribute name / 속성 이름
     * @return the attribute / 속성
     * @throws DomException InvalidCharacterError if the name is not valid / 이름이 올바르지 않으면 InvalidCharacterError
     */
    public Attr createAttribute(String localName) {
        if (!Namespaces.isValidName(localName)) {
            throw DomException.invalidCharacter("invalid attribute name: " + localName);
        }
        return new Attr(this, null, null, htmlDocument ? Namespaces.asciiLower(localName) : localName, "");
    }

    /**
     * Creates an attribute with a namespace (<code>createAttributeNS</code>).
     * <p>
     * 네임스페이스가 있는 속성을 만든다(<code>createAttributeNS</code>).
     *
     * @param namespace     namespace or null / 네임스페이스 또는 null
     * @param qualifiedName qualified name / 정규화된 이름
     * @return the attribute / 속성
     * @throws DomException InvalidCharacterError or NamespaceError / InvalidCharacterError 또는 NamespaceError
     */
    public Attr createAttributeNS(String namespace, String qualifiedName) {
        Namespaces.QualifiedName q = Namespaces.validateAndExtract(namespace, qualifiedName);
        return new Attr(this, q.namespace, q.prefix, q.localName, "");
    }

    /**
     * Tells whether the window (the component) of this document has the focus.
     * <p>
     * 이 문서의 창(컴포넌트)에 포커스가 있는지 알려 준다.
     *
     * @return true if it has the focus / 포커스가 있으면 true
     */
    public boolean isWindowFocused() {
        return windowFocused;
    }

    /**
     * Records that the window (the component) gained or lost the focus; the host calls this together with
     * {@link FocusManager#windowFocusChanged(Document, boolean)}.
     * <p>
     * 창(컴포넌트)이 포커스를 얻거나 잃었음을 기록한다. 호스트가 {@link FocusManager#windowFocusChanged(Document, boolean)} 와 함께 호출한다.
     *
     * @param value the new state / 새 상태
     */
    public void setWindowFocused(boolean value) {
        this.windowFocused = value;
    }

    /**
     * Adds a listener and remembers that a <code>visibilitychange</code> listener exists (the host asks it from any thread, to know whether a page that is left
     * has anyone to tell).
     * <p>
     * 리스너를 추가하고 <code>visibilitychange</code> 리스너가 있다는 것을 기억한다(떠나는 페이지에 알릴 상대가 있는지 호스트가 아무 스레드에서나 묻는다).
     *
     * @param type     the event type / 이벤트 종류
     * @param listener the callback / 콜백
     * @param capture  the capture flag / 캡처 플래그
     * @param once     the once flag / once 플래그
     * @param passive  the passive flag / passive 플래그
     */
    public void addEventListener(String type, EventListener listener, boolean capture, boolean once, boolean passive) {
        if (listener != null && "visibilitychange".equals(type)) {
            visibilityListener = true;
        }
        super.addEventListener(type, listener, capture, once, passive);
    }

    /**
     * Tells whether a <code>visibilitychange</code> listener was ever registered at this document (readable from any thread).
     * <p>
     * 이 문서에 <code>visibilitychange</code> 리스너가 등록된 적이 있는지 알려 준다(어느 스레드에서나 읽을 수 있다).
     *
     * @return true if one was registered / 등록된 적이 있으면 true
     */
    public boolean hasVisibilityListener() {
        return visibilityListener;
    }

    /**
     * True once a <code>visibilitychange</code> listener was registered.
     * <p>
     * <code>visibilitychange</code> 리스너가 등록된 뒤이면 true.
     */
    private volatile boolean visibilityListener;

    /**
     * Creates an event object by interface name (<code>createEvent</code>): <code>Event</code>, <code>Events</code>,
     * <code>HTMLEvents</code>, <code>CustomEvent</code> and the UI events (<code>UIEvent</code>, <code>MouseEvent</code>, <code>WheelEvent</code>,
     * <code>KeyboardEvent</code>, <code>FocusEvent</code>, <code>InputEvent</code>, <code>CompositionEvent</code>; case-insensitive). The event must be initialized with
     * {@link Event#initEvent(String, boolean, boolean)} before it is dispatched.
     * <p>
     * 인터페이스 이름으로 이벤트 객체를 만든다(<code>createEvent</code>): <code>Event</code>, <code>Events</code>, <code>HTMLEvents</code>,
     * <code>CustomEvent</code>(대소문자 무시). 전달하기 전에 {@link Event#initEvent(String, boolean, boolean)} 로 초기화해야 한다.
     *
     * @param interfaceName interface name / 인터페이스 이름
     * @return the uninitialized event / 초기화되지 않은 이벤트
     * @throws DomException NotSupportedError for unknown interfaces / 알 수 없는 인터페이스이면 NotSupportedError
     */
    public Event createEvent(String interfaceName) {
        String n = Namespaces.asciiLower(interfaceName == null ? "" : interfaceName);
        if ("event".equals(n) || "events".equals(n) || "htmlevents".equals(n)) {
            return new Event("");
        }
        if ("customevent".equals(n)) {
            return new CustomEvent("", false, false);
        }
        if ("uievent".equals(n) || "uievents".equals(n)) {
            return new UiEvent("", false, false);
        }
        if ("mouseevent".equals(n) || "mouseevents".equals(n)) {
            return new MouseEvent("", false, false);
        }
        if ("wheelevent".equals(n)) {
            return new WheelEvent("", false, false);
        }
        if ("keyboardevent".equals(n)) {
            return new KeyboardEvent("", false, false);
        }
        if ("focusevent".equals(n)) {
            return new FocusEvent("", false, false);
        }
        if ("inputevent".equals(n)) {
            return new InputEvent("", false, false);
        }
        if ("compositionevent".equals(n)) {
            return new CompositionEvent("", false, false);
        }
        throw DomException.notSupported("unsupported event interface: " + interfaceName);
    }

    /**
     * Imports a copy of a node from another document (<code>importNode</code>).
     * <p>
     * 다른 문서의 노드의 복사본을 가져온다(<code>importNode</code>).
     *
     * @param node node to copy / 복사할 노드
     * @param deep true to copy the descendants / 후손도 복사하려면 true
     * @return the copy, owned by this document / 이 문서가 소유한 복사본
     * @throws DomException NotSupportedError for documents / 문서이면 NotSupportedError
     */
    public Node importNode(Node node, boolean deep) {
        if (node.getNodeType() == DOCUMENT_NODE) {
            throw DomException.notSupported("a document cannot be imported");
        }
        return node.cloneInternal(this, deep);
    }

    /**
     * Moves a node (with its subtree) into this document (<code>adoptNode</code>).
     * <p>
     * 노드(와 하위 트리)를 이 문서로 옮긴다(<code>adoptNode</code>).
     *
     * @param node node to adopt / 옮길 노드
     * @return the same node / 같은 노드
     * @throws DomException NotSupportedError for documents / 문서이면 NotSupportedError
     */
    public Node adoptNode(Node node) {
        adoptInternal(node);
        return node;
    }

    /**
     * Moves a node into this document: removes it from its parent and changes the owner of the whole subtree.
     * <p>
     * 노드를 이 문서로 옮긴다: 부모에서 떼어 내고 하위 트리 전체의 소유자를 바꾼다.
     *
     * @param node node to adopt / 옮길 노드
     */
    final void adoptInternal(Node node) {
        if (node.getNodeType() == DOCUMENT_NODE) {
            throw DomException.notSupported("a document cannot be adopted");
        }
        Document old = node.doc;
        if (node.parent != null) {
            node.parent.removeChildInternal(node, false);
        }
        if (old != this) {
            Node n = node;
            // 하위 트리의 모든 노드의 소유 문서를 바꾸고 알린다.
            while (n != null) {
                n.doc = this;
                n.adoptedFrom(old);
                n = nextInTree(n, node);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 조회
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the first element in tree order with an id (<code>getElementById</code>). The index is rebuilt only when the document changed.
     * <p>
     * 트리 순서로 id 가 같은 첫 요소를 반환한다(<code>getElementById</code>). 색인은 문서가 바뀐 경우에만 다시 만든다.
     *
     * @param id id value / id 값
     * @return element or null / 요소 또는 null
     */
    public Element getElementById(String id) {
        if (id == null || id.length() == 0) {
            return null;
        }
        if (idIndex == null || idIndexVersion != mutationVersion) {
            Map<String, Element> index = new HashMap<String, Element>();
            Node n = nextInTree(this, this);
            // 트리를 순서대로 훑으며 처음 만나는 id 만 색인에 넣는다.
            while (n != null) {
                if (n instanceof Element) {
                    String eid = ((Element) n).getId();
                    if (eid.length() > 0 && !index.containsKey(eid)) {
                        index.put(eid, (Element) n);
                    }
                }
                n = nextInTree(n, this);
            }
            idIndex = index;
            idIndexVersion = mutationVersion;
        }
        return idIndex.get(id);
    }

    /**
     * Returns the live legacy collection of every element in this document.
     * <p>
     * 문서의 모든 요소를 담은 라이브 레거시 컬렉션을 반환한다.
     *
     * @return the legacy collection for this document / 이 문서의 레거시 컬렉션
     */
    public AllElements getAll() {
        if (allElements == null) {
            allElements = new AllElements(this);
        }
        return allElements;
    }

    /**
     * Returns the live collection of all elements with a tag name.
     * <p>
     * 태그 이름을 가진 모든 요소의 라이브 컬렉션을 반환한다.
     *
     * @param qualifiedName name or * / 이름 또는 *
     * @return collection / 컬렉션
     */
    public ElementCollection getElementsByTagName(String qualifiedName) {
        return ElementCollections.byTagName(this, qualifiedName);
    }

    /**
     * Returns the live collection of all elements with a namespace and local name.
     * <p>
     * 네임스페이스와 로컬 이름을 가진 모든 요소의 라이브 컬렉션을 반환한다.
     *
     * @param namespace namespace or * / 네임스페이스 또는 *
     * @param localName local name or * / 로컬 이름 또는 *
     * @return collection / 컬렉션
     */
    public ElementCollection getElementsByTagNameNS(String namespace, String localName) {
        return ElementCollections.byTagNameNS(this, namespace, localName);
    }

    /**
     * Returns the live collection of all elements that have all the class names.
     * <p>
     * 모든 클래스 이름을 가진 모든 요소의 라이브 컬렉션을 반환한다.
     *
     * @param classNames space separated class names / 공백으로 구분한 클래스 이름
     * @return collection / 컬렉션
     */
    public ElementCollection getElementsByClassName(String classNames) {
        return ElementCollections.byClassName(this, classNames);
    }

    /**
     * Returns the first element in the document that matches a selector list.
     * <p>
     * 문서에서 선택자 목록에 일치하는 첫 요소를 반환한다.
     *
     * @param selectors selector list / 선택자 목록
     * @return element or null / 요소 또는 null
     * @throws DomException SyntaxError or NotSupportedError / SyntaxError 또는 NotSupportedError
     */
    public Element querySelector(String selectors) {
        return requireSelectorEngine().querySelector(this, selectors);
    }

    /**
     * Returns all elements of the document that match a selector list as a static list.
     * <p>
     * 선택자 목록에 일치하는 문서의 모든 요소를 정적 목록으로 반환한다.
     *
     * @param selectors selector list / 선택자 목록
     * @return static list / 정적 목록
     * @throws DomException SyntaxError or NotSupportedError / SyntaxError 또는 NotSupportedError
     */
    public NodeList querySelectorAll(String selectors) {
        return new StaticNodeList(requireSelectorEngine().querySelectorAll(this, selectors));
    }

    /**
     * Returns the DOM implementation object (<code>document.implementation</code>).
     * <p>
     * DOM 구현 객체를 반환한다(<code>document.implementation</code>).
     *
     * @return implementation / 구현 객체
     */
    public DomImplementation getImplementation() {
        return new DomImplementation();
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 변경 통지
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the mutation version: it grows whenever the tree structure or an attribute changes. Live collections and the id index compare it
     * to decide whether to refresh.
     * <p>
     * 변경 버전을 반환한다: 트리 구조나 속성이 바뀔 때마다 증가한다. 라이브 컬렉션과 id 색인은 이것을 비교해 갱신 여부를 정한다.
     *
     * @return version / 버전
     */
    public long getMutationVersion() {
        return mutationVersion;
    }

    /**
     * Increments the mutation version.
     * <p>
     * 변경 버전을 올린다.
     */
    final void bumpVersion() {
        mutationVersion++;
    }

    /**
     * Registers a listener of DOM changes.
     * <p>
     * DOM 변경 리스너를 등록한다.
     *
     * @param listener listener, not null / 리스너(null 불가)
     */
    public void addMutationListener(DomMutationListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        List<DomMutationListener> copy = new ArrayList<DomMutationListener>(mutationListeners);
        copy.add(listener);
        mutationListeners = copy;
    }

    /**
     * Removes a listener of DOM changes.
     * <p>
     * DOM 변경 리스너를 제거한다.
     *
     * @param listener listener / 리스너
     */
    public void removeMutationListener(DomMutationListener listener) {
        List<DomMutationListener> copy = new ArrayList<DomMutationListener>(mutationListeners);
        copy.remove(listener);
        mutationListeners = copy;
    }

    /**
     * Reports a child list change.
     * <p>
     * 자식 목록 변경을 보고한다.
     *
     * @param target  parent whose children changed / 자식이 바뀐 부모
     * @param added   added nodes / 추가된 노드
     * @param removed removed nodes / 제거된 노드
     * @param prev    previous sibling or null / 이전 형제 또는 null
     * @param next    next sibling or null / 다음 형제 또는 null
     */
    final void recordChildList(Node target, List<Node> added, List<Node> removed, Node prev, Node next) {
        // 살아 있는 범위와 노드 반복자는 변경 리스너와 상관없이 항상 따라간다.
        if (liveRanges != null) {
            liveRanges.childListChanged(target, added, removed, prev, next);
        }
        if (mutationListeners.isEmpty()) {
            return;
        }
        deliver(new MutationRecord(MutationRecord.CHILD_LIST, target, added, removed, prev, next, null, null, null), target.isConnected());
    }

    /**
     * Reports an attribute change.
     * <p>
     * 속성 변경을 보고한다.
     *
     * @param target    element / 요소
     * @param local     attribute local name / 속성 로컬 이름
     * @param namespace attribute namespace or null / 속성 네임스페이스 또는 null
     * @param oldValue  previous value or null / 이전 값 또는 null
     */
    final void recordAttribute(Element target, String local, String namespace, String oldValue) {
        if (mutationListeners.isEmpty()) {
            return;
        }
        deliver(new MutationRecord(MutationRecord.ATTRIBUTES, target, new ArrayList<Node>(0), new ArrayList<Node>(0), null, null, local,
                namespace, oldValue), target.isConnected());
    }

    /**
     * Reports a change of character data.
     * <p>
     * 문자 데이터 변경을 보고한다.
     *
     * @param target   character node / 문자 노드
     * @param oldValue previous data / 이전 데이터
     */
    final void recordCharacterData(CharacterData target, String oldValue) {
        if (mutationListeners.isEmpty()) {
            return;
        }
        deliver(new MutationRecord(MutationRecord.CHARACTER_DATA, target, new ArrayList<Node>(0), new ArrayList<Node>(0), null, null, null,
                null, oldValue), target.isConnected());
    }

    /**
     * Passes a record to all listeners; a failing listener is reported and does not stop the others.
     * <p>
     * 기록을 모든 리스너에 전달한다. 실패한 리스너는 보고되며 다른 리스너를 막지 않는다.
     *
     * @param record    the record / 기록
     * @param connected whether the changed node is connected / 변경된 노드가 연결되어 있는지 여부
     */
    private void deliver(MutationRecord record, boolean connected) {
        List<DomMutationListener> snapshot = mutationListeners;
        // 등록된 리스너를 차례로 호출한다. 목록은 쓰기 시 복사하므로 순회 중 변경에 안전하다.
        for (int i = 0; i < snapshot.size(); i++) {
            try {
                snapshot.get(i).mutated(record, connected);
            } catch (RuntimeException e) {
                errorHandler.listenerFailed(this, null, e);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 다른 계층이 설치하는 서비스
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Installs the element factory (the HTML layer installs the one for specialized HTML elements); null restores the plain one.
     * <p>
     * 요소 팩터리를 설치한다(HTML 계층이 특화된 HTML 요소용 팩터리를 설치한다). null 이면 기본 팩터리로 되돌린다.
     *
     * @param factory factory or null / 팩터리 또는 null
     */
    public void setElementFactory(ElementFactory factory) {
        this.elementFactory = factory == null ? PLAIN_ELEMENTS : factory;
    }

    /**
     * Returns the support object of the HTML form controls of this document, creating it when needed. It owns the form host, the
     * observation of the tree changes that radio button groups and <code>select</code> elements depend on, and the state version.
     * <p>
     * 이 문서의 HTML 폼 컨트롤 지원 객체를 반환하며 필요하면 만든다. 폼 호스트, 라디오 버튼 그룹과 <code>select</code> 요소가 의존하는 트리 변경의 관찰,
     * 상태 버전을 가진다.
     *
     * @return the support object / 지원 객체
     */
    public final org.duckdns.hjow.swingwebpanel.dom.html.FormSupport getFormSupport() {
        if (formSupport == null) {
            formSupport = new org.duckdns.hjow.swingwebpanel.dom.html.FormSupport(this);
        }
        return formSupport;
    }

    /**
     * Installs the host of the form controls of this document (navigation of submissions, validation bubbles, file choosers, state change
     * notifications); null removes it.
     * <p>
     * 이 문서의 폼 컨트롤의 호스트(제출 이동, 검증 말풍선, 파일 선택기, 상태 변경 통지)를 설치한다. null 이면 제거한다.
     *
     * @param host the host or null / 호스트 또는 null
     */
    public void setFormHost(org.duckdns.hjow.swingwebpanel.dom.html.FormHost host) {
        getFormSupport().setHost(host);
    }

    /**
     * Returns the host of the form controls, or null if none was installed.
     * <p>
     * 폼 컨트롤의 호스트를 반환한다. 설치하지 않았으면 null.
     *
     * @return the host or null / 호스트 또는 null
     */
    public org.duckdns.hjow.swingwebpanel.dom.html.FormHost getFormHost() {
        return formSupport == null ? null : formSupport.getHost();
    }

    /**
     * Installs the selector engine.
     * <p>
     * 선택자 엔진을 설치한다.
     *
     * @param engine engine or null / 엔진 또는 null
     */
    public void setSelectorEngine(SelectorEngine engine) {
        this.selectorEngine = engine;
    }

    /**
     * Returns the installed selector engine, or null.
     * <p>
     * 설치된 선택자 엔진을 반환한다. 없으면 null.
     *
     * @return engine or null / 엔진 또는 null
     */
    public SelectorEngine getSelectorEngine() {
        return selectorEngine;
    }

    /**
     * Returns the selector engine or throws a NotSupportedError.
     * <p>
     * 선택자 엔진을 반환하거나 NotSupportedError 를 던진다.
     *
     * @return engine / 엔진
     */
    final SelectorEngine requireSelectorEngine() {
        if (selectorEngine == null) {
            throw DomException.notSupported("selectors are not available in this document");
        }
        return selectorEngine;
    }

    /**
     * Installs the markup parser and serializer.
     * <p>
     * 마크업 파서와 직렬화기를 설치한다.
     *
     * @param support support object or null / 지원 객체 또는 null
     */
    public void setMarkupSupport(MarkupSupport support) {
        this.markupSupport = support;
    }

    /**
     * Returns the markup support or throws a NotSupportedError.
     * <p>
     * 마크업 지원을 반환하거나 NotSupportedError 를 던진다.
     *
     * @return support / 지원 객체
     */
    final MarkupSupport requireMarkupSupport() {
        if (markupSupport == null) {
            throw DomException.notSupported("markup parsing and serialization are not available in this document");
        }
        return markupSupport;
    }

    /**
     * Installs the handler that receives failures of event listeners; null restores the printing handler.
     * <p>
     * 이벤트 리스너의 실패를 받는 핸들러를 설치한다. null 이면 출력 핸들러로 되돌린다.
     *
     * @param handler handler or null / 핸들러 또는 null
     */
    public void setListenerErrorHandler(ListenerErrorHandler handler) {
        this.errorHandler = handler == null ? PRINTING_HANDLER : handler;
    }

    /**
     * Returns the handler for failures of event listeners of this document's nodes.
     * <p>
     * 이 문서의 노드의 이벤트 리스너 실패를 받는 핸들러를 반환한다.
     *
     * @return handler / 핸들러
     */
    final ListenerErrorHandler getListenerErrorHandlerInternal() {
        return errorHandler;
    }

    /**
     * Sets the window that follows the document in the event path (all events except <code>load</code> propagate from the document to it).
     * <p>
     * 이벤트 경로에서 문서 뒤에 오는 window 를 지정한다(<code>load</code> 를 제외한 모든 이벤트가 문서에서 그것으로 전파된다).
     *
     * @param window event target or null / 이벤트 대상 또는 null
     */
    public void setEventWindow(EventTarget window) {
        this.eventWindow = window;
    }

    /**
     * Returns the window as the next event target, except for <code>load</code> events.
     * <p>
     * <code>load</code> 이벤트를 제외하고 window 를 다음 이벤트 대상으로 반환한다.
     *
     * @param event the event / 이벤트
     * @return the window or null / window 또는 null
     */
    protected EventTarget getEventParent(Event event) {
        return eventWindow != null && !"load".equals(event.getType()) ? eventWindow : null;
    }

    /**
     * Creates an empty document with the same type and settings.
     * <p>
     * 같은 종류와 설정의 빈 문서를 만든다.
     *
     * @param target ignored (a document is its own owner) / 무시한다(문서는 자기 자신이 소유자)
     * @return the copy / 복사본
     */
    Node shallowCopy(Document target) {
        Document d = new Document(htmlDocument, contentType);
        d.url = url;
        d.baseUrl = baseUrl;
        d.characterSet = characterSet;
        d.compatibilityMode = compatibilityMode;
        d.elementFactory = elementFactory;
        return d;
    }

    /**
     * Compares the content type of two documents.
     * <p>
     * 두 문서의 콘텐츠 타입을 비교한다.
     *
     * @param other document / 문서
     * @return true if equal / 같으면 true
     */
    boolean equalOwnData(Node other) {
        return contentType.equals(((Document) other).contentType);
    }
}
