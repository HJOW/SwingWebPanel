package org.duckdns.hjow.swingwebpanel.html;

import java.util.List;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DocumentFragment;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.dom.Node;

/**
 * The HTML parser: it connects the encoding detection ({@link EncodingSniffer}), the input preprocessing
 * ({@link InputPreprocessor}), the {@link HtmlTokenizer} and the {@link HtmlTreeBuilder} and builds a DOM tree of
 * <code>org.duckdns.hjow.swingwebpanel.dom</code> from markup. It is implemented from scratch after the parsing chapter of the HTML Standard; nothing
 * of the JRE's HTML or XML parsers is used.
 * <p>
 * Typical use:
 * <pre>
 * Document doc = HtmlParser.parseDocument("&lt;p&gt;Hello &lt;b&gt;world");              // a whole document
 * Document doc2 = HtmlParser.parseDocument(bytes, "euc-kr", "http://host/a.html");  // bytes of a response, with encoding detection
 * DocumentFragment f = HtmlParser.parseFragment(contextElement, "&lt;li&gt;one&lt;li&gt;two"); // innerHTML
 * </pre>
 * For documents that load resources while they are parsed (scripts, <code>document.write</code>), create an instance, set a
 * {@link ParserListener} and call {@link #run()}; when the listener calls {@link ParserControl#pause()} (for example to wait for an
 * external script), {@link #run()} returns and {@link #resume()} continues. HTML parse errors never stop the parser; they are collected
 * in {@link #getErrors()}.
 * <p>
 * Threading: an instance is used by one thread at a time (the thread that owns the document).
 * <p>
 * HTML 파서: 인코딩 판정({@link EncodingSniffer}), 입력 전처리({@link InputPreprocessor}), {@link HtmlTokenizer}, {@link HtmlTreeBuilder} 를 이어
 * 마크업으로부터 <code>org.duckdns.hjow.swingwebpanel.dom</code> 의 DOM 트리를 만든다. HTML 표준의 파싱 장을 따라 처음부터 구현했으며 JRE 의 HTML/XML 파서는 쓰지 않는다.
 * <p>
 * 사용 예:
 * <pre>
 * Document doc = HtmlParser.parseDocument("&lt;p&gt;Hello &lt;b&gt;world");              // 문서 전체
 * Document doc2 = HtmlParser.parseDocument(bytes, "euc-kr", "http://host/a.html");  // 응답 바이트(인코딩 판정 포함)
 * DocumentFragment f = HtmlParser.parseFragment(contextElement, "&lt;li&gt;one&lt;li&gt;two"); // innerHTML
 * </pre>
 * 파싱 중 자원을 불러오는 문서(스크립트, <code>document.write</code>)는 인스턴스를 만들어 {@link ParserListener} 를 지정하고 {@link #run()} 을 호출한다.
 * 리스너가 (예: 외부 스크립트를 기다리려고) {@link ParserControl#pause()} 를 호출하면 {@link #run()} 이 반환되고 {@link #resume()} 으로 계속한다.
 * HTML 파싱 오류는 파서를 멈추지 않으며 {@link #getErrors()} 에 모인다.
 * <p>
 * 스레드: 인스턴스는 한 번에 한 스레드(문서를 소유한 스레드)가 쓴다.
 */
public final class HtmlParser implements ParserControl, ParserHost {

    /**
     * The document that receives the nodes (a fragment parse creates nodes in the document of the context element).
     * <p>
     * 노드를 받는 문서(조각 파싱은 컨텍스트 요소의 문서에서 노드를 만든다).
     */
    private final Document doc;

    /**
     * The decoded input (also kept for a restart with another encoding), or null when the parser got a string.
     * <p>
     * 디코딩된 입력(다른 인코딩으로 다시 시작할 때를 위해서도 보관). 문자열을 받았으면 null.
     */
    private DecodedText decoded;

    /**
     * The text to parse, with LF line ends.
     * <p>
     * 파싱할 텍스트(줄 끝은 LF).
     */
    private String input;

