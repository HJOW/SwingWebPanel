package org.duckdns.hjow.swingwebpanel.css.anim;

/**
 * The time source of the animations of a document (plan item P10-07): the engine reads it whenever it computes styles or advances.
 * The component uses the system time; tests use a clock that they set by hand, so that the value of an animation at a given moment, the
 * delays and the order of the events are checked deterministically. The same clock structure is meant for <code>requestAnimationFrame</code>
 * (P15-07): all timers of a document run on one time line.
 * <p>
 * 문서의 애니메이션의 시간 원천(계획 P10-07): 엔진은 스타일을 계산하거나 진행할 때마다 읽는다. 컴포넌트는 시스템 시간을 쓰고, 시험은 손으로 정하는 시계를 써서 어느 순간의
 * 애니메이션 값, 지연, 이벤트 순서를 결정적으로 확인한다. 같은 시계 구조를 <code>requestAnimationFrame</code>(P15-07)에도 쓰도록 한 것이다: 문서의 모든 타이머는
 * 하나의 시간 축에서 움직인다.
 */
public interface AnimationClock {

    /**
     * The system clock: <code>System.currentTimeMillis()</code>.
     * <p>
     * 시스템 시계: <code>System.currentTimeMillis()</code>.
     */
    AnimationClock SYSTEM = new AnimationClock() {
        /**
         * Returns the system time.
         * <p>
         * 시스템 시간을 반환한다.
         *
         * @return milliseconds / 밀리초
         */
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    };

    /**
     * Returns the current time.
     * <p>
     * 현재 시각을 반환한다.
     *
     * @return milliseconds on a monotonic time line / 단조 증가하는 시간 축의 밀리초
     */
    long nowMillis();
}
