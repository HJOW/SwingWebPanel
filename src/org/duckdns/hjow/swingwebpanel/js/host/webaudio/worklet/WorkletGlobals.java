package org.duckdns.hjow.swingwebpanel.js.host.webaudio.worklet;

import java.util.ArrayList;
import java.util.List;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.IteratorHelper;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrayObject;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioWorkletNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AutomationRate;

/**
 * Installs what <code>AudioWorkletGlobalScope</code> has (Web Audio API 1.32.2) on the global object of the realm of a {@link WorkletScope}: <code>registerProcessor()</code>,
 * the attributes <code>currentFrame</code>, <code>currentTime</code> and <code>sampleRate</code>, and the <code>AudioWorkletProcessor</code> constructor with the
 * <code>port</code> attribute of its prototype. Everything else of the global object comes from the engine: the built-in objects of ECMAScript 2015, the <code>console</code> that
 * reaches the console of the page, and the channel messaging (<code>MessagePort</code>, <code>MessageEvent</code>). There is no window, no document, no timer and no network API.
 * <p>
 * <code>registerProcessor()</code> follows 1.32.2.2 step by step and checks the conversion of <code>parameterDescriptors</code> to a sequence of
 * <code>AudioParamDescriptor</code> (the member <code>name</code> is required, the values are floats, <code>automationRate</code> is <code>a-rate</code> or <code>k-rate</code>), duplicate names
 * (<code>NotSupportedError</code>) and <code>minValue &lt;= defaultValue &lt;= maxValue</code> (<code>InvalidStateError</code>).
 * <p>
 * {@link WorkletScope} 의 realm 의 전역 객체에 <code>AudioWorkletGlobalScope</code> 가 가진 것(Web Audio API 1.32.2)을 설치한다: <code>registerProcessor()</code>, 속성
 * <code>currentFrame</code>, <code>currentTime</code>, <code>sampleRate</code>, 그리고 <code>AudioWorkletProcessor</code> 생성자와 그 프로토타입의 <code>port</code> 속성. 전역 객체의 나머지는
 * 엔진에서 온다: ECMAScript 2015 의 내장 객체, 페이지의 콘솔에 닿는 <code>console</code>, 채널 메시징(<code>MessagePort</code>, <code>MessageEvent</code>). window, document, 타이머, 네트워크 API 는
 * 없다.
 * <p>
 * <code>registerProcessor()</code> 는 1.32.2.2 를 단계별로 따르며, <code>parameterDescriptors</code> 를 <code>AudioParamDescriptor</code> 시퀀스로 변환하는 것(<code>name</code> 멤버는 필수이고, 값은
 * float 이며, <code>automationRate</code> 는 <code>a-rate</code> 나 <code>k-rate</code>), 이름 중복(<code>NotSupportedError</code>), <code>minValue &lt;= defaultValue &lt;= maxValue</code>
 * (<code>InvalidStateError</code>)를 검사한다.
 */
final class WorkletGlobals {

    /**
     * Private constructor: static helpers only.
     * <p>
     * 비공개 생성자: 정적 도우미만 있다.
     */
    private WorkletGlobals() {
    }

