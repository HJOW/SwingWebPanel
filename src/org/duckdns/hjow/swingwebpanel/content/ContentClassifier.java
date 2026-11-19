package org.duckdns.hjow.swingwebpanel.content;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XmlDecoder;
import org.duckdns.hjow.swingwebpanel.content.xhtml.XmlProlog;

/**
 * Decides which processor shows a body (plan items P17-01, P01-17, P01-18). One decision table serves the network (a response with
 * a <code>Content-Type</code> and maybe <code>Content-Disposition: attachment</code>) and <code>setContent</code> (a MIME argument), so
 * both paths use the same content handling.
 * <p>
 * <b>Policy for responses</b> ({@link #classifyResponse(String, boolean, byte[])}):
 * <ol>
 * <li><code>Content-Disposition: attachment</code> always means a download.</li>
 * <li>A missing or malformed <code>Content-Type</code> is not an error: the type is found by looking at the first bytes
 * ({@link #sniff(byte[])}, the "unknown MIME type" algorithm of the WHATWG MIME Sniffing Standard in a reduced form: HTML tags,
 * <code>&lt;?xml</code>, PDF, image/audio/video signatures, archives, then "text or binary"). The result is text/html, text/plain or a
 * binary type, never an error page.</li>
 * <li><code>text/html</code> is HTML, <code>application/xhtml+xml</code> is XHTML, <code>text/markdown</code> (and the older
 * <code>text/x-markdown</code>) is Markdown, <code>text/plain</code> is plain text.</li>
 * <li>Every other <code>text/*</code> type, JavaScript types, <code>application/json</code> and <code>+json</code> types and
 * <code>application/xml</code>, <code>+xml</code> types (RSS, Atom, ...) are shown as plain text, so their source is readable. Two
 * exceptions: the <code>text/*</code> types that Chromium does not show as text (calendar, vCard, CSV, RTF and similar: the list
 * <code>kUnsupportedTextTypes</code> of <code>third_party/blink/common/mime_util/mime_util.cc</code>, read on 2026-11-16, current
 * development revision of the Chromium repository) go to the download path; and an <code>application/xml</code> or <code>text/xml</code>
 * body whose root element is <code>html</code> in the XHTML namespace is XHTML, as in browsers.</li>
 * <li><code>image/*</code> is an image (<code>image/svg+xml</code> included), <code>audio/*</code> and <code>application/ogg</code> are
 * audio, <code>video/*</code> is video (shown as an unsupported-video placeholder).</li>
 * <li>Everything else (<code>application/octet-stream</code>, PDF, archives, fonts, <code>multipart/*</code>, ...) is a download.</li>
 * </ol>
 * <b>Policy for setContent</b> ({@link #classifySetContent(String)}): the MIME argument must be one of <code>text/html</code>,
 * <code>text/plain</code>, <code>text/markdown</code>, <code>application/xhtml+xml</code>; parameters are removed and the type is
 * compared ignoring case; null, empty and every other type is an <code>IllegalArgumentException</code> (not a download).
 * <p>
 * Not covered: the "text or binary" check for servers that send binary data as <code>text/plain</code>, and the
 * <code>X-Content-Type-Options: nosniff</code> header.
 * <p>
 * 본문을 어느 처리기가 보여 줄지 정한다(계획 P17-01, P01-17, P01-18). 결정 표 하나가 네트워크(<code>Content-Type</code> 과 경우에 따라
 * <code>Content-Disposition: attachment</code> 가 있는 응답)와 <code>setContent</code>(MIME 인수)를 함께 맡으므로 두 경로가 같은 콘텐츠 처리를 쓴다.
 * <p>
 * <b>응답 정책</b>({@link #classifyResponse(String, boolean, byte[])}):
 * <ol>
 * <li><code>Content-Disposition: attachment</code> 는 언제나 다운로드이다.</li>
 * <li>없거나 잘못된 <code>Content-Type</code> 은 오류가 아니다: 첫 바이트를 살펴 타입을 알아낸다({@link #sniff(byte[])}. WHATWG MIME Sniffing 표준의
 * "unknown MIME type" 알고리즘을 줄인 것: HTML 태그, <code>&lt;?xml</code>, PDF, 이미지/오디오/비디오 시그니처, 압축 파일, 그 뒤 "텍스트인지 바이너리인지").
 * 결과는 text/html, text/plain 또는 바이너리 타입이며 오류 페이지가 아니다.</li>
 * <li><code>text/html</code> 은 HTML, <code>application/xhtml+xml</code> 은 XHTML, <code>text/markdown</code>(과 옛 <code>text/x-markdown</code>)은
 * Markdown, <code>text/plain</code> 은 일반 텍스트이다.</li>
 * <li>그 밖의 <code>text/*</code> 타입, JavaScript 타입, <code>application/json</code> 과 <code>+json</code> 타입, <code>application/xml</code> 과
 * <code>+xml</code> 타입(RSS, Atom 등)은 소스를 읽을 수 있도록 일반 텍스트로 보인다. 예외 둘: Chromium 이 텍스트로 보이지 않는 <code>text/*</code>
 * 타입(캘린더, vCard, CSV, RTF 등. <code>third_party/blink/common/mime_util/mime_util.cc</code> 의 <code>kUnsupportedTextTypes</code> 목록, 2026-11-16 에
 * Chromium 저장소의 당시 개발 판에서 확인)은 다운로드 경로로 가고, 루트 요소가 XHTML 네임스페이스의 <code>html</code> 인 <code>application/xml</code>,
 * <code>text/xml</code> 본문은 브라우저처럼 XHTML 이다.</li>
 * <li><code>image/*</code> 는 이미지(<code>image/svg+xml</code> 포함), <code>audio/*</code> 와 <code>application/ogg</code> 는 오디오, <code>video/*</code> 는
 * 비디오(미지원 비디오 대체 표시)이다.</li>
 * <li>그 밖의 모든 것(<code>application/octet-stream</code>, PDF, 압축 파일, 글꼴, <code>multipart/*</code> 등)은 다운로드이다.</li>
 * </ol>
 * <b>setContent 정책</b>({@link #classifySetContent(String)}): MIME 인수는 <code>text/html</code>, <code>text/plain</code>, <code>text/markdown</code>,
 * <code>application/xhtml+xml</code> 중 하나여야 한다. 매개변수는 제거하고 대소문자를 무시하여 비교한다. null, 빈 값, 그 밖의 모든 타입은
 * <code>IllegalArgumentException</code> 이다(다운로드가 아니다).
 * <p>
 * 다루지 않는 것: 서버가 바이너리를 <code>text/plain</code> 으로 보내는 경우의 "텍스트인지 바이너리인지" 검사, <code>X-Content-Type-Options: nosniff</code> 헤더.
 */
