package org.duckdns.hjow.swingwebpanel.media.playback;

import java.util.Arrays;

/**
 * A normalized list of time ranges in seconds (the data of a <code>TimeRanges</code> object): the ranges are ordered, not empty and neither
 * overlapping nor touching (plan item P18-08). Immutable; {@link #add(double, double)} returns a new list.
 * <p>
 * 초 단위 시간 범위의 정규화된 목록(<code>TimeRanges</code> 객체의 데이터): 범위는 순서대로이고 비어 있지 않으며 겹치거나 맞닿지 않는다(계획 P18-08).
 * 불변이며 {@link #add(double, double)} 는 새 목록을 반환한다.
 */
public final class MediaRanges {

    /**
     * The list without any range.
     * <p>
     * 범위가 없는 목록.
     */
    public static final MediaRanges EMPTY = new MediaRanges(new double[0], new double[0]);

    /**
     * The start of each range.
     * <p>
     * 각 범위의 시작.
     */
    private final double[] starts;

    /**
     * The end of each range.
     * <p>
     * 각 범위의 끝.
     */
    private final double[] ends;

    /**
     * Creates a list from normalized arrays (the arrays are not copied).
     * <p>
     * 정규화된 배열로 목록을 만든다(배열을 복사하지 않는다).
     *
     * @param starts the starts / 시작들
     * @param ends   the ends / 끝들
     */
    private MediaRanges(double[] starts, double[] ends) {
        this.starts = starts;
        this.ends = ends;
    }

    /**
     * Creates a list with one range, or the empty list if the range is empty.
     * <p>
     * 범위 하나의 목록을 만든다. 범위가 비었으면 빈 목록.
     *
     * @param start the start in seconds / 시작(초)
     * @param end   the end in seconds / 끝(초)
     * @return the list / 목록
     */
    public static MediaRanges of(double start, double end) {
        return EMPTY.add(start, end);
    }

    /**
     * Returns the number of ranges.
     * <p>
     * 범위의 수를 반환한다.
     *
     * @return the number / 수
     */
    public int size() {
        return starts.length;
    }

    /**
     * Returns the start of a range.
     * <p>
     * 범위의 시작을 반환한다.
     *
     * @param index the index, 0 or more and below {@link #size()} / 번호(0 이상 {@link #size()} 미만)
     * @return the start in seconds / 시작(초)
     * @throws IndexOutOfBoundsException if the index is out of range / 번호가 범위를 벗어나면
     */
    public double start(int index) {
        return starts[index];
    }

    /**
     * Returns the end of a range.
     * <p>
     * 범위의 끝을 반환한다.
     *
     * @param index the index, 0 or more and below {@link #size()} / 번호(0 이상 {@link #size()} 미만)
     * @return the end in seconds / 끝(초)
     * @throws IndexOutOfBoundsException if the index is out of range / 번호가 범위를 벗어나면
     */
    public double end(int index) {
        return ends[index];
    }

    /**
     * Tells whether a time is inside one of the ranges (the ends are inclusive).
     * <p>
     * 시간이 범위 중 하나 안에 있는지 알려 준다(끝점 포함).
     *
     * @param time the time in seconds / 시간(초)
     * @return true if inside / 안에 있으면 true
     */
    public boolean contains(double time) {
        // 범위를 하나씩 확인한다.
        for (int i = 0; i < starts.length; i++) {
            if (time >= starts[i] && time <= ends[i]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a list that also covers a range, merging every range that overlaps or touches it. A range with a start that is not below its end
     * (or with a NaN end) changes nothing.
     * <p>
     * 범위 하나를 더 덮는 목록을 반환하며, 그 범위와 겹치거나 맞닿는 범위는 모두 합친다. 시작이 끝보다 작지 않은(또는 끝이 NaN 인) 범위는 아무것도
     * 바꾸지 않는다.
     *
     * @param start the start in seconds / 시작(초)
     * @param end   the end in seconds / 끝(초)
     * @return the new list, or this list if nothing changes / 새 목록. 바뀌는 것이 없으면 이 목록
     */
    public MediaRanges add(double start, double end) {
        if (!(start < end)) {
            return this;
        }
        double s = start;
        double e = end;
        double[] ns = new double[starts.length + 1];
        double[] ne = new double[starts.length + 1];
        int n = 0;
        boolean placed = false;
        // 기존 범위를 순서대로 훑으며 새 범위와 겹치거나 맞닿는 것은 합치고 나머지는 그대로 둔다.
        for (int i = 0; i < starts.length; i++) {
            if (ends[i] < s) {
                ns[n] = starts[i];
                ne[n] = ends[i];
                n++;
            } else if (starts[i] > e) {
                // 새 범위를 이 범위보다 앞에 한 번만 넣는다.
                if (!placed) {
                    ns[n] = s;
                    ne[n] = e;
                    n++;
                    placed = true;
                }
                ns[n] = starts[i];
                ne[n] = ends[i];
                n++;
            } else {
                s = Math.min(s, starts[i]);
                e = Math.max(e, ends[i]);
            }
        }
        if (!placed) {
            ns[n] = s;
            ne[n] = e;
            n++;
        }
        return new MediaRanges(Arrays.copyOf(ns, n), Arrays.copyOf(ne, n));
    }

    /**
     * Returns a text such as <code>[0.0,1.5][3.0,4.0]</code> (for diagnostics and tests).
     * <p>
     * <code>[0.0,1.5][3.0,4.0]</code> 같은 문자열을 반환한다(진단과 시험용).
     *
     * @return the text / 문자열
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // 범위를 순서대로 문자열에 붙인다.
        for (int i = 0; i < starts.length; i++) {
            sb.append('[').append(starts[i]).append(',').append(ends[i]).append(']');
        }
        return sb.toString();
    }
}
