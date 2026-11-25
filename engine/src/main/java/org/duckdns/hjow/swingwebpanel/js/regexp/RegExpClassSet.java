package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A Unicode Sets class with singleton code points and finite sequences of code points.
 * <p>
 * 단일 코드 포인트와 유한한 코드 포인트 열을 함께 담는 Unicode Sets 클래스.
 */
final class RegExpClassSet {

    /** Singleton elements as character ranges. / 문자 구간으로 저장한 단일 요소. */
    RegExpCharSet characters;

    /** Elements with zero or at least two code points. / 코드 포인트가 없거나 둘 이상인 요소. */
    private final Set<List<Integer>> strings = new LinkedHashSet<List<Integer>>();

    /** Static MayContainStrings result, independent of the resulting set. / 연산 결과와 구분되는 정적 MayContainStrings 판정. */
    boolean mayContainStrings;

    /** Starts an empty class. / 빈 클래스를 만든다. */
    RegExpClassSet() {
        this(new RegExpCharSet());
    }

    /**
     * Starts a character-only class.
     * <p>
     * 문자만 담은 클래스를 만든다.
     *
     * @param characters character set / 문자 집합
     */
    RegExpClassSet(RegExpCharSet characters) {
        this.characters = characters;
    }

    /**
     * Adds a sequence, using the same representation as other class operands for singleton sequences.
     * The caller must leave the sequence unchanged after adding it.
     * <p>
     * 코드 포인트 열을 추가한다. 길이 하나인 열은 다른 피연산자의 단일 문자와 같은 형식을 사용한다.
     * 호출자는 추가한 열을 수정하지 않는다.
     *
     * @param sequence sequence of code points / 코드 포인트 열
     */
    void addSequence(List<Integer> sequence) {
        if (sequence.size() == 1) {
            characters.addChar(sequence.get(0).intValue());
        } else {
            strings.add(sequence);
            mayContainStrings = true;
        }
    }

    /**
     * Appends another class to this union under construction.
     * <p>
     * 만들고 있는 합집합에 다른 클래스를 추가한다.
     *
     * @param other source class / 원본 클래스
     */
    void addAll(RegExpClassSet other) {
        characters.addAll(other.characters);
        strings.addAll(other.strings);
        mayContainStrings |= other.mayContainStrings;
    }

    /**
     * Finishes building the character ranges and applies the v-mode folding closure when needed.
     * <p>
     * 문자 구간 구성을 끝내고 필요하면 v 모드의 접기 동치 문자를 포함한다.
     *
     * @param fold whether matching ignores case / 일치 시 대소문자를 무시하는지 여부
     */
    void finishCharacters(boolean fold) {
        characters.normalize();
        if (fold && !characters.isEmpty()) {
            characters = CaseFold.closeUnicodeSet(characters);
        }
    }

    /**
     * Intersects this class and another class.
     * <p>
     * 두 클래스의 교집합을 만든다.
     *
     * @param other normalized class / 정규화한 클래스
     * @return intersection / 교집합
     */
    RegExpClassSet intersect(RegExpClassSet other) {
        RegExpClassSet result = new RegExpClassSet(characters.intersect(other.characters));
        result.strings.addAll(strings);
        result.strings.retainAll(other.strings);
        result.mayContainStrings = mayContainStrings && other.mayContainStrings;
        return result;
    }

    /**
     * Subtracts another class from this class.
     * <p>
     * 이 클래스에서 다른 클래스를 뺀다.
     *
     * @param other normalized class / 정규화한 클래스
     * @return difference / 차집합
     */
    RegExpClassSet subtract(RegExpClassSet other) {
        RegExpClassSet result = new RegExpClassSet(characters.subtract(other.characters));
        result.strings.addAll(strings);
        result.strings.removeAll(other.strings);
        result.mayContainStrings = mayContainStrings;
        return result;
    }

    /**
     * Builds alternatives in descending code point length, keeping shorter matches available for backtracking.
     * A character class handles all length-one alternatives and an empty alternative comes last.
     * <p>
     * 코드 포인트 수가 큰 대안을 먼저 배치하고 역추적에서 짧은 대안도 시도할 수 있게 한다.
     * 길이 하나인 대안은 문자 클래스가 맡고 빈 대안은 가장 뒤에 둔다.
     *
     * @return a class or alternation node / 클래스 또는 대안 구문 노드
     */
    RegExpNode toNode() {
        if (strings.isEmpty()) {
            return characterNode();
        }
        List<List<Integer>> ordered = new ArrayList<List<Integer>>(strings);
        Collections.sort(ordered, new Comparator<List<Integer>>() {
            /** Orders longer sequences first. / 긴 코드 포인트 열을 먼저 정렬한다. */
            public int compare(List<Integer> left, List<Integer> right) {
                return right.size() - left.size();
            }
        });
        List<RegExpNode> alternatives = new ArrayList<RegExpNode>();
        // 길이가 둘 이상인 열을 일련의 문자 비교로 컴파일한다.
        for (List<Integer> sequence : ordered) {
            if (!sequence.isEmpty()) {
                List<RegExpNode> terms = new ArrayList<RegExpNode>(sequence.size());
                // 각 코드 포인트를 개별 문자 노드로 유지한다.
                for (Integer codePoint : sequence) {
                    terms.add(RegExpNode.of(RegExpNode.CHAR, codePoint.intValue()));
                }
                alternatives.add(RegExpNode.list(RegExpNode.SEQ, terms));
            }
        }
        if (!characters.isEmpty()) {
            alternatives.add(characterNode());
        }
        if (strings.contains(Collections.emptyList())) {
            alternatives.add(new RegExpNode(RegExpNode.EMPTY));
        }
        return RegExpNode.list(RegExpNode.ALT, alternatives);
    }

    /**
     * Creates the length-one alternative.
     * <p>
     * 길이가 하나인 대안의 노드를 만든다.
     *
     * @return class node / 클래스 노드
     */
    private RegExpNode characterNode() {
        RegExpNode node = new RegExpNode(RegExpNode.SET);
        node.set = characters;
        return node;
    }
}
