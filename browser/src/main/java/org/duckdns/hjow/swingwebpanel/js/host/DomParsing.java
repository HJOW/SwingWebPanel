package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlResult;
import org.duckdns.hjow.swingwebpanel.css.selector.CssSelectorEngine;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.dom.XmlSerializer;
import org.duckdns.hjow.swingwebpanel.html.HtmlMarkupSupport;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * <code>DOMParser</code> and <code>XMLSerializer</code> of a page (plan item P20-04; HTML Standard "DOMParser" and DOM Parsing and
 * Serialization "XML Serialization"). They are tools for scripts: they make new documents and markup text, and they never change the
 * document of the page and never touch the source text that <code>getContent()</code> returns.
 * <ul>
 * <li><code>new DOMParser().parseFromString(text, type)</code>: <code>text/html</code> parses with the HTML parser (scripting disabled, so
 * scripts do not run and <code>noscript</code> content is parsed as markup); <code>text/xml</code>, <code>application/xml</code>,
 * <code>application/xhtml+xml</code> and <code>image/svg+xml</code> parse with the XML parser of the component (external entities and
 * external DTDs are never fetched); if the text is not well-formed XML the result is a document whose only child is a <code>parsererror</code>
 * element in the namespace <code>http://www.mozilla.org/newlayout/xml/parsererror.xml</code> that holds the position and message of the
 * error. Any other <code>type</code>, and a missing argument, is a <code>TypeError</code>. The URL of the new document is the URL of the page.</li>
 * <li><code>new XMLSerializer().serializeToString(node)</code>: the XML serialization of {@link XmlSerializer}.</li>
 * </ul>
 * <p>
 * 페이지의 <code>DOMParser</code> 와 <code>XMLSerializer</code>(계획 P20-04, HTML 표준 "DOMParser", DOM Parsing and Serialization "XML Serialization").
 * 스크립트가 쓰는 도구이며 새 문서와 마크업 텍스트를 만들 뿐 페이지의 문서를 바꾸거나 <code>getContent()</code> 가 반환하는 원문을 건드리지 않는다.
 * <ul>
 * <li><code>new DOMParser().parseFromString(text, type)</code>: <code>text/html</code> 은 HTML 파서로 해석한다(스크립트 비활성이므로 스크립트는
 * 실행되지 않고 <code>noscript</code> 내용도 마크업으로 해석한다). <code>text/xml</code>, <code>application/xml</code>,
 * <code>application/xhtml+xml</code>, <code>image/svg+xml</code> 은 컴포넌트의 XML 파서로 해석한다(외부 엔티티와 외부 DTD 는 가져오지 않는다). 텍스트가
 * well-formed XML 이 아니면 결과는 오류의 위치와 메시지를 담은 <code>http://www.mozilla.org/newlayout/xml/parsererror.xml</code> 네임스페이스의
 * <code>parsererror</code> 요소 하나만 자식으로 가진 문서이다. 그 밖의 <code>type</code> 과 인수 누락은 <code>TypeError</code>. 새 문서의 URL은
 * 페이지의 URL이다.</li>
 * <li><code>new XMLSerializer().serializeToString(node)</code>: {@link XmlSerializer} 의 XML 직렬화.</li>
 * </ul>
 */
public final class DomParsing {

    /**
     * The namespace of the <code>parsererror</code> element that a failed XML parse returns.
     * <p>
     * XML 해석이 실패했을 때 반환하는 <code>parsererror</code> 요소의 네임스페이스.
     */
    public static final String PARSER_ERROR_NAMESPACE = "http://www.mozilla.org/newlayout/xml/parsererror.xml";

    /**
     * The types that <code>parseFromString</code> accepts.
     * <p>
     * <code>parseFromString</code> 이 받는 type.
     */
    private static final String[] TYPES = { "text/html", "text/xml", "application/xml", "application/xhtml+xml", "image/svg+xml" };

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page the page / 페이지
     */
    DomParsing(PageScripts page) {
        this.page = page;
    }

    /**
     * The <code>DOMParser</code> object (script visible members: <code>parseFromString</code>).
     * <p>
     * <code>DOMParser</code> 객체(스크립트에 보이는 멤버: <code>parseFromString</code>).
     */
    public static final class Parser {
        /**
         * The page that made the parser.
         * <p>
         * 파서를 만든 페이지.
         */
        private final PageScripts page;

        /**
         * Creates the parser.
         * <p>
         * 파서를 만든다.
         *
         * @param page the page / 페이지
         */
        Parser(PageScripts page) {
            this.page = page;
        }

