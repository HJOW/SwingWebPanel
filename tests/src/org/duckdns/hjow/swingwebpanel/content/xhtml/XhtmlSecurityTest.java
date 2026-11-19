package org.duckdns.hjow.swingwebpanel.content.xhtml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Namespaces;
import org.duckdns.hjow.swingwebpanel.testing.Check;
import org.duckdns.hjow.swingwebpanel.testing.TestHttpServer;

/**
 * Security and robustness tests of {@link XhtmlParser} (plan item P17-05): a document can never make the parser read anything outside
 * its own text (external DTD, external entities, parameter entities, XInclude, <code>file:</code> and HTTP references), and attacks on
 * the parser (entity bombs, deep nesting, huge documents, slow parses) end with a bounded structured error. Whether a request reaches
 * the network is decided by a counter in a local HTTP server, whether a local file is read by a marker in a temporary file.
 * <p>
 * {@link XhtmlParser} 의 보안과 견고성 시험(계획 P17-05): 문서는 파서가 자기 텍스트 밖의 어떤 것(외부 DTD, 외부 엔티티, 매개변수 엔티티, XInclude,
 * <code>file:</code> 과 HTTP 참조)도 읽게 만들 수 없고, 파서에 대한 공격(엔티티 폭탄, 깊은 중첩, 거대한 문서, 느린 해석)은 한정된 구조화 오류로 끝난다.
 * 요청이 네트워크에 닿았는지는 로컬 HTTP 서버의 카운터로, 로컬 파일을 읽었는지는 임시 파일의 표식으로 판정한다.
 */
public class XhtmlSecurityTest {

    /**
     * The XHTML namespace.
     * <p>
     * XHTML 네임스페이스.
     */
    private static final String NS = Namespaces.HTML;

    /**
     * The public identifier of XHTML 1.0 Strict.
     * <p>
     * XHTML 1.0 Strict 의 공개 식별자.
     */
    private static final String STRICT_PUBLIC = "-//W3C//DTD XHTML 1.0 Strict//EN";

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public XhtmlSecurityTest() {
    }

    /**
     * Starts a local server that answers everything with a marker text; every request is counted.
     * <p>
     * 모든 요청에 표식 텍스트를 답하는 로컬 서버를 시작한다. 요청은 모두 센다.
     *
     * @return the started server / 시작한 서버
     * @throws IOException if the server cannot start / 서버를 시작할 수 없으면
     */
    private static TestHttpServer server() throws IOException {
        TestHttpServer s = new TestHttpServer();
        s.routeFixed("/evil.dtd", TestHttpServer.Response.ok("application/xml-dtd", "<!ENTITY x \"LOADED_FROM_NETWORK\">"));
        s.routeFixed("/ent.txt", TestHttpServer.Response.ok("text/plain", "LOADED_FROM_NETWORK"));
        s.routeFixed("/p.dtd", TestHttpServer.Response.ok("application/xml-dtd", "<!ENTITY % q \"LOADED_FROM_NETWORK\">"));
        s.routeFixed("/inc.xml", TestHttpServer.Response.ok("application/xml", "<included>LOADED_FROM_NETWORK</included>"));
        s.start();
        return s;
    }

    /**
     * Checks that the server was never asked and that the marker is nowhere in the document.
     * <p>
     * 서버가 한 번도 요청받지 않았고 문서 어디에도 표식이 없는지 확인한다.
     *
     * @param s    the server / 서버
     * @param r    the result / 결과
     * @param what what was tried / 시도한 것
     */
    private static void untouched(TestHttpServer s, XhtmlResult r, String what) {
        Check.equal(0, s.totalRequests(), what + ": the server received no request");
        Check.isFalse(XhtmlTreeDump.dump(r.getDocument()).indexOf("LOADED_FROM") >= 0, what + ": nothing was loaded into the document");
    }

