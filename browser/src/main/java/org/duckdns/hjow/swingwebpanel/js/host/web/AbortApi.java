package org.duckdns.hjow.swingwebpanel.js.host.web;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.DomExceptionObjects;
import org.duckdns.hjow.swingwebpanel.js.host.Scheduler;

/**
 * Installs <code>AbortController</code> and <code>AbortSignal</code> (DOM Standard, section 3): the controller object that scripts hold,
 * the signal interface (an event target with <code>aborted</code>, <code>reason</code>, <code>throwIfAborted()</code>, <code>onabort</code>), and the
 * static <code>AbortSignal.abort()</code>, <code>AbortSignal.timeout()</code> and <code>AbortSignal.any()</code>. Dependent signals follow their
 * source signals; <code>timeout()</code> uses the page's scheduler, so it stops when the page is replaced.
 * <p>
 * <code>AbortController</code> 와 <code>AbortSignal</code> 을 설치한다(DOM 표준 3절): 스크립트가 쥐는 컨트롤러 객체, 신호 인터페이스(<code>aborted</code>,
 * <code>reason</code>, <code>throwIfAborted()</code>, <code>onabort</code> 를 가진 이벤트 대상), 정적 <code>AbortSignal.abort()</code>,
 * <code>AbortSignal.timeout()</code>, <code>AbortSignal.any()</code>. 종속 신호는 원본 신호를 따르고 <code>timeout()</code> 은 페이지의 스케줄러를 쓰므로
 * 페이지가 교체되면 멈춘다.
 */
final class AbortApi {

    /**
     * The script object of an <code>AbortController</code>.
     * <p>
     * <code>AbortController</code> 의 스크립트 객체.
     */
    private static final class ControllerObject extends JsObject {
        /** The signal of the controller. / 컨트롤러의 신호. */
        final AbortSignal signal;

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto  the prototype / 프로토타입
         * @param signal the signal / 신호
         */
        ControllerObject(JsObject proto, AbortSignal signal) {
            super(proto);
            this.signal = signal;
        }
    }

    /** The page APIs. / 페이지 API. */
    private final WebApis api;

    /** The prototype of controllers. / 컨트롤러의 프로토타입. */
    private JsObject controllerProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the page APIs / 페이지 API
     */
    AbortApi(WebApis api) {
        this.api = api;
    }

    /**
     * Installs both interfaces.
     * <p>
     * 두 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = api.bridge();
        final JsRealm realm = api.realm();
        bridge.addClassifier(new DomBridge.Classifier() {
            public String classify(Object target) {
                return target instanceof AbortSignal ? "AbortSignal" : null;
            }
        });
        DomBridge.InterfaceInfo sig = bridge.defineInterface("AbortSignal", "EventTarget", AbortSignal.class,
                new String[] { "a:aborted", "a:reason", "m:throwIfAborted()" });
        bridge.getEventHandlers().define(sig.getPrototype(), AbortSignal.class, new String[] { "abort" });
        Natives.method(realm, sig.getConstructor(), "abort", 0, this, "staticAbort");
        Natives.method(realm, sig.getConstructor(), "timeout", 1, this, "staticTimeout");
        Natives.method(realm, sig.getConstructor(), "any", 1, this, "staticAny");
        Natives.ClassDef c = Natives.defineClass(realm, g, "AbortController", 0, null, new NativeFunction.Construct() {
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                return new ControllerObject(Ops.getPrototypeFromConstructor(r, newTarget, controllerProto), newSignal());
            }
        });
        controllerProto = c.prototype;
        Natives.getter(realm, c.prototype, "signal", this, "controllerSignal");
        Natives.method(realm, c.prototype, "abort", 0, this, "controllerAbort");
    }

    /**
     * Creates a signal that is not aborted.
     * <p>
     * 중단되지 않은 신호를 만든다.
     *
     * @return the signal / 신호
     */
    AbortSignal newSignal() {
        return new AbortSignal(api);
    }

    /**
     * Creates the default abort reason: an <code>AbortError</code> DOMException.
     * <p>
     * 기본 중단 이유를 만든다: <code>AbortError</code> DOMException.
     *
     * @return the reason / 이유
     */
    JsValue defaultReason() {
        return DomExceptionObjects.create(api.bridge(), "AbortError", "signal is aborted without reason");
    }

