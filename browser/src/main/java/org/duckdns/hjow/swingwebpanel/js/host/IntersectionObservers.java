package org.duckdns.hjow.swingwebpanel.js.host;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.duckdns.hjow.swingwebpanel.css.cascade.ComputedStyle;
import org.duckdns.hjow.swingwebpanel.css.props.Prop;
import org.duckdns.hjow.swingwebpanel.dom.Document;
import org.duckdns.hjow.swingwebpanel.dom.DomRectReadOnly;
import org.duckdns.hjow.swingwebpanel.dom.Element;
import org.duckdns.hjow.swingwebpanel.dom.Node;
import org.duckdns.hjow.swingwebpanel.js.Js;
import org.duckdns.hjow.swingwebpanel.js.JsException;
import org.duckdns.hjow.swingwebpanel.js.JsObject;
import org.duckdns.hjow.swingwebpanel.js.JsRealm;
import org.duckdns.hjow.swingwebpanel.js.JsValue;
import org.duckdns.hjow.swingwebpanel.js.NativeFunction;
import org.duckdns.hjow.swingwebpanel.js.Ops;
import org.duckdns.hjow.swingwebpanel.js.WellKnownSymbols;
import org.duckdns.hjow.swingwebpanel.js.dom.DomBridge;

/**
 * <code>IntersectionObserver</code> and <code>IntersectionObserverEntry</code> (Intersection Observer, W3C Working Draft 18 October 2023, sections 2 and 3):
 * the constructor with its <code>root</code>, <code>rootMargin</code>, <code>scrollMargin</code> and <code>threshold</code> options, <code>observe</code>,
 * <code>unobserve</code>, <code>disconnect</code>, <code>takeRecords</code>, and the processing of "run the update intersection observations steps". Choices
 * where this page differs from a browser:
 * <ul>
 * <li>the containing block chain is followed through the computed <code>position</code> (<code>fixed</code> goes to the viewport, <code>absolute</code> to the
 * nearest positioned ancestor, the others to the parent element); a container with <code>overflow</code> other than <code>visible</code> clips with its padding
 * box (the <code>html</code> element, whose overflow belongs to the viewport, does not); <code>clip-path</code> and <code>contain</code> are not looked at;</li>
 * <li><code>isIntersecting</code> follows the clipped rectangle: a target that is clipped away completely is not intersecting, one that touches the root with
 * zero area is (the specification text compares the unclipped rectangles);</li>
 * <li>a target that has no box (not in the document or <code>display: none</code>) does not intersect and reports zero rectangles;</li>
 * <li><code>scrollMargin</code> is parsed and read back, but it does not change the clipping, because elements of the page are never scrolled;</li>
 * <li>percentages of <code>rootMargin</code> are relative to the width of the root rectangle for all four sides (as the text says);</li>
 * <li>an observer is notified in the same task that computed its entries (not in a task of its own), after the resize observers;</li>
 * <li>there are no cross-origin targets (every frame has its own page and observes its own document).</li>
 * </ul>
 * <p>
 * <code>IntersectionObserver</code> 와 <code>IntersectionObserverEntry</code>(Intersection Observer, W3C Working Draft 2023-10-18, 2~3절): <code>root</code>,
 * <code>rootMargin</code>, <code>scrollMargin</code>, <code>threshold</code> 옵션이 있는 생성자, <code>observe</code>, <code>unobserve</code>,
 * <code>disconnect</code>, <code>takeRecords</code>, "update intersection observations steps" 의 처리. 브라우저와 다른 선택:
 * <ul>
 * <li>포함 블록 체인은 계산된 <code>position</code> 으로 따라간다(<code>fixed</code> 는 뷰포트, <code>absolute</code> 는 가장 가까운 위치 지정 조상, 나머지는
 * 부모 요소). <code>overflow</code> 가 <code>visible</code> 이 아닌 컨테이너는 padding box 로 자른다(overflow 가 뷰포트의 것인 <code>html</code> 요소는 자르지
 * 않는다). <code>clip-path</code> 와 <code>contain</code> 은 보지 않는다.</li>
 * <li><code>isIntersecting</code> 은 잘린 사각형을 따른다. 완전히 잘려 나간 대상은 교차하지 않고, 면적 0 으로 root 에 닿은 대상은 교차한다(규격 문구는 자르지 않은
 * 사각형을 비교한다).</li>
 * <li>상자가 없는 대상(문서에 없거나 <code>display: none</code>)은 교차하지 않고 사각형은 모두 0 이다.</li>
 * <li><code>scrollMargin</code> 은 해석하고 되돌려 읽을 수 있지만 자르기에는 영향이 없다. 페이지의 요소는 스크롤되지 않기 때문이다.</li>
 * <li><code>rootMargin</code> 의 백분율은 네 변 모두 root 사각형의 폭에 대한 비율이다(문구대로).</li>
 * <li>관찰자는 항목을 계산한 같은 작업에서(자기만의 작업이 아니라) 크기 관찰자 뒤에 통지받는다.</li>
 * <li>교차 출처 대상은 없다(프레임마다 자기 페이지가 있고 자기 문서를 관찰한다).</li>
 * </ul>
 */
final class IntersectionObservers {

