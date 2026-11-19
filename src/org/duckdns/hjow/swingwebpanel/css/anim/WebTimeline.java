package org.duckdns.hjow.swingwebpanel.css.anim;

/**
 * A document timeline of Web Animations Level 1 (sections 4.3.1, 4.3.2 and 6.3, <code>DocumentTimeline</code>): its current time is the time of
 * the document (milliseconds since the time origin of the page, the clock of <code>performance.now()</code> and <code>requestAnimationFrame</code>)
 * minus its origin time. The default document timeline (<code>document.timeline</code>) has the origin time 0; scripts create others with
 * <code>new DocumentTimeline({originTime})</code>. A timeline is inactive (its current time is NaN) while the document has no time, which is the
 * case for a document without a script host and after the page was disposed. It is monotonically increasing.
 * <p>
 * Web Animations Level 1 의 문서 타임라인(4.3.1, 4.3.2, 6.3 절, <code>DocumentTimeline</code>): 현재 시간은 문서의 시간(페이지의 시간 기준점 이후 밀리초,
 * <code>performance.now()</code> 와 <code>requestAnimationFrame</code> 의 시계)에서 원점 시간을 뺀 것이다. 기본 문서 타임라인(<code>document.timeline</code>)의
 * 원점 시간은 0 이며 스크립트는 <code>new DocumentTimeline({originTime})</code> 으로 다른 것을 만든다. 문서에 시간이 없는 동안(스크립트 호스트가 없는 문서와
 * 폐기된 페이지) 타임라인은 비활성(현재 시간이 NaN)이다. 단조 증가한다.
 */
public final class WebTimeline {

    /** The document registry whose time this timeline follows. 이 타임라인이 따르는 시간의 문서 등록부. */
    private final WebAnimationSet set;
    /** The origin time in milliseconds relative to the time origin. 시간 기준점에 대한 원점 시간(밀리초). */
    private final double originTime;

    /**
     * Creates a timeline of a document.
     * <p>
     * 문서의 타임라인을 만든다.
     *
     * @param set        the registry of the document, null for a timeline that is always inactive / 문서의 등록부. 늘 비활성인 타임라인이면 null
     * @param originTime the origin time in milliseconds / 원점 시간(밀리초)
     */
    public WebTimeline(WebAnimationSet set, double originTime) {
        this.set = set;
        this.originTime = originTime;
    }

    /**
     * Returns the registry of the document of the timeline.
     * <p>
     * 타임라인의 문서 등록부를 반환한다.
     *
     * @return the registry or null / 등록부 또는 null
     */
    public WebAnimationSet getSet() {
        return set;
    }

    /**
     * Returns the origin time.
     * <p>
     * 원점 시간을 반환한다.
     *
     * @return milliseconds / 밀리초
     */
    public double getOriginTime() {
        return originTime;
    }

    /**
     * Returns the current time (<code>timeline.currentTime</code>): it changes only at an animation frame while something animates.
     * <p>
     * 현재 시간을 반환한다(<code>timeline.currentTime</code>): 무언가가 애니메이션되는 동안에는 애니메이션 프레임에서만 바뀐다.
     *
     * @return milliseconds, NaN if inactive / 밀리초. 비활성이면 NaN
     */
    public double getCurrentTime() {
        double t = set == null ? Double.NaN : set.documentTime();
        return Double.isNaN(t) ? Double.NaN : t - originTime;
    }

    /**
     * Tells whether the timeline is active (has a current time).
     * <p>
     * 타임라인이 활성인지(현재 시간이 있는지) 알려 준다.
     *
     * @return true if active / 활성이면 true
     */
    public boolean isActive() {
        return !Double.isNaN(getCurrentTime());
    }

    /**
     * Converts a time of this timeline to a time relative to the time origin (4.3.1): the time plus the origin time.
     * <p>
     * 이 타임라인의 시간을 시간 기준점에 대한 시간으로 바꾼다(4.3.1): 시간에 원점 시간을 더한다.
     *
     * @param time the timeline time / 타임라인 시간
     * @return the origin-relative time, NaN if inactive or the input is NaN / 기준점 상대 시간. 비활성이거나 입력이 NaN 이면 NaN
     */
    public double toOriginRelative(double time) {
        return !isActive() || Double.isNaN(time) ? Double.NaN : time + originTime;
    }
}
