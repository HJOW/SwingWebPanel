package org.duckdns.hjow.swingwebpanel.content.error;

import org.duckdns.hjow.swingwebpanel.content.xhtml.XmlParseError;
import org.duckdns.hjow.swingwebpanel.net.FailureKind;

/**
 * The generator of the error pages (plan item P17-06): HTTP error statuses without a body, DNS, connection, timeout and TLS failures,
 * redirect problems, blocked schemes, missing <code>classpath://</code> resources, FTP failures (shown as 404, a project requirement),
 * invalid addresses and XML parsing errors of XHTML documents. Every method returns an {@link ErrorPage}: the HTML source plus the
 * plain facts. This class replaces the small <code>core.ErrorPages</code>, whose titles and explanations it keeps word for word.
 * <p>
 * The pages are deterministic and safe:
 * <ul>
 * <li>Everything that comes from outside (the address, the reason phrase of the server, the messages of the network layer and of the XML
 * parser, a line of the broken document) is HTML-escaped by {@link #escape(String)}: the five markup characters, control characters and
 * the bidirectional override characters (which could disguise an address) are neutralized. An address can never become markup.</li>
 * <li>The page has a stable <code>&lt;title&gt;</code> that equals the heading (<code>404 Not Found</code>), the explanation, the
 * address, and, if the address can be loaded again, a link to it (<code>&lt;a href&gt;</code>) as the retry link. An address with a
 * scheme that the component does not load (<code>javascript:</code>, <code>data:</code>, <code>file:</code> and so on) or with white space or control
 * characters is shown as text without a link; so is an address longer than {@value #MAX_LINK_LENGTH} characters. An address that is
 * displayed is cut to {@value #MAX_DISPLAY_LENGTH} characters.</li>
 * <li>The page uses only plain HTML and a few simple CSS rules, and the CSS classes <code>swp-error</code> and
 * <code>swp-error-&lt;kind&gt;</code> on the <code>body</code>.</li>
 * </ul>
 * <p>
 * 오류 페이지 생성기(계획 P17-06): 본문 없는 HTTP 오류 상태, DNS, 연결, 시간 초과, TLS 실패, 리디렉션 문제, 차단된 스킴, 없는 <code>classpath://</code>
 * 리소스, FTP 실패(404 로 표시. 프로젝트 요구사항), 잘못된 주소, XHTML 문서의 XML 해석 오류. 모든 메서드는 {@link ErrorPage}(HTML 원문과 이스케이프하지
 * 않은 사실들)를 반환한다. 이 클래스는 작은 <code>core.ErrorPages</code> 를 대체하며 그 제목과 설명을 그대로 유지한다.
 * <p>
 * 페이지는 결정적이고 안전하다:
 * <ul>
 * <li>바깥에서 온 모든 것(주소, 서버의 이유 문구, 네트워크 계층과 XML 파서의 메시지, 깨진 문서의 한 줄)은 {@link #escape(String)} 로 HTML 이스케이프한다:
 * 마크업 문자 다섯 개, 제어 문자, 주소를 위장할 수 있는 양방향 덮어쓰기 문자를 무력화한다. 주소는 마크업이 될 수 없다.</li>
 * <li>페이지에는 제목줄과 같은 안정적인 <code>&lt;title&gt;</code>(<code>404 Not Found</code>), 설명, 주소, 그리고 주소를 다시 읽을 수 있으면 다시 시도 링크로서
 * 그 주소로 가는 <code>&lt;a href&gt;</code> 가 있다. 컴포넌트가 읽지 않는 스킴(<code>javascript:</code>, <code>data:</code>, <code>file:</code> 등)이나 공백/제어 문자가
 * 든 주소, {@value #MAX_LINK_LENGTH} 자보다 긴 주소는 링크 없이 텍스트로만 보인다. 표시하는 주소는 {@value #MAX_DISPLAY_LENGTH} 자에서 자른다.</li>
 * <li>페이지는 일반 HTML 과 단순한 CSS 몇 줄만 쓰며 <code>body</code> 에 CSS 클래스 <code>swp-error</code> 와 <code>swp-error-&lt;종류&gt;</code> 를 단다.</li>
 * </ul>
 */
public final class ErrorPageBuilder {

    /**
     * The longest address that gets a link.
     * <p>
     * 링크를 만드는 주소의 최대 길이.
     */
    public static final int MAX_LINK_LENGTH = 8192;

    /**
     * The longest address text that is displayed (longer text is cut and ends with three dots).
     * <p>
     * 표시하는 주소 텍스트의 최대 길이(더 길면 잘리고 점 세 개로 끝난다).
     */
    public static final int MAX_DISPLAY_LENGTH = 2000;

