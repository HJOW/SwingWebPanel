package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.HashSet;
import java.util.Set;

/**
 * The inline parser of CommonMark 0.31.2: it turns the raw text of a paragraph, heading or table cell into inline nodes (text, code spans,
 * emphasis, links, images, autolinks, raw HTML, line breaks) using the delimiter stack algorithm for emphasis and the bracket stack
 * algorithm for links, plus the GFM strikethrough and extended autolinks when GFM is on. Every scan that could be repeated at many
 * positions of one input is bounded or cached so that adversarial input stays linear or near linear (see the notes of each method).
 * One instance is used for one document on one thread.
 * <p>
 * CommonMark 0.31.2 의 인라인 파서: 문단, 제목, 표 셀의 원본 텍스트를 강조에는 구분자 스택 알고리즘, 링크에는 대괄호 스택 알고리즘을 써서 인라인
 * 노드(텍스트, 코드 스팬, 강조, 링크, 이미지, 자동 링크, 원시 HTML, 줄바꿈)로 바꾸며 GFM 이 켜져 있으면 취소선과 확장 자동 링크도 처리한다.
 * 한 입력의 여러 위치에서 되풀이될 수 있는 모든 탐색은 제한하거나 결과를 캐시하여 악의적인 입력에서도 선형에 가깝게 유지한다(각 메서드의
 * 설명 참조). 인스턴스 하나는 문서 하나를 한 스레드에서 처리하는 데 쓴다.
 */
final class InlineParser {

    /** Maximum nesting of parentheses in an unbracketed link destination. 꺾쇠 없는 링크 목적지에서 괄호의 최대 중첩. */
    private static final int MAX_PAREN_DEPTH = 32;

    /** Maximum number of characters inside the brackets of a link label. 링크 라벨 대괄호 안의 최대 문자 수. */
    private static final int MAX_LABEL_CHARS = 999;

    /** An entry of the delimiter stack (emphasis and strikethrough runs). 구분자 스택의 항목(강조/취소선 구분자 열). */
    private static final class Delimiter {
        /** The delimiter character. 구분자 문자. */
        char cc;

        /** Number of delimiter characters still unused. 아직 쓰이지 않은 구분자 문자 수. */
        int numdelims;

        /** Original length of the run. 원래 열의 길이. */
        int origdelims;

        /** The text node that holds the delimiter characters. 구분자 문자를 담은 텍스트 노드. */
        Node node;

        /** Previous entry (towards the bottom). 이전 항목(바닥 쪽). */
        Delimiter previous;

        /** Next entry (towards the top). 다음 항목(꼭대기 쪽). */
        Delimiter next;

        /** Whether the run can open emphasis. 열이 강조를 열 수 있는지 여부. */
        boolean canOpen;

        /** Whether the run can close emphasis. 열이 강조를 닫을 수 있는지 여부. */
        boolean canClose;
    }

    /** An entry of the bracket stack (potential link or image openers). 대괄호 스택의 항목(링크/이미지가 될 수 있는 여는 대괄호). */
    private static final class Bracket {
        /** The text node of the opening bracket. 여는 대괄호의 텍스트 노드. */
        Node node;

        /** Previous entry. 이전 항목. */
        Bracket previous;

        /** Top of the delimiter stack when the bracket was pushed. 대괄호를 쌓을 때의 구분자 스택 꼭대기. */
        Delimiter previousDelimiter;

        /** Index of the opening bracket in the subject. 주제 문자열에서 여는 대괄호의 위치. */
        int index;

        /** True for an image opener. 이미지 여는 표식이면 true. */
        boolean image;

        /** False once a link was formed after this opener (links cannot contain links). 이 뒤에 링크가 만들어지면 false(링크 안에 링크 불가). */
        boolean active = true;

        /** True if another bracket was pushed after this one. 이 뒤에 다른 대괄호가 쌓였으면 true. */
        boolean bracketAfter;

        /** True once a deeper match failed because of the nesting limit: every bracket below fails the same way. 중첩 제한 때문에 더 안쪽 짝짓기가 실패한 뒤 true: 아래의 모든 대괄호도 같은 이유로 실패한다. */
        boolean dead;
    }

    /** Link reference definitions. 링크 참조 정의. */
    private final LinkRefs refs;

    /** Whether the GFM extensions are on. GFM 확장이 켜져 있는지 여부. */
    private final boolean gfm;

    /** Maximum inline nesting depth. 인라인 최대 중첩 깊이. */
    private final int maxInlineDepth;

    /** The text being parsed. 파싱 중인 텍스트. */
    private String subject;

    /** Length of the subject. 주제 문자열 길이. */
    private int len;

    /** Current position. 현재 위치. */
    private int pos;

    /** Top of the delimiter stack. 구분자 스택의 꼭대기. */
    private Delimiter delimiters;

    /** Top of the bracket stack. 대괄호 스택의 꼭대기. */
    private Bracket brackets;

    /** Backtick run lengths for which no closing run exists in the rest of the subject. 나머지에 닫는 백틱열이 없는 백틱열 길이들. */
    private final Set<Integer> failedTicks = new HashSet<Integer>();

    /** First position from which a search for the end of a comment failed. 주석 끝 탐색이 실패한 첫 위치. */
    private int commentFail;

    /** First position from which a search for the end of a processing instruction failed. 처리 명령 끝 탐색이 실패한 첫 위치. */
    private int piFail;

    /** First position from which a search for the end of a CDATA section failed. CDATA 끝 탐색이 실패한 첫 위치. */
    private int cdataFail;

    /** First position from which a search for the end of a declaration failed. 선언 끝 탐색이 실패한 첫 위치. */
    private int declFail;

