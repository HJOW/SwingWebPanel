package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.host.anim.AnimationPage;

/**
 * The page side that the Web Animations binding ({@link org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi}) uses (P10-08-h): it gives the binding the
 * realm, the bridge, the document and the environment of the page, the clock of <code>performance.now()</code>, and the animation frames of
 * {@link Timers} (the Web Animations step runs right before the <code>requestAnimationFrame</code> callbacks of the same frame).
 * <p>
 * Web Animations 연결({@link org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi})이 쓰는 페이지 쪽(P10-08-h): 연결에 페이지의 realm, 브리지, 문서, 환경,
 * <code>performance.now()</code> 의 시계, {@link Timers} 의 애니메이션 프레임을 준다(Web Animations 단계는 같은 프레임의 <code>requestAnimationFrame</code> 콜백 바로 전에
 * 실행된다).
 */
final class AnimationHostImpl implements AnimationPage {

    /** The page. 페이지. */
    private final PageScripts page;

    /**
     * Creates the page side.
     * <p>
     * 페이지 쪽을 만든다.
     *
     * @param page the page / 페이지
     */
    AnimationHostImpl(PageScripts page) {
        this.page = page;
    }

    /**
     * Returns the realm.
     * <p>
     * realm 을 반환한다.
     *
     * @return the realm / realm
     */
    public JsRealm getRealm() {
        return page.getRealm();
    }

    /**
     * Returns the bridge.
     * <p>
     * 브리지를 반환한다.
     *
     * @return the bridge / 브리지
     */
    public DomBridge getBridge() {
        return page.getBridge();
    }

    /**
     * Returns the document.
     * <p>
     * 문서를 반환한다.
     *
     * @return the document / 문서
     */
    public Document getDocument() {
        return page.getDocument();
    }

    /**
     * Returns the environment.
     * <p>
     * 환경을 반환한다.
     *
     * @return the environment / 환경
     */
    public HostEnvironment getEnvironment() {
        return page.getEnvironment();
    }

    /**
     * Returns the time since the start of the page on the scheduler clock (the clock of <code>performance.now()</code> and of the animation frames).
     * <p>
     * 스케줄러 시계로 페이지 시작 이후 시간을 반환한다(<code>performance.now()</code> 와 애니메이션 프레임의 시계).
     *
     * @return milliseconds / 밀리초
     */
    public double now() {
        Scheduler s = page.getEnvironment().getScheduler();
        return s == null ? System.currentTimeMillis() - page.startTime() : s.now() - page.startTime();
    }

    /**
     * Tells whether scripts may run.
     * <p>
     * 스크립트를 실행할 수 있는지 알려 준다.
     *
     * @return true if they may / 실행할 수 있으면 true
     */
    public boolean scripting() {
        return page.scripting();
    }

    /**
     * Reports an error.
     * <p>
     * 오류를 보고한다.
     *
     * @param e the error / 오류
     */
    public void reportError(RuntimeException e) {
        page.reportError(e);
    }

    /**
     * Installs the receiver of the animation frames on the timers of the page.
     * <p>
     * 페이지의 타이머에 애니메이션 프레임을 받는 쪽을 설치한다.
     *
     * @param hook the receiver / 받는 쪽
     */
    public void setFrameHook(FrameHook hook) {
        page.getTimers().setAnimationHook(hook);
    }

    /**
     * Asks the timers of the page for an animation frame.
     * <p>
     * 페이지의 타이머에 애니메이션 프레임을 요청한다.
     */
    public void requestFrame() {
        page.getTimers().scheduleFrame();
    }

    /**
     * Creates and installs the Web Animations binding of a page.
     * <p>
     * 페이지의 Web Animations 연결을 만들어 설치한다.
     *
     * @param page the page / 페이지
     * @return the binding / 연결
     */
    static org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi install(PageScripts page) {
        org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi api = new org.duckdns.hjow.swingwebpanel.js.host.anim.WebAnimationsApi(new AnimationHostImpl(page));
        api.install();
        return api;
    }
}
