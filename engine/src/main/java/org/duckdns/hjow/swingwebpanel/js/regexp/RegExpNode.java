package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract syntax tree node of a parsed regular expression. One class with a type tag keeps the parser and the code
 * generator compact. Nodes are created by {@link RegExpParser} and read by {@link RegExpCompiler}.
 * <p>
 * 해석한 정규식의 추상 구문 트리 노드. 형식 태그가 있는 하나의 클래스로 해석기와 코드 생성기를 간결하게 유지한다.
 * 노드는 {@link RegExpParser} 가 만들고 {@link RegExpCompiler} 가 읽는다.
 */
final class RegExpNode {

    /**
     * Node type: matches the empty string.
     * <p>
     * 노드 종류: 빈 문자열과 일치.
     */
    static final int EMPTY = 0;

    /**
     * Node type: one literal character, see {@link #value}.
     * <p>
     * 노드 종류: 리터럴 문자 하나, {@link #value} 참조.
     */
    static final int CHAR = 1;

    /**
     * Node type: dot.
     * <p>
     * 노드 종류: 점.
     */
    static final int ANY = 2;

    /**
     * Node type: character class, see {@link #set}.
     * <p>
     * 노드 종류: 문자 클래스, {@link #set} 참조.
     */
    static final int SET = 3;

    /**
     * Node type: concatenation of {@link #children}.
     * <p>
     * 노드 종류: {@link #children} 의 연결.
     */
    static final int SEQ = 4;

    /**
     * Node type: alternation of {@link #children}.
     * <p>
     * 노드 종류: {@link #children} 의 선택.
     */
    static final int ALT = 5;

    /**
     * Node type: group; {@link #value} is the capture index, 0 for a non-capturing group.
     * <p>
     * 노드 종류: 그룹. {@link #value} 는 캡처 번호이며 비캡처 그룹은 0.
     */
    static final int GROUP = 6;

    /**
     * Node type: back reference to group {@link #value}.
     * <p>
     * 노드 종류: 그룹 {@link #value} 에 대한 역참조.
     */
    static final int BACKREF = 7;

    /**
     * Node type: zero-width assertion, kind in {@link #value}.
     * <p>
     * 노드 종류: 너비 0 단언, 종류는 {@link #value}.
     */
    static final int ASSERT = 8;

    /**
     * Node type: lookahead, {@link #value} is 1 for a negative lookahead.
     * <p>
     * 노드 종류: 선행 탐색. 부정이면 {@link #value} 가 1.
     */
    static final int LOOK = 9;

    /**
     * Node type: lookbehind, {@link #value} is 1 for a negative lookbehind.
     * <p>
     * 노드 종류: 뒤쪽 탐색. 부정이면 {@link #value} 가 1.
     */
    static final int LOOK_BEHIND = 11;

    /**
     * Node type: quantified {@link #child}.
     * <p>
     * 노드 종류: 수량자가 붙은 {@link #child}.
     */
    static final int REPEAT = 10;

    /**
     * Assertion kind: start of input or line (^).
     * <p>
     * 단언 종류: 입력 또는 줄의 시작(^).
     */
    static final int BOL = 0;

    /**
     * Assertion kind: end of input or line ($).
     * <p>
     * 단언 종류: 입력 또는 줄의 끝($).
     */
    static final int EOL = 1;

    /**
     * Assertion kind: word boundary (\b).
     * <p>
     * 단언 종류: 단어 경계(\b).
     */
    static final int WORD_BOUNDARY = 2;

    /**
     * Assertion kind: not a word boundary (\B).
     * <p>
     * 단언 종류: 단어 경계 아님(\B).
     */
    static final int NOT_WORD_BOUNDARY = 3;

    /**
     * Quantifier maximum meaning "unbounded".
     * <p>
     * "무제한"을 뜻하는 수량자 최댓값.
     */
    static final int INFINITE = Integer.MAX_VALUE;

    /**
     * The node type tag.
     * <p>
     * 노드 종류 태그.
     */
    final int type;

    /**
     * Type specific number: character, capture index, back reference index, assertion kind or negative flag.
     * <p>
     * 종류에 따른 숫자: 문자, 캡처 번호, 역참조 번호, 단언 종류, 부정 여부.
     */
    int value;

    /**
     * The character class of a SET node.
     * <p>
     * SET 노드의 문자 클래스.
     */
    RegExpCharSet set;

    /**
     * Children of SEQ and ALT nodes.
     * <p>
     * SEQ, ALT 노드의 자식들.
     */
    List<RegExpNode> children;

