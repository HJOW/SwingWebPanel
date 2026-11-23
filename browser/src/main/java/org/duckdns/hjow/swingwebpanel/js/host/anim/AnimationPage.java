package org.duckdns.hjow.swingwebpanel.js.host.anim;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.HostEnvironment;

/**
 * What the script binding of Web Animations needs from the page whose scripts it serves: the realm, the DOM bridge, the document, the environment,
 * the document clock (the clock of <code>performance.now()</code> and <code>requestAnimationFrame</code>), whether scripts may run, error reporting,
 * and animation frames shared with <code>requestAnimationFrame</code> (the step "update animations and send events" runs right before the
 * animation frame callbacks of the same frame, with the same time). The page's script host provides it.
 * <p>
 * Web Animations 의 스크립트 연결이 그 스크립트를 담당하는 페이지에서 필요로 하는 것: realm, DOM 브리지, 문서, 환경, 문서 시계(<code>performance.now()</code> 와
 * <code>requestAnimationFrame</code> 의 시계), 스크립트를 실행할 수 있는지 여부, 오류 보고, <code>requestAnimationFrame</code> 과 함께 쓰는 애니메이션 프레임("update
 * animations and send events" 단계는 같은 프레임의 애니메이션 프레임 콜백 바로 전에 같은 시각으로 실행된다). 페이지의 스크립트 호스트가 제공한다.
 */
public interface AnimationPage {

    /**
     * A receiver of the animation frames.
     * <p>
     * 애니메이션 프레임을 받는 쪽.
     */
    interface FrameHook {
        /**
         * Runs the Web Animations part of an animation frame as part of a page task.
         * <p>
         * 페이지 작업의 일부로 애니메이션 프레임의 Web Animations 부분을 실행한다.
         *
         * @param now the time of the frame in milliseconds since the time origin / 시간 기준점 이후 밀리초의 프레임 시각
         */
        void frame(double now);
    }

    /**
     * Returns the realm of the page.
     * <p>
     * 페이지의 realm 을 반환한다.
     *
     * @return the realm / realm
     */
    JsRealm getRealm();

    /**
     * Returns the DOM bridge of the page.
     * <p>
     * 페이지의 DOM 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    DomBridge getBridge();

    /**
     * Returns the document of the page.
     * <p>
     * 페이지의 문서를 반환한다.
     *
     * @return the document / 문서
     */
    Document getDocument();

    /**
     * Returns the environment of the page.
     * <p>
     * 페이지의 환경을 반환한다.
     *
     * @return the environment / 환경
     */
    HostEnvironment getEnvironment();

    /**
     * Returns the time now in milliseconds since the time origin of the page (the value of <code>performance.now()</code>).
     * <p>
     * 페이지의 시간 기준점 이후 지금 시간(밀리초)을 반환한다(<code>performance.now()</code> 의 값).
     *
     * @return milliseconds / 밀리초
     */
    double now();

    /**
     * Tells whether scripts may run on the page.
     * <p>
     * 페이지에서 스크립트를 실행할 수 있는지 알려 준다.
     *
     * @return true if they may / 실행할 수 있으면 true
     */
    boolean scripting();

    /**
     * Reports an error that a listener or a callback threw.
     * <p>
     * 리스너나 콜백이 던진 오류를 보고한다.
     *
     * @param e the error / 오류
     */
    void reportError(RuntimeException e);

    /**
     * Installs the receiver of the animation frames.
     * <p>
     * 애니메이션 프레임을 받는 쪽을 설치한다.
     *
     * @param hook the receiver / 받는 쪽
     */
    void setFrameHook(FrameHook hook);

    /**
     * Asks for an animation frame (about 16 ms later, shared with <code>requestAnimationFrame</code>).
     * <p>
     * 애니메이션 프레임을 요청한다(약 16ms 뒤, <code>requestAnimationFrame</code> 과 공유).
     */
    void requestFrame();
}
