package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.text.CharClass;

/**
 * Recursive descent parser for ECMAScript 2015 regular expression patterns (ECMA-262 21.2.1) including the Annex B.1.4
 * web compatibility syntax when the u flag is absent: octal escapes, identity escapes of any character except c (and k
 * when the pattern has named groups), literal <code>]</code>, <code>{</code>, <code>}</code>, quantifiable lookaheads, a
 * <code>\c</code> that is not followed by a letter meaning a backslash, and class escapes such as <code>[\d-x]</code>.
 * With the u flag these forms are early errors and <code>u-brace code point escapes</code> and surrogate pair escapes are supported.
 * Named groups <code>(?&lt;name&gt;...)</code> with <code>\k&lt;name&gt;</code>, lookbehind and Unicode property escapes are
 * accepted as ES2018 syntax.
 * Instances are single use and not thread-safe.
 * <p>
 * ECMAScript 2015 정규식 패턴(ECMA-262 21.2.1)의 재귀 하강 해석기. u 플래그가 없으면 부속서 B.1.4 의 웹 호환 구문을 허용한다:
 * 8진 escape, c(그리고 이름 있는 그룹이 있을 때 k)를 뺀 모든 문자의 identity escape, 리터럴 <code>]</code>, <code>{</code>, <code>}</code>,
 * 수량자가 붙는 선행 탐색, 글자가 뒤따르지 않는 <code>\c</code>(역슬래시를 뜻함), <code>[\d-x]</code> 같은 클래스 escape.
 * u 플래그가 있으면 이런 형태는 조기 오류이고 <code>u-brace code point escapes</code> 와 서로게이트 쌍 escape 를 지원한다.
 * 이름 있는 그룹 <code>(?&lt;name&gt;...)</code> 와 <code>\k&lt;name&gt;</code>, 뒤쪽 탐색, 유니코드 속성 escape 를 ES2018 구문으로
 * 받아들인다. 인스턴스는 한 번만 쓰며 스레드 안전하지 않다.
 */
final class RegExpParser {

    /**
     * The pattern text.
     * <p>
     * 패턴 문자열.
     */
    private final String src;

    /**
     * Length of the pattern.
     * <p>
     * 패턴 길이.
     */
    private final int n;

    /**
     * True if the u flag is set.
     * <p>
     * u 플래그가 있으면 true.
     */
    private final boolean unicode;

    /**
     * Current read offset.
     * <p>
     * 현재 읽는 위치.
     */
    private int pos;

    /**
     * Number of capturing groups opened so far.
     * <p>
     * 지금까지 연 캡처 그룹 수.
     */
    private int groupCounter;

    /**
     * Total number of capturing groups found by the pre-scan.
     * <p>
     * 사전 훑기로 찾은 전체 캡처 그룹 수.
     */
    private int totalGroups;

    /**
     * True if the pattern contains a named group (affects the meaning of \k).
     * <p>
     * 패턴에 이름 있는 그룹이 있으면 true(\k 의 의미에 영향).
     */
    private boolean hasNamed;

    /**
     * Group names by group number (index 0 unused, null for unnamed groups).
     * <p>
     * 그룹 번호별 이름(0 번은 쓰지 않음, 이름 없는 그룹은 null).
     */
    private final List<String> names = new ArrayList<String>();

    /**
     * Named back references waiting for resolution after parsing.
     * <p>
     * 해석이 끝난 뒤 이름을 풀어야 하는 이름 있는 역참조들.
     */
    private final List<RegExpNode> namedRefs = new ArrayList<RegExpNode>();

    /**
     * Creates a parser.
     * <p>
     * 해석기를 만든다.
     *
     * @param src   pattern text / 패턴 문자열
     * @param flags flag bits from {@link RegExpFlags} / {@link RegExpFlags} 의 플래그 비트
     */
    RegExpParser(String src, int flags) {
        this.src = src;
        this.n = src.length();
        this.unicode = (flags & RegExpFlags.UNICODE) != 0;
    }

    /**
     * Returns the number of capturing groups (valid after {@link #parse()}).
     * <p>
     * 캡처 그룹 수를 반환한다({@link #parse()} 뒤에 유효).
     *
     * @return group count / 그룹 수
     */
    int getGroupCount() {
        return groupCounter;
    }

