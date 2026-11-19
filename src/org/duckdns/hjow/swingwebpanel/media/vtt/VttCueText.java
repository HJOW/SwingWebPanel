package org.duckdns.hjow.swingwebpanel.media.vtt;

import java.util.ArrayList;
import java.util.List;

/**
 * The WebVTT cue text parser (WebVTT 6.4 "WebVTT cue text parsing rules" with its tokenizer, and 6.6 "rules for extracting the chapter title"):
 * the cue text becomes a tree of {@link VttNode} objects. The tags <code>c</code>, <code>i</code>, <code>b</code>, <code>u</code>,
 * <code>ruby</code>, <code>rt</code> (only inside a ruby), <code>v</code> and <code>lang</code> make spans, a tag in angle brackets that starts
 * with a digit is a timestamp, other tags and an end tag that does not match are ignored, and character references are replaced. The parser
 * never fails: every input has a tree. A reference is replaced where it stands (the position then continues right after the reference).
 * <p>
 * WebVTT cue 텍스트 파서(WebVTT 6.4 "WebVTT cue text parsing rules"와 그 토크나이저, 6.6 "rules for extracting the chapter title"): cue 텍스트가
 * {@link VttNode} 객체의 트리가 된다. 태그 <code>c</code>, <code>i</code>, <code>b</code>, <code>u</code>, <code>ruby</code>, <code>rt</code>(루비 안에서만),
 * <code>v</code>, <code>lang</code> 은 span 이 되고, 꺾쇠 괄호 안에서 숫자로 시작하는 태그는 타임스탬프이며, 다른 태그와 맞지 않는 종료 태그는 무시하고,
 * 문자 참조는 바꾼다. 파서는 실패하지 않는다: 모든 입력에 트리가 있다. 참조는 있는 자리에서 바꾼다(위치는 참조 바로 뒤에서 이어진다).
 */
public final class VttCueText {

    /**
     * The token kind: a string of text.
     * <p>
     * 토큰 종류: 텍스트 문자열.
     */
    private static final int T_STRING = 0;

    /**
     * The token kind: a start tag.
     * <p>
     * 토큰 종류: 시작 태그.
     */
    private static final int T_START = 1;

    /**
     * The token kind: an end tag.
     * <p>
     * 토큰 종류: 종료 태그.
     */
    private static final int T_END = 2;

    /**
     * The token kind: a timestamp tag.
     * <p>
     * 토큰 종류: 타임스탬프 태그.
     */
    private static final int T_TIMESTAMP = 3;

    /**
     * The tokenizer state: the data state.
     * <p>
     * 토크나이저 상태: 데이터 상태.
     */
    private static final int S_DATA = 0;

    /**
     * The tokenizer state: the tag state.
     * <p>
     * 토크나이저 상태: 태그 상태.
     */
    private static final int S_TAG = 1;

    /**
     * The tokenizer state: the start tag state.
     * <p>
     * 토크나이저 상태: 시작 태그 상태.
     */
    private static final int S_START = 2;

    /**
     * The tokenizer state: the start tag class state.
     * <p>
     * 토크나이저 상태: 시작 태그 클래스 상태.
     */
    private static final int S_CLASS = 3;

    /**
     * The tokenizer state: the start tag annotation state.
     * <p>
     * 토크나이저 상태: 시작 태그 주석 상태.
     */
    private static final int S_ANNOTATION = 4;

    /**
     * The tokenizer state: the end tag state.
     * <p>
     * 토크나이저 상태: 종료 태그 상태.
     */
    private static final int S_END = 5;

    /**
     * The tokenizer state: the timestamp tag state.
     * <p>
     * 토크나이저 상태: 타임스탬프 태그 상태.
     */
    private static final int S_TIMESTAMP = 6;

    /**
     * One token of the cue text tokenizer.
     * <p>
     * cue 텍스트 토크나이저의 토큰 하나.
     */
    private static final class Token {
        /**
         * The kind, one of the <code>T_</code> constants.
         * <p>
         * 종류(<code>T_</code> 상수 중 하나).
         */
        int kind;

        /**
         * The text of a string, the name of a tag, or the value of a timestamp tag.
         * <p>
         * 문자열의 텍스트, 태그의 이름, 또는 타임스탬프 태그의 값.
         */
        String name = "";

        /**
         * The classes of a start tag.
         * <p>
         * 시작 태그의 클래스.
         */
        final List<String> classes = new ArrayList<String>();