        /**
         * Parses markup into a new document of the given type.
         * <p>
         * 마크업을 주어진 type 의 새 문서로 해석한다.
         *
         * @param markup the markup / 마크업
         * @param type   one of the five supported MIME types / 지원하는 다섯 MIME 타입 중 하나
         * @return the new document / 새 문서
         * @throws JsException a TypeError for another type / 다른 type 이면 TypeError
         */
        public Document parseFromString(String markup, String type) {
            boolean known = false;
            // 지원하는 타입 목록에 있는지 본다(대소문자까지 정확히 같아야 한다).
            for (int i = 0; i < TYPES.length; i++) {
                if (TYPES[i].equals(type)) {
                    known = true;
                }
            }
            if (!known) {
                throw JsException.typeError("Failed to execute 'parseFromString' on 'DOMParser': The provided value '" + type
                        + "' is not a valid enum value of type SupportedType.");
            }
            String url = page.getDocument().getUrl();
            if ("text/html".equals(type)) {
                Document doc = Document.createHtmlDocument();
                doc.setMarkupSupport(new HtmlMarkupSupport());
                CssSelectorEngine.install(doc);
                doc.setUrl(url);
                HtmlParser parser = new HtmlParser(doc, markup);
                parser.setScripting(false);
                parser.run();
                return doc;
            }
            Document doc = Document.createXmlDocument(type);
            doc.setMarkupSupport(new HtmlMarkupSupport());
            CssSelectorEngine.install(doc);
            XhtmlResult result = new XhtmlParser(doc).parse(markup, url);
            if (result.isWellFormed()) {
                doc.setUrl(url);
                return doc;
            }
            return errorDocument(type, url, result);
        }

        /**
         * Builds the document that a failed XML parse returns: a <code>parsererror</code> element with the position and the message of the error.
         * <p>
         * 실패한 XML 해석이 반환하는 문서를 만든다: 오류의 위치와 메시지를 담은 <code>parsererror</code> 요소.
         *
         * @param type   the MIME type / MIME 타입
         * @param url    the URL of the page / 페이지의 URL
         * @param result the failed parse / 실패한 해석
         * @return the document / 문서
         */
        private static Document errorDocument(String type, String url, XhtmlResult result) {
            Document doc = Document.createXmlDocument(type);
            doc.setMarkupSupport(new HtmlMarkupSupport());
            CssSelectorEngine.install(doc);
            doc.setUrl(url);
            Element root = doc.createElementNS(PARSER_ERROR_NAMESPACE, "parsererror");
            String text = "XML parsing error";
            if (result.getError() != null) {
                text = "error on line " + result.getError().getLine() + " at column " + result.getError().getColumn() + ": "
                        + result.getError().getMessage();
            }
            root.appendChild(doc.createTextNode(text));
            doc.appendChild(root);
            return doc;
        }
    }

    /**
     * The <code>XMLSerializer</code> object (script visible members: <code>serializeToString</code>).
     * <p>
     * <code>XMLSerializer</code> 객체(스크립트에 보이는 멤버: <code>serializeToString</code>).
     */
    public static final class Serializer {
        /**
         * Creates the serializer.
         * <p>
         * 직렬화기를 만든다.
         */
        Serializer() {
        }

        /**
         * Returns the XML serialization of a node.
         * <p>
         * 노드의 XML 직렬화를 반환한다.
         *
         * @param root the node / 노드
         * @return the markup / 마크업
         */
        public String serializeToString(Node root) {
            return XmlSerializer.serialize(root);
        }
    }

    /**
     * Installs <code>DOMParser</code> and <code>XMLSerializer</code> (interfaces and constructors).
     * <p>
     * <code>DOMParser</code> 와 <code>XMLSerializer</code> 를 설치한다(인터페이스와 생성자).
     */
    void install() {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interface of a parser or serializer.
             * <p>
             * 파서와 직렬화기의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof Parser) {
                    return "DOMParser";
                } else if (o instanceof Serializer) {
                    return "XMLSerializer";
                }
                return null;
            }
        });
        final DomBridge.InterfaceInfo parser = bridge.defineInterface("DOMParser", null, Parser.class, new String[] { "m:parseFromString(String,String)" });
        bridge.setConstructor("DOMParser", new NativeFunction(realm, realm.getFunctionPrototype(), "DOMParser", 0, refuse("DOMParser"),
                new NativeFunction.Construct() {
            /**
             * Creates a parser.
             * <p>
             * 파서를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the parser wrapper / 파서 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(new Parser(page));
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, parser.getPrototype()));
                return w;
            }
        }));
        final DomBridge.InterfaceInfo serializer = bridge.defineInterface("XMLSerializer", null, Serializer.class,
                new String[] { "m:serializeToString(Node)" });
        bridge.setConstructor("XMLSerializer", new NativeFunction(realm, realm.getFunctionPrototype(), "XMLSerializer", 0, refuse("XMLSerializer"),
                new NativeFunction.Construct() {
            /**
             * Creates a serializer.
             * <p>
             * 직렬화기를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the serializer wrapper / 직렬화기 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(new Serializer());
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, serializer.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Creates the behaviour of an interface object that is called without <code>new</code>: it throws a TypeError.
     * <p>
     * <code>new</code> 없이 호출한 인터페이스 객체의 동작을 만든다: TypeError 를 던진다.
     *
     * @param name the interface name / 인터페이스 이름
     * @return the behaviour / 동작
     */
    private static NativeFunction.Call refuse(final String name) {
        return new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator.");
            }
        };
    }
}
