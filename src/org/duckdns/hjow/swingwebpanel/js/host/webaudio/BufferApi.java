package org.duckdns.hjow.swingwebpanel.js.host.webaudio;

import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.builtins.typed.ElementType;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;

/**
 * The script interface <code>AudioBuffer</code> (Web Audio API 1.4): the attributes <code>sampleRate</code>, <code>length</code>, <code>duration</code> and
 * <code>numberOfChannels</code>, the operations <code>getChannelData</code>, <code>copyFromChannel</code> and <code>copyToChannel</code> over an {@link AudioBufferValue}, and the
 * constructor <code>new AudioBuffer({length, sampleRate, numberOfChannels})</code>. The rules about the data (the same <code>Float32Array</code> for a channel every time, the
 * snapshot that a node acquires) are those of {@link AudioBufferValue}.
 * <p>
 * 스크립트 인터페이스 <code>AudioBuffer</code>(Web Audio API 1.4): 속성 <code>sampleRate</code>, <code>length</code>, <code>duration</code>, <code>numberOfChannels</code>, {@link AudioBufferValue}
 * 위의 연산 <code>getChannelData</code>, <code>copyFromChannel</code>, <code>copyToChannel</code>, 생성자 <code>new AudioBuffer({length, sampleRate, numberOfChannels})</code>.
 * 데이터에 관한 규칙(채널마다 매번 같은 <code>Float32Array</code>, 노드가 가져가는 스냅샷)은 {@link AudioBufferValue} 의 것이다.
 */
final class BufferApi {

    /**
     * The owner of the script interface.
     * <p>
     * 스크립트 인터페이스의 소유자.
     */
    private final WebAudioApi api;

    /**
     * The prototype of <code>AudioBuffer</code>.
     * <p>
     * <code>AudioBuffer</code> 의 프로토타입.
     */
    private JsObject proto;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param api the owner of the script interface / 스크립트 인터페이스의 소유자
     */
    BufferApi(WebAudioApi api) {
        this.api = api;
    }

