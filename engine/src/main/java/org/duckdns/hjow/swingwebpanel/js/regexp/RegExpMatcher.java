package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.Arrays;

/**
 * The backtracking virtual machine that executes a {@link RegExpProgram} on one input string. One matcher serves
 * the attempts of a single <code>exec</code> call; it is not thread-safe.
 * <p>
 * State changes that must be undone when the search backtracks (captures and registers) are logged on an explicit stack,
 * so deep recursion is never used and long inputs cannot overflow the Java stack. The stack holds fixed size entries of
 * four ints: a type tag and three operands.
 * <p>
 * Backtrack entry types: BR (resume at pc with pos), CAP and REG (restore a capture slot or register), GREEDY (a
 * single-character repeat that can give back characters), LAZY (a lazy single-character repeat that can take one more
 * character) and LOOK (a lookahead marker). A completed positive lookahead drops the alternatives made inside it (it is
 * atomic) but keeps the undo records of its captures; a negative lookahead undoes everything made inside it.
 * <p>
 * 하나의 입력 문자열에서 {@link RegExpProgram} 을 실행하는 역추적 가상 기계. 일치 검색기 하나가 <code>exec</code> 한 번의 시도들을 맡으며
 * 스레드 안전하지 않다.
 * <p>
 * 역추적할 때 되돌려야 하는 상태 변경(캡처와 레지스터)은 명시적인 스택에 기록하므로 깊은 재귀를 쓰지 않으며 긴 입력이 Java 스택을 넘치게 하지 않는다.
 * 스택은 종류 표시와 피연산자 세 개로 이루어진 int 네 개짜리 고정 크기 항목을 담는다.
 * <p>
 * 역추적 항목 종류: BR(pc 와 pos 로 재개), CAP 와 REG(캡처 칸이나 레지스터 복원), GREEDY(문자를 돌려줄 수 있는 단일 문자 반복),
 * LAZY(문자를 하나 더 가져갈 수 있는 게으른 단일 문자 반복), LOOK(선행 탐색 표지). 끝난 긍정 선행 탐색은 안에서 만든 대안들을 버리고(원자적)
 * 캡처의 되돌림 기록은 남긴다. 부정 선행 탐색은 안에서 한 모든 일을 되돌린다.
 */
final class RegExpMatcher {

    /**
     * Entry type: branch alternative (operands: pc, pos).
     * <p>
     * 항목 종류: 분기 대안(피연산자: pc, pos).
     */
    private static final int BT_BR = 0;

    /**
     * Entry type: restore a capture slot (operands: slot, old value).
     * <p>
     * 항목 종류: 캡처 칸 복원(피연산자: 칸, 이전 값).
     */
    private static final int BT_CAP = 1;

    /**
     * Entry type: restore a register (operands: register, old value).
     * <p>
     * 항목 종류: 레지스터 복원(피연산자: 레지스터, 이전 값).
     */
    private static final int BT_REG = 2;

    /**
     * Entry type: greedy single-character repeat (operands: continuation pc, minimum position, current end).
     * <p>
     * 항목 종류: 탐욕적 단일 문자 반복(피연산자: 이어질 pc, 최소 위치, 현재 끝).
     */
    private static final int BT_GREEDY = 3;

    /**
     * Entry type: lazy single-character repeat (operands: pc of the repeat instruction, position, count).
     * <p>
     * 항목 종류: 게으른 단일 문자 반복(피연산자: 반복 명령의 pc, 위치, 횟수).
     */
    private static final int BT_LAZY = 4;

    /**
     * Entry type: lookahead marker (operands: continuation pc, start position, negative flag).
     * <p>
     * 항목 종류: 선행 탐색 표지(피연산자: 이어질 pc, 시작 위치, 부정 여부).
     */
    private static final int BT_LOOK = 5;

    /**
     * Step count mask for polling the cancellation hook (every 1024 steps).
     * <p>
     * 취소 훅을 확인하는 단계 수 마스크(1024 단계마다).
     */
    private static final int CANCEL_MASK = 1023;

    /**
     * The program being executed.
     * <p>
     * 실행 중인 프로그램.
     */
    private final RegExpProgram prog;