    /**
     * An external DTD is never fetched: its entities stay undeclared (an error), a document that does not use them parses, and the
     * DOCTYPE node keeps the identifiers.
     * <p>
     * 외부 DTD 는 절대 가져오지 않는다: 그 엔티티는 선언되지 않은 채로 남고(오류), 쓰지 않는 문서는 해석되며, DOCTYPE 노드는 식별자를 유지한다.
     *
     * @throws Exception if the server fails / 서버가 실패하면
     */
    public void testExternalDtdIsNotLoaded() throws Exception {
        TestHttpServer s = server();
        try {
            String dtd = s.url("/evil.dtd");
            XhtmlResult r = XhtmlParser.parseDocument("<!DOCTYPE r SYSTEM \"" + dtd + "\"><r>&x;</r>", null);
            Check.notNull(r.getError(), "the entity of the external DTD is undeclared");
            untouched(s, r, "external DTD with an entity reference");
            XhtmlResult ok = XhtmlParser.parseDocument("<!DOCTYPE r PUBLIC \"-//EVIL//DTD//EN\" \"" + dtd + "\"><r>plain</r>", null);
            Check.isNull(ok.getError(), "a document that does not need the DTD parses: " + ok.getError());
            untouched(s, ok, "external DTD without a reference");
            Check.equal(dtd, ok.getDocument().getDoctype().getSystemId(), "the DOCTYPE node keeps the system identifier");
            Check.equal("-//EVIL//DTD//EN", ok.getDocument().getDoctype().getPublicId(), "and the public identifier");
            // 서브셋이 있는 DOCTYPE 에서도 외부 식별자는 읽지 않고 내부 선언은 그대로 쓴다.
            XhtmlResult both = XhtmlParser.parseDocument("<!DOCTYPE r SYSTEM \"" + dtd + "\" [<!ENTITY y \"inner\">]><r>&y;</r>", null);
            Check.isNull(both.getError(), "internal subset next to an external identifier: " + both.getError());
            Check.equal("inner", both.getDocument().getDocumentElement().getTextContent(), "internal entity works");
            untouched(s, both, "external identifier with an internal subset");
        } finally {
            s.stop();
        }
    }

    /**
     * External general and parameter entities and a remote DTD fragment via a parameter entity (blind XXE) are never fetched.
     * <p>
     * 외부 일반 엔티티와 외부 매개변수 엔티티, 매개변수 엔티티로 읽는 원격 DTD 조각(blind XXE)은 절대 가져오지 않는다.
     *
     * @throws Exception if the server fails / 서버가 실패하면
     */
    public void testExternalEntitiesAreNotLoaded() throws Exception {
        TestHttpServer s = server();
        try {
            XhtmlResult general = XhtmlParser.parseDocument("<!DOCTYPE r [<!ENTITY x SYSTEM \"" + s.url("/ent.txt") + "\">]><r>a&x;b</r>", null);
            Check.notNull(general.getError(), "a reference to an external entity is an error, not an empty string");
            untouched(s, general, "external general entity");
            XhtmlResult param = XhtmlParser.parseDocument("<!DOCTYPE r [<!ENTITY % p SYSTEM \"" + s.url("/p.dtd") + "\"> %p;]><r>x</r>", null);
            untouched(s, param, "external parameter entity");
            XhtmlResult blind = XhtmlParser.parseDocument("<!DOCTYPE r [<!ENTITY % remote SYSTEM \"" + s.url("/p.dtd") + "\">"
                    + "<!ENTITY % wrap \"<!ENTITY send SYSTEM '" + s.url("/ent.txt") + "?%q;'>\"> %remote; %wrap; ]><r>&send;</r>", null);
            untouched(s, blind, "blind XXE through a parameter entity");
            Check.notNull(blind.getError(), "the document cannot work without the remote declarations");
        } finally {
            s.stop();
        }
    }

    /**
     * XInclude is off: <code>xi:include</code> is an ordinary element and its target is not requested.
     * <p>
     * XInclude 는 꺼져 있다: <code>xi:include</code> 는 일반 요소이며 대상을 요청하지 않는다.
     *
     * @throws Exception if the server fails / 서버가 실패하면
     */
    public void testXIncludeIsOff() throws Exception {
        TestHttpServer s = server();
        try {
            XhtmlResult r = XhtmlParser.parseDocument("<root xmlns:xi=\"http://www.w3.org/2001/XInclude\"><xi:include href=\"" + s.url("/inc.xml")
                    + "\" parse=\"xml\"/><xi:include href=\"" + s.url("/ent.txt") + "\" parse=\"text\"/></root>", null);
            Check.isNull(r.getError(), "well-formed: " + r.getError());
            untouched(s, r, "XInclude");
            Element include = (Element) r.getDocument().getDocumentElement().getFirstChild();
            Check.equal("include", include.getLocalName(), "an ordinary element");
            Check.equal("http://www.w3.org/2001/XInclude", include.getNamespaceURI(), "in the XInclude namespace");
            Check.isNull(include.getFirstChild(), "without included content");
        } finally {
            s.stop();
        }
    }

