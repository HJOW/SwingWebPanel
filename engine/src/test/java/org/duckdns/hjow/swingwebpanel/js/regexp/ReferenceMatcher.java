package org.duckdns.hjow.swingwebpanel.js.regexp;

/**
 * A deliberately naive reference implementation of the ECMA-262 21.2.2 matching semantics used only by tests. It
 * interprets the syntax tree directly with continuation passing and copies the capture array whenever the specification
 * creates a new state, exactly like the algorithm text (including RepeatMatcher with its empty check and the capture
 * reset per iteration). It is slow and recursive, so inputs must be short. Because it shares {@link RegExpParser}
 * with the engine, it checks the code generator and the virtual machine (and their optimizations), not the parser.
 * <p>
 * ECMA-262 21.2.2 의 일치 의미를 일부러 단순하게 구현한 참조 구현으로 시험에서만 쓴다. 구문 트리를 연속(continuation) 전달 방식으로 직접 해석하고,
 * 규격이 새 상태를 만들 때마다 캡처 배열을 복사한다(빈 검사와 반복마다 캡처 초기화가 있는 RepeatMatcher 포함). 느리고 재귀적이므로 입력은 짧아야 한다.
 * {@link RegExpParser} 를 엔진과 공유하므로 해석기가 아니라 코드 생성기와 가상 기계(및 그 최적화)를 검증한다.
 */
final class ReferenceMatcher {

    /**
     * A continuation: receives the end position and captures and returns the final result array, or null for failure.
     * <p>
     * 연속: 끝 위치와 캡처를 받아 최종 결과 배열을 반환한다. 실패하면 null.
     */
    private interface Cont {
        /**
         * Continues matching.
         * <p>
         * 일치를 이어 간다.
         *
         * @param pos  current position / 현재 위치
         * @param caps captures / 캡처
         * @return result array or null / 결과 배열 또는 null
         */
        int[] run(int pos, int[] caps);
    }

    /**
     * The input string.
     * <p>
     * 입력 문자열.
     */
    private final String in;

    /**
     * Flag bits.
     * <p>
     * 플래그 비트.
     */
    private final int flags;

    /**
     * Start position of the current attempt.
     * <p>
     * 현재 시도의 시작 위치.
     */
    private int attemptStart;

    /**
     * Creates a reference matcher.
     * <p>
     * 참조 일치 검색기를 만든다.
     *
     * @param in    input string / 입력 문자열
     * @param flags flag bits (i, m, s are honored; u is not supported) / 플래그 비트(i, m, s 지원, u 는 미지원)
     */
    ReferenceMatcher(String in, int flags) {
        this.in = in;
        this.flags = flags;
    }