    /**
     * Returns group names by group number (valid after {@link #parse()}).
     * <p>
     * 그룹 번호별 이름을 반환한다({@link #parse()} 뒤에 유효).
     *
     * @return names list, index 0 unused / 이름 목록(0 번은 쓰지 않음)
     */
    List<String> getGroupNames() {
        return names;
    }

    /**
     * Creates a syntax error at an offset.
     * <p>
     * 지정한 위치의 구문 오류를 만든다.
     *
     * @param reason reason text / 원인 문구
     * @param at     offset / 위치
     * @return the exception / 예외
     */
    private RegExpSyntaxException error(String reason, int at) {
        return new RegExpSyntaxException(src, reason, at);
    }

    /**
     * Parses the whole pattern.
     * <p>
     * 패턴 전체를 해석한다.
     *
     * @return root node / 루트 노드
     * @throws RegExpSyntaxException on any early error / 조기 오류가 있으면
     */
    RegExpNode parse() throws RegExpSyntaxException {
        prescan();
        names.add(null);
        RegExpNode root = parseDisjunction();
        if (pos < n) {
            throw error("Unmatched ')'", pos);
        }
        // 이름 있는 역참조를 그룹 번호로 바꾼다.
        for (int i = 0; i < namedRefs.size(); i++) {
            RegExpNode ref = namedRefs.get(i);
            int idx = names.indexOf(ref.name);
            if (idx < 0) {
                throw error("Invalid named capture referenced", -1);
            }
            ref.value = idx;
        }
        return root;
    }

    /**
     * Counts capturing groups and detects named groups before the real parse, so that decimal escapes can tell
     * a back reference from a legacy octal escape.
     * <p>
     * 십진 escape 가 역참조인지 레거시 8진 escape 인지 구별할 수 있도록, 본 해석 전에 캡처 그룹 수를 세고 이름 있는 그룹을 찾는다.
     */
    private void prescan() {
        boolean inClass = false;
        // 이스케이프와 문자 클래스 안을 건너뛰며 여는 괄호를 센다.
        for (int i = 0; i < n; i++) {
            char c = src.charAt(i);
            if (c == '\\') {
                i++;
            } else if (inClass) {
                if (c == ']') {
                    inClass = false;
                }
            } else if (c == '[') {
                inClass = true;
            } else if (c == '(') {
                if (i + 1 < n && src.charAt(i + 1) == '?') {
                    if (i + 3 < n && src.charAt(i + 2) == '<' && src.charAt(i + 3) != '=' && src.charAt(i + 3) != '!') {
                        totalGroups++;
                        hasNamed = true;
                    }
                } else {
                    totalGroups++;
                }
            }
        }
    }

    /**
     * Reads one pattern character: a code point with the u flag, else a code unit.
     * <p>
     * 패턴 문자 하나를 읽는다. u 플래그가 있으면 코드 포인트, 아니면 코드 단위.
     *
     * @return the character / 문자
     */
    private int nextChar() {
        char c = src.charAt(pos++);
        if (unicode && Character.isHighSurrogate(c) && pos < n && Character.isLowSurrogate(src.charAt(pos))) {
            return Character.toCodePoint(c, src.charAt(pos++));
        }
        return c;
    }

