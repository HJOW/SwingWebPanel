package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DeclHandler;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.duckdns.hjow.swingwebpanel.dom.DocumentType;
import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.ProcessingInstruction;
import org.duckdns.hjow.swingwebpanel.html.ParserControl;
import org.duckdns.hjow.swingwebpanel.html.ParserListener;
import org.duckdns.hjow.swingwebpanel.html.ParserNodeData;
import org.duckdns.hjow.swingwebpanel.net.UrlUtil;

/**
 * The SAX handler that builds the DOM tree of an XHTML document (plan item P17-05). It creates exactly the nodes that the HTML parser
 * creates for the same markup: elements through {@link Document#createElementInternal(String, String, String)} in the namespace that
 * the document gives them, attributes (namespace declarations included, in the XMLNS namespace), text, comments, CDATA sections,
 * processing instructions and the document type. For <code>script</code>, <code>style</code>, <code>title</code>, <code>link</code>,
 * <code>img</code>, <code>base</code> and <code>meta</code> elements in the XHTML namespace it makes the same notifications to the
 * {@link ParserListener} and sets the same parser flags ({@link ParserNodeData}) as the HTML tree builder, and it puts the children of a
 * <code>template</code> element into the template contents. While building it enforces the {@link XhtmlLimits}; the check happens in the
 * callbacks, so an attack ends at the first callback after the limit is reached. Every request for an external entity or DTD is
 * refused with an empty source, whatever the parser features say.
 * <p>
 * XHTML 문서의 DOM 트리를 만드는 SAX 핸들러(계획 P17-05). 같은 마크업에 대해 HTML 파서가 만드는 것과 같은 노드를 만든다: 문서가 정한 네임스페이스의
 * 요소({@link Document#createElementInternal(String, String, String)}), 속성(네임스페이스 선언은 XMLNS 네임스페이스), 텍스트, 주석, CDATA 섹션, 처리 명령,
 * 문서 유형. XHTML 네임스페이스의 <code>script</code>, <code>style</code>, <code>title</code>, <code>link</code>, <code>img</code>, <code>base</code>,
 * <code>meta</code> 요소에 대해서는 HTML 트리 생성기와 같은 {@link ParserListener} 통지를 하고 같은 파서 플래그({@link ParserNodeData})를 지정하며,
 * <code>template</code> 요소의 자식은 template contents 에 넣는다. 만드는 동안 {@link XhtmlLimits} 를 지키며, 검사는 콜백에서 일어나므로 공격은 상한에
 * 닿은 뒤 첫 콜백에서 끝난다. 외부 엔티티나 DTD 요청은 파서 기능 설정과 관계없이 모두 빈 소스로 거부한다.
 */
final class XhtmlBuilder extends DefaultHandler implements LexicalHandler, DeclHandler {

    /**
     * The exception that ends the parse when a limit is exceeded.
     * <p>
     * 상한을 넘었을 때 해석을 끝내는 예외.
     */
    static final class LimitException extends SAXException {
        /**
         * Serialization version.
         * <p>
         * 직렬화 버전.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Line where the limit was hit, 0 if unknown.
         * <p>
         * 상한에 닿은 줄. 모르면 0.
         */
        final int line;

        /**
         * Column where the limit was hit, 0 if unknown.
         * <p>
         * 상한에 닿은 열. 모르면 0.
         */
        final int column;

        /**
         * Creates the exception.
         * <p>
         * 예외를 만든다.
         *
         * @param message the message / 메시지
         * @param line    the line or 0 / 줄 또는 0
         * @param column  the column or 0 / 열 또는 0
         */
        LimitException(String message, int line, int column) {
            super(message);
            this.line = line;
            this.column = column;
        }
    }

    /**
     * The parser control that is handed to the listener. An XML parse cannot be suspended and the XML syntax has no
     * <code>document.write</code>, so both requests are ignored.
     * <p>
     * 리스너에 넘기는 파서 제어. XML 해석은 중단할 수 없고 XML 구문에는 <code>document.write</code> 가 없으므로 두 요청 모두 무시한다.
     */
    private final class Control implements ParserControl {
        /**
         * Ignored: the XML syntax has no insertion point.
         * <p>
         * 무시한다: XML 구문에는 삽입 지점이 없다.
         *
         * @param text markup text / 마크업 문자열
         */
        public void insertAtInsertionPoint(String text) {
        }

