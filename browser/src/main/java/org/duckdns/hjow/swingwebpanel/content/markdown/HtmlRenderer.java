package org.duckdns.hjow.swingwebpanel.content.markdown;

/**
 * Renders the Markdown syntax tree as HTML in the style of the CommonMark reference implementation (so the output of the specification
 * examples can be compared literally), plus the GFM tables, strikethrough and task list items. The traversal is iterative, so a deeply
 * nested tree cannot overflow the Java stack. Raw HTML is escaped unless the options allow it, and dangerous URL schemes are neutralized.
 * <p>
 * Markdown 구문 트리를 CommonMark 참조 구현 방식의 HTML 로 변환한다(규격 예제의 출력을 글자 그대로 비교할 수 있다). GFM 의 표, 취소선, 작업 목록
 * 항목도 만든다. 순회는 반복형이므로 깊게 중첩된 트리도 Java 스택을 넘치게 하지 않는다. 원시 HTML 은 옵션이 허용하지 않으면 이스케이프하고
 * 위험한 URL 스킴은 무력화한다.
 */
final class HtmlRenderer {

    /** Tags removed by the GFM tag filter (lower case). GFM 태그 필터가 제거하는 태그(소문자). */
    private static final String[] FILTERED = { "title", "textarea", "style", "xmp", "iframe", "noembed", "noframes", "script", "plaintext" };

    /** Output buffer. 출력 버퍼. */
    private final StringBuilder out = new StringBuilder();

    /** True if the last piece written was exactly one line feed. 마지막에 쓴 조각이 정확히 줄 바꿈 하나였으면 true. */
    private boolean lastNewline = true;

    /** Greater than zero while inside an image description (tags are not written). 이미지 설명 안에서는 0보다 큼(태그를 쓰지 않는다). */
    private int disableTags;

    /** Whether raw HTML is passed through. 원시 HTML 을 그대로 내보낼지 여부. */
    private final boolean allowRawHtml;

    /** Whether the GFM tag filter applies to passed-through HTML. 그대로 내보내는 HTML 에 GFM 태그 필터를 적용할지 여부. */
    private final boolean tagFilter;

    /** Whether unsafe URL schemes are neutralized. 위험한 URL 스킴을 무력화할지 여부. */
    private final boolean neutralize;

    /**
     * Creates a renderer.
     * <p>
     * 렌더러를 만든다.
     *
     * @param opts options / 옵션
     */
    HtmlRenderer(MarkdownOptions opts) {
        this.allowRawHtml = opts.isAllowRawHtml();
        this.tagFilter = opts.isGfm();
        this.neutralize = opts.isNeutralizeUnsafeUrls();
    }

    /**
     * Renders a tree.
     * <p>
     * 트리를 변환한다.
     *
     * @param root the document node / 문서 노드
     * @return the HTML fragment / HTML 조각
     */
    String render(Node root) {
        Node cur = root;
        boolean entering = true;
        // 노드를 진입/종료 이벤트 순서로 방문한다(재귀 없는 순회).
        while (cur != null) {
            visit(cur, entering);
            if (entering && cur.isContainer()) {
                if (cur.firstChild != null) {
                    cur = cur.firstChild;
                } else {
                    entering = false;
                }
            } else if (cur == root) {
                break;
            } else if (cur.next != null) {
                cur = cur.next;
                entering = true;
            } else {
                cur = cur.parent;
                entering = false;
            }
        }
        return out.toString();
    }

    /**
     * Writes text as it is.
     * <p>
     * 텍스트를 그대로 쓴다.
     *
     * @param s the text / 텍스트
     */
    private void lit(String s) {
        out.append(s);
        lastNewline = s.length() == 1 && s.charAt(0) == '\n';
    }

    /**
     * Writes escaped text.
     * <p>
     * 이스케이프한 텍스트를 쓴다.
     *
     * @param s the text / 텍스트
     */
    private void text(String s) {
        lit(MdText.escapeHtml(s));
    }

    /**
     * Starts a new line unless the output already ends with a line feed written on its own.
     * <p>
     * 출력이 이미 단독으로 쓴 줄 바꿈으로 끝나지 않았으면 새 줄을 시작한다.
     */
    private void cr() {
        if (!lastNewline) {
            lit("\n");
        }
    }

    /**
     * Writes a tag without attributes (nothing while tags are disabled).
     * <p>
     * 속성 없는 태그를 쓴다(태그가 비활성인 동안에는 쓰지 않는다).
     *
     * @param s the tag text, for example <code>&lt;em&gt;</code> / 태그 문자열(예: <code>&lt;em&gt;</code>)
     */
    private void tag(String s) {
        if (disableTags == 0) {
            out.append(s);
            lastNewline = false;
        }
    }

