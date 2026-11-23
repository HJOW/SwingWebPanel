package org.duckdns.hjow.swingwebpanel.content.error;

import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlParser;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlResult;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.ElementCollection;
import org.duckdns.hjow.swingwebpanel.html.HtmlParser;
import org.duckdns.hjow.swingwebpanel.net.FailureKind;
import org.duckdns.hjow.swingwebpanel.testing.Check;

/**
 * Tests of {@link ErrorPageBuilder} (plan item P17-06): the content of the pages for each failure, the escaping of everything that comes
 * from outside (the pages are parsed again with the HTML parser to see what really happens to hostile text), the retry link rules, the
 * mapping of the network failure kinds and the determinism of the output.
 * <p>
 * {@link ErrorPageBuilder} 의 시험(계획 P17-06): 실패별 페이지 내용, 바깥에서 온 모든 것의 이스케이프(적대적인 텍스트가 실제로 어떻게 되는지 보기 위해
 * 페이지를 HTML 파서로 다시 해석한다), 다시 시도 링크 규칙, 네트워크 실패 종류의 대응, 출력의 결정성.
 */
public class ErrorPageBuilderTest {

    /**
     * Creates the suite (required by the runner).
     * <p>
     * 스위트를 만든다(실행기가 필요로 함).
     */
    public ErrorPageBuilderTest() {
    }

    /**
     * Parses the HTML of a page the way the component does.
     * <p>
     * 컴포넌트가 하는 대로 페이지의 HTML 을 해석한다.
     *
     * @param page the page / 페이지
     * @return the document / 문서
     */
    private static Document parse(ErrorPage page) {
        return HtmlParser.parseDocument(page.getHtml());
    }

    /**
     * Returns the text of the first element with a tag name.
     * <p>
     * 태그 이름이 같은 첫 요소의 텍스트를 반환한다.
     *
     * @param d    the document / 문서
     * @param name the tag name / 태그 이름
     * @return the text content / 텍스트 내용
     */
    private static String text(Document d, String name) {
        return d.getElementsByTagName(name).item(0).getTextContent();
    }

    /**
     * An HTTP error without a body: heading, title, standard phrase, explanation, retry link and CSS classes.
     * <p>
     * 본문 없는 HTTP 오류: 제목줄, 제목, 표준 문구, 설명, 다시 시도 링크, CSS 클래스.
     */
    public void testHttpStatus() {
        ErrorPage p = ErrorPageBuilder.http("http://example.org/missing?a=1&b=2", 404, null);
        Check.equal(ErrorKind.HTTP_STATUS, p.getKind(), "kind");
        Check.equal(404, p.getStatus(), "status");
        Check.equal("Not Found", p.getTitle(), "title");
        Check.equal("404 Not Found", p.getHeading(), "heading");
        Check.equal("text/html", p.getMimeType(), "media type");
        Check.isTrue(p.hasRetryLink(), "retry link");
        Check.equal("http://example.org/missing?a=1&b=2", p.getUrl(), "the plain address");
        Document d = parse(p);
        Check.equal("404 Not Found", text(d, "title"), "the title element equals the heading");
        Check.equal("404 Not Found", text(d, "h1"), "heading");
        Check.equal("The server answered with an error and no content.", p.getMessage(), "message");
        Element link = (Element) d.getElementsByTagName("a").item(0);
        Check.equal("http://example.org/missing?a=1&b=2", link.getAttribute("href"), "the retry link goes to the failed address");
        Check.equal("http://example.org/missing?a=1&b=2", link.getTextContent(), "and shows it");
        Check.equal("swp-error swp-error-http-status", d.getElementsByTagName("body").item(0).getAttribute("class"), "classes");
        Check.isTrue(p.getHtml().startsWith("<!DOCTYPE html>"), "standards mode page");
        Check.equal(Document.NO_QUIRKS_MODE, d.getCompatibilityMode(), "no quirks");
        Check.equal("Internal Server Error", ErrorPageBuilder.http("http://x/", 500, "").getTitle(), "standard phrase for an empty reason");
        Check.equal("Teapot", ErrorPageBuilder.http("http://x/", 418, " Teapot ").getTitle(), "the phrase of the server is kept (trimmed)");
        Check.equal("418 Teapot", ErrorPageBuilder.http("http://x/", 418, "Teapot").getHeading(), "heading with the phrase of the server");
        Check.equal("Server Error", ErrorPageBuilder.http("http://x/", 599, null).getTitle(), "other 5xx");
        Check.equal("Error", ErrorPageBuilder.http("http://x/", 499, null).getTitle(), "other 4xx");
        Check.equal("Error", ErrorPageBuilder.http("http://x/", 150, null).getTitle(), "other codes");
        Check.equal(0, ErrorPageBuilder.http("http://x/", 0, "Oops").getStatus(), "no status below 100");
        Check.equal("Oops", ErrorPageBuilder.http("http://x/", 0, "Oops").getHeading(), "heading without a status");
    }