    /**
     * A <code>file:</code> reference cannot read a local file, neither as an external entity nor as an external DTD.
     * <p>
     * <code>file:</code> 참조는 외부 엔티티로도 외부 DTD 로도 로컬 파일을 읽을 수 없다.
     *
     * @throws Exception if the temporary file cannot be written / 임시 파일을 쓸 수 없으면
     */
    public void testLocalFilesAreNotRead() throws Exception {
        File secret = File.createTempFile("swp-xhtml-secret", ".txt");
        File dtd = File.createTempFile("swp-xhtml-dtd", ".dtd");
        try {
            write(secret, "TOPSECRET_LOCAL_FILE");
            write(dtd, "<!ENTITY x \"LOADED_FROM_DTD_FILE\">");
            String secretUrl = secret.toURI().toString();
            XhtmlResult entity = XhtmlParser.parseDocument("<!DOCTYPE r [<!ENTITY x SYSTEM \"" + secretUrl + "\">]><r>&x;</r>", null);
            Check.notNull(entity.getError(), "file entity is an error");
            Check.isFalse(XhtmlTreeDump.dump(entity.getDocument()).indexOf("TOPSECRET") >= 0, "the file content is not in the document");
            Check.isFalse(String.valueOf(entity.getError()).indexOf("TOPSECRET") >= 0, "nor in the error");
            XhtmlResult external = XhtmlParser.parseDocument("<!DOCTYPE r SYSTEM \"" + dtd.toURI().toString() + "\"><r>&x;</r>", null);
            Check.notNull(external.getError(), "entity of a local DTD file stays undeclared");
            Check.isFalse(XhtmlTreeDump.dump(external.getDocument()).indexOf("LOADED_FROM") >= 0, "the DTD file was not read");
            // 상대 경로나 URL 이 아닌 시스템 식별자도 마찬가지로 읽지 않는다.
            XhtmlResult relative = XhtmlParser.parseDocument("<!DOCTYPE r [<!ENTITY x SYSTEM \"" + secret.getName() + "\">]><r>&x;</r>", null);
            Check.notNull(relative.getError(), "relative system identifier is an error");
        } finally {
            secret.delete();
            dtd.delete();
        }
    }

