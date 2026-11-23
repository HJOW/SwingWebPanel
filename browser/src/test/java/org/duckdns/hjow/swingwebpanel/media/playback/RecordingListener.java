package org.duckdns.hjow.swingwebpanel.media.playback;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.media.MediaException;

/**
 * A playback listener for tests that records the reports as short words and lets a test wait for a report (plan item P18-08).
 * <p>
 * 보고를 짧은 단어로 기록하고 시험이 보고를 기다릴 수 있게 하는 시험용 재생 리스너(계획 P18-08).
 */
public final class RecordingListener implements PlaybackListener {

    /**
     * The reports in order: <code>loaded</code>, <code>loadFailed</code>, <code>seeked</code>, <code>looped</code>, <code>ended</code>,
     * <code>duration</code>, <code>time</code>, <code>failed</code>.
     * <p>
     * 순서대로의 보고: <code>loaded</code>, <code>loadFailed</code>, <code>seeked</code>, <code>looped</code>, <code>ended</code>,
     * <code>duration</code>, <code>time</code>, <code>failed</code>.
     */
    private final List<String> events = new ArrayList<String>();

    /**
     * The last loaded info.
     * <p>
     * 마지막으로 로드된 정보.
     */
    private MediaInfo info;

    /**
     * The last failure.
     * <p>
     * 마지막 실패.
     */
    private MediaException error;

    /**
     * The last duration that became known.
     * <p>
     * 마지막으로 알게 된 길이.
     */
    private double knownDuration = Double.NaN;

    /**
     * Creates the listener.
     * <p>
     * 리스너를 만든다.
     */
    public RecordingListener() {
    }

    /**
     * Adds a report and wakes the waiting test.
     * <p>
     * 보고를 더하고 기다리는 시험을 깨운다.
     *
     * @param event the word / 단어
     */
    private synchronized void add(String event) {
        events.add(event);
        notifyAll();
    }

    /**
     * Records the loaded media.
     * <p>
     * 로드된 미디어를 기록한다.
     *
     * @param i the facts / 정보
     */
    public void loaded(MediaInfo i) {
        synchronized (this) {
            info = i;
        }
        add("loaded");
    }

    /**
     * Records a failed load.
     * <p>
     * 로드 실패를 기록한다.
     *
     * @param e the reason / 이유
     */
    public void loadFailed(MediaException e) {
        synchronized (this) {
            error = e;
        }
        add("loadFailed");
    }

    /**
     * Records the start of the sound.
     * <p>
     * 소리의 시작을 기록한다.
     */
    public void started() {
        add("started");
    }

    /**
     * Records a finished seek.
     * <p>
     * 끝난 탐색을 기록한다.
     */
    public void seeked() {
        add("seeked");
    }

    /**
     * Records a loop wrap-around.
     * <p>
     * 반복의 되돌아감을 기록한다.
     */
    public void looped() {
        add("looped");
    }

    /**
     * Records the end.
     * <p>
     * 끝을 기록한다.
     */
    public void ended() {
        add("ended");
    }

    /**
     * Records a duration.
     * <p>
     * 길이를 기록한다.
     *
     * @param seconds the duration / 길이
     */
    public void durationKnown(double seconds) {
        synchronized (this) {
            knownDuration = seconds;
        }
        add("duration");
    }

    /**
     * Records a time update.
     * <p>
     * 시간 갱신을 기록한다.
     */
    public void timeUpdate() {
        add("time");
    }

    /**
     * Records a playback failure.
     * <p>
     * 재생 실패를 기록한다.
     *
     * @param e the reason / 이유
     */
    public void failed(MediaException e) {
        synchronized (this) {
            error = e;
        }
        add("failed");
    }

    /**
     * Counts the reports of a word.
     * <p>
     * 단어의 보고 횟수를 센다.
     *
     * @param event the word / 단어
     * @return the number / 횟수
     */
    public synchronized int count(String event) {
        int n = 0;
        // 기록된 보고에서 같은 단어를 센다.
        for (String e : events) {
            if (e.equals(event)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Returns the reports without the periodic time updates, joined by commas.
     * <p>
     * 정기 시간 갱신을 뺀 보고를 쉼표로 이어 반환한다.
     *
     * @return the text / 문자열
     */
    public synchronized String summary() {
        StringBuilder sb = new StringBuilder();
        // 시간 갱신은 횟수가 일정하지 않으므로 빼고 이어 붙인다.
        for (String e : events) {
            if (!"time".equals(e)) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(e);
            }
        }
        return sb.toString();
    }

    /**
     * Returns the last loaded info.
     * <p>
     * 마지막으로 로드된 정보를 반환한다.
     *
     * @return the info or null / 정보 또는 null
     */
    public synchronized MediaInfo info() {
        return info;
    }

    /**
     * Returns the last failure.
     * <p>
     * 마지막 실패를 반환한다.
     *
     * @return the failure or null / 실패 또는 null
     */
    public synchronized MediaException error() {
        return error;
    }

    /**
     * Returns the last duration that became known.
     * <p>
     * 마지막으로 알게 된 길이를 반환한다.
     *
     * @return the duration or NaN / 길이 또는 NaN
     */
    public synchronized double knownDuration() {
        return knownDuration;
    }

    /**
     * Waits until a word was reported at least a number of times.
     * <p>
     * 단어가 적어도 주어진 횟수만큼 보고될 때까지 기다린다.
     *
     * @param event the word / 단어
     * @param times the number of reports / 보고 횟수
     * @throws InterruptedException if interrupted / 인터럽트되면
     */
    public synchronized void await(String event, int times) throws InterruptedException {
        long end = System.currentTimeMillis() + 20000;
        // 원하는 횟수가 될 때까지 기다린다.
        while (countLocked(event) < times) {
            long left = end - System.currentTimeMillis();
            if (left <= 0) {
                throw new AssertionError("timeout waiting for " + times + " x " + event + ", reports: " + events);
            }
            wait(left);
        }
    }

    /**
     * Counts a word while the monitor is held.
     * <p>
     * 모니터를 쥔 채 단어를 센다.
     *
     * @param event the word / 단어
     * @return the number / 횟수
     */
    private int countLocked(String event) {
        int n = 0;
        // 기록된 보고에서 같은 단어를 센다.
        for (String e : events) {
            if (e.equals(event)) {
                n++;
            }
        }
        return n;
    }
}