    /**
     * The explanation of an HTTP error without a body.
     * <p>
     * 본문 없는 HTTP 오류의 설명.
     */
    private static final String HTTP_MESSAGE = "The server answered with an error and no content.";

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ErrorPageBuilder() {
    }

    /**
     * Builds the page for an HTTP error status that came without a body.
     * <p>
     * 본문 없이 온 HTTP 오류 상태의 페이지를 만든다.
     *
     * @param url    the address that was requested / 요청한 주소
     * @param status the HTTP status (a value below 100 shows no status) / HTTP 상태(100 미만이면 상태를 표시하지 않는다)
     * @param reason the reason phrase of the server, or null or "" for the standard phrase / 서버의 이유 문구. null 이나 ""이면 표준 문구
     * @return the page / 페이지
     */
    public static ErrorPage http(String url, int status, String reason) {
        String phrase = reason == null || reason.trim().length() == 0 ? reasonPhrase(status) : reason.trim();
        return build(ErrorKind.HTTP_STATUS, url, status < 100 ? 0 : status, phrase, HTTP_MESSAGE, "");
    }

    /**
     * Builds the page for a failure of one of the kinds, with the standard title and explanation.
     * <p>
     * 종류에 맞는 표준 제목과 설명으로 실패 페이지를 만든다.
     *
     * @param kind   the kind, other than {@link ErrorKind#HTTP_STATUS} and {@link ErrorKind#XML_PARSE_ERROR} (these are treated as
     *               {@link ErrorKind#LOAD_FAILED}) / 종류. {@link ErrorKind#HTTP_STATUS}, {@link ErrorKind#XML_PARSE_ERROR} 는
     *               {@link ErrorKind#LOAD_FAILED} 로 다룬다
     * @param url    the address that was requested / 요청한 주소
     * @param detail the message of the layer that failed, or null / 실패한 계층의 메시지. 없으면 null
     * @return the page / 페이지
     */
    public static ErrorPage failure(ErrorKind kind, String url, String detail) {
        String d = detail == null ? "" : detail.trim();
        switch (kind) {
        case DNS:
            return build(kind, url, 0, "Server not found", "The server could not be found.", d);
        case CONNECT:
            return build(kind, url, 0, "Connection failed", "The connection was refused or the server is unreachable.", d);
        case TIMEOUT:
            return build(kind, url, 0, "Timed out", "The server did not answer in time.", d);
        case TLS:
            return build(kind, url, 0, "Secure connection failed", "The secure (TLS) connection could not be established.", d);
        case TOO_MANY_REDIRECTS:
            return build(kind, url, 0, "Too many redirects", "The server redirected the request too many times.", d);
        case REDIRECT_LOOP:
            return build(kind, url, 0, "Redirect loop", "The server redirected the request in a loop.", d);
        case TOO_LARGE:
            return build(kind, url, 0, "Response too large", "The response is larger than the limit for pages.", d);
        case TRUNCATED:
            return build(kind, url, 0, "Incomplete response", "The connection ended before the whole response arrived.", d);
        case NOT_FOUND:
            return build(kind, url, 404, "Not Found", "The requested resource does not exist.", d);
        case FTP_FAILURE:
            return build(kind, url, 404, "Not Found", "The FTP address could not be accessed.", d);
        case CLASSPATH_MISSING:
            return build(kind, url, 404, "Not Found", "The resource does not exist on the class path.", d);
        case BLOCKED_SCHEME:
            return blockedScheme(url);
        case INVALID_URL:
            return build(kind, url, 0, "Invalid address", "The address is not valid.", d);
        case CORS:
            return build(kind, url, 0, "Request blocked", "The cross-origin request was blocked.", d);
        default:
            return build(ErrorKind.LOAD_FAILED, url, 0, "Load failed", "The page could not be loaded.", d);
        }
    }

