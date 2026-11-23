package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleResolver;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.MutationRecord;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;

/**
 * The observers that watch the geometry of the page, <code>IntersectionObserver</code> and <code>ResizeObserver</code>, and the "update the rendering" step that
 * drives them (Intersection Observer, W3C Working Draft 18 October 2023, and Resize Observer Module Level 1, fetched 2026-11-17; see
 * <code>docs/progress/webapi2.md</code>). The page has no frame clock of its own, so a step is queued ({@link #UPDATE_DELAY_MS} ms later, once for a burst of
 * changes) whenever something that can change the geometry happened and an observer has a target: an observed target is added, the document changed, the
 * viewport was resized or scrolled, or the component published a new layout. A step first runs the resize observers (the loop of "gather active observations at
 * depth", "broadcast active observations", deeper targets only), then the intersection observers, the order of the HTML event loop. The geometry comes from the
 * layout hook of the environment ({@link HostEnvironment#getElementBounds}, in viewport pixels) and the computed styles of the style resolver. Every method runs
 * on the thread that owns the page's scripts (under the page lock).
 * <p>
 * 페이지의 기하를 지켜보는 관찰자 <code>IntersectionObserver</code>, <code>ResizeObserver</code> 와 그것을 구동하는 "렌더링 갱신" 단계(Intersection Observer, W3C
 * Working Draft 2023-10-18 와 Resize Observer Module Level 1, 2026-11-17 조회, <code>docs/progress/webapi2.md</code> 참조). 페이지에는 자체 프레임 시계가
 * 없으므로 기하를 바꿀 수 있는 일이 일어나고 관찰자에 대상이 있으면(관찰 대상 추가, 문서 변경, 뷰포트 크기 변경이나 스크롤, 컴포넌트가 새 레이아웃을 게시) 단계를
 * {@link #UPDATE_DELAY_MS} ms 뒤에 한 번(변경이 몰려도 한 번) 예약한다. 단계는 먼저 크기 관찰자(깊이별로 "활성 관찰 모으기", "활성 관찰 방송"을 되풀이하며 더 깊은
 * 대상만), 그 다음 교차 관찰자를 실행한다(HTML 이벤트 루프의 순서). 기하는 환경의 레이아웃 훅({@link HostEnvironment#getElementBounds}, 뷰포트 픽셀)과
 * 스타일 리졸버의 계산 스타일에서 온다. 모든 메서드는 페이지 스크립트를 소유한 스레드에서(페이지 잠금 아래) 실행된다.
 */
final class RenderObservers {

    /**
     * How long after the first change of a burst the rendering step runs, in milliseconds (the length of a frame at 60 Hz, like <code>requestAnimationFrame</code>).
     * <p>
     * 변경이 몰릴 때 첫 변경 뒤 렌더링 단계가 실행되기까지의 시간(밀리초). 60Hz 한 프레임의 길이이며 <code>requestAnimationFrame</code> 과 같다.
     */
    static final long UPDATE_DELAY_MS = 16;

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The intersection observers.
     * <p>
     * 교차 관찰자.
     */
    private final IntersectionObservers intersections;

    /**
     * The resize observers.
     * <p>
     * 크기 관찰자.
     */
    private final ResizeObservers resizes;

    /**
     * The step that is queued, or null.
     * <p>
     * 예약된 단계. 없으면 null.
     */
    private Scheduler.Handle pending;

    /**
     * True once the page is disposed.
     * <p>
     * 페이지가 폐기된 뒤이면 true.
     */
    private boolean disposed;

    /**
     * True while a step runs (a step does not nest).
     * <p>
     * 단계가 실행되는 동안 true(단계는 중첩되지 않는다).
     */
    private boolean updating;

    /**
     * Creates the observers of a page.
     * <p>
     * 페이지의 관찰자를 만든다.
     *
     * @param page the page / 페이지
     */
    RenderObservers(PageScripts page) {
        this.page = page;
        this.intersections = new IntersectionObservers(page, this);
        this.resizes = new ResizeObservers(page, this);
    }

    /**
     * Installs the constructors and the entry interfaces on the global object.
     * <p>
     * 전역 객체에 생성자와 항목 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        intersections.install(g);
        resizes.install(g);
    }

    /**
     * Tells whether some observer has a target.
     * <p>
     * 어떤 관찰자에 대상이 있는지 알려 준다.
     *
     * @return true if one has / 있으면 true
     */
    boolean hasTargets() {
        return intersections.hasTargets() || resizes.hasTargets();
    }

    /**
     * Queues a rendering step unless one is queued already or nothing is observed. It only posts to the scheduler and may be called from any thread.
     * <p>
     * 이미 예약되었거나 관찰하는 것이 없으면 아니고, 그 밖에는 렌더링 단계를 예약한다. 스케줄러에 올리기만 하며 어느 스레드에서나 부를 수 있다.
     */
    void requestUpdate() {
        Scheduler s = page.getEnvironment().getScheduler();
        synchronized (this) {
            if (disposed || pending != null || s == null || !hasTargets()) {
                return;
            }
            pending = s.post(new Runnable() {
                /**
                 * Runs the step as a page task.
                 * <p>
                 * 단계를 페이지 작업으로 실행한다.
                 */
                public void run() {
                    page.runTask(new Runnable() {
                        /**
                         * Runs the step.
                         * <p>
                         * 단계를 실행한다.
                         */
                        public void run() {
                            synchronized (RenderObservers.this) {
                                pending = null;
                            }
                            update();
                        }
                    });
                }
            }, UPDATE_DELAY_MS);
        }
    }

