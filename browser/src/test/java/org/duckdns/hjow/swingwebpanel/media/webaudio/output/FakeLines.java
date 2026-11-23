package org.duckdns.hjow.swingwebpanel.media.webaudio.output;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * A fake sound system for tests (plan item P25-07 tests): it supports the sample rates and channel counts that the test names and hands out
 * {@link FakeLine}s, which it remembers. The class is not a suite.
 * <p>
 * 시험용 가짜 소리 시스템(계획 P25-07 시험): 시험이 정한 샘플링 주파수와 채널 수를 지원하고 {@link FakeLine} 을 내주며 그것들을 기억한다. 이 클래스는 스위트가 아니다.
 */
public final class FakeLines implements JavaSoundOutput.Lines {

    /**
     * The supported sample rates (empty means all).
     * <p>
     * 지원하는 샘플링 주파수(비어 있으면 모두).
     */
    private final Set<Integer> rates = new HashSet<Integer>();

    /**
     * The supported channel counts (empty means all).
     * <p>
     * 지원하는 채널 수(비어 있으면 모두).
     */
    private final Set<Integer> channelCounts = new HashSet<Integer>();

    /**
     * The lines that were opened.
     * <p>
     * 열린 라인.
     */
    private final ArrayList<FakeLine> lines = new ArrayList<FakeLine>();

    /**
     * True if opening fails.
     * <p>
     * 열기가 실패하면 true.
     */
    private boolean busy;

    /**
     * The formats that were asked about.
     * <p>
     * 물어본 형식.
     */
    private final ArrayList<AudioFormat> asked = new ArrayList<AudioFormat>();

    /**
     * Creates a system that supports every format.
     * <p>
     * 모든 형식을 지원하는 시스템을 만든다.
     */
    public FakeLines() {
    }

    /**
     * Limits the supported sample rates.
     * <p>
     * 지원하는 샘플링 주파수를 제한한다.
     *
     * @param values the rates / 주파수
     * @return this / 이것
     */
    public FakeLines onlyRates(int... values) {
        rates.clear();
        // 지원하는 주파수를 넣는다.
        for (int v : values) {
            rates.add(Integer.valueOf(v));
        }
        return this;
    }

    /**
     * Limits the supported channel counts.
     * <p>
     * 지원하는 채널 수를 제한한다.
     *
     * @param values the channel counts / 채널 수
     * @return this / 이것
     */
    public FakeLines onlyChannels(int... values) {
        channelCounts.clear();
        // 지원하는 채널 수를 넣는다.
        for (int v : values) {
            channelCounts.add(Integer.valueOf(v));
        }
        return this;
    }

    /**
     * Makes every opening fail as a busy device does.
     * <p>
     * 사용 중인 장치처럼 모든 열기가 실패하게 한다.
     *
     * @param value true to fail / 실패하게 하려면 true
     */
    public void setBusy(boolean value) {
        busy = value;
    }

    /**
     * Tells whether the format is supported.
     * <p>
     * 형식을 지원하는지 알려 준다.
     *
     * @param format the format / 형식
     * @return true if supported / 지원하면 true
     */
    public boolean isSupported(AudioFormat format) {
        synchronized (asked) {
            asked.add(format);
        }
        return (rates.isEmpty() || rates.contains(Integer.valueOf((int) format.getSampleRate())))
                && (channelCounts.isEmpty() || channelCounts.contains(Integer.valueOf(format.getChannels())));
    }

    /**
     * Opens a fake line.
     * <p>
     * 가짜 라인을 연다.
     *
     * @param format      the format / 형식
     * @param bufferBytes the buffer in bytes / 버퍼(바이트)
     * @return the line / 라인
     * @throws LineUnavailableException if the system is busy / 시스템이 사용 중이면
     */
    public SourceDataLine open(AudioFormat format, int bufferBytes) throws LineUnavailableException {
        if (busy) {
            throw new LineUnavailableException("the device is busy");
        }
        FakeLine line = new FakeLine(format, bufferBytes);
        synchronized (lines) {
            lines.add(line);
        }
        return line;
    }

    /**
     * Returns the line that was opened last, or null.
     * <p>
     * 마지막으로 연 라인을 반환한다. 없으면 null.
     *
     * @return the line or null / 라인 또는 null
     */
    public FakeLine last() {
        synchronized (lines) {
            return lines.isEmpty() ? null : lines.get(lines.size() - 1);
        }
    }

    /**
     * Returns the number of lines that were opened.
     * <p>
     * 열린 라인의 수를 반환한다.
     *
     * @return the count / 수
     */
    public int count() {
        synchronized (lines) {
            return lines.size();
        }
    }

    /**
     * Returns the number of formats that were asked about.
     * <p>
     * 물어본 형식의 수를 반환한다.
     *
     * @return the count / 수
     */
    public int askedCount() {
        synchronized (asked) {
            return asked.size();
        }
    }
}
