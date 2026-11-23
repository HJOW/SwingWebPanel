package org.duckdns.hjow.swingwebpanel.js.host;

/**
 * The times at which a page reached the stages of its loading (Navigation Timing: <code>domInteractive</code>, <code>domContentLoadedEventStart</code> and
 * <code>domContentLoadedEventEnd</code>, <code>domComplete</code>, <code>loadEventStart</code> and <code>loadEventEnd</code>), as the scheduler clock
 * (milliseconds since the Unix epoch) said when the stage was reached. A stage that was not reached has the time 0. Only the stages that the page host itself passes
 * through are recorded: the network times of the navigation (fetch, connection, response) are not known to the page and are never invented.
 * <p>
 * 페이지가 로딩 단계에 이른 시각(Navigation Timing: <code>domInteractive</code>, <code>domContentLoadedEventStart</code>, <code>domContentLoadedEventEnd</code>,
 * <code>domComplete</code>, <code>loadEventStart</code>, <code>loadEventEnd</code>)을 단계에 이른 순간의 스케줄러 시계(유닉스 에포크 이후 밀리초)로 기록한다. 이르지
 * 못한 단계의 시각은 0 이다. 페이지 호스트가 직접 지나는 단계만 기록하며, 이동의 네트워크 시각(가져오기, 연결, 응답)은 페이지가 알지 못하므로 지어내지 않는다.
 */
final class PageTimeline {

    /**
     * The stage <code>domInteractive</code> (the document has been parsed, <code>readyState</code> became <code>interactive</code>).
     * <p>
     * <code>domInteractive</code> 단계(문서를 해석했고 <code>readyState</code> 가 <code>interactive</code> 가 되었다).
     */
    static final int DOM_INTERACTIVE = 0;

    /**
     * The stage <code>domContentLoadedEventStart</code>.
     * <p>
     * <code>domContentLoadedEventStart</code> 단계.
     */
    static final int DCL_START = 1;

    /**
     * The stage <code>domContentLoadedEventEnd</code>.
     * <p>
     * <code>domContentLoadedEventEnd</code> 단계.
     */
    static final int DCL_END = 2;

    /**
     * The stage <code>domComplete</code> (<code>readyState</code> became <code>complete</code>).
     * <p>
     * <code>domComplete</code> 단계(<code>readyState</code> 가 <code>complete</code> 가 되었다).
     */
    static final int DOM_COMPLETE = 3;

    /**
     * The stage <code>loadEventStart</code>.
     * <p>
     * <code>loadEventStart</code> 단계.
     */
    static final int LOAD_START = 4;

    /**
     * The stage <code>loadEventEnd</code>.
     * <p>
     * <code>loadEventEnd</code> 단계.
     */
    static final int LOAD_END = 5;

    /**
     * The times of the stages, 0 until a stage was reached.
     * <p>
     * 단계별 시각. 단계에 이르기 전에는 0.
     */
    private final long[] times = new long[6];

    /**
     * Records that a stage was reached now (the first time only: a stage is reached once).
     * <p>
     * 단계에 지금 이르렀음을 기록한다(처음 한 번만: 단계는 한 번 이른다).
     *
     * @param stage the stage, one of the constants of this class / 단계. 이 클래스의 상수 중 하나
     * @param time  the scheduler time in milliseconds / 스케줄러 시각(밀리초)
     */
    synchronized void reached(int stage, long time) {
        if (times[stage] == 0) {
            times[stage] = time;
        }
    }

    /**
     * Returns the time of a stage.
     * <p>
     * 단계의 시각을 반환한다.
     *
     * @param stage the stage / 단계
     * @return the scheduler time in milliseconds, or 0 if the stage was not reached / 스케줄러 시각(밀리초). 이르지 못했으면 0
     */
    synchronized long get(int stage) {
        return times[stage];
    }
}