        /**
         * Ignores the request (an XML parse cannot be suspended); the parse goes on.
         * <p>
         * 요청을 무시한다(XML 해석은 중단할 수 없다). 해석은 계속된다.
         */
        public void pause() {
        }

        /**
         * Returns false: whole documents only.
         * <p>
         * false 를 반환한다: 문서 전체만 해석한다.
         *
         * @return false / false
         */
        public boolean isFragment() {
            return false;
        }

        /**
         * Returns the document.
         * <p>
         * 문서를 반환한다.
         *
         * @return the document / 문서
         */
        public Document getDocument() {
            return doc;
        }
    }

    /**
     * The document under construction.
     * <p>
     * 만드는 중인 문서.
     */
    private final Document doc;

    /**
     * The listener or null.
     * <p>
     * 리스너 또는 null.
     */
    private final ParserListener listener;

    /**
     * The prolog scan of the original text (identifiers of the DOCTYPE).
     * <p>
     * 원문 텍스트의 프롤로그 검사 결과(DOCTYPE 의 식별자).
     */
    private final XmlProlog prolog;

    /**
     * The limits of this parse.
     * <p>
     * 이 해석의 상한.
     */
    private final XhtmlLimits limits;

    /**
     * Time after which the parse is stopped, in milliseconds since the epoch.
     * <p>
     * 이 시각이 지나면 해석을 멈춘다(에포크 기준 밀리초).
     */
    private final long deadline;

    /**
     * The control given to the listener.
     * <p>
     * 리스너에 주는 제어.
     */
    private final Control control = new Control();

    /**
     * The stack of open elements.
     * <p>
     * 열린 요소의 스택.
     */
    private final List<Element> stack = new ArrayList<Element>();

    /**
     * Text that has been delivered and not yet turned into a node.
     * <p>
     * 전달되었으나 아직 노드가 되지 않은 텍스트.
     */
    private final StringBuilder pending = new StringBuilder();

    /**
     * The content of the CDATA section that is open, or null.
     * <p>
     * 열려 있는 CDATA 섹션의 내용. 없으면 null.
     */
    private StringBuilder cdata;

    /**
     * The identifiers of external references that were refused.
     * <p>
     * 거부한 외부 참조의 식별자.
     */
    private final List<String> refused = new ArrayList<String>();

    /**
     * The locator of the parser (valid during the callbacks).
     * <p>
     * 파서의 위치 정보(콜백 동안만 유효).
     */
    private Locator locator;

    /**
     * The number of nodes created.
     * <p>
     * 만든 노드 수.
     */
    private int nodes;

    /**
     * The number of text and attribute value characters seen.
     * <p>
     * 지금까지 본 텍스트와 속성 값 문자 수.
     */
    private long chars;

    /**
     * The number of entity references expanded so far.
     * <p>
     * 지금까지 펼친 엔티티 참조 수.
     */
    private int expansions;

    /**
     * The names of the general entities that the document declares (internal subset and the declarations that the parser adds).
     * <p>
     * 문서가 선언한 일반 엔티티의 이름(내부 서브셋과 파서가 추가한 선언).
     */
    private final Set<String> declared = new HashSet<String>();

    /**
     * The names of the declared general entities whose content is outside the document (<code>SYSTEM</code> or <code>PUBLIC</code>).
     * <p>
     * 선언된 일반 엔티티 중 내용이 문서 밖에 있는(<code>SYSTEM</code> 또는 <code>PUBLIC</code>) 것의 이름.
     */
    private final Set<String> external = new HashSet<String>();

    /**
     * Counter of callbacks, used to look at the clock only now and then.
     * <p>
     * 콜백 수. 시계를 가끔만 보려고 쓴다.
     */
    private int ticks;

    /**
     * True after the first <code>base</code> element with an <code>href</code>.
     * <p>
     * 첫 <code>href</code> 가 있는 <code>base</code> 요소를 지난 뒤에는 true.
     */
    private boolean baseSeen;

    /**
     * True after the first <code>title</code> element has been reported.
     * <p>
     * 첫 <code>title</code> 요소를 보고한 뒤에는 true.
     */
    private boolean titleSeen;

    /**
     * The fatal error that the parser reported, or null.
     * <p>
     * 파서가 보고한 치명적 오류. 없으면 null.
     */
    private SAXParseException fatal;