    /**
     * The instruction words.
     * <p>
     * 명령어.
     */
    private final int[] code;

    /**
     * The input string.
     * <p>
     * 입력 문자열.
     */
    private final String in;

    /**
     * Length of the input.
     * <p>
     * 입력 길이.
     */
    private final int end;

    /**
     * True if the u flag is set.
     * <p>
     * u 플래그가 있으면 true.
     */
    private final boolean unicode;

    /**
     * True if the i flag is set.
     * <p>
     * i 플래그가 있으면 true.
     */
    private final boolean ignoreCase;

    /**
     * Step budget.
     * <p>
     * 단계 예산.
     */
    private final long stepLimit;

    /**
     * Maximum number of stack entries.
     * <p>
     * 스택 항목 최대 개수.
     */
    private final int maxEntries;

    /**
     * Cancellation hook or null.
     * <p>
     * 취소 훅 또는 null.
     */
    private final RegExpCancel cancel;

    /**
     * Capture slots: start and end per group.
     * <p>
     * 캡처 칸: 그룹별 시작과 끝.
     */
    private final int[] caps;

    /**
     * Registers: group start positions, loop counters and loop positions.
     * <p>
     * 레지스터: 그룹 시작 위치, 반복 횟수, 반복 위치.
     */
    private final int[] regs;

    /**
     * The backtracking stack.
     * <p>
     * 역추적 스택.
     */
    private int[] stack = new int[256];

    /**
     * Number of ints in use on the stack.
     * <p>
     * 스택에서 쓰는 int 수.
     */
    private int sp;

    /**
     * Steps used so far by this matcher.
     * <p>
     * 이 일치 검색기가 지금까지 쓴 단계 수.
     */
    private long steps;

    /**
     * Program counter.
     * <p>
     * 프로그램 카운터.
     */
    private int pc;

    /**
     * Current input position.
     * <p>
     * 현재 입력 위치.
     */
    private int pos;

    /**
     * Required end position, or -1 for an ordinary match.
     * <p>
     * 필요한 끝 위치. 일반 일치이면 -1.
     */
    private int requiredEnd = -1;

    /**
     * Creates a matcher for one execution.
     * <p>
     * 실행 한 번을 위한 일치 검색기를 만든다.
     *
     * @param prog   program / 프로그램
     * @param input  input string / 입력 문자열
     * @param cancel cancellation hook or null / 취소 훅 또는 null
     */
    RegExpMatcher(RegExpProgram prog, String input, RegExpCancel cancel) {
        this.prog = prog;
        this.code = prog.code;
        this.in = input;
        this.end = input.length();
        this.unicode = RegExpFlags.isUnicodeMode(prog.getFlags());
        this.ignoreCase = (prog.getFlags() & RegExpFlags.IGNORE_CASE) != 0;
        this.stepLimit = prog.getStepLimit();
        this.maxEntries = prog.getMaxBacktrackEntries();
        this.cancel = cancel;
        this.caps = new int[2 * (prog.groupCount() + 1)];
        this.regs = new int[prog.registerCount];
    }

    /**
     * Returns a copy of the capture offsets of the last successful attempt.
     * <p>
     * 마지막으로 성공한 시도의 캡처 위치 복사본을 반환한다.
     *
     * @return capture offsets / 캡처 위치
     */
    int[] result() {
        return caps.clone();
    }

    /**
     * Throws if the step budget is exhausted or the hook cancels.
     * <p>
     * 단계 예산을 다 썼거나 훅이 취소하면 예외를 던진다.
     */
    private void checkLimits() {
        if (steps > stepLimit) {
            throw new RegExpLimitException("Regular expression too complex (step limit " + stepLimit + " exceeded)", false);
        }
        if (cancel != null && cancel.isCancelled()) {
            throw new RegExpLimitException("Regular expression match cancelled", true);
        }
    }

