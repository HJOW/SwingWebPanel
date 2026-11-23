package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.html.ParserListener;

/**
 * The parser for <code>application/xhtml+xml</code> documents (plan item P17-05): XML syntax in, the project's own DOM
 * (<code>org.duckdns.hjow.swingwebpanel.dom</code>) out. The tokenizing is done by the XML parser of the Java platform through the public JAXP/SAX API
 * (<code>javax.xml.parsers</code>, <code>org.xml.sax</code>); everything the document can name from outside is switched off, and the tree
 * is built by {@link XhtmlBuilder} into the same node classes that the HTML parser produces.
 * <p>
 * Rules that this class guarantees:
 * <ul>
 * <li><b>No error recovery.</b> XML names are case sensitive and the first well-formedness error ends the parse with a structured
 * {@link XmlParseError} (line, column, message, source line) in the result. There is no fall back to the forgiving HTML parser.</li>
 * <li><b>Nothing outside the text is read.</b> The external DTD subset and external entities are never loaded, XInclude is off, and
 * every request that reaches the entity resolver gets an empty answer (and is listed in {@link XhtmlResult#getRefusedReferences()}).
 * The external identifiers of the DOCTYPE are removed from the text before the XML parser sees it (the DOCTYPE node of the DOM keeps
 * them), so the parser cannot even try to open them. The default limits that the JRE puts on entity expansion (64,000 references in JDK 6, less in newer JREs) are lifted (<code>secure-processing</code> off, and the <code>jdk.xml</code> properties where the JRE has them) because {@link XhtmlLimits} replaces them, with a count of every entity reference.</li>
 * <li><b>Named references.</b> See {@link XhtmlEntities}: XHTML, HTML, MathML and SVG DTD identifiers get the HTML character reference
 * names; any other undeclared entity is an error.</li>
 * <li><b>Bounded work.</b> {@link XhtmlLimits} bound input size, depth, node count, attribute count, expanded text size and time;
 * an entity bomb ends with {@link XmlParseError.Kind#LIMIT_EXCEEDED}.</li>
 * <li><b>Document.</b> The document has the content type <code>application/xhtml+xml</code>, is not an HTML document (names are case
 * sensitive), is in no-quirks mode and gets its character set from the encoding decision ({@link XmlDecoder}). The XML declaration is
 * not a node; comments, processing instructions (<code>xml-stylesheet</code> included, which this class does not interpret) and the
 * document type are.</li>
 * </ul>
 * Typical use:
 * <pre>
 * XhtmlParser p = new XhtmlParser();
 * p.setListener(scripts.createParserListener());              // optional, same callbacks as HtmlParser
 * XhtmlResult r = p.parse(bytes, "UTF-8", "http://host/page.xhtml");
 * if (!r.isWellFormed()) { show an error page from r.getError(); } else { use r.getDocument(); }
 * </pre>
 * Threading: an instance is used by one thread and parses once; the listener is called on that thread in document order. Because an XML
 * parse cannot be suspended, the {@link org.duckdns.hjow.swingwebpanel.html.ParserControl} that the listener receives ignores <code>pause()</code> and
 * <code>insertAtInsertionPoint</code>; an external script cannot hold up the parse and must be handled after it.
 * <p>
 * <code>application/xhtml+xml</code> 문서의 파서(계획 P17-05): XML 구문을 받아 프로젝트 자체 DOM(<code>org.duckdns.hjow.swingwebpanel.dom</code>)을 만든다. 토큰화는 Java
 * 플랫폼의 XML 파서를 공개 JAXP/SAX API(<code>javax.xml.parsers</code>, <code>org.xml.sax</code>)로 써서 하며, 문서가 바깥에서 가져올 수 있는 모든 것은
 * 꺼 두고, 트리는 {@link XhtmlBuilder} 가 HTML 파서가 만드는 것과 같은 노드 클래스로 만든다.
 * <p>
 * 이 클래스가 보장하는 규칙:
 * <ul>
 * <li><b>오류 복구 없음.</b> XML 이름은 대소문자를 구분하고 첫 well-formed 오류가 해석을 끝내며 결과에 구조화된 {@link XmlParseError}(줄, 열, 메시지,
 * 소스 줄)가 담긴다. 관대한 HTML 파서로 되돌아가지 않는다.</li>
 * <li><b>텍스트 밖은 읽지 않는다.</b> 외부 DTD 서브셋과 외부 엔티티는 절대 읽지 않고, XInclude 는 꺼져 있으며, 엔티티 해석기에 닿는 모든 요청에는 빈 답을
 * 준다({@link XhtmlResult#getRefusedReferences()} 에 기록). DOCTYPE 의 외부 식별자는 XML 파서가 보기 전에 텍스트에서 제거하므로(DOM 의 DOCTYPE 노드는
 * 그대로 가진다) 파서가 열어 보려는 시도조차 하지 못한다. JRE 가 엔티티 확장에 두는 기본 상한(JDK 6 은 6만 4천 번, 새 JRE 는 더 적다)은 풀어 둔다(<code>secure-processing</code> 끄기와, 있는 JRE 에서는 <code>jdk.xml</code> 속성). 모든 엔티티 참조를 세는 {@link XhtmlLimits} 가 대신하기 때문이다.</li>
 * <li><b>이름 있는 참조.</b> {@link XhtmlEntities} 참조: XHTML, HTML, MathML, SVG DTD 식별자에는 HTML 문자 참조 이름을 주고 그 밖의 선언되지 않은
 * 엔티티는 오류이다.</li>
 * <li><b>한정된 작업.</b> {@link XhtmlLimits} 가 입력 크기, 깊이, 노드 수, 속성 수, 펼친 텍스트 크기, 시간을 제한한다. 엔티티 폭탄은
 * {@link XmlParseError.Kind#LIMIT_EXCEEDED} 로 끝난다.</li>
 * <li><b>문서.</b> 문서의 콘텐츠 타입은 <code>application/xhtml+xml</code>, HTML 문서가 아니며(이름은 대소문자 구분), no-quirks 모드이고, 문자셋은 인코딩
 * 판정({@link XmlDecoder})에서 얻는다. XML 선언은 노드가 아니다. 주석, 처리 명령(이 클래스가 해석하지 않는 <code>xml-stylesheet</code> 포함), 문서 유형은
 * 노드이다.</li>
 * </ul>
 * 사용 예는 위의 영문 코드와 같다.
 * <p>
 * 스레드: 인스턴스는 한 스레드가 쓰며 한 번만 해석한다. 리스너는 그 스레드에서 문서 순서대로 호출된다. XML 해석은 중단할 수 없으므로 리스너가 받는
 * {@link org.duckdns.hjow.swingwebpanel.html.ParserControl} 은 <code>pause()</code> 와 <code>insertAtInsertionPoint</code> 를 무시한다. 외부 스크립트가 해석을 붙잡을 수
 * 없으므로 해석이 끝난 뒤에 처리해야 한다.
 */
