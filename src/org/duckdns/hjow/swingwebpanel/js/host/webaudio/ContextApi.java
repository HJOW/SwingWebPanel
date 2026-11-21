package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import java.util.concurrent.Executor;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ArrayBufferObject;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.TypedArrays;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioBuffer;
import org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException;
import org.duckdns.hjow.swingwebpanel.media.webaudio.BaseAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OfflineAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.OutputRenderDriver;
import org.duckdns.hjow.swingwebpanel.media.webaudio.PeriodicWave;
import org.duckdns.hjow.swingwebpanel.media.webaudio.RealtimeAudioContext;
import org.duckdns.hjow.swingwebpanel.media.webaudio.ScriptProcessorNode;
import org.duckdns.hjow.swingwebpanel.media.webaudio.WebAudioLimits;
import org.duckdns.hjow.swingwebpanel.media.webaudio.decode.DecodeWarningCallback;
import org.duckdns.hjow.swingwebpanel.media.webaudio.output.OutputFactory;

/**
 * The script interfaces of the contexts (Web Audio API 1.1 to 1.3): <code>BaseAudioContext</code> (the clock, the destination, the listener, the state, the factories of all nodes, buffers
 * and waves, and <code>decodeAudioData</code>), <code>AudioContext</code> (the context of a page that plays: <code>resume</code>, <code>suspend</code>, <code>close</code>, the latencies, the output
 * timestamp and the sink) and <code>OfflineAudioContext</code> (<code>startRendering</code>, <code>suspend(time)</code>, <code>resume</code>, <code>length</code>, <code>oncomplete</code>), with the events
 * <code>statechange</code>, <code>sinkchange</code> and <code>complete</code> and the interface <code>OfflineAudioCompletionEvent</code>.
 * <p>
 * <b>State.</b> The <code>state</code> attribute is the <em>visible</em> state of the {@link ContextValue}. A control call (<code>resume</code>, <code>suspend</code>, <code>close</code>) asks the engine at once and
 * queues a task of the page; in that task the visible state is set to what the engine is in at that moment, the <code>statechange</code> event is fired when it changed, and the promise is
 * resolved, as the specification describes ("queue a media element task"). A new <code>AudioContext</code> is <code>suspended</code> until its first task has run and <code>running</code> afterwards
 * (there is no autoplay restriction: the decision (e) of the plan, because the component is a Swing application whose host can decide for itself), whether or not a sound device exists; time
 * advances with the wall clock (see {@link ClockRenderDriver}). An offline context is <code>suspended</code>, becomes <code>running</code> when its rendering starts, <code>suspended</code>
 * at a suspension and <code>closed</code> when the rendering is complete.
 * <p>
 * <b>Not supported here.</b> <code>createMediaStreamSource</code>, <code>createMediaStreamTrackSource</code> and <code>createMediaStreamDestination</code> throw <code>NotSupportedError</code> (the project has no media streams
 * or capture devices, the explicit exception EXC-14); <code>createMediaElementSource</code> and <code>createScriptProcessor</code> are added by the output stage and throw <code>NotSupportedError</code>
 * until then; the output device of a context is the default one (<code>sinkId</code> is the empty string; <code>setSinkId</code> accepts the empty string and <code>{type: "none"}</code> and refuses any other
 * device with <code>NotSupportedError</code>).
 * <p>
 * 컨텍스트의 스크립트 인터페이스(Web Audio API 1.1~1.3): <code>BaseAudioContext</code>(시계, 목적지, 청취자, 상태, 모든 노드와 버퍼와 파형의 팩터리, <code>decodeAudioData</code>),
 * <code>AudioContext</code>(소리를 내는 페이지의 컨텍스트: <code>resume</code>, <code>suspend</code>, <code>close</code>, 지연, 출력 타임스탬프, 싱크), <code>OfflineAudioContext</code>
 * (<code>startRendering</code>, <code>suspend(time)</code>, <code>resume</code>, <code>length</code>, <code>oncomplete</code>)와 이벤트 <code>statechange</code>, <code>sinkchange</code>, <code>complete</code>,
 * 인터페이스 <code>OfflineAudioCompletionEvent</code>.
 * <p>
 * <b>상태.</b> <code>state</code> 속성은 {@link ContextValue} 의 <em>보이는</em> 상태이다. 제어 호출(<code>resume</code>, <code>suspend</code>, <code>close</code>)은 엔진에 즉시 요청하고 페이지의
 * 작업을 쌓는다. 그 작업에서 보이는 상태를 그 순간 엔진이 있는 상태로 정하고, 바뀌었으면 <code>statechange</code> 이벤트를 발생시키고, 프라미스를 resolve 한다(규격이 말하는
 * "queue a media element task"). 새 <code>AudioContext</code> 는 첫 작업이 실행될 때까지 <code>suspended</code> 이고 그 뒤에는 <code>running</code> 이다(자동 재생 제한은 없다: 계획의 결정 e.
 * 컴포넌트는 호스트가 스스로 정할 수 있는 Swing 응용이기 때문이다). 소리 장치가 있든 없든 같다. 시간은 벽시계와 함께 흐른다({@link ClockRenderDriver} 참고). 오프라인 컨텍스트는
 * <code>suspended</code> 이고, 렌더링이 시작되면 <code>running</code>, 일시 중단에서 <code>suspended</code>, 렌더링이 끝나면 <code>closed</code> 이다.
 * <p>
 * <b>여기서 지원하지 않는 것.</b> <code>createMediaStreamSource</code>, <code>createMediaStreamTrackSource</code>, <code>createMediaStreamDestination</code> 은 <code>NotSupportedError</code> 를 던진다(프로젝트에는 미디어
 * 스트림과 캡처 장치가 없다. 명시적 예외 EXC-14). <code>createMediaElementSource</code> 와 <code>createScriptProcessor</code> 는 출력 단계가 더하며 그때까지 <code>NotSupportedError</code> 를 던진다.
 * 컨텍스트의 출력 장치는 기본 장치이다(<code>sinkId</code> 는 빈 문자열. <code>setSinkId</code> 는 빈 문자열과 <code>{type: "none"}</code> 을 받고 다른 장치는 <code>NotSupportedError</code> 로 거부한다).
 */
final class ContextApi {

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
     * The nodes.
     * <p>
     * 노드.
     */
    private final NodeApi nodes;

    /**
     * The DOM bridge.
     * <p>
     * DOM 브리지.
     */
    private final DomBridge bridge;

    /**
     * The realm.
     * <p>
     * The script realm.
     * <p>
     * 스크립트 realm.
     */
    private final JsRealm realm;

    /**
     * The prototype of <code>OfflineAudioCompletionEvent</code>.
     * <p>
     * <code>OfflineAudioCompletionEvent</code> 의 프로토타입.
     */
    private JsObject completionProto;

    /**
     * The prototype of <code>AudioProcessingEvent</code>.
     * <p>
     * <code>AudioProcessingEvent</code> 의 프로토타입.
     */
    private JsObject processingProto;

    /**
     * The prototypes of the two kinds of contexts.
     * <p>
     * 두 종류 컨텍스트의 프로토타입.
     */
    private JsObject realtimeProto;

    /**
     * The prototype of <code>OfflineAudioContext</code>.
     * <p>
     * <code>OfflineAudioContext</code> 의 프로토타입.
     */
    private JsObject offlineProto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api     the owner of the script interface / 스크립트 인터페이스의 소유자
     * @param members the member installer / 멤버 설치기
     * @param nodes   the nodes / 노드
     */
    ContextApi(WebAudioApi api, Members members, NodeApi nodes) {
        this.api = api;
        this.members = members;
        this.nodes = nodes;
        this.bridge = api.bridge();
        this.realm = api.realm();
    }

    /**
     * Names the interface of a context value.
     * <p>
     * 컨텍스트 값의 인터페이스 이름을 정한다.
     *
     * @param target the Java object / Java 객체
     * @return the interface name, or null / 인터페이스 이름. 아니면 null
     */
    String classify(Object target) {
        if (target instanceof ContextValue) {
            return ((ContextValue) target).isOffline() ? "OfflineAudioContext" : "AudioContext";
        }
        return null;
    }