    /**
     * Disjunction :: Alternative ( | Alternative )*
     * <p>
     * Disjunction :: Alternative ( | Alternative )*
     *
     * @return node / 노드
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private RegExpNode parseDisjunction() throws RegExpSyntaxException {
        List<RegExpNode> alts = new ArrayList<RegExpNode>();
        alts.add(parseAlternative());
        // '|' 로 구분된 선택지를 모두 읽는다.
        while (pos < n && src.charAt(pos) == '|') {
            pos++;
            alts.add(parseAlternative());
        }
        return RegExpNode.list(RegExpNode.ALT, alts);
    }

    /**
     * Alternative :: Term*
     * <p>
     * Alternative :: Term*
     *
     * @return node / 노드
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private RegExpNode parseAlternative() throws RegExpSyntaxException {
        List<RegExpNode> terms = new ArrayList<RegExpNode>();
        // '|' 나 ')' 나 끝을 만날 때까지 항을 읽는다.
        while (pos < n && src.charAt(pos) != '|' && src.charAt(pos) != ')') {
            terms.add(parseTerm());
        }
        return RegExpNode.list(RegExpNode.SEQ, terms);
    }

    /**
     * Term :: Assertion | Atom Quantifier?
     * <p>
     * Term :: Assertion | Atom Quantifier?
     *
     * @return node / 노드
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private RegExpNode parseTerm() throws RegExpSyntaxException {
        char c = src.charAt(pos);
        int groupsBefore = groupCounter;
        int start = pos;
        RegExpNode atom;
        boolean quantifiable = true;
        switch (c) {
        case '^':
            pos++;
            atom = RegExpNode.of(RegExpNode.ASSERT, RegExpNode.BOL);
            quantifiable = false;
            break;
        case '$':
            pos++;
            atom = RegExpNode.of(RegExpNode.ASSERT, RegExpNode.EOL);
            quantifiable = false;
            break;
        case '(':
            atom = parseGroup();
            quantifiable = atom.type != RegExpNode.LOOK_BEHIND && (atom.type != RegExpNode.LOOK || !unicode);
            break;
        case '.':
            pos++;
            atom = new RegExpNode(RegExpNode.ANY);
            break;
        case '[':
            atom = parseClass();
            break;
        case '\\':
            pos++;
            if (pos >= n) {
                throw error("\\ at end of pattern", pos);
            }
            if (src.charAt(pos) == 'b') {
                pos++;
                atom = RegExpNode.of(RegExpNode.ASSERT, RegExpNode.WORD_BOUNDARY);
                quantifiable = false;
            } else if (src.charAt(pos) == 'B') {
                pos++;
                atom = RegExpNode.of(RegExpNode.ASSERT, RegExpNode.NOT_WORD_BOUNDARY);
                quantifiable = false;
            } else {
                atom = parseAtomEscape();
            }
            break;
        case '*':
        case '+':
        case '?':
            throw error("Nothing to repeat", pos);
        case '{':
            if (unicode) {
                throw error("Nothing to repeat", pos);
            }
            if (bracedQuantifierEnd(pos) >= 0) {
                throw error("Nothing to repeat", pos);
            }
            pos++;
            atom = RegExpNode.of(RegExpNode.CHAR, '{');
            break;
        case '}':
        case ']':
            if (unicode) {
                throw error("Lone quantifier brackets", pos);
            }
            pos++;
            atom = RegExpNode.of(RegExpNode.CHAR, c);
            break;
        default:
            atom = RegExpNode.of(RegExpNode.CHAR, nextChar());
            break;
        }
        int min;
        int max;
        char q = pos < n ? src.charAt(pos) : 0;
        if (q == '*') {
            min = 0;
            max = RegExpNode.INFINITE;
            pos++;
        } else if (q == '+') {
            min = 1;
            max = RegExpNode.INFINITE;
            pos++;
        } else if (q == '?') {
            min = 0;
            max = 1;
            pos++;
        } else if (q == '{') {
            int end = bracedQuantifierEnd(pos);
            if (end < 0) {
                if (unicode) {
                    throw error("Incomplete quantifier", pos);
                }
                return atom;
            }
            int comma = src.indexOf(',', pos);
            if (comma < 0 || comma > end) {
                min = parseNumber(pos + 1, end - 1);
                max = min;
            } else {
                min = parseNumber(pos + 1, comma);
                max = comma + 1 == end - 1 ? RegExpNode.INFINITE : parseNumber(comma + 1, end - 1);
            }
            if (min > max) {
                throw error("numbers out of order in {} quantifier", pos);
            }
            pos = end;
        } else {
            return atom;
        }
        if (!quantifiable) {
            throw error("Nothing to repeat", start);
        }
        boolean greedy = true;
        if (pos < n && src.charAt(pos) == '?') {
            greedy = false;
            pos++;
        }
        RegExpNode rep = new RegExpNode(RegExpNode.REPEAT);
        rep.child = atom;
        rep.min = min;
        rep.max = max;
        rep.greedy = greedy;
        rep.firstGroup = groupsBefore + 1;
        rep.lastGroup = groupCounter;
        if (pos < n) {
            char next = src.charAt(pos);
            if (next == '*' || next == '+' || next == '?' || (next == '{' && bracedQuantifierEnd(pos) >= 0)) {
                throw error("Nothing to repeat", pos);
            }
        }
        return rep;
    }

    /**
     * If a valid braced quantifier <code>{n}</code>, <code>{n,}</code> or <code>{n,m}</code> starts at p, returns the
     * offset just after its closing brace, else -1.
     * <p>
     * p 에서 올바른 중괄호 수량자 <code>{n}</code>, <code>{n,}</code>, <code>{n,m}</code> 이 시작하면 닫는 중괄호 바로 다음 위치를,
     * 아니면 -1 을 반환한다.
     *
     * @param p index of the opening brace / 여는 중괄호 위치
     * @return end offset or -1 / 끝 위치 또는 -1
     */
    private int bracedQuantifierEnd(int p) {
        int i = p + 1;
        int digits = 0;
        // 최솟값 숫자를 읽는다.
        while (i < n && src.charAt(i) >= '0' && src.charAt(i) <= '9') {
            i++;
            digits++;
        }
        if (digits == 0) {
            return -1;
        }
        if (i < n && src.charAt(i) == ',') {
            i++;
            // 최댓값 숫자(없을 수 있음)를 읽는다.
            while (i < n && src.charAt(i) >= '0' && src.charAt(i) <= '9') {
                i++;
            }
        }
        if (i < n && src.charAt(i) == '}') {
            return i + 1;
        }
        return -1;
    }

