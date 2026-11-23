package org.duckdns.hjow.swingwebpanel.content.markdown;

/**
 * Converts Markdown text to HTML (plan items P17-03, P17-04). The converter follows CommonMark 0.31.2 and, unless switched off in the
 * {@link MarkdownOptions}, the GitHub Flavored Markdown extensions (tables, strikethrough, task lists, extended autolinks). By default raw
 * HTML in the Markdown is escaped and shown as text and links with the schemes <code>javascript:</code>, <code>vbscript:</code>,
 * <code>data:</code> and <code>file:</code> lose their destination. The conversion never changes its input; the browser component keeps
 * returning the original Markdown from <code>getContent()</code> and uses this class only to build the page that is displayed.
 * <p>
 * Usage: <code>String html = MarkdownConverter.toHtml("# Title\n\ntext");</code> for a fragment or
 * <code>MarkdownConverter.toHtmlDocument(markdown, "Title")</code> for a complete HTML5 page.
 * <p>
 * All methods are thread-safe (no shared state). Input longer than {@link MarkdownOptions#getMaxInputLength()} throws
 * {@link MarkdownLimitException}; deeply nested blocks and inline elements are limited without failing (see the options).
 * <p>
 * Markdown 텍스트를 HTML 로 변환한다(계획 P17-03, P17-04). 변환기는 CommonMark 0.31.2 를 따르며 {@link MarkdownOptions} 에서 끄지 않으면
 * GitHub Flavored Markdown 확장(표, 취소선, 작업 목록, 확장 자동 링크)도 처리한다. 기본적으로 Markdown 안의 원시 HTML 은 이스케이프하여 텍스트로
 * 보여 주고 <code>javascript:</code>, <code>vbscript:</code>, <code>data:</code>, <code>file:</code> 스킴의 링크는 목적지를 잃는다. 변환은 입력을
 * 바꾸지 않으며, 브라우저 컴포넌트는 <code>getContent()</code> 로 원래 Markdown 을 계속 돌려주고 이 클래스는 화면에 보일 페이지를 만드는 데에만 쓴다.
 * <p>
 * 사용법: 조각은 <code>String html = MarkdownConverter.toHtml("# Title\n\ntext");</code>, 완전한 HTML5 페이지는
 * <code>MarkdownConverter.toHtmlDocument(markdown, "Title")</code>.
 * <p>
 * 모든 메서드는 스레드 안전하다(공유 상태 없음). {@link MarkdownOptions#getMaxInputLength()} 보다 긴 입력은 {@link MarkdownLimitException} 을
 * 던지며, 깊게 중첩된 블록과 인라인 요소는 실패 없이 제한된다(옵션 참조).
 */
public final class MarkdownConverter {

    /** Style of the generated page. 생성되는 페이지의 스타일. */
    private static final String STYLE = "body { font-family: sans-serif; line-height: 1.5; margin: 16px; }\n"
            + "pre { background-color: #f4f4f4; padding: 8px; }\n"
            + "code { background-color: #f4f4f4; }\n"
            + "pre code { background-color: transparent; }\n"
            + "blockquote { border-left: 4px solid #cccccc; margin-left: 0; padding-left: 12px; color: #555555; }\n"
            + "table { border-collapse: collapse; }\n"
            + "th, td { border: 1px solid #cccccc; padding: 4px 8px; }\n"
            + "img { max-width: 100%; }\n"
            + "pre.md-raw-html { color: #8a4b00; }\n";

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private MarkdownConverter() {
    }

    /**
     * Converts Markdown to an HTML fragment with the default options.
     * <p>
     * 기본 옵션으로 Markdown 을 HTML 조각으로 변환한다.
     *
     * @param markdown the Markdown text / Markdown 텍스트
     * @return HTML fragment (block elements, ending with a line feed) / HTML 조각(블록 요소들, 줄 바꿈으로 끝남)
     * @throws IllegalArgumentException if the text is null / 텍스트가 null 이면
     * @throws MarkdownLimitException   if the text is longer than the limit / 텍스트가 제한보다 길면
     */
    public static String toHtml(String markdown) {
        return toHtml(markdown, new MarkdownOptions());
    }

    /**
     * Converts Markdown to an HTML fragment.
     * <p>
     * Markdown 을 HTML 조각으로 변환한다.
     *
     * @param markdown the Markdown text / Markdown 텍스트
     * @param options  options; null means the defaults / 옵션. null 이면 기본값
     * @return HTML fragment / HTML 조각
     * @throws IllegalArgumentException if the text is null / 텍스트가 null 이면
     * @throws MarkdownLimitException   if the text is longer than the limit / 텍스트가 제한보다 길면
     */
    public static String toHtml(String markdown, MarkdownOptions options) {
        MarkdownOptions o = options == null ? new MarkdownOptions() : options.copy();
        Node doc = parse(markdown, o);
        return new HtmlRenderer(o).render(doc);
    }

