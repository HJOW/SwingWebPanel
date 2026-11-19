package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * The block parser of CommonMark 0.31.2, a line by line implementation of the strategy in Appendix A of the specification: every line is
 * matched against the open container blocks, new blocks are started where the line allows it, and the rest of the line becomes text of
 * the innermost open block (or a lazy paragraph continuation). It also recognizes GFM tables and task list items when GFM is on.
 * Containers nested deeper than the configured limit are not opened; their markers stay as text. After all lines are read, the inline
 * parser runs over the paragraphs, headings and table cells. One instance parses one document on one thread.
 * <p>
 * CommonMark 0.31.2 의 블록 파서: 규격 부록 A 의 전략을 줄 단위로 구현한다. 각 줄을 열려 있는 컨테이너 블록과 맞춰 보고, 줄이 허용하는 곳에서
 * 새 블록을 시작하며, 나머지는 가장 안쪽 열린 블록의 텍스트(또는 느슨한 문단 이어쓰기)가 된다. GFM 이 켜져 있으면 표와 작업 목록 항목도 인식한다.
 * 설정한 제한보다 깊게 중첩될 컨테이너는 열지 않고 표식을 텍스트로 남긴다. 모든 줄을 읽은 뒤 문단, 제목, 표 셀에 인라인 파서를 돌린다.
 * 인스턴스 하나는 문서 하나를 한 스레드에서 파싱한다.
 */
final class BlockParser {

    /** Indentation of an indented code block. 들여쓰기 코드 블록의 들여쓰기. */
    private static final int CODE_INDENT = 4;

    /** Number of block start checks. 블록 시작 검사의 개수. */
    private static final int START_COUNT = 9;

    /** Whether the GFM extensions are on. GFM 확장이 켜져 있는지 여부. */
    private final boolean gfm;

    /** Maximum nesting depth of containers. 컨테이너의 최대 중첩 깊이. */
    private final int maxDepth;

    /** The inline parser. 인라인 파서. */
    private final InlineParser inlines;

    /** The root node. 루트 노드. */
    private final Node doc = new Node(NodeType.DOCUMENT);

    /** The innermost open block. 가장 안쪽의 열린 블록. */
    private Node tip = doc;

    /** The tip at the start of the current line. 현재 줄을 시작할 때의 tip. */
    private Node oldTip = doc;

    /** The last container that matched the current line. 현재 줄과 일치한 마지막 컨테이너. */
    private Node lastMatchedContainer = doc;

    /** The current line. 현재 줄. */
    private String currentLine = "";

    /** Number of the current line (1-based). 현재 줄 번호(1부터). */
    private int lineNumber;

    /** Offset in the current line. 현재 줄에서의 오프셋. */
    private int offset;

    /** Column in the current line (tabs expanded). 현재 줄에서의 열(탭 확장). */
    private int column;

    /** Offset of the next non-space character. 다음 공백 아닌 문자의 오프셋. */
    private int nextNonspace;

    /** Column of the next non-space character. 다음 공백 아닌 문자의 열. */
    private int nextNonspaceColumn;

    /** Indentation between the column and the next non-space character. 열과 다음 공백 아닌 문자 사이의 들여쓰기. */
    private int indent;

    /** True if the indentation is at least 4. 들여쓰기가 4 이상이면 true. */
    private boolean indented;

    /** True if the rest of the line is blank. 줄의 나머지가 비어 있으면 true. */
    private boolean blank;

    /** True if a tab was consumed only in part. 탭을 일부만 소비했으면 true. */
    private boolean partiallyConsumedTab;

    /** True if all open blocks matched the line. 열린 블록이 모두 줄과 일치했으면 true. */
    private boolean allClosed;

    /**
     * Creates a parser.
     * <p>
     * 파서를 만든다.
     *
     * @param opts options / 옵션
     * @param refs link reference definitions that the parser fills / 파서가 채우는 링크 참조 정의
     */
    BlockParser(MarkdownOptions opts, LinkRefs refs) {
        this.gfm = opts.isGfm();
        this.maxDepth = opts.getMaxBlockDepth();
        this.inlines = new InlineParser(refs, opts);
    }

    /**
     * Parses a Markdown text into a syntax tree with inline nodes.
     * <p>
     * Markdown 텍스트를 인라인 노드를 포함한 구문 트리로 파싱한다.
     *
     * @param input the Markdown text / Markdown 텍스트
     * @return the document node / 문서 노드
     */
    Node parse(String input) {
        int n = input.length();
        int i = 0;
        // 줄 끝(\n, \r\n, \r)마다 줄을 잘라 한 줄씩 처리한다.
        while (i < n) {
            int e = i;
            // 줄 끝 문자를 만날 때까지 한 줄의 끝을 찾는다.
            while (e < n && input.charAt(e) != '\n' && input.charAt(e) != '\r') {
                e++;
            }
            incorporateLine(input.substring(i, e));
            if (e < n && input.charAt(e) == '\r' && e + 1 < n && input.charAt(e + 1) == '\n') {
                e++;
            }
            i = e + 1;
        }
        // 열려 있는 블록을 안쪽부터 모두 닫는다.
        while (tip != null) {
            finalizeBlock(tip, lineNumber);
        }
        processInlines();
        return doc;
    }

    /**
     * Tells whether a block type takes the lines of the input as its raw content.
     * <p>
     * 블록 종류가 입력의 줄을 원본 내용으로 받는지 알려 준다.
     *
     * @param t the type / 종류
     * @return true for paragraphs, code blocks and HTML blocks / 문단, 코드 블록, HTML 블록이면 true
     */
    private static boolean acceptsLines(NodeType t) {
        return t == NodeType.PARAGRAPH || t == NodeType.CODE_BLOCK || t == NodeType.HTML_BLOCK;
    }