    /**
     * Returns the buffer value of a receiver or throws the standard TypeError.
     * <p>
     * 수신자의 버퍼 값을 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the value / 값
     */
    static AudioBufferValue buffer(JsValue self) {
        AudioBufferValue v = (AudioBufferValue) DomBridge.targetOf(self, AudioBufferValue.class);
        if (v == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return v;
    }

    /**
     * Returns the buffer value of an argument, or throws a TypeError that names the parameter.
     * <p>
     * 인수의 버퍼 값을 반환하거나 매개변수를 이름 붙인 TypeError 를 던진다.
     *
     * @param v    the argument / 인수
     * @param what the description for the message / 메시지에 쓸 설명
     * @return the value / 값
     */
    static AudioBufferValue bufferArg(JsValue v, String what) {
        AudioBufferValue b = (AudioBufferValue) DomBridge.targetOf(v, AudioBufferValue.class);
        if (b == null) {
            throw JsException.typeError(what + " is not of type 'AudioBuffer'.");
        }
        return b;
    }

    /**
     * Wraps a buffer value for the script (the same value always gives the same object while the page holds it).
     * <p>
     * 버퍼 값을 스크립트용으로 감싼다(같은 값은 페이지가 붙잡고 있는 동안 언제나 같은 객체를 준다).
     *
     * @param v the value / 값
     * @return the script object / 스크립트 객체
     */
    JsValue wrap(AudioBufferValue v) {
        return api.bridge().toJs(v);
    }

    /**
     * Installs the interface.
     * <p>
     * 인터페이스를 설치한다.
     */
    void install() {
        final DomBridge bridge = api.bridge();
        final JsRealm realm = api.realm();
        final DomBridge.InterfaceInfo info = bridge.defineInterface("AudioBuffer", null, AudioBufferValue.class, new String[0]);
        proto = info.getPrototype();
        api.accessor(proto, "sampleRate", new WebAudioApi.Op() {
            /**
             * Returns the sample rate of the buffer.
             * <p>
             * 버퍼의 샘플링 주파수를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the sample rate / 샘플링 주파수
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(buffer(self).getSampleRate());
            }
        }, null);
        api.accessor(proto, "length", new WebAudioApi.Op() {
            /**
             * Returns the length of the buffer in frames.
             * <p>
             * 버퍼의 길이(프레임)를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the length / 길이
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(buffer(self).getLength());
            }
        }, null);
        api.accessor(proto, "duration", new WebAudioApi.Op() {
            /**
             * Returns the duration of the buffer in seconds.
             * <p>
             * 버퍼의 재생 시간(초)을 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the duration / 재생 시간
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(buffer(self).getDuration());
            }
        }, null);
        api.accessor(proto, "numberOfChannels", new WebAudioApi.Op() {
            /**
             * Returns the number of channels of the buffer.
             * <p>
             * 버퍼의 채널 수를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the number of channels / 채널 수
             */
            public JsValue call(JsValue self, JsValue[] args) {
                return Js.num(buffer(self).getNumberOfChannels());
            }
        }, null);
        api.method(proto, "getChannelData", 1, new WebAudioApi.Op() {
            /**
             * Returns the <code>Float32Array</code> of a channel.
             * <p>
             * 채널의 <code>Float32Array</code> 를 반환한다.
             *
             * @param self this value / this 값
             * @param args arguments: the channel / 인수: 채널
             * @return the array / 배열
             */
            public JsValue call(JsValue self, JsValue[] args) {
                AudioBufferValue b = buffer(self);
                Idl.requireArgs(args, 1, "getChannelData", "AudioBuffer");
                return b.channelData(realm, Idl.ulong(args[0]));
            }
        });
        api.method(proto, "copyFromChannel", 2, new WebAudioApi.Op() {
            /**
             * Copies samples of a channel into a <code>Float32Array</code>.
             * <p>
             * 채널의 표본을 <code>Float32Array</code> 로 복사한다.
             *
             * @param self this value / this 값
             * @param args arguments: the destination, the channel and the offset / 인수: 대상, 채널, offset
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                AudioBufferValue b = buffer(self);
                Idl.requireArgs(args, 2, "copyFromChannel", "AudioBuffer");
                b.copyFromChannel(Idl.typedArray(args[0], ElementType.FLOAT32, "Failed to execute 'copyFromChannel' on 'AudioBuffer': parameter 1"),
                        Idl.ulong(args[1]), args.length > 2 && !args[2].isUndefined() ? Idl.ulong(args[2]) : 0);
                return Js.UNDEFINED;
            }
        });
        api.method(proto, "copyToChannel", 2, new WebAudioApi.Op() {
            /**
             * Copies samples of a <code>Float32Array</code> into a channel.
             * <p>
             * <code>Float32Array</code> 의 표본을 채널로 복사한다.
             *
             * @param self this value / this 값
             * @param args arguments: the source, the channel and the offset / 인수: 원본, 채널, offset
             * @return undefined / undefined
             */
            public JsValue call(JsValue self, JsValue[] args) {
                AudioBufferValue b = buffer(self);
                Idl.requireArgs(args, 2, "copyToChannel", "AudioBuffer");
                b.copyToChannel(Idl.typedArray(args[0], ElementType.FLOAT32, "Failed to execute 'copyToChannel' on 'AudioBuffer': parameter 1"),
                        Idl.ulong(args[1]), args.length > 2 && !args[2].isUndefined() ? Idl.ulong(args[2]) : 0);
                return Js.UNDEFINED;
            }
        });
        bridge.setConstructor("AudioBuffer", new NativeFunction(realm, realm.getFunctionPrototype(), "AudioBuffer", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'AudioBuffer': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a buffer from the options <code>{length, sampleRate, numberOfChannels}</code>.
             * <p>
             * 옵션 <code>{length, sampleRate, numberOfChannels}</code> 로 버퍼를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments: the options / 인수: 옵션
             * @param newTarget the new target / new 대상
             * @return the wrapper / 래퍼
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Idl.requireCtorArgs(args, 1, "AudioBuffer");
                JsObject dict = Idl.dictionary(args[0], "Failed to construct 'AudioBuffer': The provided value");
                JsValue length = Idl.member(dict, "length");
                JsValue rate = Idl.member(dict, "sampleRate");
                if (length.isUndefined()) {
                    throw JsException.typeError("Failed to construct 'AudioBuffer': required member length is undefined.");
                }
                if (rate.isUndefined()) {
                    throw JsException.typeError("Failed to construct 'AudioBuffer': required member sampleRate is undefined.");
                }
                JsValue channels = Idl.member(dict, "numberOfChannels");
                AudioBufferValue v;
                try {
                    v = AudioBufferValue.create(channels.isUndefined() ? 1 : Idl.ulong(channels), Idl.ulong(length), Idl.flt(rate));
                } catch (org.duckdns.hjow.swingwebpanel.media.webaudio.AudioException e) {
                    throw api.error(e);
                }
                HostObject w = bridge.wrap(v);
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, proto));
                return w;
            }
        }));
    }
}