    /**
     * The fragment context element, or null.
     * <p>
     * 조각 파싱의 컨텍스트 요소 또는 null.
     */
    private final Element context;

    /**
     * The listener, or null.
     * <p>
     * 리스너 또는 null.
     */
    private ParserListener listener;

    /**
     * The parser scripting flag.
     * <p>
     * parser scripting 플래그.
     */
    private boolean scripting = true;

    /**
     * True for an iframe srcdoc document.
     * <p>
     * iframe srcdoc 문서이면 true.
     */
    private boolean srcdoc;

    /**
     * Maximum number of stored parse errors.
     * <p>
     * 저장할 파싱 오류의 최대 수.
     */
    private int errorLimit = ParseErrorLog.DEFAULT_LIMIT;

    /**
     * True if input stream errors (control characters and so on) are scanned for.
     * <p>
     * 입력 스트림 오류(제어 문자 등)를 검사하면 true.
     */
    private boolean scanInputErrors = true;

    /**
     * The error collector of the current pass.
     * <p>
     * 현재 패스의 오류 수집기.
     */
    private ParseErrorLog log;

    /**
     * The tokenizer of the current pass.
     * <p>
     * 현재 패스의 토큰화기.
     */
    private HtmlTokenizer tokenizer;

    /**
     * The tree builder of the current pass.
     * <p>
     * 현재 패스의 트리 생성기.
     */
    private HtmlTreeBuilder builder;

    /**
     * The root element that receives the nodes of a fragment parse.
     * <p>
     * 조각 파싱에서 노드를 받는 루트 요소.
     */
    private Element fragmentRoot;

    /**
     * True when {@link #pause()} was called and not yet resumed.
     * <p>
     * {@link #pause()} 가 호출되고 아직 재개되지 않았으면 true.
     */
    private boolean paused;

    /**
     * True after the parse is complete.
     * <p>
     * 파싱이 끝난 뒤에는 true.
     */
    private boolean finished;

    /**
     * Encoding that a late <code>meta</code> element asked for, or null.
     * <p>
     * 늦게 나온 <code>meta</code> 요소가 요청한 인코딩. 없으면 null.
     */
    private String restartEncoding;

    /**
     * Base URI of the document before parsing (restored on a restart).
     * <p>
     * 파싱 전 문서의 기준 URI(다시 시작할 때 복원한다).
     */
    private final String initialBase;

    /**
     * Creates a parser for already decoded text.
     * <p>
     * 이미 디코딩된 텍스트를 위한 파서를 만든다.
     *
     * @param doc    document that receives the nodes (it should be empty) / 노드를 받는 문서(비어 있어야 한다)
     * @param source markup text (line ends may be CR LF, CR or LF) / 마크업 텍스트(줄 끝은 CR LF, CR, LF 모두 가능)
     */
    public HtmlParser(Document doc, String source) {
        this(doc, source, null, null);
    }

    /**
     * Creates a parser for bytes that have been decoded by {@link EncodingSniffer}; a <code>meta</code> element that contradicts a
     * tentative decision restarts the parse with the right encoding (as long as no script has been run).
     * <p>
     * {@link EncodingSniffer} 로 디코딩한 바이트를 위한 파서를 만든다. 잠정적인 판정과 모순되는 <code>meta</code> 요소가 나오면(스크립트가 아직 실행되지
     * 않았다면) 올바른 인코딩으로 파싱을 다시 시작한다.
     *
     * @param doc  document that receives the nodes (it should be empty) / 노드를 받는 문서(비어 있어야 한다)
     * @param text decoded text / 디코딩된 텍스트
     */
    public HtmlParser(Document doc, DecodedText text) {
        this(doc, text.getText(), text, null);
    }