    /**
     * Returns the context value of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 컨텍스트 값을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the value / 값
     */
    static ContextValue contextOf(JsValue self) {
        ContextValue c = (ContextValue) DomBridge.targetOf(self, ContextValue.class);
        if (c == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return c;
    }

    /**
     * Returns the context value of the first argument of a constructor or throws a TypeError that names the parameter.
     * <p>
     * 생성자의 첫 인수의 컨텍스트 값을 반환하거나 매개변수를 이름 붙인 TypeError 를 던진다.
     *
     * @param v    the argument / 인수
     * @param ctor the interface name / 인터페이스 이름
     * @return the value / 값
     */
    static ContextValue contextOf(JsValue v, String ctor) {
        ContextValue c = (ContextValue) DomBridge.targetOf(v, ContextValue.class);
        if (c == null) {
            throw JsException.typeError("Failed to construct '" + ctor + "': parameter 1 is not of type 'BaseAudioContext'.");
        }
        return c;
    }

    /**
     * Creates the <code>complete</code> event of a rendering with the prototype of its interface.
     * <p>
     * 렌더링의 <code>complete</code> 이벤트를 인터페이스의 프로토타입과 함께 만든다.
     *
     * @param rendered the rendered buffer / 렌더링한 버퍼
     * @return the event / 이벤트
     */
    OfflineAudioCompletionEvent completionEvent(AudioBufferValue rendered) {
        OfflineAudioCompletionEvent e = new OfflineAudioCompletionEvent("complete", false, false, rendered);
        api.adopt(e, completionProto);
        return e;
    }

    /**
     * Defines a promise-returning operation: a failure of the arguments, of the receiver or of the engine is a rejected promise, not an exception, as WebIDL says.
     * <p>
     * 프라미스를 반환하는 연산을 정의한다: 인수, 수신자, 엔진의 실패는 WebIDL 대로 예외가 아니라 reject 된 프라미스이다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param body   the operation, which returns the promise / 프라미스를 반환하는 연산
     */
    private void promiseMethod(JsObject proto, String name, int length, final WebAudioApi.Op body) {
        api.method(proto, name, length, new WebAudioApi.Op() {
            /**
             * Runs the operation and turns an exception into a rejected promise.
             * <p>
             * 연산을 실행하고 예외를 reject 된 프라미스로 바꾼다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                try {
                    return body.call(self, args);
                } catch (JsException e) {
                    return Promises.rejected(realm, e.getValue(realm));
                } catch (AudioException e) {
                    return Promises.rejected(realm, api.errorValue(e));
                }
            }
        });
    }

    /**
     * Installs the interfaces of the contexts.
     * <p>
     * 컨텍스트의 인터페이스를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        new BufferApi(api).install();
        installEvents();
        DomBridge.InterfaceInfo base = bridge.defineInterface("BaseAudioContext", "EventTarget", ContextValue.class, new String[0]);
        installBase(base.getPrototype());
        DomBridge.InterfaceInfo realtime = bridge.defineInterface("AudioContext", "BaseAudioContext", ContextValue.class, new String[0]);
        realtimeProto = realtime.getPrototype();
        installRealtime(realtimeProto);
        DomBridge.InterfaceInfo offline = bridge.defineInterface("OfflineAudioContext", "BaseAudioContext", ContextValue.class, new String[0]);
        offlineProto = offline.getPrototype();
        installOffline(offlineProto);
        // 이전 이름(webkitAudioContext)은 같은 생성자를 가리킨다(Chromium 과 Safari 가 가졌던 접두사 이름).
        g.defineData("webkitAudioContext", g.get("AudioContext"), true, false, true);
        g.defineData("webkitOfflineAudioContext", g.get("OfflineAudioContext"), true, false, true);
    }

    /**
     * Installs the event interfaces <code>OfflineAudioCompletionEvent</code> and <code>AudioProcessingEvent</code> with their constructors.
     * <p>
     * 이벤트 인터페이스 <code>OfflineAudioCompletionEvent</code> 와 <code>AudioProcessingEvent</code> 를 생성자와 함께 설치한다.
     */
    private void installEvents() {
        DomBridge.InterfaceInfo completion = bridge.defineInterface("OfflineAudioCompletionEvent", "Event", OfflineAudioCompletionEvent.class, new String[0]);
        completionProto = completion.getPrototype();
        api.accessor(completionProto, "renderedBuffer", new WebAudioApi.Op() {
            /**
             * Returns the rendered buffer.
             * <p>
             * 렌더링한 버퍼를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the buffer / 버퍼
             */
            public JsValue call(JsValue self, JsValue[] args) {
                OfflineAudioCompletionEvent e = (OfflineAudioCompletionEvent) DomBridge.targetOf(self, OfflineAudioCompletionEvent.class);
                if (e == null) {
                    throw JsException.typeError("Illegal invocation");
                }
                return bridge.toJs(e.getRenderedBuffer());
            }
        }, null);
        bridge.setConstructor("OfflineAudioCompletionEvent", new NativeFunction(realm, (JsObject) realm.getGlobalObject().get("Event"), "OfflineAudioCompletionEvent", 2,
                new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'OfflineAudioCompletionEvent': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an event from a type and the init dictionary with the required <code>renderedBuffer</code>.
             * <p>
             * 종류와 필수 <code>renderedBuffer</code> 가 있는 초기화 사전으로 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Idl.requireCtorArgs(args, 2, "OfflineAudioCompletionEvent");
                JsObject dict = Idl.dictionary(args[1], "Failed to construct 'OfflineAudioCompletionEvent': The provided value");
                JsValue buffer = Idl.member(dict, "renderedBuffer");
                if (buffer.isUndefined()) {
                    throw JsException.typeError("Failed to construct 'OfflineAudioCompletionEvent': required member renderedBuffer is undefined.");
                }
                AudioBufferValue b = BufferApi.bufferArg(buffer, "Failed to construct 'OfflineAudioCompletionEvent': The provided value for 'renderedBuffer'");
                OfflineAudioCompletionEvent e = new OfflineAudioCompletionEvent(Ops.toString(args[0]), Ops.toBoolean(Idl.member(dict, "bubbles")),
                        Ops.toBoolean(Idl.member(dict, "cancelable")), b);
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, completionProto));
                return w;
            }
        }));
        DomBridge.InterfaceInfo processing = bridge.defineInterface("AudioProcessingEvent", "Event", AudioProcessingEvent.class, new String[0]);
        processingProto = processing.getPrototype();
        api.accessor(processingProto, "playbackTime", new WebAudioApi.Op() {
            /**
             * Returns the playback time.
             * <p>
             * 재생 시간을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time / 시간
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(processing(self).getPlaybackTime());
            }
        }, null);
        api.accessor(processingProto, "inputBuffer", new WebAudioApi.Op() {
            /**
             * Returns the input buffer.
             * <p>
             * 입력 버퍼를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the buffer / 버퍼
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(processing(self).getInputBuffer());
            }
        }, null);
        api.accessor(processingProto, "outputBuffer", new WebAudioApi.Op() {
            /**
             * Returns the output buffer.
             * <p>
             * 출력 버퍼를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the buffer / 버퍼
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(processing(self).getOutputBuffer());
            }
        }, null);
        bridge.setConstructor("AudioProcessingEvent", new NativeFunction(realm, (JsObject) realm.getGlobalObject().get("Event"), "AudioProcessingEvent", 2,
                new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'AudioProcessingEvent': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an event from a type and the init dictionary with the required <code>playbackTime</code>, <code>inputBuffer</code> and <code>outputBuffer</code>.
             * <p>
             * 종류와 필수 <code>playbackTime</code>, <code>inputBuffer</code>, <code>outputBuffer</code> 가 있는 초기화 사전으로 이벤트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Idl.requireCtorArgs(args, 2, "AudioProcessingEvent");
                JsObject dict = Idl.dictionary(args[1], "Failed to construct 'AudioProcessingEvent': The provided value");
                String[] required = {"playbackTime", "inputBuffer", "outputBuffer"};
                // 필수 멤버가 모두 있는지 확인한다.
                for (int i = 0; i < required.length; i++) {
                    if (Idl.member(dict, required[i]).isUndefined()) {
                        throw JsException.typeError("Failed to construct 'AudioProcessingEvent': required member " + required[i] + " is undefined.");
                    }
                }
                AudioProcessingEvent e = new AudioProcessingEvent(Ops.toString(args[0]), Ops.toBoolean(Idl.member(dict, "bubbles")),
                        Ops.toBoolean(Idl.member(dict, "cancelable")), Idl.dbl(Idl.member(dict, "playbackTime")),
                        BufferApi.bufferArg(Idl.member(dict, "inputBuffer"), "Failed to construct 'AudioProcessingEvent': The provided value for 'inputBuffer'"),
                        BufferApi.bufferArg(Idl.member(dict, "outputBuffer"), "Failed to construct 'AudioProcessingEvent': The provided value for 'outputBuffer'"));
                HostObject w = bridge.wrap(e);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, processingProto));
                return w;
            }
        }));
    }

    /**
     * Returns the processing event of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 처리 이벤트를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the event / 이벤트
     */
    private static AudioProcessingEvent processing(JsValue self) {
        AudioProcessingEvent e = (AudioProcessingEvent) DomBridge.targetOf(self, AudioProcessingEvent.class);
        if (e == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Creates a node for a factory: the options dictionary is built from the factory's arguments (none for most).
     * <p>
     * 팩터리를 위해 노드를 만든다: 옵션 사전은 팩터리의 인수로 만든다(대부분 없다).
     *
     * @param self    the receiver / 수신자
     * @param type    the interface name of the node / 노드의 인터페이스 이름
     * @param options the options, null for none / 옵션. 없으면 null
     * @return the node / 노드
     */
    private JsValue createNode(JsValue self, String type, JsObject options) {
        ContextValue c = contextOf(self);
        return bridge.toJs(nodes.create(c, type, options));
    }

    /**
     * Defines the factory of a node that takes no argument.
     * <p>
     * 인수가 없는 노드의 팩터리를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param method the name of the factory / 팩터리 이름
     * @param type   the interface name of the node / 노드의 인터페이스 이름
     */
    private void factory(JsObject proto, String method, final String type) {
        api.method(proto, method, 0, new WebAudioApi.Op() {
            /**
             * Creates the node.
             * <p>
             * 노드를 만든다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the node / 노드
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return createNode(self, type, null);
            }
        });
    }

    /**
     * Defines the factory of a node that takes one optional argument, which becomes a member of the options.
     * <p>
     * 선택 인수 하나를 받는 노드의 팩터리를 정의한다. 인수는 옵션의 멤버가 된다.
     *
     * @param proto   the prototype / 프로토타입
     * @param method  the name of the factory / 팩터리 이름
     * @param type    the interface name of the node / 노드의 인터페이스 이름
     * @param member  the member of the options that the argument becomes / 인수가 되는 옵션 멤버
     */
    private void factoryWithArgument(JsObject proto, String method, final String type, final String member) {
        api.method(proto, method, 0, new WebAudioApi.Op() {
            /**
             * Creates the node.
             * <p>
             * 노드를 만든다.
             *
             * @param self this value / this 값
             * @param args arguments: the optional argument / 인수: 선택 인수
             * @return the node / 노드
             */
            public JsValue call(JsValue self, JsValue[] args) {
                JsObject options = null;
                if (args.length > 0 && !args[0].isUndefined()) {
                    options = realm.newObject();
                    options.defineData(member, args[0], true, true, true);
                }
                return createNode(self, type, options);
            }
        });
    }

    /**
     * Installs <code>BaseAudioContext</code>.
     * <p>
     * <code>BaseAudioContext</code> 를 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installBase(JsObject proto) {
        bridge.getEventHandlers().define(proto, ContextValue.class, new String[] {"statechange"});
        api.accessor(proto, "destination", new WebAudioApi.Op() {
            /**
             * Returns the destination node.
             * <p>
             * 목적지 노드를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the destination / 목적지
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(contextOf(self).destination);
            }
        }, null);
        api.accessor(proto, "sampleRate", new WebAudioApi.Op() {
            /**
             * Returns the sample rate.
             * <p>
             * 샘플링 주파수를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the sample rate / 샘플링 주파수
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(contextOf(self).engine().getSampleRate());
            }
        }, null);
        api.accessor(proto, "currentTime", new WebAudioApi.Op() {
            /**
             * Returns the time of the context.
             * <p>
             * 컨텍스트의 시간을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the time in seconds / 시간(초)
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(contextOf(self).engine().getCurrentTime());
            }
        }, null);
        api.accessor(proto, "listener", new WebAudioApi.Op() {
            /**
             * Returns the listener.
             * <p>
             * 청취자를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the listener / 청취자
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return bridge.toJs(contextOf(self).engine().getListener());
            }
        }, null);
        api.accessor(proto, "state", new WebAudioApi.Op() {
            /**
             * Returns the state.
             * <p>
             * 상태를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.str(contextOf(self).getState());
            }
        }, null);
        api.accessor(proto, "renderQuantumSize", new WebAudioApi.Op() {
            /**
             * Returns the number of frames of a render quantum.
             * <p>
             * render quantum 의 프레임 수를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the number of frames / 프레임 수
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(contextOf(self).engine().getRenderQuantumSize());
            }
        }, null);
        factory(proto, "createAnalyser", "AnalyserNode");
        factory(proto, "createBiquadFilter", "BiquadFilterNode");
        factory(proto, "createBufferSource", "AudioBufferSourceNode");
        factory(proto, "createConstantSource", "ConstantSourceNode");
        factory(proto, "createConvolver", "ConvolverNode");
        factory(proto, "createDynamicsCompressor", "DynamicsCompressorNode");
        factory(proto, "createGain", "GainNode");
        factory(proto, "createOscillator", "OscillatorNode");
        factory(proto, "createPanner", "PannerNode");
        factory(proto, "createStereoPanner", "StereoPannerNode");
        factory(proto, "createWaveShaper", "WaveShaperNode");
        factoryWithArgument(proto, "createDelay", "DelayNode", "maxDelayTime");
        factoryWithArgument(proto, "createChannelSplitter", "ChannelSplitterNode", "numberOfOutputs");
        factoryWithArgument(proto, "createChannelMerger", "ChannelMergerNode", "numberOfInputs");
        api.method(proto, "createIIRFilter", 2, new WebAudioApi.Op() {
            /**
             * Creates an IIR filter from its coefficients.
             * <p>
             * 계수로 IIR 필터를 만든다.
             *
             * @param self this value / this 값
             * @param args arguments: the feedforward and the feedback coefficients / 인수: 분자와 분모 계수
             * @return the node / 노드
             */
            public JsValue call(JsValue self, JsValue[] args) {
                ContextValue c = contextOf(self);
                Idl.requireArgs(args, 2, "createIIRFilter", "BaseAudioContext");
                JsObject options = realm.newObject();
                options.defineData("feedforward", args[0], true, true, true);
                options.defineData("feedback", args[1], true, true, true);
                return bridge.toJs(nodes.create(c, "IIRFilterNode", options));
            }
        });
        api.method(proto, "createBuffer", 3, new WebAudioApi.Op() {
            /**
             * Creates a silent buffer.
             * <p>
             * 무음 버퍼를 만든다.
             *
             * @param self this value / this 값
             * @param args arguments: the number of channels, the length and the sample rate / 인수: 채널 수, 길이, 샘플링 주파수
             * @return the buffer / 버퍼
             */
            public JsValue call(JsValue self, JsValue[] args) {
                contextOf(self);
                Idl.requireArgs(args, 3, "createBuffer", "BaseAudioContext");
                return bridge.toJs(AudioBufferValue.create(Idl.ulong(args[0]), Idl.ulong(args[1]), Idl.flt(args[2])));
            }
        });
        api.method(proto, "createPeriodicWave", 2, new WebAudioApi.Op() {
            /**
             * Creates a periodic wave from Fourier coefficients.
             * <p>
             * 푸리에 계수로 주기파를 만든다.
             *
             * @param self this value / this 값
             * @param args arguments: the real and imaginary terms and the constraints / 인수: 실수항과 허수항, 제약
             * @return the wave / 파형
             */
            public JsValue call(JsValue self, JsValue[] args) {
                ContextValue c = contextOf(self);
                Idl.requireArgs(args, 2, "createPeriodicWave", "BaseAudioContext");
                JsObject constraints = Idl.dictionary(Js.arg(args, 2), "Failed to execute 'createPeriodicWave' on 'BaseAudioContext': The provided value");
                PeriodicWave w = ValueApi.makeWave(api, c, args[0], args[1], Idl.member(constraints, "disableNormalization"));
                return bridge.toJs(w);
            }
        });
        api.method(proto, "createScriptProcessor", 0, new WebAudioApi.Op() {
            /**
             * Creates a script processor (deprecated, 1.29): <code>createScriptProcessor(bufferSize = 0, numberOfInputChannels = 2, numberOfOutputChannels = 2)</code>.
             * <p>
             * 스크립트 프로세서를 만든다(폐기 예정, 1.29): <code>createScriptProcessor(bufferSize = 0, numberOfInputChannels = 2, numberOfOutputChannels = 2)</code>.
             *
             * @param self this value / this 값
             * @param args arguments: the buffer size and the numbers of channels / 인수: 버퍼 크기와 채널 수
             * @return the node / 노드
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return createScriptProcessor(contextOf(self), args);
            }
        });
        promiseMethod(proto, "decodeAudioData", 1, new WebAudioApi.Op() {
            /**
             * Decodes the audio file data of an <code>ArrayBuffer</code> in the background.
             * <p>
             * <code>ArrayBuffer</code> 의 오디오 파일 데이터를 백그라운드에서 디코딩한다.
             *
             * @param self this value / this 값
             * @param args arguments: the data, the success callback and the error callback / 인수: 데이터, 성공 콜백, 오류 콜백
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return decodeAudioData(self, args);
            }
        });
    }

    /**
     * Reads an optional callback argument: undefined and null mean none, a function is the callback, anything else is a TypeError.
     * <p>
     * 선택적 콜백 인수를 읽는다: undefined 와 null 은 없음, 함수는 콜백, 그 밖은 TypeError 이다.
     *
     * @param args  the arguments / 인수
     * @param index the position / 위치
     * @param name  the name for the message / 메시지에 쓸 이름
     * @return the function, or null for none / 함수. 없으면 null
     */
    private static JsValue callback(JsValue[] args, int index, String name) {
        if (index >= args.length || args[index].isNullOrUndefined()) {
            return null;
        }
        if (!args[index].isCallable()) {
            throw JsException.typeError("Failed to execute 'decodeAudioData' on 'BaseAudioContext': The " + name + " is not a function.");
        }
        return args[index];
    }

    /**
     * Calls a callback of a script with one argument inside a task of the page; an exception that it throws is reported to the page.
     * <p>
     * 페이지의 작업 안에서 스크립트의 콜백을 인수 하나로 부른다. 던진 예외는 페이지에 보고한다.
     *
     * @param fn  the callback, or null / 콜백. 없으면 null
     * @param arg the argument / 인수
     */
    private void callCallback(JsValue fn, JsValue arg) {
        if (fn == null) {
            return;
        }
        try {
            Ops.call(fn, Js.UNDEFINED, new JsValue[] {arg});
        } catch (JsException e) {
            bridge.reportError(e);
        }
    }

    /**
     * Implements <code>decodeAudioData(audioData, successCallback, errorCallback)</code> (1.1.2): the <code>ArrayBuffer</code> is detached and its bytes go to the decoding service of the
     * context, which decodes on another thread, resamples to the sample rate of the context and delivers the result as a task of the page, where the promise is resolved and the
     * success callback is called; a failure rejects the promise with an <code>EncodingError</code> and calls the error callback. A detached buffer is a <code>DataCloneError</code>.
     * <p>
     * <code>decodeAudioData(audioData, successCallback, errorCallback)</code> 를 구현한다(1.1.2): <code>ArrayBuffer</code> 는 분리되고 그 바이트는 컨텍스트의 디코딩 서비스로 간다.
     * 서비스는 다른 스레드에서 디코딩하고 컨텍스트의 샘플링 주파수로 리샘플링하며 결과를 페이지의 작업으로 전달한다. 그 작업에서 프라미스를 resolve 하고 성공 콜백을 부른다.
     * 실패하면 프라미스를 <code>EncodingError</code> 로 reject 하고 오류 콜백을 부른다. 분리된 버퍼는 <code>DataCloneError</code> 이다.
     *
     * @param self this value / this 값
     * @param args arguments / 인수
     * @return the promise / 프라미스
     */
    private JsValue decodeAudioData(JsValue self, JsValue[] args) {
        final ContextValue c = contextOf(self);
        Idl.requireArgs(args, 1, "decodeAudioData", "BaseAudioContext");
        if (!TypedArrays.isArrayBuffer(args[0])) {
            throw JsException.typeError("Failed to execute 'decodeAudioData' on 'BaseAudioContext': parameter 1 is not of type 'ArrayBuffer'.");
        }
        final JsValue success = callback(args, 1, "success callback");
        final JsValue failure = callback(args, 2, "error callback");
        ArrayBufferObject data = (ArrayBufferObject) args[0];
        if (api.isDisposed()) {
            throw api.domError("InvalidStateError", "The document is not fully active.");
        }
        final Promises.Deferred d = Promises.create(realm);
        // 닫힌 컨텍스트와 분리된 버퍼는 디코딩하지 않고 reject 한다(분리된 버퍼는 DataCloneError, 닫힌 컨텍스트는 InvalidStateError).
        if (c.closeRequested || c.getState().equals("closed")) {
            failDecode(d, failure, api.domError("InvalidStateError", "Failed to execute 'decodeAudioData' on 'BaseAudioContext': Cannot decode audio data on a closed context.").getValue(realm));
            return d.promise;
        }
        if (data.isDetached()) {
            failDecode(d, failure, api.domError("DataCloneError", "Failed to execute 'decodeAudioData' on 'BaseAudioContext': Cannot decode detached ArrayBuffer").getValue(realm));
            return d.promise;
        }
        byte[] bytes = data.getData();
        data.detach();
        try {
            c.decoder().decode(bytes, null, c.engine().getSampleRate(), new DecodeWarningCallback() {
                /**
                 * Reports on the console of the page that the audio data was damaged or cut off and that the buffer holds only what could be decoded (the specification of 1.1.2 recommends a warning
                 * for developers; the promise still resolves, as the decoder returns what it decoded). It is called just before {@link #onSuccess(AudioBuffer)}, in the same task of the page.
                 * <p>
                 * 오디오 데이터가 손상되었거나 잘려서 버퍼가 디코딩할 수 있었던 만큼만 담고 있다는 경고를 페이지의 콘솔에 보고한다(1.1.2 는 개발자를 위한 경고를 권고한다. 디코더가
                 * 디코딩한 만큼을 돌려주므로 프라미스는 그대로 resolve 된다). {@link #onSuccess(AudioBuffer)} 직전에 같은 페이지 작업에서 불린다.
                 *
                 * @param message the warning of the decoder / 디코더의 경고
                 */
                public void onWarning(String message) {
                    realm.getConsole().message("warn", "decodeAudioData: " + message);
                }

                /**
                 * Resolves the promise with the decoded buffer and calls the success callback.
                 * <p>
                 * 프라미스를 디코딩한 버퍼로 resolve 하고 성공 콜백을 부른다.
                 *
                 * @param buffer the decoded audio / 디코딩한 오디오
                 */
                public void onSuccess(AudioBuffer buffer) {
                    JsValue value = bridge.toJs(AudioBufferValue.adopt(buffer));
                    d.resolve(value);
                    callCallback(success, value);
                }

                /**
                 * Rejects the promise with the error and calls the error callback.
                 * <p>
                 * 프라미스를 오류로 reject 하고 오류 콜백을 부른다.
                 *
                 * @param error the error / 오류
                 */
                public void onError(AudioException error) {
                    JsValue e = api.errorValue(error);
                    d.reject(e);
                    callCallback(failure, e);
                }
            }, new Executor() {
                /**
                 * Runs the callback as a task of the page.
                 * <p>
                 * 콜백을 페이지의 작업으로 실행한다.
                 *
                 * @param command the callback / 콜백
                 */
                public void execute(Runnable command) {
                    api.postTask(command);
                }
            });
        } catch (AudioException e) {
            failDecode(d, failure, api.errorValue(e));
        }
        return d.promise;
    }

    /**
     * Rejects the promise of a decoding at once and queues the call of the error callback (the specification calls it in a task of its own).
     * <p>
     * 디코딩의 프라미스를 즉시 reject 하고 오류 콜백 호출을 쌓는다(규격은 자기 작업에서 부른다).
     *
     * @param d       the promise / 프라미스
     * @param failure the error callback, or null / 오류 콜백. 없으면 null
     * @param error   the error / 오류
     */
    private void failDecode(Promises.Deferred d, final JsValue failure, final JsValue error) {
        d.reject(error);
        if (failure != null) {
            api.postTask(new Runnable() {
                /**
                 * Calls the error callback.
                 * <p>
                 * 오류 콜백을 부른다.
                 */
                public void run() {
                    callCallback(failure, error);
                }
            });
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 스크립트 프로세서
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Implements <code>createScriptProcessor(bufferSize, numberOfInputChannels, numberOfOutputChannels)</code> (1.29, deprecated): <code>bufferSize</code> 0 lets the implementation pick
     * (2048), otherwise a power of two from 256 to 16384; the numbers of channels are from 0 to 32, not both 0 (everything else is an <code>IndexSizeError</code>). The handlers of the
     * <code>audioprocess</code> event run as tasks of the page, two blocks of <code>bufferSize</code> frames behind the audio (see {@link ScriptProcessorNode}).
     * <p>
     * <code>createScriptProcessor(bufferSize, numberOfInputChannels, numberOfOutputChannels)</code> 를 구현한다(1.29, 폐기 예정): <code>bufferSize</code> 0 은 구현이 고르고(2048), 아니면
     * 256 ~ 16384 의 2 의 거듭제곱이다. 채널 수는 0 ~ 32 이며 둘 다 0 은 안 된다(나머지는 모두 <code>IndexSizeError</code>). <code>audioprocess</code> 이벤트의 핸들러는 페이지의 작업으로
     * 실행되며 오디오보다 <code>bufferSize</code> 프레임 블록 둘만큼 뒤진다({@link ScriptProcessorNode} 참고).
     *
     * @param c    the context / 컨텍스트
     * @param args the arguments / 인수
     * @return the node / 노드
     */
    private JsValue createScriptProcessor(ContextValue c, JsValue[] args) {
        int bufferSize = args.length > 0 && !args[0].isUndefined() ? Idl.ulong(args[0]) : 0;
        int inputs = args.length > 1 && !args[1].isUndefined() ? Idl.ulong(args[1]) : 2;
        int outputs = args.length > 2 && !args[2].isUndefined() ? Idl.ulong(args[2]) : 2;
        ScriptProcessorNode node = c.engine().createScriptProcessor(bufferSize, inputs, outputs);
        NodeValue v = nodes.wrap(c, node);
        node.setProcessListener(new ScriptProcessing(v));
        return bridge.toJs(v);
    }

    /**
     * The code that the engine runs for each block of a script processor (as a task of the page): it builds the <code>AudioProcessingEvent</code> over the input and the output of the block,
     * dispatches it at the node, and takes the <code>outputBuffer</code> that the handlers filled back into the engine. Without a listener of the event nothing is built and the output stays
     * silent.
     * <p>
     * 엔진이 스크립트 프로세서의 블록마다 (페이지의 작업으로) 실행하는 코드: 블록의 입력과 출력 위에 <code>AudioProcessingEvent</code> 를 만들어 노드에 전달하고, 핸들러가 채운
     * <code>outputBuffer</code> 를 엔진으로 되가져간다. 이벤트의 리스너가 없으면 아무것도 만들지 않으며 출력은 무음으로 남는다.
     */
    private final class ScriptProcessing implements ScriptProcessorNode.ProcessListener {

        /**
         * The node that the event is dispatched at.
         * <p>
         * 이벤트를 전달할 노드.
         */
        private final NodeValue node;

        /**
         * Creates the code of a node.
         * <p>
         * 노드의 코드를 만든다.
         *
         * @param node the node / 노드
         */
        ScriptProcessing(NodeValue node) {
            this.node = node;
        }

        /**
         * Processes one block: the buffers of the event are the buffers of the block (a script that touches a channel gets its own bytes, a channel that it does not touch costs no copy);
         * what the output buffer holds when the handlers have run is played, and the buffers are made independent of the block before it goes back to the pool of the node.
         * <p>
         * 블록 하나를 처리한다: 이벤트의 버퍼는 블록의 버퍼이다(스크립트가 닿은 채널은 자기 바이트를 얻고 닿지 않은 채널은 복사가 없다). 핸들러가 실행된 뒤 출력 버퍼에 있는 것이 재생되며,
         * 버퍼는 노드의 풀로 돌아가기 전에 블록에서 독립하게 한다.
         *
         * @param inputBuffer  the input of the block, or null / 블록의 입력. 없으면 null
         * @param outputBuffer the output of the block, silent at first, or null / 블록의 출력. 처음에는 무음. 없으면 null
         * @param playbackTime the context time at which the output starts to play / 출력이 재생되기 시작하는 컨텍스트 시간
         */
        public void process(AudioBuffer inputBuffer, AudioBuffer outputBuffer, double playbackTime) {
            // 이벤트의 리스너(addEventListener 와 onaudioprocess)가 없거나 페이지가 끝났으면 출력은 무음으로 둔다.
            if (api.isDisposed() || !node.hasEventListeners("audioprocess")) {
                return;
            }
            AudioBufferValue in = inputBuffer == null ? null : AudioBufferValue.adopt(inputBuffer);
            AudioBufferValue out = outputBuffer == null ? null : AudioBufferValue.adopt(outputBuffer);
            AudioProcessingEvent e = new AudioProcessingEvent("audioprocess", false, false, playbackTime, in, out);
            api.adopt(e, processingProto);
            try {
                api.fire(node, e);
            } finally {
                // 핸들러가 쓴 출력을 엔진 블록으로 되가져가고, 두 버퍼를 블록에서 독립하게 한다(블록은 곧 재활용된다).
                if (out != null) {
                    out.storeInto(outputBuffer);
                    out.detach();
                }
                if (in != null) {
                    in.detach();
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 실시간 컨텍스트
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Brings the visible state of a context in line with the engine and fires <code>statechange</code> if it changed (call it inside a task of the page).
     * <p>
     * 컨텍스트의 보이는 상태를 엔진에 맞추고 바뀌었으면 <code>statechange</code> 를 발생시킨다(페이지의 작업 안에서 부른다).
     *
     * @param c the context / 컨텍스트
     */
    private void syncState(ContextValue c) {
        if (c.setState(c.engine().getState().getScriptName())) {
            api.fire(c, "statechange");
        }
    }

    /**
     * Installs <code>AudioContext</code> with its constructor.
     * <p>
     * <code>AudioContext</code> 를 생성자와 함께 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installRealtime(final JsObject proto) {
        bridge.getEventHandlers().define(proto, ContextValue.class, new String[] {"sinkchange"});
        api.accessor(proto, "baseLatency", new WebAudioApi.Op() {
            /**
             * Returns the processing latency in seconds.
             * <p>
             * 처리 지연(초)을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the latency / 지연
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(contextOf(self).engine().getBaseLatency());
            }
        }, null);
        api.accessor(proto, "outputLatency", new WebAudioApi.Op() {
            /**
             * Returns the output latency in seconds.
             * <p>
             * 출력 지연(초)을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the latency / 지연
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(contextOf(self).engine().getOutputLatency());
            }
        }, null);
        api.accessor(proto, "sinkId", new WebAudioApi.Op() {
            /**
             * Returns the identifier of the output device: the empty string for the default device, or an object <code>{type: "none"}</code> after the output was switched off.
             * <p>
             * 출력 장치의 식별자를 반환한다: 기본 장치이면 빈 문자열, 출력을 껐으면 <code>{type: "none"}</code> 객체.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the identifier / 식별자
             */
            public JsValue call(JsValue self, JsValue[] args) {
                ContextValue c = contextOf(self);
                if (!c.sinkNone) {
                    return Js.str("");
                }
                JsObject info = realm.newObject();
                info.defineData("type", Js.str("none"), true, true, true);
                return info;
            }
        }, null);
        api.method(proto, "getOutputTimestamp", 0, new WebAudioApi.Op() {
            /**
             * Returns the position of the output as <code>{contextTime, performanceTime}</code>.
             * <p>
             * 출력의 위치를 <code>{contextTime, performanceTime}</code> 로 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the timestamp / 타임스탬프
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return outputTimestamp(contextOf(self));
            }
        });
        promiseMethod(proto, "resume", 0, new WebAudioApi.Op() {
            /**
             * Resumes the progression of the time.
             * <p>
             * 시간의 진행을 재개한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return control(contextOf(self), 0);
            }
        });
        promiseMethod(proto, "suspend", 0, new WebAudioApi.Op() {
            /**
             * Suspends the progression of the time.
             * <p>
             * 시간의 진행을 일시 중단한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return control(contextOf(self), 1);
            }
        });
        promiseMethod(proto, "close", 0, new WebAudioApi.Op() {
            /**
             * Closes the context.
             * <p>
             * 컨텍스트를 닫는다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return control(contextOf(self), 2);
            }
        });
        promiseMethod(proto, "setSinkId", 1, new WebAudioApi.Op() {
            /**
             * Chooses the output device: the default one (the empty string) or none.
             * <p>
             * 출력 장치를 고른다: 기본 장치(빈 문자열) 또는 없음.
             *
             * @param self this value / this 값
             * @param args arguments: the identifier / 인수: 식별자
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return setSinkId(contextOf(self), args);
            }
        });
        api.method(proto, "createMediaElementSource", 1, new WebAudioApi.Op() {
            /**
             * Creates a source node for the audio of an <code>audio</code> element: from now on the sound of the element goes through the graph, not to the speakers.
             * <p>
             * <code>audio</code> 요소의 오디오를 위한 원천 노드를 만든다: 이제부터 요소의 소리는 스피커가 아니라 그래프를 거쳐 나간다.
             *
             * @param self this value / this 값
             * @param args arguments: the element / 인수: 요소
             * @return the node / 노드
             */
            public JsValue call(JsValue self, JsValue[] args) {
                ContextValue c = contextOf(self);
                Idl.requireArgs(args, 1, "createMediaElementSource", "AudioContext");
                JsObject options = realm.newObject();
                options.defineData("mediaElement", args[0], true, true, true);
                return createNode(self, "MediaElementAudioSourceNode", options);
            }
        });
        String[] refused = {"createMediaStreamSource", "createMediaStreamTrackSource", "createMediaStreamDestination"};
        // 미디어 스트림 계열 팩터리는 이름만 두고 NotSupportedError 를 던진다(명시적 예외 EXC-14).
        for (int i = 0; i < refused.length; i++) {
            final String name = refused[i];
            api.method(proto, name, name.endsWith("Destination") ? 0 : 1, new WebAudioApi.Op() {
                /**
                 * Refuses: the project has no media streams.
                 * <p>
                 * 거부한다: 프로젝트에는 미디어 스트림이 없다.
                 *
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return never / 반환하지 않음
                 */
                public JsValue call(JsValue self, JsValue[] args) {
                    contextOf(self);
                    throw new AudioException(AudioException.Kind.NOT_SUPPORTED, name + " is not supported: there are no media streams.");
                }
            });
        }
        bridge.setConstructor("AudioContext", new NativeFunction(realm, bridge.getInterface("BaseAudioContext").getConstructor(), "AudioContext", 0,
                new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'AudioContext': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a context that plays.
             * <p>
             * 소리를 내는 컨텍스트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the options / 인수: 옵션
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                ContextValue c;
                try {
                    c = createRealtime(Js.arg(args, 0));
                } catch (AudioException e) {
                    throw api.error(e);
                }
                HostObject w = bridge.wrap(c);
                c.bindWrapper(w);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, realtimeProto));
                return w;
            }
        }));
    }

    /**
     * Creates a real-time context from the options <code>{latencyHint, sampleRate}</code> and starts it.
     * <p>
     * 옵션 <code>{latencyHint, sampleRate}</code> 로 실시간 컨텍스트를 만들고 시작한다.
     *
     * @param options the options argument / 옵션 인수
     * @return the context / 컨텍스트
     */
    private ContextValue createRealtime(JsValue options) {
        JsObject dict = Idl.dictionary(options, "Failed to construct 'AudioContext': The provided value");
        JsValue hint = Idl.member(dict, "latencyHint");
        double latency = RealtimeAudioContext.LATENCY_INTERACTIVE;
        // latencyHint 는 열거형 문자열이거나 초 단위 수이다.
        if (!hint.isUndefined()) {
            if (hint instanceof JsString) {
                latency = RealtimeAudioContext.resolveLatency(Idl.stringEnum(Ops.toString(hint), new String[] {"balanced", "interactive", "playback"},
                        "AudioContextLatencyCategory"), 0);
            } else {
                latency = RealtimeAudioContext.resolveLatency(null, Idl.dbl(hint));
            }
        }
        JsValue rate = Idl.member(dict, "sampleRate");
        float sampleRate = rate.isUndefined() ? RealtimeAudioContext.DEFAULT_SAMPLE_RATE : Idl.flt(rate);
        // sinkId 는 기본 장치(빈 문자열)이거나 {type: "none"} 이다. 다른 장치는 지원하지 않는다.
        JsValue sink = Idl.member(dict, "sinkId");
        boolean none = sink.isUndefined() ? false : parseSink(sink, "Failed to construct 'AudioContext'");
        if (api.isDisposed()) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "The document is not fully active.");
        }
        RealtimeAudioContext engine = OutputRenderDriver.createContext(sampleRate, latency, none ? OutputRenderDriver.silentOutputs() : hostOutputs(),
                "swingwebpanel-audio-render-" + api.nextRenderSerial());
        ContextValue c = new ContextValue(api, engine);
        c.sinkNone = none;
        api.register(c);
        engine.setTaskSink(api.sink());
        c.destination = nodes.wrap(c, engine.getDestination());
        engine.resume();
        syncLater(c);
        return c;
    }

    /**
     * Returns the factory of the outputs that the component chose for the real-time contexts of the page, or null for the system output (Java Sound).
     * <p>
     * 컴포넌트가 페이지의 실시간 컨텍스트를 위해 고른 출력의 팩터리를 반환한다. 시스템 출력(Java Sound)이면 null.
     *
     * @return the factory or null / 팩터리 또는 null
     */
    private OutputFactory hostOutputs() {
        return api.page().getEnvironment().getWebAudioOutputFactory();
    }

    /**
     * Reads a sink identifier of the constructor or of <code>setSinkId</code>: the empty string is the default device (false), <code>{type: "none"}</code> is no output (true); any other
     * device is a <code>NotSupportedError</code> (the project has one output device, the one that the host chose).
     * <p>
     * 생성자나 <code>setSinkId</code> 의 싱크 식별자를 읽는다: 빈 문자열은 기본 장치(false), <code>{type: "none"}</code> 은 출력 없음(true)이다. 다른 장치는
     * <code>NotSupportedError</code> 이다(프로젝트에는 호스트가 고른 출력 장치 하나뿐이다).
     *
     * @param sink the value / 값
     * @param who  the call for the messages / 메시지에 쓸 호출
     * @return true for no output / 출력 없음이면 true
     */
    private static boolean parseSink(JsValue sink, String who) {
        if (sink.isObject()) {
            JsValue type = ((JsObject) sink).get("type");
            if (type.isUndefined() || !"none".equals(Ops.toString(type))) {
                throw JsException.typeError(who + ": The provided value is not a valid AudioSinkOptions.");
            }
            return true;
        } else if ("".equals(Ops.toString(sink))) {
            return false;
        }
        throw new AudioException(AudioException.Kind.NOT_SUPPORTED, who + ": the audio output device '" + Ops.toString(sink) + "' is not supported.");
    }

    /**
     * Queues the task that brings the visible state in line with the engine.
     * <p>
     * 보이는 상태를 엔진에 맞추는 작업을 쌓는다.
     *
     * @param c the context / 컨텍스트
     */
    private void syncLater(final ContextValue c) {
        api.postTask(new Runnable() {
            /**
             * Synchronizes the state.
             * <p>
             * 상태를 맞춘다.
             */
            public void run() {
                syncState(c);
            }
        });
    }

    /**
     * Implements <code>resume()</code> (0), <code>suspend()</code> (1) and <code>close()</code> (2) of a real-time context.
     * <p>
     * 실시간 컨텍스트의 <code>resume()</code>(0), <code>suspend()</code>(1), <code>close()</code>(2)를 구현한다.
     *
     * @param c    the context / 컨텍스트
     * @param what the operation / 연산
     * @return the promise / 프라미스
     */
    private JsValue control(final ContextValue c, int what) {
        if (api.isDisposed()) {
            throw api.domError("InvalidStateError", "The document is not fully active.");
        }
        if (c.closeRequested) {
            throw api.domError("InvalidStateError", "Cannot " + (what == 0 ? "resume" : what == 1 ? "suspend" : "close") + " a closed AudioContext.");
        }
        final Promises.Deferred d = Promises.create(realm);
        if (what == 0) {
            c.realtime().resume();
        } else if (what == 1) {
            c.realtime().suspend();
        } else {
            c.closeRequested = true;
            c.realtime().close();
            c.closeDecoder();
            // 연결된 미디어 요소는 영구히 노드에 묶인 채 소리 없이 계속 진행하도록 탭을 놓는다.
            c.releaseTaps();
            api.unregister(c);
        }
        api.postTask(new Runnable() {
            /**
             * Synchronizes the state (the event first) and resolves the promise.
             * <p>
             * 상태를 맞추고(이벤트가 먼저) 프라미스를 resolve 한다.
             */
            public void run() {
                syncState(c);
                d.resolve(Js.UNDEFINED);
            }
        });
        return d.promise;
    }

    /**
     * Implements <code>setSinkId</code>: the default device (the empty string) and <code>{type: "none"}</code>; every other device is a <code>NotSupportedError</code>.
     * <p>
     * <code>setSinkId</code> 를 구현한다: 기본 장치(빈 문자열)와 <code>{type: "none"}</code>. 다른 장치는 모두 <code>NotSupportedError</code> 이다.
     *
     * @param c    the context / 컨텍스트
     * @param args the arguments / 인수
     * @return the promise / 프라미스
     */
    private JsValue setSinkId(final ContextValue c, JsValue[] args) {
        Idl.requireArgs(args, 1, "setSinkId", "AudioContext");
        boolean none = parseSink(args[0], "Failed to execute 'setSinkId' on 'AudioContext'");
        if (api.isDisposed()) {
            throw api.domError("InvalidStateError", "Failed to execute 'setSinkId' on 'AudioContext': the document is not fully active.");
        }
        final Promises.Deferred d = Promises.create(realm);
        final boolean changed = c.sinkNone != none;
        c.sinkNone = none;
        OutputRenderDriver driver = c.outputDriver();
        // 렌더링 드라이버에 새 출력의 팩터리를 알리고, 달라졌으면 돌고 있는 구간을 곧바로 새 출력으로 바꾼다(일시정지 중이면 다음 재개부터 새 출력을 쓴다).
        if (driver != null) {
            driver.setOutputFactory(none ? OutputRenderDriver.silentOutputs() : hostOutputs());
            if (changed) {
                driver.restart();
            }
        }
        api.postTask(new Runnable() {
            /**
             * Fires <code>sinkchange</code> if the sink changed and resolves the promise.
             * <p>
             * 싱크가 바뀌었으면 <code>sinkchange</code> 를 발생시키고 프라미스를 resolve 한다.
             */
            public void run() {
                d.resolve(Js.UNDEFINED);
                if (changed) {
                    api.fire(c, "sinkchange");
                }
            }
        });
        return d.promise;
    }

    /**
     * Builds the result of <code>getOutputTimestamp()</code>: the position of the context behind the latencies, and the time of the page clock (zeros before the first quantum).
     * <p>
     * <code>getOutputTimestamp()</code> 의 결과를 만든다: 지연만큼 뒤의 컨텍스트 위치와 페이지 시계의 시간(첫 quantum 전에는 0).
     *
     * @param c the context / 컨텍스트
     * @return the object / 객체
     */
    private JsValue outputTimestamp(ContextValue c) {
        BaseAudioContext e = c.engine();
        double contextTime = 0;
        double performanceTime = 0;
        // 첫 quantum 을 렌더링한 뒤에만 값이 있다. 컨텍스트 시간은 현재 시간보다 앞서지 않는다.
        if (e.getCurrentFrame() > 0) {
            OutputRenderDriver driver = c.outputDriver();
            double now = 0;
            JsValue perf = realm.getGlobalObject().get("performance");
            if (perf.isObject()) {
                now = Ops.toNumber(Ops.invoke(realm, perf, "now", Js.NO_ARGS));
            }
            // 장치로 재생하는 드라이버는 지금 들리는 컨텍스트 시간을 안다. 그 시각과 지금의 차이만큼 페이지 시계를 되돌려 같은 순간의 페이지 시계 값을 만든다.
            if (driver != null) {
                OutputRenderDriver.Timestamp ts = driver.getOutputTimestamp();
                contextTime = Math.max(0.0, ts.getContextTime());
                performanceTime = Math.max(0.0, now - (System.nanoTime() - ts.getNanoTime()) / 1e6);
            } else {
                contextTime = Math.max(0.0, e.getCurrentTime() - e.getBaseLatency() - e.getOutputLatency());
                performanceTime = now;
            }
        }
        JsObject o = realm.newObject();
        o.defineData("contextTime", Js.num(contextTime), true, true, true);
        o.defineData("performanceTime", Js.num(performanceTime), true, true, true);
        return o;
    }

    // ---------------------------------------------------------------------------------------------------------------------------------
    // 오프라인 컨텍스트
    // ---------------------------------------------------------------------------------------------------------------------------------

    /**
     * Returns the offline context of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 오프라인 컨텍스트를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the context / 컨텍스트
     */
    private static ContextValue offlineOf(JsValue self) {
        ContextValue c = contextOf(self);
        if (!c.isOffline()) {
            throw JsException.typeError("Illegal invocation");
        }
        return c;
    }

    /**
     * Installs <code>OfflineAudioContext</code> with its constructor.
     * <p>
     * <code>OfflineAudioContext</code> 를 생성자와 함께 설치한다.
     *
     * @param proto the prototype / 프로토타입
     */
    private void installOffline(JsObject proto) {
        bridge.getEventHandlers().define(proto, ContextValue.class, new String[] {"complete"});
        api.accessor(proto, "length", new WebAudioApi.Op() {
            /**
             * Returns the length of the rendering in frames.
             * <p>
             * 렌더링 길이(프레임)를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the length / 길이
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(offlineOf(self).offline().getLength());
            }
        }, null);
        promiseMethod(proto, "startRendering", 0, new WebAudioApi.Op() {
            /**
             * Starts the rendering.
             * <p>
             * 렌더링을 시작한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise of the rendered buffer / 렌더링한 버퍼의 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return startRendering(offlineOf(self));
            }
        });
        promiseMethod(proto, "resume", 0, new WebAudioApi.Op() {
            /**
             * Continues a suspended rendering.
             * <p>
             * 일시 중단된 렌더링을 이어 간다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the promise / 프라미스
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return resumeRendering(offlineOf(self));
            }
        });
        promiseMethod(proto, "suspend", 1, new WebAudioApi.Op() {
            /**
             * Schedules a suspension of the rendering at a time.
             * <p>
             * 시간에 렌더링의 일시 중단을 예약한다.
             *
             * @param self this value / this 값
             * @param args arguments: the time / 인수: 시간
             * @return the promise, resolved when the rendering stands still / 프라미스. 렌더링이 서면 resolve 된다
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return suspendAt(offlineOf(self), args);
            }
        });
        bridge.setConstructor("OfflineAudioContext", new NativeFunction(realm, bridge.getInterface("BaseAudioContext").getConstructor(), "OfflineAudioContext", 1,
                new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'OfflineAudioContext': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an offline context from the options or from the three arguments.
             * <p>
             * 옵션 또는 인수 셋으로 오프라인 컨텍스트를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                ContextValue c;
                try {
                    c = createOffline(args);
                } catch (AudioException e) {
                    throw api.error(e);
                }
                HostObject w = bridge.wrap(c);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, offlineProto));
                return w;
            }
        }));
    }

    /**
     * Creates an offline context: <code>new OfflineAudioContext({numberOfChannels, length, sampleRate})</code> or <code>new OfflineAudioContext(numberOfChannels, length, sampleRate)</code>.
     * <p>
     * 오프라인 컨텍스트를 만든다: <code>new OfflineAudioContext({numberOfChannels, length, sampleRate})</code> 또는 <code>new OfflineAudioContext(numberOfChannels, length, sampleRate)</code>.
     *
     * @param args the arguments of the constructor / 생성자의 인수
     * @return the context / 컨텍스트
     */
    private ContextValue createOffline(JsValue[] args) {
        int channels;
        int length;
        float rate;
        // 첫 인수가 객체이면 옵션 형식, 아니면 인수 셋 형식이다.
        if (args.length >= 1 && args[0].isObject()) {
            JsObject dict = (JsObject) args[0];
            JsValue len = dict.get("length");
            JsValue sr = dict.get("sampleRate");
            if (len.isUndefined()) {
                throw JsException.typeError("Failed to construct 'OfflineAudioContext': required member length is undefined.");
            }
            if (sr.isUndefined()) {
                throw JsException.typeError("Failed to construct 'OfflineAudioContext': required member sampleRate is undefined.");
            }
            JsValue ch = dict.get("numberOfChannels");
            channels = ch.isUndefined() ? 1 : Idl.ulong(ch);
            length = Idl.ulong(len);
            rate = Idl.flt(sr);
        } else {
            Idl.requireCtorArgs(args, 3, "OfflineAudioContext");
            channels = Idl.ulong(args[0]);
            length = Idl.ulong(args[1]);
            rate = Idl.flt(args[2]);
        }
        if (api.isDisposed()) {
            throw new AudioException(AudioException.Kind.INVALID_STATE, "The document is not fully active.");
        }
        // 엔진은 만들 때 결과 전체를 할당하므로 먼저 페이지의 메모리 예산을 확인한다(범위 밖의 값은 엔진이 NotSupportedError 로 거부한다).
        int quantum = WebAudioLimits.RENDER_QUANTUM_SIZE;
        long rounded = length < 1 ? 0 : ((long) length + quantum - 1) / quantum * quantum;
        long samples = rounded * Math.max(1, channels);
        boolean reserved = false;
        if (channels >= 1 && channels <= WebAudioLimits.MAX_CHANNELS && length >= 1) {
            if (!api.reserveOffline(samples)) {
                throw new AudioException(AudioException.Kind.NOT_SUPPORTED, "The offline contexts of the page hold too much memory: " + samples + " more samples would exceed the limit.");
            }
            reserved = true;
        }
        OfflineAudioContext engine;
        try {
            engine = new OfflineAudioContext(channels, length, rate);
        } catch (RuntimeException e) {
            if (reserved) {
                api.releaseOffline(samples);
            }
            throw e;
        }
        ContextValue c = new ContextValue(api, engine);
        c.reservedSamples = samples;
        try {
            api.register(c);
        } catch (AudioException e) {
            api.releaseOffline(samples);
            c.reservedSamples = 0;
            throw e;
        }
        engine.setTaskSink(api.sink());
        c.destination = nodes.wrap(c, engine.getDestination());
        return c;
    }

    /**
     * Returns the renderer of an offline context, creating it when the first suspension or the rendering needs it.
     * <p>
     * 오프라인 컨텍스트의 렌더러를 반환한다. 첫 일시 중단이나 렌더링이 필요로 할 때 만든다.
     *
     * @param c the context / 컨텍스트
     * @return the renderer / 렌더러
     */
    private OfflineRender rendererOf(ContextValue c) {
        OfflineRender r = c.renderer;
        if (r == null) {
            r = new OfflineRender(api, this, c);
            c.renderer = r;
        }
        return r;
    }

    /**
     * Implements <code>startRendering()</code>.
     * <p>
     * <code>startRendering()</code> 을 구현한다.
     *
     * @param c the context / 컨텍스트
     * @return the promise / 프라미스
     */
    private JsValue startRendering(final ContextValue c) {
        if (api.isDisposed()) {
            throw api.domError("InvalidStateError", "The document is not fully active.");
        }
        if (c.renderingStarted) {
            throw api.domError("InvalidStateError", "cannot call startRendering more than once");
        }
        c.renderingStarted = true;
        Promises.Deferred d = Promises.create(realm);
        c.setState("running");
        api.postTask(new Runnable() {
            /**
             * Fires <code>statechange</code> for the change to <code>running</code>.
             * <p>
             * <code>running</code> 으로의 변경에 대해 <code>statechange</code> 를 발생시킨다.
             */
            public void run() {
                api.fire(c, "statechange");
            }
        });
        rendererOf(c).begin(d);
        return d.promise;
    }

    /**
     * Implements <code>resume()</code> of an offline context.
     * <p>
     * 오프라인 컨텍스트의 <code>resume()</code> 을 구현한다.
     *
     * @param c the context / 컨텍스트
     * @return the promise / 프라미스
     */
    private JsValue resumeRendering(final ContextValue c) {
        if (api.isDisposed()) {
            throw api.domError("InvalidStateError", "The document is not fully active.");
        }
        if (c.getState().equals("closed")) {
            throw api.domError("InvalidStateError", "cannot resume a closed OfflineAudioContext");
        }
        if (!c.renderingStarted) {
            throw api.domError("InvalidStateError", "cannot resume an OfflineAudioContext that has not started");
        }
        final Promises.Deferred d = Promises.create(realm);
        final boolean changed = c.setState("running");
        rendererOf(c).resume();
        api.postTask(new Runnable() {
            /**
             * Resolves the promise and fires <code>statechange</code> if the state changed.
             * <p>
             * 프라미스를 resolve 하고 상태가 바뀌었으면 <code>statechange</code> 를 발생시킨다.
             */
            public void run() {
                if (changed) {
                    api.fire(c, "statechange");
                }
                d.resolve(Js.UNDEFINED);
            }
        });
        return d.promise;
    }

    /**
     * Implements <code>suspend(suspendTime)</code> of an offline context: the time is rounded up to the render quantum; a frame in the past, at or after the end
     * or already scheduled is an <code>InvalidStateError</code>.
     * <p>
     * 오프라인 컨텍스트의 <code>suspend(suspendTime)</code> 을 구현한다: 시간은 render quantum 으로 올림하며, 지난 프레임, 끝 이후의 프레임, 이미 예약된 프레임은
     * <code>InvalidStateError</code> 이다.
     *
     * @param c    the context / 컨텍스트
     * @param args the arguments / 인수
     * @return the promise / 프라미스
     */
    private JsValue suspendAt(ContextValue c, JsValue[] args) {
        Idl.requireArgs(args, 1, "suspend", "OfflineAudioContext");
        double time = Idl.dbl(args[0]);
        if (api.isDisposed()) {
            throw api.domError("InvalidStateError", "The document is not fully active.");
        }
        if (c.getState().equals("closed")) {
            throw api.domError("InvalidStateError", "cannot schedule a suspend on a closed OfflineAudioContext");
        }
        OfflineAudioContext e = c.offline();
        double quantum = e.getRenderQuantumSize();
        double frameCount = Math.ceil(time * e.getSampleRate() / quantum);
        long frame = frameCount < 0 ? -1 : (long) (frameCount * quantum);
        if (frameCount < 0 || frameCount > e.getLength() || frame <= e.getCurrentFrame() || frame >= e.getLength()) {
            throw api.domError("InvalidStateError", "cannot schedule a suspend at frame " + frame + " (" + time + " seconds): it is outside the rendering or not after the current time");
        }
        Promises.Deferred d = Promises.create(realm);
        if (!rendererOf(c).schedule(frame, d)) {
            throw api.domError("InvalidStateError", "cannot schedule more than one suspend at frame " + frame + " (" + time + " seconds)");
        }
        return d.promise;
    }
}
