package org.duckdns.hjow.swingwebpanel.js.regexp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.js.text.CharClass;
import org.duckdns.hjow.swingwebpanel.js.text.UnicodeProperties;

/**
 * A set of characters (code units, or code points with the u flag) stored as sorted, merged, inclusive ranges,
 * optionally negated. A set is mutable while the pattern is parsed and must be {@link #normalize()}d before
 * {@link #contains(int)} is used; afterwards it is only read, so it can be shared by threads.
 * <p>
 * 정렬되고 병합된 닫힌 구간으로 저장하는 문자 집합(코드 단위, u 플래그면 코드 포인트). 부정(negation) 여부를 가질 수 있다.
 * 패턴을 해석하는 동안은 변경할 수 있고 {@link #contains(int)} 를 쓰기 전에 {@link #normalize()} 해야 한다.
 * 그 뒤에는 읽기만 하므로 여러 스레드가 공유할 수 있다.
 */
final class RegExpCharSet {

    /**
     * Cached Unicode property sets, shared only after normalization.
     * <p>
     * 정규화한 뒤 공유하는 유니코드 속성 집합 캐시.
     */
    private static final Map<String, RegExpCharSet> UNICODE_PROPERTIES = new HashMap<String, RegExpCharSet>();

    /**
     * Range storage: lo0, hi0, lo1, hi1 ...
     * <p>
     * 구간 저장소: lo0, hi0, lo1, hi1 ...
     */
    private int[] ranges = new int[8];

    /**
     * Number of ints used in {@link #ranges} (twice the range count).
     * <p>
     * {@link #ranges} 에서 쓰는 int 개수(구간 수의 두 배).
     */
    private int used;

    /**
     * True if the set matches characters that are NOT in the ranges (the class was written as [^...]).
     * <p>
     * 구간에 없는 문자와 일치하면 true([^...] 로 쓴 클래스).
     */
    boolean negated;

    /**
     * Creates an empty set.
     * <p>
     * 빈 집합을 만든다.
     */
    RegExpCharSet() {
    }

    /**
     * Adds a single character.
     * <p>
     * 문자 하나를 추가한다.
     *
     * @param c character / 문자
     */
    void addChar(int c) {
        addRange(c, c);
    }

    /**
     * Adds an inclusive range.
     * <p>
     * 닫힌 구간을 추가한다.
     *
     * @param lo first character / 첫 문자
     * @param hi last character / 마지막 문자
     */
    void addRange(int lo, int hi) {
        if (used + 2 > ranges.length) {
            ranges = Arrays.copyOf(ranges, ranges.length * 2);
        }
        ranges[used++] = lo;
        ranges[used++] = hi;
    }

    /**
     * Adds all ranges of another set (its negation flag is ignored).
     * <p>
     * 다른 집합의 모든 구간을 추가한다(그 집합의 부정 여부는 무시).
     *
     * @param other normalized or raw set / 집합
     */
    void addAll(RegExpCharSet other) {
        // 다른 집합의 구간을 하나씩 복사한다.
        for (int i = 0; i < other.used; i += 2) {
            addRange(other.ranges[i], other.ranges[i + 1]);
        }
    }

    /**
     * Sorts and merges the ranges. Must be called once building is finished.
     * <p>
     * 구간을 정렬하고 병합한다. 구성이 끝나면 한 번 호출해야 한다.
     */
    void normalize() {
        int count = used / 2;
        if (count <= 1) {
            return;
        }
        long[] pairs = new long[count];
        // 구간을 (시작, 끝) 하나의 long 으로 묶어 시작 순으로 정렬한다.
        for (int i = 0; i < count; i++) {
            pairs[i] = (((long) ranges[2 * i]) << 32) | (ranges[2 * i + 1] & 0xFFFFFFFFL);
        }
        Arrays.sort(pairs);
        int w = 0;
        int curLo = (int) (pairs[0] >> 32);
        int curHi = (int) pairs[0];
        // 겹치거나 맞닿은 구간을 하나로 합친다.
        for (int i = 1; i < count; i++) {
            int lo = (int) (pairs[i] >> 32);
            int hi = (int) pairs[i];
            if (lo <= curHi + 1) {
                if (hi > curHi) {
                    curHi = hi;
                }
            } else {
                ranges[w++] = curLo;
                ranges[w++] = curHi;
                curLo = lo;
                curHi = hi;
            }
        }
        ranges[w++] = curLo;
        ranges[w++] = curHi;
        used = w;
    }