    /**
     * The reason phrases of the common statuses.
     * <p>
     * 흔한 상태 코드의 이유 문구.
     */
    public void testReasonPhrases() {
        int[] codes = {400, 401, 403, 404, 405, 408, 410, 429, 451, 500, 501, 502, 503, 504, 505};
        String[] names = {"Bad Request", "Unauthorized", "Forbidden", "Not Found", "Method Not Allowed", "Request Timeout", "Gone", "Too Many Requests",
            "Unavailable For Legal Reasons", "Internal Server Error", "Not Implemented", "Bad Gateway", "Service Unavailable", "Gateway Timeout",
            "HTTP Version Not Supported"};
        // 상태 코드마다 표준 문구를 확인한다.
        for (int i = 0; i < codes.length; i++) {
            Check.equal(names[i], ErrorPageBuilder.reasonPhrase(codes[i]), "phrase of " + codes[i]);
        }
    }

    /**
     * The network and project failures: title, explanation, status and class of each kind.
     * <p>
     * 네트워크와 프로젝트의 실패: 종류별 제목, 설명, 상태, 클래스.
     */
    public void testFailures() {
        Object[][] table = {
            {ErrorKind.DNS, "Server not found", "The server could not be found.", 0},
            {ErrorKind.CONNECT, "Connection failed", "The connection was refused or the server is unreachable.", 0},
            {ErrorKind.TIMEOUT, "Timed out", "The server did not answer in time.", 0},
            {ErrorKind.TLS, "Secure connection failed", "The secure (TLS) connection could not be established.", 0},
            {ErrorKind.TOO_MANY_REDIRECTS, "Too many redirects", "The server redirected the request too many times.", 0},
            {ErrorKind.REDIRECT_LOOP, "Redirect loop", "The server redirected the request in a loop.", 0},
            {ErrorKind.TOO_LARGE, "Response too large", "The response is larger than the limit for pages.", 0},
            {ErrorKind.TRUNCATED, "Incomplete response", "The connection ended before the whole response arrived.", 0},
            {ErrorKind.NOT_FOUND, "Not Found", "The requested resource does not exist.", 404},
            {ErrorKind.FTP_FAILURE, "Not Found", "The FTP address could not be accessed.", 404},
            {ErrorKind.CLASSPATH_MISSING, "Not Found", "The resource does not exist on the class path.", 404},
            {ErrorKind.BLOCKED_SCHEME, "Unsupported address", "This address is not supported. Only http, https, ftp and classpath addresses can be loaded.", 400},
            {ErrorKind.INVALID_URL, "Invalid address", "The address is not valid.", 0},
            {ErrorKind.CORS, "Request blocked", "The cross-origin request was blocked.", 0},
            {ErrorKind.LOAD_FAILED, "Load failed", "The page could not be loaded.", 0}
        };
        // 종류마다 같은 표의 값이 나오는지 확인한다. 상세 메시지는 설명 뒤에 한 문장으로 붙는다.
        for (int i = 0; i < table.length; i++) {
            ErrorKind kind = (ErrorKind) table[i][0];
            ErrorPage p = ErrorPageBuilder.failure(kind, "http://example.org/x", null);
            Check.equal(kind, p.getKind(), "kind " + kind);
            Check.equal(table[i][1], p.getTitle(), "title of " + kind);
            Check.equal(table[i][2], p.getMessage(), "message of " + kind);
            Check.equal(((Integer) table[i][3]).intValue(), p.getStatus(), "status of " + kind);
            Document d = parse(p);
            Check.equal(p.getHeading(), text(d, "title"), "title element of " + kind);
            Check.equal(table[i][2], d.getElementsByClassName("swp-message").item(0).getTextContent(), "message on the page of " + kind);
            ErrorPage withDetail = ErrorPageBuilder.failure(kind, "http://example.org/x", "  java.net.ConnectException: refused  ");
            if (kind != ErrorKind.BLOCKED_SCHEME) {
                Check.equal(table[i][2] + " java.net.ConnectException: refused", withDetail.getHtml().replaceAll("(?s).*<p class=\"swp-message\">(.*?)</p>.*", "$1"), "detail of " + kind);
            }
        }
        Check.equal(ErrorKind.LOAD_FAILED, ErrorPageBuilder.failure(ErrorKind.HTTP_STATUS, "http://x/", null).getKind(), "HTTP_STATUS is not a transport failure");
        Check.equal(ErrorKind.LOAD_FAILED, ErrorPageBuilder.failure(ErrorKind.XML_PARSE_ERROR, "http://x/", null).getKind(), "nor is XML_PARSE_ERROR");
        Check.equal(404, ErrorPageBuilder.classpathMissing("classpath://a/b.html").getStatus(), "classpath missing");
        Check.equal(ErrorKind.CLASSPATH_MISSING, ErrorPageBuilder.classpathMissing("classpath://a/b.html").getKind(), "classpath kind");
        Check.isTrue(ErrorPageBuilder.classpathMissing("classpath://a/b.html").hasRetryLink(), "a classpath address can be tried again");
        Check.equal(404, ErrorPageBuilder.ftpFailure("ftp://example.org/x", "530 Login incorrect").getStatus(), "FTP failures are 404");
        Check.contains(ErrorPageBuilder.ftpFailure("ftp://example.org/x", "530 Login incorrect").getHtml(), "530 Login incorrect", "FTP detail");
    }