public final class XhtmlParser {

    /**
     * The content type of the documents that this parser creates.
     * <p>
     * 이 파서가 만드는 문서의 콘텐츠 타입.
     */
    public static final String CONTENT_TYPE = "application/xhtml+xml";

    /**
     * The shared, configured parser factory (guarded by its own monitor).
     * <p>
     * 공유하는, 설정이 끝난 파서 팩터리(자기 모니터로 보호한다).
     */
    private static SAXParserFactory factory;

    /**
     * The document that receives the nodes.
     * <p>
     * 노드를 받는 문서.
     */
    private final Document doc;

    /**
     * The listener or null.
     * <p>
     * 리스너 또는 null.
     */
    private ParserListener listener;

    /**
     * The limits of the next parse.
     * <p>
     * 다음 해석의 상한.
     */
    private XhtmlLimits limits = new XhtmlLimits();

    /**
     * True after {@link #parse(String, String)} or {@link #parse(byte[], String, String)} was called.
     * <p>
     * 해석 메서드를 호출한 뒤에는 true.
     */
    private boolean used;

    /**
     * Creates a parser with a new empty document of the content type {@link #CONTENT_TYPE}.
     * <p>
     * 콘텐츠 타입이 {@link #CONTENT_TYPE} 인 새 빈 문서를 가진 파서를 만든다.
     */
    public XhtmlParser() {
        this(Document.createXmlDocument(CONTENT_TYPE));
    }