        /**
         * The annotation of a start tag, or null.
         * <p>
         * 시작 태그의 주석. 없으면 null.
         */
        String annotation;
    }

    /**
     * The text.
     * <p>
     * 텍스트.
     */
    private final String in;

    /**
     * The position in the text.
     * <p>
     * 텍스트 안의 위치.
     */
    private int pos;

    /**
     * Creates a parser for a text.
     * <p>
     * 텍스트의 파서를 만든다.
     *
     * @param in the cue text / cue 텍스트
     */
    private VttCueText(String in) {
        this.in = in;
    }

    /**
     * Parses cue text into a tree.
     * <p>
     * cue 텍스트를 트리로 해석한다.
     *
     * @param text     the cue text / cue 텍스트
     * @param language the fallback language, or null / 대체 언어. 없으면 null
     * @return the root, a list of nodes / 루트(노드의 목록)
     */
    public static VttNode parse(String text, String language) {
        return new VttCueText(text).build(language);
    }

    /**
     * Returns the chapter title of a cue: the text of the nodes, without the ruby text (WebVTT 6.6).
     * <p>
     * cue 의 장 제목을 반환한다: 루비 텍스트를 뺀 노드의 텍스트(WebVTT 6.6).
     *
     * @param text the cue text / cue 텍스트
     * @return the title / 제목
     */
    public static String chapterTitle(String text) {
        StringBuilder sb = new StringBuilder();
        collectText(parse(text, null), sb);
        return sb.toString();
    }

    /**
     * Writes a time as the text of a timestamp node (WebVTT 6.5): <code>hh:mm:ss.ttt</code> with all components, the hours with at least two digits.
     * <p>
     * 시각을 타임스탬프 노드의 텍스트로 쓴다(WebVTT 6.5): 모든 구성 요소가 있는 <code>hh:mm:ss.ttt</code>, 시는 두 자리 이상.
     *
     * @param seconds the time in seconds / 시각(초)
     * @return the text / 텍스트
     */
    public static String formatTimestamp(double seconds) {
        return VttTimestamps.format(seconds);
    }

    /**
     * Appends the text of a node and its descendants in tree order, skipping the ruby text.
     * <p>
     * 노드와 자손의 텍스트를 트리 순서로 덧붙인다(루비 텍스트는 건너뛴다).
     *
     * @param node the node / 노드
     * @param sb   the buffer / 버퍼
     */
    private static void collectText(VttNode node, StringBuilder sb) {
        if (node.kind == VttNode.RUBY_TEXT) {
            return;
        }
        if (node.kind == VttNode.TEXT) {
            sb.append(node.value);
        }
        // 자식을 순서대로 훑는다.
        for (int i = 0; i < node.children.size(); i++) {
            collectText(node.children.get(i), sb);
        }
    }

    /**
     * Builds the tree from the tokens (the loop of WebVTT 6.4).
     * <p>
     * 토큰에서 트리를 만든다(WebVTT 6.4 의 반복).
     *
     * @param language the fallback language or null / 대체 언어. 없으면 null
     * @return the root / 루트
     */
    private VttNode build(String language) {
        VttNode result = new VttNode(VttNode.LIST);
        VttNode current = result;
        List<String> languages = new ArrayList<String>();
        if (language != null) {
            result.language = language;
            languages.add(language);
        }
        // 입력이 끝날 때까지 토큰을 하나씩 읽어 트리에 반영한다.
        while (pos < in.length()) {
            Token t = nextToken();
            if (t.kind == T_STRING) {
                VttNode text = new VttNode(VttNode.TEXT);
                text.value = t.name;
                current.append(text);
            } else if (t.kind == T_START) {
                int kind = startKind(t.name, current);
                if (kind >= 0) {
                    if (kind == VttNode.LANGUAGE) {
                        languages.add(t.annotation == null ? "" : t.annotation);
                    }
                    VttNode node = new VttNode(kind);
                    // 빈 문자열인 클래스는 뺀다.
                    for (int i = 0; i < t.classes.size(); i++) {
                        if (t.classes.get(i).length() > 0) {
                            node.classes.add(t.classes.get(i));
                        }
                    }
                    if (kind == VttNode.VOICE) {
                        node.value = t.annotation == null ? "" : t.annotation;
                    }
                    if (!languages.isEmpty()) {
                        node.language = languages.get(languages.size() - 1);
                    }
                    current.append(node);
                    current = node;
                }
            } else if (t.kind == T_END) {
                current = closeTag(t.name, current, languages);
            } else {
                // 타임스탬프 태그: 값 전체가 타임스탬프여야 한다.
                int[] p = { 0 };
                double time = VttTimestamps.collectTimestamp(t.name, p);
                if (!Double.isNaN(time) && p[0] == t.name.length()) {
                    VttNode stamp = new VttNode(VttNode.TIMESTAMP);
                    stamp.time = time;
                    current.append(stamp);
                }
            }
        }
        return result;
    }