    /**
     * Converts Markdown to a complete HTML5 page with the default options.
     * <p>
     * 기본 옵션으로 Markdown 을 완전한 HTML5 페이지로 변환한다.
     *
     * @param markdown the Markdown text / Markdown 텍스트
     * @param title    page title; null takes the text of the first heading (or an empty title) / 페이지 제목. null 이면 첫 제목의 텍스트(없으면 빈 제목)
     * @return HTML page / HTML 페이지
     * @throws IllegalArgumentException if the text is null / 텍스트가 null 이면
     * @throws MarkdownLimitException   if the text is longer than the limit / 텍스트가 제한보다 길면
     */
    public static String toHtmlDocument(String markdown, String title) {
        return toHtmlDocument(markdown, title, new MarkdownOptions());
    }

    /**
     * Converts Markdown to a complete HTML5 page: a doctype, a UTF-8 declaration, the title, a small default style and the body.
     * <p>
     * Markdown 을 완전한 HTML5 페이지로 변환한다: 문서 유형, UTF-8 선언, 제목, 작은 기본 스타일, 본문.
     *
     * @param markdown the Markdown text / Markdown 텍스트
     * @param title    page title; null takes the text of the first heading (or an empty title) / 페이지 제목. null 이면 첫 제목의 텍스트(없으면 빈 제목)
     * @param options  options; null means the defaults / 옵션. null 이면 기본값
     * @return HTML page / HTML 페이지
     * @throws IllegalArgumentException if the text is null / 텍스트가 null 이면
     * @throws MarkdownLimitException   if the text is longer than the limit / 텍스트가 제한보다 길면
     */
    public static String toHtmlDocument(String markdown, String title, MarkdownOptions options) {
        MarkdownOptions o = options == null ? new MarkdownOptions() : options.copy();
        Node doc = parse(markdown, o);
        String pageTitle = title != null ? title : firstHeadingText(doc);
        String body = new HtmlRenderer(o).render(doc);
        StringBuilder sb = new StringBuilder(body.length() + 600);
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<title>");
        sb.append(MdText.escapeHtml(pageTitle));
        sb.append("</title>\n<style>\n").append(STYLE).append("</style>\n</head>\n<body>\n");
        sb.append(body);
        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    /**
     * Parses a Markdown text.
     * <p>
     * Markdown 텍스트를 파싱한다.
     *
     * @param markdown the text / 텍스트
     * @param o        private copy of the options / 옵션의 개인 복사본
     * @return the syntax tree / 구문 트리
     */
    private static Node parse(String markdown, MarkdownOptions o) {
        if (markdown == null) {
            throw new IllegalArgumentException("markdown must not be null");
        }
        if (markdown.length() > o.getMaxInputLength()) {
            throw new MarkdownLimitException("Markdown input is longer than " + o.getMaxInputLength() + " characters");
        }
        return new BlockParser(o, new LinkRefs(markdown.length())).parse(markdown);
    }

    /**
     * Returns the plain text of the first heading of a document.
     * <p>
     * 문서의 첫 제목의 평문을 반환한다.
     *
     * @param doc the document / 문서
     * @return text or an empty string / 텍스트 또는 빈 문자열
     */
    private static String firstHeadingText(Node doc) {
        Node n = doc.firstChild;
        // 최상위 블록에서 첫 제목을 찾는다.
        while (n != null && n.type != NodeType.HEADING) {
            n = n.next;
        }
        if (n == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Node cur = n.firstChild;
        // 제목의 자손에서 텍스트와 코드의 글자를 모은다(재귀 없는 전위 순회).
        while (cur != null) {
            if (cur.type == NodeType.TEXT || cur.type == NodeType.CODE) {
                sb.append(cur.literal);
            } else if (cur.type == NodeType.SOFTBREAK || cur.type == NodeType.LINEBREAK) {
                sb.append(' ');
            }
            if (cur.firstChild != null) {
                cur = cur.firstChild;
            } else {
                // 다음 형제가 없으면 부모로 올라간다.
                while (cur != null && cur != n && cur.next == null) {
                    cur = cur.parent;
                }
                if (cur == null || cur == n) {
                    break;
                }
                cur = cur.next;
            }
        }
        // 원시 HTML 태그가 빠진 자리의 공백을 하나로 줄인다.
        String t = sb.toString();
        StringBuilder c = new StringBuilder(t.length());
        boolean space = true;
        // 연속한 공백을 하나로 줄인다.
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (ch == ' ') {
                if (!space) {
                    c.append(' ');
                }
                space = true;
            } else {
                c.append(ch);
                space = false;
            }
        }
        return MdText.trim(c.toString());
    }
}