    /**
     * Every kind of the network layer maps to the page that the core used before, with status 404 for missing resources and FTP and 400
     * for blocked schemes.
     * <p>
     * 네트워크 계층의 모든 종류는 core 가 쓰던 페이지로 대응하며, 없는 리소스와 FTP 는 상태 404, 차단된 스킴은 400 이다.
     */
    public void testNetworkFailureMapping() {
        FailureKind[] kinds = FailureKind.values();
        ErrorKind[] expected = new ErrorKind[kinds.length];
        // 네트워크 종류마다 기대하는 오류 종류를 정한다. 표에 없는 종류는 일반 실패이다.
        for (int i = 0; i < kinds.length; i++) {
            expected[i] = ErrorKind.LOAD_FAILED;
            switch (kinds[i]) {
            case INVALID_URL:
                expected[i] = ErrorKind.INVALID_URL;
                break;
            case BLOCKED_SCHEME:
                expected[i] = ErrorKind.BLOCKED_SCHEME;
                break;
            case DNS:
                expected[i] = ErrorKind.DNS;
                break;
            case CONNECT:
                expected[i] = ErrorKind.CONNECT;
                break;
            case TIMEOUT:
                expected[i] = ErrorKind.TIMEOUT;
                break;
            case TLS:
                expected[i] = ErrorKind.TLS;
                break;
            case TOO_MANY_REDIRECTS:
                expected[i] = ErrorKind.TOO_MANY_REDIRECTS;
                break;
            case REDIRECT_LOOP:
                expected[i] = ErrorKind.REDIRECT_LOOP;
                break;
            case TOO_LARGE:
                expected[i] = ErrorKind.TOO_LARGE;
                break;
            case TRUNCATED:
                expected[i] = ErrorKind.TRUNCATED;
                break;
            case NOT_FOUND:
                expected[i] = ErrorKind.NOT_FOUND;
                break;
            case FTP_FAILURE:
                expected[i] = ErrorKind.FTP_FAILURE;
                break;
            case CORS:
                expected[i] = ErrorKind.CORS;
                break;
            default:
                break;
            }
            ErrorPage p = ErrorPageBuilder.forFailure(kinds[i], "http://example.org/", "detail");
            Check.equal(expected[i], p.getKind(), "kind for " + kinds[i]);
            Check.notNull(p.getHtml(), "page for " + kinds[i]);
        }
        Check.equal(404, ErrorPageBuilder.forFailure(FailureKind.NOT_FOUND, "http://x/", null).getStatus(), "not found");
        Check.equal(404, ErrorPageBuilder.forFailure(FailureKind.FTP_FAILURE, "ftp://x/", null).getStatus(), "FTP is 404 (project requirement)");
        Check.equal(400, ErrorPageBuilder.forFailure(FailureKind.BLOCKED_SCHEME, "file:///x", null).getStatus(), "blocked scheme");
        Check.equal(0, ErrorPageBuilder.forFailure(FailureKind.DNS, "http://x/", null).getStatus(), "DNS shows no status");
        Check.equal("Connection failed", ErrorPageBuilder.forFailure(FailureKind.CONNECT, "http://x/", null).getTitle(), "same title as before");
    }