    /**
     * Builds the page for a failure that the network layer reported (see {@link FailureKind}). A cancelled load, which has no page of
     * its own, and every kind without a special page become {@link ErrorKind#LOAD_FAILED}. Missing resources and FTP failures are
     * shown with status 404 and a blocked scheme with status 400.
     * <p>
     * 네트워크 계층이 보고한 실패({@link FailureKind} 참조)의 페이지를 만든다. 자기 페이지가 없는 취소된 로드와 특별한 페이지가 없는 모든 종류는
     * {@link ErrorKind#LOAD_FAILED} 가 된다. 없는 리소스와 FTP 실패는 상태 404, 차단된 스킴은 상태 400 으로 표시한다.
     *
     * @param kind   the failure kind / 실패 종류
     * @param url    the address that was requested / 요청한 주소
     * @param detail the message of the network layer or null / 네트워크 계층의 메시지. 없으면 null
     * @return the page / 페이지
     */
    public static ErrorPage forFailure(FailureKind kind, String url, String detail) {
        switch (kind) {
        case INVALID_URL:
            return failure(ErrorKind.INVALID_URL, url, detail);
        case BLOCKED_SCHEME:
            return blockedScheme(url);
        case DNS:
            return failure(ErrorKind.DNS, url, detail);
        case CONNECT:
            return failure(ErrorKind.CONNECT, url, detail);
        case TIMEOUT:
            return failure(ErrorKind.TIMEOUT, url, detail);
        case TLS:
            return failure(ErrorKind.TLS, url, detail);
        case TOO_MANY_REDIRECTS:
            return failure(ErrorKind.TOO_MANY_REDIRECTS, url, detail);
        case REDIRECT_LOOP:
            return failure(ErrorKind.REDIRECT_LOOP, url, detail);
        case TOO_LARGE:
            return failure(ErrorKind.TOO_LARGE, url, detail);
        case TRUNCATED:
            return failure(ErrorKind.TRUNCATED, url, detail);
        case NOT_FOUND:
            return failure(ErrorKind.NOT_FOUND, url, detail);
        case FTP_FAILURE:
            return failure(ErrorKind.FTP_FAILURE, url, detail);
        case CORS:
            return failure(ErrorKind.CORS, url, detail);
        default:
            return failure(ErrorKind.LOAD_FAILED, url, detail);
        }
    }

    /**
     * Builds the page for an address whose scheme is not supported (status 400, no retry link).
     * <p>
     * 지원하지 않는 스킴의 주소에 대한 페이지를 만든다(상태 400, 다시 시도 링크 없음).
     *
     * @param url the address / 주소
     * @return the page / 페이지
     */
    public static ErrorPage blockedScheme(String url) {
        return build(ErrorKind.BLOCKED_SCHEME, url, 400, "Unsupported address",
                "This address is not supported. Only http, https, ftp and classpath addresses can be loaded.", "");
    }

    /**
     * Builds the page for a <code>classpath://</code> resource that does not exist (status 404).
     * <p>
     * 존재하지 않는 <code>classpath://</code> 리소스의 페이지를 만든다(상태 404).
     *
     * @param url the address (<code>classpath://...</code>) / 주소(<code>classpath://...</code>)
     * @return the page / 페이지
     */
    public static ErrorPage classpathMissing(String url) {
        return failure(ErrorKind.CLASSPATH_MISSING, url, null);
    }

    /**
     * Builds the page for a failed FTP access (status 404, as the project requires).
     * <p>
     * 실패한 FTP 접근의 페이지를 만든다(프로젝트 요구사항대로 상태 404).
     *
     * @param url    the FTP address / FTP 주소
     * @param detail the message of the FTP layer or null / FTP 계층의 메시지. 없으면 null
     * @return the page / 페이지
     */
    public static ErrorPage ftpFailure(String url, String detail) {
        return failure(ErrorKind.FTP_FAILURE, url, detail);
    }

    /**
     * Builds the page for an XML parsing error of an XHTML document: the message of the parser, then the position and the offending line
     * with a marker under the column.
     * <p>
     * XHTML 문서의 XML 해석 오류 페이지를 만든다: 파서의 메시지, 그 뒤에 위치와 문제의 줄(열 아래에 표시를 붙인다).
     *
     * @param url   the address of the document / 문서의 주소
     * @param error the error from the XHTML parser / XHTML 파서의 오류
     * @return the page / 페이지
     */
    public static ErrorPage xmlParseError(String url, XmlParseError error) {
        String title;
        switch (error.getKind()) {
        case ENCODING:
            title = "XML Encoding Error";
            break;
        case LIMIT_EXCEEDED:
            title = "XML Limit Exceeded";
            break;
        case INTERNAL:
            title = "XML Parser Failure";
            break;
        default:
            title = "XML Parsing Error";
            break;
        }
        StringBuilder d = new StringBuilder();
        if (error.getLine() > 0) {
            d.append("Line Number ").append(error.getLine());
            if (error.getColumn() > 0) {
                d.append(", Column ").append(error.getColumn());
            }
            d.append(':');
            String src = error.getSourceLine();
            if (src.length() > 0) {
                d.append('\n').append(src);
                int col = error.getColumn();
                // 열이 줄 안에 있으면 그 아래에 하이픈과 ^ 로 위치를 표시한다.
                if (col >= 1 && col <= src.length() + 1) {
                    d.append('\n');
                    // 열 앞의 글자 수만큼 하이픈을 쓴다.
                    for (int i = 1; i < col; i++) {
                        d.append('-');
                    }
                    d.append('^');
                }
            }
        }
        return build(ErrorKind.XML_PARSE_ERROR, url, 0, title, error.getMessage(), d.toString());
    }