public final class ContentClassifier {

    /**
     * The number of bytes that the sniffing algorithm looks at (the resource header length of the MIME Sniffing Standard).
     * <p>
     * 판별 알고리즘이 살펴보는 바이트 수(MIME Sniffing 표준의 resource header 길이).
     */
    public static final int SNIFF_LENGTH = 1445;

    /**
     * The media type for binary data.
     * <p>
     * 바이너리 데이터의 미디어 타입.
     */
    public static final String OCTET_STREAM = "application/octet-stream";

    /**
     * The types that <code>setContent</code> accepts.
     * <p>
     * <code>setContent</code> 가 받는 타입.
     */
    private static final String[] SET_CONTENT_TYPES = {"text/html", "text/plain", "text/markdown", "application/xhtml+xml"};

    /**
     * <code>text/*</code> types that are not shown as text (copied from Chromium's <code>kUnsupportedTextTypes</code>).
     * <p>
     * 텍스트로 보이지 않는 <code>text/*</code> 타입(Chromium 의 <code>kUnsupportedTextTypes</code> 에서 옮김).
     */
    private static final Set<String> UNSUPPORTED_TEXT = new HashSet<String>(Arrays.asList("text/calendar", "text/x-calendar",
            "text/x-vcalendar", "text/vcalendar", "text/vcard", "text/x-vcard", "text/directory", "text/ldif", "text/qif", "text/x-qif",
            "text/x-csv", "text/x-vcf", "text/rtf", "text/comma-separated-values", "text/csv", "text/tab-separated-values", "text/tsv",
            "text/ofx", "text/vnd.sun.j2me.app-descriptor", "text/x-ms-iqy", "text/x-ms-odc", "text/x-ms-rqy", "text/x-ms-contact"));

