package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.DomException;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.media.playback.MediaPlayer;

/**
 * The script interface of the media elements (plan items P18-05-c, P18-08, P18-09): the members of <code>HTMLMediaElement</code> that
 * depend on the state of the page (<code>error</code>, <code>networkState</code>, <code>readyState</code>, <code>currentTime</code>,
 * <code>duration</code>, <code>paused</code>, <code>ended</code>, <code>seeking</code>, the rates, <code>volume</code>, <code>muted</code>, the
 * time ranges, <code>load()</code>, <code>play()</code>, <code>pause()</code>, <code>canPlayType()</code>), the interfaces
 * <code>MediaError</code> and <code>TimeRanges</code>, <code>videoWidth</code> and <code>videoHeight</code> of <code>HTMLVideoElement</code>
 * (always 0), and the <code>Audio</code> constructor. The reflected content attributes (<code>src</code>, <code>autoplay</code>,
 * <code>loop</code>, <code>controls</code>, <code>preload</code>, <code>defaultMuted</code>) and the constants come from the interface
 * definitions of the DOM bridge. The members are thin: they find the state of the element in {@link MediaElements} and convert the values as
 * WebIDL does (a non-finite number is a <code>TypeError</code>).
 * <p>
 * 미디어 요소의 스크립트 인터페이스(계획 P18-05-c, P18-08, P18-09): 페이지의 상태에 따라 달라지는 <code>HTMLMediaElement</code> 의 멤버
 * (<code>error</code>, <code>networkState</code>, <code>readyState</code>, <code>currentTime</code>, <code>duration</code>, <code>paused</code>,
 * <code>ended</code>, <code>seeking</code>, 속도, <code>volume</code>, <code>muted</code>, 시간 범위, <code>load()</code>, <code>play()</code>,
 * <code>pause()</code>, <code>canPlayType()</code>), 인터페이스 <code>MediaError</code> 와 <code>TimeRanges</code>, <code>HTMLVideoElement</code> 의
 * <code>videoWidth</code>, <code>videoHeight</code>(항상 0), <code>Audio</code> 생성자. 속성으로 반영되는 콘텐츠 속성(<code>src</code>,
 * <code>autoplay</code>, <code>loop</code>, <code>controls</code>, <code>preload</code>, <code>defaultMuted</code>)과 상수는 DOM 브리지의 인터페이스
 * 정의에서 온다. 멤버는 얇다: {@link MediaElements} 에서 요소의 상태를 찾고 WebIDL 처럼 값을 변환한다(유한하지 않은 수는 <code>TypeError</code>).
 */
final class MediaBindings {

    /**
     * The id of a member that reads <code>error</code>.
     * <p>
     * <code>error</code> 를 읽는 멤버의 번호.
     */
    private static final int A_ERROR = 0;

    /**
     * The id of the <code>currentSrc</code> getter.
     * <p>
     * <code>currentSrc</code> getter 의 번호.
     */
    private static final int A_CURRENT_SRC = 1;

    /**
     * The id of the <code>networkState</code> getter.
     * <p>
     * <code>networkState</code> getter 의 번호.
     */
    private static final int A_NETWORK_STATE = 2;

    /**
     * The id of the <code>readyState</code> getter.
     * <p>
     * <code>readyState</code> getter 의 번호.
     */
    private static final int A_READY_STATE = 3;

    /**
     * The id of the <code>seeking</code> getter.
     * <p>
     * <code>seeking</code> getter 의 번호.
     */
    private static final int A_SEEKING = 4;

    /**
     * The id of the <code>currentTime</code> accessor.
     * <p>
     * <code>currentTime</code> 접근자의 번호.
     */
    private static final int A_CURRENT_TIME = 5;

    /**
     * The id of the <code>duration</code> getter.
     * <p>
     * <code>duration</code> getter 의 번호.
     */
    private static final int A_DURATION = 6;

    /**
     * The id of the <code>paused</code> getter.
     * <p>
     * <code>paused</code> getter 의 번호.
     */
    private static final int A_PAUSED = 7;

    /**
     * The id of the <code>ended</code> getter.
     * <p>
     * <code>ended</code> getter 의 번호.
     */
    private static final int A_ENDED = 8;

    /**
     * The id of the <code>defaultPlaybackRate</code> accessor.
     * <p>
     * <code>defaultPlaybackRate</code> 접근자의 번호.
     */
    private static final int A_DEFAULT_RATE = 9;

    /**
     * The id of the <code>playbackRate</code> accessor.
     * <p>
     * <code>playbackRate</code> 접근자의 번호.
     */
    private static final int A_RATE = 10;