    /**
     * Builds a page from its parts (the common part of all other methods).
     * <p>
     * 부분들로 페이지를 만든다(다른 모든 메서드의 공통 부분).
     *
     * @param kind    kind / 종류
     * @param url     the address (may be null) / 주소(null 가능)
     * @param status  status or 0 / 상태 또는 0
     * @param title   short title / 짧은 제목
     * @param message explanation / 설명
     * @param detail  extra text or "" / 덧붙이는 텍스트 또는 ""
     * @return the page / 페이지
     */
    public static ErrorPage build(ErrorKind kind, String url, int status, String title, String message, String detail) {
        String u = url == null ? "" : url;
        String t = title == null ? "" : title;
        String m = message == null ? "" : message;
        String d = detail == null ? "" : detail;
        // 설명 뒤에 계층의 상세 메시지를 한 문장으로 덧붙인다(XML 오류의 위치 정보는 따로 pre 에 둔다).
        String shownMessage = kind == ErrorKind.XML_PARSE_ERROR || d.length() == 0 ? m : m + " " + d;
        String shownDetail = kind == ErrorKind.XML_PARSE_ERROR ? d : "";
        boolean link = isRetryable(u);
        String heading = status > 0 ? status + " " + t : t;
        String kindClass = kind.name().toLowerCase().replace('_', '-');
        StringBuilder sb = new StringBuilder(1024);
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n<title>").append(escape(heading)).append("</title>\n");
        sb.append("<style>\nbody { font-family: sans-serif; margin: 2em; }\nh1 { font-size: 1.5em; }\n");
        sb.append(".swp-url { word-wrap: break-word; }\npre.swp-detail { background: #f0f0f0; padding: 0.5em; white-space: pre-wrap; word-wrap: break-word; }\n</style>\n</head>\n");
        sb.append("<body class=\"swp-error swp-error-").append(kindClass).append("\">\n<h1>").append(escape(heading)).append("</h1>\n");
        sb.append("<p class=\"swp-message\">").append(escape(shownMessage)).append("</p>\n");
        if (shownDetail.length() > 0) {
            sb.append("<pre class=\"swp-detail\">").append(escape(shownDetail)).append("</pre>\n");
        }
        if (u.length() > 0) {
            String shown = escape(shorten(u, MAX_DISPLAY_LENGTH));
            if (link) {
                sb.append("<p class=\"swp-url\">Try again: <a href=\"").append(escape(u)).append("\">").append(shown).append("</a></p>\n");
            } else {
                sb.append("<p class=\"swp-url\">").append(shown).append("</p>\n");
            }
        }
        sb.append("</body>\n</html>\n");
        return new ErrorPage(kind, status > 0 ? status : 0, t, m, d, u, link && u.length() > 0, sb.toString());
    }