    /**
     * Parses decimal digits in [from, to) into an int, clamping huge values to the unbounded marker.
     * <p>
     * [from, to) 의 10진 숫자를 int 로 읽는다. 지나치게 큰 값은 무제한 표시값으로 제한한다.
     *
     * @param from first index / 시작 위치
     * @param to   end index (exclusive) / 끝 위치(미포함)
     * @return the value / 값
     */
    private int parseNumber(int from, int to) {
        long v = 0;
        // 숫자를 자리마다 누적하고 int 한도를 넘으면 고정한다.
        for (int i = from; i < to; i++) {
            v = v * 10 + (src.charAt(i) - '0');
            if (v > Integer.MAX_VALUE) {
                v = Integer.MAX_VALUE;
            }
        }
        return (int) v;
    }

    /**
     * Parses a parenthesized construct: capturing group, (?:), (?=), (?!) or a named group.
     * <p>
     * 괄호 구문을 해석한다: 캡처 그룹, (?:), (?=), (?!), 이름 있는 그룹.
     *
     * @return node / 노드
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private RegExpNode parseGroup() throws RegExpSyntaxException {
        int open = pos;
        pos++;
        RegExpNode node;
        if (src.startsWith("?=", pos) || src.startsWith("?!", pos)) {
            boolean negative = src.charAt(pos + 1) == '!';
            pos += 2;
            node = RegExpNode.of(RegExpNode.LOOK, negative ? 1 : 0, parseDisjunction());
        } else if (src.startsWith("?<=", pos) || src.startsWith("?<!", pos)) {
            boolean negative = src.charAt(pos + 2) == '!';
            pos += 3;
            node = RegExpNode.of(RegExpNode.LOOK_BEHIND, negative ? 1 : 0, parseDisjunction());
        } else if (src.startsWith("?:", pos)) {
            pos += 2;
            node = RegExpNode.of(RegExpNode.GROUP, 0, parseDisjunction());
        } else if (src.startsWith("?<", pos) && pos + 2 < n && src.charAt(pos + 2) != '=' && src.charAt(pos + 2) != '!') {
            pos += 2;
            String name = parseGroupName();
            int index = ++groupCounter;
            // 이름 목록이 그룹 번호까지 채워질 때까지 빈 칸을 추가한다.
            while (names.size() <= index) {
                names.add(null);
            }
            if (names.contains(name)) {
                throw error("Duplicate capture group name", pos);
            }
            names.set(index, name);
            node = RegExpNode.of(RegExpNode.GROUP, index, parseDisjunction());
        } else if (pos < n && src.charAt(pos) == '?') {
            throw error("Invalid group", pos);
        } else {
            int index = ++groupCounter;
            // 이름 목록이 그룹 번호까지 채워질 때까지 빈 칸을 추가한다.
            while (names.size() <= index) {
                names.add(null);
            }
            node = RegExpNode.of(RegExpNode.GROUP, index, parseDisjunction());
        }
        if (pos >= n || src.charAt(pos) != ')') {
            throw error("Unterminated group", open);
        }
        pos++;
        return node;
    }

    /**
     * Reads a group name up to the closing angle bracket.
     * <p>
     * 닫는 꺽쇠까지 그룹 이름을 읽는다.
     *
     * @return the name / 이름
     * @throws RegExpSyntaxException if the name is not an identifier / 이름이 식별자가 아니면
     */
    private String parseGroupName() throws RegExpSyntaxException {
        StringBuilder sb = new StringBuilder();
        // 식별자 문자를 '>' 까지 읽는다.
        while (pos < n && src.charAt(pos) != '>') {
            int start = pos;
            int cp;
            if (src.startsWith("\\u", pos)) {
                // 그룹 이름의 유니코드 이스케이프는 패턴 플래그와 관계없이 UnicodeMode로 읽는다.
                pos++;
                cp = parseUnicodeEscape(true);
            } else {
                cp = src.codePointAt(pos);
                pos += Character.charCount(cp);
            }
            boolean ok = sb.length() == 0 ? CharClass.isIdentifierStart(cp) : CharClass.isIdentifierPart(cp);
            if (!ok) {
                throw error("Invalid capture group name", start);
            }
            sb.appendCodePoint(cp);
        }
        if (pos >= n || sb.length() == 0) {
            throw error("Invalid capture group name", pos);
        }
        pos++;
        return sb.toString();
    }

