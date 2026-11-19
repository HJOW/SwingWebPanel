package sourcecheck;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Documentation rules of the project: every class, method, field and constant (private ones and enum constants included,
 * anonymous class bodies excluded) must have a Javadoc comment that contains both English and Korean text, and every loop
 * must have a Korean (non-Javadoc) comment nearby. The scanner is not a full Java parser: it walks class bodies, classifies
 * member headers (type, method, field, initializer) and skips method bodies and field initializers by brace matching.
 * <p>
 * 프로젝트의 주석 규칙: 모든 클래스, 메서드, 필드, 상수(비공개와 enum 상수 포함, 익명 클래스 본문 제외)에는 영어와 한국어가 모두 들어간
 * Javadoc 이 있어야 하고, 모든 반복문 근처에는 한국어(Javadoc 이 아닌) 주석이 있어야 한다. 이 스캐너는 완전한 Java 구문 분석기가 아니며
 * 클래스 본문을 훑으며 멤버 머리부(타입, 메서드, 필드, 초기화 블록)를 분류하고 메서드 본문과 필드 초기값은 중괄호 짝으로 건너뛴다.
 */
final class DocChecks {

    /**
     * Matches inline tags such as {at-link ...} that are removed before counting words.
     * <p>
     * 단어 수를 세기 전에 제거하는 인라인 태그({@code {@link ...}} 등)를 찾는 패턴.
     */
    private static final Pattern INLINE_TAG = Pattern.compile("\\{@[^}]*\\}");

    /**
     * Matches code elements removed before counting words.
     * <p>
     * 단어 수를 세기 전에 제거하는 code 요소를 찾는 패턴.
     */
    private static final Pattern CODE_ELEMENT = Pattern.compile("(?is)<code>.*?</code>");