    /**
     * Tells whether a parent type may contain a child type.
     * <p>
     * 부모 종류가 자식 종류를 담을 수 있는지 알려 준다.
     *
     * @param parent parent type / 부모 종류
     * @param child  child type / 자식 종류
     * @return true if allowed / 허용되면 true
     */
    private static boolean canContain(NodeType parent, NodeType child) {
        switch (parent) {
            case DOCUMENT:
            case BLOCK_QUOTE:
            case ITEM:
                return child != NodeType.ITEM;
            case LIST:
                return child == NodeType.ITEM;
            default:
                return false;
        }
    }

    /**
     * Finds the next non-space character of the line and sets the line state variables.
     * <p>
     * 줄에서 다음 공백 아닌 문자를 찾고 줄 상태 변수를 갱신한다.
     */
    private void findNextNonspace() {
        int i = offset;
        int cols = column;
        int n = currentLine.length();
        // 공백과 탭을 건너뛰며 열을 계산한다.
        while (i < n) {
            char c = currentLine.charAt(i);
            if (c == ' ') {
                i++;
                cols++;
            } else if (c == '\t') {
                i++;
                cols += 4 - (cols % 4);
            } else {
                break;
            }
        }
        blank = i >= n;
        nextNonspace = i;
        nextNonspaceColumn = cols;
        indent = nextNonspaceColumn - column;
        indented = indent >= CODE_INDENT;
    }

    /**
     * Moves to the next non-space character.
     * <p>
     * 다음 공백 아닌 문자로 이동한다.
     */
    private void advanceNextNonspace() {
        offset = nextNonspace;
        column = nextNonspaceColumn;
        partiallyConsumedTab = false;
    }

    /**
     * Advances the offset by a number of characters or columns, splitting tabs where needed.
     * <p>
     * 오프셋을 문자 수 또는 열 수만큼 전진시키며 필요하면 탭을 나눈다.
     *
     * @param count   how far / 얼마나
     * @param columns true to count columns, false to count characters / 열 수이면 true, 문자 수이면 false
     */
    private void advanceOffset(int count, boolean columns) {
        int n = currentLine.length();
        // 지정한 만큼 한 글자씩 전진하며 탭은 열 기준으로 나눈다.
        while (count > 0 && offset < n) {
            char c = currentLine.charAt(offset);
            if (c == '\t') {
                int charsToTab = 4 - (column % 4);
                if (columns) {
                    partiallyConsumedTab = charsToTab > count;
                    int charsToAdvance = charsToTab > count ? count : charsToTab;
                    column += charsToAdvance;
                    offset += partiallyConsumedTab ? 0 : 1;
                    count -= charsToAdvance;
                } else {
                    partiallyConsumedTab = false;
                    column += charsToTab;
                    offset += 1;
                    count -= 1;
                }
            } else {
                partiallyConsumedTab = false;
                offset += 1;
                column += 1;
                count -= 1;
            }
        }
    }

    /**
     * Adds the rest of the current line to the raw content of the tip block.
     * <p>
     * 현재 줄의 나머지를 tip 블록의 원본 내용에 더한다.
     */
    private void addLine() {
        if (partiallyConsumedTab) {
            offset += 1;
            int charsToTab = 4 - (column % 4);
            // 일부만 소비한 탭은 남은 열 수만큼 공백으로 바꿔 넣는다.
            for (int k = 0; k < charsToTab; k++) {
                tip.content.append(' ');
            }
        }
        tip.content.append(currentLine, offset, currentLine.length()).append('\n');
    }

    /**
     * Adds a child block to the tip, closing blocks that cannot contain it.
     * <p>
     * tip 에 자식 블록을 더하며 그것을 담을 수 없는 블록은 닫는다.
     *
     * @param type kind of the new block / 새 블록의 종류
     * @return the new block / 새 블록
     */
    private Node addChild(NodeType type) {
        // 새 블록을 담을 수 있는 조상이 나올 때까지 열린 블록을 닫는다.
        while (!canContain(tip.type, type)) {
            finalizeBlock(tip, lineNumber - 1);
        }
        Node n = new Node(type);
        n.content = new StringBuilder();
        n.startLine = lineNumber;
        n.depth = tip.depth + 1;
        tip.appendChild(n);
        tip = n;
        return n;
    }

    /**
     * Closes the blocks that did not match the current line.
     * <p>
     * 현재 줄과 일치하지 않은 블록을 닫는다.
     */
    private void closeUnmatchedBlocks() {
        if (!allClosed) {
            // 일치한 마지막 컨테이너에 이를 때까지 안쪽 블록부터 닫는다.
            while (oldTip != lastMatchedContainer) {
                Node parent = oldTip.parent;
                finalizeBlock(oldTip, lineNumber - 1);
                oldTip = parent;
            }
            allClosed = true;
        }
    }