    /**
     * Creates a parser (the common constructor).
     * <p>
     * 파서를 만든다(공통 생성자).
     *
     * @param doc     document / 문서
     * @param source  text / 텍스트
     * @param decoded decoded text or null / 디코딩 결과 또는 null
     * @param context fragment context or null / 조각 컨텍스트 또는 null
     */
    private HtmlParser(Document doc, String source, DecodedText decoded, Element context) {
        this.doc = doc;
        this.decoded = decoded;
        this.input = InputPreprocessor.normalize(source);
        this.context = context;
        this.initialBase = context == null ? doc.getBaseUri() : null;
    }

    /**
     * Sets the listener that is told about scripts, style sheets, images and so on (call before {@link #run()}).
     * <p>
     * 스크립트, 스타일시트, 이미지 등을 통지받는 리스너를 지정한다({@link #run()} 전에 호출).
     *
     * @param l listener or null / 리스너 또는 null
     */
    public void setListener(ParserListener l) {
        this.listener = l;
    }

    /**
     * Sets the parser scripting flag: if true (the default) the content of <code>noscript</code> is raw text, as when JavaScript is
     * enabled.
     * <p>
     * parser scripting 플래그를 지정한다. true(기본값)이면 <code>noscript</code> 내용이 원문 텍스트로 처리된다. 자바스크립트가 켜진 때와 같다.
     *
     * @param enabled true if scripting is enabled / 스크립트가 켜져 있으면 true
     */
    public void setScripting(boolean enabled) {
        this.scripting = enabled;
    }

    /**
     * Marks the document as an iframe srcdoc document: no quirks mode detection.
     * <p>
     * 문서를 iframe srcdoc 문서로 표시한다: quirks 모드를 판정하지 않는다.
     *
     * @param value true for srcdoc / srcdoc 이면 true
     */
    public void setSrcdoc(boolean value) {
        this.srcdoc = value;
    }

    /**
     * Sets how many parse errors are stored (the counter keeps counting).
     * <p>
     * 저장할 파싱 오류의 수를 지정한다(개수는 계속 센다).
     *
     * @param limit maximum number / 최대 수
     */
    public void setErrorLimit(int limit) {
        this.errorLimit = limit;
    }

    /**
     * Switches the scan for input stream errors (control characters, noncharacters) on or off; it is on by default.
     * <p>
     * 입력 스트림 오류(제어 문자, 비문자) 검사를 켜거나 끈다. 기본값은 켜짐이다.
     *
     * @param enabled true to scan / 검사하려면 true
     */
    public void setScanInputErrors(boolean enabled) {
        this.scanInputErrors = enabled;
    }

    /**
     * Creates the tokenizer and the tree builder for one pass over the input.
     * <p>
     * 입력을 한 번 훑는 데 쓸 토큰화기와 트리 생성기를 만든다.
     */
    private void startPass() {
        log = new ParseErrorLog(input, errorLimit, listener);
        if (scanInputErrors) {
            InputPreprocessor.scanErrors(input, log);
        }
        tokenizer = new HtmlTokenizer(input, log);
        builder = new HtmlTreeBuilder(doc, tokenizer, log, listener, this, this, scripting);
        builder.srcdoc = srcdoc;
        if (context != null) {
            if (Namespaces.HTML.equals(context.getNamespaceURI())) {
                String n = context.getLocalName();
                // 컨텍스트 요소에 따라 토큰화기의 시작 상태를 정한다.
                if ("title".equals(n) || "textarea".equals(n)) {
                    tokenizer.setState(HtmlTokenizer.RCDATA);
                } else if ("style".equals(n) || "xmp".equals(n) || "iframe".equals(n) || "noembed".equals(n) || "noframes".equals(n)
                        || ("noscript".equals(n) && scripting)) {
                    tokenizer.setState(HtmlTokenizer.RAWTEXT);
                } else if ("script".equals(n)) {
                    tokenizer.setState(HtmlTokenizer.SCRIPT_DATA);
                } else if ("plaintext".equals(n)) {
                    tokenizer.setState(HtmlTokenizer.PLAINTEXT);
                }
                tokenizer.setLastStartTag(n);
            }
            fragmentRoot = builder.startFragment(context);
        } else if (decoded != null) {
            doc.setCharacterSet(decoded.getEncoding());
        }
    }