    /**
     * Matches HTML tags removed before counting words.
     * <p>
     * 단어 수를 세기 전에 제거하는 HTML 태그를 찾는 패턴.
     */
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]*>");

    /**
     * Matches an English word of at least two letters.
     * <p>
     * 두 글자 이상의 영어 단어를 찾는 패턴.
     */
    private static final Pattern ENGLISH_WORD = Pattern.compile("[A-Za-z]{2,}");

    /**
     * The scanned source file.
     * <p>
     * 스캔한 소스 파일.
     */
    private final JavaScanner sc;

    /**
     * Display path used in violation messages.
     * <p>
     * 위반 메시지에 쓰는 표시 경로.
     */
    private final String path;

    /**
     * Receives the violations.
     * <p>
     * 위반을 받는 목록.
     */
    private final List<Violation> out;

    /**
     * Cursor into the token list used by the member walker.
     * <p>
     * 멤버 순회가 사용하는 토큰 목록 위치.
     */
    private int cur;

    /**
     * Creates a checker for one scanned file.
     * <p>
     * 스캔된 파일 하나를 검사하는 객체를 만든다.
     *
     * @param sc   scanned source / 스캔된 소스
     * @param path display path / 표시 경로
     * @param out  list receiving violations / 위반을 받을 목록
     */
    DocChecks(JavaScanner sc, String path, List<Violation> out) {
        this.sc = sc;
        this.path = path;
        this.out = out;
    }

    /**
     * Checks Javadoc presence and language of all members.
     * <p>
     * 모든 멤버의 Javadoc 존재 여부와 언어를 검사한다.
     */
    void checkJavadoc() {
        List<JavaScanner.Tok> t = sc.toks;
        int n = t.size();
        boolean packageInfo = path.endsWith("package-info.java");
        cur = 0;
        // 파일 앞쪽의 package/import 문을 건너뛴다. package-info 는 package 앞의 Javadoc 을 검사한다.
        while (cur < n) {
            JavaScanner.Tok k = t.get(cur);
            if (k.isIdent("package")) {
                if (packageInfo) {
                    requireDoc(k, "package", "package declaration");
                }
                skipTo(';');
            } else if (k.isIdent("import")) {
                skipTo(';');
            } else {
                break;
            }
        }
        // 최상위 타입 선언을 차례로 검사한다.
        while (cur < n) {
            int before = cur;
            parseMember();
            if (cur == before) {
                cur++;
            }
        }
    }

    /**
     * Checks that every loop has a nearby Korean comment.
     * <p>
     * 모든 반복문에 가까운 한국어 주석이 있는지 검사한다.
     */
    void checkLoops() {
        List<JavaScanner.Tok> t = sc.toks;
        // 토큰을 훑으며 for/while/do 키워드마다 주변 주석을 확인한다.
        for (int p = 0; p < t.size(); p++) {
            JavaScanner.Tok k = t.get(p);
            if (!(k.isIdent("for") || k.isIdent("while") || k.isIdent("do"))) {
                continue;
            }
            if (k.isIdent("while") && isDoWhileTail(p)) {
                continue;
            }
            if (!hasKoreanCommentNear(k.line)) {
                out.add(new Violation(path, k.line, "loop-comment",
                        "loop '" + k.text + "' needs a Korean explanatory comment (same line, up to 2 lines before, or first lines of the body)"));
            }
        }
    }

    /**
     * Tests whether the while token at the given index is the closing part of a do-while statement.
     * <p>
     * 주어진 위치의 while 토큰이 do-while 문의 끝부분인지 확인한다.
     *
     * @param p token index of the while keyword / while 키워드의 토큰 위치
     * @return true for a do-while tail / do-while 끝부분이면 true
     */
    private boolean isDoWhileTail(int p) {
        List<JavaScanner.Tok> t = sc.toks;
        if (p == 0 || !t.get(p - 1).isSym('}')) {
            return false;
        }
        int depth = 0;
        int j = p + 1;
        // 조건식의 닫는 괄호를 찾아 그 뒤가 세미콜론인지 본다.
        while (j < t.size()) {
            JavaScanner.Tok k = t.get(j);
            if (k.isSym('(')) {
                depth++;
            } else if (k.isSym(')')) {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
            j++;
        }
        return j + 1 < t.size() && t.get(j + 1).isSym(';');
    }

    /**
     * Tests whether a non-Javadoc comment with Korean text is close to a line.
     * <p>
     * 한 줄 가까이에 한국어가 든 일반 주석(Javadoc 아님)이 있는지 확인한다.
     *
     * @param line line of the loop keyword / 반복문 키워드의 줄
     * @return true if such a comment exists / 있으면 true
     */
    private boolean hasKoreanCommentNear(int line) {
        List<JavaScanner.Cmt> cs = sc.cmts;
        // 모든 주석 중 Javadoc 이 아니고 한글이 있으며 위치가 가까운 것을 찾는다.
        for (int i = 0; i < cs.size(); i++) {
            JavaScanner.Cmt c = cs.get(i);
            if (!c.doc && c.endLine >= line - 2 && c.startLine <= line + 2 && countHangul(c.text) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Skips tokens up to and including the next given punctuation at depth zero of parentheses.
     * <p>
     * 소괄호 깊이 0 에서 다음에 나오는 주어진 구두점까지(포함) 토큰을 건너뛴다.
     *
     * @param c punctuation to stop after / 이 문자 뒤에서 멈춘다
     */
    private void skipTo(char c) {
        List<JavaScanner.Tok> t = sc.toks;
        // 구두점을 만날 때까지 토큰을 하나씩 소비한다.
        while (cur < t.size()) {
            JavaScanner.Tok k = t.get(cur++);
            if (k.isSym(c)) {
                return;
            }
        }
    }

    /**
     * Skips a balanced block that starts at the current token (an opening brace) and ends after the matching closing brace.
     * <p>
     * 현재 토큰(여는 중괄호)에서 시작해 짝이 되는 닫는 중괄호 뒤까지 건너뛴다.
     */
    private void skipBlock() {
        List<JavaScanner.Tok> t = sc.toks;
        int depth = 0;
        // 중괄호 깊이가 0 이 될 때까지 소비한다.
        while (cur < t.size()) {
            JavaScanner.Tok k = t.get(cur++);
            if (k.isSym('{')) {
                depth++;
            } else if (k.isSym('}')) {
                depth--;
                if (depth <= 0) {
                    return;
                }
            }
        }
    }

    /**
     * Skips a balanced parenthesis group starting at the current token.
     * <p>
     * 현재 토큰에서 시작하는 소괄호 묶음을 건너뛴다.
     */
    private void skipParens() {
        List<JavaScanner.Tok> t = sc.toks;
        int depth = 0;
        // 소괄호 깊이가 0 이 될 때까지 소비한다.
        while (cur < t.size()) {
            JavaScanner.Tok k = t.get(cur++);
            if (k.isSym('(')) {
                depth++;
            } else if (k.isSym(')')) {
                depth--;
                if (depth <= 0) {
                    return;
                }
            }
        }
    }

    /**
     * Skips an annotation use that starts at the given index and returns the index after it; returns the same index if the
     * tokens there are the start of an annotation type declaration.
     * <p>
     * 주어진 위치에서 시작하는 애노테이션 사용을 건너뛰고 그 다음 위치를 반환한다. 애노테이션 타입 선언의 시작이면 같은 위치를 반환한다.
     *
     * @param j index of the at-sign token / 골뱅이 토큰의 위치
     * @return index after the annotation / 애노테이션 다음 위치
     */
    private int skipAnnotation(int j) {
        List<JavaScanner.Tok> t = sc.toks;
        int n = t.size();
        if (j + 1 >= n || t.get(j + 1).type != JavaScanner.IDENT || t.get(j + 1).isIdent("interface")) {
            return j;
        }
        int k = j + 2;
        // 점으로 이어진 정규화된 이름을 건너뛴다.
        while (k + 1 < n && t.get(k).isSym('.') && t.get(k + 1).type == JavaScanner.IDENT) {
            k += 2;
        }
        if (k < n && t.get(k).isSym('(')) {
            int save = cur;
            cur = k;
            skipParens();
            k = cur;
            cur = save;
        }
        return k;
    }

    /**
     * Parses one member of a type body (or one top-level declaration) and checks its Javadoc.
     * <p>
     * 타입 본문의 멤버 하나(또는 최상위 선언 하나)를 해석하고 Javadoc 을 검사한다.
     */
    private void parseMember() {
        List<JavaScanner.Tok> t = sc.toks;
        int n = t.size();
        JavaScanner.Tok first = t.get(cur);
        if (first.isSym(';')) {
            cur++;
            return;
        }
        if (first.isSym('}')) {
            return;
        }
        int start = cur;
        int paren = 0;
        boolean sawParen = false;
        String parenName = null;
        boolean isType = false;
        String typeKind = null;
        String typeName = null;
        String lastIdent = null;
        boolean onlyStatic = true;
        int j = cur;
        JavaScanner.Tok stop = null;
        // 머리부를 {, ;, = 중 하나를 만날 때까지 읽으며 종류(타입/메서드/필드)를 판단한다.
        while (j < n) {
            JavaScanner.Tok k = t.get(j);
            if (k.isSym('@') && paren == 0) {
                cur = j;
                int nj = skipAnnotation(j);
                if (nj == j) {
                    isType = true;
                    typeKind = "@interface";
                    j++;
                    continue;
                }
                j = nj;
                continue;
            }
            if (k.isSym('(')) {
                if (paren == 0 && !sawParen) {
                    sawParen = true;
                    parenName = lastIdent;
                }
                paren++;
                j++;
                continue;
            }
            if (k.isSym(')')) {
                paren--;
                j++;
                continue;
            }
            if (paren == 0 && (k.isSym('{') || k.isSym(';') || k.isSym('=') || k.isSym('}'))) {
                stop = k;
                break;
            }
            if (k.type == JavaScanner.IDENT && paren == 0) {
                if (k.text.equals("class") || k.text.equals("interface") || k.text.equals("enum")) {
                    isType = true;
                    typeKind = k.text.equals("interface") && typeKind != null ? typeKind : k.text;
                    if (j + 1 < n && t.get(j + 1).type == JavaScanner.IDENT && typeName == null) {
                        typeName = t.get(j + 1).text;
                    }
                }
                if (!k.text.equals("static")) {
                    onlyStatic = false;
                }
                lastIdent = k.text;
            } else if (paren == 0 && !k.isSym('<') && !k.isSym('>')) {
                onlyStatic = false;
            }
            j++;
        }
        if (stop == null) {
            cur = n;
            return;
        }
        if (isType) {
            requireDoc(first, typeKind, typeName == null ? "?" : typeName, start);
            if (stop.isSym('{')) {
                cur = j + 1;
                parseTypeBody("enum".equals(typeKind));
            } else {
                cur = j + 1;
            }
            return;
        }
        if (sawParen) {
            requireDoc(first, "method", parenName == null ? "?" : parenName, start);
            cur = j;
            finishAfterHeader(stop);
            return;
        }
        if (stop.isSym('{') && onlyStatic) {
            cur = j;
            skipBlock();
            return;
        }
        if (stop.isSym('}')) {
            cur = j;
            return;
        }
        requireDoc(first, "field", lastIdent == null ? "?" : lastIdent, start);
        cur = j;
        finishAfterHeader(stop);
    }

    /**
     * Consumes the body or initializer that follows a member header.
     * <p>
     * 멤버 머리부 뒤에 오는 본문이나 초기값을 소비한다.
     *
     * @param stop the token that ended the header (brace, semicolon or equals) / 머리부를 끝낸 토큰
     */
    private void finishAfterHeader(JavaScanner.Tok stop) {
        List<JavaScanner.Tok> t = sc.toks;
        if (stop.isSym('{')) {
            skipBlock();
        } else if (stop.isSym(';')) {
            cur++;
        } else if (stop.isSym('=')) {
            int depth = 0;
            cur++;
            // 초기값은 깊이 0 의 세미콜론까지 중괄호/소괄호/대괄호를 세며 건너뛴다.
            while (cur < t.size()) {
                JavaScanner.Tok k = t.get(cur);
                if (k.isSym('(') || k.isSym('[') || k.isSym('{')) {
                    depth++;
                } else if (k.isSym(')') || k.isSym(']') || k.isSym('}')) {
                    depth--;
                    if (depth < 0) {
                        return;
                    }
                } else if (k.isSym(';') && depth == 0) {
                    cur++;
                    return;
                }
                cur++;
            }
        }
    }

    /**
     * Parses the members of a type body after its opening brace and consumes the closing brace.
     * <p>
     * 여는 중괄호 뒤의 타입 본문 멤버를 해석하고 닫는 중괄호를 소비한다.
     *
     * @param isEnum whether the body starts with enum constants / 본문이 enum 상수로 시작하는지 여부
     */
    private void parseTypeBody(boolean isEnum) {
        List<JavaScanner.Tok> t = sc.toks;
        if (isEnum) {
            parseEnumConstants();
        }
        // 닫는 중괄호를 만날 때까지 멤버를 하나씩 검사한다.
        while (cur < t.size() && !t.get(cur).isSym('}')) {
            int before = cur;
            parseMember();
            if (cur == before) {
                cur++;
            }
        }
        if (cur < t.size()) {
            cur++;
        }
    }

    /**
     * Parses the constant list at the start of an enum body, requiring Javadoc for each constant.
     * <p>
     * enum 본문 앞쪽의 상수 목록을 해석하며 각 상수의 Javadoc 을 요구한다.
     */
    private void parseEnumConstants() {
        List<JavaScanner.Tok> t = sc.toks;
        // 세미콜론이나 닫는 중괄호가 나올 때까지 상수를 하나씩 읽는다.
        while (cur < t.size()) {
            JavaScanner.Tok k = t.get(cur);
            if (k.isSym(';')) {
                cur++;
                return;
            }
            if (k.isSym('}')) {
                return;
            }
            if (k.isSym(',')) {
                cur++;
                continue;
            }
            int constStart = cur;
            JavaScanner.Tok constFirst = k;
            // 상수 앞의 애노테이션은 건너뛴다.
            while (cur < t.size() && t.get(cur).isSym('@')) {
                int nj = skipAnnotation(cur);
                if (nj == cur) {
                    return;
                }
                cur = nj;
            }
            if (cur >= t.size() || t.get(cur).type != JavaScanner.IDENT) {
                return;
            }
            requireDoc(constFirst, "enum constant", t.get(cur).text, constStart);
            cur++;
            if (cur < t.size() && t.get(cur).isSym('(')) {
                skipParens();
            }
            if (cur < t.size() && t.get(cur).isSym('{')) {
                skipBlock();
            }
        }
    }

    /**
     * Requires a bilingual Javadoc before a declaration.
     * <p>
     * 선언 앞에 영어/한국어 Javadoc 이 있어야 함을 확인한다.
     *
     * @param first token that starts the declaration / 선언을 시작하는 토큰
     * @param kind  declaration kind for messages / 메시지용 선언 종류
     * @param name  declaration name for messages / 메시지용 선언 이름
     */
    private void requireDoc(JavaScanner.Tok first, String kind, String name) {
        requireDoc(first, kind, name, indexOf(first));
    }

    /**
     * Requires a bilingual Javadoc before a declaration whose first token index is known.
     * <p>
     * 첫 토큰 위치를 아는 선언 앞에 영어/한국어 Javadoc 이 있어야 함을 확인한다.
     *
     * @param first      token that starts the declaration / 선언을 시작하는 토큰
     * @param kind       declaration kind for messages / 메시지용 선언 종류
     * @param name       declaration name for messages / 메시지용 선언 이름
     * @param startIndex index of the first token / 첫 토큰의 위치
     */
    private void requireDoc(JavaScanner.Tok first, String kind, String name, int startIndex) {
        int prevEnd = startIndex > 0 ? sc.toks.get(startIndex - 1).end : 0;
        JavaScanner.Cmt doc = null;
        List<JavaScanner.Cmt> cs = sc.cmts;
        // 직전 토큰과 선언 첫 토큰 사이의 마지막 Javadoc 주석을 찾는다.
        for (int i = 0; i < cs.size(); i++) {
            JavaScanner.Cmt c = cs.get(i);
            if (c.doc && c.pos >= prevEnd && c.end <= first.pos) {
                doc = c;
            }
        }
        if (doc == null) {
            out.add(new Violation(path, first.line, "javadoc", "missing Javadoc for " + kind + " " + name));
            return;
        }
        String reason = languageProblem(doc.text);
        if (reason != null) {
            out.add(new Violation(path, doc.startLine, "javadoc-lang", "Javadoc of " + kind + " " + name + ": " + reason));
        }
    }

    /**
     * Finds the index of a token in the token list.
     * <p>
     * 토큰 목록에서 토큰의 위치를 찾는다.
     *
     * @param tok token / 토큰
     * @return index or 0 if not found / 위치(없으면 0)
     */
    private int indexOf(JavaScanner.Tok tok) {
        List<JavaScanner.Tok> t = sc.toks;
        // 위치 값이 같은 토큰을 선형 탐색으로 찾는다.
        for (int i = 0; i < t.size(); i++) {
            if (t.get(i) == tok) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Checks that a Javadoc comment contains English and Korean text.
     * <p>
     * Javadoc 주석에 영어와 한국어가 모두 들어 있는지 확인한다.
     *
     * @param doc full Javadoc text / Javadoc 전체 문자열
     * @return null if fine, otherwise the problem description / 문제가 없으면 null, 있으면 설명
     */
    static String languageProblem(String doc) {
        String s = doc;
        s = INLINE_TAG.matcher(s).replaceAll(" ");
        s = CODE_ELEMENT.matcher(s).replaceAll(" ");
        s = HTML_TAG.matcher(s).replaceAll(" ");
        int hangul = countHangul(s);
        int words = 0;
        Matcher m = ENGLISH_WORD.matcher(s);
        // 영어 단어 수를 센다.
        while (m.find()) {
            words++;
        }
        if (hangul < 2 && words < 2) {
            return "empty description";
        }
        if (hangul < 2) {
            return "Korean description is missing";
        }
        if (words < 2) {
            return "English description is missing";
        }
        return null;
    }

    /**
     * Counts Hangul characters in a text.
     * <p>
     * 문자열에서 한글 문자의 수를 센다.
     *
     * @param s text / 문자열
     * @return number of Hangul syllables and jamo / 한글 음절과 자모의 수
     */
    static int countHangul(String s) {
        int count = 0;
        // 한글 음절(AC00~D7A3)과 호환 자모(3131~318E)를 센다.
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 0xAC00 && c <= 0xD7A3) || (c >= 0x3131 && c <= 0x318E)) {
                count++;
            }
        }
        return count;
    }
}