    /**
     * Handles one tree event.
     * <p>
     * 트리 이벤트 하나를 처리한다.
     *
     * @param node     the node / 노드
     * @param entering true when entering, false when leaving / 진입이면 true, 종료이면 false
     */
    private void visit(Node node, boolean entering) {
        switch (node.type) {
            case DOCUMENT:
                break;
            case TEXT:
                text(node.literal);
                break;
            case SOFTBREAK:
                lit("\n");
                break;
            case LINEBREAK:
                tag("<br />");
                cr();
                break;
            case CODE:
                tag("<code>");
                text(node.literal);
                tag("</code>");
                break;
            case HTML_INLINE:
                if (allowRawHtml && disableTags == 0) {
                    lit(filter(node.literal));
                } else {
                    text(node.literal);
                }
                break;
            case EMPH:
                tag(entering ? "<em>" : "</em>");
                break;
            case STRONG:
                tag(entering ? "<strong>" : "</strong>");
                break;
            case DEL:
                tag(entering ? "<del>" : "</del>");
                break;
            case LINK:
                visitLink(node, entering);
                break;
            case IMAGE:
                visitImage(node, entering);
                break;
            case PARAGRAPH:
                visitParagraph(node, entering);
                break;
            case HEADING:
                if (entering) {
                    cr();
                    tag("<h" + node.level + ">");
                } else {
                    tag("</h" + node.level + ">");
                    cr();
                }
                break;
            case CODE_BLOCK:
                visitCodeBlock(node);
                break;
            case THEMATIC_BREAK:
                cr();
                tag("<hr />");
                cr();
                break;
            case BLOCK_QUOTE:
                cr();
                tag(entering ? "<blockquote>" : "</blockquote>");
                cr();
                break;
            case LIST:
                visitList(node, entering);
                break;
            case ITEM:
                if (entering) {
                    tag("<li>");
                } else {
                    tag("</li>");
                    cr();
                }
                break;
            case HTML_BLOCK:
                cr();
                if (allowRawHtml) {
                    lit(filter(node.literal));
                } else {
                    tag("<pre class=\"md-raw-html\">");
                    text(node.literal);
                    tag("</pre>");
                }
                cr();
                break;
            case TABLE:
                cr();
                tag(entering ? "<table>" : "</table>");
                cr();
                break;
            case TABLE_ROW:
                visitRow(node, entering);
                break;
            case TABLE_CELL:
                visitCell(node, entering);
                break;
            default:
                break;
        }
    }

    /**
     * Writes a link.
     * <p>
     * 링크를 쓴다.
     *
     * @param node     the link / 링크
     * @param entering entering or leaving / 진입 또는 종료
     */
    private void visitLink(Node node, boolean entering) {
        if (!entering) {
            tag("</a>");
            return;
        }
        StringBuilder sb = new StringBuilder("<a");
        if (!(neutralize && MdText.isUnsafeUrl(node.destination))) {
            sb.append(" href=\"").append(MdText.escapeHtml(node.destination)).append('"');
        }
        if (node.title != null && node.title.length() > 0) {
            sb.append(" title=\"").append(MdText.escapeHtml(node.title)).append('"');
        }
        sb.append('>');
        tag(sb.toString());
    }

    /**
     * Writes an image: its description becomes the alt attribute text.
     * <p>
     * 이미지를 쓴다: 설명 부분이 alt 속성 텍스트가 된다.
     *
     * @param node     the image / 이미지
     * @param entering entering or leaving / 진입 또는 종료
     */
    private void visitImage(Node node, boolean entering) {
        if (entering) {
            if (disableTags == 0) {
                if (neutralize && MdText.isUnsafeUrl(node.destination)) {
                    lit("<img src=\"\" alt=\"");
                } else {
                    lit("<img src=\"" + MdText.escapeHtml(node.destination) + "\" alt=\"");
                }
            }
            disableTags++;
        } else {
            disableTags--;
            if (disableTags == 0) {
                if (node.title != null && node.title.length() > 0) {
                    lit("\" title=\"" + MdText.escapeHtml(node.title));
                }
                lit("\" />");
            }
        }
    }

    /**
     * Writes a paragraph; paragraphs of tight list items have no tags. The checkbox of a task list item is written at the start of its
     * first paragraph.
     * <p>
     * 문단을 쓴다. 촘촘한 목록 항목의 문단에는 태그가 없다. 작업 목록 항목의 체크박스는 첫 문단의 맨 앞에 쓴다.
     *
     * @param node     the paragraph / 문단
     * @param entering entering or leaving / 진입 또는 종료
     */
    private void visitParagraph(Node node, boolean entering) {
        Node grandparent = node.parent == null ? null : node.parent.parent;
        boolean tight = grandparent != null && grandparent.type == NodeType.LIST && grandparent.list != null && grandparent.list.tight;
        if (entering) {
            if (!tight) {
                cr();
                tag("<p>");
            }
            Node item = node.parent;
            if (item != null && item.type == NodeType.ITEM && item.task != 0 && item.firstChild == node) {
                tag(item.task == 2 ? "<input type=\"checkbox\" checked=\"\" disabled=\"\" /> " : "<input type=\"checkbox\" disabled=\"\" /> ");
            }
        } else if (!tight) {
            tag("</p>");
            cr();
        }
    }