    /**
     * Parses until the end of the input or until a listener pauses the parser.
     * <p>
     * 입력 끝까지, 또는 리스너가 파서를 멈출 때까지 파싱한다.
     */
    public void run() {
        if (finished) {
            return;
        }
        if (builder == null) {
            startPass();
        }
        paused = false;
        // 늦은 meta 로 인코딩이 바뀌면 처음부터 다시 파싱하므로 패스를 반복한다.
        while (true) {
            runPass();
            if (restartEncoding == null) {
                break;
            }
            restart();
        }
        if (!paused) {
            finished = true;
        }
    }

    /**
     * Continues after {@link ParserControl#pause()}.
     * <p>
     * {@link ParserControl#pause()} 뒤에 계속한다.
     */
    public void resume() {
        run();
    }

    /**
     * Runs the tokenizer and the tree builder until the input ends, the parser stops or the parser is paused.
     * <p>
     * 입력이 끝나거나 파서가 멈추거나 일시 정지될 때까지 토큰화기와 트리 생성기를 돌린다.
     */
    private void runPass() {
        // 토큰을 하나씩 받아 트리 생성기에 넘긴다. 일시 정지, 정지, 재시작 요청이면 멈춘다.
        while (!paused && !builder.stopped) {
            tokenizer.setAllowCdata(builder.adjustedNs() != TreeBuilderBase.NS_HTML);
            HtmlToken t = tokenizer.nextToken();
            builder.process(t);
            if (t.getType() == HtmlToken.EOF) {
                if (!builder.stopped) {
                    builder.stopParsing();
                }
                break;
            }
        }
        builder.flushText();
    }

    /**
     * Throws away the tree built so far and prepares a new pass over the same bytes decoded with the requested encoding.
     * <p>
     * 지금까지 만든 트리를 버리고 요청된 인코딩으로 다시 디코딩한 같은 바이트로 새 패스를 준비한다.
     */
    private void restart() {
        decoded = decoded.redecode(restartEncoding);
        restartEncoding = null;
        input = InputPreprocessor.normalize(decoded.getText());
        // 문서의 자식을 모두 지우고 문서 모드와 기준 URI 를 처음 상태로 되돌린다.
        while (doc.getFirstChild() != null) {
            doc.removeChild(doc.getFirstChild());
        }
        doc.setCompatibilityMode(Document.NO_QUIRKS_MODE);
        doc.setBaseUri(initialBase);
        startPass();
    }

