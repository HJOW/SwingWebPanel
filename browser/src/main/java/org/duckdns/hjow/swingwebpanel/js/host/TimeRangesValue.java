package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.media.playback.MediaRanges;

/**
 * The value behind a script's <code>TimeRanges</code> object (<code>buffered</code>, <code>seekable</code> and <code>played</code> of a media
 * element, plan item P18-08): a static normalized list of ranges in seconds (HTML5 section 4.7.10.14). The class is public because the DOM
 * bridge calls its methods.
 * <p>
 * 스크립트의 <code>TimeRanges</code> 객체(미디어 요소의 <code>buffered</code>, <code>seekable</code>, <code>played</code>, 계획 P18-08) 뒤의 값: 초 단위
 * 범위의 정적인 정규화 목록(HTML5 4.7.10.14). DOM 브리지가 메서드를 부르므로 클래스는 public 이다.
 */
public final class TimeRangesValue {

    /**
     * The ranges.
     * <p>
     * 범위들.
     */
    private final MediaRanges ranges;

    /**
     * Creates the value.
     * <p>
     * 값을 만든다.
     *
     * @param ranges the normalized ranges / 정규화된 범위
     */
    TimeRangesValue(MediaRanges ranges) {
        this.ranges = ranges;
    }

    /**
     * Returns the number of ranges.
     * <p>
     * 범위의 수를 반환한다.
     *
     * @return the number / 수
     */
    public int getLength() {
        return ranges.size();
    }

    /**
     * Returns the start of a range.
     * <p>
     * 범위의 시작을 반환한다.
     *
     * @param index the index / 번호
     * @return the start in seconds / 시작(초)
     * @throws DomException IndexSizeError if the index is out of range / 번호가 범위를 벗어나면 IndexSizeError
     */
    public double start(int index) {
        check(index);
        return ranges.start(index);
    }

    /**
     * Returns the end of a range.
     * <p>
     * 범위의 끝을 반환한다.
     *
     * @param index the index / 번호
     * @return the end in seconds / 끝(초)
     * @throws DomException IndexSizeError if the index is out of range / 번호가 범위를 벗어나면 IndexSizeError
     */
    public double end(int index) {
        check(index);
        return ranges.end(index);
    }

    /**
     * Checks an index.
     * <p>
     * 번호를 확인한다.
     *
     * @param index the index / 번호
     */
    private void check(int index) {
        if (index < 0 || index >= ranges.size()) {
            throw DomException.indexSize("The index provided (" + index + ") is greater than or equal to the maximum bound (" + ranges.size() + ").");
        }
    }
}
