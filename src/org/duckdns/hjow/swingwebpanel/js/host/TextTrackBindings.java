package org.duckdns.hjow.swingwebpanel.js.host;

import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.TrackEvent;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsString;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;
import org.duckdns.hjow.swingwebpanel.js.dom.HostObject;
import org.duckdns.hjow.swingwebpanel.js.dom.IndexedObject;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttCue;
import org.duckdns.hjow.swingwebpanel.media.vtt.VttRegion;

/**
 * The script interface of the text tracks (plan item P20-13; HTML 4.8.10 "The track element" and 4.8.11.11 "Timed text tracks", WebVTT 9 "API"):
 * the interfaces <code>TextTrackList</code>, <code>TextTrack</code>, <code>TextTrackCueList</code>, <code>TextTrackCue</code>, <code>VTTCue</code>
 * (with its constructor), <code>VTTRegion</code> (with its constructor) and <code>TrackEvent</code> (with its constructor), the members
 * <code>textTracks</code> and <code>addTextTrack()</code> of the media elements, and the members <code>kind</code>, <code>readyState</code> and
 * <code>track</code> of <code>HTMLTrackElement</code>. The accessors that are plain getters and setters come from the member specifications of the
 * DOM bridge and read the classes {@link TextTrackListValue}, {@link TextTrackValue}, {@link TextTrackCueListValue}, {@link TextTrackCueValue} and
 * {@link VttRegionValue}; the members that take objects or the keyword <code>auto</code> (<code>region</code>, <code>line</code>,
 * <code>position</code>) are written here. The interfaces that a script cannot construct (<code>TextTrackList</code>, <code>TextTrack</code>,
 * <code>TextTrackCueList</code>, <code>TextTrackCue</code>) throw <code>TypeError: Illegal constructor</code>.
 * <p>
 * 텍스트 트랙의 스크립트 인터페이스(계획 P20-13. HTML 4.8.10 "The track element", 4.8.11.11 "Timed text tracks", WebVTT 9 "API"): 인터페이스
 * <code>TextTrackList</code>, <code>TextTrack</code>, <code>TextTrackCueList</code>, <code>TextTrackCue</code>, <code>VTTCue</code>(생성자 포함),
 * <code>VTTRegion</code>(생성자 포함), <code>TrackEvent</code>(생성자 포함), 미디어 요소의 <code>textTracks</code>, <code>addTextTrack()</code>,
 * <code>HTMLTrackElement</code> 의 <code>kind</code>, <code>readyState</code>, <code>track</code>. 단순한 getter/setter 인 접근자는 DOM 브리지의 멤버
 * 명세에서 오며 {@link TextTrackListValue}, {@link TextTrackValue}, {@link TextTrackCueListValue}, {@link TextTrackCueValue}, {@link VttRegionValue}
 * 클래스를 읽는다. 객체나 키워드 <code>auto</code> 를 받는 멤버(<code>region</code>, <code>line</code>, <code>position</code>)는 여기서 쓴다. 스크립트가
 * 만들 수 없는 인터페이스(<code>TextTrackList</code>, <code>TextTrack</code>, <code>TextTrackCueList</code>, <code>TextTrackCue</code>)는
 * <code>TypeError: Illegal constructor</code> 를 던진다.
 */
final class TextTrackBindings {

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The media elements.
     * <p>
     * 미디어 요소.
     */
    private final MediaElements media;

    /**
     * The text tracks.
     * <p>
     * 텍스트 트랙.
     */
    private final TextTracks tracks;

    /**
     * Creates the installer.
     * <p>
     * 설치기를 만든다.
     *
     * @param page  the page / 페이지
     * @param media the media elements / 미디어 요소
     */
    TextTrackBindings(PageScripts page, MediaElements media) {
        this.page = page;
        this.media = media;
        this.tracks = media.textTracks();
    }

    /**
     * Returns the state of the media element that a receiver stands for, or throws the standard TypeError.
     * <p>
     * 수신자가 나타내는 미디어 요소의 상태를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the state / 상태
     */
    private MediaElements.Media mediaOf(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (!MediaElements.isMedia(e)) {
            throw JsException.typeError("Illegal invocation");
        }
        return media.state(e);
    }