    /**
     * Writes a text to a file.
     * <p>
     * 파일에 텍스트를 쓴다.
     *
     * @param f    the file / 파일
     * @param text the text / 텍스트
     * @throws IOException if writing fails / 쓰기에 실패하면
     */
    private static void write(File f, String text) throws IOException {
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /**
     * Whatever the features of the parser say, the entity resolver of the builder refuses every request with an empty source and records it.
     * <p>
     * 파서의 기능 설정과 관계없이 빌더의 엔티티 해석기는 모든 요청을 빈 소스로 거부하고 기록한다.
     *
     * @throws Exception if reading the empty source fails / 빈 소스를 읽는 데 실패하면
     */
    public void testResolverRefusesEverything() throws Exception {
        XhtmlBuilder b = new XhtmlBuilder(Document.createXmlDocument("application/xhtml+xml"), null, XmlProlog.scan(""), new XhtmlLimits());
        org.xml.sax.InputSource in = b.resolveEntity("-//X//Y", "http://example.org/a.dtd");
        Check.equal(-1, in.getCharacterStream().read(), "empty source");
        b.resolveEntity(null, "file:///etc/passwd");
        b.resolveEntity("-//only public//", null);
        Check.equal("http://example.org/a.dtd", b.getRefused().get(0), "recorded system identifier");
        Check.equal("file:///etc/passwd", b.getRefused().get(1), "recorded file identifier");
        Check.equal("-//only public//", b.getRefused().get(2), "recorded public identifier when there is no system identifier");
    }

    /**
     * "Billion laughs": nested entities that would expand to gigabytes end with a limit error within seconds.
     * <p>
     * "Billion laughs": 수 기가바이트로 펼쳐질 중첩 엔티티는 몇 초 안에 상한 오류로 끝난다.
     */
    public void testBillionLaughs() {
        StringBuilder bomb = new StringBuilder("<?xml version=\"1.0\"?><!DOCTYPE lolz [<!ENTITY lol \"lol\">");
        // 앞 엔티티를 열 번씩 쓰는 엔티티를 아홉 단계 선언한다(10^9 번 펼쳐진다).
        for (int i = 1; i <= 9; i++) {
            bomb.append("<!ENTITY lol").append(i).append(" \"");
            // 앞 엔티티를 열 번 참조한다.
            for (int k = 0; k < 10; k++) {
                bomb.append("&lol").append(i == 1 ? "" : String.valueOf(i - 1)).append(';');
            }
            bomb.append("\">");
        }
        bomb.append("]><lolz>&lol9;</lolz>");
        long t = System.currentTimeMillis();
        XhtmlResult r = XhtmlParser.parseDocument(bomb.toString(), null);
        long ms = System.currentTimeMillis() - t;
        Check.notNull(r.getError(), "bomb is an error");
        Check.equal(XmlParseError.Kind.LIMIT_EXCEEDED, r.getError().getKind(), "limit error: " + r.getError());
        Check.isTrue(ms < 20000, "ended in " + ms + " ms");
    }

    /**
     * Entities that expand to nothing but to many references, and one large entity used very often, are bounded as well.
     * <p>
     * 아무것도 아닌 것으로 펼쳐지지만 참조가 많은 엔티티, 큰 엔티티 하나를 아주 자주 쓰는 문서도 한정된다.
     */
    public void testOtherEntityBombs() {
        StringBuilder empty = new StringBuilder("<!DOCTYPE r [<!ENTITY a0 \"\">");
        // 빈 엔티티를 둘씩 참조하는 단계를 서른 번 선언한다(2^30 번 펼쳐진다).
        for (int i = 1; i <= 30; i++) {
            empty.append("<!ENTITY a").append(i).append(" \"&a").append(i - 1).append(";&a").append(i - 1).append(";\">");
        }
        empty.append("]><r>&a30;</r>");
        XhtmlResult e = XhtmlParser.parseDocument(empty.toString(), null);
        Check.notNull(e.getError(), "expansion count bomb is an error");
        Check.equal(XmlParseError.Kind.LIMIT_EXCEEDED, e.getError().getKind(), "limit error: " + e.getError());
        StringBuilder big = new StringBuilder("<!DOCTYPE r [<!ENTITY a \"");
        // 5000 자짜리 엔티티를 2만 번 쓰면 1억 자가 된다.
        for (int i = 0; i < 5000; i++) {
            big.append('a');
        }
        big.append("\">]><r>");
        // 엔티티를 2만 번 참조한다.
        for (int i = 0; i < 20000; i++) {
            big.append("&a;");
        }
        big.append("</r>");
        long t = System.currentTimeMillis();
        XhtmlResult b = XhtmlParser.parseDocument(big.toString(), null);
        Check.notNull(b.getError(), "quadratic blowup is an error");
        Check.equal(XmlParseError.Kind.LIMIT_EXCEEDED, b.getError().getKind(), "limit error: " + b.getError());
        Check.isTrue(System.currentTimeMillis() - t < 20000, "bounded time");
    }

    /**
     * The limits can be changed and are enforced: depth, nodes, attributes, input size, text size, expansions and time.
     * <p>
     * 상한은 바꿀 수 있고 지켜진다: 깊이, 노드 수, 속성 수, 입력 크기, 텍스트 크기, 확장 수, 시간.
     */
    public void testLimits() {
        StringBuilder deep = new StringBuilder("<d>");
        // 600 단계의 중첩 요소(기본 상한 512 초과).
        for (int i = 0; i < 600; i++) {
            deep.append("<e>");
        }
        // 닫는 태그도 600 개 쓴다.
        for (int i = 0; i < 600; i++) {
            deep.append("</e>");
        }
        deep.append("</d>");
        limited(XhtmlParser.parseDocument(deep.toString(), null), "default depth limit");
        limited(parse("<a><b><c><d/></c></b></a>", new XhtmlLimits().setMaxDepth(3)), "custom depth limit");
        Check.isNull(parse("<a><b><c/></b></a>", new XhtmlLimits().setMaxDepth(3)).getError(), "depth equal to the limit is allowed");
        StringBuilder many = new StringBuilder("<r>");
        // 요소 1000 개.
        for (int i = 0; i < 1000; i++) {
            many.append("<i/>");
        }
        many.append("</r>");
        limited(parse(many.toString(), new XhtmlLimits().setMaxNodes(100)), "node limit");
        Check.isNull(parse(many.toString(), new XhtmlLimits().setMaxNodes(1001)).getError(), "nodes up to the limit are allowed");
        StringBuilder attrs = new StringBuilder("<r");
        // 속성 50 개.
        for (int i = 0; i < 50; i++) {
            attrs.append(" a").append(i).append("=\"v\"");
        }
        attrs.append("/>");
        limited(parse(attrs.toString(), new XhtmlLimits().setMaxAttributes(10)), "attribute limit");
        limited(parse("<r>0123456789</r>", new XhtmlLimits().setMaxChars(10)), "input size limit");
        limited(parse("<r>0123456789</r>", new XhtmlLimits().setMaxTextChars(5)), "text size limit");
        limited(parse("<r a=\"0123456789\"/>", new XhtmlLimits().setMaxTextChars(5)), "attribute text counts too");
        limited(parse("<!DOCTYPE html PUBLIC \"" + STRICT_PUBLIC + "\" \"x.dtd\"><r>&nbsp;&nbsp;&nbsp;</r>", new XhtmlLimits().setMaxEntityExpansions(2)), "expansion limit");
        StringBuilder slow = new StringBuilder("<r>");
        // 요소 20만 개: 1 밀리초 안에는 해석할 수 없다.
        for (int i = 0; i < 200000; i++) {
            slow.append("<i>x</i>");
        }
        slow.append("</r>");
        limited(parse(slow.toString(), new XhtmlLimits().setMaxMillis(1)), "time limit");
        Check.throwsException(IllegalArgumentException.class, new org.duckdns.hjow.swingwebpanel.testing.Block() {
            /**
             * Sets a limit of zero.
             * <p>
             * 0 인 상한을 지정한다.
             *
             * @throws Exception always the expected one / 기대하는 예외
             */
            public void run() throws Exception {
                new XhtmlLimits().setMaxDepth(0);
            }
        }, "limits must be positive");
        XhtmlLimits l = new XhtmlLimits().setMaxDepth(7);
        XhtmlParser p = new XhtmlParser();
        p.setLimits(l);
        l.setMaxDepth(1);
        Check.equal(7, p.getLimits().getMaxDepth(), "the parser keeps a copy");
    }

    /**
     * Parses with limits.
     * <p>
     * 상한을 주고 해석한다.
     *
     * @param xml    the text / 텍스트
     * @param limits the limits / 상한
     * @return the result / 결과
     */
    private static XhtmlResult parse(String xml, XhtmlLimits limits) {
        XhtmlParser p = new XhtmlParser();
        p.setLimits(limits);
        return p.parse(xml, null);
    }

    /**
     * Checks that a result is a limit error.
     * <p>
     * 결과가 상한 오류인지 확인한다.
     *
     * @param r    the result / 결과
     * @param what what was tested / 시험한 것
     */
    private static void limited(XhtmlResult r, String what) {
        Check.notNull(r.getError(), what + ": error expected");
        Check.equal(XmlParseError.Kind.LIMIT_EXCEEDED, r.getError().getKind(), what + ": " + r.getError());
        Check.isTrue(r.getError().getMessage().length() > 0, what + ": message");
    }

    /**
     * A page with far more named references than the default limit of some JREs (64,000 expansions) parses, and the text is complete.
     * <p>
     * 일부 JRE 의 기본 상한(엔티티 6만 4천 번)보다 훨씬 많은 이름 있는 참조가 든 페이지도 해석되고 텍스트가 온전하다.
     */
    public void testManyNamedReferencesAreFine() {
        StringBuilder sb = new StringBuilder("<!DOCTYPE html PUBLIC \"" + STRICT_PUBLIC + "\" \"x.dtd\"><html xmlns=\"" + NS + "\"><body><p>");
        // 이름 있는 참조를 10만 번 쓴다.
        for (int i = 0; i < 100000; i++) {
            sb.append("&nbsp;");
        }
        sb.append("</p></body></html>");
        XhtmlResult r = XhtmlParser.parseDocument(sb.toString(), null);
        Check.isNull(r.getError(), "100000 references: " + r.getError());
        Check.equal(100000, r.getDocument().getDocumentElement().getTextContent().length(), "all references were expanded");
    }
}