    /**
     * Creates the handler.
     * <p>
     * 핸들러를 만든다.
     *
     * @param doc      the empty document / 빈 문서
     * @param listener the listener or null / 리스너 또는 null
     * @param prolog   the prolog scan of the original text / 원문 텍스트의 프롤로그 검사
     * @param limits   the limits (copied by the caller) / 상한(호출자가 복사한 것)
     */
    XhtmlBuilder(Document doc, ParserListener listener, XmlProlog prolog, XhtmlLimits limits) {
        this.doc = doc;
        this.listener = listener;
        this.prolog = prolog;
        this.limits = limits;
        this.deadline = System.currentTimeMillis() + limits.getMaxMillis();
    }

    /**
     * Returns the fatal error that the parser reported, or null.
     * <p>
     * 파서가 보고한 치명적 오류를 반환한다. 없으면 null.
     *
     * @return the error or null / 오류 또는 null
     */
    SAXParseException getFatal() {
        return fatal;
    }

    /**
     * Returns the identifiers of the refused external references.
     * <p>
     * 거부한 외부 참조의 식별자를 반환한다.
     *
     * @return the identifiers / 식별자
     */
    List<String> getRefused() {
        return refused;
    }

    /**
     * Returns the number of nodes created so far.
     * <p>
     * 지금까지 만든 노드 수를 반환한다.
     *
     * @return the count / 개수
     */
    int getNodeCount() {
        return nodes;
    }

    /**
     * Remembers the locator.
     * <p>
     * 위치 정보를 기억한다.
     *
     * @param l the locator / 위치 정보
     */
    public void setDocumentLocator(Locator l) {
        this.locator = l;
    }

    /**
     * Fixes the document mode: XML documents are always in no-quirks mode.
     * <p>
     * 문서 모드를 정한다: XML 문서는 항상 no-quirks 모드이다.
     */
    public void startDocument() {
        doc.setCompatibilityMode(Document.NO_QUIRKS_MODE);
        if (listener != null) {
            listener.documentModeDetermined(Document.NO_QUIRKS_MODE);
        }
    }

    /**
     * Refuses an external entity or DTD: nothing outside the document text is ever read.
     * <p>
     * 외부 엔티티나 DTD 를 거부한다: 문서 텍스트 밖의 어떤 것도 읽지 않는다.
     *
     * @param publicId public identifier or null / 공개 식별자 또는 null
     * @param systemId system identifier or null / 시스템 식별자 또는 null
     * @return an empty source / 빈 소스
     */
    public InputSource resolveEntity(String publicId, String systemId) {
        refused.add(systemId != null ? systemId : String.valueOf(publicId));
        return new InputSource(new StringReader(""));
    }

    /**
     * Counts a callback and checks the clock now and then.
     * <p>
     * 콜백을 세고 가끔 시계를 확인한다.
     *
     * @throws SAXException if the time limit is exceeded / 시간 상한을 넘으면
     */
    private void tick() throws SAXException {
        ticks++;
        if ((ticks & 0x3FF) == 0 && System.currentTimeMillis() > deadline) {
            throw limit("The document took more than " + limits.getMaxMillis() + " ms to parse");
        }
    }

    /**
     * Creates the exception for an exceeded limit with the current position.
     * <p>
     * 현재 위치가 담긴, 상한 초과 예외를 만든다.
     *
     * @param message the message / 메시지
     * @return the exception / 예외
     */
    private LimitException limit(String message) {
        int line = locator == null ? 0 : locator.getLineNumber();
        int col = locator == null ? 0 : locator.getColumnNumber();
        return new LimitException(message, line, col);
    }

    /**
     * Counts a created node and checks the limit.
     * <p>
     * 만든 노드를 세고 상한을 확인한다.
     *
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    private void countNode() throws SAXException {
        nodes++;
        if (nodes > limits.getMaxNodes()) {
            throw limit("The document has more than " + limits.getMaxNodes() + " nodes");
        }
    }

    /**
     * Counts characters of text or attribute values and checks the limit.
     * <p>
     * 텍스트나 속성 값의 문자를 세고 상한을 확인한다.
     *
     * @param n number of characters / 문자 수
     * @throws SAXException if there are too many characters (an entity expansion bomb) / 문자가 너무 많으면(엔티티 확장 폭탄)
     */
    private void countChars(int n) throws SAXException {
        chars += n;
        if (chars > limits.getMaxTextChars()) {
            throw limit("The document expands to more than " + limits.getMaxTextChars() + " characters of text");
        }
    }

