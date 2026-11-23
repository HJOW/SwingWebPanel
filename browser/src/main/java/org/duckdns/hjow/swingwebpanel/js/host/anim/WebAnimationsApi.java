package org.duckdns.hjow.swingwebpanel.js.host.anim;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.duckdns.hjow.swingwebpanel.css.CssEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationEngine;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationException;
import org.duckdns.hjow.swingwebpanel.css.anim.AnimationPlaybackEvent;
import org.duckdns.hjow.swingwebpanel.css.anim.EffectTiming;
import org.duckdns.hjow.swingwebpanel.css.anim.KeyframeEffectModel;
import org.duckdns.hjow.swingwebpanel.css.anim.WebAnimation;
import org.duckdns.hjow.swingwebpanel.css.anim.WebAnimationHost;
import org.duckdns.hjow.swingwebpanel.css.anim.WebAnimationSet;
import org.duckdns.hjow.swingwebpanel.css.anim.WebTimeline;
import org.duckdns.hjow.swingwebpanel.css.cascade.StyleAnimator;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * The script interface of Web Animations Level 1 (W3C Working Draft 5 June 2023, https://www.w3.org/TR/2023/WD-web-animations-1-20230605/, plan item
 * P10-08-h): <code>Animation</code> (constructor, <code>id</code>, <code>effect</code>, <code>timeline</code>, <code>startTime</code>,
 * <code>currentTime</code>, <code>playbackRate</code>, <code>playState</code>, <code>replaceState</code>, <code>pending</code>, <code>ready</code>,
 * <code>finished</code>, <code>onfinish</code>, <code>oncancel</code>, <code>onremove</code>, <code>play()</code>, <code>pause()</code>,
 * <code>reverse()</code>, <code>finish()</code>, <code>cancel()</code>, <code>updatePlaybackRate()</code>, <code>persist()</code>,
 * <code>commitStyles()</code>), <code>AnimationEffect</code> (<code>getTiming()</code>, <code>getComputedTiming()</code>, <code>updateTiming()</code>),
 * <code>KeyframeEffect</code> (both constructors, <code>target</code>, <code>pseudoElement</code>, <code>composite</code>, <code>iterationComposite</code>
 * of Level 2 which is stored only, <code>getKeyframes()</code>, <code>setKeyframes()</code>), <code>AnimationTimeline</code> and
 * <code>DocumentTimeline</code> (constructor with <code>originTime</code>), <code>AnimationPlaybackEvent</code>, <code>Element.animate()</code>,
 * <code>Element.getAnimations({subtree})</code>, <code>Document.getAnimations()</code> and <code>document.timeline</code>. The model lives in
 * {@link org.duckdns.hjow.swingwebpanel.css.anim}: the binding connects to the {@link WebAnimationSet} of the page's {@link AnimationEngine} (so the effects reach the
 * computed style and the rendering) and acts as its {@link WebAnimationHost}: promises are real promises of the realm, the finish notification steps
 * are microtasks, and the step "update animations and send events" runs in the animation frames of <code>requestAnimationFrame</code> with the same
 * time, followed by a microtask checkpoint and the dispatch of the playback events. All methods run on the thread that owns the page's scripts.
 * <p>
 * Web Animations Level 1(W3C Working Draft 2023-06-05, 계획 P10-08-h)의 스크립트 인터페이스: <code>Animation</code>(생성자와 모든 속성·메서드),
 * <code>AnimationEffect</code>, <code>KeyframeEffect</code>(두 생성자, Level 2 의 <code>iterationComposite</code> 는 보관만), <code>AnimationTimeline</code>,
 * <code>DocumentTimeline</code>(<code>originTime</code> 생성자), <code>AnimationPlaybackEvent</code>, <code>Element.animate()</code>,
 * <code>Element.getAnimations({subtree})</code>, <code>Document.getAnimations()</code>, <code>document.timeline</code>. 모델은 {@link org.duckdns.hjow.swingwebpanel.css.anim}
 * 에 있다: 연결은 페이지의 {@link AnimationEngine} 이 가진 {@link WebAnimationSet} 에 이어지고(그래서 효과가 계산 스타일과 렌더링에 닿는다) 그 {@link WebAnimationHost}
 * 역할을 한다: 프라미스는 realm 의 실제 프라미스, 끝 알림 단계는 마이크로태스크이며, "update animations and send events" 단계는 <code>requestAnimationFrame</code> 의
 * 애니메이션 프레임에서 같은 시각으로 실행되고 마이크로태스크 검사점과 재생 이벤트 전달이 뒤따른다. 모든 메서드는 페이지 스크립트를 소유한 스레드에서 실행된다.
 */
public final class WebAnimationsApi implements WebAnimationHost, WebAnimationSet.TimeSource, AnimationPage.FrameHook {

    /** The page. 페이지. */
    private final AnimationPage page;
    /** The Web Animations state of the document, created at the first use. 문서의 Web Animations 상태. 처음 쓸 때 만든다. */
    private WebAnimationSet set;
    /** The (inactive) default timelines of documents other than the page's document. 페이지 문서가 아닌 문서의 (비활성) 기본 타임라인. */
    private final Map<Document, WebTimeline> otherTimelines = new WeakHashMap<Document, WebTimeline>();
    /** Set by {@link #dispose()}. {@link #dispose()} 가 정한다. */
    private volatile boolean disposed;

    /**
     * Creates the binding of a page; {@link #install()} defines the interfaces.
     * <p>
     * 페이지의 연결을 만든다. {@link #install()} 이 인터페이스를 정의한다.
     *
     * @param page the page / 페이지
     */
    public WebAnimationsApi(AnimationPage page) {
        this.page = page;
    }

    // ------------------------------------------------------------------------------------------------ model connection

    /**
     * Returns the Web Animations state of the page's document: the one of the animation engine of the page's style engine, or a state without styles
     * when the page has no style engine (a page without rendering).
     * <p>
     * 페이지 문서의 Web Animations 상태를 반환한다: 페이지 스타일 엔진의 애니메이션 엔진이 가진 것, 페이지에 스타일 엔진이 없으면(렌더링 없는 페이지) 스타일 없는 상태.
     *
     * @return the state / 상태
     */
    WebAnimationSet set() {
        if (set == null) {
            CssEngine css = page.getEnvironment().getCssEngine();
            StyleAnimator animator = css == null ? null : css.getResolver().getAnimator();
            set = animator instanceof AnimationEngine ? ((AnimationEngine) animator).getWebAnimations() : new WebAnimationSet(null, null);
            if (!disposed) {
                set.attach(this, this);
            }
        }
        return set;
    }

