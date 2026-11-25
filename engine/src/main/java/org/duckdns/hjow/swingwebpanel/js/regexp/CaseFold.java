package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.text.UnicodeCase;

/**
 * ECMAScript RegExp Canonicalize and inverse case groups using generated Unicode mappings.
 * <p>
 * 생성한 유니코드 매핑을 적용하는 ECMAScript 정규식 Canonicalize와 역방향 대소문자 묶음.
 */
final class CaseFold {

    /** Shared empty case group. / 빈 대소문자 묶음. */
    private static final int[] EMPTY = new int[0];

    /** Static canonicalization helpers. / 정적 정규화 도우미. */
    private CaseFold() {
    }

    /**
     * Lazy legacy table, covering UTF-16 code units as required without full Unicode matching.
     * <p>
     * 유니코드 일치 모드를 쓰지 않을 때 UTF-16 코드 단위에 적용하는 지연 표.
     */
    private static final class Legacy {
        /** Canonical form by UTF-16 code unit. / UTF-16 코드 단위별 정규형. */
        static final char[] CANON = new char[0x10000];

        /** Characters sharing a canonical code unit. / 같은 정규형 코드 단위를 공유하는 문자. */
        static final Map<Integer, int[]> GROUPS = build();

        /**
         * Initializes canonical code units and their inverse groups.
         * <p>
         * 정규형 코드 단위와 역방향 묶음을 초기화한다.
         *
         * @return inverse groups / 역방향 묶음
         */
        private static Map<Integer, int[]> build() {
            Map<Integer, List<Integer>> groups = new HashMap<Integer, List<Integer>>();
            // 대문자 매핑 후 값이 바뀐 코드 단위만 역방향 묶음에 등록한다.
            for (int c = 0; c < CANON.length; c++) {
                int folded = UnicodeCase.legacyUpper(c);
                CANON[c] = (char) folded;
                if (c != folded) {
                    add(groups, c, folded);
                }
            }
            return toArrays(groups);
        }
    }

    /**
     * Lazy supplementary-capable Unicode fold table and inverse.
     * <p>
     * 보충 평면을 포함하는 지연 유니코드 접기 표와 역방향 표.
     */
    private static final class FullUnicode {
        /** Source and folded code points in source order. / 원본 순서의 원본과 접은 코드 포인트 쌍. */
        static final int[] MAP = UnicodeCase.simpleFoldMappings();

        /** Code points with a shared simple case fold. / 같은 단순 접기 결과를 갖는 코드 포인트. */
        static final Map<Integer, int[]> GROUPS = build();

        /**
         * Builds inverse groups for nontrivial simple folds.
         * <p>
         * 원본을 변경하는 단순 접기의 역방향 묶음을 만든다.
         *
         * @return inverse groups / 역방향 묶음
         */
        private static Map<Integer, int[]> build() {
            Map<Integer, List<Integer>> groups = new HashMap<Integer, List<Integer>>();
            // 각 단순 접기 원본을 접은 코드 포인트의 묶음에 넣는다.
            for (int i = 0; i < MAP.length; i += 2) {
                add(groups, MAP[i], MAP[i + 1]);
            }
            return toArrays(groups);
        }
    }