    /**
     * Returns the node that children are inserted into: the template contents for a <code>template</code>, otherwise the element.
     * <p>
     * 자식을 넣을 노드를 반환한다: <code>template</code> 이면 template contents, 아니면 요소 자신.
     *
     * @param e the open element / 열린 요소
     * @return the insertion target / 삽입 대상
     */
    private static Node targetOf(Element e) {
        if (Namespaces.HTML.equals(e.getNamespaceURI()) && "template".equals(e.getLocalName())) {
            return ParserNodeData.ensureTemplateContents(e);
        }
        return e;
    }

    /**
     * Appends a node to the current open element, or to the document if no element is open.
     * <p>
     * 노드를 현재 열린 요소에, 열린 요소가 없으면 문서에 덧붙인다.
     *
     * @param child the node / 노드
     */
    private void append(Node child) {
        Node parent = stack.isEmpty() ? doc : targetOf(stack.get(stack.size() - 1));
        parent.appendChild(child);
    }

    /**
     * Turns the pending text into a text node.
     * <p>
     * 대기 중인 텍스트를 텍스트 노드로 만든다.
     *
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    private void flushText() throws SAXException {
        if (pending.length() > 0 && !stack.isEmpty()) {
            countNode();
            append(doc.createTextNode(pending.toString()));
        }
        pending.setLength(0);
    }

    /**
     * Builds an element and its attributes, inserts it and notifies the listener.
     * <p>
     * 요소와 속성을 만들어 삽입하고 리스너에 알린다.
     *
     * @param uri       namespace URI or "" / 네임스페이스 URI 또는 ""
     * @param localName local name / 로컬 이름
     * @param qName     qualified name / 정규화된 이름
     * @param atts      attributes / 속성
     * @throws SAXException if a limit is exceeded / 상한을 넘으면
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        flushText();
        tick();
        if (stack.size() >= limits.getMaxDepth()) {
            throw limit("The elements are nested deeper than " + limits.getMaxDepth() + " levels");
        }
        if (atts.getLength() > limits.getMaxAttributes()) {
            throw limit("An element has more than " + limits.getMaxAttributes() + " attributes");
        }
        countNode();
        String ns = uri == null || uri.length() == 0 ? null : uri;
        int colon = qName.indexOf(':');
        String prefix = colon > 0 ? qName.substring(0, colon) : null;
        String local = localName != null && localName.length() > 0 ? localName : (colon > 0 ? qName.substring(colon + 1) : qName);
        Element el = doc.createElementInternal(ns, prefix, local);
        // 속성을 하나씩 요소에 지정한다. 값의 길이는 상한 검사에 센다.
        for (int i = 0; i < atts.getLength(); i++) {
            countChars(atts.getValue(i).length());
            addAttribute(el, atts.getQName(i), atts.getURI(i), atts.getValue(i));
        }
        boolean html = Namespaces.HTML.equals(ns);
        if (html && "template".equals(local)) {
            ParserNodeData.ensureTemplateContents(el);
        } else if (html && "script".equals(local)) {
            ParserNodeData.scriptFlags(el).setParserInserted(true);
        }
        append(el);
        stack.add(el);
        if (listener != null) {
            listener.elementInserted(el);
            if (html) {
                notifyInserted(el, local);
            }
        }
    }

    /**
     * Sets one attribute; a name that the DOM refuses is set without name checks (the XML parser has already checked it).
     * <p>
     * 속성 하나를 지정한다. DOM 이 거부하는 이름은 이름 검사 없이 지정한다(XML 파서가 이미 검사했다).
     *
     * @param el    the element / 요소
     * @param qName qualified name / 정규화된 이름
     * @param uri   namespace URI or "" / 네임스페이스 URI 또는 ""
     * @param value the value / 값
     */
    private static void addAttribute(Element el, String qName, String uri, String value) {
        try {
            if ("xmlns".equals(qName) || qName.startsWith("xmlns:")) {
                el.setAttributeNS(Namespaces.XMLNS, qName, value);
            } else {
                el.setAttributeNS(uri == null || uri.length() == 0 ? null : uri, qName, value);
            }
        } catch (DomException ex) {
            el.setAttributeUnchecked(qName, value);
        }
    }