    /**
     * Tells the type of an HTML block start condition for the current line, or 0.
     * <p>
     * 현재 줄의 HTML 블록 시작 조건 번호를 알려 주며 없으면 0 이다.
     *
     * @param s the line from the position of the less-than sign / 꺾쇠 위치부터의 줄
     * @return start condition 1 to 7 or 0 / 시작 조건 1~7 또는 0
     */
    private static int htmlBlockStart(String s) {
        int n = s.length();
        if (s.startsWith("<!--")) {
            return 2;
        }
        if (s.startsWith("<?")) {
            return 3;
        }
        if (s.startsWith("<![CDATA[")) {
            return 5;
        }
        if (n >= 3 && s.charAt(1) == '!' && MdText.isAsciiLetter(s.charAt(2))) {
            return 4;
        }
        int p = 1;
        boolean closing = false;
        if (p < n && s.charAt(p) == '/') {
            closing = true;
            p++;
        }
        int nameStart = p;
        // 태그 이름은 영숫자로 이어진다.
        while (p < n && MdText.isAsciiAlnum(s.charAt(p))) {
            p++;
        }
        String name = s.substring(nameStart, p).toLowerCase(java.util.Locale.ENGLISH);
        if (!closing && (name.equals("script") || name.equals("pre") || name.equals("textarea") || name.equals("style"))
                && (p >= n || s.charAt(p) == '>' || MdText.isAsciiWhitespace(s.charAt(p)))) {
            return 1;
        }
        if (name.length() > 0 && HtmlTags.isBlockName(name)) {
            if (p >= n || MdText.isAsciiWhitespace(s.charAt(p)) || s.charAt(p) == '>' || (s.charAt(p) == '/' && p + 1 < n && s.charAt(p + 1) == '>')) {
                return 6;
            }
        }
        int end = HtmlTags.matchTag(s, 0);
        if (end > 0) {
            // 조건 7: 태그 뒤에는 공백뿐이어야 한다.
            if (MdText.isBlank(s.substring(end))) {
                return 7;
            }
        }
        return 0;
    }

    /**
     * Tells whether a line ends an HTML block of start condition 1 to 5.
     * <p>
     * 줄이 시작 조건 1~5 의 HTML 블록을 끝내는지 알려 준다.
     *
     * @param type start condition / 시작 조건
     * @param s    the line / 줄
     * @return true if the line contains the end marker / 줄에 끝 표식이 있으면 true
     */
    private static boolean htmlBlockEnds(int type, String s) {
        switch (type) {
            case 1:
                return containsIgnoreCase(s, "</script>") || containsIgnoreCase(s, "</pre>") || containsIgnoreCase(s, "</textarea>")
                        || containsIgnoreCase(s, "</style>");
            case 2:
                return s.indexOf("-->") >= 0;
            case 3:
                return s.indexOf("?>") >= 0;
            case 4:
                return s.indexOf('>') >= 0;
            case 5:
                return s.indexOf("]]>") >= 0;
            default:
                return false;
        }
    }