    /**
     * Returns the default timeline of a document: the active one of the page's document, an inactive one for other documents.
     * <p>
     * 문서의 기본 타임라인을 반환한다: 페이지 문서는 활성인 것, 다른 문서는 비활성인 것.
     *
     * @param d the document / 문서
     * @return the timeline / 타임라인
     */
    private WebTimeline timelineOf(Document d) {
        if (d == page.getDocument()) {
            return set().getTimeline();
        }
        WebTimeline t = otherTimelines.get(d);
        if (t == null) {
            t = new WebTimeline(null, 0);
            otherTimelines.put(d, t);
        }
        return t;
    }

    /**
     * Stops the script animations of the page (the scripts are stopped or the page is disposed); safe on any thread.
     * <p>
     * 페이지의 스크립트 애니메이션을 멈춘다(스크립트가 멈추거나 페이지가 폐기된다). 어느 스레드에서나 안전하다.
     */
    public void dispose() {
        disposed = true;
        WebAnimationSet s = set;
        if (s != null) {
            s.detach();
        }
        page.getEnvironment().animationsChanged();
    }

    // ------------------------------------------------------------------------------------------------ WebAnimationHost

    /**
     * A promise of the realm settled by the model.
     * <p>
     * 모델이 확정하는 realm 의 프라미스.
     */
    private final class Slot implements WebAnimationHost.PromiseSlot {
        /** The promise object. 프라미스 객체. */
        final JsObject promise;
        /** The resolve function. resolve 함수. */
        private JsValue resolveFn;
        /** The reject function. reject 함수. */
        private JsValue rejectFn;
        /** The animation that is the resolution value. resolve 값인 애니메이션. */
        private final WebAnimation animation;
        /** True while pending. 대기 중이면 true. */
        private boolean pending = true;