    /**
     * Notifies the listener about the elements that the loading model cares about, as the HTML tree builder does after an insertion.
     * <p>
     * HTML 트리 생성기가 삽입 뒤에 하듯, 로딩 모델이 관심을 갖는 요소를 리스너에 알린다.
     *
     * @param el    the element in the XHTML namespace / XHTML 네임스페이스의 요소
     * @param local its local name / 로컬 이름
     */
    private void notifyInserted(Element el, String local) {
        if ("img".equals(local)) {
            listener.imageFound(el);
        } else if ("link".equals(local)) {
            String rel = el.getAttribute("rel");
            if (rel != null && hasToken(rel, "stylesheet")) {
                listener.stylesheetFound(el);
            }
        } else if ("base".equals(local)) {
            handleBase(el);
        } else if ("meta".equals(local)) {
            String httpEquiv = el.getAttribute("http-equiv");
            String content = el.getAttribute("content");
            if (httpEquiv != null && content != null && "refresh".equalsIgnoreCase(httpEquiv.trim())) {
                listener.metaRefresh(content);
            }
        }
    }

    /**
     * The first <code>base</code> element with an <code>href</code> fixes the base URL of the document.
     * <p>
     * <code>href</code> 가 있는 첫 <code>base</code> 요소가 문서의 기준 URL 을 정한다.
     *
     * @param el the element / 요소
     */
    private void handleBase(Element el) {
        String href = el.getAttribute("href");
        if (baseSeen || href == null) {
            return;
        }
        baseSeen = true;
        String resolved = null;
        try {
            resolved = UrlUtil.resolve(doc.getUrl(), href);
        } catch (IllegalArgumentException ex) {
            resolved = null;
        }
        if (resolved != null) {
            doc.setBaseUri(resolved);
            listener.baseHrefChanged(href, resolved);
        }
    }