    /**
     * Tells whether an address gets a retry link: an address with an http, https, ftp or classpath scheme, without white space or
     * control characters, and not longer than {@link #MAX_LINK_LENGTH}.
     * <p>
     * 주소에 다시 시도 링크를 다는지 알려 준다: http, https, ftp, classpath 스킴이며 공백이나 제어 문자가 없고 {@link #MAX_LINK_LENGTH} 를 넘지 않는 주소.
     *
     * @param url the address / 주소
     * @return true if it gets a link / 링크를 달면 true
     */
    static boolean isRetryable(String url) {
        if (url.length() == 0 || url.length() > MAX_LINK_LENGTH) {
            return false;
        }
        // 공백이나 제어 문자가 있으면 링크로 쓰지 않는다.
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c <= 0x20 || c >= 0x7F && c <= 0x9F) {
                return false;
            }
        }
        int colon = url.indexOf(':');
        if (colon <= 0) {
            return false;
        }
        String scheme = url.substring(0, colon).toLowerCase();
        return "http".equals(scheme) || "https".equals(scheme) || "ftp".equals(scheme) || "classpath".equals(scheme);
    }

    /**
     * Cuts a text to a maximum length (without splitting a surrogate pair) and marks the cut with three dots.
     * <p>
     * 텍스트를 최대 길이에서 자르고(서로게이트 쌍은 나누지 않는다) 잘렸음을 점 세 개로 표시한다.
     *
     * @param s   the text / 텍스트
     * @param max the maximum length / 최대 길이
     * @return the text, shortened if needed / 필요하면 줄인 텍스트
     */
    static String shorten(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        int end = max;
        if (Character.isHighSurrogate(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end) + "...";
    }

    /**
     * Escapes a text for HTML text and attribute values. The characters <code>&amp; &lt; &gt; " '</code> become character references,
     * control characters (except tab and line feed), the C1 controls and the bidirectional formatting characters
     * (U+200E, U+200F, U+061C, U+202A to U+202E, U+2066 to U+2069) become U+FFFD so that they cannot hide or reorder the text of an
     * address. null is treated as empty.
     * <p>
     * 텍스트를 HTML 본문과 속성 값용으로 이스케이프한다. <code>&amp; &lt; &gt; " '</code> 는 문자 참조가 되고, 제어 문자(탭과 줄 바꿈 제외), C1 제어 문자,
     * 양방향 서식 문자(U+200E, U+200F, U+061C, U+202A~U+202E, U+2066~U+2069)는 주소의 텍스트를 숨기거나 순서를 바꾸지 못하도록 U+FFFD 가 된다.
     * null 은 빈 문자열로 본다.
     *
     * @param s the text or null / 텍스트 또는 null
     * @return the escaped text / 이스케이프한 텍스트
     */
    public static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length() + 16);
        // 글자마다 HTML 특수 문자는 문자 참조로, 제어 문자와 양방향 서식 문자는 대체 문자로 바꾼다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            case '"':
                sb.append("&quot;");
                break;
            case '\'':
                sb.append("&#39;");
                break;
            default:
                if (isNeutralized(c)) {
                    sb.append((char) 0xFFFD);
                } else {
                    sb.append(c);
                }
                break;
            }
        }
        return sb.toString();
    }

    /**
     * Tells whether a character is replaced by U+FFFD in escaped text.
     * <p>
     * 이스케이프한 텍스트에서 문자가 U+FFFD 로 바뀌는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true if it is replaced / 바뀌면 true
     */
    private static boolean isNeutralized(char c) {
        if (c < 0x20) {
            return c != '\t' && c != '\n';
        }
        return c >= 0x7F && c <= 0x9F || c == 0x200E || c == 0x200F || c == 0x061C || c >= 0x202A && c <= 0x202E || c >= 0x2066 && c <= 0x2069;
    }

    /**
     * Returns the standard reason phrase of an HTTP status code.
     * <p>
     * HTTP 상태 코드의 표준 이유 문구를 반환한다.
     *
     * @param status the status code / 상태 코드
     * @return the phrase; <code>Server Error</code> for other 5xx and <code>Error</code> for everything else / 문구. 그 밖의 5xx 는
     *         <code>Server Error</code>, 나머지는 <code>Error</code>
     */
    public static String reasonPhrase(int status) {
        switch (status) {
        case 400:
            return "Bad Request";
        case 401:
            return "Unauthorized";
        case 402:
            return "Payment Required";
        case 403:
            return "Forbidden";
        case 404:
            return "Not Found";
        case 405:
            return "Method Not Allowed";
        case 406:
            return "Not Acceptable";
        case 407:
            return "Proxy Authentication Required";
        case 408:
            return "Request Timeout";
        case 409:
            return "Conflict";
        case 410:
            return "Gone";
        case 411:
            return "Length Required";
        case 412:
            return "Precondition Failed";
        case 413:
            return "Payload Too Large";
        case 414:
            return "URI Too Long";
        case 415:
            return "Unsupported Media Type";
        case 416:
            return "Range Not Satisfiable";
        case 417:
            return "Expectation Failed";
        case 421:
            return "Misdirected Request";
        case 422:
            return "Unprocessable Content";
        case 426:
            return "Upgrade Required";
        case 428:
            return "Precondition Required";
        case 429:
            return "Too Many Requests";
        case 431:
            return "Request Header Fields Too Large";
        case 451:
            return "Unavailable For Legal Reasons";
        case 500:
            return "Internal Server Error";
        case 501:
            return "Not Implemented";
        case 502:
            return "Bad Gateway";
        case 503:
            return "Service Unavailable";
        case 504:
            return "Gateway Timeout";
        case 505:
            return "HTTP Version Not Supported";
        case 507:
            return "Insufficient Storage";
        case 508:
            return "Loop Detected";
        case 511:
            return "Network Authentication Required";
        default:
            return status >= 500 ? "Server Error" : "Error";
        }
    }
}