    /**
     * Creates the parser.
     * <p>
     * 파서를 만든다.
     *
     * @param refs link reference definitions (filled by the block parser) / 링크 참조 정의(블록 파서가 채움)
     * @param opts options / 옵션
     */
    InlineParser(LinkRefs refs, MarkdownOptions opts) {
        this.refs = refs;
        this.gfm = opts.isGfm();
        this.maxInlineDepth = opts.getMaxInlineDepth();
    }

    /**
     * Parses the raw text of a block into inline children of the block.
     * <p>
     * 블록의 원본 텍스트를 블록의 인라인 자식 노드로 파싱한다.
     *
     * @param block the paragraph, heading or table cell / 문단, 제목, 표 셀
     * @param raw   its raw text / 원본 텍스트
     */
    void parse(Node block, String raw) {
        subject = MdText.trim(raw);
        len = subject.length();
        pos = 0;
        delimiters = null;
        brackets = null;
        failedTicks.clear();
        commentFail = Integer.MAX_VALUE;
        piFail = Integer.MAX_VALUE;
        cdataFail = Integer.MAX_VALUE;
        declFail = Integer.MAX_VALUE;
        // 텍스트 끝까지 인라인 요소를 하나씩 읽는다.
        while (pos < len) {
            parseInline(block);
        }
        processEmphasis(null);
        consolidateText(block);
        if (gfm) {
            GfmAutolink.apply(block);
        }
    }

    /**
     * Parses the link reference definition that starts at a position of a paragraph's raw text.
     * <p>
     * 문단의 원본 텍스트에서 한 위치에서 시작하는 링크 참조 정의를 파싱한다.
     *
     * @param s     the raw text / 원본 텍스트
     * @param start index of the opening bracket / 여는 대괄호의 위치
     * @return index after the definition (after its line end), or -1 if there is no definition / 정의(줄 끝 포함) 다음 위치. 정의가 아니면 -1
     */
    int parseReference(String s, int start) {
        subject = s;
        len = s.length();
        pos = start;
        int n = parseLinkLabel();
        if (n == 0) {
            return -1;
        }
        String rawLabel = s.substring(start, start + n);
        if (pos >= len || s.charAt(pos) != ':') {
            return -1;
        }
        pos++;
        spnl();
        String dest = parseLinkDestination();
        if (dest == null) {
            return -1;
        }
        int beforeTitle = pos;
        spnl();
        String title = null;
        if (pos != beforeTitle) {
            title = parseLinkTitle();
        }
        if (title == null) {
            title = "";
            pos = beforeTitle;
        }
        // 정의는 줄 끝에서 끝나야 한다. 제목이 줄 끝에서 끝나지 않으면 제목을 버리고 목적지가 줄 끝에서 끝나는지 본다.
        boolean atLineEnd = true;
        if (!skipSpaceAtEndOfLine()) {
            if (title.length() == 0) {
                atLineEnd = false;
            } else {
                title = "";
                pos = beforeTitle;
                atLineEnd = skipSpaceAtEndOfLine();
            }
        }
        if (!atLineEnd) {
            return -1;
        }
        String normalized = MdText.normalizeReference(rawLabel.substring(1, rawLabel.length() - 1));
        if (normalized.length() == 0) {
            return -1;
        }
        refs.add(normalized, dest, title);
        return pos;
    }

    /**
     * Skips spaces and then a line end or the end of the text.
     * <p>
     * 공백을 건너뛴 뒤 줄 끝 또는 텍스트 끝이 있는지 확인한다.
     *
     * @return true if a line end or the end follows (and it was consumed) / 줄 끝이나 끝이 이어지면(그리고 소비했으면) true
     */
    private boolean skipSpaceAtEndOfLine() {
        int p = pos;
        // 줄 끝 앞의 공백을 건너뛴다.
        while (p < len && subject.charAt(p) == ' ') {
            p++;
        }
        if (p >= len) {
            pos = p;
            return true;
        }
        if (subject.charAt(p) == '\n') {
            pos = p + 1;
            return true;
        }
        return false;
    }

    /**
     * Reads one inline element (or one character that starts no element) at the current position.
     * <p>
     * 현재 위치에서 인라인 요소 하나(또는 요소를 시작하지 않는 문자 하나)를 읽는다.
     *
     * @param block the block that receives the nodes / 노드를 받을 블록
     */
    private void parseInline(Node block) {
        char c = subject.charAt(pos);
        boolean res;
        switch (c) {
            case '\n':
                res = parseNewline(block);
                break;
            case '\\':
                res = parseBackslash(block);
                break;
            case '`':
                res = parseBackticks(block);
                break;
            case '*':
            case '_':
                res = handleDelim(c, block);
                break;
            case '~':
                res = gfm && handleDelim(c, block);
                break;
            case '[':
                res = parseOpenBracket(block);
                break;
            case '!':
                res = parseBang(block);
                break;
            case ']':
                res = parseCloseBracket(block);
                break;
            case '<':
                res = parseAutolink(block) || parseHtmlTag(block);
                break;
            case '&':
                res = parseEntity(block);
                break;
            default:
                res = parseString(block);
                break;
        }
        if (!res) {
            pos++;
            block.appendChild(Node.text(String.valueOf(c)));
        }
    }

    /**
     * Tells whether a character can start an inline element other than plain text.
     * <p>
     * 문자가 일반 텍스트가 아닌 인라인 요소를 시작할 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true for special characters / 특수 문자이면 true
     */
    private boolean isSpecial(char c) {
        switch (c) {
            case '\n':
            case '`':
            case '[':
            case ']':
            case '\\':
            case '!':
            case '<':
            case '&':
            case '*':
            case '_':
                return true;
            case '~':
                return gfm;
            default:
                return false;
        }
    }