    /**
     * Non-text types that are shown as text (JavaScript, JSON, XML families).
     * <p>
     * 텍스트로 보이는 비텍스트 타입(JavaScript, JSON, XML 계열).
     */
    private static final Set<String> SHOWN_AS_TEXT = new HashSet<String>(Arrays.asList("application/javascript", "application/x-javascript",
            "application/ecmascript", "application/x-ecmascript", "application/json", "application/xml", "application/atom+xml",
            "application/rss+xml"));

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ContentClassifier() {
    }

    /**
     * Looks at a media type alone. A missing or malformed type gives {@link ContentKind#UNKNOWN}; there is no sniffing, no
     * XHTML detection for XML types and no attachment handling here (see {@link #classifyResponse(String, boolean, byte[])}).
     * <p>
     * 미디어 타입만 보고 판정한다. 없거나 잘못된 타입은 {@link ContentKind#UNKNOWN} 이다. 내용 판별, XML 타입의 XHTML 검출, 첨부 처리는 여기에 없다
     * ({@link #classifyResponse(String, boolean, byte[])} 참조).
     *
     * @param mime the media type in any letter case, parameters are not allowed; null or "" means missing / 미디어 타입(대소문자 무관, 매개변수 불가).
     *             null 이나 ""은 없음
     * @return the kind / 종류
     */
    public static ContentKind kindOfMime(String mime) {
        if (mime == null) {
            return ContentKind.UNKNOWN;
        }
        String m = mime.trim().toLowerCase();
        if (m.length() == 0 || m.indexOf('/') <= 0) {
            return ContentKind.UNKNOWN;
        }
        if ("text/html".equals(m)) {
            return ContentKind.HTML;
        }
        if ("application/xhtml+xml".equals(m)) {
            return ContentKind.XHTML;
        }
        if ("text/markdown".equals(m) || "text/x-markdown".equals(m)) {
            return ContentKind.MARKDOWN;
        }
        if ("text/plain".equals(m)) {
            return ContentKind.PLAIN_TEXT;
        }
        if (m.startsWith("text/")) {
            return UNSUPPORTED_TEXT.contains(m) ? ContentKind.DOWNLOAD : ContentKind.PLAIN_TEXT;
        }
        if (m.startsWith("image/")) {
            return ContentKind.IMAGE;
        }
        if (m.startsWith("audio/") || "application/ogg".equals(m)) {
            return ContentKind.AUDIO;
        }
        if (m.startsWith("video/")) {
            return ContentKind.VIDEO;
        }
        if (SHOWN_AS_TEXT.contains(m) || m.startsWith("application/") && (m.endsWith("+json") || m.endsWith("+xml"))) {
            return ContentKind.PLAIN_TEXT;
        }
        return ContentKind.DOWNLOAD;
    }

    /**
     * Decides how to show a response (see the class description for the policy).
     * <p>
     * 응답을 어떻게 보여 줄지 정한다(정책은 클래스 설명 참조).
     *
     * @param contentTypeHeader the <code>Content-Type</code> header value or null / <code>Content-Type</code> 헤더 값 또는 null
     * @param attachment        true if the response has <code>Content-Disposition: attachment</code> / 응답에 <code>Content-Disposition: attachment</code> 가 있으면 true
     * @param head              the first bytes of the body (at least {@link #SNIFF_LENGTH} if available; more helps to recognize an XHTML root
     *                          element behind a long DOCTYPE); null is treated as empty / 본문의 첫 바이트(가능하면 {@link #SNIFF_LENGTH} 이상. 더 많으면
     *                          긴 DOCTYPE 뒤의 XHTML 루트 요소를 알아보는 데 도움이 된다). null 은 빈 배열
     * @return the decision, never null / 결정(null 아님)
     */
    public static ContentDecision classifyResponse(String contentTypeHeader, boolean attachment, byte[] head) {
        return classifyResponse(ContentType.parse(contentTypeHeader), attachment, head);
    }