    /**
     * Tells whether a string contains a substring, ignoring ASCII case.
     * <p>
     * 문자열이 부분 문자열을 포함하는지 ASCII 대소문자를 무시하고 알려 준다.
     *
     * @param s   the string / 문자열
     * @param sub the lower-case substring / 소문자 부분 문자열
     * @return true if contained / 포함하면 true
     */
    private static boolean containsIgnoreCase(String s, String sub) {
        int max = s.length() - sub.length();
        // 모든 위치에서 대소문자 무시 비교를 해 본다.
        for (int i = 0; i <= max; i++) {
            if (s.regionMatches(true, i, sub, 0, sub.length())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processes one line of input.
     * <p>
     * 입력의 한 줄을 처리한다.
     *
     * @param rawLine the line without its line ending / 줄 끝을 뺀 줄
     */
    private void incorporateLine(String rawLine) {
        String ln = rawLine;
        boolean allMatched = true;
        Node container = doc;
        oldTip = tip;
        offset = 0;
        column = 0;
        blank = false;
        partiallyConsumedTab = false;
        lineNumber++;
        if (ln.indexOf((char) 0) >= 0) {
            ln = ln.replace((char) 0, (char) 0xFFFD);
        }
        currentLine = ln;
        Node lastChild;
        // 열려 있는 컨테이너마다 이번 줄이 이어지는지 확인한다.
        while ((lastChild = container.lastChild) != null && lastChild.open) {
            container = lastChild;
            findNextNonspace();
            int r = continueBlock(container);
            if (r == 1) {
                allMatched = false;
            } else if (r == 2) {
                return;
            }
            if (!allMatched) {
                container = container.parent;
                break;
            }
        }
        allClosed = container == oldTip;
        lastMatchedContainer = container;
        boolean matchedLeaf = container.type != NodeType.PARAGRAPH && acceptsLines(container.type);
        // 코드 블록 안이 아니면 새 블록을 시작할 수 있는지 계속 확인한다.
        while (!matchedLeaf) {
            findNextNonspace();
            if (!indented && !maybeSpecial(container)) {
                advanceNextNonspace();
                break;
            }
            int i;
            // 블록 시작 검사를 차례로 시도한다.
            for (i = 0; i < START_COUNT; i++) {
                int res = blockStart(i, container);
                if (res == 1) {
                    container = tip;
                    break;
                } else if (res == 2) {
                    container = tip;
                    matchedLeaf = true;
                    break;
                }
            }
            if (i == START_COUNT) {
                advanceNextNonspace();
                break;
            }
        }
        if (!allClosed && !blank && tip.type == NodeType.PARAGRAPH) {
            addLine();
            return;
        }
        closeUnmatchedBlocks();
        if (blank && container.lastChild != null) {
            container.lastChild.lastLineBlank = true;
        }
        NodeType t = container.type;
        boolean lastLineBlank = blank && !(t == NodeType.BLOCK_QUOTE || (t == NodeType.CODE_BLOCK && container.fenced)
                || (t == NodeType.ITEM && container.firstChild == null && container.startLine == lineNumber));
        Node cont = container;
        // 마지막 줄이 비었는지를 조상 쪽으로 전파한다.
        while (cont != null) {
            cont.lastLineBlank = lastLineBlank;
            cont = cont.parent;
        }
        if (acceptsLines(t)) {
            addLine();
            if (t == NodeType.HTML_BLOCK && container.htmlBlockType >= 1 && container.htmlBlockType <= 5
                    && htmlBlockEnds(container.htmlBlockType, currentLine.substring(Math.min(offset, currentLine.length())))) {
                finalizeBlock(container, lineNumber);
            }
        } else if (t == NodeType.TABLE) {
            if (offset < ln.length() && !blank) {
                addTableRow(container, ln.substring(offset));
            }
        } else if (offset < ln.length() && !blank) {
            addChild(NodeType.PARAGRAPH);
            advanceNextNonspace();
            addLine();
        }
    }


    /**
     * Quick test whether the line can start a block at the next non-space character.
     * <p>
     * 줄이 다음 공백 아닌 문자에서 블록을 시작할 수 있는지 빠르게 검사한다.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return true if a start is possible / 시작할 수 있으면 true
     */
    private boolean maybeSpecial(Node container) {
        if (nextNonspace >= currentLine.length()) {
            return false;
        }
        char c = currentLine.charAt(nextNonspace);
        if ("#`~*+_=<>-".indexOf(c) >= 0 || (c >= '0' && c <= '9')) {
            return true;
        }
        return gfm && container.type == NodeType.PARAGRAPH && (c == '|' || c == ':');
    }

    /**
     * Checks whether the current line continues an open block.
     * <p>
     * 현재 줄이 열린 블록을 이어 가는지 확인한다.
     *
     * @param c the open block / 열린 블록
     * @return 0 if it continues, 1 if not, 2 if the line was consumed completely (closing fence) / 이어지면 0, 아니면 1, 줄을 모두 소비했으면(닫는 울타리) 2
     */
    private int continueBlock(Node c) {
        switch (c.type) {
            case DOCUMENT:
            case LIST:
                return 0;
            case BLOCK_QUOTE:
                if (!indented && nextNonspace < currentLine.length() && currentLine.charAt(nextNonspace) == '>') {
                    advanceNextNonspace();
                    advanceOffset(1, false);
                    if (offset < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(offset))) {
                        advanceOffset(1, true);
                    }
                    return 0;
                }
                return 1;
            case ITEM:
                if (blank) {
                    if (c.firstChild == null) {
                        return 1;
                    }
                    advanceNextNonspace();
                    return 0;
                }
                if (indent >= c.list.markerOffset + c.list.padding) {
                    advanceOffset(c.list.markerOffset + c.list.padding, true);
                    return 0;
                }
                return 1;
            case CODE_BLOCK:
                return continueCodeBlock(c);
            case HTML_BLOCK:
                return blank && (c.htmlBlockType == 6 || c.htmlBlockType == 7) ? 1 : 0;
            case PARAGRAPH:
            case TABLE:
                return blank ? 1 : 0;
            default:
                return 1;
        }
    }

    /**
     * Continuation of a code block: closing fence, indentation or blank lines.
     * <p>
     * 코드 블록의 이어짐: 닫는 울타리, 들여쓰기, 빈 줄.
     *
     * @param c the code block / 코드 블록
     * @return 0, 1 or 2 as for {@link #continueBlock} / {@link #continueBlock} 과 같은 0, 1, 2
     */
    private int continueCodeBlock(Node c) {
        if (c.fenced) {
            if (indent <= 3 && nextNonspace < currentLine.length() && currentLine.charAt(nextNonspace) == c.fenceChar) {
                int p = nextNonspace;
                // 닫는 울타리는 같은 문자의 연속이며 길이가 열 때 이상이고 뒤에는 공백뿐이다.
                while (p < currentLine.length() && currentLine.charAt(p) == c.fenceChar) {
                    p++;
                }
                int run = p - nextNonspace;
                int q = p;
                // 닫는 울타리 뒤의 공백과 탭을 건너뛴다.
                while (q < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(q))) {
                    q++;
                }
                if (run >= c.fenceLength && q == currentLine.length()) {
                    finalizeBlock(c, lineNumber);
                    return 2;
                }
            }
            int i = c.fenceOffset;
            // 여는 울타리의 들여쓰기만큼 줄 앞의 공백을 걷어 낸다.
            while (i > 0 && offset < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(offset))) {
                advanceOffset(1, true);
                i--;
            }
            return 0;
        }
        if (indent >= CODE_INDENT) {
            advanceOffset(CODE_INDENT, true);
        } else if (blank) {
            advanceNextNonspace();
        } else {
            return 1;
        }
        return 0;
    }

    /**
     * Tries one block start.
     * <p>
     * 블록 시작 하나를 시도한다.
     *
     * @param which     index of the start check / 시작 검사 번호
     * @param container the matched container / 일치한 컨테이너
     * @return 0 if the line does not start such a block, 1 for a container block, 2 for a leaf block / 시작하지 않으면 0, 컨테이너 블록이면 1, 잎 블록이면 2
     */
    private int blockStart(int which, Node container) {
        switch (which) {
            case 0:
                return blockQuoteStart(container);
            case 1:
                return atxHeadingStart();
            case 2:
                return fencedCodeStart();
            case 3:
                return htmlBlockStart(container);
            case 4:
                return setextHeadingStart(container);
            case 5:
                return thematicBreakStart();
            case 6:
                return listItemStart(container);
            case 7:
                return indentedCodeStart();
            default:
                return gfm ? tableStart(container) : 0;
        }
    }