    /**
     * Hostile text in the address, in the reason phrase and in the detail never becomes markup: parsing the page again shows only text.
     * <p>
     * 주소, 이유 문구, 상세 내용의 적대적인 텍스트는 마크업이 되지 않는다: 페이지를 다시 해석하면 텍스트만 보인다.
     */
    public void testHostileTextIsEscaped() {
        String url = "http://h/\"><script>alert(1)</script><img src=x onerror=alert(2)>?q='&a=<b>";
        String evil = "<script>alert(3)</script></title><h1 onclick=\"x\">&amp;";
        ErrorPage[] pages = {
            ErrorPageBuilder.http(url, 404, evil),
            ErrorPageBuilder.failure(ErrorKind.DNS, url, evil),
            ErrorPageBuilder.failure(ErrorKind.CONNECT, url, evil),
            ErrorPageBuilder.ftpFailure(url, evil),
            ErrorPageBuilder.forFailure(FailureKind.TLS, url, evil),
            ErrorPageBuilder.blockedScheme(url),
            ErrorPageBuilder.classpathMissing(url)
        };
        // 페이지마다 다시 해석한 DOM 에 script, img, 이벤트 속성이 없어야 하고 문자열은 그대로 텍스트로 보여야 한다.
        for (int i = 0; i < pages.length; i++) {
            Document d = parse(pages[i]);
            String what = "page " + i;
            Check.equal(0, d.getElementsByTagName("script").getLength(), what + ": no script element");
            Check.equal(0, d.getElementsByTagName("img").getLength(), what + ": no img element");
            Check.equal(1, d.getElementsByTagName("h1").getLength(), what + ": exactly one heading");
            Check.equal(1, d.getElementsByTagName("title").getLength(), what + ": exactly one title");
            Check.isNull(d.getElementsByTagName("h1").item(0).getAttribute("onclick"), what + ": no event attribute");
            Check.isNull(d.getElementsByTagName("body").item(0).getAttribute("onclick"), what + ": no event attribute on body");
            Check.equal(0, eventAttributes(d), what + ": no event handler attributes at all");
            Check.isFalse(pages[i].getHtml().indexOf("<script>alert") >= 0, what + ": no raw script tag");
            Check.isFalse(pages[i].getHtml().indexOf("<img src=x") >= 0, what + ": no raw img tag");
            ElementCollection links = d.getElementsByTagName("a");
            if (links.getLength() > 0) {
                Check.equal(url, ((Element) links.item(0)).getAttribute("href"), what + ": the href is exactly the address");
            }
            Check.contains(d.getElementsByClassName("swp-url").item(0).getTextContent(), "alert(1)", what + ": the address is shown as text");
        }
        Check.contains(parse(pages[0]).getElementsByTagName("title").item(0).getTextContent(), evil, "the reason phrase is shown as text in the title");
        Check.contains(parse(pages[1]).getElementsByClassName("swp-message").item(0).getTextContent(), evil, "the detail is shown as text");
    }