    /**
     * What one observer has registered for one target (the <code>IntersectionObserverRegistration</code> record).
     * <p>
     * 관찰자 하나가 대상 하나에 등록한 것(<code>IntersectionObserverRegistration</code> 기록).
     */
    private static final class Registration {
        /**
         * The index of the threshold that the last ratio reached (-1 before the first computation).
         * <p>
         * 마지막 비율이 도달한 임계값의 인덱스(첫 계산 전에는 -1).
         */
        int previousThresholdIndex = -1;

        /**
         * Whether the target was intersecting at the last computation.
         * <p>
         * 마지막 계산에서 대상이 교차했는지 여부.
         */
        boolean previousIsIntersecting;
    }

    /**
     * One observer.
     * <p>
     * 관찰자 하나.
     */
    private static final class Observer {
        /**
         * The callback.
         * <p>
         * 콜백.
         */
        JsObject callback;

        /**
         * The script object.
         * <p>
         * 스크립트 객체.
         */
        JsObject self;

        /**
         * The explicit root (an element or a document), or null for the implicit root.
         * <p>
         * 명시적 root(요소나 문서). 암묵적 root 이면 null.
         */
        Node root;

        /**
         * The <code>rootMargin</code> as top, right, bottom, left.
         * <p>
         * 위, 오른쪽, 아래, 왼쪽 순서의 <code>rootMargin</code>.
         */
        Margin rootMargin;

        /**
         * The <code>scrollMargin</code> as top, right, bottom, left.
         * <p>
         * 위, 오른쪽, 아래, 왼쪽 순서의 <code>scrollMargin</code>.
         */
        Margin scrollMargin;

        /**
         * The sorted thresholds.
         * <p>
         * 정렬된 임계값.
         */
        double[] thresholds;

        /**
         * The frozen script array that <code>thresholds</code> returns.
         * <p>
         * <code>thresholds</code> 가 반환하는 얼린 스크립트 배열.
         */
        JsValue thresholdArray;

        /**
         * The observed targets in the order of <code>observe()</code>, with their registrations.
         * <p>
         * <code>observe()</code> 순서의 관찰 대상과 그 등록.
         */
        final List<Element> targets = new ArrayList<Element>();

        /**
         * The registrations by target.
         * <p>
         * 대상별 등록.
         */
        final Map<Element, Registration> registrations = new HashMap<Element, Registration>();

        /**
         * The entries that wait for the callback (the <code>[[QueuedEntries]]</code> slot).
         * <p>
         * 콜백을 기다리는 항목(<code>[[QueuedEntries]]</code> 슬롯).
         */
        final List<JsValue> queue = new ArrayList<JsValue>();
    }

    /**
     * A parsed margin: four lengths that are pixels or percentages.
     * <p>
     * 해석한 여백: 픽셀이거나 백분율인 길이 네 개.
     */
    static final class Margin {
        /**
         * The values in top, right, bottom, left order.
         * <p>
         * 위, 오른쪽, 아래, 왼쪽 순서의 값.
         */
        final double[] value = new double[4];

        /**
         * Whether the value at an index is a percentage.
         * <p>
         * 해당 인덱스의 값이 백분율인지 여부.
         */
        final boolean[] percent = new boolean[4];

        /**
         * Serializes the margin the way the attributes return it: values separated by a space, with <code>px</code> or <code>%</code>.
         * <p>
         * 속성이 반환하는 방식으로 직렬화한다: 공백으로 구분한 값에 <code>px</code> 또는 <code>%</code> 를 붙인다.
         *
         * @return the text / 문자열
         */
        String serialize() {
            StringBuilder out = new StringBuilder();
            // 네 값을 차례로 적는다.
            for (int i = 0; i < 4; i++) {
                if (i > 0) {
                    out.append(' ');
                }
                out.append(Ops.toString(Js.num(value[i]))).append(percent[i] ? "%" : "px");
            }
            return out.toString();
        }

        /**
         * Returns the length of one side in pixels for a rectangle of a given width.
         * <p>
         * 주어진 폭의 사각형에 대한 한 변의 길이를 픽셀로 반환한다.
         *
         * @param side  0 top, 1 right, 2 bottom, 3 left / 0 위, 1 오른쪽, 2 아래, 3 왼쪽
         * @param width the width of the rectangle that percentages refer to / 백분율이 가리키는 사각형의 폭
         * @return pixels / 픽셀
         */
        double px(int side, double width) {
            return percent[side] ? value[side] * width / 100.0 : value[side];
        }
    }

    /**
     * The script object of an observer.
     * <p>
     * 관찰자의 스크립트 객체.
     */
    private static final class ObserverObject extends JsObject {
        /**
         * The observer behind the object.
         * <p>
         * 객체 뒤의 관찰자.
         */
        final Observer observer;

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto    the prototype / 프로토타입
         * @param observer the observer / 관찰자
         */
        ObserverObject(JsObject proto, Observer observer) {
            super(proto);
            this.observer = observer;
        }
    }

    /**
     * The script object of an entry: the values of the attributes in the order of {@link #ENTRY_ATTRIBUTES}.
     * <p>
     * 항목의 스크립트 객체: {@link #ENTRY_ATTRIBUTES} 순서의 속성 값.
     */
    private static final class EntryObject extends JsObject {
        /**
         * The attribute values.
         * <p>
         * 속성 값.
         */
        final JsValue[] values;