    /**
     * Installs the members of the global scope; call it with the lock of the realm held, once, while the realm is built.
     * <p>
     * 전역 범위의 멤버를 설치한다. realm 을 만드는 동안 realm 의 잠금을 쥐고 한 번 부른다.
     *
     * @param scope the scope that owns the realm / realm 을 소유한 스코프
     */
    static void install(final WorkletScope scope) {
        final JsRealm realm = scope.realm();
        final JsObject global = realm.getGlobalObject();
        installProcessor(scope, realm, global);
        realm.method(global, "registerProcessor", 2, new NativeFunction.Call() {
            /**
             * Registers a processor class under a name.
             * <p>
             * 프로세서 클래스를 이름으로 등록한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments: the name and the constructor / 인수: 이름과 생성자
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                register(scope, args);
                return Js.UNDEFINED;
            }
        });
        global.defineAccessor("currentFrame", realm.newFunction("get currentFrame", 0, new NativeFunction.Call() {
            /**
             * Returns the frame of the block that is processed.
             * <p>
             * 처리 중인 블록의 프레임을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the frame / 프레임
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num((double) scope.engine().getCurrentFrame());
            }
        }), null, true, true);
        global.defineAccessor("currentTime", realm.newFunction("get currentTime", 0, new NativeFunction.Call() {
            /**
             * Returns the context time of the block that is processed.
             * <p>
             * 처리 중인 블록의 컨텍스트 시간을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time in seconds / 시간(초)
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(scope.engine().getCurrentTime());
            }
        }), null, true, true);
        global.defineAccessor("sampleRate", realm.newFunction("get sampleRate", 0, new NativeFunction.Call() {
            /**
             * Returns the sample rate of the context.
             * <p>
             * 컨텍스트의 샘플링 주파수를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the sample rate / 샘플링 주파수
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(scope.engine().getSampleRate());
            }
        }), null, true, true);
    }

    /**
     * Installs <code>AudioWorkletProcessor</code>: the constructor that a subclass reaches with <code>super()</code>, and the <code>port</code> attribute.
     * <p>
     * <code>AudioWorkletProcessor</code> 를 설치한다: 하위 클래스가 <code>super()</code> 로 닿는 생성자와 <code>port</code> 속성.
     *
     * @param scope  the scope / 스코프
     * @param realm  the realm / realm
     * @param global the global object / 전역 객체
     */
    private static void installProcessor(final WorkletScope scope, JsRealm realm, JsObject global) {
        final JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, realm.getFunctionPrototype(), "AudioWorkletProcessor", 0, new NativeFunction.Call() {
            /**
             * The interface object cannot be called without <code>new</code>.
             * <p>
             * 인터페이스 객체는 <code>new</code> 없이 호출할 수 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'AudioWorkletProcessor': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates the instance: only while a node is being constructed (the pending processor construction data of 1.32.2.3), with the port that the node made.
             * <p>
             * 인스턴스를 만든다: 노드를 만드는 중에만(1.32.2.3 의 pending processor construction data) 노드가 만든 포트와 함께.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the instance / 인스턴스
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsObject port = scope.takePendingPort();
                if (port == null) {
                    throw JsException.typeError("Failed to construct 'AudioWorkletProcessor': Illegal constructor");
                }
                return new ProcessorObject(Ops.getPrototypeFromConstructor(r, newTarget, proto), port);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineData(WellKnownSymbols.TO_STRING_TAG, Js.str("AudioWorkletProcessor"), false, false, true);
        proto.defineAccessor("port", realm.newFunction("get port", 0, new NativeFunction.Call() {
            /**
             * Returns the port of the processor.
             * <p>
             * 프로세서의 포트를 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the port / 포트
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                if (!(self instanceof ProcessorObject)) {
                    throw JsException.typeError("Illegal invocation");
                }
                return ((ProcessorObject) self).getPort();
            }
        }), null, true, true);
        global.defineBuiltin("AudioWorkletProcessor", ctor);
    }

    /**
     * The steps of <code>registerProcessor(name, processorCtor)</code> (1.32.2.2).
     * <p>
     * <code>registerProcessor(name, processorCtor)</code> 의 단계(1.32.2.2).
     *
     * @param scope the scope / 스코프
     * @param args  the arguments / 인수
     */
    private static void register(WorkletScope scope, JsValue[] args) {
        if (args.length < 2) {
            throw JsException.typeError("Failed to execute 'registerProcessor' on 'AudioWorkletGlobalScope': 2 arguments required, but only " + args.length + " present.");
        }
        String name = Ops.toString(args[0]);
        if (name.length() == 0) {
            throw scope.domError("NotSupportedError", "The processor name is empty.");
        }
        if (scope.isRegistered(name)) {
            throw scope.domError("NotSupportedError", "A processor with name '" + name + "' is already registered.");
        }
        if (!(args[1] instanceof JsObject) || !((JsObject) args[1]).isConstructor()) {
            throw JsException.typeError("Failed to execute 'registerProcessor' on 'AudioWorkletGlobalScope': The provided callback is not a constructor.");
        }
        JsObject ctor = (JsObject) args[1];
        if (!(ctor.get("prototype") instanceof JsObject)) {
            throw JsException.typeError("Failed to execute 'registerProcessor' on 'AudioWorkletGlobalScope': The 'prototype' property of the constructor is not an object.");
        }
        if (scope.registeredCount() >= WorkletLimits.MAX_PROCESSORS) {
            throw scope.domError("NotSupportedError", "Too many processors are registered (" + WorkletLimits.MAX_PROCESSORS + ").");
        }
        JsValue descriptors = ctor.get("parameterDescriptors");
        List<AudioWorkletNode.ParamDescriptor> list = descriptors.isUndefined() ? new ArrayList<AudioWorkletNode.ParamDescriptor>() : descriptors(scope, descriptors);
        scope.addRegistration(name, ctor, list);
    }

    /**
     * Converts the value of <code>parameterDescriptors</code> to the descriptors of the parameters.
     * <p>
     * <code>parameterDescriptors</code> 의 값을 파라미터 설명으로 변환한다.
     *
     * @param scope the scope / 스코프
     * @param value the value (an iterable of dictionaries) / 값(사전의 이터러블)
     * @return the descriptors, in order / 설명(순서대로)
     */
    private static List<AudioWorkletNode.ParamDescriptor> descriptors(WorkletScope scope, JsValue value) {
        JsRealm realm = scope.realm();
        if (!(value instanceof JsObject)) {
            throw JsException.typeError("The 'parameterDescriptors' property of the constructor is not a sequence.");
        }
        JsObject o = (JsObject) value;
        JsValue[] items;
        // 배열과 형식화 배열은 길이로 읽고, 그 밖의 객체는 반복자로 읽는다.
        if (Ops.isArray(o) || o instanceof TypedArrayObject) {
            items = Ops.createListFromArrayLike(o);
        } else {
            JsValue method = Ops.getMethod(o, WellKnownSymbols.ITERATOR);
            if (method == null || method.isNullOrUndefined()) {
                throw JsException.typeError("The 'parameterDescriptors' property of the constructor is not iterable.");
            }
            List<JsValue> all = IteratorHelper.toList(realm, o);
            items = all.toArray(new JsValue[all.size()]);
        }
        List<AudioWorkletNode.ParamDescriptor> out = new ArrayList<AudioWorkletNode.ParamDescriptor>();
        // 항목마다 사전으로 변환하고 이름이 겹치는지 확인한다.
        for (int i = 0; i < items.length; i++) {
            AudioWorkletNode.ParamDescriptor d = descriptor(scope, items[i]);
            // 이미 만든 설명과 이름이 같은지 확인한다.
            for (int j = 0; j < out.size(); j++) {
                if (out.get(j).getName().equals(d.getName())) {
                    throw scope.domError("NotSupportedError", "Two parameters have the same name '" + d.getName() + "'.");
                }
            }
            out.add(d);
        }
        return out;
    }

    /**
     * Converts one dictionary to an <code>AudioParamDescriptor</code>: the members are read in alphabetical order, as WebIDL says.
     * <p>
     * 사전 하나를 <code>AudioParamDescriptor</code> 로 변환한다: 멤버는 WebIDL 대로 알파벳 순으로 읽는다.
     *
     * @param scope the scope / 스코프
     * @param item  the dictionary / 사전
     * @return the descriptor / 설명
     */
    private static AudioWorkletNode.ParamDescriptor descriptor(WorkletScope scope, JsValue item) {
        if (!(item instanceof JsObject)) {
            throw JsException.typeError("The provided value is not of type 'AudioParamDescriptor'.");
        }
        JsObject d = (JsObject) item;
        JsValue rateValue = d.get("automationRate");
        AutomationRate rate = AutomationRate.A_RATE;
        // automationRate 는 a-rate 나 k-rate 이어야 한다.
        if (!rateValue.isUndefined()) {
            AutomationRate parsed = AutomationRate.fromScriptName(Ops.toString(rateValue));
            if (parsed == null) {
                throw JsException.typeError("The provided value '" + Ops.toString(rateValue) + "' is not a valid enum value of type AutomationRate.");
            }
            rate = parsed;
        }
        float defaultValue = floatMember(d, "defaultValue", 0f);
        float maxValue = floatMember(d, "maxValue", Float.MAX_VALUE);
        float minValue = floatMember(d, "minValue", -Float.MAX_VALUE);
        JsValue nameValue = d.get("name");
        if (nameValue.isUndefined()) {
            throw JsException.typeError("Failed to read the 'name' property from 'AudioParamDescriptor': Required member is undefined.");
        }
        String name = Ops.toString(nameValue);
        try {
            return new AudioWorkletNode.ParamDescriptor(name, defaultValue, minValue, maxValue, rate);
        } catch (AudioException e) {
            throw scope.domError(e.getKind().getExceptionName(), e.getMessage());
        }
    }

    /**
     * Reads a <code>float</code> member of a dictionary: the default for an undefined member, a <code>TypeError</code> for a value that is not finite or does not fit a float.
     * <p>
     * 사전의 <code>float</code> 멤버를 읽는다: undefined 이면 기본 값, 유한하지 않거나 float 에 맞지 않으면 <code>TypeError</code>.
     *
     * @param d        the dictionary / 사전
     * @param member   the name of the member / 멤버 이름
     * @param fallback the default / 기본 값
     * @return the value / 값
     */
    private static float floatMember(JsObject d, String member, float fallback) {
        JsValue v = d.get(member);
        if (v.isUndefined()) {
            return fallback;
        }
        double x = Ops.toNumber(v);
        float f = (float) x;
        if (Double.isNaN(x) || Double.isInfinite(x) || Float.isInfinite(f)) {
            throw JsException.typeError("The provided float value for '" + member + "' is non-finite or out of range.");
        }
        return f;
    }
}