    /**
     * Creates a parser for a document that the caller has prepared (for example with a selector engine installed). The document must be an
     * XML document without children.
     * <p>
     * 호출자가 준비한 문서(예: 선택자 엔진이 설치된 문서)를 위한 파서를 만든다. 문서는 자식이 없는 XML 문서여야 한다.
     *
     * @param document the empty XML document / 빈 XML 문서
     * @throws IllegalArgumentException if the document is null, an HTML document or not empty / 문서가 null, HTML 문서, 비어 있지 않은 문서이면
     */
    public XhtmlParser(Document document) {
        if (document == null || document.isHtmlDocument() || document.getFirstChild() != null) {
            throw new IllegalArgumentException("an empty XML document is required");
        }
        this.doc = document;
    }

    /**
     * Returns the document that is (or will be) filled by the parse. The caller may install services on it before the parse.
     * <p>
     * 해석이 채우는(채울) 문서를 반환한다. 호출자는 해석 전에 여기에 서비스를 설치할 수 있다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Sets the listener that receives the same notifications as for HTML (scripts, style sheets, title, base URL and so on); call before
     * parsing.
     * <p>
     * HTML 과 같은 통지(스크립트, 스타일시트, 제목, 기준 URL 등)를 받는 리스너를 지정한다. 해석 전에 호출한다.
     *
     * @param l the listener or null / 리스너 또는 null
     */
    public void setListener(ParserListener l) {
        this.listener = l;
    }

    /**
     * Sets the limits of the parse (copied when the parse starts); call before parsing.
     * <p>
     * 해석의 상한을 지정한다(해석을 시작할 때 복사한다). 해석 전에 호출한다.
     *
     * @param value the limits, not null / 상한(null 불가)
     * @throws IllegalArgumentException if the value is null / null 이면
     */
    public void setLimits(XhtmlLimits value) {
        if (value == null) {
            throw new IllegalArgumentException("limits must not be null");
        }
        this.limits = new XhtmlLimits(value);
    }

    /**
     * Returns a copy of the limits.
     * <p>
     * 상한의 복사본을 반환한다.
     *
     * @return the limits / 상한
     */
    public XhtmlLimits getLimits() {
        return new XhtmlLimits(limits);
    }

    /**
     * Decodes the bytes of a response (see {@link XmlDecoder}) and parses them.
     * <p>
     * 응답 바이트를 디코딩하고({@link XmlDecoder} 참조) 해석한다.
     *
     * @param bytes       the response body / 응답 본문
     * @param httpCharset the charset label of the Content-Type header or null / Content-Type 헤더의 charset 라벨 또는 null
     * @param url         the document URL or null / 문서 URL 또는 null
     * @return the result; it never throws for malformed input / 결과. 잘못된 입력에도 예외를 던지지 않는다
     * @throws IllegalStateException if this parser has parsed already / 이 파서가 이미 해석했으면
     */
    public XhtmlResult parse(byte[] bytes, String httpCharset, String url) {
        return parseText(XmlDecoder.decode(bytes, httpCharset), url);
    }