        /**
         * Creates the object.
         * <p>
         * 객체를 만든다.
         *
         * @param proto  the prototype / 프로토타입
         * @param values the attribute values / 속성 값
         */
        EntryObject(JsObject proto, JsValue[] values) {
            super(proto);
            this.values = values;
        }
    }

    /**
     * The attributes of <code>IntersectionObserverEntry</code> in the order of the values of an {@link EntryObject}.
     * <p>
     * {@link EntryObject} 의 값 순서와 같은 <code>IntersectionObserverEntry</code> 의 속성.
     */
    private static final String[] ENTRY_ATTRIBUTES = { "time", "rootBounds", "boundingClientRect", "intersectionRect", "isIntersecting", "intersectionRatio",
        "target" };

    /**
     * The page.
     * <p>
     * 페이지.
     */
    private final PageScripts page;

    /**
     * The coordinator.
     * <p>
     * 조정자.
     */
    private final RenderObservers owner;

    /**
     * All observers that observe at least one target.
     * <p>
     * 대상을 하나 이상 관찰하는 모든 관찰자.
     */
    private final List<Observer> observers = new ArrayList<Observer>();

    /**
     * The prototype of the entries.
     * <p>
     * 항목의 프로토타입.
     */
    private JsObject entryProto;

    /**
     * Creates the helper.
     * <p>
     * 도우미를 만든다.
     *
     * @param page  the page / 페이지
     * @param owner the coordinator / 조정자
     */
    IntersectionObservers(PageScripts page, RenderObservers owner) {
        this.page = page;
        this.owner = owner;
    }

    /**
     * Tells whether an observer has a target.
     * <p>
     * 관찰자에 대상이 있는지 알려 준다.
     *
     * @return true if one has / 있으면 true
     */
    boolean hasTargets() {
        return !observers.isEmpty();
    }