    /**
     * Parses an escape outside a character class. The backslash has been consumed; the current character follows it.
     * <p>
     * 문자 클래스 밖의 escape 를 해석한다. 역슬래시는 이미 읽었고 현재 문자는 그 다음 문자이다.
     *
     * @return node / 노드
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private RegExpNode parseAtomEscape() throws RegExpSyntaxException {
        char e = src.charAt(pos);
        switch (e) {
        case 'd':
            pos++;
            return setNode(RegExpCharSet.digits());
        case 'D':
            pos++;
            return setNode(RegExpCharSet.digits().complement(maxChar()));
        case 's':
            pos++;
            return setNode(RegExpCharSet.space());
        case 'S':
            pos++;
            return setNode(RegExpCharSet.space().complement(maxChar()));
        case 'w':
            pos++;
            return setNode(RegExpCharSet.word());
        case 'W':
            pos++;
            return setNode(RegExpCharSet.word().complement(maxChar()));
        case 'p':
        case 'P':
            return setNode(parseUnicodeProperty(e == 'P'));
        case 'k':
            if (hasNamed) {
                pos++;
                if (pos >= n || src.charAt(pos) != '<') {
                    throw error("Invalid named reference", pos);
                }
                pos++;
                RegExpNode ref = RegExpNode.of(RegExpNode.BACKREF, -1);
                ref.name = parseGroupName();
                namedRefs.add(ref);
                return ref;
            }
            if (unicode) {
                throw error("Invalid escape", pos);
            }
            pos++;
            return RegExpNode.of(RegExpNode.CHAR, 'k');
        default:
            break;
        }
        if (e >= '1' && e <= '9') {
            int digitsStart = pos;
            int end = pos;
            // 십진 escape 의 숫자를 모두 읽어 역참조 번호를 정한다.
            while (end < n && src.charAt(end) >= '0' && src.charAt(end) <= '9') {
                end++;
            }
            int number = parseNumber(digitsStart, end);
            if (number <= totalGroups) {
                pos = end;
                return RegExpNode.of(RegExpNode.BACKREF, number);
            }
            if (unicode) {
                throw error("Invalid escape", digitsStart - 1);
            }
            if (e >= '8') {
                pos++;
                return RegExpNode.of(RegExpNode.CHAR, e);
            }
            return RegExpNode.of(RegExpNode.CHAR, parseLegacyOctal());
        }
        if (e == '0') {
            if (pos + 1 < n && src.charAt(pos + 1) >= '0' && src.charAt(pos + 1) <= '9') {
                if (unicode) {
                    throw error("Invalid decimal escape", pos);
                }
                return RegExpNode.of(RegExpNode.CHAR, parseLegacyOctal());
            }
            pos++;
            return RegExpNode.of(RegExpNode.CHAR, 0);
        }
        return RegExpNode.of(RegExpNode.CHAR, parseCharacterEscape(false));
    }

    /**
     * Largest character value for complement sets.
     * <p>
     * 여집합에 쓰는 최대 문자 값.
     *
     * @return 0x10FFFF with the u flag, else 0xFFFF / u 플래그면 0x10FFFF, 아니면 0xFFFF
     */
    private int maxChar() {
        return unicode ? 0x10FFFF : 0xFFFF;
    }