    /**
     * Decides how to show a response whose header has been parsed already.
     * <p>
     * 헤더를 이미 해석한 응답을 어떻게 보여 줄지 정한다.
     *
     * @param type       the parsed <code>Content-Type</code>, not null / 해석한 <code>Content-Type</code>(null 불가)
     * @param attachment true for an attachment / 첨부이면 true
     * @param head       the first bytes of the body or null / 본문의 첫 바이트 또는 null
     * @return the decision, never null / 결정(null 아님)
     */
    public static ContentDecision classifyResponse(ContentType type, boolean attachment, byte[] head) {
        String mime = type.getMime();
        String charset = type.getCharset();
        boolean sniffed = false;
        if (mime.length() == 0) {
            mime = sniff(head);
            sniffed = true;
        }
        if (attachment) {
            return new ContentDecision(ContentKind.DOWNLOAD, mime, charset, sniffed, ContentDecision.Reason.ATTACHMENT);
        }
        ContentKind kind = kindOfMime(mime);
        // 루트 요소가 XHTML 네임스페이스의 html 인 XML 문서는 XHTML 로 본다.
        if (kind == ContentKind.PLAIN_TEXT && ("application/xml".equals(mime) || "text/xml".equals(mime)) && isXhtmlRoot(head)) {
            kind = ContentKind.XHTML;
        }
        ContentDecision.Reason reason = kind == ContentKind.DOWNLOAD ? ContentDecision.Reason.NOT_DISPLAYABLE : ContentDecision.Reason.NONE;
        return new ContentDecision(kind, mime, charset, sniffed, reason);
    }

    /**
     * Tells whether the first bytes of an XML document show an <code>html</code> root element in the XHTML namespace.
     * <p>
     * XML 문서의 첫 바이트가 XHTML 네임스페이스의 <code>html</code> 루트 요소를 보여 주는지 알려 준다.
     *
     * @param head the first bytes or null / 첫 바이트 또는 null
     * @return true for an XHTML root element / XHTML 루트 요소이면 true
     */
    private static boolean isXhtmlRoot(byte[] head) {
        if (head == null || head.length == 0) {
            return false;
        }
        return XmlProlog.scan(XmlDecoder.decode(head, null).getText()).isXhtmlRoot();
    }

    /**
     * Checks the MIME argument of <code>setContent</code> and returns its kind.
     * <p>
     * <code>setContent</code> 의 MIME 인수를 검사하고 종류를 반환한다.
     *
     * @param mime the argument, for example <code>"Text/HTML; charset=utf-8"</code> / 인수. 예: <code>"Text/HTML; charset=utf-8"</code>
     * @return {@link ContentKind#HTML}, {@link ContentKind#PLAIN_TEXT}, {@link ContentKind#MARKDOWN} or {@link ContentKind#XHTML} /
     *         네 가지 중 하나
     * @throws IllegalArgumentException if the argument is null, empty or not one of the four supported types / 인수가 null, 빈 값이거나
     *                                  지원하는 네 타입이 아니면
     */
    public static ContentKind classifySetContent(String mime) {
        return kindOfMime(requireSetContentMime(mime));
    }

    /**
     * Checks the MIME argument of <code>setContent</code> and returns the normalized type.
     * <p>
     * <code>setContent</code> 의 MIME 인수를 검사하고 정규화한 타입을 반환한다.
     *
     * @param mime the argument / 인수
     * @return one of <code>text/html</code>, <code>text/plain</code>, <code>text/markdown</code>, <code>application/xhtml+xml</code> / 네 가지 중 하나
     * @throws IllegalArgumentException if the argument is null, empty or not one of the four supported types / 인수가 null, 빈 값이거나
     *                                  지원하는 네 타입이 아니면
     */
    public static String requireSetContentMime(String mime) {
        if (mime == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }
        String m = ContentType.mimeOf(mime);
        if (m.length() == 0) {
            throw new IllegalArgumentException("contentType is empty or malformed: " + mime);
        }
        // 지원하는 네 타입 중 같은 것을 찾는다.
        for (int i = 0; i < SET_CONTENT_TYPES.length; i++) {
            if (SET_CONTENT_TYPES[i].equals(m)) {
                return m;
            }
        }
        throw new IllegalArgumentException("unsupported contentType: " + m + " (supported: text/html, text/plain, text/markdown, application/xhtml+xml)");
    }