    /**
     * The single child of GROUP, LOOK and REPEAT nodes.
     * <p>
     * GROUP, LOOK, REPEAT 노드의 단일 자식.
     */
    RegExpNode child;

    /**
     * Quantifier minimum of a REPEAT node.
     * <p>
     * REPEAT 노드의 수량자 최솟값.
     */
    int min;

    /**
     * Quantifier maximum of a REPEAT node ({@link #INFINITE} if unbounded).
     * <p>
     * REPEAT 노드의 수량자 최댓값(무제한이면 {@link #INFINITE}).
     */
    int max;

    /**
     * True for a greedy quantifier.
     * <p>
     * 탐욕적 수량자이면 true.
     */
    boolean greedy;

    /**
     * First capture group number inside a REPEAT child (greater than {@link #lastGroup} if none).
     * <p>
     * REPEAT 자식 안의 첫 캡처 그룹 번호(없으면 {@link #lastGroup} 보다 큼).
     */
    int firstGroup;

    /**
     * Last capture group number inside a REPEAT child.
     * <p>
     * REPEAT 자식 안의 마지막 캡처 그룹 번호.
     */
    int lastGroup;

    /**
     * Group name used by a named back reference until it is resolved.
     * <p>
     * 이름 있는 역참조가 해석되기 전까지 쓰는 그룹 이름.
     */
    String name;

    /**
     * Creates a node of the given type.
     * <p>
     * 지정한 종류의 노드를 만든다.
     *
     * @param type node type constant / 노드 종류 상수
     */
    RegExpNode(int type) {
        this.type = type;
    }

    /**
     * Creates a node with a value.
     * <p>
     * 값이 있는 노드를 만든다.
     *
     * @param type  node type / 노드 종류
     * @param value type specific number / 종류별 숫자
     * @return the node / 노드
     */
    static RegExpNode of(int type, int value) {
        RegExpNode n = new RegExpNode(type);
        n.value = value;
        return n;
    }

    /**
     * Creates a node with one child.
     * <p>
     * 자식이 하나인 노드를 만든다.
     *
     * @param type  node type / 노드 종류
     * @param value type specific number / 종류별 숫자
     * @param child child node / 자식 노드
     * @return the node / 노드
     */
    static RegExpNode of(int type, int value, RegExpNode child) {
        RegExpNode n = of(type, value);
        n.child = child;
        return n;
    }

    /**
     * Creates a SEQ or ALT node, or returns the only child when there is one.
     * <p>
     * SEQ 또는 ALT 노드를 만든다. 자식이 하나뿐이면 그 자식을 반환한다.
     *
     * @param type     SEQ or ALT / SEQ 또는 ALT
     * @param children child nodes / 자식 노드들
     * @return the combined node / 합쳐진 노드
     */
    static RegExpNode list(int type, List<RegExpNode> children) {
        if (children.size() == 1) {
            return children.get(0);
        }
        if (children.isEmpty()) {
            return new RegExpNode(EMPTY);
        }
        RegExpNode n = new RegExpNode(type);
        n.children = new ArrayList<RegExpNode>(children);
        return n;
    }

    /**
     * Minimum number of characters (code units or code points) this node always consumes.
     * Zero means the node can match the empty string. The result saturates instead of overflowing.
     * <p>
     * 이 노드가 항상 소비하는 문자(코드 단위 또는 코드 포인트)의 최소 개수. 0 이면 빈 문자열과 일치할 수 있다.
     * 넘치지 않고 포화한다.
     *
     * @return minimum consumed length / 최소 소비 길이
     */
    int minLength() {
        switch (type) {
        case CHAR:
        case ANY:
        case SET:
            return 1;
        case SEQ: {
            long sum = 0;
            // 자식들의 최소 길이를 더한다.
            for (int i = 0; i < children.size(); i++) {
                sum += children.get(i).minLength();
            }
            return (int) Math.min(sum, Integer.MAX_VALUE);
        }
        case ALT: {
            int best = Integer.MAX_VALUE;
            // 선택지 중 가장 짧은 것이 최소이다.
            for (int i = 0; i < children.size(); i++) {
                best = Math.min(best, children.get(i).minLength());
            }
            return best;
        }
        case GROUP:
            return child.minLength();
        case REPEAT:
            return (int) Math.min((long) min * child.minLength(), Integer.MAX_VALUE);
        default:
            return 0;
        }
    }
}