    /**
     * Pushes one backtracking entry.
     * <p>
     * 역추적 항목 하나를 쌓는다.
     *
     * @param type entry type / 항목 종류
     * @param a    operand 1 / 피연산자 1
     * @param b    operand 2 / 피연산자 2
     * @param c    operand 3 / 피연산자 3
     */
    private void push(int type, int a, int b, int c) {
        if (sp + 4 > stack.length) {
            if (stack.length / 4 >= maxEntries) {
                throw new RegExpLimitException("Regular expression too complex (backtracking stack limit exceeded)", false);
            }
            stack = Arrays.copyOf(stack, stack.length * 2);
        }
        stack[sp] = type;
        stack[sp + 1] = a;
        stack[sp + 2] = b;
        stack[sp + 3] = c;
        sp += 4;
    }

    /**
     * Sets a capture slot, logging the old value for backtracking.
     * <p>
     * 캡처 칸을 정하고 역추적을 위해 이전 값을 기록한다.
     *
     * @param slot slot index / 칸 번호
     * @param v    new value / 새 값
     */
    private void setCap(int slot, int v) {
        if (caps[slot] != v) {
            push(BT_CAP, slot, caps[slot], 0);
            caps[slot] = v;
        }
    }

    /**
     * Sets a register, logging the old value for backtracking.
     * <p>
     * 레지스터를 정하고 역추적을 위해 이전 값을 기록한다.
     *
     * @param reg register index / 레지스터 번호
     * @param v   new value / 새 값
     */
    private void setReg(int reg, int v) {
        if (regs[reg] != v) {
            push(BT_REG, reg, regs[reg], 0);
            regs[reg] = v;
        }
    }

    /**
     * Reads the character at a position: a code point with the u flag, else a code unit.
     * <p>
     * 위치의 문자를 읽는다. u 플래그가 있으면 코드 포인트, 아니면 코드 단위.
     *
     * @param p position (less than the input length) / 위치(입력 길이 미만)
     * @return the character / 문자
     */
    private int charAt(int p) {
        char c = in.charAt(p);
        if (unicode && c >= 0xD800 && c <= 0xDBFF && p + 1 < end) {
            char d = in.charAt(p + 1);
            if (d >= 0xDC00 && d <= 0xDFFF) {
                return Character.toCodePoint(c, d);
            }
        }
        return c;
    }

    /**
     * Tests the line terminator characters of ECMAScript.
     * <p>
     * ECMAScript 의 줄 끝 문자인지 검사한다.
     *
     * @param c character / 문자
     * @return true for LF, CR, LS, PS / LF, CR, LS, PS 이면 true
     */
    private static boolean isLineTerminator(int c) {
        return c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029;
    }

    /**
     * Tests the word characters used by \b: ASCII letters, digits and underscore.
     * <p>
     * \b 가 쓰는 단어 문자인지 검사한다: ASCII 글자, 숫자, 밑줄.
     *
     * @param c character / 문자
     * @return true for a word character / 단어 문자이면 true
     */
    private static boolean isWord(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    /**
     * Tests whether a character is in a class, applying case-insensitivity and negation.
     * <p>
     * 문자가 클래스에 속하는지 대소문자 무시와 부정을 적용해 검사한다.
     *
     * @param set class / 클래스
     * @param c   character / 문자
     * @return true if the class matches / 클래스가 일치하면 true
     */
    private boolean setMatches(RegExpCharSet set, int c) {
        boolean found = set.contains(c);
        if (!found && ignoreCase) {
            if (!unicode && c < 128) {
                // 비유니코드 모드의 ASCII 글자는 대소문자 짝만 확인하면 된다.
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    found = set.contains(c ^ 0x20);
                }
            } else {
                int[] eq = CaseFold.equivalents(c, unicode);
                // 같은 정규형을 가진 다른 문자 중 클래스에 있는 것이 있는지 확인한다.
                for (int i = 0; i < eq.length; i++) {
                    if (set.contains(eq[i])) {
                        found = true;
                        break;
                    }
                }
            }
        }
        return found != set.negated;
    }