    /**
     * Reads a run of plain text.
     * <p>
     * 일반 텍스트 구간을 읽는다.
     *
     * @param block the block / 블록
     * @return true if text was read / 텍스트를 읽었으면 true
     */
    private boolean parseString(Node block) {
        int start = pos;
        // 특수 문자가 나오기 전까지가 일반 텍스트이다.
        while (pos < len && !isSpecial(subject.charAt(pos))) {
            pos++;
        }
        if (pos == start) {
            return false;
        }
        block.appendChild(Node.text(subject.substring(start, pos)));
        return true;
    }

    /**
     * Handles a line ending: a hard break after two or more spaces, otherwise a soft break; spaces at the start of the next line are skipped.
     * <p>
     * 줄 끝을 처리한다: 공백 두 개 이상 뒤이면 강제 줄바꿈, 아니면 부드러운 줄바꿈이며 다음 줄 앞의 공백은 건너뛴다.
     *
     * @param block the block / 블록
     * @return always true / 항상 true
     */
    private boolean parseNewline(Node block) {
        Node last = block.lastChild;
        pos++;
        if (last != null && last.type == NodeType.TEXT && last.literal.length() > 0 && last.literal.charAt(last.literal.length() - 1) == ' ') {
            boolean hard = last.literal.length() >= 2 && last.literal.charAt(last.literal.length() - 2) == ' ';
            int e = last.literal.length();
            // 줄 끝의 공백을 모두 지운다.
            while (e > 0 && last.literal.charAt(e - 1) == ' ') {
                e--;
            }
            last.literal = last.literal.substring(0, e);
            block.appendChild(new Node(hard ? NodeType.LINEBREAK : NodeType.SOFTBREAK));
        } else {
            block.appendChild(new Node(NodeType.SOFTBREAK));
        }
        // 다음 줄 앞의 공백을 건너뛴다.
        while (pos < len && subject.charAt(pos) == ' ') {
            pos++;
        }
        return true;
    }

    /**
     * Handles a backslash: an escaped punctuation character, a hard line break before a line end, or a literal backslash.
     * <p>
     * 백슬래시를 처리한다: 이스케이프된 구두점, 줄 끝 앞이면 강제 줄바꿈, 그 외에는 글자 그대로의 백슬래시.
     *
     * @param block the block / 블록
     * @return always true / 항상 true
     */
    private boolean parseBackslash(Node block) {
        pos++;
        if (pos < len && subject.charAt(pos) == '\n') {
            pos++;
            block.appendChild(new Node(NodeType.LINEBREAK));
        } else if (pos < len && MdText.isAsciiPunctuation(subject.charAt(pos))) {
            block.appendChild(Node.text(String.valueOf(subject.charAt(pos))));
            pos++;
        } else {
            block.appendChild(Node.text("\\"));
        }
        return true;
    }

    /**
     * Counts the repeated occurrences of a character from a position.
     * <p>
     * 위치부터 같은 문자가 반복되는 횟수를 센다.
     *
     * @param c the character / 문자
     * @param p start index / 시작 위치
     * @return run length / 연속 길이
     */
    private int runLength(char c, int p) {
        int q = p;
        // 같은 문자가 이어지는 동안 센다.
        while (q < len && subject.charAt(q) == c) {
            q++;
        }
        return q - p;
    }

    /**
     * Handles a backtick run: a code span if a closing run of the same length follows, otherwise literal backticks. A failed search is
     * remembered per run length so that many unmatched backtick runs cost linear time in total.
     * <p>
     * 백틱 열을 처리한다: 같은 길이의 닫는 열이 뒤에 있으면 코드 스팬, 없으면 글자 그대로의 백틱. 실패한 탐색은 열 길이별로 기억하므로 짝 없는
     * 백틱 열이 많아도 전체 비용이 선형이다.
     *
     * @param block the block / 블록
     * @return always true / 항상 true
     */
    private boolean parseBackticks(Node block) {
        int start = pos;
        int n = runLength('`', pos);
        pos += n;
        int afterOpen = pos;
        if (!failedTicks.contains(Integer.valueOf(n))) {
            int p = afterOpen;
            // 같은 길이의 닫는 백틱 열을 찾는다.
            while (p < len) {
                int q = subject.indexOf('`', p);
                if (q < 0) {
                    break;
                }
                int m = runLength('`', q);
                if (m == n) {
                    String contents = subject.substring(afterOpen, q).replace('\n', ' ');
                    if (contents.length() > 0 && contents.charAt(0) == ' ' && contents.charAt(contents.length() - 1) == ' '
                            && hasNonSpace(contents)) {
                        contents = contents.substring(1, contents.length() - 1);
                    }
                    Node code = new Node(NodeType.CODE);
                    code.literal = contents;
                    block.appendChild(code);
                    pos = q + m;
                    return true;
                }
                p = q + m;
            }
            failedTicks.add(Integer.valueOf(n));
        }
        pos = afterOpen;
        block.appendChild(Node.text(subject.substring(start, afterOpen)));
        return true;
    }

