package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Compiles an ECMAScript regular expression into a {@link RegExpProgram}: the pattern is parsed into a syntax tree
 * by {@link RegExpParser} and translated to the instruction set interpreted by the backtracking matcher.
 * <p>
 * Translation rules (ECMA-262 21.2.2): alternation and quantifiers become split/jump instructions; a quantifier whose
 * atom is a single character or class becomes one REPEAT instruction that needs constant backtracking memory; other
 * quantifiers use counted loops with an empty-iteration check (an iteration that matches the empty string after the
 * minimum count is a failure) and reset the captures of the group range at the start of every iteration; groups store
 * their capture only when they complete; lookaheads are atomic (they leave only their captures behind).
 * <p>
 * Use {@link #compile(String, int)} or {@link #compile(String, String)}; {@link #validate(String, String)} only checks
 * the early errors of a regular expression literal. All methods are static and thread-safe.
 * <p>
 * ECMAScript 정규식을 {@link RegExpProgram} 으로 컴파일한다. {@link RegExpParser} 가 패턴을 구문 트리로 해석하고, 이를 역추적
 * 일치 검색기가 해석하는 명령으로 번역한다.
 * <p>
 * 번역 규칙(ECMA-262 21.2.2): 선택과 수량자는 분기/점프 명령이 된다. 원자가 문자 하나나 클래스인 수량자는 일정한 역추적 메모리만 쓰는 REPEAT 명령
 * 하나가 된다. 그 밖의 수량자는 빈 반복 검사(최솟값을 채운 뒤 빈 문자열과 일치한 반복은 실패)가 있는 횟수 반복문을 쓰고 반복이 시작될 때마다 그룹 범위의
 * 캡처를 초기화한다. 그룹은 끝났을 때만 캡처를 저장한다. 선행 탐색은 원자적이어서 캡처만 남긴다.
 * <p>
 * {@link #compile(String, int)} 또는 {@link #compile(String, String)} 을 쓴다. {@link #validate(String, String)} 은 정규식 리터럴의
 * 조기 오류만 검사한다. 모든 메서드는 정적이며 스레드 안전하다.
 */
public final class RegExpCompiler {

    /**
     * Largest program size in ints; larger patterns are rejected as too large.
     * <p>
     * 프로그램 크기(int 개수) 상한. 더 큰 패턴은 너무 크다며 거부한다.
     */
    private static final int MAX_CODE_SIZE = 4000000;

    /**
     * True if the i flag is set.
     * <p>
     * i 플래그가 있으면 true.
     */
    private final boolean ignoreCase;

    /**
     * True if the m flag is set.
     * <p>
     * m 플래그가 있으면 true.
     */
    private final boolean multiline;

    /**
     * True if the u flag is set.
     * <p>
     * u 플래그가 있으면 true.
     */
    private final boolean unicode;

    /**
     * True if the s flag is set.
     * <p>
     * s 플래그가 있으면 true.
     */
    private final boolean dotAll;

    /**
     * Flag bits passed to subprograms.
     * <p>
     * 하위 프로그램에 넘기는 플래그 비트.
     */
    private final int flags;

    /**
     * The pattern text, used for error messages.
     * <p>
     * 오류 메시지에 쓰는 패턴 문자열.
     */
    private final String source;

    /**
     * Number of capturing groups; group start registers use indexes 0..groupCount.
     * <p>
     * 캡처 그룹 수. 그룹 시작 레지스터는 0..groupCount 번을 쓴다.
     */
    private final int groupCount;

    /**
     * Instruction words generated so far.
     * <p>
     * 지금까지 생성한 명령어.
     */
    private int[] code = new int[64];

    /**
     * Number of instruction words generated so far.
     * <p>
     * 지금까지 생성한 명령어 수.
     */
    private int len;

    /**
     * Character classes referenced by SET instructions.
     * <p>
     * SET 명령이 가리키는 문자 클래스들.
     */
    private final List<RegExpCharSet> sets = new ArrayList<RegExpCharSet>();

    /**
     * Compiled lookbehind bodies.
     * <p>
     * 컴파일한 뒤쪽 탐색 본문.
     */
    private final List<RegExpProgram> lookbehinds = new ArrayList<RegExpProgram>();

    /**
     * Next free register index for loops.
     * <p>
     * 반복문이 쓸 다음 빈 레지스터 번호.
     */
    private int nextReg;

    /**
     * Creates a code generator.
     * <p>
     * 코드 생성기를 만든다.
     *
     * @param source     pattern text / 패턴 문자열
     * @param flags      flag bits / 플래그 비트
     * @param groupCount number of capturing groups / 캡처 그룹 수
     */
    private RegExpCompiler(String source, int flags, int groupCount) {
        this.source = source;
        this.flags = flags;
        this.ignoreCase = (flags & RegExpFlags.IGNORE_CASE) != 0;
        this.multiline = (flags & RegExpFlags.MULTILINE) != 0;
        this.unicode = RegExpFlags.isUnicodeMode(flags);
        this.dotAll = (flags & RegExpFlags.DOT_ALL) != 0;
        this.groupCount = groupCount;
        this.nextReg = groupCount + 1;
    }

    /**
     * Compiles a pattern with flag bits.
     * <p>
     * 플래그 비트와 함께 패턴을 컴파일한다.
     *
     * @param source pattern text (the part between the slashes) / 패턴 문자열(슬래시 사이 부분)
     * @param flags  flag bits from {@link RegExpFlags} / {@link RegExpFlags} 의 플래그 비트
     * @return the compiled program / 컴파일된 프로그램
     * @throws RegExpSyntaxException if the pattern is invalid / 패턴이 올바르지 않으면
     */
    public static RegExpProgram compile(String source, int flags) throws RegExpSyntaxException {
        int unicodeFlags = RegExpFlags.UNICODE | RegExpFlags.UNICODE_SETS;
        if ((flags & unicodeFlags) == unicodeFlags) {
            throw new RegExpSyntaxException(source, "Regular expression cannot combine u and v flags", 0);
        }
        RegExpParser parser = new RegExpParser(source, flags);
        RegExpNode root = parser.parse();
        RegExpCompiler gen = new RegExpCompiler(source, flags, parser.getGroupCount());
        gen.generate(root);
        gen.emit(RegExpProgram.OP_MATCH);
        RegExpCharSet[] setArray = gen.sets.toArray(new RegExpCharSet[gen.sets.size()]);
        String[] names = new String[parser.getGroupCount() + 1];
        // 해석기가 모은 그룹 이름을 번호 순 배열로 옮긴다.
        for (int i = 1; i < names.length && i < parser.getGroupNames().size(); i++) {
            names[i] = parser.getGroupNames().get(i);
        }
        int firstChar = -1;
        boolean unicodeMode = RegExpFlags.isUnicodeMode(flags);
        if ((flags & RegExpFlags.IGNORE_CASE) == 0) {
            int lit = firstLiteral(root);
            if (lit >= 0 && lit < 0x10000 && !(unicodeMode && lit >= 0xD800 && lit <= 0xDFFF)) {
                firstChar = lit;
            }
        }
        boolean anchored = (flags & RegExpFlags.MULTILINE) == 0 && isAnchored(root);
        return new RegExpProgram(source, flags, parser.getGroupCount(), Arrays.copyOf(gen.code, gen.len), setArray, gen.nextReg,
                names, firstChar, anchored, gen.lookbehinds.toArray(new RegExpProgram[gen.lookbehinds.size()]));
    }

    /**
     * Compiles a pattern with a flags string such as "gi".
     * <p>
     * "gi" 같은 플래그 문자열과 함께 패턴을 컴파일한다.
     *
     * @param source pattern text / 패턴 문자열
     * @param flags  flags text (null means none) / 플래그 문자열(null 은 없음)
     * @return the compiled program / 컴파일된 프로그램
     * @throws RegExpSyntaxException if the pattern or flags are invalid / 패턴이나 플래그가 올바르지 않으면
     */
    public static RegExpProgram compile(String source, String flags) throws RegExpSyntaxException {
        return compile(source, RegExpFlags.parse(flags));
    }

    /**
     * Checks the early errors of a regular expression (pattern and flags) without generating code; this is what the
     * script parser calls for a regular expression literal.
     * <p>
     * 코드를 만들지 않고 정규식(패턴과 플래그)의 조기 오류를 검사한다. 스크립트 해석기가 정규식 리터럴에 대해 호출한다.
     *
     * @param source pattern text / 패턴 문자열
     * @param flags  flags text / 플래그 문자열
     * @throws RegExpSyntaxException if the literal is invalid / 리터럴이 올바르지 않으면
     */
    public static void validate(String source, String flags) throws RegExpSyntaxException {
        int bits = RegExpFlags.parse(flags);
        new RegExpParser(source, bits).parse();
    }

    /**
     * Finds a literal character every match must start with, or -1.
     * <p>
     * 모든 일치가 반드시 시작하는 리터럴 문자를 찾는다. 없으면 -1.
     *
     * @param n syntax tree node / 구문 트리 노드
     * @return character or -1 / 문자 또는 -1
     */
    private static int firstLiteral(RegExpNode n) {
        switch (n.type) {
        case RegExpNode.CHAR:
            return n.value;
        case RegExpNode.GROUP:
            return firstLiteral(n.child);
        case RegExpNode.SEQ:
            return firstLiteral(n.children.get(0));
        case RegExpNode.REPEAT:
            return n.min >= 1 ? firstLiteral(n.child) : -1;
        default:
            return -1;
        }
    }

    /**
     * Tells whether every match must begin with the start-of-input assertion.
     * <p>
     * 모든 일치가 입력 시작 단언으로 시작해야 하는지 알려 준다.
     *
     * @param n syntax tree node / 구문 트리 노드
     * @return true if anchored at the start / 시작에 고정되어 있으면 true
     */
    private static boolean isAnchored(RegExpNode n) {
        switch (n.type) {
        case RegExpNode.ASSERT:
            return n.value == RegExpNode.BOL;
        case RegExpNode.GROUP:
            return isAnchored(n.child);
        case RegExpNode.SEQ:
            return isAnchored(n.children.get(0));
        case RegExpNode.ALT:
            // 모든 선택지가 시작에 고정되어야 전체가 고정된 것이다.
            for (int i = 0; i < n.children.size(); i++) {
                if (!isAnchored(n.children.get(i))) {
                    return false;
                }
            }
            return true;
        default:
            return false;
        }
    }

    /**
     * Appends one instruction word.
     * <p>
     * 명령어 하나를 덧붙인다.
     *
     * @param w word / 명령어
     * @return its index / 위치
     */
    private int emit(int w) {
        if (len == code.length) {
            if (len >= MAX_CODE_SIZE) {
                throw new IllegalStateException("regular expression too large");
            }
            code = Arrays.copyOf(code, len * 2);
        }
        code[len] = w;
        return len++;
    }

    /**
     * Generates code for a node.
     * <p>
     * 노드의 코드를 생성한다.
     *
     * @param node syntax tree node / 구문 트리 노드
     * @throws RegExpSyntaxException if the program becomes too large / 프로그램이 너무 커지면
     */
    private void generate(RegExpNode node) throws RegExpSyntaxException {
        try {
            gen(node);
        } catch (IllegalStateException e) {
            throw new RegExpSyntaxException(source, "Regular expression too large", -1);
        }
    }

    /**
     * Recursive code generation.
     * <p>
     * 재귀적인 코드 생성.
     *
     * @param n syntax tree node / 구문 트리 노드
     */
    private void gen(RegExpNode n) {
        switch (n.type) {
        case RegExpNode.EMPTY:
            break;
        case RegExpNode.CHAR:
            emit(RegExpProgram.OP_CHAR);
            emit(ignoreCase ? CaseFold.canonicalize(n.value, unicode) : n.value);
            break;
        case RegExpNode.ANY:
            emit(dotAll ? RegExpProgram.OP_ANY_ALL : RegExpProgram.OP_ANY);
            break;
        case RegExpNode.SET:
            emit(RegExpProgram.OP_SET);
            emit(addSet(n.set));
            break;
        case RegExpNode.SEQ:
            // 연결은 자식 코드를 순서대로 이어 붙인다.
            for (int i = 0; i < n.children.size(); i++) {
                gen(n.children.get(i));
            }
            break;
        case RegExpNode.ALT:
            genAlternation(n);
            break;
        case RegExpNode.GROUP:
            if (n.value > 0) {
                emit(RegExpProgram.OP_GROUP_START);
                emit(n.value);
                gen(n.child);
                emit(RegExpProgram.OP_GROUP_END);
                emit(n.value);
            } else {
                gen(n.child);
            }
            break;
        case RegExpNode.BACKREF:
            emit(RegExpProgram.OP_BACKREF);
            emit(n.value);
            break;
        case RegExpNode.ASSERT:
            genAssertion(n);
            break;
        case RegExpNode.LOOK: {
            emit(RegExpProgram.OP_LOOK_START);
            emit(n.value);
            int contSlot = emit(0);
            gen(n.child);
            emit(RegExpProgram.OP_LOOK_END);
            emit(n.value);
            code[contSlot] = len;
            break;
        }
        case RegExpNode.LOOK_BEHIND:
            emit(RegExpProgram.OP_LOOK_BEHIND);
            emit(n.value);
            emit(compileLookbehind(n.child));
            break;
        case RegExpNode.REPEAT:
            genRepeat(n);
            break;
        default:
            throw new IllegalStateException("unknown node " + n.type);
        }
    }

    /**
     * Adds a character class to the table and returns its index.
     * <p>
     * 문자 클래스를 표에 추가하고 번호를 반환한다.
     *
     * @param s class / 클래스
     * @return index / 번호
     */
    private int addSet(RegExpCharSet s) {
        sets.add(s);
        return sets.size() - 1;
    }

    /**
     * Generates an anchor or word boundary assertion.
     * <p>
     * 위치 단언 또는 단어 경계 단언을 생성한다.
     *
     * @param n ASSERT node / ASSERT 노드
     */
    private void genAssertion(RegExpNode n) {
        switch (n.value) {
        case RegExpNode.BOL:
            emit(multiline ? RegExpProgram.OP_BOL_M : RegExpProgram.OP_BOL);
            break;
        case RegExpNode.EOL:
            emit(multiline ? RegExpProgram.OP_EOL_M : RegExpProgram.OP_EOL);
            break;
        case RegExpNode.WORD_BOUNDARY:
            emit(RegExpProgram.OP_WORD_B);
            break;
        default:
            emit(RegExpProgram.OP_NOT_WORD_B);
            break;
        }
    }

    /**
     * Generates an alternation: try each alternative in order, remembering the next as a backtrack point.
     * <p>
     * 선택을 생성한다: 선택지를 차례로 시도하고 다음 선택지를 역추적 지점으로 기억한다.
     *
     * @param n ALT node / ALT 노드
     */
    private void genAlternation(RegExpNode n) {
        int count = n.children.size();
        int[] jumpSlots = new int[count];
        int jumps = 0;
        // 마지막을 뺀 선택지마다 분기 명령을 두고, 성공하면 끝으로 건너뛴다.
        for (int i = 0; i < count; i++) {
            if (i < count - 1) {
                emit(RegExpProgram.OP_SPLIT);
                int firstSlot = emit(0);
                int secondSlot = emit(0);
                code[firstSlot] = len;
                gen(n.children.get(i));
                emit(RegExpProgram.OP_JMP);
                jumpSlots[jumps++] = emit(0);
                code[secondSlot] = len;
            } else {
                gen(n.children.get(i));
            }
        }
        // 각 선택지 끝의 점프 목적지를 전체 끝으로 채운다.
        for (int i = 0; i < jumps; i++) {
            code[jumpSlots[i]] = len;
        }
    }

    /**
     * Removes non-capturing groups around a node.
     * <p>
     * 노드를 감싼 비캡처 그룹을 벗겨 낸다.
     *
     * @param n node / 노드
     * @return the inner node / 안쪽 노드
     */
    private static RegExpNode unwrap(RegExpNode n) {
        RegExpNode cur = n;
        // 캡처하지 않는 그룹이 겹쳐 있으면 안쪽까지 들어간다.
        while (cur.type == RegExpNode.GROUP && cur.value == 0) {
            cur = cur.child;
        }
        return cur;
    }

    /**
     * Generates a quantifier.
     * <p>
     * 수량자를 생성한다.
     *
     * @param n REPEAT node / REPEAT 노드
     */
    private void genRepeat(RegExpNode n) {
        if (n.max == 0) {
            return;
        }
        RegExpNode atom = unwrap(n.child);
        boolean greedy = n.greedy;
        if (atom.type == RegExpNode.CHAR || atom.type == RegExpNode.ANY || atom.type == RegExpNode.SET) {
            int kind;
            int arg = 0;
            if (atom.type == RegExpNode.CHAR) {
                kind = RegExpProgram.KIND_CHAR;
                arg = ignoreCase ? CaseFold.canonicalize(atom.value, unicode) : atom.value;
            } else if (atom.type == RegExpNode.ANY) {
                kind = dotAll ? RegExpProgram.KIND_ANY_ALL : RegExpProgram.KIND_ANY;
            } else {
                kind = RegExpProgram.KIND_SET;
                arg = addSet(atom.set);
            }
            emit(RegExpProgram.OP_REPEAT);
            emit(kind);
            emit(arg);
            emit(n.min);
            emit(n.max);
            emit(greedy ? 1 : 0);
            return;
        }
        boolean canBeEmpty = n.child.minLength() == 0;
        boolean hasGroups = n.firstGroup <= n.lastGroup;
        if (n.min == 0 && n.max == 1 && !canBeEmpty) {
            emit(RegExpProgram.OP_SPLIT);
            int firstSlot = emit(0);
            int secondSlot = emit(0);
            int bodyStart = len;
            gen(n.child);
            int exit = len;
            code[firstSlot] = greedy ? bodyStart : exit;
            code[secondSlot] = greedy ? exit : bodyStart;
            return;
        }
        if (n.min == 0 && n.max == RegExpNode.INFINITE && !canBeEmpty) {
            int head = emit(RegExpProgram.OP_SPLIT);
            int firstSlot = emit(0);
            int secondSlot = emit(0);
            int bodyStart = len;
            if (hasGroups) {
                emit(RegExpProgram.OP_RESET_CAPS);
                emit(n.firstGroup);
                emit(n.lastGroup);
            }
            gen(n.child);
            emit(RegExpProgram.OP_JMP);
            emit(head);
            int exit = len;
            code[firstSlot] = greedy ? bodyStart : exit;
            code[secondSlot] = greedy ? exit : bodyStart;
            return;
        }
        int reg = nextReg;
        nextReg += 2;
        emit(RegExpProgram.OP_SET_REG);
        emit(reg);
        emit(0);
        int head = emit(RegExpProgram.OP_LOOP);
        emit(reg);
        emit(n.min);
        emit(n.max);
        emit(greedy ? 1 : 0);
        int exitSlot = emit(0);
        emit(RegExpProgram.OP_ITER_START);
        emit(reg + 1);
        if (hasGroups) {
            emit(RegExpProgram.OP_RESET_CAPS);
            emit(n.firstGroup);
            emit(n.lastGroup);
        }
        gen(n.child);
        emit(RegExpProgram.OP_LOOP_END);
        emit(reg);
        emit(reg + 1);
        emit(n.min);
        emit(head);
        code[exitSlot] = len;
    }

    /**
     * Compiles a lookbehind body as an independent matcher with the same capture numbering and flags.
     * <p>
     * 같은 캡처 번호와 플래그로 뒤쪽 탐색 본문을 독립 일치 검색기로 컴파일한다.
     *
     * @param body assertion body / 단언 본문
     * @return subprogram index / 하위 프로그램 번호
     */
    private int compileLookbehind(RegExpNode body) {
        RegExpCompiler gen = new RegExpCompiler(source, flags, groupCount);
        try {
            gen.generate(body);
        } catch (RegExpSyntaxException e) {
            throw new IllegalStateException(e.getMessage());
        }
        gen.emit(RegExpProgram.OP_MATCH);
        RegExpProgram program = new RegExpProgram(source, flags, groupCount, Arrays.copyOf(gen.code, gen.len),
                gen.sets.toArray(new RegExpCharSet[gen.sets.size()]), gen.nextReg, new String[groupCount + 1], -1, false,
                gen.lookbehinds.toArray(new RegExpProgram[gen.lookbehinds.size()]));
        lookbehinds.add(program);
        return lookbehinds.size() - 1;
    }
}