    /**
     * The id of the <code>volume</code> accessor.
     * <p>
     * <code>volume</code> 접근자의 번호.
     */
    private static final int A_VOLUME = 11;

    /**
     * The id of the <code>muted</code> accessor.
     * <p>
     * <code>muted</code> 접근자의 번호.
     */
    private static final int A_MUTED = 12;

    /**
     * The id of the <code>buffered</code> getter.
     * <p>
     * <code>buffered</code> getter 의 번호.
     */
    private static final int A_BUFFERED = 13;

    /**
     * The id of the <code>seekable</code> getter.
     * <p>
     * <code>seekable</code> getter 의 번호.
     */
    private static final int A_SEEKABLE = 14;

    /**
     * The id of the <code>played</code> getter.
     * <p>
     * <code>played</code> getter 의 번호.
     */
    private static final int A_PLAYED = 15;

    /**
     * The id of the <code>videoWidth</code> and <code>videoHeight</code> getters (always 0).
     * <p>
     * <code>videoWidth</code>, <code>videoHeight</code> getter 의 번호(항상 0).
     */
    private static final int A_VIDEO_SIZE = 16;

    /**
     * The id of the <code>preservesPitch</code> accessor.
     * <p>
     * <code>preservesPitch</code> 접근자의 번호.
     */
    private static final int A_PRESERVES_PITCH = 17;

    /**
     * The id of the <code>load()</code> method.
     * <p>
     * <code>load()</code> 메서드의 번호.
     */
    private static final int M_LOAD = 0;

    /**
     * The id of the <code>play()</code> method.
     * <p>
     * <code>play()</code> 메서드의 번호.
     */
    private static final int M_PLAY = 1;

    /**
     * The id of the <code>pause()</code> method.
     * <p>
     * <code>pause()</code> 메서드의 번호.
     */
    private static final int M_PAUSE = 2;

    /**
     * The id of the <code>canPlayType()</code> method.
     * <p>
     * <code>canPlayType()</code> 메서드의 번호.
     */
    private static final int M_CAN_PLAY_TYPE = 3;

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The media elements of the page.
     * <p>
     * 페이지의 미디어 요소.
     */
    private final MediaElements media;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page  the page / 페이지
     * @param media the media elements / 미디어 요소
     */
    MediaBindings(PageScripts page, MediaElements media) {
        this.page = page;
        this.media = media;
    }

    /**
     * Returns the state of the media element that a receiver stands for, or throws the standard TypeError.
     * <p>
     * 수신자가 나타내는 미디어 요소의 상태를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element state / 요소 상태
     */
    private MediaElements.Media state(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (!MediaElements.isMedia(e)) {
            throw JsException.typeError("Illegal invocation");
        }
        return media.state(e);
    }