    /**
     * Tells whether a string has a character other than a space.
     * <p>
     * 문자열에 공백이 아닌 문자가 있는지 알려 준다.
     *
     * @param s the string / 문자열
     * @return true if it has one / 있으면 true
     */
    private static boolean hasNonSpace(String s) {
        // 공백이 아닌 문자를 하나라도 찾으면 true 이다.
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != ' ') {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles an emphasis (or strikethrough) delimiter run: determines whether it can open and close and pushes it on the delimiter stack.
     * <p>
     * 강조(또는 취소선) 구분자 열을 처리한다: 열 수 있는지 닫을 수 있는지 판정하고 구분자 스택에 쌓는다.
     *
     * @param cc    the delimiter character / 구분자 문자
     * @param block the block / 블록
     * @return true if the run was consumed / 열을 소비했으면 true
     */
    private boolean handleDelim(char cc, Node block) {
        int startpos = pos;
        int num = runLength(cc, pos);
        if (num == 0) {
            return false;
        }
        int before = startpos == 0 ? '\n' : subject.codePointBefore(startpos);
        int afterPos = startpos + num;
        int after = afterPos >= len ? '\n' : subject.codePointAt(afterPos);
        boolean afterWs = MdText.isUnicodeWhitespace(after);
        boolean afterPunct = MdText.isPunctuation(after);
        boolean beforeWs = MdText.isUnicodeWhitespace(before);
        boolean beforePunct = MdText.isPunctuation(before);
        boolean leftFlanking = !afterWs && (!afterPunct || beforeWs || beforePunct);
        boolean rightFlanking = !beforeWs && (!beforePunct || afterWs || afterPunct);
        boolean canOpen;
        boolean canClose;
        if (cc == '_') {
            canOpen = leftFlanking && (!rightFlanking || beforePunct);
            canClose = rightFlanking && (!leftFlanking || afterPunct);
        } else {
            canOpen = leftFlanking;
            canClose = rightFlanking;
        }
        pos = afterPos;
        Node node = Node.text(subject.substring(startpos, pos));
        block.appendChild(node);
        // 취소선은 정확히 물결표 두 개일 때만 구분자가 된다.
        if ((canOpen || canClose) && (cc != '~' || num == 2)) {
            Delimiter d = new Delimiter();
            d.cc = cc;
            d.numdelims = num;
            d.origdelims = num;
            d.node = node;
            d.previous = delimiters;
            d.canOpen = canOpen;
            d.canClose = canClose;
            if (delimiters != null) {
                delimiters.next = d;
            }
            delimiters = d;
        }
        return true;
    }

    /**
     * Removes a delimiter from the stack.
     * <p>
     * 구분자를 스택에서 제거한다.
     *
     * @param d the delimiter / 구분자
     */
    private void removeDelimiter(Delimiter d) {
        if (d.previous != null) {
            d.previous.next = d.next;
        }
        if (d.next == null) {
            delimiters = d.previous;
        } else {
            d.next.previous = d.previous;
        }
    }

    /**
     * Returns the index into the "openers bottom" table for a closer.
     * <p>
     * 닫는 구분자에 대한 "여는 구분자 하한" 표의 색인을 반환한다.
     *
     * @param closer the closing delimiter / 닫는 구분자
     * @return index / 색인
     */
    private static int bottomKey(Delimiter closer) {
        int c = closer.cc == '*' ? 0 : closer.cc == '_' ? 1 : 2;
        return c * 6 + (closer.canOpen ? 3 : 0) + closer.origdelims % 3;
    }

    /**
     * Matches the closing delimiters above a stack position with their openers and builds the emphasis, strong and strikethrough nodes
     * (the "process emphasis" procedure of the specification). The table of lower bounds keeps the work linear.
     * <p>
     * 스택 위치 위의 닫는 구분자를 여는 구분자와 짝지어 강조, 센 강조, 취소선 노드를 만든다(규격의 "process emphasis" 절차). 하한 표 덕분에
     * 작업량이 선형으로 유지된다.
     *
     * @param stackBottom the entry below the first one to process, or null for all / 처리할 첫 항목 아래의 항목. 전체이면 null
     */
    private void processEmphasis(Delimiter stackBottom) {
        Delimiter[] openersBottom = new Delimiter[18];
        // 하한 표를 스택 바닥으로 초기화한다.
        for (int i = 0; i < openersBottom.length; i++) {
            openersBottom[i] = stackBottom;
        }
        Delimiter closer = delimiters;
        // 바닥 위의 첫 닫는 구분자 후보를 찾는다.
        while (closer != null && closer.previous != stackBottom) {
            closer = closer.previous;
        }
        // 위로 올라가며 닫는 구분자마다 짝이 되는 여는 구분자를 찾는다.
        while (closer != null) {
            if (!closer.canClose) {
                closer = closer.next;
                continue;
            }
            int key = bottomKey(closer);
            Delimiter opener = closer.previous;
            boolean found = false;
            // 아래로 내려가며 짝이 되는 여는 구분자를 찾는다.
            while (opener != null && opener != stackBottom && opener != openersBottom[key]) {
                boolean oddMatch = false;
                if (closer.cc != '~') {
                    oddMatch = (closer.canOpen || opener.canClose) && closer.origdelims % 3 != 0
                            && (opener.origdelims + closer.origdelims) % 3 == 0;
                }
                if (opener.cc == closer.cc && opener.canOpen && !oddMatch && (closer.cc != '~' || opener.numdelims == closer.numdelims)) {
                    found = true;
                    break;
                }
                opener = opener.previous;
            }
            Delimiter oldCloser = closer;
            if (!found) {
                closer = closer.next;
                openersBottom[key] = oldCloser.previous;
                if (!oldCloser.canOpen) {
                    removeDelimiter(oldCloser);
                }
                continue;
            }
            Node openerInl = opener.node;
            Node closerInl = closer.node;
            int maxHeight = 0;
            // 사이에 들어갈 노드들의 최대 높이를 구해 중첩 제한을 확인한다.
            for (Node t = openerInl.next; t != null && t != closerInl; t = t.next) {
                maxHeight = Math.max(maxHeight, t.height);
            }
            if (maxHeight + 1 > maxInlineDepth) {
                // 너무 깊게 중첩된다. 이 여는 구분자와 그 아래의 구분자는 모두 이 깊은 노드를 포함하게 되어 더 이상 짝지을 수 없으므로
                // 스택에서 빼 글자 그대로 둔다(같은 열을 되풀이해 훑지 않게 한다).
                Delimiter d = opener;
                // 여는 구분자부터 스택 바닥 위까지 모두 뺀다.
                while (d != null && d != stackBottom) {
                    Delimiter below = d.previous;
                    removeDelimiter(d);
                    d = below;
                }
                Delimiter dead = closer;
                closer = closer.next;
                if (dead.canOpen) {
                    dead.canClose = false;
                } else {
                    removeDelimiter(dead);
                }
                continue;
            }
            int use = closer.numdelims >= 2 && opener.numdelims >= 2 ? 2 : 1;
            opener.numdelims -= use;
            closer.numdelims -= use;
            openerInl.literal = openerInl.literal.substring(0, openerInl.literal.length() - use);
            closerInl.literal = closerInl.literal.substring(0, closerInl.literal.length() - use);
            NodeType t = closer.cc == '~' ? NodeType.DEL : use == 1 ? NodeType.EMPH : NodeType.STRONG;
            Node emph = new Node(t);
            emph.height = maxHeight + 1;
            Node tmp = openerInl.next;
            // 여는/닫는 구분자 사이의 노드를 새 노드의 자식으로 옮긴다.
            while (tmp != null && tmp != closerInl) {
                Node nx = tmp.next;
                tmp.unlink();
                emph.appendChild(tmp);
                tmp = nx;
            }
            openerInl.insertAfter(emph);
            // 열고 닫는 구분자 사이의 항목은 스택에서 뺀다.
            if (opener.next != closer) {
                opener.next = closer;
                closer.previous = opener;
            }
            if (opener.numdelims == 0) {
                openerInl.unlink();
                removeDelimiter(opener);
            }
            if (closer.numdelims == 0) {
                closerInl.unlink();
                Delimiter nextCloser = closer.next;
                removeDelimiter(closer);
                closer = nextCloser;
            }
        }
        // 남은 구분자는 모두 글자 그대로 둔다.
        while (delimiters != null && delimiters != stackBottom) {
            removeDelimiter(delimiters);
        }
    }

    /**
     * Pushes a bracket on the bracket stack.
     * <p>
     * 대괄호 스택에 대괄호를 쌓는다.
     *
     * @param node  the text node of the bracket / 대괄호의 텍스트 노드
     * @param index index of the opening bracket / 여는 대괄호의 위치
     * @param image true for an image opener / 이미지이면 true
     */
    private void addBracket(Node node, int index, boolean image) {
        if (brackets != null) {
            brackets.bracketAfter = true;
        }
        Bracket b = new Bracket();
        b.node = node;
        b.previous = brackets;
        b.previousDelimiter = delimiters;
        b.index = index;
        b.image = image;
        brackets = b;
    }

    /**
     * Pops the top of the bracket stack.
     * <p>
     * 대괄호 스택의 꼭대기를 뺀다.
     */
    private void removeBracket() {
        brackets = brackets.previous;
    }

    /**
     * Handles an opening bracket.
     * <p>
     * 여는 대괄호를 처리한다.
     *
     * @param block the block / 블록
     * @return always true / 항상 true
     */
    private boolean parseOpenBracket(Node block) {
        int startpos = pos;
        pos++;
        Node node = Node.text("[");
        block.appendChild(node);
        addBracket(node, startpos, false);
        return true;
    }

    /**
     * Handles an exclamation mark: the start of an image if a bracket follows.
     * <p>
     * 느낌표를 처리한다: 대괄호가 뒤따르면 이미지의 시작이다.
     *
     * @param block the block / 블록
     * @return always true / 항상 true
     */
    private boolean parseBang(Node block) {
        int startpos = pos;
        pos++;
        if (pos < len && subject.charAt(pos) == '[') {
            pos++;
            Node node = Node.text("![");
            block.appendChild(node);
            addBracket(node, startpos + 1, true);
        } else {
            block.appendChild(Node.text("!"));
        }
        return true;
    }

    /**
     * Handles a closing bracket: forms an inline link or image, a reference link or image, or leaves a literal bracket.
     * <p>
     * 닫는 대괄호를 처리한다: 인라인 링크/이미지나 참조 링크/이미지를 만들거나 글자 그대로의 대괄호로 남긴다.
     *
     * @param block the block / 블록
     * @return always true / 항상 true
     */
    private boolean parseCloseBracket(Node block) {
        pos++;
        int startpos = pos;
        Bracket opener = brackets;
        if (opener == null) {
            block.appendChild(Node.text("]"));
            return true;
        }
        if (!opener.active || opener.dead) {
            block.appendChild(Node.text("]"));
            removeBracket();
            return true;
        }
        boolean isImage = opener.image;
        int savepos = pos;
        boolean matched = false;
        String dest = null;
        String title = null;
        // 인라인 링크 (목적지 "제목") 형태를 먼저 시도한다.
        if (pos < len && subject.charAt(pos) == '(') {
            pos++;
            spnl();
            dest = parseLinkDestination();
            if (dest != null) {
                spnl();
                if (MdText.isAsciiWhitespace(subject.charAt(pos - 1))) {
                    title = parseLinkTitle();
                }
                spnl();
                if (pos < len && subject.charAt(pos) == ')') {
                    pos++;
                    matched = true;
                }
            }
            if (!matched) {
                pos = savepos;
                dest = null;
                title = null;
            }
        }
        // 인라인 링크가 아니면 참조 링크를 시도한다.
        if (!matched) {
            int beforeLabel = pos;
            int n = parseLinkLabel();
            String refLabel = null;
            if (n > 2) {
                refLabel = subject.substring(beforeLabel, beforeLabel + n);
            } else if (!opener.bracketAfter) {
                refLabel = subject.substring(opener.index, startpos);
            }
            if (n == 0) {
                pos = savepos;
            }
            if (refLabel != null) {
                LinkRefs.Ref r = refs.get(MdText.normalizeReference(refLabel.substring(1, refLabel.length() - 1)));
                if (r != null) {
                    dest = r.destination;
                    title = r.title;
                    matched = true;
                }
            }
        }
        if (matched) {
            int maxHeight = 0;
            // 링크 텍스트가 될 노드들의 최대 높이를 구해 중첩 제한을 확인한다.
            for (Node t = opener.node.next; t != null; t = t.next) {
                maxHeight = Math.max(maxHeight, t.height);
            }
            if (maxHeight + 1 > maxInlineDepth) {
                removeBracket();
                // 바깥의 모든 대괄호도 이 깊은 노드를 포함하므로 같은 이유로 실패한다. 한 번에 표시해 두어 되풀이해 훑지 않는다.
                Bracket b = brackets;
                while (b != null && !b.dead) {
                    b.dead = true;
                    b = b.previous;
                }
                pos = startpos;
                block.appendChild(Node.text("]"));
                return true;
            }
            Node node = new Node(isImage ? NodeType.IMAGE : NodeType.LINK);
            node.destination = dest;
            node.title = title == null ? "" : title;
            node.height = maxHeight + 1;
            Node tmp = opener.node.next;
            // 여는 대괄호 뒤의 모든 노드를 링크의 자식으로 옮긴다.
            while (tmp != null) {
                Node nx = tmp.next;
                tmp.unlink();
                node.appendChild(tmp);
                tmp = nx;
            }
            block.appendChild(node);
            processEmphasis(opener.previousDelimiter);
            removeBracket();
            opener.node.unlink();
            // 링크 안에 링크는 만들 수 없으므로 앞의 링크 여는 대괄호를 모두 비활성화한다.
            if (!isImage) {
                Bracket b = brackets;
                // 스택의 모든 링크 여는 대괄호를 비활성화한다.
                while (b != null) {
                    if (!b.image) {
                        b.active = false;
                    }
                    b = b.previous;
                }
            }
            return true;
        }
        removeBracket();
        pos = startpos;
        block.appendChild(Node.text("]"));
        return true;
    }

    /**
     * Skips spaces and at most one line ending followed by spaces.
     * <p>
     * 공백과 줄 끝 하나(와 뒤따르는 공백)를 건너뛴다.
     */
    private void spnl() {
        // 줄 끝 앞의 공백을 건너뛴다.
        while (pos < len && subject.charAt(pos) == ' ') {
            pos++;
        }
        if (pos < len && subject.charAt(pos) == '\n') {
            pos++;
            // 다음 줄 앞의 공백을 건너뛴다.
            while (pos < len && subject.charAt(pos) == ' ') {
                pos++;
            }
        }
    }

    /**
     * Reads a link label <code>[...]</code> at the current position (at most 999 characters, no unescaped brackets inside).
     * <p>
     * 현재 위치의 링크 라벨 <code>[...]</code> 를 읽는다(최대 999자, 이스케이프되지 않은 대괄호 불가).
     *
     * @return the length including the brackets and the position is advanced past it, or 0 (position unchanged) / 대괄호를 포함한 길이(위치도 그만큼 이동). 실패하면 0(위치 그대로)
     */
    private int parseLinkLabel() {
        if (pos >= len || subject.charAt(pos) != '[') {
            return 0;
        }
        int p = pos + 1;
        int count = 0;
        // 닫는 대괄호가 나올 때까지 읽되 이스케이프된 문자는 한 쌍으로 센다.
        while (p < len) {
            char c = subject.charAt(p);
            if (c == '\\') {
                p += p + 1 < len ? 2 : 1;
                count += 2;
            } else if (c == '[') {
                return 0;
            } else if (c == ']') {
                int n = p + 1 - pos;
                pos = p + 1;
                return n;
            } else {
                p++;
                count++;
            }
            if (count > MAX_LABEL_CHARS) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * Reads a link destination (angle-bracketed or bare with balanced parentheses) and returns it unescaped and URL-normalized.
     * Nesting of parentheses in a bare destination is limited to 32 levels so that a long run of opening parentheses cannot make every
     * bracket scan to the end of the input.
     * <p>
     * 링크 목적지(꺾쇠로 감싼 것 또는 괄호 짝이 맞는 맨 문자열)를 읽어 이스케이프를 풀고 URL 정규화하여 돌려준다. 맨 목적지의 괄호 중첩은 32단계로
     * 제한하여 여는 괄호가 길게 이어져도 모든 대괄호가 입력 끝까지 탐색하지 않도록 한다.
     *
     * @return the destination (possibly empty) or null if there is none / 목적지(비어 있을 수 있음). 없으면 null
     */
    private String parseLinkDestination() {
        if (pos < len && subject.charAt(pos) == '<') {
            int p = pos + 1;
            // 꺾쇠 안에서는 줄바꿈과 이스케이프되지 않은 꺾쇠가 올 수 없다.
            while (p < len) {
                char c = subject.charAt(p);
                if (c == '\\' && p + 1 < len && subject.charAt(p + 1) != '\n') {
                    p += 2;
                } else if (c == '>') {
                    String raw = subject.substring(pos + 1, p);
                    pos = p + 1;
                    return MdText.normalizeUri(MdText.unescapeString(raw));
                } else if (c == '\n' || c == '<' || c == (char) 0) {
                    return null;
                } else {
                    p++;
                }
            }
            return null;
        }
        int start = pos;
        int open = 0;
        int p = pos;
        // 공백, 제어 문자, 짝이 맞지 않는 닫는 괄호가 나올 때까지 읽는다.
        while (p < len) {
            char c = subject.charAt(p);
            if (c == '\\' && p + 1 < len && MdText.isAsciiPunctuation(subject.charAt(p + 1))) {
                p += 2;
            } else if (c == '(') {
                open++;
                if (open > MAX_PAREN_DEPTH) {
                    return null;
                }
                p++;
            } else if (c == ')') {
                if (open < 1) {
                    break;
                }
                open--;
                p++;
            } else if (c <= ' ' || c == 0x7F) {
                break;
            } else {
                p++;
            }
        }
        if (p == start && !(p < len && subject.charAt(p) == ')')) {
            return null;
        }
        if (open != 0) {
            return null;
        }
        pos = p;
        return MdText.normalizeUri(MdText.unescapeString(subject.substring(start, p)));
    }

    /**
     * Reads a link title in double quotes, single quotes or parentheses and returns it unescaped; the position stays if there is none.
     * <p>
     * 큰따옴표, 작은따옴표, 괄호로 감싼 링크 제목을 읽어 이스케이프를 풀어 돌려준다. 없으면 위치는 그대로이다.
     *
     * @return the title or null / 제목 또는 null
     */
    private String parseLinkTitle() {
        if (pos >= len) {
            return null;
        }
        char open = subject.charAt(pos);
        char close;
        if (open == '"' || open == '\'') {
            close = open;
        } else if (open == '(') {
            close = ')';
        } else {
            return null;
        }
        int p = pos + 1;
        // 닫는 따옴표(또는 괄호)가 나올 때까지 읽는다. 괄호 형태에는 이스케이프되지 않은 여는 괄호가 올 수 없다.
        while (p < len) {
            char c = subject.charAt(p);
            if (c == '\\' && p + 1 < len && MdText.isAsciiPunctuation(subject.charAt(p + 1))) {
                p += 2;
            } else if (c == close) {
                String raw = subject.substring(pos + 1, p);
                pos = p + 1;
                return MdText.unescapeString(raw);
            } else if (open == '(' && c == '(') {
                return null;
            } else {
                p++;
            }
        }
        return null;
    }

    /**
     * Handles <code>&lt;</code> followed by an absolute URI or an email address and <code>&gt;</code>.
     * <p>
     * <code>&lt;</code> 뒤에 절대 URI 나 이메일 주소와 <code>&gt;</code> 가 오는 경우를 처리한다.
     *
     * @param block the block / 블록
     * @return true if an autolink was read / 자동 링크를 읽었으면 true
     */
    private boolean parseAutolink(Node block) {
        int end = matchEmailAutolink(pos);
        boolean email = end >= 0;
        if (!email) {
            end = matchUriAutolink(pos);
        }
        if (end < 0) {
            return false;
        }
        String text = subject.substring(pos + 1, end - 1);
        Node link = new Node(NodeType.LINK);
        link.destination = MdText.normalizeUri(email ? "mailto:" + text : text);
        link.title = "";
        link.height = 1;
        link.appendChild(Node.text(text));
        block.appendChild(link);
        pos = end;
        return true;
    }

    /**
     * Matches <code>&lt;scheme:...&gt;</code> at a position.
     * <p>
     * 위치에서 <code>&lt;scheme:...&gt;</code> 를 찾는다.
     *
     * @param start index of the less-than sign / 꺾쇠 여는 기호의 위치
     * @return index after the closing sign or -1 / 닫는 기호 다음 위치 또는 -1
     */
    private int matchUriAutolink(int start) {
        int p = start + 1;
        if (p >= len || !MdText.isAsciiLetter(subject.charAt(p))) {
            return -1;
        }
        int schemeStart = p;
        p++;
        // 스킴은 영문자로 시작하고 영숫자, +, ., - 가 이어지며 전체 2~32자이다.
        while (p < len && p - schemeStart < 33 && (MdText.isAsciiAlnum(subject.charAt(p)) || "+.-".indexOf(subject.charAt(p)) >= 0)) {
            p++;
        }
        int schemeLen = p - schemeStart;
        if (schemeLen < 2 || schemeLen > 32 || p >= len || subject.charAt(p) != ':') {
            return -1;
        }
        p++;
        // 공백, 제어 문자, 꺾쇠가 나오기 전까지가 주소이다.
        while (p < len) {
            char c = subject.charAt(p);
            if (c == '>') {
                return p + 1;
            }
            if (c <= ' ' || c == '<') {
                return -1;
            }
            p++;
        }
        return -1;
    }

    /**
     * Matches <code>&lt;local@domain&gt;</code> at a position.
     * <p>
     * 위치에서 <code>&lt;local@domain&gt;</code> 를 찾는다.
     *
     * @param start index of the less-than sign / 꺾쇠 여는 기호의 위치
     * @return index after the closing sign or -1 / 닫는 기호 다음 위치 또는 -1
     */
    private int matchEmailAutolink(int start) {
        int p = start + 1;
        int localStart = p;
        // 로컬 부분에 쓸 수 있는 문자가 이어지는 동안 읽는다.
        while (p < len && isEmailLocalChar(subject.charAt(p))) {
            p++;
        }
        if (p == localStart || p >= len || subject.charAt(p) != '@') {
            return -1;
        }
        p++;
        // 도메인은 점으로 구분된 레이블들이다.
        while (true) {
            int labelStart = p;
            // 레이블은 영숫자로 시작하고 끝나며 가운데에는 하이픈이 올 수 있다(최대 63자).
            while (p < len && (MdText.isAsciiAlnum(subject.charAt(p)) || subject.charAt(p) == '-')) {
                p++;
            }
            int labelLen = p - labelStart;
            if (labelLen == 0 || labelLen > 63 || subject.charAt(labelStart) == '-' || subject.charAt(p - 1) == '-') {
                return -1;
            }
            if (p < len && subject.charAt(p) == '.') {
                p++;
            } else {
                break;
            }
        }
        return p < len && subject.charAt(p) == '>' ? p + 1 : -1;
    }

    /**
     * Tells whether a character is allowed in the local part of an email autolink.
     * <p>
     * 문자가 이메일 자동 링크의 로컬 부분에 쓸 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true if allowed / 허용되면 true
     */
    private static boolean isEmailLocalChar(char c) {
        return MdText.isAsciiAlnum(c) || ".!#$%&'*+/=?^_`{|}~-".indexOf(c) >= 0;
    }

    /**
     * Handles an inline raw HTML construct (open tag, closing tag, comment, processing instruction, declaration, CDATA section).
     * Searches for the end of the unbounded constructs are cached by start position so that many unterminated openers cost linear time.
     * <p>
     * 인라인 원시 HTML 구성 요소(여는 태그, 닫는 태그, 주석, 처리 명령, 선언, CDATA 섹션)를 처리한다. 끝이 정해지지 않은 구성 요소의 끝 탐색은
     * 시작 위치별로 캐시하므로 끝나지 않은 시작이 많아도 선형 시간이 든다.
     *
     * @param block the block / 블록
     * @return true if HTML was read / HTML 을 읽었으면 true
     */
    private boolean parseHtmlTag(Node block) {
        int end = -1;
        if (subject.startsWith("<!--", pos)) {
            if (subject.startsWith("<!-->", pos)) {
                end = pos + 5;
            } else if (subject.startsWith("<!--->", pos)) {
                end = pos + 6;
            } else if (pos < commentFail) {
                int e = subject.indexOf("-->", pos + 4);
                if (e >= 0) {
                    end = e + 3;
                } else {
                    commentFail = pos;
                }
            }
        } else if (subject.startsWith("<?", pos)) {
            if (pos < piFail) {
                int e = subject.indexOf("?>", pos + 2);
                if (e >= 0) {
                    end = e + 2;
                } else {
                    piFail = pos;
                }
            }
        } else if (subject.startsWith("<![CDATA[", pos)) {
            if (pos < cdataFail) {
                int e = subject.indexOf("]]>", pos + 9);
                if (e >= 0) {
                    end = e + 3;
                } else {
                    cdataFail = pos;
                }
            }
        } else if (subject.startsWith("<!", pos) && pos + 2 < len && MdText.isAsciiLetter(subject.charAt(pos + 2))) {
            if (pos < declFail) {
                int e = subject.indexOf('>', pos + 3);
                if (e >= 0) {
                    end = e + 1;
                } else {
                    declFail = pos;
                }
            }
        } else {
            end = HtmlTags.matchTag(subject, pos);
        }
        if (end < 0) {
            return false;
        }
        Node n = new Node(NodeType.HTML_INLINE);
        n.literal = subject.substring(pos, end);
        block.appendChild(n);
        pos = end;
        return true;
    }

    /**
     * Handles a character reference.
     * <p>
     * 문자 참조를 처리한다.
     *
     * @param block the block / 블록
     * @return true if a well-formed reference was read / 올바른 참조를 읽었으면 true
     */
    private boolean parseEntity(Node block) {
        int end = MdText.matchEntity(subject, pos);
        if (end < 0) {
            return false;
        }
        block.appendChild(Node.text(MdText.decodeEntity(subject.substring(pos, end))));
        pos = end;
        return true;
    }

    /**
     * Merges adjacent text children of a block into single text nodes.
     * <p>
     * 블록의 이웃한 텍스트 자식 노드를 하나의 텍스트 노드로 합친다.
     *
     * @param block the block / 블록
     */
    private void consolidateText(Node block) {
        mergeChildren(block);
        Node cur = block.firstChild;
        // 강조/링크 같은 컨테이너 노드의 자식도 같은 방식으로 합친다(재귀 없는 전위 순회).
        while (cur != null) {
            if (cur.firstChild != null) {
                mergeChildren(cur);
                cur = cur.firstChild;
            } else {
                // 다음 형제가 없으면 부모로 올라간다.
                while (cur != null && cur != block && cur.next == null) {
                    cur = cur.parent;
                }
                if (cur == null || cur == block) {
                    break;
                }
                cur = cur.next;
            }
        }
    }

    /**
     * Merges adjacent text children of one node.
     * <p>
     * 노드 하나의 이웃한 텍스트 자식을 합친다.
     *
     * @param parent the parent node / 부모 노드
     */
    private void mergeChildren(Node parent) {
        Node n = parent.firstChild;
        // 이웃한 텍스트 자식을 하나로 합친다.
        while (n != null) {
            if (n.type == NodeType.TEXT && n.next != null && n.next.type == NodeType.TEXT) {
                StringBuilder sb = new StringBuilder(n.literal);
                Node nx = n.next;
                // 이어지는 텍스트 노드를 모두 합친다.
                while (nx != null && nx.type == NodeType.TEXT) {
                    sb.append(nx.literal);
                    Node after = nx.next;
                    nx.unlink();
                    nx = after;
                }
                n.literal = sb.toString();
            }
            n = n.next;
        }
    }
}
