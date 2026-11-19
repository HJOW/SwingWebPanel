package org.duckdns.hjow.swingwebpanel.content.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * The extended autolinks of GitHub Flavored Markdown: <code>www.</code> addresses, <code>http://</code>, <code>https://</code> and
 * <code>ftp://</code> addresses and email addresses that are recognized in plain text without angle brackets. The step runs over the text
 * nodes of one block after the inline parse, skips text that is already inside a link or image, and splits a text node into text and
 * link nodes where addresses are found. Trailing punctuation, unbalanced closing parentheses and entity-like endings are not part of a link.
 * <p>
 * GitHub Flavored Markdown 의 확장 자동 링크: 꺾쇠 없이 일반 텍스트에서 인식하는 <code>www.</code> 주소, <code>http://</code>,
 * <code>https://</code>, <code>ftp://</code> 주소, 이메일 주소. 이 단계는 인라인 파싱 뒤 블록 하나의 텍스트 노드를 훑으며, 이미 링크나 이미지
 * 안에 있는 텍스트는 건너뛰고, 주소를 찾으면 텍스트 노드를 텍스트와 링크 노드로 나눈다. 끝의 구두점, 짝이 맞지 않는 닫는 괄호, 엔티티처럼 보이는
 * 끝은 링크에 포함하지 않는다.
 */
final class GfmAutolink {

    /** Private constructor: static helper class. 비공개 생성자: 정적 도우미 클래스이다. */
    private GfmAutolink() {
    }

