package org.duckdns.hjow.swingwebpanel.css.anim;

/**
 * What the Web Animations model needs from the script side of a page (Web Animations Level 1): promises for the <code>ready</code> and
 * <code>finished</code> attributes of the animations, microtasks for the "finish notification steps", and a new animation frame (a call of
 * {@link WebAnimationSet#update(double)} followed by a microtask checkpoint and the dispatch of {@link WebAnimationSet#takeEvents()}) whenever
 * something waits for one. The page's script host implements it with real promises and its event loop; tests implement it with plain records.
 * Every method is called on the document thread.
 * <p>
 * Web Animations 모델이 페이지의 스크립트 쪽에서 필요로 하는 것(Web Animations Level 1): 애니메이션의 <code>ready</code>, <code>finished</code> 속성을 위한
 * 프라미스, "finish notification steps" 를 위한 마이크로태스크, 무언가가 기다릴 때의 새 애니메이션 프레임({@link WebAnimationSet#update(double)} 호출과 이어지는
 * 마이크로태스크 검사점, {@link WebAnimationSet#takeEvents()} 의 전달). 페이지의 스크립트 호스트는 실제 프라미스와 이벤트 루프로 구현하고 시험은 단순한 기록으로
 * 구현한다. 모든 메서드는 문서 스레드에서 불린다.
 */
public interface WebAnimationHost {

    /**
     * A promise of an animation (<code>ready</code> or <code>finished</code>) that the model settles.
     * <p>
     * 모델이 확정하는 애니메이션의 프라미스(<code>ready</code> 또는 <code>finished</code>).
     */
    interface PromiseSlot {
        /**
         * Resolves the promise with its animation (no effect once settled).
         * <p>
         * 프라미스를 애니메이션으로 resolve 한다(이미 확정되었으면 효과 없음).
         */
        void resolve();

        /**
         * Rejects the promise with a <code>DOMException</code> named <code>AbortError</code> and marks it as handled (no effect once settled).
         * <p>
         * 프라미스를 이름이 <code>AbortError</code> 인 <code>DOMException</code> 으로 reject 하고 처리됨으로 표시한다(이미 확정되었으면 효과 없음).
         */
        void reject();

        /**
         * Tells whether the promise is still pending.
         * <p>
         * 프라미스가 아직 대기 중인지 알려 준다.
         *
         * @return true while pending / 대기 중이면 true
         */
        boolean isPending();
    }

    /**
     * Creates a promise of an animation.
     * <p>
     * 애니메이션의 프라미스를 만든다.
     *
     * @param animation the animation that is the resolution value / resolve 값이 되는 애니메이션
     * @param resolved  true for a promise that is already resolved with the animation / 이미 애니메이션으로 resolve 된 프라미스이면 true
     * @return the promise / 프라미스
     */
    PromiseSlot newPromise(WebAnimation animation, boolean resolved);

    /**
     * Queues a microtask.
     * <p>
     * 마이크로태스크를 큐에 넣는다.
     *
     * @param task the task / 작업
     */
    void queueMicrotask(Runnable task);

    /**
     * Asks for an animation frame soon (about 16 ms later); repeated requests before the frame give one frame.
     * <p>
     * 곧(약 16ms 뒤) 애니메이션 프레임을 요청한다. 프레임 전의 반복 요청은 프레임 하나가 된다.
     */
    void requestFrame();

    /**
     * Tells that the animated style of the page changed outside a frame (the component renders again).
     * <p>
     * 프레임 밖에서 페이지의 애니메이션 스타일이 바뀌었음을 알린다(컴포넌트가 다시 렌더링한다).
     */
    void styleChanged();
}
