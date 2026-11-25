package org.duckdns.hjow.swingwebpanel.js.builtins.stringarray;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * Stable merge sort for <code>Array.prototype.sort</code>. The caller writes back the working copy only after sorting succeeds.
 * The default order converts each pair to strings during comparison and compares UTF-16 code units.
 * The caller handles <code>undefined</code> elements separately because they always sort last.
 * <p>
 * <code>Array.prototype.sort</code>의 안정적인 병합 정렬. 정렬이 성공하면 호출자가 작업 복사본을 원래 객체에 쓴다.
 * 기본 순서는 비교할 두 값을 문자열로 바꾸어 UTF-16 코드 단위로 비교한다.
 * <code>undefined</code> 요소는 항상 맨 뒤에 정렬되므로 호출자가 따로 처리한다.
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
        if (values.length >= 2) {
            mergeSort(values, new JsValue[values.length], 0, values.length, compareFn);
        }
    }

    /**
     * Stable top-down merge sort of values.
     * <p>
     * 값을 정렬하는 안정적인 하향식 병합 정렬.
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
     * Compares a pair by string conversion or the numeric result of the callback. NaN counts as zero.
     * <p>
     * 문자열 변환 또는 콜백의 숫자 결과로 두 값을 비교한다. NaN은 0으로 본다.
     *
     * @param compareFn the function / 함수
     * @param x         first value / 첫 값
     * @param y         second value / 둘째 값
     * @return negative, zero or positive / 음수, 0, 양수
     */
    private static double compare(JsValue compareFn, JsValue x, JsValue y) {
        if (compareFn.isUndefined()) {
            return Ops.toString(x).compareTo(Ops.toString(y));
        }
        double v = Ops.toNumber(Ops.call(compareFn, Js.UNDEFINED, new JsValue[] { x, y }));
        return v != v ? 0 : v;
    }
}