    /**
     * Counts the attributes whose name starts with <code>on</code> in a document.
     * <p>
     * 문서에서 이름이 <code>on</code> 으로 시작하는 속성의 수를 센다.
     *
     * @param d the document / 문서
     * @return the number of event handler attributes / 이벤트 핸들러 속성의 수
     */
    private static int eventAttributes(Document d) {
        int n = 0;
        ElementCollection all = d.getElementsByTagName("*");
        // 모든 요소의 속성 이름을 확인한다.
        for (int i = 0; i < all.getLength(); i++) {
            java.util.List<String> names = ((Element) all.item(i)).getAttributeNames();
            // 요소의 속성 이름을 하나씩 확인한다.
            for (int k = 0; k < names.size(); k++) {
                if (names.get(k).startsWith("on")) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * The escaping function: markup characters, quotes, control and bidirectional characters.
     * <p>
     * 이스케이프 함수: 마크업 문자, 따옴표, 제어 문자, 양방향 문자.
     */
    public void testEscape() {
        Check.equal("", ErrorPageBuilder.escape(null), "null");
        Check.equal("plain text 123", ErrorPageBuilder.escape("plain text 123"), "nothing to escape");
        Check.equal("&amp;&lt;&gt;&quot;&#39;", ErrorPageBuilder.escape("&<>\"'"), "the five characters");
        Check.equal("a&amp;amp;b", ErrorPageBuilder.escape("a&amp;b"), "an existing reference is escaped, not kept");
        Check.equal("a\tb\nc", ErrorPageBuilder.escape("a\tb\nc"), "tab and line feed stay");
        String f = String.valueOf((char) 0xFFFD);
        Check.equal(f + f + f + f, ErrorPageBuilder.escape("" + (char) 0 + (char) 1 + (char) 13 + (char) 0x7F), "control characters");
        Check.equal(f + f, ErrorPageBuilder.escape("" + (char) 0x80 + (char) 0x9F), "C1 controls");
        Check.equal(f + f + f + f + f + f + f + f + f, ErrorPageBuilder.escape("" + (char) 0x202A + (char) 0x202B + (char) 0x202C + (char) 0x202D + (char) 0x202E
                + (char) 0x2066 + (char) 0x2067 + (char) 0x2068 + (char) 0x2069), "bidirectional overrides and isolates");
        Check.equal(f + f + f, ErrorPageBuilder.escape("" + (char) 0x200E + (char) 0x200F + (char) 0x061C), "direction marks");
        Check.equal("한글 \uD83D\uDE00 é", ErrorPageBuilder.escape("한글 \uD83D\uDE00 é"), "other characters stay");
    }

    /**
     * The retry link: only for addresses that the component can load, and without links for blocked schemes, white space, control
     * characters and absurd lengths; long addresses are cut for display.
     * <p>
     * 다시 시도 링크: 컴포넌트가 읽을 수 있는 주소에만 만들고, 차단된 스킴, 공백, 제어 문자, 터무니없는 길이에는 만들지 않는다. 긴 주소는 표시할 때 자른다.
     */
    public void testRetryLink() {
        String[] linked = {"http://example.org/", "HTTPS://example.org/a?b#c", "ftp://example.org/dir/", "classpath://pages/a.html", "http://example.org/%20x"};
        // 읽을 수 있는 주소에는 링크가 있다.
        for (int i = 0; i < linked.length; i++) {
            Check.isTrue(ErrorPageBuilder.http(linked[i], 404, null).hasRetryLink(), "link for " + linked[i]);
            Check.equal(1, parse(ErrorPageBuilder.http(linked[i], 404, null)).getElementsByTagName("a").getLength(), "a element for " + linked[i]);
        }
        String[] plain = {"javascript:alert(1)", "data:text/html,<b>", "file:///etc/passwd", "mailto:a@b.c", "blob:http://x/y", "about:blank", "ws://x/", "no-scheme",
            "http://example.org/a b", "http://example.org/a\tb", "http://example.org/a\nb", "http://example.org/a" + (char) 0 + "b", "", "://x", "vbscript:x"};
        // 읽지 않는 주소나 쓸 수 없는 문자가 든 주소에는 링크가 없다.
        for (int i = 0; i < plain.length; i++) {
            ErrorPage p = ErrorPageBuilder.failure(ErrorKind.CONNECT, plain[i], null);
            Check.isFalse(p.hasRetryLink(), "no link for [" + plain[i] + "]");
            Check.equal(0, parse(p).getElementsByTagName("a").getLength(), "no a element for [" + plain[i] + "]");
        }
        StringBuilder sb = new StringBuilder("http://example.org/");
        // 3000 자 주소: 링크는 있고 표시는 2000 자에서 잘린다.
        for (int i = 0; i < 3000; i++) {
            sb.append('x');
        }
        ErrorPage longPage = ErrorPageBuilder.http(sb.toString(), 404, null);
        Check.isTrue(longPage.hasRetryLink(), "a long address still gets a link");
        Element a = (Element) parse(longPage).getElementsByTagName("a").item(0);
        Check.equal(sb.toString(), a.getAttribute("href"), "the href is complete");
        Check.equal(ErrorPageBuilder.MAX_DISPLAY_LENGTH + 3, a.getTextContent().length(), "the shown text is cut");
        Check.isTrue(a.getTextContent().endsWith("..."), "and marked");
        // 주소를 8192 자 넘게 늘린다.
        for (int i = 0; i < 6000; i++) {
            sb.append('y');
        }
        ErrorPage huge = ErrorPageBuilder.http(sb.toString(), 404, null);
        Check.isFalse(huge.hasRetryLink(), "an address over 8192 characters gets no link");
        Check.isTrue(huge.getHtml().length() < 10000, "and the page stays small");
        Check.equal(sb.toString(), huge.getUrl(), "but the plain address is available");
        Check.isFalse(ErrorPageBuilder.http(null, 404, null).hasRetryLink(), "null address");
        Check.equal(0, parse(ErrorPageBuilder.http(null, 404, null)).getElementsByClassName("swp-url").getLength(), "no address paragraph for null");
    }

    /**
     * The blocked scheme page has no link and the right explanation.
     * <p>
     * 차단된 스킴의 페이지에는 링크가 없고 설명이 맞다.
     */
    public void testBlockedScheme() {
        ErrorPage p = ErrorPageBuilder.blockedScheme("file:///c:/secret.txt");
        Check.equal(400, p.getStatus(), "status");
        Check.equal("400 Unsupported address", p.getHeading(), "heading");
        Check.isFalse(p.hasRetryLink(), "no link");
        Check.contains(p.getMessage(), "Only http, https, ftp and classpath", "explanation");
        Check.contains(parse(p).getElementsByClassName("swp-url").item(0).getTextContent(), "file:///c:/secret.txt", "the address is shown");
    }

    /**
     * An XML parsing error: heading, message, position, the source line and the marker under the column; hostile source lines are
     * escaped.
     * <p>
     * XML 해석 오류: 제목줄, 메시지, 위치, 소스 줄, 열 아래의 표시. 적대적인 소스 줄은 이스케이프한다.
     */
    public void testXmlParseError() {
        XhtmlResult r = XhtmlParser.parseDocument("<?xml version=\"1.0\"?>\n<a>\n<b></c>\n</a>", "http://example.org/p.xhtml");
        Check.notNull(r.getError(), "error");
        ErrorPage p = ErrorPageBuilder.xmlParseError("http://example.org/p.xhtml", r.getError());
        Check.equal(ErrorKind.XML_PARSE_ERROR, p.getKind(), "kind");
        Check.equal("XML Parsing Error", p.getTitle(), "title");
        Check.equal(0, p.getStatus(), "no status");
        Check.isTrue(p.hasRetryLink(), "the document can be loaded again");
        Check.equal(r.getError().getMessage(), p.getMessage(), "the message of the parser");
        Check.contains(p.getDetail(), "Line Number 3, Column " + r.getError().getColumn() + ":\n<b></c>\n", "position and source line");
        Check.isTrue(p.getDetail().endsWith("^"), "marker under the column: " + p.getDetail());
        Document d = parse(p);
        Check.equal(p.getDetail(), d.getElementsByClassName("swp-detail").item(0).getTextContent(), "the detail is in a pre element");
        Check.equal("pre", d.getElementsByClassName("swp-detail").item(0).getLocalName(), "pre element");
        Check.equal("swp-error swp-error-xml-parse-error", d.getElementsByTagName("body").item(0).getAttribute("class"), "classes");
        XhtmlResult h = XhtmlParser.parseDocument("<a>\n<img src=\"x\" onerror=\"<script>alert(1)</script>\"/>\n</a>", null);
        Check.notNull(h.getError(), "error in a hostile line");
        ErrorPage hp = ErrorPageBuilder.xmlParseError("http://example.org/h.xhtml", h.getError());
        Document hd = parse(hp);
        Check.equal(0, hd.getElementsByTagName("script").getLength(), "no script from the source line");
        Check.equal(0, hd.getElementsByTagName("img").getLength(), "no img from the source line");
        Check.contains(hd.getElementsByClassName("swp-detail").item(0).getTextContent(), "<img src=\"x\" onerror=", "the source line is shown as text");
        // 위치를 모르는 오류와 다른 종류의 오류도 페이지가 된다.
        XhtmlResult empty = XhtmlParser.parseDocument("", null);
        Check.notNull(ErrorPageBuilder.xmlParseError("http://x/", empty.getError()).getHtml(), "error at the very start");
        // 상한 오류와 인코딩 오류도 각자의 제목으로 페이지가 된다.
        XhtmlParser limited = new XhtmlParser();
        limited.setLimits(new org.duckdns.hjow.swingwebpanel.content.xhtml.XhtmlLimits().setMaxDepth(2));
        XhtmlResult deep = limited.parse("<a><b><c/></b></a>", null);
        Check.equal("XML Limit Exceeded", ErrorPageBuilder.xmlParseError("http://x/", deep.getError()).getTitle(), "limit error title");
        XhtmlResult bad = XhtmlParser.parseDocument(new byte[] {'<', 'a', '>', (byte) 0xFF}, null, null);
        Check.equal("XML Encoding Error", ErrorPageBuilder.xmlParseError("http://x/", bad.getError()).getTitle(), "encoding error title");
    }

    /**
     * The output is deterministic and the generated pages are small, well-formed enough for the HTML parser and contain what a host needs.
     * <p>
     * 출력은 결정적이고, 생성한 페이지는 작으며 HTML 파서에 알맞고 호스트에 필요한 것을 담는다.
     */
    public void testDeterminismAndStructure() {
        String a = ErrorPageBuilder.http("http://example.org/x", 503, "Service Unavailable").getHtml();
        String b = ErrorPageBuilder.http("http://example.org/x", 503, "Service Unavailable").getHtml();
        Check.equal(a, b, "same input, same page");
        Check.isTrue(a.length() < 1500, "small page: " + a.length());
        Check.isTrue(a.indexOf("<meta charset=\"utf-8\">") > 0, "declares its encoding");
        Check.isTrue(a.indexOf("<html lang=\"en\">") > 0, "language");
        Document d = HtmlParser.parseDocument(a);
        Check.equal("503 Service Unavailable", text(d, "title"), "title");
        Check.equal(1, d.getElementsByTagName("h1").getLength(), "one heading");
        Check.equal(1, d.getElementsByTagName("style").getLength(), "one style element");
        Check.equal(0, d.getElementsByTagName("script").getLength(), "never a script");
        Check.equal(0, d.getElementsByTagName("pre").getLength(), "no detail block for an HTTP error");
        ErrorPage nulls = ErrorPageBuilder.build(ErrorKind.LOAD_FAILED, null, 0, null, null, null);
        Check.equal("", nulls.getUrl(), "null address becomes empty");
        Check.equal("", nulls.getTitle(), "null title becomes empty");
        Check.notNull(nulls.getHtml(), "a page for null values");
        Check.equal(ErrorKind.values().length, 17, "the number of kinds (update the tests when a kind is added)");
    }
}