    /**
     * Tells the observers that a change of the document happened (the geometry may have changed).
     * <p>
     * 문서가 바뀌었음을 관찰자에게 알린다(기하가 바뀌었을 수 있다).
     *
     * @param record  the change / 변경
     * @param attached whether the changed node is in the document / 바뀐 노드가 문서에 있는지 여부
     */
    void mutated(MutationRecord record, boolean attached) {
        requestUpdate();
    }

    /**
     * Runs the rendering step now: the resize observers, then the intersection observers. A callback that changes the page queues the next step.
     * <p>
     * 렌더링 단계를 지금 실행한다: 크기 관찰자, 그 다음 교차 관찰자. 콜백이 페이지를 바꾸면 다음 단계가 예약된다.
     */
    void update() {
        if (disposed || updating) {
            return;
        }
        updating = true;
        try {
            resizes.run();
            intersections.run();
        } finally {
            updating = false;
        }
    }

    /**
     * Drops the observers and the queued step (the page is disposed).
     * <p>
     * 관찰자와 예약된 단계를 버린다(페이지가 폐기됨).
     */
    void dispose() {
        Scheduler.Handle h;
        synchronized (this) {
            disposed = true;
            h = pending;
            pending = null;
        }
        if (h != null) {
            h.cancel();
        }
        intersections.dispose();
        resizes.dispose();
    }

    /**
     * Returns the border box of an element in viewport pixels from the layout hook.
     * <p>
     * 레이아웃 훅에서 요소의 border box 를 뷰포트 픽셀로 반환한다.
     *
     * @param e the element / 요소
     * @return x, y, width, height / x, y, 폭, 높이
     */
    double[] bounds(Element e) {
        return page.getEnvironment().getElementBounds(e);
    }

    /**
     * Tells whether an element has a box in the layout (it is in the document and not <code>display: none</code> itself or by an ancestor).
     * <p>
     * 요소가 레이아웃에 상자를 가지는지 알려 준다(문서에 있고 자신이나 조상이 <code>display: none</code> 이 아니다).
     *
     * @param e the element / 요소
     * @return true if it is rendered / 렌더링되면 true
     */
    boolean isRendered(Element e) {
        if (!e.isConnected()) {
            return false;
        }
        double[][] rects = page.getEnvironment().getElementRects(e);
        // 호스트가 상자별 사각형을 알려 주면 상자가 있는지로, 아니면 크기가 있는지로 판단한다.
        if (rects != null) {
            return rects.length > 0;
        }
        double[] b = bounds(e);
        return b[2] > 0 || b[3] > 0;
    }

    /**
     * Returns the computed style of an element, or null if the page has no style resolver.
     * <p>
     * 요소의 계산 스타일을 반환한다. 페이지에 스타일 리졸버가 없으면 null.
     *
     * @param e the element / 요소
     * @return the style or null / 스타일 또는 null
     */
    ComputedStyle style(Element e) {
        StyleResolver r = page.getEnvironment().getStyleResolver();
        return r == null ? null : r.getComputedStyle(e);
    }

    /**
     * Returns the time in milliseconds since the page started (<code>performance.now()</code>).
     * <p>
     * 페이지가 시작된 뒤의 시간(밀리초)을 반환한다(<code>performance.now()</code>).
     *
     * @return the time / 시간
     */
    double now() {
        Scheduler sch = page.getEnvironment().getScheduler();
        long t = sch == null ? System.currentTimeMillis() : sch.now();
        return t - page.startTime();
    }

    /**
     * Freezes a script array (the value of a <code>FrozenArray</code> attribute) with <code>Object.freeze</code> of the realm.
     * <p>
     * 스크립트 배열을 realm 의 <code>Object.freeze</code> 로 얼린다(<code>FrozenArray</code> 속성의 값).
     *
     * @param array the array / 배열
     * @return the same array / 같은 배열
     */
    JsValue freeze(JsValue array) {
        JsRealm realm = page.getRealm();
        JsValue object = realm.getGlobalObject().get("Object");
        // Object.freeze 가 있으면 배열을 얼린다.
        if (object instanceof JsObject && ((JsObject) object).get("freeze").isCallable()) {
            Ops.call(((JsObject) object).get("freeze"), object, new JsValue[] { array });
        }
        return array;
    }

    /**
     * Defines a read-only accessor on a prototype.
     * <p>
     * 프로토타입에 읽기 전용 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the attribute / 속성 이름
     * @param get   the getter / 게터
     */
    void getter(JsObject proto, String name, NativeFunction.Call get) {
        proto.defineAccessor(name, page.getRealm().newFunction("get " + name, 0, get), null, true, true);
    }

    /**
     * Returns a number that a script passed as an option, or throws the error that WebIDL gives for a value that is not a finite number.
     * <p>
     * 스크립트가 옵션으로 준 숫자를 반환한다. 유한한 숫자가 아니면 WebIDL 이 정한 오류를 던진다.
     *
     * @param v    the value / 값
     * @param what the description for the message / 메시지에 쓸 설명
     * @return the number / 숫자
     */
    static double finite(JsValue v, String what) {
        double d = Ops.toNumber(v);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw JsException.typeError("The provided double value is non-finite (" + what + ").");
        }
        return d;
    }
}