    /**
     * Matches one single-character atom at a position.
     * <p>
     * 위치에서 단일 문자 원자 하나를 일치시킨다.
     *
     * @param kind atom kind (KIND_ constants of {@link RegExpProgram}) / 원자 종류({@link RegExpProgram} 의 KIND_ 상수)
     * @param arg  character or class index / 문자 또는 클래스 번호
     * @param p    position / 위치
     * @return position after the atom, or -1 if it does not match / 원자 뒤의 위치. 일치하지 않으면 -1
     */
    private int matchAtom(int kind, int arg, int p) {
        if (p >= end) {
            return -1;
        }
        int c = charAt(p);
        int next = p + (c > 0xFFFF ? 2 : 1);
        switch (kind) {
        case RegExpProgram.KIND_CHAR:
            return (ignoreCase ? CaseFold.canonicalize(c, unicode) : c) == arg ? next : -1;
        case RegExpProgram.KIND_ANY:
            return isLineTerminator(c) ? -1 : next;
        case RegExpProgram.KIND_ANY_ALL:
            return next;
        default:
            return setMatches(prog.sets[arg], c) ? next : -1;
        }
    }

    /**
     * Steps back over one character of a single-character repeat, never going before its minimum position.
     * <p>
     * 단일 문자 반복에서 문자 하나만큼 되돌아간다. 최소 위치보다 앞으로는 가지 않는다.
     *
     * @param cur    current position / 현재 위치
     * @param minPos minimum position / 최소 위치
     * @return the earlier position / 앞선 위치
     */
    private int stepBack(int cur, int minPos) {
        if (unicode && cur - 2 >= minPos && Character.isLowSurrogate(in.charAt(cur - 1)) && Character.isHighSurrogate(in.charAt(cur - 2))) {
            return cur - 2;
        }
        return cur - 1;
    }

    /**
     * Pops entries until an alternative can be resumed.
     * <p>
     * 재개할 수 있는 대안이 나올 때까지 항목을 꺼낸다.
     *
     * @return true if an alternative was resumed (pc and pos are set), false if the attempt failed / 대안을 재개했으면 true(pc, pos 설정됨), 시도가 실패했으면 false
     */
    private boolean backtrack() {
        // 스택 맨 위부터 되돌림 기록은 적용하고, 재개할 수 있는 항목을 만나면 멈춘다.
        while (sp > 0) {
            sp -= 4;
            steps++;
            switch (stack[sp]) {
            case BT_BR:
                pc = stack[sp + 1];
                pos = stack[sp + 2];
                return true;
            case BT_CAP:
                caps[stack[sp + 1]] = stack[sp + 2];
                break;
            case BT_REG:
                regs[stack[sp + 1]] = stack[sp + 2];
                break;
            case BT_GREEDY: {
                int cont = stack[sp + 1];
                int minPos = stack[sp + 2];
                int np = stepBack(stack[sp + 3], minPos);
                if (np > minPos) {
                    stack[sp + 3] = np;
                    sp += 4;
                }
                pc = cont;
                pos = np;
                return true;
            }
            case BT_LAZY: {
                int at = stack[sp + 1];
                int cur = stack[sp + 2];
                int count = stack[sp + 3];
                int np = matchAtom(code[at + 1], code[at + 2], cur);
                if (np >= 0) {
                    steps++;
                    if (count + 1 < code[at + 4]) {
                        push(BT_LAZY, at, np, count + 1);
                    }
                    pc = at + 6;
                    pos = np;
                    return true;
                }
                break;
            }
            default:
                if (stack[sp + 3] == 1) {
                    pc = stack[sp + 1];
                    pos = stack[sp + 2];
                    return true;
                }
                break;
            }
        }
        return false;
    }

    /**
     * Tries to match the program at exactly one start position.
     * <p>
     * 정확히 한 시작 위치에서 프로그램이 일치하는지 시도한다.
     *
     * @param start start position / 시작 위치
     * @return true if matched (see {@link #result()}) / 일치했으면 true({@link #result()} 참조)
     * @throws RegExpLimitException if a resource limit is exceeded / 자원 제한을 넘으면
     */
    boolean matchAt(int start) {
        requiredEnd = -1;
        return run(start);
    }

    /**
     * Tries to match and backtracks until the successful path ends exactly at the requested position.
     * <p>
     * 성공 경로가 요청한 위치에서 정확히 끝날 때까지 역추적한다.
     *
     * @param start start position / 시작 위치
     * @param end required end / 필요한 끝 위치
     * @return true if matched / 일치하면 true
     */
    boolean matchAtEnding(int start, int end) {
        requiredEnd = end;
        return run(start);
    }