    /**
     * Returns the <code>track</code> element that a receiver stands for, or throws the standard TypeError.
     * <p>
     * 수신자가 나타내는 <code>track</code> 요소를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the element / 요소
     */
    private static Element trackElementOf(JsValue self) {
        Element e = (Element) DomBridge.targetOf(self, Element.class);
        if (!TextTracks.isTrack(e)) {
            throw JsException.typeError("Illegal invocation");
        }
        return e;
    }

    /**
     * Returns the cue that a receiver stands for, or throws the standard TypeError.
     * <p>
     * 수신자가 나타내는 cue 를 반환하거나 표준 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the cue / cue
     */
    private static TextTrackCueValue cueOf(JsValue self) {
        Object t = DomBridge.targetOf(self, TextTrackCueValue.class);
        if (t == null) {
            throw JsException.typeError("Illegal invocation");
        }
        return (TextTrackCueValue) t;
    }

    /**
     * Defines an accessor on a prototype.
     * <p>
     * 프로토타입에 접근자를 정의한다.
     *
     * @param proto the prototype / 프로토타입
     * @param name  the name / 이름
     * @param get   the getter / getter
     * @param set   the setter or null / setter. 없으면 null
     */
    private void accessor(JsObject proto, String name, NativeFunction.Call get, NativeFunction.Call set) {
        JsRealm r = page.getRealm();
        proto.defineAccessor(name, r.newFunction("get " + name, 0, get), set == null ? null : r.newFunction("set " + name, 1, set), true, true);
    }

    /**
     * Makes a constructor that always fails, for an interface that a script cannot construct.
     * <p>
     * 스크립트가 만들 수 없는 인터페이스를 위해 항상 실패하는 생성자를 만든다.
     *
     * @param bridge the bridge / 브리지
     * @param name   the interface / 인터페이스
     * @param parent the parent interface object (the function the new one inherits from) or null / 부모 인터페이스 객체(새 함수가 상속하는 함수). 없으면 null
     * @return the new interface object / 새 인터페이스 객체
     */
    private NativeFunction illegalConstructor(DomBridge bridge, String name, JsObject parent) {
        JsRealm realm = page.getRealm();
        JsObject proto = parent == null ? realm.getFunctionPrototype() : parent;
        NativeFunction made = new NativeFunction(realm, proto, name, 0, new NativeFunction.Call() {
            /**
             * An interface object without a constructor cannot be called.
             * <p>
             * 생성자가 없는 인터페이스 객체는 부를 수 없다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Illegal constructor");
            }
        }, new NativeFunction.Construct() {
            /**
             * An interface object without a constructor cannot be constructed.
             * <p>
             * 생성자가 없는 인터페이스 객체는 만들 수 없다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return never / 반환하지 않음
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                throw JsException.typeError("Illegal constructor");
            }
        });
        bridge.setConstructor(name, made);
        return made;
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
        bridge.addClassifier(new DomBridge.Classifier() {
            /**
             * Names the interface of the objects of the text tracks.
             * <p>
             * 텍스트 트랙의 객체의 인터페이스 이름을 정한다.
             *
             * @param target the Java object / Java 객체
             * @return the interface name or null / 인터페이스 이름. 모르면 null
             */
            public String classify(Object target) {
                if (target instanceof TextTrackValue) {
                    return "TextTrack";
                } else if (target instanceof TextTrackListValue) {
                    return "TextTrackList";
                } else if (target instanceof TextTrackCueValue) {
                    return "VTTCue";
                } else if (target instanceof TextTrackCueListValue) {
                    return "TextTrackCueList";
                }
                return target instanceof VttRegionValue ? "VTTRegion" : null;
            }
        });
        installLists(bridge);
        installCues(bridge);
        installRegion(bridge);
        installTrackEvent(bridge);
        installElementMembers(bridge);
    }

    /**
     * Installs <code>TextTrackList</code>, <code>TextTrack</code> and <code>TextTrackCueList</code>.
     * <p>
     * <code>TextTrackList</code>, <code>TextTrack</code>, <code>TextTrackCueList</code> 를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private void installLists(DomBridge bridge) {
        DomBridge.InterfaceInfo list = bridge.defineInterface("TextTrackList", "EventTarget", TextTrackListValue.class,
                new String[] { "a:length", "m:getTrackById(String)" });
        bridge.getEventHandlers().define(list.getPrototype(), TextTrackListValue.class, new String[] { "change", "addtrack", "removetrack" });
        bridge.setIndexed("TextTrackList", new IndexedObject.Source() {
            /**
             * Returns the number of tracks.
             * <p>
             * 트랙의 수를 반환한다.
             *
             * @param target the list / 목록
             * @return the number / 수
             */
            public int length(Object target) {
                return ((TextTrackListValue) target).getLength();
            }