    /**
     * Called by the tree builder when a <code>meta</code> element declares an encoding.
     * <p>
     * <code>meta</code> 요소가 인코딩을 선언했을 때 트리 생성기가 호출한다.
     *
     * @param canonicalName canonical encoding name / 표준 인코딩 이름
     * @return true if the parse must restart / 파싱을 다시 시작해야 하면 true
     */
    public boolean metaCharset(String canonicalName) {
        if (decoded == null || decoded.getRawBytes() == null || decoded.isCertain() || context != null) {
            return false;
        }
        String enc = canonicalName;
        if (EncodingLabels.UTF_16BE.equals(enc) || EncodingLabels.UTF_16LE.equals(enc)) {
            enc = EncodingLabels.UTF_8;
        } else if (EncodingLabels.X_USER_DEFINED.equals(enc)) {
            enc = EncodingLabels.WINDOWS_1252;
        }
        if (!EncodingLabels.isDecodable(enc)) {
            return false;
        }
        if (enc.equals(decoded.getEncoding())) {
            decoded = decoded.withCertainty();
            return false;
        }
        if (builder != null && builder.scriptsDispatched) {
            return false;
        }
        restartEncoding = enc;
        return true;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // ParserControl
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Inserts markup text at the insertion point (see {@link ParserControl#insertAtInsertionPoint(String)}).
     * <p>
     * 삽입 지점에 마크업 텍스트를 삽입한다({@link ParserControl#insertAtInsertionPoint(String)} 참조).
     *
     * @param text markup text / 마크업 텍스트
     */
    public void insertAtInsertionPoint(String text) {
        if (context == null && tokenizer != null && !finished) {
            tokenizer.insertText(InputPreprocessor.normalize(text));
        }
    }

    /**
     * Asks {@link #run()} to return after the current token.
     * <p>
     * 현재 토큰 뒤에 {@link #run()} 이 반환되도록 요청한다.
     */
    public void pause() {
        paused = true;
    }

    /**
     * Tells whether the parser is paused.
     * <p>
     * 파서가 일시 정지 상태인지 알려 준다.
     *
     * @return true if paused / 일시 정지이면 true
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Tells whether the parse is complete.
     * <p>
     * 파싱이 끝났는지 알려 준다.
     *
     * @return true if complete / 끝났으면 true
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Tells whether a fragment is being parsed.
     * <p>
     * 조각을 파싱 중인지 알려 준다.
     *
     * @return true for a fragment parse / 조각 파싱이면 true
     */
    public boolean isFragment() {
        return context != null;
    }

    /**
     * Returns the document that nodes are created in.
     * <p>
     * 노드를 만드는 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Returns the stored parse errors of the last pass.
     * <p>
     * 마지막 패스에서 저장된 파싱 오류를 반환한다.
     *
     * @return unmodifiable list / 수정할 수 없는 목록
     */
    public List<ParseError> getErrors() {
        return log == null ? java.util.Collections.<ParseError>emptyList() : log.getErrors();
    }

    /**
     * Returns the number of parse errors of the last pass, including those that were not stored.
     * <p>
     * 마지막 패스의 파싱 오류 수를 저장하지 않은 것까지 포함해 반환한다.
     *
     * @return count / 개수
     */
    public int getErrorCount() {
        return log == null ? 0 : log.getTotal();
    }

    /**
     * Returns the error codes of the last pass in order.
     * <p>
     * 마지막 패스의 오류 코드를 순서대로 반환한다.
     *
     * @return codes / 코드
     */
    public List<String> getErrorCodes() {
        return log == null ? java.util.Collections.<String>emptyList() : log.getCodes();
    }

    /**
     * Returns the decoded input of a byte parse (with the final encoding decision), or null for a string parse.
     * <p>
     * 바이트 파싱의 디코딩된 입력(최종 인코딩 판정 포함)을 반환한다. 문자열 파싱이면 null.
     *
     * @return the decoded text or null / 디코딩 결과 또는 null
     */
    public DecodedText getDecodedText() {
        return decoded;
    }

    /**
     * Returns the nodes of a finished fragment parse as a document fragment (the nodes are moved out of the internal root element).
     * <p>
     * 끝난 조각 파싱의 노드를 문서 조각으로 반환한다(노드는 내부 루트 요소에서 옮겨진다).
     *
     * @return the fragment, or null if this is not a fragment parse / 조각. 조각 파싱이 아니면 null
     */
    public DocumentFragment takeFragment() {
        if (fragmentRoot == null) {
            return null;
        }
        DocumentFragment f = doc.createDocumentFragment();
        // 루트 요소의 자식을 차례로 조각으로 옮긴다.
        while (fragmentRoot.getFirstChild() != null) {
            f.appendChild(fragmentRoot.getFirstChild());
        }
        return f;
    }

    // ---------------------------------------------------------------------------------------------------------------------------
    // 편의 메서드
    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Parses markup text into a new HTML document (scripting enabled, no listener).
     * <p>
     * 마크업 텍스트를 새 HTML 문서로 파싱한다(스크립트 활성, 리스너 없음).
     *
     * @param source markup text / 마크업 텍스트
     * @return the document / 문서
     */
    public static Document parseDocument(String source) {
        return parseDocument(source, null, null, true);
    }

    /**
     * Parses markup text into a new HTML document.
     * <p>
     * 마크업 텍스트를 새 HTML 문서로 파싱한다.
     *
     * @param source    markup text / 마크업 텍스트
     * @param url       document URL (used to resolve <code>base</code>), or null / 문서 URL(<code>base</code> 해석에 쓴다). 없으면 null
     * @param listener  listener or null / 리스너 또는 null
     * @param scripting parser scripting flag / parser scripting 플래그
     * @return the document / 문서
     */
    public static Document parseDocument(String source, String url, ParserListener listener, boolean scripting) {
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        if (url != null) {
            doc.setUrl(url);
        }
        HtmlParser p = new HtmlParser(doc, source);
        p.setListener(listener);
        p.setScripting(scripting);
        p.run();
        return doc;
    }

    /**
     * Decodes the bytes of a response with {@link EncodingSniffer} and parses them into a new HTML document.
     * <p>
     * 응답 바이트를 {@link EncodingSniffer} 로 디코딩해 새 HTML 문서로 파싱한다.
     *
     * @param bytes       response body / 응답 본문
     * @param httpCharset charset parameter of the Content-Type header, or null / Content-Type 헤더의 charset 매개변수 또는 null
     * @param url         document URL or null / 문서 URL 또는 null
     * @return the document; <code>getCharacterSet()</code> tells the encoding that was used / 문서. <code>getCharacterSet()</code> 이 사용한 인코딩을 알려 준다
     */
    public static Document parseDocument(byte[] bytes, String httpCharset, String url) {
        Document doc = Document.createHtmlDocument();
        doc.setMarkupSupport(new HtmlMarkupSupport());
        if (url != null) {
            doc.setUrl(url);
        }
        HtmlParser p = new HtmlParser(doc, EncodingSniffer.decode(bytes, httpCharset));
        p.run();
        return doc;
    }

    /**
     * Parses markup in the context of an element ("HTML fragment parsing algorithm", the algorithm behind <code>innerHTML</code>).
     * The nodes are created in the owner document of the context element and returned in a new document fragment; scripts in the
     * markup are marked "already started" so that they never run.
     * <p>
     * 요소를 컨텍스트로 마크업을 파싱한다("HTML fragment parsing algorithm", <code>innerHTML</code> 의 알고리즘). 노드는 컨텍스트 요소의 소유
     * 문서에서 만들어 새 문서 조각에 담아 반환한다. 마크업 안의 스크립트는 "already started" 로 표시되어 절대 실행되지 않는다.
     *
     * @param context the context element (for <code>innerHTML</code> the element itself) / 컨텍스트 요소(<code>innerHTML</code> 이면 요소 자신)
     * @param markup  markup text / 마크업 텍스트
     * @return the fragment / 조각
     */
    public static DocumentFragment parseFragment(Element context, String markup) {
        return parseFragment(context, markup, true);
    }

    /**
     * Parses markup in the context of an element.
     * <p>
     * 요소를 컨텍스트로 마크업을 파싱한다.
     *
     * @param context   the context element / 컨텍스트 요소
     * @param markup    markup text / 마크업 텍스트
     * @param scripting parser scripting flag (decides how <code>noscript</code> is parsed) / parser scripting 플래그(<code>noscript</code> 를 파싱하는 방식을 정한다)
     * @return the fragment / 조각
     */
    public static DocumentFragment parseFragment(Element context, String markup, boolean scripting) {
        Document doc = context.getOwnerDocument();
        HtmlParser p = new HtmlParser(doc, markup, null, context);
        p.setScripting(scripting);
        p.setScanInputErrors(false);
        p.run();
        return p.takeFragment();
    }

    /**
     * Returns the context node that {@link Node} based callers (such as <code>insertAdjacentHTML</code>) may need: the element itself.
     * <p>
     * <code>insertAdjacentHTML</code> 같은 {@link Node} 기반 호출자가 필요로 할 수 있는 컨텍스트 노드를 반환한다: 요소 자신.
     *
     * @param n a node / 노드
     * @return the node as an element, or null if it is not one / 요소이면 그 요소, 아니면 null
     */
    static Element asElement(Node n) {
        return n instanceof Element ? (Element) n : null;
    }
}
