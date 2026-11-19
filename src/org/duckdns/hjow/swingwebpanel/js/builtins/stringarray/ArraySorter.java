package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The sorting algorithm of <code>Array.prototype.sort</code>: a stable merge sort (equal elements keep their order, which ES2015 does not
 * require but all current engines provide) that works on a Java array copy and therefore leaves the original array untouched when the
 * comparison function throws. The default order compares the string forms of the elements by UTF-16 code units; each element is converted
 * to a string once (the specification converts at every comparison, which is only observable through side effects of user defined
 * <code>toString</code> methods). Elements that are <code>undefined</code> must be removed by the caller; they always sort last.
 * <p>
 * <code>Array.prototype.sort</code> 의 정렬 알고리즘: 안정적인 병합 정렬(같은 요소는 순서를 유지한다. ES2015 가 요구하지는 않지만 현재의 모든 엔진이
 * 제공한다). Java 배열 복사본에서 동작하므로 비교 함수가 예외를 던져도 원래 배열은 바뀌지 않는다. 기본 순서는 요소의 문자열 형태를 UTF-16 코드 단위로
 * 비교하며, 요소마다 문자열 변환을 한 번만 한다(규격은 비교할 때마다 변환하며 사용자 정의 <code>toString</code> 의 부작용으로만 관찰된다).
 * <code>undefined</code> 요소는 호출자가 미리 빼야 하며 항상 맨 뒤에 정렬된다.
 */
final class ArraySorter {

    /**
     * Private constructor: static helper class.
     * <p>
     * 비공개 생성자: 정적 도우미 클래스이다.
     */
    private ArraySorter() {
    }

    /**
     * Sorts the values in place (the array is the caller's private working copy).
     * <p>
     * 값을 제자리에서 정렬한다(배열은 호출자의 전용 작업 복사본이다).
     *
     * @param values    the values to sort, none undefined / 정렬할 값(undefined 없음)
     * @param compareFn comparison function or undefined for the default order / 비교 함수. 기본 순서이면 undefined
     */
    static void sort(JsValue[] values, JsValue compareFn) {
        int n = values.length;
        if (n < 2) {
            return;
        }
        if (compareFn.isUndefined()) {
            String[] keys = new String[n];
            Integer[] order = new Integer[n];
            // 요소마다 문자열을 한 번만 만들고 위치 번호로 간접 정렬한다.
            for (int i = 0; i < n; i++) {
                keys[i] = Ops.toString(values[i]);
                order[i] = Integer.valueOf(i);
            }
            mergeSortIndices(order, keys, new Integer[n], 0, n);
            JsValue[] copy = values.clone();
            // 정렬된 위치 번호 순서대로 값을 다시 배치한다.
            for (int i = 0; i < n; i++) {
                values[i] = copy[order[i].intValue()];
            }
            return;
        }
        mergeSort(values, new JsValue[n], 0, n, compareFn);
    }

    /**
     * Stable top-down merge sort of an index array by string keys.
     * <p>
     * 문자열 키 기준으로 위치 번호 배열을 정렬하는 안정적인 하향식 병합 정렬.
     *
     * @param order the index array / 위치 번호 배열
     * @param keys  string keys by index / 위치 번호별 문자열 키
     * @param tmp   scratch array of the same length / 같은 길이의 임시 배열
     * @param from  first position / 시작 위치
     * @param to    end position (exclusive) / 끝 위치(미포함)
     */
    private static void mergeSortIndices(Integer[] order, String[] keys, Integer[] tmp, int from, int to) {
        if (to - from < 2) {
            return;
        }
        int mid = (from + to) >>> 1;
        mergeSortIndices(order, keys, tmp, from, mid);
        mergeSortIndices(order, keys, tmp, mid, to);
        int i = from;
        int j = mid;
        int k = from;
        // 두 정렬된 구간을 합친다. 같으면 왼쪽을 먼저 둔다(안정성).
        while (i < mid && j < to) {
            if (keys[order[i].intValue()].compareTo(keys[order[j].intValue()]) <= 0) {
                tmp[k++] = order[i++];
            } else {
                tmp[k++] = order[j++];
            }
        }
        // 왼쪽에 남은 요소를 이어 붙인다.
        while (i < mid) {
            tmp[k++] = order[i++];
        }
        // 오른쪽에 남은 요소를 이어 붙인다.
        while (j < to) {
            tmp[k++] = order[j++];
        }
        System.arraycopy(tmp, from, order, from, to - from);
    }

    /**
     * Stable top-down merge sort of values with a user comparison function.
     * <p>
     * 사용자 비교 함수로 값을 정렬하는 안정적인 하향식 병합 정렬.
     *
     * @param a         the values / 값
     * @param tmp       scratch array of the same length / 같은 길이의 임시 배열
     * @param from      first position / 시작 위치
     * @param to        end position (exclusive) / 끝 위치(미포함)
     * @param compareFn the comparison function / 비교 함수
     */
    private static void mergeSort(JsValue[] a, JsValue[] tmp, int from, int to, JsValue compareFn) {
        if (to - from < 2) {
            return;
        }
        int mid = (from + to) >>> 1;
        mergeSort(a, tmp, from, mid, compareFn);
        mergeSort(a, tmp, mid, to, compareFn);
        int i = from;
        int j = mid;
        int k = from;
        // 두 정렬된 구간을 합친다. 비교 결과가 0 이하이면 왼쪽을 먼저 둔다(안정성).
        while (i < mid && j < to) {
            Support.poll(k);
            if (compare(compareFn, a[i], a[j]) <= 0) {
                tmp[k++] = a[i++];
            } else {
                tmp[k++] = a[j++];
            }
        }
        // 왼쪽에 남은 요소를 이어 붙인다.
        while (i < mid) {
            tmp[k++] = a[i++];
        }
        // 오른쪽에 남은 요소를 이어 붙인다.
        while (j < to) {
            tmp[k++] = a[j++];
        }
        System.arraycopy(tmp, from, a, from, to - from);
    }

    /**
     * Calls the comparison function: ToNumber of its result, NaN counts as 0.
     * <p>
     * 비교 함수를 호출한다. 결과에 ToNumber 를 적용하며 NaN 은 0 으로 본다.
     *
     * @param compareFn the function / 함수
     * @param x         first value / 첫 값
     * @param y         second value / 둘째 값
     * @return negative, zero or positive / 음수, 0, 양수
     */
    private static double compare(JsValue compareFn, JsValue x, JsValue y) {
        double v = Ops.toNumber(Ops.call(compareFn, Js.UNDEFINED, new JsValue[] { x, y }));
        return v != v ? 0 : v;
    }
}