    /**
     * Block quote start.
     * <p>
     * 인용 블록 시작.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return start result / 시작 결과
     */
    private int blockQuoteStart(Node container) {
        if (!indented && nextNonspace < currentLine.length() && currentLine.charAt(nextNonspace) == '>' && container.depth < maxDepth) {
            advanceNextNonspace();
            advanceOffset(1, false);
            if (offset < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(offset))) {
                advanceOffset(1, true);
            }
            closeUnmatchedBlocks();
            addChild(NodeType.BLOCK_QUOTE);
            return 1;
        }
        return 0;
    }

    /**
     * ATX heading start.
     * <p>
     * ATX 제목 시작.
     *
     * @return start result / 시작 결과
     */
    private int atxHeadingStart() {
        if (indented || nextNonspace >= currentLine.length() || currentLine.charAt(nextNonspace) != '#') {
            return 0;
        }
        int p = nextNonspace;
        // # 문자가 연속된 길이를 센다. 1~6 이고 뒤에 공백/탭/줄 끝이 와야 한다.
        while (p < currentLine.length() && currentLine.charAt(p) == '#') {
            p++;
        }
        int level = p - nextNonspace;
        if (level > 6 || (p < currentLine.length() && !MdText.isSpaceOrTab(currentLine.charAt(p)))) {
            return 0;
        }
        int q = p;
        // 여는 # 열 뒤의 공백과 탭을 건너뛴다.
        while (q < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(q))) {
            q++;
        }
        advanceNextNonspace();
        advanceOffset(q - nextNonspace, false);
        closeUnmatchedBlocks();
        Node h = addChild(NodeType.HEADING);
        h.level = level;
        h.content.append(stripAtxClosing(currentLine.substring(offset)));
        advanceOffset(currentLine.length() - offset, false);
        return 2;
    }

    /**
     * Removes the optional closing sequence of an ATX heading.
     * <p>
     * ATX 제목의 선택적인 닫는 # 열을 제거한다.
     *
     * @param s the heading text / 제목 텍스트
     * @return text without the closing sequence / 닫는 열을 뺀 텍스트
     */
    private static String stripAtxClosing(String s) {
        int e = s.length();
        // 끝의 공백과 탭을 건너뛴다.
        while (e > 0 && MdText.isSpaceOrTab(s.charAt(e - 1))) {
            e--;
        }
        int h = e;
        // 끝의 # 열을 건너뛴다.
        while (h > 0 && s.charAt(h - 1) == '#') {
            h--;
        }
        if (h == e) {
            return s.substring(0, e);
        }
        if (h == 0) {
            return "";
        }
        if (MdText.isSpaceOrTab(s.charAt(h - 1))) {
            int b = h;
            // 제목 내용 끝의 공백을 건너뛴다.
            while (b > 0 && MdText.isSpaceOrTab(s.charAt(b - 1))) {
                b--;
            }
            return s.substring(0, b);
        }
        return s.substring(0, e);
    }

    /**
     * Fenced code block start.
     * <p>
     * 울타리 코드 블록 시작.
     *
     * @return start result / 시작 결과
     */
    private int fencedCodeStart() {
        if (indented || nextNonspace >= currentLine.length()) {
            return 0;
        }
        char c = currentLine.charAt(nextNonspace);
        if (c != '`' && c != '~') {
            return 0;
        }
        int p = nextNonspace;
        // 같은 문자가 연속된 울타리 길이를 센다.
        while (p < currentLine.length() && currentLine.charAt(p) == c) {
            p++;
        }
        int run = p - nextNonspace;
        if (run < 3 || (c == '`' && currentLine.indexOf('`', p) >= 0)) {
            return 0;
        }
        closeUnmatchedBlocks();
        Node b = addChild(NodeType.CODE_BLOCK);
        b.fenced = true;
        b.fenceLength = run;
        b.fenceChar = c;
        b.fenceOffset = indent;
        advanceNextNonspace();
        advanceOffset(run, false);
        return 2;
    }

    /**
     * HTML block start.
     * <p>
     * HTML 블록 시작.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return start result / 시작 결과
     */
    private int htmlBlockStart(Node container) {
        if (indented || nextNonspace >= currentLine.length() || currentLine.charAt(nextNonspace) != '<') {
            return 0;
        }
        int type = htmlBlockStart(currentLine.substring(nextNonspace));
        if (type == 0) {
            return 0;
        }
        if (type == 7 && (container.type == NodeType.PARAGRAPH || (!allClosed && !blank && tip.type == NodeType.PARAGRAPH))) {
            return 0;
        }
        closeUnmatchedBlocks();
        Node b = addChild(NodeType.HTML_BLOCK);
        b.htmlBlockType = type;
        return 2;
    }

    /**
     * Consumes the link reference definitions at the start of a paragraph's content.
     * <p>
     * 문단 내용의 앞에 있는 링크 참조 정의를 소비한다.
     *
     * @param p the paragraph / 문단
     * @return true if at least one definition was consumed / 정의를 하나라도 소비했으면 true
     */
    private boolean consumeReferences(Node p) {
        String s = p.content.toString();
        int pos = 0;
        boolean has = false;
        // 문단 앞에서부터 참조 정의가 이어지는 동안 소비한다.
        while (pos < s.length() && s.charAt(pos) == '[') {
            int np = inlines.parseReference(s, pos);
            if (np < 0) {
                break;
            }
            pos = np;
            has = true;
        }
        if (has) {
            p.content = new StringBuilder(s.substring(pos));
        }
        return has;
    }

    /**
     * Setext heading start: an underline below a paragraph.
     * <p>
     * setext 제목 시작: 문단 아래의 밑줄.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return start result / 시작 결과
     */
    private int setextHeadingStart(Node container) {
        if (indented || container.type != NodeType.PARAGRAPH || nextNonspace >= currentLine.length()) {
            return 0;
        }
        char c = currentLine.charAt(nextNonspace);
        if (c != '=' && c != '-') {
            return 0;
        }
        int p = nextNonspace;
        // 밑줄 문자가 연속된 구간을 지나 뒤에는 공백뿐이어야 한다.
        while (p < currentLine.length() && currentLine.charAt(p) == c) {
            p++;
        }
        // 밑줄 뒤의 공백과 탭을 건너뛴다.
        while (p < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(p))) {
            p++;
        }
        if (p < currentLine.length()) {
            return 0;
        }
        closeUnmatchedBlocks();
        consumeReferences(container);
        if (container.content.length() > 0 && !MdText.isBlank(container.content.toString())) {
            Node heading = new Node(NodeType.HEADING);
            heading.level = c == '=' ? 1 : 2;
            heading.content = container.content;
            heading.startLine = container.startLine;
            heading.depth = container.depth;
            container.insertAfter(heading);
            container.unlink();
            tip = heading;
            advanceOffset(currentLine.length() - offset, false);
            return 2;
        }
        return 0;
    }

    /**
     * Thematic break start.
     * <p>
     * 주제 분리선 시작.
     *
     * @return start result / 시작 결과
     */
    private int thematicBreakStart() {
        if (indented || nextNonspace >= currentLine.length()) {
            return 0;
        }
        char c = currentLine.charAt(nextNonspace);
        if (c != '*' && c != '_' && c != '-') {
            return 0;
        }
        int count = 0;
        // 같은 문자가 공백/탭을 사이에 두고 3 개 이상이고 다른 문자가 없어야 한다.
        for (int i = nextNonspace; i < currentLine.length(); i++) {
            char d = currentLine.charAt(i);
            if (d == c) {
                count++;
            } else if (!MdText.isSpaceOrTab(d)) {
                return 0;
            }
        }
        if (count < 3) {
            return 0;
        }
        closeUnmatchedBlocks();
        addChild(NodeType.THEMATIC_BREAK);
        advanceOffset(currentLine.length() - offset, false);
        return 2;
    }

    /**
     * Parses a list marker at the next non-space character.
     * <p>
     * 다음 공백 아닌 문자에서 목록 표식을 해석한다.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return the list data, or null if there is no marker / 목록 정보. 표식이 없으면 null
     */
    private Node.ListData parseListMarker(Node container) {
        if (indent >= CODE_INDENT || nextNonspace >= currentLine.length()) {
            return null;
        }
        Node.ListData data = new Node.ListData();
        data.markerOffset = indent;
        char c = currentLine.charAt(nextNonspace);
        int markerLen;
        if (c == '*' || c == '+' || c == '-') {
            data.ordered = false;
            data.bulletChar = c;
            markerLen = 1;
        } else if (c >= '0' && c <= '9') {
            int p = nextNonspace;
            // 숫자는 최대 9자리이며 뒤에 . 또는 ) 가 와야 한다.
            while (p < currentLine.length() && currentLine.charAt(p) >= '0' && currentLine.charAt(p) <= '9' && p - nextNonspace < 10) {
                p++;
            }
            int digits = p - nextNonspace;
            if (digits > 9 || p >= currentLine.length() || (currentLine.charAt(p) != '.' && currentLine.charAt(p) != ')')) {
                return null;
            }
            int start = Integer.parseInt(currentLine.substring(nextNonspace, p));
            if (container.type == NodeType.PARAGRAPH && start != 1) {
                return null;
            }
            data.ordered = true;
            data.start = start;
            data.delimiter = currentLine.charAt(p);
            markerLen = digits + 1;
        } else {
            return null;
        }
        int after = nextNonspace + markerLen;
        if (after < currentLine.length() && !MdText.isSpaceOrTab(currentLine.charAt(after))) {
            return null;
        }
        if (container.type == NodeType.PARAGRAPH && MdText.isBlank(currentLine.substring(after))) {
            return null;
        }
        advanceNextNonspace();
        advanceOffset(markerLen, true);
        int spacesStartCol = column;
        int spacesStartOffset = offset;
        // 표식 뒤의 공백을 최대 5열까지 소비한다.
        do {
            advanceOffset(1, true);
        } while (column - spacesStartCol < 5 && offset < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(offset)));
        boolean blankItem = offset >= currentLine.length();
        int spacesAfter = column - spacesStartCol;
        if (spacesAfter >= 5 || spacesAfter < 1 || blankItem) {
            data.padding = markerLen + 1;
            column = spacesStartCol;
            offset = spacesStartOffset;
            if (offset < currentLine.length() && MdText.isSpaceOrTab(currentLine.charAt(offset))) {
                advanceOffset(1, true);
            }
        } else {
            data.padding = markerLen + spacesAfter;
        }
        return data;
    }

    /**
     * Tells whether two list data describe items of the same list.
     * <p>
     * 두 목록 정보가 같은 목록의 항목인지 알려 준다.
     *
     * @param a list data of the list / 목록의 정보
     * @param b list data of the new item / 새 항목의 정보
     * @return true if of the same type / 같은 종류이면 true
     */
    private static boolean listsMatch(Node.ListData a, Node.ListData b) {
        return a != null && a.ordered == b.ordered && a.delimiter == b.delimiter && a.bulletChar == b.bulletChar;
    }

    /**
     * List item start.
     * <p>
     * 목록 항목 시작.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return start result / 시작 결과
     */
    private int listItemStart(Node container) {
        if ((indented && container.type != NodeType.LIST) || container.depth >= maxDepth) {
            return 0;
        }
        Node.ListData data = parseListMarker(container);
        if (data == null) {
            return 0;
        }
        closeUnmatchedBlocks();
        Node c = container;
        if (tip.type != NodeType.LIST || !listsMatch(c.list, data)) {
            c = addChild(NodeType.LIST);
            c.list = data;
        }
        c = addChild(NodeType.ITEM);
        c.list = data;
        return 1;
    }

    /**
     * Indented code block start.
     * <p>
     * 들여쓰기 코드 블록 시작.
     *
     * @return start result / 시작 결과
     */
    private int indentedCodeStart() {
        if (indented && tip.type != NodeType.PARAGRAPH && !blank) {
            advanceOffset(CODE_INDENT, true);
            closeUnmatchedBlocks();
            addChild(NodeType.CODE_BLOCK);
            return 2;
        }
        return 0;
    }

    /**
     * GFM table start: a delimiter row below a paragraph whose last line is the header row with the same number of cells.
     * <p>
     * GFM 표 시작: 마지막 줄이 같은 수의 셀을 가진 머리 행인 문단 아래의 구분 행.
     *
     * @param container the matched container / 일치한 컨테이너
     * @return start result / 시작 결과
     */
    private int tableStart(Node container) {
        if (indented || container.type != NodeType.PARAGRAPH || nextNonspace >= currentLine.length()) {
            return 0;
        }
        int[] aligns = TableSupport.parseDelimiterRow(currentLine.substring(nextNonspace));
        if (aligns == null) {
            return 0;
        }
        String c = container.content.toString();
        if (c.endsWith("\n")) {
            c = c.substring(0, c.length() - 1);
        }
        int nl = c.lastIndexOf('\n');
        String header = c.substring(nl + 1);
        String before = nl < 0 ? "" : c.substring(0, nl + 1);
        List<String> headerCells = TableSupport.splitRow(header);
        if (headerCells.size() != aligns.length || MdText.isBlank(header)) {
            return 0;
        }
        closeUnmatchedBlocks();
        if (before.length() > 0) {
            container.content = new StringBuilder(before);
            finalizeBlock(container, lineNumber - 1);
        } else {
            Node parent = container.parent;
            container.unlink();
            tip = parent;
        }
        Node table = addChild(NodeType.TABLE);
        table.aligns = aligns;
        Node row = new Node(NodeType.TABLE_ROW);
        row.open = false;
        row.header = true;
        // 머리 셀을 만들어 행에 붙인다.
        for (int i = 0; i < aligns.length; i++) {
            Node cell = new Node(NodeType.TABLE_CELL);
            cell.open = false;
            cell.header = true;
            cell.align = aligns[i];
            cell.content = new StringBuilder(headerCells.get(i));
            row.appendChild(cell);
        }
        table.appendChild(row);
        advanceOffset(currentLine.length() - offset, false);
        return 2;
    }

    /**
     * Adds a body row to a table; missing cells are empty and extra cells are ignored.
     * <p>
     * 표에 본문 행을 더한다. 모자란 셀은 빈 셀이고 남는 셀은 무시한다.
     *
     * @param table the table / 표
     * @param text  the row text / 행 텍스트
     */
    private void addTableRow(Node table, String text) {
        List<String> cells = TableSupport.splitRow(text);
        Node row = new Node(NodeType.TABLE_ROW);
        row.open = false;
        // 머리 행의 열 수에 맞춰 셀을 만든다.
        for (int i = 0; i < table.aligns.length; i++) {
            Node cell = new Node(NodeType.TABLE_CELL);
            cell.open = false;
            cell.align = table.aligns[i];
            cell.content = new StringBuilder(i < cells.size() ? cells.get(i) : "");
            row.appendChild(cell);
        }
        table.appendChild(row);
    }

    /**
     * Closes a block and applies the end-of-block processing of its type.
     * <p>
     * 블록을 닫고 그 종류의 블록 종료 처리를 한다.
     *
     * @param block the block / 블록
     * @param line  the last line number of the block / 블록의 마지막 줄 번호
     */
    private void finalizeBlock(Node block, int line) {
        Node above = block.parent;
        block.open = false;
        switch (block.type) {
            case PARAGRAPH:
                consumeReferences(block);
                if (MdText.isBlank(block.content.toString())) {
                    block.unlink();
                }
                break;
            case LIST:
                finalizeList(block);
                break;
            case CODE_BLOCK:
                finalizeCode(block);
                break;
            case HTML_BLOCK:
                block.literal = stripTrailingBlankLines(block.content.toString(), false);
                block.content = null;
                break;
            default:
                break;
        }
        tip = above;
    }

    /**
     * Decides whether a list is tight.
     * <p>
     * 목록이 촘촘한지 판정한다.
     *
     * @param list the list / 목록
     */
    private void finalizeList(Node list) {
        Node item = list.firstChild;
        // 마지막이 아닌 항목이 빈 줄로 끝나거나 항목 안의 블록 사이에 빈 줄이 있으면 느슨한 목록이다.
        while (item != null) {
            if (endsWithBlankLine(item) && item.next != null) {
                list.list.tight = false;
                break;
            }
            Node sub = item.firstChild;
            // 항목 안의 블록마다 빈 줄로 끝나는지 확인한다.
            while (sub != null) {
                if (endsWithBlankLine(sub) && (item.next != null || sub.next != null)) {
                    list.list.tight = false;
                    break;
                }
                sub = sub.next;
            }
            if (!list.list.tight) {
                break;
            }
            item = item.next;
        }
    }

    /**
     * Tells whether a block (or the last block inside a list or item) ended with a blank line.
     * <p>
     * 블록(또는 목록/항목의 마지막 블록)이 빈 줄로 끝났는지 알려 준다.
     *
     * @param block the block / 블록
     * @return true if it ended with a blank line / 빈 줄로 끝났으면 true
     */
    private static boolean endsWithBlankLine(Node block) {
        Node b = block;
        // 목록과 항목은 마지막 자식을 따라 내려가며 확인한다.
        while (b != null) {
            if (b.lastLineBlank) {
                return true;
            }
            if (!b.lastLineChecked && (b.type == NodeType.LIST || b.type == NodeType.ITEM)) {
                b.lastLineChecked = true;
                b = b.lastChild;
            } else {
                b.lastLineChecked = true;
                break;
            }
        }
        return false;
    }

    /**
     * Finishes a code block: the info string and the literal text.
     * <p>
     * 코드 블록을 마무리한다: 정보 문자열과 글자 내용.
     *
     * @param block the code block / 코드 블록
     */
    private void finalizeCode(Node block) {
        String content = block.content.toString();
        block.content = null;
        if (block.fenced) {
            int nl = content.indexOf('\n');
            String first = nl < 0 ? content : content.substring(0, nl);
            block.info = MdText.unescapeString(MdText.trim(first));
            block.literal = nl < 0 ? "" : content.substring(nl + 1);
        } else {
            block.literal = stripTrailingBlankLines(content, true);
        }
    }

    /**
     * Removes trailing blank lines of a block's content.
     * <p>
     * 블록 내용 끝의 빈 줄을 제거한다.
     *
     * @param s           the content / 내용
     * @param keepNewline true to keep one final line feed / 마지막 줄 바꿈 하나를 남기려면 true
     * @return the content without trailing blank lines / 끝의 빈 줄을 제거한 내용
     */
    private static String stripTrailingBlankLines(String s, boolean keepNewline) {
        int e = s.length();
        // 끝에서부터 "줄 바꿈 + 공백들" 묶음을 하나씩 걷어 낸다(마지막 줄 자체의 끝 공백은 남긴다).
        while (true) {
            int p = e;
            // 끝의 공백을 건너뛴다.
            while (p > 0 && s.charAt(p - 1) == ' ') {
                p--;
            }
            if (p > 0 && s.charAt(p - 1) == '\n') {
                e = p - 1;
            } else {
                break;
            }
        }
        return keepNewline ? s.substring(0, e) + "\n" : s.substring(0, e);
    }

    /**
     * Runs the inline parser over every paragraph, heading and table cell, and marks task list items.
     * <p>
     * 모든 문단, 제목, 표 셀에 인라인 파서를 돌리고 작업 목록 항목을 표시한다.
     */
    private void processInlines() {
        List<Node> targets = new ArrayList<Node>();
        Node n = doc;
        // 블록 트리를 재귀 없이 순회하며 인라인 처리 대상을 모은다.
        while (n != null) {
            if (n.type == NodeType.PARAGRAPH || n.type == NodeType.HEADING || n.type == NodeType.TABLE_CELL) {
                targets.add(n);
            }
            if (n.firstChild != null) {
                n = n.firstChild;
            } else {
                // 다음 형제가 없으면 부모로 올라간다.
                while (n != null && n != doc && n.next == null) {
                    n = n.parent;
                }
                if (n == null || n == doc) {
                    break;
                }
                n = n.next;
            }
        }
        // 대상마다 작업 표식을 확인한 뒤 인라인을 파싱하고 원본 내용을 버린다.
        for (int i = 0; i < targets.size(); i++) {
            Node t = targets.get(i);
            String raw = t.content.toString();
            if (gfm && t.type == NodeType.PARAGRAPH && t.parent != null && t.parent.type == NodeType.ITEM && t.parent.firstChild == t) {
                raw = detectTask(t.parent, raw);
            }
            inlines.parse(t, raw);
            t.content = null;
        }
    }

    /**
     * Detects a task list marker <code>[ ]</code>, <code>[x]</code> at the start of the first paragraph of a list item.
     * <p>
     * 목록 항목의 첫 문단 맨 앞에서 작업 표식 <code>[ ]</code>, <code>[x]</code> 를 찾는다.
     *
     * @param item the list item / 목록 항목
     * @param raw  the raw paragraph text / 문단 원본 텍스트
     * @return the text without the marker if there was one, otherwise the same text / 표식이 있었으면 표식을 뺀 텍스트, 아니면 같은 텍스트
     */
    private static String detectTask(Node item, String raw) {
        int p = 0;
        int n = raw.length();
        // 표식 앞의 공백/탭을 건너뛴다.
        while (p < n && MdText.isSpaceOrTab(raw.charAt(p))) {
            p++;
        }
        if (p + 3 < n && raw.charAt(p) == '[' && raw.charAt(p + 2) == ']') {
            char m = raw.charAt(p + 1);
            char after = raw.charAt(p + 3);
            if ((m == ' ' || m == 'x' || m == 'X') && (after == ' ' || after == '\t' || after == '\n')) {
                item.task = m == ' ' ? 1 : 2;
                return raw.substring(p + 4);
            }
        }
        return raw;
    }
}
