package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioListener;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioParam;
import org.duckdns.hjow.swingwebpanel.media.webaudio.PeriodicWave;

/**
 * The script interfaces of the objects that are not nodes and not contexts: <code>AudioParam</code> (Web Audio API 1.6: <code>value</code>, <code>automationRate</code>,
 * the limits, and the seven operations of the automation timeline), <code>AudioListener</code> (1.11: the position and the orientation of the listener) and
 * <code>PeriodicWave</code> (1.26: a wave of Fourier coefficients, with its constructor). They are the engine objects themselves, wrapped by the DOM bridge, so the same
 * parameter is always the same script object while the page holds it; their members are installed by {@link Members} from the Java API of the engine. A script cannot construct an
 * <code>AudioParam</code> or an <code>AudioListener</code> (they belong to a node or a context).
 * <p>
 * 노드도 컨텍스트도 아닌 객체의 스크립트 인터페이스: <code>AudioParam</code>(Web Audio API 1.6: <code>value</code>, <code>automationRate</code>, 한계, 자동화 타임라인의 연산 일곱),
 * <code>AudioListener</code>(1.11: 청취자의 위치와 방향), <code>PeriodicWave</code>(1.26: 푸리에 계수의 파형. 생성자 포함). 엔진 객체 자체를 DOM 브리지가 감싼 것이므로 같은 파라미터는
 * 페이지가 붙잡고 있는 동안 언제나 같은 스크립트 객체이다. 멤버는 {@link Members} 가 엔진의 Java API 에서 설치한다. 스크립트는 <code>AudioParam</code> 과
 * <code>AudioListener</code> 를 생성할 수 없다(노드나 컨텍스트에 속한다).
 */
final class ValueApi {

    /**
     * The owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * The member installer.
     * <p>
     * 멤버 설치기.
     */
    private final Members members;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api     the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param members the member installer / 멤버 설치기
     */
    ValueApi(WebAudioApi api, Members members) {
        this.api = api;
        this.members = members;
    }

    /**
     * Installs the interfaces.
     * <p>
     * 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        DomBridge bridge = api.bridge();
        DomBridge.InterfaceInfo param = bridge.defineInterface("AudioParam", null, AudioParam.class, new String[0]);
        members.attributes(param.getPrototype(), AudioParam.class, "AudioParam", "a:value=", "a:automationRate=", "a:defaultValue", "a:minValue", "a:maxValue");
        members.methods(param.getPrototype(), AudioParam.class, "AudioParam", "setValueAtTime", "linearRampToValueAtTime", "exponentialRampToValueAtTime",
                "setTargetAtTime", "setValueCurveAtTime", "cancelScheduledValues", "cancelAndHoldAtTime");
        DomBridge.InterfaceInfo listener = bridge.defineInterface("AudioListener", null, AudioListener.class, new String[0]);
        members.attributes(listener.getPrototype(), AudioListener.class, "AudioListener", "a:positionX", "a:positionY", "a:positionZ", "a:forwardX", "a:forwardY",
                "a:forwardZ", "a:upX", "a:upY", "a:upZ");
        members.methods(listener.getPrototype(), AudioListener.class, "AudioListener", "setPosition", "setOrientation");
        installPeriodicWave(bridge);
    }

    /**
     * Installs <code>PeriodicWave</code> with its constructor <code>new PeriodicWave(context, {real, imag, disableNormalization})</code>.
     * <p>
     * <code>PeriodicWave</code> 를 생성자 <code>new PeriodicWave(context, {real, imag, disableNormalization})</code> 와 함께 설치한다.
     *
     * @param bridge the DOM bridge / DOM 브리지
     */
    private void installPeriodicWave(final DomBridge bridge) {
        final JsRealm realm = api.realm();
        final DomBridge.InterfaceInfo info = bridge.defineInterface("PeriodicWave", null, PeriodicWave.class, new String[0]);
        bridge.setConstructor("PeriodicWave", new NativeFunction(realm, realm.getFunctionPrototype(), "PeriodicWave", 1, new NativeFunction.Call() {
            /**
             * Calling the constructor without <code>new</code> is an error.
             * <p>
             * <code>new</code> 없이 생성자를 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'PeriodicWave': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a periodic wave from the options.
             * <p>
             * 옵션으로 주기파를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the context and the options / 인수: 컨텍스트와 옵션
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Idl.requireCtorArgs(args, 1, "PeriodicWave");
                ContextValue c = ContextApi.contextOf(args[0], "PeriodicWave");
                JsObject dict = Idl.dictionary(Js.arg(args, 1), "Failed to construct 'PeriodicWave': The provided value");
                PeriodicWave wave = makeWave(api, c, Idl.member(dict, "real"), Idl.member(dict, "imag"), Idl.member(dict, "disableNormalization"));
                HostObject w = bridge.wrap(wave);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Creates a periodic wave in a context from the coefficients that a script gave (the constructor and <code>createPeriodicWave</code> share this).
     * <p>
     * 스크립트가 준 계수로 컨텍스트에 주기파를 만든다(생성자와 <code>createPeriodicWave</code> 가 같이 쓴다).
     *
     * @param api     the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param c       the context / 컨텍스트
     * @param real    the cosine terms, undefined for none / 코사인 항. 없으면 undefined
     * @param imag    the sine terms, undefined for none / 사인 항. 없으면 undefined
     * @param disable the <code>disableNormalization</code> value, undefined for false / <code>disableNormalization</code> 값. 없으면 undefined(false)
     * @return the wave / 파형
     */
    static PeriodicWave makeWave(WebAudioApi api, ContextValue c, JsValue real, JsValue imag, JsValue disable) {
        float[] r = real.isUndefined() ? null : Idl.floatSequence(api.realm(), real, "The provided value for 'real'");
        float[] i = imag.isUndefined() ? null : Idl.floatSequence(api.realm(), imag, "The provided value for 'imag'");
        try {
            return c.engine().createPeriodicWave(r, i, Ops.toBoolean(disable));
        } catch (org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException e) {
            throw api.error(e);
        }
    }
}