    /**
     * Returns the canonical form of a code unit or code point in the current RegExp mode.
     * <p>
     * 현재 정규식 모드에서 코드 단위나 코드 포인트의 정규형을 반환한다.
     *
     * @param ch code unit or code point / 코드 단위 또는 코드 포인트
     * @param unicode whether Unicode matching is active / 유니코드 일치 모드 여부
     * @return canonical form / 정규형
     */
    static int canonicalize(int ch, boolean unicode) {
        if (ch < 0x80) {
            // ASCII 접기는 유니코드 모드에서 소문자, 레거시 모드에서 대문자이다.
            return unicode ? (ch >= 'A' && ch <= 'Z' ? ch + ('a' - 'A') : ch) : UnicodeCase.legacyUpper(ch);
        }
        if (!unicode) {
            return ch < Legacy.CANON.length ? Legacy.CANON[ch] : ch;
        }
        int[] map = FullUnicode.MAP;
        int lo = 0;
        int hi = map.length / 2;
        // 원본 코드 포인트에 대해 단순 접기 결과가 있는지 이진 탐색한다.
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            int source = map[2 * mid];
            if (ch < source) {
                hi = mid;
            } else if (ch > source) {
                lo = mid + 1;
            } else {
                return map[2 * mid + 1];
            }
        }
        return ch;
    }

    /**
     * Finds the shared case group, including the given character if alternatives exist.
     * The returned array is immutable for callers.
     * <p>
     * 대안이 있으면 입력 문자를 포함하는 대소문자 묶음을 찾는다. 호출자는 반환 배열을 수정하지 않는다.
     *
     * @param ch code unit or code point / 코드 단위 또는 코드 포인트
     * @param unicode whether Unicode matching is active / 유니코드 일치 모드 여부
     * @return the case group or an empty array / 대소문자 묶음 또는 빈 배열
     */
    static int[] equivalents(int ch, boolean unicode) {
        Map<Integer, int[]> groups = unicode ? FullUnicode.GROUPS : Legacy.GROUPS;
        int[] group = groups.get(Integer.valueOf(canonicalize(ch, unicode)));
        return group == null ? EMPTY : group;
    }

    /**
     * Expands a normalized set to contain all Unicode case equivalents of its members.
     * This represents folded code points as full equivalence classes for set operations.
     * <p>
     * 정규화 집합에 포함된 코드 포인트의 유니코드 대소문자 동치 문자를 모두 넣는다.
     * 집합 연산에서 접힌 코드 포인트 하나를 같은 접기 결과를 갖는 전체 문자의 묶음으로 표현한다.
     *
     * @param set normalized original set / 정규화한 원본 집합
     * @return normalized expanded set / 정규화한 확장 집합
     */
    static RegExpCharSet closeUnicodeSet(RegExpCharSet set) {
        RegExpCharSet result = new RegExpCharSet();
        result.addAll(set);
        // 유니코드 전체를 훑지 않고 접기 결과를 공유하는 묶음만 살핀다.
        for (int[] group : FullUnicode.GROUPS.values()) {
            boolean present = false;
            // 원본 집합에 속하는 문자가 하나라도 있는지 확인한다.
            for (int member : group) {
                if (set.contains(member)) {
                    present = true;
                    break;
                }
            }
            if (present) {
                // 대응하는 동치 문자를 모두 추가한다.
                for (int member : group) {
                    result.addChar(member);
                }
            }
        }
        result.normalize();
        return result;
    }

    /**
     * Expands only those equivalence classes whose canonical member is already present.
     * This represents a raw set matched against canonical input without folding the set.
     * <p>
     * 집합에 정규형 문자가 들어 있는 대소문자 묶음만 확장한다.
     * 원본 집합을 접지 않고 정규형 입력으로 검사하는 의미를 나타낸다.
     *
     * @param set normalized raw set / 정규화한 원본 집합
     * @return equivalent set suitable for matching raw input / 원본 입력을 검사할 수 있는 동치 집합
     */
    static RegExpCharSet expandCanonicalMembers(RegExpCharSet set) {
        RegExpCharSet included = new RegExpCharSet();
        RegExpCharSet excluded = new RegExpCharSet();
        // 묶음의 정규형이 들어 있는지에 따라 묶음 전체를 추가하거나 빼도록 기록한다.
        for (Map.Entry<Integer, int[]> entry : FullUnicode.GROUPS.entrySet()) {
            RegExpCharSet destination = set.contains(entry.getKey().intValue()) ? included : excluded;
            // 같은 정규형을 가진 모든 문자를 하나의 묶음으로 처리한다.
            for (int cp : entry.getValue()) {
                destination.addChar(cp);
            }
        }
        excluded.normalize();
        RegExpCharSet result = set.subtract(excluded);
        result.addAll(included);
        result.normalize();
        return result;
    }

    /**
     * Adds a nonidentity mapping to an inverse group.
     * <p>
     * 원본과 결과가 다른 매핑을 역방향 묶음에 추가한다.
     *
     * @param groups inverse groups / 역방향 묶음
     * @param source original code point / 원본 코드 포인트
     * @param folded canonical code point / 정규형 코드 포인트
     */
    private static void add(Map<Integer, List<Integer>> groups, int source, int folded) {
        Integer key = Integer.valueOf(folded);
        List<Integer> group = groups.get(key);
        if (group == null) {
            group = new ArrayList<Integer>();
            group.add(key);
            groups.put(key, group);
        }
        group.add(Integer.valueOf(source));
    }

    /**
     * Converts working groups to directly iterable arrays.
     * <p>
     * 작업용 묶음을 바로 순회할 수 있는 배열로 바꾼다.
     *
     * @param groups working lists / 작업용 목록
     * @return array groups / 배열 묶음
     */
    private static Map<Integer, int[]> toArrays(Map<Integer, List<Integer>> groups) {
        Map<Integer, int[]> result = new HashMap<Integer, int[]>();
        // 묶음마다 코드 포인트 배열 하나를 채운다.
        for (Map.Entry<Integer, List<Integer>> entry : groups.entrySet()) {
            List<Integer> group = entry.getValue();
            int[] members = new int[group.size()];
            // 목록 순서대로 코드 포인트의 값을 꺼낸다.
            for (int i = 0; i < members.length; i++) {
                members[i] = group.get(i).intValue();
            }
            result.put(entry.getKey(), members);
        }
        return result;
    }
}