    /**
     * Creates a signal that follows others: it is aborted when the first of the sources aborts, or at once if one already is.
     * <p>
     * 다른 신호를 따르는 신호를 만든다. 원본 중 처음으로 중단되는 것을 따라 중단되며 이미 중단된 것이 있으면 곧바로 중단된다.
     *
     * @param sources the source signals (not null items) / 원본 신호(null 항목 없음)
     * @return the dependent signal / 종속 신호
     */
    AbortSignal dependent(List<AbortSignal> sources) {
        final AbortSignal result = newSignal();
        // 이미 중단된 원본이 있으면 그 이유로 곧바로 중단한다.
        for (int i = 0; i < sources.size(); i++) {
            if (sources.get(i).isAborted()) {
                result.signalAbort(sources.get(i).getReason());
                return result;
            }
        }
        // 아직 중단되지 않은 원본마다 중단되면 결과도 같은 이유로 중단하도록 등록한다.
        for (int i = 0; i < sources.size(); i++) {
            final AbortSignal source = sources.get(i);
            source.addAlgorithm(new Runnable() {
                public void run() {
                    result.signalAbort(source.getReason());
                }
            });
        }
        return result;
    }

    /**
     * Returns <code>controller.signal</code>.
     * <p>
     * <code>controller.signal</code> 을 반환한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the signal / 신호
     */
    JsValue controllerSignal(JsValue self, JsValue[] args) {
        return api.bridge().toJs(Natives.self(self, ControllerObject.class).signal);
    }

    /**
     * Implements <code>controller.abort(reason)</code>.
     * <p>
     * <code>controller.abort(reason)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return undefined / undefined
     */
    JsValue controllerAbort(JsValue self, JsValue[] args) {
        Natives.self(self, ControllerObject.class).signal.signalAbort(Js.arg(args, 0));
        return Js.UNDEFINED;
    }

    /**
     * Implements <code>AbortSignal.abort(reason)</code>: a signal that is already aborted.
     * <p>
     * <code>AbortSignal.abort(reason)</code> 를 구현한다: 이미 중단된 신호.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the signal / 신호
     */
    JsValue staticAbort(JsValue self, JsValue[] args) {
        AbortSignal s = newSignal();
        s.signalAbort(Js.arg(args, 0));
        return api.bridge().toJs(s);
    }

    /**
     * Implements <code>AbortSignal.timeout(ms)</code>: a signal that aborts with a <code>TimeoutError</code> after the time.
     * <p>
     * <code>AbortSignal.timeout(ms)</code> 를 구현한다: 시간이 지나면 <code>TimeoutError</code> 로 중단되는 신호.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the signal / 신호
     */
    JsValue staticTimeout(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "timeout", "AbortSignal");
        double ms = Ops.toNumber(args[0]);
        if (Double.isNaN(ms) || ms < 0 || ms > 18446744073709551615.0) {
            throw JsException.typeError("Failed to execute 'timeout' on 'AbortSignal': Value is outside the 'unsigned long long' value range.");
        }
        final AbortSignal s = newSignal();
        api.postLater(new Runnable() {
            public void run() {
                s.signalAbort(DomExceptionObjects.create(api.bridge(), "TimeoutError", "signal timed out"));
            }
        }, (long) Math.min(ms, 2147483647.0));
        return api.bridge().toJs(s);
    }

    /**
     * Implements <code>AbortSignal.any(signals)</code>.
     * <p>
     * <code>AbortSignal.any(signals)</code> 를 구현한다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the dependent signal / 종속 신호
     */
    JsValue staticAny(JsValue self, JsValue[] args) {
        Natives.requireArgs(args, 1, "any", "AbortSignal");
        List<JsValue> items = IteratorHelper.toList(api.realm(), args[0]);
        List<AbortSignal> sources = new ArrayList<AbortSignal>();
        // 반복 가능한 인수의 각 항목은 AbortSignal 이어야 한다.
        for (int i = 0; i < items.size(); i++) {
            Object t = DomBridge.targetOf(items.get(i), AbortSignal.class);
            if (t == null) {
                throw JsException.typeError("Failed to execute 'any' on 'AbortSignal': Failed to convert value to 'AbortSignal'.");
            }
            sources.add((AbortSignal) t);
        }
        return api.bridge().toJs(dependent(sources));
    }

    /**
     * Tells which signal object a script value is.
     * <p>
     * 스크립트 값이 어느 신호 객체인지 알려 준다.
     *
     * @param v the value / 값
     * @return the signal or null / 신호 또는 null
     */
    static AbortSignal signalOf(JsValue v) {
        Object t = DomBridge.targetOf(v, AbortSignal.class);
        return t == null ? null : (AbortSignal) t;
    }

    /**
     * Cancels a scheduled task if there is one (a null-safe helper for timers).
     * <p>
     * 예약된 작업이 있으면 취소한다(타이머용 null 안전 도우미).
     *
     * @param h the handle or null / 핸들 또는 null
     */
    static void cancel(Scheduler.Handle h) {
        if (h != null) {
            h.cancel();
        }
    }
}