    /**
     * Parses a Unicode property escape and returns its normalized set.
     * <p>
     * 유니코드 속성 escape 를 해석해 정규화한 집합을 돌려준다.
     *
     * @param complement true for \P / \P 이면 true
     * @return property set / 속성 집합
     * @throws RegExpSyntaxException on invalid syntax or an unknown property / 구문 또는 속성 오류 시
     */
    private RegExpCharSet parseUnicodeProperty(boolean complement) throws RegExpSyntaxException {
        int at = pos;
        if (!unicode || pos + 2 >= n || src.charAt(pos + 1) != '{') {
            throw error("Invalid property escape", at);
        }
        int end = src.indexOf('}', pos + 2);
        if (end < 0 || end == pos + 2) {
            throw error("Invalid property escape", at);
        }
        String name = src.substring(pos + 2, end);
        RegExpCharSet set = RegExpCharSet.unicodeProperty(name);
        if (set == null) {
            throw error("Invalid property name " + name, at);
        }
        pos = end + 1;
        return complement ? set.complement(0x10FFFF) : set;
    }

    /**
     * Creates a class node for a predefined set.
     * <p>
     * 미리 정의된 집합의 클래스 노드를 만든다.
     *
     * @param set normalized set / 정규화된 집합
     * @return SET node / SET 노드
     */
    private RegExpNode setNode(RegExpCharSet set) {
        RegExpNode node = new RegExpNode(RegExpNode.SET);
        node.set = set;
        return node;
    }

    /**
     * Reads a legacy (Annex B) octal escape at the current position: up to three octal digits with a value at most 0377.
     * <p>
     * 현재 위치의 레거시(부속서 B) 8진 escape 를 읽는다: 값이 0377 이하인 최대 세 자리 8진수.
     *
     * @return the character value / 문자 값
     */
    private int parseLegacyOctal() {
        int v = src.charAt(pos++) - '0';
        // 두 번째, 세 번째 8진 자리를 값이 0377 을 넘지 않는 동안 이어 읽는다.
        for (int i = 0; i < 2 && pos < n && src.charAt(pos) >= '0' && src.charAt(pos) <= '7' && v * 8 + (src.charAt(pos) - '0') <= 0377; i++) {
            v = v * 8 + (src.charAt(pos++) - '0');
        }
        return v;
    }