    /**
     * Tells whether a space separated token list contains a token, ignoring ASCII case.
     * <p>
     * 공백으로 구분한 토큰 목록에 토큰이 있는지 ASCII 대소문자를 무시하고 알려 준다.
     *
     * @param list  token list / 토큰 목록
     * @param token lower case token / 소문자 토큰
     * @return true if present / 있으면 true
     */
    private static boolean hasToken(String list, String token) {
        String[] parts = list.trim().split("[ \t\n\f\r]+");
        // 목록의 토큰을 하나씩 비교한다.
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closes an element and tells the listener that its children are complete.
     * <p>
     * 요소를 닫고 리스너에 자식이 완성되었음을 알린다.
     *
     * @param uri       namespace URI or "" / 네임스페이스 URI 또는 ""
     * @param localName local name / 로컬 이름
     * @param qName     qualified name / 정규화된 이름
     * @throws SAXException if a limit is exceeded / 상한을 넘으면
     */
    public void endElement(String uri, String localName, String qName) throws SAXException {
        flushText();
        tick();
        Element el = stack.remove(stack.size() - 1);
        if (listener == null) {
            return;
        }
        listener.elementClosed(el);
        String ns = el.getNamespaceURI();
        String local = el.getLocalName();
        if (Namespaces.HTML.equals(ns)) {
            if ("style".equals(local)) {
                listener.styleReady(el);
            } else if ("title".equals(local) && !titleSeen) {
                titleSeen = true;
                listener.titleReady(el.getTextContent());
            } else if ("script".equals(local)) {
                listener.scriptReady(el, control);
            }
        } else if (Namespaces.SVG.equals(ns) && "script".equals(local)) {
            listener.scriptReady(el, control);
        }
    }

    /**
     * Collects text; it becomes a text node (or part of a CDATA section) at the next structural event.
     * <p>
     * 텍스트를 모은다. 다음 구조 이벤트에서 텍스트 노드(또는 CDATA 섹션의 일부)가 된다.
     *
     * @param ch    characters / 문자 배열
     * @param start start index / 시작 위치
     * @param len   length / 길이
     * @throws SAXException if the text limit is exceeded / 텍스트 상한을 넘으면
     */
    public void characters(char[] ch, int start, int len) throws SAXException {
        tick();
        countChars(len);
        if (cdata != null) {
            cdata.append(ch, start, len);
        } else if (!stack.isEmpty()) {
            pending.append(ch, start, len);
        }
    }

    /**
     * Treated like {@link #characters(char[], int, int)}: white space in element content is kept in the DOM.
     * <p>
     * {@link #characters(char[], int, int)} 와 같게 다룬다: 요소 내용의 공백은 DOM 에 남긴다.
     *
     * @param ch    characters / 문자 배열
     * @param start start index / 시작 위치
     * @param len   length / 길이
     * @throws SAXException if the text limit is exceeded / 텍스트 상한을 넘으면
     */
    public void ignorableWhitespace(char[] ch, int start, int len) throws SAXException {
        characters(ch, start, len);
    }

    /**
     * Adds a processing instruction (for example <code>xml-stylesheet</code>) as a node.
     * <p>
     * 처리 명령(예: <code>xml-stylesheet</code>)을 노드로 추가한다.
     *
     * @param target the target / 대상
     * @param data   the data / 데이터
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    public void processingInstruction(String target, String data) throws SAXException {
        flushText();
        tick();
        countNode();
        try {
            ProcessingInstruction pi = doc.createProcessingInstruction(target, data == null ? "" : data);
            append(pi);
        } catch (DomException ex) {
            nodes--;
        }
    }

    /**
     * Adds a comment as a node.
     * <p>
     * 주석을 노드로 추가한다.
     *
     * @param ch    characters / 문자 배열
     * @param start start index / 시작 위치
     * @param len   length / 길이
     * @throws SAXException if a limit is exceeded / 상한을 넘으면
     */
    public void comment(char[] ch, int start, int len) throws SAXException {
        flushText();
        tick();
        countChars(len);
        countNode();
        append(doc.createComment(new String(ch, start, len)));
    }

    /**
     * Starts collecting a CDATA section.
     * <p>
     * CDATA 섹션 수집을 시작한다.
     *
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    public void startCDATA() throws SAXException {
        flushText();
        cdata = new StringBuilder();
    }

    /**
     * Ends the CDATA section and adds it as a node.
     * <p>
     * CDATA 섹션을 끝내고 노드로 추가한다.
     *
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    public void endCDATA() throws SAXException {
        String data = cdata == null ? "" : cdata.toString();
        cdata = null;
        countNode();
        append(doc.createCDATASection(data));
    }

    /**
     * Adds the document type node; the identifiers come from the original text, not from the rewritten one.
     * <p>
     * 문서 유형 노드를 추가한다. 식별자는 다시 쓴 텍스트가 아니라 원문 텍스트의 것이다.
     *
     * @param name     DOCTYPE name / DOCTYPE 이름
     * @param publicId public identifier or null / 공개 식별자 또는 null
     * @param systemId system identifier or null / 시스템 식별자 또는 null
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        countNode();
        String pub = prolog.hasDoctype() ? prolog.getPublicId() : publicId;
        String sys = prolog.hasDoctype() ? prolog.getSystemId() : systemId;
        DocumentType dt = doc.createDocumentType(name, pub, sys);
        doc.appendChild(dt);
    }

    /**
     * Not needed.
     * <p>
     * 필요 없다.
     */
    public void endDTD() {
    }

    /**
     * Checks and counts the expansion of a general entity reference. The XML parser silently drops a reference to an entity that is
     * not declared or whose content is external (when the declaration is not read or external entities are off); this method turns
     * both cases into well-formedness errors, so that no text vanishes without a message. Every reference counts against the limit
     * (the references themselves are expanded in place, as browsers do, and leave no node in the DOM).
     * <p>
     * 일반 엔티티 참조의 펼침을 검사하고 센다. XML 파서는 선언되지 않았거나 내용이 외부에 있는 엔티티(선언을 읽지 않거나 외부 엔티티가 꺼진 경우)에 대한
     * 참조를 조용히 버린다. 이 메서드는 두 경우를 모두 well-formed 오류로 바꾸어 메시지 없이 텍스트가 사라지지 않게 한다. 모든 참조를 상한에 맞춰 센다
     * (참조 자체는 브라우저처럼 제자리에서 펼쳐지며 DOM 에 노드를 남기지 않는다).
     *
     * @param name entity name / 엔티티 이름
     * @throws SAXException if the entity is undeclared or external, or if there are too many expansions / 엔티티가 선언되지 않았거나
     *                      외부에 있거나 펼침이 너무 많으면
     */
    public void startEntity(String name) throws SAXException {
        // 일반 엔티티 참조가 펼쳐질 때마다 검사하고 센다(DTD 와 매개변수 엔티티는 제외).
        if (name != null && !name.startsWith("%") && !"[dtd]".equals(name)) {
            if (!declared.contains(name) && !isPredefined(name)) {
                throw new SAXParseException("The entity \"" + name + "\" was referenced, but not declared.", locator);
            }
            if (external.contains(name)) {
                throw new SAXParseException("The entity \"" + name + "\" is an external entity; external entities are never loaded.", locator);
            }
            expansions++;
            tick();
            if (expansions > limits.getMaxEntityExpansions()) {
                throw limit("The document expands more than " + limits.getMaxEntityExpansions() + " entity references");
            }
        }
    }

    /**
     * Tells whether a name is one of the five entities that XML predefines.
     * <p>
     * 이름이 XML 이 미리 정의하는 다섯 엔티티 중 하나인지 알려 준다.
     *
     * @param name entity name / 엔티티 이름
     * @return true if predefined / 미리 정의되었으면 true
     */
    private static boolean isPredefined(String name) {
        return "amp".equals(name) || "lt".equals(name) || "gt".equals(name) || "quot".equals(name) || "apos".equals(name);
    }

    /**
     * Remembers the name of a declared internal general entity.
     * <p>
     * 선언된 내부 일반 엔티티의 이름을 기억한다.
     *
     * @param name  entity name (parameter entities start with %) / 엔티티 이름(매개변수 엔티티는 %로 시작)
     * @param value replacement text / 치환 텍스트
     */
    public void internalEntityDecl(String name, String value) {
        if (!name.startsWith("%")) {
            declared.add(name);
        }
    }

    /**
     * Remembers the name of a declared external entity and the identifier that the document names; nothing is loaded.
     * <p>
     * 선언된 외부 엔티티의 이름과 문서가 지정한 식별자를 기억한다. 아무것도 읽지 않는다.
     *
     * @param name     entity name (parameter entities start with %) / 엔티티 이름(매개변수 엔티티는 %로 시작)
     * @param publicId public identifier or null / 공개 식별자 또는 null
     * @param systemId system identifier or null / 시스템 식별자 또는 null
     */
    public void externalEntityDecl(String name, String publicId, String systemId) {
        refused.add(systemId != null ? systemId : String.valueOf(publicId));
        if (!name.startsWith("%")) {
            declared.add(name);
            external.add(name);
        }
    }

    /**
     * Not needed.
     * <p>
     * 필요 없다.
     *
     * @param name  element name / 요소 이름
     * @param model content model / 콘텐츠 모델
     */
    public void elementDecl(String name, String model) {
    }

    /**
     * Not needed.
     * <p>
     * 필요 없다.
     *
     * @param eName  element name / 요소 이름
     * @param aName  attribute name / 속성 이름
     * @param type   attribute type / 속성 형
     * @param mode   default mode / 기본값 방식
     * @param value  default value / 기본값
     */
    public void attributeDecl(String eName, String aName, String type, String mode, String value) {
    }

    /**
     * Not needed.
     * <p>
     * 필요 없다.
     *
     * @param name entity name / 엔티티 이름
     */
    public void endEntity(String name) {
    }

    /**
     * An entity that the parser skipped is an undeclared entity: a well-formedness error, never silently dropped text.
     * <p>
     * 파서가 건너뛴 엔티티는 선언되지 않은 엔티티이다: 조용히 사라지는 텍스트가 아니라 well-formed 오류이다.
     *
     * @param name entity name / 엔티티 이름
     * @throws SAXException always / 항상
     */
    public void skippedEntity(String name) throws SAXException {
        throw new SAXParseException("The entity \"" + name + "\" is not declared or is an external entity (external entities are never loaded).", locator);
    }

    /**
     * Ignores recoverable errors (this is a non-validating parse).
     * <p>
     * 복구 가능한 오류는 무시한다(검증하지 않는 해석이다).
     *
     * @param e the error / 오류
     */
    public void error(SAXParseException e) {
    }

    /**
     * Remembers the fatal error and ends the parse.
     * <p>
     * 치명적 오류를 기억하고 해석을 끝낸다.
     *
     * @param e the error / 오류
     * @throws SAXException always / 항상
     */
    public void fatalError(SAXParseException e) throws SAXException {
        this.fatal = e;
        throw e;
    }

    /**
     * Turns the last pending text into a node at the end of the document.
     * <p>
     * 문서 끝에서 마지막 대기 텍스트를 노드로 만든다.
     *
     * @throws SAXException if there are too many nodes / 노드가 너무 많으면
     */
    public void endDocument() throws SAXException {
        flushText();
    }
}
