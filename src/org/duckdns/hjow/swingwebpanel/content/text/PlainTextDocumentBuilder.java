package org.duckdns.hjow.swingwebpanel.content.text;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;

/**
 * Builds the DOM document of a plain text page (plan item P17-02). The text is never interpreted: whatever it contains (tags, script
 * elements, character references, an XML declaration) is the <em>data</em> of one text node, so it can neither run a script nor change
 * the page. The shape of the document follows the HTML Standard's rule for text files and what browsers build:
 * <pre>
 * html &gt; head
 *      &gt; body &gt; pre style="word-wrap: break-word; white-space: pre-wrap;" &gt; (text)
 * </pre>
 * The line structure of the text is kept: CR LF and CR become LF (the input stream preprocessing that the HTML Standard applies to text
 * files as well), NUL becomes U+FFFD (the PLAINTEXT tokenizer state of the standard), and nothing else is touched, so leading white space,
 * blank lines and a trailing newline stay as they are. <code>white-space: pre-wrap</code> shows them as written and wraps lines that
 * are longer than the window (as browsers do for <code>text/plain</code>); the page has no title.
 * <p>
 * The builder is used for <code>text/plain</code> and for the other text types that are shown as text (CSS, JavaScript, JSON, XML source).
 * It does not change the received source: <code>getContent()</code> keeps returning the original string.
 * <p>
 * 일반 텍스트 페이지의 DOM 문서를 만든다(계획 P17-02). 텍스트는 절대 해석되지 않는다: 무엇이 들어 있든(태그, script 요소, 문자 참조, XML 선언) 텍스트 노드
 * 하나의 <em>데이터</em>이므로 스크립트를 실행하거나 페이지를 바꿀 수 없다. 문서의 모양은 HTML 표준의 텍스트 파일 규칙과 브라우저가 만드는 것을 따른다
 * (위의 영문 트리 참조). 텍스트의 줄 구조는 유지한다: CR LF 와 CR 은 LF 가 되고(HTML 표준이 텍스트 파일에도 적용하는 입력 스트림 전처리), NUL 은
 * U+FFFD 가 되며(표준의 PLAINTEXT 토큰화 상태), 그 밖에는 손대지 않으므로 앞쪽 공백, 빈 줄, 끝의 줄 바꿈이 그대로 남는다.
 * <code>white-space: pre-wrap</code> 이 이를 쓴 그대로 보여 주며 창보다 긴 줄은 줄바꿈한다(브라우저가 <code>text/plain</code> 에 하는 것과 같다).
 * 페이지에는 제목이 없다.
 * <p>
 * 이 생성기는 <code>text/plain</code> 과 텍스트로 보여 주는 다른 타입(CSS, JavaScript, JSON, XML 소스)에 쓴다. 수신 원문은 바꾸지 않으며
 * <code>getContent()</code> 는 계속 원래 문자열을 반환한다.
 */
public final class PlainTextDocumentBuilder {

    /**
     * The inline style of the <code>pre</code> element.
     * <p>
     * <code>pre</code> 요소의 인라인 스타일.
     */
    public static final String PRE_STYLE = "word-wrap: break-word; white-space: pre-wrap;";

    /**
     * The replacement character that stands for NUL.
     * <p>
     * NUL 을 대신하는 대체 문자.
     */
    private static final char REPLACEMENT = (char) 0xFFFD;

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private PlainTextDocumentBuilder() {
    }

    /**
     * Builds a document for a text (content type <code>text/plain</code>, no URL).
     * <p>
     * 텍스트의 문서를 만든다(콘텐츠 타입 <code>text/plain</code>, URL 없음).
     *
     * @param text the text, null is treated as empty / 텍스트(null 은 빈 문자열)
     * @return the document / 문서
     */
    public static Document build(String text) {
        return build(text, null, "text/plain");
    }

    /**
     * Builds a document for a text (content type <code>text/plain</code>).
     * <p>
     * 텍스트의 문서를 만든다(콘텐츠 타입 <code>text/plain</code>).
     *
     * @param text the text, null is treated as empty / 텍스트(null 은 빈 문자열)
     * @param url  the document URL or null / 문서 URL 또는 null
     * @return the document / 문서
     */
    public static Document build(String text, String url) {
        return build(text, url, "text/plain");
    }

    /**
     * Builds a document for a text.
     * <p>
     * 텍스트의 문서를 만든다.
     *
     * @param text        the text, null is treated as empty / 텍스트(null 은 빈 문자열)
     * @param url         the document URL or null / 문서 URL 또는 null
     * @param contentType the content type reported by <code>document.contentType</code>, for example <code>text/plain</code> or
     *                    <code>text/css</code>; null means <code>text/plain</code> / <code>document.contentType</code> 이 보고할 콘텐츠 타입.
     *                    예: <code>text/plain</code>, <code>text/css</code>. null 이면 <code>text/plain</code>
     * @return the document / 문서
     */
    public static Document build(String text, String url, String contentType) {
        Document doc = new Document(true, contentType == null || contentType.length() == 0 ? "text/plain" : contentType);
        if (url != null) {
            doc.setUrl(url);
        }
        populate(doc, text);
        return doc;
    }

    /**
     * Fills an empty document that the caller has prepared (for example with a selector engine installed) with the plain text
     * structure.
     * <p>
     * 호출자가 준비한(예: 선택자 엔진이 설치된) 빈 문서를 일반 텍스트 구조로 채운다.
     *
     * @param doc  the empty HTML document / 빈 HTML 문서
     * @param text the text, null is treated as empty / 텍스트(null 은 빈 문자열)
     * @throws IllegalArgumentException if the document is null or already has children / 문서가 null 이거나 이미 자식이 있으면
     */
    public static void populate(Document doc, String text) {
        if (doc == null || doc.getFirstChild() != null) {
            throw new IllegalArgumentException("an empty document is required");
        }
        Element html = doc.createElement("html");
        Element head = doc.createElement("head");
        Element body = doc.createElement("body");
        Element pre = doc.createElement("pre");
        pre.setAttribute("style", PRE_STYLE);
        pre.appendChild(doc.createTextNode(normalize(text)));
        body.appendChild(pre);
        html.appendChild(head);
        html.appendChild(body);
        doc.appendChild(html);
        doc.setCompatibilityMode(Document.NO_QUIRKS_MODE);
    }

    /**
     * Normalizes a text the way the HTML Standard does for the input stream of a text file: CR LF and CR become LF and NUL becomes
     * U+FFFD. The input string is not modified; if there is nothing to change the same string is returned.
     * <p>
     * HTML 표준이 텍스트 파일의 입력 스트림에 하는 대로 텍스트를 정규화한다: CR LF 와 CR 은 LF, NUL 은 U+FFFD 가 된다. 입력 문자열은 바꾸지 않으며
     * 바꿀 것이 없으면 같은 문자열을 반환한다.
     *
     * @param text the text, null is treated as empty / 텍스트(null 은 빈 문자열)
     * @return the normalized text / 정규화한 텍스트
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        int n = text.length();
        int i = 0;
        // 바꿔야 할 문자(CR, NUL)가 처음 나오는 곳까지 건너뛴다.
        while (i < n && text.charAt(i) != '\r' && text.charAt(i) != 0) {
            i++;
        }
        if (i == n) {
            return text;
        }
        StringBuilder sb = new StringBuilder(n);
        sb.append(text, 0, i);
        // 나머지를 훑으며 CR LF 와 CR 을 LF 로, NUL 을 U+FFFD 로 바꾼다.
        for (; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                sb.append('\n');
                if (i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else if (c == 0) {
                sb.append(REPLACEMENT);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