    /**
     * Parses CharacterEscape (and IdentityEscape) at the current position, which is the character after the backslash.
     * <p>
     * 현재 위치(역슬래시 다음 문자)의 CharacterEscape(와 IdentityEscape)를 해석한다.
     *
     * @param inClass true inside a character class / 문자 클래스 안이면 true
     * @return the character value / 문자 값
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private int parseCharacterEscape(boolean inClass) throws RegExpSyntaxException {
        char e = src.charAt(pos);
        switch (e) {
        case 'f':
            pos++;
            return 0x0C;
        case 'n':
            pos++;
            return 0x0A;
        case 'r':
            pos++;
            return 0x0D;
        case 't':
            pos++;
            return 0x09;
        case 'v':
            pos++;
            return 0x0B;
        case 'c':
            if (pos + 1 < n) {
                char l = src.charAt(pos + 1);
                if ((l >= 'a' && l <= 'z') || (l >= 'A' && l <= 'Z')) {
                    pos += 2;
                    return l % 32;
                }
                if (inClass && !unicode && ((l >= '0' && l <= '9') || l == '_')) {
                    pos += 2;
                    return l % 32;
                }
            }
            if (unicode) {
                throw error("Invalid unicode escape", pos);
            }
            return '\\';
        case 'x': {
            int h = pos + 2 < n ? hex2(pos + 1) : -1;
            if (h >= 0) {
                pos += 3;
                return h;
            }
            if (unicode) {
                throw error("Invalid escape", pos);
            }
            pos++;
            return 'x';
        }
        case 'u':
            return parseUnicodeEscape(unicode);
        case '0':
            pos++;
            return 0;
        default:
            break;
        }
        // 나머지는 identity escape 이다. u 플래그에서는 구문 문자와 '/' (클래스 안에서는 '-')만 허용한다.
        if (unicode) {
            if ("^$\\.*+?()[]{}|/".indexOf(e) < 0 && !(inClass && e == '-')) {
                throw error("Invalid escape", pos);
            }
            pos++;
            return e;
        }
        return nextChar();
    }

    /**
     * Reads two hexadecimal digits at an index.
     * <p>
     * 지정한 위치의 16진수 두 자리를 읽는다.
     *
     * @param i start index / 시작 위치
     * @return value or -1 / 값 또는 -1
     */
    private int hex2(int i) {
        int h = CharClass.hexValue(src.charAt(i));
        int l = CharClass.hexValue(src.charAt(i + 1));
        return (h < 0 || l < 0) ? -1 : (h << 4) | l;
    }

    /**
     * Reads four hexadecimal digits at an index.
     * <p>
     * 지정한 위치의 16진수 네 자리를 읽는다.
     *
     * @param i start index / 시작 위치
     * @return value or -1 / 값 또는 -1
     */
    private int hex4(int i) {
        if (i + 4 > n) {
            return -1;
        }
        int h = hex2(i);
        int l = hex2(i + 2);
        return (h < 0 || l < 0) ? -1 : (h << 8) | l;
    }

    /**
     * Parses a unicode escape; the current character is the letter u.
     * <p>
     * 유니코드 escape 를 해석한다. 현재 문자는 글자 u 이다.
     *
     * @param fullUnicode whether to use UnicodeMode for this escape / 이스케이프의 UnicodeMode 적용 여부
     * @return code point or code unit / 코드 포인트 또는 코드 단위
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private int parseUnicodeEscape(boolean fullUnicode) throws RegExpSyntaxException {
        int at = pos;
        if (fullUnicode && pos + 1 < n && src.charAt(pos + 1) == '{') {
            int i = pos + 2;
            long v = 0;
            int count = 0;
            // 닫는 중괄호까지 16진수를 읽는다.
            while (i < n && src.charAt(i) != '}') {
                int h = CharClass.hexValue(src.charAt(i));
                if (h < 0) {
                    throw error("Invalid Unicode escape", at);
                }
                v = v * 16 + h;
                if (v > 0x10FFFF) {
                    throw error("Invalid Unicode escape", at);
                }
                i++;
                count++;
            }
            if (i >= n || count == 0) {
                throw error("Invalid Unicode escape", at);
            }
            pos = i + 1;
            return (int) v;
        }
        int v = hex4(pos + 1);
        if (v < 0) {
            if (fullUnicode) {
                throw error("Invalid Unicode escape", at);
            }
            pos++;
            return 'u';
        }
        pos += 5;
        // UnicodeMode에서는 이어지는 두 서로게이트 이스케이프를 코드 포인트 하나로 합친다.
        if (fullUnicode && v >= 0xD800 && v <= 0xDBFF && src.startsWith("\\u", pos)) {
            int low = hex4(pos + 2);
            if (low >= 0xDC00 && low <= 0xDFFF) {
                pos += 6;
                return Character.toCodePoint((char) v, (char) low);
            }
        }
        return v;
    }

    /**
     * A parsed class atom: a single character or a predefined set.
     * <p>
     * 해석한 클래스 원소: 문자 하나 또는 미리 정의된 집합.
     */
    private static final class ClassAtom {
        /**
         * The character, or -1 if this atom is a set.
         * <p>
         * 문자. 집합이면 -1.
         */
        int ch = -1;
        /**
         * The set for \d \D \s \S \w \W, else null.
         * <p>
         * \d \D \s \S \w \W 의 집합. 아니면 null.
         */
        RegExpCharSet set;
    }