    /**
     * Executes a match after its end constraint has been selected.
     * <p>
     * 끝 조건을 정한 뒤 일치를 실행한다.
     *
     * @param start start position / 시작 위치
     * @return true if matched / 일치하면 true
     */
    private boolean run(int start) {
        Arrays.fill(caps, -1);
        Arrays.fill(regs, 0);
        sp = 0;
        pc = 0;
        pos = start;
        caps[0] = start;
        // 명령을 하나씩 실행한다. 실패하면 역추적하여 다음 대안에서 이어 간다.
        while (true) {
            steps++;
            if (steps > stepLimit || (steps & CANCEL_MASK) == 0) {
                checkLimits();
            }
            boolean ok = true;
            switch (code[pc]) {
            case RegExpProgram.OP_MATCH:
                if (requiredEnd < 0 || pos == requiredEnd) {
                    caps[1] = pos;
                    return true;
                }
                ok = false;
                break;
            case RegExpProgram.OP_CHAR: {
                int np = matchAtom(RegExpProgram.KIND_CHAR, code[pc + 1], pos);
                if (np < 0) {
                    ok = false;
                } else {
                    pos = np;
                    pc += 2;
                }
                break;
            }
            case RegExpProgram.OP_ANY: {
                int np = matchAtom(RegExpProgram.KIND_ANY, 0, pos);
                if (np < 0) {
                    ok = false;
                } else {
                    pos = np;
                    pc++;
                }
                break;
            }
            case RegExpProgram.OP_ANY_ALL: {
                int np = matchAtom(RegExpProgram.KIND_ANY_ALL, 0, pos);
                if (np < 0) {
                    ok = false;
                } else {
                    pos = np;
                    pc++;
                }
                break;
            }
            case RegExpProgram.OP_SET: {
                int np = matchAtom(RegExpProgram.KIND_SET, code[pc + 1], pos);
                if (np < 0) {
                    ok = false;
                } else {
                    pos = np;
                    pc += 2;
                }
                break;
            }
            case RegExpProgram.OP_SPLIT:
                push(BT_BR, code[pc + 2], pos, 0);
                pc = code[pc + 1];
                break;
            case RegExpProgram.OP_JMP:
                pc = code[pc + 1];
                break;
            case RegExpProgram.OP_GROUP_START:
                setReg(code[pc + 1], pos);
                pc += 2;
                break;
            case RegExpProgram.OP_GROUP_END: {
                int g = code[pc + 1];
                setCap(2 * g, regs[g]);
                setCap(2 * g + 1, pos);
                pc += 2;
                break;
            }
            case RegExpProgram.OP_RESET_CAPS:
                // 그룹 범위의 캡처를 모두 지운다.
                for (int g = code[pc + 1]; g <= code[pc + 2]; g++) {
                    setCap(2 * g, -1);
                    setCap(2 * g + 1, -1);
                }
                pc += 3;
                break;
            case RegExpProgram.OP_BOL:
                if (pos == 0) {
                    pc++;
                } else {
                    ok = false;
                }
                break;
            case RegExpProgram.OP_BOL_M:
                if (pos == 0 || isLineTerminator(in.charAt(pos - 1))) {
                    pc++;
                } else {
                    ok = false;
                }
                break;
            case RegExpProgram.OP_EOL:
                if (pos == end) {
                    pc++;
                } else {
                    ok = false;
                }
                break;
            case RegExpProgram.OP_EOL_M:
                if (pos == end || isLineTerminator(in.charAt(pos))) {
                    pc++;
                } else {
                    ok = false;
                }
                break;
            case RegExpProgram.OP_WORD_B:
            case RegExpProgram.OP_NOT_WORD_B: {
                boolean a = pos > 0 && isWord(in.charAt(pos - 1));
                boolean b = pos < end && isWord(in.charAt(pos));
                boolean boundary = a != b;
                if (boundary == (code[pc] == RegExpProgram.OP_WORD_B)) {
                    pc++;
                } else {
                    ok = false;
                }
                break;
            }
            case RegExpProgram.OP_BACKREF: {
                int np = matchBackReference(code[pc + 1]);
                if (np < 0) {
                    ok = false;
                } else {
                    pos = np;
                    pc += 2;
                }
                break;
            }
            case RegExpProgram.OP_LOOK_START:
                push(BT_LOOK, code[pc + 2], pos, code[pc + 1]);
                pc += 3;
                break;
            case RegExpProgram.OP_LOOK_END:
                if (code[pc + 1] == 0) {
                    finishPositiveLookahead();
                    pc += 2;
                } else {
                    undoToLookMarker();
                    ok = false;
                }
                break;
            case RegExpProgram.OP_LOOK_BEHIND:
                if (executeLookbehind(code[pc + 1] == 1, prog.lookbehinds[code[pc + 2]])) {
                    pc += 3;
                } else {
                    ok = false;
                }
                break;
            case RegExpProgram.OP_SET_REG:
                setReg(code[pc + 1], code[pc + 2]);
                pc += 3;
                break;
            case RegExpProgram.OP_LOOP: {
                int count = regs[code[pc + 1]];
                int min = code[pc + 2];
                int max = code[pc + 3];
                boolean greedy = code[pc + 4] == 1;
                int exit = code[pc + 5];
                if (count < min) {
                    pc += 6;
                } else if (max != RegExpNode.INFINITE && count >= max) {
                    pc = exit;
                } else if (greedy) {
                    push(BT_BR, exit, pos, 0);
                    pc += 6;
                } else {
                    push(BT_BR, pc + 6, pos, 0);
                    pc = exit;
                }
                break;
            }
            case RegExpProgram.OP_ITER_START:
                setReg(code[pc + 1], pos);
                pc += 2;
                break;
            case RegExpProgram.OP_LOOP_END: {
                int reg = code[pc + 1];
                int count = regs[reg];
                // 최솟값을 채운 뒤의 반복이 빈 문자열과 일치했다면 실패로 본다.
                if (count >= code[pc + 3] && pos == regs[code[pc + 2]]) {
                    ok = false;
                } else {
                    setReg(reg, count + 1);
                    pc = code[pc + 4];
                }
                break;
            }
            case RegExpProgram.OP_REPEAT:
                ok = executeRepeat();
                break;
            default:
                throw new IllegalStateException("bad instruction " + code[pc]);
            }
            if (!ok && !backtrack()) {
                return false;
            }
        }
    }