    /**
     * Converts an argument to a finite number as WebIDL does for a <code>double</code>.
     * <p>
     * WebIDL 이 <code>double</code> 에 하듯 인수를 유한한 수로 바꾼다.
     *
     * @param args the arguments / 인수
     * @return the number / 수
     */
    private static double finite(JsValue[] args) {
        double v = Ops.toNumber(Js.arg(args, 0));
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            throw JsException.typeError("The provided double value is non-finite.");
        }
        return v;
    }

    /**
     * The getters and setters of the attributes, selected by a number.
     * <p>
     * 번호로 고르는 속성의 getter 와 setter.
     */
    private final class Attribute implements NativeFunction.Call {
        /**
         * Which attribute (one of the <code>A_</code> constants).
         * <p>
         * 어느 속성인지(<code>A_</code> 상수 중 하나).
         */
        private final int id;

        /**
         * True for the setter.
         * <p>
         * setter 이면 true.
         */
        private final boolean setter;

        /**
         * Creates the accessor function.
         * <p>
         * 접근자 함수를 만든다.
         *
         * @param id     the attribute / 속성
         * @param setter true for the setter / setter 이면 true
         */
        Attribute(int id, boolean setter) {
            this.id = id;
            this.setter = setter;
        }

        /**
         * Reads or writes the attribute of the element.
         * <p>
         * 요소의 속성을 읽거나 쓴다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the value (undefined for a setter) / 값(setter 는 undefined)
         */
        public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            MediaElements.Media m = state(self);
            if (setter) {
                write(m, args);
                return Js.UNDEFINED;
            }
            return read(m);
        }

        /**
         * Reads the attribute.
         * <p>
         * 속성을 읽는다.
         *
         * @param m the element state / 요소 상태
         * @return the value / 값
         */
        private JsValue read(MediaElements.Media m) {
            DomBridge bridge = page.getBridge();
            switch (id) {
            case A_ERROR:
                return m.error == null ? Js.NULL : bridge.toJs(m.error);
            case A_CURRENT_SRC:
                return Js.str(m.currentSrc);
            case A_NETWORK_STATE:
                return Js.num(m.networkState);
            case A_READY_STATE:
                return Js.num(m.readyState);
            case A_SEEKING:
                return Js.bool(m.seeking);
            case A_CURRENT_TIME:
                return Js.num(media.position(m));
            case A_DURATION:
                return Js.num(m.duration);
            case A_PAUSED:
                return Js.bool(m.paused);
            case A_ENDED:
                return Js.bool(media.isEnded(m));
            case A_DEFAULT_RATE:
                return Js.num(m.defaultRate);
            case A_RATE:
                return Js.num(m.playbackRate);
            case A_PRESERVES_PITCH:
                return Js.bool(m.preservesPitch);
            case A_VOLUME:
                return Js.num(m.volume);
            case A_MUTED:
                return Js.bool(m.muted);
            case A_BUFFERED:
                return bridge.toJs(new TimeRangesValue(media.buffered(m)));
            case A_SEEKABLE:
                return bridge.toJs(new TimeRangesValue(media.seekable(m)));
            case A_PLAYED:
                return bridge.toJs(new TimeRangesValue(media.played(m)));
            default:
                return Js.num(0);
            }
        }

        /**
         * Writes the attribute.
         * <p>
         * 속성을 쓴다.
         *
         * @param m    the element state / 요소 상태
         * @param args the arguments / 인수
         */
        private void write(MediaElements.Media m, JsValue[] args) {
            switch (id) {
            case A_CURRENT_TIME:
                media.setPosition(m, finite(args));
                break;
            case A_DEFAULT_RATE:
            case A_RATE:
                double rate = finite(args);
                // 지원 범위(보통 속도의 4분의 1배에서 4배) 밖의 속도는 조용히 무시하지 않고 NotSupportedError 로 거절한다.
                if (!MediaPlayer.isRateSupported(rate)) {
                    throw page.getBridge().domError(DomException.notSupported("The playback rate provided (" + rate
                            + ") is not supported: use a rate from 0.25 to 4."));
                }
                if (id == A_RATE) {
                    media.setPlaybackRate(m, rate);
                } else {
                    media.setDefaultPlaybackRate(m, rate);
                }
                break;
            case A_PRESERVES_PITCH:
                media.setPreservesPitch(m, Ops.toBoolean(Js.arg(args, 0)));
                break;
            case A_VOLUME:
                double v = finite(args);
                if (v < 0 || v > 1) {
                    throw page.getBridge().domError(DomException.indexSize("The volume provided (" + v + ") is outside the range [0, 1]."));
                }
                media.setVolume(m, v);
                break;
            case A_MUTED:
                media.setMuted(m, Ops.toBoolean(Js.arg(args, 0)));
                break;
            default:
                break;
            }
        }
    }

    /**
     * The methods of the media elements, selected by a number.
     * <p>
     * 번호로 고르는 미디어 요소의 메서드.
     */
    private final class Method implements NativeFunction.Call {
        /**
         * Which method (one of the <code>M_</code> constants).
         * <p>
         * 어느 메서드인지(<code>M_</code> 상수 중 하나).
         */
        private final int id;

        /**
         * Creates the method function.
         * <p>
         * 메서드 함수를 만든다.
         *
         * @param id the method / 메서드
         */
        Method(int id) {
            this.id = id;
        }

        /**
         * Runs the method on the element.
         * <p>
         * 요소에서 메서드를 실행한다.
         *
         * @param r    the realm / realm
         * @param self this value / this 값
         * @param args arguments / 인수
         * @return the result / 결과
         */
        public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
            MediaElements.Media m = state(self);
            switch (id) {
            case M_LOAD:
                media.load(m);
                return Js.UNDEFINED;
            case M_PLAY:
                return media.play(m);
            case M_PAUSE:
                media.pause(m);
                return Js.UNDEFINED;
            default:
                return Js.str(media.canPlayType(m.element(), Ops.toString(Js.arg(args, 0))));
            }
        }
    }

    /**
     * Defines an accessor on a prototype.
     * <p>
     * 프로토타입에 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param id    the attribute / 속성
     * @param write true if the attribute can be set / 설정할 수 있으면 true
     */
    private void accessor(JsObject proto, String name, int id, boolean write) {
        JsRealm r = page.getRealm();
        proto.defineAccessor(name, r.newFunction("get " + name, 0, new Attribute(id, false)),
                write ? r.newFunction("set " + name, 1, new Attribute(id, true)) : null, true, true);
    }

    /**
     * Defines a method on a prototype.
     * <p>
     * 프로토타입에 메서드를 정의한다.
     *
     * @param proto  the prototype / 프로토타입
     * @param name   the name / 이름
     * @param length the number of declared parameters / 선언된 매개변수 수
     * @param id     the method / 메서드
     */
    private void method(JsObject proto, String name, int length, int id) {
        proto.defineData(name, page.getRealm().newFunction(name, length, new Method(id)), true, true, true);
    }

    /**
     * Installs everything.
     * <p>
     * 모두 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final DomBridge bridge = page.getBridge();
        bridge.defineInterface("MediaError", null, MediaErrorValue.class, new String[] { "c:MEDIA_ERR_ABORTED=1", "c:MEDIA_ERR_NETWORK=2",
            "c:MEDIA_ERR_DECODE=3", "c:MEDIA_ERR_SRC_NOT_SUPPORTED=4", "a:code", "a:message" });
        bridge.defineInterface("TimeRanges", null, TimeRangesValue.class, new String[] { "a:length", "m:start(int)", "m:end(int)" });
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Names the interface of the value objects of the media elements.
             * <p>
             * 미디어 요소의 값 객체의 인터페이스 이름을 정한다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름. 모르면 null
             */
            public String classify(Object target) {
                if (target instanceof MediaErrorValue) {
                    return "MediaError";
                }
                return target instanceof TimeRangesValue ? "TimeRanges" : null;
            }
        });
        JsObject proto = bridge.getInterface("HTMLMediaElement").getPrototype();
        accessor(proto, "error", A_ERROR, false);
        accessor(proto, "currentSrc", A_CURRENT_SRC, false);
        accessor(proto, "networkState", A_NETWORK_STATE, false);
        accessor(proto, "readyState", A_READY_STATE, false);
        accessor(proto, "seeking", A_SEEKING, false);
        accessor(proto, "currentTime", A_CURRENT_TIME, true);
        accessor(proto, "duration", A_DURATION, false);
        accessor(proto, "paused", A_PAUSED, false);
        accessor(proto, "ended", A_ENDED, false);
        accessor(proto, "defaultPlaybackRate", A_DEFAULT_RATE, true);
        accessor(proto, "playbackRate", A_RATE, true);
        accessor(proto, "preservesPitch", A_PRESERVES_PITCH, true);
        accessor(proto, "volume", A_VOLUME, true);
        accessor(proto, "muted", A_MUTED, true);
        accessor(proto, "buffered", A_BUFFERED, false);
        accessor(proto, "seekable", A_SEEKABLE, false);
        accessor(proto, "played", A_PLAYED, false);
        method(proto, "load", 0, M_LOAD);
        method(proto, "play", 0, M_PLAY);
        method(proto, "pause", 0, M_PAUSE);
        method(proto, "canPlayType", 1, M_CAN_PLAY_TYPE);
        JsObject video = bridge.getInterface("HTMLVideoElement").getPrototype();
        accessor(video, "videoWidth", A_VIDEO_SIZE, false);
        accessor(video, "videoHeight", A_VIDEO_SIZE, false);
        installAudioConstructor(g, bridge);
    }

    /**
     * Installs the <code>Audio</code> constructor: <code>new Audio(src)</code> creates an <code>audio</code> element whose
     * <code>preload</code> is <code>auto</code> and, when a URL is given, whose <code>src</code> is that URL (which starts the load).
     * <p>
     * <code>Audio</code> 생성자를 설치한다: <code>new Audio(src)</code> 는 <code>preload</code> 가 <code>auto</code> 인 <code>audio</code> 요소를
     * 만들고, URL 을 주면 <code>src</code> 를 그 URL 로 정한다(로드가 시작된다).
     *
     * @param g      the global object / 전역 객체
     * @param bridge the bridge / 브리지
     */
    private void installAudioConstructor(JsObject g, final DomBridge bridge) {
        NativeFunction ctor = new NativeFunction(page.getRealm(), "Audio", 0, new NativeFunction.Call() {
            /**
             * Calling without new is an error.
             * <p>
             * new 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'Audio': Please use the 'new' operator.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates the <code>audio</code> element.
             * <p>
             * <code>audio</code> 요소를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the element / 요소
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                Element e = page.getDocument().createElement("audio");
                e.setAttribute("preload", "auto");
                // URL 을 주면 src 로 정하여 로드를 시작한다.
                if (args.length > 0 && args[0] != Js.UNDEFINED) {
                    e.setAttribute("src", Ops.toString(args[0]));
                }
                return (JsObject) bridge.toJs(e);
            }
        });
        ctor.defineData("prototype", bridge.getInterface("HTMLAudioElement").getPrototype(), false, false, false);
        g.defineBuiltin("Audio", ctor);
    }
}