    /**
     * Parses a character class starting at the opening bracket.
     * <p>
     * 여는 대괄호에서 시작하는 문자 클래스를 해석한다.
     *
     * @return SET node / SET 노드
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private RegExpNode parseClass() throws RegExpSyntaxException {
        int open = pos;
        pos++;
        RegExpCharSet set = new RegExpCharSet();
        if (pos < n && src.charAt(pos) == '^') {
            set.negated = true;
            pos++;
        }
        // 닫는 대괄호까지 클래스 원소와 범위를 읽는다.
        while (true) {
            if (pos >= n) {
                throw error("Unterminated character class", open);
            }
            if (src.charAt(pos) == ']') {
                pos++;
                break;
            }
            ClassAtom a = parseClassAtom();
            if (pos + 1 < n && src.charAt(pos) == '-' && src.charAt(pos + 1) != ']') {
                pos++;
                ClassAtom b = parseClassAtom();
                if (a.set == null && b.set == null) {
                    if (a.ch > b.ch) {
                        throw error("Range out of order in character class", open);
                    }
                    set.addRange(a.ch, b.ch);
                } else {
                    if (unicode) {
                        throw error("Invalid character class", open);
                    }
                    // 부속서 B: 클래스 escape 가 범위 끝이면 '-' 를 리터럴로 취급한다.
                    addAtom(set, a);
                    set.addChar('-');
                    addAtom(set, b);
                }
            } else {
                addAtom(set, a);
            }
        }
        set.normalize();
        RegExpNode node = new RegExpNode(RegExpNode.SET);
        node.set = set;
        return node;
    }

    /**
     * Adds a class atom to a set.
     * <p>
     * 클래스 원소를 집합에 추가한다.
     *
     * @param set target set / 대상 집합
     * @param a   atom / 원소
     */
    private void addAtom(RegExpCharSet set, ClassAtom a) {
        if (a.set != null) {
            set.addAll(a.set);
        } else {
            set.addChar(a.ch);
        }
    }

    /**
     * Parses one class atom (ClassAtom / ClassEscape).
     * <p>
     * 클래스 원소 하나(ClassAtom / ClassEscape)를 해석한다.
     *
     * @return the atom / 원소
     * @throws RegExpSyntaxException on error / 오류 시
     */
    private ClassAtom parseClassAtom() throws RegExpSyntaxException {
        ClassAtom a = new ClassAtom();
        if (src.charAt(pos) != '\\') {
            a.ch = nextChar();
            return a;
        }
        pos++;
        if (pos >= n) {
            throw error("\\ at end of pattern", pos);
        }
        char e = src.charAt(pos);
        switch (e) {
        case 'd':
            pos++;
            a.set = RegExpCharSet.digits();
            return a;
        case 'D':
            pos++;
            a.set = RegExpCharSet.digits().complement(maxChar());
            return a;
        case 's':
            pos++;
            a.set = RegExpCharSet.space();
            return a;
        case 'S':
            pos++;
            a.set = RegExpCharSet.space().complement(maxChar());
            return a;
        case 'w':
            pos++;
            a.set = RegExpCharSet.word();
            return a;
        case 'W':
            pos++;
            a.set = RegExpCharSet.word().complement(maxChar());
            return a;
        case 'p':
        case 'P':
            a.set = parseUnicodeProperty(e == 'P');
            return a;
        case 'b':
            pos++;
            a.ch = 0x08;
            return a;
        default:
            break;
        }
        if (e >= '0' && e <= '9') {
            if (unicode) {
                if (e == '0' && !(pos + 1 < n && src.charAt(pos + 1) >= '0' && src.charAt(pos + 1) <= '9')) {
                    pos++;
                    a.ch = 0;
                    return a;
                }
                throw error("Invalid class escape", pos);
            }
            if (e >= '8') {
                pos++;
                a.ch = e;
            } else {
                a.ch = parseLegacyOctal();
            }
            return a;
        }
        a.ch = parseCharacterEscape(true);
        return a;
    }
}