    /**
     * Executes one lookbehind assertion and copies its captures on success.
     * <p>
     * 뒤쪽 탐색 단언 하나를 실행하고 성공하면 캡처를 복사한다.
     *
     * @param negative true for negative lookbehind / 부정 뒤쪽 탐색이면 true
     * @param body compiled assertion body / 컴파일된 단언 본문
     * @return assertion result / 단언 결과
     */
    private boolean executeLookbehind(boolean negative, RegExpProgram body) {
        // 가능한 시작점마다 본문이 현재 위치에서 정확히 끝나는지 검사한다.
        for (int start = 0; start <= pos; ) {
            int[] found = body.matchAtEnding(in, start, pos, cancel);
            if (found != null) {
                if (negative) {
                    return false;
                }
                // 성공한 본문의 캡처 위치를 바깥 일치에 역추적 가능하게 복사한다.
                for (int i = 2; i < found.length && i < caps.length; i++) {
                    setCap(i, found[i]);
                }
                return true;
            }
            boolean pair = unicode && start + 1 < end && Character.isHighSurrogate(in.charAt(start))
                    && Character.isLowSurrogate(in.charAt(start + 1));
            start += pair ? 2 : 1;
        }
        return negative;
    }

    /**
     * Executes a single-character repeat instruction.
     * <p>
     * 단일 문자 반복 명령을 실행한다.
     *
     * @return false if the repeat cannot match / 반복이 일치할 수 없으면 false
     */
    private boolean executeRepeat() {
        int kind = code[pc + 1];
        int arg = code[pc + 2];
        int min = code[pc + 3];
        int max = code[pc + 4];
        boolean greedy = code[pc + 5] == 1;
        int p = pos;
        int count = 0;
        // 먼저 최소 횟수만큼은 반드시 일치해야 한다.
        while (count < min) {
            int np = matchAtom(kind, arg, p);
            if (np < 0) {
                return false;
            }
            p = np;
            count++;
        }
        steps += count;
        int minPos = p;
        if (greedy) {
            long before = steps;
            // 가능한 만큼 최대한 많이 소비하고, 돌려줄 수 있도록 항목을 남긴다.
            while (count < max) {
                int np = matchAtom(kind, arg, p);
                if (np < 0) {
                    break;
                }
                p = np;
                count++;
                steps++;
            }
            if (steps - before > 0 && steps > stepLimit) {
                checkLimits();
            }
            if (p > minPos) {
                push(BT_GREEDY, pc + 6, minPos, p);
            }
        } else if (max > min) {
            push(BT_LAZY, pc, p, min);
        }
        pos = p;
        pc += 6;
        return true;
    }