    /**
     * Parses text that is already decoded (for example the argument of <code>setContent</code>); the XML declaration's encoding is
     * ignored. A leading byte order mark character is skipped.
     * <p>
     * 이미 디코딩된 텍스트(예: <code>setContent</code> 의 인수)를 해석한다. XML 선언의 인코딩은 무시한다. 앞의 바이트 순서 표시 문자는 건너뛴다.
     *
     * @param text the document text / 문서 텍스트
     * @param url  the document URL or null / 문서 URL 또는 null
     * @return the result; it never throws for malformed input / 결과. 잘못된 입력에도 예외를 던지지 않는다
     * @throws IllegalArgumentException if the text is null / 텍스트가 null 이면
     * @throws IllegalStateException    if this parser has parsed already / 이 파서가 이미 해석했으면
     */
    public XhtmlResult parse(String text, String url) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return parseText(XmlText.ofString(text), url);
    }

    /**
     * Parses text with a new parser (shortcut).
     * <p>
     * 새 파서로 텍스트를 해석한다(줄임).
     *
     * @param text the document text / 문서 텍스트
     * @param url  the document URL or null / 문서 URL 또는 null
     * @return the result / 결과
     */
    public static XhtmlResult parseDocument(String text, String url) {
        return new XhtmlParser().parse(text, url);
    }

    /**
     * Parses bytes with a new parser (shortcut).
     * <p>
     * 새 파서로 바이트를 해석한다(줄임).
     *
     * @param bytes       the response body / 응답 본문
     * @param httpCharset the charset label or null / charset 라벨 또는 null
     * @param url         the document URL or null / 문서 URL 또는 null
     * @return the result / 결과
     */
    public static XhtmlResult parseDocument(byte[] bytes, String httpCharset, String url) {
        return new XhtmlParser().parse(bytes, httpCharset, url);
    }

    /**
     * Where the original text and the text given to the XML parser differ: the rewritten DOCTYPE and a skipped byte order mark
     * character change columns on one line.
     * <p>
     * 원문 텍스트와 XML 파서에 준 텍스트가 다른 곳: 다시 쓴 DOCTYPE 과 건너뛴 바이트 순서 표시 문자는 한 줄의 열을 바꾼다.
     */
    private static final class Mapping {
        /**
         * The text that the XML parser reads.
         * <p>
         * XML 파서가 읽는 텍스트.
         */
        String text;

        /**
         * The line (1 based) on which the rewritten DOCTYPE ends, or 0 if it was not rewritten.
         * <p>
         * 다시 쓴 DOCTYPE 이 끝나는 줄(1부터). 다시 쓰지 않았으면 0.
         */
        int line;

        /**
         * Column after the DOCTYPE in the rewritten text.
         * <p>
         * 다시 쓴 텍스트에서 DOCTYPE 다음의 열.
         */
        int newColumn;

        /**
         * Column after the DOCTYPE in the original text.
         * <p>
         * 원문 텍스트에서 DOCTYPE 다음의 열.
         */
        int oldColumn;

        /**
         * True if a leading byte order mark character was removed.
         * <p>
         * 앞의 바이트 순서 표시 문자를 제거했으면 true.
         */
        boolean bomSkipped;

        /**
         * Maps a column of the XML parser's text back to the original text.
         * <p>
         * XML 파서 텍스트의 열을 원문 텍스트의 열로 되돌린다.
         *
         * @param l line reported by the XML parser / XML 파서가 보고한 줄
         * @param c column reported by the XML parser / XML 파서가 보고한 열
         * @return the column in the original text / 원문 텍스트의 열
         */
        int column(int l, int c) {
            int result = c;
            if (line > 0 && l == line && c >= newColumn) {
                result = c - newColumn + oldColumn;
            }
            if (bomSkipped && l == 1 && c > 0) {
                result++;
            }
            return result;
        }
    }

    /**
     * The common part of both parse methods.
     * <p>
     * 두 해석 메서드의 공통 부분.
     *
     * @param decoded the decoded text / 디코딩한 텍스트
     * @param url     document URL or null / 문서 URL 또는 null
     * @return the result / 결과
     */
    private XhtmlResult parseText(XmlText decoded, String url) {
        if (used) {
            throw new IllegalStateException("an XhtmlParser parses only once");
        }
        used = true;
        XhtmlLimits lim = new XhtmlLimits(limits);
        if (url != null) {
            doc.setUrl(url);
        }
        if (!XmlText.SOURCE_STRING.equals(decoded.getSource())) {
            doc.setCharacterSet(decoded.getEncoding());
        }
        String original = decoded.getText();
        boolean bom = original.length() > 0 && original.charAt(0) == (char) 0xFEFF;
        String xml = bom ? original.substring(1) : original;
        List<String> refused = new ArrayList<String>();
        if (xml.length() > lim.getMaxChars()) {
            XmlParseError e = new XmlParseError(XmlParseError.Kind.LIMIT_EXCEEDED,
                    "The document is larger than " + lim.getMaxChars() + " characters", 1, 1, original);
            return new XhtmlResult(doc, decoded, e, refused, false);
        }
        XmlProlog prolog = XmlProlog.scan(xml);
        if (prolog.isDoctypeMalformed()) {
            int[] at = positionOf(xml, prolog.getDoctypeStart());
            XmlParseError e = new XmlParseError(XmlParseError.Kind.NOT_WELL_FORMED, "The DOCTYPE declaration is not well-formed",
                    at[0], at[1] + (bom && at[0] == 1 ? 1 : 0), original);
            return new XhtmlResult(doc, decoded, e, refused, false);
        }
        Mapping map = rewrite(xml, prolog);
        map.bomSkipped = bom;
        XhtmlBuilder builder = new XhtmlBuilder(doc, listener, prolog, lim);
        XmlParseError error = run(builder, map, original);
        if (decoded.hasError()) {
            XmlParseError enc = new XmlParseError(XmlParseError.Kind.ENCODING, decoded.getErrorMessage(), decoded.getErrorLine(),
                    decoded.getErrorColumn(), original);
            // 인코딩 오류 앞에서 난 해석 오류는 그대로 두고, 그렇지 않으면 인코딩 오류가 진짜 원인이다(잘린 텍스트의 끝 오류는 그 결과일 뿐이다).
            if (error == null || !before(error, enc)) {
                error = enc;
            }
        }
        refused.addAll(builder.getRefused());
        // 문서가 DOCTYPE 에서 지정한 외부 식별자도 요청한 것으로 기록한다(읽지는 않았다).
        if (prolog.hasDoctype() && prolog.hasExternalId()) {
            refused.add(prolog.getSystemId() != null ? prolog.getSystemId() : prolog.getPublicId());
        }
        Element root = doc.getDocumentElement();
        boolean xhtml = root != null && Namespaces.HTML.equals(root.getNamespaceURI()) && "html".equals(root.getLocalName());
        return new XhtmlResult(doc, decoded, error, refused, xhtml);
    }

    /**
     * Tells whether an error is located before another one.
     * <p>
     * 오류가 다른 오류보다 앞에 있는지 알려 준다.
     *
     * @param a first error / 첫 오류
     * @param b second error / 둘째 오류
     * @return true if a is strictly before b / a 가 b 보다 엄격히 앞이면 true
     */
    private static boolean before(XmlParseError a, XmlParseError b) {
        return a.getLine() < b.getLine() || a.getLine() == b.getLine() && a.getColumn() < b.getColumn();
    }

    /**
     * Runs the XML parser over the prepared text and converts every way of failing into an {@link XmlParseError}.
     * <p>
     * 준비한 텍스트에 XML 파서를 돌리고 모든 실패를 {@link XmlParseError} 로 바꾼다.
     *
     * @param builder  the handler / 핸들러
     * @param map      the text for the parser and the position mapping / 파서에 줄 텍스트와 위치 대응
     * @param original the original text (for the source line of an error) / 원문 텍스트(오류의 소스 줄용)
     * @return the error or null / 오류 또는 null
     */
    private XmlParseError run(XhtmlBuilder builder, Mapping map, String original) {
        try {
            XMLReader reader = newReader();
            reader.setContentHandler(builder);
            reader.setErrorHandler(builder);
            reader.setEntityResolver(builder);
            reader.setProperty("http://xml.org/sax/properties/lexical-handler", builder);
            reader.setProperty("http://xml.org/sax/properties/declaration-handler", builder);
            reader.parse(new InputSource(new StringReader(map.text)));
            return null;
        } catch (XhtmlBuilder.LimitException e) {
            return new XmlParseError(XmlParseError.Kind.LIMIT_EXCEEDED, e.getMessage(), e.line, map.column(e.line, e.column), original);
        } catch (SAXParseException e) {
            return new XmlParseError(XmlParseError.Kind.NOT_WELL_FORMED, e.getMessage(), e.getLineNumber(),
                    map.column(e.getLineNumber(), e.getColumnNumber()), original);
        } catch (SAXException e) {
            Exception inner = e.getException();
            if (inner instanceof XhtmlBuilder.LimitException) {
                XhtmlBuilder.LimitException le = (XhtmlBuilder.LimitException) inner;
                return new XmlParseError(XmlParseError.Kind.LIMIT_EXCEEDED, le.getMessage(), le.line, map.column(le.line, le.column), original);
            }
            return new XmlParseError(XmlParseError.Kind.INTERNAL, String.valueOf(e.getMessage()), 0, 0, original);
        } catch (IOException e) {
            return new XmlParseError(XmlParseError.Kind.INTERNAL, String.valueOf(e.getMessage()), 0, 0, original);
        } catch (ParserConfigurationException e) {
            return new XmlParseError(XmlParseError.Kind.INTERNAL, "No XML parser is available: " + e.getMessage(), 0, 0, original);
        } catch (StackOverflowError e) {
            return new XmlParseError(XmlParseError.Kind.LIMIT_EXCEEDED, "The XML parser ran out of stack space", 0, 0, original);
        } catch (RuntimeException e) {
            return new XmlParseError(XmlParseError.Kind.INTERNAL, e.getClass().getName() + ": " + e.getMessage(), 0, 0, original);
        }
    }

    /**
     * Creates a new XML reader from the shared factory.
     * <p>
     * 공유 팩터리에서 새 XML 리더를 만든다.
     *
     * @return the reader / 리더
     * @throws ParserConfigurationException if no parser can be created / 파서를 만들 수 없으면
     * @throws SAXException                 if the parser fails to initialize / 파서 초기화에 실패하면
     */
    private static XMLReader newReader() throws ParserConfigurationException, SAXException {
        synchronized (XhtmlParser.class) {
            if (factory == null) {
                factory = configuredFactory();
            }
            SAXParser p = factory.newSAXParser();
            XMLReader reader = p.getXMLReader();
            // 새 JRE 의 파서가 두는 기본 상한(엔티티 개수, 요소 깊이, 속성 수 등)은 풀고 이 클래스의 XhtmlLimits 가 대신 지킨다. 모르는 속성은 건너뛴다.
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/entityExpansionLimit", "0");
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit", "0");
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/maxGeneralEntitySizeLimit", "0");
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/entityReplacementLimit", "0");
            // 이 세 속성은 JDK 8 에서 0 이 "제한 없음"이 아니라 한도 0 으로 읽히므로 가장 큰 값을 준다.
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/maxElementDepth", String.valueOf(Integer.MAX_VALUE));
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/elementAttributeLimit", String.valueOf(Integer.MAX_VALUE));
            setProperty(reader, "http://www.oracle.com/xml/jaxp/properties/maxXMLNameLimit", String.valueOf(Integer.MAX_VALUE));
            return reader;
        }
    }

    /**
     * Creates the parser factory with every protective feature that this JRE knows. A feature that is not recognized is skipped: the
     * protection does not depend on any of them, because the entity resolver of {@link XhtmlBuilder} refuses every external request and
     * the DOCTYPE is rewritten so that it names no external subset.
     * <p>
     * 이 JRE 가 아는 모든 보호 기능을 켠 파서 팩터리를 만든다. 인식하지 못하는 기능은 건너뛴다: {@link XhtmlBuilder} 의 엔티티 해석기가 모든 외부 요청을
     * 거부하고 DOCTYPE 을 다시 써서 외부 서브셋을 지정하지 않으므로 보호는 이 기능들 중 어느 것에도 의존하지 않는다.
     *
     * @return the factory / 팩터리
     */
    private static SAXParserFactory configuredFactory() {
        SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(true);
        f.setValidating(false);
        setFeature(f, "http://xml.org/sax/features/namespace-prefixes", true);
        setFeature(f, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(f, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(f, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        // 해석기의 기본 안전 한도(엔티티 6만 4천 번 등)는 풀고 이 클래스의 XhtmlLimits 가 대신 지킨다. 한도를 풀어도 외부 접근은 열리지 않는다.
        setFeature(f, "http://javax.xml.XMLConstants/feature/secure-processing", false);
        try {
            f.setXIncludeAware(false);
        } catch (UnsupportedOperationException e) {
            // XInclude 를 지원하지 않는 파서는 처음부터 꺼 둔 것과 같으므로 그대로 둔다.
            return f;
        }
        return f;
    }

    /**
     * Sets a property of the XML reader and ignores a property that the parser does not know.
     * <p>
     * XML 리더의 속성을 지정하고, 파서가 모르는 속성이면 무시한다.
     *
     * @param reader the reader / 리더
     * @param name   the property name / 속성 이름
     * @param value  the value / 값
     */
    private static void setProperty(XMLReader reader, String name, String value) {
        try {
            reader.setProperty(name, value);
        } catch (SAXException e) {
            // 이 JRE 에 없는 속성이다(JDK 6 의 파서가 그렇다). 이 클래스의 XhtmlLimits 가 대신 지키므로 건너뛴다.
            return;
        }
    }

    /**
     * Sets a factory feature and ignores a feature that the parser does not know.
     * <p>
     * 팩터리 기능을 지정하고, 파서가 모르는 기능이면 무시한다.
     *
     * @param f     the factory / 팩터리
     * @param name  the feature name / 기능 이름
     * @param value the value / 값
     */
    private static void setFeature(SAXParserFactory f, String name, boolean value) {
        try {
            f.setFeature(name, value);
        } catch (ParserConfigurationException e) {
            // 이 파서가 모르는 기능은 건너뛴다(보호는 해석기 거부와 DOCTYPE 다시 쓰기에도 있다).
            return;
        } catch (SAXException e) {
            return;
        }
    }

    /**
     * Removes the external identifiers from the DOCTYPE and, for a well-known W3C DTD, adds the entity declarations that the DTD would
     * have given (see {@link XhtmlEntities}). The number of line breaks stays the same so that line numbers of errors are right.
     * <p>
     * DOCTYPE 에서 외부 식별자를 제거하고, 잘 알려진 W3C DTD 이면 그 DTD 가 주었을 엔티티 선언을 추가한다({@link XhtmlEntities} 참조). 오류의 줄 번호가
     * 맞도록 줄 바꿈 수는 그대로 둔다.
     *
     * @param xml    the text / 텍스트
     * @param prolog the prolog scan of the text / 텍스트의 프롤로그 검사
     * @return the text for the XML parser and the position mapping / XML 파서에 줄 텍스트와 위치 대응
     */
    private static Mapping rewrite(String xml, XmlProlog prolog) {
        Mapping m = new Mapping();
        m.text = xml;
        if (!prolog.hasDoctype() || !prolog.hasExternalId()) {
            return m;
        }
        String subset = prolog.getInternalSubset();
        StringBuilder d = new StringBuilder("<!DOCTYPE ").append(prolog.getDoctypeName()).append(" [");
        if (subset != null) {
            d.append(subset);
        }
        if (XhtmlEntities.isKnownDtd(prolog.getPublicId(), prolog.getSystemId())) {
            d.append(XhtmlEntities.declarationsFor(xml, subset));
        }
        d.append(']');
        int oldBreaks = countBreaks(xml.substring(prolog.getDoctypeStart(), prolog.getDoctypeEnd()));
        int newBreaks = countBreaks(d.toString());
        // 줄 바꿈이 줄어든 만큼 닫는 대괄호 앞에 채워 넣어 뒤쪽의 줄 번호를 유지한다.
        for (int i = newBreaks; i < oldBreaks; i++) {
            d.append('\n');
        }
        d.append('>');
        StringBuilder out = new StringBuilder(xml.length() + d.length());
        out.append(xml, 0, prolog.getDoctypeStart()).append(d).append(xml, prolog.getDoctypeEnd(), xml.length());
        m.text = out.toString();
        int[] oldEnd = positionOf(xml, prolog.getDoctypeEnd());
        int[] newEnd = positionOf(m.text, prolog.getDoctypeStart() + d.length());
        if (oldEnd[0] == newEnd[0]) {
            m.line = newEnd[0];
            m.newColumn = newEnd[1];
            m.oldColumn = oldEnd[1];
        }
        return m;
    }

    /**
     * Counts line breaks (LF, CR LF and CR each count once).
     * <p>
     * 줄 바꿈을 센다(LF, CR LF, CR 각각 한 번).
     *
     * @param s the text / 텍스트
     * @return the number of line breaks / 줄 바꿈 수
     */
    private static int countBreaks(String s) {
        int n = 0;
        // 문자를 훑으며 줄 바꿈마다 센다. CR LF 는 한 번으로 센다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' && (i + 1 >= s.length() || s.charAt(i + 1) != '\n')) {
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the line and column (both 1 based) of an index, counted like an XML parser does.
     * <p>
     * 위치의 줄과 열(모두 1부터)을 XML 파서처럼 센 값으로 반환한다.
     *
     * @param s     the text / 텍스트
     * @param index the index / 위치
     * @return {line, column} / {줄, 열}
     */
    private static int[] positionOf(String s, int index) {
        int line = 1;
        int col = 1;
        int end = Math.min(index, s.length());
        // 위치까지의 문자를 훑으며 줄과 열을 갱신한다.
        for (int i = 0; i < end; i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\r' && (i + 1 >= s.length() || s.charAt(i + 1) != '\n')) {
                line++;
                col = 1;
            } else if (c != '\r') {
                col++;
            }
        }
        return new int[] {line, col};
    }
}