    /**
     * Tells whether <code>setContent</code> accepts a MIME argument.
     * <p>
     * <code>setContent</code> 가 MIME 인수를 받는지 알려 준다.
     *
     * @param mime the argument or null / 인수 또는 null
     * @return true if it is one of the four supported types / 지원하는 네 타입 중 하나이면 true
     */
    public static boolean isSetContentMime(String mime) {
        try {
            requireSetContentMime(mime);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Finds the media type of a body that came without a usable <code>Content-Type</code> ("identifying a resource with an unknown MIME
     * type" of the WHATWG MIME Sniffing Standard, reduced). The steps, in order, on at most {@link #SNIFF_LENGTH} bytes: an HTML tag
     * after optional white space (<code>&lt;!DOCTYPE HTML</code>, <code>&lt;html</code>, <code>&lt;head</code>, <code>&lt;script</code>,
     * <code>&lt;iframe</code>, <code>&lt;h1</code>, <code>&lt;div</code>, <code>&lt;font</code>, <code>&lt;table</code>, <code>&lt;a</code>,
     * <code>&lt;style</code>, <code>&lt;title</code>, <code>&lt;b</code>, <code>&lt;body</code>, <code>&lt;br</code>, <code>&lt;p</code> or
     * <code>&lt;!--</code>, each followed by a space or <code>&gt;</code>, ignoring case) is <code>text/html</code>; <code>&lt;?xml</code>
     * is <code>text/xml</code>; <code>%PDF-</code> is PDF; a byte order mark means <code>text/plain</code>; then the signatures of GIF, PNG,
     * JPEG, BMP, WebP, ICO and AVIF images, of WAVE, AIFF, AU, MIDI, MP3 (ID3), Ogg, FLAC and MP4/M4A/WebM media and of gzip, ZIP and RAR
     * archives; finally a body that contains binary control bytes is <code>application/octet-stream</code> and everything else is
     * <code>text/plain</code>.
     * <p>
     * 쓸 만한 <code>Content-Type</code> 없이 온 본문의 미디어 타입을 찾는다(WHATWG MIME Sniffing 표준의 "identifying a resource with an unknown MIME
     * type"을 줄인 것). 최대 {@link #SNIFF_LENGTH} 바이트에 대해 순서대로: 공백이 있을 수 있는 앞부분 뒤의 HTML 태그(영문 설명의 목록. 모두 뒤에 공백 또는
     * <code>&gt;</code> 가 오며 대소문자 무시)는 <code>text/html</code>, <code>&lt;?xml</code> 은 <code>text/xml</code>, <code>%PDF-</code> 는 PDF, 바이트 순서 표시는
     * <code>text/plain</code>, 이어서 GIF, PNG, JPEG, BMP, WebP, ICO, AVIF 이미지와 WAVE, AIFF, AU, MIDI, MP3(ID3), Ogg, FLAC, MP4/M4A/WebM 미디어와 gzip, ZIP, RAR
     * 압축 파일의 시그니처, 마지막으로 바이너리 제어 바이트가 있으면 <code>application/octet-stream</code>, 나머지는 <code>text/plain</code> 이다.
     *
     * @param head the first bytes of the body or null (treated as empty, which gives <code>text/plain</code>) / 본문의 첫 바이트. null 은 빈 배열로 보며
     *             <code>text/plain</code> 이 된다
     * @return the media type in lower case / 소문자 미디어 타입
     */
    public static String sniff(byte[] head) {
        byte[] b = head == null ? new byte[0] : head;
        int n = Math.min(b.length, SNIFF_LENGTH);
        int i = n >= 3 && u(b[0]) == 0xEF && u(b[1]) == 0xBB && u(b[2]) == 0xBF ? 3 : 0;
        // 앞쪽의 UTF-8 바이트 순서 표시와 공백 바이트(탭, LF, FF, CR, 스페이스)는 HTML 태그 판별에서 건너뛴다.
        while (i < n && (b[i] == 0x09 || b[i] == 0x0A || b[i] == 0x0C || b[i] == 0x0D || b[i] == 0x20)) {
            i++;
        }
        if (looksLikeHtml(b, i, n)) {
            return "text/html";
        }
        if (matches(b, i, n, "<?xml")) {
            return "text/xml";
        }
        if (matches(b, 0, n, "%PDF-")) {
            return "application/pdf";
        }
        if (matches(b, 0, n, "%!PS-Adobe-")) {
            return "application/postscript";
        }
        if (n >= 2 && (u(b[0]) == 0xFE && u(b[1]) == 0xFF || u(b[0]) == 0xFF && u(b[1]) == 0xFE) || n >= 3 && u(b[0]) == 0xEF && u(b[1]) == 0xBB && u(b[2]) == 0xBF) {
            return "text/plain";
        }
        String signature = signatureType(b, n);
        if (signature != null) {
            return signature;
        }
        // 바이너리 제어 바이트(00-08, 0B, 0E-1A, 1C-1F)가 하나라도 있으면 바이너리 데이터이다.
        for (int k = 0; k < n; k++) {
            int c = u(b[k]);
            if (c <= 0x08 || c == 0x0B || c >= 0x0E && c <= 0x1A || c >= 0x1C && c <= 0x1F) {
                return OCTET_STREAM;
            }
        }
        return "text/plain";
    }

    /**
     * Converts a byte to an unsigned value.
     * <p>
     * 바이트를 부호 없는 값으로 바꾼다.
     *
     * @param b the byte / 바이트
     * @return 0 to 255 / 0~255
     */
    private static int u(byte b) {
        return b & 0xFF;
    }

    /**
     * Tells whether the bytes at a position start with an ASCII string (case sensitive).
     * <p>
     * 위치의 바이트가 ASCII 문자열로 시작하는지 알려 준다(대소문자 구분).
     *
     * @param b   bytes / 바이트
     * @param pos position / 위치
     * @param n   number of usable bytes / 쓸 수 있는 바이트 수
     * @param s   the ASCII string / ASCII 문자열
     * @return true if equal / 같으면 true
     */
    private static boolean matches(byte[] b, int pos, int n, String s) {
        if (pos + s.length() > n) {
            return false;
        }
        // 글자를 하나씩 비교한다.
        for (int i = 0; i < s.length(); i++) {
            if (u(b[pos + i]) != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the bytes at a position start with an ASCII string, ignoring ASCII case.
     * <p>
     * 위치의 바이트가 ASCII 문자열로 시작하는지 ASCII 대소문자를 무시하고 알려 준다.
     *
     * @param b   bytes / 바이트
     * @param pos position / 위치
     * @param n   number of usable bytes / 쓸 수 있는 바이트 수
     * @param s   the lower-case ASCII string / 소문자 ASCII 문자열
     * @return true if equal / 같으면 true
     */
    private static boolean matchesIgnoreCase(byte[] b, int pos, int n, String s) {
        if (pos + s.length() > n) {
            return false;
        }
        // 글자를 하나씩 소문자로 바꿔 비교한다.
        for (int i = 0; i < s.length(); i++) {
            int c = u(b[pos + i]);
            if (c >= 'A' && c <= 'Z') {
                c += 32;
            }
            if (c != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tells whether the bytes at a position start with one of the HTML tag patterns of the MIME Sniffing Standard.
     * <p>
     * 위치의 바이트가 MIME Sniffing 표준의 HTML 태그 패턴 중 하나로 시작하는지 알려 준다.
     *
     * @param b   bytes / 바이트
     * @param pos position after the leading white space / 앞쪽 공백 뒤의 위치
     * @param n   number of usable bytes / 쓸 수 있는 바이트 수
     * @return true for HTML / HTML 이면 true
     */
    private static boolean looksLikeHtml(byte[] b, int pos, int n) {
        String[] tags = {"<!doctype html", "<html", "<head", "<script", "<iframe", "<h1", "<div", "<font", "<table", "<a", "<style",
            "<title", "<b", "<body", "<br", "<p", "<!--"};
        // 태그 패턴마다 일치하고 바로 뒤가 공백 또는 '>' 인지 확인한다.
        for (int i = 0; i < tags.length; i++) {
            int end = pos + tags[i].length();
            if (matchesIgnoreCase(b, pos, n, tags[i]) && end < n && (b[end] == 0x20 || b[end] == 0x3E)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for the signature of an image, audio, video or archive format.
     * <p>
     * 이미지, 오디오, 비디오, 압축 파일 형식의 시그니처를 찾는다.
     *
     * @param b bytes / 바이트
     * @param n number of usable bytes / 쓸 수 있는 바이트 수
     * @return the media type or null / 미디어 타입. 없으면 null
     */
    private static String signatureType(byte[] b, int n) {
        if (n >= 4 && u(b[0]) == 0x00 && u(b[1]) == 0x00 && u(b[2]) == 0x01 && u(b[3]) == 0x00) {
            return "image/x-icon";
        }
        if (matches(b, 0, n, "GIF87a") || matches(b, 0, n, "GIF89a")) {
            return "image/gif";
        }
        if (n >= 8 && u(b[0]) == 0x89 && matches(b, 1, n, "PNG") && u(b[4]) == 0x0D && u(b[5]) == 0x0A && u(b[6]) == 0x1A && u(b[7]) == 0x0A) {
            return "image/png";
        }
        if (n >= 3 && u(b[0]) == 0xFF && u(b[1]) == 0xD8 && u(b[2]) == 0xFF) {
            return "image/jpeg";
        }
        if (matches(b, 0, n, "BM")) {
            return "image/bmp";
        }
        if (matches(b, 0, n, "RIFF") && n >= 12) {
            if (matches(b, 8, n, "WEBPVP")) {
                return "image/webp";
            }
            if (matches(b, 8, n, "WAVE")) {
                return "audio/wave";
            }
            if (matches(b, 8, n, "AVI ")) {
                return "video/avi";
            }
        }
        if (matches(b, 0, n, "FORM") && matches(b, 8, n, "AIFF")) {
            return "audio/aiff";
        }
        if (matches(b, 0, n, ".snd")) {
            return "audio/basic";
        }
        if (matches(b, 0, n, "ID3")) {
            return "audio/mpeg";
        }
        if (matches(b, 0, n, "OggS")) {
            return "application/ogg";
        }
        if (matches(b, 0, n, "fLaC")) {
            return "audio/flac";
        }
        if (matches(b, 0, n, "MThd")) {
            return "audio/midi";
        }
        if (n >= 4 && u(b[0]) == 0x1A && u(b[1]) == 0x45 && u(b[2]) == 0xDF && u(b[3]) == 0xA3) {
            return "video/webm";
        }
        if (n >= 12 && matches(b, 4, n, "ftyp")) {
            if (matches(b, 8, n, "avif") || matches(b, 8, n, "avis")) {
                return "image/avif";
            }
            return matches(b, 8, n, "M4A ") ? "audio/mp4" : "video/mp4";
        }
        if (n >= 3 && u(b[0]) == 0x1F && u(b[1]) == 0x8B && u(b[2]) == 0x08) {
            return "application/x-gzip";
        }
        if (matches(b, 0, n, "PK") && n >= 4 && u(b[2]) == 0x03 && u(b[3]) == 0x04) {
            return "application/zip";
        }
        if (matches(b, 0, n, "Rar!") && n >= 7 && u(b[4]) == 0x1A && u(b[5]) == 0x07 && u(b[6]) == 0x00) {
            return "application/x-rar-compressed";
        }
        return null;
    }
}