    /**
     * Searches forward from a start index.
     * <p>
     * 시작 위치부터 앞쪽으로 검색한다.
     *
     * @param root       syntax tree / 구문 트리
     * @param groupCount number of capturing groups / 캡처 그룹 수
     * @param start      start index / 시작 위치
     * @return capture offsets or null / 캡처 위치 또는 null
     */
    int[] exec(RegExpNode root, int groupCount, int start) {
        // 시작 위치를 하나씩 옮기며 첫 일치를 찾는다.
        for (int i = start; i <= in.length(); i++) {
            attemptStart = i;
            int[] caps = new int[2 * (groupCount + 1)];
            java.util.Arrays.fill(caps, -1);
            int[] r = match(root, i, caps, new Cont() {
                /**
                 * Final continuation: records the match range.
                 * <p>
                 * 마지막 연속: 일치 범위를 기록한다.
                 *
                 * @param pos  end position / 끝 위치
                 * @param caps captures / 캡처
                 * @return result / 결과
                 */
                public int[] run(int pos, int[] caps) {
                    int[] r = caps.clone();
                    r[0] = attemptStart;
                    r[1] = pos;
                    return r;
                }
            });
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    /**
     * Canonicalize of the specification.
     * <p>
     * 규격의 Canonicalize.
     *
     * @param c character / 문자
     * @return canonical form / 정규형
     */
    private int canon(int c) {
        return (flags & RegExpFlags.IGNORE_CASE) != 0 ? CaseFold.canonicalize(c, false) : c;
    }

    /**
     * Tests a class with case-insensitivity and negation, by brute force over equivalents.
     * <p>
     * 클래스를 대소문자 무시와 부정까지 포함하여 동치 문자를 직접 확인하는 방식으로 검사한다.
     *
     * @param set class / 클래스
     * @param c   character / 문자
     * @return true if matched / 일치하면 true
     */
    private boolean inSet(RegExpCharSet set, int c) {
        boolean found = set.contains(c);
        if (!found && (flags & RegExpFlags.IGNORE_CASE) != 0) {
            int[] eq = CaseFold.equivalents(c, false);
            // 같은 정규형의 모든 문자를 확인한다.
            for (int i = 0; i < eq.length; i++) {
                if (set.contains(eq[i])) {
                    found = true;
                }
            }
        }
        return found != set.negated;
    }

    /**
     * Tests the word characters used by word boundaries.
     * <p>
     * 단어 경계가 쓰는 단어 문자인지 검사한다.
     *
     * @param p position / 위치
     * @return true if the position holds a word character / 그 위치가 단어 문자이면 true
     */
    private boolean wordAt(int p) {
        if (p < 0 || p >= in.length()) {
            return false;
        }
        char c = in.charAt(p);
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
    }

    /**
     * Tests a line terminator.
     * <p>
     * 줄 끝 문자인지 검사한다.
     *
     * @param c character / 문자
     * @return true for a line terminator / 줄 끝 문자이면 true
     */
    private static boolean lineTerminator(int c) {
        return c == 0x0A || c == 0x0D || c == 0x2028 || c == 0x2029;
    }

    /**
     * Matches a node at a position and then runs the continuation.
     * <p>
     * 위치에서 노드를 일치시킨 뒤 연속을 실행한다.
     *
     * @param n    node / 노드
     * @param pos  position / 위치
     * @param caps captures / 캡처
     * @param k    continuation / 연속
     * @return result or null / 결과 또는 null
     */
    private int[] match(final RegExpNode n, final int pos, final int[] caps, final Cont k) {
        switch (n.type) {
        case RegExpNode.EMPTY:
            return k.run(pos, caps);
        case RegExpNode.CHAR:
            if (pos < in.length() && canon(in.charAt(pos)) == canon(n.value)) {
                return k.run(pos + 1, caps);
            }
            return null;
        case RegExpNode.ANY:
            if (pos < in.length() && ((flags & RegExpFlags.DOT_ALL) != 0 || !lineTerminator(in.charAt(pos)))) {
                return k.run(pos + 1, caps);
            }
            return null;
        case RegExpNode.SET:
            if (pos < in.length() && inSet(n.set, in.charAt(pos))) {
                return k.run(pos + 1, caps);
            }
            return null;
        case RegExpNode.SEQ:
            return sequence(n, 0, pos, caps, k);
        case RegExpNode.ALT:
            // 선택지를 왼쪽부터 차례로 시도한다.
            for (int i = 0; i < n.children.size(); i++) {
                int[] r = match(n.children.get(i), pos, caps, k);
                if (r != null) {
                    return r;
                }
            }
            return null;
        case RegExpNode.GROUP:
            if (n.value == 0) {
                return match(n.child, pos, caps, k);
            }
            return match(n.child, pos, caps, new Cont() {
                /**
                 * Stores the capture of the finished group.
                 * <p>
                 * 끝난 그룹의 캡처를 저장한다.
                 *
                 * @param end  end position / 끝 위치
                 * @param c    captures / 캡처
                 * @return result / 결과
                 */
                public int[] run(int end, int[] c) {
                    int[] cap = c.clone();
                    cap[2 * n.value] = pos;
                    cap[2 * n.value + 1] = end;
                    return k.run(end, cap);
                }
            });
        case RegExpNode.BACKREF: {
            int s = caps[2 * n.value];
            int e = caps[2 * n.value + 1];
            if (s < 0 || e < 0) {
                return k.run(pos, caps);
            }
            int len = e - s;
            if (pos + len > in.length()) {
                return null;
            }
            // 캡처된 문자열과 현재 입력을 정규형으로 비교한다.
            for (int i = 0; i < len; i++) {
                if (canon(in.charAt(s + i)) != canon(in.charAt(pos + i))) {
                    return null;
                }
            }
            return k.run(pos + len, caps);
        }
        case RegExpNode.ASSERT:
            return assertion(n.value, pos) ? k.run(pos, caps) : null;
        case RegExpNode.LOOK: {
            int[] inner = match(n.child, pos, caps, new Cont() {
                /**
                 * Ends the lookahead body, returning its captures.
                 * <p>
                 * 선행 탐색 본문을 끝내며 그 캡처를 반환한다.
                 *
                 * @param end end position / 끝 위치
                 * @param c   captures / 캡처
                 * @return the captures / 캡처
                 */
                public int[] run(int end, int[] c) {
                    return c.clone();
                }
            });
            if (n.value == 0) {
                return inner == null ? null : k.run(pos, inner);
            }
            return inner != null ? null : k.run(pos, caps);
        }
        case RegExpNode.REPEAT:
            return repeat(n, n.min, n.max, pos, caps, k);
        default:
            throw new IllegalStateException("node " + n.type);
        }
    }

    /**
     * Matches the children of a SEQ node starting with the given index.
     * <p>
     * SEQ 노드의 자식들을 주어진 번호부터 일치시킨다.
     *
     * @param n    SEQ node / SEQ 노드
     * @param idx  child index / 자식 번호
     * @param pos  position / 위치
     * @param caps captures / 캡처
     * @param k    continuation / 연속
     * @return result or null / 결과 또는 null
     */
    private int[] sequence(final RegExpNode n, final int idx, int pos, int[] caps, final Cont k) {
        if (idx == n.children.size()) {
            return k.run(pos, caps);
        }
        return match(n.children.get(idx), pos, caps, new Cont() {
            /**
             * Continues with the next child.
             * <p>
             * 다음 자식으로 이어 간다.
             *
             * @param p position / 위치
             * @param c captures / 캡처
             * @return result / 결과
             */
            public int[] run(int p, int[] c) {
                return sequence(n, idx + 1, p, c, k);
            }
        });
    }

    /**
     * Evaluates a zero-width assertion.
     * <p>
     * 너비 0 단언을 평가한다.
     *
     * @param kind assertion kind / 단언 종류
     * @param pos  position / 위치
     * @return true if it holds / 성립하면 true
     */
    private boolean assertion(int kind, int pos) {
        boolean multiline = (flags & RegExpFlags.MULTILINE) != 0;
        switch (kind) {
        case RegExpNode.BOL:
            return pos == 0 || (multiline && lineTerminator(in.charAt(pos - 1)));
        case RegExpNode.EOL:
            return pos == in.length() || (multiline && lineTerminator(in.charAt(pos)));
        case RegExpNode.WORD_BOUNDARY:
            return wordAt(pos - 1) != wordAt(pos);
        default:
            return wordAt(pos - 1) == wordAt(pos);
        }
    }

    /**
     * RepeatMatcher of ECMA-262 21.2.2.5.1.
     * <p>
     * ECMA-262 21.2.2.5.1 의 RepeatMatcher.
     *
     * @param n    REPEAT node / REPEAT 노드
     * @param min  remaining minimum / 남은 최솟값
     * @param max  remaining maximum / 남은 최댓값
     * @param pos  position / 위치
     * @param caps captures / 캡처
     * @param k    continuation / 연속
     * @return result or null / 결과 또는 null
     */
    private int[] repeat(final RegExpNode n, final int min, final int max, final int pos, final int[] caps, final Cont k) {
        if (max == 0) {
            return k.run(pos, caps);
        }
        Cont d = new Cont() {
            /**
             * Continuation of one iteration: rejects an empty iteration once the minimum is satisfied.
             * <p>
             * 반복 한 번의 연속: 최솟값을 채운 뒤의 빈 반복은 거부한다.
             *
             * @param p position / 위치
             * @param c captures / 캡처
             * @return result / 결과
             */
            public int[] run(int p, int[] c) {
                if (min == 0 && p == pos) {
                    return null;
                }
                int min2 = min == 0 ? 0 : min - 1;
                int max2 = max == RegExpNode.INFINITE ? RegExpNode.INFINITE : max - 1;
                return repeat(n, min2, max2, p, c, k);
            }
        };
        int[] cap = caps.clone();
        // 반복이 시작될 때 원자 안의 그룹 캡처를 모두 지운다.
        for (int g = n.firstGroup; g <= n.lastGroup; g++) {
            cap[2 * g] = -1;
            cap[2 * g + 1] = -1;
        }
        if (min != 0) {
            return match(n.child, pos, cap, d);
        }
        if (!n.greedy) {
            int[] z = k.run(pos, caps);
            if (z != null) {
                return z;
            }
            return match(n.child, pos, cap, d);
        }
        int[] z = match(n.child, pos, cap, d);
        if (z != null) {
            return z;
        }
        return k.run(pos, caps);
    }
}