    /**
     * Writes a code block.
     * <p>
     * 코드 블록을 쓴다.
     *
     * @param node the code block / 코드 블록
     */
    private void visitCodeBlock(Node node) {
        String lang = null;
        if (node.info != null && node.info.length() > 0) {
            int e = 0;
            // 정보 문자열의 첫 낱말이 언어 이름이다.
            while (e < node.info.length() && !Character.isWhitespace(node.info.charAt(e))) {
                e++;
            }
            if (e > 0) {
                lang = node.info.substring(0, e);
            }
        }
        cr();
        tag("<pre>");
        tag(lang == null ? "<code>" : "<code class=\"language-" + MdText.escapeHtml(lang) + "\">");
        text(node.literal);
        tag("</code>");
        tag("</pre>");
        cr();
    }

    /**
     * Writes a list.
     * <p>
     * 목록을 쓴다.
     *
     * @param node     the list / 목록
     * @param entering entering or leaving / 진입 또는 종료
     */
    private void visitList(Node node, boolean entering) {
        String name = node.list.ordered ? "ol" : "ul";
        if (entering) {
            cr();
            if (node.list.ordered && node.list.start != 1) {
                tag("<ol start=\"" + node.list.start + "\">");
            } else {
                tag("<" + name + ">");
            }
            cr();
        } else {
            cr();
            tag("</" + name + ">");
            cr();
        }
    }

    /**
     * Writes a table row, with the <code>thead</code> and <code>tbody</code> sections around it.
     * <p>
     * 표의 행을 쓴다. 앞뒤로 <code>thead</code>, <code>tbody</code> 구역을 만든다.
     *
     * @param node     the row / 행
     * @param entering entering or leaving / 진입 또는 종료
     */
    private void visitRow(Node node, boolean entering) {
        if (entering) {
            cr();
            if (node.header) {
                tag("<thead>");
                cr();
            } else if (node.prev != null && node.prev.header) {
                tag("<tbody>");
                cr();
            }
            tag("<tr>");
            cr();
        } else {
            tag("</tr>");
            cr();
            if (node.header) {
                tag("</thead>");
                cr();
            } else if (node.next == null) {
                tag("</tbody>");
                cr();
            }
        }
    }

    /**
     * Writes a table cell.
     * <p>
     * 표의 셀을 쓴다.
     *
     * @param node     the cell / 셀
     * @param entering entering or leaving / 진입 또는 종료
     */
    private void visitCell(Node node, boolean entering) {
        String name = node.header ? "th" : "td";
        if (entering) {
            String align = node.align == 1 ? " align=\"left\"" : node.align == 2 ? " align=\"center\"" : node.align == 3 ? " align=\"right\"" : "";
            tag("<" + name + align + ">");
        } else {
            tag("</" + name + ">");
            cr();
        }
    }

    /**
     * Applies the GFM tag filter: the leading less-than sign of the filtered tags becomes an entity.
     * <p>
     * GFM 태그 필터를 적용한다: 걸러 내는 태그의 여는 꺾쇠를 엔티티로 바꾼다.
     *
     * @param html the raw HTML / 원시 HTML
     * @return filtered HTML / 걸러낸 HTML
     */
    private String filter(String html) {
        if (!tagFilter || html.indexOf('<') < 0) {
            return html;
        }
        StringBuilder sb = new StringBuilder(html.length() + 8);
        int i = 0;
        int n = html.length();
        // 꺾쇠를 만날 때마다 걸러 낼 태그인지 확인한다.
        while (i < n) {
            char c = html.charAt(i);
            if (c == '<' && isFilteredTag(html, i)) {
                sb.append("&lt;");
            } else {
                sb.append(c);
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * Tells whether the tag that starts at a less-than sign is one of the filtered tags.
     * <p>
     * 꺾쇠에서 시작하는 태그가 걸러 낼 태그인지 알려 준다.
     *
     * @param s the text / 텍스트
     * @param i index of the less-than sign / 꺾쇠의 위치
     * @return true if it must be filtered / 걸러 내야 하면 true
     */
    private static boolean isFilteredTag(String s, int i) {
        int p = i + 1;
        if (p < s.length() && s.charAt(p) == '/') {
            p++;
        }
        // 걸러 낼 태그 이름과 대소문자 무시로 비교하고 이름 뒤에 공백, >, / 또는 끝이 오는지 본다.
        for (int k = 0; k < FILTERED.length; k++) {
            String name = FILTERED[k];
            if (s.regionMatches(true, p, name, 0, name.length())) {
                int e = p + name.length();
                if (e >= s.length() || MdText.isAsciiWhitespace(s.charAt(e)) || s.charAt(e) == '>' || s.charAt(e) == '/') {
                    return true;
                }
            }
        }
        return false;
    }
}