    /**
     * Matches a back reference at the current position.
     * <p>
     * 현재 위치에서 역참조를 일치시킨다.
     *
     * @param g group number / 그룹 번호
     * @return position after the match, or -1 / 일치 뒤의 위치. 실패하면 -1
     */
    private int matchBackReference(int g) {
        int s = caps[2 * g];
        int e = caps[2 * g + 1];
        if (s < 0 || e < 0) {
            return pos;
        }
        int len = e - s;
        if (!ignoreCase) {
            if (pos + len > end || !in.regionMatches(s, in, pos, len)) {
                return -1;
            }
            return pos + len;
        }
        int a = s;
        int b = pos;
        // 캡처된 문자열과 현재 입력을 정규형으로 바꿔 문자 단위로 비교한다.
        while (a < e) {
            if (b >= end) {
                return -1;
            }
            int c1 = charAt(a);
            int c2 = charAt(b);
            if (CaseFold.canonicalize(c1, unicode) != CaseFold.canonicalize(c2, unicode)) {
                return -1;
            }
            a += c1 > 0xFFFF ? 2 : 1;
            b += c2 > 0xFFFF ? 2 : 1;
        }
        return b;
    }

    /**
     * Completes a positive lookahead: removes the alternatives created inside it but keeps the undo records, then
     * restores the position where the lookahead started.
     * <p>
     * 긍정 선행 탐색을 마무리한다: 안에서 만든 대안은 지우되 되돌림 기록은 남기고, 선행 탐색이 시작된 위치를 복원한다.
     */
    private void finishPositiveLookahead() {
        int m = sp - 4;
        // 가장 가까운 선행 탐색 표지를 찾는다.
        while (stack[m] != BT_LOOK) {
            m -= 4;
        }
        int startPos = stack[m + 2];
        int w = m;
        // 표지 위의 항목 중 캡처와 레지스터 되돌림 기록만 아래로 옮겨 남긴다.
        for (int r = m + 4; r < sp; r += 4) {
            int t = stack[r];
            if (t == BT_CAP || t == BT_REG) {
                stack[w] = t;
                stack[w + 1] = stack[r + 1];
                stack[w + 2] = stack[r + 2];
                stack[w + 3] = stack[r + 3];
                w += 4;
            }
        }
        sp = w;
        pos = startPos;
    }

    /**
     * Undoes everything done since the nearest lookahead marker and removes the marker (used when a negative lookahead's
     * body matched, which makes the lookahead fail).
     * <p>
     * 가장 가까운 선행 탐색 표지 이후에 한 모든 일을 되돌리고 표지를 지운다(부정 선행 탐색의 본문이 일치해 선행 탐색이 실패할 때 쓴다).
     */
    private void undoToLookMarker() {
        // 표지를 만날 때까지 캡처와 레지스터를 원래 값으로 되돌린다.
        while (true) {
            sp -= 4;
            int t = stack[sp];
            if (t == BT_CAP) {
                caps[stack[sp + 1]] = stack[sp + 2];
            } else if (t == BT_REG) {
                regs[stack[sp + 1]] = stack[sp + 2];
            } else if (t == BT_LOOK) {
                return;
            }
        }
    }
}