    /**
     * Applies the extended autolinks to the text nodes of a block.
     * <p>
     * 블록의 텍스트 노드에 확장 자동 링크를 적용한다.
     *
     * @param block the paragraph, heading or table cell / 문단, 제목, 표 셀
     */
    static void apply(Node block) {
        List<Node> texts = new ArrayList<Node>();
        Node cur = block.firstChild;
        // 링크와 이미지 안쪽은 건너뛰며 텍스트 노드를 모은다(재귀 없는 전위 순회).
        while (cur != null) {
            if (cur.type == NodeType.TEXT) {
                if (mayContainAddress(cur.literal)) {
                    texts.add(cur);
                }
            }
            boolean skip = cur.type == NodeType.LINK || cur.type == NodeType.IMAGE;
            if (cur.firstChild != null && !skip) {
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
        // 모은 텍스트 노드를 하나씩 분해한다.
        for (int i = 0; i < texts.size(); i++) {
            split(texts.get(i));
        }
    }

    /**
     * Quick test that skips text that cannot contain an address.
     * <p>
     * 주소를 담을 수 없는 텍스트를 빨리 걸러 내는 검사.
     *
     * @param s the text / 텍스트
     * @return true if an address may be present / 주소가 있을 수 있으면 true
     */
    private static boolean mayContainAddress(String s) {
        return s.indexOf('@') >= 0 || s.indexOf("www.") >= 0 || s.indexOf("://") >= 0;
    }

    /**
     * Splits one text node into text and link nodes.
     * <p>
     * 텍스트 노드 하나를 텍스트와 링크 노드로 나눈다.
     *
     * @param node the text node / 텍스트 노드
     */
    private static void split(Node node) {
        String s = node.literal;
        int n = s.length();
        List<Node> out = new ArrayList<Node>();
        int emitted = 0;
        int i = 0;
        boolean any = false;
        // 왼쪽에서 오른쪽으로 주소가 시작될 만한 위치를 찾는다.
        while (i < n) {
            int start = -1;
            int end = -1;
            String prefix = "";
            char c = s.charAt(i);
            if (c == 'w' && s.startsWith("www.", i) && (i == 0 || isBoundary(s.charAt(i - 1)))) {
                end = wwwEnd(s, i);
                start = i;
                prefix = "http://";
            } else if ((c == 'h' || c == 'H' || c == 'f' || c == 'F') && (i == 0 || !MdText.isAsciiLetter(s.charAt(i - 1)))) {
                int schemeEnd = schemeEnd(s, i);
                if (schemeEnd > 0) {
                    end = urlEnd(s, schemeEnd);
                    start = i;
                }
            } else if (c == '@') {
                int ls = i;
                // 로컬 부분은 영숫자와 . - _ + 이며 이미 처리한 위치 앞으로는 가지 않는다.
                while (ls > emitted && isLocalChar(s.charAt(ls - 1))) {
                    ls--;
                }
                if (ls < i) {
                    int e = emailEnd(s, i);
                    if (e > 0) {
                        start = ls;
                        end = e;
                        prefix = "mailto:";
                    }
                }
            }
            if (start >= 0 && end > start) {
                if (start > emitted) {
                    out.add(Node.text(s.substring(emitted, start)));
                }
                String text = s.substring(start, end);
                Node link = new Node(NodeType.LINK);
                link.destination = MdText.normalizeUri(prefix + text);
                link.title = "";
                link.height = 1;
                link.appendChild(Node.text(text));
                out.add(link);
                emitted = end;
                i = end;
                any = true;
            } else {
                i++;
            }
        }
        if (!any) {
            return;
        }
        if (emitted < n) {
            out.add(Node.text(s.substring(emitted)));
        }
        Node anchor = node;
        // 나눈 노드들을 원래 텍스트 노드 자리에 차례로 끼워 넣고 원래 노드를 지운다.
        for (int k = 0; k < out.size(); k++) {
            anchor.insertAfter(out.get(k));
            anchor = out.get(k);
        }
        node.unlink();
    }

    /**
     * Tells whether a character may precede a <code>www.</code> autolink.
     * <p>
     * 문자가 <code>www.</code> 자동 링크 앞에 올 수 있는지 알려 준다.
     *
     * @param c the character before / 앞의 문자
     * @return true for whitespace and the delimiters * _ ~ ( / 공백과 구분자 * _ ~ ( 이면 true
     */
    private static boolean isBoundary(char c) {
        return MdText.isAsciiWhitespace(c) || c == '*' || c == '_' || c == '~' || c == '(';
    }

    /**
     * Tells whether a character is allowed in the local part of an extended email autolink.
     * <p>
     * 문자가 확장 이메일 자동 링크의 로컬 부분에 쓸 수 있는지 알려 준다.
     *
     * @param c the character / 문자
     * @return true if allowed / 허용되면 true
     */
    private static boolean isLocalChar(char c) {
        return MdText.isAsciiAlnum(c) || c == '.' || c == '-' || c == '_' || c == '+';
    }

    /**
     * Returns the end of the scheme and "://" of an http, https or ftp address at a position, or -1.
     * <p>
     * 위치의 http, https, ftp 주소에서 스킴과 "://" 의 끝을 반환하며 아니면 -1 이다.
     *
     * @param s the text / 텍스트
     * @param i start index / 시작 위치
     * @return index after "://" or -1 / "://" 다음 위치 또는 -1
     */
    private static int schemeEnd(String s, int i) {
        if (s.regionMatches(true, i, "http://", 0, 7)) {
            return i + 7;
        }
        if (s.regionMatches(true, i, "https://", 0, 8)) {
            return i + 8;
        }
        if (s.regionMatches(true, i, "ftp://", 0, 6)) {
            return i + 6;
        }
        return -1;
    }

    /**
     * Checks the domain that starts at a position.
     * <p>
     * 위치에서 시작하는 도메인을 검사한다.
     *
     * @param s          the text / 텍스트
     * @param start      start index / 시작 위치
     * @param allowShort true if a domain without a period is allowed / 마침표 없는 도메인을 허용하면 true
     * @return index after the domain or -1 if invalid / 도메인 다음 위치. 올바르지 않으면 -1
     */
    private static int checkDomain(String s, int start, boolean allowShort) {
        int n = s.length();
        int i = start;
        int underscoresLast = 0;
        int underscoresPrev = 0;
        int periods = 0;
        // 영숫자, 밑줄, 하이픈, 마침표가 이어지는 동안 도메인이다. 마지막 두 구간에는 밑줄이 없어야 한다.
        while (i < n) {
            char c = s.charAt(i);
            if (c == '_') {
                underscoresLast++;
            } else if (c == '.') {
                underscoresPrev = underscoresLast;
                underscoresLast = 0;
                periods++;
            } else if (!MdText.isAsciiAlnum(c) && c != '-') {
                break;
            }
            i++;
        }
        if (underscoresPrev > 0 || underscoresLast > 0) {
            return -1;
        }
        if (i == start || (!allowShort && periods == 0)) {
            return -1;
        }
        return i;
    }

    /**
     * Returns the end of a <code>www.</code> autolink that starts at a position, or -1.
     * <p>
     * 위치에서 시작하는 <code>www.</code> 자동 링크의 끝을 반환하며 아니면 -1 이다.
     *
     * @param s     the text / 텍스트
     * @param start index of "www." / "www." 의 위치
     * @return end index or -1 / 끝 위치 또는 -1
     */
    private static int wwwEnd(String s, int start) {
        int d = checkDomain(s, start, false);
        if (d < 0 || d <= start + 4) {
            return -1;
        }
        int end = pathEnd(s, d);
        end = trimEnd(s, start, end);
        return end > start + 4 ? end : -1;
    }

    /**
     * Returns the end of an http, https or ftp autolink whose domain starts at a position, or -1.
     * <p>
     * 도메인이 위치에서 시작하는 http, https, ftp 자동 링크의 끝을 반환하며 아니면 -1 이다.
     *
     * @param s      the text / 텍스트
     * @param domain index after "://" / "://" 다음 위치
     * @return end index or -1 / 끝 위치 또는 -1
     */
    private static int urlEnd(String s, int domain) {
        int d = checkDomain(s, domain, true);
        if (d < 0) {
            return -1;
        }
        int end = pathEnd(s, d);
        end = trimEnd(s, domain, end);
        return end > domain ? end : -1;
    }

    /**
     * Extends an address over the characters after the domain: everything up to whitespace or a less-than sign.
     * <p>
     * 도메인 뒤의 문자로 주소를 늘린다: 공백이나 꺾쇠 여는 기호 전까지 모두.
     *
     * @param s the text / 텍스트
     * @param p index after the domain / 도메인 다음 위치
     * @return end index / 끝 위치
     */
    private static int pathEnd(String s, int p) {
        int n = s.length();
        // 공백이나 꺾쇠가 나오기 전까지 주소에 포함한다.
        while (p < n && !MdText.isAsciiWhitespace(s.charAt(p)) && s.charAt(p) != '<') {
            p++;
        }
        return p;
    }

    /**
     * Removes the trailing characters that do not belong to an address: punctuation, unbalanced closing parentheses and an entity-like
     * ending.
     * <p>
     * 주소에 속하지 않는 끝의 문자를 제거한다: 구두점, 짝이 맞지 않는 닫는 괄호, 엔티티처럼 보이는 끝.
     *
     * @param s     the text / 텍스트
     * @param start start of the address / 주소의 시작
     * @param end   tentative end / 임시 끝 위치
     * @return the final end / 최종 끝 위치
     */
    private static int trimEnd(String s, int start, int end) {
        // 끝에서부터 주소가 아닌 문자를 하나씩 떼어 낸다.
        while (end > start) {
            char c = s.charAt(end - 1);
            if ("?!.,:*_~".indexOf(c) >= 0) {
                end--;
            } else if (c == ';') {
                int p = end - 2;
                // 세미콜론 앞이 & 와 영숫자들이면 엔티티처럼 보이므로 통째로 뗀다.
                while (p >= start && MdText.isAsciiAlnum(s.charAt(p))) {
                    p--;
                }
                if (p >= start && s.charAt(p) == '&' && p < end - 2) {
                    end = p;
                } else {
                    break;
                }
            } else if (c == ')') {
                int open = 0;
                int close = 0;
                // 여는 괄호와 닫는 괄호의 수를 센다.
                for (int k = start; k < end; k++) {
                    if (s.charAt(k) == '(') {
                        open++;
                    } else if (s.charAt(k) == ')') {
                        close++;
                    }
                }
                if (close <= open) {
                    break;
                }
                end--;
            } else {
                break;
            }
        }
        return end;
    }

    /**
     * Returns the end of the domain of an email address whose at sign is at a position, or -1.
     * <p>
     * 골뱅이가 위치에 있는 이메일 주소의 도메인 끝을 반환하며 아니면 -1 이다.
     *
     * @param s  the text / 텍스트
     * @param at index of the at sign / 골뱅이의 위치
     * @return end index or -1 / 끝 위치 또는 -1
     */
    private static int emailEnd(String s, int at) {
        int n = s.length();
        int p = at + 1;
        int periods = 0;
        // 도메인은 영숫자, 하이픈, 밑줄이며 마침표는 뒤에 영숫자가 올 때만 구분자로 쓴다.
        while (p < n) {
            char c = s.charAt(p);
            if (MdText.isAsciiAlnum(c) || c == '-' || c == '_') {
                p++;
            } else if (c == '.' && p + 1 < n && MdText.isAsciiAlnum(s.charAt(p + 1))) {
                periods++;
                p++;
            } else {
                break;
            }
        }
        if (p == at + 1 || periods == 0) {
            return -1;
        }
        char last = s.charAt(p - 1);
        if (last == '-' || last == '_') {
            return -1;
        }
        return p;
    }
}
