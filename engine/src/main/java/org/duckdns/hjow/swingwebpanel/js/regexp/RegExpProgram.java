package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * A compiled ECMAScript regular expression. It is immutable apart from the two resource limits, so one program
 * can be executed by several threads at the same time (each execution has its own matcher state).
 * <p>
 * Matching works on UTF-16 strings. Without the u flag characters are code units; with the u flag they are code points
 * (a surrogate pair is one character). Flags g and y are not interpreted here: the caller chooses the start index and
 * decides between {@link #exec(String, int)} (scan forward, as for the global flag or a plain exec) and
 * {@link #matchAt(String, int)} (match exactly at the index, as for the sticky flag).
 * Results are arrays of <code>2 * (groupCount + 1)</code> ints: start and end offset of the whole match followed by start
 * and end of every group, -1 for groups that did not participate.
 * <p>
 * Protection against catastrophic backtracking: every execution counts steps and fails with
 * {@link RegExpLimitException} when more than {@link #getStepLimit()} steps are used or when the backtracking stack holds
 * more than {@link #getMaxBacktrackEntries()} entries; an optional {@link RegExpCancel} is polled every 1024 steps.
 * <p>
 * 컴파일된 ECMAScript 정규식. 두 가지 자원 제한을 빼면 불변이므로 한 프로그램을 여러 스레드가 동시에 실행할 수 있다(실행마다 자기 일치 상태를 가진다).
 * <p>
 * 일치 검색은 UTF-16 문자열 위에서 한다. u 플래그가 없으면 문자는 코드 단위이고, 있으면 코드 포인트(서로게이트 쌍이 한 문자)이다. 플래그 g 와 y 는
 * 여기서 해석하지 않는다. 호출자가 시작 위치를 정하고 {@link #exec(String, int)} (앞쪽으로 훑음: 전역 플래그나 일반 exec)와
 * {@link #matchAt(String, int)} (정확히 그 위치에서 일치: 고정 플래그) 중 하나를 고른다.
 * 결과는 <code>2 * (groupCount + 1)</code> 개의 int 배열이다: 전체 일치의 시작/끝 위치, 이어서 각 그룹의 시작/끝 위치이며 참여하지 않은 그룹은 -1 이다.
 * <p>
 * 치명적인 역추적 방어: 실행마다 단계를 세어 {@link #getStepLimit()} 보다 많이 쓰거나 역추적 스택이 {@link #getMaxBacktrackEntries()} 개보다 많아지면
 * {@link RegExpLimitException} 으로 실패한다. 선택적인 {@link RegExpCancel} 은 1024 단계마다 확인한다.
 */
public final class RegExpProgram {

    /**
     * Default step budget of one execution.
     * <p>
     * 실행 한 번의 기본 단계 예산.
     */
    public static final long DEFAULT_STEP_LIMIT = 100000000L;

    /**
     * Default maximum number of backtracking stack entries (each entry uses four ints).
     * <p>
     * 기본 역추적 스택 항목 최대 개수(항목 하나는 int 네 개).
     */
    public static final int DEFAULT_MAX_BACKTRACK_ENTRIES = 1500000;

    /**
     * Instruction: successful end of the pattern.
     * <p>
     * 명령: 패턴의 성공적인 끝.
     */
    static final int OP_MATCH = 0;

    /**
     * Instruction: literal character, operand arg.
     * <p>
     * 명령: 리터럴 문자, 피연산자 arg.
     */
    static final int OP_CHAR = 1;

    /**
     * Instruction: dot without the s flag.
     * <p>
     * 명령: s 플래그 없는 점.
     */
    static final int OP_ANY = 2;

    /**
     * Instruction: dot with the s flag.
     * <p>
     * 명령: s 플래그 있는 점.
     */
    static final int OP_ANY_ALL = 3;

    /**
     * Instruction: character class, operand set index.
     * <p>
     * 명령: 문자 클래스, 피연산자 클래스 번호.
     */
    static final int OP_SET = 4;

    /**
     * Instruction: try the first target and remember the second as a backtrack point.
     * <p>
     * 명령: 첫 번째 목적지를 시도하고 두 번째를 역추적 지점으로 기억한다.
     */
    static final int OP_SPLIT = 5;

    /**
     * Instruction: unconditional jump.
     * <p>
     * 명령: 무조건 점프.
     */
    static final int OP_JMP = 6;

    /**
     * Instruction: remember the start of a capture group.
     * <p>
     * 명령: 캡처 그룹의 시작을 기억한다.
     */
    static final int OP_GROUP_START = 7;

    /**
     * Instruction: store the capture of a finished group.
     * <p>
     * 명령: 끝난 그룹의 캡처를 저장한다.
     */
    static final int OP_GROUP_END = 8;

    /**
     * Instruction: clear the captures of a range of groups.
     * <p>
     * 명령: 그룹 범위의 캡처를 지운다.
     */
    static final int OP_RESET_CAPS = 9;

    /**
     * Instruction: start of input.
     * <p>
     * 명령: 입력 시작.
     */
    static final int OP_BOL = 10;

    /**
     * Instruction: start of input or line (multiline).
     * <p>
     * 명령: 입력 또는 줄의 시작(여러 줄).
     */
    static final int OP_BOL_M = 11;

    /**
     * Instruction: end of input.
     * <p>
     * 명령: 입력 끝.
     */
    static final int OP_EOL = 12;

    /**
     * Instruction: end of input or line (multiline).
     * <p>
     * 명령: 입력 또는 줄의 끝(여러 줄).
     */
    static final int OP_EOL_M = 13;

    /**
     * Instruction: word boundary.
     * <p>
     * 명령: 단어 경계.
     */
    static final int OP_WORD_B = 14;

    /**
     * Instruction: not a word boundary.
     * <p>
     * 명령: 단어 경계 아님.
     */
    static final int OP_NOT_WORD_B = 15;

    /**
     * Instruction: back reference, operand group number.
     * <p>
     * 명령: 역참조, 피연산자 그룹 번호.
     */
    static final int OP_BACKREF = 16;

    /**
     * Instruction: start of a lookahead, operands: negative flag, continuation address.
     * <p>
     * 명령: 선행 탐색 시작, 피연산자: 부정 여부, 이어질 주소.
     */
    static final int OP_LOOK_START = 17;

    /**
     * Instruction: end of a lookahead body, operand negative flag.
     * <p>
     * 명령: 선행 탐색 본문의 끝, 피연산자 부정 여부.
     */
    static final int OP_LOOK_END = 18;

    /**
     * Instruction: set a register, operands: register, value.
     * <p>
     * 명령: 레지스터 설정, 피연산자: 레지스터, 값.
     */
    static final int OP_SET_REG = 19;

    /**
     * Instruction: loop head with a count, operands: register, min, max, greedy, exit address.
     * <p>
     * 명령: 횟수가 있는 반복 머리, 피연산자: 레지스터, 최솟값, 최댓값, 탐욕 여부, 탈출 주소.
     */
    static final int OP_LOOP = 20;

    /**
     * Instruction: start of a loop iteration, operand position register.
     * <p>
     * 명령: 반복 한 번의 시작, 피연산자 위치 레지스터.
     */
    static final int OP_ITER_START = 21;

    /**
     * Instruction: end of a loop iteration with the empty check, operands: register, position register, min, head.
     * <p>
     * 명령: 빈 반복 검사가 있는 반복 한 번의 끝, 피연산자: 레지스터, 위치 레지스터, 최솟값, 머리 주소.
     */
    static final int OP_LOOP_END = 22;

    /**
     * Instruction: repeat a single character atom, operands: kind, arg, min, max, greedy.
     * <p>
     * 명령: 단일 문자 원자의 반복, 피연산자: 종류, arg, 최솟값, 최댓값, 탐욕 여부.
     */
    static final int OP_REPEAT = 23;

    /**
     * Instruction: lookbehind, operands: negative flag and subprogram index.
     * <p>
     * 명령: 뒤쪽 탐색. 피연산자: 부정 여부와 하위 프로그램 번호.
     */
    static final int OP_LOOK_BEHIND = 24;

    /**
     * Atom kind: literal character.
     * <p>
     * 원자 종류: 리터럴 문자.
     */
    static final int KIND_CHAR = 0;

    /**
     * Atom kind: dot without line terminators.
     * <p>
     * 원자 종류: 줄 끝 문자를 뺀 점.
     */
    static final int KIND_ANY = 1;

    /**
     * Atom kind: dot with line terminators.
     * <p>
     * 원자 종류: 줄 끝 문자를 포함하는 점.
     */
    static final int KIND_ANY_ALL = 2;

    /**
     * Atom kind: character class.
     * <p>
     * 원자 종류: 문자 클래스.
     */
    static final int KIND_SET = 3;

    /**
     * The pattern text.
     * <p>
     * 패턴 문자열.
     */
    private final String source;

    /**
     * Flag bits.
     * <p>
     * 플래그 비트.
     */
    private final int flags;

    /**
     * Number of capturing groups.
     * <p>
     * 캡처 그룹 수.
     */
    private final int groupCount;

    /**
     * Instruction words.
     * <p>
     * 명령어.
     */
    final int[] code;

    /**
     * Character classes used by the instructions.
     * <p>
     * 명령이 쓰는 문자 클래스들.
     */
    final RegExpCharSet[] sets;

    /**
     * Subprograms of lookbehind assertions.
     * <p>
     * 뒤쪽 탐색 단언의 하위 프로그램.
     */
    final RegExpProgram[] lookbehinds;

    /**
     * Number of registers needed by the matcher.
     * <p>
     * 일치 검색기에 필요한 레지스터 수.
     */
    final int registerCount;

    /**
     * Group names by number (null for unnamed groups; index 0 unused).
     * <p>
     * 번호별 그룹 이름(이름 없으면 null, 0 번은 쓰지 않음).
     */
    private final String[] groupNames;

    /**
     * Literal character every match starts with, or -1.
     * <p>
     * 모든 일치가 시작하는 리터럴 문자. 없으면 -1.
     */
    private final int firstChar;

    /**
     * True if only index 0 can start a match.
     * <p>
     * 인덱스 0 에서만 일치가 시작될 수 있으면 true.
     */
    private final boolean anchored;

    /**
     * Step budget of one execution.
     * <p>
     * 실행 한 번의 단계 예산.
     */
    private volatile long stepLimit = DEFAULT_STEP_LIMIT;

    /**
     * Maximum number of backtracking stack entries.
     * <p>
     * 역추적 스택 항목의 최대 개수.
     */
    private volatile int maxBacktrackEntries = DEFAULT_MAX_BACKTRACK_ENTRIES;

    /**
     * Creates a program; use {@link RegExpCompiler} instead.
     * <p>
     * 프로그램을 만든다. 대신 {@link RegExpCompiler} 를 쓴다.
     *
     * @param source        pattern text / 패턴 문자열
     * @param flags         flag bits / 플래그 비트
     * @param groupCount    group count / 그룹 수
     * @param code          instruction words / 명령어
     * @param sets          character classes / 문자 클래스들
     * @param registerCount register count / 레지스터 수
     * @param groupNames    group names / 그룹 이름
     * @param firstChar     literal first character or -1 / 리터럴 첫 문자 또는 -1
     * @param anchored      whether anchored at the start / 시작에 고정되었는지
     */
    RegExpProgram(String source, int flags, int groupCount, int[] code, RegExpCharSet[] sets, int registerCount, String[] groupNames,
            int firstChar, boolean anchored, RegExpProgram[] lookbehinds) {
        this.source = source;
        this.flags = flags;
        this.groupCount = groupCount;
        this.code = code;
        this.sets = sets;
        this.registerCount = registerCount;
        this.groupNames = groupNames;
        this.firstChar = firstChar;
        this.anchored = anchored;
        this.lookbehinds = lookbehinds;
    }

    /**
     * Returns the pattern text.
     * <p>
     * 패턴 문자열을 반환한다.
     *
     * @return pattern / 패턴
     */
    public String getSource() {
        return source;
    }

    /**
     * Returns the flag bits (see {@link RegExpFlags}).
     * <p>
     * 플래그 비트를 반환한다({@link RegExpFlags} 참조).
     *
     * @return flags / 플래그
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns the number of capturing groups (not counting group 0).
     * <p>
     * 캡처 그룹 수를 반환한다(0 번 그룹 제외).
     *
     * @return group count / 그룹 수
     */
    public int groupCount() {
        return groupCount;
    }

    /**
     * Returns the name of a group (ES2018 named groups), or null.
     * <p>
     * 그룹 이름(ES2018 이름 있는 그룹)을 반환한다. 없으면 null.
     *
     * @param group group number 1..groupCount / 그룹 번호 1..groupCount
     * @return name or null / 이름 또는 null
     */
    public String getGroupName(int group) {
        return group > 0 && group < groupNames.length ? groupNames[group] : null;
    }

    /**
     * Returns the step budget of one execution.
     * <p>
     * 실행 한 번의 단계 예산을 반환한다.
     *
     * @return step limit / 단계 한도
     */
    public long getStepLimit() {
        return stepLimit;
    }

    /**
     * Sets the step budget of one execution (executions started afterwards use it).
     * <p>
     * 실행 한 번의 단계 예산을 정한다(이후 시작하는 실행에 적용).
     *
     * @param limit positive step limit / 양의 단계 한도
     */
    public void setStepLimit(long limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        this.stepLimit = limit;
    }

    /**
     * Returns the maximum number of backtracking stack entries.
     * <p>
     * 역추적 스택 항목의 최대 개수를 반환한다.
     *
     * @return maximum entries / 최대 항목 수
     */
    public int getMaxBacktrackEntries() {
        return maxBacktrackEntries;
    }

    /**
     * Sets the maximum number of backtracking stack entries (executions started afterwards use it).
     * <p>
     * 역추적 스택 항목의 최대 개수를 정한다(이후 시작하는 실행에 적용).
     *
     * @param entries positive entry count / 양의 항목 수
     */
    public void setMaxBacktrackEntries(int entries) {
        if (entries <= 0) {
            throw new IllegalArgumentException("entries must be positive");
        }
        this.maxBacktrackEntries = entries;
    }

    /**
     * Searches forward from a start index for the first match (RegExpBuiltinExec without the sticky flag).
     * <p>
     * 시작 위치부터 앞쪽으로 첫 일치를 찾는다(고정 플래그 없는 RegExpBuiltinExec).
     *
     * @param input      string to search / 검색할 문자열
     * @param startIndex index where the search starts (lastIndex) / 검색을 시작할 위치(lastIndex)
     * @return capture offsets, or null if there is no match / 캡처 위치 배열. 일치가 없으면 null
     * @throws RegExpLimitException if a resource limit is exceeded / 자원 제한을 넘으면
     */
    public int[] exec(String input, int startIndex) {
        return exec(input, startIndex, null);
    }

    /**
     * Same as {@link #exec(String, int)} with a cancellation hook.
     * <p>
     * 취소 훅이 있는 {@link #exec(String, int)}.
     *
     * @param input      string to search / 검색할 문자열
     * @param startIndex search start / 검색 시작 위치
     * @param cancel     cancellation hook or null / 취소 훅 또는 null
     * @return capture offsets or null / 캡처 위치 배열 또는 null
     * @throws RegExpLimitException if a resource limit is exceeded or the hook cancels / 자원 제한을 넘거나 훅이 취소하면
     */
    public int[] exec(String input, int startIndex, RegExpCancel cancel) {
        int len = input.length();
        int i = startIndex < 0 ? 0 : startIndex;
        if (i > len) {
            return null;
        }
        RegExpMatcher m = new RegExpMatcher(this, input, cancel);
        boolean unicode = RegExpFlags.isUnicodeMode(flags);
        if (anchored) {
            return i == 0 && m.matchAt(0) ? m.result() : null;
        }
        // 시작 위치를 하나씩 옮기며 일치를 시도한다. 첫 문자가 정해져 있으면 그 문자가 있는 곳으로 건너뛴다.
        while (i <= len) {
            if (firstChar >= 0) {
                i = input.indexOf(firstChar, i);
                if (i < 0) {
                    return null;
                }
            }
            if (m.matchAt(i)) {
                return m.result();
            }
            boolean pair = unicode && i + 1 < len && Character.isHighSurrogate(input.charAt(i)) && Character.isLowSurrogate(input.charAt(i + 1));
            i += pair ? 2 : 1;
        }
        return null;
    }

    /**
     * Tries to match exactly at an index (the sticky flag behavior).
     * <p>
     * 정확히 그 위치에서 일치하는지 시도한다(고정 플래그 동작).
     *
     * @param input string / 문자열
     * @param index index where the match must start / 일치가 시작해야 하는 위치
     * @return capture offsets or null / 캡처 위치 배열 또는 null
     * @throws RegExpLimitException if a resource limit is exceeded / 자원 제한을 넘으면
     */
    public int[] matchAt(String input, int index) {
        return matchAt(input, index, null);
    }

    /**
     * Same as {@link #matchAt(String, int)} with a cancellation hook.
     * <p>
     * 취소 훅이 있는 {@link #matchAt(String, int)}.
     *
     * @param input  string / 문자열
     * @param index  index where the match must start / 일치가 시작해야 하는 위치
     * @param cancel cancellation hook or null / 취소 훅 또는 null
     * @return capture offsets or null / 캡처 위치 배열 또는 null
     * @throws RegExpLimitException if a resource limit is exceeded or the hook cancels / 자원 제한을 넘거나 훅이 취소하면
     */
    public int[] matchAt(String input, int index, RegExpCancel cancel) {
        if (index < 0 || index > input.length()) {
            return null;
        }
        RegExpMatcher m = new RegExpMatcher(this, input, cancel);
        return m.matchAt(index) ? m.result() : null;
    }

    /**
     * Matches at a start position and requires the match to end at the requested index.
     * <p>
     * 시작 위치에서 일치하되 지정 위치에서 끝나야 한다.
     *
     * @param input input string / 입력 문자열
     * @param start start index / 시작 위치
     * @param expectedEnd expected end / 필요한 끝 위치
     * @param cancel cancellation hook / 취소 훅
     * @return captures or null / 캡처 또는 null
     */
    int[] matchAtEnding(String input, int start, int expectedEnd, RegExpCancel cancel) {
        if (start < 0 || start > input.length() || expectedEnd < start || expectedEnd > input.length()) {
            return null;
        }
        RegExpMatcher m = new RegExpMatcher(this, input, cancel);
        return m.matchAtEnding(start, expectedEnd) ? m.result() : null;
    }
}