    /**
     * Installs <code>IntersectionObserver</code> and <code>IntersectionObserverEntry</code>.
     * <p>
     * <code>IntersectionObserver</code> 와 <code>IntersectionObserverEntry</code> 를 설치한다.
     *
     * @param g the global object / 전역 객체
     */
    void install(JsObject g) {
        final JsRealm realm = page.getRealm();
        installEntry(g, realm);
        final JsObject proto = realm.newObject();
        NativeFunction ctor = new NativeFunction(realm, "IntersectionObserver", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'IntersectionObserver': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an observer: the algorithm "initialize a new IntersectionObserver".
             * <p>
             * 관찰자를 만든다: "initialize a new IntersectionObserver" 알고리즘.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the observer object / 관찰자 객체
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue cb = Js.arg(args, 0);
                if (!cb.isCallable()) {
                    throw JsException.typeError("Failed to construct 'IntersectionObserver': The callback provided as parameter 1 is not a function.");
                }
                Observer o = new Observer();
                o.callback = (JsObject) cb;
                initialize(o, Js.arg(args, 1));
                o.self = new ObserverObject(Ops.getPrototypeFromConstructor(r, newTarget, proto), o);
                return o.self;
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG,
                org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor.data(Js.str("IntersectionObserver"), false, false, true));
        owner.getter(proto, "root", new NativeFunction.Call() {
            /**
             * Returns the explicit root or null.
             * <p>
             * 명시적 root 또는 null 을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the root / root
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                return o.root == null ? Js.NULL : page.getBridge().toJs(o.root);
            }
        });
        owner.getter(proto, "rootMargin", new NativeFunction.Call() {
            /**
             * Returns the serialized root margin.
             * <p>
             * 직렬화한 root 여백을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(observerOf(self).rootMargin.serialize());
            }
        });
        owner.getter(proto, "scrollMargin", new NativeFunction.Call() {
            /**
             * Returns the serialized scroll margin.
             * <p>
             * 직렬화한 스크롤 여백을 반환한다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the text / 문자열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return Js.str(observerOf(self).scrollMargin.serialize());
            }
        });
        owner.getter(proto, "thresholds", new NativeFunction.Call() {
            /**
             * Returns the frozen array of thresholds (the same array every time).
             * <p>
             * 임계값의 얼린 배열을 반환한다(매번 같은 배열).
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the array / 배열
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                return observerOf(self).thresholdArray;
            }
        });
        realm.method(proto, "observe", 1, new NativeFunction.Call() {
            /**
             * Observes a target: "observe a target Element".
             * <p>
             * 대상을 관찰한다: "observe a target Element".
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                observe(observerOf(self), elementArg(args, "observe"));
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "unobserve", 1, new NativeFunction.Call() {
            /**
             * Stops observing a target: "unobserve a target Element".
             * <p>
             * 대상 관찰을 멈춘다: "unobserve a target Element".
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                unobserve(observerOf(self), elementArg(args, "unobserve"));
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "disconnect", 0, new NativeFunction.Call() {
            /**
             * Stops observing every target.
             * <p>
             * 모든 대상의 관찰을 멈춘다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return undefined / undefined
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                o.targets.clear();
                o.registrations.clear();
                observers.remove(o);
                return Js.UNDEFINED;
            }
        });
        realm.method(proto, "takeRecords", 0, new NativeFunction.Call() {
            /**
             * Returns the queued entries and empties the queue.
             * <p>
             * 대기 중인 항목을 반환하고 대기열을 비운다.
             *
             * @param r    the realm / realm
             * @param self this value / this 값
             * @param args arguments / 인수
             * @return the entries / 항목
             */
            public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                Observer o = observerOf(self);
                JsValue array = r.newArray(new ArrayList<JsValue>(o.queue));
                o.queue.clear();
                return array;
            }
        });
        g.defineBuiltin("IntersectionObserver", ctor);
    }

    /**
     * Installs <code>IntersectionObserverEntry</code>: the constructor takes an init dictionary, the attributes are getters of the prototype.
     * <p>
     * <code>IntersectionObserverEntry</code> 를 설치한다: 생성자는 초기화 사전을 받고 속성은 프로토타입의 게터이다.
     *
     * @param g     the global object / 전역 객체
     * @param realm the realm / realm
     */
    private void installEntry(JsObject g, JsRealm realm) {
        entryProto = realm.newObject();
        final JsObject proto = entryProto;
        NativeFunction ctor = new NativeFunction(realm, "IntersectionObserverEntry", 1, new NativeFunction.Call() {
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
                throw JsException.typeError("Failed to construct 'IntersectionObserverEntry': Please use the 'new' operator, this DOM object constructor cannot be called as a function.");
            }
        }, new NativeFunction.Construct() {
            /**
             * Creates an entry from a <code>IntersectionObserverEntryInit</code> dictionary.
             * <p>
             * <code>IntersectionObserverEntryInit</code> 사전에서 항목을 만든다.
             *
             * @param r         the realm / realm
             * @param args      arguments / 인수
             * @param newTarget the new target / new 대상
             * @return the entry / 항목
             */
            public JsObject construct(JsRealm r, JsValue[] args, JsObject newTarget) {
                JsValue init = Js.arg(args, 0);
                if (!init.isObject()) {
                    throw JsException.typeError("Failed to construct 'IntersectionObserverEntry': 1 argument required, but only 0 present.");
                }
                JsObject d = (JsObject) init;
                JsValue[] values = new JsValue[ENTRY_ATTRIBUTES.length];
                // 필수 멤버를 차례로 읽어 값으로 바꾼다.
                for (int i = 0; i < ENTRY_ATTRIBUTES.length; i++) {
                    JsValue v = d.get(ENTRY_ATTRIBUTES[i]);
                    if (v.isUndefined()) {
                        throw JsException.typeError("Failed to construct 'IntersectionObserverEntry': required member " + ENTRY_ATTRIBUTES[i] + " is undefined.");
                    }
                    if (i == 1 || i == 2 || i == 3) {
                        values[i] = v.isObject() ? rectOf((JsObject) v) : Js.NULL;
                    } else if (i == 4) {
                        values[i] = Js.bool(Ops.toBoolean(v));
                    } else if (i == 0 || i == 5) {
                        values[i] = Js.num(RenderObservers.finite(v, ENTRY_ATTRIBUTES[i]));
                    } else {
                        values[i] = v;
                    }
                }
                return new EntryObject(Ops.getPrototypeFromConstructor(r, newTarget, proto), values);
            }
        });
        ctor.defineData("prototype", proto, false, false, false);
        proto.defineBuiltin("constructor", ctor);
        proto.defineOwnProperty(WellKnownSymbols.TO_STRING_TAG,
                org.duckdns.hjow.swingwebpanel.js.PropertyDescriptor.data(Js.str("IntersectionObserverEntry"), false, false, true));
        // 속성마다 프로토타입의 게터를 정의한다.
        for (int i = 0; i < ENTRY_ATTRIBUTES.length; i++) {
            final int index = i;
            owner.getter(proto, ENTRY_ATTRIBUTES[i], new NativeFunction.Call() {
                /**
                 * Returns the value of the attribute.
                 * <p>
                 * 속성의 값을 반환한다.
                 *
                 * @param r    the realm / realm
                 * @param self this value / this 값
                 * @param args arguments / 인수
                 * @return the value / 값
                 */
                public JsValue call(JsRealm r, JsValue self, JsValue[] args) {
                    if (!(self instanceof EntryObject)) {
                        throw JsException.typeError("Illegal invocation");
                    }
                    return ((EntryObject) self).values[index];
                }
            });
        }
        g.defineBuiltin("IntersectionObserverEntry", ctor);
    }

    /**
     * Reads a <code>DOMRectInit</code> (or a rectangle object) into a read-only rectangle.
     * <p>
     * <code>DOMRectInit</code>(또는 사각형 객체)를 읽기 전용 사각형으로 읽는다.
     *
     * @param d the dictionary / 사전
     * @return the rectangle / 사각형
     */
    private JsValue rectOf(JsObject d) {
        double x = member(d, "x");
        double y = member(d, "y");
        double w = member(d, "width");
        double h = member(d, "height");
        return page.getBridge().toJs(new DomRectReadOnly(x, y, w, h));
    }

    /**
     * Reads a number member of a dictionary (0 if absent).
     * <p>
     * 사전의 숫자 멤버를 읽는다(없으면 0).
     *
     * @param d    the dictionary / 사전
     * @param name the member / 멤버
     * @return the number / 숫자
     */
    private static double member(JsObject d, String name) {
        JsValue v = d.get(name);
        return v.isUndefined() ? 0 : RenderObservers.finite(v, name);
    }

    /**
     * Returns the observer behind a receiver or throws a TypeError.
     * <p>
     * 수신자 뒤의 관찰자를 반환하거나 TypeError 를 던진다.
     *
     * @param self this value / this 값
     * @return the observer / 관찰자
     */
    private static Observer observerOf(JsValue self) {
        if (self instanceof ObserverObject) {
            return ((ObserverObject) self).observer;
        }
        throw JsException.typeError("Illegal invocation");
    }

    /**
     * Returns the first argument as an element or throws a TypeError.
     * <p>
     * 첫 인수를 요소로 반환하거나 TypeError 를 던진다.
     *
     * @param args the arguments / 인수
     * @param what the method name / 메서드 이름
     * @return the element / 요소
     */
    private static Element elementArg(JsValue[] args, String what) {
        Element e = (Element) DomBridge.targetOf(Js.arg(args, 0), Element.class);
        if (e == null) {
            throw JsException.typeError("Failed to execute '" + what + "' on 'IntersectionObserver': parameter 1 is not of type 'Element'.");
        }
        return e;
    }

    /**
     * The algorithm "initialize a new IntersectionObserver": reads the options.
     * <p>
     * "initialize a new IntersectionObserver" 알고리즘: 옵션을 읽는다.
     *
     * @param o       the observer / 관찰자
     * @param options the options value / 옵션 값
     */
    private void initialize(Observer o, JsValue options) {
        String rootMargin = "0px";
        String scrollMargin = "0px";
        JsValue threshold = Js.UNDEFINED;
        // 옵션 사전이 있으면 멤버를 읽는다.
        if (options.isObject()) {
            JsObject d = (JsObject) options;
            JsValue root = d.get("root");
            if (!root.isUndefined() && !root.isNull()) {
                Node n = (Node) DomBridge.targetOf(root, Node.class);
                if (!(n instanceof Element) && !(n instanceof Document)) {
                    throw JsException.typeError("Failed to construct 'IntersectionObserver': Failed to read the 'root' property from 'IntersectionObserverInit': "
                            + "The provided value is not of type '(Document or Element)'.");
                }
                o.root = n;
            }
            JsValue rm = d.get("rootMargin");
            if (!rm.isUndefined()) {
                rootMargin = Ops.toString(rm);
            }
            JsValue sm = d.get("scrollMargin");
            if (!sm.isUndefined()) {
                scrollMargin = Ops.toString(sm);
            }
            threshold = d.get("threshold");
        }
        o.rootMargin = parseMargin(rootMargin);
        if (o.rootMargin == null) {
            throw JsException.syntaxError("Failed to construct 'IntersectionObserver': rootMargin must be specified in pixels or percent.");
        }
        o.scrollMargin = parseMargin(scrollMargin);
        if (o.scrollMargin == null) {
            throw JsException.syntaxError("Failed to construct 'IntersectionObserver': scrollMargin must be specified in pixels or percent.");
        }
        List<Double> list = new ArrayList<Double>();
        // 임계값은 숫자 하나이거나 숫자의 배열이다.
        if (threshold.isObject()) {
            JsObject array = (JsObject) threshold;
            long n = (long) Ops.toNumber(array.get("length"));
            // 배열의 원소를 차례로 읽어 임계값으로 모은다.
            for (long i = 0; i < n; i++) {
                list.add(Double.valueOf(RenderObservers.finite(array.get(String.valueOf(i)), "threshold")));
            }
        } else if (!threshold.isUndefined()) {
            list.add(Double.valueOf(RenderObservers.finite(threshold, "threshold")));
        }
        // 범위를 벗어난 임계값은 RangeError 이다.
        for (Double t : list) {
            if (t.doubleValue() < 0 || t.doubleValue() > 1) {
                throw JsException.rangeError("Failed to construct 'IntersectionObserver': Threshold values must be numbers between 0 and 1.");
            }
        }
        Collections.sort(list);
        if (list.isEmpty()) {
            list.add(Double.valueOf(0));
        }
        o.thresholds = new double[list.size()];
        List<JsValue> items = new ArrayList<JsValue>();
        // 정렬된 임계값을 배열과 스크립트 값에 옮긴다.
        for (int i = 0; i < o.thresholds.length; i++) {
            o.thresholds[i] = list.get(i).doubleValue();
            items.add(Js.num(o.thresholds[i]));
        }
        o.thresholdArray = owner.freeze(page.getRealm().newArray(items));
    }

    /**
     * Parses a margin: one to four lengths that are absolute lengths or percentages ("parse a margin").
     * <p>
     * 여백을 해석한다: 절대 길이나 백분율인 길이 한 개에서 네 개("parse a margin").
     *
     * @param text the text / 문자열
     * @return the margin, or null if the text is not a margin / 여백. 여백이 아니면 null
     */
    static Margin parseMargin(String text) {
        String t = text.trim();
        String[] parts = t.length() == 0 ? new String[0] : t.split("[ \\t\\r\\n\\f]+");
        if (parts.length > 4) {
            return null;
        }
        Margin m = new Margin();
        double[] v = new double[4];
        boolean[] pc = new boolean[4];
        int n = parts.length == 0 ? 1 : parts.length;
        // 토큰마다 숫자와 단위를 나눠 픽셀이나 백분율로 바꾼다.
        for (int i = 0; i < n; i++) {
            String token = parts.length == 0 ? "0px" : parts[i];
            int k = 0;
            // 숫자 부분의 끝을 찾는다.
            while (k < token.length() && ("+-.0123456789eE".indexOf(token.charAt(k)) >= 0)) {
                // e 가 단위의 시작(em 등)이면 숫자가 끝난 것이다.
                if ((token.charAt(k) == 'e' || token.charAt(k) == 'E') && (k + 1 >= token.length() || !(Character.isDigit(token.charAt(k + 1)) || token.charAt(k + 1) == '-'
                        || token.charAt(k + 1) == '+'))) {
                    break;
                }
                k++;
            }
            double number;
            try {
                number = Double.parseDouble(token.substring(0, k));
            } catch (NumberFormatException e) {
                return null;
            }
            String unit = token.substring(k).toLowerCase(Locale.ENGLISH);
            double factor;
            // 단위를 픽셀로 바꾸는 배율을 정한다(단위 없는 값은 허용되지 않는다).
            if (unit.equals("px")) {
                factor = 1;
            } else if (unit.equals("%")) {
                factor = 1;
                pc[i] = true;
            } else if (unit.equals("in")) {
                factor = 96;
            } else if (unit.equals("cm")) {
                factor = 96 / 2.54;
            } else if (unit.equals("mm")) {
                factor = 96 / 25.4;
            } else if (unit.equals("q")) {
                factor = 96 / 101.6;
            } else if (unit.equals("pt")) {
                factor = 96 / 72.0;
            } else if (unit.equals("pc")) {
                factor = 16;
            } else {
                return null;
            }
            v[i] = number * factor;
        }
        // 값이 하나, 둘, 셋이면 CSS 의 margin 처럼 나머지 변을 채운다.
        if (n == 1) {
            v[1] = v[2] = v[3] = v[0];
            pc[1] = pc[2] = pc[3] = pc[0];
        } else if (n == 2) {
            v[2] = v[0];
            pc[2] = pc[0];
            v[3] = v[1];
            pc[3] = pc[1];
        } else if (n == 3) {
            v[3] = v[1];
            pc[3] = pc[1];
        }
        System.arraycopy(v, 0, m.value, 0, 4);
        System.arraycopy(pc, 0, m.percent, 0, 4);
        return m;
    }

    /**
     * The algorithm "observe a target Element".
     * <p>
     * "observe a target Element" 알고리즘.
     *
     * @param o      the observer / 관찰자
     * @param target the target / 대상
     */
    private void observe(Observer o, Element target) {
        if (o.registrations.containsKey(target)) {
            return;
        }
        o.registrations.put(target, new Registration());
        o.targets.add(target);
        if (!observers.contains(o)) {
            observers.add(o);
        }
        // 처음 관찰하는 대상은 다음 렌더링 단계에서 초기 항목을 만든다.
        owner.requestUpdate();
    }

    /**
     * The algorithm "unobserve a target Element".
     * <p>
     * "unobserve a target Element" 알고리즘.
     *
     * @param o      the observer / 관찰자
     * @param target the target / 대상
     */
    private void unobserve(Observer o, Element target) {
        if (o.registrations.remove(target) == null) {
            return;
        }
        o.targets.remove(target);
        if (o.targets.isEmpty()) {
            observers.remove(o);
        }
    }

    /**
     * Runs the step "run the update intersection observations steps" for all observers, then notifies the observers that have entries.
     * <p>
     * 모든 관찰자에 대해 "run the update intersection observations steps" 단계를 실행하고 항목이 있는 관찰자에 통지한다.
     */
    void run() {
        if (observers.isEmpty()) {
            return;
        }
        double time = owner.now();
        // 관찰자마다 대상을 observe() 순서대로 계산한다.
        for (Observer o : new ArrayList<Observer>(observers)) {
            Rect rootRect = rootRectangle(o);
            Rect rootBounds = rootRect == null ? null : expand(rootRect, o.rootMargin);
            // 대상마다 교차를 계산하고 임계값이나 교차 상태가 바뀌었으면 항목을 대기열에 넣는다.
            for (Element target : new ArrayList<Element>(o.targets)) {
                Registration reg = o.registrations.get(target);
                if (reg == null) {
                    continue;
                }
                Result r = compute(o, target, rootBounds);
                int index = 0;
                // 첫 임계값보다 큰 값을 찾고 없으면 길이이다.
                while (index < o.thresholds.length && !(o.thresholds[index] > r.ratio)) {
                    index++;
                }
                if (index != reg.previousThresholdIndex || r.intersecting != reg.previousIsIntersecting) {
                    o.queue.add(entry(time, rootBounds, r, target));
                }
                reg.previousThresholdIndex = index;
                reg.previousIsIntersecting = r.intersecting;
            }
        }
        // 항목이 쌓인 관찰자에 콜백을 부른다("notify intersection observers").
        for (Observer o : new ArrayList<Observer>(observers)) {
            if (o.queue.isEmpty()) {
                continue;
            }
            JsValue entries = page.getRealm().newArray(new ArrayList<JsValue>(o.queue));
            o.queue.clear();
            try {
                Ops.call(o.callback, o.self, new JsValue[] { entries, o.self });
            } catch (JsException e) {
                page.reportError(e);
            }
        }
    }

    /**
     * Builds the entry object of a computation.
     * <p>
     * 계산 결과의 항목 객체를 만든다.
     *
     * @param time       the time / 시각
     * @param rootBounds the root rectangle, or null / root 사각형. 없으면 null
     * @param r          the result / 결과
     * @param target     the target / 대상
     * @return the entry / 항목
     */
    private JsValue entry(double time, Rect rootBounds, Result r, Element target) {
        DomBridge b = page.getBridge();
        JsValue[] values = new JsValue[ENTRY_ATTRIBUTES.length];
        values[0] = Js.num(time);
        values[1] = rootBounds == null ? Js.NULL : b.toJs(rootBounds.toDom());
        values[2] = b.toJs(r.targetRect.toDom());
        values[3] = b.toJs(r.intersectionRect.toDom());
        values[4] = Js.bool(r.intersecting);
        values[5] = Js.num(r.ratio);
        values[6] = b.toJs(target);
        return new EntryObject(entryProto, values);
    }

    /**
     * A rectangle in viewport pixels.
     * <p>
     * 뷰포트 픽셀의 사각형.
     */
    private static final class Rect {
        /**
         * The left edge.
         * <p>
         * 왼쪽 가장자리.
         */
        final double x;

        /**
         * The top edge.
         * <p>
         * 위쪽 가장자리.
         */
        final double y;

        /**
         * The width (not negative).
         * <p>
         * 폭(음수가 아님).
         */
        final double w;

        /**
         * The height (not negative).
         * <p>
         * 높이(음수가 아님).
         */
        final double h;

        /**
         * Creates a rectangle.
         * <p>
         * 사각형을 만든다.
         *
         * @param x the left / 왼쪽
         * @param y the top / 위쪽
         * @param w the width / 폭
         * @param h the height / 높이
         */
        Rect(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = Math.max(0, w);
            this.h = Math.max(0, h);
        }

        /**
         * Converts to the DOM rectangle that scripts see.
         * <p>
         * 스크립트가 보는 DOM 사각형으로 바꾼다.
         *
         * @return the rectangle / 사각형
         */
        DomRectReadOnly toDom() {
            return new DomRectReadOnly(x, y, w, h);
        }

        /**
         * Returns the area.
         * <p>
         * 면적을 반환한다.
         *
         * @return width times height / 폭 곱하기 높이
         */
        double area() {
            return w * h;
        }

        /**
         * Intersects with another rectangle; rectangles that only touch intersect in a rectangle without area.
         * <p>
         * 다른 사각형과 교차시킨다. 닿기만 하는 사각형은 면적 없는 사각형으로 교차한다.
         *
         * @param o the other rectangle / 다른 사각형
         * @return the intersection, or null if they are apart / 교차한 사각형. 떨어져 있으면 null
         */
        Rect intersect(Rect o) {
            double left = Math.max(x, o.x);
            double top = Math.max(y, o.y);
            double right = Math.min(x + w, o.x + o.w);
            double bottom = Math.min(y + h, o.y + o.h);
            if (right < left || bottom < top) {
                return null;
            }
            return new Rect(left, top, right - left, bottom - top);
        }
    }

    /**
     * The result of the computation for one target.
     * <p>
     * 대상 하나에 대한 계산 결과.
     */
    private static final class Result {
        /**
         * The bounding box of the target (zero if it has none).
         * <p>
         * 대상의 bounding box(없으면 0).
         */
        Rect targetRect = new Rect(0, 0, 0, 0);

        /**
         * The visible part of the target.
         * <p>
         * 대상의 보이는 부분.
         */
        Rect intersectionRect = new Rect(0, 0, 0, 0);

        /**
         * Whether the target intersects the root.
         * <p>
         * 대상이 root 와 교차하는지 여부.
         */
        boolean intersecting;

        /**
         * The ratio of the intersection area to the area of the target.
         * <p>
         * 대상 면적에 대한 교차 면적의 비율.
         */
        double ratio;
    }

    /**
     * Returns the root intersection rectangle without the margin: the viewport for the implicit root and for a document, the padding box of a clipping element
     * or else its border box for an element.
     * <p>
     * 여백을 뺀 root 교차 사각형을 반환한다: 암묵적 root 와 문서는 뷰포트, 요소는 자르는 요소이면 padding box, 아니면 border box.
     *
     * @param o the observer / 관찰자
     * @return the rectangle, or null if the root has no box / 사각형. root 에 상자가 없으면 null
     */
    private Rect rootRectangle(Observer o) {
        HostEnvironment env = page.getEnvironment();
        if (o.root == null || o.root instanceof Document) {
            return new Rect(0, 0, env.getViewportWidth(), env.getViewportHeight());
        }
        Element root = (Element) o.root;
        if (!owner.isRendered(root)) {
            return new Rect(0, 0, 0, 0);
        }
        return clipsContent(root) ? paddingBox(root) : box(root);
    }

    /**
     * Returns the border box of an element.
     * <p>
     * 요소의 border box 를 반환한다.
     *
     * @param e the element / 요소
     * @return the rectangle / 사각형
     */
    private Rect box(Element e) {
        double[] b = owner.bounds(e);
        return new Rect(b[0], b[1], b[2], b[3]);
    }

    /**
     * Returns the padding box of an element (its border box without the borders).
     * <p>
     * 요소의 padding box 를 반환한다(border box 에서 테두리를 뺀 것).
     *
     * @param e the element / 요소
     * @return the rectangle / 사각형
     */
    private Rect paddingBox(Element e) {
        double[] b = owner.bounds(e);
        ComputedStyle cs = owner.style(e);
        if (cs == null) {
            return new Rect(b[0], b[1], b[2], b[3]);
        }
        double top = number(cs.px(Prop.BORDER_TOP_WIDTH));
        double left = number(cs.px(Prop.BORDER_LEFT_WIDTH));
        double right = number(cs.px(Prop.BORDER_RIGHT_WIDTH));
        double bottom = number(cs.px(Prop.BORDER_BOTTOM_WIDTH));
        return new Rect(b[0] + left, b[1] + top, b[2] - left - right, b[3] - top - bottom);
    }

    /**
     * Returns the number, or 0 for NaN.
     * <p>
     * 숫자를 반환하며 NaN 이면 0.
     *
     * @param d the number / 숫자
     * @return the number / 숫자
     */
    private static double number(double d) {
        return Double.isNaN(d) ? 0 : d;
    }

    /**
     * Tells whether an element clips its content to its padding box (its <code>overflow</code> is not <code>visible</code> on some axis).
     * <p>
     * 요소가 내용을 padding box 로 자르는지 알려 준다(어느 축이든 <code>overflow</code> 가 <code>visible</code> 이 아니다).
     *
     * @param e the element / 요소
     * @return true if it clips / 자르면 true
     */
    private boolean clipsContent(Element e) {
        ComputedStyle cs = owner.style(e);
        if (cs == null) {
            return false;
        }
        String x = cs.keyword(Prop.OVERFLOW_X);
        String y = cs.keyword(Prop.OVERFLOW_Y);
        return (x != null && !x.equals("visible")) || (y != null && !y.equals("visible"));
    }

    /**
     * Expands a rectangle by a margin (positive values grow it); percentages refer to the width of the rectangle.
     * <p>
     * 사각형을 여백만큼 넓힌다(양수는 키운다). 백분율은 사각형의 폭에 대한 비율이다.
     *
     * @param r the rectangle / 사각형
     * @param m the margin / 여백
     * @return the expanded rectangle / 넓힌 사각형
     */
    private static Rect expand(Rect r, Margin m) {
        double top = m.px(0, r.w);
        double right = m.px(1, r.w);
        double bottom = m.px(2, r.w);
        double left = m.px(3, r.w);
        return new Rect(r.x - left, r.y - top, r.w + left + right, r.h + top + bottom);
    }

    /**
     * Returns the containing block of an element as far as the clip chain needs it: null for the viewport.
     * <p>
     * 클립 체인에 필요한 만큼의 요소의 포함 블록을 반환한다. 뷰포트이면 null.
     *
     * @param e the element / 요소
     * @return the containing block element, or null for the viewport / 포함 블록 요소. 뷰포트이면 null
     */
    private Element containingBlock(Element e) {
        ComputedStyle cs = owner.style(e);
        String position = cs == null ? null : cs.keyword(Prop.POSITION);
        if ("fixed".equals(position)) {
            return null;
        }
        Element parent = e.getParentElement();
        // 절대 위치는 위치가 지정된 가장 가까운 조상이 포함 블록이다.
        if ("absolute".equals(position)) {
            while (parent != null) {
                ComputedStyle ps = owner.style(parent);
                String pp = ps == null ? null : ps.keyword(Prop.POSITION);
                if (pp != null && !pp.equals("static")) {
                    return parent;
                }
                parent = parent.getParentElement();
            }
            return null;
        }
        return parent;
    }

    /**
     * The algorithm "compute the intersection of a target element and the root", with the result of the step.
     * <p>
     * "compute the intersection of a target element and the root" 알고리즘과 단계의 결과.
     *
     * @param o          the observer / 관찰자
     * @param target     the target / 대상
     * @param rootBounds the root rectangle with the margin, or null / 여백을 더한 root 사각형. 없으면 null
     * @return the result / 결과
     */
    private Result compute(Observer o, Element target, Rect rootBounds) {
        Result out = new Result();
        // 명시적 root 의 후손이 아니거나 다른 문서의 대상은 교차하지 않는다.
        if (o.root != null && !o.root.contains(target)) {
            return out;
        }
        if (rootBounds == null || !owner.isRendered(target)) {
            return out;
        }
        Rect targetRect = box(target);
        out.targetRect = targetRect;
        Rect visible = targetRect;
        Element stop = o.root instanceof Element ? (Element) o.root : target.getOwnerDocument().getDocumentElement();
        Element container = containingBlock(target);
        // 포함 블록을 따라 올라가며 내용을 자르는 컨테이너의 padding box 로 자른다.
        while (visible != null && container != null && container != stop) {
            if (clipsContent(container)) {
                visible = visible.intersect(paddingBox(container));
            }
            container = containingBlock(container);
        }
        // 마지막으로 root 교차 사각형과 교차시킨다.
        Rect result = visible == null ? null : visible.intersect(rootBounds);
        if (result == null) {
            return out;
        }
        out.intersectionRect = result;
        out.intersecting = true;
        double targetArea = targetRect.area();
        // 면적이 있으면 면적 비율, 없으면 교차 여부에 따라 1 또는 0 이다.
        if (targetArea > 0) {
            out.ratio = Math.min(1.0, result.area() / targetArea);
        } else {
            out.ratio = 1;
        }
        return out;
    }

    /**
     * Drops all observers (the page is disposed).
     * <p>
     * 모든 관찰자를 버린다(페이지가 폐기됨).
     */
    void dispose() {
        observers.clear();
    }
}