    /**
     * Returns the node kind that a start tag makes, or -1 if the tag is ignored.
     * <p>
     * 시작 태그가 만드는 노드 종류를 반환한다. 무시하는 태그이면 -1.
     *
     * @param name    the tag name / 태그 이름
     * @param current the current node / 현재 노드
     * @return the kind or -1 / 종류. 무시하면 -1
     */
    private static int startKind(String name, VttNode current) {
        if ("c".equals(name)) {
            return VttNode.CLASS;
        } else if ("i".equals(name)) {
            return VttNode.ITALIC;
        } else if ("b".equals(name)) {
            return VttNode.BOLD;
        } else if ("u".equals(name)) {
            return VttNode.UNDERLINE;
        } else if ("ruby".equals(name)) {
            return VttNode.RUBY;
        } else if ("rt".equals(name)) {
            // 루비 텍스트는 루비 안에서만 만든다.
            return current.kind == VttNode.RUBY ? VttNode.RUBY_TEXT : -1;
        } else if ("v".equals(name)) {
            return VttNode.VOICE;
        } else if ("lang".equals(name)) {
            return VttNode.LANGUAGE;
        }
        return -1;
    }

    /**
     * Handles an end tag: it closes the current node when it matches (a ruby end tag inside a ruby text closes both).
     * <p>
     * 종료 태그를 처리한다: 현재 노드와 맞으면 닫는다(루비 텍스트 안의 ruby 종료 태그는 둘 다 닫는다).
     *
     * @param name      the tag name / 태그 이름
     * @param current   the current node / 현재 노드
     * @param languages the language stack / 언어 스택
     * @return the new current node / 새 현재 노드
     */
    private static VttNode closeTag(String name, VttNode current, List<String> languages) {
        int k = current.kind;
        boolean match = ("c".equals(name) && k == VttNode.CLASS) || ("i".equals(name) && k == VttNode.ITALIC) || ("b".equals(name) && k == VttNode.BOLD)
                || ("u".equals(name) && k == VttNode.UNDERLINE) || ("ruby".equals(name) && k == VttNode.RUBY) || ("rt".equals(name) && k == VttNode.RUBY_TEXT)
                || ("v".equals(name) && k == VttNode.VOICE);
        if (match) {
            return current.parent;
        }
        if ("lang".equals(name) && k == VttNode.LANGUAGE) {
            if (!languages.isEmpty()) {
                languages.remove(languages.size() - 1);
            }
            return current.parent;
        }
        if ("ruby".equals(name) && k == VttNode.RUBY_TEXT) {
            return current.parent.parent;
        }
        return current;
    }

