package org.duckdns.hjow.swingwebpanel.media.webaudio.decode;

import java.util.Arrays;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;

/**
 * The growing array of one channel that the decoding pipeline collects its output samples in. When the length is known in advance the array
 * has exactly that size and never grows (no copy at the end); otherwise it doubles up to the limit. Appending past the limit is an
 * <code>EncodingError</code>, so a stream that turns out to be much longer than announced cannot use up the memory. Not thread-safe: one
 * decoding thread owns it.
 * <p>
 * 디코딩 파이프라인이 출력 표본을 모으는 채널 하나의 늘어나는 배열. 길이를 미리 알면 정확히 그 크기이고 늘어나지 않는다(마지막에 복사가 없다). 아니면
 * 한도까지 두 배씩 늘어난다. 한도를 넘겨 덧붙이면 <code>EncodingError</code> 이므로 알린 것보다 훨씬 긴 것으로 드러난 스트림이 메모리를 다 쓸 수 없다.
 * 스레드 안전하지 않다. 디코딩 스레드 하나가 소유한다.
 */
final class ChannelSink {

    /**
     * The collected samples (the first {@link #length} are valid).
     * <p>
     * 모은 표본(앞의 {@link #length} 개가 유효하다).
     */
    private float[] data;

    /**
     * The number of valid samples.
     * <p>
     * 유효한 표본 수.
     */
    private int length;

    /**
     * The largest number of samples that may be appended.
     * <p>
     * 덧붙일 수 있는 표본 수의 최댓값.
     */
    private final int limit;

    /**
     * Creates a sink.
     * <p>
     * 싱크를 만든다.
     *
     * @param initial the initial capacity: the expected length or a guess / 처음 용량: 예상 길이나 추정값
     * @param limit   the largest length / 최대 길이
     */
    ChannelSink(int initial, int limit) {
        this.limit = limit;
        this.data = new float[Math.max(1, Math.min(initial, limit))];
    }

    /**
     * Appends samples.
     * <p>
     * 표본을 덧붙인다.
     *
     * @param src the samples / 표본
     * @param n   the number of samples to append / 덧붙일 표본 수
     * @throws AudioException <code>EncodingError</code> if the limit would be exceeded / 한도를 넘게 되면 <code>EncodingError</code>
     */
    void append(float[] src, int n) {
        if (n <= 0) {
            return;
        }
        if ((long) length + n > limit) {
            throw new AudioException(AudioException.Kind.ENCODING, "the decoded audio is longer than the limit of " + limit + " frames per channel");
        }
        // 용량이 모자라면 두 배(한도까지)로 늘린다.
        if (length + n > data.length) {
            int bigger = (int) Math.min((long) limit, Math.max((long) length + n, (long) data.length * 2));
            data = Arrays.copyOf(data, bigger);
        }
        System.arraycopy(src, 0, data, length, n);
        length += n;
    }

    /**
     * Returns the number of samples collected so far.
     * <p>
     * 지금까지 모은 표본 수를 반환한다.
     *
     * @return the length / 길이
     */
    int length() {
        return length;
    }

    /**
     * Returns the samples as an array of exactly their number (the internal array itself if it is full, so the sink must not be used after
     * this call).
     * <p>
     * 표본을 정확히 그 수만큼의 배열로 반환한다(가득 찼으면 내부 배열 그대로이므로 이 호출 뒤에는 싱크를 쓰면 안 된다).
     *
     * @return the samples / 표본
     */
    float[] finish() {
        return length == data.length ? data : Arrays.copyOf(data, length);
    }
}