        /**
         * Creates a pending promise.
         * <p>
         * 대기 중인 프라미스를 만든다.
         *
         * @param a the animation / 애니메이션
         */
        Slot(WebAnimation a) {
            animation = a;
            JsRealm realm = page.getRealm();
            NativeFunction executor = realm.newFunction("", 2, new NativeFunction.Call() {
                /**
                 * Keeps the resolving functions.
                 * <p>
                 * 확정 함수를 보관한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args the functions / 함수
                 * @return undefined / undefined
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    resolveFn = Js.arg(args, 0);
                    rejectFn = Js.arg(args, 1);
                    return Js.UNDEFINED;
                }
            });
            promise = Ops.construct(realm.getIntrinsic("%Promise%"), new JsValue[] { executor });
        }

        /**
         * Resolves the promise with the animation.
         * <p>
         * 프라미스를 애니메이션으로 resolve 한다.
         */
        public void resolve() {
            if (!pending) {
                return;
            }
            pending = false;
            Ops.call(resolveFn, Js.UNDEFINED, new JsValue[] { page.getBridge().toJs(animation) });
        }

        /**
         * Rejects the promise with an <code>AbortError</code>.
         * <p>
         * 프라미스를 <code>AbortError</code> 로 reject 한다.
         */
        public void reject() {
            if (!pending) {
                return;
            }
            pending = false;
            Ops.call(rejectFn, Js.UNDEFINED, new JsValue[] { DomExceptionObjects.create(page.getBridge(), "AbortError", "The user aborted a request.") });
        }

        /**
         * Tells whether it is pending.
         * <p>
         * 대기 중인지 알려 준다.
         *
         * @return true if pending / 대기 중이면 true
         */
        public boolean isPending() {
            return pending;
        }
    }

    /**
     * Creates a promise of an animation.
     * <p>
     * 애니메이션의 프라미스를 만든다.
     *
     * @param animation the animation / 애니메이션
     * @param resolved  true for a resolved promise / resolve 된 프라미스이면 true
     * @return the promise / 프라미스
     */
    public PromiseSlot newPromise(WebAnimation animation, boolean resolved) {
        Slot s = new Slot(animation);
        if (resolved) {
            s.resolve();
        }
        return s;
    }

    /**
     * Queues a microtask on the realm; an error is reported.
     * <p>
     * realm 에 마이크로태스크를 넣는다. 오류는 보고한다.
     *
     * @param task the task / 작업
     */
    public void queueMicrotask(final Runnable task) {
        page.getRealm().enqueueJob(new Runnable() {
            /**
             * Runs the task.
             * <p>
             * 작업을 실행한다.
             */
            public void run() {
                try {
                    task.run();
                } catch (JsException e) {
                    page.reportError(e);
                }
            }
        });
    }

    /**
     * Asks for an animation frame.
     * <p>
     * 애니메이션 프레임을 요청한다.
     */
    public void requestFrame() {
        if (!disposed && page.scripting()) {
            page.requestFrame();
        }
    }

    /**
     * Tells the component that the animated style changed.
     * <p>
     * 애니메이션 스타일이 바뀌었음을 컴포넌트에 알린다.
     */
    public void styleChanged() {
        if (!disposed) {
            page.getEnvironment().animationsChanged();
        }
    }

    /**
     * Returns the document clock (the clock of <code>performance.now()</code>).
     * <p>
     * 문서 시계(<code>performance.now()</code> 의 시계)를 반환한다.
     *
     * @return milliseconds since the time origin / 시간 기준점 이후 밀리초
     */
    public double now() {
        return page.now();
    }

    /**
     * Runs "update animations and send events" at the time of an animation frame: the model is updated, the microtasks run (the promises settle
     * first), then the playback events are dispatched in their order, and the component renders again.
     * <p>
     * 애니메이션 프레임의 시각에 "update animations and send events" 를 실행한다: 모델을 갱신하고, 마이크로태스크를 실행하며(프라미스가 먼저 확정된다), 재생 이벤트를
     * 순서대로 전달하고, 컴포넌트가 다시 렌더링하게 한다.
     *
     * @param now the frame time / 프레임 시각
     */
    public void frame(double now) {
        WebAnimationSet s = set;
        if (disposed || s == null) {
            return;
        }
        s.update(now);
        page.getRealm().runJobs();
        // 정렬된 재생 이벤트를 대상 애니메이션에 차례로 전달한다.
        for (WebAnimationSet.PendingEvent e : s.takeEvents()) {
            if (disposed) {
                return;
            }
            e.getTarget().dispatchEvent(e.getEvent());
        }
        styleChanged();
    }

    // ------------------------------------------------------------------------------------------------ install

    /** Operation code: the getter of the animation id. 연산 번호: 애니메이션 id 읽기. */
    private static final int A_ID = 1;
    /** Operation code: the setter of the animation id. 연산 번호: 애니메이션 id 쓰기. */
    private static final int A_ID_SET = 2;
    /** Operation code: the getter of the effect. 연산 번호: effect 읽기. */
    private static final int A_EFFECT = 3;
    /** Operation code: the setter of the effect. 연산 번호: effect 쓰기. */
    private static final int A_EFFECT_SET = 4;
    /** Operation code: the getter of the timeline. 연산 번호: timeline 읽기. */
    private static final int A_TIMELINE = 5;
    /** Operation code: the setter of the timeline. 연산 번호: timeline 쓰기. */
    private static final int A_TIMELINE_SET = 6;
    /** Operation code: the getter of the start time. 연산 번호: startTime 읽기. */
    private static final int A_START = 7;
    /** Operation code: the setter of the start time. 연산 번호: startTime 쓰기. */
    private static final int A_START_SET = 8;
    /** Operation code: the getter of the current time. 연산 번호: currentTime 읽기. */
    private static final int A_CURRENT = 9;
    /** Operation code: the setter of the current time. 연산 번호: currentTime 쓰기. */
    private static final int A_CURRENT_SET = 10;
    /** Operation code: the getter of the playback rate. 연산 번호: playbackRate 읽기. */
    private static final int A_RATE = 11;
    /** Operation code: the setter of the playback rate. 연산 번호: playbackRate 쓰기. */
    private static final int A_RATE_SET = 12;
    /** Operation code: the play state. 연산 번호: 재생 상태. */
    private static final int A_PLAY_STATE = 13;
    /** Operation code: the replace state. 연산 번호: 교체 상태. */
    private static final int A_REPLACE_STATE = 14;
    /** Operation code: the pending flag. 연산 번호: 대기 표시. */
    private static final int A_PENDING = 15;
    /** Operation code: the ready promise. 연산 번호: ready 프라미스. */
    private static final int A_READY = 16;
    /** Operation code: the finished promise. 연산 번호: finished 프라미스. */
    private static final int A_FINISHED = 17;
    /** Operation code: the cancel method. 연산 번호: cancel 메서드. */
    private static final int A_CANCEL = 18;
    /** Operation code: the finish method. 연산 번호: finish 메서드. */
    private static final int A_FINISH = 19;
    /** Operation code: the play method. 연산 번호: play 메서드. */
    private static final int A_PLAY = 20;
    /** Operation code: the pause method. 연산 번호: pause 메서드. */
    private static final int A_PAUSE = 21;
    /** Operation code: the updatePlaybackRate method. 연산 번호: updatePlaybackRate 메서드. */
    private static final int A_UPDATE_RATE = 22;
    /** Operation code: the reverse method. 연산 번호: reverse 메서드. */
    private static final int A_REVERSE = 23;
    /** Operation code: the persist method. 연산 번호: persist 메서드. */
    private static final int A_PERSIST = 24;
    /** Operation code: the commitStyles method. 연산 번호: commitStyles 메서드. */
    private static final int A_COMMIT = 25;
    /** Operation code: the getTiming method. 연산 번호: getTiming 메서드. */
    private static final int E_TIMING = 30;
    /** Operation code: the getComputedTiming method. 연산 번호: getComputedTiming 메서드. */
    private static final int E_COMPUTED = 31;
    /** Operation code: the updateTiming method. 연산 번호: updateTiming 메서드. */
    private static final int E_UPDATE = 32;
    /** Operation code: the getter of the effect target. 연산 번호: 효과 대상 읽기. */
    private static final int K_TARGET = 40;
    /** Operation code: the setter of the effect target. 연산 번호: 효과 대상 쓰기. */
    private static final int K_TARGET_SET = 41;
    /** Operation code: the getter of the pseudo-element. 연산 번호: 의사 요소 읽기. */
    private static final int K_PSEUDO = 42;
    /** Operation code: the setter of the pseudo-element. 연산 번호: 의사 요소 쓰기. */
    private static final int K_PSEUDO_SET = 43;
    /** Operation code: the getter of the composite operation. 연산 번호: 합성 연산 읽기. */
    private static final int K_COMPOSITE = 44;
    /** Operation code: the setter of the composite operation. 연산 번호: 합성 연산 쓰기. */
    private static final int K_COMPOSITE_SET = 45;
    /** Operation code: the getter of the iteration composite operation. 연산 번호: 반복 합성 연산 읽기. */
    private static final int K_ITER = 46;
    /** Operation code: the setter of the iteration composite operation. 연산 번호: 반복 합성 연산 쓰기. */
    private static final int K_ITER_SET = 47;
    /** Operation code: the getKeyframes method. 연산 번호: getKeyframes 메서드. */
    private static final int K_GET_KEYFRAMES = 48;
    /** Operation code: the setKeyframes method. 연산 번호: setKeyframes 메서드. */
    private static final int K_SET_KEYFRAMES = 49;
    /** Operation code: the current time of a timeline. 연산 번호: 타임라인의 현재 시간. */
    private static final int T_CURRENT = 60;
    /** Operation code: the animate method of elements. 연산 번호: 요소의 animate 메서드. */
    private static final int EL_ANIMATE = 70;
    /** Operation code: the getAnimations method of elements. 연산 번호: 요소의 getAnimations 메서드. */
    private static final int EL_ANIMATIONS = 71;
    /** Operation code: the getAnimations method of documents. 연산 번호: 문서의 getAnimations 메서드. */
    private static final int D_ANIMATIONS = 80;
    /** Operation code: the timeline of documents. 연산 번호: 문서의 timeline. */
    private static final int D_TIMELINE = 81;

    /** The prototype of <code>Animation</code>. <code>Animation</code> 의 프로토타입. */
    private JsObject animationProto;
    /** The prototype of <code>KeyframeEffect</code>. <code>KeyframeEffect</code> 의 프로토타입. */
    private JsObject effectProto;
    /** The prototype of <code>DocumentTimeline</code>. <code>DocumentTimeline</code> 의 프로토타입. */
    private JsObject timelineProto;

    /**
     * Calls one member of the interfaces by its operation code.
     * <p>
     * 연산 번호로 인터페이스의 멤버 하나를 부른다.
     */
    private final class Member implements NativeFunction.Call {
        /** The operation code. 연산 번호. */
        private final int op;

        /**
         * Creates the member.
         * <p>
         * 멤버를 만든다.
         *
         * @param op the operation code / 연산 번호
         */
        Member(int op) {
            this.op = op;
        }

        /**
         * Runs the member and turns the errors of the model into JavaScript errors.
         * <p>
         * 멤버를 실행하고 모델의 오류를 JavaScript 오류로 바꾼다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the result / 결과
         */
        public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            try {
                return dispatch(op, self, args);
            } catch (AnimationException e) {
                throw error(e);
            }
        }
    }

    /**
     * Turns an error of the model into a JavaScript error.
     * <p>
     * 모델의 오류를 JavaScript 오류로 바꾼다.
     *
     * @param e the error / 오류
     * @return the JavaScript exception / JavaScript 예외
     */
    private JsException error(AnimationException e) {
        if (AnimationException.TYPE_ERROR.equals(e.getKind())) {
            return JsException.typeError(e.getMessage());
        }
        return new JsException(DomExceptionObjects.create(page.getBridge(), e.getKind(), e.getMessage()));
    }

    /**
     * Defines a getter.
     * <p>
     * 읽기 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param op    the operation / 연산
     */
    private void getter(JsObject proto, String name, int op) {
        proto.defineAccessor(name, page.getRealm().newFunction("get " + name, 0, new Member(op)), null, true, true);
    }

    /**
     * Defines a getter and a setter.
     * <p>
     * 읽기와 쓰기 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param get   the getter operation / 읽기 연산
     * @param set   the setter operation / 쓰기 연산
     */
    private void accessor(JsObject proto, String name, int get, int set) {
        JsRealm realm = page.getRealm();
        proto.defineAccessor(name, realm.newFunction("get " + name, 0, new Member(get)), realm.newFunction("set " + name, 1, new Member(set)), true, true);
    }

    /**
     * Defines a method.
     * <p>
     * 메서드를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the length / 길이
     * @param op     the operation / 연산
     */
    private void method(JsObject proto, String name, int length, int op) {
        proto.defineBuiltin(name, page.getRealm().newFunction(name, length, new Member(op)));
    }

    /**
     * Creates the behaviour of an interface object called without <code>new</code>.
     * <p>
     * <code>new</code> 없이 부른 인터페이스 객체의 동작을 만든다.
     *
     * @param name the interface name / 인터페이스 이름
     * @return the behaviour / 동작
     */
    private static NativeFunction.Call notCallable(final String name) {
        return new NativeFunction.Call() {
            /**
             * Throws the TypeError of a constructor called as a function.
             * <p>
             * 함수로 부른 생성자의 TypeError 를 던진다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct '" + name + "': Please use the 'new' operator, this DOM object constructor cannot be called "
                        + "as a function.");
            }
        };
    }

    /**
     * Installs the interfaces, the members of <code>Element</code> and <code>Document</code>, and the frame hook.
     * <p>
     * 인터페이스, <code>Element</code> 와 <code>Document</code> 의 멤버, 프레임 훅을 설치한다.
     */
    public void install() {
        final DomBridge bridge = page.getBridge();
        final JsRealm realm = page.getRealm();
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Chooses the interfaces of the model objects.
             * <p>
             * 모델 객체의 인터페이스를 고른다.
             *
             * @param o the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름 또는 null
             */
            public String classify(Object o) {
                if (o instanceof WebAnimation) {
                    return "Animation";
                }
                if (o instanceof KeyframeEffectModel) {
                    return "KeyframeEffect";
                }
                return o instanceof WebTimeline ? "DocumentTimeline" : null;
            }
        });
        installTimelines(bridge, realm);
        installEffects(bridge, realm);
        installAnimation(bridge, realm);
        installEvent(bridge, realm);
        JsObject element = bridge.getInterface("Element").getPrototype();
        method(element, "animate", 1, EL_ANIMATE);
        method(element, "getAnimations", 0, EL_ANIMATIONS);
        JsObject document = bridge.getInterface("Document").getPrototype();
        method(document, "getAnimations", 0, D_ANIMATIONS);
        getter(document, "timeline", D_TIMELINE);
        page.setFrameHook(this);
    }

    /**
     * Installs <code>AnimationTimeline</code> and <code>DocumentTimeline</code>.
     * <p>
     * <code>AnimationTimeline</code> 과 <code>DocumentTimeline</code> 을 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     */
    private void installTimelines(DomBridge bridge, JsRealm realm) {
        DomBridge.InterfaceInfo base = bridge.defineInterface("AnimationTimeline", null, WebTimeline.class, new String[0]);
        getter(base.getPrototype(), "currentTime", T_CURRENT);
        DomBridge.InterfaceInfo doc = bridge.defineInterface("DocumentTimeline", "AnimationTimeline", WebTimeline.class, new String[0]);
        timelineProto = doc.getPrototype();
        bridge.setConstructor("DocumentTimeline", new NativeFunction(realm, base.getConstructor(), "DocumentTimeline", 0, notCallable("DocumentTimeline"),
                new NativeFunction.Construct() {
            /**
             * Creates a document timeline of the page's document with an origin time (<code>new DocumentTimeline({originTime})</code>).
             * <p>
             * 원점 시간을 가진 페이지 문서의 문서 타임라인을 만든다(<code>new DocumentTimeline({originTime})</code>).
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the timeline / 타임라인
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue opts = Js.arg(args, 0);
                double origin = 0;
                if (opts.isObject()) {
                    JsValue o = ((JsObject) opts).get("originTime");
                    if (!o.isUndefined()) {
                        origin = KeyframeArguments.finite(o, "originTime", "Failed to construct 'DocumentTimeline': ");
                    }
                } else if (!opts.isUndefined() && !opts.isNull()) {
                    throw JsException.typeError("Failed to construct 'DocumentTimeline': The provided value is not of type 'DocumentTimelineOptions'.");
                }
                HostObject w = page.getBridge().wrap(new WebTimeline(set(), origin));
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, timelineProto));
                return w;
            }
        }));
    }

    /**
     * Installs <code>AnimationEffect</code> and <code>KeyframeEffect</code>.
     * <p>
     * <code>AnimationEffect</code> 와 <code>KeyframeEffect</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     */
    private void installEffects(DomBridge bridge, JsRealm realm) {
        DomBridge.InterfaceInfo base = bridge.defineInterface("AnimationEffect", null, KeyframeEffectModel.class, new String[0]);
        method(base.getPrototype(), "getTiming", 0, E_TIMING);
        method(base.getPrototype(), "getComputedTiming", 0, E_COMPUTED);
        method(base.getPrototype(), "updateTiming", 0, E_UPDATE);
        DomBridge.InterfaceInfo info = bridge.defineInterface("KeyframeEffect", "AnimationEffect", KeyframeEffectModel.class, new String[0]);
        effectProto = info.getPrototype();
        accessor(effectProto, "target", K_TARGET, K_TARGET_SET);
        accessor(effectProto, "pseudoElement", K_PSEUDO, K_PSEUDO_SET);
        accessor(effectProto, "composite", K_COMPOSITE, K_COMPOSITE_SET);
        accessor(effectProto, "iterationComposite", K_ITER, K_ITER_SET);
        method(effectProto, "getKeyframes", 0, K_GET_KEYFRAMES);
        method(effectProto, "setKeyframes", 1, K_SET_KEYFRAMES);
        bridge.setConstructor("KeyframeEffect", new NativeFunction(realm, base.getConstructor(), "KeyframeEffect", 1, notCallable("KeyframeEffect"),
                new NativeFunction.Construct() {
            /**
             * Creates a keyframe effect: <code>new KeyframeEffect(target, keyframes, options)</code> or <code>new KeyframeEffect(source)</code>.
             * <p>
             * 키프레임 효과를 만든다: <code>new KeyframeEffect(target, keyframes, options)</code> 또는 <code>new KeyframeEffect(source)</code>.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the effect / 효과
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                KeyframeEffectModel fx;
                try {
                    if (args.length == 1) {
                        // 인수가 하나이면 복사 생성자이다.
                        KeyframeEffectModel source = (KeyframeEffectModel) DomBridge.targetOf(args[0], KeyframeEffectModel.class);
                        if (source == null) {
                            throw JsException.typeError("Failed to construct 'KeyframeEffect': parameter 1 is not of type 'KeyframeEffect'.");
                        }
                        fx = KeyframeEffectModel.copyOf(source);
                    } else if (args.length == 0) {
                        throw JsException.typeError("Failed to construct 'KeyframeEffect': 2 arguments required, but only 0 present.");
                    } else {
                        fx = createEffect(targetArg(args[0], "Failed to construct 'KeyframeEffect': "), Js.arg(args, 1), Js.arg(args, 2), false,
                                "Failed to construct 'KeyframeEffect': ");
                    }
                } catch (AnimationException e) {
                    throw error(e);
                }
                HostObject w = page.getBridge().wrap(fx);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, effectProto));
                return w;
            }
        }));
    }

    /**
     * Installs <code>Animation</code>.
     * <p>
     * <code>Animation</code> 을 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     */
    private void installAnimation(DomBridge bridge, JsRealm realm) {
        DomBridge.InterfaceInfo info = bridge.defineInterface("Animation", "EventTarget", WebAnimation.class, new String[0]);
        animationProto = info.getPrototype();
        bridge.getEventHandlers().define(animationProto, WebAnimation.class, new String[] { "finish", "cancel", "remove" });
        accessor(animationProto, "id", A_ID, A_ID_SET);
        accessor(animationProto, "effect", A_EFFECT, A_EFFECT_SET);
        accessor(animationProto, "timeline", A_TIMELINE, A_TIMELINE_SET);
        accessor(animationProto, "startTime", A_START, A_START_SET);
        accessor(animationProto, "currentTime", A_CURRENT, A_CURRENT_SET);
        accessor(animationProto, "playbackRate", A_RATE, A_RATE_SET);
        getter(animationProto, "playState", A_PLAY_STATE);
        getter(animationProto, "replaceState", A_REPLACE_STATE);
        getter(animationProto, "pending", A_PENDING);
        getter(animationProto, "ready", A_READY);
        getter(animationProto, "finished", A_FINISHED);
        method(animationProto, "cancel", 0, A_CANCEL);
        method(animationProto, "finish", 0, A_FINISH);
        method(animationProto, "play", 0, A_PLAY);
        method(animationProto, "pause", 0, A_PAUSE);
        method(animationProto, "updatePlaybackRate", 1, A_UPDATE_RATE);
        method(animationProto, "reverse", 0, A_REVERSE);
        method(animationProto, "persist", 0, A_PERSIST);
        method(animationProto, "commitStyles", 0, A_COMMIT);
        bridge.setConstructor("Animation", new NativeFunction(realm, bridge.getInterface("EventTarget").getConstructor(), "Animation", 0,
                notCallable("Animation"), new NativeFunction.Construct() {
            /**
             * Creates an animation (<code>new Animation(effect, timeline)</code>); a missing timeline is the default document timeline.
             * <p>
             * 애니메이션을 만든다(<code>new Animation(effect, timeline)</code>). 타임라인이 없으면 기본 문서 타임라인이다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the animation / 애니메이션
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                String context = "Failed to construct 'Animation': ";
                KeyframeEffectModel fx = effectArg(Js.arg(args, 0), context);
                JsValue tl = Js.arg(args, 1);
                WebTimeline timeline = tl.isUndefined() ? set().getTimeline() : timelineArg(tl, context);
                WebAnimation a = new WebAnimation(set());
                a.setTimeline(timeline);
                a.setEffect(fx);
                HostObject w = page.getBridge().wrap(a);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, animationProto));
                return w;
            }
        }));
    }

    /**
     * Installs <code>AnimationPlaybackEvent</code>.
     * <p>
     * <code>AnimationPlaybackEvent</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     * @param realm  the realm / realm
     */
    private void installEvent(DomBridge bridge, JsRealm realm) {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("AnimationPlaybackEvent", "Event", AnimationPlaybackEvent.class,
                new String[] { "a:currentTime", "a:timelineTime" });
        bridge.setConstructor("AnimationPlaybackEvent", new NativeFunction(realm, bridge.getInterface("Event").getConstructor(), "AnimationPlaybackEvent", 1,
                notCallable("AnimationPlaybackEvent"), new NativeFunction.Construct() {
            /**
             * Creates an event from an <code>AnimationPlaybackEventInit</code> dictionary.
             * <p>
             * <code>AnimationPlaybackEventInit</code> 사전으로 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the event / 이벤트
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 1) {
                    throw JsException.typeError("Failed to construct 'AnimationPlaybackEvent': 1 argument required, but only 0 present.");
                }
                String type = Ops.toString(args[0]);
                JsValue init = Js.arg(args, 1);
                boolean bubbles = false;
                boolean cancelable = false;
                double current = Double.NaN;
                double timeline = Double.NaN;
                // 초기화 사전의 멤버를 읽는다(null 시간은 NaN).
                if (init.isObject()) {
                    JsObject o = (JsObject) init;
                    bubbles = Ops.toBoolean(o.get("bubbles"));
                    cancelable = Ops.toBoolean(o.get("cancelable"));
                    JsValue c = o.get("currentTime");
                    if (!c.isUndefined() && !c.isNull()) {
                        current = KeyframeArguments.finite(c, "currentTime", "Failed to construct 'AnimationPlaybackEvent': ");
                    }
                    JsValue t = o.get("timelineTime");
                    if (!t.isUndefined() && !t.isNull()) {
                        timeline = KeyframeArguments.finite(t, "timelineTime", "Failed to construct 'AnimationPlaybackEvent': ");
                    }
                }
                AnimationPlaybackEvent e = new AnimationPlaybackEvent(type, bubbles, cancelable, current, timeline);
                HostObject w = page.getBridge().wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    // ------------------------------------------------------------------------------------------------ argument helpers

    /**
     * Reads a nullable element argument.
     * <p>
     * null 일 수 있는 요소 인수를 읽는다.
     *
     * @param v       the value / 값
     * @param context the prefix of the message / 메시지 앞부분
     * @return the element or null / 요소 또는 null
     */
    private static Element targetArg(JsValue v, String context) {
        if (v.isNull() || v.isUndefined()) {
            return null;
        }
        Element e = (Element) DomBridge.targetOf(v, Element.class);
        if (e == null) {
            throw JsException.typeError(context + "The provided value is not of type 'Element'.");
        }
        return e;
    }

    /**
     * Reads a nullable effect argument.
     * <p>
     * null 일 수 있는 효과 인수를 읽는다.
     *
     * @param v       the value / 값
     * @param context the prefix of the message / 메시지 앞부분
     * @return the effect or null / 효과 또는 null
     */
    private static KeyframeEffectModel effectArg(JsValue v, String context) {
        if (v.isNull() || v.isUndefined()) {
            return null;
        }
        KeyframeEffectModel e = (KeyframeEffectModel) DomBridge.targetOf(v, KeyframeEffectModel.class);
        if (e == null) {
            throw JsException.typeError(context + "The provided value is not of type 'AnimationEffect'.");
        }
        return e;
    }

    /**
     * Reads a nullable timeline argument.
     * <p>
     * null 일 수 있는 타임라인 인수를 읽는다.
     *
     * @param v       the value / 값
     * @param context the prefix of the message / 메시지 앞부분
     * @return the timeline or null / 타임라인 또는 null
     */
    private static WebTimeline timelineArg(JsValue v, String context) {
        if (v.isNull() || v.isUndefined()) {
            return null;
        }
        WebTimeline t = (WebTimeline) DomBridge.targetOf(v, WebTimeline.class);
        if (t == null) {
            throw JsException.typeError(context + "The provided value is not of type 'AnimationTimeline'.");
        }
        return t;
    }

    /**
     * Reads a nullable time (the WebIDL type <code>double?</code>).
     * <p>
     * null 일 수 있는 시간(WebIDL 형 <code>double?</code>)을 읽는다.
     *
     * @param v       the value / 값
     * @param name    the attribute name / 속성 이름
     * @param context the prefix of the message / 메시지 앞부분
     * @return the time, NaN for null / 시간. null 이면 NaN
     */
    private static double nullableTime(JsValue v, String name, String context) {
        if (v.isNull() || v.isUndefined()) {
            return Double.NaN;
        }
        return KeyframeArguments.finite(v, name, context);
    }

    /**
     * Creates a keyframe effect (the procedure of the <code>KeyframeEffect(target, keyframes, options)</code> constructor): the options are read
     * first, then the pseudo-element, the timing and the composite operation are set, and finally the keyframes are processed.
     * <p>
     * 키프레임 효과를 만든다(<code>KeyframeEffect(target, keyframes, options)</code> 생성자의 절차): 옵션을 먼저 읽고, 의사 요소, 시간, 합성 연산을 정한 뒤
     * 마지막으로 키프레임을 처리한다.
     *
     * @param target    the target or null / 대상 또는 null
     * @param keyframes the keyframes argument / 키프레임 인수
     * @param options   the options argument / 옵션 인수
     * @param animate   true when called by <code>animate()</code> (reads the members of <code>KeyframeAnimationOptions</code>) / <code>animate()</code> 가 부르면 true
     * @param context   the prefix of the messages / 메시지 앞부분
     * @return the effect / 효과
     */
    private KeyframeEffectModel createEffect(Element target, JsValue keyframes, JsValue options, boolean animate, String context) {
        KeyframeArguments.TimingInput in = KeyframeArguments.readOptions(options, true, animate, context);
        return createEffect(target, keyframes, in, context);
    }

    /**
     * Creates a keyframe effect from options that were read already.
     * <p>
     * 이미 읽은 옵션으로 키프레임 효과를 만든다.
     *
     * @param target    the target or null / 대상 또는 null
     * @param keyframes the keyframes argument / 키프레임 인수
     * @param in        the options / 옵션
     * @param context   the prefix of the messages / 메시지 앞부분
     * @return the effect / 효과
     */
    private KeyframeEffectModel createEffect(Element target, JsValue keyframes, KeyframeArguments.TimingInput in, String context) {
        String pseudo = null;
        if (in.pseudoPresent && in.pseudoElement != null) {
            pseudo = KeyframeEffectModel.normalizePseudo(in.pseudoElement);
            if (pseudo == null) {
                throw syntaxError(in.pseudoElement);
            }
        }
        in.validate(context);
        EffectTiming timing = new EffectTiming();
        in.applyTo(timing);
        KeyframeEffectModel fx = new KeyframeEffectModel(timing);
        if (in.composite != null) {
            fx.setComposite(in.composite);
        }
        if (in.iterationComposite != null) {
            fx.setIterationComposite(in.iterationComposite);
        }
        fx.setKeyframes(KeyframeArguments.process(page.getRealm(), keyframes, context));
        fx.setTarget(target, pseudo);
        return fx;
    }

    /**
     * Creates the SyntaxError of an invalid pseudo-element selector.
     * <p>
     * 올바르지 않은 의사 요소 선택자의 SyntaxError 를 만든다.
     *
     * @param text the selector / 선택자
     * @return the exception / 예외
     */
    private JsException syntaxError(String text) {
        return new JsException(DomExceptionObjects.create(page.getBridge(), "SyntaxError", "'" + text + "' is not a valid pseudo-element selector."));
    }

    /**
     * Returns the animation of a receiver or throws the TypeError of an illegal invocation.
     * <p>
     * 수신자의 애니메이션을 반환하거나 잘못된 호출의 TypeError 를 던진다.
     *
     * @param self the receiver / 수신자
     * @return the animation / 애니메이션
     */
    private static WebAnimation animation(JsValue self) {
        WebAnimation a = (WebAnimation) DomBridge.targetOf(self, WebAnimation.class);
        if (a == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return a;
    }

    /**
     * Returns the effect of a receiver or throws.
     * <p>
     * 수신자의 효과를 반환하거나 던진다.
     *
     * @param self the receiver / 수신자
     * @return the effect / 효과
     */
    private static KeyframeEffectModel effect(JsValue self) {
        KeyframeEffectModel e = (KeyframeEffectModel) DomBridge.targetOf(self, KeyframeEffectModel.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Converts a list of animations to a script array.
     * <p>
     * 애니메이션 목록을 스크립트 배열로 바꾼다.
     *
     * @param list the animations / 애니메이션
     * @return the array / 배열
     */
    private JsValue array(List<WebAnimation> list) {
        JsValue[] items = new JsValue[list.size()];
        // 애니메이션마다 래퍼를 넣는다.
        for (int i = 0; i < items.length; i++) {
            items[i] = page.getBridge().toJs(list.get(i));
        }
        return page.getRealm().newArray(items);
    }

    // ------------------------------------------------------------------------------------------------ members

    /**
     * Runs a member.
     * <p>
     * 멤버를 실행한다.
     *
     * @param op   the operation code / 연산 번호
     * @param self the receiver / 수신자
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    private JsValue dispatch(int op, JsValue self, JsValue[] args) {
        if (op < E_TIMING) {
            return animationMember(op, animation(self), args);
        }
        if (op < T_CURRENT) {
            return effectMember(op, effect(self), args);
        }
        if (op == T_CURRENT) {
            WebTimeline t = (WebTimeline) DomBridge.targetOf(self, WebTimeline.class);
            if (t == null) {
                throw JsException.typeError("Illegal invocation");
            }
            return KeyframeArguments.time(t.getCurrentTime());
        }
        if (op == EL_ANIMATE || op == EL_ANIMATIONS) {
            Element e = (Element) DomBridge.targetOf(self, Element.class);
            if (e == null) {
                throw JsException.typeError("Illegal invocation");
            }
            return op == EL_ANIMATE ? animate(e, args) : elementAnimations(e, Js.arg(args, 0));
        }
        Document d = (Document) DomBridge.targetOf(self, Document.class);
        if (d == null) {
            throw JsException.typeError("Illegal invocation");
        }
        if (op == D_TIMELINE) {
            return page.getBridge().toJs(timelineOf(d));
        }
        return array(d == page.getDocument() ? set().getAnimations(d, true) : new java.util.ArrayList<WebAnimation>());
    }

    /**
     * Runs a member of <code>Animation</code>.
     * <p>
     * <code>Animation</code> 의 멤버를 실행한다.
     *
     * @param op   the operation code / 연산 번호
     * @param a    the animation / 애니메이션
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    private JsValue animationMember(int op, WebAnimation a, JsValue[] args) {
        String context = "Failed to set a property on 'Animation': ";
        JsValue v = Js.arg(args, 0);
        switch (op) {
            case A_ID:
                return Js.str(a.getId());
            case A_ID_SET:
                a.setId(Ops.toString(v));
                return Js.UNDEFINED;
            case A_EFFECT:
                return a.getEffect() == null ? Js.NULL : page.getBridge().toJs(a.getEffect());
            case A_EFFECT_SET:
                a.setEffect(effectArg(v, context));
                return Js.UNDEFINED;
            case A_TIMELINE:
                return a.getTimeline() == null ? Js.NULL : page.getBridge().toJs(a.getTimeline());
            case A_TIMELINE_SET:
                a.setTimeline(timelineArg(v, context));
                return Js.UNDEFINED;
            case A_START:
                return KeyframeArguments.time(a.getStartTime());
            case A_START_SET:
                a.setStartTime(nullableTime(v, "startTime", context));
                return Js.UNDEFINED;
            case A_CURRENT:
                return KeyframeArguments.time(a.getCurrentTime());
            case A_CURRENT_SET:
                a.setCurrentTime(nullableTime(v, "currentTime", context));
                return Js.UNDEFINED;
            case A_RATE:
                return Js.num(a.getPlaybackRate());
            case A_RATE_SET:
                a.setPlaybackRate(KeyframeArguments.finite(v, "playbackRate", context));
                return Js.UNDEFINED;
            case A_PLAY_STATE:
                return Js.str(a.getPlayState());
            case A_REPLACE_STATE:
                return Js.str(a.getReplaceState());
            case A_PENDING:
                return Js.bool(a.isPending());
            case A_READY:
                return promiseOf(a.getReady());
            case A_FINISHED:
                return promiseOf(a.getFinished());
            case A_CANCEL:
                a.cancel();
                return Js.UNDEFINED;
            case A_FINISH:
                a.finish();
                return Js.UNDEFINED;
            case A_PLAY:
                a.play();
                return Js.UNDEFINED;
            case A_PAUSE:
                a.pause();
                return Js.UNDEFINED;
            case A_UPDATE_RATE:
                a.updatePlaybackRate(KeyframeArguments.finite(v, "playbackRate", "Failed to execute 'updatePlaybackRate' on 'Animation': "));
                return Js.UNDEFINED;
            case A_REVERSE:
                a.reverse();
                return Js.UNDEFINED;
            case A_PERSIST:
                a.persist();
                return Js.UNDEFINED;
            default:
                a.getSet().commitStyles(a);
                return Js.UNDEFINED;
        }
    }

    /**
     * Returns the script promise of a promise slot.
     * <p>
     * 프라미스 자리의 스크립트 프라미스를 반환한다.
     *
     * @param p the slot / 자리
     * @return the promise / 프라미스
     */
    private static JsValue promiseOf(PromiseSlot p) {
        return p instanceof Slot ? ((Slot) p).promise : Js.UNDEFINED;
    }

    /**
     * Runs a member of <code>AnimationEffect</code> or <code>KeyframeEffect</code>.
     * <p>
     * <code>AnimationEffect</code> 나 <code>KeyframeEffect</code> 의 멤버를 실행한다.
     *
     * @param op   the operation code / 연산 번호
     * @param e    the effect / 효과
     * @param args the arguments / 인수
     * @return the result / 결과
     */
    private JsValue effectMember(int op, KeyframeEffectModel e, JsValue[] args) {
        JsRealm realm = page.getRealm();
        JsValue v = Js.arg(args, 0);
        switch (op) {
            case E_TIMING:
                return KeyframeArguments.timingToJs(realm, e.getTiming());
            case E_COMPUTED: {
                WebAnimation a = e.getAnimation();
                EffectTiming.Computed c = e.getTiming().compute(a == null ? Double.NaN : a.getCurrentTime(), a != null && a.getPlaybackRate() < 0, true);
                return KeyframeArguments.computedTimingToJs(realm, e.getTiming(), c);
            }
            case E_UPDATE: {
                String context = "Failed to execute 'updateTiming' on 'AnimationEffect': ";
                KeyframeArguments.TimingInput in = new KeyframeArguments.TimingInput();
                if (v.isObject()) {
                    KeyframeArguments.readTiming((JsObject) v, in, context);
                } else if (!v.isUndefined() && !v.isNull()) {
                    throw JsException.typeError(context + "The provided value is not of type 'OptionalEffectTiming'.");
                }
                in.validate(context);
                in.applyTo(e.getTiming());
                e.changed();
                return Js.UNDEFINED;
            }
            case K_TARGET:
                return e.getTarget() == null ? Js.NULL : page.getBridge().toJs(e.getTarget());
            case K_TARGET_SET:
                e.setTarget(targetArg(v, "Failed to set the 'target' property on 'KeyframeEffect': "), e.getPseudoElement());
                return Js.UNDEFINED;
            case K_PSEUDO:
                return e.getPseudoElement() == null ? Js.NULL : Js.str(e.getPseudoElement());
            case K_PSEUDO_SET: {
                if (v.isNull() || v.isUndefined()) {
                    e.setTarget(e.getTarget(), null);
                    return Js.UNDEFINED;
                }
                String text = Ops.toString(v);
                String p = KeyframeEffectModel.normalizePseudo(text);
                // 올바르지 않은 선택자는 SyntaxError 이며 대상을 바꾸지 않는다.
                if (p == null) {
                    throw syntaxError(text);
                }
                e.setTarget(e.getTarget(), p);
                return Js.UNDEFINED;
            }
            case K_COMPOSITE:
                return Js.str(e.getComposite());
            case K_COMPOSITE_SET: {
                String c = KeyframeArguments.enumOrNull(v, new String[] { "replace", "add", "accumulate" });
                if (c != null) {
                    e.setComposite(c);
                }
                return Js.UNDEFINED;
            }
            case K_ITER:
                return Js.str(e.getIterationComposite());
            case K_ITER_SET: {
                String c = KeyframeArguments.enumOrNull(v, new String[] { "replace", "accumulate" });
                if (c != null) {
                    e.setIterationComposite(c);
                }
                return Js.UNDEFINED;
            }
            case K_GET_KEYFRAMES:
                return KeyframeArguments.keyframesToJs(realm, e.getKeyframes());
            default:
                e.setKeyframes(KeyframeArguments.process(realm, v, "Failed to execute 'setKeyframes' on 'KeyframeEffect': "));
                return Js.UNDEFINED;
        }
    }

    /**
     * Implements <code>Element.animate(keyframes, options)</code> (6.8): a keyframe effect on the element, an animation on the timeline of the options
     * (or the default timeline of the element's document) with the id of the options, played with auto-rewind.
     * <p>
     * <code>Element.animate(keyframes, options)</code> 을 구현한다(6.8): 요소에 대한 키프레임 효과, 옵션의 타임라인(또는 요소 문서의 기본 타임라인)에 대한
     * 애니메이션과 옵션의 id 를 만들어 자동 되감기로 재생한다.
     *
     * @param e    the element / 요소
     * @param args the arguments / 인수
     * @return the animation / 애니메이션
     */
    private JsValue animate(Element e, JsValue[] args) {
        String context = "Failed to execute 'animate' on 'Element': ";
        if (args.length < 1) {
            throw JsException.typeError(context + "1 argument required, but only 0 present.");
        }
        KeyframeArguments.TimingInput in = KeyframeArguments.readOptions(Js.arg(args, 1), true, true, context);
        KeyframeEffectModel fx = createEffect(e, args[0], in, context);
        WebTimeline timeline = in.timelinePresent ? timelineArg(in.timeline, context) : timelineOf(e.getOwnerDocument());
        WebAnimation a = new WebAnimation(set());
        a.setTimeline(timeline);
        a.setEffect(fx);
        if (in.id != null) {
            a.setId(in.id);
        }
        a.play();
        return page.getBridge().toJs(a);
    }

    /**
     * Implements <code>Element.getAnimations(options)</code>: the relevant animations of the element, or of its subtree with <code>{subtree: true}</code>.
     * <p>
     * <code>Element.getAnimations(options)</code> 을 구현한다: 요소의 관련 애니메이션, <code>{subtree: true}</code> 이면 서브트리의 것.
     *
     * @param e       the element / 요소
     * @param options the options / 옵션
     * @return the array / 배열
     */
    private JsValue elementAnimations(Element e, JsValue options) {
        boolean subtree = false;
        if (options.isObject()) {
            subtree = Ops.toBoolean(((JsObject) options).get("subtree"));
        } else if (!options.isUndefined() && !options.isNull()) {
            throw JsException.typeError("Failed to execute 'getAnimations' on 'Element': The provided value is not of type 'GetAnimationsOptions'.");
        }
        if (e.getOwnerDocument() != page.getDocument()) {
            return page.getRealm().newArray(new JsValue[0]);
        }
        return array(set().getAnimations(e, subtree));
    }
}