    /**
     * Tests membership in the ranges (the negation flag is not applied). The set must be normalized.
     * <p>
     * 구간에 속하는지 검사한다(부정 여부는 적용하지 않음). 집합은 정규화되어 있어야 한다.
     *
     * @param c character / 문자
     * @return true if inside one of the ranges / 구간 중 하나에 속하면 true
     */
    boolean contains(int c) {
        int lo = 0;
        int hi = used / 2 - 1;
        // 구간 배열에서 이분 탐색한다.
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (c < ranges[2 * mid]) {
                hi = mid - 1;
            } else if (c > ranges[2 * mid + 1]) {
                lo = mid + 1;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * Intersects two normalized sets without modifying them.
     * <p>
     * 두 정규화 집합을 변경하지 않고 교집합을 계산한다.
     *
     * @param other normalized set / 정규화 집합
     * @return normalized intersection / 정규화한 교집합
     */
    RegExpCharSet intersect(RegExpCharSet other) {
        RegExpCharSet result = new RegExpCharSet();
        int left = 0;
        int right = 0;
        // 겹친 부분을 결과에 넣고 끝점이 먼저 오는 구간을 넘긴다.
        while (left < used && right < other.used) {
            int lo = Math.max(ranges[left], other.ranges[right]);
            int hi = Math.min(ranges[left + 1], other.ranges[right + 1]);
            if (lo <= hi) {
                result.addRange(lo, hi);
            }
            if (ranges[left + 1] <= other.ranges[right + 1]) {
                left += 2;
            } else {
                right += 2;
            }
        }
        return result;
    }

    /**
     * Subtracts another normalized set from this normalized set.
     * <p>
     * 이 정규화 집합에서 다른 정규화 집합을 뺀다.
     *
     * @param other normalized set / 정규화 집합
     * @return normalized difference / 정규화한 차집합
     */
    RegExpCharSet subtract(RegExpCharSet other) {
        RegExpCharSet result = new RegExpCharSet();
        int right = 0;
        // 왼쪽의 각 구간에서 오른쪽 구간과 겹치지 않는 조각을 뽑는다.
        for (int left = 0; left < used; left += 2) {
            int next = ranges[left];
            int end = ranges[left + 1];
            // 현재 왼쪽 구간 전에 끝난 오른쪽 구간은 다시 볼 필요가 없다.
            while (right < other.used && other.ranges[right + 1] < next) {
                right += 2;
            }
            // 오른쪽 구간 사이의 빈 부분을 남기고 겹친 부분을 건너뛴다.
            while (right < other.used && other.ranges[right] <= end) {
                if (next < other.ranges[right]) {
                    result.addRange(next, other.ranges[right] - 1);
                }
                next = other.ranges[right + 1] + 1;
                if (next > end) {
                    break;
                }
                right += 2;
            }
            if (next <= end) {
                result.addRange(next, end);
            }
        }
        return result;
    }

    /**
     * Complements the ranges of this normalized set within 0..max.
     * <p>
     * 이 정규화 집합의 구간을 0..max 안에서 여집합으로 만든다.
     *
     * @param max largest character to include / 포함할 최대 문자
     * @return complement set / 여집합
     */
    RegExpCharSet complement(int max) {
        RegExpCharSet out = new RegExpCharSet();
        int next = 0;
        // 정규화된 구간 사이의 빈 곳을 새 구간으로 만든다.
        for (int i = 0; i < used; i += 2) {
            if (ranges[i] > next) {
                out.addRange(next, Math.min(ranges[i] - 1, max));
            }
            next = ranges[i + 1] + 1;
            if (next > max) {
                break;
            }
        }
        if (next <= max) {
            out.addRange(next, max);
        }
        return out;
    }

    /**
     * Builds the set for \d.
     * <p>
     * \d 용 집합을 만든다.
     *
     * @return digits 0-9 / 숫자 0-9
     */
    static RegExpCharSet digits() {
        RegExpCharSet s = new RegExpCharSet();
        s.addRange('0', '9');
        s.normalize();
        return s;
    }

    /**
     * Builds the set for \w (ASCII word characters).
     * <p>
     * \w 용 집합을 만든다(ASCII 단어 문자).
     *
     * @return letters, digits and underscore / 글자, 숫자, 밑줄
     */
    static RegExpCharSet word() {
        RegExpCharSet s = new RegExpCharSet();
        s.addRange('0', '9');
        s.addRange('A', 'Z');
        s.addChar('_');
        s.addRange('a', 'z');
        s.normalize();
        return s;
    }

    /**
     * Builds the set for \s (WhiteSpace and LineTerminator of ECMAScript).
     * <p>
     * \s 용 집합을 만든다(ECMAScript 의 WhiteSpace 와 LineTerminator).
     *
     * @return space characters / 공백 문자
     */
    static RegExpCharSet space() {
        RegExpCharSet s = new RegExpCharSet();
        int start = -1;
        // 0..0xFFFF 의 문자를 훑으며 공백 문자의 연속 구간을 만든다.
        for (int c = 0; c <= 0x10000; c++) {
            boolean in = c < 0x10000 && CharClass.isWhiteSpaceOrLineTerminator(c);
            if (in && start < 0) {
                start = c;
            } else if (!in && start >= 0) {
                s.addRange(start, c - 1);
                start = -1;
            }
        }
        s.normalize();
        return s;
    }

    /**
     * Returns the Unicode property escape set for an exact ECMAScript name or alias.
     * <p>
     * ECMAScript가 정확히 일치하는 이름이나 별칭으로 받는 유니코드 속성 집합을 돌려준다.
     *
     * @param propertyName canonical or aliased property name / 표준 또는 별칭 속성 이름
     * @return set, or null for an unknown property / 집합. 모르면 null
     */
    static synchronized RegExpCharSet unicodeProperty(String propertyName) {
        RegExpCharSet cached = UNICODE_PROPERTIES.get(propertyName);
        if (cached != null) {
            return cached;
        }
        int[] ranges = UnicodeProperties.ranges(propertyName);
        if (ranges == null) {
            return null;
        }
        RegExpCharSet set = fromRanges(ranges);
        UNICODE_PROPERTIES.put(propertyName, set);
        return set;
    }

    /**
     * Builds a normalized set from generated inclusive ranges.
     * <p>
     * 생성된 닫힌 구간에서 정규화 집합을 만든다.
     *
     * @param ranges lo, hi pairs / lo, hi 쌍
     * @return set / 집합
     */
    private static RegExpCharSet fromRanges(int[] ranges) {
        RegExpCharSet set = new RegExpCharSet();
        // 생성 표의 닫힌 lo/hi 쌍을 집합 구간으로 차례로 복사한다.
        for (int i = 0; i < ranges.length; i += 2) {
            set.addRange(ranges[i], ranges[i + 1]);
        }
        set.normalize();
        return set;
    }
}