            /**
             * Returns a track.
             * <p>
             * 트랙을 반환한다.
             *
             * @param target the list / 목록
             * @param index  the index / 위치
             * @return the track / 트랙
             */
            public Object item(Object target, int index) {
                return ((TextTrackListValue) target).item(index);
            }

            /**
             * A text track list has no named access.
             * <p>
             * 텍스트 트랙 목록에는 이름 접근이 없다.
             *
             * @param target the list / 목록
             * @param name   the name / 이름
             * @return null / null
             */
            public Object named(Object target, String name) {
                return null;
            }
        });
        illegalConstructor(bridge, "TextTrackList", bridge.getInterface("EventTarget").getConstructor());
        DomBridge.InterfaceInfo track = bridge.defineInterface("TextTrack", "EventTarget", TextTrackValue.class,
                new String[] { "a:kind", "a:label", "a:language", "a:id", "a:inBandMetadataTrackDispatchType", "a:mode=", "a:cues", "a:activeCues",
                    "m:addCue(Object)", "m:removeCue(Object)" });
        bridge.getEventHandlers().define(track.getPrototype(), TextTrackValue.class, new String[] { "cuechange" });
        illegalConstructor(bridge, "TextTrack", bridge.getInterface("EventTarget").getConstructor());
        bridge.defineInterface("TextTrackCueList", null, TextTrackCueListValue.class, new String[] { "a:length", "m:getCueById(String)" });
        bridge.setIndexed("TextTrackCueList", new IndexedObject.Source() {
            /**
             * Returns the number of cues.
             * <p>
             * cue 의 수를 반환한다.
             *
             * @param target the list / 목록
             * @return the number / 수
             */
            public int length(Object target) {
                return ((TextTrackCueListValue) target).getLength();
            }

            /**
             * Returns a cue.
             * <p>
             * cue 를 반환한다.
             *
             * @param target the list / 목록
             * @param index  the index / 위치
             * @return the cue / cue
             */
            public Object item(Object target, int index) {
                return ((TextTrackCueListValue) target).item(index);
            }

            /**
             * A cue list has no named access.
             * <p>
             * cue 목록에는 이름 접근이 없다.
             *
             * @param target the list / 목록
             * @param name   the name / 이름
             * @return null / null
             */
            public Object named(Object target, String name) {
                return null;
            }
        });
        illegalConstructor(bridge, "TextTrackCueList", null);
    }

    /**
     * Installs <code>TextTrackCue</code> and <code>VTTCue</code> with the members that take objects or <code>auto</code>.
     * <p>
     * <code>TextTrackCue</code>, <code>VTTCue</code> 와 객체나 <code>auto</code> 를 받는 멤버를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private void installCues(final DomBridge bridge) {
        final JsRealm realm = page.getRealm();
        DomBridge.InterfaceInfo base = bridge.defineInterface("TextTrackCue", "EventTarget", TextTrackCueValue.class,
                new String[] { "a:track", "a:id=", "a:startTime=", "a:endTime=", "a:pauseOnExit=" });
        bridge.getEventHandlers().define(base.getPrototype(), TextTrackCueValue.class, new String[] { "enter", "exit" });
        final NativeFunction baseCtor = illegalConstructor(bridge, "TextTrackCue", bridge.getInterface("EventTarget").getConstructor());
        final DomBridge.InterfaceInfo vtt = bridge.defineInterface("VTTCue", "TextTrackCue", TextTrackCueValue.class,
                new String[] { "a:vertical=", "a:snapToLines=", "a:lineAlign=", "a:positionAlign=", "a:size=", "a:align=", "a:text=", "m:getCueAsHTML()" });
        accessor(vtt.getPrototype(), "region", new NativeFunction.Call() {
            /**
             * Reads <code>region</code>.
             * <p>
             * <code>region</code> 을 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the region or null / region. 없으면 null
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                VttRegionValue v = tracks.regionValue(cueOf(self).data.region);
                return v == null ? Js.NULL : bridge.toJs(v);
            }
        }, new NativeFunction.Call() {
            /**
             * Writes <code>region</code>: a <code>VTTRegion</code> or null.
             * <p>
             * <code>region</code> 을 쓴다: <code>VTTRegion</code> 또는 null.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                TextTrackCueValue cue = cueOf(self);
                JsValue v = Js.arg(args, 0);
                if (v.isNullOrUndefined()) {
                    cue.data.region = null;
                } else {
                    Object region = DomBridge.targetOf(v, VttRegionValue.class);
                    if (region == null) {
                        throw JsException.typeError("Failed to set the 'region' property on 'VTTCue': The provided value is not of type 'VTTRegion'.");
                    }
                    cue.data.region = ((VttRegionValue) region).data;
                }
                return Js.UNDEFINED;
            }
        });
        accessor(vtt.getPrototype(), "line", new NativeFunction.Call() {
            /**
             * Reads <code>line</code>: a number or <code>"auto"</code>.
             * <p>
             * <code>line</code> 을 읽는다: 수 또는 <code>"auto"</code>.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the line / 줄
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                double d = cueOf(self).data.line;
                return VttCue.isAuto(d) ? Js.str("auto") : Js.num(d);
            }
        }, new NativeFunction.Call() {
            /**
             * Writes <code>line</code>: a finite number or <code>"auto"</code>.
             * <p>
             * <code>line</code> 을 쓴다: 유한한 수 또는 <code>"auto"</code>.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                TextTrackCueValue cue = cueOf(self);
                cue.data.line = autoOrNumber(Js.arg(args, 0), "line");
                return Js.UNDEFINED;
            }
        });
        accessor(vtt.getPrototype(), "position", new NativeFunction.Call() {
            /**
             * Reads <code>position</code>: a number or <code>"auto"</code>.
             * <p>
             * <code>position</code> 을 읽는다: 수 또는 <code>"auto"</code>.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the position / 위치
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                double d = cueOf(self).data.position;
                return VttCue.isAuto(d) ? Js.str("auto") : Js.num(d);
            }
        }, new NativeFunction.Call() {
            /**
             * Writes <code>position</code>: <code>"auto"</code> or a number from 0 to 100 (<code>IndexSizeError</code> otherwise).
             * <p>
             * <code>position</code> 을 쓴다: <code>"auto"</code> 또는 0~100 의 수(아니면 <code>IndexSizeError</code>).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                TextTrackCueValue cue = cueOf(self);
                double d = autoOrNumber(Js.arg(args, 0), "position");
                if (!VttCue.isAuto(d) && (d < 0 || d > 100)) {
                    throw bridge.domError(org.duckdns.hjow.swingwebpanel.dom.DomException.indexSize("Failed to set the 'position' property on 'VTTCue': The value provided (" + d
                            + ") is outside the range (0, 100)."));
                }
                cue.data.position = d;
                return Js.UNDEFINED;
            }
        });
        bridge.setConstructor("VTTCue", new NativeFunction(realm, baseCtor, "VTTCue", 3, new NativeFunction.Call() {
            /**
             * Calling without <code>new</code> is an error.
             * <p>
             * <code>new</code> 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'VTTCue': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a cue from the start time, the end time and the text.
             * <p>
             * 시작 시각, 끝 시각, 텍스트로 cue 를 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the cue / cue
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                if (args.length < 3) {
                    throw JsException.typeError("Failed to construct 'VTTCue': 3 arguments required, but only " + args.length + " present.");
                }
                VttCue data = new VttCue();
                data.startTime = TextTrackCueValue.finite(Ops.toNumber(args[0]), "startTime");
                double end = Ops.toNumber(args[1]);
                if (Double.isNaN(end) || end == Double.NEGATIVE_INFINITY) {
                    throw JsException.typeError("Failed to construct 'VTTCue': The provided double value is non-finite.");
                }
                data.endTime = end;
                data.text = Ops.toString(args[2]);
                HostObject w = bridge.wrap(tracks.newCue(data));
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, vtt.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Converts the value of <code>line</code> or <code>position</code>: the string <code>auto</code> is the keyword, anything else is converted to a
     * number, which must be finite.
     * <p>
     * <code>line</code>, <code>position</code> 의 값을 변환한다: 문자열 <code>auto</code> 는 키워드이고 그 밖의 값은 수로 바꾸며 유한해야 한다.
     *
     * @param v    the value / 값
     * @param name the member for the message / 메시지용 멤버 이름
     * @return the number, or {@link VttCue#AUTO} / 수. 또는 {@link VttCue#AUTO}
     */
    private static double autoOrNumber(JsValue v, String name) {
        if (v instanceof JsString && "auto".equals(Ops.toString(v))) {
            return VttCue.AUTO;
        }
        double d = Ops.toNumber(v);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw JsException.typeError("Failed to set the '" + name + "' property on 'VTTCue': The provided double value is non-finite.");
        }
        return d;
    }

    /**
     * Installs <code>VTTRegion</code> with its constructor.
     * <p>
     * <code>VTTRegion</code> 과 그 생성자를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private void installRegion(final DomBridge bridge) {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("VTTRegion", null, VttRegionValue.class, new String[] { "a:id=", "a:width=", "a:lines=",
            "a:regionAnchorX=", "a:regionAnchorY=", "a:viewportAnchorX=", "a:viewportAnchorY=", "a:scroll=" });
        bridge.setConstructor("VTTRegion", new NativeFunction(page.getRealm(), "VTTRegion", 0, new NativeFunction.Call() {
            /**
             * Calling without <code>new</code> is an error.
             * <p>
             * <code>new</code> 없이 부르면 오류이다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return never / 반환하지 않음
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                throw JsException.typeError("Failed to construct 'VTTRegion': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates a region with the defaults.
             * <p>
             * 기본값으로 region 을 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the region / region
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                HostObject w = bridge.wrap(tracks.regionValue(new VttRegion()));
                w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                return w;
            }
        }));
    }

    /**
     * Installs <code>TrackEvent</code> with its constructor.
     * <p>
     * <code>TrackEvent</code> 와 그 생성자를 설치한다.
     *
     * @param bridge the bridge / 브리지
     */
    private void installTrackEvent(final DomBridge bridge) {
        final DomBridge.InterfaceInfo info = bridge.defineInterface("TrackEvent", "Event", TrackEvent.class, new String[] { "a:track" });
        bridge.setConstructor("TrackEvent", new NativeFunction(page.getRealm(), bridge.getInterface("Event").getConstructor(), "TrackEvent", 1,
                new NativeFunction.Call() {
                    /**
                     * Calling without <code>new</code> is an error.
                     * <p>
                     * <code>new</code> 없이 부르면 오류이다.
                     *
                     * @param r    the realm / realm
                     * @param self this value / this 값
                     * @param args arguments / 인수
                     * @return never / 반환하지 않음
                     */
                    public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                        throw JsException.typeError("Failed to construct 'TrackEvent': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
                    }
                }, new NativeFunction.Construct() {
                    /**
                     * Creates an event from a type and an init dictionary (<code>track</code>, <code>bubbles</code>, <code>cancelable</code>).
                     * <p>
                     * 종류와 초기화 사전(<code>track</code>, <code>bubbles</code>, <code>cancelable</code>)으로 이벤트를 만든다.
                     *
                     * @param r         the realm / realm
                     * @param args      arguments / 인수
                     * @param newTarget the new target / new 대상
                     * @return the event / 이벤트
                     */
                    public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                        if (args.length < 1) {
                            throw JsException.typeError("Failed to construct 'TrackEvent': 1 argument required, but only 0 present.");
                        }
                        JsValue opts = Js.arg(args, 1);
                        boolean bubbles = false;
                        boolean cancelable = false;
                        Object track = null;
                        if (opts.isObject()) {
                            bubbles = Ops.toBoolean(((JsObject) opts).get("bubbles"));
                            cancelable = Ops.toBoolean(((JsObject) opts).get("cancelable"));
                            JsValue t = ((JsObject) opts).get("track");
                            // track 은 텍스트 트랙이거나 null 이어야 한다.
                            if (!t.isNullOrUndefined()) {
                                track = DomBridge.targetOf(t, TextTrackValue.class);
                                if (track == null) {
                                    throw JsException.typeError("Failed to construct 'TrackEvent': The provided value is not of type 'TextTrack'.");
                                }
                            }
                        } else if (!opts.isUndefined()) {
                            throw JsException.typeError("Failed to construct 'TrackEvent': The provided value is not of type 'TrackEventInit'.");
                        }
                        HostObject w = bridge.wrap(new TrackEvent(Ops.toString(args[0]), bubbles, cancelable, track));
                        w.setPrototypeOf(Ops.getPrototypeFromConstructor(r, newTarget, info.getPrototype()));
                        return w;
                    }
                }));
    }

    /**
     * Installs the members of the elements: <code>textTracks</code> and <code>addTextTrack()</code> of the media elements, and
     * <code>kind</code>, <code>readyState</code> and <code>track</code> of <code>HTMLTrackElement</code>.
     * <p>
     * 요소의 멤버를 설치한다: 미디어 요소의 <code>textTracks</code>, <code>addTextTrack()</code> 과 <code>HTMLTrackElement</code> 의 <code>kind</code>,
     * <code>readyState</code>, <code>track</code>.
     *
     * @param bridge the bridge / 브리지
     */
    private void installElementMembers(final DomBridge bridge) {
        JsRealm realm = page.getRealm();
        JsObject mediaProto = bridge.getInterface("HTMLMediaElement").getPrototype();
        accessor(mediaProto, "textTracks", new NativeFunction.Call() {
            /**
             * Returns the live list of the text tracks (the same object every time).
             * <p>
             * 텍스트 트랙의 살아 있는 목록을 반환한다(매번 같은 객체).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the list / 목록
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(tracks.listValue(mediaOf(self)));
            }
        }, null);
        mediaProto.defineData("addTextTrack", realm.newFunction("addTextTrack", 1, new NativeFunction.Call() {
            /**
             * Creates a text track of a kind, with a label and a language, and adds it to the list.
             * <p>
             * 종류, 레이블, 언어가 있는 텍스트 트랙을 만들어 목록에 더한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments (kind, label, language) / 인수(종류, 레이블, 언어)
             * @return the track / 트랙
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                MediaElements.Media m = mediaOf(self);
                if (args.length < 1) {
                    throw JsException.typeError("Failed to execute 'addTextTrack' on 'HTMLMediaElement': 1 argument required, but only 0 present.");
                }
                String kind = Ops.toString(args[0]);
                boolean known = false;
                // 알려진 종류인지 확인한다(열거형이므로 아니면 TypeError).
                for (int i = 0; i < TextTrackValue.KINDS.length; i++) {
                    if (TextTrackValue.KINDS[i].equals(kind)) {
                        known = true;
                    }
                }
                if (!known) {
                    throw JsException.typeError("Failed to execute 'addTextTrack' on 'HTMLMediaElement': The provided value '" + kind
                            + "' is not a valid enum value of type TextTrackKind.");
                }
                String label = args.length > 1 && !args[1].isUndefined() ? Ops.toString(args[1]) : "";
                String language = args.length > 2 && !args[2].isUndefined() ? Ops.toString(args[2]) : "";
                return bridge.toJs(tracks.addTextTrack(m, kind, label, language));
            }
        }), true, true, true);
        JsObject trackProto = bridge.getInterface("HTMLTrackElement").getPrototype();
        accessor(trackProto, "kind", new NativeFunction.Call() {
            /**
             * Reads <code>kind</code>: the keyword that the attribute stands for.
             * <p>
             * <code>kind</code> 를 읽는다: 속성이 나타내는 키워드.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the kind / 종류
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(TextTrackValue.canonicalKind(trackElementOf(self).getAttribute("kind")));
            }
        }, new NativeFunction.Call() {
            /**
             * Writes <code>kind</code>: it sets the content attribute.
             * <p>
             * <code>kind</code> 를 쓴다: 콘텐츠 속성을 정한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                trackElementOf(self).setAttribute("kind", Ops.toString(Js.arg(args, 0)));
                return Js.UNDEFINED;
            }
        });
        accessor(trackProto, "readyState", new NativeFunction.Call() {
            /**
             * Reads <code>readyState</code> of the text track.
             * <p>
             * 텍스트 트랙의 <code>readyState</code> 를 읽는다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the state / 상태
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.num(tracks.trackFor(trackElementOf(self)).readyState);
            }
        }, null);
        accessor(trackProto, "track", new NativeFunction.Call() {
            /**
             * Returns the text track of the element (the same object every time).
             * <p>
             * 요소의 텍스트 트랙을 반환한다(매번 같은 객체).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the track / 트랙
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return bridge.toJs(tracks.trackFor(trackElementOf(self)));
            }
        }, null);
    }
}