    /**
     * Collapses the white space of an annotation: a sequence of white space becomes one space and the ends are trimmed.
     * <p>
     * 주석의 공백을 줄인다: 공백의 연속은 스페이스 하나가 되고 양 끝은 잘린다.
     *
     * @param s the annotation / 주석
     * @return the cleaned annotation / 정리한 주석
     */
    private static String collapse(String s) {
        StringBuilder sb = new StringBuilder();
        boolean pendingSpace = false;
        // 글자마다 공백이면 표시만 해 두고, 다음 공백이 아닌 글자 앞에서 스페이스 하나를 쓴다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (VttTimestamps.isSpace(c)) {
                pendingSpace = sb.length() > 0;
            } else {
                if (pendingSpace) {
                    sb.append(' ');
                    pendingSpace = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Runs the tokenizer for the next token (WebVTT 6.4 "WebVTT cue text tokenizer"). The input is not at its end when it is called.
     * <p>
     * 다음 토큰을 위해 토크나이저를 실행한다(WebVTT 6.4 "WebVTT cue text tokenizer"). 호출할 때 입력은 끝이 아니다.
     *
     * @return the token / 토큰
     */
    private Token nextToken() {
        int state = S_DATA;
        StringBuilder result = new StringBuilder();
        StringBuilder buffer = new StringBuilder();
        Token token = new Token();
        // 토큰이 만들어질 때까지 문자를 하나씩 상태에 따라 처리한다.
        while (true) {
            boolean eof = pos >= in.length();
            char c = eof ? 0 : in.charAt(pos);
            switch (state) {
            case S_DATA:
                if (eof) {
                    return string(token, result);
                } else if (c == '&') {
                    // 문자 참조를 그 자리에서 바꾼다(참조가 아니면 앰퍼샌드 그대로).
                    VttEntities.Reference ref = VttEntities.consume(in, pos + 1, false);
                    if (ref == null) {
                        result.append('&');
                        pos++;
                    } else {
                        result.append(ref.text);
                        pos = ref.end;
                    }
                    continue;
                } else if (c == '<') {
                    // 쌓인 텍스트가 있으면 먼저 문자열 토큰으로 돌려 주고 '<' 는 다음 호출에서 읽는다.
                    if (result.length() > 0) {
                        return string(token, result);
                    }
                    state = S_TAG;
                } else {
                    result.append(c);
                }
                break;
            case S_TAG:
                if (eof) {
                    return start(token, "", null);
                } else if (c == '>') {
                    pos++;
                    return start(token, "", null);
                } else if (c == '\t' || c == '\n' || c == '\f' || c == ' ') {
                    state = S_ANNOTATION;
                } else if (c == '.') {
                    state = S_CLASS;
                } else if (c == '/') {
                    state = S_END;
                } else if (VttTimestamps.isDigit(c)) {
                    result.append(c);
                    state = S_TIMESTAMP;
                } else {
                    result.append(c);
                    state = S_START;
                }
                break;
            case S_START:
                if (eof) {
                    return start(token, result.toString(), null);
                } else if (c == '>') {
                    pos++;
                    return start(token, result.toString(), null);
                } else if (c == '\t' || c == '\f' || c == ' ') {
                    state = S_ANNOTATION;
                } else if (c == '\n') {
                    buffer.append(c);
                    state = S_ANNOTATION;
                } else if (c == '.') {
                    state = S_CLASS;
                } else {
                    result.append(c);
                }
                break;
            case S_CLASS:
                if (eof || c == '>') {
                    if (!eof) {
                        pos++;
                    }
                    token.classes.add(buffer.toString());
                    return start(token, result.toString(), null);
                } else if (c == '\t' || c == '\f' || c == ' ') {
                    token.classes.add(buffer.toString());
                    buffer.setLength(0);
                    state = S_ANNOTATION;
                } else if (c == '\n') {
                    token.classes.add(buffer.toString());
                    buffer.setLength(0);
                    buffer.append(c);
                    state = S_ANNOTATION;
                } else if (c == '.') {
                    token.classes.add(buffer.toString());
                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
                break;
            case S_ANNOTATION:
                if (eof || c == '>') {
                    if (!eof) {
                        pos++;
                    }
                    return start(token, result.toString(), collapse(buffer.toString()));
                } else if (c == '&') {
                    VttEntities.Reference ref = VttEntities.consume(in, pos + 1, false);
                    if (ref == null) {
                        buffer.append('&');
                        pos++;
                    } else {
                        buffer.append(ref.text);
                        pos = ref.end;
                    }
                    continue;
                } else {
                    buffer.append(c);
                }
                break;
            case S_END:
                if (eof || c == '>') {
                    if (!eof) {
                        pos++;
                    }
                    token.kind = T_END;
                    token.name = result.toString();
                    return token;
                }
                result.append(c);
                break;
            default:
                // 타임스탬프 태그 상태
                if (eof || c == '>') {
                    if (!eof) {
                        pos++;
                    }
                    token.kind = T_TIMESTAMP;
                    token.name = result.toString();
                    return token;
                }
                result.append(c);
                break;
            }
            pos++;
        }
    }

    /**
     * Makes a string token.
     * <p>
     * 문자열 토큰을 만든다.
     *
     * @param token  the token object / 토큰 객체
     * @param result the text / 텍스트
     * @return the token / 토큰
     */
    private static Token string(Token token, StringBuilder result) {
        token.kind = T_STRING;
        token.name = result.toString();
        return token;
    }

    /**
     * Makes a start tag token.
     * <p>
     * 시작 태그 토큰을 만든다.
     *
     * @param token      the token object, which may hold classes / 토큰 객체(클래스가 있을 수 있다)
     * @param name       the tag name / 태그 이름
     * @param annotation the annotation or null / 주석. 없으면 null
     * @return the token / 토큰
     */
    private static Token start(Token token, String name, String annotation) {
        token.kind = T_START;
        token.name = name;
        token.annotation = annotation;
        return token;
    }
}
